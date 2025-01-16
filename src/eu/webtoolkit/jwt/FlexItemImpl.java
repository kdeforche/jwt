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

class FlexItemImpl extends StdLayoutItemImpl implements WWidgetItemImpl {
  private static Logger logger = LoggerFactory.getLogger(FlexItemImpl.class);

  public FlexItemImpl(WWidgetItem item) {
    super();
    this.item_ = item;
  }

  public WLayoutItem getLayoutItem() {
    return this.item_;
  }

  public int getMinimumHeight() {
    if (this.item_.getWidget().isHidden()) {
      return 0;
    } else {
      return (int) this.item_.getWidget().getMinimumHeight().toPixels();
    }
  }

  public int getMinimumWidth() {
    if (this.item_.getWidget().isHidden()) {
      return 0;
    } else {
      return (int) this.item_.getWidget().getMinimumWidth().toPixels();
    }
  }

  public int getMaximumHeight() {
    if (this.item_.getWidget().isHidden()) {
      return 0;
    } else {
      return (int) this.item_.getWidget().getMaximumHeight().toPixels();
    }
  }

  public int getMaximumWidth() {
    if (this.item_.getWidget().isHidden()) {
      return 0;
    } else {
      return (int) this.item_.getWidget().getMaximumWidth().toPixels();
    }
  }

  public DomElement createDomElement(
      DomElement parent, boolean fitWidth, boolean fitHeight, WApplication app) {
    WWidget w = this.item_.getWidget();
    DomElement result = w.createSDomElement(app);
    ResizeSensor.applyIfNeeded(w);
    return result;
  }

  private WWidgetItem item_;
}
