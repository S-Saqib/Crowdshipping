/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package result;

import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JFrame;
/**
 *
 * @author Saqib
 */
public class ResultPlotter extends JComponent {
    
    private HashSet <CoordinateArraySequence> servedUserTrajectories;
    private ArrayList <ArrayList<CoordinateArraySequence>> facility;
    private int windowWidth, windowHeight;
    private int pointRadius;
    private int scalingFactor;
    
    public ResultPlotter (){
        servedUserTrajectories = new HashSet<CoordinateArraySequence>();
        facility = new ArrayList<ArrayList<CoordinateArraySequence>>();
        windowWidth = 510;
        windowHeight = 510;
        pointRadius = 2;
        scalingFactor = 5;
    }
    
    public void addUserTrajectory (CoordinateArraySequence newTraj){
        servedUserTrajectories.add(newTraj);
    }
    
    public void addFacility (ArrayList<CoordinateArraySequence> newFacility){
        facility.add(newFacility);
    }
    
    public void draw() {
        JFrame window = new JFrame();
	window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	window.setBounds(0, 0, windowWidth, windowHeight);
	window.getContentPane().add(this);
	window.setVisible(true);
    }
    
    public void paint(Graphics g) {
        Color color = new Color(0x000000);
        g.setColor(color);
        for (CoordinateArraySequence userTrajectory: servedUserTrajectories){
            g.fillOval((int)userTrajectory.getX(0)*scalingFactor, (int)userTrajectory.getY(0)*scalingFactor, pointRadius, pointRadius);
            g.fillOval((int)userTrajectory.getX(1)*scalingFactor, (int)userTrajectory.getY(1)*scalingFactor, pointRadius, pointRadius);
            g.drawLine((int)userTrajectory.getX(0)*5, (int)userTrajectory.getY(0)*5, (int)userTrajectory.getX(1)*5, (int)userTrajectory.getY(1)*5);
        }
        
        for (int i=0; i<facility.size(); i++){
            color = new Color(0x000000 + ((0x0000FF)<<(8*i)));
            System.out.println(i + ": " + color);
            g.setColor(color);
            ArrayList<CoordinateArraySequence> singleFacilityGraph = facility.get(i);
            for (CoordinateArraySequence trajectory : singleFacilityGraph){
                g.fillOval((int)trajectory.getX(0)*scalingFactor, (int)trajectory.getY(0)*scalingFactor, pointRadius, pointRadius);
                for (int j=1; j<trajectory.size(); j++){
                    g.fillOval((int)trajectory.getX(j)*scalingFactor, (int)trajectory.getY(j)*scalingFactor, pointRadius, pointRadius);
                    g.drawLine((int)trajectory.getX(j-1)*5, (int)trajectory.getY(j-1)*5, (int)trajectory.getX(j)*5, (int)trajectory.getY(j)*5);
                }
            }
        }
    }
}
