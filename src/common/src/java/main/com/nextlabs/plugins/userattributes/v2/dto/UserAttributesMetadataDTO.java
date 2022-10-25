package com.nextlabs.plugins.userattributes.v2.dto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserAttributesMetadataDTO implements Externalizable, Serializable {
	private static final Log LOG = LogFactory.getLog(UserAttributesMetadataDTO.class);
	private static final long serialVersionUID = 12345634252578349L;
	private long timestamp;
	private List<String> attributes;
	private Map<String, String> attributesType;
	private long userInformationSize;

	public UserAttributesMetadataDTO() {
		this.timestamp = 0;
		this.attributes = new ArrayList<String>();
		this.attributesType = new HashMap<String, String>();
		this.userInformationSize = 0;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<String> attributes) {
		this.attributes.clear();
		for(String attribute : attributes){
			this.attributes.add(attribute.toLowerCase());
		}
	}

	public Map<String, String> getAttributesType() {
		return attributesType;
	}

	public void setAttributesType(Map<String, String> attributesType) {
		this.attributesType.clear();
		for(String key : attributesType.keySet()){
			this.attributesType.put(key.toLowerCase(), attributesType.get(key).toLowerCase());
		}
	}

	public long getUserInformationSize() {
		return userInformationSize;
	}

	public void setUserInformationSize(long userInformationSize) {
		this.userInformationSize = userInformationSize;
	}

	private void writeTimestamp(ObjectOutput out) throws IOException {
		out.writeLong(timestamp);
	}

	private void readTimestamp(ObjectInput in) throws IOException {
		this.timestamp = in.readLong();
	}

	private void writeAttributes(ObjectOutput out) throws IOException {
		out.writeLong(attributes.size());
		for (String attr : attributes) {
			out.writeUTF(attr);
		}
		LOG.debug("Attributes Size (Write): " + attributes.size());
	}

	private void readAttributes(ObjectInput in) throws IOException {
		long numAttrs = in.readLong();
	
		for (int i = 0; i < numAttrs; i++) {
			this.attributes.add(in.readUTF());
		}
		LOG.debug("Attributes Size (Read): " + numAttrs);
	}

	private void writeAttributesType(ObjectOutput out) throws IOException {
		out.writeLong(attributesType.size());
		for (String key : attributesType.keySet()) {
			out.writeUTF(key);
			out.writeUTF(attributesType.get(key));
		}
		LOG.debug("Attributes Types Size (Write): " + attributesType.size());
	}

	private void readAttributesType(ObjectInput in) throws IOException {
		long numAttrs = in.readLong();
	
		for (int i = 0; i < numAttrs; i++) {
			String key = in.readUTF();
			String value = in.readUTF();
			this.attributesType.put(key, value);
		}
		LOG.debug("Attributes Types Size (Read): " + attributesType.size());
	}

	private void writeUserInformationSize(ObjectOutput out) throws IOException {
		out.writeLong(userInformationSize);
		LOG.debug("User Information Size (Write): " + userInformationSize);
	}

	private void readUserInformationSize(ObjectInput in) throws IOException {
		this.userInformationSize = in.readLong();
		LOG.debug("User Information Size (Read): " + userInformationSize);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		try {
			// timestamp
			writeTimestamp(out);

			// attributes
			writeAttributes(out);

			// attributesType
			writeAttributesType(out);

			// userInformationSize
			writeUserInformationSize(out);
		} catch (IOException e) {
			LOG.error("Unable to write external", e);
			throw e;
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		try {
			// timestamp
			readTimestamp(in);

			// attributes
			readAttributes(in);

			// attributesType
			readAttributesType(in);

			// userInformationSize
			readUserInformationSize(in);
		} catch (IOException e) {
			LOG.error("Unable to read external", e);
			throw e;
		}
	}

	@Override
	public String toString() {
		String value = String.format("ts: %d, a: %s, at: %s, uis: %d", timestamp, attributes, attributesType,
				userInformationSize);
		return value;
	}

	public static void main(String[] args) {
		byte[] outputBytes = null;
		UserAttributesMetadataDTO metadatadto2 = new UserAttributesMetadataDTO();

		List<String> attributesList = new ArrayList<String>();
		Map<String, String> attributesTypeMap = new HashMap<String, String>();

		attributesList.add("firstName");
		attributesList.add("lastName");
		attributesList.add("multiValueTest");
		attributesList.add("postOfficeBox");
		attributesList.add("networkAddress");

		attributesTypeMap.put("lastName", "string");
		attributesTypeMap.put("multiValueTest", "multi-string");
		attributesTypeMap.put("unixId", "string");
		attributesTypeMap.put("networkAddress", "multi-string");
		attributesTypeMap.put("postOfficeBox", "multi-string");
		attributesTypeMap.put("firstName", "string");
		attributesTypeMap.put("windowsSid", "string");

		metadatadto2.setTimestamp(1479781813913L);
		metadatadto2.setAttributes(attributesList);
		metadatadto2.setAttributesType(attributesTypeMap);
		metadatadto2.setUserInformationSize(10);

		ByteArrayOutputStream output = null;
		ObjectOutput out = null;
		try {
			output = new ByteArrayOutputStream();
			out = new ObjectOutputStream(output);
			System.out.println(metadatadto2.toString());
			out.writeObject(metadatadto2);
			outputBytes = output.toByteArray();
			
			System.out.println("ByteArrayOutputStream size: " + output.size());
			System.out.println("ObjectOutputStream size: " + outputBytes.length);
			
			for(byte outputByte: outputBytes){
				System.out.print(String.format("%02x ", outputByte));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		ByteArrayInputStream input = null;
		ObjectInput in = null;
		try {
			input = new ByteArrayInputStream(outputBytes);
			in = new ObjectInputStream(input);
			Object obj = in.readObject();
			if(obj instanceof UserAttributesMetadataDTO){
				UserAttributesMetadataDTO newDTO = (UserAttributesMetadataDTO) obj; 
				System.out.println(newDTO);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		
	}
	
	
}
