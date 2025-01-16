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

class StdWidgetItemImpl extends StdLayoutItemImpl implements WWidgetItemImpl {
  private static Logger logger = LoggerFactory.getLogger(StdWidgetItemImpl.class);

  public StdWidgetItemImpl(WWidgetItem item) {
    super();
    this.item_ = item;
  }

  public static String getChildrenResizeJS() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WtResize.js", wtjs10());
    return "Wt4.11.2.ChildrenResize";
  }

  public static String getChildrenGetPSJS() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WtResize.js", wtjs11());
    return "Wt4.11.2.ChildrenGetPS";
  }

  public static String getSecondResizeJS() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WtResize.js", wtjs12());
    return "Wt4.11.2.LastResize";
  }

  public static String getSecondGetPSJS() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WtResize.js", wtjs13());
    return "Wt4.11.2.LastGetPS";
  }

  public String getId() {
    return this.item_.getWidget().getId();
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

  public WLayoutItem getLayoutItem() {
    return this.item_;
  }

  public DomElement createDomElement(
      DomElement parent, boolean fitWidth, boolean fitHeight, WApplication app) {
    WWidget w = this.item_.getWidget();
    w.setInline(false);
    DomElement d = w.createSDomElement(app);
    DomElement result = d;
    if (app.getEnvironment().agentIsIElt(9)
        && (d.getType() == DomElementType.TEXTAREA
            || d.getType() == DomElementType.SELECT
            || d.getType() == DomElementType.INPUT
            || d.getType() == DomElementType.BUTTON)) {
      d.removeProperty(Property.StyleDisplay);
    }
    if (!app.getEnvironment().agentIsIElt(9)
        && w.getJavaScriptMember(WWidget.WT_RESIZE_JS).length() == 0
        && d.getType() != DomElementType.TABLE
        && app.getTheme().canBorderBoxElement(d)) {
      d.setProperty(Property.StyleBoxSizing, "border-box");
    }
    return result;
  }

  private WWidgetItem item_;

  static WJavaScriptPreamble wtjs10() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "ChildrenResize",
        "(function(t,i,e,o){const s=this,n=e>=0;if(o)if(n){t.style.height=e+\"px\";t.lh=!0}else{t.style.height=\"\";t.lh=!1}else t.lh=!1;if(s.boxSizing(t)){e-=s.px(t,\"marginTop\");e-=s.px(t,\"marginBottom\");e-=s.px(t,\"borderTopWidth\");e-=s.px(t,\"borderBottomWidth\");e-=s.px(t,\"paddingTop\");e-=s.px(t,\"paddingBottom\");i-=s.px(t,\"marginLeft\");i-=s.px(t,\"marginRight\");i-=s.px(t,\"borderLeftWidth\");i-=s.px(t,\"borderRightWidth\");i-=s.px(t,\"paddingLeft\");i-=s.px(t,\"paddingRight\")}function p(t){let i=s.px(t,\"marginTop\");i+=s.px(t,\"marginBottom\");if(!s.boxSizing(t)){i+=s.px(t,\"borderTopWidth\");i+=s.px(t,\"borderBottomWidth\");i+=s.px(t,\"paddingTop\");i+=s.px(t,\"paddingBottom\")}return i}for(const o of t.childNodes)if(1===o.nodeType&&!o.classList.contains(\"wt-reparented\"))if(n){const t=e-p(o);if(t>0){if(o.offsetTop>0){const t=s.css(o,\"overflow\");\"visible\"!==t&&\"\"!==t||(o.style.overflow=\"auto\")}if(o.wtResize)o.wtResize(o,i,t,!0);else{const i=t+\"px\";if(o.style.height!==i){o.style.height=i;o.lh=!0}}}}else if(o.wtResize)o.wtResize(o,i,-1,!0);else{o.style.height=\"\";o.lh=!1}})");
  }

  static WJavaScriptPreamble wtjs11() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "ChildrenGetPS",
        "(function(t,i,e,o){return o})");
  }

  static WJavaScriptPreamble wtjs12() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "LastResize",
        "(function(t,i,e,o){const s=this,n=e>=0;if(o)if(n){t.style.height=e+\"px\";t.lh=!0}else{t.style.height=\"\";t.lh=!1}else t.lh=!1;let p=t.lastChild;for(;p&&1===p.nodeType&&(p.classList.contains(\"wt-reparented\")||p.classList.contains(\"resize-sensor\"));)p=p.previousSibling;if(!p)return;const h=p.previousSibling;if(n){if((e-=h.offsetHeight+s.px(h,\"marginTop\")+s.px(h,\"marginBottom\"))>0)if(p.wtResize)p.wtResize(p,i,e,!0);else{p.style.height=e+\"px\";p.lh=!0}}else if(p.wtResize)p.wtResize(p,-1,-1,!0);else{p.style.height=\"\";p.lh=!1}})");
  }

  static WJavaScriptPreamble wtjs13() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "LastGetPS",
        "(function(t,i,e,o){const s=this;for(const n of t.childNodes)if(n!==i){const t=s.css(n,\"position\");\"absolute\"!==t&&\"fixed\"!==t&&(0===e?o=Math.max(o,n.offsetWidth):o+=n.offsetHeight+s.px(n,\"marginTop\")+s.px(n,\"marginBottom\"))}return o})");
  }
}
