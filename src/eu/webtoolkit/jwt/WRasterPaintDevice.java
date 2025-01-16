package eu.webtoolkit.jwt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.EnumSet;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.webtoolkit.jwt.WPainterPath.Segment;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;
import eu.webtoolkit.jwt.utils.EnumUtils;

public class WRasterPaintDevice extends WResource implements WPaintDevice {
	private static Logger logger = LoggerFactory.getLogger(WRasterPaintDevice.class);

	private static final int MITER_LIMIT = 10;

	enum Format { PngFormat }

	private Format format;
	private WLength width;
	private WLength height;

	private WPainter painter;
	private EnumSet<PainterChangeFlag> changeFlags;

	private BufferedImage image;
	private Graphics2D g2;
	private Paint penPaint, brushPaint;

	private volatile ByteArrayOutputStream output;

	public WRasterPaintDevice(String format, WLength width, WLength height) {
		this.width = width;
		this.height = height;
		if (format.equals("png"))
		    this.format = Format.PngFormat;
		else
		    throw new RuntimeException("Unsupported format: " + format);
		this.changeFlags = EnumSet.noneOf(PainterChangeFlag.class);
		
		if (width.toPixels() > 0 && height.toPixels() > 0)
			this.image = new BufferedImage((int)width.toPixels(), (int)height.toPixels(), BufferedImage.TYPE_INT_ARGB);
	}

	protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
		response.setContentType("image/png");
		ByteArrayOutputStream out = output;
		if (out != null)
			out.writeTo(response.getOutputStream());
	}

	@Override
	public void done() {
		if (image == null)
			return;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
		  ImageIO.write(image, "png", out);
		  output = out;
		} catch (IOException e) {
			logger.error("Unexpected IOException when writing png to byte buffer", e);
		}
	}

	
	public void drawArc(WRectF rect, double startAngle, double spanAngle) {
		drawShape(new Arc2D.Double(rect.getLeft(), rect.getTop(), rect.getWidth(), rect.getHeight(),
			startAngle, spanAngle, Arc2D.OPEN));
	}

	
	public void drawImage(WRectF rect, String imageUri, int imgWidth, int imgHeight, WRectF sourceRect) {
		processChangeFlags();
		try {
			String realImageUri = imageUri;
			Path p = Paths.get(imageUri);
			if (!DataUri.isDataUri(imageUri) && !p.isAbsolute()) {
      			ServletContext context = WApplication.getInstance().getEnvironment().getServer().getServletContext();
				realImageUri = context.getRealPath(imageUri);
			}
			BufferedImage image = ImageIO.read(new File(realImageUri));
			BufferedImage subImg = image.getSubimage((int)sourceRect.getLeft(), (int)sourceRect.getTop(), (int)sourceRect.getWidth(), (int)sourceRect.getHeight());
			float xScale = (float)(rect.getWidth() / sourceRect.getWidth());
			float yScale = (float)(rect.getHeight() / sourceRect.getHeight());
			AffineTransform t = new AffineTransform(xScale, 0f, 0f, yScale, rect.getLeft(), rect.getTop());
			g2.drawImage(subImg, t, null);
		} catch (IOException e) {
			logger.error("IOException when reading image: " + imageUri, e);
		}
	}

	
	public void drawLine(double x1, double y1, double x2, double y2) {
		drawShape(new Line2D.Double(x1, y1, x2, y2));
	}

	public void drawRect(WRectF rect) {
		drawShape(new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));
	}

	
	public void drawPath(WPainterPath path) {
		drawShape(createShape(path));
	}

	/**
	 * Converts a jwt.WPainterPath to an awt.Shape
	 * 
	 * @param path
	 * @return a shape that represents the path
	 */
	public static Shape createShape(WPainterPath path) {
		GeneralPath p = new GeneralPath();

		for (Iterator<Segment> i = path.getSegments().iterator(); i.hasNext();) {
			Segment s = i.next();
			switch (s.getType()) {
			case MoveTo:
				p.moveTo((float)s.getX(), (float)s.getY());
				break;
			case LineTo:
				p.lineTo((float)s.getX(), (float)s.getY());
				break;
			case QuadC:
				double x1 = s.getX();
				double y1 = s.getY();
				s = i.next();
				p.quadTo((float)x1, (float)y1, (float)s.getX(), (float)s.getY());
				break;
			case CubicC1:
				x1 = s.getX();
				y1 = s.getY();
				s = i.next();
				double x2 = s.getX();
				double y2 = s.getY();
				s = i.next();
				p.curveTo((float)x1, (float)y1, (float)x2, (float)y2, (float)s.getX(), (float)s.getY());
				break;
			case ArcC:
				double cx = s.getX();
				double cy = s.getY();
				s = i.next();
				double rx = s.getX();
				double ry = s.getY();
				s = i.next();
				Arc2D arc = new Arc2D.Double((float)(cx - rx), (float)(cy - ry), (float)(rx * 2), (float)(ry * 2), (float)s.getX(), (float)s.getY(), Arc2D.OPEN);
				p.append(arc, true);
				break;
			default:
				throw new RuntimeException("Unexpected segment type: " + s.getType());
			}
		}
		return p;
	}

	@Override
	public void drawText(WRectF rect, EnumSet<AlignmentFlag> flags, TextFlag textFlag, CharSequence text, WPointF clipPoint) {
		if (textFlag == TextFlag.WordWrap)
			throw new UnsupportedOperationException("drawText(): WordWrap not yet implemented");

		if (clipPoint != null && this.getPainter() != null) {
			if (!this.getPainter().getClipPathTransform().map(this.getPainter().getClipPath())
					.isPointInPath(this.getPainter().getWorldTransform().map(clipPoint))) {
				return;
			}
		}
		
		processChangeFlags();
		
		double px = 0, py = 0;

		AlignmentFlag horizontalAlign = EnumUtils.enumFromSet(EnumUtils.mask(flags, AlignmentFlag.AlignHorizontalMask));
		AlignmentFlag verticalAlign = EnumUtils.enumFromSet(EnumUtils.mask(flags, AlignmentFlag.AlignVerticalMask));

		String s = text.toString();
		
		switch (horizontalAlign) {
		case Left:
			px = rect.getLeft();
			break;
		case Right:
			px = rect.getRight() - g2.getFontMetrics().stringWidth(s);
			break;
		case Center:
			px = rect.getCenter().getX() - g2.getFontMetrics().stringWidth(s)/2;
			break;
		}
		
		switch (verticalAlign) {
		case Bottom:
			py = rect.getBottom();
			break;
		case Top:
			py = rect.getTop() + g2.getFontMetrics().getHeight();
			break;
		case Middle:
			py = rect.getCenter().getY() + g2.getFontMetrics().getHeight()/2;
		}
		
		py -= g2.getFontMetrics().getDescent();

		g2.setPaint(penPaint);
		g2.drawString(s, (float)px, (float)py);
	}

	
	public WLength getHeight() {
		return height;
	}
	

	public WPainter getPainter() {
		return painter;
	}

	
	public WLength getWidth() {
		return width;
	}

	
	public void init() {
		if (image != null) {
			this.g2 = image.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		}
		
		changeFlags.add(PainterChangeFlag.Pen);
		changeFlags.add(PainterChangeFlag.Brush);
		changeFlags.add(PainterChangeFlag.Font);
	}

	
	public boolean isPaintActive() {
		return painter != null;
	}

	
	public void setChanged(EnumSet<PainterChangeFlag> flags) {
		this.changeFlags.addAll(flags);
	}

	
	public void setChanged(PainterChangeFlag flag, PainterChangeFlag... flags) {
		setChanged(EnumSet.of(flag, flags));
	}

	public void setPainter(WPainter painter) {
		this.painter = painter;
	}

	private void drawShape(Shape shape) {
		processChangeFlags();

		if (painter.getPen().getStyle() != PenStyle.None) {
			g2.setPaint(penPaint);
			g2.draw(shape);
		}

		if (painter.getBrush().getStyle() != BrushStyle.None) {
			g2.setPaint(brushPaint);
			g2.fill(shape);
		}
	}

	private void processChangeFlags() {
		boolean resetTransform = changeFlags.contains(PainterChangeFlag.Transform);

		if (changeFlags.contains(PainterChangeFlag.Clipping)) {
			setTransform(painter.getClipPathTransform());
			if (painter.getClipPath().isEmpty())
				g2.setClip(null);
			else
				g2.setClip(createShape(painter.getClipPath()));
			resetTransform = true;
		}

		if (resetTransform)
			setTransform(painter.getCombinedTransform());

		if (changeFlags.contains(PainterChangeFlag.Pen)) {
			g2.setStroke(createStroke(painter, painter.getPen()));
			penPaint = createColor(painter.getPen().getColor());
		}

		if (changeFlags.contains(PainterChangeFlag.Brush))
			brushPaint = createColor(painter.getBrush().getColor());
		
		if (changeFlags.contains(PainterChangeFlag.Font))
			g2.setFont(createFont(painter.getFont()));
		
		if (changeFlags.contains(PainterChangeFlag.Hints)) {
			if (painter.getRenderHints().contains(RenderHint.Antialiasing))
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			else
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		changeFlags.clear();
	}

	/** Converts a jwt.WColor to an awt.Color
	 *
	 * @param color the JWt color
	 * @return the corresponding AWT color
	 */
	public static Color createColor(WColor color) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	/** converts a jwt.WPen to an awt.Stroke
	 * 
	 * @param painter the painter used to take into account transformations for the pen width
	 * @param pen the JWt pen
	 * @return the corresponding AWT Stroke
	 */
	public static Stroke createStroke(WPainter painter, WPen pen) {
		int cap = 0;
		switch (pen.getCapStyle()) {
		case Flat:      cap = BasicStroke.CAP_BUTT; break;
		case Round:     cap = BasicStroke.CAP_ROUND; break;
		case Square:    cap = BasicStroke.CAP_SQUARE; break;
		}

		int join = 0;
		switch (pen.getJoinStyle()) {
		case Bevel:     join = BasicStroke.JOIN_BEVEL; break;
		case Miter:     join = BasicStroke.JOIN_MITER; break;
		case Round:     join = BasicStroke.JOIN_ROUND; break;
		}

		float width = 0;
		if (pen.getStyle() != PenStyle.None)
			width = (float) painter.normalizedPenWidth(pen.getWidth(), true).toPixels();

		switch (pen.getStyle()) {
		case DashLine:
		{
			float[] dash = {4, 2};
			return new BasicStroke(width, cap, join, MITER_LIMIT, dash, 0);
		}
		case DotLine:
		{
			float[] dash = {1, 2};
			return new BasicStroke(width, cap, join, MITER_LIMIT, dash, 0);
		}
		case DashDotLine:
		{
			float[] dash = {4, 2, 1, 2};
			return new BasicStroke(width, cap, join, MITER_LIMIT, dash, 0);
		}
		case DashDotDotLine:
		{
			float[] dash = {4, 2, 1, 2, 1, 2};
			return new BasicStroke(width, cap, join, MITER_LIMIT, dash, 0);
		}
		default:
			return new BasicStroke(width, cap, join);
		}
	}
	
	private Font createFont(WFont font) {
		String name = "";
		
		switch (font.getGenericFamily()) {
		case Default: break;
		case Cursive: break; // ??
		case Monospace: name = "Monospaced"; break;
		case Serif:     name = "Serif";      break;
		case SansSerif: name = "SansSerif"; break;
		}
		
		if (font.getSpecificFamilies().length() != 0) {
			if (name.length() != 0)
				name += " ";
			name += font.getSpecificFamilies().toString();
		}
	
		int style = Font.PLAIN;
		switch (font.getStyle()) {
		case Italic: style = Font.ITALIC;  break;
		case Normal:                       break;
		case Oblique: style = Font.ITALIC; break; // ??
		}
		
		switch (font.getWeight()) {
		case Normal: break;
		case Bolder:
		case Bold: style |= Font.BOLD; break;
		}
	
		int size = (int) (font.getSizeLength(16).toPixels());
		
		return new Font(name, style, size);
	}

	private void setTransform(WTransform t) {
		g2.setTransform(new AffineTransform(t.getM11(), t.getM12(), t.getM21(), t.getM22(), t.getM31(), t.getM32()));
	}
	
	/**
	 * Clears the image (resets the background to solid white).
	 */
	public void clear() {
		if (g2 != null) {
			g2.setBackground(new Color(255, 255, 255, 0));
			g2.clearRect(0, 0, (int)width.getValue(), (int)height.getValue());
		}
	}


	@Override
	public WFontMetrics getFontMetrics() {
		processChangeFlags();
		FontMetrics metrics = g2.getFontMetrics(g2.getFont());
		return new WFontMetrics(painter.getFont(), metrics.getLeading(), metrics.getAscent(), metrics.getDescent());
	}


	@Override
	public WTextItem measureText(CharSequence text, double maxWidth, boolean wordWrap) {	
		if (!wordWrap) {
			processChangeFlags();
			FontMetrics metrics = g2.getFontMetrics(g2.getFont());

			return new WTextItem(text, metrics.stringWidth(text.toString()));
		} else {
			AttributedCharacterIterator paragraph = new AttributedString(text.toString()).getIterator();
			LineBreakMeasurer lbm = new LineBreakMeasurer(paragraph, g2.getFontRenderContext());
			TextLayout layout = lbm.nextLayout((float) maxWidth);
			return new WTextItem(text.subSequence(0, layout.getCharacterCount()), layout.getBounds().getWidth());
		}
	}

	@Override
	public WTextItem measureText(CharSequence text) {
		return measureText(text, -1, false);
	}


	@Override
	public WTextItem measureText(CharSequence text, double maxWidth) {
		return measureText(text, maxWidth, false);
	}

	@Override
	public EnumSet<PaintDeviceFeatureFlag> getFeatures() {
		return EnumSet.of(PaintDeviceFeatureFlag.FontMetrics, PaintDeviceFeatureFlag.WordWrap);
	}

}
