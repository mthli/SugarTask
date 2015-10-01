package io.github.mthli.sugartask;

import android.app.Activity;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.FragmentActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SugarTask {
    public interface TaskDescription {
        Object onBackground();
    }

    public interface MessageListener {
        void handleMessage(@NonNull Message message);
    }

    public interface FinishListener {
        void onFinish(@Nullable Object result);
    }

    public interface BrokenListener {
        void onBroken(@NonNull Exception e);
    }

    public class Context {
        @MainThread
        public SugarTask.Register tag(@NonNull String name) {
            return new Register(name);
        }
    }

    public class Register {
        private String name;

        public Register(@NonNull String name) {
            this.name = name;
        }

        @MainThread
        public SugarTask.Builder assign(@NonNull TaskDescription description) {
            taskMap.put(name, description);

            return new Builder(name);
        }
    }

    public class Builder {
        private String name;

        public Builder(@NonNull String name) {
            this.name = name;
        }

        @MainThread
        public SugarTask.Builder handle(@NonNull MessageListener listener) {
            messageMap.put(name, listener);

            return this;
        }

        @MainThread
        public SugarTask.Builder finish(@NonNull FinishListener listener) {
            finishMap.put(name, listener);

            return this;
        }

        @MainThread
        public SugarTask.Builder broken(@NonNull BrokenListener listener) {
            brokenMap.put(name, listener);

            return this;
        }

        @MainThread
        public void execute() {
            executor.execute(buildRunnable(name));
        }
    }

    private class Result {
        public String name;

        public Object object;

        public Result(@NonNull String name, @Nullable Object object) {
            this.name = name;
            this.object = object;
        }
    }

    @MainThread
    public static SugarTask.Context with(@NonNull Activity activity) {
        return getInstance().buildContext();
    }

    @MainThread
    public static SugarTask.Context with(@NonNull FragmentActivity activity) {
        return getInstance().buildContext();
    }

    @MainThread
    public static SugarTask.Context with(@NonNull Fragment fragment) {
        return getInstance().buildContext();
    }

    @MainThread
    public static SugarTask.Context with(@NonNull android.support.v4.app.Fragment fragment) {
        return getInstance().buildContext();
    }

    private static final int MESSAGE_FINISH = 0x65535;

    private static final int MESSAGE_BROKEN = 0x65536;

    @WorkerThread
    public static void post(@NonNull Message message) {
        getInstance().handler.sendMessage(message);
    }

    private static class SugarTaskHolder {
        public static final SugarTask INSTANCE = new SugarTask();
    }

    private static SugarTask getInstance() {
        return SugarTaskHolder.INSTANCE;
    }

    private Map<String, TaskDescription> taskMap = new HashMap<>();

    private Map<String, MessageListener> messageMap = new HashMap<>();

    private Map<String, FinishListener> finishMap = new HashMap<>();

    private Map<String, BrokenListener> brokenMap = new HashMap<>();

    private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 8);

    private Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == MESSAGE_FINISH && message.obj instanceof Result) {
                Result result = (Result) message.obj;

                if (finishMap.containsKey(result.name)) {
                    finishMap.get(result.name).onFinish(result.object);
                    finishMap.remove(result.name);
                }

                if (messageMap.containsKey(result.name)) {
                    messageMap.remove(result.name);
                }
            } else if (message.what == MESSAGE_BROKEN && message.obj instanceof Result) {
                Result result = (Result) message.obj;

                if (brokenMap.containsKey(result.name)) {
                    brokenMap.get(result.name).onBroken((Exception) result.object);
                    brokenMap.remove(result.name);
                }

                if (messageMap.containsKey(result.name)) {
                    messageMap.remove(result.name);
                }
            } else {
                for (MessageListener listener : messageMap.values()) {
                    listener.handleMessage(message);
                }
            }

            return true;
        }
    });

    private Context buildContext() {
        return new Context();
    }

    private Runnable buildRunnable(@NonNull final String name) {
        return new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                if (taskMap.containsKey(name)) {
                    Message message = new Message();

                    try {
                        message.what = MESSAGE_FINISH;
                        message.obj = new Result(name, taskMap.get(name).onBackground());
                    } catch (Exception e) {
                        message.what = MESSAGE_BROKEN;
                        message.obj = new Result(name, e);
                    }

                    post(message);
                }
            }
        };
    }
}