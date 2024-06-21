package gollorum.signpost.utils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Delay implements IDelay {

    public static final Delay INSTANCE = new Delay();

    private static class Task {
        private final Supplier<Boolean> canRun;
        private final Runnable run;

        public boolean canRun() { return canRun.get(); }
        public void run() { run.run(); }

        private Task(Supplier<Boolean> canRun, Runnable run) {
            this.canRun = canRun;
            this.run = run;
        }
    }

    private final List<Task> serverTasks = new ArrayList<>();
    private final List<Task> clientTasks = new ArrayList<>();

    @Override
    public void forFramesImpl(int frames, boolean onClient, Runnable run) {
        if(frames == 0) run.run();
        else {
            AtomicInteger framesLeft = new AtomicInteger(frames);
            (onClient ? clientTasks : serverTasks).add(new Task(() -> framesLeft.decrementAndGet() <= 0, run));
        }
    }

    @Override
    public void onServerUntilImpl(Supplier<Boolean> canRun, Runnable run) {
        if(canRun.get()) run.run();
        else serverTasks.add(new Task(canRun, run));
    }

    @Override
    public void onClientUntilImpl(Supplier<Boolean> canRun, Runnable run) {
        if(canRun.get()) run.run();
        else clientTasks.add(new Task(canRun, run));
    }

    @Override
    public void onServerUntilImpl(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        if(canRun.get()) run.run();
        else delayUntil(canRun, run, timeoutFrames, serverTasks, onTimeOut);
    }

    @Override
    public void onClientUntilImpl(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        if(canRun.get()) run.run();
        else delayUntil(canRun, run, timeoutFrames, clientTasks, onTimeOut);
    }

    @Override
    public <T> void onServerUntilIsPresentImpl(Supplier<Optional<T>> supplier, Consumer<T> run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(supplier.get());
        if(result.get().isPresent()) run.accept(result.get().get());
        else delayUntil(
            () -> {
                result.set(supplier.get());
                return result.get().isPresent();
            },
            () -> run.accept(result.get().get()),
            timeoutFrames,
            serverTasks,
            onTimeOut
        );
    }

    @Override
    public <T> void onClientUntilIsPresentImpl(Supplier<Optional<T>> supplier, Consumer<T> run, int timeoutFrames, Optional<Runnable> onTimeOut) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(supplier.get());
        if(result.get().isPresent()) run.accept(result.get().get());
        else delayUntil(
            () -> {
                result.set(supplier.get());
                return result.get().isPresent();
            },
            () -> run.accept(result.get().get()),
            timeoutFrames,
            clientTasks,
            onTimeOut
        );
    }

    private void delayUntil(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, List<Task> taskList, Optional<Runnable> onTimeOut) {
        AtomicInteger framesLeft = new AtomicInteger(timeoutFrames);
        taskList.add(new Task(
            () -> {
                framesLeft.set(framesLeft.get() - 1);
                return canRun.get() || framesLeft.get() < 0;
            },
            () -> {
                if (framesLeft.get() >= 0) run.run();
                else onTimeOut.ifPresent(Runnable::run);
            }
        ));
    }

    @SubscribeEvent
    void onServerTick(TickEvent.ServerTickEvent event) {
        Task[] tasks = serverTasks.toArray(new Task[0]);
        serverTasks.clear();
        for(Task task: tasks) {
            if(task.canRun()) task.run();
            else serverTasks.add(task);
        }
    }

    @SubscribeEvent
    void onClientTick(TickEvent.ClientTickEvent event) {
        Task[] tasks = clientTasks.toArray(new Task[0]);
        clientTasks.clear();
        for(Task task: tasks) {
            if(task.canRun()) task.run();
            else clientTasks.add(task);
        }
    }

}
