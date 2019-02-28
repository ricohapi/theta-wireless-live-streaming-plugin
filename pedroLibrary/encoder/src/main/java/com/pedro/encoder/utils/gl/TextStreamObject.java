package com.pedro.encoder.utils.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import java.io.IOException;

/**
 * Created by pedro on 23/09/17.
 */

public class TextStreamObject extends StreamObjectBase {

  private static final String TAG = "TextStreamObject";

  private int numFrames;
  private Bitmap imageBitmap;

  public TextStreamObject() {
  }

  @Override
  public int getWidth() {
    return imageBitmap.getWidth();
  }

  @Override
  public int getHeight() {
    return imageBitmap.getHeight();
  }

  public void load(String text, float textSize, int textColor) throws IOException {
    numFrames = 1;
    imageBitmap = textAsBitmap(text, textSize, textColor);
    Log.i(TAG, "finish load text");
  }

  @Override
  public void resize(int width, int height) {
    imageBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
  }

  @Override
  public void recycle() {
    imageBitmap.recycle();
  }

  private Bitmap textAsBitmap(String text, float textSize, int textColor) {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextSize(textSize);
    paint.setColor(textColor);
    paint.setTextAlign(Paint.Align.LEFT);
    float baseline = -paint.ascent(); // ascent() is negative
    int width = (int) (paint.measureText(text) + 0.5f); // round
    int height = (int) (baseline + paint.descent() + 0.5f);
    Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(image);
    canvas.drawText(text, 0, baseline, paint);
    return image;
  }

  @Override
  public int getNumFrames() {
    return numFrames;
  }

  public Bitmap getImageBitmap() {
    return imageBitmap;
  }

  @Override
  public int updateFrame() {
    return 0;
  }
}
