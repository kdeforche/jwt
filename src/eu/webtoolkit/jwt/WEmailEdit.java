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
 * A widget for inputting email addresses.
 *
 * <p>This widget directly corresponds to an <code>&lt;input type=&quot;email&quot;&gt;</code>:
 *
 * <p>
 *
 * <ul>
 *   <li>Values set with {@link WEmailEdit#setValueText(String value) setValueText()} or received
 *       from the client are automatically {@link WEmailEdit#sanitize(String input, boolean
 *       multiple) sanitized} according to the sanitization algorithm from the <a
 *       href="https://html.spec.whatwg.org/multipage/input.html#email-state-(type=email)">HTML
 *       specification</a>.
 *   <li>Browsers will automatically perform validation on the input.
 *   <li>On-screen keyboards should adjust themselves accordingly for email address input.
 *   <li>Optionally, a {@link WEmailEdit#setPattern(CharSequence pattern) regular expression} can be
 *       configured.
 *   <li>It&apos;s possible to enter {@link WEmailEdit#setMultiple(boolean multiple) multiple
 *       comma-separated email addresses} if configured.
 * </ul>
 *
 * <p>Upon construction, a {@link WEmailValidator} is automatically created and associated with the
 * {@link WEmailEdit}. Changing any of the email edit&apos;s properties, like the {@link
 * WEmailEdit#setPattern(CharSequence pattern) pattern} or whether {@link
 * WEmailEdit#setMultiple(boolean multiple) multiple addresses} are enabled, will automatically
 * cause the associated {@link WEmailEdit#getEmailValidator() email validator} to be updated and
 * vice versa.
 *
 * <p>
 *
 * <p><i><b>Note: </b>At the time of writing, Firefox does not do sanitization: <a
 * href="https://bugzilla.mozilla.org/show_bug.cgi?id=1518162">https://bugzilla.mozilla.org/show_bug.cgi?id=1518162</a>.
 * This may cause the browser to add the <code>:invalid</code> pseudo tag to inputs that are deemed
 * valid by JWt. </i>
 *
 * <p><i><b>Note: </b>Wt does not do any Punycode encoding or decoding. At the time of writing, if
 * you put an internationalized email address into a {@link WEmailEdit} on Blink-based browsers like
 * Google Chrome, it will be converted to Punycode by the browser. Firefox and Safari do not do this
 * encoding, so these email addresses will be deemed invalid by the {@link WEmailValidator}. </i>
 */
public class WEmailEdit extends WFormWidget {
  private static Logger logger = LoggerFactory.getLogger(WEmailEdit.class);

  /**
   * Creates a new email edit.
   *
   * <p>A default-constructed {@link WEmailEdit} will have {@link WEmailEdit#isMultiple()
   * isMultiple()} set to <code>false</code>, and the {@link WEmailEdit#getPattern() getPattern()}
   * set to the empty string (i.e. no pattern).
   *
   * <p>A default-constructed {@link WEmailValidator} will be automatically created and associated
   * with this {@link WEmailEdit}. This validator can be unset or changed using {@link
   * WFormWidget#setValidator(WValidator validator) WFormWidget#setValidator()}.
   */
  public WEmailEdit(WContainerWidget parentContainer) {
    super();
    this.pattern_ = new WString();
    this.value_ = "";
    this.flags_ = new BitSet();
    super.setInline(true);
    this.setFormObject(true);
    this.setValidator(new WEmailValidator());
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new email edit.
   *
   * <p>Calls {@link #WEmailEdit(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WEmailEdit() {
    this((WContainerWidget) null);
  }
  // public void  remove() ;
  /**
   * Returns the associated email validator.
   *
   * <p>If {@link WFormWidget#getValidator()} is <code>null</code> or not a <code>
   * {@link WEmailValidator}</code>, this will return <code>null</code>.
   *
   * <p>
   *
   * @see WFormWidget#setValidator(WValidator validator)
   */
  public WEmailValidator getEmailValidator() {
    return ObjectUtils.cast(this.getValidator(), WEmailValidator.class);
  }
  /**
   * Sets whether this input accepts multiple comma-separated email addresses.
   *
   * <p>
   *
   * @see WEmailEdit#isMultiple()
   */
  public void setMultiple(boolean multiple) {
    if (multiple != this.isMultiple()) {
      this.flags_.set(BIT_MULTIPLE_CHANGED);
      this.flags_.set(BIT_MULTIPLE, multiple);
      this.setValueText(this.getValueText());
      WEmailValidator validator = this.getEmailValidator();
      if (validator != null) {
        validator.setMultiple(multiple);
      }
      this.repaint();
    }
  }
  /**
   * Returns whether this input accepts multiple comma-separated email addresses.
   *
   * <p>
   *
   * @see WEmailEdit#setMultiple(boolean multiple)
   */
  public boolean isMultiple() {
    return this.flags_.get(BIT_MULTIPLE);
  }
  /**
   * Sets a regular expression that email addresses should match.
   *
   * <p>The regular expression is an ECMAScript style regular expression.
   *
   * <p>
   *
   * @see WEmailEdit#getPattern()
   */
  public void setPattern(final CharSequence pattern) {
    if (!(pattern.toString().equals(this.getPattern().toString()))) {
      this.flags_.set(BIT_PATTERN_CHANGED);
      this.pattern_ = WString.toWString(pattern);
      WEmailValidator validator = this.getEmailValidator();
      if (validator != null) {
        validator.setPattern(this.pattern_);
      }
      this.repaint();
    }
  }
  /**
   * Returns the regular expression that email addresses should match.
   *
   * <p>
   *
   * @see WEmailEdit#setPattern(CharSequence pattern)
   */
  public WString getPattern() {
    return this.pattern_;
  }
  /**
   * Event signal emitted when the text in the input field changed.
   *
   * <p>This signal is emitted whenever the text contents has changed. Unlike the {@link
   * WFormWidget#changed()} signal, the signal is fired on every change, not only when the focus is
   * lost. Unlike the {@link WInteractWidget#keyPressed()} signal, this signal is fired also for
   * other events that change the text, such as paste actions.
   *
   * <p>
   *
   * @see WInteractWidget#keyPressed()
   * @see WFormWidget#changed()
   */
  public EventSignal textInput() {
    return this.voidEventSignal(INPUT_SIGNAL, true);
  }
  /**
   * Sets the value.
   *
   * <p>The value will be automatically {@link WEmailEdit#sanitize(String input, boolean multiple)
   * sanitized}.
   *
   * <p>
   *
   * @see WEmailEdit#sanitize(String input, boolean multiple)
   */
  public void setValueText(final String value) {
    this.value_ = sanitize(value, this.isMultiple());
    this.flags_.set(BIT_VALUE_CHANGED);
    this.repaint();
  }
  /**
   * Returns the current value as an UTF-8 string.
   *
   * <p>
   *
   * @see WEmailEdit#setValueText(String value)
   */
  public String getValueText() {
    return this.value_;
  }
  /**
   * Sanitizes the given UTF-8 string, returning an UTF-8 string.
   *
   * <p>The sanitization is performed according to the WHATWG spec:
   *
   * <p>
   *
   * <ul>
   *   <li>If multiple is true, all leading or trailing ASCII whitespace is removed from every email
   *       address.
   *   <li>If multiple is false, all carriage return (<code>\r</code>) and newline (<code>\n</code>)
   *       characters are removed and all leading and trailing ASCII whitespace is removed.
   * </ul>
   *
   * <p>
   */
  public static String sanitize(final String input, final boolean multiple) {
    String u8String = input;
    if (multiple) {
      List<String> splits = new ArrayList<String>();
      StringUtils.split(splits, u8String, ",", false);
      int resultLength = 0;
      for (String split : splits) {
        split = eu.webtoolkit.jwt.utils.whatwg.InfraUtils.trim(split);
        resultLength += split.length() + 1;
      }
      String result = "";
      ;

      for (String split : splits) {
        if (result.length() != 0) {
          result += ',';
        }
        result += split;
      }
      return result;
    } else {
      u8String = eu.webtoolkit.jwt.utils.whatwg.InfraUtils.stripNewlines(u8String);
      u8String = eu.webtoolkit.jwt.utils.whatwg.InfraUtils.trim(u8String);
      return u8String;
    }
  }

  DomElementType getDomElementType() {
    return DomElementType.INPUT;
  }

  public void setFormData(final WObject.FormData formData) {
    if (this.flags_.get(BIT_VALUE_CHANGED) || this.isReadOnly()) {
      return;
    }
    if (!(formData.values.length == 0)) {
      String u8Value = formData.values[0];
      this.value_ = sanitize(u8Value, this.isMultiple());
    }
  }

  protected void render(EnumSet<RenderFlag> flags) {
    super.render(flags);
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WEmailEdit.js", wtjs1());
    super.setJavaScriptMember("wtEncodeValue", "Wt4.11.2.encodeEmailValue");
  }

  void updateDom(final DomElement element, boolean all) {
    super.updateDom(element, all);
    if (all) {
      element.setAttribute("type", "email");
    }
    if (all || this.flags_.get(BIT_MULTIPLE_CHANGED)) {
      if (this.isMultiple()) {
        element.setAttribute("multiple", "multiple");
      } else {
        if (!all) {
          element.removeAttribute("multiple");
        }
      }
      this.flags_.clear(BIT_MULTIPLE_CHANGED);
    }
    if (all || this.flags_.get(BIT_PATTERN_CHANGED)) {
      if (!(this.getPattern().length() == 0)) {
        element.setAttribute("pattern", this.getPattern().toString());
      } else {
        if (!all) {
          element.removeAttribute("pattern");
        }
      }
      this.flags_.clear(BIT_PATTERN_CHANGED);
    }
    if (all || this.flags_.get(BIT_VALUE_CHANGED)) {
      if (!(all && this.value_.length() == 0)) {
        element.setProperty(Property.Value, this.value_);
      }
      this.flags_.clear(BIT_VALUE_CHANGED);
    }
  }

  protected void validatorChanged() {
    WEmailValidator validator = this.getEmailValidator();
    if (validator != null) {
      this.setMultiple(validator.isMultiple());
      this.setPattern(validator.getPattern());
    }
    super.validatorChanged();
  }

  private static String INPUT_SIGNAL = "input";
  private WString pattern_;
  private String value_;
  private static final int BIT_MULTIPLE = 0;
  private static final int BIT_MULTIPLE_CHANGED = 1;
  private static final int BIT_PATTERN_CHANGED = 2;
  private static final int BIT_VALUE_CHANGED = 3;
  BitSet flags_;

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "encodeEmailValue",
        "(function(n){function e(n){return n.replaceAll(new RegExp(\"(^[\\\\t\\\\r\\\\f\\\\n ]+)|([\\\\t\\\\r\\\\f\\\\n ]+$)\",\"g\"),\"\")}return r=n.value,n.multiple?r.split(\",\").map(e).join(\",\"):e(function(n){return n.replaceAll(new RegExp(\"[\\\\r\\\\n]+\",\"g\"),\"\")}(r));var r})");
  }
}
