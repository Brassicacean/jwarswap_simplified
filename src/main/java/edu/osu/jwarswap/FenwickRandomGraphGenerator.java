package edu.osu.jwarswap;
import java.util.Arrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collections;

public class FenwickRandomGraphGenerator {
	private int mEdges;
	private int[] srcDegSeq;
	private int[] tgtDegSeq;
	private IntFenwickTree srcDegTree;
	private double factor1;
	private double factor2;
	private FenwickEdgeGenerator randomEdgeGenerator;
	private int[] vertexNames = null;
	
	public int countEdges() {
		return this.mEdges;
	}
	
	public void assignNames(int[] names) {
		/** 
		 * Provide a list of integers to use as the names of the vertices.
		 * The order of the names should be the same as the order of the vertices'
		 * respective degrees in the input degree sequences.
		 */
		this.vertexNames = names;
	}
	
	public FenwickRandomGraphGenerator(int[] srcdegseq, int[] tgtdegseq, double factor1, double factor2) {
		/** Produces a randomized network using WaRSwap algorithm 
		 * Inputs: A degree sequence is all that should be needed.
		 * Outputs: An edge-list. It shouldn't need to be more complicated than that.
		*/ 
		this.srcDegSeq = Arrays.copyOf(srcdegseq, srcdegseq.length);
		this.tgtDegSeq = Arrays.copyOf(tgtdegseq, tgtdegseq.length);
		this.factor1 = factor1;
		this.factor2 = factor2;

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
		// 0. Make a set of the targets already in srcVtx.
		int finalPos = targets.length - 2 - targets[targets.length - 1];  // last position that has a target.
		IntOpenHashSet targetsSet = new IntOpenHashSet(Arrays.copyOfRange(targets, 0, finalPos + 1));
		// 1. Choose a full source
		IntArrayList srcList = new IntArrayList();
		for (int idx = 0; idx < srcVtx; idx++) srcList.add(idx);
		Collections.shuffle(srcList);
		IntIterator srcIterator = srcList.intIterator();
		while (srcIterator.hasNext() && targets[targets.length - 1] > 0) {
			int swapSrcVtx = srcIterator.nextInt();
			int edgeListStart = this.srcDegTree.getSumTo(swapSrcVtx - 1);
			int swapSrcDeg = this.srcDegSeq[swapSrcVtx];
			
			// Find a target that is not already connected to srcVtx.
			
			// 3. Set the capacities of all the targets attached to the full source to 0.
			// Need to save the capacities for later.
			Int2IntOpenHashMap tgtCapacities = new Int2IntOpenHashMap();
			for (int i = edgeListStart; i < edgeListStart + swapSrcDeg; i++) {
				tgtCapacities.put(edgeArr[i][1], randomEdgeGenerator.capacityOf(edgeArr[i][1]));
				randomEdgeGenerator.capacityOf(edgeArr[i][1], 0);
			}

			// 2. Find one target attached to that source that isn't shared with srcVtx.
			// If a target can't be chosen, go back to 1.
			for (int i = edgeListStart; i < edgeListStart + swapSrcDeg; i++) {
				if (targetsSet.contains(edgeArr[i][1])) continue;  // This target cannot be swapped out.
				if (randomEdgeGenerator.capacitySum() < 1) break;  // There are no allowable targets to swap into swapSrcVtx.
				if (targets[targets.length - 1] == 0) break;  // srcVtx is already full. 

				// 4. Choose a random weighted target.
				int tgtVtx = randomEdgeGenerator.selectTarget(swapSrcDeg);
				// 5. Give the new target to the full source and give the current target to srcVtx.
				targets[finalPos + 1] = edgeArr[i][1];
				randomEdgeGenerator.capacityOf(edgeArr[i][1], tgtCapacities.get(edgeArr[i][1]));
				edgeArr[i][1] = tgtVtx;
				tgtCapacities.addTo(tgtVtx, -1);  // We're forming an edge, so we have to reduce the capacity accordingly.
				finalPos++; targets[targets.length - 1]--;
				// Also need to prevent it from being chosen again.
				targetsSet.add(targets[finalPos]);
				tgtCapacities.put(edgeArr[i][1], randomEdgeGenerator.capacityOf(edgeArr[i][1]));
				randomEdgeGenerator.capacityOf(edgeArr[i][1], 0);
			}
			// 6. Restore all capacities, then subtract one from the selected target.
			for (int i = edgeListStart; i < edgeListStart + swapSrcDeg; i++) {
					randomEdgeGenerator.capacityOf(edgeArr[i][1], tgtCapacities.get(edgeArr[i][1]));
				}
			}
			if (targets[targets.length - 1] > 0) {
				System.err.println(srcVtx + " was not filled during back-swapping. Remaining capacity: " + targets[targets.length - 1]);
			}
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
				randomEdgeGenerator = new FenwickEdgeGenerator(this.tgtDegSeq, factor1, factor2);
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
