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
 * Theme based on the Twitter Bootstrap 2 CSS framework.
 *
 * <p>This theme implements support for building a JWt web application that uses Twitter Bootstrap
 * as a theme for its (layout and) styling. The theme comes with CSS from Bootstrap version 2.2.2.
 * Only the CSS components of twitter bootstrap are used, but not the JavaScript (i.e. the
 * functional parts), since the functionality is already built-in to the widgets from the library.
 *
 * <p>Using this theme, various widgets provided by the library are rendered using markup that is
 * compatible with Twitter Bootstrap. The bootstrap theme is also extended with a proper
 * (compatible) styling of widgets for which bootstrap does not provide styling (table views, tree
 * views, sliders, etc...).
 *
 * <p>By default, the theme will use CSS resources that are shipped together with the JWt
 * distribution, but since the Twitter Bootstrap CSS API is a popular API for custom themes, you can
 * easily replace the CSS with custom-built CSS (by reimplementing {@link
 * WBootstrap2Theme#getStyleSheets() getStyleSheets()}).
 *
 * <p>Although this theme facilitates the use of Twitter Bootstrap with JWt, it is still important
 * to understand how Bootstrap expects markup to be, especially related to layout using its grid
 * system, for which we refer to the official bootstrap documentation, see <a
 * href="http://getbootstrap.com">http://getbootstrap.com</a>
 *
 * <p>
 *
 * @see WApplication#setTheme(WTheme theme)
 */
public class WBootstrap2Theme extends WTheme {
  private static Logger logger = LoggerFactory.getLogger(WBootstrap2Theme.class);

  /** Constructor. */
  public WBootstrap2Theme() {
    super();
    this.responsive_ = false;
  }
  /**
   * Enables responsive features.
   *
   * <p>Responsive features can be enabled only at application startup.
   *
   * <p>Responsive features are disabled by default.
   */
  public void setResponsive(boolean enabled) {
    this.responsive_ = enabled;
  }
  /**
   * Returns whether responsive features are enabled.
   *
   * <p>
   *
   * @see WBootstrap2Theme#setResponsive(boolean enabled)
   */
  public boolean isResponsive() {
    return this.responsive_;
  }

  public String getName() {
    return "bootstrap2";
  }

  public String getResourcesUrl() {
    return WApplication.getRelativeResourcesUrl() + "themes/bootstrap/2/";
  }

  public List<WLinkedCssStyleSheet> getStyleSheets() {
    List<WLinkedCssStyleSheet> result = new ArrayList<WLinkedCssStyleSheet>();
    final String themeDir = this.getResourcesUrl();
    result.add(new WLinkedCssStyleSheet(new WLink(themeDir + "bootstrap.css")));
    if (this.responsive_) {
      result.add(new WLinkedCssStyleSheet(new WLink(themeDir + "bootstrap-responsive.css")));
    }
    result.add(new WLinkedCssStyleSheet(new WLink(themeDir + "wt.css")));
    return result;
  }

  public void init(WApplication app) {
    app.getBuiltinLocalizedStrings().useBuiltin(WtServlet.BootstrapTheme_xml);
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
        child.setStyleClass("Wt-chkbox");
        ((WFormWidget) child).getLabel().addStyleClass("checkbox-inline");
        break;
      case WidgetThemeRole.MenuItemClose:
        {
          child.addStyleClass("close");
          ((WText) child).setText("&times;");
          break;
        }
      case WidgetThemeRole.DialogCoverWidget:
        child.addStyleClass("modal-backdrop Wt-bootstrap2");
        break;
      case WidgetThemeRole.DialogTitleBar:
        child.addStyleClass("modal-header");
        break;
      case WidgetThemeRole.DialogBody:
        child.addStyleClass("modal-body");
        break;
      case WidgetThemeRole.DialogFooter:
        child.addStyleClass("modal-footer");
        break;
      case WidgetThemeRole.DialogCloseIcon:
        {
          child.addStyleClass("close");
          ((WText) child).setText("&times;");
          break;
        }
      case WidgetThemeRole.TableViewRowContainer:
        {
          WAbstractItemView view = (WAbstractItemView) widget;
          child.toggleStyleClass("Wt-striped", view.hasAlternatingRowColors());
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
      case WidgetThemeRole.TimePickerPopup:
        child.addStyleClass("Wt-timepicker");
        break;
      case WidgetThemeRole.PanelTitleBar:
        child.addStyleClass("accordion-heading");
        break;
      case WidgetThemeRole.PanelCollapseButton:
        child.setFloatSide(Side.Left);
      case WidgetThemeRole.PanelTitle:
        child.addStyleClass("accordion-toggle");
        break;
      case WidgetThemeRole.PanelBody:
        child.addStyleClass("accordion-inner");
        break;
      case WidgetThemeRole.InPlaceEditing:
        child.addStyleClass("input-append");
        break;
      case WidgetThemeRole.NavCollapse:
        child.addStyleClass("nav-collapse");
        break;
      case WidgetThemeRole.NavBrand:
        child.addStyleClass("brand");
        break;
      case WidgetThemeRole.NavbarForm:
        child.addStyleClass("navbar-form");
        break;
      case WidgetThemeRole.NavbarSearchForm:
        child.addStyleClass("navbar-search");
        break;
      case WidgetThemeRole.NavbarSearchInput:
        child.addStyleClass("search-query");
        break;
      case WidgetThemeRole.NavbarAlignLeft:
        child.addStyleClass("pull-left");
        break;
      case WidgetThemeRole.NavbarAlignRight:
        child.addStyleClass("pull-right");
        break;
      case WidgetThemeRole.NavbarMenu:
        child.addStyleClass("navbar-nav");
        break;
      case WidgetThemeRole.NavbarBtn:
        child.addStyleClass("btn-navbar");
        break;
    }
  }

  public void apply(WWidget widget, final DomElement element, int elementRole) {
    final boolean creating = element.getMode() == DomElement.Mode.Create;
    if (!widget.isThemeStyleEnabled()) {
      return;
    }
    {
      WPopupWidget popup = ObjectUtils.cast(widget, WPopupWidget.class);
      if (popup != null) {
        WDialog dialog = ObjectUtils.cast(widget, WDialog.class);
        if (!(dialog != null)) {
          element.addPropertyWord(Property.Class, "dropdown-menu");
        }
      }
    }
    switch (element.getType()) {
      case A:
        {
          WPushButton btn = ObjectUtils.cast(widget, WPushButton.class);
          if (creating && btn != null) {
            element.addPropertyWord(Property.Class, "btn");
            if (btn.isDefault()) {
              element.addPropertyWord(Property.Class, "btn-primary");
            }
          }
          if (element.getProperty(Property.Class).indexOf("dropdown-toggle") != -1) {
            WMenuItem item = ObjectUtils.cast(widget.getParent(), WMenuItem.class);
            if (!(ObjectUtils.cast(item.getParentMenu(), WPopupMenu.class) != null)) {
              DomElement b = DomElement.createNew(DomElementType.B);
              b.setProperty(Property.Class, "caret");
              element.addChild(b);
            }
          }
          break;
        }
      case BUTTON:
        {
          if (creating && !widget.hasStyleClass("list-group-item")) {
            element.addPropertyWord(Property.Class, "btn");
          }
          WPushButton button = ObjectUtils.cast(widget, WPushButton.class);
          if (button != null) {
            if (creating && button.isDefault()) {
              element.addPropertyWord(Property.Class, "btn-primary");
            }
            if (button.getMenu() != null
                && element.getProperties().get(Property.InnerHTML) != null) {
              element.addPropertyWord(Property.InnerHTML, "<span class=\"caret\"></span>");
            }
            if (creating && !(button.getText().length() == 0)) {
              element.addPropertyWord(Property.Class, "with-label");
            }
            if (!button.getLink().isNull()) {
              logger.error(
                  new StringWriter()
                      .append(
                          "Cannot use WPushButton::setLink() after the button has been rendered with WBootstrapTheme")
                      .toString());
            }
          }
          break;
        }
      case DIV:
        {
          WDialog dialog = ObjectUtils.cast(widget, WDialog.class);
          if (dialog != null) {
            element.addPropertyWord(Property.Class, "modal");
            return;
          }
          WPanel panel = ObjectUtils.cast(widget, WPanel.class);
          if (panel != null) {
            element.addPropertyWord(Property.Class, "accordion-group");
            return;
          }
          WProgressBar bar = ObjectUtils.cast(widget, WProgressBar.class);
          if (bar != null) {
            switch (elementRole) {
              case ElementThemeRole.MainElement:
                element.addPropertyWord(Property.Class, "progress");
                break;
              case ElementThemeRole.ProgressBarBar:
                element.addPropertyWord(Property.Class, "bar");
                break;
              case ElementThemeRole.ProgressBarLabel:
                element.addPropertyWord(Property.Class, "bar-label");
            }
            return;
          }
          WGoogleMap map = ObjectUtils.cast(widget, WGoogleMap.class);
          if (map != null) {
            element.addPropertyWord(Property.Class, "Wt-googlemap");
            return;
          }
          WAbstractItemView itemView = ObjectUtils.cast(widget, WAbstractItemView.class);
          if (itemView != null) {
            element.addPropertyWord(Property.Class, "form-inline");
            return;
          }
          WNavigationBar navBar = ObjectUtils.cast(widget, WNavigationBar.class);
          if (navBar != null) {
            element.addPropertyWord(Property.Class, "navbar");
            return;
          }
          break;
        }
      case LABEL:
        {
          if (elementRole == ElementThemeRole.ToggleButtonRole) {
            WCheckBox cb = ObjectUtils.cast(widget, WCheckBox.class);
            WRadioButton rb = null;
            if (cb != null) {
              element.addPropertyWord(Property.Class, "checkbox");
            } else {
              rb = ObjectUtils.cast(widget, WRadioButton.class);
              if (rb != null) {
                element.addPropertyWord(Property.Class, "radio");
              }
            }
            if ((cb != null || rb != null) && widget.isInline()) {
              element.addPropertyWord(Property.Class, "inline");
            }
          }
          break;
        }
      case LI:
        {
          WMenuItem item = ObjectUtils.cast(widget, WMenuItem.class);
          if (item != null) {
            if (item.isSeparator()) {
              element.addPropertyWord(Property.Class, "divider");
            }
            if (item.isSectionHeader()) {
              element.addPropertyWord(Property.Class, "nav-header");
            }
            if (item.getMenu() != null) {
              if (ObjectUtils.cast(item.getParentMenu(), WPopupMenu.class) != null) {
                element.addPropertyWord(Property.Class, "dropdown-submenu");
              } else {
                element.addPropertyWord(Property.Class, "dropdown");
              }
            }
          }
          break;
        }
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
          break;
        }
      case UL:
        {
          WPopupMenu popupMenu = ObjectUtils.cast(widget, WPopupMenu.class);
          if (popupMenu != null) {
            element.addPropertyWord(Property.Class, "dropdown-menu");
            if (popupMenu.getParentItem() != null
                && ObjectUtils.cast(popupMenu.getParentItem().getParentMenu(), WPopupMenu.class)
                    != null) {
              element.addPropertyWord(Property.Class, "submenu");
            }
          } else {
            WMenu menu = ObjectUtils.cast(widget, WMenu.class);
            if (menu != null) {
              element.addPropertyWord(Property.Class, "nav");
              WTabWidget tabs = ObjectUtils.cast(menu.getParent().getParent(), WTabWidget.class);
              if (tabs != null) {
                element.addPropertyWord(Property.Class, "nav-tabs");
              }
            } else {
              WSuggestionPopup suggestions = ObjectUtils.cast(widget, WSuggestionPopup.class);
              if (suggestions != null) {
                element.addPropertyWord(Property.Class, "typeahead");
              }
            }
          }
          break;
        }
      case SPAN:
        {
          WInPlaceEdit inPlaceEdit = ObjectUtils.cast(widget, WInPlaceEdit.class);
          if (inPlaceEdit != null) {
            element.addPropertyWord(Property.Class, "Wt-in-place-edit");
          } else {
            WDatePicker picker = ObjectUtils.cast(widget, WDatePicker.class);
            if (picker != null) {
              element.addPropertyWord(Property.Class, "Wt-datepicker");
            }
          }
          break;
        }
      default:
        break;
    }
  }

  public String getDisabledClass() {
    return "disabled";
  }

  public String getActiveClass() {
    return "active";
  }

  public String utilityCssClass(int utilityCssClassRole) {
    switch (utilityCssClassRole) {
      case UtilityCssClassRole.ToolTipInner:
        return "tooltip-inner";
      case UtilityCssClassRole.ToolTipOuter:
        return "tooltip fade top in";
      default:
        return "";
    }
  }

  public boolean isCanStyleAnchorAsButton() {
    return true;
  }

  public void applyValidationStyle(
      WWidget widget, final WValidator.Result validation, EnumSet<ValidationStyleFlag> styles) {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/BootstrapValidate.js", wtjs1());
    app.loadJavaScript("js/BootstrapValidate.js", wtjs2());
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
    return element.getType() != DomElementType.INPUT;
  }

  private boolean responsive_;

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "validate",
        "(function(t){let e;if(t.options){const s=t.options.item(t.selectedIndex);e=null===s?\"\":s.text}else e=\"object\"==typeof t.wtLObj&&\"function\"==typeof t.wtLObj.getValue?t.wtLObj.getValue():\"function\"==typeof t.wtEncodeValue?t.wtEncodeValue(t):t.value;e=t.wtValidate.validate(e);this.setValidationState(t,e.valid,e.message,1)})");
  }

  static WJavaScriptPreamble wtjs2() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptFunction,
        "setValidationState",
        "(function(t,e,s,l){const o=e&&0!=(2&l),i=!e&&0!=(1&l);let a=\"Wt-valid\",c=\"Wt-invalid\";const n=this.theme;if(\"object\"==typeof n){a=n.classes.valid;c=n.classes.invalid}t.classList.toggle(a,o);t.classList.toggle(c,i);let u,f,d;u=t.closest(\".control-group\");if(u){f=\"success\";d=\"error\"}else{u=t.closest(\".form-group\");if(u){f=\"has-success\";d=\"has-error\"}}if(u){const l=u.querySelectorAll(\".Wt-validation-message\");for(const o of l)o.textContent=e?t.defaultTT:s;u.classList.toggle(f,o);u.classList.toggle(d,i)}e?t.setAttribute(\"title\",t.defaultTT):t.setAttribute(\"title\",s)})");
  }
}
