package com.example.tlinux.mpedecoderbymediacodec.decoder;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecoderTest {

    public static void test() throws IOException {

        MediaExtractor extractor = new MediaExtractor();
        String path = "/sdcard/jiangzhende.mp3";
        extractor.setDataSource(path);

        MediaFormat selectFormat = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            if (isAudioForamt(mediaFormat)){
                extractor.selectTrack(i);
                selectFormat = mediaFormat;
                break;
            }
        }

        if (selectFormat == null) {
            Log.e("AAAA","can't find a audio stream");
            return;
        }

        MediaCodec mediaCodec = MediaUtils.createDecoderByName(selectFormat);
        mediaCodec.configure(selectFormat,null,null,0);

        AudioAttributes audioAttributes =new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        int sampleRate = selectFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = selectFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        if (channelCount == 2) {
            channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        }
//        int encoding = selectFormat.getInteger(MediaFormat.KEY);
        Log.e("AAAA",selectFormat.toString());
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(encoding).build();
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,channelConfig,encoding);
        final AudioTrack audioTrack = new AudioTrack(audioAttributes,audioFormat,minBufferSize,AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);

        Log.e("AAAA","sampleRate: "+sampleRate+"  channelCount:"+channelCount+"   channelConfig: "+channelConfig+"  encoding:"+encoding);
        Mp3Decoder mp3Decoder = new Mp3Decoder(extractor, mediaCodec, new Mp3Decoder.Mp3DecoderListener() {
            @Override
            public void onDecodData(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
                if (buffer.hasArray()) {
                    audioTrack.write(buffer.array(),bufferInfo.offset, bufferInfo.size);
                } else{
                    if (useBuffer==null || useBuffer.length<bufferInfo.size) {
                        useBuffer = new byte[bufferInfo.size];
                    }
                    buffer.get(useBuffer,0,bufferInfo.size);
                    audioTrack.write(useBuffer,0,bufferInfo.size);
                    if (useBuffer[0]!=0) {
                        StringBuilder builder = new StringBuilder();
                        for (int i =0;i<10;i++) {
                            builder.append((int) useBuffer[i]);
                        }
                        Log.e("AAAA",builder.toString());
                    }
                }
            }

            @Override
            public void onDataFormatChange(MediaFormat newFormat) {
            }

            @Override
            public void onDecodeEndOfFrame(long pts) {

            }
        });
        audioTrack.play();
        mp3Decoder.start();

    }


    private static boolean isAudioForamt(MediaFormat mediaFormat) {
        return TextUtils.equals(mediaFormat.getString(MediaFormat.KEY_MIME),
                MediaFormat.MIMETYPE_AUDIO_MPEG);
    }

    private static byte[] useBuffer;
}
