package gollorum.signpost.utils.math.geometry;

import com.sun.javafx.geom.Matrix3f;

public class Ray {

    public final Vector3 start;
    public final Vector3 dir;

    public Ray(Vector3 start, Vector3 dir) {
        this.start = start;
        this.dir = dir;
    }

    public Ray mul(Matrix3f mat){
        return new Ray(
            start.mul(mat),
            dir.mul(mat)
        );
    }

    public Ray offset(Vector3 vec){
        return new Ray(start.add(vec), dir);
    }
}
