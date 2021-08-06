package gollorum.signpost.networking;

import gollorum.signpost.utils.serialization.BufferSerializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializedWith {

    Class<? extends BufferSerializable<?>> serializer();

    boolean optional() default false;

}
