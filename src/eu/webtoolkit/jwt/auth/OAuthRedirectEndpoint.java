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

final class OAuthRedirectEndpoint extends WResource {
  private static Logger logger = LoggerFactory.getLogger(OAuthRedirectEndpoint.class);

  public OAuthRedirectEndpoint(OAuthProcess process) {
    super();
    this.process_ = process;
  }

  public void sendError(final WebResponse response) throws IOException {
    response.setStatus(500);
    Writer o = response.out();
    o.append("<html><body>OAuth error</body></html>");
  }

  public void handleRequest(final WebRequest request, final WebResponse response)
      throws IOException {
    response.setContentType("text/html; charset=UTF-8");
    String stateE = request.getParameter("state");
    if (!(stateE != null) || !stateE.equals(this.process_.oAuthState_)) {
      logger.error(
          new StringWriter()
              .append(WString.tr("Wt.Auth.OAuthService.invalid-state"))
              .append(", state: ")
              .append(stateE != null ? stateE : "(empty)")
              .toString());
      this.process_.setError(WString.tr("Wt.Auth.OAuthService.invalid-state"));
      this.sendError(response);
      return;
    }
    String errorE = request.getParameter("error");
    if (errorE != null) {
      logger.error(
          new StringWriter().append(WString.tr("Wt.Auth.OAuthService." + errorE)).toString());
      this.process_.setError(WString.tr("Wt.Auth.OAuthService." + errorE));
      this.sendError(response);
      return;
    }
    String codeE = request.getParameter("code");
    if (!(codeE != null)) {
      logger.error(
          new StringWriter().append(WString.tr("Wt.Auth.OAuthService.missing-code")).toString());
      this.process_.setError(WString.tr("Wt.Auth.OAuthService.missing-code"));
      this.sendError(response);
      return;
    }
    this.process_.requestToken(codeE);
    this.sendResponse(response);
  }

  public void sendResponse(final WebResponse response) throws IOException {
    Writer o = response.out();
    WApplication app = WApplication.getInstance();
    final boolean usePopup =
        app.getEnvironment().hasAjax() && this.process_.service_.isPopupEnabled();
    if (!usePopup) {
      this.process_.doneCallbackConnection_ =
          app.unsuspended()
              .addListener(
                  this.process_,
                  () -> {
                    OAuthRedirectEndpoint.this.process_.onOAuthDone();
                  });
      String redirectTo = app.makeAbsoluteUrl(app.url(this.process_.startInternalPath_));
      o.append(
              "<!DOCTYPE html><html lang=\"en\" dir=\"ltr\">\n<head><meta http-equiv=\"refresh\" content=\"0; url=")
          .append(redirectTo)
          .append("\" /></head>\n<body><p><a href=\"")
          .append(redirectTo)
          .append("\"> Click here to continue</a></p></body></html>");
    } else {
      String appJs = app.getJavaScriptClass();
      o.append(
          "<!DOCTYPE html><html lang=\"en\" dir=\"ltr\">\n<head><title></title>\n<script type=\"text/javascript\"");
      if (response.getNonce().length() != 0) {
        o.append(" nonce=\"").append(response.getNonce()).append("\"");
      }
      o.append(">\nfunction load() { if (window.opener.")
          .append(appJs)
          .append(") {var ")
          .append(appJs)
          .append("= window.opener.")
          .append(appJs)
          .append(";")
          .append(this.process_.redirected_.createCall())
          .append(";window.close();}\n}\n</script></head><body onload=\"load();\"></body></html>");
    }
  }

  private OAuthProcess process_;
}
