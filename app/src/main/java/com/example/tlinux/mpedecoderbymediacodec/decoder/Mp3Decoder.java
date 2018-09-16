package com.example.tlinux.mpedecoderbymediacodec.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC;

public class Mp3Decoder {

    private static final int STATE_IDLE=0;  //初始化情况 | releas
    private static final int STATE_RUNNING = 2;  // 正在播放  start
    private static final int STATE_PAUSE = 3;  // 暂停   pause
    private static final int STATE_RELEASED = 4;

    private int mState ;
    private final DecodThread mDecodThread;
    private final MediaExtractor mMediaExtractor;
    private final MediaCodec mMediaCodec;
    private Mp3DecoderListener mListener;


     Mp3Decoder(MediaExtractor extractor,MediaCodec mediaCodec,Mp3DecoderListener listener) {
        mState = STATE_IDLE;
        mMediaExtractor = extractor;
        mMediaCodec = mediaCodec;
        mListener = listener;
        mDecodThread  = new DecodThread(mMediaExtractor,mMediaCodec,new DecodThread.DecoderListener(){

            @Override
            public void onDecodData(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
                if (mListener != null) {
                    mListener.onDecodData(buffer,bufferInfo);
                }
            }

            @Override
            public void onDataFormatChange(MediaFormat newFormat) {
                if (mListener != null) {
                    mListener.onDataFormatChange(newFormat);
                }
            }
        });
    }


    public void start() {
        if (mState!=STATE_IDLE) {
            throw new IllegalStateException("start state error: "+mState);
        }
        mState = STATE_RUNNING;
        mMediaCodec.start();
        mDecodThread.start();
    }

    public void resume() {
        if (mState!=STATE_PAUSE) {
            throw new IllegalStateException("resume state error: "+mState);
        }
        mDecodThread.resumeDecord();
        mState = STATE_RUNNING;
    }

    public void stop(){
        if (mState == STATE_IDLE || mState == STATE_RELEASED) {
            throw new IllegalStateException("resume state error: "+mState);
        }
        // seekto 0 and pause
        mDecodThread.stopDecord();
    }

    public void pause(){
       if (mState == STATE_IDLE || mState == STATE_RELEASED) {
           throw new IllegalStateException("pause state error: "+mState);
       }
       mDecodThread.pauseDecord();
       mState = STATE_PAUSE;
    }

    public void release() {
        if (mState == STATE_IDLE) {
            throw new IllegalStateException("release state error: "+mState);
        }
        if (mState == STATE_RELEASED) return;

        if (mState == STATE_PAUSE) {
            resume();
        }
        mDecodThread.releaseDecord();
        mState = STATE_IDLE;
    }

    public void seekTo(long millis){
        if (mState == STATE_IDLE) {
            mMediaExtractor.seekTo(millis*1000,SEEK_TO_PREVIOUS_SYNC);
        }
        if (mState == STATE_RELEASED) {
            throw new IllegalStateException("seekTo state error: "+mState);
        }
        mDecodThread.seekTo(millis*1000);
    }

    public interface Mp3DecoderListener {
        void onDecodData(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo);
        void onDataFormatChange(MediaFormat newFormat);
    }


    static class DecodThread extends Thread {

        private final MediaExtractor mMediaExtractor;
        private final MediaCodec mMediaCodec;
        private final DecoderListener mDecoderListener;

        private Object mPauseLock = new Object();

        private volatile boolean mLoop = false;
        private volatile AtomicLong mSeekTime = new AtomicLong(-1);
        private volatile int mState = STATE_IDLE;


        public static final int STATE_IDLE = 0;
        public static final int STATE_RUNNING = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_RELEASED =3;


        @Override
        public synchronized void start() {
            if (STATE_IDLE!= mState)  {
                throw new IllegalStateException("start mState error: mState="+ mState);
            }
            super.start();
        }

        public void resumeDecord() {
            if (mState == STATE_IDLE || mState == STATE_RELEASED) {
                throw new IllegalStateException("start mState error: mState="+ mState);
            } else {
                synchronized (mPauseLock) {
                    mState = STATE_RUNNING;
                    mPauseLock.notifyAll();
                }
            }
        }

        public void pauseDecord() {
            if (mState == STATE_RELEASED || mState == STATE_IDLE) {
                throw new IllegalStateException("pauseDecord mState error: mState="+ mState);
            }
            synchronized (mPauseLock) {
                mState = STATE_PAUSE;
            }
        }

        public void stopDecord() {
            if (mState == STATE_RELEASED || mState == STATE_IDLE) {
                throw new IllegalStateException("pauseDecord mState error: mState="+ mState);
            }
            synchronized (mPauseLock) {
                mState = STATE_PAUSE;
            }
            seekTo(0);
        }


        private void releaseAsync() {
            mMediaCodec.flush();
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaExtractor.release();
        }

        private boolean endAsync() {
            if (mLoop) {
                mMediaExtractor.seekTo(0,SEEK_TO_PREVIOUS_SYNC);
                return false;
            } else {
                releaseAsync();
                return true;
            }
        }

        public void seekTo(long millis) {
            mSeekTime.set(millis);
        }

        private DecodThread(MediaExtractor mediaExtractor, MediaCodec mediaCodec,
                            DecoderListener decoderListener) {
            mMediaCodec = mediaCodec;
            mMediaExtractor = mediaExtractor;
            mDecoderListener = decoderListener;
        }

        public void run() {
            ByteBuffer inputBuffer = ByteBuffer.allocate(2048);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (true) {
                synchronized (mPauseLock) {
                    //1 pause
                    while (mState == STATE_PAUSE) {
                        try {
                            mPauseLock.wait();  // pause 的时候不接收中断,只接收外部控制
                        } catch (InterruptedException e) {
                            Log.w("DECODER","interrupt in pauseing state");
                        }
                    }
                }

                if (mState == STATE_RELEASED) {
                    // release
                    releaseAsync();
                    break;
                }

                long seekTime;
                if ((seekTime =mSeekTime.getAndSet(-1))>0){
                    mMediaExtractor.seekTo(seekTime,SEEK_TO_PREVIOUS_SYNC);
                }

                int length;
                if((length = mMediaExtractor.readSampleData(inputBuffer,0))>0) {
                    long pts = mMediaExtractor.getSampleTime();
                    int ret = decodeData(inputBuffer,0,length,bufferInfo,pts);
                    if (ret == MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                        if (endAsync()) break;
                    }
                    mMediaExtractor.advance();
                } else {
                    //读取完成
                   if (endAsync()) {
                       break;
                   }
                }
            }
        }

        public void releaseDecord() {
        }

        interface DecoderListener {
            void onDecodData(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo);

            void onDataFormatChange(MediaFormat newFormat);
        }

        private int decodeData(ByteBuffer data, int offset, int length,
                               MediaCodec.BufferInfo bufferInfo /*output*/,long pts) {
            MediaCodec codec = mMediaCodec;
            DecoderListener listener = mDecoderListener;
            int inputBufferId = codec.dequeueInputBuffer(5000);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferId);
                inputBuffer.put(data);
                codec.queueInputBuffer(inputBufferId,offset,length,pts,0);
            }
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo,5000);
            if (outputBufferId >= 0) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
                if (listener!=null)
                    listener.onDecodData(outputBuffer,bufferInfo);
                codec.releaseOutputBuffer(outputBufferId,false);
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (listener!=null)
                    listener.onDataFormatChange(codec.getInputFormat());
            } else {
                return outputBufferId;
            }

            return 0;
        }
    }
}
