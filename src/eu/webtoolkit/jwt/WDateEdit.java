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
 * A date edit.
 *
 * <p>A date picker is a line edit with support for date entry (using an icon and a calendar).
 *
 * <p>A {@link WDateValidator} is used to validate date entry.
 *
 * <p>In many cases, it provides a more convenient implementation of a date picker compared to
 * {@link WDatePicker} since it is implemented as a line edit. This also makes the implementation
 * ready for a native HTML5 control.
 */
public class WDateEdit extends WLineEdit {
  private static Logger logger = LoggerFactory.getLogger(WDateEdit.class);

  /** Creates a new date edit. */
  public WDateEdit(WContainerWidget parentContainer) {
    super();
    this.popup_ = null;
    this.uCalendar_ = null;
    this.customFormat_ = false;
    this.changed()
        .addListener(
            this,
            () -> {
              WDateEdit.this.setFromLineEdit();
            });
    this.uCalendar_ = new WCalendar();
    this.calendar_ = this.uCalendar_;
    this.calendar_.setSingleClickSelect(true);
    this.calendar_
        .activated()
        .addListener(
            this,
            (WDate e1) -> {
              WDateEdit.this.setFocusTrue();
            });
    this.calendar_
        .selectionChanged()
        .addListener(
            this,
            () -> {
              WDateEdit.this.setFromCalendar();
            });
    this.setValidator(
        new WDateValidator(LocaleUtils.getDateFormat(WApplication.getInstance().getLocale())));
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new date edit.
   *
   * <p>Calls {@link #WDateEdit(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WDateEdit() {
    this((WContainerWidget) null);
  }

  public void remove() {
    super.remove();
  }
  /**
   * Sets the date.
   *
   * <p>Does nothing if the current date is <code>Null</code>.
   *
   * <p>
   *
   * @see WDateEdit#getDate()
   */
  public void setDate(final WDate date) {
    if (!(date == null)) {
      this.setText(date.toString(this.getFormat()));
      this.calendar_.select(date);
      this.calendar_.browseTo(date);
    }
  }
  /**
   * Returns the date.
   *
   * <p>Reads the current date.
   *
   * <p>Returns <code>null</code> if the date could not be parsed using the current {@link
   * WDateEdit#getFormat() getFormat()}. <br>
   *
   * <p>
   *
   * @see WDateEdit#setDate(WDate date)
   * @see WLineEdit#getText()
   */
  public WDate getDate() {
    return WDate.fromString(this.getText(), this.getFormat());
  }
  /**
   * Returns the validator.
   *
   * <p>Most of the configuration of the date edit is stored in the validator.
   */
  public WDateValidator getDateValidator() {
    return ObjectUtils.cast(super.getValidator(), WDateValidator.class);
  }
  /**
   * Sets the format used for representing the date.
   *
   * <p>This sets the format in the validator.
   *
   * <p>The default format is based on the current WLocale.
   *
   * <p>
   *
   * @see WDateValidator#setFormat(String format)
   */
  public void setFormat(final String format) {
    WDateValidator dv = this.getDateValidator();
    if (dv != null) {
      WDate d = this.getDate();
      dv.setFormat(format);
      this.setDate(d);
      this.customFormat_ = true;
    } else {
      logger.warn(
          new StringWriter()
              .append("setFormat() ignored since validator is not a WDateValidator")
              .toString());
    }
  }
  /**
   * Returns the format.
   *
   * <p>
   *
   * @see WDateEdit#setFormat(String format)
   */
  public String getFormat() {
    WDateValidator dv = this.getDateValidator();
    if (dv != null) {
      return dv.getFormat();
    } else {
      logger.warn(
          new StringWriter()
              .append("format() is bogus  since validator is not a WDateValidator")
              .toString());
      return "";
    }
  }
  /**
   * Sets the lower limit of the valid date range.
   *
   * <p>This sets the lower limit of the valid date range in the validator.
   *
   * <p>
   *
   * @see WDateValidator#setBottom(WDate bottom)
   */
  public void setBottom(final WDate bottom) {
    WDateValidator dv = this.getDateValidator();
    if (dv != null) {
      dv.setBottom(bottom);
    } else {
      this.calendar_.setBottom(bottom);
    }
  }
  /**
   * Returns the lower limit of the valid date range.
   *
   * <p>
   *
   * @see WDateEdit#setBottom(WDate bottom)
   */
  public WDate getBottom() {
    return this.calendar_.getBottom();
  }
  /**
   * Sets the upper limit of the valid date range.
   *
   * <p>This sets the upper limit of the valid date range in the validator.
   *
   * <p>
   *
   * @see WDateValidator#setTop(WDate top)
   */
  public void setTop(final WDate top) {
    WDateValidator dv = this.getDateValidator();
    if (dv != null) {
      dv.setTop(top);
    } else {
      this.calendar_.setTop(top);
    }
  }
  /**
   * Returns the upper limit of the valid range.
   *
   * <p>
   *
   * @see WDateEdit#setTop(WDate top)
   */
  public WDate getTop() {
    return this.calendar_.getTop();
  }
  /**
   * Returns the calendar widget.
   *
   * <p>The calendar may be 0 (e.g. when using a native date entry widget).
   */
  public WCalendar getCalendar() {
    return this.calendar_;
  }
  /** Hide/unhide the widget. */
  public void setHidden(boolean hidden, final WAnimation animation) {
    super.setHidden(hidden, animation);
    if (this.popup_ != null && hidden) {
      this.popup_.setHidden(hidden, animation);
    }
  }

  public void load() {
    boolean wasLoaded = this.isLoaded();
    super.load();
    if (wasLoaded) {
      return;
    }
    String TEMPLATE = "${calendar}";
    WTemplate t = new WTemplate(new WString(TEMPLATE), (WContainerWidget) null);
    WTemplate temp = t;
    this.popup_ = new WPopupWidget(t);
    if (this.isHidden()) {
      this.popup_.setHidden(true);
    }
    this.popup_.setAnchorWidget(this);
    this.popup_.setTransient(true);
    this.calendar_
        .activated()
        .addListener(
            this.popup_,
            (WDate e1) -> {
              WDateEdit.this.popup_.hide();
            });
    temp.bindWidget("calendar", this.uCalendar_);
    WApplication.getInstance().getTheme().apply(this, this.popup_, WidgetThemeRole.DatePickerPopup);
    this.escapePressed()
        .addListener(
            this.popup_,
            () -> {
              WDateEdit.this.popup_.hide();
            });
    this.escapePressed()
        .addListener(
            this,
            () -> {
              WDateEdit.this.setFocusTrue();
            });
  }

  public void refresh() {
    super.refresh();
    WDateValidator dv = this.getDateValidator();
    if (!this.customFormat_ && dv != null) {
      WDate d = this.getDate();
      dv.setFormat(LocaleUtils.getDateFormat(WApplication.getInstance().getLocale()));
      this.setDate(d);
    } else {
      logger.warn(
          new StringWriter()
              .append("setFormat() ignored since validator is not a WDateValidator")
              .toString());
    }
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (flags.contains(RenderFlag.Full)) {
      this.defineJavaScript();
      WDateValidator dv = this.getDateValidator();
      if (dv != null) {
        this.setTop(dv.getTop());
        this.setBottom(dv.getBottom());
      }
    }
    super.render(flags);
  }

  protected void propagateSetEnabled(boolean enabled) {
    super.propagateSetEnabled(enabled);
  }

  protected void validatorChanged() {
    WDateValidator dv = this.getDateValidator();
    if (dv != null) {
      this.calendar_.setBottom(dv.getBottom());
      this.calendar_.setTop(dv.getTop());
    }
    super.validatorChanged();
  }
  /** Sets the value from the calendar to the line edit. */
  protected void setFromCalendar() {
    if (!this.calendar_.getSelection().isEmpty()) {
      WDate calDate = this.calendar_.getSelection().iterator().next();
      this.setText(calDate.toString(this.getFormat()));
      this.textInput().trigger();
      this.changed().trigger();
    }
  }
  /** Sets the value from the line edit to the calendar. */
  protected void setFromLineEdit() {
    WDate d = WDate.fromString(this.getText(), this.getFormat());
    if ((d != null)) {
      if (this.calendar_.getSelection().isEmpty()) {
        this.calendar_.select(d);
        this.calendar_.selectionChanged().trigger();
      } else {
        WDate j = this.calendar_.getSelection().iterator().next();
        if (!(j == d || (j != null && j.equals(d)))) {
          this.calendar_.select(d);
          this.calendar_.selectionChanged().trigger();
        }
      }
      this.calendar_.browseTo(d);
    }
  }

  private WPopupWidget popup_;
  private WCalendar uCalendar_;
  private WCalendar calendar_;
  private boolean customFormat_;

  private void defineJavaScript() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WDateEdit.js", wtjs1());
    String jsObj =
        "new Wt4.11.2.WDateEdit("
            + app.getJavaScriptClass()
            + ","
            + this.getJsRef()
            + ","
            + jsStringLiteral(this.popup_.getId())
            + ");";
    this.setJavaScriptMember(" WDateEdit", jsObj);
    final AbstractEventSignal b = this.mouseMoved();
    final AbstractEventSignal c = this.keyWentDown();
    this.connectJavaScript(this.mouseMoved(), "mouseMove");
    this.connectJavaScript(this.mouseWentUp(), "mouseUp");
    this.connectJavaScript(this.mouseWentDown(), "mouseDown");
    this.connectJavaScript(this.mouseWentOut(), "mouseOut");
  }

  private void connectJavaScript(final AbstractEventSignal s, final String methodName) {
    String jsFunction =
        "function(dobj, event) {var o = "
            + this.getJsRef()
            + ";if (o && o.wtDObj) o.wtDObj."
            + methodName
            + "(dobj, event);}";
    s.addListener(jsFunction);
  }

  private void setFocusTrue() {
    this.setFocus(true);
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WDateEdit",
        "(function(t,s,i){const o=\"hover\",e=\"active\",n=\"unselectable\";s.wtDObj=this;const c=t.WT;function u(){return s.readOnly}function a(){s.classList.remove(e)}function r(){const t=c.$(i).wtPopup;t.bindHide(a);t.show(s,c.Vertical)}this.mouseOut=function(t,i){s.classList.remove(o)};this.mouseMove=function(t,i){if(u())return;const e=c.widgetCoordinates(s,i).x>s.offsetWidth-40;s.classList.toggle(o,e)};this.mouseDown=function(t,i){if(u())return;if(c.widgetCoordinates(s,i).x>s.offsetWidth-40){s.classList.add(n);s.classList.add(e)}};this.mouseUp=function(t,i){s.classList.remove(n);c.widgetCoordinates(s,i).x>s.offsetWidth-40&&r()}})");
  }
}
