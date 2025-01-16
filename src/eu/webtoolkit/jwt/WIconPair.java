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
 * A widget that shows one of two icons depending on its state.
 *
 * <p>This is a utility class that simply manages two images, only one of which is shown at a single
 * time, which reflects the current &apos;state&apos;.
 *
 * <p>The widget may react to click events, by changing state.
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>This widget does not provide styling, and can be styled using inline or external CSS as
 * appropriate. The image may be styled via the <code>&lt;img&gt;</code> elements.
 */
public class WIconPair extends WCompositeWidget {
  private static Logger logger = LoggerFactory.getLogger(WIconPair.class);

  /**
   * An enumeration describing a type of icon.
   *
   * <p>Each icon type describe how the string representing the icon should be interpreted.
   * Depending on the interpretation the icon is looked for in different places, or within different
   * resources.
   *
   * <p>Under <code>URI</code>, a static image is expected. This can be linked in many ways, using
   * absolute paths, relative paths, or even external resources. The images can thus be served by
   * JWt itself (in its docroot), or be retrieved from another server.
   *
   * <p>With <code>IconName</code>, a simple string is expected, matching the format of the Font
   * Awesome version you are using. In this case it is required to include the stylesheet ({@link
   * WApplication#useStyleSheet(WLink link, String media) WApplication#useStyleSheet()}) in your
   * application before this can be used.
   */
  public enum IconType {
    /** The URI of an image, linking to a static resource. */
    URI,
    /** The name of a Font Awesome icon, using an external bundle. */
    IconName;

    /** Returns the numerical representation of this enum. */
    public int getValue() {
      return ordinal();
    }
  }
  /**
   * Construct an icon pair from the two icons.
   *
   * <p>The constructor takes the URL or the Font Awesome name of the two icons. When <code>
   * clickIsSwitch</code> is set <code>true</code>, clicking on the icon will switch state.
   *
   * <p>
   *
   * <p><i><b>Note: </b>The Font Aweswome name can be followed by sizing information separated by a
   * space if the Font Aweswome version used allows it. A valid icon can look like this for example:
   * <code>&quot;fa-solid fa-camera fa-1x&quot;</code>, or <code>
   * &quot;fa-solid fa-camera fa-sm&quot;</code> (valid for version v6.7.2). More sizing information
   * can be found on <a
   * href="https://docs.fontawesome.com/web/style/size">https://docs.fontawesome.com/web/style/size</a>.
   * </i>
   *
   * <p><i><b>Warning: </b>By default, the strings are considered to represent the URIs of the
   * icons. Use {@link WIconPair#setIcon1Type(WIconPair.IconType type) setIcon1Type()} and {@link
   * WIconPair#setIcon2Type(WIconPair.IconType type) setIcon2Type()} to change the IconType
   * accordingly if it is not what the string represents. Both can be set at once using
   * setIconTypes(). </i>
   *
   * @see WIconPair#setIcon1Type(WIconPair.IconType type)
   * @see WIconPair#setIcon2Type(WIconPair.IconType type)
   */
  public WIconPair(
      final String icon1Str,
      final String icon2Str,
      boolean clickIsSwitch,
      WContainerWidget parentContainer) {
    super();
    this.clickIsSwitch_ = clickIsSwitch;
    this.impl_ = new WContainerWidget();
    this.iconStr_[0] = icon1Str;
    this.iconStr_[1] = icon2Str;
    this.wicon_[0] = null;
    this.wicon_[1] = null;
    this.setImplementation(this.impl_);
    this.image_[0] = new WImage(new WLink(icon1Str));
    this.impl_.addWidget(this.image_[0]);
    this.image_[1] = new WImage(new WLink(icon2Str));
    this.impl_.addWidget(this.image_[1]);
    this.impl_.setLoadLaterWhenInvisible(false);
    this.setInline(true);
    this.image_[1].hide();
    if (clickIsSwitch) {
      this.image_[0].clicked().preventPropagation();
      this.image_[1].clicked().preventPropagation();
      this.image_[0]
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WIconPair.this.showIcon2();
              });
      this.image_[1]
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WIconPair.this.showIcon1();
              });
      this.getDecorationStyle().setCursor(Cursor.PointingHand);
    }
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Construct an icon pair from the two icons.
   *
   * <p>Calls {@link #WIconPair(String icon1Str, String icon2Str, boolean clickIsSwitch,
   * WContainerWidget parentContainer) this(icon1Str, icon2Str, true, (WContainerWidget)null)}
   */
  public WIconPair(final String icon1Str, final String icon2Str) {
    this(icon1Str, icon2Str, true, (WContainerWidget) null);
  }
  /**
   * Construct an icon pair from the two icons.
   *
   * <p>Calls {@link #WIconPair(String icon1Str, String icon2Str, boolean clickIsSwitch,
   * WContainerWidget parentContainer) this(icon1Str, icon2Str, clickIsSwitch,
   * (WContainerWidget)null)}
   */
  public WIconPair(final String icon1Str, final String icon2Str, boolean clickIsSwitch) {
    this(icon1Str, icon2Str, clickIsSwitch, (WContainerWidget) null);
  }
  /**
   * Sets the IconType of the first icon.
   *
   * <p>
   *
   * @see WIconPair#setIconsType(WIconPair.IconType type)
   */
  public void setIcon1Type(WIconPair.IconType type) {
    this.resetIcon(0, type);
    if (this.clickIsSwitch_) {
      this.getUsedIcon1()
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WIconPair.this.showIcon2();
              });
    }
  }
  /**
   * Sets the IconType of the second icon.
   *
   * <p>
   *
   * @see WIconPair#setIconsType(WIconPair.IconType type)
   */
  public void setIcon2Type(WIconPair.IconType type) {
    this.resetIcon(1, type);
    if (this.clickIsSwitch_) {
      this.getUsedIcon2()
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WIconPair.this.showIcon1();
              });
    }
  }
  /**
   * Sets the IconType of the both icons.
   *
   * <p>Sets the IconType of both icons. The icon type should be {@link WIconPair.IconType#URI URI}
   * if the URI of the icon was given at construction. If the name of a Font Awesome icon was given,
   * the icon type should be set to {@link WIconPair.IconType#IconName IconName} instead.
   *
   * <p>By default this will be set to {@link WIconPair.IconType#URI URI}.
   *
   * <p>
   *
   * @see WIconPair#setIcon1Type(WIconPair.IconType type)
   * @see WIconPair#setIcon2Type(WIconPair.IconType type)
   */
  public void setIconsType(WIconPair.IconType type) {
    this.setIcon1Type(type);
    this.setIcon2Type(type);
  }
  /**
   * Sets the state, which determines the visible icon.
   *
   * <p>The first icon has number 0, and the second icon has number 1.
   *
   * <p>The default state is 0.
   *
   * <p>
   *
   * @see WIconPair#getState()
   */
  public void setState(int num) {
    if (num == 0) {
      this.getUsedIcon1().show();
      this.getUsedIcon2().hide();
    } else {
      this.getUsedIcon1().hide();
      this.getUsedIcon2().show();
    }
  }
  /**
   * Returns the current state.
   *
   * <p>
   *
   * @see WIconPair#setState(int num)
   */
  public int getState() {
    return this.getUsedIcon1().isHidden() ? 1 : 0;
  }

  public WImage getIcon1() {
    return this.getUriIcon1();
  }

  public WImage getIcon2() {
    return this.getUriIcon2();
  }
  /**
   * Returns the first icon as {@link WImage}.
   *
   * <p>If first icon type is {@link WIconPair.IconType#URI URI} returns the first icon as {@link
   * WImage}, otherwise returns nullptr.
   *
   * <p>
   *
   * @see WIconPair#getIconNameIcon1()
   */
  public WImage getUriIcon1() {
    return this.image_[0];
  }
  /**
   * Returns the second icon as {@link WImage}.
   *
   * <p>If second icon type is {@link WIconPair.IconType#URI URI} returns the second icon as {@link
   * WImage}, otherwise returns nullptr.
   *
   * <p>
   *
   * @see WIconPair#getIconNameIcon2()
   */
  public WImage getUriIcon2() {
    return this.image_[1];
  }
  /**
   * Returns the first icon as WIcon.
   *
   * <p>If first icon type is {@link WIconPair.IconType#IconName IconName} returns the first icon as
   * WIcon, otherwise returns nullptr.
   *
   * <p>
   *
   * @see WIconPair#getUriIcon1()
   */
  public WIcon getIconNameIcon1() {
    return this.wicon_[0];
  }
  /**
   * Returns the second icon as WIcon.
   *
   * <p>If second icon type is {@link WIconPair.IconType#IconName IconName} returns the second icon
   * as WIcon, otherwise returns nullptr.
   *
   * <p>
   *
   * @see WIconPair#getUriIcon2()
   */
  public WIcon getIconNameIcon2() {
    return this.wicon_[1];
  }
  /**
   * Sets the state to 0 (show icon 1).
   *
   * <p>
   *
   * @see WIconPair#setState(int num)
   */
  public void showIcon1() {
    this.setState(0);
  }
  /**
   * Sets the state to 1 (show icon 2).
   *
   * <p>
   *
   * @see WIconPair#setState(int num)
   */
  public void showIcon2() {
    this.setState(1);
  }
  /**
   * Signal emitted when clicked while in state 0 (icon 1 is shown).
   *
   * <p>Equivalent to:
   *
   * <pre>{@code
   * icon1().clicked()
   *
   * }</pre>
   */
  public EventSignal1<WMouseEvent> icon1Clicked() {
    return this.getUsedIcon1().clicked();
  }
  /**
   * Signal emitted when clicked while in state 1 (icon 2 is shown).
   *
   * <p>Equivalent to:
   *
   * <pre>{@code
   * icon2().clicked()
   *
   * }</pre>
   */
  public EventSignal1<WMouseEvent> icon2Clicked() {
    return this.getUsedIcon2().clicked();
  }

  private final boolean clickIsSwitch_;
  private WContainerWidget impl_;
  private String[] iconStr_ = new String[2];
  private WIcon[] wicon_ = new WIcon[2];
  private WImage[] image_ = new WImage[2];

  private WInteractWidget usedIcon(int i) {
    if (this.wicon_[i] != null) {
      return this.wicon_[i];
    }
    return this.image_[i];
  }

  private WInteractWidget getUsedIcon1() {
    return this.usedIcon(0);
  }

  private WInteractWidget getUsedIcon2() {
    return this.usedIcon(1);
  }

  private void resetIcon(int i, WIconPair.IconType type) {
    int currentState = this.getState();
    if (this.wicon_[i] != null) {
      {
        WWidget toRemove = WidgetUtils.remove(this.impl_, this.wicon_[i]);
        if (toRemove != null) toRemove.remove();
      }
    }
    if (this.image_[i] != null) {
      {
        WWidget toRemove = WidgetUtils.remove(this.impl_, this.image_[i]);
        if (toRemove != null) toRemove.remove();
      }
    }
    if (type == WIconPair.IconType.IconName) {
      this.wicon_[i] = new WIcon(this.iconStr_[i]);
      this.impl_.addWidget(this.wicon_[i]);
      this.image_[i] = null;
    } else {
      this.image_[i] = new WImage(new WLink(this.iconStr_[i]));
      this.impl_.addWidget(this.image_[i]);
      this.wicon_[i] = null;
    }
    this.resetLearnedSlots();
    this.setState(currentState);
  }
}
