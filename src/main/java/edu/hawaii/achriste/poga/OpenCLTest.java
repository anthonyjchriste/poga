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

/**
 * Simulates parallel code in a serial manner in-order to debug kernel
 * algorithms without running them on the GPU.
 * 
 * @author Anthony Christe
 */
public class OpenCLTest {

    /**
     * Simulates running the jaccardSimilarity finder in parallel.
     * @see ParallelOverlappingCommunities.cl for documentation on this algorithm.
     */
    public static float[] jaccardSimilarity(int[] adjList, int[] startIndices, int sims) {
        System.out.println(sims);
        float[] jaccards = new float[sims];
        int order = adjList[0];
        for(int i = 0; i < order; i++) {
        int gid = i;
        int keystoneIndex = gid + 1;
        int keystone = adjList[keystoneIndex];

        
        int degree = adjList[keystoneIndex + 1] - keystone;
        int neighborIndexStart = order + keystone + 2;
        int neighborIndexStop = order + keystone + degree + 2;
        int outIndex = startIndices[gid];
        int offset = 0;
        int iDegree;
        int jDegree;
        int intersection = 0;
        int iIndex = 0;
        int jIndex = 0;
        int iMax;
        int jMax;

        if (degree > 1) {
            for (int neighborI = neighborIndexStart; neighborI < neighborIndexStop; neighborI++) {
                for (int neighborJ = neighborI + 1; neighborJ < neighborIndexStop; neighborJ++) {
                    iIndex = adjList[adjList[neighborI] + 1] + order + 2;
                    jIndex = adjList[adjList[neighborJ] + 1] + order + 2;

                    iDegree = adjList[adjList[neighborI] + 2] - adjList[adjList[neighborI] + 1];
                    jDegree = adjList[adjList[neighborJ] + 2] - adjList[adjList[neighborJ] + 1];

                    iMax = iIndex + iDegree;
                    jMax = jIndex + jDegree;

                    while (iIndex < iMax && jIndex < jMax) {
                        if (adjList[iIndex] == adjList[jIndex]) {
                            intersection++;
                            iIndex++;
                            jIndex++;
                            continue;
                        }
                        if (adjList[iIndex] < adjList[jIndex]) {
                            iIndex++;
                        } else {
                            jIndex++;
                        }
                    }

                    //System.out.println(neighborI + "," + neighborJ + " " + adjList[neighborI] + "," + adjList[neighborJ] + " outIdx:" + outIndex + " offset:" + offset + " iDegree:" + iDegree + " jDegree:" + jDegree);
                    jaccards[outIndex + offset] = (float) intersection / (iDegree + jDegree + 2 - intersection); 
                    intersection = 0;
                    offset++;
                }
            }
        }
        }
        return jaccards;
    }
}
