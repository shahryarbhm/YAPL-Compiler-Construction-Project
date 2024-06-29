package yapl.lib;

/**
 * The YAPL BOOLEAN data type.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class BoolType extends Type {

	/** If constant, the value is stored in this object. */
	public boolean value;
	
	/** Create a new BoolType object. */
	public BoolType()
	{
		
	}
	
	/** Create a new BoolType object with the given constant value.
	 * 
	 * @param value		the constant value.
	 */
	public BoolType(boolean value)
	{
		this.value = value;
	}
	
	/** Return a string representation of this type. */
	public String toString()
	{
		return "Boolean";
	}

	@Override
	public boolean isCompatible(Type start, Type type) {
		return type instanceof BoolType;
	}

}
