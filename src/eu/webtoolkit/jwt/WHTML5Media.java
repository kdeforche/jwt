/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import eu.webtoolkit.jwt.utils.EnumUtils;

/**
 * Abstract baseclass for HTML5 media elements
 * <p>
 * 
 * This class is an abstract base class for HTML5 media elements (audio, video).
 */
public abstract class WHTML5Media extends WWebWidget {
	/**
	 * Enumeration for playback options.
	 */
	public enum Options {
		/**
		 * Start playing as soon as the video is loaded.
		 */
		Autoplay(1),
		/**
		 * Enable loop mode.
		 */
		Loop(2),
		/**
		 * Show video controls in the browser.
		 */
		Controls(4);

		private int value;

		Options(int value) {
			this.value = value;
		}

		/**
		 * Returns the numerical representation of this enum.
		 */
		public int getValue() {
			return value;
		}
	}

	/**
	 * Enumeration for preload strategy.
	 */
	public enum PreloadMode {
		/**
		 * Hints that the user will probably not play the video.
		 */
		PreloadNone,
		/**
		 * Hints that it is ok to download the entire resource.
		 */
		PreloadAuto,
		/**
		 * Hints that retrieving metadata is a good option.
		 */
		PreloadMetadata;

		/**
		 * Returns the numerical representation of this enum.
		 */
		public int getValue() {
			return ordinal();
		}
	}

	/**
	 * Consctructor for a HTML5 media widget.
	 * <p>
	 * A freshly constructed HTML5Video widget has no options set, no media
	 * sources, and has preload mode set to PreloadAuto.
	 */
	public WHTML5Media(WContainerWidget parent) {
		super(parent);
		this.sources_ = new ArrayList<WHTML5Media.Source>();
		this.mediaId_ = "";
		this.flags_ = EnumSet.noneOf(WHTML5Media.Options.class);
		this.preloadMode_ = WHTML5Media.PreloadMode.PreloadAuto;
		this.alternative_ = null;
		this.flagsChanged_ = false;
		this.preloadChanged_ = false;
		this.setInline(false);
		WApplication app = WApplication.getInstance();
		String THIS_JS = "js/WHTML5Media.js";
		if (!app.isJavaScriptLoaded(THIS_JS)) {
			app.doJavaScript(wtjs1(app), false);
			app.setJavaScriptLoaded(THIS_JS);
		}
		this.doJavaScript("new Wt3_1_5.WHTML5Media(" + app.getJavaScriptClass()
				+ "," + this.getJsRef() + ");");
		this.setJavaScriptMember("WtPlay", "function() {$('#" + this.getId()
				+ "').data('obj').play();}");
		this.setJavaScriptMember("WtPause", "function() {$('#" + this.getId()
				+ "').data('obj').pause();}");
	}

	/**
	 * Consctructor for a HTML5 media widget.
	 * <p>
	 * Calls {@link #WHTML5Media(WContainerWidget parent)
	 * this((WContainerWidget)null)}
	 */
	public WHTML5Media() {
		this((WContainerWidget) null);
	}

	/**
	 * Set the media element options.
	 * <p>
	 * 
	 * @see WHTML5Media.Options
	 */
	public void setOptions(EnumSet<WHTML5Media.Options> flags) {
		this.flags_ = EnumSet.copyOf(flags);
		this.flagsChanged_ = true;
		this.repaint(EnumSet.of(RepaintFlag.RepaintPropertyAttribute));
	}

	/**
	 * Set the media element options.
	 * <p>
	 * Calls {@link #setOptions(EnumSet flags) setOptions(EnumSet.of(flag,
	 * flags))}
	 */
	public final void setOptions(WHTML5Media.Options flag,
			WHTML5Media.Options... flags) {
		setOptions(EnumSet.of(flag, flags));
	}

	/**
	 * Retrieve the configured options.
	 */
	public EnumSet<WHTML5Media.Options> getOptions() {
		return this.flags_;
	}

	/**
	 * Set the preload mode.
	 */
	public void setPreloadMode(WHTML5Media.PreloadMode mode) {
		this.preloadMode_ = mode;
		this.preloadChanged_ = true;
		this.repaint(EnumSet.of(RepaintFlag.RepaintPropertyAttribute));
	}

	/**
	 * Retrieve the preload mode.
	 */
	public WHTML5Media.PreloadMode getPreloadMode() {
		return this.preloadMode_;
	}

	/**
	 * Add a media source.
	 * <p>
	 * This method specifies a media source using only the URL. You may add as
	 * many video sources as you want. The browser will select the appropriate
	 * video stream to display to the user.
	 */
	public void addSource(String url) {
		this.sources_.add(new WHTML5Media.Source(url));
	}

	/**
	 * Add a media source.
	 * <p>
	 * This method specifies a media source using the URL and the mime type
	 * (e.g. video/ogg; codecs=&quot;theora, vorbis&quot;).
	 */
	public void addSource(String url, String type) {
		this.sources_.add(new WHTML5Media.Source(url, type));
	}

	/**
	 * Add a media source.
	 * <p>
	 * This method specifies a media source using the URL, the mime type, and
	 * the media attribute.
	 */
	public void addSource(String url, String type, String media) {
		this.sources_.add(new WHTML5Media.Source(url, type, media));
	}

	/**
	 * Content to be shown when media cannot be played.
	 * <p>
	 * As the HTML5 media tags are not supported by all browsers, it is a good
	 * idea to provide fallback options when the media cannot be displayed. If
	 * the media can be played by the browser, the alternative content will be
	 * suppressed.
	 * <p>
	 * The two reasons to display the alternative content are (1) the media tag
	 * is not supported, or (2) the media tag is supported, but none of the
	 * media sources are supported by the browser. In the first case, fall-back
	 * is automatic and does not rely on JavaScript in the browser; in the
	 * latter case, JavaScript is required to make the fallback work.
	 * <p>
	 * The alternative content can be any widget: you can set it to an
	 * alternative media player (QuickTime, Flash, ...), show a Flash movie, an
	 * animated gif, a text, a poster image, ...
	 */
	public void setAlternativeContent(WWidget alternative) {
		if (this.alternative_ != null) {
			if (this.alternative_ != null)
				this.alternative_.remove();
		}
		this.alternative_ = alternative;
		if (this.alternative_ != null) {
			this.addChild(this.alternative_);
		}
	}

	/**
	 * Invoke {@link WHTML5Media#play() play()} on the media element.
	 * <p>
	 * JavaScript must be available for this function to work.
	 */
	public void play() {
		this.doJavaScript(this.getJsRef() + ".WtPlay();");
	}

	/**
	 * Invoke {@link WHTML5Media#pause() pause()} on the media element.
	 * <p>
	 * JavaScript must be available for this function to work.
	 */
	public void pause() {
		this.doJavaScript(this.getJsRef() + ".WtPause();");
	}

	void getDomChanges(List<DomElement> result, WApplication app) {
		if (this.mediaId_.length() != 0) {
			DomElement media = DomElement.getForUpdate(this.mediaId_,
					DomElementType.DomElement_DIV);
			this.updateMediaDom(media, false);
			result.add(media);
		}
		super.getDomChanges(result, app);
	}

	DomElement createDomElement(WApplication app) {
		DomElement result = null;
		if (this.isInLayout()) {
			this.setJavaScriptMember(WT_RESIZE_JS, "function() {}");
		}
		if (app.getEnvironment().agentIsIE()) {
			result = DomElement.createNew(DomElementType.DomElement_DIV);
			if (this.alternative_ != null) {
				result.addChild(this.alternative_.createSDomElement(app));
			}
		} else {
			DomElement media = this.createMediaDomElement();
			DomElement wrap = null;
			if (this.isInLayout()) {
				media.setProperty(Property.PropertyStylePosition, "absolute");
				media.setProperty(Property.PropertyStyleLeft, "0");
				media.setProperty(Property.PropertyStyleRight, "0");
				wrap = DomElement.createNew(DomElementType.DomElement_DIV);
				wrap.setProperty(Property.PropertyStylePosition, "relative");
			}
			result = wrap != null ? wrap : media;
			if (wrap != null) {
				this.mediaId_ = this.getId() + "_media";
				media.setId(this.mediaId_);
			} else {
				this.mediaId_ = this.getId();
			}
			this.updateMediaDom(media, true);
			if (wrap != null) {
				wrap.addChild(media);
			}
		}
		if (this.isInLayout()) {
			StringWriter ss = new StringWriter();
			ss.append("function(self, w, h) {");
			if (this.mediaId_.length() != 0) {
				ss
						.append("v="
								+ this.getJsMediaRef()
								+ ";if(v){v.setAttribute('width', w);v.setAttribute('height', h);}");
			}
			if (this.alternative_ != null) {
				ss.append("a=" + this.alternative_.getJsRef() + ";if(a && a.")
						.append(WT_RESIZE_JS).append(")a.")
						.append(WT_RESIZE_JS).append("(a, w, h);");
			}
			ss.append("}");
			this.setJavaScriptMember(WT_RESIZE_JS, ss.toString());
		}
		this.setId(result, app);
		this.updateDom(result, true);
		this.setJavaScriptMember("mediaId", "'" + this.mediaId_ + "'");
		return result;
	}

	void updateMediaDom(DomElement element, boolean all) {
		if (all && this.alternative_ != null) {
			element
					.setAttribute(
							"onerror",
							"if(event.target.error && event.target.error.code==event.target.error.MEDIA_ERR_SRC_NOT_SUPPORTED){while (this.hasChildNodes())if (Wt3_1_5.hasTag(this.firstChild,'SOURCE')){this.removeChild(this.firstChild);}else{this.parentNode.insertBefore(this.firstChild, this);}this.style.display= 'none';}");
		}
		if (all || this.flagsChanged_) {
			if (!all
					|| !EnumUtils.mask(this.flags_,
							WHTML5Media.Options.Controls).isEmpty()) {
				element.setAttribute("controls", !EnumUtils.mask(this.flags_,
						WHTML5Media.Options.Controls).isEmpty() ? "controls"
						: "");
			}
			if (!all
					|| !EnumUtils.mask(this.flags_,
							WHTML5Media.Options.Autoplay).isEmpty()) {
				element.setAttribute("autoplay", !EnumUtils.mask(this.flags_,
						WHTML5Media.Options.Autoplay).isEmpty() ? "autoplay"
						: "");
			}
			if (!all
					|| !EnumUtils.mask(this.flags_, WHTML5Media.Options.Loop)
							.isEmpty()) {
				element.setAttribute("loop", !EnumUtils.mask(this.flags_,
						WHTML5Media.Options.Loop).isEmpty() ? "loop" : "");
			}
		}
		if (all || this.preloadChanged_) {
			switch (this.preloadMode_) {
			case PreloadNone:
				element.setAttribute("preload", "none");
				break;
			default:
			case PreloadAuto:
				element.setAttribute("preload", "auto");
				break;
			case PreloadMetadata:
				element.setAttribute("preload", "metadata");
				break;
			}
		}
		if (all) {
			for (int i = 0; i < this.sources_.size(); ++i) {
				DomElement src = DomElement
						.createNew(DomElementType.DomElement_SOURCE);
				src.setAttribute("src",
						fixRelativeUrl(this.sources_.get(i).url));
				if (this.sources_.get(i).hasType) {
					src.setAttribute("type", this.sources_.get(i).type);
				}
				if (this.sources_.get(i).hasMedia) {
					src.setAttribute("media", this.sources_.get(i).media);
				}
				if (i + 1 >= this.sources_.size() && this.alternative_ != null) {
					src
							.setAttribute(
									"onerror",
									"media = parentNode;if(media){while (media && media.hasChildNodes())if (Wt3_1_5.hasTag(media.firstChild,'SOURCE')){media.removeChild(media.firstChild);}else{media.parentNode.insertBefore(media.firstChild, media);}media.style.display= 'none';}");
				}
				element.addChild(src);
			}
		}
		if (all) {
			if (this.alternative_ != null) {
				element.addChild(this.alternative_
						.createSDomElement(WApplication.getInstance()));
			}
		}
		this.flagsChanged_ = this.preloadChanged_ = false;
	}

	abstract DomElement createMediaDomElement();

	/**
	 * Returns the JavaScript reference to the media object, or null.
	 * <p>
	 * It is possible, for browser compatibility reasons, that
	 * {@link WWidget#getJsRef() WWidget#getJsRef()} is not the HTML5 media
	 * element. {@link WHTML5Media#getJsMediaRef() getJsMediaRef()} is
	 * guaranteed to be an expression that evaluates to the media object. This
	 * expression may yield null, if the video object is not rendered at all
	 * (e.g. on older versions of Internet Explorer).
	 */
	protected String getJsMediaRef() {
		if (this.mediaId_.length() == 0) {
			return "null";
		} else {
			return "Wt3_1_5.getElement('" + this.mediaId_ + "')";
		}
	}

	static class Source {
		public Source(String url, String type, String media) {
			this.type = type;
			this.url = url;
			this.media = media;
			this.hasMedia = true;
			this.hasType = true;
		}

		public Source(String url, String type) {
			this.type = type;
			this.url = url;
			this.media = "";
			this.hasMedia = false;
			this.hasType = true;
		}

		public Source(String url) {
			this.type = "";
			this.url = url;
			this.media = "";
			this.hasMedia = false;
			this.hasType = false;
		}

		public String type;
		public String url;
		public String media;
		public boolean hasMedia;
		public boolean hasType;
	}

	private List<WHTML5Media.Source> sources_;
	private String mediaId_;
	EnumSet<WHTML5Media.Options> flags_;
	private WHTML5Media.PreloadMode preloadMode_;
	private WWidget alternative_;
	private boolean flagsChanged_;
	private boolean preloadChanged_;
	private boolean hasPoster_;

	static String wtjs1(WApplication app) {
		return "Wt3_1_5.WHTML5Media = function(c,b){jQuery.data(b,\"obj\",this);this.alternativeEl=this.mediaEl=null;this.play=function(){if(b.mediaId){var a=$(\"#\"+b.mediaId).get(0);if(a){a.play();return}}if(b.alternativeId)(a=$(\"#\"+b.alternativeId).get(0))&&a.WtPlay&&a.WtPlay()};this.pause=function(){if(b.mediaId){var a=$(\"#\"+b.mediaId).get(0);if(a){a.pause();return}}if(b.alternativeId)(a=$(\"#\"+b.alternativeId).get(0))&&a.WtPlay&&a.WtPause()}};";
	}
}
