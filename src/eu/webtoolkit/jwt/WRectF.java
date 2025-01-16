/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import eu.webtoolkit.jwt.auth.*;
import eu.webtoolkit.jwt.auth.mfa.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.servlet.*;
import eu.webtoolkit.jwt.utils.*;
import java.io.*;
import java.lang.ref.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A value class that defines a rectangle.
 *
 * <p>The rectangle is defined by a top-left point and a width and height.
 *
 * <p>
 *
 * <h3>JavaScript exposability</h3>
 *
 * <p>A WRectF is JavaScript exposable. If a WRectF {@link
 * WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}, it can be accessed in your
 * custom JavaScript code through {@link WJavaScriptHandle#getJsRef() its handle&apos;s jsRef()}. A
 * rectangle is represented in JavaScript as an array of four elements (x,y,width,height), e.g. a
 * rectangle WRectF(10,20,30,40) will be represented in JavaScript as:
 *
 * <pre>{@code
 * [10, 20, 30, 40]
 *
 * }</pre>
 *
 * <p>
 *
 * <p><i><b>Warning: </b>A WRectF that is JavaScript exposed should be modified only through its
 * {@link WJavaScriptHandle handle}. Any attempt at modifying it will cause an exception to be
 * thrown. </i>
 *
 * @see WPaintedWidget#createJSRect()
 */
public class WRectF extends WJavaScriptExposableObject {
  private static Logger logger = LoggerFactory.getLogger(WRectF.class);

  /**
   * Default constructor.
   *
   * <p>Constructs an empty rectangle.
   *
   * <p>
   *
   * @see WRectF#isEmpty()
   */
  public WRectF() {
    super();
    this.x_ = 0;
    this.y_ = 0;
    this.width_ = 0;
    this.height_ = 0;
  }
  /**
   * Creates a rectangle.
   *
   * <p>Constructs a rectangle with top left point (<code>x</code>, <code>y</code>) and size <code>
   * width</code> x <code>height</code>.
   */
  public WRectF(double x, double y, double width, double height) {
    super();
    this.x_ = x;
    this.y_ = y;
    this.width_ = width;
    this.height_ = height;
  }

  public WRectF(final WRectF other) {
    super(other);
    this.x_ = other.getX();
    this.y_ = other.getY();
    this.width_ = other.getWidth();
    this.height_ = other.getHeight();
  }
  /**
   * Creates a rectangle.
   *
   * <p>Constructs a rectangle from the two points <code>topLeft</code> and <code>bottomRight</code>
   * .
   *
   * <p>If you want to create a rectangle from two arbitrary corner points, you can use this
   * constructor too, but should call {@link WRectF#getNormalized() getNormalized()} afterwords.
   */
  public WRectF(final WPointF topLeft, final WPointF bottomRight) {
    super();
    this.x_ = topLeft.getX();
    this.y_ = topLeft.getY();
    this.width_ = bottomRight.getX() - topLeft.getX();
    this.height_ = bottomRight.getY() - topLeft.getY();
  }

  public WRectF clone() {
    return new WRectF(this);
  }
  /** Indicates whether some other object is "equal to" this one. */
  public boolean equals(final WRectF rhs) {
    if (!this.sameBindingAs(rhs)) {
      return false;
    }
    return this.x_ == rhs.x_
        && this.y_ == rhs.y_
        && this.width_ == rhs.width_
        && this.height_ == rhs.height_;
  }
  // public boolean () ;
  /**
   * Determines whether or not this rectangle is empty.
   *
   * <p>A rectangle is empty if its width and height are zero.
   *
   * <p>A rectangle that {@link WJavaScriptExposableObject#isJavaScriptBound()} is JavaScript bound
   * is never empty.
   */
  public boolean isEmpty() {
    if (this.isJavaScriptBound()) {
      return false;
    }
    return this.width_ == 0 && this.height_ == 0;
  }
  /**
   * Sets the X-position of the left side.
   *
   * <p>The right side of the rectangle does not move, and as a result, the rectangle may be
   * resized.
   *
   * <p>
   *
   * @exception {@link WException} if the rectangle {@link
   *     WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}
   */
  public void setX(double x) {
    this.checkModifiable();
    this.width_ += this.x_ - x;
    this.x_ = x;
  }
  /**
   * Sets the Y-position of the top side.
   *
   * <p>The bottom side of the rectangle does not move, and as a result, the rectangle may be
   * resized.
   *
   * <p>
   *
   * @exception {@link WException} if the rectangle {@link
   *     WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}
   */
  public void setY(double y) {
    this.checkModifiable();
    this.height_ += this.y_ - y;
    this.y_ = y;
  }
  /**
   * Sets the width.
   *
   * <p>The right side of the rectangle may move, but this does not affect the left side.
   *
   * <p>
   *
   * @exception {@link WException} if the rectangle {@link
   *     WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}
   */
  public void setWidth(double width) {
    this.width_ = width;
  }
  /**
   * Sets the Y-position of the top side.
   *
   * <p>The bottom side of the rectangle may move, but this does not affect the Y position of the
   * top side.
   *
   * <p>
   *
   * @exception {@link WException} if the rectangle {@link
   *     WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}
   */
  public void setHeight(double height) {
    this.height_ = height;
  }
  /**
   * Returns the X-position (left side offset).
   *
   * <p>This is equivalent to {@link WRectF#getLeft() getLeft()}.
   *
   * <p>
   *
   * @see WRectF#getY()
   * @see WRectF#getLeft()
   */
  public double getX() {
    return this.x_;
  }
  /**
   * Returns the Y-position (top side offset).
   *
   * <p>This is equivalent to {@link WRectF#getTop() getTop()}.
   *
   * <p>
   *
   * @see WRectF#getX()
   * @see WRectF#getTop()
   */
  public double getY() {
    return this.y_;
  }
  /**
   * Returns the width.
   *
   * <p>
   *
   * @see WRectF#getHeight()
   */
  public double getWidth() {
    return this.width_;
  }
  /**
   * Returns the height.
   *
   * <p>
   *
   * @see WRectF#getWidth()
   */
  public double getHeight() {
    return this.height_;
  }
  /**
   * Returns the X position (left side offset).
   *
   * <p>
   *
   * @see WRectF#getX()
   * @see WRectF#getRight()
   */
  public double getLeft() {
    return this.x_;
  }
  /**
   * Returns the Y position (top side offset).
   *
   * <p>
   *
   * @see WRectF#getY()
   * @see WRectF#getBottom()
   */
  public double getTop() {
    return this.y_;
  }
  /**
   * Returns the the right side offset.
   *
   * <p>
   *
   * @see WRectF#getLeft()
   */
  public double getRight() {
    return this.x_ + this.width_;
  }
  /**
   * Returns the bottom side offset.
   *
   * <p>
   *
   * @see WRectF#getTop()
   */
  public double getBottom() {
    return this.y_ + this.height_;
  }
  /**
   * Returns the top left point.
   *
   * <p>
   *
   * @see WRectF#getLeft()
   * @see WRectF#getTop()
   */
  public WPointF getTopLeft() {
    return new WPointF(this.x_, this.y_);
  }
  /**
   * Returns the top right point.
   *
   * <p>
   *
   * @see WRectF#getRight()
   * @see WRectF#getTop()
   */
  public WPointF getTopRight() {
    return new WPointF(this.x_ + this.width_, this.y_);
  }
  /** Returns the center point. */
  public WPointF getCenter() {
    return new WPointF(this.x_ + this.width_ / 2, this.y_ + this.height_ / 2);
  }
  /**
   * Returns the bottom left point.
   *
   * <p>
   *
   * @see WRectF#getLeft()
   * @see WRectF#getBottom()
   */
  public WPointF getBottomLeft() {
    return new WPointF(this.x_, this.y_ + this.height_);
  }
  /**
   * Returns the bottom right point.
   *
   * <p>
   *
   * @see WRectF#getRight()
   * @see WRectF#getBottom()
   */
  public WPointF getBottomRight() {
    return new WPointF(this.x_ + this.width_, this.y_ + this.height_);
  }
  /**
   * Tests if a rectangle contains a point.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This method is not supported if this rectangle {@link
   * WJavaScriptExposableObject#isJavaScriptBound()} </i>
   */
  public boolean contains(final WPointF p) {
    return this.contains(p.getX(), p.getY());
  }
  /**
   * Tests if a rectangle contains a point.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This method is not supported if this rectangle {@link
   * WJavaScriptExposableObject#isJavaScriptBound()} </i>
   */
  public boolean contains(double x, double y) {
    return x >= this.x_
        && x <= this.x_ + this.width_
        && y >= this.y_
        && y <= this.y_ + this.height_;
  }
  /** Tests if two rectangles intersect. */
  public boolean intersects(final WRectF other) {
    if (this.isEmpty() || other.isEmpty()) {
      return false;
    } else {
      WRectF r1 = this.getNormalized();
      WRectF r2 = other.getNormalized();
      boolean intersectX =
          r2.getLeft() >= r1.getLeft() && r2.getLeft() <= r1.getRight()
              || r2.getRight() >= r1.getLeft() && r2.getRight() <= r1.getRight();
      boolean intersectY =
          r2.getTop() >= r1.getTop() && r2.getTop() <= r1.getBottom()
              || r2.getBottom() >= r1.getTop() && r2.getBottom() <= r1.getBottom();
      return intersectX && intersectY;
    }
  }
  /**
   * Makes the union of to rectangles.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This method is not supported if this rectangle {@link
   * WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}. </i>
   */
  public WRectF united(final WRectF other) {
    if (this.isEmpty()) {
      return other;
    } else {
      if (other.isEmpty()) {
        return this;
      } else {
        WRectF r1 = this.getNormalized();
        WRectF r2 = other.getNormalized();
        double l = Math.min(r1.getLeft(), r2.getLeft());
        double r = Math.max(r1.getRight(), r2.getRight());
        double t = Math.min(r1.getTop(), r2.getTop());
        double b = Math.max(r1.getBottom(), r2.getBottom());
        return new WRectF(l, t, r - l, b - t);
      }
    }
  }
  /**
   * Returns a normalized rectangle.
   *
   * <p>A normalized rectangle has a positive width and height.
   *
   * <p>This method supports JavaScript bound rectangles.
   */
  public WRectF getNormalized() {
    double x;
    double y;
    double w;
    double h;
    if (this.width_ > 0) {
      x = this.x_;
      w = this.width_;
    } else {
      x = this.x_ + this.width_;
      w = -this.width_;
    }
    if (this.height_ > 0) {
      y = this.y_;
      h = this.height_;
    } else {
      y = this.y_ + this.height_;
      h = -this.height_;
    }
    WRectF result = new WRectF(x, y, w, h);
    if (this.isJavaScriptBound()) {
      result.assignBinding(this, "Wt4.11.2.gfxUtils.rect_normalized(" + this.getJsRef() + ')');
    }
    return result;
  }

  public String getJsValue() {
    char[] buf = new char[30];
    StringBuilder ss = new StringBuilder();
    ss.append('[');
    ss.append(MathUtils.roundJs(this.x_, 3)).append(',');
    ss.append(MathUtils.roundJs(this.y_, 3)).append(',');
    ss.append(MathUtils.roundJs(this.width_, 3)).append(',');
    ss.append(MathUtils.roundJs(this.height_, 3)).append(']');
    return ss.toString();
  }

  public WPainterPath toPath() {
    WPainterPath path = new WPainterPath(new WPointF(this.x_, this.y_));
    path.lineTo(this.x_ + this.width_, this.y_);
    path.lineTo(this.x_ + this.width_, this.y_ + this.height_);
    path.lineTo(this.x_, this.y_ + this.height_);
    path.closeSubPath();
    return path;
  }

  protected void assignFromJSON(final com.google.gson.JsonElement value) {
    try {
      final com.google.gson.JsonArray ar = (com.google.gson.JsonArray) value;
      if (ar.size() == 4
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(0)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(1)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(2)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(3)))) {
        this.x_ = JsonUtils.orIfNullDouble(JsonUtils.toNumber(ar.get(0)), this.x_);
        this.y_ = JsonUtils.orIfNullDouble(JsonUtils.toNumber(ar.get(1)), this.y_);
        this.width_ = JsonUtils.orIfNullDouble(JsonUtils.toNumber(ar.get(2)), this.width_);
        this.height_ = JsonUtils.orIfNullDouble(JsonUtils.toNumber(ar.get(3)), this.height_);
      } else {
        logger.error(new StringWriter().append("Couldn't convert JSON to WRectF").toString());
      }
    } catch (final RuntimeException e) {
      logger.error(
          new StringWriter().append("Couldn't convert JSON to WRectF: " + e.toString()).toString());
    }
  }

  private double x_;
  private double y_;
  private double width_;
  private double height_;
}
