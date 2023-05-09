package edu.osu.jwarswap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;

public class Setup {
	
	public static HashMap<Byte, int[]> extractColorGroups(HashMap<Integer, Byte> vColorHash){
		HashMap<Byte, HashSet<Integer>> colorVertexHash = new HashMap<Byte, HashSet<Integer>>();
		// List the vertices of each color in a hashmap of sets.
		for (Integer vtx: vColorHash.keySet()) {
			Byte color = vColorHash.get(vtx);
			if (! colorVertexHash.containsKey(color)) {
				colorVertexHash.put(color, new HashSet<Integer>());
			}
			colorVertexHash.get(color).add(vtx);
		}
		HashMap<Byte, int[]> layerHash = new HashMap<Byte, int[]>();
		for (Byte color: colorVertexHash.keySet()) {
			HashSet<Integer> vertexSet = colorVertexHash.get(color);
			layerHash.put(color, new int[vertexSet.size()]);
			int i = 0;
			for (Integer vtx: vertexSet) {
				layerHash.get(color)[i] = vtx;
				i++;
			}
			Arrays.sort(layerHash.get(color));
		}
		return layerHash;
	}
	
	
	public static HashMap<Byte, HashMap<Byte, LinkedList<int[]>>> getLayerSubGraphs(
			int[][] edgeList, HashMap<Integer, Byte> vColorHash) {
		/*
		 * For each color combination (layer) that has connections in it,
		 * list the graph generator for that layer.
		 */
		//Data structure for storing all the directed layers. Layers that don't
		// have any edges won't be included.
		HashMap<Byte, HashMap<Byte, LinkedList<int[]>>> layerSubGraphs = 
				new HashMap<Byte, HashMap<Byte, LinkedList<int[]>>>();
		for (int i = 0; i < edgeList.length; i++) {
			Byte color1 = vColorHash.get(edgeList[i][0]);
			Byte color2 = vColorHash.get(edgeList[i][1]);
			
			// Make sure the slots exist in the hash map.
			if (! layerSubGraphs.containsKey(color1)) {
				layerSubGraphs.put(color1, new HashMap<Byte, LinkedList<int[]>>());
			}
			if (! layerSubGraphs.get(color1).containsKey(color2)) {
				layerSubGraphs.get(color1).put(color2, new LinkedList<int[]>());
			}
			int[] edge = {edgeList[i][0], edgeList[i][1]};
			layerSubGraphs.get(color1).get(color2).add(edge);
		}
		return layerSubGraphs;  // Placeholder.
	}


	public static void countDegrees(
			int[][] edgeList,
			HashMap<Integer, Integer> srcHashMap,
			HashMap<Integer, Integer> tgtHashMap) {
		/**
		 * Make a tally of the in- and out-degrees of each vertex. 
		 */
		for (int i = 0; i < edgeList.length; i++) {
			int src = edgeList[i][0];
			int tgt = edgeList[i][1];
			srcHashMap.put(src, srcHashMap.getOrDefault(src, 0) + 1);
			tgtHashMap.put(tgt, tgtHashMap.getOrDefault(tgt, 0) + 1);
		}
		// Ensure the adjacency matrix is square by completing the HashMaps.
		for(int src: srcHashMap.keySet())
			tgtHashMap.put(src, tgtHashMap.getOrDefault(src, 0));
		for(int tgt: tgtHashMap.keySet())
			srcHashMap.put(tgt, srcHashMap.getOrDefault(tgt, 0));
	}

	
	public static LinkedList<int[]> degreeSequences(int[][] edgeList) {
		/** 
		 * Return a list containing the source (left) degree sequence,
		 * the target (right) degree sequence, and an array representing the
		 * respective names of the vertices, in that order.
		 */
		HashMap <Integer, Integer> srcHashMap = new HashMap <Integer, Integer> ();
		HashMap <Integer, Integer> tgtHashMap = new HashMap <Integer, Integer> ();
		Setup.countDegrees(edgeList, srcHashMap, tgtHashMap);
		
		// Create the degree sequences and put them in the output list.
		int[][] inout = new int[srcHashMap.size()][3];
		int i = 0;
		// List the in- and out-degrees for each vertex.
		for(int vtx: srcHashMap.keySet()) {
			inout[i][0] = srcHashMap.get(vtx);
			inout[i][1] = tgtHashMap.get(vtx);
			inout[i][2] = vtx;
			//System.out.println(inout[i][0] + "\t" + inout[i][1] + "\t" + inout[i][2]);
			i++;

		}
		
		// Sort the degree sequence in reverse order where in-degree trumps out-degree.
		Arrays.sort(inout, new HierarchicalComparator());
		
		LinkedList<int[]> degSeqs = new LinkedList<int[]>();
		int[] tgtDegSeq = new int[tgtHashMap.size()];
		int[] srcDegSeq = new int[tgtHashMap.size()];
		int[] vertexNames = new int[tgtHashMap.size()];
		for (i = 0; i < srcHashMap.size(); i++) {
			srcDegSeq[i] = inout[i][0];
			tgtDegSeq[i] = inout[i][1];
			vertexNames[i] = inout[i][2];
		}
		degSeqs.add(srcDegSeq);
		degSeqs.add(tgtDegSeq);
		degSeqs.add(vertexNames);
		return degSeqs;
	}
	
	
	public static LinkedList<FenwickRandomGraphGenerator> getLayerGenerators(
			int[][] edgeList, HashMap<Integer, Byte> vColorHash, double[] coefficients) {
		/**
		 * Create and return a list of FenwickRandomGraphGenerators corresponding to each graph layer.
		 */
		LinkedList<FenwickRandomGraphGenerator> genList = new LinkedList<FenwickRandomGraphGenerator>();
		HashMap<Byte, HashMap<Byte, LinkedList<int[]>>> layerSubGraphs = getLayerSubGraphs(edgeList, vColorHash);
		for (Byte color1: layerSubGraphs.keySet()) {
			for (Byte color2: layerSubGraphs.get(color1).keySet()) {
				LinkedList<int[]> subGraphEdgeList = layerSubGraphs.get(color1).get(color2);
				int[][]subGraphEdgeArray = new int[subGraphEdgeList.size()][2];
				int i = 0;
				for(int[] edge: subGraphEdgeList) {
					subGraphEdgeArray[i][0] = edge[0];
					subGraphEdgeArray[i][1] = edge[1];
					i++;
				}
				LinkedList<int[]> degSeqs = degreeSequences(subGraphEdgeArray);
				int[] vertexNames = degSeqs.pop(), tgtDegSeq = degSeqs.pop(), srcDegSeq = degSeqs.pop();
				FenwickRandomGraphGenerator gen = new FenwickRandomGraphGenerator(tgtDegSeq, srcDegSeq, coefficients);
				gen.assignNames(vertexNames);
				genList.add(gen);
			}
		}
		return genList;
	}
	
	
	public static ArrayList<int[][]> getEdgeLists(
			int[][] edgeList, HashMap<Integer, Byte> vColorHash) {
		/**
		 * Create and return a list of FenwickRandomGraphGenerators corresponding to each graph layer.
		 */
		ArrayList<int[][]> edgeLists = new ArrayList<int[][]>();
		HashMap<Byte, HashMap<Byte, LinkedList<int[]>>> layerSubGraphs = getLayerSubGraphs(edgeList, vColorHash);
		for (Byte color1: layerSubGraphs.keySet()) {
			for (Byte color2: layerSubGraphs.get(color1).keySet()) {
				LinkedList<int[]> subGraphEdgeList = layerSubGraphs.get(color1).get(color2);
				int[][]subGraphEdgeArray = new int[subGraphEdgeList.size()][2];
				int i = 0;
				for(int[] edge: subGraphEdgeList) {
					subGraphEdgeArray[i][0] = edge[0];
					subGraphEdgeArray[i][1] = edge[1];
					i++;
				}
				edgeLists.add(subGraphEdgeArray);
			}
		}
		return edgeLists;
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
