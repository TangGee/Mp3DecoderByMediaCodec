package com.example.tlinux.mpedecoderbymediacodec.decoder;

public class MediaIllegalStateException extends IllegalStateException{

    public MediaIllegalStateException(String method, String needState,String currentState){
        super("method("+method+") need state "+needState+"  current state:" +currentState );
    }

    public MediaIllegalStateException(String method, int needState,int currentState){
        this(method,needState+"",currentState+"");
    }
}
