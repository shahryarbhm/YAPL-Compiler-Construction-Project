package yapl.impl;

import yapl.interfaces.Symbol;
import yapl.lib.YAPLException;

import java.util.Stack;
import java.util.Hashtable;

/**
 * Implementation of {@link yapl.interfaces.Symboltable}.
 * See the documentation of the member fields for implementation details.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class Symboltable implements yapl.interfaces.Symboltable {

	/** The Hashtable key used internally for storing the parent symbol.
	 * MUST NOT be a valid identifier in YAPL4.
	 */
	protected final static String ParentSymKey = new String("@");
	
	/** Inner class representing a scope. */
	protected class Scope {
		/** The hashtable storing symbols with identifiers as keys. */
		public Hashtable<String, Symbol> table = new Hashtable<String, Symbol>();
		
		/** The parent symbol attached to a scope. */
		public Symbol parentSymbol = null;
		
		/** Are the identifiers of this scope globally visible? */
		public boolean isGlobal = false;
		
		public Scope(boolean isGlobal) {
			this.isGlobal = isGlobal;
		}
	}
	
	/** Stack of scopes.
	 * Each scope is represented by a Hashtable of Symbol objects.
	 * If a link to a parent symbol (eg. function name) is necessary,
	 * it is stored as the value of the key {@link #ParentSymKey} in the Hashtable.
	 */ 
	protected Stack<Scope> scopes;
	
	/** If <code>true</code>, generate debugging output for method calls. */
	protected boolean debugOn = false;
	
	/** Print <code>msg</code> to <code>System.out</code> if <code>debugOn == true</code>.
	 * 
	 * @param method	the calling method of this class.
	 * @param msg		the message to be printed.
	 */
	protected void trace(String method, String msg)
	{
		if (debugOn) {
			System.out.println("Symboltable." + method + ": " + msg);
		}
	}
	
	/** Create a new symbol table.
	 * The global scope is created, too.
	 */
	public Symboltable()
	{
		scopes = new Stack<Scope>();
		scopes.push(new Scope(true));
	}

	public void setDebug(boolean on) {
		debugOn = on;
	}

	public void openScope(boolean isGlobal)
	{
		trace("openScope()", "opening new " + (isGlobal ? "global" : "local") 
				+ " scope");
		scopes.push(new Scope(isGlobal));
	}
	
	public void closeScope()
	{
		scopes.pop();
		trace("closeScope()", "closed scope");
	}
	
	public void addSymbol(Symbol s)
	throws YAPLException
	{
		Hashtable<String, Symbol> sc = scopes.peek().table;  // current scope
		if (s.getName() == null) {
			throw new YAPLException(YAPLException.Internal);
		}
		Symbol symExist = (Symbol) sc.get(s.getName());
		if (symExist != null) {
			throw new YAPLException(YAPLException.SymbolExists, symExist);
		}
		s.setGlobal(scopes.peek().isGlobal);
		trace("addSymbol(Symbol)", "adding symbol " + s);
		sc.put(s.getName(), s);
	}
	
	/** Lookup a symbol in the stack of scopes.
	 * Symbols in an inner scope hide symbols of the same name in an outer scope.
	 * @return <code>null</code> if a symbol of the given name does not exist.
	 * @throws YAPLException	(Internal) if <code>name</code> is <code>null</code>.
	 */
	public Symbol lookup(String name)
	throws YAPLException
	{
		int i;
		Hashtable<String, Symbol> sc;
		Symbol sym;
		try {
			for (i = scopes.size()-1; i >= 0; i--) {
				sc = scopes.elementAt(i).table;
				sym = sc.get(name);
				if (sym != null) {
					trace("lookup(String)", "returning symbol " + sym.getName());
					return sym;
				}
			}
		} catch (Exception e) {
			throw new YAPLException(YAPLException.Internal);
		}
		trace("lookup(String)", "returning null");
		return null;
	}
	
	/** Set the parent symbol of the current scope.
	 * If a parent symbol has already be set, it will be overwritten.
	 * @param sym    the parent symbol (eg. function name).
	 */
	public void setParentSymbol(Symbol sym)
	{
		scopes.peek().parentSymbol = sym;
	}
	
	/** Return the nearest parent symbol of the given kind within the
	 * stack of scopes.
	 * @param kind    one of the <em>kind</em> constants defined by the {@link Symbol} class.
	 * @return <code>null</code> if no appropriate symbol can be found. 
	 */
	public Symbol getNearestParentSymbol(int kind)
	{
		Scope sc;
		int i;
		Symbol sym;
		try {
			for (i = scopes.size()-1; i >= 0; i--) {
				sc = scopes.elementAt(i);
				sym = sc.parentSymbol;
				if (sym != null && sym.getKind() == kind) {
					trace("enclosingSymbol(int)", "returning symbol " + sym.getName());
					return sym;
				}
			}
		} catch (Exception e) {
			/* should not occur */
		}
		trace("enclosingSymbol(int)", "returning null");
		return null;
	}

}
