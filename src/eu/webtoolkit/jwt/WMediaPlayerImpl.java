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

final class WMediaPlayerImpl extends WTemplate {
  private static Logger logger = LoggerFactory.getLogger(WMediaPlayerImpl.class);

  public WMediaPlayerImpl(WMediaPlayer player, final CharSequence text) {
    super(text, (WContainerWidget) null);
    this.player_ = player;
    this.setFormObject(true);
  }

  String renderRemoveJs(boolean recursive) {
    if (this.isRendered()) {
      String result = this.player_.getJsPlayerRef() + ".jPlayer('destroy');";
      if (!recursive) {
        result += "Wt4.11.2.remove('" + this.getId() + "');";
      }
      return result;
    } else {
      return super.renderRemoveJs(recursive);
    }
  }

  protected void setFormData(final WObject.FormData formData) {
    this.player_.setFormData(formData);
  }

  private WMediaPlayer player_;
}
