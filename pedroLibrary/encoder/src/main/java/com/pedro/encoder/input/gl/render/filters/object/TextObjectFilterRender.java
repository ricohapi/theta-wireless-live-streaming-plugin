package com.pedro.encoder.input.gl.render.filters.object;

import android.graphics.Typeface;
import android.opengl.GLES20;
import android.os.Build;
import androidx.annotation.RequiresApi;
import com.pedro.encoder.utils.gl.TextStreamObject;

/**
 * Created by pedro on 27/07/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TextObjectFilterRender extends BaseObjectFilterRender {

  private String text;
  private float textSize;
  private int textColor;
  private Typeface typeface;

  public TextObjectFilterRender() {
    super();
    streamObject = new TextStreamObject();
  }

  @Override
  protected void drawFilter() {
    super.drawFilter();
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, streamObjectTextureId[0]);
    //Set alpha. 0f if no image loaded.
    GLES20.glUniform1f(uAlphaHandle, streamObjectTextureId[0] == -1 ? 0f : alpha);
  }

  public void setText(String text, float textSize, int textColor) {
    setText(text, textSize, textColor, null);
  }

  public void setText(String text, float textSize, int textColor, Typeface typeface) {
    this.text = text;
    this.textSize = textSize;
    this.textColor = textColor;
    this.typeface = typeface;
    ((TextStreamObject) streamObject).load(text, textSize, textColor, typeface);
    shouldLoad = true;
  }

  public void addText(String text) {
    setText(this.text + text, textSize, textColor, typeface);
  }

  public void updateColor(int textColor) {
    setText(this.text + text, textSize, textColor, typeface);
  }

  public void updateTypeface(Typeface typeface) {
    setText(this.text + text, textSize, textColor, typeface);
  }

  public void updateTextSize(float textSize) {
    setText(this.text + text, textSize, textColor, typeface);
  }
}
