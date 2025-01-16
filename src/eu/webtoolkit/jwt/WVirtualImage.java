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

/**
 * An abstract widget that shows a viewport to a virtually large image.
 *
 * <p>WVirtualImage is an abstract class which renders a large image in small pieces. The large
 * image is broken down, and rendered as a grid of smaller square images parts.
 *
 * <p>The WVirtualImage may provide interactive navigation using the mouse, by reacting to dragging
 * of the mouse on the image.
 *
 * <p>The WVirtualImage renders pieces in and bordering the current viewport. In this way, provided
 * the individual pieces load sufficiently fast, the user has effectively the impression of
 * scrolling through a single large image, without glitches. Whenever the image is navigated, if
 * necessary, new images are rendered to maintain the border. Images that are too far from the
 * current viewport are pruned away, so that browser memory remains bounded.
 *
 * <p>To use this class, you must reimplement one of two virtual methods to specify the contents of
 * each grid piece. Either you provide a suitable {@link WImage} for every grid piece, or you
 * provide a {@link WResource} which renders the contents for a {@link WImage} for every grid piece.
 *
 * <p>The total image dimensions are (0, 0) to (imageWidth, imageHeight) for a finite image, and
 * become unbounded (including negative numbers) for each dimension which is Infinite.
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>Styling through CSS is not applicable.
 */
public class WVirtualImage extends WCompositeWidget {
  private static Logger logger = LoggerFactory.getLogger(WVirtualImage.class);

  /** Special value for imageWidth or imageHeight. */
  public static final long Infinite = Long.MAX_VALUE;
  /**
   * Creates a viewport for a virtual image.
   *
   * <p>You must specify the size of the viewport, and the size of the virtual image. The latter
   * dimensions may be the special value Infinite, indicating that in one or more dimensions, the
   * image size is infinite (in practice limited by the maximum integer value).
   *
   * <p>In addition, you must specify the size of each square grid item. The default is 256 by 256.
   */
  public WVirtualImage(
      int viewPortWidth,
      int viewPortHeight,
      long imageWidth,
      long imageHeight,
      int gridImageSize,
      WContainerWidget parentContainer) {
    super();
    this.viewPortChanged_ = new Signal2<Long, Long>();
    this.grid_ = new HashMap<Long, WImage>();
    this.gridImageSize_ = gridImageSize;
    this.viewPortWidth_ = viewPortWidth;
    this.viewPortHeight_ = viewPortHeight;
    this.imageWidth_ = imageWidth;
    this.imageHeight_ = imageHeight;
    this.currentX_ = 0;
    this.currentY_ = 0;
    this.setImplementation(this.impl_ = new WContainerWidget());
    this.impl_.resize(new WLength(this.viewPortWidth_), new WLength(this.viewPortHeight_));
    this.impl_.setPositionScheme(PositionScheme.Relative);
    WContainerWidget scrollArea = new WContainerWidget();
    this.impl_.addWidget(scrollArea);
    scrollArea.resize(
        new WLength(100, LengthUnit.Percentage), new WLength(100, LengthUnit.Percentage));
    scrollArea.setPositionScheme(PositionScheme.Absolute);
    scrollArea.setOverflow(Overflow.Hidden);
    this.contents_ = new WContainerWidget();
    scrollArea.addWidget(this.contents_);
    this.contents_.setPositionScheme(PositionScheme.Absolute);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a viewport for a virtual image.
   *
   * <p>Calls {@link #WVirtualImage(int viewPortWidth, int viewPortHeight, long imageWidth, long
   * imageHeight, int gridImageSize, WContainerWidget parentContainer) this(viewPortWidth,
   * viewPortHeight, imageWidth, imageHeight, 256, (WContainerWidget)null)}
   */
  public WVirtualImage(int viewPortWidth, int viewPortHeight, long imageWidth, long imageHeight) {
    this(viewPortWidth, viewPortHeight, imageWidth, imageHeight, 256, (WContainerWidget) null);
  }
  /**
   * Creates a viewport for a virtual image.
   *
   * <p>Calls {@link #WVirtualImage(int viewPortWidth, int viewPortHeight, long imageWidth, long
   * imageHeight, int gridImageSize, WContainerWidget parentContainer) this(viewPortWidth,
   * viewPortHeight, imageWidth, imageHeight, gridImageSize, (WContainerWidget)null)}
   */
  public WVirtualImage(
      int viewPortWidth, int viewPortHeight, long imageWidth, long imageHeight, int gridImageSize) {
    this(
        viewPortWidth,
        viewPortHeight,
        imageWidth,
        imageHeight,
        gridImageSize,
        (WContainerWidget) null);
  }
  /** Destructor. */
  public void remove() {
    super.remove();
  }
  /**
   * Regenerates and redraws the image pieces.
   *
   * <p>This method invalidates all current grid images, and recreates them.
   */
  public void redrawAll() {
    this.contents_.clear();
    this.grid_.clear();
    this.generateGridItems(this.currentX_, this.currentY_);
  }
  /**
   * Enables mouse dragging to scroll around the image.
   *
   * <p>The cursor is changed to a &apos;move&apos; symbol to indicate the interactivity.
   */
  public void enableDragging() {
    this.impl_
        .mouseWentDown()
        .addListener(
            "function(obj, event) {  var pc = Wt4.11.2.pageCoordinates(event);  obj.setAttribute('dsx', pc.x);  obj.setAttribute('dsy', pc.y);}");
    this.impl_
        .mouseMoved()
        .addListener(
            "function(obj, event) {var WT= Wt4.11.2;var lastx = obj.getAttribute('dsx');var lasty = obj.getAttribute('dsy');if (lastx != null && lastx != '') {var nowxy = WT.pageCoordinates(event);var img = "
                + this.contents_.getJsRef()
                + ";img.style.left = (WT.pxself(img, 'left')+nowxy.x-lastx) + 'px';img.style.top = (WT.pxself(img, 'top')+nowxy.y-lasty) + 'px';obj.setAttribute('dsx', nowxy.x);obj.setAttribute('dsy', nowxy.y);}}");
    this.impl_
        .mouseWentUp()
        .addListener(
            "function(obj, event) {" + this.impl_.getJsRef() + ".removeAttribute('dsx');}");
    this.impl_
        .mouseWentUp()
        .addListener(
            this,
            (WMouseEvent e1) -> {
              WVirtualImage.this.mouseUp(e1);
            });
    this.impl_.getDecorationStyle().setCursor(Cursor.OpenHand);
  }
  /**
   * Scrolls the viewport of the image over a distance.
   *
   * <p>
   *
   * @see WVirtualImage#scrollTo(long newX, long newY)
   */
  public void scroll(long dx, long dy) {
    this.scrollTo(this.currentX_ + dx, this.currentY_ + dy);
  }
  /**
   * Scrolls the viewport of the image to a specific coordinate.
   *
   * <p>Scroll the viewport so that its top left coordinate becomes (x, y).
   *
   * <p>
   *
   * @see WVirtualImage#scroll(long dx, long dy)
   */
  public void scrollTo(long newX, long newY) {
    this.internalScrollTo(newX, newY, true);
  }
  /**
   * Returns the virtual image width.
   *
   * <p>
   *
   * @see WVirtualImage#getImageHeight()
   * @see WVirtualImage#resizeImage(long w, long h)
   */
  public long getImageWidth() {
    return this.imageWidth_;
  }
  /**
   * Returns the virtual image height.
   *
   * <p>
   *
   * @see WVirtualImage#getImageWidth()
   * @see WVirtualImage#resizeImage(long w, long h)
   */
  public long getImageHeight() {
    return this.imageHeight_;
  }
  /**
   * Resizes the virtual image.
   *
   * <p>This sets a new virtual size for the image. The viewport size sets the visible portion of
   * the image.
   *
   * <p>
   *
   * @see WVirtualImage#getImageWidth()
   * @see WVirtualImage#getImageHeight()
   */
  public void resizeImage(long w, long h) {
    this.imageWidth_ = w;
    this.imageHeight_ = h;
    this.redrawAll();
  }
  /**
   * Returns the viewport width.
   *
   * <p>
   *
   * @see WVirtualImage#getViewPortHeight()
   */
  public int getViewPortWidth() {
    return this.viewPortWidth_;
  }
  /**
   * Returns the viewport height.
   *
   * <p>
   *
   * @see WVirtualImage#getViewPortWidth()
   */
  public int getViewPortHeight() {
    return this.viewPortHeight_;
  }
  /**
   * Returns the size of a single piece.
   *
   * <p>This is the size of a side of the square pieces that is used to render the visible part of
   * the image.
   */
  public int getGridImageSize() {
    return this.gridImageSize_;
  }
  /**
   * Returns the current top left X coordinate.
   *
   * <p>
   *
   * @see WVirtualImage#getCurrentTopLeftY()
   */
  public long getCurrentTopLeftX() {
    return this.currentX_;
  }
  /**
   * Returns the current top left Y coordinate.
   *
   * <p>
   *
   * @see WVirtualImage#getCurrentTopLeftX()
   */
  public long getCurrentTopLeftY() {
    return this.currentY_;
  }
  /**
   * Returns the current bottom right X coordinate.
   *
   * <p>
   *
   * @see WVirtualImage#getCurrentBottomRightY()
   */
  public long getCurrentBottomRightX() {
    return this.currentX_ + this.viewPortWidth_;
  }
  /**
   * Returns the current bottom right Y coordinate.
   *
   * <p>
   *
   * @see WVirtualImage#getCurrentBottomRightX()
   */
  public long getCurrentBottomRightY() {
    return this.currentY_ + this.viewPortHeight_;
  }
  /**
   * Signal emitted whenever the viewport changes.
   *
   * <p>The viewport can be changed by the user dragging the image or through the API methods {@link
   * WVirtualImage#scrollTo(long newX, long newY) scrollTo()} and {@link WVirtualImage#scroll(long
   * dx, long dy) scroll()}.
   */
  public Signal2<Long, Long> viewPortChanged() {
    return this.viewPortChanged_;
  }
  /**
   * Creates a grid image for the given rectangle.
   *
   * <p>Create the image which spans image coordinates with left upper corner (x, y) and given width
   * and height.
   *
   * <p>Width and height will not necesarilly equal {@link WVirtualImage#getGridImageSize()
   * getGridImageSize()}, if the the image is not infinite sized.
   *
   * <p>The default implementation calls render() and creates an image for the resource returned.
   *
   * <p>You should override this method if you wish to serve for example static image content.
   *
   * <p>
   */
  protected WImage createImage(long x, long y, int width, int height) {
    WResource r = this.render(x, y, width, height);
    return new WImage(new WLink(r), "", (WContainerWidget) null);
  }
  /**
   * Render a grid image for the given rectangle.
   *
   * <p>Returns a resource that streams an image which renders the rectangle which spans image
   * coordinates with left upper corner (x, y) and given width and height.
   *
   * <p>Width and height will not necesarilly equal to {@link WVirtualImage#getGridImageSize()
   * getGridImageSize()}, if the the image is not infinite sized.
   *
   * <p>The default implementation throws an Exception. You must reimplement this method unless you
   * reimplement {@link WVirtualImage#createImage(long x, long y, int width, int height)
   * createImage()}.
   *
   * <p>
   *
   * @see WVirtualImage#createImage(long x, long y, int width, int height)
   */
  protected WResource render(long x, long y, int width, int height) {
    throw new WException("You should reimplement WVirtualImage::render()");
  }

  private Signal2<Long, Long> viewPortChanged_;
  private WContainerWidget impl_;
  private WContainerWidget contents_;

  static class Rect {
    private static Logger logger = LoggerFactory.getLogger(Rect.class);

    public long x1;
    public long y1;
    public long x2;
    public long y2;

    public Rect(long x1_, long y1_, long x2_, long y2_) {
      this.x1 = x1_;
      this.y1 = y1_;
      this.x2 = x2_;
      this.y2 = y2_;
    }
  }

  private Map<Long, WImage> grid_;
  private int gridImageSize_;
  private int viewPortWidth_;
  private int viewPortHeight_;
  private long imageWidth_;
  private long imageHeight_;
  private long currentX_;
  private long currentY_;

  private void mouseUp(final WMouseEvent e) {
    this.internalScrollTo(
        this.currentX_ - e.getDragDelta().x,
        this.currentY_ - e.getDragDelta().y,
        !WApplication.getInstance().getEnvironment().hasAjax());
  }

  private WVirtualImage.Rect neighbourhood(long x, long y, int marginX, int marginY) {
    long x1 = x - marginX;
    if (this.imageWidth_ != Infinite) {
      x1 = Math.max((long) 0, x1);
    }
    long y1 = Math.max((long) 0, y - marginY);
    long x2 = x + this.viewPortWidth_ + marginX;
    if (this.imageWidth_ != Infinite) {
      x2 = Math.min(this.imageWidth_, x2);
    }
    long y2 = Math.min(this.imageHeight_, y + this.viewPortHeight_ + marginY);
    return new WVirtualImage.Rect(x1, y1, x2, y2);
  }

  private long gridKey(long i, long j) {
    return i * 1000 + j;
  }

  static class Coordinate {
    private static Logger logger = LoggerFactory.getLogger(Coordinate.class);

    public long i;
    public long j;
  }

  private void decodeKey(long key, final WVirtualImage.Coordinate coordinate) {
    coordinate.i = key / 1000;
    coordinate.j = key % 1000;
  }

  private void generateGridItems(long newX, long newY) {
    WVirtualImage.Rect newNb =
        this.neighbourhood(newX, newY, this.viewPortWidth_, this.viewPortHeight_);
    long i1 = newNb.x1 / this.gridImageSize_;
    long j1 = newNb.y1 / this.gridImageSize_;
    long i2 = newNb.x2 / this.gridImageSize_ + 1;
    long j2 = newNb.y2 / this.gridImageSize_ + 1;
    for (int invisible = 0; invisible < 2; ++invisible) {
      for (long i = i1; i < i2; ++i) {
        for (long j = j1; j < j2; ++j) {
          long key = this.gridKey(i, j);
          WImage it = this.grid_.get(key);
          if (it == null) {
            boolean v = this.visible(i, j);
            if (v && !(invisible != 0) || !v && invisible != 0) {
              long brx = i * this.gridImageSize_ + this.gridImageSize_;
              long bry = j * this.gridImageSize_ + this.gridImageSize_;
              brx = Math.min(brx, this.imageWidth_);
              bry = Math.min(bry, this.imageHeight_);
              final int width = (int) (brx - i * this.gridImageSize_);
              final int height = (int) (bry - j * this.gridImageSize_);
              if (width > 0 && height > 0) {
                WImage img =
                    this.createImage(
                        i * this.gridImageSize_, j * this.gridImageSize_, width, height);
                img.mouseWentDown().preventDefaultAction(true);
                img.setPositionScheme(PositionScheme.Absolute);
                img.setOffsets(
                    new WLength((double) i * this.gridImageSize_), EnumSet.of(Side.Left));
                img.setOffsets(new WLength((double) j * this.gridImageSize_), EnumSet.of(Side.Top));
                this.grid_.put(key, img);
                this.contents_.addWidget(img);
              }
            }
          }
        }
      }
    }
    this.currentX_ = newX;
    this.currentY_ = newY;
    this.cleanGrid();
  }

  private void cleanGrid() {
    WVirtualImage.Rect cleanNb =
        this.neighbourhood(
            this.currentX_, this.currentY_, this.viewPortWidth_ * 3, this.viewPortHeight_ * 3);
    long i1 = cleanNb.x1 / this.gridImageSize_;
    long j1 = cleanNb.y1 / this.gridImageSize_;
    long i2 = cleanNb.x2 / this.gridImageSize_ + 1;
    long j2 = cleanNb.y2 / this.gridImageSize_ + 1;
    for (Iterator<Map.Entry<Long, WImage>> it_it = this.grid_.entrySet().iterator();
        it_it.hasNext(); ) {
      Map.Entry<Long, WImage> it = it_it.next();
      WVirtualImage.Coordinate coordinate = new WVirtualImage.Coordinate();
      this.decodeKey(it.getKey(), coordinate);
      if (coordinate.i < i1 || coordinate.i > i2 || coordinate.j < j1 || coordinate.j > j2) {
        {
          WWidget toRemove = it.getValue().removeFromParent();
          if (toRemove != null) toRemove.remove();
        }

        it_it.remove();
      } else {
      }
    }
  }

  private boolean visible(long i, long j) {
    long x1 = i * this.gridImageSize_;
    long y1 = j * this.gridImageSize_;
    long x2 = x1 + this.gridImageSize_;
    long y2 = y1 + this.gridImageSize_;
    return x2 >= this.currentX_
        && y2 >= this.currentY_
        && x1 <= this.currentX_ + this.viewPortWidth_
        && y1 <= this.currentY_ + this.viewPortHeight_;
  }

  private void internalScrollTo(long newX, long newY, boolean moveViewPort) {
    if (this.imageWidth_ != Infinite) {
      newX = clamp(newX, 0, this.imageWidth_ - this.viewPortWidth_);
    }
    if (this.imageHeight_ != Infinite) {
      newY = clamp(newY, 0, this.imageHeight_ - this.viewPortHeight_);
    }
    if (moveViewPort) {
      this.contents_.setOffsets(new WLength((double) -newX), EnumSet.of(Side.Left));
      this.contents_.setOffsets(new WLength((double) -newY), EnumSet.of(Side.Top));
    }
    this.generateGridItems(newX, newY);
    this.viewPortChanged_.trigger(this.currentX_, this.currentY_);
  }

  static long clamp(long v, long min, long max) {
    return Math.max(min, Math.min(v, max));
  }
}
