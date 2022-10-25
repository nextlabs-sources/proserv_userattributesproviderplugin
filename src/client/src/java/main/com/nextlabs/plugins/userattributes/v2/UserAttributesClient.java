package com.nextlabs.plugins.userattributes.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.commons.api.BasicCache;

import com.bluejungle.destiny.agent.controlmanager.AgentTypeEnum;
import com.bluejungle.destiny.agent.controlmanager.ControlMngr;
import com.bluejungle.destiny.agent.controlmanager.IControlManager;
import com.bluejungle.destiny.agent.ipc.IOSWrapper;
import com.bluejungle.destiny.agent.ipc.OSWrapper;
import com.bluejungle.framework.comp.ComponentManagerFactory;
import com.bluejungle.framework.crypt.IDecryptor;
import com.bluejungle.framework.crypt.ReversibleEncryptor;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IMultivalue;
import com.bluejungle.framework.expressions.ValueType;
import com.bluejungle.framework.utils.SerializationUtils;
import com.bluejungle.pf.domain.destiny.serviceprovider.IHeartbeatServiceProvider;
import com.nextlabs.plugins.userattributes.v2.dto.UserAttributesMetadataDTO;
import com.nextlabs.plugins.userattributes.v2.dto.UserAttributesRequestDTO;
import com.nextlabs.plugins.userattributes.v2.dto.UserInformationDTO;
import com.nextlabs.plugins.userattributes.v2.helper.ByteIntegerConverter;
import com.nextlabs.plugins.userattributes.v2.helper.OSUtils;
import com.nextlabs.plugins.userattributes.v2.helper.PropertyLoader;
import com.nextlabs.plugins.userattributes.v2.helper.RequestType;
import com.nextlabs.plugins.userattributes.v2.helper.Utils;
import com.nextlabs.rms.cache.RMSCacheManager;

public class UserAttributesClient implements IHeartbeatServiceProvider {
	private static final String REQUEST_MODE_KEY = "request_mode";
	private static final String INFINI_SPAN_MODE = "infinispan_mode";
	private static final String INFINI_SPAN_HOSTNAME = "infinispan_hostname";
	private static final String INFINI_SPAN_PORT = "infinispan_port";
	private static final String ATTRTYPE = "attrtype";
	private static final String ATTRINDEX = "attrindex";
	private static final String ALL_DOMAINS_USERS = "all_domains_users";
	private static final String CURRENT_DOMAIN_USERS = "current_domain_users";
	private static final String LOGGED_IN_USERS = "logged_in_users";

	private long timestamp = 0;
	private static String hostName;
	private IOSWrapper osWrapper = ComponentManagerFactory.getComponentManager().getComponent(OSWrapper.class);
	private IControlManager controlMngr = ComponentManagerFactory.getComponentManager()
			.getComponent(ControlMngr.COMP_INFO);
	private static AgentTypeEnum agentType;
	private Map<String, String> attributesType = new HashMap<String, String>();
	private Map<String, Integer> attrToIndexMap = new HashMap<String, Integer>();
	private static final Logger log = LogManager.getLogger(UserAttributesClient.class.getName());
	private static byte[] bytes;
	private Runtime runtime = Runtime.getRuntime();
	private static String installloc;
	public static Properties PLUGIN_PROPS = null;
	public static Properties HOTROD_PROPS = null;
	public static final String CLIENT_PROPS_FILE = "jservice/config/UserAttributesClient.properties";
	public static final String CACHE_NAME = "UserAttributeClientCache";
	public int MB = 1024 * 1024;
	public boolean getAllUsers = true;
	private static BasicCache<String, HashMap<String, Integer>> eapattrcache;
	private static BasicCache<String, List<Object>> eapCache;
	private boolean isInfiniSpanCache;
	private RequestType requestType;
	private ClassLoader classLoader = getClass().getClassLoader();

	public void init() {
		try {
			long startTime = System.nanoTime();
			log.debug("init() started");
	
			hostName = osWrapper.getFQDN();
	
			// get agent type
			agentType = AgentTypeEnum.getAgentTypeEnum(controlMngr.getAgentType().getValue());
			log.debug("agent type is " + agentType.getName());
	
			installloc = findInstallFolder();
			IDecryptor decryptor = new ReversibleEncryptor();
		
			PLUGIN_PROPS = PropertyLoader.loadProperties(installloc + CLIENT_PROPS_FILE);
			
			HOTROD_PROPS=new Properties();
			HOTROD_PROPS.setProperty("infinispan.client.hotrod.server_list", PLUGIN_PROPS.getProperty("infinispan.client.hotrod.server_list"));
			HOTROD_PROPS.setProperty("infinispan.client.hotrod.auth_username", PLUGIN_PROPS.getProperty("infinispan.client.hotrod.auth_username"));
			HOTROD_PROPS.setProperty("infinispan.client.hotrod.auth_password",decryptor.decrypt(PLUGIN_PROPS.getProperty("infinispan.client.hotrod.auth_password")));
			HOTROD_PROPS.setProperty("infinispan.client.hotrod.auth_realm", PLUGIN_PROPS.getProperty("infinispan.client.hotrod.auth_realm"));
			HOTROD_PROPS.setProperty("infinispan.client.hotrod.sasl_mechanism", PLUGIN_PROPS.getProperty("infinispan.client.hotrod.sasl_mechanism"));
			// Set Request Type
		
			checkRequestType();
	
			// Initialize Cache
			initializeCache(HOTROD_PROPS);
	
			// Load Initial Data
			loadInitialData();
	
			log.debug("init() finished");
	
			long endTime = System.nanoTime();
			log.info("Time Taken: " + Long.toString((endTime - startTime) / 1000) + "us");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void checkRequestType() {
		String requestTypeString = PLUGIN_PROPS.getProperty(REQUEST_MODE_KEY, ALL_DOMAINS_USERS.toLowerCase());
		if (requestTypeString.equals(ALL_DOMAINS_USERS)) {
			log.info("Will get All Users from All Domains");
			this.requestType = RequestType.ALL_USERS_FROM_ALL_DOMAINS;
		} else if (requestTypeString.equals(CURRENT_DOMAIN_USERS)) {
			log.info("Will get All Users from Current Domain");
			this.requestType = RequestType.ALL_USERS_FROM_CURRENT_DOMAIN;
		} else if (requestTypeString.equals(LOGGED_IN_USERS)) {
			log.info("Will get All Logged In Users from Current Domain");
			this.requestType = RequestType.LOGGED_IN_USERS_FROM_CURRENT_DOMAIN;
		} else {
			// Default to "all users from all domains"
			log.info("Default to get All Users from All Domains");
			this.requestType = RequestType.ALL_USERS_FROM_ALL_DOMAINS;
		}
	}

	private void loadInitialData() {
		try {
			loadFromDisk();
		} catch (FileNotFoundException e) {
			log.info("No cached attribute information stored on disk");
		} catch (IOException e) {
			log.error("Unable to load attributes information from disk", e);
		}
	}

	private void initializeCache(Properties hOTROD_PROPS2) {
				
			isInfiniSpanCache = true;
			initializeInfiniSpanCache(hOTROD_PROPS2);
		
	}

	private void initializeInfiniSpanCache(Properties hOTROD_PROPS2) {
		try {
			ISCacheManager.init(hOTROD_PROPS2, log);
			ISCacheManager cacheManager = ISCacheManager.getInstance();
			eapCache = cacheManager.getEapCache();
			eapattrcache = cacheManager.getEapAttrCache();
		} catch (IOException e) {
			log.error("cache not initialized");
		}
	}

	public Serializable prepareRequest(String id) {
		log.debug(String.format("OnPrepareRequest: %d MB Free, %d MB Used, %d MB Total", runtime.freeMemory() / MB,
				(runtime.totalMemory() - runtime.freeMemory()) / MB, runtime.totalMemory() / MB));
		log.debug("Plugin id is " + id);
		if (UserAttributesRequestDTO.PLUGIN.equals(id)) {
			String[] currentUsers = currentUserIds();
			UserAttributesRequestDTO request = new UserAttributesRequestDTO(timestamp, hostName, currentUsers,
					this.requestType);

			return (Serializable) request;
		} else {
			log.warn("Plugin id doesn not match");
		}

		return null;
	}

	public void processResponse(String id, String data) {
		log.debug(String.format("Data Size: %d MB", data.length() / MB));
		log.debug(String.format("OnReceiveResponse: %d MB Free, %d MB Used, %d MB Total", runtime.freeMemory() / MB,
				(runtime.totalMemory() - runtime.freeMemory()) / MB, runtime.totalMemory() / MB));
		log.info("Start processing response from Server with data length " + data.length() + " with memory "
				+ runtime.totalMemory() / MB);

		bytes = (byte[]) SerializationUtils.unwrapSerialized(data, this.classLoader);

		log.info("After serialized memory " + runtime.maxMemory() / MB);

		data = null;
		if (bytes == null) {
			if (log.isInfoEnabled())
				log.info("Server Response : bytes is null");
		}
		if (bytes != null) {

			if (UserAttributesRequestDTO.PLUGIN.equals(id)) {
				String sPath = findInstallFolder();
				String zipFileName = sPath + "/userattributes.zip";

				try {
					FileOutputStream fileOutputStream = new FileOutputStream(zipFileName);
					fileOutputStream.write(bytes);
					fileOutputStream.close();

					File zipFile = new File(zipFileName);
					Utils.uncompressFiles(sPath, zipFile);

					loadFromDisk();
				} catch (FileNotFoundException e) {
					log.error("FileNotFoundException", e);
				} catch (IOException e) {
					log.error("IOException", e);
				}
			}
		}
	}

	private synchronized void update(UserAttributesMetadataDTO metadata) {
		if (metadata == null) {
			log.warn("There is no response to update");
			return;
		}

		if (timestamp > 0 && (timestamp == metadata.getTimestamp())) {
			log.info("Ignore update since last sync time stamp is same");
			return;
		}

		timestamp = metadata.getTimestamp();

		attrToIndexMap.clear();
		// userCache.clear();

		int i = 0;
		for (String attribute : metadata.getAttributes()) {
			attrToIndexMap.put(attribute.toLowerCase(), i++);
		}

		log.info("User(s) Map size " + metadata.getUserInformationSize());

		attributesType = metadata.getAttributesType();

		for (String key : attributesType.keySet()) {
			log.debug("Attribute " + key + " has type of " + attributesType.get(key));
		}
		if (isInfiniSpanCache) {
			int index = 0;
			cleareapattrcache();
			for (String attribute : metadata.getAttributes()) {
				String type = metadata.getAttributesType().get(attribute);
				HashMap<String, Integer> eapattrmap = new HashMap<String, Integer>();
				eapattrmap.put(ATTRINDEX, index++);
				if ((type.toLowerCase()).startsWith("multi")) {
					eapattrmap.put(ATTRTYPE, 1);
				} else if ((type.toLowerCase()).startsWith("number")) {
					eapattrmap.put(ATTRTYPE, 2);
				} else {
					eapattrmap.put(ATTRTYPE, 0);
				}
				eapattrcache.put(attribute, eapattrmap);
			}
			log.debug("Infinispan EAP ATTR CACHE " + eapattrcache);
		}
	}

	private void cleareapattrcache() {
		for (String key : eapattrcache.keySet()) {
			eapattrcache.remove(key);
		}

	}

	private void loadFromDisk() throws IOException, FileNotFoundException {
		UserAttributesMetadataDTO metadata = new UserAttributesMetadataDTO();

		ObjectInput ois = null;
		long time = System.currentTimeMillis();
		try {
			ois = new ObjectInputStream(getMetadataInputStream());
			metadata = (UserAttributesMetadataDTO) ois.readObject();

			update(metadata);
			loadUsersToCache();
			log.debug("Data load from disk successfully");
		} catch (ClassNotFoundException e) {
			log.error("Cannot find MetadataDTO class. ", e);
		} finally {
			if (ois != null) {
				ois.close();
			}
		}
		log.info("Total time to load: " + (System.currentTimeMillis() - time) / 1000 + " seconds");
		metadata = null;
	}

	private void loadUsersToCache() {

		FileInputStream fileInput;
		try {
			fileInput = getUserInformationInputStream();
			byte[] sizeBytes = new byte[4];
			fileInput.read(sizeBytes, 0, 4);

			int size = ByteIntegerConverter.byteArrayToInt(sizeBytes);
			log.info("Disk User Size: " + size);

			for (int i = 0; i < size; i++) {
				loadUserInformation(fileInput, i);
			}

			fileInput.close();
		} catch (IOException e) {
			log.error("Not able to load users to cache");
		}

	}

	private void loadUserInformation(FileInputStream fileInput, int i) {

		try {
			ObjectInput objectInput = new ObjectInputStream(fileInput);
			UserInformationDTO userInformation = (UserInformationDTO) objectInput.readObject();

			IEvalValue[] results = userInformation.getValues()
					.toArray(new IEvalValue[userInformation.getValues().size()]);
			if(userInformation.getUserId()!=null){
				eapCache.put(userInformation.getUserId().toLowerCase(), convertToRMSCache(results));
				}
			// userCache.put(id, results);

		} catch (IOException e) {
			log.error("Unable to read user information for user " + i, e);
		} catch (ClassNotFoundException e) {
			log.error("Class not found " + i, e);
		}

	}

	private List<Object> convertToRMSCache(IEvalValue[] results) {
		List<Object> attrList = new ArrayList<Object>();
		Object[] object = new Object[attrToIndexMap.size()];
		Iterator<String> iterator = attrToIndexMap.keySet().iterator();
		while (iterator.hasNext()) {
			String attr = iterator.next();
			if (!attributesType.get(attr).startsWith("multi")) {
				object[attrToIndexMap.get(attr)] = results[attrToIndexMap.get(attr)].getValue();
			} else {
				List<String> sb = new ArrayList<String>();
				if (results[attrToIndexMap.get(attr)].getType() == ValueType.MULTIVAL) {
					IMultivalue mv = (IMultivalue) results[attrToIndexMap.get(attr)].getValue();
					Iterator<IEvalValue> ievalIterator = mv.iterator();
					while (ievalIterator.hasNext()) {
						IEvalValue v = ievalIterator.next();
						if (v == null) {
							sb.add("null");
						} else {
							sb.add((String) v.getValue());
						}
					}
				} else if (results[attrToIndexMap.get(attr)].getType() == ValueType.STRING) {
					sb.add((String) results[attrToIndexMap.get(attr)].getValue());
				} else if (results[attrToIndexMap.get(attr)].getType() == ValueType.LONG) {
					sb.add(Long.toString((long) results[attrToIndexMap.get(attr)].getValue()));
				}
				object[attrToIndexMap.get(attr)] = sb;
				log.debug("Attribute" + sb);
			}
		}
		attrList = Arrays.asList(object);
		return attrList;
	}

	private FileInputStream getMetadataInputStream() throws IOException, FileNotFoundException {
		return new FileInputStream(getMetadataFileName());
	}

	private FileInputStream getUserInformationInputStream() throws IOException, FileNotFoundException {
		return new FileInputStream(getUserInformationFileName());
	}

	private String getUserInformationFileName() {
		String sPath = findInstallFolder();
		return (sPath + "/userinformation.bin");
	}

	private String getMetadataFileName() {
		String sPath = findInstallFolder();
		return (sPath + "/usermetadata.bin");
	}

	private String[] currentUserIds() {
		return osWrapper.getLoggedInUsers();
	}

	public String findInstallFolder() {

		String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

		try {
			path = URLDecoder.decode(path, "UTF-8");

		} catch (Exception e) {
			log.error("Exeception while decoding the path:", e);
		}

		int endIndex = path.indexOf("jservice/jar");

		if (OSUtils.isWindows()) {
			path = path.substring(1, endIndex);
		} else {
			path = path.substring(0, endIndex);
		}
		return path;
	}
	
//	public BasicCache<String, List<Object>> getEapCache() {
//        return eapCache;
//    }
//
//    public BasicCache<String, HashMap<String, Integer>> getEapAttrCache() {
//        return eapattrcache;
//    }
//
//	public static void main(String[] args) {
//		
//		HOTROD_PROPS=new Properties();
//		HOTROD_PROPS.setProperty("infinispan.client.hotrod.server_list", "10.65.2.227:8081");
//		HOTROD_PROPS.setProperty("infinispan.client.hotrod.auth_username", "admin");
//		HOTROD_PROPS.setProperty("infinispan.client.hotrod.auth_password","123next!");
//		HOTROD_PROPS.setProperty("infinispan.client.hotrod.auth_realm", "default");
//		HOTROD_PROPS.setProperty("infinispan.client.hotrod.sasl_mechanism","SCRAM-SHA-512");
//		
//		UserAttributesClient uac=new UserAttributesClient();
//		uac.initializeCache(HOTROD_PROPS);
//		
//		IEvalValue obj1=EvalValue.build("test1");
//		IEvalValue obj2=EvalValue.build("test2");
//		ArrayList<Object> objList=new ArrayList<Object>();
//		objList.add(obj1);
//		objList.add(obj2);
//		uac.getEapCache().put("user1", objList);
//		
//		HashMap<String, Integer> map=new HashMap<String, Integer>();
//		map.put("aatre1",0);
//		uac.getEapAttrCache().put("attributename1", map);
//		HashMap<String, Integer> map1=new HashMap<String, Integer>();
//		map.put("aatre2",1);
//		uac.getEapAttrCache().put("attributename2", map);
//		
//		System.out.println("end");
//	}
}
