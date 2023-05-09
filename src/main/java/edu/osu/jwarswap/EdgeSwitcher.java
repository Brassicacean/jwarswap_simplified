package edu.osu.jwarswap;
import java.util.Random;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashMap;

public class EdgeSwitcher {
    private int[][] edgeArr;
    private HashMap <Integer, IntOpenHashSet> adjacencies = new HashMap <Integer, IntOpenHashSet>();
    private int m_edges;
    private Random random = new Random();

    public EdgeSwitcher(int[][] _edgeArr) {
    	/**
    	 * edgeArr is copied. The EdgeSwitcher alters the copy incrementally to generate a randomized graph with the same degree sequence. 
    	 */
        this.m_edges = _edgeArr.length;

        this.edgeArr = new int[m_edges][2]; // _edgeArr.clone();
        for (int row = 0; row < m_edges; row++) {
        	this.edgeArr[row][0] = _edgeArr[row][0];
        	this.edgeArr[row][1] = _edgeArr[row][1];
        }
        // track the existing adjacencies.
        for (int row = 0; row < m_edges; row++){
            int v1 = this.edgeArr[row][0]; int v2 = this.edgeArr[row][1];
            if (! this.adjacencies.containsKey(v1)) this.adjacencies.put(v1, new IntOpenHashSet());
            this.adjacencies.get(v1).add(v2);
        }
    }
    private void switchEdges() {
    	// select two random edges that are different
        int u = random.nextInt(m_edges);
        int v = random.nextInt(m_edges - 1);
        if (v == u) v++;
        // Get their starting points and end points
        int v0 = edgeArr[v][0]; int v1 = edgeArr[v][1];
        int u0 = edgeArr[u][0]; int u1 = edgeArr[u][1];
        // If the can't be switched, don't try.
        if (this.adjacencies.get(v0).contains(u1) || this.adjacencies.get(u0).contains(v1)) {
//        	System.out.println("Switching failed.")
            ;;
        } else {  // Otherwise, exchange the end points.
//        	System.out.println("Switching succeeded.");
            edgeArr[u][1] = v1;
            edgeArr[v][1] = u1;
            this.adjacencies.get(v0).remove(v1);
            this.adjacencies.get(v0).add(u1);
            this.adjacencies.get(u0).remove(u1);
            this.adjacencies.get(u0).add(v1);
        }
    }
    public void switchNEdges(int switches) {
    	switches = switches * m_edges;
//    	System.out.println("Actually switching " + m_edges + " times.");
        for (int i = 0; i < switches; i++) {
            switchEdges();
        }
    }

    public int[][] getEdgeArr() {
        return this.edgeArr.clone();
    }
}
