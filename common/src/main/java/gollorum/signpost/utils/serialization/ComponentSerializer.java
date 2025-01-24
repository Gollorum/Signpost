package gollorum.signpost.utils.serialization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class ComponentSerializer implements BufferSerializable<Component> {

    public static final ComponentSerializer instance = new ComponentSerializer();

    @Override
    public Class<Component> getTargetClass() {
        return Component.class;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Component component) {
        buffer.writeUtf(Component.Serializer.toJson(component, buffer.registryAccess()));
    }

    @Override
    public Component decode(RegistryFriendlyByteBuf buffer) {
        return Component.Serializer.fromJson(buffer.readUtf(), buffer.registryAccess());
    }
}
