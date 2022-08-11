package edu.osu.jwarswap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.lang.Math;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/** Copyright (C) 2022 
 * @author Zachary Aleksei Bright
 * 
 * This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Contact info:  megrawm@oregonstate.edu

 */

/**
 * @author zach
 *
 */

@Deprecated
public class RandomSelectionTree {

	private int[] degSeq;
	private int[][] sumTree;
	private int[] ids;  // Track the target ids.
	private int mEdges;  // Number of edges
	// For each source vertex, store the nodes that cannot be traversed.
	private Int2IntOpenHashMap forbidden;
	// For each source vertex, store the number that needs to be subtracted from the maximum weight.
	// Think: Does this make sense? How are weights calculated when some nodes are forbidden?
	// Yes, this makes sense. We make the random variable encompass a distance that doesn't include
	// the highest number leaves, then give the forbidden leaves' probabilities to them by 
	// adding their weight to the total weight when selected. This unfortunately means starting
	// over from the root but should rarely contribute too much to runtime, I think.
	private HashMap<Integer, Integer> remainingTargets = new HashMap<Integer, Integer>();

	public class IntSumTree{
		public int[] leaves;
		public int sum;
		private int[] tree;
		private int[] ids[];
		
		public IntSumTree(int[] array) {
			this.leaves = array;
			this.sum = 0;
			for (int i: this.leaves) sum += i;
			int finalsize = roundNearestPower2(this.sum);
			this.tree = new int[finalsize * 2];
		}
	}
	
	public RandomSelectionTree(int[] degSeq, int[] idlist) {
		this.degSeq = degSeq;
		this.mEdges = 0;
		for (int i: degSeq) {
			this.mEdges += i;
		}
		int finalsize = roundNearestPower2(this.mEdges);

		// Make the initial sumTree and the array of ids. 
		// The sumTree array must be twice as big as the leaves to accommodate nodes.
		this.sumTree = new int[2][finalsize * 2];
		this.ids = new int[finalsize];
		int current = 0;
		int idx = 0;
		for (int i: degSeq) {  // Once for each vertex.
			this.remainingTargets.put(idlist[idx], i);
			for (int j = 0; j < i; j++) {  // Once for each mini-vertex ("stub", or opportunity to form an edge).
				this.sumTree[0][current + finalsize - 1] = 1;  // This row sums to give current capacity.
				this.sumTree[1][current + finalsize - 1] = i;  // This row gives a sum of max capacities (kind of abstract, but essential).
				this.ids[current] = idlist[idx];
				current++;
			}
			idx++;
		}
		// Calculate all the nodes' values by summing each node's children.
		// Start at the bottom layer above the leaves and work up to the root. 
		for (int i = finalsize - 2; i >= 0; i--) {
			this.sumTree[0][i] = this.sumTree[0][i * 2 + 1] + this.sumTree[0][i * 2 + 1];
			this.sumTree[1][i] = this.sumTree[1][i * 2 + 1] + this.sumTree[1][i * 2 + 1];
		}
	}
	
	private static int roundNearestPower2(int v){
		// Consider using A C program with __builtin_clz for faster performance.
		// Using Sean Eron Anderson's method to round to nearest power of 2 
		// https://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
		// This requires us to interpret the int as 32-bit unsigned, which is fine here. 
		
		// NOTE: If v > 1073741824, then the results will be negative because ints are signed.
		// There is no safety for this because I want this to be branchless. 
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v++;
		return v;
	}
	
	private static int highestDenominatorPower2(int v) {
		// Return the highest power p of 2 such that v % p == 0
		return v & ~(v - 1);
	}
	
	private static int countTrailingZeros2(int v) {
		// Return the number of trailing zeros in the binary representation of a number.
		// This is the same as the highest N such that v % 2^N == 0.
		
		// Using Sean Eron Anderson's method to count trailing zeros.
		// https://graphics.stanford.edu/~seander/bithacks.html#ZerosOnRightModLookup
		// This requires us to interpret the int as 32-bit unsigned, which is fine here.
		final int[] Mod37BitPosition = // map a bit value mod 37 to its position
		{
		  32, 0, 1, 26, 2, 23, 27, 0, 3, 16, 24, 30, 28, 11, 0, 13, 4,
		  7, 17, 0, 25, 22, 31, 15, 29, 10, 12, 6, 0, 21, 14, 9, 5,
		  20, 8, 19, 18
		};
		return Mod37BitPosition[(-v & v) % 37];
	}

	public int[] nextRandTargets(int degS, double factor) {
		/* Get a number of targets for the vertex S using the randomSelectionTree. The chosen leaves are removed.*/
		if (this.remainingTargets.size() < degS) {
			return null;
		}
		int[] outlist = new int[this.mEdges];
		HashSet<Integer> selected = new HashSet<Integer>();
		for (int i = 0; i < this.mEdges; i++) {
			// Needs to grab an edge from a vertex that hasn't been chosen before, using the weighted selection tree.
			int id = this.makeRandomSelection(selected, degS, factor);
			outlist[i] = id;
			selected.add(id);
		}
		return outlist;
	}
	
	private double summedWeight(int capSum, int degSum, double k) {
		/* Return the summed weight of a node or leaf in the sum tree */
		double weight = capSum - k * degSum;
		return weight;
	}
	
	private void remove(int position) {
		/* Remove a leaf from the randomSelectionTree in O(log(n)) time. */
		int degree = this.sumTree[1][position];
		// subtract one from the degree, and if the degree is 0, remove it.
		Integer cap = this.remainingTargets.get(this.ids[position]) - 1;
		if (cap == 0) {
			this.remainingTargets.remove(this.ids[position]);
		} else {
			this.remainingTargets.put(this.ids[position], cap);
		}
		do {
			this.sumTree[0][position] -= 1;
			this.sumTree[1][position] -= degree;
			// subtract one; make even; divide by 2
			position = position - 1;
			position = position & ~1;
			position = position / 2;
		} while (position != 0);
	}
	
	private int makeRandomSelection(HashSet<Integer> forbidden, int degS, double factor) {
		/* Use the weights to randomly find a leaf and return its address. */
		double k = degS / (factor * this.mEdges);
		int maxCapSum = this.sumTree[0][0];
		int maxDegSum = this.sumTree[1][0];
		double maxWeight =  summedWeight(maxCapSum, maxDegSum, k);
		int capSum, degSum, id, position;
		do {  // For a sparse matrix, this will not need to be done too many times on average. Maybe there's a better way to avoid multi-edges?
			double threshold = Math.random() * maxWeight;  // TODO: Use java.util.Random here. 
			position = 0;
			// Track the sums of left leaves.
			// We're looking for the leaf where the summedWeight of left leaves encompasses the threshold.
			int leftCapSum = 0;
			int leftDegSum = 0;
			while (position < this.sumTree[1].length / 2 - 1) {  // TODO: make sure this checks if we have reached a leaf. I think it does.
				capSum = this.sumTree[0][position * 2 + 1];
				degSum = this.sumTree[1][position * 2 + 1];
				if (summedWeight(capSum + leftCapSum, degSum + leftDegSum, k) < threshold) { 
					position = position * 2 + 1;  // Go left.
				} else {
					position = position * 2 + 2;  // Go right.
					leftCapSum += capSum;
					leftDegSum += degSum;
				}
			}
			// This conversion is awkward but necessary to get the id from the leaf.
			id = this.ids[position - this.sumTree[1].length / 2 + 1];
		} while (forbidden.contains(id));  // If it would select a forbidden vertex, make it restart.
		remove(position);
		return id;  // TODO: What if it's impossible to reach a leaf? How to catch that and what to return? 
	}
	
	private void forbidEdges (int min, int max) {
		// Find the smallest set of nodes that includes all leaves from min to max and no other leaves.
		// First find the largest power of 2 that divides min.
	}
}