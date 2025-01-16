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
 * A media player.
 *
 * <p>This widget implements a media player, suitable to play video or audio, and with a
 * customizable user-interface.
 *
 * <p>To support cross-browser playing of video or audio content, you may need to provide the
 * contents appropriately encoded. For audio, at least an MP3 or MP4 audio (M4A) encoding should be
 * supplied, while for video the M4V encoding should be provided. Additional encodings are
 * beneficial since they increase the chance that native HTML <code>&lt;video&gt;</code> or <code>
 * &lt;audio&gt;</code> elements can be used (which may be hardware accelerated), instead of the
 * flash player. See <a
 * href="http://www.jplayer.org/latest/developer-guide/#reference-html5-media">HTML5 browser media
 * support</a>.
 *
 * <p>You need to specify the encoding types you are going to use when instantiating the media
 * player, since based on the chosen encodings, a particular suitable implementation will be used.
 * Thus, you need to call {@link WMediaPlayer#addSource(MediaEncoding encoding, WLink link)
 * addSource()} immediately, but you may pass empty URLs if you do not yet want to load media.
 *
 * <p>The player provides a user-interface to control the playback which may be freely customized,
 * and which is independent of the underlying media technology (HTML video or Flash player). The
 * controls user-interface may be implemented as a JWt widget, where the controls (buttons, progress
 * bars, and text widgets) are bound directly to the video player component (client-side).
 *
 * <p>This widget relies on a third-party JavaScript component <a
 * href="http://www.jplayer.org/">jPlayer</a>, which is distributed together with JWt.
 *
 * <p>The default user-interface can be themed using jPlayer themes. The theme is global (it applies
 * to all media player instances), and is configured by loading a CSS stylesheet.
 *
 * <p>The following code creates a video using the default controls:
 *
 * <pre>{@code
 * ...
 *
 * }</pre>
 *
 * <p>Alternatively, a custom widget may be set which implements the controls, using {@link
 * WMediaPlayer#setControlsWidget(WWidget controlsWidget) setControlsWidget()}. In this case, you
 * should add to this widget the buttons, text place holders, and progress bars and bind them to the
 * media player using the {@link WMediaPlayer#setButton(MediaPlayerButtonId id, WInteractWidget w)
 * setButton()}, {@link WMediaPlayer#setText(MediaPlayerTextId id, WText w) setText()} and {@link
 * WMediaPlayer#setProgressBar(MediaPlayerProgressBarId id, WProgressBar w) setProgressBar()}
 * methods. The controls widget is integrated in the media player, and this has as unique benefit
 * (for a video player) that they may also be shown when the video player is maximized.
 *
 * <p>Finally, you may want to control the media player only through widgets external to the media
 * player. This may be configured by setting <code>null</code> as controlsWidget. In this case
 * however, full screen mode should not be used since there is no way to restore the original size.
 */
public class WMediaPlayer extends WCompositeWidget {
  private static Logger logger = LoggerFactory.getLogger(WMediaPlayer.class);

  /**
   * Creates a new media player.
   *
   * <p>The player is instantiated with default controls.
   *
   * <p>
   *
   * @see WMediaPlayer#setControlsWidget(WWidget controlsWidget)
   */
  public WMediaPlayer(MediaType mediaType, WContainerWidget parentContainer) {
    super();
    this.signals_ = new ArrayList<JSignal>();
    this.signalsDouble_ = new ArrayList<WMediaPlayer.SignalDouble>();
    this.mediaType_ = mediaType;
    this.videoWidth_ = 0;
    this.videoHeight_ = 0;
    this.title_ = new WString();
    this.media_ = new ArrayList<WMediaPlayer.Source>();
    this.initialJs_ = "";
    this.gui_ = this;
    this.boundSignals_ = 0;
    this.boundSignalsDouble_ = 0;
    this.mediaUpdated_ = false;
    this.status_ = new WMediaPlayer.State();
    for (int i = 0; i < 11; ++i) {
      this.control_[i] = (WInteractWidget) null;
    }
    for (int i = 0; i < 3; ++i) {
      this.display_[i] = null;
    }
    for (int i = 0; i < 2; ++i) {
      this.progressBar_[i] = null;
    }
    WTemplate impl = new WMediaPlayerImpl(this, tr("Wt.WMediaPlayer.template"));
    impl.bindEmpty("gui");
    this.setImplementation(impl);
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/WMediaPlayer.js", wtjs1());
    String res = WApplication.getRelativeResourcesUrl() + "jPlayer/";
    if (!app.isCustomJQuery()) {
      app.requireJQuery(res + "jquery.min.js");
    }
    if (app.require(res + "jquery.jplayer.min.js")) {
      app.useStyleSheet(new WLink(res + "skin/jplayer.blue.monday.css"));
    }
    if (this.mediaType_ == MediaType.Video) {
      this.setVideoSize(480, 270);
    }
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a new media player.
   *
   * <p>Calls {@link #WMediaPlayer(MediaType mediaType, WContainerWidget parentContainer)
   * this(mediaType, (WContainerWidget)null)}
   */
  public WMediaPlayer(MediaType mediaType) {
    this(mediaType, (WContainerWidget) null);
  }
  /** Destructor. */
  public void remove() {
    for (int i = 0; i < this.signals_.size(); ++i) {}

    for (int i = 0; i < this.signalsDouble_.size(); ++i) {}

    super.remove();
  }
  /**
   * Sets the video size.
   *
   * <p>This sets the size for the video. The actual size of the media player may be slightly
   * larger, if the controlWidget take additional space (i.e. is not overlayed on top of the video).
   *
   * <p>CSS Themes for the default jPlayer controls support two formats (480 x 270 and 640 x 360).
   *
   * <p>The default video size is 480 x 270.
   */
  public void setVideoSize(int width, int height) {
    if (width != this.videoWidth_ || height != this.videoHeight_) {
      this.videoWidth_ = width;
      this.videoHeight_ = height;
      this.setWidth(new WLength(this.videoWidth_));
      if (this.isRendered()) {
        StringBuilder ss = new StringBuilder();
        ss.append("'size', {")
            .append("width: \"")
            .append(this.videoWidth_)
            .append("px\",")
            .append("height: \"")
            .append(this.videoHeight_)
            .append("px\",")
            .append("cssClass: \"jp-video-")
            .append(this.videoHeight_)
            .append("p\"")
            .append("}");
        this.playerDo("option", ss.toString());
      }
    }
  }
  /**
   * Returns the video width.
   *
   * <p>
   *
   * @see WMediaPlayer#setVideoSize(int width, int height)
   */
  public int getVideoWidth() {
    return this.videoWidth_;
  }
  /**
   * Returns the video height.
   *
   * <p>
   *
   * @see WMediaPlayer#setVideoSize(int width, int height)
   */
  public int getVideoHeight() {
    return this.videoHeight_;
  }
  /**
   * Sets the user-interface controls widget.
   *
   * <p>This sets a widget that contains the controls (buttons, text widgets, etc...) to allow the
   * user to control the player.
   *
   * <p>Widgets that implement the buttons, bars, and text holders should be bound to the player
   * using {@link WMediaPlayer#setButton(MediaPlayerButtonId id, WInteractWidget w) setButton()},
   * {@link WMediaPlayer#setText(MediaPlayerTextId id, WText w) setText()} and {@link
   * WMediaPlayer#setProgressBar(MediaPlayerProgressBarId id, WProgressBar w) setProgressBar()}
   * calls.
   *
   * <p>Setting a <code>null</code> widget will result in a player without controls. For an audio
   * player this has the effect of being entirely invisible.
   *
   * <p>The default controls widget is a widget that can be styled using a jPlayer CSS theme.
   */
  public void setControlsWidget(WWidget controlsWidget) {
    this.gui_ = controlsWidget;
    WTemplate impl = ObjectUtils.cast(this.getImplementation(), WTemplate.class);
    if (controlsWidget != null) {
      controlsWidget.addStyleClass("jp-gui");
      impl.bindWidget("gui", controlsWidget);
    } else {
      impl.bindEmpty("gui");
    }
  }
  /**
   * Returns the user-interface controls widget.
   *
   * <p>
   *
   * @see WMediaPlayer#setControlsWidget(WWidget controlsWidget)
   */
  public WWidget getControlsWidget() {
    if (this.gui_ == this) {
      (this).createDefaultGui();
    }
    return this.gui_;
  }
  /**
   * Sets the media title.
   *
   * <p>
   *
   * @see MediaPlayerTextId#Title
   */
  public void setTitle(final CharSequence title) {
    this.title_ = WString.toWString(title);
    if (this.display_[(int) MediaPlayerTextId.Title.getValue()] != null) {
      this.display_[(int) MediaPlayerTextId.Title.getValue()].setText(this.title_);
      if (this.gui_ != null) {
        WTemplate t = ObjectUtils.cast(this.gui_, WTemplate.class);
        if (t != null) {
          t.bindString("title-display", (this.title_.length() == 0) ? "none" : "");
        }
      }
    }
  }
  /**
   * Adds a source.
   *
   * <p>Adds a media source. The source may be specified as a URL or as a dynamic resource.
   *
   * <p>You may pass a null <code>link</code> if you want to indicate the media types you will use
   * (later) without already loading data.
   */
  public void addSource(MediaEncoding encoding, final WLink link) {
    this.media_.add(new WMediaPlayer.Source());
    this.media_.get(this.media_.size() - 1).link = link;
    this.media_.get(this.media_.size() - 1).encoding = encoding;
    this.mediaUpdated_ = true;
    this.scheduleRender();
  }
  /**
   * Returns a source.
   *
   * <p>Returns the media source for the given <code>encoding</code>, which must have previously
   * been added using {@link WMediaPlayer#addSource(MediaEncoding encoding, WLink link)
   * addSource()}.
   */
  public WLink getSource(MediaEncoding encoding) {
    for (int i = 0; i < this.media_.size(); ++i) {
      if (this.media_.get(i).encoding == encoding) {
        return this.media_.get(i).link;
      }
    }
    return new WLink("");
  }
  /**
   * Clears all sources.
   *
   * <p>
   *
   * @see WMediaPlayer#addSource(MediaEncoding encoding, WLink link)
   */
  public void clearSources() {
    this.media_.clear();
    this.mediaUpdated_ = true;
    this.scheduleRender();
  }
  /**
   * Binds a control button.
   *
   * <p>A control button is typically implemented as a {@link WAnchor} or a {@link WPushButton}
   * (although any {@link WInteractWidget} can work).
   *
   * <p>You should use this method in conjunction with {@link WMediaPlayer#setControlsWidget(WWidget
   * controlsWidget) setControlsWidget()} to bind buttons in a custom control interface to media
   * player functions.
   *
   * <p>The default control widget implements all buttons using a {@link WAnchor}.
   */
  public void setButton(MediaPlayerButtonId id, WInteractWidget w) {
    if (this.control_[(int) id.getValue()] != null) {
      {
        WWidget toRemove =
            WidgetUtils.remove(
                this.control_[(int) id.getValue()].getParent(), this.control_[(int) id.getValue()]);
        if (toRemove != null) toRemove.remove();
      }
    }
    this.control_[(int) id.getValue()] = w;
  }
  /**
   * Returns a control button.
   *
   * <p>
   *
   * @see WMediaPlayer#setButton(MediaPlayerButtonId id, WInteractWidget w)
   */
  public WInteractWidget getButton(MediaPlayerButtonId id) {
    this.getControlsWidget();
    return this.control_[(int) id.getValue()];
  }
  /**
   * Binds a control progress bar.
   *
   * <p>The progress bar for the {@link MediaPlayerProgressBarId#Time} indication should be
   * contained in a {@link WContainerWidget} which bounds the width of the progress bar, rather than
   * setting a width on the progress bar. This is because the progress bar may, in some cases, also
   * be used to indicate which part of the media can be seeked, and for this its width is being
   * manipulated.
   *
   * <p>You should use this method in conjunction with {@link WMediaPlayer#setControlsWidget(WWidget
   * controlsWidget) setControlsWidget()} to bind progress bars in a custom control interface to
   * media player functions.
   */
  public void setProgressBar(MediaPlayerProgressBarId id, WProgressBar w) {
    final MediaPlayerProgressBarId bc_id = id;
    {
      WProgressBar toRemove = this.progressBar_[(int) id.getValue()];
      if (toRemove != null) toRemove.remove();
    }

    this.progressBar_[(int) id.getValue()] = w;
    if (w != null) {
      w.setFormat(WString.Empty);
      w.valueChanged()
          .addListener(
              this,
              (Double event) -> {
                WMediaPlayer.this.updateFromProgressBar(bc_id, event);
              });
      this.updateProgressBarState(id);
    }
  }
  /**
   * Returns a control progress bar.
   *
   * <p>
   *
   * @see WMediaPlayer#setProgressBar(MediaPlayerProgressBarId id, WProgressBar w)
   */
  public WProgressBar getProgressBar(MediaPlayerProgressBarId id) {
    this.getControlsWidget();
    return this.progressBar_[(int) id.getValue()];
  }
  /**
   * Sets a text place-holder widget.
   *
   * <p>This binds the widget that displays text such as current time and total duration of the
   * loaded media.
   *
   * <p>You should use this method in conjunction with {@link WMediaPlayer#setControlsWidget(WWidget
   * controlsWidget) setControlsWidget()} to bind progress bars in a custom control interface to
   * media player functions.
   */
  public void setText(MediaPlayerTextId id, WText w) {
    {
      WText toRemove = this.display_[(int) id.getValue()];
      if (toRemove != null) toRemove.remove();
    }

    this.display_[(int) id.getValue()] = w;
    if (id == MediaPlayerTextId.Title && w != null) {
      w.setText(this.title_);
    }
  }
  /**
   * Returns a text place-holder widget.
   *
   * <p>
   *
   * @see WMediaPlayer#setText(MediaPlayerTextId id, WText w)
   */
  public WText getText(MediaPlayerTextId id) {
    this.getControlsWidget();
    return this.display_[(int) id.getValue()];
  }
  /**
   * Pauses the player.
   *
   * <p>
   *
   * @see WMediaPlayer#play()
   */
  public void pause() {
    this.playerDo("pause");
  }
  /**
   * Start or resume playing.
   *
   * <p>The player starts or resumes playing at the current time.
   *
   * <p>
   *
   * @see WMediaPlayer#seek(double time)
   */
  public void play() {
    if (this.isRendered()) {
      this.doJavaScript(
          "setTimeout(function(){" + this.getJsPlayerRef() + ".jPlayer('play'); }, 0);");
    } else {
      this.playerDo("play");
    }
  }
  /**
   * Stops the player.
   *
   * <p>
   *
   * @see WMediaPlayer#play()
   */
  public void stop() {
    this.playerDo("stop");
  }
  /**
   * Seeks to a time.
   *
   * <p>If possible, the player sets the current time to the indicated <code>time</code> (expressed
   * in seconds).
   *
   * <p>
   *
   * <p><i><b>Note: </b>It may be the case that this only works after the player has already loaded
   * the media. </i>
   */
  public void seek(double time) {
    if (this.status_.seekPercent != 0) {
      double pct = time / (this.status_.seekPercent * this.status_.duration / 100);
      pct = Math.min(1.0, pct);
      this.playerDo("playHead", String.valueOf(pct * 100));
    }
  }
  /**
   * Sets the playback rate.
   *
   * <p>This modifies the playback rate, expressed as a ratio of the normal (natural) playback rate.
   *
   * <p>The default value is 1.0
   *
   * <p>
   *
   * <p><i><b>Note: </b>Not all browsers support this function. </i>
   */
  public void setPlaybackRate(double rate) {
    if (rate != this.status_.playbackRate) {
      this.status_.playbackRate = rate;
      this.playerDoData("wtPlaybackRate", String.valueOf(rate));
    }
  }
  /**
   * Sets the volume.
   *
   * <p>This modifies the volume, which must be a number between 0 and 1.0.
   *
   * <p>The default value is 0.8
   */
  public void setVolume(double volume) {
    this.status_.volume = volume;
    this.playerDo("volume", String.valueOf(volume));
  }
  /**
   * Returns the volume.
   *
   * <p>
   *
   * @see WMediaPlayer#setVolume(double volume)
   */
  public double getVolume() {
    return this.status_.volume;
  }
  /**
   * Mutes or unmutes the playback volume.
   *
   * <p>
   *
   * @see WMediaPlayer#setVolume(double volume)
   */
  public void mute(boolean mute) {
    this.playerDo(mute ? "mute" : "unmute");
  }
  /**
   * Returns whether the media is currently playing.
   *
   * <p>
   *
   * @see WMediaPlayer#play()
   */
  public boolean isPlaying() {
    return this.status_.playing;
  }
  /**
   * Returns the current player state.
   *
   * <p>The state reflects in how far the media player has loaded the media, and has determined its
   * characteristics.
   */
  public MediaReadyState getReadyState() {
    return this.status_.readyState;
  }
  /**
   * Returns the duration.
   *
   * <p>The duration may be reported as 0 if the player has not yet loaded the media to determine
   * the duration. Otherwise the duration is the duration of the loaded media, expressed in seconds.
   *
   * <p>
   *
   * @see WMediaPlayer#getReadyState()
   * @see WMediaPlayer#getCurrentTime()
   */
  public double getDuration() {
    return this.status_.duration;
  }
  /**
   * Returns the current playback time.
   *
   * <p>Returns the current playback time, expressed in seconds.
   *
   * <p>
   *
   * @see WMediaPlayer#seek(double time)
   */
  public double getCurrentTime() {
    return this.status_.currentTime;
  }
  /**
   * Returns the current playback rate.
   *
   * <p>
   *
   * @see WMediaPlayer#setPlaybackRate(double rate)
   */
  public double getPlaybackRate() {
    return this.status_.playbackRate;
  }
  /**
   * Event that indicates a time update.
   *
   * <p>The event indicates that the {@link WMediaPlayer#getCurrentTime() getCurrentTime()} has
   * changed.
   */
  public JSignal1<Double> timeUpdated() {
    return this.signalDouble(
        TIME_UPDATED_SIGNAL, this.getJsPlayerRef() + ".data('jPlayer').status.currentTime");
  }
  /**
   * Event that indicates that playback started.
   *
   * <p>The event is fired when playback has started (or is being continued).
   */
  public JSignal playbackStarted() {
    return this.signal(PLAYBACK_STARTED_SIGNAL);
  }
  /**
   * Event that indicates that playback paused.
   *
   * <p>The event is fired when playback has been paused.
   */
  public JSignal playbackPaused() {
    return this.signal(PLAYBACK_PAUSED_SIGNAL);
  }
  /** Event that indicates that the video or audio has ended. */
  public JSignal ended() {
    return this.signal(ENDED_SIGNAL);
  }
  /** Event that indicates that the volume has changed. */
  public JSignal1<Double> volumeChanged() {
    return this.signalDouble(
        VOLUME_CHANGED_SIGNAL, this.getJsPlayerRef() + ".data('jPlayer').options.volume");
  }

  String getJsPlayerRef() {
    return "$('#" + this.getId() + " .jp-jplayer')";
  }

  public void refresh() {
    super.refresh();
    this.render(EnumSet.of(RenderFlag.Full));
  }

  protected void setFormData(final WObject.FormData formData) {
    if (!(formData.values.length == 0)) {
      List<String> attributes = new ArrayList<String>();
      StringUtils.split(attributes, formData.values[0], ";", false);
      if (attributes.size() == 8) {
        try {
          this.status_.volume = Double.parseDouble(attributes.get(0));
          this.status_.currentTime = Double.parseDouble(attributes.get(1));
          this.status_.duration = Double.parseDouble(attributes.get(2));
          this.status_.playing = attributes.get(3).equals("0");
          this.status_.ended = attributes.get(4).equals("1");
          this.status_.readyState = intToReadyState(Integer.parseInt(attributes.get(5)));
          this.status_.playbackRate = Double.parseDouble(attributes.get(6));
          this.status_.seekPercent = Double.parseDouble(attributes.get(7));
          this.updateProgressBarState(MediaPlayerProgressBarId.Time);
          this.updateProgressBarState(MediaPlayerProgressBarId.Volume);
        } catch (final RuntimeException e) {
          throw new WException(
              "WMediaPlayer: error parsing: " + formData.values[0] + ": " + e.toString());
        }
      } else {
        throw new WException("WMediaPlayer: error parsing: " + formData.values[0]);
      }
    }
  }

  protected void render(EnumSet<RenderFlag> flags) {
    WApplication app = WApplication.getInstance();
    if (this.mediaUpdated_ || flags.contains(RenderFlag.Full) && !this.media_.isEmpty()) {
      StringBuilder ss = new StringBuilder();
      ss.append('{');
      boolean first = true;
      for (int i = 0; i < this.media_.size(); ++i) {
        if (this.media_.get(i).link.isNull()) {
          continue;
        }
        if (!first) {
          ss.append(',');
        }
        String url = app.resolveRelativeUrl(this.media_.get(i).link.getUrl());
        ss.append(mediaNames[(int) this.media_.get(i).encoding.getValue()])
            .append(": ")
            .append(WWebWidget.jsStringLiteral(url));
        first = false;
      }
      ss.append('}');
      if (!!EnumUtils.mask(flags, RenderFlag.Full).isEmpty()) {
        this.playerDo("setMedia", ss.toString());
      } else {
        this.initialJs_ = ".jPlayer('setMedia', " + ss.toString() + ')' + this.initialJs_;
      }
      this.mediaUpdated_ = false;
    }
    if (flags.contains(RenderFlag.Full)) {
      if (this.gui_ == this) {
        this.createDefaultGui();
      }
      StringBuilder ss = new StringBuilder();
      ss.append(this.getJsPlayerRef()).append(".jPlayer({").append("ready: function () {");
      if (this.initialJs_.length() != 0) {
        ss.append("$(this)").append(this.initialJs_).append(';');
      }
      this.initialJs_ = "";
      ss.append("},")
          .append("swfPath: \"")
          .append(WApplication.getResourcesUrl())
          .append("jPlayer\",")
          .append("supplied: \"");
      boolean first = true;
      for (int i = 0; i < this.media_.size(); ++i) {
        if (this.media_.get(i).encoding != MediaEncoding.PosterImage) {
          if (!first) {
            ss.append(',');
          }
          ss.append(mediaNames[(int) this.media_.get(i).encoding.getValue()]);
          first = false;
        }
      }
      ss.append("\",");
      if (this.mediaType_ == MediaType.Video) {
        ss.append("size: {")
            .append("width: \"")
            .append(this.videoWidth_)
            .append("px\",")
            .append("height: \"")
            .append(this.videoHeight_)
            .append("px\",")
            .append("cssClass: \"jp-video-")
            .append(this.videoHeight_)
            .append("p\"")
            .append("},");
      }
      ss.append("cssSelectorAncestor: ")
          .append(this.gui_ != null ? "'#" + this.getId() + '\'' : "''")
          .append(", cssSelector: {");
      String[] controlSelectors = {
        "videoPlay",
        "play",
        "pause",
        "stop",
        "volumeMute",
        "volumeUnmute",
        "volumeMax",
        "fullScreen",
        "restoreScreen",
        "repeat",
        "repeatOff"
      };
      first = true;
      for (int i = MediaPlayerButtonId.VideoPlay.getValue();
          i < (int) MediaPlayerButtonId.RepeatOff.getValue();
          ++i) {
        if (this.control_[i] != null) {
          if (!first) {
            ss.append(", ");
          }
          ss.append(controlSelectors[i])
              .append(":\"#")
              .append(this.control_[i].getId())
              .append("\"");
          first = false;
        }
      }
      String[] displaySelectors = {"currentTime", "duration"};
      for (int i = MediaPlayerTextId.CurrentTime.getValue();
          i < (int) MediaPlayerTextId.Duration.getValue();
          ++i) {
        if (this.control_[i] != null) {
          if (!first) {
            ss.append(", ");
          }
          ss.append(displaySelectors[i])
              .append(":\"#")
              .append(this.display_[i].getId())
              .append("\"");
          first = false;
        }
      }
      if (this.progressBar_[(int) MediaPlayerProgressBarId.Time.getValue()] != null) {
        if (!first) {
          ss.append(", ");
        }
        ss.append("seekBar:\"#")
            .append(this.progressBar_[(int) MediaPlayerProgressBarId.Time.getValue()].getId())
            .append("\", ")
            .append("playBar:\"#bar")
            .append(this.progressBar_[(int) MediaPlayerProgressBarId.Time.getValue()].getId())
            .append("\"");
        first = false;
      }
      if (this.progressBar_[(int) MediaPlayerProgressBarId.Volume.getValue()] != null) {
        if (!first) {
          ss.append(", ");
        }
        ss.append("volumeBar:\"#")
            .append(this.progressBar_[(int) MediaPlayerProgressBarId.Volume.getValue()].getId())
            .append("\", ")
            .append("volumeBarValue:\"#bar")
            .append(this.progressBar_[(int) MediaPlayerProgressBarId.Volume.getValue()].getId())
            .append("\"");
        first = false;
      }
      ss.append('}').append("});");
      ss.append("new Wt4.11.2.WMediaPlayer(")
          .append(app.getJavaScriptClass())
          .append(',')
          .append(this.getJsRef())
          .append(");");
      this.doJavaScript(ss.toString());
      this.boundSignals_ = 0;
      this.boundSignalsDouble_ = 0;
    }
    if (this.boundSignals_ < this.signals_.size()) {
      StringBuilder ss = new StringBuilder();
      ss.append(this.getJsPlayerRef());
      for (int i = this.boundSignals_; i < this.signals_.size(); ++i) {
        ss.append(".bind('")
            .append(this.signals_.get(i).getName())
            .append("', function(o, e) { ")
            .append(this.signals_.get(i).createCall())
            .append("})");
      }
      ss.append(';');
      this.doJavaScript(ss.toString());
      this.boundSignals_ = this.signals_.size();
    }
    if (this.boundSignalsDouble_ < this.signalsDouble_.size()) {
      StringBuilder ss = new StringBuilder();
      ss.append(this.getJsPlayerRef());
      for (int i = this.boundSignalsDouble_; i < this.signalsDouble_.size(); ++i) {
        ss.append(".bind('")
            .append(this.signalsDouble_.get(i).signal.getName())
            .append("', function(o, e) { ")
            .append(
                this.signalsDouble_.get(i).signal.createCall(this.signalsDouble_.get(i).jsExprA1))
            .append("})");
      }
      ss.append(';');
      this.doJavaScript(ss.toString());
      this.boundSignals_ = this.signals_.size();
    }
    super.render(flags);
  }

  static class Source {
    private static Logger logger = LoggerFactory.getLogger(Source.class);

    public MediaEncoding encoding;
    public WLink link;
  }

  static class SignalDouble {
    private static Logger logger = LoggerFactory.getLogger(SignalDouble.class);

    public JSignal1<Double> signal;
    public String jsExprA1;
  }

  private List<JSignal> signals_;
  private List<WMediaPlayer.SignalDouble> signalsDouble_;
  private MediaType mediaType_;
  private int videoWidth_;
  private int videoHeight_;
  private WString title_;
  private List<WMediaPlayer.Source> media_;
  private String initialJs_;
  private WInteractWidget[] control_ = new WInteractWidget[11];
  private WText[] display_ = new WText[3];
  private WProgressBar[] progressBar_ = new WProgressBar[2];
  private WWidget gui_;
  private int boundSignals_;
  private int boundSignalsDouble_;
  private boolean mediaUpdated_;

  static class State {
    private static Logger logger = LoggerFactory.getLogger(State.class);

    public boolean playing;
    public boolean ended;
    public MediaReadyState readyState;
    public double seekPercent;
    public double volume;
    public double duration;
    public double currentTime;
    public double playbackRate;

    public State() {
      this.playing = false;
      this.readyState = MediaReadyState.HaveNothing;
      this.seekPercent = 0;
      this.volume = 0.8;
      this.duration = 0;
      this.currentTime = 0;
      this.playbackRate = 1;
    }
  }

  private WMediaPlayer.State status_;

  private void createDefaultGui() {
    this.gui_ = (WWidget) null;
    WTemplate ui =
        new WTemplate(
            tr("Wt.WMediaPlayer.defaultgui-" + media[(int) this.mediaType_.getValue()]),
            (WContainerWidget) null);
    this.addAnchor(ui, MediaPlayerButtonId.Play, "play-btn", "jp-play");
    this.addAnchor(ui, MediaPlayerButtonId.Pause, "pause-btn", "jp-pause");
    this.addAnchor(ui, MediaPlayerButtonId.Stop, "stop-btn", "jp-stop");
    this.addAnchor(ui, MediaPlayerButtonId.VolumeMute, "mute-btn", "jp-mute");
    this.addAnchor(ui, MediaPlayerButtonId.VolumeUnmute, "unmute-btn", "jp-unmute");
    this.addAnchor(ui, MediaPlayerButtonId.VolumeMax, "volume-max-btn", "jp-volume-max");
    this.addAnchor(ui, MediaPlayerButtonId.RepeatOn, "repeat-btn", "jp-repeat");
    this.addAnchor(ui, MediaPlayerButtonId.RepeatOff, "repeat-off-btn", "jp-repeat-off");
    if (this.mediaType_ == MediaType.Video) {
      this.addAnchor(
          ui, MediaPlayerButtonId.VideoPlay, "video-play-btn", "jp-video-play-icon", "play");
      this.addAnchor(ui, MediaPlayerButtonId.FullScreen, "full-screen-btn", "jp-full-screen");
      this.addAnchor(
          ui, MediaPlayerButtonId.RestoreScreen, "restore-screen-btn", "jp-restore-screen");
    }
    this.addText(ui, MediaPlayerTextId.CurrentTime, "current-time", "jp-current-time");
    this.addText(ui, MediaPlayerTextId.Duration, "duration", "jp-duration");
    this.addText(ui, MediaPlayerTextId.Title, "title", "");
    this.addProgressBar(
        ui, MediaPlayerProgressBarId.Time, "progress-bar", "jp-seek-bar", "jp-play-bar");
    this.addProgressBar(
        ui, MediaPlayerProgressBarId.Volume, "volume-bar", "jp-volume-bar", "jp-volume-bar-value");
    ui.bindString("title-display", (this.title_.length() == 0) ? "none" : "");
    this.addStyleClass(this.mediaType_ == MediaType.Video ? "jp-video" : "jp-audio");
    this.setControlsWidget(ui);
  }

  private void addAnchor(
      WTemplate t,
      MediaPlayerButtonId id,
      String bindId,
      final String styleClass,
      final String altText) {
    String text = "";
    if (altText.length() == 0) {
      text = styleClass.substring(3);
    } else {
      text = altText;
    }
    text = "Wt.WMediaPlayer." + text;
    WAnchor anchor =
        new WAnchor(new WLink("javascript:;"), WString.tr(text), (WContainerWidget) null);
    anchor.setStyleClass(styleClass);
    anchor.setAttributeValue("tabindex", "1");
    anchor.setToolTip(WString.tr(text));
    anchor.setInline(false);
    this.setButton(id, anchor);
    t.bindWidget(bindId, anchor);
  }

  private final void addAnchor(
      WTemplate t, MediaPlayerButtonId id, String bindId, final String styleClass) {
    addAnchor(t, id, bindId, styleClass, "");
  }

  private void addText(WTemplate t, MediaPlayerTextId id, String bindId, final String styleClass) {
    WText text = new WText();
    text.setInline(false);
    if (styleClass.length() != 0) {
      text.setStyleClass(styleClass);
    }
    this.setText(id, text);
    t.bindWidget(bindId, text);
  }

  private void addProgressBar(
      WTemplate t,
      MediaPlayerProgressBarId id,
      String bindId,
      final String styleClass,
      final String valueStyleClass) {
    WProgressBar progressBar = new WProgressBar();
    progressBar.setStyleClass(styleClass);
    progressBar.setValueStyleClass(valueStyleClass);
    progressBar.setInline(false);
    this.setProgressBar(id, progressBar);
    t.bindWidget(bindId, progressBar);
  }

  private JSignal signal(String name) {
    for (int i = 0; i < this.signals_.size(); ++i) {
      if (this.signals_.get(i).getName().equals(name)) {
        return this.signals_.get(i);
      }
    }
    JSignal result;
    this.signals_.add(result = new JSignal(this, name, true));
    this.scheduleRender();
    return result;
  }

  private JSignal1<Double> signalDouble(String name, final String jsExpr) {
    for (int i = 0; i < this.signalsDouble_.size(); ++i) {
      if (this.signalsDouble_.get(i).signal.getName().equals(name)) {
        return this.signalsDouble_.get(i).signal;
      }
    }
    WMediaPlayer.SignalDouble sd = new WMediaPlayer.SignalDouble();
    sd.signal = new JSignal1<Double>(this, name, true) {};
    sd.jsExprA1 = jsExpr;
    this.signalsDouble_.add(sd);
    this.scheduleRender();
    return sd.signal;
  }

  private void updateProgressBarState(MediaPlayerProgressBarId id) {
    WProgressBar bar = this.getProgressBar(id);
    if (bar != null) {
      switch (id) {
        case Time:
          bar.setState(
              0, this.status_.seekPercent * this.status_.duration, this.status_.currentTime);
          break;
        case Volume:
          bar.setState(0, 1, this.status_.volume);
      }
    }
  }

  private void updateFromProgressBar(MediaPlayerProgressBarId id, double value) {
    switch (id) {
      case Time:
        this.seek(value);
        break;
      case Volume:
        this.setVolume(value);
    }
  }

  private void playerDo(final String method, final String args) {
    StringBuilder ss = new StringBuilder();
    ss.append(".jPlayer('").append(method).append('\'');
    if (args.length() != 0) {
      ss.append(',').append(args);
    }
    ss.append(')');
    this.playerDoRaw(ss.toString());
  }

  private final void playerDo(final String method) {
    playerDo(method, "");
  }

  private void playerDoData(final String method, final String args) {
    this.playerDoRaw(".data('jPlayer')." + method + "(" + args + ")");
  }

  private void playerDoRaw(final String jqueryMethod) {
    StringBuilder ss = new StringBuilder();
    if (this.isRendered()) {
      ss.append(this.getJsPlayerRef());
    }
    ss.append(jqueryMethod);
    if (this.isRendered()) {
      ss.append(';');
    }
    if (this.isRendered()) {
      this.doJavaScript(ss.toString());
    } else {
      this.initialJs_ += ss.toString();
    }
  }

  private static String LOAD_STARTED_SIGNAL = "jPlayer_loadstart.Wt";
  private static String TIME_UPDATED_SIGNAL = "jPlayer_timeupdate.Wt";
  private static String PLAYBACK_STARTED_SIGNAL = "jPlayer_play.Wt";
  private static String PLAYBACK_PAUSED_SIGNAL = "jPlayer_pause.Wt";
  private static String ENDED_SIGNAL = "jPlayer_ended.Wt";
  private static String VOLUME_CHANGED_SIGNAL = "jPlayer_volumechange.Wt";
  private static String[] mediaNames = {
    "poster", "mp3", "m4a", "oga", "wav", "webma", "fla", "m4v", "ogv", "webmv", "flv"
  };
  private static String[] media = {"audio", "video"};

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WMediaPlayer",
        "(function(t,a){a.wtObj=this;a.wtEncodeValue=function(){const t=$(a).find(\".jp-jplayer\").data(\"jPlayer\"),e=t.status;return t.options.volume+\";\"+e.currentTime+\";\"+e.duration+\";\"+(e.paused?1:0)+\";\"+(e.ended?1:0)+\";\"+e.readyState+\";\"+(e.playbackRate?e.playbackRate:1)+\";\"+e.seekPercent};$(a).find(\".jp-jplayer\").data(\"jPlayer\").wtPlaybackRate=function(t){const a=this.htmlElement.video||this.htmlElement.audio;a&&(a.playbackRate=t);return this}})");
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
