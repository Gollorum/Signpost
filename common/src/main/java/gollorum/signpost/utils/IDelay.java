package gollorum.signpost.utils;

import gollorum.signpost.Signpost;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IDelay {

    public static IDelay getInstance(){
        return Signpost.getDelay();
    }

    public static void onClientForFrames(int frames, Runnable run) {
        IDelay.forFrames(frames, true, run);
    }

    public static void onServerForFrames(int frames, Runnable run) {
        IDelay.forFrames(frames, false, run);
    }

    public void forFramesImpl(int frames, boolean onClient, Runnable run);
    public static void forFrames(int frames, boolean onClient, Runnable run) {
        getInstance().forFramesImpl(frames, onClient, run);
    }

    public void onServerUntilImpl(Supplier<Boolean> canRun, Runnable run);
    public static void onServerUntil(Supplier<Boolean> canRun, Runnable run) {
        getInstance().onServerUntilImpl(canRun, run);
    }

    public void onClientUntilImpl(Supplier<Boolean> canRun, Runnable run);
    public static void onClientUntil(Supplier<Boolean> canRun, Runnable run) {
        getInstance().onClientUntilImpl(canRun, run);
    }

    public static void until(Supplier<Boolean> canRun, Runnable run, boolean onClient) {
        if(onClient)
            onClientUntil(canRun, run);
        else
            onServerUntil(canRun, run);
    }

    public void onServerUntilImpl(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, Optional<Runnable> onTimeOut);
    public static void onServerUntil(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        getInstance().onServerUntilImpl(canRun, run, timeoutFrames, onTimeOut);
    }

    public void onClientUntilImpl(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, Optional<Runnable> onTimeOut);
    public static void onClientUntil(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        getInstance().onClientUntilImpl(canRun, run, timeoutFrames, onTimeOut);
    }

    public <T> void onServerUntilIsPresentImpl(Supplier<Optional<T>> supplier, Consumer<T> run, int timeoutFrames, Optional<Runnable> onTimeOut);
    public static <T> void onServerUntilIsPresent(Supplier<Optional<T>> supplier, Consumer<T> run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        getInstance().onServerUntilIsPresentImpl(supplier, run, timeoutFrames, onTimeOut);
    }

    public <T> void onClientUntilIsPresentImpl(Supplier<Optional<T>> supplier, Consumer<T> run, int timeoutFrames, Optional<Runnable> onTimeOut);
    public static <T> void onClientUntilIsPresent(Supplier<Optional<T>> supplier, Consumer<T> run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        getInstance().onClientUntilIsPresentImpl(supplier, run, timeoutFrames, onTimeOut);
    }

    public static void until(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, boolean onClient, Optional<Runnable> onTimeOut) {
        if(onClient)
            onClientUntil(canRun, run, timeoutFrames, onTimeOut);
        else
            onServerUntil(canRun, run, timeoutFrames, onTimeOut);
    }

    public static <T> void untilIsPresent(Supplier<Optional<T>> supplier, Consumer<T> run, int timeoutFrames, boolean onClient, Optional<Runnable> onTimeOut) {
        if(onClient)
            onClientUntilIsPresent(supplier, run, timeoutFrames, onTimeOut);
        else
            onServerUntilIsPresent(supplier, run, timeoutFrames, onTimeOut);
    }

}
