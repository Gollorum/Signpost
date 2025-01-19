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
        buffer.writeComponent(component);
    }

    @Override
    public Component decode(RegistryFriendlyByteBuf buffer) {
        return buffer.readComponent();
    }
}
