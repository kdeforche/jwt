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
 * A CSS style sheet.
 *
 * <p>
 *
 * @see WApplication#getStyleSheet()
 */
public class WCssStyleSheet {
  private static Logger logger = LoggerFactory.getLogger(WCssStyleSheet.class);

  /** Creates a new (internal) style sheet. */
  public WCssStyleSheet() {
    this.rules_ = new ArrayList<WCssRule>();
    this.rulesAdded_ = new ArrayList<WCssRule>();
    this.rulesModified_ = new HashSet<WCssRule>();
    this.rulesRemoved_ = new ArrayList<String>();
    this.defined_ = new HashSet<String>();
  }
  /**
   * Adds a CSS rule.
   *
   * <p>Add a rule using the CSS selector <code>selector</code>, with CSS declarations in <code>
   * declarations</code>. These declarations must be a list separated by semi-colons (;).
   *
   * <p>Optionally, you may give a <code>ruleName</code>, which may later be used to check if the
   * rule was already defined.
   *
   * <p>
   *
   * @see WCssStyleSheet#isDefined(String ruleName)
   */
  public WCssTextRule addRule(
      final String selector, final String declarations, final String ruleName) {
    WCssTextRule r = new WCssTextRule(selector, declarations);
    WCssTextRule result = r;
    this.addRule(r, ruleName);
    return result;
  }
  /**
   * Adds a CSS rule.
   *
   * <p>Returns {@link #addRule(String selector, String declarations, String ruleName)
   * addRule(selector, declarations, "")}
   */
  public final WCssTextRule addRule(final String selector, final String declarations) {
    return addRule(selector, declarations, "");
  }
  /**
   * Adds a CSS rule.
   *
   * <p>Add a rule using the CSS selector <code>selector</code>, with styles specified in <code>
   * style</code>.
   *
   * <p>Optionally, you may give a <code>ruleName</code>, which may later be used to check if the
   * rule was already defined.
   *
   * <p>
   *
   * @see WCssStyleSheet#isDefined(String ruleName)
   */
  public WCssTemplateRule addRule(
      final String selector, final WCssDecorationStyle style, final String ruleName) {
    WCssTemplateRule r = new WCssTemplateRule(selector);
    r.getTemplateWidget().setDecorationStyle(style);
    WCssTemplateRule result = r;
    this.addRule(r, ruleName);
    return result;
  }
  /**
   * Adds a CSS rule.
   *
   * <p>Returns {@link #addRule(String selector, WCssDecorationStyle style, String ruleName)
   * addRule(selector, style, "")}
   */
  public final WCssTemplateRule addRule(final String selector, final WCssDecorationStyle style) {
    return addRule(selector, style, "");
  }
  /**
   * Adds a CSS rule.
   *
   * <p>Optionally, you may give a <code>ruleName</code>, which may later be used to check if the
   * rule was already defined. Note: you may not pass the same rule to 2 diffrent applications.
   *
   * <p>
   *
   * @see WCssStyleSheet#isDefined(String ruleName)
   */
  public WCssRule addRule(WCssRule rule, final String ruleName) {
    rule.sheet_ = this;
    this.rulesAdded_.add(rule);
    this.rules_.add(rule);
    if (ruleName.length() != 0) {
      this.defined_.add(ruleName);
    }
    return this.rules_.get(this.rules_.size() - 1);
  }
  /**
   * Adds a CSS rule.
   *
   * <p>Returns {@link #addRule(WCssRule rule, String ruleName) addRule(rule, "")}
   */
  public final WCssRule addRule(WCssRule rule) {
    return addRule(rule, "");
  }
  // public Rule  addRule(<Woow... some pseudoinstantiation type!> rule) ;
  /**
   * Returns if a rule was already defined in this style sheet.
   *
   * <p>Returns whether a rule was added with the given <code>ruleName</code>.
   *
   * <p>
   *
   * @see WCssStyleSheet#addRule(String selector, String declarations, String ruleName)
   */
  public boolean isDefined(final String ruleName) {
    boolean i = this.defined_.contains(ruleName);
    return i != false;
  }
  /** Removes a rule. */
  public WCssRule removeRule(WCssRule rule) {
    WCssRule r = CollectionUtils.take(this.rules_, rule);
    if (r != null) {
      if (!this.rulesAdded_.remove(rule)) {
        this.rulesRemoved_.add(rule.getSelector());
      }
      this.rulesModified_.remove(rule);
    }
    return r;
  }

  void ruleModified(WCssRule rule) {
    if (this.rulesAdded_.indexOf(rule) == -1) {
      this.rulesModified_.add(rule);
    }
  }

  public void cssText(final StringBuilder out, boolean all) {
    if (all) {
      final List<WCssRule> toProcess = this.rules_;
      for (int i = 0; i < toProcess.size(); ++i) {
        WCssRule rule = toProcess.get(i);
        out.append(rule.getSelector()).append(" { ").append(rule.getDeclarations()).append(" }\n");
      }
    } else {
      final List<WCssRule> toProcess = this.rulesAdded_;
      for (int i = 0; i < toProcess.size(); ++i) {
        final WCssRule rule = toProcess.get(i);
        out.append(rule.getSelector()).append(" { ").append(rule.getDeclarations()).append(" }\n");
      }
    }
    this.rulesAdded_.clear();
    if (all) {
      this.rulesModified_.clear();
    }
  }

  public void javaScriptUpdate(WApplication app, final StringBuilder js, boolean all) {
    if (!all) {
      for (int i = 0; i < this.rulesRemoved_.size(); ++i) {
        js.append("Wt4.11.2.removeCssRule(");
        DomElement.jsStringLiteral(js, this.rulesRemoved_.get(i), '\'');
        js.append(");");
      }
      this.rulesRemoved_.clear();
      for (Iterator<WCssRule> i_it = this.rulesModified_.iterator(); i_it.hasNext(); ) {
        WCssRule i = i_it.next();
        js.append("{ var d= Wt4.11.2.getCssRule(");
        DomElement.jsStringLiteral(js, i.getSelector(), '\'');
        js.append(");if(d){");
        DomElement d = DomElement.updateGiven("d", DomElementType.SPAN);
        if (i.updateDomElement(d, false)) {
          EscapeOStream sout = new EscapeOStream(js);
          d.asJavaScript(sout, DomElement.Priority.Update);
        }

        js.append("}}");
      }
      this.rulesModified_.clear();
    }
    if (!app.getEnvironment().agentIsIElt(9)
        && app.getEnvironment().getAgent() != UserAgent.Konqueror) {
      if (all) {
        final List<WCssRule> toProcess = this.rules_;
        for (int i = 0; i < toProcess.size(); ++i) {
          WCssRule rule = toProcess.get(i);
          js.append("Wt4.11.2.addCss('").append(rule.getSelector()).append("',");
          DomElement.jsStringLiteral(js, rule.getDeclarations(), '\'');
          js.append(");\n");
        }
      } else {
        final List<WCssRule> toProcess = this.rulesAdded_;
        for (int i = 0; i < toProcess.size(); ++i) {
          final WCssRule rule = toProcess.get(i);
          js.append("Wt4.11.2.addCss('").append(rule.getSelector()).append("',");
          DomElement.jsStringLiteral(js, rule.getDeclarations(), '\'');
          js.append(");\n");
        }
      }
      this.rulesAdded_.clear();
      if (all) {
        this.rulesModified_.clear();
      }
    } else {
      StringBuilder css = new StringBuilder();
      this.cssText(css, all);
      if (!(css.length() == 0)) {
        js.append("Wt4.11.2.addCssText(");
        DomElement.jsStringLiteral(js, css.toString(), '\'');
        js.append(");\n");
      }
    }
  }

  private List<WCssRule> rules_;
  private List<WCssRule> rulesAdded_;
  private Set<WCssRule> rulesModified_;
  private List<String> rulesRemoved_;
  private Set<String> defined_;

  boolean isDirty() {
    return !this.rulesAdded_.isEmpty()
        || !this.rulesModified_.isEmpty()
        || !this.rulesRemoved_.isEmpty();
  }
}
