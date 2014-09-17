package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see simpledb.HeapPage#HeapPage
 */
public class HeapFile implements DbFile {
	private File file;
	private TupleDesc tupD;

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    public HeapFile(File f, TupleDesc td) {
        this.file = f;
        this.tupD = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
    	return this.file.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return this.tupD;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
    	try {
            int pagenum = pid.pageNumber();
            int pSize = BufferPool.getPageSize();
            byte[] page = new byte[pSize];
            InputStream input = new FileInputStream(this.file);
            input.skip(BufferPool.getPageSize() * pagenum);
            
            for (int i = 0; i < pSize; i++ ) {
                byte data = (byte) input.read();
                page[i] = data; 
            }
            input.close();
            HeapPageId hpID = (HeapPageId) pid;
            HeapPage p = new HeapPage(hpID, page);
            return p;
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
        
        return null;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        int n = (int)this.file.length() / BufferPool.getPageSize();
        return n;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
    	return new hpIterator(tid);
    }
    
    class hpIterator implements DbFileIterator {
    	
    	private Iterator<Tuple> it;
    	private int currPage;
    	private int numPage;
    	private BufferPool bp;
    	private boolean status;
    	private TransactionId t;


        public hpIterator(TransactionId tid) {
        	this.status = false;
        	this.numPage = numPages();
        	this.currPage = 0;
        	this.bp = Database.getBufferPool();
        	this.t = tid;
        }
        
        private void getNext() throws DbException, TransactionAbortedException {
        	HeapPageId pid = new HeapPageId(getId(), this.currPage);
        	HeapPage p = (HeapPage) this.bp.getPage(this.t, pid, null);
        	this.it = p.iterator();
        	this.currPage++;
        }
        
        public void open() throws DbException, TransactionAbortedException {
        	this.getNext();
        	this.status = true;
        }
        
        public void close() {
        	this.status = false;
        }
        
        public void rewind() throws DbException, TransactionAbortedException {
            if (!this.status) {
                throw new DbException("Iterator Already closed");
            }
            this.currPage = 0;
            getNext();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
        	if (!this.status) {
        		return false;
        	}
        	if (this.it.hasNext()) {
        		return true;
        	}
        	if (this.currPage  < this.numPage) {
        		return true;
        	}
        	return false;
        }


        @Override
        public Tuple next() throws DbException, TransactionAbortedException {
            if ((!hasNext()) || this.status == false) {
            	throw new NoSuchElementException("No next or not open");
            }
            if (this.it.hasNext()) {
            	return this.it.next();
            }
            else {
            	getNext();
            	return this.it.next();
            }
        }
    }

}

