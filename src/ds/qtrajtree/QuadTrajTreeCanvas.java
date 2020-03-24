package ds.qtrajtree;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import ds.qtree.Node;


public class QuadTrajTreeCanvas extends JComponent{
	/**
	 * 
	 */
        private int []trajColorPallette, qNodeColorPallettee;
	private static final long serialVersionUID = 4703035325723368366L;
        private static final int windowSize = 1000;
        private static final int scaleNormalizer = 9;
        private static final int displacementOffset = 40;
	QuadTrajTree quadTrajTree;
	public QuadTrajTreeCanvas(QuadTrajTree quadTrajTree) {
		super();
		this.quadTrajTree = quadTrajTree;
                int trajCount = quadTrajTree.getTotalNodeTraj(quadTrajTree.getQuadTree().getRootNode());
                int nodeCount = quadTrajTree.getQuadTree().getNodeCount();
                System.out.println("Before drawing, number of trajs = " + trajCount + ", number of qNodes = " + nodeCount +
                        ", number of qNodes having trajs = " + quadTrajTree.nodeToIntraTrajsMap.size());
	}

	public void draw() {
		JFrame window = new JFrame();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setBounds(0, 0, windowSize, windowSize);
		window.getContentPane().add(this);
		window.setVisible(true);
        }
	
	public void paint(Graphics g) {
		
		for (Entry<Node, ArrayList<CoordinateArraySequence>> entry : quadTrajTree.nodeToIntraTrajsMap.entrySet())
		{
			Node node = entry.getKey();
			Color color = new Color((int)(Math.random() * 0x1000000));
			g.setColor(color);
			g.drawRect ((int)node.getX()*scaleNormalizer + displacementOffset, (int)node.getY()*scaleNormalizer + displacementOffset,
                                (int)node.getW()*scaleNormalizer, (int)node.getH()*scaleNormalizer);
			ArrayList<CoordinateArraySequence> trajectories = entry.getValue();
			for (CoordinateArraySequence trajectory : trajectories) {
                            // will need to modify the following line later for multipoint trajectories (iterating over variable size of the coordinates
                            g.drawLine((int)trajectory.getX(0)*scaleNormalizer + displacementOffset, (int)trajectory.getY(0)*scaleNormalizer + displacementOffset,
                                    (int)trajectory.getX(1)*scaleNormalizer + displacementOffset, (int)trajectory.getY(1)*scaleNormalizer + displacementOffset);
			}
			
		}
	}
}