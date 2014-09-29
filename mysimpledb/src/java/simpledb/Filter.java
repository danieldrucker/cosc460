package simpledb;

import java.util.*;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     *
     * @param p     The predicate to filter tuples with
     * @param child The child operator
     * 
     */
    
    private Predicate pred;
    private DbIterator[] childrenOp;
    
    
    
    public Filter(Predicate p, DbIterator child) {
        this.pred = p;
        this.childrenOp = new DbIterator[]{child};       
        
    }

    public Predicate getPredicate() {
        return this.pred;
    }

    public TupleDesc getTupleDesc() {
        return this.childrenOp[0].getTupleDesc();
    }


    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        super.open();
    }

    public void close() {
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        this.childrenOp[0].rewind();
    }
	

    /**
     * AbstractDbIterator.readNext implementation. Iterates over tuples from the
     * child operator, applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     *
     * @return The next tuple that passes the filter, or null if there are no
     * more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
            TransactionAbortedException, DbException {
    	while (this.childrenOp[0].hasNext()) {
    		System.out.println("here");
    	    Tuple t = this.childrenOp[0].next();
    	    if (this.pred.filter(t)) {
    	        return t;
    	    }
    	}
    	return null;
    }

    @Override
    public DbIterator[] getChildren() {
        return this.getChildren();
    }

    @Override
    public void setChildren(DbIterator[] children) {
        this.setChildren(children);
    }

}
