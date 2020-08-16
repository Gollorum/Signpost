package gollorum.signpost.utils.math.geometry;

import net.minecraft.util.math.AxisAlignedBB;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class AABB implements Intersectable<Ray, Float> {
    
    public final Vector3 min;
    public final Vector3 max;

    public AABB(Vector3... vectors) {
        this.min = Arrays.stream(vectors).reduce(Vector3::min).get();
        this.max = Arrays.stream(vectors).reduce(Vector3::max).get();
    }

    @Override
    public Optional<Float> IntersectWith(Ray ray) {
        Vector3 dirFrac = ray.dir.map(f -> 1 / f);
        Vector3 t1 = min.map(ray.start, dirFrac, a -> b -> c -> (a - b) * c);
        Vector3 t2 = max.map(ray.start, dirFrac, a -> b -> c -> (a - b) * c);
        float tmin = Vector3.min(t1, t2).max();
        float tmax = Vector3.max(t1, t2).min();

        return tmax >= 0 && tmin <= tmax ? Optional.of(tmin) : Optional.empty();
    }

    @Override
    public AABB getBounds() { return this; }

    public AABB map(Function<Float, Float> f) {
        return new AABB(min.map(f), max.map(f));
    }

    public AABB apply(Function<Vector3, Vector3> f) {
        return new AABB(Arrays.stream(allCorners()).map(f).toArray(Vector3[]::new));
    }

    public AABB offset(Vector3 v){ return new AABB(v.add(min), v.add(max)); }

    public AxisAlignedBB asMinecraftBB(){
        return new AxisAlignedBB(min.asVec3d(), max.asVec3d());
    }

    public Vector3[] allCorners(){
        Vector3[] ret = new Vector3[8];
        ret[0] = min;
        ret[1] = new Vector3(max.x, min.y, min.z);
        ret[2] = new Vector3(min.x, max.y, min.z);
        ret[3] = new Vector3(min.x, min.y, max.z);
        ret[4] = new Vector3(max.x, max.y, min.z);
        ret[5] = new Vector3(max.x, min.y, max.z);
        ret[6] = new Vector3(min.x, max.y, max.z);
        ret[7] = new Vector3(max.x, max.y, max.z);
        return ret;
    }
}
