package query.topk;

import java.util.ArrayList;
import java.util.PriorityQueue;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import ds.qtrajtree.QuadTrajTree;
import java.util.Comparator;
import java.util.HashSet;
import query.service.ServiceQueryProcessor;
import result.ResultPlotter;

class serviceByFacilities {

    public int id;
    public double service;

    public serviceByFacilities(int facilityId, double facilityServiceValue) {
        id = facilityId;
        service = facilityServiceValue;
    }
}

class facilityServiceComparator implements Comparator<serviceByFacilities> {

    @Override
    public int compare(serviceByFacilities o1, serviceByFacilities o2) {
        if (o1.service > o2.service) {
            return -1;
        }
        if (o1.service < o2.service) {
            return +1;
        }
        return 0;
    }
}

public class BestKQueryProcessor {

    QuadTrajTree quadTrajTree;

    public BestKQueryProcessor(QuadTrajTree quadTrajTree) {
        this.quadTrajTree = quadTrajTree;
    }

    ArrayList<CandidateSolution> bestKFacilitiesBinaryService(ArrayList<ArrayList<CoordinateArraySequence>> routeGraphs, int k, ResultPlotter mapView, DiskIO diskIO) {

        double from, to;
        ArrayList<CandidateSolution> ret = new ArrayList<CandidateSolution>();
        //SolutionComparator comparator = new SolutionComparator();
        facilityServiceComparator comparator = new facilityServiceComparator();
        CandidateSolution[] candidateSolutions = new CandidateSolution[routeGraphs.size()];
        serviceByFacilities temp = null;
        PriorityQueue<serviceByFacilities> facilityService = new PriorityQueue<serviceByFacilities>(routeGraphs.size(), comparator);
        for (int i = 0; i < routeGraphs.size(); i++) {
            ArrayList<CoordinateArraySequence> routeGraph = routeGraphs.get(i);
            candidateSolutions[i] = new CandidateSolution(quadTrajTree, routeGraph, i);
            temp = new serviceByFacilities(candidateSolutions[i].id, candidateSolutions[i].fitness());
            facilityService.add(temp);
        }

        while (!facilityService.isEmpty() && ret.size() < k) {
            temp = facilityService.poll();
            CandidateSolution candidateSolution = candidateSolutions[temp.id];
            //System.out.println(candidateSolution.id + " " + candidateSolution.fitness());
            if (candidateSolution.finished) {
                //k--;
                ret.add(candidateSolution);
            } else {
                if (!candidateSolution.relaxStateBinary(mapView, diskIO)) {
                    //System.out.println("FOUND!!");
                    //k--;
                    //ret.add(candidateSolution);
                    //continue;
                    candidateSolution.finished = true;
                }
                temp.service = candidateSolution.fitness();
                facilityService.add(temp);
                //candidateSolutions.add(candidateSolution);
            }
            candidateSolutions[temp.id] = candidateSolution;
            //System.out.println(facilityService.size() + " " + ret.size() + " " + k);
        }
        return ret;
    }
    
    /* For Uniform Service Function */
    ArrayList<CandidateSolution> bestKFacilitiesUniformService(ArrayList<ArrayList<CoordinateArraySequence>> routeGraphs, int k, ResultPlotter mapView) {

        double from, to;
        ArrayList<CandidateSolution> ret = new ArrayList<CandidateSolution>();
        //SolutionComparator comparator = new SolutionComparator();
        facilityServiceComparator comparator = new facilityServiceComparator();
        CandidateSolution[] candidateSolutions = new CandidateSolution[routeGraphs.size()];
        serviceByFacilities temp = null;
        PriorityQueue<serviceByFacilities> facilityService = new PriorityQueue<serviceByFacilities>(routeGraphs.size(), comparator);
        for (int i = 0; i < routeGraphs.size(); i++) {
            ArrayList<CoordinateArraySequence> routeGraph = routeGraphs.get(i);
            candidateSolutions[i] = new CandidateSolution(quadTrajTree, routeGraph, i);
            temp = new serviceByFacilities(candidateSolutions[i].id, candidateSolutions[i].fitness());
            facilityService.add(temp);
        }

        while (!facilityService.isEmpty() && ret.size() < k) {
            temp = facilityService.poll();
            CandidateSolution candidateSolution = candidateSolutions[temp.id];
            //System.out.println(candidateSolution.id + " " + candidateSolution.fitness());
            if (candidateSolution.finished) {
                //k--;
                ret.add(candidateSolution);
            } else {
                if (!candidateSolution.relaxStateUniform(mapView)) {
                    //System.out.println("FOUND!!");
                    //k--;
                    //ret.add(candidateSolution);
                    //continue;
                    candidateSolution.finished = true;
                }
                temp.service = candidateSolution.fitness();
                facilityService.add(temp);
                //candidateSolutions.add(candidateSolution);
            }
            candidateSolutions[temp.id] = candidateSolution;
            //System.out.println(facilityService.size() + " " + ret.size() + " " + k);
        }
        return ret;
    }
    

    ArrayList<CandidateSolution> bestKFacilitiesBruteForce(ArrayList<ArrayList<CoordinateArraySequence>> routeGraphs, int k) {
        ArrayList<CandidateSolution> ret = new ArrayList<CandidateSolution>();
        //SolutionComparator comparator = new SolutionComparator();
        facilityServiceComparator comparator = new facilityServiceComparator();
        CandidateSolution[] candidateSolutions = new CandidateSolution[routeGraphs.size()];
        serviceByFacilities temp = null;

        PriorityQueue<serviceByFacilities> facilityService = new PriorityQueue<serviceByFacilities>(routeGraphs.size(), comparator);
        for (int i = 0; i < routeGraphs.size(); i++) {
            CandidateSolution candidateSolution = new CandidateSolution(quadTrajTree, routeGraphs.get(i), i);
            candidateSolution.hServe = 0;
            ServiceQueryProcessor serviceQueryProcessor = new ServiceQueryProcessor(quadTrajTree);
            HashSet<Integer> served = new HashSet<Integer>();
            candidateSolution.aServe = serviceQueryProcessor.evaluateService(quadTrajTree.getQuadTree().getRootNode(), routeGraphs.get(i), served);
            //candidateSolutions.add(candidateSolution);
            temp = new serviceByFacilities(candidateSolution.id, candidateSolution.fitness());
            facilityService.add(temp);
        }

        while (!facilityService.isEmpty() && ret.size() < k) {
            ret.add(candidateSolutions[facilityService.poll().id]);
        }
        return ret;
    }
}
