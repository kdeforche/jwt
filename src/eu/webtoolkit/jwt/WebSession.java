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
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WebSession {
  private static Logger logger = LoggerFactory.getLogger(WebSession.class);

  enum State {
    JustCreated,
    ExpectLoad,
    Loaded,
    Suspended,
    Dead;

    /** Returns the numerical representation of this enum. */
    public int getValue() {
      return ordinal();
    }
  }

  public WebSession(
      WtServlet controller,
      final String sessionId,
      EntryPointType type,
      final String favicon,
      WebRequest request,
      WEnvironment env) {
    this.mutex_ = new ReentrantLock();
    this.eventQueueMutex_ = new ReentrantLock();
    this.eventQueue_ = new LinkedList<ApplicationEvent>();
    this.type_ = type;
    this.favicon_ = favicon;
    this.state_ = WebSession.State.JustCreated;
    this.sessionId_ = sessionId;
    this.sessionIdCookie_ = "";
    this.multiSessionId_ = "";
    this.sessionIdChanged_ = false;
    this.sessionIdCookieChanged_ = false;
    this.sessionIdInUrl_ = false;
    this.controller_ = controller;
    this.renderer_ = new WebRenderer(this);
    this.applicationName_ = "";
    this.bookmarkUrl_ = "";
    this.basePath_ = "";
    this.absoluteBaseUrl_ = "";
    this.applicationUrl_ = "";
    this.deploymentPath_ = "";
    this.docRoot_ = "";
    this.redirect_ = "";
    this.pagePathInfo_ = "";
    this.asyncResponse_ = null;
    this.webSocket_ = null;
    this.bootStyleResponse_ = null;
    this.canWriteWebSocket_ = false;
    this.webSocketConnected_ = false;
    this.pollRequestsIgnored_ = 0;
    this.progressiveBoot_ = false;
    this.deferredRequest_ = null;
    this.deferredResponse_ = null;
    this.deferCount_ = 0;
    this.recursiveEvent_ = this.mutex_.newCondition();
    this.recursiveEventDone_ = this.mutex_.newCondition();
    this.newRecursiveEvent_ = null;
    this.updatesPendingEvent_ = this.mutex_.newCondition();
    this.updatesPending_ = false;
    this.triggerUpdate_ = false;
    this.embeddedEnv_ = new WEnvironment(this);
    this.app_ = null;
    this.debug_ = this.controller_.getConfiguration().debug();
    this.handlers_ = new ArrayList<WebSession.Handler>();
    this.recursiveEventHandler_ = null;
    this.env_ = env != null ? env : this.embeddedEnv_;
    if (request != null) {
      this.env_.updateUrlScheme(request);
    }
    if (request != null) {
      this.applicationUrl_ = request.getScriptName();
    } else {
      this.applicationUrl_ = "/";
    }
    this.deploymentPath_ = this.applicationUrl_;
    int slashpos = this.deploymentPath_.lastIndexOf('/');
    if (slashpos != -1) {
      this.basePath_ = this.deploymentPath_.substring(0, 0 + slashpos + 1);
      this.applicationName_ = this.deploymentPath_.substring(slashpos + 1);
    } else {
      this.basePath_ = "";
      this.applicationName_ = this.applicationUrl_;
    }
    if (this.controller_.getConfiguration().sessionIdCookie()) {
      this.sessionIdCookie_ = MathUtils.randomId();
      this.sessionIdCookieChanged_ = true;
      javax.servlet.http.Cookie cookie =
          new javax.servlet.http.Cookie("Wt" + this.sessionIdCookie_, "1");
      cookie.setSecure(this.env_.getUrlScheme().equals("https"));
      cookie.setHttpOnly(true);
      this.getRenderer().setCookie(cookie);
    }
  }

  public WebSession(
      WtServlet controller,
      final String sessionId,
      EntryPointType type,
      final String favicon,
      WebRequest request) {
    this(controller, sessionId, type, favicon, request, (WEnvironment) null);
  }

  public void destruct() {
    if (this.asyncResponse_ != null) {
      this.asyncResponse_.flush();
      this.asyncResponse_ = null;
    }
    if (this.deferredResponse_ != null) {
      this.deferredResponse_.flush();
      this.deferredResponse_ = null;
    }
    this.mutex_.lock();
    this.updatesPendingEvent_.signal();
    this.mutex_.unlock();
    this.flushBootStyleResponse();
  }

  public static WebSession getInstance() {
    WebSession.Handler handler = WebSession.Handler.getInstance();
    return handler != null ? handler.getSession() : null;
  }

  public boolean isAttachThreadToLockedHandler() {
    Handler.attachThreadToHandler(
        new WebSession.Handler(this, WebSession.Handler.LockOption.NoLock));
    return true;
  }

  public EntryPointType getType() {
    return this.type_;
  }

  public String getFavicon() {
    return this.favicon_;
  }

  public String getDocType() {
    final boolean xhtml = this.env_.getContentType() == HtmlContentType.XHTML1;
    if (xhtml) {
      return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
    } else {
      return "<!DOCTYPE html>";
    }
  }

  public String getSessionId() {
    return this.sessionId_;
  }

  public String getMultiSessionId() {
    return this.multiSessionId_;
  }

  public void setMultiSessionId(final String multiSessionId) {
    this.multiSessionId_ = multiSessionId;
  }

  public WtServlet getController() {
    return this.controller_;
  }

  public WEnvironment getEnv() {
    return this.env_;
  }

  public WApplication getApp() {
    return this.app_;
  }

  public WebRenderer getRenderer() {
    return this.renderer_;
  }

  public boolean isUseUrlRewriting() {
    final Configuration conf = this.controller_.getConfiguration();
    return !(conf.getSessionTracking() == Configuration.SessionTracking.CookiesURL
        && this.env_.supportsCookies());
  }

  public boolean isDebug() {
    return this.debug_;
  }

  public void redirect(final String url) {
    this.redirect_ = url;
    if (this.redirect_.length() == 0) {
      this.redirect_ = "?";
    }
  }

  public String getRedirect() {
    String result = this.redirect_;
    this.redirect_ = "";
    return result;
  }

  public void setApplication(WApplication app) {
    this.app_ = app;
  }

  public void externalNotify(final WEvent.Impl event) {
    try {
      if (this.recursiveEventHandler_ != null && !(this.newRecursiveEvent_ != null)) {
        this.newRecursiveEvent_ = new WEvent.Impl(event);
        this.recursiveEvent_.signal();
        while (this.newRecursiveEvent_ != null) {
          this.recursiveEventDone_.awaitUninterruptibly();
        }
      } else {
        if (this.app_ != null) {
          this.app_.notify(new WEvent(event));
        } else {
          this.notify(new WEvent(event));
        }
      }
    } catch (IOException ioe) {
      logger.info("Ignoring exception {}", ioe.getMessage(), ioe);
    }
  }

  public void notify(final WEvent event) throws IOException {
    if (event.impl_.response != null) {
      try {
        this.renderer_.serveResponse(event.impl_.response);
      } catch (final RuntimeException e) {
        logger.error(
            new StringWriter()
                .append("Exception in WApplication::notify(): ")
                .append(e.toString())
                .toString());
        logger.error("Exception: {}", e.getMessage(), e);
      }
      return;
    }
    if (event.impl_.function != null) {
      try {
        event.impl_.function.run();
        ;
        if (event.impl_.handler.getRequest() != null) {
          this.render(event.impl_.handler);
        }
      } catch (final RuntimeException e) {
        logger.error(
            new StringWriter()
                .append("Exception in WApplication::notify(): ")
                .append(e.toString())
                .toString());
        logger.error("Exception: {}", e.getMessage(), e);
      }
      return;
    }
    final WebSession.Handler handler = event.impl_.handler;
    if (!(handler.getResponse() != null)) {
      return;
    }
    final WebRequest request = handler.getRequest();
    final WebResponse response = handler.getResponse();
    if (WebSession.Handler.getInstance() != handler) {
      WebSession.Handler.getInstance().setRequest(request, response);
    }
    if (event.impl_.renderOnly) {
      this.render(handler);
      return;
    }
    String requestE = request.getParameter("request");
    if (requestE != null && requestE.equals("jserror")) {
      String err = request.getParameter("err");
      if (err != null) {
        this.app_.handleJavaScriptError(err);
      } else {
        logger.error(
            new StringWriter()
                .append("malformed jserror request: missing err parameter")
                .toString());
        this.app_.handleJavaScriptError("unknown error");
      }
      this.renderer_.setJSSynced(false);
      this.render(handler);
      return;
    }
    String pageIdE = request.getParameter("pageId");
    if (pageIdE != null && !pageIdE.equals(String.valueOf(this.renderer_.getPageId()))) {
      handler.getResponse().setContentType("text/javascript; charset=UTF-8");
      handler.getResponse().out().append("{}");
      handler.flushResponse();
      return;
    }
    switch (this.state_) {
      case JustCreated:
        this.render(handler);
        break;
      case ExpectLoad:
      case Loaded:
      case Suspended:
        if ((!(requestE != null) || !requestE.equals("resource"))
            && handler.getResponse().getResponseType() == WebRequest.ResponseType.Page) {
          if (!this.env_.agentIsIE()) {
            if (!str(handler.getRequest().getHeaderValue("User-Agent"))
                .equals(this.env_.getUserAgent())) {
              logger.warn(
                  new StringWriter()
                      .append("secure:")
                      .append("change of user-agent not allowed.")
                      .toString());
              logger.info(
                  new StringWriter()
                      .append("old user agent: ")
                      .append(this.env_.getUserAgent())
                      .toString());
              logger.info(
                  new StringWriter()
                      .append("new user agent: ")
                      .append(str(handler.getRequest().getHeaderValue("User-Agent")))
                      .toString());
              this.serveError(403, handler, "Forbidden");
              return;
            }
          }
          String ca = handler.getRequest().getClientAddress(this.controller_.getConfiguration());
          if (!ca.equals(this.env_.getClientAddress())) {
            boolean isInvalid = this.sessionIdCookie_.length() == 0;
            if (!isInvalid) {
              String cookie = str(request.getHeaderValue("Cookie"));
              if (cookie.indexOf("Wt" + this.sessionIdCookie_) == -1) {
                isInvalid = true;
              }
            }
            if (isInvalid) {
              logger.warn(
                  new StringWriter()
                      .append("secure:")
                      .append("change of IP address (")
                      .append(this.env_.getClientAddress())
                      .append(" -> ")
                      .append(ca)
                      .append(") not allowed.")
                      .toString());
              this.serveError(403, handler, "Forbidden");
              return;
            }
          }
        }
        if (this.sessionIdCookieChanged_) {
          String cookie = str(request.getHeaderValue("Cookie"));
          if (cookie.indexOf("Wt" + this.sessionIdCookie_) == -1) {
            this.sessionIdCookie_ = "";
            logger.info(new StringWriter().append("session id cookie not working").toString());
          }
          this.sessionIdCookieChanged_ = false;
        }
        if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script) {
          String sidE = request.getParameter("sid");
          if (!(sidE != null) || !sidE.equals(String.valueOf(this.renderer_.getScriptId()))) {
            throw new WException("Script id mismatch");
          }
          if (!this.env_.hasAjax()) {
            this.env_.enableAjax(request);
            this.app_.enableAjax();
            if (this.env_.getInternalPath().length() > 1) {
              this.changeInternalPath(this.env_.getInternalPath(), handler.getResponse());
            }
          } else {
            String hashE = request.getParameter("_");
            if (hashE != null) {
              this.changeInternalPath(hashE, handler.getResponse());
            }
          }
          this.render(handler);
        } else {
          try {
            if (0L != 0) {
              this.app_.requestTooLarge().trigger(0L);
            }
          } catch (final RuntimeException e) {
            logger.error(
                new StringWriter()
                    .append("Exception in WApplication::requestTooLarge")
                    .append(e.toString())
                    .toString());
            throw e;
          }
          String hashE = request.getParameter("_");
          WResource resource = null;
          if (!(requestE != null)) {
            if (request.getPathInfo().length() != 0) {
              resource =
                  this.app_.decodeExposedResource(
                      "/path/" + StringUtils.prepend(request.getPathInfo(), '/'));
            }
            if (!(resource != null) && hashE != null) {
              resource = this.app_.decodeExposedResource("/path/" + hashE);
            }
          }
          String resourceE = request.getParameter("resource");
          String signalE = this.getSignal(request, "");
          String verE = request.getParameter("ver");
          if (signalE != null) {
            this.progressiveBoot_ = false;
          }
          if (resource != null
              || requestE != null && requestE.equals("resource") && resourceE != null) {
            if (resourceE != null && resourceE.equals("blank")) {
              handler.getResponse().setContentType("text/html");
              handler
                  .getResponse()
                  .out()
                  .append("<html><head><title>bhm</title></head><body> </body></html>");
              handler.flushResponse();
            } else {
              if (!(resource != null)) {
                int ver = 0;
                try {
                  if (verE != null) {
                    ver = Integer.parseInt(verE);
                  }
                } catch (final RuntimeException e) {
                  ver = 0;
                }
                resource = this.app_.decodeExposedResource(resourceE, ver);
              }
              if (resource != null) {
                try {
                  resource.handle(request, response);
                  handler.setRequest((WebRequest) null, (WebResponse) null);
                } catch (final RuntimeException e) {
                  logger.error(
                      new StringWriter()
                          .append("Exception while streaming resource")
                          .append(e.toString())
                          .toString());
                  throw e;
                }
              } else {
                logger.error(
                    new StringWriter()
                        .append("decodeResource(): resource '")
                        .append(resourceE)
                        .append("' not exposed")
                        .toString());
                handler.getResponse().setStatus(404);
                handler.getResponse().setContentType("text/html");
                handler
                    .getResponse()
                    .out()
                    .append("<html><body><h1>Page not found.</h1></body></html>");
                handler.flushResponse();
              }
            }
          } else {
            this.env_.updateUrlScheme(request);
            if (signalE != null) {
              String ackIdE = request.getParameter("ackId");
              boolean invalidAckId = this.env_.hasAjax() && !request.isWebSocketMessage();
              WebRenderer.AckState ackState = WebRenderer.AckState.CorrectAck;
              if (invalidAckId && ackIdE != null) {
                try {
                  ackState = this.renderer_.ackUpdate((int) Integer.parseInt(ackIdE));
                  if (ackState != WebRenderer.AckState.BadAck) {
                    invalidAckId = false;
                  }
                } catch (final RuntimeException e) {
                }
              }
              if (invalidAckId) {
                if (!(ackIdE != null)) {
                  logger.warn(
                      new StringWriter().append("secure:").append("missing ackId").toString());
                } else {
                  logger.warn(
                      new StringWriter().append("secure:").append("invalid ackId").toString());
                }
                this.serveError(403, handler, "Forbidden");
                return;
              }
              if (signalE.equals("poll")
                  && ackState != WebRenderer.AckState.CorrectAck
                  && this.renderer_.isJsSynced()) {
                logger.debug(
                    new StringWriter()
                        .append("Ignoring poll with incorrect ack -- was rescheduled in browser?")
                        .toString());
                handler.flushResponse();
                return;
              }
              if (this.asyncResponse_ != null) {
                this.asyncResponse_.flush();
                this.asyncResponse_ = null;
              }
              if (signalE.equals("poll")) {
                if (!WtServlet.isAsyncSupported() && this.renderer_.isJsSynced()) {
                  this.updatesPendingEvent_.signal();
                  if (!this.updatesPending_) {
                    try {
                      this.updatesPendingEvent_.await(
                          this.controller_.getConfiguration().getServerPushTimeout() * 2,
                          java.util.concurrent.TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                    }
                  }
                  if (!this.updatesPending_) {
                    handler.flushResponse();
                    return;
                  }
                }
                if (!this.updatesPending_ && this.renderer_.isJsSynced()) {
                  if (!(this.webSocket_ != null) || this.pollRequestsIgnored_ == 2) {
                    if (this.webSocket_ != null) {
                      logger.info(
                          new StringWriter().append("discarding broken websocket").toString());
                      this.webSocket_.flush();
                      this.webSocket_ = null;
                    }
                    this.pollRequestsIgnored_ = 0;
                    this.asyncResponse_ = handler.getResponse();
                    handler.setRequest((WebRequest) null, (WebResponse) null);
                  } else {
                    ++this.pollRequestsIgnored_;
                    logger.debug(
                        new StringWriter()
                            .append("ignored poll request (#")
                            .append(String.valueOf(this.pollRequestsIgnored_))
                            .append(")")
                            .toString());
                  }
                } else {
                  this.pollRequestsIgnored_ = 0;
                }
              } else {
                if (!WtServlet.isAsyncSupported()) {
                  this.updatesPending_ = false;
                  this.updatesPendingEvent_.signal();
                }
              }
              if (handler.getRequest() != null) {
                logger.debug(new StringWriter().append("signal: ").append(signalE).toString());
                try {
                  handler.nextSignal = -1;
                  this.notifySignal(event);
                } catch (final RuntimeException e) {
                  logger.error(
                      new StringWriter()
                          .append("error during event handling: ")
                          .append(e.toString())
                          .toString());
                  throw e;
                }
              }
            }
            if (handler.getResponse() != null
                && handler.getResponse().getResponseType() == WebRequest.ResponseType.Page
                && (!this.env_.hasAjax()
                    || this.isSuspended()
                    || !this.controller_.getConfiguration().reloadIsNewSession())) {
              this.app_.getDomRoot().setRendered(false);
              this.env_.parameters_ = handler.getRequest().getParameterMap();
              if (hashE != null) {
                this.changeInternalPath(hashE, handler.getResponse());
              } else {
                if (handler.getRequest().getPathInfo().length() != 0) {
                  this.changeInternalPath(
                      handler.getRequest().getPathInfo(), handler.getResponse());
                } else {
                  this.changeInternalPath("", handler.getResponse());
                }
              }
            }
            if (!(signalE != null)) {
              if (this.getType() == EntryPointType.WidgetSet) {
                logger.error(
                    new StringWriter()
                        .append("bogus request: missing signal, discarding")
                        .toString());
                handler.flushResponse();
                return;
              }
              logger.info(new StringWriter().append("refreshing session").toString());
              this.flushBootStyleResponse();
              if (handler.getRequest() != null) {
                this.env_.parameters_ = handler.getRequest().getParameterMap();
                this.env_.updateHostName(handler.getRequest());
              }
              this.app_.refresh();
            }
            if (handler.getResponse() != null && !(this.recursiveEventHandler_ != null)) {
              this.render(handler);
            }
          }
        }
      case Dead:
        break;
    }
  }

  public void doRecursiveEventLoop() {
    try {
      WebSession.Handler handler = WebSession.Handler.getInstance();
      if (handler.getRequest() != null && !WtServlet.isAsyncSupported()) {
        throw new WException(
            "Recursive eventloop requires a Servlet 3.0 enabled servlet container and an application with async-supported enabled.");
      }
      if (handler.getRequest() != null) {
        handler.getSession().notifySignal(new WEvent(new WEvent.Impl(handler)));
      } else {
        if (this.app_.isUpdatesEnabled()) {
          this.app_.triggerUpdate();
        }
      }
      if (handler.getResponse() != null) {
        handler.getSession().render(handler);
      }
      if (this.state_ == WebSession.State.Dead) {
        this.recursiveEventHandler_ = null;
        throw new WException("doRecursiveEventLoop(): session was killed");
      }
      WebSession.Handler prevRecursiveEventHandler = this.recursiveEventHandler_;
      this.recursiveEventHandler_ = handler;
      this.newRecursiveEvent_ = null;
      while (!(this.newRecursiveEvent_ != null)) {
        this.recursiveEvent_.awaitUninterruptibly();
      }
      if (this.state_ == WebSession.State.Dead) {
        this.recursiveEventHandler_ = null;

        this.newRecursiveEvent_ = null;
        throw new WException("doRecursiveEventLoop(): session was killed");
      }
      this.setLoaded();
      this.app_.notify(new WEvent(this.newRecursiveEvent_));

      this.newRecursiveEvent_ = null;
      this.recursiveEventDone_.signal();
      this.recursiveEventHandler_ = prevRecursiveEventHandler;
    } catch (IOException ioe) {
      logger.info("Ignoring exception {}", ioe.getMessage(), ioe);
    }
  }

  public void deferRendering() {
    if (!(this.deferredRequest_ != null)) {
      WebSession.Handler handler = WebSession.Handler.getInstance();
      this.deferredRequest_ = handler.getRequest();
      this.deferredResponse_ = handler.getResponse();
      handler.setRequest((WebRequest) null, (WebResponse) null);
    }
    ++this.deferCount_;
  }

  public void resumeRendering() {
    if (--this.deferCount_ == 0) {
      WebSession.Handler handler = WebSession.Handler.getInstance();
      handler.setRequest(this.deferredRequest_, this.deferredResponse_);
      this.deferredRequest_ = null;
      this.deferredResponse_ = null;
    }
  }

  public void setTriggerUpdate(boolean update) {
    this.triggerUpdate_ = update;
  }

  public void expire() {
    this.kill();
  }

  public boolean isUnlockRecursiveEventLoop() {
    if (!(this.recursiveEventHandler_ != null)) {
      return false;
    }
    WebSession.Handler handler = WebSession.Handler.getInstance();
    this.recursiveEventHandler_.setRequest(handler.getRequest(), handler.getResponse());
    handler.setRequest((WebRequest) null, (WebResponse) null);
    this.newRecursiveEvent_ = new WEvent.Impl(this.recursiveEventHandler_);
    this.recursiveEvent_.signal();
    return true;
  }
  // public void pushEmitStack(WObject  obj) ;
  // public void popEmitStack() ;
  // public WObject  getEmitStackTop() ;
  public boolean isDead() {
    return this.state_ == WebSession.State.Dead;
  }

  public boolean isSuspended() {
    return this.state_ == WebSession.State.Suspended;
  }

  public WebSession.State getState() {
    return this.state_;
  }

  public void kill() {
    this.state_ = WebSession.State.Dead;
    this.isUnlockRecursiveEventLoop();
  }

  public boolean isProgressiveBoot() {
    return this.progressiveBoot_;
  }

  public String getApplicationName() {
    return this.applicationName_;
  }

  public String getApplicationUrl() {
    return this.applicationUrl_;
  }

  public String getDeploymentPath() {
    return this.deploymentPath_;
  }

  public boolean hasSessionIdInUrl() {
    return this.sessionIdInUrl_;
  }

  public void setSessionIdInUrl(boolean value) {
    this.sessionIdInUrl_ = value;
  }

  public boolean isUseUglyInternalPaths() {
    return false;
  }

  public void setPagePathInfo(final String path) {
    if (!this.isUseUglyInternalPaths()) {
      this.pagePathInfo_ = path;
    }
  }

  public String getPagePathInfo() {
    return this.pagePathInfo_;
  }

  public String getMostRelativeUrl(final String internalPath) {
    return this.appendSessionQuery(this.getBookmarkUrl(internalPath));
  }

  public final String getMostRelativeUrl() {
    return getMostRelativeUrl("");
  }

  public String appendInternalPath(final String baseUrl, final String internalPath) {
    if (internalPath.length() == 0 || internalPath.equals("/")) {
      if (baseUrl.length() == 0) {
        if (this.applicationName_.length() == 0) {
          return ".";
        } else {
          return this.applicationName_;
        }
      } else {
        return baseUrl;
      }
    } else {
      if (this.isUseUglyInternalPaths()) {
        return baseUrl + "?_=" + DomElement.urlEncodeS(internalPath, "#/");
      } else {
        if (this.applicationName_.length() == 0) {
          return baseUrl + DomElement.urlEncodeS(internalPath.substring(1), "#/");
        } else {
          return baseUrl + DomElement.urlEncodeS(internalPath, "#/");
        }
      }
    }
  }

  public String appendSessionQuery(final String url) {
    String result = url;
    if (this.env_.agentIsSpiderBot()) {
      return result;
    }
    int questionPos = result.indexOf('?');
    if (questionPos == -1) {
      result += this.getSessionQuery();
    } else {
      if (questionPos == result.length() - 1) {
        result += this.getSessionQuery().substring(1);
      } else {
        result += '&' + this.getSessionQuery().substring(1);
      }
    }
    if (result.startsWith("?")) {
      result = this.applicationUrl_ + result;
    }
    if (WebSession.Handler.getInstance().getResponse() != null) {
      return WebSession.Handler.getInstance().getResponse().encodeURL(result);
    } else {
      questionPos = result.indexOf('?');
      return result.substring(0, 0 + questionPos)
          + ";jsessionid="
          + this.getSessionId()
          + result.substring(questionPos);
    }
  }

  public String ajaxCanonicalUrl(final WebResponse request) {
    String hashE = null;
    if (this.applicationName_.length() == 0) {
      hashE = request.getParameter("_");
    }
    if (this.pagePathInfo_.length() != 0 || hashE != null && hashE.length() > 1) {
      String url = "";
      if (this.applicationName_.length() == 0) {
        url = this.fixRelativeUrl("?");
        url = url.substring(0, 0 + url.length() - 1);
      } else {
        url = this.fixRelativeUrl(this.applicationName_);
      }
      boolean firstParameter = true;
      for (Iterator<Map.Entry<String, String[]>> i_it =
              request.getParameterMap().entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String[]> i = i_it.next();
        if (!i.getKey().equals("_")) {
          url +=
              (firstParameter ? '?' : '&')
                  + Utils.urlEncode(i.getKey())
                  + '='
                  + Utils.urlEncode(i.getValue()[0]);
          firstParameter = false;
        }
      }
      url += '#' + (this.app_ != null ? this.app_.getInternalPath() : this.env_.getInternalPath());
      return url;
    } else {
      return "";
    }
  }

  enum BootstrapOption {
    ClearInternalPath,
    KeepInternalPath;

    /** Returns the numerical representation of this enum. */
    public int getValue() {
      return ordinal();
    }
  }

  public String getBootstrapUrl(final WebResponse response, WebSession.BootstrapOption option) {
    switch (option) {
      case KeepInternalPath:
        {
          String url = "";
          String internalPath =
              this.app_ != null ? this.app_.getInternalPath() : this.env_.getInternalPath();
          if (this.isUseUglyInternalPaths()) {
            if (internalPath.length() > 1) {
              url = "?_=" + DomElement.urlEncodeS(internalPath, "#/");
            }
            if (isAbsoluteUrl(this.applicationUrl_)) {
              url = this.applicationUrl_ + url;
            }
          } else {
            if (!isAbsoluteUrl(this.applicationUrl_)) {
              if (internalPath.length() > 1) {
                String lastPart = internalPath.substring(internalPath.lastIndexOf('/') + 1);
                url = "";
              } else {
                url = this.applicationName_;
              }
            } else {
              if (this.applicationName_.length() == 0 && internalPath.length() > 1) {
                internalPath = internalPath.substring(1);
              }
              url = this.applicationUrl_ + internalPath;
            }
          }
          return this.appendSessionQuery(url);
        }
      case ClearInternalPath:
        {
          String url = "";
          if (this.applicationName_.length() == 0) {
            url = this.fixRelativeUrl(".");
            url = url.substring(0, 0 + url.length() - 1);
          } else {
            url = this.fixRelativeUrl(this.applicationName_);
          }
          return this.appendSessionQuery(url);
        }
      default:
        assert false;
    }
    return "";
  }

  public String fixRelativeUrl(final String url) {
    if (isAbsoluteUrl(url)) {
      return url;
    }
    if (url.length() > 0 && url.charAt(0) == '#') {
      if (!isAbsoluteUrl(this.applicationUrl_)) {
        return url;
      } else {
        return this.applicationName_ + url;
      }
    }
    if (!isAbsoluteUrl(this.applicationUrl_)) {
      if (url.length() != 0 && url.charAt(0) == '/') {
        return url;
      } else {
        if (this.env_.publicDeploymentPath_.length() != 0) {
          String dp = this.env_.publicDeploymentPath_;
          if (url.length() == 0) {
            return dp;
          } else {
            if (url.charAt(0) == '?') {
              return dp + url;
            } else {
              int s = dp.lastIndexOf('/');
              String parentDir = dp.substring(0, 0 + s + 1);
              if (url.charAt(0) == '.'
                  && (url.length() == 1
                      || url.charAt(1) == '?'
                      || url.charAt(1) == '#'
                      || url.charAt(1) == ';')) {
                return parentDir + url.substring(1);
              } else {
                if (url.length() >= 2 && url.charAt(0) == '.' && url.charAt(1) == '/') {
                  return parentDir + url.substring(2);
                } else {
                  return parentDir + url;
                }
              }
            }
          }
        } else {
          if (this.env_.isInternalPathUsingFragments()) {
            return url;
          } else {
            String rel = "";
            String pi = this.pagePathInfo_;
            for (int i = 0; i < pi.length(); ++i) {
              if (pi.charAt(i) == '/') {
                rel += "../";
              }
            }
            if (url.length() == 0) {
              return rel + this.applicationName_;
            } else {
              return rel + url;
            }
          }
        }
      }
    } else {
      return this.makeAbsoluteUrl(url);
    }
  }

  public String makeAbsoluteUrl(final String url) {
    if (isAbsoluteUrl(url)) {
      return url;
    } else {
      if (url.length() != 0
          && url.charAt(0) == '.'
          && (url.length() == 1 || url.charAt(1) != '.')) {
        return this.absoluteBaseUrl_ + (url.charAt(1));
      } else {
        if (url.length() == 0 || url.charAt(0) != '/') {
          return this.absoluteBaseUrl_ + url;
        } else {
          return host(this.absoluteBaseUrl_) + url;
        }
      }
    }
  }

  public String getBookmarkUrl(final String internalPath) {
    String result = this.bookmarkUrl_;
    return this.appendInternalPath(result, internalPath);
  }

  public String getBookmarkUrl() {
    if (this.app_ != null) {
      return this.getBookmarkUrl(this.app_.getInternalPath());
    } else {
      return this.getBookmarkUrl(this.env_.getInternalPath());
    }
  }

  public String getCgiValue(final String varName) {
    WebRequest request = WebSession.Handler.getInstance().getRequest();
    if (request != null) {
      return str("");
    } else {
      if (varName.equals("DOCUMENT_ROOT")) {
        return this.docRoot_;
      } else {
        return "";
      }
    }
  }

  public String getCgiHeader(final String headerName) {
    WebRequest request = WebSession.Handler.getInstance().getRequest();
    if (request != null) {
      return str(request.getHeaderValue(headerName));
    } else {
      return "";
    }
  }

  public EventType getEventType(final WEvent event) {
    if (event.impl_.handler == null) {
      return EventType.Other;
    }
    final WebSession.Handler handler = event.impl_.handler;
    final WebRequest request = handler.getRequest();
    if (event.impl_.renderOnly || !(handler.getRequest() != null)) {
      return EventType.Other;
    }
    String pageIdE = handler.getRequest().getParameter("pageId");
    if (pageIdE != null && !pageIdE.equals(String.valueOf(this.renderer_.getPageId()))) {
      return EventType.Other;
    }
    switch (this.state_) {
      case ExpectLoad:
      case Loaded:
      case Suspended:
        if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script) {
          return EventType.Other;
        } else {
          if (this.resourceRequest(request)) {
            return EventType.Resource;
          } else {
            String signalE = this.getSignal(request, "");
            if (signalE != null) {
              if (signalE.equals("none")
                  || signalE.equals("load")
                  || signalE.equals("hash")
                  || signalE.equals("poll")
                  || signalE.equals("keepAlive")) {
                return EventType.Other;
              } else {
                List<Integer> signalOrder = this.getSignalProcessingOrder(event);
                int timerSignals = 0;
                for (int i = 0; i < signalOrder.size(); ++i) {
                  int signalI = signalOrder.get(i);
                  String se = signalI > 0 ? 'e' + String.valueOf(signalI) : "";
                  String s = this.getSignal(request, se);
                  if (!(s != null)) {
                    break;
                  } else {
                    if (signalE.equals("user")) {
                      return EventType.User;
                    } else {
                      AbstractEventSignal esb = this.decodeSignal(s, false);
                      if (!(esb != null)) {
                        continue;
                      }
                      WTimerWidget t = ObjectUtils.cast(esb.getOwner(), WTimerWidget.class);
                      if (t != null) {
                        ++timerSignals;
                      } else {
                        return EventType.User;
                      }
                    }
                  }
                }
                if (timerSignals != 0) {
                  return EventType.Timer;
                }
              }
            } else {
              return EventType.Other;
            }
          }
        }
      default:
        return EventType.Other;
    }
  }

  public void setState(WebSession.State state, int timeout) {
    if (this.state_ != WebSession.State.Dead) {
      this.state_ = state;
      logger.debug(
          new StringWriter()
              .append("Setting to expire in ")
              .append(String.valueOf(timeout))
              .append("s")
              .toString());
    }
  }

  static class Handler {
    private static Logger logger = LoggerFactory.getLogger(Handler.class);

    enum LockOption {
      NoLock,
      TryLock,
      TakeLock;

      /** Returns the numerical representation of this enum. */
      public int getValue() {
        return ordinal();
      }
    }

    public Handler() {
      this.nextSignal = -1;
      this.signalOrder = new ArrayList<Integer>();
      this.prevHandler_ = null;
      this.session_ = null;
      this.request_ = null;
      this.response_ = null;
      this.killed_ = false;
      this.init();
    }

    public Handler(final WebSession session, final WebRequest request, final WebResponse response) {
      this.nextSignal = -1;
      this.signalOrder = new ArrayList<Integer>();
      this.prevHandler_ = null;
      this.session_ = session;
      this.request_ = request;
      this.response_ = response;
      this.killed_ = false;
      session.getMutex().lock();
      this.init();
    }

    public Handler(final WebSession session, WebSession.Handler.LockOption lockOption) {
      this.nextSignal = -1;
      this.signalOrder = new ArrayList<Integer>();
      this.prevHandler_ = null;
      this.session_ = session;
      this.request_ = null;
      this.response_ = null;
      this.killed_ = false;
      switch (lockOption) {
        case NoLock:
          break;
        case TakeLock:
          session.getMutex().lock();
          break;
        case TryLock:
          session.getMutex().tryLock();
          break;
      }
      this.init();
    }

    public Handler(WebSession session) {
      this.nextSignal = -1;
      this.signalOrder = new ArrayList<Integer>();
      this.prevHandler_ = null;
      this.session_ = session;
      this.request_ = null;
      this.response_ = null;
      this.killed_ = false;
      session.getMutex().lock();
      this.init();
    }

    public void release() {
      if (this.isHaveLock()) {
        this.session_.processQueuedEvents(this);
        if (this.session_.triggerUpdate_) {
          this.session_.pushUpdates();
        }
        this.session_.getMutex().unlock();
      }
      attachThreadToHandler(this.prevHandler_);
    }

    public static WebSession.Handler getInstance() {
      return threadHandler_.get();
    }

    public boolean isHaveLock() {
      return this.session_.getMutex().isHeldByCurrentThread();
    }

    public void unlock() {
      if (this.isHaveLock()) {
        this.session_.getMutex().unlock();
      }
    }

    public void flushResponse() {
      if (this.response_ != null) {
        this.response_.flush();
        this.setRequest((WebRequest) null, (WebResponse) null);
      }
    }

    public WebResponse getResponse() {
      return this.response_;
    }

    public WebRequest getRequest() {
      return this.request_;
    }

    public WebSession getSession() {
      return this.session_;
    }

    public void setRequest(WebRequest request, WebResponse response) {
      this.request_ = request;
      this.response_ = response;
    }

    public int nextSignal;
    public List<Integer> signalOrder;

    static void attachThreadToSession(final WebSession session) {
      attachThreadToHandler((WebSession.Handler) null);
      if (!(session != null)) {
        return;
      }
      if (session.state_ == WebSession.State.Dead) {
        logger.warn(new StringWriter().append("attaching to dead session?").toString());
      }
      if (!session.isAttachThreadToLockedHandler()) {
        logger.warn(
            new StringWriter()
                .append("attachThread(): no thread is holding this application's lock ?")
                .toString());
        WebSession.Handler.attachThreadToHandler(
            new WebSession.Handler(session, WebSession.Handler.LockOption.NoLock));
      }
    }

    public static WebSession.Handler attachThreadToHandler(WebSession.Handler handler) {
      WebSession.Handler result;
      result = threadHandler_.get();
      threadHandler_.set(handler);
      return result;
    }

    private void init() {
      this.prevHandler_ = attachThreadToHandler(this);
    }

    private WebSession.Handler prevHandler_;
    private WebSession session_;
    private WebRequest request_;
    private WebResponse response_;
    private boolean killed_;
  }

  public void handleRequest(final WebSession.Handler handler) throws IOException {
    try {
      final WebRequest request = handler.getRequest();
      String wtdE = request.getParameter("wtd");
      final Configuration conf = this.controller_.getConfiguration();
      String origin = request.getHeaderValue("Origin");
      if (request.isWebSocketRequest()) {
        String trustedOrigin = this.env_.getUrlScheme() + "://" + this.env_.getHostName();
        if (origin != null
            && (trustedOrigin.equals(origin)
                || this.getType() == EntryPointType.WidgetSet && conf.isAllowedOrigin(origin))
            && wtdE != null
            && wtdE.equals(this.sessionId_)) {
        } else {
          if (origin != null) {
            logger.error(
                new StringWriter()
                    .append("WebSocket request refused: Origin '")
                    .append(origin)
                    .append("' not allowed (trusted origin is '")
                    .append(trustedOrigin)
                    .append("')")
                    .toString());
          } else {
            logger.error(
                new StringWriter().append("WebSocket request refused: missing Origin").toString());
          }
          handler.getResponse().setStatus(403);
          handler.flushResponse();
          return;
        }
      } else {
        if (origin != null) {
          if (this.getType() == EntryPointType.WidgetSet
              && (wtdE != null && wtdE.equals(this.sessionId_)
                  || this.state_ == WebSession.State.JustCreated)
              && conf.isAllowedOrigin(origin)) {
            if (isEqual(origin, "null")) {
              origin = "*";
            }
            handler.getResponse().addHeader("Access-Control-Allow-Origin", origin);
            handler.getResponse().addHeader("Access-Control-Allow-Credentials", "true");
            handler.getResponse().addHeader("Vary", "Origin");
            if (isEqual(request.getRequestMethod(), "OPTIONS")) {
              WebResponse response = handler.getResponse();
              response.setStatus(200);
              response.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
              response.addHeader("Access-Control-Max-Age", "1728000");
              String requestHeaders = request.getHeaderValue("Access-Control-Request-Headers");
              if (requestHeaders != null) {
                response.addHeader("Access-Control-Allow-Headers", requestHeaders);
              }
              handler.flushResponse();
              return;
            }
          }
        }
      }
      String requestE = request.getParameter("request");
      boolean requestForResource = this.resourceRequest(request);
      boolean requestForStyle = requestE != null && requestE.equals("style");
      if (requestE != null && requestE.equals("ws") && !request.isWebSocketRequest()) {
        logger.error(new StringWriter().append("invalid WebSocket request, ignoring").toString());
        logger.info(
            new StringWriter()
                .append("Connection: ")
                .append(str(request.getHeaderValue("Connection")))
                .toString());
        logger.info(
            new StringWriter()
                .append("Upgrade: ")
                .append(str(request.getHeaderValue("Upgrade")))
                .toString());
        logger.info(
            new StringWriter()
                .append("Sec-WebSocket-Version: ")
                .append(str(request.getHeaderValue("Sec-WebSocket-Version")))
                .toString());
        handler.flushResponse();
        return;
      }
      if (request.isWebSocketRequest()) {
        if (conf.webSockets()) {
          handler.getResponse().setStatus(500);
          handler.flushResponse();
          throw new WException("Server does not implement JSR-356 for WebSockets");
        }
      }
      handler.getResponse().setResponseType(WebRequest.ResponseType.Page);
      if (!(requestForResource
          || isEqual(request.getRequestMethod(), "POST")
          || isEqual(request.getRequestMethod(), "GET"))) {
        handler.getResponse().setStatus(400);
        handler.flushResponse();
        return;
      }
      if (this.env_.hasAjax()
          && isEqual(request.getRequestMethod(), "GET")
          && !requestForResource
          && !requestForStyle
          && conf.reloadIsNewSession()
          && !this.isSuspended()
          && wtdE != null
          && wtdE.equals(this.sessionId_)) {
        logger.warn(
            new StringWriter()
                .append("secure:")
                .append("Unexpected GET request with wtd of existing Ajax session")
                .toString());
        this.serveError(403, handler, "Forbidden");
        return;
      }
      if ((!(wtdE != null) || !wtdE.equals(this.sessionId_))
          && this.state_ != WebSession.State.JustCreated
          && (requestE != null
              && (requestE.equals("jsupdate")
                  || requestE.equals("jserror")
                  || requestE.equals("resource")))) {
        logger.debug(
            new StringWriter()
                .append("CSRF: ")
                .append(wtdE != null ? wtdE : "no wtd")
                .append(" != ")
                .append(this.sessionId_)
                .append(", requestE: ")
                .append(requestE != null ? requestE : "none")
                .toString());
        logger.warn(
            new StringWriter().append("secure:").append("CSRF prevention kicked in.").toString());
        this.serveError(403, handler, "Forbidden");
      } else {
        try {
          switch (this.state_) {
            case JustCreated:
              {
                if (conf.getSessionTracking() == Configuration.SessionTracking.Combined) {
                  this.getRenderer().updateMultiSessionCookie(request);
                }
                switch (this.type_) {
                  case Application:
                    {
                      this.init(request);
                      if (requestE != null) {
                        if (requestE.equals("jsupdate")
                            || requestE.equals("jserror")
                            || requestE.equals("script")) {
                          handler.getResponse().setResponseType(WebRequest.ResponseType.Update);
                          logger.info(
                              new StringWriter()
                                  .append("signal from dead session, sending reload.")
                                  .toString());
                          this.renderer_.letReloadJS(handler.getResponse(), true);
                          this.kill();
                          break;
                        } else {
                          if (!requestE.equals("page")) {
                            logger.info(
                                new StringWriter()
                                    .append("Not serving this: request of type '")
                                    .append(requestE)
                                    .append(
                                        "' in a brand new session (probably coming from an old session)")
                                    .toString());
                            handler.getResponse().setContentType("text/html");
                            handler
                                .getResponse()
                                .out()
                                .append("<html><head></head><body></body></html>");
                            this.kill();
                            break;
                          }
                        }
                      }
                      {
                        String internalPath = this.env_.getCookie("WtInternalPath");
                        if (internalPath != null) {
                          this.env_.setInternalPath(internalPath);
                        }
                      }
                      boolean forcePlain =
                          this.env_.agentIsSpiderBot() || !this.env_.agentSupportsAjax();
                      this.progressiveBoot_ =
                          !forcePlain && conf.progressiveBootstrap(this.env_.getInternalPath());
                      if (forcePlain || this.progressiveBoot_) {
                        if (!this.start(handler.getResponse())) {
                          throw new WException("Could not start application.");
                        }
                        this.app_.notify(new WEvent(new WEvent.Impl(handler)));
                        if (this.env_.agentIsSpiderBot()) {
                          this.kill();
                        } else {
                          if (this.controller_.limitPlainHtmlSessions()) {
                            logger.warn(
                                new StringWriter()
                                    .append("secure:")
                                    .append("DoS: plain HTML sessions being limited")
                                    .toString());
                            if (forcePlain) {
                              this.kill();
                            } else {
                              this.setState(WebSession.State.Loaded, conf.getBootstrapTimeout());
                            }
                          } else {
                            this.setLoaded();
                          }
                        }
                      } else {
                        this.serveResponse(handler);
                        this.setState(WebSession.State.Loaded, conf.getBootstrapTimeout());
                      }
                      break;
                    }
                  case WidgetSet:
                    if (requestForResource || requestForStyle) {
                      String resourceE = request.getParameter("resource");
                      if (resourceE != null && resourceE.equals("blank")) {
                        handler.getResponse().setContentType("text/html");
                        handler
                            .getResponse()
                            .out()
                            .append("<html><head><title>bhm</title></head><body> </body></html>");
                      } else {
                        logger.info(
                            new StringWriter()
                                .append("not starting session for unexpected request type.")
                                .toString());
                        handler.getResponse().setContentType("text/html");
                        handler
                            .getResponse()
                            .out()
                            .append("<html><head></head><body></body></html>");
                      }
                      this.kill();
                    } else {
                      handler.getResponse().setResponseType(WebRequest.ResponseType.Script);
                      this.init(request);
                      this.env_.enableAjax(request);
                      if (!this.start(handler.getResponse())) {
                        throw new WException("Could not start application.");
                      }
                      this.app_.notify(new WEvent(new WEvent.Impl(handler)));
                      this.setExpectLoad();
                    }
                    break;
                  default:
                    assert false;
                }
                break;
              }
            case ExpectLoad:
            case Loaded:
            case Suspended:
              {
                if (conf.getSessionTracking() == Configuration.SessionTracking.Combined) {
                  String signalE = handler.getRequest().getParameter("signal");
                  boolean isKeepAlive =
                      requestE != null && signalE != null && signalE.equals("keepAlive");
                  if (isKeepAlive || !this.env_.hasAjax()) {
                    this.getRenderer().updateMultiSessionCookie(request);
                  }
                }
                if (requestE != null) {
                  if (requestE.equals("jsupdate") || requestE.equals("jserror")) {
                    handler.getResponse().setResponseType(WebRequest.ResponseType.Update);
                  } else {
                    if (requestE.equals("script")) {
                      handler.getResponse().setResponseType(WebRequest.ResponseType.Script);
                      if (this.state_ == WebSession.State.Loaded) {
                        this.setExpectLoad();
                      }
                    } else {
                      if (requestE.equals("style")) {
                        this.flushBootStyleResponse();
                        String page = request.getParameter("page");
                        boolean ios5 =
                            this.env_.agentIsMobileWebKit()
                                && (this.env_.getUserAgent().indexOf("OS 5_") != -1
                                    || this.env_.getUserAgent().indexOf("OS 6_") != -1
                                    || this.env_.getUserAgent().indexOf("OS 7_") != -1
                                    || this.env_.getUserAgent().indexOf("OS 8_") != -1);
                        String jsE = request.getParameter("js");
                        boolean nojs = jsE != null && jsE.equals("no");
                        boolean bootStyle =
                            (this.app_ != null || !ios5 && !nojs)
                                && page != null
                                && page.equals(String.valueOf(this.renderer_.getPageId()));
                        if (!bootStyle) {
                          handler.getResponse().setContentType("text/css");
                          handler.flushResponse();
                        } else {
                          int i = 0;
                          final int MAX_TRIES = 1000;
                          while (!(this.app_ != null) && i < MAX_TRIES) {
                            this.mutex_.unlock();
                            ThreadUtils.sleep(Duration.ofMillis(5));
                            this.mutex_.lock();
                            ++i;
                          }
                          if (i < MAX_TRIES) {
                            this.renderer_.serveLinkedCss(handler.getResponse());
                          }
                          handler.flushResponse();
                        }
                        break;
                      }
                    }
                  }
                }
                if (!(this.app_ != null)) {
                  String resourceE = request.getParameter("resource");
                  if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script) {
                    this.env_.enableAjax(request);
                    if (!this.start(handler.getResponse())) {
                      throw new WException("Could not start application.");
                    }
                  } else {
                    if (requestForResource && resourceE != null && resourceE.equals("blank")) {
                      handler.getResponse().setContentType("text/html");
                      handler
                          .getResponse()
                          .out()
                          .append("<html><head><title>bhm</title></head><body> </body></html>");
                      break;
                    } else {
                      String jsE = request.getParameter("js");
                      if (jsE != null && jsE.equals("no")) {
                        if (!this.start(handler.getResponse())) {
                          throw new WException("Could not start application.");
                        }
                        if (this.controller_.limitPlainHtmlSessions()) {
                          logger.warn(
                              new StringWriter()
                                  .append("secure:")
                                  .append("DoS: plain HTML sessions being limited")
                                  .toString());
                          this.kill();
                        }
                      } else {
                        if (!conf.reloadIsNewSession()
                            && wtdE != null
                            && wtdE.equals(this.sessionId_)) {
                          this.serveResponse(handler);
                          this.setState(WebSession.State.Loaded, conf.getBootstrapTimeout());
                        } else {
                          handler.getResponse().setContentType("text/html");
                          handler
                              .getResponse()
                              .out()
                              .append("<html><body><h1>Refusing to respond.</h1></body></html>");
                        }
                        break;
                      }
                    }
                  }
                }
                boolean doNotify = false;
                if (handler.getRequest() != null) {
                  String signalE = handler.getRequest().getParameter("signal");
                  boolean isPoll = signalE != null && signalE.equals("poll");
                  if (requestForResource || isPoll || !this.isUnlockRecursiveEventLoop()) {
                    doNotify = true;
                    if (this.env_.hasAjax()) {
                      if (this.state_ != WebSession.State.ExpectLoad
                          && this.state_ != WebSession.State.Suspended
                          && handler.getResponse().getResponseType()
                              == WebRequest.ResponseType.Update) {
                        this.setLoaded();
                      }
                    } else {
                      if (this.state_ != WebSession.State.ExpectLoad
                          && !(this.state_ == WebSession.State.Suspended && requestForResource)
                          && !this.controller_.limitPlainHtmlSessions()) {
                        this.setLoaded();
                      }
                    }
                  }
                } else {
                  doNotify = false;
                }
                if (doNotify) {
                  this.app_.notify(new WEvent(new WEvent.Impl(handler)));
                  if (handler.getResponse() != null && !requestForResource) {
                    this.app_.notify(new WEvent(new WEvent.Impl(handler, true)));
                  }
                }
                break;
              }
            case Dead:
              logger.info(
                  new StringWriter().append("request to dead session, ignoring").toString());
              break;
          }
        } catch (final WException e) {
          logger.error(new StringWriter().append("fatal error: ").append(e.toString()).toString());
          logger.error("Exception: {}", e.getMessage(), e);
          this.kill();
          if (handler.getResponse() != null) {
            this.serveError(500, handler, "Internal Server Error");
          }
        } catch (final RuntimeException e) {
          logger.error(new StringWriter().append("fatal error: ").append(e.toString()).toString());
          logger.error("Exception: {}", e.getMessage(), e);
          this.kill();
          if (handler.getResponse() != null) {
            this.serveError(500, handler, "Internal Server Error");
          }
        }
      }
      if (handler.getResponse() != null) {
        handler.flushResponse();
      }
    } catch (InterruptedException ie) {
      logger.info("Ignoring exception {}", ie.getMessage(), ie);
    }
  }

  public ReentrantLock getMutex() {
    return this.mutex_;
  }

  public static ThreadLocal<WebSession.Handler> threadHandler_ =
      new ThreadLocal<WebSession.Handler>();

  public void setExpectLoad() {
    if (this.controller_.getConfiguration().ajaxPuzzle()) {
      this.setState(
          WebSession.State.ExpectLoad, this.controller_.getConfiguration().getBootstrapTimeout());
    } else {
      this.setLoaded();
    }
  }

  public void setLoaded() {
    boolean wasSuspended = this.state_ == WebSession.State.Suspended;
    this.setState(WebSession.State.Loaded, this.controller_.getConfiguration().getSessionTimeout());
    if (wasSuspended) {
      if (this.env_.hasAjax() && this.controller_.getConfiguration().reloadIsNewSession()) {
        this.app_.doJavaScript("Wt4.11.2.history.removeSessionId()");
        this.sessionIdInUrl_ = false;
      }
      this.app_.unsuspended().trigger();
    }
  }
  // public void generateNewSessionId() ;
  public void queueEvent(final ApplicationEvent event) {
    this.eventQueueMutex_.lock();
    this.eventQueue_.addLast(event);
    logger.debug(
        new StringWriter()
            .append("queueEvent(): ")
            .append(String.valueOf(this.eventQueue_.size()))
            .toString());
    this.eventQueueMutex_.unlock();
  }

  public void handleWebSocketMessage(final WebSession.Handler handler) throws IOException {
    WebRequest message = handler.getRequest();
    boolean closing = message.getContentLength() == 0;
    if (!closing) {
      String connectedE = message.getParameter("connected");
      if (connectedE != null) {
        this.renderer_.ackUpdate(Integer.parseInt(connectedE));
        this.webSocketConnected_ = true;
        this.canWriteWebSocket_ = true;
      }
      String wsRqIdE = message.getParameter("wsRqId");
      if (wsRqIdE != null) {
        int wsRqId = Integer.parseInt(wsRqIdE);
        this.renderer_.addWsRequestId(wsRqId);
      }
      String signalE = message.getParameter("signal");
      if (signalE != null && signalE.equals("ping")) {
        logger.debug(new StringWriter().append("ws: handle ping").toString());
        if (this.canWriteWebSocket_) {
          this.webSocket_.out().append("{}");
          this.webSocket_.flushBuffer();
          return;
        }
      }
      String pageIdE = message.getParameter("pageId");
      if (pageIdE != null && !pageIdE.equals(String.valueOf(this.renderer_.getPageId()))) {
        closing = true;
      }
      if (!closing) {
        this.handleRequest(handler);
      } else {
        this.webSocket_.flush();
      }
    }
  }

  private void checkTimers() {
    WContainerWidget timers = this.app_.getTimerRoot();
    final List<WWidget> timerWidgets = timers.getChildren();
    List<WTimerWidget> expired = new ArrayList<WTimerWidget>();
    for (int i = 0; i < timerWidgets.size(); ++i) {
      WTimerWidget wti = ObjectUtils.cast(timerWidgets.get(i), WTimerWidget.class);
      if (wti.isTimerExpired()) {
        expired.add(wti);
      }
    }
    WMouseEvent dummy = new WMouseEvent();
    for (int i = 0; i < expired.size(); ++i) {
      expired.get(i).clicked().trigger(dummy);
    }
  }

  private void hibernate() {
    if (this.app_ != null && this.app_.localizedStrings_ != null) {
      this.app_.localizedStrings_.hibernate();
    }
  }

  private boolean resourceRequest(final WebRequest request) {
    if (this.state_ == WebSession.State.ExpectLoad
        || this.state_ == WebSession.State.Loaded
        || this.state_ == WebSession.State.Suspended) {
      String requestE = request.getParameter("request");
      String resourceE = request.getParameter("resource");
      if (requestE != null && requestE.equals("resource") && resourceE != null) {
        return true;
      } else {
        if (!(requestE != null) && this.app_ != null) {
          if (request.getPathInfo().length() != 0
              && this.app_.decodeExposedResource(
                      "/path/" + StringUtils.prepend(request.getPathInfo(), '/'))
                  != null) {
            return true;
          }
          String hashE = request.getParameter("_");
          if (hashE != null && this.app_.decodeExposedResource("/path/" + hashE) != null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private ReentrantLock mutex_;
  private ReentrantLock eventQueueMutex_;
  private LinkedList<ApplicationEvent> eventQueue_;
  private EntryPointType type_;
  private String favicon_;
  private WebSession.State state_;
  private String sessionId_;
  private String sessionIdCookie_;
  private String multiSessionId_;
  boolean sessionIdChanged_;
  private boolean sessionIdCookieChanged_;
  private boolean sessionIdInUrl_;
  private WtServlet controller_;
  private WebRenderer renderer_;
  private String applicationName_;
  private String bookmarkUrl_;
  private String basePath_;
  private String absoluteBaseUrl_;
  private String applicationUrl_;
  private String deploymentPath_;
  private String docRoot_;
  private String redirect_;
  String pagePathInfo_;
  private WebResponse asyncResponse_;
  WebResponse webSocket_;
  private WebResponse bootStyleResponse_;
  private boolean canWriteWebSocket_;
  private boolean webSocketConnected_;
  private int pollRequestsIgnored_;
  private boolean progressiveBoot_;
  private WebRequest deferredRequest_;
  private WebResponse deferredResponse_;
  private int deferCount_;
  private java.util.concurrent.locks.Condition recursiveEvent_;
  private java.util.concurrent.locks.Condition recursiveEventDone_;
  private WEvent.Impl newRecursiveEvent_;
  private java.util.concurrent.locks.Condition updatesPendingEvent_;
  private boolean updatesPending_;
  private boolean triggerUpdate_;
  private WEnvironment embeddedEnv_;
  private WEnvironment env_;
  private WApplication app_;
  private boolean debug_;
  private List<WebSession.Handler> handlers_;
  private WebSession.Handler recursiveEventHandler_;

  void pushUpdates() {
    try {
      logger.debug(new StringWriter().append("pushUpdates()").toString());
      this.triggerUpdate_ = false;
      if (!(this.app_ != null) || !this.renderer_.isDirty()) {
        logger.debug(new StringWriter().append("pushUpdates(): nothing to do").toString());
        return;
      }
      this.updatesPending_ = true;
      if (this.asyncResponse_ != null) {
        this.asyncResponse_.setResponseType(WebRequest.ResponseType.Update);
        this.app_.notify(new WEvent(new WEvent.Impl(this.asyncResponse_)));
        this.updatesPending_ = false;
        this.asyncResponse_.flush();
        this.asyncResponse_ = null;
      } else {
        if (this.webSocket_ != null && this.webSocketConnected_) {
          if (this.webSocket_.isWebSocketMessagePending()) {
            logger.debug(
                new StringWriter().append("pushUpdates(): web socket message pending").toString());
            return;
          }
          if (this.canWriteWebSocket_) {
            this.webSocket_.setResponseType(WebRequest.ResponseType.Update);
            this.app_.notify(new WEvent(new WEvent.Impl(this.webSocket_)));
            this.updatesPending_ = false;
            this.webSocket_.flushBuffer();
          }
        }
      }
      if (this.updatesPending_) {
        logger.debug(new StringWriter().append("pushUpdates(): cannot write now").toString());
        this.updatesPendingEvent_.signal();
      }
    } catch (IOException ioe) {
      logger.info("Ignoring exception {}", ioe.getMessage(), ioe);
    }
  }
  // private WResource  decodeResource(final String resourceId) ;
  private AbstractEventSignal decodeSignal(final String signalId, boolean checkExposed) {
    AbstractEventSignal result = this.app_.decodeExposedSignal(signalId);
    if (result != null && checkExposed) {
      WWidget w = ObjectUtils.cast(result.getOwner(), WWidget.class);
      if (w != null && !this.app_.isExposed(w)) {
        result = null;
      }
    }
    if (!(result != null) && checkExposed) {
      if (this.app_.getJustRemovedSignals().contains(signalId) == false) {
        logger.error(
            new StringWriter()
                .append("decodeSignal(): signal '")
                .append(signalId)
                .append("' not exposed")
                .toString());
      }
    }
    return result;
  }

  private AbstractEventSignal decodeSignal(
      final String objectId, final String name, boolean checkExposed) {
    String signalId = this.app_.encodeSignal(objectId, name);
    return this.decodeSignal(signalId, checkExposed && !name.equals("resized"));
  }

  private static WObject.FormData getFormData(final WebRequest request, final String name) {
    List<UploadedFile> files = new ArrayList<UploadedFile>();
    CollectionUtils.findInMultimap(request.getUploadedFiles(), name, files);
    return new WObject.FormData(request.getParameterValues(name), files);
  }

  private void render(final WebSession.Handler handler) throws IOException {
    logger.debug(new StringWriter().append("render()").toString());
    try {
      if (!this.env_.hasAjax()) {
        try {
          this.checkTimers();
        } catch (final RuntimeException e) {
          logger.error(
              new StringWriter()
                  .append("Exception while triggering timers")
                  .append(e.toString())
                  .toString());
          throw e;
        }
      }
      if (this.app_ != null && this.app_.hasQuit()) {
        this.kill();
      }
      if (handler.getResponse() != null) {
        this.updatesPending_ = false;
        this.serveResponse(handler);
      }
    } catch (final RuntimeException e) {
      handler.flushResponse();
      throw e;
    }
  }

  private void serveError(int status, final WebSession.Handler handler, final String e)
      throws IOException {
    this.renderer_.serveError(status, handler.getResponse(), e);
    handler.flushResponse();
  }

  private void serveResponse(final WebSession.Handler handler) throws IOException {
    if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Page) {
      this.pagePathInfo_ = handler.getRequest().getPathInfo();
      String wtdE = handler.getRequest().getParameter("wtd");
      if (wtdE != null && wtdE.equals(this.sessionId_)) {
        this.sessionIdInUrl_ = true;
      } else {
        this.sessionIdInUrl_ = false;
      }
    }
    if (!handler.getRequest().isWebSocketMessage()) {
      if (handler.getResponse().getResponseType() == WebRequest.ResponseType.Script) {
        this.mutex_.unlock();
        try {
          ThreadUtils.sleep(Duration.ofMillis(1));
        } catch (final InterruptedException e) {
        }
        this.mutex_.lock();
      }
      this.renderer_.serveResponse(handler.getResponse());
    }
    handler.flushResponse();
  }

  enum SignalKind {
    LearnedStateless(0),
    AutoLearnStateless(1),
    Dynamic(2);

    private int value;

    SignalKind(int value) {
      this.value = value;
    }

    /** Returns the numerical representation of this enum. */
    public int getValue() {
      return value;
    }
  }

  private void processSignal(
      AbstractEventSignal s,
      final String se,
      final WebRequest request,
      WebSession.SignalKind kind) {
    if (!(s != null)) {
      return;
    }
    switch (kind) {
      case LearnedStateless:
        s.processLearnedStateless();
        break;
      case AutoLearnStateless:
        s.processAutoLearnStateless(this.renderer_);
        break;
      case Dynamic:
        JavaScriptEvent jsEvent = new JavaScriptEvent();
        jsEvent.get(request, se);
        s.processDynamic(jsEvent);
    }
  }

  private List<Integer> getSignalProcessingOrder(final WEvent e) {
    final WebSession.Handler handler = e.impl_.handler;
    List<Integer> highPriority = new ArrayList<Integer>();
    List<Integer> normalPriority = new ArrayList<Integer>();
    for (int i = 0; ; ++i) {
      final WebRequest request = handler.getRequest();
      String se = i > 0 ? 'e' + String.valueOf(i) : "";
      String signalE = this.getSignal(request, se);
      if (!(signalE != null)) {
        break;
      }
      if (!signalE.equals("user")
          && !signalE.equals("hash")
          && !signalE.equals("none")
          && !signalE.equals("poll")
          && !signalE.equals("load")
          && !signalE.equals("keepAlive")) {
        AbstractEventSignal signal = this.decodeSignal(signalE, true);
        if (!(signal != null)) {
        } else {
          if (signal.getName() == WFormWidget.CHANGE_SIGNAL) {
            highPriority.add(i);
          } else {
            normalPriority.add(i);
          }
        }
      } else {
        normalPriority.add(i);
      }
    }
    highPriority.addAll(normalPriority);
    return highPriority;
  }

  private void notifySignal(final WEvent e) throws IOException {
    final WebSession.Handler handler = e.impl_.handler;
    if (handler.nextSignal == -1) {
      Utils.copyList(this.getSignalProcessingOrder(e), handler.signalOrder);
      handler.nextSignal = 0;
    }
    for (int i = handler.nextSignal; i < handler.signalOrder.size(); ++i) {
      if (!(handler.getRequest() != null)) {
        return;
      }
      final WebRequest request = handler.getRequest();
      int signalI = handler.signalOrder.get(i);
      String se = signalI > 0 ? 'e' + String.valueOf(signalI) : "";
      String signalE = this.getSignal(request, se);
      if (!(signalE != null)) {
        return;
      }
      logger.debug(new StringWriter().append("signal: ").append(signalE).toString());
      if (this.getType() != EntryPointType.WidgetSet
          || !signalE.equals("none") && !signalE.equals("load")) {
        this.renderer_.setRendered(true);
      }
      if (signalE.equals("none") || signalE.equals("load")) {
        if (signalE.equals("load")) {
          if (!this.renderer_.checkResponsePuzzle(request)) {
            this.app_.quit();
          } else {
            this.setLoaded();
          }
        }
        this.renderer_.setVisibleOnly(false);
      } else {
        if (signalE.equals("keepAlive")) {
        } else {
          if (!signalE.equals("poll")) {
            this.propagateFormValues(e, se);
            boolean discardStateless = !request.isWebSocketMessage() && i == 0;
            if (discardStateless) {
              this.renderer_.saveChanges();
            }
            handler.nextSignal = i + 1;
            if (signalE.equals("hash")) {
              String hashE = request.getParameter(se + "_");
              if (hashE != null) {
                this.changeInternalPath(hashE, handler.getResponse());
                this.app_.doJavaScript("Wt4.11.2.scrollHistory();");
              } else {
                this.changeInternalPath("", handler.getResponse());
              }
            } else {
              for (int k = 0; k < 3; ++k) {
                WebSession.SignalKind kind = WebSession.SignalKind.values()[k];
                if (kind == WebSession.SignalKind.AutoLearnStateless && 0L != 0) {
                  break;
                }
                AbstractEventSignal s;
                if (signalE.equals("user")) {
                  String idE = request.getParameter(se + "id");
                  String nameE = request.getParameter(se + "name");
                  if (!(idE != null) || !(nameE != null)) {
                    break;
                  }
                  s = this.decodeSignal(idE, nameE, k == 0);
                } else {
                  s = this.decodeSignal(signalE, k == 0);
                }
                this.processSignal(s, se, request, kind);
                if (kind == WebSession.SignalKind.LearnedStateless && discardStateless) {
                  this.renderer_.discardChanges();
                }
              }
            }
          }
        }
      }
    }
    this.app_.getJustRemovedSignals().clear();
  }

  private void propagateFormValues(final WEvent e, final String se) {
    final WebRequest request = e.impl_.handler.getRequest();
    this.renderer_.updateFormObjectsList(this.app_);
    Map<String, WObject> formObjects = this.renderer_.getFormObjects();
    String focus = request.getParameter(se + "focus");
    if (focus != null) {
      int selectionStart = -1;
      int selectionEnd = -1;
      try {
        String selStart = request.getParameter(se + "selstart");
        if (selStart != null) {
          selectionStart = Integer.parseInt(selStart);
        }
        String selEnd = request.getParameter(se + "selend");
        if (selEnd != null) {
          selectionEnd = Integer.parseInt(selEnd);
        }
      } catch (final RuntimeException ee) {
        logger.error(
            new StringWriter().append("Could not lexical cast selection range").toString());
      }
      this.app_.setFocus(focus, selectionStart, selectionEnd);
    } else {
      this.app_.setFocus("", -1, -1);
    }
    for (Iterator<Map.Entry<String, WObject>> i_it = formObjects.entrySet().iterator();
        i_it.hasNext(); ) {
      Map.Entry<String, WObject> i = i_it.next();
      String formName = i.getKey();
      WObject obj = i.getValue();
      if (!(0L != 0)) {
        WWidget w = ObjectUtils.cast(obj, WWidget.class);
        if (w != null && !w.isEnabled()) {
          continue;
        }
        obj.setFormData(getFormData(request, se + formName));
      } else {
        obj.setRequestTooLarge(0L);
      }
    }
  }

  private String getSignal(final WebRequest request, final String se) {
    String signalE = request.getParameter(se + "signal");
    if (!(signalE != null)) {
      final int signalLength = 7 + se.length();
      final Map<String, String[]> entries = request.getParameterMap();
      for (Iterator<Map.Entry<String, String[]>> i_it = entries.entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String[]> i = i_it.next();
        if (i.getKey().length() > (int) signalLength
            && i.getKey().substring(0, 0 + signalLength).equals(se + "signal=")) {
          signalE = i.getValue()[0];
          String v = i.getKey().substring(signalLength);
          if (v.length() >= 2) {
            String e = v.substring(v.length() - 2);
            if (e.equals(".x") || e.equals(".y")) {
              v = v.substring(0, 0 + v.length() - 2);
            }
          }
          (signalE) = v;
          break;
        }
      }
    }
    return signalE;
  }

  private void init(final WebRequest request) {
    this.env_.init(request);
    String hashE = request.getParameter("_");
    this.absoluteBaseUrl_ =
        this.env_.getUrlScheme() + "://" + this.env_.getHostName() + this.basePath_;
    boolean useAbsoluteUrls;
    String absoluteBaseUrl = this.app_.readConfigurationProperty("baseURL", this.absoluteBaseUrl_);
    if (absoluteBaseUrl != this.absoluteBaseUrl_) {
      this.absoluteBaseUrl_ = absoluteBaseUrl;
      useAbsoluteUrls = true;
    } else {
      useAbsoluteUrls = false;
    }
    if (useAbsoluteUrls) {
      int slashpos = this.absoluteBaseUrl_.lastIndexOf('/');
      if (slashpos != -1 && slashpos != this.absoluteBaseUrl_.length() - 1) {
        this.absoluteBaseUrl_ = this.absoluteBaseUrl_.substring(0, 0 + slashpos + 1);
      }
      slashpos = this.absoluteBaseUrl_.indexOf("://");
      if (slashpos != -1) {
        slashpos = this.absoluteBaseUrl_.indexOf("/", slashpos + 3);
        if (slashpos != -1) {
          this.deploymentPath_ = this.absoluteBaseUrl_.substring(slashpos) + this.applicationName_;
        }
      }
    }
    this.bookmarkUrl_ = this.applicationName_;
    if (this.getType() == EntryPointType.WidgetSet || useAbsoluteUrls) {
      this.applicationUrl_ = this.absoluteBaseUrl_ + this.applicationName_;
      this.bookmarkUrl_ = this.applicationUrl_;
    }
    String extraPathInfo = request.getPathInfo();
    String path = extraPathInfo;
    if (path.length() == 0 && hashE != null) {
      path = hashE;
    }
    this.env_.setInternalPath(path);
    this.pagePathInfo_ = extraPathInfo;
    this.docRoot_ = this.getCgiValue("DOCUMENT_ROOT");
  }

  private boolean start(WebResponse response) {
    try {
      this.app_ = this.controller_.doCreateApplication(this);
      if (this.app_ != null) {
        if (!this.app_.internalPathValid_) {
          if (response.getResponseType() == WebRequest.ResponseType.Page) {
            response.setStatus(404);
          }
        }
      } else {
        throw new WException("WebSession::start: ApplicationCreator returned a nullptr");
      }
    } catch (final RuntimeException e) {
      this.app_ = null;
      this.kill();
      throw e;
    }
    return this.app_ != null;
  }

  private String getSessionQuery() {
    String result = "?wtd=" + DomElement.urlEncodeS(this.sessionId_);
    if (this.getType() == EntryPointType.WidgetSet) {
      result += "&wtt=widgetset";
    }
    return result;
  }

  private void flushBootStyleResponse() {
    if (this.bootStyleResponse_ != null) {
      this.bootStyleResponse_.flush();
      this.bootStyleResponse_ = null;
    }
  }

  private void changeInternalPath(final String path, WebResponse response) {
    if (!this.app_.internalPathIsChanged_) {
      if (!this.app_.changedInternalPath(path)) {
        if (response.getResponseType() == WebRequest.ResponseType.Page) {
          response.setStatus(404);
        }
      }
    }
  }

  private void processQueuedEvents(final WebSession.Handler handler) {
    for (; ; ) {
      ApplicationEvent event = this.getPopQueuedEvent();
      if (event != null) {
        if (!this.isDead()) {
          this.externalNotify(new WEvent.Impl(handler, event.function));
          if (this.getApp() != null && this.getApp().hasQuit()) {
            this.kill();
          }
          if (this.isDead()) {
            this.getController().removeSession(event.sessionId);
          }
        } else {
          if (event.fallbackFunction != null) {
            event.fallbackFunction.run();
          }
          ;
        }
      } else {
        break;
      }
    }
  }

  private ApplicationEvent getPopQueuedEvent() {
    this.eventQueueMutex_.lock();
    ApplicationEvent result = null;
    logger.debug(
        new StringWriter()
            .append("popQueuedEvent(): ")
            .append(String.valueOf(this.eventQueue_.size()))
            .toString());
    if (!this.eventQueue_.isEmpty()) {
      result = this.eventQueue_.getFirst();
      this.eventQueue_.removeFirst();
    }
    this.eventQueueMutex_.unlock();
    return result;
  }

  private static UploadedFile uf;

  static boolean isAbsoluteUrl(final String url) {
    return url.indexOf(":") != -1;
  }

  static String host(final String url) {
    int pos = 0;
    for (int i = 0; i < 3; ++i) {
      pos = url.indexOf('/', pos);
      if (pos == -1) {
        return url;
      } else {
        ++pos;
      }
    }
    return url.substring(0, 0 + pos - 1);
  }

  static String str(String v) {
    return v != null ? v : "";
  }

  static boolean isEqual(String s1, String s2) {
    if (s1 == null) {
      return s2 == null;
    } else {
      return s1.equals(s2);
    }
  }
}
