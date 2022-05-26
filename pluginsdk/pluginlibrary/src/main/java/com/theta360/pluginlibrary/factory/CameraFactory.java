package com.theta360.pluginlibrary.factory;

public abstract class CameraFactory {
    public Camera makeCamera() {
        Camera camera = makeAbstractCamera();
        return camera;
    }
    public MediaRecorder makeMediaRecorder() {
        MediaRecorder mediaRecorder  = makeAbstractMediaRecorder();
        return mediaRecorder;
    }

    protected abstract Camera makeAbstractCamera();
    protected abstract MediaRecorder makeAbstractMediaRecorder();
}
