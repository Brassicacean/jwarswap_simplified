package edu.osu.jwarswap;
import java.util.Random;
import java.util.Arrays;
import java.lang.Math;

public class FenwickEdgeGenerator {
	private double[] coefficients;
	private IntFenwickTree[] degreePowerTrees;
	private IntFenwickTree capacityTree;  // Just the capacities.
	private int[] degrees;
	private int mEdges;
	private int nVertices;
	private Random random;
	
	private static boolean selfLoops = true;
	
	public static void setSelfLoop(boolean loop) {
		selfLoops = loop;
	}
	
	FenwickEdgeGenerator(int[] tgtDegrees, double[] coefficients){
		this.coefficients = coefficients;
		this.random = new Random();
		this.degrees = Arrays.copyOf(tgtDegrees, tgtDegrees.length);
		this.mEdges = 0;
		this.nVertices = this.degrees.length;
		this.degreePowerTrees = new IntFenwickTree[coefficients.length];
		// This is where the polynomial function trees are made. Each one 
		// corresponds to a coefficient and will be used to compute 
		// coefficient_i * sum from j=0 to j=index of degree_j^{i+1} * capacity_j
		for (int i = 0; i < coefficients.length; i++) {
			int[] degreePowerCapacities = new int[this.nVertices];
			for (int j = 0; j < this.nVertices; j++) {
				int deg = this.degrees[j];
				this.mEdges += deg;
				degreePowerCapacities[j] = deg * (int) Math.pow(deg, i + 1);
			}
			this.degreePowerTrees[i] = new IntFenwickTree(degreePowerCapacities);
		}
		// Initialize the trees that will allow quick sum calculations.
		this.capacityTree = new IntFenwickTree(this.degrees);
	}
	
	public int[] capacities() {
		int[] arr = new int[this.capacityTree.arr.length];
		for (int i = 0; i < this.capacityTree.arr.length; i++) arr[i] = this.capacityTree.arr[i];
		return arr;
	}
	
	public int capacitySum() {
        /** Get the sum of all capacities */
		return this.capacityTree.getSumTo(nVertices - 1);
	}
	
	public int capacitySumTo(int tgtVtx) {
        /** Get the sum of capacities up to and including tgtVtx */
		return this.capacityTree.getSumTo(tgtVtx);
	}
	
	public int capacityOf(int tgtVtx) {
		/** Return the capacity of tgtVtx. */
		return this.capacityTree.getValueOf(tgtVtx);
	}
	
	public void capacityOf(int tgtVtx, int newCap) {
		/**
		 * Update the capacity of tgtVtx to newCap, and update the other trees accordingly.
		*/
		this.capacityTree.update(tgtVtx, newCap);
		for (int i = 0; i < this.degreePowerTrees.length; i++) {
			degreePowerTrees[i].update(tgtVtx, newCap * (int) Math.pow(this.degrees[tgtVtx], i + 1));
		}
	}

	
	private double cumulativeWeight(int srcDeg, int index) {
        /** Return the cumulative weights of targets up to index, given
        * srcDeg. Depends on the current capacities of each target up to index.
        */
		// TODO: I think this is right, but it should be tested carefully
        // The only likely problem is with float precision.
		double weight = this.capacitySumTo(index);
		for (int i = 0; i < this.degreePowerTrees.length; i++) {
			weight += this.coefficients[i] * 
					Math.pow(srcDeg, i + 1) * 
					(double) this.degreePowerTrees[i].getSumTo(index);
		}
		return weight;
	}
	
	
	private int doubleSearch(double sum, int srcDeg) {
		 /** K is the constant, sourceDegree/(m * f).
		 Use the three Fenwick trees to find a target node using appropriate weights.
		 This is adapted from the algorithm to find the highest index where the sum is
		 less than `sum`. */
			
		// This works because we are searching a non-existent tree which is a linear
		// combination of the computed trees wihch represent the components of the 
        // weighing function, and we have chosen coefficients such that all the trees 
        // have strictly non-negative values.
		int offset = 1;
        int[] values = new int[this.coefficients.length + 1];
		while (offset < this.capacityTree.tree.length) {
			offset <<= 1;
		}
		offset >>= 1;
		int level = 0;
		while (offset > 0) {
			// Calculate the weight with Bayati et al.'s function.
			if (level + offset < this.capacityTree.tree.length - 1) {
				//double weight = this.capacityTree.tree[level + offset];
                int[] new_values = new int[this.coefficients.length + 1];
				new_values[0] = this.capacityTree.tree[level + offset];
				double weight = values[0] + new_values[0];
                for (int i = 0; i < this.degreePowerTrees.length; i++) {
                    // Idea: Use a vector of ints, compute floats only for comparison,
                    // update a vector of cumulative values.  
					//weight += (double) this.degreePowerTrees[i].tree[level + offset] *
					//		this.coefficients[i] *
					//		Math.pow(srcDeg, i + 1);
                    new_values[i + 1] = this.degreePowerTrees[i].tree[level + offset] * 
                        (int) Math.pow(srcDeg, i + 1);
                    weight += this.coefficients[i] * (values[i + 1] + new_values[i + 1]);
				}
				if (sum > weight) {
					//sum -= weight;
                    // Update
                    for (int i = 0; i < values.length; i++) values[i] += new_values[i];
					level += offset;
				}
			}
			offset >>= 1;
		}
		return level;
	}

	
	public int selectTarget(int srcDeg) {
		/** Selects a random target using Bayati et al's weight formula modified by ZAB. */
		//if (this.capacitySum() < 1) {
		//	System.out.println("Warning: trying to select an edge with total capacity " + this.capacitySum());
		//}
		double r = this.random.nextDouble();
		double maxWeight = this.cumulativeWeight(srcDeg, this.nVertices - 1);
		//System.out.println("maxWeight: " + maxWeight);
		double sum = r * maxWeight; // TODO: Is this numerically stable? Do I even know what that means in this context?
		//System.out.println("sum: " + sum);
		return this.doubleSearch(sum, srcDeg);
	}
	
	public int formEdge(int srcDeg) {
		/** Selects a target and updates the capacity to reflect that an edge has been formed. */
		int capacity = 0;
		int target = 0;
		do {
			target = this.selectTarget(srcDeg);
			// Update the capacity of the target.
			capacity = this.capacityOf(target);
		} while (capacity < 1);
		this.capacityOf(target, capacity - 1);
		return target;
	}
	
	public int[] fillSrcVtx(int srcVtx, int srcDeg) {
		/**
		* Form all edges for a given source vertex, sampling without replacement.
		* One target for each "slot" in the source plus one at the end to say how
		* much swapping is needed.
		*/
		// If self-loops aren't allowed, set probability of self-connection to 0.
		int sameCap = 0;
		if (! selfLoops) {
			sameCap = this.capacityOf(srcVtx);
			this.capacityOf(srcVtx, 0);
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
				int tgtCap = this.capacityOf(target);
//				System.out.println("tgtCap: " + tgtCap);
				capacities[i] = tgtCap;
				this.capacityOf(target, 0);
				room -= tgtCap;
//				System.out.println("Chose target at: " + target);
			}
		}
		
		// Restore the capacities.
		for (int i = 0; i < srcDeg - targets[srcDeg]; i++) {
			this.capacityOf(targets[i], capacities[i]);
		}
		if (! selfLoops) {
			this.capacityOf(srcVtx, sameCap);
		}
//		for (int i: targets) System.out.print(i + " ");
//		System.out.println();
		return targets;
	}
}
