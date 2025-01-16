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
 * An abstract widget that can receive user-interface interaction.
 *
 * <p>This abstract widget provides access to event signals that correspond to user-interface
 * interaction through mouse or keyboard.
 *
 * <p>When JavaScript is disabled, only the {@link WInteractWidget#clicked() clicked()} event will
 * propagate (but without event details information).
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>Styling through CSS is not applicable.
 */
public abstract class WInteractWidget extends WWebWidget {
  private static Logger logger = LoggerFactory.getLogger(WInteractWidget.class);

  /** Create an InteractWidget. */
  public WInteractWidget(WContainerWidget parentContainer) {
    super();
    this.dragSlot_ = null;
    this.dragTouchSlot_ = null;
    this.dragTouchEndSlot_ = null;
    this.mouseOverDelay_ = 0;
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Create an InteractWidget.
   *
   * <p>Calls {@link #WInteractWidget(WContainerWidget parentContainer)
   * this((WContainerWidget)null)}
   */
  public WInteractWidget() {
    this((WContainerWidget) null);
  }

  public void remove() {
    super.remove();
  }
  /**
   * Event signal emitted when a keyboard key is pushed down.
   *
   * <p>The keyWentDown signal is the first signal emitted when a key is pressed (before the
   * keyPressed signal). Unlike {@link WInteractWidget#keyPressed() keyPressed()} however it is also
   * emitted for modifier keys (such as &quot;shift&quot;, &quot;control&quot;, ...) or keyboard
   * navigation keys that do not have a corresponding character.
   *
   * <p>Form widgets (like {@link WLineEdit}) will receive key events when focussed. Other widgets
   * will receive key events when they contain (directly or indirectly) a form widget that has
   * focus.
   *
   * <p>To capture a key down event when no element has focus, see {@link
   * WApplication#globalKeyWentDown()}
   *
   * <p>
   *
   * @see WInteractWidget#keyPressed()
   * @see WInteractWidget#keyWentUp()
   */
  public EventSignal1<WKeyEvent> keyWentDown() {
    return this.keyEventSignal(KEYDOWN_SIGNAL, true);
  }
  /**
   * Event signal emitted when a &quot;character&quot; was entered.
   *
   * <p>The keyPressed signal is emitted when a key is pressed, and a character is entered. Unlike
   * {@link WInteractWidget#keyWentDown() keyWentDown()}, it is emitted only for key presses that
   * result in a character being entered, and thus not for modifier keys or keyboard navigation
   * keys.
   *
   * <p>Form widgets (like {@link WLineEdit}) will receive key events when focussed. Other widgets
   * will receive key events when they contain (directly or indirectly) a form widget that has
   * focus.
   *
   * <p>To capture a key press when no element has focus, see {@link
   * WApplication#globalKeyPressed()}
   *
   * <p>
   *
   * @see WInteractWidget#keyWentDown()
   */
  public EventSignal1<WKeyEvent> keyPressed() {
    return this.keyEventSignal(KEYPRESS_SIGNAL, true);
  }
  /**
   * Event signal emitted when a keyboard key is released.
   *
   * <p>This is the counter-part of the {@link WInteractWidget#keyWentDown() keyWentDown()} event.
   * Every key-down has its corresponding key-up.
   *
   * <p>Form widgets (like {@link WLineEdit}) will receive key events when focussed. Other widgets
   * will receive key events when they contain (directly or indirectly) a form widget that has
   * focus.
   *
   * <p>To capture a key up event when no element has focus, see {@link
   * WApplication#globalKeyWentUp()}
   *
   * <p>
   *
   * @see WInteractWidget#keyWentDown()
   */
  public EventSignal1<WKeyEvent> keyWentUp() {
    return this.keyEventSignal(KEYUP_SIGNAL, true);
  }
  /**
   * Event signal emitted when enter was pressed.
   *
   * <p>This signal is emitted when the Enter or Return key was pressed.
   *
   * <p>Form widgets (like {@link WLineEdit}) will receive key events when focussed. Other widgets
   * will receive key events when they contain (directly or indirectly) a form widget that has
   * focus.
   *
   * <p>To capture an enter press when no element has focus, see {@link
   * WApplication#globalEnterPressed()}
   *
   * <p>
   *
   * @see WInteractWidget#keyPressed()
   * @see Key#Enter
   */
  public EventSignal enterPressed() {
    return this.voidEventSignal(ENTER_PRESS_SIGNAL, true);
  }
  /**
   * Event signal emitted when escape was pressed.
   *
   * <p>This signal is emitted when the Escape key was pressed.
   *
   * <p>Form widgets (like {@link WLineEdit}) will receive key events when focussed. Other widgets
   * will receive key events when they contain (directly or indirectly) a form widget that has
   * focus.
   *
   * <p>To capture an escape press when no element has focus, see {@link
   * WApplication#globalEscapePressed()}
   *
   * <p>
   *
   * @see WInteractWidget#keyPressed()
   * @see Key#Escape
   */
  public EventSignal escapePressed() {
    return this.voidEventSignal(ESCAPE_PRESS_SIGNAL, true);
  }
  /**
   * Event signal emitted when the primary mouse button was clicked on this widget.
   *
   * <p>The event details contains information such as the {@link WMouseEvent#getButton()}, optional
   * {@link WMouseEvent#getModifiers() keyboard modifiers}, and mouse coordinates relative to the
   * {@link WMouseEvent#getWidget()}, the window {@link WMouseEvent#getWindow()}, or the {@link
   * WMouseEvent#getDocument()}.
   *
   * <p>For more details, see the <a
   * href="https://developer.mozilla.org/en-US/docs/Web/API/Element/click_event">MDN
   * documentation</a>.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the event details contain invalid information.
   * </i>
   */
  public EventSignal1<WMouseEvent> clicked() {
    return this.mouseEventSignal(M_CLICK_SIGNAL, true);
  }
  /**
   * Event signal emitted when the primary mouse button was double clicked on this widget.
   *
   * <p>The event details contains information such as the {@link WMouseEvent#getButton()}, optional
   * {@link WMouseEvent#getModifiers() keyboard modifiers}, and mouse coordinates relative to the
   * {@link WMouseEvent#getWidget()}, the window {@link WMouseEvent#getWindow()}, or the {@link
   * WMouseEvent#getDocument()}.
   *
   * <p>For more details, see the <a
   * href="https://developer.mozilla.org/en-US/docs/Web/API/Element/click_event">MDN
   * documentation</a>.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> doubleClicked() {
    return this.mouseEventSignal(DBL_CLICK_SIGNAL, true);
  }
  /**
   * Event signal emitted when a mouse button was pushed down on this widget.
   *
   * <p>The event details contains information such as the {@link WMouseEvent#getButton()}, optional
   * {@link WMouseEvent#getModifiers() keyboard modifiers}, and mouse coordinates relative to the
   * {@link WMouseEvent#getWidget()}, the window {@link WMouseEvent#getWindow()}, or the {@link
   * WMouseEvent#getDocument()}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> mouseWentDown() {
    return this.mouseEventSignal(MOUSE_DOWN_SIGNAL, true);
  }
  /**
   * Event signal emitted when a mouse button was released on this widget.
   *
   * <p>The event details contains information such as the {@link WMouseEvent#getButton()}, optional
   * {@link WMouseEvent#getModifiers() keyboard modifiers}, and mouse coordinates relative to the
   * {@link WMouseEvent#getWidget()}, the window {@link WMouseEvent#getWindow()}, or the {@link
   * WMouseEvent#getDocument()}.
   *
   * <p>If you connect also the {@link WInteractWidget#mouseWentDown() mouseWentDown()} signal, then
   * a subsequent {@link WInteractWidget#mouseWentUp() mouseWentUp()} will be received by the same
   * widget, even if mouse is no longer over the original widget.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> mouseWentUp() {
    return this.mouseEventSignal(MOUSE_UP_SIGNAL, true);
  }
  /**
   * Event signal emitted when the mouse went out of this widget.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> mouseWentOut() {
    return this.mouseEventSignal(MOUSE_OUT_SIGNAL, true);
  }
  /**
   * Event signal emitted when the mouse entered this widget.
   *
   * <p>The signal is emitted as soon as the mouse enters the widget, or after some delay as
   * configured by {@link WInteractWidget#setMouseOverDelay(int delay) setMouseOverDelay()}
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> mouseWentOver() {
    return this.mouseEventSignal(MOUSE_OVER_SIGNAL, true);
  }
  /**
   * Event signal emitted when the mouse moved over this widget.
   *
   * <p>The mouse event contains information on the button(s) currently pressed. If multiple buttons
   * are currently pressed, only the button with smallest enum value is returned.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> mouseMoved() {
    return this.mouseEventSignal(MOUSE_MOVE_SIGNAL, true);
  }
  /**
   * Event signal emitted when the mouse is dragged over this widget.
   *
   * <p>The mouse event contains information on the button(s) currently pressed. If multiple buttons
   * are currently pressed, only the button with smallest enum value is returned.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> mouseDragged() {
    return this.mouseEventSignal(MOUSE_DRAG_SIGNAL, true);
  }
  /**
   * Event signal emitted when the mouse scroll wheel was used.
   *
   * <p>The event details contains information such as the {@link WMouseEvent#getWheelDelta() wheel
   * delta}, optional {@link WMouseEvent#getModifiers() keyboard modifiers}, and mouse coordinates
   * relative to the {@link WMouseEvent#getWidget()}, the window {@link WMouseEvent#getWindow()}, or
   * the {@link WMouseEvent#getDocument()}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WMouseEvent> mouseWheel() {
    if (WApplication.getInstance().getEnvironment().agentIsIElt(9)
        || WApplication.getInstance().getEnvironment().getAgent() == UserAgent.Edge) {
      return this.mouseEventSignal(MOUSE_WHEEL_SIGNAL, true);
    } else {
      return this.mouseEventSignal(WHEEL_SIGNAL, true);
    }
  }
  /**
   * Event signal emitted when a finger is placed on the screen.
   *
   * <p>The event details contains information such as the {@link WTouchEvent#getTouches()}, {@link
   * WTouchEvent#getTargetTouches() target touches} and {@link WTouchEvent#getChangedTouches()
   * changed touches}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WTouchEvent> touchStarted() {
    return this.touchEventSignal(TOUCH_START_SIGNAL, true);
  }
  /**
   * Event signal emitted when a finger is removed from the screen.
   *
   * <p>The event details contains information such as the {@link WTouchEvent#getTouches()}, {@link
   * WTouchEvent#getTargetTouches() target touches} and {@link WTouchEvent#getChangedTouches()
   * changed touches}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WTouchEvent> touchEnded() {
    return this.touchEventSignal(TOUCH_END_SIGNAL, true);
  }
  /**
   * Event signal emitted when a finger, which is already placed on the screen, is moved across the
   * screen.
   *
   * <p>The event details contains information such as the {@link WTouchEvent#getTouches()}, {@link
   * WTouchEvent#getTargetTouches() target touches} and {@link WTouchEvent#getChangedTouches()
   * changed touches}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WTouchEvent> touchMoved() {
    return this.touchEventSignal(TOUCH_MOVE_SIGNAL, true);
  }
  /**
   * Event signal emitted when a gesture is started.
   *
   * <p>The event details contains information about the {@link WGestureEvent#getScale()} and the
   * {@link WGestureEvent#getRotation()}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WGestureEvent> gestureStarted() {
    return this.gestureEventSignal(GESTURE_START_SIGNAL, true);
  }
  /**
   * Event signal emitted when a gesture is changed.
   *
   * <p>The event details contains information about the {@link WGestureEvent#getScale()} and the
   * {@link WGestureEvent#getRotation()}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WGestureEvent> gestureChanged() {
    return this.gestureEventSignal(GESTURE_CHANGE_SIGNAL, true);
  }
  /**
   * Event signal emitted when a gesture is ended.
   *
   * <p>The event details contains information about the {@link WGestureEvent#getScale()} and the
   * {@link WGestureEvent#getRotation()}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal1<WGestureEvent> gestureEnded() {
    return this.gestureEventSignal(GESTURE_END_SIGNAL, true);
  }
  /**
   * Configure dragging for drag and drop.
   *
   * <p>Enable drag&amp;drop for this widget. The mimeType is used to find a suitable drop target,
   * which must accept dropping of this mimetype.
   *
   * <p>By default, the entire widget is dragged. One may specify another widget to be dragged (for
   * example the parent as <code>dragWidget</code>) or a <code>dragWidget</code> whose function is
   * only to represent the drag visually (when <code>isDragWidgetOnly</code> = <code>true</code>).
   *
   * <p>The widget to be identified as source in the dropEvent may be given explicitly, and will
   * default to this widget otherwise.
   *
   * <p>When using a touch interface, the widget can also be dragged after a long press.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, drag&amp;drop does not work. </i>
   *
   * @see WWidget#dropEvent(WDropEvent event)
   * @see WWidget#acceptDrops(String mimeType, String hoverStyleClass)
   * @see WDropEvent
   */
  public void setDraggable(
      final String mimeType, WWidget dragWidget, boolean isDragWidgetOnly, WObject sourceObject) {
    if (dragWidget == null) {
      dragWidget = this;
    }
    if (sourceObject == null) {
      sourceObject = this;
    }
    if (isDragWidgetOnly) {
      dragWidget.hide();
    }
    WApplication app = WApplication.getInstance();
    this.setAttributeValue("dmt", mimeType);
    this.setAttributeValue("dwid", dragWidget.getId());
    this.setAttributeValue("dsid", app.encodeObject(sourceObject));
    if (!(this.dragSlot_ != null)) {
      this.dragSlot_ = new JSlot();
      this.dragSlot_.setJavaScript(
          "function(o,e){" + app.getJavaScriptClass() + "._p_.dragStart(o,e);" + "}");
    }
    if (!(this.dragTouchSlot_ != null)) {
      this.dragTouchSlot_ = new JSlot();
      this.dragTouchSlot_.setJavaScript(
          "function(o,e){" + app.getJavaScriptClass() + "._p_.touchStart(o,e);" + "}");
    }
    if (!(this.dragTouchEndSlot_ != null)) {
      this.dragTouchEndSlot_ = new JSlot();
      this.dragTouchEndSlot_.setJavaScript(
          "function(){" + app.getJavaScriptClass() + "._p_.touchEnded();" + "}");
    }
    this.voidEventSignal(DRAGSTART_SIGNAL, true).preventDefaultAction(true);
    this.mouseWentDown().addListener(this.dragSlot_);
    this.touchStarted().addListener(this.dragTouchSlot_);
    this.touchStarted().preventDefaultAction(true);
    this.touchEnded().addListener(this.dragTouchEndSlot_);
  }
  /**
   * Configure dragging for drag and drop.
   *
   * <p>Calls {@link #setDraggable(String mimeType, WWidget dragWidget, boolean isDragWidgetOnly,
   * WObject sourceObject) setDraggable(mimeType, (WWidget)null, false, (WObject)null)}
   */
  public final void setDraggable(final String mimeType) {
    setDraggable(mimeType, (WWidget) null, false, (WObject) null);
  }
  /**
   * Configure dragging for drag and drop.
   *
   * <p>Calls {@link #setDraggable(String mimeType, WWidget dragWidget, boolean isDragWidgetOnly,
   * WObject sourceObject) setDraggable(mimeType, dragWidget, false, (WObject)null)}
   */
  public final void setDraggable(final String mimeType, WWidget dragWidget) {
    setDraggable(mimeType, dragWidget, false, (WObject) null);
  }
  /**
   * Configure dragging for drag and drop.
   *
   * <p>Calls {@link #setDraggable(String mimeType, WWidget dragWidget, boolean isDragWidgetOnly,
   * WObject sourceObject) setDraggable(mimeType, dragWidget, isDragWidgetOnly, (WObject)null)}
   */
  public final void setDraggable(
      final String mimeType, WWidget dragWidget, boolean isDragWidgetOnly) {
    setDraggable(mimeType, dragWidget, isDragWidgetOnly, (WObject) null);
  }
  /**
   * Disable drag &amp; drop for this widget.
   *
   * <p>
   *
   * @see WInteractWidget#setDraggable(String mimeType, WWidget dragWidget, boolean
   *     isDragWidgetOnly, WObject sourceObject)
   */
  public void unsetDraggable() {
    if (this.dragSlot_ != null) {
      this.mouseWentDown().removeListener(this.dragSlot_);
      this.dragSlot_ = null;
    }
    if (this.dragTouchSlot_ != null) {
      this.touchStarted().removeListener(this.dragTouchSlot_);
      this.dragTouchSlot_ = null;
    }
    if (this.dragTouchEndSlot_ != null) {
      this.touchEnded().removeListener(this.dragTouchEndSlot_);
      this.dragTouchEndSlot_ = null;
    }
    EventSignal dragStart = this.voidEventSignal(DRAGSTART_SIGNAL, false);
    if (dragStart != null) {
      dragStart.preventDefaultAction(false);
    }
  }
  /**
   * Sets a delay for the mouse over event.
   *
   * <p>This sets a delay (in milliseconds) before the mouse over event is emitted.
   *
   * <p>The default value is 0.
   *
   * <p>
   *
   * @see WInteractWidget#mouseWentOver()
   */
  public void setMouseOverDelay(int delay) {
    this.mouseOverDelay_ = delay;
    EventSignal1<WMouseEvent> mouseOver = this.mouseEventSignal(MOUSE_OVER_SIGNAL, false);
    if (mouseOver != null) {
      mouseOver.ownerRepaint();
    }
  }
  /**
   * Returns the mouse over signal delay.
   *
   * <p>
   *
   * @see WInteractWidget#setMouseOverDelay(int delay)
   */
  public int getMouseOverDelay() {
    return this.mouseOverDelay_;
  }

  public void setPopup(boolean popup) {
    if (popup && WApplication.getInstance().getEnvironment().hasAjax()) {
      this.clicked()
          .addListener(
              "function(o,e) {  if (Wt4.11.2.WPopupWidget && o.wtPopup) {Wt4.11.2.WPopupWidget.popupClicked = o;document.dispatchEvent(new MouseEvent('click', e));Wt4.11.2.WPopupWidget.popupClicked = null; }}");
      this.clicked().preventPropagation();
    }
    super.setPopup(popup);
  }

  public void load() {
    if (!this.isDisabled()) {
      if (this.getParent() != null) {
        this.flags_.set(BIT_ENABLED, this.getParent().isEnabled());
      } else {
        this.flags_.set(BIT_ENABLED, true);
      }
    } else {
      this.flags_.set(BIT_ENABLED, false);
    }
    super.load();
  }

  public boolean isEnabled() {
    return !this.isDisabled() && this.flags_.get(BIT_ENABLED);
  }

  void updateDom(final DomElement element, boolean all) {
    boolean updateKeyDown = false;
    WApplication app = WApplication.getInstance();
    EventSignal enterPress = this.voidEventSignal(ENTER_PRESS_SIGNAL, false);
    EventSignal escapePress = this.voidEventSignal(ESCAPE_PRESS_SIGNAL, false);
    EventSignal1<WKeyEvent> keyDown = this.keyEventSignal(KEYDOWN_SIGNAL, false);
    updateKeyDown =
        enterPress != null && enterPress.needsUpdate(all)
            || escapePress != null && escapePress.needsUpdate(all)
            || keyDown != null && keyDown.needsUpdate(all);
    if (updateKeyDown) {
      List<DomElement.EventAction> actions = new ArrayList<DomElement.EventAction>();
      if (enterPress != null) {
        if (enterPress.needsUpdate(true)) {
          String extraJS = "";
          final WEnvironment env = app.getEnvironment();
          if (ObjectUtils.cast(this, WFormWidget.class) != null
              && !env.agentIsOpera()
              && !env.agentIsIE()) {
            extraJS = "var g=this.onchange;this.onchange=function(){this.onchange=g;};";
          }
          actions.add(
              new DomElement.EventAction(
                  "e.keyCode && (e.keyCode == 13)",
                  enterPress.getJavaScript() + extraJS,
                  enterPress.encodeCmd(),
                  enterPress.isExposedSignal()));
        }
        enterPress.updateOk();
      }
      if (escapePress != null) {
        if (escapePress.needsUpdate(true)) {
          actions.add(
              new DomElement.EventAction(
                  "e.keyCode && (e.keyCode == 27)",
                  escapePress.getJavaScript(),
                  escapePress.encodeCmd(),
                  escapePress.isExposedSignal()));
        }
        escapePress.updateOk();
      }
      if (keyDown != null) {
        if (keyDown.needsUpdate(true)) {
          actions.add(
              new DomElement.EventAction(
                  "", keyDown.getJavaScript(), keyDown.encodeCmd(), keyDown.isExposedSignal()));
        }
        keyDown.updateOk();
      }
      if (!actions.isEmpty()) {
        element.setEvent("keydown", actions);
      } else {
        if (!all) {
          element.setEvent("keydown", "", "");
        }
      }
    }
    EventSignal1<WMouseEvent> mouseDown = this.mouseEventSignal(MOUSE_DOWN_SIGNAL, false);
    EventSignal1<WMouseEvent> mouseUp = this.mouseEventSignal(MOUSE_UP_SIGNAL, false);
    EventSignal1<WMouseEvent> mouseMove = this.mouseEventSignal(MOUSE_MOVE_SIGNAL, false);
    EventSignal1<WMouseEvent> mouseDrag = this.mouseEventSignal(MOUSE_DRAG_SIGNAL, false);
    boolean updateMouseMove =
        mouseMove != null && mouseMove.needsUpdate(all)
            || mouseDrag != null && mouseDrag.needsUpdate(all);
    boolean updateMouseDown = mouseDown != null && mouseDown.needsUpdate(all) || updateMouseMove;
    boolean updateMouseUp = mouseUp != null && mouseUp.needsUpdate(all) || updateMouseMove;
    String CheckDisabled =
        "if(o.classList.contains('"
            + app.getTheme().getDisabledClass()
            + "')){Wt4.11.2.cancelEvent(e);return;}";
    if (updateMouseDown) {
      StringBuilder js = new StringBuilder();
      js.append(CheckDisabled);
      if (mouseUp != null && mouseUp.isConnected()) {
        js.append(app.getJavaScriptClass()).append("._p_.saveDownPos(event);");
      }
      if (mouseDrag != null && mouseDrag.isConnected()
          || mouseDown != null
              && mouseDown.isConnected()
              && (mouseUp != null && mouseUp.isConnected()
                  || mouseMove != null && mouseMove.isConnected())) {
        js.append("Wt4.11.2.capture(this);");
      }
      if (mouseMove != null && mouseMove.isConnected()
          || mouseDrag != null && mouseDrag.isConnected()) {
        js.append("Wt4.11.2.mouseDown(e);");
      }
      if (mouseDown != null) {
        js.append(mouseDown.getJavaScript());
        element.setEvent(
            "mousedown", js.toString(), mouseDown.encodeCmd(), mouseDown.isExposedSignal());
        mouseDown.updateOk();
      } else {
        element.setEvent("mousedown", js.toString(), "", false);
      }
    }
    if (updateMouseUp) {
      StringBuilder js = new StringBuilder();
      js.append(CheckDisabled);
      if (mouseMove != null && mouseMove.isConnected()
          || mouseDrag != null && mouseDrag.isConnected()) {
        js.append("Wt4.11.2.mouseUp(e);");
      }
      if (mouseUp != null) {
        js.append(mouseUp.getJavaScript());
        element.setEvent("mouseup", js.toString(), mouseUp.encodeCmd(), mouseUp.isExposedSignal());
        mouseUp.updateOk();
      } else {
        element.setEvent("mouseup", js.toString(), "", false);
      }
    }
    if (updateMouseMove) {
      List<DomElement.EventAction> actions = new ArrayList<DomElement.EventAction>();
      if (mouseMove != null) {
        actions.add(
            new DomElement.EventAction(
                "", mouseMove.getJavaScript(), mouseMove.encodeCmd(), mouseMove.isExposedSignal()));
        mouseMove.updateOk();
      }
      if (mouseDrag != null) {
        actions.add(
            new DomElement.EventAction(
                "Wt4.11.2.buttons",
                mouseDrag.getJavaScript() + "Wt4.11.2.drag(e);",
                mouseDrag.encodeCmd(),
                mouseDrag.isExposedSignal()));
        mouseDrag.updateOk();
      }
      element.setEvent("mousemove", actions);
    }
    EventSignal1<WTouchEvent> touchStart = this.touchEventSignal(TOUCH_START_SIGNAL, false);
    EventSignal1<WTouchEvent> touchEnd = this.touchEventSignal(TOUCH_END_SIGNAL, false);
    EventSignal1<WTouchEvent> touchMove = this.touchEventSignal(TOUCH_MOVE_SIGNAL, false);
    boolean updateTouchMove = touchMove != null && touchMove.needsUpdate(all);
    boolean updateTouchStart = touchStart != null && touchStart.needsUpdate(all) || updateTouchMove;
    boolean updateTouchEnd = touchEnd != null && touchEnd.needsUpdate(all) || updateTouchMove;
    if (updateTouchStart) {
      StringBuilder js = new StringBuilder();
      js.append(CheckDisabled);
      if (touchEnd != null && touchEnd.isConnected()) {
        js.append(app.getJavaScriptClass()).append("._p_.saveDownPos(event);");
      }
      if (touchStart != null
          && touchStart.isConnected()
          && (touchEnd != null && touchEnd.isConnected()
              || touchMove != null && touchMove.isConnected())) {
        js.append("Wt4.11.2.capture(this);");
      }
      if (touchStart != null) {
        js.append(touchStart.getJavaScript());
        element.setEvent(
            "touchstart", js.toString(), touchStart.encodeCmd(), touchStart.isExposedSignal());
        touchStart.updateOk();
      } else {
        element.setEvent("touchstart", js.toString(), "", false);
      }
    }
    if (updateTouchEnd) {
      StringBuilder js = new StringBuilder();
      js.append(CheckDisabled);
      if (touchEnd != null) {
        js.append(touchEnd.getJavaScript());
        element.setEvent(
            "touchend", js.toString(), touchEnd.encodeCmd(), touchEnd.isExposedSignal());
        touchEnd.updateOk();
      } else {
        element.setEvent("touchend", js.toString(), "", false);
      }
    }
    if (updateTouchMove) {
      if (touchMove != null) {
        element.setEvent(
            "touchmove",
            touchMove.getJavaScript(),
            touchMove.encodeCmd(),
            touchMove.isExposedSignal());
        touchMove.updateOk();
      }
    }
    EventSignal1<WMouseEvent> mouseClick = this.mouseEventSignal(M_CLICK_SIGNAL, false);
    EventSignal1<WMouseEvent> mouseDblClick = this.mouseEventSignal(DBL_CLICK_SIGNAL, false);
    boolean updateMouseClick =
        mouseClick != null && mouseClick.needsUpdate(all)
            || mouseDblClick != null && mouseDblClick.needsUpdate(all);
    if (updateMouseClick) {
      StringBuilder js = new StringBuilder();
      js.append(CheckDisabled);
      if (mouseDrag != null) {
        js.append("if (Wt4.11.2.dragged()) return;");
      }
      if (mouseDblClick != null && mouseDblClick.needsUpdate(all)) {
        if (mouseClick != null) {
          if (mouseClick.isDefaultActionPrevented() || mouseClick.isPropagationPrevented()) {
            js.append("Wt4.11.2.cancelEvent(e");
            if (mouseClick.isDefaultActionPrevented() && mouseClick.isPropagationPrevented()) {
              js.append(");");
            } else {
              if (mouseClick.isDefaultActionPrevented()) {
                js.append(",0x2);");
              } else {
                js.append(",0x1);");
              }
            }
          }
        }
        js.append("if(Wt4.11.2.isDblClick(o, e)) {").append(mouseDblClick.getJavaScript());
        if (mouseDblClick.isExposedSignal()) {
          js.append(app.getJavaScriptClass())
              .append("._p_.update(o,'")
              .append(mouseDblClick.encodeCmd())
              .append("',e,true);");
        }
        mouseDblClick.updateOk();
        js.append(
            "}else{if (Wt4.11.2.isIElt9 && document.createEventObject) e = document.createEventObject(e);o.wtE1 = e;o.wtClickTimeout = setTimeout(function() {o.wtClickTimeout = null; o.wtE1 = null;");
        if (mouseClick != null) {
          js.append(mouseClick.getJavaScript());
          if (mouseClick.isExposedSignal()) {
            js.append(app.getJavaScriptClass())
                .append("._p_.update(o,'")
                .append(mouseClick.encodeCmd())
                .append("',e,true);");
          }
          mouseClick.updateOk();
        }
        final Configuration conf = app.getEnvironment().getServer().getConfiguration();
        js.append("},").append(conf.getDoubleClickTimeout()).append(");}");
      } else {
        if (mouseClick != null && mouseClick.needsUpdate(all)) {
          js.append(mouseClick.getJavaScript());
          if (mouseClick.isExposedSignal()) {
            js.append(app.getJavaScriptClass())
                .append("._p_.update(o,'")
                .append(mouseClick.encodeCmd())
                .append("',e,true);");
          }
          mouseClick.updateOk();
        }
      }
      element.setEvent(
          CLICK_SIGNAL, js.toString(), mouseClick != null ? mouseClick.encodeCmd() : "");
      if (mouseDblClick != null) {
        if (app.getEnvironment().agentIsIElt(9)) {
          element.setEvent("dblclick", "this.onclick()");
        }
      }
    }
    EventSignal1<WMouseEvent> mouseOver = this.mouseEventSignal(MOUSE_OVER_SIGNAL, false);
    EventSignal1<WMouseEvent> mouseOut = this.mouseEventSignal(MOUSE_OUT_SIGNAL, false);
    boolean updateMouseOver = mouseOver != null && mouseOver.needsUpdate(all);
    if (this.mouseOverDelay_ != 0) {
      if (updateMouseOver) {
        StringBuilder js = new StringBuilder();
        js.append("o.over=setTimeout(function() {")
            .append("o.over = null;")
            .append(mouseOver.getJavaScript());
        if (mouseOver.isExposedSignal()) {
          js.append(app.getJavaScriptClass())
              .append("._p_.update(o,'")
              .append(mouseOver.encodeCmd())
              .append("',e,true);");
        }
        js.append("},").append(this.mouseOverDelay_).append(");");
        element.setEvent("mouseover", js.toString(), "");
        mouseOver.updateOk();
        if (!(mouseOut != null)) {
          mouseOut = this.mouseEventSignal(MOUSE_OUT_SIGNAL, true);
        }
        element.setEvent(
            "mouseout",
            "clearTimeout(o.over); o.over=null;" + mouseOut.getJavaScript(),
            mouseOut.encodeCmd(),
            mouseOut.isExposedSignal());
        mouseOut.updateOk();
      }
    } else {
      if (updateMouseOver) {
        element.setEventSignal("mouseover", mouseOver);
        mouseOver.updateOk();
      }
      boolean updateMouseOut = mouseOut != null && mouseOut.needsUpdate(all);
      if (updateMouseOut) {
        element.setEventSignal("mouseout", mouseOut);
        mouseOut.updateOk();
      }
    }
    EventSignal dragStart = this.voidEventSignal(DRAGSTART_SIGNAL, false);
    if (dragStart != null && dragStart.needsUpdate(all)) {
      element.setEventSignal("dragstart", dragStart);
      dragStart.updateOk();
    }
    this.updateEventSignals(element, all);
    super.updateDom(element, all);
  }

  void propagateRenderOk(boolean deep) {
    final LinkedList<AbstractEventSignal> other = this.eventSignals();
    for (Iterator<AbstractEventSignal> i_it = other.iterator(); i_it.hasNext(); ) {
      AbstractEventSignal i = i_it.next();
      final AbstractEventSignal s = i;
      s.updateOk();
    }
    super.propagateRenderOk(deep);
  }

  protected void propagateSetEnabled(boolean enabled) {
    this.flags_.set(BIT_ENABLED, enabled);
    WApplication app = WApplication.getInstance();
    String disabledClass = app.getTheme().getDisabledClass();
    this.toggleStyleClass(disabledClass, !enabled, true);
    super.propagateSetEnabled(enabled);
  }

  void updateEventSignals(final DomElement element, boolean all) {
    final LinkedList<AbstractEventSignal> other = this.eventSignals();
    for (Iterator<AbstractEventSignal> i_it = other.iterator(); i_it.hasNext(); ) {
      AbstractEventSignal i = i_it.next();
      final AbstractEventSignal s = i;
      if (s.getName() == WInteractWidget.M_CLICK_SIGNAL && this.flags_.get(BIT_REPAINT_TO_AJAX)) {
        element.unwrap();
      }
      this.updateSignalConnection(element, s, s.getName(), all);
    }
  }

  JSlot dragSlot_;
  protected JSlot dragTouchSlot_;
  protected JSlot dragTouchEndSlot_;
  static String M_CLICK_SIGNAL = "M_click";
  static String CLICK_SIGNAL = "click";
  private static String KEYDOWN_SIGNAL = "M_keydown";
  static String KEYPRESS_SIGNAL = "keypress";
  private static String KEYUP_SIGNAL = "keyup";
  private static String ENTER_PRESS_SIGNAL = "M_enterpress";
  private static String ESCAPE_PRESS_SIGNAL = "M_escapepress";
  private static String DBL_CLICK_SIGNAL = "M_dblclick";
  static String MOUSE_DOWN_SIGNAL = "M_mousedown";
  static String MOUSE_UP_SIGNAL = "M_mouseup";
  private static String MOUSE_OUT_SIGNAL = "M_mouseout";
  private static String MOUSE_OVER_SIGNAL = "M_mouseover";
  private static String MOUSE_MOVE_SIGNAL = "M_mousemove";
  private static String MOUSE_DRAG_SIGNAL = "M_mousedrag";
  static String MOUSE_WHEEL_SIGNAL = "mousewheel";
  static String WHEEL_SIGNAL = "wheel";
  private static String TOUCH_START_SIGNAL = "touchstart";
  private static String TOUCH_MOVE_SIGNAL = "touchmove";
  private static String TOUCH_END_SIGNAL = "touchend";
  private static String GESTURE_START_SIGNAL = "gesturestart";
  private static String GESTURE_CHANGE_SIGNAL = "gesturechange";
  private static String GESTURE_END_SIGNAL = "gestureend";
  private static String DRAGSTART_SIGNAL = "dragstart";
  private int mouseOverDelay_;
}
