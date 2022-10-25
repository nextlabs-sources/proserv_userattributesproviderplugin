/*
 * Created on Jun 11, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id$:
 */
package com.nextlabs.plugins.userattributes.v2.dto;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import com.nextlabs.plugins.userattributes.v2.helper.RequestType;

public class UserAttributesRequestDTO implements Externalizable, Serializable {
	public static final String PLUGIN = "UserAttributesPluginV2";
	private static final long serialVersionUID = 1234563434352525789L;
	private long timestamp;
	private String hostname;
	private String[] userIds;
	private RequestType requestType;

	public UserAttributesRequestDTO() {
	}

	public UserAttributesRequestDTO(long timestamp, String hostname, String[] userIds, RequestType requestType) {
		this.timestamp = timestamp;
		this.hostname = hostname;
		this.userIds = userIds;
		this.requestType = requestType;
	}

	public boolean upToDate(long lastChange) {
		return lastChange <= timestamp;
	}

	public String getDomain() {
		if (hostname != null) {
			String[] nameAndDomain = hostname.split("[.]", 2);
			if (nameAndDomain.length == 2) {
				return nameAndDomain[1];
			}
		}

		return null;
	}

	public String[] getUserIds() {
		return userIds;
	}

	public RequestType getRequestType() {
		return requestType;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		timestamp = in.readLong();
		hostname = in.readUTF();
		requestType = (RequestType) in.readObject();

		int numUsers = in.readInt();
		userIds = new String[numUsers];
		for (int i = 0; i < numUsers; i++) {
			userIds[i] = in.readUTF();
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(timestamp);
		out.writeUTF(hostname);
		out.writeObject(requestType);

		out.writeInt(userIds.length);
		for (String userId : userIds) {
			out.writeUTF(userId);
		}

	}
}
