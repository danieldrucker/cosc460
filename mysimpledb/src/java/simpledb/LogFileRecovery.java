package simpledb;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.*;

/**
 * @author mhay
 */
class LogFileRecovery {

    private final RandomAccessFile readOnlyLog;

    /**
     * Helper class for LogFile during rollback and recovery.
     * This class given a read only view of the actual log file.
     *
     * If this class wants to modify the log, it should do something
     * like this:  Database.getLogFile().logAbort(tid);
     *
     * @param readOnlyLog a read only copy of the log file
     */
    public LogFileRecovery(RandomAccessFile readOnlyLog) {
        this.readOnlyLog = readOnlyLog;
    }

    /**
     * Print out a human readable representation of the log
     */
    public void print() throws IOException {
        // since we don't know when print will be called, we can save our current location in the file
        // and then jump back to it after printing
        Long currentOffset = readOnlyLog.getFilePointer();

        readOnlyLog.seek(0);
        long lastCheckpoint = readOnlyLog.readLong(); // ignore this
        System.out.println("BEGIN LOG FILE");
        while (readOnlyLog.getFilePointer() < readOnlyLog.length()) {
            int type = readOnlyLog.readInt();
            long tid = readOnlyLog.readLong();
            switch (type) {
                case LogType.BEGIN_RECORD:
                    System.out.println("<T_" + tid + " BEGIN>");
                    break;
                case LogType.COMMIT_RECORD:
                    System.out.println("<T_" + tid + " COMMIT>");
                    break;
                case LogType.ABORT_RECORD:
                    System.out.println("<T_" + tid + " ABORT>");
                    break;
                case LogType.UPDATE_RECORD:
                    Page beforeImg = LogFile.readPageData(readOnlyLog);
                    Page afterImg = LogFile.readPageData(readOnlyLog);  // after image
                    System.out.println("<T_" + tid + " UPDATE pid=" + beforeImg.getId() +">");
                    break;
                case LogType.CLR_RECORD:
                    afterImg = LogFile.readPageData(readOnlyLog);  // after image
                    System.out.println("<T_" + tid + " CLR pid=" + afterImg.getId() +">");
                    break;
                case LogType.CHECKPOINT_RECORD:
                    int count = readOnlyLog.readInt();
                    Set<Long> tids = new HashSet<Long>();
                    for (int i = 0; i < count; i++) {
                        long nextTid = readOnlyLog.readLong();
                        tids.add(nextTid);
                    }
                    System.out.println("<T_" + tid + " CHECKPOINT " + tids + ">");
                    break;
                default:
                    throw new RuntimeException("Unexpected type!  Type = " + type);
            }
            long startOfRecord = readOnlyLog.readLong();   // ignored, only useful when going backwards thru log
        }
        System.out.println("END LOG FILE");

        // return the file pointer to its original position
        readOnlyLog.seek(currentOffset);

    }

    /**
     * Rollback the specified transaction, setting the state of any
     * of pages it updated to their pre-updated state.  To preserve
     * transaction semantics, this should not be called on
     * transactions that have already committed (though this may not
     * be enforced by this method.)
     *
     * This is called from LogFile.recover after both the LogFile and
     * the BufferPool are locked.
     *
     * @param tidToRollback The transaction to rollback
     * @throws java.io.IOException if tidToRollback has already committed
     */
    public void rollback(TransactionId tidToRollback) throws IOException {
        long file_start = readOnlyLog.getFilePointer();
    	readOnlyLog.seek(readOnlyLog.length() - LogFile.LONG_SIZE); // undoing so move to end of logfile
        
    	boolean notDone = true;
        int type;
        long tid = tidToRollback.getId();  
        
        while (notDone) {
        	long offset = readOnlyLog.readLong();
        	readOnlyLog.seek(offset);
        	type = readOnlyLog.readInt();
        	long trans = readOnlyLog.readLong();
        	if (trans == tid) {
	        	switch (type) {
		            case LogType.BEGIN_RECORD:
		                if (trans == tid) {
		                	notDone = false;
		                }
		                break;
		            case LogType.COMMIT_RECORD:
		            	throw new IOException("Transaction Already committed");
		            case LogType.ABORT_RECORD:
		                break;
		            case LogType.UPDATE_RECORD:
		            	if (trans == tid) {
		            		System.out.println("found transaction update");
		            		Page beforeImg = LogFile.readPageData(readOnlyLog);
		            		print();

			                int tabid =  beforeImg.getId().getTableId();
			                DbFile f = Database.getCatalog().getDatabaseFile(tabid);
			                beforeImg.setBeforeImage();
			                f.writePage(beforeImg);
			                Database.getBufferPool().discardPage(beforeImg.getId());
			                
			                //write CLR log
			                LogFile clrWrite = Database.getLogFile();
			                clrWrite.logCLR(tid, beforeImg);
			                print();
		            	}
		                break;
		            case LogType.CLR_RECORD:
		                break;
		            case LogType.CHECKPOINT_RECORD:
		                break;
	                }
        	}
	        readOnlyLog.seek(offset - LogFile.LONG_SIZE);
        }
        Database.getLogFile().logAbort(tid);
        readOnlyLog.seek(file_start);
    }

    /**
     * Recover the database system by ensuring that the updates of
     * committed transactions are installed and that the
     * updates of uncommitted transactions are not installed.
     *
     * This is called from LogFile.recover after both the LogFile and
     * the BufferPool are locked.
     */
    public void recover() throws IOException {
    	long file_start = readOnlyLog.getFilePointer();
        long chkpt_offset = readOnlyLog.readLong();
        ArrayList<Long> losers = new ArrayList<Long>();
        ArrayList<Long> undone = new ArrayList<Long>();
        
        if (chkpt_offset != -1) {
            readOnlyLog.seek(chkpt_offset + LogFile.INT_SIZE + LogFile.LONG_SIZE);
            int count = readOnlyLog.readInt();
            for (int i = 0; i < count; i++) {
                long tid = readOnlyLog.readLong();
                losers.add(tid);
            }
            readOnlyLog.readLong();
        } 
        
        long curr_pos = readOnlyLog.getFilePointer();
        
        //redo
        while (curr_pos < readOnlyLog.length()) {
            int type = readOnlyLog.readInt();
            long trans = readOnlyLog.readLong();
            switch (type) {
                case LogType.BEGIN_RECORD:
                    losers.add(trans);
                    break;
                case LogType.COMMIT_RECORD:
                    losers.remove(trans);
                    break;
                case LogType.ABORT_RECORD:
                    losers.remove(trans);
                    break;
                case LogType.UPDATE_RECORD:
                    Page beforeImg = LogFile.readPageData(readOnlyLog);
                    Page afterImg = LogFile.readPageData(readOnlyLog);
                    
                    int tabId = beforeImg.getId().getTableId();
                    DbFile f = Database.getCatalog().getDatabaseFile(tabId);
                    beforeImg.setBeforeImage();
                    f.writePage(afterImg);
                    break;
                case LogType.CLR_RECORD:
                    Page clr_afterImg = LogFile.readPageData(readOnlyLog);
                    int clr_tabId = clr_afterImg.getId().getTableId();
                    DbFile clr_f = Database.getCatalog().getDatabaseFile(clr_tabId);
                    clr_afterImg.setBeforeImage();
                    clr_f.writePage(clr_afterImg);
                    break;
                case LogType.CHECKPOINT_RECORD:
                    throw new IOException("No checkpoint logs should be encountered at this stage");
            }
            readOnlyLog.readLong(); //reads past the offset for the log
            curr_pos = readOnlyLog.getFilePointer();
        }
        
        readOnlyLog.seek(readOnlyLog.length() - LogFile.LONG_SIZE);
        
        //undo
        while (!losers.isEmpty()) {
            long offset = readOnlyLog.readLong();
            readOnlyLog.seek(offset);
            int type = readOnlyLog.readInt();
            long trans = readOnlyLog.readLong();
            if (losers.contains(trans)) {
                switch (type) {
                    case LogType.BEGIN_RECORD:
                        losers.remove(trans);
                        undone.add(trans);
                        break;
                    case LogType.COMMIT_RECORD:
                        break;
                    case LogType.ABORT_RECORD:
                        break;
                    case LogType.UPDATE_RECORD:
                        Page beforeImg = LogFile.readPageData(readOnlyLog);
                        int tabid =  beforeImg.getId().getTableId();
                        DbFile f = Database.getCatalog().getDatabaseFile(tabid);
                        beforeImg.setBeforeImage();
                        f.writePage(beforeImg);
                        
                        //write CLR log
                        LogFile clrWrite = Database.getLogFile();
                        clrWrite.logCLR(trans, beforeImg);
                        break;
                    case LogType.CLR_RECORD:
                        break;
                    case LogType.CHECKPOINT_RECORD:
                        break;
                    }
            }
            readOnlyLog.seek(offset - LogFile.LONG_SIZE);       
        }
        
        for (Long tid: undone) {
            Database.getLogFile().logAbort(tid);
        }
        readOnlyLog.seek(file_start);
    }
}
