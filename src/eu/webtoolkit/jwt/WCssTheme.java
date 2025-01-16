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
 * CSS-based theme support. This implements the classic JWt themes, which were available before
 * theme support was customized with the addition of the {@link WTheme} class.
 *
 * <p>The following table shows which style classes are applied by this theme.
 *
 * <p>
 *
 * <table border="1" cellspacing="3" cellpadding="3">
 * <tr><td rowspan="12">{@link WAbstractItemView} *
 * </td><td>.Wt-itemview
 * </td><td>
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-headerdiv
 * </td><td>the header container
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-header
 * </td><td>the header
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-header .Wt-label
 * </td><td>a header label
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-header .Wt-label
 * </td><td>a header label
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-tv-rh
 * </td><td>resize handle
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-tv-sh
 * </td><td>sort handle
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-tv-sh-none
 * </td><td>sort handle, unsorted
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-tv-sh-down
 * </td><td>sort handle, descending sort
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-tv-sh-up
 * </td><td>sort handle, ascending sort
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-selected
 * </td><td>selected item (or row)
 * </td></tr>
 * <tr><td>.Wt-itemview .Wt-spacer
 * </td><td>spacer (briefly visible during scrolling)
 * </td></tr>
 * <tr><td>{@link WAbstractSpinBox}
 * </td><td>.Wt-spinbox
 * </td><td>(for the HTML4 implementation)
 * </td></tr>
 * <tr><td rowspan="11">{@link WCalendar} *
 * </td><td>.Wt-cal
 * </td><td>
 * </td></tr>
 * <tr><td>.Wt-cal table.d1
 * </td><td>the table (single letter days)
 * </td></tr>
 * <tr><td>.Wt-cal table.d3
 * </td><td>the table (three letter days)
 * </td></tr>
 * <tr><td>.Wt-cal table.dlong
 * </td><td>the table (ful day names)
 * </td></tr>
 * <tr><td>.Wt-cal th.caption
 * </td><td>a caption cell (containing month/year navigation)
 * </td></tr>
 * <tr><td>.Wt-cal th
 * </td><td>week day header cell
 * </td></tr>
 * <tr><td>.Wt-cal td
 * </td><td>day cell
 * </td></tr>
 * <tr><td>.Wt-cal-oom
 * </td><td>out-of-month day
 * </td></tr>
 * <tr><td>.Wt-cal-oom
 * </td><td>out-of-range day (ray &lt; bottom or day &gt; top)
 * </td></tr>
 * <tr><td>.Wt-cal-sel
 * </td><td>selected day
 * </td></tr>
 * <tr><td>.Wt-cal-now
 * </td><td>today
 * </td></tr>
 * <tr><td rowspan="2">{@link WDateEdit}
 * </td><td>.Wt-dateedit
 * </td><td>
 * </td></tr>
 * <tr><td>.Wt-datepicker
 * </td><td>the popup
 * </td></tr>
 * <tr><td>{@link WDatePicker}
 * </td><td>.Wt-datepicker
 * </td><td>the popup
 * </td></tr>
 * <tr><td rowspan="5">{@link WDialog}
 * </td><td>.Wt-dialog
 * </td><td>the dialog
 * </td></tr>
 * <tr><td>.Wt-dialog .closeicon
 * </td><td>the close icon in the titlebar
 * </td></tr>
 * <tr><td>.Wt-dialog .titlebar
 * </td><td>the titlebar
 * </td></tr>
 * <tr><td>.Wt-dialog .body
 * </td><td>the dialog body
 * </td></tr>
 * <tr><td>.Wt-dialog .footer
 * </td><td>the dialog footer
 * </td></tr>
 * <tr><td rowspan="8">{@link WMenuItem}
 * </td><td>.item
 * </td><td>an unselected item
 * </td></tr>
 * <tr><td>.itemselected
 * </td><td>a selected item
 * </td></tr>
 * <tr><td>.item.Wt-closable
 * </td><td>a closable item
 * </td></tr>
 * <tr><td>.item.Wt-separator
 * </td><td>a separator item
 * </td></tr>
 * <tr><td>.item.Wt-sectheader
 * </td><td>a section header item
 * </td></tr>
 * <tr><td>.item .Wt-icon
 * </td><td>the item&apos;s icon
 * </td></tr>
 * <tr><td>.item .Wt-chkbox
 * </td><td>the item&apos;s checkbox
 * </td></tr>
 * <tr><td>.item .closeicon
 * </td><td>the item&apos;s close icon
 * </td></tr>
 * <tr><td>{@link WMessageBox}
 * </td><td>.Wt-dialog
 * </td><td>see supra (WDialog)
 * </td></tr>
 * <tr><td rowspan="3">{@link WPanel}
 * </td><td>.Wt-panel
 * </td><td>
 * </td></tr>
 * <tr><td>.Wt-panel .titlebar
 * </td><td>the titlebar
 * </td></tr>
 * <tr><td>.Wt-panel .body
 * </td><td>the body
 * </td></tr>
 * <tr><td>{@link WPopupMenu}
 * </td><td>.Wt-popupmenu
 * </td><td>the popup menu; for the items, see supra (WMenuItem)
 * </td></tr>
 * <tr><td>{@link WPopupWidget}
 * </td><td>.Wt-outset
 * </td><td>
 * </td></tr>
 * <tr><td rowspan="3">{@link WProgressBar}
 * </td><td>.Wt-progressbar
 * </td><td>
 * </td></tr>
 * <tr><td>.Wt-progressbar .Wt-pgb-bar
 * </td><td>the bar
 * </td></tr>
 * <tr><td>.Wt-progressbar .Wt-pgb-label
 * </td><td>the value label
 * </td></tr>
 * <tr><td>{@link WPushButton}
 * </td><td>.Wt-btn
 * </td><td>
 * </td></tr>
 * <tr><td rowspan="6">{@link WSlider} *
 * </td><td>.Wt-slider-h <i>or</i> .Wt-slider-v
 * </td><td>for horizontal or vertical slider
 * </td></tr>
 * <tr><td>.Wt-slider-[hv] .Wt-slider-bg
 * </td><td>background element
 * </td></tr>
 * <tr><td>.Wt-slider-[hv] .fill
 * </td><td>fill to the current value
 * </td></tr>
 * <tr><td>.Wt-slider-[hv] .handle
 * </td><td>the slider handle
 * </td></tr>
 * <tr><td>.Wt-slider-[hv] .Wt-w
 * </td><td>an additional element for styling
 * </td></tr>
 * <tr><td>.Wt-slider-[hv] .Wt-e
 * </td><td>an additional element for styling
 * </td></tr>
 * <tr><td rowspan="3">{@link WSuggestionPopup}
 * </td><td>.Wt-suggest
 * </td><td>
 * </td></tr>
 * <tr><td>.Wt-suggest li
 * </td><td>an item
 * </td></tr>
 * <tr><td>.Wt-suggest .active
 * </td><td>an active item
 * </td></tr>
 * <tr><td>{@link WTabWidget}
 * </td><td>.Wt-tabs
 * </td><td>the header, which is a {@link WMenu}
 * </td></tr>
 * <tr><td rowspan="3">{@link WTableView} *
 * </td><td>.Wt-tableview
 * </td><td>see supra (WAbstractItemView)
 * </td></tr>
 * <tr><td>.Wt-tableview .Wt-contents
 * </td><td>the contents area
 * </td></tr>
 * <tr><td>.Wt-tableview .Wt-contents .Wt-tv-c
 * </td><td>a contents cell
 * </td></tr>
 * <tr><td rowspan="10">{@link WTreeNode} *
 * </td><td>.Wt-tree
 * </td><td>a tree node
 * </td></tr>
 * <tr><td>.Wt-tree.Wt-trunk
 * </td><td>a trunk node
 * </td></tr>
 * <tr><td>.Wt-tree.Wt-end
 * </td><td>an end node (last node within parent)
 * </td></tr>
 * <tr><td>.Wt-tree ul
 * </td><td>children list
 * </td></tr>
 * <tr><td>.Wt-tree .Wt-ctrl
 * </td><td>collapse/expand control
 * </td></tr>
 * <tr><td>.Wt-tree .Wt-ctrl.expand
 * </td><td>expand control
 * </td></tr>
 * <tr><td>.Wt-tree .Wt-ctrl.collapse
 * </td><td>collapse control
 * </td></tr>
 * <tr><td>.Wt-tree .Wt-ctrl.noexpand
 * </td><td>an item that cannot be expanded
 * </td></tr>
 * <tr><td>.Wt-tree .Wt-selected
 * </td><td>a selected node
 * </td></tr>
 * <tr><td>.Wt-tree .Wt-label
 * </td><td>the label
 * </td></tr>
 * <tr><td rowspan="10">{@link WTreeView} *
 * </td><td>.Wt-treeview
 * </td><td>see supra (WAbstractItemView)
 * </td></tr>
 * <tr><td>.Wt-treeview ul
 * </td><td>a node
 * </td></tr>
 * <tr><td>.Wt-treeview ul.Wt-tv-root
 * </td><td>the root node
 * </td></tr>
 * <tr><td>.Wt-treeview .Wt-tv-row
 * </td><td>a row of additional cells
 * </td></tr>
 * <tr><td>.Wt-treeview .Wt-trunk
 * </td><td>a trunk node
 * </td></tr>
 * <tr><td>.Wt-treeview .Wt-end
 * </td><td>an end node (last node within parent)
 * </td></tr>
 * <tr><td>.Wt-treeview .Wt-ctrl
 * </td><td>collapse/expand control
 * </td></tr>
 * <tr><td>.Wt-treeview .Wt-ctrl.expand
 * </td><td>expand control
 * </td></tr>
 * <tr><td>.Wt-treeview .Wt-ctrl.collapse
 * </td><td>collapse control
 * </td></tr>
 * <tr><td>.Wt-treeview .Wt-ctrl.noexpand
 * </td><td>an item that cannot be expanded
 * </td></tr>
 * </table>
 *
 * <p><code>*</code> CSS selectors for these widgets are currently still hard-coded in the widget
 * itself.
 *
 * <p>
 *
 * @see WApplication#setTheme(WTheme theme)
 */
public class WCssTheme extends WTheme {
  private static Logger logger = LoggerFactory.getLogger(WCssTheme.class);

  /**
   * Constructor.
   *
   * <p>Creates a classic JWt theme. JWt comes with two CSS themes: &quot;polished&quot; and
   * default. For the bootstrap theme, use {@link WBootstrapTheme}.
   */
  public WCssTheme(final String name) {
    super();
    this.name_ = name;
  }

  public String getName() {
    return this.name_;
  }
  /**
   * Returns the stylesheets.
   *
   * <p>Returns wt.css, plus on IE wt_ie.css, plus on IE6 wt_ie6.css. The style sheets are located
   * in the theme directory in the resources folder.
   */
  public List<WLinkedCssStyleSheet> getStyleSheets() {
    List<WLinkedCssStyleSheet> result = new ArrayList<WLinkedCssStyleSheet>();
    if (this.name_.length() != 0) {
      String themeDir = this.getResourcesUrl();
      WApplication app = WApplication.getInstance();
      result.add(new WLinkedCssStyleSheet(new WLink(themeDir + "wt.css")));
      if (app.getEnvironment().agentIsIElt(9)) {
        result.add(new WLinkedCssStyleSheet(new WLink(themeDir + "wt_ie.css")));
      }
      if (app.getEnvironment().getAgent() == UserAgent.IE6) {
        result.add(new WLinkedCssStyleSheet(new WLink(themeDir + "wt_ie6.css")));
      }
    }
    return result;
  }

  public void apply(WWidget widget, WWidget child, int widgetRole) {
    if (!widget.isThemeStyleEnabled()) {
      return;
    }
    switch (widgetRole) {
      case WidgetThemeRole.MenuItemIcon:
        child.addStyleClass("Wt-icon");
        break;
      case WidgetThemeRole.MenuItemCheckBox:
        child.addStyleClass("Wt-chkbox");
        break;
      case WidgetThemeRole.MenuItemClose:
        widget.addStyleClass("Wt-closable");
        child.addStyleClass("closeicon");
        break;
      case WidgetThemeRole.DialogCoverWidget:
        child.setStyleClass("Wt-dialogcover in");
        break;
      case WidgetThemeRole.DialogTitleBar:
        child.addStyleClass("titlebar");
        break;
      case WidgetThemeRole.DialogBody:
        child.addStyleClass("body");
        break;
      case WidgetThemeRole.DialogFooter:
        child.addStyleClass("footer");
        break;
      case WidgetThemeRole.DialogCloseIcon:
        child.addStyleClass("closeicon");
        break;
      case WidgetThemeRole.TableViewRowContainer:
        {
          WAbstractItemView view = ObjectUtils.cast(widget, WAbstractItemView.class);
          String backgroundImage = "";
          if (view.hasAlternatingRowColors()) {
            backgroundImage = "stripes/stripe-";
          } else {
            backgroundImage = "no-stripes/no-stripe-";
          }
          backgroundImage =
              this.getResourcesUrl()
                  + backgroundImage
                  + String.valueOf((int) view.getRowHeight().toPixels())
                  + "px.gif";
          child.getDecorationStyle().setBackgroundImage(new WLink(backgroundImage));
          break;
        }
      case WidgetThemeRole.DatePickerPopup:
        child.addStyleClass("Wt-datepicker");
        break;
      case WidgetThemeRole.DatePickerIcon:
        {
          WImage icon = ObjectUtils.cast(child, WImage.class);
          icon.setImageLink(new WLink(WApplication.getRelativeResourcesUrl() + "date.gif"));
          icon.setVerticalAlignment(AlignmentFlag.Middle);
          icon.resize(new WLength(16), new WLength(16));
          break;
        }
      case WidgetThemeRole.PanelTitleBar:
        child.addStyleClass("titlebar");
        break;
      case WidgetThemeRole.PanelBody:
        child.addStyleClass("body");
        break;
      case WidgetThemeRole.PanelCollapseButton:
        child.setFloatSide(Side.Left);
        break;
      case WidgetThemeRole.AuthWidgets:
        WApplication app = WApplication.getInstance();
        app.useStyleSheet(new WLink(WApplication.getRelativeResourcesUrl() + "form.css"));
        app.getBuiltinLocalizedStrings().useBuiltin(WtServlet.AuthCssTheme_xml);
        break;
    }
  }

  public void apply(WWidget widget, final DomElement element, int elementRole) {
    boolean creating = element.getMode() == DomElement.Mode.Create;
    if (!widget.isThemeStyleEnabled()) {
      return;
    }
    {
      WPopupWidget popup = ObjectUtils.cast(widget, WPopupWidget.class);
      if (popup != null) {
        element.addPropertyWord(Property.Class, "Wt-outset");
      }
    }
    switch (element.getType()) {
      case BUTTON:
        if (creating) {
          element.addPropertyWord(Property.Class, "Wt-btn");
          WPushButton b = ObjectUtils.cast(widget, WPushButton.class);
          if (b != null) {
            if (b.isDefault()) {
              element.addPropertyWord(Property.Class, "Wt-btn-default");
            }
            if (!(b.getText().length() == 0)) {
              element.addPropertyWord(Property.Class, "with-label");
            }
          }
        }
        break;
      case UL:
        if (ObjectUtils.cast(widget, WPopupMenu.class) != null) {
          element.addPropertyWord(Property.Class, "Wt-popupmenu Wt-outset");
        } else {
          WTabWidget tabs = ObjectUtils.cast(widget.getParent().getParent(), WTabWidget.class);
          if (tabs != null) {
            element.addPropertyWord(Property.Class, "Wt-tabs");
          } else {
            WSuggestionPopup suggestions = ObjectUtils.cast(widget, WSuggestionPopup.class);
            if (suggestions != null) {
              element.addPropertyWord(Property.Class, "Wt-suggest");
            }
          }
        }
        break;
      case LI:
        {
          WMenuItem item = ObjectUtils.cast(widget, WMenuItem.class);
          if (item != null) {
            if (item.isSeparator()) {
              element.addPropertyWord(Property.Class, "Wt-separator");
            }
            if (item.isSectionHeader()) {
              element.addPropertyWord(Property.Class, "Wt-sectheader");
            }
            if (item.getMenu() != null) {
              element.addPropertyWord(Property.Class, "submenu");
            }
          }
        }
        break;
      case DIV:
        {
          WDialog dialog = ObjectUtils.cast(widget, WDialog.class);
          if (dialog != null) {
            element.addPropertyWord(Property.Class, "Wt-dialog");
            return;
          }
          WPanel panel = ObjectUtils.cast(widget, WPanel.class);
          if (panel != null) {
            element.addPropertyWord(Property.Class, "Wt-panel Wt-outset");
            return;
          }
          WProgressBar bar = ObjectUtils.cast(widget, WProgressBar.class);
          if (bar != null) {
            switch (elementRole) {
              case ElementThemeRole.MainElement:
                element.addPropertyWord(Property.Class, "Wt-progressbar");
                break;
              case ElementThemeRole.ProgressBarBar:
                element.addPropertyWord(Property.Class, "Wt-pgb-bar");
                break;
              case ElementThemeRole.ProgressBarLabel:
                element.addPropertyWord(Property.Class, "Wt-pgb-label");
            }
            return;
          }
        }
        break;
      case INPUT:
        {
          WAbstractSpinBox spinBox = ObjectUtils.cast(widget, WAbstractSpinBox.class);
          if (spinBox != null) {
            element.addPropertyWord(Property.Class, "Wt-spinbox");
            return;
          }
          WDateEdit dateEdit = ObjectUtils.cast(widget, WDateEdit.class);
          if (dateEdit != null) {
            element.addPropertyWord(Property.Class, "Wt-dateedit");
            return;
          }
          WTimeEdit timeEdit = ObjectUtils.cast(widget, WTimeEdit.class);
          if (timeEdit != null) {
            element.addPropertyWord(Property.Class, "Wt-timeedit");
            return;
          }
        }
        break;
      default:
        break;
    }
  }
  /**
   * Returns a generic CSS class name for a disabled element.
   *
   * <p>The CSS class Wt-disabled is applied to disabled classes.
   */
  public String getDisabledClass() {
    return "Wt-disabled";
  }
  /**
   * Returns a generic CSS class name for an active element.
   *
   * <p>The CSS class Wt-selected is applied to active classes.
   */
  public String getActiveClass() {
    return "Wt-selected";
  }

  public String utilityCssClass(int utilityCssClassRole) {
    switch (utilityCssClassRole) {
      case UtilityCssClassRole.ToolTipOuter:
        return "Wt-tooltip";
      default:
        return "";
    }
  }
  /**
   * Returns whether the theme allows for an anchor to be styled as a button.
   *
   * <p>Returns false.
   */
  public boolean isCanStyleAnchorAsButton() {
    return false;
  }

  public void applyValidationStyle(
      WWidget widget, final WValidator.Result validation, EnumSet<ValidationStyleFlag> styles) {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/CssThemeValidate.js", wtjs1());
    app.loadJavaScript("js/CssThemeValidate.js", wtjs2());
    if (app.getEnvironment().hasAjax()) {
      StringBuilder js = new StringBuilder();
      js.append("Wt4.11.2.setValidationState(")
          .append(widget.getJsRef())
          .append(",")
          .append(validation.getState() == ValidationState.Valid)
          .append(",")
          .append(WString.toWString(validation.getMessage()).getJsStringLiteral())
          .append(",")
          .append(EnumUtils.valueOf(styles))
          .append(");");
      widget.doJavaScript(js.toString());
    } else {
      boolean validStyle =
          validation.getState() == ValidationState.Valid
              && styles.contains(ValidationStyleFlag.ValidStyle);
      boolean invalidStyle =
          validation.getState() != ValidationState.Valid
              && styles.contains(ValidationStyleFlag.InvalidStyle);
      widget.toggleStyleClass("Wt-valid", validStyle);
      widget.toggleStyleClass("Wt-invalid", invalidStyle);
    }
  }

  public boolean canBorderBoxElement(final DomElement element) {
    return true;
  }

  private String name_;

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "validate",
        "(function(t){let e;if(t.options){const i=t.options.item(t.selectedIndex);e=null===i?\"\":i.text}else e=\"object\"==typeof t.wtLObj&&\"function\"==typeof t.wtLObj.getValue?t.wtLObj.getValue():\"function\"==typeof t.wtEncodeValue?t.wtEncodeValue(t):t.value;e=t.wtValidate.validate(e);this.setValidationState(t,e.valid,e.message,1)})");
  }

  static WJavaScriptPreamble wtjs2() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "setValidationState",
        "(function(t,e,i,a){const l=e&&0!=(2&a),n=!e&&0!=(1&a);t.classList.toggle(\"Wt-valid\",l);t.classList.toggle(\"Wt-invalid\",n);void 0===t.defaultTT&&(t.defaultTT=t.getAttribute(\"title\")||\"\");e?t.setAttribute(\"title\",t.defaultTT):t.setAttribute(\"title\",i)})");
  }
}
