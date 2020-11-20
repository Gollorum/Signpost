package gollorum.signpost.utils;

import gollorum.signpost.Signpost;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class Delay {

    public static void register(IEventBus bus) { bus.register(Delay.class); }

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

    private static final List<Task> serverTasks = new ArrayList<>();
    private static final List<Task> clientTasks = new ArrayList<>();

    public static void onServerUntil(Supplier<Boolean> canRun, Runnable run) {
        if(canRun.get()) run.run();
        else serverTasks.add(new Task(canRun, run));
    }
    public static void onClientUntil(Supplier<Boolean> canRun, Runnable run) {
        if(canRun.get()) run.run();
        else clientTasks.add(new Task(canRun, run));
    }
    public static void until(Supplier<Boolean> canRun, Runnable run) {
        if(canRun.get()) run.run();
        else if(Signpost.getServerType().isServer)
            serverTasks.add(new Task(canRun, run));
        else
            clientTasks.add(new Task(canRun, run));
    }

    public static void onServerUntil(Supplier<Boolean> canRun, Runnable run, int timeoutFrames) {
        if(canRun.get()) run.run();
        else delayUntil(canRun, run, timeoutFrames, serverTasks);
    }
    public static void onClientUntil(Supplier<Boolean> canRun, Runnable run, int timeoutFrames) {
        if(canRun.get()) run.run();
        else delayUntil(canRun, run, timeoutFrames, clientTasks);
    }
    public static void until(Supplier<Boolean> canRun, Runnable run, int timeoutFrames) {
        if(canRun.get()) run.run();
        else delayUntil(canRun, run, timeoutFrames, Signpost.getServerType().isServer ? serverTasks : clientTasks);
    }

    private static void delayUntil(Supplier<Boolean> canRun, Runnable run, int timeoutFrames, List<Task> taskList) {
        AtomicInteger framesLeft = new AtomicInteger(timeoutFrames);
        taskList.add(new Task(
            () -> {
                framesLeft.set(framesLeft.get() - 1);
                return canRun.get() || framesLeft.get() < 0;
            },
            () -> {
                if(framesLeft.get() >= 0) run.run();
            }
        ));
    }

    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        Task[] tasks = serverTasks.toArray(new Task[0]);
        serverTasks.clear();
        for(Task task: tasks) {
            if(task.canRun()) task.run();
            else serverTasks.add(task);
        }
    }

    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        Task[] tasks = clientTasks.toArray(new Task[0]);
        clientTasks.clear();
        for(Task task: tasks) {
            if(task.canRun()) task.run();
            else clientTasks.add(task);
        }
    }

}
