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

package com.theta360.cloudstreaming.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import java.io.IOException;
import timber.log.Timber;

/**
 * Preview camera image
 */
public class CameraPreview {

    private Camera mCamera;
    private int mCameraId;
    private Camera.Parameters mParameters;

    /**
     * Constructor
     */
    public CameraPreview() {
    }

    /**
     * Start preview
     * @param surfaceTexture Surface texture
     */
    public void start(SurfaceTexture surfaceTexture) {
        if (mCamera == null) {
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCameraId = i;
                }
                try {
                    mCamera = Camera.open(mCameraId);
                } catch (Exception e) {
                    Timber.d(e.getMessage());
                }
            }
            mCamera.setErrorCallback(mErrorCallback);
            mParameters = mCamera.getParameters();
            mParameters.setPreviewSize(3840, 1920);
            mParameters.set("RIC_SHOOTING_MODE", "RicStillPreview3840");
            mParameters.set("RIC_PROC_STITCHING", "RicStaticStitching");
            mParameters.set("recording-hint", "false");
            mCamera.setParameters(mParameters);
            mCamera.setDisplayOrientation(0);
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                Timber.d(e.getMessage());
            }
            mCamera.startPreview();
        }
    }

    /**
     * Stop preview
     */
    public void stop() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
            mParameters = null;
        }
    }

    private Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            String cameraError = String.format("Camera error. error=%d", error);
            Timber.e(cameraError);
            throw new RuntimeException(cameraError);
        }
    };

}
