package edu.osu.jwarswap;
import java.util.LinkedList;

public class Statistics {
	static public double zScore(LinkedList<Long> control, long original) {
		/**
		 * Generate a z-score for the null hypothesis that original is drawn from the 
		 * same population as control by assuming a normal distribution of control counts.
		 */
		// Estimate the control mean
		long sum = 0;
		for (long count: control) sum += count;
		double mean = (double) sum / (double) control.size();
		// Estimate the control standard deviation
		double sumSquaredDifferences = 0;
		for (long count: control) {
			double diff = (count - mean);
			sumSquaredDifferences += diff * diff;
		}
		double STD = Math.sqrt(sumSquaredDifferences / (double) (control.size() - 1));
		// compute the z-score
		double zScore  = ((double) original - mean) / STD;
		return zScore;
	}

	public static double pValue(LinkedList<Long> control, long original) {
		/** 
		 * Generate a p-value as the proportion of values in control greater than
		 * or equal to original.
		 */
		// TODO: It would be nice to have a Baysian estimate, though it would be 
		// more difficult to implement and would affect output very little.
		int smaller = 0;
		for (long count: control) if (count < original) smaller++;
		double pValue = 1.0 - ((double) smaller / (double) control.size());
		return pValue;
	}
}
