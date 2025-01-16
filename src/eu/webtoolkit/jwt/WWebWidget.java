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
 * A base class for widgets with an HTML counterpart.
 *
 * <p>All descendants of WWebWidget implement a widget which corresponds almost one-on-one with an
 * HTML element. These widgets provide most capabilities of these HTML elements, but rarely make no
 * attempt to do anything more.
 *
 * <p>
 *
 * @see WCompositeWidget
 */
public abstract class WWebWidget extends WWidget {
  private static Logger logger = LoggerFactory.getLogger(WWebWidget.class);

  /** Construct a WebWidget. */
  public WWebWidget(WContainerWidget parentContainer) {
    super();
    this.elementTagName_ = "";
    this.flags_ = new BitSet();
    this.width_ = null;
    this.height_ = null;
    this.id_ = (String) null;
    this.transientImpl_ = null;
    this.layoutImpl_ = null;
    this.lookImpl_ = null;
    this.otherImpl_ = null;
    this.flags_.set(BIT_INLINE);
    this.flags_.set(BIT_ENABLED);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Construct a WebWidget.
   *
   * <p>Calls {@link #WWebWidget(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WWebWidget() {
    this((WContainerWidget) null);
  }

  public void remove() {
    this.beingDeleted();
    WWidget unique_this = this.removeFromParent();
    this.transientImpl_ = null;
    this.layoutImpl_ = null;
    this.lookImpl_ = null;
    this.otherImpl_ = null;
    super.remove();
  }

  public List<WWidget> getChildren() {
    final List<WWidget> result = new ArrayList<WWidget>();
    this.iterateChildren(
        (WWidget c) -> {
          result.add(c);
        });
    return result;
  }
  /**
   * Signal emitted when children have been added or removed.
   *
   * <p>
   *
   * @see WWebWidget#getChildren()
   */
  public Signal childrenChanged() {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    return this.otherImpl_.childrenChanged_;
  }

  public void setPositionScheme(PositionScheme scheme) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.positionScheme_ = scheme;
    if (scheme == PositionScheme.Absolute || scheme == PositionScheme.Fixed) {
      this.flags_.clear(BIT_INLINE);
    }
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  public PositionScheme getPositionScheme() {
    return this.layoutImpl_ != null ? this.layoutImpl_.positionScheme_ : PositionScheme.Static;
  }

  public void setOffsets(final WLength offset, EnumSet<Side> sides) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    if (sides.contains(Side.Top)) {
      this.layoutImpl_.offsets_[0] = offset;
    }
    if (sides.contains(Side.Right)) {
      this.layoutImpl_.offsets_[1] = offset;
    }
    if (sides.contains(Side.Bottom)) {
      this.layoutImpl_.offsets_[2] = offset;
    }
    if (sides.contains(Side.Left)) {
      this.layoutImpl_.offsets_[3] = offset;
    }
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint();
  }

  public WLength getOffset(Side s) {
    if (!(this.layoutImpl_ != null)) {
      return WLength.Auto;
    }
    switch (s) {
      case Top:
        return this.layoutImpl_.offsets_[0];
      case Right:
        return this.layoutImpl_.offsets_[1];
      case Bottom:
        return this.layoutImpl_.offsets_[2];
      case Left:
        return this.layoutImpl_.offsets_[3];
      default:
        logger.error(
            new StringWriter()
                .append("offset(Side) with invalid side: ")
                .append(String.valueOf((int) s.getValue()))
                .toString());
        return new WLength();
    }
  }

  public void resize(final WLength width, final WLength height) {
    boolean changed = false;
    if (!(this.width_ != null) && !width.isAuto()) {
      this.width_ = new WLength();
    }
    if (this.width_ != null && !this.width_.equals(width)) {
      changed = true;
      this.width_ = nonNegative(width);
      this.flags_.set(BIT_WIDTH_CHANGED);
    }
    if (!(this.height_ != null) && !height.isAuto()) {
      this.height_ = new WLength();
    }
    if (this.height_ != null && !this.height_.equals(height)) {
      changed = true;
      this.height_ = nonNegative(height);
      this.flags_.set(BIT_HEIGHT_CHANGED);
    }
    if (changed) {
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
      super.resize(width, height);
    }
  }

  public WLength getWidth() {
    return this.width_ != null ? this.width_ : WLength.Auto;
  }

  public WLength getHeight() {
    return this.height_ != null ? this.height_ : WLength.Auto;
  }

  public void setMinimumSize(final WLength width, final WLength height) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.minimumWidth_ = nonNegative(width);
    this.layoutImpl_.minimumHeight_ = nonNegative(height);
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  public WLength getMinimumWidth() {
    return this.layoutImpl_ != null ? this.layoutImpl_.minimumWidth_ : new WLength(0);
  }

  public WLength getMinimumHeight() {
    return this.layoutImpl_ != null ? this.layoutImpl_.minimumHeight_ : new WLength(0);
  }

  public void setMaximumSize(final WLength width, final WLength height) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.maximumWidth_ = nonNegative(width);
    this.layoutImpl_.maximumHeight_ = nonNegative(height);
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  public WLength getMaximumWidth() {
    return this.layoutImpl_ != null ? this.layoutImpl_.maximumWidth_ : WLength.Auto;
  }

  public WLength getMaximumHeight() {
    return this.layoutImpl_ != null ? this.layoutImpl_.maximumHeight_ : WLength.Auto;
  }

  public void setLineHeight(final WLength height) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.lineHeight_ = height;
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  public WLength getLineHeight() {
    return this.layoutImpl_ != null ? this.layoutImpl_.lineHeight_ : WLength.Auto;
  }

  public void setFloatSide(Side s) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.floatSide_ = s;
    this.flags_.set(BIT_FLOAT_SIDE_CHANGED);
    this.repaint();
  }

  public Side getFloatSide() {
    if (this.layoutImpl_ != null) {
      return this.layoutImpl_.floatSide_;
    } else {
      return null;
    }
  }

  public void setClearSides(EnumSet<Side> sides) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.clearSides_ = EnumSet.copyOf(sides);
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint();
  }

  public EnumSet<Side> getClearSides() {
    if (this.layoutImpl_ != null) {
      return this.layoutImpl_.clearSides_;
    } else {
      return EnumSet.noneOf(Side.class);
    }
  }

  public void setMargin(final WLength margin, EnumSet<Side> sides) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    if (sides.contains(Side.Top)) {
      this.layoutImpl_.margin_[0] = margin;
    }
    if (sides.contains(Side.Right)) {
      this.layoutImpl_.margin_[1] = margin;
    }
    if (sides.contains(Side.Bottom)) {
      this.layoutImpl_.margin_[2] = margin;
    }
    if (sides.contains(Side.Left)) {
      this.layoutImpl_.margin_[3] = margin;
    }
    this.flags_.set(BIT_MARGINS_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  public WLength getMargin(Side side) {
    if (!(this.layoutImpl_ != null)) {
      return new WLength(0);
    }
    switch (side) {
      case Top:
        return this.layoutImpl_.margin_[0];
      case Right:
        return this.layoutImpl_.margin_[1];
      case Bottom:
        return this.layoutImpl_.margin_[2];
      case Left:
        return this.layoutImpl_.margin_[3];
      default:
        logger.error(
            new StringWriter()
                .append("margin(Side) with invalid side: ")
                .append(String.valueOf((int) side.getValue()))
                .toString());
        return new WLength();
    }
  }

  public void setHiddenKeepsGeometry(boolean enabled) {
    this.flags_.set(BIT_DONOT_STUB);
    this.flags_.set(BIT_HIDE_WITH_VISIBILITY, enabled);
    this.flags_.set(BIT_HIDDEN_CHANGED);
  }

  public boolean isHiddenKeepsGeometry() {
    return this.flags_.get(BIT_HIDE_WITH_VISIBILITY) && !this.flags_.get(BIT_HIDE_WITH_OFFSETS);
  }

  public void setHidden(boolean hidden, final WAnimation animation) {
    if (canOptimizeUpdates() && (animation.isEmpty() && hidden == this.isHidden())) {
      return;
    }
    boolean wasVisible = this.isVisible();
    this.flags_.set(BIT_HIDDEN, hidden);
    this.flags_.set(BIT_HIDDEN_CHANGED);
    if (!animation.isEmpty()
        && WApplication.getInstance().getEnvironment().supportsCss3Animations()
        && WApplication.getInstance().getEnvironment().hasAjax()) {
      if (!(this.transientImpl_ != null)) {
        this.transientImpl_ = new WWebWidget.TransientImpl();
      }
      this.transientImpl_.animation_ = animation;
    }
    boolean shouldBeVisible = !hidden;
    if (shouldBeVisible && this.getParent() != null) {
      shouldBeVisible = this.getParent().isVisible();
    }
    if (!canOptimizeUpdates() || shouldBeVisible != wasVisible) {
      this.propagateSetVisible(shouldBeVisible);
    }
    WApplication.getInstance().getSession().getRenderer().updateFormObjects(this, true);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  public boolean isHidden() {
    return this.flags_.get(BIT_HIDDEN);
  }

  public boolean isVisible() {
    if (this.flags_.get(BIT_STUBBED) || this.flags_.get(BIT_HIDDEN)) {
      return false;
    } else {
      if (this.getParent() != null) {
        return this.getParent().isVisible();
      } else {
        return this == WApplication.getInstance().getDomRoot()
            || this == WApplication.getInstance().getDomRoot2();
      }
    }
  }

  public void setDisabled(boolean disabled) {
    if (canOptimizeUpdates()
        && disabled == this.flags_.get(BIT_DISABLED)
        && !this.flags_.get(BIT_PARENT_CHANGED)) {
      return;
    }
    boolean wasEnabled = this.isEnabled();
    this.flags_.set(BIT_DISABLED, disabled);
    this.flags_.set(BIT_DISABLED_CHANGED);
    boolean shouldBeEnabled = !disabled;
    if (shouldBeEnabled && this.getParent() != null) {
      shouldBeEnabled = this.getParent().isEnabled();
    }
    if (shouldBeEnabled != wasEnabled) {
      this.propagateSetEnabled(shouldBeEnabled);
    }
    WApplication.getInstance().getSession().getRenderer().updateFormObjects(this, true);
    this.repaint();
  }

  public boolean isDisabled() {
    return this.flags_.get(BIT_DISABLED);
  }

  public boolean isEnabled() {
    if (this.isDisabled()) {
      return false;
    } else {
      if (this.getParent() != null) {
        return this.getParent().isEnabled();
      } else {
        return true;
      }
    }
  }

  public void setPopup(boolean popup) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.zIndex_ = popup ? -1 : 0;
    if (popup && this.getParent() != null) {
      this.calcZIndex();
    }
    this.flags_.set(BIT_ZINDEX_CHANGED);
    this.repaint();
  }

  public boolean isPopup() {
    return this.layoutImpl_ != null ? this.layoutImpl_.zIndex_ != 0 : false;
  }

  public void setInline(boolean inl) {
    this.flags_.set(BIT_INLINE, inl);
    // this.resetLearnedSlot(WWidget.show);
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint();
  }

  public boolean isInline() {
    return this.flags_.get(BIT_INLINE);
  }

  public void setDecorationStyle(final WCssDecorationStyle style) {
    if (!(this.lookImpl_ != null)) {
      this.lookImpl_ = new WWebWidget.LookImpl(this);
    }
    this.lookImpl_.decorationStyle_ = style;
  }

  public WCssDecorationStyle getDecorationStyle() {
    if (!(this.lookImpl_ != null)) {
      this.lookImpl_ = new WWebWidget.LookImpl(this);
    }
    if (!(this.lookImpl_.decorationStyle_ != null)) {
      this.lookImpl_.decorationStyle_ = new WCssDecorationStyle();
      this.lookImpl_.decorationStyle_.setWebWidget(this);
    }
    return this.lookImpl_.decorationStyle_;
  }

  public void setStyleClass(final String styleClass) {
    if (canOptimizeUpdates() && styleClass.equals(this.getStyleClass())) {
      return;
    }
    if (!(this.lookImpl_ != null)) {
      this.lookImpl_ = new WWebWidget.LookImpl(this);
    }
    this.lookImpl_.styleClass_ = styleClass;
    this.flags_.set(BIT_STYLECLASS_CHANGED);
    this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
  }

  public String getStyleClass() {
    return this.lookImpl_ != null ? this.lookImpl_.styleClass_ : "";
  }

  public void addStyleClass(final String styleClass, boolean force) {
    if (!(this.lookImpl_ != null)) {
      this.lookImpl_ = new WWebWidget.LookImpl(this);
    }
    String currentClass = this.lookImpl_.styleClass_;
    Set<String> classes = new HashSet<String>();
    StringUtils.split(classes, currentClass, " ", true);
    if (classes.contains(styleClass) == false) {
      this.lookImpl_.styleClass_ = StringUtils.addWord(this.lookImpl_.styleClass_, styleClass);
      if (!force) {
        this.flags_.set(BIT_STYLECLASS_CHANGED);
        this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
      }
    }
    if (force && this.isRendered()) {
      if (!(this.transientImpl_ != null)) {
        this.transientImpl_ = new WWebWidget.TransientImpl();
      }
      CollectionUtils.add(this.transientImpl_.addedStyleClasses_, styleClass);
      this.transientImpl_.removedStyleClasses_.remove(styleClass);
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
  }

  public void removeStyleClass(final String styleClass, boolean force) {
    if (!(this.lookImpl_ != null)) {
      this.lookImpl_ = new WWebWidget.LookImpl(this);
    }
    if (this.hasStyleClass(styleClass)) {
      this.lookImpl_.styleClass_ = StringUtils.eraseWord(this.lookImpl_.styleClass_, styleClass);
      if (!force) {
        this.flags_.set(BIT_STYLECLASS_CHANGED);
        this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
      }
    }
    if (force && this.isRendered()) {
      if (!(this.transientImpl_ != null)) {
        this.transientImpl_ = new WWebWidget.TransientImpl();
      }
      CollectionUtils.add(this.transientImpl_.removedStyleClasses_, styleClass);
      this.transientImpl_.addedStyleClasses_.remove(styleClass);
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
  }

  public boolean hasStyleClass(final String styleClass) {
    if (!(this.lookImpl_ != null)) {
      return false;
    }
    String currentClass = this.lookImpl_.styleClass_;
    Set<String> classes = new HashSet<String>();
    StringUtils.split(classes, currentClass, " ", true);
    return classes.contains(styleClass) != false;
  }

  public void setVerticalAlignment(AlignmentFlag alignment, final WLength length) {
    if (AlignmentFlag.AlignHorizontalMask.contains(alignment)) {
      logger.error(
          new StringWriter()
              .append("setVerticalAlignment(): alignment ")
              .append(String.valueOf(alignment.getValue()))
              .append(" is not vertical")
              .toString());
    }
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.verticalAlignment_ = alignment;
    this.layoutImpl_.verticalAlignmentLength_ = length;
    this.flags_.set(BIT_GEOMETRY_CHANGED);
    this.repaint();
  }

  public AlignmentFlag getVerticalAlignment() {
    return this.layoutImpl_ != null ? this.layoutImpl_.verticalAlignment_ : AlignmentFlag.Baseline;
  }

  public WLength getVerticalAlignmentLength() {
    return this.layoutImpl_ != null ? this.layoutImpl_.verticalAlignmentLength_ : WLength.Auto;
  }

  public void setToolTip(final CharSequence text, TextFormat textFormat) {
    this.flags_.clear(BIT_TOOLTIP_DEFERRED);
    if (canOptimizeUpdates() && (text.toString().equals(this.getStoredToolTip().toString()))) {
      return;
    }
    if (!(this.lookImpl_ != null)) {
      this.lookImpl_ = new WWebWidget.LookImpl(this);
    }
    if (!(this.lookImpl_.toolTip_ != null)) {
      this.lookImpl_.toolTip_ = new WString();
    }
    this.lookImpl_.toolTip_ = WString.toWString(text);
    this.lookImpl_.toolTipTextFormat_ = textFormat;
    this.flags_.set(BIT_TOOLTIP_CHANGED);
    this.repaint();
  }

  public void setDeferredToolTip(boolean enable, TextFormat textFormat) {
    this.flags_.set(BIT_TOOLTIP_DEFERRED, enable);
    if (!enable) {
      this.setToolTip("", textFormat);
    } else {
      if (!(this.lookImpl_ != null)) {
        this.lookImpl_ = new WWebWidget.LookImpl(this);
      }
      if (!(this.lookImpl_.toolTip_ != null)) {
        this.lookImpl_.toolTip_ = new WString();
      } else {
        this.lookImpl_.toolTip_ = new WString();
      }
      this.lookImpl_.toolTipTextFormat_ = textFormat;
      this.flags_.set(BIT_TOOLTIP_CHANGED);
      this.repaint();
    }
  }

  public WString getToolTip() {
    return this.getStoredToolTip();
  }

  public void refresh() {
    if (this.lookImpl_ != null && this.lookImpl_.toolTip_ != null) {
      if (this.lookImpl_.toolTip_.refresh()) {
        this.flags_.set(BIT_TOOLTIP_CHANGED);
        this.repaint();
      }
    }
    this.iterateChildren(
        (WWidget c) -> {
          c.refresh();
        });
    super.refresh();
  }

  public void setAttributeValue(final String name, final String value) {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    if (!(this.otherImpl_.attributes_ != null)) {
      this.otherImpl_.attributes_ = new HashMap<String, String>();
    }
    String i = this.otherImpl_.attributes_.get(name);
    if (i != null && i.equals(value)) {
      return;
    }
    this.otherImpl_.attributes_.put(name, value);
    if (!(this.transientImpl_ != null)) {
      this.transientImpl_ = new WWebWidget.TransientImpl();
    }
    this.transientImpl_.attributesSet_.add(name);
    this.repaint();
  }

  public String getAttributeValue(final String name) {
    if (this.otherImpl_ != null && this.otherImpl_.attributes_ != null) {
      String i = this.otherImpl_.attributes_.get(name);
      if (i != null) {
        return i;
      }
    }
    return "";
  }

  public void setJavaScriptMember(final String name, final String value) {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    if (!(this.otherImpl_.jsMembers_ != null)) {
      this.otherImpl_.jsMembers_ = new ArrayList<WWebWidget.OtherImpl.Member>();
    }
    final List<WWebWidget.OtherImpl.Member> members = this.otherImpl_.jsMembers_;
    int index = this.indexOfJavaScriptMember(name);
    String terminatedValue = value;
    if (terminatedValue.length() != 0
        && terminatedValue.charAt(terminatedValue.length() - 1) != ';'
        && !terminatedValue.equals("0")) {
      terminatedValue += ";";
    }
    if (index != -1 && members.get(index).value.equals(terminatedValue)) {
      return;
    }
    if (value.length() == 0) {
      if (index != -1) {
        members.remove(0 + index);
      } else {
        return;
      }
    } else {
      if (index == -1) {
        WWebWidget.OtherImpl.Member m = new WWebWidget.OtherImpl.Member();
        m.name = name;
        m.value = terminatedValue;
        members.add(m);
      } else {
        members.get(index).value = terminatedValue;
      }
    }
    this.addJavaScriptStatement(WWebWidget.JavaScriptStatementType.SetMember, name);
    this.repaint();
  }

  public String getJavaScriptMember(final String name) {
    int index = this.indexOfJavaScriptMember(name);
    if (index != -1) {
      return this.otherImpl_.jsMembers_.get(index).value;
    } else {
      return "";
    }
  }

  public void callJavaScriptMember(final String name, final String args) {
    this.addJavaScriptStatement(
        WWebWidget.JavaScriptStatementType.CallMethod, name + "(" + args + ");");
    this.repaint();
  }

  public void load() {
    this.flags_.set(BIT_LOADED);
    this.iterateChildren(
        (WWidget c) -> {
          WWebWidget.this.doLoad(c);
        });
    if (this.flags_.get(BIT_HIDE_WITH_OFFSETS)) {
      this.getParent().setHideWithOffsets(true);
    }
  }

  public boolean isLoaded() {
    return this.flags_.get(BIT_LOADED);
  }

  int getZIndex() {
    if (this.layoutImpl_ != null) {
      return this.layoutImpl_.zIndex_;
    } else {
      return 0;
    }
  }

  public void setId(final String id) {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    WApplication app = WApplication.getInstance();
    for (int i = 0; i < this.jsignals_.size(); ++i) {
      AbstractEventSignal signal = this.jsignals_.get(i);
      if (signal.isExposedSignal()) {
        app.removeExposedSignal(signal);
      }
    }
    if (!(this.id_ != null)) {
      this.id_ = "";
    }
    this.id_ = id;
    for (int i = 0; i < this.jsignals_.size(); ++i) {
      AbstractEventSignal signal = this.jsignals_.get(i);
      if (signal.isExposedSignal()) {
        app.addExposedSignal(signal);
      }
    }
  }

  public WWidget find(final String name) {
    if (this.getObjectName().equals(name)) {
      return this;
    } else {
      final WWidget[] result = new WWidget[1];
      result[0] = null;
      this.iterateChildren(
          (WWidget c) -> {
            if (!(result[0] != null)) {
              result[0] = c.find(name);
            }
          });
      return result[0];
    }
  }

  public WWidget findById(final String id) {
    if (this.getId().equals(id)) {
      return this;
    } else {
      final WWidget[] result = new WWidget[1];
      result[0] = null;
      this.iterateChildren(
          (WWidget c) -> {
            if (!(result[0] != null)) {
              result[0] = c.findById(id);
            }
          });
      if (result[0] != null) {
        return result[0];
      }
    }
    return null;
  }

  public void setSelectable(boolean selectable) {
    this.flags_.set(BIT_SET_SELECTABLE, selectable);
    this.flags_.set(BIT_SET_UNSELECTABLE, !selectable);
    this.flags_.set(BIT_SELECTABLE_CHANGED);
    this.repaint();
  }

  public void doJavaScript(final String javascript) {
    this.addJavaScriptStatement(WWebWidget.JavaScriptStatementType.Statement, javascript);
    this.repaint();
  }

  public String getId() {
    if (this.id_ != null) {
      return this.id_;
    } else {
      return super.getId();
    }
  }
  /**
   * Create DOM element for widget.
   *
   * <p>This is an internal function, and should not be called directly, or be overridden!
   */
  protected DomElement createDomElement(WApplication app) {
    this.setRendered(true);
    DomElement result;
    if (this.otherImpl_ != null && this.otherImpl_.elementTagName_ != null) {
      result = DomElement.createNew(DomElementType.OTHER);
      result.setDomElementTagName(this.otherImpl_.elementTagName_);
    } else {
      result = DomElement.createNew(this.getDomElementType());
    }
    this.setId(result, app);
    this.updateDom(result, true);
    return result;
  }
  /**
   * Get DOM changes for this widget.
   *
   * <p>This is an internal function, and should not be called directly, or be overridden!
   */
  protected void getDomChanges(final List<DomElement> result, WApplication app) {
    DomElement e = DomElement.getForUpdate(this, this.getDomElementType());
    this.updateDom(e, false);
    result.add(e);
  }

  abstract DomElementType getDomElementType();

  DomElement createStubElement(WApplication app) {
    this.propagateRenderOk();
    this.flags_.set(BIT_STUBBED);
    DomElement stub = DomElement.createNew(DomElementType.SPAN);
    if (!this.flags_.get(BIT_HIDE_WITH_OFFSETS)) {
      stub.setProperty(Property.StyleDisplay, "none");
    } else {
      stub.setProperty(Property.StylePosition, "absolute");
      stub.setProperty(Property.StyleLeft, "-10000px");
      stub.setProperty(Property.StyleTop, "-10000px");
      stub.setProperty(Property.StyleVisibility, "hidden");
    }
    if (app.getEnvironment().hasJavaScript()) {
      stub.setProperty(Property.InnerHTML, "...");
    }
    if (!app.getEnvironment().agentIsSpiderBot() || this.id_ != null) {
      stub.setId(this.getId());
    }
    return stub;
  }

  DomElement createActualElement(WWidget self, WApplication app) {
    this.flags_.clear(BIT_STUBBED);
    DomElement result = this.createDomElement(app);
    app.getTheme().apply(self, result, ElementThemeRole.MainElement);
    String styleClass = result.getProperty(Property.Class);
    if (styleClass.length() != 0) {
      if (!(this.lookImpl_ != null)) {
        this.lookImpl_ = new WWebWidget.LookImpl(this);
      }
      this.lookImpl_.styleClass_ = styleClass;
    }
    return result;
  }
  /**
   * Change the way the widget is loaded when invisible.
   *
   * <p>By default, invisible widgets are loaded only after visible content. For tiny widgets this
   * may lead to a performance loss, instead of the expected increase, because they require many
   * more DOM manipulations to render, reducing the overall responsiveness of the application.
   *
   * <p>Therefore, this is disabled for some widgets like {@link WImage}, or empty
   * WContainerWidgets.
   *
   * <p>You may also want to disable deferred loading when JavaScript event handling expects the
   * widget to be loaded.
   *
   * <p>Usually the default settings are fine, but you may want to change the behaviour.
   *
   * <p>
   *
   * @see WApplication#setTwoPhaseRenderingThreshold(int bytes)
   */
  public void setLoadLaterWhenInvisible(boolean how) {
    this.flags_.set(BIT_DONOT_STUB, !how);
  }
  /**
   * returns the current html tag name
   *
   * <p>
   *
   * @see WWebWidget#setHtmlTagName(String tag)
   */
  public String getHtmlTagName() {
    if (this.otherImpl_ != null && this.otherImpl_.elementTagName_ != null) {
      return this.otherImpl_.elementTagName_;
    } else {
      DomElementType type = this.getDomElementType();
      return DomElement.tagName(type);
    }
  }
  /**
   * set the custom HTML tag name
   *
   * <p>The custom tag will replace the actual tag. The tag is not tested to see if it is a valid
   * one and a closing tag will always be added.
   *
   * <p>
   *
   * @see WWebWidget#getHtmlTagName()
   */
  public void setHtmlTagName(final String tag) {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    if (!(this.otherImpl_.elementTagName_ != null)) {
      this.otherImpl_.elementTagName_ = "";
    }
    this.otherImpl_.elementTagName_ = tag;
  }

  public static WString escapeText(final CharSequence text, boolean newlinestoo) {
    String result = text.toString();
    result = escapeText(result, newlinestoo);
    return new WString(result);
  }

  public static final WString escapeText(final CharSequence text) {
    return escapeText(text, false);
  }

  public static String escapeText(final String text, boolean newlinestoo) {
    EscapeOStream sout = new EscapeOStream();
    if (newlinestoo) {
      sout.pushEscape(EscapeOStream.RuleSet.PlainTextNewLines);
    } else {
      sout.pushEscape(EscapeOStream.RuleSet.Plain);
    }
    StringUtils.sanitizeUnicode(sout, text);
    return sout.toString();
  }

  public static final String escapeText(final String text) {
    return escapeText(text, false);
  }

  public static String unescapeText(final String text) {
    return XHtmlFilter.htmlEntityDecode(text);
  }

  public static boolean removeScript(final CharSequence text) {
    return XSSFilter.removeScript(text);
  }
  /**
   * Turn a CharEncoding::UTF8 encoded string into a JavaScript string literal.
   *
   * <p>The <code>delimiter</code> may be a single or double quote.
   */
  public static String jsStringLiteral(final String value, char delimiter) {
    StringBuilder result = new StringBuilder();
    DomElement.jsStringLiteral(result, value, delimiter);
    return result.toString();
  }
  /**
   * Turn a CharEncoding::UTF8 encoded string into a JavaScript string literal.
   *
   * <p>Returns {@link #jsStringLiteral(String value, char delimiter) jsStringLiteral(value, '\'')}
   */
  public static final String jsStringLiteral(final String value) {
    return jsStringLiteral(value, '\'');
  }

  static String jsStringLiteral(final CharSequence value, char delimiter) {
    return WString.toWString(value).getJsStringLiteral(delimiter);
  }

  static final String jsStringLiteral(final CharSequence value) {
    return jsStringLiteral(value, '\'');
  }

  static String resolveRelativeUrl(final String url) {
    return WApplication.getInstance().resolveRelativeUrl(url);
  }

  void setFormObject(boolean how) {
    this.flags_.set(BIT_FORM_OBJECT, how);
    WApplication.getInstance().getSession().getRenderer().updateFormObjects(this, false);
  }

  static boolean canOptimizeUpdates() {
    return !WApplication.getInstance().getSession().getRenderer().isPreLearning();
  }

  void setZIndex(int zIndex) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.zIndex_ = zIndex;
    this.flags_.set(BIT_ZINDEX_CHANGED);
    this.repaint();
  }

  public boolean isRendered() {
    return this.flags_.get(WWebWidget.BIT_RENDERED);
  }

  public void setCanReceiveFocus(boolean enabled) {
    this.setTabIndex(enabled ? 0 : Integer.MIN_VALUE);
  }

  public boolean isCanReceiveFocus() {
    if (this.otherImpl_ != null) {
      return this.otherImpl_.tabIndex_ != Integer.MIN_VALUE;
    } else {
      return false;
    }
  }

  public boolean isSetFirstFocus() {
    if (this.isVisible() && this.isEnabled()) {
      if (this.isCanReceiveFocus()) {
        this.setFocus(true);
        return true;
      }
      final boolean[] result = new boolean[1];
      result[0] = false;
      this.iterateChildren(
          (WWidget w) -> {
            if (!result[0]) {
              result[0] = w.isSetFirstFocus();
            }
          });
      if (result[0]) {
        return true;
      }
      return false;
    } else {
      return false;
    }
  }

  public void setFocus(boolean focus) {
    this.flags_.set(BIT_GOT_FOCUS, focus);
    this.repaint();
    WApplication app = WApplication.getInstance();
    if (focus) {
      app.setFocus(this.getId(), -1, -1);
    } else {
      if (app.getFocus().equals(this.getId())) {
        app.setFocus("", -1, -1);
      }
    }
  }

  public boolean hasFocus() {
    return WApplication.getInstance().getFocus().equals(this.getId());
  }

  public void setTabIndex(int index) {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    this.otherImpl_.tabIndex_ = index;
    this.flags_.set(BIT_TABINDEX_CHANGED);
    this.repaint();
  }

  public int getTabIndex() {
    if (!(this.otherImpl_ != null)) {
      return this.isCanReceiveFocus() ? 0 : Integer.MIN_VALUE;
    } else {
      return this.otherImpl_.tabIndex_;
    }
  }
  /**
   * Signal emitted when the widget lost focus.
   *
   * <p>This signals is only emitted for a widget that {@link WWebWidget#isCanReceiveFocus()
   * isCanReceiveFocus()}
   */
  public EventSignal blurred() {
    return this.voidEventSignal(BLUR_SIGNAL, true);
  }
  /**
   * Signal emitted when the widget recieved focus.
   *
   * <p>This signals is only emitted for a widget that {@link WWebWidget#isCanReceiveFocus()
   * isCanReceiveFocus()}
   */
  public EventSignal focussed() {
    return this.voidEventSignal(FOCUS_SIGNAL, true);
  }

  public boolean isScrollVisibilityEnabled() {
    return this.flags_.get(BIT_SCROLL_VISIBILITY_ENABLED);
  }

  public void setScrollVisibilityEnabled(boolean enabled) {
    if (enabled) {
      if (!(this.otherImpl_ != null)) {
        this.otherImpl_ = new WWebWidget.OtherImpl(this);
      }
      if (!(this.otherImpl_.jsScrollVisibilityChanged_ != null)) {
        this.otherImpl_.jsScrollVisibilityChanged_ =
            new JSignal1<Boolean>(this, "scrollVisibilityChanged") {};
        this.otherImpl_.jsScrollVisibilityChanged_.addListener(
            this,
            (Boolean e1) -> {
              WWebWidget.this.jsScrollVisibilityChanged(e1);
            });
      }
    }
    if (this.isScrollVisibilityEnabled() != enabled) {
      this.flags_.set(BIT_SCROLL_VISIBILITY_ENABLED, enabled);
      this.flags_.set(BIT_SCROLL_VISIBILITY_CHANGED);
      this.repaint();
    }
  }

  public int getScrollVisibilityMargin() {
    if (!(this.otherImpl_ != null)) {
      return 0;
    } else {
      return this.otherImpl_.scrollVisibilityMargin_;
    }
  }

  public void setScrollVisibilityMargin(int margin) {
    if (this.getScrollVisibilityMargin() != margin) {
      if (!(this.otherImpl_ != null)) {
        this.otherImpl_ = new WWebWidget.OtherImpl(this);
      }
      this.otherImpl_.scrollVisibilityMargin_ = margin;
      if (this.isScrollVisibilityEnabled()) {
        this.flags_.set(BIT_SCROLL_VISIBILITY_CHANGED);
        this.repaint();
      }
    }
  }

  public Signal1<Boolean> scrollVisibilityChanged() {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    return this.otherImpl_.scrollVisibilityChanged_;
  }

  public boolean isScrollVisible() {
    return this.flags_.get(BIT_IS_SCROLL_VISIBLE);
  }

  public void setThemeStyleEnabled(boolean enabled) {
    this.flags_.set(BIT_THEME_STYLE_DISABLED, !enabled);
  }

  public boolean isThemeStyleEnabled() {
    return !this.flags_.get(BIT_THEME_STYLE_DISABLED);
  }

  public void setObjectName(final String name) {
    if (!this.getObjectName().equals(name)) {
      super.setObjectName(name);
      this.flags_.set(BIT_OBJECT_NAME_CHANGED);
      this.repaint();
    }
  }

  public int getBaseZIndex() {
    if (!(this.layoutImpl_ != null)) {
      return DEFAULT_BASE_Z_INDEX;
    } else {
      return this.layoutImpl_.baseZIndex_;
    }
  }

  public void setBaseZIndex(int zIndex) {
    if (!(this.layoutImpl_ != null)) {
      this.layoutImpl_ = new WWebWidget.LayoutImpl();
    }
    this.layoutImpl_.baseZIndex_ = zIndex;
  }

  void repaint(EnumSet<RepaintFlag> flags) {
    if (this.isStubbed()) {
      final WebRenderer renderer = WApplication.getInstance().getSession().getRenderer();
      if (renderer.isPreLearning()) {
        renderer.learningIncomplete();
      }
    }
    if (!this.flags_.get(BIT_RENDERED)) {
      return;
    }
    super.scheduleRerender(false, flags);
    if (flags.contains(RepaintFlag.ToAjax)) {
      this.flags_.set(BIT_REPAINT_TO_AJAX);
    }
  }

  final void repaint(RepaintFlag flag, RepaintFlag... flags) {
    repaint(EnumSet.of(flag, flags));
  }

  final void repaint() {
    repaint(EnumSet.noneOf(RepaintFlag.class));
  }

  protected void iterateChildren(final HandleWidgetMethod method) {}

  void getFormObjects(final Map<String, WObject> formObjects) {
    if (this.flags_.get(BIT_FORM_OBJECT)) {
      formObjects.put(this.getId(), this);
    }
    this.iterateChildren(
        (WWidget c) -> {
          c.getWebWidget().getSFormObjects(formObjects);
        });
  }

  void doneRerender() {
    this.iterateChildren(
        (WWidget c) -> {
          c.getWebWidget().doneRerender();
        });
  }

  void updateDom(final DomElement element, boolean all) {
    WApplication app = null;
    if (this.flags_.get(BIT_GEOMETRY_CHANGED)
        || this.flags_.get(BIT_FLEX_BOX_CHANGED)
        || !this.flags_.get(BIT_HIDE_WITH_VISIBILITY) && this.flags_.get(BIT_HIDDEN_CHANGED)
        || all) {
      if (this.flags_.get(BIT_HIDE_WITH_VISIBILITY) || !this.flags_.get(BIT_HIDDEN)) {
        String Inline = "inline";
        String InlineTable = "inline-table";
        String InlineBlock = "inline-block";
        String Block = "block";
        String Flex = "flex";
        String InlineFlex = "inline-flex";
        String Empty = "";
        String display = null;
        final boolean defaultInline =
            element.getType() == DomElementType.OTHER
                ? DomElement.isDefaultInline(this.getDomElementType())
                : element.isDefaultInline();
        if (defaultInline != this.flags_.get(BIT_INLINE)) {
          if (this.flags_.get(BIT_INLINE)) {
            if (element.getType() == DomElementType.TABLE) {
              display = InlineTable;
            }
            if (element.getType() == DomElementType.LI) {
              display = Inline;
            } else {
              if (element.getType() != DomElementType.TD) {
                if (!(app != null)) {
                  app = WApplication.getInstance();
                }
                if (app.getEnvironment().agentIsIElt(9)) {
                  display = Inline;
                  element.setProperty(Property.StyleZoom, "1");
                } else {
                  display = InlineBlock;
                }
              }
            }
          } else {
            display = Block;
          }
        } else {
          if (!all && this.flags_.get(BIT_HIDDEN_CHANGED)) {
            if (defaultInline == this.flags_.get(BIT_INLINE)) {
              display = Empty;
            } else {
              display = this.flags_.get(BIT_INLINE) ? Inline : Block;
            }
          }
        }
        if (this.flags_.get(BIT_FLEX_BOX)) {
          display = this.flags_.get(BIT_INLINE) ? InlineFlex : Flex;
        } else {
          if (this.flags_.get(BIT_FLEX_BOX_CHANGED) && !(display != null)) {
            display = Empty;
          }
        }
        if (display != null) {
          element.setProperty(Property.StyleDisplay, display);
          if (element.getType() == DomElementType.FIELDSET) {
            element.setProperty(Property.StyleFlexDirection, "column");
          }
        }
      } else {
        element.setProperty(Property.StyleDisplay, "none");
      }
    }
    if (this.flags_.get(BIT_ZINDEX_CHANGED) || all) {
      if (this.layoutImpl_ != null) {
        if (this.layoutImpl_.zIndex_ > 0) {
          element.setProperty(Property.StyleZIndex, String.valueOf(this.layoutImpl_.zIndex_));
          element.addPropertyWord(Property.Class, "Wt-popup");
          if (!all
              && !this.flags_.get(BIT_STYLECLASS_CHANGED)
              && this.lookImpl_ != null
              && this.lookImpl_.styleClass_.length() != 0) {
            element.addPropertyWord(Property.Class, this.lookImpl_.styleClass_);
          }
          if (!(app != null)) {
            app = WApplication.getInstance();
          }
          if (all
              && app.getEnvironment().getAgent() == UserAgent.IE6
              && element.getType() == DomElementType.DIV) {
            DomElement i = DomElement.createNew(DomElementType.IFRAME);
            i.setId("sh" + this.getId());
            i.setProperty(Property.Class, "Wt-shim");
            i.setProperty(Property.Src, "javascript:false;");
            i.setAttribute("title", "Popup Shim");
            i.setAttribute("tabindex", "-1");
            i.setAttribute("frameborder", "0");
            app.addAutoJavaScript(
                "{var w = "
                    + this.getJsRef()
                    + ";if (w && !Wt4.11.2.isHidden(w)) {var i = Wt4.11.2.getElement('"
                    + i.getId()
                    + "');i.style.width=w.clientWidth + 'px';i.style.height=w.clientHeight + 'px';}}");
            element.addChild(i);
          }
        }
      }
      this.flags_.clear(BIT_ZINDEX_CHANGED);
    }
    if (this.flags_.get(BIT_GEOMETRY_CHANGED) || all) {
      if (this.layoutImpl_ != null) {
        if (!(this.flags_.get(BIT_HIDE_WITH_VISIBILITY) && this.flags_.get(BIT_HIDDEN))) {
          switch (this.layoutImpl_.positionScheme_) {
            case Static:
              break;
            case Relative:
              element.setProperty(Property.StylePosition, "relative");
              break;
            case Absolute:
              element.setProperty(Property.StylePosition, "absolute");
              break;
            case Fixed:
              element.setProperty(Property.StylePosition, "fixed");
              break;
          }
        }
        if (this.layoutImpl_.clearSides_.equals(Side.Left)) {
          element.setProperty(Property.StyleClear, "left");
        } else {
          if (this.layoutImpl_.clearSides_.equals(Side.Right)) {
            element.setProperty(Property.StyleClear, "right");
          } else {
            if (this.layoutImpl_.clearSides_.equals(EnumSet.of(Side.Left, Side.Right))) {
              element.setProperty(Property.StyleClear, "both");
            }
          }
        }
        if (this.layoutImpl_.minimumWidth_.getValue() != 0) {
          String text =
              this.layoutImpl_.minimumWidth_.isAuto()
                  ? "0px"
                  : this.layoutImpl_.minimumWidth_.getCssText();
          element.setProperty(Property.StyleMinWidth, text);
        }
        if (this.layoutImpl_.minimumHeight_.getValue() != 0) {
          String text =
              this.layoutImpl_.minimumHeight_.isAuto()
                  ? "0px"
                  : this.layoutImpl_.minimumHeight_.getCssText();
          element.setProperty(Property.StyleMinHeight, text);
        }
        if (!this.layoutImpl_.maximumWidth_.isAuto()) {
          element.setProperty(Property.StyleMaxWidth, this.layoutImpl_.maximumWidth_.getCssText());
        }
        if (!this.layoutImpl_.maximumHeight_.isAuto()) {
          element.setProperty(
              Property.StyleMaxHeight, this.layoutImpl_.maximumHeight_.getCssText());
        }
        if (this.layoutImpl_.positionScheme_ != PositionScheme.Static) {
          if (!this.layoutImpl_.offsets_[0].isAuto()
              || !this.layoutImpl_.offsets_[1].isAuto()
              || !this.layoutImpl_.offsets_[2].isAuto()
              || !this.layoutImpl_.offsets_[3].isAuto()) {
            for (int i = 0; i < 4; ++i) {
              Property property = properties[i];
              if (!(app != null)) {
                app = WApplication.getInstance();
              }
              if (app.getLayoutDirection() == LayoutDirection.RightToLeft) {
                if (i == 1) {
                  property = properties[3];
                } else {
                  if (i == 3) {
                    property = properties[1];
                  }
                }
              }
              if (app.getEnvironment().hasAjax() && !app.getEnvironment().agentIsIElt(9)
                  || !this.layoutImpl_.offsets_[i].isAuto()) {
                element.setProperty(property, this.layoutImpl_.offsets_[i].getCssText());
              }
            }
          }
        }
        switch (this.layoutImpl_.verticalAlignment_) {
          case Baseline:
            break;
          case Sub:
            element.setProperty(Property.StyleVerticalAlign, "sub");
            break;
          case Super:
            element.setProperty(Property.StyleVerticalAlign, "super");
            break;
          case Top:
            element.setProperty(Property.StyleVerticalAlign, "top");
            break;
          case TextTop:
            element.setProperty(Property.StyleVerticalAlign, "text-top");
            break;
          case Middle:
            element.setProperty(Property.StyleVerticalAlign, "middle");
            break;
          case Bottom:
            element.setProperty(Property.StyleVerticalAlign, "bottom");
            break;
          case TextBottom:
            element.setProperty(Property.StyleVerticalAlign, "text-bottom");
            break;
          default:
            break;
        }
        if (!this.layoutImpl_.lineHeight_.isAuto()) {
          element.setProperty(Property.StyleLineHeight, this.layoutImpl_.lineHeight_.getCssText());
        }
      }
      this.flags_.clear(BIT_GEOMETRY_CHANGED);
    }
    if (this.width_ != null && (this.flags_.get(BIT_WIDTH_CHANGED) || all)) {
      if (!all || !this.width_.isAuto()) {
        element.setProperty(Property.StyleWidth, this.width_.getCssText());
      }
      this.flags_.clear(BIT_WIDTH_CHANGED);
    }
    if (this.height_ != null && (this.flags_.get(BIT_HEIGHT_CHANGED) || all)) {
      if (!all || !this.height_.isAuto()) {
        element.setProperty(Property.StyleHeight, this.height_.getCssText());
      }
      this.flags_.clear(BIT_HEIGHT_CHANGED);
    }
    if (this.flags_.get(BIT_FLOAT_SIDE_CHANGED) || all) {
      if (this.layoutImpl_ != null) {
        if (this.layoutImpl_.floatSide_ == null) {
          if (this.flags_.get(BIT_FLOAT_SIDE_CHANGED)) {
            element.setProperty(Property.StyleFloat, "none");
          }
        } else {
          if (!(app != null)) {
            app = WApplication.getInstance();
          }
          boolean ltr = app.getLayoutDirection() == LayoutDirection.LeftToRight;
          switch (this.layoutImpl_.floatSide_) {
            case Left:
              element.setProperty(Property.StyleFloat, ltr ? "left" : "right");
              break;
            case Right:
              element.setProperty(Property.StyleFloat, ltr ? "right" : "left");
              break;
            default:;
          }
        }
      }
      this.flags_.clear(BIT_FLOAT_SIDE_CHANGED);
    }
    if (this.layoutImpl_ != null) {
      boolean changed = this.flags_.get(BIT_MARGINS_CHANGED);
      if (changed || all) {
        if (changed || this.layoutImpl_.margin_[0].getValue() != 0) {
          element.setProperty(Property.StyleMarginTop, this.layoutImpl_.margin_[0].getCssText());
        }
        if (changed || this.layoutImpl_.margin_[1].getValue() != 0) {
          element.setProperty(Property.StyleMarginRight, this.layoutImpl_.margin_[1].getCssText());
        }
        if (changed || this.layoutImpl_.margin_[2].getValue() != 0) {
          element.setProperty(Property.StyleMarginBottom, this.layoutImpl_.margin_[2].getCssText());
        }
        if (changed || this.layoutImpl_.margin_[3].getValue() != 0) {
          element.setProperty(Property.StyleMarginLeft, this.layoutImpl_.margin_[3].getCssText());
        }
        this.flags_.clear(BIT_MARGINS_CHANGED);
      }
    }
    if (this.lookImpl_ != null) {
      if ((this.lookImpl_.toolTip_ != null || this.flags_.get(BIT_TOOLTIP_DEFERRED))
          && (this.flags_.get(BIT_TOOLTIP_CHANGED) || all)) {
        if (!all
            || (!(this.lookImpl_.toolTip_.length() == 0)
                || this.flags_.get(BIT_TOOLTIP_DEFERRED))) {
          if (!(app != null)) {
            app = WApplication.getInstance();
          }
          if ((this.lookImpl_.toolTipTextFormat_ != TextFormat.Plain
                  || this.flags_.get(BIT_TOOLTIP_DEFERRED))
              && app.getEnvironment().hasAjax()) {
            app.loadJavaScript("js/ToolTip.js", wtjs10());
            WString tooltipText = new WString(this.lookImpl_.toolTip_.toString());
            if (this.lookImpl_.toolTipTextFormat_ == TextFormat.Plain) {
              tooltipText = escapeText(this.lookImpl_.toolTip_);
            } else {
              if (this.lookImpl_.toolTipTextFormat_ == TextFormat.XHTML) {
                boolean res = removeScript(tooltipText);
                if (!res) {
                  tooltipText = escapeText(this.lookImpl_.toolTip_);
                }
              }
            }
            String deferred = this.flags_.get(BIT_TOOLTIP_DEFERRED) ? "true" : "false";
            element.callJavaScript(
                "Wt4.11.2.toolTip("
                    + app.getJavaScriptClass()
                    + ","
                    + jsStringLiteral(this.getId())
                    + ","
                    + WString.toWString(tooltipText).getJsStringLiteral()
                    + ", "
                    + deferred
                    + ", "
                    + jsStringLiteral(
                        app.getTheme().utilityCssClass(UtilityCssClassRole.ToolTipInner))
                    + ", "
                    + jsStringLiteral(
                        app.getTheme().utilityCssClass(UtilityCssClassRole.ToolTipOuter))
                    + ");");
            if (this.flags_.get(BIT_TOOLTIP_DEFERRED)
                && !this.lookImpl_.loadToolTip_.isConnected()) {
              this.lookImpl_.loadToolTip_.addListener(
                  this,
                  () -> {
                    WWebWidget.this.loadToolTip();
                  });
            }
            element.removeAttribute("title");
          } else {
            element.setAttribute("title", this.lookImpl_.toolTip_.toString());
          }
        }
        this.flags_.clear(BIT_TOOLTIP_CHANGED);
      }
      if (this.lookImpl_.decorationStyle_ != null) {
        this.lookImpl_.decorationStyle_.updateDomElement(element, all);
      }
      if (all || this.flags_.get(BIT_STYLECLASS_CHANGED)) {
        if (!all || this.lookImpl_.styleClass_.length() != 0) {
          element.addPropertyWord(Property.Class, this.lookImpl_.styleClass_);
        }
      }
      this.flags_.clear(BIT_STYLECLASS_CHANGED);
    }
    if (!all && this.transientImpl_ != null) {
      for (int i = 0; i < this.transientImpl_.addedStyleClasses_.size(); ++i) {
        element.callJavaScript(
            "Wt4.11.2.$('"
                + this.getId()
                + "').classList.add('"
                + this.transientImpl_.addedStyleClasses_.get(i)
                + "');");
      }
      for (int i = 0; i < this.transientImpl_.removedStyleClasses_.size(); ++i) {
        element.callJavaScript(
            "Wt4.11.2.$('"
                + this.getId()
                + "').classList.remove('"
                + this.transientImpl_.removedStyleClasses_.get(i)
                + "');");
      }
      if (!this.transientImpl_.childRemoveChanges_.isEmpty()) {
        if ((int) this.getChildren().size() != this.transientImpl_.addedChildren_
            || this.transientImpl_.specialChildRemove_) {
          for (int i = 0; i < this.transientImpl_.childRemoveChanges_.size(); ++i) {
            final String js = this.transientImpl_.childRemoveChanges_.get(i);
            if (js.charAt(0) == '_') {
              element.callJavaScript("Wt4.11.2.remove('" + js.substring(1) + "');", true);
            } else {
              element.callJavaScript(js, true);
            }
          }
        } else {
          element.removeAllChildren();
        }
        this.transientImpl_.addedChildren_ = 0;
        this.transientImpl_.childRemoveChanges_.clear();
        this.transientImpl_.specialChildRemove_ = false;
      }
    }
    if (this.transientImpl_ != null) {
      if (this.getParent() != null && this.getParent().isDisabled()) {
        this.propagateSetEnabled(false);
      }
    }
    if (all || this.flags_.get(BIT_SELECTABLE_CHANGED)) {
      if (this.flags_.get(BIT_SET_UNSELECTABLE)) {
        element.addPropertyWord(Property.Class, "unselectable");
        element.setAttribute("unselectable", "on");
        element.setAttribute("onselectstart", "return false;");
      } else {
        if (this.flags_.get(BIT_SET_SELECTABLE)) {
          element.addPropertyWord(Property.Class, "selectable");
          element.setAttribute("unselectable", "off");
          element.setAttribute("onselectstart", "event.cancelBubble=true; return true;");
        }
      }
      this.flags_.clear(BIT_SELECTABLE_CHANGED);
    }
    if (this.otherImpl_ != null) {
      if (this.otherImpl_.attributes_ != null) {
        if (all) {
          for (Iterator<Map.Entry<String, String>> i_it =
                  this.otherImpl_.attributes_.entrySet().iterator();
              i_it.hasNext(); ) {
            Map.Entry<String, String> i = i_it.next();
            if (i.getKey().equals("style")) {
              element.setProperty(Property.Style, i.getValue());
            } else {
              element.setAttribute(i.getKey(), i.getValue());
            }
          }
        } else {
          if (this.transientImpl_ != null) {
            for (int i = 0; i < this.transientImpl_.attributesSet_.size(); ++i) {
              String attr = this.transientImpl_.attributesSet_.get(i);
              if (attr.equals("style")) {
                element.setProperty(Property.Style, this.otherImpl_.attributes_.get(attr));
              } else {
                element.setAttribute(attr, this.otherImpl_.attributes_.get(attr));
              }
            }
          }
        }
      }
      if (all && this.otherImpl_.jsMembers_ != null) {
        for (int i = 0; i < this.otherImpl_.jsMembers_.size(); i++) {
          WWebWidget.OtherImpl.Member member = this.otherImpl_.jsMembers_.get(i);
          boolean notHere = false;
          if (this.otherImpl_.jsStatements_ != null) {
            for (int j = 0; j < this.otherImpl_.jsStatements_.size(); ++j) {
              final WWebWidget.OtherImpl.JavaScriptStatement jss =
                  this.otherImpl_.jsStatements_.get(j);
              if (jss.type == WWebWidget.JavaScriptStatementType.SetMember
                  && jss.data.equals(member.name)) {
                notHere = true;
                break;
              }
            }
          }
          if (notHere) {
            continue;
          }
          this.declareJavaScriptMember(element, member.name, member.value);
        }
      }
      if (this.otherImpl_.jsStatements_ != null) {
        for (int i = 0; i < this.otherImpl_.jsStatements_.size(); ++i) {
          final WWebWidget.OtherImpl.JavaScriptStatement jss = this.otherImpl_.jsStatements_.get(i);
          switch (jss.type) {
            case SetMember:
              this.declareJavaScriptMember(element, jss.data, this.getJavaScriptMember(jss.data));
              break;
            case CallMethod:
              element.callMethod(jss.data);
              break;
            case Statement:
              element.callJavaScript(jss.data);
              break;
          }
        }
        this.otherImpl_.jsStatements_ = null;
      }
    }
    if (this.flags_.get(BIT_HIDE_WITH_VISIBILITY)) {
      if (this.flags_.get(BIT_HIDDEN_CHANGED) || all && this.flags_.get(BIT_HIDDEN)) {
        if (this.flags_.get(BIT_HIDDEN)) {
          element.callJavaScript("Wt4.11.2.$('" + this.getId() + "').classList.add('Wt-hidden');");
          element.setProperty(Property.StyleVisibility, "hidden");
          if (this.flags_.get(BIT_HIDE_WITH_OFFSETS)) {
            element.setProperty(Property.StylePosition, "absolute");
            element.setProperty(Property.StyleTop, "-10000px");
            element.setProperty(Property.StyleLeft, "-10000px");
          }
        } else {
          if (this.flags_.get(BIT_HIDE_WITH_OFFSETS)) {
            if (this.layoutImpl_ != null) {
              switch (this.layoutImpl_.positionScheme_) {
                case Static:
                  element.setProperty(Property.StylePosition, "static");
                  break;
                case Relative:
                  element.setProperty(Property.StylePosition, "relative");
                  break;
                case Absolute:
                  element.setProperty(Property.StylePosition, "absolute");
                  break;
                case Fixed:
                  element.setProperty(Property.StylePosition, "fixed");
                  break;
              }
              if (!this.layoutImpl_.offsets_[0].isAuto()) {
                element.setProperty(Property.StyleTop, this.layoutImpl_.offsets_[0].getCssText());
              } else {
                element.setProperty(Property.StyleTop, "");
              }
              if (!this.layoutImpl_.offsets_[3].isAuto()) {
                element.setProperty(Property.StyleLeft, this.layoutImpl_.offsets_[3].getCssText());
              } else {
                element.setProperty(Property.StyleTop, "");
              }
            } else {
              element.setProperty(Property.StylePosition, "static");
              element.setProperty(Property.StyleTop, "");
              element.setProperty(Property.StyleLeft, "");
            }
          }
          element.callJavaScript(
              "Wt4.11.2.$('" + this.getId() + "').classList.remove('Wt-hidden');");
          element.setProperty(Property.StyleVisibility, "visible");
        }
      }
    }
    if (!all && this.flags_.get(BIT_HIDDEN_CHANGED) || all && !this.flags_.get(BIT_HIDDEN)) {
      if (this.transientImpl_ != null && !this.transientImpl_.animation_.isEmpty()) {
        String THIS_JS = "js/WWebWidget.js";
        if (!(app != null)) {
          app = WApplication.getInstance();
        }
        app.loadJavaScript(THIS_JS, wtjs1());
        app.loadJavaScript(THIS_JS, wtjs2());
        if (!this.flags_.get(BIT_HIDE_WITH_VISIBILITY)) {
          StringBuilder ss = new StringBuilder();
          ss.append("Wt4.11.2")
              .append(".animateDisplay(")
              .append(app.getJavaScriptClass())
              .append(",'")
              .append(this.getId())
              .append("',")
              .append(EnumUtils.valueOf(this.transientImpl_.animation_.getEffects()))
              .append(",")
              .append((int) this.transientImpl_.animation_.getTimingFunction().getValue())
              .append(",")
              .append(this.transientImpl_.animation_.getDuration())
              .append(",'")
              .append(element.getProperty(Property.StyleDisplay))
              .append("');");
          element.callJavaScript(ss.toString());
          if (all) {
            element.setProperty(Property.StyleDisplay, "none");
          } else {
            element.removeProperty(Property.StyleDisplay);
          }
        } else {
          StringBuilder ss = new StringBuilder();
          ss.append("Wt4.11.2")
              .append(".animateVisible('")
              .append(this.getId())
              .append("',")
              .append(EnumUtils.valueOf(this.transientImpl_.animation_.getEffects()))
              .append(",")
              .append((int) this.transientImpl_.animation_.getTimingFunction().getValue())
              .append(",")
              .append(this.transientImpl_.animation_.getDuration())
              .append(",'")
              .append(element.getProperty(Property.StyleVisibility))
              .append("','")
              .append(element.getProperty(Property.StylePosition))
              .append("','")
              .append(element.getProperty(Property.StyleTop))
              .append("','")
              .append(element.getProperty(Property.StyleLeft))
              .append("');");
          element.callJavaScript(ss.toString());
          if (all) {
            element.setProperty(Property.StyleVisibility, "hidden");
            element.setProperty(Property.StylePosition, "absolute");
            element.setProperty(Property.StyleTop, "-10000px");
            element.setProperty(Property.StyleLeft, "-10000px");
          } else {
            element.removeProperty(Property.StyleVisibility);
            element.removeProperty(Property.StylePosition);
            element.removeProperty(Property.StyleTop);
            element.removeProperty(Property.StyleLeft);
          }
        }
      }
    }
    this.flags_.clear(BIT_HIDDEN_CHANGED);
    if (this.flags_.get(BIT_GOT_FOCUS)) {
      if (!(app != null)) {
        app = WApplication.getInstance();
      }
      final WEnvironment env = app.getEnvironment();
      element.callJavaScript(
          "setTimeout(function() {var o = "
              + this.getJsRef()
              + ";if (o) {if (!o.classList.contains('"
              + app.getTheme().getDisabledClass()
              + "')) {try { o.focus();} catch (e) {}}}}, "
              + (env.agentIsIElt(9) ? "500" : "10")
              + ");");
      this.flags_.clear(BIT_GOT_FOCUS);
    }
    if (this.flags_.get(BIT_TABINDEX_CHANGED) || all) {
      if (this.otherImpl_ != null && this.otherImpl_.tabIndex_ != Integer.MIN_VALUE) {
        element.setProperty(Property.TabIndex, String.valueOf(this.otherImpl_.tabIndex_));
      } else {
        if (!all) {
          element.removeAttribute("tabindex");
        }
      }
      this.flags_.clear(BIT_TABINDEX_CHANGED);
    }
    if (all || this.flags_.get(BIT_SCROLL_VISIBILITY_CHANGED)) {
      String SCROLL_JS = "js/ScrollVisibility.js";
      if (!(app != null)) {
        app = WApplication.getInstance();
      }
      if (!app.isJavaScriptLoaded(SCROLL_JS) && this.isScrollVisibilityEnabled()) {
        app.loadJavaScript(SCROLL_JS, wtjs3());
        StringBuilder ss = new StringBuilder();
        ss.append("if (!Wt4.11.2.scrollVisibility) {Wt4.11.2.scrollVisibility = new ");
        ss.append("Wt4.11.2.ScrollVisibility(").append(app.getJavaScriptClass() + "); }");
        element.callJavaScript(ss.toString());
      }
      if (this.isScrollVisibilityEnabled()) {
        StringBuilder ss = new StringBuilder();
        ss.append("Wt4.11.2.scrollVisibility.add({");
        ss.append("el:").append(this.getJsRef()).append(',');
        ss.append("margin:").append(this.getScrollVisibilityMargin()).append(',');
        ss.append("visible:").append(this.isScrollVisible());
        ss.append("});");
        element.callJavaScript(ss.toString());
        this.flags_.set(BIT_SCROLL_VISIBILITY_LOADED);
      } else {
        if (this.flags_.get(BIT_SCROLL_VISIBILITY_LOADED)) {
          element.callJavaScript(
              "Wt4.11.2.scrollVisibility.remove(" + jsStringLiteral(this.getId()) + ");");
          this.flags_.clear(BIT_SCROLL_VISIBILITY_LOADED);
        }
      }
      this.flags_.clear(BIT_SCROLL_VISIBILITY_CHANGED);
    }
    if (all || this.flags_.get(BIT_OBJECT_NAME_CHANGED)) {
      if (this.getObjectName().length() != 0) {
        element.setAttribute("data-object-name", this.getObjectName());
      } else {
        if (!all) {
          element.removeAttribute("data-object-name");
        }
      }
      this.flags_.clear(BIT_OBJECT_NAME_CHANGED);
    }
    this.renderOk();
    this.transientImpl_ = null;
  }

  boolean domCanBeSaved() {
    final boolean[] canBeSaved = new boolean[1];
    canBeSaved[0] = true;
    this.iterateChildren(
        (WWidget child) -> {
          canBeSaved[0] = canBeSaved[0] && child.getWebWidget().domCanBeSaved();
        });
    return canBeSaved[0];
  }

  void propagateRenderOk(boolean deep) {
    this.flags_.clear(BIT_FLEX_BOX_CHANGED);
    this.flags_.clear(BIT_HIDDEN_CHANGED);
    this.flags_.clear(BIT_GEOMETRY_CHANGED);
    this.flags_.clear(BIT_FLOAT_SIDE_CHANGED);
    this.flags_.clear(BIT_TOOLTIP_CHANGED);
    this.flags_.clear(BIT_MARGINS_CHANGED);
    this.flags_.clear(BIT_STYLECLASS_CHANGED);
    this.flags_.clear(BIT_SELECTABLE_CHANGED);
    this.flags_.clear(BIT_WIDTH_CHANGED);
    this.flags_.clear(BIT_HEIGHT_CHANGED);
    this.flags_.clear(BIT_DISABLED_CHANGED);
    this.flags_.clear(BIT_ZINDEX_CHANGED);
    this.flags_.clear(BIT_TABINDEX_CHANGED);
    this.flags_.clear(BIT_SCROLL_VISIBILITY_CHANGED);
    this.flags_.clear(BIT_OBJECT_NAME_CHANGED);
    this.flags_.clear(BIT_PARENT_CHANGED);
    this.renderOk();
    if (deep) {
      this.iterateChildren(
          (WWidget c) -> {
            c.getWebWidget().propagateRenderOk();
          });
    }
    this.transientImpl_ = null;
  }

  final void propagateRenderOk() {
    propagateRenderOk(true);
  }

  String renderRemoveJs(boolean recursive) {
    final StringBuilder result = new StringBuilder();
    if (this.isRendered() && this.isScrollVisibilityEnabled()) {
      result
          .append("Wt4.11.2.scrollVisibility.remove(")
          .append(jsStringLiteral(this.getId()))
          .append(");");
      this.flags_.set(BIT_SCROLL_VISIBILITY_CHANGED);
      this.flags_.clear(BIT_SCROLL_VISIBILITY_LOADED);
    }
    this.iterateChildren(
        (WWidget c) -> {
          result.append(c.renderRemoveJs(true));
        });
    if (!recursive) {
      if ((result.length() == 0)) {
        result.append("_").append(this.getId());
      } else {
        result.append("Wt4.11.2.remove('").append(this.getId()).append("');");
      }
    }
    return result.toString();
  }

  protected void propagateSetEnabled(final boolean enabled) {
    this.iterateChildren(
        (WWidget c) -> {
          if (!c.isDisabled()) {
            c.getWebWidget().propagateSetEnabled(enabled);
          }
        });
  }

  protected void propagateSetVisible(final boolean visible) {
    this.iterateChildren(
        (WWidget c) -> {
          if (!c.isHidden()) {
            c.getWebWidget().propagateSetVisible(visible);
          }
        });
  }

  boolean isStubbed() {
    if (this.flags_.get(BIT_STUBBED)) {
      return true;
    } else {
      WWidget p = this.getParent();
      return p != null ? p.isStubbed() : false;
    }
  }

  protected void enableAjax() {
    if (!this.isStubbed()) {
      for (Iterator<AbstractEventSignal> i_it = this.eventSignals().iterator(); i_it.hasNext(); ) {
        AbstractEventSignal i = i_it.next();
        final AbstractEventSignal s = i;
        if (s.getName() == WInteractWidget.M_CLICK_SIGNAL) {
          this.repaint(EnumSet.of(RepaintFlag.ToAjax));
        }
        s.ownerRepaint();
      }
    }
    if (this.flags_.get(BIT_TOOLTIP_DEFERRED)
        || this.lookImpl_ != null && this.lookImpl_.toolTipTextFormat_ != TextFormat.Plain) {
      this.flags_.set(BIT_TOOLTIP_CHANGED);
      this.repaint();
    }
    this.iterateChildren(
        (WWidget c) -> {
          c.enableAjax();
        });
  }

  void setHideWithOffsets(boolean how) {
    if (how) {
      if (!this.flags_.get(BIT_HIDE_WITH_OFFSETS)) {
        this.flags_.set(BIT_HIDE_WITH_VISIBILITY);
        this.flags_.set(BIT_HIDE_WITH_OFFSETS);
        // this.resetLearnedSlot(WWidget.show);
        // this.resetLearnedSlot(WWidget.hide);
        if (this.getParent() != null) {
          this.getParent().setHideWithOffsets(true);
        }
      }
    }
  }
  // protected AbstractEventSignal.LearningListener  getStateless(<pointertomember or
  // dependentsizedarray> methodpointertomember or dependentsizedarray>) ;
  WWidget getSelfWidget() {
    WWidget p = null;
    WWidget p_parent = this;
    do {
      p = p_parent;
      p_parent = p.getParent();
    } while (p_parent != null && ObjectUtils.cast(p_parent, WCompositeWidget.class) != null);
    return p;
  }

  void doLoad(WWidget w) {
    w.load();
    if (!w.isLoaded()) {
      logger.error(
          new StringWriter()
              .append("improper load() implementation: base implementation not called")
              .toString());
    }
  }

  protected void widgetAdded(WWidget child) {
    child.setParentWidget(this);
    if (this.flags_.get(BIT_LOADED)) {
      this.doLoad(child);
    }
    WApplication.getInstance().getSession().getRenderer().updateFormObjects(this, false);
    if (!(this.transientImpl_ != null)) {
      this.transientImpl_ = new WWebWidget.TransientImpl();
    }
    ++this.transientImpl_.addedChildren_;
    this.emitChildrenChanged();
  }

  protected void widgetRemoved(WWidget child, boolean renderRemove) {
    if (!this.flags_.get(BIT_BEING_DELETED) && renderRemove) {
      String js = child.renderRemoveJs(false);
      if (!(this.transientImpl_ != null)) {
        this.transientImpl_ = new WWebWidget.TransientImpl();
      }
      this.transientImpl_.childRemoveChanges_.add(js);
      if (js.charAt(0) != '_') {
        this.transientImpl_.specialChildRemove_ = true;
      }
      this.repaint(EnumSet.of(RepaintFlag.SizeAffected));
    }
    child.setParentWidget((WWidget) null);
    if (this.transientImpl_ != null
        && !child.getWebWidget().isRendered()
        && !child.getWebWidget().isStubbed()) {
      --this.transientImpl_.addedChildren_;
    }
    if (!child.getWebWidget().flags_.get(BIT_BEING_DELETED)) {
      child.getWebWidget().setRendered(false);
    }
    WApplication.getInstance()
        .getSession()
        .getRenderer()
        .updateFormObjects(child.getWebWidget(), true);
    this.emitChildrenChanged();
  }

  protected WWidget manageWidget(WWidget managed, WWidget w) {
    if (managed != null) {
      this.widgetRemoved(managed, true);
    }
    WWidget result = managed;
    managed = w;
    if (managed != null) {
      this.widgetAdded(managed);
    }
    return result;
  }

  protected void render(EnumSet<RenderFlag> flags) {
    super.render(flags);
  }

  void signalConnectionsChanged() {
    this.repaint();
  }

  protected void beingDeleted() {
    this.flags_.set(BIT_BEING_DELETED);
  }

  private static final int BIT_INLINE = 0;
  private static final int BIT_HIDDEN = 1;
  private static final int BIT_LOADED = 2;
  private static final int BIT_RENDERED = 3;
  private static final int BIT_STUBBED = 4;
  private static final int BIT_FORM_OBJECT = 5;
  private static final int BIT_FLEX_BOX = 6;
  private static final int BIT_FLEX_BOX_CHANGED = 7;
  private static final int BIT_GEOMETRY_CHANGED = 8;
  private static final int BIT_HIDE_WITH_OFFSETS = 9;
  private static final int BIT_BEING_DELETED = 10;
  private static final int BIT_DONOT_STUB = 11;
  private static final int BIT_FLOAT_SIDE_CHANGED = 12;
  static final int BIT_REPAINT_TO_AJAX = 13;
  private static final int BIT_HIDE_WITH_VISIBILITY = 14;
  private static final int BIT_HIDDEN_CHANGED = 15;
  static final int BIT_ENABLED = 16;
  private static final int BIT_TOOLTIP_CHANGED = 17;
  private static final int BIT_MARGINS_CHANGED = 18;
  private static final int BIT_STYLECLASS_CHANGED = 19;
  private static final int BIT_SET_UNSELECTABLE = 20;
  private static final int BIT_SET_SELECTABLE = 21;
  private static final int BIT_SELECTABLE_CHANGED = 22;
  private static final int BIT_WIDTH_CHANGED = 23;
  private static final int BIT_HEIGHT_CHANGED = 24;
  private static final int BIT_DISABLED = 25;
  private static final int BIT_DISABLED_CHANGED = 26;
  private static final int BIT_CONTAINS_LAYOUT = 27;
  private static final int BIT_ZINDEX_CHANGED = 28;
  private static final int BIT_TOOLTIP_DEFERRED = 29;
  private static final int BIT_GOT_FOCUS = 30;
  private static final int BIT_TABINDEX_CHANGED = 31;
  private static final int BIT_SCROLL_VISIBILITY_ENABLED = 32;
  private static final int BIT_SCROLL_VISIBILITY_LOADED = 33;
  private static final int BIT_IS_SCROLL_VISIBLE = 34;
  private static final int BIT_SCROLL_VISIBILITY_CHANGED = 35;
  private static final int BIT_THEME_STYLE_DISABLED = 36;
  private static final int BIT_OBJECT_NAME_CHANGED = 37;
  private static final int BIT_PARENT_CHANGED = 38;
  private static String FOCUS_SIGNAL = "focus";
  private static String BLUR_SIGNAL = "blur";
  private static final int DEFAULT_BASE_Z_INDEX = 1100;
  private static final int Z_INDEX_INCREMENT = 1100;
  private String elementTagName_;

  private void loadToolTip() {
    if (!(this.lookImpl_.toolTip_ != null)) {
      this.lookImpl_.toolTip_ = new WString();
    }
    this.lookImpl_.toolTip_ = this.getToolTip();
    this.flags_.set(BIT_TOOLTIP_CHANGED);
    this.repaint();
  }

  BitSet flags_;
  private WLength width_;
  private WLength height_;
  private String id_;

  static class TransientImpl {
    private static Logger logger = LoggerFactory.getLogger(TransientImpl.class);

    public List<String> childRemoveChanges_;
    public List<String> addedStyleClasses_;
    public List<String> removedStyleClasses_;
    public List<String> attributesSet_;
    public int addedChildren_;
    public boolean specialChildRemove_;
    public WAnimation animation_;

    public TransientImpl() {
      this.childRemoveChanges_ = new ArrayList<String>();
      this.addedStyleClasses_ = new ArrayList<String>();
      this.removedStyleClasses_ = new ArrayList<String>();
      this.attributesSet_ = new ArrayList<String>();
      this.addedChildren_ = 0;
      this.specialChildRemove_ = false;
      this.animation_ = new WAnimation();
    }
  }

  WWebWidget.TransientImpl transientImpl_;

  static class LayoutImpl {
    private static Logger logger = LoggerFactory.getLogger(LayoutImpl.class);

    public PositionScheme positionScheme_;
    public Side floatSide_;
    public EnumSet<Side> clearSides_;
    public WLength[] offsets_ = new WLength[4];
    public WLength minimumWidth_;
    public WLength minimumHeight_;
    public WLength maximumWidth_;
    public WLength maximumHeight_;
    public int baseZIndex_;
    public int zIndex_;
    public AlignmentFlag verticalAlignment_;
    public WLength verticalAlignmentLength_;
    public WLength[] margin_ = new WLength[4];
    public WLength lineHeight_;

    public LayoutImpl() {
      this.positionScheme_ = PositionScheme.Static;
      this.floatSide_ = null;
      this.clearSides_ = EnumSet.noneOf(Side.class);
      this.minimumWidth_ = new WLength(0);
      this.minimumHeight_ = new WLength(0);
      this.maximumWidth_ = new WLength();
      this.maximumHeight_ = new WLength();
      this.baseZIndex_ = DEFAULT_BASE_Z_INDEX;
      this.zIndex_ = 0;
      this.verticalAlignment_ = AlignmentFlag.Baseline;
      this.verticalAlignmentLength_ = new WLength();
      this.lineHeight_ = new WLength();
      for (int i = 0; i < 4; ++i) {
        this.offsets_[i] = WLength.Auto;
        this.margin_[i] = new WLength(0);
      }
    }
  }

  WWebWidget.LayoutImpl layoutImpl_;

  static class LookImpl {
    private static Logger logger = LoggerFactory.getLogger(LookImpl.class);

    public WCssDecorationStyle decorationStyle_;
    public String styleClass_;
    public WString toolTip_;
    public TextFormat toolTipTextFormat_;
    public JSignal loadToolTip_;

    public LookImpl(WWebWidget w) {
      this.decorationStyle_ = null;
      this.styleClass_ = "";
      this.toolTip_ = null;
      this.toolTipTextFormat_ = TextFormat.Plain;
      this.loadToolTip_ = new JSignal(w, "Wt-loadToolTip");
    }
  }

  private WWebWidget.LookImpl lookImpl_;

  static class DropMimeType {
    private static Logger logger = LoggerFactory.getLogger(DropMimeType.class);

    public String hoverStyleClass;

    public DropMimeType() {
      this.hoverStyleClass = "";
    }

    public DropMimeType(final String aHoverStyleClass) {
      this.hoverStyleClass = aHoverStyleClass;
    }
  }

  enum JavaScriptStatementType {
    SetMember,
    CallMethod,
    Statement;

    /** Returns the numerical representation of this enum. */
    public int getValue() {
      return ordinal();
    }
  }

  static class OtherImpl {
    private static Logger logger = LoggerFactory.getLogger(OtherImpl.class);

    static class Member {
      private static Logger logger = LoggerFactory.getLogger(Member.class);

      public String name;
      public String value;
    }

    static class JavaScriptStatement {
      private static Logger logger = LoggerFactory.getLogger(JavaScriptStatement.class);

      public JavaScriptStatement(WWebWidget.JavaScriptStatementType aType, final String aData) {
        this.type = aType;
        this.data = aData;
      }

      public WWebWidget.JavaScriptStatementType type;
      public String data;
    }

    public String elementTagName_;
    public Map<String, String> attributes_;
    public List<WWebWidget.OtherImpl.Member> jsMembers_;
    public List<WWebWidget.OtherImpl.JavaScriptStatement> jsStatements_;
    public JSignal2<Integer, Integer> resized_;
    public int tabIndex_;
    public JSignal3<String, String, WMouseEvent> dropSignal_;
    public JSignal3<String, String, WTouchEvent> dropSignal2_;
    public Map<String, WWebWidget.DropMimeType> acceptedDropMimeTypes_;
    public Signal childrenChanged_;
    public int scrollVisibilityMargin_;
    public Signal1<Boolean> scrollVisibilityChanged_;
    public JSignal1<Boolean> jsScrollVisibilityChanged_;

    public OtherImpl(final WWebWidget self) {
      this.elementTagName_ = (String) null;
      this.attributes_ = null;
      this.jsMembers_ = null;
      this.jsStatements_ = null;
      this.resized_ = null;
      this.tabIndex_ = Integer.MIN_VALUE;
      this.dropSignal_ = null;
      this.dropSignal2_ = null;
      this.acceptedDropMimeTypes_ = null;
      this.childrenChanged_ = new Signal();
      this.scrollVisibilityMargin_ = 0;
      this.scrollVisibilityChanged_ = new Signal1<Boolean>();
      this.jsScrollVisibilityChanged_ = null;
    }
  }

  WWebWidget.OtherImpl otherImpl_;

  void renderOk() {
    super.renderOk();
    this.flags_.clear(BIT_REPAINT_TO_AJAX);
  }

  private void calcZIndex() {
    this.layoutImpl_.zIndex_ = -1;
    WWebWidget ww = this.getParentWebWidget();
    if (ww != null) {
      final List<WWidget> children = ww.getChildren();
      int maxZ = 0;
      for (int i = 0; i < children.size(); ++i) {
        WWebWidget wi = children.get(i).getWebWidget();
        if (wi.getBaseZIndex() <= this.getBaseZIndex()) {
          maxZ = Math.max(maxZ, wi.getZIndex());
        }
      }
      this.layoutImpl_.zIndex_ = Math.max(this.getBaseZIndex(), maxZ + Z_INDEX_INCREMENT);
    }
  }

  boolean needsToBeRendered() {
    return this.flags_.get(BIT_DONOT_STUB)
        || !this.flags_.get(BIT_HIDDEN)
        || !WApplication.getInstance().getSession().getRenderer().isVisibleOnly();
  }

  void getSDomChanges(final List<DomElement> result, WApplication app) {
    if (this.flags_.get(BIT_STUBBED)) {
      if (app.getSession().getRenderer().isPreLearning()) {
        this.getDomChanges(result, app);
        this.scheduleRerender(true);
      } else {
        if (!app.getSession().getRenderer().isVisibleOnly()) {
          this.flags_.clear(BIT_STUBBED);
          DomElement stub = DomElement.getForUpdate(this, DomElementType.SPAN);
          WWidget self = this.getSelfWidget();
          this.setRendered(true);
          self.render(EnumSet.of(RenderFlag.Full));
          DomElement realElement = this.createDomElement(app);
          app.getTheme().apply(self, realElement, ElementThemeRole.MainElement);
          stub.unstubWith(realElement, !this.flags_.get(BIT_HIDE_WITH_OFFSETS));
          result.add(stub);
        }
      }
    } else {
      this.render(EnumSet.of(RenderFlag.Update));
      this.getDomChanges(result, app);
    }
  }

  private void getSFormObjects(final Map<String, WObject> result) {
    if (!this.flags_.get(BIT_STUBBED)
        && !this.flags_.get(BIT_HIDDEN)
        && this.flags_.get(BIT_RENDERED)) {
      this.getFormObjects(result);
    }
  }

  WWebWidget getParentWebWidget() {
    WWidget p = this.getParent();
    while (p != null && ObjectUtils.cast(p, WCompositeWidget.class) != null) {
      p = p.getParent();
    }
    return p != null ? p.getWebWidget() : null;
  }

  boolean setAcceptDropsImpl(final String mimeType, boolean accept, final String hoverStyleClass) {
    boolean result = false;
    boolean changed = false;
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    if (!(this.otherImpl_.acceptedDropMimeTypes_ != null)) {
      this.otherImpl_.acceptedDropMimeTypes_ = new HashMap<String, WWebWidget.DropMimeType>();
    }
    WWebWidget.DropMimeType i = this.otherImpl_.acceptedDropMimeTypes_.get(mimeType);
    if (i == null) {
      if (accept) {
        result = this.otherImpl_.acceptedDropMimeTypes_.isEmpty();
        this.otherImpl_.acceptedDropMimeTypes_.put(
            mimeType, new WWebWidget.DropMimeType(hoverStyleClass));
        changed = true;
      }
    } else {
      if (!accept) {
        this.otherImpl_.acceptedDropMimeTypes_.remove(mimeType);
        changed = true;
      }
    }
    if (changed) {
      String mimeTypes = "";
      for (Iterator<Map.Entry<String, WWebWidget.DropMimeType>> j_it =
              this.otherImpl_.acceptedDropMimeTypes_.entrySet().iterator();
          j_it.hasNext(); ) {
        Map.Entry<String, WWebWidget.DropMimeType> j = j_it.next();
        mimeTypes += "{" + j.getKey() + ":" + j.getValue().hoverStyleClass + "}";
      }
      this.setAttributeValue("amts", mimeTypes);
    }
    if (result && !(this.otherImpl_.dropSignal_ != null)) {
      this.otherImpl_.dropSignal_ = new JSignal3<String, String, WMouseEvent>(this, "_drop") {};
    }
    if (result && !(this.otherImpl_.dropSignal2_ != null)) {
      this.otherImpl_.dropSignal2_ = new JSignal3<String, String, WTouchEvent>(this, "_drop2") {};
    }
    return result;
  }

  void setImplementLayoutSizeAware(boolean aware) {
    if (!aware) {
      if (this.otherImpl_ != null) {
        if (this.otherImpl_.resized_ != null) {
          this.otherImpl_.resized_ = null;
          String v = this.getJavaScriptMember(WT_RESIZE_JS);
          if (v.length() == 1) {
            this.setJavaScriptMember(WT_RESIZE_JS, "");
          } else {
            this.addJavaScriptStatement(WWebWidget.JavaScriptStatementType.SetMember, WT_RESIZE_JS);
          }
        }
      }
    }
  }

  JSignal2<Integer, Integer> resized() {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    if (!(this.otherImpl_.resized_ != null)) {
      this.otherImpl_.resized_ = new JSignal2<Integer, Integer>(this, "resized") {};
      this.otherImpl_.resized_.addListener(
          this,
          (Integer e1, Integer e2) -> {
            WWebWidget.this.layoutSizeChanged(e1, e2);
          });
      String v = this.getJavaScriptMember(WT_RESIZE_JS);
      if (v.length() == 0) {
        this.setJavaScriptMember(WT_RESIZE_JS, "0");
      } else {
        this.addJavaScriptStatement(WWebWidget.JavaScriptStatementType.SetMember, WT_RESIZE_JS);
      }
    }
    return this.otherImpl_.resized_;
  }

  private void addJavaScriptStatement(WWebWidget.JavaScriptStatementType type, final String data) {
    if (!(this.otherImpl_ != null)) {
      this.otherImpl_ = new WWebWidget.OtherImpl(this);
    }
    if (!(this.otherImpl_.jsStatements_ != null)) {
      this.otherImpl_.jsStatements_ = new ArrayList<WWebWidget.OtherImpl.JavaScriptStatement>();
    }
    final List<WWebWidget.OtherImpl.JavaScriptStatement> v = this.otherImpl_.jsStatements_;
    if (type == WWebWidget.JavaScriptStatementType.SetMember) {
      for (int i = 0; i < v.size(); ++i) {
        if (v.get(i).type == WWebWidget.JavaScriptStatementType.SetMember
            && v.get(i).data.equals(data)) {
          return;
        }
      }
    }
    if (v.isEmpty() || v.get(v.size() - 1).type != type || !v.get(v.size() - 1).data.equals(data)) {
      v.add(new WWebWidget.OtherImpl.JavaScriptStatement(type, data));
    }
  }

  private int indexOfJavaScriptMember(final String name) {
    if (this.otherImpl_ != null && this.otherImpl_.jsMembers_ != null) {
      for (int i = 0; i < this.otherImpl_.jsMembers_.size(); i++) {
        if (this.otherImpl_.jsMembers_.get(i).name.equals(name)) {
          return i;
        }
      }
    }
    return -1;
  }

  private void declareJavaScriptMember(
      final DomElement element, final String name, final String value) {
    if (name.charAt(0) != ' ') {
      if (name.equals(WT_RESIZE_JS) && this.otherImpl_.resized_ != null) {
        StringBuilder combined = new StringBuilder();
        if (value.length() > 1) {
          String unterminatedValue = value;
          if (unterminatedValue.charAt(unterminatedValue.length() - 1) == ';') {
            unterminatedValue = unterminatedValue.substring(0, unterminatedValue.length() - 1);
          }
          combined
              .append(name)
              .append("=function(s,w,h) {")
              .append(WApplication.getInstance().getJavaScriptClass())
              .append("._p_.propagateSize(s,w,h);")
              .append("(")
              .append(unterminatedValue)
              .append(")(s,w,h);")
              .append("}");
        } else {
          combined
              .append(name)
              .append("=")
              .append(WApplication.getInstance().getJavaScriptClass())
              .append("._p_.propagateSize");
        }
        element.callMethod(combined.toString());
      } else {
        if (value.length() > 0) {
          element.callMethod(name + "=" + value);
        } else {
          element.callMethod(name + "=null");
        }
      }
    } else {
      element.callJavaScript(value);
    }
  }

  private WString getStoredToolTip() {
    return this.lookImpl_ != null && this.lookImpl_.toolTip_ != null
        ? this.lookImpl_.toolTip_
        : WString.Empty;
  }

  private void undoSetFocus() {}

  private void jsScrollVisibilityChanged(boolean visible) {
    this.flags_.set(BIT_IS_SCROLL_VISIBLE, visible);
    if (this.otherImpl_ != null) {
      this.otherImpl_.scrollVisibilityChanged_.trigger(visible);
    }
  }

  private void emitChildrenChanged() {
    if (!this.flags_.get(BIT_BEING_DELETED) && this.otherImpl_ != null) {
      this.otherImpl_.childrenChanged_.trigger();
    }
  }

  protected void setParentWidget(WWidget parent) {
    this.flags_.set(BIT_PARENT_CHANGED, parent != this.getParent());
    super.setParentWidget(parent);
    if (parent != null) {
      if (this.isPopup()) {
        this.calcZIndex();
      }
      this.propagateSetEnabled(parent.isEnabled());
    }
  }

  void setRendered(boolean rendered) {
    if (rendered) {
      this.flags_.set(BIT_RENDERED);
    } else {
      this.flags_.clear(BIT_RENDERED);
      this.renderOk();
      this.iterateChildren(
          (WWidget c) -> {
            c.getWebWidget().setRendered(false);
          });
    }
  }

  void setId(DomElement element, WApplication app) {
    if (!app.getEnvironment().agentIsSpiderBot() || this.id_ != null) {
      if (!this.flags_.get(BIT_FORM_OBJECT)) {
        element.setId(this.getId());
      } else {
        element.setName(this.getId());
      }
    }
  }

  WWebWidget getWebWidget() {
    return this;
  }

  protected EventSignal voidEventSignal(String name, boolean create) {
    AbstractEventSignal b = this.getEventSignal(name);
    if (b != null) {
      return (EventSignal) b;
    } else {
      if (!create) {
        return null;
      } else {
        EventSignal result = new EventSignal(name, this);
        this.addEventSignal(result);
        return result;
      }
    }
  }

  EventSignal1<WKeyEvent> keyEventSignal(String name, boolean create) {
    AbstractEventSignal b = this.getEventSignal(name);
    if (b != null) {
      return (EventSignal1<WKeyEvent>) b;
    } else {
      if (!create) {
        return null;
      } else {
        EventSignal1<WKeyEvent> result =
            new EventSignal1<WKeyEvent>(name, this, WKeyEvent.templateEvent);
        this.addEventSignal(result);
        return result;
      }
    }
  }

  EventSignal1<WMouseEvent> mouseEventSignal(String name, boolean create) {
    AbstractEventSignal b = this.getEventSignal(name);
    if (b != null) {
      return (EventSignal1<WMouseEvent>) b;
    } else {
      if (!create) {
        return null;
      } else {
        EventSignal1<WMouseEvent> result =
            new EventSignal1<WMouseEvent>(name, this, WMouseEvent.templateEvent);
        this.addEventSignal(result);
        return result;
      }
    }
  }

  EventSignal1<WScrollEvent> scrollEventSignal(String name, boolean create) {
    AbstractEventSignal b = this.getEventSignal(name);
    if (b != null) {
      return (EventSignal1<WScrollEvent>) b;
    } else {
      if (!create) {
        return null;
      } else {
        EventSignal1<WScrollEvent> result =
            new EventSignal1<WScrollEvent>(name, this, WScrollEvent.templateEvent);
        this.addEventSignal(result);
        return result;
      }
    }
  }

  EventSignal1<WTouchEvent> touchEventSignal(String name, boolean create) {
    AbstractEventSignal b = this.getEventSignal(name);
    if (b != null) {
      return (EventSignal1<WTouchEvent>) b;
    } else {
      if (!create) {
        return null;
      } else {
        EventSignal1<WTouchEvent> result =
            new EventSignal1<WTouchEvent>(name, this, WTouchEvent.templateEvent);
        this.addEventSignal(result);
        return result;
      }
    }
  }

  EventSignal1<WGestureEvent> gestureEventSignal(String name, boolean create) {
    AbstractEventSignal b = this.getEventSignal(name);
    if (b != null) {
      return (EventSignal1<WGestureEvent>) b;
    } else {
      if (!create) {
        return null;
      } else {
        EventSignal1<WGestureEvent> result =
            new EventSignal1<WGestureEvent>(name, this, WGestureEvent.templateEvent);
        this.addEventSignal(result);
        return result;
      }
    }
  }

  protected void updateSignalConnection(
      final DomElement element, final AbstractEventSignal signal, String name, boolean all) {
    if (name.charAt(0) != 'M' && signal.needsUpdate(all)) {
      element.setEventSignal(name, signal);
      signal.updateOk();
    }
  }

  protected void parentResized(final WWidget parent, final EnumSet<Orientation> directions) {
    if (this.flags_.get(BIT_CONTAINS_LAYOUT)) {
      this.iterateChildren(
          (WWidget c) -> {
            if (!c.isHidden()) {
              c.getWebWidget().parentResized(parent, directions);
            }
          });
    }
  }

  protected final void parentResized(
      final WWidget parent, final Orientation direction, Orientation... directions) {
    parentResized(parent, EnumSet.of(direction, directions));
  }

  void containsLayout() {
    if (!this.flags_.get(BIT_CONTAINS_LAYOUT)) {
      this.flags_.set(BIT_CONTAINS_LAYOUT);
      WWebWidget p = this.getParentWebWidget();
      if (p != null) {
        p.containsLayout();
      }
    }
  }

  protected void setFlexBox(boolean enabled) {
    this.flags_.set(BIT_FLEX_BOX, enabled);
    this.flags_.set(BIT_FLEX_BOX_CHANGED);
  }

  static Property[] properties = {
    Property.StyleTop, Property.StyleRight, Property.StyleBottom, Property.StyleLeft
  };

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "animateDisplay",
        "(function(t,n,i,o,e,s){const a=t.WT;!function n(i,o,e,s,r){const d={None:0,SlideInFromLeft:1,SlideInFromRight:2,SlideInFromBottom:3,SlideInFromTop:4,Pop:5,Fade:256},l=[\"ease\",\"linear\",\"ease-in\",\"ease-out\",\"ease-in-out\"],c=[0,1,3,2,4,5],p=document.getElementById(i),m=1===p.childElementCount?p.firstElementChild:null;function f(t,n,i){for(const o of Object.keys(n)){i&&void 0===i[o]&&(i[o]=t.style[o]);t.style[o]=n[o]}}const h=f;if((()=>{const t={},n=a.css(p,\"display\");f(p,{display:r},t);const i=a.css(p,\"display\");h(p,t);return n!==i})()){const F=p.parentNode;if(F.wtAnimateChild){F.wtAnimateChild(a,p,o,e,s,{display:r});return}if(p.classList.contains(\"animating\")){Promise.all(p.getAnimations().map((t=>t.finished))).then((()=>{n(i,o,e,s,r)}));return}p.classList.add(\"animating\");const b=255&o,x=\"none\"===r,S={},I={},T={duration:s,iterations:1,easing:l[x?c[e]:e]};function u(){p.wtAnimatedHidden&&p.wtAnimatedHidden(x);0===p.getAnimations().length&&p.classList.remove(\"animating\");t.layouts2&&t.layouts2.setElementDirty(p)}function y(){p.style.display=r;p.wtPosition&&p.wtPosition();window.onshow&&window.onshow()}function g(t,n,i,e){x||y();const s=a.px(p,t),l={transform:`translate${e}(${(a.px(p,n)+s)*(i?-1:1)}px)`},c={transform:`translate${e}(0px)`};if(o&d.Fade){l.opacity=0;c.opacity=1}const m=x?[c,l]:[l,c];p.animate(m,T).finished.then((()=>{x&&(p.style.display=r);u()}))}function w(){x||y();const t={},n={};switch(b){case d.Pop:f(p,{transformOrigin:\"50% 50%\"},S);t.transform=\"scale(.2)\";n.transform=\"scale(1)\";break;case d.SlideInFromRight:t.transform=\"translateX(100%)\";n.transform=\"translateX(0)\";break;case d.SlideInFromLeft:t.transform=\"translateX(-100%)\";n.transform=\"translateX(0)\"}if(o&d.Fade){t.opacity=0;n.opacity=a.css(p,\"opacity\")}const i=x?[n,t]:[t,n];p.animate(i,T).finished.then((()=>{x&&(p.style.display=r);h(p,S);u()}))}const E=[\"absolute\",\"fixed\"].includes(a.css(p,\"position\"));switch(b){case d.SlideInFromTop:case d.SlideInFromBottom:E?g(\"height\",b===d.SlideInFromTop?\"top\":\"bottom\",b===d.SlideInFromTop,\"Y\"):function(){x||y();f(p,{overflow:\"hidden\"},S);const t=[],n=(()=>{if(!m)return null;const t=p.getBoundingClientRect(),n=m.getBoundingClientRect();return{left:n.x-t.x-a.px(p,\"border-left-width\"),top:n.y-t.y-a.px(p,\"border-top-width\")}})();if(m&&b===d.SlideInFromTop){const n={transform:\"translateY(0px)\"},i={transform:`translateY(-${p.clientHeight}px)`},o=x?[n,i]:[i,n];t.push(m.animate(o,T))}{const n={height:\"0px\",paddingTop:\"0px\",paddingBottom:\"0px\",borderTopWidth:\"0px\",borderBottomWidth:\"0px\"},i={height:a.css(p,\"height\"),paddingTop:a.css(p,\"padding-top\"),paddingBottom:a.css(p,\"padding-bottom\"),borderTopWidth:a.css(p,\"border-top-width\"),borderBottomWidth:a.css(p,\"border-bottom-width\")};if(o&d.Fade){n.opacity=0;i.opacity=1}const e=x?[i,n]:[n,i];t.push(p.animate(e,T))}if(m){f(p,{position:\"relative\"},S);const t={position:\"absolute\",left:`${n.left}px`,top:`${n.top}px`,width:`${m.clientWidth}px`};f(m,t,I)}Promise.all(t.map((t=>t.finished))).then((()=>{x&&(p.style.display=r);h(p,S);h(m,I);u()}))}();break;case d.SlideInFromLeft:case d.SlideInFromRight:E?g(\"width\",b===d.SlideInFromLeft?\"left\":\"right\",b===d.SlideInFromLeft,\"X\"):w();break;case d.None:case d.Pop:w()}}}(n,i,o,e,s)})");
  }

  static WJavaScriptPreamble wtjs2() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "animateVisible",
        "(function(t,n,i,o,e,s,a,r){})");
  }

  static WJavaScriptPreamble wtjs10() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "toolTip",
        "(function(e,t,n,o,l,i){const c=document.getElementById(t),u=e.WT,a=c.toolTip;a||(c.toolTip=new function(){let a=null,s=null,d=null,r=null;let m=!1,T=n,f=!1;this.setToolTipText=function(e){T=e;if(m){this.showToolTip();clearTimeout(a);m=!1}};this.showToolTip=function(){!o||T||m||function(){m=!0;e.emit(c,\"Wt-loadToolTip\")}();if(T){r=document.createElement(\"div\");r.className=l;r.innerHTML=T;const e=document.createElement(\"div\");e.className=i;document.body.appendChild(e);e.appendChild(r);const t=d.x,n=d.y;u.fitToWindow(e,t+10,n+10,t-10,n-10);let o=0;const c=parseInt(u.css(e,\"zIndex\"),10);document.querySelectorAll(\".Wt-dialog, .modal, .modal-dialog\").forEach((function(t){o=Math.max(o,parseInt(u.css(t,\"zIndex\"),10));if(o>c){const t=o+1e3;e.style.zIndex=t}}));r.addEventListener(\"mouseenter\",(function(){f=!0}));r.addEventListener(\"mouseleave\",(function(){f=!1}))}clearInterval(s);s=null;s=setInterval((function(){document.querySelectorAll(`#${t}:hover`).length||p()}),200)};function p(){clearTimeout(a);setTimeout((function(){if(!f&&r){r.parentElement.remove();r=null;clearInterval(s);s=null}}),200)}function v(e){clearTimeout(a);d=u.pageCoordinates(e);r||(a=setTimeout((function(){c.toolTip.showToolTip()}),500))}c.addEventListener(\"mouseenter\",v);c.addEventListener(\"mousemove\",v);c.addEventListener(\"mouseleave\",p)});a&&a.setToolTipText(n)})");
  }

  static WJavaScriptPreamble wtjs3() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "ScrollVisibility",
        "(function(e){const t=e.WT;function i(e){return\"hidden\"!==e.style.visibility&&\"none\"!==e.style.display&&!e.classList.contains(\"out\")&&(!((e=e.parentNode)&&!t.hasTag(e,\"BODY\"))||i(e))}let n=0;const o={},s=!1;function c(e,n){if(!i(e))return!1;const o=t.widgetPageCoordinates(e),s=o.x-document.body.scrollLeft-document.documentElement.scrollLeft,c=o.y-document.body.scrollTop-document.documentElement.scrollTop,l=t.windowSize(),r=s,d=e.offsetWidth,a=c,u=e.offsetHeight,f=-n,m=l.x+2*n,v=-n,b=l.y+2*n;return r+d>=f&&f+m>=r&&a+u>=v&&v+b>=a}function l(){for(const t of Object.values(o)){const i=c(t.el,t.margin);if(i!==t.visible){t.visible=i;e.emit(t.el,\"scrollVisibilityChanged\",i)}}}const r=new MutationObserver(l);this.add=function(t){0===n&&function(){r.observe(document,{childList:!0,attributes:!0,subtree:!0,characterData:!0});window.addEventListener(\"resize\",l,!0);window.addEventListener(\"scroll\",l,!0)}();const i=t.el.id,d=i in o;s;const a=c(t.el,t.margin);if(t.visible!==a){t.visible=a;e.emit(t.el,\"scrollVisibilityChanged\",a)}o[i]=t;s;d||++n};this.remove=function(e){if(0!==n){if(e in o){s;delete o[e];--n}0===n&&function(){r.disconnect();window.removeEventListener(\"resize\",l,{capture:!0});window.removeEventListener(\"scroll\",l,{capture:!0})}()}}})");
  }

  static WLength nonNegative(final WLength w) {
    if (w.isAuto()) {
      return w;
    } else {
      return new WLength(Math.abs(w.getValue()), w.getUnit());
    }
  }
}
