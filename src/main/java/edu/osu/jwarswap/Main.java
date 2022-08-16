package edu.osu.jwarswap;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.File;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;

public class Main {
	public static void main(String[] args) {
		final String graphfile = args[0];
		final String rand_outdir = args[1];
		final int ngraphs = (int) Integer.valueOf(args[2]);
		final double factor = (double) Double.valueOf(args[3]);
		final int threads = (int) Integer.valueOf(args[4]);
		runWarswap(graphfile, rand_outdir, ngraphs, factor, threads);
	}
	
	private static void runWarswap(String graphfile, String rand_outdir, int ngraphs, double factor, int threads) {
		// Create the first interval.
		int increment = ngraphs / threads;
		int start = 0, end = increment;
		int[] tgtDegSeq = null;
		int[] srcDegSeq = null;

		try {
			LinkedList <int[]> degSeqs = degreeSequences(graphfile);
			tgtDegSeq = degSeqs.pop();
			srcDegSeq = degSeqs.pop();
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + graphfile);
			System.exit(1);
		}
		for (int threadno = 1; threadno < threads; threadno++) {
			// Anonymous thread object. Start it and let it run in the background.
			new WarswapTask(tgtDegSeq, srcDegSeq, rand_outdir, start, end, factor).start();
			start = end + 1;
			end += increment;
		}
		// For the final increment
		end = ngraphs - 1;
		new WarswapTask(tgtDegSeq, srcDegSeq, rand_outdir, start, end, factor).start();
	}
	

	
	
	private static LinkedList<int[]> degreeSequences(String graphfile)
	/** Read the file given by the input string and produce a pair of degree sequences
	 * interpreting the file as a directed edge list.*/
	throws FileNotFoundException{
		File edgelistFile = new File(graphfile);
		Scanner edgelistScanner = new Scanner(edgelistFile);
		// Count the number of connections with each node in them.
		// Treat the edge list as bipartite for the sake of making the degree
		// sequences i.e. If it's bipartite, each node is treated as a separate source and target node.
		HashMap <Integer, Integer> srcHashMap = new HashMap <Integer, Integer> ();
		HashMap <Integer, Integer> tgtHashMap = new HashMap <Integer, Integer> ();
		while(edgelistScanner.hasNextLine()) {
			String line = edgelistScanner.nextLine();
			Scanner lineScanner = new Scanner(line);
			int src = Integer.valueOf(lineScanner.findInLine("^\\d+"));
			int tgt = Integer.valueOf(lineScanner.findInLine("\\d+$"));
//			System.out.println(src + " " + tgt);
			srcHashMap.put(src, srcHashMap.getOrDefault(src, 0) + 1);
			tgtHashMap.put(tgt, tgtHashMap.getOrDefault(tgt, 0) + 1);
			lineScanner.close();
		}
		edgelistScanner.close();
		
		// Ensure the adjacency matrix is square by completing the HashMaps.
		for(int src: srcHashMap.keySet()) {
			tgtHashMap.put(src, tgtHashMap.getOrDefault(src, 0));
		}
		for(int tgt: tgtHashMap.keySet()) {
			srcHashMap.put(tgt, srcHashMap.getOrDefault(tgt, 0));
		}
		
		// Create the degree sequences and put them in the output list.
		int[][] inout = new int[srcHashMap.size()][2];
		int i = 0;
		// List the in- and out-degrees for each vertex.
		for(int vtx: srcHashMap.keySet()) {
			inout[i][0] = srcHashMap.get(vtx);
			inout[i][1] = tgtHashMap.get(vtx);
			i++;
		}
		
		Arrays.sort(inout, new HierarchicalComparator());
		LinkedList <int[]> degSeqs = new LinkedList <int[]>();
		
		int[] tgtDegSeq = new int[tgtHashMap.size()];
		int[] srcDegSeq = new int[tgtHashMap.size()];
//		System.out.println("\ntgthashMap keys");
		for (i = 0; i < srcHashMap.size(); i++) {
			srcDegSeq[i] = inout[i][0];
			tgtDegSeq[i] = inout[i][1];
		}

		degSeqs.add(tgtDegSeq);
		degSeqs.add(srcDegSeq);		

		return degSeqs;
	}
}


class HierarchicalComparator implements Comparator<int[]>{
	public int compare(int[] pair1, int[] pair2) {
		/** Comparator for sorting a 2d array in reverse order based on first column in row, then second.*/
		if (pair1[0] > pair2[0]) {
			return -1;
		}
		else if (pair1[0] < pair2[0]) {
			return 1;
		}
		else if (pair1[1] > pair2[1]) {
			return -1;
		}
		else if (pair1[1] < pair2[1]) {
			return 1;
		}
		else {
			return 0;
		}
	}
}