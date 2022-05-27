package com.theta360.pluginlibrary.factory;

public class VCameraFactory extends CameraFactory {

    public Camera makeAbstractCamera() {
        return new VCamera();
    }

    public MediaRecorder makeAbstractMediaRecorder() {
        return new VMediaRecorder();
    }
}
