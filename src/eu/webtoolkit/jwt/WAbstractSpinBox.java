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
 * An abstract spin box.
 *
 * <p>Although the element can be rendered using a native HTML5 control, by default it is rendered
 * using an HTML4 compatibility workaround which is implemented using JavaScript and CSS, as most
 * browsers do not yet implement the HTML5 native element.
 */
public abstract class WAbstractSpinBox extends WLineEdit {
  private static Logger logger = LoggerFactory.getLogger(WAbstractSpinBox.class);

  /**
   * Configures whether a native HTML5 control should be used.
   *
   * <p>When <code>native</code>, the new &quot;number&quot; input element, specified by HTML5 and
   * when implemented by the browser, is used rather than the built-in element. The native control
   * is styled by the browser (usually in sync with the OS) rather than through the theme chosen.
   *
   * <p>The default is <code>false</code> (as native support is now well implemented).
   */
  public void setNativeControl(boolean nativeControl) {
    this.preferNative_ = nativeControl;
  }
  /**
   * Returns whether a native HTML5 control is used.
   *
   * <p>Taking into account the preference for a native control, configured using {@link
   * WAbstractSpinBox#setNativeControl(boolean nativeControl) setNativeControl()}, this method
   * returns whether a native control is actually being used.
   */
  public boolean isNativeControl() {
    if (this.preferNative_) {
      if (super.getInputMask().length() != 0) {
        return false;
      }
      final WEnvironment env = WApplication.getInstance().getEnvironment();
      if (env.agentIsChrome()
              && (int) env.getAgent().getValue() >= (int) UserAgent.Chrome5.getValue()
          || env.agentIsSafari()
              && (int) env.getAgent().getValue() >= (int) UserAgent.Safari4.getValue()
          || env.agentIsOpera()
              && (int) env.getAgent().getValue() >= (int) UserAgent.Opera10.getValue()) {
        return true;
      }
    }
    return false;
  }
  /**
   * Sets a prefix.
   *
   * <p>Option to set a prefix string shown in front of the value, e.g.:
   *
   * <p>
   *
   * <pre>{@code
   * spinBox.setPrefix("$ ");
   *
   * }</pre>
   *
   * <p>The default prefix is empty.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Not supported by the native controls. </i>
   */
  public void setPrefix(final CharSequence prefix) {
    if (!(this.prefix_.toString().equals(prefix.toString()))) {
      this.prefix_ = WString.toWString(prefix);
      this.setText(this.getTextFromValue());
      this.changed_ = true;
      this.repaint();
    }
  }
  /**
   * Returns the prefix.
   *
   * <p>
   *
   * @see WAbstractSpinBox#setPrefix(CharSequence prefix)
   */
  public WString getPrefix() {
    return this.prefix_;
  }
  /**
   * Sets a suffix.
   *
   * <p>Option to set a suffix string shown to the right of the value, e.g.:
   *
   * <p>
   *
   * <pre>{@code
   * spinBox.setSuffix(" crates");
   *
   * }</pre>
   *
   * <p>The default suffix is empty.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Not supported by the native controls. </i>
   */
  public void setSuffix(final CharSequence suffix) {
    if (!(this.suffix_.toString().equals(suffix.toString()))) {
      this.suffix_ = WString.toWString(suffix);
      this.setText(this.getTextFromValue());
      this.changed_ = true;
      this.repaint();
    }
  }
  /**
   * Returns the suffix.
   *
   * <p>
   *
   * @see WAbstractSpinBox#setSuffix(CharSequence suffix)
   */
  public WString getSuffix() {
    return this.suffix_;
  }

  public void setText(final String text) {
    this.parseValue(text);
    super.setText(this.getTextFromValue());
  }

  public ValidationState validate() {
    return super.validate();
  }

  public void refresh() {
    if (this.isRendered()) {
      this.doJavaScript(
          this.getJsRef()
              + ".wtObj.setLocale("
              + jsStringLiteral(LocaleUtils.getDecimalPoint(LocaleUtils.getCurrentLocale()))
              + ","
              + jsStringLiteral(LocaleUtils.getGroupSeparator(LocaleUtils.getCurrentLocale()))
              + ");");
    }
    super.refresh();
  }

  public JSignal2<Integer, Integer> jsValueChanged() {
    return this.jsValueChanged_;
  }
  /** Constructor. */
  protected WAbstractSpinBox(WContainerWidget parentContainer) {
    super();
    this.changed_ = false;
    this.valueChangedConnection_ = false;
    this.preferNative_ = false;
    this.setup_ = false;
    this.prefix_ = new WString();
    this.suffix_ = new WString();
    this.jsValueChanged_ = new JSignal2<Integer, Integer>(this, "spinboxValueChanged", true) {};
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Constructor.
   *
   * <p>Calls {@link #WAbstractSpinBox(WContainerWidget parentContainer)
   * this((WContainerWidget)null)}
   */
  protected WAbstractSpinBox() {
    this((WContainerWidget) null);
  }

  void updateDom(final DomElement element, boolean all) {
    if (all || this.changed_) {
      if (!all) {
        if (!this.isNativeControl()) {
          this.doJavaScript(
              this.getJsRef()
                  + ".wtObj.configure("
                  + String.valueOf(this.getDecimals())
                  + ","
                  + WString.toWString(this.getPrefix()).getJsStringLiteral()
                  + ","
                  + WString.toWString(this.getSuffix()).getJsStringLiteral()
                  + ","
                  + this.getJsMinMaxStep()
                  + ");");
        } else {
          this.setValidator(this.createValidator());
        }
      }
    }
    this.changed_ = false;
    super.updateDom(element, all);
    if (all && this.isNativeControl()) {
      element.setAttribute("type", "number");
    }
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (!this.setup_ && flags.contains(RenderFlag.Full)) {
      this.setup();
    }
    if (this.jsValueChanged().needsUpdate(true)) {
      StringBuilder function = new StringBuilder();
      function.append(this.getJsRef()).append(".wtObj.jsValueChanged=");
      if (this.jsValueChanged().isConnected()) {
        function
            .append("function(oldv, v){")
            .append("var o=null;var e=null;")
            .append(this.jsValueChanged().createCall("oldv", "v"))
            .append("};");
      } else {
        function.append("function() {};");
      }
      this.doJavaScript(function.toString());
    }
    super.render(flags);
  }

  protected void setFormData(final WObject.FormData formData) {
    super.setFormData(formData);
    this.parseValue(this.getText());
  }

  void propagateRenderOk(boolean deep) {
    this.changed_ = false;
    super.propagateRenderOk(deep);
  }

  abstract String getJsMinMaxStep();

  abstract int getDecimals();

  abstract boolean parseNumberValue(final String text);

  protected abstract String getTextFromValue();

  abstract WValidator createValidator();

  protected abstract WValidator.Result getValidateRange();

  protected int boxPadding(Orientation orientation) {
    if (!this.isNativeControl() && orientation == Orientation.Horizontal) {
      return super.boxPadding(orientation) + 8;
    } else {
      return super.boxPadding(orientation);
    }
  }

  boolean changed_;
  boolean valueChangedConnection_;
  private boolean preferNative_;
  private boolean setup_;
  private WString prefix_;
  private WString suffix_;

  private void defineJavaScript() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WSpinBox.js", wtjs1());
    StringBuilder ss = new StringBuilder();
    ss.append("new Wt4.11.2.WSpinBox(")
        .append(app.getJavaScriptClass())
        .append(",")
        .append(this.getJsRef())
        .append(",")
        .append(this.getDecimals())
        .append(",")
        .append(WString.toWString(this.getPrefix()).getJsStringLiteral())
        .append(",")
        .append(WString.toWString(this.getSuffix()).getJsStringLiteral())
        .append(",")
        .append(this.getJsMinMaxStep())
        .append(",")
        .append(jsStringLiteral(LocaleUtils.getDecimalPoint(LocaleUtils.getCurrentLocale())))
        .append(",")
        .append(jsStringLiteral(LocaleUtils.getGroupSeparator(LocaleUtils.getCurrentLocale())))
        .append(");");
    this.setJavaScriptMember(" WSpinBox", ss.toString());
  }

  private void connectJavaScript(final AbstractEventSignal s, final String methodName) {
    String jsFunction =
        "function(obj, event) {var o = "
            + this.getJsRef()
            + ";if (o && o.wtObj) o.wtObj."
            + methodName
            + "(obj, event);}";
    s.addListener(jsFunction);
  }

  private void setup() {
    this.setup_ = true;
    boolean useNative = this.isNativeControl();
    if (!useNative) {
      this.defineJavaScript();
      final AbstractEventSignal b = this.mouseMoved();
      final AbstractEventSignal c = this.keyWentDown();
      this.connectJavaScript(this.mouseMoved(), "mouseMove");
      this.connectJavaScript(this.mouseWentUp(), "mouseUp");
      this.connectJavaScript(this.mouseWentDown(), "mouseDown");
      this.connectJavaScript(this.mouseWentOut(), "mouseOut");
      this.connectJavaScript(this.keyWentDown(), "keyDown");
      this.connectJavaScript(this.keyWentUp(), "keyUp");
      if (!(this.prefix_.length() == 0) || !(this.suffix_.length() == 0)) {
        this.setValidator(new SpinBoxValidator(this));
      }
    }
  }

  boolean parseValue(final String text) {
    String textUtf8 = text;
    boolean valid = true;
    if (!this.isNativeControl()) {
      valid = false;
      String prefixUtf8 = this.prefix_.toString();
      String suffixUtf8 = this.suffix_.toString();
      if (textUtf8.startsWith(prefixUtf8)) {
        textUtf8 = textUtf8.substring(prefixUtf8.length());
        if (textUtf8.endsWith(suffixUtf8)) {
          textUtf8 = textUtf8.substring(0, 0 + textUtf8.length() - suffixUtf8.length());
          valid = true;
        }
      }
    }
    if (valid) {
      valid = textUtf8.length() > 0;
    }
    if (valid) {
      valid = this.parseNumberValue(textUtf8);
    }
    return valid;
  }

  private JSignal2<Integer, Integer> jsValueChanged_;

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WSpinBox",
        "(function(t,e,s,n,i,o,l,a,c,u){const f=\"Must be a number\",r=\"The number must be at least \",d=\"The number may be at most \",h=\"dn\",p=\"up\",y=\"unselectable\";e.wtObj=this;const b=this,v=t.WT,g=\"crosshair\";let m,L=null,W=!1,x=null,C=!1,R=!1;function w(){return e.readOnly}function j(){const t=e.wtLObj;let s=\"\";if(void 0!==t){s=t.getValue();\"\"===s&&(s=n+\"0\"+i)}else s=e.value;if(s.startsWith(n)){s=s.substring(n.length);if(s.endsWith(i)){s=s.substring(0,s.length-i.length);u&&(s=s.split(u).join(\"\"));s=s.replace(c,\".\");return Number(s)}}return null}function E(t){const a=e.wtLObj;if(t>l)if(R){t=o+(t-o)%(l-o+1)}else t=l;else if(t<o)if(R){const e=l-o;t=l-(Math.abs(t-o)-1)%(e+1)}else t=o;let f=t.toFixed(s);f=f.replace(\".\",c);const r=f.indexOf(c);let d=\"\";if(-1!==r){for(let t=0;t<r;t++){d+=f.charAt(t);t<r-1&&(r-t-1)%3==0&&(d+=u)}d+=f.substr(r)}else d=f;const h=e.value;void 0!==a?a.setValue(n+d+i):e.value=n+d+i;W=!0;b.jsValueChanged(h,t)}function V(){let t=j();if(null!==t){t+=a;E(t)}}function D(){let t=j();if(null!==t){t-=a;E(t)}}this.setIsDoubleSpinBox=function(t){C=t;this.configure(s,n,i,o,l,a)};this.setWrapAroundEnabled=function(t){R=t};this.configure=function(t,e,c,u,h,p){s=t;n=e;i=c;o=u;l=h;a=p;const y=C||void 0===v.WIntValidator;x=y?new v.WDoubleValidator(!0,!1,o,l,\".\",\"\",f,f,r+o,d+l):new v.WIntValidator(!0,o,l,\"\",f,f,r+o,d+l)};this.mouseOut=function(t,s){e.classList.remove(h,p)};this.mouseMove=function(t,s){if(!w())if(L){const t=v.pageCoordinates(s).y-L.y;let e=m;if(null!==e){e-=t*a;E(e)}}else{const t=v.widgetCoordinates(e,s);(e.classList.contains(h)||e.classList.contains(p))&&e.classList.remove(h,p);let n=-1;\"object\"==typeof v.theme&&\"bootstrap\"===v.theme.type&&(n=v.theme.version);if(n>=4&&t.x>e.offsetWidth-30&&t.x<e.offsetWidth-10){const s=e.offsetHeight/2;if(t.y>=s-3&&t.y<=s+3)e.style.cursor=g;else{e.style.cursor=\"default\";t.y<s-1?e.classList.add(p):e.classList.add(h)}}else if(n<4&&t.x>e.offsetWidth-22){const s=e.offsetHeight/2;if(t.y>=s-3&&t.y<=s+3)e.style.cursor=g;else{e.style.cursor=\"default\";t.y<s-1?e.classList.add(p):e.classList.add(h)}}else\"\"!==e.style.cursor&&(e.style.cursor=\"\")}};this.mouseDown=function(t,s){v.capture(null);if(!w())if(e.style.cursor===g){v.capture(null);v.capture(e);e.classList.add(y);L=v.pageCoordinates(s);m=j()}else{const t=v.widgetCoordinates(e,s);let n=-1;\"object\"==typeof v.theme&&\"bootstrap\"===v.theme.type&&(n=v.theme.version);if(n>=5&&t.x>e.offsetWidth-30&&t.x<e.offsetWidth-10){v.cancelEvent(s);v.capture(e);e.classList.add(y);const n=e.offsetHeight/2;t.y<n?v.eventRepeat((function(){V()})):v.eventRepeat((function(){D()}))}else if(n<4&&t.x>e.offsetWidth-22){v.cancelEvent(s);v.capture(e);e.classList.add(y);const n=e.offsetHeight/2;t.y<n?v.eventRepeat((function(){V()})):v.eventRepeat((function(){D()}))}}};this.mouseUp=function(t,s){e.classList.remove(y);if(!w()){if(W||null!==L){L=null;W=!1;t.onchange()}v.stopRepeat()}};this.keyDown=function(t,e){w()||(40===e.keyCode?v.eventRepeat((function(){D()})):38===e.keyCode&&v.eventRepeat((function(){V()})))};this.keyUp=function(t,e){if(!w()){if(W){W=!1;t.onchange()}v.stopRepeat()}};this.setLocale=function(t,e){c=t;u=e};this.validate=function(t){let e=j();null===e&&(e=\"a\");return x.validate(e)};this.jsValueChanged=function(){};this.setIsDoubleSpinBox(C)})");
  }
}
