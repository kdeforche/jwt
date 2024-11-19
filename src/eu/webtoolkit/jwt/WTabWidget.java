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
 * A widget that organizes contents in tab panes.
 *
 * <p>This widget combines a horizontal {@link WMenu} with a {@link WStackedWidget}, and a tab-like
 * look.
 *
 * <p>A tab widget will place the tab bar on top of the contents, and fit the contents below it.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * WTabWidget examples = new WTabWidget(this);
 *
 * examples.addTab(helloWorldExample(), "Hello World");
 * examples.addTab(chartExample(), "Charts");
 * examples.addTab(new WText("A WText"), "WText");
 *
 * examples.currentChanged().addListener(this, new Signal.Listener(){
 * public void trigger() {
 * //custom code
 * }
 * });
 * examples.setInternalPathEnabled();
 * examples.setInternalBasePath("/examples");
 *
 * }</pre>
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>The tab widget is styled by the current CSS theme.
 *
 * <p>
 *
 * <table border="1" cellspacing="3" cellpadding="3">
 * <tr><td><div align="center">
 * <img src="doc-files/WTabWidget-default-1.png">
 * <p>
 * <strong>An example WTabWidget (default)</strong></p>
 * </div>
 *
 *
 * </td><td><div align="center">
 * <img src="doc-files/WTabWidget-polished-1.png">
 * <p>
 * <strong>An example WTabWidget (polished)</strong></p>
 * </div>
 *
 *
 * </td></tr>
 * </table>
 */
public class WTabWidget extends WCompositeWidget {
  private static Logger logger = LoggerFactory.getLogger(WTabWidget.class);

  /** Creates a new tab widget. */
  public WTabWidget(WContainerWidget parentContainer) {
    super();
    this.currentChanged_ = new Signal1<Integer>();
    this.tabClosed_ = new Signal1<Integer>();
    this.contentsWidgets_ = new ArrayList<WWidget>();
    this.create();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new tab widget.
   *
   * <p>Calls {@link #WTabWidget(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WTabWidget() {
    this((WContainerWidget) null);
  }
  /**
   * Adds a new tab, with <i>child</i> as content, and the given label.
   *
   * <p>Returns the menu item that implements the tab item.
   */
  public WMenuItem addTab(WWidget child, final CharSequence label, ContentLoading loadPolicy) {
    return this.insertTab(this.getCount(), child, label, loadPolicy);
  }
  /**
   * Adds a new tab, with <i>child</i> as content, and the given label.
   *
   * <p>Returns {@link #addTab(WWidget child, CharSequence label, ContentLoading loadPolicy)
   * addTab(child, label, ContentLoading.Lazy)}
   */
  public final WMenuItem addTab(WWidget child, final CharSequence label) {
    return addTab(child, label, ContentLoading.Lazy);
  }
  /**
   * Inserts a new tab, with <i>child</i> as content, and the given label.
   *
   * <p>Returns the menu item that implements the tab item.
   */
  public WMenuItem insertTab(
      int index, WWidget child, final CharSequence label, ContentLoading loadPolicy) {
    this.contentsWidgets_.add(0 + index, child);
    WMenuItem item = new WMenuItem(label, child, loadPolicy);
    WMenuItem result = item;
    this.menu_.insertItem(index, item);
    return result;
  }
  /**
   * Inserts a new tab, with <i>child</i> as content, and the given label.
   *
   * <p>Returns {@link #insertTab(int index, WWidget child, CharSequence label, ContentLoading
   * loadPolicy) insertTab(index, child, label, ContentLoading.Lazy)}
   */
  public final WMenuItem insertTab(int index, WWidget child, final CharSequence label) {
    return insertTab(index, child, label, ContentLoading.Lazy);
  }
  /**
   * Removes a tab item.
   *
   * <p>
   *
   * @see WMenu#removeItem(WMenuItem item)
   */
  public WWidget removeTab(WWidget child) {
    int tabIndex = this.getIndexOf(child);
    if (tabIndex != -1) {
      this.contentsWidgets_.remove(0 + tabIndex);
      WMenuItem item = this.menu_.itemAt(tabIndex);
      WWidget result = item.getRemoveContents();
      {
        WMenuItem toRemove = this.menu_.removeItem(item);
        if (toRemove != null) toRemove.remove();
      }

      return result;
    } else {
      return null;
    }
  }
  /** Returns the number of tabs. */
  public int getCount() {
    return this.contentsWidgets_.size();
  }
  /** Returns the content widget at the given tab <i>index</i>. */
  public WWidget getWidget(int index) {
    return this.contentsWidgets_.get(index);
  }
  /** Returns the item at the given tab <i>index</i>. */
  public WMenuItem getItemAt(int index) {
    return this.menu_.itemAt(index);
  }
  /**
   * Returns the index of the tab of the given content widget.
   *
   * <p>If the widget is not in this tab widget, then -1 is returned.
   */
  public int getIndexOf(WWidget widget) {
    return this.contentsWidgets_.indexOf(widget);
  }
  /** Activates the tab at <i>index</i>. */
  public void setCurrentIndex(int index) {
    this.menu_.select(index);
  }
  /** Returns the index of the activated tab. */
  public int getCurrentIndex() {
    return this.menu_.getCurrentIndex();
  }
  /** Activates the tab showing the given <i>widget</i> */
  public void setCurrentWidget(WWidget widget) {
    this.setCurrentIndex(this.getIndexOf(widget));
  }
  /** Returns the widget of the activated tab. */
  public WWidget getCurrentWidget() {
    return this.menu_.getCurrentItem().getContents();
  }
  /** Returns the item of the activated tab. */
  public WMenuItem getCurrentItem() {
    return this.menu_.getCurrentItem();
  }
  /**
   * Enables or disables a tab.
   *
   * <p>Enables or disables the tab at <code>index</code>. A disabled tab cannot be activated.
   */
  public void setTabEnabled(int index, boolean enable) {
    this.menu_.setItemDisabled(index, !enable);
  }
  /** Returns whether a tab is enabled. */
  public boolean isTabEnabled(int index) {
    return !this.menu_.isItemDisabled(index);
  }
  /**
   * Hides or shows a tab.
   *
   * <p>Hides or shows the tab at <code>index</code>.
   */
  public void setTabHidden(int index, boolean hidden) {
    this.menu_.setItemHidden(index, hidden);
  }
  /** Returns whether a tab is hidden. */
  public boolean isTabHidden(int index) {
    return this.menu_.isItemHidden(index);
  }
  /**
   * Make it possible to close a tab interactively or by {@link WTabWidget#closeTab(int index)
   * closeTab()}.
   *
   * <p>A tab that has been closed is marked as hidden, but not removed from the menu.
   *
   * <p>
   *
   * @see WTabWidget#removeTab(WWidget child)
   */
  public void setTabCloseable(int index, boolean closeable) {
    this.menu_.itemAt(index).setCloseable(closeable);
  }
  /**
   * Returns whether a tab is closeable.
   *
   * <p>
   *
   * @see WTabWidget#setTabCloseable(int index, boolean closeable)
   */
  public boolean isTabCloseable(int index) {
    return this.menu_.itemAt(index).isCloseable();
  }
  /** Changes the label for a tab. */
  public void setTabText(int index, final CharSequence label) {
    WMenuItem item = this.menu_.itemAt(index);
    item.setText(label);
  }
  /**
   * Returns the label for a tab.
   *
   * <p>
   *
   * @see WTabWidget#setTabText(int index, CharSequence label)
   */
  public WString getTabText(int index) {
    WMenuItem item = this.menu_.itemAt(index);
    return item.getText();
  }
  /**
   * Sets the tooltip for a tab.
   *
   * <p>The tooltip is shown when the user hovers over the label.
   */
  public void setTabToolTip(int index, final CharSequence tip) {
    WMenuItem item = this.menu_.itemAt(index);
    item.setToolTip(tip);
  }
  /**
   * Returns the tooltip for a tab.
   *
   * <p>
   *
   * @see WTabWidget#setTabToolTip(int index, CharSequence tip)
   */
  public WString getTabToolTip(int index) {
    WMenuItem item = this.menu_.itemAt(index);
    return item.getToolTip();
  }
  /**
   * Enables internal paths for items.
   *
   * <p>The menu participates in the internal path by changing the internal path when an item has
   * been selected, and listening for path changes to react to path selections. As a consequence
   * this allows the user to bookmark the current menu selection and revisit it later, use
   * back/forward buttons to navigate through history of visited menu items, and allows indexing of
   * pages.
   *
   * <p>For each menu item, {@link WMenuItem#getPathComponent()} is appended to the <code>basePath
   * </code>, which defaults to the internal path ({@link WApplication#getInternalPath()}). A
   * &apos;/&apos; is appended to the base path, to turn it into a folder, if needed.
   *
   * <p>By default, menu interaction does not change the application internal path.
   *
   * <p>
   *
   * @see WMenuItem#setPathComponent(String path)
   */
  public void setInternalPathEnabled(final String basePath) {
    this.menu_.setInternalPathEnabled(basePath);
  }
  /**
   * Enables internal paths for items.
   *
   * <p>Calls {@link #setInternalPathEnabled(String basePath) setInternalPathEnabled("")}
   */
  public final void setInternalPathEnabled() {
    setInternalPathEnabled("");
  }
  /**
   * Returns whether internal paths are enabled.
   *
   * <p>
   *
   * @see WTabWidget#setInternalPathEnabled(String basePath)
   */
  public boolean isInternalPathEnabled() {
    return this.menu_.isInternalPathEnabled();
  }
  /**
   * Sets the internal base path.
   *
   * <p>A &apos;/&apos; is appended to turn it into a folder, if needed.
   *
   * <p>
   *
   * @see WTabWidget#setInternalPathEnabled(String basePath)
   * @see WTabWidget#getInternalBasePath()
   */
  public void setInternalBasePath(final String path) {
    this.menu_.setInternalBasePath(path);
  }
  /**
   * Returns the internal base path.
   *
   * <p>The default value is the application&apos;s internalPath ({@link
   * WApplication#getInternalPath()}) that was recorded when {@link
   * WTabWidget#setInternalPathEnabled(String basePath) setInternalPathEnabled()} was called, and
   * together with each {@link WMenuItem#getPathComponent()} determines the paths for each item.
   *
   * <p>For example, if {@link WTabWidget#getInternalBasePath() getInternalBasePath()} is <code>
   * &quot;/examples/&quot;</code> and pathComponent() for a particular item is <code>
   * &quot;charts/&quot;</code>, then the internal path for that item will be <code>
   * &quot;/examples/charts/&quot;</code>.
   *
   * <p>
   *
   * @see WTabWidget#setInternalPathEnabled(String basePath)
   */
  public String getInternalBasePath() {
    return this.menu_.getInternalBasePath();
  }
  /**
   * Signal emitted when the user activates a tab.
   *
   * <p>The index of the newly activated tab is passed as an argument.
   */
  public Signal1<Integer> currentChanged() {
    return this.currentChanged_;
  }
  /**
   * Closes a tab at <code>index</code>.
   *
   * <p>A tab that has been closed is marked as hidden, but not removed from the menu.
   *
   * <p>
   *
   * @see WTabWidget#removeTab(WWidget child)
   * @see WTabWidget#setTabHidden(int index, boolean hidden)
   */
  public void closeTab(int index) {
    this.setTabHidden(index, true);
    this.tabClosed_.trigger(index);
  }
  /**
   * Signal emitted when the user closes a tab.
   *
   * <p>The index of the closed tab is passed as an argument.
   *
   * <p>
   *
   * @see WTabWidget#closeTab(int index)
   * @see WTabWidget#setTabCloseable(int index, boolean closeable)
   */
  public Signal1<Integer> tabClosed() {
    return this.tabClosed_;
  }
  /**
   * Returns the contents stack.
   *
   * <p>The tab widget is implemented as a {@link WMenu} + {@link WStackedWidget} which displays the
   * contents. This method returns a reference to this contents stack.
   */
  public WStackedWidget getContentsStack() {
    return this.menu_.getContentsStack();
  }
  /** Sets how overflow of contained children must be handled. */
  public void setOverflow(Overflow value, EnumSet<Orientation> orientation) {
    this.layout_.setOverflow(value, orientation);
  }
  /**
   * Sets how overflow of contained children must be handled.
   *
   * <p>Calls {@link #setOverflow(Overflow value, EnumSet orientation) setOverflow(value,
   * EnumSet.of(orientation, orientation))}
   */
  public final void setOverflow(
      Overflow value, Orientation orientatio, Orientation... orientation) {
    setOverflow(value, EnumSet.of(orientatio, orientation));
  }
  /**
   * Sets how overflow of contained children must be handled.
   *
   * <p>Calls {@link #setOverflow(Overflow value, EnumSet orientation) setOverflow(value, EnumSet.of
   * (Orientation.Horizontal, Orientation.Vertical))}
   */
  public final void setOverflow(Overflow value) {
    setOverflow(value, EnumSet.of(Orientation.Horizontal, Orientation.Vertical));
  }

  private Signal1<Integer> currentChanged_;
  private Signal1<Integer> tabClosed_;
  private WContainerWidget layout_;
  private WMenu menu_;
  private List<WWidget> contentsWidgets_;

  private void create() {
    this.layout_ = new WContainerWidget();
    this.setImplementation(this.layout_);
    WStackedWidget stack = new WStackedWidget();
    this.menu_ = new WMenu(stack, (WContainerWidget) null);
    this.layout_.addWidget(this.menu_);
    this.layout_.addWidget(stack);
    this.setJavaScriptMember(WT_RESIZE_JS, StdWidgetItemImpl.getSecondResizeJS());
    this.setJavaScriptMember(WT_GETPS_JS, StdWidgetItemImpl.getSecondGetPSJS());
    this.menu_
        .itemSelected()
        .addListener(
            this,
            (WMenuItem e1) -> {
              WTabWidget.this.onItemSelected(e1);
            });
    this.menu_
        .itemClosed()
        .addListener(
            this,
            (WMenuItem e1) -> {
              WTabWidget.this.onItemClosed(e1);
            });
  }

  private void onItemSelected(WMenuItem item) {
    this.currentChanged_.trigger(this.menu_.getCurrentIndex());
  }

  private void onItemClosed(WMenuItem item) {
    this.closeTab(this.menu_.indexOf(item));
  }
  // private void setJsSize() ;
}
