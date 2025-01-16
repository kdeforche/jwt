/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.auth;

import eu.webtoolkit.jwt.*;
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
 * An OAuth authorization (and authentication) process.
 *
 * <p>The process implements the state machine that is needed to complete an OAuth authorization
 * cycle.
 *
 * <p>Optionally, it also provides authentication, by using the service-specific logic which uses
 * the access token to return identity information.
 *
 * <p>A process is created for a particular scope, which represents what kind of information one
 * wants to access, and which is used to inform the user of the kind of operations he needs to
 * authorize for your application to make with his protected data.
 *
 * <p>
 *
 * @see OAuthService#createProcess(String scope)
 */
public class OAuthProcess extends WObject {
  private static Logger logger = LoggerFactory.getLogger(OAuthProcess.class);

  /**
   * Returns the scope for which this process was created.
   *
   * <p>The scope represents how much protected information the web application wants to access, and
   * in what way.
   *
   * <p>
   *
   * @see OAuthService#createProcess(String scope)
   * @see OAuthService#getAuthenticationScope()
   */
  public String getScope() {
    return this.scope_;
  }
  /**
   * Returns the OAuth service which spawned this process.
   *
   * <p>
   *
   * @see OAuthService#createProcess(String scope)
   */
  public OAuthService getService() {
    return this.service_;
  }
  /**
   * Starts an authorization process.
   *
   * <p>This starts an authorization process to request an accesstoken to access protected
   * information within the process scope.
   *
   * <p>The authorization process ends with the {@link OAuthProcess#authorized() authorized()}
   * signal which signals the obtained token.
   *
   * <p>
   *
   * <p><i><b>Note: </b>To be able to use a popup (instead of a page redirect), you should connect
   * this method directly to an, since popup windows are blocked in most web browsers unless they
   * are the direct consequence of an event. </i>
   */
  public void startAuthorize() {
    WApplication app = WApplication.getInstance();
    if (app.getEnvironment().hasJavaScript() && this.service_.isPopupEnabled()) {
      return;
    }
    this.redirectEndpoint_.getUrl();
    int timeout = 600;
    String value = "";
    if (app.readConfigurationProperty("oauth2-redirect-timeout", value) != null) {
      try {
        timeout = Integer.parseInt(value);
      } catch (final RuntimeException e) {
        logger.error(
            new StringWriter()
                .append(
                    WString.tr(
                        "Wt.Auth.OAuthService.could not convert 'oauth2-redirect-timeout' to int: "))
                .append(value)
                .toString());
      }
    }
    app.suspend(Duration.ofSeconds(timeout));
    this.startInternalPath_ = app.getInternalPath();
    app.redirect(this.getAuthorizeUrl());
  }
  /**
   * Starts an authorization and authentication process.
   *
   * <p>This is {@link OAuthProcess#startAuthorize() startAuthorize()} followed by {@link
   * OAuthProcess#getIdentity(OAuthAccessToken token) getIdentity()}.
   *
   * <p>This requires that the process is created with an authorization scope that includes
   * sufficient rights for authentication (at least {@link OAuthService#getAuthenticationScope()})
   *
   * <p>The authentication process ends with the {@link OAuthProcess#authenticated()
   * authenticated()} signal which signals the obtained identity.
   *
   * <p>
   *
   * <p><i><b>Note: </b>To be able to use a popup (instead of a page redirect), you should connect
   * this method directly to an, since popup windows are blocked in most web browsers unless they
   * are the direct consequence of an event. </i>
   */
  public void startAuthenticate() {
    this.authenticate_ = true;
    this.startAuthorize();
  }
  /**
   * Connects an implementation to start an authentication process to a signal.
   *
   * <p>If JavaScript is available, this method connects a JavaScript function to the <code>signal
   * </code>, otherwise {@link OAuthProcess#startAuthenticate() startAuthenticate()} is connected to
   * <code>signal</code>.
   */
  public void connectStartAuthenticate(final AbstractEventSignal s) {
    if (WApplication.getInstance().getEnvironment().hasJavaScript()
        && this.service_.isPopupEnabled()) {
      StringBuilder js = new StringBuilder();
      js.append("function(object, event) {")
          .append("Wt4.11.2.PopupWindow(Wt4.11.2")
          .append(",")
          .append(WWebWidget.jsStringLiteral(this.getAuthorizeUrl()))
          .append(", ")
          .append(this.service_.getPopupWidth())
          .append(", ")
          .append(this.service_.getPopupHeight())
          .append(");")
          .append("}");
      s.addListener(js.toString());
    }
    s.addListener(
        this,
        () -> {
          OAuthProcess.this.startAuthenticate();
        });
  }
  /**
   * Obtains an authenticated identity.
   *
   * <p>The authentication process uses an access token to issue one or more protected requests for
   * obtaining identity information. This is not part of the OAuth protocol, since OAuth does not
   * standardize the use of the access token to obtain this information.
   *
   * <p>The authentication process ends with the {@link OAuthProcess#authenticated()
   * authenticated()} signal which signals the obtained identity.
   */
  public void getIdentity(final OAuthAccessToken token) {
    throw new WException("OAuth::Process::Identity(): not specialized");
  }
  /**
   * Error information, in case authentication or identification failed.
   *
   * <p>The error message contains details when the {@link OAuthProcess#authorized() authorized()}
   * or {@link OAuthProcess#authenticated() authenticated()} signals indicate respectively an
   * invalid token or invalid identity.
   */
  public WString getError() {
    return this.error_;
  }
  /**
   * Returns the access token.
   *
   * <p>This returns the access token that was obtained in the last authorization cycle.
   */
  public OAuthAccessToken getToken() {
    return this.token_;
  }
  /**
   * Authorization signal.
   *
   * <p>This signal indicates the end of an authorization process started with {@link
   * OAuthProcess#startAuthorize() startAuthorize()}. If the authorization process was successful,
   * then the parameter carries a valid access token that was obtained. If the authorization process
   * failed then the access token parameter is invalid, and you can get more information using
   * {@link OAuthProcess#getError() getError()}.
   *
   * <p>Authorization can fail because of a protocol error, aconfiguration problem, or because the
   * user denied the authorization.
   *
   * <p>
   *
   * @see OAuthProcess#startAuthorize()
   * @see OAuthAccessToken#isValid()
   */
  public Signal1<OAuthAccessToken> authorized() {
    return this.authorized_;
  }
  /**
   * Authentication signal.
   *
   * <p>This signal indicates the end of an authentication process started with {@link
   * OAuthProcess#startAuthenticate() startAuthenticate()} or {@link
   * OAuthProcess#getIdentity(OAuthAccessToken token) getIdentity()}. If the authentication process
   * was successful, then the parameter is a valid and authentic identity. If the authentication
   * process failed then the identity parameter is invalid, and you can get more information using
   * {@link OAuthProcess#getError() getError()}.
   *
   * <p>Authentication can fail because authorization failed (in case of {@link
   * OAuthProcess#startAuthenticate() startAuthenticate()}), or because of a protocol error, or
   * configuration problem.
   *
   * <p>
   *
   * @see OAuthProcess#startAuthenticate()
   * @see OAuthProcess#getIdentity(OAuthAccessToken token)
   * @see Identity#isValid()
   */
  public Signal1<Identity> authenticated() {
    return this.authenticated_;
  }
  /**
   * Constructor.
   *
   * <p>
   *
   * @see OAuthService#createProcess(String scope)
   */
  protected OAuthProcess(final OAuthService service, final String scope) {
    super();
    this.service_ = service;
    this.scope_ = scope;
    this.authenticate_ = false;
    this.authorized_ = new Signal1<OAuthAccessToken>();
    this.authenticated_ = new Signal1<Identity>();
    this.redirected_ = new JSignal(this, "redirected");
    this.oAuthState_ = "";
    this.token_ = new OAuthAccessToken();
    this.error_ = new WString();
    this.startInternalPath_ = "";
    this.redirectEndpoint_ = null;
    this.httpClient_ = null;
    this.doneCallbackConnection_ = new AbstractSignal.Connection();
    this.redirectEndpoint_ = new OAuthRedirectEndpoint(this);
    WApplication app = WApplication.getInstance();
    PopupWindow.loadJavaScript(app);
    String url = app.makeAbsoluteUrl(this.redirectEndpoint_.getUrl());
    this.oAuthState_ = this.service_.encodeState(url);
    this.redirected_.addListener(
        this,
        () -> {
          OAuthProcess.this.onOAuthDone();
        });
  }
  /**
   * Exception thrown while parsing a token response.
   *
   * <p>
   *
   * @see OAuthProcess#parseTokenResponse(HttpMessage response)
   */
  protected static class TokenError extends RuntimeException {
    private static Logger logger = LoggerFactory.getLogger(TokenError.class);

    /** Constructor. */
    public TokenError(final CharSequence error) {
      super();
      this.error_ = WString.toWString(error);
    }
    /** The error. */
    public WString getError() {
      return this.error_;
    }

    private WString error_;
  }
  /**
   * Parses the response for a token request.
   *
   * <p>Throws a {@link TokenError} when the response indicates an error, or when the response could
   * not be properly parsed.
   *
   * <p>Some OAuth implementations may uses a non-standard encoding of the token.
   */
  protected OAuthAccessToken parseTokenResponse(final HttpMessage response) {
    if (response.getStatus() == 200 || response.getStatus() == 400) {
      String contenttype = response.getHeader("Content-Type");
      if (contenttype != null) {
        String mimetype = contenttype.trim();
        List<String> tokens = new ArrayList<String>();
        StringUtils.split(tokens, mimetype, ";", false);
        String combinedType = "";
        String params = "";
        if (tokens.size() > 0) {
          combinedType = tokens.get(0);
          combinedType = combinedType.trim();
        }
        if (tokens.size() > 1) {
          params = tokens.get(1);
          params = params.trim();
        }
        if (combinedType.equals("text/plain")) {
          if (params.startsWith("charset=UTF-8")) {
            return this.parseUrlEncodedToken(response);
          } else {
            throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badresponse"));
          }
        } else {
          if (combinedType.equals("application/json")) {
            return this.parseJsonToken(response);
          } else {
            throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badresponse"));
          }
        }
      } else {
        throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badresponse"));
      }
    } else {
      throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badresponse"));
    }
  }
  /**
   * Sets the error.
   *
   * <p>This should be used in {@link OAuthProcess#getIdentity(OAuthAccessToken token)
   * getIdentity()} implementations to set the error, before emitting {@link
   * OAuthProcess#authenticated() authenticated()} with an invalid {@link Identity}.
   */
  protected void setError(final CharSequence error) {
    this.error_ = WString.toWString(error);
  }

  final OAuthService service_;
  private String scope_;
  private boolean authenticate_;
  private Signal1<OAuthAccessToken> authorized_;
  private Signal1<Identity> authenticated_;
  JSignal redirected_;
  String oAuthState_;
  private OAuthAccessToken token_;
  private WString error_;
  String startInternalPath_;
  private OAuthRedirectEndpoint redirectEndpoint_;
  private HttpClient httpClient_;
  AbstractSignal.Connection doneCallbackConnection_;

  void requestToken(final String authorizationCode) {
    try {
      String url = this.service_.getTokenEndpoint();
      Method m = this.service_.getTokenRequestMethod();
      StringBuilder ss = new StringBuilder();
      ss.append("grant_type=authorization_code")
          .append("&redirect_uri=")
          .append(Utils.urlEncode(this.service_.getGenerateRedirectEndpoint()))
          .append("&code=")
          .append(authorizationCode);
      this.httpClient_ = new HttpClient();
      this.httpClient_.setTimeout(Duration.ofSeconds(15));
      this.httpClient_
          .done()
          .addListener(
              this,
              (Exception event1, HttpMessage event2) -> {
                OAuthProcess.this.handleToken(event1, event2);
              });
      String clientId = Utils.urlEncode(this.service_.getClientId());
      String clientSecret = Utils.urlEncode(this.service_.getClientSecret());
      if (m == Method.Get) {
        List<org.apache.http.Header> headers = new ArrayList<org.apache.http.Header>();
        if (this.service_.getClientSecretMethod() == ClientSecretMethod.HttpAuthorizationBasic) {
          headers.add(
              new org.apache.http.message.BasicHeader(
                  "Authorization",
                  "Basic " + Utils.base64Encode(clientId + ":" + clientSecret, false)));
        } else {
          if (this.service_.getClientSecretMethod() == ClientSecretMethod.PlainUrlParameter) {
            ss.append("&client_id=")
                .append(clientId)
                .append("&client_secret=")
                .append(clientSecret);
          }
        }
        boolean hasQuery = url.indexOf('?') != -1;
        url += (hasQuery ? '&' : '?') + ss.toString();
        this.httpClient_.get(url, headers);
      } else {
        HttpMessage post = new HttpMessage();
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        if (this.service_.getClientSecretMethod() == ClientSecretMethod.HttpAuthorizationBasic) {
          post.setHeader(
              "Authorization", "Basic " + Utils.base64Encode(clientId + ":" + clientSecret, false));
        } else {
          if (this.service_.getClientSecretMethod() == ClientSecretMethod.RequestBodyParameter) {
            ss.append("&client_id=")
                .append(clientId)
                .append("&client_secret=")
                .append(clientSecret);
          }
        }
        post.addBodyText(ss.toString());
        this.httpClient_.post(url, post);
      }
    } catch (Exception e) {
      logger.info("Ignoring exception {}", e.getMessage(), e);
    }
  }

  private void handleToken(Exception err, final HttpMessage response) {
    if (err == null) {
      this.doParseTokenResponse(response);
    } else {
      logger.error(
          new StringWriter().append("handleToken(): ").append(err.getMessage()).toString());
      this.setError(new WString(err.getMessage()));
    }
    WApplication app = WApplication.getInstance();
    if (app.getEnvironment().hasAjax()) {
    } else {
      this.onOAuthDone();
    }
  }

  private OAuthAccessToken parseUrlEncodedToken(final HttpMessage response) {
    Map<String, String[]> params = new HashMap<String, String[]>();
    AuthUtils.parseFormUrlEncoded(response, params);
    if (response.getStatus() == 200) {
      String accessTokenE = AuthUtils.getParamValue(params, "access_token");
      if (accessTokenE != null) {
        String accessToken = accessTokenE;
        WDate expires = null;
        String expiresE = AuthUtils.getParamValue(params, "expires");
        if (expiresE != null) {
          expires = WDate.getCurrentServerDate().addSeconds(Integer.parseInt(expiresE));
        }
        return new OAuthAccessToken(accessToken, expires, "");
      } else {
        throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badresponse"));
      }
    } else {
      String errorE = AuthUtils.getParamValue(params, "error");
      if (errorE != null) {
        throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService." + errorE));
      } else {
        throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badresponse"));
      }
    }
  }

  private OAuthAccessToken parseJsonToken(final HttpMessage response) {
    com.google.gson.JsonObject root = new com.google.gson.JsonObject();
    com.google.gson.JsonParseException pe = null;
    try {
      root =
          (com.google.gson.JsonObject) new com.google.gson.JsonParser().parse(response.getBody());
    } catch (com.google.gson.JsonParseException error) {
      pe = error;
    }
    boolean ok = root != null;
    if (!ok) {
      logger.error(
          new StringWriter().append("parseJsonToken(): ").append(pe.toString()).toString());
      throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badjson"));
    } else {
      if (response.getStatus() == 200) {
        try {
          String accessToken = root.get("access_token").getAsString();
          int secs = JsonUtils.orIfNullInt(root.get("expires_in"), -1);
          WDate expires = null;
          if (secs > 0) {
            expires = WDate.getCurrentServerDate().addSeconds(secs);
          }
          String refreshToken = JsonUtils.orIfNullString(root.get("refresh_token"), "");
          String idToken = JsonUtils.orIfNullString(root.get("id_token"), "");
          return new OAuthAccessToken(accessToken, expires, refreshToken, idToken);
        } catch (final RuntimeException e) {
          logger.error(
              new StringWriter().append("token response error: ").append(e.toString()).toString());
          throw new OAuthProcess.TokenError(WString.tr("Wt.Auth.OAuthService.badresponse"));
        }
      } else {
        throw new OAuthProcess.TokenError(
            WString.tr(
                "Wt.Auth.OAuthService."
                    + JsonUtils.orIfNullString(root.get("error"), "missing error")));
      }
    }
  }

  private String getAuthorizeUrl() {
    StringBuilder url = new StringBuilder();
    url.append(this.service_.getAuthorizationEndpoint());
    boolean hasQuery = url.toString().indexOf('?') != -1;
    url.append(hasQuery ? '&' : '?')
        .append("client_id=")
        .append(Utils.urlEncode(this.service_.getClientId()))
        .append("&redirect_uri=")
        .append(Utils.urlEncode(this.service_.getGenerateRedirectEndpoint()))
        .append("&scope=")
        .append(Utils.urlEncode(this.scope_))
        .append("&response_type=code")
        .append("&state=")
        .append(Utils.urlEncode(this.oAuthState_));
    logger.info(new StringWriter().append("authorize URL: ").append(url.toString()).toString());
    return url.toString();
  }

  private void doParseTokenResponse(final HttpMessage response) {
    try {
      this.token_ = this.parseTokenResponse(response);
    } catch (final OAuthProcess.TokenError e) {
      this.error_ = e.getError();
    }
  }

  void onOAuthDone() {
    boolean success = (this.error_.length() == 0);
    this.authorized().trigger(success ? this.token_ : OAuthAccessToken.Invalid);
    if (success && this.authenticate_) {
      this.authenticate_ = false;
      this.getIdentity(this.token_);
    }
    if (this.doneCallbackConnection_.isConnected()) {
      this.doneCallbackConnection_.disconnect();
    }
  }
}
