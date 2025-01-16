/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.chart;

import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.auth.*;
import eu.webtoolkit.jwt.auth.mfa.*;
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
 * A widget for selecting an X axis range to display on an associated {@link
 * eu.webtoolkit.jwt.chart.WCartesianChart}.
 *
 * <p>
 *
 * <p><i><b>Note: </b>This widget currently only works with the HtmlCanvas rendering method. </i>
 */
public class WAxisSliderWidget extends WPaintedWidget {
  private static Logger logger = LoggerFactory.getLogger(WAxisSliderWidget.class);

  /**
   * Creates an axis slider widget.
   *
   * <p>Creates an axis slider widget that is not associated with a chart. Before it is used, a
   * chart should be assigned with setChart(), and a series column chosen with setSeriesColumn().
   */
  public WAxisSliderWidget(WContainerWidget parentContainer) {
    super();
    this.series_ = null;
    this.seriesPen_ = new WPen();
    this.selectedSeriesPen_ = this.seriesPen_;
    this.handleBrush_ = new WBrush(new WColor(0, 0, 200));
    this.background_ = new WBrush(new WColor(230, 230, 230));
    this.selectedAreaBrush_ = new WBrush(new WColor(255, 255, 255));
    this.autoPadding_ = false;
    this.labelsEnabled_ = true;
    this.yAxisZoomEnabled_ = true;
    this.transform_ = null;
    this.init();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates an axis slider widget.
   *
   * <p>Calls {@link #WAxisSliderWidget(WContainerWidget parentContainer)
   * this((WContainerWidget)null)}
   */
  public WAxisSliderWidget() {
    this((WContainerWidget) null);
  }
  /**
   * Creates an axis slider widget.
   *
   * <p>Creates an axis slider widget associated with the given data series of the given chart.
   */
  public WAxisSliderWidget(WDataSeries series, WContainerWidget parentContainer) {
    super();
    this.series_ = series;
    this.seriesPen_ = new WPen();
    this.selectedSeriesPen_ = this.seriesPen_;
    this.handleBrush_ = new WBrush(new WColor(0, 0, 200));
    this.background_ = new WBrush(new WColor(230, 230, 230));
    this.selectedAreaBrush_ = new WBrush(new WColor(255, 255, 255));
    this.autoPadding_ = false;
    this.labelsEnabled_ = true;
    this.yAxisZoomEnabled_ = true;
    this.transform_ = null;
    this.init();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates an axis slider widget.
   *
   * <p>Calls {@link #WAxisSliderWidget(WDataSeries series, WContainerWidget parentContainer)
   * this(series, (WContainerWidget)null)}
   */
  public WAxisSliderWidget(WDataSeries series) {
    this(series, (WContainerWidget) null);
  }
  /** Destructor. */
  public void remove() {
    if (this.getChart() != null) {
      this.getChart().removeAxisSliderWidget(this);
    }
    if (this.selectedSeriesPen_ != this.seriesPen_) {}

    super.remove();
  }

  public void setSeries(WDataSeries series) {
    if (this.series_ != series) {
      if (this.series_ != null) {
        this.getChart().removeAxisSliderWidget(this);
      }
      this.series_ = series;
      if (this.series_ != null) {
        this.getChart().addAxisSliderWidget(this);
      }
      this.update();
    }
  }
  /** Set the pen to draw the data series with. */
  public void setSeriesPen(final WPen pen) {
    if (!pen.equals(this.seriesPen_)) {
      this.seriesPen_ = pen;
      this.update();
    }
  }
  /** Returns the pen to draw the data series with. */
  public WPen getSeriesPen() {
    return this.seriesPen_;
  }
  /**
   * Set the pen to draw the selected part of the data series with.
   *
   * <p>If not set, this defaults to {@link WAxisSliderWidget#getSeriesPen() getSeriesPen()}.
   */
  public void setSelectedSeriesPen(final WPen pen) {
    if (this.selectedSeriesPen_ != this.seriesPen_) {}

    this.selectedSeriesPen_ = pen.clone();
    this.update();
  }
  /** Returns the pen to draw the selected part of the data series with. */
  public WPen getSelectedSeriesPen() {
    return this.selectedSeriesPen_;
  }
  /** Set the brush to draw the handles left and right of the selected area with. */
  public void setHandleBrush(final WBrush brush) {
    if (!brush.equals(this.handleBrush_)) {
      this.handleBrush_ = brush;
      this.update();
    }
  }
  /** Returns the brush to draw the handles left and right of the selected area with. */
  public WBrush getHandleBrush() {
    return this.handleBrush_;
  }
  /** Set the background brush. */
  public void setBackground(final WBrush brush) {
    if (!brush.equals(this.background_)) {
      this.background_ = brush;
      this.update();
    }
  }
  /** Returns the background brush. */
  public WBrush getBackground() {
    return this.background_;
  }
  /** Set the brush for the selected area. */
  public void setSelectedAreaBrush(final WBrush brush) {
    if (!brush.equals(this.selectedAreaBrush_)) {
      this.selectedAreaBrush_ = brush;
      this.update();
    }
  }
  /** Returns the brush for the selected area. */
  public WBrush getSelectedAreaBrush() {
    return this.selectedAreaBrush_;
  }
  /**
   * Sets an internal margin for the selection area.
   *
   * <p>This configures the area (in pixels) around the selection area that is available for the
   * axes and labels, and the handles.
   *
   * <p>Alternatively, you can configure the chart layout to be computed automatically using {@link
   * WAxisSliderWidget#setAutoLayoutEnabled(boolean enabled) setAutoLayoutEnabled()}.
   *
   * <p>
   *
   * @see WAxisSliderWidget#setAutoLayoutEnabled(boolean enabled)
   */
  public void setSelectionAreaPadding(int padding, EnumSet<Side> sides) {
    if (sides.contains(Side.Top)) {
      this.padding_[0] = padding;
    }
    if (sides.contains(Side.Right)) {
      this.padding_[1] = padding;
    }
    if (sides.contains(Side.Bottom)) {
      this.padding_[2] = padding;
    }
    if (sides.contains(Side.Left)) {
      this.padding_[3] = padding;
    }
  }
  /**
   * Sets an internal margin for the selection area.
   *
   * <p>Calls {@link #setSelectionAreaPadding(int padding, EnumSet sides)
   * setSelectionAreaPadding(padding, EnumSet.of(side, sides))}
   */
  public final void setSelectionAreaPadding(int padding, Side side, Side... sides) {
    setSelectionAreaPadding(padding, EnumSet.of(side, sides));
  }
  /**
   * Sets an internal margin for the selection area.
   *
   * <p>Calls {@link #setSelectionAreaPadding(int padding, EnumSet sides)
   * setSelectionAreaPadding(padding, Side.AllSides)}
   */
  public final void setSelectionAreaPadding(int padding) {
    setSelectionAreaPadding(padding, Side.AllSides);
  }
  /**
   * Returns the internal margin for the selection area.
   *
   * <p>This is either the padding set through {@link WAxisSliderWidget#setSelectionAreaPadding(int
   * padding, EnumSet sides) setSelectionAreaPadding()} or computed using {@link
   * WAxisSliderWidget#setAutoLayoutEnabled(boolean enabled) setAutoLayoutEnabled()}.
   *
   * <p>
   */
  public int getSelectionAreaPadding(Side side) {
    switch (side) {
      case Top:
        return this.padding_[0];
      case Right:
        return this.padding_[1];
      case Bottom:
        return this.padding_[2];
      case Left:
        return this.padding_[3];
      default:
        logger.error(
            new StringWriter().append("selectionAreaPadding(): improper side.").toString());
        return 0;
    }
  }
  /**
   * Configures the axis slider layout to be automatic.
   *
   * <p>This configures the selection area so that the space around it is suited for the text that
   * is rendered.
   */
  public void setAutoLayoutEnabled(boolean enabled) {
    this.autoPadding_ = enabled;
  }
  /**
   * Configures the axis slider layout to be automatic.
   *
   * <p>Calls {@link #setAutoLayoutEnabled(boolean enabled) setAutoLayoutEnabled(true)}
   */
  public final void setAutoLayoutEnabled() {
    setAutoLayoutEnabled(true);
  }
  /**
   * Returns whether chart layout is computed automatically.
   *
   * <p>
   *
   * @see WAxisSliderWidget#setAutoLayoutEnabled(boolean enabled)
   */
  public boolean isAutoLayoutEnabled() {
    return this.autoPadding_;
  }
  /**
   * Set whether to draw the X axis tick labels on the slider widget.
   *
   * <p>AxisProperty::Labels are enabled by default.
   */
  public void setLabelsEnabled(boolean enabled) {
    if (enabled != this.labelsEnabled_) {
      this.labelsEnabled_ = enabled;
      this.update();
    }
  }
  /**
   * Set whether to draw the X axis tick labels on the slider widget.
   *
   * <p>Calls {@link #setLabelsEnabled(boolean enabled) setLabelsEnabled(true)}
   */
  public final void setLabelsEnabled() {
    setLabelsEnabled(true);
  }
  /**
   * Returns whether the X axis tick labels are drawn.
   *
   * <p>
   *
   * @see WAxisSliderWidget#setLabelsEnabled(boolean enabled)
   */
  public boolean isLabelsEnabled() {
    return this.labelsEnabled_;
  }
  /**
   * Set whether the Y axis of the associated chart should be updated to fit the series.
   *
   * <p>Y axis zoom is enabled by default.
   */
  public void setYAxisZoomEnabled(boolean enabled) {
    if (enabled != this.yAxisZoomEnabled_) {
      this.yAxisZoomEnabled_ = enabled;
      this.update();
    }
  }
  /**
   * Set whether the Y axis of the associated chart should be updated to fit the series.
   *
   * <p>Calls {@link #setYAxisZoomEnabled(boolean enabled) setYAxisZoomEnabled(true)}
   */
  public final void setYAxisZoomEnabled() {
    setYAxisZoomEnabled(true);
  }
  /**
   * Returns whether the Y axis of the associated chart should be updated to fit the series.
   *
   * <p>
   *
   * @see WAxisSliderWidget#setYAxisZoomEnabled(boolean enabled)
   */
  public boolean isYAxisZoomEnabled() {
    return this.yAxisZoomEnabled_;
  }

  public WDataSeries getSeries() {
    return this.series_;
  }

  protected void render(EnumSet<RenderFlag> flags) {
    super.render(flags);
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WAxisSliderWidget.js", wtjs1());
  }

  protected void paintEvent(WPaintDevice paintDevice) {
    if (!(this.getChart() != null)) {
      logger.error(
          new StringWriter()
              .append("Attempted to draw a slider widget not associated with a chart.")
              .toString());
      return;
    }
    if (!this.getChart().cObjCreated_ || this.getChart().needsRerender()) {
      return;
    }
    if (this.series_.getType() != SeriesType.Line && this.series_.getType() != SeriesType.Curve) {
      if (this.getMethod() == RenderMethod.HtmlCanvas) {
        StringBuilder ss = new StringBuilder();
        ss.append("\ndelete ").append(this.getJsRef()).append(".wtSObj;");
        ss.append("\nif (")
            .append(this.getObjJsRef())
            .append(") {")
            .append(this.getObjJsRef())
            .append(".canvas.style.cursor = 'auto';")
            .append("setTimeout(")
            .append(this.getObjJsRef())
            .append(".repaint,0);}\n");
        this.doJavaScript(ss.toString());
      }
      logger.error(
          new StringWriter()
              .append("WAxisSliderWidget is not associated with a line or curve series.")
              .toString());
      return;
    }
    final WAxis xAxis = this.getChart().getXAxis(this.series_.getXAxis());
    final WCartesianChart.AxisStruct xAxisStruct =
        this.getChart().xAxes_.get(this.series_.getXAxis());
    WPainter painter = new WPainter(paintDevice);
    boolean horizontal = this.getChart().getOrientation() == Orientation.Vertical;
    double w = horizontal ? this.getWidth().getValue() : this.getHeight().getValue();
    double h = horizontal ? this.getHeight().getValue() : this.getWidth().getValue();
    boolean autoPadding = this.autoPadding_;
    if (autoPadding
        && !!EnumUtils.mask(paintDevice.getFeatures(), PaintDeviceFeatureFlag.FontMetrics).isEmpty()
        && this.labelsEnabled_) {
      logger.error(
          new StringWriter()
              .append(
                  "setAutoLayout(): device does not have font metrics (not even server-side font metrics).")
              .toString());
      autoPadding = false;
    }
    if (autoPadding) {
      if (horizontal) {
        if (this.labelsEnabled_) {
          this.setSelectionAreaPadding(0, EnumSet.of(Side.Top));
          this.setSelectionAreaPadding(
              (int) (xAxis.calcMaxTickLabelSize(paintDevice, Orientation.Vertical) + 10),
              EnumSet.of(Side.Bottom));
          this.setSelectionAreaPadding(
              (int)
                  Math.max(
                      xAxis.calcMaxTickLabelSize(paintDevice, Orientation.Horizontal) / 2, 10.0),
              EnumUtils.or(EnumSet.of(Side.Left), Side.Right));
        } else {
          this.setSelectionAreaPadding(0, EnumSet.of(Side.Top));
          this.setSelectionAreaPadding(
              5, EnumUtils.or(EnumUtils.or(EnumSet.of(Side.Left), Side.Right), Side.Bottom));
        }
      } else {
        if (this.labelsEnabled_) {
          this.setSelectionAreaPadding(0, EnumSet.of(Side.Right));
          this.setSelectionAreaPadding(
              (int)
                  Math.max(xAxis.calcMaxTickLabelSize(paintDevice, Orientation.Vertical) / 2, 10.0),
              EnumUtils.or(EnumSet.of(Side.Top), Side.Bottom));
          this.setSelectionAreaPadding(
              (int) (xAxis.calcMaxTickLabelSize(paintDevice, Orientation.Horizontal) + 10),
              EnumSet.of(Side.Left));
        } else {
          this.setSelectionAreaPadding(0, EnumSet.of(Side.Right));
          this.setSelectionAreaPadding(
              5, EnumUtils.or(EnumUtils.or(EnumSet.of(Side.Top), Side.Bottom), Side.Left));
        }
      }
    }
    double left =
        horizontal
            ? this.getSelectionAreaPadding(Side.Left)
            : this.getSelectionAreaPadding(Side.Top);
    double right =
        horizontal
            ? this.getSelectionAreaPadding(Side.Right)
            : this.getSelectionAreaPadding(Side.Bottom);
    double top =
        horizontal
            ? this.getSelectionAreaPadding(Side.Top)
            : this.getSelectionAreaPadding(Side.Right);
    double bottom =
        horizontal
            ? this.getSelectionAreaPadding(Side.Bottom)
            : this.getSelectionAreaPadding(Side.Left);
    double maxW = w - left - right;
    WRectF drawArea = new WRectF(left, 0, maxW, h);
    List<WAxis.Segment> segmentsBak = new ArrayList<WAxis.Segment>();
    for (int i = 0; i < xAxis.segments_.size(); ++i) {
      segmentsBak.add(new WAxis.Segment(xAxis.segments_.get(i)));
    }
    double renderIntervalBak = xAxis.renderInterval_;
    double fullRenderLengthBak = xAxis.fullRenderLength_;
    xAxis.prepareRender(
        horizontal ? Orientation.Horizontal : Orientation.Vertical, drawArea.getWidth());
    final WRectF chartArea = this.getChart().chartArea_;
    WRectF selectionRect = null;
    {
      double u =
          -xAxisStruct.transformHandle.getValue().getDx()
              / (chartArea.getWidth() * xAxisStruct.transformHandle.getValue().getM11());
      selectionRect = new WRectF(0, top, maxW, h - (top + bottom));
      this.transform_.setValue(
          new WTransform(
              1 / xAxisStruct.transformHandle.getValue().getM11(), 0, 0, 1, u * maxW, 0));
    }
    WRectF seriesArea = new WRectF(left, top + 5, maxW, h - (top + bottom + 5));
    WTransform selectionTransform =
        this.hv(new WTransform(1, 0, 0, 1, left, 0).multiply(this.transform_.getValue()));
    WRectF rect = selectionTransform.map(this.hv(selectionRect));
    painter.fillRect(this.hv(new WRectF(left, top, maxW, h - top - bottom)), this.background_);
    painter.fillRect(rect, this.selectedAreaBrush_);
    final double TICK_LENGTH = 5;
    final double ANGLE1 = 15;
    final double ANGLE2 = 80;
    double tickStart = 0.0;
    double tickEnd = 0.0;
    double labelPos = 0.0;
    AlignmentFlag labelHFlag = AlignmentFlag.Center;
    AlignmentFlag labelVFlag = AlignmentFlag.Middle;
    if (horizontal) {
      tickStart = 0;
      tickEnd = TICK_LENGTH;
      labelPos = TICK_LENGTH;
      labelVFlag = AlignmentFlag.Top;
    } else {
      tickStart = -TICK_LENGTH;
      tickEnd = 0;
      labelPos = -TICK_LENGTH;
      labelHFlag = AlignmentFlag.Right;
    }
    if (horizontal) {
      if (xAxis.getLabelAngle() > ANGLE1) {
        labelHFlag = AlignmentFlag.Right;
        if (xAxis.getLabelAngle() > ANGLE2) {
          labelVFlag = AlignmentFlag.Middle;
        }
      } else {
        if (xAxis.getLabelAngle() < -ANGLE1) {
          labelHFlag = AlignmentFlag.Left;
          if (xAxis.getLabelAngle() < -ANGLE2) {
            labelVFlag = AlignmentFlag.Middle;
          }
        }
      }
    } else {
      if (xAxis.getLabelAngle() > ANGLE1) {
        labelVFlag = AlignmentFlag.Bottom;
        if (xAxis.getLabelAngle() > ANGLE2) {
          labelHFlag = AlignmentFlag.Center;
        }
      } else {
        if (xAxis.getLabelAngle() < -ANGLE1) {
          labelVFlag = AlignmentFlag.Top;
          if (xAxis.getLabelAngle() < -ANGLE2) {
            labelHFlag = AlignmentFlag.Center;
          }
        }
      }
    }
    EnumSet<AxisProperty> axisProperties = EnumSet.of(AxisProperty.Line);
    if (this.labelsEnabled_) {
      axisProperties.add(AxisProperty.Labels);
    }
    if (horizontal) {
      xAxis.render(
          painter,
          axisProperties,
          new WPointF(drawArea.getLeft(), h - bottom),
          new WPointF(drawArea.getRight(), h - bottom),
          tickStart,
          tickEnd,
          labelPos,
          EnumUtils.or(EnumSet.of(labelHFlag), labelVFlag));
      WPainterPath line = new WPainterPath();
      line.moveTo(drawArea.getLeft() + 0.5, h - (bottom - 0.5));
      line.lineTo(drawArea.getRight(), h - (bottom - 0.5));
      painter.strokePath(line, xAxis.getPen());
    } else {
      xAxis.render(
          painter,
          axisProperties,
          new WPointF(this.getSelectionAreaPadding(Side.Left) - 1, drawArea.getLeft()),
          new WPointF(this.getSelectionAreaPadding(Side.Left) - 1, drawArea.getRight()),
          tickStart,
          tickEnd,
          labelPos,
          EnumUtils.or(EnumSet.of(labelHFlag), labelVFlag));
      WPainterPath line = new WPainterPath();
      line.moveTo(this.getSelectionAreaPadding(Side.Left) - 0.5, drawArea.getLeft() + 0.5);
      line.lineTo(this.getSelectionAreaPadding(Side.Left) - 0.5, drawArea.getRight());
      painter.strokePath(line, xAxis.getPen());
    }
    WPainterPath curve = new WPainterPath();
    {
      WTransform t =
          new WTransform(1, 0, 0, 1, seriesArea.getLeft(), seriesArea.getTop())
              .multiply(
                  new WTransform(
                      seriesArea.getWidth() / chartArea.getWidth(),
                      0,
                      0,
                      seriesArea.getHeight() / chartArea.getHeight(),
                      0,
                      0))
              .multiply(new WTransform(1, 0, 0, 1, -chartArea.getLeft(), -chartArea.getTop()));
      if (!horizontal) {
        t.assign(
            new WTransform(
                    0,
                    1,
                    1,
                    0,
                    this.getSelectionAreaPadding(Side.Left)
                        - this.getSelectionAreaPadding(Side.Right)
                        - 5,
                    0)
                .multiply(t)
                .multiply(new WTransform(0, 1, 1, 0, 0, 0)));
      }
      curve.assign(t.map(this.getChart().pathForSeries(this.series_)));
    }
    {
      WRectF leftHandle = this.hv(new WRectF(-5, top, 5, h - top - bottom));
      WTransform t =
          new WTransform(1, 0, 0, 1, left, -top)
              .multiply(
                  new WTransform()
                      .translate(this.transform_.getValue().map(selectionRect.getTopLeft())));
      painter.fillRect(this.hv(t).map(leftHandle), this.handleBrush_);
    }
    {
      WRectF rightHandle = this.hv(new WRectF(0, top, 5, h - top - bottom));
      WTransform t =
          new WTransform(1, 0, 0, 1, left, -top)
              .multiply(
                  new WTransform()
                      .translate(this.transform_.getValue().map(selectionRect.getTopRight())));
      painter.fillRect(this.hv(t).map(rightHandle), this.handleBrush_);
    }
    if (this.selectedSeriesPen_ != this.seriesPen_
        && !this.selectedSeriesPen_.equals(this.seriesPen_)) {
      WPainterPath clipPath = new WPainterPath();
      clipPath.addRect(this.hv(selectionRect));
      painter.setClipPath(selectionTransform.map(clipPath));
      painter.setClipping(true);
      painter.setPen(this.getSelectedSeriesPen());
      painter.drawPath(curve);
      WPainterPath leftClipPath = new WPainterPath();
      leftClipPath.addRect(
          this.hv(new WTransform(1, 0, 0, 1, -selectionRect.getWidth(), 0).map(selectionRect)));
      painter.setClipPath(
          this.hv(
                  new WTransform(1, 0, 0, 1, left, -top)
                      .multiply(
                          new WTransform()
                              .translate(
                                  this.transform_.getValue().map(selectionRect.getTopLeft()))))
              .map(leftClipPath));
      painter.setPen(this.getSeriesPen());
      painter.drawPath(curve);
      WPainterPath rightClipPath = new WPainterPath();
      rightClipPath.addRect(
          this.hv(new WTransform(1, 0, 0, 1, selectionRect.getWidth(), 0).map(selectionRect)));
      painter.setClipPath(
          this.hv(
                  new WTransform(1, 0, 0, 1, left - selectionRect.getRight(), -top)
                      .multiply(
                          new WTransform()
                              .translate(
                                  this.transform_.getValue().map(selectionRect.getTopRight()))))
              .map(rightClipPath));
      painter.drawPath(curve);
      painter.setClipping(false);
    } else {
      WPainterPath clipPath = new WPainterPath();
      clipPath.addRect(this.hv(new WRectF(left, top, maxW, h - top - bottom)));
      painter.setClipPath(clipPath);
      painter.setClipping(true);
      painter.setPen(this.getSeriesPen());
      painter.drawPath(curve);
      painter.setClipping(false);
    }
    if (this.getMethod() == RenderMethod.HtmlCanvas) {
      WApplication app = WApplication.getInstance();
      StringBuilder ss = new StringBuilder();
      ss.append("new Wt4.11.2.WAxisSliderWidget(")
          .append(app.getJavaScriptClass())
          .append(",")
          .append(this.getJsRef())
          .append(",")
          .append(this.getObjJsRef())
          .append(",")
          .append("{chart:")
          .append(this.getChart().getCObjJsRef())
          .append(",transform:")
          .append(this.transform_.getJsRef())
          .append(",rect:function(){return ")
          .append(rect.getJsRef())
          .append("},drawArea:")
          .append(drawArea.getJsRef())
          .append(",series:")
          .append(this.getChart().getSeriesIndexOf(this.series_))
          .append(",updateYAxis:")
          .append(StringUtils.asString(this.yAxisZoomEnabled_).toString())
          .append("});");
      this.doJavaScript(ss.toString());
    }
    xAxis.segments_.clear();
    for (int i = 0; i < segmentsBak.size(); ++i) {
      xAxis.segments_.add(new WAxis.Segment(segmentsBak.get(i)));
    }
    xAxis.renderInterval_ = renderIntervalBak;
    xAxis.fullRenderLength_ = fullRenderLengthBak;
  }

  private void init() {
    this.transform_ = this.createJSTransform();
    this.mouseWentDown()
        .addListener("function(o, e){var o=" + this.getSObjJsRef() + ";if(o){o.mouseDown(o, e);}}");
    this.mouseWentUp()
        .addListener("function(o, e){var o=" + this.getSObjJsRef() + ";if(o){o.mouseUp(o, e);}}");
    this.mouseDragged()
        .addListener("function(o, e){var o=" + this.getSObjJsRef() + ";if(o){o.mouseDrag(o, e);}}");
    this.mouseMoved()
        .addListener(
            "function(o, e){var o=" + this.getSObjJsRef() + ";if(o){o.mouseMoved(o, e);}}");
    this.touchStarted()
        .addListener(
            "function(o, e){var o=" + this.getSObjJsRef() + ";if(o){o.touchStarted(o, e);}}");
    this.touchEnded()
        .addListener(
            "function(o, e){var o=" + this.getSObjJsRef() + ";if(o){o.touchEnded(o, e);}}");
    this.touchMoved()
        .addListener(
            "function(o, e){var o=" + this.getSObjJsRef() + ";if(o){o.touchMoved(o, e);}}");
    this.setSelectionAreaPadding(0, EnumSet.of(Side.Top));
    this.setSelectionAreaPadding(20, EnumUtils.or(EnumSet.of(Side.Left), Side.Right));
    this.setSelectionAreaPadding(30, EnumSet.of(Side.Bottom));
    if (this.getChart() != null) {
      this.getChart().addAxisSliderWidget(this);
    }
  }

  private String getSObjJsRef() {
    return this.getJsRef() + ".wtSObj";
  }

  private WRectF hv(final WRectF rect) {
    boolean horizontal = this.getChart().getOrientation() == Orientation.Vertical;
    if (horizontal) {
      return rect;
    } else {
      return new WRectF(
          this.getWidth().getValue() - rect.getY() - rect.getHeight(),
          rect.getX(),
          rect.getHeight(),
          rect.getWidth());
    }
  }

  private WTransform hv(final WTransform t) {
    boolean horizontal = this.getChart().getOrientation() == Orientation.Vertical;
    if (horizontal) {
      return t;
    } else {
      return new WTransform(0, 1, 1, 0, 0, 0)
          .multiply(t)
          .multiply(new WTransform(0, 1, 1, 0, 0, 0));
    }
  }

  private WCartesianChart getChart() {
    if (this.series_ != null) {
      return this.series_.getChart();
    } else {
      return null;
    }
  }

  private WDataSeries series_;
  private WPen seriesPen_;
  private WPen selectedSeriesPen_;
  private WBrush handleBrush_;
  private WBrush background_;
  private WBrush selectedAreaBrush_;
  private boolean autoPadding_;
  private boolean labelsEnabled_;
  private boolean yAxisZoomEnabled_;
  private int[] padding_ = new int[4];
  private WJavaScriptHandle<WTransform> transform_;

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WAxisSliderWidget",
        "(function(t,e,n,o){let r=!1;const i={};e.wtSObj=this;const c=this,s=t.WT;n.canvas.style.msTouchAction=\"none\";function a(t){return 2===t.pointerType||3===t.pointerType||\"pen\"===t.pointerType||\"touch\"===t.pointerType}let u=!1;if(window.MSPointerEvent||window.PointerEvent){e.style.touchAction=\"none\";n.canvas.style.msTouchAction=\"none\";n.canvas.style.touchAction=\"none\"}(window.MSPointerEvent||window.PointerEvent)&&function(){const t=[];function n(){t.length>0&&!u?u=!0:t.length<=0&&u&&(u=!1)}function o(o){if(a(o)){o.preventDefault();t.push(o);n();i.start(e,{touches:t.slice(0)})}}function r(o){if(u&&a(o)){o.preventDefault();for(let e=0;e<t.length;++e)if(t[e].pointerId===o.pointerId){t.splice(e,1);break}n();i.end(e,{touches:t.slice(0),changedTouches:[]})}}function c(o){if(a(o)){o.preventDefault();for(let e=0;e<t.length;++e)if(t[e].pointerId===o.pointerId){t[e]=o;break}n();i.moved(e,{touches:t.slice(0)})}}const s=e.wtEObj;if(s){e.removeEventListener(\"pointerdown\",s.pointerDown);e.removeEventListener(\"pointerup\",s.pointerUp);e.removeEventListener(\"pointerout\",s.pointerUp);e.removeEventListener(\"pointermove\",s.pointerMove)}e.wtEObj={pointerDown:o,pointerUp:r,pointerMove:c};e.addEventListener(\"pointerdown\",o);e.addEventListener(\"pointerup\",r);e.addEventListener(\"pointerout\",r);e.addEventListener(\"pointermove\",c)}();const f=s.gfxUtils.rect_left,l=s.gfxUtils.rect_right,h=s.gfxUtils.rect_top,d=s.gfxUtils.rect_bottom;function v(){return o.chart.config.series[o.series].xAxis}this.xAxis=v;function p(){return o.chart.config.minZoom.x[v()]}function g(){return o.chart.config.maxZoom.x[v()]}let w=null;let y=null;function x(){!function(t){if(!r){r=!0;requestAnimationFrame((function(){t();r=!1}))}}(n.repaint)}this.changeRange=function(t,e){t<0&&(t=0);e>1&&(e=1);const n=o.drawArea;o.transform[0]=e-t;o.transform[4]=t*n[2];x()};function m(){const t=o.drawArea,e=o.transform[4]/t[2];return[e,o.transform[0]+e]}function E(t){x();if(t){const t=o.transform,e=o.drawArea,n=t[4]/e[2],r=t[0]+n;o.chart.setXRange(o.series,n,r,o.updateYAxis)}}function b(){return!o.chart.config.isHorizontal}function M(t,e,n){return b()?t.y>=h(e)&&t.y<=d(e)&&t.x>f(e)-n/2&&t.x<f(e)+n/2:t.x>=f(e)&&t.x<=l(e)&&t.y>h(e)-n/2&&t.y<h(e)+n/2}function A(t,e,n){return b()?t.y>=h(e)&&t.y<=d(e)&&t.x>l(e)-n/2&&t.x<l(e)+n/2:t.x>=f(e)&&t.x<=l(e)&&t.y>d(e)-n/2&&t.y<d(e)+n/2}function D(t,e){return b()?t.y>=h(e)&&t.y<=d(e)&&t.x>f(e)&&t.x<l(e):t.x>=f(e)&&t.x<=l(e)&&t.y>h(e)&&t.y<d(e)}this.mouseDown=function(t,n){if(u)return;w=s.widgetCoordinates(e,n);const r=o.rect();if(M(w,r,10))y=1;else if(A(w,r,10))y=3;else{if(!D(w,r)){y=null;return}y=2}s.cancelEvent(n)};this.mouseUp=function(t,e){if(!u){w=null;if(null!==y){y=null;s.cancelEvent(e)}}};function T(t){const e=o.transform,n=o.drawArea,r=e[4]/n[2],i=e[0]+r;let s=(r*n[2]+t)/n[2];if(i<=s)return;const a=1/(i-s);if(a>g())return;if(a<p())return;s<0&&(s=0);s>1&&(s=1);c.changeRange(s,i);E(!0);const u=m();if(Math.abs(u[1]-i)>Math.abs(u[0]-r)){c.changeRange(r,i);E(!0)}}function C(t){const e=o.transform,n=o.drawArea,r=e[4]/n[2],i=e[0]+r;let s=(i*n[2]+t)/n[2];if(s<=r)return;const a=1/(s-r);if(a>g())return;if(a<p())return;s<0&&(s=0);s>1&&(s=1);c.changeRange(r,s);E(!0);const u=m();if(Math.abs(u[0]-r)>Math.abs(u[1]-i)){c.changeRange(r,i);E(!0)}}function L(t){const e=o.transform,n=o.drawArea,r=e[4]/n[2],i=e[0]+r,s=r*n[2];let a=s+t;if(a<0){t=-s;a=0}let u=a/n[2];const f=i*n[2];let l=f+t;if(l>n[2]){a=s+(t=n[2]-f);u=a/n[2];l=n[2]}const h=l/n[2];c.changeRange(u,h);E(!0)}this.mouseDrag=function(t,n){if(u)return;if(!y)return;s.cancelEvent(n);const o=s.widgetCoordinates(e,n);if(null===w){w=o;return}let r;r=b()?o.x-w.x:o.y-w.y;switch(y){case 1:T(r);break;case 2:L(r);break;case 3:C(r)}w=o;E(!0)};this.mouseMoved=function(t,r){setTimeout((function(){if(u)return;if(y)return;const t=s.widgetCoordinates(e,r),i=o.rect();M(t,i,10)||A(t,i,10)?b()?n.canvas.style.cursor=\"col-resize\":n.canvas.style.cursor=\"row-resize\":D(t,i)?n.canvas.style.cursor=\"move\":n.canvas.style.cursor=\"auto\"}),0)};let R=!1,S=!1,U=null;i.start=function(t,e){R=1===e.touches.length;S=2===e.touches.length;if(R){w=s.widgetCoordinates(n.canvas,e.touches[0]);const t=o.rect();if(M(w,t,20))y=1;else if(A(w,t,20))y=3;else{if(!D(w,t)){y=null;return}y=2}s.capture(null);s.capture(n.canvas);e.preventDefault&&e.preventDefault()}else if(S){y=null;const t=[s.widgetCoordinates(n.canvas,e.touches[0]),s.widgetCoordinates(n.canvas,e.touches[1])],r=o.rect();if(!D(t[0],r)||!D(t[1],r))return;U=b()?Math.abs(t[0].x-t[1].x):Math.abs(t[0].y-t[1].y);s.capture(null);s.capture(n.canvas);e.preventDefault&&e.preventDefault()}};i.end=function(t,n){const o=Array.prototype.slice.call(n.touches),r=R,c=S;let a=0===o.length;R=1===o.length;S=2===o.length;a||function(){for(let t=0;t<n.changedTouches.length;++t)!function(){const e=n.changedTouches[t].identifier;for(let t=0;t<o.length;++t)if(o[t].identifier===e){o.splice(t,1);return}}()}();a=0===o.length;R=1===o.length;S=2===o.length;if(a&&r){w=null;if(null===y)return;y=null;s.cancelEvent(n)}if(R&&c){S=!1;U=null;s.cancelEvent(n);i.start(e,n)}if(a&&c){S=!1;U=null;s.cancelEvent(n)}};i.moved=function(t,o){if(y){o.preventDefault&&o.preventDefault();const t=s.widgetCoordinates(e,o);if(null===w){w=t;return}let n;n=b()?t.x-w.x:t.y-w.y;switch(y){case 1:T(n);break;case 2:L(n);break;case 3:C(n)}w=t;E(!0)}else if(S){o.preventDefault&&o.preventDefault();const t=[s.widgetCoordinates(n.canvas,o.touches[0]),s.widgetCoordinates(n.canvas,o.touches[1])];let e;e=b()?Math.abs(t[0].x-t[1].x):Math.abs(t[0].y-t[1].y);const r=e-U;T(-r/2);C(r/2);U=e;E(!0)}};this.updateConfig=function(t){for(const[e,n]of Object.entries(t))o[e]=n;E(!1)};c.updateConfig({});if(!window.TouchEvent||window.MSPointerEvent||window.PointerEvent){const t=function(){};c.touchStarted=t;c.touchEnded=t;c.touchMoved=t}else{c.touchStarted=i.start;c.touchEnded=i.end;c.touchMoved=i.moved}})");
  }
}
