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
 * Abstract baseclass for native media elements.
 *
 * <p>This class is an abstract base class for HTML5 media elements (&lt;audio&gt;, &lt;video&gt;).
 */
public abstract class WAbstractMedia extends WInteractWidget {
  private static Logger logger = LoggerFactory.getLogger(WAbstractMedia.class);

  /**
   * Consctructor for a media widget.
   *
   * <p>A freshly constructed media widget has no options set, no media sources, and has preload
   * mode set to PreloadAuto.
   */
  public WAbstractMedia(WContainerWidget parentContainer) {
    super();
    this.sources_ = new ArrayList<WAbstractMedia.Source>();
    this.sourcesRendered_ = 0;
    this.mediaId_ = "";
    this.flags_ = EnumSet.noneOf(PlayerOption.class);
    this.preloadMode_ = MediaPreloadMode.Auto;
    this.alternative_ = null;
    this.flagsChanged_ = false;
    this.preloadChanged_ = false;
    this.sourcesChanged_ = false;
    this.playing_ = false;
    this.volume_ = -1;
    this.current_ = -1;
    this.duration_ = -1;
    this.ended_ = false;
    this.readyState_ = MediaReadyState.HaveNothing;
    this.setInline(false);
    this.setFormObject(true);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Consctructor for a media widget.
   *
   * <p>Calls {@link #WAbstractMedia(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WAbstractMedia() {
    this((WContainerWidget) null);
  }

  public void remove() {
    {
      WWidget oldWidget = this.alternative_;
      this.alternative_ = null;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.alternative_);
        if (toRemove != null) toRemove.remove();
      }
    }
    super.remove();
  }
  /** Set the media element options. */
  public void setOptions(final EnumSet<PlayerOption> flags) {
    this.flags_ = EnumSet.copyOf(flags);
    this.flagsChanged_ = true;
    this.repaint();
  }
  /**
   * Set the media element options.
   *
   * <p>Calls {@link #setOptions(EnumSet flags) setOptions(EnumSet.of(flag, flags))}
   */
  public final void setOptions(PlayerOption flag, PlayerOption... flags) {
    setOptions(EnumSet.of(flag, flags));
  }
  /** Retrieve the configured options. */
  public EnumSet<PlayerOption> getOptions() {
    return this.flags_;
  }
  /** Set the preload mode. */
  public void setPreloadMode(MediaPreloadMode mode) {
    this.preloadMode_ = mode;
    this.preloadChanged_ = true;
    this.repaint();
  }
  /** Retrieve the preload mode. */
  public MediaPreloadMode getPreloadMode() {
    return this.preloadMode_;
  }
  /**
   * Removes all source elements.
   *
   * <p>This method can be used to remove all media sources. Afterward, you may add new media
   * sources with calls to {@link WAbstractMedia#addSource(WLink link, String type, String media)
   * addSource()}.
   *
   * <p>Use this to reuse a {@link WAbstractMedia} instantiation to play something else.
   */
  public void clearSources() {
    this.sources_.clear();
    this.repaint();
  }
  /**
   * Add a media source.
   *
   * <p>This method specifies a media source (which may be a URL or dynamic resource). You may add
   * as many media sources as you want. The browser will select the appropriate media stream to
   * display to the user.
   *
   * <p>This method specifies a media source using the URL, the mime type, and the media attribute.
   * HTML allows for empty type and media attributes.
   */
  public void addSource(final WLink link, final String type, final String media) {
    this.sources_.add(new WAbstractMedia.Source(this, link, type, media));
    this.sourcesChanged_ = true;
    this.repaint();
  }
  /**
   * Add a media source.
   *
   * <p>Calls {@link #addSource(WLink link, String type, String media) addSource(link, "", "")}
   */
  public final void addSource(final WLink link) {
    addSource(link, "", "");
  }
  /**
   * Add a media source.
   *
   * <p>Calls {@link #addSource(WLink link, String type, String media) addSource(link, type, "")}
   */
  public final void addSource(final WLink link, final String type) {
    addSource(link, type, "");
  }
  /**
   * Content to be shown when media cannot be played.
   *
   * <p>As not all browsers are HTML5 compliant, it is a good idea to provide fallback options when
   * the media cannot be displayed. If the media can be played by the browser, the alternative
   * content will be suppressed.
   *
   * <p>The two reasons to display the alternative content are (1) the media tag is not supported,
   * or (2) the media tag is supported, but none of the media sources are supported by the browser.
   * In the first case, fall-back is automatic and does not rely on JavaScript in the browser; in
   * the latter case, JavaScript is required to make the fallback work.
   *
   * <p>The alternative content can be any widget: you can set it to an alternative media player
   * (QuickTime, Flash, ...), show a Flash movie, an animated gif, a text, a poster image, ...
   */
  public void setAlternativeContent(WWidget alternative) {
    {
      WWidget oldWidget = this.alternative_;
      this.alternative_ = alternative;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.alternative_);
        if (toRemove != null) toRemove.remove();
      }
    }
  }
  /**
   * Invoke {@link WAbstractMedia#play() play()} on the media element.
   *
   * <p>JavaScript must be available for this function to work.
   */
  public void play() {
    this.loadJavaScript();
    this.doJavaScript(this.getJsRef() + ".wtObj.play();");
  }
  /**
   * Invoke {@link WAbstractMedia#pause() pause()} on the media element.
   *
   * <p>JavaScript must be available for this function to work.
   */
  public void pause() {
    this.loadJavaScript();
    this.doJavaScript(this.getJsRef() + ".wtObj.pause();");
  }
  /** Returns whether the media is playing. */
  public boolean isPlaying() {
    return this.playing_;
  }
  /** Returns the media&apos;s readyState. */
  public MediaReadyState getReadyState() {
    return this.readyState_;
  }
  /**
   * Event signal emitted when playback has begun.
   *
   * <p>This event fires when play was invoked, or when the media element starts playing because the
   * Autoplay option was provided.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal playbackStarted() {
    return this.voidEventSignal(PLAYBACKSTARTED_SIGNAL, true);
  }
  /**
   * Event signal emitted when the playback has paused.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal playbackPaused() {
    return this.voidEventSignal(PLAYBACKPAUSED_SIGNAL, true);
  }
  /**
   * Event signal emitted when the playback stopped because the end of the media was reached.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal ended() {
    return this.voidEventSignal(ENDED_SIGNAL, true);
  }
  /**
   * Event signal emitted when the current playback position has changed.
   *
   * <p>This event is fired when the playback position has changed, both when the media is in a
   * normal playing mode, but also when it has changed discontinuously because of another reason.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal timeUpdated() {
    return this.voidEventSignal(TIMEUPDATED_SIGNAL, true);
  }
  /**
   * Event signal emitted when the playback volume has changed.
   *
   * <p>
   *
   * <p><i><b>Note: </b>When JavaScript is disabled, the signal will never fire. </i>
   */
  public EventSignal volumeChanged() {
    return this.voidEventSignal(VOLUMECHANGED_SIGNAL, true);
  }
  /**
   * Returns the JavaScript reference to the media object, or null.
   *
   * <p>It is possible, for browser compatibility reasons, that {@link WWidget#getJsRef()} is not
   * the media element. {@link WAbstractMedia#getJsMediaRef() getJsMediaRef()} is guaranteed to be
   * an expression that evaluates to the media object. This expression may yield null, if the video
   * object is not rendered at all (e.g. on older versions of Internet Explorer).
   */
  public String getJsMediaRef() {
    if (this.mediaId_.length() == 0) {
      return "null";
    } else {
      return "Wt4.11.2.getElement('" + this.mediaId_ + "')";
    }
  }

  protected void getDomChanges(final List<DomElement> result, WApplication app) {
    if (this.mediaId_.length() != 0) {
      DomElement media = DomElement.getForUpdate(this.mediaId_, DomElementType.DIV);
      this.updateMediaDom(media, false);
      if (this.sourcesChanged_) {
        for (int i = 0; i < this.sourcesRendered_; ++i) {
          media.callJavaScript(
              "Wt4.11.2.remove('" + this.mediaId_ + "s" + String.valueOf(i) + "');", true);
        }
        this.sourcesRendered_ = 0;
        for (int i = 0; i < this.sources_.size(); ++i) {
          DomElement src = DomElement.createNew(DomElementType.SOURCE);
          src.setId(this.mediaId_ + "s" + String.valueOf(i));
          this.renderSource(src, this.sources_.get(i), i + 1 >= this.sources_.size());
          media.addChild(src);
        }
        this.sourcesRendered_ = this.sources_.size();
        this.sourcesChanged_ = false;
        media.callJavaScript(this.getJsMediaRef() + ".load();");
      }
      result.add(media);
    }
    super.getDomChanges(result, app);
  }

  protected DomElement createDomElement(WApplication app) {
    this.loadJavaScript();
    DomElement result = null;
    if (this.isInLayout()) {
      this.setJavaScriptMember(WT_RESIZE_JS, "function() {}");
    }
    if (app.getEnvironment().agentIsIElt(9)) {
      result = DomElement.createNew(DomElementType.DIV);
      if (this.alternative_ != null) {
        result.addChild(this.alternative_.createSDomElement(app));
      }
    } else {
      DomElement media = this.createMediaDomElement();
      DomElement wrap = null;
      if (this.isInLayout()) {
        media.setProperty(Property.StylePosition, "absolute");
        media.setProperty(Property.StyleLeft, "0");
        media.setProperty(Property.StyleRight, "0");
        wrap = DomElement.createNew(DomElementType.DIV);
        wrap.setProperty(Property.StylePosition, "relative");
      }
      result = wrap != null ? wrap : media;
      if (wrap != null) {
        this.mediaId_ = this.getId() + "_media";
        media.setId(this.mediaId_);
      } else {
        this.mediaId_ = this.getId();
      }
      this.updateMediaDom(media, true);
      for (int i = 0; i < this.sources_.size(); ++i) {
        DomElement src = DomElement.createNew(DomElementType.SOURCE);
        src.setId(this.mediaId_ + "s" + String.valueOf(i));
        this.renderSource(src, this.sources_.get(i), i + 1 >= this.sources_.size());
        media.addChild(src);
      }
      this.sourcesRendered_ = this.sources_.size();
      this.sourcesChanged_ = false;
      if (wrap != null) {
        wrap.addChild(media);
      }
    }
    if (this.isInLayout()) {
      StringWriter ss = new StringWriter();
      ss.append("function(self, w, h) {");
      if (this.mediaId_.length() != 0) {
        ss.append(
            "v="
                + this.getJsMediaRef()
                + ";if (v) {if (w >= 0) v.setAttribute('width', w);if (h >= 0) v.setAttribute('height', h);}");
      }
      if (this.alternative_ != null) {
        ss.append("a=" + this.alternative_.getJsRef() + ";if (a && a.")
            .append(WT_RESIZE_JS)
            .append(")a.")
            .append(WT_RESIZE_JS)
            .append("(a, w, h);");
      }
      ss.append("}");
      this.setJavaScriptMember(WT_RESIZE_JS, ss.toString());
    }
    this.setId(result, app);
    this.updateDom(result, true);
    if (this.isInLayout()) {
      result.setEvent(PLAYBACKSTARTED_SIGNAL, "");
      result.setEvent(PLAYBACKPAUSED_SIGNAL, "");
      result.setEvent(ENDED_SIGNAL, "");
      result.setEvent(TIMEUPDATED_SIGNAL, "");
      result.setEvent(VOLUMECHANGED_SIGNAL, "");
    }
    this.setJavaScriptMember("mediaId", "'" + this.mediaId_ + "'");
    return result;
  }

  protected void iterateChildren(final HandleWidgetMethod method) {
    if (this.alternative_ != null) {
      method.handle(this.alternative_);
    }
  }

  void updateMediaDom(final DomElement element, boolean all) {
    if (all && this.alternative_ != null) {
      element.setAttribute(
          "onerror",
          "if(event.target.error && event.target.error.code==event.target.error.MEDIA_ERR_SRC_NOT_SUPPORTED){while (this.hasChildNodes())if (Wt4.11.2.hasTag(this.firstChild,'SOURCE')){this.removeChild(this.firstChild);}else{this.parentNode.insertBefore(this.firstChild, this);}this.style.display= 'none';}");
    }
    if (all || this.flagsChanged_) {
      if (!all || this.flags_.contains(PlayerOption.Controls)) {
        element.setAttribute(
            "controls", this.flags_.contains(PlayerOption.Controls) ? "controls" : "");
      }
      if (!all || this.flags_.contains(PlayerOption.Autoplay)) {
        element.setAttribute(
            "autoplay", this.flags_.contains(PlayerOption.Autoplay) ? "autoplay" : "");
      }
      if (!all || this.flags_.contains(PlayerOption.Loop)) {
        element.setAttribute("loop", this.flags_.contains(PlayerOption.Loop) ? "loop" : "");
      }
    }
    if (all || this.preloadChanged_) {
      switch (this.preloadMode_) {
        case None:
          element.setAttribute("preload", "none");
          break;
        default:
        case Auto:
          element.setAttribute("preload", "auto");
          break;
        case Metadata:
          element.setAttribute("preload", "metadata");
          break;
      }
    }
    this.updateEventSignals(element, all);
    if (all) {
      if (this.alternative_ != null) {
        element.addChild(this.alternative_.createSDomElement(WApplication.getInstance()));
      }
    }
    this.flagsChanged_ = this.preloadChanged_ = false;
  }

  abstract DomElement createMediaDomElement();

  protected void setFormData(final WObject.FormData formData) {
    if (!(formData.values.length == 0)) {
      List<String> attributes = new ArrayList<String>();
      StringUtils.split(attributes, formData.values[0], ";", false);
      if (attributes.size() == 6) {
        try {
          this.volume_ = Double.parseDouble(attributes.get(0));
        } catch (final RuntimeException e) {
          this.volume_ = -1;
        }
        try {
          this.current_ = Double.parseDouble(attributes.get(1));
        } catch (final RuntimeException e) {
          this.current_ = -1;
        }
        try {
          this.duration_ = Double.parseDouble(attributes.get(2));
        } catch (final RuntimeException e) {
          this.duration_ = -1;
        }
        this.playing_ = attributes.get(3).equals("0");
        this.ended_ = attributes.get(4).equals("1");
        try {
          this.readyState_ = intToReadyState(Integer.parseInt(attributes.get(5)));
        } catch (final RuntimeException e) {
          this.readyState_ = MediaReadyState.HaveNothing;
        }
      } else {
        throw new WException("WAbstractMedia: error parsing: " + formData.values[0]);
      }
    }
  }

  protected void enableAjax() {
    super.enableAjax();
    if (this.flags_.contains(PlayerOption.Autoplay)) {
      this.play();
    }
  }

  static class Source extends WObject {
    private static Logger logger = LoggerFactory.getLogger(Source.class);

    public Source(WAbstractMedia parent, final WLink link, final String type, final String media) {
      super();
      this.parent = parent;
      this.connection = new AbstractSignal.Connection();
      this.type = type;
      this.media = media;
      this.link = link;
      if (link.getType() == LinkType.Resource) {
        this.connection =
            link.getResource()
                .dataChanged()
                .addListener(
                    this,
                    () -> {
                      WAbstractMedia.Source.this.resourceChanged();
                    });
      }
    }

    public void resourceChanged() {
      this.parent.sourcesChanged_ = true;
      this.parent.repaint();
    }

    public WAbstractMedia parent;
    public AbstractSignal.Connection connection;
    public String type;
    public String media;
    public WLink link;
  }

  private void renderSource(
      DomElement element, final WAbstractMedia.Source source, boolean isLast) {
    element.setAttribute("src", resolveRelativeUrl(source.link.getUrl()));
    if (!source.type.equals("")) {
      element.setAttribute("type", source.type);
    }
    if (!source.media.equals("")) {
      element.setAttribute("media", source.media);
    }
    if (isLast && this.alternative_ != null) {
      element.setAttribute(
          "onerror",
          "var media = this.parentNode;if(media){while (media && media.children.length)if (Wt4.11.2.hasTag(media.firstChild,'SOURCE')){media.removeChild(media.firstChild);}else{media.parentNode.insertBefore(media.firstChild, media);}media.style.display= 'none';}");
    } else {
      element.setAttribute("onerror", "");
    }
  }

  private List<WAbstractMedia.Source> sources_;
  private int sourcesRendered_;
  private String mediaId_;
  EnumSet<PlayerOption> flags_;
  private MediaPreloadMode preloadMode_;
  private WWidget alternative_;
  private boolean flagsChanged_;
  private boolean preloadChanged_;
  private boolean sourcesChanged_;
  private boolean playing_;
  private double volume_;
  private double current_;
  private double duration_;
  private boolean ended_;
  private MediaReadyState readyState_;
  private static String PLAYBACKSTARTED_SIGNAL = "play";
  private static String PLAYBACKPAUSED_SIGNAL = "pause";
  private static String ENDED_SIGNAL = "ended";
  private static String TIMEUPDATED_SIGNAL = "timeupdate";
  private static String VOLUMECHANGED_SIGNAL = "volumechange";

  private void loadJavaScript() {
    if (this.getJavaScriptMember(" WAbstractMedia").length() == 0) {
      WApplication app = WApplication.getInstance();
      app.loadJavaScript("js/WAbstractMedia.js", wtjs1());
      this.setJavaScriptMember(
          " WAbstractMedia",
          "new Wt4.11.2.WAbstractMedia(" + app.getJavaScriptClass() + "," + this.getJsRef() + ");");
    }
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WAbstractMedia",
        "(function(t,e){e.wtObj=this;const a=t.WT;this.play=function(){if(e.mediaId){const t=a.$(e.mediaId);if(t){t.play();return}}if(e.alternativeId){const t=a.$(e.alternativeId);t&&t.WtPlay&&t.WtPlay()}};this.pause=function(){if(e.mediaId){const t=a.$(e.mediaId);if(t){t.pause();return}}if(e.alternativeId){const t=a.$(e.alternativeId);t&&t.WtPlay&&t.WtPause()}};e.wtEncodeValue=function(){if(e.mediaId){const t=a.$(e.mediaId);if(t)return t.volume+\";\"+t.currentTime+\";\"+(t.readyState>=1?t.duration:0)+\";\"+(t.paused?\"1\":\"0\")+\";\"+(t.ended?\" 1\":\"0\")+\";\"+t.readyState}return null}})");
  }

  static MediaReadyState intToReadyState(int i) {
    switch (i) {
      case 0:
        return MediaReadyState.HaveNothing;
      case 1:
        return MediaReadyState.HaveMetaData;
      case 2:
        return MediaReadyState.HaveCurrentData;
      case 3:
        return MediaReadyState.HaveFutureData;
      case 4:
        return MediaReadyState.HaveEnoughData;
      default:
        throw new WException("Invalid readystate");
    }
  }
}
