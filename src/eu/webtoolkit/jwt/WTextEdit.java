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
 * A rich-text XHTML editor.
 *
 * <p>The editor provides interactive editing of XHTML text. By default it provides basic mark-up
 * (font, formatting, color, links, and lists), but additional buttons may be added to the tool bars
 * that add additional formatting options.
 *
 * <p>The implementation is based on <a href="http://tinymce.moxiecode.com/">TinyMCE</a>. The widget
 * may be configured and tailored using the {@link WTextEdit#setConfigurationSetting(String name,
 * Object value) setConfigurationSetting()} and related methods that provide direct access to the
 * underlying TinyMCE component.
 *
 * <p>You can use this widget with TinyMCE version 3 or version 4.
 *
 * <p>The choice is global and set using Configuration#setTinyMCEVersion(int).
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>Styling through CSS is not applicable.
 *
 * <p><div align="center"> <img src="doc-files/WTextEdit-1.png">
 *
 * <p><strong>Default configuration of a WTextEdit</strong> </div>
 */
public class WTextEdit extends WTextArea {
  private static Logger logger = LoggerFactory.getLogger(WTextEdit.class);

  /** Creates a new text editor. */
  public WTextEdit(WContainerWidget parentContainer) {
    super();
    this.onChange_ = new JSignal(this, "change");
    this.onRender_ = new JSignal(this, "render");
    this.contentChanged_ = false;
    this.configurationSettings_ = new HashMap<String, Object>();
    this.init();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new text editor.
   *
   * <p>Calls {@link #WTextEdit(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WTextEdit() {
    this((WContainerWidget) null);
  }
  /**
   * Creates a new text editor and initialize with given text.
   *
   * <p>The <code>text</code> should be valid XHTML.
   */
  public WTextEdit(final String text, WContainerWidget parentContainer) {
    super(text, (WContainerWidget) null);
    this.onChange_ = new JSignal(this, "change");
    this.onRender_ = new JSignal(this, "render");
    this.contentChanged_ = false;
    this.configurationSettings_ = new HashMap<String, Object>();
    this.init();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new text editor and initialize with given text.
   *
   * <p>Calls {@link #WTextEdit(String text, WContainerWidget parentContainer) this(text,
   * (WContainerWidget)null)}
   */
  public WTextEdit(final String text) {
    this(text, (WContainerWidget) null);
  }
  /** Destructor. */
  public void remove() {
    super.remove();
  }
  /**
   * Returns the TinyMCE version.
   *
   * <p>This returns the configured version of TinyMCE (currently 3 or 4).
   */
  public int getVersion() {
    return this.version_;
  }
  /**
   * Sets the content.
   *
   * <p>The <code>text</code> should be valid XHTML.
   *
   * <p>The default value is &quot;&quot;.
   */
  public void setText(final String text) {
    super.setText(text);
    this.contentChanged_ = true;
  }
  /**
   * Sets the stylesheet for displaying the content.
   *
   * <p>The content is rendered using the rules defined in this stylesheet. The stylesheet is also
   * used to derive additional styles that are available in the text editor, for example in the
   * &quot;styleselect&quot; button.
   *
   * <p>Multiple stylesheets may be specified as a comma separated list.
   */
  public void setStyleSheet(final String uri) {
    this.setConfigurationSetting("content_css", uri);
  }
  /**
   * Returns the content stylesheet.
   *
   * <p>
   *
   * @see WTextEdit#setStyleSheet(String uri)
   */
  public String getStyleSheet() {
    return StringUtils.asString(this.getConfigurationSetting("content_css")).toString();
  }
  /**
   * Loads additional TinyMCE plugins.
   *
   * <p>Wt loads by default only the plugin &apos;safari&apos; (which adds support for the Safari
   * web browser). Use this method to load additional plugins. Multiple plugins may be specified as
   * a comma separated list.
   *
   * <p>The various plugins are described in the <a
   * href="http://www.tinymce.com/wiki.php/Plugins">TinyMCE documentation</a>.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Plugins can only be loaded before the initial display of the widget. </i>
   */
  public void setExtraPlugins(final String plugins) {
    this.setConfigurationSetting("plugins", plugins);
  }
  /**
   * Returns the extra plugins.
   *
   * <p>
   *
   * @see WTextEdit#setExtraPlugins(String plugins)
   */
  public String getExtraPlugins() {
    return StringUtils.asString(this.getConfigurationSetting("plugins")).toString();
  }
  /**
   * Configures a tool bar.
   *
   * <p>This configures the buttons for the <code>i&apos;th</code> tool bar (with 0 &lt;= <code>i
   * </code> &lt;= 3).
   *
   * <p>
   *
   * <h3>TinyMCE 3</h3>
   *
   * <p>The syntax and available buttons is documented in the <a
   * href="http://www.tinymce.com/wiki.php/Configuration3x:theme_advanced_buttons_1_n">TinyMCE
   * documentation</a>.
   *
   * <p>The default <i>config</i> for the first tool bar (<code>i</code> = 0) is: "fontselect, |,
   * bold, italic, underline, |, fontsizeselect, |, forecolor, backcolor, |, justifyleft,
   * justifycenter, justifyright, justifyfull, |, anchor, |, numlist, bullist".
   *
   * <p>By default, the other three tool bars are disabled (<code>config</code> = &quot;&quot;).
   *
   * <p>
   *
   * <h3>TinyMCE 4</h3>
   *
   * <p>The syntax and available buttons is documented in the <a
   * href="http://www.tinymce.com/wiki.php/Configuration:toolbar%3CN%3E">TinyMCEdocumentation</a>.
   *
   * <p>The default <i>config</i> for the first tool bar (<code>i</code> = 0) is: &quot;undo redo |
   * styleselect | bold italic | link&quot;.
   *
   * <p>Some buttons are only available after loading extra plugins using {@link
   * WTextEdit#setExtraPlugins(String plugins) setExtraPlugins()}.
   *
   * <p>
   *
   * <p><i><b>Note: </b>The tool bar configuration can only be set before the initial display of the
   * widget. </i>
   */
  public void setToolBar(int i, final String config) {
    String setting = "";
    if (this.version_ < 4) {
      setting = "theme_advanced_buttons";
    } else {
      setting = "toolbar";
    }
    this.setConfigurationSetting(setting + String.valueOf(i + 1), config);
  }
  /**
   * Returns a tool bar configuration.
   *
   * <p>
   *
   * @see WTextEdit#setToolBar(int i, String config)
   */
  public String getToolBar(int i) {
    String setting = "";
    if (this.version_ < 4) {
      setting = "theme_advanced_buttons";
    } else {
      setting = "toolbar";
    }
    return StringUtils.asString(this.getConfigurationSetting(setting + String.valueOf(i + 1)))
        .toString();
  }
  /**
   * Configure a TinyMCE setting.
   *
   * <p>A list of possible settings can be found at: <a
   * href="http://tinymce.moxiecode.com/wiki.php/Configuration">http://tinymce.moxiecode.com/wiki.php/Configuration</a>
   *
   * <p>The widget itself will also define a number of configuration settings and these may be
   * overridden using this method.
   */
  public void setConfigurationSetting(final String name, final Object value) {
    if ((value != null)) {
      this.configurationSettings_.put(name, value);
    } else {
      this.configurationSettings_.remove(name);
    }
  }
  /**
   * Returns a TinyMCE configuration setting&apos;s value.
   *
   * <p>An empty Any is returned when no value could be found for the provided argument.
   */
  public Object getConfigurationSetting(final String name) {
    Object it = this.configurationSettings_.get(name);
    if (it != null) {
      return it;
    } else {
      return null;
    }
  }
  /**
   * Sets the placeholder text.
   *
   * <p>This method is not supported on {@link WTextEdit} and will thrown an exception instead.
   */
  public void setPlaceholderText(final CharSequence placeholder) {
    throw new WException("WTextEdit::setPlaceholderText() is not implemented.");
  }

  public void setReadOnly(boolean readOnly) {
    super.setReadOnly(readOnly);
    if (readOnly) {
      this.setConfigurationSetting("readonly", "1");
    } else {
      this.setConfigurationSetting("readonly", null);
    }
  }

  public void propagateSetEnabled(boolean enabled) {
    super.propagateSetEnabled(enabled);
    this.setReadOnly(!enabled);
  }

  public void resize(final WLength width, final WLength height) {
    super.resize(width, height);
  }
  /**
   * Signal emitted when rendered.
   *
   * <p>A text edit is instantiated asynchronously as it depends on additional JavaScript libraries
   * and initialization. This signal is emitted when the component is initialized. The underlying
   * TinyMCE editor component is accessible as {@link WWidget#getJsRef()} + &quot;.ed&quot;.
   */
  public JSignal rendered() {
    return this.onRender_;
  }

  String renderRemoveJs(boolean recursive) {
    if (this.isRendered()) {
      String result = this.getJsRef() + ".ed.remove();";
      if (!recursive) {
        result += "Wt4.11.2.remove('" + this.getId() + "');";
      }
      return result;
    } else {
      return super.renderRemoveJs(recursive);
    }
  }

  void updateDom(final DomElement element, boolean all) {
    super.updateDom(element, all);
    if (element.getType() == DomElementType.TEXTAREA) {
      element.removeProperty(Property.StyleDisplay);
    }
    if (all && element.getType() == DomElementType.TEXTAREA) {
      StringWriter config = new StringWriter();
      config.append("{");
      boolean first = true;
      for (Iterator<Map.Entry<String, Object>> it_it =
              this.configurationSettings_.entrySet().iterator();
          it_it.hasNext(); ) {
        Map.Entry<String, Object> it = it_it.next();
        if (it.getKey().equals("plugins")) {
          continue;
        }
        if (!first) {
          config.append(',');
        }
        first = false;
        config
            .append(it.getKey())
            .append(": ")
            .append(StringUtils.asJSLiteral(it.getValue(), TextFormat.UnsafeXHTML));
      }
      if (!first) {
        config.append(',');
      }
      config.append("plugins: '").append(this.getPlugins()).append("'");
      config.append(",init_instance_callback: obj.init}");
      DomElement dummy = new DomElement(DomElement.Mode.Update, DomElementType.TABLE);
      this.updateDom(dummy, true);
      element.callJavaScript(
          "(function() { var obj = "
              + this.getJsRef()
              + ".wtObj;obj.render("
              + config.toString()
              + ","
              + jsStringLiteral(dummy.getCssStyle())
              + ","
              + (this.changed().isConnected() ? "true" : "false")
              + ");})();");
      this.contentChanged_ = false;
    }
    if (!all && this.contentChanged_) {
      element.callJavaScript(this.getJsRef() + ".ed.load();");
      this.contentChanged_ = false;
    }
  }

  protected void getDomChanges(final List<DomElement> result, WApplication app) {
    DomElement e = DomElement.getForUpdate(this.getFormName(), DomElementType.TABLE);
    this.updateDom(e, false);
    super.getDomChanges(result, app);
    result.add(e);
  }

  boolean domCanBeSaved() {
    return false;
  }

  protected int boxPadding(Orientation orientation) {
    return 0;
  }

  protected int boxBorder(Orientation orientation) {
    return 0;
  }

  private JSignal onChange_;
  private JSignal onRender_;
  private int version_;
  private boolean contentChanged_;
  private Map<String, Object> configurationSettings_;

  private String getPlugins() {
    String plugins = this.getExtraPlugins();
    if (this.version_ == 3) {
      if (plugins.length() != 0) {
        plugins += ",";
      }
      plugins += "safari";
    }
    return plugins;
  }

  private void init() {
    WApplication app = WApplication.getInstance();
    this.setInline(false);
    initTinyMCE();
    this.version_ = getTinyMCEVersion();
    this.setJavaScriptMember(
        " WTextEdit",
        "new Wt4.11.2.WTextEdit(" + app.getJavaScriptClass() + "," + this.getJsRef() + ");");
    this.setJavaScriptMember(
        WT_RESIZE_JS,
        "function(e, w, h, s) { var obj = "
            + this.getJsRef()
            + ".wtObj; obj.wtResize(e, w, h, s); };");
    String direction = app.getLayoutDirection() == LayoutDirection.LeftToRight ? "ltr" : "rtl";
    this.setConfigurationSetting("directionality", direction);
    String toolbar = "";
    if (this.version_ < 4) {
      toolbar =
          "fontselect,|,bold,italic,underline,|,fontsizeselect,|,forecolor,backcolor,|,justifyleft,justifycenter,justifyright,justifyfull,|,anchor,|,numlist,bullist";
    } else {
      toolbar = "undo redo | styleselect | bold italic | link";
    }
    this.setToolBar(0, toolbar);
    for (int i = 1; i <= 3; i++) {
      this.setToolBar(i, "");
    }
    this.setConfigurationSetting("doctype", WApplication.getInstance().getDocType());
    this.setConfigurationSetting("relative_urls", true);
    if (this.version_ < 4) {
      this.setConfigurationSetting("button_tile_map", true);
      this.setConfigurationSetting("theme", "advanced");
      this.setConfigurationSetting("theme_advanced_toolbar_location", "top");
      this.setConfigurationSetting("theme_advanced_toolbar_align", "left");
    }
    this.onChange_.addListener(
        this,
        () -> {
          WTextEdit.this.propagateOnChange();
        });
  }

  private void propagateOnChange() {
    this.changed().trigger();
  }

  private static void initTinyMCE() {
    String THIS_JS = "js/WTextEdit.js";
    WApplication app = WApplication.getInstance();
    if (!app.isJavaScriptLoaded(THIS_JS)) {
      if (app.getEnvironment().hasAjax()) {
        app.doJavaScript("window.tinyMCE_GZ = { loaded: true };", false);
      }
      String tinyMCEURL = "";
      tinyMCEURL = WApplication.readConfigurationProperty("tinyMCEURL", tinyMCEURL);
      if (tinyMCEURL.length() == 0) {
        int version = getTinyMCEVersion();
        String folder = "";
        String jsFile = "";
        if (version < 3) {
          folder = "tinymce/";
          jsFile = "tinymce.js";
        } else {
          if (version == 3) {
            folder = "tiny_mce/";
            jsFile = "tiny_mce.js";
          } else {
            folder = "tinymce/";
            jsFile = "tinymce.min.js";
          }
        }
        String tinyMCEBaseURL = WApplication.getRelativeResourcesUrl() + folder;
        tinyMCEBaseURL = WApplication.readConfigurationProperty("tinyMCEBaseURL", tinyMCEBaseURL);
        if (tinyMCEBaseURL.length() != 0
            && tinyMCEBaseURL.charAt(tinyMCEBaseURL.length() - 1) != '/') {
          tinyMCEBaseURL += '/';
        }
        tinyMCEURL = tinyMCEBaseURL + jsFile;
      }
      app.require(tinyMCEURL, "window['tinyMCE']");
      app.getStyleSheet().addRule(".mceEditor", "display: block; position: absolute;");
      app.loadJavaScript(THIS_JS, wtjs1());
    }
  }

  private static int getTinyMCEVersion() {
    String version = "3";
    version = WApplication.readConfigurationProperty("tinyMCEVersion", version);
    return Integer.parseInt(version);
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WTextEdit",
        "(function(e,t){t.wtObj=this;let i,o,n=0;const s=this,d=e.WT;let r;tinymce.dom.Event.domLoaded||(tinymce.dom.Event.domLoaded=!0);tinyMCE.init({mode:\"none\"});this.render=function(i,o,n){r=o;t.ed=new tinymce.Editor(t.id,i,tinymce.EditorManager);t.ed.render();n&&(tinymce.EditorManager.majorVersion<4?t.ed.onChange.add((function(){e.emit(t,\"change\")})):t.ed.on(\"change\",(function(){e.emit(t,\"change\")})));setTimeout((function(){e.emit(t,\"render\")}),0)};this.init=function(){const e=d.getElement(t.id+\"_ifr\");let n,a;if(tinymce.EditorManager.majorVersion<4){const t=e.parentNode.parentNode.parentNode.parentNode;a=t;n=t.parentNode}else{n=e.parentNode.parentNode.parentNode}if(a){a.style.cssText=\"width:100%;\"+r;t.style.height=a.offsetHeight+\"px\"}n.wtResize=t.wtResize;d.isGecko?setTimeout((function(){s.wtResize(t,i,o,!0)}),100):s.wtResize(t,i,o,!0);e.contentDocument.body.addEventListener(\"paste\",(function(e){const i=e.clipboardData||e.originalEvent.clipboardData;function o(e){return 0===e.indexOf(\"image/\")}if(i&&i.types)for(let n=0,s=i.types.length;n<s;++n){const s=i.types[n];if(o(s)||o(s.type)){const o=i.items[n].getAsFile(),s=new FileReader;s.onload=function(e){t.ed.insertContent('<img src=\"'+this.result+'\"></img>')};s.readAsDataURL(o);d.cancelEvent(e)}}}))};this.wtResize=function(r,a,l,p){if(l<0)return;const c=d.getElement(r.id+\"_ifr\");if(c){let p,f,h=0;h=d.px(r,\"marginTop\")+d.px(r,\"marginBottom\");d.boxSizing(r)||(h+=d.px(r,\"borderTopWidth\")+d.px(r,\"borderBottomWidth\")+d.px(r,\"paddingTop\")+d.px(r,\"paddingBottom\"));r.style.height=l-h+\"px\";const y=\"absolute\"!==t.style.position;if(tinymce.EditorManager.majorVersion<4){const e=c.parentNode.parentNode,t=e.parentNode.parentNode;f=t;p=t.parentNode;y||void 0===a||(p.style.width=a-2+\"px\");for(let i=0,o=t.rows.length;i<o;i++)t.rows[i]!==e&&(l-=t.rows[i].offsetHeight)}else{const e=c.parentNode,t=e.parentNode;p=t.parentNode;y||void 0===a||(p.style.width=a-2+\"px\");for(let i=0,o=t.childNodes.length;i<o;i++)t.childNodes[i]!==e&&(l-=t.childNodes[i].offsetHeight+1);l-=1}if(l<0){if(n<10){const e=100*Math.pow(2,n);setTimeout((function(){s.wtResize(t,i,o,!0)}),e)}n+=1;return}l+=\"px\";if(y){p.style.position=\"static\";p.style.display=\"block\"}else{p.style.position=r.style.position;p.style.left=r.style.left;p.style.top=r.style.top;!y&&f&&(f.style.width=a+\"px\");if(f){f.style.height=l+\"px\";p.style.height=r.style.height}}if(c.style.height!==l){n=0;c.style.height=l;e.layouts2&&e.layouts2.setElementDirty(t)}}else{i=a;o=l}};o=t.offsetHeight;i=t.offsetWidth})");
  }
}
