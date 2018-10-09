package com.example.tlinux.mpedecoderbymediacodec.decoder;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp3Player {

    private static final int STATE_IDLE = 0;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSE = 4;

    private int mState;
    private long mDuration =-1;
    private MediaExtractor mMediaExtractor;
    private Mp3Decoder mMp3Decoder;
    private AudioTrack mAudioTrack;
    private byte[] mAudioBuffer;


    public Mp3Player() {
        mState = STATE_IDLE;
    }

    public void setDataSource(String dataSource) throws IOException {
        mMediaExtractor = MediaUtils.createMediaExtractor(dataSource);
        mState = STATE_INITIALIZED;
    }

    public void prepare() throws IOException {
        if (mState!=STATE_INITIALIZED) {
            throw new MediaIllegalStateException("prepare",STATE_INITIALIZED,mState);
        }

        MediaFormat audorFormat = MediaUtils.getAndSelectAudioFormat(mMediaExtractor);
        if (audorFormat == null) {
            throw new IOException("no audioForamt");
        }
        MediaCodec mediaCodec = MediaUtils.createDecoderByName(audorFormat);
        mediaCodec.configure(audorFormat,null,null,0);

        if (audorFormat.containsKey(MediaFormat.KEY_DURATION))
            mDuration = audorFormat.getLong(MediaFormat.KEY_DURATION);
        mAudioTrack = createAudioTrack(audorFormat);
        mMp3Decoder = new Mp3Decoder(mMediaExtractor,mediaCodec,decoderListener);
        mState = STATE_PREPARED;
    }

    public void start() {
        if (mState!=STATE_PREPARED) {
            throw new MediaIllegalStateException("start",STATE_PREPARED,mState);
        }
        mAudioTrack.play();
        mMp3Decoder.start();
        mState = STATE_PLAYING;
    }

    //TODO 状态是否由decode返回
    public void resume() {
        if (mState == STATE_PLAYING) return;
        if (mState == STATE_PAUSE) {
            mMp3Decoder.resume();
            mAudioTrack.play();
            mState = STATE_PLAYING;
        } else {
            throw new MediaIllegalStateException("resume",STATE_PAUSE,mState);
        }
    }

    public boolean isPlaying() {
        return mState == STATE_PLAYING;
    }

    public void pause() {
        if (mState == STATE_PAUSE) return;
        if (mState == STATE_PLAYING) {
            mAudioTrack.pause();
            mMp3Decoder.pause();
            mAudioTrack.flush();
            mState = STATE_PAUSE;
        } else {
            throw new MediaIllegalStateException("pause",STATE_PAUSE+"or"+STATE_PLAYING,mState+"");
        }
    }

    public void stop() {
        if (mState == STATE_PREPARED) return;
        if (mState < STATE_PREPARED) {
            throw new MediaIllegalStateException("stop","> STATE_PREPARED",mState+"");
        }

        mMp3Decoder.stop();
        mState = STATE_PAUSE;
    }

    public void release() {
        if (mState == STATE_IDLE) return;
        if (mState > STATE_IDLE) {
            mMp3Decoder.release();
            mMp3Decoder = null;
            mMediaExtractor = null;
            mAudioTrack.release();
            mAudioTrack = null;
            mState = STATE_IDLE;
        }
    }

    public void seek(long millis) {
        if (mState>STATE_PREPARED) {
            mMp3Decoder.seekTo(millis);
        } else {
            throw new MediaIllegalStateException("seek",">STATE_PREPARED",mState+"");
        }
    }

    public long getDuration() {
        return mDuration;
    }

    public boolean isPrepared() {
        return mState>STATE_PREPARED;
    }

    private AudioTrack createAudioTrack(MediaFormat mediaFormat) {
        AudioAttributes audioAttributes =new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        if (channelCount == 2) {
            channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        }
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(encoding).build();
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,channelConfig,encoding);
        final AudioTrack audioTrack = new AudioTrack(audioAttributes,audioFormat,minBufferSize,AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        return audioTrack;
    }


    private Mp3Decoder.Mp3DecoderListener decoderListener = new Mp3Decoder.Mp3DecoderListener() {
        @Override
        public void onDecodData(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
            if (buffer.hasArray()) {
                mAudioTrack.write(buffer.array(),bufferInfo.offset,bufferInfo.size);
            } else{
                if (mAudioBuffer==null || mAudioBuffer.length<bufferInfo.size) {
                    mAudioBuffer = new byte[bufferInfo.size];
                }
                buffer.get(mAudioBuffer,0,bufferInfo.size);
                mAudioTrack.write(mAudioBuffer,bufferInfo.offset,bufferInfo.size);
                if (mAudioBuffer[0]!=0) {
                    StringBuilder builder = new StringBuilder();
                    for (int i =0;i<10;i++) {
                        builder.append((int) mAudioBuffer[i]);
                    }
                    Log.e("AAAA",builder.toString());
                }
            }
            if (mPlayerListener!=null) mPlayerListener.progress(bufferInfo.presentationTimeUs);
        }

        @Override
        public void onDataFormatChange(MediaFormat newFormat) {

        }

        @Override
        public void onDecodeEndOfFrame(long pts) {
            if (mDuration<0) mDuration = pts;
            if (mPlayerListener!=null) {
                mPlayerListener.onStreamEnd();
            }

        }
    };


    public void setPlayerListener(PlayerListener playerListener) {
        mPlayerListener = playerListener;
    }
    private PlayerListener mPlayerListener;

    public interface PlayerListener {
        void progress(long progress);
        void onStreamEnd();
    }

}
