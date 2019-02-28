package com.pedro.encoder.utils;

import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.os.Environment;
import com.pedro.encoder.video.FormatVideoEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by pedro on 25/01/17.
 */

public class YUVUtil {

  private static byte[] rotate90Buffer;
  private static byte[] rotate180Buffer;
  private static byte[] rotate270Buffer;

  public static void preAllocateRotateBuffers(int length) {
    rotate90Buffer = new byte[length];
    rotate180Buffer = new byte[length];
    rotate270Buffer = new byte[length];
  }

  private static byte[] yv420pBuffer;
  private static byte[] yv420psBuffer;
  private static byte[] yv420spBuffer;

  public static void preAllocateYv12Buffers(int length) {
    yv420pBuffer = new byte[length];
    yv420psBuffer = new byte[length];
    yv420spBuffer = new byte[length];
  }

  private static byte[] nv420pBuffer;
  private static byte[] nv420spBuffer;
  private static byte[] nv420ppBuffer;

  public static void preAllocateNv21Buffers(int length) {
    nv420pBuffer = new byte[length];
    nv420spBuffer = new byte[length];
    nv420ppBuffer = new byte[length];
  }

  // for the vbuffer for YV12(android YUV), @see below:
  // https://developer.android.com/reference/android/hardware/Camera.Parameters.html#setPreviewFormat(int)
  // https://developer.android.com/reference/android/graphics/ImageFormat.html#YV12
  public static int getYuvBuffer(int width, int height) {
    // stride = ALIGN(width, 16)
    int stride = (int) Math.ceil(width / 16.0) * 16;
    // y_size = stride * height
    int y_size = stride * height;
    // c_stride = ALIGN(stride/2, 16)
    int c_stride = (int) Math.ceil(width / 32.0) * 16;
    // c_size = c_stride * height/2
    int c_size = c_stride * height / 2;
    // size = y_size + c_size * 2
    return y_size + c_size * 2;
  }

  public static byte[] ARGBtoYUV420SemiPlanar(int[] input, int width, int height) {
        /*
         * COLOR_FormatYUV420SemiPlanar is NV12
         */
    final int frameSize = width * height;
    byte[] yuv420sp = new byte[width * height * 3 / 2];
    int yIndex = 0;
    int uvIndex = frameSize;

    int a, R, G, B, Y, U, V;
    int index = 0;
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {

        a = (input[index] & 0xff000000) >> 24; // a is not used obviously
        R = (input[index] & 0xff0000) >> 16;
        G = (input[index] & 0xff00) >> 8;
        B = (input[index] & 0xff) >> 0;

        // well known RGB to YUV algorithm
        Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
        U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
        V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

        // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
        //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
        //    pixel AND every other scanline.
        yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
        if (j % 2 == 0 && index % 2 == 0) {
          yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
          yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
        }

        index++;
      }
    }
    return yuv420sp;
  }

  public static byte[] YV12toYUV420byColor(byte[] input, int width, int height,
      FormatVideoEncoder formatVideoEncoder) {
    switch (formatVideoEncoder) {
      case YUV420PLANAR:
        return YV12toYUV420Planar(input, width, height);
      //this is like nv21
      case YUV420PACKEDSEMIPLANAR:
        return YV12toYUV420PackedSemiPlanar(input, width, height);
      case YUV420SEMIPLANAR:
        return YV12toYUV420SemiPlanar(input, width, height);
      //convert to nv21 and then to yuv420PP
      case YUV420PACKEDPLANAR:
        return NV21toYUV420PackedPlanar(YV12toYUV420PackedSemiPlanar(input, width, height), width,
            height);
      default:
        return null;
    }
  }

  // the color transform, @see http://stackoverflow.com/questions/15739684/mediacodec-and-camera-color-space-incorrect
  public static byte[] YV12toYUV420PackedSemiPlanar(byte[] input, int width, int height) {
        /*
         * COLOR_TI_FormatYUV420PackedSemiPlanar is NV21
         * We convert by putting the corresponding U and V bytes together (interleaved).
         */
    final int frameSize = width * height;
    final int qFrameSize = frameSize / 4;

    System.arraycopy(input, 0, yv420psBuffer, 0, frameSize); // Y

    for (int i = 0; i < qFrameSize; i++) {
      yv420psBuffer[frameSize + i * 2 + 1] = input[frameSize + i + qFrameSize]; // Cb (U)
      yv420psBuffer[frameSize + i * 2] = input[frameSize + i]; // Cr (V)
    }
    return yv420psBuffer;
  }

  // the color transform, @see http://stackoverflow.com/questions/15739684/mediacodec-and-camera-color-space-incorrect
  public static byte[] YV12toYUV420SemiPlanar(byte[] input, int width, int height) {
        /*
         * COLOR_FormatYUV420SemiPlanar is NV12
         * We convert by putting the corresponding U and V bytes together (interleaved).
         */
    final int frameSize = width * height;
    final int qFrameSize = frameSize / 4;

    System.arraycopy(input, 0, yv420spBuffer, 0, frameSize); // Y

    for (int i = 0; i < qFrameSize; i++) {
      yv420spBuffer[frameSize + i * 2] = input[frameSize + i + qFrameSize]; // Cb (U)
      yv420spBuffer[frameSize + i * 2 + 1] = input[frameSize + i]; // Cr (V)
    }
    return yv420spBuffer;
  }

  public static byte[] YV12toYUV420Planar(byte[] input, int width, int height) {
        /*
         * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
         * So we just have to reverse U and V.
         */

    final int frameSize = width * height;
    final int qFrameSize = frameSize / 4;

    System.arraycopy(input, 0, yv420pBuffer, 0, frameSize); // Y
    System.arraycopy(input, frameSize + qFrameSize, yv420pBuffer, frameSize, qFrameSize); // Cb (U)
    System.arraycopy(input, frameSize, yv420pBuffer, frameSize + qFrameSize, qFrameSize); // Cr (V)
    return yv420pBuffer;
  }

  public static byte[] NV21toYUV420byColor(byte[] input, int width, int height,
      FormatVideoEncoder formatVideoEncoder) {
    switch (formatVideoEncoder) {
      case YUV420PLANAR:
        return NV21toYUV420Planar(input, width, height);
      case YUV420PACKEDPLANAR:
        return NV21toYUV420PackedPlanar(input, width, height);
      case YUV420SEMIPLANAR:
        return NV21toYUV420SemiPlanar(input, width, height);
      //because yuv420PSP and NV21 is the same
      case YUV420PACKEDSEMIPLANAR:
        return input;
      default:
        return null;
    }
  }

  public static byte[] rotateNV21(byte[] data, int width, int height, int rotation) {
    switch (rotation) {
      case 0:
        return data;
      case 90:
        return rotateNV21Degree90(data, width, height);
      case 180:
        return rotateNV21Degree180(data, width, height);
      case 270:
        return rotateNV21Degree270(data, width, height);
      default:
        return null;
    }
  }

  private static byte[] rotateNV21Degree90(byte[] data, int imageWidth, int imageHeight) {
    // Rotate the Y luma
    int i = 0;
    for (int x = 0; x < imageWidth; x++) {
      for (int y = imageHeight - 1; y >= 0; y--) {
        rotate90Buffer[i] = data[y * imageWidth + x];
        i++;
      }
    }
    // Rotate the U and V color components
    i = imageWidth * imageHeight * 3 / 2 - 1;
    for (int x = imageWidth - 1; x > 0; x = x - 2) {
      for (int y = 0; y < imageHeight / 2; y++) {
        rotate90Buffer[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
        i--;
        rotate90Buffer[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
        i--;
      }
    }
    return rotate90Buffer;
  }

  private static byte[] rotateNV21Degree180(byte[] data, int imageWidth, int imageHeight) {
    int count = 0;
    for (int i = imageWidth * imageHeight - 1; i >= 0; i--) {
      rotate180Buffer[count] = data[i];
      count++;
    }
    for (int i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth * imageHeight; i -= 2) {
      rotate180Buffer[count++] = data[i - 1];
      rotate180Buffer[count++] = data[i];
    }
    return rotate180Buffer;
  }

  private static byte[] rotateNV21Degree270(byte[] data, int imageWidth, int imageHeight) {
    int nWidth = 0, nHeight = 0;
    int wh = 0;
    int uvHeight = 0;
    if (imageWidth != nWidth || imageHeight != nHeight) {
      wh = imageWidth * imageHeight;
      uvHeight = imageHeight >> 1;// uvHeight = height / 2
    }
    // ??Y
    int k = 0;
    for (int i = 0; i < imageWidth; i++) {
      int nPos = 0;
      for (int j = 0; j < imageHeight; j++) {
        rotate270Buffer[k] = data[nPos + i];
        k++;
        nPos += imageWidth;
      }
    }
    for (int i = 0; i < imageWidth; i += 2) {
      int nPos = wh;
      for (int j = 0; j < uvHeight; j++) {
        rotate270Buffer[k] = data[nPos + i];
        rotate270Buffer[k + 1] = data[nPos + i + 1];
        k += 2;
        nPos += imageWidth;
      }
    }
    return rotateNV21Degree180(rotate270Buffer, imageWidth, imageHeight);
  }

  public static byte[] NV21toYUV420PackedPlanar(byte[] input, int width, int height) {
        /*
         * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
         * So we just have to reverse U and V.
         */
    final int frameSize = width * height;
    final int qFrameSize = frameSize / 4;

    System.arraycopy(input, 0, nv420ppBuffer, 0, frameSize); // Y

    for (int i = 0; i < qFrameSize; i++) {
      nv420ppBuffer[frameSize + i + qFrameSize] = input[frameSize + i * 2 + 1]; // Cb (U)
      nv420ppBuffer[frameSize + i] = input[frameSize + i * 2]; // Cr (V)
    }

    return nv420ppBuffer;
  }

  // the color transform, @see http://stackoverflow.com/questions/15739684/mediacodec-and-camera-color-space-incorrect
  public static byte[] NV21toYUV420SemiPlanar(byte[] input, int width, int height) {
        /*
         * COLOR_FormatYUV420SemiPlanar is NV12
         * We convert by putting the corresponding U and V bytes together (interleaved).
         */
    final int frameSize = width * height;
    final int qFrameSize = frameSize / 4;

    System.arraycopy(input, 0, nv420spBuffer, 0, frameSize); // Y

    for (int i = 0; i < qFrameSize; i++) {
      nv420spBuffer[frameSize + i * 2] = input[frameSize + i * 2 + 1]; // Cb (U)
      nv420spBuffer[frameSize + i * 2 + 1] = input[frameSize + i * 2]; // Cr (V)
    }
    return nv420spBuffer;
  }

  public static byte[] NV21toYUV420Planar(byte[] input, int width, int height) {
        /*
         * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
         * So we just have to reverse U and V.
         */
    final int frameSize = width * height;
    final int qFrameSize = frameSize / 4;

    System.arraycopy(input, 0, nv420pBuffer, 0, frameSize); // Y

    for (int i = 0; i < qFrameSize; i++) {
      nv420pBuffer[frameSize + i] = input[frameSize + i * 2 + 1]; // Cb (U)
      nv420pBuffer[frameSize + i + qFrameSize] = input[frameSize + i * 2]; // Cr (V)
    }

    return nv420pBuffer;
  }

  public void dumpYUVData(byte[] buffer, int len, String name) {
    File f = new File(Environment.getExternalStorageDirectory().getPath() + "/tmp/", name);
    if (f.exists()) {
      f.delete();
    }
    try {
      FileOutputStream out = new FileOutputStream(f);
      out.write(buffer);
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static byte[] CropYuv(int src_format, byte[] src_yuv, int src_width, int src_height,
      int dst_width, int dst_height) {
    byte[] dst_yuv;
    if (src_yuv == null) return null;
    // simple implementation: copy the corner
    if (src_width == dst_width && src_height == dst_height) {
      dst_yuv = src_yuv;
    } else {
      dst_yuv = new byte[(int) (dst_width * dst_height * 1.5)];
      switch (src_format) {
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: // I420
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: // YV12
        {
          // copy Y
          int src_yoffset = 0;
          int dst_yoffset = 0;
          for (int i = 0; i < dst_height; i++) {
            System.arraycopy(src_yuv, src_yoffset, dst_yuv, dst_yoffset, dst_width);
            src_yoffset += src_width;
            dst_yoffset += dst_width;
          }

          // copy u
          int src_uoffset = 0;
          int dst_uoffset = 0;
          src_yoffset = src_width * src_height;
          dst_yoffset = dst_width * dst_height;
          for (int i = 0; i < dst_height / 2; i++) {
            System.arraycopy(src_yuv, src_yoffset + src_uoffset, dst_yuv, dst_yoffset + dst_uoffset,
                dst_width / 2);
            src_uoffset += src_width / 2;
            dst_uoffset += dst_width / 2;
          }

          // copy v
          int src_voffset = 0;
          int dst_voffset = 0;
          src_uoffset = src_width * src_height + src_width * src_height / 4;
          dst_uoffset = dst_width * dst_height + dst_width * dst_height / 4;
          for (int i = 0; i < dst_height / 2; i++) {
            System.arraycopy(src_yuv, src_uoffset + src_voffset, dst_yuv, dst_uoffset + dst_voffset,
                dst_width / 2);
            src_voffset += src_width / 2;
            dst_voffset += dst_width / 2;
          }
        }
        break;
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: // NV12
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar: // NV21
        case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
        case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar: {
          // copy Y
          int src_yoffset = 0;
          int dst_yoffset = 0;
          for (int i = 0; i < dst_height; i++) {
            System.arraycopy(src_yuv, src_yoffset, dst_yuv, dst_yoffset, dst_width);
            src_yoffset += src_width;
            dst_yoffset += dst_width;
          }

          // copy u and v
          int src_uoffset = 0;
          int dst_uoffset = 0;
          src_yoffset = src_width * src_height;
          dst_yoffset = dst_width * dst_height;
          for (int i = 0; i < dst_height / 2; i++) {
            System.arraycopy(src_yuv, src_yoffset + src_uoffset, dst_yuv, dst_yoffset + dst_uoffset,
                dst_width);
            src_uoffset += src_width;
            dst_uoffset += dst_width;
          }
        }
        break;

        default: {
          dst_yuv = null;
        }
        break;
      }
    }
    return dst_yuv;
  }

  public static byte[] rotatePixelsNV21(byte[] input, int width, int height, int rotation) {
    byte[] output = new byte[input.length];

    boolean swap = (rotation == 90 || rotation == 270);
    boolean yflip = (rotation == 90 || rotation == 180);
    boolean xflip = (rotation == 270 || rotation == 180);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int xo = x, yo = y;
        int w = width, h = height;
        int xi = xo, yi = yo;
        if (swap) {
          xi = w * yo / h;
          yi = h * xo / w;
        }
        if (yflip) {
          yi = h - yi - 1;
        }
        if (xflip) {
          xi = w - xi - 1;
        }
        output[w * yo + xo] = input[w * yi + xi];
        int fs = w * h;
        int qs = (fs >> 2);
        xi = (xi >> 1);
        yi = (yi >> 1);
        xo = (xo >> 1);
        yo = (yo >> 1);
        w = (w >> 1);
        h = (h >> 1);
        // adjust for interleave here
        int ui = fs + (w * yi + xi) * 2;
        int uo = fs + (w * yo + xo) * 2;
        // and here
        int vi = ui + 1;
        int vo = uo + 1;
        output[uo] = input[ui];
        output[vo] = input[vi];
      }
    }
    return output;
  }

  public static byte[] mirrorNV21(byte[] input, int width, int height) {
    byte[] output = new byte[input.length];

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int xo = x, yo = y;
        int w = width, h = height;
        int xi = xo, yi = yo;
        yi = h - yi - 1;
        output[w * yo + xo] = input[w * yi + xi];
        int fs = w * h;
        int qs = (fs >> 2);
        xi = (xi >> 1);
        yi = (yi >> 1);
        xo = (xo >> 1);
        yo = (yo >> 1);
        w = (w >> 1);
        h = (h >> 1);
        // adjust for interleave here
        int ui = fs + (w * yi + xi) * 2;
        int uo = fs + (w * yo + xo) * 2;
        // and here
        int vi = ui + 1;
        int vo = uo + 1;
        output[uo] = input[ui];
        output[vo] = input[vi];
      }
    }
    return output;
  }

  public static byte[] bitmapToNV21(int inputWidth, int inputHeight, Bitmap bitmap) {
    int [] argb = new int[inputWidth * inputHeight];
    bitmap.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
    byte [] yuv = ARGBtoYUV420SemiPlanar(argb, inputWidth, inputHeight);
    bitmap.recycle();
    return yuv;
  }
}
