package edu.osu.jwarswap;

import java.util.Arrays;
import java.util.Random;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleHeaps;

@Deprecated
public class SampleHopScotch{
	private Random random;
	private double[] freeWeightsArr;
	private int[] indicesArr;
	private IntArrayList usedIndicesList;
	private double[] cumuWeightsArr;
	private double maxCumuWeight;
	private int N;
	
	// For the heap version:
	private int[] heap;
	
	public SampleHopScotch(double[] weights) {
		this.random = new Random();
		this.N = weights.length;
		setup(weights);
	}
	
	public SampleHopScotch(double[] weights, int[] heap, int[] inversionArray) {
		this.random = new Random();
		this.N = weights.length;
		setupHeap(weights, heap, inversionArray);
	}
	
	public void setup(double[] weights) {
		this.cumuWeightsArr = new double[this.N];
		this.usedIndicesList = new IntArrayList();
		
		// Sort the weights and produce an array of indices that tell us where they originally were.
		this.indicesArr = new int[weights.length];
		this.freeWeightsArr = new double[weights.length];

		double[][] pair = new double[weights.length][2];
		for (int i = 0; i < weights.length; i++) {
			pair[i][0] = (double) i;
			pair[i][1] = weights[i];
		}
		  // This should sort indices by weight in reverse.
		Arrays.sort(pair, (a,b)->Double.compare(b[1], a[1]));
		for (int i = 0; i < weights.length; i++) {
			this.indicesArr[i] = (int) pair[i][0];
			this.freeWeightsArr[i] = pair[i][1];
		}
			
		// Assume weights are already sorted.
//		System.out.println(weights[0]);
		this.cumuWeightsArr[0] = this.freeWeightsArr[0];
		for (int i = 1; i < this.freeWeightsArr.length; i++) {
			this.cumuWeightsArr[i] = this.cumuWeightsArr[i - 1] + this.freeWeightsArr[i];
		}
		this.maxCumuWeight = this.cumuWeightsArr[this.N - 1];
	}
	
	public void setupHeap(double[] weights, int[] heap, int[] inversionArray) {
		this.cumuWeightsArr = new double[this.N];
		this.usedIndicesList = new IntArrayList();
		
		// Sort the weights and produce an array of indices that tell us where they originally were.
		this.indicesArr = Arrays.copyOf(inversionArray,  this.N);
		this.freeWeightsArr = weights;
		this.heap = Arrays.copyOf(heap, this.N);
		
		for (int i = 0; i < N; i++) {
			this.indicesArr[i] = this.heap[i];  // TODO: This is incomplete
		}
			
		// Assume weights are already sorted.
//		System.out.println(weights[0]);
		this.cumuWeightsArr[0] = this.freeWeightsArr[0];
		for (int i = 1; i < this.freeWeightsArr.length; i++) {
			this.cumuWeightsArr[i] = this.cumuWeightsArr[i - 1] + this.freeWeightsArr[i];
		}
		this.maxCumuWeight = this.cumuWeightsArr[this.N - 1];

	}
	
	public int selectRandomWithoutReplacement() {
		if (usedIndicesList.size() >= this.N) {
			throw new IllegalStateException("There are no more elements left to sample.");
		}
		// Select a random index from the list of weights, which hasn't been chosen before.
		double targetDistance = this.random.nextDouble() * this.maxCumuWeight;
//		System.out.print("usedIndicesList: ");
//		for (int i: this.usedIndicesList) {
//			System.out.print(i + " ");
//		}
//		System.out.print("\n");
//		System.out.println("targetDistance: " + targetDistance);
//		System.out.println("maxCumuWeight: " + this.maxCumuWeight);
		int guessIdx = 0;
		// Keep track of illegal choices and their effect on other elements
		double lostWeight = 0;
		IntIterator usedIndicesIterator = this.usedIndicesList.intIterator();
		int iteratorPlace = 0;  // Keep track of where in the list this is.
		int justSmaller = -1;  // Placeholder value that prevents problems.
		int i = -1;
		if (usedIndicesIterator.hasNext()) {
			i = usedIndicesIterator.nextInt();
		}
		while(true) {
			// Find the greatest used index smaller then guessIdx.
			while (usedIndicesIterator.hasNext()) {
//				System.out.println(iteratorPlace);
				if (i > guessIdx) {
					break;
				} else {
					justSmaller = i;
					i = usedIndicesIterator.nextInt();
					lostWeight += this.freeWeightsArr[justSmaller];
					iteratorPlace++;
				}
			}
			if (this.cumuWeightsArr[guessIdx] - lostWeight > targetDistance && ! (guessIdx == justSmaller)) {  
				// Want to account for floating-point errors which could cause an illegal choice to be selected.
				this.usedIndicesList.add(iteratorPlace, guessIdx);
				this.maxCumuWeight -= this.freeWeightsArr[guessIdx];
				return this.indicesArr[guessIdx];
			}
			// Move toward the correct choice given the random uniform number.
			double weight = this.freeWeightsArr[guessIdx];
			double hopDistance = targetDistance - (this.cumuWeightsArr[guessIdx] - lostWeight);
			int hopIndices = 1 + (int) (hopDistance / weight);
			guessIdx += hopIndices;
//			System.out.println("guessIdx: " + guessIdx);
//			System.out.println("hopIndices: " + hopIndices);
//			System.out.println("hopDistance: " + hopDistance);
//			System.out.println("weight: " + weight);
		}
	}
}