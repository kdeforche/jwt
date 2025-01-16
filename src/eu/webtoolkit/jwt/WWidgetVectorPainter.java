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

class WWidgetVectorPainter extends WWidgetPainter {
  private static Logger logger = LoggerFactory.getLogger(WWidgetVectorPainter.class);

  public WWidgetVectorPainter(WPaintedWidget widget, WWidgetPainter.RenderType renderType) {
    super(widget);
    this.renderType_ = renderType;
  }

  public WPaintDevice createPaintDevice(boolean paintUpdate) {
    if (this.renderType_ == WWidgetPainter.RenderType.InlineSvg) {
      return new WSvgImage(
          new WLength(this.widget_.renderWidth_),
          new WLength(this.widget_.renderHeight_),
          paintUpdate);
    } else {
      return new WVmlImage(
          new WLength(this.widget_.renderWidth_),
          new WLength(this.widget_.renderHeight_),
          paintUpdate);
    }
  }

  public WPaintDevice getPaintDevice(boolean paintUpdate) {
    return this.createPaintDevice(paintUpdate);
  }

  public void createContents(DomElement canvas, WPaintDevice device) {
    WVectorImage vectorDevice = ObjectUtils.cast(device, WVectorImage.class);
    canvas.setProperty(Property.InnerHTML, vectorDevice.getRendered());
  }

  public void updateContents(final List<DomElement> result, WPaintDevice device) {
    WVectorImage vectorDevice = ObjectUtils.cast(device, WVectorImage.class);
    if (this.widget_.repaintFlags_.contains(PaintFlag.Update)) {
      DomElement painter =
          DomElement.updateGiven(
              "Wt4.11.2.getElement('p" + this.widget_.getId() + "').firstChild",
              DomElementType.DIV);
      painter.setProperty(Property.AddedInnerHTML, vectorDevice.getRendered());
      WApplication app = WApplication.getInstance();
      if (app.getEnvironment().agentIsOpera()) {
        painter.callMethod("forceRedraw();");
      }
      result.add(painter);
    } else {
      DomElement canvas = DomElement.getForUpdate('p' + this.widget_.getId(), DomElementType.DIV);
      canvas.setProperty(Property.InnerHTML, vectorDevice.getRendered());
      result.add(canvas);
    }
    this.widget_.sizeChanged_ = false;
  }

  public WWidgetPainter.RenderType getRenderType() {
    return this.renderType_;
  }

  private WWidgetPainter.RenderType renderType_;
}
