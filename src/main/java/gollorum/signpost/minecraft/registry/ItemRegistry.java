package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.Waystone;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

import static gollorum.signpost.Signpost.MOD_ID;

public class ItemRegistry {

    public static final ItemGroup ITEM_GROUP = ItemGroup.TRANSPORTATION;

    private static final DeferredRegister<Item> REGISTER = new DeferredRegister<>(ForgeRegistries.ITEMS, MOD_ID);

    private static final RegistryObject<Item> WAYSTONE_ITEM =
        REGISTER.register(Waystone.REGISTRY_NAME,
            () -> new BlockItem(Waystone.INSTANCE, new Item.Properties().group(ITEM_GROUP)));

    private static final Object[] POSTS_ITEMS =
        Arrays.stream(Post.All_INFOS)
            .map(ItemRegistry::registerPostItem)
            .toArray();

    private static RegistryObject<Item> registerPostItem(Post.Info postInfo){
        return REGISTER.register(postInfo.registryName,
            () -> new BlockItem(postInfo.post, new Item.Properties().group(ITEM_GROUP)));
    }

    public static void register(IEventBus bus){
        REGISTER.register(bus);
    }
}
