package edu.osu.jwarswap;
import java.util.Random;
import java.util.Arrays;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class FenwickEdgeGenerator {
	private IntFenwickTree degreeCapacityTree;  // Degree times remaining capacity.
	private IntFenwickTree capacityTree;  // Just the capacities.
	private int[] degrees;
	private int mEdges;
	private int nVertices;
	private double factor;
	private Random random;
	
	FenwickEdgeGenerator(int[] tgtDegrees, double factor){
		this.random = new Random();
		this.degrees = Arrays.copyOf(tgtDegrees, tgtDegrees.length);
		this.mEdges = 0;
		this.nVertices = 0;
		this.factor = factor;
		int[] degreeCapacities = new int[this.degrees.length];  // Initializes as the squares of degrees.
		for (int i = 0; i < this.degrees.length; i++) {
			this.nVertices++;
			int deg = this.degrees[i];
			this.mEdges += deg;
			degreeCapacities[i] = deg * deg;
		}
		// Initialize the trees that will allow quick sum calculations.
		this.capacityTree = new IntFenwickTree(this.degrees);
		this.degreeCapacityTree = new IntFenwickTree(degreeCapacities);
	}
	
	public int capacitySumTo(int tgtVtx) {
		return this.capacityTree.getSumTo(tgtVtx);
	}
	
	public int capacityOf(int tgtVtx) {
		/** Return the capacity of tgtVtx. */
		return this.capacityTree.getValueOf(tgtVtx);
	}
	
	public void capacityOf(int tgtVtx, int newCap) {
		/** Update the capacity of tgtVtx to newCap.*/
		this.capacityTree.update(tgtVtx, newCap);
		this.degreeCapacityTree.update(tgtVtx, newCap * this.degrees[tgtVtx]);
	}
	
	private int doubleSearch(double sum, double K) {
		 /** K is the constant, sourceDegree/(m * f).
		 Use the two Fenwick trees to find a target node using appropriate weights.
		 This is adapted from the algorithm to find the highest index where the sum is
		 less than sum. */
			
		// This should work because we are searching a non-existent tree which is a linear
		// combination of degreeTree and capacityTree, which represent positive arrays,
		// and we have chosen k such that weightTree[i] = capacity[i] * (1 - k * degree[i])
		// is always positive.
//		System.out.println("doubleSearch on: " + sum + ", " + K);
		int level = 1;
		while (level < this.degreeCapacityTree.tree.length) {
			level <<= 1;
		}
		level >>= 1;
		int offset = 0;
		while (level > 0) {
			// Calculate the weight with Bayati et al.'s function.
			if (offset + level < this.degreeCapacityTree.tree.length) {
				double weight = (double) this.capacityTree.tree[offset + level] -
						K * (double) this.degreeCapacityTree.tree[offset + level];
				if (sum > weight) {
					sum -= weight;  // TODO: Is this numerically stable?
					offset += level;
				}
			}
			level >>= 1;
		}
//		System.out.println("Returning offset: " + offset);
		return offset;
	}

	public int selectTarget(int srcDeg) {
		/** Selects a random target using Bayati et al's weight formula. */
		int maxCap = this.capacityTree.getSumTo(this.nVertices - 1);
//		System.out.println("maxCap: " + maxCap);
		double K = srcDeg / (this.factor * this.mEdges);
		double r = this.random.nextDouble();
		double maxWeight = (double) maxCap -
				K * (double) this.degreeCapacityTree.getSumTo(this.nVertices - 1);
//		System.out.println("maxWeight: " + maxWeight);
//		System.out.println("Max degree * capacity: " + this.degreeCapacityTree.getSumTo(this.nVertices - 1));
		double sum = r * maxWeight; // TODO: Is this numerically stable?
		return this.doubleSearch(sum, K);
	}
	
	public int formEdge(int srcDeg) {
		/** Selects a target and updates the capacity to reflect that an edge has been formed. */
		int target = this.selectTarget(srcDeg);
		// Update the capacity of the target.
		int tgtDeg = this.degrees[target];
		int capacity = this.capacityTree.getValueOf(target);
		this.capacityTree.update(target, capacity - 1);
		this.degreeCapacityTree.update(target, tgtDeg * (capacity - 1));
		return target;
	}
	
	public int[] fillSrcVtx(int srcDeg) {
//		System.out.println("srcDeg: " + srcDeg);
		// Form all edges for a given source vertex, sampling without replacement.
		// One target for each "slot" in the source plus one at the end to say how
		// much swapping is needed.
		int[] targets = new int[srcDeg + 1];
		int[] capacities = new int[srcDeg];
		int room = this.capacityTree.getSumTo(this.nVertices - 1) - srcDeg; 
//		System.out.println("Room: " + room);
		// Select all the targets, record all their capacities, set the capacities to 0.
		for (int i = 0; i < srcDeg; i++) {
			// If an edge can't be formed, return targets with a signal to initiate edge-swapping.
			if (room < 0) {
//				System.out.println("Source vertex cannot be filled.");
				targets[srcDeg]++;  // Increase the indicated number of unfilled edges.
//				return targets;
			} else {
				int target = formEdge(srcDeg);
				targets[i] = target;
				// record the capacity, then set to 0 temporarily.
				int tgtCap = this.capacityTree.getValueOf(target);
				capacities[i] = tgtCap;
				this.capacityTree.update(target, 0);
				this.degreeCapacityTree.update(target, 0);
				room -= tgtCap;
			}
		}
		
		// Restore the capacities.
		for (int i = 0; i < srcDeg - targets[srcDeg]; i++) {
			this.capacityTree.update(targets[i], capacities[i]);
			this.degreeCapacityTree.update(targets[i], capacities[i] * this.degrees[targets[i]]);
		}
		return targets;
	}
}
