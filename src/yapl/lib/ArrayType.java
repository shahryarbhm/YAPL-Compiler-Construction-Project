package yapl.lib;

/**
 * An array data type of the YAPL language.
 * A multi-dimensional array is represented recursively as a single-dimensional
 * array with an array base type.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class ArrayType extends Type {

	/**
	 * The array length (number of elements).
	 * A negative length means an undefined array length.
	 */
	public int len = -1;

	/** The element type. */
	public Type base = null;

	/**
	 * Create a new array type with undefined length.
	 * 
	 * @param base the element (base) type.
	 */
	public ArrayType(Type base) {
		this.base = base;
		this.len = -1;
	}

	/**
	 * Create a new array type.
	 * 
	 * @param len the number of array elements.
	 *            A value <code>len &lt; 0</code> means an undefined array length.
	 */
	public ArrayType(int len) {
		this.len = len;
	}

	/**
	 * Create a new array type.
	 * 
	 * @param base the element (base) type.
	 * @param len  the number of array elements.
	 *             A value <code>len &lt; 0</code> means an undefined array length.
	 */
	public ArrayType(Type base, int len) {
		this.base = base;
		this.len = len;
	}

	/**
	 * Check if the given index is valid.
	 * 
	 * @param index the array index to be checked.
	 * @return <code>true</code> if the index is valid.
	 */
	public boolean indexIsValid(int index) {
		return index >= 0 && index < len;
	}

	@Override
	public boolean isCompatible(Type start, Type type) {
		if (!(type instanceof ArrayType))
			return false;
		ArrayType other = (ArrayType) type;
		return base.isCompatible(other.base) &&
				(len < 0 || other.len < 0 || other.len == len);
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(base.toString());
		buf.append('[');
		if (len >= 0)
			buf.append(len);
		buf.append(']');
		return buf.toString();
	}

}