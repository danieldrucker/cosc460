package simpledb;

import java.io.Serializable;
import java.util.*;


/**
 * Tuple maintains information about the contents of a tuple. Tuples have a
 * specified schema specified by a TupleDesc object and contain Field objects
 * with the data for each field.
 */
public class Tuple implements Serializable {

    private static final long serialVersionUID = 1L;
    TupleDesc tupD;
    RecordId rid;
    Field[] fields;
    

    /**
     * Create a new tuple with the specified schema (type).
     *
     * @param td the schema of this tuple. It must be a valid TupleDesc
     *           instance with at least one field.
     */
    public Tuple(TupleDesc td) {
        if (!(td.numFields() > 0)) {
        	throw new RuntimeException("TupleDesc empty");
        }
        else {
        	this.tupD = td;
        	this.fields = new Field[td.numFields()];
        }
    }

    /**
     * @return The TupleDesc representing the schema of this tuple.
     */
    public TupleDesc getTupleDesc() {
        return this.tupD;
    }

    /**
     * @return The RecordId representing the location of this tuple on disk. May
     * be null.
     */
    public RecordId getRecordId() {
        return this.rid;
    }

    /**
     * Set the RecordId information for this tuple.
     *
     * @param rid the new RecordId for this tuple.
     */
    public void setRecordId(RecordId rid) {
        this.rid = rid;
    }

    /**
     * Change the value of the ith field of this tuple.
     *
     * @param i index of the field to change. It must be a valid index.
     * @param f new value for the field.
     */
    public void setField(int i, Field f) {
        if (i < 0 || i >= this.fields.length) {
        	throw new NoSuchElementException("No element");
        }
        if (!(f.getType().equals(tupD.getFieldType(i)))) {
        	throw new RuntimeException("Types do not match");
        }
        this.fields[i] = f;
    }

    /**
     * @param i field index to return. Must be a valid index.
     * @return the value of the ith field, or null if it has not been set.
     */
    public Field getField(int i) {
        if (i < 0 || i >= this.fields.length) {
        	throw new NoSuchElementException("No element");
        }
        return this.fields[i];
    }

    /**
     * Returns the contents of this Tuple as a string. Note that to pass the
     * system tests, the format needs to be as follows:
     * <p/>
     * column1\tcolumn2\tcolumn3\t...\tcolumnN
     * <p/>
     * where \t is any whitespace, except newline
     */
    public String toString() {
    	String str = "";
    	for (int i = 0; i < this.tupD.numFields(); i++) {
    		System.out.println(this.fields[i]);
    		str += this.fields[i] + "   ";
    	}
    	return str;
    }
}
