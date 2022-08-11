package edu.osu.jwarswap;
import java.util.Arrays;
import peersim.util.RandPermutation;
import java.util.Random;
import java.util.Iterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collections;

public class FenwickRandomGraphGenerator {
	// TODO: Make these static by writing methods to create each variable.
	private int mEdges;
	private int[] srcDegSeq;
	private int[] tgtDegSeq;
	private IntFenwickTree srcDegTree;
	private double factor;
	private FenwickEdgeGenerator randomEdgeGenerator;
	
	public FenwickRandomGraphGenerator(int[] srcdegseq, int[] tgtdegseq, double factor) {
		// Produces a randomized network using WaRSwap algorithm 
		// Inputs: A degree sequence is all that should be needed.
		// Outputs: An edge-list. It shouldn't need to be more complicated than that.
		this.srcDegSeq = Arrays.copyOf(srcdegseq, srcdegseq.length);
		Arrays.sort(this.srcDegSeq);
		for (int left=0, right=this.srcDegSeq.length - 1; left<right; left++, right--) {
		    // exchange the first and last
		    int temp = this.srcDegSeq[left];
		    this.srcDegSeq[left]  = this.srcDegSeq[right];
		    this.srcDegSeq[right] = temp;
		}
		// Fenwick tree to easily get sums.
		this.srcDegTree = new IntFenwickTree(srcDegSeq);
		
		this.tgtDegSeq = Arrays.copyOf(tgtdegseq, tgtdegseq.length);
		this.factor = factor;
		
		// Make a generator for random vertex-vertex connections (edges). This is kept modular
		// because there may be multiple ways to do it. I have two in mind with different
		// situational efficiency.

		// Establish the number of edges
		int m1 = 0;
		int m2 = 0;
		for (int deg: srcdegseq) m1 += deg;
		for (int deg: tgtdegseq) m2 += deg;
		if (m1 != m2) {
			throw new ArithmeticException("The partitions do not have equal degree sums.");
		} else {
			this.mEdges = m1;
		}
	}
	
	private void swapEdges(int[][]edgeArr, int[] targets, int srcVtx, FenwickEdgeGenerator randomEdgeGenerator){
		// A list of all the sources chosen before srcVtx.
		int[] possibleVtx = new int[srcVtx]; 
		for (int i = 0; i < srcVtx; i++) possibleVtx[i] = i;
		// Get the targets that are already in the array and put them in a set.
//		System.out.print("Targets: ");
//		for (int target: targets) System.out.print(target + " ");
//		System.out.println();
		IntOpenHashSet targetsSet = new IntOpenHashSet(Arrays.copyOfRange(targets, 0, targets.length - 2 - targets[targets.length - 1]));
//		double[] possibleVtxWeights = new double[srcVtx + 1];
//		int nswaps = targets[targets.length];  // Number of swaps to make.

		// The last element is a counter for unfilled edge slots. While there is an unused edge slot, keep trying to fill it.
		while (targets[targets.length - 1] > 0) {
			// Choose one of the unsaturated targets at random.
			// This method will only select an unsaturated target and won't update
			// capacities.
			int tgtVtx = randomEdgeGenerator.selectTarget(this.srcDegSeq[srcVtx]);
//			System.out.println("tgtVtx: " + tgtVtx);
			int tgtCap = randomEdgeGenerator.capacityOf(tgtVtx);
			if (tgtCap == 0) throw new IllegalStateException("Chose a full target for swapping: " + tgtVtx);  // This should be impossible.
			// From the sources already filled, make a list from which one can be chosen at random.
			IntArrayList srcList = new IntArrayList();
			for (int idx = 0; idx < srcVtx; idx++) srcList.add(idx);
			Collections.shuffle(srcList);
			IntIterator srcIterator = srcList.intIterator();
			// Try to fill the target until it's full or all edges are selected.
			while (tgtCap > 0 && targets[targets.length - 1] > 0) {
//				System.out.println("maxCap: " + randomEdgeGenerator.capacitySumTo(this.tgtDegSeq.length - 1));
				// Choose a random source vertex that hasn't been chosen yet.
//				System.out.print("Targets: ");
				for (int target: targets) System.out.print(target + " ");
//				System.out.println();
				int swapSrcVtx = srcIterator.nextInt();
//				System.out.println("swapSrcVtx: " + swapSrcVtx);
				// The first edge in the edge list that uses sourceVtx.
				// The edge list generator is designed so that all edges of a source are
				// contiguous.
				int edgeListStart = this.srcDegTree.getSumTo(swapSrcVtx - 1);
				int swapSrcDeg = this.srcDegSeq[swapSrcVtx];
				
				// Check if it is already connected to the chosen target.
				for (int i = edgeListStart; i < edgeListStart + swapSrcDeg; i++) {
//					System.out.println(edgeArr[i][1]);
					if (edgeArr[i][1] == tgtVtx) continue;
				}
				
				// Make an iterator of swappable target indices in the edge list.
				IntArrayList tgtList = new IntArrayList();
				for (int i = edgeListStart; i < edgeListStart + swapSrcDeg; i++) tgtList.add(edgeArr[i][1]);
				Collections.shuffle(tgtList);
				IntIterator tgtIterator = tgtList.intIterator();
				// Search randomly for a legal target to swap, then make the swap.
				// Continue until a swap is made or there are none left to try.
				while (tgtIterator.hasNext()) {
					int swapTgtIdx = tgtIterator.nextInt(); // Row in the edge-list.
					int swapTgtVtx = edgeArr[swapTgtIdx][1];
					if (! targetsSet.contains(swapTgtVtx)) {
						edgeArr[swapTgtIdx][1] = tgtVtx;
						// Put the swapped target in the targets list at the first unfilled spot.
						targets[targets.length - targets[targets.length - 1] - 1] = swapTgtVtx;
						targetsSet.add(swapTgtVtx);  // Register that this target is now connected to srcVtx
						tgtCap--;
						// Update capacity of target.
						randomEdgeGenerator.capacityOf(tgtVtx, tgtCap);
						// Decrement the counter for unfilled edges.
						targets[targets.length - 1]--;
						break;
					}
				}
			}
		}
		// By now, targets should be full of valid target IDs.
	}
	
	public int[][] generate() {
		// Generate a random graph using the WaRSwap algorithm.
		boolean success = true;
		int[][] edgeArr;
		do {
			edgeArr = new int[this.mEdges][2];
//			System.out.println("edgeArr length: " + edgeArr.length);
			try {
				randomEdgeGenerator = new FenwickEdgeGenerator(this.tgtDegSeq, factor);
				int edgeNum = 0;  // Keep track of where we're at.
				for (int srcVtx = 0; srcVtx < this.srcDegSeq.length; srcVtx++) {
					int srcDeg = this.srcDegSeq[srcVtx];
					// Make a small edge list that contains connections to the current source.
					int[] targets = this.randomEdgeGenerator.fillSrcVtx(srcDeg);
//					System.out.println("smallEdgeList: ");
					// If swaps have to be made, detect it and make swaps.
					if (targets[srcDeg] > 0) {
//						throw new IllegalStateException();
						swapEdges(edgeArr, targets, srcVtx, randomEdgeGenerator);
					}
					// Add the new edges to the edge list.
					for (int j = 0; j < srcDeg; j++) {
//						System.out.print(targets[j] + " ");
						edgeArr[edgeNum + j][0] = srcVtx;
						edgeArr[edgeNum + j][1] = targets[j];
					}
//					System.out.println();
					
					// Now advance to the next appropriate starting position.
					edgeNum += srcDeg;
				}
				return edgeArr;
			} catch (IllegalStateException e) {
				throw e;
//				success = false;
			}
			// If it failed to make a graph, try again.
			// We're supposed to do edge-swapping at this stage, but this is an easy temporary solution.
			// Also, it may improve sampling uniformity at the expense of time.
		} while(success == false);
//		return edgeArr;
		
	}
}
