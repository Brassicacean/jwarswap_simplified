package edu.osu.jwarswap;
import java.util.Random;
import java.util.Arrays;

public class FenwickEdgeGenerator {
	private IntFenwickTree degreeDegreeCapacityTree;  // Degree squared times remaining capacity.
	private IntFenwickTree degreeCapacityTree;  // Degree times remaining capacity.
	private IntFenwickTree capacityTree;  // Just the capacities.
	private int[] degrees;
	private int mEdges;
	private int nVertices;
	private double factor1;
	private double factor2;
	private Random random;
	
	private static boolean selfLoops = true;
	
	public static void setSelfLoop(boolean loop) {
		selfLoops = loop;
	}
	
	FenwickEdgeGenerator(int[] tgtDegrees, double factor1, double factor2){
		this.random = new Random();
		this.degrees = Arrays.copyOf(tgtDegrees, tgtDegrees.length);
		this.mEdges = 0;
		this.nVertices = 0;
		this.factor1 = factor1;
		this.factor2 = factor2;
		int[] degreeCapacities = new int[this.degrees.length];  // Initializes as the squares of degrees.
		int[] degreeDegreeCapacities = new int[this.degrees.length];  // Initializes as the cubess of degrees.
		for (int i = 0; i < this.degrees.length; i++) {
			this.nVertices++;
			int deg = this.degrees[i];
			this.mEdges += deg;
			degreeCapacities[i] = deg * deg;
			degreeDegreeCapacities[i] = deg * deg * deg;
		}
		// Initialize the trees that will allow quick sum calculations.
		this.capacityTree = new IntFenwickTree(this.degrees);
		this.degreeCapacityTree = new IntFenwickTree(degreeCapacities);
		this.degreeDegreeCapacityTree = new IntFenwickTree(degreeDegreeCapacities);
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
		this.degreeDegreeCapacityTree.update(tgtVtx, newCap * this.degrees[tgtVtx] * this.degrees[tgtVtx]);
	}
	
	private int doubleSearch(double sum, double K, double L) {
		 /** K is the constant, sourceDegree/(m * f).
		 Use the two Fenwick trees to find a target node using appropriate weights.
		 This is adapted from the algorithm to find the highest index where the sum is
		 less than sum. */
			
		// This should work because we are searching a non-existent tree which is a linear
		// combination of degreeTree and capacityTree, which represent positive arrays,
		// and we have chosen k such that weightTree[i] = capacity[i] * (1 - k * degree[i])
		// is always positive.
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
						K * (double) this.degreeCapacityTree.tree[offset + level] +
						L * (double) this.degreeDegreeCapacityTree.tree[offset + level];
				if (sum > weight) {
					sum -= weight;  // TODO: Is this numerically stable?
					offset += level;
				}
			}
			level >>= 1;
		}
		return offset;
	}

	public int selectTarget(int srcDeg) {
		/** Selects a random target using Bayati et al's weight formula. */
		int maxCap = this.capacityTree.getSumTo(this.nVertices - 1);
		double K = srcDeg / (this.factor1 * this.mEdges);  // Linear coefficient
		double L = srcDeg * srcDeg * this.factor2;  // Quadratic coefficient
//		if (L > 0) System.out.println(L);
		double r = this.random.nextDouble();
		double maxWeight = (double) maxCap -
				K * (double) this.degreeCapacityTree.getSumTo(this.nVertices - 1) +
				L * (double) this.degreeDegreeCapacityTree.getSumTo(this.nVertices - 1);
		double sum = r * maxWeight; // TODO: Is this numerically stable? Do I even know what that means in this context?
		return this.doubleSearch(sum, K, L);
	}
	
	public int formEdge(int srcDeg) {
		/** Selects a target and updates the capacity to reflect that an edge has been formed. */
		int target = this.selectTarget(srcDeg);
		// Update the capacity of the target.
		int tgtDeg = this.degrees[target];
		int capacity = this.capacityTree.getValueOf(target) - 1;
		this.capacityTree.update(target, capacity);
		this.degreeCapacityTree.update(target, tgtDeg * capacity);
		this.degreeDegreeCapacityTree.update(target, tgtDeg * capacity * capacity);
		return target;
	}
	
	public int[] fillSrcVtx(int srcVtx, int srcDeg) {
		/**
		* Form all edges for a given source vertex, sampling without replacement.
		* One target for each "slot" in the source plus one at the end to say how
		* much swapping is needed.
		*/
		// If self-loops aren't allowed, set probability of self-connection to 0.
		int sameDeg = 0, sameDegCap = 0, sameDegDegCap = 0;
		if (! selfLoops) {
			sameDeg = this.capacityTree.getValueOf(srcVtx);
			this.capacityTree.update(srcVtx, 0);
			sameDegCap = this.capacityTree.getValueOf(srcVtx);
			this.degreeCapacityTree.update(srcVtx, 0);
			sameDegDegCap = this.capacityTree.getValueOf(srcVtx);
			this.degreeDegreeCapacityTree.update(srcVtx, 0);
		}
		int[] targets = new int[srcDeg + 1];
		int[] capacities = new int[srcDeg];
		int room = this.capacityTree.getSumTo(this.nVertices - 1) - srcDeg; 
		// Select all the targets, record all their capacities, set the capacities to 0.
		for (int i = 0; i < srcDeg; i++) {
			// If an edge can't be formed, return targets with a signal to initiate edge-swapping.
			if (room < 0) {
				targets[srcDeg]++;  // Increase the indicated number of unfilled edges.
			} else {
				int target = formEdge(srcDeg);
				targets[i] = target;
				// record the capacity, then set to 0 temporarily.
				int tgtCap = this.capacityTree.getValueOf(target);
				capacities[i] = tgtCap;
				this.capacityTree.update(target, 0);
				this.degreeCapacityTree.update(target, 0);
				this.degreeDegreeCapacityTree.update(target, 0);
				room -= tgtCap;
			}
		}
		
		// Restore the capacities.
		for (int i = 0; i < srcDeg - targets[srcDeg]; i++) {
			this.capacityTree.update(targets[i], capacities[i]);
			this.degreeCapacityTree.update(targets[i], capacities[i] * this.degrees[targets[i]]);
			this.degreeDegreeCapacityTree.update(targets[i], capacities[i] * this.degrees[targets[i]] * this.degrees[targets[i]]);
		}
		if (! selfLoops) {
			this.capacityTree.update(srcVtx, sameDeg);
			this.degreeCapacityTree.update(srcVtx, sameDegCap);
			this.degreeDegreeCapacityTree.update(srcVtx, sameDegDegCap);
		}
		return targets;
	}
}
