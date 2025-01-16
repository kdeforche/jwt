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
 * A value class that defines a 2D affine transformation matrix.
 *
 * <p>The matrix is encoded using 6 parameters:
 *
 * <pre>{@code
 * m11  m12   0
 * m21  m22   0
 * dx   dy    1
 *
 * }</pre>
 *
 * <p>In this representation, {@link WTransform#getDx() getDx()} (= {@link WTransform#getM31()
 * getM31()}) and {@link WTransform#getDy() getDy()} (= {@link WTransform#getM32() getM32()})
 * represent the translation components, and m<i>xy</i> represent a 2D matrix that contains the
 * scale, rotation (and skew) components.
 *
 * <p>The transformation is used to represent a tansformed coordinate system, and provides methods
 * to {@link WTransform#rotate(double angle) rotate()}, {@link WTransform#scale(double sx, double
 * sy) scale()}, {@link WTransform#shear(double sh, double sv) shear()} or {@link
 * WTransform#translate(double dx, double dy) translate()} this coordinate system.
 *
 * <p>There are also 2 methods to decompose an arbitrary matrix into elementary operations:
 *
 * <ul>
 *   <li>{@link WTransform#decomposeTranslateRotateScaleSkew(WTransform.TRSSDecomposition result)
 *       decomposeTranslateRotateScaleSkew()} decomposes into a <i>T</i> &amp;#x2218; <i>R</i>
 *       &amp;#x2218; <i>Sxx</i> &amp;#x2218; <i>Sxy</i>
 *   <li>{@link WTransform#decomposeTranslateRotateScaleRotate(WTransform.TRSRDecomposition result)
 *       decomposeTranslateRotateScaleRotate()} decomposes into a <i>T</i> &amp;#x2218; <i>R1</i>
 *       &amp;#x2218; <i>Sxx</i> &amp;#x2218; <i>R2</i>
 * </ul>
 *
 * <p>with <i>T</i> a translation, <i>R</i> a rotation, <i>Sxx</i> a scale, and <i>Sxy</i> a skew
 * component.
 *
 * <p>
 *
 * <h3>JavaScript exposability</h3>
 *
 * <p>A WTransform is JavaScript exposable. If a WTransform {@link
 * WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}, it can be accessed in your
 * custom JavaScript code through {@link WJavaScriptHandle#getJsRef() its handle&apos;s jsRef()}. A
 * transform is represented as a JavaScript array, e.g. a WTransform(m11, m12, m21, m22, dx, dy)
 * will be represented in JavaScript by:
 *
 * <pre>{@code
 * [m11, m12, m21, m22, dx, dy]
 *
 * }</pre>
 *
 * <p>As an exception to the general rule that {@link WJavaScriptExposableObject#isJavaScriptBound()
 * JavaScript bound} objects should not be modified, WTransform does support many modifications.
 * These modifications will then accumulate in the JavaScript representation of the transform.
 */
public class WTransform extends WJavaScriptExposableObject {
  private static Logger logger = LoggerFactory.getLogger(WTransform.class);

  /**
   * Default constructor.
   *
   * <p>Creates the identity transformation matrix.
   */
  public WTransform() {
    super();
    this.reset();
  }
  /**
   * Construct a custom matrix by specifying the parameters.
   *
   * <p>Creates a matrix from the specified parameters.
   */
  public WTransform(double m11, double m12, double m21, double m22, double dx, double dy) {
    super();
    this.m_[M11] = m11;
    this.m_[M12] = m21;
    this.m_[M13] = dx;
    this.m_[M21] = m12;
    this.m_[M22] = m22;
    this.m_[M23] = dy;
  }
  /** Copy constructor. */
  public WTransform(final WTransform other) {
    super(other);
    for (int i = 0; i < 6; ++i) {
      this.m_[i] = other.m_[i];
    }
  }
  // public WTransform assign(final WJavaScriptExposableObject rhs) ;
  /**
   * Assignment method.
   *
   * <p>Copies the transformation from the <code>rhs</code>.
   */
  public WTransform assign(final WTransform rhs) {
    if (rhs.isJavaScriptBound()) {
      this.assignBinding(rhs);
    } else {
      this.clientBinding_ = null;
    }
    for (int i = 0; i < 6; ++i) {
      this.m_[i] = rhs.m_[i];
    }
    return this;
  }
  /**
   * Clone method.
   *
   * <p>Clones this {@link WTransform} object.
   */
  public WTransform clone() {
    WTransform result = new WTransform();
    result.assign(this);
    return result;
  }
  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * <p>Returns <code>true</code> if the transforms are exactly the same.
   */
  public boolean equals(final WTransform rhs) {
    if (!this.sameBindingAs(rhs)) {
      return false;
    }
    for (int i = 0; i < 6; ++i) {
      if (this.m_[i] != rhs.m_[i]) {
        return false;
      }
    }
    return true;
  }
  /**
   * Identity check.
   *
   * <p>Returns true if the transform represents an identity transformation.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This is always false if the transform is {@link
   * WJavaScriptExposableObject#isJavaScriptBound() JavaScript bound}. </i>
   */
  public boolean isIdentity() {
    return !this.isJavaScriptBound()
        && this.m_[M11] == 1.0
        && this.m_[M22] == 1.0
        && this.m_[M21] == 0.0
        && this.m_[M12] == 0.0
        && this.m_[M13] == 0.0
        && this.m_[M23] == 0.0;
  }
  /** Returns the horizontal scaling factor. */
  public double getM11() {
    return this.m_[M11];
  }
  /** Returns the vertical shearing factor. */
  public double getM12() {
    return this.m_[M21];
  }
  /** Returns m13 = 0. */
  public double getM13() {
    return 0;
  }
  /** Returns the horizontal shearing factor. */
  public double getM21() {
    return this.m_[M12];
  }
  /** Returns the vertical scaling factor. */
  public double getM22() {
    return this.m_[M22];
  }
  /** Returns m23 = 0. */
  public double getM23() {
    return 0;
  }
  /**
   * Returns the horizontal translation factor.
   *
   * <p>Is equivalent to {@link WTransform#getDx() getDx()}
   */
  public double getM31() {
    return this.m_[M13];
  }
  /**
   * Returns the vertical translation factor.
   *
   * <p>Is equivalent to {@link WTransform#getDy() getDy()}
   */
  public double getM32() {
    return this.m_[M23];
  }
  /** Returns m33 = 1. */
  public double getM33() {
    return 1;
  }
  /**
   * Returns the horizontal translation factor.
   *
   * <p>Is equivalent to {@link WTransform#getM31() getM31()}
   */
  public double getDx() {
    return this.m_[DX];
  }
  /**
   * Returns the vertical translation factor.
   *
   * <p>Is equivalent to {@link WTransform#getM32() getM32()}
   */
  public double getDy() {
    return this.m_[DY];
  }
  /**
   * Applys the transformation to a point.
   *
   * <p>Returns the transformed point.
   *
   * <p>
   *
   * <p><i><b>Note: </b>If this transform or the given point {@link
   * WJavaScriptExposableObject#isJavaScriptBound() are JavaScript bound}, the resulting point will
   * also be JavaScript bound. </i>
   *
   * @see WTransform#map(double x, double y, Double tx, Double ty)
   */
  public WPointF map(final WPointF p) {
    if (this.isIdentity()) {
      return p;
    }
    double x;
    double y;
    x = this.m_[M11] * p.getX() + this.m_[M12] * p.getY() + this.m_[M13];
    y = this.m_[M21] * p.getX() + this.m_[M22] * p.getY() + this.m_[M23];
    ;
    WPointF result = new WPointF(x, y);
    if (this.isJavaScriptBound() || p.isJavaScriptBound()) {
      WJavaScriptExposableObject o = this;
      if (p.isJavaScriptBound()) {
        o = p;
      }
      result.assignBinding(
          o, "Wt4.11.2.gfxUtils.transform_mult(" + this.getJsRef() + ',' + p.getJsRef() + ')');
    }
    return result;
  }
  /**
   * Applys the transformation to a point.
   *
   * <p>Sets the point (<i>tx</i>, <code>ty</code>) to the transformation of the point (<i>x</i>,
   * <code>y</code>).
   *
   * <p>
   *
   * @see WTransform#map(WPointF p)
   */
  public void map(double x, double y, Double tx, Double ty) {
    tx = this.m_[M11] * x + this.m_[M12] * y + this.m_[M13];
    ty = this.m_[M21] * x + this.m_[M22] * y + this.m_[M23];
  }
  /**
   * Applies the transformation to a rectangle.
   *
   * <p>Since the rectangle is aligned with X and Y axes, this may increase the size of the
   * rectangle even for a transformation that only rotates.
   *
   * <p>
   *
   * <p><i><b>Note: </b>If this transform or the given rectangle {@link
   * WJavaScriptExposableObject#isJavaScriptBound() are JavaScript bound}, the resulting rectangle
   * will also be JavaScript bound. </i>
   */
  public WRectF map(final WRectF rect) {
    if (this.isIdentity()) {
      return rect;
    }
    double minX;
    double minY;
    double maxX;
    double maxY;
    WPointF p = this.map(rect.getTopLeft());
    minX = maxX = p.getX();
    minY = maxY = p.getY();
    for (int i = 0; i < 3; ++i) {
      WPointF p2 =
          this.map(
              i == 0 ? rect.getBottomLeft() : i == 1 ? rect.getTopRight() : rect.getBottomRight());
      minX = Math.min(minX, p2.getX());
      maxX = Math.max(maxX, p2.getX());
      minY = Math.min(minY, p2.getY());
      maxY = Math.max(maxY, p2.getY());
    }
    WRectF result = new WRectF(minX, minY, maxX - minX, maxY - minY);
    if (this.isJavaScriptBound() || rect.isJavaScriptBound()) {
      WJavaScriptExposableObject o = this;
      if (rect.isJavaScriptBound()) {
        o = rect;
      }
      result.assignBinding(
          o, "Wt4.11.2.gfxUtils.transform_mult(" + this.getJsRef() + ',' + rect.getJsRef() + ')');
    }
    return result;
  }
  /**
   * Applies the transformation to a painter path.
   *
   * <p>This will transform all individual points according to the transformation. The radius of
   * arcs will be unaffected.
   *
   * <p>
   *
   * <p><i><b>Note: </b>If this transform or the given path {@link
   * WJavaScriptExposableObject#isJavaScriptBound() are JavaScript bound}, the resulting path will
   * also be JavaScript bound. </i>
   */
  public WPainterPath map(final WPainterPath path) {
    if (this.isIdentity()) {
      return path;
    }
    WPainterPath result = new WPainterPath();
    if (this.isJavaScriptBound() || path.isJavaScriptBound()) {
      WJavaScriptExposableObject o = this;
      if (!this.isJavaScriptBound()) {
        o = path;
      }
      result.assignBinding(
          o, "Wt4.11.2.gfxUtils.transform_apply(" + this.getJsRef() + ',' + path.getJsRef() + ')');
    }
    final List<WPainterPath.Segment> sourceSegments = path.getSegments();
    for (int i = 0; i < sourceSegments.size(); ++i) {
      double tx;
      double ty;
      if (sourceSegments.get(i).getType() == SegmentType.ArcR
          || sourceSegments.get(i).getType() == SegmentType.ArcAngleSweep) {
        result.segments_.add(sourceSegments.get(i));
      } else {
        tx =
            this.m_[M11] * sourceSegments.get(i).getX()
                + this.m_[M12] * sourceSegments.get(i).getY()
                + this.m_[M13];
        ty =
            this.m_[M21] * sourceSegments.get(i).getX()
                + this.m_[M22] * sourceSegments.get(i).getY()
                + this.m_[M23];
        ;
        result.segments_.add(new WPainterPath.Segment(tx, ty, sourceSegments.get(i).getType()));
      }
    }
    return result;
  }
  /**
   * Resets the transformation to the identity.
   *
   * <p>
   *
   * @exception {@link WException} if the transform {@link
   *     WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}
   * @see WTransform#isIdentity()
   * @see WTransform#WTransform()
   */
  public void reset() {
    this.checkModifiable();
    this.m_[M11] = this.m_[M22] = 1;
    this.m_[M21] = this.m_[M12] = this.m_[M13] = this.m_[M23] = 0;
  }
  /**
   * Rotates the transformation.
   *
   * <p>Applies a clock-wise rotation to the current transformation matrix, over <code>angle</code>
   * degrees.
   *
   * <p>
   *
   * @see WTransform#rotateRadians(double angle)
   */
  public WTransform rotate(double angle) {
    this.rotateRadians(degreesToRadians(angle));
    return this;
  }
  /**
   * Rotates the transformation.
   *
   * <p>Applies a clock-wise rotation to the current transformation matrix, over <code>angle</code>
   * radians.
   *
   * <p>
   *
   * @see WTransform#rotate(double angle)
   */
  public WTransform rotateRadians(double angle) {
    double r11 = Math.cos(angle);
    double r12 = -Math.sin(angle);
    double r21 = -r12;
    double r22 = r11;
    return this.multiplyAndAssign(new WTransform(r11, r21, r12, r22, 0, 0));
  }
  /**
   * Scales the transformation.
   *
   * <p>Scales the current transformation.
   *
   * <p>
   *
   * @see WTransform#shear(double sh, double sv)
   */
  public WTransform scale(double sx, double sy) {
    return this.multiplyAndAssign(new WTransform(sx, 0, 0, sy, 0, 0));
  }
  /**
   * Shears the transformation.
   *
   * <p>Shears the current transformation.
   *
   * <p>
   *
   * @see WTransform#scale(double sx, double sy)
   * @see WTransform#rotate(double angle)
   */
  public WTransform shear(double sh, double sv) {
    return this.multiplyAndAssign(new WTransform(0, sv, sh, 0, 0, 0));
  }
  /**
   * Translates the transformation.
   *
   * <p>Translates the current transformation.
   */
  public WTransform translate(double dx, double dy) {
    return this.multiplyAndAssign(new WTransform(1, 0, 0, 1, dx, dy));
  }
  /**
   * Translates the transformation.
   *
   * <p>Translates the current transformation.
   *
   * <p>
   *
   * <p><i><b>Note: </b>If this transform or the given point {@link
   * WJavaScriptExposableObject#isJavaScriptBound() are JavaScript bound}, the resulting transform
   * will also be JavaScript bound. </i>
   */
  public WTransform translate(final WPointF p) {
    boolean identity = this.isIdentity();
    String refBefore = this.getJsRef();
    this.translate(p.getX(), p.getY());
    if (this.isJavaScriptBound() || p.isJavaScriptBound()) {
      WJavaScriptExposableObject o = this;
      if (!this.isJavaScriptBound()) {
        o = p;
      }
      if (identity) {
        this.assignBinding(
            o, "((function(){var p=" + p.getJsRef() + ";return [1,0,0,1,p[0],p[1]];})())");
      } else {
        this.assignBinding(
            o,
            "Wt4.11.2.gfxUtils.transform_mult((function(){var p="
                + p.getJsRef()
                + ";return [1,0,0,1,p[0],p[1]];})(),("
                + refBefore
                + "))");
      }
    }
    return this;
  }
  /** Adds a transform that is conceptually applied after this transform. */
  public WTransform multiplyAndAssign(final WTransform Y) {
    if (this.isIdentity()) {
      return this.assign(Y);
    }
    if (Y.isIdentity()) {
      return this;
    }
    final WTransform X = this;
    if (this.isJavaScriptBound() || Y.isJavaScriptBound()) {
      WJavaScriptExposableObject o = this;
      if (!this.isJavaScriptBound()) {
        o = Y;
      }
      this.assignBinding(
          o, "Wt4.11.2.gfxUtils.transform_mult(" + this.getJsRef() + ',' + Y.getJsRef() + ')');
    }
    double z11 = X.m_[M11] * Y.m_[M11] + X.m_[M12] * Y.m_[M21];
    double z12 = X.m_[M11] * Y.m_[M12] + X.m_[M12] * Y.m_[M22];
    double z13 = X.m_[M11] * Y.m_[M13] + X.m_[M12] * Y.m_[M23] + X.m_[M13];
    double z21 = X.m_[M21] * Y.m_[M11] + X.m_[M22] * Y.m_[M21];
    double z22 = X.m_[M21] * Y.m_[M12] + X.m_[M22] * Y.m_[M22];
    double z23 = X.m_[M21] * Y.m_[M13] + X.m_[M22] * Y.m_[M23] + X.m_[M23];
    this.m_[M11] = z11;
    this.m_[M12] = z12;
    this.m_[M13] = z13;
    this.m_[M21] = z21;
    this.m_[M22] = z22;
    this.m_[M23] = z23;
    return this;
  }
  /** Multiply 2 transform objects. */
  public WTransform multiply(final WTransform rhs) {
    WTransform result = new WTransform();
    result.assign(this);
    return result.multiplyAndAssign(rhs);
  }
  /** Returns the determinant. */
  public double getDeterminant() {
    return this.getM11() * (this.getM33() * this.getM22() - this.getM32() * this.getM23())
        - this.getM21() * (this.getM33() * this.getM12() - this.getM32() * this.getM13())
        + this.getM31() * (this.getM23() * this.getM12() - this.getM22() * this.getM13());
  }
  /** Returns the adjoint. */
  public WTransform getAdjoint() {
    WTransform res =
        new WTransform(
            this.getM33() * this.getM22() - this.getM32() * this.getM23(),
            -(this.getM33() * this.getM12() - this.getM32() * this.getM13()),
            -(this.getM33() * this.getM21() - this.getM31() * this.getM23()),
            this.getM33() * this.getM11() - this.getM31() * this.getM13(),
            this.getM32() * this.getM21() - this.getM31() * this.getM22(),
            -(this.getM32() * this.getM11() - this.getM31() * this.getM12()));
    if (this.isJavaScriptBound()) {
      res.assignBinding(this, "Wt4.11.2.gfxUtils.transform_adjoint(" + this.getJsRef() + ")");
    }
    return res;
  }
  /**
   * Returns the inverted transformation.
   *
   * <p>Returns <code>this</code> if the transformation could not be inverted ({@link
   * WTransform#getDeterminant() getDeterminant()} == 0), and logs an error instead.
   */
  public WTransform getInverted() {
    double det = this.getDeterminant();
    if (det != 0) {
      WTransform adj = this.getAdjoint();
      WTransform res =
          new WTransform(
              adj.getM11() / det,
              adj.getM12() / det,
              adj.getM21() / det,
              adj.getM22() / det,
              adj.getM31() / det,
              adj.getM32() / det);
      if (this.isJavaScriptBound()) {
        res.assignBinding(this, "Wt4.11.2.gfxUtils.transform_inverted(" + this.getJsRef() + ")");
      }
      return res;
    } else {
      logger.error(new StringWriter().append("inverted(): oops, determinant == 0").toString());
      return this;
    }
  }
  /**
   * Result of a TRSS decomposition.
   *
   * <p>
   *
   * @see WTransform#decomposeTranslateRotateScaleSkew(WTransform.TRSSDecomposition result)
   */
  public static class TRSSDecomposition {
    private static Logger logger = LoggerFactory.getLogger(TRSSDecomposition.class);

    /** X component of translation. */
    public double dx;
    /** Y component of translation. */
    public double dy;
    /** Rotation angle (radians) */
    public double alpha;
    /** X component of scale. */
    public double sx;
    /** Y component of scale. */
    public double sy;
    /** Shear (in Y direction) */
    public double sh;
  }
  /**
   * Decomposes the transformation.
   *
   * <p>Decomposes the transformation into elementary operations: translation (<i>dx</i>, <code>dy
   * </code>), followed by rotation (<i>alpha</i>), followed by scale (<i>sx</i>, <code>sy</code>)
   * and vertical shearing factor (<code>sh</code>). The angle is expressed in radians.
   *
   * <p>This performs a <a href="http://en.wikipedia.org/wiki/Gram_schmidt">Gram-Schmidt
   * orthonormalization</a>.
   */
  public void decomposeTranslateRotateScaleSkew(final WTransform.TRSSDecomposition result) {
    double[] q1 = new double[2];
    double r11 = norm(this.m_[M11], this.m_[M21]);
    q1[0] = this.m_[M11] / r11;
    q1[1] = this.m_[M21] / r11;
    double r12 = this.m_[M12] * q1[0] + this.m_[M22] * q1[1];
    double r22 = norm(this.m_[M12] - r12 * q1[0], this.m_[M22] - r12 * q1[1]);
    result.alpha = Math.atan2(q1[1], q1[0]);
    result.sx = r11;
    result.sy = r22;
    result.sh = r12 / r11;
    result.dx = this.m_[DX];
    result.dy = this.m_[DY];
  }
  /**
   * Result of a TRSR decomposition.
   *
   * <p>
   *
   * @see WTransform#decomposeTranslateRotateScaleRotate(WTransform.TRSRDecomposition result)
   */
  public static class TRSRDecomposition {
    private static Logger logger = LoggerFactory.getLogger(TRSRDecomposition.class);

    /** X component of translation. */
    public double dx;
    /** Y component of translation. */
    public double dy;
    /** First rotation angle (radians) */
    public double alpha1;
    /** X component of scale. */
    public double sx;
    /** Y component of scale. */
    public double sy;
    /** Second rotation angle (radians) */
    public double alpha2;
  }
  /**
   * Decomposes the transformation.
   *
   * <p>Decomposes the transformation into elementary operations: translation (<i>dx</i>, <code>dy
   * </code>), followed by rotation (<i>alpha2</i>), followed by scale (<i>sx</i>, <code>sy</code>)
   * and again a rotation (<code>alpha2</code>). The angles are expressed in radians.
   *
   * <p>This performs a <a href="http://en.wikipedia.org/wiki/Singular_value_decomposition">Singular
   * Value Decomposition (SVD)</a>.
   */
  public void decomposeTranslateRotateScaleRotate(final WTransform.TRSRDecomposition result) {
    double[] mtm = new double[4];
    logger.debug(
        new StringWriter()
            .append("M: \n")
            .append(String.valueOf(this.m_[M11]))
            .append(" ")
            .append(String.valueOf(this.m_[M12]))
            .append("\n   ")
            .append(String.valueOf(this.m_[M21]))
            .append(" ")
            .append(String.valueOf(this.m_[M22]))
            .toString());
    matrixMultiply(
        this.m_[M11],
        this.m_[M21],
        this.m_[M12],
        this.m_[M22],
        this.m_[M11],
        this.m_[M12],
        this.m_[M21],
        this.m_[M22],
        mtm);
    double[] e = new double[2];
    double[] V = new double[4];
    eigenValues(mtm, e, V);
    result.sx = Math.sqrt(e[0]);
    result.sy = Math.sqrt(e[1]);
    logger.debug(
        new StringWriter()
            .append("V: \n")
            .append(String.valueOf(V[M11]))
            .append(" ")
            .append(String.valueOf(V[M12]))
            .append("\n   ")
            .append(String.valueOf(V[M21]))
            .append(" ")
            .append(String.valueOf(V[M22]))
            .toString());
    if (V[0] * V[3] - V[1] * V[2] < 0) {
      result.sx = -result.sx;
      V[0] = -V[0];
      V[2] = -V[2];
    }
    double[] U = new double[4];
    matrixMultiply(this.m_[0], this.m_[1], this.m_[2], this.m_[3], V[0], V[1], V[2], V[3], U);
    U[0] /= result.sx;
    U[2] /= result.sx;
    U[1] /= result.sy;
    U[3] /= result.sy;
    logger.debug(
        new StringWriter()
            .append("U: \n")
            .append(String.valueOf(U[M11]))
            .append(" ")
            .append(String.valueOf(U[M12]))
            .append("\n   ")
            .append(String.valueOf(U[M21]))
            .append(" ")
            .append(String.valueOf(U[M22]))
            .toString());
    if (U[0] * U[3] - U[1] * U[2] < 0) {
      result.sx = -result.sx;
      U[0] = -U[0];
      U[2] = -U[2];
    }
    result.alpha1 = Math.atan2(U[2], U[0]);
    result.alpha2 = Math.atan2(V[1], V[0]);
    logger.debug(
        new StringWriter()
            .append("alpha1: ")
            .append(String.valueOf(result.alpha1))
            .append(", alpha2: ")
            .append(String.valueOf(result.alpha2))
            .append(", sx: ")
            .append(String.valueOf(result.sx))
            .append(", sy: ")
            .append(String.valueOf(result.sy))
            .toString());
    result.dx = this.m_[DX];
    result.dy = this.m_[DY];
  }
  /** Utility method to convert degrees to radians. */
  public static double degreesToRadians(double angle) {
    return angle / 180. * 3.14159265358979323846;
  }
  /**
   * A constant that represents the identity transform.
   *
   * <p>
   *
   * @see WTransform#isIdentity()
   */
  public static final WTransform Identity = new WTransform();

  public boolean closeTo(final WTransform other) {
    if (this.isJavaScriptBound() || other.isJavaScriptBound()) {
      return false;
    }
    return Math.abs(this.m_[0] - other.m_[0]) <= EPS
        && Math.abs(this.m_[1] - other.m_[1]) <= EPS
        && Math.abs(this.m_[2] - other.m_[2]) <= EPS
        && Math.abs(this.m_[3] - other.m_[3]) <= EPS
        && Math.abs(this.m_[4] - other.m_[4]) <= EPS
        && Math.abs(this.m_[5] - other.m_[5]) <= EPS;
  }

  public String getJsValue() {
    char[] buf = new char[30];
    StringBuilder ss = new StringBuilder();
    ss.append('[');
    ss.append(MathUtils.roundJs(this.m_[0], 16)).append(',');
    ss.append(MathUtils.roundJs(this.m_[2], 16)).append(',');
    ss.append(MathUtils.roundJs(this.m_[1], 16)).append(',');
    ss.append(MathUtils.roundJs(this.m_[3], 16)).append(',');
    ss.append(MathUtils.roundJs(this.m_[4], 16)).append(',');
    ss.append(MathUtils.roundJs(this.m_[5], 16)).append(']');
    return ss.toString();
  }

  protected void assignFromJSON(final com.google.gson.JsonElement value) {
    try {
      final com.google.gson.JsonArray ar = (com.google.gson.JsonArray) value;
      if (ar.size() == 6
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(0)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(1)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(2)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(3)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(4)))
          && !JsonUtils.isNull(JsonUtils.toNumber(ar.get(5)))) {
        for (int i = 0; i < 6; ++i) {
          this.m_[i] = JsonUtils.orIfNullDouble(JsonUtils.toNumber(ar.get(i)), this.m_[i]);
        }
      } else {
        logger.error(new StringWriter().append("Couldn't convert JSON to WTransform").toString());
      }
    } catch (final RuntimeException e) {
      logger.error(
          new StringWriter()
              .append("Couldn't convert JSON to WTransform: " + e.toString())
              .toString());
    }
  }

  private static final int M11 = 0;
  private static final int M12 = 1;
  private static final int M21 = 2;
  private static final int M22 = 3;
  private static final int M13 = 4;
  private static final int DX = 4;
  private static final int M23 = 5;
  private static final int DY = 5;
  private double[] m_ = new double[6];
  private static final double EPS = 1E-12;

  static double norm(double x1, double x2) {
    return Math.sqrt(x1 * x1 + x2 * x2);
  }

  static void matrixMultiply(
      double a11,
      double a12,
      double a21,
      double a22,
      double b11,
      double b12,
      double b21,
      double b22,
      double[] result) {
    result[0] = a11 * b11 + a12 * b21;
    result[1] = a11 * b12 + a12 * b22;
    result[2] = a21 * b11 + a22 * b21;
    result[3] = a21 * b12 + a22 * b22;
  }

  static void eigenValues(double[] m, double[] l, double[] v) {
    final double a = m[0];
    final double b = m[1];
    final double c = m[2];
    final double d = m[3];
    double B = -a - d;
    double C = a * d - b * c;
    double Dsqr = B * B - 4 * C;
    if (Dsqr <= 0) {
      Dsqr = 0;
    }
    double D = Math.sqrt(Dsqr);
    l[0] = -(B + (B < 0 ? -D : D)) / 2.0;
    l[1] = -B - l[0];
    if (Math.abs(l[0] - l[1]) < 1E-5) {
      v[0] = 1;
      v[2] = 0;
      v[1] = 0;
      v[3] = 1;
    } else {
      if (Math.abs(c) > 1E-5) {
        v[0] = d - l[0];
        v[2] = -c;
        v[1] = d - l[1];
        v[3] = -c;
      } else {
        if (Math.abs(b) > 1E-5) {
          v[0] = -b;
          v[2] = a - l[0];
          v[1] = -b;
          v[3] = a - l[1];
        } else {
          if (Math.abs(l[0] - a) < 1E-5) {
            v[0] = 1;
            v[2] = 0;
            v[1] = 0;
            v[3] = 1;
          } else {
            v[0] = 0;
            v[2] = 1;
            v[1] = 1;
            v[3] = 0;
          }
        }
      }
    }
    double v1l = Math.sqrt(v[0] * v[0] + v[2] * v[2]);
    v[0] /= v1l;
    v[2] /= v1l;
    double v2l = Math.sqrt(v[1] * v[1] + v[3] * v[3]);
    v[1] /= v2l;
    v[3] /= v2l;
  }
}
