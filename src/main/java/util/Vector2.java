package util;

public class Vector2 {
    public double x, y;
    public Vector2(double x, double y){
        this.x = x;
        this.y = y;
    }

    public static double getDistance(Vector2 pos1, Vector2 pos2){
        return Math.sqrt(Math.pow((pos2.x - pos1.x), 2) + Math.pow((pos2.y - pos1.y), 2));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
