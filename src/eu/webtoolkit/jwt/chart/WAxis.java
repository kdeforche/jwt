/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.chart;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.lang.ref.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.*;
import javax.servlet.*;
import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.utils.*;
import eu.webtoolkit.jwt.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which represents an axis of a cartesian chart.
 * <p>
 * 
 * A cartesian chart has two or three axes: an X axis ({@link XAxis}), a Y axis
 * ({@link YAxis}) and optionally a second Y axis ({@link Y2Axis}). Each of the
 * up to three axes in a cartesian chart has a unique {@link } that identifies
 * which of these three axes it is in the enclosing chart().
 * <p>
 * Use {@link } to change the visibility of an axis, {@link } to show grid lines
 * for an axis. The pen styles for rendering the axis or grid lines may be
 * changed using {@link } and {@link }. A margin between the axis and the main
 * plot area may be configured using {@link }.
 * <p>
 * By default, the axis will automatically adjust its range so that all data
 * will be visible. You may manually specify a range using {@link }, setMaximum
 * or {@link }. The interval between labels is by default automatically adjusted
 * depending on the axis length and the range, but may be manually specified
 * using {@link }.
 * <p>
 * The axis has support for being &quot;broken&quot;, to support displaying data
 * with a few outliers which would otherwise swamp the chart. This is not done
 * automatically, but instead you need to use {@link } to specify the value range
 * that needs to be omitted from the axis. The omission is rendered in the axis
 * and in bars that cross the break.
 * <p>
 * The labels are shown using a &quot;%.4g&quot; format string for numbers, and
 * a suitable format for {@link DateScale} or {@link DateTimeScale} scales,
 * based on heuristics. The format may be customized using {@link }. The angle of
 * the label text may be changed using {@link }. By default, all labels are
 * printed horizontally.
 * <p>
 * 
 * @see WCartesianChart
 */
public class WAxis {
	private static Logger logger = LoggerFactory.getLogger(WAxis.class);

	/**
	 * Constant which indicates automatic minimum calculation.
	 * <p>
	 */
	public static final double AUTO_MINIMUM = -Double.MAX_VALUE;
	/**
	 * Constant which indicates automatic maximum calculation.
	 * <p>
	 */
	public static final double AUTO_MAXIMUM = Double.MAX_VALUE;

	/**
	 * Returns the axis id.
	 * <p>
	 */
	public Axis getId() {
		return this.axis_;
	}

	/**
	 * Sets whether this axis is visible.
	 * <p>
	 * Changes whether the axis is displayed, including ticks and labels. The
	 * rendering of the grid lines is controlled seperately by {@link }.
	 * <p>
	 * The default value is true for the X axis and first Y axis, but false for
	 * the second Y axis.
	 * <p>
	 */
	public void setVisible(boolean visible) {
		if (!ChartUtils.equals(this.visible_, visible)) {
			this.visible_ = visible;
			update();
		}
		;
	}

	/**
	 * Returns whether this axis is visible.
	 * <p>
	 * 
	 * @see WAxis#setVisible(boolean visible)
	 */
	public boolean isVisible() {
		return this.visible_;
	}

	/**
	 * Sets the axis location.
	 * <p>
	 * Configures the location of the axis, relative to values on the other axis
	 * (i.e. Y values for the X axis, and X values for the Y axis).
	 * <p>
	 * The default value is {@link }.
	 * <p>
	 */
	public void setLocation(AxisValue location) {
		if (!ChartUtils.equals(this.location_, location)) {
			this.location_ = location;
			update();
		}
		;
	}

	/**
	 * Returns the axis location.
	 * <p>
	 * 
	 * @see WAxis#setLocation(AxisValue location)
	 */
	public AxisValue getLocation() {
		return this.location_;
	}

	/**
	 * Sets the scale of the axis.
	 * <p>
	 * For the X scale in a {@link CategoryChart}, the scale should be left
	 * unchanged to {@link CategoryScale}.
	 * <p>
	 * For all other axes, the default value is {@link LinearScale}, but this
	 * may be changed to {@link LogScale} or {@link DateScale}.
	 * {@link DateScale} is only useful for the X axis in a ScatterPlot which
	 * contains {@link WDate} values.
	 * <p>
	 */
	public void setScale(AxisScale scale) {
		if (!ChartUtils.equals(this.scale_, scale)) {
			this.scale_ = scale;
			update();
		}
		;
	}

	/**
	 * Returns the scale of the axis.
	 * <p>
	 * 
	 * @see WAxis#setScale(AxisScale scale)
	 */
	public AxisScale getScale() {
		return this.scale_;
	}

	/**
	 * Sets the minimum value displayed on the axis.
	 * <p>
	 * By default, the minimum and maximum values are determined automatically
	 * so that all the data can be displayed.
	 * <p>
	 * The numerical value corresponding to a data point is defined by it&apos;s
	 * AxisScale type.
	 * <p>
	 */
	public void setMinimum(double minimum) {
		final WAxis.Segment s = this.segments_.get(0);
		if (!ChartUtils.equals(s.minimum, minimum)) {
			s.minimum = minimum;
			update();
		}
		;
		if (!ChartUtils.equals(s.maximum, Math.max(s.minimum, s.maximum))) {
			s.maximum = Math.max(s.minimum, s.maximum);
			update();
		}
		;
		this.roundLimits_.remove(AxisValue.MinimumValue);
		this.update();
	}

	/**
	 * Returns the minimum value displayed on the axis.
	 * <p>
	 * This returned the minimum value that was set using
	 * {@link WAxis#setMinimum(double minimum) setMinimum()}, or otherwise the
	 * automatically calculated (and rounded) minimum.
	 * <p>
	 * The numerical value corresponding to a data point is defined by it&apos;s
	 * AxisScale type.
	 * <p>
	 * 
	 * @see WAxis#setMinimum(double minimum)
	 */
	public double getMinimum() {
		return !EnumUtils.mask(this.getAutoLimits(), AxisValue.MinimumValue)
				.isEmpty() ? this.segments_.get(0).renderMinimum
				: this.segments_.get(0).minimum;
	}

	/**
	 * Sets the maximum value for the axis displayed on the axis.
	 * <p>
	 * By default, the minimum and maximum values are determined automatically
	 * so that all the data can be displayed.
	 * <p>
	 * The numerical value corresponding to a data point is defined by it&apos;s
	 * AxisScale type.
	 * <p>
	 * 
	 * @see WAxis#setMinimum(double minimum)
	 */
	public void setMaximum(double maximum) {
		final WAxis.Segment s = this.segments_.get(this.segments_.size() - 1);
		if (!ChartUtils.equals(s.maximum, maximum)) {
			s.maximum = maximum;
			update();
		}
		;
		if (!ChartUtils.equals(s.minimum, Math.min(s.minimum, s.maximum))) {
			s.minimum = Math.min(s.minimum, s.maximum);
			update();
		}
		;
		this.roundLimits_.remove(AxisValue.MaximumValue);
		this.update();
	}

	/**
	 * Returns the maximum value displayed on the axis.
	 * <p>
	 * This returned the maximum value that was set using
	 * {@link WAxis#setMaximum(double maximum) setMaximum()}, or otherwise the
	 * automatically calculated (and rounded) maximum.
	 * <p>
	 * The numerical value corresponding to a data point is defined by it&apos;s
	 * AxisScale type.
	 * <p>
	 * 
	 * @see WAxis#setMaximum(double maximum)
	 */
	public double getMaximum() {
		final WAxis.Segment s = this.segments_.get(this.segments_.size() - 1);
		return !EnumUtils.mask(this.getAutoLimits(), AxisValue.MaximumValue)
				.isEmpty() ? s.renderMaximum : s.maximum;
	}

	/**
	 * Sets the axis range (minimum and maximum values) manually.
	 * <p>
	 * Specifies both minimum and maximum value for the axis. This automatically
	 * disables automatic range calculation.
	 * <p>
	 * The numerical value corresponding to a data point is defined by it&apos;s
	 * AxisScale type.
	 * <p>
	 * 
	 * @see WAxis#setMinimum(double minimum)
	 * @see WAxis#setMaximum(double maximum)
	 */
	public void setRange(double minimum, double maximum) {
		if (maximum > minimum) {
			this.segments_.get(0).minimum = minimum;
			this.segments_.get(this.segments_.size() - 1).maximum = maximum;
			this.roundLimits_.clear();
			this.update();
		}
	}

	/**
	 * Sets the axis resolution.
	 * <p>
	 * Specifies the axis resolution, in case maximum-minimum &lt; resolution
	 * minimum and maximum are modified so the maximum - minimum = resolution
	 * <p>
	 * The default resolution is 0, which uses a built-in epsilon.
	 * <p>
	 */
	public void setResolution(final double resolution) {
		this.resolution_ = resolution;
		this.update();
	}

	/**
	 * Returns the axis resolution.
	 * <p>
	 * 
	 * @see WAxis#setResolution(double resolution)
	 */
	public double getResolution() {
		return this.resolution_;
	}

	/**
	 * Let the minimum and/or maximum be calculated from the data.
	 * <p>
	 * Using this method, you can indicate that you want to have automatic
	 * limits, rather than limits set manually using
	 * {@link WAxis#setMinimum(double minimum) setMinimum()} or
	 * {@link WAxis#setMaximum(double maximum) setMaximum()}.
	 * <p>
	 * <code>locations</code> can be {@link } and/or {@link }.
	 * <p>
	 * The default value is {@link } | {@link }.
	 */
	public void setAutoLimits(EnumSet<AxisValue> locations) {
		if (!EnumUtils.mask(locations, AxisValue.MinimumValue).isEmpty()) {
			if (!ChartUtils.equals(this.segments_.get(0).minimum, AUTO_MINIMUM)) {
				this.segments_.get(0).minimum = AUTO_MINIMUM;
				update();
			}
			;
			this.roundLimits_.add(AxisValue.MinimumValue);
		}
		if (!EnumUtils.mask(locations, AxisValue.MaximumValue).isEmpty()) {
			if (!ChartUtils.equals(
					this.segments_.get(this.segments_.size() - 1).maximum,
					AUTO_MAXIMUM)) {
				this.segments_.get(this.segments_.size() - 1).maximum = AUTO_MAXIMUM;
				update();
			}
			;
			this.roundLimits_.add(AxisValue.MaximumValue);
		}
	}

	/**
	 * Let the minimum and/or maximum be calculated from the data.
	 * <p>
	 * Calls {@link #setAutoLimits(EnumSet locations)
	 * setAutoLimits(EnumSet.of(location, locations))}
	 */
	public final void setAutoLimits(AxisValue location, AxisValue... locations) {
		setAutoLimits(EnumSet.of(location, locations));
	}

	/**
	 * Returns the limits that are calculated automatically.
	 * <p>
	 * This returns the limits ({@link } and/or {@link }) that are calculated
	 * automatically from the data, rather than being specified manually using
	 * {@link WAxis#setMinimum(double minimum) setMinimum()} and/or
	 * {@link WAxis#setMaximum(double maximum) setMaximum()}.
	 * <p>
	 * 
	 * @see WAxis#setAutoLimits(EnumSet locations)
	 */
	public EnumSet<AxisValue> getAutoLimits() {
		EnumSet<AxisValue> result = EnumSet.noneOf(AxisValue.class);
		if (this.segments_.get(0).minimum == AUTO_MINIMUM) {
			result.add(AxisValue.MinimumValue);
		}
		if (this.segments_.get(this.segments_.size() - 1).maximum == AUTO_MAXIMUM) {
			result.add(AxisValue.MaximumValue);
		}
		return result;
	}

	/**
	 * Specifies whether limits should be rounded.
	 * <p>
	 * When enabling rounding, this has the effect of rounding down the minimum
	 * value, or rounding up the maximum value, to the nearest label interval.
	 * <p>
	 * By default, rounding is enabled for an auto-calculated limited, and
	 * disabled for a manually specifed limit.
	 * <p>
	 * 
	 * @see WAxis#setAutoLimits(EnumSet locations)
	 */
	public void setRoundLimits(EnumSet<AxisValue> locations) {
		this.roundLimits_ = EnumSet.copyOf(locations);
	}

	/**
	 * Specifies whether limits should be rounded.
	 * <p>
	 * Calls {@link #setRoundLimits(EnumSet locations)
	 * setRoundLimits(EnumSet.of(location, locations))}
	 */
	public final void setRoundLimits(AxisValue location, AxisValue... locations) {
		setRoundLimits(EnumSet.of(location, locations));
	}

	/**
	 * Returns whether limits should be rounded.
	 * <p>
	 * 
	 * @see WAxis#setRoundLimits(EnumSet locations)
	 */
	public EnumSet<AxisValue> getRoundLimits() {
		return this.roundLimits_;
	}

	/**
	 * Specifies a range that needs to be omitted from the axis.
	 * <p>
	 * This is useful to display data with a few outliers which would otherwise
	 * swamp the chart. This is not done automatically, but instead you need to
	 * use {@link } to specify the value range that needs to be omitted from the
	 * axis. The omission is rendered in the axis and in BarSeries that cross
	 * the break.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>This feature is incompatible with the interactive
	 * features of {@link WCartesianChart}. </i>
	 * </p>
	 */
	public void setBreak(double minimum, double maximum) {
		if (this.segments_.size() != 2) {
			this.segments_.add(new WAxis.Segment());
			this.segments_.get(1).maximum = this.segments_.get(0).maximum;
		}
		this.segments_.get(0).maximum = minimum;
		this.segments_.get(1).minimum = maximum;
		this.update();
	}

	/**
	 * Sets the label interval.
	 * <p>
	 * Specifies the interval for displaying labels (and ticks) on the axis. The
	 * default value is 0.0, and indicates that the interval should be computed
	 * automatically.
	 * <p>
	 * The unit for the label interval is in logical units (i.e. the same as
	 * minimum or maximum).
	 * <p>
	 */
	public void setLabelInterval(double labelInterval) {
		if (!ChartUtils.equals(this.labelInterval_, labelInterval)) {
			this.labelInterval_ = labelInterval;
			update();
		}
		;
	}

	/**
	 * Returns the label interval.
	 * <p>
	 * 
	 * @see WAxis#setLabelInterval(double labelInterval)
	 */
	public double getLabelInterval() {
		return this.labelInterval_;
	}

	/**
	 * Sets a point to be included as one of the labels (if possible).
	 * <p>
	 * The given point will be included as one of the labels, by adjusting the
	 * minimum value on the axis, if that minimum is auto-computed. This is only
	 * applicable to a Linear scale axis.
	 * <p>
	 * The default value is 0.0.
	 * <p>
	 * 
	 * @see WAxis#setRoundLimits(EnumSet locations)
	 */
	public void setLabelBasePoint(double labelBasePoint) {
		if (!ChartUtils.equals(this.labelBasePoint_, labelBasePoint)) {
			this.labelBasePoint_ = labelBasePoint;
			update();
		}
		;
	}

	/**
	 * Returns the base point for labels.
	 * <p>
	 * 
	 * @see WAxis#setLabelBasePoint(double labelBasePoint)
	 */
	public double getLabelBasePoint() {
		return this.labelBasePoint_;
	}

	/**
	 * Sets the label format.
	 * <p>
	 * Sets a format string which is used to format values, both for the axis
	 * labels as well as data series values (see {@link }).
	 * <p>
	 * For an axis with a {@link LinearScale} or {@link LogScale} scale, the
	 * format string must be a format string that is accepted by snprintf() and
	 * which formats one double. If the format string is an empty string, then
	 * {@link } is used.
	 * <p>
	 * For an axis with a {@link DateScale} scale, the format string must be a
	 * format string accepted by {@link }, to format a date. If the format string
	 * is an empty string, a suitable format is chosen based on heuristics.
	 * <p>
	 * For an axis with a {@link DateTimeScale} scale, the format string must be
	 * a format string accepted by {@link }, to format a date. If the format
	 * string is an empty string, a suitable format is chosen based on
	 * heuristics.
	 * <p>
	 * The default value is &quot;%.4g&quot; for a numeric axis, and a suitable
	 * format for date(time) scales based on a heuristic taking into account the
	 * current axis range.
	 * <p>
	 */
	public void setLabelFormat(final CharSequence format) {
		if (!ChartUtils.equals(this.labelFormat_, WString.toWString(format))) {
			this.labelFormat_ = WString.toWString(format);
			update();
		}
		;
		this.defaultLabelFormat_ = false;
	}

	/**
	 * Returns the label format string.
	 * <p>
	 * 
	 * @see WAxis#setLabelFormat(CharSequence format)
	 */
	public WString getLabelFormat() {
		switch (this.scale_) {
		case CategoryScale:
			return new WString();
		case DateScale:
		case DateTimeScale:
			if (this.defaultLabelFormat_) {
				if (!this.segments_.isEmpty()) {
					final WAxis.Segment s = this.segments_.get(0);
					return this.defaultDateTimeFormat(s);
				} else {
					return this.labelFormat_;
				}
			} else {
				return this.labelFormat_;
			}
		default:
			return this.defaultLabelFormat_ ? new WString("%.4g")
					: this.labelFormat_;
		}
	}

	/**
	 * Sets the label angle.
	 * <p>
	 * Sets the angle used for displaying the labels (in degrees). A 0 angle
	 * corresponds to horizontal text.
	 * <p>
	 * The default value is 0.0.
	 * <p>
	 */
	public void setLabelAngle(double angle) {
		if (this.renderingMirror_) {
			this.labelAngle_ = angle;
		} else {
			if (!ChartUtils.equals(this.labelAngle_, angle)) {
				this.labelAngle_ = angle;
				update();
			}
			;
		}
	}

	/**
	 * Returns the label angle.
	 * <p>
	 * 
	 * @see WAxis#setLabelAngle(double angle)
	 */
	public double getLabelAngle() {
		return this.labelAngle_;
	}

	/**
	 * Sets the title orientation.
	 * <p>
	 * Sets the orientation used for displaying the title.
	 * <p>
	 * The default value is Horizontal
	 * <p>
	 */
	public void setTitleOrientation(final Orientation orientation) {
		if (!ChartUtils.equals(this.titleOrientation_, orientation)) {
			this.titleOrientation_ = orientation;
			update();
		}
		;
	}

	/**
	 * Returns the title orientation.
	 * <p>
	 * 
	 * @see WAxis#setTitleOrientation(Orientation orientation)
	 */
	public Orientation getTitleOrientation() {
		return this.titleOrientation_;
	}

	/**
	 * Sets whether gridlines are displayed for this axis.
	 * <p>
	 * When <i>enabled</i>, gird lines are drawn for each tick on this axis,
	 * using the {@link }.
	 * <p>
	 * Unlike all other visual aspects of an axis, rendering of the gridlines is
	 * not controlled by setDisplayEnabled().
	 * <p>
	 */
	public void setGridLinesEnabled(boolean enabled) {
		if (!ChartUtils.equals(this.gridLines_, enabled)) {
			this.gridLines_ = enabled;
			update();
		}
		;
	}

	/**
	 * Returns whether gridlines are displayed for this axis.
	 * <p>
	 * 
	 * @see WAxis#setGridLinesEnabled(boolean enabled)
	 */
	public boolean isGridLinesEnabled() {
		return this.gridLines_;
	}

	/**
	 * Changes the pen used for rendering the axis and ticks.
	 * <p>
	 * The default value is a black pen of 0 width.
	 * <p>
	 */
	public void setPen(final WPen pen) {
		if (!ChartUtils.equals(this.pen_, pen)) {
			this.pen_ = pen;
			update();
		}
		;
	}

	/**
	 * Returns the pen used for rendering the axis and ticks.
	 * <p>
	 * 
	 * @see WAxis#setPen(WPen pen)
	 */
	public WPen getPen() {
		return this.pen_;
	}

	/**
	 * Changes the pen used for rendering labels for this axis.
	 * <p>
	 * The default value is a black pen of 0 width.
	 * <p>
	 * 
	 * @see WAxis#setPen(WPen pen)
	 */
	public void setTextPen(final WPen pen) {
		if (!ChartUtils.equals(this.textPen_, pen)) {
			this.textPen_ = pen;
			update();
		}
		;
	}

	/**
	 * Returns the pen used for rendering labels for this axis.
	 * <p>
	 * 
	 * @see WAxis#setTextPen(WPen pen)
	 */
	public WPen getTextPen() {
		return this.textPen_;
	}

	/**
	 * Changes the pen used for rendering the grid lines.
	 * <p>
	 * The default value is a gray pen of 0 width.
	 * <p>
	 * 
	 * @see WAxis#setPen(WPen pen)
	 */
	public void setGridLinesPen(final WPen pen) {
		if (!ChartUtils.equals(this.gridLinesPen_, pen)) {
			this.gridLinesPen_ = pen;
			update();
		}
		;
	}

	/**
	 * Returns the pen used for rendering the grid lines.
	 * <p>
	 * 
	 * @see WAxis#setGridLinesPen(WPen pen)
	 */
	public WPen getGridLinesPen() {
		return this.gridLinesPen_;
	}

	/**
	 * Sets the margin between the axis and the plot area.
	 * <p>
	 * The margin is defined in pixels.
	 * <p>
	 * The default value is 0.
	 * <p>
	 */
	public void setMargin(int pixels) {
		if (!ChartUtils.equals(this.margin_, pixels)) {
			this.margin_ = pixels;
			update();
		}
		;
	}

	/**
	 * Returns the margin between the axis and the plot area.
	 * <p>
	 * 
	 * @see WAxis#setMargin(int pixels)
	 */
	public int getMargin() {
		return this.margin_;
	}

	/**
	 * Sets the axis title.
	 * <p>
	 * The default title is empty.
	 * <p>
	 */
	public void setTitle(final CharSequence title) {
		if (!ChartUtils.equals(this.title_, WString.toWString(title))) {
			this.title_ = WString.toWString(title);
			update();
		}
		;
	}

	/**
	 * Returns the axis title.
	 * <p>
	 * 
	 * @see WAxis#setTitle(CharSequence title)
	 */
	public WString getTitle() {
		return this.title_;
	}

	/**
	 * Sets the axis title font.
	 * <p>
	 * The default title font is a 12 point Sans Serif font.
	 * <p>
	 */
	public void setTitleFont(final WFont titleFont) {
		if (!ChartUtils.equals(this.titleFont_, titleFont)) {
			this.titleFont_ = titleFont;
			update();
		}
		;
	}

	/**
	 * Returns the axis title font.
	 * <p>
	 * 
	 * @see WAxis#setTitleFont(WFont titleFont)
	 */
	public WFont getTitleFont() {
		return this.titleFont_;
	}

	/**
	 * Sets the offset from the axis for the title label.
	 */
	public void setTitleOffset(double offset) {
		this.titleOffset_ = offset;
	}

	/**
	 * Returns the title offset.
	 */
	public double getTitleOffset() {
		return this.titleOffset_;
	}

	/**
	 * Sets the axis label font.
	 * <p>
	 * The default label font is a 10 point Sans Serif font.
	 * <p>
	 */
	public void setLabelFont(final WFont labelFont) {
		if (!ChartUtils.equals(this.labelFont_, labelFont)) {
			this.labelFont_ = labelFont;
			update();
		}
		;
	}

	/**
	 * Returns the axis label font.
	 * <p>
	 * 
	 * @see WAxis#setLabelFont(WFont labelFont)
	 */
	public WFont getLabelFont() {
		return this.labelFont_;
	}

	/**
	 * Returns the label for a value.
	 * <p>
	 * This returns the label text that corresponds to a given value.
	 * <p>
	 * The default implementation uses the {@link WAxis#getLabelFormat()
	 * getLabelFormat()} to properly represent the value.
	 */
	public WString getLabel(double u) {
		String buf = null;
		WString text = new WString();
		if (this.scale_ == AxisScale.CategoryScale) {
			text = this.chart_.categoryLabel((int) u, this.axis_);
			if ((text.length() == 0)) {
				text = new WString(LocaleUtils.toString(
						LocaleUtils.getCurrentLocale(), u));
			}
		} else {
			if (this.scale_ == AxisScale.DateScale) {
				WDate d = WDate.fromJulianDay((int) u);
				WString format = this.getLabelFormat();
				return new WString(d.toString(format.toString()));
			} else {
				String format = this.getLabelFormat().toString();
				if (format.length() == 0) {
					text = new WString(LocaleUtils.toString(
							LocaleUtils.getCurrentLocale(), u));
				} else {
					buf = String.format(format, u);
					text = new WString(buf);
				}
			}
		}
		return text;
	}

	/**
	 * Set the range to zoom to on this axis.
	 * <p>
	 * The minimum is the lowest value to be displayed, and the maximum is the
	 * highest value to be displayed.
	 * <p>
	 * If the difference between minimum and maximum is less than {@link }, the
	 * zoom range will be made more narrow around the center of minimum and
	 * maximum.
	 * <p>
	 * If the given minimum is larger than the given maximum, the two values are
	 * swapped.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>This is only implemented for the X and first Y axis. It
	 * has no effect on the second Y axis. </i>
	 * </p>
	 */
	public void setZoomRange(double minimum, double maximum) {
		if (maximum < minimum) {
			double temp = maximum;
			maximum = minimum;
			minimum = temp;
		}
		if (minimum <= this.getMinimum()) {
			minimum = AUTO_MINIMUM;
		}
		if (maximum >= this.getMaximum()) {
			maximum = AUTO_MAXIMUM;
		}
		if (minimum != AUTO_MINIMUM && maximum != AUTO_MAXIMUM
				&& maximum - minimum < this.getMinimumZoomRange()) {
			minimum = (minimum + maximum) / 2.0 - this.getMinimumZoomRange()
					/ 2.0;
			maximum = (minimum + maximum) / 2.0 + this.getMinimumZoomRange()
					/ 2.0;
		}
		if (!ChartUtils.equals(this.zoomMin_, minimum)) {
			this.zoomMin_ = minimum;
			update();
		}
		;
		if (!ChartUtils.equals(this.zoomMax_, maximum)) {
			this.zoomMax_ = maximum;
			update();
		}
		;
		this.zoomRangeDirty_ = true;
	}

	/**
	 * Get the zoom range minimum for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * 
	 * @see WAxis#setZoomRange(double minimum, double maximum)
	 */
	public double getZoomMinimum() {
		double min = this.getDrawnMinimum();
		if (this.zoomMin_ <= min) {
			return min;
		}
		return this.zoomMin_;
	}

	/**
	 * Get the zoom range maximum for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * 
	 * @see WAxis#setZoomRange(double minimum, double maximum)
	 */
	public double getZoomMaximum() {
		double max = this.getDrawnMaximum();
		if (this.zoomMax_ >= max) {
			return max;
		}
		return this.zoomMax_;
	}

	/**
	 * A signal triggered when the zoom range is changed on the client side.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>If you want to use this signal, you must connect a signal
	 * listener before the chart is rendered. </i>
	 * </p>
	 */
	public Signal2<Double, Double> zoomRangeChanged() {
		return this.zoomRangeChanged_;
	}

	/**
	 * Sets the zoom level for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode. The zoom
	 * level should be &gt;= 1 and smaller than {@link }
	 * <p>
	 * <p>
	 * <i><b>Note: </b>This is only implemented for the X and first Y axis. It
	 * has no effect on the second Y axis.</i>
	 * </p>
	 * 
	 * @deprecated Use {@link WAxis#setZoomRange(double minimum, double maximum)
	 *             setZoomRange()} instead.
	 */
	public void setZoom(double zoom) {
		double min = this.getDrawnMinimum();
		double max = this.getDrawnMaximum();
		this.setZoomRange(this.getZoomMinimum(), this.getZoomMinimum()
				+ (max - min) / zoom);
	}

	/**
	 * Get the zoom level for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * 
	 * @see WAxis#setZoom(double zoom)
	 * @deprecated Use {@link WAxis#getZoomMinimum() getZoomMinimum()} and
	 *             {@link WAxis#getZoomMaximum() getZoomMaximum()} instead.
	 */
	public double getZoom() {
		if (this.zoomMin_ == AUTO_MINIMUM && this.zoomMax_ == AUTO_MAXIMUM) {
			return 1.0;
		}
		double min = this.getDrawnMinimum();
		double max = this.getDrawnMaximum();
		return (max - min) / (this.getZoomMaximum() - this.getZoomMinimum());
	}

	/**
	 * Sets the maximum zoom level for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode. The zoom
	 * level should be &gt;= 1 (1 = no zoom).
	 * <p>
	 * <p>
	 * <i><b>Note: </b>This is only implemented for the X and first Y axis. It
	 * has no effect on the second Y axis.</i>
	 * </p>
	 * 
	 * @deprecated Use {@link } instead
	 */
	public void setMaxZoom(double maxZoom) {
		if (maxZoom < 1) {
			maxZoom = 1;
		}
		if (this.minimumZoomRange_ != AUTO_MINIMUM) {
			this.setMinimumZoomRange((this.getMaximum() - this.getMinimum())
					/ maxZoom);
		}
		if (!ChartUtils.equals(this.maxZoom_, maxZoom)) {
			this.maxZoom_ = maxZoom;
			update();
		}
		;
	}

	/**
	 * Get the maximum zoom level for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * 
	 * @see WAxis#setMaxZoom(double maxZoom)
	 * @deprecated Use {@link } instead
	 */
	public double getMaxZoom() {
		double min = this.getDrawnMinimum();
		double max = this.getDrawnMaximum();
		double zoom = (max - min) / this.getMinimumZoomRange();
		if (zoom < 1.0) {
			return 1.0;
		} else {
			return (max - min) / this.getMinimumZoomRange();
		}
	}

	/**
	 * Sets the minimum zoom range for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * This range is the smallest difference there can be between
	 * {@link WAxis#getZoomMinimum() getZoomMinimum()} and
	 * {@link WAxis#getZoomMaximum() getZoomMaximum()}.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>This is only implemented for the X and first Y axis. It
	 * has no effect on the second Y axis. </i>
	 * </p>
	 */
	public void setMinimumZoomRange(double size) {
		if (!ChartUtils.equals(this.minimumZoomRange_, size)) {
			this.minimumZoomRange_ = size;
			update();
		}
		;
	}

	/**
	 * Get the minimum zoom range for this axis.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * 
	 * @see WAxis#setMinimumZoomRange(double size)
	 */
	public double getMinimumZoomRange() {
		double min = this.getDrawnMinimum();
		double max = this.getDrawnMaximum();
		if (this.minimumZoomRange_ == AUTO_MINIMUM) {
			return (max - min) / this.maxZoom_;
		} else {
			return this.minimumZoomRange_;
		}
	}

	/**
	 * Sets the value to pan to for this axis.
	 * <p>
	 * This sets the leftmost (horizontal axis) or bottom (vertical axis) value
	 * to be displayed on the chart.
	 * <p>
	 * Note that if this would cause the chart to go out of bounds, the panning
	 * of the chart will be automatically adjusted.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>This is only implemented for the X and first Y axis. It
	 * has no effect on the second Y axis.
	 * <p>
	 * If the pan position has been changed on the client side, this may not
	 * reflect the actual pan position.</i>
	 * </p>
	 * 
	 * @deprecated Use {@link WAxis#setZoomRange(double minimum, double maximum)
	 *             setZoomRange()} instead.
	 */
	public void setPan(double pan) {
		this.setZoomRange(pan,
				this.getZoomMaximum() + pan - this.getZoomMinimum());
	}

	/**
	 * Get the value to pan to for this axis, when pan is enabled on the chart.
	 * <p>
	 * Only applies to a {@link WCartesianChart} in interactive mode.
	 * <p>
	 * 
	 * @see WAxis#setPan(double pan)
	 * @deprecated Use {@link WAxis#getZoomMinimum() getZoomMinimum()} instead.
	 */
	public double getPan() {
		if (!this.isInverted()) {
			return this.getZoomMinimum();
		} else {
			return this.getZoomMaximum();
		}
	}

	/**
	 * Sets the padding between the chart area and this axis.
	 * <p>
	 */
	public void setPadding(int padding) {
		if (!ChartUtils.equals(this.padding_, padding)) {
			this.padding_ = padding;
			update();
		}
		;
	}

	/**
	 * Returns the padding between the chart area and this axis.
	 * <p>
	 * 
	 * @see WAxis#setPadding(int padding)
	 */
	public int getPadding() {
		return this.padding_;
	}

	/**
	 * Sets the direction that the axis ticks should point to.
	 * <p>
	 * If set to Outwards, the axis ticks will point outside of the chart, and
	 * the labels will be on the outside.
	 * <p>
	 * If set to Inwards, the axis ticks will point inside of the chart, and the
	 * labels will be on the inside. Also, the {@link WAxis#getPadding()
	 * getPadding()} will be set to 25.
	 * <p>
	 * 
	 * @see WAxis#setPadding(int padding)
	 */
	public void setTickDirection(TickDirection direction) {
		if (direction == TickDirection.Inwards) {
			this.setPadding(25);
		}
		if (!ChartUtils.equals(this.tickDirection_, direction)) {
			this.tickDirection_ = direction;
			update();
		}
		;
	}

	/**
	 * Gets the direction that the axis ticks point to.
	 * <p>
	 * 
	 * @see WAxis#setTickDirection(TickDirection direction)
	 */
	public TickDirection getTickDirection() {
		return this.tickDirection_;
	}

	/**
	 * Enables soft clipping of axis labels.
	 * <p>
	 * This is set to <code>false</code> by for a 3D chart and to
	 * <code>true</code> for a 2D chart.
	 * <p>
	 * This setting determines how labels should be clipped in case not the
	 * entire axis is visible due to clipping. &quot;Hard&quot; clipping is done
	 * by the paint device and may truncate labels. &quot;Soft&quot; clipping
	 * will determine if the corresponding tick is visible, and draw the label
	 * (unclipped), preventing labels from being truncated. For a 2D chart, this
	 * feature is only relevant when {@link zoom is enabled} on a
	 * {@link WCartesianChart}.
	 * <table border="1" cellspacing="3" cellpadding="3">
	 * <tr>
	 * <td><div align="center"> <img
	 * src="doc-files//WAxis-partialLabelClipping-disabled.png"
	 * alt="Soft clipping enabled (slower).">
	 * <p>
	 * <strong>Soft clipping enabled (slower).</strong>
	 * </p>
	 * </div> This is the default for {@link WCartesianChart}. The tick for 0 is
	 * visible, and the 0 is shown completely. The tick for 01/01/86 is not
	 * visible, so its label is not shown.</td>
	 * <td><div align="center"> <img
	 * src="doc-files//WAxis-partialLabelClipping-enabled.png"
	 * alt="Soft clipping disabled (faster).">
	 * <p>
	 * <strong>Soft clipping disabled (faster).</strong>
	 * </p>
	 * </div> The tick of the 0 is visible, but the 0 is shown partially. Also,
	 * the tick of 01/01/86 is not visible, but the label is partially shown.</td>
	 * </tr>
	 * </table>
	 */
	public void setSoftLabelClipping(boolean enabled) {
		if (!ChartUtils.equals(this.partialLabelClipping_, !enabled)) {
			this.partialLabelClipping_ = !enabled;
			update();
		}
		;
	}

	/**
	 * Returns whether soft label clipping is enabled.
	 * <p>
	 */
	public boolean isSoftLabelClipping() {
		return !this.partialLabelClipping_;
	}

	public int getSegmentCount() {
		return (int) this.segments_.size();
	}

	public double getSegmentMargin() {
		return this.segmentMargin_;
	}

	boolean prepareRender(Orientation orientation, double length) {
		this.fullRenderLength_ = length;
		double totalRenderRange = 0;
		for (int i = 0; i < this.segments_.size(); ++i) {
			final WAxis.Segment s = this.segments_.get(i);
			this.computeRange(s);
			totalRenderRange += s.renderMaximum - s.renderMinimum;
		}
		double clipMin = 0;
		double clipMax = 0;
		if (this.scale_ == AxisScale.CategoryScale
				|| this.scale_ == AxisScale.LogScale) {
			clipMin = clipMax = this.getPadding();
		} else {
			if (this.isInverted()) {
				clipMin = this.segments_.get(this.segments_.size() - 1).renderMaximum == 0 ? 0
						: this.getPadding();
				clipMax = this.segments_.get(0).renderMinimum == 0 ? 0 : this
						.getPadding();
			} else {
				clipMin = this.segments_.get(0).renderMinimum == 0 ? 0 : this
						.getPadding();
				clipMax = this.segments_.get(this.segments_.size() - 1).renderMaximum == 0 ? 0
						: this.getPadding();
			}
		}
		double totalRenderLength = length;
		double totalRenderStart = clipMin;
		final double SEGMENT_MARGIN = 40;
		totalRenderLength -= SEGMENT_MARGIN * (this.segments_.size() - 1)
				+ clipMin + clipMax;
		if (totalRenderLength <= 0) {
			this.renderInterval_ = 1.0;
			return false;
		}
		for (int it = 0; it < 2; ++it) {
			double rs = totalRenderStart;
			double TRR = totalRenderRange;
			totalRenderRange = 0;
			for (int i = 0; i < this.segments_.size(); ++i) {
				final WAxis.Segment s = this.segments_.get(i);
				boolean roundMinimumLimit = i == 0
						&& !EnumUtils.mask(this.roundLimits_,
								AxisValue.MinimumValue).isEmpty();
				boolean roundMaximumLimit = i == this.segments_.size() - 1
						&& !EnumUtils.mask(this.roundLimits_,
								AxisValue.MaximumValue).isEmpty();
				double diff = s.renderMaximum - s.renderMinimum;
				s.renderStart = rs;
				s.renderLength = diff / TRR * totalRenderLength;
				if (i == 0) {
					this.renderInterval_ = this.labelInterval_;
					if (this.renderInterval_ == 0) {
						if (this.scale_ == AxisScale.CategoryScale) {
							double numLabels = this.calcAutoNumLabels(
									orientation, s) / 1.5;
							int rc = this.chart_.numberOfCategories(this.axis_);
							this.renderInterval_ = Math.max(1.0,
									Math.floor(rc / numLabels));
						} else {
							if (this.scale_ == AxisScale.LogScale) {
								this.renderInterval_ = 1;
							} else {
								double numLabels = this.calcAutoNumLabels(
										orientation, s);
								this.renderInterval_ = round125(diff
										/ numLabels);
							}
						}
					}
				}
				if (this.renderInterval_ == 0) {
					this.renderInterval_ = 1;
					return false;
				}
				if (this.scale_ == AxisScale.LinearScale) {
					if (it == 0) {
						if (roundMinimumLimit) {
							s.renderMinimum = roundDown125(s.renderMinimum,
									this.renderInterval_);
							if (s.renderMinimum <= this.labelBasePoint_
									&& s.renderMaximum >= this.labelBasePoint_) {
								double interv = this.labelBasePoint_
										- s.renderMinimum;
								interv = this.renderInterval_
										* 2
										* Math.ceil(interv
												/ (this.renderInterval_ * 2));
								s.renderMinimum = this.labelBasePoint_ - interv;
							}
						}
						if (roundMaximumLimit) {
							s.renderMaximum = roundUp125(s.renderMaximum,
									this.renderInterval_);
						}
					}
				} else {
					if (this.scale_ == AxisScale.DateScale
							|| this.scale_ == AxisScale.DateTimeScale) {
						double daysInterval = 0.0;
						WDate min = null;
						WDate max = null;
						int interval;
						if (this.scale_ == AxisScale.DateScale) {
							daysInterval = this.renderInterval_;
							min = WDate.fromJulianDay((int) s.renderMinimum);
							max = WDate.fromJulianDay((int) s.renderMaximum);
						} else {
							if (this.scale_ == AxisScale.DateTimeScale) {
								daysInterval = this.renderInterval_
										/ (60.0 * 60.0 * 24);
								min = new WDate(new Date(
										(long) (long) s.renderMinimum));
								max = new WDate(new Date(
										(long) (long) s.renderMaximum));
							}
						}
						logger.debug(new StringWriter().append("Range: ")
								.append(min.toString()).append(", ")
								.append(max.toString()).toString());
						if (daysInterval > 200) {
							s.dateTimeRenderUnit = WAxis.DateTimeUnit.Years;
							interval = Math.max(1,
									(int) round125(daysInterval / 365));
							if (roundMinimumLimit) {
								if (min.getDay() != 1 && min.getMonth() != 1) {
									min = new WDate(min.getYear(), 1, 1);
								}
							}
							if (roundMaximumLimit) {
								if (max.getDay() != 1 && max.getMonth() != 1) {
									max = new WDate(max.getYear() + 1, 1, 1);
								}
							}
						} else {
							if (daysInterval > 20) {
								s.dateTimeRenderUnit = WAxis.DateTimeUnit.Months;
								double d = daysInterval / 30;
								if (d < 1.3) {
									interval = 1;
								} else {
									if (d < 2.3) {
										interval = 2;
									} else {
										if (d < 3.3) {
											interval = 3;
										} else {
											if (d < 4.3) {
												interval = 4;
											} else {
												interval = 6;
											}
										}
									}
								}
								if (roundMinimumLimit) {
									if ((min.getMonth() - 1) % interval != 0) {
										int m = roundDown(min.getMonth() - 1,
												interval) + 1;
										min = new WDate(min.getYear(), m, 1);
									} else {
										if (min.getDay() != 1) {
											min = new WDate(min.getYear(),
													min.getMonth(), 1);
										}
									}
								}
								if (roundMaximumLimit) {
									if (max.getDay() != 1) {
										max = new WDate(max.getYear(),
												max.getMonth(), 1).addMonths(1);
									}
									if ((max.getMonth() - 1) % interval != 0) {
										int m = roundDown(max.getMonth() - 1,
												interval) + 1;
										max = new WDate(max.getYear(), m, 1)
												.addMonths(interval);
									}
								}
							} else {
								if (daysInterval > 0.6) {
									s.dateTimeRenderUnit = WAxis.DateTimeUnit.Days;
									if (daysInterval < 1.3) {
										interval = 1;
										if (roundMinimumLimit) {
											min.setTime(new WTime(0, 0));
										}
										if (roundMaximumLimit) {
											if (!(max.getTime() == new WTime(0,
													0) || (max.getTime() != null && max
													.getTime().equals(
															new WTime(0, 0))))) {
												max = max.addDays(1);
											}
										}
									} else {
										interval = 7 * Math.max(1,
												(int) ((daysInterval + 5) / 7));
										if (roundMinimumLimit) {
											int dw = min.getDayOfWeek();
											min = min.addDays(-(dw - 1));
										}
										if (roundMaximumLimit) {
											int days = min.getDaysTo(max);
											if (!(max.getTime() == new WTime(0,
													0) || (max.getTime() != null && max
													.getTime().equals(
															new WTime(0, 0))))) {
												++days;
											}
											days = roundUp(days, interval);
											max = min.addDays(days);
										}
									}
								} else {
									double minutes = daysInterval * 24 * 60;
									if (minutes > 40) {
										s.dateTimeRenderUnit = WAxis.DateTimeUnit.Hours;
										double d = minutes / 60;
										if (d < 1.3) {
											interval = 1;
										} else {
											if (d < 2.3) {
												interval = 2;
											} else {
												if (d < 3.3) {
													interval = 3;
												} else {
													if (d < 4.3) {
														interval = 4;
													} else {
														if (d < 6.3) {
															interval = 6;
														} else {
															interval = 12;
														}
													}
												}
											}
										}
										if (roundMinimumLimit) {
											if (min.getTime().getHour()
													% interval != 0) {
												int h = roundDown(min.getTime()
														.getHour(), interval);
												min.setTime(new WTime(h, 0));
											} else {
												if (min.getTime().getMinute() != 0) {
													min.setTime(new WTime(min
															.getTime()
															.getHour(), 0));
												}
											}
										}
										if (roundMaximumLimit) {
											if (max.getTime().getMinute() != 0) {
												max.setTime(new WTime(max
														.getTime().getHour(), 0));
												max = max.addSeconds(60 * 60);
											}
											if (max.getTime().getHour()
													% interval != 0) {
												int h = roundDown(max.getTime()
														.getHour(), interval);
												max.setTime(new WTime(h, 0));
												max = max
														.addSeconds(interval * 60 * 60);
											}
										}
									} else {
										if (minutes > 0.8) {
											s.dateTimeRenderUnit = WAxis.DateTimeUnit.Minutes;
											if (minutes < 1.3) {
												interval = 1;
											} else {
												if (minutes < 2.3) {
													interval = 2;
												} else {
													if (minutes < 5.3) {
														interval = 5;
													} else {
														if (minutes < 10.3) {
															interval = 10;
														} else {
															if (minutes < 15.3) {
																interval = 15;
															} else {
																if (minutes < 20.3) {
																	interval = 20;
																} else {
																	interval = 30;
																}
															}
														}
													}
												}
											}
											if (roundMinimumLimit) {
												if (min.getTime().getMinute()
														% interval != 0) {
													int m = roundDown(min
															.getTime()
															.getMinute(),
															interval);
													min.setTime(new WTime(min
															.getTime()
															.getHour(), m));
												} else {
													if (min.getTime()
															.getSecond() != 0) {
														min.setTime(new WTime(
																min.getTime()
																		.getHour(),
																min.getTime()
																		.getMinute()));
													}
												}
											}
											if (roundMaximumLimit) {
												if (max.getTime().getSecond() != 0) {
													max.setTime(new WTime(max
															.getTime()
															.getHour(), max
															.getTime()
															.getMinute()));
													max = max.addSeconds(60);
												}
												if (max.getTime().getMinute()
														% interval != 0) {
													int m = roundDown(max
															.getTime()
															.getMinute(),
															interval);
													max.setTime(new WTime(max
															.getTime()
															.getHour(), m));
													max = max
															.addSeconds(interval * 60);
												}
											}
										} else {
											s.dateTimeRenderUnit = WAxis.DateTimeUnit.Seconds;
											double seconds = minutes * 60;
											if (seconds < 1.3) {
												interval = 1;
											} else {
												if (seconds < 2.3) {
													interval = 2;
												} else {
													if (seconds < 5.3) {
														interval = 5;
													} else {
														if (seconds < 10.3) {
															interval = 10;
														} else {
															if (seconds < 15.3) {
																interval = 15;
															} else {
																if (seconds < 20.3) {
																	interval = 20;
																} else {
																	interval = 30;
																}
															}
														}
													}
												}
											}
											if (roundMinimumLimit) {
												if (min.getTime().getSecond()
														% interval != 0) {
													int sec = roundDown(min
															.getTime()
															.getSecond(),
															interval);
													min.setTime(new WTime(min
															.getTime()
															.getHour(), min
															.getTime()
															.getMinute(), sec));
												} else {
													if (min.getTime().getMsec() != 0) {
														min.setTime(new WTime(
																min.getTime()
																		.getHour(),
																min.getTime()
																		.getMinute(),
																min.getTime()
																		.getSecond()));
													}
												}
											}
											if (roundMaximumLimit) {
												if (max.getTime().getMsec() != 0) {
													max.setTime(new WTime(max
															.getTime()
															.getHour(), max
															.getTime()
															.getMinute(), max
															.getTime()
															.getSecond()));
													max = max.addSeconds(1);
												}
												if (max.getTime().getSecond()
														% interval != 0) {
													int sec = roundDown(max
															.getTime()
															.getSecond(),
															interval);
													max.setTime(new WTime(max
															.getTime()
															.getHour(), max
															.getTime()
															.getMinute(), sec));
													max = max
															.addSeconds(interval);
												}
											}
										}
									}
								}
							}
						}
						s.dateTimeRenderInterval = interval;
						if (this.scale_ == AxisScale.DateScale) {
							s.renderMinimum = min.toJulianDay();
							s.renderMaximum = max.toJulianDay();
						} else {
							if (this.scale_ == AxisScale.DateTimeScale) {
								s.renderMinimum = min.getDate().getTime();
								s.renderMaximum = max.getDate().getTime();
							}
						}
					}
				}
				totalRenderRange += s.renderMaximum - s.renderMinimum;
				rs += s.renderLength + SEGMENT_MARGIN;
			}
		}
		return true;
	}

	public void render(final WPainter painter,
			EnumSet<AxisProperty> properties, final WPointF axisStart,
			final WPointF axisEnd, double tickStart, double tickEnd,
			double labelPos, EnumSet<AlignmentFlag> labelFlags,
			final WTransform transform, AxisValue side) {
		List<WPen> pens = new ArrayList<WPen>();
		List<WPen> textPens = new ArrayList<WPen>();
		this.render(painter, properties, axisStart, axisEnd, tickStart,
				tickEnd, labelPos, labelFlags, transform, side, pens, textPens);
	}

	public final void render(final WPainter painter,
			EnumSet<AxisProperty> properties, final WPointF axisStart,
			final WPointF axisEnd, double tickStart, double tickEnd,
			double labelPos, EnumSet<AlignmentFlag> labelFlags) {
		render(painter, properties, axisStart, axisEnd, tickStart, tickEnd,
				labelPos, labelFlags, new WTransform(), AxisValue.MinimumValue);
	}

	public final void render(final WPainter painter,
			EnumSet<AxisProperty> properties, final WPointF axisStart,
			final WPointF axisEnd, double tickStart, double tickEnd,
			double labelPos, EnumSet<AlignmentFlag> labelFlags,
			final WTransform transform) {
		render(painter, properties, axisStart, axisEnd, tickStart, tickEnd,
				labelPos, labelFlags, transform, AxisValue.MinimumValue);
	}

	public void render(final WPainter painter,
			EnumSet<AxisProperty> properties, final WPointF axisStart,
			final WPointF axisEnd, double tickStart, double tickEnd,
			double labelPos, EnumSet<AlignmentFlag> labelFlags,
			final WTransform transform, AxisValue side, List<WPen> pens,
			List<WPen> textPens) {
		WFont oldFont1 = painter.getFont();
		painter.setFont(this.labelFont_);
		boolean vertical = axisStart.getX() == axisEnd.getX();
		WPointF axStart = new WPointF();
		WPointF axEnd = new WPointF();
		if (this.isInverted()) {
			axStart = axisEnd;
			axEnd = axisStart;
		} else {
			axStart = axisStart;
			axEnd = axisEnd;
		}
		for (int segment = 0; segment < this.getSegmentCount(); ++segment) {
			final WAxis.Segment s = this.segments_.get(segment);
			if (!EnumUtils.mask(properties, AxisProperty.Line).isEmpty()) {
				painter.setPen(this.getPen().clone());
				WPointF begin = interpolate(axisStart, axisEnd,
						this.mapToDevice(s.renderMinimum, segment));
				WPointF end = interpolate(axisStart, axisEnd,
						this.mapToDevice(s.renderMaximum, segment));
				{
					WPainterPath path = new WPainterPath();
					path.moveTo(begin);
					path.lineTo(end);
					painter.drawPath(transform.map(path).getCrisp());
				}
				boolean rotate = vertical;
				if (segment != 0) {
					painter.save();
					painter.translate(begin);
					if (rotate) {
						painter.rotate(90);
					}
					painter.drawPath(new TildeStartMarker(
							(int) this.segmentMargin_));
					painter.restore();
				}
				if (segment != this.getSegmentCount() - 1) {
					painter.save();
					painter.translate(end);
					if (rotate) {
						painter.rotate(90);
					}
					painter.drawPath(new TildeEndMarker(
							(int) this.segmentMargin_));
					painter.restore();
				}
			}
			if (pens.isEmpty()) {
				pens.add(this.getPen());
				textPens.add(this.getTextPen());
			}
			for (int level = 1; level <= pens.size(); ++level) {
				WPainterPath ticksPath = new WPainterPath();
				List<WAxis.TickLabel> ticks = new ArrayList<WAxis.TickLabel>();
				AxisConfig cfg = new AxisConfig();
				cfg.zoomLevel = level;
				cfg.side = side;
				this.getLabelTicks(ticks, segment, cfg);
				for (int i = 0; i < ticks.size(); ++i) {
					double u = this.mapToDevice(ticks.get(i).u, segment);
					WPointF p = interpolate(axisStart, axisEnd, u);
					if (!EnumUtils.mask(properties, AxisProperty.Line)
							.isEmpty()
							&& ticks.get(i).tickLength != WAxis.TickLabel.TickLength.Zero) {
						double ts = tickStart;
						double te = tickEnd;
						if (ticks.get(i).tickLength == WAxis.TickLabel.TickLength.Short) {
							ts = tickStart / 2;
							te = tickEnd / 2;
						}
						if (vertical) {
							ticksPath.moveTo(new WPointF(p.getX() + ts, p
									.getY()));
							ticksPath.lineTo(new WPointF(p.getX() + te, p
									.getY()));
						} else {
							ticksPath.moveTo(new WPointF(p.getX(), p.getY()
									+ ts));
							ticksPath.lineTo(new WPointF(p.getX(), p.getY()
									+ te));
						}
					}
					if (!EnumUtils.mask(properties, AxisProperty.Labels)
							.isEmpty() && !(ticks.get(i).label.length() == 0)) {
						WPointF labelP = new WPointF();
						if (vertical) {
							labelP = new WPointF(p.getX() + labelPos, p.getY());
						} else {
							labelP = new WPointF(p.getX(), p.getY() + labelPos);
						}
						this.renderLabel(painter, ticks.get(i).label, labelP,
								labelFlags, this.getLabelAngle(), 3, transform,
								textPens.get(level - 1));
					}
				}
				if (!ticksPath.isEmpty()) {
					painter.strokePath(transform.map(ticksPath).getCrisp(),
							pens.get(level - 1));
				}
			}
		}
		painter.setFont(oldFont1);
	}

	public List<Double> gridLinePositions(AxisConfig config) {
		List<Double> pos = new ArrayList<Double>();
		for (int segment = 0; segment < this.segments_.size(); ++segment) {
			List<WAxis.TickLabel> ticks = new ArrayList<WAxis.TickLabel>();
			this.getLabelTicks(ticks, segment, config);
			for (int i = 0; i < ticks.size(); ++i) {
				if (ticks.get(i).tickLength == WAxis.TickLabel.TickLength.Long) {
					pos.add(this.mapToDevice(ticks.get(i).u, segment));
				}
			}
		}
		return pos;
	}

	/**
	 * Set whether this axis should be inverted.
	 * <p>
	 * When inverted, the axis will be drawn in the opposite direction, e.g. if
	 * normally, the low values are on the left and high values on the right,
	 * when inverted, the low values will be on the right and high values on the
	 * left.
	 */
	public void setInverted(boolean inverted) {
		if (!ChartUtils.equals(this.inverted_, inverted)) {
			this.inverted_ = inverted;
			update();
		}
		;
	}

	/**
	 * Set whether this axis should be inverted.
	 * <p>
	 * Calls {@link #setInverted(boolean inverted) setInverted(true)}
	 */
	public final void setInverted() {
		setInverted(true);
	}

	/**
	 * Get whether this axis is inverted.
	 * <p>
	 * 
	 * @see WAxis#setInverted(boolean inverted)
	 */
	public boolean isInverted() {
		return this.inverted_;
	}

	/**
	 * A label transform function.
	 * <p>
	 * 
	 * The label transform is a function from double to double.
	 * <p>
	 */
	public static interface LabelTransform {
		/**
		 * Apply the label transform.
		 */
		public double apply(double d);
	}

	static class IdentityLabelTransform implements WAxis.LabelTransform {
		private static Logger logger = LoggerFactory
				.getLogger(IdentityLabelTransform.class);

		public double apply(double d) {
			return d;
		}
	}

	/**
	 * Set the transform function to apply to a given side.
	 * <p>
	 * The label transform must be a function from double to double, and will be
	 * applied on the double value of the model coordinate of every axis tick.
	 * <p>
	 * The label transform will not move the position of the axis ticks, only
	 * change the labels displayed at the ticks.
	 * <p>
	 * This can be useful in combination with a {@link WAxis#getLocation()
	 * getLocation()} set to BothSides, to show different labels on each side.
	 * <p>
	 * If DateScale or DateTimeScale are used, the double value will be in
	 * seconds since the Epoch (00:00:00 UTC, January 1, 1970).
	 * <p>
	 * Only MinimumValue, ZeroValue and MaximumValue are accepted for side. If
	 * you set a label transform for another side, the label transform will not
	 * be used.
	 * <p>
	 * The label transform will not be used if the {@link WAxis#getScale()
	 * getScale()} is CategoryScale.
	 */
	public void setLabelTransform(final WAxis.LabelTransform transform,
			AxisValue side) {
		this.labelTransforms_.put(side, transform);
		this.update();
	}

	/**
	 * Get the label transform configured for the given side.
	 * <p>
	 * If no transform is configured for the given side, the identity function
	 * is returned.
	 * <p>
	 * 
	 * @see WAxis#setLabelTransform(WAxis.LabelTransform transform, AxisValue
	 *      side)
	 */
	public WAxis.LabelTransform getLabelTransform(AxisValue side) {
		WAxis.LabelTransform it = this.labelTransforms_.get(side);
		if (it != null) {
			return it;
		} else {
			return new WAxis.IdentityLabelTransform();
		}
	}

	public void renderLabel(final WPainter painter, final CharSequence text,
			final WPointF p, EnumSet<AlignmentFlag> flags, double angle,
			int margin, WTransform transform, final WPen pen) {
		AlignmentFlag horizontalAlign = EnumUtils.enumFromSet(EnumUtils.mask(
				flags, AlignmentFlag.AlignHorizontalMask));
		AlignmentFlag verticalAlign = EnumUtils.enumFromSet(EnumUtils.mask(
				flags, AlignmentFlag.AlignVerticalMask));
		double width = 1000;
		double height = 20;
		WPointF pos = p;
		double left = pos.getX();
		double top = pos.getY();
		switch (horizontalAlign) {
		case AlignLeft:
			left += margin;
			break;
		case AlignCenter:
			left -= width / 2;
			break;
		case AlignRight:
			left -= width + margin;
		default:
			break;
		}
		switch (verticalAlign) {
		case AlignTop:
			top += margin;
			break;
		case AlignMiddle:
			top -= height / 2;
			break;
		case AlignBottom:
			top -= height + margin;
			break;
		default:
			break;
		}
		WPen oldPen = painter.getPen().clone();
		painter.setPen(pen.clone());
		List<WString> splitText = splitLabel(text);
		boolean clipping = painter.hasClipping();
		if (!this.partialLabelClipping_ && clipping
				&& this.getTickDirection() == TickDirection.Outwards
				&& this.getLocation() != AxisValue.ZeroValue) {
			painter.setClipping(false);
		}
		WPointF transformedPoint = transform.map(pos);
		if (angle == 0) {
			for (int i = 0; i < splitText.size(); ++i) {
				double yOffset = calcYOffset(i, splitText.size(), height,
						EnumSet.of(verticalAlign));
				WTransform offsetTransform = new WTransform(1, 0, 0, 1, 0,
						yOffset);
				painter.drawText(
						offsetTransform.multiply(transform).map(
								new WRectF(left, top, width, height)),
						EnumSet.of(horizontalAlign, verticalAlign),
						TextFlag.TextSingleLine,
						splitText.get(i),
						clipping && !this.partialLabelClipping_ ? transformedPoint
								: null);
			}
		} else {
			painter.save();
			painter.translate(transform.map(pos));
			painter.rotate(-angle);
			transformedPoint = painter.getWorldTransform().getInverted()
					.map(transformedPoint);
			for (int i = 0; i < splitText.size(); ++i) {
				double yOffset = calcYOffset(i, splitText.size(), height,
						EnumSet.of(verticalAlign));
				painter.drawText(
						new WRectF(left - pos.getX(), top - pos.getY()
								+ yOffset, width, height),
						EnumSet.of(horizontalAlign, verticalAlign),
						TextFlag.TextSingleLine,
						splitText.get(i),
						clipping && !this.partialLabelClipping_ ? transformedPoint
								: null);
			}
			painter.restore();
		}
		painter.setClipping(clipping);
		painter.setPen(oldPen);
	}

	void setRenderMirror(boolean enable) {
		this.renderingMirror_ = enable;
	}

	public double calcTitleSize(WPaintDevice d, Orientation orientation) {
		WMeasurePaintDevice device = new WMeasurePaintDevice(d);
		WPainter painter = new WPainter(device);
		painter.setFont(this.titleFont_);
		painter.drawText(0, 0, 100, 100, EnumSet.of(AlignmentFlag.AlignCenter),
				this.getTitle());
		return orientation == Orientation.Vertical ? device.getBoundingRect()
				.getHeight() : device.getBoundingRect().getWidth();
	}

	public double calcMaxTickLabelSize(WPaintDevice d, Orientation orientation) {
		WMeasurePaintDevice device = new WMeasurePaintDevice(d);
		WPainter painter = new WPainter(device);
		painter.setFont(this.labelFont_);
		List<WAxis.TickLabel> ticks = new ArrayList<WAxis.TickLabel>();
		for (int i = 0; i < this.getSegmentCount(); ++i) {
			AxisConfig cfg = new AxisConfig();
			cfg.zoomLevel = 1;
			if (this.getLocation() == AxisValue.MinimumValue
					|| this.getLocation() == AxisValue.BothSides) {
				cfg.side = AxisValue.MinimumValue;
				this.getLabelTicks(ticks, i, cfg);
			}
			if (this.getLocation() == AxisValue.MaximumValue
					|| this.getLocation() == AxisValue.BothSides) {
				cfg.side = AxisValue.MaximumValue;
				this.getLabelTicks(ticks, i, cfg);
			}
			if (this.getLocation() == AxisValue.ZeroValue) {
				cfg.side = AxisValue.ZeroValue;
				this.getLabelTicks(ticks, i, cfg);
			}
		}
		painter.rotate(-this.labelAngle_);
		for (int i = 0; i < ticks.size(); ++i) {
			painter.drawText(0, 0, 100, 100,
					EnumSet.of(AlignmentFlag.AlignRight), ticks.get(i).label);
		}
		return orientation == Orientation.Vertical ? device.getBoundingRect()
				.getHeight() : device.getBoundingRect().getWidth();
	}

	/**
	 * Represents a Date time unit.
	 */
	protected enum DateTimeUnit {
		Seconds, Minutes, Hours, Days, Months, Years;

		/**
		 * Returns the numerical representation of this enum.
		 */
		public int getValue() {
			return ordinal();
		}
	}

	/**
	 * Represents a label/tick on the axis.
	 */
	static class TickLabel {
		private static Logger logger = LoggerFactory.getLogger(TickLabel.class);

		/**
		 * Enumeration for a tick type.
		 */
		public enum TickLength {
			Zero, Short, Long;

			/**
			 * Returns the numerical representation of this enum.
			 */
			public int getValue() {
				return ordinal();
			}
		}

		/**
		 * Position on the axis.
		 */
		public double u;
		/**
		 * Tick length.
		 */
		public WAxis.TickLabel.TickLength tickLength;
		/**
		 * Label text.
		 */
		public WString label;

		/**
		 * Creates a label tick.
		 */
		public TickLabel(double v, WAxis.TickLabel.TickLength length,
				final CharSequence l) {
			this.u = v;
			this.tickLength = length;
			this.label = WString.toWString(l);
		}

		/**
		 * Creates a label tick.
		 * <p>
		 * Calls
		 * {@link #TickLabel(double v, WAxis.TickLabel.TickLength length, CharSequence l)
		 * this(v, length, new WString())}
		 */
		public TickLabel(double v, WAxis.TickLabel.TickLength length) {
			this(v, length, new WString());
		}
	}

	WAxis() {
		this.chart_ = null;
		this.axis_ = Axis.XAxis;
		this.visible_ = true;
		this.location_ = AxisValue.MinimumValue;
		this.scale_ = AxisScale.LinearScale;
		this.resolution_ = 0.0;
		this.labelInterval_ = 0;
		this.labelBasePoint_ = 0;
		this.labelFormat_ = new WString();
		this.defaultLabelFormat_ = true;
		this.gridLines_ = false;
		this.pen_ = new WPen();
		this.gridLinesPen_ = new WPen(WColor.gray);
		this.margin_ = 0;
		this.labelAngle_ = 0;
		this.title_ = new WString();
		this.titleFont_ = new WFont();
		this.labelFont_ = new WFont();
		this.roundLimits_ = EnumSet.of(AxisValue.MinimumValue,
				AxisValue.MaximumValue);
		this.segmentMargin_ = 40;
		this.titleOffset_ = 0;
		this.textPen_ = new WPen(WColor.black);
		this.titleOrientation_ = Orientation.Horizontal;
		this.maxZoom_ = 4.0;
		this.minimumZoomRange_ = AUTO_MINIMUM;
		this.zoomMin_ = AUTO_MINIMUM;
		this.zoomMax_ = AUTO_MAXIMUM;
		this.zoomRangeDirty_ = true;
		this.padding_ = 0;
		this.tickDirection_ = TickDirection.Outwards;
		this.partialLabelClipping_ = true;
		this.inverted_ = false;
		this.labelTransforms_ = new HashMap<AxisValue, WAxis.LabelTransform>();
		this.zoomRangeChanged_ = new Signal2<Double, Double>();
		this.segments_ = new ArrayList<WAxis.Segment>();
		this.titleFont_.setFamily(WFont.GenericFamily.SansSerif, "Arial");
		this.titleFont_.setSize(WFont.Size.FixedSize, new WLength(12,
				WLength.Unit.Point));
		this.labelFont_.setFamily(WFont.GenericFamily.SansSerif, "Arial");
		this.labelFont_.setSize(WFont.Size.FixedSize, new WLength(10,
				WLength.Unit.Point));
		this.segments_.add(new WAxis.Segment());
	}

	/**
	 * Returns the label (and ticks) information for this axis.
	 */
	protected void getLabelTicks(final List<WAxis.TickLabel> ticks,
			int segment, AxisConfig config) {
		double divisor = Math.pow(2.0, config.zoomLevel - 1);
		final WAxis.Segment s = this.segments_.get(segment);
		switch (this.scale_) {
		case CategoryScale: {
			int renderInterval = Math.max(1, (int) this.renderInterval_);
			if (renderInterval == 1) {
				ticks.add(new WAxis.TickLabel(s.renderMinimum,
						WAxis.TickLabel.TickLength.Long));
				for (int i = (int) (s.renderMinimum + 0.5); i < s.renderMaximum; ++i) {
					ticks.add(new WAxis.TickLabel(i + 0.5,
							WAxis.TickLabel.TickLength.Long));
					ticks.add(new WAxis.TickLabel(i,
							WAxis.TickLabel.TickLength.Zero, this
									.getLabel((double) i)));
				}
			} else {
				for (int i = (int) s.renderMinimum; i < s.renderMaximum; i += renderInterval) {
					ticks.add(new WAxis.TickLabel(i,
							WAxis.TickLabel.TickLength.Long, this
									.getLabel((double) i)));
				}
			}
			break;
		}
		case LinearScale: {
			double interval = this.renderInterval_ / divisor;
			for (int i = 0;; ++i) {
				double v = s.renderMinimum + interval * i;
				if (v - s.renderMaximum > EPSILON * interval) {
					break;
				}
				WString t = new WString();
				if (i % 2 == 0) {
					if (this.hasLabelTransformOnSide(config.side)) {
						t = this.getLabel(this.getLabelTransform(config.side)
								.apply(v));
					} else {
						t = this.getLabel(v);
					}
				}
				ticks.add(new WAxis.TickLabel(v,
						i % 2 == 0 ? WAxis.TickLabel.TickLength.Long
								: WAxis.TickLabel.TickLength.Short, t));
			}
			break;
		}
		case LogScale: {
			double v = s.renderMinimum > 0 ? s.renderMinimum : 0.0001;
			double p = v;
			int i = 0;
			for (;; ++i) {
				if (v - s.renderMaximum > EPSILON * s.renderMaximum) {
					break;
				}
				if (i == 9) {
					v = p = 10 * p;
					i = 0;
				}
				if (i == 0) {
					WString text = this.getLabel(v);
					if (this.hasLabelTransformOnSide(config.side)) {
						text = this.getLabel(this
								.getLabelTransform(config.side).apply(v));
					}
					ticks.add(new WAxis.TickLabel(v,
							WAxis.TickLabel.TickLength.Long, text));
				} else {
					ticks.add(new WAxis.TickLabel(v,
							WAxis.TickLabel.TickLength.Short));
				}
				v += p;
			}
			break;
		}
		case DateTimeScale:
		case DateScale: {
			WString format = this.getLabelFormat();
			WDate dt = null;
			if (this.scale_ == AxisScale.DateScale) {
				dt = WDate.fromJulianDay((int) s.renderMinimum);
				if (!(dt != null)) {
					String exception = "Invalid julian day: "
							+ String.valueOf(s.renderMinimum);
					throw new WException(exception);
				}
			} else {
				dt = new WDate(new Date((long) (long) s.renderMinimum));
			}
			WAxis.DateTimeUnit unit;
			int interval;
			if (config.zoomLevel == 1) {
				unit = s.dateTimeRenderUnit;
				interval = s.dateTimeRenderInterval;
			} else {
				double daysInterval = 0.0;
				if (this.scale_ == AxisScale.DateScale) {
					daysInterval = this.renderInterval_;
				} else {
					daysInterval = this.renderInterval_ / (60.0 * 60.0 * 24);
				}
				daysInterval /= divisor;
				if (daysInterval > 200) {
					unit = WAxis.DateTimeUnit.Years;
					interval = Math.max(1, (int) round125(daysInterval / 365));
				} else {
					if (daysInterval > 20) {
						unit = WAxis.DateTimeUnit.Months;
						double d = daysInterval / 30;
						if (d < 1.3) {
							interval = 1;
						} else {
							if (d < 2.3) {
								interval = 2;
							} else {
								if (d < 3.3) {
									interval = 3;
								} else {
									if (d < 4.3) {
										interval = 4;
									} else {
										interval = 6;
									}
								}
							}
						}
					} else {
						if (daysInterval > 0.6) {
							unit = WAxis.DateTimeUnit.Days;
							if (daysInterval < 1.3) {
								interval = 1;
							} else {
								interval = 7 * Math.max(1,
										(int) ((daysInterval + 5) / 7));
							}
						} else {
							double minutes = daysInterval * 24 * 60;
							if (minutes > 40) {
								unit = WAxis.DateTimeUnit.Hours;
								double d = minutes / 60;
								if (d < 1.3) {
									interval = 1;
								} else {
									if (d < 2.3) {
										interval = 2;
									} else {
										if (d < 3.3) {
											interval = 3;
										} else {
											if (d < 4.3) {
												interval = 4;
											} else {
												if (d < 6.3) {
													interval = 6;
												} else {
													interval = 12;
												}
											}
										}
									}
								}
							} else {
								if (minutes > 0.8) {
									unit = WAxis.DateTimeUnit.Minutes;
									if (minutes < 1.3) {
										interval = 1;
									} else {
										if (minutes < 2.3) {
											interval = 2;
										} else {
											if (minutes < 5.3) {
												interval = 5;
											} else {
												if (minutes < 10.3) {
													interval = 10;
												} else {
													if (minutes < 15.3) {
														interval = 15;
													} else {
														if (minutes < 20.3) {
															interval = 20;
														} else {
															interval = 30;
														}
													}
												}
											}
										}
									}
								} else {
									unit = WAxis.DateTimeUnit.Seconds;
									double seconds = minutes * 60;
									if (seconds < 1.3) {
										interval = 1;
									} else {
										if (seconds < 2.3) {
											interval = 2;
										} else {
											if (seconds < 5.3) {
												interval = 5;
											} else {
												if (seconds < 10.3) {
													interval = 10;
												} else {
													if (seconds < 15.3) {
														interval = 15;
													} else {
														if (seconds < 20.3) {
															interval = 20;
														} else {
															interval = 30;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			boolean atTick = interval > 1
					|| unit.getValue() <= WAxis.DateTimeUnit.Days.getValue()
					|| !!EnumUtils.mask(this.roundLimits_,
							AxisValue.MinimumValue).isEmpty();
			for (;;) {
				long dl = this.getDateNumber(dt);
				if (dl > s.renderMaximum) {
					break;
				}
				WDate next = null;
				switch (unit) {
				case Years:
					next = dt.addYears(interval);
					break;
				case Months:
					next = dt.addMonths(interval);
					break;
				case Days:
					next = dt.addDays(interval);
					break;
				case Hours:
					next = dt.addSeconds(interval * 60 * 60);
					break;
				case Minutes:
					next = dt.addSeconds(interval * 60);
					break;
				case Seconds:
					next = dt.addSeconds(interval);
					break;
				}
				WString text = new WString();
				{
					WDate transformedDt = dt;
					if (this.hasLabelTransformOnSide(config.side)) {
						transformedDt = new WDate(new Date((long) (long) this
								.getLabelTransform(config.side).apply(
										(double) dt.getDate().getTime())));
					}
					text = new WString(
							transformedDt.toString(format.toString()));
				}
				if (dl >= s.renderMinimum) {
					ticks.add(new WAxis.TickLabel((double) dl,
							WAxis.TickLabel.TickLength.Long, atTick ? text
									: new WString()));
				}
				if (!atTick) {
					double tl = (this.getDateNumber(next) + dl) / 2;
					if (tl >= s.renderMinimum && tl <= s.renderMaximum) {
						ticks.add(new WAxis.TickLabel((double) tl,
								WAxis.TickLabel.TickLength.Zero, text));
					}
				}
				dt = next;
			}
			break;
		}
		}
	}

	/**
	 * Returns the Date format.
	 */
	protected WString autoDateFormat(final WDate dt, WAxis.DateTimeUnit unit,
			boolean atTick) {
		if (atTick) {
			switch (unit) {
			case Months:
			case Years:
			case Days:
				if (dt.getTime().getSecond() != 0) {
					return new WString("dd/MM/yy hh:mm:ss");
				} else {
					if (dt.getTime().getHour() != 0) {
						return new WString("dd/MM/yy hh:mm");
					} else {
						return new WString("dd/MM/yy");
					}
				}
			case Hours:
				if (dt.getTime().getSecond() != 0) {
					return new WString("dd/MM hh:mm:ss");
				} else {
					if (dt.getTime().getMinute() != 0) {
						return new WString("dd/MM hh:mm");
					} else {
						return new WString("h'h' dd/MM");
					}
				}
			case Minutes:
				if (dt.getTime().getSecond() != 0) {
					return new WString("hh:mm:ss");
				} else {
					return new WString("hh:mm");
				}
			case Seconds:
				return new WString("hh:mm:ss");
			}
		} else {
			switch (unit) {
			case Years:
				return new WString("yyyy");
			case Months:
				return new WString("MMM yy");
			case Days:
				return new WString("dd/MM/yy");
			case Hours:
				return new WString("h'h' dd/MM");
			case Minutes:
				return new WString("hh:mm");
			case Seconds:
				return new WString("hh:mm:ss");
			default:
				break;
			}
		}
		return WString.Empty;
	}

	private WAbstractChartImplementation chart_;
	private Axis axis_;
	private boolean visible_;
	private AxisValue location_;
	private AxisScale scale_;
	private double resolution_;
	private double labelInterval_;
	private double labelBasePoint_;
	private WString labelFormat_;
	private boolean defaultLabelFormat_;
	private boolean gridLines_;
	private WPen pen_;
	private WPen gridLinesPen_;
	private int margin_;
	private double labelAngle_;
	private WString title_;
	private WFont titleFont_;
	private WFont labelFont_;
	private EnumSet<AxisValue> roundLimits_;
	private double segmentMargin_;
	private double titleOffset_;
	private WPen textPen_;
	private Orientation titleOrientation_;
	private double maxZoom_;
	private double minimumZoomRange_;
	double zoomMin_;
	double zoomMax_;
	boolean zoomRangeDirty_;
	private int padding_;
	private TickDirection tickDirection_;
	private boolean partialLabelClipping_;
	private boolean inverted_;
	private HashMap<AxisValue, WAxis.LabelTransform> labelTransforms_;
	private boolean renderingMirror_;
	private Signal2<Double, Double> zoomRangeChanged_;

	static class Segment {
		private static Logger logger = LoggerFactory.getLogger(Segment.class);

		public double minimum;
		public double maximum;
		public double renderMinimum;
		public double renderMaximum;
		public double renderLength;
		public double renderStart;
		public WAxis.DateTimeUnit dateTimeRenderUnit;
		public int dateTimeRenderInterval;

		public Segment() {
			this.minimum = AUTO_MINIMUM;
			this.maximum = AUTO_MAXIMUM;
			this.renderMinimum = AUTO_MINIMUM;
			this.renderMaximum = AUTO_MAXIMUM;
			this.renderLength = AUTO_MAXIMUM;
			this.renderStart = AUTO_MAXIMUM;
			this.dateTimeRenderUnit = WAxis.DateTimeUnit.Days;
			this.dateTimeRenderInterval = 0;
		}

		public Segment(final WAxis.Segment other) {
			this.minimum = other.minimum;
			this.maximum = other.maximum;
			this.renderMinimum = other.renderMinimum;
			this.renderMaximum = other.renderMaximum;
			this.renderLength = other.renderLength;
			this.renderStart = other.renderStart;
			this.dateTimeRenderUnit = other.dateTimeRenderUnit;
			this.dateTimeRenderInterval = other.dateTimeRenderInterval;
		}
	}

	List<WAxis.Segment> segments_;
	double renderInterval_;
	double fullRenderLength_;

	void init(WAbstractChartImplementation chart, Axis axis) {
		this.chart_ = chart;
		this.axis_ = axis;
		if (axis == Axis.XAxis || this.axis_ == Axis.XAxis_3D
				|| this.axis_ == Axis.YAxis_3D) {
			if (this.chart_.getChartType() == ChartType.CategoryChart) {
				this.scale_ = AxisScale.CategoryScale;
			} else {
				if (this.scale_ == AxisScale.CategoryScale) {
					this.scale_ = AxisScale.LinearScale;
				}
			}
		}
		if (axis == Axis.Y2Axis) {
			this.visible_ = false;
		}
	}

	private void update() {
		if (this.chart_ != null) {
			this.chart_.update();
		}
	}

	// private boolean (final T m, final T v) ;
	private void computeRange(final WAxis.Segment segment) {
		segment.renderMinimum = segment.minimum;
		segment.renderMaximum = segment.maximum;
		final boolean findMinimum = segment.renderMinimum == AUTO_MINIMUM;
		final boolean findMaximum = segment.renderMaximum == AUTO_MAXIMUM;
		if (this.scale_ == AxisScale.CategoryScale) {
			int rc = this.chart_.numberOfCategories(this.axis_);
			rc = Math.max(1, rc);
			if (findMinimum) {
				segment.renderMinimum = -0.5;
			}
			if (findMaximum) {
				segment.renderMaximum = rc - 0.5;
			}
		} else {
			if (findMinimum || findMaximum) {
				double minimum = Double.MAX_VALUE;
				double maximum = -Double.MAX_VALUE;
				WAbstractChartImplementation.RenderRange rr = this.chart_
						.computeRenderRange(this.axis_, this.scale_);
				minimum = rr.minimum;
				maximum = rr.maximum;
				if (minimum == Double.MAX_VALUE) {
					if (this.scale_ == AxisScale.LogScale) {
						minimum = 1;
					} else {
						if (this.scale_ == AxisScale.DateScale) {
							minimum = WDate.getCurrentDate().toJulianDay() - 10;
						} else {
							minimum = 0;
						}
					}
				}
				if (maximum == -Double.MAX_VALUE) {
					if (this.scale_ == AxisScale.LogScale) {
						maximum = 10;
					} else {
						if (this.scale_ == AxisScale.DateScale) {
							maximum = WDate.getCurrentDate().toJulianDay();
						} else {
							maximum = 100;
						}
					}
				}
				if (findMinimum) {
					segment.renderMinimum = Math.min(minimum,
							findMaximum ? maximum : segment.maximum);
				}
				if (findMaximum) {
					segment.renderMaximum = Math.max(maximum,
							findMinimum ? minimum : segment.minimum);
				}
			}
			double diff = segment.renderMaximum - segment.renderMinimum;
			if (this.scale_ == AxisScale.LogScale) {
				double minLog10 = Math.log10(segment.renderMinimum);
				double maxLog10 = Math.log10(segment.renderMaximum);
				if (findMinimum && findMaximum) {
					segment.renderMinimum = Math.pow(10, Math.floor(minLog10));
					segment.renderMaximum = Math.pow(10, Math.ceil(maxLog10));
					if (segment.renderMinimum == segment.renderMaximum) {
						segment.renderMaximum = Math.pow(10,
								Math.ceil(maxLog10) + 1);
					}
				} else {
					if (findMinimum) {
						segment.renderMinimum = Math.pow(10,
								Math.floor(minLog10));
						if (segment.renderMinimum == segment.renderMaximum) {
							segment.renderMinimum = Math.pow(10,
									Math.floor(minLog10) - 1);
						}
					} else {
						if (findMaximum) {
							segment.renderMaximum = Math.pow(10,
									Math.ceil(maxLog10));
							if (segment.renderMinimum == segment.renderMaximum) {
								segment.renderMaximum = Math.pow(10,
										Math.ceil(maxLog10) + 1);
							}
						}
					}
				}
			} else {
				double resolution = this.resolution_;
				if (resolution == 0) {
					if (this.scale_ == AxisScale.LinearScale) {
						resolution = Math.max(1E-3,
								Math.abs(1E-3 * segment.renderMinimum));
					} else {
						if (this.scale_ == AxisScale.DateScale) {
							resolution = 1;
						} else {
							if (this.scale_ == AxisScale.DateTimeScale) {
								resolution = 120;
							}
						}
					}
				}
				if (Math.abs(diff) < resolution) {
					double average = (segment.renderMaximum + segment.renderMinimum) / 2.0;
					double d = resolution;
					if (findMinimum && findMaximum) {
						segment.renderMaximum = average + d / 2.0;
						segment.renderMinimum = average - d / 2.0;
					} else {
						if (findMinimum) {
							segment.renderMinimum = segment.renderMaximum - d;
						} else {
							if (findMaximum) {
								segment.renderMaximum = segment.renderMinimum
										+ d;
							}
						}
					}
					diff = segment.renderMaximum - segment.renderMinimum;
				}
				if (findMinimum && segment.renderMinimum >= 0
						&& segment.renderMinimum - 0.50 * diff <= 0) {
					segment.renderMinimum = 0;
				}
				if (findMaximum && segment.renderMaximum <= 0
						&& segment.renderMaximum + 0.50 * diff >= 0) {
					segment.renderMaximum = 0;
				}
			}
		}
		assert segment.renderMinimum < segment.renderMaximum;
	}

	private double getValue(final Object v) {
		switch (this.scale_) {
		case LinearScale:
		case LogScale:
			return StringUtils.asNumber(v);
		case DateScale:
			if (v.getClass().equals(WDate.class)) {
				WDate d = ((WDate) v);
				return (double) d.toJulianDay();
			} else {
				return Double.NaN;
			}
		case DateTimeScale:
			if (v.getClass().equals(WDate.class)) {
				WDate d = ((WDate) v);
				WDate dt = null;
				dt = d;
				return (double) dt.getDate().getTime();
			} else {
				return Double.NaN;
			}
		default:
			return -1.0;
		}
	}

	private double calcAutoNumLabels(Orientation orientation,
			final WAxis.Segment s) {
		if (orientation == Orientation.Horizontal) {
			if (Math.abs(this.labelAngle_) <= 15) {
				return s.renderLength
						/ Math.max((double) AUTO_H_LABEL_PIXELS, new WLength(
								this.defaultDateTimeFormat(s).toString()
										.length(), WLength.Unit.FontEm)
								.toPixels());
			} else {
				if (Math.abs(this.labelAngle_) <= 40) {
					return s.renderLength / (2 * AUTO_V_LABEL_PIXELS);
				} else {
					return s.renderLength / AUTO_V_LABEL_PIXELS;
				}
			}
		} else {
			return s.renderLength / AUTO_V_LABEL_PIXELS;
		}
	}

	private WString defaultDateTimeFormat(final WAxis.Segment s) {
		if (this.scale_ != AxisScale.DateScale
				&& this.scale_ != AxisScale.DateTimeScale) {
			return WString.Empty;
		}
		WDate dt = null;
		if (this.scale_ == AxisScale.DateScale) {
			dt = WDate.fromJulianDay((int) s.renderMinimum);
			if (!(dt != null)) {
				String exception = "Invalid julian day: "
						+ String.valueOf(s.renderMinimum);
				throw new WException(exception);
			}
		} else {
			dt = new WDate(new Date((long) (long) s.renderMinimum));
		}
		int interval = s.dateTimeRenderInterval;
		WAxis.DateTimeUnit unit = s.dateTimeRenderUnit;
		boolean atTick = interval > 1
				|| unit.getValue() <= WAxis.DateTimeUnit.Days.getValue()
				|| !!EnumUtils.mask(this.roundLimits_, AxisValue.MinimumValue)
						.isEmpty();
		return this.autoDateFormat(dt, unit, atTick);
	}

	double mapFromDevice(double d) {
		final WAxis.Segment firstSegment = this.segments_.get(0);
		final WAxis.Segment lastSegment = this.segments_.get(this.segments_
				.size() - 1);
		if (this.isInverted()) {
			d = lastSegment.renderStart + lastSegment.renderLength - d
					+ firstSegment.renderStart;
		}
		for (int i = 0; i < this.segments_.size(); ++i) {
			final WAxis.Segment s = this.segments_.get(i);
			boolean isLastSegment = i == this.segments_.size() - 1;
			if (isLastSegment
					|| !this.isInverted()
					&& d < this.mapToDevice(s.renderMaximum, i)
					|| this.isInverted()
					&& d < -(this.mapToDevice(s.renderMaximum, i)
							- lastSegment.renderStart
							- lastSegment.renderLength - firstSegment.renderStart)) {
				d = d - s.renderStart;
				if (this.scale_ != AxisScale.LogScale) {
					return s.renderMinimum + d
							* (s.renderMaximum - s.renderMinimum)
							/ s.renderLength;
				} else {
					return Math.exp(Math.log(s.renderMinimum)
							+ d
							* (Math.log(s.renderMaximum) - Math
									.log(s.renderMinimum)) / s.renderLength);
				}
			}
		}
		return 0;
	}

	double mapToDevice(final Object value) {
		return this.mapToDevice(this.getValue(value));
	}

	double mapToDevice(final Object value, int segment) {
		return this.mapToDevice(this.getValue(value), segment);
	}

	private double mapToDevice(double value) {
		if (Double.isNaN(value)) {
			return value;
		}
		for (int i = 0; i < this.segments_.size(); ++i) {
			if (value <= this.segments_.get(i).renderMaximum
					|| i == this.segments_.size() - 1) {
				return this.mapToDevice(value, i);
			}
		}
		assert false;
		return Double.NaN;
	}

	double mapToDevice(double u, int segment) {
		if (Double.isNaN(u)) {
			return u;
		}
		final WAxis.Segment s = this.segments_.get(segment);
		double d;
		if (this.scale_ != AxisScale.LogScale) {
			d = (u - s.renderMinimum) / (s.renderMaximum - s.renderMinimum)
					* s.renderLength;
		} else {
			u = Math.max(s.renderMinimum, u);
			d = (Math.log(u) - Math.log(s.renderMinimum))
					/ (Math.log(s.renderMaximum) - Math.log(s.renderMinimum))
					* s.renderLength;
		}
		if (this.isInverted()) {
			final WAxis.Segment firstSegment = this.segments_.get(0);
			final WAxis.Segment lastSegment = this.segments_.get(this.segments_
					.size() - 1);
			return lastSegment.renderStart + lastSegment.renderLength
					- (s.renderStart + d) + firstSegment.renderStart;
		} else {
			return s.renderStart + d;
		}
	}

	boolean isOnAxis(double d) {
		for (int i = 0; i < this.segments_.size(); ++i) {
			if (d >= this.segments_.get(i).renderMinimum
					&& d <= this.segments_.get(i).renderMaximum) {
				return true;
			}
		}
		return false;
	}

	private double getDrawnMinimum() {
		if (!this.isInverted()) {
			return this.mapFromDevice(0.0);
		} else {
			return this.mapFromDevice(this.fullRenderLength_);
		}
	}

	private double getDrawnMaximum() {
		if (!this.isInverted()) {
			return this.mapFromDevice(this.fullRenderLength_);
		} else {
			return this.mapFromDevice(0.0);
		}
	}

	private long getDateNumber(WDate dt) {
		switch (this.scale_) {
		case DateScale:
			return (long) dt.toJulianDay();
		case DateTimeScale:
			return (long) dt.getDate().getTime();
		default:
			return 1;
		}
	}

	void setZoomRangeFromClient(double minimum, double maximum) {
		if (minimum > maximum) {
			double temp = minimum;
			minimum = maximum;
			maximum = temp;
		}
		double min = this.getDrawnMinimum();
		double max = this.getDrawnMaximum();
		if (minimum <= min) {
			minimum = AUTO_MINIMUM;
		}
		if (maximum >= max) {
			maximum = AUTO_MAXIMUM;
		}
		this.zoomMin_ = minimum;
		this.zoomMax_ = maximum;
	}

	private boolean hasLabelTransformOnSide(AxisValue side) {
		return this.labelTransforms_.get(side) != null;
	}

	private static double EPSILON = 1E-3;
	private static final int AUTO_V_LABEL_PIXELS = 25;
	private static final int AUTO_H_LABEL_PIXELS = 80;

	static double round125(double v) {
		double n = Math.pow(10, Math.floor(Math.log10(v)));
		double msd = v / n;
		if (msd < 1.5) {
			return n;
		} else {
			if (msd < 3.3) {
				return 2 * n;
			} else {
				if (msd < 7) {
					return 5 * n;
				} else {
					return 10 * n;
				}
			}
		}
	}

	static double roundUp125(double v, double t) {
		return t * Math.ceil((v - 1E-10) / t);
	}

	static double roundDown125(double v, double t) {
		return t * Math.floor((v + 1E-10) / t);
	}

	static int roundDown(int v, int factor) {
		return v / factor * factor;
	}

	static int roundUp(int v, int factor) {
		return ((v - 1) / factor + 1) * factor;
	}

	static WPointF interpolate(final WPointF p1, final WPointF p2, double u) {
		double x = p1.getX();
		if (p2.getX() - p1.getX() > 0) {
			x += u;
		} else {
			if (p2.getX() - p1.getX() < 0) {
				x -= u;
			}
		}
		double y = p1.getY();
		if (p2.getY() - p1.getY() > 0) {
			y += u;
		} else {
			if (p2.getY() - p1.getY() < 0) {
				y -= u;
			}
		}
		return new WPointF(x, y);
	}

	static List<WString> splitLabel(CharSequence text) {
		String s = text.toString();
		List<String> splitText = new ArrayList<String>();
		splitText = new ArrayList<String>(Arrays.asList(s.split("\n")));
		List<WString> result = new ArrayList<WString>();
		for (int i = 0; i < splitText.size(); ++i) {
			result.add(new WString(splitText.get(i)));
		}
		return result;
	}

	static double calcYOffset(int lineNb, int nbLines, double lineHeight,
			EnumSet<AlignmentFlag> verticalAlign) {
		if (verticalAlign.equals(AlignmentFlag.AlignMiddle)) {
			return -((nbLines - 1) * lineHeight / 2.0) + lineNb * lineHeight;
		} else {
			if (verticalAlign.equals(AlignmentFlag.AlignTop)) {
				return lineNb * lineHeight;
			} else {
				if (verticalAlign.equals(AlignmentFlag.AlignBottom)) {
					return -(nbLines - 1 - lineNb) * lineHeight;
				} else {
					return 0;
				}
			}
		}
	}

	private static final double calcYOffset(int lineNb, int nbLines,
			double lineHeight, AlignmentFlag verticalAlig,
			AlignmentFlag... verticalAlign) {
		return calcYOffset(lineNb, nbLines, lineHeight,
				EnumSet.of(verticalAlig, verticalAlign));
	}
}
