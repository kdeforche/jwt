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

class WTimerWidget extends WInteractWidget {
  private static Logger logger = LoggerFactory.getLogger(WTimerWidget.class);

  public WTimerWidget(WTimer timer) {
    super();
    this.timer_ = timer;
    this.timerStarted_ = false;
  }

  public void remove() {
    this.timer_.timerWidget_ = (WTimerWidget) null;
    super.remove();
  }

  public void timerStart(boolean jsRepeat) {
    this.timerStarted_ = true;
    this.jsRepeat_ = jsRepeat;
    this.repaint();
  }

  public boolean isTimerExpired() {
    return this.timer_.getRemainingInterval() == 0;
  }

  public boolean isJsRepeat() {
    return this.jsRepeat_;
  }

  private WTimer timer_;
  private boolean timerStarted_;
  private boolean jsRepeat_;

  void updateDom(final DomElement element, boolean all) {
    if (this.timerStarted_
        || (!WApplication.getInstance().getEnvironment().hasJavaScript() || all)
            && this.timer_.isActive()) {
      if (this.jsRepeat_) {
        element.setTimeout(
            this.timer_.getRemainingInterval(), (int) this.timer_.getInterval().toMillis());
      } else {
        element.setTimeout(this.timer_.getRemainingInterval(), false);
      }
      this.timerStarted_ = false;
    }
    super.updateDom(element, all);
  }

  DomElementType getDomElementType() {
    return DomElementType.SPAN;
  }

  String renderRemoveJs(boolean recursive) {
    return "{var obj="
        + this.getJsRef()
        + ";if (obj && obj.timer) {clearTimeout(obj.timer);obj.timer = null;}Wt4.11.2.remove('"
        + this.getId()
        + "');}";
  }

  protected void enableAjax() {
    if (this.timer_.isActive()) {
      this.timerStarted_ = true;
      this.repaint();
    }
    super.enableAjax();
  }
}
