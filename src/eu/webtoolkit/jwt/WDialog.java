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
 * A WDialog shows a dialog.
 *
 * <p>By default, the dialog is <i>modal</i>. A modal window blocks the user interface, and does not
 * allow the user to interact with any other part of the user interface until the dialog is closed
 * (this is enforced at the server side, so you may rely on this behavior).
 *
 * <p>A modal dialog can be instantiated synchronously or asynchronously. A non-modal dialog can
 * only be instantiated asynchronously.
 *
 * <p>When using a dialog asynchronously, there is no API call that waits for the dialog to be
 * closed. Then, the usage is similar to instantiating any other widget. The dialog may be closed by
 * calling {@link WDialog#accept() accept()}, {@link WDialog#reject() reject()} or {@link
 * WDialog#done(DialogCode result) done()} (or connecting a signal to one of these methods). This
 * will hide the dialog and emit the {@link WDialog#finished() finished()} signal, which you then
 * can listen for to process the dialog result and delete the dialog. Unlike other widgets, a dialog
 * does not need to be added to a parent widget, but is hidden by default. You must use the method
 * {@link WWidget#show()} or {@link WDialog#setHidden(boolean hidden, WAnimation animation)
 * setHidden()} to show the dialog.
 *
 * <p>The synchronous use of a dialog involves a call to {@link WDialog#exec(WAnimation animation)
 * exec()} which will block (suspend the thread) until the dialog window is closed, and return the
 * dialog result. Events within dialog are handled using a so-called recursive event loop.
 * Typically, an OK button will be connected to {@link WDialog#accept() accept()}, and in some cases
 * a {@link StandardButton#Cancel} button to {@link WDialog#reject() reject()}. This solution has
 * the drawback that it is not scalable to many concurrent sessions, since for every session with a
 * recursive event loop, a thread is locked until {@link WDialog#exec(WAnimation animation) exec()}
 * returns. A thread that is locked by a recursive event loop cannot be used to process requests
 * from another sessions. When all threads in the threadpool are locked in recursive event loops,
 * the server will be unresponsive to requests from any other session. In practical terms, this
 * means you must not use {@link WDialog#exec(WAnimation animation) exec()}, unless your application
 * will never be used by more concurrent users than the amount of threads in your threadpool (like
 * on some intranets or extranets). Using {@link WDialog#exec(WAnimation animation) exec()} is not
 * supported from outside the regular event loop (i.e. when taking a lock on a session using {@link
 * WApplication#getUpdateLock()} or by posting an event using WServer::post()). This functionality
 * is only available on Servlet 3.0 compatible servlet containers. Use {@link
 * WDialog#setModal(boolean modal) setModal()} to create a non-modal dialog. A non-modal dialog does
 * not block the underlying user interface: the user must not first deal with the dialog before
 * interacting with the rest of the user interface.
 *
 * <p>Contents for the dialog is defined by adding it to the {@link WDialog#getContents()
 * getContents()} widget.
 *
 * <p>This dialog looks like this (using the default css themes):
 *
 * <p>
 *
 * <table border="1" cellspacing="3" cellpadding="3">
 * <tr><td><div align="center">
 * <img src="doc-files/WDialog-default-1.png">
 * <p>
 * <strong>A simple custom dialog (default)</strong></p>
 * </div>
 *
 *
 * </td><td><div align="center">
 * <img src="doc-files/WDialog-polished-1.png">
 * <p>
 * <strong>A simple custom dialog (polished)</strong></p>
 * </div>
 *
 *
 * </td></tr>
 * </table>
 *
 * <p>
 *
 * <p><i><b>Note: </b>For the dialog (or rather, the silkscreen covering the user interface below)
 * to render properly in IE, the &quot;html body&quot; margin is set to 0 (if it wasn&apos;t
 * already). </i>
 */
public class WDialog extends WPopupWidget {
  private static Logger logger = LoggerFactory.getLogger(WDialog.class);

  /**
   * Constructs a new dialog.
   *
   * <p>Unlike other widgets, the dialog does not require a parent container since it is a top-level
   * widget.
   */
  public WDialog() {
    super(new WTemplate(tr("Wt.WDialog.template"), (WContainerWidget) null));
    this.moved_ = new JSignal2<Integer, Integer>(this, "moved") {};
    this.resized_ = new JSignal2<Integer, Integer>(this, "resized") {};
    this.zIndexChanged_ = new JSignal1<Integer>(this, "zIndexChanged") {};
    this.delayedJs_ = new ArrayList<String>();
    this.finished_ = new Signal1<DialogCode>();
    this.escapeConnection1_ = new AbstractSignal.Connection();
    this.escapeConnection2_ = new AbstractSignal.Connection();
    this.enterConnection1_ = new AbstractSignal.Connection();
    this.enterConnection2_ = new AbstractSignal.Connection();
    this.create();
  }
  /**
   * Constructs a dialog with a given window title.
   *
   * <p>Unlike other widgets, the dialog does not require a parent container since it is a top-level
   * widget.
   */
  public WDialog(final CharSequence windowTitle) {
    super(new WTemplate(tr("Wt.WDialog.template"), (WContainerWidget) null));
    this.moved_ = new JSignal2<Integer, Integer>(this, "moved") {};
    this.resized_ = new JSignal2<Integer, Integer>(this, "resized") {};
    this.zIndexChanged_ = new JSignal1<Integer>(this, "zIndexChanged") {};
    this.delayedJs_ = new ArrayList<String>();
    this.finished_ = new Signal1<DialogCode>();
    this.escapeConnection1_ = new AbstractSignal.Connection();
    this.escapeConnection2_ = new AbstractSignal.Connection();
    this.enterConnection1_ = new AbstractSignal.Connection();
    this.enterConnection2_ = new AbstractSignal.Connection();
    this.create();
    this.setWindowTitle(windowTitle);
  }
  /** Deletes a dialog. */
  public void remove() {
    this.hide();
    super.remove();
  }
  /**
   * Sets the dialog window title.
   *
   * <p>The window title is displayed in the title bar.
   *
   * <p>
   *
   * @see WDialog#setTitleBarEnabled(boolean enable)
   */
  public void setWindowTitle(final CharSequence windowTitle) {
    this.caption_.bindString("title", windowTitle, TextFormat.Plain);
  }
  /**
   * Returns the dialog window title.
   *
   * <p>
   *
   * @see WDialog#setWindowTitle(CharSequence windowTitle)
   */
  public WString getWindowTitle() {
    return this.caption_.resolveStringValue("title");
  }
  /**
   * Enables or disables the title bar.
   *
   * <p>The titlebar is enabled by default.
   */
  public void setTitleBarEnabled(boolean enable) {
    this.titleBar_.setHidden(!enable);
  }
  /**
   * Returns whether the title bar is enabled.
   *
   * <p>
   *
   * @see WDialog#setTitleBarEnabled(boolean enable)
   */
  public boolean isTitleBarEnabled() {
    return !this.titleBar_.isHidden();
  }
  /**
   * Returns the dialog title bar container.
   *
   * <p>The title bar contains a single text that contains the caption. You may customize the title
   * bar by for example adding other content.
   */
  public WContainerWidget getTitleBar() {
    return this.titleBar_;
  }
  /**
   * Returns the dialog contents container.
   *
   * <p>Content to the dialog window may be added to this container widget.
   */
  public WContainerWidget getContents() {
    return this.contents_;
  }
  /**
   * Returns the dialog footer container.
   *
   * <p>This is an optional section which is typically used for buttons.
   */
  public WContainerWidget getFooter() {
    if (!(this.footer_ != null)) {
      WContainerWidget footer = this.footer_ = new WContainerWidget();
      WApplication.getInstance().getTheme().apply(this, this.footer_, WidgetThemeRole.DialogFooter);
      WContainerWidget layoutContainer = (WContainerWidget) this.impl_.resolveWidget("layout");
      layoutContainer.getLayout().addWidget(footer);
    }
    return this.footer_;
  }
  /**
   * Executes the dialog in a recursive event loop.
   *
   * <p>Executes the dialog synchronously. This blocks the current thread of execution until one of
   * {@link WDialog#done(DialogCode result) done()}, {@link WDialog#accept() accept()} or {@link
   * WDialog#reject() reject()} is called.
   *
   * <p><i>Warning: using {@link WDialog#exec(WAnimation animation) exec()} does not scale to many
   * concurrent sessions, since the thread is locked until exec returns, so the entire server will
   * be unresponsive when the thread pool is exhausted.</i>
   *
   * <p><i>This functionality is only available on Servlet 3.0 compatible servlet containers.</i>
   *
   * <p>
   *
   * @see WDialog#done(DialogCode result)
   * @see WDialog#accept()
   * @see WDialog#reject()
   */
  public DialogCode exec(final WAnimation animation) {
    if (this.recursiveEventLoop_) {
      throw new WException("WDialog::exec(): already being executed.");
    }
    this.animateShow(animation);
    if (!WtServlet.isAsyncSupported()) {
      throw new WException(
          "WDialog#exec() requires a Servlet 3.0 enabled servlet container and an application with async-supported enabled.");
    }
    WApplication app = WApplication.getInstance();
    this.recursiveEventLoop_ = true;
    if (app.getEnvironment().isTest()) {
      app.getEnvironment().dialogExecuted().trigger(this);
      if (this.recursiveEventLoop_) {
        throw new WException("Test case must close dialog");
      }
    } else {
      do {
        app.waitForEvent();
      } while (this.recursiveEventLoop_);
    }
    this.hide();
    return this.result_;
  }
  /**
   * Executes the dialog in a recursive event loop.
   *
   * <p>Returns {@link #exec(WAnimation animation) exec(new WAnimation())}
   */
  public final DialogCode exec() {
    return exec(new WAnimation());
  }
  /**
   * Stops the dialog.
   *
   * <p>Sets the dialog result, and emits the {@link WDialog#finished() finished()} signal.
   *
   * <p>
   *
   * @see WDialog#finished()
   * @see WDialog#getResult()
   */
  public void done(DialogCode result) {
    if (this.isHidden()) {
      return;
    }
    this.result_ = result;
    if (this.recursiveEventLoop_) {
      this.recursiveEventLoop_ = false;
    } else {
      this.hide();
    }
    this.finished_.trigger(result);
  }
  /**
   * Closes the dialog, with result is Accepted.
   *
   * <p>
   *
   * @see WDialog#done(DialogCode result)
   * @see WDialog#reject()
   */
  public void accept() {
    this.done(DialogCode.Accepted);
  }
  /**
   * Closes the dialog, with result is Rejected.
   *
   * <p>
   *
   * @see WDialog#done(DialogCode result)
   * @see WDialog#accept()
   */
  public void reject() {
    this.done(DialogCode.Rejected);
  }
  /**
   * Lets pressing the escape key reject the dialog.
   *
   * <p>Before JWt 3.1.5, pressing escape automatically rejected the dialog. Since 3.1.4 this
   * behaviour is no longer the default since it may interfere with other functionality in the
   * dialog. Use this method to enable this behaviour.
   *
   * <p>
   *
   * @see WDialog#reject()
   */
  public void rejectWhenEscapePressed(boolean enable) {
    this.escapeIsReject_ = enable;
  }
  /**
   * Lets pressing the escape key reject the dialog.
   *
   * <p>Calls {@link #rejectWhenEscapePressed(boolean enable) rejectWhenEscapePressed(true)}
   */
  public final void rejectWhenEscapePressed() {
    rejectWhenEscapePressed(true);
  }
  /**
   * Signal emitted when the dialog is closed.
   *
   * <p>
   *
   * @see WDialog#done(DialogCode result)
   * @see WDialog#accept()
   * @see WDialog#reject()
   */
  public Signal1<DialogCode> finished() {
    return this.finished_;
  }
  /**
   * Returns the result that was set for this dialog.
   *
   * <p>
   *
   * @see WDialog#done(DialogCode result)
   */
  public DialogCode getResult() {
    return this.result_;
  }
  /**
   * Sets whether the dialog is modal.
   *
   * <p>A modal dialog will block the underlying user interface. A modal dialog can be shown
   * synchronously or asynchronously. A non-modal dialog can only be shown asynchronously.
   *
   * <p>By default a dialog is modal.
   */
  public void setModal(boolean modal) {
    this.modal_ = modal;
  }
  /**
   * Returns whether the dialog is modal.
   *
   * <p>
   *
   * @see WDialog#setModal(boolean modal)
   */
  public boolean isModal() {
    return this.modal_;
  }
  /**
   * Adds a resize handle to the dialog.
   *
   * <p>The resize handle is shown in the bottom right corner of the dialog, and allows the user to
   * resize the dialog (but not smaller than the content allows).
   *
   * <p>This also sets the minimum width and height to {@link WLength#Auto} to use the initial width
   * and height as minimum sizes. You may want to provide other values for minimum width and height
   * to allow the dialog to be reduced in size.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>
   *
   * @see WDialog#setMinimumSize(WLength width, WLength height)
   * @see WDialog#setMaximumSize(WLength width, WLength height)
   */
  public void setResizable(boolean resizable) {
    if (resizable != this.resizable_) {
      this.resizable_ = resizable;
      this.toggleStyleClass("Wt-resizable", resizable);
      this.setSelectable(!resizable);
      if (resizable) {
        this.contents_.setSelectable(true);
      }
      if (this.resizable_) {
        Resizable.loadJavaScript(WApplication.getInstance());
        this.setJavaScriptMember(
            " Resizable",
            "(new Wt4.11.2.Resizable(Wt4.11.2,"
                + this.getJsRef()
                + ")).onresize(function(w, h, done) {var obj = "
                + this.getJsRef()
                + ".wtObj;if (obj) obj.onresize(w, h, done); });");
      }
    }
  }
  /**
   * Returns whether the dialog has a resize handle.
   *
   * <p>
   *
   * @see WDialog#setResizable(boolean resizable)
   */
  public boolean isResizable() {
    return this.resizable_;
  }
  /**
   * Allows the dialog to be moved.
   *
   * <p>The dialog can be moved by grabbing the titlebar.
   *
   * <p>The default value is <code>true</code>.
   */
  public void setMovable(boolean movable) {
    this.movable_ = movable;
    this.layoutContainer_.toggleStyleClass("movable", this.movable_);
  }
  /**
   * Returns whether the dialog can be moved.
   *
   * <p>
   *
   * @see WDialog#setMovable(boolean movable)
   */
  public boolean isMovable() {
    return this.movable_;
  }
  /**
   * Adds a close button to the titlebar.
   *
   * <p>The close button is shown in the title bar. Clicking the close button will reject the
   * dialog.
   */
  public void setClosable(boolean closable) {
    if (closable == this.isClosable()) {
      return;
    }
    if (closable) {
      WTheme theme = WApplication.getInstance().getTheme();
      if (ObjectUtils.cast(theme, WBootstrap5Theme.class) != null) {
        this.closeIcon_ = new WPushButton((WContainerWidget) this.titleBar_);
      } else {
        this.closeIcon_ = new WText();
        this.titleBar_.insertWidget(0, this.closeIcon_);
      }
      theme.apply(this, this.closeIcon_, WidgetThemeRole.DialogCloseIcon);
      this.closeIcon_
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WDialog.this.reject();
              });
    } else {
      {
        WWidget toRemove = WidgetUtils.remove(this.titleBar_, this.closeIcon_);
        if (toRemove != null) toRemove.remove();
      }

      this.closeIcon_ = null;
    }
  }
  /** Returns whether the dialog can be closed. */
  public boolean isClosable() {
    return this.closeIcon_ != null;
  }
  /**
   * Set focus on the first widget in the dialog.
   *
   * <p>Autofocus is enabled by default. If a widget inside of this dialog already has focus, the
   * focus will not be changed.
   */
  public void setAutoFocus(boolean enable) {
    this.autoFocus_ = enable;
  }

  public void setHidden(boolean hidden, final WAnimation animation) {
    if (this.contents_ != null && this.isHidden() != hidden) {
      if (!hidden) {
        if (this.footer_ != null) {
          for (int i = 0; i < this.getFooter().getCount(); ++i) {
            WPushButton b = ObjectUtils.cast(this.getFooter().getWidget(i), WPushButton.class);
            if (b != null && b.isDefault()) {
              this.enterConnection1_ =
                  this.enterPressed()
                      .addListener(
                          this,
                          () -> {
                            WDialog.this.onDefaultPressed();
                          });
              this.enterConnection2_ =
                  this.impl_
                      .enterPressed()
                      .addListener(
                          this,
                          () -> {
                            WDialog.this.onDefaultPressed();
                          });
              break;
            }
          }
        }
        if (this.escapeIsReject_) {
          if (this.isModal()) {
            this.escapeConnection1_ =
                this.escapePressed()
                    .addListener(
                        this,
                        () -> {
                          WDialog.this.onEscapePressed();
                        });
          } else {
            this.escapeConnection1_ =
                WApplication.getInstance()
                    .globalEscapePressed()
                    .addListener(
                        this,
                        () -> {
                          WDialog.this.onEscapePressed();
                        });
          }
          this.escapeConnection2_ =
              this.impl_
                  .escapePressed()
                  .addListener(
                      this,
                      () -> {
                        WDialog.this.onEscapePressed();
                      });
        }
      } else {
        this.escapeConnection1_.disconnect();
        this.escapeConnection2_.disconnect();
        this.enterConnection1_.disconnect();
        this.enterConnection2_.disconnect();
      }
      DialogCover c = this.getCover();
      if (!hidden) {
        if (!WWebWidget.canOptimizeUpdates() || this.isRendered()) {
          this.doJavaScript(
              "var o = " + this.getJsRef() + ";if (o && o.wtObj) o.wtObj.centerDialog();");
        }
        if (c != null) {
          c.pushDialog(this, animation);
        }
        if (this.modal_) {
          this.doJSAfterLoad(
              "try {var ae=document.activeElement;if (ae && ae.blur && ae.nodeName != 'BODY') {document.activeElement.blur();}} catch (e) { }");
        }
      } else {
        if (c != null) {
          c.popDialog(this, animation);
        }
      }
    }
    super.setHidden(hidden, animation);
  }

  public void positionAt(WWidget widget, Orientation orientation) {
    this.setPositionScheme(PositionScheme.Absolute);
    if (WApplication.getInstance().getEnvironment().hasJavaScript()) {
      this.setOffsets(new WLength(0), EnumSet.of(Side.Left, Side.Top));
    }
    super.positionAt(widget, orientation);
  }
  /** Set the position of the widget at the mouse position. */
  public void positionAt(final WMouseEvent ev) {
    this.setPositionScheme(PositionScheme.Fixed);
    if (WApplication.getInstance().getEnvironment().hasJavaScript()) {
      this.setOffsets(new WLength(ev.getWindow().x), EnumSet.of(Side.Left));
      this.setOffsets(new WLength(ev.getWindow().y), EnumSet.of(Side.Top));
    }
  }
  /** Raises this dialog to be the top-most dialog. */
  public void raiseToFront() {
    this.doJSAfterLoad(this.getJsRef() + ".wtObj.bringToFront()");
    DialogCover c = this.getCover();
    c.bringToFront(this);
  }

  public void setMinimumSize(final WLength width, final WLength height) {
    super.setMinimumSize(width, height);
    this.impl_.resolveWidget("layout").setMinimumSize(width, height);
  }

  public void setMaximumSize(final WLength width, final WLength height) {
    super.setMaximumSize(width, height);
    WLength w = width.getUnit() != LengthUnit.Percentage ? width : WLength.Auto;
    WLength h = height.getUnit() != LengthUnit.Percentage ? height : WLength.Auto;
    this.impl_.resolveWidget("layout").setMaximumSize(w, h);
  }
  /**
   * Signal emitted when the dialog is being resized by the user.
   *
   * <p>The information passed are the new width and height.
   *
   * <p>
   *
   * @see WDialog#setResizable(boolean resizable)
   */
  public JSignal2<Integer, Integer> resized() {
    return this.resized_;
  }
  /**
   * Signal emitted when the dialog is being moved by the user.
   *
   * <p>The information passed are the new x and y position (relative to the wietdow).
   */
  public JSignal2<Integer, Integer> moved() {
    return this.moved_;
  }
  /**
   * Event signal emitted when a keyboard key is pushed down.
   *
   * <p>The event will be triggered if nothing in the {@link WDialog} has focus
   *
   * <p>
   */
  public EventSignal1<WKeyEvent> keyWentDown() {
    return this.layoutContainer_.keyWentDown();
  }
  /**
   * Event signal emitted when a &quot;character&quot; was entered.
   *
   * <p>The event will be triggered if nothing in the {@link WDialog} has focus
   *
   * <p>
   */
  public EventSignal1<WKeyEvent> keyPressed() {
    return this.layoutContainer_.keyPressed();
  }
  /**
   * Event signal emitted when a keyboard key is released.
   *
   * <p>The event will be triggered if nothing in the {@link WDialog} has focus
   *
   * <p>
   */
  public EventSignal1<WKeyEvent> keyWentUp() {
    return this.layoutContainer_.keyWentUp();
  }
  /**
   * Event signal emitted when enter was pressed.
   *
   * <p>The event will be triggered if nothing in the {@link WDialog} has focus
   *
   * <p>
   */
  public EventSignal enterPressed() {
    return this.layoutContainer_.enterPressed();
  }
  /**
   * Event signal emitted when escape was pressed.
   *
   * <p>The event will be triggered if nothing in the {@link WDialog} has focus
   *
   * <p>
   */
  public EventSignal escapePressed() {
    return this.layoutContainer_.escapePressed();
  }
  /** Event signal emitted when a finger is placed on the screen. */
  public EventSignal1<WTouchEvent> touchStarted() {
    return this.layoutContainer_.touchStarted();
  }
  /** Event signal emitted when a finger is removed from the screen. */
  public EventSignal1<WTouchEvent> touchEnded() {
    return this.layoutContainer_.touchEnded();
  }
  /**
   * Event signal emitted when a finger, which is already placed on the screen, is moved across the
   * screen.
   */
  public EventSignal1<WTouchEvent> touchMoved() {
    return this.layoutContainer_.touchMoved();
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (flags.contains(RenderFlag.Full)) {
      WApplication app = WApplication.getInstance();
      boolean centerX = this.getOffset(Side.Left).isAuto() && this.getOffset(Side.Right).isAuto();
      boolean centerY = this.getOffset(Side.Top).isAuto() && this.getOffset(Side.Bottom).isAuto();
      if (app.getEnvironment().hasAjax()) {
        if (this.getWidth().isAuto()) {
          if (this.getMaximumWidth().getUnit() == LengthUnit.Percentage
              || this.getMaximumWidth().toPixels() == 0) {
            this.impl_
                .resolveWidget("layout")
                .setMaximumSize(new WLength(999999), this.getMaximumHeight());
          }
        }
      }
      this.doJavaScript(
          "new Wt4.11.2.WDialog("
              + app.getJavaScriptClass()
              + ","
              + this.getJsRef()
              + ","
              + this.titleBar_.getJsRef()
              + ","
              + (this.movable_ ? "1" : "0")
              + ","
              + (centerX ? "1" : "0")
              + ","
              + (centerY ? "1" : "0")
              + ","
              + (this.moved_.isConnected() ? '"' + this.moved_.getName() + '"' : "null")
              + ","
              + (this.resized_.isConnected() ? '"' + this.resized_.getName() + '"' : "null")
              + ",\""
              + this.zIndexChanged_.getName()
              + '"'
              + ");");
      for (int i = 0; i < this.delayedJs_.size(); ++i) {
        this.doJavaScript(this.delayedJs_.get(i));
      }
      this.delayedJs_.clear();
      if (!app.getEnvironment().agentIsIElt(9) && !app.getEnvironment().hasAjax()) {
        String js = WString.tr("Wt.WDialog.CenterJS").toXhtml();
        StringUtils.replace(js, "$el", "'" + this.getId() + "'");
        StringUtils.replace(js, "$centerX", centerX ? "1" : "0");
        StringUtils.replace(js, "$centerY", centerY ? "1" : "0");
        this.impl_.bindString(
            "center-script", "<script>" + js + "</script>", TextFormat.UnsafeXHTML);
      } else {
        this.impl_.bindEmpty("center-script");
      }
    }
    if (!this.isModal()) {
      this.impl_
          .mouseWentDown()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WDialog.this.bringToFront(e1);
              });
    }
    if (flags.contains(RenderFlag.Full) && this.autoFocus_) {
      if (!(this.impl_.findById(WApplication.getInstance().getFocus()) != null)) {
        this.impl_.isSetFirstFocus();
      }
    }
    super.render(flags);
  }

  protected void onPathChange() {}

  private WTemplate impl_;
  private WTemplate caption_;
  private WInteractWidget closeIcon_;
  private WContainerWidget titleBar_;
  private WContainerWidget contents_;
  WContainerWidget layoutContainer_;
  private WContainerWidget footer_;
  private boolean modal_;
  private boolean resizable_;
  private boolean movable_;
  private boolean escapeIsReject_;
  private boolean autoFocus_;
  private JSignal2<Integer, Integer> moved_;
  private JSignal2<Integer, Integer> resized_;
  private JSignal1<Integer> zIndexChanged_;
  private List<String> delayedJs_;
  private Signal1<DialogCode> finished_;
  private DialogCode result_;
  private boolean recursiveEventLoop_;
  private AbstractSignal.Connection escapeConnection1_;
  private AbstractSignal.Connection escapeConnection2_;
  private AbstractSignal.Connection enterConnection1_;
  private AbstractSignal.Connection enterConnection2_;

  private void create() {
    this.closeIcon_ = null;
    this.footer_ = null;
    this.modal_ = true;
    this.resizable_ = false;
    this.recursiveEventLoop_ = false;
    this.escapeIsReject_ = false;
    this.autoFocus_ = true;
    this.impl_ = ObjectUtils.cast(this.getImplementation(), WTemplate.class);
    String CSS_RULES_NAME = "Wt::WDialog";
    WApplication app = WApplication.getInstance();
    if (!app.getStyleSheet().isDefined(CSS_RULES_NAME)) {
      if (app.getEnvironment().agentIsIElt(9)) {
        app.getStyleSheet().addRule("body", "height: 100%;");
      }
      String position = app.getEnvironment().getAgent() == UserAgent.IE6 ? "absolute" : "fixed";
      app.getStyleSheet()
          .addRule(
              "div.Wt-dialog",
              ""
                  + (!app.getEnvironment().hasAjax()
                      ? "left: 50%; top: 50%;margin-left: -100px; margin-top: -50px;"
                      : "left: 0px; top: 0px;"),
              CSS_RULES_NAME);
      if (app.getEnvironment().getAgent() == UserAgent.IE6) {
        app.getStyleSheet()
            .addRule(
                "div.Wt-dialogcover",
                "position: absolute;left: expression((ignoreMe2 = document.documentElement.scrollLeft) + 'px' );top: expression((ignoreMe = document.documentElement.scrollTop) + 'px' );");
        if (!app.getEnvironment().hasAjax()) {
          app.getStyleSheet()
              .addRule(
                  "div.Wt-dialog",
                  "position: absolute;left: expression((ignoreMe2 = document.documentElement.scrollLeft + document.documentElement.clientWidth/2) + 'px' );top: expression((ignoreMe = document.documentElement.scrollTop + document.documentElement.clientHeight/2) + 'px' );");
        }
      }
    }
    app.loadJavaScript("js/WDialog.js", wtjs1());
    WContainerWidget layoutContainer = new WContainerWidget();
    this.layoutContainer_ = layoutContainer;
    layoutContainer.setGlobalUnfocused(true);
    WApplication.getInstance()
        .getTheme()
        .apply(this, layoutContainer, WidgetThemeRole.DialogContent);
    layoutContainer.addStyleClass("dialog-layout");
    WVBoxLayout layoutPtr = new WVBoxLayout();
    WVBoxLayout layout = layoutPtr;
    layout.setContentsMargins(0, 0, 0, 0);
    layout.setSpacing(0);
    layoutContainer.setLayout(layoutPtr);
    this.impl_.bindWidget("layout", layoutContainer);
    this.titleBar_ = new WContainerWidget();
    app.getTheme().apply(this, this.titleBar_, WidgetThemeRole.DialogTitleBar);
    this.caption_ = new WTemplate(tr("Wt.WDialog.titlebar"), (WContainerWidget) this.titleBar_);
    this.contents_ = new WContainerWidget();
    app.getTheme().apply(this, this.contents_, WidgetThemeRole.DialogBody);
    layout.addWidget(this.titleBar_);
    layout.addWidget(this.contents_, 1);
    if (app.getEnvironment().hasAjax()) {
      this.impl_.setMargin(new WLength(0));
      if (!app.getEnvironment().agentIsIElt(9)) {
        this.setPositionScheme(PositionScheme.Fixed);
      }
    } else {
      this.setPositionScheme(
          app.getEnvironment().getAgent() == UserAgent.IE6
              ? PositionScheme.Absolute
              : PositionScheme.Fixed);
    }
    this.setMovable(true);
    this.zIndexChanged_.addListener(
        this,
        (Integer e1) -> {
          WDialog.this.zIndexChanged(e1);
        });
  }

  private void onEscapePressed() {
    DialogCover c = this.getCover();
    if (c != null && c.isTopDialogRendered(this)) {
      this.reject();
    }
  }

  private void onDefaultPressed() {
    DialogCover c = this.getCover();
    if (this.footer_ != null && c != null && c.isTopDialogRendered(this)) {
      for (int i = 0; i < this.getFooter().getCount(); ++i) {
        WPushButton b = ObjectUtils.cast(this.getFooter().getWidget(i), WPushButton.class);
        if (b != null && b.isDefault()) {
          if (b.isEnabled()) {
            b.clicked().trigger(new WMouseEvent());
          }
          break;
        }
      }
    }
  }

  private void bringToFront(final WMouseEvent e) {
    if (e.getButton() == MouseButton.Left && e.getModifiers().equals(KeyboardModifier.None)) {
      this.raiseToFront();
    }
  }

  private void zIndexChanged(int zIndex) {
    this.impl_.layoutImpl_.zIndex_ = zIndex;
  }

  void doJSAfterLoad(String js) {
    if (this.isRendered()) {
      this.doJavaScript(js);
    } else {
      this.delayedJs_.add(js);
    }
  }

  private DialogCover getCover() {
    WApplication app = WApplication.getInstance();
    if (app.getDomRoot() != null) {
      WWidget w = app.findWidget("dialog-cover");
      if (w != null) {
        return ObjectUtils.cast(w, DialogCover.class);
      } else {
        DialogCover d = new DialogCover();
        DialogCover result = d;
        app.addGlobalWidget(result);
        return result;
      }
    } else {
      return null;
    }
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WDialog",
        "(function(t,e,i,o,s,n,l,y,f){e.wtObj=this;const p=this,d=e.querySelector(\".dialog-layout\"),a=t.WT;let r,x,h=-1,u=-1,c=-1,w=-1,g=!1,m=-1,b=-1,z=-1,S=-1;function M(){if(l){const i=a.pxself(e,\"left\"),o=a.pxself(e,\"top\");if(i!==h||o!==u){h=i;u=o;t.emit(e,l,h,u)}}}function v(i,o){if(!g&&(i!==c||o!==w)){c=i;w=o;y&&t.emit(e,y,c,w)}}function E(t){const i=t||window.event,o=a.pageCoordinates(i),l=a.windowCoordinates(i),y=a.windowSize();if(l.x>0&&l.x<y.x&&l.y>0&&l.y<y.y){s=n=!1;if(\"auto\"===e.style.right||\"\"===e.style.right){e.style.left=a.px(e,\"left\")+o.x-r+\"px\";e.style.right=\"\"}else{e.style.right=a.px(e,\"right\")+r-o.x+\"px\";e.style.left=\"auto\"}if(\"auto\"===e.style.bottom||\"\"===e.style.bottom){e.style.top=a.px(e,\"top\")+o.y-x+\"px\";e.style.bottom=\"\"}else{e.style.bottom=a.px(e,\"bottom\")+x-o.y+\"px\";e.style.top=\"auto\"}r=o.x;x=o.y}}if(i&&o){i.onmousedown=function(t){const e=t||window.event;a.capture(i);const o=a.pageCoordinates(e);r=o.x;x=o.y;i.onmousemove=E};i.onmouseup=function(t){i.onmousemove=null;M();a.capture(null)}}this.centerDialog=function(){const t=a.parsePct(a.css(e,\"max-width\"),0),o=a.parsePct(a.css(e,\"max-height\"),0);if(0!==t){const e=a.windowSize(),i=d.firstChild.wtLayout;i&&i.setMaxSize&&i.setMaxSize(e.x*t/100,e.y*o/100)}if(null!==e.parentNode){if(\"none\"!==e.style.display){const t=a.windowSize(),i=e.offsetWidth,o=e.offsetHeight;-1!==m&&(s=!0);-1!==b&&(n=!0);if(s){e.style.left=Math.round((t.x-i)/2+(a.isIE6?document.documentElement.scrollLeft:0))+\"px\";e.style.marginLeft=\"0px\"}if(n){e.style.top=Math.round((t.y-o)/2+(a.isIE6?document.documentElement.scrollTop:0))+\"px\";e.style.marginTop=\"0px\"}\"\"!==e.style.position&&(e.style.visibility=\"visible\");M()}}else e=i=null};function C(){return a.windowSize().x*m/100}function D(){return a.windowSize().y*b/100}this.bringToFront=function(){const i=a.maxZIndex();if(i>e.style.zIndex){const o=i+1;e.style.zIndex=o;t.emit(e,f,o)}};this.onresize=function(i,o,l){s=n=!1;g=!l;!function(t,i,o,s){if(s){if(i>0){if(!a.boxSizing(d)){i-=a.px(d,\"border-left-width\");i-=a.px(d,\"border-right-width\");i-=a.px(d,\"padding-left\");i-=a.px(d,\"padding-right\")}d.style.width=`${i}px`}if(o>0){if(!a.boxSizing(d)){o-=a.px(d,\"border-top-width\");o-=a.px(d,\"border-bottom-width\");o-=a.px(d,\"padding-top\");o-=a.px(d,\"padding-bottom\")}d.style.height=`${o}px`}}p.centerDialog();e.wtResize&&e.wtResize(e,i,o,!0)}(0,i,o,!0);const y=d.firstChild.wtLayout;y&&y.setMaxSize&&y.setMaxSize(0,0);t.layouts2&&t.layouts2.scheduleAdjust();l&&v(i,o)};d.wtResize=function(t,i,o,s){\"\"===e.style.position&&(e.style.position=a.isIE6?\"absolute\":\"fixed\");e.style.visibility=\"visible\";b=a.parsePct(e.style.height,b);m=a.parsePct(e.style.width,m);if(s){e.style.height=Math.max(0,o)+\"px\";e.style.width=Math.max(0,i)+\"px\"}v(i,o);p.centerDialog();const n=-1!==m,l=-1!==b;if(n&&l){z=C();S=D();p.onresize(z,S,!0)}else if(n){z=C();p.onresize(z,o,!0)}else if(l){S=D();p.onresize(i,S,!0)}};e.wtPosition=function(){p.centerDialog();t.layouts2&&t.layouts2.adjust()};\"\"!==e.style.width&&(a.parsePx(e.style.width)>0?d.style.width=e.style.width:d.style.width=e.offsetWidth+\"px\");\"\"!==e.style.height&&(a.parsePx(e.style.height)>0?d.style.height=e.style.height:d.style.height=e.offsetHeight+\"px\");p.centerDialog()})");
  }
}
