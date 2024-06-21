package gollorum.signpost.mixin;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ArgumentTypeInfos.class)
public abstract class ArgumentTypeInfosAccessor {

    @Accessor("BY_CLASS")
    public static Map<Class<?>, ArgumentTypeInfo<?, ?>> getByClassMap() {
        throw new AssertionError();
    }

}
