package com.pedro.encoder.input.video;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import com.theta360.pluginlibrary.activity.ThetaInfo;
import com.theta360.pluginlibrary.factory.Camera;
import com.theta360.pluginlibrary.factory.Camera.Parameters;
import theta360.media.CamcorderProfile;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;

import com.pedro.encoder.Frame;
import com.theta360.pluginlibrary.factory.FactoryBase;
import com.theta360.pluginlibrary.values.ThetaModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pedro on 20/01/17.
 *
 * This class need use same resolution, fps and imageFormat that VideoEncoder
 * Tested with YV12 and NV21.
 * <p>
 * Advantage = you can control fps of the stream.
 * Disadvantages = you cant use all resolutions, only resolution that your camera support.
 * <p>
 * If you want use all resolutions. You can use libYuv for resize images in OnPreviewFrame:
 * https://chromium.googlesource.com/libyuv/libyuv/
 */

public class Camera1ApiManager implements Camera.PreviewCallback {

  private String TAG = "Camera1ApiManager";
  private FactoryBase factory;
  private Camera camera = null;
  private Camera.CameraInfo info = null;
  private SurfaceView surfaceView;
  private TextureView textureView;
  private SurfaceTexture surfaceTexture;
  private GetCameraData getCameraData;
  private boolean running = false;
  private boolean lanternEnable = false;
  private boolean videoStabilizationEnable = false;
  private int cameraSelect;
  private CameraHelper.Facing facing = CameraHelper.Facing.BACK;
  private boolean isPortrait = false;
  private Context context;

  //default parameters for camera
  private int width = 640;
  private int height = 480;
  private int fps = 30;
  private int rotation = 0;
  private int imageFormat = ImageFormat.NV21;
  private byte[] yuvBuffer;
  private List<Camera.Size> previewSizeBack;
  private List<Camera.Size> previewSizeFront;
  private float distance;
  private CameraCallbacks cameraCallbacks;
  private final int focusAreaSize = 100;

  private final int sensorOrientation = 0;
  //Value obtained from Camera.Face documentation api about bounds
  private final Rect faceSensorScale = new Rect(-1000, -1000, 1000, 1000);

  public Camera1ApiManager(SurfaceView surfaceView, GetCameraData getCameraData) {
    this.surfaceView = surfaceView;
    this.getCameraData = getCameraData;
    this.context = surfaceView.getContext();
    init();
  }

  public Camera1ApiManager(TextureView textureView, GetCameraData getCameraData) {
    this.textureView = textureView;
    this.getCameraData = getCameraData;
    this.context = textureView.getContext();
    init();
  }

  public Camera1ApiManager(SurfaceTexture surfaceTexture, Context context) {
    this.surfaceTexture = surfaceTexture;
    this.context = context;
    init();
  }

  private void init() {
    //TODO Factory Cameraクラスインスタンス生成
    factory = new FactoryBase();
    if(ThetaModel.isVCameraModel()) {
      camera = factory.abstractCamera(FactoryBase.CameraModel.VCamera);
    } else {
      camera = factory.abstractCamera(FactoryBase.CameraModel.XCamera);
    }
    cameraSelect = selectCameraBack();
    previewSizeBack = getPreviewSize();
    cameraSelect = selectCameraFront();
    previewSizeFront = getPreviewSize();
  }

  public void setRotation(int rotation) {
    this.rotation = rotation;
  }

  public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
    this.surfaceTexture = surfaceTexture;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public void setCameraFacing(CameraHelper.Facing cameraFacing) {
    facing = cameraFacing;
  }

  public void start(CameraHelper.Facing cameraFacing, int width, int height, int fps) {
    int facing = cameraFacing == CameraHelper.Facing.BACK ? Camera.CameraInfo.CAMERA_FACING_BACK
            : Camera.CameraInfo.CAMERA_FACING_FRONT;
    this.width = width;
    this.height = height;
    this.fps = fps;
    cameraSelect =
            facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK ? selectCameraBack() : selectCameraFront();
    start();
  }

  public void start(int width, int height, int fps) {
    start(facing, width, height, fps);
  }

  private void start() {
    if (!checkCanOpen()) {
      throw new CameraOpenException("This camera resolution cant be opened");
    }
    yuvBuffer = new byte[width * height * 3 / 2];
    try {
      if (ThetaModel.isVCameraModel()){
        camera.open(cameraSelect);
        info = camera.getNewCameraInfo();
        camera.getCameraInfo(cameraSelect, info);
      } else {
        camera.open(context, 2);
        info = camera.getNewCameraInfo();
        camera.getCameraInfo(context, cameraSelect, info);
      }
      facing = info.facing() == Camera.CameraInfo.CAMERA_FACING_FRONT ? CameraHelper.Facing.FRONT : CameraHelper.Facing.BACK;
      isPortrait = context.getResources().getConfiguration().orientation
              == Configuration.ORIENTATION_PORTRAIT;
      Camera.Parameters parameters = camera.getParameters();
      //TODO 天頂補正ON FW対応後、コメント解除
//      if(!ThetaModel.isVCameraModel()) {
//        parameters.set("RIC_PROC_ZENITH_CORRECTION", "RicZenithCorrectionOnAuto");
//      }
      String version = ThetaInfo.getThetaFirmwareVersion(context);
      if(!ThetaModel.isVCameraModel() && version.compareTo("1.20.0") >= 0){
        //THETA X fw1.20 supports 16:9 preview mode
        parameters.setPreviewSize(width, height);
        parameters.setPreviewFormat(imageFormat);
        if(fps == 15) {
          parameters.setPreviewFrameRate(0);  //THETA X fw1.20 15fps mode
        } else {
          parameters.setPreviewFrameRate(30);  //THETA X fw1.20 30fps mode
        }
        camera.setParameters();
        camera.setDisplayOrientation(rotation);
        if (surfaceView != null) {
          camera.setPreviewDisplay(surfaceView.getHolder());
        } else if (textureView != null) {
          camera.setPreviewTexture(textureView.getSurfaceTexture());
        } else {
          camera.setPreviewTexture(surfaceTexture);
        }
      } else {
        height = (int) (width * 5 / 10);  //not supports 16:9 preview mode
        parameters.setPreviewSize(width, height);
        parameters.setPreviewFormat(imageFormat);
        List<int[]> list = new ArrayList<>();
        int[] i = {1000, 2000, 3000, 4000, 5000, 6000, 8000, 10000, 15000, 20000, 24000, 30000};
        list.add(i);
        //      List<int[]> list = parameters.getSupportedPreviewFpsRange();
        int[] range = adaptFpsRange(fps, list);
        parameters.setPreviewFpsRange(range[0], range[1]);
        camera.setParameters();
        camera.setDisplayOrientation(rotation);
        if (surfaceView != null) {
          camera.setPreviewDisplay(surfaceView.getHolder());
          camera.addCallbackBuffer(yuvBuffer);
          camera.setPreviewCallbackWithBuffer(this);
        } else if (textureView != null) {
          camera.setPreviewTexture(textureView.getSurfaceTexture());
          camera.addCallbackBuffer(yuvBuffer);
          camera.setPreviewCallbackWithBuffer(this);
        } else {
          camera.setPreviewTexture(surfaceTexture);
        }
      }
      camera.startPreview();
      running = true;
      if (cameraCallbacks != null) {
        cameraCallbacks.onCameraChanged(facing);
      }
      Log.i(TAG, width + "X" + height);
    } catch (IOException e) {
      if (cameraCallbacks != null) cameraCallbacks.onCameraError(e.getMessage());
      Log.e(TAG, "Error", e);
    }
  }

  public void setPreviewOrientation(final int orientation) {
    this.rotation = orientation;
    if (!camera.isCameraNullCheck() && running) {
      camera.stopPreview();
      camera.setDisplayOrientation(orientation);
      camera.startPreview();
    }
  }

  public void setZoom(MotionEvent event) {
    try {
      if (!camera.isCameraNullCheck() && running && camera.getParameters() != null && camera.getParameters()
              .isZoomSupported()) {
        Camera.Parameters params = camera.getParameters();
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = CameraHelper.getFingerSpacing(event);

        if (newDist > distance) {
          if (zoom < maxZoom) {
            zoom++;
          }
        } else if (newDist < distance) {
          if (zoom > 0) {
            zoom--;
          }
        }

        distance = newDist;
        params.setZoom(zoom);
        camera.setParameters();
      }
    } catch (Exception e) {
      Log.e(TAG, "Error", e);
    }
  }

  public void setExposure(int value) {
    if (!camera.isCameraNullCheck() && camera.getParameters() != null) {
      Camera.Parameters params = camera.getParameters();
      if (value > params.getMaxExposureCompensation()) value = params.getMaxExposureCompensation();
      else if (value < params.getMinExposureCompensation()) value = params.getMinExposureCompensation();
      params.setExposureCompensation(value);
      camera.setParameters();
    }
  }

  public int getExposure() {
    if (!camera.isCameraNullCheck() && camera.getParameters() != null) {
      Camera.Parameters params = camera.getParameters();
      return params.getExposureCompensation();
    }
    return 0;
  }

  public int getMaxExposure() {
    if (!camera.isCameraNullCheck() && camera.getParameters() != null) {
      Camera.Parameters params = camera.getParameters();
      return params.getMaxExposureCompensation();
    }
    return 0;
  }

  public int getMinExposure() {
    if (!camera.isCameraNullCheck() && camera.getParameters() != null) {
      Camera.Parameters params = camera.getParameters();
      return params.getMinExposureCompensation();
    }
    return 0;
  }

  private int selectCameraBack() {
    return selectCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
  }

  private int selectCameraFront() {
    return selectCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
  }

  private int selectCamera(int facing) {
    int number = camera.getNumberOfCameras();
    for (int i = 0; i < number; i++) {
      info = camera.getNewCameraInfo();
      if (ThetaModel.isVCameraModel()){
        camera.getCameraInfo(i, info);
      } else {
        camera.getCameraInfo(context, i, info);
      }
      if (info.facing() == facing) return i;
    }
    return 0;
  }

  public void stopCamera() {
    if (!camera.isCameraNullCheck()) {
      camera.release();
      camera.initializationCamera();
    }
  }

  public void stop() {
    if (!camera.isCameraNullCheck()) {
      camera.stopPreview();
      camera.setPreviewCallback(null);
      camera.setPreviewCallbackWithBuffer(null);
      camera.release();
      camera.initializationCamera();
    }
    running = false;
  }

  public int getLcdBrightness() {
    return camera.getLcdBrightness();
  }

  public int getLedPowerBrightness(int ledId) {
    return camera.getLedPowerBrightness(ledId);
  }

  public int getLedStatusBrightness(int ledId) {
    return camera.getLedStatusBrightness(ledId);
  }

  public void ctrlLcdBrightness(int brightness) {
    camera.ctrlLcdBrightness(brightness);
  }

  public void ctrlLedPowerBrightness(int ledId, int brightness) {
    camera.ctrlLedPowerBrightness(ledId, brightness);
  }

  public void ctrlLedStatusBrightness(int ledId, int brightness) {
    camera.ctrlLedStatusBrightness(ledId, brightness);
  }

  public boolean isRunning() {
    return running;
  }

  private int[] adaptFpsRange(int expectedFps, List<int[]> fpsRanges) {
    expectedFps *= 1000;
    int[] closestRange = fpsRanges.get(0);
    int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
    for (int[] range : fpsRanges) {
      if (range[0] <= expectedFps && range[1] >= expectedFps) {
        int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
        if (curMeasure < measure) {
          closestRange = range;
          measure = curMeasure;
        }
      }
    }
    return closestRange;
  }

  @Override
  public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
    getCameraData.inputYUVData(new Frame(data, rotation, facing == CameraHelper.Facing.FRONT && isPortrait, imageFormat));
    camera.addCallbackBuffer(yuvBuffer);
  }

  @Override
  public void onPreviewFrame(byte[] data, theta360.hardware.Camera camera) {
    getCameraData.inputYUVData(new Frame(data, rotation, facing == CameraHelper.Facing.FRONT && isPortrait, imageFormat));
    camera.addCallbackBuffer(yuvBuffer);
  }

  /**
   * See: https://developer.android.com/reference/android/graphics/ImageFormat.html to know name of
   * constant values
   * Example: 842094169 -> YV12, 17 -> NV21
   */
  public List<Integer> getCameraPreviewImageFormatSupported() {
    List<Integer> formats;
    if (!camera.isCameraNullCheck()) {
      formats = camera.getParameters().getSupportedPreviewFormats();
      for (Integer i : formats) {
        Log.i(TAG, "camera format supported: " + i);
      }
    } else {
      if (ThetaModel.isVCameraModel()){
        camera.open(cameraSelect);
      } else {
        camera.open(context, 2);
      }
      formats = camera.getParameters().getSupportedPreviewFormats();
      camera.release();
//      camera = null;
    }
    return formats;
  }

  private List<Camera.Size> getPreviewSize() {
    List<Camera.Size> previewSizes;
//    Camera.Size maxSize;
    if (ThetaModel.isVCameraModel()){
      camera.open(cameraSelect);
    } else {
      camera.open(context, 2);
    }
//    maxSize = getMaxEncoderSizeSupported();
    //TODO getSupportedPreviewSizes()が正しく動くまでの暫定処置
    previewSizes = camera.getParameters().getSupportedPreviewSizes();
    Camera.Size size1 = camera.new Size(3840, 1920);
    Camera.Size size2 = camera.new Size(1920, 960);
    Camera.Size size3 = camera.new Size(1024, 512);
    previewSizes.add(size1);
    previewSizes.add(size2);
    previewSizes.add(size3);
    return previewSizes;
  }

  public List<Camera.Size> getPreviewSizeBack() {
    return previewSizeBack;
  }

  public List<Camera.Size> getPreviewSizeFront() {
    return previewSizeFront;
  }

  /**
   * @return max size that device can record.
   */
  private Camera.Size getMaxEncoderSizeSupported() {
    if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
      return camera.new Size(5760, 2880);
    } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_4K)) {
      return camera.new Size(3840, 1920);
    } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_2K)) {
      return camera.new Size(1920, 960);
    } else {
      return camera.new Size(1024, 512);
    }
  }

  public CameraHelper.Facing getCameraFacing() {
    return facing;
  }

  public void switchCamera() throws CameraOpenException {
    if (!camera.isCameraNullCheck()) {
      int oldCamera = cameraSelect;
      int number = camera.getNumberOfCameras();
      for (int i = 0; i < number; i++) {
        if (cameraSelect != i) {
          cameraSelect = i;
          if (!checkCanOpen()) {
            cameraSelect = oldCamera;
            throw new CameraOpenException("This camera resolution cant be opened");
          }
          stop();
          start();
          return;
        }
      }
    }
  }

  private boolean checkCanOpen() {
    List<Camera.Size> previews;
    if (cameraSelect == selectCameraBack()) {
      previews = previewSizeBack;
    } else {
      previews = previewSizeFront;
    }
    for (Camera.Size size : previews) {

      if (size.width == width ) {
//      if (size.width == width && size.height == height) {
        return true;
      }
    }
    return false;
  }

  public boolean isLanternEnabled() {
    return lanternEnable;
  }

  /**
   * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
   */
  public void enableLantern() throws Exception {
    if (!camera.isCameraNullCheck()) {
      Camera.Parameters parameters = camera.getParameters();
      List<String> supportedFlashModes = parameters.getSupportedFlashModes();
      if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
        if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
          parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
          camera.setParameters();
          lanternEnable = true;
        } else {
          Log.e(TAG, "Lantern unsupported");
          throw new Exception("Lantern unsupported");
        }
      }
    }
  }

  public List<int[]> getSupportedFps() {
    List<int[]> supportedFps = new ArrayList<>();
    int[] i = {1000, 2000, 3000, 4000, 5000, 6000, 8000, 10000, 15000, 20000, 24000, 30000};
    if (!camera.isCameraNullCheck()) {
      supportedFps.add(i);
//      supportedFps = camera.getParameters().getSupportedPreviewFpsRange();
    } else {
      if (ThetaModel.isVCameraModel()){
        camera.open(cameraSelect);
      } else {
        camera.open(context, 2);
      }
      supportedFps.add(i);
//      supportedFps = camera.getParameters().getSupportedPreviewFpsRange();
      camera.release();
//      camera = null;
    }
    for (int[] range : supportedFps) {
      range[0] /= 1000;
      range[1] /= 1000;
    }
    return supportedFps;
  }

  /**
   * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
   */
  public void disableLantern() {
    if (!camera.isCameraNullCheck()) {
      Camera.Parameters parameters = camera.getParameters();
      parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
      camera.setParameters();
      lanternEnable = false;
    }
  }

  public void enableRecordingHint() {
    if (!camera.isCameraNullCheck()) {
      Camera.Parameters parameters = camera.getParameters();
      parameters.setRecordingHint(true);
      camera.setParameters();
    }
  }

  public void disableRecordingHint() {
    if (!camera.isCameraNullCheck()) {
      Camera.Parameters parameters = camera.getParameters();
      parameters.setRecordingHint(false);
      camera.setParameters();
    }
  }

  public boolean enableVideoStabilization() {
    if (!camera.isCameraNullCheck()) {
      Camera.Parameters parameters = camera.getParameters();
      if (parameters.isVideoStabilizationSupported()) {
        parameters.setVideoStabilization(true);
        videoStabilizationEnable = true;
      }
    }
    return videoStabilizationEnable;
  }

  public void disableVideoStabilization() {
    if (!camera.isCameraNullCheck()) {
      Camera.Parameters parameters = camera.getParameters();
      if (parameters.isVideoStabilizationSupported()) {
        parameters.setVideoStabilization(false);
        videoStabilizationEnable = false;
      }
    }
  }

  public boolean isVideoStabilizationEnabled() {
    return videoStabilizationEnable;
  }

  public void setCameraCallbacks(CameraCallbacks cameraCallbacks) {
    this.cameraCallbacks = cameraCallbacks;
  }

  private Rect calculateFocusArea(float x, float y, float previewWidth, float previewHeight) {
    int left = clamp((int) (x / previewWidth * 2000f - 1000f), focusAreaSize);
    int top = clamp((int) (y / previewHeight * 2000f - 1000f), focusAreaSize);
    return new Rect(left, top, left + focusAreaSize, top + focusAreaSize);
  }

  private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
    int result;
    if (Math.abs(touchCoordinateInCameraReper) + focusAreaSize / 2 > 1000){
      if (touchCoordinateInCameraReper > 0){
        result = 1000 - focusAreaSize / 2;
      } else {
        result = -1000 + focusAreaSize / 2;
      }
    } else{
      result = touchCoordinateInCameraReper - focusAreaSize / 2;
    }
    return result;
  }
}