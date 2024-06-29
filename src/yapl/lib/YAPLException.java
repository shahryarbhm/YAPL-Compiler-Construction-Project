package yapl.lib;
import yapl.interfaces.CompilerError;
import yapl.interfaces.Symbol;
import yapl.interfaces.Token;

/** 
 * Exception type implementing a YAPL compiler error condition
 * other than a syntax or lexical error.
 *
 * @author Mario Taschwer
 * @version $Id:YAPLException.java 52 2008-03-04 09:02:13Z mt $
 */
public class YAPLException extends Exception implements CompilerError
{
    private static final long serialVersionUID = -462157652628403847L;
	
	private int kind;
	private Token token;
	private Symbol symbol;
	private int intVal;

	/* Constructor methods */
	
	/** Create a new exception.
 	 * @param errorNumber	one of the error constants defined by the CompilerError interface.
	 */
	public YAPLException(int errorNumber)
	{
		this.kind = errorNumber;
	}
	
	/** Create a new exception related to the given token.
	 * @param kind		one of the kind constants defined by this class.
	 * @param token		the related token.
	 */
	public YAPLException(int kind, Token token)
	{
		this.kind = kind;
		this.token = token;
	}
	
	/** Create a new exception related to the given symbol.
	 * 
	 * @param kind		one of the kind constants defined by this class.
	 * @param symbol	the related symbol.
	 */
	public YAPLException(int kind, Symbol symbol)
	{
		this.kind = kind;
		this.symbol = symbol;
	}
	
	/** Create a new exception related to the given token and symbol.
	 * 
	 * @param kind		one of the kind constants defined by this class.
	 * @param token		the related token.
	 * @param symbol	the related symbol.
	 */
	public YAPLException(int kind, Token token, Symbol symbol)
	{
		this.kind = kind;
		this.token = token;
		this.symbol = symbol;
	}
	
	/** Create a new exception related to the given symbol and integer.
	 * @param kind		one of the kind constants defined by this class.
	 * @param symbol	the related symbol.
	 * @param value		the related integer.
	 */
	public YAPLException(int kind, Symbol symbol, int value)
	{
		this.kind = kind;
		this.symbol = symbol;
		this.intVal = value;
	}
	
	/**
	 * Inform this object about the last token consumed by the parser.
	 * This will be used as the error location within the source code
	 * if this information is not already available with this object.
	 * @param token    the last token consumed by the parser.
	 */
	public void setLastToken(Token token)
	{
		if (this.token == null)
			this.token = token;
	}
	
	public String getMessage()
	{
		StringBuffer buf = new StringBuffer();
		switch (kind) {
		case Internal:
			buf.append("internal error\n");
			break;
		case SymbolExists:
			buf.append("symbol '");
			buf.append(symbol.getName());
			buf.append("' already declared in current scope (as ");
			buf.append(symbol.getKindString());
			buf.append(")");
			break;
		case IdentNotDecl:
			buf.append("identifier '");
			buf.append(token.toString());
			buf.append("' not declared");
			break;
		case SymbolIllegalUse:
			buf.append("illegal use of ");
			buf.append(symbol.getKindString());
			buf.append(" '");
			buf.append(symbol.getName());
			buf.append("'");
			break;
        case EndIdentMismatch:
            buf.append("End ");
            buf.append(token.toString());
            buf.append(" does not match ");
            buf.append(symbol.getKindString());
            buf.append(' ');
            buf.append(symbol.getName());
            break;
        case SelectorNotArray:
            buf.append("expression before '[' is not an array type");
            break;
        case BadArraySelector:
            buf.append("array index or dimension is not an integer type");
            break;
        case ArrayLenNotArray:
            buf.append("expression after '#' is not an array type");
            break;
		case IllegalOp1Type:
			buf.append("illegal operand type for unary operator ");
			buf.append(token.toString());
			break;
		case IllegalOp2Type:
			buf.append("illegal operand types for binary operator ");
			buf.append(token.toString());
			break;
		case IllegalRelOpType:
			buf.append("non-integer operand types for relational operator ");
			buf.append(token.toString());
			break;
		case IllegalEqualOpType:
			buf.append("illegal operand types for equality operator ");
			buf.append(token.toString());
			break;
        case ProcNotFuncExpr:
            buf.append("using procedure ");
            buf.append(symbol.getName());
            buf.append(" (not a function) in expression");
            break;
        case ReadonlyAssign:
        	buf.append("read-only l-value in assignment");
        	break;
		case TypeMismatchAssign:
			buf.append("type mismatch in assignment");
			break;
		case ArgNotApplicable:
			buf.append("argument #");
			buf.append(intVal);
			buf.append(" not applicable to procedure ");
			buf.append(symbol.getName());
			break;
		case ReadonlyArg:
			buf.append("read-only argument #");
			buf.append(intVal);
			buf.append(" passed to read-write procedure ");
			buf.append(symbol.getName());
			break;
		case TooFewArgs:
			buf.append("too few arguments for procedure ");
			buf.append(symbol.getName());
			break;
        case CondNotBool:
            buf.append("condition is not a boolean expression");
            break;
        case ReadonlyNotReference:
        	buf.append("type qualified as readonly is not a reference type");
        	break;
		case MissingReturn:
			buf.append("missing Return statement in function ");
			buf.append(symbol.getName());
			break;
        case InvalidReturnType:
            buf.append("returning none or invalid type from function ");
            buf.append(symbol.getName());
            break;
        case IllegalRetValProc:
            buf.append("illegal return value in procedure ");
            buf.append(symbol.getName());
            buf.append(" (not a function)");
            break;
        case IllegalRetValMain:
            buf.append("illegal return value in main program");
            break;
        case SelectorNotRecord:
            buf.append("expression before . is not a record type");
            break;
        case InvalidRecordField:
            buf.append("invalid field ");
            buf.append(token.toString());
            buf.append(" of record ");
            buf.append(symbol.getName());
            break;
        case InvalidNewType:
            buf.append("invalid type used with 'new'");
            break;
		default:
			buf.append("internal error: unknown YAPLException kind ");
			buf.append(kind);
			break;
		}
		return buf.toString();
	}
	
	/* Methods implementing the CompilerError interface. */
	
	public int errorNumber()
	{
		return this.kind;
	}
	
	public int line()
	{
		return (this.token == null) ? -1 : this.token.line();
	}
	
	public int column()
	{
		return (this.token == null) ? -1 : this.token.column();
	}
}