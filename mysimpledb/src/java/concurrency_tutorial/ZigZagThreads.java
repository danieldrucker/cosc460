package concurrency_tutorial;

public class ZigZagThreads {
    private static final LockManager lm = new LockManager();
    public static LockManager getLockManager() { return lm; }

    public static void main(String args[]) throws InterruptedException {
        int numZigZags = 10;
        for (int i = 0; i < numZigZags; i++) {
            new Thread(new Zigger()).start();
        }
        for (int i = 0; i < numZigZags; i++) {
            new Thread(new Zagger()).start();
        }
    }

    static class Zigger implements Runnable {

        protected String myPattern;
        protected boolean isZigger;

        public Zigger() {
            myPattern = "//////////";
            isZigger = true;
        }

        public void run() {
            ZigZagThreads.getLockManager().acquireLock(isZigger);
            System.out.println(myPattern);
            ZigZagThreads.getLockManager().releaseLock();
        }
    }

    static class Zagger extends Zigger {

        public Zagger() {
            myPattern = "\\\\\\\\\\\\\\\\\\\\";
            isZigger = false;
        }

    }

    static class LockManager {
        private boolean inUse = false;
        private boolean needZig = true;

        private synchronized boolean isLockFree(boolean isZigger) {
            if (((isZigger && needZig) || !(isZigger && needZig)) && !inUse){
            	return true;
            } else {
            	return false;
            }
        }

        public void acquireLock(boolean isZigger) {
            boolean waiting = true;
            while (waiting) {
                synchronized (this) {
                    // check if lock is available
                    if (!inUse) {
                        // it's not in use, so we can take it!
                    	if (isZigger && needZig) {
                    		needZig = false;
                            inUse = true;
                            waiting = false;
                    	}
                    	else if (!isZigger && !needZig) {
                    		needZig = true;
                            inUse = true;
                            waiting = false;
                    	}
                    }
                }
                if (waiting) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) { }
                }
            }
        }


        public synchronized void releaseLock() {
            inUse = false;
        
        }
    }


}