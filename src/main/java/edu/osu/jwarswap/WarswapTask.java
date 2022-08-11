package edu.osu.jwarswap;
import java.io.BufferedWriter;
import java.io.File;  // Import the File class
import java.io.IOException;  // Import the IOException class to handle errors

import java.io.FileWriter;

 class WarswapTask implements Runnable{
	private Thread thread;
	private String threadname;
	private int[] tgtDegSeq, srcDegSeq;
	private String rand_outdir;
	private int start, end;
	private double factor;
	
	public WarswapTask(int[] tgtDegSeq, int[] srcDegSeq, String rand_outdir, int start, int end, double factor){
		// Task to make random networks and write them to files.
		// Will work on a single thread and either pull numbers from a
		// queue or just receive a list at the start of jobs to do.
		this.tgtDegSeq = tgtDegSeq;
		this.srcDegSeq = srcDegSeq;
		this.rand_outdir = rand_outdir;
		this.start = start;
		this.end = end;
		this.factor = factor;
	}
	
	
	public void run() {
//		System.out.println("Degree sequences:");
		for (int deg: srcDegSeq) System.out.print(deg + " ");
//		System.out.println();
		for (int deg: tgtDegSeq) System.out.print(deg + " ");
//		System.out.println();
		FenwickRandomGraphGenerator gen = new FenwickRandomGraphGenerator(srcDegSeq, tgtDegSeq, factor);
		for (int job = start; job <= end; job++) {
			// Make each graph in this loop and write it to a file as a tab-separated edge list.
			int[][] edgeArr = gen.generate();
			String filepath = rand_outdir + "/" + "randgraph." + job + ".tsv";
		    try {
			    BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
		    	// Write the edge-list as a tsv.
		    	for (int row = 0; row < edgeArr.length; row++) {
		    		writer.write(edgeArr[row][0] + "\t" + edgeArr[row][1] + "\n");
		    	}
		    	writer.close();
		    } catch (IOException e) {
				System.err.println("Error occurred wile creating file " + filepath);
				e.printStackTrace();
			}
		}
		
		// Once I'm ready, I'll add the ability to enumerate subgraphs. 
	}
	public void start() {
		if (thread == null) {
			if (threadname == null) {
				threadname = "job" + start + " to " + end;
			}
			thread = new Thread(this, threadname);
		}
		try {
			File outdirFile = new File(rand_outdir);
			outdirFile.mkdir();
		} catch (Exception e) {
			System.err.println("Error occured while creating directory " + rand_outdir);
		}
		thread.start();
	}
	
	public static void main() {
		int[] tDegSeq = {5,5,4,3,2};
		int[] sDegSeq = {5,5,3,3,3};
		WarswapTask test1 = new WarswapTask(tDegSeq, sDegSeq, "/home/zachary/Documents/rand_outgraphs_test", 0, 10, 20.0);
		test1.start();
	}
}
