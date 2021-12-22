package com.pedro.encoder.input.gl.render.filters;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import androidx.annotation.RequiresApi;
import com.pedro.encoder.R;
import com.pedro.encoder.utils.gl.GlUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by pedro on 3/02/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SharpnessFilterRender extends BaseFilterRender {

  //rotation matrix
  private final float[] squareVertexDataFilter = {
      // X, Y, Z, U, V
      -1f, -1f, 0f, 0f, 0f, //bottom left
      1f, -1f, 0f, 1f, 0f, //bottom right
      -1f, 1f, 0f, 0f, 1f, //top left
      1f, 1f, 0f, 1f, 1f, //top right
  };

  private int program = -1;
  private int aPositionHandle = -1;
  private int aTextureHandle = -1;
  private int uMVPMatrixHandle = -1;
  private int uSTMatrixHandle = -1;
  private int uSamplerHandle = -1;
  private int uStepSizeHandle = -1;
  private int uSharpnessHandle = -1;

  private float sharpness = 0.5f;

  public SharpnessFilterRender() {
    squareVertex = ByteBuffer.allocateDirect(squareVertexDataFilter.length * FLOAT_SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer();
    squareVertex.put(squareVertexDataFilter).position(0);
    Matrix.setIdentityM(MVPMatrix, 0);
    Matrix.setIdentityM(STMatrix, 0);
  }

  @Override
  protected void initGlFilter(Context context) {
    String vertexShader = GlUtil.getStringFromRaw(context, R.raw.simple_vertex);
    String fragmentShader = GlUtil.getStringFromRaw(context, R.raw.sharpness_fragment);

    program = GlUtil.createProgram(vertexShader, fragmentShader);
    aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
    aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
    uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
    uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix");
    uSamplerHandle = GLES20.glGetUniformLocation(program, "uSampler");
    uStepSizeHandle = GLES20.glGetUniformLocation(program, "uStepSize");
    uSharpnessHandle = GLES20.glGetUniformLocation(program, "uBrightness");
  }

  @Override
  protected void drawFilter() {
    GLES20.glUseProgram(program);

    squareVertex.position(SQUARE_VERTEX_DATA_POS_OFFSET);
    GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
        SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
    GLES20.glEnableVertexAttribArray(aPositionHandle);

    squareVertex.position(SQUARE_VERTEX_DATA_UV_OFFSET);
    GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
        SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
    GLES20.glEnableVertexAttribArray(aTextureHandle);

    GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, MVPMatrix, 0);
    GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, STMatrix, 0);
    GLES20.glUniform2f(uStepSizeHandle, 1f / getWidth(), 1f / getHeight());
    GLES20.glUniform1f(uSharpnessHandle, sharpness);

    GLES20.glUniform1i(uSamplerHandle, 4);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, previousTexId);
  }

  @Override
  public void release() {
    GLES20.glDeleteProgram(program);
  }

  public float getSharpness() {
    return sharpness;
  }

  /**
   * @param sharpness between 0 and 1. 0 means no change.
   */
  public void setSharpness(float sharpness) {
    this.sharpness = sharpness;
  }
}
