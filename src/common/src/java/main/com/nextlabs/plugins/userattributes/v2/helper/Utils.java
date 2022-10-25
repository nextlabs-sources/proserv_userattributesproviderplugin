package com.nextlabs.plugins.userattributes.v2.helper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.nextlabs.plugins.userattributes.v2.dto.UserAttributesResponseDTO;

public class Utils {

	private static final Log LOG = LogFactory.getLog(Utils.class);

	public static void writeData(Object data, String path) throws IOException {

		File file = new File(path);

		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();

		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(data);
		} finally {
			if (out != null) {
				out.close();
			}
		}

	}

	public static Object readData(String path) {

		File file = new File(path);
		Object obj = null;

		try {
			if (file.exists()) {
				FileInputStream fin = new FileInputStream(path);
				ObjectInputStream ois = new ObjectInputStream(fin);
				obj = ois.readObject();

				ois.close();
				fin.close();
			}
		} catch (IOException e) {
			LOG.error(" Utils readData() error: ", e);
			obj = null;
		} catch (ClassNotFoundException e) {
			LOG.error(" Utils readData() error: ", e);
			obj = null;
		}

		return obj;

	}

	public static byte[] compressDataOld(Object data) {
		byte[] result = null;

		// convert data to byte array to compress
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] originalData = null;

		try {
			out = new ObjectOutputStream(outputStream);
			out.writeObject(data);
			originalData = outputStream.toByteArray();
		} catch (Exception e) {
			LOG.error("Unable to convert original data to byte array " + e.getMessage(), e);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				outputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		Deflater deflater = new Deflater();
		deflater.setInput(originalData);

		ByteArrayOutputStream compressedStream = new ByteArrayOutputStream(originalData.length);
		deflater.finish();
		byte[] buffer = new byte[1024];

		try {
			while (!deflater.finished()) {
				int count = deflater.deflate(buffer);
				compressedStream.write(buffer, 0, count);
			}
		} catch (Exception e) {
			LOG.error("Unable to compress the original data " + e.getMessage(), e);
		} finally {
			try {
				compressedStream.close();
			} catch (IOException e) {
				// ignore close exception
			}
		}

		result = compressedStream.toByteArray();
		LOG.info("Original: " + originalData.length + " bytes");
		LOG.info("Compressed: " + result.length + " bytes");

		return result;

	}

	public static byte[] compressData(Object data) {
		ByteArrayOutputStream byteArrayOutputStream = null;
		GZIPOutputStream gzipOutputStream = null;
		ObjectOutput objectOutputStream = null;
		byte[] compressedData = null;

		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			objectOutputStream = new ObjectOutputStream(gzipOutputStream);
			objectOutputStream.writeObject(data);

			objectOutputStream.close();

			compressedData = byteArrayOutputStream.toByteArray();
		} catch (Exception e) {
			LOG.error("Unable to convert original data to byte array " + e.getMessage(), e);
		} finally {
			try {
				objectOutputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				gzipOutputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				byteArrayOutputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}
		return compressedData;
	}

	public static Object decompressData(byte[] data) {
		ByteArrayInputStream byteArrayInputStream = null;
		GZIPInputStream gzipInputStream = null;
		ObjectInput objectInputStream = null;
		Object obj = null;

		try {
			byteArrayInputStream = new ByteArrayInputStream(data);
			gzipInputStream = new GZIPInputStream(byteArrayInputStream);
			objectInputStream = new ObjectInputStream(gzipInputStream);

			obj = objectInputStream.readObject();

			objectInputStream.close();
		} catch (IOException e) {
			LOG.error("IOException", e);
		} catch (ClassNotFoundException e) {
			LOG.error("ClassNotFoundException", e);
			e.printStackTrace();
		} finally {
			try {
				objectInputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}

			try {
				gzipInputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}

			try {
				byteArrayInputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}
		return obj;
	}

	public static Object decompressDataOld(byte[] data) {
		Inflater inflater = new Inflater();
		inflater.setInput(data);

		LOG.info("Data Length: " + data.length);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

		byte[] buffer = new byte[1024];

		try {
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				outputStream.write(buffer, 0, count);
			}
		} catch (Exception e) {
			LOG.error("Unable to decomporess the data " + e.getMessage(), e);
		} finally {
			try {
				outputStream.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		byte[] output = outputStream.toByteArray();
		LOG.debug("Compressed: " + data.length);
		LOG.debug("Original: " + output.length);

		Object obj = null;
		ByteArrayInputStream bis = null;
		ObjectInputStream ois = null;
		try {
			bis = new ByteArrayInputStream(output);
			ois = new ObjectInputStream(bis);
			obj = ois.readObject();
		} catch (IOException ex) {
			LOG.error("Unable to convert bytes to object " + ex.getMessage(), ex);
		} catch (ClassNotFoundException e) {
			LOG.error("Unable to convert bytes to object " + e.getMessage(), e);
		}

		return obj;
	}

	public static void compressFiles(List<String> entries, String zipFile) {

		byte[] buffer = new byte[1024];

		FileOutputStream fos = null;
		ZipOutputStream zos = null;

		try {
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);

			for (String zipEntry : entries) {
				File f = new File(zipEntry);
				ZipEntry ze = new ZipEntry(f.getName());
				zos.putNextEntry(ze);
				FileInputStream in = new FileInputStream(f);

				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}

				in.close();
				zos.closeEntry();
			}
		} catch (IOException e) {
			LOG.error(" Utils compressFiles() error: ", e);
		} finally {
			if (zos != null) {
				try {
					zos.close();
				} catch (IOException ignore) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ignore) {
				}
			}
		}

	}

	public static void uncompressFiles(String extractTo, File zipFile) {

		ZipFile zip = null;

		try {
			zip = new ZipFile(zipFile);
			Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				File file = new File(extractTo, entry.getName());
				InputStream in = zip.getInputStream(entry);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
				byte[] buffer = new byte[8192];
				int read;
				while (-1 != (read = in.read(buffer))) {
					out.write(buffer, 0, read);
				}

				in.close();
				out.close();
			}
			zipFile.delete();

		} catch (ZipException e) {
			LOG.error("Exception in uncompressing the file:", e);
		} catch (IOException e) {
			LOG.error("Exception in uncompressing the file:", e);
		} finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException ignore) {
				}
			}
		}
	}

	public static Timestamp getFileModifiedDate(String path) {

		File file = new File(path);

		Timestamp modifiedDate = null;

		if (file.exists()) {

			modifiedDate = new Timestamp(file.lastModified());

		}

		return modifiedDate;

	}

	public static void main(String[] args) {
		List<String> attributes = new ArrayList<String>();
		List<IEvalValue> attributesValue = new ArrayList<IEvalValue>();
		for (int i = 0; i < 10; i++) {
			attributes.add("attr" + i);
			attributesValue.add(EvalValue.build("some random string" + Math.random()));
		}
		UserAttributesResponseDTO response = new UserAttributesResponseDTO(Long.parseLong("2342342"), attributes);

		response.addUserInfo("abraham lincoln", attributesValue);
		Utils.compressData(response);
	}

}
