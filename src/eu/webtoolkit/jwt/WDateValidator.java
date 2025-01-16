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
 * A validator for date input.
 *
 * <p>This validator accepts input in the given date format, and optionally checks if the date is
 * within a given range.
 *
 * <p>The format string used are the ones accepted by java.text.SimpleDateFormat
 *
 * <p>
 *
 * <h3>i18n</h3>
 *
 * <p>The strings used in the {@link WDateValidator} can be translated by overriding the default
 * values for the following localization keys:
 *
 * <ul>
 *   <li>Wt.WDateValidator.DateTooEarly: The date must be after {1}
 *   <li>Wt.WDateValidator.DateTooLate: The date must be before {1}
 *   <li>Wt.WDateValidator.WrongDateRange: The date must be between {1} and {2}
 *   <li>Wt.WDateValidator.WrongFormat: Must be a date in the format &apos;{1}&apos;
 * </ul>
 */
public class WDateValidator extends WValidator {
  private static Logger logger = LoggerFactory.getLogger(WDateValidator.class);

  /**
   * Creates a date validator.
   *
   * <p>The validator will accept dates using the current locale&apos;s format.
   *
   * <p>
   */
  public WDateValidator() {
    super();
    this.formats_ = new ArrayList<String>();
    this.bottom_ = null;
    this.top_ = null;
    this.tooEarlyText_ = new WString();
    this.tooLateText_ = new WString();
    this.notADateText_ = new WString();
    this.setFormat(LocaleUtils.getDateFormat(LocaleUtils.getCurrentLocale()));
  }
  /**
   * Creates a date validator.
   *
   * <p>The validator will accept dates in the indicated range using the current locale&apos;s
   * format.
   *
   * <p>
   */
  public WDateValidator(final WDate bottom, final WDate top) {
    super();
    this.formats_ = new ArrayList<String>();
    this.bottom_ = bottom;
    this.top_ = top;
    this.tooEarlyText_ = new WString();
    this.tooLateText_ = new WString();
    this.notADateText_ = new WString();
    this.setFormat(LocaleUtils.getDateFormat(LocaleUtils.getCurrentLocale()));
  }
  /**
   * Creates a date validator.
   *
   * <p>The validator will accept dates in the date format <code>format</code>.
   */
  public WDateValidator(final String format) {
    super();
    this.formats_ = new ArrayList<String>();
    this.bottom_ = null;
    this.top_ = null;
    this.tooEarlyText_ = new WString();
    this.tooLateText_ = new WString();
    this.notADateText_ = new WString();
    this.setFormat(format);
  }
  /**
   * Creates a date validator.
   *
   * <p>The validator will accept only dates within the indicated range <i>bottom</i> to <i>top</i>,
   * in the date format <code>format</code>.
   */
  public WDateValidator(final String format, final WDate bottom, final WDate top) {
    super();
    this.formats_ = new ArrayList<String>();
    this.bottom_ = bottom;
    this.top_ = top;
    this.tooEarlyText_ = new WString();
    this.tooLateText_ = new WString();
    this.notADateText_ = new WString();
    this.setFormat(format);
  }
  /**
   * Sets the bottom of the valid date range.
   *
   * <p>The default is a null date constructed using WDate().
   */
  public void setBottom(final WDate bottom) {
    if (!(this.bottom_ == bottom || (this.bottom_ != null && this.bottom_.equals(bottom)))) {
      this.bottom_ = bottom;
      this.repaint();
    }
  }
  /** Returns the bottom date of the valid range. */
  public WDate getBottom() {
    return this.bottom_;
  }
  /**
   * Sets the top of the valid date range.
   *
   * <p>The default is a null date constructed using WDate().
   */
  public void setTop(final WDate top) {
    if (!(this.top_ == top || (this.top_ != null && this.top_.equals(top)))) {
      this.top_ = top;
      this.repaint();
    }
  }
  /** Returns the top date of the valid range. */
  public WDate getTop() {
    return this.top_;
  }
  /** Sets the date format used to parse date strings. */
  public void setFormat(final String format) {
    if (this.formats_.isEmpty() || !this.formats_.get(0).equals(format)) {
      this.formats_.clear();
      this.formats_.add(format);
      this.repaint();
    }
  }
  /**
   * Returns the format string used to parse date strings.
   *
   * <p>
   *
   * @see WDateValidator#setFormat(String format)
   */
  public String getFormat() {
    return this.formats_.get(0);
  }
  /** Sets the date formats used to parse date strings. */
  public void setFormats(final List<String> formats) {
    Utils.copyList(formats, this.formats_);
    this.repaint();
  }
  /** Returns the date formats used to parse date strings. */
  public List<String> getFormats() {
    return this.formats_;
  }
  /**
   * Validates the given input.
   *
   * <p>The input is considered valid only when it is blank for a non-mandatory field, or represents
   * a date in the given format, and within the valid range.
   */
  public WValidator.Result validate(final String input) {
    if (input.length() == 0) {
      return super.validate(input);
    }
    for (int i = 0; i < this.formats_.size(); ++i) {
      try {
        WDate d = WDate.fromString(input, this.formats_.get(i));
        if ((d != null)) {
          if (!(this.bottom_ == null)) {
            if (d.before(this.bottom_)) {
              return new WValidator.Result(ValidationState.Invalid, this.getInvalidTooEarlyText());
            }
          }
          if (!(this.top_ == null)) {
            if (d.after(this.top_)) {
              return new WValidator.Result(ValidationState.Invalid, this.getInvalidTooLateText());
            }
          }
          return new WValidator.Result(ValidationState.Valid);
        }
      } catch (final RuntimeException e) {
        logger.warn(new StringWriter().append("validate(): ").append(e.toString()).toString());
      }
    }
    return new WValidator.Result(ValidationState.Invalid, this.getInvalidNotADateText());
  }
  /**
   * Sets the message to display when the input is not a date.
   *
   * <p>The default message is &quot;The date must be of the format {1}&quot;, with as first
   * argument the format string.
   */
  public void setInvalidNotADateText(final CharSequence text) {
    this.notADateText_ = WString.toWString(text);
  }
  /**
   * Returns the message displayed when the input is not a date.
   *
   * <p>
   *
   * @see WDateValidator#setInvalidNotADateText(CharSequence text)
   */
  public WString getInvalidNotADateText() {
    if (!(this.notADateText_.length() == 0)) {
      return this.notADateText_.clone().arg(this.formats_.get(0));
    } else {
      return WString.tr("Wt.WDateValidator.WrongFormat").arg(this.formats_.get(0));
    }
  }
  /**
   * Sets the message to display when the date is earlier than bottom.
   *
   * <p>The default message is &quot;The date must be between {1} and {2}&quot; or &quot;The date
   * must be after {1}&quot;.
   */
  public void setInvalidTooEarlyText(final CharSequence text) {
    this.tooEarlyText_ = WString.toWString(text);
    this.repaint();
  }
  /**
   * Returns the message displayed when date is too early.
   *
   * <p>
   *
   * @see WDateValidator#setInvalidTooEarlyText(CharSequence text)
   */
  public WString getInvalidTooEarlyText() {
    if (!(this.tooEarlyText_.length() == 0)) {
      return this.tooEarlyText_
          .clone()
          .arg(this.bottom_.toString(this.formats_.get(0)))
          .arg(this.top_.toString(this.formats_.get(0)));
    } else {
      if ((this.bottom_ == null)) {
        return new WString();
      } else {
        if ((this.top_ == null)) {
          return WString.tr("Wt.WDateValidator.DateTooEarly")
              .arg(this.bottom_.toString(this.formats_.get(0)));
        } else {
          return WString.tr("Wt.WDateValidator.WrongDateRange")
              .arg(this.bottom_.toString(this.formats_.get(0)))
              .arg(this.top_.toString(this.formats_.get(0)));
        }
      }
    }
  }
  /**
   * Sets the message to display when the date is later than top.
   *
   * <p>Depending on whether {@link WDateValidator#getBottom() getBottom()} and {@link
   * WDateValidator#getTop() getTop()} are defined, the default message is &quot;The date must be
   * between {1} and {2}&quot; or &quot;The date must be before {2}&quot;.
   */
  public void setInvalidTooLateText(final CharSequence text) {
    this.tooLateText_ = WString.toWString(text);
    this.repaint();
  }
  /**
   * Returns the message displayed when the date is too late.
   *
   * <p>
   *
   * @see WDateValidator#setInvalidTooLateText(CharSequence text)
   */
  public WString getInvalidTooLateText() {
    if (!(this.tooLateText_.length() == 0)) {
      return this.tooLateText_
          .clone()
          .arg(this.bottom_.toString(this.formats_.get(0)))
          .arg(this.top_.toString(this.formats_.get(0)));
    } else {
      if ((this.top_ == null)) {
        return new WString();
      } else {
        if ((this.bottom_ == null)) {
          return WString.tr("Wt.WDateValidator.DateTooLate")
              .arg(this.top_.toString(this.formats_.get(0)));
        } else {
          return WString.tr("Wt.WDateValidator.WrongDateRange")
              .arg(this.bottom_.toString(this.formats_.get(0)))
              .arg(this.top_.toString(this.formats_.get(0)));
        }
      }
    }
  }

  public String getJavaScriptValidate() {
    loadJavaScript(WApplication.getInstance());
    StringBuilder js = new StringBuilder();
    js.append("new Wt4.11.2.WDateValidator(").append(this.isMandatory()).append(",[");
    for (int i = 0; i < this.formats_.size(); ++i) {
      WDate.RegExpInfo r = WDate.formatToRegExp(this.formats_.get(i));
      if (i != 0) {
        js.append(',');
      }
      js.append("{")
          .append("regexp:")
          .append(WWebWidget.jsStringLiteral(r.regexp))
          .append(',')
          .append("getMonth:function(results){")
          .append(r.monthGetJS)
          .append(";},")
          .append("getDay:function(results){")
          .append(r.dayGetJS)
          .append(";},")
          .append("getYear:function(results){")
          .append(r.yearGetJS)
          .append(";}")
          .append("}");
    }
    js.append("],");
    if (!(this.bottom_ == null)) {
      js.append("new Date(")
          .append(this.bottom_.getYear())
          .append(',')
          .append(this.bottom_.getMonth() - 1)
          .append(',')
          .append(this.bottom_.getDay())
          .append(")");
    } else {
      js.append("null");
    }
    js.append(',');
    if (!(this.top_ == null)) {
      js.append("new Date(")
          .append(this.top_.getYear())
          .append(',')
          .append(this.top_.getMonth() - 1)
          .append(',')
          .append(this.top_.getDay())
          .append(")");
    } else {
      js.append("null");
    }
    js.append(',')
        .append(WString.toWString(this.getInvalidBlankText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidNotADateText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidTooEarlyText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidTooLateText()).getJsStringLiteral())
        .append(");");
    return js.toString();
  }

  private List<String> formats_;
  private WDate bottom_;
  private WDate top_;
  private WString tooEarlyText_;
  private WString tooLateText_;
  private WString notADateText_;

  private static void loadJavaScript(WApplication app) {
    app.loadJavaScript("js/WDateValidator.js", wtjs1());
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WDateValidator",
        "(function(e,t,a,i,l,g,n,r){this.validate=function(s){if(0===s.length)return e?{valid:!1,message:l}:{valid:!0};let u=null,o=-1,d=-1,m=-1;for(const e of t){u=new RegExp(\"^\"+e.regexp+\"$\").exec(s);if(null!==u){o=e.getMonth(u);d=e.getDay(u);m=e.getYear(u);break}}if(null===u)return{valid:!1,message:g};if(d<=0||d>31||o<=0||o>12)return{valid:!1,message:g};const v=new Date(m,o-1,d);return v.getDate()!==d||v.getMonth()!==o-1||v.getFullYear()!==m||v.getFullYear()<1400?{valid:!1,message:g}:a&&v.getTime()<a.getTime()?{valid:!1,message:n}:i&&v.getTime()>i.getTime()?{valid:!1,message:r}:{valid:!0}}})");
  }
}
