package gollorum.signpost.minecraft.rendering;

import com.mojang.math.Transformation;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.resources.model.ModelState;
import org.joml.Matrix4f;

import java.util.Objects;

public final class RotatedModelState implements ModelState {

    private final Angle angle;
    private final Matrix4f matrix;

    public RotatedModelState(Angle angle) {
        this.angle = angle;
        matrix = new Matrix4f().rotationY(angle.radians());
    }

    // 1.21.1's ModelState exposes only getRotation(); there are no per-face transformations yet.
    @Override
    public Transformation getRotation() {
        return new Transformation(matrix);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RotatedModelState that)) return false;
        return Objects.equals(this.angle, that.angle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(angle);
    }

    @Override
    public String toString() {
        return "RotatedModelState[" +
            "angle=" + angle + ']';
    }

}
