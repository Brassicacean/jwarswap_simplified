package edu.osu.jwarswap;

import edu.osu.netmotifs.subenum.ByteArray;
import edu.osu.netmotifs.subenum.HashGraph;
import edu.osu.netmotifs.subenum.MatGraph;

import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.lang.Math;

import com.carrotsearch.hppc.LongLongOpenHashMap;

public class Main {
	private static String graphfile = null;
	private static String randOutdir = null;
	private static int ngraphs = 0;
	private static double[] coefficients;
	private static int threads = 1;
	private static String vertexFile = null;
	private static boolean enumerate = true;
	private static boolean entropy = false;
	
	private final static String helpMessage = 
			"java -jar jwarswap.jar [Options] graphfile rand-outdir ngraphs factor1 factor2... factorN\n" +
			"graphfile: A two-column edge-list separated by tabs.\n" +
			"rand-outdir: A directory to put randomized output graphs into.\n"+
			"ngraphs: Number of random graphs to generate.\n" +
			"factor1..N: The factors to use for weighted edge selection.\n" +
			"Options:\n" + 
			"\t--threads THREADS, -t THREADS: Use THREADS threads.\n" +
			"\t--vertex-file VERTEX_FILE, -v VERTEX_FILE: Use the file, VERTEX_FILE, to provide the colors of the vertices.\n" +
			"\t--help, -h: Print this help message.\n" +
			"\t--no-motifs, -n: Don't enumarate subgraphs to find motifs.\n" + 
			"\t--motif-size SIZE, -s SIZE: Enumerate motifs of size SIZE. Don't use a size greater than 5.\n" +
			"\t--motif-outfile FILE, -o FILE: Write motif discovery results to FILE." +
			"\t--no-self-loops: Don't allow self-loops during graph randomization." +
			"\t--entropy: Only compute the sample entropy, don't save any graphs.";
	private static String motifsOutfile = null;
	public static void main(String[] args) throws IOException {
		parseArguments(args);
		
//		int[][] edgeArray = Parsing.parseEdgeListFile(graphfile);
//		HashMap<Integer, Byte> vColorHash = Parsing.readColors(vertexFile);
//		WarswapTask.prepareGenerators(edgeArray, vColorHash, factor1);
		WarswapTask[] tasks = runWarswap(graphfile, vertexFile, randOutdir, ngraphs, coefficients, threads);
		if (enumerate) getResults(tasks, motifsOutfile, graphfile);
		System.out.println(getSwaps(tasks) + " swaps were made");
		System.out.println("All done!");
		System.exit(0);
	}
	
	
	private static void parseArguments(String[] args) {
		LinkedList<Double> coefficientList = new LinkedList<Double>();
		int position = 0;
		int i = 0;
		while (i < args.length) {
			switch (args[i]) {
			case "--no-self-loops":
				FenwickEdgeGenerator.setSelfLoop(false);
				break;
			case "--no-motifs": case "-n":
				enumerate = false;
				WarswapTask.setEnumerate(false);
				break;
			case "--motif-size": case "-s":
				i++;
				WarswapTask.setMotifSize(Integer.valueOf(args[i]));
				break;
			case "--threads": case "-t" :
				i++;
				threads = (int) Integer.valueOf(args[i]);
				break;
			case "--vertex-file": case "-v":
				i++;
				vertexFile = args[i];
				break;
			case "--help": case "-h":
				System.out.println(helpMessage);
				System.exit(0);
			case "--motif-outfile": case "-o":
				i++;
				motifsOutfile = args[i];
				break;
			case "--entropy": 
				entropy = true;
				WarswapTask.setSaveGraphs(false);  //TODO: Consider making this part of an independent option.
				WarswapTask.setEntropy(true);
				break;
			default:  // Read positional arguments.
				switch (position) {
					case 0: graphfile = args[i]; break;
					case 1: randOutdir = args[i]; break;
					case 2: ngraphs = (int) Integer.valueOf(args[i]); break;
					default: coefficientList.add(Double.valueOf(args[i]));
				}
				position++;
				break;
			}
			i++;
		}
		if (position <= 3) {
			System.err.println("At least three positional arguments are required.");
			System.err.println(helpMessage);
			System.exit(1);
		}
		coefficients = new double[coefficientList.size()];
		int idx = 0;
		for (Double coef: coefficientList) {
			coefficients[idx] = (double) coef;
			idx++;
		}
	}
	
	private static boolean checkWeights(String graphfile, double[] coefficients) {
		int[] srcDegSeq = null, tgtDegSeq = null;
		try {
			LinkedList<int[]> degSeqs = Parsing.degreeSequences(graphfile);
			srcDegSeq = degSeqs.pop();
			tgtDegSeq = degSeqs.pop();
//			degSeqs.pop();  // Discard the names. 
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + graphfile);
			System.exit(1);
		}
		for (int deg1: srcDegSeq) {
			for(int deg2: tgtDegSeq) {
				double weight = 1.0;
				for (int i = 0; i < coefficients.length; i++) {
					weight += coefficients[i] * Math.pow(deg1 * deg2, i + 1);
				}
				if (weight <= 0) return true;
			}
		}
		return false;
	}
	
	private static void printGraphInfo(String graphFile) {
		int[] srcDegSeq = null, tgtDegSeq = null;
		try {
			LinkedList<int[]> degSeqs = Parsing.degreeSequences(graphfile);
			srcDegSeq = degSeqs.pop();
			tgtDegSeq = degSeqs.pop();
//			degSeqs.pop();  // Discard the names. 
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + graphfile);
			System.exit(1);
		}
		//System.out.println("Degree sequences:");
		//for (int deg: srcDegSeq) System.out.print(deg + " ");
		//System.out.println();
		//for (int deg: tgtDegSeq) System.out.print(deg + " ");
		//System.out.println();
	}
	
	private static WarswapTask[] runWarswap(String graphfile, String vertexFile, String rand_outdir, int ngraphs, double[] coefficients, int threads) 
			throws FileNotFoundException {
		/**
		 * Create a number of threads and send them a fixed number of graphs to make so that ngraphs graphs are made in total. 
		 * If vertexFile is given, use it during motif discovery.
		 */
		printGraphInfo(graphfile);
		if (checkWeights(graphfile, coefficients)) {
			System.out.println("Invalid factors: There is a source-target pair with a negative sampling weight.");
			System.exit(1);
		}
		// Set up the vertex colors.
		HashMap<Integer, Byte> vColorHash = null;
		if (vertexFile != null) {
			try {
				vColorHash = Parsing.readColors(vertexFile);
				HashGraph.assignColors(vColorHash);
				MatGraph.assignColors(vColorHash);
			} catch(IOException e) {
				System.err.println("Error reading file: " + vertexFile);
				System.exit(1);
			}
		}
		// For storing the counts of each subgraph type for statistical analysis.
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();
		// Create the first interval.
		int increment = ngraphs / threads;
		int start = 0, end = increment;
		WarswapTask[] tasks = new WarswapTask[threads];
		int[][] edgeList = Parsing.parseEdgeListFile(graphfile);
		for (int i = 0; i < threads; i++) {
			tasks[i] = new WarswapTask(rand_outdir, start, end);
			// Must set up the graph generators. If there is a color file, then there is a
			// different procedure, because there are different layers that must be created
			// separately.
			if (vertexFile != null) {
				tasks[i].prepareGenerators(edgeList, vColorHash, coefficients);
			} else {
				tasks[i].prepareGenerators(edgeList, coefficients);
			}
			Future<?> f = executor.submit(tasks[i]);
			futures.add(f);
			start = end + 1;
			end = Math.min(end + increment, ngraphs - 1);
		}
		for (Future<?> f: futures)
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				System.exit(1);
			}
		return tasks;
	}
	
	
	private static int getSwaps(WarswapTask[] tasks) {
		int swaps = 0;
		for (WarswapTask task: tasks) {
			swaps += task.getSwapCount();
		}
		return swaps;
	}
	
	
	private static void getResults(WarswapTask[] tasks, String motifOutfile, String graphfile) {
		// Get the counts
		HashMap <Long, LinkedList <Long>> allSubgraphCounts = new HashMap <Long, LinkedList <Long>>();
		for (WarswapTask task: tasks) {
			HashMap <Long, LinkedList <Long>> subgraphsCount = task.getSubgraphCounts();
			for (long key: subgraphsCount.keySet()) {
				if (! allSubgraphCounts.containsKey(key)) allSubgraphCounts.put(key, new LinkedList <Long>());
				allSubgraphCounts.get(key).addAll(subgraphsCount.get(key));	
			}
		}
		int[][] edgeArr = null;
		try {
			edgeArr = Parsing.parseEdgeListFile(graphfile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		LongLongOpenHashMap origSubgraphs = WarswapTask.getSubgraphs(edgeArr);
		String outBuffer = "Adj. matrix\tZ-Score\tP-Value\n";
		for (long key: origSubgraphs.keys) {
		// Fill lists with zeros until all graphs have a count.
		// (Normally, if a subgraph doesn't show up, it doesn't get a count.)
			if (! allSubgraphCounts.containsKey(key)) {
				allSubgraphCounts.put(key, new LinkedList<Long>());
			}
			while (allSubgraphCounts.get(key).size() < tasks.length) {
				allSubgraphCounts.get(key).add((long) 0);
			}
			if (origSubgraphs.get(key) > 0) {
				outBuffer = outBuffer.concat(
						subgraphInfo(key, WarswapTask.getMotifSize(),allSubgraphCounts.get(key), origSubgraphs.get(key)));
			}
		}
		if (motifOutfile == null) {
			System.out.print(outBuffer);
		} else {
			try {
				FileWriter outFileWriter = new FileWriter(motifOutfile);
				outFileWriter.write(outBuffer);
				outFileWriter.close();
			} catch(IOException e) {
				System.err.println("An error occured while attempting to create " + motifOutfile);
				System.exit(1);
			}
		}
	}
	
	private static String subgraphInfo(long subgID, int motifSize, LinkedList<Long> counts, long original) {
		String outBuffer = "";
		double pValue = Statistics.pValue(counts, original);
		double zScore = Statistics.zScore(counts, original);
		byte[] adjMatrix = ByteArray.longToByteArray(subgID, 2* motifSize * motifSize);
		int adjPos = 0;  // track position in the adjacency matrix
		// Write the first row.
		for (int column = 0; column < motifSize; column++) {
			outBuffer = outBuffer.concat(adjMatrix[adjPos] + " ");
			adjPos++;
		}
		// Write the Z-score and P-value. 
		outBuffer = outBuffer.concat("\t" + zScore + "\t" + pValue + "\n");
		// Finish writing the rows.
		for (int row = 1; row < motifSize; row++) {
			for (int column = 0; column < motifSize; column++) {
				outBuffer = outBuffer.concat(adjMatrix[adjPos] + " ");
				adjPos++;
			}
			outBuffer = outBuffer.concat("\n");
		}
		outBuffer = outBuffer.concat("\n");  // One more line to separate entries.
		return outBuffer;
	}
}
