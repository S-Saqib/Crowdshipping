/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.rtree;

import com.github.davidmoten.rtreemulti.RTree;
import com.github.davidmoten.rtreemulti.Visitor;
import com.github.davidmoten.rtreemulti.Leaf;
import com.github.davidmoten.rtreemulti.NonLeaf;
import com.github.davidmoten.rtreemulti.geometry.Geometry;

import ds.transformed_trajectory.TransformedTrajectory;
import java.util.HashMap;

/**
 *
 * @author ashik
 */
public class Rtree {
    private HashMap<String, TransformedTrajectory> transformedTrajectories;
    private RTree<String, Geometry> tree;

    public Rtree(HashMap<String, TransformedTrajectory> transformedTrajectories) {
        this.transformedTrajectories = transformedTrajectories;
        this.tree = RTree.star().dimensions(2).create();
        
        transformedTrajectories.entrySet().forEach((entry) -> {
            TransformedTrajectory traj = entry.getValue();
            traj.setEnvelope();
            this.tree = this.tree.add(entry.getKey(), traj.getEnvelope());
        });
    }
    
    public HashMap<String, Integer> getTrajectoryToLeafMapping(){
        HashMap<String, Integer> trajectoryToLeafMapping = new HashMap<>();
        Integer[] leafCount = new Integer[]{0};
        this.tree.visit(new Visitor<String, Geometry>() {

            @Override
            public void leaf(Leaf<String, Geometry> node) {
                node.entries().forEach((entry) -> {
                    trajectoryToLeafMapping.put(entry.value(), leafCount[0]);
                });
                leafCount[0]++;
            }

            @Override
            public void nonLeaf(NonLeaf<String, Geometry> node) {
                //System.out.println(node);
            }
        });
        return trajectoryToLeafMapping;
    }
    
    
}
