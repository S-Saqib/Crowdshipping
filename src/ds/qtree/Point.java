package ds.qtree;

public class Point{

    private double x;
    private double y;
    private long timeInSec;
    private Object opt_value;
    private Object traj_id;

    /**
     * Creates a new point object.
     *
     * @param {double} x The x-coordinate of the point.
     * @param {double} y The y-coordinate of the point.
     * @param {Object} opt_value Optional value associated with the point.     
     */
    public Point(double x, double y, long timeInSec, Object opt_value, Object traj_id) {
        this.x = x;
        this.y = y;
        this.timeInSec = timeInSec;
        this.opt_value = opt_value;
        this.traj_id = traj_id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Object getValue() {
        return opt_value;
    }

    public void setValue(Object opt_value) {
        this.opt_value = opt_value;
    }
    
    public long getTimeInSec() {
        return timeInSec;
    }

    public void setTimeInSec(long timeInSec) {
        this.timeInSec = timeInSec;
    }

    public Object getTraj_id() {
        return traj_id;
    }

    public void setTraj_id(Object traj_id) {
        this.traj_id = traj_id;
    }

    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + "), " + this.timeInSec + ", " + this.traj_id + "-" + this.opt_value;
    }

    public int compareTo(Object o) {
        Point tmp = (Point) o;
        if (this.x < tmp.x) {
            return -1;
        } else if (this.x > tmp.x) {
            return 1;
        } else {
            if (this.y < tmp.y) {
                return -1;
            } else if (this.y > tmp.y) {
                return 1;
            }
            return 0;
        }

    }

}
