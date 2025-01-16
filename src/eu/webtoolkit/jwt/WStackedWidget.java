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
 * A container widget that stacks its widgets on top of each other.
 *
 * <p>This is a container widget which at all times has only one item visible. The widget
 * accomplishes this using setHidden(bool) on the children.
 *
 * <p>Using {@link WStackedWidget#getCurrentIndex() getCurrentIndex()} and {@link
 * WStackedWidget#setCurrentIndex(int index) setCurrentIndex()} you can retrieve or set the visible
 * widget.
 *
 * <p>When calling the above <code>
 * {@link WStackedWidget#setCurrentIndex(int index) setCurrentIndex()}</code>, this will fire the
 * {@link WStackedWidget#currentWidgetChanged() currentWidgetChanged()} signal. This allows
 * developers to know when the current visible widget has changed and what the new visible widget
 * is.
 *
 * <p>WStackedWidget, like {@link WContainerWidget}, is by default not inline.
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>The widget is rendered using an HTML <code>&lt;div&gt;</code> tag and does not provide
 * styling. It can be styled using inline or external CSS as appropriate.
 *
 * <p>
 *
 * @see WMenu
 */
public class WStackedWidget extends WContainerWidget {
  private static Logger logger = LoggerFactory.getLogger(WStackedWidget.class);

  /** Creates a new stack. */
  public WStackedWidget(WContainerWidget parentContainer) {
    super();
    this.animation_ = new WAnimation();
    this.autoReverseAnimation_ = false;
    this.currentIndex_ = -1;
    this.widgetsAdded_ = false;
    this.javaScriptDefined_ = false;
    this.loadAnimateJS_ = false;
    this.loadPolicies_ = new ArrayList<ContentLoading>();
    this.currentWidgetChanged_ = new Signal1<WWidget>();
    this.setOverflow(Overflow.Hidden);
    this.addStyleClass("Wt-stack");
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new stack.
   *
   * <p>Calls {@link #WStackedWidget(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WStackedWidget() {
    this((WContainerWidget) null);
  }

  public void addWidget(WWidget widget) {
    super.addWidget(widget);
    int index = this.getCount() - 1;
    this.loadPolicies_.add(0 + index, ContentLoading.Eager);
    if (this.currentIndex_ == -1) {
      this.currentIndex_ = 0;
    }
    this.widgetsAdded_ = true;
  }
  // public Widget  addWidget(<Woow... some pseudoinstantiation type!> widget) ;
  public WWidget removeWidget(WWidget widget) {
    int index = this.getIndexOf(widget);
    WWidget result = super.removeWidget(widget);
    this.loadPolicies_.remove(0 + index);
    if (this.currentIndex_ >= this.getCount()) {
      if (this.getCount() > 0) {
        this.setCurrentIndex(this.getCount() - 1);
      } else {
        this.currentIndex_ = -1;
      }
    }
    return result;
  }
  /**
   * Returns the index of the widget that is currently shown.
   *
   * <p>
   *
   * @see WStackedWidget#setCurrentIndex(int index)
   * @see WStackedWidget#getCurrentWidget()
   */
  public int getCurrentIndex() {
    return this.currentIndex_;
  }
  /**
   * Returns the widget that is currently shown.
   *
   * <p>
   *
   * @see WStackedWidget#setCurrentWidget(WWidget widget)
   * @see WStackedWidget#getCurrentIndex()
   */
  public WWidget getCurrentWidget() {
    if (this.currentIndex_ >= 0 && this.currentIndex_ < this.getCount()) {
      return this.getWidget(this.currentIndex_);
    } else {
      return null;
    }
  }
  /** Insert a widget at a given index. */
  public void insertWidget(int index, WWidget widget) {
    super.insertWidget(index, widget);
    this.loadPolicies_.add(0 + index, ContentLoading.Eager);
    if (this.currentIndex_ == -1) {
      this.currentIndex_ = 0;
    }
    this.widgetsAdded_ = true;
  }
  /**
   * Changes the current widget.
   *
   * <p>The widget with index <code>index</code> is made visible, while all other widgets are
   * hidden.
   *
   * <p>The change of current widget is done using the animation settings specified by {@link
   * WStackedWidget#setTransitionAnimation(WAnimation animation, boolean autoReverse)
   * setTransitionAnimation()}.
   *
   * <p>The default value for current index is 0 if there are child widgets, if no child widgets
   * were added this returns -1.
   *
   * <p>
   *
   * @see WStackedWidget#getCurrentIndex()
   * @see WStackedWidget#setCurrentWidget(WWidget widget)
   */
  public void setCurrentIndex(int index) {
    this.setCurrentIndex(index, this.animation_, this.autoReverseAnimation_);
  }
  /**
   * Changes the current widget using a custom animation.
   *
   * <p>
   *
   * @see WStackedWidget#getCurrentIndex()
   * @see WStackedWidget#setCurrentWidget(WWidget widget)
   */
  public void setCurrentIndex(int index, final WAnimation animation, boolean autoReverse) {
    if (!animation.isEmpty()
        && WApplication.getInstance().getEnvironment().supportsCss3Animations()
        && (this.isRendered() && this.javaScriptDefined_ || !canOptimizeUpdates())) {
      if (canOptimizeUpdates() && index == this.currentIndex_) {
        return;
      }
      this.loadAnimateJS();
      WWidget previous = this.getCurrentWidget();
      if (previous != null) {
        this.doJavaScript(this.getJsRef() + ".wtObj.adjustScroll(" + previous.getJsRef() + ");");
      }
      this.setJavaScriptMember("wtAutoReverse", autoReverse ? "true" : "false");
      if (previous != null) {
        previous.animateHide(animation);
      }
      this.getWidget(index).animateShow(animation);
      this.currentIndex_ = index;
    } else {
      this.currentIndex_ = index;
      for (int i = 0; i < this.getCount(); ++i) {
        if (!canOptimizeUpdates() || this.getWidget(i).isHidden() != (this.currentIndex_ != i)) {
          this.getWidget(i).setHidden(this.currentIndex_ != i);
        }
      }
      if (this.currentIndex_ >= 0 && this.isRendered() && this.javaScriptDefined_) {
        this.doJavaScript(
            this.getJsRef()
                + ".wtObj.setCurrent("
                + this.getWidget(this.currentIndex_).getJsRef()
                + ");");
      }
    }
    if (this.loadPolicies_.get(this.currentIndex_) == ContentLoading.Lazy) {
      WContainerWidget container =
          ObjectUtils.cast(this.getCurrentWidget(), WContainerWidget.class);
      if (container.getCount() != 0) {
        this.currentWidgetChanged().trigger(container.getWidget(0));
      }
    } else {
      this.currentWidgetChanged().trigger(this.getCurrentWidget());
    }
  }
  /**
   * Changes the current widget using a custom animation.
   *
   * <p>Calls {@link #setCurrentIndex(int index, WAnimation animation, boolean autoReverse)
   * setCurrentIndex(index, animation, true)}
   */
  public final void setCurrentIndex(int index, final WAnimation animation) {
    setCurrentIndex(index, animation, true);
  }
  /**
   * Changes the current widget.
   *
   * <p>The widget <code>widget</code>, which must have been added before, is made visible, while
   * all other widgets are hidden.
   *
   * <p>
   *
   * @see WStackedWidget#getCurrentWidget()
   * @see WStackedWidget#setCurrentIndex(int index)
   */
  public void setCurrentWidget(WWidget widget) {
    this.setCurrentIndex(this.getIndexOf(widget));
  }
  /**
   * Specifies an animation used during transitions.
   *
   * <p>The animation is used to hide the previously current widget and show the next current widget
   * using {@link WStackedWidget#setCurrentIndex(int index) setCurrentIndex()}.
   *
   * <p>The initial value for <code>animation</code> is WAnimation(), specifying no animation.
   *
   * <p>When <code>autoReverse</code> is set to <code>true</code>, then the reverse animation is
   * chosen when the new index precedes the current index. This only applies to {@link
   * AnimationEffect#SlideInFromLeft}, {@link AnimationEffect#SlideInFromRight}, {@link
   * AnimationEffect#SlideInFromTop} or {@link AnimationEffect#SlideInFromBottom} transition
   * effects.
   *
   * <p>
   *
   * <p><i><b>Note: </b>If you intend to use a transition animation with a {@link WStackedWidget}
   * you should set it before it is first rendered. Otherwise, transition animations caused by
   * {@link WStackedWidget#setCurrentIndex(int index) setCurrentIndex()} may not be correctly
   * performed. If you do want to force this change you can use {@link WApplication#processEvents()}
   * before calling {@link WStackedWidget#setCurrentIndex(int index) setCurrentIndex()}. </i>
   *
   * <p><i><b>Note: </b>It is also not supported to use a {@link AnimationEffect#Pop} animation on a
   * {@link WStackedWidget}. </i>
   *
   * @see WStackedWidget#setCurrentIndex(int index)
   */
  public void setTransitionAnimation(final WAnimation animation, boolean autoReverse) {
    if (WApplication.getInstance().getEnvironment().supportsCss3Animations()) {
      if (!animation.isEmpty()) {
        this.addStyleClass("Wt-animated");
      }
      this.animation_ = animation;
      this.autoReverseAnimation_ = autoReverse;
      this.loadAnimateJS();
    }
  }
  /**
   * Specifies an animation used during transitions.
   *
   * <p>Calls {@link #setTransitionAnimation(WAnimation animation, boolean autoReverse)
   * setTransitionAnimation(animation, false)}
   */
  public final void setTransitionAnimation(final WAnimation animation) {
    setTransitionAnimation(animation, false);
  }
  /**
   * Signal which indicates that the current widget was changed.
   *
   * <p>This signal is emitted when the current widget was changed. It holds a pointer to the new
   * current widget. It is emitted every time the {@link WStackedWidget#setCurrentIndex(int index)
   * setCurrentIndex()} or {@link WStackedWidget#setCurrentWidget(WWidget widget)
   * setCurrentWidget()} is called.
   */
  public Signal1<WWidget> currentWidgetChanged() {
    return this.currentWidgetChanged_;
  }

  protected DomElement createDomElement(WApplication app) {
    return super.createDomElement(app);
  }

  protected void getDomChanges(final List<DomElement> result, WApplication app) {
    super.getDomChanges(result, app);
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (this.widgetsAdded_ || flags.contains(RenderFlag.Full)) {
      for (int i = 0; i < this.getCount(); ++i) {
        if (!canOptimizeUpdates() || this.getWidget(i).isHidden() != (this.currentIndex_ != i)) {
          this.getWidget(i).setHidden(this.currentIndex_ != i);
        }
      }
      this.widgetsAdded_ = false;
    }
    if (flags.contains(RenderFlag.Full)) {
      this.defineJavaScript();
      if (this.currentIndex_ >= 0 && this.isRendered() && this.javaScriptDefined_) {
        this.doJavaScript(
            this.getJsRef()
                + ".wtObj.setCurrent("
                + this.getWidget(this.currentIndex_).getJsRef()
                + ");");
      }
    }
    super.render(flags);
  }

  private WAnimation animation_;
  private boolean autoReverseAnimation_;
  int currentIndex_;
  private boolean widgetsAdded_;
  private boolean javaScriptDefined_;
  private boolean loadAnimateJS_;
  List<ContentLoading> loadPolicies_;
  private Signal1<WWidget> currentWidgetChanged_;

  private void defineJavaScript() {
    if (!this.javaScriptDefined_) {
      this.javaScriptDefined_ = true;
      WApplication app = WApplication.getInstance();
      app.loadJavaScript("js/WStackedWidget.js", wtjs1());
      this.setJavaScriptMember(
          " WStackedWidget",
          "new Wt4.11.2.WStackedWidget(" + app.getJavaScriptClass() + "," + this.getJsRef() + ");");
      this.setJavaScriptMember(WT_RESIZE_JS, this.getJsRef() + ".wtObj.wtResize");
      this.setJavaScriptMember(WT_GETPS_JS, this.getJsRef() + ".wtObj.wtGetPs");
      if (this.loadAnimateJS_) {
        this.loadAnimateJS_ = false;
        this.loadAnimateJS();
      }
    }
  }

  private void loadAnimateJS() {
    if (!this.loadAnimateJS_) {
      this.loadAnimateJS_ = true;
      if (this.javaScriptDefined_) {
        WApplication.getInstance().loadJavaScript("js/WStackedWidget.js", wtjs2());
        this.setJavaScriptMember("wtAnimateChild", this.getJsRef() + ".wtObj.animateChild");
        this.setJavaScriptMember("wtAutoReverse", this.autoReverseAnimation_ ? "true" : "false");
      }
    }
  }

  void setLoadPolicy(int index, ContentLoading loadPolicy) {
    this.loadPolicies_.set(index, loadPolicy);
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WStackedWidget",
        "(function(t,e){e.wtObj=this;const i=t.WT,s=[],o=[];let n=null,l=null;function d(t){return 1===t.nodeType&&!t.classList.contains(\"wt-reparented\")&&!t.classList.contains(\"resize-sensor\")}this.reApplySize=function(){l&&this.wtResize(e,n,l,!1)};this.wtResize=function(t,e,s,o){n=e;l=s;const a=s>=0;if(o)if(a){t.style.height=s+\"px\";t.lh=!0}else{t.style.height=\"\";t.lh=!1}else t.lh=!1;if(i.boxSizing(t)){s-=i.px(t,\"marginTop\");s-=i.px(t,\"marginBottom\");s-=i.px(t,\"borderTopWidth\");s-=i.px(t,\"borderBottomWidth\");s-=i.px(t,\"paddingTop\");s-=i.px(t,\"paddingBottom\");e-=i.px(t,\"marginLeft\");e-=i.px(t,\"marginRight\");e-=i.px(t,\"borderLeftWidth\");e-=i.px(t,\"borderRightWidth\");e-=i.px(t,\"paddingLeft\");e-=i.px(t,\"paddingRight\")}function r(t){let e=i.px(t,\"marginTop\");e+=i.px(t,\"marginBottom\");if(!i.boxSizing(t)){e+=i.px(t,\"borderTopWidth\");e+=i.px(t,\"borderBottomWidth\");e+=i.px(t,\"paddingTop\");e+=i.px(t,\"paddingBottom\")}return e}for(const o of t.childNodes)if(d(o)&&!i.isHidden(o)&&!o.classList.contains(\"out\"))if(a){const t=s-r(o);if(t>0){if(o.offsetTop>0){const t=i.css(o,\"overflow\");\"visible\"!==t&&\"\"!==t||(o.style.overflow=\"auto\")}if(o.wtResize)o.wtResize(o,e,t,!0);else{const e=t+\"px\";if(o.style.height!==e){o.style.height=e;o.lh=!0}}}}else if(o.wtResize)o.wtResize(o,e,-1,!0);else{o.style.height=\"\";o.lh=!1}};this.wtGetPs=function(t,e,i,s){return s};this.adjustScroll=function(t){const i=e.scrollLeft,n=e.scrollTop;for(let l=0,a=e.childNodes.length;l<a;++l){const a=e.childNodes[l];if(d(a))if(a!==t){if(\"none\"!==a.style.display){o[l]=i;s[l]=n}}else if(void 0!==o[l]){e.scrollLeft=o[l];e.scrollTop=s[l]}else{e.scrollLeft=0;e.scrollTop=0}}};this.setCurrent=function(t){this.adjustScroll(t);for(let i=0,s=e.childNodes.length;i<s;++i){const s=e.childNodes[i];if(d(s))if(s!==t)\"none\"!==s.style.display&&(s.style.display=\"none\");else{s.style.flexFlow?s.style.display=\"flex\":s.style.display=\"\";if(e.lh){e.lh=!1;e.style.height=\"\"}}}this.reApplySize()}})");
  }

  static WJavaScriptPreamble wtjs2() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptPrototype,
        "WStackedWidget.prototype.animateChild",
        "(function(t,e,i,s,o,n){const l=function(t,e,i,s,o,n){const d=[\"ease\",\"linear\",\"ease-in\",\"ease-out\",\"ease-in-out\"],a=\"Webkit\"===t.vendorPrefix(t.styleAttribute(\"animation-duration\"))?\"webkitAnimationEnd\":\"animationend\";if(\"none\"===n.display)return;const r=e.parentNode,p=r.wtAutoReverse;const c=function(){let t,i=-1,s=-1;const o=r.childNodes.length;for(t=0;t<o&&(-1===i||-1===s);++t){const o=r.childNodes[t];o===e?s=t:\"none\"===o.style.display||o.classList.contains(\"out\")||(i=t)}return{from:i,to:s}}();if(-1===c.from||-1===c.to||c.from===c.to)return;const h=r.childNodes[c.from],f=r.childNodes[c.to];let y=r.scrollHeight,u=r.scrollWidth;if(h.classList.contains(\"in\")){h.addEventListener(a,(function(){l(t,e,i,s,1,n)}),{once:!0});return}if(f.classList.contains(\"out\")){h.addEventListener(a,(function(){l(t,e,i,s,1,n)}),{once:!0});return}y-=t.px(r,\"paddingTop\");y-=t.px(r,\"paddingBottom\");y-=t.px(f,\"marginTop\");y-=t.px(f,\"marginBottom\");y-=t.px(f,\"borderTopWidth\");y-=t.px(f,\"borderBottomWidth\");y-=t.px(f,\"paddingTop\");y-=t.px(f,\"paddingBottom\");u-=t.px(r,\"paddingLeft\");u-=t.px(r,\"paddingRight\");u-=t.px(f,\"marginLeft\");u-=t.px(f,\"marginRight\");u-=t.px(f,\"borderLeftWidth\");u-=t.px(f,\"borderRightWidth\");u-=t.px(f,\"paddingLeft\");u-=t.px(f,\"paddingRight\");f.style.left=h.style.left||t.px(r,\"paddingLeft\");f.style.top=h.style.top||t.px(r,\"paddingTop\");f.style.width=u+\"px\";f.style.height=y+\"px\";f.style.position=\"absolute\";t.isGecko&&256&i&&(f.style.opacity=\"0\");f.style.display=n.display;let g=p&&c.to<c.from,m=[];switch(255&i){case 1:g=!g;case 2:m=[\"slide\"];break;case 3:m=[\"slideup\"];break;case 4:m=[\"slidedown\"];break;case 5:m=[\"pop\"]}256&i&&m.push(\"fade\");g&&m.push(\"reverse\");h.style[t.styleAttribute(\"animation-duration\")]=o+\"ms\";f.style[t.styleAttribute(\"animation-duration\")]=o+\"ms\";h.style[t.styleAttribute(\"animation-timing-function\")]=d[[0,1,3,2,4,5][s]];f.style[t.styleAttribute(\"animation-timing-function\")]=d[s];h.classList.add(...m,\"out\");h.addEventListener(a,(function(){h.classList.remove(...m,\"out\");h.style.display=\"none\";if(r.lh&&f.lh){f.style.height=\"\";f.lh=!1}h.style[t.styleAttribute(\"animation-duration\")]=\"\";h.style[t.styleAttribute(\"animation-timing-function\")]=\"\"}),{once:!0});f.classList.add(...m,\"in\");f.addEventListener(a,(function(){f.classList.remove(...m,\"in\");f.style.position=\"\";f.style.left=\"\";f.style.width=\"\";f.style.top=\"\";r.lh?f.lh=!0:f.lh||(f.style.height=\"\");t.isGecko&&256&i&&(f.style.opacity=\"1\");f.style[t.styleAttribute(\"animation-duration\")]=\"\";f.style[t.styleAttribute(\"animation-timing-function\")]=\"\"}),{once:!0})};l(t,e,i,s,o,n)})");
  }
}
