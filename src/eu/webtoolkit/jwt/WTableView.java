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
 * An MVC View widget for tabular data.
 *
 * <p>The view displays data from a {@link WAbstractItemModel} in a table. It provides incremental
 * rendering, without excessive use of client- or serverside resources.
 *
 * <p>The rendering (and editing) of items is handled by a {@link WAbstractItemDelegate}, by default
 * it uses {@link WItemDelegate} which renders data of all predefined roles (see also {@link
 * ItemDataRole}), including text, icons, checkboxes, and tooltips.
 *
 * <p>The view provides virtual scrolling in both horizontal and vertical directions, and can
 * therefore be used to display large data models (with large number of columns and rows).
 *
 * <p>When the view is updated, it will read the data from the model row per row, starting at the
 * top visible row. If <code></code>(r1,c1) and <code></code>(r2,c2) are two model indexes of
 * visible table cells, and <code>r1</code> <code>&lt;</code> <code>r2</code> or <code>r1</code>
 * <code>==</code> <code>r2</code> and <code>c1</code> <code>&lt;</code> <code>c2</code>, then the
 * data for the first model index is read before the second. Keep this into account when
 * implementing a custom {@link WAbstractItemModel} if you want to optimize performance.
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
 * <p>By default, all columns are given a width of 150px. Column widths of all columns can be set
 * through the API method {@link WTableView#setColumnWidth(int column, WLength width)
 * setColumnWidth()}, and also by the user using handles provided in the header.
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
 * WAbstractItemView#setDropsEnabled(boolean enable) WAbstractItemView#setDropsEnabled()}), the view
 * may receive a drop event on a particular item, at least if the item indicates support for drops
 * (controlled by the {@link ItemFlag#DropEnabled} flag).
 *
 * <p>You may also react to mouse click events on any item, by connecting to one of the {@link
 * WAbstractItemView#clicked()} or {@link WAbstractItemView#doubleClicked()} signals.
 *
 * <p>If a {@link WTableView} is not constrained in height (either by a layout manager or by {@link
 * WWidget#setHeight(WLength height) WWidget#setHeight()}), then it will grow according to the size
 * of the model.
 */
public class WTableView extends WAbstractItemView {
  private static Logger logger = LoggerFactory.getLogger(WTableView.class);

  /** Constructor. */
  public WTableView(WContainerWidget parentContainer) {
    super();
    this.headers_ = null;
    this.canvas_ = null;
    this.table_ = null;
    this.headerContainer_ = null;
    this.contentsContainer_ = null;
    this.headerColumnsCanvas_ = null;
    this.headerColumnsTable_ = null;
    this.headerColumnsHeaderContainer_ = null;
    this.headerColumnsContainer_ = null;
    this.plainTable_ = null;
    this.dropEvent_ =
        new JSignal5<Integer, Integer, String, String, WMouseEvent>(this.impl_, "dropEvent") {};
    this.rowDropEvent_ =
        new JSignal6<Integer, Integer, String, String, String, WMouseEvent>(
            this.impl_, "rowDropEvent") {};
    this.scrolled_ = new JSignal4<Integer, Integer, Integer, Integer>(this.impl_, "scrolled") {};
    this.itemTouchSelectEvent_ = new JSignal1<WTouchEvent>(this.impl_, "itemTouchSelectEvent") {};
    this.touchStartConnection_ = new AbstractSignal.Connection();
    this.touchMoveConnection_ = new AbstractSignal.Connection();
    this.touchEndConnection_ = new AbstractSignal.Connection();
    this.firstColumn_ = -1;
    this.lastColumn_ = -1;
    this.viewportLeft_ = 0;
    this.viewportWidth_ = 1000;
    this.viewportTop_ = 0;
    this.viewportHeight_ = 800;
    this.scrollToRow_ = -1;
    this.scrollToHint_ = ScrollHint.EnsureVisible;
    this.columnResizeConnected_ = false;
    this.preloadMargin_[0] =
        this.preloadMargin_[1] = this.preloadMargin_[2] = this.preloadMargin_[3] = new WLength();
    this.setSelectable(false);
    this.setStyleClass("Wt-itemview Wt-tableview");
    this.setup();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Constructor.
   *
   * <p>Calls {@link #WTableView(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WTableView() {
    this((WContainerWidget) null);
  }

  public void remove() {
    this.impl_.clear();
    super.remove();
  }

  public WWidget itemWidget(final WModelIndex index) {
    if ((index.getColumn() < this.headerColumnsTable_.getCount()
            || this.isColumnRendered(index.getColumn()))
        && this.isRowRendered(index.getRow())) {
      int renderedRow = index.getRow() - this.getFirstRow();
      int renderedCol;
      if (index.getColumn() < this.headerColumnsTable_.getCount()) {
        renderedCol = index.getColumn();
      } else {
        renderedCol =
            this.headerColumnsTable_.getCount() + index.getColumn() - this.getFirstColumn();
      }
      if (this.isAjaxMode()) {
        WTableView.ColumnWidget column = this.columnContainer(renderedCol);
        return column.getWidget(renderedRow);
      } else {
        return this.plainTable_.getElementAt(renderedRow + 1, renderedCol);
      }
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
                  WTableView.this.modelColumnsInserted(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .columnsAboutToBeRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTableView.this.modelColumnsAboutToBeRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .rowsInserted()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTableView.this.modelRowsInserted(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .rowsAboutToBeRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTableView.this.modelRowsAboutToBeRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .rowsRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WTableView.this.modelRowsRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .dataChanged()
            .addListener(
                this,
                (WModelIndex e1, WModelIndex e2) -> {
                  WTableView.this.modelDataChanged(e1, e2);
                }));
    this.modelConnections_.add(
        model
            .headerDataChanged()
            .addListener(
                this,
                (Orientation e1, Integer e2, Integer e3) -> {
                  WTableView.this.modelHeaderDataChanged(e1, e2, e3);
                }));
    this.modelConnections_.add(
        model
            .layoutAboutToBeChanged()
            .addListener(
                this,
                () -> {
                  WTableView.this.modelLayoutAboutToBeChanged();
                }));
    this.modelConnections_.add(
        model
            .layoutChanged()
            .addListener(
                this,
                () -> {
                  WTableView.this.modelLayoutChanged();
                }));
    this.modelConnections_.add(
        model
            .modelReset()
            .addListener(
                this,
                () -> {
                  WTableView.this.modelReset();
                }));
    this.firstColumn_ = this.lastColumn_ = -1;
    this.adjustSize();
  }

  public void setColumnWidth(int column, final WLength width) {
    WLength rWidth = new WLength(Math.round(width.getValue()), width.getUnit());
    double delta = rWidth.toPixels() - this.columnInfo(column).width.toPixels();
    this.columnInfo(column).width = rWidth;
    if (this.columnInfo(column).hidden) {
      delta = 0;
    }
    if (this.isAjaxMode()) {
      this.headers_.setWidth(new WLength(this.headers_.getWidth().toPixels() + delta));
      this.canvas_.setWidth(new WLength(this.canvas_.getWidth().toPixels() + delta));
      if ((int) this.renderState_.getValue()
          >= (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
        return;
      }
      if (this.isColumnRendered(column)) {
        this.updateColumnOffsets();
      } else {
        if (column < this.getFirstColumn()) {
          this.setSpannerCount(Side.Left, this.getSpannerCount(Side.Left));
        }
      }
    }
    if ((int) this.renderState_.getValue()
        >= (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
      return;
    }
    WWidget hc;
    if (column < this.getRowHeaderCount()) {
      hc = this.headerColumnsHeaderContainer_.getWidget(column);
    } else {
      hc = this.headers_.getWidget(column - this.getRowHeaderCount());
    }
    hc.setWidth(new WLength(0));
    hc.setWidth(new WLength(rWidth.toPixels() + 1));
    if (!this.isAjaxMode()) {
      hc.getParent().resize(new WLength(rWidth.toPixels() + 1), hc.getHeight());
    }
  }

  public void setAlternatingRowColors(boolean enable) {
    super.setAlternatingRowColors(enable);
    this.updateTableBackground();
  }

  public void setRowHeight(final WLength rowHeight) {
    int renderedRowCount = this.getModel() != null ? this.getLastRow() - this.getFirstRow() + 1 : 0;
    WLength len = new WLength((int) rowHeight.toPixels());
    super.setRowHeight(len);
    if (this.isAjaxMode()) {
      this.canvas_.setLineHeight(len);
      this.headerColumnsCanvas_.setLineHeight(len);
      if (this.getModel() != null) {
        double ch = this.getCanvasHeight();
        this.canvas_.resize(this.canvas_.getWidth(), new WLength(ch));
        this.headerColumnsCanvas_.setHeight(new WLength(ch));
        double th = renderedRowCount * len.toPixels();
        this.setRenderedHeight(th);
      }
    } else {
      this.plainTable_.setLineHeight(len);
      this.resize(this.getWidth(), this.getHeight());
    }
    this.updateTableBackground();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
  }

  public void setHeaderHeight(final WLength height) {
    super.setHeaderHeight(height);
    if (!this.isAjaxMode()) {
      this.resize(this.getWidth(), this.getHeight());
    }
  }

  public void resize(final WLength width, final WLength height) {
    if (this.isAjaxMode()) {
      if (height.getUnit() == LengthUnit.Percentage) {
        logger.error(
            new StringWriter().append("resize(): height cannot be a Percentage").toString());
        return;
      }
      if (!height.isAuto()) {
        this.viewportHeight_ =
            (int) Math.ceil(height.toPixels() - this.getHeaderHeight().toPixels());
        if (this.scrollToRow_ != -1) {
          WModelIndex index = this.getModel().getIndex(this.scrollToRow_, 0, this.getRootIndex());
          this.scrollToRow_ = -1;
          this.scrollTo(index, this.scrollToHint_);
        }
      } else {
        this.viewportHeight_ = 800;
      }
    } else {
      if (!(this.plainTable_ != null)) {
        return;
      }
      this.plainTable_.setWidth(width);
      if (!height.isAuto()) {
        if (this.impl_.getCount() < 2) {
          this.impl_.addWidget(this.getCreatePageNavigationBar());
        }
      }
    }
    this.computeRenderedArea();
    super.resize(width, height);
    this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
  }

  public void setColumnHidden(int column, boolean hidden) {
    if (this.columnInfo(column).hidden != hidden) {
      super.setColumnHidden(column, hidden);
      int delta = (int) this.columnInfo(column).width.toPixels() + 7;
      if (hidden) {
        delta = -delta;
      }
      if (this.isAjaxMode()) {
        this.headers_.setWidth(new WLength(this.headers_.getWidth().toPixels() + delta));
        this.canvas_.setWidth(new WLength(this.canvas_.getWidth().toPixels() + delta));
        if (this.isColumnRendered(column)) {
          this.updateColumnOffsets();
        } else {
          if (column < this.getFirstColumn()) {
            this.setSpannerCount(Side.Left, this.getSpannerCount(Side.Left));
          }
        }
        if ((int) this.renderState_.getValue()
            >= (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
          return;
        }
        WWidget hc = this.headerWidget(column, false);
        hc.setHidden(hidden);
      } else {
        if ((int) this.renderState_.getValue()
            < (int) WAbstractItemView.RenderState.NeedRerenderData.getValue()) {
          for (int i = 0; i < this.plainTable_.getRowCount(); ++i) {
            this.plainTable_.getElementAt(i, column).setHidden(hidden);
          }
        }
      }
    }
  }

  public void setRowHeaderCount(int count) {
    super.setRowHeaderCount(count);
    this.scheduleRerender(WAbstractItemView.RenderState.NeedRerender);
  }

  public int getPageCount() {
    if (this.getModel() != null) {
      return (this.getModel().getRowCount(this.getRootIndex()) - 1) / this.getPageSize() + 1;
    } else {
      return 1;
    }
  }

  public int getPageSize() {
    if (this.getHeight().isAuto()) {
      return 10000;
    } else {
      final int navigationBarHeight = 25;
      int pageSize =
          (int)
              ((this.getHeight().toPixels()
                      - this.getHeaderHeight().toPixels()
                      - navigationBarHeight)
                  / this.getRowHeight().toPixels());
      if (pageSize <= 0) {
        pageSize = 1;
      }
      return pageSize;
    }
  }

  public int getCurrentPage() {
    return this.renderedFirstRow_ / this.getPageSize();
  }

  public void setCurrentPage(int page) {
    this.renderedFirstRow_ = page * this.getPageSize();
    if (this.getModel() != null) {
      this.renderedLastRow_ =
          Math.min(
              this.renderedFirstRow_ + this.getPageSize() - 1,
              this.getModel().getRowCount(this.getRootIndex()) - 1);
    } else {
      this.renderedLastRow_ = this.renderedFirstRow_;
    }
    this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
  }

  public void scrollTo(final WModelIndex index, ScrollHint hint) {
    if ((index.getParent() == this.getRootIndex()
        || (index.getParent() != null && index.getParent().equals(this.getRootIndex())))) {
      if (this.isAjaxMode()) {
        int rh = (int) this.getRowHeight().toPixels();
        int rowY = index.getRow() * rh;
        if (this.viewportHeight_ != 800) {
          if (hint == ScrollHint.EnsureVisible) {
            if (this.viewportTop_ + this.viewportHeight_ < rowY) {
              hint = ScrollHint.PositionAtTop;
            } else {
              if (rowY < this.viewportTop_) {
                hint = ScrollHint.PositionAtBottom;
              }
            }
          }
          switch (hint) {
            case PositionAtTop:
              this.viewportTop_ = rowY;
              break;
            case PositionAtBottom:
              this.viewportTop_ = rowY - this.viewportHeight_ + rh;
              break;
            case PositionAtCenter:
              this.viewportTop_ = rowY - (this.viewportHeight_ - rh) / 2;
              break;
            default:
              break;
          }
          this.viewportTop_ = Math.max(0, this.viewportTop_);
          if (hint != ScrollHint.EnsureVisible) {
            this.computeRenderedArea();
            this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
          }
        } else {
          this.scrollToRow_ = index.getRow();
          this.scrollToHint_ = hint;
        }
        if (this.isRendered()) {
          StringBuilder s = new StringBuilder();
          s.append(this.getJsRef())
              .append(".wtObj.setScrollToPending();")
              .append("setTimeout(function() {")
              .append(this.getJsRef())
              .append(".wtObj.scrollTo(-1, ")
              .append(rowY)
              .append(",")
              .append((int) hint.getValue())
              .append("); }, 0);");
          this.doJavaScript(s.toString());
        }
      } else {
        this.setCurrentPage(index.getRow() / this.getPageSize());
      }
    }
  }
  /** Scrolls the view x px left and y px top. */
  public void scrollTo(int x, int y) {
    if (this.isAjaxMode()) {
      if (this.isRendered()) {
        StringBuilder s = new StringBuilder();
        s.append(this.getJsRef())
            .append(".wtObj.scrollToPx(")
            .append(x)
            .append(", ")
            .append(y)
            .append(");");
        this.doJavaScript(s.toString());
      }
    }
  }
  /** set css overflow */
  public void setOverflow(Overflow overflow, EnumSet<Orientation> orientation) {
    if (this.contentsContainer_ != null) {
      this.contentsContainer_.setOverflow(overflow, orientation);
    }
  }
  /**
   * set css overflow
   *
   * <p>Calls {@link #setOverflow(Overflow overflow, EnumSet orientation) setOverflow(overflow,
   * EnumSet.of(orientatio, orientation))}
   */
  public final void setOverflow(
      Overflow overflow, Orientation orientatio, Orientation... orientation) {
    setOverflow(overflow, EnumSet.of(orientatio, orientation));
  }
  /**
   * set css overflow
   *
   * <p>Calls {@link #setOverflow(Overflow overflow, EnumSet orientation) setOverflow(overflow,
   * EnumSet.of (Orientation.Horizontal, Orientation.Vertical))}
   */
  public final void setOverflow(Overflow overflow) {
    setOverflow(overflow, EnumSet.of(Orientation.Horizontal, Orientation.Vertical));
  }
  /**
   * Sets preloading margin.
   *
   * <p>By default the table view loads in an area equal to 3 times its height and 3 times its
   * width. This makes it so that the user can scroll a full page in each direction without the
   * delay caused when the table view dynamically needs to load more data.
   *
   * <p>{@link WTableView#setPreloadMargin(WLength margin, EnumSet side) setPreloadMargin()} allows
   * to customize this margin.
   *
   * <p>e.g. if the table view is H pixels high, and C is the preload margin in pixels set on the
   * top and bottom, then enough rows are loaded to fill the area that is H + 2C pixels high. H
   * pixels visible, C pixels above, and C pixels below.
   *
   * <p>Set to 0 pixels if you don&apos;t want to load more rows or columns than are currently
   * visible.
   *
   * <p>Set to a default-constructed {@link WLength} (auto) if you want to keep default behaviour.
   */
  public void setPreloadMargin(final WLength margin, EnumSet<Side> side) {
    if (side.contains(Side.Top)) {
      this.preloadMargin_[0] = margin;
    }
    if (side.contains(Side.Right)) {
      this.preloadMargin_[1] = margin;
    }
    if (side.contains(Side.Bottom)) {
      this.preloadMargin_[2] = margin;
    }
    if (side.contains(Side.Left)) {
      this.preloadMargin_[3] = margin;
    }
    this.computeRenderedArea();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
  }
  /**
   * Sets preloading margin.
   *
   * <p>Calls {@link #setPreloadMargin(WLength margin, EnumSet side) setPreloadMargin(margin,
   * EnumSet.of(sid, side))}
   */
  public final void setPreloadMargin(final WLength margin, Side sid, Side... side) {
    setPreloadMargin(margin, EnumSet.of(sid, side));
  }
  /**
   * Sets preloading margin.
   *
   * <p>Calls {@link #setPreloadMargin(WLength margin, EnumSet side) setPreloadMargin(margin,
   * Side.AllSides)}
   */
  public final void setPreloadMargin(final WLength margin) {
    setPreloadMargin(margin, Side.AllSides);
  }
  /**
   * Retrieves the preloading margin.
   *
   * <p>
   *
   * @see WTableView#setPreloadMargin(WLength margin, EnumSet side)
   */
  public WLength getPreloadMargin(Side side) {
    switch (side) {
      case Top:
        return this.preloadMargin_[0];
      case Right:
        return this.preloadMargin_[1];
      case Bottom:
        return this.preloadMargin_[2];
      case Left:
        return this.preloadMargin_[3];
      default:
        return new WLength();
    }
  }

  public void setHidden(boolean hidden, final WAnimation animation) {
    boolean change = this.isHidden() != hidden;
    super.setHidden(hidden, animation);
    if (change && !hidden) {
      WApplication app = WApplication.getInstance();
      if (app.getEnvironment().hasAjax()
          && this.isRendered()
          && app.getEnvironment().agentIsIE()
          && !app.getEnvironment().agentIsIElt(9)) {
        StringBuilder s = new StringBuilder();
        s.append(this.getJsRef()).append(".wtObj.resetScroll();");
        this.doJavaScript(s.toString());
      }
    }
  }
  /**
   * Returns the model index corresponding to a widget.
   *
   * <p>This returns the model index for the item that is or contains the given widget.
   */
  public WModelIndex getModelIndexAt(WWidget widget) {
    for (WWidget w = widget; w != null; w = w.getParent()) {
      if (w.hasStyleClass("Wt-tv-c")) {
        WTableView.ColumnWidget column =
            ObjectUtils.cast(w.getParent(), WTableView.ColumnWidget.class);
        if (!(column != null)) {
          return null;
        }
        int row = this.getFirstRow() + column.getIndexOf(w);
        int col = column.getColumn();
        return this.getModel().getIndex(row, col, this.getRootIndex());
      }
    }
    return null;
  }

  public EventSignal1<WScrollEvent> scrolled() {
    if (WApplication.getInstance().getEnvironment().hasAjax() && this.contentsContainer_ != null) {
      return this.contentsContainer_.scrolled();
    }
    throw new WException("Scrolled signal existes only with ajax.");
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (flags.contains(RenderFlag.Full)
        && !this.isAjaxMode()
        && WApplication.getInstance().getEnvironment().hasAjax()) {
      this.plainTable_ = null;
      this.setup();
    }
    if (this.isAjaxMode()) {
      if (flags.contains(RenderFlag.Full)) {
        this.defineJavaScript();
      }
      if (!this.canvas_.doubleClicked().isConnected()
          && (this.getEditTriggers().contains(EditTrigger.DoubleClicked)
              || this.doubleClicked().isConnected())) {
        this.canvas_
            .doubleClicked()
            .addListener(
                this,
                (WMouseEvent event) -> {
                  WTableView.this.handleDblClick(false, event);
                });
        this.canvas_.doubleClicked().preventPropagation();
        this.headerColumnsCanvas_
            .doubleClicked()
            .addListener(
                this,
                (WMouseEvent event) -> {
                  WTableView.this.handleDblClick(true, event);
                });
        this.headerColumnsCanvas_.doubleClicked().preventPropagation();
        this.contentsContainer_
            .doubleClicked()
            .addListener(
                this,
                (WMouseEvent event) -> {
                  WTableView.this.handleRootDoubleClick(0, event);
                });
        this.headerColumnsContainer_
            .doubleClicked()
            .addListener(
                this,
                (WMouseEvent event) -> {
                  WTableView.this.handleRootDoubleClick(0, event);
                });
      }
      if (!this.touchStartConnection_.isConnected() && this.touchStarted().isConnected()) {
        this.touchStartConnection_ =
            this.canvas_
                .touchStarted()
                .addListener(
                    this,
                    (WTouchEvent e1) -> {
                      WTableView.this.handleTouchStarted(e1);
                    });
      }
      if (!this.touchMoveConnection_.isConnected() && this.touchMoved().isConnected()) {
        this.touchMoveConnection_ =
            this.canvas_
                .touchMoved()
                .addListener(
                    this,
                    (WTouchEvent e1) -> {
                      WTableView.this.handleTouchMoved(e1);
                    });
      }
      if (!this.touchEndConnection_.isConnected() && this.touchEnded().isConnected()) {
        this.touchEndConnection_ =
            this.canvas_
                .touchEnded()
                .addListener(
                    this,
                    (WTouchEvent e1) -> {
                      WTableView.this.handleTouchEnded(e1);
                    });
      }
      StringBuilder s = new StringBuilder();
      s.append(this.getJsRef())
          .append(".wtObj.setItemDropsEnabled(")
          .append(this.enabledDropLocations_.contains(DropLocation.OnItem))
          .append(");");
      s.append(this.getJsRef())
          .append(".wtObj.setRowDropsEnabled(")
          .append(this.enabledDropLocations_.contains(DropLocation.BetweenRows))
          .append(");");
      this.doJavaScript(s.toString());
    }
    if (this.getModel() != null) {
      while (this.renderState_ != WAbstractItemView.RenderState.RenderOk) {
        WAbstractItemView.RenderState s = this.renderState_;
        this.renderState_ = WAbstractItemView.RenderState.RenderOk;
        switch (s) {
          case NeedRerender:
            this.resetGeometry();
            this.rerenderHeader();
            this.rerenderData();
            break;
          case NeedRerenderHeader:
            this.rerenderHeader();
            break;
          case NeedRerenderData:
            this.rerenderData();
            break;
          case NeedUpdateModelIndexes:
            this.updateModelIndexes();
          case NeedAdjustViewPort:
            this.adjustToViewport();
            break;
          default:
            break;
        }
      }
    }
    super.render(flags);
  }
  /**
   * Called when rows or columns are inserted/removed.
   *
   * <p>Override this method when you want to adjust the table&apos;s size when columns or rows are
   * inserted or removed. The method is also called when the model is reset. The default
   * implementation does nothing.
   */
  protected void adjustSize() {}

  protected void enableAjax() {
    this.plainTable_ = null;
    this.setup();
    this.defineJavaScript();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderHeader);
    super.enableAjax();
  }

  private WTableView.ColumnWidget createColumnWidget(int column) {
    assert this.isAjaxMode();
    WTableView.ColumnWidget result = new WTableView.ColumnWidget(column, (WContainerWidget) null);
    WTableView.ColumnWidget columnWidget = result;
    final WAbstractItemView.ColumnInfo ci = this.columnInfo(column);
    columnWidget.setStyleClass(ci.getStyleClass());
    columnWidget.setPositionScheme(PositionScheme.Absolute);
    columnWidget.setOffsets(new WLength(0), EnumSet.of(Side.Top, Side.Left));
    columnWidget.setOverflow(Overflow.Hidden);
    columnWidget.setHeight(this.table_.getHeight());
    if (column >= this.getRowHeaderCount()) {
      if (this.table_.getCount() == 0 || column > this.columnContainer(-1).getColumn()) {
        this.table_.addWidget(columnWidget);
      } else {
        this.table_.insertWidget(0, columnWidget);
      }
    } else {
      this.headerColumnsTable_.insertWidget(column, columnWidget);
    }
    return result;
  }

  static class ColumnWidget extends WContainerWidget {
    private static Logger logger = LoggerFactory.getLogger(ColumnWidget.class);

    public int getColumn() {
      return this.column_;
    }

    private ColumnWidget(int column, WContainerWidget parentContainer) {
      super();
      this.column_ = column;
      if (parentContainer != null) parentContainer.addWidget(this);
    }

    private ColumnWidget(int column) {
      this(column, (WContainerWidget) null);
    }

    private int column_;
    // private WTableView.ColumnWidget  createColumnWidget(int column) ;
  }

  private WContainerWidget headers_;
  private WContainerWidget canvas_;
  private WContainerWidget table_;
  private WContainerWidget headerContainer_;
  private WContainerWidget contentsContainer_;
  private WContainerWidget headerColumnsCanvas_;
  private WContainerWidget headerColumnsTable_;
  private WContainerWidget headerColumnsHeaderContainer_;
  private WContainerWidget headerColumnsContainer_;
  private WTable plainTable_;
  private JSignal5<Integer, Integer, String, String, WMouseEvent> dropEvent_;
  private JSignal6<Integer, Integer, String, String, String, WMouseEvent> rowDropEvent_;
  private JSignal4<Integer, Integer, Integer, Integer> scrolled_;
  private JSignal1<WTouchEvent> itemTouchSelectEvent_;
  private AbstractSignal.Connection touchStartConnection_;
  private AbstractSignal.Connection touchMoveConnection_;
  private AbstractSignal.Connection touchEndConnection_;
  private WLength[] preloadMargin_ = new WLength[4];
  private int firstColumn_;
  private int lastColumn_;
  private int viewportLeft_;
  private int viewportWidth_;
  private int viewportTop_;
  private int viewportHeight_;
  private int renderedFirstRow_;
  private int renderedLastRow_;
  private int renderedFirstColumn_;
  private int renderedLastColumn_;
  private int scrollToRow_;
  private ScrollHint scrollToHint_;
  private boolean columnResizeConnected_;

  private void updateTableBackground() {
    if (this.isAjaxMode()) {
      WApplication.getInstance()
          .getTheme()
          .apply(this, this.table_, WidgetThemeRole.TableViewRowContainer);
      WApplication.getInstance()
          .getTheme()
          .apply(this, this.headerColumnsTable_, WidgetThemeRole.TableViewRowContainer);
    } else {
      WApplication.getInstance()
          .getTheme()
          .apply(this, this.plainTable_, WidgetThemeRole.TableViewRowContainer);
    }
  }

  private WTableView.ColumnWidget columnContainer(int renderedColumn) {
    assert this.isAjaxMode();
    if (renderedColumn < this.headerColumnsTable_.getCount() && renderedColumn >= 0) {
      return ObjectUtils.cast(
          this.headerColumnsTable_.getWidget(renderedColumn), WTableView.ColumnWidget.class);
    } else {
      if (this.table_.getCount() > 0) {
        if (renderedColumn < 0) {
          return ObjectUtils.cast(
              this.table_.getWidget(this.table_.getCount() - 1), WTableView.ColumnWidget.class);
        } else {
          return ObjectUtils.cast(
              this.table_.getWidget(renderedColumn - this.headerColumnsTable_.getCount()),
              WTableView.ColumnWidget.class);
        }
      } else {
        return null;
      }
    }
  }

  private void modelColumnsInserted(final WModelIndex parent, int start, int end) {
    if (!(parent == this.getRootIndex()
        || (parent != null && parent.equals(this.getRootIndex())))) {
      return;
    }
    int count = end - start + 1;
    int width = 0;
    for (int i = start; i < start + count; ++i) {
      this.columns_.add(0 + i, this.createColumnInfo(i));
      width += (int) this.columnInfo(i).width.toPixels() + 7;
    }
    this.shiftModelIndexColumns(start, end - start + 1);
    if (this.isAjaxMode()) {
      this.canvas_.setWidth(new WLength(this.canvas_.getWidth().toPixels() + width));
    }
    if ((int) this.renderState_.getValue()
        < (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
      this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderHeader);
    }
    if (start > this.getLastColumn() + 1
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerender
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerenderData) {
      return;
    }
    this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
    this.adjustSize();
  }

  private void modelColumnsAboutToBeRemoved(final WModelIndex parent, int start, int end) {
    if (!(parent == this.getRootIndex()
        || (parent != null && parent.equals(this.getRootIndex())))) {
      return;
    }
    for (int r = 0; r < this.getModel().getRowCount(); r++) {
      for (int c = start; c <= end; c++) {
        this.closeEditor(this.getModel().getIndex(r, c), false);
      }
    }
    this.shiftModelIndexColumns(start, -(end - start + 1));
    int count = end - start + 1;
    int width = 0;
    for (int i = start; i < start + count; ++i) {
      if (!this.columnInfo(i).hidden) {
        width += (int) this.columnInfo(i).width.toPixels() + 7;
      }
    }
    WApplication app = WApplication.getInstance();
    for (int i = start; i < start + count; ++i) {
      app.getStyleSheet().removeRule(this.columns_.get(i).styleRule);
    }
    for (int ii = 0; ii < (0 + start + count) - (0 + start); ++ii) this.columns_.remove(0 + start);
    ;
    if (this.isAjaxMode()) {
      this.canvas_.setWidth(new WLength(this.canvas_.getWidth().toPixels() - width));
    }
    if (start <= this.currentSortColumn_ && this.currentSortColumn_ <= end) {
      this.currentSortColumn_ = -1;
    }
    if ((int) this.renderState_.getValue()
        < (int) WAbstractItemView.RenderState.NeedRerenderHeader.getValue()) {
      this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderHeader);
    }
    if (start > this.getLastColumn()
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerender
        || this.renderState_ == WAbstractItemView.RenderState.NeedRerenderData) {
      return;
    }
    this.resetGeometry();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
    this.adjustSize();
  }

  private void modelRowsInserted(final WModelIndex parent, int start, int end) {
    if (!(parent == this.getRootIndex()
        || (parent != null && parent.equals(this.getRootIndex())))) {
      return;
    }
    int count = end - start + 1;
    this.shiftModelIndexRows(start, count);
    this.computeRenderedArea();
    if (this.isAjaxMode()) {
      this.canvas_.setHeight(new WLength(this.getCanvasHeight()));
      this.headerColumnsCanvas_.setHeight(new WLength(this.getCanvasHeight()));
      this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
      if (start < this.getFirstRow()) {
        this.setSpannerCount(Side.Top, this.getSpannerCount(Side.Top) + count);
      } else {
        if (start <= this.getLastRow()) {
          this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
        }
      }
    } else {
      if (start <= this.getLastRow()) {
        this.scheduleRerender(WAbstractItemView.RenderState.NeedRerenderData);
      }
    }
    this.adjustSize();
  }

  private void modelRowsAboutToBeRemoved(final WModelIndex parent, int start, int end) {
    if (!(parent == this.getRootIndex()
        || (parent != null && parent.equals(this.getRootIndex())))) {
      return;
    }
    for (int c = 0; c < this.getColumnCount(); c++) {
      for (int r = start; r <= end; r++) {
        this.closeEditor(this.getModel().getIndex(r, c), false);
      }
    }
    this.shiftModelIndexRows(start, -(end - start + 1));
    int overlapTop = calcOverlap(0, this.getSpannerCount(Side.Top), start, end + 1);
    int overlapMiddle = calcOverlap(this.getFirstRow(), this.getLastRow() + 1, start, end + 1);
    if (overlapMiddle > 0) {
      int first = Math.max(0, start - this.getFirstRow());
      for (int i = 0; i < this.getRenderedColumnsCount(); ++i) {
        WTableView.ColumnWidget column = this.columnContainer(i);
        for (int j = 0; j < overlapMiddle; ++j) {
          {
            WWidget toRemove = column.getWidget(first).removeFromParent();
            if (toRemove != null) toRemove.remove();
          }
        }
      }
      this.setSpannerCount(Side.Bottom, this.getSpannerCount(Side.Bottom) + overlapMiddle);
    }
    if (overlapTop > 0) {
      this.setSpannerCount(Side.Top, this.getSpannerCount(Side.Top) - overlapTop);
      this.setSpannerCount(Side.Bottom, this.getSpannerCount(Side.Bottom) + overlapTop);
    }
  }

  private void modelRowsRemoved(final WModelIndex parent, int start, int end) {
    if (!(parent == this.getRootIndex()
        || (parent != null && parent.equals(this.getRootIndex())))) {
      return;
    }
    if (this.isAjaxMode()) {
      this.canvas_.setHeight(new WLength(this.getCanvasHeight()));
      this.headerColumnsCanvas_.setHeight(new WLength(this.getCanvasHeight()));
      this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
    }
    this.scheduleRerender(WAbstractItemView.RenderState.NeedUpdateModelIndexes);
    this.computeRenderedArea();
    this.adjustSize();
  }

  void modelDataChanged(final WModelIndex topLeft, final WModelIndex bottomRight) {
    if (!(topLeft.getParent() == this.getRootIndex()
        || (topLeft.getParent() != null && topLeft.getParent().equals(this.getRootIndex())))) {
      return;
    }
    if ((int) this.renderState_.getValue()
        < (int) WAbstractItemView.RenderState.NeedRerenderData.getValue()) {
      int row1 = Math.max(topLeft.getRow(), this.getFirstRow());
      int row2 = Math.min(bottomRight.getRow(), this.getLastRow());
      int col1 = Math.max(topLeft.getColumn(), this.getFirstColumn());
      int col2 = Math.min(bottomRight.getColumn(), this.getLastColumn());
      for (int i = row1; i <= row2; ++i) {
        int renderedRow = i - this.getFirstRow();
        int rhc = this.isAjaxMode() ? this.getRowHeaderCount() : 0;
        for (int j = topLeft.getColumn(); j < rhc; ++j) {
          int renderedColumn = j;
          WModelIndex index = this.getModel().getIndex(i, j, this.getRootIndex());
          this.updateItem(index, renderedRow, renderedColumn);
        }
        for (int j = col1; j <= col2; ++j) {
          int renderedColumn = rhc + j - this.getFirstColumn();
          WModelIndex index = this.getModel().getIndex(i, j, this.getRootIndex());
          this.updateItem(index, renderedRow, renderedColumn);
        }
      }
    }
  }

  void modelLayoutChanged() {
    super.modelLayoutChanged();
    this.resetGeometry();
  }

  private WWidget renderWidget(WWidget widget, final WModelIndex index) {
    WAbstractItemDelegate itemDelegate = this.getItemDelegate(index.getColumn());
    EnumSet<ViewItemRenderFlag> renderFlags = EnumSet.noneOf(ViewItemRenderFlag.class);
    if (this.isAjaxMode()) {
      if (this.isSelected(index)) {
        renderFlags.add(ViewItemRenderFlag.Selected);
      }
    }
    if (this.isEditing(index)) {
      renderFlags.add(ViewItemRenderFlag.Editing);
      if (this.hasEditFocus(index)) {
        renderFlags.add(ViewItemRenderFlag.Focused);
      }
    }
    if (!this.isValid(index)) {
      renderFlags.add(ViewItemRenderFlag.Invalid);
    }
    boolean initial = !(widget != null);
    WWidget wAfter = itemDelegate.update(widget, index, renderFlags);
    if (wAfter != null) {
      widget = wAfter;
    }
    widget.setInline(false);
    widget.addStyleClass("Wt-tv-c");
    widget.setHeight(this.getRowHeight());
    if (renderFlags.contains(ViewItemRenderFlag.Editing)) {
      widget.setTabIndex(-1);
      this.setEditorWidget(index, widget);
    }
    if (initial) {
      if (renderFlags.contains(ViewItemRenderFlag.Editing)) {
        Object state = this.getEditState(index);
        if ((state != null)) {
          itemDelegate.setEditState(widget, index, state);
        }
      }
    }
    return wAfter;
  }

  private int getSpannerCount(final Side side) {
    assert this.isAjaxMode();
    switch (side) {
      case Top:
        {
          return (int)
              (this.table_.getOffset(Side.Top).toPixels() / this.getRowHeight().toPixels());
        }
      case Bottom:
        {
          return (int)
              (this.getModel().getRowCount(this.getRootIndex())
                  - (this.table_.getOffset(Side.Top).toPixels()
                          + this.table_.getHeight().toPixels())
                      / this.getRowHeight().toPixels());
        }
      case Left:
        return this.firstColumn_;
      case Right:
        return this.getColumnCount() - (this.lastColumn_ + 1);
      default:
        assert false;
        return -1;
    }
  }

  private void setSpannerCount(final Side side, final int count) {
    assert this.isAjaxMode();
    switch (side) {
      case Top:
        {
          int size =
              this.getModel().getRowCount(this.getRootIndex())
                  - count
                  - this.getSpannerCount(Side.Bottom);
          double to = count * this.getRowHeight().toPixels();
          this.table_.setOffsets(new WLength(to), EnumSet.of(Side.Top));
          this.headerColumnsTable_.setOffsets(new WLength(to), EnumSet.of(Side.Top));
          double th = size * this.getRowHeight().toPixels();
          this.setRenderedHeight(th);
          break;
        }
      case Bottom:
        {
          int size =
              this.getModel().getRowCount(this.getRootIndex())
                  - this.getSpannerCount(Side.Top)
                  - count;
          double th = size * this.getRowHeight().toPixels();
          this.setRenderedHeight(th);
          break;
        }
      case Left:
        {
          int total = 0;
          for (int i = this.getRowHeaderCount(); i < count; i++) {
            if (!this.columnInfo(i).hidden) {
              total += (int) this.columnInfo(i).width.toPixels() + 7;
            }
          }
          this.table_.setOffsets(new WLength(total), EnumSet.of(Side.Left));
          this.firstColumn_ = count;
          break;
        }
      case Right:
        this.lastColumn_ = this.getColumnCount() - count - 1;
        break;
      default:
        assert false;
    }
  }

  private void renderTable(final int fr, final int lr, final int fc, final int lc) {
    assert this.isAjaxMode();
    if (fr > this.getLastRow()
        || this.getFirstRow() > lr
        || fc > this.getLastColumn()
        || this.getFirstColumn() > lc) {
      this.reset();
    }
    int oldFirstRow = this.getFirstRow();
    int oldLastRow = this.getLastRow();
    int topRowsToAdd = 0;
    int bottomRowsToAdd = 0;
    if (oldLastRow - oldFirstRow < 0) {
      topRowsToAdd = 0;
      this.setSpannerCount(Side.Top, fr);
      this.setSpannerCount(Side.Bottom, this.getModel().getRowCount(this.getRootIndex()) - fr);
      bottomRowsToAdd = lr - fr + 1;
    } else {
      topRowsToAdd = this.getFirstRow() - fr;
      bottomRowsToAdd = lr - this.getLastRow();
    }
    int oldFirstCol = this.getFirstColumn();
    int oldLastCol = this.getLastColumn();
    int leftColsToAdd = 0;
    int rightColsToAdd = 0;
    if (oldLastCol - oldFirstCol < 0) {
      leftColsToAdd = 0;
      this.setSpannerCount(Side.Left, fc);
      this.setSpannerCount(Side.Right, this.getColumnCount() - fc);
      rightColsToAdd = lc - fc + 1;
    } else {
      leftColsToAdd = this.getFirstColumn() - fc;
      rightColsToAdd = lc - this.getLastColumn();
    }
    for (int i = 0; i < -leftColsToAdd; ++i) {
      this.removeSection(Side.Left);
    }
    for (int i = 0; i < -rightColsToAdd; ++i) {
      this.removeSection(Side.Right);
    }
    for (int i = 0; i < -topRowsToAdd; ++i) {
      this.removeSection(Side.Top);
    }
    for (int i = 0; i < -bottomRowsToAdd; ++i) {
      this.removeSection(Side.Bottom);
    }
    for (int i = 0; i < leftColsToAdd; ++i) {
      this.addSection(Side.Left);
    }
    for (int i = 0; i < rightColsToAdd; ++i) {
      this.addSection(Side.Right);
    }
    for (int i = 0; i < topRowsToAdd; ++i) {
      int row = fr + i;
      for (int col = 0; col < this.getRowHeaderCount(); ++col) {
        WTableView.ColumnWidget w = this.columnContainer(col);
        w.insertWidget(
            i,
            this.renderWidget(
                (WWidget) null, this.getModel().getIndex(row, col, this.getRootIndex())));
      }
      for (int col = fc; col <= lc; ++col) {
        WTableView.ColumnWidget w = this.columnContainer(col - fc + this.getRowHeaderCount());
        w.insertWidget(
            i,
            this.renderWidget(
                (WWidget) null, this.getModel().getIndex(row, col, this.getRootIndex())));
      }
      this.addSection(Side.Top);
    }
    if (oldLastRow != -1 && (leftColsToAdd > 0 || rightColsToAdd > 0)) {
      for (int row = Math.max(oldFirstRow, fr); row <= Math.min(oldLastRow, lr); ++row) {
        for (int j = 0; j < leftColsToAdd; ++j) {
          int col = fc + j;
          int renderCol = this.getRowHeaderCount() + j;
          WTableView.ColumnWidget w = this.columnContainer(renderCol);
          w.addWidget(
              this.renderWidget(
                  (WWidget) null, this.getModel().getIndex(row, col, this.getRootIndex())));
        }
        for (int j = 0; j < rightColsToAdd; ++j) {
          int col = lc - rightColsToAdd + 1 + j;
          WTableView.ColumnWidget w = this.columnContainer(col - fc + this.getRowHeaderCount());
          w.addWidget(
              this.renderWidget(
                  (WWidget) null, this.getModel().getIndex(row, col, this.getRootIndex())));
        }
      }
    }
    for (int i = 0; i < bottomRowsToAdd; ++i) {
      int row = oldLastRow == -1 ? fr + i : oldLastRow + 1 + i;
      for (int col = 0; col < this.getRowHeaderCount(); ++col) {
        WTableView.ColumnWidget w = this.columnContainer(col);
        w.addWidget(
            this.renderWidget(
                (WWidget) null, this.getModel().getIndex(row, col, this.getRootIndex())));
      }
      for (int col = fc; col <= lc; ++col) {
        WTableView.ColumnWidget w = this.columnContainer(col - fc + this.getRowHeaderCount());
        w.addWidget(
            this.renderWidget(
                (WWidget) null, this.getModel().getIndex(row, col, this.getRootIndex())));
      }
      this.addSection(Side.Bottom);
    }
    this.updateColumnOffsets();
    assert this.getLastRow() == lr && this.getFirstRow() == fr;
    assert this.getLastColumn() == lc && this.getFirstColumn() == fc;
    final double marginTop =
        (this.getPreloadMargin(Side.Top).isAuto()
                ? this.viewportHeight_
                : this.getPreloadMargin(Side.Top).toPixels())
            / 2;
    final double marginBottom =
        (this.getPreloadMargin(Side.Bottom).isAuto()
                ? this.viewportHeight_
                : this.getPreloadMargin(Side.Bottom).toPixels())
            / 2;
    final double marginLeft =
        (this.getPreloadMargin(Side.Left).isAuto()
                ? this.viewportWidth_
                : this.getPreloadMargin(Side.Left).toPixels())
            / 2;
    final double marginRight =
        (this.getPreloadMargin(Side.Right).isAuto()
                ? this.viewportWidth_
                : this.getPreloadMargin(Side.Right).toPixels())
            / 2;
    final double scrollX1 = Math.round(Math.max(0.0, this.viewportLeft_ - marginLeft));
    final double scrollX2 = Math.round(this.viewportLeft_ + marginRight);
    final double scrollY1 = Math.round(Math.max(0.0, this.viewportTop_ - marginTop));
    final double scrollY2 = Math.round(this.viewportTop_ + marginBottom);
    StringBuilder s = new StringBuilder();
    char[] buf = new char[30];
    s.append(this.getJsRef()).append(".wtObj.scrolled(");
    s.append(MathUtils.roundJs(scrollX1, 3)).append(", ");
    s.append(MathUtils.roundJs(scrollX2, 3)).append(", ");
    s.append(MathUtils.roundJs(scrollY1, 3)).append(", ");
    s.append(MathUtils.roundJs(scrollY2, 3)).append(");");
    this.doJavaScript(s.toString());
  }

  private void addSection(final Side side) {
    assert this.isAjaxMode();
    switch (side) {
      case Top:
        this.setSpannerCount(side, this.getSpannerCount(side) - 1);
        break;
      case Bottom:
        this.setSpannerCount(side, this.getSpannerCount(side) - 1);
        break;
      case Left:
        {
          WTableView.ColumnWidget w = this.createColumnWidget(this.getFirstColumn() - 1);
          if (!this.columnInfo(w.getColumn()).hidden) {
            this.table_.setOffsets(
                new WLength(
                    this.table_.getOffset(Side.Left).toPixels()
                        - this.getColumnWidth(w.getColumn()).toPixels()
                        - 7),
                EnumSet.of(Side.Left));
          } else {
            w.hide();
          }
          --this.firstColumn_;
          break;
        }
      case Right:
        {
          WTableView.ColumnWidget w = this.createColumnWidget(this.getLastColumn() + 1);
          if (this.columnInfo(w.getColumn()).hidden) {
            w.hide();
          }
          ++this.lastColumn_;
          break;
        }
      default:
        assert false;
    }
  }

  private void removeSection(final Side side) {
    assert this.isAjaxMode();
    int row = this.getFirstRow();
    int col = this.getFirstColumn();
    switch (side) {
      case Top:
        this.setSpannerCount(side, this.getSpannerCount(side) + 1);
        for (int i = 0; i < this.getRenderedColumnsCount(); ++i) {
          WTableView.ColumnWidget w = this.columnContainer(i);
          this.deleteItem(row, col + i, w.getWidget(0));
        }
        break;
      case Bottom:
        row = this.getLastRow();
        this.setSpannerCount(side, this.getSpannerCount(side) + 1);
        for (int i = 0; i < this.getRenderedColumnsCount(); ++i) {
          WTableView.ColumnWidget w = this.columnContainer(i);
          this.deleteItem(row, col + i, w.getWidget(w.getCount() - 1));
        }
        break;
      case Left:
        {
          WTableView.ColumnWidget w = this.columnContainer(this.getRowHeaderCount());
          if (!this.columnInfo(w.getColumn()).hidden) {
            this.table_.setOffsets(
                new WLength(
                    this.table_.getOffset(Side.Left).toPixels()
                        + this.getColumnWidth(w.getColumn()).toPixels()
                        + 7),
                EnumSet.of(Side.Left));
          }
          ++this.firstColumn_;
          for (int i = w.getCount() - 1; i >= 0; --i) {
            this.deleteItem(row + i, col, w.getWidget(i));
          }
          {
            WWidget toRemove = w.removeFromParent();
            if (toRemove != null) toRemove.remove();
          }

          break;
        }
      case Right:
        {
          WTableView.ColumnWidget w = this.columnContainer(-1);
          col = w.getColumn();
          --this.lastColumn_;
          for (int i = w.getCount() - 1; i >= 0; --i) {
            this.deleteItem(row + i, col, w.getWidget(i));
          }
          {
            WWidget toRemove = w.removeFromParent();
            if (toRemove != null) toRemove.remove();
          }

          break;
        }
      default:
        break;
    }
  }

  private int getFirstRow() {
    if (this.isAjaxMode()) {
      return this.getSpannerCount(Side.Top);
    } else {
      return this.renderedFirstRow_;
    }
  }

  private int getLastRow() {
    if (this.isAjaxMode()) {
      return this.getModel().getRowCount(this.getRootIndex())
          - this.getSpannerCount(Side.Bottom)
          - 1;
    } else {
      return this.renderedLastRow_;
    }
  }

  private int getFirstColumn() {
    if (this.isAjaxMode()) {
      return this.firstColumn_;
    } else {
      return 0;
    }
  }

  private int getLastColumn() {
    if (this.isAjaxMode()) {
      return this.lastColumn_;
    } else {
      return this.getColumnCount() - 1;
    }
  }

  private void setup() {
    this.impl_.clear();
    WApplication app = WApplication.getInstance();
    if (app.getEnvironment().hasAjax()) {
      this.impl_.setPositionScheme(PositionScheme.Relative);
      this.headers_ = new WContainerWidget();
      this.headers_.setStyleClass("Wt-headerdiv headerrh");
      this.table_ = new WContainerWidget();
      this.table_.setStyleClass("Wt-tv-contents");
      this.table_.setPositionScheme(PositionScheme.Absolute);
      this.table_.setWidth(new WLength(100, LengthUnit.Percentage));
      WGridLayout layout = new WGridLayout();
      layout.setHorizontalSpacing(0);
      layout.setVerticalSpacing(0);
      layout.setContentsMargins(0, 0, 0, 0);
      this.headerContainer_ = new WContainerWidget();
      this.headerContainer_.setStyleClass("Wt-header headerrh");
      this.headerContainer_.setOverflow(Overflow.Hidden);
      this.headerContainer_.addWidget(this.headers_);
      this.canvas_ = new WContainerWidget();
      this.canvas_.setStyleClass("Wt-spacer");
      this.canvas_.setPositionScheme(PositionScheme.Relative);
      this.canvas_
          .clicked()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleSingleClick(false, event);
              });
      this.canvas_
          .clicked()
          .addListener("function(o, e) { document.dispatchEvent(new Event(e.type, e));}");
      this.canvas_.clicked().preventPropagation();
      this.canvas_
          .mouseWentDown()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleMouseWentDown(false, event);
              });
      this.canvas_.mouseWentDown().preventPropagation();
      this.canvas_
          .mouseWentDown()
          .addListener("function(o, e) { document.dispatchEvent(new Event(e.type, e));}");
      this.canvas_
          .mouseWentUp()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleMouseWentUp(false, event);
              });
      this.canvas_.mouseWentUp().preventPropagation();
      this.canvas_
          .mouseWentUp()
          .addListener("function(o, e) { document.dispatchEvent(new Event(e.type, e));}");
      this.canvas_.addWidget(this.table_);
      this.contentsContainer_ = new WContainerWidget();
      this.contentsContainer_.setOverflow(Overflow.Auto);
      this.contentsContainer_.setPositionScheme(PositionScheme.Absolute);
      this.contentsContainer_.addWidget(this.canvas_);
      this.contentsContainer_
          .clicked()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleRootSingleClick(0, event);
              });
      this.contentsContainer_
          .mouseWentUp()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleRootMouseWentUp(0, event);
              });
      this.headerColumnsHeaderContainer_ = new WContainerWidget();
      this.headerColumnsHeaderContainer_.setStyleClass("Wt-header Wt-headerdiv headerrh");
      this.headerColumnsHeaderContainer_.hide();
      this.headerColumnsTable_ = new WContainerWidget();
      this.headerColumnsTable_.setStyleClass("Wt-tv-contents");
      this.headerColumnsTable_.setPositionScheme(PositionScheme.Absolute);
      this.headerColumnsTable_.setWidth(new WLength(100, LengthUnit.Percentage));
      this.headerColumnsCanvas_ = new WContainerWidget();
      this.headerColumnsCanvas_.setPositionScheme(PositionScheme.Relative);
      this.headerColumnsCanvas_.clicked().preventPropagation();
      this.headerColumnsCanvas_
          .clicked()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleSingleClick(true, event);
              });
      this.headerColumnsCanvas_
          .clicked()
          .addListener("function(o, e) { document.dispatchEvent(new Event(e.type, e));}");
      this.headerColumnsCanvas_.mouseWentDown().preventPropagation();
      this.headerColumnsCanvas_
          .mouseWentDown()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleMouseWentDown(true, event);
              });
      this.headerColumnsCanvas_
          .mouseWentDown()
          .addListener("function(o, e) { document.dispatchEvent(new Event(e.type, e));}");
      this.headerColumnsCanvas_.mouseWentUp().preventPropagation();
      this.headerColumnsCanvas_
          .mouseWentUp()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleMouseWentUp(true, event);
              });
      this.headerColumnsCanvas_
          .mouseWentUp()
          .addListener("function(o, e) { document.dispatchEvent(new Event(e.type, e));}");
      this.headerColumnsCanvas_.addWidget(this.headerColumnsTable_);
      this.headerColumnsContainer_ = new WContainerWidget();
      this.headerColumnsContainer_.setPositionScheme(PositionScheme.Absolute);
      this.headerColumnsContainer_.setOverflow(Overflow.Hidden);
      this.headerColumnsContainer_.addWidget(this.headerColumnsCanvas_);
      this.headerColumnsContainer_.hide();
      this.headerColumnsContainer_
          .clicked()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleRootSingleClick(0, event);
              });
      this.headerColumnsContainer_
          .mouseWentUp()
          .addListener(
              this,
              (WMouseEvent event) -> {
                WTableView.this.handleRootMouseWentUp(0, event);
              });
      layout.addWidget(this.headerColumnsHeaderContainer_, 0, 0);
      layout.addWidget(this.headerContainer_, 0, 1);
      layout.addWidget(this.headerColumnsContainer_, 1, 0);
      layout.addWidget(this.contentsContainer_, 1, 1);
      for (int i = 0; i < layout.getCount(); ++i) {
        layout.getItemAt(i).getWidget().addStyleClass("tcontainer");
      }
      layout.setRowStretch(1, 1);
      layout.setColumnStretch(1, 1);
      this.impl_.setLayout(layout);
    } else {
      this.plainTable_ = new WTable();
      this.plainTable_.setStyleClass("Wt-plaintable");
      this.plainTable_.setAttributeValue("style", "table-layout: fixed;");
      this.plainTable_.setHeaderCount(1);
      this.impl_.addWidget(this.plainTable_);
      this.resize(this.getWidth(), this.getHeight());
    }
    this.setRowHeight(this.getRowHeight());
    this.updateTableBackground();
  }

  private void reset() {
    assert this.isAjaxMode();
    int total = 0;
    for (int i = 0; i < this.getColumnCount(); ++i) {
      if (!this.columnInfo(i).hidden) {
        total += (int) this.columnInfo(i).width.toPixels() + 7;
      }
    }
    this.headers_.setWidth(new WLength(total));
    this.canvas_.resize(new WLength(total), new WLength(this.getCanvasHeight()));
    this.headerColumnsCanvas_.setHeight(new WLength(this.getCanvasHeight()));
    this.computeRenderedArea();
    int renderedRows = this.getLastRow() - this.getFirstRow() + 1;
    for (int i = 0; i < renderedRows; ++i) {
      this.removeSection(Side.Top);
    }
    this.setSpannerCount(Side.Top, 0);
    this.setSpannerCount(Side.Left, this.getRowHeaderCount());
    this.table_.clear();
    this.setSpannerCount(Side.Bottom, this.getModel().getRowCount(this.getRootIndex()));
    this.setSpannerCount(Side.Right, this.getColumnCount() - this.getRowHeaderCount());
    this.headerColumnsTable_.clear();
    for (int i = 0; i < this.getRowHeaderCount(); ++i) {
      this.createColumnWidget(i);
    }
  }

  private void rerenderHeader() {
    this.saveExtraHeaderWidgets();
    if (this.isAjaxMode()) {
      this.headers_.clear();
      this.headerColumnsHeaderContainer_.clear();
      for (int i = 0; i < this.getColumnCount(); ++i) {
        WWidget w = this.createHeaderWidget(i);
        w.setFloatSide(Side.Left);
        w.setWidth(new WLength(this.columnInfo(i).width.toPixels() + 1));
        if (this.columnInfo(i).hidden) {
          w.hide();
        }
        if (i < this.getRowHeaderCount()) {
          this.headerColumnsHeaderContainer_.addWidget(w);
        } else {
          this.headers_.addWidget(w);
        }
      }
    } else {
      for (int i = 0; i < this.getColumnCount(); ++i) {
        WWidget w = this.createHeaderWidget(i);
        WTableCell cell = this.plainTable_.getElementAt(0, i);
        cell.clear();
        cell.setStyleClass("headerrh");
        w.setWidth(new WLength(this.columnInfo(i).width.toPixels() + 1));
        cell.resize(new WLength(this.columnInfo(i).width.toPixels() + 1), w.getHeight());
        if (this.columnInfo(i).hidden) {
          cell.hide();
        }
        cell.addWidget(w);
      }
    }
  }

  private void rerenderData() {
    if (this.isAjaxMode()) {
      this.reset();
      this.renderTable(
          this.renderedFirstRow_,
          this.renderedLastRow_,
          this.renderedFirstColumn_,
          this.renderedLastColumn_);
    } else {
      this.pageChanged().trigger();
      while (this.plainTable_.getRowCount() > 1) {
        this.plainTable_.removeRow(this.plainTable_.getRowCount() - 1);
      }
      for (int i = this.getFirstRow(); i <= this.getLastRow(); ++i) {
        int renderedRow = i - this.getFirstRow();
        String cl = WApplication.getInstance().getTheme().getActiveClass();
        if (this.getSelectionBehavior() == SelectionBehavior.Rows
            && this.isSelected(this.getModel().getIndex(i, 0, this.getRootIndex()))) {
          WTableRow row = this.plainTable_.getRowAt(renderedRow + 1);
          row.setStyleClass(cl);
        }
        for (int j = this.getFirstColumn(); j <= this.getLastColumn(); ++j) {
          int renderedCol = j - this.getFirstColumn();
          final WModelIndex index = this.getModel().getIndex(i, j, this.getRootIndex());
          WWidget w = this.renderWidget((WWidget) null, index);
          WTableCell cell = this.plainTable_.getElementAt(renderedRow + 1, renderedCol);
          if (this.columnInfo(j).hidden) {
            cell.hide();
          }
          WInteractWidget wi = ObjectUtils.cast(w, WInteractWidget.class);
          if (wi != null && !this.isEditing(index)) {
            wi.clicked()
                .addListener(
                    this,
                    (WMouseEvent event) -> {
                      WTableView.this.handleClick(index, event);
                    });
          }
          if (this.getSelectionBehavior() == SelectionBehavior.Items && this.isSelected(index)) {
            cell.setStyleClass(cl);
          }
          cell.addWidget(w);
        }
      }
    }
  }

  private void adjustToViewport() {
    assert this.isAjaxMode();
    this.computeRenderedArea();
    if (this.renderedFirstRow_ != this.getFirstRow()
        || this.renderedLastRow_ != this.getLastRow()
        || this.renderedFirstColumn_ != this.getFirstColumn()
        || this.renderedLastColumn_ != this.getLastColumn()) {
      this.renderTable(
          this.renderedFirstRow_,
          this.renderedLastRow_,
          this.renderedFirstColumn_,
          this.renderedLastColumn_);
    }
  }

  private void computeRenderedArea() {
    if (this.isAjaxMode()) {
      final int borderRows = 5;
      int modelHeight = 0;
      if (this.getModel() != null) {
        modelHeight = this.getModel().getRowCount(this.getRootIndex());
      }
      if (this.viewportHeight_ != -1) {
        final int top = Math.min(this.viewportTop_, (int) this.canvas_.getHeight().toPixels());
        final int height =
            Math.min(this.viewportHeight_, (int) this.canvas_.getHeight().toPixels());
        final double renderedRows = height / this.getRowHeight().toPixels();
        final double renderedRowsAbove =
            this.getPreloadMargin(Side.Top).isAuto()
                ? renderedRows + borderRows
                : this.getPreloadMargin(Side.Top).toPixels() / this.getRowHeight().toPixels();
        final double renderedRowsBelow =
            this.getPreloadMargin(Side.Bottom).isAuto()
                ? renderedRows + borderRows
                : this.getPreloadMargin(Side.Bottom).toPixels() / this.getRowHeight().toPixels();
        this.renderedFirstRow_ = (int) Math.floor(top / this.getRowHeight().toPixels());
        this.renderedLastRow_ =
            (int)
                Math.ceil(
                    Math.min(
                        this.renderedFirstRow_ + renderedRows + renderedRowsBelow,
                        modelHeight - 1.0));
        this.renderedFirstRow_ =
            (int) Math.floor(Math.max(this.renderedFirstRow_ - renderedRowsAbove, 0.0));
      } else {
        this.renderedFirstRow_ = 0;
        this.renderedLastRow_ = modelHeight - 1;
      }
      if (this.renderedFirstRow_ % 2 == 1) {
        --this.renderedFirstRow_;
      }
      final int borderColumnPixels = 200;
      final double marginLeft =
          this.getPreloadMargin(Side.Left).isAuto()
              ? this.viewportWidth_ + borderColumnPixels
              : this.getPreloadMargin(Side.Left).toPixels();
      final double marginRight =
          this.getPreloadMargin(Side.Right).isAuto()
              ? this.viewportWidth_ + borderColumnPixels
              : this.getPreloadMargin(Side.Right).toPixels();
      int left = (int) Math.floor(Math.max(0.0, this.viewportLeft_ - marginLeft));
      int right =
          (int)
              Math.ceil(
                  Math.min(
                      Math.max(this.canvas_.getWidth().toPixels(), this.viewportWidth_ * 1.0),
                      this.viewportLeft_ + this.viewportWidth_ + marginRight));
      int total = 0;
      this.renderedFirstColumn_ = this.getRowHeaderCount();
      this.renderedLastColumn_ = this.getColumnCount() - 1;
      for (int i = this.getRowHeaderCount(); i < this.getColumnCount(); i++) {
        if (this.columnInfo(i).hidden) {
          continue;
        }
        int w = (int) this.columnInfo(i).width.toPixels();
        if (total <= left && left < total + w) {
          this.renderedFirstColumn_ = i;
        }
        if (total <= right && right < total + w) {
          this.renderedLastColumn_ = i;
          break;
        }
        total += w + 7;
      }
      assert this.renderedLastColumn_ == -1
          || this.renderedFirstColumn_ <= this.renderedLastColumn_;
    } else {
      this.renderedFirstColumn_ = 0;
      if (this.getModel() != null) {
        this.renderedLastColumn_ = this.getColumnCount() - 1;
        int cp = Math.max(0, Math.min(this.getCurrentPage(), this.getPageCount() - 1));
        this.setCurrentPage(cp);
      } else {
        this.renderedFirstRow_ = this.renderedLastRow_ = 0;
      }
    }
  }

  WContainerWidget getHeaderContainer() {
    return this.headerContainer_;
  }

  WWidget headerWidget(int column, boolean contentsOnly) {
    WWidget result = null;
    if (this.isAjaxMode()) {
      if (this.headers_ != null) {
        if (column < this.headerColumnsTable_.getCount()) {
          if (column < this.headerColumnsHeaderContainer_.getCount()) {
            result = this.headerColumnsHeaderContainer_.getWidget(column);
          }
        } else {
          if (column - this.headerColumnsTable_.getCount() < this.headers_.getCount()) {
            result = this.headers_.getWidget(column - this.headerColumnsTable_.getCount());
          }
        }
      }
    } else {
      if (this.plainTable_ != null && column < this.plainTable_.getColumnCount()) {
        result = this.plainTable_.getElementAt(0, column).getWidget(0);
      }
    }
    if (result != null && contentsOnly) {
      return result.find("contents");
    } else {
      return result;
    }
  }

  private void onViewportChange(int left, int top, int width, int height) {
    assert this.isAjaxMode();
    this.viewportLeft_ = left;
    this.viewportWidth_ = width;
    this.viewportTop_ = top;
    this.viewportHeight_ = height;
    if (this.scrollToRow_ != -1) {
      WModelIndex index = this.getModel().getIndex(this.scrollToRow_, 0, this.getRootIndex());
      this.scrollToRow_ = -1;
      this.scrollTo(index, this.scrollToHint_);
    }
    this.computeRenderedArea();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
  }

  private void onColumnResize() {
    this.computeRenderedArea();
    this.scheduleRerender(WAbstractItemView.RenderState.NeedAdjustViewPort);
  }

  private void resetGeometry() {
    if (this.isAjaxMode()) {
      this.reset();
    } else {
      this.renderedLastRow_ =
          Math.min(
              this.getModel().getRowCount(this.getRootIndex()) - 1,
              this.renderedFirstRow_ + this.getPageSize() - 1);
      this.renderedLastColumn_ = this.getColumnCount() - 1;
    }
  }

  private void handleSingleClick(boolean headerColumns, final WMouseEvent event) {
    WModelIndex index = this.translateModelIndex(headerColumns, event);
    this.handleClick(index, event);
  }

  private void handleDblClick(boolean headerColumns, final WMouseEvent event) {
    WModelIndex index = this.translateModelIndex(headerColumns, event);
    this.handleDoubleClick(index, event);
  }

  private void handleMouseWentDown(boolean headerColumns, final WMouseEvent event) {
    WModelIndex index = this.translateModelIndex(headerColumns, event);
    this.handleMouseDown(index, event);
  }

  private void handleMouseWentUp(boolean headerColumns, final WMouseEvent event) {
    WModelIndex index = this.translateModelIndex(headerColumns, event);
    this.handleMouseUp(index, event);
  }

  private void handleTouchSelected(final WTouchEvent event) {
    List<WModelIndex> indices = new ArrayList<WModelIndex>();
    for (int i = 0; i < event.getTouches().size(); i++) {
      indices.add(this.translateModelIndex(event.getTouches().get(i)));
    }
    this.handleTouchSelect(indices, event);
  }

  private void handleTouchStarted(final WTouchEvent event) {
    List<WModelIndex> indices = new ArrayList<WModelIndex>();
    final List<Touch> touches = event.getChangedTouches();
    for (int i = 0; i < touches.size(); i++) {
      indices.add(this.translateModelIndex(touches.get(i)));
    }
    this.handleTouchStart(indices, event);
  }

  private void handleTouchMoved(final WTouchEvent event) {
    List<WModelIndex> indices = new ArrayList<WModelIndex>();
    final List<Touch> touches = event.getChangedTouches();
    for (int i = 0; i < touches.size(); i++) {
      indices.add(this.translateModelIndex(touches.get(i)));
    }
    this.handleTouchMove(indices, event);
  }

  private void handleTouchEnded(final WTouchEvent event) {
    List<WModelIndex> indices = new ArrayList<WModelIndex>();
    final List<Touch> touches = event.getChangedTouches();
    for (int i = 0; i < touches.size(); i++) {
      indices.add(this.translateModelIndex(touches.get(i)));
    }
    this.handleTouchEnd(indices, event);
  }

  private WModelIndex translateModelIndex(boolean headerColumns, final WMouseEvent event) {
    int row = (int) (event.getWidget().y / this.getRowHeight().toPixels());
    int column = -1;
    int total = 0;
    if (headerColumns) {
      for (int i = 0; i < this.getRowHeaderCount(); ++i) {
        if (!this.columnInfo(i).hidden) {
          total += (int) this.columnInfo(i).width.toPixels() + 7;
        }
        if (event.getWidget().x < total) {
          column = i;
          break;
        }
      }
    } else {
      for (int i = this.getRowHeaderCount(); i < this.getColumnCount(); i++) {
        if (!this.columnInfo(i).hidden) {
          total += (int) this.columnInfo(i).width.toPixels() + 7;
        }
        if (event.getWidget().x < total) {
          column = i;
          break;
        }
      }
    }
    if (column >= 0 && row >= 0 && row < this.getModel().getRowCount(this.getRootIndex())) {
      return this.getModel().getIndex(row, column, this.getRootIndex());
    } else {
      return null;
    }
  }

  private WModelIndex translateModelIndex(final Touch touch) {
    int row = (int) (touch.getWidget().y / this.getRowHeight().toPixels());
    int column = -1;
    int total = 0;
    for (int i = this.getRowHeaderCount(); i < this.getColumnCount(); i++) {
      if (!this.columnInfo(i).hidden) {
        total += (int) this.columnInfo(i).width.toPixels() + 7;
      }
      if (touch.getWidget().x < total) {
        column = i;
        break;
      }
    }
    if (column >= 0 && row >= 0 && row < this.getModel().getRowCount(this.getRootIndex())) {
      return this.getModel().getIndex(row, column, this.getRootIndex());
    } else {
      return null;
    }
  }

  private void handleRootSingleClick(int u, final WMouseEvent event) {
    this.handleClick(null, event);
  }

  private void handleRootDoubleClick(int u, final WMouseEvent event) {
    this.handleDoubleClick(null, event);
  }

  private void handleRootMouseWentDown(int u, final WMouseEvent event) {
    this.handleMouseDown(null, event);
  }

  private void handleRootMouseWentUp(int u, final WMouseEvent event) {
    this.handleMouseUp(null, event);
  }

  private void updateItem(final WModelIndex index, int renderedRow, int renderedColumn) {
    WContainerWidget parentWidget;
    int wIndex;
    if (this.isAjaxMode()) {
      parentWidget = this.columnContainer(renderedColumn);
      wIndex = renderedRow;
    } else {
      parentWidget = this.plainTable_.getElementAt(renderedRow + 1, renderedColumn);
      wIndex = 0;
    }
    WWidget current = parentWidget.getWidget(wIndex);
    WWidget wAfter = this.renderWidget(current, index);
    WWidget w = null;
    if (wAfter != null) {
      w = wAfter;
    } else {
      w = current;
    }
    if (wAfter != null) {
      {
        WWidget toRemove = parentWidget.removeWidget(current);
        if (toRemove != null) toRemove.remove();
      }

      parentWidget.insertWidget(wIndex, wAfter);
      if (!this.isAjaxMode() && !this.isEditing(index)) {
        WInteractWidget wi = ObjectUtils.cast(w, WInteractWidget.class);
        if (wi != null) {
          wi.clicked()
              .addListener(
                  this,
                  (WMouseEvent event) -> {
                    WTableView.this.handleClick(index, event);
                  });
        }
      }
    }
  }

  boolean internalSelect(final WModelIndex index, SelectionFlag option) {
    if (this.getSelectionBehavior() == SelectionBehavior.Rows && index.getColumn() != 0) {
      return this.internalSelect(
          this.getModel().getIndex(index.getRow(), 0, index.getParent()), option);
    }
    if (super.internalSelect(index, option)) {
      this.renderSelected(this.isSelected(index), index);
      return true;
    } else {
      return false;
    }
  }

  void selectRange(final WModelIndex first, final WModelIndex last) {
    for (int c = first.getColumn(); c <= last.getColumn(); ++c) {
      for (int r = first.getRow(); r <= last.getRow(); ++r) {
        this.internalSelect(
            this.getModel().getIndex(r, c, this.getRootIndex()), SelectionFlag.Select);
      }
    }
  }

  private void shiftModelIndexRows(int start, int count) {
    final SortedSet<WModelIndex> set = this.getSelectionModel().selection_;
    List<WModelIndex> toShift = new ArrayList<WModelIndex>();
    List<WModelIndex> toErase = new ArrayList<WModelIndex>();
    for (Iterator<WModelIndex> it_it =
            set.tailSet(this.getModel().getIndex(start, 0, this.getRootIndex())).iterator();
        it_it.hasNext(); ) {
      WModelIndex it = it_it.next();
      if (count < 0) {
        if (it.getRow() < start - count) {
          toErase.add(it);
          continue;
        }
      }
      toShift.add(it);
      toErase.add(it);
    }
    for (int i = 0; i < toErase.size(); ++i) {
      set.remove(toErase.get(i));
    }
    for (int i = 0; i < toShift.size(); ++i) {
      WModelIndex newIndex =
          this.getModel()
              .getIndex(
                  toShift.get(i).getRow() + count,
                  toShift.get(i).getColumn(),
                  toShift.get(i).getParent());
      set.add(newIndex);
    }
    this.shiftEditorRows(this.getRootIndex(), start, count, true);
    if (!toErase.isEmpty()) {
      this.selectionChanged().trigger();
    }
  }

  private void shiftModelIndexColumns(int start, int count) {
    final SortedSet<WModelIndex> set = this.getSelectionModel().selection_;
    List<WModelIndex> toShift = new ArrayList<WModelIndex>();
    List<WModelIndex> toErase = new ArrayList<WModelIndex>();
    for (Iterator<WModelIndex> it_it = set.iterator(); it_it.hasNext(); ) {
      WModelIndex it = it_it.next();
      if (count < 0) {
        if (it.getColumn() < start - count) {
          toErase.add(it);
          continue;
        }
      }
      if (it.getColumn() >= start) {
        toShift.add(it);
        toErase.add(it);
      }
    }
    for (int i = 0; i < toErase.size(); ++i) {
      set.remove(toErase.get(i));
    }
    for (int i = 0; i < toShift.size(); ++i) {
      WModelIndex newIndex =
          this.getModel()
              .getIndex(
                  toShift.get(i).getRow(),
                  toShift.get(i).getColumn() + count,
                  toShift.get(i).getParent());
      set.add(newIndex);
    }
    this.shiftEditorColumns(this.getRootIndex(), start, count, true);
    if (!toShift.isEmpty() || !toErase.isEmpty()) {
      this.selectionChanged().trigger();
    }
  }

  private void renderSelected(boolean selected, final WModelIndex index) {
    String cl = WApplication.getInstance().getTheme().getActiveClass();
    if (this.getSelectionBehavior() == SelectionBehavior.Rows) {
      if (this.isRowRendered(index.getRow())) {
        int renderedRow = index.getRow() - this.getFirstRow();
        if (this.isAjaxMode()) {
          for (int i = 0; i < this.getRenderedColumnsCount(); ++i) {
            WTableView.ColumnWidget column = this.columnContainer(i);
            WWidget w = column.getWidget(renderedRow);
            w.toggleStyleClass(cl, selected);
          }
        } else {
          WTableRow row = this.plainTable_.getRowAt(renderedRow + 1);
          row.toggleStyleClass(cl, selected);
        }
      }
    } else {
      WWidget w = this.itemWidget(index);
      if (w != null) {
        w.toggleStyleClass(cl, selected);
      }
    }
  }

  private int getRenderedColumnsCount() {
    assert this.isAjaxMode();
    return this.headerColumnsTable_.getCount() + this.table_.getCount();
  }

  private void defineJavaScript() {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WTableView.js", wtjs1());
    StringBuilder s = new StringBuilder();
    s.append("new Wt4.11.2.WTableView(")
        .append(app.getJavaScriptClass())
        .append(',')
        .append(this.getJsRef())
        .append(',')
        .append(this.contentsContainer_.getJsRef())
        .append(',')
        .append(this.viewportTop_)
        .append(',')
        .append(this.headerContainer_.getJsRef())
        .append(',')
        .append(this.headerColumnsContainer_.getJsRef())
        .append(",'")
        .append(WApplication.getInstance().getTheme().getActiveClass())
        .append("');");
    this.setJavaScriptMember(" WTableView", s.toString());
    if (!this.dropEvent_.isConnected()) {
      this.dropEvent_.addListener(
          this,
          (Integer e1, Integer e2, String e3, String e4, WMouseEvent e5) -> {
            WTableView.this.onDropEvent(e1, e2, e3, e4, e5);
          });
    }
    if (!this.rowDropEvent_.isConnected()) {
      this.rowDropEvent_.addListener(
          this,
          (Integer e1, Integer e2, String e3, String e4, String e5, WMouseEvent e6) -> {
            WTableView.this.onRowDropEvent(e1, e2, e3, e4, e5, e6);
          });
    }
    if (!this.scrolled_.isConnected()) {
      this.scrolled_.addListener(
          this,
          (Integer e1, Integer e2, Integer e3, Integer e4) -> {
            WTableView.this.onViewportChange(e1, e2, e3, e4);
          });
    }
    if (!this.itemTouchSelectEvent_.isConnected()) {
      this.itemTouchSelectEvent_.addListener(
          this,
          (WTouchEvent e1) -> {
            WTableView.this.handleTouchSelected(e1);
          });
    }
    if (!this.columnResizeConnected_) {
      this.columnResized()
          .addListener(
              this,
              (Integer e1, WLength e2) -> {
                WTableView.this.onColumnResize();
              });
      this.columnResizeConnected_ = true;
    }
    if (this.canvas_ != null) {
      app.addAutoJavaScript(
          "{var obj = " + this.getJsRef() + ";if (obj && obj.wtObj) obj.wtObj.autoJavaScript();}");
      this.connectObjJS(this.canvas_.mouseWentDown(), "mouseDown");
      this.connectObjJS(this.canvas_.mouseWentUp(), "mouseUp");
      final AbstractEventSignal a = this.canvas_.touchStarted();
      this.connectObjJS(this.canvas_.touchStarted(), "touchStart");
      this.connectObjJS(this.canvas_.touchMoved(), "touchMove");
      this.connectObjJS(this.canvas_.touchEnded(), "touchEnd");
      final AbstractEventSignal ccScrolled = this.contentsContainer_.scrolled();
      this.connectObjJS(ccScrolled, "onContentsContainerScroll");
      final AbstractEventSignal cKeyDown = this.canvas_.keyWentDown();
      this.connectObjJS(cKeyDown, "onkeydown");
    }
  }

  private boolean isRowRendered(final int row) {
    return row >= this.getFirstRow() && row <= this.getLastRow();
  }

  private boolean isColumnRendered(final int column) {
    return column >= this.getFirstColumn() && column <= this.getLastColumn();
  }

  private void updateColumnOffsets() {
    assert this.isAjaxMode();
    int totalRendered = 0;
    for (int i = 0; i < this.getRowHeaderCount(); ++i) {
      WAbstractItemView.ColumnInfo ci = this.columnInfo(i);
      WTableView.ColumnWidget w = this.columnContainer(i);
      w.setOffsets(new WLength(0), EnumSet.of(Side.Left));
      w.setOffsets(new WLength(totalRendered), EnumSet.of(Side.Left));
      w.setWidth(new WLength(0));
      w.setWidth(new WLength(ci.width.toPixels() + 7));
      if (!this.columnInfo(i).hidden) {
        totalRendered += (int) ci.width.toPixels() + 7;
      }
      w.setHidden(ci.hidden);
    }
    this.headerColumnsContainer_.setWidth(new WLength(totalRendered));
    this.headerColumnsCanvas_.setWidth(new WLength(totalRendered));
    this.headerColumnsTable_.setWidth(new WLength(totalRendered));
    this.headerColumnsHeaderContainer_.setWidth(new WLength(totalRendered));
    this.headerColumnsContainer_.setHidden(totalRendered == 0);
    this.headerColumnsHeaderContainer_.setHidden(totalRendered == 0);
    int fc = this.getFirstColumn();
    int lc = this.getLastColumn();
    totalRendered = 0;
    int total = 0;
    for (int i = this.getRowHeaderCount(); i < this.getColumnCount(); ++i) {
      WAbstractItemView.ColumnInfo ci = this.columnInfo(i);
      if (i >= fc && i <= lc) {
        WTableView.ColumnWidget w = this.columnContainer(this.getRowHeaderCount() + i - fc);
        w.setOffsets(new WLength(0), EnumSet.of(Side.Left));
        w.setOffsets(new WLength(totalRendered), EnumSet.of(Side.Left));
        w.setWidth(new WLength(0));
        w.setWidth(new WLength(ci.width.toPixels() + 7));
        if (!this.columnInfo(i).hidden) {
          totalRendered += (int) ci.width.toPixels() + 7;
        }
        w.setHidden(ci.hidden);
      }
      if (!this.columnInfo(i).hidden) {
        total += (int) this.columnInfo(i).width.toPixels() + 7;
      }
    }
    double ch = this.getCanvasHeight();
    this.canvas_.resize(new WLength(total), new WLength(ch));
    this.headerColumnsCanvas_.setHeight(new WLength(ch));
    this.headers_.setWidth(new WLength(total));
    this.table_.setWidth(new WLength(totalRendered));
  }

  private void updateModelIndexes() {
    int row1 = this.getFirstRow();
    int row2 = this.getLastRow();
    int col1 = this.getFirstColumn();
    int col2 = this.getLastColumn();
    for (int i = row1; i <= row2; ++i) {
      int renderedRow = i - this.getFirstRow();
      int rhc = this.isAjaxMode() ? this.getRowHeaderCount() : 0;
      for (int j = 0; j < rhc; ++j) {
        int renderedColumn = j;
        WModelIndex index = this.getModel().getIndex(i, j, this.getRootIndex());
        this.updateModelIndex(index, renderedRow, renderedColumn);
      }
      for (int j = col1; j <= col2; ++j) {
        int renderedColumn = rhc + j - this.getFirstColumn();
        WModelIndex index = this.getModel().getIndex(i, j, this.getRootIndex());
        this.updateModelIndex(index, renderedRow, renderedColumn);
      }
    }
  }

  private void updateModelIndex(final WModelIndex index, int renderedRow, int renderedColumn) {
    WContainerWidget parentWidget;
    int wIndex;
    if (this.isAjaxMode()) {
      parentWidget = this.columnContainer(renderedColumn);
      wIndex = renderedRow;
    } else {
      parentWidget = this.plainTable_.getElementAt(renderedRow + 1, renderedColumn);
      wIndex = 0;
    }
    WAbstractItemDelegate itemDelegate = this.getItemDelegate(index.getColumn());
    WWidget widget = parentWidget.getWidget(wIndex);
    itemDelegate.updateModelIndex(widget, index);
  }

  private void onDropEvent(
      int renderedRow, int columnId, String sourceId, String mimeType, WMouseEvent event) {
    WDropEvent e =
        new WDropEvent(WApplication.getInstance().decodeObject(sourceId), mimeType, event);
    WModelIndex index =
        this.getModel()
            .getIndex(
                this.getFirstRow() + renderedRow, this.columnById(columnId), this.getRootIndex());
    this.dropEvent(e, index);
  }

  private void onRowDropEvent(
      int renderedRow,
      int columnId,
      String sourceId,
      String mimeType,
      String side,
      WMouseEvent event) {
    WDropEvent e =
        new WDropEvent(WApplication.getInstance().decodeObject(sourceId), mimeType, event);
    WModelIndex index = null;
    if (renderedRow >= 0) {
      index =
          this.getModel()
              .getIndex(
                  this.getFirstRow() + renderedRow, this.columnById(columnId), this.getRootIndex());
    }
    this.dropEvent(e, index, side.equals("top") ? Side.Top : Side.Bottom);
  }

  private void deleteItem(int row, int col, WWidget w) {
    this.persistEditor(this.getModel().getIndex(row, col, this.getRootIndex()));
    {
      WWidget toRemove = w.removeFromParent();
      if (toRemove != null) toRemove.remove();
    }
  }

  private boolean isAjaxMode() {
    return this.table_ != null;
  }

  private double getCanvasHeight() {
    return Math.max(
        1.0, this.getModel().getRowCount(this.getRootIndex()) * this.getRowHeight().toPixels());
  }

  private void setRenderedHeight(double th) {
    this.table_.setHeight(new WLength(th));
    this.headerColumnsTable_.setHeight(new WLength(th));
    for (int i = 0; i < this.getRenderedColumnsCount(); ++i) {
      WTableView.ColumnWidget w = this.columnContainer(i);
      w.setHeight(new WLength(th));
    }
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WTableView",
        "(function(t,e,l,o,i,s,n){e.wtObj=this;const c=this,r=t.WT,d=document.body.classList.contains(\"Wt-rtl\");function a(t){return d?r.isGecko?-t.scrollLeft:t.scrollWidth-t.clientWidth-t.scrollLeft:t.scrollLeft}let f=0,u=0,h=0,p=0,g=0,m=0===o,T=!1,b=!1,y=0,N=0,w=0,v=0;this.onContentsContainerScroll=function(){N=i.scrollLeft=l.scrollLeft;y=s.scrollTop=l.scrollTop;l.clientWidth&&l.clientHeight&&0===g&&(l.scrollTop<h||l.scrollTop>p||l.scrollLeft<f||l.scrollLeft>u)&&t.emit(e,\"scrolled\",Math.round(a(l)),Math.round(l.scrollTop),Math.round(l.clientWidth),Math.round(l.clientHeight))};l.wtResize=function(l,i,s,n){if(!m){l.scrollTop=o;l.onscroll();m=!0}if(i-w>(u-f)/2||s-v>(p-h)/2){w=i;v=s;const o=l.clientHeight===l.firstChild.offsetHeight?-1:l.clientHeight;t.emit(e,\"scrolled\",Math.round(a(l)),Math.round(l.scrollTop),Math.round(l.clientWidth),Math.round(o))}};function x(t){let e=0;for(const l of t.parentNode.childNodes){if(l.isEqualNode(t))return e;e++}return-1}function E(t){let e=-1,l=-1,o=!1,i=!1,s=null,c=r.target(t);for(;c&&void 0!==c.classList&&!c.classList.contains(\"Wt-tv-contents\");){if(c.classList.contains(\"Wt-tv-c\")){\"true\"===c.getAttribute(\"drop\")&&(i=!0);c.classList.contains(n)&&(o=!0);s=c;c=c.parentNode;e=1*c.className.split(\" \")[0].substring(7);l=x(s);break}c=c.parentNode}return{columnId:e,rowIdx:l,selected:o,drop:i,el:s}}function L(){return r.pxself(l.firstChild,\"lineHeight\")}let W,S=null;this.mouseDown=function(l,o){r.capture(null);const i=E(o);if(!o.ctrlKey&&!o.shiftKey){const l={ctrlKey:o.ctrlKey,shiftKey:o.shiftKey,target:o.target,srcElement:o.srcElement,type:o.type,which:o.which,touches:o.touches,changedTouches:o.changedTouches,pageX:o.pageX,pageY:o.pageY,clientX:o.clientX,clientY:o.clientY};S=setTimeout((function(){\"true\"===e.getAttribute(\"drag\")&&function(t){return t.el.classList.contains(n)}(i)&&t._p_.dragStart(e,l)}),400)}};this.mouseUp=function(){clearTimeout(S)};this.resizeHandleMDown=function(o,n){const d=o.parentNode;let a=-(r.pxself(d,\"width\")-1),f=1e4;document.body.classList.contains(\"Wt-rtl\")&&([a,f]=[-f,-a]);new r.SizeHandle(r,\"h\",o.offsetWidth,e.offsetHeight,a,f,\"Wt-hsh2\",(function(o){!function(o,n){const d=document.body.classList.contains(\"Wt-rtl\");d&&(n=-n);const a=o.className.split(\" \")[0],f=1*a.substring(7),u=o.parentNode,h=u.parentNode!==i,p=h?s.firstChild:l.firstChild,g=p.firstChild,m=p.querySelector(\".\"+a),T=r.pxself(o,\"width\")-1+n;let b=o.nextSibling,y=m.nextSibling;const N=r.pxself(u,\"width\")+n+\"px\";u.style.width=p.style.width=g.style.width=N;if(h){s.style.width=N;s.firstChild.style.width=N;l.style.left=N;i.style.left=N}o.style.width=T+1+\"px\";m.style.width=T+7+\"px\";t.layouts2.adjust(e.childNodes[0].id,[[1,1]]);for(;b;b=b.nextSibling)if(y){d?y.style.right=r.pxself(y,\"right\")+n+\"px\":y.style.left=r.pxself(y,\"left\")+n+\"px\";y=y.nextSibling}t.emit(e,\"columnResized\",f,parseInt(T));c.autoJavaScript()}(d,o)}),o,e,n,-2,-1)};let C=0;this.touchStart=function(t,e){if(e.touches.length>1){clearTimeout(W);W=setTimeout((function(){k(t,e)}),1e3);C=e.touches.length}else{clearTimeout(W);W=setTimeout((function(){k(t,e)}),50);C=1}};function k(l,o){t.emit(e,{name:\"itemTouchSelectEvent\",eventObject:l,event:o})}this.touchMove=function(t,e){1===e.touches.length&&W&&clearTimeout(W)};this.touchEnd=function(){W&&1!==C&&clearTimeout(W)};this.scrolled=function(t,e,l,o){f=t;u=e;h=l;p=o};this.resetScroll=function(){i.scrollLeft=N;l.scrollLeft=N;l.scrollTop=y;s.scrollTop=y};this.setScrollToPending=function(){g+=1};this.scrollToPx=function(t,e){y=e;N=t;this.resetScroll()};this.scrollTo=function(t,e,o){g>0&&(g-=1);if(-1!==e){const t=l.scrollTop,i=l.clientHeight;0===o&&(t+i<e?o=1:e<t&&(o=2));switch(o){case 1:l.scrollTop=e;break;case 2:l.scrollTop=e-(i-L());break;case 3:l.scrollTop=e-(i-L())/2}l.onscroll()}};this.setItemDropsEnabled=function(t){T=t};this.setRowDropsEnabled=function(t){b=t};function H(t,e,l){if(l){const l=document.createElement(\"div\");l.className=\"Wt-drop-site-\"+e;t.style.position=\"relative\";t.appendChild(l);t.dropVisual=l}else{t.style.position=\"\";t.dropVisual.remove();delete t.dropVisual}}function I(t,e,o){if(-1===t){H(i,\"bottom\",o);return}if(\"top\"===e&&t>0){t-=1;e=\"bottom\"}const s=l.firstChild.firstChild;for(const l of s.childNodes){H(l.childNodes[t],e,o)}}function M(){const t=l.firstChild.firstChild.firstChild;return t?t.childNodes.length:0}let R=null,D=null;e.handleDragDrop=function(l,o,i,s,n){if(R){R.className=R.classNameOrig;R=null}if(D){I(D.row,D.side,!1);D=null}if(\"end\"===l)return;const c=E(i);if(null!==c.ele)if(!c.selected&&c.drop&&T)if(\"drop\"===l)t.emit(e,{name:\"dropEvent\",eventObject:o,event:i},c.rowIdx,c.columnId,s,n);else{o.className=\"Wt-valid-drop\";R=c.el;R.classNameOrig=R.className;R.className=R.className+\" Wt-drop-site\"}else if(!c.selected&&b){if(!c.columnId){c.el=null;c.rowIdx=-1}const r=c.rowIdx,d=c.columnId,a=\"bottom\";if(\"drop\"===l)t.emit(e,{name:\"rowDropEvent\",eventObject:o,event:i},r,d,s,n,a);else{o.className=\"Wt-valid-drop\";D={row:c.el?r:M()-1,side:a};I(D.row,D.side,!0)}}else o.className=\"\"};this.onkeydown=function(t){const e=t||window.event;if(9===e.keyCode){r.cancelEvent(e);const o=E(e);if(!o.el)return;let i=o.el.parentNode;const s=x(o.el),n=x(i),c=i.parentNode.childNodes.length,d=i.childNodes.length,a=e.shiftKey;let f,u=!1,h=s;for(;;){for(;a?h>=0:h<d;h=a?h-1:h+1){f=h!==s||u?a?c-1:0:a?n-1:n+1;for(;a?f>=0:f<c;f=a?f-1:f+1){if(h===s&&f===n)return;i=i.parentNode.childNodes[f];const p=i.childNodes[h].querySelectorAll(\"input, select, textarea, button\");if(p.length>0){setTimeout((function(){p.forEach((t=>t.dispatchEvent(new Event(\"focus\",{bubbles:!0}))));p.forEach((t=>t.dispatchEvent(new Event(\"select\",{bubbles:!0}))))}),0);return}}}h=a?d-1:0;u=!0}}else if(e.keyCode>=37&&e.keyCode<=40){const g=r.target(e);function l(t){return r.hasTag(t,\"INPUT\")&&\"text\"===t.type||r.hasTag(t,\"TEXTAREA\")}if(r.hasTag(g,\"SELECT\"))return;const m=E(e);if(!m.el)return;let T=m.el.parentNode,b=x(m.el),y=x(T);const N=T.parentNode.childNodes.length,w=T.childNodes.length;switch(e.keyCode){case 39:if(l(g)){if(r.getSelectionRange(g).start!==g.value.length)return}y++;break;case 38:b--;break;case 37:if(l(g)){if(0!==r.getSelectionRange(g).start)return}y--;break;case 40:b++;break;default:return}r.cancelEvent(e);if(b>-1&&b<w&&y>-1&&y<N){T=T.parentNode.childNodes[y];const v=T.childNodes[b].querySelectorAll(\"input, select, textarea, button\");if(v.length>0){setTimeout((function(){v.forEach((t=>t.dispatchEvent(new Event(\"focus\",{bubbles:!0}))))}),0);return}}}};this.autoJavaScript=function(){if(null===e.parentNode){e=l=i=null;this.autoJavaScript=function(){};return}if(r.isHidden(e))return;if(!r.isIE&&(y!==l.scrollTop||N!==l.scrollLeft)){void 0===N?c&&r.isGecko?i.scrollLeft=l.scrollLeft=N=0:N=l.scrollLeft:i.scrollLeft=l.scrollLeft=N;s.scrollTop=l.scrollTop=y}let o=e.offsetWidth-r.px(e,\"borderLeftWidth\")-r.px(e,\"borderRightWidth\");const n=l.offsetWidth-l.clientWidth;o-=s.clientWidth;if(o>200&&o!==l.tw){l.tw=o;l.style.width=o+\"px\";i.style.width=o-n+\"px\"}const c=document.body.classList.contains(\"Wt-rtl\");c?i.style.marginLeft=n+\"px\":i.style.marginRight=n+\"px\";const d=l.offsetHeight-l.clientHeight,a=s.style;if(a&&a.marginBottom!==d+\"px\"){a.marginBottom=d+\"px\";t.layouts2.adjust(e.childNodes[0].id,[[1,0]])}}})");
  }

  static int calcOverlap(int start1, int end1, int start2, int end2) {
    int s = Math.max(start1, start2);
    int e = Math.min(end1, end2);
    return Math.max(0, e - s);
  }
}
