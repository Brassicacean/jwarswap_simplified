package edu.osu.jwarswap;
import java.util.Arrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collections;

public class FenwickRandomGraphGenerator {
	private int mEdges;
	private int[] srcDegSeq;
	private int[] tgtDegSeq;
	private IntFenwickTree srcDegTree;
	private double factor;
	private FenwickEdgeGenerator randomEdgeGenerator;
	private int[] vertexNames = null;
	
	public int countEdges() {
		return this.mEdges;
	}
	
	public void assignNames(int[] names) {
		/** 
		 * Provide a list of integers to use as the names of the vertices.
		 * The order of the names should be the same as the order of the verices'
		 * respective degrees in the input degree sequences.
		 */
		this.vertexNames = names;
	}
	
	public FenwickRandomGraphGenerator(int[] srcdegseq, int[] tgtdegseq, double factor) {
		/** Produces a randomized network using WaRSwap algorithm 
		 * Inputs: A degree sequence is all that should be needed.
		 * Outputs: An edge-list. It shouldn't need to be more complicated than that.
		*/ 
		this.srcDegSeq = Arrays.copyOf(srcdegseq, srcdegseq.length);
		this.tgtDegSeq = Arrays.copyOf(tgtdegseq, tgtdegseq.length);
		this.factor = factor;

		// Fenwick tree to easily get sums.
		this.srcDegTree = new IntFenwickTree(srcDegSeq);

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
		IntOpenHashSet targetsSet = new IntOpenHashSet(Arrays.copyOfRange(targets, 0, targets.length - 2 - targets[targets.length - 1]));

		// The last element is a counter for unfilled edge slots. While there is an unused edge slot, keep trying to fill it.
		while (targets[targets.length - 1] > 0) {
			// Choose one of the unsaturated targets at random.
			// This method will only select an unsaturated target and won't update
			// capacities.
			int tgtVtx = randomEdgeGenerator.selectTarget(this.srcDegSeq[srcVtx]);
			int tgtCap = randomEdgeGenerator.capacityOf(tgtVtx);
			if (tgtCap == 0) throw new IllegalStateException("Chose a full target for swapping: " + tgtVtx);  // This should be impossible.
			// From the sources already filled, make a list from which one can be chosen at random.
			IntArrayList srcList = new IntArrayList();
			for (int idx = 0; idx < srcVtx; idx++) srcList.add(idx);
			Collections.shuffle(srcList);
			IntIterator srcIterator = srcList.intIterator();
			// Try to fill the target until it's full or all edges are selected.
			while (tgtCap > 0 && targets[targets.length - 1] > 0) {
				// Choose a random source vertex that hasn't been chosen yet.
				int swapSrcVtx = srcIterator.nextInt();
				// The first edge in the edge list that uses sourceVtx.
				// The edge list generator is designed so that all edges with the same source are
				// next to each other.
				int edgeListStart = this.srcDegTree.getSumTo(swapSrcVtx - 1);
				int swapSrcDeg = this.srcDegSeq[swapSrcVtx];
				
				// Check if it is already connected to the chosen target.
				boolean continue_flag = false;
				for (int i = edgeListStart; i < edgeListStart + swapSrcDeg; i++) {
					if (edgeArr[i][1] == tgtVtx) {
						continue_flag=true;
						break;
					}
				}
				if (continue_flag) continue;
				
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
	
	private void renameVertices(int[][] edgeArr) {
		/**
		 *  Rename the vertices in the edge-list to match the input names.
		 */
		for (int i = 0; i < edgeArr.length; i++) {
			edgeArr[i][0] = this.vertexNames[edgeArr[i][0]];
			edgeArr[i][1] = this.vertexNames[edgeArr[i][1]];
		}
	}
	
	public int[][] generate() {
		// Generate a random graph using the WaRSwap algorithm.
		boolean success = true;
		int[][] edgeArr;
		do {
			edgeArr = new int[this.mEdges][2];
			try {
				randomEdgeGenerator = new FenwickEdgeGenerator(this.tgtDegSeq, factor);
				int edgeNum = 0;  // Keep track of where we're at.
				for (int srcVtx = 0; srcVtx < this.srcDegSeq.length; srcVtx++) {
					int srcDeg = this.srcDegSeq[srcVtx];
					// Make a small edge list that contains connections to the current source.
					int[] targets = this.randomEdgeGenerator.fillSrcVtx(srcVtx, srcDeg);
					// If swaps have to be made, detect it and make swaps.
					if (targets[srcDeg] > 0) {
						swapEdges(edgeArr, targets, srcVtx, randomEdgeGenerator);
					}
					// Add the new edges to the edge list.
					for (int j = 0; j < srcDeg; j++) {
						edgeArr[edgeNum + j][0] = srcVtx;
						edgeArr[edgeNum + j][1] = targets[j];
					}
					// Now advance to the next appropriate starting position.
					edgeNum += srcDeg;
				}
				renameVertices(edgeArr);  // Recover the original vertex names.
				return edgeArr;
			} catch (IllegalStateException e) {
				throw e;
			}
		} while(success == false);
	}
}
