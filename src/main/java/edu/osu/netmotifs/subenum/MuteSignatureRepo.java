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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import com.carrotsearch.hppc.LongLongOpenHashMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Created by Saeed on 3/8/14.
 * Modified by Zachary A. Bright, no longer uses a FileWriter, instead just stores and returns values for individual graphs.
 */

public class MuteSignatureRepo {
    static int capacity = 40 * 1000 * 1000;
    static int motifSize = 3;
    //HashMultiset<BoolArray> labelMap = HashMultiset.create();
    FreqMap labelMap = new FreqMap();
    LongLongOpenHashMap longLabelMap = new LongLongOpenHashMap();
    
    HashMap<String, Long> stringLabelMap = new HashMap<String, Long>();
    
    private boolean verbose = true;
    private ReentrantLock lock = new ReentrantLock();

    public MuteSignatureRepo() {
    }

    public static int getCapacity() {
        return capacity;
    }

    public static void setCapacity(int capacity) {
        MuteSignatureRepo.capacity = capacity;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

//    public void add(Multiset<BoolArray> multiset) {
//    	lock.lock();
//    	for (HashMultiset.Entry<BoolArray> entry : multiset.entrySet()) {
//    		BoolArray label = new SubGraphStructure(entry.getElement().getArray()).getOrderedForm().getAdjacencyArray();
//    		labelMap.add(label, entry.getCount());
//    	}
//    	
//    	if (isVerbose()) {
//    		System.out.printf("Added %,d new signatures. LabelMap size:%,d\n", multiset.elementSet().size(), size());
//    	}
//    	if (size() > capacity)
//    		try {
//    			flush();
//    		} catch (IOException exp) {
//    			exp.printStackTrace();
//    			System.exit(-1);
//    		}
//    	lock.unlock();
//    }
    
    /**
     * MA: Changed to byteArrays
     * @param multiset
     */
    public void add(Multiset<ByteArray> multiset) {
    	lock.lock();
    	for (HashMultiset.Entry<ByteArray> entry : multiset.entrySet()) {
    		ByteArray label = new SubGraphStructure(entry.getElement().getArray()).getOrderedForm().getAdjacencyArray();
    		labelMap.add(label, entry.getCount());
    	}
    	
//        if (isVerbose()) {
//            System.out.printf("Added %,d new signatures. LabelMap size:%,d\n", multiset.elementSet().size(), size());
//        }
    	if (size() > capacity) {
    		//TODO: Deal with this.
    	}
    	lock.unlock();
    }
    
    /**
     * MA: Changed by Mitra to support colored graphs with color coded matrices
     * @param longMap
     * @param k
     */
    public void add(LongLongOpenHashMap longMap, int k) {
    	lock.lock();
    	for (int i = 0; i < longMap.keys.length; i++) {
    		if (longMap.allocated[i]) {
    			long key = longMap.keys[i];
    			key = ByteArray.byteArrayToLong(new SubGraphStructure(ByteArray.longToByteArray(key, k * k * 2)).getOrderedForm().getAdjacencyArray().getArray());
    			longLabelMap.putOrAdd(key, longMap.values[i], longMap.values[i]);
    		}
    	}
    	
//        if (isVerbose()) {
//            System.out.printf("Added %,d new signatures. LabelMap size:%,d\n", longMap.size(), size());
//        }
    	if (size() > capacity) flush();
    	lock.unlock();
    }
    
    
    public void add(HashMap<String, Long> longMap, int k) {
        lock.lock();
        Iterator<String> keySetItr = longMap.keySet().iterator();
        while (keySetItr.hasNext()) {
			String subgStr = (String) keySetItr.next();
			String isomorphicKey = ByteArray.byteArrayToString(new SubGraphStructure(ByteArray.stringToByteArray(subgStr)).getOrderedForm().getAdjacencyArray().getArray());
			Long count = stringLabelMap.get(isomorphicKey);
			if (count == null)
				stringLabelMap.put(isomorphicKey, longMap.get(subgStr).longValue());
			else
				stringLabelMap.put(isomorphicKey, longMap.get(subgStr).longValue() + count.longValue());
				
		}
        if (size() > capacity) {
        	//TODO: Do something to deal with this.
        }
        lock.unlock();
    }


    public int size() {
        return labelMap.size() + longLabelMap.size();
    }
    
    /**
     * @Modified by Mitra Ansariola
     * @author Saeed
     * 
     * Modify the following code:
     * 1- Change the name of method to : writeToOutputFile
     * 2- Use adj matrix representation of each subgraph instead of long value
     * 
     * @throws IOException
     */
    
    public LongLongOpenHashMap flush() {
    	lock.lock();
    	
    	LongLongOpenHashMap outHashMap = longLabelMap.clone();
    	labelMap.clear();
    	longLabelMap.clear();
    	lock.unlock();
    	return outHashMap;
    }
    
}
