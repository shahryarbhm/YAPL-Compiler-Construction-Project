package yapl.interfaces;

/**
 * Interface to the <code>Token</code> class generated by JavaCC.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public interface Token {
	
	/**
	 * Return the token kind, as defined by the JavaCC API.
	 */
	public int getKind();

	/**
	 * Return the string representation of the token, as defined
	 * by the JavaCC API.
	 */
	public String toString();
	
	/**
	 * Return the source line number where the token begins.
	 */
	public int line();

	/**
	 * Return the source code column number where the token begins.
	 */
	public int column();
}
