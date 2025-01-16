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
 * A widget that renders an XHTML template.
 *
 * <p>The XHTML template may contain references to variables which replaced by strings are widgets.
 *
 * <p>Since the template text may be supplied by a {@link WString}, you can conveniently store the
 * string in a message resource bundle, and make it localized by using {@link WString#tr(String key)
 * WString#tr()}.
 *
 * <p>Placeholders (for variables and functions) are delimited by: <code>${...}</code>. To use a
 * literal <code>&quot;${&quot;</code>, use <code>&quot;$${&quot;</code>. Place holder names can
 * contain &apos;_&apos;, &apos;-&apos;, &apos;.&apos; and alfanumeric characters.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * WString userName = ...;
 *
 * WTemplate t = new WTemplate();
 * t.setTemplateText("<div> How old are you, ${friend} ? ${age-input} </div>");
 *
 * t.bindString("friend", userName, TextFormat::Plain);
 * t.bindWidget("age-input", ageEdit_ = new WLineEdit());
 *
 * }</pre>
 *
 * <p>There are currently three syntactic constructs defined: variable place holders, functions and
 * conditional blocks.
 *
 * <p>
 *
 * <h3>A. Variable placeholders</h3>
 *
 * <p><code>${var}</code> defines a placeholder for the variable &quot;var&quot;, and gets replaced
 * with whatever is bound to that variable:
 *
 * <ul>
 *   <li>a widget, using {@link WTemplate#bindWidget(String varName, WWidget widget) bindWidget()}
 *   <li>a string value, using {@link WTemplate#bindString(String varName, CharSequence value,
 *       TextFormat textFormat) bindString()} or {@link WTemplate#bindInt(String varName, int value)
 *       bindInt()}
 *   <li>or in general, the result of {@link WTemplate#resolveString(String varName, List args,
 *       Writer result) resolveString()} and {@link WTemplate#resolveWidget(String varName)
 *       resolveWidget()} methods.
 * </ul>
 *
 * <p>Optionally, additional arguments can be specified using the following syntax:
 *
 * <p><code>${var arg1=&quot;A value&quot; arg2=&apos;A second value&apos;}</code>
 *
 * <p>The arguments can thus be simple strings or quoted strings (single or double quoted). These
 * arguments are applied to a resolved widget in {@link WTemplate#applyArguments(WWidget w, List
 * args) applyArguments()} and currently supports only style classes.
 *
 * <p>You can bind widgets and values to variables using {@link WTemplate#bindWidget(String varName,
 * WWidget widget) bindWidget()}, {@link WTemplate#bindString(String varName, CharSequence value,
 * TextFormat textFormat) bindString()} or {@link WTemplate#bindInt(String varName, int value)
 * bindInt()} or by reimplementing the {@link WTemplate#resolveString(String varName, List args,
 * Writer result) resolveString()} and {@link WTemplate#resolveWidget(String varName)
 * resolveWidget()} methods.
 *
 * <p>
 *
 * <p><i><b>Note: </b>The use of XML comments (<code>&lt;!-- ... -.</code>) around variables that
 * are bound to widgets will result in bad behaviour since the template parser is ignorant about
 * these comments and the corresponding widgets will believe that they are rendered but aren&apos;t
 * actually. </i>
 *
 * <h3>B. Functions</h3>
 *
 * <p><code>${fun:arg}</code> defines a placeholder for applying a function &quot;fun&quot; to an
 * argument &quot;arg&quot;.
 *
 * <p>Optionally, additional arguments can be specified as with a variable placeholder.
 *
 * <p>{@link Functions} are resolved by {@link WTemplate#resolveFunction(String name, List args,
 * Writer result) resolveFunction()}, and the default implementation considers functions bound with
 * {@link WTemplate#addFunction(String name, WTemplate.Function function) addFunction()}. There are
 * currently three functions that are generally useful:
 *
 * <ul>
 *   <li>: resolves a localized strings, this is convenient to create a language neutral template,
 *       which contains translated strings Functions::id : resolves the id of a bound widget, this
 *       is convenient to bind &lt;label&gt; elements to a form widget using its for attribute.
 *   <li>: recursively renders another string as macro block optional arguments substituted before
 *       processing template substitution. For example, the following template uses the
 *       &quot;tr&quot; function to translate the age-label using the &quot;age-label&quot;
 *       internationalized key. WTemplate t = new WTemplate(); t.setTemplateText(&quot;&lt;div&gt;
 *       ${tr:age-label} ${age-input} &lt;/div&gt;&quot;); t.addFunction(&quot;tr&quot;,
 *       WTemplate.Functions.tr); t.bindWidget(&quot;age-input&quot;, ageEdit = new WLineEdit()); C.
 *       Conditional blocks
 * </ul>
 *
 * <p><code>${&lt;cond&gt;}</code> starts a conditional block with a condition name
 * &quot;cond&quot;, and must be closed by a balanced <code>${&lt;/cond&gt;}</code>.
 *
 * <p>For example:
 *
 * <pre>{@code
 * WTemplate t = new WTemplate();
 * t.setTemplateText("<div> ${<if-register>} Register ... ${</if-register>}</div>");
 * t.setCondition("if-register", true);
 *
 * }</pre>
 *
 * <p>Conditions are set using {@link WTemplate#setCondition(String name, boolean value)
 * setCondition()}.
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>This widget does not provide styling, and can be styled using inline or external CSS as
 * appropriate.
 */
public class WTemplate extends WInteractWidget {
  private static Logger logger = LoggerFactory.getLogger(WTemplate.class);

  private boolean _tr(final List<WString> args, final Writer result) throws IOException {
    if (args.size() >= 1) {
      WString s = WString.tr(args.get(0).toString());
      for (int j = 1; j < args.size(); ++j) {
        s.arg(args.get(j));
      }
      result.append(s.toXhtml());
      return true;
    } else {
      logger.error(
          new StringWriter().append("Functions::tr(): expects at least one argument").toString());
      return false;
    }
  }

  private boolean _block(final List<WString> args, final Writer result) throws IOException {
    if (args.size() < 1) {
      return false;
    }
    WString tblock = WString.tr(args.get(0).toString());
    for (int i = 1; i < args.size(); ++i) {
      tblock.arg(args.get(i));
    }
    this.renderTemplateText(result, tblock);
    return true;
  }

  private boolean _while(final List<WString> args, final Writer result) throws IOException {
    if (args.size() < 2) {
      return false;
    }
    WString tblock = WString.tr(args.get(1).toString());
    for (int i = 2; i < args.size(); ++i) {
      tblock.arg(args.get(i));
    }
    while (this.conditionValue(args.get(0).toString())) {
      this.renderTemplateText(result, tblock);
    }
    return true;
  }

  private boolean _id(final List<WString> args, final Writer result) throws IOException {
    if (args.size() == 1) {
      WWidget w = this.resolveWidget(args.get(0).toString());
      if (w != null) {
        result.append(w.getId());
        return true;
      } else {
        return false;
      }
    } else {
      logger.error(
          new StringWriter().append("Functions::tr(): expects exactly one argument").toString());
      return false;
    }
  }
  /**
   * A function interface type.
   *
   * <p>
   *
   * @see WTemplate#addFunction(String name, WTemplate.Function function)
   * @see WTemplate.Functions#tr
   * @see WTemplate.Functions#id
   * @see WTemplate.Functions#block
   * @see WTemplate.Functions#while_f
   */
  public static interface Function {
    public boolean evaluate(WTemplate t, final List<WString> args, final Writer result);
  }

  static class TrFunction implements WTemplate.Function {
    private static Logger logger = LoggerFactory.getLogger(TrFunction.class);

    public boolean evaluate(WTemplate t, final List<WString> args, final Writer result) {
      try {
        return t._tr(args, result);
      } catch (IOException ioe) {
        return false;
      }
    }
  }

  static class BlockFunction implements WTemplate.Function {
    private static Logger logger = LoggerFactory.getLogger(BlockFunction.class);

    public boolean evaluate(WTemplate t, final List<WString> args, final Writer result) {
      try {
        return t._block(args, result);
      } catch (IOException ioe) {
        return false;
      }
    }
  }

  static class WhileFunction implements WTemplate.Function {
    private static Logger logger = LoggerFactory.getLogger(WhileFunction.class);

    public boolean evaluate(WTemplate t, final List<WString> args, final Writer result) {
      try {
        return t._while(args, result);
      } catch (IOException ioe) {
        return false;
      }
    }
  }

  static class IdFunction implements WTemplate.Function {
    private static Logger logger = LoggerFactory.getLogger(IdFunction.class);

    public boolean evaluate(WTemplate t, final List<WString> args, final Writer result) {
      try {
        return t._id(args, result);
      } catch (IOException ioe) {
        return false;
      }
    }
  }
  /**
   * A collection of predefined functions.
   *
   * <p>
   *
   * @see WTemplate#addFunction(String name, WTemplate.Function function)
   */
  public static class Functions {
    private static Logger logger = LoggerFactory.getLogger(Functions.class);

    /**
     * A function that resolves to a localized string.
     *
     * <p>For example, when bound to the function <code>&quot;tr&quot;</code>, template that
     * contains the placeholder
     *
     * <pre>{@code
     * ... ${tr:name} ...
     *
     * }</pre>
     *
     * will be resolved to the value of:
     *
     * <pre>{@code
     * WString::tr("name")
     *
     * }</pre>
     *
     * <p>
     *
     * @see WTemplate#addFunction(String name, WTemplate.Function function)
     */
    public static final WTemplate.Function tr = new WTemplate.TrFunction();
    /**
     * A function that renders a macro block.
     *
     * <p>The function will consider the first argument as the key for a localized string that is a
     * macro block, and additional arguments as positional parameters in that block.
     *
     * <p>For example, a template that contains:
     *
     * <pre>{@code
     * ...
     * ${block:form-field category}
     * ...
     *
     * }</pre>
     *
     * <p>would look-up the following message:
     *
     * <p>
     *
     * <pre>{@code
     * <message id="form-field">
     * <div class="control-group">
     * ${{1}-info}
     * </div>
     * </message>
     *
     * }</pre>
     *
     * <p>and render as:
     *
     * <p>
     *
     * <pre>{@code
     * ...
     * <div class="control-group">
     * ${category-info}
     * </div>
     * ...
     *
     * }</pre>
     */
    public static final WTemplate.Function block = new WTemplate.BlockFunction();
    /**
     * A function that renders a macro block as long as the given condition is true.
     *
     * <p>The function will consider the first argument as the condition, and the second argument as
     * the key for a localized string that is a macro block.
     *
     * <p>Just like the {@link WTemplate.Functions#block block} function, you can provide additional
     * arguments, so the third argument will be what is filled in into <code>{1}</code> in the macro
     * block, etc.
     */
    public static final WTemplate.Function while_f = new WTemplate.WhileFunction();
    /**
     * A function that resolves the id of a bound widget.
     *
     * <p>For example, when bound to the function <code>&quot;id&quot;</code>, template text that
     * contains a place-holder
     *
     * <pre>{@code
     * ... ${id:name} ...
     *
     * }</pre>
     *
     * <p>will be resolved to the value of:
     *
     * <pre>{@code
     * t.resolveWidget("name").id()
     *
     * }</pre>
     *
     * <p>This is useful for binding labels to input elements.
     *
     * <p>
     *
     * @see WTemplate#addFunction(String name, WTemplate.Function function)
     */
    public static final WTemplate.Function id = new WTemplate.IdFunction();
  }
  /** Creates a template widget. */
  public WTemplate(WContainerWidget parentContainer) {
    super();
    this.previouslyRendered_ = null;
    this.newlyRendered_ = null;
    this.functions_ = new HashMap<String, WTemplate.Function>();
    this.strings_ = new HashMap<String, WString>();
    this.widgets_ = new HashMap<String, WWidget>();
    this.conditions_ = new HashSet<String>();
    this.text_ = new WString();
    this.errorText_ = "";
    this.encodeInternalPaths_ = false;
    this.encodeTemplateText_ = true;
    this.changed_ = false;
    this.widgetIdMode_ = TemplateWidgetIdMode.None;
    this.plainTextNewLineEscStream_ = new EscapeOStream();
    this.plainTextNewLineEscStream_.pushEscape(EscapeOStream.RuleSet.PlainTextNewLines);
    this.setInline(false);
    this.setTemplateText(WString.Empty);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a template widget.
   *
   * <p>Calls {@link #WTemplate(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WTemplate() {
    this((WContainerWidget) null);
  }
  /**
   * Creates a template widget with given template.
   *
   * <p>The <code>templateText</code> must be proper XHTML, and this is checked unless the XHTML is
   * resolved from a message resource bundle. This behavior is similar to a {@link WText} when
   * configured with the {@link TextFormat#XHTML} textformat.
   */
  public WTemplate(final CharSequence text, WContainerWidget parentContainer) {
    super();
    this.previouslyRendered_ = null;
    this.newlyRendered_ = null;
    this.functions_ = new HashMap<String, WTemplate.Function>();
    this.strings_ = new HashMap<String, WString>();
    this.widgets_ = new HashMap<String, WWidget>();
    this.conditions_ = new HashSet<String>();
    this.text_ = new WString();
    this.errorText_ = "";
    this.encodeInternalPaths_ = false;
    this.encodeTemplateText_ = true;
    this.changed_ = false;
    this.widgetIdMode_ = TemplateWidgetIdMode.None;
    this.plainTextNewLineEscStream_ = new EscapeOStream();
    this.plainTextNewLineEscStream_.pushEscape(EscapeOStream.RuleSet.PlainTextNewLines);
    this.setInline(false);
    this.setTemplateText(text);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a template widget with given template.
   *
   * <p>Calls {@link #WTemplate(CharSequence text, WContainerWidget parentContainer) this(text,
   * (WContainerWidget)null)}
   */
  public WTemplate(final CharSequence text) {
    this(text, (WContainerWidget) null);
  }

  public void remove() {
    this.clear();

    super.remove();
  }
  /**
   * Returns the template.
   *
   * <p>
   *
   * @see WTemplate#setTemplateText(CharSequence text, TextFormat textFormat)
   */
  public WString getTemplateText() {
    return this.text_;
  }
  /**
   * Sets the template text.
   *
   * <p>The <code>text</code> must be proper XHTML, and this is checked unless the XHTML is resolved
   * from a message resource bundle or TextFormat is {@link TextFormat#UnsafeXHTML}. This behavior
   * is similar to a {@link WText} when configured with the {@link TextFormat#XHTML} textformat.
   *
   * <p>Changing the template text does not {@link WTemplate#clear() clear()} bound widgets or
   * values.
   *
   * <p>
   *
   * @see WTemplate#clear()
   */
  public void setTemplateText(final CharSequence text, TextFormat textFormat) {
    this.text_ = WString.toWString(text);
    if (textFormat == TextFormat.XHTML && this.text_.isLiteral()) {
      if (!removeScript(this.text_)) {
        this.text_ = escapeText(this.text_, true);
      }
    } else {
      if (textFormat == TextFormat.Plain) {
        this.text_ = escapeText(this.text_, true);
      }
    }
    this.changed_ = true;
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }
  /**
   * Sets the template text.
   *
   * <p>Calls {@link #setTemplateText(CharSequence text, TextFormat textFormat)
   * setTemplateText(text, TextFormat.XHTML)}
   */
  public final void setTemplateText(final CharSequence text) {
    setTemplateText(text, TextFormat.XHTML);
  }
  /**
   * Sets how the varName should be reflected on bound widgets.
   *
   * <p>To easily identify a widget in the browser, it may be convenient to reflect the varName,
   * either through the object name (recommended) or the widget&apos;s ID.
   *
   * <p>The default value is {@link TemplateWidgetIdMode#None} which does not reflect the varName on
   * the bound widget.
   */
  public void setWidgetIdMode(TemplateWidgetIdMode mode) {
    this.widgetIdMode_ = mode;
  }
  /**
   * Returns how the varName is reflected on a bound widget.
   *
   * <p>
   *
   * @see WTemplate#setWidgetIdMode(TemplateWidgetIdMode mode)
   */
  public TemplateWidgetIdMode getWidgetIdMode() {
    return this.widgetIdMode_;
  }
  /**
   * Binds a string value to a variable.
   *
   * <p>Each occurrence of the variable within the template will be substituted by its value.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Depending on the <code>textFormat</code>, the <code>value</code> is
   * validated according as for a {@link WText}. The default ({@link TextFormat#XHTML}) filters
   * &quot;active&quot; content, to avoid XSS-based security risks. </i>
   *
   * @see WTemplate#bindWidget(String varName, WWidget widget)
   * @see WTemplate#bindInt(String varName, int value)
   */
  public void bindString(final String varName, final CharSequence value, TextFormat textFormat) {
    WWidget w = this.resolveWidget(varName);
    if (w != null) {
      this.bindWidget(varName, (WWidget) null);
    }
    WString v = WString.toWString(value);
    if (textFormat == TextFormat.XHTML && v.isLiteral()) {
      if (!removeScript(v)) {
        v = escapeText(v, true);
      }
    } else {
      if (textFormat == TextFormat.Plain) {
        v = escapeText(v, true);
      }
    }
    WString i = this.strings_.get(varName);
    if (i == null || !(i.toString().equals(v.toString()))) {
      this.strings_.put(varName, v);
      this.changed_ = true;
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
  }
  /**
   * Binds a string value to a variable.
   *
   * <p>Calls {@link #bindString(String varName, CharSequence value, TextFormat textFormat)
   * bindString(varName, value, TextFormat.XHTML)}
   */
  public final void bindString(final String varName, final CharSequence value) {
    bindString(varName, value, TextFormat.XHTML);
  }
  /**
   * Binds an integer value to a variable.
   *
   * <p>
   *
   * @see WTemplate#bindString(String varName, CharSequence value, TextFormat textFormat)
   */
  public void bindInt(final String varName, int value) {
    this.bindString(varName, String.valueOf(value), TextFormat.UnsafeXHTML);
  }
  /**
   * Binds a widget to a variable.
   *
   * <p>The corresponding variable reference within the template will be replaced with the widget
   * (rendered as XHTML). Since a single widget may be instantiated only once in a template, the
   * variable <code>varName</code> may occur at most once in the template, and the <code>widget
   * </code> must not yet be bound to another variable.
   *
   * <p>The widget is reparented to the {@link WTemplate}, so that it is deleted when the {@link
   * WTemplate} is deleted.
   *
   * <p>If a widget was already bound to the variable, it is deleted first. If previously a string
   * or other value was bound to the variable, it is removed.
   *
   * <p>You may also pass a <code>null</code> <code>widget</code>, which will resolve to an empty
   * string.
   *
   * <p>
   *
   * @see WTemplate#bindString(String varName, CharSequence value, TextFormat textFormat)
   * @see WTemplate#resolveWidget(String varName)
   */
  public WWidget bindWidget(final String varName, WWidget widget) {
    boolean setNull = !(widget != null);
    WWidget widgetPtrCopy = widget;
    if (!setNull) {
      this.strings_.remove(varName);
      switch (this.widgetIdMode_) {
        case None:
          break;
        case SetObjectName:
          widget.setObjectName(varName);
          break;
        case SetId:
          widget.setId(varName);
      }
    } else {
      WString j = this.strings_.get(varName);
      if (j != null && (j.length() == 0)) {
        return widgetPtrCopy;
      }
      this.strings_.put(varName, new WString());
    }
    {
      WWidget toRemove = this.removeWidget(varName);
      if (toRemove != null) toRemove.remove();
    }

    {
      WWidget oldWidget = this.widgets_.get(varName);
      this.widgets_.put(varName, widget);
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.widgets_.get(varName));
        if (toRemove != null) toRemove.remove();
      }
    }
    this.changed_ = true;
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    return widgetPtrCopy;
  }
  // public Widget  bindWidget(final String varName, <Woow... some pseudoinstantiation type!>
  // widget) ;
  // public Widget  (final String varName) ;
  // public Widget  (final String varName, Arg1 arg1) ;
  // public Widget  (final String varName, Arg1 arg1, Arg2 arg2) ;
  // public Widget  (final String varName, Arg1 arg1, Arg2 arg2, Arg3 arg3) ;
  // public Widget  (final String varName, Arg1 arg1, Arg2 arg2, Arg3 arg3, Arg4 arg4) ;
  /**
   * Unbinds a widget by variable name.
   *
   * <p>This removes a previously bound widget and unbinds the corresponding variable, effectively
   * undoing the effect of {@link WTemplate#bindWidget(String varName, WWidget widget)
   * bindWidget()}.
   *
   * <p>If this template does not contain a widget for the given <code>varName</code>, <code>null
   * </code> is returned.
   */
  public WWidget removeWidget(final String varName) {
    WWidget result = null;
    WWidget i = this.widgets_.get(varName);
    if (i != null) {
      {
        WWidget oldWidget = i;
        i = null;
        result = this.manageWidget(oldWidget, i);
      }
      this.widgets_.remove(varName);
      this.changed_ = true;
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
    return result;
  }
  /**
   * Unbinds a widget by widget pointer.
   *
   * <p>This removes a previously bound widget and unbinds the corresponding variable, effectively
   * undoing the effect of {@link WTemplate#bindWidget(String varName, WWidget widget)
   * bindWidget()}.
   *
   * <p>If this template does not contain the given widget, <code>null</code> is returned.
   */
  public WWidget removeWidget(WWidget widget) {
    String k = CollectionUtils.keyForValue(this.widgets_, widget);
    if (k != null) {
      return this.removeWidget(k);
    } else {
      return null;
    }
  }
  /**
   * Binds an empty string to a variable.
   *
   * <p>If a widget was bound to the variable, it is deleted first.
   *
   * <p>
   *
   * @see WTemplate#bindString(String varName, CharSequence value, TextFormat textFormat)
   */
  public void bindEmpty(final String varName) {
    this.bindWidget(varName, (WWidget) null);
  }
  /**
   * Binds a function.
   *
   * <p>{@link Functions} are useful to automatically resolve placeholders.
   *
   * <p>The syntax for a function &apos;fun&apos; applied to a single argument &apos;bla&apos; is:
   *
   * <p><code>${fun:bla}</code>
   *
   * <p>There are three predefined functions, which can be bound using:
   *
   * <pre>{@code
   * WTemplate t = ...;
   * t.addFunction("id", WTemplate.Functions.id);
   * t.addFunction("tr", WTemplate.Functions.tr);
   * t.addFunction("block", WTemplate.Functions.block);
   *
   * }</pre>
   */
  public void addFunction(final String name, WTemplate.Function function) {
    this.functions_.put(name, function);
  }
  /**
   * Sets a condition.
   *
   * <p>This enables or disables the inclusion of a conditional block.
   *
   * <p>The default value of all conditions is <code>false</code>.
   */
  public void setCondition(final String name, boolean value) {
    if (this.conditionValue(name) != value) {
      if (value) {
        this.conditions_.add(name);
      } else {
        this.conditions_.remove(name);
      }
      this.changed_ = true;
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
  }
  /**
   * Returns a condition value.
   *
   * <p>
   *
   * @see WTemplate#setCondition(String name, boolean value)
   */
  public boolean conditionValue(final String name) {
    return this.conditions_.contains(name) != false;
  }
  /** Returns the set of conditions set to true. */
  public Set<String> getConditionsSet() {
    return this.conditions_;
  }
  /**
   * Resolves the string value for a variable name.
   *
   * <p>This is the main method used to resolve variables in the template text, during rendering.
   *
   * <p>The default implementation considers first whether a string was bound using {@link
   * WTemplate#bindString(String varName, CharSequence value, TextFormat textFormat) bindString()}.
   * If so, that string is returned. If not, it will attempt to resolve a widget with that variable
   * name using {@link WTemplate#resolveWidget(String varName) resolveWidget()}, and render it as
   * XHTML. If that fails too, {@link WTemplate#handleUnresolvedVariable(String varName, List args,
   * Writer result) handleUnresolvedVariable()} is called, passing the initial arguments.
   *
   * <p>You may want to reimplement this method to provide on-demand loading of strings for your
   * template.
   *
   * <p>The result stream expects a UTF-8 encoded string value.
   *
   * <p>
   *
   * <p><i><b>Warning: </b>When specializing this class, you need to make sure that you append
   * proper XHTML to the <code>result</code>, without unsafe active contents. The {@link
   * WTemplate#format(Writer result, String s, TextFormat textFormat) format()} methods may be used
   * for this purpose. </i>
   *
   * @see WTemplate#renderTemplate(Writer result)
   */
  public void resolveString(final String varName, final List<WString> args, final Writer result)
      throws IOException {
    WString i = this.strings_.get(varName);
    if (i != null) {
      result.append(i.toString());
    } else {
      WWidget w = this.resolveWidget(varName);
      if (w != null) {
        w.setParentWidget(this);
        if (this.previouslyRendered_ != null && this.previouslyRendered_.contains(w) != false) {
          result.append("<span id=\"").append(w.getId()).append("\"> </span>");
        } else {
          this.applyArguments(w, args);
          w.htmlText(result);
        }
        if (this.newlyRendered_ != null) {
          this.newlyRendered_.add(w);
        }
      } else {
        this.handleUnresolvedVariable(varName, args, result);
      }
    }
  }
  /**
   * Handles a variable that could not be resolved.
   *
   * <p>This method is called from {@link WTemplate#resolveString(String varName, List args, Writer
   * result) resolveString()} for variables that could not be resolved.
   *
   * <p>The default implementation implementation writes &quot;??&quot; + varName + &quot;??&quot;
   * to the result stream.
   *
   * <p>The result stream expects a UTF-8 encoded string value.
   *
   * <p>
   *
   * <p><i><b>Warning: </b>When specializing this class, you need to make sure that you append
   * proper XHTML to the <code>result</code>, without unsafe active contents. The {@link
   * WTemplate#format(Writer result, String s, TextFormat textFormat) format()} methods may be used
   * for this purpose. </i>
   *
   * @see WTemplate#resolveString(String varName, List args, Writer result)
   */
  public void handleUnresolvedVariable(
      final String varName, final List<WString> args, final Writer result) throws IOException {
    result.append("??").append(varName).append("??");
  }
  /**
   * Resolves a widget for a variable name.
   *
   * <p>The default implementation returns a widget that was bound using {@link
   * WTemplate#bindWidget(String varName, WWidget widget) bindWidget()}.
   *
   * <p>You may want to reimplement this method to create widgets on-demand. All widgets that are
   * returned by this method are reparented to the {@link WTemplate}, so they will be deleted when
   * the template is destroyed, but they are not deleted by {@link WTemplate#clear() clear()}
   * (unless bind was called on them as in the example below).
   *
   * <p>This method is typically used for delayed binding of widgets. Usage example:
   *
   * <pre>{@code
   * if (WWidget known = super.resolveWidget(varName)) {
   * return known;
   * } else {
   * if (varName == "age-input") {
   * WWidget w = new WLineEdit(); // widget only created when used
   * bindWidget(varName, w);
   * return w;
   * }
   * }
   *
   * }</pre>
   */
  public WWidget resolveWidget(final String varName) {
    WWidget j = this.widgets_.get(varName);
    if (j != null) {
      return j;
    } else {
      return null;
    }
  }
  /**
   * Resolves a string that was bound to a variable name.
   *
   * <p>Returns the string that was bound to the variable with {@link WTemplate#bindString(String
   * varName, CharSequence value, TextFormat textFormat) bindString()}.
   *
   * <p>This method is not to be confused with {@link WTemplate#resolveString(String varName, List
   * args, Writer result) resolveString()} which resolves any variable to a string (both strings and
   * widgets alike).
   */
  public WString resolveStringValue(final String varName) {
    WString i = this.strings_.get(varName);
    if (i != null) {
      return i;
    } else {
      return WString.Empty;
    }
  }

  public List<WWidget> getWidgets() {
    List<WWidget> result = new ArrayList<WWidget>();
    for (Iterator<Map.Entry<String, WWidget>> j_it = this.widgets_.entrySet().iterator();
        j_it.hasNext(); ) {
      Map.Entry<String, WWidget> j = j_it.next();
      result.add(j.getValue());
    }
    return result;
  }

  public String varName(WWidget w) {
    for (Iterator<Map.Entry<String, WWidget>> j_it = this.widgets_.entrySet().iterator();
        j_it.hasNext(); ) {
      Map.Entry<String, WWidget> j = j_it.next();
      if (j.getValue() == w) {
        return j.getKey();
      }
    }
    return "";
  }
  /**
   * Resolves a function call.
   *
   * <p>This resolves a function with name <code>name</code>, and one or more arguments <code>args
   * </code>, and writes the result into the stream <code>result</code>. The method returns whether
   * a function was matched and applied.
   *
   * <p>The default implementation considers functions that were bound using {@link
   * WTemplate#addFunction(String name, WTemplate.Function function) addFunction()}.
   *
   * <p>
   *
   * @see WTemplate#addFunction(String name, WTemplate.Function function)
   */
  public boolean resolveFunction(final String name, final List<WString> args, final Writer result)
      throws IOException {
    WTemplate.Function i = this.functions_.get(name);
    if (i != null) {
      boolean ok = i.evaluate(this, args, result);
      if (!ok) {
        result.append("??").append(name).append(":??");
      }
      return true;
    }
    return false;
  }
  // public T (final String varName) ;
  /**
   * Erases all variable bindings.
   *
   * <p>Removes all strings and deletes all widgets that were previously bound using {@link
   * WTemplate#bindString(String varName, CharSequence value, TextFormat textFormat) bindString()}
   * and {@link WTemplate#bindWidget(String varName, WWidget widget) bindWidget()}.
   *
   * <p>This also resets all conditions set using {@link WTemplate#setCondition(String name, boolean
   * value) setCondition()}, but does not remove functions added with {@link
   * WTemplate#addFunction(String name, WTemplate.Function function) addFunction()}
   */
  public void clear() {
    for (Iterator<Map.Entry<String, WWidget>> it_it = this.widgets_.entrySet().iterator();
        it_it.hasNext(); ) {
      Map.Entry<String, WWidget> it = it_it.next();
      WWidget w = it.getValue();
      if (w != null) {
        this.widgetRemoved(w, false);
      }
    }
    this.widgets_.clear();
    this.strings_.clear();
    this.conditions_.clear();
    this.changed_ = true;
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }
  /**
   * Enables internal path anchors in the XHTML template.
   *
   * <p>Anchors to internal paths are represented differently depending on the session
   * implementation (plain HTML, Ajax or HTML5 history). By enabling this option, anchors which
   * reference an internal path (by referring a URL of the form <code>href=&quot;#/...&quot;</code>
   * ), are re-encoded to link to the internal path.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>
   */
  public void setInternalPathEncoding(boolean enabled) {
    if (this.encodeInternalPaths_ != enabled) {
      this.encodeInternalPaths_ = enabled;
      this.changed_ = true;
      this.repaint();
    }
  }
  /**
   * Returns whether internal paths are enabled.
   *
   * <p>
   *
   * @see WTemplate#setInternalPathEncoding(boolean enabled)
   */
  public boolean hasInternalPathEncoding() {
    return this.encodeInternalPaths_;
  }
  /**
   * Configures when internal path encoding is done.
   *
   * <p>By default, the internal path encoding (if enabled) is done on the template text before
   * placeholders are being resolved. In some rare situations, you may want to postpone the internal
   * path encoding until after placeholders have been resolved, e.g. if a placeholder was used to
   * provide the string for an anchor href.
   *
   * <p>The default value is <code>true</code>
   */
  public void setEncodeTemplateText(boolean on) {
    this.encodeTemplateText_ = on;
  }
  /**
   * Returns whether internal path encoding is done on the template text.
   *
   * <p>
   *
   * @see WTemplate#setEncodeTemplateText(boolean on)
   */
  public boolean isEncodeTemplateText() {
    return this.encodeTemplateText_;
  }

  public void refresh() {
    if (this.text_.refresh() || !this.strings_.isEmpty()) {
      this.changed_ = true;
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
    super.refresh();
  }
  /**
   * Renders the template into the given result stream.
   *
   * <p>The default implementation will call {@link WTemplate#renderTemplateText(Writer result,
   * CharSequence templateText) renderTemplateText()} with the {@link WTemplate#getTemplateText()
   * getTemplateText()}.
   */
  public void renderTemplate(final Writer result) throws IOException {
    this.renderTemplateText(result, this.getTemplateText());
  }
  /**
   * Renders a template into the given result stream.
   *
   * <p>The default implementation will parse the template, and resolve variables by calling {@link
   * WTemplate#resolveString(String varName, List args, Writer result) resolveString()}.
   *
   * <p>You may want to reimplement this method to manage resources that are needed to load content
   * on-demand (e.g. database objects), or support a custom template language.
   *
   * <p>Return: true if rendered successfully. @see WTemplate#getErrorText()
   */
  public boolean renderTemplateText(final Writer result, final CharSequence templateText)
      throws IOException {
    this.errorText_ = "";
    String text = "";
    if (this.encodeTemplateText_) {
      text = this.encode(WString.toWString(templateText).toXhtml());
    } else {
      text = WString.toWString(templateText).toXhtml();
    }
    int lastPos = 0;
    List<WString> args = new ArrayList<WString>();
    List<String> conditions = new ArrayList<String>();
    int suppressing = 0;
    for (int pos = text.indexOf('$'); pos != -1; pos = text.indexOf('$', pos)) {
      if (!(suppressing != 0)) {
        result.append(text.substring(lastPos, lastPos + pos - lastPos));
      }
      lastPos = pos;
      if (pos + 1 < text.length()) {
        if (text.charAt(pos + 1) == '$') {
          if (!(suppressing != 0)) {
            result.append('$');
          }
          lastPos += 2;
        } else {
          if (text.charAt(pos + 1) == '{') {
            int startName = pos + 2;
            int endName = StringUtils.findFirstOf(text, " \r\n\t}", startName);
            args.clear();
            int endVar = parseArgs(text, endName, args);
            if (endVar == -1) {
              StringWriter errorStream = new StringWriter();
              errorStream
                  .append("variable syntax error near \"")
                  .append(text.substring(pos))
                  .append("\"");
              this.errorText_ = errorStream.toString();
              logger.error(new StringWriter().append(this.errorText_).toString());
              return false;
            }
            String name = text.substring(startName, startName + endName - startName);
            int nl = name.length();
            if (nl > 2 && name.charAt(0) == '<' && name.charAt(nl - 1) == '>') {
              if (name.charAt(1) != '/') {
                String cond = name.substring(1, 1 + nl - 2);
                conditions.add(cond);
                if (suppressing != 0 || !this.conditionValue(cond)) {
                  ++suppressing;
                }
              } else {
                String cond = name.substring(2, 2 + nl - 3);
                if (conditions.isEmpty() || !conditions.get(conditions.size() - 1).equals(cond)) {
                  StringWriter errorStream = new StringWriter();
                  errorStream.append("mismatching condition block end: ").append(cond);
                  this.errorText_ = errorStream.toString();
                  logger.error(new StringWriter().append(this.errorText_).toString());
                  return false;
                }
                conditions.remove(conditions.size() - 1);
                if (suppressing != 0) {
                  --suppressing;
                }
              }
            } else {
              if (!(suppressing != 0)) {
                int colonPos = name.indexOf(':');
                boolean handled = false;
                if (colonPos != -1) {
                  String fname = name.substring(0, 0 + colonPos);
                  String arg0 = name.substring(colonPos + 1);
                  args.add(0, new WString(arg0));
                  if (this.resolveFunction(fname, args, result)) {
                    handled = true;
                  } else {
                    args.remove(0);
                  }
                }
                if (!handled) {
                  this.resolveString(name, args, result);
                }
              }
            }
            lastPos = endVar + 1;
          } else {
            if (!(suppressing != 0)) {
              result.append('$');
            }
            lastPos += 1;
          }
        }
      } else {
        if (!(suppressing != 0)) {
          result.append('$');
        }
        lastPos += 1;
      }
      pos = lastPos;
    }
    result.append(text.substring(lastPos));
    return true;
  }
  /**
   * Renders the errors during renderring.
   *
   * <p>
   *
   * @see WTemplate#renderTemplateText(Writer result, CharSequence templateText)
   */
  public String getErrorText() {
    return this.errorText_;
  }
  /**
   * Applies arguments to a resolved widget.
   *
   * <p>Currently only a <code>class</code> argument is handled, which adds one or more style
   * classes to the widget <code>w</code>, using {@link WWidget#addStyleClass(String styleClass,
   * boolean force) WWidget#addStyleClass()}.
   */
  protected void applyArguments(WWidget w, final List<WString> args) {
    for (int i = 0; i < args.size(); ++i) {
      String s = args.get(i).toString();
      if (s.startsWith("class=")) {
        w.addStyleClass(new WString(s.substring(6)).toString());
      }
    }
  }

  void updateDom(final DomElement element, boolean all) {
    try {
      if (this.changed_ || all) {
        Set<WWidget> previouslyRendered = new HashSet<WWidget>();
        List<WWidget> newlyRendered = new ArrayList<WWidget>();
        for (Iterator<Map.Entry<String, WWidget>> i_it = this.widgets_.entrySet().iterator();
            i_it.hasNext(); ) {
          Map.Entry<String, WWidget> i = i_it.next();
          WWidget w = i.getValue();
          if (w != null && w.isRendered()) {
            if (w.getWebWidget().domCanBeSaved()) {
              previouslyRendered.add(w);
            } else {
              this.unrenderWidget(w, element);
            }
          }
        }
        boolean saveWidgets = element.getMode() == DomElement.Mode.Update;
        this.previouslyRendered_ = saveWidgets ? previouslyRendered : null;
        this.newlyRendered_ = newlyRendered;
        StringWriter html = new StringWriter();
        this.renderTemplate(html);
        this.previouslyRendered_ = null;
        this.newlyRendered_ = null;
        for (int i = 0; i < newlyRendered.size(); ++i) {
          WWidget w = newlyRendered.get(i);
          if (previouslyRendered.contains(w) != false) {
            if (saveWidgets) {
              element.saveChild(w.getId());
            }
            previouslyRendered.remove(w);
          }
        }
        if (this.encodeTemplateText_) {
          element.setProperty(Property.InnerHTML, html.toString());
        } else {
          element.setProperty(Property.InnerHTML, this.encode(html.toString()));
        }
        for (Iterator<WWidget> i_it = previouslyRendered.iterator(); i_it.hasNext(); ) {
          WWidget i = i_it.next();
          WWidget w = i;
          for (Iterator<Map.Entry<String, WWidget>> j_it = this.widgets_.entrySet().iterator();
              j_it.hasNext(); ) {
            Map.Entry<String, WWidget> j = j_it.next();
            if (j.getValue() == w) {
              this.unrenderWidget(w, element);
              break;
            }
          }
        }
        WApplication.getInstance().getSession().getRenderer().updateFormObjects(this, true);
        this.changed_ = false;
      }
      super.updateDom(element, all);
    } catch (IOException ioe) {
      logger.info("Ignoring exception {}", ioe.getMessage(), ioe);
    }
  }

  DomElementType getDomElementType() {
    DomElementType type = this.isInline() ? DomElementType.SPAN : DomElementType.DIV;
    WContainerWidget p = ObjectUtils.cast(this.getParentWebWidget(), WContainerWidget.class);
    if (p != null && p.isList()) {
      type = DomElementType.LI;
    }
    return type;
  }

  void propagateRenderOk(boolean deep) {
    this.changed_ = false;
    super.propagateRenderOk(deep);
  }

  protected void iterateChildren(final HandleWidgetMethod method) {
    for (Iterator<Map.Entry<String, WWidget>> it_it = this.widgets_.entrySet().iterator();
        it_it.hasNext(); ) {
      Map.Entry<String, WWidget> it = it_it.next();
      WWidget w = it.getValue();
      if (w != null) {
        method.handle(w);
      }
    }
  }
  /**
   * Utility method to safely format an XHTML string.
   *
   * <p>The string is formatted according to the indicated <code>textFormat</code>. It is
   * recommended to use this method when specializing {@link WTemplate#resolveString(String varName,
   * List args, Writer result) resolveString()} to avoid security risks.
   */
  protected void format(final Writer result, final String s, TextFormat textFormat)
      throws IOException {
    this.format(result, new WString(s), textFormat);
  }
  /**
   * Utility method to safely format an XHTML string.
   *
   * <p>Calls {@link #format(Writer result, String s, TextFormat textFormat) format(result, s,
   * TextFormat.Plain)}
   */
  protected final void format(final Writer result, final String s) throws IOException {
    format(result, s, TextFormat.Plain);
  }
  /**
   * Utility method to safely format an XHTML string.
   *
   * <p>The string is formatted according to the indicated <code>textFormat</code>. It is
   * recommended to use this method when specializing {@link WTemplate#resolveString(String varName,
   * List args, Writer result) resolveString()} to avoid security risks.
   */
  protected void format(final Writer result, final CharSequence s, TextFormat textFormat)
      throws IOException {
    if (textFormat == TextFormat.XHTML) {
      WString v = WString.toWString(s);
      if (removeScript(v)) {
        result.append(v.toString());
        return;
      } else {
        EscapeOStream sout = new EscapeOStream(result);
        sout.append(v.toString(), this.plainTextNewLineEscStream_);
        return;
      }
    } else {
      if (textFormat == TextFormat.Plain) {
        EscapeOStream sout = new EscapeOStream(result);
        sout.append(s.toString(), this.plainTextNewLineEscStream_);
        return;
      }
    }
    result.append(s.toString());
  }
  /**
   * Utility method to safely format an XHTML string.
   *
   * <p>Calls {@link #format(Writer result, CharSequence s, TextFormat textFormat) format(result, s,
   * TextFormat.Plain)}
   */
  protected final void format(final Writer result, final CharSequence s) throws IOException {
    format(result, s, TextFormat.Plain);
  }

  protected void enableAjax() {
    super.enableAjax();
  }
  /**
   * Notifies the template that it has changed and must be rerendered.
   *
   * <p>If you update a {@link WTemplate} with e.g bindWidget or setCondition, or change the
   * template text, the template will automatically be rerendered.
   *
   * <p>However, if you create a subclass of {@link WTemplate} and override resolveString or
   * resolveWidget, you will have to notify the {@link WTemplate} if it has changed with a call to
   * {@link WTemplate#reset() reset()}.
   */
  protected void reset() {
    this.changed_ = true;
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  private Set<WWidget> previouslyRendered_;
  private List<WWidget> newlyRendered_;
  private Map<String, WTemplate.Function> functions_;
  private Map<String, WString> strings_;
  private Map<String, WWidget> widgets_;
  private Set<String> conditions_;
  private WString text_;
  private String errorText_;
  private boolean encodeInternalPaths_;
  private boolean encodeTemplateText_;
  private boolean changed_;
  private TemplateWidgetIdMode widgetIdMode_;

  private String encode(final String text) {
    WApplication app = WApplication.getInstance();
    if (app != null && (this.encodeInternalPaths_ || app.getSession().hasSessionIdInUrl())) {
      EnumSet<RefEncoderOption> options = EnumSet.noneOf(RefEncoderOption.class);
      if (this.encodeInternalPaths_) {
        options.add(RefEncoderOption.EncodeInternalPaths);
      }
      if (app.getSession().hasSessionIdInUrl()) {
        options.add(RefEncoderOption.EncodeRedirectTrampoline);
      }
      return RefEncoder.EncodeRefs(new WString(text), options).toString();
    } else {
      return text;
    }
  }

  private static int parseArgs(final String text, int pos, final List<WString> result) {
    int Error = -1;
    if (pos == -1) {
      return Error;
    }
    final int Next = 0;
    final int Name = 1;
    final int Value = 2;
    final int SValue = 3;
    final int DValue = 4;
    int state = Next;
    StringBuilder v = new StringBuilder();
    for (; pos < text.length(); ++pos) {
      char c = text.charAt(pos);
      switch (state) {
        case Next:
          if (!Character.isWhitespace(c)) {
            if (c == '}') {
              return pos;
            } else {
              if (Character.isLetter(c) || c == '_') {
                state = Name;
                v.setLength(0);
                v.append(c);
              } else {
                if (c == '\'') {
                  state = SValue;
                  v.setLength(0);
                } else {
                  if (c == '"') {
                    state = DValue;
                    v.setLength(0);
                  } else {
                    return Error;
                  }
                }
              }
            }
          }
          break;
        case Name:
          if (c == '=') {
            state = Value;
            v.append('=');
          } else {
            if (Character.isWhitespace(c)) {
              result.add(new WString(v.toString()));
              state = Next;
            } else {
              if (c == '}') {
                result.add(new WString(v.toString()));
                return pos;
              } else {
                if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                  v.append(c);
                } else {
                  return Error;
                }
              }
            }
          }
          break;
        case Value:
          if (c == '\'') {
            state = SValue;
          } else {
            if (c == '"') {
              state = DValue;
            } else {
              return Error;
            }
          }
          break;
        case SValue:
        case DValue:
          char quote = state == SValue ? '\'' : '"';
          int end = text.indexOf(quote, pos);
          if (end == -1) {
            return Error;
          }
          if (text.charAt(end - 1) == '\\') {
            v.append(text.substring(pos, pos + end - pos - 1)).append(quote);
          } else {
            v.append(text.substring(pos, pos + end - pos));
            result.add(new WString(v.toString()));
            state = Next;
          }
          pos = end;
      }
    }
    return pos == text.length() ? -1 : pos;
  }

  private void unrenderWidget(WWidget w, final DomElement el) {
    String removeJs = w.renderRemoveJs(false);
    if (removeJs.charAt(0) == '_') {
      el.callJavaScript("Wt4.11.2.remove('" + removeJs.substring(1) + "');", true);
    } else {
      el.callJavaScript(removeJs, true);
    }
    w.getWebWidget().setRendered(false);
  }

  private EscapeOStream plainTextNewLineEscStream_;
}
