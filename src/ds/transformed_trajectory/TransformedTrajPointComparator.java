/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.transformed_trajectory;

import java.util.Comparator;

/**
 *
 * @author Saqib
 */
public class TransformedTrajPointComparator implements Comparator<Object>{

    @Override
    public int compare(Object o1, Object o2) {
        TransformedTrajPoint t1 = (TransformedTrajPoint)o1;
        TransformedTrajPoint t2 = (TransformedTrajPoint)o2;
        
        if (t1.getTimeIndex()< t2.getTimeIndex()) return -1;
        if (t1.getTimeIndex() > t2.getTimeIndex()) return 1;
        if (t1.getTimeInSec()< t2.getTimeInSec()) return -1;
        if (t1.getTimeInSec() > t2.getTimeInSec()) return 1;
        if (t1.getqNodeIndex() < t2.getqNodeIndex()) return -1;
        if (t1.getqNodeIndex() > t2.getqNodeIndex()) return 1;
        return 0;
    }
    
}
