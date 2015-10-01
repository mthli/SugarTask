package io.github.mthli.sugartask;

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
                test();
            }
        });
    }

    private void test() {
        SugarTask.with(this)
                .name("9527")
                .assign(new SugarTask.TaskDescription() {
                    @Override
                    public Object onBackground() {
                        try {
                            Thread.sleep(1000);

                            Message message = new Message();
                            message.what = 9527;
                            SugarTask.post(message);

                            Thread.sleep(1000);
                        } catch (Exception e) {}

                        return null;
                    }
                })
                .handle(new SugarTask.MessageListener() {
                    @Override
                    public void handleMessage(@NonNull Message message) {
                        Log.e("message", String.valueOf(message.what));
                    }
                })
                .finish(new SugarTask.FinishListener() {
                    @Override
                    public void onFinish(@Nullable Object result) {
                        Toast.makeText(getContext(), "finish", Toast.LENGTH_SHORT).show();
                    }
                })
                .broken(new SugarTask.BrokenListener() {
                    @Override
                    public void onBroken(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                })
                .execute();
    }
}
