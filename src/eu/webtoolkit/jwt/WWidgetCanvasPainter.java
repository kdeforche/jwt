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

class WWidgetCanvasPainter extends WWidgetPainter {
  private static Logger logger = LoggerFactory.getLogger(WWidgetCanvasPainter.class);

  public WWidgetCanvasPainter(WPaintedWidget widget) {
    super(widget);
  }

  public WPaintDevice createPaintDevice(boolean paintUpdate) {
    return new WCanvasPaintDevice(
        new WLength(this.widget_.renderWidth_),
        new WLength(this.widget_.renderHeight_),
        paintUpdate);
  }

  public WPaintDevice getPaintDevice(boolean paintUpdate) {
    return this.createPaintDevice(paintUpdate);
  }

  public void createContents(DomElement result, WPaintDevice device) {
    String wstr = String.valueOf(this.widget_.renderWidth_);
    String hstr = String.valueOf(this.widget_.renderHeight_);
    result.setProperty(Property.StylePosition, "relative");
    result.setProperty(Property.StyleOverflowX, "hidden");
    result.setProperty(Property.StyleOverflowY, "hidden");
    DomElement canvas = DomElement.createNew(DomElementType.CANVAS);
    canvas.setId('c' + this.widget_.getId());
    canvas.setProperty(Property.StyleDisplay, "block");
    canvas.setAttribute("width", wstr);
    canvas.setAttribute("height", hstr);
    result.addChild(canvas);
    this.widget_.sizeChanged_ = false;
    WCanvasPaintDevice canvasDevice = ObjectUtils.cast(device, WCanvasPaintDevice.class);
    DomElement text = null;
    if (canvasDevice.getTextMethod() == WCanvasPaintDevice.TextMethod.DomText) {
      text = DomElement.createNew(DomElementType.DIV);
      text.setId('t' + this.widget_.getId());
      text.setProperty(Property.StylePosition, "absolute");
      text.setProperty(Property.StyleZIndex, "1");
      text.setProperty(Property.StyleTop, "0px");
      text.setProperty(Property.StyleLeft, "0px");
    }
    DomElement el = text != null ? text : result;
    boolean hasJsObjects = this.widget_.jsObjects_.size() > 0;
    WApplication app = WApplication.getInstance();
    {
      StringBuilder ss = new StringBuilder();
      ss.append("new Wt4.11.2.WPaintedWidget(")
          .append(app.getJavaScriptClass())
          .append(",")
          .append(this.widget_.getJsRef())
          .append(");");
      el.callJavaScript(ss.toString());
    }
    String updateAreasJs = "";
    if (hasJsObjects) {
      StringBuilder ss = new StringBuilder();
      ss.append("new Wt4.11.2.WJavaScriptObjectStorage(")
          .append(app.getJavaScriptClass())
          .append(",")
          .append(this.widget_.getJsRef())
          .append(");");
      this.widget_.jsObjects_.updateJs(ss, true);
      el.callJavaScript(ss.toString());
      if (this.widget_.areaImage_ != null) {
        this.widget_.areaImage_.setTargetJS(this.widget_.getObjJsRef());
        updateAreasJs = this.widget_.areaImage_.getUpdateAreasJS();
      }
    }
    canvasDevice.render(this.widget_.getJsRef(), 'c' + this.widget_.getId(), el, updateAreasJs);
    if (text != null) {
      result.addChild(text);
    }
  }

  public void updateContents(final List<DomElement> result, WPaintDevice device) {
    WCanvasPaintDevice canvasDevice = ObjectUtils.cast(device, WCanvasPaintDevice.class);
    if (this.widget_.sizeChanged_) {
      DomElement canvas =
          DomElement.getForUpdate('c' + this.widget_.getId(), DomElementType.CANVAS);
      canvas.setAttribute("width", String.valueOf(this.widget_.renderWidth_));
      canvas.setAttribute("height", String.valueOf(this.widget_.renderHeight_));
      result.add(canvas);
      this.widget_.sizeChanged_ = false;
    }
    boolean domText = canvasDevice.getTextMethod() == WCanvasPaintDevice.TextMethod.DomText;
    DomElement el =
        DomElement.getForUpdate(
            domText ? 't' + this.widget_.getId() : this.widget_.getId(), DomElementType.DIV);
    if (domText) {
      el.removeAllChildren();
    }
    boolean hasJsObjects = this.widget_.jsObjects_.size() > 0;
    String updateAreasJs = "";
    if (hasJsObjects) {
      StringBuilder ss = new StringBuilder();
      this.widget_.jsObjects_.updateJs(ss, false);
      el.callJavaScript(ss.toString());
      if (this.widget_.areaImage_ != null) {
        this.widget_.areaImage_.setTargetJS(this.widget_.getObjJsRef());
        updateAreasJs = this.widget_.areaImage_.getUpdateAreasJS();
      }
    }
    canvasDevice.render(this.widget_.getJsRef(), 'c' + this.widget_.getId(), el, updateAreasJs);
    result.add(el);
  }

  public WWidgetPainter.RenderType getRenderType() {
    return WWidgetPainter.RenderType.HtmlCanvas;
  }
}
