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
 * A cartesian chart.
 *
 * <p>A cartesian chart is a chart that uses X and Y axes. It can display one or multiple data
 * series, which each may be rendered using bars, lines, areas, or points.
 *
 * <p>To use a cartesian chart, the minimum you need to do is set a model using {@link
 * WAbstractChart#setModel(WAbstractItemModel model) WAbstractChart#setModel()}, set the model
 * column that holds the X data using {@link WCartesianChart#setXSeriesColumn(int modelColumn)
 * setXSeriesColumn()}, and add one or more series using {@link
 * WCartesianChart#addSeries(WDataSeries series) addSeries()}. Each series corresponds to one data
 * column that holds Y data.
 *
 * <p>A cartesian chart is either a {@link ChartType#Category} or a {@link ChartType#Scatter}.
 *
 * <p>In a <b>{@link ChartType#Category}</b>, the X series represent different categories, which are
 * listed consecutively in model row order. The X axis scale is set to {@link AxisScale#Discrete}.
 *
 * <p><div align="center"> <img src="doc-files/ChartWCartesianChart-1.png">
 *
 * <p><strong>A category chart with bar series</strong> </div>
 *
 * <p>Each series may be rendered differently, and this is configured in the data series (see {@link
 * WDataSeries} for more information).
 *
 * <p>In a <b>{@link ChartType#Scatter}</b>, the X series data are interpreted as numbers on a
 * numerical scale. The scale for the X axis defaults to a {@link AxisScale#Linear}, but this may be
 * changed to a {@link AxisScale#Date} when the X series contains dates (of type {@link
 * eu.webtoolkit.jwt.WDate}) to create a time series chart, or to a {@link AxisScale#Log}. A {@link
 * ChartType#Scatter} supports the same types of data series as a {@link ChartType#Category}, but
 * does not support stacking. In a scatter plot, the X series do not need to be ordered in
 * increasing values, and may be set differently for each dataseries using {@link
 * WDataSeries#setXSeriesColumn(int modelColumn) WDataSeries#setXSeriesColumn()}.
 *
 * <p><div align="center"> <img src="doc-files/ChartWCartesianChart-2.png">
 *
 * <p><strong>A time series scatter plot with line series</strong> </div>
 *
 * <p>Missing data in a model series Y values is interpreted as a <i>break</i>. For curve-like
 * series, this breaks the curve (or line).
 *
 * <p>The cartesian chart has support for dual Y axes. Each data series may be bound to one of the
 * two Y axes. By default, only the first Y axis is displayed. To show the second Y axis you will
 * need to call:
 *
 * <p>By default a chart has a horizontal X axis and a vertical Y axis, which corresponds to a
 * {@link Orientation#Vertical} orientation. The orientation may be changed to {@link
 * Orientation#Horizontal} using {@link WCartesianChart#setOrientation(Orientation orientation)
 * setOrientation()}.
 *
 * <p>The styling of the series data are dictated by a palette which may be set using
 * setPalette(WChartPalette *), but may be overridden by settings in each data series.
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>Styling through CSS is not applicable.
 *
 * <p>
 *
 * <h3>Client-side interaction</h3>
 *
 * <p>WCartesianChart has several features that allow interaction with the chart without server
 * roundtrips. These features include zoom, pan, crosshair and follow curve functionality.
 *
 * <p>
 *
 * <p><i><b>Note: </b>Client side interaction is only available if the chart is drawn on an HTML
 * canvas. This is the default rendering method on modern browsers, see {@link
 * WPaintedWidget#setPreferredMethod(RenderMethod method) WPaintedWidget#setPreferredMethod()} </i>
 *
 * <p><i><b>Note: </b>Some features are currently not supported in interactive mode:
 *
 * <ul>
 *   <li>Axes set at {@link AxisValue#Zero} position will not always be drawn correctly. They may be
 *       clipped off outside of the chart area, and when zooming, the axis ticks will change size.
 *   <li>{@link WAxis#setBreak(double minimum, double maximum) WAxis#setBreak()} is incompatible
 *       with interactive mode
 * </ul>
 *
 * </i>
 *
 * @see WDataSeries
 * @see WAxis
 * @see WPieChart
 */
public class WCartesianChart extends WAbstractChart {
  private static Logger logger = LoggerFactory.getLogger(WCartesianChart.class);

  /**
   * Creates a new cartesian chart.
   *
   * <p>Creates a cartesian chart of type {@link ChartType#Category}.
   */
  public WCartesianChart(WContainerWidget parentContainer) {
    super();
    this.interface_ = new WChart2DImplementation(this);
    this.orientation_ = Orientation.Vertical;
    this.XSeriesColumn_ = -1;
    this.type_ = ChartType.Category;
    this.series_ = new ArrayList<WDataSeries>();
    this.xAxes_ = new ArrayList<WCartesianChart.AxisStruct>();
    this.yAxes_ = new ArrayList<WCartesianChart.AxisStruct>();
    this.barMargin_ = 0;
    this.legend_ = new WLegend();
    this.axisPadding_ = 5;
    this.borderPen_ = new WPen(PenStyle.None);
    this.textPen_ = new WPen();
    this.chartArea_ = null;
    this.hasDeferredToolTips_ = false;
    this.jsDefined_ = false;
    this.zoomEnabled_ = false;
    this.panEnabled_ = false;
    this.rubberBandEnabled_ = true;
    this.crosshairEnabled_ = false;
    this.crosshairColor_ = new WColor(StandardColor.Black);
    this.crosshairXAxis_ = 0;
    this.crosshairYAxis_ = 0;
    this.seriesSelectionEnabled_ = false;
    this.selectedSeries_ = null;
    this.followCurve_ = null;
    this.curveManipulationEnabled_ = false;
    this.onDemandLoadingEnabled_ = false;
    this.loadingBackground_ = new WBrush(StandardColor.LightGray);
    this.cObjCreated_ = false;
    this.seriesSelected_ = new Signal2<WDataSeries, WPointF>();
    this.jsSeriesSelected_ = new JSignal2<Double, Double>(this, "seriesSelected") {};
    this.curvePaths_ = new HashMap<WDataSeries, WJavaScriptHandle<WPainterPath>>();
    this.freePainterPaths_ = new ArrayList<WJavaScriptHandle<WPainterPath>>();
    this.curveTransforms_ = new HashMap<WDataSeries, WJavaScriptHandle<WTransform>>();
    this.freeTransforms_ = new ArrayList<WJavaScriptHandle<WTransform>>();
    this.freePens_ = new ArrayList<WJavaScriptHandle<WPen>>();
    this.curveLabels_ = new ArrayList<CurveLabel>();
    this.axisSliderWidgets_ = new ArrayList<WAxisSliderWidget>();
    this.wheelActions_ = new HashMap<EnumSet<KeyboardModifier>, InteractiveAction>();
    this.loadTooltip_ = new JSignal2<Double, Double>(this, "loadTooltip") {};
    this.barTooltips_ = new ArrayList<WCartesianChart.BarTooltip>();
    this.init();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new cartesian chart.
   *
   * <p>Calls {@link #WCartesianChart(WContainerWidget parentContainer)
   * this((WContainerWidget)null)}
   */
  public WCartesianChart() {
    this((WContainerWidget) null);
  }
  /**
   * Creates a new cartesian chart.
   *
   * <p>Creates a cartesian chart of the indicated <code>type</code>.
   */
  public WCartesianChart(ChartType type, WContainerWidget parentContainer) {
    super();
    this.interface_ = new WChart2DImplementation(this);
    this.orientation_ = Orientation.Vertical;
    this.XSeriesColumn_ = -1;
    this.type_ = type;
    this.series_ = new ArrayList<WDataSeries>();
    this.xAxes_ = new ArrayList<WCartesianChart.AxisStruct>();
    this.yAxes_ = new ArrayList<WCartesianChart.AxisStruct>();
    this.barMargin_ = 0;
    this.legend_ = new WLegend();
    this.axisPadding_ = 5;
    this.borderPen_ = new WPen(PenStyle.None);
    this.textPen_ = new WPen();
    this.chartArea_ = null;
    this.hasDeferredToolTips_ = false;
    this.jsDefined_ = false;
    this.zoomEnabled_ = false;
    this.panEnabled_ = false;
    this.rubberBandEnabled_ = true;
    this.crosshairEnabled_ = false;
    this.crosshairColor_ = new WColor(StandardColor.Black);
    this.crosshairXAxis_ = 0;
    this.crosshairYAxis_ = 0;
    this.seriesSelectionEnabled_ = false;
    this.selectedSeries_ = null;
    this.followCurve_ = null;
    this.curveManipulationEnabled_ = false;
    this.onDemandLoadingEnabled_ = false;
    this.loadingBackground_ = new WBrush(StandardColor.LightGray);
    this.cObjCreated_ = false;
    this.seriesSelected_ = new Signal2<WDataSeries, WPointF>();
    this.jsSeriesSelected_ = new JSignal2<Double, Double>(this, "seriesSelected") {};
    this.curvePaths_ = new HashMap<WDataSeries, WJavaScriptHandle<WPainterPath>>();
    this.freePainterPaths_ = new ArrayList<WJavaScriptHandle<WPainterPath>>();
    this.curveTransforms_ = new HashMap<WDataSeries, WJavaScriptHandle<WTransform>>();
    this.freeTransforms_ = new ArrayList<WJavaScriptHandle<WTransform>>();
    this.freePens_ = new ArrayList<WJavaScriptHandle<WPen>>();
    this.curveLabels_ = new ArrayList<CurveLabel>();
    this.axisSliderWidgets_ = new ArrayList<WAxisSliderWidget>();
    this.wheelActions_ = new HashMap<EnumSet<KeyboardModifier>, InteractiveAction>();
    this.loadTooltip_ = new JSignal2<Double, Double>(this, "loadTooltip") {};
    this.barTooltips_ = new ArrayList<WCartesianChart.BarTooltip>();
    this.init();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new cartesian chart.
   *
   * <p>Calls {@link #WCartesianChart(ChartType type, WContainerWidget parentContainer) this(type,
   * (WContainerWidget)null)}
   */
  public WCartesianChart(ChartType type) {
    this(type, (WContainerWidget) null);
  }

  public void remove() {
    List<WAxisSliderWidget> copy = new ArrayList<WAxisSliderWidget>(this.axisSliderWidgets_);
    this.axisSliderWidgets_.clear();
    for (int i = 0; i < copy.size(); ++i) {
      copy.get(i).setSeries((WDataSeries) null);
    }
    super.remove();
  }
  /**
   * Sets the chart type.
   *
   * <p>The chart type determines how (x,y) data are interpreted. In a {@link ChartType#Category},
   * the X values are categories, and these are plotted consecutively, evenly spaced, and in row
   * order. In a {@link ChartType#Scatter}, the X values are interpreted numerically (as for Y
   * values).
   *
   * <p>The default chart type is a {@link ChartType#Category}.
   *
   * <p>
   *
   * @see WCartesianChart#getType()
   * @see WAxis#setScale(AxisScale scale)
   * @see WCartesianChart#getAxis(Axis axis)
   */
  public void setType(ChartType type) {
    if (this.type_ != type) {
      this.type_ = type;
      this.xAxes_.get(0).axis.init(this.interface_, Axis.X);
      this.update();
    }
  }
  /**
   * Returns the chart type.
   *
   * <p>
   *
   * @see WCartesianChart#setType(ChartType type)
   */
  public ChartType getType() {
    return this.type_;
  }
  /**
   * Sets the chart orientation.
   *
   * <p>Sets the chart orientation, which corresponds to the orientation of the Y axis: a {@link
   * Orientation#Vertical} orientation corresponds to the conventional way of a horizontal X axis
   * and vertical Y axis. A {@link Orientation#Horizontal} orientation is the other way around.
   *
   * <p>The default orientation is {@link Orientation#Vertical}.
   *
   * <p>
   *
   * @see WCartesianChart#getOrientation()
   */
  public void setOrientation(Orientation orientation) {
    if (this.orientation_ != orientation) {
      this.orientation_ = orientation;
      this.update();
    }
  }
  /**
   * Returns the chart orientation.
   *
   * <p>
   *
   * @see WCartesianChart#setOrientation(Orientation orientation)
   */
  public Orientation getOrientation() {
    return this.orientation_;
  }
  /**
   * Sets the the model column for the X series.
   *
   * <p>Use this method to specify the default data for the X series. For a {@link
   * ChartType#Scatter} this is mandatory if an X series is not specified for every {@link
   * WDataSeries}. For a {@link ChartType#Category}, if not specified, an increasing series of
   * integer numbers will be used (1, 2, ...).
   *
   * <p>Scatterplot dataseries may each individually be given its own X series data using {@link
   * WDataSeries#setXSeriesColumn(int modelColumn) WDataSeries#setXSeriesColumn()}
   *
   * <p>The default value is -1 (not specified).
   *
   * <p>The series column is reset to -1 when the model is set (or changed). Thus you need to set a
   * model before configuring the series.
   *
   * <p>
   *
   * @see WCartesianChart#XSeriesColumn()
   */
  public void setXSeriesColumn(int modelColumn) {
    if (this.XSeriesColumn_ != modelColumn) {
      this.XSeriesColumn_ = modelColumn;
      this.update();
    }
  }
  /**
   * set the pen used to render the labels
   *
   * <p>This method overwrites the pen for all axes
   *
   * <p>
   *
   * @see WAxis#setTextPen(WPen pen)
   */
  public void setTextPen(final WPen pen) {
    if (pen.equals(this.textPen_)) {
      return;
    }
    this.textPen_ = pen;
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      this.xAxes_.get(i).axis.setTextPen(pen);
    }
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      this.yAxes_.get(i).axis.setTextPen(pen);
    }
  }
  /**
   * Returns the model column for the X series.
   *
   * <p>
   *
   * @see WCartesianChart#setXSeriesColumn(int modelColumn)
   */
  public int XSeriesColumn() {
    return this.XSeriesColumn_;
  }
  /**
   * Adds a data series.
   *
   * <p>A single chart may display one or more data series. Each data series displays data from a
   * single model column in the chart. Series are plotted in the order that they have been added to
   * the chart.
   *
   * <p>The series column is reset to -1 when the model is set (or changed). Thus you need to set a
   * model before configuring the series.
   *
   * <p>
   *
   * @see WCartesianChart#removeSeries(WDataSeries series)
   * @see WCartesianChart#setSeries(List series)
   */
  public void addSeries(WDataSeries series) {
    WDataSeries s = series;
    this.series_.add(series);
    s.setChart(this);
    if (s.getType() == SeriesType.Line || s.getType() == SeriesType.Curve) {
      this.assignJSPathsForSeries(s);
      this.assignJSTransformsForSeries(s);
    }
    this.update();
  }
  /**
   * Removes a data series.
   *
   * <p>This will disassociate the given series from any WAxisSliderWidgets.
   *
   * <p>
   *
   * @see WCartesianChart#addSeries(WDataSeries series)
   * @see WCartesianChart#setSeries(List series)
   */
  public WDataSeries removeSeries(WDataSeries series) {
    int index = this.getSeriesIndexOf(series);
    if (index != -1) {
      for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
        if (this.axisSliderWidgets_.get(i).getSeries() == series) {
          this.axisSliderWidgets_.get(i).setSeries((WDataSeries) null);
        }
      }
      if (series.getType() == SeriesType.Line || series.getType() == SeriesType.Curve) {
        this.freeJSPathsForSeries(series);
        this.freeJSTransformsForSeries(series);
      }
      final WDataSeries result = this.series_.get(index);
      this.series_.remove(0 + index);
      this.update();
      return result;
    } else {
      return null;
    }
  }
  /**
   * Sets all data series.
   *
   * <p>Replaces the current list of series with the new list.
   *
   * <p>
   *
   * @see WCartesianChart#getSeries(int modelColumn)
   * @see WCartesianChart#removeSeries(WDataSeries series)
   */
  public void setSeries(List<WDataSeries> series) {
    Utils.copyList(series, this.series_);
    this.freeJSHandlesForAllSeries();
    this.assignJSHandlesForAllSeries();
    for (int i = 0; i < this.series_.size(); ++i) {
      this.series_.get(i).setChart(this);
    }
    this.update();
  }
  /**
   * Returns a data series corresponding to a data column.
   *
   * <p>Returns a reference to the first data series that plots data from <code>modelColumn</code>.
   */
  public WDataSeries getSeries(int modelColumn) {
    int index = this.getSeriesIndexOf(modelColumn);
    if (index != -1) {
      return this.series_.get(index);
    }
    throw new WException("Column " + String.valueOf(modelColumn) + " not in plot");
  }
  /**
   * Returns a list with the current data series.
   *
   * <p>Returns the complete list of current data series.
   *
   * <p>
   */
  public List<WDataSeries> getSeries() {
    return this.series_;
  }
  /**
   * Returns a chart axis.
   *
   * <p>Returns a reference to the specified <code>axis</code>.
   */
  public WAxis getAxis(Axis axis) {
    if (axis == Axis.X) {
      return this.getXAxis(0);
    } else {
      return this.getYAxis(axis == Axis.Y1 ? 0 : 1);
    }
  }
  /**
   * Sets an axis.
   *
   * <p>
   *
   * @see WCartesianChart#getAxis(Axis axis)
   */
  public void setAxis(WAxis waxis, Axis axis) {
    if (axis == Axis.X) {
      this.xAxes_.get(0).axis = waxis;
      this.xAxes_.get(0).axis.init(this.interface_, axis);
    } else {
      int yIndex = axis == Axis.Y1 ? 0 : 1;
      this.yAxes_.get(yIndex).axis = waxis;
      this.yAxes_.get(yIndex).axis.init(this.interface_, axis);
    }
  }
  /**
   * Returns a vector of all X axes associated with this chart.
   *
   * <p>This defaults to a vector of one axis.
   */
  public List<WAxis> getXAxes() {
    List<WAxis> result = new ArrayList<WAxis>();
    ;

    for (int i = 0; i < this.xAxes_.size(); ++i) {
      result.add(this.xAxes_.get(i).axis);
    }
    return result;
  }
  /**
   * Returns a vector of all Y axes associated with this chart.
   *
   * <p>This defaults to a vector of two axes: the Y1 and Y2 axes. Y1 will be at index 0, and Y2
   * will be at index 1.
   */
  public List<WAxis> getYAxes() {
    List<WAxis> result = new ArrayList<WAxis>();
    ;

    for (int i = 0; i < this.yAxes_.size(); ++i) {
      result.add(this.yAxes_.get(i).axis);
    }
    return result;
  }
  /** Returns the number of X axes associated with this chart. */
  public int getXAxisCount() {
    return (int) this.xAxes_.size();
  }
  /** Returns the number of Y axes associated with this chart. */
  public int getYAxisCount() {
    return (int) this.yAxes_.size();
  }
  /**
   * Retrieves the X axis at index i.
   *
   * <p>The following expression is always true:
   *
   * <p>
   *
   * <pre>{@code
   * getAxis(Axis.XAxis) == getXAxis(0)
   *
   * }</pre>
   *
   * <p>
   *
   * <p><i><b>Note: </b>Precondition: 0 &lt;= i &lt; {@link WCartesianChart#getXAxisCount()
   * getXAxisCount()} </i>
   */
  public WAxis getXAxis(int i) {
    return this.xAxes_.get(i).axis;
  }
  /**
   * Retrieves the Y axis at index i.
   *
   * <p>The following expressions are always true:
   *
   * <p>
   *
   * <pre>{@code
   * axis(Y1) == yAxis(0)
   * axis(Y2) == yAxis(1)
   *
   * }</pre>
   *
   * <p>
   *
   * <p><i><b>Note: </b>Precondition: 0 &lt;= i &lt; {@link WCartesianChart#getYAxisCount()
   * getYAxisCount()} </i>
   */
  public WAxis getYAxis(int i) {
    return this.yAxes_.get(i).axis;
  }
  /**
   * Adds a X axis to this chart.
   *
   * <p>The first extra axis will have index 1, the next index 2,...
   *
   * <p>Returns the index of the added axis.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This transfers ownership of the given {@link WAxis} to this chart. </i>
   *
   * <p><i><b>Note: </b>Precondition: waxis is not null </i>
   */
  public int addXAxis(WAxis waxis) {
    int idx = (int) this.xAxes_.size();
    this.xAxes_.add(new WCartesianChart.AxisStruct(waxis));
    this.xAxes_.get(idx).axis.initXAxis(this.interface_, idx);
    this.xAxes_.get(idx).axis.setPadding(this.getAxisPadding());
    this.xAxes_.get(idx).axis.setSoftLabelClipping(true);
    this.xAxes_.get(idx).transformHandle = this.createJSTransform();
    this.xAxes_.get(idx).transformChanged =
        new JSignal(this, "xTransformChanged" + String.valueOf(idx));
    this.update();
    return idx;
  }
  /**
   * Adds a Y axis to this chart.
   *
   * <p>The first extra axis will have index 2, the next index 3,...
   *
   * <p>Returns the index of the added axis.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This transfers ownership of the given {@link WAxis} to this chart. </i>
   *
   * <p><i><b>Note: </b>Precondition: waxis is not null </i>
   */
  public int addYAxis(WAxis waxis) {
    int idx = (int) this.yAxes_.size();
    this.yAxes_.add(new WCartesianChart.AxisStruct(waxis));
    this.yAxes_.get(idx).axis.initYAxis(this.interface_, idx);
    this.yAxes_.get(idx).axis.setPadding(this.getAxisPadding());
    this.yAxes_.get(idx).axis.setSoftLabelClipping(true);
    this.yAxes_.get(idx).transformHandle = this.createJSTransform();
    this.yAxes_.get(idx).transformChanged =
        new JSignal(this, "yTransformChanged" + String.valueOf(idx));
    this.update();
    return idx;
  }
  /**
   * Removes the X axis with the given id.
   *
   * <p>The indices of the axes with an id higher than xAxisId will be decremented.
   *
   * <p>Any {@link WDataSeries} associated with the removed axis are also removed.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Precondition: 0 &lt;= xAxisId &lt; {@link WCartesianChart#getXAxisCount()
   * getXAxisCount()} </i>
   */
  public WAxis removeXAxis(int xAxisId) {
    {
      int i = 0;
      while (i < this.series_.size()) {
        if (this.series_.get(i).getXAxis() == xAxisId) {
          this.removeSeries(this.series_.get(i));
        } else {
          if (this.series_.get(i).getXAxis() > xAxisId) {
            this.series_.get(i).bindToXAxis(this.series_.get(i).getXAxis() - 1);
          }
          ++i;
        }
      }
    }
    if (this.getCrosshairXAxis() > xAxisId) {
      this.setCrosshairXAxis(this.getCrosshairXAxis() - 1);
    }
    this.clearPensForAxis(Axis.X, xAxisId);
    final WAxis result = this.xAxes_.get(xAxisId).axis;
    this.xAxes_.remove(0 + xAxisId);
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      this.xAxes_.get(i).axis.xAxis_ = (int) i;
    }
    this.update();
    return result;
  }
  /**
   * Removes the Y axis with the given id.
   *
   * <p>The indices of the axes with an id higher than yAxisId will be decremented.
   *
   * <p>Any {@link WDataSeries} associated with the removed axis are also removed.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Precondition: 0 &lt;= yAxisId &lt; {@link WCartesianChart#getYAxisCount()
   * getYAxisCount()} </i>
   */
  public WAxis removeYAxis(int yAxisId) {
    {
      int i = 0;
      while (i < this.series_.size()) {
        if (this.series_.get(i).getYAxis() == yAxisId) {
          this.removeSeries(this.series_.get(i));
        } else {
          if (this.series_.get(i).getYAxis() > yAxisId) {
            this.series_.get(i).bindToYAxis(this.series_.get(i).getYAxis() - 1);
          }
          ++i;
        }
      }
    }
    if (this.getCrosshairYAxis() > yAxisId) {
      this.setCrosshairYAxis(this.getCrosshairYAxis() - 1);
    }
    this.clearPensForAxis(Axis.Y, yAxisId);
    final WAxis result = this.yAxes_.get(yAxisId).axis;
    this.yAxes_.remove(0 + yAxisId);
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      this.yAxes_.get(i).axis.yAxis_ = (int) i;
      this.yAxes_.get(i).axis.axis_ = i == 1 ? Axis.Y2 : Axis.Y;
    }
    this.update();
    return result;
  }
  /**
   * Clears all X axes.
   *
   * <p>The effect is the same as repeatedly using {@link WCartesianChart#removeYAxis(int yAxisId)
   * removeYAxis()} until are axes are removed, i.e. any {@link WDataSeries} will also be removed.
   */
  public void clearXAxes() {
    while (!this.series_.isEmpty()) {
      this.removeSeries(this.series_.get(this.series_.size() - 1));
    }
    this.clearPens();
    this.xAxes_.clear();
    this.update();
  }
  /**
   * Clears all Y axes.
   *
   * <p>The effect is the same as repeatedly using {@link WCartesianChart#removeYAxis(int yAxisId)
   * removeYAxis()} until are axes are removed, i.e. any {@link WDataSeries} will also be removed.
   */
  public void clearYAxes() {
    while (!this.series_.isEmpty()) {
      this.removeSeries(this.series_.get(this.series_.size() - 1));
    }
    this.clearPens();
    this.yAxes_.clear();
    this.update();
  }
  /**
   * Sets the margin between bars of different series.
   *
   * <p>Use this method to change the margin that is set between bars of different series. The
   * margin is specified as a fraction of the width. For example, a value of 0.1 adds a 10% margin
   * between bars of each series. Negative values are also allowed. For example, use a margin of -1
   * to plot the bars of different series on top of each other.
   *
   * <p>The default value is 0.
   */
  public void setBarMargin(double margin) {
    if (this.barMargin_ != margin) {
      this.barMargin_ = margin;
      this.update();
    }
  }
  /**
   * Returns the margin between bars of different series.
   *
   * <p>
   *
   * @see WCartesianChart#setBarMargin(double margin)
   */
  public double getBarMargin() {
    return this.barMargin_;
  }
  /**
   * Enables the legend.
   *
   * <p>The location of the legend can be configured using {@link
   * WCartesianChart#setLegendLocation(LegendLocation location, Side side, AlignmentFlag alignment)
   * setLegendLocation()}. Only series for which the legend is enabled are included in this legend.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>
   *
   * @see WDataSeries#isLegendEnabled()
   * @see WCartesianChart#setLegendLocation(LegendLocation location, Side side, AlignmentFlag
   *     alignment)
   */
  public void setLegendEnabled(boolean enabled) {
    this.legend_.setLegendEnabled(enabled);
    this.update();
  }
  /**
   * Returns whether the legend is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendEnabled(boolean enabled)
   */
  public boolean isLegendEnabled() {
    return this.legend_.isLegendEnabled();
  }
  /**
   * Configures the legend location.
   *
   * <p>The legend can be renderd either inside or outside of the chart area. When <code>location
   * </code> is {@link LegendLocation#Inside}, the legend will be rendered inside the chart. When
   * <code>location</code> is {@link LegendLocation#Outside}, the legend is rendered outside the
   * chart, in the chart padding area.
   *
   * <p>The provided <code>side</code> can either be {@link Side#Left}, {@link Side#Right}, {@link
   * Side#Top}, {@link Side#Bottom} and configures the side of the chart at which the legend is
   * displayed.
   *
   * <p>The <code>alignment</code> specifies how the legend is aligned. This can be a horizontal
   * alignment flag ({@link AlignmentFlag#Left}, {@link AlignmentFlag#Center}, or {@link
   * AlignmentFlag#Right}), when the <code>side</code> is {@link Side#Bottom} or {@link Side#Top},
   * or a vertical alignment flag ({@link AlignmentFlag#Top}, {@link AlignmentFlag#Middle}, or
   * {@link AlignmentFlag#Bottom}) when the <code>side</code> is {@link Side#Left} or {@link
   * Side#Right}.
   *
   * <p>The default location is {@link LegendLocation#Outside}, {@link Side#Right} and {@link
   * AlignmentFlag#Middle}.
   *
   * <p>To have more control over the legend, you could reimplement the {@link
   * WCartesianChart#renderLegendItem(WPainter painter, WPointF pos, WDataSeries series)
   * renderLegendItem()} method to customize how one item in the legend is rendered, or,
   * alternatively you can disable the legend generated by the chart itself, and reimplement the
   * {@link WCartesianChart#paint(WPainter painter, WRectF rectangle) paint()} method in which you
   * use the {@link WCartesianChart#renderLegendItem(WPainter painter, WPointF pos, WDataSeries
   * series) renderLegendItem()} method repeatedly to render a customized legend.
   *
   * <p>
   *
   * @see WDataSeries#setLegendEnabled(boolean enabled)
   */
  public void setLegendLocation(LegendLocation location, Side side, AlignmentFlag alignment) {
    this.legend_.setLegendLocation(location, side, alignment);
    this.update();
  }
  /**
   * Configures the legend decoration.
   *
   * <p>This configures the font, border and background for the legend.
   *
   * <p>The default font is a 10pt sans serif font (the same as the default axis label font), the
   * default <code>border</code> is PenStyle::None and the default <code>background</code> is
   * BrushStyle::None.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendEnabled(boolean enabled)
   */
  public void setLegendStyle(final WFont font, final WPen border, final WBrush background) {
    this.legend_.setLegendStyle(font, border, background);
    this.update();
  }
  /**
   * Returns the legend location.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendLocation(LegendLocation location, Side side, AlignmentFlag
   *     alignment)
   */
  public LegendLocation getLegendLocation() {
    return this.legend_.getLegendLocation();
  }
  /**
   * Returns the legend side.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendLocation(LegendLocation location, Side side, AlignmentFlag
   *     alignment)
   */
  public Side getLegendSide() {
    return this.legend_.getLegendSide();
  }
  /**
   * Returns the legend alignment.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendLocation(LegendLocation location, Side side, AlignmentFlag
   *     alignment)
   */
  public AlignmentFlag getLegendAlignment() {
    return this.legend_.getLegendAlignment();
  }
  /**
   * Returns the legend columns.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendColumns(int columns, WLength columnWidth)
   */
  public int getLegendColumns() {
    return this.legend_.getLegendColumns();
  }
  /**
   * Returns the legend column width.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendColumns(int columns, WLength columnWidth)
   */
  public WLength getLegendColumnWidth() {
    return this.legend_.getLegendColumnWidth();
  }
  /**
   * Returns the legend font.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendStyle(WFont font, WPen border, WBrush background)
   */
  public WFont getLegendFont() {
    return this.legend_.getLegendFont();
  }
  /**
   * Returns the legend border pen.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendStyle(WFont font, WPen border, WBrush background)
   */
  public WPen getLegendBorder() {
    return this.legend_.getLegendBorder();
  }
  /**
   * Returns the legend background brush.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendStyle(WFont font, WPen border, WBrush background)
   */
  public WBrush getLegendBackground() {
    return this.legend_.getLegendBackground();
  }
  /**
   * Configures multiple legend columns.
   *
   * <p>Multiple columns are typically useful when placing the legend at the top or at the bottom of
   * the chart.
   *
   * <p>The default value is a single column, 100 pixels wide.
   *
   * <p>When automatic chart layout is enabled, then the legend column width is computed
   * automatically, and this setting is ignored.
   *
   * <p>
   *
   * @see WAbstractChart#setAutoLayoutEnabled(boolean enabled)
   */
  public void setLegendColumns(int columns, final WLength columnWidth) {
    this.legend_.setLegendColumns(columns);
    this.legend_.setLegendColumnWidth(columnWidth);
    this.update();
  }

  public void paint(final WPainter painter, final WRectF rectangle) {
    for (WAbstractArea area : this.getAreas()) {
      (this).removeArea(area);
    }
    if (!painter.isActive()) {
      throw new WException("WCartesianChart::paint(): painter is not active.");
    }
    WRectF rect = rectangle;
    if ((rect == null) || rect.isEmpty()) {
      rect = painter.getWindow();
    }
    this.render(painter, rect);
  }
  /**
   * Draws the marker for a given data series.
   *
   * <p>Draws the marker for the indicated <code>series</code> in the <code>result</code>. This
   * method is called while painting the chart, and you may want to reimplement this method if you
   * wish to provide a custom marker for a particular data series.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendEnabled(boolean enabled)
   */
  public void drawMarker(final WDataSeries series, final WPainterPath result) {
    this.drawMarker(series, series.getMarker(), result);
  }
  /**
   * Renders the legend icon for a given data series.
   *
   * <p>Renders the legend icon for the indicated <code>series</code> in the <code>painter</code> at
   * position <code>pos</code>.
   *
   * <p>This method is called while rendering a legend item, and you may want to reimplement this
   * method if you wish to provide a custom legend icon for a particular data series.
   *
   * <p>
   *
   * @see WCartesianChart#renderLegendItem(WPainter painter, WPointF pos, WDataSeries series)
   */
  public void renderLegendIcon(
      final WPainter painter, final WPointF pos, final WDataSeries series) {
    WShadow shadow = painter.getShadow();
    switch (series.getType()) {
      case Bar:
        {
          WPainterPath path = new WPainterPath();
          path.moveTo(-6, 8);
          path.lineTo(-6, -8);
          path.lineTo(6, -8);
          path.lineTo(6, 8);
          painter.translate(pos.getX() + 7.5, pos.getY());
          painter.setShadow(series.getShadow());
          painter.fillPath(path, series.getBrush());
          painter.setShadow(shadow);
          painter.strokePath(path, series.getPen());
          painter.translate(-(pos.getX() + 7.5), -pos.getY());
          break;
        }
      case Line:
      case Curve:
        {
          painter.setPen(series.getPen().clone());
          double offset = series.getPen().getWidth().equals(new WLength(0)) ? 0.5 : 0;
          painter.setShadow(series.getShadow());
          painter.drawLine(pos.getX(), pos.getY() + offset, pos.getX() + 16, pos.getY() + offset);
          painter.setShadow(shadow);
        }
      case Point:
        {
          WPainterPath path = new WPainterPath();
          this.drawMarker(series, path);
          if (!path.isEmpty()) {
            painter.translate(pos.getX() + 8, pos.getY());
            painter.setShadow(series.getShadow());
            painter.fillPath(path, series.getMarkerBrush());
            painter.setShadow(shadow);
            painter.strokePath(path, series.getMarkerPen());
            painter.translate(-(pos.getX() + 8), -pos.getY());
          }
          break;
        }
    }
  }
  /**
   * Renders the legend item for a given data series.
   *
   * <p>Renders the legend item for the indicated <code>series</code> in the <code>painter</code> at
   * position <code>pos</code>. The default implementation draws the marker, and the series
   * description to the right. The series description is taken from the model&apos;s header data for
   * that series&apos; data column.
   *
   * <p>This method is called while painting the chart, and you may want to reimplement this method
   * if you wish to provide a custom marker for a particular data series.
   *
   * <p>
   *
   * @see WCartesianChart#setLegendEnabled(boolean enabled)
   */
  public void renderLegendItem(
      final WPainter painter, final WPointF pos, final WDataSeries series) {
    WPen fontPen = painter.getPen();
    this.renderLegendIcon(painter, pos, series);
    painter.setPen(fontPen.clone());
    int width = (int) this.getLegendColumnWidth().toPixels();
    if (width < 100) {
      width = 100;
    }
    painter.drawText(
        pos.getX() + 23,
        pos.getY() - 9,
        width,
        20,
        EnumUtils.or(EnumSet.of(AlignmentFlag.Left), AlignmentFlag.Middle),
        series.getModel().getHeaderData(series.getModelColumn()));
  }
  /**
   * Maps from device coordinates to model coordinates.
   *
   * <p>Maps a position in the chart back to model coordinates.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>If the chart is interactive, mapFromDevice will correctly take the current zoom range into
   * account.
   *
   * <p>
   *
   * @see WCartesianChart#mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int
   *     ySegment)
   */
  public WPointF mapFromDevice(final WPointF point, Axis ordinateAxis) {
    return this.mapFromDevice(point, ordinateAxis == Axis.Y1 ? 0 : 1);
  }
  /**
   * Maps from device coordinates to model coordinates.
   *
   * <p>Returns {@link #mapFromDevice(WPointF point, Axis ordinateAxis) mapFromDevice(point,
   * Axis.Ordinate)}
   */
  public final WPointF mapFromDevice(final WPointF point) {
    return mapFromDevice(point, Axis.Ordinate);
  }
  /**
   * Maps from device coordinates to model coordinates.
   *
   * <p>Maps a position in the chart back to model coordinates.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>If the chart is interactive, mapFromDevice will correctly take the current zoom range into
   * account.
   *
   * <p>
   *
   * @see WCartesianChart#mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int
   *     ySegment)
   */
  public WPointF mapFromDevice(final WPointF point, int ordinateAxis) {
    return this.mapFromDevice(point, this.getXAxis(0), this.getYAxis(ordinateAxis));
  }
  /**
   * Maps from device coordinates to model coordinates, ignoring the current zoom range.
   *
   * <p>Maps a position in the chart back to model coordinates, as if the chart was not zoomed in
   * (nor panned).
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>This function will not take the current zoom range into account. The mapping will be
   * performed as if {@link WCartesianChart#zoomRangeTransform(int yAxis) zoomRangeTransform()} is
   * the identity transform.
   *
   * <p>
   *
   * @see WCartesianChart#mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis
   *     ordinateAxis, int xSegment, int ySegment)
   */
  public WPointF mapFromDeviceWithoutTransform(final WPointF point, Axis ordinateAxis) {
    return this.mapFromDeviceWithoutTransform(point, ordinateAxis == Axis.Y1 ? 0 : 1);
  }
  /**
   * Maps from device coordinates to model coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapFromDeviceWithoutTransform(WPointF point, Axis ordinateAxis)
   * mapFromDeviceWithoutTransform(point, Axis.Ordinate)}
   */
  public final WPointF mapFromDeviceWithoutTransform(final WPointF point) {
    return mapFromDeviceWithoutTransform(point, Axis.Ordinate);
  }
  /**
   * Maps from device coordinates to model coordinates.
   *
   * <p>Maps a position in the chart back to model coordinates.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>If the chart is interactive, mapFromDevice will correctly take the current zoom range into
   * account.
   *
   * <p>
   *
   * @see WCartesianChart#mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int
   *     ySegment)
   */
  public WPointF mapFromDevice(final WPointF point, final WAxis xAxis, final WAxis yAxis) {
    if (this.isInteractive()) {
      return this.mapFromDeviceWithoutTransform(
          this.zoomRangeTransform(
                  this.xAxes_.get(xAxis.xAxis_).transformHandle.getValue(),
                  this.yAxes_.get(yAxis.yAxis_).transformHandle.getValue())
              .getInverted()
              .map(point),
          xAxis,
          yAxis);
    } else {
      return this.mapFromDeviceWithoutTransform(point, xAxis, yAxis);
    }
  }
  /**
   * Maps from device coordinates to model coordinates, ignoring the current zoom range.
   *
   * <p>Maps a position in the chart back to model coordinates, as if the chart was not zoomed in
   * (nor panned).
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>This function will not take the current zoom range into account. The mapping will be
   * performed as if {@link WCartesianChart#zoomRangeTransform(int yAxis) zoomRangeTransform()} is
   * the identity transform.
   *
   * <p>
   *
   * @see WCartesianChart#mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis
   *     ordinateAxis, int xSegment, int ySegment)
   */
  public WPointF mapFromDeviceWithoutTransform(final WPointF point, int ordinateAxis) {
    return this.mapFromDeviceWithoutTransform(point, this.getXAxis(0), this.getYAxis(ordinateAxis));
  }
  /**
   * Maps from device coordinates to model coordinates, ignoring the current zoom range.
   *
   * <p>Maps a position in the chart back to model coordinates, as if the chart was not zoomed in
   * (nor panned).
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>This function will not take the current zoom range into account. The mapping will be
   * performed as if {@link WCartesianChart#zoomRangeTransform(int yAxis) zoomRangeTransform()} is
   * the identity transform.
   *
   * <p>
   *
   * @see WCartesianChart#mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis
   *     ordinateAxis, int xSegment, int ySegment)
   */
  public WPointF mapFromDeviceWithoutTransform(
      final WPointF point, final WAxis xAxis, final WAxis yAxis) {
    WPointF p = this.inverseHv(point.getX(), point.getY(), this.getWidth().toPixels());
    return new WPointF(
        xAxis.mapFromDevice(p.getX() - this.chartArea_.getLeft()),
        yAxis.mapFromDevice(this.chartArea_.getBottom() - p.getY()));
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>This returns the chart device coordinates for a (x,y) pair of model values.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>The <code>xSegment</code> and <code>ySegment</code> arguments are relevant only when the
   * corresponding axis is broken using {@link WAxis#setBreak(double minimum, double maximum)
   * WAxis#setBreak()}. Then, its possible values may be 0 (below the break) or 1 (above the break).
   *
   * <p>If the chart is interactive, mapToDevice will correctly take the current zoom range into
   * account.
   *
   * <p>
   *
   * @see WCartesianChart#mapFromDevice(WPointF point, Axis ordinateAxis)
   */
  public WPointF mapToDevice(
      final Object xValue, final Object yValue, Axis axis, int xSegment, int ySegment) {
    return this.mapToDevice(xValue, yValue, axis == Axis.Y1 ? 0 : 1, xSegment, ySegment);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>Returns {@link #mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int
   * ySegment) mapToDevice(xValue, yValue, Axis.Ordinate, 0, 0)}
   */
  public final WPointF mapToDevice(final Object xValue, final Object yValue) {
    return mapToDevice(xValue, yValue, Axis.Ordinate, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>Returns {@link #mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int
   * ySegment) mapToDevice(xValue, yValue, axis, 0, 0)}
   */
  public final WPointF mapToDevice(final Object xValue, final Object yValue, Axis axis) {
    return mapToDevice(xValue, yValue, axis, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>Returns {@link #mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int
   * ySegment) mapToDevice(xValue, yValue, axis, xSegment, 0)}
   */
  public final WPointF mapToDevice(
      final Object xValue, final Object yValue, Axis axis, int xSegment) {
    return mapToDevice(xValue, yValue, axis, xSegment, 0);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>This returns the chart device coordinates for a (x,y) pair of model values.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>The <code>xSegment</code> and <code>ySegment</code> arguments are relevant only when the
   * corresponding axis is broken using {@link WAxis#setBreak(double minimum, double maximum)
   * WAxis#setBreak()}. Then, its possible values may be 0 (below the break) or 1 (above the break).
   *
   * <p>If the chart is interactive, mapToDevice will correctly take the current zoom range into
   * account.
   *
   * <p>
   *
   * @see WCartesianChart#mapFromDevice(WPointF point, Axis ordinateAxis)
   */
  public WPointF mapToDevice(
      final Object xValue, final Object yValue, int axis, int xSegment, int ySegment) {
    return this.mapToDevice(
        xValue, yValue, this.getXAxis(0), this.getYAxis(axis), xSegment, ySegment);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>Returns {@link #mapToDevice(Object xValue, Object yValue, int axis, int xSegment, int
   * ySegment) mapToDevice(xValue, yValue, axis, 0, 0)}
   */
  public final WPointF mapToDevice(final Object xValue, final Object yValue, int axis) {
    return mapToDevice(xValue, yValue, axis, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>Returns {@link #mapToDevice(Object xValue, Object yValue, int axis, int xSegment, int
   * ySegment) mapToDevice(xValue, yValue, axis, xSegment, 0)}
   */
  public final WPointF mapToDevice(
      final Object xValue, final Object yValue, int axis, int xSegment) {
    return mapToDevice(xValue, yValue, axis, xSegment, 0);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>This returns the chart device coordinates for a (x,y) pair of model values.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>The <code>xSegment</code> and <code>ySegment</code> arguments are relevant only when the
   * corresponding axis is broken using {@link WAxis#setBreak(double minimum, double maximum)
   * WAxis#setBreak()}. Then, its possible values may be 0 (below the break) or 1 (above the break).
   *
   * <p>If the chart is interactive, mapToDevice will correctly take the current zoom range into
   * account.
   *
   * <p>
   *
   * @see WCartesianChart#mapFromDevice(WPointF point, Axis ordinateAxis)
   */
  public WPointF mapToDevice(
      final Object xValue,
      final Object yValue,
      final WAxis xAxis,
      final WAxis yAxis,
      int xSegment,
      int ySegment) {
    if (this.isInteractive()) {
      return this.zoomRangeTransform(
              this.xAxes_.get(xAxis.xAxis_).transformHandle.getValue(),
              this.yAxes_.get(yAxis.yAxis_).transformHandle.getValue())
          .map(this.mapToDeviceWithoutTransform(xValue, yValue, xAxis, yAxis, xSegment, ySegment));
    } else {
      return this.mapToDeviceWithoutTransform(xValue, yValue, xAxis, yAxis, xSegment, ySegment);
    }
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>Returns {@link #mapToDevice(Object xValue, Object yValue, WAxis xAxis, WAxis yAxis, int
   * xSegment, int ySegment) mapToDevice(xValue, yValue, xAxis, yAxis, 0, 0)}
   */
  public final WPointF mapToDevice(
      final Object xValue, final Object yValue, final WAxis xAxis, final WAxis yAxis) {
    return mapToDevice(xValue, yValue, xAxis, yAxis, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates.
   *
   * <p>Returns {@link #mapToDevice(Object xValue, Object yValue, WAxis xAxis, WAxis yAxis, int
   * xSegment, int ySegment) mapToDevice(xValue, yValue, xAxis, yAxis, xSegment, 0)}
   */
  public final WPointF mapToDevice(
      final Object xValue,
      final Object yValue,
      final WAxis xAxis,
      final WAxis yAxis,
      int xSegment) {
    return mapToDevice(xValue, yValue, xAxis, yAxis, xSegment, 0);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>This returns the chart device coordinates for a (x,y) pair of model values.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>The <code>xSegment</code> and <code>ySegment</code> arguments are relevant only when the
   * corresponding axis is broken using {@link WAxis#setBreak(double minimum, double maximum)
   * WAxis#setBreak()}. Then, its possible values may be 0 (below the break) or 1 (above the break).
   *
   * <p>This function will not take the current zoom range into account.The mapping will be
   * performed as if {@link WCartesianChart#zoomRangeTransform(int yAxis) zoomRangeTransform()} is
   * the identity transform.
   *
   * <p>
   *
   * @see WCartesianChart#mapFromDeviceWithoutTransform(WPointF point, Axis ordinateAxis)
   */
  public WPointF mapToDeviceWithoutTransform(
      final Object xValue, final Object yValue, Axis ordinateAxis, int xSegment, int ySegment) {
    return this.mapToDeviceWithoutTransform(
        xValue, yValue, ordinateAxis == Axis.Y1 ? 0 : 1, xSegment, ySegment);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis,
   * int xSegment, int ySegment) mapToDeviceWithoutTransform(xValue, yValue, Axis.Ordinate, 0, 0)}
   */
  public final WPointF mapToDeviceWithoutTransform(final Object xValue, final Object yValue) {
    return mapToDeviceWithoutTransform(xValue, yValue, Axis.Ordinate, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis,
   * int xSegment, int ySegment) mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0)}
   */
  public final WPointF mapToDeviceWithoutTransform(
      final Object xValue, final Object yValue, Axis ordinateAxis) {
    return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis,
   * int xSegment, int ySegment) mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, xSegment,
   * 0)}
   */
  public final WPointF mapToDeviceWithoutTransform(
      final Object xValue, final Object yValue, Axis ordinateAxis, int xSegment) {
    return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, xSegment, 0);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>This returns the chart device coordinates for a (x,y) pair of model values.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>The <code>xSegment</code> and <code>ySegment</code> arguments are relevant only when the
   * corresponding axis is broken using {@link WAxis#setBreak(double minimum, double maximum)
   * WAxis#setBreak()}. Then, its possible values may be 0 (below the break) or 1 (above the break).
   *
   * <p>This function will not take the current zoom range into account.The mapping will be
   * performed as if {@link WCartesianChart#zoomRangeTransform(int yAxis) zoomRangeTransform()} is
   * the identity transform.
   *
   * <p>
   *
   * @see WCartesianChart#mapFromDeviceWithoutTransform(WPointF point, Axis ordinateAxis)
   */
  public WPointF mapToDeviceWithoutTransform(
      final Object xValue, final Object yValue, int ordinateAxis, int xSegment, int ySegment) {
    return this.mapToDeviceWithoutTransform(
        xValue, yValue, this.getXAxis(0), this.getYAxis(ordinateAxis), xSegment, ySegment);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, int ordinateAxis,
   * int xSegment, int ySegment) mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0)}
   */
  public final WPointF mapToDeviceWithoutTransform(
      final Object xValue, final Object yValue, int ordinateAxis) {
    return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, int ordinateAxis,
   * int xSegment, int ySegment) mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, xSegment,
   * 0)}
   */
  public final WPointF mapToDeviceWithoutTransform(
      final Object xValue, final Object yValue, int ordinateAxis, int xSegment) {
    return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, xSegment, 0);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>This returns the chart device coordinates for a (x,y) pair of model values.
   *
   * <p>This uses the axis dimensions that are based on the latest chart rendering. If you have not
   * yet rendered the chart, or wish that the mapping already reflects model changes since the last
   * rendering, you should call {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice
   * device) initLayout()} first.
   *
   * <p>The <code>xSegment</code> and <code>ySegment</code> arguments are relevant only when the
   * corresponding axis is broken using {@link WAxis#setBreak(double minimum, double maximum)
   * WAxis#setBreak()}. Then, its possible values may be 0 (below the break) or 1 (above the break).
   *
   * <p>This function will not take the current zoom range into account.The mapping will be
   * performed as if {@link WCartesianChart#zoomRangeTransform(int yAxis) zoomRangeTransform()} is
   * the identity transform.
   *
   * <p>
   *
   * @see WCartesianChart#mapFromDeviceWithoutTransform(WPointF point, Axis ordinateAxis)
   */
  public WPointF mapToDeviceWithoutTransform(
      final Object xValue,
      final Object yValue,
      final WAxis xAxis,
      final WAxis yAxis,
      int xSegment,
      int ySegment) {
    double x = this.chartArea_.getLeft() + xAxis.mapToDevice(xValue, xSegment);
    double y = this.chartArea_.getBottom() - yAxis.mapToDevice(yValue, ySegment);
    return this.hv(x, y, this.getWidth().toPixels());
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, WAxis xAxis, WAxis
   * yAxis, int xSegment, int ySegment) mapToDeviceWithoutTransform(xValue, yValue, xAxis, yAxis, 0,
   * 0)}
   */
  public final WPointF mapToDeviceWithoutTransform(
      final Object xValue, final Object yValue, final WAxis xAxis, final WAxis yAxis) {
    return mapToDeviceWithoutTransform(xValue, yValue, xAxis, yAxis, 0, 0);
  }
  /**
   * Maps model values onto chart coordinates, ignoring the current zoom range.
   *
   * <p>Returns {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, WAxis xAxis, WAxis
   * yAxis, int xSegment, int ySegment) mapToDeviceWithoutTransform(xValue, yValue, xAxis, yAxis,
   * xSegment, 0)}
   */
  public final WPointF mapToDeviceWithoutTransform(
      final Object xValue,
      final Object yValue,
      final WAxis xAxis,
      final WAxis yAxis,
      int xSegment) {
    return mapToDeviceWithoutTransform(xValue, yValue, xAxis, yAxis, xSegment, 0);
  }
  /**
   * Initializes the chart layout.
   *
   * <p>The mapping between model and device coordinates is only established after a rendering
   * phase, or after calling initLayout manually.
   *
   * <p>You need a layout in order to use the {@link WCartesianChart#mapFromDevice(WPointF point,
   * Axis ordinateAxis) mapFromDevice()} and {@link WCartesianChart#mapToDevice(Object xValue,
   * Object yValue, Axis axis, int xSegment, int ySegment) mapToDevice()} methods.
   *
   * <p>Unless a specific chart rectangle is specified, the entire widget area is assumed.
   */
  public boolean initLayout(final WRectF rectangle, WPaintDevice device) {
    if (this.getXAxisCount() == 0 || this.getYAxisCount() == 0) {
      return false;
    }
    WRectF rect = rectangle;
    if ((rect == null) || rect.isEmpty()) {
      rect = new WRectF(0.0, 0.0, this.getWidth().toPixels(), this.getHeight().toPixels());
    }
    if (this.getOrientation() == Orientation.Vertical) {
      this.width_ = (int) rect.getWidth();
      this.height_ = (int) rect.getHeight();
    } else {
      this.width_ = (int) rect.getHeight();
      this.height_ = (int) rect.getWidth();
    }
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      this.xAxes_.get(i).location.initLoc = AxisValue.Minimum;
      this.xAxes_.get(i).location.finLoc = AxisValue.Minimum;
    }
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      this.yAxes_.get(i).location.initLoc = AxisValue.Minimum;
      this.yAxes_.get(i).location.finLoc = AxisValue.Minimum;
    }
    WPaintDevice created = null;
    WPaintDevice d = device;
    if (!(d != null)) {
      created = this.getCreatePaintDevice();
      d = created;
    }
    boolean autoLayout = this.isAutoLayoutEnabled();
    if (autoLayout
        && EnumUtils.mask(d.getFeatures(), PaintDeviceFeatureFlag.FontMetrics).isEmpty()) {
      logger.error(
          new StringWriter()
              .append(
                  "setAutoLayout(): device does not have font metrics (not even server-side font metrics).")
              .toString());
      autoLayout = false;
    }
    WCartesianChart self = this;
    self.clearPens();
    if (this.isInteractive()) {
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        self.createPensForAxis(Axis.X, i);
      }
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        self.createPensForAxis(Axis.Y, i);
      }
    }
    if (autoLayout) {
      self.setPlotAreaPadding(40, EnumUtils.or(EnumSet.of(Side.Left), Side.Right));
      self.setPlotAreaPadding(30, EnumUtils.or(EnumSet.of(Side.Top), Side.Bottom));
      this.calcChartArea();
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        this.xAxes_.get(i).transform.assign(new WTransform());
      }
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        this.yAxes_.get(i).transform.assign(new WTransform());
      }
      if (this.chartArea_.getWidth() <= 5
          || this.chartArea_.getHeight() <= 5
          || !this.prepareAxes(device)) {
        if (this.isInteractive()) {
          for (int i = 0; i < this.getXAxisCount(); ++i) {
            this.xAxes_.get(i).transform.assign(this.xAxes_.get(i).transformHandle.getValue());
          }
          for (int i = 0; i < this.getYAxisCount(); ++i) {
            this.yAxes_.get(i).transform.assign(this.yAxes_.get(i).transformHandle.getValue());
          }
        }
        return false;
      }
      {
        WMeasurePaintDevice md = new WMeasurePaintDevice(d);
        WPainter painter = new WPainter(md);
        this.renderAxes(painter, EnumSet.of(AxisProperty.Line, AxisProperty.Labels));
        this.renderLegend(painter);
        WRectF bounds = md.getBoundingRect();
        final int MARGIN = 5;
        int corrLeft = (int) Math.max(0.0, rect.getLeft() - bounds.getLeft() + MARGIN);
        int corrRight = (int) Math.max(0.0, bounds.getRight() - rect.getRight() + MARGIN);
        int corrTop = (int) Math.max(0.0, rect.getTop() - bounds.getTop() + MARGIN);
        int corrBottom = (int) Math.max(0.0, bounds.getBottom() - rect.getBottom() + MARGIN);
        self.setPlotAreaPadding(
            this.getPlotAreaPadding(Side.Left) + corrLeft, EnumSet.of(Side.Left));
        self.setPlotAreaPadding(
            this.getPlotAreaPadding(Side.Right) + corrRight, EnumSet.of(Side.Right));
        self.setPlotAreaPadding(this.getPlotAreaPadding(Side.Top) + corrTop, EnumSet.of(Side.Top));
        self.setPlotAreaPadding(
            this.getPlotAreaPadding(Side.Bottom) + corrBottom, EnumSet.of(Side.Bottom));
      }
    }
    created = null;
    this.calcChartArea();
    boolean result =
        this.chartArea_.getWidth() > 5
            && this.chartArea_.getHeight() > 5
            && this.prepareAxes(device);
    if (this.isInteractive()) {
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        this.xAxes_.get(i).transform.assign(this.xAxes_.get(i).transformHandle.getValue());
      }
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        this.yAxes_.get(i).transform.assign(this.yAxes_.get(i).transformHandle.getValue());
      }
    } else {
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        this.xAxes_.get(i).transform.assign(new WTransform());
      }
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        this.yAxes_.get(i).transform.assign(new WTransform());
      }
    }
    if (this.isInteractive()) {
      if (this.curvePaths_.isEmpty()) {
        self.assignJSHandlesForAllSeries();
      }
    }
    return result;
  }
  /**
   * Initializes the chart layout.
   *
   * <p>Returns {@link #initLayout(WRectF rectangle, WPaintDevice device) initLayout(null,
   * (WPaintDevice)null)}
   */
  public final boolean initLayout() {
    return initLayout(null, (WPaintDevice) null);
  }
  /**
   * Initializes the chart layout.
   *
   * <p>Returns {@link #initLayout(WRectF rectangle, WPaintDevice device) initLayout(rectangle,
   * (WPaintDevice)null)}
   */
  public final boolean initLayout(final WRectF rectangle) {
    return initLayout(rectangle, (WPaintDevice) null);
  }
  /**
   * Creates a widget which renders the a legend item.
   *
   * <p>The legend item widget will contain a text and a {@link WPaintedWidget} which draws the
   * series&apos; symbol.
   */
  public WWidget createLegendItemWidget(int index) {
    WContainerWidget legendItem = new WContainerWidget();
    legendItem.addWidget(new WCartesianChart.IconWidget(this, index, (WContainerWidget) null));
    WText label = new WText(this.getModel().getHeaderData(index), (WContainerWidget) null);
    label.setVerticalAlignment(AlignmentFlag.Top);
    legendItem.addWidget(label);
    return legendItem;
  }
  /**
   * Adds a data point area (used for displaying e.g. tooltips).
   *
   * <p>You may want to specialize this is if you wish to modify (or ignore) the area.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Currently, an area is only created if the {@link ItemDataRole#ToolTip} data
   * at the data point is not empty. </i>
   */
  public void addDataPointArea(
      final WDataSeries series, int xRow, int xColumn, WAbstractArea area) {
    if (this.getAreas().isEmpty()) {
      this.addAreaMask();
    }
    this.addArea(area);
  }
  /**
   * Sets the padding between the chart area and the axes.
   *
   * <p>This calls WAxes::setPadding() on all axes.
   *
   * <p>
   *
   * @see WCartesianChart#getAxisPadding()
   */
  public void setAxisPadding(int padding) {
    this.axisPadding_ = padding;
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      this.xAxes_.get(i).axis.setPadding(padding);
    }
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      this.yAxes_.get(i).axis.setPadding(padding);
    }
  }
  /**
   * Returns the padding between the chart area and the axes.
   *
   * <p>This number may not reflect the actual padding of the individual axes, if another padding
   * has been applied on the individual axes.
   *
   * <p>
   *
   * @see WCartesianChart#setAxisPadding(int padding)
   */
  public int getAxisPadding() {
    return this.axisPadding_;
  }
  /**
   * Sets the pen of the border to be drawn around the chart area.
   *
   * <p>
   *
   * @see WCartesianChart#getBorderPen()
   */
  public void setBorderPen(final WPen pen) {
    if (!this.borderPen_.equals(pen)) {
      this.borderPen_ = pen;
      this.update();
    }
  }
  /**
   * Returns the pen used to draw the border around the chart area.
   *
   * <p>Defaults to PenStyle::None.
   *
   * <p>
   *
   * @see WCartesianChart#setBorderPen(WPen pen)
   */
  public WPen getBorderPen() {
    return this.borderPen_;
  }
  /**
   * Add a curve label.
   *
   * <p>
   *
   * @see CurveLabel#CurveLabel(WDataSeries series, WPointF point, String label)
   */
  public void addCurveLabel(final CurveLabel label) {
    this.curveLabels_.add(label);
    this.update();
  }
  /**
   * Configure all curve labels at once.
   *
   * <p>
   *
   * @see WCartesianChart#addCurveLabel(CurveLabel label)
   */
  public void setCurveLabels(final List<CurveLabel> labels) {
    Utils.copyList(labels, this.curveLabels_);
    this.update();
  }
  /**
   * Clear all curve labels.
   *
   * <p>
   *
   * @see WCartesianChart#addCurveLabel(CurveLabel label)
   */
  public void clearCurveLabels() {
    this.curveLabels_.clear();
    this.update();
  }
  /**
   * Get all of the registered curve labels.
   *
   * <p>
   *
   * @see WCartesianChart#setCurveLabels(List labels)
   */
  public List<CurveLabel> getCurveLabels() {
    return this.curveLabels_;
  }
  /**
   * Returns whether this chart is interactive.
   *
   * <p>Return true iff one of the interactive features is enabled, and the chart is being rendered
   * on an HTML canvas.
   */
  boolean isInteractive() {
    return !this.xAxes_.isEmpty()
        && !this.yAxes_.isEmpty()
        && (this.zoomEnabled_
            || this.panEnabled_
            || this.crosshairEnabled_
            || this.followCurve_ != null
            || this.axisSliderWidgets_.size() > 0
            || this.seriesSelectionEnabled_
            || this.curveManipulationEnabled_)
        && this.getMethod() == RenderMethod.HtmlCanvas;
  }
  /**
   * Enables zoom functionality.
   *
   * <p>When using the mouse, press the ctrl key while scrolling to zoom in/out a specific point on
   * the chart. If you press shift+ctrl, it will only zoom vertically. If you press alt+ctrl, it
   * will only zoom horizontally. To change these default mappings, use {@link
   * WCartesianChart#setWheelActions(Map wheelActions) setWheelActions()}.
   *
   * <p>When using touch, you can use a pinch gesture to zoom in/out. If the pinch gesture is
   * vertical/horizontal, it will zoom only vertically/horizontally, otherwise it will zoom both
   * axes equally.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>
   *
   * @see WCartesianChart#isZoomEnabled()
   * @see WCartesianChart#setWheelActions(Map wheelActions)
   */
  public void setZoomEnabled(boolean zoomEnabled) {
    if (this.zoomEnabled_ != zoomEnabled) {
      this.zoomEnabled_ = zoomEnabled;
      this.updateJSConfig("zoom", this.zoomEnabled_);
    }
  }
  /**
   * Enables zoom functionality.
   *
   * <p>Calls {@link #setZoomEnabled(boolean zoomEnabled) setZoomEnabled(true)}
   */
  public final void setZoomEnabled() {
    setZoomEnabled(true);
  }
  /**
   * Returns whether zoom is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setZoomEnabled(boolean zoomEnabled)
   */
  public boolean isZoomEnabled() {
    return this.zoomEnabled_;
  }
  /**
   * Enables pan functionality.
   *
   * <p>When using the mouse, you can click and drag to pan the chart (if zoomed in), or use the
   * scrollwheel.
   *
   * <p>When using touch, you can drag to pan the chart. If the rubberband effect is enabled, this
   * is intertial (it will keep scrolling after you let go) and there is an overscroll and bounce
   * back effect on the sides.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>
   *
   * @see WCartesianChart#isPanEnabled()
   */
  public void setPanEnabled(boolean panEnabled) {
    if (this.panEnabled_ != panEnabled) {
      this.panEnabled_ = panEnabled;
      this.updateJSConfig("pan", this.panEnabled_);
    }
  }
  /**
   * Enables pan functionality.
   *
   * <p>Calls {@link #setPanEnabled(boolean panEnabled) setPanEnabled(true)}
   */
  public final void setPanEnabled() {
    setPanEnabled(true);
  }
  /**
   * Returns whether pan is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setPanEnabled(boolean panEnabled)
   */
  public boolean isPanEnabled() {
    return this.panEnabled_;
  }
  /**
   * Enables the crosshair functionality.
   *
   * <p>When enabled, the crosshair will follow mouse movement, and show in the top right corner the
   * coordinate (according to X axis and the first Y axis) corresponding to this position.
   *
   * <p>When using touch, the crosshair can be moved with a drag. If both panning and the crosshair
   * are enabled, the crosshair will be moved when dragging close to the crosshair. Otherwise, the
   * chart will pan.
   */
  public void setCrosshairEnabled(boolean crosshair) {
    if (this.crosshairEnabled_ != crosshair) {
      this.crosshairEnabled_ = crosshair;
      this.updateJSConfig("crosshair", this.crosshairEnabled_);
    }
  }
  /**
   * Enables the crosshair functionality.
   *
   * <p>Calls {@link #setCrosshairEnabled(boolean crosshair) setCrosshairEnabled(true)}
   */
  public final void setCrosshairEnabled() {
    setCrosshairEnabled(true);
  }
  /**
   * Returns whether the crosshair is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setCrosshairEnabled(boolean crosshair)
   */
  public boolean isCrosshairEnabled() {
    return this.crosshairEnabled_;
  }
  /**
   * Sets the crosshair color.
   *
   * <p>The crosshair color is black by default.
   *
   * <p>
   *
   * @see WCartesianChart#setCrosshairEnabled(boolean crosshair)
   */
  public void setCrosshairColor(final WColor color) {
    if (!this.crosshairColor_.equals(color)) {
      this.crosshairColor_ = color;
      this.updateJSConfig("crosshairColor", jsStringLiteral(color.getCssText(true)));
    }
  }
  /**
   * Returns the crosshair color.
   *
   * <p>
   *
   * @see WCartesianChart#setCrosshairEnabled(boolean crosshair)
   * @see WCartesianChart#setCrosshairColor(WColor color)
   */
  public WColor getCrosshairColor() {
    return this.crosshairColor_;
  }
  /**
   * Sets the X axis to use for the crosshair.
   *
   * <p>Defaults to 0 (first X axis)
   */
  public void setCrosshairXAxis(int xAxis) {
    if (this.crosshairXAxis_ != xAxis) {
      this.crosshairXAxis_ = xAxis;
      this.updateJSConfig("crosshairXAxis", xAxis);
    }
  }
  /** Returns the X axis to use for the crosshair. */
  public int getCrosshairXAxis() {
    return this.crosshairXAxis_;
  }
  /**
   * Sets the Y axis to use for the crosshair.
   *
   * <p>Defaults to 0 (first Y axis)
   */
  public void setCrosshairYAxis(int yAxis) {
    if (this.crosshairYAxis_ != yAxis) {
      this.crosshairYAxis_ = yAxis;
      this.updateJSConfig("crosshairYAxis", yAxis);
    }
  }
  /** Returns the Y axis to use for the crosshair. */
  public int getCrosshairYAxis() {
    return this.crosshairYAxis_;
  }
  /**
   * Enables the follow curve functionality for a data series.
   *
   * <p>This enables follow curve functionality for the data series corresponding to the given
   * column.
   *
   * <p>If the data series is of type {@link SeriesType#Line} or {@link SeriesType#Curve}, the
   * crosshair can only be moved in the x direction. The y position of the crosshair will be
   * determined by the value of the data series. The crosshair will snap to the nearest point that
   * is defined in the data series.
   *
   * <p>When using the mouse, the x position will change on mouseover. When using touch, the x
   * position can be moved with a drag. The follow curve functionality has priority over the
   * crosshair functionality.
   *
   * <p>Use column index -1 or {@link WCartesianChart#disableFollowCurve() disableFollowCurve()} to
   * disable the follow curve feature.
   *
   * <p>
   *
   * <p><i><b>Note: </b>The follow curve functionality requires that the X axis values of the data
   * series are monotonically increasing or decreasing. </i>
   *
   * @deprecated Use {@link WCartesianChart#setFollowCurve(WDataSeries series) setFollowCurve()}
   *     instead
   */
  public void setFollowCurve(int followCurve) {
    if (followCurve == -1) {
      this.setFollowCurve((WDataSeries) null);
    } else {
      for (int i = 0; i < this.series_.size(); ++i) {
        if (this.series_.get(i).getModelColumn() == followCurve) {
          this.setFollowCurve(this.series_.get(i));
        }
      }
    }
  }
  /**
   * Enabled the follow curve funtionality for a data series.
   *
   * <p>This enables follow curve functionality for the data series corresponding to the given
   * column.
   *
   * <p>If the data series is of type {@link SeriesType#Line} or {@link SeriesType#Curve}, the
   * crosshair can only be moved in the x direction. The y position of the crosshair will be
   * determined by the value of the data series. The crosshair will snap to the nearest point that
   * is defined in the data series.
   *
   * <p>When using the mouse, the x position will change on mouseover. When using touch, the x
   * position can be moved with a drag. The follow curve functionality has priority over the
   * crosshair functionality.
   *
   * <p>Set to null to disable the follow curve feature.
   *
   * <p>
   *
   * <p><i><b>Note: </b>The follow curve functionality requires that the X axis values of the data
   * series are monotonically increasing or decreasing. </i>
   */
  public void setFollowCurve(WDataSeries series) {
    if (this.followCurve_ != series) {
      this.followCurve_ = series;
      this.updateJSConfig("followCurve", series != null ? this.getSeriesIndexOf(series) : -1);
    }
  }
  /**
   * Disable the follow curve functionality.
   *
   * <p>
   *
   * @see WCartesianChart#setFollowCurve(int followCurve)
   */
  public void disableFollowCurve() {
    this.setFollowCurve((WDataSeries) null);
  }
  /**
   * Returns the curve that is to be followed.
   *
   * <p>If follow curve functionality is not enabled, returns -1.
   *
   * <p>
   *
   * @see WCartesianChart#setFollowCurve(int followCurve)
   */
  public WDataSeries getFollowCurve() {
    return this.followCurve_;
  }
  /**
   * Enables/disables the inertial scrolling and rubberband effect.
   *
   * <p>
   *
   * @see WCartesianChart#setPanEnabled(boolean panEnabled)
   */
  public void setRubberBandEffectEnabled(boolean rubberBandEnabled) {
    if (this.rubberBandEnabled_ != rubberBandEnabled) {
      this.rubberBandEnabled_ = rubberBandEnabled;
      this.updateJSConfig("rubberBand", this.rubberBandEnabled_);
    }
  }
  /**
   * Enables/disables the inertial scrolling and rubberband effect.
   *
   * <p>Calls {@link #setRubberBandEffectEnabled(boolean rubberBandEnabled)
   * setRubberBandEffectEnabled(true)}
   */
  public final void setRubberBandEffectEnabled() {
    setRubberBandEffectEnabled(true);
  }
  /**
   * Checks whether the rubberband effect is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setRubberBandEffectEnabled(boolean rubberBandEnabled)
   */
  public boolean isRubberBandEffectEnabled() {
    return this.rubberBandEnabled_;
  }
  /**
   * Sets the mapping of mouse wheel actions for interactive charts.
   *
   * <p>
   *
   * @see WCartesianChart#getWheelActions()
   */
  public void setWheelActions(Map<EnumSet<KeyboardModifier>, InteractiveAction> wheelActions) {
    this.wheelActions_ = wheelActions;
    this.updateJSConfig("wheelActions", wheelActionsToJson(this.wheelActions_));
  }
  /**
   * Returns the current mouse wheel actions for interactive charts.
   *
   * <p>
   *
   * @see WCartesianChart#setWheelActions(Map wheelActions)
   */
  public Map<EnumSet<KeyboardModifier>, InteractiveAction> getWheelActions() {
    return this.wheelActions_;
  }
  /**
   * Enables or disables soft label clipping on all axes.
   *
   * <p>
   *
   * @see WAxis#setSoftLabelClipping(boolean enabled)
   */
  public void setSoftLabelClipping(boolean enabled) {
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      this.xAxes_.get(i).axis.setSoftLabelClipping(enabled);
    }
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      this.yAxes_.get(i).axis.setSoftLabelClipping(enabled);
    }
  }
  /**
   * Sets whether series selection is enabled.
   *
   * <p>If series selection is enabled, series can be selected with a mouse click or long press. If
   * the selected series is a {@link SeriesType#Line} or {@link SeriesType#Curve}, it can be
   * manipulated if {@link WCartesianChart#setCurveManipulationEnabled(boolean enabled) curve
   * manipulation} is enabled. The series that are not selected, will be shown in a lighter color.
   */
  public void setSeriesSelectionEnabled(boolean enabled) {
    if (this.seriesSelectionEnabled_ != enabled) {
      this.seriesSelectionEnabled_ = enabled;
      this.updateJSConfig("seriesSelection", this.seriesSelectionEnabled_);
    }
  }
  /**
   * Sets whether series selection is enabled.
   *
   * <p>Calls {@link #setSeriesSelectionEnabled(boolean enabled) setSeriesSelectionEnabled(true)}
   */
  public final void setSeriesSelectionEnabled() {
    setSeriesSelectionEnabled(true);
  }
  /**
   * Returns whether series selection is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setSeriesSelectionEnabled(boolean enabled)
   */
  public boolean isSeriesSelectionEnabled() {
    return this.seriesSelectionEnabled_;
  }
  /**
   * A signal that notifies the selection of a new curve.
   *
   * <p>This signal is emitted if a series is selected using a mouse click or long press. The first
   * argument is the selected series. The second argument is the point that was selected, in model
   * coordinates.
   *
   * <p>
   *
   * @see WCartesianChart#setSeriesSelectionEnabled(boolean enabled)
   */
  public Signal2<WDataSeries, WPointF> seriesSelected() {
    return this.seriesSelected_;
  }
  /**
   * Sets the series that is currently selected.
   *
   * <p>The series with the given model column will be selected. The other series will be shown in a
   * lighter color. The series that is currently selected is the one that can be manipulated if
   * {@link WCartesianChart#setCurveManipulationEnabled(boolean enabled) curve manipulation} is
   * enabled, and it is a {@link SeriesType#Line} or {@link SeriesType#Curve}.
   *
   * <p>The selected series can be changed using a long touch or mouse click.
   *
   * <p>If the argument provided is null or {@link WCartesianChart#setSeriesSelectionEnabled(boolean
   * enabled) series selection} is not enabled, no series will be selected.
   *
   * <p>
   *
   * @see WCartesianChart#setCurveManipulationEnabled(boolean enabled)
   * @see WCartesianChart#setSeriesSelectionEnabled(boolean enabled)
   */
  public void setSelectedSeries(WDataSeries series) {
    if (this.selectedSeries_ != series) {
      this.selectedSeries_ = series;
      this.update();
    }
  }
  /**
   * Get the currently selected curve.
   *
   * <p>-1 means that no curve is currently selected.
   *
   * <p>
   *
   * @see WCartesianChart#setSelectedSeries(WDataSeries series)
   */
  public WDataSeries getSelectedSeries() {
    return this.selectedSeries_;
  }
  /**
   * Enable curve manipulation.
   *
   * <p>If curve manipulation is enabled, the {@link WDataSeries#setScale(double scale) scale} and
   * {@link WDataSeries#setOffset(double offset) offset} of the {@link
   * WCartesianChart#getSelectedSeries() selected curve} can be manipulated interactively using
   * drag, scroll, and pinch.
   *
   * <p>
   *
   * @see WDataSeries#setOffset(double offset)
   * @see WDataSeries#setScale(double scale)
   */
  public void setCurveManipulationEnabled(boolean enabled) {
    if (this.curveManipulationEnabled_ != enabled) {
      this.curveManipulationEnabled_ = enabled;
      this.updateJSConfig("curveManipulation", this.curveManipulationEnabled_);
    }
  }
  /**
   * Enable curve manipulation.
   *
   * <p>Calls {@link #setCurveManipulationEnabled(boolean enabled)
   * setCurveManipulationEnabled(true)}
   */
  public final void setCurveManipulationEnabled() {
    setCurveManipulationEnabled(true);
  }
  /**
   * Returns whether curve manipulation is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setCurveManipulationEnabled(boolean enabled)
   */
  public boolean isCurveManipulationEnabled() {
    return this.curveManipulationEnabled_;
  }
  /**
   * Enable on-demand loading.
   *
   * <p>By default, when on-demand loading is not enabled, the entire chart area is loaded,
   * regardless of whether it is within the current zoom range of the X axis.
   *
   * <p>When on-demand loading is enabled only the currently visible area + some margin is loaded.
   * As the visible area changes, different data is loaded. This improves performance for charts
   * with a lot of data if not all of the data needs to be visible at the same time.
   *
   * <p>This feature is especially useful in combination with {@link
   * WAxis#setMaximumZoomRange(double size) WAxis#setMaximumZoomRange()} or {@link
   * WAxis#setMinZoom(double minZoom) WAxis#setMinZoom()}, which makes it impossible for the user to
   * view all of the data at the same time, because that would incur too much overhead.
   *
   * <p>
   *
   * <p><i><b>Note: </b>On-demand loading requires that the X axis data for all data series is
   * sorted in ascending order. This feature is optimized for equidistant X axis data, but
   * that&apos;s not a requirement. </i>
   *
   * <p><i><b>Note: </b>If no minimum or maximum are set on the Y axis (or axes), then the chart
   * will still have to scan all data of its data series to automatically determine the minimum and
   * maximum Y axis values. If this performance hit is undesirable and the Y axis range is known or
   * guaranteed to be within a certain range, make sure to {@link WAxis#setRange(double minimum,
   * double maximum) set a range} on the Y axis (or axes). </i>
   *
   * @see WCartesianChart#isOnDemandLoadingEnabled()
   */
  public void setOnDemandLoadingEnabled(boolean enabled) {
    if (this.onDemandLoadingEnabled_ != enabled) {
      this.onDemandLoadingEnabled_ = enabled;
      this.update();
    }
  }
  /**
   * Returns whether on-demand loading is enabled.
   *
   * <p>
   *
   * @see WCartesianChart#setOnDemandLoadingEnabled(boolean enabled)
   */
  public boolean isOnDemandLoadingEnabled() {
    return this.onDemandLoadingEnabled_;
  }
  /**
   * Set the background brush for the unloaded area.
   *
   * <p>
   *
   * @see WCartesianChart#setOnDemandLoadingEnabled(boolean enabled)
   * @see WCartesianChart#getLoadingBackground()
   */
  public void setLoadingBackground(final WBrush brush) {
    if (!this.loadingBackground_.equals(brush)) {
      this.loadingBackground_ = brush;
      if (this.isOnDemandLoadingEnabled()) {
        this.update();
      }
    }
  }
  /**
   * Returns the background brush for the unloaded area.
   *
   * <p>
   *
   * @see WCartesianChart#setOnDemandLoadingEnabled(boolean enabled)
   * @see WCartesianChart#setLoadingBackground(WBrush brush)
   */
  public WBrush getLoadingBackground() {
    return this.loadingBackground_;
  }

  public void iterateSeries(
      SeriesIterator iterator, WPainter painter, boolean reverseStacked, boolean extremesOnly) {
    double groupWidth = 0.0;
    int numBarGroups;
    int currentBarGroup;
    int rowCount = this.getModel() != null ? this.getModel().getRowCount() : 0;
    List<Double> posStackedValuesInit = new ArrayList<Double>();
    List<Double> minStackedValuesInit = new ArrayList<Double>();
    final boolean scatterPlot = this.type_ == ChartType.Scatter;
    if (scatterPlot) {
      numBarGroups = 1;
      currentBarGroup = 0;
    } else {
      numBarGroups = this.getCalcNumBarGroups();
      currentBarGroup = 0;
      {
        int insertPos = 0;
        for (int ii = 0; ii < (rowCount); ++ii) posStackedValuesInit.add(insertPos + ii, 0.0);
      }
      ;
      {
        int insertPos = 0;
        for (int ii = 0; ii < (rowCount); ++ii) minStackedValuesInit.add(insertPos + ii, 0.0);
      }
      ;
    }
    boolean containsBars = false;
    for (int g = 0; g < this.series_.size(); ++g) {
      if (this.series_.get(g).isHidden()
          && !(this.axisSliderWidgetForSeries(this.series_.get(g))
              && (ObjectUtils.cast(iterator, SeriesRenderIterator.class) != null
                  || ObjectUtils.cast(iterator, ExtremesIterator.class) != null))) {
        continue;
      }
      groupWidth =
          this.series_.get(g).getBarWidth()
              * (this.getXAxis(this.series_.get(g).getXAxis()).mapToDevice(2)
                  - this.getXAxis(this.series_.get(g).getXAxis()).mapToDevice(1));
      if (containsBars) {
        ++currentBarGroup;
      }
      containsBars = false;
      int startSeries;
      int endSeries;
      if (scatterPlot) {
        startSeries = endSeries = g;
      } else {
        if (this.series_.get(g).getModel() == this.getModel()) {
          for (int i = 0; i < rowCount; ++i) {
            posStackedValuesInit.set(i, minStackedValuesInit.set(i, 0.0));
          }
          if (reverseStacked) {
            endSeries = g;
            int xAxis = this.series_.get(g).getXAxis();
            int yAxis = this.series_.get(g).getYAxis();
            for (; ; ) {
              if (g < this.series_.size()
                  && ((int) g == endSeries || this.series_.get(g).isStacked())
                  && this.series_.get(g).getXAxis() == xAxis
                  && this.series_.get(g).getYAxis() == yAxis) {
                if (this.series_.get(g).getType() == SeriesType.Bar) {
                  containsBars = true;
                }
                for (int row = 0; row < rowCount; ++row) {
                  double y =
                      StringUtils.asNumber(
                          this.getModel().getData(row, this.series_.get(g).getModelColumn()));
                  if (!Double.isNaN(y)) {
                    if (y > 0) {
                      posStackedValuesInit.set(row, posStackedValuesInit.get(row) + y);
                    } else {
                      minStackedValuesInit.set(row, minStackedValuesInit.get(row) + y);
                    }
                  }
                }
                ++g;
              } else {
                break;
              }
            }
            --g;
            startSeries = g;
          } else {
            startSeries = g;
            int xAxis = this.series_.get(g).getXAxis();
            int yAxis = this.series_.get(g).getYAxis();
            if (this.series_.get(g).getType() == SeriesType.Bar) {
              containsBars = true;
            }
            ++g;
            for (; ; ) {
              if (g < this.series_.size()
                  && this.series_.get(g).isStacked()
                  && this.series_.get(g).getXAxis() == xAxis
                  && this.series_.get(g).getYAxis() == yAxis) {
                if (this.series_.get(g).getType() == SeriesType.Bar) {
                  containsBars = true;
                }
                ++g;
              } else {
                break;
              }
            }
            --g;
            endSeries = g;
          }
        } else {
          throw new WException(
              "Configuring different models for WDataSeries are unsupported for category charts!");
        }
      }
      int i = startSeries;
      for (; ; ) {
        boolean doSeries =
            this.series_.get(i).getXAxis() >= 0
                && this.series_.get(i).getXAxis() < this.getXAxisCount()
                && this.series_.get(i).getYAxis() >= 0
                && this.series_.get(i).getYAxis() < this.getYAxisCount()
                && iterator.startSeries(
                    this.series_.get(i), groupWidth, numBarGroups, currentBarGroup);
        List<Double> posStackedValues = new ArrayList<Double>();
        List<Double> minStackedValues = new ArrayList<Double>();
        if (doSeries || !scatterPlot && i != endSeries) {
          for (int currentXSegment = 0;
              currentXSegment < this.getXAxis(this.series_.get(i).getXAxis()).getSegmentCount();
              ++currentXSegment) {
            for (int currentYSegment = 0;
                currentYSegment < this.getYAxis(this.series_.get(i).getYAxis()).getSegmentCount();
                ++currentYSegment) {
              posStackedValues.clear();
              posStackedValues.addAll(posStackedValuesInit);
              minStackedValues.clear();
              minStackedValues.addAll(minStackedValuesInit);
              if (painter != null) {
                WRectF csa =
                    this.chartSegmentArea(
                        this.getXAxis(this.series_.get(i).getXAxis()),
                        this.getYAxis(this.series_.get(i).getYAxis()),
                        currentXSegment,
                        currentYSegment);
                iterator.startSegment(currentXSegment, currentYSegment, csa);
                painter.save();
                if (!this.isInteractive()) {
                  WPainterPath clipPath = new WPainterPath();
                  clipPath.addRect(this.hv(csa));
                  painter.setClipPath(clipPath);
                  painter.setClipping(true);
                }
              } else {
                iterator.startSegment(currentXSegment, currentYSegment, null);
              }
              int startRow = 0;
              int endRow =
                  this.series_.get(i).getModel() != null
                      ? this.series_.get(i).getModel().getRowCount()
                      : 0;
              if (this.isInteractive()
                  && !extremesOnly
                  && this.isOnDemandLoadingEnabled()
                  && this.series_.get(i).getModel() != null
                  && !this.axisSliderWidgetForSeries(this.series_.get(i))) {
                int xColumn =
                    this.series_.get(i).XSeriesColumn() == -1
                        ? this.XSeriesColumn()
                        : this.series_.get(i).XSeriesColumn();
                double zoomMin = this.getXAxis(this.series_.get(i).getXAxis()).getZoomMinimum();
                double zoomMax = this.getXAxis(this.series_.get(i).getXAxis()).getZoomMaximum();
                double zoomRange = zoomMax - zoomMin;
                if (xColumn == -1) {
                  startRow = Math.max(0, (int) (zoomMin - zoomRange));
                  endRow = Math.min(endRow, (int) Math.ceil(zoomMax + zoomRange) + 1);
                } else {
                  startRow =
                      Math.max(
                          binarySearchRow(
                                  this.series_.get(i).getModel(),
                                  xColumn,
                                  zoomMin - zoomRange,
                                  0,
                                  this.series_.get(i).getModel().getRowCount() - 1)
                              - 1,
                          startRow);
                  endRow =
                      Math.min(
                          binarySearchRow(
                                  this.series_.get(i).getModel(),
                                  xColumn,
                                  zoomMax + zoomRange,
                                  0,
                                  this.series_.get(i).getModel().getRowCount() - 1)
                              + 1,
                          endRow);
                }
              }
              for (int row = startRow; row < endRow; ++row) {
                int[] xIndex = {-1, -1};
                int[] yIndex = {-1, -1};
                double x;
                if (scatterPlot) {
                  int c = this.series_.get(i).XSeriesColumn();
                  if (c == -1) {
                    c = this.XSeriesColumn();
                  }
                  if (c != -1) {
                    xIndex[0] = row;
                    xIndex[1] = c;
                    x = this.series_.get(i).getModel().getData(xIndex[0], xIndex[1]);
                  } else {
                    x = row;
                  }
                } else {
                  x = row;
                }
                yIndex[0] = row;
                yIndex[1] = this.series_.get(i).getModelColumn();
                double y = this.series_.get(i).getModel().getData(yIndex[0], yIndex[1]);
                if (scatterPlot) {
                  iterator.newValue(
                      this.series_.get(i), x, y, 0, xIndex[0], xIndex[1], yIndex[0], yIndex[1]);
                } else {
                  double prevStack = 0;
                  double nextStack = 0;
                  boolean hasValue = !Double.isNaN(y);
                  if (hasValue) {
                    if (y > 0) {
                      prevStack = nextStack = posStackedValues.get(row);
                    } else {
                      prevStack = nextStack = minStackedValues.get(row);
                    }
                    if (reverseStacked) {
                      nextStack -= y;
                    } else {
                      nextStack += y;
                    }
                    if (y > 0) {
                      posStackedValues.set(row, nextStack);
                    } else {
                      minStackedValues.set(row, nextStack);
                    }
                  }
                  if (doSeries) {
                    if (reverseStacked) {
                      iterator.newValue(
                          this.series_.get(i),
                          x,
                          hasValue ? prevStack : y,
                          nextStack,
                          xIndex[0],
                          xIndex[1],
                          yIndex[0],
                          yIndex[1]);
                    } else {
                      iterator.newValue(
                          this.series_.get(i),
                          x,
                          hasValue ? nextStack : y,
                          prevStack,
                          xIndex[0],
                          xIndex[1],
                          yIndex[0],
                          yIndex[1]);
                    }
                  }
                }
                if (extremesOnly && this.isOnDemandLoadingEnabled()) {
                  row = Math.max(endRow - 2, row);
                }
              }
              iterator.endSegment();
              if (painter != null) {
                painter.restore();
              }
            }
          }
          posStackedValuesInit.clear();
          posStackedValuesInit.addAll(posStackedValues);
          minStackedValuesInit.clear();
          minStackedValuesInit.addAll(minStackedValues);
        }
        if (doSeries) {
          iterator.endSeries();
        }
        if (i == endSeries) {
          break;
        } else {
          if (endSeries < startSeries) {
            --i;
          } else {
            ++i;
          }
        }
      }
    }
  }

  public final void iterateSeries(SeriesIterator iterator, WPainter painter) {
    iterateSeries(iterator, painter, false, false);
  }

  public final void iterateSeries(
      SeriesIterator iterator, WPainter painter, boolean reverseStacked) {
    iterateSeries(iterator, painter, reverseStacked, false);
  }

  void addAxisSliderWidget(WAxisSliderWidget slider) {
    this.axisSliderWidgets_.add(slider);
    StringBuilder ss = new StringBuilder();
    ss.append('[');
    for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
      if (i != 0) {
        ss.append(',');
      }
      ss.append('"').append(this.axisSliderWidgets_.get(i).getId()).append('"');
    }
    ss.append(']');
    this.updateJSConfig("sliders", ss.toString());
  }

  void removeAxisSliderWidget(WAxisSliderWidget slider) {
    for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
      if (slider == this.axisSliderWidgets_.get(i)) {
        this.axisSliderWidgets_.remove(0 + i);
        StringBuilder ss = new StringBuilder();
        ss.append('[');
        for (int j = 0; j < this.axisSliderWidgets_.size(); ++j) {
          if (j != 0) {
            ss.append(',');
          }
          ss.append('"').append(this.axisSliderWidgets_.get(j).getId()).append('"');
        }
        ss.append(']');
        this.updateJSConfig("sliders", ss.toString());
        return;
      }
    }
  }

  static class AxisLocation {
    private static Logger logger = LoggerFactory.getLogger(AxisLocation.class);

    public AxisLocation() {
      this.minOffset = 0;
      this.maxOffset = 0;
      this.initLoc = AxisValue.Minimum;
      this.finLoc = AxisValue.Minimum;
    }

    public int minOffset;
    public int maxOffset;
    public AxisValue initLoc;
    public AxisValue finLoc;
  }

  static class PenAssignment {
    private static Logger logger = LoggerFactory.getLogger(PenAssignment.class);

    public WJavaScriptHandle<WPen> pen;
    public WJavaScriptHandle<WPen> textPen;
    public WJavaScriptHandle<WPen> gridPen;

    public PenAssignment(
        final WJavaScriptHandle<WPen> pen,
        final WJavaScriptHandle<WPen> textPen,
        final WJavaScriptHandle<WPen> gridPen) {
      this.pen = pen;
      this.textPen = textPen;
      this.gridPen = gridPen;
    }
  }

  static class AxisStruct {
    private static Logger logger = LoggerFactory.getLogger(AxisStruct.class);

    public AxisStruct() {
      this.axis = new WAxis();
      this.calculatedWidth = 0;
      this.location = new WCartesianChart.AxisLocation();
      this.transform = new WTransform();
      this.transformHandle = null;
      this.transformChanged = null;
      this.pens = new ArrayList<WCartesianChart.PenAssignment>();
    }

    public AxisStruct(WAxis ax) {
      this.axis = ax;
      this.calculatedWidth = 0;
      this.location = new WCartesianChart.AxisLocation();
      this.transform = new WTransform();
      this.transformHandle = null;
      this.transformChanged = null;
      this.pens = new ArrayList<WCartesianChart.PenAssignment>();
    }

    public AxisStruct(final WCartesianChart.AxisStruct other) {
      this.axis = other.axis;
      this.calculatedWidth = other.calculatedWidth;
      this.location = other.location;
      this.transform = other.transform;
      this.transformHandle = other.transformHandle;
      this.transformChanged = other.transformChanged;
      this.pens = other.pens;
      other.calculatedWidth = 0;
      other.location = new WCartesianChart.AxisLocation();
      other.transform.assign(new WTransform());
    }

    public WAxis axis;
    public int calculatedWidth;
    public WCartesianChart.AxisLocation location;
    public WTransform transform;
    public WJavaScriptHandle<WTransform> transformHandle;
    public JSignal transformChanged;
    public List<WCartesianChart.PenAssignment> pens;
  }

  private WChart2DImplementation interface_;
  private Orientation orientation_;
  private int XSeriesColumn_;
  private ChartType type_;
  private List<WDataSeries> series_;
  List<WCartesianChart.AxisStruct> xAxes_;
  private List<WCartesianChart.AxisStruct> yAxes_;
  private double barMargin_;
  private WLegend legend_;
  private int axisPadding_;
  private WPen borderPen_;
  WPen textPen_;
  private int width_;
  private int height_;
  WRectF chartArea_;
  boolean hasDeferredToolTips_;
  private boolean jsDefined_;
  private boolean zoomEnabled_;
  private boolean panEnabled_;
  private boolean rubberBandEnabled_;
  private boolean crosshairEnabled_;
  private WColor crosshairColor_;
  private int crosshairXAxis_;
  private int crosshairYAxis_;
  private boolean seriesSelectionEnabled_;
  private WDataSeries selectedSeries_;
  private WDataSeries followCurve_;
  private boolean curveManipulationEnabled_;
  private boolean onDemandLoadingEnabled_;
  private WBrush loadingBackground_;
  boolean cObjCreated_;
  private Signal2<WDataSeries, WPointF> seriesSelected_;
  private JSignal2<Double, Double> jsSeriesSelected_;
  Map<WDataSeries, WJavaScriptHandle<WPainterPath>> curvePaths_;
  private List<WJavaScriptHandle<WPainterPath>> freePainterPaths_;
  Map<WDataSeries, WJavaScriptHandle<WTransform>> curveTransforms_;
  private List<WJavaScriptHandle<WTransform>> freeTransforms_;
  private List<WJavaScriptHandle<WPen>> freePens_;
  private List<CurveLabel> curveLabels_;
  private List<WAxisSliderWidget> axisSliderWidgets_;
  private Map<EnumSet<KeyboardModifier>, InteractiveAction> wheelActions_;
  private JSignal2<Double, Double> loadTooltip_;

  static class BarTooltip {
    private static Logger logger = LoggerFactory.getLogger(BarTooltip.class);

    public BarTooltip(final WDataSeries series, int xRow, int xColumn, int yRow, int yColumn) {
      this.series = series;
      this.xRow = xRow;
      this.xColumn = xColumn;
      this.yRow = yRow;
      this.yColumn = yColumn;
    }

    public double[] xs = new double[4];
    public double[] ys = new double[4];
    public WDataSeries series;
    public int xRow;
    public int xColumn;
    public int yRow;
    public int yColumn;
  }

  List<WCartesianChart.BarTooltip> barTooltips_;

  private void init() {
    this.setPalette(new WStandardPalette(PaletteFlavour.Muted));
    this.xAxes_.add(new WCartesianChart.AxisStruct());
    this.yAxes_.add(new WCartesianChart.AxisStruct());
    this.yAxes_.add(new WCartesianChart.AxisStruct());
    this.yAxes_.get(this.yAxes_.size() - 1).axis.setLocation(AxisValue.Maximum);
    this.getAxis(Axis.X).init(this.interface_, Axis.X);
    this.getAxis(Axis.Y1).init(this.interface_, Axis.Y1);
    this.getAxis(Axis.Y2).init(this.interface_, Axis.Y2);
    this.getAxis(Axis.Y2).setVisible(false);
    this.getAxis(Axis.X).setPadding(this.axisPadding_);
    this.getAxis(Axis.Y1).setPadding(this.axisPadding_);
    this.getAxis(Axis.Y2).setPadding(this.axisPadding_);
    this.getAxis(Axis.X).setSoftLabelClipping(true);
    this.getAxis(Axis.Y1).setSoftLabelClipping(true);
    this.getAxis(Axis.Y2).setSoftLabelClipping(true);
    this.setPlotAreaPadding(40, EnumUtils.or(EnumSet.of(Side.Left), Side.Right));
    this.setPlotAreaPadding(30, EnumUtils.or(EnumSet.of(Side.Top), Side.Bottom));
    this.xAxes_.get(0).transformHandle = this.createJSTransform();
    this.xAxes_.get(0).transformChanged = new JSignal(this, "xTransformChanged0");
    for (int i = 0; i < 2; ++i) {
      this.yAxes_.get(i).transformHandle = this.createJSTransform();
      this.yAxes_.get(i).transformChanged =
          new JSignal(this, "yTransformChanged" + String.valueOf(i));
    }
    if (WApplication.getInstance() != null
        && WApplication.getInstance().getEnvironment().hasAjax()) {
      this.mouseWentDown()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.mouseDown(o, e);}}");
      this.mouseWentUp()
          .addListener("function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.mouseUp(o, e);}}");
      this.mouseDragged()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.mouseDrag(o, e);}}");
      this.mouseMoved()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.mouseMove(o, e);}}");
      this.mouseWheel()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.mouseWheel(o, e);}}");
      this.mouseWentOut()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.mouseOut(o, e);}}");
      this.touchStarted()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.touchStart(o, e);}}");
      this.touchEnded()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.touchEnd(o, e);}}");
      this.touchMoved()
          .addListener(
              "function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.touchMoved(o, e);}}");
      this.clicked()
          .addListener("function(o, e){var o=" + this.getCObjJsRef() + ";if(o){o.clicked(o, e);}}");
      this.jsSeriesSelected_.addListener(
          this,
          (Double e1, Double e2) -> {
            WCartesianChart.this.jsSeriesSelected(e1, e2);
          });
      this.loadTooltip_.addListener(
          this,
          (Double e1, Double e2) -> {
            WCartesianChart.this.loadTooltip(e1, e2);
          });
      this.voidEventSignal("dragstart", true).preventDefaultAction(true);
    }
    this.wheelActions_.put(EnumSet.of(KeyboardModifier.None), InteractiveAction.PanMatching);
    this.wheelActions_.put(
        EnumUtils.or(EnumSet.of(KeyboardModifier.Alt), KeyboardModifier.Control),
        InteractiveAction.ZoomX);
    this.wheelActions_.put(
        EnumUtils.or(EnumSet.of(KeyboardModifier.Control), KeyboardModifier.Shift),
        InteractiveAction.ZoomY);
    this.wheelActions_.put(EnumSet.of(KeyboardModifier.Control), InteractiveAction.ZoomXY);
    this.wheelActions_.put(
        EnumUtils.or(
            EnumUtils.or(EnumSet.of(KeyboardModifier.Alt), KeyboardModifier.Control),
            KeyboardModifier.Shift),
        InteractiveAction.ZoomXY);
  }

  private static String wheelActionsToJson(
      Map<EnumSet<KeyboardModifier>, InteractiveAction> wheelActions) {
    StringBuilder ss = new StringBuilder();
    ss.append('{');
    boolean first = true;
    for (Iterator<Map.Entry<EnumSet<KeyboardModifier>, InteractiveAction>> it_it =
            wheelActions.entrySet().iterator();
        it_it.hasNext(); ) {
      Map.Entry<EnumSet<KeyboardModifier>, InteractiveAction> it = it_it.next();
      if (first) {
        first = false;
      } else {
        ss.append(',');
      }
      ss.append(EnumUtils.valueOf(it.getKey())).append(':').append((int) it.getValue().getValue());
    }
    ss.append('}');
    return ss.toString();
  }

  static WColor lightenColor(final WColor in) {
    double[] hsl = new double[3];
    in.toHSL(hsl);
    double h = hsl[0];
    double s = hsl[1];
    double l = hsl[2];
    return WColor.fromHSL(h, Math.max(s - 0.2, 0.0), Math.min(l + 0.2, 1.0), in.getAlpha());
  }

  protected void getDomChanges(final List<DomElement> result, WApplication app) {
    super.getDomChanges(result, app);
    for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
      this.axisSliderWidgets_.get(i).update();
    }
  }

  protected void modelChanged() {
    this.XSeriesColumn_ = -1;
    while (this.axisSliderWidgets_.size() > 0) {
      this.axisSliderWidgets_.get(this.axisSliderWidgets_.size() - 1).setSeries((WDataSeries) null);
    }
    this.freeAllJSPaths();
    this.freeAllJSTransforms();
    this.series_.clear();
    this.update();
  }

  protected void modelReset() {
    this.update();
  }
  /**
   * Paints the widget.
   *
   * <p>This calls {@link WCartesianChart#render(WPainter painter, WRectF rectangle) render()} to
   * paint on the paint device.
   */
  protected void paintEvent(WPaintDevice paintDevice) {
    this.hasDeferredToolTips_ = false;
    WPainter painter = new WPainter(paintDevice);
    painter.setRenderHint(RenderHint.Antialiasing);
    this.paint(painter);
    if (this.hasDeferredToolTips_ && !this.jsDefined_) {
      this.defineJavaScript();
    }
    if (this.isInteractive() || this.hasDeferredToolTips_) {
      this.setZoomAndPan();
      List<WRectF> xModelAreas = new ArrayList<WRectF>();
      List<WRectF> yModelAreas = new ArrayList<WRectF>();
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        double modelBottom = this.getYAxis(0).mapFromDevice(0);
        double modelTop = this.getYAxis(0).mapFromDevice(this.chartArea_.getHeight());
        double modelLeft = this.getXAxis(i).mapFromDevice(0);
        double modelRight = this.getXAxis(i).mapFromDevice(this.chartArea_.getWidth());
        WRectF modelArea =
            new WRectF(modelLeft, modelBottom, modelRight - modelLeft, modelTop - modelBottom);
        xModelAreas.add(modelArea);
      }
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        double modelBottom = this.getYAxis(i).mapFromDevice(0);
        double modelTop = this.getYAxis(i).mapFromDevice(this.chartArea_.getHeight());
        double modelLeft = this.getAxis(Axis.X).mapFromDevice(0);
        double modelRight = this.getAxis(Axis.X).mapFromDevice(this.chartArea_.getWidth());
        WRectF modelArea =
            new WRectF(modelLeft, modelBottom, modelRight - modelLeft, modelTop - modelBottom);
        yModelAreas.add(modelArea);
      }
      WRectF insideArea = this.getInsideChartArea();
      int coordPaddingX = 5;
      int coordPaddingY = 5;
      if (this.getOrientation() == Orientation.Vertical) {
        for (int i = 0; i < this.getXAxisCount(); ++i) {
          if (this.getXAxis(i).isVisible()
              && (this.xAxes_.get(i).location.initLoc == AxisValue.Maximum
                  || this.xAxes_.get(i).location.initLoc == AxisValue.Both)) {
            if (this.getXAxis(i).getTickDirection() == TickDirection.Inwards) {
              coordPaddingY = 25;
            }
            break;
          }
        }
        for (int i = 0; i < this.getYAxisCount(); ++i) {
          if (this.getYAxis(i).isVisible()
              && (this.yAxes_.get(i).location.initLoc == AxisValue.Maximum
                  || this.yAxes_.get(i).location.initLoc == AxisValue.Both)) {
            if (this.getYAxis(i).getTickDirection() == TickDirection.Inwards) {
              coordPaddingX = 40;
            }
            break;
          }
        }
      } else {
        for (int i = 0; i < this.getXAxisCount(); ++i) {
          if (this.getXAxis(i).isVisible()
              && (this.xAxes_.get(i).location.initLoc == AxisValue.Maximum
                  || this.xAxes_.get(i).location.initLoc == AxisValue.Both)) {
            if (this.getXAxis(i).getTickDirection() == TickDirection.Inwards) {
              coordPaddingX = 40;
            }
            break;
          }
        }
        for (int i = 0; i < this.getYAxisCount(); ++i) {
          if (this.getYAxis(i).isVisible()
              && (this.yAxes_.get(i).location.initLoc == AxisValue.Minimum
                  || this.yAxes_.get(i).location.initLoc == AxisValue.Both)) {
            if (this.getYAxis(i).getTickDirection() == TickDirection.Inwards) {
              coordPaddingY = 25;
            }
            break;
          }
        }
      }
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        if ((this.getXAxis(i).zoomRangeChanged().isConnected() || this.isOnDemandLoadingEnabled())
            && !this.xAxes_.get(i).transformChanged.isConnected()) {
          final int axis = i;
          this.xAxes_
              .get(i)
              .transformChanged
              .addListener(
                  this,
                  () -> {
                    WCartesianChart.this.xTransformChanged(axis);
                  });
        }
      }
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        if ((this.getYAxis(i).zoomRangeChanged().isConnected() || this.isOnDemandLoadingEnabled())
            && !this.yAxes_.get(i).transformChanged.isConnected()) {
          final int axis = i;
          this.yAxes_
              .get(i)
              .transformChanged
              .addListener(
                  this,
                  () -> {
                    WCartesianChart.this.yTransformChanged(axis);
                  });
        }
      }
      char[] buf = new char[30];
      WApplication app = WApplication.getInstance();
      StringBuilder ss = new StringBuilder();
      int selectedCurve =
          this.selectedSeries_ != null ? this.getSeriesIndexOf(this.selectedSeries_) : -1;
      int followCurve = this.followCurve_ != null ? this.getSeriesIndexOf(this.followCurve_) : -1;
      ss.append("new Wt4.11.2.WCartesianChart(")
          .append(app.getJavaScriptClass())
          .append(",")
          .append(this.getJsRef())
          .append(",")
          .append(this.getObjJsRef())
          .append(",{curveManipulation:")
          .append(StringUtils.asString(this.curveManipulationEnabled_).toString())
          .append(",seriesSelection:")
          .append(StringUtils.asString(this.seriesSelectionEnabled_).toString())
          .append(",selectedCurve:")
          .append(selectedCurve)
          .append(",isHorizontal:")
          .append(StringUtils.asString(this.getOrientation() == Orientation.Horizontal).toString())
          .append(",zoom:")
          .append(StringUtils.asString(this.zoomEnabled_).toString())
          .append(",pan:")
          .append(StringUtils.asString(this.panEnabled_).toString())
          .append(",crosshair:")
          .append(StringUtils.asString(this.crosshairEnabled_).toString())
          .append(",crosshairXAxis:")
          .append(this.crosshairXAxis_)
          .append(",crosshairYAxis:")
          .append(this.crosshairYAxis_)
          .append(",crosshairColor:")
          .append(jsStringLiteral(this.crosshairColor_.getCssText(true)))
          .append(",followCurve:")
          .append(followCurve)
          .append(",xTransforms:[");
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(this.xAxes_.get(i).transformHandle.getJsRef());
      }
      ss.append("],yTransforms:[");
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(this.yAxes_.get(i).transformHandle.getJsRef());
      }
      ss.append("],area:")
          .append(this.hv(this.chartArea_).getJsRef())
          .append(",insideArea:")
          .append(this.hv(insideArea).getJsRef())
          .append(",xModelAreas:[");
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(xModelAreas.get(i).getJsRef());
      }
      ss.append("],yModelAreas:[");
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(yModelAreas.get(i).getJsRef());
      }
      ss.append("],");
      ss.append("hasToolTips:")
          .append(StringUtils.asString(this.hasDeferredToolTips_).toString())
          .append(",notifyTransform:{x:[");
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(
            StringUtils.asString(
                    this.getXAxis(i).zoomRangeChanged().isConnected()
                        || this.isOnDemandLoadingEnabled())
                .toString());
      }
      ss.append("], y:[");
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(
            StringUtils.asString(
                    this.getYAxis(i).zoomRangeChanged().isConnected()
                        || this.isOnDemandLoadingEnabled())
                .toString());
      }
      ss.append("]},ToolTipInnerStyle:")
          .append(jsStringLiteral(app.getTheme().utilityCssClass(UtilityCssClassRole.ToolTipInner)))
          .append(",ToolTipOuterStyle:")
          .append(jsStringLiteral(app.getTheme().utilityCssClass(UtilityCssClassRole.ToolTipOuter)))
          .append(",");
      this.updateJSPens(ss);
      ss.append("series:{");
      {
        boolean firstCurvePath = true;
        for (int i = 0; i < this.series_.size(); ++i) {
          if (this.curvePaths_.get(this.series_.get(i)) != null) {
            if (firstCurvePath) {
              firstCurvePath = false;
            } else {
              ss.append(",");
            }
            ss.append((int) i).append(":{");
            ss.append("curve:")
                .append(this.curvePaths_.get(this.series_.get(i)).getJsRef())
                .append(",");
            ss.append("transform:")
                .append(this.curveTransforms_.get(this.series_.get(i)).getJsRef())
                .append(",");
            ss.append("xAxis:").append(this.series_.get(i).getXAxis()).append(',');
            ss.append("yAxis:").append(this.series_.get(i).getYAxis());
            ss.append("}");
          }
        }
      }
      ss.append("},");
      ss.append("minZoom:{x:[");
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(MathUtils.roundJs(this.getXAxis(i).getMinZoom(), 16));
      }
      ss.append("],y:[");
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(MathUtils.roundJs(this.getYAxis(i).getMinZoom(), 16));
      }
      ss.append("]},");
      ss.append("maxZoom:{x:[");
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(MathUtils.roundJs(this.getXAxis(i).getMaxZoom(), 16));
      }
      ss.append("],y:[");
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append(MathUtils.roundJs(this.getYAxis(i).getMaxZoom(), 16));
      }
      ss.append("]},");
      ss.append("rubberBand:").append(this.rubberBandEnabled_).append(',');
      ss.append("sliders:[");
      for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append('"').append(this.axisSliderWidgets_.get(i).getId()).append('"');
      }
      ss.append("],");
      ss.append("wheelActions:").append(wheelActionsToJson(this.wheelActions_)).append(",");
      ss.append("coordinateOverlayPadding:[").append(coordPaddingX).append(",");
      ss.append(coordPaddingY).append("],");
      ss.append("xAxes:[");
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append('{');
        ss.append("width:")
            .append(MathUtils.roundJs(this.xAxes_.get(i).calculatedWidth, 16))
            .append(',');
        ss.append("side:'").append(locToJsString(this.xAxes_.get(i).location.initLoc)).append("',");
        ss.append("minOffset:").append(this.xAxes_.get(i).location.minOffset).append(',');
        ss.append("maxOffset:").append(this.xAxes_.get(i).location.maxOffset);
        ss.append('}');
      }
      ss.append("],");
      ss.append("yAxes:[");
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        if (i != 0) {
          ss.append(',');
        }
        ss.append('{');
        ss.append("width:")
            .append(MathUtils.roundJs(this.yAxes_.get(i).calculatedWidth, 16))
            .append(',');
        ss.append("side:'").append(locToJsString(this.yAxes_.get(i).location.initLoc)).append("',");
        ss.append("minOffset:").append(this.yAxes_.get(i).location.minOffset).append(',');
        ss.append("maxOffset:").append(this.yAxes_.get(i).location.maxOffset);
        ss.append('}');
      }
      ss.append("]});");
      this.doJavaScript(ss.toString());
      this.cObjCreated_ = true;
    }
  }
  /**
   * Renders the chart.
   *
   * <p>Renders the chart within the given rectangle. To accomodate both rendering of horizontal and
   * vertically oriented charts, all rendering logic assumes horizontal. This &quot;chart
   * coordinates&quot; space is transformed to painter coordinates using {@link
   * WCartesianChart#hv(double x, double y) hv()}.
   */
  protected void render(final WPainter painter, final WRectF rectangle) {
    painter.save();
    painter.translate(rectangle.getTopLeft());
    if (this.initLayout(rectangle, painter.getDevice())) {
      this.renderBackground(painter);
      for (int i = 0; i < this.getXAxisCount(); ++i) {
        this.renderGrid(painter, this.getXAxis(i));
      }
      for (int i = 0; i < this.getYAxisCount(); ++i) {
        this.renderGrid(painter, this.getYAxis(i));
      }
      this.renderAxes(painter, EnumSet.of(AxisProperty.Line));
      this.renderSeries(painter);
      this.renderAxes(painter, EnumSet.of(AxisProperty.Labels));
      this.renderBorder(painter);
      this.renderCurveLabels(painter);
      this.renderLegend(painter);
      this.renderOther(painter);
    }
    painter.restore();
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>The result needs further transformation using {@link WCartesianChart#hv(double x, double y)
   * hv()} to painter coordinates.
   */
  protected WPointF map(
      double xValue, double yValue, Axis yAxis, int currentXSegment, int currentYSegment) {
    return this.map(xValue, yValue, yAxis == Axis.Y1 ? 0 : 1, currentXSegment, currentYSegment);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>Returns {@link #map(double xValue, double yValue, Axis yAxis, int currentXSegment, int
   * currentYSegment) map(xValue, yValue, Axis.Ordinate, 0, 0)}
   */
  protected final WPointF map(double xValue, double yValue) {
    return map(xValue, yValue, Axis.Ordinate, 0, 0);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>Returns {@link #map(double xValue, double yValue, Axis yAxis, int currentXSegment, int
   * currentYSegment) map(xValue, yValue, yAxis, 0, 0)}
   */
  protected final WPointF map(double xValue, double yValue, Axis yAxis) {
    return map(xValue, yValue, yAxis, 0, 0);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>Returns {@link #map(double xValue, double yValue, Axis yAxis, int currentXSegment, int
   * currentYSegment) map(xValue, yValue, yAxis, currentXSegment, 0)}
   */
  protected final WPointF map(double xValue, double yValue, Axis yAxis, int currentXSegment) {
    return map(xValue, yValue, yAxis, currentXSegment, 0);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>The result needs further transformation using {@link WCartesianChart#hv(double x, double y)
   * hv()} to painter coordinates.
   */
  protected WPointF map(
      double xValue, double yValue, int yAxis, int currentXSegment, int currentYSegment) {
    return this.map(
        xValue, yValue, this.getXAxis(0), this.getYAxis(yAxis), currentXSegment, currentYSegment);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>Returns {@link #map(double xValue, double yValue, int yAxis, int currentXSegment, int
   * currentYSegment) map(xValue, yValue, yAxis, 0, 0)}
   */
  protected final WPointF map(double xValue, double yValue, int yAxis) {
    return map(xValue, yValue, yAxis, 0, 0);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>Returns {@link #map(double xValue, double yValue, int yAxis, int currentXSegment, int
   * currentYSegment) map(xValue, yValue, yAxis, currentXSegment, 0)}
   */
  protected final WPointF map(double xValue, double yValue, int yAxis, int currentXSegment) {
    return map(xValue, yValue, yAxis, currentXSegment, 0);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>The result needs further transformation using {@link WCartesianChart#hv(double x, double y)
   * hv()} to painter coordinates.
   */
  protected WPointF map(
      double xValue,
      double yValue,
      final WAxis xAxis,
      final WAxis yAxis,
      int currentXSegment,
      int currentYSegment) {
    double x = this.chartArea_.getLeft() + xAxis.mapToDevice(xValue, currentXSegment);
    double y = this.chartArea_.getBottom() - yAxis.mapToDevice(yValue, currentYSegment);
    return new WPointF(x, y);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>Returns {@link #map(double xValue, double yValue, WAxis xAxis, WAxis yAxis, int
   * currentXSegment, int currentYSegment) map(xValue, yValue, xAxis, yAxis, 0, 0)}
   */
  protected final WPointF map(double xValue, double yValue, final WAxis xAxis, final WAxis yAxis) {
    return map(xValue, yValue, xAxis, yAxis, 0, 0);
  }
  /**
   * Map (x, y) value pair to chart coordinates coordinates.
   *
   * <p>Returns {@link #map(double xValue, double yValue, WAxis xAxis, WAxis yAxis, int
   * currentXSegment, int currentYSegment) map(xValue, yValue, xAxis, yAxis, currentXSegment, 0)}
   */
  protected final WPointF map(
      double xValue, double yValue, final WAxis xAxis, final WAxis yAxis, int currentXSegment) {
    return map(xValue, yValue, xAxis, yAxis, currentXSegment, 0);
  }
  /**
   * Utility function for rendering text.
   *
   * <p>This method renders text on the chart position <i>pos</i>, with a particular alignment
   * <i>flags</i>. These are both specified in chart coordinates. The position is converted to
   * painter coordinates using {@link WCartesianChart#hv(double x, double y) hv()}, and the
   * alignment flags are changed accordingly. The rotation, indicated by <i>angle</i> is specified
   * in painter coordinates and thus an angle of 0 always indicates horizontal text, regardless of
   * the chart orientation.
   */
  protected void renderLabel(
      final WPainter painter,
      final CharSequence text,
      final WPointF p,
      EnumSet<AlignmentFlag> flags,
      double angle,
      int margin) {
    AlignmentFlag horizontalAlign =
        EnumUtils.enumFromSet(EnumUtils.mask(flags, AlignmentFlag.AlignHorizontalMask));
    AlignmentFlag verticalAlign =
        EnumUtils.enumFromSet(EnumUtils.mask(flags, AlignmentFlag.AlignVerticalMask));
    AlignmentFlag rHorizontalAlign = horizontalAlign;
    AlignmentFlag rVerticalAlign = verticalAlign;
    double width = 1000;
    double height = 20;
    WPointF pos = this.hv(p);
    if (this.getOrientation() == Orientation.Horizontal) {
      switch (horizontalAlign) {
        case Left:
          rVerticalAlign = AlignmentFlag.Top;
          break;
        case Center:
          rVerticalAlign = AlignmentFlag.Middle;
          break;
        case Right:
          rVerticalAlign = AlignmentFlag.Bottom;
          break;
        default:
          break;
      }
      switch (verticalAlign) {
        case Top:
          rHorizontalAlign = AlignmentFlag.Right;
          break;
        case Middle:
          rHorizontalAlign = AlignmentFlag.Center;
          break;
        case Bottom:
          rHorizontalAlign = AlignmentFlag.Left;
          break;
        default:
          break;
      }
    }
    double left = 0;
    double top = 0;
    switch (rHorizontalAlign) {
      case Left:
        left += margin;
        break;
      case Center:
        left -= width / 2;
        break;
      case Right:
        left -= width + margin;
      default:
        break;
    }
    switch (rVerticalAlign) {
      case Top:
        top += margin;
        break;
      case Middle:
        top -= height / 2;
        break;
      case Bottom:
        top -= height + margin;
        break;
      default:
        break;
    }
    WPen oldPen = painter.getPen();
    painter.setPen(this.textPen_.clone());
    WTransform oldTransform = painter.getWorldTransform().clone();
    painter.translate(pos);
    if (angle == 0) {
      painter.drawText(
          new WRectF(left, top, width, height),
          EnumUtils.or(EnumSet.of(rHorizontalAlign), rVerticalAlign),
          text);
    } else {
      painter.rotate(-angle);
      painter.drawText(
          new WRectF(left, top, width, height),
          EnumUtils.or(EnumSet.of(rHorizontalAlign), rVerticalAlign),
          text);
    }
    painter.setWorldTransform(oldTransform, false);
    painter.setPen(oldPen);
  }
  /**
   * Conversion between chart and painter coordinates.
   *
   * <p>Converts from chart coordinates to painter coordinates, taking into account the chart
   * orientation.
   */
  protected WPointF hv(double x, double y) {
    return this.hv(x, y, this.height_);
  }
  /**
   * Conversion between chart and painter coordinates.
   *
   * <p>Converts from chart coordinates to painter coordinates, taking into account the chart
   * orientation.
   */
  protected WPointF hv(final WPointF p) {
    if (p.isJavaScriptBound()) {
      if (this.getOrientation() == Orientation.Vertical) {
        return p;
      } else {
        return p.swapHV(this.height_);
      }
    }
    return this.hv(p.getX(), p.getY());
  }
  /**
   * Conversion between chart and painter coordinates.
   *
   * <p>Converts from chart coordinates to painter coordinates, taking into account the chart
   * orientation.
   */
  protected WRectF hv(final WRectF r) {
    if (this.getOrientation() == Orientation.Vertical) {
      return r;
    } else {
      WPointF tl = this.hv(r.getBottomLeft());
      return new WRectF(tl.getX(), tl.getY(), r.getHeight(), r.getWidth());
    }
  }
  /**
   * Returns the segment area for a combination of X and Y segments.
   *
   * <p>This segment area is used for clipping when rendering in a particular segment.
   */
  protected WRectF chartSegmentArea(final WAxis yAxis, int xSegment, int ySegment) {
    return this.chartSegmentArea(this.getXAxis(0), yAxis, xSegment, ySegment);
  }
  /**
   * Returns the segment area for a combination of X and Y segments.
   *
   * <p>This segment area is used for clipping when rendering in a particular segment.
   */
  protected WRectF chartSegmentArea(
      final WAxis xAxis, final WAxis yAxis, int xSegment, int ySegment) {
    final WAxis.Segment xs = xAxis.segments_.get(xSegment);
    final WAxis.Segment ys = yAxis.segments_.get(ySegment);
    double xRenderStart =
        xAxis.isInverted() ? xAxis.mapToDevice(xs.renderMaximum, xSegment) : xs.renderStart;
    double xRenderEnd =
        xAxis.isInverted()
            ? xAxis.mapToDevice(xs.renderMinimum, xSegment)
            : xs.renderStart + xs.renderLength;
    double yRenderStart =
        yAxis.isInverted() ? yAxis.mapToDevice(ys.renderMaximum, ySegment) : ys.renderStart;
    double yRenderEnd =
        yAxis.isInverted()
            ? yAxis.mapToDevice(ys.renderMinimum, ySegment)
            : ys.renderStart + ys.renderLength;
    double x1 =
        this.chartArea_.getLeft()
            + xRenderStart
            + (xSegment == 0
                ? xs.renderMinimum == 0 ? 0 : -this.getAxisPadding()
                : -xAxis.getSegmentMargin() / 2);
    double x2 =
        this.chartArea_.getLeft()
            + xRenderEnd
            + (xSegment == xAxis.getSegmentCount() - 1
                ? xs.renderMaximum == 0 ? 0 : this.getAxisPadding()
                : xAxis.getSegmentMargin() / 2);
    double y1 =
        this.chartArea_.getBottom()
            - yRenderEnd
            - (ySegment == yAxis.getSegmentCount() - 1
                ? ys.renderMaximum == 0 ? 0 : this.getAxisPadding()
                : yAxis.getSegmentMargin() / 2);
    double y2 =
        this.chartArea_.getBottom()
            - yRenderStart
            + (ySegment == 0
                ? ys.renderMinimum == 0 ? 0 : this.getAxisPadding()
                : yAxis.getSegmentMargin() / 2);
    return new WRectF(
        Math.floor(x1 + 0.5), Math.floor(y1 + 0.5), Math.floor(x2 - x1), Math.floor(y2 - y1));
  }
  /**
   * Calculates the chart area.
   *
   * <p>This calculates the chartArea(), which is the rectangle (in chart coordinates) that bounds
   * the actual chart (thus excluding axes, labels, titles, legend, etc...).
   *
   * <p>
   *
   * @see WAbstractChart#getPlotAreaPadding(Side side)
   */
  protected void calcChartArea() {
    if (this.orientation_ == Orientation.Vertical) {
      this.chartArea_ =
          new WRectF(
              this.getPlotAreaPadding(Side.Left),
              this.getPlotAreaPadding(Side.Top),
              Math.max(
                  10,
                  this.width_
                      - this.getPlotAreaPadding(Side.Left)
                      - this.getPlotAreaPadding(Side.Right)),
              Math.max(
                  10,
                  this.height_
                      - this.getPlotAreaPadding(Side.Top)
                      - this.getPlotAreaPadding(Side.Bottom)));
    } else {
      this.chartArea_ =
          new WRectF(
              this.getPlotAreaPadding(Side.Top),
              this.getPlotAreaPadding(Side.Right),
              Math.max(
                  10,
                  this.width_
                      - this.getPlotAreaPadding(Side.Top)
                      - this.getPlotAreaPadding(Side.Bottom)),
              Math.max(
                  10,
                  this.height_
                      - this.getPlotAreaPadding(Side.Right)
                      - this.getPlotAreaPadding(Side.Left)));
    }
  }
  /**
   * Prepares the axes for rendering.
   *
   * <p>Computes axis properties such as the range (if not manually specified), label interval (if
   * not manually specified) and axis locations. These properties are stored within the axes.
   *
   * <p>
   *
   * @see WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
   */
  protected boolean prepareAxes(WPaintDevice device) {
    if (this.xAxes_.isEmpty()) {
      return true;
    }
    if (this.yAxes_.isEmpty()) {
      return true;
    }
    Orientation yDir = this.orientation_;
    Orientation xDir =
        this.orientation_ == Orientation.Vertical ? Orientation.Horizontal : Orientation.Vertical;
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      if (!this.xAxes_.get(i).axis.prepareRender(xDir, this.chartArea_.getWidth())) {
        return false;
      }
    }
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      if (!this.yAxes_.get(i).axis.prepareRender(yDir, this.chartArea_.getHeight())) {
        return false;
      }
    }
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      this.xAxes_.get(i).location.initLoc = this.xAxes_.get(i).axis.getLocation();
    }
    List<WAxis> minimumXaxes = this.collectAxesAtLocation(Axis.X, AxisValue.Minimum);
    int offset = 0;
    for (int i = 0; i < minimumXaxes.size(); ++i) {
      final WAxis axis = minimumXaxes.get(i);
      if (axis.getLocation() != AxisValue.Both) {
        this.xAxes_.get(axis.getXAxisId()).location.initLoc = AxisValue.Minimum;
      }
      this.xAxes_.get(axis.getXAxisId()).location.minOffset = offset;
      this.xAxes_.get(axis.getXAxisId()).calculatedWidth = this.calcAxisSize(axis, device) + 10;
      if (i == 0 && axis.getTickDirection() == TickDirection.Inwards) {
        offset += 10;
      } else {
        offset += this.xAxes_.get(axis.getXAxisId()).calculatedWidth;
      }
    }
    List<WAxis> maximumXaxes = this.collectAxesAtLocation(Axis.X, AxisValue.Maximum);
    offset = 0;
    for (int i = 0; i < maximumXaxes.size(); ++i) {
      final WAxis axis = maximumXaxes.get(i);
      if (axis.getLocation() != AxisValue.Both) {
        this.xAxes_.get(axis.getXAxisId()).location.initLoc = AxisValue.Maximum;
      }
      this.xAxes_.get(axis.getXAxisId()).location.maxOffset = offset;
      this.xAxes_.get(axis.getXAxisId()).calculatedWidth = this.calcAxisSize(axis, device) + 10;
      if (i == 0 && axis.getTickDirection() == TickDirection.Inwards) {
        offset += 10;
      } else {
        offset += this.xAxes_.get(axis.getXAxisId()).calculatedWidth;
      }
    }
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      this.xAxes_.get(i).location.finLoc = this.xAxes_.get(i).location.initLoc;
    }
    if (!minimumXaxes.isEmpty()
        && minimumXaxes.get(0).getLocation() == AxisValue.Minimum
        && minimumXaxes.get(0).getScale() != AxisScale.Discrete
        && (this.getAxis(Axis.Y).isInverted()
            ? this.yAxes_
                    .get(0)
                    .axis
                    .segments_
                    .get(this.yAxes_.get(0).axis.segments_.size() - 1)
                    .renderMaximum
                == 0
            : this.yAxes_.get(0).axis.segments_.get(0).renderMinimum == 0)
        && minimumXaxes.get(0).getTickDirection() == TickDirection.Outwards) {
      this.xAxes_.get(minimumXaxes.get(0).getXAxisId()).location.finLoc = AxisValue.Zero;
    }
    if (!maximumXaxes.isEmpty()
        && maximumXaxes.get(0).getLocation() == AxisValue.Maximum
        && maximumXaxes.get(0).getScale() != AxisScale.Discrete
        && (this.getAxis(Axis.Y).isInverted()
            ? this.yAxes_.get(0).axis.segments_.get(0).renderMinimum == 0
            : this.yAxes_
                    .get(0)
                    .axis
                    .segments_
                    .get(this.yAxes_.get(0).axis.segments_.size() - 1)
                    .renderMaximum
                == 0)) {
      this.xAxes_.get(maximumXaxes.get(0).getXAxisId()).location.finLoc = AxisValue.Zero;
    }
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      this.yAxes_.get(i).location.initLoc = this.yAxes_.get(i).axis.getLocation();
    }
    List<WAxis> minimumYaxes = this.collectAxesAtLocation(Axis.Y, AxisValue.Minimum);
    offset = 0;
    for (int i = 0; i < minimumYaxes.size(); ++i) {
      final WAxis axis = minimumYaxes.get(i);
      if (axis.getLocation() != AxisValue.Both) {
        this.yAxes_.get(axis.getYAxisId()).location.initLoc = AxisValue.Minimum;
      }
      this.yAxes_.get(axis.getYAxisId()).location.minOffset = offset;
      this.yAxes_.get(axis.getYAxisId()).calculatedWidth = this.calcAxisSize(axis, device) + 10;
      if (i == 0 && axis.getTickDirection() == TickDirection.Inwards) {
        offset += 10;
      } else {
        offset += this.yAxes_.get(axis.getYAxisId()).calculatedWidth;
      }
    }
    List<WAxis> maximumYaxes = this.collectAxesAtLocation(Axis.Y, AxisValue.Maximum);
    offset = 0;
    for (int i = 0; i < maximumYaxes.size(); ++i) {
      final WAxis axis = maximumYaxes.get(i);
      if (axis.getLocation() != AxisValue.Both) {
        this.yAxes_.get(axis.getYAxisId()).location.initLoc = AxisValue.Maximum;
      }
      this.yAxes_.get(axis.getYAxisId()).location.maxOffset = offset;
      this.yAxes_.get(axis.getYAxisId()).calculatedWidth = this.calcAxisSize(axis, device) + 10;
      if (i == 0 && axis.getTickDirection() == TickDirection.Inwards) {
        offset += 10;
      } else {
        offset += this.yAxes_.get(axis.getYAxisId()).calculatedWidth;
      }
    }
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      this.yAxes_.get(i).location.finLoc = this.yAxes_.get(i).location.initLoc;
    }
    if (!minimumYaxes.isEmpty()
        && minimumYaxes.get(0).getLocation() == AxisValue.Minimum
        && (this.getAxis(Axis.X).isInverted()
            ? this.xAxes_
                    .get(0)
                    .axis
                    .segments_
                    .get(this.xAxes_.get(0).axis.segments_.size() - 1)
                    .renderMaximum
                == 0
            : this.xAxes_.get(0).axis.segments_.get(0).renderMinimum == 0)
        && minimumYaxes.get(0).getTickDirection() == TickDirection.Outwards) {
      this.yAxes_.get(minimumYaxes.get(0).getYAxisId()).location.finLoc = AxisValue.Zero;
    }
    if (!maximumYaxes.isEmpty()
        && maximumYaxes.get(0).getLocation() == AxisValue.Maximum
        && (this.getAxis(Axis.X).isInverted()
            ? this.xAxes_.get(0).axis.segments_.get(0).renderMinimum == 0
            : this.xAxes_
                    .get(0)
                    .axis
                    .segments_
                    .get(this.xAxes_.get(0).axis.segments_.size() - 1)
                    .renderMaximum
                == 0)) {
      this.yAxes_.get(maximumYaxes.get(0).getYAxisId()).location.finLoc = AxisValue.Zero;
    }
    return true;
  }
  /**
   * Renders the background.
   *
   * <p>
   *
   * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
   */
  protected void renderBackground(final WPainter painter) {
    if (this.getBackground().getStyle() != BrushStyle.None) {
      painter.fillRect(this.hv(this.chartArea_), this.getBackground());
    }
    if (this.isOnDemandLoadingEnabled() && this.getXAxisCount() == 1) {
      painter.save();
      WPainterPath clipPath = new WPainterPath();
      clipPath.addRect(this.hv(this.chartArea_));
      painter.setClipPath(clipPath);
      painter.setClipping(true);
      double zoomRange = this.getXAxis(0).getZoomMaximum() - this.getXAxis(0).getZoomMinimum();
      double zoomStart = this.getXAxis(0).getZoomMinimum() - zoomRange;
      double zoomEnd = this.getXAxis(0).getZoomMaximum() + zoomRange;
      double minX =
          Math.max(
              this.chartArea_.getLeft() + this.getXAxis(0).mapToDevice(zoomStart),
              this.chartArea_.getLeft());
      double maxX =
          Math.min(
              this.chartArea_.getLeft() + this.getXAxis(0).mapToDevice(zoomEnd),
              this.chartArea_.getRight());
      painter.fillRect(
          this.zoomRangeTransform(this.getXAxis(0), this.getYAxis(0))
              .map(
                  this.hv(
                      new WRectF(
                          this.chartArea_.getLeft(),
                          this.chartArea_.getTop(),
                          minX - this.chartArea_.getLeft(),
                          this.chartArea_.getHeight()))),
          this.getLoadingBackground());
      painter.fillRect(
          this.zoomRangeTransform(this.getXAxis(0), this.getYAxis(0))
              .map(
                  this.hv(
                      new WRectF(
                          maxX,
                          this.chartArea_.getTop(),
                          this.chartArea_.getRight() - maxX,
                          this.chartArea_.getHeight()))),
          this.getLoadingBackground());
      painter.restore();
    }
  }
  /**
   * Renders one or more properties of the axes.
   *
   * <p>This calls {@link WCartesianChart#renderAxis(WPainter painter, WAxis axis, EnumSet
   * properties) renderAxis()} for each axis.
   *
   * <p>
   *
   * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
   */
  protected void renderAxes(final WPainter painter, EnumSet<AxisProperty> properties) {
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      this.renderAxis(painter, this.xAxes_.get(i).axis, properties);
    }
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      this.renderAxis(painter, this.yAxes_.get(i).axis, properties);
    }
  }
  /**
   * Renders one or more properties of the axes.
   *
   * <p>Calls {@link #renderAxes(WPainter painter, EnumSet properties) renderAxes(painter,
   * EnumSet.of(propertie, properties))}
   */
  protected final void renderAxes(
      final WPainter painter, AxisProperty propertie, AxisProperty... properties) {
    renderAxes(painter, EnumSet.of(propertie, properties));
  }
  /**
   * Renders the border of the chart area.
   *
   * <p>
   *
   * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
   * @see WCartesianChart#setBorderPen(WPen pen)
   */
  protected void renderBorder(final WPainter painter) {
    WPainterPath area = new WPainterPath();
    int horizontalShift = 0;
    int verticalShift = 0;
    if (this.hasInwardsYAxisOnMaximumSide()) {
      horizontalShift = -1;
    }
    if (this.hasInwardsXAxisOnMinimumSide()) {
      verticalShift = -1;
    }
    area.addRect(
        this.hv(
            new WRectF(
                this.chartArea_.getLeft(),
                this.chartArea_.getTop(),
                this.chartArea_.getWidth() + horizontalShift,
                this.chartArea_.getHeight() + verticalShift)));
    painter.strokePath(area.getCrisp(), this.borderPen_);
  }
  /**
   * Renders the curve labels.
   *
   * <p>
   *
   * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
   * @see WCartesianChart#addCurveLabel(CurveLabel label)
   */
  protected void renderCurveLabels(final WPainter painter) {
    if (this.isInteractive()) {
      painter.save();
      WPainterPath clipPath = new WPainterPath();
      clipPath.addRect(this.hv(this.chartArea_));
      painter.setClipPath(clipPath);
      painter.setClipping(true);
    }
    for (int i = 0; i < this.curveLabels_.size(); ++i) {
      final CurveLabel label = this.curveLabels_.get(i);
      for (int j = 0; j < this.series_.size(); ++j) {
        final WDataSeries series = this.series_.get(j);
        if (series.isHidden()) {
          continue;
        }
        if (series == label.getSeries()) {
          WTransform t = this.zoomRangeTransform(series.getYAxis());
          if (series.getType() == SeriesType.Line || series.getType() == SeriesType.Curve) {
            t.assign(t.multiply(this.curveTransform(series)));
          }
          int xSegment = 0;
          double x = this.getAxis(Axis.X).getValue(label.getX());
          if (!this.isInteractive()) {
            while (xSegment < this.getAxis(Axis.X).getSegmentCount()
                && (this.getAxis(Axis.X).segments_.get(xSegment).renderMinimum > x
                    || this.getAxis(Axis.X).segments_.get(xSegment).renderMaximum < x)) {
              ++xSegment;
            }
          }
          int ySegment = 0;
          double y = this.getYAxis(series.getYAxis()).getValue(label.getY());
          if (!this.isInteractive()) {
            while (ySegment < this.getYAxis(series.getYAxis()).getSegmentCount()
                && (this.getYAxis(series.getYAxis()).segments_.get(ySegment).renderMinimum > y
                    || this.getYAxis(series.getYAxis()).segments_.get(ySegment).renderMaximum
                        < y)) {
              ++ySegment;
            }
          }
          if (xSegment < this.getAxis(Axis.X).getSegmentCount()
              && ySegment < this.getYAxis(series.getYAxis()).getSegmentCount()) {
            WPointF devicePoint =
                this.mapToDeviceWithoutTransform(
                    label.getX(), label.getY(), series.getYAxis(), xSegment, ySegment);
            WTransform translation = new WTransform().translate(t.map(devicePoint));
            painter.save();
            painter.setWorldTransform(translation);
            label.render(painter);
            painter.restore();
          }
        }
      }
    }
    if (this.isInteractive()) {
      painter.restore();
    }
  }
  /**
   * Renders all series data, including value labels.
   *
   * <p>
   *
   * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
   */
  protected void renderSeries(final WPainter painter) {
    if (this.isInteractive()) {
      painter.save();
      WPainterPath clipPath = new WPainterPath();
      clipPath.addRect(this.hv(this.chartArea_));
      painter.setClipPath(clipPath);
      painter.setClipping(true);
    }
    this.barTooltips_.clear();
    {
      SeriesRenderIterator iterator = new SeriesRenderIterator(this, painter);
      this.iterateSeries(iterator, painter, true);
    }
    {
      LabelRenderIterator iterator = new LabelRenderIterator(this, painter);
      this.iterateSeries(iterator, painter);
    }
    {
      MarkerRenderIterator iterator = new MarkerRenderIterator(this, painter);
      this.iterateSeries(iterator, painter);
    }
    if (this.isInteractive()) {
      painter.restore();
    }
  }
  /**
   * Renders the (default) legend and chart titles.
   *
   * <p>
   *
   * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
   */
  protected void renderLegend(final WPainter painter) {
    boolean vertical = this.getOrientation() == Orientation.Vertical;
    int w = vertical ? this.width_ : this.height_;
    int h = vertical ? this.height_ : this.width_;
    final int legendPadding = 10;
    int legendWidth = 0;
    int legendHeight = 0;
    if (this.isLegendEnabled()) {
      painter.save();
      int numSeriesWithLegend = 0;
      for (int i = 0; i < this.series_.size(); ++i) {
        if (this.series_.get(i).isLegendEnabled()) {
          ++numSeriesWithLegend;
        }
      }
      painter.setFont(this.getLegendFont());
      WFont f = painter.getFont();
      if (this.isAutoLayoutEnabled()
          && painter.getDevice().getFeatures().contains(PaintDeviceFeatureFlag.FontMetrics)) {
        int columnWidth = 0;
        for (int i = 0; i < this.series_.size(); ++i) {
          if (this.series_.get(i).isLegendEnabled()) {
            WString s =
                this.series_.get(i).getModel().getHeaderData(this.series_.get(i).getModelColumn());
            WTextItem t = painter.getDevice().measureText(s);
            columnWidth = Math.max(columnWidth, (int) t.getWidth());
          }
        }
        columnWidth += 25;
        WCartesianChart self = this;
        self.legend_.setLegendColumnWidth(new WLength(columnWidth));
        if (this.getLegendSide() == Side.Top || this.getLegendSide() == Side.Bottom) {
          self.legend_.setLegendColumns(Math.max(1, w / columnWidth - 1));
        }
      }
      int numLegendRows = (numSeriesWithLegend - 1) / this.getLegendColumns() + 1;
      double lineHeight = f.getSizeLength().toPixels() * 1.5;
      legendWidth =
          (int) this.getLegendColumnWidth().toPixels()
              * Math.min(this.getLegendColumns(), numSeriesWithLegend);
      legendHeight = (int) (numLegendRows * lineHeight);
      int x = 0;
      int y = 0;
      switch (this.getLegendSide()) {
        case Left:
          if (this.getLegendLocation() == LegendLocation.Inside) {
            x = this.getPlotAreaPadding(Side.Left) + legendPadding;
          } else {
            x = this.getPlotAreaPadding(Side.Left) - legendPadding - legendWidth;
          }
          break;
        case Right:
          x = w - this.getPlotAreaPadding(Side.Right);
          if (this.getLegendLocation() == LegendLocation.Inside) {
            x -= legendPadding + legendWidth;
          } else {
            x += legendPadding;
          }
          break;
        case Top:
          if (this.getLegendLocation() == LegendLocation.Inside) {
            y = this.getPlotAreaPadding(Side.Top) + legendPadding;
          } else {
            y = this.getPlotAreaPadding(Side.Top) - legendPadding - legendHeight;
          }
          break;
        case Bottom:
          y = h - this.getPlotAreaPadding(Side.Bottom);
          if (this.getLegendLocation() == LegendLocation.Inside) {
            y -= legendPadding + legendHeight;
          } else {
            y += legendPadding;
          }
        default:
          break;
      }
      switch (this.getLegendAlignment()) {
        case Top:
          y = this.getPlotAreaPadding(Side.Top) + legendPadding;
          break;
        case Middle:
          {
            double middle =
                this.getPlotAreaPadding(Side.Top)
                    + (h - this.getPlotAreaPadding(Side.Top) - this.getPlotAreaPadding(Side.Bottom))
                        / 2;
            y = (int) (middle - legendHeight / 2);
          }
          break;
        case Bottom:
          y = h - this.getPlotAreaPadding(Side.Bottom) - legendPadding - legendHeight;
          break;
        case Left:
          x = this.getPlotAreaPadding(Side.Left) + legendPadding;
          break;
        case Center:
          {
            double center =
                this.getPlotAreaPadding(Side.Left)
                    + (w - this.getPlotAreaPadding(Side.Left) - this.getPlotAreaPadding(Side.Right))
                        / 2;
            x = (int) (center - legendWidth / 2);
          }
          break;
        case Right:
          x = w - this.getPlotAreaPadding(Side.Right) - legendPadding - legendWidth;
          break;
        default:
          break;
      }
      int xOffset = 0;
      int yOffset = 0;
      if (this.getLegendLocation() == LegendLocation.Outside) {
        switch (this.getLegendSide()) {
          case Top:
            {
              if (this.getOrientation() == Orientation.Horizontal) {
                for (int i = this.getYAxisCount() - 1; i >= 0; --i) {
                  if (this.getYAxis(i).isVisible()
                      && (this.yAxes_.get(i).location.initLoc == AxisValue.Minimum
                          || this.yAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    yOffset =
                        -(this.yAxes_.get(i).location.minOffset
                            + this.yAxes_.get(i).calculatedWidth);
                    break;
                  }
                }
              } else {
                for (int i = this.getXAxisCount() - 1; i >= 0; --i) {
                  if (this.getXAxis(i).isVisible()
                      && (this.xAxes_.get(i).location.initLoc == AxisValue.Maximum
                          || this.xAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    yOffset =
                        -(this.xAxes_.get(i).location.minOffset
                            + this.xAxes_.get(i).calculatedWidth);
                    break;
                  }
                }
              }
              yOffset -= 5;
            }
            break;
          case Bottom:
            {
              if (this.getOrientation() == Orientation.Horizontal) {
                for (int i = this.getYAxisCount() - 1; i >= 0; --i) {
                  if (this.getYAxis(i).isVisible()
                      && (this.yAxes_.get(i).location.initLoc == AxisValue.Maximum
                          || this.yAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    yOffset =
                        this.yAxes_.get(i).location.maxOffset + this.yAxes_.get(i).calculatedWidth;
                    break;
                  }
                }
              } else {
                for (int i = this.getXAxisCount() - 1; i >= 0; --i) {
                  if (this.getXAxis(i).isVisible()
                      && (this.xAxes_.get(i).location.initLoc == AxisValue.Minimum
                          || this.xAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    yOffset =
                        this.xAxes_.get(i).location.maxOffset + this.xAxes_.get(i).calculatedWidth;
                    break;
                  }
                }
              }
              yOffset += 5;
            }
            break;
          case Left:
            {
              if (this.getOrientation() == Orientation.Horizontal) {
                for (int i = this.getXAxisCount() - 1; i >= 0; --i) {
                  if (this.getXAxis(i).isVisible()
                      && (this.xAxes_.get(i).location.initLoc == AxisValue.Minimum
                          || this.xAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    xOffset =
                        -(this.xAxes_.get(i).location.minOffset
                            + this.xAxes_.get(i).calculatedWidth);
                    break;
                  }
                }
              } else {
                for (int i = this.getYAxisCount() - 1; i >= 0; --i) {
                  if (this.getYAxis(i).isVisible()
                      && (this.yAxes_.get(i).location.initLoc == AxisValue.Minimum
                          || this.yAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    xOffset =
                        -(this.yAxes_.get(i).location.minOffset
                            + this.yAxes_.get(i).calculatedWidth);
                    break;
                  }
                }
              }
              xOffset -= 5;
            }
            break;
          case Right:
            {
              if (this.getOrientation() == Orientation.Horizontal) {
                for (int i = this.getXAxisCount() - 1; i >= 0; --i) {
                  if (this.getXAxis(i).isVisible()
                      && (this.xAxes_.get(i).location.initLoc == AxisValue.Maximum
                          || this.xAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    xOffset =
                        this.xAxes_.get(i).location.maxOffset + this.xAxes_.get(i).calculatedWidth;
                    break;
                  }
                }
              } else {
                for (int i = this.getYAxisCount() - 1; i >= 0; --i) {
                  if (this.getYAxis(i).isVisible()
                      && (this.yAxes_.get(i).location.initLoc == AxisValue.Maximum
                          || this.yAxes_.get(i).location.initLoc == AxisValue.Both)) {
                    xOffset =
                        this.yAxes_.get(i).location.maxOffset + this.yAxes_.get(i).calculatedWidth;
                    break;
                  }
                }
              }
              xOffset += 5;
            }
            break;
          default:
            break;
        }
      } else {
        switch (this.getLegendSide()) {
          case Top:
            yOffset = 5;
            break;
          case Bottom:
            yOffset = -5;
            break;
          case Left:
            xOffset = 5;
            break;
          case Right:
            xOffset = -5;
            break;
          default:
            break;
        }
      }
      painter.setPen(this.getLegendBorder().clone());
      painter.setBrush(this.getLegendBackground());
      painter.drawRect(
          x + xOffset - legendPadding / 2,
          y + yOffset - legendPadding / 2,
          legendWidth + legendPadding,
          legendHeight + legendPadding);
      painter.setPen(new WPen());
      painter.setFont(this.getLegendFont());
      int item = 0;
      for (int i = 0; i < this.series_.size(); ++i) {
        if (this.series_.get(i).isLegendEnabled()) {
          int col = item % this.getLegendColumns();
          int row = item / this.getLegendColumns();
          double itemX = x + xOffset + col * this.getLegendColumnWidth().toPixels();
          double itemY = y + yOffset + row * lineHeight;
          this.renderLegendItem(
              painter, new WPointF(itemX, itemY + lineHeight / 2), this.series_.get(i));
          ++item;
        }
      }
      painter.restore();
    }
    if (!(this.getTitle().length() == 0)) {
      int x =
          this.getPlotAreaPadding(Side.Left)
              + (w - this.getPlotAreaPadding(Side.Left) - this.getPlotAreaPadding(Side.Right)) / 2;
      painter.save();
      painter.setFont(this.getTitleFont());
      double titleHeight = this.getTitleFont().getSizeLength().toPixels();
      final int TITLE_PADDING = 10;
      final int TITLE_WIDTH = 1000;
      int titleOffset = 0;
      if (this.getOrientation() == Orientation.Horizontal) {
        for (int i = this.getYAxisCount() - 1; i >= 0; --i) {
          if (this.yAxes_.get(i).location.initLoc == AxisValue.Minimum
              || this.yAxes_.get(i).location.initLoc == AxisValue.Both) {
            titleOffset =
                this.yAxes_.get(i).location.minOffset + this.yAxes_.get(i).calculatedWidth;
            break;
          }
        }
      } else {
        for (int i = this.getXAxisCount() - 1; i >= 0; --i) {
          if (this.xAxes_.get(i).location.initLoc == AxisValue.Maximum
              || this.xAxes_.get(i).location.initLoc == AxisValue.Both) {
            titleOffset =
                this.xAxes_.get(i).location.minOffset + this.xAxes_.get(i).calculatedWidth;
            break;
          }
        }
      }
      if (this.getLegendSide() == Side.Top && this.getLegendLocation() == LegendLocation.Outside) {
        titleOffset += legendHeight + legendPadding + 5;
      }
      painter.drawText(
          x - TITLE_WIDTH / 2,
          this.getPlotAreaPadding(Side.Top) - titleHeight - TITLE_PADDING - titleOffset,
          TITLE_WIDTH,
          titleHeight,
          EnumUtils.or(EnumSet.of(AlignmentFlag.Center), AlignmentFlag.Top),
          this.getTitle());
      painter.restore();
    }
  }
  /**
   * Renders properties of one axis.
   *
   * <p>
   *
   * @see WCartesianChart#renderAxes(WPainter painter, EnumSet properties)
   */
  protected void renderAxis(
      final WPainter painter, final WAxis axis, EnumSet<AxisProperty> properties) {
    if (!axis.isVisible()) {
      return;
    }
    boolean isYAxis = axis.getId() != Axis.X;
    final AxisValue location =
        axis.getId() == Axis.X
            ? this.xAxes_.get(axis.getXAxisId()).location.finLoc
            : this.yAxes_.get(axis.getYAxisId()).location.finLoc;
    if (this.isInteractive()
        && ObjectUtils.cast(painter.getDevice(), WCanvasPaintDevice.class) != null) {
      WRectF clipRect = null;
      WRectF area = this.hv(this.chartArea_);
      if (axis.getLocation() == AxisValue.Zero && location == AxisValue.Zero) {
        clipRect = area;
      } else {
        if (isYAxis != (this.getOrientation() == Orientation.Horizontal)) {
          double h = area.getHeight();
          if (this.xAxes_.get(0).location.finLoc == AxisValue.Zero
                  && this.getOrientation() == Orientation.Vertical
              || this.yAxes_.get(0).location.finLoc == AxisValue.Zero
                  && this.getOrientation() == Orientation.Horizontal) {
            h += 1;
          }
          clipRect = new WRectF(0.0, area.getTop(), isYAxis ? this.width_ : this.height_, h);
        } else {
          clipRect =
              new WRectF(
                  area.getLeft(), 0.0, area.getWidth(), isYAxis ? this.width_ : this.height_);
        }
      }
      if (properties.equals(AxisProperty.Labels)) {
        clipRect =
            new WRectF(
                clipRect.getLeft() - 1,
                clipRect.getTop() - 1,
                clipRect.getWidth() + 2,
                clipRect.getHeight() + 2);
      }
      WPainterPath clipPath = new WPainterPath();
      clipPath.addRect(clipRect);
      painter.save();
      painter.setClipPath(clipPath);
      painter.setClipping(true);
    }
    List<AxisValue> locations = new ArrayList<AxisValue>();
    if (location == AxisValue.Both) {
      locations.add(AxisValue.Minimum);
      locations.add(AxisValue.Maximum);
    } else {
      locations.add(location);
    }
    for (int l = 0; l < locations.size(); ++l) {
      WPointF axisStart = new WPointF();
      WPointF axisEnd = new WPointF();
      double tickStart = 0.0;
      double tickEnd = 0.0;
      double labelPos = 0.0;
      AlignmentFlag labelHFlag = AlignmentFlag.Center;
      AlignmentFlag labelVFlag = AlignmentFlag.Middle;
      if (isYAxis) {
        labelVFlag = AlignmentFlag.Middle;
        axisStart.setY(this.chartArea_.getBottom());
        axisEnd.setY(this.chartArea_.getTop());
      } else {
        labelHFlag = AlignmentFlag.Center;
        axisStart.setX(this.chartArea_.getLeft());
        axisEnd.setX(this.chartArea_.getRight());
      }
      switch (locations.get(l)) {
        case Minimum:
          if (isYAxis) {
            double x =
                this.chartArea_.getLeft() - this.yAxes_.get(axis.getYAxisId()).location.minOffset;
            if (axis.getTickDirection() == TickDirection.Inwards) {
              tickStart = 0;
              tickEnd = TICK_LENGTH;
              labelPos = TICK_LENGTH;
              labelHFlag = AlignmentFlag.Left;
              axisStart.setX(x);
              axisEnd.setX(x);
            } else {
              tickStart = -TICK_LENGTH;
              tickEnd = 0;
              labelPos = -TICK_LENGTH;
              labelHFlag = AlignmentFlag.Right;
              x -= axis.getMargin();
              axisStart.setX(x);
              axisEnd.setX(x);
            }
          } else {
            double y =
                this.chartArea_.getBottom()
                    - 1
                    + this.xAxes_.get(axis.getXAxisId()).location.minOffset;
            if (axis.getTickDirection() == TickDirection.Inwards) {
              tickStart = -TICK_LENGTH;
              tickEnd = 0;
              labelPos = -TICK_LENGTH;
              labelVFlag = AlignmentFlag.Bottom;
              axisStart.setY(y);
              axisEnd.setY(y);
            } else {
              tickStart = 0;
              tickEnd = TICK_LENGTH;
              labelPos = TICK_LENGTH;
              labelVFlag = AlignmentFlag.Top;
              axisStart.setY(y);
              axisEnd.setY(y);
            }
          }
          break;
        case Maximum:
          if (isYAxis) {
            double x =
                this.chartArea_.getRight() + this.yAxes_.get(axis.getYAxisId()).location.maxOffset;
            if (axis.getTickDirection() == TickDirection.Inwards) {
              tickStart = -TICK_LENGTH;
              tickEnd = 0;
              labelPos = -TICK_LENGTH;
              labelHFlag = AlignmentFlag.Right;
              x -= 1;
              axisStart.setX(x);
              axisEnd.setX(x);
            } else {
              tickStart = 0;
              tickEnd = TICK_LENGTH;
              labelPos = TICK_LENGTH;
              labelHFlag = AlignmentFlag.Left;
              x += axis.getMargin();
              axisStart.setX(x);
              axisEnd.setX(x);
            }
          } else {
            double y =
                this.chartArea_.getTop() - this.xAxes_.get(axis.getXAxisId()).location.maxOffset;
            if (axis.getTickDirection() == TickDirection.Inwards) {
              tickStart = 0;
              tickEnd = TICK_LENGTH;
              labelPos = TICK_LENGTH;
              labelVFlag = AlignmentFlag.Top;
              axisStart.setY(y);
              axisEnd.setY(y);
            } else {
              tickStart = -TICK_LENGTH;
              tickEnd = 0;
              labelPos = -TICK_LENGTH;
              labelVFlag = AlignmentFlag.Bottom;
              axisStart.setY(y);
              axisEnd.setY(y);
            }
          }
          break;
        case Zero:
          tickStart = -TICK_LENGTH;
          tickEnd = TICK_LENGTH;
          if (isYAxis) {
            double x = this.chartArea_.getLeft() + this.getAxis(Axis.X).mapToDevice(0.0);
            axisStart.setX(x);
            axisEnd.setX(x);
            labelHFlag = AlignmentFlag.Right;
            if (this.getType() == ChartType.Category) {
              labelPos = this.chartArea_.getLeft() - axisStart.getX() - TICK_LENGTH;
            } else {
              labelPos = -TICK_LENGTH;
            }
          } else {
            double y = this.chartArea_.getBottom() - this.getAxis(Axis.Y).mapToDevice(0.0);
            axisStart.setY(y);
            axisEnd.setY(y);
            labelVFlag = AlignmentFlag.Top;
            if (this.getType() == ChartType.Category) {
              labelPos = this.chartArea_.getBottom() - axisStart.getY() + TICK_LENGTH;
            } else {
              labelPos = TICK_LENGTH;
            }
          }
          break;
        case Both:
          assert false;
          break;
      }
      if (properties.contains(AxisProperty.Labels) && !(axis.getTitle().length() == 0)) {
        if (this.isInteractive()) {
          painter.setClipping(false);
        }
        WFont oldFont2 = painter.getFont();
        WFont titleFont = axis.getTitleFont();
        painter.setFont(titleFont);
        boolean chartVertical = this.getOrientation() == Orientation.Vertical;
        if (isYAxis) {
          double u = axisStart.getX();
          if (chartVertical) {
            if (axis.getTitleOrientation() == Orientation.Horizontal) {
              this.renderLabel(
                  painter,
                  axis.getTitle(),
                  new WPointF(
                      u + (labelHFlag == AlignmentFlag.Right ? 15 : -15),
                      this.chartArea_.getTop() - 8),
                  EnumUtils.or(EnumSet.of(labelHFlag), AlignmentFlag.Bottom),
                  0,
                  10);
            } else {
              WPaintDevice device = painter.getDevice();
              double size = 0;
              double titleSizeW = 0;
              if (device.getFeatures().contains(PaintDeviceFeatureFlag.FontMetrics)) {
                if (axis.getTickDirection() == TickDirection.Outwards) {
                  size = axis.calcMaxTickLabelSize(device, Orientation.Horizontal);
                }
                titleSizeW = axis.calcTitleSize(device, Orientation.Vertical);
                if (axis.getTickDirection() == TickDirection.Inwards) {
                  titleSizeW = -titleSizeW;
                }
              } else {
                size = 35;
                if (axis.getTickDirection() == TickDirection.Inwards) {
                  size = -20;
                }
              }
              this.renderLabel(
                  painter,
                  axis.getTitle(),
                  new WPointF(
                      u
                          + (labelHFlag == AlignmentFlag.Right
                              ? -(size + titleSizeW + 5)
                              : +(size + titleSizeW + 5)),
                      this.chartArea_.getCenter().getY()),
                  EnumUtils.or(EnumSet.of(AlignmentFlag.Center), AlignmentFlag.Middle),
                  locations.get(l) == AxisValue.Maximum ? -90 : 90,
                  10);
            }
          } else {
            double extraMargin = 0;
            WPaintDevice device = painter.getDevice();
            if (axis.getTickDirection() == TickDirection.Outwards) {
              if (device.getFeatures().contains(PaintDeviceFeatureFlag.FontMetrics)) {
                extraMargin = axis.calcMaxTickLabelSize(device, Orientation.Vertical);
              } else {
                extraMargin = 15;
              }
            }
            if (locations.get(l) != AxisValue.Maximum) {
              extraMargin = -extraMargin;
            }
            EnumSet<AlignmentFlag> alignment =
                EnumUtils.or(
                    EnumSet.of(
                        locations.get(l) == AxisValue.Maximum
                            ? AlignmentFlag.Left
                            : AlignmentFlag.Right),
                    AlignmentFlag.Middle);
            this.renderLabel(
                painter,
                axis.getTitle(),
                new WPointF(u + extraMargin, this.chartArea_.getCenter().getY()),
                alignment,
                0,
                10);
          }
        } else {
          double u = axisStart.getY();
          if (chartVertical) {
            double extraMargin = 0;
            WPaintDevice device = painter.getDevice();
            if (device.getFeatures().contains(PaintDeviceFeatureFlag.FontMetrics)) {
              if (axis.getTickDirection() == TickDirection.Outwards) {
                extraMargin = axis.calcMaxTickLabelSize(device, Orientation.Vertical);
              }
            } else {
              if (axis.getTickDirection() == TickDirection.Outwards) {
                extraMargin = 15;
              }
            }
            if (locations.get(l) == AxisValue.Maximum) {
              extraMargin = -extraMargin;
            }
            EnumSet<AlignmentFlag> alignment =
                EnumUtils.or(
                    EnumSet.of(
                        locations.get(l) == AxisValue.Maximum
                            ? AlignmentFlag.Bottom
                            : AlignmentFlag.Top),
                    AlignmentFlag.Center);
            this.renderLabel(
                painter,
                axis.getTitle(),
                new WPointF(this.chartArea_.getCenter().getX(), u + extraMargin),
                alignment,
                0,
                10);
          } else {
            if (axis.getTitleOrientation() == Orientation.Vertical) {
              WPaintDevice device = painter.getDevice();
              double extraMargin = 0;
              if (device.getFeatures().contains(PaintDeviceFeatureFlag.FontMetrics)) {
                if (axis.getTickDirection() == TickDirection.Outwards) {
                  extraMargin = axis.calcMaxTickLabelSize(device, Orientation.Horizontal);
                }
                extraMargin += axis.calcTitleSize(device, Orientation.Vertical);
              } else {
                extraMargin = 40;
              }
              if (locations.get(l) == AxisValue.Maximum) {
                extraMargin = -extraMargin;
              }
              this.renderLabel(
                  painter,
                  axis.getTitle(),
                  new WPointF(this.chartArea_.getCenter().getX(), u + extraMargin),
                  EnumUtils.or(EnumSet.of(AlignmentFlag.Middle), AlignmentFlag.Center),
                  locations.get(l) == AxisValue.Maximum ? -90 : 90,
                  10);
            } else {
              EnumSet<AlignmentFlag> alignment =
                  EnumUtils.or(
                      EnumSet.of(
                          locations.get(l) == AxisValue.Maximum
                              ? AlignmentFlag.Bottom
                              : AlignmentFlag.Top),
                      AlignmentFlag.Left);
              this.renderLabel(
                  painter,
                  axis.getTitle(),
                  new WPointF(this.chartArea_.getRight(), u),
                  alignment,
                  0,
                  8);
            }
          }
        }
        painter.setFont(oldFont2);
        if (this.isInteractive()) {
          painter.setClipping(true);
        }
      }
      final double ANGLE1 = 15;
      final double ANGLE2 = 80;
      if (isYAxis) {
        if (axis.getLabelAngle() > ANGLE1) {
          labelVFlag = labelPos < 0 ? AlignmentFlag.Bottom : AlignmentFlag.Top;
          if (axis.getLabelAngle() > ANGLE2) {
            labelHFlag = AlignmentFlag.Center;
          }
        } else {
          if (axis.getLabelAngle() < -ANGLE1) {
            labelVFlag = labelPos < 0 ? AlignmentFlag.Top : AlignmentFlag.Bottom;
            if (axis.getLabelAngle() < -ANGLE2) {
              labelHFlag = AlignmentFlag.Center;
            }
          }
        }
      } else {
        if (axis.getLabelAngle() > ANGLE1) {
          labelHFlag = labelPos > 0 ? AlignmentFlag.Right : AlignmentFlag.Left;
          if (axis.getLabelAngle() > ANGLE2) {
            labelVFlag = AlignmentFlag.Middle;
          }
        } else {
          if (axis.getLabelAngle() < -ANGLE1) {
            labelHFlag = labelPos > 0 ? AlignmentFlag.Left : AlignmentFlag.Right;
            if (axis.getLabelAngle() < -ANGLE2) {
              labelVFlag = AlignmentFlag.Middle;
            }
          }
        }
      }
      if (this.getOrientation() == Orientation.Horizontal) {
        axisStart = this.hv(axisStart);
        axisEnd = this.hv(axisEnd);
        AlignmentFlag rHFlag = AlignmentFlag.Center;
        AlignmentFlag rVFlag = AlignmentFlag.Middle;
        switch (labelHFlag) {
          case Left:
            rVFlag = AlignmentFlag.Top;
            break;
          case Center:
            rVFlag = AlignmentFlag.Middle;
            break;
          case Right:
            rVFlag = AlignmentFlag.Bottom;
            break;
          default:
            break;
        }
        switch (labelVFlag) {
          case Top:
            rHFlag = AlignmentFlag.Right;
            break;
          case Middle:
            rHFlag = AlignmentFlag.Center;
            break;
          case Bottom:
            rHFlag = AlignmentFlag.Left;
            break;
          default:
            break;
        }
        labelHFlag = rHFlag;
        labelVFlag = rVFlag;
        boolean invertTicks = !isYAxis;
        if (invertTicks) {
          tickStart = -tickStart;
          tickEnd = -tickEnd;
          labelPos = -labelPos;
        }
      }
      List<WPen> pens = new ArrayList<WPen>();
      List<WPen> textPens = new ArrayList<WPen>();
      if (this.isInteractive()) {
        final List<WCartesianChart.PenAssignment> assignment =
            axis.getId() == Axis.X
                ? this.xAxes_.get(axis.getXAxisId()).pens
                : this.yAxes_.get(axis.getYAxisId()).pens;
        if (!assignment.isEmpty()) {
          for (int i = 0; i < assignment.size(); ++i) {
            pens.add(assignment.get(i).pen.getValue());
            textPens.add(assignment.get(i).textPen.getValue());
          }
        }
      }
      WTransform transform = new WTransform();
      WRectF area = this.hv(this.chartArea_);
      if (axis.getLocation() == AxisValue.Zero) {
        transform.assign(
            new WTransform(1, 0, 0, -1, area.getLeft(), area.getBottom())
                .multiply(this.xAxes_.get(axis.getXAxisId()).transform)
                .multiply(this.yAxes_.get(axis.getYAxisId()).transform)
                .multiply(new WTransform(1, 0, 0, -1, -area.getLeft(), area.getBottom())));
      } else {
        if (isYAxis && this.getOrientation() == Orientation.Vertical) {
          transform.assign(
              new WTransform(1, 0, 0, -1, 0, area.getBottom())
                  .multiply(this.yAxes_.get(axis.getYAxisId()).transform)
                  .multiply(new WTransform(1, 0, 0, -1, 0, area.getBottom())));
        } else {
          if (isYAxis && this.getOrientation() == Orientation.Horizontal) {
            transform.assign(
                new WTransform(0, 1, 1, 0, area.getLeft(), 0)
                    .multiply(this.yAxes_.get(axis.getYAxisId()).transform)
                    .multiply(new WTransform(0, 1, 1, 0, 0, -area.getLeft())));
          } else {
            if (this.getOrientation() == Orientation.Horizontal) {
              transform.assign(
                  new WTransform(0, 1, 1, 0, 0, area.getTop())
                      .multiply(this.xAxes_.get(axis.getXAxisId()).transform)
                      .multiply(new WTransform(0, 1, 1, 0, -area.getTop(), 0)));
            } else {
              transform.assign(
                  new WTransform(1, 0, 0, 1, area.getLeft(), 0)
                      .multiply(this.xAxes_.get(axis.getXAxisId()).transform)
                      .multiply(new WTransform(1, 0, 0, 1, -area.getLeft(), 0)));
            }
          }
        }
      }
      AxisValue side = location == AxisValue.Both ? locations.get(l) : axis.getLocation();
      axis.render(
          painter,
          properties,
          axisStart,
          axisEnd,
          tickStart,
          tickEnd,
          labelPos,
          EnumUtils.or(EnumSet.of(labelHFlag), labelVFlag),
          transform,
          side,
          pens,
          textPens);
    }
    if (this.isInteractive()) {
      painter.restore();
    }
  }
  /**
   * Renders properties of one axis.
   *
   * <p>Calls {@link #renderAxis(WPainter painter, WAxis axis, EnumSet properties)
   * renderAxis(painter, axis, EnumSet.of(propertie, properties))}
   */
  protected final void renderAxis(
      final WPainter painter,
      final WAxis axis,
      AxisProperty propertie,
      AxisProperty... properties) {
    renderAxis(painter, axis, EnumSet.of(propertie, properties));
  }
  /**
   * Renders grid lines along the ticks of the given axis.
   *
   * <p>
   *
   * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
   */
  protected void renderGrid(final WPainter painter, final WAxis ax) {
    if (!ax.isGridLinesEnabled()) {
      return;
    }
    final boolean isXAxis = ax.getId() == Axis.X;
    final boolean isYAxis = ax.getId() != Axis.X;
    if (!isXAxis && this.xAxes_.isEmpty()) {
      return;
    }
    if (!isYAxis && this.yAxes_.isEmpty()) {
      return;
    }
    final WAxis other = isYAxis ? this.getAxis(Axis.X) : this.getAxis(Axis.Y1);
    final WAxis.Segment s0 = other.segments_.get(0);
    final WAxis.Segment sn = other.segments_.get(other.segments_.size() - 1);
    double ou0 = s0.renderStart;
    double oun = sn.renderStart + sn.renderLength;
    if (!isYAxis) {
      boolean gridLines = this.getAxis(Axis.Y1).isGridLinesEnabled();
      for (int i = 1; i < this.getYAxisCount(); ++i) {
        if (!(this.getYAxis(i).isVisible() && this.getYAxis(i).isGridLinesEnabled())) {
          continue;
        }
        final WAxis.Segment s0_2 = this.getYAxis(i).segments_.get(0);
        final WAxis.Segment sn_2 = this.getYAxis(i).segments_.get(0);
        if (!gridLines || s0_2.renderStart < ou0) {
          ou0 = s0_2.renderStart;
        }
        if (!gridLines || sn_2.renderStart + sn_2.renderLength > oun) {
          oun = sn_2.renderStart + sn_2.renderLength;
        }
        gridLines = true;
      }
    }
    if (!isYAxis) {
      ou0 = this.chartArea_.getBottom() - ou0;
      oun = this.chartArea_.getBottom() - oun;
    } else {
      ou0 = this.chartArea_.getLeft() + ou0;
      oun = this.chartArea_.getLeft() + oun;
    }
    if (this.isInteractive()) {
      painter.save();
      WPainterPath clipPath = new WPainterPath();
      clipPath.addRect(this.hv(this.chartArea_));
      painter.setClipPath(clipPath);
      painter.setClipping(true);
    }
    List<WPen> pens = new ArrayList<WPen>();
    final List<WCartesianChart.PenAssignment> assignments =
        ax.getId() == Axis.X
            ? this.xAxes_.get(ax.getXAxisId()).pens
            : this.yAxes_.get(ax.getYAxisId()).pens;
    if (assignments.isEmpty()) {
      pens.add(ax.getGridLinesPen());
    } else {
      for (int i = 0; i < assignments.size(); ++i) {
        pens.add(assignments.get(i).gridPen.getValue());
      }
    }
    AxisConfig axisConfig = new AxisConfig();
    if (ax.getLocation() == AxisValue.Both) {
      axisConfig.side = AxisValue.Minimum;
    } else {
      axisConfig.side = ax.getLocation();
    }
    for (int level = 1; level <= pens.size(); ++level) {
      WPainterPath gridPath = new WPainterPath();
      axisConfig.zoomLevel = level;
      List<Double> gridPos = ax.gridLinePositions(axisConfig);
      for (int i = 0; i < gridPos.size(); ++i) {
        double u = gridPos.get(i);
        if (isYAxis) {
          u = this.chartArea_.getBottom() - u;
          gridPath.moveTo(this.hv(ou0, u));
          gridPath.lineTo(this.hv(oun, u));
        } else {
          u = this.chartArea_.getLeft() + u;
          gridPath.moveTo(this.hv(u, ou0));
          gridPath.lineTo(this.hv(u, oun));
        }
      }
      painter.strokePath(
          this.zoomRangeTransform(isYAxis ? ax.getYAxisId() : 0).map(gridPath).getCrisp(),
          pens.get(level - 1));
    }
    if (this.isInteractive()) {
      painter.restore();
    }
  }
  /**
   * Renders other, user-defined things.
   *
   * <p>The default implementation sets the painter&apos;s {@link WPainter#setClipPath(WPainterPath
   * clipPath) clip path} to the chart area, but does not enable clipping.
   *
   * <p>This method can be overridden to draw extra content onto the chart.
   *
   * <p>Chart coordinates can be mapped to device coordinates with {@link
   * WCartesianChart#mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis,
   * int xSegment, int ySegment) mapToDeviceWithoutTransform()}. If these need to move and scale
   * along with the zoom range, those points can be transformed with {@link
   * WCartesianChart#zoomRangeTransform(int yAxis) zoomRangeTransform()}.
   *
   * <p>This method is called last by default. If you want to render other things at some other
   * moment, you can override render(WPainter&amp;, const WRectF&amp;).
   */
  protected void renderOther(final WPainter painter) {
    WPainterPath clipPath = new WPainterPath();
    clipPath.addRect(this.hv(this.chartArea_));
    painter.setClipPath(clipPath);
  }
  /** Calculates the total number of bar groups. */
  protected int getCalcNumBarGroups() {
    int numBarGroups = 0;
    boolean newGroup = true;
    for (int i = 0; i < this.series_.size(); ++i) {
      if (this.series_.get(i).getType() == SeriesType.Bar) {
        if (newGroup || !this.series_.get(i).isStacked()) {
          ++numBarGroups;
        }
        newGroup = false;
      } else {
        newGroup = true;
      }
    }
    return numBarGroups;
  }

  protected void render(EnumSet<RenderFlag> flags) {
    super.render(flags);
    if (flags.contains(RenderFlag.Full) || !this.jsDefined_) {
      this.defineJavaScript();
    }
  }

  protected void setFormData(final WObject.FormData formData) {
    super.setFormData(formData);
    List<WTransform> xTransforms = new ArrayList<WTransform>();
    for (int i = 0; i < this.xAxes_.size(); ++i) {
      xTransforms.add(this.xAxes_.get(i).transformHandle.getValue());
    }
    List<WTransform> yTransforms = new ArrayList<WTransform>();
    for (int i = 0; i < this.yAxes_.size(); ++i) {
      yTransforms.add(this.yAxes_.get(i).transformHandle.getValue());
    }
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      if (!this.getXAxis(i).zoomRangeDirty_) {
        WPointF devicePan =
            new WPointF(xTransforms.get(i).getDx() / xTransforms.get(i).getM11(), 0.0);
        WPointF modelPan = new WPointF(this.getXAxis(i).mapFromDevice(-devicePan.getX()), 0.0);
        if (xTransforms.get(i).isIdentity()) {
          this.getXAxis(i).setZoomRangeFromClient(WAxis.AUTO_MINIMUM, WAxis.AUTO_MAXIMUM);
        } else {
          double z = xTransforms.get(i).getM11();
          double x = modelPan.getX();
          double min = this.getXAxis(i).mapFromDevice(0.0);
          double max = this.getXAxis(i).mapFromDevice(this.getXAxis(i).fullRenderLength_);
          double x2 = x + (max - min) / z;
          this.getXAxis(i).setZoomRangeFromClient(x, x2);
        }
      }
    }
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      if (!this.getYAxis(i).zoomRangeDirty_) {
        WPointF devicePan =
            new WPointF(0.0, yTransforms.get(i).getDy() / yTransforms.get(i).getM22());
        WPointF modelPan = new WPointF(0.0, this.getYAxis(i).mapFromDevice(-devicePan.getY()));
        if (yTransforms.get(i).isIdentity()) {
          this.getYAxis(i).setZoomRangeFromClient(WAxis.AUTO_MINIMUM, WAxis.AUTO_MAXIMUM);
        } else {
          double z = yTransforms.get(i).getM22();
          double y = modelPan.getY();
          double min = this.getYAxis(i).mapFromDevice(0.0);
          double max = this.getYAxis(i).mapFromDevice(this.getYAxis(i).fullRenderLength_);
          double y2 = y + (max - min) / z;
          this.getYAxis(i).setZoomRangeFromClient(y, y2);
        }
      }
    }
    if (this.curveTransforms_.size() != 0) {
      for (int i = 0; i < this.series_.size(); ++i) {
        final WDataSeries s = this.series_.get(i);
        int xAxis = s.getXAxis();
        int yAxis = s.getYAxis();
        if (xAxis >= 0
            && xAxis < this.getXAxisCount()
            && yAxis >= 0
            && yAxis < this.getYAxisCount()) {
          if ((s.getType() == SeriesType.Line || s.getType() == SeriesType.Curve)
              && !s.isHidden()) {
            if (!s.scaleDirty_) {
              s.scale_ = this.curveTransforms_.get(s).getValue().getM22();
            }
            if (!s.offsetDirty_) {
              double origin;
              if (this.getOrientation() == Orientation.Horizontal) {
                origin =
                    this.mapToDeviceWithoutTransform(
                            0.0, 0.0, this.getXAxis(xAxis), this.getYAxis(yAxis))
                        .getX();
              } else {
                origin =
                    this.mapToDeviceWithoutTransform(
                            0.0, 0.0, this.getXAxis(xAxis), this.getYAxis(yAxis))
                        .getY();
              }
              double dy = this.curveTransforms_.get(s).getValue().getDy();
              double scale = this.curveTransforms_.get(s).getValue().getM22();
              double offset = -dy + origin * (1 - scale) + this.getYAxis(yAxis).mapToDevice(0.0, 0);
              if (this.getOrientation() == Orientation.Horizontal) {
                s.offset_ = -this.getYAxis(yAxis).mapFromDevice(offset);
              } else {
                s.offset_ = this.getYAxis(yAxis).mapFromDevice(offset);
              }
            }
          }
        }
      }
    }
  }
  /**
   * Returns the current zoom range transform.
   *
   * <p>This transform maps device coordinates from the fully zoomed out position to the current
   * zoom range.
   *
   * <p>This transform is a {@link WJavaScriptExposableObject#isJavaScriptBound() JavaScript bound}
   * transform if this chart is interactive. Otherwise, this transform is just the identity
   * transform.
   *
   * <p>
   *
   * @see WCartesianChart#setZoomEnabled(boolean zoomEnabled)
   * @see WCartesianChart#setPanEnabled(boolean panEnabled)
   * @see WAxis#setZoomRange(double minimum, double maximum)
   */
  protected WTransform zoomRangeTransform(int yAxis) {
    return this.zoomRangeTransform(this.getXAxis(0), this.getYAxis(yAxis));
  }
  /**
   * Returns the current zoom range transform.
   *
   * <p>Returns {@link #zoomRangeTransform(int yAxis) zoomRangeTransform(0)}
   */
  protected final WTransform zoomRangeTransform() {
    return zoomRangeTransform(0);
  }
  /**
   * Returns the current zoom range transform.
   *
   * <p>This transform maps device coordinates from the fully zoomed out position to the current
   * zoom range.
   *
   * <p>This transform is a {@link WJavaScriptExposableObject#isJavaScriptBound() JavaScript bound}
   * transform if this chart is interactive. Otherwise, this transform is just the identity
   * transform.
   *
   * <p>
   *
   * @see WCartesianChart#setZoomEnabled(boolean zoomEnabled)
   * @see WCartesianChart#setPanEnabled(boolean panEnabled)
   * @see WAxis#setZoomRange(double minimum, double maximum)
   */
  protected WTransform zoomRangeTransform(final WAxis xAxis, final WAxis yAxis) {
    return this.zoomRangeTransform(
        this.xAxes_.get(xAxis.xAxis_).transform, this.yAxes_.get(yAxis.yAxis_).transform);
  }

  private List<WAxis> collectAxesAtLocation(Axis ax, AxisValue side) {
    final List<WCartesianChart.AxisStruct> axes = ax == Axis.X ? this.xAxes_ : this.yAxes_;
    final List<WCartesianChart.AxisStruct> otherAxes = ax == Axis.X ? this.yAxes_ : this.xAxes_;
    List<WAxis> result = new ArrayList<WAxis>();
    for (int i = 0; i < axes.size(); ++i) {
      final WAxis axis = axes.get(i).axis;
      if (!axis.isVisible()) {
        continue;
      }
      if (axis.getLocation() == AxisValue.Zero) {
        if (side == AxisValue.Minimum) {
          if (axis.getScale() == AxisScale.Discrete
              || otherAxes.get(0).axis.segments_.get(0).renderMinimum >= 0
              || !otherAxes.get(0).axis.isOnAxis(0.0)
                  && otherAxes
                          .get(0)
                          .axis
                          .segments_
                          .get(otherAxes.get(0).axis.segments_.size() - 1)
                          .renderMaximum
                      > 0) {
            result.add(axis);
          }
        } else {
          if (side == AxisValue.Maximum) {
            if (otherAxes
                    .get(0)
                    .axis
                    .segments_
                    .get(otherAxes.get(0).axis.segments_.size() - 1)
                    .renderMaximum
                <= 0) {
              result.add(axis);
            }
          }
        }
      }
    }
    for (int i = 0; i < axes.size(); ++i) {
      final WAxis axis = axes.get(i).axis;
      if (!axis.isVisible()) {
        continue;
      }
      if (axis.getLocation() == side || axis.getLocation() == AxisValue.Both) {
        result.add(axis);
      }
    }
    return result;
  }

  private int getSeriesIndexOf(int modelColumn) {
    for (int i = 0; i < this.series_.size(); ++i) {
      if (this.series_.get(i).getModelColumn() == modelColumn) {
        return i;
      }
    }
    return -1;
  }

  int getSeriesIndexOf(final WDataSeries series) {
    for (int i = 0; i < this.series_.size(); ++i) {
      if (this.series_.get(i) == series) {
        return i;
      }
    }
    return -1;
  }

  private boolean hasInwardsXAxisOnMinimumSide() {
    List<WAxis> minimumXaxes = this.collectAxesAtLocation(Axis.X, AxisValue.Minimum);
    return !minimumXaxes.isEmpty()
        && minimumXaxes.get(0).getTickDirection() == TickDirection.Inwards;
  }

  private boolean hasInwardsYAxisOnMaximumSide() {
    List<WAxis> maximumYaxes = this.collectAxesAtLocation(Axis.Y, AxisValue.Maximum);
    return !maximumYaxes.isEmpty()
        && maximumYaxes.get(0).getTickDirection() == TickDirection.Inwards;
  }

  private void clearPens() {
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      this.clearPensForAxis(Axis.X, i);
    }
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      this.clearPensForAxis(Axis.Y, i);
    }
  }

  private void clearPensForAxis(Axis ax, int axisId) {
    final List<WCartesianChart.PenAssignment> assignments =
        ax == Axis.X ? this.xAxes_.get(axisId).pens : this.yAxes_.get(axisId).pens;
    for (int i = 0; i < assignments.size(); ++i) {
      final WCartesianChart.PenAssignment assignment = assignments.get(i);
      this.freePens_.add(assignment.pen);
      this.freePens_.add(assignment.textPen);
      this.freePens_.add(assignment.gridPen);
    }
    assignments.clear();
  }

  private void createPensForAxis(Axis ax, int axisId) {
    final WAxis axis = ax == Axis.X ? this.getXAxis(axisId) : this.getYAxis(axisId);
    if (!axis.isVisible() || axis.getScale() == AxisScale.Log) {
      return;
    }
    final WCartesianChart.AxisStruct axisStruct =
        ax == Axis.X ? this.xAxes_.get(axisId) : this.yAxes_.get(axisId);
    double zoom = axis.getZoom();
    if (zoom > axis.getMaxZoom()) {
      zoom = axis.getMaxZoom();
    }
    int level = toZoomLevel(zoom);
    List<WCartesianChart.PenAssignment> assignments =
        new ArrayList<WCartesianChart.PenAssignment>();
    boolean stop = false;
    for (int i = 1; !stop; ++i) {
      if (this.isOnDemandLoadingEnabled() && i > level + 1) {
        break;
      }
      double z = Math.pow(2.0, i - 1);
      stop = z >= axis.getMaxZoom();
      WJavaScriptHandle<WPen> pen = null;
      if (this.freePens_.size() > 0) {
        pen = this.freePens_.get(this.freePens_.size() - 1);
        this.freePens_.remove(this.freePens_.size() - 1);
      } else {
        pen = this.createJSPen();
      }
      WPen p = axis.getPen().clone();
      p.setColor(
          new WColor(
              p.getColor().getRed(),
              p.getColor().getGreen(),
              p.getColor().getBlue(),
              i == level ? p.getColor().getAlpha() : 0));
      pen.setValue(p);
      WJavaScriptHandle<WPen> textPen = null;
      if (this.freePens_.size() > 0) {
        textPen = this.freePens_.get(this.freePens_.size() - 1);
        this.freePens_.remove(this.freePens_.size() - 1);
      } else {
        textPen = this.createJSPen();
      }
      p = axis.getTextPen().clone();
      p.setColor(
          new WColor(
              p.getColor().getRed(),
              p.getColor().getGreen(),
              p.getColor().getBlue(),
              i == level ? p.getColor().getAlpha() : 0));
      textPen.setValue(p);
      WJavaScriptHandle<WPen> gridPen = null;
      if (this.freePens_.size() > 0) {
        gridPen = this.freePens_.get(this.freePens_.size() - 1);
        this.freePens_.remove(this.freePens_.size() - 1);
      } else {
        gridPen = this.createJSPen();
      }
      p = axis.getGridLinesPen().clone();
      p.setColor(
          new WColor(
              p.getColor().getRed(),
              p.getColor().getGreen(),
              p.getColor().getBlue(),
              i == level ? p.getColor().getAlpha() : 0));
      gridPen.setValue(p);
      assignments.add(new WCartesianChart.PenAssignment(pen, textPen, gridPen));
    }
    Utils.copyList(assignments, axisStruct.pens);
  }

  String getCObjJsRef() {
    return this.getJsRef() + ".wtCObj";
  }

  private void assignJSHandlesForAllSeries() {
    if (!this.isInteractive()) {
      return;
    }
    for (int i = 0; i < this.series_.size(); ++i) {
      final WDataSeries s = this.series_.get(i);
      if (s.getType() == SeriesType.Line || s.getType() == SeriesType.Curve) {
        this.assignJSPathsForSeries(s);
        this.assignJSTransformsForSeries(s);
      }
    }
  }

  private void freeJSHandlesForAllSeries() {
    this.freeAllJSPaths();
    this.freeAllJSTransforms();
  }

  private void assignJSPathsForSeries(final WDataSeries series) {
    WJavaScriptHandle<WPainterPath> handle = null;
    if (this.freePainterPaths_.size() > 0) {
      handle = this.freePainterPaths_.get(this.freePainterPaths_.size() - 1);
      this.freePainterPaths_.remove(this.freePainterPaths_.size() - 1);
    } else {
      handle = this.createJSPainterPath();
    }
    this.curvePaths_.put(series, handle);
  }

  private void freeJSPathsForSeries(final WDataSeries series) {
    this.freePainterPaths_.add(this.curvePaths_.get(series));
    this.curvePaths_.remove(series);
  }

  private void freeAllJSPaths() {
    for (Iterator<Map.Entry<WDataSeries, WJavaScriptHandle<WPainterPath>>> it_it =
            this.curvePaths_.entrySet().iterator();
        it_it.hasNext(); ) {
      Map.Entry<WDataSeries, WJavaScriptHandle<WPainterPath>> it = it_it.next();
      this.freePainterPaths_.add(it.getValue());
    }
    this.curvePaths_.clear();
  }

  private void assignJSTransformsForSeries(final WDataSeries series) {
    WJavaScriptHandle<WTransform> handle = null;
    if (this.freeTransforms_.size() > 0) {
      handle = this.freeTransforms_.get(this.freeTransforms_.size() - 1);
      this.freeTransforms_.remove(this.freeTransforms_.size() - 1);
    } else {
      handle = this.createJSTransform();
    }
    this.curveTransforms_.put(series, handle);
  }

  private void freeJSTransformsForSeries(final WDataSeries series) {
    this.freeTransforms_.add(this.curveTransforms_.get(series));
    this.curveTransforms_.remove(series);
  }

  private void freeAllJSTransforms() {
    for (Iterator<Map.Entry<WDataSeries, WJavaScriptHandle<WTransform>>> it_it =
            this.curveTransforms_.entrySet().iterator();
        it_it.hasNext(); ) {
      Map.Entry<WDataSeries, WJavaScriptHandle<WTransform>> it = it_it.next();
      this.freeTransforms_.add(it.getValue());
    }
    this.curveTransforms_.clear();
  }

  private void updateJSPens(final StringBuilder js) {
    js.append("pens:{x:[");
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      if (i != 0) {
        js.append(',');
      }
      this.updateJSPensForAxis(js, Axis.X, i);
    }
    js.append("],y:[");
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      if (i != 0) {
        js.append(',');
      }
      this.updateJSPensForAxis(js, Axis.Y, i);
    }
    js.append("]},");
    js.append("penAlpha:{x:[");
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      if (i != 0) {
        js.append(',');
      }
      js.append('[').append(this.getXAxis(i).getPen().getColor().getAlpha()).append(',');
      js.append(this.getXAxis(i).getTextPen().getColor().getAlpha()).append(',');
      js.append(this.getXAxis(i).getGridLinesPen().getColor().getAlpha()).append(']');
    }
    js.append("],y:[");
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      if (i != 0) {
        js.append(',');
      }
      js.append('[').append(this.getYAxis(i).getPen().getColor().getAlpha()).append(',');
      js.append(this.getYAxis(i).getTextPen().getColor().getAlpha()).append(',');
      js.append(this.getYAxis(i).getGridLinesPen().getColor().getAlpha()).append(']');
    }
    js.append("]},");
  }

  private void updateJSPensForAxis(final StringBuilder js, Axis axis, int axisId) {
    final List<WCartesianChart.PenAssignment> pens =
        axis == Axis.X ? this.xAxes_.get(axisId).pens : this.yAxes_.get(axisId).pens;
    js.append("[");
    for (int i = 0; i < pens.size(); ++i) {
      if (i != 0) {
        js.append(",");
      }
      final WCartesianChart.PenAssignment assignment = pens.get(i);
      js.append("[");
      js.append(assignment.pen.getJsRef());
      js.append(",");
      js.append(assignment.textPen.getJsRef());
      js.append(",");
      js.append(assignment.gridPen.getJsRef());
      js.append("]");
    }
    js.append("]");
  }

  private void updateJSConfig(final String key, Object value) {
    if (this.getMethod() == RenderMethod.HtmlCanvas) {
      if (!this.cObjCreated_) {
        this.update();
      } else {
        this.doJavaScript(
            this.getCObjJsRef()
                + ".updateConfig({"
                + key
                + ":"
                + StringUtils.asString(value).toString()
                + "});");
      }
    }
  }

  WPainterPath pathForSeries(final WDataSeries series) {
    WJavaScriptHandle<WPainterPath> it = this.curvePaths_.get(series);
    if (it == null) {
      return new WPainterPath();
    } else {
      return it.getValue();
    }
  }

  private WTransform zoomRangeTransform(final WTransform xTransform, final WTransform yTransform) {
    if (this.getOrientation() == Orientation.Vertical) {
      return new WTransform(1, 0, 0, -1, this.chartArea_.getLeft(), this.chartArea_.getBottom())
          .multiply(xTransform)
          .multiply(yTransform)
          .multiply(
              new WTransform(1, 0, 0, -1, -this.chartArea_.getLeft(), this.chartArea_.getBottom()));
    } else {
      WRectF area = this.hv(this.chartArea_);
      return new WTransform(0, 1, 1, 0, area.getLeft(), area.getTop())
          .multiply(xTransform)
          .multiply(yTransform)
          .multiply(new WTransform(0, 1, 1, 0, -area.getTop(), -area.getLeft()));
    }
  }

  WTransform calculateCurveTransform(final WDataSeries series) {
    final WAxis xAxis = this.getXAxis(series.getXAxis());
    final WAxis yAxis = this.getYAxis(series.getYAxis());
    double origin;
    if (this.getOrientation() == Orientation.Horizontal) {
      origin = this.mapToDeviceWithoutTransform(0.0, 0.0, xAxis, yAxis).getX();
    } else {
      origin = this.mapToDeviceWithoutTransform(0.0, 0.0, xAxis, yAxis).getY();
    }
    double offset = yAxis.mapToDevice(0.0, 0) - yAxis.mapToDevice(series.getOffset(), 0);
    if (this.getOrientation() == Orientation.Horizontal) {
      offset = -offset;
    }
    return new WTransform(1, 0, 0, series.getScale(), 0, origin * (1 - series.getScale()) + offset);
  }

  WTransform curveTransform(final WDataSeries series) {
    WJavaScriptHandle<WTransform> it = this.curveTransforms_.get(series);
    WTransform t = new WTransform();
    if (it == null) {
      t.assign(this.calculateCurveTransform(series));
    } else {
      t.assign(it.getValue());
    }
    if (this.getOrientation() == Orientation.Vertical) {
      return t;
    } else {
      return new WTransform(0, 1, 1, 0, 0, 0)
          .multiply(t)
          .multiply(new WTransform(0, 1, 1, 0, 0, 0));
    }
  }

  private void setZoomAndPan() {
    List<WTransform> xTransforms = new ArrayList<WTransform>();
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      if (this.getXAxis(i).zoomMin_ != WAxis.AUTO_MINIMUM
          || this.getXAxis(i).zoomMax_ != WAxis.AUTO_MAXIMUM) {
        double xPan = -this.getXAxis(i).mapToDevice(this.getXAxis(i).getPan(), 0);
        double xZoom = this.getXAxis(i).getZoom();
        if (xZoom > this.getXAxis(i).getMaxZoom()) {
          xZoom = this.getXAxis(i).getMaxZoom();
        }
        if (xZoom < this.getXAxis(i).getMinZoom()) {
          xZoom = this.getXAxis(i).getMinZoom();
        }
        xTransforms.add(new WTransform(xZoom, 0, 0, 1, xZoom * xPan, 0));
      } else {
        double xZoom = this.getXAxis(i).getMinZoom();
        xTransforms.add(new WTransform(xZoom, 0, 0, 1, 0, 0));
      }
    }
    List<WTransform> yTransforms = new ArrayList<WTransform>();
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      if (this.getYAxis(i).zoomMin_ != WAxis.AUTO_MINIMUM
          || this.getYAxis(i).zoomMax_ != WAxis.AUTO_MAXIMUM) {
        double yPan = -this.getYAxis(i).mapToDevice(this.getYAxis(i).getPan(), 0);
        double yZoom = this.getYAxis(i).getZoom();
        if (yZoom > this.getYAxis(i).getMaxZoom()) {
          yZoom = this.getYAxis(i).getMaxZoom();
        }
        if (yZoom < this.getYAxis(i).getMinZoom()) {
          yZoom = this.getYAxis(i).getMinZoom();
        }
        yTransforms.add(new WTransform(1, 0, 0, yZoom, 0, yZoom * yPan));
      } else {
        double yZoom = this.getYAxis(i).getMinZoom();
        yTransforms.add(new WTransform(1, 0, 0, yZoom, 0, 0));
      }
    }
    WRectF chartArea = this.hv(this.getInsideChartArea());
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      WRectF transformedArea =
          this.zoomRangeTransform(xTransforms.get(i), new WTransform()).map(chartArea);
      if (this.getOrientation() == Orientation.Vertical) {
        if (transformedArea.getLeft() > chartArea.getLeft()) {
          double diff = chartArea.getLeft() - transformedArea.getLeft();
          xTransforms.set(i, new WTransform(1, 0, 0, 1, diff, 0).multiply(xTransforms.get(i)));
        } else {
          if (transformedArea.getRight() < chartArea.getRight()) {
            double diff = chartArea.getRight() - transformedArea.getRight();
            xTransforms.set(i, new WTransform(1, 0, 0, 1, diff, 0).multiply(xTransforms.get(i)));
          }
        }
      } else {
        if (transformedArea.getTop() > chartArea.getTop()) {
          double diff = chartArea.getTop() - transformedArea.getTop();
          xTransforms.set(i, new WTransform(1, 0, 0, 1, diff, 0).multiply(xTransforms.get(i)));
        } else {
          if (transformedArea.getBottom() < chartArea.getBottom()) {
            double diff = chartArea.getBottom() - transformedArea.getBottom();
            xTransforms.set(i, new WTransform(1, 0, 0, 1, diff, 0).multiply(xTransforms.get(i)));
          }
        }
      }
    }
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      WRectF transformedArea =
          this.zoomRangeTransform(new WTransform(), yTransforms.get(i)).map(chartArea);
      if (this.getOrientation() == Orientation.Vertical) {
        if (transformedArea.getTop() > chartArea.getTop()) {
          double diff = chartArea.getTop() - transformedArea.getTop();
          yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, -diff).multiply(yTransforms.get(i)));
        } else {
          if (transformedArea.getBottom() < chartArea.getBottom()) {
            double diff = chartArea.getBottom() - transformedArea.getBottom();
            yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, -diff).multiply(yTransforms.get(i)));
          }
        }
      } else {
        if (transformedArea.getLeft() > chartArea.getLeft()) {
          double diff = chartArea.getLeft() - transformedArea.getLeft();
          yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, diff).multiply(yTransforms.get(i)));
        } else {
          if (transformedArea.getRight() < chartArea.getRight()) {
            double diff = chartArea.getRight() - transformedArea.getRight();
            yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, diff).multiply(yTransforms.get(i)));
          }
        }
      }
    }
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      this.xAxes_.get(i).transformHandle.setValue(xTransforms.get(i));
    }
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      this.yAxes_.get(i).transformHandle.setValue(yTransforms.get(i));
    }
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      this.getXAxis(i).zoomRangeDirty_ = false;
    }
    for (int i = 0; i < this.getYAxisCount(); ++i) {
      this.getYAxis(i).zoomRangeDirty_ = false;
    }
  }

  private void addAreaMask() {
    WRectF all = this.hv(new WRectF(0, 0, this.width_, this.height_));
    WRectF chart = this.hv(this.chartArea_);
    List<WRectF> rects = new ArrayList<WRectF>();
    rects.add(new WRectF(all.getTopLeft(), new WPointF(all.getRight(), chart.getTop())));
    rects.add(new WRectF(new WPointF(all.getLeft(), chart.getBottom()), all.getBottomRight()));
    rects.add(new WRectF(new WPointF(all.getLeft(), chart.getTop()), chart.getBottomLeft()));
    rects.add(new WRectF(chart.getTopRight(), new WPointF(all.getRight(), chart.getBottom())));
    for (int i = 0; i < rects.size(); ++i) {
      if (rects.get(i).getHeight() > 0 && rects.get(i).getWidth() > 0) {
        WRectArea rect = new WRectArea(rects.get(i));
        rect.setHole(true);
        rect.setTransformable(false);
        this.addArea(rect);
      }
    }
  }

  private void xTransformChanged(int xAxis) {
    if (this.isOnDemandLoadingEnabled()) {
      this.update();
    }
    this.getXAxis(xAxis)
        .zoomRangeChanged()
        .trigger(this.getXAxis(xAxis).getZoomMinimum(), this.getXAxis(xAxis).getZoomMaximum());
  }

  private void yTransformChanged(int yAxis) {
    if (this.isOnDemandLoadingEnabled()) {
      this.update();
    }
    this.getYAxis(yAxis)
        .zoomRangeChanged()
        .trigger(this.getYAxis(yAxis).getZoomMinimum(), this.getYAxis(yAxis).getZoomMaximum());
  }

  private void jsSeriesSelected(double x, double y) {
    if (!this.isSeriesSelectionEnabled()) {
      return;
    }
    double smallestSqDistance = Double.POSITIVE_INFINITY;
    WDataSeries closestSeries = null;
    WPointF closestPointPx = new WPointF();
    WPointF closestPointBeforeSeriesTransform = new WPointF();
    for (int i = 0; i < this.series_.size(); ++i) {
      final WDataSeries series = this.series_.get(i);
      if (!series.isHidden()
          && (series.getType() == SeriesType.Line || series.getType() == SeriesType.Curve)) {
        WTransform transform =
            this.zoomRangeTransform(
                this.xAxes_.get(series.getXAxis()).transformHandle.getValue(),
                this.yAxes_.get(series.getYAxis()).transformHandle.getValue());
        WPointF p = transform.getInverted().map(new WPointF(x, y));
        WPainterPath path = this.pathForSeries(series);
        WTransform t = this.curveTransform(series);
        for (int j = 0; j < path.getSegments().size(); ++j) {
          final WPainterPath.Segment seg = path.getSegments().get(j);
          if (seg.getType() != SegmentType.CubicC1
              && seg.getType() != SegmentType.CubicC2
              && seg.getType() != SegmentType.QuadC) {
            WPointF segP = t.map(new WPointF(seg.getX(), seg.getY()));
            double dx = p.getX() - segP.getX();
            double dy = p.getY() - segP.getY();
            double d2 = dx * dx + dy * dy;
            if (d2 < smallestSqDistance) {
              smallestSqDistance = d2;
              closestSeries = series;
              closestPointPx = segP;
              closestPointBeforeSeriesTransform = new WPointF(seg.getX(), seg.getY());
            }
          }
        }
      }
    }
    {
      WTransform transform =
          this.zoomRangeTransform(
              this.xAxes_
                  .get(closestSeries != null ? closestSeries.getXAxis() : 0)
                  .transformHandle
                  .getValue(),
              this.yAxes_
                  .get(closestSeries != null ? closestSeries.getYAxis() : 0)
                  .transformHandle
                  .getValue());
      WPointF closestDisplayPoint = transform.map(closestPointPx);
      double dx = closestDisplayPoint.getX() - x;
      double dy = closestDisplayPoint.getY() - y;
      double d2 = dx * dx + dy * dy;
      if (d2 > CURVE_SELECTION_DISTANCE_SQUARED) {
        return;
      }
    }
    this.setSelectedSeries(closestSeries);
    if (closestSeries != null) {
      this.seriesSelected_.trigger(
          closestSeries,
          this.mapFromDeviceWithoutTransform(
              closestPointBeforeSeriesTransform, closestSeries.getAxis()));
    } else {
      this.seriesSelected_.trigger(
          (WDataSeries) null,
          this.mapFromDeviceWithoutTransform(closestPointBeforeSeriesTransform, Axis.Y));
    }
  }

  private void loadTooltip(double x, double y) {
    List<Double> pxs = new ArrayList<Double>();
    List<Double> rxs = new ArrayList<Double>();
    List<Double> pys = new ArrayList<Double>();
    List<Double> rys = new ArrayList<Double>();
    for (int i = 0; i < this.getXAxisCount(); ++i) {
      double px =
          this.zoomRangeTransform(this.xAxes_.get(i).transformHandle.getValue(), new WTransform())
              .getInverted()
              .map(new WPointF(x, 0.0))
              .getX();
      double rx =
          MarkerMatchIterator.MATCH_RADIUS / this.xAxes_.get(i).transformHandle.getValue().getM11();
      pxs.add(px);
      rxs.add(rx);
      for (int j = 0; j < this.getYAxisCount(); ++j) {
        WPointF p =
            this.zoomRangeTransform(new WTransform(), this.yAxes_.get(j).transformHandle.getValue())
                .getInverted()
                .map(new WPointF(0.0, y));
        pys.add(p.getY());
        rys.add(
            MarkerMatchIterator.MATCH_RADIUS
                / this.yAxes_.get(j).transformHandle.getValue().getM22());
      }
    }
    MarkerMatchIterator iterator = new MarkerMatchIterator(this, pxs, pys, rxs, rys);
    this.iterateSeries(iterator, (WPainter) null);
    if (iterator.getMatchedSeries() != null) {
      final WDataSeries series = iterator.getMatchedSeries();
      WString tooltip = series.getModel().getToolTip(iterator.getYRow(), iterator.getYColumn());
      boolean isDeferred =
          series
              .getModel()
              .flags(iterator.getYRow(), iterator.getYColumn())
              .contains(ItemFlag.DeferredToolTip);
      boolean isXHTML =
          series
              .getModel()
              .flags(iterator.getYRow(), iterator.getYColumn())
              .contains(ItemFlag.XHTMLText);
      if (!(tooltip.length() == 0) && isDeferred | isXHTML) {
        if (isXHTML) {
          boolean res = removeScript(tooltip);
          if (!res) {
            tooltip = escapeText(tooltip);
          }
        } else {
          tooltip = escapeText(tooltip);
        }
        this.doJavaScript(
            this.getCObjJsRef()
                + ".updateTooltip("
                + WString.toWString(tooltip).getJsStringLiteral()
                + ");");
      }
    } else {
      for (int btt = 0; btt < this.barTooltips_.size(); ++btt) {
        double[] xs = this.barTooltips_.get(btt).xs;
        double[] ys = this.barTooltips_.get(btt).ys;
        int j = 0;
        int k = 3;
        boolean c = false;
        WPointF p =
            this.zoomRangeTransform(
                    this.xAxes_
                        .get(this.barTooltips_.get(btt).series.getXAxis())
                        .transformHandle
                        .getValue(),
                    this.yAxes_
                        .get(this.barTooltips_.get(btt).series.getYAxis())
                        .transformHandle
                        .getValue())
                .getInverted()
                .map(new WPointF(x, y));
        for (; j < 4; k = j++) {
          if ((ys[j] <= p.getY() && p.getY() < ys[k] || ys[k] <= p.getY() && p.getY() < ys[j])
              && p.getX() < (xs[k] - xs[j]) * (p.getY() - ys[j]) / (ys[k] - ys[j]) + xs[j]) {
            c = !c;
          }
        }
        if (c) {
          WString tooltip =
              this.barTooltips_
                  .get(btt)
                  .series
                  .getModel()
                  .getToolTip(this.barTooltips_.get(btt).yRow, this.barTooltips_.get(btt).yColumn);
          if (!(tooltip.length() == 0)) {
            this.doJavaScript(
                this.getCObjJsRef()
                    + ".updateTooltip("
                    + WString.toWString(escapeText(tooltip, false)).getJsStringLiteral()
                    + ");");
          }
          return;
        }
      }
    }
  }

  WPointF hv(double x, double y, double width) {
    if (this.orientation_ == Orientation.Vertical) {
      return new WPointF(x, y);
    } else {
      return new WPointF(width - y, x);
    }
  }

  private WPointF inverseHv(double x, double y, double width) {
    if (this.orientation_ == Orientation.Vertical) {
      return new WPointF(x, y);
    } else {
      return new WPointF(y, width - x);
    }
  }

  WPointF inverseHv(final WPointF p) {
    if (p.isJavaScriptBound()) {
      if (this.getOrientation() == Orientation.Vertical) {
        return p;
      } else {
        return p.inverseSwapHV(this.height_);
      }
    }
    return this.inverseHv(p.getX(), p.getY(), this.height_);
  }

  private WRectF getInsideChartArea() {
    double xRenderStart = 0;
    double xRenderEnd = 0;
    if (this.getXAxisCount() >= 1) {
      final WAxis xAxis = this.getXAxis(0);
      final WAxis.Segment xs = xAxis.segments_.get(0);
      xRenderStart = xAxis.isInverted() ? xAxis.mapToDevice(xs.renderMaximum, 0) : xs.renderStart;
      xRenderEnd =
          xAxis.isInverted()
              ? xAxis.mapToDevice(xs.renderMinimum, 0)
              : xs.renderStart + xs.renderLength;
    }
    double yRenderStart = 0;
    double yRenderEnd = 0;
    if (this.getYAxisCount() >= 1) {
      final WAxis yAxis = this.getYAxis(0);
      final WAxis.Segment ys = yAxis.segments_.get(0);
      yRenderStart = yAxis.isInverted() ? yAxis.mapToDevice(ys.renderMaximum, 0) : ys.renderStart;
      yRenderEnd =
          yAxis.isInverted()
              ? yAxis.mapToDevice(ys.renderMinimum, 0)
              : ys.renderStart + ys.renderLength;
    }
    double x1 = this.chartArea_.getLeft() + xRenderStart;
    double x2 = this.chartArea_.getLeft() + xRenderEnd;
    double y1 = this.chartArea_.getBottom() - yRenderEnd;
    double y2 = this.chartArea_.getBottom() - yRenderStart;
    return new WRectF(x1, y1, x2 - x1, y2 - y1);
  }

  private int calcAxisSize(final WAxis axis, WPaintDevice device) {
    if (device.getFeatures().contains(PaintDeviceFeatureFlag.FontMetrics)) {
      if (this.getOrientation() == Orientation.Horizontal != (axis.getId() == Axis.X)) {
        WMeasurePaintDevice md = new WMeasurePaintDevice(device);
        double h = TICK_LENGTH;
        h += axis.calcMaxTickLabelSize(md, Orientation.Vertical);
        if (!(axis.getTitle().length() == 0)) {
          h += axis.calcTitleSize(md, Orientation.Vertical);
        }
        return (int) Math.ceil(h);
      } else {
        WMeasurePaintDevice md = new WMeasurePaintDevice(device);
        double w = TICK_LENGTH;
        w += axis.calcMaxTickLabelSize(md, Orientation.Horizontal);
        if (!(axis.getTitle().length() == 0)
            && axis.getTitleOrientation() == Orientation.Vertical) {
          w += axis.calcTitleSize(md, Orientation.Vertical) + 10;
        }
        return (int) Math.ceil(w);
      }
    } else {
      if (this.getOrientation() == Orientation.Horizontal != (axis.getId() == Axis.X)) {
        return TICK_LENGTH + 20 + ((axis.getTitle().length() == 0) ? 0 : 20);
      } else {
        return TICK_LENGTH + 30 + ((axis.getTitle().length() == 0) ? 0 : 15);
      }
    }
  }

  private void defineJavaScript() {
    WApplication app = WApplication.getInstance();
    if (app != null && (this.isInteractive() || this.hasDeferredToolTips_)) {
      app.loadJavaScript("js/ChartCommon.js", wtjs2());
      app.doJavaScript(
          "if (!Wt4.11.2.chartCommon) {Wt4.11.2.chartCommon = new "
              + "Wt4.11.2.ChartCommon("
              + app.getJavaScriptClass()
              + "); }",
          false);
      app.loadJavaScript("js/WCartesianChart.js", wtjs1());
      this.jsDefined_ = true;
    } else {
      this.jsDefined_ = false;
    }
  }

  void drawMarker(final WDataSeries series, MarkerType marker, final WPainterPath result) {
    final double size = 6.0;
    final double hsize = size / 2;
    switch (marker) {
      case Circle:
        result.addEllipse(-hsize, -hsize, size, size);
        break;
      case Square:
        result.addRect(new WRectF(-hsize, -hsize, size, size));
        break;
      case Cross:
        result.moveTo(-1.3 * hsize, 0);
        result.lineTo(1.3 * hsize, 0);
        result.moveTo(0, -1.3 * hsize);
        result.lineTo(0, 1.3 * hsize);
        break;
      case XCross:
        result.moveTo(-hsize, -hsize);
        result.lineTo(hsize, hsize);
        result.moveTo(-hsize, hsize);
        result.lineTo(hsize, -hsize);
        break;
      case Triangle:
        result.moveTo(0, 0.6 * hsize);
        result.lineTo(-hsize, 0.6 * hsize);
        result.lineTo(0, -hsize);
        result.lineTo(hsize, 0.6 * hsize);
        result.closeSubPath();
        break;
      case Star:
        {
          double angle = 3.14159265358979323846 / 2.0;
          for (int i = 0; i < 5; ++i) {
            double x = Math.cos(angle) * hsize;
            double y = -Math.sin(angle) * hsize;
            result.moveTo(0, 0);
            result.lineTo(x, y);
            angle += 3.14159265358979323846 * 2.0 / 5.0;
          }
        }
        break;
      case InvertedTriangle:
        result.moveTo(0, -0.6 * hsize);
        result.lineTo(-hsize, -0.6 * hsize);
        result.lineTo(0, hsize);
        result.lineTo(hsize, -0.6 * hsize);
        result.closeSubPath();
        break;
      case Diamond:
        {
          double s = Math.sqrt(2.0) * hsize;
          result.moveTo(0, s);
          result.lineTo(s, 0);
          result.lineTo(0, -s);
          result.lineTo(-s, 0);
          result.closeSubPath();
          break;
        }
      case Asterisk:
        {
          double angle = 3.14159265358979323846 / 2.0;
          for (int i = 0; i < 6; ++i) {
            double x = Math.cos(angle) * hsize;
            double y = -Math.sin(angle) * hsize;
            result.moveTo(0, 0);
            result.lineTo(x, y);
            angle += 3.14159265358979323846 / 3.0;
          }
          break;
        }
      case Custom:
        result.assign(series.getCustomMarker());
        break;
      default:;
    }
  }

  private boolean axisSliderWidgetForSeries(WDataSeries series) {
    for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
      if (this.axisSliderWidgets_.get(i).getSeries() == series) {
        return true;
      }
    }
    return false;
  }

  static class IconWidget extends WPaintedWidget {
    private static Logger logger = LoggerFactory.getLogger(IconWidget.class);

    public IconWidget(WCartesianChart chart, int index, WContainerWidget parentContainer) {
      super();
      this.chart_ = chart;
      this.index_ = index;
      this.setInline(true);
      this.resize(new WLength(20), new WLength(20));
      if (parentContainer != null) parentContainer.addWidget(this);
    }

    public IconWidget(WCartesianChart chart, int index) {
      this(chart, index, (WContainerWidget) null);
    }

    protected void paintEvent(WPaintDevice paintDevice) {
      WPainter painter = new WPainter(paintDevice);
      this.chart_.renderLegendIcon(
          painter, new WPointF(2.5, 10.0), this.chart_.getSeries(this.index_));
    }

    private WCartesianChart chart_;
    private int index_;
  }

  static WJavaScriptPreamble wtjs2() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "ChartCommon",
        "(function(t){const n=t.WT,e=this,r=n.gfxUtils,i=r.rect_top,o=r.rect_bottom,f=r.rect_left,u=r.rect_right,s=r.transform_mult;function l(t,n,e,r){let i=0;r&&(i=1);const o=n.length;function f(t){return e?n[t]:n[o-1-t]}function u(t){for(;2===f(t)[2]||3===f(t)[2];)t--;return t}let s=Math.floor(o/2);s=u(s);let l=0,h=o,c=!1;if(f(0)[i]>t)return e?-1:o;if(f(o-1)[i]<t)return e?o:-1;for(;!c;){let n=s+1;n<o&&(2===f(n)[2]||3===f(n)[2])&&(n+=2);if(f(s)[i]>t){h=s;s=Math.floor((h+l)/2);s=u(s)}else if(f(s)[i]===t)c=!0;else if(n<o&&f(n)[i]>t)c=!0;else if(n<o&&f(n)[i]===t){s=n;c=!0}else{l=s;s=Math.floor((h+l)/2);s=u(s)}}return e?s:o-1-s}function h(t,n){return n[0][t]<n[n.length-1][t]}this.findClosestPoint=function(t,n,e){let r=0;e&&(r=1);const i=h(r,n);let o=l(t,n,i,e);o<0&&(o=0);if(o>=n.length)return[n[n.length-1][0],n[n.length-1][1]];o>=n.length&&(o=n.length-2);if(n[o][r]===t)return[n[o][0],n[o][1]];let f=i?o+1:o-1;i&&2===n[f][2]&&(f+=2);if(!i&&f<0)return[n[o][0],n[o][1]];!i&&f>0&&3===n[f][2]&&(f-=2);return Math.abs(t-n[o][r])<Math.abs(n[f][r]-t)?[n[o][0],n[o][1]]:[n[f][0],n[f][1]]};this.minMaxY=function(t,n){const e=n?0:1;let r=t[0][e],i=t[0][e];for(let n=1;n<t.length;++n)if(2!==t[n][2]&&3!==t[n][2]&&5!==t[n][2]){t[n][e]>i&&(i=t[n][e]);t[n][e]<r&&(r=t[n][e])}return[r,i]};this.projection=function(t,n){const e=Math.cos(t),r=Math.sin(t),i=e*e,o=r*r,f=e*r,u=-n[0]*e-n[1]*r;return[i,f,f,o,e*u+n[0],r*u+n[1]]};this.distanceSquared=function(t,n){const e=[n[0]-t[0],n[1]-t[1]];return e[0]*e[0]+e[1]*e[1]};this.distanceLessThanRadius=function(t,n,r){return r*r>=e.distanceSquared(t,n)};this.toZoomLevel=function(t){return Math.floor(Math.log(t)/Math.LN2+.5)+1};this.isPointInRect=function(t,n){const e=t.x??t[0],r=t.y??t[1];return e>=f(n)&&e<=u(n)&&r>=i(n)&&r<=o(n)};this.toDisplayCoord=function(t,n,e,r,i){let o,f;if(e){o=[(t[0]-i[0])/i[2],(t[1]-i[1])/i[3]];f=[r[0]+o[1]*r[2],r[1]+o[0]*r[3]]}else{o=[(t[0]-i[0])/i[2],1-(t[1]-i[1])/i[3]];f=[r[0]+o[0]*r[2],r[1]+o[1]*r[3]]}return s(n,f)};this.findYRange=function(t,n,r,u,s,c,a,m,g){if(0===t.length)return null;const d=e.toDisplayCoord([r,0],[1,0,0,1,0,0],s,c,a),M=e.toDisplayCoord([u,0],[1,0,0,1,0,0],s,c,a),x=s?1:0,b=s?0:1,y=h(x,t);let C,_,p,R,D,E=l(d[x],t,y,s),L=l(M[x],t,y,s),O=1/0,T=-1/0;const A=E===L&&E===t.length||-1===E&&-1===L;if(!A){if(y)if(E<0)E=0;else{E++;t[E]&&2===t[E][2]&&(E+=2)}else E>=t.length-1&&(E=t.length-2);!y&&L<0&&(L=0);for(C=Math.min(E,L);C<=Math.max(E,L)&&C<t.length;++C)if(2!==t[C][2]&&3!==t[C][2]){t[C][b]<O&&(O=t[C][b]);t[C][b]>T&&(T=t[C][b])}if(y&&E>0||!y&&E<t.length-1){if(y){R=E-1;t[R]&&3===t[R][2]&&(R-=2)}else{R=E+1;t[R]&&2===t[R][2]&&(R+=2)}_=(d[x]-t[R][x])/(t[E][x]-t[R][x]);p=t[R][b]+_*(t[E][b]-t[R][b]);p<O&&(O=p);p>T&&(T=p)}if(y&&L<t.length-1||!y&&L>0){if(y){D=L+1;2===t[D][2]&&(D+=2)}else{D=L-1;3===t[D][2]&&(D-=2)}_=(M[x]-t[L][x])/(t[D][x]-t[L][x]);p=t[L][b]+_*(t[D][b]-t[L][b]);p<O&&(O=p);p>T&&(T=p)}}let P,S;const W=a[2]/(u-r),Y=s?2:3;if(!A){P=c[Y]/(T-O);S=10;P=c[Y]/(c[Y]/P+20);P>g.y[n]&&(P=g.y[n]);P<m.y[n]&&(P=m.y[n])}let Z;Z=s?[d[1]-i(c),A?0:(O+T)/2-c[2]/P/2-f(c)]:[d[0]-f(c),A?0:-((O+T)/2+c[3]/P/2-o(c))];return{xZoom:W,yZoom:P,panPoint:Z}};this.matchXAxis=function(t,n,e,r,s){function l(t){return r[t].side}function h(t){return r[t].width}function c(t){return r[t].minOffset}function a(t){return r[t].maxOffset}if(s){if(n<i(e)||n>o(e))return-1}else if(t<f(e)||t>u(e))return-1;for(let m=0;m<r.length;++m)if(s){if((\"min\"===l(m)||\"both\"===l(m))&&t>=f(e)-c(m)-h(m)&&t<=f(e)-c(m))return m;if((\"max\"===l(m)||\"both\"===l(m))&&t>=u(e)+a(m)&&t<=u(e)+a(m)+h(m))return m}else{if((\"min\"===l(m)||\"both\"===l(m))&&n<=o(e)+c(m)+h(m)&&n>=o(e)+c(m))return m;if((\"max\"===l(m)||\"both\"===l(m))&&n<=i(e)-a(m)&&n>=i(e)-a(m)-h(m))return m}return-1};this.matchYAxis=function(t,n,e,r,s){function l(t){return r[t].side}function h(t){return r[t].width}function c(t){return r[t].minOffset}function a(t){return r[t].maxOffset}if(s){if(t<f(e)||t>u(e))return-1}else if(n<i(e)||n>o(e))return-1;for(let m=0;m<r.length;++m)if(s){if((\"min\"===l(m)||\"both\"===l(m))&&n>=i(e)-c(m)-h(m)&&n<=i(e)-c(m))return m;if((\"max\"===l(m)||\"both\"===l(m))&&n>=o(e)+a(m)&&n<=o(e)+a(m)+h(m))return m}else{if((\"min\"===l(m)||\"both\"===l(m))&&t>=f(e)-c(m)-h(m)&&t<=f(e)-c(m))return m;if((\"max\"===l(m)||\"both\"===l(m))&&t>=u(e)+a(m)&&t<=u(e)+a(m)+h(m))return m}return-1}})");
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WCartesianChart",
        "(function(t,e,n,o){e.wtCObj=this;const i=this,r=t.WT;i.config=o;const l=r.gfxUtils,s=l.transform_mult,u=l.transform_inverted,f=l.transform_assign,c=l.transform_equal,a=l.transform_apply,x=l.rect_top,p=l.rect_bottom,y=l.rect_left,h=l.rect_right,d=l.rect_intersection,m=r.chartCommon,v=m.minMaxY,w=m.findClosestPoint,T=m.projection,M=m.distanceLessThanRadius,g=m.toZoomLevel,b=m.isPointInRect,E=m.findYRange,P=function(t,e){return m.matchXAxis(t,e,L(),o.xAxes,S())},C=function(t,e){return m.matchYAxis(t,e,L(),o.yAxes,S())};function A(t,e){const n=(i=t,o.xModelAreas[i]);var i;const r=function(t){return o.yModelAreas[t]}(e);return S()?[r[0],n[1],r[2],n[3]]:[n[0],r[1],n[2],r[3]]}function O(){return o.followCurve}function D(){return o.crosshair||-1!==O()}function S(){return o.isHorizontal}function j(t){return o.xTransforms[t]}function I(t){return o.yTransforms[t]}function L(){return o.area}function _(){return o.insideArea}function F(t=null){return null===t?o.series:o.series[t]}function U(t){return F(t).transform}function Y(t){return S()?s([0,1,1,0,0,0],s(U(t),[0,1,1,0,0,0])):U(t)}function Z(t){return F(t).curve}function R(t){return F(t).xAxis}function X(t){return F(t).yAxis}function k(){return o.seriesSelection}function W(){return o.sliders}function q(){return o.hasToolTips}function z(){return o.coordinateOverlayPadding}function B(){return o.curveManipulation}function N(t){return o.minZoom.x[t]}function K(t){return o.minZoom.y[t]}function H(t){return o.maxZoom.x[t]}function G(t){return o.maxZoom.y[t]}function J(){return o.pens}function $(){return o.penAlpha}function Q(){return o.selectedCurve}function V(t){t.preventDefault&&t.preventDefault()}function tt(t,n){e.addEventListener(t,n)}function et(t,n){e.removeEventListener(t,n)}function nt(t){return t.length}function ot(){return nt(o.xAxes)}function it(){return nt(o.yAxes)}function rt(){return o.crosshairXAxis}function lt(){return o.crosshairYAxis}let st=!1;const ut=function(t){if(!st){st=!0;requestAnimationFrame((function(){t();st=!1}))}};if(window.MSPointerEvent||window.PointerEvent){e.style.touchAction=\"none\";n.canvas.style.msTouchAction=\"none\";n.canvas.style.touchAction=\"none\"}const ft=.07,ct=.001,at=1.5;let xt=e.wtEObj2;if(!xt){xt={};xt.contextmenuListener=function(t){V(t);et(\"contextmenu\",xt.contextmenuListener)}}e.wtEObj2=xt;const pt={};function yt(t){return 2===t.pointerType||3===t.pointerType||\"pen\"===t.pointerType||\"touch\"===t.pointerType}let ht=!1;(window.MSPointerEvent||window.PointerEvent)&&function(){const t=[];function n(){ht=nt(t)>0}function o(o){if(yt(o)){V(o);t.push(o);n();pt.start(e,{touches:t.slice(0)})}}function i(o){if(ht&&yt(o)){V(o);for(let e=0;e<nt(t);++e)if(t[e].pointerId===o.pointerId){t.splice(e,1);break}n();pt.end(e,{touches:t.slice(0),changedTouches:[]})}}function r(o){if(yt(o)){V(o);for(let e=0;e<nt(t);++e)if(t[e].pointerId===o.pointerId){t[e]=o;break}n();pt.moved(e,{touches:t.slice(0)})}}const l=e.wtEObj;if(l)if(window.PointerEvent){et(\"pointerdown\",l.pointerDown);et(\"pointerup\",l.pointerUp);et(\"pointerout\",l.pointerUp);et(\"pointermove\",l.pointerMove)}else{et(\"MSPointerDown\",l.pointerDown);et(\"MSPointerUp\",l.pointerUp);et(\"MSPointerOut\",l.pointerUp);et(\"MSPointerMove\",l.pointerMove)}e.wtEObj={pointerDown:o,pointerUp:i,pointerMove:r};if(window.PointerEvent){tt(\"pointerdown\",o);tt(\"pointerup\",i);tt(\"pointerout\",i);tt(\"pointermove\",r)}else{tt(\"MSPointerDown\",o);tt(\"MSPointerUp\",i);tt(\"MSPointerOut\",i);tt(\"MSPointerMove\",r)}}();let dt=e.wtOObj??null,mt=null,vt=!0,wt=null,Tt=-1,Mt=-1,gt=[],bt=!1,Et=!1,Pt=null,Ct=null,At=null;const Ot={x:0,y:0};let Dt=null,St=null,jt=e.wtTObj;if(!jt){jt={overTooltip:!1};e.wtTObj=jt}function It(){if(jt){if(jt.tooltipTimeout){clearTimeout(jt.tooltipTimeout);jt.tooltipTimeout=null}if(!jt.overTooltip&&jt.tooltipOuterDiv){document.body.removeChild(jt.tooltipOuterDiv);jt.toolTipEl=null;jt.tooltipOuterDiv=null}}}let Lt=null,_t=!1,Ft=null;const Ut=[];for(let t=0;t<ot();++t){Ut.push([0,0,0,0,0,0]);f(Ut[t],j(t))}const Yt=[];for(let t=0;t<it();++t){Yt.push([0,0,0,0,0,0]);f(Yt[t],I(t))}function Zt(){if(function(){for(let t=0;t<ot();++t)if(o.notifyTransform.x[t])return!0;for(let t=0;t<it();++t)if(o.notifyTransform.y[t])return!0;return!1}()){if(Ft){window.clearTimeout(Ft);Ft=null}Ft=setTimeout((function(){for(let e=0;e<ot();++e)if(o.notifyTransform.x[e]&&!c(Ut[e],j(e))){t.emit(n.widget,\"xTransformChanged\"+e);f(Ut[e],j(e))}for(let e=0;e<it();++e)if(o.notifyTransform.y[e]&&!c(Yt[e],I(e))){t.emit(n.widget,\"yTransformChanged\"+e);f(Yt[e],I(e))}}),250)}}const Rt=function(t,e){f(t,e);Zt()};function Xt(t=0,e=0){if(S()){const n=y(L()),o=x(L());return s([0,1,1,0,n,o],s(j(t),s(I(e),[0,1,1,0,-o,-n])))}{const n=y(L()),o=p(L());return s([1,0,0,-1,n,o],s(j(t),s(I(e),[1,0,0,-1,-n,o])))}}n.combinedTransform=Xt;function kt(t,e){return s(Xt(t,e),_())}function Wt(t,e,n,o=!1){let i,r;i=o?t:s(u(Xt(e,n)),t);r=S()?[(i[1]-L()[1])/L()[3],(i[0]-L()[0])/L()[2]]:[(i[0]-L()[0])/L()[2],1-(i[1]-L()[1])/L()[3]];return[A(e,n)[0]+r[0]*A(e,n)[2],A(e,n)[1]+r[1]*A(e,n)[3]]}function qt(t,e,n,o=!1){return m.toDisplayCoord(t,o?[1,0,0,1,0,0]:Xt(e,n),S(),L(),A(e,n))}function zt(){for(let t=0;t<ot();++t){let e,n;const o=A(t,0);if(S()){e=(Wt([0,x(L())],t,0)[0]-o[0])/o[2];n=(Wt([0,p(L())],t,0)[0]-o[0])/o[2]}else{e=(Wt([y(L()),0],t,0)[0]-o[0])/o[2];n=(Wt([h(L()),0],t,0)[0]-o[0])/o[2]}for(let o=0;o<nt(W());++o){const i=r.$(W()[o]);if(i){const o=i.wtSObj;o&&o.xAxis()===t&&o.changeRange(e,n)}}}}function Bt(){It();q()&&jt.tooltipPosition&&(jt.tooltipTimeout=setTimeout((function(){Ht()}),500));vt&&ut((function(){n.repaint();D()&&Nt()}))}function Nt(){if(!vt)return;const t=dt.getContext(\"2d\");t.clearRect(0,0,dt.width,dt.height);t.save();t.beginPath();t.moveTo(y(L()),x(L()));t.lineTo(h(L()),x(L()));t.lineTo(h(L()),p(L()));t.lineTo(y(L()),p(L()));t.closePath();t.clip();let e,n=s(u(Xt(rt(),lt())),mt),i=mt[0],r=mt[1];if(-1!==O()){n=w(S()?n[1]:n[0],Z(O()),S());const t=s(Xt(R(O()),X(O())),s(Y(O()),n));i=t[0];r=t[1];mt[0]=i;mt[1]=r}e=S()?[(n[1]-L()[1])/L()[3],(n[0]-L()[0])/L()[2]]:[(n[0]-L()[0])/L()[2],1-(n[1]-L()[1])/L()[3]];if(-1!==O()){const t=A(R(O()),X(O()));n=[t[0]+e[0]*t[2],t[1]+e[1]*t[3]]}else{const t=A(rt(),lt());n=[t[0]+e[0]*t[2],t[1]+e[1]*t[3]]}t.fillStyle=t.strokeStyle=o.crosshairColor;t.font=\"16px sans-serif\";t.textAlign=\"right\";t.textBaseline=\"top\";let l=n[0].toFixed(2),f=n[1].toFixed(2);\"-0.00\"===l&&(l=\"0.00\");\"-0.00\"===f&&(f=\"0.00\");t.fillText(\"(\"+l+\",\"+f+\")\",h(L())-z()[0],x(L())+z()[1]);t.setLineDash&&t.setLineDash([1,2]);t.beginPath();t.moveTo(Math.floor(i)+.5,Math.floor(x(L()))+.5);t.lineTo(Math.floor(i)+.5,Math.floor(p(L()))+.5);t.moveTo(Math.floor(y(L()))+.5,Math.floor(r)+.5);t.lineTo(Math.floor(h(L()))+.5,Math.floor(r)+.5);t.stroke();t.restore()}function Kt(t=null){let e;S()&&(1===t?t=2:2===t&&(t=1));for(let n=0;n<ot();++n){let o=kt(n,0);if(S()){if(null===t||2===t){if(j(n)[0]<1){j(n)[0]=1;o=kt(n,0)}if(x(o)>x(_())){e=x(_())-x(o);j(n)[4]=j(n)[4]+e}else if(p(o)<p(_())){e=p(_())-p(o);j(n)[4]=j(n)[4]+e}}}else if(null===t||1===t){if(j(n)[0]<1){j(n)[0]=1;o=kt(n,0)}if(y(o)>y(_())){e=y(_())-y(o);j(n)[4]=j(n)[4]+e}else if(h(o)<h(_())){e=h(_())-h(o);j(n)[4]=j(n)[4]+e}}}for(let n=0;n<it();++n){let o=kt(0,n);if(S()){if(null===t||1===t){if(I(n)[3]<1){I(n)[3]=1;o=kt(0,n)}if(y(o)>y(_())){e=y(_())-y(o);I(n)[5]=I(n)[5]+e}else if(h(o)<h(_())){e=h(_())-h(o);I(n)[5]=I(n)[5]+e}}}else if(null===t||2===t){if(I(n)[3]<1){I(n)[3]=1;o=kt(0,n)}if(x(o)>x(_())){e=x(_())-x(o);I(n)[5]=I(n)[5]-e}else if(p(o)<p(_())){e=p(_())-p(o);I(n)[5]=I(n)[5]-e}}}Zt()}function Ht(){jt.toolTipEl||t.emit(n.widget,\"loadTooltip\",jt.tooltipPosition[0],jt.tooltipPosition[1])}this.updateTooltip=function(t){It();if(t){if(!jt.tooltipPosition)return;jt.toolTipEl=document.createElement(\"div\");jt.toolTipEl.className=o.ToolTipInnerStyle;jt.toolTipEl.innerHTML=t;jt.tooltipOuterDiv=document.createElement(\"div\");jt.tooltipOuterDiv.className=o.ToolTipOuterStyle;document.body.appendChild(jt.tooltipOuterDiv);jt.tooltipOuterDiv.appendChild(jt.toolTipEl);const e=r.widgetPageCoordinates(n.canvas),i=jt.tooltipPosition[0]+e.x,l=jt.tooltipPosition[1]+e.y;r.fitToWindow(jt.tooltipOuterDiv,i+10,l+10,i-10,l-10);jt.toolTipEl.addEventListener(\"mouseenter\",(function(){jt.overTooltip=!0}));jt.toolTipEl.addEventListener(\"mouseleave\",(function(){jt.overTooltip=!1}))}};this.mouseMove=function(t,e){setTimeout((function(){setTimeout(It,200);if(ht)return;const t=r.widgetCoordinates(n.canvas,e);if(b(t,L())){if(q()){jt.tooltipPosition=[t.x,t.y];jt.tooltipTimeout=setTimeout((function(){Ht()}),500)}if(null===wt&&D()&&vt){mt=[t.x,t.y];ut(Nt)}}}),0)};this.mouseOut=function(t,e){setTimeout(It,200)};this.mouseDown=function(t,e){if(ht)return;const o=r.widgetCoordinates(n.canvas,e),i=C(o.x,o.y),l=b(o,L()),s=P(o.x,o.y);if(-1!==i||-1!==s||l){wt=o;Tt=s;Mt=i}};this.mouseUp=function(t,e){if(!ht){wt=null;Tt=-1;Mt=-1}};this.mouseDrag=function(t,e){if(ht)return;if(null===wt){i.mouseDown(t,e);return}const l=r.widgetCoordinates(n.canvas,e);if(1===r.buttons)if(-1===Mt&&-1===Tt&&B()&&F(Q())){const t=Q();let e;e=S()?l.x-wt.x:l.y-wt.y;f(U(t),s([1,0,0,1,0,e/I(X(Q()))[3]],U(t)));Bt()}else o.pan&&ne({x:l.x-wt.x,y:l.y-wt.y},0,Tt,Mt);wt=l};this.clicked=function(e,o){if(ht)return;if(null!==wt)return;if(!k())return;const i=r.widgetCoordinates(n.canvas,o);t.emit(n.widget,\"seriesSelected\",i.x,i.y)};this.mouseWheel=function(t,e){const i=(e.metaKey<<3)+(e.altKey<<2)+(e.ctrlKey<<1)+e.shiftKey,l=o.wheelActions[i];if(void 0===l)return;const u=r.widgetCoordinates(n.canvas,e),c=P(u.x,u.y),x=C(u.x,u.y),p=b(u,L());if(-1===c&&-1===x&&!p)return;const y=r.normalizeWheel(e);if(p&&0===i&&B()){const t=Q(),n=-y.spinY;if(F(t)){const o=Y(t),i=a(o,Z(t)),l=v(i,S()),u=(l[0]+l[1])/2;r.cancelEvent(e);const c=Math.pow(1.2,n);f(U(t),s([1,0,0,c,0,u-c*u],U(t)));Bt();return}}if(4!==l&&5!==l&&6!==l||!o.pan){if(o.zoom){r.cancelEvent(e);let t=-y.spinY;0===t&&(t=-y.spinX);1===l?oe(u,0,t,c,x):0===l?oe(u,t,0,c,x):2===l?oe(u,t,t,c,x):3===l&&(0!==y.pixelX?oe(u,t,0,c,x):oe(u,0,t,c,x))}}else{const t=[];for(let e=0;e<ot();++e)t.push(j(e)[4]);const n=[];for(let t=0;t<it();++t)n.push(I(t)[5]);6===l?ne({x:-y.pixelX,y:-y.pixelY},0,c,x):5===l?ne({x:0,y:-y.pixelX-y.pixelY},0,c,x):4===l&&ne({x:-y.pixelX-y.pixelY,y:0},0,c,x);for(let n=0;n<ot();++n)t[n]!==j(n)[4]&&r.cancelEvent(e);for(let t=0;t<it();++t)n[t]!==I(t)[5]&&r.cancelEvent(e)}};const Gt=function(){k()&&t.emit(n.widget,\"seriesSelected\",wt.x,wt.y)};function Jt(){return dt||n.canvas}pt.start=function(t,e,i){bt=1===nt(e.touches);Et=2===nt(e.touches);if(bt){_t=!1;const t=r.widgetCoordinates(n.canvas,e.touches[0]),o=C(t.x,t.y),l=b(t,L()),s=P(t.x,t.y);if(-1===o&&-1===s&&!l)return;Lt=-1===o&&-1===s&&D()&&M(mt,[t.x,t.y],30)?1:0;St=Date.now();wt=t;Mt=o;Tt=s;if(1!==Lt){!i&&l&&(Dt=window.setTimeout(Gt,200));tt(\"contextmenu\",xt.contextmenuListener)}r.capture(null);r.capture(Jt())}else{if(!Et||!o.zoom&&!B())return;{if(Dt){window.clearTimeout(Dt);Dt=null}_t=!1;gt=[r.widgetCoordinates(n.canvas,e.touches[0]),r.widgetCoordinates(n.canvas,e.touches[1])].map((function(t){return[t.x,t.y]}));let t=-1,o=-1;if(!gt.every((function(t){return b(t,L())}))){t=P(gt[0][0],gt[0][1]);if(-1!==t){if(t!==P(gt[1][0],gt[1][1])){Et=null;return}}else{o=C(gt[0][0],gt[0][1]);if(1===o){Et=null;return}if(o!==C(gt[1][0],gt[1][1])){Et=null;return}}}r.capture(null);r.capture(Jt());Pt=Math.atan2(gt[1][1]-gt[0][1],gt[1][0]-gt[0][0]);Ct=[(gt[0][0]+gt[1][0])/2,(gt[0][1]+gt[1][1])/2];const i=Math.abs(Math.sin(Pt)),l=Math.abs(Math.cos(Pt));Pt=i<Math.sin(.125*Math.PI)?0:l<Math.cos(.375*Math.PI)?Math.PI/2:Math.tan(Pt)>0?Math.PI/4:-Math.PI/4;At=T(Pt,Ct);Tt=t;Mt=o}}V(e)};function $t(t,e=null){if(!_t)return;const n=Date.now();e=e??n-St;const o={x:0,y:0};let i;if(-1!==Tt)i=kt(Tt,0);else if(-1===Mt){i=kt(0,0);for(let t=1;t<ot();++t)i=d(i,kt(t,0));for(let t=1;t<it();++t)i=d(i,kt(0,t))}else i=kt(0,Mt);const r=2e-4;if(e>34){vt=!1;const n=Math.floor(e/17-1);for(let e=0;e<n;++e){$t(t,17);if(!_t){vt=!0;Bt();return}}e-=17*n;vt=!0}Ot.x!==1/0&&Ot.x!==-1/0||(Ot.x>0?Ot.x=at:Ot.x=-1.5);if(isFinite(Ot.x)){Ot.x=Ot.x/(1+.003*e);i[0]+=Ot.x*e;if(y(i)>y(_())){Ot.x=Ot.x+-r*(y(i)-y(_()))*e;Ot.x*=.7}else if(h(i)<h(_())){Ot.x=Ot.x+-r*(h(i)-h(_()))*e;Ot.x*=.7}Math.abs(Ot.x)<ct&&(y(i)>y(_())?Ot.x=ct:h(i)<h(_())&&(Ot.x=-.001));Math.abs(Ot.x)>at&&(Ot.x=(Ot.x>0?1:-1)*at);o.x=Ot.x*e}Ot.y!==1/0&&Ot.y!==-1/0||(Ot.y>0?Ot.y=at:Ot.y=-1.5);if(isFinite(Ot.y)){Ot.y=Ot.y/(1+.003*e);i[1]+=Ot.y*e;if(x(i)>x(_())){Ot.y=Ot.y+-r*(x(i)-x(_()))*e;Ot.y*=.7}else if(p(i)<p(_())){Ot.y=Ot.y+-r*(p(i)-p(_()))*e;Ot.y*=.7}Math.abs(Ot.y)<.001&&(x(i)>x(_())?Ot.y=.001:p(i)<p(_())&&(Ot.y=-.001));Math.abs(Ot.y)>at&&(Ot.y=(Ot.y>0?1:-1)*at);o.y=Ot.y*e}if(-1!==Tt)i=kt(Tt,0);else if(-1===Mt){i=kt(0,0);for(let t=1;t<ot();++t)i=d(i,kt(t,0));for(let t=1;t<it();++t)i=d(i,kt(0,t))}else i=kt(0,Mt);ne(o,1,Tt,Mt);let l;if(-1!==Tt)l=kt(Tt,0);else if(-1===Mt){l=kt(0,0);for(let t=1;t<ot();++t)l=d(l,kt(t,0));for(let t=1;t<it();++t)l=d(l,kt(0,t))}else l=kt(0,Mt);if(y(i)>y(_())&&y(l)<=y(_())){Ot.x=0;ne({x:-o.x,y:0},1,Tt,Mt);Kt(1)}if(h(i)<h(_())&&h(l)>=h(_())){Ot.x=0;ne({x:-o.x,y:0},1,Tt,Mt);Kt(1)}if(x(i)>x(_())&&x(l)<=x(_())){Ot.y=0;ne({x:0,y:-o.y},1,Tt,Mt);Kt(2)}if(p(i)<p(_())&&p(l)>=p(_())){Ot.y=0;ne({x:0,y:-o.y},1,Tt,Mt);Kt(2)}if(Math.abs(Ot.x)<.02&&Math.abs(Ot.y)<.02&&function(t){return x(t)<=x(_())+3&&p(t)>=p(_())-3&&y(t)<=y(_())+3&&h(t)>=h(_())-3}(l)){Kt();_t=!1;wt=null;Ot.x=0;Ot.y=0;St=null;gt=[]}else{St=n;vt&&requestAnimationFrame($t)}}pt.end=function(t,e){if(Dt){window.clearTimeout(Dt);Dt=null}window.setTimeout((function(){et(\"contextmenu\",xt.contextmenuListener)}),0);let n=Array.prototype.slice.call(e.touches),r=0===nt(n);if(!r)for(let t=0;t<nt(e.changedTouches);++t){const o=e.changedTouches[t].identifier;for(let t=0;t<nt(n);++t)if(n[t].identifier===o){n.splice(t,1);return}}r=0===nt(n);bt=1===nt(n);Et=2===nt(n);if(r){Qt=null;if(0===Lt&&(isFinite(Ot.x)||isFinite(Ot.y))&&o.rubberBand){St=Date.now();_t=!0;requestAnimationFrame($t)}else{1===Lt&&i.mouseUp(null,null);n=[];Pt=null;Ct=null;At=null;St=null}Lt=null}else(bt||Et)&&pt.start(t,e,!0)};let Qt=null,Vt=null,te=null;pt.moved=function(t,e){if((bt||Et)&&(!bt||null!==wt)){V(e);Vt=r.widgetCoordinates(n.canvas,e.touches[0]);nt(e.touches)>1&&(te=r.widgetCoordinates(n.canvas,e.touches[1]));if(-1===Tt&&-1===Mt&&bt&&Dt&&!M([Vt.x,Vt.y],[wt.x,wt.y],3)){window.clearTimeout(Dt);Dt=null}Qt||(Qt=setTimeout((function(){if(-1===Tt&&-1===Mt&&bt&&B()&&F(Q())){const t=Q();if(F(t)){const e=Vt;let n;n=S()?(e.x-wt.x)/I(X(Q()))[3]:(e.y-wt.y)/I(X(Q()))[3];U(t)[5]+=n;wt=e;Bt()}}else if(bt){const t=Vt,e=Date.now(),n={x:t.x-wt.x,y:t.y-wt.y},i=e-St;St=e;if(1===Lt){mt[0]+=n.x;mt[1]+=n.y;D()&&vt&&requestAnimationFrame(Nt)}else if(o.pan){Ot.x=n.x/i;Ot.y=n.y/i;ne(n,o.rubberBand?2:0,Tt,Mt)}wt=t}else if(-1===Tt&&-1===Mt&&Et&&B()&&F(Q())){const t=Q();if(F(t)){const e=S()?0:1,n=[Vt,te].map((function(t){return[t.x,t.y]})),o=Math.abs(gt[1][e]-gt[0][e]),i=Math.abs(n[1][e]-n[0][e]);let r=o>0?i/o:1;i===o&&(r=1);const l=s(u(Xt(R(t),X(t))),[0,(gt[0][e]+gt[1][e])/2])[1],c=s(u(Xt(R(t),X(t))),[0,(n[0][e]+n[1][e])/2])[1];f(U(t),s([1,0,0,r,0,-r*l+c],U(t)));Bt();wt=null;gt=n}}else if(Et&&o.zoom){const t=Wt(mt,rt(),lt());let e=(gt[0][0]+gt[1][0])/2,n=(gt[0][1]+gt[1][1])/2;const o=[Vt,te].map((function(t){return 0===Pt?[t.x,n]:Pt===Math.PI/2?[e,t.y]:s(At,[t.x,t.y])})),i=Math.abs(gt[1][0]-gt[0][0]),r=Math.abs(o[1][0]-o[0][0]);let l=i>0?r/i:1;r!==i&&Pt!==Math.PI/2||(l=1);let u=(o[0][0]+o[1][0])/2;const f=Math.abs(gt[1][1]-gt[0][1]),c=Math.abs(o[1][1]-o[0][1]);let a=f>0?c/f:1;c!==f&&0!==Pt||(a=1);let x=(o[0][1]+o[1][1])/2;if(S()){[l,a]=[a,l];[e,n]=[n,e];[u,x]=[x,u]}const p=[];for(let t=0;t<ot();++t)p.push(l);for(let t=0;t<ot();++t){j(t)[0]*p[t]>H(t)&&(p[t]=H(t)/j(t)[0]);j(t)[0]*p[t]<N(t)&&(p[t]=N(t)/j(t)[0])}const y=[];for(let t=0;t<it();++t)y.push(a);for(let t=0;t<it();++t){I(t)[3]*y[t]>G(t)&&(y[t]=G(t)/I(t)[3]);I(t)[3]*y[t]<K(t)&&(y[t]=K(t)/I(t)[3])}if(-1!==Tt)1!==p[Tt]&&(p[Tt]<1||j(Tt)[0]!==H(Tt))&&Rt(j(Tt),s([p[Tt],0,0,1,-p[Tt]*e+u,0],j(Tt)));else if(-1===Mt){for(let t=0;t<ot();++t)1!==p[t]&&(p[t]<1||j(t)[0]!==H(t))&&Rt(j(t),s([p[t],0,0,1,-p[t]*e+u,0],j(t)));for(let t=0;t<it();++t)1!==y[t]&&(y[t]<1||I(t)[3]!==G(t))&&Rt(I(t),s([1,0,0,y[t],0,-y[t]*n+x],I(t)))}else 1!==y[Mt]&&(y[Mt]<1||I(Mt)[3]!==G(Mt))&&Rt(I(Mt),s([1,0,0,y[Mt],0,-y[Mt]*n+x],I(Mt)));Kt();const h=qt(t,rt(),lt());mt[0]=h[0];mt[1]=h[1];gt=o;ee();Bt();zt()}Qt=null}),1))}};function ee(){for(let t=0;t<nt(J().x);++t){let e=g(j(t)[0])-1;j(t)[0]===H(t)&&(e=nt(J().x[t])-1);e>=nt(J().x[t])&&(e=nt(J().x[t])-1);for(let n=0;n<nt(J().x[t]);++n)if(e===n)for(let e=0;e<nt(J().x[t][n]);++e)J().x[t][n][e].color[3]=$().x[t][e];else for(let e=0;e<nt(J().x[t][n]);++e)J().x[t][n][e].color[3]=0}for(let t=0;t<nt(J().y);++t){let e=g(I(t)[3])-1;I(t)[3]===G(t)&&(e=nt(J().y[t])-1);e>=nt(J().y[t])&&(e=nt(J().y[t])-1);for(let n=0;n<nt(J().y[t]);++n)if(e===n)for(let e=0;e<nt(J().y[t][n]);++e)J().y[t][n][e].color[3]=$().y[t][e];else for(let e=0;e<nt(J().y[t][n]);++e)J().y[t][n][e].color[3]=0}}function ne(t,e=0,n=-1,o=-1){const i=Wt(mt,rt(),lt());S()&&(t={x:t.y,y:-t.x});if(1&e){if(-1!==n)j(n)[4]=j(n)[4]+t.x;else if(-1===o){for(let e=0;e<ot();++e)j(e)[4]=j(e)[4]+t.x;for(let e=0;e<it();++e)I(e)[5]=I(e)[5]-t.y}else I(o)[5]=I(o)[5]-t.y;Zt()}else if(2&e){let e;if(-1!==n)e=kt(n,0);else if(-1===o){e=kt(0,0);for(let t=1;t<ot();++t)e=d(e,kt(t,0));for(let t=1;t<it();++t)e=d(e,kt(0,t))}else e=kt(0,o);y(e)>y(_())?t.x>0&&(t.x=t.x/(1+(y(e)-y(_()))*ft)):h(e)<h(_())&&t.x<0&&(t.x=t.x/(1+(h(_())-h(e))*ft));x(e)>x(_())?t.y>0&&(t.y=t.y/(1+(x(e)-x(_()))*ft)):p(e)<p(_())&&t.y<0&&(t.y=t.y/(1+(p(_())-p(e))*ft));if(-1!==n)j(n)[4]=j(n)[4]+t.x;else if(-1===o){for(let e=0;e<ot();++e)j(e)[4]=j(e)[4]+t.x;for(let e=0;e<it();++e)I(e)[5]=I(e)[5]-t.y}else I(o)[5]=I(o)[5]-t.y;-1===o&&(mt[0]=mt[0]+t.x);-1===n&&(mt[1]=mt[1]+t.y);Zt()}else{if(-1!==n)j(n)[4]=j(n)[4]+t.x;else if(-1===o){for(let e=0;e<ot();++e)j(e)[4]=j(e)[4]+t.x;for(let e=0;e<it();++e)I(e)[5]=I(e)[5]-t.y}else I(o)[5]=I(o)[5]-t.y;-1===o&&(mt[0]=mt[0]+t.x);-1===n&&(mt[1]=mt[1]+t.y);Kt()}const r=qt(i,rt(),lt());mt[0]=r[0];mt[1]=r[1];Bt();zt()}function oe(t,e,n,o=-1,i=-1){const r=Wt(mt,rt(),lt());let l;l=S()?[t.y-x(L()),t.x-y(L())]:s(u([1,0,0,-1,y(L()),p(L())]),[t.x,t.y]);const f=l[0],c=l[1];let a=Math.pow(1.2,S()?n:e),h=Math.pow(1.2,S()?e:n);if(-1!==o){j(o)[0]*a>H(o)&&(a=H(o)/j(o)[0]);(a<1||j(o)[0]!==H(o))&&Rt(j(o),s([a,0,0,1,f-a*f,0],j(o)))}else if(-1===i){for(let t=0;t<ot();++t){let e=a;j(t)[0]*a>H(t)&&(e=H(t)/j(t)[0]);(e<1||j(t)[0]!==H(t))&&Rt(j(t),s([e,0,0,1,f-e*f,0],j(t)))}for(let t=0;t<it();++t){let e=h;I(t)[3]*h>G(t)&&(e=G(t)/I(t)[3]);(e<1||I(t)[3]!==G(t))&&Rt(I(t),s([1,0,0,e,0,c-e*c],I(t)))}}else{I(i)[3]*h>G(i)&&(h=G(i)/I(i)[3]);(h<1||I(i)[3]!==G(i))&&Rt(I(i),s([1,0,0,h,0,c-h*c],I(i)))}Kt();const d=qt(r,rt(),lt());mt[0]=d[0];mt[1]=d[1];ee();Bt();zt()}this.setXRange=function(t,e,n,i){const r=A(R(t),0);e=r[0]+r[2]*e;n=r[0]+r[2]*n;if(y(r)>h(r)){e>y(r)&&(e=y(r));n<h(r)&&(n=h(r))}else{e<y(r)&&(e=y(r));n>h(r)&&(n=h(r))}const l=Z(t),s=E(l,X(t),e,n,S(),L(),A(R(t),X(t)),o.minZoom,o.maxZoom),u=s.xZoom,f=s.yZoom,c=s.panPoint,a=Wt(mt,rt(),lt());j(R(t))[0]=u;f&&i&&(I(X(t))[3]=f);j(R(t))[4]=-c[0]*u;f&&i&&(I(X(t))[5]=-c[1]*f);Zt();const x=qt(a,rt(),lt());mt[0]=x[0];mt[1]=x[1];Kt();ee();Bt();zt()};this.getSeries=function(t){return Z(t)};this.rangeChangedCallbacks=[];this.updateConfig=function(t){for(const[e,n]of Object.entries(t))o[e]=n;!function(){if(!D()||null!==dt&&n.canvas.width===dt.width&&n.canvas.height===dt.height){if(null!==dt&&!D()){dt.parentNode.removeChild(dt);delete e.wtOObj;dt=null}}else{if(dt){dt.parentNode.removeChild(dt);delete e.wtOObj;dt=null}const t=document.createElement(\"canvas\");t.setAttribute(\"width\",n.canvas.width);t.setAttribute(\"height\",n.canvas.height);t.style.position=\"absolute\";t.style.display=\"block\";t.style.left=\"0\";t.style.top=\"0\";if(window.MSPointerEvent||window.PointerEvent){t.style.msTouchAction=\"none\";t.style.touchAction=\"none\"}n.canvas.parentNode.appendChild(t);dt=t;e.wtOObj=dt}mt=[(y(L())+h(L()))/2,(x(L())+p(L()))/2]}();ee();Bt();zt()};this.updateConfig({});if(!window.TouchEvent||window.MSPointerEvent||window.PointerEvent){const t=function(){};i.touchStart=t;i.touchEnd=t;i.touchMoved=t}else{i.touchStart=pt.start;i.touchEnd=pt.end;i.touchMoved=pt.moved}})");
  }

  static String locToJsString(AxisValue loc) {
    switch (loc) {
      case Minimum:
        return "min";
      case Maximum:
        return "max";
      case Zero:
        return "zero";
      case Both:
        return "both";
    }
    assert false;
    return "";
  }

  static int binarySearchRow(
      final WAbstractChartModel model, int xColumn, double d, int minRow, int maxRow) {
    if (minRow == maxRow) {
      return minRow;
    }
    double min = model.getData(minRow, xColumn);
    double max = model.getData(maxRow, xColumn);
    if (d <= min) {
      return minRow;
    }
    if (d >= max) {
      return maxRow;
    }
    double start = minRow + (d - min) / (max - min) * (maxRow - minRow);
    double data = model.getData((int) start, xColumn);
    if (data < d) {
      return binarySearchRow(model, xColumn, d, (int) start + 1, maxRow);
    } else {
      if (data > d) {
        return binarySearchRow(model, xColumn, d, minRow, (int) start - 1);
      } else {
        return (int) start;
      }
    }
  }

  private static final int TICK_LENGTH = 5;
  private static final int CURVE_LABEL_PADDING = 10;
  private static final int DEFAULT_CURVE_LABEL_WIDTH = 100;
  private static final int CURVE_SELECTION_DISTANCE_SQUARED = 400;

  static int toZoomLevel(double zoomFactor) {
    return (int) Math.floor(Math.log(zoomFactor) / Math.log(2.0) + 0.5) + 1;
  }
}
