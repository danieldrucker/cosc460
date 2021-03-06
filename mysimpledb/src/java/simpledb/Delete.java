package simpledb;

import java.io.IOException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     *
     * @param t     The transaction this delete runs in
     * @param child The child operator from which to read tuples for deletion
     */
    
    private TransactionId tid;
    private DbIterator[] childDb;
    private Tuple result;
    private int deleted;
    private boolean canRun;
    
    public Delete(TransactionId t, DbIterator child) {
    	this.canRun = true;
    	this.deleted = 0;
        this.tid = t;
        this.childDb = new DbIterator[]{child};
        Type types[] = new Type[]{Type.INT_TYPE};
        String names[] = new String[]{"deleted"};
        this.result = new Tuple(new TupleDesc(types, names));
    }

    public TupleDesc getTupleDesc() {
        return this.childDb[0].getTupleDesc();
    }

    public void open() throws DbException, TransactionAbortedException {
        this.childDb[0].open();
        super.open();
    }

    public void close() {
        this.childDb[0].close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        this.childDb[0].rewind();
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     *
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        BufferPool bp = Database.getBufferPool();
        if (!this.canRun) {
        	return null;
		}
        while (this.childDb[0].hasNext()) {
            Tuple t = this.childDb[0].next();
            try {
                bp.deleteTuple(this.tid, t);
                this.deleted++;
            } catch(IOException e) {
                throw new DbException("DbException: unable to delete tuple");
            }
        }
        this.result.setField(0, new IntField(this.deleted));
        this.canRun = false;
        return this.result;
    }

    @Override
    public DbIterator[] getChildren() {
        return this.childDb;
    }

    @Override
    public void setChildren(DbIterator[] children) {
        for (int i = 0; i < children.length; i++) {
            this.childDb[i] = children[i];
        }
    }

}