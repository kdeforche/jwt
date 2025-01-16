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
 * A menu presented in a popup window.
 *
 * <p>The menu implements a typical context menu, with support for submenu&apos;s. It is a
 * specialized {@link WMenu} from which it inherits most of the API.
 *
 * <p>When initially created, the menu is invisible, until {@link WPopupMenu#popup(WPoint p)
 * popup()} or exec() is called. Then, the menu will remain visible until an item is selected, or
 * the user cancels the menu (by hitting Escape or clicking elsewhere).
 *
 * <p>The implementation assumes availability of JavaScript to position the menu at the current
 * mouse position and provide feed-back of the currently selected item.
 *
 * <p>As with {@link WDialog}, there are two ways of using the menu. The simplest way is to use one
 * of the synchronous exec() methods, which starts a reentrant event loop and waits until the user
 * cancelled the popup menu (by hitting Escape or clicking elsewhere), or selected an item.
 *
 * <p>Alternatively, you can use one of the {@link WPopupMenu#popup(WPoint p) popup()} methods to
 * show the menu and listen to the {@link WPopupMenu#triggered() triggered()} signal where you read
 * the {@link WPopupMenu#getResult() getResult()}, or associate the menu with a button using {@link
 * WPushButton#setMenu(WPopupMenu popupMenu) WPushButton#setMenu()}.
 *
 * <p>You have several options to react to the selection of an item:
 *
 * <ul>
 *   <li>Either you use the {@link WMenuItem} itself to identify the action, perhaps by
 *       specialization or simply by binding custom data using {@link WMenuItem#setData(Object data)
 *       WMenuItem#setData()}.
 *   <li>You can bind a separate method to each item&apos;s {@link WMenuItem#triggered()} signal.
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Create a menu with some items
 * WPopupMenu popup = new WPopupMenu();
 * popup.addItem("icons/item1.gif", "Item 1");
 * popup.addItem("Item 2").setCheckable(true);
 * popup.addItem("Item 3");
 * popup.addSeparator();
 * popup.addItem("Item 4");
 * popup.addSeparator();
 * popup.addItem("Item 5");
 * popup.addItem("Item 6");
 * popup.addSeparator();
 *
 * WPopupMenu subMenu = new WPopupMenu();
 * subMenu.addItem("Sub Item 1");
 * subMenu.addItem("Sub Item 2");
 * popup.addMenu("Item 7", subMenu);
 *
 * WMenuItem item = popup.exec(event);
 *
 * if (item != null) {
 * // ... do associated action.
 * }
 *
 * }</pre>
 *
 * <p>A snapshot of the {@link WPopupMenu}: <div align="center"> <img
 * src="doc-files/WPopupMenu-default-1.png">
 *
 * <p><strong>WPopupMenu example (default)</strong> </div>
 *
 * <p><div align="center"> <img src="doc-files/WPopupMenu-polished-1.png">
 *
 * <p><strong>WPopupMenu example (polished)</strong> </div>
 *
 * <p>
 *
 * @see WMenuItem
 */
public class WPopupMenu extends WMenu {
  private static Logger logger = LoggerFactory.getLogger(WPopupMenu.class);

  /**
   * Creates a new popup menu.
   *
   * <p>The menu is hidden, by default, and must be shown using {@link WPopupMenu#popup(WPoint p)
   * popup()} or exec().
   */
  public WPopupMenu(WStackedWidget contentsStack, WContainerWidget parentContainer) {
    super(contentsStack, (WContainerWidget) null);
    this.topLevel_ = null;
    this.result_ = null;
    this.location_ = null;
    this.button_ = null;
    this.aboutToHide_ = new Signal();
    this.triggered_ = new Signal1<WMenuItem>();
    this.cancel_ = new JSignal(this, "cancel");
    this.recursiveEventLoop_ = false;
    this.willPopup_ = false;
    this.hideOnSelect_ = true;
    this.autoHideDelay_ = -1;
    String CSS_RULES_NAME = "Wt::WPopupMenu";
    WApplication app = WApplication.getInstance();
    if (!app.getStyleSheet().isDefined(CSS_RULES_NAME)) {
      app.getStyleSheet()
          .addRule(".Wt-notselected .Wt-popupmenu", "visibility: hidden;", CSS_RULES_NAME);
    }
    app.addGlobalWidget(this);
    this.getWebWidget().setBaseZIndex(110000);
    this.setPopup(true);
    this.hide();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new popup menu.
   *
   * <p>Calls {@link #WPopupMenu(WStackedWidget contentsStack, WContainerWidget parentContainer)
   * this((WStackedWidget)null, (WContainerWidget)null)}
   */
  public WPopupMenu() {
    this((WStackedWidget) null, (WContainerWidget) null);
  }
  /**
   * Creates a new popup menu.
   *
   * <p>Calls {@link #WPopupMenu(WStackedWidget contentsStack, WContainerWidget parentContainer)
   * this(contentsStack, (WContainerWidget)null)}
   */
  public WPopupMenu(WStackedWidget contentsStack) {
    this(contentsStack, (WContainerWidget) null);
  }

  public void remove() {
    if (this.button_ != null) {
      WPushButton b = ObjectUtils.cast(this.button_, WPushButton.class);
      if (b != null && b.getMenu() == this) {
        b.setMenu((WPopupMenu) null);
      }
    }
    if (this.isGlobalWidget()) {
      WApplication.getInstance().removeGlobalWidget(this);
    }
    super.remove();
  }
  /**
   * Shows the the popup at a position.
   *
   * <p>Displays the popup at a point with document coordinates <code>point</code>. The positions
   * intelligent, and will chose one of the four menu corners to correspond to this point so that
   * the popup menu is completely visible within the window.
   *
   * <p>
   */
  public void popup(final WPoint p) {
    this.popupImpl();
    this.setOffsets(new WLength(42), EnumSet.of(Side.Left, Side.Top));
    this.setOffsets(new WLength(-10000), EnumSet.of(Side.Left, Side.Top));
    this.doJavaScript(
        "Wt4.11.2.positionXY('"
            + this.getId()
            + "',"
            + String.valueOf(p.getX())
            + ","
            + String.valueOf(p.getY())
            + ");");
  }
  /**
   * Shows the the popup at the location of a mouse event.
   *
   * <p>This is a convenience method for {@link WPopupMenu#popup(WPoint p) popup()} that uses the
   * event&apos;s document coordinates.
   *
   * <p>
   *
   * @see WPopupMenu#popup(WPoint p)
   * @see WMouseEvent#getDocument()
   */
  public void popup(final WMouseEvent e) {
    this.popup(new WPoint(e.getDocument().x, e.getDocument().y));
  }

  public void setButton(WInteractWidget button) {
    this.button_ = button;
    if (this.button_ != null) {
      this.button_
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WPopupMenu.this.popupAtButton();
              });
      this.button_.addStyleClass("dropdown-toggle");
    }
  }
  /**
   * Shows the popup besides a widget.
   *
   * <p>
   *
   * @see WWidget#positionAt(WWidget widget, Orientation orientation)
   */
  public void popup(WWidget location, Orientation orientation) {
    this.location_ = location;
    this.popupImpl();
    this.doJavaScript(this.getJsRef() + ".wtObj.popupAt(" + location.getJsRef() + ");");
    this.positionAt(location, orientation);
  }
  /**
   * Shows the popup besides a widget.
   *
   * <p>Calls {@link #popup(WWidget location, Orientation orientation) popup(location,
   * Orientation.Vertical)}
   */
  public final void popup(WWidget location) {
    popup(location, Orientation.Vertical);
  }
  /**
   * Executes the the popup at a position.
   *
   * <p>Displays the popup at a point with document coordinates <code>p</code>, using {@link
   * WPopupMenu#popup(WPoint p) popup()}, and the waits until a menu item is selected, or the menu
   * is cancelled.
   *
   * <p>Returns the selected menu (or sub-menu) item, or <code>null</code> if the user cancelled the
   * menu.
   *
   * <p>
   *
   * @see WPopupMenu#popup(WPoint p)
   */
  public WMenuItem exec(final WPoint p) {
    if (this.recursiveEventLoop_) {
      throw new WException("WPopupMenu::exec(): already being executed.");
    }
    this.popup(p);
    this.exec();
    return this.result_;
  }
  /**
   * Executes the the popup at the location of a mouse event.
   *
   * <p>This is a convenience method for {@link WPopupMenu#exec(WPoint p) exec()} that uses the
   * event&apos;s document coordinates.
   *
   * <p>
   *
   * @see WPopupMenu#exec(WPoint p)
   */
  public WMenuItem exec(final WMouseEvent e) {
    return this.exec(new WPoint(e.getDocument().x, e.getDocument().y));
  }
  /**
   * Executes the popup besides a widget.
   *
   * <p>
   *
   * @see WWidget#positionAt(WWidget widget, Orientation orientation)
   */
  public WMenuItem exec(WWidget location, Orientation orientation) {
    if (this.recursiveEventLoop_) {
      throw new WException("WPopupMenu::exec(): already being executed.");
    }
    this.popup(location, orientation);
    this.exec();
    return this.result_;
  }
  /**
   * Executes the popup besides a widget.
   *
   * <p>Returns {@link #exec(WWidget location, Orientation orientation) exec(location,
   * Orientation.Vertical)}
   */
  public final WMenuItem exec(WWidget location) {
    return exec(location, Orientation.Vertical);
  }
  /**
   * Returns the last triggered menu item.
   *
   * <p>The result is <code>null</code> when the user cancelled the popup menu.
   */
  public WMenuItem getResult() {
    return this.result_;
  }

  public void setHidden(boolean hidden, final WAnimation animation) {
    super.setHidden(hidden, animation);
    if (this.cancel_.isConnected()
        || WApplication.getInstance().getSession().getRenderer().isPreLearning()) {
      this.doJavaScript(this.getJsRef() + ".wtObj.setHidden(" + (hidden ? "1" : "0") + ");");
    }
  }

  public void setMaximumSize(final WLength width, final WLength height) {
    super.setMaximumSize(width, height);
    this.getUl().setMaximumSize(width, height);
  }

  public void setMinimumSize(final WLength width, final WLength height) {
    super.setMinimumSize(width, height);
    this.getUl().setMinimumSize(width, height);
  }
  /**
   * Signal emitted when the popup is hidden.
   *
   * <p>Unlike the {@link WMenu#itemSelected()} signal, {@link WPopupMenu#aboutToHide()
   * aboutToHide()} is only emitted by the toplevel popup menu (and not by submenus), and is also
   * emitted when no item was selected.
   *
   * <p>You can use {@link WPopupMenu#getResult() getResult()} to get the selected item, which may
   * be <code>null</code>.
   *
   * <p>
   *
   * @see WPopupMenu#triggered()
   * @see WMenu#itemSelected()
   */
  public Signal aboutToHide() {
    return this.aboutToHide_;
  }
  /**
   * Signal emitted when an item is selected.
   *
   * <p>Unlike the {@link WMenu#itemSelected()} signal, {@link WPopupMenu#triggered() triggered()}
   * is only emitted by the toplevel popup menu (and not by submenus).
   *
   * <p>
   *
   * @see WPopupMenu#aboutToHide()
   * @see WMenu#itemSelected()
   */
  public Signal1<WMenuItem> triggered() {
    return this.triggered_;
  }
  /**
   * Configure auto-hide when the mouse leaves the menu.
   *
   * <p>If <code>enabled</code>, The popup menu will be hidden when the mouse leaves the menu for
   * longer than <code>autoHideDelay</code> (milliseconds). The popup menu result will be 0, as if
   * the user cancelled.
   *
   * <p>By default, this option is disabled.
   */
  public void setAutoHide(boolean enabled, int autoHideDelay) {
    if (enabled) {
      this.autoHideDelay_ = autoHideDelay;
    } else {
      this.autoHideDelay_ = -1;
    }
  }
  /**
   * Configure auto-hide when the mouse leaves the menu.
   *
   * <p>Calls {@link #setAutoHide(boolean enabled, int autoHideDelay) setAutoHide(enabled, 0)}
   */
  public final void setAutoHide(boolean enabled) {
    setAutoHide(enabled, 0);
  }
  /**
   * Set whether this popup menu should hide when an item is selected.
   *
   * <p>Defaults to true.
   *
   * <p>
   *
   * @see WPopupMenu#isHideOnSelect()
   */
  public void setHideOnSelect(boolean enabled) {
    this.hideOnSelect_ = enabled;
  }
  /**
   * Set whether this popup menu should hide when an item is selected.
   *
   * <p>Calls {@link #setHideOnSelect(boolean enabled) setHideOnSelect(true)}
   */
  public final void setHideOnSelect() {
    setHideOnSelect(true);
  }
  /**
   * Returns whether this popup menu should hide when an item is selected.
   *
   * <p>
   *
   * @see WPopupMenu#setHideOnSelect(boolean enabled)
   */
  public boolean isHideOnSelect() {
    return this.hideOnSelect_;
  }

  protected void renderSelected(WMenuItem item, boolean selected) {}

  protected void setCurrent(int index) {
    if (this.getContentsStack() != null) {
      super.setCurrent(index);
    }
  }

  void getSDomChanges(final List<DomElement> result, WApplication app) {
    super.getSDomChanges(result, app);
    this.willPopup_ = false;
  }

  protected void render(EnumSet<RenderFlag> flags) {
    super.render(flags);
    this.willPopup_ = false;
  }

  String renderRemoveJs(boolean recursive) {
    String result = super.renderRemoveJs(true);
    result += "Wt4.11.2.remove('" + this.getId() + "');";
    return result;
  }

  private WPopupMenu topLevel_;
  WMenuItem result_;
  private WWidget location_;
  private WInteractWidget button_;
  private Signal aboutToHide_;
  private Signal1<WMenuItem> triggered_;
  private JSignal cancel_;
  private boolean recursiveEventLoop_;
  private boolean willPopup_;
  private boolean hideOnSelect_;
  private int autoHideDelay_;

  private void exec() {
    WApplication app = WApplication.getInstance();
    this.recursiveEventLoop_ = true;
    if (app.getEnvironment().isTest()) {
      app.getEnvironment().popupExecuted().trigger(this);
      if (this.recursiveEventLoop_) {
        throw new WException("Test case must close popup menu.");
      }
    } else {
      do {
        app.waitForEvent();
      } while (this.recursiveEventLoop_);
    }
  }

  private void cancel() {
    if (this.willPopup_) {
      return;
    }
    if (!this.isHidden()) {
      this.done((WMenuItem) null);
    }
  }

  private void done(WMenuItem result) {
    if (this.isHidden()) {
      return;
    }
    if (this.location_ != null && this.location_ == this.button_) {
      this.button_.removeStyleClass("active", true);
      if (this.getParentItem() != null) {
        this.getParentItem().removeStyleClass("open");
      }
    }
    this.location_ = null;
    this.result_ = result;
    boolean shouldHide =
        !(result != null) || ((WPopupMenu) result.getParentMenu()).isHideOnSelect();
    if (shouldHide) {
      this.hide();
    }
    this.recursiveEventLoop_ = false;
    if (this.result_ != null) {
      this.triggered_.trigger(this.result_);
    }
    if (shouldHide) {
      this.aboutToHide_.trigger();
    }
  }

  private void popupImpl() {
    this.result_ = null;
    WApplication app = WApplication.getInstance();
    this.prepareRender(app);
    this.show();
    this.willPopup_ = true;
    this.scheduleRender();
  }

  private void prepareRender(WApplication app) {
    if (!this.cancel_.isConnected()) {
      app.loadJavaScript("js/WPopupMenu.js", wtjs1());
      StringBuilder s = new StringBuilder();
      s.append("new Wt4.11.2.WPopupMenu(")
          .append(app.getJavaScriptClass())
          .append(',')
          .append(this.getJsRef())
          .append(',')
          .append(this.autoHideDelay_)
          .append(");");
      this.setJavaScriptMember(" WPopupMenu", s.toString());
      this.cancel_.addListener(
          this,
          () -> {
            WPopupMenu.this.cancel();
          });
      this.connectSignals(this);
    }
    this.adjustPadding();
  }

  private void adjustPadding() {
    boolean needPadding = false;
    for (int i = 0; i < this.getCount(); ++i) {
      WMenuItem item = this.itemAt(i);
      if (item.getIcon().length() != 0 || item.isCheckable()) {
        needPadding = true;
        break;
      }
    }
    for (int i = 0; i < this.getCount(); ++i) {
      WMenuItem item = this.itemAt(i);
      item.setItemPadding(needPadding);
      WPopupMenu subMenu = ObjectUtils.cast(item.getMenu(), WPopupMenu.class);
      if (subMenu != null) {
        subMenu.adjustPadding();
      }
    }
  }

  private void popupAtButton() {
    if (!this.isHidden()) {
      return;
    }
    if (!(this.topLevel_ != null) || this.topLevel_ == this) {
      this.button_.addStyleClass("active", true);
      if (this.getParentItem() != null) {
        this.getParentItem().addStyleClass("open");
      }
      this.popup(this.button_);
    }
  }

  private void connectSignals(final WPopupMenu topLevel) {
    this.topLevel_ = topLevel;
    this.itemSelected()
        .addListener(
            topLevel,
            (WMenuItem e1) -> {
              topLevel.done(e1);
            });
    for (int i = 0; i < this.getCount(); ++i) {
      WMenuItem item = this.itemAt(i);
      WPopupMenu subMenu = ObjectUtils.cast(item.getMenu(), WPopupMenu.class);
      if (subMenu != null) {
        subMenu.connectSignals(topLevel);
      }
    }
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WPopupMenu",
        "(function(e,t,n){t.wtObj=this;const o=e.WT;let i=null,s=null,u=null;if(o.isIOS){t.addEventListener(\"touchstart\",T);t.addEventListener(\"touchend\",I)}function c(){a(t,null);t.style.display=\"none\";setTimeout((function(){e.emit(t.id,\"cancel\")}),0)}function r(e,t){e.classList.toggle(\"active\",t)}function d(e){if(e.subMenu)return e.subMenu;{const t=e.lastChild;if(t&&o.hasTag(t,\"UL\")){e.subMenu=t;t.parentItem=e;t.addEventListener(\"mousemove\",l);v(t);return t}return null}}function a(e,t){function n(e,t){if(e===t)return!0;if(t){const o=t.parentNode.parentItem;return!!o&&n(e,o)}return!1}!function e(o){for(const i of o.childNodes)if(n(i,t)){if(i!==t){const t=d(i);t&&e(t)}}else{r(i,!1);const t=d(i);if(t){t.style.display=\"none\";e(t)}}}(e)}function l(e){let n=o.target(e);for(;n&&!o.hasTag(n,\"LI\")&&!o.hasTag(n,\"UL\");)n=n.parentNode;if(o.hasTag(n,\"LI\")){if(n===s)return;s=n;r(n,!0);const e=d(n);e&&function(e){e.style.display=\"block\";if(e.parentNode===e.parentItem){e.parentNode.removeChild(e);t.parentNode.appendChild(e)}const n=o.px(e,\"paddingTop\")+o.px(e,\"borderTopWidth\");o.positionAtWidget(e.id,e.parentItem.id,o.Horizontal,-n);a(e,null);if(o.isIOS){e.removeEventListener(\"touchstart\",T);e.addEventListener(\"touchstart\",T);e.removeEventListener(\"touchend\",I);e.addEventListener(\"touchend\",I)}}(e);a(t,n)}}function f(){clearTimeout(i);n>=0&&(i=setTimeout(c,n))}function m(){clearTimeout(i)}function v(e){e.addEventListener(\"mouseleave\",f);e.addEventListener(\"mouseenter\",m)}function p(){return null!==document.getElementById(t.id)}function h(e){p()&&1!==o.button(e)&&c()}function E(){p()&&c()}function L(e){p()&&27===e.keyCode&&c()}this.setHidden=function(e){if(i){clearTimeout(i);i=null}s=null;if(e){t.style.position=\"\";t.style.display=\"\";t.style.left=\"\";t.style.top=\"\";document.removeEventListener(\"mousedown\",h);!function(){if(o.isIOS){document.removeEventListener(\"touchstart\",y);document.removeEventListener(\"touchend\",g)}else document.removeEventListener(\"click\",E)}();document.removeEventListener(\"keydown\",L)}else{setTimeout((function(){document.addEventListener(\"mousedown\",h);!function(){if(o.isIOS){document.addEventListener(\"touchstart\",y);document.addEventListener(\"touchend\",g)}else document.addEventListener(\"click\",E)}();document.addEventListener(\"keydown\",L)}),0);t.style.display=\"block\"}a(t,null)};this.popupAt=function(e){v(e)};function y(e){const t=e.originalEvent.touches;u=t.length>1?null:{x:t[0].screenX,y:t[0].screenY}}function T(e){e.stopPropagation()}function g(e){if(u){const t=e.originalEvent.changedTouches[0];Math.abs(u.x-t.screenX)<20&&Math.abs(u.y-t.screenY)<20&&E();u=null}}function I(e){e.stopPropagation()}setTimeout((function(){v(t)}),0);t.addEventListener(\"mousemove\",l)})");
  }
}
