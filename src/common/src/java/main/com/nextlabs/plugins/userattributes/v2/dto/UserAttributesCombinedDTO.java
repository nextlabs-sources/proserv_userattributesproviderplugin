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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.plugins.userattributes.v2.dto.UserAttributesResponseDTO.IDAndValues;
import com.nextlabs.plugins.userattributes.v2.helper.ByteIntegerConverter;

public class UserAttributesCombinedDTO implements Externalizable, Serializable {
	private static final Log LOG = LogFactory.getLog(UserAttributesCombinedDTO.class);
	private static final long serialVersionUID = 1234563436622525789L;
	private byte[] metadata;
	private byte[] userInformation;

	public UserAttributesCombinedDTO() {
		this.metadata = new byte[0];
		this.userInformation = new byte[0];
	}

	public UserAttributesCombinedDTO(UserAttributesResponseDTO response) {
		UserAttributesMetadataDTO metadataDTO = new UserAttributesMetadataDTO();
		UserAttributesUserInformationDTO userInformationDTO = new UserAttributesUserInformationDTO();

		// Metadata
		metadataDTO.setTimestamp(response.getTimestamp());
		metadataDTO.setAttributes(response.getAttributes());
		metadataDTO.setAttributesType(response.getAttributesType());
		metadataDTO.setUserInformationSize(response.getUserInformation().size());

		// User Information
		List<UserInformationDTO> userInformationList = convertUserInformationFromResponse(
				response.getUserInformation());
		userInformationDTO.setUserInformationList(userInformationList);

		setMetadata(metadataDTO);
		setUserInformation(userInformationDTO);

		LOG.debug("Metadata info: " + metadataDTO.toString());
		LOG.debug("UserInformation info: " + userInformationDTO.toString());
	}

	public UserAttributesMetadataDTO getMetadata() {
		UserAttributesMetadataDTO metadataDTO = new UserAttributesMetadataDTO();

		ByteArrayInputStream byteArrayInputStream = null;
		ObjectInputStream objectInputStream = null;
		try {
			LOG.debug("MetadataSize: " + this.metadata.length);
			byteArrayInputStream = new ByteArrayInputStream(this.metadata);
			objectInputStream = new ObjectInputStream(byteArrayInputStream);
			metadataDTO = (UserAttributesMetadataDTO) objectInputStream.readObject();
		} catch (IOException ex) {
			LOG.error("IOException: Unable to convert bytes to object " + ex.getMessage(), ex);
		} catch (ClassNotFoundException e) {
			LOG.error("ClassNotFoundException: Unable to convert bytes to object " + e.getMessage(), e);
		} finally {
			// Close Object Input Stream
			try {
				byteArrayInputStream.close();
			} catch (IOException e) {
				// Should not reach here
				LOG.error("Unable to close byte array input stream", e);
			}

			// Close Byte Array Input Stream
			try {
				objectInputStream.close();
			} catch (IOException e) {
				// Should not reach here
				LOG.error("Unable to close obejct input stream", e);
			}
		}

		return metadataDTO;
	}

	public void setMetadata(UserAttributesMetadataDTO metadataDTO) {
		ByteArrayOutputStream byteArrayOutputStream = null;
		ObjectOutputStream objectOutputStream = null;

		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(metadataDTO);

			this.metadata = byteArrayOutputStream.toByteArray();
			LOG.debug("MetadataSize: " + this.metadata.length);
		} catch (IOException e) {
			LOG.error("unable to serialize metadata, resetting to zero size byte array", e);
			this.userInformation = new byte[0];
		} finally {
			// Close Object Output Stream
			try {
				byteArrayOutputStream.close();
			} catch (IOException e) {
				// Should not reach here
				LOG.error("Unable to close byte array output stream", e);
			}

			// Close Byte Array Output Stream
			try {
				objectOutputStream.close();
			} catch (IOException e) {
				// Should not reach here
				LOG.error("Unable to close object output stream", e);
			}
		}
	}

	public UserAttributesUserInformationDTO getUserInformation() {
		UserAttributesUserInformationDTO userInformationDTO = new UserAttributesUserInformationDTO();

		ByteArrayInputStream byteArrayInputStream = null;
		ObjectInputStream objectInputStream = null;
		try {
			LOG.debug("User Information Size: " + this.userInformation.length);
			byteArrayInputStream = new ByteArrayInputStream(this.userInformation);
			objectInputStream = new ObjectInputStream(byteArrayInputStream);
			userInformationDTO = (UserAttributesUserInformationDTO) objectInputStream.readObject();
		} catch (IOException ex) {
			LOG.error("IOException: Unable to convert bytes to object " + ex.getMessage(), ex);
		} catch (ClassNotFoundException e) {
			LOG.error("ClassNotFoundException: Unable to convert bytes to object " + e.getMessage(), e);
		} finally {
			// Close Object Input Stream
			try {
				byteArrayInputStream.close();
			} catch (IOException e) {
				// Should not reach here
				LOG.error("Unable to close byte array input stream", e);
			}

			// Close Byte Array Input Stream
			try {
				objectInputStream.close();
			} catch (IOException e) {
				// Should not reach here
				LOG.error("Unable to close object input stream", e);
			}
		}

		return userInformationDTO;
	}

	public void setUserInformation(UserAttributesUserInformationDTO userInformationDTO) {
		byte[] uiBytes = userInformationDTO.toByteArray();
		this.userInformation = uiBytes;
		
		LOG.debug("User Size: " + ByteIntegerConverter.byteArrayToInt(uiBytes));
	}

	public byte[] getRawMetadata() {
		return this.metadata;
	}

	public byte[] getRawUserInformation() {
		return this.userInformation;
	}

	public void writeExternal(ObjectOutput metadataOut, ObjectOutput dataOut) throws IOException {
		metadataOut.write(this.metadata);
		dataOut.write(this.userInformation);
	}

	private List<UserInformationDTO> convertUserInformationFromResponse(List<IDAndValues> responseUserInformationList) {
		List<UserInformationDTO> userInformationList = new ArrayList<UserInformationDTO>();

		for (IDAndValues responseUserInformation : responseUserInformationList) {
			UserInformationDTO userInformation = new UserInformationDTO();
			userInformation.setUserId(responseUserInformation.getUserId());
			userInformation.setValues(responseUserInformation.getValues());

			userInformationList.add(userInformation);
		}

		return userInformationList;

	}

	@Override
	public String toString() {
		// Print size
		String value = String.format("metadata: %d, userinformation: %d", metadata.length, userInformation.length);
		return value;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// Metadata
		out.writeInt(this.metadata.length);
		LOG.debug("Metadata Length (write): " + metadata.length);
		out.write(this.metadata);

		// User Information
		out.writeInt(this.userInformation.length);
		LOG.debug("User Information Length (write): " + userInformation.length);
		out.write(this.userInformation);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// Metadata
		int metadataLength = in.readInt();
		LOG.debug("Metadata Expected Length (Read): " + metadataLength);
		byte[] metadataValue = new byte[metadataLength];
		in.readFully(metadataValue, 0, metadataLength);
		this.metadata = metadataValue;
		LOG.debug("Metadata Actual Length (Read): " + this.metadata.length);

		// User Information
		int userInformationLength = in.readInt();
		LOG.debug("User Information Expected Length (Read): " + userInformationLength);
		byte[] userInformationValue = new byte[userInformationLength];
		in.readFully(userInformationValue, 0, userInformationLength);
		this.userInformation = userInformationValue;
		LOG.debug("User Information Actual Length (Read): " + this.userInformation.length);
	}
}
