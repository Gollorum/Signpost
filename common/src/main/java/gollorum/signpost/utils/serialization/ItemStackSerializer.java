package gollorum.signpost.utils.serialization;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class ItemStackSerializer {

    public static final CompoundSerializable<ItemStack> Compound = new Compound();
    public static final BufferSerializable<ItemStack> Buffer = new Buffer();

    private static final class Compound implements CompoundSerializable<ItemStack> {

        private Compound() {}

        @Override
        public void encode(CompoundTag compound, ItemStack itemStack, HolderLookup.Provider provider) {
            compound.put("ItemStack", itemStack.save(provider));
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("ItemStack");
        }

        @Override
        public ItemStack decode(CompoundTag compound, HolderLookup.Provider provider) {
            var tag = compound.get("ItemStack");
            if(tag == null) return ItemStack.EMPTY;
            return ItemStack.parse(provider, tag).orElse(ItemStack.EMPTY);
        }
    }

    private static final class Buffer implements BufferSerializable<ItemStack> {

        @Override
        public Class<ItemStack> getTargetClass() {
            return ItemStack.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ItemStack itemStack) {
            buffer.writeBoolean(itemStack.isEmpty());
            if(!itemStack.isEmpty()) {
                buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
                buffer.writeInt(itemStack.getCount());
            }
        }

        @Override
        public ItemStack decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readBoolean()
                ? ItemStack.EMPTY
                : new ItemStack(
                    BuiltInRegistries.ITEM.get(buffer.readResourceLocation()),
                    buffer.readInt()
                );
        }

    }

}