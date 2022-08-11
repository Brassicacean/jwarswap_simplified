package edu.osu.jwarswap;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.File;
import java.util.LinkedList;
import java.util.HashMap;

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
	 * interpreting the file as a bipartite edge list.*/
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
		
		// Create the degree sequences and put them in the output list.
		LinkedList <int[]> degSeqs = new LinkedList <int[]>();

		int[] tgtDegSeq = new int[tgtHashMap.size()];
		int i = 0;
//		System.out.println("\ntgthashMap keys");
		for (Integer key: tgtHashMap.keySet()) {
			tgtDegSeq[i] = (int) tgtHashMap.get(key);
			i++;
//			System.out.print(key + ": " + tgtHashMap.get(key) + ", ");
		}
//		System.out.println();
		degSeqs.add(tgtDegSeq);
		int[] srcDegSeq = new int[srcHashMap.size()];
		i = 0;
//		System.out.println("srcHashMap keys:");
		for (Integer key: srcHashMap.keySet()) {
			srcDegSeq[i] = (int) srcHashMap.get(key);
			i++;
//			System.out.print(key + ": " + srcHashMap.get(key) + ", ");
		}
		degSeqs.add(srcDegSeq);		
		return degSeqs;
	}
	
}
