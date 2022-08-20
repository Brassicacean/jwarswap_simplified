/**
 * The MIT License (MIT)

Copyright (c) 2014 Saeed Shahrivari

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

@modified by Mitra Ansariola

Original code is created by Saeed Shahrivari 
@cite : 1. Shahrivari S, Jalili S. Fast Parallel All-Subgraph Enumeration Using Multicore Machines. Scientific Programming. 2015 Jun 16;2015:e901321. 
@code available at : https://github.com/shahrivari/subenum
 */
package edu.osu.netmotifs.subenum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import com.carrotsearch.hppc.LongLongOpenHashMap;


public class CallEnumerateSubGraphs {
	
	public CallEnumerateSubGraphs(int motifSize, String inputGraphPath, String outputPath, int noOfThreads) {

		Graph graph = null;

        try {
        	graph = HashGraph.readStructureFromFile(inputGraphPath);
            if (graph.vertexCount() < 20000) 
                graph = MatGraph.readStructureFromFile(inputGraphPath);

//          logForOutput += graph.getGraphInfo();
//            logForOutput += "Enumeration started at : " + Calendar.getInstance().getTime() + "\n";
//            logForOutput += "Graph loaded in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " msecs.\n";
//            stopwatch.reset().start();

            SMPEnumerator.setMaxCount(Long.MAX_VALUE);
            
            if (motifSize == 1) 
            	OneNodeMotifsEnumeration(inputGraphPath, outputPath, graph.vertexCount());
            else if (motifSize < 6)
            	//TODO: What to do with the LongLongOpenHashMap?
            	SMPEnumerator.enumerateNonIsoInParallel(graph, motifSize, 1);
            else 
            	SMPEnumerator.enumerateNonIsoInParallel(graph, motifSize, 1);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
	
	public void OneNodeMotifsEnumeration(String edgeFile, String outputFile, int vertexCount) {
		int selfLoopCount = 0;
		try {
			InputStream inputStream = new FileInputStream(
					new File(edgeFile));
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(inputStream));
			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				if (line.split("\t")[0].equalsIgnoreCase(line.split("\t")[1]))
					selfLoopCount++;
			}
			
			bufferedReader.close();
			inputStream.close();
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
