package com.pedro.encoder.input.video;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

/**
 * Created by pedro on 7/10/17.
 */

@IntDef({ CAMERA_FACING_BACK, CAMERA_FACING_FRONT})
@Retention(RetentionPolicy.SOURCE)
public @interface Camera1Facing {
}
