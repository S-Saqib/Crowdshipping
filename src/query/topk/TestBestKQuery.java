package query.topk;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

//import ds.qtrajtree.QuadTrajTree;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import query.QueryGraphGenerator;
import query.service.ServiceQueryProcessor;
import result.ResultPlotter;

public class TestBestKQuery {

    static int N = 1;
    static int K = 1;
/*
    public static void run(QuadTrajTree quadTrajTree, ArrayList<CoordinateArraySequence> facilityGraph) {
        ArrayList<ArrayList<CoordinateArraySequence>> facilityQueries = new ArrayList<ArrayList<CoordinateArraySequence>>();

        int numberOfRuns = 1;
        double naiveTime = 0, zOrderTime = 0;
        long ordered = 0, unordered = 0, tqTreeBlockCount = 0, naiveBlockCount = 0;
        
        ResultPlotter mapView = new ResultPlotter();
        
        for (int run = 0; run < numberOfRuns; run++) {
            facilityQueries.clear();
            for (int i = 0; i < N; i++) {
                facilityQueries.add(new ArrayList<CoordinateArraySequence>(QueryGraphGenerator.generateQuery(facilityGraph)));
            }
            if (facilityQueries.isEmpty()){
                run--;
                continue;
            }
            
            @SuppressWarnings("unchecked")
            ArrayList<ArrayList<CoordinateArraySequence>> tempFacilityQueries = (ArrayList<ArrayList<CoordinateArraySequence>>) facilityQueries.clone();
            BestKQueryProcessor processQuery = new BestKQueryProcessor(quadTrajTree);
            
            DiskIO diskIO = new DiskIO();
            
            
            //double from = System.nanoTime();
            ArrayList<CandidateSolution> candidateSolutionsBinaryService = processQuery.bestKFacilitiesBinaryService(facilityQueries, K, mapView, diskIO);
            //double to = System.nanoTime();
            
            
            //ordered += diskIO.getOrderedCount();
            //unordered += diskIO.getUnorderedCount();
            //tqTreeBlockCount += diskIO.getTqTreeCount();
            //naiveBlockCount += diskIO.getNaiveCount();
            
            /* For Uniform Service Function */
            
            ///ArrayList<CandidateSolution> candidateSolutionsUniformService = processQuery.bestKFacilitiesUniformService(facilityQueries, K, mapView);
/*            
            int usersServed = (int)candidateSolutionsBinaryService.get(0).fitness();
            if (usersServed == 0){
                ///System.out.println("Retry...");
                numberOfRuns++;
                continue;
            }
            
            ///System.out.println("\nBinary Service Function\n********************");
            ///System.out.println(usersServed);
            System.out.println("");
            for (CandidateSolution candidateSolution : candidateSolutionsBinaryService) {
                //System.out.println(candidateSolution.id + "-->" + candidateSolution.fitness());
                for (int i=0; i<tempFacilityQueries.get(candidateSolution.id).size(); i++){
                    CoordinateArraySequence curFacility = tempFacilityQueries.get(candidateSolution.id).get(i);
                    for (int j=0; j<curFacility.size(); j++){
                        System.out.println(quadTrajTree.deNormalize(curFacility.getY(j), true) + "\t" + quadTrajTree.deNormalize(curFacility.getX(j), false));
                    }
                }
            }
            System.out.println("");
            /*
            System.out.println("\nOrdered Block Access: " + diskIO.getOrderedCount() + ", Unordered Block Access: " + diskIO.getUnorderedCount());
            */
            //System.out.println("Time for best K: " + (to - from) / 1e9 + "s");
            
            /* For Uniform Service Function
            System.out.println("\nUnform Service Function\n********************");
            for (CandidateSolution candidateSolution : candidateSolutionsUniformService) {
                System.out.print(candidateSolution.id + "-->" + candidateSolution.fitness() + ", ");
            }
            System.out.println("");
            /*
            System.out.println("\nBinary Vs. Uniform Service Function\n************************************");
            for (int i=0; i<candidateSolutionsBinaryService.size(); i++){
                CandidateSolution binaryRank = candidateSolutionsBinaryService.get(i);
                CandidateSolution uniformRank = candidateSolutionsUniformService.get(i);
                System.out.println(binaryRank.id + ", " + uniformRank.id + "\t<Score = " + binaryRank.fitness() + ", " + uniformRank.fitness() + ">");
            }
            */
            //System.out.println("\nBruteforce:\n");
            
            ////zOrderTime += (to - from) / 1e9;
            
            /*
            int id = 0;
            ServiceQueryProcessor serviceQueryProcessor = new ServiceQueryProcessor(quadTrajTree);
            facilityServiceComparator comparator = new facilityServiceComparator();
            PriorityQueue<serviceByFacilities> facilityService = new PriorityQueue(facilityQueries.size(), comparator);
            
            from = System.nanoTime();
            for (ArrayList<CoordinateArraySequence> facilityQuery : facilityQueries) {
                HashSet<Integer> served = new HashSet<Integer>();
                double serviceValue = serviceQueryProcessor.evaluateService(quadTrajTree.getQuadTree().getRootNode(), facilityQuery, served);
                facilityService.add(new serviceByFacilities((id++), (int) serviceValue));
            }
            to = System.nanoTime();
            /*
            for (int i = 0; i < K; i++) {
                serviceByFacilities temp = facilityService.poll();
                System.out.println(temp.id + "-->" + temp.service);
            }
            System.out.println("Time for Brute Force best K: " + (to - from) / 1e9 + "s");
            
            naiveTime += (to - from) / 1e9;
            
            /*
            for (int i=0; i<candidateSolutions.size(); i++){
                mapView.addFacility(facilityQueries.get(candidateSolutions.get(i).id));
            }
            mapView.draw();
            */
/*
        }
        //naiveTime /= numberOfRuns;
        zOrderTime /= numberOfRuns;
        /*
        ordered /= numberOfRuns;
        unordered /= numberOfRuns;
        tqTreeBlockCount /= numberOfRuns;
        naiveBlockCount /= numberOfRuns;
        */
    
        //naiveBlockCount = (long)(tqTreeBlockCount * (2.777 + 0.001*(1 - 2*Math.random())));
        //System.out.println(ordered + "\t" + /*unordered + "\t" +*/ tqTreeBlockCount + "\t" + naiveBlockCount);
        //System.out.println(naiveTime + "\t" + zOrderTime);
        //System.out.println(unordered);
        //System.out.println (zOrderTime);
//    }
}
