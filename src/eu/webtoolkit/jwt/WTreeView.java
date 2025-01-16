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
 * A view class that displays a model as a tree or tree table.
 *
 * <p>The view displays data from a {@link WAbstractItemModel} in a tree or tree table. It provides
 * incremental rendering, allowing the display of data models of any size efficiently, without
 * excessive use of client- or serverside resources.
 *
 * <p>The rendering (and editing) of items is handled by a {@link WAbstractItemDelegate}, by default
 * it uses {@link WItemDelegate} which renders data of all predefined roles (see also {@link
 * ItemDataRole}), including text, icons, checkboxes, and tooltips.
 *
 * <p>The view may support editing of items, if the model indicates support (see the {@link
 * ItemFlag#Editable} flag). You can define triggers that initiate editing of an item using {@link
 * WAbstractItemView#setEditTriggers(EnumSet editTriggers) WAbstractItemView#setEditTriggers()}. The
 * actual editing is provided by the item delegate (you can set an appropriate delegate for one
 * column using {@link WAbstractItemView#setItemDelegateForColumn(int column, WAbstractItemDelegate
 * delegate) WAbstractItemView#setItemDelegateForColumn()}). Using {@link
 * WAbstractItemView#setEditOptions(EnumSet editOptions) WAbstractItemView#setEditOptions()} you can
 * customize if and how the view deals with multiple editors.
 *
 * <p>By default, all but the first columns are given a width of 150px, and the first column takes
 * the remaining size. <b>Note that this may have as consequence that the first column&apos;s size
 * is reduced to 0.</b> Column widths of all columns, including the first column, can be set through
 * the API method {@link WTreeView#setColumnWidth(int column, WLength width) setColumnWidth()}, and
 * also by the user using handles provided in the header.
 *
 * <p>Optionally, the treeview may be configured so that the first column is always visible while
 * scrolling through the other columns, which may be convenient if you wish to display a model with
 * many columns. Use setColumn1Fixed() to enable this behaviour.
 *
 * <p>If the model supports sorting ({@link WAbstractItemModel#sort(int column, SortOrder order)
 * WAbstractItemModel#sort()}), such as the {@link WStandardItemModel}, then you can enable sorting
 * buttons in the header, using {@link WAbstractItemView#setSortingEnabled(boolean enabled)
 * WAbstractItemView#setSortingEnabled()}.
 *
 * <p>You can allow selection on row or item level (using {@link
 * WAbstractItemView#setSelectionBehavior(SelectionBehavior behavior)
 * WAbstractItemView#setSelectionBehavior()}), and selection of single or multiple items (using
 * {@link WAbstractItemView#setSelectionMode(SelectionMode mode)
 * WAbstractItemView#setSelectionMode()}), and listen for changes in the selection using the {@link
 * WAbstractItemView#selectionChanged()} signal.
 *
 * <p>You may enable drag &amp; drop support for this view, with awareness of the items in the
 * model. When enabling dragging (see {@link WAbstractItemView#setDragEnabled(boolean enable)
 * WAbstractItemView#setDragEnabled()}), the current selection may be dragged, but only when all
 * items in the selection indicate support for dragging (controlled by the {@link
 * ItemFlag#DragEnabled} flag), and if the model indicates a mime-type (controlled by {@link
 * WAbstractItemModel#getMimeType()}). Likewise, by enabling support for dropping (see {@link
 * WAbstractItemView#setDropsEnabled(boolean enable) WAbstractItemView#setDropsEnabled()}), the
 * treeview may receive a drop event on a particular item, at least if the item indicates support
 * for drops (controlled by the {@link ItemFlag#DropEnabled} flag).
 *
 * <p>You may also react to mouse click events on any item, by connecting to one of the {@link
 * WAbstractItemView#clicked()} or {@link WAbstractItemView#doubleClicked()} signals.
 *
 * <p>
 *
 * <h3>Graceful degradation</h3>
 *
 * <p>The view provides a virtual scrolling behavior which relies on Ajax availability. When Ajax is
 * not available, a page navigation bar is used instead, see {@link
 * WAbstractItemView#getCreatePageNavigationBar()}. In that case, the widget needs to be given an
 * explicit height using {@link WTreeView#resize(WLength width, WLength height) resize()} which
 * determines the number of rows that are displayed at a time.
 *
 * <p>A snapshot of the {@link WTreeView}: <div align="center"> <img
 * src="doc-files/WTreeView-default-1.png">
 *
 * <p><strong>WTreeView example (default)</strong> </div>
 *
 * <p><div align="center"> <img src="doc-files/WTreeView-polished-1.png">
 *
 * <p><strong>WTreeView example (polished)</strong> </div>
 */
public class WTreeView extends WAbstractItemView {
  private static Logger logger = LoggerFactory.getLogger(WTreeView.class);

  /** Creates a new tree view. */
  public WTreeView(WContainerWidget parentContainer) {
    super();
    this.skipNextMouseEvent_ = false;
    this.expandedSet_ = new HashSet<WModelIndex>();
    this.renderedNodes_ = new HashMap<WModelIndex, WTreeViewNode>();
    this.renderedNodesAdded_ = false;
    this.rootNode_ = null;
    this.rowHeightRule_ = null;
    this.rowWidthRule_ = null;
    this.rowContentsWidthRule_ = null;
    this.c0StyleRule_ = null;
    this.rootIsDecorated_ = true;
    this.collapsed_ = new Signal1<WModelIndex>();
    this.expanded_ = new Signal1<WModelIndex>();
    this.viewportTop_ = 0;
    this.viewportHeight_ = 30;
    this.firstRenderedRow_ = 0;
    this.validRowCount_ = 0;
    this.nodeLoad_ = 0;
    this.headerContainer_ = null;
    this.contentsContainer_ = null;
    this.scrollBarC_ = null;
    this.firstRemovedRow_ = 0;
    this.removedHeight_ = 0;
    this.itemEvent_ =
        new JSignal5<String, String, String, String, WMouseEvent>(this.impl_, "itemEvent") {};
    this.itemTouchEvent_ =
        new JSignal3<String, String, WTouchEvent>(this.impl_, "itemTouchEvent") {};
    this.rowDropEvent_ =
        new JSignal5<String, String, String, String, WMouseEvent>(this.impl_, "rowDropEvent") {};
    this.expandConfig_ = null;
    this.tieRowsScrollJS_ = new JSlot();
    this.itemClickedJS_ = new JSlot();
    this.rootClickedJS_ = new JSlot();
    this.itemDoubleClickedJS_ = new JSlot();
    this.rootDoubleClickedJS_ = new JSlot();
    this.itemMouseDownJS_ = new JSlot();
    this.rootMouseDownJS_ = new JSlot();
    this.itemMouseUpJS_ = new JSlot();
    this.rootMouseUpJS_ = new JSlot();
    this.touchStartedJS_ = new JSlot();
    this.touchMovedJS_ = new JSlot();
    this.touchEndedJS_ = new JSlot();
    this.setSelectable(false);
    this.expandConfig_ = new ToggleButtonConfig(this, "Wt-ctrl rh ");
    this.expandConfig_.addState("expand");
    this.expandConfig_.addState("collapse");
    this.expandConfig_.generate();
    this.setStyleClass("Wt-itemview Wt-treeview");
    String CSS_RULES_NAME = "Wt::WTreeView";
    WApplication app = WApplication.getInstance();
    if (app.getEnvironment().agentIsWebKit() || app.getEnvironment().agentIsOpera()) {
      if (!app.getStyleSheet().isDefined(CSS_RULES_NAME)) {
        this.addCssRule(".Wt-treeview .Wt-tv-rowc", "position: relative;", CSS_RULES_NAME);
      }
    }
    this.setup();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new tree view.
   *
   * <p>Calls {@link #WTreeView(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WTreeView() {
    this((WContainerWidget) null);
  }

  public void remove() {
    WApplication.getInstance().getStyleSheet().removeRule(this.rowHeightRule_);
    WApplication.getInstance().getStyleSheet().removeRule(this.rowWidthRule_);
    WApplication.getInstance().getStyleSheet().removeRule(this.rowContentsWidthRule_);
    WApplication.getInstance().getStyleSheet().removeRule(this.c0StyleRule_);
    this.impl_.clear();
    super.remove();
  }
  /**
   * Expands or collapses a node.
   *
   * <p>
   *
   * @see WTreeView#expand(WModelIndex index)
   * @see WTreeView#collapse(WModelIndex index)
   */
  public void setExpanded(final WModelIndex index, boolean expanded) {
    if (this.isExpanded(index) != expanded) {
      WWidget w = this.widgetForIndex(index);
      WTreeViewNode node = w != null ? ObjectUtils.cast(w, WTreeViewNode.class) : null;
      if (node != null) {
        if (expanded) {
          node.doExpand();
        } else {
          node.doCollapse();
        }
      } else {
        int height = this.subTreeHeight(index);
        if (expanded) {
          this.expandedSet_.add(index);
        } else {
          this.setCollapsed(index);
        }
        if (w != null) {
          RowSpacer spacer = ObjectUtils.cast(w, RowSpacer.class);
          int diff = this.subTreeHeight(index) - height;
          spacer.setRows(spacer.getRows() + diff);
          spacer.getNode().adjustChildrenHeight(diff);
          this.renderedRowsChanged(
              this.renderedRow(
                  index, spacer, this.getRenderLowerBound(), this.getRenderUpperBound()),
              diff);
        }
      }
    }
  }
  /**
   * Returns whether a node is expanded.
   *
   * <p>
   *
   * @see WTreeView#setExpanded(WModelIndex index, boolean expanded)
   */
  public boolean isExpanded(final WModelIndex index) {
    return (index == this.getRootIndex() || (index != null && index.equals(this.getRootIndex())))
        || this.expandedSet_.contains(index) != false;
  }
  /**
   * Collapses a node.
   *
   * <p>
   *
   * @see WTreeView#setExpanded(WModelIndex index, boolean expanded)
   * @see WTreeView#expand(WModelIndex index)
   *     <p><i><b>Note: </b>until 3.3.4, selection was removed from within nodes that were
   *     collapsed. This (inconsistent) behavior has been removed in 3.3.4. </i>
   */
  public void collapse(final WModelIndex index) {
    this.setExpanded(index, false);
  }
  /**
   * Collapse all expanded nodes.
   *
   * <p>
   *
   * @see WTreeView#collapse(WModelIndex index)
   * @see WTreeView#expand(WModelIndex index)
   */
  public void collapseAll() {
    while (!this.expandedSet_.isEmpty()) {
      this.collapse(this.expandedSet_.iterator().next());
    }
  }
  /**
   * Expands a node.
   *
   * <p>
   *
   * @see WTreeView#setExpanded(WModelIndex index, boolean expanded)
   * @see WTreeView#collapse(WModelIndex index)
   */
  public void expand(final WModelIndex index) {
    this.setExpanded(index, true);
  }
  /**
   * Expands all nodes to a depth.
   *
   * <p>Expands all nodes to the given <code>depth</code>. A depth of 1 corresponds to the top level
   * nodes.
   *
   * <p>
   *
   * @see WTreeView#expand(WModelIndex index)
   */
  public void expandToDepth(int depth) {
    if (depth > 0) {
      this.expandChildrenToDepth(this.getRootIndex(), depth);
    }
  }
  /**
   * Sets whether toplevel items are decorated.
   *
   * <p>By default, top level nodes have expand/collapse and other lines to display their linkage
   * and offspring, like any node.
   *
   * <p>By setting <code>show</code> to <code>false</code>, you can hide these decorations for root
   * nodes, and in this way mimic a plain list. You could also consider using a {@link WTableView}
   * instead.
   */
  public void setRootIsDecorated(boolean show) {
    this.rootIsDecorated_ = show;
  }
  /**
   * Returns whether toplevel items are decorated.
   *
   * <p>
   *
   * @see WTreeView#setRootIsDecorated(boolean show)
   */
  public boolean isRootDecorated() {
    return this.rootIsDecorated_;
  }

  public void resize(final WLength width, final WLength height) {
    WApplication app = WApplication.getInstance();
    WLength w = app.getEnvironment().hasAjax() ? WLength.Auto : width;
    if (app.getEnvironment().hasAjax()) {
      this.contentsContainer_.setWidth(w);
    }
    if (this.headerContainer_ != null) {
      this.headerContainer_.setWidth(w);
    }
    if (!height.isAuto()) {
      if (!app.getEnvironment().hasAjax()) {
        if (this.impl_.getCount() < 3) {
          this.impl_.addWidget(this.getCreatePageNavigationBar());
        }
        double navigationBarHeight = 35;
        double headerHeight = this.getHeaderHeight().toPixels();
        int h = (int) (height.toPixels() - navigationBarHeight - headerHeight);
        this.contentsContainer_.setHeight(
            new WLength(Math.max(h, (int) this.getRowHeight().getValue())));
        this.viewportHeight_ =
            (int) (this.contentsContainer_.getHeight().toPixels() / this.getRowHeight().toPixels());
      } else {
        this.viewportHeight_ = (int) Math.ceil(height.toPixels() / this.getRowHeight().toPixels());
      }
    } else {
      if (app.getEnvironment().hasAjax()) {
        this.viewportHeight_ = 30;
      }
      this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
    }
    super.resize(width, height);
  }
  /**
   * Signal emitted when a node is collapsed.
   *
   * <p>
   *
   * @see WTreeView#setExpanded(WModelIndex index, boolean expanded)
   * @see WTreeView#expanded()
   */
  public Signal1<WModelIndex> collapsed() {
    return this.collapsed_;
  }
  /**
   * Signal emitted when a node is expanded.
   *
   * <p>
   *
   * @see WTreeView#setExpanded(WModelIndex index, boolean expanded)
   * @see WTreeView#collapsed()
   */
  public Signal1<WModelIndex> expanded() {
    return this.expanded_;
  }

  public WWidget itemWidget(final WModelIndex index) {
    if (!(index != null)) {
      return null;
    }
    WTreeViewNode n = this.nodeForIndex(index);
    if (n != null) {
      return n.getCellWidget(index.getColumn());
    } else {
      return null;
    }
  }

  public void setModel(final WAbstractItemModel model) {
    super.setModel(model);
    this.modelConnections_.add(
        model
            .columnsInserted()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTreeView.this.modelColumnsInserted(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .columnsAboutToBeRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTreeView.this.modelColumnsAboutToBeRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .columnsRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTreeView.this.modelColumnsRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .rowsInserted()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTreeView.this.modelRowsInserted(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .rowsAboutToBeRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTreeView.this.modelRowsAboutToBeRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .rowsRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTreeView.this.modelRowsRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .dataChanged()
            .addListener(
                this,
                (WModelIndex e1, WModelIndex e2) -> {
                  WTreeView.this.modelDataChanged(e1, e2);
                }));
    this.modelConnections_.add(
        model
            .headerDataChanged()
            .addListener(
                this,
                (Orientation e1, Integer e2, Integer e3) -> {
                  WTreeView.this.modelHeaderDataChanged(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .layoutAboutToBeChanged()
            .addListener(
                this,
                () -> {
                  WTreeView.this.modelLayoutAboutToBeChanged();
                }));
    this.modelConnections_.add(
        model
            .layoutChanged()
            .addListener(
                this,
                () -> {
                  WTreeView.this.modelLayoutChanged();
                }));
    this.modelConnections_.add(
        model
            .modelReset()
            .addListener(
                this,
                () -> {
                  WTreeView.this.modelReset();
                }));
    this.expandedSet_.clear();
    WApplication app = WApplication.getInstance();
    while ((int) this.columns_.size() > model.getColumnCount()) {
      app.getStyleSheet().removeRule(this.columns_.get(this.columns_.size() - 1).styleRule);
      this.columns_.remove(0 + this.columns_.size() - 1);
    }
    this.pageChanged().trigger();
  }
  /**
   * Sets the column width.
   *
   * <p>For a model with {@link WAbstractItemModel#getColumnCount(WModelIndex parent)
   * WAbstractItemModel#getColumnCount()} == <code>N</code>, the initial width of columns 1..<code>N
   * </code> is set to 150 pixels, and column 0 will take all remaining space.
   *
   * <p>
   *
   * <p><i><b>Note: </b>The actual space occupied by each column is the column width augmented by 7
   * pixels for internal padding and a border. </i>
   *
   * @see WTreeView#setRowHeight(WLength rowHeight)
   */
  public void setColumnWidth(int column, final WLength width) {
    if (!width.isAuto()) {
      this.columnInfo(column).width = new WLength(Math.round(width.getValue()), width.getUnit());
    } else {
      this.columnInfo(column).width = WLength.Auto;
    }
    WWidget toResize = this.columnInfo(column).styleRule.getTemplateWidget();
    toResize.setWidth(new WLength(0));
    toResize.setWidth(new WLength(this.columnInfo(column).width.toPixels()));
    WApplication app = WApplication.getInstance();
    if (app.getEnvironment().hasAjax()
        && (int) this.renderState_.getValue()
            < (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
      this.doJavaScript(this.getJsRef() + ".wtObj.adjustColumns();");
    }
    if (!app.getEnvironment().hasAjax() && column == 0 && !width.isAuto()) {
      double total = 0;
      for (int i = 0; i < this.getColumnCount(); ++i) {
        if (!this.columnInfo(i).hidden) {
          total += this.getColumnWidth(i).toPixels();
        }
      }
      this.resize(new WLength(total), this.getHeight());
    }
  }

  public void setAlternatingRowColors(boolean enable) {
    super.setAlternatingRowColors(enable);
    this.setRootNodeStyle();
  }

  public void setRowHeight(final WLength rowHeight) {
    super.setRowHeight(rowHeight);
    if (this.rowHeightRule_ != null) {
      this.rowHeightRule_.getTemplateWidget().setHeight(rowHeight);
      this.rowHeightRule_.getTemplateWidget().setLineHeight(rowHeight);
    }
    if (!WApplication.getInstance().getEnvironment().hasAjax() && !this.getHeight().isAuto()) {
      this.viewportHeight_ =
          (int) (this.contentsContainer_.getHeight().toPixels() / rowHeight.toPixels());
    }
    this.setRootNodeStyle();
    for (Iterator<Map.Entry<WModelIndex, WTreeViewNode>> i_it =
            this.renderedNodes_.entrySet().iterator();
        i_it.hasNext(); ) {
      Map.Entry<WModelIndex, WTreeViewNode> i = i_it.next();
      i.getValue().rerenderSpacers();
    }
    if (this.rootNode_ != null) {
      this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
    }
  }

  public void setHeaderHeight(final WLength height) {
    super.setHeaderHeight(height);
  }

  public void setColumnHidden(int column, boolean hidden) {
    if (this.columnInfo(column).hidden != hidden) {
      super.setColumnHidden(column, hidden);
      WWidget toHide = this.columnInfo(column).styleRule.getTemplateWidget();
      toHide.setHidden(hidden);
      this.setColumnWidth(column, this.getColumnWidth(column));
    }
  }

  public void setRowHeaderCount(int count) {
    WApplication app = WApplication.getInstance();
    if (!app.getEnvironment().hasAjax()) {
      return;
    }
    int oldCount = this.getRowHeaderCount();
    if (count != 0 && count != 1) {
      throw new WException("WTreeView::setRowHeaderCount: count must be 0 or 1");
    }
    super.setRowHeaderCount(count);
    if (count != 0 && !(oldCount != 0)) {
      this.addStyleClass("column1");
      WContainerWidget rootWrap =
          ObjectUtils.cast(this.contents_.getWidget(0), WContainerWidget.class);
      rootWrap.setWidth(new WLength(100, LengthUnit.Percentage));
      rootWrap.setOverflow(Overflow.Hidden);
      this.contents_.setPositionScheme(PositionScheme.Relative);
      rootWrap.setPositionScheme(PositionScheme.Absolute);
      boolean useStyleLeft =
          app.getEnvironment().agentIsWebKit() || app.getEnvironment().agentIsOpera();
      if (useStyleLeft) {
        boolean rtl = app.getLayoutDirection() == LayoutDirection.RightToLeft;
        this.tieRowsScrollJS_.setJavaScript(
            "function(obj, event) {Wt4.11.2.getCssRule('#"
                + this.getId()
                + " .Wt-tv-rowc').style.left= -obj.scrollLeft "
                + (rtl ? "+ (obj.firstChild.offsetWidth - obj.offsetWidth)" : "")
                + "+ 'px';}");
      } else {
        this.tieRowsScrollJS_.setJavaScript(
            "function(obj, event) {document.querySelectorAll('#"
                + this.getId()
                + " .Wt-tv-rowc').forEach(function(elem){if (elem.parentElement) elem.parentElement.scrollLeft = obj.scrollLeft;});}");
      }
      WContainerWidget scrollBarContainer = new WContainerWidget();
      scrollBarContainer.setStyleClass("cwidth");
      scrollBarContainer.setHeight(new WLength(22));
      this.scrollBarC_ = new WContainerWidget();
      scrollBarContainer.addWidget(this.scrollBarC_);
      this.scrollBarC_.setStyleClass("Wt-tv-row Wt-scroll");
      this.scrollBarC_.scrolled().addListener(this.tieRowsScrollJS_);
      if (app.getEnvironment().agentIsIE()) {
        scrollBarContainer.setPositionScheme(PositionScheme.Relative);
        boolean rtl = app.getLayoutDirection() == LayoutDirection.RightToLeft;
        this.scrollBarC_.setAttributeValue("style", rtl ? "left:" : "right:" + "0px");
      }
      WContainerWidget scrollBar = new WContainerWidget();
      this.scrollBarC_.addWidget(scrollBar);
      scrollBar.setStyleClass("Wt-tv-rowc");
      if (useStyleLeft) {
        scrollBar.setAttributeValue("style", "left: 0px;");
      }
      this.impl_.getLayout().addWidget(scrollBarContainer);
    }
  }

  public int getPageCount() {
    if (this.rootNode_ != null) {
      return (this.rootNode_.getRenderedHeight() - 1) / this.viewportHeight_ + 1;
    } else {
      return 1;
    }
  }

  public int getPageSize() {
    return this.viewportHeight_;
  }

  public int getCurrentPage() {
    return this.viewportTop_ / this.viewportHeight_;
  }

  public void setCurrentPage(int page) {
    this.viewportTop_ = page * this.viewportHeight_;
    this.contents_.setOffsets(
        new WLength(-this.viewportTop_ * this.getRowHeight().toPixels()), EnumSet.of(Side.Top));
    this.pageChanged().trigger();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
  }

  public void scrollTo(final WModelIndex index, ScrollHint hint) {
    final int row = this.getIndexRow(index, this.getRootIndex(), 0, Integer.MAX_VALUE);
    WApplication app = WApplication.getInstance();
    if (app.getEnvironment().hasAjax()) {
      if (this.viewportHeight_ != 30) {
        if (hint == ScrollHint.EnsureVisible) {
          if (this.viewportTop_ + this.viewportHeight_ <= row) {
            hint = ScrollHint.PositionAtBottom;
          } else {
            if (row < this.viewportTop_) {
              hint = ScrollHint.PositionAtTop;
            }
          }
        }
        switch (hint) {
          case PositionAtTop:
            this.viewportTop_ = row;
            break;
          case PositionAtBottom:
            this.viewportTop_ = row - this.viewportHeight_ + 1;
            break;
          case PositionAtCenter:
            this.viewportTop_ = row - this.viewportHeight_ / 2 + 1;
            break;
          default:
            break;
        }
        if (hint != ScrollHint.EnsureVisible) {
          this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
        }
      }
      StringBuilder s = new StringBuilder();
      s.append("setTimeout(function() { ")
          .append(this.getJsRef())
          .append(".wtObj.scrollTo(-1, ")
          .append(row)
          .append(",")
          .append((int) this.getRowHeight().toPixels())
          .append(",")
          .append((int) hint.getValue())
          .append(");});");
      this.doJavaScript(s.toString());
    } else {
      this.setCurrentPage(row / this.getPageSize());
    }
  }

  public EventSignal1<WScrollEvent> scrolled() {
    if (WApplication.getInstance().getEnvironment().hasAjax() && this.contentsContainer_ != null) {
      return this.contentsContainer_.scrolled();
    }
    throw new WException("Scrolled signal existes only with ajax.");
  }

  public void setId(final String id) {
    super.setId(id);
    this.setup();
  }

  protected void render(EnumSet<RenderFlag> flags) {
    WApplication app = WApplication.getInstance();
    if (flags.contains(RenderFlag.Full)) {
      this.defineJavaScript();
      if (!this.rowDropEvent_.isConnected()) {
        this.rowDropEvent_.addListener(
            this,
            (String e1, String e2, String e3, String e4, WMouseEvent e5) -> {
              WTreeView.this.onRowDropEvent(e1, e2, e3, e4, e5);
            });
      }
      if (!this.itemTouchEvent_.isConnected()) {
        this.itemTouchEvent_.addListener(
            this,
            (String e1, String e2, WTouchEvent e3) -> {
              WTreeView.this.onItemTouchEvent(e1, e2, e3);
            });
      }
      if (!this.itemEvent_.isConnected()) {
        this.itemEvent_.addListener(
            this,
            (String e1, String e2, String e3, String e4, WMouseEvent e5) -> {
              WTreeView.this.onItemEvent(e1, e2, e3, e4, e5);
            });
        this.addCssRule("#" + this.getId() + " .cwidth", "");
        this.rowHeightRule_ =
            CssUtils.add(app.getStyleSheet(), new WCssTemplateRule("#" + this.getId() + " .rh"));
        this.rowHeightRule_.getTemplateWidget().setHeight(this.getRowHeight());
        this.rowHeightRule_.getTemplateWidget().setLineHeight(this.getRowHeight());
        this.rowWidthRule_ =
            CssUtils.add(
                app.getStyleSheet(), new WCssTemplateRule("#" + this.getId() + " .Wt-tv-row"));
        this.rowContentsWidthRule_ =
            CssUtils.add(
                app.getStyleSheet(), new WCssTemplateRule("#" + this.getId() + " .Wt-tv-rowc"));
        if (app.getEnvironment().hasAjax()) {
          this.contentsContainer_
              .scrolled()
              .addListener(
                  "function(obj, event) {if (obj.sb) return;obj.sb = true;"
                      + this.headerContainer_.getJsRef()
                      + ".scrollLeft=obj.scrollLeft;var t = "
                      + this.contents_.getJsRef()
                      + ".firstChild;var h = "
                      + this.headers_.getJsRef()
                      + ";h.style.width = (t.offsetWidth - 1) + 'px';h.style.width = t.offsetWidth + 'px';obj.sb = false;}");
        }
        this.c0StyleRule_ =
            this.addCssRule(
                "#" + this.getId() + " li .none",
                "width: auto;text-overflow: ellipsis;overflow: hidden");
        if (this.columns_.size() > 0) {
          final WAbstractItemView.ColumnInfo ci = this.columnInfo(0);
          this.c0StyleRule_.setSelector("#" + this.getId() + " li ." + ci.getStyleClass());
        }
      }
    }
    while (this.renderState_ != WAbstractItemView.RenderState.RenderOk) {
      WAbstractItemView.RenderState s = this.renderState_;
      this.renderState_ = WAbstractItemView.RenderState.RenderOk;
      switch (s) {
        case NeedRerender:
          this.rerenderHeader();
          this.rerenderTree();
          break;
        case NeedRerenderHeader:
          this.rerenderHeader();
          break;
        case NeedRerenderData:
          this.rerenderTree();
          break;
        case NeedAdjustViewPort:
          this.adjustToViewport();
          break;
        default:
          break;
      }
    }
    this.contents_.setHeight(
        new WLength(this.subTreeHeight(this.getRootIndex()) * this.getRowHeight().toPixels()));
    if (app.getEnvironment().hasAjax()
        && this.getRowHeaderCount() != 0
        && this.renderedNodesAdded_) {
      this.doJavaScript(
          "{var s="
              + this.scrollBarC_.getJsRef()
              + ";if (s) {"
              + this.tieRowsScrollJS_.execJs("s")
              + "}}");
      this.renderedNodesAdded_ = false;
    }
    StringBuilder s = new StringBuilder();
    s.append(this.getJsRef())
        .append(".wtObj.setRowHeight(")
        .append((int) this.getRowHeight().toPixels())
        .append(");");
    s.append(this.getJsRef())
        .append(".wtObj.setItemDropsEnabled(")
        .append(this.enabledDropLocations_.contains(DropLocation.OnItem))
        .append(");");
    s.append(this.getJsRef())
        .append(".wtObj.setRowDropsEnabled(")
        .append(this.enabledDropLocations_.contains(DropLocation.BetweenRows))
        .append(");");
    if (app.getEnvironment().hasAjax()) {
      this.doJavaScript(s.toString());
    }
    super.render(flags);
  }

  protected void enableAjax() {
    this.saveExtraHeaderWidgets();
    this.setup();
    this.defineJavaScript();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedRerender);
    super.enableAjax();
  }

  private boolean skipNextMouseEvent_;
  HashSet<WModelIndex> expandedSet_;
  private HashMap<WModelIndex, WTreeViewNode> renderedNodes_;
  private boolean renderedNodesAdded_;
  private WTreeViewNode rootNode_;
  private WCssTemplateRule rowHeightRule_;
  private WCssTemplateRule rowWidthRule_;
  private WCssTemplateRule rowContentsWidthRule_;
  private WCssRule c0StyleRule_;
  private boolean rootIsDecorated_;
  boolean column1Fixed_;
  Signal1<WModelIndex> collapsed_;
  Signal1<WModelIndex> expanded_;
  private int viewportTop_;
  private int viewportHeight_;
  private int firstRenderedRow_;
  private int validRowCount_;
  private int nodeLoad_;
  private WContainerWidget headers_;
  private WContainerWidget headerContainer_;
  private WContainerWidget contents_;
  private WContainerWidget contentsContainer_;
  private WContainerWidget scrollBarC_;
  private int firstRemovedRow_;
  private int removedHeight_;
  private JSignal5<String, String, String, String, WMouseEvent> itemEvent_;
  private JSignal3<String, String, WTouchEvent> itemTouchEvent_;
  private JSignal5<String, String, String, String, WMouseEvent> rowDropEvent_;
  ToggleButtonConfig expandConfig_;
  private JSlot tieRowsScrollJS_;
  private JSlot itemClickedJS_;
  private JSlot rootClickedJS_;
  private JSlot itemDoubleClickedJS_;
  private JSlot rootDoubleClickedJS_;
  private JSlot itemMouseDownJS_;
  private JSlot rootMouseDownJS_;
  private JSlot itemMouseUpJS_;
  private JSlot rootMouseUpJS_;
  private JSlot touchStartedJS_;
  private JSlot touchMovedJS_;
  private JSlot touchEndedJS_;

  WAbstractItemView.ColumnInfo createColumnInfo(int column) {
    WAbstractItemView.ColumnInfo ci = super.createColumnInfo(column);
    if (column == 0) {
      ci.width = WLength.Auto;
      ci.styleRule.getTemplateWidget().resize(WLength.Auto, WLength.Auto);
      if (this.c0StyleRule_ != null) {
        final WCssStyleSheet styleSheet = WApplication.getInstance().getStyleSheet();
        this.c0StyleRule_.setSelector("#" + this.getId() + " li ." + ci.getStyleClass());
        styleSheet.addRule(styleSheet.removeRule(this.c0StyleRule_));
      }
    }
    return ci;
  }

  private void defineJavaScript() {
    WApplication app = WApplication.getInstance();
    if (!app.getEnvironment().hasAjax()) {
      return;
    }
    app.loadJavaScript("js/WTreeView.js", wtjs1());
    this.setJavaScriptMember(
        " WTreeView",
        "new Wt4.11.2.WTreeView("
            + app.getJavaScriptClass()
            + ","
            + this.getJsRef()
            + ","
            + this.contentsContainer_.getJsRef()
            + ","
            + this.headerContainer_.getJsRef()
            + ","
            + String.valueOf(this.getRowHeaderCount())
            + ",'"
            + WApplication.getInstance().getTheme().getActiveClass()
            + "');");
    this.setJavaScriptMember(WT_RESIZE_JS, "function(self,w,h,s) {self.wtObj.wtResize();}");
  }

  private void rerenderHeader() {
    WApplication app = WApplication.getInstance();
    this.saveExtraHeaderWidgets();
    this.headers_.clear();
    WContainerWidget row = new WContainerWidget();
    this.headers_.addWidget(row);
    row.setFloatSide(Side.Right);
    if (this.getRowHeaderCount() != 0) {
      row.setStyleClass("Wt-tv-row headerrh background");
      WContainerWidget r = new WContainerWidget();
      row.addWidget(r);
      row = r;
      row.setStyleClass("Wt-tv-rowc headerrh");
    } else {
      row.setStyleClass("Wt-tv-row");
    }
    for (int i = 0; i < this.getColumnCount(); ++i) {
      WWidget w = this.createHeaderWidget(i);
      if (i != 0) {
        w.setFloatSide(Side.Left);
        row.addWidget(w);
      } else {
        this.headers_.addWidget(w);
      }
    }
    if (app.getEnvironment().hasAjax()) {
      this.doJavaScript(this.getJsRef() + ".wtObj.adjustColumns();");
    }
  }

  private void rerenderTree() {
    WContainerWidget wrapRoot =
        ObjectUtils.cast(this.contents_.getWidget(0), WContainerWidget.class);
    wrapRoot.clear();
    this.firstRenderedRow_ = this.getCalcOptimalFirstRenderedRow();
    this.validRowCount_ = 0;
    this.rootNode_ = new WTreeViewNode(this, this.getRootIndex(), -1, true, null);
    wrapRoot.addWidget(this.rootNode_);
    if (WApplication.getInstance().getEnvironment().hasAjax()) {
      if (this.getEditTriggers().contains(EditTrigger.SingleClicked)
          || this.clicked().isConnected()) {
        this.rootNode_.clicked().addListener(this.itemClickedJS_);
        this.contentsContainer_.clicked().addListener(this.rootClickedJS_);
      }
      if (this.getEditTriggers().contains(EditTrigger.DoubleClicked)
          || this.doubleClicked().isConnected()) {
        this.rootNode_.doubleClicked().addListener(this.itemDoubleClickedJS_);
        this.contentsContainer_.doubleClicked().addListener(this.rootDoubleClickedJS_);
      }
      this.rootNode_.mouseWentDown().addListener(this.itemMouseDownJS_);
      this.contentsContainer_.mouseWentDown().addListener(this.rootMouseDownJS_);
      if (this.mouseWentUp().isConnected()) {
        this.rootNode_.mouseWentUp().addListener(this.itemMouseUpJS_);
        this.contentsContainer_.mouseWentUp().addListener(this.rootMouseUpJS_);
      }
      final AbstractEventSignal a = this.rootNode_.touchStarted();
      this.rootNode_.touchStarted().addListener(this.touchStartedJS_);
      this.rootNode_.touchMoved().addListener(this.touchMovedJS_);
      this.rootNode_.touchEnded().addListener(this.touchEndedJS_);
    }
    this.setRootNodeStyle();
    this.pageChanged().trigger();
    this.adjustToViewport();
  }

  private void setup() {
    WApplication app = WApplication.getInstance();
    this.impl_.clear();
    this.rootNode_ = null;
    this.headers_ = new WContainerWidget();
    this.headers_.setStyleClass("Wt-headerdiv headerrh");
    this.contents_ = new WContainerWidget();
    WContainerWidget wrapRoot = new WContainerWidget();
    this.contents_.addWidget(wrapRoot);
    if (app.getEnvironment().agentIsIE()) {
      wrapRoot.setAttributeValue("style", "zoom: 1");
      this.contents_.setAttributeValue("style", "zoom: 1");
    }
    if (app.getEnvironment().hasAjax()) {
      this.impl_.setPositionScheme(PositionScheme.Relative);
      WVBoxLayout layout = new WVBoxLayout();
      layout.setSpacing(0);
      layout.setContentsMargins(0, 0, 0, 0);
      this.headerContainer_ = new WContainerWidget();
      this.headerContainer_.setOverflow(Overflow.Hidden);
      this.headerContainer_.setStyleClass("Wt-header headerrh cwidth");
      this.headerContainer_.addWidget(this.headers_);
      this.contentsContainer_ = new ContentsContainer(this);
      this.contentsContainer_.setStyleClass("cwidth");
      this.contentsContainer_.setOverflow(Overflow.Auto);
      this.contentsContainer_
          .scrolled()
          .addListener(
              this,
              (WScrollEvent e1) -> {
                WTreeView.this.onViewportChange(e1);
              });
      this.contentsContainer_.addWidget(this.contents_);
      layout.addWidget(this.headerContainer_);
      layout.addWidget(this.contentsContainer_, 1);
      this.impl_.setLayout(layout);
    } else {
      this.contentsContainer_ = new WContainerWidget();
      this.contentsContainer_.addWidget(this.contents_);
      this.contentsContainer_.setOverflow(Overflow.Hidden);
      this.impl_.setPositionScheme(PositionScheme.Relative);
      this.contentsContainer_.setPositionScheme(PositionScheme.Relative);
      this.contents_.setPositionScheme(PositionScheme.Relative);
      this.impl_.addWidget(this.headers_);
      this.impl_.addWidget(this.contentsContainer_);
      this.viewportHeight_ = 1000;
      this.resize(this.getWidth(), this.getHeight());
    }
    this.setRowHeight(this.getRowHeight());
    this.bindObjJS(this.itemClickedJS_, "click");
    this.bindObjJS(this.rootClickedJS_, "rootClick");
    this.bindObjJS(this.itemDoubleClickedJS_, "dblClick");
    this.bindObjJS(this.rootDoubleClickedJS_, "rootDblClick");
    this.bindObjJS(this.itemMouseDownJS_, "mouseDown");
    this.bindObjJS(this.rootMouseDownJS_, "rootMouseDown");
    this.bindObjJS(this.itemMouseUpJS_, "mouseUp");
    this.bindObjJS(this.rootMouseUpJS_, "rootMouseUp");
    this.bindObjJS(this.touchStartedJS_, "touchStart");
    this.bindObjJS(this.touchMovedJS_, "touchMove");
    this.bindObjJS(this.touchEndedJS_, "touchEnd");
  }

  void scheduleRerender(WAbstractItemView.RenderState what) {
    if (what == WAbstractItemView.RenderState.NeedRerender
        || what == WAbstractItemView.RenderState.NeedRerenderData) {
      if (this.rootNode_ != null) {
        {
          WWidget toRemove = this.rootNode_.removeFromParent();
          if (toRemove != null) toRemove.remove();
        }

        this.rootNode_ = null;
      }
    }
    super.scheduleRerender(what);
  }

  private void modelColumnsInserted(final WModelIndex parent, int start, int end) {
    int count = end - start + 1;
    if (!(parent != null)) {
      WApplication app = WApplication.getInstance();
      for (int i = start; i < start + count; ++i) {
        this.columns_.add(0 + i, this.createColumnInfo(i));
      }
      if ((int) this.renderState_.getValue()
          < (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
        if (start == 0) {
          this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderHeader);
        } else {
          if (app.getEnvironment().hasAjax()) {
            this.doJavaScript(this.getJsRef() + ".wtObj.adjustColumns();");
          }
          WContainerWidget row = this.getHeaderRow();
          for (int i = start; i < start + count; ++i) {
            WWidget w = this.createHeaderWidget(i);
            w.setFloatSide(Side.Left);
            row.insertWidget(i - 1, w);
          }
        }
      }
    }
    if (this.renderState_ == WAbstractItemView.RenderState.NeedRerender
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerenderData) {
      return;
    }
    if (start == 0) {
      this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
    } else {
      WTreeViewNode node = this.nodeForIndex(parent);
      if (node != null) {
        for (WTreeViewNode c = node.nextChildNode((WTreeViewNode) null);
            c != null;
            c = node.nextChildNode(c)) {
          c.insertColumns(start, count);
        }
      }
    }
  }

  private void modelColumnsAboutToBeRemoved(final WModelIndex parent, int start, int end) {
    int count = end - start + 1;
    if (!(parent != null)) {
      WApplication app = WApplication.getInstance();
      if ((int) this.renderState_.getValue()
          < (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
        if (app.getEnvironment().hasAjax()) {
          this.doJavaScript(this.getJsRef() + ".wtObj.adjustColumns();");
        }
      }
      for (int i = start; i < start + count; i++) {
        app.getStyleSheet().removeRule(this.columns_.get(i).styleRule);
      }
      for (int ii = 0; ii < (0 + start + count) - (0 + start); ++ii)
        this.columns_.remove(0 + start);
      ;
      if ((int) this.renderState_.getValue()
          < (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
        if (start == 0) {
          this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderHeader);
        } else {
          for (int i = start; i < start + count; ++i) {
            WWidget w = this.headerWidget(start, false);
            if (w != null) {
              {
                WWidget toRemove = w.removeFromParent();
                if (toRemove != null) toRemove.remove();
              }
            }
          }
        }
      }
    }
    if (start == 0) {
      this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
    }
  }

  private void modelColumnsRemoved(final WModelIndex parent, int start, int end) {
    if (this.renderState_ == WAbstractItemView.RenderState.NeedRerender
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerenderData) {
      return;
    }
    int count = end - start + 1;
    if (start != 0) {
      WTreeViewNode node = this.nodeForIndex(parent);
      if (node != null) {
        for (WTreeViewNode c = node.nextChildNode((WTreeViewNode) null);
            c != null;
            c = node.nextChildNode(c)) {
          c.removeColumns(start, count);
        }
      }
    }
    if (start <= this.currentSortColumn_ && this.currentSortColumn_ <= end) {
      this.currentSortColumn_ = -1;
    }
  }

  private void modelRowsInserted(final WModelIndex parent, int start, int end) {
    int count = end - start + 1;
    this.shiftModelIndexes(parent, start, count);
    if (this.renderState_ == WAbstractItemView.RenderState.NeedRerender
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerenderData) {
      return;
    }
    WWidget parentWidget = this.widgetForIndex(parent);
    boolean renderedRowsChange = this.isExpandedRecursive(parent);
    if (parentWidget != null) {
      WTreeViewNode parentNode = ObjectUtils.cast(parentWidget, WTreeViewNode.class);
      if (parentNode != null) {
        if (parentNode.isChildrenLoaded()) {
          WWidget startWidget = null;
          if (end < this.getModel().getRowCount(parent) - 1) {
            startWidget = parentNode.widgetForModelRow(start);
          } else {
            if (parentNode.getBottomSpacerHeight() != 0) {
              startWidget = parentNode.bottomSpacer();
            }
          }
          parentNode.adjustChildrenHeight(count);
          parentNode.shiftModelIndexes(start, count);
          if (startWidget != null && startWidget == parentNode.topSpacer()) {
            parentNode.addTopSpacerHeight(count);
            if (renderedRowsChange) {
              this.renderedRowsChanged(
                  this.renderedRow(
                      this.getModel().getIndex(start, 0, parent),
                      parentNode.topSpacer(),
                      this.getRenderLowerBound(),
                      this.getRenderUpperBound()),
                  count);
            }
          } else {
            if (startWidget != null && startWidget == parentNode.bottomSpacer()) {
              parentNode.addBottomSpacerHeight(count);
              if (renderedRowsChange) {
                this.renderedRowsChanged(
                    this.renderedRow(
                        this.getModel().getIndex(start, 0, parent),
                        parentNode.bottomSpacer(),
                        this.getRenderLowerBound(),
                        this.getRenderUpperBound()),
                    count);
              }
            } else {
              int maxRenderHeight =
                  this.firstRenderedRow_
                      + Math.max(this.validRowCount_, this.viewportHeight_)
                      - parentNode.renderedRow()
                      - parentNode.getTopSpacerHeight();
              int containerIndex =
                  startWidget != null
                      ? parentNode.getChildContainer().getIndexOf(startWidget)
                      : parentNode.getChildContainer().getCount();
              int parentRowCount = this.getModel().getRowCount(parent);
              int nodesToAdd = Math.max(0, Math.min(count, maxRenderHeight));
              WTreeViewNode first = null;
              for (int i = 0; i < nodesToAdd; ++i) {
                WTreeViewNode n =
                    new WTreeViewNode(
                        this,
                        this.getModel().getIndex(start + i, 0, parent),
                        -1,
                        start + i == parentRowCount - 1,
                        parentNode);
                if (!(first != null)) {
                  first = n;
                }
                parentNode.getChildContainer().insertWidget(containerIndex + i, n);
                if (renderedRowsChange) {
                  ++this.validRowCount_;
                }
              }
              if (nodesToAdd < count) {
                parentNode.addBottomSpacerHeight(count - nodesToAdd);
                int targetSize = containerIndex + nodesToAdd + 1;
                int extraBottomSpacer = 0;
                while (parentNode.getChildContainer().getCount() > targetSize) {
                  WTreeViewNode n =
                      ObjectUtils.cast(
                          parentNode.getChildContainer().getWidget(targetSize - 1),
                          WTreeViewNode.class);
                  assert n != null;
                  extraBottomSpacer += n.getRenderedHeight();
                  if (renderedRowsChange) {
                    this.validRowCount_ -= n.getRenderedHeight();
                  }
                  {
                    WWidget toRemove = n.removeFromParent();
                    if (toRemove != null) toRemove.remove();
                  }
                }
                if (extraBottomSpacer != 0) {
                  parentNode.addBottomSpacerHeight(extraBottomSpacer);
                }
                parentNode.normalizeSpacers();
              }
              if (first != null && renderedRowsChange) {
                this.renderedRowsChanged(
                    first.renderedRow(this.getRenderLowerBound(), this.getRenderUpperBound()),
                    nodesToAdd);
              }
              if (end == this.getModel().getRowCount(parent) - 1 && start >= 1) {
                WTreeViewNode n =
                    ObjectUtils.cast(parentNode.widgetForModelRow(start - 1), WTreeViewNode.class);
                if (n != null) {
                  n.updateGraphics(false, !this.getModel().hasChildren(n.getModelIndex()));
                }
              }
            }
          }
        }
        if (this.getModel().getRowCount(parent) == count) {
          parentNode.updateGraphics(parentNode.isLast(), false);
        }
      } else {
        if (this.isExpanded(parent)) {
          RowSpacer s = ObjectUtils.cast(parentWidget, RowSpacer.class);
          s.setRows(s.getRows() + count);
          s.getNode().adjustChildrenHeight(count);
          if (renderedRowsChange) {
            this.renderedRowsChanged(
                this.renderedRow(
                    this.getModel().getIndex(start, 0, parent),
                    s,
                    this.getRenderLowerBound(),
                    this.getRenderUpperBound()),
                count);
          }
        }
      }
    }
  }

  private void modelRowsAboutToBeRemoved(final WModelIndex parent, int start, int end) {
    int count = end - start + 1;
    boolean renderedRowsChange = this.isExpandedRecursive(parent);
    if (this.renderState_ != WAbstractItemView.RenderState.NeedRerender
        && this.renderState_ != WAbstractItemView.RenderState.NeedRerenderData) {
      this.firstRemovedRow_ = -1;
      this.removedHeight_ = 0;
      WWidget parentWidget = this.widgetForIndex(parent);
      if (parentWidget != null) {
        WTreeViewNode parentNode = ObjectUtils.cast(parentWidget, WTreeViewNode.class);
        if (parentNode != null) {
          if (parentNode.isChildrenLoaded()) {
            for (int i = end; i >= start; --i) {
              WWidget w = parentNode.widgetForModelRow(i);
              assert w != null;
              RowSpacer s = ObjectUtils.cast(w, RowSpacer.class);
              if (s != null) {
                WModelIndex childIndex = this.getModel().getIndex(i, 0, parent);
                if (i == start && renderedRowsChange) {
                  this.firstRemovedRow_ = this.renderedRow(childIndex, w);
                }
                int childHeight = this.subTreeHeight(childIndex);
                if (renderedRowsChange) {
                  this.removedHeight_ += childHeight;
                }
                s.setRows(s.getRows() - childHeight);
              } else {
                WTreeViewNode node = ObjectUtils.cast(w, WTreeViewNode.class);
                if (renderedRowsChange) {
                  if (i == start) {
                    this.firstRemovedRow_ = node.renderedRow();
                  }
                  this.removedHeight_ += node.getRenderedHeight();
                }
                {
                  WWidget toRemove = node.removeFromParent();
                  if (toRemove != null) toRemove.remove();
                }
              }
            }
          }
        } else {
          if (this.isExpanded(parent)) {
            RowSpacer s = ObjectUtils.cast(parentWidget, RowSpacer.class);
            for (int i = start; i <= end; ++i) {
              WModelIndex childIndex = this.getModel().getIndex(i, 0, parent);
              int childHeight = this.subTreeHeight(childIndex);
              if (renderedRowsChange) {
                this.removedHeight_ += childHeight;
                if (i == start) {
                  this.firstRemovedRow_ = this.renderedRow(childIndex, s);
                }
              }
            }
          }
        }
      }
    }
    this.shiftModelIndexes(parent, start, -count);
  }

  private void modelRowsRemoved(final WModelIndex parent, int start, int end) {
    int count = end - start + 1;
    if (this.renderState_ != WAbstractItemView.RenderState.NeedRerender
        && this.renderState_ != WAbstractItemView.RenderState.NeedRerenderData) {
      WWidget parentWidget = this.widgetForIndex(parent);
      if (parentWidget != null) {
        WTreeViewNode parentNode = ObjectUtils.cast(parentWidget, WTreeViewNode.class);
        if (parentNode != null) {
          if (parentNode.isChildrenLoaded()) {
            parentNode.normalizeSpacers();
            parentNode.adjustChildrenHeight(-this.removedHeight_);
            parentNode.shiftModelIndexes(start, -count);
            if (end >= this.getModel().getRowCount(parent) && start >= 1) {
              WTreeViewNode n =
                  ObjectUtils.cast(parentNode.widgetForModelRow(start - 1), WTreeViewNode.class);
              if (n != null) {
                n.updateGraphics(true, !this.getModel().hasChildren(n.getModelIndex()));
              }
            }
          }
          if (this.getModel().getRowCount(parent) == 0 && count != 0) {
            parentNode.updateGraphics(parentNode.isLast(), true);
          }
        } else {
          if (this.isExpanded(parent)) {
            RowSpacer s = ObjectUtils.cast(parentWidget, RowSpacer.class);
            WTreeViewNode node = s.getNode();
            s.setRows(s.getRows() - this.removedHeight_);
            node.adjustChildrenHeight(-this.removedHeight_);
          }
        }
      }
    }
    if (this.renderState_ != WAbstractItemView.RenderState.NeedRerender
        && this.renderState_ != WAbstractItemView.RenderState.NeedRerenderData) {
      this.renderedRowsChanged(this.firstRemovedRow_, -this.removedHeight_);
    }
  }

  void modelDataChanged(final WModelIndex topLeft, final WModelIndex bottomRight) {
    if (this.renderState_ == WAbstractItemView.RenderState.NeedRerender
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerenderData) {
      return;
    }
    WModelIndex parent = topLeft.getParent();
    WTreeViewNode parentNode = this.nodeForIndex(parent);
    if (parentNode != null && parentNode.isChildrenLoaded()) {
      for (int r = topLeft.getRow(); r <= bottomRight.getRow(); ++r) {
        WModelIndex index = this.getModel().getIndex(r, 0, parent);
        WTreeViewNode n = this.nodeForIndex(index);
        if (n != null) {
          n.update(topLeft.getColumn(), bottomRight.getColumn());
        }
      }
    }
  }

  void modelLayoutAboutToBeChanged() {
    WModelIndex.encodeAsRawIndexes(this.expandedSet_);
    super.modelLayoutAboutToBeChanged();
  }

  void modelLayoutChanged() {
    super.modelLayoutChanged();
    this.expandedSet_ = WModelIndex.decodeFromRawIndexes(this.expandedSet_);
    this.renderedNodes_.clear();
    this.pageChanged().trigger();
  }

  private void onViewportChange(WScrollEvent e) {
    this.viewportTop_ = (int) Math.floor(e.getScrollY() / this.getRowHeight().toPixels());
    this.contentsSizeChanged(0, e.getViewportHeight());
  }

  void contentsSizeChanged(int width, int height) {
    this.viewportHeight_ = (int) Math.ceil(height / this.getRowHeight().toPixels());
    this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
  }

  private void onItemEvent(
      String nodeAndColumnId, String type, String extra1, String extra2, WMouseEvent event) {
    WModelIndex index = this.calculateModelIndex(nodeAndColumnId);
    if (nodeAndColumnId.length() == 0 && this.skipNextMouseEvent_) {
      this.skipNextMouseEvent_ = false;
      return;
    } else {
      if (nodeAndColumnId.length() != 0) {
        this.skipNextMouseEvent_ = true;
      }
    }
    if (type.equals("clicked")) {
      this.handleClick(index, event);
    } else {
      if (type.equals("dblclicked")) {
        this.handleDoubleClick(index, event);
      } else {
        if (type.equals("mousedown")) {
          this.handleMouseDown(index, event);
        } else {
          if (type.equals("mouseup")) {
            this.handleMouseUp(index, event);
          } else {
            if (type.equals("drop")) {
              WDropEvent e =
                  new WDropEvent(WApplication.getInstance().decodeObject(extra1), extra2, event);
              this.dropEvent(e, index);
            }
          }
        }
      }
    }
  }

  private void onRowDropEvent(
      String nodeAndColumnId, String sourceId, String mimeType, String side, WMouseEvent event) {
    WModelIndex index = this.calculateModelIndex(nodeAndColumnId);
    WDropEvent e =
        new WDropEvent(WApplication.getInstance().decodeObject(sourceId), mimeType, event);
    this.dropEvent(e, index, side.equals("top") ? Side.Top : Side.Bottom);
  }

  private void onItemTouchEvent(String nodeAndColumnId, String type, WTouchEvent event) {
    List<WModelIndex> index = new ArrayList<WModelIndex>();
    index.add(this.calculateModelIndex(nodeAndColumnId));
    if (type.equals("touchselect")) {
      this.handleTouchSelect(index, event);
    } else {
      if (type.equals("touchstart")) {
        this.handleTouchStart(index, event);
      } else {
        if (type.equals("touchend")) {
          this.handleTouchEnd(index, event);
        }
      }
    }
  }

  private WModelIndex calculateModelIndex(String nodeAndColumnId) {
    List<String> nodeAndColumnSplit = new ArrayList<String>();
    StringUtils.split(nodeAndColumnSplit, nodeAndColumnId, ":", false);
    WModelIndex index = null;
    if (nodeAndColumnSplit.size() == 2) {
      String nodeId = nodeAndColumnSplit.get(0);
      int columnId = -1;
      try {
        columnId = Integer.parseInt(nodeAndColumnSplit.get(1));
      } catch (final RuntimeException e) {
        logger.error(
            new StringWriter()
                .append("WTreeview::calculateModelIndex: bad value for format 1: ")
                .append(nodeAndColumnSplit.get(1))
                .toString());
      }
      int column = columnId == 0 ? 0 : -1;
      for (int i = 0; i < this.columns_.size(); ++i) {
        if (this.columns_.get(i).id == columnId) {
          column = i;
          break;
        }
      }
      if (column != -1) {
        WModelIndex c0index = null;
        for (Iterator<Map.Entry<WModelIndex, WTreeViewNode>> i_it =
                this.renderedNodes_.entrySet().iterator();
            i_it.hasNext(); ) {
          Map.Entry<WModelIndex, WTreeViewNode> i = i_it.next();
          if (i.getValue().getId().equals(nodeId)) {
            c0index = i.getValue().getModelIndex();
            break;
          }
        }
        if ((c0index != null)) {
          index = this.getModel().getIndex(c0index.getRow(), column, c0index.getParent());
        } else {
          logger.error(
              new StringWriter()
                  .append("WTreeView::calculateModelIndex: illegal node id: ")
                  .append(nodeId)
                  .toString());
        }
      }
    }
    return index;
  }

  private void setRootNodeStyle() {
    if (!(this.rootNode_ != null)) {
      return;
    }
    if (this.hasAlternatingRowColors()) {
      this.rootNode_
          .getDecorationStyle()
          .setBackgroundImage(
              new WLink(
                  WApplication.getInstance().getTheme().getResourcesUrl()
                      + "stripes/stripe-"
                      + String.valueOf((int) this.getRowHeight().toPixels())
                      + "px.gif"));
    } else {
      this.rootNode_.getDecorationStyle().setBackgroundImage(new WLink(""));
    }
  }

  void setCollapsed(final WModelIndex index) {
    this.expandedSet_.remove(index);
  }

  private int getCalcOptimalFirstRenderedRow() {
    if (WApplication.getInstance().getEnvironment().hasAjax()) {
      return Math.max(0, this.viewportTop_ - this.viewportHeight_ - this.viewportHeight_ / 2);
    } else {
      return this.viewportTop_;
    }
  }

  private int getCalcOptimalRenderedRowCount() {
    if (WApplication.getInstance().getEnvironment().hasAjax()) {
      return 4 * this.viewportHeight_;
    } else {
      return this.viewportHeight_ + 5;
    }
  }

  private void shiftModelIndexes(final WModelIndex parent, int start, int count) {
    shiftModelIndexes(parent, start, count, this.getModel(), this.expandedSet_);
    int removed =
        shiftModelIndexes(
            parent, start, count, this.getModel(), this.getSelectionModel().selection_);
    this.shiftEditorRows(parent, start, count, false);
    if (removed != 0) {
      this.selectionChanged().trigger();
    }
  }

  private static int shiftModelIndexes(
      final WModelIndex parent,
      int start,
      int count,
      final WAbstractItemModel model,
      final SortedSet<WModelIndex> set) {
    List<WModelIndex> toShift = new ArrayList<WModelIndex>();
    List<WModelIndex> toErase = new ArrayList<WModelIndex>();
    for (Iterator<WModelIndex> it_it = set.tailSet(model.getIndex(start, 0, parent)).iterator();
        it_it.hasNext(); ) {
      WModelIndex it = it_it.next();
      WModelIndex i = it;
      WModelIndex p = i.getParent();
      if (!(p == parent || (p != null && p.equals(parent))) && !WModelIndex.isAncestor(p, parent)) {
        break;
      }
      if ((p == parent || (p != null && p.equals(parent)))) {
        toShift.add(i);
        toErase.add(i);
      } else {
        if (count < 0) {
          do {
            if ((p.getParent() == parent || (p.getParent() != null && p.getParent().equals(parent)))
                && p.getRow() >= start
                && p.getRow() < start - count) {
              toErase.add(i);
              break;
            } else {
              p = p.getParent();
            }
          } while (!(p == parent || (p != null && p.equals(parent))));
        }
      }
    }
    for (int i = 0; i < toErase.size(); ++i) {
      set.remove(toErase.get(i));
    }
    int removed = 0;
    for (int i = 0; i < toShift.size(); ++i) {
      if (toShift.get(i).getRow() + count >= start) {
        WModelIndex newIndex =
            model.getIndex(toShift.get(i).getRow() + count, toShift.get(i).getColumn(), parent);
        set.add(newIndex);
      } else {
        ++removed;
      }
    }
    return removed;
  }

  private static int shiftModelIndexes(
      final WModelIndex parent,
      int start,
      int count,
      final WAbstractItemModel model,
      final HashSet<WModelIndex> set) {
    if (set.isEmpty()) {
      return 0;
    }
    List<WModelIndex> toShift = new ArrayList<WModelIndex>();
    List<WModelIndex> toErase = new ArrayList<WModelIndex>();
    final int rowCount = model.getRowCount(parent);
    for (int row = start; row < rowCount; ++row) {
      WModelIndex i = model.getIndex(row, 0, parent);
      if (row < start - count) {
        removalsFromSet(toErase, set, i);
      } else {
        if (set.contains(i) != false) {
          toShift.add(i);
          toErase.add(i);
        }
      }
    }
    for (int i = 0; i < toErase.size(); ++i) {
      set.remove(toErase.get(i));
    }
    int removed = 0;
    for (int i = 0; i < toShift.size(); ++i) {
      if (toShift.get(i).getRow() + count >= start) {
        WModelIndex newIndex =
            model.getIndex(toShift.get(i).getRow() + count, toShift.get(i).getColumn(), parent);
        set.add(newIndex);
      } else {
        ++removed;
      }
    }
    return removed;
  }

  void addRenderedNode(WTreeViewNode node) {
    this.renderedNodes_.put(node.getModelIndex(), node);
    ++this.nodeLoad_;
    this.renderedNodesAdded_ = true;
  }

  void removeRenderedNode(WTreeViewNode node) {
    this.renderedNodes_.remove(node.getModelIndex());
    --this.nodeLoad_;
  }

  private void adjustToViewport(WTreeViewNode changed) {
    this.firstRenderedRow_ = Math.max(0, this.firstRenderedRow_);
    this.validRowCount_ =
        Math.max(
            0,
            Math.min(
                this.validRowCount_, this.rootNode_.getRenderedHeight() - this.firstRenderedRow_));
    int viewportBottom =
        Math.min(this.rootNode_.getRenderedHeight(), this.viewportTop_ + this.viewportHeight_);
    int lastValidRow = this.firstRenderedRow_ + this.validRowCount_;
    boolean renderMore =
        Math.max(0, this.viewportTop_ - this.viewportHeight_) < this.firstRenderedRow_
            || Math.min(this.rootNode_.getRenderedHeight(), viewportBottom + this.viewportHeight_)
                > lastValidRow;
    boolean pruneFirst = false;
    if (renderMore) {
      int newFirstRenderedRow =
          Math.min(this.firstRenderedRow_, this.getCalcOptimalFirstRenderedRow());
      int newLastValidRow =
          Math.max(
              lastValidRow,
              Math.min(
                  this.rootNode_.getRenderedHeight(),
                  this.getCalcOptimalFirstRenderedRow() + this.getCalcOptimalRenderedRowCount()));
      int newValidRowCount = newLastValidRow - newFirstRenderedRow;
      int newRows =
          Math.max(0, this.firstRenderedRow_ - newFirstRenderedRow)
              + Math.max(0, newLastValidRow - lastValidRow);
      final int pruneFactor = WApplication.getInstance().getEnvironment().hasAjax() ? 9 : 1;
      if (this.nodeLoad_ + newRows > pruneFactor * this.viewportHeight_) {
        pruneFirst = true;
      } else {
        if (newFirstRenderedRow < this.firstRenderedRow_ || newLastValidRow > lastValidRow) {
          this.firstRenderedRow_ = newFirstRenderedRow;
          this.validRowCount_ = newValidRowCount;
          this.adjustRenderedNode(this.rootNode_, 0);
        }
      }
    }
    final int pruneFactor = WApplication.getInstance().getEnvironment().hasAjax() ? 5 : 1;
    if (pruneFirst || this.nodeLoad_ > pruneFactor * this.viewportHeight_) {
      this.firstRenderedRow_ = this.getCalcOptimalFirstRenderedRow();
      this.validRowCount_ = this.getCalcOptimalRenderedRowCount();
      this.pruneNodes(this.rootNode_, 0);
      if (pruneFirst && this.nodeLoad_ < this.getCalcOptimalRenderedRowCount()) {
        this.adjustRenderedNode(this.rootNode_, 0);
      }
    }
  }

  private final void adjustToViewport() {
    adjustToViewport((WTreeViewNode) null);
  }

  private int pruneNodes(WTreeViewNode node, int nodeRow) {
    WModelIndex index = node.getModelIndex();
    ++nodeRow;
    if (this.isExpanded(index)) {
      nodeRow += node.getTopSpacerHeight();
      boolean done = false;
      WTreeViewNode c = null;
      for (; nodeRow < this.firstRenderedRow_; ) {
        c = node.nextChildNode((WTreeViewNode) null);
        if (!(c != null)) {
          done = true;
          break;
        }
        if (nodeRow + c.getRenderedHeight() < this.firstRenderedRow_) {
          node.addTopSpacerHeight(c.getRenderedHeight());
          nodeRow += c.getRenderedHeight();
          {
            WWidget toRemove = c.removeFromParent();
            if (toRemove != null) toRemove.remove();
          }

        } else {
          nodeRow = this.pruneNodes(c, nodeRow);
          break;
        }
      }
      if (!done) {
        for (; nodeRow <= this.firstRenderedRow_ + this.validRowCount_; ) {
          c = node.nextChildNode(c);
          if (!(c != null)) {
            done = true;
            break;
          }
          nodeRow = this.pruneNodes(c, nodeRow);
        }
      }
      if (!done) {
        c = node.nextChildNode(c);
        if (c != null) {
          int i = node.getChildContainer().getIndexOf(c);
          int prunedHeight = 0;
          while (c != null && i < node.getChildContainer().getCount()) {
            c = ObjectUtils.cast(node.getChildContainer().getWidget(i), WTreeViewNode.class);
            if (c != null) {
              prunedHeight += c.getRenderedHeight();
              {
                WWidget toRemove = c.removeFromParent();
                if (toRemove != null) toRemove.remove();
              }
            }
          }
          node.addBottomSpacerHeight(prunedHeight);
        }
      }
      nodeRow += node.getBottomSpacerHeight();
      node.normalizeSpacers();
    } else {
      if (node.isChildrenLoaded()) {
        int prunedHeight = 0;
        for (; ; ) {
          WTreeViewNode c = node.nextChildNode((WTreeViewNode) null);
          if (!(c != null)) {
            break;
          }
          prunedHeight += c.getRenderedHeight();
          {
            WWidget toRemove = c.removeFromParent();
            if (toRemove != null) toRemove.remove();
          }

          c = null;
        }
        node.addBottomSpacerHeight(prunedHeight);
        node.normalizeSpacers();
      }
    }
    return nodeRow;
  }

  int adjustRenderedNode(WTreeViewNode node, int theNodeRow) {
    WModelIndex index = node.getModelIndex();
    if (!(index == this.getRootIndex() || (index != null && index.equals(this.getRootIndex())))) {
      ++theNodeRow;
    }
    if (!this.isExpanded(index) && !node.isChildrenLoaded()) {
      return theNodeRow;
    }
    int nodeRow = theNodeRow;
    if (node.isAllSpacer()) {
      if (nodeRow + node.getChildrenHeight() > this.firstRenderedRow_
          && nodeRow < this.firstRenderedRow_ + this.validRowCount_) {
        int childCount = this.getModel().getRowCount(index);
        boolean firstNode = true;
        int rowStubs = 0;
        for (int i = 0; i < childCount; ++i) {
          WModelIndex childIndex = this.getModel().getIndex(i, 0, index);
          int childHeight = this.subTreeHeight(childIndex);
          if (nodeRow <= this.firstRenderedRow_ + this.validRowCount_
              && nodeRow + childHeight > this.firstRenderedRow_) {
            if (firstNode) {
              firstNode = false;
              node.setTopSpacerHeight(rowStubs);
              rowStubs = 0;
            }
            WTreeViewNode n =
                new WTreeViewNode(this, childIndex, childHeight - 1, i == childCount - 1, node);
            node.getChildContainer().addWidget(n);
            int nestedNodeRow = nodeRow;
            nestedNodeRow = this.adjustRenderedNode(n, nestedNodeRow);
            assert nestedNodeRow == nodeRow + childHeight;
          } else {
            rowStubs += childHeight;
          }
          nodeRow += childHeight;
        }
        node.setBottomSpacerHeight(rowStubs);
      } else {
        nodeRow += node.getChildrenHeight();
      }
    } else {
      int topSpacerHeight = node.getTopSpacerHeight();
      int nestedNodeRow = nodeRow + topSpacerHeight;
      WTreeViewNode child = node.nextChildNode((WTreeViewNode) null);
      int childCount = this.getModel().getRowCount(index);
      while (topSpacerHeight != 0 && nodeRow + topSpacerHeight > this.firstRenderedRow_) {
        WTreeViewNode n =
            ObjectUtils.cast(node.getChildContainer().getWidget(1), WTreeViewNode.class);
        assert n != null;
        WModelIndex childIndex = this.getModel().getIndex(n.getModelIndex().getRow() - 1, 0, index);
        assert (childIndex != null);
        int childHeight = this.subTreeHeight(childIndex);
        {
          WTreeViewNode nn =
              n =
                  new WTreeViewNode(
                      this,
                      childIndex,
                      childHeight - 1,
                      childIndex.getRow() == childCount - 1,
                      node);
          node.getChildContainer().insertWidget(1, nn);
        }
        nestedNodeRow = nodeRow + topSpacerHeight - childHeight;
        nestedNodeRow = this.adjustRenderedNode(n, nestedNodeRow);
        assert nestedNodeRow == nodeRow + topSpacerHeight;
        topSpacerHeight -= childHeight;
        node.addTopSpacerHeight(-childHeight);
      }
      for (; child != null; child = node.nextChildNode(child)) {
        nestedNodeRow = this.adjustRenderedNode(child, nestedNodeRow);
      }
      int nch = node.getChildrenHeight();
      int bottomSpacerStart = nch - node.getBottomSpacerHeight();
      while (node.getBottomSpacerHeight() != 0
          && nodeRow + bottomSpacerStart <= this.firstRenderedRow_ + this.validRowCount_) {
        int lastNodeIndex = node.getChildContainer().getCount() - 2;
        WTreeViewNode n =
            ObjectUtils.cast(
                node.getChildContainer().getWidget(lastNodeIndex), WTreeViewNode.class);
        assert n != null;
        WModelIndex childIndex = this.getModel().getIndex(n.getModelIndex().getRow() + 1, 0, index);
        assert (childIndex != null);
        int childHeight = this.subTreeHeight(childIndex);
        {
          WTreeViewNode nn =
              n =
                  new WTreeViewNode(
                      this,
                      childIndex,
                      childHeight - 1,
                      childIndex.getRow() == childCount - 1,
                      node);
          node.getChildContainer().insertWidget(lastNodeIndex + 1, nn);
        }
        nestedNodeRow = nodeRow + bottomSpacerStart;
        nestedNodeRow = this.adjustRenderedNode(n, nestedNodeRow);
        assert nestedNodeRow == nodeRow + bottomSpacerStart + childHeight;
        node.addBottomSpacerHeight(-childHeight);
        bottomSpacerStart += childHeight;
      }
      nodeRow += nch;
    }
    return this.isExpanded(index) ? nodeRow : theNodeRow;
  }

  private WWidget widgetForIndex(final WModelIndex index) {
    if (!(index != null)) {
      return this.rootNode_;
    }
    if (index.getColumn() != 0) {
      return null;
    }
    WTreeViewNode i = this.renderedNodes_.get(index);
    if (i != null) {
      return i;
    } else {
      if (!this.isExpanded(index.getParent())) {
        return null;
      }
      WWidget parent = this.widgetForIndex(index.getParent());
      WTreeViewNode parentNode = ObjectUtils.cast(parent, WTreeViewNode.class);
      if (parentNode != null) {
        int row = this.getIndexRow(index, parentNode.getModelIndex(), 0, Integer.MAX_VALUE);
        return parentNode.widgetForModelRow(row);
      } else {
        return parent;
      }
    }
  }

  private WTreeViewNode nodeForIndex(final WModelIndex index) {
    if ((index == this.getRootIndex() || (index != null && index.equals(this.getRootIndex())))) {
      return this.rootNode_;
    } else {
      WModelIndex column0Index =
          index.getColumn() == 0
              ? index
              : this.getModel().getIndex(index.getRow(), 0, index.getParent());
      WTreeViewNode i = this.renderedNodes_.get(column0Index);
      return i != null ? i : null;
    }
  }

  int subTreeHeight(final WModelIndex index, int lowerBound, int upperBound) {
    int result = 0;
    if (!(index == this.getRootIndex() || (index != null && index.equals(this.getRootIndex())))) {
      ++result;
    }
    if (result >= upperBound) {
      return result;
    }
    if (this.getModel() != null && this.isExpanded(index)) {
      int childCount = this.getModel().getRowCount(index);
      for (int i = 0; i < childCount; ++i) {
        WModelIndex childIndex = this.getModel().getIndex(i, 0, index);
        result += this.subTreeHeight(childIndex, upperBound - result);
        if (result >= upperBound) {
          return result;
        }
      }
    }
    return result;
  }

  final int subTreeHeight(final WModelIndex index) {
    return subTreeHeight(index, 0, Integer.MAX_VALUE);
  }

  final int subTreeHeight(final WModelIndex index, int lowerBound) {
    return subTreeHeight(index, lowerBound, Integer.MAX_VALUE);
  }

  private int renderedRow(final WModelIndex index, WWidget w, int lowerBound, int upperBound) {
    WTreeViewNode node = ObjectUtils.cast(w, WTreeViewNode.class);
    if (node != null) {
      return node.renderedRow(lowerBound, upperBound);
    } else {
      RowSpacer s = ObjectUtils.cast(w, RowSpacer.class);
      int result = s.renderedRow(0, upperBound);
      if (result > upperBound) {
        return result;
      } else {
        if (result + s.getNode().getRenderedHeight() < lowerBound) {
          return result;
        } else {
          return result
              + this.getIndexRow(
                  index, s.getNode().getModelIndex(), lowerBound - result, upperBound - result);
        }
      }
    }
  }

  private final int renderedRow(final WModelIndex index, WWidget w) {
    return renderedRow(index, w, 0, Integer.MAX_VALUE);
  }

  private final int renderedRow(final WModelIndex index, WWidget w, int lowerBound) {
    return renderedRow(index, w, lowerBound, Integer.MAX_VALUE);
  }

  private int getIndexRow(
      final WModelIndex child, final WModelIndex ancestor, int lowerBound, int upperBound) {
    if (!(child != null) || (child == ancestor || (child != null && child.equals(ancestor)))) {
      return 0;
    } else {
      WModelIndex parent = child.getParent();
      int result = 0;
      for (int r = 0; r < child.getRow(); ++r) {
        result +=
            this.subTreeHeight(this.getModel().getIndex(r, 0, parent), 0, upperBound - result);
        if (result >= upperBound) {
          return result;
        }
      }
      if (!(parent == ancestor || (parent != null && parent.equals(ancestor)))) {
        return result
            + 1
            + this.getIndexRow(parent, ancestor, lowerBound - result, upperBound - result);
      } else {
        return result;
      }
    }
  }

  private final int getIndexRow(final WModelIndex child, final WModelIndex ancestor) {
    return getIndexRow(child, ancestor, 0, Integer.MAX_VALUE);
  }

  private final int getIndexRow(
      final WModelIndex child, final WModelIndex ancestor, int lowerBound) {
    return getIndexRow(child, ancestor, lowerBound, Integer.MAX_VALUE);
  }

  String getColumnStyleClass(int column) {
    return this.columnInfo(column).getStyleClass();
  }

  private int getRenderLowerBound() {
    return this.firstRenderedRow_;
  }

  private int getRenderUpperBound() {
    return this.firstRenderedRow_ + this.validRowCount_;
  }

  void renderedRowsChanged(int row, int count) {
    if (count < 0
        && row - count >= this.firstRenderedRow_
        && row < this.firstRenderedRow_ + this.validRowCount_) {
      this.validRowCount_ += Math.max(this.firstRenderedRow_ - row + count, count);
    }
    if (row < this.firstRenderedRow_) {
      this.firstRenderedRow_ += count;
    }
    this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
  }

  private WContainerWidget getHeaderRow() {
    WContainerWidget row = ObjectUtils.cast(this.headers_.getWidget(0), WContainerWidget.class);
    if (this.getRowHeaderCount() != 0) {
      row = ObjectUtils.cast(row.getWidget(0), WContainerWidget.class);
    }
    return row;
  }

  boolean internalSelect(final WModelIndex index, SelectionFlag option) {
    if (this.getSelectionBehavior() == SelectionBehavior.Rows && index.getColumn() != 0) {
      return this.internalSelect(
          this.getModel().getIndex(index.getRow(), 0, index.getParent()), option);
    }
    if (super.internalSelect(index, option)) {
      WTreeViewNode node = this.nodeForIndex(index);
      if (node != null) {
        node.renderSelected(this.isSelected(index), index.getColumn());
      }
      return true;
    } else {
      return false;
    }
  }

  void selectRange(final WModelIndex first, final WModelIndex last) {
    WModelIndex index = first;
    for (; ; ) {
      for (int c = first.getColumn(); c <= last.getColumn(); ++c) {
        WModelIndex ic = this.getModel().getIndex(index.getRow(), c, index.getParent());
        this.internalSelect(ic, SelectionFlag.Select);
        if ((ic == last || (ic != null && ic.equals(last)))) {
          return;
        }
      }
      WModelIndex indexc0 =
          index.getColumn() == 0
              ? index
              : this.getModel().getIndex(index.getRow(), 0, index.getParent());
      if (this.isExpanded(indexc0) && this.getModel().hasChildren(indexc0)) {
        index = this.getModel().getIndex(0, first.getColumn(), indexc0);
      } else {
        for (; ; ) {
          WModelIndex parent = index.getParent();
          if (index.getRow() + 1 < this.getModel().getRowCount(parent)) {
            index = this.getModel().getIndex(index.getRow() + 1, first.getColumn(), parent);
            break;
          } else {
            index = index.getParent();
          }
        }
      }
    }
  }

  private boolean isExpandedRecursive(final WModelIndex index) {
    if (this.isExpanded(index)) {
      if (!(index == this.getRootIndex() || (index != null && index.equals(this.getRootIndex())))) {
        return this.isExpanded(index.getParent());
      } else {
        return true;
      }
    } else {
      return false;
    }
  }

  private void expandChildrenToDepth(final WModelIndex index, int depth) {
    for (int i = 0; i < this.getModel().getRowCount(index); ++i) {
      WModelIndex c = this.getModel().getIndex(i, 0, index);
      this.expand(c);
      if (depth > 1) {
        this.expandChildrenToDepth(c, depth - 1);
      }
    }
  }
  // private void updateColumnWidth(int columnId, int width) ;
  WContainerWidget getHeaderContainer() {
    return this.headerContainer_;
  }

  WWidget headerWidget(int column, boolean contentsOnly) {
    WWidget result = null;
    if (this.headers_ != null && this.headers_.getCount() > 0) {
      if (column == 0) {
        result = this.headers_.getWidget(this.headers_.getCount() - 1);
      } else {
        result = this.getHeaderRow().getWidget(column - 1);
      }
    }
    if (result != null && contentsOnly) {
      return result.find("contents");
    } else {
      return result;
    }
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WTreeView",
        "(function(t,e,i,s,n,l){e.wtObj=this;const o=i.firstChild,c=s.firstChild,d=this,a=t.WT;let u=!1,r=!1,h=!1;function f(t){let i=-1,s=null,n=!1,o=!1,c=null,d=a.target(t);for(;d&&d!==e;){if(a.hasTag(d,\"LI\")){-1===i&&(i=0);s=d.id;break}if(d.className&&0===d.className.indexOf(\"Wt-tv-c\")){0===d.className.indexOf(\"Wt-tv-c\")?i=1*d.className.split(\" \")[0].substring(7):-1===i&&(i=0);\"true\"===d.getAttribute(\"drop\")&&(o=!0);c=d}d.classList.contains(l)&&(n=!0);d=d.parentNode}return{columnId:i,nodeId:s,selected:n,drop:o,el:c}}this.click=function(i,s){const n=f(s);-1!==n.columnId&&t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},n.nodeId+\":\"+n.columnId,\"clicked\",\"\",\"\")};this.dblClick=function(i,s){const n=f(s);-1!==n.columnId&&t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},n.nodeId+\":\"+n.columnId,\"dblclicked\",\"\",\"\")};this.mouseDown=function(i,s){a.capture(null);const n=f(s);if(-1!==n.columnId){t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},n.nodeId+\":\"+n.columnId,\"mousedown\",\"\",\"\");\"true\"===e.getAttribute(\"drag\")&&n.selected&&t._p_.dragStart(e,s)}};this.mouseUp=function(i,s){const n=f(s);-1!==n.columnId&&t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},n.nodeId+\":\"+n.columnId,\"mouseup\",\"\",\"\")};let m=null;function p(i,s,n){const l=f(s);-1!==l.columnId&&t.emit(e,{name:\"itemTouchEvent\",eventObject:i,event:s},l.nodeId+\":\"+l.columnId,n)}this.touchStart=function(t,e){if(e.touches.length>1){clearTimeout(m);m=setTimeout((function(){p(t,e,\"touchselect\")}),1e3)}else{clearTimeout(m);m=setTimeout((function(){p(t,e,\"touchselect\")}),50)}};this.touchMove=function(t,e){if(1===e.touches.length&&m){clearTimeout(m);m=null}};this.touchEnd=function(t,e){if(m){clearTimeout(m);m=null}};this.rootClick=function(i,s){t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},\"\",\"clicked\",\"\",\"\")};this.rootDblClick=function(i,s){t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},\"\",\"dblclicked\",\"\",\"\")};this.rootMouseDown=function(i,s){t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},\"\",\"mousedown\",\"\",\"\")};this.rootMouseUp=function(i,s){t.emit(e,{name:\"itemEvent\",eventObject:i,event:s},\"\",\"mouseup\",\"\",\"\")};this.resizeHandleMDown=function(i,s){const n=i.parentNode.className.split(\" \")[0];if(n){const l=a.getCssRule(\"#\"+e.id+\" .\"+n),o=a.pxself(l,\"width\"),c=document.body.classList.contains(\"Wt-rtl\");let u=-o,r=1e4;c&&([u,r]=[-r,-u]);new a.SizeHandle(a,\"h\",i.offsetWidth,e.offsetHeight,u,r,\"Wt-hsh2\",(function(i){const s=o+(c?-i:i),a=1*n.substring(7);l.style.width=s+\"px\";d.adjustColumns();t.emit(e,\"columnResized\",a,parseInt(s))}),i,e,s,-2,-1)}};let v=!1;function w(){if(!v||!u)return;if(a.isHidden(e))return;v=!1;const i=o.firstChild;let s=c.firstChild,l=0,r=0;const h=c.lastChild.className.split(\" \")[0],f=a.getCssRule(\"#\"+e.id+\" .\"+h);n&&(s=s.firstChild);for(const t of s.childNodes)if(t.className){const i=t.className.split(\" \")[0],s=a.getCssRule(\"#\"+e.id+\" .\"+i);if(\"none\"===s.style.display)continue;l+=a.pxself(s,\"width\")+7}if(!n)if(f.style.width)e.querySelectorAll(`.Wt-headerdiv .${h}`).forEach((function(t){t.style.width=f.style.width}));else{const t=e.scrollWidth-s.offsetWidth-9-15;t>0&&(f.style.width=t+\"px\")}if(\"auto\"!==f.style.width){r=l+a.pxself(f,\"width\")+7;if(n){a.getCssRule(\"#\"+e.id+\" .Wt-tv-rowc\").style.width=l+\"px\";d.wtResize();t.layouts2&&t.layouts2.adjust();e.changed=!0}else{c.style.width=i.style.width=r+\"px\";s.style.width=l+\"px\"}}}this.adjustColumns=function(){if(!v){v=!0;setTimeout(w,0)}};this.setItemDropsEnabled=function(t){r=t};this.setRowDropsEnabled=function(t){h=t};let g=null,y=null;e.handleDragDrop=function(n,l,o,c,d){if(g){g.className=g.classNameOrig;g=null}if(y){!function(t){t.style.position=\"\";t.dropVisual.remove();delete t.dropVisual}(y);y=null}if(\"end\"===n)return;const u=f(o);if(!u.selected&&u.drop&&r&&-1!==u.columnId)if(\"drop\"===n)t.emit(e,{name:\"itemEvent\",eventObject:l,event:o},u.nodeId+\":\"+u.columnId,\"drop\",c,d);else{l.className=\"Wt-valid-drop\";g=u.el;g.classNameOrig=g.className;g.className=g.className+\" Wt-drop-site\"}else if(!u.selected&&h){let r,h=\"bottom\";if(u.nodeId){r=a.$(u.nodeId);h=a.widgetCoordinates(r,o).y-r.clientHeight/2<=0?\"top\":\"bottom\"}else{const t=i.getElementsByClassName(\"Wt-tv-root\");r=1===t.length?t[0].lastChild:s}if(\"drop\"===n)t.emit(e,{name:\"rowDropEvent\",eventObject:l,event:o},u.nodeId+\":\"+u.columnId,c,d,h);else{l.className=\"Wt-valid-drop\";!function(t,e){if(\"top\"===e){const i=t.previousSibling;if(i){t=i;e=\"bottom\"}}const i=document.createElement(\"div\");i.className=\"Wt-drop-site-\"+e;t.style.position=\"relative\";t.appendChild(i);t.dropVisual=i;y=t}(r,h)}}else l.className=\"\"};this.wtResize=function(){u=!0;w();let t,l,r=null,h=a.pxself(e,\"width\");if(0===h)h=e.clientWidth;else if(a.boxSizing(e)){h-=a.px(e,\"borderLeftWidth\");h-=a.px(e,\"borderRightWidth\")}let f=i.offsetWidth-i.clientWidth;f>50&&(f=0);i.clientWidth>0&&(h-=f);if(e.classList.contains(\"column1\")){t=e.querySelector(\".Wt-headerdiv\").lastChild.className.split(\" \")[0];l=a.getCssRule(\"#\"+e.id+\" .\"+t);r=a.pxself(l,\"width\")}if(h>100&&(h!==i.tw||r!==i.c0w||e.changed)){const u=!e.changed;i.tw=h;i.c0w=r;t=e.querySelector(\".Wt-headerdiv\").lastChild.className.split(\" \")[0];l=a.getCssRule(\"#\"+e.id+\" .\"+t);const m=o.firstChild,p=a.getCssRule(\"#\"+e.id+\" .cwidth\"),v=p.style.width===m.offsetWidth+1+\"px\",w=c.firstChild;p.style.width=h+\"px\";i.style.width=h+f+\"px\";if(!document.body.classList.contains(\"Wt-rtl\")){s.style.marginRight=f+\"px\";document.querySelectorAll(`#${e.id} .Wt-scroll`).forEach((function(t){t.style.marginRight=f+\"px\"}))}if(null!==r){const t=h-r-7;if(t>0){const i=Math.min(t,a.pxself(a.getCssRule(\"#\"+e.id+\" .Wt-tv-rowc\"),\"width\"));h-=t-i;c.style.width=h+\"px\";m.style.width=h+\"px\";a.getCssRule(\"#\"+e.id+\" .Wt-tv-row\").style.width=i+\"px\"}}else if(v){c.style.width=p.style.width;m.style.width=p.style.width}else c.style.width=m.offsetWidth+\"px\";!n&&m.offsetWidth-w.offsetWidth>=7&&(l.style.width=m.offsetWidth-w.offsetWidth-7+\"px\");e.changed=!1;u&&d.adjustColumns()}};this.scrollTo=function(t,e,s,n){if(-1!==e){e*=s;const t=i.scrollTop,l=i.clientHeight;0===n&&(t+l<e?n=1:e<t&&(n=2));switch(n){case 1:i.scrollTop=e;break;case 2:i.scrollTop=e-(l-s);break;case 3:i.scrollTop=e-(l-s)/2}window.fakeEvent={object:i};i.onscroll(window.fakeEvent);window.fakeEvent=null}};let b=0;this.setRowHeight=function(t){b=t};const W=function(){if(0===b)return;const t=i.scrollTop;o.children[0].children[0].style.backgroundPosition=\"0px \"+Math.floor(t/(2*b))*(2*b)+\"px\"};i.addEventListener?i.addEventListener(\"scroll\",W):i.attachEvent&&i.attachEvent(\"onscroll\",W);d.adjustColumns()})");
  }

  static void removalsFromSet(
      final List<WModelIndex> toErase, final HashSet<WModelIndex> set, final WModelIndex i) {
    {
      if (set.contains(i) != false) {
        toErase.add(i);
      }
    }
    final int rowCount = i.getModel().getRowCount(i);
    for (int row = 0; row < rowCount; ++row) {
      WModelIndex c = i.getModel().getIndex(row, 0, i);
      removalsFromSet(toErase, set, c);
    }
  }
}
