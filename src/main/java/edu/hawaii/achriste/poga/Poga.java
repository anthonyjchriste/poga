/**
 * This program manages calls to the OpenCL framework in order to perform edge
 * similarity analysis and hierarchical clustering.
 *
 * This file is part of a masters thesis work at UH Manoa. No person other than
 * the copyright holder may modify, use, or distribute this work, unless given
 * explicit permission by the copyright holder.
 *
 * Copyright (c) 2013 Anthony Christe
 */
package edu.hawaii.achriste.poga;

import java.io.IOException;

import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.util.IOUtils;
import java.io.File;

/**
 * Manages the setup/teardown and execution of various kernels on OpenCL
 * devices for overlapping graph algorithms. 
 * 
 * @author Anthony Christe
 */
public class Poga {
    /**
     * OpenCL context object.
     */
    private CLContext context = null;
    
    /**
     * OpenCL queue object.
     */
    private CLQueue queue = null;
    
    /**
     * OpenCL program object.
     */
    private CLProgram program = null;
    
    /**
     * Locations of source file which contains OpenCL kernels for this 
     * program.
     */
    private final String PROGRAM_NAME = "src/main/opencl/ParallelOverlappingCommunities.cl";
    
    /**
     * Kernel used for finding Jaccard similarities on non-weighted,
     * non-directed graphs.
     */
    private final String JACCARD_KERNEL = "jaccardSimilarity";

    /**
     * Create a context object, queue object, and program object.
     */
    public Poga() {
        context = JavaCL.createBestContext();
        queue = context.createDefaultQueue();

        try {
            String src = IOUtils.readText(new File(PROGRAM_NAME));
            program = context.createProgram(src);
        } catch (NullPointerException | IOException e) {
            System.err.format("Unable to open program source file %s\n", PROGRAM_NAME);
        }
    }

    /**
     * Calculates the binomial coefficient (n choose k) using the multiplicative
     * method.
     *
     * @param n The total number of items to choose from.
     * @param k The numbers of items to choose.
     * @return The binomial coefficient (n choose k).
     */
    private int binomialCoefficient(float n, float k) {
        int result = 1;
        for (int i = 1; i <= k; i++) {
            result *= (n - (k - i)) / i;
        }
        return result;
    }

    /**
     * Finds the Jaccard distance between any two pairs of edges connected by a
     * keystone.
     */
    public void runEdgeSimilarities(String pairsFile) {
        Graph graph = new Graph(pairsFile);
        int[] graphAdjList = graph.getAdjList();

        Pointer<Integer> adjListPtr = Pointer.pointerToInts(graphAdjList);

        // Find the number of similarities and create start indexes for the output
        // TODO: Can this be paralellized? Is it worth it?
        int similarities = 0;
        int degree;
        int[] startIndices = new int[graphAdjList[0]];
        int startIndex = 0;

        for (int i = 1; i < graphAdjList[0] + 1; i++) {
            degree = graphAdjList[i + 1] - graphAdjList[i];

            if (degree > 1) {
                similarities += this.binomialCoefficient(degree, 2);
                startIndices[i - 1] = startIndex;
                startIndex += this.binomialCoefficient(degree, 2);
            } else {
                startIndices[i - 1] = -1;
            }
        }

        //System.out.println(Arrays.toString(OpenCLTest.jaccardSimilarity(graphAdjList, startIndices, similarities)));
        
        
        Pointer<Integer> startIndicesPtr = Pointer.pointerToInts(startIndices);
        Pointer<Float> jaccardsPtr = Pointer.allocateFloats(similarities);

        CLBuffer<Integer> adjList = context.createBuffer(Usage.Input, adjListPtr);
        CLBuffer<Integer> startIndicesBuf = context.createBuffer(Usage.Input, startIndicesPtr);
        CLBuffer<Float> jaccards = context.createBuffer(Usage.Output, jaccardsPtr);

        CLKernel jaccardSimilaritiesKernel = program.createKernel(JACCARD_KERNEL);
        jaccardSimilaritiesKernel.setArgs(adjList, startIndicesBuf, jaccards);

        CLEvent jaccardSimilaritiesEvent = jaccardSimilaritiesKernel.enqueueNDRange(queue, new int[]{graphAdjList[0]});
        jaccardsPtr = jaccards.read(queue, jaccardSimilaritiesEvent);

        this.writeResults(jaccardsPtr.getFloats());
        
    }
    
    public void writeResults(float[] results) {
        try (java.io.BufferedWriter out = new java.io.BufferedWriter(new java.io.FileWriter("out.jaccs"))) {
            for(int i = 0; i < results.length; i++) {
                out.append(Float.toString(results[i]) + "\n");
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            System.err.println("Could not write to output file");
            e.printStackTrace();
        }
                
    }

    public static void main(String[] args) {
        Poga poga = new Poga();
        poga.runEdgeSimilarities(args[0]);
    }
}
