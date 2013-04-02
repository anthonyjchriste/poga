/*
* This file contains OpenCL kernel source code with a variety of kernels that provide
* parallel computations for finding the Jaccard distance and hierarchical clustering.
*
* This file is part of a masters thesis work at UH Manoa. No person
* other than the copyright holder may modify, use, or distribute this work, unless
* given explicit permission by the copyright holder.
* 
* Copyright (c) 2013 Anthony Christe
*/

/*
 * This kernel finds the Jaccard similarities for all edge-pairs connected to a
 * keystone for a given graph.
 *
 * @param adjList       (IN) 1D array containing Graph (see: documentation)
 * @param startIndices  (IN) Location in jaccards for this thread to start writing similarities
 * @param jaccard       (OUT) Where results are written to
 */
__kernel void jaccardSimilarity(__global const int * adjList,      
                                __global const int * startIndices,
                                __global float * jaccards) {

    /* Each thread is assigned a vertex */
    int gid = get_global_id(0);
    
    /* We add one to compensate for the order index */
    int keystoneIndex = gid + 1;
    
    /* Which vertice are we currently looking at */
    int keystone = adjList[keystoneIndex];
    
    /* The order of this graph */
    int order = adjList[0];
    
    /* The degree of this vertex */
    int degree = adjList[keystoneIndex + 1] - keystone;
    
    /* Locations of all neighbors to this vertex (absolute) on the adj. list */
    int neighborIndexStart = order + keystone + 2;
    int neighborIndexStop = order + keystone + degree + 2;
    
    /* Output location this thread can start writing to */
    int outIndex = startIndices[gid];
    
    /* Offset to write each result at */
    int offset = 0;
    
    /* The degree of neighbors i, j */
    int iDegree, jDegree;
    
    /* Size of the intersection between neighbors i, j */
    int intersection = 0;
    
    /* Used when comparing two sets of neighbors for intersection size */
    int iIndex = 0;
    int jIndex = 0;
    
    /* Used to determine when to stop searching for intersection vertices */
    int iMax, jMax;

    /* If the current node is a keystone */
    if (degree > 1) {
        
        /* Loop over all pairs of neighbors to this vertex */
        for (int neighborI = neighborIndexStart; neighborI < neighborIndexStop; neighborI++) {
            for (int neighborJ = neighborI + 1; neighborJ < neighborIndexStop; neighborJ++) {
                
                /* Find the starting locations of each neighbor in the appropriate sub-index */
                iIndex = adjList[adjList[neighborI] + 1] + order + 2;
                jIndex = adjList[adjList[neighborJ] + 1] + order + 2;
                
                /* Find the degree of each neighbor */
                iDegree = adjList[adjList[neighborI] + 2] - adjList[adjList[neighborI] + 1];
                jDegree = adjList[adjList[neighborJ] + 2] - adjList[adjList[neighborJ] + 1];
                
                /* Calculate the max indices for each neighbor */
                iMax = iIndex + iDegree;
                jMax = jIndex + jDegree;

                /* Find the intersection between the two neighbors */
                while(iIndex < iMax && jIndex < jMax) {
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
                
                /* Output the jaccard similarity and increase the offset */
                jaccards[outIndex + offset] = (float) intersection / (iDegree + jDegree + 2 - intersection);
                offset++;
                
                /* Reset the similarity */
                intersection = 0;
            }
        }
    }
}


