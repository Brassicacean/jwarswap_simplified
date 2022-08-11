package edu.osu.jwarswap;
import java.util.Arrays;
import java.util.Set;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Random;
import java.lang.Math;

@Deprecated
public class HopScotchEdgeGenerator {
	private int srcDeg;
	private double[] tgtWeights;
	private int[] tgtDegSeq;
	private int[] tgtCapSeq;
	private int[] tgtIdxSeq;
	private int[] srcDegSeq;
	private double factor;
	private int mEdges;  // Number of edges
	
//	// Use this to randomly sample target edges
//	private SampleHopScotch sampler;
	public HopScotchEdgeGenerator(int[] srcdegseq, int[] tgtdegseq, double factor) {
		// This object needs to:
		// a. Weigh the connections between each pair of possible vertices to be selected. 
		// b. Choose an edge at random using the probability distribution given by the weights.
		// For the basic method, it will need to compute the weights each time the
		this.factor = factor;
		this.srcDegSeq = Arrays.copyOf(srcdegseq, srcdegseq.length);
		// Sort this.srcDegSeq in reverse. We don't care about their original ordering.
		Arrays.sort(this.srcDegSeq);
		for (int left=0, right=this.srcDegSeq.length - 1; left<right; left++, right--) {
		    // exchange the first and last
		    int temp = this.srcDegSeq[left];
		    this.srcDegSeq[left]  = this.srcDegSeq[right];
		    this.srcDegSeq[right] = temp;
		}

		this.tgtDegSeq = Arrays.copyOf(tgtdegseq, tgtdegseq.length);
		this.tgtWeights = new double[tgtdegseq.length];
		mEdges = 0;
		for (int deg: this.tgtDegSeq) {
			mEdges += deg;
		}
		this.tgtCapSeq = Arrays.copyOf(this.tgtDegSeq, this.tgtDegSeq.length);  // Capacity = degree - occupied.
		this.tgtIdxSeq = new int[this.tgtDegSeq.length];
		for (int i = 0; i < tgtdegseq.length; i++) {
			this.tgtIdxSeq[i] = i;  // Sequence of {0..length}
		}
		this.srcDeg = 0;
	}
	
	public void reset() {
		this.tgtCapSeq = Arrays.copyOf(this.tgtDegSeq, this.tgtDegSeq.length);  // Capacity = degree - occupied.
		for (int i = 0; i < this.tgtDegSeq.length; i++) {
			this.tgtIdxSeq[i] = i;  // Sequence of {0..length}
		}
		this.srcDeg = 0;
	}
	
	private void computeTargetWeights() {
		// Recompute the target weights.
		System.out.println("factor: " + this.factor);
		System.out.println("mEdges: " + this.mEdges);
		System.out.println("Weights:");
		for (int i = 0; i < this.tgtDegSeq.length; i++) {
			this.tgtWeights[i] = this.tgtCapSeq[i] * (1 - (this.tgtDegSeq[i] * this.srcDeg / (this.factor * this.mEdges)));
			System.out.print(this.tgtWeights[i] + " ");
		}
		System.out.println();
		// Sort the index numbers by their respective weights.
		double[][] pair = new double[this.tgtIdxSeq.length][2];
		for (int i = 0; i < this.tgtIdxSeq.length; i++) {
			pair[i][0] = Double.valueOf(this.tgtIdxSeq[i]);
			pair[i][1] = this.tgtWeights[i];
		}
		//   This should sort indices by weight in reverse.
		Arrays.sort(pair, (a,b)->Double.compare(b[1], a[1]));
		for (int i = 0; i < this.tgtIdxSeq.length; i++) {
			this.tgtIdxSeq[i] = (int) pair[i][0];
		}
	}
	
	// I'm deprecating the alias method because it isn't suitable for sampling without replacement and it isn't easy to adapt for it either.
	@Deprecated
	private class sampleAlias {
		private Random random;
		private int[] aliasIdxArr;
		private double[] aliasWeightArr;
		public sampleAlias() {
			this.random = new Random();
		}
		public sampleAlias(double[] weights) {
			this.random = new Random();
			this.setup(weights);
		}
		public void setup (double[] weights) {
			// Thanks to Bruce Hill (https://blog.bruce-hill.com/a-faster-weighted-random-choice)
			// for explaining this.
			int N = weights.length;
			double mean = 0.0;
			for (double weight: weights) {
				mean += weight;
			}
			mean = mean / N;
			
			IntArrayList largeIdxList = new IntArrayList();
			DoubleArrayList largeWeightList = new DoubleArrayList();
			IntArrayList smallIdxList = new IntArrayList();
			DoubleArrayList smallWeightList = new DoubleArrayList();
			for (int i = 0; i < weights.length; i++) {
				if (weights[i] < mean) {
					smallIdxList.add(i);
					smallWeightList.add(weights[i] / mean);
				} else {
					largeIdxList.add(i);
					largeWeightList.add(weights[i] / mean);
				}
			}
	
			aliasIdxArr = new int[N];
			aliasWeightArr = new double[N];
			int smallIdx = smallIdxList.popInt();
			double smallWeight = smallWeightList.popDouble();
			int largeIdx = largeIdxList.popInt();
			double largeWeight = largeWeightList.popDouble();
			
			while (smallIdxList.size()> 0 && largeIdxList.size() > 0) {
				aliasIdxArr[smallIdx] = largeIdx;
				aliasWeightArr[smallIdx] = smallWeight;
				largeWeight = largeWeight - (1 - smallWeight);
				if (largeWeight > 1) {
					smallIdx = largeIdx;
					smallWeight = largeWeight;
					largeIdx = largeIdxList.popInt();
					largeWeight = largeWeightList.popDouble();				
				} else {
					smallIdx = smallIdxList.popInt();
					smallWeight = smallWeightList.popDouble();
				}
				
			}
		}
		@Deprecated
		public int selectWeighted() {
			double r = this.random.nextDouble() * this.aliasWeightArr.length;
			int i = (int) r;
			int alias = this.aliasIdxArr[i];
			double odds = this.aliasWeightArr[i];
			if (r - i > odds) {
				return alias;
			} else {
				return i;
			}
		}
	}
	
	public int[] chooseTgtGivenSrc(int srcvtx, Set <Integer> forbidden) {
		// Finds a target vertex to pair with srcvtx, returns the pair.
		int newSrcDeg = this.srcDegSeq[srcvtx];
		if (newSrcDeg != this.srcDeg) {
			this.srcDeg = newSrcDeg;
			this.computeTargetWeights();
		}
		int tgtVtx = 0;
		do {
			
		} while (forbidden.contains(tgtVtx));
		int[] edge = {srcvtx, 0};  //TODO: Implement the actual procedure.
		return edge;
	}
	
	public int[][] fillSrcVtx(int srcVtx) {
		// fill a source vertex assuming it is empty by repeatedly sampling target vertices.
		// make a miniature edge-list to append to the final one.
		try {
		this.srcDeg = this.srcDegSeq[srcVtx];
		this.computeTargetWeights();
		SampleHopScotch sampler = new SampleHopScotch(this.tgtWeights);
		int[][] edgeArr = new int[this.srcDeg][2];
		for (int i = 0; i < this.srcDeg; i++) {
			int tgtVtx = sampler.selectRandomWithoutReplacement();
			this.tgtCapSeq[tgtVtx]--;
			edgeArr[i][0] = srcVtx;
			edgeArr[i][1] = tgtVtx;
		}
		return edgeArr;
		} catch (IllegalStateException e) {
			throw e;
		}
	}
	
	public int[] chooseRandomEdge() {
		// Select a pair of vertices at random using the weighting scheme. 
		int[] edge = {0, 0};  // TODO: Implement the actual procedure.
		return edge;
	}
}
