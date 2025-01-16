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

class StdGridLayoutImpl2 extends StdLayoutImpl {
  private static Logger logger = LoggerFactory.getLogger(StdGridLayoutImpl2.class);

  public StdGridLayoutImpl2(WLayout layout, final Grid grid) {
    super(layout);
    this.grid_ = grid;
    this.needAdjust_ = false;
    this.needRemeasure_ = false;
    this.needConfigUpdate_ = false;
    this.addedItems_ = new ArrayList<WLayoutItem>();
    this.removedItems_ = new ArrayList<String>();
    String THIS_JS = "js/StdGridLayoutImpl2.js";
    WApplication app = WApplication.getInstance();
    if (!app.isJavaScriptLoaded(THIS_JS)) {
      app.getStyleSheet().addRule("table.Wt-hcenter", "margin: 0px auto;position: relative");
      app.loadJavaScript(THIS_JS, wtjs1());
      app.loadJavaScript(THIS_JS, appjs1());
      app.doJavaScript(app.getJavaScriptClass() + ".layouts2.scheduleAdjust();");
      app.doJavaScript(
          "(function(){var f=function(){"
              + app.getJavaScriptClass()
              + ".layouts2.scheduleAdjust();};window.addEventListener('load',f);})();");
      WApplication.getInstance()
          .addAutoJavaScript(
              "if("
                  + app.getJavaScriptClass()
                  + ".layouts2) "
                  + app.getJavaScriptClass()
                  + ".layouts2.adjustNow();");
    }
  }

  public int getMinimumWidth() {
    final int colCount = this.grid_.columns_.size();
    int total = 0;
    for (int i = 0; i < colCount; ++i) {
      total += this.minimumWidthForColumn(i);
    }
    return total + (colCount - 1) * this.grid_.horizontalSpacing_;
  }

  public int getMinimumHeight() {
    final int rowCount = this.grid_.rows_.size();
    int total = 0;
    for (int i = 0; i < rowCount; ++i) {
      total += this.minimumHeightForRow(i);
    }
    return total + (rowCount - 1) * this.grid_.verticalSpacing_;
  }

  public int getMaximumWidth() {
    final int colCount = this.grid_.columns_.size();
    int total = 0;
    for (int i = 0; i < colCount; ++i) {
      int colMax = this.maximumWidthForColumn(i);
      if (colMax == 0) {
        return 0;
      }
      total += colMax;
    }
    return total + (colCount - 1) * this.grid_.horizontalSpacing_;
  }

  public int getMaximumHeight() {
    final int rowCount = this.grid_.rows_.size();
    int total = 0;
    for (int i = 0; i < rowCount; ++i) {
      int rowMax = this.maximumHeightForRow(i);
      if (rowMax == 0) {
        return 0;
      }
      total += rowMax;
    }
    return total + (rowCount - 1) * this.grid_.verticalSpacing_;
  }

  public void itemAdded(WLayoutItem item) {
    this.addedItems_.add(item);
    this.update();
  }

  public void itemRemoved(WLayoutItem item) {
    this.addedItems_.remove(item);
    this.removedItems_.add(getImpl(item).getId());
    this.update();
  }

  public void updateDom(final DomElement parent) {
    WApplication app = WApplication.getInstance();
    if (this.needConfigUpdate_) {
      this.needConfigUpdate_ = false;
      DomElement div = DomElement.getForUpdate(this, DomElementType.DIV);
      for (int i = 0; i < this.addedItems_.size(); ++i) {
        WLayoutItem item = this.addedItems_.get(i);
        DomElement c = this.createElement(item, app);
        div.addChild(c);
      }
      this.addedItems_.clear();
      for (int i = 0; i < this.removedItems_.size(); ++i) {
        parent.callJavaScript("Wt4.11.2.remove('" + this.removedItems_.get(i) + "');", true);
      }
      this.removedItems_.clear();
      parent.addChild(div);
      StringBuilder js = new StringBuilder();
      js.append(app.getJavaScriptClass())
          .append(".layouts2.updateConfig('")
          .append(this.getId())
          .append("',");
      this.streamConfig(js, app);
      js.append(");");
      app.doJavaScript(js.toString());
      this.needRemeasure_ = false;
      this.needAdjust_ = false;
    }
    if (this.needRemeasure_) {
      this.needRemeasure_ = false;
      StringBuilder js = new StringBuilder();
      js.append(app.getJavaScriptClass())
          .append(".layouts2.setDirty('")
          .append(this.getId())
          .append("');");
      app.doJavaScript(js.toString());
    }
    if (this.needAdjust_) {
      this.needAdjust_ = false;
      StringBuilder js = new StringBuilder();
      js.append(app.getJavaScriptClass())
          .append(".layouts2.adjust('")
          .append(this.getId())
          .append("', [");
      boolean first = true;
      final int colCount = this.grid_.columns_.size();
      final int rowCount = this.grid_.rows_.size();
      for (int row = 0; row < rowCount; ++row) {
        for (int col = 0; col < colCount; ++col) {
          if (this.grid_.items_.get(row).get(col).update_) {
            this.grid_.items_.get(row).get(col).update_ = false;
            if (!first) {
              js.append(",");
            }
            first = false;
            js.append("[").append((int) row).append(",").append((int) col).append("]");
          }
        }
      }
      js.append("]);");
      app.doJavaScript(js.toString());
    }
    final int colCount = this.grid_.columns_.size();
    final int rowCount = this.grid_.rows_.size();
    for (int i = 0; i < rowCount; ++i) {
      for (int j = 0; j < colCount; ++j) {
        WLayoutItem item = this.grid_.items_.get(i).get(j).item_;
        if (item != null) {
          WLayout nested = item.getLayout();
          if (nested != null) {
            (ObjectUtils.cast(nested.getImpl(), StdLayoutImpl.class)).updateDom(parent);
          }
        }
      }
    }
  }

  public void update() {
    WContainerWidget c = this.getContainer();
    if (c != null) {
      c.layoutChanged(false);
    }
    this.needConfigUpdate_ = true;
  }

  public DomElement createDomElement(
      DomElement parent, boolean fitWidth, boolean fitHeight, WApplication app) {
    this.needAdjust_ = this.needConfigUpdate_ = this.needRemeasure_ = false;
    this.addedItems_.clear();
    this.removedItems_.clear();
    final int colCount = this.grid_.columns_.size();
    final int rowCount = this.grid_.rows_.size();
    int[] margin = {0, 0, 0, 0};
    int maxWidth = 0;
    int maxHeight = 0;
    if (this.getLayout().getParentLayout() == null) {
      if (this.getContainer() == app.getRoot()) {
        app.setBodyClass(app.getBodyClass() + " Wt-layout");
        app.setHtmlClass(app.getHtmlClass() + " Wt-layout");
      }
      margin[3] = this.getLayout().getContentsMargin(Side.Left);
      margin[0] = this.getLayout().getContentsMargin(Side.Top);
      margin[1] = this.getLayout().getContentsMargin(Side.Right);
      margin[2] = this.getLayout().getContentsMargin(Side.Bottom);
      maxWidth = pixelSize(this.getContainer().getMaximumWidth());
      maxHeight = pixelSize(this.getContainer().getMaximumHeight());
    }
    StringBuilder js = new StringBuilder();
    js.append(app.getJavaScriptClass())
        .append(".layouts2.add(new Wt4.11.2.StdLayout2(")
        .append(app.getJavaScriptClass())
        .append(",'")
        .append(this.getId())
        .append("',");
    if (this.getLayout().getParentLayout() != null
        && ObjectUtils.cast(getImpl(this.getLayout().getParentLayout()), StdGridLayoutImpl2.class)
            != null) {
      js.append("'").append(getImpl(this.getLayout().getParentLayout()).getId()).append("',");
    } else {
      js.append("null,");
    }
    boolean progressive = !app.getEnvironment().hasAjax();
    js.append(fitWidth ? '1' : '0')
        .append(",")
        .append(fitHeight ? '1' : '0')
        .append(",")
        .append(progressive ? '1' : '0')
        .append(",");
    js.append(maxWidth)
        .append(",")
        .append(maxHeight)
        .append(",[")
        .append(this.grid_.horizontalSpacing_)
        .append(",")
        .append(margin[3])
        .append(",")
        .append(margin[1])
        .append("],[")
        .append(this.grid_.verticalSpacing_)
        .append(",")
        .append(margin[0])
        .append(",")
        .append(margin[2])
        .append("],");
    this.streamConfig(js, app);
    DomElement div = DomElement.createNew(DomElementType.DIV);
    div.setId(this.getId());
    div.setProperty(Property.StylePosition, "relative");
    DomElement table = null;
    DomElement tbody = null;
    DomElement tr = null;
    if (progressive) {
      table = DomElement.createNew(DomElementType.TABLE);
      StringBuilder style = new StringBuilder();
      if (maxWidth != 0) {
        style.append("max-width: ").append(maxWidth).append("px;");
      }
      if (maxHeight != 0) {
        style.append("max-height: ").append(maxHeight).append("px;");
      }
      style.append("width: 100%;");
      table.setProperty(Property.Style, style.toString());
      int totalColStretch = 0;
      for (int col = 0; col < colCount; ++col) {
        totalColStretch += Math.max(0, this.grid_.columns_.get(col).stretch_);
      }
      for (int col = 0; col < colCount; ++col) {
        DomElement c = DomElement.createNew(DomElementType.COL);
        int stretch = Math.max(0, this.grid_.columns_.get(col).stretch_);
        if (stretch != 0 || totalColStretch == 0) {
          char[] buf = new char[30];
          double pct = totalColStretch == 0 ? 100.0 / colCount : 100.0 * stretch / totalColStretch;
          StringBuilder ss = new StringBuilder();
          ss.append("width:").append(MathUtils.roundCss(pct, 2)).append("%;");
          c.setProperty(Property.Style, ss.toString());
        }
        table.addChild(c);
      }
      tbody = DomElement.createNew(DomElementType.TBODY);
    }
    List<Boolean> overSpanned = new ArrayList<Boolean>();
    {
      int insertPos = 0;
      for (int ii = 0; ii < (colCount * rowCount); ++ii) overSpanned.add(insertPos + ii, false);
    }
    ;
    int prevRowWithItem = -1;
    for (int row = 0; row < rowCount; ++row) {
      if (table != null) {
        tr = DomElement.createNew(DomElementType.TR);
      }
      boolean rowVisible = false;
      int prevColumnWithItem = -1;
      for (int col = 0; col < colCount; ++col) {
        final Grid.Item item = this.grid_.items_.get(row).get(col);
        if (!overSpanned.get(row * colCount + col)) {
          for (int i = 0; i < item.rowSpan_; ++i) {
            for (int j = 0; j < item.colSpan_; ++j) {
              if (i + j > 0) {
                overSpanned.set((row + i) * colCount + col + j, true);
              }
            }
          }
          AlignmentFlag hAlign =
              EnumUtils.enumFromSet(
                  EnumUtils.mask(item.alignment_, AlignmentFlag.AlignHorizontalMask));
          AlignmentFlag vAlign =
              EnumUtils.enumFromSet(
                  EnumUtils.mask(item.alignment_, AlignmentFlag.AlignVerticalMask));
          DomElement td = null;
          if (table != null) {
            boolean itemVisible = this.hasItem(row, col);
            rowVisible = rowVisible || itemVisible;
            td = DomElement.createNew(DomElementType.TD);
            if (itemVisible) {
              int[] padding = {0, 0, 0, 0};
              int nextRow = this.nextRowWithItem(row, col);
              int prevRow = prevRowWithItem;
              int nextCol = this.nextColumnWithItem(row, col);
              int prevCol = prevColumnWithItem;
              if (prevRow == -1) {
                padding[0] = margin[0];
              } else {
                padding[0] = (this.grid_.verticalSpacing_ + 1) / 2;
              }
              if (nextRow == (int) rowCount) {
                padding[2] = margin[2];
              } else {
                padding[2] = this.grid_.verticalSpacing_ / 2;
              }
              if (prevCol == -1) {
                padding[3] = margin[3];
              } else {
                padding[3] = (this.grid_.horizontalSpacing_ + 1) / 2;
              }
              if (nextCol == (int) colCount) {
                padding[1] = margin[1];
              } else {
                padding[1] = this.grid_.horizontalSpacing_ / 2;
              }
              StringBuilder style = new StringBuilder();
              if (app.getLayoutDirection() == LayoutDirection.RightToLeft) {
                int tmp = padding[1];
                padding[1] = padding[3];
                padding[3] = tmp;
              }
              if (padding[0] == padding[1]
                  && padding[0] == padding[2]
                  && padding[0] == padding[3]) {
                if (padding[0] != 0) {
                  style.append("padding:").append(padding[0]).append("px;");
                }
              } else {
                style
                    .append("padding:")
                    .append(padding[0])
                    .append("px ")
                    .append(padding[1])
                    .append("px ")
                    .append(padding[2])
                    .append("px ")
                    .append(padding[3])
                    .append("px;");
              }
              if ((int) vAlign.getValue() != 0) {
                switch (vAlign) {
                  case Top:
                    style.append("vertical-align:top;");
                    break;
                  case Middle:
                    style.append("vertical-align:middle;");
                    break;
                  case Bottom:
                    style.append("vertical-align:bottom;");
                  default:
                    break;
                }
              }
              td.setProperty(Property.Style, style.toString());
              if (item.rowSpan_ != 1) {
                td.setProperty(Property.RowSpan, String.valueOf(item.rowSpan_));
              }
              if (item.colSpan_ != 1) {
                td.setProperty(Property.ColSpan, String.valueOf(item.colSpan_));
              }
              prevColumnWithItem = col;
            }
          }
          DomElement c = null;
          if (!(table != null)) {
            if (item.item_ != null) {
              c = this.createElement(item.item_, app);
              div.addChild(c);
            }
          } else {
            if (item.item_ != null) {
              c = getImpl(item.item_).createDomElement((DomElement) null, true, true, app);
            }
          }
          if (table != null) {
            if (c != null) {
              if (!app.getEnvironment().agentIsIElt(9)) {
                c.setProperty(Property.StyleBoxSizing, "border-box");
              }
              if ((int) hAlign.getValue() == 0) {
                hAlign = AlignmentFlag.Justify;
              }
              switch (hAlign) {
                case Center:
                  {
                    DomElement itable = DomElement.createNew(DomElementType.TABLE);
                    itable.setProperty(Property.Class, "Wt-hcenter");
                    if ((int) vAlign.getValue() == 0) {
                      itable.setProperty(Property.Style, "height:100%;");
                    }
                    DomElement irow = DomElement.createNew(DomElementType.TR);
                    DomElement itd = DomElement.createNew(DomElementType.TD);
                    if ((int) vAlign.getValue() == 0) {
                      itd.setProperty(Property.Style, "height:100%;");
                    }
                    boolean haveMinWidth = c.getProperty(Property.StyleMinWidth).length() != 0;
                    itd.addChild(c);
                    if (app.getEnvironment().agentIsIElt(9)) {
                      if (haveMinWidth) {
                        DomElement spacer = DomElement.createNew(DomElementType.DIV);
                        spacer.setProperty(
                            Property.StyleWidth, c.getProperty(Property.StyleMinWidth));
                        spacer.setProperty(Property.StyleHeight, "1px");
                        itd.addChild(spacer);
                      }
                    }
                    irow.addChild(itd);
                    itable.addChild(irow);
                    c = itable;
                    break;
                  }
                case Right:
                  if (!c.isDefaultInline()) {
                    c.setProperty(Property.StyleFloat, "right");
                  } else {
                    td.setProperty(Property.StyleTextAlign, "right");
                  }
                  break;
                case Left:
                  if (!c.isDefaultInline()) {
                    c.setProperty(Property.StyleFloat, "left");
                  } else {
                    td.setProperty(Property.StyleTextAlign, "left");
                  }
                  break;
                default:
                  break;
              }
              boolean haveMinWidth = c.getProperty(Property.StyleMinWidth).length() != 0;
              td.addChild(c);
              if (app.getEnvironment().agentIsIElt(9)) {
                if (haveMinWidth) {
                  DomElement spacer = DomElement.createNew(DomElementType.DIV);
                  spacer.setProperty(Property.StyleWidth, c.getProperty(Property.StyleMinWidth));
                  spacer.setProperty(Property.StyleHeight, "1px");
                  td.addChild(spacer);
                }
              }
            }
            tr.addChild(td);
          }
        }
      }
      if (tr != null) {
        if (!rowVisible) {
          tr.setProperty(Property.StyleDisplay, "hidden");
        } else {
          prevRowWithItem = row;
        }
        tbody.addChild(tr);
      }
    }
    js.append("));");
    if (table != null) {
      table.addChild(tbody);
      div.addChild(table);
    }
    div.callJavaScript(js.toString());
    if (this.getLayout().getParentLayout() == null) {
      WContainerWidget c = this.getContainer();
      if (c.getPositionScheme() == PositionScheme.Relative
          || c.getPositionScheme() == PositionScheme.Absolute) {
        div.setProperty(Property.StylePosition, "absolute");
        div.setProperty(Property.StyleLeft, "0");
        div.setProperty(Property.StyleRight, "0");
      } else {
        if (app.getEnvironment().agentIsIE()) {
          if (app.getEnvironment().agentIsIE()
              && c.getParent().getPositionScheme() != PositionScheme.Static) {
            parent.setProperty(Property.StylePosition, "relative");
          }
        }
      }
      AlignmentFlag hAlign =
          EnumUtils.enumFromSet(
              EnumUtils.mask(c.getContentAlignment(), AlignmentFlag.AlignHorizontalMask));
      switch (hAlign) {
        case Center:
          {
            DomElement itable = DomElement.createNew(DomElementType.TABLE);
            itable.setProperty(Property.Class, "Wt-hcenter");
            if (fitHeight) {
              itable.setProperty(Property.Style, "height:100%;");
            }
            DomElement irow = DomElement.createNew(DomElementType.TR);
            DomElement itd = DomElement.createNew(DomElementType.TD);
            if (fitHeight) {
              itd.setProperty(Property.Style, "height:100%;");
            }
            itd.addChild(div);
            irow.addChild(itd);
            itable.addChild(irow);
            itable.setId(this.getId() + "l");
            div = itable;
            break;
          }
        case Left:
          break;
        case Right:
          div.setProperty(Property.StyleFloat, "right");
          break;
        default:
          break;
      }
    }
    return div;
  }

  public boolean itemResized(WLayoutItem item) {
    final int colCount = this.grid_.columns_.size();
    final int rowCount = this.grid_.rows_.size();
    for (int row = 0; row < rowCount; ++row) {
      for (int col = 0; col < colCount; ++col) {
        if (this.grid_.items_.get(row).get(col).item_ == item
            && !this.grid_.items_.get(row).get(col).update_) {
          this.grid_.items_.get(row).get(col).update_ = true;
          this.needAdjust_ = true;
          return true;
        }
      }
    }
    return false;
  }

  public boolean isParentResized() {
    if (!this.needRemeasure_) {
      this.needRemeasure_ = true;
      return true;
    } else {
      return false;
    }
  }

  private final Grid grid_;
  private boolean needAdjust_;
  private boolean needRemeasure_;
  private boolean needConfigUpdate_;
  private List<WLayoutItem> addedItems_;
  private List<String> removedItems_;

  private int nextRowWithItem(int row, int c) {
    for (row += this.grid_.items_.get(row).get(c).rowSpan_;
        row < (int) this.grid_.rows_.size();
        ++row) {
      for (int col = 0;
          col < this.grid_.columns_.size();
          col += this.grid_.items_.get(row).get(col).colSpan_) {
        if (this.hasItem(row, col)) {
          return row;
        }
      }
    }
    return this.grid_.rows_.size();
  }

  private int nextColumnWithItem(int row, int col) {
    for (; ; ) {
      col = col + this.grid_.items_.get(row).get(col).colSpan_;
      if (col < (int) this.grid_.columns_.size()) {
        for (int i = 0; i < this.grid_.rows_.size(); ++i) {
          if (this.hasItem(i, col)) {
            return col;
          }
        }
      } else {
        return this.grid_.columns_.size();
      }
    }
  }

  private boolean hasItem(int row, int col) {
    WLayoutItem item = this.grid_.items_.get(row).get(col).item_;
    if (item != null) {
      WWidget w = item.getWidget();
      return !(w != null) || !w.isHidden();
    } else {
      return false;
    }
  }

  private int minimumHeightForRow(int row) {
    int minHeight = 0;
    final int colCount = this.grid_.columns_.size();
    for (int j = 0; j < colCount; ++j) {
      WLayoutItem item = this.grid_.items_.get(row).get(j).item_;
      if (item != null) {
        minHeight = Math.max(minHeight, getImpl(item).getMinimumHeight());
      }
    }
    return minHeight;
  }

  private int minimumWidthForColumn(int col) {
    int minWidth = 0;
    final int rowCount = this.grid_.rows_.size();
    for (int i = 0; i < rowCount; ++i) {
      WLayoutItem item = this.grid_.items_.get(i).get(col).item_;
      if (item != null) {
        minWidth = Math.max(minWidth, getImpl(item).getMinimumWidth());
      }
    }
    return minWidth;
  }

  private int maximumHeightForRow(int row) {
    int maxHeight = Integer.MAX_VALUE;
    boolean isConstrained = false;
    final int colCount = this.grid_.columns_.size();
    for (int j = 0; j < colCount; ++j) {
      WLayoutItem item = this.grid_.items_.get(row).get(j).item_;
      if (item != null) {
        int itemMaxHeight = getImpl(item).getMaximumHeight();
        if (itemMaxHeight > 0) {
          if (isConstrained) {
            maxHeight = Math.min(maxHeight, itemMaxHeight);
          } else {
            maxHeight = itemMaxHeight;
            isConstrained = true;
          }
        }
      }
    }
    if (!isConstrained) {
      maxHeight = 0;
    }
    return maxHeight;
  }

  private int maximumWidthForColumn(int col) {
    int maxWidth = Integer.MAX_VALUE;
    boolean isConstrained = false;
    final int rowCount = this.grid_.rows_.size();
    for (int i = 0; i < rowCount; ++i) {
      WLayoutItem item = this.grid_.items_.get(i).get(col).item_;
      if (item != null) {
        int itemMaxWidth = getImpl(item).getMaximumWidth();
        if (itemMaxWidth > 0) {
          if (isConstrained) {
            maxWidth = Math.min(maxWidth, itemMaxWidth);
          } else {
            maxWidth = itemMaxWidth;
            isConstrained = true;
          }
        }
      }
    }
    if (!isConstrained) {
      maxWidth = 0;
    }
    return maxWidth;
  }

  private static int pixelSize(final WLength size) {
    if (size.getUnit() == LengthUnit.Percentage) {
      return 0;
    } else {
      return (int) size.toPixels();
    }
  }

  private void streamConfig(
      final StringBuilder js, final List<Grid.Section> sections, boolean rows, WApplication app) {
    js.append("[");
    for (int i = 0; i < sections.size(); ++i) {
      if (i != 0) {
        js.append(",");
      }
      js.append("[").append(sections.get(i).stretch_).append(",");
      if (sections.get(i).resizable_) {
        SizeHandle.loadJavaScript(app);
        js.append("[");
        final WLength size = sections.get(i).initialSize_;
        if (size.isAuto()) {
          js.append("-1");
        } else {
          if (size.getUnit() == LengthUnit.Percentage) {
            js.append(size.getValue()).append(",1");
          } else {
            js.append(size.toPixels());
          }
        }
        js.append("],");
      } else {
        js.append("0,");
      }
      if (rows) {
        js.append(this.minimumHeightForRow(i)).append(",").append(this.maximumHeightForRow(i));
      } else {
        js.append(this.minimumWidthForColumn(i)).append(",").append(this.maximumWidthForColumn(i));
      }
      js.append("]");
    }
    js.append("]");
  }

  private void streamConfig(final StringBuilder js, WApplication app) {
    js.append("{ rows:");
    this.streamConfig(js, this.grid_.rows_, true, app);
    js.append(", cols:");
    this.streamConfig(js, this.grid_.columns_, false, app);
    js.append(", items: [");
    final int colCount = this.grid_.columns_.size();
    final int rowCount = this.grid_.rows_.size();
    for (int row = 0; row < rowCount; ++row) {
      for (int col = 0; col < colCount; ++col) {
        final Grid.Item item = this.grid_.items_.get(row).get(col);
        AlignmentFlag hAlign =
            EnumUtils.enumFromSet(
                EnumUtils.mask(item.alignment_, AlignmentFlag.AlignHorizontalMask));
        AlignmentFlag vAlign =
            EnumUtils.enumFromSet(EnumUtils.mask(item.alignment_, AlignmentFlag.AlignVerticalMask));
        if (row + col != 0) {
          js.append(",");
        }
        if (item.item_ != null) {
          String id = getImpl(item.item_).getId();
          js.append("{");
          if (item.colSpan_ != 1 || item.rowSpan_ != 1) {
            js.append("span: [")
                .append(item.colSpan_)
                .append(",")
                .append(item.rowSpan_)
                .append("],");
          }
          if (EnumUtils.valueOf(item.alignment_) != 0) {
            int align = 0;
            if (hAlign != null) {
              switch (hAlign) {
                case Left:
                  align |= 0x1;
                  break;
                case Right:
                  align |= 0x2;
                  break;
                case Center:
                  align |= 0x4;
                  break;
                default:
                  break;
              }
            }
            if (vAlign != null) {
              switch (vAlign) {
                case Top:
                  align |= 0x10;
                  break;
                case Bottom:
                  align |= 0x20;
                  break;
                case Middle:
                  align |= 0x40;
                  break;
                default:
                  break;
              }
            }
            js.append("align:").append((int) align).append(",");
          }
          js.append("dirty:")
              .append(this.grid_.items_.get(row).get(col).update_ ? 2 : 0)
              .append(",id:'")
              .append(id)
              .append("'")
              .append("}");
          this.grid_.items_.get(row).get(col).update_ = 0 != 0;
        } else {
          js.append("null");
        }
      }
    }
    js.append("]}");
  }

  private DomElement createElement(WLayoutItem item, WApplication app) {
    DomElement c = getImpl(item).createDomElement((DomElement) null, true, true, app);
    c.setProperty(Property.StyleVisibility, "hidden");
    return c;
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "StdLayout2",
        "(function(e,t,i,s,n,o,f,l,r,c,a){const d=e.WT;this.descendants=[];const u=0,g=1,h=3,p=1,m=0,z=1e6,x=\"-\"+z+\"px\",y=this;let S,w,L,W,b,M,R=a,E=!0,H=!0,N=!1;const C=document.body.classList.contains(\"Wt-rtl\"),v=[{initialized:!1,config:R.cols,margins:r,maxSize:f,measures:[],sizes:[],stretched:[],fixedSize:[],Left:C?\"Right\":\"Left\",left:C?\"right\":\"left\",Right:C?\"Left\":\"Right\",Size:\"Width\",size:\"width\",alignBits:0,getItem:function(e,t){return R.items[t*v[m].config.length+e]},setItem:function(e,t,i){R.items[t*v[m].config.length+e]=i},handleClass:\"Wt-vrh2\",resizeDir:\"h\",resizerClass:\"Wt-hsh2\",fitSize:s,resizeHandles:[]},{initialized:!1,config:R.rows,margins:c,maxSize:l,measures:[],sizes:[],stretched:[],fixedSize:[],Left:\"Top\",left:\"top\",Right:\"Bottom\",Size:\"Height\",size:\"height\",alignBits:4,getItem:function(e,t){return R.items[e*v[m].config.length+t]},setItem:function(e,t,i){R.items[e*v[m].config.length+t]=i},handleClass:\"Wt-hrh2\",resizeDir:\"v\",resizerClass:\"Wt-vsh2\",fitSize:n,resizeHandles:[]}];document.getElementById(t).wtLayout=this;function D(e){for(const t of R.items)if(t&&t.id===e)return t;return null}function I(e,t,i,s){const n=v[t];let o,f,l=t?e.scrollHeight:e.scrollWidth;if(t===m){const i=d.pxself(e,n.left);if(l+i>s.clientWidth||l+i===s.clientWidth&&d.isGecko&&\"hidden\"===s.parentNode.parentNode.style.visibility){o=e.style[n.left];k(e,n.left,x);l=t?e.scrollHeight:e.scrollWidth}}let r=t?e.clientHeight:e.clientWidth;if(d.isGecko&&!e.style[n.size]&&t===m&&(\"visible\"===(c=d.css(e,\"overflow\"))||\"none\"===c)){f=e.style[n.size];k(e,n.size,\"\")}var c;let a=t?e.offsetHeight:e.offsetWidth;o&&k(e,n.left,o);f&&k(e,n.size,f);r>=z&&(r-=z);l>=z&&(l-=z);a>=z&&(a-=z);if(0===l){l=d.pxself(e,n.size);0===l||d.isOpera||d.isGecko||(l-=d.px(e,\"border\"+n.Left+\"Width\")+d.px(e,\"border\"+n.Right+\"Width\"))}if(l>a)if(0===d.pxself(e,n.size))l=r;else{let t=!1;e.querySelectorAll(\".Wt-popup\").forEach((function(e){\"none\"!==e.style.display&&(t=!0)}));t&&(l=r)}const u=d.px(e,\"border\"+n.Left+\"Width\")+d.px(e,\"border\"+n.Right+\"Width\"),g=a-(r+u)!=0;if(i)return[l,g];(d.isGecko||d.isWebKit)&&t===m&&e.getBoundingClientRect().width!==Math.ceil(e.getBoundingClientRect().width)&&(l+=1);d.boxSizing(e)||d.isOpera||(l+=u);l+=d.px(e,\"margin\"+n.Left)+d.px(e,\"margin\"+n.Right);d.boxSizing(e)||(l+=d.px(e,\"padding\"+n.Left)+d.px(e,\"padding\"+n.Right));l+=a-(r+u);l<a&&(l=a);const h=d.px(e,\"max\"+n.Size);h>0&&(l=Math.min(h,l));return[Math.round(l),g]}function A(e,t){const i=v[t];if(\"none\"===e.style.display)return 0;if(e[\"layoutMin\"+i.Size])return e[\"layoutMin\"+i.Size];{let t=d.px(e,\"min\"+i.Size);d.boxSizing(e)||(t+=d.px(e,\"padding\"+i.Left)+d.px(e,\"padding\"+i.Right));return t}}function T(e,t){const i=v[t];let s=d.px(e,\"margin\"+i.Left)+d.px(e,\"margin\"+i.Right);d.boxSizing(e)||(s+=d.px(e,\"border\"+i.Left+\"Width\")+d.px(e,\"border\"+i.Right+\"Width\")+d.px(e,\"padding\"+i.Left)+d.px(e,\"padding\"+i.Right));return s}function j(e,t){const i=v[t];return d.px(e,\"padding\"+i.Left)+d.px(e,\"padding\"+i.Right)}function B(e,t){if(d.boxSizing(e)){const i=v[t];return d.px(e,\"border\"+i.Left+\"Width\")+d.px(e,\"border\"+i.Right+\"Width\")+d.px(e,\"padding\"+i.Left)+d.px(e,\"padding\"+i.Right)}return 0}function P(e,t){const i=v[t];return Math.round(d.px(e,\"border\"+i.Left+\"Width\")+d.px(e,\"border\"+i.Right+\"Width\")+d.px(e,\"margin\"+i.Left)+d.px(e,\"margin\"+i.Right)+d.px(e,\"padding\"+i.Left)+d.px(e,\"padding\"+i.Right))}function G(t,i,s){t.dirty=Math.max(t.dirty,i);E=!0;s&&e.layouts2.scheduleAdjust()}function k(e,t,i){if(e.style[t]!==i){e.style[t]=i;return!0}return!1}function _(e){return\"none\"===e.style.display&&!e.ed||e.classList.contains(\"Wt-hidden\")}this.updateSizeInParent=function(e){if(w&&L.id){const e=d.$(L.id);if(e){if(L!==e){w=e.parentNode.wtLayout;w?L=e:J()}}else J()}if(w&&W){const i=v[e];let s=i.measures[2];i.maxSize>0&&(s=Math.min(i.maxSize,s));if(M){const i=d.getElement(t);if(!i)return;let n=i,o=n.parentNode;for(;;){o.wtGetPS&&(s=o.wtGetPS(o,n,e,s));s+=P(o,e);if(o===L)break;1===e&&o===i.parentNode&&!o.lh&&o.offsetHeight>s&&(s=o.offsetHeight);n=o;o=n.parentNode}}else s+=b[e];w.setChildSize(L,e,s)}};function q(e,t){const i=v[e];let s,n=!0,o=0,f=0;for(s=0;s<i.sizes.length;++s)if(s!==t)if(i.config[s][u]>0){o+=i.sizes[s]-i.measures[p][s];n=!1}else f+=i.sizes[s]-i.measures[p][s];n&&(o=f);const l=i.config[t][h];l>0&&(o=Math.min(o,l-i.sizes[t]));return o}function O(i,s,n){const o=s.di,f=v[i],l=v[1^i],r=d.getElement(t);let c,a,g;for(g=o-1;g>=0;--g)if(f.sizes[g]>=0){c=f.config[g][u]>0&&0===f.config[g+1][u]?-q(i,g+1):-(f.sizes[g]-f.measures[p][g]);break}a=f.config[g][u]>0&&0===f.config[g+1][u]?f.sizes[o]-f.measures[p][o]:q(i,g);C&&([c,a]=[-a,-c]);new d.SizeHandle(d,f.resizeDir,d.pxself(s,f.size),d.pxself(s,l.size),c,a,f.resizerClass,(function(t){!function(t,i,s){const n=v[t];C&&(s=-s);if(n.config[i][u]>0&&0===n.config[i+1][u]){++i;s=-s}n.fixedSize[i]=n.sizes[i]+s;e.layouts2.scheduleAdjust()}(i,g,t)}),s,r,n,0,0)}function $(e,t){const i=e.config.length;if(0!==e.config[t][g])for(let s=t+1;s<i;++s)if(e.measures[p][s]>-1)return!0;for(let i=t-1;i>=0;--i)if(e.measures[p][i]>-1)return 0!==e.config[i][g];return!1}this.setConfig=function(t){const i=R;R=t;v[0].config=R.cols;v[1].config=R.rows;v[0].stretched=[];v[1].stretched=[];for(const e of i.items)if(e){const t=D(e.id);if(t){t.ps=e.ps;t.sc=e.sc;t.ms=e.ms;t.size=e.size;t.psize=e.psize;t.fs=e.fs;t.margin=e.margin;t.set=e.set}else if(e.set){e.set[m]&&k(e.w,v[m].size,\"\");e.set[1]&&k(e.w,v[1].size,\"\")}}H=!0;E=!0;e.layouts2.scheduleAdjust()};this.getId=function(){return t};this.setElDirty=function(t){const i=D(t.id);if(i){i.dirty=2;E=!0;e.layouts2.scheduleAdjust()}};this.setItemsDirty=function(t){const i=v[m].config.length;for(const[s,n]of t){const t=R.items[s*i+n];if(t){t.dirty=2;if(t.layout){t.layout=!1;t.wasLayout=!0;e.layouts2.setChildLayoutsDirty(y,t.w)}}}E=!0};this.setDirty=function(){H=!0};this.setAllDirty=function(){for(const e of R.items)e&&(e.dirty=2);E=!0};this.setChildSize=function(e,t,i){const s=v[m].config.length,n=v[t],o=D(e.id);if(o){const f=function(e){let t=0;for(const i of R.items){if(i&&i.id===e)return t;t+=1}return null}(e.id),l=t===m?f%s:f/s;if(o.align>>n.alignBits&15||!n.stretched[l]){o.ps||(o.ps=[]);o.ps[t]=i}o.layout=!0;G(o,1)}};function J(){const e=d.getElement(t);S=null===i;w=null;L=null;W=!0;N=!0;b=[];M=!1;if(S){let t=e,i=t.parentNode;b=[0,0];for(;i!==document&&!t.classList.contains(\"wt-reparented\");){b[m]+=P(i,m);b[1]+=P(i,1);i.wtGetPS&&(M=!0);const e=i.parentNode.wtLayout;if(e){L=i;w=e;break}t=i;i=t.parentNode;1===i.childNodes.length||i.wtGetPS||(W=!1)}const s=e.parentNode;for(let e=0;e<2;++e)v[e].sizeSet=0!==d.pxself(s,v[e].size)}else{w=document.getElementById(i).wtLayout;L=e;b[m]=P(L,m);b[1]=P(L,1)}}this.measure=function(e){const i=d.getElement(t);if(i&&!d.isHidden(i)){N||J();if(E||H){!function(e,t,i){const s=v[e],n=v[1^e],f=s.measures,l=s.config.length,r=n.config.length,c=f.slice();if(5===c.length){c[0]=c[0].slice();c[p]=c[p].slice()}if(E){if(i&&void 0===s.minSize){s.minSize=d.px(i,\"min\"+s.Size);s.minSize>0&&(s.minSize-=B(i,e))}const h=[],z=[],x=!0;let S=!1;for(let R=0;R<l;++R){let N=0,C=s.config[R][2],D=!0;for(let j=0;j<r;++j){const P=s.getItem(R,j);if(P){if(!P.w||e===m&&P.dirty>1){const O=d.$(P.id);if(!O){s.setItem(R,j,null);continue}if(O!==P.w){P.w=O;const $=P;[O,...O.querySelectorAll(\"img\")].filter((e=>\"IMG\"===e.tagName)).forEach((function(e){e.addEventListener(\"load\",(function(){G($,1,!0)}))}))}}if(!o&&\"absolute\"!==P.w.style.position){P.w.style.position=\"absolute\";P.w.style.visibility=\"hidden\"}P.ps||(P.ps=[]);P.sc||(P.sc=[]);P.ms||(P.ms=[]);P.size||(P.size=[]);P.psize||(P.psize=[]);P.fs||(P.fs=[]);P.margin||(P.margin=[]);const q=!P.set;P.set||(P.set=[!1,!1]);if(_(P.w)){P.ps[e]=P.ms[e]=0;continue}if(P.w){if(P.dirty){let J;if(P.dirty>1){J=A(P.w,e);P.ms[e]=J}else J=P.ms[e];P.dirty>1&&(P.margin[e]=T(P.w,e));if(!P.set[e])if(e!==m&&q){const Q=Math.round(d.px(P.w,s.size));Q>Math.max(B(P.w,e),J)?P.fs[e]=Q+P.margin[e]:P.fs[e]=0}else{const U=d.pxself(P.w,s.size);P.fs[e]=U?U+P.margin[e]:0}const K=P.align>>s.alignBits&15;let F=P.fs[e];if(K||x||s.config[R][u]<=0)if(P.layout){0===F&&(F=P.ps[e]);P.ps[e]=F}else{if(P.wasLayout){P.wasLayout=!1;P.set=[!1,!1];P.ps=[];P.w.wtResize&&P.w.wtResize(P.w,-1,-1,!0);k(P.w,v[1].size,\"\")}const V=I(P.w,e,!1,t),X=V[0];let Y=P.set[e];Y&&P.psize[e]>8&&(Y=X>=P.psize[e]-4&&X<=P.psize[e]+4);const Z=void 0!==P.ps[e]&&s.config[R][u]>0&&P.set[e];F=Y||Z?Math.max(F,P.ps[e]):Math.max(F,X);P.ps[e]=F;P.sc[e]=V[1]}else P.layout&&0===F&&(F=P.ps[e]);if(P.span&&1!==P.span[e])S=!0;else{F>N&&(N=F);J>C&&(C=J)}}else if(P.span&&1!==P.span[e])S=!0;else{P.ps[e]>N&&(N=P.ps[e]);P.ms[e]>C&&(C=P.ms[e])}_(P.w)||P.span&&1!==P.span[e]||(D=!1)}}}D?C=N=-1:C>N&&(N=C);h[R]=N;z[R]=C}if(S){function a(t,i){for(let n=l-1;n>=0;--n)for(let o=0;o<r;++o){const f=s.getItem(n,o);if(f&&f.span&&f.span[e]>1){let o,l=t(f),r=0,c=0;for(o=0;o<f.span[e];++o){const e=i[n+o];if(-1!==e){l-=e;++r;s.config[n+o][u]>0&&(c+=s.config[n+o][u]);0!==o&&(l-=s.margins[0])}}if(l>=0)if(r>0){c>0&&(r=c);for(o=0;o<f.span[e];++o)if(-1!==i[n+o]){let e;e=c>0?s.config[n+o][u]:1;if(e>0){const t=Math.round(l*(e/r));l-=t;r-=e;i[n+o]+=t}}}else i[n]=l}}}a((function(t){return t.ps[e]}),h);a((function(t){return t.ms[e]}),z)}let w=0,L=0;for(let ee=0;ee<l;++ee){z[ee]>h[ee]&&(h[ee]=z[ee]);if(z[ee]>-1){w+=h[ee];L+=z[ee]}}let W=0,b=!0,M=!1;for(let te=0;te<l;++te)if(z[te]>-1){if(b){W+=s.margins[1];b=!1}else{W+=s.margins[0];M&&(W+=10)}M=0!==s.config[te][g]}b||(W+=s.margins[2]);w+=W;L+=W;s.measures=[h,z,w,L,W]}(H||c[2]!==s.measures[2])&&y.updateSizeInParent(e);if(i&&0===s.minSize&&c[3]!==s.measures[3]&&\"Wt-domRoot\"!==i.parentNode.className){const ie=s.measures[3]+\"px\";k(i,\"min\"+s.Size,ie)}i&&e===m&&i&&d.hasTag(i,\"TD\")&&k(i,s.size,s.measures[2]+\"px\")}(e,i,S?i.parentNode:null)}1===e&&(E=H=!1)}};this.setMaxSize=function(e,t){v[m].maxSize=e;v[1].maxSize=t};this.apply=function(e){const i=d.getElement(t);if(!i)return!1;if(d.isHidden(i))return!0;!function(e,i){const s=v[e],n=v[1^e],f=s.measures;let l=0,r=!1,c=!1;const a=S?i.parentNode:null;if(0===s.maxSize)if(a){const t=d.css(a,\"position\");\"absolute\"===t&&(l=d.pxself(a,s.size));if(0===l){if(!s.initialized){if(e===m&&(\"absolute\"===t||\"fixed\"===t)){a.style.display=\"none\";l=a.clientWidth;a.style.display=\"\"}l=e?a.clientHeight:a.clientWidth;r=!0;let i,n;if(d.hasTag(a,\"TD\")||d.hasTag(a,\"TH\")||a.parentNode.classList.contains(\"Wt-domRoot\")){i=0;n=1}else{i=s.minSize?s.minSize:f[3];n=0}l-(i+j(a,e))<=1&&(s.maxSize=999999)}if(0===l&&0===s.maxSize){l=e?a.clientHeight:a.clientWidth;r=!0}}}else{l=d.pxself(i,s.size);c=!0}else if(s.sizeSet){l=d.pxself(a,s.size);c=!0}let z=0;a&&a.wtGetPS&&1===e&&(z=a.wtGetPS(a,i,e,0));let x=f[2];x<s.minSize&&(x=s.minSize);if(s.maxSize&&!s.sizeSet){const t=Math.min(x,s.maxSize)+z;!a.parentNode.wtResize&&k(a,s.size,t+B(a,e)+\"px\")&&w&&w.setElDirty(L);l=t;c=!0}s.cSize=l;if(1===e&&a&&a.wtResize){const e=n.cSize,t=s.cSize;a.wtResize(a,Math.round(e),Math.round(t),!0)}l-=z;if(!c){let t=0;if(void 0===s.cPadding){t=r?j(a,e):B(a,e);s.cPadding=t}else t=s.cPadding;l-=t}s.initialized=!0;if(a&&l<=0)return;l<f[3]-z&&(l=f[3]-z);let y=[];const W=s.config.length,b=n.config.length;let M;for(M=0;M<W;++M)s.stretched[M]=!1;if(l>=f[3]-z){const e=-1,t=-2,i=0,n=1;let o=l-f[4];const r=[],c=[0,0],a=[0,0];let d=0;for(let h=0;h<W;++h)if(f[p][h]>-1){let t=-1;$(s,h)||delete s.fixedSize[h];if(void 0!==s.fixedSize[h]&&(h+1===W||f[p][h+1]>-1))t=s.fixedSize[h];else if($(s,h)&&0!==s.config[h][g]&&s.config[h][g][0]>=0){t=s.config[h][g][0];s.config[h][g][1]&&(t=(l-f[4])*t/100)}if(t>=0){r[h]=e;y[h]=t;o-=y[h]}else{let e;if(s.config[h][u]>0){e=n;r[h]=s.config[h][u];d+=r[h]}else{e=i;r[h]=0}c[e]+=f[p][h];a[e]+=f[0][h];y[h]=f[0][h]}}else{r[h]=t;y[h]=-1}s.fixedSize.length>W&&(s.fixedSize.length=W);if(0===d){for(let e=0;e<W;++e)if(0===r[e]){r[e]=1;++d}a[n]=a[i];c[n]=c[i];a[i]=0;c[i]=0}if(o>a[i]+c[n]){o-=a[i];if(o>a[n]){if(s.fitSize){o-=a[n];let e=!0;for(;e&&o>0;){const t=o/d;let i=0;for(let e=0;e<W;++e)if(r[e]>0){const n=i;i+=r[e]*t;const f=Math.round(i)-Math.round(n);y[e]+=f;o-=f;const l=s.config[e][h];if(l>0){const t=y[e];y[e]=Math.min(y[e],l);if(y[e]===l){o+=t-l;d-=r[e];r[e]=-1}}s.stretched[e]=!0}if(0===d)for(let e=0;e<W;++e)if(0===r[e]){r[e]=1;++d}e=d>0}}}else{const e=n;o<c[e]&&(o=c[e]);let t;t=a[e]-c[e]>0?(o-c[e])/(a[e]-c[e]):0;let i=0;for(let e=0;e<W;++e)if(r[e]>0){const s=i;i+=(f[0][e]-f[p][e])*t;y[e]=f[p][e]+Math.round(i)-Math.round(s)}}}else{for(let e=0;e<W;++e)r[e]>0&&(y[e]=f[p][e]);o-=c[n];const e=i;o<c[e]&&(o=c[e]);let t;t=a[e]-c[e]>0?(o-c[e])/(a[e]-c[e]):0;let s=0;for(let e=0;e<W;++e)if(0===r[e]){const i=s;s+=(f[0][e]-f[p][e])*t;y[e]=f[p][e]+Math.round(s)-Math.round(i)}}}else y=f[p];s.sizes=y;let R=s.margins[1],E=!0,H=!1;const N=H;for(let f=0;f<W;++f){if(y[f]>-1){if(H){const o=t+\"-rs\"+e+\"-\"+f;let l=d.getElement(o);if(!l){s.resizeHandles[f]=o;l=document.createElement(\"div\");l.setAttribute(\"id\",o);l.di=f;l.style.position=\"absolute\";l.style[n.left]=n.margins[1]+\"px\";l.style[s.size]=\"5px\";n.cSize&&(l.style[n.size]=n.cSize-n.margins[2]-n.margins[1]+\"px\");l.className=s.handleClass;i.insertBefore(l,i.firstChild);l.onmousedown=l.ontouchstart=function(t){const i=t||window.event;O(e,this,i)}}R+=s.margins[0]/2;k(l,s.left,R+\"px\");R+=5}else if(s.resizeHandles[f]){const e=d.getElement(s.resizeHandles[f]);e.parentNode.removeChild(e);delete s.resizeHandles[f]}E?E=!1:R+=H?s.margins[0]/2:s.margins[0];H=0!==s.config[f][g]}else if(s.resizeHandles[f]){const e=d.getElement(s.resizeHandles[f]);e.parentNode.removeChild(e);delete s.resizeHandles[f]}for(let t=0;t<b;++t){const i=s.getItem(f,t);if(i&&i.w){const t=i.w;let n,l=Math.max(y[f],0);if(i.span){let t,n=H;for(t=1;t<i.span[e]&&!(f+t>=y.length);++t){n&&(l+=10);n=0!==s.config[f+t][g];y[f+t-1]>-1&&y[f+t]>-1&&(l+=s.margins[0]);l+=y[f+t]}}k(t,\"visibility\",\"\");let r=i.align>>s.alignBits&15,c=i.ps[e];l<c&&(r=0);if(r){switch(r){case 1:n=R;break;case 4:n=R+(l-c)/2;break;case 2:n=R+(l-c)}c-=i.margin[e];if(i.layout){k(t,s.size,c+\"px\")&&G(i,1);i.set[e]=!0}else if(l>=c&&i.set[e]){k(t,s.size,c+\"px\")&&G(i,1);i.set[e]=!1}i.size[e]=c;i.psize[e]=c}else{const o=i.margin[e],f=Math.max(0,l-o),r=0===e&&i.sc[e];if(_(t)||!r&&l===c&&!i.layout)if(i.fs[e])e===m&&k(t,s.size,i.fs[e]-o+\"px\");else{k(t,s.size,\"\")&&G(i,1);i.set&&(i.set[e]=!1)}else if(k(t,s.size,f+\"px\")){G(i,1);i.set[e]=!0}n=R;i.size[e]=f;i.psize[e]=l}if(o)if(N){k(t,s.left,\"10px\");\"absolute\"!==d.css(t,\"position\")&&(t.style.position=\"relative\")}else k(t,s.left,\"0px\");else k(t,s.left,n+\"px\");if(1===e){t.wtResize&&t.wtResize(t,i.set[m]?Math.round(i.size[m]):-1,i.set[1]?Math.round(i.size[1]):-1,!0);i.dirty=0}}}y[f]>-1&&(R+=y[f])}if(s.resizeHandles.length>W){for(const e of s.resizeHandles)if(e){const t=d.getElement(e);t.parentNode.removeChild(t)}s.resizeHandles.length=W}i.querySelectorAll(`:scope > .${n.handleClass}`).forEach((function(e){e.style[s.size]=l-s.margins[2]-s.margins[1]+\"px\"}))}(e,i);return!0};this.contains=function(e){const i=d.getElement(t),s=d.getElement(e.getId());return!(!i||!s)&&d.contains(i,s)};this.WT=d})");
  }

  static WJavaScriptPreamble appjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.ApplicationScope,
        JavaScriptObjectType.JavaScriptObject,
        "layouts2",
        "new function(){const e=[];let t=!1;const i=this;let s=!1;this.find=function(e){const t=document.getElementById(e);return t?t.wtLayout:null};this.setDirty=function(e){const t=this.find(e);if(t){t.setDirty();i.scheduleAdjust()}};this.setElementDirty=function(e){let t=e;e=e.parentNode;for(;e&&e!==document.body;){const i=e.wtLayout;i&&i.setElDirty(t);t=e;e=e.parentNode}};this.setChildLayoutsDirty=function(e,t){for(const i of e.descendants){if(t){const s=e.WT.getElement(i.getId());if(s&&!e.WT.contains(t,s))continue}i.setDirty()}};this.add=function(t){!function e(t,i){for(let s=0,n=t.length;s<n;++s){const o=t[s];if(o.getId()===i.getId()){t[s]=i;i.descendants=o.descendants;return}if(o.contains(i)){e(o.descendants,i);return}if(i.contains(o)){i.descendants.push(o);t.splice(s,1);--s;--n}}t.push(i)}(e,t);i.scheduleAdjust()};let n=!1,o=0;this.scheduleAdjust=function(e){e&&(s=!0);if(!n){t?++o:o=0;if(!(o>=6)){n=!0;setTimeout((function(){i.adjust()}),0)}}};this.adjust=function(o,f){if(o){const e=this.find(o);e&&e.setItemsDirty(f);i.scheduleAdjust()}else{n=!1;if(!t){t=!0;l(e,0);r(e,0);l(e,1);r(e,1);t=!1;s=!1}}function l(e,t){for(const i of e){l(i.descendants,t);1===t&&s?i.setDirty():0===t&&i.setAllDirty();i.measure(t)}}function r(e,t){for(let i=0,s=e.length;i<s;++i){const n=e[i];if(n.apply(t))r(n.descendants,t);else{e.splice(i,1);--i;--s}}}};this.updateConfig=function(e,t){const i=this.find(e);i&&i.setConfig(t)};this.adjustNow=function(){n&&i.adjust()};let f=null;window.onresize=function(){clearTimeout(f);f=setTimeout((function(){f=null;i.scheduleAdjust(!0)}),20)};window.onshow=function(){s=!0;i.adjust()}}");
  }

  static WJavaScriptPreamble wtjs10() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "ChildrenResize",
        "(function(t,i,e,o){const s=this,n=e>=0;if(o)if(n){t.style.height=e+\"px\";t.lh=!0}else{t.style.height=\"\";t.lh=!1}else t.lh=!1;if(s.boxSizing(t)){e-=s.px(t,\"marginTop\");e-=s.px(t,\"marginBottom\");e-=s.px(t,\"borderTopWidth\");e-=s.px(t,\"borderBottomWidth\");e-=s.px(t,\"paddingTop\");e-=s.px(t,\"paddingBottom\");i-=s.px(t,\"marginLeft\");i-=s.px(t,\"marginRight\");i-=s.px(t,\"borderLeftWidth\");i-=s.px(t,\"borderRightWidth\");i-=s.px(t,\"paddingLeft\");i-=s.px(t,\"paddingRight\")}function p(t){let i=s.px(t,\"marginTop\");i+=s.px(t,\"marginBottom\");if(!s.boxSizing(t)){i+=s.px(t,\"borderTopWidth\");i+=s.px(t,\"borderBottomWidth\");i+=s.px(t,\"paddingTop\");i+=s.px(t,\"paddingBottom\")}return i}for(const o of t.childNodes)if(1===o.nodeType&&!o.classList.contains(\"wt-reparented\"))if(n){const t=e-p(o);if(t>0){if(o.offsetTop>0){const t=s.css(o,\"overflow\");\"visible\"!==t&&\"\"!==t||(o.style.overflow=\"auto\")}if(o.wtResize)o.wtResize(o,i,t,!0);else{const i=t+\"px\";if(o.style.height!==i){o.style.height=i;o.lh=!0}}}}else if(o.wtResize)o.wtResize(o,i,-1,!0);else{o.style.height=\"\";o.lh=!1}})");
  }

  static WJavaScriptPreamble wtjs11() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "ChildrenGetPS",
        "(function(t,i,e,o){return o})");
  }

  static WJavaScriptPreamble wtjs12() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "LastResize",
        "(function(t,i,e,o){const s=this,n=e>=0;if(o)if(n){t.style.height=e+\"px\";t.lh=!0}else{t.style.height=\"\";t.lh=!1}else t.lh=!1;let p=t.lastChild;for(;p&&1===p.nodeType&&(p.classList.contains(\"wt-reparented\")||p.classList.contains(\"resize-sensor\"));)p=p.previousSibling;if(!p)return;const h=p.previousSibling;if(n){if((e-=h.offsetHeight+s.px(h,\"marginTop\")+s.px(h,\"marginBottom\"))>0)if(p.wtResize)p.wtResize(p,i,e,!0);else{p.style.height=e+\"px\";p.lh=!0}}else if(p.wtResize)p.wtResize(p,-1,-1,!0);else{p.style.height=\"\";p.lh=!1}})");
  }

  static WJavaScriptPreamble wtjs13() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "LastGetPS",
        "(function(t,i,e,o){const s=this;for(const n of t.childNodes)if(n!==i){const t=s.css(n,\"position\");\"absolute\"!==t&&\"fixed\"!==t&&(0===e?o=Math.max(o,n.offsetWidth):o+=n.offsetHeight+s.px(n,\"marginTop\")+s.px(n,\"marginBottom\"))}return o})");
  }
}
