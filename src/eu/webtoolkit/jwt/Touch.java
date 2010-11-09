/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import eu.webtoolkit.jwt.servlet.WebRequest;

/**
 * A single finger touch of a touch event.
 * <p>
 * 
 * @see WTouchEvent
 */
public class Touch {
	/**
	 * Constructor.
	 */
	public Touch(int identifier, int clientX, int clientY, int documentX,
			int documentY, int screenX, int screenY, int widgetX, int widgetY) {
		this.clientX_ = clientX;
		this.clientY_ = clientY;
		this.documentX_ = documentX;
		this.documentY_ = documentY;
		this.widgetX_ = widgetX;
		this.widgetY_ = widgetY;
		this.identifier_ = identifier;
	}

	/**
	 * Returns the touch position relative to the document.
	 */
	public Coordinates getDocument() {
		return new Coordinates(this.documentX_, this.documentY_);
	}

	/**
	 * Returns the touch position relative to the window.
	 * <p>
	 * This differs from {@link Touch#getDocument() getDocument()} only when
	 * scrolling through the document.
	 */
	public Coordinates getWindow() {
		return new Coordinates(this.clientX_, this.clientY_);
	}

	/**
	 * Returns the touch position relative to the screen.
	 */
	public Coordinates getScreen() {
		return new Coordinates(this.screenX_, this.screenY_);
	}

	/**
	 * Returns the touch position relative to the widget.
	 */
	public Coordinates getWidget() {
		return new Coordinates(this.widgetX_, this.widgetY_);
	}

	private int clientX_;
	private int clientY_;
	private int documentX_;
	private int documentY_;
	private int screenX_;
	private int screenY_;
	private int widgetX_;
	private int widgetY_;
	private int identifier_;

	static int asInt(String v) {
		return Integer.parseInt(v);
	}

	static int parseIntParameter(WebRequest request, String name, int ifMissing) {
		String p;
		if ((p = request.getParameter(name)) != null) {
			try {
				return asInt(p);
			} catch (NumberFormatException ee) {
				WApplication.getInstance().log("error").append(
						"Could not cast event property '").append(name).append(
						": ").append(p).append("' to int");
				return ifMissing;
			}
		} else {
			return ifMissing;
		}
	}

	static String getStringParameter(WebRequest request, String name) {
		String p;
		if ((p = request.getParameter(name)) != null) {
			return p;
		} else {
			return "";
		}
	}

	static void decodeTouches(String str, List<Touch> result) {
		if (str.length() == 0) {
			return;
		}
		List<String> s = new ArrayList<String>();
		s = new ArrayList<String>(Arrays.asList(str.split(";")));
		if (s.size() % 9 != 0) {
			WApplication.getInstance().log("error").append(
					"Could not parse touches array '").append(str).append("'");
			return;
		}
		try {
			for (int i = 0; i < s.size(); i += 9) {
				result.add(new Touch(asInt(s.get(i + 0)), asInt(s.get(i + 1)),
						asInt(s.get(i + 2)), asInt(s.get(i + 3)), asInt(s
								.get(i + 4)), asInt(s.get(i + 5)), asInt(s
								.get(i + 6)), asInt(s.get(i + 7)), asInt(s
								.get(i + 8))));
			}
		} catch (NumberFormatException ee) {
			WApplication.getInstance().log("error").append(
					"Could not parse touches array '").append(str).append("'");
			return;
		}
	}
}
