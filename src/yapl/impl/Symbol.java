/**
 * This package contains implementations of the interfaces defined
 * in the package yapl.interfaces.
 */
package yapl.impl;

import yapl.lib.*;

/**
 * Implementation of the {@link yapl.interfaces.Symbol} interface.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class Symbol implements yapl.interfaces.Symbol {
	
	protected int kind;
	protected String name;
	protected Type type;
	protected boolean reference = false;
	protected boolean readonly = false;
	protected boolean global;
	protected int offset;
	protected yapl.interfaces.Symbol next = null;
	protected boolean returnSeen = false;

	/* array indices must correspond to the integer constants defined
	 * by yapl.interfaces.Symbol.
	 */
	private String[] kindStrings = { "program", "procedure", "variable", "constant",
			"typename", "field", "parameter" };
	
	/**
	 * Create a new symbol of given kind and name.
	 * 
	 * @param kind
	 *            one of the constants defined by
	 *            {@link yapl.interfaces.Symbol}.
	 * @param name
	 *            the symbol's name (identifier).
	 */
	public Symbol(int kind, String name) {
		this.kind = kind;
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yapl.interfaces.Symbol#getKind()
	 */
	public int getKind() {
		return kind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yapl.interfaces.Symbol#getKindString()
	 */
	public String getKindString() {
		if (kind >= 0 && kind < kindStrings.length)
			return kindStrings[kind];
		return null;
	}

	/* (non-Javadoc)
	 * @see yapl.interfaces.Symbol#setKind(int)
	 */
	public void setKind(int kind) {
		this.kind = kind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yapl.interfaces.Symbol#getName()
	 */
	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isReference() {
		return reference;
	}
	
	public void setReference(boolean isReference) {
		reference = isReference;
	}
	
	@Override
    public boolean isReadonly()
    {
        return readonly;
    }

    @Override
    public void setReadonly(boolean isReadonly)
    {
        readonly = isReadonly;
    }

    /* (non-Javadoc)
	 * @see yapl.interfaces.Symbol#isGlobal()
	 */
	public boolean isGlobal() {
		return global;
	}

	/* (non-Javadoc)
	 * @see yapl.interfaces.Symbol#setGlobal(boolean)
	 */
	public void setGlobal(boolean isGlobal) {
		global = isGlobal;
	}

	/* (non-Javadoc)
	 * @see yapl.interfaces.Symbol#getOffset()
	 */
	public int getOffset() {
		return offset;
	}

	/* (non-Javadoc)
	 * @see yapl.interfaces.Symbol#setOffset(int)
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yapl.interfaces.Symbol#getNextSymbol()
	 */
	public yapl.interfaces.Symbol getNextSymbol() {
		return next;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yapl.interfaces.Symbol#setNextSymbol(yapl.interfaces.Symbol)
	 */
	public void setNextSymbol(yapl.interfaces.Symbol symbol) {
		next = symbol;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yapl.interfaces.Symbol#getReturnSeen()
	 */
	public boolean getReturnSeen() {
		return returnSeen;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see yapl.interfaces.Symbol#setReturnSeen(boolean)
	 */
	public void setReturnSeen(boolean seen) {
		returnSeen = seen;
	}

	/* (non-Javadoc)
	 * @see yapl.interfaces.Symbol#toString()
	 */
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append(name);
		s.append(" (");
		s.append(global ? "global" : "local");
		s.append(", ");
		s.append(getKindString());
		s.append(", type = ");
		s.append(type == null ? "(null)" : type.toString());
		s.append(", offset = ");
		s.append(offset);
		s.append(", returnSeen = ");
		s.append(returnSeen);
		s.append(")");
		return s.toString();
	}

}
