package gollorum.signpost.minecraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class PropertiesUtil {

    public enum WoodType{
        Oak, DarkOak, Spruce, Birch, Jungle, Acacia, Warped, Crimson
    }

    public static Block.Properties STONE = Block.Properties.of(Material.STONE, MaterialColor.STONE)
        .strength(1.5F, 6.0F).requiresCorrectToolForDrops();

    public static Block.Properties IRON = Block.Properties.of(Material.METAL, MaterialColor.METAL)
        .strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    private static Block.Properties wood(MaterialColor color){
        return Block.Properties.of(Material.WOOD, color)
            .strength(2.0F, 3.0F).sound(SoundType.WOOD);
    }

    private static MaterialColor colorFor(WoodType type){
        return switch (type) {
            case Oak -> MaterialColor.WOOD;
            case DarkOak -> MaterialColor.COLOR_BROWN;
            case Spruce -> MaterialColor.PODZOL;
            case Birch -> MaterialColor.SAND;
            case Jungle -> MaterialColor.DIRT;
            case Acacia -> MaterialColor.COLOR_ORANGE;
            case Warped -> MaterialColor.WARPED_STEM;
            case Crimson -> MaterialColor.CRIMSON_STEM;
        };
    }

    public static Block.Properties wood(WoodType type){
        return wood(colorFor(type));
    }
}
