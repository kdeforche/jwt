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
 * A widget that renders (XHTML) text.
 *
 * <p>The text is provided through a {@link WString}, which may either hold a literal text, or a key
 * to localized text which is looked up in locale dependent XML files (see {@link WString#tr(String
 * key) WString#tr()}).
 *
 * <p>Use {@link WText#setTextFormat(TextFormat textFormat) setTextFormat()} to configure the
 * textFormat of the text. The default textFormat is {@link TextFormat#XHTML}, which allows XHMTL
 * markup to be included in the text. Tags and attributes that indicate &quot;active&quot; content
 * are not allowed and stripped out, to avoid security risks exposed by JavaScript such as the
 * common web-based <a href="http://en.wikipedia.org/wiki/Cross_site_scriptingCross-Site">Cross-Site
 * Scripting (XSS)</a> malicious attack. XSS is the situation where one user of your web application
 * is able to execute a script in another user&apos;s browser while your application only intended
 * to display a message entered by the mailicious user to the other user. To defeat this attack, JWt
 * assumes that content in a WText is intended to be passive, and not contain any scripting
 * elements.
 *
 * <p>The {@link TextFormat#XHTML} format will automatically change to {@link TextFormat#Plain} if
 * the text is not valid XML. Properly formatted HTML, which is not valid XHTML (e.g. a <code>
 * &lt;br&gt;</code> tag without closing tag) will thus be shown literally, since the HTML markup
 * will be escaped. JWt does this as a safety measure, since it cannot reliably run the XSS filter
 * without parsing the XML successfully.
 *
 * <p>The {@link TextFormat#Plain} format will display the text literally (escaping any HTML special
 * characters).
 *
 * <p>In some situations, {@link TextFormat#UnsafeXHTML} may be useful to explicitly allow scripting
 * content. Like {@link TextFormat#XHTML}, it allows XHTML markup, but it also allows potentially
 * dangerous tags and attributes. Use this if you&apos;re sure that a user cannot interfere with the
 * text set, and {@link TextFormat#XHTML} is too limiting.
 *
 * <p>WText is by default {@link WWidget#setInline(boolean inlined) inline}, unless the XHTML
 * contents starts with an element such as <code>&lt;div&gt;</code>, <code>&lt;h&gt;</code> or
 * <code>&lt;p&gt;</code> that is displayed as a block, in which case the widget will also display
 * as a block.
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>The widget corresponds to an HTML <code>&lt;span&gt;</code> tag or an HTML <code>&lt;div&gt;
 * </code> depending on whether the widget is inline.
 *
 * <p>This widget does not provide styling, and can be styled using inline or external CSS as
 * appropriate.
 *
 * <p>
 *
 * @see WApplication#setLocale(Locale locale, boolean doRefresh)
 */
public class WText extends WInteractWidget {
  private static Logger logger = LoggerFactory.getLogger(WText.class);

  /** Creates a text widget with an empty text. */
  public WText(WContainerWidget parentContainer) {
    super();
    this.text_ = new WText.RichText();
    this.flags_ = new BitSet();
    this.padding_ = null;
    this.flags_.set(BIT_WORD_WRAP);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a text widget with an empty text.
   *
   * <p>Calls {@link #WText(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WText() {
    this((WContainerWidget) null);
  }
  /**
   * Creates a text widget with given text.
   *
   * <p>The textFormat is set to {@link TextFormat#XHTML}, unless the <code>text</code> is literal
   * (not created using {@link WString#tr(String key) WString#tr()}) and it could not be parsed as
   * valid XML. In that case the textFormat is set to {@link TextFormat#Plain}.
   *
   * <p>Therefore, if you wish to use {@link TextFormat#XHTML}, but cannot be sure about <code>text
   * </code> being valid XML, you should verify that the {@link WText#getTextFormat()
   * getTextFormat()} is {@link TextFormat#XHTML} after construction.
   *
   * <p>The XML parser will silently discard malicious tags and attributes for literal {@link
   * TextFormat#XHTML} text.
   */
  public WText(final CharSequence text, WContainerWidget parentContainer) {
    super();
    this.text_ = new WText.RichText();
    this.flags_ = new BitSet();
    this.padding_ = null;
    this.flags_.set(BIT_WORD_WRAP);
    this.setText(text);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a text widget with given text.
   *
   * <p>Calls {@link #WText(CharSequence text, WContainerWidget parentContainer) this(text,
   * (WContainerWidget)null)}
   */
  public WText(final CharSequence text) {
    this(text, (WContainerWidget) null);
  }
  /**
   * Creates a text widget with given text and format.
   *
   * <p>If <i>textFormat</i> is {@link TextFormat#XHTML} and <code>text</code> is not literal (not
   * created using {@link WString#tr(String key) WString#tr()}), then if the <code>text</code> could
   * not be parsed as valid XML, the textFormat is changed to {@link TextFormat#Plain}.
   *
   * <p>Therefore, if you wish to use {@link TextFormat#XHTML}, but cannot be sure about <code>text
   * </code> being valid XML, you should verify that the {@link WText#getTextFormat()
   * getTextFormat()} is {@link TextFormat#XHTML} after construction.
   *
   * <p>The XML parser will silently discard malicious tags and attributes for literal {@link
   * TextFormat#XHTML} text.
   */
  public WText(final CharSequence text, TextFormat format, WContainerWidget parentContainer) {
    super();
    this.text_ = new WText.RichText();
    this.flags_ = new BitSet();
    this.padding_ = null;
    this.text_.format = format;
    this.flags_.set(BIT_WORD_WRAP);
    this.setText(text);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a text widget with given text and format.
   *
   * <p>Calls {@link #WText(CharSequence text, TextFormat format, WContainerWidget parentContainer)
   * this(text, format, (WContainerWidget)null)}
   */
  public WText(final CharSequence text, TextFormat format) {
    this(text, format, (WContainerWidget) null);
  }
  /** Destructor. */
  public void remove() {

    super.remove();
  }
  /**
   * Returns the text.
   *
   * <p>When a literal XHTMLFormatted text was set, this may differ from the text that was set since
   * malicious tags/attributes may have been stripped.
   *
   * <p>
   *
   * @see WText#setText(CharSequence text)
   */
  public WString getText() {
    return this.text_.text;
  }
  /**
   * Sets the text.
   *
   * <p>When the current format is {@link TextFormat#XHTML}, and <code>text</code> is literal (not
   * created using {@link WString#tr(String key) WString#tr()}), it is parsed using an XML parser
   * which discards malicious tags and attributes silently. When the parser encounters an XML parse
   * error, the textFormat is changed to {@link TextFormat#Plain}. If <code>text</code> is not a
   * literal, the same parser is applied only when the text is resolved.
   *
   * <p>Returns whether the text could be set using the current textFormat. A return value of <code>
   * false</code> indicates that the textFormat was changed in order to be able to accept the new
   * text.
   *
   * <p>
   *
   * @see WText#getText()
   * @see WText#setText(CharSequence text)
   */
  public boolean setText(final CharSequence text) {
    boolean unChanged =
        canOptimizeUpdates() && (text.toString().equals(this.text_.text.toString()));
    boolean ok = this.text_.setText(text);
    if (canOptimizeUpdates() && unChanged) {
      return true;
    }
    this.flags_.set(BIT_TEXT_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    return ok;
  }
  /**
   * Sets the text format.
   *
   * <p>The textFormat controls how the string should be interpreted: either as plain text, which is
   * displayed literally, or as XHTML-markup.
   *
   * <p>When changing the textFormat to {@link TextFormat#XHTML}, and the current text is literal
   * (not created using {@link WString#tr(String key) WString#tr()}), the current text is parsed
   * using an XML parser which discards malicious tags and attributes silently. When the parser
   * encounters an XML parse error, the textFormat is left unchanged, and this method returns false.
   *
   * <p>Returns whether the textFormat could be set for the current text.
   *
   * <p>The default format is {@link TextFormat#XHTML}.
   */
  public boolean setTextFormat(TextFormat textFormat) {
    return this.text_.setFormat(textFormat);
  }
  /**
   * Returns the text format.
   *
   * <p>
   *
   * @see WText#setTextFormat(TextFormat textFormat)
   */
  public TextFormat getTextFormat() {
    return this.text_.format;
  }
  /**
   * Configures word wrapping.
   *
   * <p>When <code>wordWrap</code> is <code>true</code>, the widget may break lines, creating a
   * multi-line text. When <code>wordWrap</code> is <code>false</code>, the text will displayed on a
   * single line, unless the text contains end-of-lines (for {@link TextFormat#Plain}) or &lt;br
   * /&gt; tags or other block-level tags (for {@link TextFormat#XHTML}).
   *
   * <p>The default value is <code>true</code>.
   *
   * <p>
   *
   * @see WText#isWordWrap()
   */
  public void setWordWrap(boolean wordWrap) {
    if (this.flags_.get(BIT_WORD_WRAP) != wordWrap) {
      this.flags_.set(BIT_WORD_WRAP, wordWrap);
      this.flags_.set(BIT_WORD_WRAP_CHANGED);
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
  }
  /**
   * Returns whether the widget may break lines.
   *
   * <p>
   *
   * @see WText#setWordWrap(boolean wordWrap)
   */
  public boolean isWordWrap() {
    return this.flags_.get(BIT_WORD_WRAP);
  }
  /**
   * Specifies how text is aligned.
   *
   * <p>Only the horizontal alignment can be specified. Note that there is no way to specify
   * vertical alignment. You can put the text in a layout with vertical alignment options though, or
   * (misuse) the line-height CSS property for single line texts.
   */
  public void setTextAlignment(AlignmentFlag textAlignment) {
    this.flags_.clear(BIT_TEXT_ALIGN_LEFT);
    this.flags_.clear(BIT_TEXT_ALIGN_CENTER);
    this.flags_.clear(BIT_TEXT_ALIGN_RIGHT);
    switch (textAlignment) {
      case Left:
        this.flags_.set(BIT_TEXT_ALIGN_LEFT);
        break;
      case Center:
        this.flags_.set(BIT_TEXT_ALIGN_CENTER);
        break;
      case Right:
        this.flags_.set(BIT_TEXT_ALIGN_RIGHT);
        break;
      default:
        logger.error(
            new StringWriter()
                .append("setTextAlignment(): illegal value for textAlignment")
                .toString());
        return;
    }
    this.flags_.set(BIT_TEXT_ALIGN_CHANGED);
    this.repaint();
  }
  /**
   * Returns the alignment of children.
   *
   * <p>
   *
   * @see WText#setTextAlignment(AlignmentFlag textAlignment)
   */
  public AlignmentFlag getTextAlignment() {
    if (this.flags_.get(BIT_TEXT_ALIGN_CENTER)) {
      return AlignmentFlag.Center;
    } else {
      if (this.flags_.get(BIT_TEXT_ALIGN_RIGHT)) {
        return AlignmentFlag.Right;
      } else {
        return AlignmentFlag.Left;
      }
    }
  }
  /**
   * Sets padding inside the widget.
   *
   * <p>Setting padding has the effect of adding distance between the widget children and the
   * border.
   *
   * <p>
   *
   * <p><i><b>Note: </b>for an {@link WWebWidget#setInline(boolean inl) inline} WText padding is
   * only supported on the left and/or right. Setting padding on the top or bottom has no effect.
   * </i>
   */
  public void setPadding(final WLength length, EnumSet<Side> sides) {
    if (!(this.padding_ != null)) {
      this.padding_ = new WLength[4];
      this.padding_[0] = this.padding_[1] = this.padding_[2] = this.padding_[3] = WLength.Auto;
    }
    if (sides.contains(Side.Top)) {
      if (this.isInline()) {
        logger.warn(
            new StringWriter()
                .append(
                    "setPadding(..., Side::Top) is not supported for inline WText. If your WText is not inline, you can call setInline(true) before setPadding(...) to disable this warning.")
                .toString());
      }
      this.padding_[0] = length;
    }
    if (sides.contains(Side.Right)) {
      this.padding_[1] = length;
    }
    if (sides.contains(Side.Bottom)) {
      if (this.isInline()) {
        logger.warn(
            new StringWriter()
                .append(
                    "setPadding(..., Side::Bottom) is not supported for inline WText. If your WText is not inline, you can call setInline(true) before setPadding(...) to disable this warning.")
                .toString());
      }
      this.padding_[2] = length;
    }
    if (sides.contains(Side.Left)) {
      this.padding_[3] = length;
    }
    this.flags_.set(BIT_PADDINGS_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }
  /**
   * Sets padding inside the widget.
   *
   * <p>Calls {@link #setPadding(WLength length, EnumSet sides) setPadding(length, EnumSet.of(side,
   * sides))}
   */
  public final void setPadding(final WLength length, Side side, Side... sides) {
    setPadding(length, EnumSet.of(side, sides));
  }
  /**
   * Sets padding inside the widget.
   *
   * <p>Calls {@link #setPadding(WLength length, EnumSet sides) setPadding(length, EnumSet.of
   * (Side.Left, Side.Right))}
   */
  public final void setPadding(final WLength length) {
    setPadding(length, EnumSet.of(Side.Left, Side.Right));
  }
  /**
   * Returns the padding set for the widget.
   *
   * <p>
   *
   * @see WText#setPadding(WLength length, EnumSet sides)
   */
  public WLength getPadding(Side side) {
    if (!(this.padding_ != null)) {
      return WLength.Auto;
    }
    switch (side) {
      case Top:
        return this.padding_[0];
      case Right:
        return this.padding_[1];
      case Bottom:
        return this.padding_[2];
      case Left:
        return this.padding_[3];
      default:
        logger.error(new StringWriter().append("padding(): improper side.").toString());
        return new WLength();
    }
  }
  /**
   * Enables internal path encoding of anchors in the XHTML text.
   *
   * <p>Anchors to internal paths are represented differently depending on the session
   * implementation (plain HTML, Ajax or HTML5 history). By enabling this option, anchors which
   * reference an internal path (by referring a URL of the form <code>href=&quot;#/...&quot;</code>
   * ), are re-encoded to link to the internal path.
   *
   * <p>When using {@link TextFormat#XHTML} (or {@link TextFormat#UnsafeXHTML}) formatted text, the
   * text is pasted verbatim in the browser (with the exception of XSS filtering if applicable).
   * With this option, however, the XHTML text may be transformed at the cost of an additional XML
   * parsing step.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>
   */
  public void setInternalPathEncoding(boolean enabled) {
    if (this.flags_.get(BIT_ENCODE_INTERNAL_PATHS) != enabled) {
      this.flags_.set(BIT_ENCODE_INTERNAL_PATHS, enabled);
      this.flags_.set(BIT_TEXT_CHANGED);
    }
  }
  /**
   * Returns whether internal paths are encoded.
   *
   * <p>
   *
   * @see WText#setInternalPathEncoding(boolean enabled)
   */
  public boolean hasInternalPathEncoding() {
    return this.flags_.get(BIT_ENCODE_INTERNAL_PATHS);
  }

  public void refresh() {
    if (this.text_.text.refresh()) {
      this.flags_.set(BIT_TEXT_CHANGED);
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
    super.refresh();
  }

  static class RichText {
    private static Logger logger = LoggerFactory.getLogger(RichText.class);

    public RichText() {
      this.text = new WString();
      this.format = TextFormat.XHTML;
    }

    public WString text;
    public TextFormat format;

    public boolean setText(final CharSequence newText) {
      this.text = WString.toWString(newText);
      boolean ok = this.isCheckWellFormed();
      if (!ok) {
        this.format = TextFormat.Plain;
      }
      return ok;
    }

    public boolean setFormat(TextFormat newFormat) {
      if (this.format != newFormat) {
        TextFormat oldFormat = this.format;
        this.format = newFormat;
        boolean ok = this.isCheckWellFormed();
        if (!ok) {
          this.format = oldFormat;
        }
        return ok;
      } else {
        return true;
      }
    }

    public boolean isCheckWellFormed() {
      if (this.format == TextFormat.XHTML && this.text.isLiteral()) {
        return removeScript(this.text);
      } else {
        return true;
      }
    }

    public String getFormattedText() {
      if (this.format == TextFormat.XHTML && !this.text.isLiteral()) {
        WString copy = this.text;
        if (removeScript(copy)) {
          return copy.toXhtml();
        } else {
          return escapeText(this.text, true).toString();
        }
      } else {
        if (this.format == TextFormat.Plain) {
          return escapeText(this.text, true).toString();
        } else {
          return this.text.toXhtml();
        }
      }
    }
  }

  private WText.RichText text_;
  private static final int BIT_WORD_WRAP = 0;
  private static final int BIT_TEXT_CHANGED = 1;
  private static final int BIT_WORD_WRAP_CHANGED = 2;
  private static final int BIT_PADDINGS_CHANGED = 3;
  private static final int BIT_ENCODE_INTERNAL_PATHS = 4;
  private static final int BIT_TEXT_ALIGN_LEFT = 5;
  private static final int BIT_TEXT_ALIGN_CENTER = 6;
  private static final int BIT_TEXT_ALIGN_RIGHT = 7;
  private static final int BIT_TEXT_ALIGN_CHANGED = 8;
  BitSet flags_;

  private String getFormattedText() {
    if (this.text_.format == TextFormat.Plain) {
      return escapeText(this.text_.text, true).toString();
    } else {
      WApplication app = WApplication.getInstance();
      if (this.flags_.get(BIT_ENCODE_INTERNAL_PATHS) || app.getSession().hasSessionIdInUrl()) {
        EnumSet<RefEncoderOption> options = EnumSet.noneOf(RefEncoderOption.class);
        if (this.flags_.get(BIT_ENCODE_INTERNAL_PATHS)) {
          options.add(RefEncoderOption.EncodeInternalPaths);
        }
        if (app.getSession().hasSessionIdInUrl()) {
          options.add(RefEncoderOption.EncodeRedirectTrampoline);
        }
        return RefEncoder.EncodeRefs(this.text_.text, options).toXhtml();
      } else {
        return this.text_.text.toXhtml();
      }
    }
  }

  private void autoAdjustInline() {
    if (this.text_.format != TextFormat.Plain && this.isInline()) {
      String t = this.text_.text.toString();
      t = StringUtils.trimLeft(t);
      if (StringUtils.startsWithIgnoreCase(t, "<div")
          || StringUtils.startsWithIgnoreCase(t, "<p")
          || StringUtils.startsWithIgnoreCase(t, "<h")) {
        this.setInline(false);
      }
    }
  }

  private WLength[] padding_;

  protected void render(EnumSet<RenderFlag> flags) {
    if (this.flags_.get(BIT_TEXT_CHANGED)) {
      this.autoAdjustInline();
    }
    super.render(flags);
  }

  void updateDom(final DomElement element, boolean all) {
    if (this.flags_.get(BIT_TEXT_CHANGED) || all) {
      String text = this.getFormattedText();
      if (this.flags_.get(BIT_TEXT_CHANGED) || text.length() != 0) {
        element.setProperty(Property.InnerHTML, text);
      }
      this.flags_.clear(BIT_TEXT_CHANGED);
    }
    if (this.flags_.get(BIT_WORD_WRAP_CHANGED) || all) {
      if (!all || !this.flags_.get(BIT_WORD_WRAP)) {
        element.setProperty(
            Property.StyleWhiteSpace, this.flags_.get(BIT_WORD_WRAP) ? "normal" : "nowrap");
      }
      this.flags_.clear(BIT_WORD_WRAP_CHANGED);
    }
    if (this.flags_.get(BIT_PADDINGS_CHANGED)
        || all
            && this.padding_ != null
            && !(this.padding_[0].isAuto()
                && this.padding_[1].isAuto()
                && this.padding_[2].isAuto()
                && this.padding_[3].isAuto())) {
      if (this.padding_[0].equals(this.padding_[1])
          && this.padding_[0].equals(this.padding_[2])
          && this.padding_[0].equals(this.padding_[3])) {
        element.setProperty(Property.StylePadding, this.padding_[0].getCssText());
      } else {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
          if (i != 0) {
            s.append(' ');
          }
          s.append(this.padding_[i].isAuto() ? "0" : this.padding_[i].getCssText());
        }
        element.setProperty(Property.StylePadding, s.toString());
      }
      this.flags_.clear(BIT_PADDINGS_CHANGED);
    }
    if (this.flags_.get(BIT_TEXT_ALIGN_CHANGED) || all) {
      if (this.flags_.get(BIT_TEXT_ALIGN_CENTER)) {
        element.setProperty(Property.StyleTextAlign, "center");
      } else {
        if (this.flags_.get(BIT_TEXT_ALIGN_RIGHT)) {
          element.setProperty(Property.StyleTextAlign, "right");
        } else {
          if (this.flags_.get(BIT_TEXT_ALIGN_LEFT)) {
            element.setProperty(Property.StyleTextAlign, "left");
          } else {
            if (!all) {
              element.setProperty(Property.StyleTextAlign, "");
            }
          }
        }
      }
      this.flags_.clear(BIT_TEXT_ALIGN_CHANGED);
    }
    super.updateDom(element, all);
  }

  DomElementType getDomElementType() {
    return this.isInline() ? DomElementType.SPAN : DomElementType.DIV;
  }

  void propagateRenderOk(boolean deep) {
    this.flags_.clear(BIT_TEXT_CHANGED);
    this.flags_.clear(BIT_WORD_WRAP_CHANGED);
    this.flags_.clear(BIT_PADDINGS_CHANGED);
    this.flags_.clear(BIT_TEXT_ALIGN_CHANGED);
    super.propagateRenderOk(deep);
  }
}
