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

import com.carrotsearch.hppc.LongLongOpenHashMap;

 class WarswapTask implements Runnable{
	private Thread thread;
	private String threadname;
	private String rand_outdir;
	private int start, end;
	private static int motifSize = 3;
	private static boolean enumerate = true;
	private LinkedList<FenwickRandomGraphGenerator> genList;
	private HashMap <Long, LinkedList <Long>> subgraphCounts = new HashMap <Long, LinkedList <Long>>();
	
	public void prepareGenerators(int[][] edgeList, HashMap<Integer, Byte> vColorHash, double factor1, double factor2) {
		this.genList = Setup.getLayerGenerators(edgeList, vColorHash, factor1, factor2);
	}
	
	public void prepareGenerators(int[][] edgeList, double factor1, double factor2) {
		LinkedList<int[]> degSeqs = Setup.degreeSequences(edgeList);
		int[] srcDegSeq = degSeqs.pop();
		int[] tgtDegSeq = degSeqs.pop();
		int[] vertexNames = degSeqs.pop();
		FenwickRandomGraphGenerator gen = new FenwickRandomGraphGenerator(srcDegSeq, tgtDegSeq, factor1, factor2);
		gen.assignNames(vertexNames);
		LinkedList<FenwickRandomGraphGenerator> genList = new LinkedList<FenwickRandomGraphGenerator>();
		genList.add(gen);
		this.genList = genList;
	}
	
	public static void setMotifSize(int size) {
		motifSize = size;
	}
	
	public static void setEnumerate(boolean bool) {
		enumerate = bool;
	}
	
	public static int getMotifSize() {
		return motifSize;
	}
	
	
	public WarswapTask(String rand_outdir, int start, int end){
		// Task to make random networks and write them to files.
		// Will work on a single thread and either pull numbers from a
		// queue or just receive a list at the start of jobs to do.
		this.rand_outdir = rand_outdir;
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
	
	
	public void run() {
		int edges = 0;
		for (FenwickRandomGraphGenerator gen: genList) edges += gen.countEdges();
		// Make each graph in this loop and write it to a file as a tab-separated edge list.
		// Each graph is made layer by layer. The layers are concatenated to a single edge-list.
		for (int job = start; job <= end; job++) {
			int[][] finalEdgeArr = new int[edges][2];
			int cursor = 0;
			for (FenwickRandomGraphGenerator gen: genList) {
				int[][] edgeArr = gen.generate();
				for (int i = 0; i < edgeArr.length; i++) {
					finalEdgeArr[cursor + i][0] = edgeArr[i][0];
					finalEdgeArr[cursor + i][1] = edgeArr[i][1];
				}
				cursor += edgeArr.length;
			}
			
			String filepath = rand_outdir + "/" + "randgraph." + job + ".tsv";
		    try {
			    BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
		    	// Write the edge-list as a tsv.
		    	for (int row = 0; row < finalEdgeArr.length; row++) {
		    		writer.write(finalEdgeArr[row][0] + "\t" + finalEdgeArr[row][1] + "\n");
		    	}
		    	writer.close();
		    } catch (IOException e) {
				System.err.println("Error occurred while creating file " + filepath);
//				e.printStackTrace();
				System.exit(1);
			}
		    // Enumerate subgraphs in edgeArr and add the counts to subgraphCounts in the appropriate locations.
		    if (enumerate) {
		    	LongLongOpenHashMap subgraphs = getSubgraphs(finalEdgeArr);
	            for (long key: subgraphs.keys) {
	            	if (!subgraphCounts.containsKey(key)) {
	            		subgraphCounts.put(key, new LinkedList<Long>());
	            	}
	        		subgraphCounts.get(key).add(subgraphs.get(key));
	            }
		    }
		}	
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
	
	public static void main(String[] args) {
		int[] tDegSeq = {7,6,6,5,5,5,4,3,2};
		int[] sDegSeq = {7,6,6,5,5,5,3,3,3};
		FenwickRandomGraphGenerator gen = new FenwickRandomGraphGenerator(sDegSeq, tDegSeq, 6.0, 0.0);
		WarswapTask test1 = new WarswapTask("/home/zachary/test_warswap_fenwick_trees", 0, 9);
		test1.genList = new LinkedList<FenwickRandomGraphGenerator>();
		test1.genList.add(gen);
		test1.start();
	}
}
