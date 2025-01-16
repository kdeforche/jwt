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
 * Class with helper functions to define throttling behaviour.
 *
 * <p>During authentication, it may be desirable to limit the number of submissions a client can
 * perform in order to prevent brute force attacks. This class allows developers to manage the
 * throttling behaviour. The main functions offered by this class are:
 *
 * <ol>
 *   <li>compute how long a user has to wait before the next authentication attempt
 *   <li>offer methods to show interactive feedback (count-down timer) on the waiting time to the
 *       user
 * </ol>
 *
 * <p>This class is used in {@link PasswordService} and in AbstractMfaProcess. By specializing this
 * class, you can change JWt&apos;s behaviour related to authentication throttling.
 *
 * <p>Throttling has to be enabled; it is enabled by calling {@link
 * PasswordService#setAttemptThrottlingEnabled(boolean enabled)
 * PasswordService#setAttemptThrottlingEnabled()} for password authentication, or {@link
 * AuthService#setMfaThrottleEnabled(boolean enabled) AuthService#setMfaThrottleEnabled()} for MFA
 * tokens.
 *
 * <p>By default this is disabled in both cases.
 *
 * <p>
 *
 * <p><i><b>Note: </b>for MFA one can also call {@link
 * AbstractMfaProcess#setMfaThrottle(AuthThrottle authThrottle) AbstractMfaProcess#setMfaThrottle()}
 * directly. The {@link AuthService} state can be used for the default TotpProcess implementation,
 * or to manage your own state in case of a custom MFA approach. </i>
 */
public class AuthThrottle {
  private static Logger logger = LoggerFactory.getLogger(AuthThrottle.class);

  // public  AuthThrottle() ;
  // public void  destroy() ;
  /**
   * Returns the amount of seconds the given {@link User} has to wait for the next authentication
   * attempt.
   *
   * <p>The default implementation queries the given user object for the last login attempt ({@link
   * User#getLastLoginAttempt()}) and the amount of consecutive failed logins ({@link
   * User#getFailedLoginAttempts()}), to compute the remaining wait time for this user. This is
   * calculated using {@link AuthThrottle#getAuthenticationThrottle(int failedAttempts)
   * getAuthenticationThrottle()} and the current server time.
   *
   * <p>Since the {@link User} object normally fetches this data from a database, throttling works
   * across different sessions, i.e. opening a new tab to get around throttling will not work.
   *
   * <p>This function returns 0 if a new attempt is permitted, or the amount of seconds the user has
   * to wait for the next attempt.
   *
   * <p>
   *
   * @see AbstractPasswordService#isAttemptThrottlingEnabled()
   * @see AbstractMfaProcess#setMfaThrottle(AuthThrottle authThrottle)
   */
  public int delayForNextAttempt(final User user) {
    int delay = 0;
    int throttlingNeeded = this.getAuthenticationThrottle(user.getFailedLoginAttempts());
    if (throttlingNeeded != 0) {
      WDate t = user.getLastLoginAttempt();
      int diff = t.getSecondsTo(WDate.getCurrentServerDate());
      if (diff < throttlingNeeded) {
        delay = throttlingNeeded - diff;
      }
    }
    if (delay > 0) {
      logger.warn(
          new StringWriter()
              .append("secure:")
              .append("delayForNextAttempt(): ")
              .append(String.valueOf(delay))
              .append(" seconds for user: ")
              .append(user.getId())
              .toString());
    }
    return delay;
  }
  /**
   * Returns the number of seconds a user needs to wait between two authentication attempts, given
   * the amount of failed attempts since the last successful login.
   *
   * <p>The returned value is in seconds.
   *
   * <p>The default implementation returns the following:
   *
   * <ul>
   *   <li>failedAttempts == 0: 0
   *   <li>failedAttempts == 1: 1
   *   <li>failedAttempts == 2: 5
   *   <li>failedAttempts == 3: 10
   *   <li>failedAttempts &gt; 3: 25
   * </ul>
   */
  public int getAuthenticationThrottle(int failedAttempts) {
    switch (failedAttempts) {
      case 0:
        return 0;
      case 1:
        return 1;
      case 2:
        return 5;
      case 3:
        return 10;
      default:
        return 25;
    }
  }
  /**
   * Prepare a widget for showing client-side updated throttling message.
   *
   * <p>This method loads the necessary JavaScript and initializes a widget in preparation of calls
   * to {@link AuthThrottle#updateThrottlingMessage(WInteractWidget button, int delay)
   * updateThrottlingMessage()}.
   *
   * <p>The widget&apos;s inner HTML will be replaced by a count-down message, defined in <code>
   * <code>Wt.Auth.throttle-retry</code></code>, for the duration of the wait time. In addition, the
   * widget is disabled, so that the user cannot click it. When the count-down reaches 0, The
   * original content of the widget is restored and the widget is re-enabled.
   *
   * <p>This will work with widgets such as buttons or anchors, which are usually used to implement
   * button-style functionality.
   *
   * <p>
   *
   * @see AuthThrottle#updateThrottlingMessage(WInteractWidget button, int delay)
   */
  public void initializeThrottlingMessage(WInteractWidget button) {
    WApplication app = WApplication.getInstance();
    app.loadJavaScript("js/AuthThrottle.js", wtjs1());
    button.setJavaScriptMember(
        " AuthThrottle",
        "new Wt4.11.2.AuthThrottle(Wt4.11.2,"
            + button.getJsRef()
            + ","
            + WString.toWString(WString.tr("Wt.Auth.throttle-retry")).getJsStringLiteral()
            + ");");
  }
  /**
   * Show the count-down message and disable the widget for the given delay.
   *
   * <p>The <code>delay</code> is specified in seconds.
   *
   * <p>This method is called after an attempt to authenticate, to inform the user with a
   * client-side count-down indicator in the widget. This method should disable the widget, and show
   * a count-down text to the user.
   *
   * <p>Each second the counter will be updated, and the number of seconds to wait for will decrease
   * by one, eventually enabling the widget again.
   *
   * <p>You need to call configureThrottling() before you can do this. See the documentation of
   * configureThrottling() for more information on the default behaviour of this method.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Throttling delay must always be verified server-side </i>
   *
   * @see AuthThrottle#delayForNextAttempt(User user)
   */
  public void updateThrottlingMessage(WInteractWidget button, int delay) {
    StringBuilder s = new StringBuilder();
    s.append(button.getJsRef()).append(".wtThrottle.reset(").append(delay).append(");");
    button.doJavaScript(s.toString());
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "AuthThrottle",
        "(function(t,l,e){l.wtThrottle=this;let n=null,i=null,r=0;function s(){clearInterval(n);n=null;t.setHtml(l,i);l.disabled=!1;i=null}function u(){if(0===r)s();else{t.setHtml(l,e.replace(\"{1}\",r));--r}}this.reset=function(t){n&&s();i=l.innerHTML;r=t;if(r){n=setInterval(u,1e3);l.disabled=!0;u()}}})");
  }
}
