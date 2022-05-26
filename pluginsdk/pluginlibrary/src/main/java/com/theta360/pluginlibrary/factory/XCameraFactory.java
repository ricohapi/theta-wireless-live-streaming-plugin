package com.theta360.pluginlibrary.factory;

public class XCameraFactory extends CameraFactory {

    public Camera makeAbstractCamera() {
        return new XCamera();
    }

    public MediaRecorder makeAbstractMediaRecorder() {
        return new XMediaRecorder();
    }
}
