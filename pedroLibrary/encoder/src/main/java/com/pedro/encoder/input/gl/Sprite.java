package com.pedro.encoder.input.gl;

import android.graphics.PointF;

import com.pedro.encoder.utils.gl.TranslateTo;

/**
 * Created by pedro on 17/11/17.
 *
 * Sprite is drawn from top left of the image.
 * Sprite positions in screen:
 *
 *  0,0     100,0
 *    ________
 *   |        |
 *   |        |
 *   | Screen |
 *   |        |
 *   |________|
 *
 *  0,100   100,100
 */

public class Sprite {

  private final float[] squareVertexDataSprite = {
      //X  Y
      0f, 1f, //top left
      1f, 1f, //top right
      0f, 0f, //bottom left
      1f, 0f, //bottom right
  };

  private PointF scale;
  private PointF position;

  public Sprite() {
    reset();
  }

  /**
   * @param deltaX Position x in percent
   * @param deltaY Position x in percent
   */
  public void translate(float deltaX, float deltaY) {
    position.x = deltaX;
    position.y = deltaY;
  }

  /**
   * @param translation Predefined position
   */
  public void translate(TranslateTo translation) {
    switch (translation) {
      case CENTER:
        this.position.x = 50f - scale.x / 2f;
        this.position.y = 50f - scale.x / 2f;
        break;
      case BOTTOM:
        this.position.x = 50f - scale.x / 2f;
        this.position.y = 100f - scale.y;
        break;
      case TOP:
        this.position.x = 50f - scale.x / 2f;
        this.position.y = 0f;
        break;
      case LEFT:
        this.position.x = 0f;
        this.position.y = 50f - scale.y / 2f;
        break;
      case RIGHT:
        this.position.x = 100f - scale.x;
        this.position.y = 50f - scale.y / 2f;
        break;
      case TOP_LEFT:
        this.position.x = 0f;
        this.position.y = 0f;
        break;
      case TOP_RIGHT:
        this.position.x = 100f - scale.x;
        this.position.y = 0f;
        break;
      case BOTTOM_LEFT:
        this.position.x = 0f;
        this.position.y = 100f - scale.y;
        break;
      case BOTTOM_RIGHT:
        this.position.x = 100f - scale.x;
        this.position.y = 100f - scale.y;
        break;
      default:
        break;
    }
  }

  /**
   * @param deltaX Scale x in percent
   * @param deltaY Scale y in percent
   */
  public void scale(float deltaX, float deltaY) {
    //keep old position
    position.x /= deltaX / scale.x;
    position.y /= deltaY / scale.y;
    //set new scale.
    scale = new PointF(deltaX, deltaY);
  }

  /**
   * @return Scale in percent
   */
  public PointF getScale() {
    return scale;
  }

  /**
   * @return Position in percent
   */
  public PointF getTranslation() {
    return position;
  }

  public void reset() {
    scale = new PointF(100f, 100f);
    position = new PointF(0f, 0f);
  }

  /**
   * @return Actual vertex of sprite.
   */
  public float[] getTransformedVertices() {
    PointF bottomRight = new PointF(squareVertexDataSprite[0], squareVertexDataSprite[1]);
    PointF bottomLeft = new PointF(squareVertexDataSprite[2], squareVertexDataSprite[3]);
    PointF topRight = new PointF(squareVertexDataSprite[4], squareVertexDataSprite[5]);
    PointF topLeft = new PointF(squareVertexDataSprite[6], squareVertexDataSprite[7]);
    //Traduce scale to Opengl vertex values
    float scaleX = scale.x / 100f;
    float scaleY = scale.y / 100f;

    //Scale sprite
    bottomRight.x /= scaleX;
    bottomRight.y /= scaleY;

    bottomLeft.x /= scaleX;
    bottomLeft.y /= scaleY;

    topRight.x /= scaleX;
    topRight.y /= scaleY;

    topLeft.x /= scaleX;
    topLeft.y /= scaleY;

    //Traduce position to Opengl values
    float positionX = -position.x / scale.x;
    float positionY = -position.y / scale.y;

    //Translate sprite
    bottomRight.x += positionX;
    bottomRight.y += positionY;

    bottomLeft.x += positionX;
    bottomLeft.y += positionY;

    topRight.x += positionX;
    topRight.y += positionY;

    topLeft.x += positionX;
    topLeft.y += positionY;

    //Recreate vertex like initial vertex.
    return new float[] {
        bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y, topRight.x, topRight.y, topLeft.x,
        topLeft.y,
    };
  }
}
