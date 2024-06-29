package yapl.lib;

/**
 * The super class of all YAPL data types.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public abstract class Type {
	
	/** Return <code>true</code> if the given type is compatible with this type. */
	public boolean isCompatible(Type type)
	{
		return isCompatible(this, type);
	}
	
	/** 
	 * Compatibility check called by {@link isCompatible(Type)}.
	 * @param start   root of call hierarchy; may be used to avoid infinite recursion.
	 * @param other   the other type to check compatibility with.
	 */
	protected abstract boolean isCompatible(Type start, Type other);
	
	/** Return <code>true</code> if this type is a reference type. Defaults to <code>false</code>. */
	public boolean isReference()
	{
		return false;
	}
	
	/** Return a string representation of this type. */
	public String toString()
	{
		return "<unknown type>";
	}

}
