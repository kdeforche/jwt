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

class WebRenderer implements SlotLearnerInterface {
  private static Logger logger = LoggerFactory.getLogger(WebRenderer.class);

  public WebRenderer(final WebSession session) {
    super();
    this.session_ = session;
    this.visibleOnly_ = true;
    this.rendered_ = false;
    this.initialStyleRendered_ = false;
    this.twoPhaseThreshold_ = 5000;
    this.pageId_ = 0;
    this.ackErrs_ = 0;
    this.expectedAckId_ = 0;
    this.scriptId_ = 0;
    this.linkedCssCount_ = -1;
    this.solution_ = "";
    this.currentStatelessSlotIsActuallyStateless_ = true;
    this.cookiesToSet_ = new ArrayList<javax.servlet.http.Cookie>();
    this.currentFormObjects_ = new HashMap<String, WObject>();
    this.currentFormObjectsList_ = "";
    this.formObjectsChanged_ = true;
    this.updateLayout_ = false;
    this.wsRequestsToHandle_ = new ArrayList<Integer>();
    this.cookieUpdateNeeded_ = false;
    this.collectedJS1_ = new StringBuilder();
    this.collectedJS2_ = new StringBuilder();
    this.invisibleJS_ = new StringBuilder();
    this.statelessJS_ = new StringBuilder();
    this.beforeLoadJS_ = new StringBuilder();
    this.updateMap_ = new HashSet<WWidget>();
    this.learning_ = false;
  }

  public void setTwoPhaseThreshold(int bytes) {
    this.twoPhaseThreshold_ = bytes;
  }

  public boolean isVisibleOnly() {
    return this.visibleOnly_;
  }

  public void setVisibleOnly(boolean how) {
    this.visibleOnly_ = how;
  }

  public boolean isRendered() {
    return this.rendered_;
  }

  public void setRendered(boolean how) {
    if (this.rendered_ != how) {
      logger.debug(
          new StringWriter().append("setRendered: ").append(String.valueOf(how)).toString());
      this.rendered_ = how;
    }
  }

  public void needUpdate(WWidget w, boolean laterOnly) {
    logger.debug(
        new StringWriter()
            .append("needUpdate: ")
            .append(w.getId())
            .append(" (")
            .append("(fixme)")
            .append(")")
            .toString());
    this.updateMap_.add(w);
    if (!laterOnly) {
      this.moreUpdates_ = true;
    }
  }

  public void doneUpdate(WWidget w) {
    logger.debug(
        new StringWriter()
            .append("doneUpdate: ")
            .append(w.getId())
            .append(" (")
            .append("(fixme)")
            .append(")")
            .toString());
    this.updateMap_.remove(w);
  }

  public void updateFormObjects(WWebWidget source, boolean checkDescendants) {
    this.formObjectsChanged_ = true;
  }

  public void updateFormObjectsList(WApplication app) {
    if (this.formObjectsChanged_) {
      this.currentFormObjects_.clear();
      app.domRoot_.getFormObjects(this.currentFormObjects_);
      if (app.domRoot2_ != null) {
        app.domRoot2_.getFormObjects(this.currentFormObjects_);
      }
    }
  }

  public Map<String, WObject> getFormObjects() {
    return this.currentFormObjects_;
  }

  public void saveChanges() throws IOException {
    this.collectedJS1_.append(this.invisibleJS_.toString());
    this.invisibleJS_.setLength(0);
    this.collectJS(this.collectedJS1_);
  }

  public void discardChanges() throws IOException {
    this.collectJS((StringBuilder) null);
  }

  public void letReloadJS(final WebResponse response, boolean newSession, boolean embedded)
      throws IOException {
    if (!embedded) {
      this.addNoCacheHeaders(response);
      this.setHeaders(response, "text/javascript; charset=UTF-8");
    }
    response.out().append("if (window.Wt) window.Wt._p_.quit(null); window.location.reload(true);");
  }

  public final void letReloadJS(final WebResponse response, boolean newSession) throws IOException {
    letReloadJS(response, newSession, false);
  }

  public void letReloadHTML(final WebResponse response, boolean newSession) throws IOException {
    this.addNoCacheHeaders(response);
    this.setHeaders(response, "text/html; charset=UTF-8");
    response
        .out()
        .append("<html><script type=\"text/javascript\" nonce=\"")
        .append(response.getNonce())
        .append("\">");
    this.letReloadJS(response, newSession, true);
    response.out().append("</script><body></body></html>");
  }

  public boolean isDirty() {
    return !this.updateMap_.isEmpty()
        || this.formObjectsChanged_
        || this.session_.getApp().hasQuit()
        || this.session_.getApp().afterLoadJavaScript_.length() != 0
        || this.session_.getApp().serverPushChanged_
        || this.session_.getApp().styleSheetsAdded_ != 0
        || !this.session_.getApp().styleSheetsToRemove_.isEmpty()
        || this.session_.getApp().getStyleSheet().isDirty()
        || this.session_.getApp().internalPathIsChanged_
        || !(this.collectedJS1_.length() == 0)
        || !(this.collectedJS2_.length() == 0)
        || !(this.invisibleJS_.length() == 0)
        || !this.wsRequestsToHandle_.isEmpty()
        || this.cookieUpdateNeeded_;
  }

  public int getScriptId() {
    return this.scriptId_;
  }

  public int getPageId() {
    return this.pageId_;
  }

  public void serveResponse(final WebResponse response) throws IOException {
    this.session_.setTriggerUpdate(false);
    switch (response.getResponseType()) {
      case Update:
        this.serveJavaScriptUpdate(response);
        break;
      case Page:
        this.initialStyleRendered_ = false;
        ++this.pageId_;
        if (this.session_.getApp() != null) {
          this.serveMainpage(response);
        } else {
          this.serveBootstrap(response);
        }
        break;
      case Script:
        boolean hybridPage = this.session_.isProgressiveBoot() || this.session_.getEnv().hasAjax();
        if (!hybridPage) {
          this.setRendered(false);
        }
        this.serveMainscript(response);
        break;
    }
  }

  public void serveError(int status, final WebResponse response, final String message)
      throws IOException {
    boolean js = response.getResponseType() != WebRequest.ResponseType.Page;
    WApplication app = this.session_.getApp();
    if (!js || !(app != null)) {
      response.setStatus(status);
      response.setContentType("text/html");
      response
          .out()
          .append("<title>Error occurred.</title>")
          .append("<h2>Error occurred.</h2>")
          .append(WWebWidget.escapeText(new WString(message), true).toString())
          .append('\n');
    } else {
      response
          .out()
          .append(app.getJavaScriptClass())
          .append("._p_.quit(null);")
          .append("document.title = 'Error occurred.';")
          .append("document.body.innerHtml='<h2>Error occurred.</h2>' +")
          .append(WWebWidget.jsStringLiteral(message))
          .append(';');
    }
  }

  public void serveLinkedCss(final WebResponse response) throws IOException {
    response.setContentType("text/css");
    if (!this.initialStyleRendered_) {
      WApplication app = this.session_.getApp();
      StringBuilder out = new StringBuilder();
      if (app.getTheme() != null) {
        app.getTheme().serveCss(out);
      }
      for (int i = 0; i < app.styleSheets_.size(); ++i) {
        app.styleSheets_.get(i).cssText(out);
      }
      app.styleSheetsAdded_ = 0;
      this.initialStyleRendered_ = true;
      this.linkedCssCount_ = app.styleSheets_.size();
      response.out().append(out.toString());
    } else {
      if (this.linkedCssCount_ > -1) {
        WApplication app = this.session_.getApp();
        StringBuilder out = new StringBuilder();
        if (app.getTheme() != null) {
          app.getTheme().serveCss(out);
        }
        int count = Math.min((int) this.linkedCssCount_, app.styleSheets_.size());
        for (int i = 0; i < count; ++i) {
          app.styleSheets_.get(i).cssText(out);
        }
        response.out().append(out.toString());
      }
    }
  }

  public void setCookie(final javax.servlet.http.Cookie cookie) {
    this.cookiesToSet_.add(cookie);
    this.cookieUpdateNeeded_ = true;
  }

  public void removeCookie(final javax.servlet.http.Cookie cookie) {
    javax.servlet.http.Cookie tmp = cookie;
    tmp.setValue("deleted");
    tmp.setMaxAge(0);
    this.cookiesToSet_.add(tmp);
    this.cookieUpdateNeeded_ = true;
  }

  public boolean isPreLearning() {
    return this.learning_;
  }

  public void learningIncomplete() {
    this.learningIncomplete_ = true;
  }

  public void updateLayout() {
    this.updateLayout_ = true;
  }

  enum AckState {
    CorrectAck,
    ReasonableAck,
    BadAck;

    /** Returns the numerical representation of this enum. */
    public int getValue() {
      return ordinal();
    }
  }

  public WebRenderer.AckState ackUpdate(int updateId) {
    logger.debug(
        new StringWriter()
            .append("ackUpdate: expecting ")
            .append(String.valueOf(this.expectedAckId_))
            .append(", received ")
            .append(String.valueOf(updateId))
            .toString());
    if (updateId == this.expectedAckId_) {
      logger.debug(new StringWriter().append("jsSynced(false) after ackUpdate okay").toString());
      this.setJSSynced(false);
      this.ackErrs_ = 0;
      return WebRenderer.AckState.CorrectAck;
    } else {
      if (this.expectedAckId_ - updateId < 5) {
        ++this.ackErrs_;
        return this.ackErrs_ < 3 ? WebRenderer.AckState.ReasonableAck : WebRenderer.AckState.BadAck;
      } else {
        return WebRenderer.AckState.BadAck;
      }
    }
  }

  public void streamRedirectJS(final StringBuilder out, final String redirect) {
    if (this.session_.getApp() != null && this.session_.getApp().internalPathIsChanged_) {
      out.append("if (window.")
          .append(this.session_.getApp().getJavaScriptClass())
          .append(") ")
          .append(this.session_.getApp().getJavaScriptClass())
          .append("._p_.setHash(")
          .append(WWebWidget.jsStringLiteral(this.session_.getApp().newInternalPath_))
          .append(", false);\n");
    }
    out.append("if (window.location.replace) window.location.replace(")
        .append(WWebWidget.jsStringLiteral(redirect))
        .append(");else window.location.href=")
        .append(WWebWidget.jsStringLiteral(redirect))
        .append(";\n");
  }

  public boolean checkResponsePuzzle(final WebRequest request) {
    if (this.solution_.length() != 0) {
      String ackPuzzleE = request.getParameter("ackPuzzle");
      if (!(ackPuzzleE != null)) {
        logger.warn(
            new StringWriter()
                .append("secure:")
                .append("Ajax puzzle fail: solution missing")
                .toString());
        return false;
      }
      String ackPuzzle = ackPuzzleE;
      List<String> answer = new ArrayList<String>();
      List<String> solution = new ArrayList<String>();
      StringUtils.split(solution, this.solution_, ",", false);
      StringUtils.split(answer, ackPuzzle, ",", false);
      int j = 0;
      boolean fail = false;
      for (int i = 0; i < solution.size(); ++i) {
        for (; j < answer.size(); ++j) {
          if (solution.get(i).equals(answer.get(j))) {
            break;
          }
        }
        if (j == answer.size()) {
          fail = true;
          break;
        }
      }
      if (j < answer.size() - 1) {
        fail = true;
      }
      if (fail) {
        logger.warn(
            new StringWriter()
                .append("secure:")
                .append("Ajax puzzle fail: '")
                .append(ackPuzzle)
                .append("' vs '")
                .append(this.solution_)
                .append('\'')
                .toString());
        this.solution_ = "";
        return false;
      } else {
        this.solution_ = "";
        return true;
      }
    } else {
      return true;
    }
  }

  public boolean isJsSynced() {
    return (this.collectedJS1_.length() == 0) && (this.collectedJS2_.length() == 0);
  }

  public void setJSSynced(boolean invisibleToo) {
    logger.debug(
        new StringWriter().append("setJSSynced: ").append(String.valueOf(invisibleToo)).toString());
    this.collectedJS1_.setLength(0);
    this.collectedJS2_.setLength(0);
    if (!invisibleToo) {
      this.collectedJS1_.append(this.invisibleJS_.toString());
    }
    this.invisibleJS_.setLength(0);
  }

  public void setStatelessSlotNotStateless() {
    this.currentStatelessSlotIsActuallyStateless_ = false;
  }

  private final WebSession session_;
  private boolean visibleOnly_;
  private boolean rendered_;
  private boolean initialStyleRendered_;
  private int twoPhaseThreshold_;
  private int pageId_;
  private int ackErrs_;
  private int expectedAckId_;
  private int scriptId_;
  private int linkedCssCount_;
  private String solution_;
  private boolean currentStatelessSlotIsActuallyStateless_;
  private List<javax.servlet.http.Cookie> cookiesToSet_;
  private Map<String, WObject> currentFormObjects_;
  private String currentFormObjectsList_;
  private boolean formObjectsChanged_;
  private boolean updateLayout_;
  private List<Integer> wsRequestsToHandle_;
  private boolean cookieUpdateNeeded_;

  private void setHeaders(final WebResponse response, final String mimeType) {
    for (javax.servlet.http.Cookie cookie : this.cookiesToSet_) {
      response.addCookie(cookie);
    }
    this.cookiesToSet_.clear();
    this.cookieUpdateNeeded_ = false;
    response.setContentType(mimeType);
  }

  private void addNoCacheHeaders(final WebResponse response) {
    response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.addHeader("Pragma", "no-cache");
    response.addHeader("Expires", "0");
  }

  private void serveJavaScriptUpdate(final WebResponse response) throws IOException {
    if (!response.isWebSocketMessage()) {
      this.addNoCacheHeaders(response);
      this.setHeaders(response, "text/javascript; charset=UTF-8");
    }
    if (this.session_.sessionIdChanged_) {
      this.collectedJS1_
          .append(this.session_.getApp().getJavaScriptClass())
          .append("._p_.setSessionUrl(")
          .append(WWebWidget.jsStringLiteral(this.getSessionUrl()))
          .append(");");
    }
    StringBuilder out = new StringBuilder();
    if (!this.rendered_) {
      this.serveMainAjax(out);
    } else {
      this.collectJavaScript();
      this.addResponseAckPuzzle(out);
      this.renderSetServerPush(out);
      logger.debug(
          new StringWriter()
              .append("js: ")
              .append(this.collectedJS1_.toString())
              .append(this.collectedJS2_.toString())
              .toString());
      out.append(this.collectedJS1_.toString()).append(this.collectedJS2_.toString());
      if (response.isWebSocketMessage()) {
        this.renderCookieUpdate(out);
        this.renderWsRequestsDone(out);
        logger.debug(
            new StringWriter()
                .append("jsSynced(false) after rendering websocket message")
                .toString());
        this.setJSSynced(false);
      }
    }
    response.out().append(out.toString());
  }

  private void serveMainscript(final WebResponse response) throws IOException {
    final Configuration conf = this.session_.getController().getConfiguration();
    boolean widgetset = this.session_.getType() == EntryPointType.WidgetSet;
    this.session_.sessionIdChanged_ = false;
    this.addNoCacheHeaders(response);
    this.setHeaders(response, "text/javascript; charset=UTF-8");
    StringBuilder out = new StringBuilder();
    if (!widgetset) {
      String redirect = this.session_.getRedirect();
      if (redirect.length() != 0) {
        this.streamRedirectJS(out, redirect);
        response.out().append(out.toString());
        return;
      }
    } else {
      this.expectedAckId_ = this.scriptId_ = MathUtils.randomInt();
      this.ackErrs_ = 0;
    }
    WApplication app = this.session_.getApp();
    final boolean innerHtml = true;
    FileServe script = new FileServe(WtServlet.Wt_js);
    script.setCondition(
        "CATCH_ERROR", conf.getErrorReporting() != Configuration.ErrorReporting.NoErrors);
    script.setCondition(
        "SHOW_ERROR", conf.getErrorReporting() == Configuration.ErrorReporting.ErrorMessage);
    script.setCondition("UGLY_INTERNAL_PATHS", this.session_.isUseUglyInternalPaths());
    script.setCondition("DYNAMIC_JS", false);
    script.setVar("WT_CLASS", "Wt4.11.2");
    script.setVar("APP_CLASS", app.getJavaScriptClass());
    script.setCondition("STRICTLY_SERIALIZED_EVENTS", conf.serializedEvents());
    script.setCondition("WEB_SOCKETS", conf.webSockets());
    script.setVar("INNER_HTML", innerHtml);
    script.setVar("ACK_UPDATE_ID", this.expectedAckId_);
    script.setVar("SESSION_URL", WWebWidget.jsStringLiteral(this.getSessionUrl()));
    script.setVar(
        "QUITTED_STR", WString.toWString(WString.tr("Wt.QuittedMessage")).getJsStringLiteral());
    script.setVar("MAX_FORMDATA_SIZE", conf.getMaxFormDataSize());
    script.setVar("MAX_PENDING_EVENTS", conf.getMaxPendingEvents());
    String deployPath = this.session_.getEnv().publicDeploymentPath_;
    if (deployPath.length() == 0) {
      deployPath = this.session_.getDeploymentPath();
    }
    script.setVar("DEPLOY_PATH", WWebWidget.jsStringLiteral(deployPath));
    script.setVar(
        "WS_PATH",
        WWebWidget.jsStringLiteral(this.session_.getController().getContextPath() + "/ws"));
    script.setVar(
        "WS_ID",
        WWebWidget.jsStringLiteral(
            String.valueOf(this.session_.getController().getIdForWebSocket())));
    script.setVar("KEEP_ALIVE", String.valueOf(conf.getKeepAlive()));
    script.setVar(
        "IDLE_TIMEOUT",
        conf.getIdleTimeout() != -1 ? String.valueOf(conf.getIdleTimeout()) : "null");
    script.setVar("INDICATOR_TIMEOUT", conf.getIndicatorTimeout());
    script.setVar("SERVER_PUSH_TIMEOUT", conf.getServerPushTimeout() * 1000);
    script.setVar("CLOSE_CONNECTION", false);
    String params = "";
    if (this.session_.getType() == EntryPointType.WidgetSet) {
      Map<String, String[]> m = this.session_.getEnv().getParameterMap();
      String[] it = m.get("Wt-params");
      Map<String, String[]> wtParams = new HashMap<String, String[]>();
      if (it != null) {
        Utils.parseFormUrlEncoded(it[0], wtParams);
        m = wtParams;
      }
      for (Iterator<Map.Entry<String, String[]>> i_it = m.entrySet().iterator(); i_it.hasNext(); ) {
        Map.Entry<String, String[]> i = i_it.next();
        if (params.length() != 0) {
          params += '&';
        }
        params += Utils.urlEncode(i.getKey()) + '=' + Utils.urlEncode(i.getValue()[0]);
      }
    }
    script.setVar("PARAMS", params);
    script.stream(out);
    out.append(app.getJavaScriptClass()).append("._p_.setPage(").append(this.pageId_).append(");");
    this.formObjectsChanged_ = true;
    app.autoJavaScriptChanged_ = true;
    if (this.session_.getType() == EntryPointType.WidgetSet) {
      out.append(app.getJavaScriptClass()).append("._p_.update(null, 'load', null, false);");
    } else {
      if (!this.rendered_) {
        this.serveMainAjax(out);
      } else {
        boolean enabledAjax = app.enableAjax_;
        if (app.enableAjax_) {
          this.collectedJS1_
              .append("var form = Wt4.11.2.getElement('Wt-form'); if (form) {")
              .append(this.beforeLoadJS_.toString());
          this.beforeLoadJS_.setLength(0);
          this.collectedJS1_
              .append("var domRoot=")
              .append(app.domRoot_.getJsRef())
              .append(';')
              .append("Wt4.11.2.progressed(domRoot);");
          int librariesLoaded = this.loadScriptLibraries(this.collectedJS1_, app);
          app.streamBeforeLoadJavaScript(this.collectedJS1_, false);
          this.collectedJS2_
              .append("Wt4.11.2.resolveRelativeAnchors();")
              .append("domRoot.style.visibility = 'visible';")
              .append(app.getJavaScriptClass())
              .append("._p_.doAutoJavaScript();");
          this.loadScriptLibraries(this.collectedJS2_, app, librariesLoaded);
          this.collectedJS2_.append('}');
          app.enableAjax_ = false;
        } else {
          app.streamBeforeLoadJavaScript(out, true);
        }
        out.append("window.")
            .append(app.getJavaScriptClass())
            .append("LoadWidgetTree = function(){\n");
        if (app.internalPathsEnabled_) {
          out.append(app.getJavaScriptClass())
              .append("._p_.enableInternalPaths(")
              .append(WWebWidget.jsStringLiteral(app.renderedInternalPath_))
              .append(");\n");
        }
        this.visibleOnly_ = false;
        this.formObjectsChanged_ = true;
        this.currentFormObjectsList_ = "";
        this.collectJavaScript();
        this.updateLoadIndicator(this.collectedJS1_, app, true);
        logger.debug(
            new StringWriter()
                .append("js: ")
                .append(this.collectedJS1_.toString())
                .append(this.collectedJS2_.toString())
                .toString());
        out.append(this.collectedJS1_.toString());
        this.addResponseAckPuzzle(out);
        out.append(app.getJavaScriptClass())
            .append("._p_.setHash(")
            .append(WWebWidget.jsStringLiteral(app.newInternalPath_))
            .append(", false);\n");
        if (!app.getEnvironment().isInternalPathUsingFragments()) {
          this.session_.setPagePathInfo(app.newInternalPath_);
        }
        out.append(app.getJavaScriptClass())
            .append("._p_.update(null, 'load', null, false);")
            .append(this.collectedJS2_.toString())
            .append("};");
        this.session_.getApp().serverPushChanged_ = true;
        this.renderSetServerPush(out);
        if (enabledAjax) {
          out.append("\nif (typeof document.readyState === 'undefined')")
              .append(" setTimeout(function() { ")
              .append(app.getJavaScriptClass())
              .append("._p_.load(true);")
              .append("}, 400);")
              .append("else ");
        }
        out.append("Wt4.11.2.ready(function() { ")
            .append(app.getJavaScriptClass())
            .append("._p_.load(true);});\n");
      }
    }
    response.out().append(out.toString());
  }

  private void serveBootstrap(final WebResponse response) throws IOException {
    final Configuration conf = this.session_.getController().getConfiguration();
    FileServe boot = new FileServe(WtServlet.Boot_html);
    this.setPageVars(boot, response.getNonce());
    StringBuilder noJsRedirectUrl = new StringBuilder();
    DomElement.htmlAttributeValue(
        noJsRedirectUrl,
        this.session_.getBootstrapUrl(response, WebSession.BootstrapOption.KeepInternalPath)
            + "&js=no");
    boot.setVar("REDIRECT_URL", noJsRedirectUrl.toString());
    boot.setVar(
        "AUTO_REDIRECT",
        "<noscript><meta http-equiv=\"refresh\" content=\"0; url="
            + noJsRedirectUrl.toString()
            + "\"></noscript>");
    boot.setVar("NOSCRIPT_TEXT", conf.getRedirectMessage());
    StringBuilder bootStyleUrl = new StringBuilder();
    DomElement.htmlAttributeValue(
        bootStyleUrl,
        this.session_.getBootstrapUrl(response, WebSession.BootstrapOption.ClearInternalPath)
            + "&request=style&page="
            + String.valueOf(this.pageId_));
    boot.setVar("BOOT_STYLE_URL", bootStyleUrl.toString());
    this.addNoCacheHeaders(response);
    if (conf.isUseXFrameSameOrigin()) {
      response.addHeader("X-Frame-Options", "SAMEORIGIN");
    }
    String contentType = "text/html; charset=UTF-8";
    this.setHeaders(response, contentType);
    StringBuilder out = new StringBuilder();
    this.streamBootContent(response, boot, false);
    boot.stream(out);
    this.setRendered(false);
    response.out().append(out.toString());
  }

  private void serveMainpage(final WebResponse response) throws IOException {
    ++this.expectedAckId_;
    this.session_.sessionIdChanged_ = false;
    final Configuration conf = this.session_.getController().getConfiguration();
    WApplication app = this.session_.getApp();
    if (!app.getEnvironment().hasAjax()
        && (app.internalPathIsChanged_
            && !app.renderedInternalPath_.equals(app.newInternalPath_))) {
      app.renderedInternalPath_ = app.newInternalPath_;
      if (this.session_.getState() == WebSession.State.JustCreated
          && conf.progressiveBootstrap(app.getEnvironment().getInternalPath())) {
        this.session_.redirect(
            this.session_.fixRelativeUrl(this.session_.getBookmarkUrl(app.newInternalPath_)));
        this.session_.kill();
      } else {
        this.session_.redirect(
            this.session_.fixRelativeUrl(this.session_.getMostRelativeUrl(app.newInternalPath_)));
      }
    }
    String redirect = this.session_.getRedirect();
    if (redirect.length() != 0) {
      response.setStatus(302);
      response.sendRedirect(redirect);
      this.setHeaders(response, "text/html; charset=UTF-8");
      return;
    }
    WWebWidget mainWebWidget = app.domRoot_;
    this.visibleOnly_ = true;
    DomElement mainElement = mainWebWidget.createSDomElement(app);
    this.setRendered(true);
    this.setJSSynced(true);
    StringBuilder styleSheets = new StringBuilder();
    if (app.getTheme() != null) {
      List<WLinkedCssStyleSheet> sheets = app.getTheme().getStyleSheets();
      for (int i = 0; i < sheets.size(); ++i) {
        this.renderStyleSheet(styleSheets, sheets.get(i), app);
      }
    }
    for (int i = 0; i < app.styleSheets_.size(); ++i) {
      this.renderStyleSheet(styleSheets, app.styleSheets_.get(i), app);
    }
    app.styleSheetsAdded_ = 0;
    this.initialStyleRendered_ = true;
    this.beforeLoadJS_.setLength(0);
    for (int i = 0; i < app.scriptLibraries_.size(); ++i) {
      String url = app.scriptLibraries_.get(i).uri;
      styleSheets.append("<script src=");
      DomElement.htmlAttributeValue(styleSheets, this.session_.fixRelativeUrl(url));
      if (conf.isUseScriptNonce()) {
        styleSheets.append(" nonce=\"").append(response.getNonce()).append("\"");
      }
      styleSheets.append("></script>\n");
      this.beforeLoadJS_.append(app.scriptLibraries_.get(i).beforeLoadJS);
    }
    app.scriptLibrariesAdded_ = 0;
    app.newBeforeLoadJavaScript_ = app.beforeLoadJavaScript_.length();
    boolean hybridPage = this.session_.isProgressiveBoot() || this.session_.getEnv().hasAjax();
    FileServe page = new FileServe(hybridPage ? WtServlet.Hybrid_html : WtServlet.Plain_html);
    this.setPageVars(page, response.getNonce());
    page.setVar("SESSION_ID", this.session_.getSessionId());
    String url =
        app.getEnvironment().agentIsSpiderBot() || !this.session_.isUseUrlRewriting()
            ? this.session_.getBookmarkUrl(app.newInternalPath_)
            : this.session_.getMostRelativeUrl(app.newInternalPath_);
    url = this.session_.fixRelativeUrl(url);
    url = StringUtils.replace(url, '&', "&amp;");
    page.setVar("RELATIVE_URL", url);
    if (conf.isInlineCss()) {
      StringBuilder css = new StringBuilder();
      app.getStyleSheet().cssText(css, true);
      page.setVar("STYLESHEET", css.toString());
    } else {
      page.setVar("STYLESHEET", "");
    }
    page.setVar("STYLESHEETS", styleSheets.toString());
    page.setVar("TITLE", WWebWidget.escapeText(app.getTitle()).toString());
    app.titleChanged_ = false;
    String contentType = "text/html; charset=UTF-8";
    this.addNoCacheHeaders(response);
    if (conf.isUseXFrameSameOrigin()) {
      response.addHeader("X-Frame-Options", "SAMEORIGIN");
    }
    this.setHeaders(response, contentType);
    this.currentFormObjectsList_ = this.createFormObjectsList(app);
    if (hybridPage) {
      this.streamBootContent(response, page, true);
    }
    StringBuilder out = new StringBuilder();
    page.streamUntil(out, "HTML");
    List<DomElement.TimeoutEvent> timeouts = new ArrayList<DomElement.TimeoutEvent>();
    {
      EscapeOStream js = new EscapeOStream();
      EscapeOStream eout = new EscapeOStream(out);
      mainElement.asHTML(eout, js, timeouts);
      this.invisibleJS_.append(js.toString());

      app.domRoot_.doneRerender();
    }
    int refresh;
    if (app.getEnvironment().hasAjax()) {
      StringBuilder str = new StringBuilder();
      DomElement.createTimeoutJs(str, timeouts, app);
      app.doJavaScript(str.toString());
      refresh = 1000000;
    } else {
      if (app.hasQuit() || conf.getSessionTimeout() == -1) {
        refresh = 1000000;
      } else {
        refresh = conf.getSessionTimeout() / 3;
        for (int i = 0; i < timeouts.size(); ++i) {
          refresh = Math.min(refresh, 1 + timeouts.get(i).msec / 1000);
        }
      }
    }
    page.setVar("REFRESH", String.valueOf(refresh));
    page.stream(out);
    app.internalPathIsChanged_ = false;
    response.out().append(out.toString());
  }

  private void serveMainAjax(final StringBuilder out) {
    final Configuration conf = this.session_.getController().getConfiguration();
    boolean widgetset = this.session_.getType() == EntryPointType.WidgetSet;
    WApplication app = this.session_.getApp();
    WWebWidget mainWebWidget = app.domRoot_;
    this.visibleOnly_ = true;
    app.loadingIndicator_.show();
    DomElement mainElement = mainWebWidget.createSDomElement(app);
    app.loadingIndicator_.hide();
    app.scriptLibrariesAdded_ = app.scriptLibraries_.size();
    int librariesLoaded = this.loadScriptLibraries(out, app);
    out.append(app.getJavaScriptClass())
        .append("._p_.autoJavaScript=function(){")
        .append(app.autoJavaScript_)
        .append("};\n");
    app.autoJavaScriptChanged_ = false;
    app.streamBeforeLoadJavaScript(out, true);
    if (!widgetset) {
      out.append("window.")
          .append(app.getJavaScriptClass())
          .append("LoadWidgetTree = function(){\n");
    }
    if (!this.initialStyleRendered_) {
      if (app.getTheme() != null) {
        List<WLinkedCssStyleSheet> styleSheets = app.getTheme().getStyleSheets();
        for (int i = 0; i < styleSheets.size(); ++i) {
          this.loadStyleSheet(out, app, styleSheets.get(i));
        }
      }
      app.styleSheetsAdded_ = app.styleSheets_.size();
      this.loadStyleSheets(out, app);
      this.initialStyleRendered_ = true;
    }
    if (conf.isInlineCss()) {
      app.getStyleSheet().javaScriptUpdate(app, out, true);
    }
    if (app.bodyHtmlClassChanged_) {
      String op = widgetset ? "+=" : "=";
      out.append("document.body.parentNode.className")
          .append(op)
          .append('\'')
          .append(app.htmlClass_)
          .append("';")
          .append("document.body.className")
          .append(op)
          .append('\'')
          .append(this.getBodyClassRtl())
          .append("';")
          .append("document.body.setAttribute('dir', '");
      if (app.getLayoutDirection() == LayoutDirection.LeftToRight) {
        out.append("LTR");
      } else {
        out.append("RTL");
      }
      out.append("');");
    }
    if (app.htmlAttributeChanged_) {
      for (Iterator<Map.Entry<String, String>> i_it = app.getHtmlAttributes().entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String> i = i_it.next();
        out.append("document.documentElement.setAttribute('")
            .append(i.getKey())
            .append("', '")
            .append(i.getValue())
            .append("');\n");
      }
      app.htmlAttributeChanged_ = false;
    }
    if (app.bodyAttributeChanged_) {
      for (Iterator<Map.Entry<String, String>> i_it = app.getBodyAttributes().entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String> i = i_it.next();
        out.append("document.body.setAttribute('")
            .append(i.getKey())
            .append("', '")
            .append(i.getValue())
            .append("');\n");
      }
      app.bodyAttributeChanged_ = false;
    }
    StringBuilder s = new StringBuilder();
    mainElement.addToParent(s, "document.body", widgetset ? 0 : -1, app);

    this.addResponseAckPuzzle(s);
    if (app.hasQuit()) {
      s.append(app.getJavaScriptClass())
          .append("._p_.quit(")
          .append(
              ((app.quittedMessage_.length() == 0)
                      ? "null"
                      : WString.toWString(app.quittedMessage_).getJsStringLiteral())
                  + ");");
    }
    if (widgetset) {
      app.domRoot2_.rootAsJavaScript(app, s, true);
    }
    logger.debug(new StringWriter().append("js: ").append(s.toString()).toString());
    out.append(s.toString());
    this.currentFormObjectsList_ = this.createFormObjectsList(app);
    out.append(app.getJavaScriptClass())
        .append("._p_.setFormObjects([")
        .append(this.currentFormObjectsList_)
        .append("]);\n");
    this.formObjectsChanged_ = false;
    this.setRendered(true);
    this.setJSSynced(true);
    if (this.visibleOnly_) {
      this.preCollectInvisibleChanges();
      if (this.twoPhaseThreshold_ > 0
          && this.invisibleJS_.length() < (int) this.twoPhaseThreshold_) {
        this.collectedJS1_.append(this.invisibleJS_.toString());
        this.invisibleJS_.setLength(0);
      } else {
        if (widgetset) {
          this.collectedJS1_
              .append(this.session_.getApp().getJavaScriptClass())
              .append("._p_.update(null, 'none', null, false);");
        }
      }
    }
    this.preLearnStateless(app, this.collectedJS1_);
    logger.debug(
        new StringWriter().append("js: ").append(this.collectedJS1_.toString()).toString());
    out.append(this.collectedJS1_.toString());
    this.collectedJS1_.setLength(0);
    this.updateLoadIndicator(out, app, true);
    if (widgetset) {
      String historyE = app.getEnvironment().getParameter("Wt-history");
      if (historyE != null) {
        out.append("Wt4.11.2")
            .append(".history.initialize('")
            .append(historyE.charAt(0))
            .append("-field', '")
            .append(historyE.charAt(0))
            .append("-iframe', '');\n");
      }
    }
    app.streamAfterLoadJavaScript(out);
    out.append("{var o=null,e=null;").append(app.hideLoadingIndicator_.getJavaScript()).append('}');
    if (!widgetset) {
      if (!app.hasQuit()) {
        out.append(this.session_.getApp().getJavaScriptClass())
            .append("._p_.update(null, 'load', null, false);\n");
      }
      out.append("};\n");
    }
    this.renderSetServerPush(out);
    out.append("Wt4.11.2.ready(function() { ")
        .append(app.getJavaScriptClass())
        .append("._p_.load(")
        .append(!widgetset)
        .append(");});\n");
    this.loadScriptLibraries(out, app, librariesLoaded);
  }
  // private void serveWidgetSet(final WebResponse request) ;
  private void collectJavaScript() throws IOException {
    WApplication app = this.session_.getApp();
    final Configuration conf = this.session_.getController().getConfiguration();
    logger.debug(
        new StringWriter()
            .append("Rendering invisible: ")
            .append(this.invisibleJS_.toString())
            .toString());
    this.collectedJS1_.append(this.invisibleJS_.toString());
    this.invisibleJS_.setLength(0);
    int librariesLoaded = this.loadScriptLibraries(this.collectedJS1_, app);
    this.loadScriptLibraries(this.collectedJS2_, app, librariesLoaded);
    app.streamBeforeLoadJavaScript(this.collectedJS1_, false);
    if (app.domRoot2_ != null) {
      app.domRoot2_.rootAsJavaScript(app, this.collectedJS1_, false);
    }
    this.collectJavaScriptUpdate(this.collectedJS1_);
    if (app.bodyHtmlClassChanged_) {
      boolean widgetset = this.session_.getType() == EntryPointType.WidgetSet;
      String op = widgetset ? "+=" : "=";
      this.collectedJS1_
          .append("document.body.parentNode.className")
          .append(op)
          .append('\'')
          .append(app.htmlClass_)
          .append("';")
          .append("document.body.className")
          .append(op)
          .append('\'')
          .append(this.getBodyClassRtl())
          .append("';")
          .append("document.body.setAttribute('dir', '");
      if (app.getLayoutDirection() == LayoutDirection.LeftToRight) {
        this.collectedJS1_.append("LTR");
      } else {
        this.collectedJS1_.append("RTL");
      }
      this.collectedJS1_.append("');");
    }
    if (app.htmlAttributeChanged_) {
      for (Iterator<Map.Entry<String, String>> i_it = app.getHtmlAttributes().entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String> i = i_it.next();
        this.collectedJS1_
            .append("document.documentElement.setAttribute('")
            .append(i.getKey())
            .append("', '")
            .append(i.getValue())
            .append("');\n");
      }
      app.htmlAttributeChanged_ = false;
    }
    if (app.bodyAttributeChanged_) {
      for (Iterator<Map.Entry<String, String>> i_it = app.getBodyAttributes().entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String> i = i_it.next();
        this.collectedJS1_
            .append("document.body.setAttribute('")
            .append(i.getKey())
            .append("', '")
            .append(i.getValue())
            .append("');\n");
      }
      app.bodyAttributeChanged_ = false;
    }
    if (this.visibleOnly_) {
      this.preCollectInvisibleChanges();
      if (this.twoPhaseThreshold_ > 0
          && this.invisibleJS_.length() < (int) this.twoPhaseThreshold_) {
        this.collectedJS1_.append(this.invisibleJS_.toString());
        this.invisibleJS_.setLength(0);
      } else {
        this.collectedJS1_
            .append(this.session_.getApp().getJavaScriptClass())
            .append("._p_.update(null, 'none', null, false);");
      }
    }
    if (conf.isInlineCss()) {
      app.getStyleSheet().javaScriptUpdate(app, this.collectedJS1_, false);
    }
    this.loadStyleSheets(this.collectedJS1_, app);
    if (app.autoJavaScriptChanged_) {
      this.collectedJS1_
          .append(app.getJavaScriptClass())
          .append("._p_.autoJavaScript=function(){")
          .append(app.autoJavaScript_)
          .append("};");
      app.autoJavaScriptChanged_ = false;
    }
    this.visibleOnly_ = true;
    app.domRoot_.doneRerender();
    if (app.domRoot2_ != null) {
      app.domRoot2_.doneRerender();
    }
    String redirect = this.session_.getRedirect();
    if (redirect.length() != 0) {
      this.streamRedirectJS(this.collectedJS1_, redirect);
    }
  }

  private void collectChanges(final List<DomElement> changes) {
    WApplication app = this.session_.getApp();
    do {
      this.moreUpdates_ = false;
      OrderedMultiMap<Integer, WWidget> depthOrder = new OrderedMultiMap<Integer, WWidget>();
      for (Iterator<WWidget> i_it = this.updateMap_.iterator(); i_it.hasNext(); ) {
        WWidget i = i_it.next();
        int depth = 1;
        WWidget ww = i;
        WWidget w = ww;
        for (; w.getParent() != null; ++depth) {
          w = w.getParent();
        }
        if (w != app.domRoot_ && w != app.domRoot2_) {
          logger.debug(
              new StringWriter()
                  .append("ignoring: ")
                  .append(ww.getId())
                  .append(" (")
                  .append("(fixme)")
                  .append(") ")
                  .append(w.getId())
                  .append(" (")
                  .append("(fixme)")
                  .append(")")
                  .toString());
          depth = 0;
        }
        depthOrder.put(depth, ww);
      }
      for (Iterator<Map.Entry<Integer, WWidget>> i_it = depthOrder.entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<Integer, WWidget> i = i_it.next();
        boolean j = this.updateMap_.contains(i.getValue());
        if (j != false) {
          WWidget w = i.getValue();
          if (i.getKey() == 0) {
            w.getWebWidget().propagateRenderOk();
            continue;
          }
          logger.debug(
              new StringWriter()
                  .append("updating: ")
                  .append(w.getId())
                  .append(" (")
                  .append("(fixme)")
                  .append(")")
                  .toString());
          if (!this.learning_ && this.visibleOnly_) {
            if (w.isRendered()) {
              w.getSDomChanges(changes, app);
            } else {
              logger.debug(new StringWriter().append("Ignoring: ").append(w.getId()).toString());
            }
          } else {
            w.getSDomChanges(changes, app);
          }
        }
      }
    } while (!this.learning_ && this.moreUpdates_);
  }

  private void collectJavaScriptUpdate(final StringBuilder out) {
    WApplication app = this.session_.getApp();
    try {
      if (this.session_.sessionIdChanged_) {
        if (this.session_.hasSessionIdInUrl()) {
          if (app.getEnvironment().hasAjax()
              && !app.getEnvironment().isInternalPathUsingFragments()) {
            this.streamRedirectJS(out, app.url(app.getInternalPath()));
          } else {
            this.streamRedirectJS(out, app.url(app.getInternalPath()));
          }
          return;
        }
        out.append(this.session_.getApp().getJavaScriptClass())
            .append("._p_.setSessionUrl(")
            .append(WWebWidget.jsStringLiteral(this.getSessionUrl()))
            .append(");");
        this.session_.sessionIdChanged_ = false;
      }
      this.collectJS(out);
      if (this.visibleOnly_) {
        this.preCollectInvisibleChanges();
        if (this.twoPhaseThreshold_ > 0
            && this.invisibleJS_.length() < (int) this.twoPhaseThreshold_) {
          this.collectedJS1_.append(this.invisibleJS_.toString());
          this.invisibleJS_.setLength(0);
        }
      }
      this.preLearnStateless(app, out);
      if (this.formObjectsChanged_) {
        String formObjectsList = this.createFormObjectsList(app);
        if (!formObjectsList.equals(this.currentFormObjectsList_)) {
          this.currentFormObjectsList_ = formObjectsList;
          out.append(app.getJavaScriptClass())
              .append("._p_.setFormObjects([")
              .append(this.currentFormObjectsList_)
              .append("]);");
        }
      }
      app.streamAfterLoadJavaScript(out);
      if (app.hasQuit()) {
        out.append(app.getJavaScriptClass())
            .append("._p_.quit(")
            .append(
                ((app.quittedMessage_.length() == 0)
                        ? "null"
                        : WString.toWString(app.quittedMessage_).getJsStringLiteral())
                    + ");");
      }
      if (this.updateLayout_) {
        out.append("window.onresize();");
        this.updateLayout_ = false;
      }
      app.renderedInternalPath_ = app.newInternalPath_;
      this.updateLoadIndicator(out, app, false);
    } catch (final RuntimeException e) {
      throw e;
    }
  }

  private void loadStyleSheet(
      final StringBuilder out, WApplication app, final WLinkedCssStyleSheet sheet) {
    out.append("Wt4.11.2")
        .append(".addStyleSheet('")
        .append(sheet.getLink().resolveUrl(app))
        .append("', '")
        .append(sheet.getMedia())
        .append("');\n ");
  }

  private void loadStyleSheets(final StringBuilder out, WApplication app) {
    int first = app.styleSheets_.size() - app.styleSheetsAdded_;
    for (int i = first; i < app.styleSheets_.size(); ++i) {
      this.loadStyleSheet(out, app, app.styleSheets_.get(i));
    }
    this.removeStyleSheets(out, app);
    app.styleSheetsAdded_ = 0;
  }

  private void removeStyleSheets(final StringBuilder out, WApplication app) {
    for (int i = (int) app.styleSheetsToRemove_.size() - 1; i > -1; --i) {
      out.append("Wt4.11.2")
          .append(".removeStyleSheet('")
          .append(app.styleSheetsToRemove_.get(i).getLink().resolveUrl(app))
          .append("');\n ");
      app.styleSheetsToRemove_.remove(0 + i);
    }
  }

  private int loadScriptLibraries(final StringBuilder out, WApplication app, int count) {
    if (count == -1) {
      int first = app.scriptLibraries_.size() - app.scriptLibrariesAdded_;
      for (int i = first; i < app.scriptLibraries_.size(); ++i) {
        String uri = this.session_.fixRelativeUrl(app.scriptLibraries_.get(i).uri);
        out.append(app.scriptLibraries_.get(i).beforeLoadJS)
            .append(app.getJavaScriptClass())
            .append("._p_.loadScript('")
            .append(uri)
            .append("',");
        DomElement.jsStringLiteral(out, app.scriptLibraries_.get(i).symbol, '\'');
        out.append(");\n");
        out.append(app.getJavaScriptClass())
            .append("._p_.onJsLoad(\"")
            .append(uri)
            .append("\",function() {\n");
      }
      count = app.scriptLibrariesAdded_;
      app.scriptLibrariesAdded_ = 0;
      return count;
    } else {
      if (count != 0) {
        out.append(app.getJavaScriptClass()).append("._p_.doAutoJavaScript();");
        for (int i = 0; i < count; ++i) {
          out.append("});");
        }
      }
      return 0;
    }
  }

  private final int loadScriptLibraries(final StringBuilder out, WApplication app) {
    return loadScriptLibraries(out, app, -1);
  }

  private void updateLoadIndicator(final StringBuilder out, WApplication app, boolean all) {
    if (app.showLoadingIndicator_.needsUpdate(all)) {
      out.append("showLoadingIndicator = function() {var o=null,e=null;\n")
          .append(app.showLoadingIndicator_.getJavaScript())
          .append("};\n");
      app.showLoadingIndicator_.updateOk();
    }
    if (app.hideLoadingIndicator_.needsUpdate(all)) {
      out.append("hideLoadingIndicator = function() {var o=null,e=null;\n")
          .append(app.hideLoadingIndicator_.getJavaScript())
          .append("};\n");
      app.hideLoadingIndicator_.updateOk();
    }
  }

  private void renderSetServerPush(final StringBuilder out) {
    if (this.session_.getApp().serverPushChanged_) {
      out.append(this.session_.getApp().getJavaScriptClass())
          .append("._p_.setServerPush(")
          .append(this.session_.getApp().isUpdatesEnabled())
          .append(");");
      this.session_.getApp().serverPushChanged_ = false;
    }
  }

  private void renderStyleSheet(
      final StringBuilder out, final WLinkedCssStyleSheet sheet, WApplication app) {
    out.append("<link href=\"");
    DomElement.htmlAttributeValue(out, sheet.getLink().resolveUrl(app));
    out.append("\" rel=\"stylesheet\" type=\"text/css\"");
    if (sheet.getMedia().length() != 0 && !sheet.getMedia().equals("all")) {
      out.append(" media=\"").append(sheet.getMedia()).append('"');
    }
    closeSpecial(out);
  }

  private String createFormObjectsList(WApplication app) {
    this.updateFormObjectsList(app);
    String result = "";
    for (Iterator<Map.Entry<String, WObject>> i_it = this.currentFormObjects_.entrySet().iterator();
        i_it.hasNext(); ) {
      Map.Entry<String, WObject> i = i_it.next();
      if (result.length() != 0) {
        result += ',';
      }
      result += "'" + i.getKey() + "'";
    }
    this.formObjectsChanged_ = false;
    return result;
  }

  private void preLearnStateless(WApplication app, final StringBuilder out) {
    if (!this.session_.getEnv().hasAjax()) {
      return;
    }
    this.collectJS(out);
    final WeakValueMap<String, AbstractEventSignal> ss = this.session_.getApp().exposedSignals();
    for (Iterator<Map.Entry<String, AbstractEventSignal>> i_it = ss.entrySet().iterator();
        i_it.hasNext(); ) {
      Map.Entry<String, AbstractEventSignal> i = i_it.next();
      AbstractEventSignal s = i.getValue();
      if (s.getOwner() == app) {
        s.processPreLearnStateless(this);
      } else {
        if (s.isCanAutoLearn()) {
          WWidget ww = (WWidget) s.getOwner();
          if (ww != null && ww.isRendered()) {
            s.processPreLearnStateless(this);
          }
        }
      }
    }
    out.append(this.statelessJS_.toString());
    this.statelessJS_.setLength(0);
  }

  private StringBuilder collectedJS1_;
  private StringBuilder collectedJS2_;
  private StringBuilder invisibleJS_;
  private StringBuilder statelessJS_;
  StringBuilder beforeLoadJS_;

  private void collectJS(StringBuilder js) {
    List<DomElement> changes = new ArrayList<DomElement>();
    this.collectChanges(changes);
    WApplication app = this.session_.getApp();
    if (js != null) {
      if (!this.isPreLearning()) {
        app.streamBeforeLoadJavaScript(js, false);
      }
      final Configuration conf = this.session_.getController().getConfiguration();
      if (conf.isInlineCss()) {
        app.getStyleSheet().javaScriptUpdate(app, js, false);
      }
      EscapeOStream sout = new EscapeOStream(js);
      for (int i = 0; i < changes.size(); ++i) {
        changes.get(i).asJavaScript(sout, DomElement.Priority.Delete);
      }
      for (int i = 0; i < changes.size(); ++i) {
        changes.get(i).asJavaScript(sout, DomElement.Priority.Update);
      }
    } else {
      for (int i = 0; i < changes.size(); ++i) {}
    }
    if (js != null) {
      if (app.titleChanged_) {
        js.append(app.getJavaScriptClass())
            .append("._p_.setTitle(")
            .append(WString.toWString(app.getTitle()).getJsStringLiteral())
            .append(");\n");
      }
      if (app.closeMessageChanged_) {
        js.append(app.getJavaScriptClass())
            .append("._p_.setCloseMessage(")
            .append(WString.toWString(app.getCloseMessage()).getJsStringLiteral())
            .append(");\n");
      }
      if (app.localeChanged_) {
        js.append(app.getJavaScriptClass())
            .append("._p_.setLocale(")
            .append(
                WString.toWString(new WString(app.getLocale().getLanguage())).getJsStringLiteral())
            .append(");\n");
      }
    }
    app.titleChanged_ = false;
    app.closeMessageChanged_ = false;
    app.localeChanged_ = false;
    if (js != null) {
      int librariesLoaded = this.loadScriptLibraries(js, app);
      app.streamAfterLoadJavaScript(js);
      if (app.internalPathIsChanged_) {
        js.append(app.getJavaScriptClass())
            .append("._p_.setHash(")
            .append(WWebWidget.jsStringLiteral(app.newInternalPath_))
            .append(", false);\n");
        if (!this.isPreLearning() && !app.getEnvironment().isInternalPathUsingFragments()) {
          this.session_.setPagePathInfo(app.newInternalPath_);
        }
      }
      this.loadScriptLibraries(js, app, librariesLoaded);
    } else {
      app.afterLoadJavaScript_ = "";
    }
    app.internalPathIsChanged_ = false;
    app.renderedInternalPath_ = app.newInternalPath_;
  }

  private void setPageVars(final FileServe page, final String nonce) {
    WApplication app = this.session_.getApp();
    final Configuration conf = this.session_.getController().getConfiguration();
    page.setVar("DOCTYPE", this.session_.getDocType());
    String htmlAttr = "";
    if (app != null && app.htmlClass_.length() != 0) {
      htmlAttr = " class=\"" + app.htmlClass_ + "\"";
    }
    if (app != null && !app.getHtmlAttributes().isEmpty()) {
      for (Iterator<Map.Entry<String, String>> i_it = app.getHtmlAttributes().entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String> i = i_it.next();
        htmlAttr += " " + i.getKey() + "=\"" + i.getValue() + "\"";
      }
    }
    if (this.session_.getEnv().agentIsIE()) {
      page.setVar(
          "HTMLATTRIBUTES",
          "xmlns:v=\"urn:schemas-microsoft-com:vml\" lang=\"en\" dir=\"ltr\"" + htmlAttr);
    } else {
      page.setVar("HTMLATTRIBUTES", "lang=\"en\" dir=\"ltr\"" + htmlAttr);
    }
    page.setVar("METACLOSE", ">");
    String attr = this.getBodyClassRtl();
    if (attr.length() != 0) {
      attr = " class=\"" + attr + "\"";
    }
    if (app != null && app.getLayoutDirection() == LayoutDirection.RightToLeft) {
      attr += " dir=\"RTL\"";
    }
    if (app != null && !app.getBodyAttributes().isEmpty()) {
      for (Iterator<Map.Entry<String, String>> i_it = app.getBodyAttributes().entrySet().iterator();
          i_it.hasNext(); ) {
        Map.Entry<String, String> i = i_it.next();
        attr += " " + i.getKey() + "=\"" + i.getValue() + "\"";
      }
    }
    page.setVar("BODYATTRIBUTES", attr);
    page.setVar("HEADDECLARATIONS", this.getHeadDeclarations());
    page.setCondition(
        "FORM", !this.session_.getEnv().agentIsSpiderBot() && !this.session_.getEnv().hasAjax());
    page.setCondition("BOOT_STYLE", true);
    page.setCondition("USE_NONCE", conf.isUseScriptNonce());
    page.setVar("NONCE", nonce);
    if (conf.isUseScriptNonce() && nonce.length() == 0) {
      logger.warn(
          new StringWriter()
              .append(
                  "An empty nonce has been defined. This may result in the CSP header not being correctly defined and used.")
              .toString());
    }
  }

  private void streamBootContent(final WebResponse response, final FileServe boot, boolean hybrid)
      throws IOException {
    final Configuration conf = this.session_.getController().getConfiguration();
    StringBuilder out = new StringBuilder();
    boot.setVar(
        "BLANK_HTML",
        this.session_.getBootstrapUrl(response, WebSession.BootstrapOption.ClearInternalPath)
            + "&amp;request=resource&amp;resource=blank");
    boot.setVar("SESSION_ID", this.session_.getSessionId());
    boot.setVar("APP_CLASS", "Wt");
    boot.streamUntil(out, "BOOT_JS");
    if (!(hybrid && this.session_.getApp().hasQuit())) {
      FileServe bootJs = new FileServe(WtServlet.Boot_js);
      bootJs.setVar(
          "SELF_URL",
          this.safeJsStringLiteral(
              this.session_.getBootstrapUrl(
                  response, WebSession.BootstrapOption.ClearInternalPath)));
      bootJs.setVar("SESSION_ID", this.session_.getSessionId());
      this.expectedAckId_ = this.scriptId_ = MathUtils.randomInt();
      this.ackErrs_ = 0;
      bootJs.setVar("SCRIPT_ID", this.scriptId_);
      bootJs.setVar("RANDOMSEED", MathUtils.randomInt());
      bootJs.setVar("RELOAD_IS_NEWSESSION", conf.reloadIsNewSession());
      bootJs.setVar(
          "USE_COOKIES", conf.getSessionTracking() == Configuration.SessionTracking.CookiesURL);
      bootJs.setVar(
          "AJAX_CANONICAL_URL", this.safeJsStringLiteral(this.session_.ajaxCanonicalUrl(response)));
      bootJs.setVar("APP_CLASS", "Wt");
      bootJs.setVar("PATH_INFO", this.safeJsStringLiteral(this.session_.pagePathInfo_));
      bootJs.setVar("DELAY_LOAD_AT_BOOT", conf.isDelayLoadAtBoot());
      bootJs.setCondition("COOKIE_CHECKS", conf.isCookieChecks());
      bootJs.setCondition("HYBRID", hybrid);
      bootJs.setCondition("PROGRESS", hybrid && !this.session_.getEnv().hasAjax());
      bootJs.setCondition("DEFER_SCRIPT", true);
      bootJs.setCondition("WEBGL_DETECT", conf.isWebglDetect());
      String internalPath =
          hybrid
              ? this.session_.getApp().getInternalPath()
              : this.session_.getEnv().getInternalPath();
      bootJs.setVar("INTERNAL_PATH", this.safeJsStringLiteral(internalPath));
      bootJs.stream(out);
    }
    response.out().append(out.toString());
  }

  private void addResponseAckPuzzle(final StringBuilder out) {
    String puzzle = "";
    final Configuration conf = this.session_.getController().getConfiguration();
    if (conf.ajaxPuzzle() && this.expectedAckId_ == this.scriptId_) {
      List<WContainerWidget> widgets = new ArrayList<WContainerWidget>();
      WApplication app = this.session_.getApp();
      this.addContainerWidgets(app.domRoot_, widgets);
      if (app.domRoot2_ != null) {
        this.addContainerWidgets(app.domRoot2_, widgets);
      }
      int r = MathUtils.randomInt() % widgets.size();
      WContainerWidget wc = widgets.get(r);
      puzzle = '"' + wc.getId() + '"';
      String l = "";
      for (WWidget w = wc.getParent(); w != null; w = w.getParent()) {
        if (w.getId().length() == 0) {
          continue;
        }
        if (w.getId().equals(l)) {
          continue;
        }
        l = w.getId();
        if (this.solution_.length() != 0) {
          this.solution_ += ',';
        }
        this.solution_ += l;
      }
    }
    ++this.expectedAckId_;
    logger.debug(
        new StringWriter()
            .append("addResponseAckPuzzle: incremented expectedAckId to ")
            .append(String.valueOf(this.expectedAckId_))
            .toString());
    out.append(this.session_.getApp().getJavaScriptClass())
        .append("._p_.response(")
        .append(this.expectedAckId_);
    if (puzzle.length() != 0) {
      out.append(",").append(puzzle);
    }
    out.append(");");
  }

  private void addContainerWidgets(WWebWidget w, final List<WContainerWidget> result) {
    for (int i = 0; i < w.getChildren().size(); ++i) {
      WWidget c = w.getChildren().get(i);
      if (!c.isRendered()) {
        return;
      }
      if (!c.isHidden()) {
        this.addContainerWidgets(c.getWebWidget(), result);
      }
      WContainerWidget wc = ObjectUtils.cast(c, WContainerWidget.class);
      if (wc != null) {
        result.add(wc);
      }
    }
  }

  private String getHeadDeclarations() {
    EscapeOStream result = new EscapeOStream();
    final Configuration conf = this.session_.getEnv().getServer().getConfiguration();
    final List<HeadMatter> headMatters = conf.getHeadMatter();
    for (int i = 0; i < headMatters.size(); ++i) {
      final HeadMatter m = headMatters.get(i);
      boolean add = true;
      if (m.getUserAgent().length() != 0) {
        String s = this.session_.getEnv().getUserAgent();
        Pattern expr = Pattern.compile(m.getUserAgent());
        if (!expr.matcher(s).find()) {
          add = false;
        }
      }
      if (add) {
        result.append(m.getContents());
      }
    }
    final List<MetaHeader> confMetaHeaders = conf.getMetaHeaders();
    List<MetaHeader> metaHeaders = new ArrayList<MetaHeader>();
    for (int i = 0; i < confMetaHeaders.size(); ++i) {
      final MetaHeader m = confMetaHeaders.get(i);
      boolean add = true;
      if (m.userAgent.length() != 0) {
        String s = this.session_.getEnv().getUserAgent();
        Pattern expr = Pattern.compile(m.userAgent);
        if (!expr.matcher(s).find()) {
          add = false;
        }
      }
      if (add) {
        metaHeaders.add(confMetaHeaders.get(i));
      }
    }
    if (this.session_.getApp() != null) {
      final List<MetaHeader> appMetaHeaders = this.session_.getApp().metaHeaders_;
      for (int i = 0; i < appMetaHeaders.size(); ++i) {
        final MetaHeader m = appMetaHeaders.get(i);
        boolean add = true;
        for (int j = 0; j < metaHeaders.size(); ++j) {
          final MetaHeader m2 = metaHeaders.get(j);
          if (m.type == m2.type && m.name.equals(m2.name)) {
            m2.content = m.content;
            add = false;
            break;
          }
        }
        if (add) {
          metaHeaders.add(m);
        }
      }
    }
    for (int i = 0; i < metaHeaders.size(); ++i) {
      final MetaHeader m = metaHeaders.get(i);
      result.append("<meta");
      if (m.name.length() != 0) {
        String attribute = "";
        switch (m.type) {
          case Meta:
            attribute = "name";
            break;
          case Property:
            attribute = "property";
            break;
          case HttpHeader:
            attribute = "http-equiv";
            break;
        }
        appendAttribute(result, attribute, m.name);
      }
      if (m.lang.length() != 0) {
        appendAttribute(result, "lang", m.lang);
      }
      appendAttribute(result, "content", m.content.toString());
      closeSpecial(result);
    }
    if (this.session_.getApp() != null) {
      for (int i = 0; i < this.session_.getApp().metaLinks_.size(); ++i) {
        final WApplication.MetaLink ml = this.session_.getApp().metaLinks_.get(i);
        result.append("<link");
        appendAttribute(result, "href", ml.href);
        appendAttribute(result, "rel", ml.rel);
        if (ml.media.length() != 0) {
          appendAttribute(result, "media", ml.media);
        }
        if (ml.hreflang.length() != 0) {
          appendAttribute(result, "hreflang", ml.hreflang);
        }
        if (ml.type.length() != 0) {
          appendAttribute(result, "type", ml.type);
        }
        if (ml.sizes.length() != 0) {
          appendAttribute(result, "sizes", ml.sizes);
        }
        if (ml.disabled) {
          appendAttribute(result, "disabled", "");
        }
        closeSpecial(result);
      }
    } else {
      if (this.session_.getEnv().agentIsIE()) {
        if (this.session_.getEnv().agentIsIElt(9)) {
          boolean selectIE7 = conf.getUaCompatible().indexOf("IE8=IE7") != -1;
          if (selectIE7) {
            result.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=7\"");
            closeSpecial(result);
          }
        } else {
          if (this.session_.getEnv().getAgent() == UserAgent.IE9) {
            result.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=9\"");
            closeSpecial(result);
          } else {
            if (this.session_.getEnv().getAgent() == UserAgent.IE10) {
              result.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=10\"");
              closeSpecial(result);
            } else {
              result.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=11\"");
              closeSpecial(result);
            }
          }
        }
      }
    }
    if (this.session_.getFavicon().length() != 0) {
      result
          .append("<link rel=\"shortcut icon\" href=\"")
          .append(this.session_.getFavicon())
          .append('"');
      closeSpecial(result);
    }
    String baseUrl = "";
    baseUrl = WApplication.readConfigurationProperty("baseURL", baseUrl);
    if (baseUrl.length() != 0) {
      result.append("<base href=\"").append(baseUrl).append('"');
      closeSpecial(result);
    }
    return result.toString();
  }

  private String getBodyClassRtl() {
    if (this.session_.getApp() != null) {
      String s = this.session_.getApp().bodyClass_;
      if (s.length() != 0) {
        s += ' ';
      }
      s +=
          this.session_.getApp().getLayoutDirection() == LayoutDirection.LeftToRight
              ? "Wt-ltr"
              : "Wt-rtl";
      this.session_.getApp().bodyHtmlClassChanged_ = false;
      return s;
    } else {
      return "";
    }
  }

  private String getSessionUrl() {
    String result = this.session_.getApplicationUrl();
    if (isAbsoluteUrl(result)) {
      return this.session_.appendSessionQuery(result);
    } else {
      return this.session_.appendSessionQuery(".").substring(1);
    }
  }

  private Set<WWidget> updateMap_;
  private boolean learning_;
  private boolean learningIncomplete_;
  private boolean moreUpdates_;

  private String safeJsStringLiteral(final String value) {
    String s = WWebWidget.jsStringLiteral(value);
    return StringUtils.replace(s, "<", "<'+'");
  }

  void addWsRequestId(int wsRqId) {
    this.wsRequestsToHandle_.add(wsRqId);
  }

  private void renderWsRequestsDone(final StringBuilder out) {
    if (!this.wsRequestsToHandle_.isEmpty()) {
      out.append(this.session_.getApp().getJavaScriptClass()).append("._p_.wsRqsDone(");
      for (int i = 0; i < this.wsRequestsToHandle_.size(); ++i) {
        if (i != 0) {
          out.append(',');
        }
        out.append(this.wsRequestsToHandle_.get(i));
      }
      out.append(");");
      this.wsRequestsToHandle_.clear();
    }
  }

  void updateMultiSessionCookie(final WebRequest request) {
    final Configuration conf = this.session_.getController().getConfiguration();
    javax.servlet.http.Cookie cookie =
        new javax.servlet.http.Cookie(
            "ms" + request.getScriptName(), this.session_.getMultiSessionId());
    cookie.setMaxAge(conf.getMultiSessionCookieTimeout());
    cookie.setSecure(this.session_.getEnv().getUrlScheme().equals("https"));
    this.setCookie(cookie);
  }

  private void renderCookieUpdate(final StringBuilder out) {
    if (this.cookieUpdateNeeded_) {
      out.append(this.session_.getApp().getJavaScriptClass()).append("._p_.refreshCookie();");
      this.cookieUpdateNeeded_ = false;
    }
  }

  private void preCollectInvisibleChanges() {
    if (this.visibleOnly_ && !this.updateMap_.isEmpty() && this.twoPhaseThreshold_ > 0) {
      this.visibleOnly_ = false;
      this.collectJavaScriptUpdate(this.invisibleJS_);
      this.visibleOnly_ = true;
    }
  }

  public String learn(AbstractEventSignal.LearningListener slot) throws IOException {
    if (slot.isInvalidated()) {
      return "";
    }
    if (slot.getType() == SlotType.PreLearnStateless) {
      this.learning_ = true;
    }
    this.learningIncomplete_ = false;
    this.currentStatelessSlotIsActuallyStateless_ = true;
    slot.trigger();
    StringBuilder js = new StringBuilder();
    this.collectJS(js);
    String result = js.toString();
    logger.debug(new StringWriter().append("learned: ").append(result).toString());
    if (slot.getType() == SlotType.PreLearnStateless) {
      slot.undoTrigger();
      this.collectJS((StringBuilder) null);
      this.learning_ = false;
    } else {
      this.statelessJS_.append(result);
    }
    if (this.currentStatelessSlotIsActuallyStateless_ && !this.learningIncomplete_) {
      slot.setJavaScript(result);
    } else {
      if (!this.currentStatelessSlotIsActuallyStateless_) {
        slot.invalidate();
      }
    }
    this.collectJS(this.statelessJS_);
    return result;
  }

  static boolean isAbsoluteUrl(final String url) {
    return url.indexOf("://") != -1;
  }

  static void appendAttribute(final EscapeOStream eos, final String name, final String value) {
    eos.append(' ').append(name).append("=\"");
    eos.pushEscape(EscapeOStream.RuleSet.HtmlAttribute);
    eos.append(value);
    eos.popEscape();
    eos.append('"');
  }

  static void closeSpecial(final StringBuilder s) {
    s.append(">\n");
  }

  static void closeSpecial(final EscapeOStream s) {
    s.append(">\n");
  }
}
