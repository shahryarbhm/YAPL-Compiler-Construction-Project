package yapl.impl;

import yapl.interfaces.Symbol;
import yapl.lib.*;

/**
 * An implementation of the {@link yapl.interfaces.Attrib} interface.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class Attrib implements yapl.interfaces.Attrib {

	private byte kind;
	private Type type;
	private boolean constant = false;
	private boolean readonly = false;
	private boolean global;
	private int offset;
	private byte register = -1;
	
	/**
	 * Create an attribute object of the given kind.
	 * 
	 * @param kind
	 *            one of the constants defined by this class.
	 */
	public Attrib(byte kind) {
		this.kind = kind;
	}

	/**
	 * Create an attribute object of the given kind and operand type.
	 * 
	 * @param kind
	 *            one of the constants defined by yapl.interfaces.Attrib.
	 * @param type
	 *            the operand data type.
	 */
	public Attrib(byte kind, Type type) {
		this.kind = kind;
		this.type = type;
	}

	/**
	 * Create an Attrib object from a symbol.
	 * 
	 * @param sym
	 *            the symbol; must be of the following
	 *            kind: {@link Symbol#Constant}, {@link Symbol#Variable},
	 *            or {@link Symbol#Parameter}.
	 * @throws YAPLException
	 *             (Internal) if <code>sym</code> is of an invalid kind.
	 */
	public Attrib(Symbol sym) throws YAPLException {
        type = sym.getType();
		switch (sym.getKind()) {
		case Symbol.Constant:
			kind = Constant;
			constant = true;
			break;
		case Symbol.Variable:  // fall through
		case Symbol.Parameter:
			kind = MemoryOperand;
			break;
		default:
			throw new YAPLException(YAPLException.Internal);
		}
		readonly = sym.isReadonly();
		global = sym.isGlobal();
		offset = sym.getOffset();
	}

	@Override
	public byte getKind() {
		return kind;
	}

	@Override
	public void setKind(byte kind) {
		this.kind = kind;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public boolean isConstant() {
		return constant;
	}

	@Override
	public void setConstant(boolean isConstant) {
		constant = isConstant;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public void setOffset(int offset) {
		this.offset = offset;
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

    @Override
	public boolean isGlobal() {
		return global;
	}

	@Override
	public void setGlobal(boolean isGlobal) {
		global = isGlobal;
	}

	@Override
	public byte getRegister() {
		return register;
	}

	@Override
	public void setRegister(byte register) {
		this.register = register;
	}

}
