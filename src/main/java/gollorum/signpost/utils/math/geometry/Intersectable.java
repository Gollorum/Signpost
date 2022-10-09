package gollorum.signpost.utils.math.geometry;

import java.util.Optional;

public interface Intersectable<Other, IntersectionInfo> {

    Optional<IntersectionInfo> IntersectWith(Other other);

    AABB getBounds();

    public static class Not<Other, IntersectionInfo> implements Intersectable<Other, IntersectionInfo> {

        @Override
        public Optional<IntersectionInfo> IntersectWith(Other other) {
            return Optional.empty();
        }

        @Override
        public AABB getBounds() {
            return new AABB(new Vector3(0, 0, 0), new Vector3(0, 0, 0));
        }
    }

}
