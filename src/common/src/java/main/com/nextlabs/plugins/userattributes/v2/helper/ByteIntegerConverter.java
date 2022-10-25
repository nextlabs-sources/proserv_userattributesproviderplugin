package com.nextlabs.plugins.userattributes.v2.helper;

import java.nio.ByteBuffer;

public class ByteIntegerConverter {

	public static byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static int byteArrayToInt(byte[] bytes) {
		int value = 0;

		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		value = buffer.getInt();
		return value;
	}
	
	public static void main(String[] args){
		for(int i = 1; i < 1000000000; i*=10){
			if(!test(i)){
				System.out.println("Fail at: " + i);
			}
		}
		System.out.println("Done");
	}
	
	public static boolean test(int value){
		return value == byteArrayToInt(intToByteArray(value));
	}
}
