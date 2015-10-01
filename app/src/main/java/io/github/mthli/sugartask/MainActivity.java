package io.github.mthli.sugartask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.activity_main_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
            }
        });
    }

    private void test() {
        SugarTask.with(this) // Activity/FragmentActivity/Fragment/android.support.v4.app.Fragment
                .name("Your thread name")
                .assign(new SugarTask.TaskDescription() {
                    @Override
                    public Object onBackground() {
                        // WorkerThread, do what you what on background.

                        // If you want to post message to MainThread for changing UI in time,
                        // just follow:
                        // Message message = new Message();
                        // message.what = your what;
                        // message.obj = your obj;
                        // ...
                        // SugarTask.post(message);

                        // return your result(Nullable).
                        return null;
                    }
                })
                .handle(new SugarTask.MessageListener() {
                    @Override
                    public void handleMessage(@NonNull Message message) {
                        // MainThread, deal with message from WorkerThread.
                    }
                })
                .finish(new SugarTask.FinishListener() {
                    @Override
                    public void onFinish(@Nullable Object result) {
                        // MainThread, deal with WorkedThread result.
                    }
                })
                .broken(new SugarTask.BrokenListener() {
                    @Override
                    public void onBroken(@NonNull Exception e) {
                        // MainThread, when WorkerThread throw an exception.
                    }
                })
                .execute(); // Here we go :)
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
