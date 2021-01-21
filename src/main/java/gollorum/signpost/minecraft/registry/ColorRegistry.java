package gollorum.signpost.minecraft.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.blockpartdata.Overlay;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Signpost.MOD_ID, bus = MOD)
public class ColorRegistry {

    @SubscribeEvent
    static void onBlockColor(ColorHandlerEvent.Block event) {
        for(Post.Variant variant : Post.AllVariants) {
            event.getBlockColors().register(ColorRegistry::getOverlayBlockColor, variant.block);
        }
    }

    @SubscribeEvent
    static void onItemColor(ColorHandlerEvent.Item event) {
        for(Post.Variant variant : Post.AllVariants) {
            event.getItemColors().register(ColorRegistry::getOverlayItemColor, variant.block);
            event.getBlockColors().register(ColorRegistry::getOverlayBlockColor, variant.block);
        }
    }

    private static int getOverlayBlockColor(BlockState blockState, @Nullable IBlockDisplayReader world, @Nullable BlockPos blockPos, int tintIndex) {
        if(world == null || blockPos == null) return -1;
        switch(tintIndex) {
            case Overlay.GrasTint: return BiomeColors.getGrassColor(world, blockPos);
            case Overlay.FoliageTint: return BiomeColors.getFoliageColor(world, blockPos);
            case Overlay.WaterTint: return BiomeColors.getWaterColor(world, blockPos);
            default: return -1;
        }
    }

    private static int getOverlayItemColor(ItemStack itemStack, int tintIndex) {
        World world = Minecraft.getInstance().world;
        PlayerEntity player = Minecraft.getInstance().player;
        if(world == null || player == null) return -1;
        switch(tintIndex) {
            case Overlay.GrasTint: return BiomeColors.getGrassColor(world, player.getPosition());
            case Overlay.FoliageTint: return BiomeColors.getFoliageColor(world, player.getPosition());
            case Overlay.WaterTint: return BiomeColors.getWaterColor(world, player.getPosition());
            default: return -1;
        }
    }

}
