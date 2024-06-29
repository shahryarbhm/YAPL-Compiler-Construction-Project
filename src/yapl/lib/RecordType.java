package yapl.lib;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import yapl.interfaces.Symbol;

/**
 * A record data type.
 * The iterator returns Symbol instances representing the record fields.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class RecordType extends Type implements Iterable<Symbol>
{
    private Symbol typeName;
    private LinkedList<Symbol> fields;

    public RecordType(Symbol name)
    {
        this.typeName = name;
        this.fields = new LinkedList<Symbol>();
    }
    
    public Symbol getSymbol()
    {
        return typeName;
    }
    
    public void addField(Symbol s)
    {
        fields.add(s);
    }
    
    public Symbol lookupField(String name)
    {
        for (Symbol s : fields) {
            if (name.equals(s.getName()))
                return s;
        }
        return null;
    }
    
    public int numFields()
    {
        return fields.size();
    }
    
    @Override
    protected boolean isCompatible(Type start, Type other)
    {
        if (!(other instanceof RecordType))
            return false;
        RecordType o = (RecordType) other;
        Iterator<Symbol> i1 = this.fields.iterator();
        Iterator<Symbol> i2 = o.fields.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            Symbol s1 = i1.next();
            Symbol s2 = i2.next();
            if (!s1.getName().equals(s2.getName()))
            	return false;
            /* avoid infinite recursion */
            if (s1.getType() == start) {
            	if (s2.getType() != start)
            		return false;
            }
            else if (!s1.getType().isCompatible(start, s2.getType()))
                return false;
        }
        return i1.hasNext() == i2.hasNext();
    }

    @Override
	public boolean isReference() {
		return true;
	}

	@Override
    public String toString()
    {
        return "record " + typeName.getName();
    }

    @Override
    public Iterator<Symbol> iterator()
    {
        return fields.iterator();
    }

}
