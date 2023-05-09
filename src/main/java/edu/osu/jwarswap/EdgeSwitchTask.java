package edu.osu.jwarswap;

import edu.osu.netmotifs.subenum.Graph;
import edu.osu.netmotifs.subenum.HashGraph;
import edu.osu.netmotifs.subenum.MatGraph;
import edu.osu.netmotifs.subenum.SMPEnumerator;

import java.io.BufferedWriter;
import java.io.File;  // Import the File class
import java.io.IOException;  // Import the IOException class to handle errors
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;	
import java.util.concurrent.ArrayBlockingQueue;

import com.carrotsearch.hppc.LongLongOpenHashMap;

 class EdgeSwitchTask implements Runnable{
	private Thread thread;
	private String threadname;
	private String rand_outdir = null;
	private int start, end;
	private static int motifSize = 3;
	private static int edgeSwaps = 0;
	private static boolean enumerate = true;
	private static boolean entropy = false;
	private LinkedList<FenwickRandomGraphGenerator> genList;
	private static ArrayList<int[][]> edgelists;
	private HashMap <Long, LinkedList <Long>> subgraphCounts = new HashMap <Long, LinkedList <Long>>();
	private static ArrayBlockingQueue<int[][]> adj_queue = null;  // TODO: Compute this when entropy==true.
	
	public static void set_edgelists(ArrayList<int[][]> _edgelists) {
		edgelists = _edgelists;
	}
	
	public static void setMotifSize(int size) {
		motifSize = size;
	}
	
	public static void setEnumerate(boolean bool) {
		enumerate = bool;
	}
	
//	public static void setEntropy(boolean bool) {
//		entropy = true;
//	}
	
	public static int getMotifSize() {
		return motifSize;
	}
	
	public static void setEdgeSwaps(int _edgeSwaps) {
		edgeSwaps = _edgeSwaps;
	}
	
	public static void setAdjQueue(ArrayBlockingQueue<int[][]> _adj_queue) {
		adj_queue = _adj_queue;
	}
	
	
	public EdgeSwitchTask(String rand_outdir, int start, int end){
		// Task to make random networks and write them to files.
		// Will work on a single thread and either pull numbers from a
		// queue or just receive a list at the start of jobs to do.
		this.rand_outdir = rand_outdir;  // TODO: Make this optional.
		this.start = start;
		this.end = end;
	}

	public HashMap <Long, LinkedList <Long>> getSubgraphCounts(){
		return subgraphCounts;
	}
	
	public static LongLongOpenHashMap getSubgraphs(int[][] edgeArr) {
		Graph graph;
		if (edgeArr.length < 20000) {
			graph = MatGraph.readStructure(edgeArr);
		} else {
			graph = HashGraph.readStructure(edgeArr);
		}
        SMPEnumerator.setMaxCount(Long.MAX_VALUE);
        LongLongOpenHashMap subgraphs = null;
        try {
        	subgraphs = SMPEnumerator.enumerateNonIsoInParallel(graph, motifSize, 1);
        } catch (InterruptedException e) {
        	e.printStackTrace();
        	System.exit(1);
        }
        return subgraphs;
	}
	
	
	private void save_graphs(String rand_outdir, int job, int[][] finalEdgeArr) {
		String filepath = rand_outdir + "/randgraph." + job + ".tsv";
	    try {
		    BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
	    	// Write the edge-list as a tsv.
	    	for (int row = 0; row < finalEdgeArr.length; row++) {
	    		writer.write(finalEdgeArr[row][0] + "\t" + finalEdgeArr[row][1] + "\n");
	    	}
	    	writer.close();
	    } catch (IOException e) {
			System.err.println("Error occurred while creating file " + filepath);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	private void tallySubgraphs(int[][] finalEdgeArr) {
    	LongLongOpenHashMap subgraphs = getSubgraphs(finalEdgeArr);
        for (long key: subgraphs.keys) {
        	if (!subgraphCounts.containsKey(key)) {
        		subgraphCounts.put(key, new LinkedList<Long>());
        	}
    		subgraphCounts.get(key).add(subgraphs.get(key));
        }
	}
	
	
	public void run() {
		int edges = 0;
		//TODO: This should now require a group of edge lists instead of generators
		for (int[][] edgelist: edgelists) edges += edgelist.length;
		// Make each graph in this loop and write it to a file as a tab-separated edge list.
		// Each graph is made layer by layer. The layers are concatenated to a single edge-list.
		for (int job = start; job <= end; job++) {
			int[][] finalEdgeArr = new int[edges][2];
			int cursor = 0;
			for (int[][] edgelist: edgelists) {
				EdgeSwitcher edgeSwitcher = new EdgeSwitcher(edgelist);
				edgeSwitcher.switchNEdges(edgeSwaps);
//				System.out.println("Swapped edges " + edgeSwaps + " times.");
				int[][] edgeArr = edgeSwitcher.getEdgeArr();
				for (int i = 0; i < edgeArr.length; i++) {
					finalEdgeArr[cursor + i][0] = edgeArr[i][0];
					finalEdgeArr[cursor + i][1] = edgeArr[i][1];
				}
				cursor += edgeArr.length;
			}
//			if (entropy) {
//				//TODO: Make it work!
//			}
			if (rand_outdir != null) {
				save_graphs(rand_outdir, job, finalEdgeArr);
			}
			if (adj_queue != null) {
				// 
				try {
					adj_queue.put(finalEdgeArr);
//					System.out.println("Sending edge-list to SumMatrix");
				} catch (InterruptedException e) {
					System.err.println("The adjacency tally queue was interrupted. If this was unexpected, please alert the developers.");
				}
			}
		    // Enumerate subgraphs in edgeArr and add the counts to subgraphCounts in the appropriate locations.
		    if (enumerate) {
		    	tallySubgraphs(finalEdgeArr);
		    }
		}
	}
	
	
	public void start() {
		if (thread == null) {
			if (threadname == null) {
				threadname = "job_" + start + "_to_" + end;
			}
			thread = new Thread(this, threadname);
		}
		if (rand_outdir != null) {
			try {
				File outdirFile = new File(rand_outdir);
				outdirFile.mkdir();
			} catch (Exception e) {
				System.err.println("Error occured while creating directory " + rand_outdir);
			}
		}
		thread.start();
	}
	
	public static void main(String[] args) {
		int[] tDegSeq = {7,6,6,5,5,5,4,3,2};
		int[] sDegSeq = {7,6,6,5,5,5,3,3,3};
		double[] coefs = {0.00387596899225, 0.0};
		FenwickRandomGraphGenerator gen = new FenwickRandomGraphGenerator(sDegSeq, tDegSeq, coefs);
		EdgeSwitchTask test1 = new EdgeSwitchTask("/home/zachary/test_warswap_fenwick_trees", 0, 9);
		test1.genList = new LinkedList<FenwickRandomGraphGenerator>();
		test1.genList.add(gen);
		test1.start();
	}
}
