
package com.theta360.pluginlibrary.factory;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public abstract class Camera {
    public abstract int getNumberOfCameras();
    public abstract void getCameraInfo(int cameraId, CameraInfo cameraInfo);
    public abstract void getCameraInfo(Context context, int cameraId, CameraInfo cameraInfo);

    public abstract void open();
    public abstract void open(int cameraId);
    public abstract void open(Context context);
    public abstract void open(Context context, int cameraId);
    public abstract boolean isCameraNullCheck();
    public abstract void initializationCamera();
    public abstract void reconnect() throws IOException;
    public abstract void setPreviewTexture(SurfaceTexture var1) throws IOException;
    public abstract void setOneShotPreviewCallback(PreviewCallback cb);
    public abstract void setPreviewCallbackWithBuffer(PreviewCallback cb);
    public abstract void addCallbackBuffer(byte[] callbackBuffer);
    public abstract void stopPreview();
    public abstract void setPreviewCallback(PreviewCallback cb);
    public abstract void release();
    public abstract void lock();
    public abstract void unlock();
    public abstract void setPreviewDisplay(SurfaceHolder surfaceHolder);
    public abstract void startPreview();
    public abstract void takePicture(ShutterCallback onShutterCallback, PictureCallback raw, PictureCallback onJpegPictureCallback);
    public abstract void takePicture(ShutterCallback onShutterCallback, PictureCallback raw, PictureCallback postView, PictureCallback onJpegPictureCallback);
    public abstract void setDisplayOrientation(int var1);
    public abstract void setErrorCallback(Camera.ErrorCallback errorCallback);
    public abstract void setParameters();
    public abstract int getLcdBrightness();
    public abstract int getLedPowerBrightness(int ledId);
    public abstract int getLedStatusBrightness(int ledId);
    public abstract void ctrlLcdBrightness(int brightness);
    public abstract void ctrlLedPowerBrightness(int ledId, int brightness);
    public abstract void ctrlLedStatusBrightness(int ledId, int brightness);
    public abstract Parameters getParameters();
    public abstract CameraInfo getNewCameraInfo();

    public abstract theta360.hardware.Camera getXCamera();
    public abstract android.hardware.Camera getVCamera();

    public class Size {
        public int width;
        public int height;

        public Size(int tmpW, int tmpH) {
            this.width = tmpW;
            this.height = tmpH;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof theta360.hardware.Camera.Size)) {
                return false;
            } else {
                theta360.hardware.Camera.Size tmpS = (theta360.hardware.Camera.Size)obj;
                return this.width == tmpS.width && this.height == tmpS.height;
            }
        }

        public int hashCode() {
            return this.width * 32713 + this.height;
        }
    }

    public abstract static class Parameters {
        public static final String RIC_SHOOTING_MODE = "RIC_SHOOTING_MODE";
        public static final String RIC_EXPOSURE_MODE = "RIC_EXPOSURE_MODE";
        public static final String RIC_EXPOSURE_LOCK = "RIC_EXPOSURE_LOCK";
        public static final String RIC_EXP_METERING_MODE = "RIC_EXP_METERING_MODE";
        public static final String RIC_MANUAL_EXPOSURE_ISO_FRONT = "RIC_MANUAL_EXPOSURE_ISO_FRONT";
        public static final String RIC_MANUAL_EXPOSURE_ISO_REAR = "RIC_MANUAL_EXPOSURE_ISO_REAR";
        public static final String RIC_MANUAL_EXPOSURE_TIME_FRONT = "RIC_MANUAL_EXPOSURE_TIME_FRONT";
        public static final String RIC_MANUAL_EXPOSURE_TIME_REAR = "RIC_MANUAL_EXPOSURE_TIME_REAR";
        public static final String RIC_AEC_MAXISO_STILL = "RIC_AEC_MAXISO_STILL";
        public static final String RIC_AEC_MAXISO_VIDEO = "RIC_AEC_MAXISO_VIDEO";
        public static final String RIC_AEC_MAXISO_PREVIEW = "RIC_AEC_MAXISO_PREVIEW";
        public static final String RIC_FACE_DETECTION = "RIC_FACE_DETECTION";
        public static final String RIC_WB_MODE = "RIC_WB_MODE";
        public static final String RIC_WB_LOCK = "RIC_WB_LOCK";
        public static final String RIC_WB_TEMPERATURE = "RIC_WB_TEMPERATURE";
        public static final String RIC_PROC_STITCHING = "RIC_PROC_STITCHING";
        public static final String RIC_PROC_ZENITH_CORRECTION = "RIC_PROC_ZENITH_CORRECTION";
        public static final String RIC_PROC_ZENITH_MANUAL_PITCH = "RIC_PROC_ZENITH_MANUAL_PITCH";
        public static final String RIC_PROC_ZENITH_MANUAL_ROLL = "RIC_PROC_ZENITH_MANUAL_ROLL";
        public static final String RIC_PROC_ZENITH_MANUAL_YAW = "RIC_PROC_ZENITH_MANUAL_YAW";
        public static final String RIC_ZENITH_DIRECTION = "RIC_ZENITH_DIRECTION";
        public static final String RIC_JPEG_COMP_FILESIZE_ENABLED = "RIC_JPEG_COMP_FILESIZE_ENABLED";
        public static final String RIC_JPEG_COMP_FILESIZE = "RIC_JPEG_COMP_FILESIZE";
        public static final String RIC_TIMESHIFT_MODE = "RIC_TIMESHIFT_MODE";
        public static final String KEY_POWER_MODE = "power_mode";
        public static final String RIC_MONITORING = "RicMonitoring";
        public static final String RIC_PREVIEW_1024 = "RicPreview1024";
        public static final String RIC_PREVIEW_3840 = "RicPreview3840";
        public static final String RIC_PREVIEW_5760 = "RicPreview5760";
        public static final String RIC_STILL_CAPTURE_STD = "RicStillCaptureStd";
        public static final String RIC_STILL_CAPTURE_STD_FRONT = "RicStillCaptureStdFront";
        public static final String RIC_STILL_CAPTURE_STD_REAR = "RicStillCaptureStdRear";
        public static final String RIC_STILL_CAPTURE_MULTIRAWNR = "RicStillCaptureMultiRawNR";
        public static final String RIC_STILL_CAPTURE_MULTIYUVHDR = "RicStillCaptureMultiYuvHdr";
        public static final String RIC_STILL_CAPTURE_MULTIYUVHHHDR = "RicStillCaptureMultiYuvHhHdr";
        public static final String RIC_STILL_CAPTURE_MULTIBRACKET = "RicStillCaptureMultiBracket";
        public static final String RIC_STILL_CAPTURE_BURST = "RicStillCaptureBurst";
        public static final String RIC_MOVIE_RECORDING_1920 = "RicMovieRecording1920";
        public static final String RIC_MOVIE_RECORDING_3840 = "RicMovieRecording3840";
        public static final String RIC_MOVIE_RECORDING_5760 = "RicMovieRecording5760";
        public static final String RIC_MOVIE_RECORDING_7680 = "RicMovieRecording7680";
        public static final String RIC_UVC_3840 = "RicUVC3840";
        public static final String RIC_UVC_5760 = "RicUVC5760";
        public static final String TIMESHIFT_DEFAULT = "TIMESHIFT_DEFAULT";
        public static final String TIMESHIFT_FIRST_FRONT_CAPTURE_START = "TIMESHIFT_FIRST_FRONT_CAPTURE_START";
        public static final String TIMESHIFT_FIRST_REAR_CAPTURE_START = "TIMESHIFT_FIRST_REAR_CAPTURE_START";
        public static final String TIMESHIFT_SECOND_FRONT_CAPTURE_START = "TIMESHIFT_SECOND_FRONT_CAPTURE_START";
        public static final String TIMESHIFT_SECOND_REAR_CAPTURE_START = "TIMESHIFT_SECOND_REAR_CAPTURE_START";
        public static final String RIC_AUTO_EXPOSURE_P = "RicAutoExposureP";
        public static final String RIC_AUTO_EXPOSURE_S = "RicAutoExposureS";
        public static final String RIC_AUTO_EXPOSURE_T = "RicAutoExposureT";
        public static final String RIC_MANUAL_EXPOSURE = "RicManualExposure";
        public static final String RIC_AUTO_EXPOSURE_RAWNR = "RicAutoExposureRawNR";
        public static final String RIC_AUTO_EXPOSURE_YUVHDR = "RicAutoExposureYuvHdr";
        public static final String RIC_AUTO_EXPOSURE_YUVHHHDR = "RicAutoExposureYuvHhHdr";
        public static final String RIC_WB_AUTO = "RicWbAuto";
        public static final String RIC_WB_MANUAL_GAIN = "RicWbManualGain";
        public static final String RIC_WB_PREVIOUS_GAIN = "RicWbPreviousGain";
        public static final String RIC_WB_PREFIX_TEMPERATURE = "RicWbPrefixTemperature";
        public static final String RIC_WB_PREFIX_DAYLIGHT = "RicWbPrefixDaylight";
        public static final String RIC_WB_PREFIX_SHADE = "RicWbPrefixShade";
        public static final String RIC_WB_PREFIX_CLOUDY_DAYLIGHT = "RicWbPrefixCloudyDaylight";
        public static final String RIC_WB_PREFIX_INCANDESCENT = "RicWbPrefixIncandescent";
        public static final String RIC_WB_PREFIX_FLUORESCENT_WW = "RicWbPrefixFluorescentWW";
        public static final String RIC_WB_PREFIX_FLUORESCENT_D = "RicWbPrefixFluorescentD";
        public static final String RIC_WB_PREFIX_FLUORESCENT_N = "RicWbPrefixFluorescentN";
        public static final String RIC_WB_PREFIX_FLUORESCENT_W = "RicWbPrefixFluorescentW";
        public static final String RIC_WB_PREFIX_FLUORESCENT_L = "RicWbPrefixFluorescentL";
        public static final String RIC_WB_AUTO_UNDERWATER = "RicWbAutoUnderwater";
        public static final String RIC_NON_STITCHING = "RicNonStitching";
        public static final String RIC_AUTO_STITCHING = "RicAutoStitching";
        public static final String RIC_STATIC_STITCHING = "RicStaticStitching";
        public static final String RIC_DYNAMIC_STITCHING_SEMIAUTO = "RicDynamicStitchingSemiAuto";
        public static final String RIC_DYNAMIC_STITCHING_AUTO = "RicDynamicStitchingAuto";
        public static final String RIC_DYNAMIC_STITCHING_SAVE = "RicDynamicStitchingSave";
        public static final String RIC_DYNAMIC_STITCHING_LOAD = "RicDynamicStitchingLoad";
        public static final String RIC_STATIC_STITCHING_JIG = "RicStaticStitchingJIG";
        public static final String RIC_ZENITH_CORRECTION_ONAUTO = "RicZenithCorrectionOnAuto";
        public static final String RIC_ZENITH_CORRECTION_APPLY = "RicZenithCorrectionApply";
        public static final String RIC_ZENITH_CORRECTION_ONSEMIAUTO = "RicZenithCorrectionOnSemiAuto";
        public static final String RIC_ZENITH_CORRECTION_ONSAVE = "RicZenithCorrectionOnSave";
        public static final String RIC_ZENITH_CORRECTION_ONLOAD = "RicZenithCorrectionOnLoad";
        public static final String RIC_ZENITH_CORRECTI_ONOFF = "RicZenithCorrectionOff";
        public static final String RIC_ZENITH_CORRECTION_ONMANUAL = "RicZenithCorrectionOnManual";
        private static final String KEY_PREVIEW_SIZE = "preview-size";
        private static final String KEY_LIVEVIEW_SIZE = "liveview-size";
        private static final String KEY_LIVEVIEW_FRAME_RATE = "liveview-frame-rate";
        private static final String KEY_PREVIEW_FORMAT = "preview-format";
        private static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
        private static final String KEY_PREVIEW_FPS_RANGE = "preview-fps-range";
        private static final String KEY_PICTURE_SIZE = "picture-size";
        private static final String KEY_PICTURE_FORMAT = "picture-format";
        private static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
        private static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
        private static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
        private static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
        private static final String KEY_JPEG_QUALITY = "jpeg-quality";
        private static final String KEY_ROTATION = "rotation";
        private static final String KEY_GPS_LATITUDE = "gps-latitude";
        private static final String KEY_GPS_LONGITUDE = "gps-longitude";
        private static final String KEY_GPS_ALTITUDE = "gps-altitude";
        private static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
        private static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
        private static final String KEY_WHITE_BALANCE = "whitebalance";
        private static final String KEY_EFFECT = "effect";
        private static final String KEY_ANTIBANDING = "antibanding";
        private static final String KEY_SCENE_MODE = "scene-mode";
        private static final String KEY_FLASH_MODE = "flash-mode";
        private static final String KEY_FOCUS_MODE = "focus-mode";
        private static final String KEY_FOCUS_AREAS = "focus-areas";
        private static final String KEY_MAX_NUM_FOCUS_AREAS = "max-num-focus-areas";
        private static final String KEY_FOCAL_LENGTH = "focal-length";
        private static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
        private static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
        private static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
        private static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
        private static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
        private static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
        private static final String KEY_AUTO_EXPOSURE_LOCK = "auto-exposure-lock";
        private static final String KEY_AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK = "auto-whitebalance-lock";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
        private static final String KEY_METERING_AREAS = "metering-areas";
        private static final String KEY_MAX_NUM_METERING_AREAS = "max-num-metering-areas";
        private static final String KEY_ZOOM = "zoom";
        private static final String KEY_MAX_ZOOM = "max-zoom";
        private static final String KEY_ZOOM_RATIOS = "zoom-ratios";
        private static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
        private static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
        private static final String KEY_FOCUS_DISTANCES = "focus-distances";
        private static final String KEY_VIDEO_SIZE = "video-size";
        private static final String KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO = "preferred-preview-size-for-video";
        private static final String KEY_MAX_NUM_DETECTED_FACES_HW = "max-num-detected-faces-hw";
        private static final String KEY_MAX_NUM_DETECTED_FACES_SW = "max-num-detected-faces-sw";
        private static final String KEY_RECORDING_HINT = "recording-hint";
        private static final String KEY_VIDEO_SNAPSHOT_SUPPORTED = "video-snapshot-supported";
        private static final String KEY_VIDEO_STABILIZATION = "video-stabilization";
        private static final String KEY_VIDEO_STABILIZATION_SUPPORTED = "video-stabilization-supported";
        private static final String KEY_AI_OFF = "ai-off";
        private static final String KEY_BURST_ENUMERATE = "burst-enumerate";
        private static final String KEY_INTERVAL_TIME_PICNUM = "INTERVAL_TIME_PICNUM";
        private static final String KEY_CAPTURE_GROUPID = "capture-groupid";
        private static final String KEY_STG_INTERFACE_LOW_POWER_MODE = "STG_INTERFACE_LOW_POWER_MODE";
        private static final String KEY_GSP_INFO_WRITE_FLAG = "GSP_INFO_WRITE_FLAG";
        public static final String PLUGIN_INFO = "plugin_info";
        private static final String SUPPORTED_VALUES_SUFFIX = "-values";
        private static final String TRUE = "true";
        private static final String FALSE = "false";
        public static final String COM_THETA360_RECEPTOR = "com.theta360.receptor";
        public static final String WHITE_BALANCE_AUTO = "auto";
        public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
        public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
        public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
        public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
        public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
        public static final String WHITE_BALANCE_TWILIGHT = "twilight";
        public static final String WHITE_BALANCE_SHADE = "shade";
        public static final String EFFECT_NONE = "none";
        public static final String EFFECT_MONO = "mono";
        public static final String EFFECT_NEGATIVE = "negative";
        public static final String EFFECT_SOLARIZE = "solarize";
        public static final String EFFECT_SEPIA = "sepia";
        public static final String EFFECT_POSTERIZE = "posterize";
        public static final String EFFECT_WHITEBOARD = "whiteboard";
        public static final String EFFECT_BLACKBOARD = "blackboard";
        public static final String EFFECT_AQUA = "aqua";
        public static final String ANTIBANDING_AUTO = "auto";
        public static final String ANTIBANDING_50HZ = "50hz";
        public static final String ANTIBANDING_60HZ = "60hz";
        public static final String ANTIBANDING_OFF = "off";
        public static final String VIDEO_PREVIEW_SWITCH = "video-preview-switch";
        public static final String FLASH_MODE_OFF = "off";
        public static final String FLASH_MODE_AUTO = "auto";
        public static final String FLASH_MODE_ON = "on";
        public static final String FLASH_MODE_RED_EYE = "red-eye";
        public static final String FLASH_MODE_TORCH = "torch";
        public static final String SCENE_MODE_AUTO = "auto";
        public static final String SCENE_MODE_ACTION = "action";
        public static final String SCENE_MODE_PORTRAIT = "portrait";
        public static final String SCENE_MODE_LANDSCAPE = "landscape";
        public static final String SCENE_MODE_NIGHT = "night";
        public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
        public static final String SCENE_MODE_THEATRE = "theatre";
        public static final String SCENE_MODE_BEACH = "beach";
        public static final String SCENE_MODE_SNOW = "snow";
        public static final String SCENE_MODE_SUNSET = "sunset";
        public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
        public static final String SCENE_MODE_FIREWORKS = "fireworks";
        public static final String SCENE_MODE_SPORTS = "sports";
        public static final String SCENE_MODE_PARTY = "party";
        public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
        public static final String SCENE_MODE_BARCODE = "barcode";
        public static final String SCENE_MODE_HDR = "hdr";
        public static final String FOCUS_MODE_AUTO = "auto";
        public static final String FOCUS_MODE_INFINITY = "infinity";
        public static final String FOCUS_MODE_MACRO = "macro";
        public static final String FOCUS_MODE_FIXED = "fixed";
        public static final String FOCUS_MODE_EDOF = "edof";
        public static final String FOCUS_MODE_CONTINUOUS_VIDEO = "continuous-video";
        public static final String FOCUS_MODE_CONTINUOUS_PICTURE = "continuous-picture";
        public static final int FOCUS_DISTANCE_NEAR_INDEX = 0;
        public static final int FOCUS_DISTANCE_OPTIMAL_INDEX = 1;
        public static final int FOCUS_DISTANCE_FAR_INDEX = 2;
        public static final int PREVIEW_FPS_MIN_INDEX = 0;
        public static final int PREVIEW_FPS_MAX_INDEX = 1;
        private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
        private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
        private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
        private static final String PIXEL_FORMAT_YUV420P = "yuv420p";
        private static final String PIXEL_FORMAT_RGB565 = "rgb565";
        private static final String PIXEL_FORMAT_JPEG = "jpeg";
        private static final String PIXEL_FORMAT_NV12 = "nv12";
        private static final String PIXEL_FORMAT_BAYER_RGGB = "bayer-rggb";

        public abstract String get(String key);
        public abstract void set(String key, String value);
        public abstract void set(String key, int value);
        public abstract void setPreviewSize(int width, int height);
        public abstract List<Size> getSupportedPreviewSizes();
        public abstract int getPictureSizeWidth();
        public abstract int getPictureSizeHeight();
        public abstract void setPictureSize(int width, int height);
        public abstract void setJpegThumbnailSize(int width, int height);
        public abstract void setExposureCompensation(int value);
        public abstract int getExposureCompensation();
        public abstract int getMaxExposureCompensation();
        public abstract int getMinExposureCompensation();
        public abstract int getMaxZoom();
        public abstract void setZoom(int var1);
        public abstract int getZoom();
        public abstract void setPreviewFormat(int var1);
        public abstract List<Integer> getSupportedPreviewFormats();
        public abstract void setPreviewFrameRate(int var1);
        public abstract List<int[]>getSupportedPreviewFpsRange();
        public abstract void setPreviewFpsRange(int min, int max);
        public abstract List<String> getSupportedFlashModes();
        public abstract void setFlashMode(String var1);
        public abstract void setRecordingHint(boolean hint);

        public abstract boolean isVideoStabilizationSupported();
        public abstract void setVideoStabilization(boolean toggle);
        public abstract boolean isZoomSupported();
    }

    public abstract static class CameraInfo {
        public static final int CAMERA_FACING_BACK = 0;
        public static final int CAMERA_FACING_FRONT = 1;
        public static final int CAMERA_FACING_DOUBLE = 2;

        public abstract int facing();
    }

    public interface ShutterCallback {
        void onShutter();
        void onLongShutter();
        void onShutterend();
    }

    public interface PictureCallback {
        void onPictureTaken(byte[] var1, theta360.hardware.Camera var2);
        void onPictureTaken(byte[] var1, android.hardware.Camera var2);
    }

    public interface PreviewCallback {
        void onPreviewFrame(byte[] var1, theta360.hardware.Camera var2);
        void onPreviewFrame(byte[] var1, android.hardware.Camera var2);
    }

    public interface ErrorCallback {
        void onError(int var1, theta360.hardware.Camera var2);
        void onError(int var1, android.hardware.Camera var2);
    }
}
