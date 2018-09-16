package com.example.tlinux.mpedecoderbymediacodec.decoder;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaFormat;

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

}
