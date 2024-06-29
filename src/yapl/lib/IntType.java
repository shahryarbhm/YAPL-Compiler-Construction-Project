package yapl.lib;

/**
 * The YAPL INTEGER data type.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class IntType extends Type {

	/** If constant, the value is stored in this object. */
	public int value;
	
	/** Create a new IntType object. */
	public IntType()
	{
		
	}
	
	/** Create a new IntType object with the given constant value.
	 * 
	 * @param value		the constant value.
	 */
	public IntType(int value)
	{
		this.value = value;
	}
	
	/** Return a string representation of this type. */
	public String toString()
	{
		return "Integer";
	}

	@Override
	public boolean isCompatible(Type start, Type type) {
		return type instanceof IntType;
	}

}
