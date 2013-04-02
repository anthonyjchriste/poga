/**
 * This file is part of a masters thesis work at UH Manoa. No person other than
 * the copyright holder may modify, use, or distribute this work, unless given
 * explicit permission by the copyright holder.
 *
 * Copyright (c) 2013 Anthony Christe
 */
package edu.hawaii.achriste.poga;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.nativelibs4java.util.IOUtils;

/**
 * Loads a .pairs file and stores the file as an adjacency list in one
 * dimension.
 * 
 * @author Anthony Christe
 *
 */
public class Graph {
    /**
     * Stores the 1D array of the graph.
     */
    private int[] adjList;

    /**
     * Initializes a new Graph object using a file of vertex pairs.
     * 
     * @param pairsFileLoc The location of the pairs file.
     */
    public Graph(String pairsFileLoc) {
        init(pairsFileLoc);
    }

    /**
     * Reads pairs file, creates node to neighbor mapping, and converts
     * the mapping into the adjacency list.
     * 
     * @param pairsFileLoc The location of the pairs file.
     */
    private void init(String pairsFileLoc) {
        String pairsFile;
        SortedMap<Integer, SortedSet<Integer>> nodeToNeighborMapping;

        pairsFile = readPairsFile(pairsFileLoc);
        nodeToNeighborMapping = createNodeToNeighborMappings(pairsFile);
        this.adjList = getAdjListFromMap(nodeToNeighborMapping);
    }

    /**
     * Reads in a file where each line is an edge of the graph.
     *
     * Each line read in contains a pair of integers delimited by a space. Each
     * integer represents a vertice and each pair of vertices represent an edge.
     *
     * @param pairsFileLoc The location of the .pairs file.
     * @return The entire network as a String.
     */
    private String readPairsFile(String pairsFileLoc) {
        String pairsFile = null;

        try {
            pairsFile = IOUtils.readText(new File(pairsFileLoc));
        } catch (IOException e) {
            System.err.format("Could not load pairs file %s\n", pairsFileLoc);
        }

        return pairsFile;
    }

    /**
     * Creates a mapping from each node N to each of its neighbors.
     *
     * Using the pairs file, this creates a mappings from each node to a set of
     * its neighbors.
     *
     * @param pairsFile The pairs file as a String.
     * @return A mapping of each node to a set of its neighbors.
     */
    private SortedMap<Integer, SortedSet<Integer>> createNodeToNeighborMappings(
            String pairsFile) {
        SortedMap<Integer, SortedSet<Integer>> neighbors = new TreeMap<>();
        String[] splitLine;

        int[] vals = new int[2];

        // Iterate over file one line at a time and grab each pair of vertices
        for (String line : pairsFile.split("\n")) {
            splitLine = line.split(" ");
            vals[0] = Integer.parseInt(splitLine[0]);
            vals[1] = Integer.parseInt(splitLine[1]);

            if (!neighbors.containsKey(vals[0])) {
                neighbors.put(vals[0], new TreeSet<Integer>());
            }
            neighbors.get(vals[0]).add(vals[1]);

            if (!neighbors.containsKey(vals[1])) {
                neighbors.put(vals[1], new TreeSet<Integer>());
            }
            neighbors.get(vals[1]).add(vals[0]);
        }
        return neighbors;
    }

    /**
     * Creates an adjacency list represented as a one dimensional vector.
     *
     * @param neighbors The mapping of nodes to their neighbors.
     * @return A one dimensional adjacency list representing our network.
     */
    private int[] getAdjListFromMap(
            SortedMap<Integer, SortedSet<Integer>> neighbors) {
        int numVertices = 0;
        int numEdges = 0;

        int adjList[];
        int nodeIndex;
        int neighborIndex;

        // Calculate the number of vertices and the number of edges
        numVertices = neighbors.entrySet().size();

        for (Integer node : neighbors.keySet()) {
            numEdges += neighbors.get(node).size();
        }

        nodeIndex = 1;
        neighborIndex = numVertices + 2;

        // Create the adj list
        adjList = new int[numVertices + numEdges + 2];

        adjList[0] = numVertices;
        adjList[numVertices + 1] = numEdges;
        for (Integer node : neighbors.keySet()) {
            adjList[nodeIndex++] = neighborIndex - numVertices - 2;
            for (Integer neighbor : neighbors.get(node)) {
                adjList[neighborIndex++] = neighbor;
            }
        }

        return adjList;
    }

    public int[] getAdjList() {
        return this.adjList;
    }

    public static void main(String[] args) {
        Graph g = new Graph("simple.pairs");
        System.out.println(java.util.Arrays.toString(g.getAdjList()));
    }
}
