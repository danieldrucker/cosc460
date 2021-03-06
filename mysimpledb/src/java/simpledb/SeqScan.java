package simpledb;

import java.util.*;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 */
public class SeqScan implements DbIterator {

    private static final long serialVersionUID = 1L;
    private TransactionId trans;
    private int tabid;
    private String talias;
    private DbFileIterator it;

    /**
     * Creates a sequential scan over the specified table as a part of the
     * specified transaction.
     *
     * @param tid        The transaction this scan is running as a part of.
     * @param tableid    the table to scan.
     * @param tableAlias the alias of this table (needed by the parser); the returned
     *                   tupleDesc should have fields with name tableAlias.fieldName
     *                   (note: this class is not responsible for handling a case where
     *                   tableAlias or fieldName are null. It shouldn't crash if they
     *                   are, but the resulting name can be null.fieldName,
     *                   tableAlias.null, or null.null).
     */
    public SeqScan(TransactionId tid, int tableid, String tableAlias) {
        this.tabid = tableid;
        this.trans = tid;
        this.talias = tableAlias;
        this.it = Database.getCatalog().getDatabaseFile(this.tabid).iterator(this.trans);
    }

    /**
     * @return return the table name of the table the operator scans. This should
     * be the actual name of the table in the catalog of the database
     */
    public String getTableName() {
        return Database.getCatalog().getTableName(this.tabid);
    }

    /**
     * @return Return the alias of the table this operator scans.
     */
    public String getAlias() {
        return this.talias;
    }

    public SeqScan(TransactionId tid, int tableid) {
        this(tid, tableid, Database.getCatalog().getTableName(tableid));
    }

    public void open() throws DbException, TransactionAbortedException {
        this.it.open();
    }

    /**
     * Returns the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor. This prefix
     * becomes useful when joining tables containing a field(s) with the same
     * name.
     *
     * @return the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor.
     */
    public TupleDesc getTupleDesc() {
        TupleDesc tupD = Database.getCatalog().getTupleDesc(this.tabid);
        Iterator<TupleDesc.TDItem> tupIt = tupD.iterator();
        
        String[] fields = new String[tupD.numFields()];
        Type[] types = new Type[tupD.numFields()];
        int i = 0;
        while (tupIt.hasNext()){
        	TupleDesc.TDItem tdItem = tupIt.next();
        	fields[i] = this.talias + "." + tdItem.fieldName;
        	types[i] = tdItem.fieldType;
        	i++;
        }
        TupleDesc td = new TupleDesc(types, fields);
        return td;
    }

    public boolean hasNext() throws TransactionAbortedException, DbException {
    	//System.out.println("SeqScan");
        return it.hasNext();
    }

    public Tuple next() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        return this.it.next();
    }

    public void close() {
        this.it.close();
    }

    public void rewind() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        this.it.rewind();
    }
}
