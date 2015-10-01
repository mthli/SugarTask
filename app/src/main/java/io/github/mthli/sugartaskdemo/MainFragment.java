package io.github.mthli.sugartaskdemo;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import io.github.mthli.sugartask.SugarTask;

public class MainFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Button button = (Button) view.findViewById(R.id.fragment_main_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                example();
            }
        });
    }

    private void example() {
        SugarTask.with(this) // Must. 必须。
                .assign(new SugarTask.TaskDescription() { // Must. 必须。
                    @Override
                    public Object onBackground() {
                        try {
                            // Post a message from WorkerThread to MainThread.
                            // 在后台线程中向主线程发送消息。
                            Message message = new Message();
                            message.obj = "begin sleep 5000ms";
                            SugarTask.post(message);

                            Thread.sleep(5000);

                            Log.e("onBackground", "after sleep 5000s");
                        } catch (Exception e) {

                        }

                        // Return your finally result(Nullable).
                        // 返回你最后得到的结构，可以为 null 。
                        return null;
                    }
                })
                .handle(new SugarTask.MessageListener() { // Option. 可选。
                    @Override
                    public void handleMessage(@NonNull Message message) {
                        // Receive message in MainThread which sent from WorkerThread.
                        // 在主线程中接收来自后台线程发送的消息。
                        Toast.makeText(getContext(), (String) message.obj, Toast.LENGTH_SHORT).show();
                    }
                })
                .finish(new SugarTask.FinishListener() { // Option. 可选。
                    @Override
                    public void onFinish(@Nullable Object result) {
                        // If WorkerThread finish without Exception and lifecycle safety, Toast will be show.
                        // You can press back button(in NavigationBar) to finish demo application before WorkerThread finish(5000ms),
                        // and Toast won't be shown because our magic, so avoid OOM :)
                        // 如果后台线程正常结束且当前上下文的生命周期安全，则显示 Toast 。
                        // 你可以尝试在后台线程结束前按下 NavigationBar 上的返回键，结束 demo ，
                        // 那么 Toast 将不会被显示，像这样我们就可以避免 OOM 。
                        Toast.makeText(getContext(), "finish", Toast.LENGTH_SHORT).show();
                    }
                })
                .broken(new SugarTask.BrokenListener() { // Option. 可选。
                    @Override
                    public void onBroken(@NonNull Exception e) {

                    }
                })
                .execute();
    }
}
