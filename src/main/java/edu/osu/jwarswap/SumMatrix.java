package edu.osu.jwarswap;
import java.util.concurrent.ArrayBlockingQueue;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class SumMatrix implements Runnable{
	private int[][] sums;
	private ArrayBlockingQueue<int[][]> queue;
	private Thread thread;
	
	public SumMatrix(int[][] edgelist, ArrayBlockingQueue <int[][]> queue) {
		LinkedList <int[]> degSeqs = Setup.degreeSequences(edgelist);
		//TODO: This doesn't give the biadjacency matrix dimensions, it gives the adjacency matrix dimensions.
		int[] srcDegSeq = degSeqs.pop();
		int[] tgtDegSeq = degSeqs.pop();
		int rows = srcDegSeq.length; int cols = tgtDegSeq.length;
		this.sums = new int [rows][cols];
		this.queue = queue;
	}
	
	public SumMatrix(int rows, int cols, ArrayBlockingQueue <int[][]> queue) {
		/** 
		 * This class has a queue which will receive an edge list whenever it can, and block when a 
		 * request cannot be received until there is room. Use the poisonpill message,
		 * int[][] poisonpill = {{-1, -1}}, to stop it.
		 */
		this.sums = new int[rows][cols];
		this.queue = queue;
	}
	
	
	private void tallyEdges(int[][] edgelist) {
		int m = edgelist.length;
		for (int i = 0; i < m; i++) {
			int u = edgelist[i][0]; int v = edgelist[i][1]; 
			this.sums[u][v]++;
		}
	}
	
	
	public int[][] getSums(){
		return this.sums.clone();
	}
	
	
	public void run() {
		/**
		 *
		 */
		try {
			while (true) {
				int[][] edgelist = queue.take();
				if (edgelist[0][0] == -1) {  // This is the poisonpill condition. 
					return;
				}
				tallyEdges(edgelist);
			}
		} catch (InterruptedException e) {
			System.err.println(e.getStackTrace());
			System.exit(1);
		}
	}
	
	public void start() {
		if (thread == null) {
			thread = new Thread(this, "sum_matrix_thread");
		}
		thread.start();
	}
	
	public void writeAvgMatrix(String outfile, int ngraphs) {
	 try {
		    BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
	    	// Write the edge-list as a tsv.
	    	for (int row = 0; row < this.sums.length; row++) {
	    		int ncols = this.sums[row].length;
	    		for (int col = 0; col < ncols - 1; col++) {
	    			writer.write((double) sums[row][col] / ngraphs + ",");	
	    		}
	    		writer.write((double) sums[row][ncols - 1] / ngraphs + "\n");
	    	}
	    	writer.close();
	    } catch (IOException e) {
			System.err.println("Error occurred while creating file " + outfile);
			e.printStackTrace();
		}
	}
}
