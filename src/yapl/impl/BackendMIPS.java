package yapl.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import yapl.interfaces.BackendAsmRM;

/**
 * Implementation of the {@link BackendAsmRM} interface for generating
 * MIPS assembler code. The generated code is to be executed by the MIPS simulator
 * <a href="http://courses.missouristate.edu/KenVollmar/MARS/">MARS</a>.
 * <p>
 * The system property <code>yapl.predefined.asm</code> needs to be set
 * to the filename of the assembler code implementing the YAPL
 * predefined functions.
 * </p>
 * <p>
 * Activation record structure (stack frame) for function calls is given below. 
 * Note that the MIPS procedure call convention is violated in two aspects 
 * (since we do not need to link with non-YAPL code):
 * (1) s-registers are treated as caller-saved, and 
 * (2) all procedure arguments are passed on the stack (as opposed to using 
 *     a-registers for the first 4 arguments).
 * </p>
 * <pre>
 * (stack, high addresses)
 * --- caller-saved registers: (order given by implementation)
 * 			$v0, $v1
 *          $a0 - $a3
 *          $t0 - $t9
 *          $s0 - $s7 (we *treat* them as caller-saved!)
 * --- procedure arguments:
 *          ...
 *          arg 2
 *          arg 1
 * 			arg 0
 * --- callee-saved registers:
 * $fp ->   $fp        (<-- $sp of previous stack frame)
 *          $ra
 * --- local variables
 * $sp ->   (top of stack, MUST NOT be written to)
 * </pre>
 * 
 * @author Mario Taschwer
 */
public class BackendMIPS implements BackendAsmRM
{
    /** The label to mark the start of the static data area. */
	private static final String StaticDataLabel = "staticData";
	
	/** Word size in bytes. */
	private static final int WordSize = 4;
	
	/** Binary logarithm of {@link #WordSize}. */
	private static final int LogWordSize = 2;
	
	/** Maximal number of array dimensions. */
	private static final int maxDims = 16;
	
	/** The register storing the static data base address. */
	private static final byte RegStatic = 23;		// $s7
	
	/** The frame pointer register. */
	private static final byte RegFP = 30;		    // $fp
	
	/** a0 register. */
	private static final byte RegA0 = 4;
	
	/** v0 register. */
	private static final byte RegV0 = 2;
	
	/** Zero register. */
	private static final byte RegZero = 0;
	
	/** Allocatable registers. */
	private byte[] usableRegs =
	{ 8, 9, 10, 11, 12, 13, 14, 15,
	  16, 17, 18, 19, 20, 21, 22, 24, 25 };

	/** The PrintStream where to send the assembler code to. */
	private PrintStream out;
	
	/** The next free (non-negative) offset in the static data area. */
	private int staticDataOffset = 0;
	
	/** The (negative) offset representing the next free position (top) of the stack,
	 * relative to the address pointed to by <code>$fp</code>.
	 * Must be re-initialized when entering a new stack frame.
	 */
	private int stackOffset;
	
	/** The size of a procedure call frame on the stack in bytes. 
	 * Includes caller-saved registers and procedure arguments.
	 */
	private int procFrameSize;
	
	/** Register allocation table. */
	private boolean[] regsUsed = new boolean[32];
	
	/* Possible values of {@link #codeSegment}. */
	public static final int SegmentNone = 0;
	public static final int SegmentText = 1;
	public static final int SegmentData = 2;
	
	/** Current code segment.
	 * Possible values are: {@link #SegmentNone}, {@link #SegmentText},
	 * {@link #SegmentData}.
	 */
	private int codeSegment = SegmentNone;

    /** Address of global variable storing the length of 1-dimensional arrays. */
    protected int dimAddr1;
    
    /** Address of global variable storing the heap address of Integer[] array 
     * representing multiple array dimensions. 
     * Its length is {@link #maxDims}. 
     */
    protected int dimAddr;
    
    /** Current number of dimensions stored in array at {@link #dimAddr}. */
    protected int nDims;

    /** Calculate the next word-aligned address greater or equal to
	 * <code>addr</code>.
	 */
	private int alignToWord(int addr)
	{
		int aligned = addr & ~(WordSize-1);
		if (aligned != addr)
			aligned += WordSize;
		return aligned;
	}
	
	/** Emit <code>.text</code> or <code>.data</data> directive, 
	 * if current code segment is different.
	 * @param segment		one of {@link #SegmentNone}, {@link #SegmentText},
	 *                      {@link #SegmentData}.
	 */
	private void ensureSegment(int segment)
	{
		if (codeSegment != segment) {
			switch(segment) {
			case SegmentText:
				out.println(".text");
				break;
			case SegmentData:
				out.println(".data");
				break;
			}
			codeSegment = segment;
		}
	}
	
    /**
     * Save used registers to stack.
     * The stack pointer is decreased by (<code>minWords</code> plus
     * the number of saved registers) words. The registers will be saved to
     * high addresses of the allocated stack space.
     * @param minWords    minimal number of words to allocate on stack.
     * @return  the actual allocated stack space size in bytes.
     */
    private int saveRegs(int minWords)
    {
        int words = minWords;
        for (int i=0; i < usableRegs.length; i++) {
            if (regsUsed[usableRegs[i]])
                words++;
        }
        int frameSize = words * WordSize;
        if (frameSize > 0) {
            int offset = stackOffset;
            stackOffset -= frameSize;
            out.println("    addi\t$sp, $sp, -"+ frameSize +"\t# saveRegs");
            for (int i=0; i < usableRegs.length; i++) {
                if (regsUsed[usableRegs[i]]) {
                    out.println("    sw  \t$"+ usableRegs[i] +", "+ offset +"($fp)");
                    offset -= WordSize;
                }
            }
        }
        return frameSize;
    }
    
    /**
     * Restore used registers from stack.
     * The number of bytes given by <code>freeStackSize</code> will be
     * deallocated on the stack.
     * @param freeStackSize     the number of bytes to deallocate on the stack;
     *                          it must match the return value of the corresponding
     *                          {@link #saveRegs(int)} call.
     * @param dontLoad          number of single register NOT to load; specify -1 if all registers
     *                          in use should be loaded.
     */
    private void restoreRegs(int freeStackSize, byte dontLoad)
    {
        stackOffset += freeStackSize;
        int offset = stackOffset;
        for (int i=0; i < usableRegs.length; i++) {
            if (regsUsed[usableRegs[i]]) {
                if (usableRegs[i] != dontLoad)
                    out.println("    lw  \t$"+ usableRegs[i] +", "+ offset +"($fp)");
                offset -= WordSize;
            }
        }
        out.println("    addi\t$sp, $sp, "+ freeStackSize +"\t# restoreRegs complete");
    }
    
    /**
     * Initialize run-time library.
     * A 1-dimensional array of length {@link #maxDims} will be allocated
     * and its start address stored at {@link #dimAddr}.
     */
    private void initRuntimeLib()
    {
        byte reg = allocReg();
        loadConst(reg, maxDims);
        storeArrayDim(0, reg);
        allocArray(reg);
        storeWord(reg, dimAddr, true);
        freeReg(reg);
    }

    /**
     * Inject content of assembler file into output stream.
     * @param path           path to assembler file.
     * @throws IOException   if assembler file cannot be opened for reading.
     */
    private void injectAsmFile(String path) throws IOException
    {
        BufferedReader rd = new BufferedReader(new FileReader(path));
        for (String line = rd.readLine(); line != null; line = rd.readLine())
            out.println(line);
        rd.close();
    }

	/** 
	 * Construct a new BackendMIPS instance writing to the given PrintStream.
	 * Assembler code for predefined and runtime library procedures, and some static data are emitted.
	 * @param outstream           where to write assembler code to.
     * @param predefinedAsmFile   path to assembler file containing code for predefined procedures (may be null or empty).
     * @param runtimelibAsmFile   path to assembler file containing code for runtime library (may be null or empty).
	 * @throws IOException        if predefinedAsmFile or runtimelibAsmFile cannot be opened for reading.
	 */
	public BackendMIPS(PrintStream outstream, String predefinedAsmFile, String runtimelibAsmFile) throws IOException
	{
		out = outstream;
		freeAllReg();
		comment("MIPS assembler code generated by the YAPL compiler");
		comment("(C) 2011-2024 ITEC, Klagenfurt University (mario.taschwer@aau.at)");
		if (predefinedAsmFile != null && predefinedAsmFile.length() > 0)
		    injectAsmFile(predefinedAsmFile);
		if (runtimelibAsmFile != null && runtimelibAsmFile.length() > 0)
		    injectAsmFile(runtimelibAsmFile);
		ensureSegment(SegmentData);
		out.println(StaticDataLabel + ":");
		this.dimAddr1 = allocStaticData(WordSize, "dimAddr1");
		this.dimAddr = allocStaticData(WordSize, "dimAddr");
	}

    /**
     * Construct a new BackendMIPS instance writing to the given PrintStream.
     * Uses system properties (if set) to inject assembler code for predefined and runtime library procedures:
     * - yapl.predefined.asm: path to file containing assembler code for predefined procedures.
     * - yapl.runtimelib.asm: path to file containing assembler code for runtime library.
     * @param outstream           where to write assembler code to.
     * @throws IOException        if predefinedAsmFile or runtimelibAsmFile cannot be opened for reading.
     */
    public BackendMIPS(PrintStream outstream) throws IOException
    {
        this(outstream, System.getProperty("yapl.predefined.asm"), System.getProperty("yapl.runtimelib.asm"));
    }

    /*--- implementation constants ---*/

	@Override
	public int wordSize() {
	    return 4;
	}
	
	@Override
	public int boolValue(boolean value) {
		return value ? 1 : 0;
	}

	/*--- register management ---*/

	/**
	 * Mark all registers as free.
	 */
	private void freeAllReg() {
		for (int r=0; r < regsUsed.length; r++)
			regsUsed[r] = false;
	}
	
    @Override
    public byte allocReg() {
        byte r;
        for (int i=0; i < usableRegs.length; i++) {
            r = usableRegs[i];
            if (!regsUsed[r]) {
                regsUsed[r] = true;
                return r;
            }
        }
        return -1;
    }

    @Override
    public void freeReg(byte reg) {
        regsUsed[reg] = false;
    }

    @Override
    public byte zeroReg() {
        return 0;
    }
    
    /*--- emitting comments and labels ---*/

    @Override
	public void comment(String comment) {
		out.println("# " + comment);
	}

	@Override
	public void emitLabel(String label, String comment) {
		out.print(label + ":");
		if (comment != null) {
			out.print("\t\t\t\t# " + comment);
		}
		out.println();
	}

    /*--- compile-time memory allocation ---*/

	@Override
	public int allocStaticData(int bytes, String comment) {
		/* align staticDataOffset to word size,
		 * must correspond to .align and .space directives
		 */
		int aligned = alignToWord(staticDataOffset);
		ensureSegment(SegmentData);
		out.println("    .align "+ LogWordSize);	// align next data on word boundary
		out.println("    .space " + bytes + "\t# " + comment + " (offset = " + aligned + ")");
		staticDataOffset = aligned + bytes;
		return aligned;
	}

    @Override
    public int allocStringConstant(String string) {
        int addr = staticDataOffset;
        int segment = codeSegment;
        ensureSegment(SegmentData);
        out.println("    .asciiz\t\"" + string + "\"\t# offset = " + addr);
        ensureSegment(segment);
        staticDataOffset += string.length() + 1;    // w/o enclosing quotes, w/ null byte
        return addr;
    }

    @Override
	public int allocStack(int bytes, String comment) {
		bytes = alignToWord(bytes);
		stackOffset -= bytes;
		int addr = stackOffset + WordSize;
		out.print("    addi\t$sp, $sp, -" + bytes);
		if (comment != null)
			out.print("\t# " + comment + " (offset = " + addr + ")");
		out.println();
		return addr;
	}

    /*--- run-time memory allocation ---*/

    @Override
	public void allocHeap(byte destReg, int bytes) {
        int savedBytes = saveRegs(0);
        loadConst(RegA0, bytes);
        loadConst(RegV0, 9);             // 'sbrk' system call code
        out.println("    syscall\t\t# sbrk");
        out.println("    move\t$"+ destReg +", $v0");
        restoreRegs(savedBytes, destReg);
	}
    
    @Override
    public void storeArrayDim(int dim, byte lenReg)
    {
        assert dim >= 0;
        // we assume that storeArrayDim() is called successively with
        // increasing values of dim
        nDims = dim+1;
        if (dim == 0) {
            // store dimension length at dimAddr1
            storeWord(lenReg, dimAddr1, true);
        }
        else if (dim > 0) {
            // store dimension length at dimAddr[dim]
            byte baseReg = allocReg();
            loadWord(baseReg, dimAddr, true);
            byte reg = allocReg();
            arrayOffsetConst(reg, baseReg, dim);
            storeWordReg(lenReg, reg);
            if (dim == 1) {
                // copy dimAddr1 to dimAddr[0]
                loadWord(reg, dimAddr1, true);
                arrayOffsetConst(baseReg, baseReg, 0);
                storeWordReg(reg, baseReg);
            }
            freeReg(reg);
            freeReg(baseReg);
        }
    }

	@Override
    public void allocArray(byte destReg)
    {
        assert nDims > 0;
        if (nDims == 1) {
            // allocate 1-dimensional array - MUST NOT call  _allocArray() in run-time library!
            int savedBytes = saveRegs(0);
            loadWord(RegA0, dimAddr1, true);
            addConst(RegA0, RegA0, 1);       // add array header size (1 word)
            shiftLeft(RegA0, RegA0, LogWordSize);
            loadConst(RegV0, 9);             // 'sbrk' system call code
            out.println("    syscall\t\t# sbrk");
            loadWord(RegA0, dimAddr1, true);
            storeWordReg(RegA0, RegV0);      // write array length
            out.println("    move\t$"+ destReg +", $v0");
            restoreRegs(savedBytes, destReg);
        }
        else if (nDims > 1) {
            // allocate multi-dimensional array - call _allocArray() in run-time library
            prepareProcCall(3);
            byte reg = allocReg();
            loadWord(reg, dimAddr, true);
            passArg(0, reg);
            loadConst(reg, 0);               // dim = 0
            passArg(1, reg);
            loadConst(reg, nDims);
            passArg(2, reg);
            freeReg(reg);
            callProc(destReg, "_allocArray");
        }
    }

    /*--- load/store operations ---*/

    @Override
	public void loadConst(byte reg, int value) {
		/* pseudo-instruction 'li' supports 32-bit immediate operand */
		out.println("    li  \t$" + reg + ", " + value);
	}

    @Override
    public void loadAddress(byte reg, int addr, boolean isStatic)
    {
        int basereg = isStatic ? RegStatic : RegFP;
        out.println("    la  \t$"+ reg +", "+ addr +"($"+ basereg +")");
    }

    @Override
	public void loadWord(byte reg, int addr, boolean isStatic) {
		int basereg = isStatic ? RegStatic : RegFP;
		out.println("    lw  \t$" + reg + ", " + addr 
					+ "($" + basereg + ")");
	}

    @Override
	public void storeWord(byte reg, int addr, boolean isStatic) {
		int basereg = isStatic ? RegStatic : RegFP;
		out.println("    sw  \t$" + reg + ", " + addr 
					+ "($" + basereg + ")");
	}

    @Override
    public void loadWordReg(byte reg, byte addrReg)
    {
        loadWordReg(reg, addrReg, 0);
    }

    @Override
	public void loadWordReg(byte reg, byte addrReg, int offset) 
    {
        out.println("    lw  \t$"+ reg +", "+ offset + "($"+ addrReg +")");
	}

	@Override
    public void storeWordReg(byte reg, int addrReg)
    {
        out.println("    sw  \t$"+ reg +", 0($"+ addrReg +")");
    }

    @Override
	public void arrayOffset(byte dest, byte baseAddr, byte index) {
    	// add header (1 word)
    	addConst(index, index, 1);
        shiftLeft(index, index, LogWordSize);
        add(dest, baseAddr, index);
	}

	private void arrayOffsetConst(byte dest, byte baseAddr, int index) {
		addConst(dest, baseAddr, (index+1)*WordSize);
	}

    @Override
    public void arrayLength(byte dest, byte baseAddr)
    {
        loadWordReg(dest, baseAddr);
    }

    /*--- run-time I/O operations ---*/

    @Override
    public void writeString(int addr)
    {
        // call predefined procedure void write(char *s)
        prepareProcCall(1);
        loadAddress(RegA0, addr, true);
        passArg(0, RegA0);
        callProc((byte) -1, "write");
    }

    /*--- arithmetic operations ---*/

    @Override
    public void neg(byte regDest, byte regX)
    {
        sub(regDest, RegZero, regX);
    }

    @Override
    public void add(byte regDest, byte regX, byte regY)
    {
        out.println("    add \t$" + regDest + ", $" + regX + ", $" + regY);
    }

    @Override
	public void addConst(byte regDest, byte regX, int value)
    {
        out.println("    addi\t$"+ regDest +", $"+ regX +", "+ value);
    }
    
    @Override
    public void sub(byte regDest, byte regX, byte regY)
    {
        out.println("    sub \t$" + regDest + ", $" + regX + ", $" + regY);
    }

    @Override
    public void mul(byte regDest, byte regX, byte regY)
    {
        out.println("    mul \t$" + regDest + ", $" + regX + ", $" + regY);
    }
    
    @Override
    public void div(byte regDest, byte regX, byte regY)
    {
        out.println("    div \t$" + regX + ", $" + regY);
        out.println("    mflo\t$" + regDest);
    }

    @Override
    public void mod(byte regDest, byte regX, byte regY)
    {
        out.println("    div \t$" + regX + ", $" + regY);
        out.println("    mfhi\t$" + regDest);
    }

    /**
     * Emit code to shift left <code>regSrc</code> by <code>numBits</code>,
     * storing the result in <code>regDest</code>.
     * @param regDest    destination register.
     * @param regSrc     source register.
     * @param numBits    number of bits to shift.
     */
    private void shiftLeft(byte regDest, byte regSrc, int numBits)
    {
        out.println("    sll \t$"+ regDest +", $"+ regSrc +", "+ numBits);
    }

    /*--- comparison operations ---*/

    @Override
    public void isLess(byte regDest, byte regX, byte regY)
    {
        out.println("    slt \t$" + regDest + ", $" + regX + ", $" + regY);
    }
    
    @Override
    public void isLessOrEqual(byte regDest, byte regX, byte regY)
    {
        out.println("    sle \t$" + regDest + ", $" + regX + ", $" + regY);
    }
    
    @Override
    public void isEqual(byte regDest, byte regX, byte regY)
    {
        out.println("    seq \t$" + regDest + ", $" + regX + ", $" + regY);
    }

    /*--- logical operations ---*/

    @Override
    public void not(byte regDest, byte regSrc)
    {
        out.println("    xori\t$" + regDest + ", $" + regSrc + ", 1");
    }
    
    @Override
    public void and(byte regDest, byte regX, byte regY)
    {
        out.println("    and \t$" + regDest + ", $" + regX + ", $" + regY);
    }

    @Override
    public void or(byte regDest, byte regX, byte regY)
    {
        out.println("    or  \t$" + regDest + ", $" + regX + ", $" + regY);
    }

    /*--- jump instructions ---*/

    @Override
    public void branchIf(byte reg, boolean value, String label) {
        if (value) {
            out.println("    bnez\t$" + reg + ", "+ label);
        } else {
            out.println("    beqz\t$" + reg + ", "+ label);
        }
    }

    @Override
    public void jump(String label) {
        out.println("    j   \t" + label);
    }

    /* --- procedure calls --- */

    @Override
    public void enterMain()
    {
        ensureSegment(SegmentText);
        out.println(".globl main");
        out.println("main:");
        out.println("    move\t$fp, $sp");
        out.println("    la  \t$" + RegStatic + ", " + StaticDataLabel
                + "\t# pointer to static data");
        stackOffset = 0;
        freeAllReg();
        initRuntimeLib();
    }

    @Override
    public void exitMain(String label)
    {
        out.println(label + ":");
        out.println("    li  \t$v0, 10\t# exit system call");
        out.println("    syscall");
    }

    @Override
    public void enterProc(String label, int nParams) {
		int offset = 2 * WordSize;
		ensureSegment(SegmentText);
		out.println(".globl "+ label);
		out.println(label + ":");
		stackOffset = -offset;
		out.println("    addi\t$sp, $sp, -" + offset);
		out.println("    sw  \t$fp, " + offset + "($sp)");
		out.println("    addi\t$fp, $sp, " + offset);
		out.println("    sw  \t$ra, -4($fp)");
		freeAllReg();
	}

    @Override
	public void exitProc(String label) {
		out.println(label + ":");
		out.println("    lw  \t$ra, -4($fp)");
		out.println("    move\t$sp, $fp");
		out.println("    lw  \t$fp, 0($fp)");
		out.println("    jr  \t$ra");
		/* no need to reset stackOffset */
	}

    @Override
	public void returnFromProc(String label, byte reg) {
		if (reg >= 0) {
			out.println("    move\t$v0, $" + reg);
		}
		out.println("    j   \t" + label);
	}

    @Override
	public void prepareProcCall(int numArgs) {
		procFrameSize = saveRegs(numArgs);
	}

    @Override
    public void passArg(int arg, byte reg) {
		out.println("    sw  \t$" + reg + ", " + (arg+1) * WordSize 
				+ "($sp)" + "\t# arg " + arg);
	}

    @Override
    public void callProc(byte reg, String name) {
		out.println("    jal \t" + name);
		if (reg >= 0)
			out.println("    add \t$" + reg + ", $v0, $zero");
		if (procFrameSize > 0) {
		    restoreRegs(procFrameSize, reg);
		    procFrameSize = 0;
		}
	}

    @Override
    public int paramOffset(int index) {
		return (index+1) * WordSize;
	}

}
