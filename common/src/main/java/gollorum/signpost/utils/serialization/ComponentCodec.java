package gollorum.signpost.utils.serialization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

public class ComponentCodec implements StreamCodec<RegistryFriendlyByteBuf, Component> {

    public static final ComponentCodec instance = new ComponentCodec();

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Component component) {
        buffer.writeUtf(Component.Serializer.toJson(component, buffer.registryAccess()));
    }

    @Override
    public Component decode(RegistryFriendlyByteBuf buffer) {
        return Component.Serializer.fromJson(buffer.readUtf(), buffer.registryAccess());
    }
}
