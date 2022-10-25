package com.nextlabs.plugins.userattributes.v2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.destiny.container.dcc.plugin.IDCCHeartbeatServerPlugin;
import com.bluejungle.destiny.server.shared.configuration.IDABSComponentConfigurationDO;
import com.bluejungle.destiny.server.shared.configuration.ITrustedDomainsConfigurationDO;
import com.bluejungle.destiny.server.shared.registration.IRegisteredDCCComponent;
import com.bluejungle.destiny.server.shared.registration.ServerComponentType;
import com.bluejungle.dictionary.Dictionary;
import com.bluejungle.dictionary.DictionaryException;
import com.bluejungle.dictionary.ElementFieldData;
import com.bluejungle.dictionary.IDictionary;
import com.bluejungle.dictionary.IDictionaryIterator;
import com.bluejungle.dictionary.IElementField;
import com.bluejungle.dictionary.IElementType;
import com.bluejungle.dictionary.IEnrollment;
import com.bluejungle.domain.enrollment.ElementTypeEnumType;
import com.bluejungle.framework.comp.ComponentManagerFactory;
import com.bluejungle.framework.comp.IComponentManager;
import com.bluejungle.framework.configuration.DestinyConfigurationStoreImpl;
import com.bluejungle.framework.configuration.IDestinyConfigurationStore;
import com.bluejungle.framework.expressions.BooleanOp;
import com.bluejungle.framework.expressions.CompositePredicate;
import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.Multivalue;
import com.bluejungle.framework.expressions.PredicateConstants;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.utils.SerializationUtils;
import com.bluejungle.pf.destiny.lib.DictionaryHelper;
import com.bluejungle.pf.domain.destiny.common.ServerSpecManager;
import com.bluejungle.pf.domain.destiny.subject.SubjectAttribute;
import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.nextlabs.plugins.userattributes.v2.dto.UserAttributesCombinedDTO;
import com.nextlabs.plugins.userattributes.v2.dto.UserAttributesRequestDTO;
import com.nextlabs.plugins.userattributes.v2.dto.UserAttributesResponseDTO;
import com.nextlabs.plugins.userattributes.v2.helper.OSUtils;
import com.nextlabs.plugins.userattributes.v2.helper.PropertyLoader;
import com.nextlabs.plugins.userattributes.v2.helper.RequestType;
import com.nextlabs.plugins.userattributes.v2.helper.ResponseElement;
import com.nextlabs.plugins.userattributes.v2.helper.Utils;


public class UserAttributesServer implements IDCCHeartbeatServerPlugin {
	private static final Log log = LogFactory.getLog(UserAttributesServer.class.getName());
	public static Properties PLUGIN_PROPS = null;

	private List<String> attributes;
	private final List<String> invalidAttributes;
	private  IElementField[] elems;
	private ClassLoader classLoader = getClass().getClassLoader();
	private final IElementType userType;
	private final IComponentManager componentManager;

	private final IDictionary dictionary;
	private final DictionaryHelper dictionaryHelper;
	private final IPredicate userCondition;
	private static List<String> keyAttributes;
	private ConcurrentMap<String, ResponseElement> responseElements;
	public static final String SERVER_PROPS_FILE = "/config/UserAttributesServer.properties";

	public UserAttributesServer() {
		componentManager = ComponentManagerFactory.getComponentManager();

		dictionary = componentManager.getComponent(Dictionary.COMP_INFO);

		try {
			userType = dictionary.getType(ElementTypeEnumType.USER.getName());
		} catch (DictionaryException e) {
			throw new IllegalStateException("Unable to obtain user type from the dictionary.", e);
		}

		userCondition = dictionary.condition(userType);

		ServerSpecManager serverSpecManager = componentManager.getComponent(ServerSpecManager.COMP_INFO);
		dictionaryHelper = new DictionaryHelper(serverSpecManager, dictionary);

		keyAttributes = loadKeyAttribute();
		attributes = loadAttributes();
		invalidAttributes = new ArrayList<String>();
		responseElements = new ConcurrentHashMap<String, ResponseElement>();
		elems = createElements(attributes);

		for (String attribute : invalidAttributes) {
			log.debug("Attribute " + attribute + " is invalid and will be removed from the attribute list");
			attributes.remove(attribute);
		}

		log.debug("Initialization finished");
	}



	public void init(IRegisteredDCCComponent notUsed) {
		return;
	}

	public Serializable serviceHeartbeatRequest(String providerName, String requestData) {
		if (UserAttributesRequestDTO.PLUGIN.equals(providerName)) {
			Serializable data = SerializationUtils.unwrapSerialized(requestData,this.classLoader);

			if (data instanceof UserAttributesRequestDTO) {
				UserAttributesRequestDTO request = (UserAttributesRequestDTO) data;

				// Check to see if we have any attributes. The list should exist
				// and have more than two entries (the first two being the
				// various ids
				// we send back)
				if (attributes == null || attributes.size() <= keyAttributes.size()) {
					// No attributes
					log.warn("No attributes being configured, return null to client");
					return null;
				}

				long lastEnrollmentSyncTime = mostRecentChange();

				String[] users = request.getUserIds();
				RequestType requestType = request.getRequestType();

				try {
					if (requestType.equals(RequestType.ALL_USERS_FROM_ALL_DOMAINS)) {
						log.info("Get All Users from All Domains");
						return getAllUsers(request, lastEnrollmentSyncTime, true);
					} else if (requestType.equals(RequestType.ALL_USERS_FROM_CURRENT_DOMAIN)) {
						log.info("Get All Users from Current Domains");
						return getAllUsers(request, lastEnrollmentSyncTime, false);
					} else if (requestType.equals(RequestType.LOGGED_IN_USERS_FROM_CURRENT_DOMAIN)) {
						log.info("Get Logged In Users from Current Domains");
						return getUsers(lastEnrollmentSyncTime, users);
					} else {
						// Default to All Users from All Domains (Safety
						// precaution)
						log.info("Get All Users from All Domains");
						return getAllUsers(request, lastEnrollmentSyncTime, true);
					}
				} catch (DictionaryException e) {
					log.error("Dictionary exception when getting user attribute information", e);
					return null;
				}

			}
		}

		return null;
	}

	private List<String> loadKeyAttribute() {

		// reload properties file for any attribute changed
		String installloc = findInstallFolder();
		PLUGIN_PROPS = PropertyLoader
				.loadProperties(installloc + SERVER_PROPS_FILE);

		if (PLUGIN_PROPS == null) {
			log.error("Cannot load properties file");
		}

		List<String> keyAttributes = new ArrayList<String>();
		String keys = PLUGIN_PROPS.getProperty("key-attributes");

		if (keys == null) {
			log.error("Cannot load property key-attributes. Please check the properties file");
		} else {
			String[] keyArray = keys.split(",");
			for (String key : keyArray) {
				keyAttributes.add(key.trim());
				log.debug("Detect key attribute " + key.trim());
			}
		}

		return keyAttributes;
	}

	private List<String> loadAttributes() {

		// reload properties file for any attribute changed
		String installloc = findInstallFolder();
		PLUGIN_PROPS = PropertyLoader
				.loadProperties(installloc + SERVER_PROPS_FILE);

		List<String> attributes = new ArrayList<String>();

		// We need these so that we can identify the user associated with the
		// attributes
		attributes.addAll(keyAttributes);
		
		if (PLUGIN_PROPS == null) {
			log.error("Cannot load properties file");
			return attributes;
		}

		String attrs = PLUGIN_PROPS.getProperty("attributes");

		if (attrs == null) {
			log.error("Cannot load property attributes. Please check the properties file");
		} else {
			String[] attributeArray = attrs.split(",");
			for (String attr : attributeArray) {
				attributes.add(attr.trim());
				log.debug("Detect attribute " + attr.trim());
			}
		}

		return attributes;

	}

	private Serializable getUsers(long lastEnrollmentSyncTime, String[] users) throws DictionaryException {
		UserAttributesResponseDTO response = new UserAttributesResponseDTO(lastEnrollmentSyncTime,
				attributes.subList(keyAttributes.size(), attributes.size()));

		getUserInfoByUser(users, response);

		log.debug("AttributesType size is " + response.getAttributesType().size());

		UserAttributesCombinedDTO combinedResponse = new UserAttributesCombinedDTO(response);
		byte[] responseArray = buildZipResponse(combinedResponse);
		return (Serializable) responseArray;
	}

	private Serializable getAllUsers(UserAttributesRequestDTO request, long lastEnrollmentSyncTime,
			boolean allDomains) {

		if (request.upToDate(lastEnrollmentSyncTime) && request.upToDate(getLastModifedPropertyFileTimeStamp())) {
			// No changes
			log.info("UAP HB: All upToDate");
			return null;
		}
		log.info("UAP HB Rebuilding");
		String domain = request.getDomain();
		Collection<String> domainList = expandDomainList(domain);
		keyAttributes = loadKeyAttribute();
		attributes = loadAttributes();
		elems = createElements(attributes);
		String key = generateKey(allDomains, domainList);
		ResponseElement responseElement = load(key);

		long responseUpdateTimeLocal = responseElement.getUpdateTime();
		if (lastEnrollmentSyncTime <= responseUpdateTimeLocal) {
			byte[] responseArray = responseElement.getResponseArray();

			log.info("lastEnrollmentSyncTime <= responseUpdateTime");
			log.info("Length of data return is " + responseArray.length);

			return (Serializable) responseArray;
		} else {
			log.info("lastEnrollmentSyncTime > responseUpdateTime");

			UserAttributesResponseDTO response = new UserAttributesResponseDTO(lastEnrollmentSyncTime,
					attributes.subList(keyAttributes.size(), attributes.size()));

			try {
				log.debug("Domain of the request " + request.getDomain());
				getUserInfoByDomain(request.getDomain(), response, allDomains);
			} catch (DictionaryException e) {
				log.error("Dictionary exception when getting user attribute information", e);
				return null;
			}

			log.debug("AttributesType size is " + response.getAttributesType().size());

			UserAttributesCombinedDTO combinedResponse = new UserAttributesCombinedDTO(response);
			byte[] responseArray = buildZipResponse(combinedResponse);

			// Put to HashMap
			save(lastEnrollmentSyncTime, key, responseArray);

			log.info("Length of data return is " + responseArray.length);
			return (Serializable) responseArray;
		}
	}

	private long getLastModifedPropertyFileTimeStamp() {
		String installloc = findInstallFolder();
		File file=new File(installloc + SERVER_PROPS_FILE);
		if(file.exists())
		{
			return file.lastModified();
		}
		return 0;
	}



	private void save(long lastEnrollmentSyncTime, String key, byte[] responseArray) {
		ResponseElement newResponseElement = new ResponseElement(responseArray, lastEnrollmentSyncTime);
		responseElements.put(key, newResponseElement);
	}

	private ResponseElement load(String key) {
		ResponseElement responseElement = responseElements.get(key);
		if (responseElement == null) {
			log.debug("HashMap Entry not found, generating dummy ResponseElement");
			responseElement = new ResponseElement();
		}
		return responseElement;
	}

	private void getUserInfoByUser(String[] userIds, UserAttributesResponseDTO response) throws DictionaryException {
		List<IPredicate> userIdentityPredicates = new ArrayList<IPredicate>();

		for (String userId : userIds) {
			userIdentityPredicates.add(dictionaryHelper.toDictionaryPredicate(new Relation(RelationOp.EQUALS,
					(SubjectAttribute) SubjectAttribute.USER_UID, Constant.build(userId)), null, SubjectType.USER));
		}

		IPredicate condition = new CompositePredicate(BooleanOp.AND, userCondition,
				new CompositePredicate(BooleanOp.OR, userIdentityPredicates));

		IDictionaryIterator<ElementFieldData> userData = null;

		try {
			userData = dictionary.queryFields(elems, condition, dictionary.getLatestConsistentTime(), null, null);

			while (userData.hasNext()) {
				buildResponse(response, userData.next());
			}
		} finally {
			if (userData != null) {
				userData.close();
			}
		}
	}

	private void getUserInfoByDomain(String rootDomain, UserAttributesResponseDTO response, boolean allDomains)
			throws DictionaryException {
		Collection<String> domains = expandDomainList(rootDomain);

		List<IPredicate> domainPredicates = new ArrayList<IPredicate>(domains.size());

		for (String domain : domains) {
			IEnrollment enrollment = dictionary.getEnrollment(domain);
			if (enrollment != null) {
				domainPredicates.add(dictionary.condition(enrollment));
			}
		}

		IPredicate domainCondition;
		if (allDomains) {
			domainCondition = PredicateConstants.TRUE;
		} else if (domainPredicates.size() > 0) {
			domainCondition = new CompositePredicate(BooleanOp.OR, domainPredicates);
		} else {
			domainCondition = PredicateConstants.FALSE;
		}

		IDictionaryIterator<ElementFieldData> usersInDomain = null;

		try {
			usersInDomain = dictionary.queryFields(elems,
					new CompositePredicate(BooleanOp.AND, domainCondition, userCondition),
					dictionary.getLatestConsistentTime(), null, null);
			while (usersInDomain.hasNext()) {
				buildResponse(response, usersInDomain.next());
			}
		} finally {
			if (usersInDomain != null) {
				usersInDomain.close();
			}
		}
	}

	/*
	 * Copied nearly verbatim from PolicyQueryImpl.java
	 */
	private Collection<String> expandDomainList(String agentDomain) {
		if (agentDomain == null || agentDomain.length() == 0) {
			log.warn("Agent domain is null - using an empty domain list.");
			return new ArrayList<String>();
		}
		SortedSet<String> res = new TreeSet<String>();
		res.add(agentDomain);
		try {
			IDestinyConfigurationStore confStore = (IDestinyConfigurationStore) componentManager
					.getComponent(DestinyConfigurationStoreImpl.COMP_INFO);
			IDABSComponentConfigurationDO dabsConfig = (IDABSComponentConfigurationDO) confStore
					.retrieveComponentConfiguration(ServerComponentType.DABS.getName());
			if (dabsConfig != null) {
				ITrustedDomainsConfigurationDO tdConfig = dabsConfig.getTrustedDomainsConfiguration();
				if (tdConfig != null) {
					String[] trustedDomains = tdConfig.getTrustedDomains();
					if (trustedDomains != null) {
						for (int i = 0; i != trustedDomains.length; i++) {
							if (trustedDomains[i] != null) {
								String[] domains = trustedDomains[i].split(",");
								for (int j = 0; j != domains.length; j++) {
									domains[j] = domains[j].trim();
								}
								if (Arrays.asList(domains).contains(agentDomain)) {
									log.info("Domain '" + agentDomain + "' is in the list of mutual trust: "
											+ trustedDomains[i]);
									for (int j = 0; j != domains.length; j++) {
										res.add(domains[j]);
									}
								}
							}
						}
					}
				} else {
					log.warn("Unable to get trusted domain configuration.");
				}
			} else {
				log.warn("Unable to get DABS configuration - trusted domains will not be configured.");
			}
		} catch (Exception ignored) {
			log.warn("Exception getting a list of trusted domains", ignored);
		}
		if (log.isInfoEnabled()) {
			StringBuffer msg = new StringBuffer("Effective list of domains: ");
			boolean first = true;
			for (String domain : res) {
				if (!first) {
					msg.append(", ");
				} else {
					first = false;
				}
				msg.append(domain);
				if (agentDomain.equals(domain)) {
					msg.append("(REQUESTED)");
				}
			}
			log.info(msg);
		}
		return res;
	}

	private void buildResponse(UserAttributesResponseDTO response, ElementFieldData data) {
		Object[] values = data.getData();

		List<IEvalValue> attributeValues = new ArrayList<IEvalValue>();

		for (int i = keyAttributes.size(); i < values.length; i++) {
			IEvalValue v;

			if (values[i] instanceof String || values[i] == null) {
				v = EvalValue.build((String) values[i]);
			} else if (values[i] instanceof Long) {
				v = EvalValue.build((Long) values[i]);
			} else if (values[i] instanceof String[]) {
				v = EvalValue.build(Multivalue.create((String[]) values[i]));
			} else if (values[i] instanceof Long[]) {
				v = EvalValue.build(Multivalue.create((Long[]) values[i]));
			} else {
				throw new IllegalArgumentException("Element " + values[i] + " of unexpected type");
			}

			attributeValues.add(v);
		}

		for (int i = 0; i < keyAttributes.size(); i++) {
			if (values[i] != null) {
				response.addUserInfo((String) values[i], attributeValues);
			}
		}

		Map<String, String> attributesType = response.getAttributesType();

		IElementField[] fields = data.getFields();
		for (IElementField field : fields) {
			if (!attributesType.containsKey(field.getName())) {
				attributesType.put(field.getName(), field.getType().getName());
				log.debug("Attribute " + field.getName() + " has type of " + field.getType().getName());
			}
		}
	}

	private byte[] buildZipResponse(UserAttributesCombinedDTO combinedDto) {
		String sPath = findInstallFolder();
		String metadataFileName = sPath +"/usermetadata.bin";
		String userinformationFileName = sPath +"/userinformation.bin";
		String zipFileName = sPath +"/userattributes.zip";
		// Save Metadata
		saveFile(combinedDto.getRawMetadata(), metadataFileName);

		// Save User Information
		saveFile(combinedDto.getRawUserInformation(), userinformationFileName);

		// Zip
		List<String> fileNames = new ArrayList<String>();
		fileNames.add(metadataFileName);
		fileNames.add(userinformationFileName);
		Utils.compressFiles(fileNames, zipFileName);

		// Read Zip File
		byte[] data = readFile(zipFileName);

		return data;
	}

	private String generateKey(boolean allDomains, Collection<String> domainCollection) {
		if (allDomains) {
			return "all_domains";
		} else {
			// Create Sorted List
			List<String> domains = new ArrayList<String>(domainCollection);
			Collections.sort(domains);
			StringBuilder stringBuilder = new StringBuilder();

			// Generate String Key from Sorted Domain List
			boolean isFirst = true;
			for (int i = 0; i < domains.size(); i++) {
				String domain = domains.get(i);
				if (!isFirst) {
					stringBuilder.append(",");
				}
				stringBuilder.append(domain);
				isFirst = false;
			}

			String key = stringBuilder.toString();
			log.info("key: " + key);
			return key;
		}

	}

	private byte[] readFile(String fileName) {
		byte[] data = null;
		FileInputStream fileInputStream = null;
		ByteArrayOutputStream byteArrayOutputStream = null;

		try {
			fileInputStream = new FileInputStream(fileName);
			byteArrayOutputStream = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int count = 0;
			while (count > -1) {
				count = fileInputStream.read(buffer, 0, 1024);
				byteArrayOutputStream.write(buffer);
			}

			data = byteArrayOutputStream.toByteArray();
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException", e);
		} catch (IOException e) {
			log.error("IOException", e);
		} finally {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				log.error("Unable to close file input stream", e);
			}
			try {
				byteArrayOutputStream.close();
			} catch (IOException e) {
				log.error("Unable to close byte array output stream", e);
			}
		}

		return data;
	}

	private void saveFile(byte[] data, String fileName) {
		FileOutputStream fileOutputStream = null;
				
		try {
			fileOutputStream = new FileOutputStream(fileName);
			fileOutputStream.write(data);
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException", e);
		} catch (IOException e) {
			log.error("IOException", e);
		} finally {
			try {
				fileOutputStream.close();
			} catch (IOException e) {
				// Should not reach here
				log.error("unable to close file output stream", e);
			}
		}
	}

	private IElementField[] createElements(List<String> attributes) {
		if (attributes == null) {
			return new IElementField[0];
		}

		// Sanity check
		String[] validFieldNames = userType.getFieldNames();

		List<IElementField> elems = new ArrayList<IElementField>();

		for (String attribute : attributes) {
			if (isValidAttribute(attribute, validFieldNames)) {
				elems.add(userType.getField(attribute));
			} else {
				log.warn("Attribute [" + attribute + "] is not known. Marked as invalid attribute");
			}
		}

		return elems.toArray(new IElementField[elems.size()]);
	}

	private boolean isValidAttribute(String attribute, String[] validFieldNames) {
		if (attribute == null) {
			log.debug("Attribute is null");
			return false;
		}

		for (String validFieldName : validFieldNames) {
			if (attribute.equals(validFieldName)) {
				log.debug("Attribute " + attribute + " is valid");
				return true;
			}
		}

		log.debug("Attribute " + attribute + " is not valid");
		invalidAttributes.add(attribute);

		return false;
	}

	private long mostRecentChange() {
		long dictionaryChange = -1;
		try {
			dictionaryChange = dictionary.getLatestConsistentTime().getTime();
		} catch (DictionaryException e) {
		}

		long fileChange = new File(getAttributesDataFileName()).lastModified();

		return Math.max(dictionaryChange, fileChange);
	}

	private String getAttributesDataFileName() {
		String installloc = findInstallFolder();
		String attributeDataFileName = null;
		attributeDataFileName = installloc + SERVER_PROPS_FILE;

		log.debug("Attribute data file name is at " + attributeDataFileName);
		return attributeDataFileName;
	}

	/* This method use to find the root install folder of the policy server */
	
	public String findInstallFolder() {

		String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

		try {
			path = URLDecoder.decode(path, "UTF-8");

		} catch (Exception e) {
			log.error("Exeception while decoding the path:", e);
		}

		int endIndex = path.indexOf("/server/plugins") + "/server/plugins".length();

		if (OSUtils.isWindows()) {
			path = path.substring(1, endIndex);
		} else {
			path = path.substring(0, endIndex);
		}
		return path;
	}
}
