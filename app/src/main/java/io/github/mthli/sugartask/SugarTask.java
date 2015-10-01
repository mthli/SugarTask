package io.github.mthli.sugartask;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
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
        private Context() {}

        @MainThread
        public Register name(@NonNull String name) {
            return new Register(name);
        }
    }

    public class Register {
        private String name;

        private Register(@NonNull String name) {
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

        private Builder(@NonNull String name) {
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

    private class Holder {
        private String name;

        private Object object;

        private Holder(@NonNull String name, @Nullable Object object) {
            this.name = name;
            this.object = object;
        }
    }

    public static final int MESSAGE_FINISH = 0x65534;

    public static final int MESSAGE_BROKEN = 0x65535;

    public static final int MESSAGE_STOP = 0x65536;

    public static final String TAG_HOOK = "HOOK";

    private static final String NAME_ACTIVITY = "ACTIVITY";

    private static final String NAME_FRAGMENT_ACTIVITY = "FRAGMENT_ACTIVITY";

    private static final String NAME_FRAGMENT = "FRAGMENT";

    private static final String NAME_SUPPORT_FRAGMENT = "SUPPORT_FRAGMENT";

    public static class HookFragment extends Fragment {
        protected boolean postEnable = true;

        @Override
        public void onStop() {
            super.onStop();

            if (postEnable) {
                Message message = new Message();
                message.what = MESSAGE_STOP;
                post(message);
            }
        }
    }

    public static class HookSupportFragment extends android.support.v4.app.Fragment {
        protected boolean postEnable = true;

        @Override
        public void onStop() {
            super.onStop();

            if (postEnable) {
                Message message = new Message();
                message.what = MESSAGE_STOP;
                post(message);
            }
        }
    }

    @MainThread
    public static Context with(@NonNull Activity activity) {
        getInstance().registerHookToContext(activity);

        return getInstance().buildContext(activity);
    }

    @MainThread
    public static Context with(@NonNull FragmentActivity activity) {
        getInstance().registerHookToContext(activity);

        return getInstance().buildContext(activity);
    }

    @MainThread
    public static Context with(@NonNull Fragment fragment) {
        getInstance().registerHookToContext(fragment);

        return getInstance().buildContext(fragment);
    }

    @MainThread
    public static Context with(@NonNull android.support.v4.app.Fragment fragment) {
        getInstance().registerHookToContext(fragment);

        return getInstance().buildContext(fragment);
    }

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

    private Holder holder = null;

    private Map<String, TaskDescription> taskMap = new HashMap<>();

    private Map<String, MessageListener> messageMap = new HashMap<>();

    private Map<String, FinishListener> finishMap = new HashMap<>();

    private Map<String, BrokenListener> brokenMap = new HashMap<>();

    private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 8);

    private Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == MESSAGE_FINISH && message.obj instanceof Holder) {
                Holder result = (Holder) message.obj;

                if (finishMap.containsKey(result.name)) {
                    finishMap.get(result.name).onFinish(result.object);
                    finishMap.remove(result.name);
                }

                if (messageMap.containsKey(result.name)) {
                    messageMap.remove(result.name);
                }

                getInstance().dispatchUnregister();
            } else if (message.what == MESSAGE_BROKEN && message.obj instanceof Holder) {
                Holder result = (Holder) message.obj;

                if (brokenMap.containsKey(result.name)) {
                    brokenMap.get(result.name).onBroken((Exception) result.object);
                    brokenMap.remove(result.name);
                }

                if (messageMap.containsKey(result.name)) {
                    messageMap.remove(result.name);
                }

                getInstance().dispatchUnregister();
            } else if (message.what == MESSAGE_STOP) {
                if (holder != null) {
                    holder.name = null;
                    holder.object = null;
                    holder = null;
                }

                taskMap.clear();
                messageMap.clear();
                finishMap.clear();
                brokenMap.clear();
            } else {
                for (MessageListener listener : messageMap.values()) {
                    listener.handleMessage(message);
                }
            }

            return true;
        }
    });

    private Context buildContext(@NonNull Activity activity) {
        holder = new Holder(NAME_ACTIVITY, activity);

        return new Context();
    }

    private Context buildContext(@NonNull FragmentActivity activity) {
        holder = new Holder(NAME_FRAGMENT_ACTIVITY, activity);

        return new Context();
    }

    private Context buildContext(@NonNull Fragment fragment) {
        holder = new Holder(NAME_FRAGMENT, fragment);

        return new Context();
    }

    private Context buildContext(@NonNull android.support.v4.app.Fragment fragment) {
        holder = new Holder(NAME_SUPPORT_FRAGMENT, fragment);

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
                        message.obj = new Holder(name, taskMap.get(name).onBackground());
                    } catch (Exception e) {
                        message.what = MESSAGE_BROKEN;
                        message.obj = new Holder(name, e);
                    }

                    post(message);
                }
            }
        };
    }

    private void registerHookToContext(@NonNull Activity activity) {
        FragmentManager manager = activity.getFragmentManager();

        HookFragment hookFragment = (HookFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookFragment == null) {
            hookFragment = new HookFragment();
            manager.beginTransaction().add(hookFragment, TAG_HOOK).commitAllowingStateLoss();
        }
    }

    private void registerHookToContext(@NonNull FragmentActivity activity) {
        android.support.v4.app.FragmentManager manager = activity.getSupportFragmentManager();

        HookSupportFragment hookSupportFragment = (HookSupportFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookSupportFragment == null) {
            hookSupportFragment = new HookSupportFragment();
            manager.beginTransaction().add(hookSupportFragment, TAG_HOOK).commitAllowingStateLoss();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void registerHookToContext(@NonNull Fragment fragment) {
        FragmentManager manager = fragment.getChildFragmentManager();

        HookFragment hookFragment = (HookFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookFragment == null) {
            hookFragment = new HookFragment();
            manager.beginTransaction().add(hookFragment, TAG_HOOK).commitAllowingStateLoss();
        }
    }

    private void registerHookToContext(@NonNull android.support.v4.app.Fragment fragment) {
        android.support.v4.app.FragmentManager manager = fragment.getChildFragmentManager();

        HookSupportFragment hookSupportFragment = (HookSupportFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookSupportFragment == null) {
            hookSupportFragment = new HookSupportFragment();
            manager.beginTransaction().add(hookSupportFragment, TAG_HOOK).commitAllowingStateLoss();
        }
    }

    private void dispatchUnregister() {
        if (holder == null) {
            return;
        }

        if (holder.name.equals(NAME_ACTIVITY) && holder.object instanceof Activity) {
            unregisterHookToContext((Activity) holder.object);
        } else if (holder.name.equals(NAME_FRAGMENT_ACTIVITY) && holder.object instanceof FragmentActivity) {
            unregisterHookToContext((FragmentActivity) holder.object);
        } else if (holder.name.equals(NAME_FRAGMENT) && holder.object instanceof Fragment) {
            unregisterHookToContext((Fragment) holder.object);
        } else if (holder.name.equals(NAME_SUPPORT_FRAGMENT) && holder.object instanceof android.support.v4.app.Fragment) {
            unregisterHookToContext((android.support.v4.app.Fragment) holder.object);
        }
    }

    private void unregisterHookToContext(@NonNull Activity activity) {
        FragmentManager manager = activity.getFragmentManager();

        HookFragment hookFragment = (HookFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookFragment != null) {
            hookFragment.postEnable = false;
            manager.beginTransaction().remove(hookFragment).commitAllowingStateLoss();
        }
    }

    private void unregisterHookToContext(@NonNull FragmentActivity activity) {
        android.support.v4.app.FragmentManager manager = activity.getSupportFragmentManager();

        HookSupportFragment hookSupportFragment = (HookSupportFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookSupportFragment != null) {
            hookSupportFragment.postEnable = false;
            manager.beginTransaction().remove(hookSupportFragment).commitAllowingStateLoss();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void unregisterHookToContext(@NonNull Fragment fragment) {
        FragmentManager manager = fragment.getChildFragmentManager();

        HookFragment hookFragment = (HookFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookFragment != null) {
            hookFragment.postEnable = false;
            manager.beginTransaction().remove(hookFragment).commitAllowingStateLoss();
        }
    }

    private void unregisterHookToContext(@NonNull android.support.v4.app.Fragment fragment) {
        android.support.v4.app.FragmentManager manager = fragment.getChildFragmentManager();

        HookSupportFragment hookSupportFragment = (HookSupportFragment) manager.findFragmentByTag(TAG_HOOK);
        if (hookSupportFragment != null) {
            hookSupportFragment.postEnable = false;
            manager.beginTransaction().remove(hookSupportFragment).commitAllowingStateLoss();
        }
    }
}