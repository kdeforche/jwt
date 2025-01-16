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

abstract class StdLayoutItemImpl extends WObject implements WLayoutItemImpl {
  private static Logger logger = LoggerFactory.getLogger(StdLayoutItemImpl.class);

  public StdLayoutItemImpl() {
    super();
  }

  public WContainerWidget getContainer() {
    return ObjectUtils.cast(this.getLayoutItem().getParentWidget(), WContainerWidget.class);
  }

  public abstract WLayoutItem getLayoutItem();

  public abstract int getMinimumWidth();

  public abstract int getMinimumHeight();

  public abstract int getMaximumWidth();

  public abstract int getMaximumHeight();

  public StdLayoutImpl getParentLayoutImpl() {
    WLayoutItem i = this.getLayoutItem();
    if (i.getParentLayout() != null) {
      return ObjectUtils.cast(i.getParentLayout().getImpl(), StdLayoutImpl.class);
    } else {
      return null;
    }
  }

  public abstract DomElement createDomElement(
      DomElement parent, boolean fitWidth, boolean fitHeight, WApplication app);
}
