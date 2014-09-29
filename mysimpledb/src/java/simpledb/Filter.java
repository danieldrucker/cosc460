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
    private Operator childOp;
    
    
    
    public Filter(Predicate p, DbIterator child) {
        this.pred = p;
        this.childOp = (Operator) child;
        
        
    }

    public Predicate getPredicate() {
        return this.pred;
    }

    public TupleDesc getTupleDesc() {
        return this.childOp.getTupleDesc();
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        this.childOp.open();
    }

    public void close() {
        this.childOp.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
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
        // some code goes here
        return null;
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        return null;
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
    }

}
