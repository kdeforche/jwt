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
 * A basic validator for email input.
 *
 * <p>This validator does basic email validation, to check if an email address is formed correctly
 * according to the WHATWG email input specification: <a
 * href="https://html.spec.whatwg.org/multipage/input.html#email-state-(type=email)">https://html.spec.whatwg.org/multipage/input.html#email-state-(type=email)</a>
 *
 * <p>This validator can also be used for {@link WEmailValidator#setMultiple(boolean multiple)
 * multiple} email addresses.
 *
 * <p>A regex {@link WEmailValidator#setPattern(CharSequence pattern) pattern} can be specified to
 * check that email addresses comply with this pattern.
 *
 * <p>
 *
 * <p><i><b>Note: </b>This validator only checks that the email address is correctly formed, but
 * does not do any further checks. If you need to be sure that the email address is valid, then we
 * recommend that you either send a confirmation email or use an email address verification service.
 * </i>
 */
public class WEmailValidator extends WValidator {
  private static Logger logger = LoggerFactory.getLogger(WEmailValidator.class);

  /** Creates an email validator. */
  public WEmailValidator() {
    super();
    this.pattern_ = new WString();
    this.notAnEmailAddressText_ = new WString();
    this.notMatchingText_ = new WString();
    this.multiple_ = false;
  }
  /**
   * Validates the given input.
   *
   * <p>The input is considered valid only when it is blank for a non-mandatory field, or represents
   * a properly formed email address, or a list of email addresses, and matches the {@link
   * WEmailValidator#getPattern() getPattern()} if non-empty.
   */
  public WValidator.Result validate(final String input) {
    String s = input;
    if (s.length() == 0) {
      if (this.isMandatory()) {
        return new WValidator.Result(ValidationState.InvalidEmpty, this.getInvalidBlankText());
      } else {
        return new WValidator.Result(ValidationState.Valid);
      }
    }
    boolean result = true;
    if (this.isMultiple()) {
      List<String> splits = new ArrayList<String>();
      StringUtils.split(splits, s, ",", false);
      for (String split : splits) {
        if (!this.validateOne(split)) {
          result = false;
          break;
        }
      }
    } else {
      result = this.validateOne(s);
    }
    if (result) {
      return new WValidator.Result(ValidationState.Valid);
    } else {
      if (!(this.getPattern().length() == 0)) {
        return new WValidator.Result(ValidationState.Invalid, this.getInvalidNotMatchingText());
      } else {
        return new WValidator.Result(
            ValidationState.Invalid, this.getInvalidNotAnEmailAddressText());
      }
    }
  }
  /**
   * Sets the message to display when the input is not a valid email address.
   *
   * <p>The default message is &quot;Must be a valid email address&quot;. This string is retrieved
   * using tr(&quot;Wt.WEmailValidator.Invalid&quot;) if {@link WEmailValidator#isMultiple()
   * isMultiple()} is <code>false</code>, or tr(&quot;Wt.WEmailValidator.Invalid.Multiple&quot;) if
   * {@link WEmailValidator#isMultiple() isMultiple()} is <code>true</code>.
   *
   * <p>
   *
   * @see WEmailValidator#getInvalidNotAnEmailAddressText()
   */
  public void setInvalidNotAnEmailAddressText(final CharSequence text) {
    this.notAnEmailAddressText_ = WString.toWString(text);
    this.repaint();
  }
  /**
   * Returns the message displayed when the input is not a valid email address.
   *
   * <p>
   *
   * @see WEmailValidator#setInvalidNotAnEmailAddressText(CharSequence text)
   */
  public WString getInvalidNotAnEmailAddressText() {
    if (!(this.notAnEmailAddressText_.length() == 0)) {
      return this.notAnEmailAddressText_;
    } else {
      if (this.isMultiple()) {
        return WString.tr("Wt.WEmailValidator.Invalid.Multiple");
      } else {
        return WString.tr("Wt.WEmailValidator.Invalid");
      }
    }
  }
  /**
   * Sets the message to display when the input does not match the required pattern.
   *
   * <p>The default message is &quot;Must be an email address matching the pattern
   * &apos;{1}&apos;&quot;, with <code>{1}</code> subsituted by the {@link
   * WEmailValidator#getPattern() getPattern()}. This string is retrieved using
   * tr(&quot;Wt.WEmailValidator.NotMaching&quot;) if {@link WEmailValidator#isMultiple()
   * isMultiple()} is <code>false</code>, or tr(&quot;Wt.WEmailValidator.NotMaching.Multiple&quot;)
   * if {@link WEmailValidator#isMultiple() isMultiple()} is <code>true</code>.
   *
   * <p>
   *
   * @see WEmailValidator#getInvalidNotMatchingText()
   */
  public void setInvalidNotMatchingText(final CharSequence text) {
    this.notMatchingText_ = WString.toWString(text);
    this.repaint();
  }
  /**
   * Returns the message displayed when the input does not match the required pattern.
   *
   * <p>
   *
   * @see WEmailValidator#setInvalidNotMatchingText(CharSequence text)
   */
  public WString getInvalidNotMatchingText() {
    if (!(this.notMatchingText_.length() == 0)) {
      return this.notMatchingText_.clone().arg(this.pattern_);
    } else {
      if (this.isMultiple()) {
        return WString.tr("Wt.WEmailValidator.NotMatching.Multiple").arg(this.pattern_);
      } else {
        return WString.tr("Wt.WEmailValidator.NotMatching").arg(this.pattern_);
      }
    }
  }
  /**
   * Sets whether multiple comma-separated email addresses are allowed.
   *
   * <p>
   *
   * @see WEmailValidator#isMultiple()
   */
  public void setMultiple(boolean multiple) {
    if (this.multiple_ != multiple) {
      this.multiple_ = multiple;
      this.repaint();
    }
  }
  /**
   * Returns whether multiple comma-separated email addresses are allowed.
   *
   * <p>
   *
   * @see WEmailValidator#setMultiple(boolean multiple)
   */
  public boolean isMultiple() {
    return this.multiple_;
  }
  /**
   * Sets the pattern for the input validation.
   *
   * <p>The pattern is in ECMAScript style regex.
   *
   * <p>
   *
   * @see WEmailValidator#getPattern()
   */
  public void setPattern(final CharSequence pattern) {
    if (!(this.pattern_.toString().equals(pattern.toString()))) {
      this.pattern_ = WString.toWString(pattern);
      this.repaint();
    }
  }
  /**
   * Returns the pattern used for the input validation.
   *
   * <p>The pattern is in ECMAScript style regex.
   *
   * <p>
   */
  public WString getPattern() {
    return this.pattern_;
  }

  public String getJavaScriptValidate() {
    loadJavaScript(WApplication.getInstance());
    StringBuilder js = new StringBuilder();
    js.append("new Wt4.11.2.WEmailValidator(")
        .append(this.isMandatory())
        .append(',')
        .append(this.isMultiple())
        .append(',')
        .append((this.pattern_.length() == 0) ? "null" : WWebWidget.jsStringLiteral(this.pattern_))
        .append(',')
        .append(WWebWidget.jsStringLiteral(this.getInvalidBlankText()))
        .append(',')
        .append(WWebWidget.jsStringLiteral(this.getInvalidNotAnEmailAddressText()))
        .append(',')
        .append(WWebWidget.jsStringLiteral(this.getInvalidNotMatchingText()))
        .append(");");
    return js.toString();
  }

  private WString pattern_;
  private WString notAnEmailAddressText_;
  private WString notMatchingText_;
  private boolean multiple_;

  private boolean validateOne(final String emailAddress) {
    Pattern emailAddressRegex =
        Pattern.compile(
            "[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*");
    if (!emailAddressRegex.matcher(emailAddress).matches()) {
      return false;
    }
    if (!(this.pattern_.length() == 0)
        && !Pattern.compile(this.pattern_.toString()).matcher(emailAddress).matches()) {
      return false;
    }
    return true;
  }

  private static void loadJavaScript(WApplication app) {
    app.loadJavaScript("js/WEmailValidator.js", wtjs1());
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WEmailValidator",
        "(function(t,e,a,n,i,l){const r=(()=>{const t=\"[a-zA-Z0-9]\",e=t+\"(?:[a-zA-Z0-9-]{0,61}\"+t+\")?\";return new RegExp(\"^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@\"+e+\"(?:\\\\.\"+e+\")*$\",\"u\")})(),s=a?new RegExp(\"^(?:\"+a+\")$\",\"u\"):null;function u(t){return r.test(t)&&(!s||s.test(t))}this.validate=function(a){if(0===a.length)return t?{valid:!1,message:n}:{valid:!0};let r;r=e?a.split(\",\").every(u):u(a);return r?{valid:!0}:s?{valid:!1,message:l}:{valid:!1,message:i}}})");
  }
}
