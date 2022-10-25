package com.nextlabs.plugins.userattributes.v2.helper;

public class ResponseElement {
	private byte[] responseArray;
	private long updateTime;
	
	public ResponseElement(byte[] responseArray, long updateTime){
		this.responseArray = responseArray;
		this.updateTime = updateTime;
	}
	
	public ResponseElement(){
		this.responseArray = new byte[0];
		this.updateTime = 0;
	}

	public byte[] getResponseArray() {
		return responseArray;
	}

	public long getUpdateTime() {
		return updateTime;
	}
	
	
}
