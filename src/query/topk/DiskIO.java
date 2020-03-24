/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query.topk;

/**
 *
 * @author Saqib
 */
public class DiskIO{
    Integer orderedBlockAccess, unorderedBlockAccess, tqTreeBlockAccess, naiveBlockAccess;
    public DiskIO(){
        orderedBlockAccess = new Integer(0);
        unorderedBlockAccess = new Integer(0);
        tqTreeBlockAccess = new Integer(0);
        naiveBlockAccess = new Integer(0);
    }
    public void setValues(int orderedCount, int unorderedCount, int tqTreeCount, int naiveCount){
        orderedBlockAccess = orderedCount;
        unorderedBlockAccess = unorderedCount;
        tqTreeBlockAccess = tqTreeCount;
        naiveBlockAccess = Integer.max(naiveCount, naiveBlockAccess);
    }
    public Integer getOrderedCount(){
        return orderedBlockAccess;
    }
    public Integer getUnorderedCount(){
        return unorderedBlockAccess;
    }
    public Integer getTqTreeCount(){
        return tqTreeBlockAccess;
    }
    public Integer getNaiveCount(){
        return naiveBlockAccess;
    }
}

