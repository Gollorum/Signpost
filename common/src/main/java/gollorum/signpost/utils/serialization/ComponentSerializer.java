package gollorum.signpost.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class ComponentSerializer implements BufferSerializable<Component> {

    public static final ComponentSerializer instance = new ComponentSerializer();

    @Override
    public Class<Component> getTargetClass() {
        return Component.class;
    }

    @Override
    public void write(Component component, FriendlyByteBuf buffer) {
        buffer.writeComponent(component);
    }

    @Override
    public Component read(FriendlyByteBuf buffer) {
        return buffer.readComponent();
    }
}
