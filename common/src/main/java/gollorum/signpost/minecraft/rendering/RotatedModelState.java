package gollorum.signpost.minecraft.rendering;

import com.mojang.math.Transformation;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.Objects;

public final class RotatedModelState implements ModelState {

    private final Angle angle;
    private final Matrix4f matrix;
    private Matrix4f _inverseMatrix = null;

    public RotatedModelState(Angle angle) {
        this.angle = angle;
        matrix = new Matrix4f().rotationY(angle.radians());
    }

    @Override
    public Transformation transformation() {
        return new Transformation(matrix);
    }

    @Override
    public Matrix4fc faceTransformation(Direction p_405422_) {
        return matrix;
    }

    @Override
    public Matrix4fc inverseFaceTransformation(Direction p_405142_) {
        if (_inverseMatrix == null) {
            _inverseMatrix = new Matrix4f();
            matrix.invert(_inverseMatrix);
        }
        return _inverseMatrix;
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
