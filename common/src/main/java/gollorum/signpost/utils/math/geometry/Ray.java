package gollorum.signpost.utils.math.geometry;

public class Ray {

    public final Vector3 start;
    public final Vector3 dir;

    public Ray(Vector3 start, Vector3 dir) {
        this.start = start;
        this.dir = dir;
    }

    public Ray offset(Vector3 vec){
        return new Ray(start.add(vec), dir);
    }

    public Vector3 atDistance(float t) {
        return start.add(dir.normalized().mul(t));
    }
}
