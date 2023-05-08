package edu.osu.jwarswap;
import java.util.Random;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashMap;

public class EdgeSwitcher {
    private int[][] edgeArr;
    private HashMap <Integer, IntOpenHashSet> adjacencies = new HashMap <Integer, IntOpenHashSet>();
    private int m_edges;
    private Random random = new Random();

    public EdgeSwitcher(int[][] edgeArr) {
        this.edgeArr = edgeArr.clone();
        m_edges = this.edgeArr.length;
        // track the existing adjacencies.
        for (int row = 0; row < m_edges; row++){
            int u = edgeArr[row][0]; int v = edgeArr[row][1];
            if (! adjacencies.containsKey(u)) adjacencies.put(u, new IntOpenHashSet());
            adjacencies.get(u).add(v);
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
        // If the can't be swapped, don't try.
        if (adjacencies.get(v0).contains(u1) | adjacencies.get(u0).contains(v1)) {
            ;;
        }
        // Otherwise, exchange the end points.
        else {
            edgeArr[u][1] = v1;
            edgeArr[v][1] = u1;
            adjacencies.get(v0).remove(v1);
            adjacencies.get(v0).add(u1);
            adjacencies.get(u0).remove(u1);
            adjacencies.get(u0).add(v1);
        }
    }
    public void switchNEdges(int switches) {
    	switches = switches * m_edges;
        for (int i = 0; i < switches; i++) {
            switchEdges();
        }
    }

    public int[][] getEdgeArr() {
        return edgeArr.clone();
    }
}
