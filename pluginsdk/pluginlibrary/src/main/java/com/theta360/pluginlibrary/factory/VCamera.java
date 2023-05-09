/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.pluginlibrary.factory;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

import com.theta360.pluginlibrary.ThetaModelException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VCamera extends Camera {
    private android.hardware.Camera mCamera;
    private android.hardware.Camera.Parameters mParameters;
    private android.hardware.Camera.CameraInfo mCameraInfo;
    private Parameters vParameters;
    private ErrorCallback eCallback;
    private ShutterCallback sCallback;
    private PictureCallback jpCallback;
    private PictureCallback rCallback;
    private PictureCallback pvCallback;
    private PreviewCallback prCallback;
    private byte[] mBuffer;


    /**
     * List<android.hardware.Camera.Size>を
     * List<com.theta360.pluginlibrary.factory.Camera.Size>に変換して返却
     */
    private List<Size> conversionCameraSize (List<android.hardware.Camera.Size> list) {
        List<Size> returnList = new ArrayList<>();
        FactoryBase factory = new FactoryBase();
        Camera fCamera = factory.abstractCamera(FactoryBase.CameraModel.VCamera);
        android.hardware.Camera.Size vCameraSize;
        for (int i = 1; i < list.size(); i++) {
            vCameraSize = list.get(i);
            Size size = fCamera.new Size(0, 0);
            size.height = vCameraSize.height;
            size.width = vCameraSize.width;
            returnList.add(size);
        }
        return  returnList;
    }

    public class VParameters extends Parameters {
        @Override
        public String get(String key) {
            return mParameters.get(key);
        }

        @Override
        public void set(String key, String value) {
            if(mParameters == null){
                mParameters = mCamera.getParameters();
            }
            mParameters.set(key, value);
        }

        @Override
        public void set(String key, int value) {
            if(mParameters == null){
                mParameters = mCamera.getParameters();
            }
            mParameters.set(key, value);
        }

        @Override
        public void setPreviewSize(int width, int height) {
            mParameters.setPreviewSize(width, height);
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
        public void setPictureSize(int width, int height) {
            mParameters.setPictureSize(width, height);
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
            List<android.hardware.Camera.Size> list = mParameters.getSupportedPreviewSizes();
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

    public class VCameraInfo extends CameraInfo {
        @Override
        public int facing() {
            return mCameraInfo.facing;
        }
    }

    private android.hardware.Camera.ErrorCallback mErrorCallback = new android.hardware.Camera.ErrorCallback() {
        @Override
        public void onError(int error, android.hardware.Camera camera) {
            eCallback.onError(error, camera);
        }
    };
    private android.hardware.Camera.ShutterCallback mShutterCallback = new android.hardware.Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            sCallback.onShutter();
        }
    };
    private android.hardware.Camera.PictureCallback jpegPictureCallback = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            jpCallback.onPictureTaken(data, camera);
        }
    };
    private android.hardware.Camera.PictureCallback rawPictureCallback = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            if (rCallback != null) {
                rCallback.onPictureTaken(data, camera);
            }
        }
    };
    private android.hardware.Camera.PictureCallback postViewPictureCallback = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            if (pvCallback != null) {
                pvCallback.onPictureTaken(data, camera);
            }
        }
    };
    private android.hardware.Camera.PreviewCallback previewCallback = new android.hardware.Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
            prCallback.onPreviewFrame(data, camera);
        }
    };

    @Override
    public int getNumberOfCameras() {
        return android.hardware.Camera.getNumberOfCameras();
    }

    @Override
    public void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
        android.hardware.Camera.getCameraInfo(cameraId, mCameraInfo);
    }

    @Override
    public void getCameraInfo(Context context, int cameraId, CameraInfo cameraInfo) {
        try {
            throw new ThetaModelException(getClass().getName()+  " getCameraInfo(Context, int, theta360.hardware.Camera.CameraInfo) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void open() {
        if (mCamera == null)
            mCamera = android.hardware.Camera.open();
    }

    @Override
    public void open(int cameraId) {
        if (mCamera == null)
            mCamera = android.hardware.Camera.open(cameraId);
    }

    @Override
    public void open(Context context) {
        try {
            throw new ThetaModelException(getClass().getName()+  " open(Context) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void open(Context context, int cameraId) {
        try {
            throw new ThetaModelException(getClass().getName()+  " open(Context, int) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        if(vParameters == null) {
            vParameters = new VParameters();
        }
        mParameters = mCamera.getParameters();
        return vParameters;
    }

    @Override
    public CameraInfo getNewCameraInfo() {
        CameraInfo cameraInfo = new VCameraInfo();
        mCameraInfo = new android.hardware.Camera.CameraInfo();
        return cameraInfo;
    }

    @Override
    public void setParameters() {
        mCamera.setParameters(mParameters);
    }

    @Override
    public int getLcdBrightness() {
        try {
            throw new ThetaModelException(getClass().getName()+  " getLcdBrightness() XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getLedPowerBrightness(int ledId) {
        try {
            throw new ThetaModelException(getClass().getName()+  " getLedPowerBrightness(int) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getLedStatusBrightness(int ledId) {
        try {
            throw new ThetaModelException(getClass().getName()+  " getLedStatusBrightness(int) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void ctrlLcdBrightness(int brightness) {
        try {
            throw new ThetaModelException(getClass().getName()+  " ctrlLcdBrightness(int) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void ctrlLedPowerBrightness(int ledId, int brightness) {
        try {
            throw new ThetaModelException(getClass().getName()+  " ctrlLedPowerBrightness(int, int) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void ctrlLedStatusBrightness(int ledId, int brightness) {
        try {
            throw new ThetaModelException(getClass().getName()+  " ctrlLedStatusBrightness(int, int) XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
            throw new ThetaModelException(getClass().getName()+  " getXCamera() XCameraOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public android.hardware.Camera getVCamera() {
        return mCamera;
    }

    @Override
    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (
                IOException e) {
            e.printStackTrace();
            close();
        }
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
