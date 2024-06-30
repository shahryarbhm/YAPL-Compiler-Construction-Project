package yapl.lib;

/**
 * An array data type of the YAPL language.
 * A multi-dimensional array is represented recursively as a single-dimensional
 * array with an array base type.
 * 
 * This class supports dimensions for multi-dimensional arrays.
 * Each dimension is represented by an integer value.
 * 
 * Example usage:
 * ArrayType arr = new ArrayType(baseType, 3, 4, 5);
 * This creates a 3-dimensional array with dimensions 3x4x5.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class ArrayType extends Type {

	/** The array dimensions. */
	public int[] dimensions;

	/** The element type. */
	public Type base;

	/**
	 * Create a new array type with undefined length.
	 * 
	 * @param base       the element (base) type.
	 * @param dimensions the array dimensions.
	 */
	public ArrayType(Type base, int... dimensions) {
		this.base = base;
		this.dimensions = dimensions;
		System.out.println("ArrayType: " + this.toString()); // Debug
	}

	/**
	 * Check if the given indices are valid.
	 * 
	 * @param indices the array indices to be checked.
	 * @return <code>true</code> if the indices are valid.
	 */
	public boolean indicesAreValid(int... indices) {
		if (indices.length != dimensions.length) {
			return false;
		}
		for (int i = 0; i < indices.length; i++) {
			if (indices[i] < 0 || indices[i] >= dimensions[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isCompatible(Type start, Type type) {
		if (!(type instanceof ArrayType)) {
			return false;
		}
		ArrayType other = (ArrayType) type;
		if (dimensions.length != other.dimensions.length) {
			return false;
		}
		for (int i = 0; i < dimensions.length; i++) {
			if (dimensions[i] != other.dimensions[i]) {
				return false;
			}
		}
		return base.isCompatible(other.base);
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(base.toString());
		for (int dimension : dimensions) {
			buf.append('[');
			buf.append(dimension);
			buf.append(']');
		}
		return buf.toString();
	}

}
