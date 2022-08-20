package edu.osu.jwarswap;

import edu.osu.netmotifs.subenum.HashGraph;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
	public static void main(String[] args) {
		//TODO: Make a better CLI.
		final String graphfile = args[0];
		final String rand_outdir = args[1];
		final int ngraphs = (int) Integer.valueOf(args[2]);
		final double factor = (double) Double.valueOf(args[3]);
		final int threads = (int) Integer.valueOf(args[4]);
		String vertexFile = "";
		if (args.length == 6) {
			vertexFile = args[5];
		}
		runWarswap(graphfile, vertexFile, rand_outdir, ngraphs, factor, threads);
	}
	
	private static void runWarswap(String graphfile, String vertexFile, String rand_outdir, int ngraphs, double factor, int threads) {
		/**
		 * Create a number of threads and send them a fixed number of graphs to make so that ngraphs graphs are made in total. 
		 * If vertexFile is given, use it during motif discovery.
		 */
		int[] tgtDegSeq = null;
		int[] srcDegSeq = null;
		try {
			LinkedList <int[]> degSeqs = Parsing.degreeSequences(graphfile);
			tgtDegSeq = degSeqs.pop();
			srcDegSeq = degSeqs.pop();
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + graphfile);
			System.exit(1);
		}
		// Set up the vertex colors.
		try {
			HashGraph.readColors(vertexFile);
		} catch(IOException e) {
			System.err.println("Error reading file: " + vertexFile);
			System.exit(1);
		}
		// For storing the counts of each subgraph type for statistcal analysis.
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();
		// Create the first interval.
		int increment = ngraphs / threads;
		int start = 0, end = increment;
		WarswapTask[] tasks = new WarswapTask[threads];
//		while(end < ngraphs - 1) {
		for (int i = 0; i < threads; i++) {
			tasks[i] = new WarswapTask(tgtDegSeq, srcDegSeq, rand_outdir, start, end, factor);
			Future<?> f = executor.submit(tasks[i]);
			futures.add(f);
			start = end + 1;
			end = Math.min(end + increment, ngraphs - 1);
		}
		
		// Get the counts
		HashMap <Long, LinkedList <Long>> allSubgraphCounts = new HashMap <Long, LinkedList <Long>>();
		for (Future<?> f: futures)
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				System.exit(1);
			}
		for (int i = 0; i < threads; i++) {
			HashMap <Long, LinkedList <Long>> subgraphsCount = tasks[i].getSubgraphCounts();
			for (long key: subgraphsCount.keySet()) {
				if (! allSubgraphCounts.containsKey(key)) allSubgraphCounts.put(key, new LinkedList <Long>());
				allSubgraphCounts.get(key).addAll(subgraphsCount.get(key));	
			}
		}
		//TODO: Must compare original subgraphs counts to randomized counts to get P-values. 
	}
}

	
	
