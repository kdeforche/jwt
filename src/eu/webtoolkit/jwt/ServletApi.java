package eu.webtoolkit.jwt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.slf4j.Logger;

import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;


/**
 * Implementation specific servlet interface,
 * this class is only to be used by JWt internals.
 *
 * @author pieter
 */
public abstract class ServletApi {
	protected abstract Logger getLogger();

	public void init(ServletContext context, boolean contextIsInitializing) {
		configureSessionTracking(context, contextIsInitializing);
		configureRequestEncoding(context, contextIsInitializing);
	}

	public void configureSessionTracking(ServletContext context, boolean contextIsInitializing) {
		if (contextIsInitializing) {
			context.setSessionTrackingModes(Set.of(SessionTrackingMode.URL));
			getLogger().info("Configured URL tracking");
		} else {
			final var modes = context.getDefaultSessionTrackingModes();
			final var urlTracking = !modes.contains(SessionTrackingMode.COOKIE) && modes.contains(SessionTrackingMode.URL);

			if (urlTracking)
				getLogger().info("Detected URL tracking: excellent!");
			else
				getLogger().error("Detected cookies configured for session tracking, this will not work well:\n" +
					" - either configure tracking directly in your web.xml:\n" +
					"\n" +
					"  <session-config>\n" +
					"    <tracking-mode>URL</tracking-mode>\n" +
					"  </session-config>\n" +
					"\n" +
					" - OR, configure eu.webtoolkit.jwt.ServletInit as a listener in your web.xml:\n" +
					"\n" +
					"  <listener>\n" +
					"    <listener-class>eu.webtoolkit.jwt.ServletInit</listener-class>\n" +
					"  </listener>\n");
		}
	}

	public abstract void configureRequestEncoding(ServletContext context, boolean contextIsInitializing);

	public HttpServletRequest getMockupHttpServletRequest() {
		return new HttpServletRequest() {
			@Override public boolean authenticate(HttpServletResponse arg0)
					throws IOException, ServletException { return false; }
			@Override public String getAuthType() { return null; }
			@Override public String getContextPath() { return null; }
			@Override public Cookie[] getCookies() { return null; }
			@Override public long getDateHeader(String arg0) { return 0; }
			@Override public String getHeader(String arg0) { return null; }
			@Override public Enumeration<String> getHeaderNames() { return null; }
			@Override public Enumeration<String> getHeaders(String arg0) { return null; }
			@Override public int getIntHeader(String arg0) { return 0; }
			@Override public String getMethod() { return null; }
			@Override public Part getPart(String arg0) throws IOException, ServletException { return null; }
			@Override public Collection<Part> getParts() throws IOException, ServletException { return null; }
			@Override public String getPathInfo() { return null; }
			@Override public String getPathTranslated() { return null; }
			@Override public String getQueryString() { return null; }
			@Override public String getRemoteUser() { return null; }
			@Override public String getRequestURI() { return null; }
			@Override public StringBuffer getRequestURL() { return null; }
			@Override public String getRequestedSessionId() { return null; }
			@Override public String getServletPath() { return null; }
			@Override public HttpSession getSession() { return null; }
			@Override public HttpSession getSession(boolean arg0) { return null; }
			@Override public Principal getUserPrincipal() { return null; }
			@Override public boolean isRequestedSessionIdFromCookie() { return false; }
			@Override public boolean isRequestedSessionIdFromURL() { return false; }
			@Override public boolean isRequestedSessionIdFromUrl() { return false; }
			@Override public boolean isRequestedSessionIdValid() { return false; }
			@Override public boolean isUserInRole(String arg0) { return false; }
			@Override public void login(String arg0, String arg1) throws ServletException {}
			@Override public void logout() throws ServletException {}
			@Override public AsyncContext getAsyncContext() { return null; }
			@Override public Object getAttribute(String arg0) { return null; }
			@Override public Enumeration<String> getAttributeNames() { return null; }
			@Override public String getCharacterEncoding() { return null; }
			@Override public int getContentLength() { return 0; }
			@Override public String getContentType() { return null; }
			@Override public DispatcherType getDispatcherType() { return null; }
			@Override public ServletInputStream getInputStream() throws IOException { return null; }
			@Override public String getLocalAddr() { return null; }
			@Override public String getLocalName() { return null; }
			@Override public int getLocalPort() { return 0; }
			@Override public Locale getLocale() { return null; }
			@Override public Enumeration<Locale> getLocales() { return null; }
			@Override public String getParameter(String arg0) { return null; }
			@Override public Map<String, String[]> getParameterMap() { return null; }
			@Override public Enumeration<String> getParameterNames() { return null; }
			@Override public String[] getParameterValues(String arg0) { return null; }
			@Override public String getProtocol() { return null; }
			@Override public BufferedReader getReader() throws IOException { return null; }
			@Override public String getRealPath(String arg0) { return null; }
			@Override public String getRemoteAddr() { return null; }
			@Override public String getRemoteHost() { return null; }
			@Override public int getRemotePort() { return 0; }
			@Override public RequestDispatcher getRequestDispatcher(String arg0) { return null; }
			@Override public String getScheme() { return null; }
			@Override public String getServerName() { return null; }
			@Override public int getServerPort() { return 0; }
			@Override public ServletContext getServletContext() { return null; }
			@Override public boolean isAsyncStarted() { return false; }
			@Override public boolean isAsyncSupported() { return false; }
			@Override public boolean isSecure() { return false; }
			@Override public void removeAttribute(String arg0) {}
			@Override public void setAttribute(String arg0, Object arg1) {}
			@Override public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {}
			@Override public AsyncContext startAsync() throws IllegalStateException { return null; }
			@Override public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1)
					throws IllegalStateException { return null; }
			@Override public long getContentLengthLong() { return 0; }
			@Override public String changeSessionId() { return null; }
			@Override public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0)
					throws IOException, ServletException { return null; }
		};
	}

	public HttpServletResponse getMockupHttpServletResponse() {
		return new HttpServletResponse() {
			@Override public void addCookie(Cookie arg0) {}
			@Override public void addDateHeader(String arg0, long arg1) {}
			@Override public void addHeader(String arg0, String arg1) {}
			@Override public void addIntHeader(String arg0, int arg1) {}
			@Override public boolean containsHeader(String arg0) { return false; }
			@Override public String encodeRedirectURL(String arg0) { return null; }
			@Override public String encodeRedirectUrl(String arg0) { return null; }
			@Override public String encodeURL(String arg0) { return null; }
			@Override public String encodeUrl(String arg0) { return null; }
			@Override public String getHeader(String arg0) { return null; }
			@Override public Collection<String> getHeaderNames() { return null; }
			@Override public Collection<String> getHeaders(String arg0) { return null; }
			@Override public int getStatus() { return 0; }
			@Override public void sendError(int arg0) throws IOException {}
			@Override public void sendError(int arg0, String arg1) throws IOException {}
			@Override public void sendRedirect(String arg0) throws IOException {}
			@Override public void setDateHeader(String arg0, long arg1) {}
			@Override public void setHeader(String arg0, String arg1) {}
			@Override public void setIntHeader(String arg0, int arg1) {}
			@Override public void setStatus(int arg0) {}
			@Override public void setStatus(int arg0, String arg1) {}
			@Override public void flushBuffer() throws IOException {}
			@Override public int getBufferSize() { return 0; }
			@Override public String getCharacterEncoding() { return null; }
			@Override public String getContentType() { return null; }
			@Override public Locale getLocale() { return null; }
			@Override public ServletOutputStream getOutputStream() throws IOException { return null; }
			@Override public PrintWriter getWriter() throws IOException { return null; }
			@Override public boolean isCommitted() { return false; }
			@Override public void reset() {}
			@Override public void resetBuffer() {}
			@Override public void setBufferSize(int arg0) {}
			@Override public void setCharacterEncoding(String arg0) {}
			@Override public void setContentLength(int arg0) {}
			@Override public void setContentType(String arg0) {}
			@Override public void setLocale(Locale arg0) {}
			@Override public void setContentLengthLong(long arg0) {}
		};
	}

	public void completeAsyncContext(HttpServletRequest request) {
		if (request.isAsyncStarted()) {
			try {
				request.getAsyncContext().complete();
			} catch (IllegalStateException e) {
				getLogger().error("IllegalStateException occurred when completing async context: {}", e.getMessage(), e);
			}
		}
	}

	protected void handleRequest(final WtServlet servlet, final WebRequest request, final WebResponse response) {
		servlet.doHandleRequest(request, response);
	}

	public void doHandleRequest(final WtServlet servlet, final WebRequest request, final WebResponse response) {
		if (servlet.getConfiguration().isUseScriptNonce()) {
			try {
				// Use reflection to bypass accessibility constraint because WebResponse
				// is in another package and addNonce should not be public.
				java.lang.reflect.Method addNonce = response.getClass().getDeclaredMethod("addNonce");
				addNonce.setAccessible(true);
				addNonce.invoke(response);
			} catch (NoSuchMethodException e) {
				// should never happen
				getLogger().error("NoSuchMethodException occurred when adding nonce header: {}", e.getMessage(), e);
			} catch (IllegalAccessException e) {
				// should never happen
				getLogger().error("IllegalAccessException occurred when adding nonce header: {}", e.getMessage(), e);
			} catch (Exception e) {
				getLogger().error("Exception occurred when adding nonce header: {}", e.getMessage(), e);
			}
		}
		if (request.isAsyncSupported()) {
			request.startAsync();
			final long asyncContextTimeout = servlet.getConfiguration().getAsyncContextTimeout();
			request.getAsyncContext().setTimeout(asyncContextTimeout);

			final AtomicBoolean handleRequestFinished = new AtomicBoolean(false);
			class MutableWrapper<T> {T t = null;}
			final MutableWrapper<Thread> threadWrapper = new MutableWrapper<Thread>();

			request.getAsyncContext().addListener(new AsyncListener() {
				@Override
				public void onTimeout(AsyncEvent e) throws IOException {
					if (handleRequestFinished.get())
						return;
					String newLine = "\n\t";

					StringBuilder params = new StringBuilder("Parameters: " + newLine);
					for (Map.Entry<String, String[]> param: request.getParameterMap().entrySet())
						params.append(param.getKey()).append(": ")
							  .append(Arrays.toString(param.getValue())).append(newLine);

					int uploadedFilesCount = 0;
					if (request.getUploadedFiles() != null)
						uploadedFilesCount = request.getUploadedFiles().size();

					StringBuilder msg = new StringBuilder();
					msg.append("Timeout: waiting more then ").append(asyncContextTimeout).append(newLine)
					.append("url: ").append(request.getRequestURL()).append(newLine)
					.append("Uploaded files count: ").append(uploadedFilesCount).append(newLine)
					.append(params);

					if (threadWrapper.t != null)
						msg.append("AsyncContext thread stack trace: ").append(newLine)
						.append(Arrays.toString(threadWrapper.t.getStackTrace()).replaceAll(", ", newLine));

					getLogger().error(msg.toString(), e.getThrowable());
				}

				@Override
				public void onStartAsync(AsyncEvent arg0) throws IOException {
				}

				@Override
				public void onError(AsyncEvent e) throws IOException {
					getLogger().error("Error during async request ", e.getThrowable());
				}

				@Override
				public void onComplete(AsyncEvent arg0) throws IOException {
				}
			});
			request.getAsyncContext().start(new Runnable() {
				@Override
				public void run() {
					threadWrapper.t = Thread.currentThread();
					handleRequest(servlet, request, response);
					handleRequestFinished.set(true);
				}
			});
		} else
			handleRequest(servlet, request, response);
	}
}
