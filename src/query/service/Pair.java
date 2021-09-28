/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package query.service;

/**
 *
 * @author Saqib
 */
class Pair implements Comparable<Pair>{
    private Integer key;
    private Long value;

    public Pair(Integer key, Long value) {
        this.key = key;
        this.value = value;
    }

    public Integer getKey() {
        return key;
    }

    public Long getValue() {
        return value;
    }
    
    @Override
    public int compareTo(Pair o) {
        return o.key - key;
    }
}
