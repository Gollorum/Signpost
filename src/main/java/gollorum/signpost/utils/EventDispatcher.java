package gollorum.signpost.utils;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public interface EventDispatcher<Event> {

    public interface Listener<Event> {
        /***
         Returns whether the listener is done and should be removed.
        */
        boolean accept(Event event);
    }

    class ConsumerWrapper<Event> implements Listener<Event> {
        private final Consumer<Event> consumer;

        public ConsumerWrapper(Consumer<Event> consumer) {
            this.consumer = consumer;
        }

        @Override
        public boolean accept(Event event) {
            consumer.accept(event);
            return false;
        }

        @Override
        public int hashCode() {
            return consumer.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj ||
                (obj instanceof EventDispatcher.ConsumerWrapper<?> && consumer.equals(((ConsumerWrapper<?>)obj).consumer)) ||
                consumer.equals(obj);
        }
    }

    boolean addListener(@Nonnull Listener<Event> listener);
    default boolean addListener(@Nonnull Consumer<Event> listener) {
        return addListener(new ConsumerWrapper<>(listener));
    }

    boolean removeListener(@Nonnull Listener<Event> listener);
    default boolean removeListener(@Nonnull Consumer<Event> listener) {
        return removeListener(new ConsumerWrapper<>(listener));
    }

    abstract class Impl<Event> implements EventDispatcher<Event> {

        protected final Set<Listener<Event>> listeners = new HashSet<>();

        public boolean addListener(@Nonnull Listener<Event> listener) { return listeners.add(listener); }

        public boolean removeListener(@Nonnull Listener<Event> listener) { return listeners.remove(listener); }

        protected void dispatch(Event event, Set<Listener<Event>> listeners, boolean clearAfterwards) {
            listeners.forEach(listener -> {
                if(listener.accept(event) && !clearAfterwards) this.listeners.remove(listener);
            });
        }

        public static class WithPublicDispatch<Event> extends Impl<Event> {

            public void dispatch(Event event, boolean clearAfterwards) {
                Set<Listener<Event>> copyOfListeners = new HashSet<>(listeners);
                if(clearAfterwards) clear();
                super.dispatch(event, copyOfListeners, clearAfterwards);
            }

            public void clear() { listeners.clear(); }

        }

    }

}
