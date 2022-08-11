package edu.osu.jwarswap;
import java.util.Arrays;
import java.util.Random;
import java.lang.Math;
import peersim.util.WeightedRandPerm;
import it.unimi.dsi.fastutil.doubles.DoubleIndirectHeaps;
import it.unimi.dsi.fastutil.doubles.DoubleComparators;

public class UnitTest {
	public static void main(String[] args) {
		Random random = new Random();
//		int[][] arr1 = {{0, 1}, {2, 3}};
//		int[][] arr2 = new int[5][2];
//		System.arraycopy(arr1, 0, arr2, 0, arr1.length);
//		for (int i = 0; i < 5; i++) {
//			System.out.println(arr2[i][0] + " " + arr2[i][1]);
//		}
//		testHeap(random);
//
//		int sampleSize = 20000;
//		long t1 = System.currentTimeMillis();
//		testHopScotch(sampleSize);
//		long t2 = System.currentTimeMillis();
//		System.out.println("HopScotch: " + (t2 - t1));
//		testWeightedRandPerm(sampleSize);
//		long t3 = System.currentTimeMillis();
//		System.out.println("HopScotch: " + (t2 - t1));
//		System.out.println("WeightedRandPerm: " + (t3 - t2));
//		int[] testInts = {
//				0, 15, 16, 17, 1073741823, 1073741824, 1073741825, 2147483647
//		};
//		for (int i: testInts) {
//			System.out.println("Nearest power of " + i + ": " + roundNearestPower2(i));
//			System.out.println("Trailing zeros of " + i + ": " + countTrailingZeros2(i));
//			System.out.println("Highest 2-power Denominator of " + i + ": " + highestDenominatorPower2(i));
//		}
		testWarswapTask();
		int[] irregSrcDegSeq = {5, 5, 3, 2, 1, 1, 1};
		int[] irregTgtDegSeq = {6, 3, 3, 2, 2, 1, 1};
		testGenerateGraph(irregSrcDegSeq, irregTgtDegSeq);
		int[] regSrcDegSeq = {5, 5, 5, 5, 5, 5, 5, 5};
		int[] regTgtDegSeq = {5, 5, 5, 5, 5, 5, 5, 5};
		testGenerateGraph(regSrcDegSeq, regTgtDegSeq);
	}
	public static void testWeightedRandPerm(int sampleSize) {
		double sigma = sampleSize / 4;
		double mu = sampleSize / 2;
		double pi = 3.14159265358979323;
		double[] uniformWeights = new double[sampleSize];
		double[] gaussianWeights = new double[sampleSize];
		double[] linearWeights = new double[sampleSize];
		for (int i = 0; i < sampleSize; i++) {
			double j = (double) i;
			uniformWeights[i] = 1;
			gaussianWeights[i] = (1 / (2 * sigma * pi)) * Math.exp(-1 * Math.pow(j - mu, 2) / (2 * Math.pow(sigma, 2)));
			linearWeights[i] = i + 1;
		}
		
		int[] uniformHist = weightedRandPermGenerateHistogram(uniformWeights);
		System.out.println("WeightedRandPerm Uniform Histogram:");
		printHistogram(uniformHist);
		
		System.out.println("WeightedRandPerm Gaussian Histogram:");
		int[] gaussianHist = weightedRandPermGenerateHistogram(gaussianWeights);
		printHistogram(gaussianHist);
		
		System.out.println("WeightedRandPerm Linear Histogram:");
		int[] linearHist = weightedRandPermGenerateHistogram(linearWeights);
		printHistogram(linearHist);	
		}

	public static void testHopScotch(int sampleSize) {
		double sigma = sampleSize / 4;
		double mu = sampleSize / 2;
		double pi = 3.14159265358979323;
		double[] uniformWeights = new double[sampleSize];
		double[] gaussianWeights = new double[sampleSize];
		double[] linearWeights = new double[sampleSize];
		for (int i = 0; i < sampleSize; i++) {
			double j = (double) i;
			uniformWeights[i] = 1;
			gaussianWeights[i] = (1 / (2 * sigma * pi)) * Math.exp(-1 * Math.pow(j - mu, 2) / (2 * Math.pow(sigma, 2)));
			linearWeights[i] = i + 1;
		}
		System.out.println(uniformWeights[0]);

		int[] uniformHist = hopscotchGenerateHistogram(uniformWeights);
		System.out.println("Hopscotch Uniform Histogram:");
		printHistogram(uniformHist);
		
		System.out.println("Hopscotch Gaussian Histogram:");
		int[] gaussianHist = hopscotchGenerateHistogram(gaussianWeights);
		printHistogram(gaussianHist);
		
		System.out.println("Hopscotch Linear Histogram:");
		int[] linearHist = hopscotchGenerateHistogram(linearWeights);
		printHistogram(linearHist);	
		}
	
	private static int[] hopscotchGenerateHistogram(double[] weights) {
		System.out.println(weights[0]);

		int sampleSize = weights.length;
		int[] histogram = new int[sampleSize];
		// Sample 5000 elements 200 times.
		for (int i = 0; i < 200; i++) {
			// Restart each time because this is what we have to do when we use WaRSwap.
			SampleHopScotch sampler = new SampleHopScotch(weights);
			// Count the number of hits
			for (int j = 0; (double) j < (double) weights.length * 0.75; j++) {
				histogram[sampler.selectRandomWithoutReplacement()]++;
			}
		}
		return histogram;
	}
	
	private static int[] weightedRandPermGenerateHistogram(double[] weights) {
		for (int i = 0; i < weights.length; i++) {
//			System.out.print(weights[i] + " ");
		}
//		System.out.print("\n");
		
		int sampleSize = weights.length;
		int[] histogram = new int[sampleSize];
		Random random = new Random();

		// Sample 5000 elements 200 times.
		for (int i = 0; i < 200; i++) {
			// Restart each time because this is what we have to do when we use WaRSwap.
			WeightedRandPerm perm = new WeightedRandPerm(random, weights);
			perm.reset(weights.length);
			for (int j = 0; (double) j < (double) weights.length * 0.75; j++) {
				histogram[perm.next()]++;
			}
		}
		return histogram;
	}
	
	private static void printHistogram(int[] array) {
		//System.out.println(array);
		// Copied from Gennadiy Ryabkin 
		// https://stackoverflow.com/questions/13106906/how-to-create-a-histogram-in-java
		int max = 0;
		for (int i: array) {
			if (max < i) max = i;
		}
		for (int range = 0; range < array.length; range++) {
			String label = range + " : ";
			int nstars = (int) ((double) array[range] / (double) max * 25.0);
			System.out.println(label + convertToStars(nstars));
		}
	}

	private static String convertToStars(int num) {
		// Copied from Gennadiy Ryabkin 
		// https://stackoverflow.com/questions/13106906/how-to-create-a-histogram-in-java
		StringBuilder builder = new StringBuilder();
		for (int j = 0; j < num; j++) {
			builder.append('*');
		}
		return builder.toString();
	}
	
	public static void testGenerateGraph(int[] srcDegSeq, int[] tgtDegSeq) {
		//HopScotchRandomGraphGenerator gen = new HopScotchRandomGraphGenerator(srcDegSeq, tgtDegSeq, 20.0);
		FenwickRandomGraphGenerator gen = new FenwickRandomGraphGenerator(srcDegSeq, tgtDegSeq, 20.0);
		int[][] edgeArr = gen.generate();
		System.out.println("Test edgelist:");
		int[] newSrcDegSeq = new int[srcDegSeq.length];
		int[] newTgtDegSeq = new int[srcDegSeq.length];
		for (int i = 0; i < edgeArr.length; i ++) {
			System.out.println(edgeArr[i][0] + " " + edgeArr[i][1]);
			newSrcDegSeq[edgeArr[i][0]]++;
			newTgtDegSeq[edgeArr[i][1]]++;
		}
		System.out.println("Reconstructed source degree sequence:");
		for (int i = 0; i < newSrcDegSeq.length; i++) {
			System.out.print(newSrcDegSeq[i] + " ");
		}
		System.out.println();
		System.out.println("Reconstructed target degree sequence:");
		for (int i = 0; i < newTgtDegSeq.length; i++) {
			System.out.print(newTgtDegSeq[i] + " ");
		}
		System.out.println();
	}
	
	public static void testHeap(Random random) {
		double[] vals = new double[20];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = random.nextDouble();
		}
		int[] inv = new int[20]; int[] heap = new int[20];
		for (int i = 0; i < vals.length; i++) {
			heap[i] = i;
			inv[i] = i;
		}
		
		DoubleIndirectHeaps.makeHeap(vals, heap, inv, vals.length, DoubleComparators.NATURAL_COMPARATOR);
		for (int i = 0; i < heap.length; i++) {
			System.out.println(vals[i] + " " + vals[heap[i]] + " " + heap[i]);
		}
	}
	public static int roundNearestPower2(int v){
		//TODO: Consider using A C program with __builtin_clz for faster performance.
		// Using Sean Eron Anderson's method to round to nearest power of 2 
		// https://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
		// This requires us to interpret the int as 32-bit unsigned, which is fine here. 
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v++;
		return v;
	}
	public static int countTrailingZeros2(int v) {
		// Return the number of trailing zeros in the binary representation of a number.
		// This is the same as the highest N such that v % 2^N == 0.
		
		// Using Sean Eron Anderson's method to count trailing zeros.
		// https://graphics.stanford.edu/~seander/bithacks.html#ZerosOnRightModLookup
		// This requires us to interpret the int as 32-bit unsigned, which is fine here.
		int r;           // put the result in r
		final int[] Mod37BitPosition = // map a bit value mod 37 to its position
		{
		  32, 0, 1, 26, 2, 23, 27, 0, 3, 16, 24, 30, 28, 11, 0, 13, 4,
		  7, 17, 0, 25, 22, 31, 15, 29, 10, 12, 6, 0, 21, 14, 9, 5,
		  20, 8, 19, 18
		};
		return Mod37BitPosition[(-v & v) % 37];
	}
	
	public static int highestDenominatorPower2(int v) {
		// Return the highest power p of 2 such that v % p == 0
		return v & ~(v - 1);
	}
	
	public static void testWarswapTask() {
		int[] tDegSeq = {5,5,4,3,2};
		int[] sDegSeq = {5,5,3,3,3};
		WarswapTask test1 = new WarswapTask(tDegSeq, sDegSeq, "/home/zachary/Documents/rand_outgraphs_test", 0, 10, 20.0);
		test1.start();
	}
}
