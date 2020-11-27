package gollorum.signpost.utils;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public interface EventDispatcher<Event> {

    boolean addListener(@Nonnull Consumer<Event> listener);

    boolean removeListener(@Nonnull Consumer<Event> listener);

    class Impl<Event> implements EventDispatcher<Event> {

        protected final Set<Consumer<Event>> listeners = new HashSet<>();

        public boolean addListener(@Nonnull Consumer<Event> listener) { return listeners.add(listener); }

        public boolean removeListener(@Nonnull Consumer<Event> listener) { return listeners.remove(listener); }

        protected void dispatch(Event event) { dispatch(event, new HashSet<>(listeners)); }

        protected void dispatch(Event event, Set<Consumer<Event>> listeners) { listeners.forEach(listener -> listener.accept(event)); }

        public static class WithPublicDispatch<Event> extends Impl<Event> {

            public void dispatch(Event event, boolean clearAfterwards) {
                Set<Consumer<Event>> copyOfListeners = new HashSet<>(listeners);
                if(clearAfterwards) clear();
                super.dispatch(event, copyOfListeners);
            }

            public void clear() { listeners.clear(); }

        }

    }

}
