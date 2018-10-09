package com.example.tlinux.mpedecoderbymediacodec;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.tlinux.mpedecoderbymediacodec.decoder.Mp3Player;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, Mp3Player.PlayerListener {

    private Mp3Player mMp3Player;
    private boolean mStoped = true;
    private SeekBar mSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMp3Player = new Mp3Player();
        String path = "/sdcard/jiangzhende.mp3";
        try {
            mMp3Player.setDataSource(path);
            mMp3Player.prepare();
        } catch (IOException e) {
            Toast.makeText(this,"file 不存在或者不是一个好的文件"+e.getMessage(),Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        findViewById(R.id.playButton).setOnClickListener(this);
        findViewById(R.id.stopButton).setOnClickListener(this);
        mSeekBar = findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax((int) mMp3Player.getDuration());
        Log.e("AAAA","mDuration"+ mMp3Player.getDuration());
        mMp3Player.setPlayerListener(this);

    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playButton) {
            if (mStoped) {
                mStoped = false;
                ((Button)v).setText("暂停");
                mMp3Player.start();
                return;
            }
            if (mMp3Player.isPlaying()) {
                mMp3Player.pause();
                ((Button)v).setText("播放");
            } else {
                mMp3Player.resume();
                ((Button)v).setText("暂停");
            }
        } else if (v.getId() == R.id.stopButton) {
            if (!mStoped){
                mSeekBar.setProgress(0);
                mMp3Player.stop();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMp3Player.release();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mMp3Player.isPrepared())
            mMp3Player.seek(seekBar.getProgress()/1000);
    }

    @Override
    public void progress(long progress) {
        if (mSeekBar!=null) mSeekBar.setProgress((int) progress);
    }

    @Override
    public void onStreamEnd() {

    }
}
