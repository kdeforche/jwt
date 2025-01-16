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
 * A validator that checks the string length of user input.
 *
 * <p>This validator checks whether user input is within the specified range of accepted string
 * lengths.
 *
 * <p>If you only want to limit the length on a line edit, you may also use {@link
 * WLineEdit#setMaxLength(int chars) WLineEdit#setMaxLength()}.
 *
 * <p>
 *
 * <h3>i18n</h3>
 *
 * <p>The strings used in this class can be translated by overriding the default values for the
 * following localization keys:
 *
 * <ul>
 *   <li>Wt.WLengthValidator.TooShort: The input must be at least {1} characters
 *   <li>Wt.WLengthValidator.BadRange: The input must have a length between {1} and {2} characters
 *   <li>Wt.WLengthValidator.TooLong: The input must be no more than {1} characters
 * </ul>
 */
public class WLengthValidator extends WValidator {
  private static Logger logger = LoggerFactory.getLogger(WLengthValidator.class);

  /** Creates a length validator that accepts input of any length. */
  public WLengthValidator() {
    super();
    this.minLength_ = 0;
    this.maxLength_ = Integer.MAX_VALUE;
    this.tooLongText_ = new WString();
    this.tooShortText_ = new WString();
  }
  /** Creates a length validator that accepts input within a length range. */
  public WLengthValidator(int minLength, int maxLength) {
    super();
    this.minLength_ = minLength;
    this.maxLength_ = maxLength;
    this.tooLongText_ = new WString();
    this.tooShortText_ = new WString();
  }
  /**
   * Sets the minimum length.
   *
   * <p>The default value is 0.
   */
  public void setMinimumLength(int minLength) {
    if (this.minLength_ != minLength) {
      this.minLength_ = minLength;
      this.repaint();
    }
  }
  /**
   * Returns the minimum length.
   *
   * <p>
   *
   * @see WLengthValidator#setMinimumLength(int minLength)
   */
  public int getMinimumLength() {
    return this.minLength_;
  }
  /**
   * Sets the maximum length.
   *
   * <p>The default value is the maximum integer value.
   */
  public void setMaximumLength(int maxLength) {
    if (this.maxLength_ != maxLength) {
      this.maxLength_ = maxLength;
      this.repaint();
    }
  }
  /**
   * Returns the maximum length.
   *
   * <p>
   *
   * @see WLengthValidator#setMaximumLength(int maxLength)
   */
  public int getMaximumLength() {
    return this.maxLength_;
  }
  /**
   * Validates the given input.
   *
   * <p>The input is considered valid only when it is blank for a non-mandatory field, or has a
   * length within the valid range.
   */
  public WValidator.Result validate(final String input) {
    if (input.length() == 0) {
      return super.validate(input);
    }
    String text = input;
    if ((int) text.length() < this.minLength_) {
      return new WValidator.Result(ValidationState.Invalid, this.getInvalidTooShortText());
    } else {
      if ((int) text.length() > this.maxLength_) {
        return new WValidator.Result(ValidationState.Invalid, this.getInvalidTooLongText());
      } else {
        return new WValidator.Result(ValidationState.Valid);
      }
    }
  }
  /**
   * Sets the message to display when the input is too short.
   *
   * <p>Depending on whether {@link WLengthValidator#getMaximumLength() getMaximumLength()} is a
   * real bound, the default message is &quot;The input must have a length between {1} and {2}
   * characters&quot; or &quot; &quot;The input must be at least {1} characters".
   */
  public void setInvalidTooShortText(final CharSequence text) {
    this.tooShortText_ = WString.toWString(text);
    this.repaint();
  }
  /**
   * Returns the message displayed when the input is too short.
   *
   * <p>
   *
   * @see WLengthValidator#setInvalidTooShortText(CharSequence text)
   */
  public WString getInvalidTooShortText() {
    if (!(this.tooShortText_.length() == 0)) {
      return this.tooShortText_.clone().arg(this.minLength_).arg(this.maxLength_);
    } else {
      if (this.minLength_ == 0) {
        return new WString();
      } else {
        if (this.maxLength_ == Integer.MAX_VALUE) {
          return WString.tr("Wt.WLengthValidator.TooShort").arg(this.minLength_);
        } else {
          return WString.tr("Wt.WLengthValidator.BadRange")
              .arg(this.minLength_)
              .arg(this.maxLength_);
        }
      }
    }
  }
  /**
   * Sets the message to display when the input is too long.
   *
   * <p>Depending on whether {@link WLengthValidator#getMinimumLength() getMinimumLength()} is
   * different from zero, the default message is &quot;The input must have a length between {1} and
   * {2} characters&quot; or &quot; &quot;The input must be no more than {2} characters".
   */
  public void setInvalidTooLongText(final CharSequence text) {
    this.tooLongText_ = WString.toWString(text);
    this.repaint();
  }
  /**
   * Returns the message displayed when the input is too long.
   *
   * <p>
   *
   * @see WLengthValidator#setInvalidTooLongText(CharSequence text)
   */
  public WString getInvalidTooLongText() {
    if (!(this.tooLongText_.length() == 0)) {
      return this.tooLongText_.clone().arg(this.minLength_).arg(this.maxLength_);
    } else {
      if (this.maxLength_ == Integer.MAX_VALUE) {
        return new WString();
      } else {
        if (this.minLength_ == 0) {
          return WString.tr("Wt.WLengthValidator.TooLong").arg(this.maxLength_);
        } else {
          return WString.tr("Wt.WLengthValidator.BadRange")
              .arg(this.minLength_)
              .arg(this.maxLength_);
        }
      }
    }
  }

  public String getJavaScriptValidate() {
    loadJavaScript(WApplication.getInstance());
    StringBuilder js = new StringBuilder();
    js.append("new Wt4.11.2.WLengthValidator(").append(this.isMandatory()).append(',');
    if (this.minLength_ != 0) {
      js.append(this.minLength_);
    } else {
      js.append("null");
    }
    js.append(',');
    if (this.maxLength_ != Integer.MAX_VALUE) {
      js.append(this.maxLength_);
    } else {
      js.append("null");
    }
    js.append(',')
        .append(WString.toWString(this.getInvalidBlankText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidTooShortText()).getJsStringLiteral())
        .append(',')
        .append(WString.toWString(this.getInvalidTooLongText()).getJsStringLiteral())
        .append(");");
    return js.toString();
  }

  private int minLength_;
  private int maxLength_;
  private WString tooLongText_;
  private WString tooShortText_;

  private static void loadJavaScript(WApplication app) {
    app.loadJavaScript("js/WLengthValidator.js", wtjs1());
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WLengthValidator",
        "(function(a,l,t,e,n,i){this.validate=function(s){return 0===s.length?a?{valid:!1,message:e}:{valid:!0}:null!==l&&s.length<l?{valid:!1,message:n}:null!==t&&s.length>t?{valid:!1,message:i}:{valid:!0}}})");
  }
}
