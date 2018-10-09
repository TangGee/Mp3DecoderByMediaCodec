package com.example.tlinux.mpedecoderbymediacodec.decoder;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import static android.media.MediaCodecList.REGULAR_CODECS;

public class MediaUtils {

    public static MediaCodec createDecoderByName(MediaFormat mediaFormat) throws IOException {
        MediaCodecList mediaCodecList = new MediaCodecList(REGULAR_CODECS);
        String name = mediaCodecList.findDecoderForFormat(mediaFormat);
        return MediaCodec.createByCodecName(name);
    }

    public static MediaCodec createDecoderByType(MediaFormat mediaFormat) throws IOException {
        return MediaCodec.createDecoderByType(
                mediaFormat.getString(MediaFormat.KEY_MIME));
    }

    public static MediaExtractor createMediaExtractor(String dataSource) throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(dataSource);
        return mediaExtractor;
    }

    public static MediaFormat getAndSelectAudioFormat(MediaExtractor mMediaExtractor) {
        MediaFormat selectFormat = null;
        for (int i = 0; i < mMediaExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = mMediaExtractor.getTrackFormat(i);
            if (isAudioForamt(mediaFormat)){
                mMediaExtractor.selectTrack(i);
                selectFormat = mediaFormat;
                break;
            }
        }

       return selectFormat;
    }


    public static boolean isAudioForamt(MediaFormat mediaFormat) {
        return TextUtils.equals(mediaFormat.getString(MediaFormat.KEY_MIME),
                MediaFormat.MIMETYPE_AUDIO_MPEG);
    }
}
