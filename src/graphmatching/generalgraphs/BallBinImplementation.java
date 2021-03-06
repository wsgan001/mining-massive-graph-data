package graphmatching.generalgraphs;

import com.sun.tools.javac.util.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 *
 * @author avishekanand
 */
public class BallBinImplementation {

    private final UndirectedGraph<String, DefaultEdge> inputGraph;
    private final Set<DefaultEdge> matchedEdges;
    private HashMap<String, Integer> binLabels;

    public BallBinImplementation(UndirectedGraph<String, DefaultEdge> graph) {
        this.inputGraph = graph;

        matchedEdges = new HashSet<>();
    }

    public Set<DefaultEdge> run(int loopLimit) {
        //initialize matching
        HashMap<String, String> matching = new HashMap<>();

        ///Track Discarded edges
        HashSet<DefaultEdge> discarded = new HashSet<>();

        //initialize bins
        binLabels = new HashMap<>();
        for (String vertex : inputGraph.vertexSet()) {
            binLabels.put(vertex, 0);
        }

        //initialize used bins
        HashSet<String> usedBins = new HashSet<>();

        //initialize used balls
        HashSet<String> usedBalls = new HashSet<>();

        //process all vertices
        for (String incomingBall : inputGraph.vertexSet()) {
            //Skip vertex if its used as a bin
            if (usedBins.contains(incomingBall)) {
                continue;
            }

            // get valid choices i.e. all choices which have not been used as balls 
            // in other words have been in matchings as balls
            //Set<String> choices = neighborsOfNode(currentNode);
            Set<String> choices = undirectedNeighborsOfNode(incomingBall, usedBalls);

            //determine the choice with the lowest label value
            Pair<String, String> topChoices = getBestCandidates(choices, binLabels);

            String bestBin = topChoices.fst;
            String nextBestBin = topChoices.snd;

            //If the best choice > n-1, then we discard the item
            if (bestBin == null || binLabels.get(bestBin) >= loopLimit) {
                DefaultEdge edge = inputGraph.getEdge(incomingBall, bestBin);
                discarded.add(edge);
                continue;
            }

            //iterate until the bestchoice is unoccupied
            while (isOccupied(bestBin, matching)) {
                //Update matching and evict already matched item
                incomingBall = updateMatching(incomingBall, bestBin, matching);

                //update labels 
                updateLabels(bestBin, nextBestBin, binLabels, loopLimit);

                //re-evaluate the choices for the evicted item
                topChoices = getBestCandidates(undirectedNeighborsOfNode(incomingBall, usedBalls), binLabels);

                bestBin = topChoices.fst;
                nextBestBin = topChoices.snd;

                //If the best choice > n-1, then we discard the item
                if (bestBin == null || binLabels.get(bestBin) >= loopLimit) {
                    discarded.add(inputGraph.getEdge(incomingBall, bestBin));
                    break;
                }
            }

            //If the best choice > n-1, then we discard the item
            if (bestBin == null || binLabels.get(bestBin) >= loopLimit) {
                continue;
            }

            //finally insert the current node into the empty best choice
            updateMatching(incomingBall, bestBin, matching);
            updateLabels(bestBin, nextBestBin, binLabels, loopLimit);

            usedBalls.add(incomingBall);
            usedBins.add(bestBin);
            binLabels.remove(incomingBall);
        }

        //construct matching
        constructMatching(matching);

        checkAugmentingPath();
        
        return matchedEdges;
    }

    private boolean isOccupied(String bestChoice, HashMap<String, String> matching) {
        return matching.containsKey(bestChoice) ? true : false;
    }

    private String updateMatching(String currentNode, String bestMatchedNeighbor, HashMap<String, String> matching) {
        String evictedItem = matching.get(bestMatchedNeighbor);
        matching.put(bestMatchedNeighbor, currentNode);

        return evictedItem;
    }

    private void updateLabels(String bestLocationChoice, String nextBestLocationChoice,
        HashMap<String, Integer> labels, int looplimit) {
        int nextBestLabel = (nextBestLocationChoice == null)
            ? looplimit - 1 : labels.get(nextBestLocationChoice);

        labels.put(bestLocationChoice, nextBestLabel + 1);
    }

    private HashSet<String> undirectedNeighborsOfNode(String currentNode, HashSet<String> usedBalls) {

        HashSet<String> choices = new HashSet<>();

        //Construct a set of neighbors
        for (DefaultEdge edge : inputGraph.edgesOf(currentNode)) {
            String nbr = inputGraph.getEdgeTarget(edge);
            nbr = nbr.equals(currentNode) ? inputGraph.getEdgeSource(edge) : nbr;

            if (usedBalls != null && usedBalls.contains(nbr)) {
                continue;
            }
            choices.add(nbr);
        }
        return choices;
    }

    private Pair<String, String> getBestCandidates(Set<String> choices, HashMap<String, Integer> labels) {
        if (choices.size() == 1) {
            return new Pair(choices.toArray()[0], null);
        }

        int bestlabel = Integer.MAX_VALUE;
        int nextBestlabel = Integer.MAX_VALUE;

        String bestChoice = null;
        String nextBestChoice = null;

        //System.out.print("Finding best choice from : ");
        for (String choice : choices) {
            int label;
            label = labels.get(choice);

            //System.out.print(choice + "(" + label + ")");
            if (label < bestlabel) {
                nextBestlabel = bestlabel;
                nextBestChoice = bestChoice;
                bestlabel = label;
                bestChoice = choice;
            } else if (label < nextBestlabel) {
                nextBestlabel = label;
                nextBestChoice = choice;
            }
        }

        //System.out.println("Best Choices : " + bestChoice + ", " + nextBestChoice);
        return new Pair(bestChoice, nextBestChoice);
    }

    private void constructMatching(HashMap<String, String> matching) {
        for (String source : matching.keySet()) {

            String target = matching.get(source);
            if (inputGraph.containsEdge(source, target)) {
                DefaultEdge edge = inputGraph.getEdge(source, target);
                matchedEdges.add(edge);
            }
        }
    }

    public Set<DefaultEdge> getMatching() {
        return matchedEdges;
    }

    public void checkAugmentingPath() {
        Set<DefaultEdge> matching = getMatching();

        HashMap<String, String> fwdMatching = new HashMap<>();
        HashMap<String, String> invMatching = new HashMap<>();

        //initialized matching
        HashSet<String> boundVertices = new HashSet<>();
        for (DefaultEdge edge : matching) {
            String edgeSource = inputGraph.getEdgeSource(edge);
            String edgeTarget = inputGraph.getEdgeTarget(edge);

            boundVertices.add(edgeSource);
            boundVertices.add(edgeTarget);

            fwdMatching.put(edgeSource, edgeTarget);
            invMatching.put(edgeTarget, edgeSource);
        }

        HashSet<String> nodesWithfreeVertices = new HashSet<>();

        //find bound vertices which are connected with free vertices
        for (String candidate : boundVertices) {
            HashSet<String> neighbors = undirectedNeighborsOfNode(candidate, null);
            neighbors.removeAll(boundVertices);

            if (!neighbors.isEmpty()) {
                nodesWithfreeVertices.add(candidate);
            }
        }

        int cnt = 0;
        for (String nodeWithFreeVertex : nodesWithfreeVertices) {

            String partner = (fwdMatching.containsKey(nodeWithFreeVertex))
                ? fwdMatching.get(nodeWithFreeVertex)
                : invMatching.get(nodeWithFreeVertex);

            LinkedList<String> dfsPath = new LinkedList<>();
            dfsPath.add(nodeWithFreeVertex);

            boolean hasAugPath = doDFS(partner, dfsPath, boundVertices, nodesWithfreeVertices,
                fwdMatching, invMatching, true);
            if (hasAugPath) {
                printAugmentingPath(dfsPath, boundVertices, fwdMatching, invMatching);
                cnt++;
            }
        }
        System.out.println("Has " + cnt + " augmenting paths..");
    }

    private boolean doDFS(String current, LinkedList<String> dfsPath,
        HashSet<String> boundVertices,
        HashSet<String> nodesWithfreeVertices,
        HashMap<String, String> fwdMatching,
        HashMap<String, String> invMatching,
        boolean outgoing) {
        dfsPath.add(current);

        //base case
        if (nodesWithfreeVertices.contains(current) && outgoing) {
            return true;
        }

        // in case of an incoming bound node choose the matched counterpart
        if (!outgoing) {
            String cand = fwdMatching.containsKey(current) ? fwdMatching.get(current)
                : invMatching.get(current);

            //flip switch
            outgoing = outgoing ? false : true;
            return doDFS(cand, dfsPath, boundVertices, nodesWithfreeVertices, fwdMatching, invMatching,
                true);
        } else {
            HashSet<String> candidates = undirectedNeighborsOfNode(current, null);
            candidates.removeAll(dfsPath);

            //flip switch
            outgoing = outgoing ? false : true;

            for (String candidate : candidates) {
                boolean hasAugPath = doDFS(candidate, dfsPath, boundVertices, nodesWithfreeVertices,
                    fwdMatching, invMatching, outgoing);
                if (hasAugPath) {
                    return true;
                } else {
                    dfsPath.remove(candidate);
                }
            }
        }
        return false;
    }

    private void printAugmentingPath(LinkedList<String> dfsPath, HashSet<String> boundVertices, HashMap<String, String> fwdMatching, HashMap<String, String> invMatching) {

        if ((dfsPath.size() + 2) > 6) {
            return;
        }

        System.out.println("Augmenting Path of path length : " + (dfsPath.size() + 2));
        String freeVertex = "";

        System.out.println("\t Free Vertex --> ");
        for (String node : dfsPath) {
            String printnode = node;
            if (binLabels.containsKey(node)) {
                printnode = node + " (" + binLabels.get(node) + ") ";
            }

            if (fwdMatching.containsKey(node)) {
                printnode = printnode + " <==> " + fwdMatching.get(node);
            } else if (invMatching.containsKey(node)) {
                printnode = printnode + " <==> " + invMatching.get(node);
            }
            System.out.println("\t" + printnode);
            freeVertex = node;
        }

        HashSet<String> freecandidates = undirectedNeighborsOfNode(freeVertex, null);
        freecandidates.removeAll(boundVertices);

        for (String freecandidate : freecandidates) {
            freeVertex = freecandidate;
            if (binLabels.containsKey(freecandidate)) {
                freeVertex = freeVertex + "FV ( " + binLabels.get(freecandidate) + ")";
            }
        }

        System.out.print(freeVertex);
        System.out.println("");
    }

}
