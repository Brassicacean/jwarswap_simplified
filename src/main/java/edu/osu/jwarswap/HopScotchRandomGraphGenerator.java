package edu.osu.jwarswap;
import java.util.Arrays;

@Deprecated
public class HopScotchRandomGraphGenerator {
	private int mEdges;
	private int[] srcDegSeq;
	private int[] tgtDegSeq;
	private double factor;
	private HopScotchEdgeGenerator randomEdgeGenerator;
	
	public HopScotchRandomGraphGenerator(int[] srcdegseq, int[] tgtdegseq, double factor) {
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

		this.tgtDegSeq = Arrays.copyOf(tgtdegseq, tgtdegseq.length);
		this.factor = factor;
		
		// Make a generator for random vertex-vertex connections (edges). This is kept modular
		// because there may be multiple ways to do it. I have two in mind with different
		// situational efficiency.
		this.randomEdgeGenerator = new HopScotchEdgeGenerator(this.srcDegSeq, this.tgtDegSeq, this.factor);

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
	
	public int[][] generate() {
		// Generate a random graph using the WaRSwap algorithm.
		boolean success = true;
		int[][] edgeArr;
		do {
			edgeArr = new int[this.mEdges][2];
			System.out.println("edgeArr length: " + edgeArr.length);
			try {
				this.randomEdgeGenerator.reset();  // Cheaper to reset than make a new one.
				int edgeNum = 0;  // Keep track of where we're at.
				for (int srcVtx = 0; srcVtx < this.srcDegSeq.length; srcVtx++) {
					int srcDeg = this.srcDegSeq[srcVtx];
					// Make a small edge list that contains connections to the currecnt source.
					int[][] smallEdgeList = this.randomEdgeGenerator.fillSrcVtx(srcVtx);
					System.out.println("smallEdgeList: ");
					for (int j = 0; j < srcDeg; j++) {
						System.out.println(smallEdgeList[j][0] + " " + smallEdgeList[j][1]);
					}
					System.out.println();
					
					// Now just add the small edge list to the final edge list.
					System.arraycopy(smallEdgeList, 0, edgeArr, edgeNum, smallEdgeList.length);
					edgeNum += srcDeg;
				}
				return edgeArr;
			} catch (IllegalStateException e) {
				success = false;
			}
			// If it failed to make a graph, try again.
			// We're supposed to do edge-swapping at this stage, but this is an easy temporary solution.
			// Also, it may improve sampling uniformity at the expense of time.
		} while(success == false);
		return edgeArr;
		
	}
}
