package com.theta360.pluginlibrary.factory;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

import com.theta360.pluginlibrary.ThetaModelException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XCamera extends Camera {
    private theta360.hardware.Camera mCamera;
    private theta360.hardware.Camera.Parameters mParameters;
    private theta360.hardware.Camera.CameraInfo mCameraInfo;
    private Parameters xParameters;
    private ErrorCallback eCallback;
    private ShutterCallback sCallback;
    private PictureCallback jpCallback;
    private PictureCallback rCallback;
    private PictureCallback pvCallback;
    private PreviewCallback prCallback;
    private byte[] mBuffer;

    /**
     * List<theta360.hardware.Camera.Size>を
     * List<com.theta360.pluginlibrary.factory.Camera.Size>に変換して返却
     */
    private List<Size> conversionCameraSize (List<theta360.hardware.Camera.Size> list) {
        List<Size> returnList = new ArrayList<>();
        FactoryBase factory = new FactoryBase();
        Camera fCamera = factory.abstractCamera(FactoryBase.CameraModel.XCamera);
        theta360.hardware.Camera.Size xCameraSize;
        for (int i = 1; i < list.size(); i++) {
            xCameraSize = list.get(i);
            Size size = fCamera.new Size(0, 0);
            size.height = xCameraSize.height;
            size.width = xCameraSize.width;
            returnList.add(size);
        }
        return  returnList;
    }

    public class XParameters extends Parameters {

        @Override
        public String get(String key) {
            return mParameters.get(key);
        }

        @Override
        public void set(String key, String value) {
            mParameters.set(key, value);
        }

        @Override
        public void set(String key, int value) {
            mParameters.set(key, value);
        }

        @Override
        public void setPreviewSize(int width, int height) {
            mParameters.setPreviewSize(width, height);
        }

        @Override
        public void setPictureSize(int width, int height) {
            mParameters.setPictureSize(width, height);
        }

        @Override
        public int getPictureSizeWidth() {
            return mParameters.getPictureSize().width;
        }

        @Override
        public int getPictureSizeHeight() {
            return mParameters.getPictureSize().height;
        }

        @Override
        public void setJpegThumbnailSize(int width, int height) {
            mParameters.setJpegThumbnailSize(width, height);
        }

        @Override
        public void setExposureCompensation(int value) {
            mParameters.setExposureCompensation(value);
        }

        @Override
        public int getExposureCompensation() {
            return mParameters.getExposureCompensation();
        }

        @Override
        public int getMaxExposureCompensation() {
            return mParameters.getMaxExposureCompensation();
        }

        @Override
        public int getMinExposureCompensation() {
            return mParameters.getMinExposureCompensation();
        }

        @Override
        public int getMaxZoom() {
            return mParameters.getMaxZoom();
        }

        @Override
        public void setZoom(int var1) {
            mParameters.setZoom(var1);
        }

        @Override
        public int getZoom() {
            return mParameters.getZoom();
        }

        @Override
        public List<Size> getSupportedPreviewSizes() {
            List<theta360.hardware.Camera.Size> list = mParameters.getSupportedPreviewSizes();
            return conversionCameraSize(list);
        }

        @Override
        public List<int[]> getSupportedPreviewFpsRange() {
            return mParameters.getSupportedPreviewFpsRange();
        }

        @Override
        public void setPreviewFormat(int var1) {
            mParameters.setPreviewFormat(var1);
        }

        @Override
        public List<Integer> getSupportedPreviewFormats() {
            return mParameters.getSupportedPreviewFormats();
        }

        @Override
        public void setPreviewFrameRate(int value) {
            mParameters.setPreviewFrameRate(value);
        }

        @Override
        public void setPreviewFpsRange(int min, int max) {
            mParameters.setPreviewFpsRange(min, max);
        }

        @Override
        public List<String> getSupportedFlashModes() {
            return mParameters.getSupportedFlashModes();
        }

        @Override
        public void setFlashMode(String var1) {
            mParameters.setFlashMode(var1);
        }

        @Override
        public void setRecordingHint(boolean hint) {
            mParameters.setRecordingHint(hint);
        }

        @Override
        public boolean isVideoStabilizationSupported() {
            return mParameters.isVideoStabilizationSupported();
        }

        @Override
        public void setVideoStabilization(boolean toggle) {
            mParameters.setVideoStabilization(toggle);
        }

        @Override
        public boolean isZoomSupported() {
            return mParameters.isZoomSupported();
        }
    }

    public class XCameraInfo extends CameraInfo {
        @Override
        public int facing() {
            return mCameraInfo.facing;
        }
    }

    private theta360.hardware.Camera.ErrorCallback mErrorCallback = new theta360.hardware.Camera.ErrorCallback() {
        @Override
        public void onError(int error, theta360.hardware.Camera camera) {
            eCallback.onError(error,camera);
        }
    };
    private theta360.hardware.Camera.ShutterCallback mShutterCallback = new theta360.hardware.Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            sCallback.onShutter();
        }

        @Override
        public void onLongShutter() {
            sCallback.onLongShutter();
        }

        @Override
        public void onShutterend() {
            sCallback.onShutterend();
        }
    };
    private theta360.hardware.Camera.PictureCallback jpegPictureCallback = new theta360.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, theta360.hardware.Camera camera) {
            jpCallback.onPictureTaken(data, camera);
        }
    };
    private theta360.hardware.Camera.PictureCallback rawPictureCallback = new theta360.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, theta360.hardware.Camera camera) {
            if (rCallback != null) {
                rCallback.onPictureTaken(data, camera);
            }
        }
    };
    private theta360.hardware.Camera.PictureCallback postViewPictureCallback = new theta360.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, theta360.hardware.Camera camera) {
            if (pvCallback != null) {
                pvCallback.onPictureTaken(data, camera);
            }
        }
    };
    private theta360.hardware.Camera.PreviewCallback previewCallback = new theta360.hardware.Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, theta360.hardware.Camera camera) {
            prCallback.onPreviewFrame(data, camera);
        }
    };


    @Override
    public int getNumberOfCameras() {
        return theta360.hardware.Camera.getNumberOfCameras();
    }

    @Override
    public void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
        try {
            throw new ThetaModelException(getClass().getName()+  " getCameraInfo() VCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getCameraInfo(Context context, int cameraId, CameraInfo cameraInfo) {
        theta360.hardware.Camera.getCameraInfo(context, cameraId, mCameraInfo);
    }

    @Override
    public void open() {
        try {
            throw new ThetaModelException(getClass().getName()+  " open() VCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void open(int cameraId) {
        try {
            throw new ThetaModelException(getClass().getName()+  " open(int) VCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void open(Context context) {
        if(mCamera == null)
            mCamera = theta360.hardware.Camera.open(context);
    }

    @Override
    public void open(Context context, int cameraId) {
        if(mCamera == null)
            mCamera = theta360.hardware.Camera.open(context, 2);
    }

    @Override
    public boolean isCameraNullCheck() {
        if(mCamera == null) {
            return true;
        }
        return false;
    }

    @Override
    public void initializationCamera() {
        if (mCamera != null) {
            mCamera = null;
        }
    }

    @Override
    public void reconnect() throws IOException {
        mCamera.reconnect();
    }

    @Override
    public void setPreviewTexture(SurfaceTexture var1) throws IOException {
        mCamera.setPreviewTexture(var1);
    }

    @Override
    public void setOneShotPreviewCallback(PreviewCallback cb) {
        prCallback = cb;
        mCamera.setOneShotPreviewCallback(previewCallback);
    }

    @Override
    public void setPreviewCallbackWithBuffer(PreviewCallback cb) {
        prCallback = cb;
        mCamera.setPreviewCallbackWithBuffer(previewCallback);
    }

    @Override
    public void addCallbackBuffer(byte[] callbackBuffer) {
        mCamera.addCallbackBuffer(callbackBuffer);
    }

    @Override
    public void setErrorCallback(ErrorCallback errorCallback) {
        if (errorCallback != null) {
            mCamera.setErrorCallback(mErrorCallback);
        } else {
            mCamera.setErrorCallback(null);
        }
    }

    @Override
    public Parameters getParameters() {
        if(xParameters == null) {
            xParameters = new XParameters();
        }
        mParameters = mCamera.getParameters();
        return xParameters;
    }

    @Override
    public CameraInfo getNewCameraInfo() {
        CameraInfo cameraInfo = new XCameraInfo();
        mCameraInfo = new theta360.hardware.Camera.CameraInfo();
        return cameraInfo;
    }

    @Override
    public void setParameters() {
        if (mParameters == null) {
            mParameters = mCamera.getParameters();
        }
        mCamera.setParameters(mParameters);
    }

    @Override
    public int getLcdBrightness() {
        return mCamera.getLcdBrightness();
    }

    @Override
    public int getLedPowerBrightness(int ledId) {
        return mCamera.getLedPowerBrightness(ledId);
    }

    @Override
    public int getLedStatusBrightness(int ledId) {
        return mCamera.getLedStatusBrightness(ledId);
    }

    @Override
    public void ctrlLcdBrightness(int brightness) {
        mCamera.ctrlLcdBrightness(brightness);
    }

    @Override
    public void ctrlLedPowerBrightness(int ledId, int brightness) {
        mCamera.ctrlLedPowerBrightness(ledId, brightness);
    }

    @Override
    public void ctrlLedStatusBrightness(int ledId, int brightness) {
        mCamera.ctrlLedStatusBrightness(ledId, brightness);
    }

    @Override
    public void startPreview() {
        mCamera.startPreview();
    }

    @Override
    public void stopPreview() {
        mCamera.stopPreview();
    }

    @Override
    public void setPreviewCallback(PreviewCallback cb) {
        prCallback = cb;
        mCamera.setPreviewCallback(previewCallback);
    }

    @Override
    public void release() {
        mCamera.release();
    }

    @Override
    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
    }

    @Override
    public void takePicture(ShutterCallback onShutterCallback, PictureCallback raw, PictureCallback onJpegPictureCallback) {
        sCallback = onShutterCallback;
        rCallback = raw;
        jpCallback = onJpegPictureCallback;
        if(mCamera != null) {
            mCamera.takePicture(mShutterCallback, rawPictureCallback, jpegPictureCallback);
        }
    }

    @Override
    public void takePicture(ShutterCallback onShutterCallback, PictureCallback raw, PictureCallback postView, PictureCallback onJpegPictureCallback) {
        sCallback = onShutterCallback;
        rCallback = raw;
        pvCallback = postView;
        jpCallback = onJpegPictureCallback;
        if(mCamera != null) {
            mCamera.takePicture(mShutterCallback, rawPictureCallback, postViewPictureCallback, jpegPictureCallback);
        }
    }

    @Override
    public void setDisplayOrientation(int var1) {
        mCamera.setDisplayOrientation(var1);
    }

    @Override
    public void lock() {
        mCamera.lock();
    }

    @Override
    public void unlock() {
        mCamera.unlock();
    }

    @Override
    public theta360.hardware.Camera getXCamera() {
        return mCamera;
    }

    @Override
    public android.hardware.Camera getVCamera() {
        try {
            throw new ThetaModelException(getClass().getName()+  " getVCamera() VCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
}
