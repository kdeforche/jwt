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
 * A class that captures information on the application environment.
 *
 * <p>The environment provides information on the client, and gives access to startup arguments.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * WEnvironment env = WApplication.instance().environment();
 *
 * // read an application startup argument
 * // (passed as argument in the URL or POST'ed to the application).
 * if (!env.getParameterValues("login").isEmpty()) {
 * String login = env.getParameterValues("login").get(0);
 * //...
 * }
 *
 * // Check for JavaScript/AJAX availability before using JavaScript-only
 * // widgets
 * WTextArea textEdit;
 * if (!env.isAjax())
 * textEdit = new WTextEdit(); // provide an HTML text editor
 * else
 * textEdit = new WTextArea(); // fall-back to a plain old text area.
 *
 * }</pre>
 */
public class WEnvironment {
  private static Logger logger = LoggerFactory.getLogger(WEnvironment.class);

  /** Wt&apos;s JavaScript scope. */
  public static String getJavaScriptWtScope() {
    return "Wt4.11.2";
  }
  /**
   * Parameters passed to the application.
   *
   * <p>Arguments passed to the application, either in the URL for a http GET, or in both the URL
   * and data submitted in a http POST.
   *
   * <p>
   *
   * @see WEnvironment#getParameterValues(String name)
   */
  public Map<String, String[]> getParameterMap() {
    return this.parameters_;
  }
  /**
   * Returns values for a query parameter.
   *
   * <p>Returns an empty list if the parameter was not defined.
   *
   * <p>One or more values may be associated with a single argument.
   *
   * <p>For example a JWt application <code>foo.wt</code> started as <code>
   * <a href="http://.../foo.wt?hello=Hello&amp;hello=World">http://.../foo.wt?hello=Hello&amp;hello=World</a>
   * </code> will result in both values <code>&quot;Hello&quot;</code> and <code>&quot;World&quot;
   * </code> to be associated with the argument <code>&quot;hello&quot;</code>.
   *
   * <p>
   *
   * @see WEnvironment#getParameterMap()
   */
  public String[] getParameterValues(final String name) {
    String[] i = this.parameters_.get(name);
    if (i != null) {
      return i;
    } else {
      return new String[0];
    }
  }
  /**
   * Returns a single value for a query parameter.
   *
   * <p>Returns the first value for a parameter, or <code>null</code> if the parameter is not found.
   *
   * <p>
   *
   * @see WEnvironment#getParameterValues(String name)
   */
  public String getParameter(final String name) {
    final String[] values = this.getParameterValues(name);
    if (!(values.length == 0)) {
      return values[0];
    } else {
      return null;
    }
  }
  /**
   * Returns the cookies from the environment.
   *
   * <p>This returns all cookies that were present in initial request for the application. Cookies
   * set with {@link WApplication#setCookie(javax.servlet.http.Cookie cookie)
   * WApplication#setCookie()} are not taken into consideration.
   *
   * <p>Cookies allow you to persist information across sessions, but note that not all clients may
   * support cookies or may some clients may be configured to block cookies.
   *
   * <p>
   *
   * @see WEnvironment#supportsCookies()
   * @see WEnvironment#getCookie(String cookieName)
   */
  public Map<String, String> getCookies() {
    return this.cookies_;
  }
  /**
   * Returns a cookie value.
   *
   * <p>Returns 0 if no value was set for the given cookie.
   *
   * <p>
   *
   * @see WEnvironment#getCookie(String cookieName)
   */
  public String getCookie(final String cookieName) {
    String i = this.cookies_.get(cookieName);
    if (i == null) {
      return null;
    } else {
      return i;
    }
  }
  /**
   * Returns a header value.
   *
   * <p>Returns a header value, or an empty string if the header was present.
   */
  public String getHeaderValue(final String name) {
    return this.session_.getCgiHeader(name);
  }
  /**
   * Returns whether the browser has enabled support for cookies.
   *
   * <p>When the user disables cookies during the visit of the page, this value is not updated.
   *
   * <p>
   *
   * @see WEnvironment#getCookies()
   * @see WEnvironment#getCookie(String cookieName)
   */
  public boolean supportsCookies() {
    return this.doesCookies_;
  }
  /**
   * Returns whether the browser has enabled support for JavaScript.
   *
   * <p>This is the same as {@link WEnvironment#hasAjax() hasAjax()}: JWt only considers using
   * JavaScript when it has detected AJAX support.
   *
   * <p>
   *
   * @see WEnvironment#hasAjax()
   */
  public boolean hasJavaScript() {
    return this.doesAjax_;
  }
  /**
   * Returns whether the browser has enabled support for AJAX.
   *
   * <p>Without support for JavaScript/AJAX, JWt will still be able to serve the application, but
   * with one considerable limitation: only the {@link WTimer#timeout()}, {@link
   * WInteractWidget#clicked()}, {@link WApplication#internalPathChanged()}, and {@link
   * WAbstractArea#clicked()} signals (and any derived signals) will generate events.
   *
   * <p>Every event will cause the complete page to be rerendered.
   *
   * <p>
   *
   * @see WEnvironment#hasJavaScript()
   */
  public boolean hasAjax() {
    return this.doesAjax_;
  }
  /**
   * Returns whether the browser has support for WebGL.
   *
   * <p>Support for WebGL is required for client-side rendering of {@link WGLWidget}.
   */
  public boolean hasWebGL() {
    return this.webGLsupported_;
  }
  /**
   * Returns the horizontal resolution of the client&apos;s screen.
   *
   * <p>Returns -1 if screen width is not known.
   *
   * <p>
   *
   * @see WEnvironment#getScreenHeight()
   */
  public int getScreenWidth() {
    return this.screenWidth_;
  }
  /**
   * Returns the vertical resolution of the client&apos;s screen.
   *
   * <p>Returns -1 if screen height is not known.
   *
   * <p>
   *
   * @see WEnvironment#getScreenWidth()
   */
  public int getScreenHeight() {
    return this.screenHeight_;
  }
  /**
   * Returns the browser-side DPI scaling factor.
   *
   * <p>Internet Explorer scales all graphics, fonts and other elements on high-density screens to
   * make them readable. This is controlled by the DPI setting of the display. If all goes well, you
   * do not have to worry about this scaling factor. Unfortunately, not all elements are scaled
   * appropriately. The scaling factor is supposed to be used only internally in JWt and is in this
   * interface for informational purposes.
   *
   * <p>
   *
   * @see WVmlImage
   */
  public double getDpiScale() {
    return this.dpiScale_;
  }
  /**
   * Returns the preferred language indicated in the request header.
   *
   * <p>The language is parsed from the HTTP <code>Accept-Language</code> field, if present. If not,
   * the locale is empty.
   *
   * <p>If multiple languages are present, the one with the highest &quot;q&quot;uality is assumed,
   * and if a tie is present, the first one is taken.
   *
   * <p>
   *
   * @see WApplication#setLocale(Locale locale, boolean doRefresh)
   */
  public Locale getLocale() {
    return this.locale_;
  }
  /**
   * Returns the time zone offset as reported by the client.
   *
   * <p>This returns the time offset that the client has relative to UTC. A positive value thus
   * means that the local time is ahead of UTC.
   *
   * <p>This requires JavaScript support.
   *
   * <p>
   */
  public Duration getTimeZoneOffset() {
    return this.timeZoneOffset_;
  }
  /**
   * Returns the time zone name as reported by the client.
   *
   * <p>
   *
   * <p><i><b>Note: </b>This requires JavaScript support and is only supported by browsers that
   * implement the JavaScript Internationalization API. No version of Internet Explorer supports
   * this, but modern browsers do. If not supported, this will return the empty string. </i>
   */
  public String getTimeZoneName() {
    return this.timeZoneName_;
  }
  /**
   * Returns the server host name that is used by the client.
   *
   * <p>The hostname is the unresolved host name with optional port number, which the browser used
   * to connect to the application.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li><code>www.mydomain.com</code>
   *   <li><code>localhost:8080</code>
   * </ul>
   *
   * <p>For HTTP 1.1 requests, this information is fetched from the HTTP <code>Host</code> header.
   * If JWt is configured behind a reverse proxy, then the last entry in the HTTP <code>
   * X-Forwarded-Host</code> header field is used instead (to infer the name of the reverse proxy
   * instead).
   *
   * <p>For HTTP 1.0 requests, the HTTP <code>Host</code> header is not required. When not present,
   * the server host name is inferred from the configured server name, which defaults to the DNS
   * name.
   */
  public String getHostName() {
    return this.host_;
  }
  /**
   * Returns the URL scheme used for the current request (<code>&quot;http&quot;</code> or <code>
   * &quot;https&quot;</code>).
   */
  public String getUrlScheme() {
    return this.urlScheme_;
  }
  /**
   * Returns the user agent.
   *
   * <p>The user agent, as reported in the HTTP <code>User-Agent</code> field.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public String getUserAgent() {
    return this.userAgent_;
  }
  /**
   * Returns the referer.
   *
   * <p>The referer, as reported in the HTTP <code>Referer</code> field.
   */
  public String getReferer() {
    return this.referer_;
  }
  /**
   * Returns the accept header.
   *
   * <p>The accept header, as reported in the HTTP <code>Accept</code> field.
   */
  public String getAccept() {
    return this.accept_;
  }
  /**
   * Returns if the user agent is a (known) indexing spider bot.
   *
   * <p>Note: currently the list of know bots is quite small. This method is used internally to
   * render the web application for optimal indexing by bots:
   *
   * <ul>
   *   <li>there is no detection for JavaScript, instead the application is directly served assuming
   *       no JavaScript support.
   *   <li>session information is omitted from the Urls.
   *   <li>no sessions are created (they are immediately stopped after the request has been
   *       handled).
   *   <li>auto-generated <code>id</code> and <code>name</code> attributes are omitted from DOM
   *       nodes. In this way, the generated page is always exactly the same.
   * </ul>
   */
  public boolean agentIsSpiderBot() {
    return this.agent_ == UserAgent.BotAgent;
  }
  /**
   * Returns the web server signature.
   *
   * <p>The value of the CGI variable <code>SERVER_SIGNATURE</code>.
   *
   * <p>Example: <code>&lt;address&gt;Apache Server at localhost Port 80&lt;/address&gt;</code>.
   */
  public String getServerSignature() {
    return this.serverSignature_;
  }
  /**
   * Returns the web server software.
   *
   * <p>The value of the CGI variable <code>SERVER_SOFTWARE</code>.
   *
   * <p>Example: <code>&quot;Apache&quot;</code>
   */
  public String getServerSoftware() {
    return this.serverSoftware_;
  }
  /**
   * Returns the email address of the server admin.
   *
   * <p>The value of the CGI variable <code>SERVER_ADMIN</code>.
   *
   * <p>Example: <code>&quot;root@localhost&quot;</code>
   */
  public String getServerAdmin() {
    return this.serverAdmin_;
  }
  /**
   * Returns the IP address of the client.
   *
   * <p>The (most likely) IP address of the client that is connected to this session.
   *
   * <p>This is taken to be the first public address that is given in the Client-IP header, or in
   * the X-Forwarded-For header (in case the client is behind a proxy). If none of these headers is
   * present, the remote socket IP address is used.
   */
  public String getClientAddress() {
    return this.clientAddress_;
  }
  /**
   * Returns the initial internal path.
   *
   * <p>This is the internal path with which the application was started.
   *
   * <p>For an application deployed at <code>&quot;/stuff/app.wt&quot;</code>, the following URL
   * indicates an internal path <code>&quot;/this/there&quot;</code>:
   *
   * <pre>{@code
   * http://www.mydomain.com/stuff/app.wt/this/there
   *
   * }</pre>
   *
   * <p>
   *
   * @see WApplication#setInternalPath(String path, boolean emitChange)
   * @see WEnvironment#getDeploymentPath()
   */
  public String getInternalPath() {
    return this.internalPath_;
  }
  /**
   * Returns the deployment path.
   *
   * <p>This is the path at which the application is deployed.
   *
   * <p>
   *
   * @see WEnvironment#getInternalPath()
   */
  public String getDeploymentPath() {
    if (this.publicDeploymentPath_.length() != 0) {
      return this.publicDeploymentPath_;
    } else {
      return this.session_.getDeploymentPath();
    }
  }
  /**
   * Returns the version of the JWt library.
   *
   * <p>Example: <code>&quot;1.99.2&quot;</code>
   */
  public static String getLibraryVersion() {
    return "4.11.2";
  }
  // public void libraryVersion(final bad java simple ref int series, final bad java simple ref int
  // major, final bad java simple ref int minor) ;
  /**
   * Returns a raw CGI environment variable.
   *
   * <p>Retrieves the value for the given CGI environment variable (like <code>
   * &quot;SSL_CLIENT_S_DN_CN&quot;</code>), if it is defined, otherwise an empty string.
   *
   * <p>
   *
   * @see WEnvironment#getServerSignature()
   * @see WEnvironment#getServerSoftware()
   * @see WEnvironment#getServerAdmin()
   */
  public String getCgiValue(final String varName) {
    if (varName.equals("QUERY_STRING")) {
      return this.queryString_;
    } else {
      return this.session_.getCgiValue(varName);
    }
  }
  /**
   * The type of the content provided to the browser.
   *
   * <p>This is here for backwards compatibility, but the implementation now alwasy returns HTML5.
   */
  public HtmlContentType getContentType() {
    return HtmlContentType.HTML5;
  }
  /**
   * Returns the user agent type.
   *
   * <p>This returns an interpretation of the {@link WEnvironment#getUserAgent() getUserAgent()}. It
   * should be used only for user-agent specific work-arounds (as a last resort).
   *
   * <p>
   *
   * @see WEnvironment#agentIsIE()
   * @see WEnvironment#agentIsOpera()
   * @see WEnvironment#agentIsGecko()
   * @see WEnvironment#agentIsChrome()
   * @see WEnvironment#agentIsSafari()
   * @see WEnvironment#agentIsWebKit()
   */
  public UserAgent getAgent() {
    return this.agent_;
  }
  /**
   * Returns whether the user agent is Microsoft Internet Explorer.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsIE() {
    return (int) this.agent_.getValue() >= (int) UserAgent.IEMobile.getValue()
        && (int) this.agent_.getValue() < (int) UserAgent.Opera.getValue();
  }
  /**
   * Returns whether the user agent is an older version of IE.
   *
   * <p>Returns whether the agent is an IE version older than the given version.
   *
   * <p>
   *
   * @see WEnvironment#agentIsIE()
   */
  public boolean agentIsIElt(int version) {
    if (this.agentIsIE()) {
      return (int) this.agent_.getValue() < (int) UserAgent.IEMobile.getValue() + (version - 5);
    } else {
      return false;
    }
  }
  /**
   * Returns whether the user agent is Internet Explorer Mobile.
   *
   * <p>Returns also <code>true</code> when the agent is Internet Explorer 5 or older.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsIEMobile() {
    return this.agent_ == UserAgent.IEMobile;
  }
  /**
   * Returns whether the user agent is Opera.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsOpera() {
    return (int) this.agent_.getValue() >= (int) UserAgent.Opera.getValue()
        && (int) this.agent_.getValue() < (int) UserAgent.Safari.getValue();
  }
  /**
   * Returns whether the user agent is WebKit-based.
   *
   * <p>Webkit-based browsers include Safari, Chrome, Arora and Konquerer browsers.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsWebKit() {
    return (int) this.agent_.getValue() >= (int) UserAgent.WebKit.getValue()
        && (int) this.agent_.getValue() < (int) UserAgent.Konqueror.getValue();
  }
  /**
   * Returns whether the user agent is Mobile WebKit-based.
   *
   * <p>Mobile Webkit-based browsers include the Android Mobile WebKit and the iPhone Mobile WebKit
   * browsers.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsMobileWebKit() {
    return (int) this.agent_.getValue() >= (int) UserAgent.MobileWebKit.getValue()
        && (int) this.agent_.getValue() < (int) UserAgent.Konqueror.getValue();
  }
  /**
   * Returns whether the user agent is Safari.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsSafari() {
    return (int) this.agent_.getValue() >= (int) UserAgent.Safari.getValue()
        && (int) this.agent_.getValue() < (int) UserAgent.Chrome0.getValue();
  }
  /**
   * Returns whether the user agent is Chrome.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsChrome() {
    return (int) this.agent_.getValue() >= (int) UserAgent.Chrome0.getValue()
        && (int) this.agent_.getValue() < (int) UserAgent.Konqueror.getValue();
  }
  /**
   * Returns whether the user agent is Gecko-based.
   *
   * <p>Gecko-based browsers include Firefox.
   *
   * <p>
   *
   * @see WEnvironment#getAgent()
   */
  public boolean agentIsGecko() {
    return (int) this.agent_.getValue() >= (int) UserAgent.Gecko.getValue()
        && (int) this.agent_.getValue() < (int) UserAgent.BotAgent.getValue();
  }
  /**
   * Returns the servlet.
   *
   * <p>This returns the servlet environment of this session.
   */
  public WtServlet getServer() {
    return this.session_.getController();
  }
  /**
   * Returns whether internal paths are implemented using URI fragments.
   *
   * <p>This may be the case for older non-HTML5 browsers which do not support HTML5 History APIs.
   */
  public boolean isInternalPathUsingFragments() {
    return this.internalPathUsingFragments_;
  }
  /** Returns whether this agent supports CSS3 animations. */
  public boolean supportsCss3Animations() {
    return this.agentIsGecko()
            && (int) this.agent_.getValue() >= (int) UserAgent.Firefox5_0.getValue()
        || this.agentIsIE() && (int) this.agent_.getValue() >= (int) UserAgent.IE10.getValue()
        || this.agentIsWebKit();
  }

  Signal1<WDialog> dialogExecuted() {
    throw new WException("Internal error");
  }

  Signal1<WPopupMenu> popupExecuted() {
    throw new WException("Internal error");
  }
  /** Returns whether this is a mocked test environment. */
  public boolean isTest() {
    return false;
  }

  WebSession session_;
  boolean doesAjax_;
  boolean doesCookies_;
  boolean internalPathUsingFragments_;
  UserAgent agent_;
  int screenWidth_;
  int screenHeight_;
  double dpiScale_;
  String queryString_;
  boolean webGLsupported_;
  Map<String, String[]> parameters_;
  Map<String, String> cookies_;
  Locale locale_;
  Duration timeZoneOffset_;
  protected String timeZoneName_;
  String host_;
  String userAgent_;
  String urlScheme_;
  String referer_;
  String accept_;
  String serverSignature_;
  String serverSoftware_;
  String serverAdmin_;
  String clientAddress_;
  String internalPath_;
  String publicDeploymentPath_;
  protected String redirectSecret_;

  WEnvironment() {
    this.session_ = null;
    this.doesAjax_ = false;
    this.doesCookies_ = false;
    this.internalPathUsingFragments_ = false;
    this.screenWidth_ = -1;
    this.screenHeight_ = -1;
    this.dpiScale_ = 1;
    this.queryString_ = "";
    this.webGLsupported_ = false;
    this.parameters_ = new HashMap<String, String[]>();
    this.cookies_ = new HashMap<String, String>();
    this.locale_ = new Locale("");
    this.timeZoneOffset_ = Duration.ofMinutes(0);
    this.timeZoneName_ = "";
    this.host_ = "";
    this.userAgent_ = "";
    this.urlScheme_ = "";
    this.referer_ = "";
    this.accept_ = "";
    this.serverSignature_ = "";
    this.serverSoftware_ = "";
    this.serverAdmin_ = "";
    this.clientAddress_ = "";
    this.internalPath_ = "";
    this.publicDeploymentPath_ = "";
    this.redirectSecret_ = "";
  }

  void setUserAgent(final String userAgent) {
    this.userAgent_ = userAgent;
    final Configuration conf = this.session_.getController().getConfiguration();
    this.agent_ = UserAgent.Unknown;
    if (this.userAgent_.indexOf("Trident/4.0") != -1) {
      this.agent_ = UserAgent.IE8;
      return;
    }
    if (this.userAgent_.indexOf("Trident/5.0") != -1) {
      this.agent_ = UserAgent.IE9;
      return;
    } else {
      if (this.userAgent_.indexOf("Trident/6.0") != -1) {
        this.agent_ = UserAgent.IE10;
        return;
      } else {
        if (this.userAgent_.indexOf("Trident/") != -1) {
          this.agent_ = UserAgent.IE11;
          return;
        } else {
          if (this.userAgent_.indexOf("MSIE 2.") != -1
              || this.userAgent_.indexOf("MSIE 3.") != -1
              || this.userAgent_.indexOf("MSIE 4.") != -1
              || this.userAgent_.indexOf("MSIE 5.") != -1
              || this.userAgent_.indexOf("IEMobile") != -1) {
            this.agent_ = UserAgent.IEMobile;
          } else {
            if (this.userAgent_.indexOf("MSIE 6.") != -1) {
              this.agent_ = UserAgent.IE6;
            } else {
              if (this.userAgent_.indexOf("MSIE 7.") != -1) {
                this.agent_ = UserAgent.IE7;
              } else {
                if (this.userAgent_.indexOf("MSIE 8.") != -1) {
                  this.agent_ = UserAgent.IE8;
                } else {
                  if (this.userAgent_.indexOf("MSIE 9.") != -1) {
                    this.agent_ = UserAgent.IE9;
                  } else {
                    if (this.userAgent_.indexOf("MSIE") != -1) {
                      this.agent_ = UserAgent.IE10;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    if (this.userAgent_.indexOf("Opera") != -1) {
      this.agent_ = UserAgent.Opera;
      int t = this.userAgent_.indexOf("Version/");
      if (t != -1) {
        String vs = this.userAgent_.substring(t + 8);
        t = vs.indexOf(' ');
        if (t != -1) {
          vs = vs.substring(0, 0 + t);
        }
        try {
          double v = Double.parseDouble(vs);
          if (v >= 10) {
            this.agent_ = UserAgent.Opera10;
          }
        } catch (final RuntimeException e) {
        }
      }
    }
    if (this.userAgent_.indexOf("Chrome") != -1) {
      if (this.userAgent_.indexOf("Android") != -1) {
        this.agent_ = UserAgent.MobileWebKitAndroid;
      } else {
        if (this.userAgent_.indexOf("Chrome/0.") != -1) {
          this.agent_ = UserAgent.Chrome0;
        } else {
          if (this.userAgent_.indexOf("Chrome/1.") != -1) {
            this.agent_ = UserAgent.Chrome1;
          } else {
            if (this.userAgent_.indexOf("Chrome/2.") != -1) {
              this.agent_ = UserAgent.Chrome2;
            } else {
              if (this.userAgent_.indexOf("Chrome/3.") != -1) {
                this.agent_ = UserAgent.Chrome3;
              } else {
                if (this.userAgent_.indexOf("Chrome/4.") != -1) {
                  this.agent_ = UserAgent.Chrome4;
                } else {
                  this.agent_ = UserAgent.Chrome5;
                }
              }
            }
          }
        }
      }
    } else {
      if (this.userAgent_.indexOf("Safari") != -1) {
        if (this.userAgent_.indexOf("iPhone") != -1 || this.userAgent_.indexOf("iPad") != -1) {
          this.agent_ = UserAgent.MobileWebKitiPhone;
        } else {
          if (this.userAgent_.indexOf("Android") != -1) {
            this.agent_ = UserAgent.MobileWebKitAndroid;
          } else {
            if (this.userAgent_.indexOf("Mobile") != -1) {
              this.agent_ = UserAgent.MobileWebKit;
            } else {
              if (this.userAgent_.indexOf("Version") == -1) {
                if (this.userAgent_.indexOf("Arora") != -1) {
                  this.agent_ = UserAgent.Arora;
                } else {
                  this.agent_ = UserAgent.Safari;
                }
              } else {
                if (this.userAgent_.indexOf("Version/3") != -1) {
                  this.agent_ = UserAgent.Safari3;
                } else {
                  this.agent_ = UserAgent.Safari4;
                }
              }
            }
          }
        }
      } else {
        if (this.userAgent_.indexOf("WebKit") != -1) {
          if (this.userAgent_.indexOf("iPhone") != -1) {
            this.agent_ = UserAgent.MobileWebKitiPhone;
          } else {
            this.agent_ = UserAgent.WebKit;
          }
        } else {
          if (this.userAgent_.indexOf("Konqueror") != -1) {
            this.agent_ = UserAgent.Konqueror;
          } else {
            if (this.userAgent_.indexOf("Gecko") != -1) {
              this.agent_ = UserAgent.Gecko;
            }
          }
        }
      }
    }
    if (this.userAgent_.indexOf("Firefox") != -1) {
      if (this.userAgent_.indexOf("Firefox/0.") != -1) {
        this.agent_ = UserAgent.Firefox;
      } else {
        if (this.userAgent_.indexOf("Firefox/1.") != -1) {
          this.agent_ = UserAgent.Firefox;
        } else {
          if (this.userAgent_.indexOf("Firefox/2.") != -1) {
            this.agent_ = UserAgent.Firefox;
          } else {
            if (this.userAgent_.indexOf("Firefox/3.0") != -1) {
              this.agent_ = UserAgent.Firefox3_0;
            } else {
              if (this.userAgent_.indexOf("Firefox/3.1") != -1) {
                this.agent_ = UserAgent.Firefox3_1;
              } else {
                if (this.userAgent_.indexOf("Firefox/3.1b") != -1) {
                  this.agent_ = UserAgent.Firefox3_1b;
                } else {
                  if (this.userAgent_.indexOf("Firefox/3.5") != -1) {
                    this.agent_ = UserAgent.Firefox3_5;
                  } else {
                    if (this.userAgent_.indexOf("Firefox/3.6") != -1) {
                      this.agent_ = UserAgent.Firefox3_6;
                    } else {
                      if (this.userAgent_.indexOf("Firefox/4.") != -1) {
                        this.agent_ = UserAgent.Firefox4_0;
                      } else {
                        this.agent_ = UserAgent.Firefox5_0;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    if (this.userAgent_.indexOf("Edge/") != -1) {
      this.agent_ = UserAgent.Edge;
    }
    if (conf.agentIsBot(this.userAgent_)) {
      this.agent_ = UserAgent.BotAgent;
    }
  }

  void setInternalPath(final String path) {
    if (path.length() == 0) {
      this.internalPath_ = path;
    } else {
      this.internalPath_ = StringUtils.prepend(path, '/');
    }
  }

  WEnvironment(WebSession session) {
    this.session_ = session;
    this.doesAjax_ = false;
    this.doesCookies_ = false;
    this.internalPathUsingFragments_ = false;
    this.screenWidth_ = -1;
    this.screenHeight_ = -1;
    this.dpiScale_ = 1;
    this.queryString_ = "";
    this.webGLsupported_ = false;
    this.parameters_ = new HashMap<String, String[]>();
    this.cookies_ = new HashMap<String, String>();
    this.locale_ = new Locale("");
    this.timeZoneOffset_ = Duration.ofMinutes(0);
    this.timeZoneName_ = "";
    this.host_ = "";
    this.userAgent_ = "";
    this.urlScheme_ = "";
    this.referer_ = "";
    this.accept_ = "";
    this.serverSignature_ = "";
    this.serverSoftware_ = "";
    this.serverAdmin_ = "";
    this.clientAddress_ = "";
    this.internalPath_ = "";
    this.publicDeploymentPath_ = "";
    this.redirectSecret_ = "";
  }

  void init(final WebRequest request) {
    final Configuration conf = this.session_.getController().getConfiguration();
    this.queryString_ = request.getQueryString();
    this.parameters_ = request.getParameterMap();
    this.host_ = str(request.getHeaderValue("Host"));
    this.referer_ = str(request.getHeaderValue("Referer"));
    this.accept_ = str(request.getHeaderValue("Accept"));
    this.serverSignature_ = str("");
    this.serverSoftware_ = str("");
    this.serverAdmin_ = str("");
    this.redirectSecret_ = this.session_.getController().getRedirectSecret(request);
    this.setUserAgent(str(request.getHeaderValue("User-Agent")));
    this.updateUrlScheme(request);
    logger.info(new StringWriter().append("UserAgent: ").append(this.userAgent_).toString());
    if (conf.isBehindReverseProxy() || conf.isTrustedProxy(request.getRemoteAddr())) {
      String forwardedHost = str(request.getHeaderValue("X-Forwarded-Host"));
      if (forwardedHost.length() != 0) {
        int i = forwardedHost.lastIndexOf(',');
        if (i == -1) {
          this.host_ = forwardedHost;
        } else {
          this.host_ = forwardedHost.substring(i + 1);
        }
      }
    }
    if (this.host_.length() == 0) {
      this.host_ = request.getServerName();
      if ((request.getServerPort() + "").length() != 0) {
        this.host_ += ":" + (request.getServerPort() + "");
      }
    }
    this.clientAddress_ = request.getClientAddress(conf);
    String cookie = request.getHeaderValue("Cookie");
    this.doesCookies_ = cookie != null;
    if (cookie != null) {
      parseCookies(cookie, this.cookies_);
    }
    this.locale_ = request.getLocale();
  }

  void updateHostName(final WebRequest request) {
    final Configuration conf = this.session_.getController().getConfiguration();
    String newHost = request.getHostName(conf);
    if (newHost.length() != 0) {
      this.host_ = newHost;
    }
  }

  void updateUrlScheme(final WebRequest request) {
    final Configuration conf = this.session_.getController().getConfiguration();
    this.urlScheme_ = request.getUrlScheme(conf);
  }

  void enableAjax(final WebRequest request) {
    this.doesAjax_ = true;
    this.session_.getController().newAjaxSession();
    this.doesCookies_ = request.getHeaderValue("Cookie") != null;
    if (!(request.getParameter("htmlHistory") != null)) {
      this.internalPathUsingFragments_ = true;
    }
    String scaleE = request.getParameter("scale");
    try {
      this.dpiScale_ = scaleE != null ? Double.parseDouble(scaleE) : 1;
    } catch (final RuntimeException e) {
      this.dpiScale_ = 1;
    }
    String webGLE = request.getParameter("webGL");
    this.webGLsupported_ = webGLE != null ? webGLE.equals("true") : false;
    String tzE = request.getParameter("tz");
    try {
      this.timeZoneOffset_ = Duration.ofMinutes(tzE != null ? Integer.parseInt(tzE) : 0);
    } catch (final RuntimeException e) {
    }
    String tzSE = request.getParameter("tzS");
    this.timeZoneName_ = tzSE != null ? tzSE : "";
    String hashE = request.getParameter("_");
    if (hashE != null) {
      this.setInternalPath(hashE);
    }
    String deployPathE = request.getParameter("deployPath");
    if (deployPathE != null) {
      this.publicDeploymentPath_ = deployPathE;
      int s = this.publicDeploymentPath_.indexOf('/');
      if (s != 0) {
        this.publicDeploymentPath_ = "";
      }
    }
    String scrWE = request.getParameter("scrW");
    if (scrWE != null) {
      try {
        this.screenWidth_ = Integer.parseInt(scrWE);
      } catch (final RuntimeException e) {
      }
    }
    String scrHE = request.getParameter("scrH");
    if (scrHE != null) {
      try {
        this.screenHeight_ = Integer.parseInt(scrHE);
      } catch (final RuntimeException e) {
      }
    }
  }

  boolean agentSupportsAjax() {
    final Configuration conf = this.session_.getController().getConfiguration();
    return conf.agentSupportsAjax(this.userAgent_);
  }

  private static void parseCookies(final String cookie, final Map<String, String> result) {
    try {
      List<String> list = new ArrayList<String>();
      StringUtils.split(list, cookie, ";", false);
      for (int i = 0; i < list.size(); ++i) {
        int e = list.get(i).indexOf('=');
        if (e == -1) {
          continue;
        }
        String cookieName = list.get(i).substring(0, 0 + e);
        String cookieValue =
            e != -1 && list.get(i).length() > e + 1 ? list.get(i).substring(e + 1) : "";
        cookieName = cookieName.trim();
        cookieValue = cookieValue.trim();
        cookieName = java.net.URLDecoder.decode(cookieName, "UTF-8");
        ;
        cookieValue = java.net.URLDecoder.decode(cookieValue, "UTF-8");
        ;
        if (!cookieName.equals("")) {
          result.put(cookieName, cookieValue);
        }
      }
    } catch (UnsupportedEncodingException uee) {
      logger.info("Ignoring exception {}", uee.getMessage(), uee);
    }
  }

  static String str(String s) {
    return s != null ? s : "";
  }
}
