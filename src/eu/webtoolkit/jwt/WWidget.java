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
 * The abstract base class for a user-interface component.
 *
 * <p>The user-interface is organized in a tree structure, in which each node is a widget. All
 * widgets, except for the application&apos;s root widget and dialogs, have a parent which is
 * usually a {@link WContainerWidget}.
 *
 * <p>This is an abstract base class. Implementations derive either from the abstract {@link
 * WWebWidget} (for basic widgets with a direct HTML counter-part) or from the abstract {@link
 * WCompositeWidget} (for anything else). To add a WWidget to a parent container add the widget to
 * the parent using {@link WContainerWidget#addWidget(WWidget widget) WContainerWidget#addWidget()}.
 * Alternatively, you may add the widget to a layout manager set for a WContainerWidget.
 *
 * <p>A widget provides methods to manage its decorative style base on CSS. It also provides access
 * to CSS-based layout, which you may not use when the widget is not inserted into a layout manager.
 */
public abstract class WWidget extends WObject {
  private static Logger logger = LoggerFactory.getLogger(WWidget.class);

  /**
   * Destructor.
   *
   * <p>Deletes a widget and all contained contents.
   *
   * <p>
   *
   * @see WWidget#removeWidget(WWidget widget)
   */
  public void remove() {
    while (!this.eventSignals_.isEmpty()) {
      AbstractEventSignal s = this.eventSignals_.getFirst();
      this.eventSignals_.removeFirst();
      ;
    }
    this.renderOk();
    super.remove();
  }
  /** Returns the parent widget. */
  public WWidget getParent() {
    return this.parent_;
  }
  /**
   * Returns child widgets.
   *
   * <p>This returns widgets for which widget.{@link WWidget#getParent() getParent()} == this.
   */
  public abstract List<WWidget> getChildren();
  /** Removes a child widget. */
  public WWidget removeWidget(WWidget widget) {
    throw new UnsupportedOperationException("WWidget::removeWidget() ought not to be called");
  }
  /**
   * Removes the widget from its parent.
   *
   * <p>This is equivalent to {@link WWidget#getParent() getParent()}.removeWidget(this);
   */
  public WWidget removeFromParent() {
    WWidget p = this.getParent();
    if (p != null) {
      return p.removeWidget(this);
    } else {
      return null;
    }
  }
  /**
   * Sets the CSS position scheme.
   *
   * <p>Establishes how the widget must be layed-out relative to its siblings. The default position
   * scheme is {@link PositionScheme#Static}.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see PositionScheme
   * @see WWidget#getPositionScheme()
   */
  public abstract void setPositionScheme(PositionScheme scheme);
  /**
   * Returns the CSS position scheme.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see PositionScheme
   * @see WWidget#setPositionScheme(PositionScheme scheme)
   */
  public abstract PositionScheme getPositionScheme();
  /**
   * Sets CSS offsets for a non-statically positioned widget.
   *
   * <p>The argument <code>sides</code> may be a combination of {@link Side#Left}, {@link
   * Side#Right}, {@link Side#Top}, and {@link Side#Bottom}.
   *
   * <p>This applies only to widgets that have a position scheme that is {@link
   * PositionScheme#Relative}, {@link PositionScheme#Absolute}, or {@link PositionScheme#Fixed}, and
   * has a slightly different meaning for these three cases.
   *
   * <p>For a {@link PositionScheme#Relative relatively positioned} widget, an offset applies
   * relative to the position the widget would have when layed-out using a {@link
   * PositionScheme#Static static} position scheme. The widget may be shifted to the left or right
   * by specifying an offset for the {@link Side#Left left} or {@link Side#Right right}) side. The
   * widget may be shifted vertically, by specifying an offset for the {@link Side#Top top} or
   * {@link Side#Bottom bottom} side.
   *
   * <p>For an {@link PositionScheme#Absolute absolutely positioned} widget, an offset specifies a
   * distance of the corresponding side of the widget with respect to the corresponding side of the
   * reference parent widget. Thus, setting all offsets to 0 result in a widget that spans the
   * entire reference widget. The reference parent widget is the first ancestor widget that is a
   * table cell, or a widget with a relative, absolute or fixed position scheme.
   *
   * <p>For an {@link PositionScheme#Fixed fixed positioned} widget, an offset specifies a distance
   * of the corresponding side of the widget with respect to the browser window, regardless of
   * scrolling. Thus, setting all offsets to 0 result in a widget that spans the entire browser
   * window.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#getOffset(Side side)
   */
  public abstract void setOffsets(final WLength offset, EnumSet<Side> sides);
  /**
   * Sets CSS offsets for a non-statically positioned widget.
   *
   * <p>Calls {@link #setOffsets(WLength offset, EnumSet sides) setOffsets(offset, EnumSet.of(side,
   * sides))}
   */
  public final void setOffsets(final WLength offset, Side side, Side... sides) {
    setOffsets(offset, EnumSet.of(side, sides));
  }
  /**
   * Sets CSS offsets for a non-statically positioned widget.
   *
   * <p>Calls {@link #setOffsets(WLength offset, EnumSet sides) setOffsets(offset, Side.AllSides)}
   */
  public final void setOffsets(final WLength offset) {
    setOffsets(offset, Side.AllSides);
  }
  /**
   * Sets CSS offsets for a non-statically positioned widget.
   *
   * <p>This is a convenience method for applying offsets in pixel units.
   *
   * <p>
   *
   * @see WWidget#setOffsets(WLength offset, EnumSet sides)
   */
  public void setOffsets(int pixels, EnumSet<Side> sides) {
    this.setOffsets(new WLength(pixels), sides);
  }
  /**
   * Sets CSS offsets for a non-statically positioned widget.
   *
   * <p>Calls {@link #setOffsets(int pixels, EnumSet sides) setOffsets(pixels, EnumSet.of(side,
   * sides))}
   */
  public final void setOffsets(int pixels, Side side, Side... sides) {
    setOffsets(pixels, EnumSet.of(side, sides));
  }
  /**
   * Sets CSS offsets for a non-statically positioned widget.
   *
   * <p>Calls {@link #setOffsets(int pixels, EnumSet sides) setOffsets(pixels, Side.AllSides)}
   */
  public final void setOffsets(int pixels) {
    setOffsets(pixels, Side.AllSides);
  }
  /**
   * Returns a CSS offset.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#setOffsets(WLength offset, EnumSet sides)
   */
  public abstract WLength getOffset(Side side);
  /**
   * Resizes the widget.
   *
   * <p>Specifies a fixed size for this widget, setting CSS <code>width</code> and <code>height
   * </code> properties. By default a widget has automatic width and height, which sets a size for
   * the widget following CSS rules.
   *
   * <p>When the widget is not managed by a layout manager, the automatic (natural) size of a widget
   * depends on whether they widget is a <i>block</i> or <i>inline</i> widget:
   *
   * <ul>
   *   <li>a <i>block</i> widget takes by default the width of the parent, and the height that it
   *       needs based on its contents
   *   <li>an <i>inline</i> widget takes the width and height that it needs based on its contents
   *       (possibly wrapping over multiple lines). The width and height of an inline widget cannot
   *       be changed (by the letter of CSS, although most browsers will react to it in varying
   *       ways).
   * </ul>
   *
   * <p>When inserted in a layout manager, the size set will be used as a widget&apos;s preferred
   * size, but the widget may be given a different size by the layout manager based on available
   * space and stretch factors. The actual size given by a layout manager may be retrieved by making
   * the widget &quot;layout size aware&quot;, using {@link WWidget#setLayoutSizeAware(boolean
   * aware) setLayoutSizeAware()}. If you have defined a <code>&quot;wtResize()&quot;</code>
   * JavaScript method for the widget, then this method will also be called.
   *
   * <p>The default width and height of a widget is {@link WLength#Auto}.
   *
   * <p>
   *
   * @see WWidget#getWidth()
   * @see WWidget#getHeight()
   */
  public void resize(final WLength width, final WLength height) {
    this.setJsSize();
  }
  /**
   * Resizes the widget.
   *
   * <p>This is a convenience method for resizing a widget using pixel units.
   *
   * <p>
   *
   * @see WWidget#resize(WLength width, WLength height)
   */
  public void resize(int widthPixels, int heightPixels) {
    this.resize(new WLength(widthPixels), new WLength(heightPixels));
  }
  /**
   * Returns the width.
   *
   * <p>Returns the width set for this widget. This is not a calculated width, based on layout, but
   * the width as specified with {@link WWidget#resize(WLength width, WLength height) resize()}.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#resize(WLength width, WLength height)
   * @see WWidget#getHeight()
   */
  public abstract WLength getWidth();
  /**
   * Sets the width.
   *
   * <p>This is a convenience method to change only the width of a widget, and is implemented as:
   *
   * <pre>{@code
   * resize(width, height())
   *
   * }</pre>
   *
   * <p>
   *
   * @see WWidget#resize(WLength width, WLength height)
   * @see WWidget#setHeight(WLength height)
   */
  public void setWidth(final WLength width) {
    this.resize(width, this.getHeight());
  }
  /**
   * Returns the height.
   *
   * <p>Returns the height set for this widget. This is not a calculated height, based on layout,
   * but the height as specified previously with {@link WWidget#resize(WLength width, WLength
   * height) resize()}.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#resize(WLength width, WLength height)
   * @see WWidget#getWidth()
   */
  public abstract WLength getHeight();
  /**
   * Sets the height.
   *
   * <p>This is a convenience method to change only the height of a widget, and is implemented as:
   *
   * <pre>{@code
   * resize(width(), height)
   *
   * }</pre>
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#resize(WLength width, WLength height)
   * @see WWidget#setWidth(WLength width)
   */
  public void setHeight(final WLength height) {
    this.resize(this.getWidth(), height);
  }
  /**
   * Sets a minimum size.
   *
   * <p>Specifies a minimum size for this widget, setting CSS <code>min-width</code> and <code>
   * min-height</code> properties.
   *
   * <p>The default minimum width and height is 0. The special value {@link WLength#Auto} indicates
   * that the initial width is used as minimum size. A {@link LengthUnit#Percentage} size should not
   * be used, as this is (in virtually all cases) undefined behaviour.
   *
   * <p>When the widget is inserted in a layout manager, then the minimum size will be taken into
   * account.
   *
   * <p>
   *
   * @see WWidget#resize(WLength width, WLength height)
   * @see WWidget#getMinimumWidth()
   * @see WWidget#getMinimumHeight()
   */
  public abstract void setMinimumSize(final WLength width, final WLength height);
  /**
   * Returns the minimum width.
   *
   * <p>Returns the minimum width set for this widget with {@link WWidget#setMinimumSize(WLength
   * width, WLength height) setMinimumSize()}.
   *
   * <p>
   *
   * @see WWidget#setMinimumSize(WLength width, WLength height)
   * @see WWidget#getMinimumHeight()
   */
  public abstract WLength getMinimumWidth();
  /**
   * Returns the minimum height.
   *
   * <p>Returns the minmum height set for this widget with {@link WWidget#setMinimumSize(WLength
   * width, WLength height) setMinimumSize()}.
   *
   * <p>
   *
   * @see WWidget#setMinimumSize(WLength width, WLength height)
   * @see WWidget#getMinimumWidth()
   */
  public abstract WLength getMinimumHeight();
  /**
   * Sets a maximum size.
   *
   * <p>Specifies a maximum size for this widget, setting CSS <code>max-width</code> and <code>
   * max-height</code> properties.
   *
   * <p>The default the maximum width and height are {@link WLength#Auto}, indicating no maximum
   * size. A {@link LengthUnit#Percentage} size should not be used, as this is (in virtually all
   * cases) undefined behaviour.
   *
   * <p>When the widget is a container widget that contains a layout manager, then setting a maximum
   * size will have the effect of letting the size of the container to reflect the preferred size of
   * the contents (rather than constraining the size of the children based on the size of the
   * container), up to the specified maximum size.
   *
   * <p>
   *
   * @see WWidget#resize(WLength width, WLength height)
   * @see WWidget#setMinimumSize(WLength width, WLength height)
   */
  public abstract void setMaximumSize(final WLength width, final WLength height);
  /**
   * Returns the maximum width.
   *
   * <p>Returns the maximum width set for this widget with {@link WWidget#setMaximumSize(WLength
   * width, WLength height) setMaximumSize()}.
   *
   * <p>
   *
   * @see WWidget#setMaximumSize(WLength width, WLength height)
   * @see WWidget#getMaximumHeight()
   */
  public abstract WLength getMaximumWidth();
  /**
   * Returns the maximum height.
   *
   * <p>Returns the minmum height set for this widget with {@link WWidget#setMaximumSize(WLength
   * width, WLength height) setMaximumSize()}.
   *
   * <p>
   *
   * @see WWidget#setMaximumSize(WLength width, WLength height)
   * @see WWidget#getMaximumWidth()
   */
  public abstract WLength getMaximumHeight();
  /**
   * Positions a widget next to another widget.
   *
   * <p>Positions this absolutely positioned widget next to another <code>widget</code>. Both
   * widgets must be visible (including all their ancestors). The current widget is shown
   * automatically if needed.
   *
   * <p>When <code>orientation</code> = {@link Orientation#Vertical}, the widget is displayed below
   * the other widget (or above in case there is not enough room below). It is aligned so that the
   * left edges align (or the right edges if there is not enough room to the right).
   *
   * <p>Conversely, when <code>orientation</code> = {@link Orientation#Horizontal}, the widget is
   * displayed to the right of the other widget (or to the left in case there is not enough room to
   * the right). It is aligned so that the top edges align (or the bottom edges if there is not
   * enough room below).
   *
   * <p>
   *
   * <p><i><b>Note: </b>This only works if JavaScript is available. </i>
   */
  public void positionAt(WWidget widget, Orientation orientation) {
    if (this.isHidden()) {
      this.show();
    }
    String side = orientation == Orientation.Horizontal ? ".Horizontal" : ".Vertical";
    this.doJavaScript(
        "Wt4.11.2.positionAtWidget('"
            + this.getId()
            + "','"
            + widget.getId()
            + "',Wt4.11.2"
            + side
            + ");");
  }
  /**
   * Positions a widget next to another widget.
   *
   * <p>Calls {@link #positionAt(WWidget widget, Orientation orientation) positionAt(widget,
   * Orientation.Vertical)}
   */
  public final void positionAt(WWidget widget) {
    positionAt(widget, Orientation.Vertical);
  }
  /** Sets the CSS line height for contained text. */
  public abstract void setLineHeight(final WLength height);
  /**
   * Returns the CSS line height for contained text.
   *
   * <p>sa {@link WWidget#setLineHeight(WLength height) setLineHeight()}
   */
  public abstract WLength getLineHeight();
  /**
   * Specifies a CSS float side.
   *
   * <p>This only applies to widgets with a {@link PositionScheme#Static} {@link
   * WWidget#getPositionScheme() getPositionScheme()}.
   *
   * <p>This lets the widget float to one of the sides of the parent widget, at the current line. A
   * typical use is to position images within text. Valid values for Side {@link Side#Left}, or
   * {@link Side#Right}.
   *
   * <p>This applies to CSS-based layout.
   */
  public abstract void setFloatSide(Side s);
  /**
   * Returns the CSS float side.
   *
   * <p>
   *
   * @see WWidget#setFloatSide(Side s)
   */
  public abstract Side getFloatSide();
  /**
   * Sets the sides that should be cleared of floats.
   *
   * <p>This pushes the widget down until it is not surrounded by floats at the <code>sides</code>
   * (which may be a combination of {@link Side#Left} and {@link Side#Right}).
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#setFloatSide(Side s)
   */
  public abstract void setClearSides(EnumSet<Side> sides);
  /**
   * Sets the sides that should be cleared of floats.
   *
   * <p>Calls {@link #setClearSides(EnumSet sides) setClearSides(EnumSet.of(side, sides))}
   */
  public final void setClearSides(Side side, Side... sides) {
    setClearSides(EnumSet.of(side, sides));
  }
  /**
   * Returns the sides that should remain empty.
   *
   * <p>
   *
   * @see WWidget#setClearSides(EnumSet sides)
   */
  public abstract EnumSet<Side> getClearSides();
  /**
   * Sets CSS margins around the widget.
   *
   * <p>Setting margin has the effect of adding a distance between the widget and surrounding
   * widgets. The default margin (with an automatic length) is zero.
   *
   * <p>Use any combination of {@link Side#Left}, {@link Side#Right}, {@link Side#Bottom}, or {@link
   * Side#Top}.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#getMargin(Side side)
   */
  public abstract void setMargin(final WLength margin, EnumSet<Side> sides);
  /**
   * Sets CSS margins around the widget.
   *
   * <p>Calls {@link #setMargin(WLength margin, EnumSet sides) setMargin(margin, EnumSet.of(side,
   * sides))}
   */
  public final void setMargin(final WLength margin, Side side, Side... sides) {
    setMargin(margin, EnumSet.of(side, sides));
  }
  /**
   * Sets CSS margins around the widget.
   *
   * <p>Calls {@link #setMargin(WLength margin, EnumSet sides) setMargin(margin, Side.AllSides)}
   */
  public final void setMargin(final WLength margin) {
    setMargin(margin, Side.AllSides);
  }
  /**
   * Sets CSS margins around the widget.
   *
   * <p>This is a convenience method for setting margins in pixel units.
   *
   * <p>
   *
   * @see WWidget#setMargin(WLength margin, EnumSet sides)
   */
  public void setMargin(int pixels, EnumSet<Side> sides) {
    this.setMargin(new WLength(pixels), sides);
  }
  /**
   * Sets CSS margins around the widget.
   *
   * <p>Calls {@link #setMargin(int pixels, EnumSet sides) setMargin(pixels, EnumSet.of(side,
   * sides))}
   */
  public final void setMargin(int pixels, Side side, Side... sides) {
    setMargin(pixels, EnumSet.of(side, sides));
  }
  /**
   * Sets CSS margins around the widget.
   *
   * <p>Calls {@link #setMargin(int pixels, EnumSet sides) setMargin(pixels, Side.AllSides)}
   */
  public final void setMargin(int pixels) {
    setMargin(pixels, Side.AllSides);
  }
  /**
   * Returns a CSS margin set.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#setMargin(WLength margin, EnumSet sides)
   */
  public abstract WLength getMargin(Side side);
  /**
   * Sets whether the widget keeps its geometry when hidden.
   *
   * <p>Normally, a widget that is hidden will no longer occupy space, causing a reflow of sibling
   * widgets. Using this method you may change this behavior to keep an (open) space when hidden.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Currently you can only set this before initial rendering. </i>
   *
   * @see WWidget#setHidden(boolean hidden, WAnimation animation)
   */
  public abstract void setHiddenKeepsGeometry(boolean enabled);
  /**
   * Returns whether the widget keeps its geometry when hidden.
   *
   * <p>
   *
   * @see WWidget#setHiddenKeepsGeometry(boolean enabled)
   */
  public abstract boolean isHiddenKeepsGeometry();
  /**
   * Hides or shows the widget.
   *
   * <p>Hides or show the widget (including all its descendant widgets). When setting <code>hidden
   * </code> = <code>false</code>, this widget and all descendant widgets that are not hidden will
   * be shown. A widget is only visible if it and all its ancestors in the widget tree are visible,
   * which may be checked using {@link WWidget#isVisible() isVisible()}.
   */
  public abstract void setHidden(boolean hidden, final WAnimation animation);
  /**
   * Hides or shows the widget.
   *
   * <p>Calls {@link #setHidden(boolean hidden, WAnimation animation) setHidden(hidden, new
   * WAnimation())}
   */
  public final void setHidden(boolean hidden) {
    setHidden(hidden, new WAnimation());
  }
  /**
   * Returns whether the widget is set hidden.
   *
   * <p>A widget that is not hidden may still be not visible when one of its ancestor widgets is
   * hidden. Use {@link WWidget#isVisible() isVisible()} to check the visibility of a widget.
   *
   * <p>
   *
   * @see WWidget#setHidden(boolean hidden, WAnimation animation)
   * @see WWidget#isVisible()
   */
  public abstract boolean isHidden();
  /**
   * Returns whether the widget is visible.
   *
   * <p>A widget is visible if it is not hidden, and none of its ancestors are hidden. This method
   * returns the true visibility, while {@link WWidget#isHidden() isHidden()} returns whether a
   * widget has been explicitly hidden.
   *
   * <p>Note that a widget may be at the same time not hidden, and not visible, in case one of its
   * ancestors was hidden.
   *
   * <p>
   *
   * @see WWidget#isHidden()
   */
  public abstract boolean isVisible();
  /**
   * Sets whether the widget is disabled.
   *
   * <p>Enables or disables the widget (including all its descendant widgets). setDisabled(false)
   * will enable this widget and all descendant widgets that are not disabled. A widget is only
   * enabled if it and all its ancestors in the widget tree are disabled.
   *
   * <p>Typically, a disabled form widget will not allow changing the value, and disabled widgets
   * will not react to mouse click events.
   *
   * <p>
   *
   * @see WWidget#disable()
   * @see WWidget#enable()
   */
  public abstract void setDisabled(boolean disabled);
  /**
   * Returns whether the widget is set disabled.
   *
   * <p>A widget that is not set disabled may still be disabled when one of its ancestor widgets is
   * set disabled. Use {@link WWidget#isEnabled() isEnabled()} to find out whether a widget is
   * enabled.
   *
   * <p>
   *
   * @see WWidget#setDisabled(boolean disabled)
   * @see WWidget#isEnabled()
   */
  public abstract boolean isDisabled();
  /**
   * Returns whether the widget is enabled.
   *
   * <p>A widget is enabled if it is not disabled, and none of its ancestors are disabled. This
   * method returns whether the widget is rendered as enabled, while {@link WWidget#isDisabled()
   * isDisabled()} returns whether a widget has been explicitly disabled.
   *
   * <p>Note that a widget may be at the same time not enabled, and not disabled, in case one of its
   * ancestors was disabled.
   *
   * <p>
   *
   * @see WWidget#isDisabled()
   */
  public abstract boolean isEnabled();
  /**
   * Lets the widget overlay over other sibling widgets.
   *
   * <p>A widget that {@link WWidget#isPopup() isPopup()} will be rendered on top of any other
   * sibling widget contained within the same parent (including other popup widgets previously added
   * to the container).
   *
   * <p>This will only have an effect when the widgetis either {@link PositionScheme#Absolute} or
   * {@link PositionScheme#Fixed} {@link WWidget#getPositionScheme() getPositionScheme()}.
   *
   * <p>This applies to CSS-based layout, and configures the z-index property.
   */
  public abstract void setPopup(boolean popup);
  /**
   * Returns whether the widget is overlayed.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#setPopup(boolean popup)
   */
  public abstract boolean isPopup();
  /**
   * Sets whether the widget is displayed inline or as a block.
   *
   * <p>This option changes whether this widget must be rendered in line with sibling widgets
   * wrapping at the right edge of the parent container (like text), or whether this widget must be
   * rendered as a rectangular block that stacks vertically with sibling widgets (unless a CSS float
   * property is applied). Depending on the widget type, the default value is inline (such as for
   * example for {@link WText}, or {@link WPushButton}), or block (such as for example for a {@link
   * WContainerWidget}).
   *
   * <p>This applies to CSS-based layout.
   */
  public abstract void setInline(boolean inlined);
  /**
   * Returns whether the widget is displayed inline or as block.
   *
   * <p>
   *
   * @see WWidget#setInline(boolean inlined)
   */
  public abstract boolean isInline();
  /**
   * Sets a CSS decoration style.
   *
   * <p>This copies the style over its current {@link WWidget#getDecorationStyle()
   * getDecorationStyle()}
   */
  public abstract void setDecorationStyle(final WCssDecorationStyle style);
  /**
   * Returns the decoration style of this widget.
   *
   * <p>This groups all decorative aspects of the widget, which do not affect the widget layout
   * (except for the border properties which may behave like extra margin around the widget).
   *
   * <p>When a decoration style has not been previously set, it returns a default decoration style
   * object.
   *
   * <p>
   *
   * @see WWidget#setDecorationStyle(WCssDecorationStyle style)
   */
  public abstract WCssDecorationStyle getDecorationStyle();
  /**
   * Sets (one or more) CSS style classes.
   *
   * <p>You may set one or more space separated style classes. CSS style class works in conjunction
   * with style sheet, and provides a flexible way to provide many widgets the same markup.
   *
   * <p>Setting an empty string removes the style class(es).
   *
   * <p>
   *
   * @see WApplication#getStyleSheet()
   */
  public abstract void setStyleClass(final String styleClass);
  /**
   * Returns the CSS style class.
   *
   * <p>
   *
   * @see WWidget#setStyleClass(String styleClass)
   */
  public abstract String getStyleClass();
  /**
   * Adds a CSS style class.
   *
   * <p>When <code>force</code> = <code>true</code>, a JavaScript call will be used to add the style
   * class to the DOM element (if JavaScript is available). This may be necessary when client-side
   * JavaScript manipulates the same style class.
   *
   * <p>The <code>styleClass</code> should be a single class (although multiple classes will work
   * for the common case that the additional style classes are all not yet present on the element.
   */
  public abstract void addStyleClass(final String styleClass, boolean force);
  /**
   * Adds a CSS style class.
   *
   * <p>Calls {@link #addStyleClass(String styleClass, boolean force) addStyleClass(styleClass,
   * false)}
   */
  public final void addStyleClass(final String styleClass) {
    addStyleClass(styleClass, false);
  }
  /**
   * Removes a CSS style class.
   *
   * <p>When <code>force</code> = <code>true</code>, a JavaScript call will be used to remove the
   * style class from the DOM element (if JavaScript is available). This may be necessary when
   * client-side JavaScript manipulates the same style class.
   *
   * <p>The <code>styleClass</code> should be a single class
   */
  public abstract void removeStyleClass(final String styleClass, boolean force);
  /**
   * Removes a CSS style class.
   *
   * <p>Calls {@link #removeStyleClass(String styleClass, boolean force)
   * removeStyleClass(styleClass, false)}
   */
  public final void removeStyleClass(final String styleClass) {
    removeStyleClass(styleClass, false);
  }
  /** Toggles a CSS style class. */
  public void toggleStyleClass(final String styleClass, boolean add, boolean force) {
    if (add) {
      this.addStyleClass(styleClass, force);
    } else {
      this.removeStyleClass(styleClass, force);
    }
  }
  /**
   * Toggles a CSS style class.
   *
   * <p>Calls {@link #toggleStyleClass(String styleClass, boolean add, boolean force)
   * toggleStyleClass(styleClass, add, false)}
   */
  public final void toggleStyleClass(final String styleClass, boolean add) {
    toggleStyleClass(styleClass, add, false);
  }
  /** Returns whether the widget has a style class. */
  public abstract boolean hasStyleClass(final String styleClass);
  /**
   * Sets the vertical alignment.
   *
   * <p>This only applies to inline widgets, and determines how to position itself on the current
   * line, with respect to sibling inline widgets.
   *
   * <p>This applies to CSS-based layout.
   */
  public abstract void setVerticalAlignment(AlignmentFlag alignment, final WLength length);
  /**
   * Sets the vertical alignment.
   *
   * <p>Calls {@link #setVerticalAlignment(AlignmentFlag alignment, WLength length)
   * setVerticalAlignment(alignment, WLength.Auto)}
   */
  public final void setVerticalAlignment(AlignmentFlag alignment) {
    setVerticalAlignment(alignment, WLength.Auto);
  }
  /**
   * Returns the vertical alignment.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#setVerticalAlignment(AlignmentFlag alignment, WLength length)
   */
  public abstract AlignmentFlag getVerticalAlignment();
  /**
   * Returns the fixed vertical alignment that was set.
   *
   * <p>This applies to CSS-based layout.
   *
   * <p>
   *
   * @see WWidget#setVerticalAlignment(AlignmentFlag alignment, WLength length)
   */
  public abstract WLength getVerticalAlignmentLength();
  /**
   * Sets a tooltip.
   *
   * <p>The tooltip is displayed when the cursor hovers over the widget.
   *
   * <p>When <code>textFormat</code> is {@link TextFormat#XHTML}, the tooltip may contain any valid
   * XHTML snippet. The tooltip will then be rendered using JavaScript.
   *
   * <p>Note: This will set deferred tooltip to false.
   *
   * <p>
   */
  public abstract void setToolTip(final CharSequence text, TextFormat textFormat);
  /**
   * Sets a tooltip.
   *
   * <p>Calls {@link #setToolTip(CharSequence text, TextFormat textFormat) setToolTip(text,
   * TextFormat.Plain)}
   */
  public final void setToolTip(final CharSequence text) {
    setToolTip(text, TextFormat.Plain);
  }
  /** Returns the tooltip. */
  public abstract WString getToolTip();
  /**
   * Enable deferred tooltip.
   *
   * <p>You may override {@link WWidget#getToolTip() getToolTip()} to read data only when the user
   * hovers over the widget.
   *
   * <p>When <code>textFormat</code> is {@link TextFormat#XHTML}, the tooltip may contain any valid
   * XHTML snippet. The tooltip will then be rendered using JavaScript.
   *
   * <p>Note: To change existing toolTip call {@link WWidget#setDeferredToolTip(boolean enable,
   * TextFormat textFormat) setDeferredToolTip()} again.
   *
   * <p>
   *
   * @see WWidget#getToolTip()
   */
  public abstract void setDeferredToolTip(boolean enable, TextFormat textFormat);
  /**
   * Enable deferred tooltip.
   *
   * <p>Calls {@link #setDeferredToolTip(boolean enable, TextFormat textFormat)
   * setDeferredToolTip(enable, TextFormat.Plain)}
   */
  public final void setDeferredToolTip(boolean enable) {
    setDeferredToolTip(enable, TextFormat.Plain);
  }
  /**
   * Refresh the widget.
   *
   * <p>The refresh method is invoked when the locale is changed using {@link
   * WApplication#setLocale(Locale locale, boolean doRefresh) WApplication#setLocale()} or when the
   * user hit the refresh button.
   *
   * <p>The widget must actualize its contents in response.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This does <b>not</b> rerender the widget! Calling {@link WWidget#refresh()
   * refresh()} usually does not have any effect (unless you&apos;ve reimplemented {@link
   * WWidget#refresh() refresh()} to attach to it an effect). </i>
   */
  public void refresh() {}
  /**
   * Returns a JavaScript expression to the corresponding DOM node.
   *
   * <p>You may want to use this in conjunction with {@link JSlot} or {@link
   * WWidget#doJavaScript(String js) doJavaScript()} in custom JavaScript code.
   *
   * <p>
   *
   * @see WWidget#isRendered()
   */
  public String getJsRef() {
    return "Wt4.11.2.$('" + this.getId() + "')";
  }
  /**
   * Sets an attribute value.
   *
   * <p>Sets the value for an HTML attribute.
   *
   * <p>This is only useful for HTML features that are not supported directly in JWt (and beware
   * that browsers have non-consistent support for many more exotic HTML features).
   *
   * <p>
   *
   * @see JSlot
   * @see WWidget#doJavaScript(String js)
   */
  public abstract void setAttributeValue(final String name, final String value);
  /**
   * Returns an attribute value.
   *
   * <p>
   *
   * @see WWidget#setAttributeValue(String name, String value)
   */
  public abstract String getAttributeValue(final String name);
  /**
   * Sets a JavaScript member.
   *
   * <p>This binds a JavaScript member, which is set as a JavaScript property to the DOM object that
   * implements this widget. The value may be any JavaScript expression, including a function.
   *
   * <p>Members that start with <code>&quot;wt&quot;</code> are reserved for internal use. You may
   * define a member <code>&quot;wtResize(self, width,
   * height)&quot;</code> method if your widget needs active layout management. If defined, this
   * method will be used by layout managers and when doing {@link WWidget#resize(WLength width,
   * WLength height) resize()} to set the size of the widget, instead of setting the CSS width and
   * height properties.
   */
  public abstract void setJavaScriptMember(final String name, final String value);
  /**
   * Returns the value of a JavaScript member.
   *
   * <p>
   *
   * @see WWidget#setJavaScriptMember(String name, String value)
   */
  public abstract String getJavaScriptMember(final String name);
  /**
   * Calls a JavaScript member.
   *
   * <p>This calls a JavaScript member.
   *
   * <p>
   *
   * @see WWidget#setJavaScriptMember(String name, String value)
   */
  public abstract void callJavaScriptMember(final String name, final String args);
  /**
   * Short hand for {@link WString#tr(String key) WString#tr()}
   *
   * <p>Creates a localized string with the given key.
   */
  public static WString tr(String key) {
    return WString.tr(key);
  }
  /**
   * Loads content just before the widget is used.
   *
   * <p>This function is called when a widget is inserted in the widget hierarchy. Widgets that get
   * inserted in the widget hierarchy will be rendered. Visible widgets are rendered immediately,
   * and invisible widgets in the back-ground (or not for a plain HTML session). This method is
   * called when the widget is directly or indirectly inserted into the widget tree.
   *
   * <p>The default implementation simply propagates the load signal to its children. You may want
   * to override this method to delay loading of resource-intensive contents.
   *
   * <p>During the life-time of a widget, this method may be called multiple times, so you should
   * make sure that you do a deferred initialization only once.
   */
  public abstract void load();
  /**
   * Returns whether this widget has been loaded.
   *
   * <p>
   *
   * @see WWidget#load()
   */
  public abstract boolean isLoaded();
  /**
   * Sets whether the widget can receive focus.
   *
   * <p>By default, only form widgets (descendants of {@link WFormWidget}), anchors ({@link
   * WAnchor}) and buttons ({@link WPushButton}) can receive focus.
   *
   * <p>Any other widget can be configured to receive focus however.
   */
  public abstract void setCanReceiveFocus(boolean enabled);
  /**
   * Returns whether the widget can receive focus.
   *
   * <p>
   *
   * @see WWidget#setCanReceiveFocus(boolean enabled)
   */
  public abstract boolean isCanReceiveFocus();
  /**
   * Sets focus.
   *
   * <p>This only has an effect for a widget which can receive focus, and is equivalent to
   * setFocus(true).
   *
   * <p>
   *
   * @see WWidget#setCanReceiveFocus(boolean enabled)
   */
  public void setFocus() {
    this.setFocus(true);
  }
  /**
   * Sets focus.
   *
   * <p>When using <code>focus</code> = <code>false</code>, you can undo a previous {@link
   * WWidget#setFocus() setFocus()} call.
   */
  public abstract void setFocus(boolean focus);
  /**
   * Set focus on the widget&apos;s first descendant.
   *
   * <p>Set focus on the widget itself, or on a first descendant which can receive focus.
   *
   * <p>Returns whether focus could be set.
   */
  public abstract boolean isSetFirstFocus();
  /** Returns whether the widget currently has the focus. */
  public abstract boolean hasFocus();
  /**
   * Sets the tab index.
   *
   * <p>For widgets that receive focus ({@link WWidget#isCanReceiveFocus() isCanReceiveFocus()}),
   * focus is passed on to the next widget in the <i>tabbing chain</i> based on their tab index.
   * When the user navigates through form widgets using the keyboard, widgets receive focus starting
   * from the element with the lowest tab index to elements with the highest tab index.
   *
   * <p>Widgets with a same tab index will receive focus in the same order as they are inserted in
   * the widget tree.
   *
   * <p>The default tab index is 0 (for a widget that can receive focus).
   *
   * <p>
   */
  public abstract void setTabIndex(int index);
  /**
   * Returns the tab index.
   *
   * <p>
   *
   * @see WWidget#setTabIndex(int index)
   */
  public abstract int getTabIndex();

  abstract int getZIndex();
  /**
   * Sets a mime type to be accepted for dropping.
   *
   * <p>You may specify a style class that is applied to the widget when the specified mimetype
   * hovers on top of it.
   *
   * <p>
   *
   * @see WWidget#dropEvent(WDropEvent event)
   * @see WInteractWidget#setDraggable(String mimeType, WWidget dragWidget, boolean
   *     isDragWidgetOnly, WObject sourceObject)
   * @see WWidget#stopAcceptDrops(String mimeType)
   */
  public void acceptDrops(final String mimeType, final String hoverStyleClass) {
    WWebWidget thisWebWidget = this.getWebWidget();
    if (thisWebWidget.setAcceptDropsImpl(mimeType, true, hoverStyleClass)) {
      thisWebWidget.otherImpl_.dropSignal_.addListener(
          this,
          (String e1, String e2, WMouseEvent e3) -> {
            WWidget.this.getDrop(e1, e2, e3);
          });
      thisWebWidget.otherImpl_.dropSignal2_.addListener(
          this,
          (String e1, String e2, WTouchEvent e3) -> {
            WWidget.this.getDropTouch(e1, e2, e3);
          });
    }
  }
  /**
   * Sets a mime type to be accepted for dropping.
   *
   * <p>Calls {@link #acceptDrops(String mimeType, String hoverStyleClass) acceptDrops(mimeType,
   * "")}
   */
  public final void acceptDrops(final String mimeType) {
    acceptDrops(mimeType, "");
  }
  /**
   * Indicates that a mime type is no longer accepted for dropping.
   *
   * <p>
   *
   * @see WWidget#acceptDrops(String mimeType, String hoverStyleClass)
   */
  public void stopAcceptDrops(final String mimeType) {
    WWebWidget thisWebWidget = this.getWebWidget();
    thisWebWidget.setAcceptDropsImpl(mimeType, false, "");
  }
  /**
   * Sets the CSS Id.
   *
   * <p>Sets a custom Id. Note that the Id must be unique across the whole widget tree, can only be
   * set right after construction and cannot be changed. This is mostly useful for in tests using a
   * test plan that manipulates DOM elements by Id.
   *
   * <p>By default, auto-generated id&apos;s are used.
   *
   * <p>
   *
   * <p><i><b>Note: </b>An id must start with a letter ([A-Za-z]), followed by one or more letters,
   * digits ([0-9]), hyphens (&quot;-&quot;), underscores (&quot;_&quot;), colons (&quot;:&quot;),
   * and periods (&quot;.&quot;). </i>
   *
   * <p><i><b>Warning: </b>We recommend that you leave the id of a widget unchanged. JWt uses the id
   * to identify widgets in the JavaScript it generates, and this can often leads to bugs. If you do
   * change the id, <b>only</b> change the id right after widget construction. However, usually
   * there&apos;s a more preferable alternative, like setting the object name ({@link
   * WObject#setObjectName(String name) WObject#setObjectName()}), or adding style classes ({@link
   * WWidget#addStyleClass(String styleClass, boolean force) addStyleClass()}). </i>
   *
   * @see WObject#getId()
   */
  public abstract void setId(final String id);
  /**
   * Finds a descendent widget by name.
   *
   * <p>
   *
   * @see WObject#setObjectName(String name)
   */
  public abstract WWidget find(final String name);
  /** Finds a descendent widget by id. */
  public abstract WWidget findById(final String id);
  /**
   * Streams the (X)HTML representation.
   *
   * <p>Streams the widget as CharEncoding::UTF8-encoded (HTML-compatible) XHTML.
   *
   * <p>This may be useful as a debugging tool for the web-savvy, or in other rare situations.
   * Usually, you will not deal directly with HTML, and calling this method on a widget that is
   * rendered may interfere with the library keeping track of changes to the widget.
   */
  public void htmlText(final Writer out) {
    DomElement element = this.createSDomElement(WApplication.getInstance());
    List<DomElement.TimeoutEvent> timeouts = new ArrayList<DomElement.TimeoutEvent>();
    EscapeOStream sout = new EscapeOStream(out);
    EscapeOStream js = new EscapeOStream();
    element.asHTML(sout, js, timeouts);
    WApplication.getInstance().doJavaScript(js.toString());
  }
  /**
   * Sets as selectable.
   *
   * <p>When a widget is made unselectable, a selection of text (or images) will not be visible (but
   * may still be possible).
   *
   * <p>By default, the widget inherits this property from its parent, and this property propagates
   * to all children. The top level container ({@link WApplication#getRoot()}) selectable by
   * default.
   */
  public abstract void setSelectable(boolean selectable);
  /**
   * Executes the given JavaScript statements when the widget is rendered or updated.
   *
   * <p>Calling {@link WApplication#doJavaScript(String javascript, boolean afterLoaded)
   * WApplication#doJavaScript()} with JavaScript code that refers to a widget using {@link
   * WWidget#getJsRef() getJsRef()}, that is still to be rendered may cause JavaScript errors
   * because the corresponding DOM node does not exist. This happens for example when a widget is
   * created, but not yet inserted in the widget tree.
   *
   * <p>This method guarantees that the JavaScript code is only run when the corresponding DOM node
   * (using {@link WWidget#getJsRef() getJsRef()}) resolves to a valid DOM object.
   *
   * <p>
   *
   * @see WWidget#getJsRef()
   */
  public abstract void doJavaScript(final String js);
  /**
   * Returns whether the widget is rendered.
   *
   * <p>
   *
   * @see WWidget#getJsRef()
   */
  public boolean isRendered() {
    WWidget self = this;
    return self.getWebWidget().isRendered();
  }

  String getInlineCssStyle() {
    WWebWidget ww = this.getWebWidget();
    DomElement e = DomElement.getForUpdate(ww, ww.getDomElementType());
    ww.updateDom(e, true);
    String result = e.getCssStyle();

    return result;
  }

  public String createJavaScript(final StringBuilder js, String insertJS) {
    WApplication app = WApplication.getInstance();
    DomElement de = this.createSDomElement(app);
    String var = de.getCreateVar();
    if (insertJS.length() != 0) {
      insertJS += var + ");";
    }
    de.createElement(js, app, insertJS);

    return var;
  }
  /**
   * Hides the widget.
   *
   * <p>
   *
   * @see WWidget#setHidden(boolean hidden, WAnimation animation)
   */
  public void hide() {
    this.flags_.set(BIT_WAS_HIDDEN, this.isHidden());
    this.setHidden(true);
  }
  /**
   * Hides the widget using an animation.
   *
   * <p>To hide the widget, the animation is replayed in reverse.
   *
   * <p>
   *
   * @see WWidget#setHidden(boolean hidden, WAnimation animation)
   */
  public void animateHide(final WAnimation animation) {
    this.setHidden(true, animation);
  }
  /**
   * Shows the widget.
   *
   * <p>
   *
   * @see WWidget#setHidden(boolean hidden, WAnimation animation)
   */
  public void show() {
    this.flags_.set(BIT_WAS_HIDDEN, this.isHidden());
    this.setHidden(false);
  }
  /**
   * Shows the widget using an animation.
   *
   * <p>
   *
   * @see WWidget#setHidden(boolean hidden, WAnimation animation)
   */
  public void animateShow(final WAnimation animation) {
    this.setHidden(false, animation);
  }
  /**
   * Enables the widget.
   *
   * <p>This calls {@link WWidget#setDisabled(boolean disabled) setDisabled()}.
   */
  public void enable() {
    this.flags_.set(BIT_WAS_DISABLED, this.isDisabled());
    this.setDisabled(false);
  }
  /**
   * Disable thes widget.
   *
   * <p>This calls {@link WWidget#setDisabled(boolean disabled) setDisabled()}.
   */
  public void disable() {
    this.flags_.set(BIT_WAS_DISABLED, this.isDisabled());
    this.setDisabled(true);
  }
  /**
   * Returns whether the widget is layout size aware.
   *
   * <p>
   *
   * @see WWidget#setLayoutSizeAware(boolean aware)
   */
  public boolean isLayoutSizeAware() {
    return this.flags_.get(BIT_RESIZE_AWARE);
  }
  /**
   * Returns whether scroll visibility detection is enabled for this widget.
   *
   * <p>
   *
   * @see WWidget#setScrollVisibilityEnabled(boolean enabled)
   */
  public abstract boolean isScrollVisibilityEnabled();
  /**
   * Sets whether scroll visibility detection is enabled for this widget.
   *
   * <p>Disabled by default. When enabled, the client keeps track of whether this widget is
   * currently visible inside of the browser window. A widget is &quot;scroll visible&quot; if it is
   * currently visible according to {@link WWidget#isVisible() isVisible()}, and its position is
   * inside of the browser window, with an extra margin determined by {@link
   * WWidget#getScrollVisibilityMargin() getScrollVisibilityMargin()}.
   *
   * <p>If scroll visibility changes, the {@link WWidget#scrollVisibilityChanged()
   * scrollVisibilityChanged()} signal is fired, and {@link WWidget#isScrollVisible()
   * isScrollVisible()} is updated.
   *
   * <p>This feature can be useful to implement infinite scroll, where a sentinel widget placed at
   * the bottom of the page causes more content to be loaded when it becomes visible, see the <code>
   * scrollvisibility</code> example.
   *
   * <p>This feature can also be used to lazy load widgets when they become visible.
   *
   * <p>
   *
   * <p><i><b>Note: </b>If the widget is &quot;scroll visible&quot; when scroll visibility detection
   * is first enabled, the {@link WWidget#scrollVisibilityChanged() scrollVisibilityChanged()}
   * signal will be emitted. If it is outside of the browser&apos;s viewport when first enabled, the
   * {@link WWidget#scrollVisibilityChanged() scrollVisibilityChanged()} signal will not be emitted.
   * </i>
   *
   * <p><i><b>Note: </b>If scroll visibility is enabled, disabled, and then enabled again, {@link
   * WWidget#isScrollVisible() isScrollVisible()} may not be correctly updated, and {@link
   * WWidget#scrollVisibilityChanged() scrollVisibilityChanged()} may not be correctly emitted,
   * because then JWt can&apos;t properly keep track of the state that the widget is in on the
   * client side. This feature is not intended to be toggled on and off, but rather enabled once and
   * disabled once after that. </i>
   */
  public abstract void setScrollVisibilityEnabled(boolean enabled);
  /**
   * Returns the margin around the viewport within which the widget is considered visible.
   *
   * <p>
   *
   * @see WWidget#setScrollVisibilityMargin(int margin)
   */
  public abstract int getScrollVisibilityMargin();
  /**
   * Sets the margin around the viewport within which the widget is considered visible.
   *
   * <p>This causes the widget to be considered &quot;scroll visible&quot; before it is within the
   * viewport. Setting this margin could be useful to trigger the loading of content before it is in
   * view.
   */
  public abstract void setScrollVisibilityMargin(int margin);
  /**
   * {@link Signal} triggered when the scroll visibility of this widget changes.
   *
   * <p>The boolean parameter indicates whether the widget is currently scroll visible.
   *
   * <p>
   *
   * @see WWidget#setScrollVisibilityEnabled(boolean enabled)
   */
  public abstract Signal1<Boolean> scrollVisibilityChanged();
  /**
   * Returns whether this widget is currently considered scroll visible.
   *
   * <p>{@link WWidget#isScrollVisible() isScrollVisible()} is initially false.
   *
   * <p>
   *
   * @see WWidget#setScrollVisibilityEnabled(boolean enabled)
   */
  public abstract boolean isScrollVisible();
  /**
   * Sets whether theme styling for a widget is enabled or disabled.
   *
   * <p>By default all widgets are styled according to the chosen theme. Disabling the theme style
   * could be useful to completely customize the style of the widget outside of the theme.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This should be changed after the construction but before the rendering of
   * the widget. </i>
   */
  public abstract void setThemeStyleEnabled(boolean enabled);
  /**
   * Returns whether this widget is currently styled by the chosen theme.
   *
   * <p>isThemeEnabled() is initially true.
   *
   * <p>
   *
   * @see WWidget#setThemeStyleEnabled(boolean enabled)
   */
  public abstract boolean isThemeStyleEnabled();

  DomElement createSDomElement(WApplication app) {
    if (!this.needsToBeRendered()) {
      DomElement result = this.getWebWidget().createStubElement(app);
      this.renderOk();
      this.scheduleRerender(true);
      return result;
    } else {
      this.getWebWidget().setRendered(true);
      this.render(EnumSet.of(RenderFlag.Full));
      return this.getWebWidget().createActualElement(this, app);
    }
  }

  static void setTabOrder(WWidget first, WWidget second) {
    second.setTabIndex(first.getTabIndex() + 1);
  }

  public boolean isExposed(WWidget w) {
    if (w == this) {
      return true;
    }
    for (WWidget p = w; p != null; p = p.getParent()) {
      if (p == this) {
        return true;
      }
    }
    return false;
  }

  public void addJSignal(AbstractEventSignal signal) {
    this.jsignals_.add(signal);
  }

  public abstract int getBaseZIndex();

  static String WT_RESIZE_JS = "wtResize";
  static String WT_GETPS_JS = "wtGetPS";
  /**
   * Sets the widget to be aware of its size set by a layout manager.
   *
   * <p>When the widget is inserted in a layout manager, it will be resized to fit within the
   * constraints imposed by the layout manager. By default, this done client-side only by setting
   * the CSS height (and if needed, width) properties of the DOM element corresponding to the
   * widget.
   *
   * <p>A widget may define a JavaScript method, <code>&quot;wtResize(self,
   * width, height)&quot;</code>, to actively manage its client-side width and height, if it wants
   * to react to these client-side size hints in a custom way (see {@link
   * WWidget#setJavaScriptMember(String name, String value) setJavaScriptMember()}).
   *
   * <p>By setting <code>sizeAware</code> to true, the widget will propagate the width and height
   * provided by the layout manager to the virtual {@link WWidget#layoutSizeChanged(int width, int
   * height) layoutSizeChanged()} method, so that you may for example change the size of contained
   * children in a particular way (doing a custom, manual, layout).
   *
   * <p>
   *
   * @see WWidget#layoutSizeChanged(int width, int height)
   */
  protected void setLayoutSizeAware(boolean aware) {
    if (aware == this.flags_.get(BIT_RESIZE_AWARE)) {
      return;
    }
    this.flags_.set(BIT_RESIZE_AWARE, aware);
    if (aware) {
      if (!(WApplication.getInstance() != null)) {
        return;
      }
      WWebWidget w = this.getWebWidget();
      if (w == this) {
        this.getWebWidget().resized();
      } else {
        this.getWebWidget()
            .resized()
            .addListener(
                this,
                (Integer e1, Integer e2) -> {
                  WWidget.this.layoutSizeChanged(e1, e2);
                });
      }
    } else {
      this.getWebWidget().setImplementLayoutSizeAware(false);
    }
  }
  /**
   * Virtual method that indicates a size change.
   *
   * <p>This method propagates the client-side width and height of the widget when the widget is
   * contained by a layout manager and setLayoutSizeAware(true) was called.
   *
   * <p>
   *
   * @see WWidget#setLayoutSizeAware(boolean aware)
   */
  protected void layoutSizeChanged(int width, int height) {}
  /** Creates a widget. */
  protected WWidget(WContainerWidget parentContainer) {
    super();
    this.flags_ = new BitSet();
    this.eventSignals_ = new LinkedList<AbstractEventSignal>();
    this.jsignals_ = new ArrayList<AbstractEventSignal>();
    this.parent_ = null;
    this.flags_.set(BIT_NEED_RERENDER);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a widget.
   *
   * <p>Calls {@link #WWidget(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  protected WWidget() {
    this((WContainerWidget) null);
  }
  /**
   * Handles a drop event.
   *
   * <p>Reimplement this method to handle a drop events for mime types you declared to accept using
   * acceptDrops.
   *
   * <p>The default implementation simply completes the drag and drop operation as if nothing
   * happened.
   *
   * <p>
   *
   * @see WWidget#acceptDrops(String mimeType, String hoverStyleClass)
   * @see WInteractWidget#setDraggable(String mimeType, WWidget dragWidget, boolean
   *     isDragWidgetOnly, WObject sourceObject)
   */
  protected void dropEvent(WDropEvent event) {}
  /**
   * Progresses to an Ajax-enabled widget.
   *
   * <p>This method is called when the progressive bootstrap method is used, and support for AJAX
   * has been detected. The default behavior will upgrade the widget&apos;s event handling to use
   * AJAX instead of full page reloads, and propagate the call to its children.
   *
   * <p>You may want to reimplement this method if you want to make changes to widget when AJAX is
   * enabled. You should always call the base implementation.
   *
   * <p>
   *
   * @see WApplication#enableAjax()
   */
  protected abstract void enableAjax();
  /**
   * Returns the widget&apos;s built-in padding.
   *
   * <p>This is used by the layout managers to correct for a built-in padding which interferes with
   * setting a widget&apos;s width (or height) to 100%.
   *
   * <p>A layout manager needs to set the width to 100% only for form widgets ({@link WTextArea},
   * {@link WLineEdit}, {@link WComboBox}, etc...). Therefore, only for those widgets this needs to
   * return the padding (the default implementation returns 0).
   *
   * <p>For form widgets, the padding depends on the specific browser/platform combination, unless
   * an explicit padding is set for the widget.
   *
   * <p>When setting an explicit padding for the widget using a style class, you will want to
   * reimplement this method to return this padding in case you want to set the widget inside a
   * layout manager.
   *
   * <p>
   *
   * @see WWidget#boxBorder(Orientation orientation)
   */
  protected int boxPadding(Orientation orientation) {
    return 0;
  }
  /**
   * Returns the widget&apos;s built-in border width.
   *
   * <p>This is used by the layout managers to correct for a built-in border which interferes with
   * setting a widget&apos;s width (or height) to 100%.
   *
   * <p>A layout manager needs to set the width to 100% only for form widgets ({@link WTextArea},
   * {@link WLineEdit}, {@link WComboBox}, etc...). Therefore, only for those widgets this needs to
   * return the border width (the default implementation returns 0).
   *
   * <p>For form widgets, the border width depends on the specific browser/platform combination,
   * unless an explicit border is set for the widget.
   *
   * <p>When setting an explicit border for the widget using a style class, you will want to
   * reimplement this method to return this border width, in case you want to set the widget inside
   * a layout manager.
   *
   * <p>
   *
   * @see WWidget#boxPadding(Orientation orientation)
   */
  protected int boxBorder(Orientation orientation) {
    return 0;
  }
  /**
   * Propagates that a widget was enabled or disabled through children.
   *
   * <p>When enabling or disabling a widget, you usually also want to disable contained children.
   * This method is called by {@link WWidget#setDisabled(boolean disabled) setDisabled()} to
   * propagate its state to all children.
   *
   * <p>You may want to reimplement this method if they wish to render differently when a widget is
   * disabled. The default implementation will propagate the signal to all children.
   */
  protected abstract void propagateSetEnabled(boolean enabled);

  protected abstract void propagateSetVisible(boolean visible);

  void getDrop(final String sourceId, final String mimeType, WMouseEvent event) {
    WDropEvent e =
        new WDropEvent(WApplication.getInstance().decodeObject(sourceId), mimeType, event);
    this.dropEvent(e);
  }

  protected void getDropTouch(final String sourceId, final String mimeType, WTouchEvent event) {
    WDropEvent e =
        new WDropEvent(WApplication.getInstance().decodeObject(sourceId), mimeType, event);
    this.dropEvent(e);
  }

  abstract void setHideWithOffsets(boolean how);

  final void setHideWithOffsets() {
    setHideWithOffsets(true);
  }

  abstract boolean isStubbed();
  /**
   * Schedules rerendering of the widget.
   *
   * <p>This schedules a rendering phase after all events have been processed. This method is used
   * internally whenever a property of a widget has been changed. But you may want to use this if
   * you are deferring actual changes to a widget in response to an event, and instead postpone this
   * until all events have been received.
   *
   * <p>
   *
   * @see WWidget#render(EnumSet flags)
   */
  protected void scheduleRender(EnumSet<RepaintFlag> flags) {
    this.scheduleRerender(false, flags);
  }
  /**
   * Schedules rerendering of the widget.
   *
   * <p>Calls {@link #scheduleRender(EnumSet flags) scheduleRender(EnumSet.of(flag, flags))}
   */
  protected final void scheduleRender(RepaintFlag flag, RepaintFlag... flags) {
    scheduleRender(EnumSet.of(flag, flags));
  }
  /**
   * Schedules rerendering of the widget.
   *
   * <p>Calls {@link #scheduleRender(EnumSet flags)
   * scheduleRender(EnumSet.noneOf(RepaintFlag.class))}
   */
  protected final void scheduleRender() {
    scheduleRender(EnumSet.noneOf(RepaintFlag.class));
  }
  /**
   * Renders the widget.
   *
   * <p>This function renders the widget (or an update for the widget), after this has been
   * scheduled using {@link WWidget#scheduleRender(EnumSet flags) scheduleRender()}.
   *
   * <p>The default implementation will render the widget by serializing changes to JavaScript and
   * HTML. You may want to reimplement this widget if you have been postponing some of the layout /
   * rendering implementation until the latest moment possible. In that case you should make sure
   * you call the base implementation however.
   */
  protected void render(EnumSet<RenderFlag> flags) {}
  /**
   * Renders the widget.
   *
   * <p>Calls {@link #render(EnumSet flags) render(EnumSet.of(flag, flags))}
   */
  protected final void render(RenderFlag flag, RenderFlag... flags) {
    render(EnumSet.of(flag, flags));
  }

  void childResized(WWidget child, EnumSet<Orientation> directions) {
    if (this.getPositionScheme() == PositionScheme.Absolute && !this.isInLayout()) {
      return;
    }
    WWidget p = this.getParent();
    if (p != null) {
      p.childResized(this, directions);
    }
  }

  final void childResized(WWidget child, Orientation direction, Orientation... directions) {
    childResized(child, EnumSet.of(direction, directions));
  }

  WWidget getAdam() {
    WWidget p = this.getParent();
    return p != null ? p.getAdam() : this;
  }

  void addEventSignal(final AbstractEventSignal s) {
    this.eventSignals_.addLast(s);
  }

  AbstractEventSignal getEventSignal(String name) {
    for (Iterator<AbstractEventSignal> i_it = this.eventSignals_.iterator(); i_it.hasNext(); ) {
      AbstractEventSignal i = i_it.next();
      final AbstractEventSignal s = i;
      if (s.getName() == name) {
        return s;
      }
    }
    return null;
  }

  LinkedList<AbstractEventSignal> eventSignals() {
    return this.eventSignals_;
  }
  // protected AbstractEventSignal.LearningListener  getStateless(<pointertomember or
  // dependentsizedarray> methodpointertomember or dependentsizedarray>) ;
  void renderOk() {
    if (this.flags_.get(BIT_NEED_RERENDER)) {
      this.flags_.clear(BIT_NEED_RERENDER);
      this.flags_.clear(BIT_NEED_RERENDER_SIZE_CHANGE);
      WApplication app = WApplication.getInstance();
      if (app != null) {
        app.getSession().getRenderer().doneUpdate(this);
      }
    }
  }

  void scheduleRerender(boolean laterOnly, EnumSet<RepaintFlag> flags) {
    if (!this.flags_.get(BIT_NEED_RERENDER)) {
      this.flags_.set(BIT_NEED_RERENDER);
      WApplication.getInstance().getSession().getRenderer().needUpdate(this, laterOnly);
    }
    if (flags.contains(RepaintFlag.SizeAffected)
        && !this.flags_.get(BIT_NEED_RERENDER_SIZE_CHANGE)) {
      this.flags_.set(BIT_NEED_RERENDER_SIZE_CHANGE);
      this.getWebWidget().parentResized(this, EnumSet.of(Orientation.Vertical));
      if (this.getPositionScheme() == PositionScheme.Absolute && !this.isInLayout()) {
        return;
      }
      WWidget p = this.getParent();
      if (p != null) {
        p.childResized(this, EnumSet.of(Orientation.Vertical));
      }
    }
  }

  final void scheduleRerender(boolean laterOnly, RepaintFlag flag, RepaintFlag... flags) {
    scheduleRerender(laterOnly, EnumSet.of(flag, flags));
  }

  final void scheduleRerender(boolean laterOnly) {
    scheduleRerender(laterOnly, EnumSet.noneOf(RepaintFlag.class));
  }

  public boolean needsRerender() {
    return this.flags_.get(BIT_NEED_RERENDER);
  }

  abstract void getSDomChanges(final List<DomElement> result, WApplication app);

  abstract boolean needsToBeRendered();

  boolean isInLayout() {
    WWidget p = this.getParent();
    if (p != null
        && (ObjectUtils.cast(p, WCompositeWidget.class) != null
            || p.getJavaScriptMember(WT_RESIZE_JS).length() != 0)) {
      return p.isInLayout();
    }
    WContainerWidget c = ObjectUtils.cast(p, WContainerWidget.class);
    return c != null && c.getLayout() != null;
  }

  boolean hasParent() {
    return this.parent_ != null;
  }

  protected WCssTextRule addCssRule(
      final String selector, final String declarations, final String ruleName) {
    WApplication app = WApplication.getInstance();
    WCssTextRule rule = new WCssTextRule(selector, declarations);
    WCssTextRule result = rule;
    app.getStyleSheet().addRule(rule, ruleName);
    return result;
  }

  protected final WCssTextRule addCssRule(final String selector, final String declarations) {
    return addCssRule(selector, declarations, "");
  }

  protected boolean isGlobalWidget() {
    return this.flags_.get(BIT_GLOBAL_WIDGET);
  }

  abstract String renderRemoveJs(boolean recursive);

  private static final int BIT_WAS_HIDDEN = 0;
  private static final int BIT_WAS_DISABLED = 1;
  private static final int BIT_NEED_RERENDER = 2;
  private static final int BIT_NEED_RERENDER_SIZE_CHANGE = 3;
  private static final int BIT_HAS_PARENT = 4;
  private static final int BIT_RESIZE_AWARE = 5;
  private static final int BIT_SCROLL_VISIBILITY_ENABLED = 6;
  private static final int BIT_GLOBAL_WIDGET = 7;
  private BitSet flags_;
  private LinkedList<AbstractEventSignal> eventSignals_;
  List<AbstractEventSignal> jsignals_;
  WWidget parent_;

  private void setJsSize() {
    if (!this.getHeight().isAuto()
        && this.getHeight().getUnit() != LengthUnit.Percentage
        && this.getJavaScriptMember(WT_RESIZE_JS).length() != 0) {
      this.callJavaScriptMember(
          WT_RESIZE_JS,
          this.getJsRef()
              + ","
              + String.valueOf(this.getWidth().toPixels())
              + ","
              + String.valueOf(this.getHeight().toPixels())
              + ","
              + "false");
    }
  }

  private void undoHideShow() {
    this.setHidden(this.flags_.get(BIT_WAS_HIDDEN));
  }

  private void undoDisableEnable() {
    this.setDisabled(this.flags_.get(BIT_WAS_DISABLED));
  }

  void setParentWidget(WWidget p) {
    this.parent_ = p;
  }

  void setGlobalWidget(boolean globalWidget) {
    this.flags_.set(BIT_GLOBAL_WIDGET, globalWidget);
  }

  abstract WWebWidget getWebWidget();
}
