package simpledb;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p/>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /**
     * Bytes per page, including header.
     */
    public static final int PAGE_SIZE = 4096;

    private static int pageSize = PAGE_SIZE;

    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;
    
    private int pageLimit;
    private ConcurrentHashMap<PageId,Page> bp;
    private LinkedList<PageId> cleanQueue;
    private LinkedList<PageId> dirtyQueue;
    
    // For lock Manager
    private static final LockManager lm = new LockManager();
    public static LockManager getLockManager() { return lm; }
    

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        this.pageLimit = numPages;
        this.bp = new ConcurrentHashMap<PageId,Page>();
        this.cleanQueue = new LinkedList<PageId>();
        this.dirtyQueue = new LinkedList<PageId>();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p/>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm) throws TransactionAbortedException, DbException {
    	BufferPool.getLockManager().lockRequest(tid, pid, perm);
        synchronized (this) {
        	System.out.println("Clean queue:  " + this.cleanQueue.toString());
        	System.out.println("Dirty queue:  " + this.dirtyQueue.toString());
	    	if (this.bp.containsKey(pid)) {
	    		HeapPage hp = (HeapPage) bp.get(pid); 
	    		if (hp.isDirty() == tid) {
	    			this.dirtyQueue.remove(pid);
	    			this.dirtyQueue.addFirst(pid);	    		
	    		} else if (hp.isDirty() == null && perm == Permissions.READ_ONLY) {
	    			this.cleanQueue.remove(pid);
	    			this.cleanQueue.addFirst(pid);
	    		} else {
	    			this.cleanQueue.remove(pid);
	    			this.dirtyQueue.addFirst(pid);
	    		}
	    		return this.bp.get(pid);
	        } else {
	            Catalog c = Database.getCatalog();
	            DbFile f = c.getDatabaseFile(pid.getTableId());
	            Page p = f.readPage(pid);
	            System.out.println(this.bp.size() + "   and page limit   " + this.pageLimit);
	            if (this.bp.size() >= this.pageLimit) {
	                evictPage();
	            }
	            //System.out.println(perm);
	            this.cleanQueue.addFirst(pid);
	            /*
	            if (perm == Permissions.READ_ONLY) {
	            	this.cleanQueue.addFirst(pid);
	            } else {
	            	this.dirtyQueue.addFirst(pid);
	            }
	            */
	            this.bp.put(pid, p);
	            return p;
	        }
        }
    }


    
    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void releasePage(TransactionId tid, PageId pid) {
        BufferPool.getLockManager().lockRelease(tid, pid);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
    	transactionComplete(tid, true);                                                        // cosc460
    }

    /**
     * Return true if the specified transaction has a lock on the specified page
     */
    public boolean holdsLock(TransactionId tid, PageId p) {
        return BufferPool.getLockManager().hasLock(tid, p);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
    	if (commit) {
    		flushPages(tid);
    	} else {
    		Catalog c = Database.getCatalog();
    		LinkedList<PageId> temp = this.dirtyQueue;
        	while (!temp.isEmpty()) {
        		PageId pid = temp.removeLast();
	            DbFile f = c.getDatabaseFile(pid.getTableId());
	            HeapPage hp = (HeapPage) bp.get(pid);
        		if (hp.isDirty() == tid) {
    	            Page p = f.readPage(pid);
    	            this.bp.put(pid, p);
        			this.dirtyQueue.remove(pid);
        			this.cleanQueue.add(pid);
        		}
        	}
    	}
    	ArrayList<PageId> releaseHold = BufferPool.getLockManager().thold.get(tid);
    	ArrayList<PageId> releaseReq = BufferPool.getLockManager().twait.get(tid);
    	for (PageId pageId : releaseHold) {
    		releasePage(tid, pageId);
    	}
    	if (releaseReq != null) {
    		for (PageId pageId : releaseReq) {
    			releaseRequest(tid, pageId);
    		}
    	}
    }
    
    public void releaseRequest(TransactionId tid, PageId pid) {
    	BufferPool.getLockManager().updateTwait(tid, pid, false);
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed until lab5).                                  // cosc460
     * May block if the lock(s) cannot be acquired.
     * <p/>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have
     * been dirtied so that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        Catalog c = Database.getCatalog();
        DbFile f = c.getDatabaseFile(tableId);
        ArrayList<Page> newPage = f.insertTuple(tid, t);
        newPage.get(0).markDirty(true, tid);
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p/>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have
     * been dirtied so that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        int tabID = t.getRecordId().getPageId().getTableId();
        Catalog c = Database.getCatalog();
        DbFile f = c.getDatabaseFile(tabID);
        ArrayList<Page> newPage = f.deleteTuple(tid, t);
        newPage.get(0).markDirty(true, tid);
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        for (PageId pid : bp.keySet()) {
            HeapPage hp = (HeapPage)bp.get(pid);
            if (hp.isDirty() != null) {
                flushPage(pid);
            }
        }

    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // only necessary for lab6                                                                            // cosc460
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        DbFile f = Database.getCatalog().getDatabaseFile(pid.getTableId());
        HeapPage hp = (HeapPage) bp.get(pid); 
        if (hp == null){
            throw new IOException("page not found in BufferPool");
        }
        hp.markDirty(false, hp.isDirty());
		this.dirtyQueue.remove(pid);
		this.cleanQueue.addFirst(pid);
        f.writePage(hp);
    }    

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
    	LinkedList<PageId> temp = this.dirtyQueue;
    	while (!temp.isEmpty()) {
    		PageId pid = temp.removeLast();
    		HeapPage hp = (HeapPage) bp.get(pid);
    		if (hp.isDirty() == tid) {
    			flushPage(pid);
    			System.out.println("flushed page - " + pid.toString());
    		}
    	}
        
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
    	//System.out.println(this.dirtyQueue.toString());
        if (this.cleanQueue.isEmpty()) {
        	throw new DbException("no clean pages to evict");
        }
    	PageId pid = this.cleanQueue.removeLast();
        HeapPage hp = (HeapPage) bp.get(pid);
        //if (hp.isDirty() == null) {throw new DbException("Dirty page in clean cue"); }
        try {
            flushPage(pid);
			System.out.println("flushed page - " + pid.toString());
            this.bp.remove(pid);
            this.cleanQueue.remove(pid);
        }
        catch (IOException e) {
            throw new DbException("could not flush page");
        }
    	System.out.println("Clean queue:  " + this.cleanQueue.toString());
    	System.out.println("Dirty queue:  " + this.dirtyQueue.toString());
    }
    

    static class LockManager {
        
        private HashMap<PageId, LockEntry> lt;
        private HashMap<TransactionId, ArrayList<PageId>> twait;
        private HashMap<TransactionId, ArrayList<PageId>> thold;
        
        public LockManager() {
            lt = new HashMap<PageId, LockEntry>();
            twait = new HashMap<TransactionId, ArrayList<PageId>>();
            thold = new HashMap<TransactionId, ArrayList<PageId>>();
        }
        
        public void lockRequest(TransactionId tid, PageId pid, Permissions p) {
            boolean waiting = true;
            
            while (waiting) {

                synchronized (this) {
                    LockEntry l = lt.get(pid);
                    if (l == null) {
                        l = new LockEntry(pid);
                        l.addTid(tid);
                        l.setType(p);
                        lt.put(pid, l);
                        System.out.println("Update thold1");
                        updateThold(tid, pid, true);
                        waiting = false;
                    } else {
                    	if (hasLock(tid, pid) && ((l.getType() == p) || (l.getType() == Permissions.READ_WRITE && p == Permissions.READ_ONLY))) {
                    		waiting = false;
                    	}
                    	else if (hasLock(tid, pid) && l.getType() == Permissions.READ_ONLY && p == Permissions.READ_WRITE) {
                    		//l.addUpgradeRequest(tid);
                            //lt.put(pid, l);
                            System.out.println("Update twait1");
                            //updateTwait(tid, pid, true);
                    		waiting = false;
                    	}
                    	else if (l.getType() == null) {
                            l.addTid(tid);
                            l.setType(p);
                            lt.put(pid, l);
                            updateThold(tid, pid, true);
                            waiting = false;
                        }
                        else if (l.getType() == p && p == Permissions.READ_ONLY) {
                            l.addTid(tid);
                            lt.put(pid, l);
                            updateThold(tid, pid, true);
                            waiting = false;
                        } 
                        else if (l.getType() == Permissions.READ_WRITE || p == Permissions.READ_WRITE) {
                        	//System.out.println(p);
                        	//System.out.println("Holding:    " + l.holding.toString());
                        	//System.out.println("Requesting:    " + l.requesting.toString());
                            updateTwait(tid, pid, true);
                            System.out.println("Add request");
                            l.addRequest(tid);
                            lt.put(pid, l);
                            //if (!l.inUse()) { System.out.println("Not in use"); }
                            //System.out.println("tid is:    " + tid + "   "+ "requesting is:   " +  lt.get(pid).requesting.toString());
                            if (!l.inUse() && (tid == lt.get(pid).requesting.getFirst())) {   
                                System.out.println("Got exit");
                                
                                l.addTid(tid);
                                l.setType(p);
                                l.removeRequest();
                                lt.put(pid, l);
                                updateTwait(tid, pid, false);
                                updateThold(tid, pid, true);
                                l.setUse(true);
                                waiting = false;
                            }
                        }
                    }
                }
                if (waiting) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {}
                }
            }
        }
        
        public void lockRelease(TransactionId tid, PageId pid) {
        	System.out.println("Balling!");
            LockEntry l = lt.get(pid);
            if (!l.holding.contains(tid)) {
                return;
            }
            synchronized (this) {
                if (!(l == null)) {
                    updateThold(tid, pid, false);
                    l.removeTid(tid);
                    lt.put(pid, l);
                    l.setUse(false);
                }
            }
        }
        

        public boolean hasLock(TransactionId tid, PageId pid) {
        	if (lt.containsKey(pid)) {
        		LockEntry l = lt.get(pid);
        		if (l.holding.contains(tid)) {
        			return true;
        		}
        	}
        	return false;
        }
        
        
        public void updateTwait(TransactionId tid, PageId pid, boolean io) {
            if (io) {
                if (!twait.containsKey(tid)) {
                    ArrayList<PageId> list = new ArrayList<PageId>();
                    list.add(pid);
                    twait.put(tid, list);
                } else {
                    ArrayList<PageId> list = twait.get(tid);
                    list.remove(pid);
                    list.add(pid);
                    twait.put(tid, list);
                }
            } else {
                ArrayList<PageId> list = twait.get(tid);
                twait.remove(pid);
                twait.put(tid, list);
            }
        }
        
        public void updateThold(TransactionId tid, PageId pid, boolean io) {
            if (io) {
                if (!thold.containsKey(tid)) {
                    ArrayList<PageId> list = new ArrayList<PageId>();
                    list.add(pid);
                    thold.put(tid, list);
                } else {
                    ArrayList<PageId> list = thold.get(tid);
                    list.remove(pid);
                    list.add(pid);
                    thold.put(tid, list);
                }
            } else {
                ArrayList<PageId> list = thold.get(tid);
                thold.remove(pid);
                thold.put(tid, list);
            }
        }
        
        public class LockEntry {
            
            private ArrayList<TransactionId> holding;
            private Permissions lockType;
            private LinkedList<TransactionId> requesting;
            private boolean inUse;
            
            public LockEntry(PageId pid) {
                holding = new ArrayList<TransactionId>();
                lockType = null;
                requesting = new LinkedList<TransactionId>();
                inUse = false;
            }
            
            public boolean inUse() {
            	return inUse;
            }
            
            public void setUse(boolean u) {
            	inUse = u;
            }
             
            public void setType(Permissions perm) {
                lockType = perm;
            }
            
            public Permissions getType(){
                return lockType;
            }
            
            public void addRequest(TransactionId tid) {
            	requesting.remove(tid);
                requesting.add(tid);
            }
            
            public TransactionId getFirst() {
                return requesting.getFirst();
            }
            
            public void removeRequest() {
                requesting.removeFirst();
            }
            
            public void removeTid(TransactionId tid) {
                holding.remove(tid);
                if (holding.isEmpty()) {
                    lockType = null;
                }
            }
            
            public void addTid(TransactionId tid) {
            	holding.remove(tid);
                holding.add(tid);
            }
            
            public void addUpgradeRequest(TransactionId tid) {
            	requesting.remove(tid);
                requesting.addFirst(tid);
            }
        }
        
    }  
}