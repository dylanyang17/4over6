package com.yangyr17.v4o6;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timeTextView = (TextView) findViewById(R.id.textViewTime);
        timeTextView.setText("0");
        timeTextView.setText(JNIUtils.StringFromJNI());
//        handler = new WorkHandler(this, getMainLooper());
//        Thread thread = new Thread(new WorkRunnable(handler));
//        thread.start();
    }

    public TextView timeTextView;
    public WorkHandler handler;
}
