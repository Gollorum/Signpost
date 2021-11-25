package gollorum.signpost.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraftforge.common.ToolType;

public class PropertiesUtil {

    public enum WoodType{
        Oak, DarkOak, Spruce, Birch, Jungle, Acacia, Warped, Crimson
    }

    public static Block.Properties STONE = Block.Properties.of(Material.STONE, MaterialColor.STONE)
        .strength(1.5F, 6.0F)
        .sound(SoundType.STONE)
        .harvestTool(ToolType.PICKAXE)
//        .requiresCorrectToolForDrops()
        ;

    public static Block.Properties IRON = Block.Properties.of(Material.METAL, MaterialColor.METAL)
        .strength(5.0F, 6.0F)
        .sound(SoundType.METAL)
        .harvestTool(ToolType.PICKAXE)
//        .requiresCorrectToolForDrops()
        ;

    private static Block.Properties wood(MaterialColor color){
        return Block.Properties.of(Material.WOOD, color)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .harvestTool(ToolType.AXE);
    }

    private static MaterialColor colorFor(WoodType type){
        switch (type) {
            case Oak: return MaterialColor.WOOD;
            case DarkOak: return MaterialColor.COLOR_BROWN;
            case Spruce: return MaterialColor.PODZOL;
            case Birch: return MaterialColor.SAND;
            case Jungle: return MaterialColor.DIRT;
            case Acacia: return MaterialColor.COLOR_ORANGE;
            default: throw new RuntimeException("Wood type " + type + "is not supported.");
        }
    }

    public static Block.Properties wood(WoodType type){
        return wood(colorFor(type));
    }
}
