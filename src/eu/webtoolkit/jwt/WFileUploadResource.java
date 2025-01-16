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

final class WFileUploadResource extends WResource {
  private static Logger logger = LoggerFactory.getLogger(WFileUploadResource.class);

  public WFileUploadResource(WFileUpload fileUpload) {
    super();
    this.fileUpload_ = fileUpload;
  }

  protected void handleRequest(final WebRequest request, final WebResponse response)
      throws IOException {
    boolean triggerUpdate = false;
    List<UploadedFile> files = new ArrayList<UploadedFile>();
    CollectionUtils.findInMultimap(request.getUploadedFiles(), "data", files);
    if (!(0 != 0)) {
      if (!files.isEmpty() || request.getParameter("data") != null) {
        triggerUpdate = true;
      }
    }
    response.setContentType("text/html; charset=utf-8");
    Writer o = response.out();
    o.append("<!DOCTYPE html>")
        .append("<html>\n")
        .append("<head><script")
        .append(" type=\"text/javascript\"");
    if (response.getNonce().length() != 0) {
      o.append(" nonce=\"").append(response.getNonce()).append("\"");
    }
    o.append(">\n").append("function load() { ");
    if (triggerUpdate || 0 != 0) {
      UserAgent agent = WApplication.getInstance().getEnvironment().getAgent();
      if (triggerUpdate) {
        logger.debug(
            new StringWriter().append("Resource handleRequest(): signaling uploaded").toString());
        if (agent == UserAgent.IE6 || agent == UserAgent.IE7) {
          o.append("window.parent.")
              .append(WApplication.getInstance().getJavaScriptClass())
              .append("._p_.update(null, '")
              .append(this.fileUpload_.uploaded().encodeCmd())
              .append("', null, true);");
        } else {
          o.append("window.parent.postMessage(")
              .append("JSON.stringify({ fu: '")
              .append(this.fileUpload_.getId())
              .append("',")
              .append("  signal: '")
              .append(this.fileUpload_.uploaded().encodeCmd())
              .append("',type: 'upload'")
              .append("}), '*');");
        }
      } else {
        if (0 != 0) {
          logger.debug(
              new StringWriter()
                  .append("Resource handleRequest(): signaling file-too-large")
                  .toString());
          String s = String.valueOf(0);
          if (agent == UserAgent.IE6 || agent == UserAgent.IE7) {
            o.append(this.fileUpload_.fileTooLarge().createCall(s));
          } else {
            o.append(" window.parent.postMessage(")
                .append("JSON.stringify({")
                .append("fileTooLargeSize: '")
                .append(s)
                .append("',type: 'file_too_large'")
                .append("'}), '*');");
          }
        }
      }
    } else {
      logger.debug(new StringWriter().append("Resource handleRequest(): no signal").toString());
    }
    o.append("}\n</script></head><body onload=\"load();\"></body></html>");
    if (!(0 != 0) && !files.isEmpty()) {
      this.fileUpload_.setFiles(files);
    }
  }

  private WFileUpload fileUpload_;
  private static UploadedFile uploaded;
}
