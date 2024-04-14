package gollorum.signpost.networking;

import gollorum.signpost.utils.Tuple;
import gollorum.signpost.utils.serialization.BufferSerializable;
import net.minecraft.network.FriendlyByteBuf;

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
    public final void encode(Self self, FriendlyByteBuf buffer) {
        try {
            for(Tuple<Field, BufferSerializable> tuple : fieldsAndSerializers) {
                tuple._2.write(tuple._1.get(self), buffer);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Something went wrong trying to serialize class " + getClass().getName() + ": " + e.getMessage());
        }
    }

    @Override
    public final Self decode(FriendlyByteBuf buffer) {
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

    public abstract void handle(Self message, PacketHandler.Context context);

    public static abstract class ForClient<Self extends ForClient<Self>>
        extends ReflectionEvent<Self>
        implements PacketHandler.Event.ForClient<Self> {

        protected ForClient(Object thisIsNotTheEmptyConstructor) { super(thisIsNotTheEmptyConstructor); }
        protected ForClient() {super();}

        @Override
        public void handle(Self message, PacketHandler.Context context) {
            if(context instanceof PacketHandler.Context.Client Client){
                handle(message, Client);
            } else throw new RuntimeException("Tried to handle client event on client");
        }
    }

    public static abstract class ForServer<Self extends ForServer<Self>>
        extends ReflectionEvent<Self> 
        implements PacketHandler.Event.ForServer<Self> {

        protected ForServer(Object thisIsNotTheEmptyConstructor) { super(thisIsNotTheEmptyConstructor); }
        protected ForServer() {super();}

        @Override
        public void handle(Self message, PacketHandler.Context context) {
            if(context instanceof PacketHandler.Context.Server server){
                handle(message, server);
            } else throw new RuntimeException("Tried to handle server event on client");
        }
    }

}

