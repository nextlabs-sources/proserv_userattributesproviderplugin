package com.nextlabs.plugins.userattributes.v2.dto;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IMultivalue;
import com.bluejungle.framework.expressions.Multivalue;
import com.bluejungle.framework.expressions.ValueType;

public class UserInformationDTO implements Externalizable, Serializable {
	private static final IEvalValue EMPTY_STRING = EvalValue.build("");
	private static final Log LOG = LogFactory.getLog(UserInformationDTO.class);
	private static final long serialVersionUID = 123456342525789L;
	private String userId;
	private List<IEvalValue> values;

	public UserInformationDTO() {
		userId = null;
		values = new ArrayList<IEvalValue>();
	}

	public UserInformationDTO(String userId, List<IEvalValue> values) {
		this.userId = userId;
		this.values = values;
	}

	public String getUserId() {
		return userId;
	}

	public List<IEvalValue> getValues() {
		return values;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// Read userId
		this.userId = in.readUTF();

		// Read values
		this.values = readValues(in);
	}

	private IEvalValue readValue(ObjectInput in) throws IOException {
		final char type = in.readChar();
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
			final int size = in.readInt();
			final char subtype = in.readChar();

			switch (subtype) {
			case 'L':
				final List<Long> l = new ArrayList<Long>();
				for (int i = 0; i < size; i++) {
					l.add(in.readLong());
				}
				v = EvalValue.build(Multivalue.create(l, ValueType.LONG));
				break;
			case 'S':
				final List<String> s = new ArrayList<String>();
				for (int i = 0; i < size; i++) {
					s.add(in.readUTF());
				}
				v = EvalValue.build(Multivalue.create(s, ValueType.STRING));
				break;
			case 'N':
				v = EvalValue.build(IMultivalue.EMPTY);
				break;
			default:
				LOG.error("Unsupported value code " + subtype);
				throw new IllegalArgumentException("Unsupported value code " + subtype);
			}
			break;
		default:
			LOG.error("Unsupported value code " + type);
			throw new IllegalArgumentException("Unsupported value code " + type);
		}

		return v;
	}

	private List<IEvalValue> readValues(ObjectInput in) throws IOException {
		final List<IEvalValue> values = new ArrayList<IEvalValue>();

		// Read Size
		final long numValues = in.readLong();

		// Read Values
		for (long j = 0; j < numValues; j++) {
			values.add(readValue(in));
		}

		return values;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setValues(List<IEvalValue> values) {
		this.values = values;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// Write userId
		out.writeUTF(userId);

		// Write values
		writeValues(out);
	}

	private void writeValue(ObjectOutput out, IEvalValue value) throws IOException {
		// Make some edge cases easier to handle
		if (value == null || value == IEvalValue.NULL || value == IEvalValue.EMPTY) {
			value = EMPTY_STRING;
		}

		final ValueType vt = value.getType();

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
			final IMultivalue mv = (IMultivalue) value.getValue();

			out.writeInt(mv.size());

			final ValueType mvt = mv.getType();

			if (mvt == ValueType.LONG) {
				out.writeChar('L');

				for (final IEvalValue e : mv) {
					out.writeLong((Long) e.getValue());
				}
			} else if (mvt == ValueType.STRING) {
				out.writeChar('S');

				for (final IEvalValue e : mv) {
					out.writeUTF((String) e.getValue());
				}
			} else if (mvt == ValueType.NULL) {
				out.writeChar('N');
			} else {
				LOG.error("Unsupported value type " + mvt);
				throw new IllegalArgumentException("Unsupported value type " + mvt);
			}
		} else {
			LOG.error("Unsupported value type " + vt);
			throw new IllegalArgumentException("Unsupported value type " + vt);
		}
	}

	private void writeValues(ObjectOutput out) throws IOException {
		// Write Size
		out.writeLong(values.size());

		// Write Values
		for (final IEvalValue value : values) {
			writeValue(out, value);
		}
	}
}
