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
 * A WPanel provides a container with a title bar.
 *
 * <p>The panel provides a container with an optional title bar, and an optional collapse icon.
 *
 * <p><div align="center"> <img src="doc-files/WPanel-default-1.png">
 *
 * <p><strong>Two panels: one collapsed and one expanded (default theme)</strong> </div>
 *
 * <p><div align="center"> <img src="doc-files/WPanel-polished-1.png">
 *
 * <p><strong>Two panels: one collapsed and one expanded (polished theme)</strong> </div>
 */
public class WPanel extends WCompositeWidget {
  private static Logger logger = LoggerFactory.getLogger(WPanel.class);

  /** Creates a panel. */
  public WPanel(WContainerWidget parentContainer) {
    super();
    this.collapseIcon_ = null;
    this.title_ = null;
    this.centralWidget_ = null;
    this.animation_ = new WAnimation();
    this.collapsed_ = new Signal();
    this.expanded_ = new Signal();
    this.collapsedSS_ = new Signal1<Boolean>();
    this.expandedSS_ = new Signal1<Boolean>();
    String TEMPLATE = "${titlebar}${contents}";
    this.impl_ = new WTemplate(new WString(TEMPLATE), (WContainerWidget) null);
    this.setImplementation(this.impl_);
    // this.implementStateless(WPanel.doExpand,WPanel.undoExpand);
    // this.implementStateless(WPanel.doCollapse,WPanel.undoCollapse);
    WContainerWidget centralArea = new WContainerWidget();
    this.impl_.bindEmpty("titlebar");
    this.impl_.bindWidget("contents", centralArea);
    this.setJavaScriptMember(
        WT_RESIZE_JS,
        "function(self, w, h, s) {var hdefined = h >= 0;if (hdefined) {var mh = Wt4.11.2.px(self, 'maxHeight');if (mh > 0) h = Math.min(h, mh);}if (Wt4.11.2.boxSizing(self)) {h -= Wt4.11.2.px(self, 'borderTopWidth') + Wt4.11.2.px(self, 'borderBottomWidth');}var c = self.lastChild;var t = c.previousSibling;if (t)h -= t.offsetHeight;h -= 8;if (hdefined && h > 0) {c.lh = true;c.style.height = h + 'px';c.querySelectorAll(':scope > *').forEach(function(self) { let padding = self.getBoundingClientRect().height - Wt4.11.2.px(self, 'height');self.style.height = (h - padding) + 'px';self.lh = true;});} else {c.style.height = '';c.lh = false;for (const child of c.children) {child.style.height = '';child.lh = false;}}};");
    this.setJavaScriptMember(WT_GETPS_JS, StdWidgetItemImpl.getSecondGetPSJS());
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a panel.
   *
   * <p>Calls {@link #WPanel(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WPanel() {
    this((WContainerWidget) null);
  }
  /**
   * Sets a title.
   *
   * <p>The panel title is set in the title bar. This method also makes the title bar visible by
   * calling setTitleBar(true).
   *
   * <p>The default value is &quot;&quot; (no title).
   *
   * <p>
   *
   * @see WPanel#getTitle()
   * @see WPanel#setTitleBar(boolean enable)
   */
  public void setTitle(final CharSequence title) {
    this.setTitleBar(true);
    if (!(this.title_ != null)) {
      this.title_ = new WText();
      this.getTitleBarWidget().addWidget(this.title_);
    }
    WText text = ObjectUtils.cast(this.title_, WText.class);
    WPushButton button = ObjectUtils.cast(this.title_, WPushButton.class);
    if (text != null) {
      text.setText(title);
    } else {
      if (button != null) {
        button.setText(title);
      }
    }
    WApplication app = WApplication.getInstance();
    app.getTheme().apply(this, this.title_, WidgetThemeRole.PanelTitle);
    app.getTheme().apply(this, this.getTitleBarWidget(), WidgetThemeRole.PanelTitleBar);
  }
  /**
   * Returns the title.
   *
   * <p>
   *
   * @see WPanel#setTitle(CharSequence title)
   */
  public WString getTitle() {
    WText text = ObjectUtils.cast(this.title_, WText.class);
    WPushButton button = ObjectUtils.cast(this.title_, WPushButton.class);
    if (text != null) {
      return text.getText();
    } else {
      if (button != null) {
        return button.getText();
      } else {
        return new WString();
      }
    }
  }
  /**
   * Shows or hides the title bar for the panel.
   *
   * <p>The title bar appears at the top of the panel.
   *
   * <p>The default value is <code>false:</code> the title bar is not shown unless a title is set or
   * the panel is made collapsible.
   *
   * <p>If the title bar is hidden, the panel is automatically made non-collapsible, and expands if
   * it was collapsed.
   *
   * <p>
   *
   * @see WPanel#setTitle(CharSequence title)
   * @see WPanel#setCollapsible(boolean on)
   */
  public void setTitleBar(boolean enable) {
    if (enable && !this.isTitleBar()) {
      this.impl_.bindWidget("titlebar", new WContainerWidget());
    } else {
      if (!enable && this.isTitleBar()) {
        if (this.isCollapsible()) {
          this.setCollapsible(false);
        }
        this.impl_.bindEmpty("titlebar");
        this.title_ = null;
      }
    }
  }
  /**
   * Returns if a title bar is set.
   *
   * <p>
   *
   * @see WPanel#setTitleBar(boolean enable)
   */
  public boolean isTitleBar() {
    return this.getTitleBarWidget() != null;
  }
  /**
   * Returns the title bar widget.
   *
   * <p>The title bar widget contains the collapse/expand icon (if the panel {@link
   * WPanel#isCollapsible() isCollapsible()}), and the title text (if a title was set using {@link
   * WPanel#setTitle(CharSequence title) setTitle()}). You can access the title bar widget to
   * customize the contents of the title.
   *
   * <p>The method returns <code>null</code> if {@link WPanel#isTitleBar() isTitleBar()} is <code>
   * false</code>. You need to call {@link WPanel#setTitleBar(boolean enable) setTitleBar()} first.
   *
   * <p>
   *
   * @see WPanel#setTitleBar(boolean enable)
   */
  public WContainerWidget getTitleBarWidget() {
    return ObjectUtils.cast(this.impl_.resolveWidget("titlebar"), WContainerWidget.class);
  }
  /**
   * Makes the panel collapsible.
   *
   * <p>When <code>on</code> is <code>true</code>, a collapse/expand icon is added to the title bar.
   * This also calls setTitleBar(true) to enable the title bar.
   *
   * <p>Additionally, the Wt-collapsible class is added to the panel.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>
   *
   * @see WPanel#setTitleBar(boolean enable)
   * @see WPanel#setCollapsed(boolean on)
   * @see WPanel#isCollapsed()
   */
  public void setCollapsible(boolean on) {
    this.toggleStyleClass("Wt-collapsible", on);
    if (on && !this.isCollapsible()) {
      String resources = WApplication.getRelativeResourcesUrl();
      this.setTitleBar(true);
      WApplication app = WApplication.getInstance();
      WIconPair icon = new WIconPair(resources + "collapse.gif", resources + "expand.gif");
      this.collapseIcon_ = icon;
      if (app.getTheme().getPanelCollapseIconSide() == Side.Left) {
        this.getTitleBarWidget().insertWidget(0, icon);
      } else {
        this.getTitleBarWidget().addWidget(icon);
      }
      this.collapseIcon_
          .icon1Clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WPanel.this.doCollapse();
              });
      this.collapseIcon_
          .icon1Clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WPanel.this.onCollapse();
              });
      this.collapseIcon_.icon1Clicked().preventPropagation();
      this.collapseIcon_
          .icon2Clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WPanel.this.doExpand();
              });
      this.collapseIcon_
          .icon2Clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WPanel.this.onExpand();
              });
      this.collapseIcon_.icon2Clicked().preventPropagation();
      this.collapseIcon_.setState(this.isCollapsed() ? 1 : 0);
      app.getTheme().apply(this, this.collapseIcon_, WidgetThemeRole.PanelCollapseButton);
      if (app.getEnvironment().hasAjax()) {
        this.getTitleBarWidget()
            .clicked()
            .addListener(
                this,
                (WMouseEvent e1) -> {
                  WPanel.this.toggleCollapse();
                });
      }
    } else {
      if (!on && this.isCollapsible()) {
        if (this.isCollapsed()) {
          this.setCollapsed(false);
        }
        {
          WWidget toRemove = WidgetUtils.remove(this.getTitleBarWidget(), this.collapseIcon_);
          if (toRemove != null) toRemove.remove();
        }

        this.collapseIcon_ = null;
      }
    }
  }
  /**
   * Returns if the panel can be collapsed by the user.
   *
   * <p>
   *
   * @see WPanel#setCollapsible(boolean on)
   */
  public boolean isCollapsible() {
    return this.collapseIcon_ != null;
  }
  /**
   * Sets the panel expanded or collapsed.
   *
   * <p>When <code>on</code> is <code>true</code>, equivalent to {@link WPanel#collapse()
   * collapse()}, otherwise to {@link WPanel#expand() expand()}.
   *
   * <p>The default value is <code>false</code>.
   *
   * <p>If the panel is collapsed, the Wt-collapsed style class is added.
   *
   * <p>
   *
   * @see WPanel#setCollapsible(boolean on)
   *     <p><i><b>Note: </b>It is possible to make a {@link WPanel} collapsible with {@link
   *     WBootstrap5Theme}, but collapsing and expanding from C++, and the accompanying signals is
   *     not supported. </i>
   */
  public void setCollapsed(boolean on) {
    if (on) {
      this.collapse();
    } else {
      this.expand();
    }
  }
  /**
   * Returns if the panel is collapsed.
   *
   * <p>
   *
   * @see WPanel#setCollapsed(boolean on)
   * @see WPanel#collapsed()
   * @see WPanel#expanded()
   *     <p><i><b>Note: </b>It is possible to make a {@link WPanel} collapsible with {@link
   *     WBootstrap5Theme}, but collapsing and expanding from C++, and the accompanying signals is
   *     not supported. </i>
   */
  public boolean isCollapsed() {
    return this.getCentralArea().isHidden();
  }
  /**
   * Collapses the panel.
   *
   * <p>When {@link WPanel#isCollapsible() isCollapsible()} is true, the panel is collapsed to
   * minimize screen real-estate.
   *
   * <p>
   *
   * @see WPanel#setCollapsible(boolean on)
   * @see WPanel#expand()
   *     <p><i><b>Note: </b>It is possible to make a {@link WPanel} collapsible with {@link
   *     WBootstrap5Theme}, but collapsing and expanding from C++, and the accompanying signals is
   *     not supported. </i>
   */
  public void collapse() {
    if (this.isCollapsible()) {
      this.collapseIcon_.showIcon2();
      this.doCollapse();
    }
  }
  /**
   * Collapses the panel.
   *
   * <p>When {@link WPanel#isCollapsible() isCollapsible()} is true, the panel is expanded to its
   * original state.
   *
   * <p>
   *
   * @see WPanel#setCollapsible(boolean on)
   * @see WPanel#expand()
   *     <p><i><b>Note: </b>It is possible to make a {@link WPanel} collapsible with {@link
   *     WBootstrap5Theme}, but collapsing and expanding from C++, and the accompanying signals is
   *     not supported. </i>
   */
  public void expand() {
    if (this.isCollapsible()) {
      this.collapseIcon_.showIcon1();
      this.doExpand();
    }
  }
  /**
   * Sets an animation.
   *
   * <p>The animation is used when collapsing or expanding the panel.
   *
   * <p>
   *
   * <p><i><b>Note: </b>It is possible to make a {@link WPanel} collapsible with {@link
   * WBootstrap5Theme}, but it&apos;s not possible to set the animation. </i>
   */
  public void setAnimation(final WAnimation transition) {
    this.animation_ = transition;
    if (!this.animation_.isEmpty()) {
      this.addStyleClass("Wt-animated");
    }
  }
  /**
   * Sets the central widget.
   *
   * <p>Sets the widget that is the contents of the panel.
   *
   * <p>The default value is <code>null</code> (no widget set).
   */
  public void setCentralWidget(WWidget w) {
    if (this.centralWidget_ != null) {
      {
        WWidget toRemove = this.getCentralArea().removeWidget(this.centralWidget_);
        if (toRemove != null) toRemove.remove();
      }

      this.centralWidget_ = null;
    }
    if (w != null) {
      this.centralWidget_ = w;
      this.centralWidget_.setInline(false);
      this.getCentralArea().addWidget(w);
      WApplication app = WApplication.getInstance();
      app.getTheme().apply(this, this.getCentralArea(), WidgetThemeRole.PanelBody);
      app.getTheme().apply(this, this.centralWidget_, WidgetThemeRole.PanelBodyContent);
    }
  }
  // public Widget  setCentralWidget(<Woow... some pseudoinstantiation type!> widget) ;
  /**
   * Returns the central widget.
   *
   * <p>
   *
   * @see WPanel#setCentralWidget(WWidget w)
   */
  public WWidget getCentralWidget() {
    return this.centralWidget_;
  }
  /**
   * Signal emitted when the panel is collapsed.
   *
   * <p>Signal emitted when the panel is collapsed. The signal is only emitted when the panel is
   * collapsed by the user using the collapse icon in the tible bar, not when calling {@link
   * WPanel#setCollapsed(boolean on) setCollapsed()}.
   *
   * <p>
   *
   * @see WPanel#expanded()
   *     <p><i><b>Note: </b>It is possible to make a {@link WPanel} collapsible with {@link
   *     WBootstrap5Theme}, but it&apos;s not possible to set the animation. </i>
   */
  public Signal collapsed() {
    return this.collapsed_;
  }
  /**
   * Signal emitted when the panel is expanded.
   *
   * <p>Signal emitted when the panel is expanded. The signal is only emitted when the panel is
   * expanded by the user using the expand icon in the title bar, not when calling {@link
   * WPanel#setCollapsed(boolean on) setCollapsed()}.
   *
   * <p>
   *
   * @see WPanel#collapsed()
   *     <p><i><b>Note: </b>It is possible to make a {@link WPanel} collapsible with {@link
   *     WBootstrap5Theme}, but it&apos;s not possible to set the animation. </i>
   */
  public Signal expanded() {
    return this.expanded_;
  }

  Signal1<Boolean> collapsedSS() {
    return this.collapsedSS_;
  }

  Signal1<Boolean> expandedSS() {
    return this.expandedSS_;
  }

  WIconPair getCollapseIcon() {
    return this.collapseIcon_;
  }

  protected void enableAjax() {
    super.enableAjax();
    if (this.isCollapsible()) {
      this.getTitleBarWidget()
          .clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WPanel.this.toggleCollapse();
              });
    }
  }

  private WIconPair collapseIcon_;
  private WWidget title_;
  private WTemplate impl_;
  private WWidget centralWidget_;
  private WAnimation animation_;
  private Signal collapsed_;
  private Signal expanded_;
  private Signal1<Boolean> collapsedSS_;
  private Signal1<Boolean> expandedSS_;
  private boolean wasCollapsed_;

  private void toggleCollapse() {
    this.setCollapsed(!this.isCollapsed());
    if (this.isCollapsed()) {
      this.collapsed_.trigger();
    } else {
      this.expanded_.trigger();
    }
  }

  private void doExpand() {
    this.wasCollapsed_ = this.isCollapsed();
    this.removeStyleClass("Wt-collapsed", true);
    this.getCentralArea().animateShow(this.animation_);
    this.expandedSS_.trigger(true);
  }

  private void doCollapse() {
    this.wasCollapsed_ = this.isCollapsed();
    this.addStyleClass("Wt-collapsed", true);
    this.getCentralArea().animateHide(this.animation_);
    this.collapsedSS_.trigger(true);
  }

  private void undoExpand() {
    if (this.wasCollapsed_) {
      this.collapse();
    }
    this.expandedSS_.trigger(false);
  }

  private void undoCollapse() {
    if (!this.wasCollapsed_) {
      this.expand();
    }
    this.collapsedSS_.trigger(false);
  }

  private void onExpand() {
    this.expanded_.trigger();
  }

  private void onCollapse() {
    this.collapsed_.trigger();
  }

  private WContainerWidget getCentralArea() {
    return ObjectUtils.cast(this.impl_.resolveWidget("contents"), WContainerWidget.class);
  }
}
