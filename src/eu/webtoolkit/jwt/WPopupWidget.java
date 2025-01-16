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

/**
 * Base class for popup widgets.
 *
 * <p>A popup widget anchors to another widget, for which it usually provides additional information
 * or assists in editing, etc...
 *
 * <p>The popup widget will position itself relative to the anchor widget by taking into account
 * available space, and switching sides if necessary to fit the widget into the current window. For
 * example, a vertically anchored widget will by default be a &quot;drop-down&quot;, positioning
 * itself under the anchor widget, but it may also choose to position itself above the anchor widget
 * if space is lacking below.
 */
public class WPopupWidget extends WCompositeWidget {
  private static Logger logger = LoggerFactory.getLogger(WPopupWidget.class);

  /**
   * Constructor.
   *
   * <p>You need to pass in a widget that provides the main contents of the widget (e.g. a {@link
   * WTemplate} or {@link WContainerWidget}).
   *
   * <p>Unlike other widgets, a popup widget is a top-level widget that should not be added to
   * another container.
   */
  public WPopupWidget(WWidget impl) {
    super();
    this.anchorWidget_ = (WWidget) null;
    this.orientation_ = Orientation.Vertical;
    this.transient_ = false;
    this.autoHideDelay_ = 0;
    this.hidden_ = new Signal();
    this.shown_ = new Signal();
    this.jsHidden_ = new JSignal(impl, "hidden");
    this.jsShown_ = new JSignal(impl, "shown");
    this.setImplementation(impl);
    WApplication.getInstance().addGlobalWidget(this);
    this.hide();
    this.setPopup(true);
    this.setPositionScheme(PositionScheme.Absolute);
    this.jsHidden_.addListener(
        this,
        () -> {
          WPopupWidget.this.hide();
        });
    this.jsShown_.addListener(
        this,
        () -> {
          WPopupWidget.this.show();
        });
    WApplication.getInstance()
        .internalPathChanged()
        .addListener(
            this,
            (String e1) -> {
              WPopupWidget.this.onPathChange();
            });
  }
  /** Destructor. */
  public void remove() {
    WApplication.getInstance().removeGlobalWidget(this);
    super.remove();
  }
  /**
   * Sets an anchor widget.
   *
   * <p>A vertical popup will show below (or above) the widget, while a horizontal popup will show
   * right (or left) of the widget.
   */
  public void setAnchorWidget(WWidget anchorWidget, Orientation orientation) {
    this.anchorWidget_ = anchorWidget;
    this.orientation_ = orientation;
  }
  /**
   * Sets an anchor widget.
   *
   * <p>Calls {@link #setAnchorWidget(WWidget anchorWidget, Orientation orientation)
   * setAnchorWidget(anchorWidget, Orientation.Vertical)}
   */
  public final void setAnchorWidget(WWidget anchorWidget) {
    setAnchorWidget(anchorWidget, Orientation.Vertical);
  }

  public WWidget getAnchorWidget() {
    return this.anchorWidget_;
  }
  /** Returns the orientation. */
  public Orientation getOrientation() {
    return this.orientation_;
  }
  /**
   * Sets transient property.
   *
   * <p>A transient popup will automatically hide when the user clicks outside of the popup. When
   * <code>autoHideDelay</code> is not 0, then it will also automatically hide when the user moves
   * the mouse outside the widget for longer than this delay (in ms).
   */
  public void setTransient(boolean isTransient, int autoHideDelay) {
    this.transient_ = isTransient;
    this.autoHideDelay_ = autoHideDelay;
    if (this.isRendered()) {
      StringBuilder ss = new StringBuilder();
      ss.append(this.getJsRef())
          .append(".wtPopup.setTransient(")
          .append(this.transient_)
          .append(',')
          .append(this.autoHideDelay_)
          .append(");");
      this.doJavaScript(ss.toString());
    }
  }
  /**
   * Sets transient property.
   *
   * <p>Calls {@link #setTransient(boolean isTransient, int autoHideDelay) setTransient(isTransient,
   * 0)}
   */
  public final void setTransient(boolean isTransient) {
    setTransient(isTransient, 0);
  }
  /**
   * Returns whether the popup is transient.
   *
   * <p>
   *
   * @see WPopupWidget#setTransient(boolean isTransient, int autoHideDelay)
   */
  public boolean isTransient() {
    return this.transient_;
  }
  /**
   * Returns the auto-hide delay.
   *
   * <p>
   *
   * @see WPopupWidget#setTransient(boolean isTransient, int autoHideDelay)
   */
  public int getAutoHideDelay() {
    return this.autoHideDelay_;
  }

  public void setHidden(boolean hidden, final WAnimation animation) {
    if (WWebWidget.canOptimizeUpdates() && hidden == this.isHidden()) {
      return;
    }
    super.setHidden(hidden, animation);
    if (!hidden && this.anchorWidget_ != null) {
      this.positionAt(this.anchorWidget_, this.orientation_);
    }
    if (!WWebWidget.canOptimizeUpdates() || this.isRendered()) {
      if (hidden) {
        this.doJavaScript(
            "var o = " + this.getJsRef() + ";if (o && o.wtPopup) o.wtPopup.hidden();");
      } else {
        this.doJavaScript("var o = " + this.getJsRef() + ";if (o && o.wtPopup) o.wtPopup.shown();");
      }
    }
    if (hidden) {
      this.hidden().trigger();
    } else {
      this.shown().trigger();
    }
  }
  /**
   * Signal emitted when the popup is hidden.
   *
   * <p>This signal is emitted when the popup is being hidden because of a client-side event (not
   * when {@link WPopupWidget#setHidden(boolean hidden, WAnimation animation) setHidden()} or {@link
   * WWidget#hide()} is called).
   */
  public Signal hidden() {
    return this.hidden_;
  }
  /**
   * Signal emitted when the popup is shown.
   *
   * <p>This signal is emitted when the popup is being shown because of a client-side event (not
   * when {@link WPopupWidget#setHidden(boolean hidden, WAnimation animation) setHidden()} or {@link
   * WWidget#show()} is called).
   */
  public Signal shown() {
    return this.shown_;
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (flags.contains(RenderFlag.Full)) {
      this.defineJS();
    }
    super.render(flags);
  }

  protected void onPathChange() {
    this.hide();
  }

  private WWidget anchorWidget_;
  private Orientation orientation_;
  private boolean transient_;
  private int autoHideDelay_;
  private Signal hidden_;
  private Signal shown_;
  private JSignal jsHidden_;
  private JSignal jsShown_;
  // private void create(WWidget  parent) ;
  private void defineJS() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WPopupWidget.js", wtjs1());
    StringBuilder jsObj = new StringBuilder();
    jsObj
        .append("new Wt4.11.2.WPopupWidget(")
        .append(app.getJavaScriptClass())
        .append(',')
        .append(this.getJsRef())
        .append(',')
        .append(this.transient_)
        .append(',')
        .append(this.autoHideDelay_)
        .append(',')
        .append(!this.isHidden())
        .append(");");
    this.setJavaScriptMember(" WPopupWidget", jsObj.toString());
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WPopupWidget",
        "(function(e,t,n,i,o){t.wtPopup=this;const s=this,u=e.WT;let c=null,d=n,l=i,r=null,h=null,a=null;function f(){if(u.isIOS){document.addEventListener(\"touchstart\",p);document.addEventListener(\"touchend\",m)}else document.addEventListener(\"click\",v)}function p(e){const t=e.originalEvent.touches;r=t.length>1?null:{x:t[0].screenX,y:t[0].screenY}}function m(e){if(r){const t=e.originalEvent.changedTouches[0];Math.abs(r.x-t.screenX)<20&&Math.abs(r.y-t.screenY)<20&&v(e)}}function v(e){let n=u.target(e);n===document&&null!==u.WPopupWidget.popupClicked&&(n=u.WPopupWidget.popupClicked);(function(e,t){if(e===t)return!0;for(t=t.parentNode;t;t=t.parentNode)if(e===t)return!0;return!1})(t,n)||s.hide()}this.bindShow=function(e){h=e};this.bindHide=function(e){a=e};this.shown=function(){d&&setTimeout((function(){f()}),0);h&&h()};this.show=function(n,i){if(\"\"!==t.style.display){t.style.display=\"\";n&&u.positionAtWidget(t.id,n.id,i);e.emit(t,\"shown\")}};this.hidden=function(){a&&a();d&&function(){if(u.isIOS){document.removeEventListener(\"touchstart\",p);document.removeEventListener(\"touchend\",m)}else document.removeEventListener(\"click\",v)}()};this.hide=function(){\"none\"!==t.style.display&&(t.style.display=\"none\");e.emit(t,\"hidden\");s.hidden()};this.setTransient=function(e,n){d=e;l=n;d&&\"hidden\"!==t.style.display&&setTimeout((function(){f()}),0)};t.addEventListener(\"mouseleave\",(function(){clearTimeout(c);l>0&&(c=setTimeout((function(){s.hide()}),l))}));t.addEventListener(\"mouseenter\",(function(){clearTimeout(c)}));o&&this.shown()})");
  }
}
