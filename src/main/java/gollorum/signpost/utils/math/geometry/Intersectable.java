package gollorum.signpost.utils.math.geometry;

import java.util.Optional;

public interface Intersectable<Other, IntersectionInfo> {

    Optional<IntersectionInfo> IntersectWith(Other other);

    AABB getBounds();

}
