package com.nextlabs.plugins.userattributes.v2.dto;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.plugins.userattributes.v2.helper.ByteIntegerConverter;

public class UserAttributesUserInformationDTO implements Externalizable, Serializable {
	private static final Log LOG = LogFactory.getLog(UserAttributesUserInformationDTO.class);
	private static final long serialVersionUID = 1234562525789L;
	private int userInformationSize;
	private List<UserInformationDTO> userInformationList;

	public UserAttributesUserInformationDTO(){
		this.userInformationSize = 0;
		this.userInformationList = new ArrayList<UserInformationDTO>();
	}
	
	public long getUserInformationSize() {
		return userInformationSize;
	}

	public List<UserInformationDTO> getUserInformationList() {
		return userInformationList;
	}
	
	public void setUserInformationList(List<UserInformationDTO> userInformationList) {
		this.userInformationList = userInformationList;
		this.userInformationSize = userInformationList.size();
	}

	public byte[] toByteArray(){
		ByteArrayOutputStream output = null;
		
		try {
			output = new ByteArrayOutputStream();
			
			byte[] size = ByteIntegerConverter.intToByteArray(userInformationSize);
			output.write(size);
		
			for(UserInformationDTO userInformation: userInformationList){
				serializeUserInformation(output, userInformation);
			}
		} catch (IOException e) {
			LOG.error("Unable to convert to byte array" , e);
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				// Should not reach here
				LOG.error("Unable to close byte array output stream", e);
			}
		}
		
		return output.toByteArray();
	}

	private void serializeUserInformation(ByteArrayOutputStream output, UserInformationDTO userInformation) {
		ObjectOutput uiObjectOutput = null;
		
		try{
		uiObjectOutput = new ObjectOutputStream(output);
		
		// Convert to Array of Bytes
		uiObjectOutput.writeObject(userInformation);
		} catch (IOException e){
			LOG.error("Unable to convert user information to byte array" , e);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// Write size
		out.writeInt(userInformationSize);
		
		// Write data
		for(UserInformationDTO userInformation : userInformationList){
			out.writeObject(userInformation);
		}
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// Read size
		this.userInformationSize = in.readInt();
		
		// Read data
		for(long i = 0; i < this.userInformationSize; i++){
			UserInformationDTO userInformation = new UserInformationDTO();
			userInformation = (UserInformationDTO) in.readObject();
			
			this.userInformationList.add(userInformation);
		}
	}
	
	@Override
	public String toString(){
		// Get String of size (for test only)
		String value = String.format("User Information Size: %d", userInformationSize);
		return value;
	}
}
