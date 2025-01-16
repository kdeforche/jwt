/*
 * Copyright (C) 2009 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import eu.webtoolkit.jwt.Utils;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WtServlet;
import eu.webtoolkit.jwt.servlet.WebRequest.ResponseType;
import eu.webtoolkit.jwt.utils.StreamUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A WebResponse which wraps the HttpServletResponse to support testing.
 * <p>
 * WebResponse is used instead of HttpServletRequest inside JWt's request handling,
 * and also in {@link WResource#handleRequest(WebRequest request, WebResponse response)}.
 * <p>
 * It augments the functionality of HttpServletResponse by having a constructor which
 * serializes the response to an arbitrary output stream, for testing purposes.
 * <p>
 * @see WebResponse
 */
public class WebResponse extends HttpServletResponseWrapper {
	private static Logger logger = LoggerFactory.getLogger(WebResponse.class);

	private OutputStreamWriter outWriter;
	private HttpServletRequest request;
	private int id;
	private ServletOutputStream outputStream;
	private ResponseType responseType;
	private String nonce = new String("");

	/**
	 * Constructor which wraps a HttpServletResponse.
	 * <p>
	 * It also saves the corresponding request. This is for convenience, when wanting
	 * to change the rendering based on request information.
	 * 
	 * @param response The HttpSerlvetResponse
	 * @param request The HttpServletRequest
	 */
	public WebResponse(HttpServletResponse response, HttpServletRequest request) {
		super(response);

		this.request = request;

		try {
			this.outWriter = new OutputStreamWriter(getOutputStream(), "UTF-8");
		} catch (IOException e) {
			logger.info("IOException in webresponse", e);
		}
	}

	/**
	 * Construct which uses a custom output stream.
	 * <p>
	 * This constructor is useful for testing purposes, for simulating a browser
	 * request and sending the output to e.g. a file.
	 * 
	 * @param out The custom output stream.
	 */
	public WebResponse(final OutputStream out) {
		super(WtServlet.getServletApi().getMockupHttpServletResponse());

		this.outputStream = new StreamUtils.ErrorSuppressingOutputStream(new ServletOutputStream() {
			@Override
			public void write(int arg0) throws IOException {
				out.write(arg0);
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener arg0) {
			}
		}, logger);

		try {
			outWriter = new OutputStreamWriter(outputStream, "UTF-8");
		} catch (IOException e) {
			logger.info("IOException in webresponse", e);
		}
	}
	
	/**
	 * Create a response with no real ServletOutputStream. Used to set up a web socket response
	 */
	public WebResponse() {
		super(WtServlet.getServletApi().getMockupHttpServletResponse());
	}

	/**
	 * Returns the output stream.
	 * <p>
	 * Returns {@link HttpServletResponseWrapper#getOutputStream()} or the custom
	 * output stream passed to {@link #WebResponse(OutputStream)}.
	 * <p>
	 * You should only use the output stream to transmit binary information. Use
	 * {@link #getWriter()} for text output.
	 */
	@Override
	public ServletOutputStream getOutputStream() {
		try {
			if (outputStream == null)
				outputStream = new StreamUtils.ErrorSuppressingOutputStream(super.getOutputStream(), logger);
			return outputStream;
		} catch (IOException e) {
			logger.error("Failed to retrieve ServletOutputStream: {}", e.getMessage());
			return new StreamUtils.ErrorSuppressingOutputStream();
		}
	}

	/**
	 * Returns a text writer.
	 * <p>
	 * This returns a writer set on the output stream, which encodes text in UTF-8
	 * format.
	 * 
	 * @return a writer for streaming text.
	 */
	public Writer out() {
		return this.outWriter;
	}

	/**
	 * Sets an ID to the WebResponse (used by JWt).
	 * 
	 * @param i
	 */
	public void setId(int i) {
		id = i;
	}

	/**
	 * Returns the ID.
	 * <p>
	 * Returns the ID previously set using {@link #setId(int)}
	 * 
	 * @return the Id.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Flushes the response.
	 * <p>
	 * This flushes the writer.
	 */
	public void flush() {
		try {
			outWriter.flush();
			getOutputStream().flush();
		} catch (IOException e) {
			logger.info("IOException in flush", e);
		} catch (Exception e) {
			logger.info("Exception in flush", e);
		} finally {
			if (request != null) {
				WtServlet.getServletApi().completeAsyncContext(request);
			}
		}
	}

	/**
	 * Returns the request path information.
	 * <p>
	 * This returns the path information that was passed in the request.
	 * 
	 * @return the request path information.
	 */
	public String getPathInfo() {
		String result = request.getPathInfo();

        // Jetty will report "/" as an internal path. Which totally makes no sense but is according
        // to the spec
        if (request.getServletPath().length() == 0)
        	if (result != null && result.equals("/"))
        		return "";

		return result == null ? "" : result;
	}

	/**
	 * Returns a request parameter value.
	 * @param string the parameter name
	 * @return the request parameter value, or the empty string if the parameter was not set.
	 */
	public String getParameter(String string) {
		String result = request.getParameter(string);
		return result == null ? "" : result;
	}

	/**
	 * Returns the request method.
	 * 
	 * @return the request method.
	 */
	public String getRequestMethod() {
		return request.getMethod();
	}

	/**
	 * Returns the request's parameter map.
	 * 
	 * @return the request's parameter map
	 */
	public Map<String, String[]> getParameterMap() {
		return ((WebRequest)request).getParameterMap();
	}

	/**
	 * Returns whether this request is a WebSocket request.
	 * 
	 * This is an internal JWt method.
	 */
	public boolean isWebSocketRequest() {
		return false;
	}

	/**
	 * Returns whether this request is a WebSocket message.
	 * 
	 * This is an internal JWt method.
	 */
	public boolean isWebSocketMessage() {
		return false;
	}

	/**
	 * Returns whether another WebSocket message is pending.
	 * 
	 * This is an internal JWt method.
	 */
	public boolean isWebSocketMessagePending() {
		return false;
	}
	
	/**
	 * Sets the response type.
	 * 
	 * This is an internal JWt method.
	 */
	public void setResponseType(ResponseType responseType) {
		this.responseType = responseType;
	}
	
	/**
	 * Returns the response type.
	 * 
	 * This is an internal JWt method.
	 */
	public ResponseType getResponseType() { 
		return this.responseType; 
	}
	
	/**
	 * Returns the nonce in the header.
	 * 
	 * @return the nonce that will be used in the header of the response if useScriptNonce is true, otherwise returns an empty String
	 */
	public String getNonce() {
		return this.nonce;
	}

	void addNonce() {
		this.nonce = generateNonce();
		addHeader("Content-Security-Policy", "script-src 'nonce-"+this.nonce+"' 'strict-dynamic' 'unsafe-eval'");
	}

	private static String generateNonce() {
		Random r = new SecureRandom();
		byte[] salt = new byte[16];
		r.nextBytes(salt);
		return Utils.base64Encode(new String(salt));
	}
}
