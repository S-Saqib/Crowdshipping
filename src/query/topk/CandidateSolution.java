package query.topk;

import java.util.ArrayList;
import java.util.Comparator;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

//import ds.qtrajtree.QuadTrajTree;
import ds.qtree.Node;
import ds.qtree.NodeType;
import java.util.HashSet;
import result.ResultPlotter;

public class CandidateSolution {
/*
    ArrayList<ArrayList<CoordinateArraySequence>> routeGraphs = new ArrayList<ArrayList<CoordinateArraySequence>>();
    ArrayList<Node> qNodes = new ArrayList<Node>();
    QuadTrajTree quadTrajTree;
    double aServe;
    double hServe;
    int id;
    boolean finished;

    public CandidateSolution(QuadTrajTree quadTrajTree, ArrayList<CoordinateArraySequence> routeGraph, int i) {
        this.routeGraphs.add(routeGraph);
        this.aServe = 0;
        finished = false;
        this.quadTrajTree = quadTrajTree;
        this.id = i;
        Node[] qChildren = new Node[4];
        //System.out.println(routeGraph.size());
        Node qNode = quadTrajTree.getQuadTree().getRootNode();
        int count;
        for (count = 1; count == 1 && qNode.getNodeType() != NodeType.LEAF && qNode.getNodeType() != NodeType.EMPTY;) {
            count = 0;
            qChildren[0] = qNode.getNe();
            qChildren[1] = qNode.getSe();
            qChildren[2] = qNode.getSw();
            qChildren[3] = qNode.getNw();
            for (int k = 0; k < 4; k++) {
                ArrayList<CoordinateArraySequence> querySubgraphs = quadTrajTree.clipGraph(qChildren[k], routeGraph);
                //System.out.println(qChildren[k]);
                if (querySubgraphs == null || querySubgraphs.size() == 0) {
                    continue;
                }
                count++;
                qNode = qChildren[k];
            }
        }
        if (count == 1); else {
            //System.out.println(qNode);
            qNode = qNode.getParent();
        }
        this.hServe = quadTrajTree.getTotalNodeTraj(qNode);
        this.qNodes.add(qNode);
        //this.hServe = quadTrajTree.getTotalNodeTraj(quadTrajTree.getQuadTree().getRootNode());
        //this.qNodes.add(quadTrajTree.getQuadTree().getRootNode());
    }

    double fitness() {
        return aServe + hServe;
    }

    boolean relaxStateBinary(ResultPlotter mapView, DiskIO diskIO) {
        ArrayList<ArrayList<CoordinateArraySequence>> newRouteGraphs = new ArrayList<ArrayList<CoordinateArraySequence>>();
        ArrayList<Node> newQNodes = new ArrayList<Node>();
        //System.out.println("id = " + this.id + ", a = " + this.aServe + ", h = " + this.hServe + ", nodes = " + this.qNodes.size() + ", routes = " + this.routeGraphs.size());
        hServe = 0;
        //double from, to, from2, to2;
        //from = System.nanoTime();
        for (int i = 0; i < routeGraphs.size(); i++) {
            Node qNode = qNodes.get(i);
            if (qNode == null || qNode.getNodeType() == NodeType.EMPTY || qNode.getNodeType() == NodeType.LEAF) continue;
            ArrayList<CoordinateArraySequence> routeGraph = routeGraphs.get(i);
            Node[] qChildren = new Node[4];
            qChildren[0] = qNode.getNe();
            qChildren[1] = qNode.getSe();
            qChildren[2] = qNode.getSw();
            qChildren[3] = qNode.getNw();
            //from2 = System.nanoTime();
            HashSet <Integer> served;
            served = new HashSet<Integer>();
            aServe += quadTrajTree.evaluateNodeTrajWithIndexBinary(qNode, routeGraph, served, mapView, diskIO);
            //to2 = System.nanoTime();
            //System.out.println("Main task = " + (to2-from2)/1e3);
            for (int k = 0; k < 4; k++) {
                ArrayList<CoordinateArraySequence> querySubgraph = new ArrayList<CoordinateArraySequence>(quadTrajTree.clipGraph(qChildren[k], routeGraph));

                //System.out.println(routeGraph.size());
                //System.out.println(quadTrajTree.getTotalNodeTraj(qChildren[k]));

                if (querySubgraph == null || querySubgraph.size() == 0) {
                    continue;
                }
                if (qChildren[k].getNodeType() == NodeType.LEAF || qChildren[k].getNodeType() == NodeType.EMPTY) {
                    continue;
                }
                //System.out.println(quadTrajTree.getTotalNodeTraj(qChildren[k]));
                hServe += (int) quadTrajTree.getTotalNodeTraj(qChildren[k]);
                newRouteGraphs.add(querySubgraph);
                newQNodes.add(qChildren[k]);
            }
        }
        //System.out.println("id = " + this.id + ", a = " + this.aServe + ", h = " + this.hServe + ", nodes = " + this.qNodes.size() + ", routes = " + this.routeGraphs.size());
        //to = System.nanoTime();
        //System.out.println("Relax = " + (to - from)/1e3 + " route = " + routeGraphs.size() + " qnode = " + qNodes);
        if (newRouteGraphs.isEmpty()) {
            return false;
        }
        routeGraphs = newRouteGraphs;
        qNodes = newQNodes;
        return true;
    }
    
    /* For Uniform Service Function */
/*
    boolean relaxStateUniform(ResultPlotter mapView) {
        ArrayList<ArrayList<CoordinateArraySequence>> newRouteGraphs = new ArrayList<ArrayList<CoordinateArraySequence>>();
        ArrayList<Node> newQNodes = new ArrayList<Node>();
        //System.out.println("id = " + this.id + ", a = " + this.aServe + ", h = " + this.hServe + ", nodes = " + this.qNodes.size() + ", routes = " + this.routeGraphs.size());
        hServe = 0;
        //double from, to, from2, to2;
        //from = System.nanoTime();
        for (int i = 0; i < routeGraphs.size(); i++) {
            Node qNode = qNodes.get(i);
            if (qNode == null || qNode.getNodeType() == NodeType.EMPTY || qNode.getNodeType() == NodeType.LEAF) continue;
            ArrayList<CoordinateArraySequence> routeGraph = routeGraphs.get(i);
            Node[] qChildren = new Node[4];
            qChildren[0] = qNode.getNe();
            qChildren[1] = qNode.getSe();
            qChildren[2] = qNode.getSw();
            qChildren[3] = qNode.getNw();
            //from2 = System.nanoTime();
            HashSet <Integer> served;
            served = new HashSet<Integer>();
            aServe += quadTrajTree.evaluateNodeTrajWithIndexUniform(qNode, routeGraph, served, mapView);
            //to2 = System.nanoTime();
            //System.out.println("Main task = " + (to2-from2)/1e3);
            for (int k = 0; k < 4; k++) {
                ArrayList<CoordinateArraySequence> querySubgraph = new ArrayList<CoordinateArraySequence>(quadTrajTree.clipGraph(qChildren[k], routeGraph));

                //System.out.println(routeGraph.size());
                //System.out.println(quadTrajTree.getTotalNodeTraj(qChildren[k]));

                if (querySubgraph == null || querySubgraph.size() == 0) {
                    continue;
                }
                if (qChildren[k].getNodeType() == NodeType.LEAF || qChildren[k].getNodeType() == NodeType.EMPTY) {
                    continue;
                }
                //System.out.println(quadTrajTree.getTotalNodeTraj(qChildren[k]));
                hServe += (int) quadTrajTree.getTotalNodeTraj(qChildren[k]);
                newRouteGraphs.add(querySubgraph);
                newQNodes.add(qChildren[k]);
            }
        }
        //System.out.println("id = " + this.id + ", a = " + this.aServe + ", h = " + this.hServe + ", nodes = " + this.qNodes.size() + ", routes = " + this.routeGraphs.size());
        //to = System.nanoTime();
        //System.out.println("Relax = " + (to - from)/1e3 + " route = " + routeGraphs.size() + " qnode = " + qNodes);
        if (newRouteGraphs.isEmpty()) {
            return false;
        }
        routeGraphs = newRouteGraphs;
        qNodes = newQNodes;
        return true;
    }
*/    
}
/*
class SolutionComparator implements Comparator<CandidateSolution> {

    @Override
    public int compare(CandidateSolution o1, CandidateSolution o2) {
        if (o1.fitness() > o2.fitness()) {
            return -1;
        }
        if (o1.fitness() < o2.fitness()) {
            return +1;
        }
        return 0;
    }
}
*/