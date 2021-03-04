package ds.qtrajtree;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Map.Entry;
import javax.swing.JComponent;
import javax.swing.JFrame;
import ds.qtree.Node;
import ds.trajectory.TrajPoint;
import ds.trajectory.Trajectory;

public class IndexCanvas extends JComponent {

    private final int[] trajColorPallette;
    private final int[] qNodeColorPallette;
    private final int windowSize;
    private final int scaleNormalizer;
    private final int displacementOffset;
    TQIndex quadTrajTree;

    public IndexCanvas(TQIndex quadTrajTree) {
        super();
        this.quadTrajTree = quadTrajTree;
        
        // fix window size and appropriate scaling, translation values
        windowSize = 1000;
        scaleNormalizer = 9;
        displacementOffset = 40;
        
        // fix color pallettes based on number of nodes containing trajectories
        int nodesHavingTrajectories = quadTrajTree.qNodeToAnonymizedTrajIdsMap.size();
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

    @Override
    public void paint(Graphics g) {

        // iterate over all entries of inter-node trajectory map and draw node boundary and tagged trajectories
        int count = 0;
        
        for (Entry<Node, ArrayList<String>> entry : quadTrajTree.qNodeToAnonymizedTrajIdsMap.entrySet()) {
            Node node = entry.getKey();
            Color qNodeColor = new Color(qNodeColorPallette[count]);
            g.setColor(qNodeColor);
            g.drawRect((int) node.getX() * scaleNormalizer + displacementOffset, (int) node.getY() * scaleNormalizer + displacementOffset,
                    (int) node.getW() * scaleNormalizer, (int) node.getH() * scaleNormalizer);
            
            Color trajColor = new Color(trajColorPallette[count]);
            g.setColor(trajColor);
            
            // iterate over all trajectories and draw a line between each pair of subsequent points
            ArrayList<Trajectory> trajectories = quadTrajTree.getQNodeTrajs(node);
            for (Trajectory trajectory : trajectories) {
                ArrayList <TrajPoint> trajPointList = new ArrayList<TrajPoint>(trajectory.getPointList());
                for (int i=1; i<trajPointList.size(); i++){
                    g.drawLine((int) trajPointList.get(i-1).getPointLocation().x * scaleNormalizer + displacementOffset,
                        (int) trajPointList.get(i-1).getPointLocation().y * scaleNormalizer + displacementOffset,
                        (int) trajPointList.get(i).getPointLocation().x * scaleNormalizer + displacementOffset,
                        (int) trajPointList.get(i).getPointLocation().y * scaleNormalizer + displacementOffset);
                }
            }
            count++;
        }
    }
}
