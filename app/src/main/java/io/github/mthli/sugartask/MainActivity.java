package io.github.mthli.sugartask;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
        SugarTask.with(this)
                .tag("name")
                .assign(new SugarTask.TaskDescription() {
                    @Override
                    public Object onBackground() {
                        try {
                            Thread.sleep(10000);
                        } catch (Exception e) {
                        }

                        return null;
                    }
                })
                .finish(new SugarTask.FinishListener() {
                    @Override
                    public void onFinish(Object result) {
                        Toast.makeText(MainActivity.this, "finish", Toast.LENGTH_SHORT).show();
                    }
                })
                .execute();
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
