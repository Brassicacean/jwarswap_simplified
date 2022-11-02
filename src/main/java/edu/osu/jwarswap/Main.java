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

import com.carrotsearch.hppc.LongLongOpenHashMap;

public class Main {
	private static String graphfile = null;
	private static String randOutdir = null;
	private static int ngraphs = 0;
	private static double factor1 = 0;
	private static double factor2 = 0;
	private static int threads = 1	;
	private static String vertexFile = null;
	private static boolean enumerate = true;
	
	private final static String helpMessage = 
			"java -jar jwarswap.jar [Options] graphfile rand-outdir ngraphs factor1 factor2\n" +
			"graphfile: A two-column edge-list separated by tabs.\n" +
			"rand-outdir: A directory to put randomized output graphs into.\n"+
			"ngraphs: Number of random graphs to generate.\n" +
			"factor1: The linear correction factor1 to use for weighted edge selection.\n" +
			"factor2: The quadratic correction factor1 to use for weighted edge selection.\n" +
			"Options:\n" + 
			"\t--threads THREADS, -t THREADS: Use THREADS threads.\n" +
			"\t--vertex-file VERTEX_FILE, -v VERTEX_FILE: Use the file, VERTEX_FILE, to provide the colors of the vertices.\n" +
			"\t--help, -h: Print this help message.\n" +
			"\t--no-motifs, -n: Don't enumarate subgraphs to find motifs.\n" + 
			"\t--motif-size SIZE, -s SIZE: Enumerate motifs of size SIZE. Don't use a size greater than 5.\n" +
			"\t--motif-outfile FILE, -o FILE: Write motif discovery results to FILE." +
			"\t--no-self-loops: Don't allow self-loops during graph randomization.";
	private static String motifsOutfile = null;
	public static void main(String[] args) throws IOException {
		parseArguments(args);
		
//		int[][] edgeArray = Parsing.parseEdgeListFile(graphfile);
//		HashMap<Integer, Byte> vColorHash = Parsing.readColors(vertexFile);
//		WarswapTask.prepareGenerators(edgeArray, vColorHash, factor1);
		WarswapTask[] tasks = runWarswap(graphfile, vertexFile, randOutdir, ngraphs, factor1, factor2, threads);
		if (enumerate) getResults(tasks, motifsOutfile, graphfile);
		System.out.println("All done!");
		System.exit(0);
	}
	
	private static void parseArguments(String[] args) {
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
			default:  // Read positional arguments.
				switch (position) {
					case 0: graphfile = args[i]; break;
					case 1: randOutdir = args[i]; break;
					case 2: ngraphs = (int) Integer.valueOf(args[i]); break;
					case 3: factor1 = (double) Double.valueOf(args[i]); break;
					case 4: factor2 = (double) Double.valueOf(args[i]); break;
				}
				position++;
				break;
			}
			i++;
		}
		if (position != 5) {
			System.err.println("Five positional arguments are required.");
			System.err.println(helpMessage);
			System.exit(1);
		}
	}
	
	private static boolean checkWeights(String graphfile, double factor1, double factor2) {
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
		int m = 0;
		for (int i: srcDegSeq) m += i;
		double K = 1 / (factor1 * m);
		for (int i: srcDegSeq) {
			for(int j: tgtDegSeq) {
				if (1.0 - (double) (i * j) * K + (double) (i * i * j * j) * factor2 < 0.0) return true;
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
		System.out.println("Degree sequences:");
		for (int deg: srcDegSeq) System.out.print(deg + " ");
		System.out.println();
		for (int deg: tgtDegSeq) System.out.print(deg + " ");
		System.out.println();
	}
	
	private static WarswapTask[] runWarswap(String graphfile, String vertexFile, String rand_outdir, int ngraphs, double factor1, double factor2, int threads) 
			throws FileNotFoundException {
		/**
		 * Create a number of threads and send them a fixed number of graphs to make so that ngraphs graphs are made in total. 
		 * If vertexFile is given, use it during motif discovery.
		 */
		printGraphInfo(graphfile);
		if (checkWeights(graphfile, factor1, factor2)) {
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
				tasks[i].prepareGenerators(edgeList, vColorHash, factor1, factor2);
			} else {
				tasks[i].prepareGenerators(edgeList, factor1, factor2);
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
