package gollorum.signpost.utils.math.geometry;

import gollorum.signpost.utils.Tuple;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class AABB implements Intersectable<Ray, Float> {
    
    public final Vector3 min;
    public final Vector3 max;

    public AABB(Vector3... vectors) {
        this.min = Arrays.stream(vectors).reduce(Vector3::min).get();
        this.max = Arrays.stream(vectors).reduce(Vector3::max).get();
    }

    public AABB(Stream<Vector3> vectors) {
        var tuple = vectors.map(v -> Tuple.of(v, v)).reduce((l, r) -> Tuple.of(Vector3.min(l._1, r._1), Vector3.max(l._2, r._2))).get();
        this.min = tuple._1;
        this.max = tuple._2;
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

    public net.minecraft.world.phys.AABB asMinecraftBB(){
        return new net.minecraft.world.phys.AABB(min.asVec3(), max.asVec3());
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
