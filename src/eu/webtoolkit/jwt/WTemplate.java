/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A widget that renders an XHTML template
 * <p>
 * 
 * The XHTML template may contain references to variables which replaced by
 * strings are widgets.
 * <p>
 * Since the template text may be supplied by a {@link WString}, you can
 * conveniently store the string in a message resource bundle, and make it
 * localized by using {@link WString#tr(String key) WString#tr()}.
 * <p>
 * The template may contain variable references using a
 * <code>${<i>varName</i>}</code> syntax to reference the variable
 * &quot;&lt;tt&gt;varName&lt;/tt&gt;&quot;. To use a literal &apos;${&apos;,
 * use &quot;$${&quot;.
 * <p>
 * Usage example: <blockquote>
 * 
 * <pre>
 * WString userName = ...;
 * 
 *  WTemplate *t = new WTemplate();
 *  t.setTemplateText(&quot;&lt;div&gt; How old are you, ${friend} ? ${age-input} &lt;/div&gt;&quot;);
 *  t.bindString(&quot;friend&quot;, userName);
 *  t.bindWidget(&quot;age-input&quot;, ageEdit_ = new WLineEdit());
 * </pre>
 * 
 * </blockquote>
 */
public class WTemplate extends WInteractWidget {
	/**
	 * Creates a template widget.
	 */
	public WTemplate(WContainerWidget parent) {
		super(parent);
		this.widgets_ = new HashMap<String, WWidget>();
		this.strings_ = new HashMap<String, String>();
		this.text_ = new WString();
		this.changed_ = false;
		this.setInline(false);
	}

	/**
	 * Creates a template widget.
	 * <p>
	 * Calls {@link #WTemplate(WContainerWidget parent)
	 * this((WContainerWidget)null)}
	 */
	public WTemplate() {
		this((WContainerWidget) null);
	}

	/**
	 * Creates a template widget with given template.
	 * <p>
	 * The <code>templateText</code> must be proper XHTML, and this is checked
	 * unless the XHTML is resolved from a message resource bundle. This
	 * behavior is similar to a {@link WText} when configured with the
	 * {@link TextFormat#XHTMLText} textformat.
	 */
	public WTemplate(CharSequence text, WContainerWidget parent) {
		super(parent);
		this.widgets_ = new HashMap<String, WWidget>();
		this.strings_ = new HashMap<String, String>();
		this.text_ = new WString();
		this.changed_ = false;
		this.setInline(false);
		this.setTemplateText(text);
	}

	/**
	 * Creates a template widget with given template.
	 * <p>
	 * Calls {@link #WTemplate(CharSequence text, WContainerWidget parent)
	 * this(text, (WContainerWidget)null)}
	 */
	public WTemplate(CharSequence text) {
		this(text, (WContainerWidget) null);
	}

	/**
	 * Returns the template.
	 * <p>
	 * 
	 * @see WTemplate#setTemplateText(CharSequence text)
	 */
	public WString getTemplateText() {
		return this.text_;
	}

	/**
	 * Sets the template text.
	 * <p>
	 * The <code>text</code> must be proper XHTML, and this is checked unless
	 * the XHTML is resolved from a message resource bundle. This behavior is
	 * similar to a {@link WText} when configured with the
	 * {@link TextFormat#XHTMLText} textformat.
	 * <p>
	 * Changing the template text does not {@link WTemplate#clear() clear()}
	 * bound widgets or values.
	 * <p>
	 * 
	 * @see WTemplate#clear()
	 */
	public void setTemplateText(CharSequence text) {
		this.text_ = WString.toWString(text);
		if (this.text_.isLiteral()) {
			if (!removeScript(this.text_)) {
				this.text_ = escapeText(this.text_, true);
			}
		}
		this.changed_ = true;
		this.repaint(EnumSet.of(RepaintFlag.RepaintInnerHtml));
	}

	/**
	 * Binds a widget to a variable name.
	 * <p>
	 * The corresponding variable reference within the template will be replaced
	 * with the widget (rendered as XHTML). Since a single widget may be
	 * instantiated only once in a template, the variable <code>varName</code>
	 * may occur at most once in the template.
	 * <p>
	 * 
	 * @see WTemplate#bindString(String varName, CharSequence value, TextFormat
	 *      textFormat)
	 * @see WTemplate#resolveWidget(String varName)
	 */
	public void bindWidget(String varName, WWidget widget) {
		WWidget i = this.widgets_.get(varName);
		if (i != null) {
			if (i != null)
				i.remove();
		}
		if (widget != null) {
			widget.setParent(this);
		}
		this.widgets_.put(varName, widget);
		this.changed_ = true;
		this.repaint(EnumSet.of(RepaintFlag.RepaintInnerHtml));
	}

	/**
	 * Binds a string value to a variable name.
	 * <p>
	 * Each occurrence of the variable within the template will be substituted
	 * by its value.
	 * <p>
	 * Depending on the <code>textFormat</code>, the <code>value</code> is
	 * validated according as for a {@link WText}.
	 * <p>
	 * 
	 * @see WTemplate#bindWidget(String varName, WWidget widget)
	 * @see WTemplate#bindInt(String varName, int value)
	 */
	public void bindString(String varName, CharSequence value,
			TextFormat textFormat) {
		WString v = WString.toWString(value);
		if (textFormat == TextFormat.XHTMLText && this.text_.isLiteral()) {
			if (!removeScript(v)) {
				v = escapeText(v, true);
			}
		} else {
			if (textFormat == TextFormat.PlainText) {
				v = escapeText(v, true);
			}
		}
		this.strings_.put(varName, v.toString());
	}

	/**
	 * Binds a string value to a variable name.
	 * <p>
	 * Calls
	 * {@link #bindString(String varName, CharSequence value, TextFormat textFormat)
	 * bindString(varName, value, TextFormat.XHTMLText)}
	 */
	public final void bindString(String varName, CharSequence value) {
		bindString(varName, value, TextFormat.XHTMLText);
	}

	/**
	 * Binds an integer value to a variable name.
	 * <p>
	 * 
	 * @see WTemplate#bindString(String varName, CharSequence value, TextFormat
	 *      textFormat)
	 */
	public void bindInt(String varName, int value) {
		this.strings_.put(varName, String.valueOf(value));
	}

	/**
	 * Resolves the string value for a variable name.
	 * <p>
	 * This is the main method used to resolve variables in the template text,
	 * during rendering.
	 * <p>
	 * The default implementation considers first whether a string was bound
	 * using
	 * {@link WTemplate#bindString(String varName, CharSequence value, TextFormat textFormat)
	 * bindString()}. If so, that string is returned. If not, it will attempt to
	 * resolve a widget with that variable name using
	 * {@link WTemplate#resolveWidget(String varName) resolveWidget()}, and
	 * render it as XHTML. If that fails too,
	 * {@link WTemplate#handleUnresolvedVariable(String varName, List args, Writer result)
	 * handleUnresolvedVariable()} is called, passing the initial arguments.
	 * <p>
	 * You may want to reimplement this method to provide on-demand loading of
	 * strings for your template.
	 * <p>
	 * The result stream expects a UTF-8 encoded string value.
	 * <p>
	 * <p>
	 * <i><b>Warning:</b>When specializing this class, you need to make sure
	 * that you append proper XHTML to the <code>result</code>, without unsafe
	 * active contents. The
	 * {@link WTemplate#format(Writer result, String s, TextFormat textFormat)
	 * format()} methods may be used for this purpose.</i>
	 * </p>
	 * 
	 * @see WTemplate#renderTemplate(Writer result)
	 */
	public void resolveString(String varName, List<WString> args, Writer result)
			throws IOException {
		String i = this.strings_.get(varName);
		if (i != null) {
			result.append(i);
		} else {
			WWidget w = this.resolveWidget(varName);
			if (w != null) {
				w.htmlText(result);
			} else {
				this.handleUnresolvedVariable(varName, args, result);
			}
		}
	}

	/**
	 * Handles a variable that could not be resolved.
	 * <p>
	 * This method is called from
	 * {@link WTemplate#resolveString(String varName, List args, Writer result)
	 * resolveString()} for variables that could not be resolved.
	 * <p>
	 * The default implementation implementation writes &quot;??&quot; + varName
	 * + &quot;??&quot; to the result stream.
	 * <p>
	 * The result stream expects a UTF-8 encoded string value.
	 * <p>
	 * <p>
	 * <i><b>Warning:</b>When specializing this class, you need to make sure
	 * that you append proper XHTML to the <code>result</code>, without unsafe
	 * active contents. The
	 * {@link WTemplate#format(Writer result, String s, TextFormat textFormat)
	 * format()} methods may be used for this purpose.</i>
	 * </p>
	 * 
	 * @see WTemplate#resolveString(String varName, List args, Writer result)
	 */
	public void handleUnresolvedVariable(String varName, List<WString> args,
			Writer result) throws IOException {
		result.append("??").append(varName).append("??");
	}

	/**
	 * Resolves a widget for a variable name.
	 * <p>
	 * The default implementation returns a widget that was bound using
	 * {@link WTemplate#bindWidget(String varName, WWidget widget) bindWidget()}.
	 * <p>
	 * You may want to reimplement this method to create widgets on-demand.
	 */
	public WWidget resolveWidget(String varName) {
		WWidget j = this.widgets_.get(varName);
		if (j != null) {
			return j;
		} else {
			return null;
		}
	}

	// public T resolve(String varName) ;
	/**
	 * Erases all variable bindings.
	 * <p>
	 * Removes all strings and deletes all widgets that were previously bound
	 * using
	 * {@link WTemplate#bindString(String varName, CharSequence value, TextFormat textFormat)
	 * bindString()} and
	 * {@link WTemplate#bindWidget(String varName, WWidget widget) bindWidget()}.
	 */
	public void clear() {
		this.setIgnoreChildRemoves(true);
		for (Iterator<Map.Entry<String, WWidget>> i_it = this.widgets_
				.entrySet().iterator(); i_it.hasNext();) {
			Map.Entry<String, WWidget> i = i_it.next();
			if (i.getValue() != null)
				i.getValue().remove();
		}
		this.setIgnoreChildRemoves(false);
		this.widgets_.clear();
		this.strings_.clear();
	}

	/**
	 * Refreshes the template.
	 * <p>
	 * This rerenders the template.
	 */
	public void refresh() {
		if (this.text_.refresh()) {
			this.changed_ = true;
			this.repaint(EnumSet.of(RepaintFlag.RepaintInnerHtml));
		}
		super.refresh();
	}

	/**
	 * Renders the template into the given result stream.
	 * <p>
	 * The default implementation will parse the template, and resolve variables
	 * by calling
	 * {@link WTemplate#resolveString(String varName, List args, Writer result)
	 * resolveString()}.
	 * <p>
	 * You may want to reimplement this method to manage resources that are
	 * needed to load content on-demand (e.g. database objects), or support a
	 * custom template language.
	 */
	protected void renderTemplate(Writer result) throws IOException {
		String text = this.text_.toString();
		int lastPos = 0;
		for (int pos = text.indexOf('$'); pos != -1; pos = text.indexOf('$',
				pos)) {
			result.append(text.substring(lastPos, lastPos + pos - lastPos));
			lastPos = pos;
			if (pos + 1 < text.length()) {
				if (text.charAt(pos + 1) == '$') {
					result.append('$');
					lastPos += 2;
				} else {
					if (text.charAt(pos + 1) == '{') {
						int startName = pos + 2;
						int endName = text.indexOf(" \r\n\t}", startName);
						int endVar = text.indexOf('}', endName);
						if (endName == -1 || endVar == -1) {
							throw new RuntimeException(
									"WTemplate syntax error at pos "
											+ String.valueOf(pos));
						}
						String name = text.substring(startName, startName
								+ endName - startName);
						List<WString> args = new ArrayList<WString>();
						this.resolveString(name, args, result);
						lastPos = endVar + 1;
					} else {
						result.append('$');
						lastPos += 1;
					}
				}
			} else {
				result.append('$');
				lastPos += 1;
			}
			pos = lastPos;
		}
		result.append(text.substring(lastPos));
	}

	void updateDom(DomElement element, boolean all) {
		try {
			if (this.changed_ || all) {
				StringWriter resolved = new StringWriter();
				this.renderTemplate(resolved);
				element.setProperty(Property.PropertyInnerHTML, resolved
						.toString());
				this.changed_ = false;
			}
			super.updateDom(element, all);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	DomElementType getDomElementType() {
		return this.isInline() ? DomElementType.DomElement_SPAN
				: DomElementType.DomElement_DIV;
	}

	void propagateRenderOk(boolean deep) {
		this.changed_ = false;
		super.propagateRenderOk(deep);
	}

	/**
	 * Utility method to safely format an XHTML string.
	 * <p>
	 * The string is formatted according to the indicated
	 * <code>textFormat</code>. It is recommended to use this method when
	 * specializing
	 * {@link WTemplate#resolveString(String varName, List args, Writer result)
	 * resolveString()} to avoid security risks.
	 */
	protected void format(Writer result, String s, TextFormat textFormat)
			throws IOException {
		this.format(result, new WString(s), textFormat);
	}

	/**
	 * Utility method to safely format an XHTML string.
	 * <p>
	 * Calls {@link #format(Writer result, String s, TextFormat textFormat)
	 * format(result, s, TextFormat.PlainText)}
	 */
	protected final void format(Writer result, String s) throws IOException {
		format(result, s, TextFormat.PlainText);
	}

	/**
	 * Utility method to safely format an XHTML string.
	 * <p>
	 * The string is formatted according to the indicated
	 * <code>textFormat</code>. It is recommended to use this method when
	 * specializing
	 * {@link WTemplate#resolveString(String varName, List args, Writer result)
	 * resolveString()} to avoid security risks.
	 */
	protected void format(Writer result, CharSequence s, TextFormat textFormat)
			throws IOException {
		WString v = WString.toWString(s);
		if (textFormat == TextFormat.XHTMLText) {
			if (!removeScript(v)) {
				v = escapeText(v, true);
			}
		} else {
			if (textFormat == TextFormat.PlainText) {
				v = escapeText(v, true);
			}
		}
		result.append(v.toString());
	}

	/**
	 * Utility method to safely format an XHTML string.
	 * <p>
	 * Calls {@link #format(Writer result, CharSequence s, TextFormat textFormat)
	 * format(result, s, TextFormat.PlainText)}
	 */
	protected final void format(Writer result, CharSequence s)
			throws IOException {
		format(result, s, TextFormat.PlainText);
	}

	private Map<String, WWidget> widgets_;
	private Map<String, String> strings_;
	private WString text_;
	private boolean changed_;
}