package gollorum.signpost.minecraft.utils;

public class CoordinatesUtil {

	public static final float VoxelSize = 1f / 16f;
	public static final float FontToVoxelSize = VoxelSize / 8f;

	public static float voxelToLocal(float voxelPos) {
		return voxelPos * VoxelSize + 0.5f;
	}

}
