package edu.osu.jwarswap;

public class IntFenwickTree {
	public int[] arr;
	public int[] tree;
	
	public static void main(String[] args) {
		int[] seq = {5, 8, 7, 2, 4};
		IntFenwickTree intFenwickTree = new IntFenwickTree(seq);
		for (int i = 0; i < seq.length; i++) {
			System.out.println("Sum " + i + ": " + intFenwickTree.getSumTo(i));
		}
		System.out.println("Search 3: " + intFenwickTree.search(3));
		System.out.println("Search 5: " + intFenwickTree.search(5));
		System.out.println("Search 21: " + intFenwickTree.search(21));
		System.out.println("Search 25: " + intFenwickTree.search(25));
	}
	
	public IntFenwickTree(int[] inArr) {
		this.arr = new int[inArr.length];
		this.tree = constructTree(inArr);
	}
	
	private int[] constructTree(int[] inArr) {
		// It's a convenience for indexing to use the first index as the root.
		tree = new int[inArr.length +1];
		for (int i = 0; i < tree.length - 1; i++) {
			update(i, inArr[i]);
		}
		return tree;
	}
	
	public void update(int index, int value) {
		int delta = value - this.arr[index];
		this.arr[index] = value;  // Update the value itself.
		index += 1;  // Shift to 1-based indexing
		while (index <= this.arr.length) {
			// Progress along the nodes of the same level and update them.
			this.tree[index] += delta;
			index += (index & (-index));
		}
	}
	
	public int getValueOf(int target) {
		return this.arr[target];
	}
	
	public int getSumTo(int index) {
		/** Get the sum of elements up to and including index. */
		int s = 0;
		index += 1;  // Shift right to fit the tree.
		while (index > 0) {
			s += tree[index];
			index -= (index & (-index));
		}
		return s;
	}

	public int search(int s) {
		/** Magical method by David Eisenstat to find the index of the first
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

