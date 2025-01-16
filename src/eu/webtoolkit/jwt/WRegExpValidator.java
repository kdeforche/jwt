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
 * A validator that checks user input against a regular expression.
 *
 * <p>This validator checks whether user input matches the given regular expression. It checks the
 * complete input; prefix ^ and suffix $ are not needed.
 *
 * <p>The regex should be specified using ECMAScript syntax (<a
 * href="http://en.cppreference.com/w/cpp/regex/ecmascript">http://en.cppreference.com/w/cpp/regex/ecmascript</a>)
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * WLineEdit lineEdit = new WLineEdit(this);
 * // an email address validator
 * WRegExpValidator validator = new WRegExpValidator("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}");
 * lineEdit.setValidator(validator);
 * lineEdit.setText("pieter@emweb.be");
 *
 * }</pre>
 *
 * <p>
 *
 * <p><i><b>Note: </b>This validator does not fully support unicode: it matches on the
 * CharEncoding::UTF8-encoded representation of the string. </i>
 *
 * <h3>i18n</h3>
 *
 * <p>The strings used in this class can be translated by overriding the default values for the
 * following localization keys:
 *
 * <ul>
 *   <li>Wt.WRegExpValidator.Invalid: Invalid input
 * </ul>
 */
public class WRegExpValidator extends WValidator {
  private static Logger logger = LoggerFactory.getLogger(WRegExpValidator.class);

  /** Sets a new regular expression validator. */
  public WRegExpValidator() {
    super();
    this.pattern_ = "";
    this.regex_ = null;
    this.noMatchText_ = new WString();
  }
  /**
   * Sets a new regular expression validator that accepts input that matches the given regular
   * expression.
   *
   * <p>This constructs a validator that matches the regular expression <code>expr</code>.
   */
  public WRegExpValidator(final String pattern) {
    super();
    this.pattern_ = pattern;
    this.regex_ = Pattern.compile(pattern);
    this.noMatchText_ = new WString();
  }
  /**
   * Sets the regular expression for valid input.
   *
   * <p>Sets the ECMAscript regular expression <code>expr</code>.
   */
  public void setRegExp(final String pattern) {
    this.regex_ = Pattern.compile(pattern);
    this.pattern_ = pattern;
    this.repaint();
  }
  /**
   * Returns the regular expression for valid input.
   *
   * <p>Returns the ECMAScript regular expression.
   */
  public String getRegExpPattern() {
    return this.pattern_;
  }
  /** Returns the regular expression for valid input. */
  public Pattern getRegExp() {
    return this.regex_;
  }
  /** Sets regular expression matching flags. */
  public void setFlags(int flags) {
    if (EnumUtils.valueOf(flags) == EnumUtils.valueOf(this.getFlags())) {
      return;
    }
    if ((EnumUtils.valueOf(flags) & (int) Pattern.CASE_INSENSITIVE) != 0) {
      this.regex_ = Pattern.compile(this.pattern_, Pattern.CASE_INSENSITIVE);
    } else {
      this.regex_ = Pattern.compile(this.pattern_);
    }
    this.repaint();
  }
  /** Returns regular expression matching flags. */
  public int getFlags() {
    if ((this.regex_.flags() & Pattern.CASE_INSENSITIVE) != 0) {
      return Pattern.CASE_INSENSITIVE;
    } else {
      return (int) 0;
    }
  }
  /**
   * Validates the given input.
   *
   * <p>The input is considered valid only when it is blank for a non-mandatory field, or matches
   * the regular expression.
   */
  public WValidator.Result validate(final String input) {
    if (input.length() == 0) {
      return super.validate(input);
    }
    if (this.regex_.matcher(input).matches()) {
      return new WValidator.Result(ValidationState.Valid);
    } else {
      return new WValidator.Result(ValidationState.Invalid, this.getInvalidNoMatchText());
    }
  }
  /**
   * Sets the message to display when the input does not match.
   *
   * <p>The default value is &quot;Invalid input&quot;.
   */
  public void setInvalidNoMatchText(final CharSequence text) {
    this.noMatchText_ = WString.toWString(text);
    this.repaint();
  }
  /**
   * Returns the message displayed when the input does not match.
   *
   * <p>
   *
   * @see WRegExpValidator#setInvalidNoMatchText(CharSequence text)
   */
  public WString getInvalidNoMatchText() {
    if (!(this.noMatchText_.length() == 0)) {
      return this.noMatchText_;
    } else {
      return WString.tr("Wt.WRegExpValidator.Invalid");
    }
  }

  public String getJavaScriptValidate() {
    loadJavaScript(WApplication.getInstance());
    StringBuilder js = new StringBuilder();
    js.append("new Wt4.11.2.WRegExpValidator(").append(this.isMandatory()).append(',');
    js.append(WWebWidget.jsStringLiteral(this.pattern_)).append(",'");
    if ((this.regex_.flags() & Pattern.CASE_INSENSITIVE) != 0) {
      js.append('i');
    }
    js.append('\'');
    js.append(',')
        .append(WWebWidget.jsStringLiteral(this.getInvalidBlankText()))
        .append(',')
        .append(WWebWidget.jsStringLiteral(this.getInvalidNoMatchText()))
        .append(");");
    return js.toString();
  }

  private String pattern_;
  private Pattern regex_;
  private WString noMatchText_;

  private static void loadJavaScript(WApplication app) {
    app.loadJavaScript("js/WRegExpValidator.js", wtjs1());
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WRegExpValidator",
        "(function(e,n,t,l,a){const i=n?new RegExp(n,t):null;this.validate=function(n){if(0===n.length)return e?{valid:!1,message:l}:{valid:!0};if(i){const e=i.exec(n);return null!==e&&e[0].length===n.length?{valid:!0}:{valid:!1,message:a}}return{valid:!0}}})");
  }

  private static int MatchCaseInsensitive = 1;
}
