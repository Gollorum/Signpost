package gollorum.signpost.utils;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class Lazy<T> {

    @Nullable
    private T value;
    private final Supplier<T> supplier;

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }

}
