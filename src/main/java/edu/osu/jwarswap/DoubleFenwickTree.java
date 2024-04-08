package edu.osu.jwarswap;

public class DoubleFenwickTree {
	public double[] arr;
	public double[] tree;
	
	public static void main(String[] args) {
		double[] seq = {5.0, 8.2, 7.1, 2.913, 4.5};
		DoubleFenwickTree doubleFenwickTree = new DoubleFenwickTree(seq);
		for (int i = 0; i < seq.length; i++) {
			System.out.println("Sum " + i + ": " + doubleFenwickTree.getSumTo(i));
		}
		System.out.println("Search 3: " + doubleFenwickTree.search(3));
		System.out.println("Search 5: " + doubleFenwickTree.search(5));
		System.out.println("Search 21: " + doubleFenwickTree.search(21));
		System.out.println("Search 25: " + doubleFenwickTree.search(25));
	}
	
	public DoubleFenwickTree(double[] inArr) {
		this.arr = new double[inArr.length];
		this.tree = constructTree(inArr);
	}
	
	private double[] constructTree(double[] inArr) {
		// It's a convenience for indexing to use the first index as the root.
		tree = new double[inArr.length +1];
		for (int i = 0; i < tree.length - 1; i++) {
			update(i, inArr[i]);
		}
		return tree;
	}
	
	public void update(int index, double value) {
		double delta = value - this.arr[index];
		this.arr[index] = value;  // Update the value itself.
		index += 1;  // Shift to 1-based indexing
		while (index <= this.arr.length) {
			// Progress along the nodes of the same level and update them.
			this.tree[index] += delta;
			index += (index & (-index));
		}
	}
	
	public double getValueOf(int target) {
		return this.arr[target];
	}
	
	public double getSumTo(int index) {
		/** Get the sum of elements up to and including index. */
		double s = 0;
		index += 1;  // Shift right to fit the tree.
		while (index > 0) {
			s += tree[index];
			index -= (index & (-index));
		}
		return s;
	}

	public int search(double s) {
		/** Magical method by David Eisenstat (I think) to find the index of the first
		 * index in the array represented by a Fenwick tree where the sum of
		 * the elements up to it is less than s.
		*/
		
		// Find the highest power of 2 in the tree.
		int level = 1;
		while (level < tree.length) {
			level <<= 1;
		}
		level >>= 1;
		// Start one place behind the highest power of 2.
		int offset = 0;
		while (level > 0) {
			// If the sum is too small, 
			if (offset + level < tree.length) {
				if (s > tree[offset + level]) {
					s -= tree[offset + level];
					offset += level;
				}
			}
			level >>= 1;
		}
		return offset;
	}
}

