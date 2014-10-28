package simpledb;

/**
 * A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

    /**
     * Create a new IntHistogram.
     * <p/>
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * <p/>
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * <p/>
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min     The minimum integer value that will ever be passed to this class for histogramming
     * @param max     The maximum integer value that will ever be passed to this class for histogramming
     */
	
	private int low;
	private int high;
	private int[] bucks;
	private int width;
	private int total;
	
    public IntHistogram(int buckets, int min, int max) {
    	this.low = min;
    	this.high = max;
    	this.total = 0;
    	//System.out.println(buckets);
    	//System.out.println(max);
    	//System.out.println(min);
    	if (buckets > max - min + 1) {
    		this.bucks = new int[max- min +1];
    		this.width = 1;
    	} else {
    		this.bucks = new int[buckets];
    		this.width = (int)Math.floor((max-min+1)/buckets);
    	}

    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     *
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
    	int bucket = findBucket(v);
    	if (bucket < 0) {
    		throw new RuntimeException("Outside of designated Range");
    	}
    	else {
    		this.bucks[bucket]++;
    		this.total++;
    	}
    }
    
    public int findBucket(int v) {
    	if (v < this.low) {
    		return -1;
    	}
    	if (v > this.high) {
    		return -2;
    	}
    	int num = v - this.low;
    	if (num == 0) {
    		return 0;
    	}
    	
    	int len = this.bucks.length;
    	int x = (num/this.width);
    	if (x >= len) {
    		return len-1;
    	} else {
    		return x;
    	}
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * <p/>
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v  Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
    	/*
    	if (v < this.low || v > this.high) {
        	System.out.println("Im in!!!!!!!!!!!");

    		return 0.0;
    	}
    	*/
    	
    	int vBucket = findBucket(v);
    	double eqFraction = -1.0;
    	double gtrFraction = -1.0;
    	double lessFraction = -1.0;
    	if (vBucket < 0) {
    		eqFraction = 0;
    		if (vBucket == -1) {
    			gtrFraction = 1.0;
    			lessFraction = 0;
    		} else {
    			gtrFraction = 0;
    			lessFraction = 1.0;
    		}
    	} else {
    		int len = this.bucks.length;
    		double width_last = (double)this.width + (double)((this.high-this.low+1) % len);
    		if (vBucket == (len - 1)) {
    			eqFraction = ((double)this.bucks[vBucket] / width_last) / (double)this.total ;
    			gtrFraction = (((double)this.bucks[vBucket]/(double)this.total) * (((double)this.high - v) / width_last));
    			lessFraction = 1.0 - (eqFraction + gtrFraction);
    		} else {
    			eqFraction = ((double)this.bucks[vBucket] / (double)this.width) / (double)this.total;
    			int b_right = (vBucket +1) * this.width + this.low;
    			if (v != (b_right-1)){
    				gtrFraction = (((double)this.bucks[vBucket]/(double)this.total) * ((double)(b_right - v-1)/ (double)this.width));
    			} else {
    				gtrFraction = 0.0;
    			}
    			int num_gtr = 0;
    			for (int i = vBucket + 1; i < len; i++) {
    				num_gtr = num_gtr + this.bucks[i];
    			}
    			gtrFraction += ((double)num_gtr / (double)this.total);
    			System.out.println(gtrFraction);
    			System.out.println(eqFraction);
    			lessFraction = 1.0 - (eqFraction + gtrFraction);
    		}
    	}
    	
    	String pred = op.toString();
    	double ret_value = -1.0;
    	
    	switch (pred) {
        case "=":	//equals case
        	ret_value = eqFraction;
        	break;
        case ">":	//greater than case
        	ret_value = gtrFraction;
            break;
        case "<":	//less than case
        	ret_value = lessFraction;
            break;
        case "<=":	//less than or equals to case
        	ret_value = eqFraction + lessFraction;
            break;
        case ">=":	//greater than or equals to case
        	ret_value = gtrFraction + eqFraction;
        	break;
        case "LIKE":	//like case
        	ret_value = eqFraction;
        	break;
        case "<>":	//not equals to case
        	ret_value = 1 - eqFraction;
        	break;
    	}

    	if (ret_value == -1.0 || ret_value > 1.0) {
    		throw new RuntimeException("shouldn't happen: ret_value = -1.0 OR ret_value > 1.0");
    	}
        return ret_value;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        String str = "min value = "+this.low+"| max value = "+this.high+"| total number of tuples = "+this.total+"| number of buckets = "+this.bucks.length+"| bucket width = "+this.width;
        return str;
    }
}
