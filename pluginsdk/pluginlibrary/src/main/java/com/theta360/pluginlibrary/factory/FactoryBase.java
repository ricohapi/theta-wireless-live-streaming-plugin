package com.theta360.pluginlibrary.factory;

public class FactoryBase {

    CameraFactory xCameraFactory = new XCameraFactory();
    CameraFactory vCameraFactory = new VCameraFactory();

    public enum CameraModel {
        XCamera, VCamera, Default
    }

    public Camera abstractCamera(CameraModel cameraModel) {
        switch (cameraModel) {
            case XCamera:
                return xCameraFactory.makeCamera();
            case VCamera:
                return vCameraFactory.makeCamera();
        }
        return null;
    }

    public MediaRecorder abstractMediaRecorder(CameraModel cameraModel) {
        switch (cameraModel) {
            case XCamera:
                return xCameraFactory.makeMediaRecorder();
            case VCamera:
                return vCameraFactory.makeMediaRecorder();
        }
        return null;
    }
}
