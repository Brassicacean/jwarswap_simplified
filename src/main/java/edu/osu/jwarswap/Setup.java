package edu.osu.jwarswap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

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
		/** Read the file given by the input string and produce a pair of degree sequences
		 * interpreting the file as a directed edge list.*/
		HashMap <Integer, Integer> srcHashMap = new HashMap <Integer, Integer> ();
		HashMap <Integer, Integer> tgtHashMap = new HashMap <Integer, Integer> ();
		Setup.countDegrees(edgeList, srcHashMap, tgtHashMap);
		
		// Create the degree sequences and put them in the output list.
		int[][] inout = new int[srcHashMap.size()][2];
		int i = 0;
		// List the in- and out-degrees for each vertex.
		for(int vtx: srcHashMap.keySet()) {
			inout[i][0] = srcHashMap.get(vtx);
			inout[i][1] = tgtHashMap.get(vtx);
			i++;
		}
		
		// Sort the degree sequence in reverse order where in-degree trumps out-degree.
		Arrays.sort(inout, new HierarchicalComparator());
		LinkedList <int[]> degSeqs = new LinkedList <int[]>();
		
		int[] tgtDegSeq = new int[tgtHashMap.size()];
		int[] srcDegSeq = new int[tgtHashMap.size()];
		for (i = 0; i < srcHashMap.size(); i++) {
			srcDegSeq[i] = inout[i][0];
			tgtDegSeq[i] = inout[i][1];
		}

		degSeqs.add(tgtDegSeq);
		degSeqs.add(srcDegSeq);		

		return degSeqs;
	}
	
	
	public static LinkedList<FenwickRandomGraphGenerator> getLayerGenerators(
			int[][] edgeList, HashMap<Integer, Byte> vColorHash, double factor) {
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
					subGraphEdgeArray[i][1] = edge[0];
					i++;
				}
				LinkedList<int[]> degSeqs = degreeSequences(subGraphEdgeArray);
				int[] tgtDegSeq = degSeqs.pop();
				int[] srcDegSeq = degSeqs.pop();
				genList.add(new FenwickRandomGraphGenerator(tgtDegSeq, srcDegSeq, factor));
			}
		}
		return genList;
	}
}