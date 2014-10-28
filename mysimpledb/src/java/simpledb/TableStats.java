package simpledb;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query.
 * <p/>
 * This class is not needed in implementing lab1|lab2|lab3.                                                   // cosc460
 */
public class TableStats {

    private static final ConcurrentHashMap<String, TableStats> statsMap = new ConcurrentHashMap<String, TableStats>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }

    public static void setStatsMap(HashMap<String, TableStats> s) {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 10;

    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     *
     * @param tableid       The table over which to compute statistics
     * @param ioCostPerPage The cost per page of IO. This doesn't differentiate between
     *                      sequential-scan IO and disk seeks.
     */
    private HeapFile f;
    private int ioCostpp;
    private Object[] histograms;
    private int[] mins;
    private int[] maxs;
    private int tuples;
    private int[] distvals;

    
    
    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // some code goes here
    	
    	this.ioCostpp = ioCostPerPage;
    	this.f = (HeapFile) Database.getCatalog().getDatabaseFile(tableid);
    	int numf = f.getTupleDesc().numFields();
    	this.mins = new int[numf];
    	this.maxs = new int[numf];
    	this.histograms = new Object[numf];
    	this.tuples = 0;
    	this.distvals = new int[numf];

    	
    	
    	DbFileIterator it = this.f.iterator(new TransactionId());
    	// initialize all min and max values to the least possible
    	for (int i = 0; i < numf; i++) {
    		this.mins[i] = Integer.MAX_VALUE;
    		this.maxs[i] = Integer.MIN_VALUE;
    	}
    	try {
	    	it.open();
	    	// set min & max values
	    	while (it.hasNext()) {
	    		Tuple tup = it.next();
	    		for (int j = 0; j < numf; j++) {
	    			Field f = tup.getField(j);
	    			int n;
	    			if (f.getType().equals(Type.STRING_TYPE)) {
	    				StringField stringfield = (StringField) f;
	    				n = stringToInt(stringfield.getValue());
	    			} else {
	    				IntField intfield = (IntField) f;
	    				n = intfield.getValue();
	    			}
	    			
	    			if (n < mins[j]) {
	    				mins[j] = n;
	    			}
	    			if (n > maxs[j]) {
	    				maxs[j] = n;
	    			}
	    		}
	    		this.tuples++;
	    	}
	    	it.rewind();
	    	Tuple t = it.next();

	    	// initialize histograms
	    	for (int k = 0; k < numf; k++) {
    			Field f = t.getField(k);
    			if (f.getType().equals(Type.STRING_TYPE)) {
    				this.histograms[k] = new StringHistogram(NUM_HIST_BINS);
    			} else {
    				this.histograms[k] = new IntHistogram(NUM_HIST_BINS, mins[k], maxs[k]);
    			}
    			this.distvals[k] = maxs[k]-mins[k]+1;
    		}
	    	it.rewind();
	    	
	    	// populate histograms
	    	while (it.hasNext()) {
	    		Tuple tup = it.next();
	    		for (int l = 0; l < numf; l++) {
	    			Field f = tup.getField(l);
	    			int n;
	    			if (f.getType().equals(Type.STRING_TYPE)) {
	    				StringField stringfield = (StringField) f;
	    				String s = stringfield.getValue();
	    				StringHistogram sh = (StringHistogram)this.histograms[l];
	    				sh.addValue(s);
	    				this.histograms[l] = sh;
	    			} else {
	    				IntField intfield = (IntField) f;
	    				n = intfield.getValue();
	    				IntHistogram ih = (IntHistogram)this.histograms[l];
	    				ih.addValue(n);
	    				this.histograms[l] = ih;
	    			}
	    		}
	    	}
	    	it.rewind();
	    	it.close();
	    	
    	} catch (Exception ex) {
    		System.out.println(ex);
    		ex.printStackTrace();
    	}
    	
    	
    }

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * <p/>
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     *
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        return this.f.numPages() * this.ioCostpp;
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     *
     * @param selectivityFactor The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     * selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        if (selectivityFactor > 0 && selectivityFactor < 1/tuples) {
        	return 1;
        }
        return (int)(this.tuples * selectivityFactor);
    }

    /**
     * This method returns the number of distinct values for a given field.
     * If the field is a primary key of the table, then the number of distinct
     * values is equal to the number of tuples.  If the field is not a primary key
     * then this must be explicitly calculated.  Note: these calculations should
     * be done once in the constructor and not each time this method is called. In
     * addition, it should only require space linear in the number of distinct values
     * which may be much less than the number of values.
     *
     * @param field the index of the field
     * @return The number of distinct values of the field.
    */
    
    public int numDistinctValues(int field) {
    	return this.distvals[field];
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     *
     * @param field    The field over which the predicate ranges
     * @param op       The logical operation in the predicate
     * @param constant The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     * predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
		if (constant.getType().equals(Type.STRING_TYPE)) {
			StringField stringfield = (StringField) constant;
			String s = stringfield.getValue();
			StringHistogram sh = (StringHistogram)this.histograms[field];
			return sh.estimateSelectivity(op, s);
		} else {
			IntField intfield = (IntField) constant;
			int n = intfield.getValue();
			IntHistogram ih = (IntHistogram)this.histograms[field];
			return ih.estimateSelectivity(op, n);
		}
    }

    /**
     * Convert a string to an integer, with the property that if the return
     * value(s1) < return value(s2), then s1 < s2
     */
    public int stringToInt(String s) {
        int i;
        int v = 0;
        for (i = 3; i >= 0; i--) {
            if (s.length() > 3 - i) {
                int ci = (int) s.charAt(3 - i);
                v += (ci) << (i * 8);
            }
        }

        // XXX: hack to avoid getting wrong results for
        // strings which don't output in the range min to max
        if (!(s.equals("") || s.equals("zzzz"))) {
            if (v < minVal()) {
                v = minVal();
            }

            if (v > maxVal()) {
                v = maxVal();
            }
        }

        return v;
    }
    
    /**
     * @return the maximum value indexed by the histogram
     */
    int maxVal() {
        return stringToInt("zzzz");
    }

    /**
     * @return the minimum value indexed by the histogram
     */
    int minVal() {
        return stringToInt("");
    }
}
