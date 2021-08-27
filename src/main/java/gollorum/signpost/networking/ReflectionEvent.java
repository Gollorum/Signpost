package gollorum.signpost.networking;

import gollorum.signpost.utils.Tuple;
import gollorum.signpost.utils.serialization.BufferSerializable;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Highly unsafe. Use with care.
public abstract class ReflectionEvent<Self extends ReflectionEvent<Self>> implements PacketHandler.Event<Self> {

    private final List<Tuple<Field, BufferSerializable>> fieldsAndSerializers;

    protected ReflectionEvent(Object thisIsNotTheEmptyConstructor) { fieldsAndSerializers = null; }

    protected ReflectionEvent() {
        fieldsAndSerializers = Arrays.stream(getClass().getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(SerializedWith.class))
            .<Tuple<Field, BufferSerializable>>map(f -> {
                f.setAccessible(true);
                try {
                    SerializedWith annotation = f.getAnnotation(SerializedWith.class);
                    BufferSerializable<?> serializer = annotation.serializer().newInstance();
                    if(annotation.optional()) serializer = serializer.optional();
                    if(f.getType().isAssignableFrom(serializer.getTargetClass())) {
                        return new Tuple<>(f, serializer);
                    } else throw new RuntimeException("Tried to construct serializer for field "
                        + f.getName() + " in class " + getClass().getName() + ", but the given one was for " + serializer.getTargetClass().getName()
                        + " (expected " + f.getType() + ")");
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Could not construct serializer for field "
                        + f.getName() + " in class " + getClass().getName() + ": " + e.getMessage());
                }
            }).collect(Collectors.toList());
    }

    @Override
    public final void encode(Self self, PacketBuffer buffer) {
        try {
            for(Tuple<Field, BufferSerializable> tuple : fieldsAndSerializers) {
                tuple._2.write(tuple._1.get(self), buffer);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Something went wrong trying to serialize class " + getClass().getName() + ": " + e.getMessage());
        }
    }

    @Override
    public final Self decode(PacketBuffer buffer) {
        try {
            Self self = (Self) getClass().newInstance();
            for(Tuple<Field, BufferSerializable> tuple : fieldsAndSerializers) {
                tuple._1.set(self, tuple._2.read(buffer));
            }
            return self;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Something went wrong trying to deserialize class " + getClass().getName() + ": " + e.getMessage());
        }
    }

    public abstract void handle(Self message, NetworkEvent.Context context);
}
