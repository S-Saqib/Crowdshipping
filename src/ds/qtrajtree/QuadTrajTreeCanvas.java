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
import java.util.Arrays;

public class QuadTrajTreeCanvas extends JComponent {

    /**
     *
     */
    private int[] trajColorPallette, qNodeColorPallette;
    private static final long serialVersionUID = 4703035325723368366L;
    private static final int windowSize = 1000;
    private static final int scaleNormalizer = 9;
    private static final int displacementOffset = 40;
    TQIndex quadTrajTree;

    public QuadTrajTreeCanvas(TQIndex quadTrajTree) {
        super();
        this.quadTrajTree = quadTrajTree;

        int nodesHavingTrajectories = quadTrajTree.qNodeToTrajsMap.size();
        int colorStepSize = 0xDDDDDD / nodesHavingTrajectories;

        qNodeColorPallette = new int[nodesHavingTrajectories];
        trajColorPallette = new int[nodesHavingTrajectories];
        
        for (int i = 0; i < nodesHavingTrajectories; i++) {
            qNodeColorPallette[i] = 0x000000 + (colorStepSize / 2) * i;
            trajColorPallette[i] = 0xDDDDDD / 2 + qNodeColorPallette[i];
        }

    }

    public void draw() {
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(0, 0, windowSize, windowSize);
        window.getContentPane().add(this);
        window.setVisible(true);
    }

    public void paint(Graphics g) {

        int count = 0;
        for (Entry<Node, ArrayList<CoordinateArraySequence>> entry : quadTrajTree.qNodeToTrajsMap.entrySet()) {
            Node node = entry.getKey();
            //Color color = new Color((int)(Math.random() * 0x1000000));
            Color qNodeColor = new Color(qNodeColorPallette[count]);
            g.setColor(qNodeColor);
            g.drawRect((int) node.getX() * scaleNormalizer + displacementOffset, (int) node.getY() * scaleNormalizer + displacementOffset,
                    (int) node.getW() * scaleNormalizer, (int) node.getH() * scaleNormalizer);
            ArrayList<CoordinateArraySequence> trajectories = entry.getValue();
            
            Color trajColor = new Color(trajColorPallette[count]);
            g.setColor(trajColor);
            
            for (CoordinateArraySequence trajectory : trajectories) {
                // will need to modify the following line later for multipoint trajectories (iterating over variable size of the coordinates
                g.drawLine((int) trajectory.getX(0) * scaleNormalizer + displacementOffset, (int) trajectory.getY(0) * scaleNormalizer + displacementOffset,
                        (int) trajectory.getX(1) * scaleNormalizer + displacementOffset, (int) trajectory.getY(1) * scaleNormalizer + displacementOffset);
            }
            
            count++;
        }
    }
}
