package concurrency_tutorial;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import simpledb.PageId;
import simpledb.TransactionId;

public class LockManagerDemo {
    private static final LockManager lm = new LockManager();
    public static LockManager getLockManager() { return lm; }
    /**
     * This main program spawns a bunch of Incrementer threads
     * at the same time.  All Incrementers are modifying the same
     * shared Counter object.  Since they are all running at the
     * same time, we should see some thread interference (aka
     * race conditions) as they try to modify the
     */
    public static void main(String args[]) throws InterruptedException {
        final Counter counter = new Counter();
        int numThreads = 20;
        final int numAdds = 10;
        for (int i = 0; i < numThreads; i++) {
            new Thread(new Incrementer(counter, numAdds, i + 1)).start();
        }
        Thread.sleep(1000);  // pauses main for 1000 msec to make sure all threads have finished before we getCount
        int expectedCount = numThreads * numAdds;
        int actualCount = counter.getCount();
        if (actualCount != expectedCount) {
            System.out.println("Thread interference!  Counter is " + actualCount + " but should be " + expectedCount + ".");
        } else {
            System.out.println("No thread interference detected. Counter is " + actualCount + ".");
        }
    }

    static class Counter {
        private int count = 0;

        public void increment(String name) {
            int currCount = count;  // read
            // introduce a delay between read and write to "encourage" race conditions
            System.out.println("Shared counter incremented by " + name + ".");
            count = currCount + 1;  // write
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Incrementer is a thread that has a reference to a shared Counter
     * object.  Once this thread starts, it increments the shared Counter
     * several times.
     */
    static class Incrementer implements Runnable {

        private final Counter counter;
        private final int numIncrements;
        private final String name;

        public Incrementer(Counter counter, int numIncrements, int index) {
            this.name = "Thread " + index;
            this.counter = counter;
            this.numIncrements = numIncrements;
        }

        public void run() {
            // increment the counter numIncrements times
            for (int i = 0; i < numIncrements; i++) {
                System.out.println(name + " attempting to acquire lock.");
                LockManagerDemo.getLockManager().acquireLock();
                System.out.println(name + " acquired lock.");
                counter.increment(name);
                LockManagerDemo.getLockManager().releaseLock();
                System.out.println(name + " released lock.");
            }
        }
    }

    static class LockManager {
	    private boolean inUse = false;
	
	    public synchronized void acquireLock() {
	        while (inUse) {
	            try {
	                wait();
	            } catch (InterruptedException e) {}
	        }
	        inUse = true;
	        notifyAll();
	    }
	        
	
	    public synchronized void releaseLock() {
	        inUse = false;
	        notifyAll();
	    }
	}
}

/*

static class LockTable {
	
    private ConcurrentHashMap<PageId,LinkedList<Lock>> map;

    public LockTable() {
    	map = new ConcurrentHashMap<PageId, LinkedList<Lock>>();
    }
    
    public synchronized void acquireLock(TransactionId tid, PageId pid) {
    	if (!map.containsKey(pid)) {
    		LinkedList<Lock> l = new LinkedList<Lock>();
    		l.add(new Lock(tid, true));
    		map.put(pid, l);
    	}
    	else {
    		LinkedList<Lock> list = map.get(pid);
    		if (list.isEmpty()) {
    			list.add(new Lock(tid, true));
    			map.put(pid, list);
    		}
    		else {
    			Lock x = new Lock(tid, false);
    			list.add(x);
    			map.put(pid, list);
    			while (map.get(pid).getFirst() != x) {
    	            try {
    	                wait();
    	            } catch (InterruptedException e) {}
    	        }
    			map.get(pid).getFirst().takeLock();
    	        notifyAll();
    		}	
    	}
    }
    
    public synchronized void releaseLock(TransactionId tid, PageId pid) {
    	if (!map.containsKey(pid)) {
    		throw new NoSuchElementException("Lock isn't being used!");
    	}
    	else {
    		LinkedList<Lock> list = map.get(pid);
    		if (list.isEmpty()) {
    			throw new NoSuchElementException("There is no transaction in the linked list!");
    		}
    		else {
    			Lock l = list.remove();
    			if (tid != l.getTid()) {
    				throw new NoSuchElementException("The transaction ID's don't match, does not hold the lock");
    			}
    			map.put(pid, list);
    	        notifyAll();
    		}	
    	}
    }
    
    public boolean hasLock(TransactionId tid, PageId pid) {
    	if (!map.containsKey(pid)) {
    		throw new NoSuchElementException("No transaction holds lock for this page");
    	} 
    	else {
    		LinkedList<Lock> list = map.get(pid);
    		if (list.isEmpty()) {
    			return false;
    		}
    		else {
    			if (list.getFirst().getTid() != tid) {
    				return false;
    			}
    			return true;
    		}
    	}
    }
    
    
    public class Lock {
    	private TransactionId transId;
    	private boolean holdslock;
    	
    	public Lock(TransactionId tid, boolean l) {
    		transId = tid;
    		holdslock = l;
    	}
    	
    	public TransactionId getTid() {
    		return transId;
    	}
    	
    	public boolean holdsL() {
    		return holdslock;
    	}
    	
    	public void releaseLock() {
    		holdslock = false;
    	}
    	
    	public void takeLock() {
    		holdslock = true;
    	}
    	
    }
}

*/

