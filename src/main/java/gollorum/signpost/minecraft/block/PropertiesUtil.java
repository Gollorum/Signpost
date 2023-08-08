package gollorum.signpost.minecraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class PropertiesUtil {

    public enum WoodType{
        Oak, DarkOak, Spruce, Birch, Jungle, Acacia, Mangrove, Bamboo, Cherry, Warped, Crimson
    }

    public static Block.Properties STONE = Block.Properties.of()
        .mapColor(MapColor.STONE)
        .instrument(NoteBlockInstrument.BASEDRUM)
        .strength(1.5F, 6.0F)
        .requiresCorrectToolForDrops();

    public static Block.Properties IRON = Block.Properties.of()
        .mapColor(MapColor.METAL)
        .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
        .requiresCorrectToolForDrops()
        .strength(5.0F, 6.0F)
        .sound(SoundType.METAL);

    private static Block.Properties wood(MapColor color){
        return Block.Properties.of()
            .mapColor(color)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD);
    }

    public static Block.Properties mushroom(MapColor color){
        return Block.Properties.of()
            .mapColor(color)
            .instrument(NoteBlockInstrument.BASS)
            .strength(0.2F)
            .sound(SoundType.WOOD);
    }

    private static MapColor colorFor(WoodType type){
        return switch (type) {
            case Oak -> MapColor.WOOD;
            case DarkOak -> MapColor.COLOR_BROWN;
            case Spruce -> MapColor.PODZOL;
            case Birch -> MapColor.SAND;
            case Jungle -> MapColor.DIRT;
            case Acacia -> MapColor.COLOR_ORANGE;
            case Mangrove -> MapColor.COLOR_RED;
            case Bamboo -> MapColor.COLOR_YELLOW;
            case Cherry -> MapColor.TERRACOTTA_WHITE;
            case Warped -> MapColor.WARPED_STEM;
            case Crimson -> MapColor.CRIMSON_STEM;
        };
    }

    public static Block.Properties wood(WoodType type){
        return wood(colorFor(type));
    }
}
