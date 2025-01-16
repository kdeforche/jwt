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

final class PaintedSlider extends WPaintedWidget {
  private static Logger logger = LoggerFactory.getLogger(PaintedSlider.class);

  public PaintedSlider(WSlider slider) {
    super();
    this.slider_ = slider;
    this.sliderReleased_ = new JSignal1<Integer>(this, "released") {};
    this.mouseDownJS_ = new JSlot();
    this.mouseMovedJS_ = new JSlot();
    this.mouseUpJS_ = new JSlot();
    this.handle_ = null;
    this.fill_ = null;
    this.setStyleClass("Wt-slider-bg");
    this.slider_.addStyleClass(
        "Wt-slider-" + (this.slider_.getOrientation() == Orientation.Horizontal ? "h" : "v"));
    if (this.slider_.getPositionScheme() == PositionScheme.Static) {
      this.slider_.setPositionScheme(PositionScheme.Relative);
      this.slider_.setOffsets(new WLength(0), EnumSet.of(Side.Left, Side.Top));
    }
    WInteractWidget fill = new WContainerWidget();
    {
      WWidget oldWidget = this.fill_;
      this.fill_ = fill;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.fill_);
        if (toRemove != null) toRemove.remove();
      }
    }
    WInteractWidget handle = this.slider_.getCreateHandle();
    {
      WWidget oldWidget = this.handle_;
      this.handle_ = handle;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.handle_);
        if (toRemove != null) toRemove.remove();
      }
    }
    this.fill_.setPositionScheme(PositionScheme.Absolute);
    this.fill_.setStyleClass("fill");
    this.handle_.setPositionScheme(PositionScheme.Absolute);
    this.handle_.setStyleClass("handle");
    this.handle_.setCanReceiveFocus(true);
    this.slider_.setCanReceiveFocus(true);
    this.connectSlots();
  }

  public void remove() {
    {
      WWidget oldWidget = this.fill_;
      this.fill_ = null;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.fill_);
        if (toRemove != null) toRemove.remove();
      }
    }
    {
      WWidget oldWidget = this.handle_;
      this.handle_ = null;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.handle_);
        if (toRemove != null) toRemove.remove();
      }
    }
    super.remove();
  }

  public void connectSlots() {
    if (WApplication.getInstance().getEnvironment().hasAjax()) {
      this.handle_.mouseWentDown().addListener(this.mouseDownJS_);
      this.handle_.touchStarted().addListener(this.mouseDownJS_);
      this.handle_.mouseMoved().addListener(this.mouseMovedJS_);
      this.handle_.touchMoved().addListener(this.mouseMovedJS_);
      this.handle_.mouseWentUp().addListener(this.mouseUpJS_);
      this.handle_.touchEnded().addListener(this.mouseUpJS_);
      this.slider_
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                PaintedSlider.this.onSliderClick(e1);
              });
      this.sliderReleased_.addListener(
          this,
          (Integer e1) -> {
            PaintedSlider.this.onSliderReleased(e1);
          });
    }
  }

  public void updateState() {
    boolean rtl = WApplication.getInstance().getLayoutDirection() == LayoutDirection.RightToLeft;
    Orientation o = this.slider_.getOrientation();
    final int handleOffset = 5;
    final int widgetLength = o == Orientation.Horizontal ? (int) this.getH() : (int) this.getW();
    int calculatedOffset = -(widgetLength / 2) + this.slider_.getHandleWidth() + handleOffset;
    WTheme theme = WApplication.getInstance().getTheme();
    WCssTheme cssTheme = ObjectUtils.cast(theme, WCssTheme.class);
    if (cssTheme != null && cssTheme.getName().equals("polished")) {
      calculatedOffset = 0;
    }
    if (o == Orientation.Horizontal) {
      this.handle_.resize(new WLength(this.slider_.getHandleWidth()), new WLength(this.getH()));
      this.handle_.setOffsets(new WLength(calculatedOffset), EnumSet.of(Side.Top));
    } else {
      this.handle_.resize(new WLength(this.getW()), new WLength(this.slider_.getHandleWidth()));
      this.handle_.setOffsets(new WLength(calculatedOffset), EnumSet.of(Side.Left));
    }
    double l = o == Orientation.Horizontal ? this.getW() : this.getH();
    double pixelsPerUnit = (l - this.slider_.getHandleWidth()) / this.getRange();
    String dir = "";
    String size = "";
    if (o == Orientation.Horizontal) {
      dir = rtl ? "right" : "left";
      size = "width";
    } else {
      dir = "top";
      size = "height";
    }
    char u = o == Orientation.Horizontal ? 'x' : 'y';
    double max = l - this.slider_.getHandleWidth();
    boolean horizontal = o == Orientation.Horizontal;
    char[] buf = new char[30];
    StringBuilder mouseDownJS = new StringBuilder();
    mouseDownJS
        .append("obj.setAttribute('down', Wt4.11.2")
        .append(".widgetCoordinates(obj, event).")
        .append(u)
        .append(");");
    StringBuilder computeD = new StringBuilder();
    computeD
        .append("var objh = ")
        .append(this.handle_.getJsRef())
        .append(",")
        .append("objf = ")
        .append(this.fill_.getJsRef())
        .append(",")
        .append("objb = ")
        .append(this.slider_.getJsRef())
        .append(",")
        .append("minVal = ")
        .append(this.slider_.getMinimum())
        .append(",")
        .append("maxVal = ")
        .append(this.slider_.getMaximum())
        .append(",")
        .append("stepVal = ")
        .append(this.slider_.getStep())
        .append(",")
        .append("page_u = WT.pageCoordinates(event).")
        .append(u)
        .append(",")
        .append("widget_page_u = WT.widgetPageCoordinates(objb).")
        .append(u)
        .append(",")
        .append("pos = page_u - widget_page_u,")
        .append("rtl = ")
        .append(rtl)
        .append(",")
        .append("horizontal = ")
        .append(horizontal)
        .append(";")
        .append("if (rtl && horizontal)");
    computeD.append("pos = ").append(MathUtils.roundJs(l, 3)).append(" - pos;");
    computeD.append("var d = pos - down;");
    computeD.append("let sliderV = Math.abs(maxVal - minVal);");
    computeD.append("let scaleFactor = ").append(MathUtils.roundJs(max, 3)).append(" / sliderV;");
    computeD.append("let scaledD = d / scaleFactor;");
    computeD.append("let absD = Math.abs(scaledD);");
    computeD.append("let signD = scaledD < 0 ? -1 : 1;");
    computeD.append("let lowDelta = absD - (absD % stepVal);");
    computeD.append("let highDelta = lowDelta + stepVal;");
    computeD.append("if (absD- lowDelta < highDelta - absD) {");
    computeD.append("d = lowDelta * signD;");
    computeD.append("} else {");
    computeD.append("d = highDelta *signD;");
    computeD.append("}");
    computeD.append("d = d * scaleFactor;");
    StringBuilder mouseMovedJS = new StringBuilder();
    mouseMovedJS
        .append("var down = obj.getAttribute('down');")
        .append("var WT = Wt4.11.2;")
        .append("if (down != null && down != '') {")
        .append(computeD.toString());
    mouseMovedJS
        .append("d = Math.max(0, Math.min(d, ")
        .append(MathUtils.roundJs(max, 3))
        .append("));");
    mouseMovedJS
        .append("var v = Math.round(d/")
        .append(MathUtils.roundJs(pixelsPerUnit, 3))
        .append(");");
    mouseMovedJS.append("var intd = v*").append(MathUtils.roundJs(pixelsPerUnit, 3)).append(";");
    mouseMovedJS
        .append("if (Math.abs(WT.pxself(objh, '")
        .append(dir)
        .append("') - intd) > 1) {")
        .append("objf.style.")
        .append(size)
        .append(" = ");
    if (o == Orientation.Vertical) {
      mouseMovedJS.append('(').append(MathUtils.roundJs(max, 3));
      mouseMovedJS.append(" - intd + ").append(this.slider_.getHandleWidth() / 2).append(")");
    } else {
      mouseMovedJS.append("intd + ").append(this.slider_.getHandleWidth() / 2);
    }
    mouseMovedJS
        .append(" + 'px';")
        .append("objh.style.")
        .append(dir)
        .append(" = intd + 'px';")
        .append("var vs = ");
    if (o == Orientation.Horizontal) {
      mouseMovedJS.append("v + ").append(this.slider_.getMinimum());
    } else {
      mouseMovedJS.append(this.slider_.getMaximum()).append(" - v");
    }
    mouseMovedJS.append(";").append("var f = objb.onValueChange;").append("if (f) f(vs);");
    if (this.slider_.sliderMoved().needsUpdate(true)) {
      mouseMovedJS.append(this.slider_.sliderMoved().createCall("vs"));
    }
    mouseMovedJS.append("}").append("}");
    StringBuilder mouseUpJS = new StringBuilder();
    mouseUpJS
        .append("var down = obj.getAttribute('down');")
        .append("var WT = Wt4.11.2;")
        .append("if (down != null && down != '') {")
        .append(computeD.toString())
        .append("d += ")
        .append(this.slider_.getHandleWidth() / 2)
        .append(";")
        .append(this.sliderReleased_.createCall("Math.round(d)"))
        .append("obj.removeAttribute('down');")
        .append("}");
    boolean enabled = !this.slider_.isDisabled();
    this.mouseDownJS_.setJavaScript(
        "function(obj, event) {" + (enabled ? mouseDownJS.toString() : "") + "}");
    this.mouseMovedJS_.setJavaScript(
        "function(obj, event) {" + (enabled ? mouseMovedJS.toString() : "") + "}");
    this.mouseUpJS_.setJavaScript(
        "function(obj, event) {" + (enabled ? mouseUpJS.toString() : "") + "}");
    this.update();
    this.updateSliderPosition();
  }

  public void updateSliderPosition() {
    double l = this.slider_.getOrientation() == Orientation.Horizontal ? this.getW() : this.getH();
    double pixelsPerUnit = (l - this.slider_.getHandleWidth()) / this.getRange();
    double u = ((double) this.slider_.getValue() - this.slider_.getMinimum()) * pixelsPerUnit;
    if (this.slider_.getOrientation() == Orientation.Horizontal) {
      this.handle_.setOffsets(new WLength(u), EnumSet.of(Side.Left));
      this.fill_.setWidth(new WLength(u + this.slider_.getHandleWidth() / 2));
    } else {
      this.handle_.setOffsets(
          new WLength(this.getH() - this.slider_.getHandleWidth() - u), EnumSet.of(Side.Top));
      this.fill_.setHeight(new WLength(u + this.slider_.getHandleWidth() / 2));
    }
    this.handle_.setFocus(true);
  }

  public void doUpdateDom(final DomElement element, boolean all) {
    if (all) {
      WApplication app = WApplication.getInstance();
      DomElement west = DomElement.createNew(DomElementType.DIV);
      west.setProperty(Property.Class, "Wt-w");
      element.addChild(west);
      DomElement east = DomElement.createNew(DomElementType.DIV);
      east.setProperty(Property.Class, "Wt-e");
      element.addChild(east);
      element.addChild(this.createSDomElement(app));
      element.addChild(this.fill_.createSDomElement(app));
      element.addChild(this.handle_.createSDomElement(app));
    }
  }

  public void sliderResized(final WLength width, final WLength height) {
    if (this.slider_.getOrientation() == Orientation.Horizontal) {
      WLength w = width;
      if (!w.isAuto()) {
        w = new WLength(w.toPixels() - 10);
      }
      this.resize(w, height);
    } else {
      WLength h = height;
      if (!h.isAuto()) {
        h = new WLength(h.toPixels() - 10);
      }
      this.resize(width, h);
    }
    this.updateState();
  }

  protected void paintEvent(WPaintDevice paintDevice) {
    int tickInterval = this.slider_.getTickInterval();
    int r = this.getRange();
    if (r == 0) {
      return;
    }
    if (tickInterval == 0) {
      tickInterval = r / 2;
    }
    int numTicks = tickInterval == 0 ? 2 : r / tickInterval + 1;
    if (numTicks < 1) {
      return;
    }
    int w = 0;
    int h = 0;
    switch (this.slider_.getOrientation()) {
      case Horizontal:
        w = (int) paintDevice.getWidth().toPixels();
        h = (int) paintDevice.getHeight().toPixels();
        break;
      case Vertical:
        w = (int) paintDevice.getHeight().toPixels();
        h = (int) paintDevice.getWidth().toPixels();
    }
    double tickStep = ((double) w + 10 - this.slider_.getHandleWidth()) / (numTicks - 1);
    WPainter painter = new WPainter(paintDevice);
    for (int i = 0; i < numTicks; ++i) {
      int v = this.slider_.getMinimum() + i * tickInterval;
      int x = -5 + this.slider_.getHandleWidth() / 2 + (int) (i * tickStep);
      switch (this.slider_.getOrientation()) {
        case Horizontal:
          this.slider_.paintTick(painter, v, x, h / 2);
          break;
        case Vertical:
          this.slider_.paintTick(painter, v, h / 2, w - x);
      }
    }
  }

  private WSlider slider_;
  private JSignal1<Integer> sliderReleased_;
  private JSlot mouseDownJS_;
  private JSlot mouseMovedJS_;
  private JSlot mouseUpJS_;
  private WInteractWidget handle_;
  private WInteractWidget fill_;

  private int getRange() {
    return this.slider_.getMaximum() - this.slider_.getMinimum();
  }

  private double getW() {
    return this.getWidth().toPixels()
        + (this.slider_.getOrientation() == Orientation.Horizontal ? 10 : 0);
  }

  private double getH() {
    return this.getHeight().toPixels()
        + (this.slider_.getOrientation() == Orientation.Vertical ? 10 : 0);
  }

  private void onSliderClick(final WMouseEvent event) {
    int x = event.getWidget().x;
    int y = event.getWidget().y;
    if (WApplication.getInstance().getLayoutDirection() == LayoutDirection.RightToLeft) {
      x = (int) (this.getW() - x);
    }
    this.onSliderReleased(this.slider_.getOrientation() == Orientation.Horizontal ? x : y);
  }

  private void onSliderReleased(int u) {
    if (this.slider_.getOrientation() == Orientation.Horizontal) {
      u -= this.slider_.getHandleWidth() / 2;
    } else {
      u = (int) this.getH() - (u + this.slider_.getHandleWidth() / 2);
    }
    double l = this.slider_.getOrientation() == Orientation.Horizontal ? this.getW() : this.getH();
    double pixelsPerUnit = (l - this.slider_.getHandleWidth()) / this.getRange();
    double v =
        Math.max(
            this.slider_.getMinimum(),
            Math.min(
                this.slider_.getMaximum(),
                this.slider_.getMinimum() + (int) ((double) u / pixelsPerUnit + 0.5)));
    v = this.getClosestNumberByStep((int) v, this.slider_.getStep());
    this.slider_.sliderMoved().trigger((int) v);
    this.slider_.setValue((int) v);
    this.slider_.valueChanged().trigger(this.slider_.getValue());
    this.updateSliderPosition();
  }

  private int getClosestNumberByStep(int value, int step) {
    int absValue = Math.abs(value);
    int sign = value < 0 ? -1 : 1;
    int lowDelta = absValue - absValue % step;
    int highDelta = lowDelta + step;
    if (absValue - lowDelta < highDelta - absValue) {
      return lowDelta * sign;
    } else {
      return highDelta * sign;
    }
  }
}
