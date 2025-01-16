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
 * A path defining a shape.
 *
 * <p>A painter path represents a (complex) path that may be composed of lines, arcs and bezier
 * curve segments, and painted onto a paint device using {@link WPainter#drawPath(WPainterPath path)
 * WPainter#drawPath()}.
 *
 * <p>The path that is composed in a painter path may consist of multiple closed sub-paths. Only the
 * last sub-path can be left open.
 *
 * <p>To compose a path, this class maintains a current position, which is the starting point for
 * the next drawing operation. An operation may draw a line (see {@link WPainterPath#lineTo(WPointF
 * point) lineTo()}), arc (see {@link WPainterPath#arcTo(double cx, double cy, double radius, double
 * startAngle, double sweepLength) arcTo()}), or bezier curve (see {@link
 * WPainterPath#quadTo(WPointF c, WPointF endPoint) quadTo()} and {@link
 * WPainterPath#cubicTo(WPointF c1, WPointF c2, WPointF endPoint) cubicTo()}) from the current
 * position to a new position. A new sub path may be started by moving the current position to a new
 * location (see {@link WPainterPath#moveTo(WPointF point) moveTo()}), which automatically closes
 * the previous sub path.
 *
 * <p>When sub paths overlap, the result is undefined (it is dependent on the underlying painting
 * device).
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * WPainter painter = new WPainter();
 *
 * WPainterPath path = new WPainterPath(new WPointF(10, 10));
 * path.lineTo(10, 20);
 * path.lineTo(30, 20);
 * path.closeSubPath();
 *
 * painter.setPen(new WPen(WColor.red));
 * painter.setBrush(new WBrush(WColor.blue));
 * painter.drawPath(path);
 *
 * }</pre>
 *
 * <p>
 *
 * <h3>JavaScript exposability</h3>
 *
 * <p>A WPainterPath is JavaScript exposable. If a WPainterPath {@link
 * WJavaScriptExposableObject#isJavaScriptBound() is JavaScript bound}, it can be accessed in your
 * custom JavaScript code through {@link WJavaScriptHandle#getJsRef() its handle&apos;s jsRef()}.
 *
 * <p>A WPainterPath is represented in JavaScript as an array of segments, where each segment is
 * defined by a three element array: [x,y,type], where type is the integer representation of the
 * type of a segment.
 *
 * <p>For example, a 10 by 10 square with the top left at (10,10) is represented as:
 *
 * <pre>{@code
 * [
 * [10,10,0], // move to (10,10)
 * [20,10,1], // line to (20,10)
 * [20,20,1], // line to (20,20)
 * [10,20,1], // line to (10,20)
 * [10,10,1]  // line to (10,10)
 * ]
 *
 * }</pre>
 *
 * <p>
 *
 * <p><i><b>Warning: </b>A WPainterPath that is JavaScript exposed should be modified only through
 * its {@link WJavaScriptHandle handle}. Any attempt at modifying it will cause an exception to be
 * thrown. </i>
 *
 * @see WPainter#drawPath(WPainterPath path)
 * @see WPaintedWidget#createJSPainterPath()
 */
public class WPainterPath extends WJavaScriptExposableObject {
  private static Logger logger = LoggerFactory.getLogger(WPainterPath.class);

  /**
   * Default constructor.
   *
   * <p>Creates an empty path, and sets the current position to (0, 0).
   */
  public WPainterPath() {
    super();
    this.isRect_ = false;
    this.openSubPathsEnabled_ = false;
    this.segments_ = new ArrayList<WPainterPath.Segment>();
  }
  /**
   * Construct a new path, and set the initial position.
   *
   * <p>Creates an empty path, and sets the current position to <code>startPoint</code>.
   */
  public WPainterPath(final WPointF startPoint) {
    super();
    this.isRect_ = false;
    this.openSubPathsEnabled_ = false;
    this.segments_ = new ArrayList<WPainterPath.Segment>();
    this.moveTo(startPoint);
  }
  /** Copy constructor. */
  public WPainterPath(final WPainterPath path) {
    super(path);
    this.isRect_ = path.isRect_;
    this.openSubPathsEnabled_ = path.openSubPathsEnabled_;
    this.segments_ = new ArrayList<WPainterPath.Segment>();
    Utils.copyList(path.segments_, this.segments_);
  }
  /** Assignment method. */
  public WPainterPath assign(final WPainterPath path) {
    if (path.isJavaScriptBound()) {
      this.assignBinding(path);
    }
    Utils.copyList(path.segments_, this.segments_);
    this.isRect_ = path.isRect_;
    return this;
  }

  public WPainterPath clone() {
    return new WPainterPath(this);
  }
  /**
   * Returns the current position.
   *
   * <p>Returns the current position, which is the end point of the last move or draw operation, and
   * which well be the start point of the next draw operation.
   */
  public WPointF getCurrentPosition() {
    return this.getPositionAtSegment(this.segments_.size());
  }
  /**
   * Returns whether the path is empty.
   *
   * <p>Returns <code>true</code> if the path contains no drawing operations. Note that move
   * operations are not considered drawing operations.
   */
  public boolean isEmpty() {
    for (int i = 0; i < this.segments_.size(); ++i) {
      if (this.segments_.get(i).getType() != SegmentType.MoveTo) {
        return false;
      }
    }
    return true;
  }
  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * <p>Returns <code>true</code> if the paths are exactly the same.
   */
  public boolean equals(final WPainterPath path) {
    if (this.segments_.size() != path.segments_.size()) {
      return false;
    }
    for (int i = 0; i < this.segments_.size(); ++i) {
      if (!this.segments_.get(i).equals(path.segments_.get(i))) {
        return false;
      }
    }
    return true;
  }
  /**
   * Closes the last sub path.
   *
   * <p>Draws a line from the current position to the start position of the last sub path (which is
   * the end point of the last move operation), and sets the current position to (0, 0).
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   */
  public void closeSubPath() {
    this.checkModifiable();
    this.moveTo(0, 0);
  }
  /**
   * Moves the current position to a new location.
   *
   * <p>Moves the current position to a new point, implicitly closing the last sub path, unless
   * {@link WPainterPath#isOpenSubPathsEnabled() open subpaths are enabled}.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#closeSubPath()
   * @see WPainterPath#moveTo(double x, double y)
   * @see WPainterPath#setOpenSubPathsEnabled(boolean enabled)
   */
  public void moveTo(final WPointF point) {
    this.moveTo(point.getX(), point.getY());
  }
  /**
   * Moves the current position to a new location.
   *
   * <p>Moves the current position to a new point, implicitly closing the last sub path, unless
   * {@link WPainterPath#isOpenSubPathsEnabled() open subpaths are enabled}.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#closeSubPath()
   * @see WPainterPath#moveTo(WPointF point)
   * @see WPainterPath#setOpenSubPathsEnabled(boolean enabled)
   */
  public void moveTo(double x, double y) {
    this.checkModifiable();
    if (!this.openSubPathsEnabled_
        && !this.segments_.isEmpty()
        && this.segments_.get(this.segments_.size() - 1).getType() != SegmentType.MoveTo) {
      WPointF startP = this.getSubPathStart();
      WPointF currentP = this.getCurrentPosition();
      if (!startP.equals(currentP)) {
        this.lineTo(startP.getX(), startP.getY());
      }
    }
    this.segments_.add(new WPainterPath.Segment(x, y, SegmentType.MoveTo));
  }
  /**
   * Draws a straight line.
   *
   * <p>Draws a straight line from the current position to <code>point</code>, which becomes the new
   * current position.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#lineTo(double x, double y)
   */
  public void lineTo(final WPointF point) {
    this.lineTo(point.getX(), point.getY());
  }
  /**
   * Draws a straight line.
   *
   * <p>Draws a straight line from the current position to (<code>x</code>, <code>y</code>), which
   * becomes the new current position.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#lineTo(WPointF point)
   */
  public void lineTo(double x, double y) {
    this.checkModifiable();
    this.segments_.add(new WPainterPath.Segment(x, y, SegmentType.LineTo));
  }
  /**
   * Draws a cubic bezier curve.
   *
   * <p>Draws a cubic bezier curve from the current position to <code>endPoint</code>, which becomes
   * the new current position. The bezier curve uses the two control points <i>c1</i> and <code>c2
   * </code>.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#cubicTo(double c1x, double c1y, double c2x, double c2y, double endPointx,
   *     double endPointy)
   */
  public void cubicTo(final WPointF c1, final WPointF c2, final WPointF endPoint) {
    this.cubicTo(c1.getX(), c1.getY(), c2.getX(), c2.getY(), endPoint.getX(), endPoint.getY());
  }
  /**
   * Draws a cubic bezier curve.
   *
   * <p>This is an overloaded method provided for convenience.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#cubicTo(WPointF c1, WPointF c2, WPointF endPoint)
   */
  public void cubicTo(
      double c1x, double c1y, double c2x, double c2y, double endPointx, double endPointy) {
    this.checkModifiable();
    this.segments_.add(new WPainterPath.Segment(c1x, c1y, SegmentType.CubicC1));
    this.segments_.add(new WPainterPath.Segment(c2x, c2y, SegmentType.CubicC2));
    this.segments_.add(new WPainterPath.Segment(endPointx, endPointy, SegmentType.CubicEnd));
  }
  /**
   * Draws an arc.
   *
   * <p>Draws an arc which is a segment of a circle. The circle is defined with center (<i>cx</i>,
   * <i>cy</i>) and <code>radius</code>. The segment starts at <code>startAngle</code>, and spans an
   * angle given by <code>spanAngle</code>. These angles are expressed in degrees, and are measured
   * counter-clockwise starting from the 3 o&apos;clock position.
   *
   * <p>Implicitly draws a line from the current position to the start of the arc, if the current
   * position is different from the start.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#arcMoveTo(double cx, double cy, double radius, double angle)
   */
  public void arcTo(double cx, double cy, double radius, double startAngle, double sweepLength) {
    this.arcTo(cx - radius, cy - radius, radius * 2, radius * 2, startAngle, sweepLength);
  }
  /**
   * Moves to a point on an arc.
   *
   * <p>Moves to a point on a circle. The circle is defined with center (<i>cx</i>, <i>cy</i>) and
   * <code>radius</code>, and the point is at <code>angle</code> degrees measured counter-clockwise
   * starting from the 3 o&apos;clock position.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#arcTo(double cx, double cy, double radius, double startAngle, double
   *     sweepLength)
   */
  public void arcMoveTo(double cx, double cy, double radius, double angle) {
    this.moveTo(getArcPosition(cx, cy, radius, radius, angle));
  }
  /**
   * Move to a point on an arc.
   *
   * <p>Moves to a point on an ellipse. The ellipse fits in the rectangle defined by top left
   * position (<code>x</code>, <i>y</i>), and size <i>width</i> x <code>height</code>, and the point
   * is at <code>angle</code> degrees measured counter-clockwise starting from the 3 o&apos;clock
   * position.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#arcTo(double cx, double cy, double radius, double startAngle, double
   *     sweepLength)
   */
  public void arcMoveTo(double x, double y, double width, double height, double angle) {
    this.moveTo(getArcPosition(x + width / 2, y + height / 2, width / 2, height / 2, angle));
  }
  /**
   * Draws a quadratic bezier curve.
   *
   * <p>Draws a quadratic bezier curve from the current position to <code>endPoint</code>, which
   * becomes the new current position. The bezier curve uses the single control point <code>c</code>
   * .
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#quadTo(double cx, double cy, double endPointX, double endPointY)
   */
  public void quadTo(final WPointF c, final WPointF endPoint) {
    this.quadTo(c.getX(), c.getY(), endPoint.getX(), endPoint.getY());
  }
  /**
   * Draws a quadratic bezier curve.
   *
   * <p>This is an overloaded method provided for convenience.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#quadTo(WPointF c, WPointF endPoint)
   */
  public void quadTo(double cx, double cy, double endPointX, double endPointY) {
    this.checkModifiable();
    this.segments_.add(new WPainterPath.Segment(cx, cy, SegmentType.QuadC));
    this.segments_.add(new WPainterPath.Segment(endPointX, endPointY, SegmentType.QuadEnd));
  }
  /**
   * Draws an ellipse.
   *
   * <p>This method closes the current sub path, and adds an ellipse that is bounded by the
   * rectangle <code>boundingRectangle</code>.
   *
   * <p><code>Note:</code> some renderers only support circles (width == height)
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#addEllipse(double x, double y, double width, double height)
   * @see WPainterPath#arcTo(double cx, double cy, double radius, double startAngle, double
   *     sweepLength)
   */
  public void addEllipse(final WRectF rect) {
    this.addEllipse(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
  }
  /**
   * Draws an ellipse.
   *
   * <p>This method closes the current sub path, and adds an ellipse that is bounded by the
   * rectangle defined by top left position (<code>x</code>, <i>y</i>), and size <i>width</i> x
   * <code>height</code>.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Some renderers only support circles (width == height) </i>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#addEllipse(WRectF rect)
   * @see WPainterPath#arcTo(double cx, double cy, double radius, double startAngle, double
   *     sweepLength)
   */
  public void addEllipse(double x, double y, double width, double height) {
    this.moveTo(x + width, y + height / 2);
    this.arcTo(x, y, width, height, 0, 360);
  }
  /**
   * Draws a rectangle.
   *
   * <p>This method closes the current sub path, unless {@link WPainterPath#isOpenSubPathsEnabled()
   * open subpaths are enabled}, and adds a rectangle that is defined by <code>rectangle</code>.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#addRect(double x, double y, double width, double height)
   */
  public void addRect(final WRectF rectangle) {
    this.addRect(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
  }
  /**
   * Draws a rectangle.
   *
   * <p>This method closes the current sub path, unless {@link WPainterPath#isOpenSubPathsEnabled()
   * open subpaths are enabled}, and adds a rectangle that is defined by top left position
   * (<i>x</i>, <code>y</code>), and size <i>width</i> x <code>height</code>.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#addRect(WRectF rectangle)
   */
  public void addRect(double x, double y, double width, double height) {
    this.checkModifiable();
    if (this.isEmpty()) {
      this.isRect_ = true;
    }
    this.moveTo(x, y);
    this.lineTo(x + width, y);
    this.lineTo(x + width, y + height);
    this.lineTo(x, y + height);
    this.lineTo(x, y);
  }
  /**
   * Adds a polygon.
   *
   * <p>If the first point is different from the current position, the last sub path is first
   * closed, unless {@link WPainterPath#isOpenSubPathsEnabled() open subpaths are enabled},
   * otherwise the last sub path is extended with the polygon.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#moveTo(WPointF point)
   * @see WPainterPath#lineTo(WPointF point)
   */
  public void addPolygon(final List<WPointF> points) {
    this.checkModifiable();
    if (!points.isEmpty()) {
      int i = 0;
      if (!this.getCurrentPosition().equals(points.get(0))) {
        this.moveTo(points.get(i++));
      }
      for (; i < points.size(); ++i) {
        this.lineTo(points.get(i));
      }
    }
  }
  /**
   * Adds a path.
   *
   * <p>Adds an entire <code>path</code> to the current path. If the path&apos;s begin position is
   * different from the current position, the last sub path is first closed, unless {@link
   * WPainterPath#isOpenSubPathsEnabled() open subpaths are enabled}, otherwise the last sub path is
   * extended with the path&apos;s first sub path.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#connectPath(WPainterPath path)
   */
  public void addPath(final WPainterPath path) {
    this.checkModifiable();
    if (!this.getCurrentPosition().equals(path.getBeginPosition())) {
      this.moveTo(path.getBeginPosition());
    }
    this.segments_.addAll(path.segments_);
  }
  /**
   * Adds a path, connecting.
   *
   * <p>Adds an entire <code>path</code> to the current path. If the path&apos;s begin position is
   * different from the current position, the last sub path is first closed, unless {@link
   * WPainterPath#isOpenSubPathsEnabled() open subpaths are enabled}, otherwise the last sub path is
   * extended with the path&apos;s first sub path.
   *
   * <p>
   *
   * @exception {@link WException} if the path {@link WJavaScriptExposableObject#isJavaScriptBound()
   *     is JavaScript bound}
   * @see WPainterPath#connectPath(WPainterPath path)
   */
  public void connectPath(final WPainterPath path) {
    this.checkModifiable();
    if (!this.getCurrentPosition().equals(path.getBeginPosition())) {
      this.lineTo(path.getBeginPosition());
    }
    this.addPath(path);
  }
  /** A segment. */
  public static class Segment {
    private static Logger logger = LoggerFactory.getLogger(Segment.class);

    /**
     * The x parameter
     *
     * <p>Depending on the {@link WPainterPath.Segment#getType() getType()}, this is either the x
     * position of the point, or something else.
     */
    public double getX() {
      return this.x_;
    }
    /**
     * The y parameter
     *
     * <p>Depending on the {@link WPainterPath.Segment#getType() getType()}, this is either the y
     * position of the point, or something else.
     */
    public double getY() {
      return this.y_;
    }
    /** The type of the segment */
    public SegmentType getType() {
      return this.type_;
    }

    public boolean equals(final WPainterPath.Segment other) {
      return this.type_ == other.type_ && this.x_ == other.x_ && this.y_ == other.y_;
    }

    Segment(double x, double y, SegmentType type) {
      this.x_ = x;
      this.y_ = y;
      this.type_ = type;
    }

    private double x_;
    private double y_;
    private SegmentType type_;
    // private WPainterPath map(final WPainterPath path) ;
  }

  public List<WPainterPath.Segment> getSegments() {
    return this.segments_;
  }

  WPointF getPositionAtSegment(int index) {
    if (index > 0) {
      final WPainterPath.Segment s = this.segments_.get(index - 1);
      switch (s.getType()) {
        case MoveTo:
        case LineTo:
        case CubicEnd:
        case QuadEnd:
          return new WPointF(s.getX(), s.getY());
        case ArcAngleSweep:
          {
            int i = this.segments_.size() - 3;
            double cx = this.segments_.get(i).getX();
            double cy = this.segments_.get(i).getY();
            double rx = this.segments_.get(i + 1).getX();
            double ry = this.segments_.get(i + 1).getY();
            double theta1 = this.segments_.get(i + 2).getX();
            double deltaTheta = this.segments_.get(i + 2).getY();
            return getArcPosition(cx, cy, rx, ry, theta1 + deltaTheta);
          }
        default:
          assert false;
      }
    }
    return new WPointF(0, 0);
  }

  boolean asRect(final WRectF result) {
    if (this.isRect_) {
      if (this.segments_.size() == 4) {
        result.setX(0);
        result.setY(0);
        result.setWidth(this.segments_.get(0).getX());
        result.setHeight(this.segments_.get(1).getY());
        return true;
      } else {
        if (this.segments_.size() == 5 && this.segments_.get(0).getType() == SegmentType.MoveTo) {
          result.setX(this.segments_.get(0).getX());
          result.setY(this.segments_.get(0).getY());
          result.setWidth(this.segments_.get(1).getX() - this.segments_.get(0).getX());
          result.setHeight(this.segments_.get(2).getY() - this.segments_.get(0).getY());
          return true;
        } else {
          return false;
        }
      }
    } else {
      return false;
    }
  }
  /**
   * Returns the bounding box of the control points.
   *
   * <p>Returns the bounding box of all control points. This is guaranteed to be a superset of the
   * actual bounding box.
   *
   * <p>The <code>transform</code> is applied to the path first.
   */
  public WRectF getControlPointRect(final WTransform transform) {
    if (this.isEmpty()) {
      return null;
    } else {
      boolean identity = transform.isIdentity();
      double minX;
      double minY;
      double maxX;
      double maxY;
      minX = minY = Double.MAX_VALUE;
      maxX = maxY = Double.MIN_VALUE;
      for (int i = 0; i < this.segments_.size(); ++i) {
        final WPainterPath.Segment s = this.segments_.get(i);
        switch (s.getType()) {
          case MoveTo:
          case LineTo:
          case CubicC1:
          case CubicC2:
          case CubicEnd:
          case QuadC:
          case QuadEnd:
            {
              if (identity) {
                minX = Math.min(s.getX(), minX);
                minY = Math.min(s.getY(), minY);
                maxX = Math.max(s.getX(), maxX);
                maxY = Math.max(s.getY(), maxY);
              } else {
                WPointF p = transform.map(new WPointF(s.getX(), s.getY()));
                minX = Math.min(p.getX(), minX);
                minY = Math.min(p.getY(), minY);
                maxX = Math.max(p.getX(), maxX);
                maxY = Math.max(p.getY(), maxY);
              }
              break;
            }
          case ArcC:
            {
              final WPainterPath.Segment s2 = this.segments_.get(i + 1);
              if (identity) {
                WPointF tl = new WPointF(s.getX() - s2.getX(), s.getY() - s2.getY());
                minX = Math.min(tl.getX(), minX);
                minY = Math.min(tl.getY(), minY);
                WPointF br = new WPointF(s.getX() + s2.getX(), s.getY() + s2.getY());
                maxX = Math.max(br.getX(), maxX);
                maxY = Math.max(br.getY(), maxY);
              } else {
                WPointF p1 = transform.map(new WPointF(s.getX(), s.getY()));
                WPointF p2 = transform.map(new WPointF(s2.getX(), s2.getY()));
                WPointF tl = new WPointF(p1.getX() - p2.getX(), p1.getY() - p2.getY());
                minX = Math.min(tl.getX(), minX);
                minY = Math.min(tl.getY(), minY);
                WPointF br = new WPointF(p1.getX() + p2.getX(), p1.getY() + p2.getY());
                maxX = Math.max(br.getX(), maxX);
                maxY = Math.max(br.getY(), maxY);
              }
              i += 2;
              break;
            }
          default:
            assert false;
        }
      }
      return new WRectF(minX, minY, maxX - minX, maxY - minY);
    }
  }
  /**
   * Returns the bounding box of the control points.
   *
   * <p>Returns {@link #getControlPointRect(WTransform transform)
   * getControlPointRect(WTransform.Identity)}
   */
  public final WRectF getControlPointRect() {
    return getControlPointRect(WTransform.Identity);
  }
  /**
   * Returns a copy of the path where straight lines are moved to be rendered crisply.
   *
   * <p>This is intended to be used on rectangles, or other paths consisting of only straight line,
   * and will nudge every edge a little bit, so that 1px straight lines are rendered as a crisp
   * line.
   *
   * <p>This will also work if the path {@link WJavaScriptExposableObject#isJavaScriptBound() is
   * JavaScript bound}.
   */
  public WPainterPath getCrisp() {
    WPainterPath result = new WPainterPath();
    if (this.isJavaScriptBound()) {
      result.assignBinding(this, "Wt4.11.2.gfxUtils.path_crisp(" + this.getJsRef() + ')');
    }
    for (int i = 0; i < this.segments_.size(); ++i) {
      final WPainterPath.Segment segment = this.segments_.get(i);
      double hx = Math.floor(segment.getX()) + 0.5;
      double hy = Math.floor(segment.getY()) + 0.5;
      result.segments_.add(new WPainterPath.Segment(hx, hy, segment.getType()));
    }
    return result;
  }
  /**
   * Disables automatically closing subpaths on moveTo.
   *
   * <p>By default, open sub paths are disabled, and moveTo and any operation that relies on moveTo
   * will automatically close the last subpath. Enabling this option disables that feature.
   *
   * <p>
   *
   * @see WPainterPath#moveTo(WPointF point)
   * @see WPainterPath#addPath(WPainterPath path)
   * @see WPainterPath#connectPath(WPainterPath path)
   * @see WPainterPath#addRect(WRectF rectangle)
   */
  public void setOpenSubPathsEnabled(boolean enabled) {
    this.openSubPathsEnabled_ = enabled;
  }
  /**
   * Disables automatically closing subpaths on moveTo.
   *
   * <p>Calls {@link #setOpenSubPathsEnabled(boolean enabled) setOpenSubPathsEnabled(true)}
   */
  public final void setOpenSubPathsEnabled() {
    setOpenSubPathsEnabled(true);
  }
  /**
   * Returns whether open subpaths are enabled.
   *
   * <p>
   *
   * @see WPainterPath#setOpenSubPathsEnabled(boolean enabled)
   */
  public boolean isOpenSubPathsEnabled() {
    return this.openSubPathsEnabled_;
  }

  boolean isPointInPath(final WPointF p) {
    boolean res = false;
    double ax = 0.0;
    double ay = 0.0;
    double px = p.getX();
    double py = p.getY();
    for (int i = 0; i < this.segments_.size(); ++i) {
      double bx = ax;
      double by = ay;
      if (this.segments_.get(i).getType() == SegmentType.ArcC) {
        WPointF arcPos =
            getArcPosition(
                this.segments_.get(i).getX(),
                this.segments_.get(i).getY(),
                this.segments_.get(i + 1).getX(),
                this.segments_.get(i + 1).getY(),
                this.segments_.get(i + 2).getX());
        bx = arcPos.getX();
        by = arcPos.getY();
      } else {
        if (this.segments_.get(i).getType() == SegmentType.ArcAngleSweep) {
          WPointF arcPos =
              getArcPosition(
                  this.segments_.get(i - 2).getX(),
                  this.segments_.get(i - 2).getY(),
                  this.segments_.get(i - 1).getX(),
                  this.segments_.get(i - 1).getY(),
                  this.segments_.get(i).getX() + this.segments_.get(i).getY());
          bx = arcPos.getX();
          by = arcPos.getY();
        } else {
          if (this.segments_.get(i).getType() != SegmentType.ArcR) {
            bx = this.segments_.get(i).getX();
            by = this.segments_.get(i).getY();
          }
        }
      }
      if (this.segments_.get(i).getType() != SegmentType.MoveTo) {
        if (ay > py != by > py && px < (bx - ax) * (py - ay) / (by - ay) + ax) {
          res = !res;
        }
      }
      ax = bx;
      ay = by;
    }
    return res;
  }

  public String getJsValue() {
    char[] buf = new char[30];
    StringBuilder ss = new StringBuilder();
    ss.append('[');
    for (int i = 0; i < this.segments_.size(); ++i) {
      final WPainterPath.Segment s = this.segments_.get(i);
      if (i != 0) {
        ss.append(',');
      }
      ss.append('[');
      ss.append(MathUtils.roundJs(s.getX(), 3)).append(',');
      ss.append(MathUtils.roundJs(s.getY(), 3)).append(',');
      ss.append((int) s.getType().getValue()).append(']');
    }
    ss.append(']');
    return ss.toString();
  }

  protected void assignFromJSON(final com.google.gson.JsonElement value) {}

  private boolean isRect_;
  private boolean openSubPathsEnabled_;
  List<WPainterPath.Segment> segments_;

  private WPointF getSubPathStart() {
    for (int i = this.segments_.size() - 1; i >= 0; --i) {
      if (this.segments_.get(i).getType() == SegmentType.MoveTo) {
        return new WPointF(this.segments_.get(i).getX(), this.segments_.get(i).getY());
      }
    }
    return new WPointF(0, 0);
  }

  private WPointF getBeginPosition() {
    WPointF result = new WPointF(0, 0);
    for (int i = 0;
        i < this.segments_.size() && this.segments_.get(i).getType() == SegmentType.MoveTo;
        ++i) {
      result = new WPointF(this.segments_.get(i).getX(), this.segments_.get(i).getY());
    }
    return result;
  }

  private static WPointF getArcPosition(double cx, double cy, double rx, double ry, double angle) {
    double a = -degreesToRadians(angle);
    return new WPointF(cx + rx * Math.cos(a), cy + ry * Math.sin(a));
  }

  void arcTo(
      double x, double y, double width, double height, double startAngle, double sweepLength) {
    this.checkModifiable();
    this.segments_.add(new WPainterPath.Segment(x + width / 2, y + height / 2, SegmentType.ArcC));
    this.segments_.add(new WPainterPath.Segment(width / 2, height / 2, SegmentType.ArcR));
    this.segments_.add(
        new WPainterPath.Segment(startAngle, sweepLength, SegmentType.ArcAngleSweep));
  }
  // private WPainterPath map(final WPainterPath path) ;
  static double degreesToRadians(double r) {
    return r / 180. * 3.14159265358979323846;
  }
}
