package jp.co.orifice.androidjava_pcmrecorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextView indicate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (true) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        findViewById(R.id.bt_rec).setOnClickListener(onClickDebug);
        indicate = findViewById(R.id.tv_indicate);
    }
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsAndValidate();
    }
    private void checkPermissionsAndValidate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }
    }

    // Button Click.
    View.OnClickListener onClickDebug = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            indicate.setText("Recording....");
            AudioCapturePcm abuf = new AudioCapturePcm();
            try {
                FileOutputStream fos  = openFileOutput("audio.pcm", Context.MODE_PRIVATE);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DataOutputStream dos = new DataOutputStream(bos);

                short[] rbuf = new short[1024];
                int samples = 10*48000;
                abuf.recStart();

                for(int i=0; i<samples/1024; i++) {
                    int rsize = abuf.popData(rbuf, 1024);
                    for (int x=0; x<rsize; x++)
                        dos.writeShort(rbuf[x]);
                }

                dos.flush();
                dos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            indicate.setText("Stop");
        }
    };
}