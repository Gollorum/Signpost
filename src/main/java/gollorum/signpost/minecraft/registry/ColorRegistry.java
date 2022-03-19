package gollorum.signpost.minecraft.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Signpost.MOD_ID, bus = MOD)
public class ColorRegistry {

    @SubscribeEvent
    static void onBlockColor(ColorHandlerEvent.Block event) {
        for(PostBlock.Variant variant : PostBlock.AllVariants) {
            event.getBlockColors().register(overlayBlockColor, variant.getBlock());
        }
    }

    @SubscribeEvent
    static void onItemColor(ColorHandlerEvent.Item event) {
        for(PostBlock.Variant variant : PostBlock.AllVariants) {
            event.getItemColors().register(overlayItemColor, variant.getBlock());
            event.getBlockColors().register(overlayBlockColor, variant.getBlock());
        }
    }

    private static final BlockColor overlayBlockColor =
        (blockState, world, blockPos, tintIndex) -> getOverlayColor(tintIndex, world, blockPos);

    private static final ItemColor overlayItemColor = (ItemStack itemStack, int tintIndex) -> {
        Level world = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;
        if(world == null || player == null) return -1;
        return getOverlayColor(tintIndex, world, player.blockPosition());
    };

    public static int getOverlayColor(int tintIndex, BlockAndTintGetter world, BlockPos pos) {
        if(world == null || pos == null) return -1;
        return switch (tintIndex) {
            case Overlay.GrasTint -> BiomeColors.getAverageGrassColor(world, pos);
            case Overlay.FoliageTint -> BiomeColors.getAverageFoliageColor(world, pos);
            case Overlay.WaterTint -> BiomeColors.getAverageWaterColor(world, pos);
            default -> -1;
        };
    }

}
