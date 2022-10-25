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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IMultivalue;
import com.bluejungle.framework.expressions.Multivalue;
import com.bluejungle.framework.expressions.ValueType;

public class UserAttributesResponseDTO implements Externalizable, Serializable {
	private long timestamp;
	private List<String> attributes;
	private Map<String, String> attributesType;
	private List<IDAndValues> userInformation;
	private static final Log log = LogFactory.getLog(UserAttributesResponseDTO.class.getName());
	private static final long serialVersionUID = 12345634342525789L;
	public static class IDAndValues {
		private String userId;
		private List<IEvalValue> values;

		public IDAndValues() {
			userId = null;
			values = new ArrayList<IEvalValue>();
		}

		public IDAndValues(String userId, List<IEvalValue> values) {
			this.userId = userId;
			this.values = values;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public String getUserId() {
			return userId;
		}

		public void setValues(List<IEvalValue> values) {
			this.values = values;
		}

		public List<IEvalValue> getValues() {
			return values;
		}
	}

	public UserAttributesResponseDTO() {
		attributes = new ArrayList<String>();
		attributesType = new HashMap<String, String>();
		userInformation = new ArrayList<IDAndValues>();
	}

	public UserAttributesResponseDTO(long timestamp, List<String> attributes) {
		this.timestamp = timestamp;
		this.attributes = attributes;
		this.attributesType = new HashMap<String, String>();
		userInformation = new ArrayList<IDAndValues>();
	}

	public void addUserInfo(String userId, List<IEvalValue> attributeValues) {
		if (userId == null) {
			throw new NullPointerException("userId");
		}

		if (attributeValues == null) {
			throw new NullPointerException("attributeValues");
		}

		if (attributeValues.size() != attributes.size()) {
			throw new IllegalArgumentException("Mismatch between number of arguments (" + attributes.size()
					+ ") and number of values (" + attributeValues.size() + ")\n");
		}

		IDAndValues idv = new IDAndValues();
		idv.userId = userId;
		idv.values = attributeValues;

		userInformation.add(idv);
	}

	public void setAttributesType(Map<String, String> attributesType) {
		this.attributesType = attributesType;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public List<IDAndValues> getUserInformation() {
		return userInformation;
	}

	public Map<String, String> getAttributesType() {
		return attributesType;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException {
		timestamp = in.readLong();

		readKeys(in);

		readUserInformation(in);

		readAttributesType(in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(timestamp);

		writeKeys(out);

		writeUserInformation(out);

		writeAttributesType(out);
	}

	private void readKeys(ObjectInput in) throws IOException {
		long numAttrs = in.readLong();

		for (int i = 0; i < numAttrs; i++) {
			attributes.add(in.readUTF());
		}
	}

	private void writeKeys(ObjectOutput out) throws IOException {
		out.writeLong(attributes.size());
		for (String attr : attributes) {
			out.writeUTF(attr);
		}
	}

	private void readUserInformation(ObjectInput in) throws IOException {
		long numUsers = in.readLong();
		for (long i = 0; i < numUsers; i++) {
			readSingleUserInformation(in);
		}
	}

	private void readAttributesType(ObjectInput in) throws IOException {
		long numAttrs = in.readLong();

		for (int i = 0; i < numAttrs; i++) {
			String key = in.readUTF();
			String value = in.readUTF();
			attributesType.put(key, value);
		}
	}

	private void writeAttributesType(ObjectOutput out) throws IOException {

		out.writeLong(attributesType.size());
		for (String key : attributesType.keySet()) {
			out.writeUTF(key);
			out.writeUTF(attributesType.get(key));
		}
	}

	private void writeUserInformation(ObjectOutput out) throws IOException {
		out.writeLong(userInformation.size());
		for (IDAndValues id : userInformation) {
			writeSingleUserInformation(out, id);
		}
	}

	private void readSingleUserInformation(ObjectInput in) throws IOException {
		String id = in.readUTF();
		List<IEvalValue> values = readUserAttributeValues(in);
		addUserInfo(id, values);
	}

	private void writeSingleUserInformation(ObjectOutput out, IDAndValues id) throws IOException {
		out.writeUTF(id.getUserId());
		writeUserAttributeValues(out, id);
	}

	private List<IEvalValue> readUserAttributeValues(ObjectInput in) throws IOException {
		List<IEvalValue> values = new ArrayList<IEvalValue>();
		long numValues = in.readLong();
		for (long j = 0; j < numValues; j++) {
			values.add(readUserAttributeValue(in));
		}

		return values;
	}

	private void writeUserAttributeValues(ObjectOutput out, IDAndValues id) throws IOException {
		out.writeLong(id.getValues().size());
		for (IEvalValue value : id.getValues()) {
			writeUserAttributeValue(out, value);
		}
	}

	private IEvalValue readUserAttributeValue(ObjectInput in) throws IOException {
		char type = in.readChar();
		IEvalValue v = IEvalValue.NULL;

		switch (type) {
		case 'L':
			v = EvalValue.build(in.readLong());
			break;
		case 'S':
			v = EvalValue.build(in.readUTF());
			break;
		case 'N':
			v = IEvalValue.NULL;
			break;
		case '[':
			int size = in.readInt();
			char subtype = in.readChar();

			switch (subtype) {
			case 'L':
				List<Long> l = new ArrayList<Long>();
				for (int i = 0; i < size; i++) {
					l.add(in.readLong());
				}
				v = EvalValue.build(Multivalue.create(l, ValueType.LONG));
				break;
			case 'S':
				List<String> s = new ArrayList<String>();
				for (int i = 0; i < size; i++) {
					s.add(in.readUTF());
				}
				v = EvalValue.build(Multivalue.create(s, ValueType.STRING));
				break;
			case 'N':
				v = EvalValue.build(IMultivalue.EMPTY);
				break;
			default:
				log.error("Unsupported value code " + subtype);
				throw new IllegalArgumentException("Unsupported value code " + subtype);
			}
			break;
		default:
			log.error("Unsupported value code " + type);
			throw new IllegalArgumentException("Unsupported value code " + type);
		}

		return v;
	}

	private static final IEvalValue EMPTY_STRING = EvalValue.build("");

	private void writeUserAttributeValue(ObjectOutput out, IEvalValue value) throws IOException {
		// Make some edge cases easier to handle
		if (value == null || value == IEvalValue.NULL || value == IEvalValue.EMPTY) {
			value = EMPTY_STRING;
		}

		ValueType vt = value.getType();

		if (vt == ValueType.LONG) {
			out.writeChar('L');
			out.writeLong((Long) value.getValue());
		} else if (vt == ValueType.STRING) {
			out.writeChar('S');
			out.writeUTF((String) value.getValue());
		} else if (vt == ValueType.NULL) {
			out.writeChar('N');
		} else if (vt == ValueType.MULTIVAL) {
			out.writeChar('[');
			IMultivalue mv = (IMultivalue) value.getValue();

			out.writeInt(mv.size());

			ValueType mvt = mv.getType();

			if (mvt == ValueType.LONG) {
				out.writeChar('L');

				for (IEvalValue e : mv) {
					out.writeLong((Long) e.getValue());
				}
			} else if (mvt == ValueType.STRING) {
				out.writeChar('S');

				for (IEvalValue e : mv) {
					out.writeUTF((String) e.getValue());
				}
			} else if (mvt == ValueType.NULL) {
				out.writeChar('N');
			} else {
				log.error("Unsupported value type " + mvt);
				throw new IllegalArgumentException("Unsupported value type " + mvt);
			}
		} else {
			log.error("Unsupported value type " + vt);
			throw new IllegalArgumentException("Unsupported value type " + vt);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Prepared at ");
		sb.append(timestamp);
		sb.append("\n");

		String[] attributesArray = attributes.toArray(new String[attributes.size()]);

		for (IDAndValues id : userInformation) {
			sb.append("User ");
			sb.append(id.getUserId());
			sb.append("\n");

			if (id.getValues().size() != attributes.size()) {
				sb.append("\tAttributes/values mismatch");
			}

			int i = 0;
			for (IEvalValue value : id.getValues()) {
				sb.append("\t");
				sb.append(attributesArray[i++]);
				sb.append("=");
				sb.append(value.toString());
				sb.append("\n");
			}
		}

		return sb.toString();
	}
}
