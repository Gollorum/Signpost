package gollorum.signpost.interactions;

import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;

import java.util.Optional;

public interface Interactable {

    public static enum InteractionResult {
        Accepted, Ignored
    }

    Intersectable<Ray, Float> getIntersection();

    default Optional<Float> intersectWith(Ray ray, Vector3 offset) {
        return getIntersection().IntersectWith(ray.offset(offset.mul(-1f)));
    }

    InteractionResult interact(InteractionInfo info);

}
