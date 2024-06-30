package yapl.version.codegen;

import java.util.List;

import yapl.interfaces.Attrib;
import yapl.interfaces.BackendAsmRM;
import yapl.interfaces.CompilerError;
import yapl.interfaces.Symbol;
import yapl.interfaces.Token;
import yapl.lib.*;

/**
 * Implementation of CodeGen interface generating assembler code for register
 * machines.
 * 
 * @see yapl.interfaces.BackendAsmRM
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class CodeGenAsmRM implements yapl.interfaces.CodeGen, YAPLConstants {

	/** The target architecture to use for code generation. */
	protected BackendAsmRM backend;

	/**
	 * Current stack frame nesting level (non-negative).
	 * 0 means main program.
	 */
	protected int stackLevel = 0;

	/** Counter for {@link #newLabel()}. */
	protected int labelNum = 0;

	/**
	 * Return the number of bytes occupied by a variable of the given data type
	 * on the target architecture.
	 * 
	 * @param type the data type to calculate the size of.
	 * @throws YAPLException (Internal)
	 *                       if <code>type</code> is not supported by this method.
	 */
	protected int sizeOf(Type type)
			throws YAPLException {
		if (type instanceof IntType
				|| type instanceof BoolType
				|| type instanceof ArrayType)
			// array variables also occupy only 1 word (start address)
			return backend.wordSize();
		else if (type instanceof RecordType)
			// fields of record types are references!
			return ((RecordType) type).numFields() * backend.wordSize();
		throw new YAPLException(YAPLException.Internal);
	}

	/**
	 * Return number of bytes occupied by a reference to the given data type
	 * on the target architecture. If the data type is primitive (int or bool),
	 * the data type size will be returned.
	 * 
	 * @param type the data type referenced.
	 */
	protected int sizeAsReference(Type type) {
		return backend.wordSize();
	}

	/**
	 * Constructor.
	 * 
	 * @param arch the target architecture to generate code for.
	 */
	public CodeGenAsmRM(BackendAsmRM backend) {
		this.backend = backend;
	}

	@Override
	public String newLabel() {
		return "L" + labelNum++;
	}

	@Override
	public void assignLabel(String label) {
		backend.emitLabel(label, null);
	}

	@Override
	public byte loadValue(Attrib attr) throws YAPLException {
		int attrKind = attr.getKind();
		if (attrKind == Attrib.RegValue)
			return attr.getRegister();
		byte reg = (attrKind == Attrib.RegAddress) ? attr.getRegister() : backend.allocReg();
		if (reg < 0) {
			throw new YAPLException(YAPLException.NoMoreRegs);
		}
		Type attrType = attr.getType();
		switch (attrKind) {
			case Attrib.RegAddress:
				backend.loadWordReg(reg, reg);
				break;
			case Attrib.Constant: {
				int value = 0;
				if (attrType instanceof IntType) {
					value = ((IntType) attrType).value;
				} else if (attrType instanceof BoolType) {
					value = backend.boolValue(((BoolType) attrType).value);
				} else {
					throw new YAPLException(YAPLException.Internal);
				}
				backend.loadConst(reg, value);
			}
				break;
			case Attrib.MemoryOperand:
				backend.loadWord(reg, attr.getOffset(), attr.isGlobal());
				break;
			default:
				throw new YAPLException(YAPLException.Internal);
		}
		attr.setRegister(reg);
		attr.setKind(Attrib.RegValue);
		return reg;
	}

	@Override
	public byte loadAddress(Attrib attr) throws YAPLException {
		int attrKind = attr.getKind();
		if (attrKind == Attrib.RegAddress)
			return attr.getRegister();
		byte reg = backend.allocReg();
		if (reg < 0) {
			throw new YAPLException(YAPLException.NoMoreRegs);
		}
		switch (attrKind) {
			case Attrib.MemoryOperand:
				backend.loadAddress(reg, attr.getOffset(), attr.isGlobal());
				break;
			default:
				System.err.println("loadAddress: attrKind = " + attrKind);
				throw new YAPLException(YAPLException.Internal);
		}
		attr.setRegister(reg);
		attr.setKind(Attrib.RegAddress);
		return reg;
	}

	@Override
	public void freeReg(Attrib attr) {
		if (attr.getKind() != Attrib.RegValue &&
				attr.getKind() != Attrib.RegAddress)
			return;
		backend.freeReg(attr.getRegister());
		attr.setKind(Attrib.Invalid);
	}

	@Override
	public void allocVariable(Symbol sym) throws YAPLException {
		Type t = sym.getType();
		int bytes = t.isReference() ? sizeAsReference(t) : sizeOf(t);
		if (sym.isGlobal()) {
			sym.setOffset(backend.allocStaticData(bytes, sym.getName()));
		} else {
			sym.setOffset(backend.allocStack(bytes, sym.getName()));
		}
	}

	@Override
	public void setFieldOffsets(RecordType record) {
		int offset = 0;
		for (Symbol field : record) {
			field.setOffset(offset);
			offset += sizeAsReference(field.getType());
		}
	}

	@Override
	public void setParamOffset(Symbol sym, int pos) {
		sym.setOffset(backend.paramOffset(pos));
	}

	@Override
	public void arrayOffset(Attrib arr, Attrib index) throws YAPLException {
		if (!(arr.getType() instanceof ArrayType)) {
			throw new YAPLException(YAPLException.Internal);
		}
		byte reg = loadValue(arr);
		byte idx = loadValue(index);
		backend.arrayOffset(reg, reg, idx);
		freeReg(index);
		arr.setType(((ArrayType) arr.getType()).base);
		arr.setKind(Attrib.RegAddress);
	}

	@Override
	public void recordOffset(Attrib record, Symbol field) throws YAPLException {
		if (!(record.getType() instanceof RecordType))
			throw new YAPLException(YAPLException.Internal);
		byte reg = loadValue(record);
		backend.addConst(reg, reg, field.getOffset());
		record.setType(field.getType());
		record.setKind(Attrib.RegAddress);
	}

	@Override
	public void assign(Attrib lvalue, Attrib expr) throws YAPLException {
		byte lreg = loadAddress(lvalue);
		byte reg = loadValue(expr);
		backend.storeWordReg(reg, lreg);
		freeReg(expr);
		freeReg(lvalue);
	}

	@Override
	public Attrib op1(Token op, Attrib x) throws YAPLException {
		byte xReg = loadValue(x);
		if (x.getType() instanceof IntType) {
			switch (op.getKind()) {
				case PLUS:
					/* nop */
					break;
				case MINUS:
					backend.neg(xReg, xReg);
					break;
				default:
					throw new YAPLException(YAPLException.IllegalOp1Type, op);
			}
		} else {
			throw new YAPLException(YAPLException.IllegalOp1Type, op);
		}
		return x;
	}

	@Override
	public Attrib op2(Attrib x, Token op, Attrib y) throws YAPLException {
		byte xReg = loadValue(x);
		byte yReg = loadValue(y);
		if (x.getType() instanceof IntType && y.getType() instanceof IntType) {
			switch (op.getKind()) {
				case PLUS:
					backend.add(xReg, xReg, yReg);
					break;
				case MINUS:
					backend.sub(xReg, xReg, yReg);
					break;
				case MULT:
					backend.mul(xReg, xReg, yReg);
					break;
				case DIV:
					backend.div(xReg, xReg, yReg);
					break;
				case MOD:
					backend.mod(xReg, xReg, yReg);
					break;
				default:
					throw new YAPLException(YAPLException.IllegalOp2Type, op);
			}
		} else if (x.getType() instanceof BoolType && y.getType() instanceof BoolType) {
			switch (op.getKind()) {
				case AND:
					backend.and(xReg, xReg, yReg);
					break;
				case OR:
					backend.or(xReg, xReg, yReg);
					break;
				default:
					throw new YAPLException(YAPLException.IllegalOp2Type, op);
			}
		} else {
			throw new YAPLException(YAPLException.IllegalOp2Type, op);
		}
		x.setConstant(x.isConstant() && y.isConstant());
		freeReg(y);
		return x;
	}

	@Override
	public Attrib relOp(Attrib x, Token op, Attrib y) throws YAPLException {
		if (!(x.getType() instanceof IntType && y.getType() instanceof IntType)) {
			throw new YAPLException(YAPLException.IllegalRelOpType, op);
		}
		byte xReg = loadValue(x);
		byte yReg = loadValue(y);
		switch (op.getKind()) {
			case LESS:
				backend.isLess(xReg, xReg, yReg);
				break;
			case LESS_EQUAL:
				backend.isLessOrEqual(xReg, xReg, yReg);
				break;
			case GREATER:
				backend.isLess(xReg, yReg, xReg);
				break;
			case GREATER_EQUAL:
				backend.isLessOrEqual(xReg, yReg, xReg);
				break;
			default:
				throw new YAPLException(YAPLException.IllegalRelOpType, op);
		}
		x.setType(new BoolType());
		x.setConstant(x.isConstant() && y.isConstant());
		freeReg(y);
		return x;
	}

	@Override
	public Attrib equalOp(Attrib x, Token op, Attrib y) throws YAPLException {
		byte xReg = loadValue(x);
		byte yReg = loadValue(y);
		if ((x.getType() instanceof IntType && y.getType() instanceof IntType)
				|| (x.getType() instanceof BoolType && y.getType() instanceof BoolType)) {
			backend.isEqual(xReg, xReg, yReg);
		} else {
			throw new YAPLException(YAPLException.IllegalEqualOpType, op);
		}
		x.setType(new BoolType());
		x.setConstant(x.isConstant() && y.isConstant());
		freeReg(y);
		return x;
	}

	@Override
	public void enterProc(Symbol proc) throws YAPLException {
		if (proc == null)
			backend.enterMain();
		else {
			int nParams = 0;
			for (Symbol p = proc.getNextSymbol(); p != null; p = p.getNextSymbol())
				nParams++;
			backend.enterProc(proc.getName(), nParams);
		}
	}

	@Override
	public void exitProc(Symbol proc) throws YAPLException {
		if (proc == null)
			backend.exitMain("main_ret");
		else
			backend.exitProc(proc.getName() + "_ret");
	}

	@Override
	public void returnFromProc(Symbol proc, Attrib returnVal)
			throws YAPLException {
		byte reg = (byte) -1;
		if (returnVal != null)
			reg = loadValue(returnVal);
		backend.returnFromProc(proc == null ? "main" : proc.getName() + "_ret", reg);
		if (returnVal != null)
			freeReg(returnVal);
	}

	@Override
	public Attrib callProc(Symbol proc, Attrib[] args) throws YAPLException {
		int narg = (args == null) ? 0 : args.length;
		backend.prepareProcCall(narg);
		if (narg > 0) {
			narg = 0;
			for (Attrib a : args) {
				byte reg = loadValue(a);
				backend.passArg(narg++, reg);
				freeReg(a);
			}
		}
		Attrib retVal = null;
		byte reg = (byte) -1;
		if (proc.getType() != null) {
			retVal = new yapl.impl.Attrib(Attrib.RegValue, proc.getType());
			reg = backend.allocReg();
			if (reg < 0)
				throw new YAPLException(YAPLException.NoMoreRegs);
			retVal.setRegister(reg);
		}
		backend.callProc(reg, proc.getName());
		return retVal;
	}

	@Override
	public void writeString(String string) throws YAPLException {
		if (string.length() > 2) {
			// remove quote characters from string
			String s = string.substring(1, string.length() - 1);
			backend.writeString(backend.allocStringConstant(s));
		}
	}

	@Override
	public void branchIfFalse(Attrib condition, String label)
			throws YAPLException {
		byte reg = loadValue(condition);
		backend.branchIf(reg, false, label);
		freeReg(condition);
	}

	@Override
	public void jump(String label) {
		backend.jump(label);
	}

	@Override
	public void storeArrayDim(int dim, Attrib length) throws YAPLException {
		byte reg = loadValue(length);
		backend.storeArrayDim(dim, reg);
		freeReg(length);
	}

	@Override
	public Attrib allocArray(ArrayType arrayType) throws YAPLException {
		byte reg = backend.allocReg();
		backend.allocArray(reg);
		Attrib attrib = new yapl.impl.Attrib(Attrib.RegValue, arrayType);
		attrib.setRegister(reg);
		return attrib;
	}

	@Override
	public Attrib allocArray(ArrayType arrayType, List dimensions) throws YAPLException {
		if (!(arrayType instanceof ArrayType)) {
			throw new YAPLException(CompilerError.Internal);
		}

		// Get the base type (int or bool)
		Type baseType = arrayType;
		while (baseType instanceof ArrayType) {
			baseType = ((ArrayType) baseType).getElementType();
		}

		// Allocate register for array pointer
		byte arrayReg = backend.allocReg();

		// Calculate total size and allocate memory
		byte sizeReg = backend.allocReg();
		backend.loadConst(sizeReg, 4); // Start with 4 bytes for length

		for (int i = dimensions.size() - 1; i >= 0; i--) {
			Attrib dim = (Attrib) dimensions.get(i);
			byte dimReg = loadValue(dim);

			// Multiply current size by dimension
			backend.mul(sizeReg, sizeReg, dimReg);

			// Add 4 bytes for length of this dimension
			backend.loadConst(dimReg, 4);
			backend.add(sizeReg, sizeReg, dimReg);

			backend.freeReg(dimReg);
		}

		// Allocate memory
		backend.allocHeap(arrayReg, sizeReg);
		backend.freeReg(sizeReg);

		// Initialize array structure
		int offset = 0;

		for (int i = 0; i < dimensions.size(); i++) {
			Attrib dim = (Attrib) dimensions.get(i);
			byte dimReg = loadValue(dim);

			// Store dimension length
			backend.storeWord(dimReg, offset, false);

			// Update offset
			offset += 4;

			backend.freeReg(dimReg);
		}

		// Create and return Attrib for the array
		Attrib result = new yapl.impl.Attrib(Attrib.RegValue, arrayType);
		result.setKind(Attrib.RegValue);
		result.setRegister(arrayReg);

		return result;
	}

	@Override
	public Attrib allocRecord(RecordType recordType) throws YAPLException {
		byte reg = backend.allocReg();
		backend.allocHeap(reg, sizeOf(recordType));
		Attrib attrib = new yapl.impl.Attrib(Attrib.RegValue, recordType);
		attrib.setRegister(reg);
		return attrib;
	}

	@Override
	public Attrib arrayLength(Attrib arr) throws YAPLException {
		byte reg = loadValue(arr);
		backend.arrayLength(reg, reg);
		arr.setKind(Attrib.RegValue);
		arr.setType(new IntType());
		return arr;
	}

	@Override
	public Attrib arrayLength(Attrib arr, int dimension) throws YAPLException {
		byte reg = loadValue(arr);

		// For multi-dimensional arrays, we need to traverse the array structure
		for (int i = 0; i < dimension; i++) {
			// Load the address of the next dimension
			backend.loadWord(reg, 0, false);
		}

		// Get the length of the current dimension
		backend.arrayLength(reg, reg);

		arr.setKind(Attrib.RegValue);
		arr.setType(new IntType());
		return arr;
	}
}
