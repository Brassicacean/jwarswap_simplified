package edu.osu.jwarswap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;

public class Parsing {
	
    public static HashMap<Integer, Byte> readColors(String path) throws IOException {
    	/** 
    	 * Read the colors from a file that maps vertices to colors. 
    	 */
    	HashMap<Integer, Byte> vColorHash = new HashMap<Integer, Byte>();
    	// Use this HashMap to keep the colors consistent whether numbers or names are used.
    	HashMap<String, Byte> colors = new HashMap<String, Byte>();
    	colors.put("TF", (byte) 0);
    	colors.put("MIR", (byte) 1);
    	colors.put("GENE", (byte) 2);
    	colors.put("0", (byte) 0);
    	colors.put("1", (byte) 1);
    	colors.put("2", (byte) 2);

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = br.readLine()) != null) {
			    if (line.isEmpty())
			        continue;
			    if (line.startsWith("#")) {
			        System.out.printf("Skipped a line: [%s]\n", line);
			        continue;
			    }
			    String[] tokens = line.split("\\s+");
			    if (tokens.length < 2) {
			        throw new IOException("The input file is malformed!");
			    }
			    int vtx = Integer.parseInt(tokens[0]);
			    if (vColorHash.containsKey(vtx)) {
			    	throw new RuntimeException(vtx + "is listed in the vertex color file more than once.");
			    }
			    byte color = colors.get(tokens[1]);
			    vColorHash.put(vtx, color);
			}
			br.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw e;
		}
        return vColorHash;
    }

	public static LinkedList<int[]> degreeSequences(String graphFile) 
			throws FileNotFoundException{
		int[][] edgeList = parseEdgeListFile(graphFile);
		return Setup.degreeSequences(edgeList);
	}


	public static int[][] parseEdgeListFile(String graphfile)
			throws FileNotFoundException{
		File edgelistFile = new File(graphfile);
		Scanner edgelistScanner = new Scanner(edgelistFile);
		LinkedList<Integer> nodes = new LinkedList<Integer>();
		while(edgelistScanner.hasNextLine()) {
			String line = edgelistScanner.nextLine();
			Scanner lineScanner = new Scanner(line);
			nodes.add(lineScanner.nextInt());
			nodes.add(lineScanner.nextInt());
			lineScanner.close();
		}
		edgelistScanner.close();
		int length = nodes.size() / 2;
		int[][] edgeArr = new int[length][2];
		ListIterator<Integer> nodesIterator = nodes.listIterator();
		for (int i = 0; i < length; i++) {
			edgeArr[i][0] = (int) nodesIterator.next();
			edgeArr[i][1] = (int) nodesIterator.next();
		}
		return edgeArr;
	}	


public static LinkedList<FenwickRandomGraphGenerator> getLayerGeneratorsFromFile(String graphFile, String vColorFile, double factor1, double factor2) 
		throws IOException{
	int[][] edgeList = parseEdgeListFile(graphFile);
	HashMap<Integer, Byte> vColorHash = readColors(vColorFile);
	LinkedList<FenwickRandomGraphGenerator> genList = Setup.getLayerGenerators(edgeList, vColorHash, factor1, factor2);
	return genList;
	}


public static LinkedList<FenwickRandomGraphGenerator> getLayerGeneratorsFromFile(String graphFile, double factor1, double factor2) 
		throws IOException{
	LinkedList<int[]> degSeqs= degreeSequences(graphFile);
	int[] vertexNames = degSeqs.pop();
	int[] tgtDegSeq = degSeqs.pop();
	int[] srcDegSeq = degSeqs.pop();
	FenwickRandomGraphGenerator gen = new FenwickRandomGraphGenerator(srcDegSeq, tgtDegSeq, factor1, factor2);
	gen.assignNames(vertexNames);
	LinkedList<FenwickRandomGraphGenerator> genList = new LinkedList<FenwickRandomGraphGenerator>();
	genList.add(gen);
	return genList;
	}
}
