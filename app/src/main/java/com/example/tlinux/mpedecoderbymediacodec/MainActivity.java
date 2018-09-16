package com.example.tlinux.mpedecoderbymediacodec;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.tlinux.mpedecoderbymediacodec.decoder.DecoderTest;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            DecoderTest.test();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
