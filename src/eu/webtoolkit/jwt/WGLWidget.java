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
 * GL support class.
 *
 * <p>The {@link WGLWidget} class is an interface to the HTML5 WebGL infrastructure for client-side
 * rendering, and OpenGL for server-side rendering. Its API is based on the WebGL API. To fully
 * understand WebGL, it is recommended to read the WebGL standard in addition to this documentation.
 *
 * <p>The most recent version of the WebGL specification can be found here: <a
 * href="http://www.khronos.org/registry/webgl/specs/latest/1.0/">http://www.khronos.org/registry/webgl/specs/latest/1.0/</a>
 *
 * <p>The goal of the {@link WGLWidget} class is to provide a method to render 3D structures in the
 * browser, where rendering and rerendering is normally done at the client side in JavaScript
 * without interaction from the server, in order to obtain a smooth user interaction. Unless the
 * scene requires server-side updates, there is no communication with the server.
 *
 * <p>The rendering interface resembles to OpenGL ES, the same standard as WebGL is based on. This
 * is a stripped down version of the normal OpenGL as we usually find them on desktops. Many
 * stateful OpenGL features are not present in OpenGL ES: no modelview and camera transformation
 * stacks, no default lighting models, no support for other rendering methods than through VBOs, ...
 * Therefore much existing example code for OpenGL applications and shaders will not work on WebGL
 * without modifications. The &apos;learning webgl&apos; web site at <a
 * href="http://learningwebgl.com/">http://learningwebgl.com/</a> is a good starting point to get
 * familiar with WebGL.
 *
 * <p>To use a WGLWidget, you must derive from it and reimplement the painter methods. Usually, you
 * will always need to implement {@link WGLWidget#initializeGL() initializeGL()} and {@link
 * WGLWidget#paintGL() paintGL()}. Optionally, you may choose to implement {@link
 * WGLWidget#resizeGL(int width, int height) resizeGL()} (if your widget does not have a fixed
 * size), and {@link WGLWidget#updateGL() updateGL()}. If you need to modify the painting methods, a
 * repaint is triggered by calling the {@link WGLWidget#repaintGL(EnumSet which) repaintGL()}
 * method. The default behaviour for any of these four painting functions is to do nothing.
 *
 * <p>The four painter methods ({@link WGLWidget#initializeGL() initializeGL()}, {@link
 * WGLWidget#resizeGL(int width, int height) resizeGL()}, {@link WGLWidget#paintGL() paintGL()} and
 * {@link WGLWidget#updateGL() updateGL()}) all record JavaScript which is sent to the browser. The
 * JavaScript code of {@link WGLWidget#paintGL() paintGL()} is cached client-side, and may be
 * executed many times, e.g. to repaint a scene from different viewpoints. The JavaScript code of
 * {@link WGLWidget#initializeGL() initializeGL()}, {@link WGLWidget#resizeGL(int width, int height)
 * resizeGL()} and {@link WGLWidget#updateGL() updateGL()} are intended for OpenGL state updates,
 * and is therefore only executed once on the client and is then discarded.
 *
 * <p>There are four painting methods that you may implement in a specialization of this class. The
 * purpose of these functions is to register what JavaScript code has to be executed to render a
 * scene. Through invocations of the WebGL functions documented below, JWt records the JavaScript
 * calls that have to be invoked in the browser.
 *
 * <ul>
 *   <li><b>{@link WGLWidget#initializeGL() initializeGL()}</b>: this function is executed after the
 *       GL context has been initialized. It is also executed when the <code>webglcontextrestored
 *       </code> signal is fired. You can distinguish between the first initialization and
 *       restoration of context using {@link WGLWidget#isRestoringContext() isRestoringContext()}.
 *       This is the ideal location to compose shader programs, send VBO&apos;s to the client,
 *       extract uniform and attribute locations, ... Due to the presence of VBO&apos;s, this
 *       function may generate a large amount of data to the client.
 *   <li><b>{@link WGLWidget#resizeGL(int width, int height) resizeGL()}</b>: this function is
 *       executed whenever the canvas dimensions change. A change in canvas size will require you to
 *       invoke the {@link WGLWidget#viewport(int x, int y, int width, int height) viewport()}
 *       function again, as well as recalculate the projection matrices (especially when the aspect
 *       ratio has changed). The {@link WGLWidget#resizeGL(int width, int height) resizeGL()}
 *       function is therefore the ideal location to set those properties. The {@link
 *       WGLWidget#resizeGL(int width, int height) resizeGL()} function is invoked automatically on
 *       every resize, and after the first {@link WGLWidget#initializeGL() initializeGL()}
 *       invocation. Additional invocations may be triggered by calling repaint() with the RESIZE_GL
 *       flag.
 *   <li><b>{@link WGLWidget#paintGL() paintGL()}</b>: this is the main scene drawing function.
 *       Through its execution, JWt records what has to be done to render a scene, and it is
 *       executed every time that the scene is to be redrawn. You can use the VBO&apos;s and shaders
 *       prepared in the {@link WGLWidget#initializeGL() initializeGL()} phase. Usually, this
 *       function sets uniforms and attributes, links attributes to VBO&apos;s, applies textures,
 *       and draws primitives. You may also create local programs, buffers, ... Remember that this
 *       function is executed a lot of times, so every buffer/program created in this function
 *       should also be destroyed to avoid memory leaks. This function is transmitted once to the
 *       client, and is executed when the scene needs to be redrawn. Redraws may be triggered from
 *       mouse events, timer triggers, events on e.g. a video element, or whatever other event. The
 *       {@link WGLWidget#paintGL() paintGL()} function can be updated through invoking {@link
 *       WGLWidget#repaintGL(EnumSet which) repaintGL()} with the PAINT_GL flag.
 *   <li><b>{@link WGLWidget#updateGL() updateGL()}</b>: VBO&apos;s, programs, uniforms, GL
 *       properties, or anything else set during intializeGL() are not necessarily immutable. If you
 *       want to change, add, remove or reconfigure those properties, the execution of an {@link
 *       WGLWidget#updateGL() updateGL()} function can be triggered by invoking {@link
 *       WGLWidget#repaintGL(EnumSet which) repaintGL()} with the UPDATE_GL flag. This signals that
 *       {@link WGLWidget#updateGL() updateGL()} needs to be evaluated - just once. It is possible
 *       that the {@link WGLWidget#paintGL() paintGL()} function also requires updates as
 *       consequence of the changes in the {@link WGLWidget#updateGL() updateGL()} function; in this
 *       case, you should also set the PAINT_GL flag of {@link WGLWidget#repaintGL(EnumSet which)
 *       repaintGL()}.
 * </ul>
 *
 * <p>The GL functions are intended to be used exclusively from within the invocation of the four
 * callback functions mentioned above. In order to manually trigger the execution of these function,
 * use the {@link WGLWidget#repaintGL(EnumSet which) repaintGL()}.
 *
 * <p>A {@link WGLWidget} must be given a size explicitly, or must be put inside a layout manager
 * that manages its width and height. The behaviour of a {@link WGLWidget} that was not given a size
 * is undefined.
 *
 * <p>
 *
 * <h3>Binary buffer transfers</h3>
 *
 * <p>In {@link WGLWidget#bufferDatafv(WGLWidget.GLenum target, java.nio.ByteBuffer buffer,
 * WGLWidget.GLenum usage, boolean binary) bufferDatafv()}, there is an additional boolean argument
 * where you can indicate that you want the data to be transferred to the client in binary form. A
 * {@link WMemoryResource} is created for each of these buffers. If you know all previous resources
 * are not required in the client anymore, you can free memory with the method {@link
 * WGLWidget#clearBinaryResources() clearBinaryResources()} (the memory is also managed, so this is
 * not neccesary). If you want to manage these resources entirely by yourself, the following method
 * can be used.
 *
 * <p>Using createAndLoadArrayBuffer(), you can load an array buffer in binary format from an URL.
 * This will cause the client to fetch the given URL, and make the contents of the file available in
 * an {@link ArrayBuffer}, which can then be used by BufferData() to bind them to an OpenGL buffer.
 * This is ideal to load VBO buffers in a faster way, as it avoids converting floats to text strings
 * on the server and then back to floats on the client. You can combine this with the use of {@link
 * WResource} (e.g. {@link WMemoryResource}) to send an std::vector of vertices to the client. Note
 * that using {@link ArrayBuffer} is not possible when you want a fall-back in the form of
 * server-side rendering.
 *
 * <p>
 *
 * <h3>Client side matrices and vectors.</h3>
 *
 * <p>The {@link WGLWidget} provides the {@link JavaScriptMatrix4x4} class as a mechanism to use
 * client-side modifiable matrices in the render functions. These matrices can be used identically
 * to the &apos;constant&apos;, with the advantage that there is no need to have a roundtrip to the
 * server to redraw the scene when they are changed. As such, they are ideal for mouse-based camera
 * manipulations, timer triggered animations, or object manipulations.
 *
 * <p>There&apos;s also support for client-side modifiable vectors, with {@link JavaScriptVector}.
 */
public class WGLWidget extends WInteractWidget {
  private static Logger logger = LoggerFactory.getLogger(WGLWidget.class);

  /** Abstract base class for all GL objects. */
  public abstract static class GlObject {
    private static Logger logger = LoggerFactory.getLogger(GlObject.class);

    public GlObject() {
      this.id_ = -1;
    }

    public GlObject(int id) {
      this.id_ = id;
    }

    public abstract String getJsRef();

    public int getId() {
      return this.id_;
    }

    public void clear() {
      this.id_ = -1;
    }

    public boolean isNull() {
      return this.id_ == -1;
    }

    private int id_;
  }
  /** Reference to a WebGLShader class. */
  public static class Shader extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(Shader.class);

    public Shader() {
      super();
    }

    public Shader(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        throw new WException("Shader: is null");
      }
      return "ctx.WtShader" + String.valueOf(this.getId());
    }
  }
  /** Reference to a WebGLProgram class. */
  public static class Program extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(Program.class);

    public Program() {
      super();
    }

    public Program(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        throw new WException("Program: is null");
      }
      return "ctx.WtProgram" + String.valueOf(this.getId());
    }
  }
  /** Reference to a shader attribute location. */
  public static class AttribLocation extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(AttribLocation.class);

    public AttribLocation() {
      super();
    }

    public AttribLocation(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        throw new WException("AttribLocation: is null");
      }
      return "ctx.WtAttrib" + String.valueOf(this.getId());
    }
  }
  /** Reference to a WebGLBuffer class. */
  public static class Buffer extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(Buffer.class);

    public Buffer() {
      super();
    }

    public Buffer(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        throw new WException("Buffer: is null");
      }
      return "ctx.WtBuffer" + String.valueOf(this.getId());
    }
  }
  /** Reference to a WebGLUniformLocation class. */
  public static class UniformLocation extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(UniformLocation.class);

    public UniformLocation() {
      super();
    }

    public UniformLocation(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        throw new WException("UniformLocation: is null");
      }
      return "ctx.WtUniform" + String.valueOf(this.getId());
    }
  }
  /** Reference to a WebGLTexture class. */
  public static class Texture extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(Texture.class);

    public Texture() {
      super();
      this.url_ = "";
    }

    public Texture(int i) {
      super(i);
      this.url_ = "";
    }

    public String getJsRef() {
      if (this.isNull()) {
        return "null";
      }
      return "ctx.WtTexture" + String.valueOf(this.getId());
    }

    public void setUrl(String url) {
      this.url_ = url;
    }

    public String getUrl() {
      return this.url_;
    }

    private String url_;
  }
  /** Reference to a WebGLFramebuffer class. */
  public static class Framebuffer extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(Framebuffer.class);

    public Framebuffer() {
      super();
    }

    public Framebuffer(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        return "null";
      }
      return "ctx.WtFramebuffer" + String.valueOf(this.getId());
    }
  }
  /** Reference to a WebGLRenderbuffer class. */
  public static class Renderbuffer extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(Renderbuffer.class);

    public Renderbuffer() {
      super();
    }

    public Renderbuffer(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        return "null";
      }
      return "ctx.WtRenderbuffer" + String.valueOf(this.getId());
    }
  }
  /** Reference to a javascript ArrayBuffer class. */
  public static class ArrayBuffer extends WGLWidget.GlObject {
    private static Logger logger = LoggerFactory.getLogger(ArrayBuffer.class);

    public ArrayBuffer() {
      super();
    }

    public ArrayBuffer(int i) {
      super(i);
    }

    public String getJsRef() {
      if (this.isNull()) {
        throw new WException("ArrayBuffer: is null");
      }
      return "ctx.WtBufferResource" + String.valueOf(this.getId());
    }
  }
  /**
   * A client-side JavaScript vector.
   *
   * <p>Using a {@link JavaScriptVector}, GL parameters can be modified without communication with
   * the server. The value of the {@link JavaScriptMatrix4x4} is updated server-side whenever an
   * event is sent to the server.
   *
   * <p>The {@link JavaScriptVector} is represented in JavaScript as an array, either as a
   * Float32Array or as a plain JavaScript array.
   */
  public static class JavaScriptVector {
    private static Logger logger = LoggerFactory.getLogger(JavaScriptVector.class);

    /**
     * Create a temporarily invalid {@link JavaScriptVector}.
     *
     * <p>Should be added to a WGLWidget with {@link
     * WGLWidget#addJavaScriptVector(WGLWidget.JavaScriptVector vec)
     * WGLWidget#addJavaScriptVector()}, and initialized with {@link
     * WGLWidget#initJavaScriptVector(WGLWidget.JavaScriptVector vec)
     * WGLWidget#initJavaScriptVector()}.
     */
    public JavaScriptVector(int length) {
      this.id_ = -1;
      this.length_ = length;
      this.jsRef_ = "";
      this.context_ = null;
      this.initialized_ = false;
    }

    public int getId() {
      return this.id_;
    }
    /** Returns whether this JavaScriptVector has been initialized. */
    public boolean isInitialized() {
      return this.initialized_;
    }
    /** Returns whether this JavaScriptVector has been assigned to a {@link WGLWidget}. */
    public boolean hasContext() {
      return this.context_ != null;
    }
    /** Returns the length (number of items) of this {@link JavaScriptVector}. */
    public int getLength() {
      return this.length_;
    }
    /**
     * Returns the JavaScript reference to this {@link JavaScriptVector}.
     *
     * <p>In order to get a valid JavaScript reference, this vector should have been added to a
     * {@link WGLWidget}.
     */
    public String getJsRef() {
      if (!this.hasContext()) {
        throw new WException("JavaScriptVector: does not belong to a WGLWidget yet");
      }
      return this.jsRef_;
    }
    /**
     * Returns the current server-side value.
     *
     * <p>Client-side changes to the {@link JavaScriptVector} are automatically synchronized.
     */
    public List<Float> getValue() {
      if (!this.hasContext()) {
        throw new WException("JavaScriptVector: vector not assigned to a WGLWidget");
      }
      for (int i = 0; i < this.context_.jsVectorList_.size(); i++) {
        if (this.context_.jsVectorList_.get(i).id == this.id_) {
          return this.context_.jsVectorList_.get(i).serverSideCopy;
        }
      }
      List<Float> result = new ArrayList<Float>();
      for (int i = 0; i < this.getLength(); ++i) {
        result.add(0.0f);
      }
      return result;
    }
    // public WGLWidget.JavaScriptVector clone() ;
    private void assignToContext(int id, WGLWidget context) {
      this.id_ = id;
      this.jsRef_ = context.getGlObjJsRef() + ".jsValues[" + String.valueOf(this.id_) + "]";
      this.context_ = context;
    }

    void initialize() {
      this.initialized_ = true;
    }

    private int id_;
    private int length_;
    private String jsRef_;
    WGLWidget context_;
    private boolean initialized_;
  }
  /**
   * A client-side JavaScript matrix.
   *
   * <p>A JavaScriptMatrix has methods that make it possible to do client-side calculations on
   * matrices.
   *
   * <p>Using a {@link JavaScriptMatrix4x4}, GL parameters can be modified without communication
   * with the server. The value of the {@link JavaScriptMatrix4x4} is updated server-side whenever
   * an event is sent to the server.
   *
   * <p>Important: only the {@link WGLWidget.JavaScriptMatrix4x4#getJsRef() getJsRef()} of the
   * return value from a call to WGLWidget::createJavaScriptMatrix() is a variable name that can be
   * used in custom JavaScript to modify a matrix from external scripts. The {@link
   * WGLWidget.JavaScriptMatrix4x4#getJsRef() getJsRef()} of return values of operations refer to
   * unnamed temporary objects - rvalues in C++-lingo.
   *
   * <p>The {@link JavaScriptMatrix4x4} is represented in JavaScript as an array of 16 elements.
   * This array represents the values of the matrix in column-major order. It is either a
   * Float32Array or a plain JavaScript array.
   */
  public static class JavaScriptMatrix4x4 {
    private static Logger logger = LoggerFactory.getLogger(JavaScriptMatrix4x4.class);

    /**
     * Creates a temporarily invalid {@link JavaScriptMatrix4x4}.
     *
     * <p>Should be added to a WGLWidget with {@link
     * WGLWidget#addJavaScriptMatrix4(WGLWidget.JavaScriptMatrix4x4 mat)
     * WGLWidget#addJavaScriptMatrix4()}, and initialized with {@link
     * WGLWidget#initJavaScriptMatrix4(WGLWidget.JavaScriptMatrix4x4 mat)
     * WGLWidget#initJavaScriptMatrix4()}.
     */
    public JavaScriptMatrix4x4() {
      this.id_ = -1;
      this.jsRef_ = "";
      this.context_ = null;
      this.operations_ = new ArrayList<WGLWidget.JavaScriptMatrix4x4.op>();
      this.matrices_ = new ArrayList<javax.vecmath.Matrix4f>();
      this.initialized_ = false;
    }

    public int getId() {
      return this.id_;
    }
    /** Returns whether this {@link JavaScriptMatrix4x4} has been initialized. */
    public boolean isInitialized() {
      return this.initialized_;
    }
    /**
     * Returns whether this {@link JavaScriptMatrix4x4} has been assigned to a {@link WGLWidget}.
     */
    public boolean hasContext() {
      return this.context_ != null;
    }
    /**
     * Returns the JavaScript reference to this {@link JavaScriptMatrix4x4}.
     *
     * <p>In order to get a valid JavaScript reference, this matrix should have been added to a
     * {@link WGLWidget}.
     */
    public String getJsRef() {
      if (!this.hasContext()) {
        throw new WException("JavaScriptMatrix4x4: does not belong to a WGLWidget yet");
      }
      return this.jsRef_;
    }
    /**
     * Returns the current server-side value.
     *
     * <p>Client-side changes to the {@link JavaScriptMatrix4x4} are automatically synchronized.
     */
    public javax.vecmath.Matrix4f getValue() {
      if (!this.hasContext()) {
        throw new WException("JavaScriptMatrix4x4: matrix not assigned to a WGLWidget");
      }
      javax.vecmath.Matrix4f originalCpy =
          new javax.vecmath.Matrix4f(
              1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
              0.0f, 1.0f);
      for (int i = 0; i < this.context_.jsMatrixList_.size(); i++) {
        if (this.context_.jsMatrixList_.get(i).id == this.id_) {
          originalCpy = this.context_.jsMatrixList_.get(i).serverSideCopy;
        }
      }
      int nbMult = 0;
      for (int i = 0; i < this.operations_.size(); i++) {
        switch (this.operations_.get(i)) {
          case TRANSPOSE:
            originalCpy = WebGLUtils.transpose(originalCpy);
            break;
          case INVERT:
            originalCpy.invert();
            break;
          case MULTIPLY:
            originalCpy.mul(this.matrices_.get(nbMult));
            nbMult++;
            break;
        }
      }
      return originalCpy;
    }

    public WGLWidget.JavaScriptMatrix4x4 inverted() {
      if (!this.isInitialized()) {
        throw new WException("JavaScriptMatrix4x4: matrix not initialized");
      }
      WGLWidget.JavaScriptMatrix4x4 copy = this.clone();
      copy.jsRef_ =
          "Wt4.11.2.glMatrix.mat4.inverse(" + this.jsRef_ + ", Wt4.11.2.glMatrix.mat4.create())";
      copy.operations_.add(WGLWidget.JavaScriptMatrix4x4.op.INVERT);
      return copy;
    }

    public WGLWidget.JavaScriptMatrix4x4 transposed() {
      if (!this.isInitialized()) {
        throw new WException("JavaScriptMatrix4x4: matrix not initialized");
      }
      WGLWidget.JavaScriptMatrix4x4 copy = this.clone();
      copy.jsRef_ =
          "Wt4.11.2.glMatrix.mat4.transpose(" + this.jsRef_ + ", Wt4.11.2.glMatrix.mat4.create())";
      copy.operations_.add(WGLWidget.JavaScriptMatrix4x4.op.TRANSPOSE);
      return copy;
    }

    public WGLWidget.JavaScriptMatrix4x4 multiply(final javax.vecmath.Matrix4f m) {
      if (!this.isInitialized()) {
        throw new WException("JavaScriptMatrix4x4: matrix not initialized");
      }
      WGLWidget.JavaScriptMatrix4x4 copy = this.clone();
      StringWriter ss = new StringWriter();
      ss.append("Wt4.11.2.glMatrix.mat4.multiply(").append(this.jsRef_).append(",");
      javax.vecmath.Matrix4f t = WebGLUtils.transpose(m);
      WebGLUtils.renderfv(ss, t, this.context_.pImpl_.getArrayType());
      ss.append(", Wt4.11.2.glMatrix.mat4.create())");
      copy.jsRef_ = ss.toString();
      copy.operations_.add(WGLWidget.JavaScriptMatrix4x4.op.MULTIPLY);
      copy.matrices_.add(m);
      return copy;
    }

    public WGLWidget.JavaScriptMatrix4x4 clone() {
      WGLWidget.JavaScriptMatrix4x4 copy = new WGLWidget.JavaScriptMatrix4x4();
      copy.id_ = this.id_;
      copy.jsRef_ = this.jsRef_;
      copy.context_ = this.context_;
      copy.initialized_ = this.initialized_;
      Utils.copyList(this.operations_, copy.operations_);
      Utils.copyList(this.matrices_, copy.matrices_);
      return copy;
    }

    private void assignToContext(int id, WGLWidget context) {
      this.id_ = id;
      this.jsRef_ = context.getGlObjJsRef() + ".jsValues[" + String.valueOf(this.id_) + "]";
      this.context_ = context;
    }

    void initialize() {
      this.initialized_ = true;
    }

    private boolean hasOperations() {
      return this.operations_.size() > 0;
    }

    private int id_;
    private String jsRef_;
    WGLWidget context_;

    enum op {
      TRANSPOSE,
      INVERT,
      MULTIPLY;

      /** Returns the numerical representation of this enum. */
      public int getValue() {
        return ordinal();
      }
    }

    List<WGLWidget.JavaScriptMatrix4x4.op> operations_;
    List<javax.vecmath.Matrix4f> matrices_;
    private boolean initialized_;
  }
  /**
   * Construct a GL widget.
   *
   * <p>Before the first rendering, you must apply a size to the {@link WGLWidget}.
   */
  public WGLWidget(WContainerWidget parentContainer) {
    super();
    this.renderOptions_ =
        EnumSet.of(
            GLRenderOption.ClientSide, GLRenderOption.ServerSide, GLRenderOption.AntiAliasing);
    this.pImpl_ = null;
    this.jsMatrixList_ = new ArrayList<WGLWidget.jsMatrixMap>();
    this.jsVectorList_ = new ArrayList<WGLWidget.jsVectorMap>();
    this.jsValues_ = 0;
    this.js_ = new StringWriter();
    this.repaintSignal_ = new JSignal(this, "repaintSignal");
    this.alternative_ = null;
    this.webglNotAvailable_ = new JSignal(this, "webglNotAvailable");
    this.webGlNotAvailable_ = false;
    this.contextRestored_ = new JSignal(this, "contextRestored");
    this.restoringContext_ = false;
    this.valueChanged_ = false;
    this.mouseWentDownSlot_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".mouseDown(o, e);}", this);
    this.mouseWentUpSlot_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".mouseUp(o, e);}", this);
    this.mouseDraggedSlot_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".mouseDrag(o, e);}", this);
    this.mouseMovedSlot_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".mouseMove(o, e);}", this);
    this.mouseWheelSlot_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".mouseWheel(o, e);}", this);
    this.touchStarted_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".touchStart(o, e);}", this);
    this.touchEnded_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".touchEnd(o, e);}", this);
    this.touchMoved_ =
        new JSlot("function(o, e){" + this.getGlObjJsRef() + ".touchMoved(o, e);}", this);
    this.repaintSlot_ =
        new JSlot("function() {var o = " + this.getGlObjJsRef() + ";if(o.ctx) o.paintGL();}", this);
    this.setInline(false);
    this.setLayoutSizeAware(true);
    this.webglNotAvailable_.addListener(
        this,
        () -> {
          WGLWidget.this.webglNotAvailable();
        });
    this.repaintSignal_.addListener(
        this,
        () -> {
          WGLWidget.this.repaintGL(GLClientSideRenderer.PAINT_GL);
        });
    this.contextRestored_.addListener(
        this,
        () -> {
          WGLWidget.this.contextRestored();
        });
    this.mouseWentDown().addListener(this.mouseWentDownSlot_);
    this.mouseWentUp().addListener(this.mouseWentUpSlot_);
    this.mouseDragged().addListener(this.mouseDraggedSlot_);
    this.mouseMoved().addListener(this.mouseMovedSlot_);
    this.mouseWheel().addListener(this.mouseWheelSlot_);
    this.touchStarted().addListener(this.touchStarted_);
    this.touchEnded().addListener(this.touchEnded_);
    this.touchMoved().addListener(this.touchMoved_);
    this.setAlternativeContent(
        new WText("Your browser does not support WebGL", (WContainerWidget) null));
    this.setFormObject(true);
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Construct a GL widget.
   *
   * <p>Calls {@link #WGLWidget(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WGLWidget() {
    this((WContainerWidget) null);
  }
  /** Destructor. */
  public void remove() {
    {
      WWidget oldWidget = this.alternative_;
      this.alternative_ = null;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.alternative_);
        if (toRemove != null) toRemove.remove();
      }
    }
    super.remove();
  }
  /**
   * Sets the rendering option.
   *
   * <p>Use this method to configure whether client-side and/or server-side rendering can be used,
   * and whether anti-aliasing should be enabled. The actual choice is also based on availability
   * (respectively client-side or server-side).
   *
   * <p>The default value is to try both ClientSide or ServerSide rendering, and to enable
   * anti-aliasing if available.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Options must be set before the widget is being rendered. </i>
   */
  public void setRenderOptions(EnumSet<GLRenderOption> options) {
    this.renderOptions_ = EnumSet.copyOf(options);
  }
  /**
   * Sets the rendering option.
   *
   * <p>Calls {@link #setRenderOptions(EnumSet options) setRenderOptions(EnumSet.of(option,
   * options))}
   */
  public final void setRenderOptions(GLRenderOption option, GLRenderOption... options) {
    setRenderOptions(EnumSet.of(option, options));
  }
  /**
   * Initialize the GL state when the widget is first shown.
   *
   * <p>{@link WGLWidget#initializeGL() initializeGL()} is called when the widget is first rendered,
   * and when the webglcontextrestored signal is fired. You can distinguish between the first
   * initialization and context restoration using {@link WGLWidget#isRestoringContext()
   * isRestoringContext()}. It usually creates most of the GL related state: shaders, VBOs, uniform
   * locations, ...
   *
   * <p>If this state is to be updated during the lifetime of the widget, you should specialize the
   * {@link WGLWidget#updateGL() updateGL()} to accomodate for this.
   */
  protected void initializeGL() {}
  /**
   * Act on resize events.
   *
   * <p>Usually, this method only contains functions to set the viewport and the projection matrix
   * (as this is aspect ration dependent).
   *
   * <p>{@link WGLWidget#resizeGL(int width, int height) resizeGL()} is rendered after initializeGL,
   * and whenever widget is resized. After this method finishes, the widget is repainted with the
   * cached client-side paint function.
   */
  protected void resizeGL(int width, int height) {}
  /**
   * Update the client-side painting function.
   *
   * <p>This method is invoked client-side when a repaint is required, i.e. when the {@link
   * WGLWidget#getRepaintSlot() getRepaintSlot()} (a JavaScript-side {@link JSlot}) is triggered.
   * Typical examples are: after mouse-based camera movements, after a timed update of a camera or
   * an object&apos;s position, after a resize event ({@link WGLWidget#resizeGL(int width, int
   * height) resizeGL()} will also be called then), after an animation event, ... In many cases,
   * this function will be executed client-side many many times.
   *
   * <p>Using the GL functions from this class, you construct a scene. The implementation tracks all
   * JavaScript calls that need to be performed to draw the scenes, and will replay them verbatim on
   * every trigger of the {@link WGLWidget#getRepaintSlot() getRepaintSlot()}. There are a few
   * mechanisms that may be employed to change what is rendered without updating the {@link
   * WGLWidget#paintGL() paintGL()} cache:
   *
   * <ul>
   *   <li>Client-side matrices may be used to change camera viewpoints, manipilate separate
   *       object&apos;s model transformation matrices, ...
   *   <li>{@link Shader} sources can be updated without requiring the paint function to be renewed
   * </ul>
   *
   * <p>Updating the {@link WGLWidget#paintGL() paintGL()} cache is usually not too expensive; the
   * VBOs, which are large in many cases, are already at the client side, while the {@link
   * WGLWidget#paintGL() paintGL()} code only draws the VBOs. Of course, if you have to draw many
   * separate objects, the {@link WGLWidget#paintGL() paintGL()} JS code may become large and
   * updating is more expensive.
   *
   * <p>In order to update the {@link WGLWidget#paintGL() paintGL()} cache, call {@link
   * WGLWidget#repaintGL(EnumSet which) repaintGL()} with the PAINT_GL parameter, which will cause
   * the invocation of this method.
   */
  protected void paintGL() {}
  /**
   * Update state set in {@link WGLWidget#initializeGL() initializeGL()}
   *
   * <p>Invoked when repaint is called with the UPDATE_GL call.
   *
   * <p>This is intended to be executed when you want to change programs, &apos;constant&apos;
   * uniforms, or even VBO&apos;s, ... without resending already initialized data. It is a mechanism
   * to make changes to what you&apos;ve set in intializeGL(). For every server-side invocation of
   * this method, the result will be rendered client-side exactly once.
   */
  protected void updateGL() {}
  /**
   * Request invocation of resizeGL, paintGL and/or updateGL.
   *
   * <p>If invoked with PAINT_GL, the client-side cached paint function is updated. If invoked with
   * RESIZE_GL or UPDATE_GL, the code will be executed once.
   *
   * <p>If invoked with multiple flags set, the order of execution will be {@link
   * WGLWidget#updateGL() updateGL()}, {@link WGLWidget#resizeGL(int width, int height) resizeGL()},
   * {@link WGLWidget#paintGL() paintGL()}.
   */
  public void repaintGL(EnumSet<GLClientSideRenderer> which) {
    if (!(this.pImpl_ != null)) {
      return;
    }
    this.pImpl_.repaintGL(which);
    if (!which.isEmpty()) {
      this.repaint();
    }
  }
  /**
   * Request invocation of resizeGL, paintGL and/or updateGL.
   *
   * <p>Calls {@link #repaintGL(EnumSet which) repaintGL(EnumSet.of(whic, which))}
   */
  public final void repaintGL(GLClientSideRenderer whic, GLClientSideRenderer... which) {
    repaintGL(EnumSet.of(whic, which));
  }
  /**
   * Returns whether a lost context is in the process of being restored.
   *
   * <p>You can check for this in {@link WGLWidget#initializeGL() initializeGL()}, to handle the
   * first initialization and restoration of context differently.
   */
  public boolean isRestoringContext() {
    return this.restoringContext_;
  }

  public void resize(final WLength width, final WLength height) {
    super.resize(width, height);
    if (this.pImpl_ != null) {
      this.layoutSizeChanged((int) width.getValue(), (int) height.getValue());
    }
  }
  /**
   * The enormous GLenum.
   *
   * <p>This enum contains all numeric constants defined by the WebGL standard, see: <a
   * href="http://www.khronos.org/registry/webgl/specs/latest/1.0/#WEBGLRENDERINGCONTEXT">http://www.khronos.org/registry/webgl/specs/latest/1.0/#WEBGLRENDERINGCONTEXT</a>
   */
  public enum GLenum {
    DEPTH_BUFFER_BIT,
    STENCIL_BUFFER_BIT,
    COLOR_BUFFER_BIT,
    POINTS,
    LINES,
    LINE_LOOP,
    LINE_STRIP,
    TRIANGLES,
    TRIANGLE_STRIP,
    TRIANGLE_FAN,
    ZERO,
    ONE,
    SRC_COLOR,
    ONE_MINUS_SRC_COLOR,
    SRC_ALPHA,
    ONE_MINUS_SRC_ALPHA,
    DST_ALPHA,
    ONE_MINUS_DST_ALPHA,
    DST_COLOR,
    ONE_MINUS_DST_COLOR,
    SRC_ALPHA_SATURATE,
    FUNC_ADD,
    BLEND_EQUATION,
    BLEND_EQUATION_RGB,
    BLEND_EQUATION_ALPHA,
    FUNC_SUBTRACT,
    FUNC_REVERSE_SUBTRACT,
    BLEND_DST_RGB,
    BLEND_SRC_RGB,
    BLEND_DST_ALPHA,
    BLEND_SRC_ALPHA,
    CONSTANT_COLOR,
    ONE_MINUS_CONSTANT_COLOR,
    CONSTANT_ALPHA,
    ONE_MINUS_CONSTANT_ALPHA,
    BLEND_COLOR,
    ARRAY_BUFFER,
    ELEMENT_ARRAY_BUFFER,
    ARRAY_BUFFER_BINDING,
    ELEMENT_ARRAY_BUFFER_BINDING,
    STREAM_DRAW,
    STATIC_DRAW,
    DYNAMIC_DRAW,
    BUFFER_SIZE,
    BUFFER_USAGE,
    CURRENT_VERTEX_ATTRIB,
    FRONT,
    BACK,
    FRONT_AND_BACK,
    CULL_FACE,
    BLEND,
    DITHER,
    STENCIL_TEST,
    DEPTH_TEST,
    SCISSOR_TEST,
    POLYGON_OFFSET_FILL,
    SAMPLE_ALPHA_TO_COVERAGE,
    SAMPLE_COVERAGE,
    NO_ERROR,
    INVALID_ENUM,
    INVALID_VALUE,
    INVALID_OPERATION,
    OUT_OF_MEMORY,
    CW,
    CCW,
    LINE_WIDTH,
    ALIASED_POINT_SIZE_RANGE,
    ALIASED_LINE_WIDTH_RANGE,
    CULL_FACE_MODE,
    FRONT_FACE,
    DEPTH_RANGE,
    DEPTH_WRITEMASK,
    DEPTH_CLEAR_VALUE,
    DEPTH_FUNC,
    STENCIL_CLEAR_VALUE,
    STENCIL_FUNC,
    STENCIL_FAIL,
    STENCIL_PASS_DEPTH_FAIL,
    STENCIL_PASS_DEPTH_PASS,
    STENCIL_REF,
    STENCIL_VALUE_MASK,
    STENCIL_WRITEMASK,
    STENCIL_BACK_FUNC,
    STENCIL_BACK_FAIL,
    STENCIL_BACK_PASS_DEPTH_FAIL,
    STENCIL_BACK_PASS_DEPTH_PASS,
    STENCIL_BACK_REF,
    STENCIL_BACK_VALUE_MASK,
    STENCIL_BACK_WRITEMASK,
    VIEWPORT,
    SCISSOR_BOX,
    COLOR_CLEAR_VALUE,
    COLOR_WRITEMASK,
    UNPACK_ALIGNMENT,
    PACK_ALIGNMENT,
    MAX_TEXTURE_SIZE,
    MAX_VIEWPORT_DIMS,
    SUBPIXEL_BITS,
    RED_BITS,
    GREEN_BITS,
    BLUE_BITS,
    ALPHA_BITS,
    DEPTH_BITS,
    STENCIL_BITS,
    POLYGON_OFFSET_UNITS,
    POLYGON_OFFSET_FACTOR,
    TEXTURE_BINDING_2D,
    SAMPLE_BUFFERS,
    SAMPLES,
    SAMPLE_COVERAGE_VALUE,
    SAMPLE_COVERAGE_INVERT,
    NUM_COMPRESSED_TEXTURE_FORMATS,
    COMPRESSED_TEXTURE_FORMATS,
    DONT_CARE,
    FASTEST,
    NICEST,
    GENERATE_MIPMAP_HINT,
    BYTE,
    UNSIGNED_BYTE,
    SHORT,
    UNSIGNED_SHORT,
    INT,
    UNSIGNED_INT,
    FLOAT,
    DEPTH_COMPONENT,
    ALPHA,
    RGB,
    RGBA,
    LUMINANCE,
    LUMINANCE_ALPHA,
    UNSIGNED_SHORT_4_4_4_4,
    UNSIGNED_SHORT_5_5_5_1,
    UNSIGNED_SHORT_5_6_5,
    FRAGMENT_SHADER,
    VERTEX_SHADER,
    MAX_VERTEX_ATTRIBS,
    MAX_VERTEX_UNIFORM_VECTORS,
    MAX_VARYING_VECTORS,
    MAX_COMBINED_TEXTURE_IMAGE_UNITS,
    MAX_VERTEX_TEXTURE_IMAGE_UNITS,
    MAX_TEXTURE_IMAGE_UNITS,
    MAX_FRAGMENT_UNIFORM_VECTORS,
    SHADER_TYPE,
    DELETE_STATUS,
    LINK_STATUS,
    VALIDATE_STATUS,
    ATTACHED_SHADERS,
    ACTIVE_UNIFORMS,
    ACTIVE_UNIFORM_MAX_LENGTH,
    ACTIVE_ATTRIBUTES,
    ACTIVE_ATTRIBUTE_MAX_LENGTH,
    SHADING_LANGUAGE_VERSION,
    CURRENT_PROGRAM,
    NEVER,
    LESS,
    EQUAL,
    LEQUAL,
    GREATER,
    NOTEQUAL,
    GEQUAL,
    ALWAYS,
    KEEP,
    REPLACE,
    INCR,
    DECR,
    INVERT,
    INCR_WRAP,
    DECR_WRAP,
    VENDOR,
    RENDERER,
    VERSION,
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR,
    TEXTURE_MAG_FILTER,
    TEXTURE_MIN_FILTER,
    TEXTURE_WRAP_S,
    TEXTURE_WRAP_T,
    TEXTURE_2D,
    TEXTURE,
    TEXTURE_CUBE_MAP,
    TEXTURE_BINDING_CUBE_MAP,
    TEXTURE_CUBE_MAP_POSITIVE_X,
    TEXTURE_CUBE_MAP_NEGATIVE_X,
    TEXTURE_CUBE_MAP_POSITIVE_Y,
    TEXTURE_CUBE_MAP_NEGATIVE_Y,
    TEXTURE_CUBE_MAP_POSITIVE_Z,
    TEXTURE_CUBE_MAP_NEGATIVE_Z,
    MAX_CUBE_MAP_TEXTURE_SIZE,
    TEXTURE0,
    TEXTURE1,
    TEXTURE2,
    TEXTURE3,
    TEXTURE4,
    TEXTURE5,
    TEXTURE6,
    TEXTURE7,
    TEXTURE8,
    TEXTURE9,
    TEXTURE10,
    TEXTURE11,
    TEXTURE12,
    TEXTURE13,
    TEXTURE14,
    TEXTURE15,
    TEXTURE16,
    TEXTURE17,
    TEXTURE18,
    TEXTURE19,
    TEXTURE20,
    TEXTURE21,
    TEXTURE22,
    TEXTURE23,
    TEXTURE24,
    TEXTURE25,
    TEXTURE26,
    TEXTURE27,
    TEXTURE28,
    TEXTURE29,
    TEXTURE30,
    TEXTURE31,
    ACTIVE_TEXTURE,
    REPEAT,
    CLAMP_TO_EDGE,
    MIRRORED_REPEAT,
    FLOAT_VEC2,
    FLOAT_VEC3,
    FLOAT_VEC4,
    INT_VEC2,
    INT_VEC3,
    INT_VEC4,
    BOOL,
    BOOL_VEC2,
    BOOL_VEC3,
    BOOL_VEC4,
    FLOAT_MAT2,
    FLOAT_MAT3,
    FLOAT_MAT4,
    SAMPLER_2D,
    SAMPLER_CUBE,
    VERTEX_ATTRIB_ARRAY_ENABLED,
    VERTEX_ATTRIB_ARRAY_SIZE,
    VERTEX_ATTRIB_ARRAY_STRIDE,
    VERTEX_ATTRIB_ARRAY_TYPE,
    VERTEX_ATTRIB_ARRAY_NORMALIZED,
    VERTEX_ATTRIB_ARRAY_POINTER,
    VERTEX_ATTRIB_ARRAY_BUFFER_BINDING,
    COMPILE_STATUS,
    INFO_LOG_LENGTH,
    SHADER_SOURCE_LENGTH,
    LOW_FLOAT,
    MEDIUM_FLOAT,
    HIGH_FLOAT,
    LOW_INT,
    MEDIUM_INT,
    HIGH_INT,
    FRAMEBUFFER,
    RENDERBUFFER,
    RGBA4,
    RGB5_A1,
    RGB565,
    DEPTH_COMPONENT16,
    STENCIL_INDEX,
    STENCIL_INDEX8,
    DEPTH_STENCIL,
    RENDERBUFFER_WIDTH,
    RENDERBUFFER_HEIGHT,
    RENDERBUFFER_INTERNAL_FORMAT,
    RENDERBUFFER_RED_SIZE,
    RENDERBUFFER_GREEN_SIZE,
    RENDERBUFFER_BLUE_SIZE,
    RENDERBUFFER_ALPHA_SIZE,
    RENDERBUFFER_DEPTH_SIZE,
    RENDERBUFFER_STENCIL_SIZE,
    FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE,
    FRAMEBUFFER_ATTACHMENT_OBJECT_NAME,
    FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL,
    FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE,
    COLOR_ATTACHMENT0,
    DEPTH_ATTACHMENT,
    STENCIL_ATTACHMENT,
    DEPTH_STENCIL_ATTACHMENT,
    NONE,
    FRAMEBUFFER_COMPLETE,
    FRAMEBUFFER_INCOMPLETE_ATTACHMENT,
    FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT,
    FRAMEBUFFER_INCOMPLETE_DIMENSIONS,
    FRAMEBUFFER_UNSUPPORTED,
    FRAMEBUFFER_BINDING,
    RENDERBUFFER_BINDING,
    MAX_RENDERBUFFER_SIZE,
    INVALID_FRAMEBUFFER_OPERATION,
    UNPACK_FLIP_Y_WEBGL,
    UNPACK_PREMULTIPLY_ALPHA_WEBGL,
    CONTEXT_LOST_WEBGL,
    UNPACK_COLORSPACE_CONVERSION_WEBGL,
    BROWSER_DEFAULT_WEBGL;

    /** Returns the numerical representation of this enum. */
    public int getValue() {
      return ordinal();
    }
  }

  public void debugger() {
    this.pImpl_.debugger();
  }
  /**
   * GL function to activate an existing texture.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glActiveTexture.xml">glActiveTexture()
   * OpenGL ES manpage</a>
   */
  public void activeTexture(WGLWidget.GLenum texture) {
    this.pImpl_.activeTexture(texture);
  }
  /**
   * GL function to attach a shader to a program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glAttachShader.xml">glAttachShader()
   * OpenGL ES manpage</a>
   */
  public void attachShader(WGLWidget.Program program, WGLWidget.Shader shader) {
    this.pImpl_.attachShader(program, shader);
  }
  /**
   * GL function to bind an attribute to a given location.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBindAttribLocation.xml">glBindAttribLocation()
   * OpenGL ES manpage</a>
   */
  public void bindAttribLocation(WGLWidget.Program program, int index, final String name) {
    this.pImpl_.bindAttribLocation(program, index, name);
  }
  /**
   * GL function to bind a buffer to a target.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBindBuffer.xml">glBindBuffer()
   * OpenGL ES manpage</a>
   */
  public void bindBuffer(WGLWidget.GLenum target, WGLWidget.Buffer buffer) {
    this.pImpl_.bindBuffer(target, buffer);
  }
  /**
   * GL function to bind a frame buffer to a target.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBindFramebuffer.xml">glBindFramebuffer()
   * OpenGL ES manpage</a>
   */
  public void bindFramebuffer(WGLWidget.GLenum target, WGLWidget.Framebuffer buffer) {
    this.pImpl_.bindFramebuffer(target, buffer);
  }
  /**
   * GL function to bind a render buffer to a target.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBindRenderbuffer.xml">glBindRenderbuffer()
   * OpenGL ES manpage</a>
   */
  public void bindRenderbuffer(WGLWidget.GLenum target, WGLWidget.Renderbuffer buffer) {
    this.pImpl_.bindRenderbuffer(target, buffer);
  }
  /**
   * GL function to bind a texture to a target.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBindTexture.xml">glBindTexture()
   * OpenGL ES manpage</a>
   */
  public void bindTexture(WGLWidget.GLenum target, WGLWidget.Texture texture) {
    this.pImpl_.bindTexture(target, texture);
  }
  /**
   * GL function to set the blending color.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBlendColor.xml">glBlendColor()
   * OpenGL ES manpage</a>
   */
  public void blendColor(double red, double green, double blue, double alpha) {
    this.pImpl_.blendColor(red, green, blue, alpha);
  }
  /**
   * GL function to set the blending equation.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBlendEquation.xml">glBlendEquation()
   * OpenGL ES manpage</a>
   */
  public void blendEquation(WGLWidget.GLenum mode) {
    this.pImpl_.blendEquation(mode);
  }
  /**
   * GL function that sets separate blending functions for RGB and alpha.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBlendEquationSeparate.xml">glBlendEquationSeparate()
   * OpenGL ES manpage</a>
   */
  public void blendEquationSeparate(WGLWidget.GLenum modeRGB, WGLWidget.GLenum modeAlpha) {
    this.pImpl_.blendEquationSeparate(modeRGB, modeAlpha);
  }
  /**
   * GL function to configure the blending function.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBlendFunc.xml">glBlendFunc()
   * OpenGL ES manpage</a>
   */
  public void blendFunc(WGLWidget.GLenum sfactor, WGLWidget.GLenum dfactor) {
    this.pImpl_.blendFunc(sfactor, dfactor);
  }
  /**
   * GL function that configures the blending function.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBlendFuncSeparate.xml">glBlendFuncSeparate()
   * OpenGL ES manpage</a>
   */
  public void blendFuncSeparate(
      WGLWidget.GLenum srcRGB,
      WGLWidget.GLenum dstRGB,
      WGLWidget.GLenum srcAlpha,
      WGLWidget.GLenum dstAlpha) {
    this.pImpl_.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
  }
  /**
   * glBufferData - create and initialize a buffer object&apos;s data store
   *
   * <p>Set the size of the currently bound WebGLBuffer object for the passed target. The buffer is
   * initialized to 0.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBufferData.xml">glBufferData()
   * OpenGL ES manpage</a>
   */
  public void bufferData(WGLWidget.GLenum target, int size, WGLWidget.GLenum usage) {
    this.pImpl_.bufferData(target, size, usage);
  }
  /**
   * glBufferData - create and initialize a buffer object&apos;s data store from an {@link
   * ArrayBuffer}
   *
   * <p>Set the size and contents of the currently bound WebGLBuffer object to be a copy of the
   * given {@link ArrayBuffer}.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBufferData.xml">glBufferData()
   * OpenGL ES manpage</a>
   *
   * <p>Note: an {@link ArrayBuffer} refers to a javascript object, which cannot be used for
   * server-side rendering. If a server-side fallback will be used, then {@link
   * WGLWidget#bufferDatafv(WGLWidget.GLenum target, java.nio.ByteBuffer buffer, WGLWidget.GLenum
   * usage, boolean binary) bufferDatafv()} should be used with the additional boolean argument to
   * indicate binary transfer of the data in case of client-side rendering.
   *
   * <p>
   */
  public void bufferData(
      WGLWidget.GLenum target, WGLWidget.ArrayBuffer res, WGLWidget.GLenum usage) {
    this.pImpl_.bufferData(target, res, usage);
  }
  /**
   * glBufferData - create and initialize a buffer object&apos;s data store from an {@link
   * ArrayBuffer}
   *
   * <p>Set the size of the currently bound WebGLBuffer object to arrayBufferSize, and copy the
   * contents of the {@link ArrayBuffer} to the buffer, starting at the given offset.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBufferData.xml">glBufferData()
   * OpenGL ES manpage</a>
   *
   * <p>Note: not functional for a server-side fall-back (see {@link
   * WGLWidget#bufferData(WGLWidget.GLenum target, WGLWidget.ArrayBuffer res, WGLWidget.GLenum
   * usage) bufferData()} for more info)
   *
   * <p>
   */
  public void bufferData(
      WGLWidget.GLenum target,
      WGLWidget.ArrayBuffer res,
      int bufferResourceOffset,
      int bufferResourceSize,
      WGLWidget.GLenum usage) {
    this.pImpl_.bufferData(target, res, bufferResourceOffset, bufferResourceSize, usage);
  }
  /**
   * Initialize a buffer object&apos;s data store from an {@link ArrayBuffer}.
   *
   * <p>Load the data of the currently bound WebGLBuffer object from the given {@link ArrayBuffer}.
   * The first byte of the resource data will be written at the given offset of the currently bound
   * buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBufferSubData.xml">glBufferSubData()
   * OpenGL ES manpage</a>
   *
   * <p>Note: not functional for a server-side fall-back (see {@link
   * WGLWidget#bufferData(WGLWidget.GLenum target, WGLWidget.ArrayBuffer res, WGLWidget.GLenum
   * usage) bufferData()} for more info)
   *
   * <p>
   */
  public void bufferSubData(WGLWidget.GLenum target, int offset, WGLWidget.ArrayBuffer res) {
    this.pImpl_.bufferSubData(target, offset, res);
  }
  /**
   * Initialize a buffer object&apos;s data store from an {@link ArrayBuffer}.
   *
   * <p>Load the data of the currently bound WebGLBuffer object from the given {@link ArrayBuffer}.
   * The byte at position arrayBufferOffset will be written to the currently bound buffer at
   * position offset.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glBufferSubData.xml">glBufferSubData()
   * OpenGL ES manpage</a>
   *
   * <p>Note: not functional for a server-side fall-back (see {@link
   * WGLWidget#bufferData(WGLWidget.GLenum target, WGLWidget.ArrayBuffer res, WGLWidget.GLenum
   * usage) bufferData()} for more info)
   *
   * <p>
   */
  public void bufferSubData(
      WGLWidget.GLenum target,
      int offset,
      WGLWidget.ArrayBuffer res,
      int bufferResourceOffset,
      int bufferResourceSize) {
    this.pImpl_.bufferSubData(target, offset, res, bufferResourceOffset, bufferResourceSize);
  }
  /**
   * GL function that loads float or double data in a VBO.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/glBufferData.xml">glBufferData()
   * OpenGL ES manpage</a>
   */
  public void bufferDatafv(
      WGLWidget.GLenum target,
      final java.nio.ByteBuffer buffer,
      WGLWidget.GLenum usage,
      boolean binary) {
    this.pImpl_.bufferDatafv(target, buffer, usage, binary);
  }
  /**
   * GL function that loads float or double data in a VBO.
   *
   * <p>Calls {@link #bufferDatafv(WGLWidget.GLenum target, java.nio.ByteBuffer buffer,
   * WGLWidget.GLenum usage, boolean binary) bufferDatafv(target, buffer, usage, false)}
   */
  public final void bufferDatafv(
      WGLWidget.GLenum target, final java.nio.ByteBuffer buffer, WGLWidget.GLenum usage) {
    bufferDatafv(target, buffer, usage, false);
  }

  public void bufferDatafv(
      WGLWidget.GLenum target, final java.nio.FloatBuffer buffer, WGLWidget.GLenum usage) {
    this.pImpl_.bufferDatafv(target, buffer, usage);
  }
  /**
   * remove all binary buffer resources
   *
   * <p>Removes all WMemoryResources that were allocated when calling bufferDatafv with binary=true.
   * This is not required, since the resources are also managed, but if you are sure they will not
   * be used anymore in the client, this can help free some memory.
   */
  public void clearBinaryResources() {
    this.pImpl_.clearBinaryResources();
  }
  /**
   * GL function that updates an existing VBO with new integer data.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/glBufferData.xml">glBufferData()
   * OpenGL ES manpage</a>
   */
  public void bufferDataiv(
      WGLWidget.GLenum target,
      final java.nio.IntBuffer buffer,
      WGLWidget.GLenum usage,
      WGLWidget.GLenum type) {
    this.pImpl_.bufferDataiv(target, buffer, usage, type);
  }
  /**
   * GL function that updates an existing VBO with new float data.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/glBufferSubData.xml">glBufferSubData()
   * OpenGL ES manpage</a>
   */
  public void bufferSubDatafv(
      WGLWidget.GLenum target, int offset, final java.nio.ByteBuffer buffer, boolean binary) {
    this.pImpl_.bufferSubDatafv(target, offset, buffer, binary);
  }
  /**
   * GL function that updates an existing VBO with new float data.
   *
   * <p>Calls {@link #bufferSubDatafv(WGLWidget.GLenum target, int offset, java.nio.ByteBuffer
   * buffer, boolean binary) bufferSubDatafv(target, offset, buffer, false)}
   */
  public final void bufferSubDatafv(
      WGLWidget.GLenum target, int offset, final java.nio.ByteBuffer buffer) {
    bufferSubDatafv(target, offset, buffer, false);
  }

  public void bufferSubDatafv(
      WGLWidget.GLenum target, int offset, final java.nio.FloatBuffer buffer) {
    this.pImpl_.bufferSubDatafv(target, offset, buffer);
  }
  /**
   * GL function that loads integer data in a VBO.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/glBufferSubData.xml">glBufferSubData()
   * OpenGL ES manpage</a>
   */
  public void bufferSubDataiv(
      WGLWidget.GLenum target, int offset, final java.nio.IntBuffer buffer, WGLWidget.GLenum type) {
    this.pImpl_.bufferSubDataiv(target, offset, buffer, type);
  }
  /**
   * GL function that clears the given buffers.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glClear.xml">glClear()
   * OpenGL ES manpage</a>
   */
  public void clear(EnumSet<WGLWidget.GLenum> mask) {
    this.pImpl_.clear(mask);
  }
  /**
   * GL function that clears the given buffers.
   *
   * <p>Calls {@link #clear(EnumSet mask) clear(EnumSet.of(mas, mask))}
   */
  public final void clear(WGLWidget.GLenum mas, WGLWidget.GLenum... mask) {
    clear(EnumSet.of(mas, mask));
  }
  /**
   * GL function that sets the clear color of the color buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glClearColor.xml">glClearColor()
   * OpenGL ES manpage</a>
   */
  public void clearColor(double r, double g, double b, double a) {
    this.pImpl_.clearColor(r, g, b, a);
  }
  /**
   * GL function that configures the depth to be set when the depth buffer is cleared.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glClearDepthf.xml">glClearDepthf()
   * OpenGL ES manpage</a>
   */
  public void clearDepth(double depth) {
    this.pImpl_.clearDepth(depth);
  }
  /**
   * GL function.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glClearStencil.xml">glClearStencil()
   * OpenGL ES manpage</a>
   */
  public void clearStencil(int s) {
    this.pImpl_.clearStencil(s);
  }
  /**
   * GL function.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glColorMask.xml">glColorMask()
   * OpenGL ES manpage</a>
   */
  public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
    this.pImpl_.colorMask(red, green, blue, alpha);
  }
  /**
   * GL function to compile a shader.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glCompileShader.xml">glCompileShader()
   * OpenGL ES manpage</a>
   */
  public void compileShader(WGLWidget.Shader shader) {
    this.pImpl_.compileShader(shader);
  }
  /**
   * GL function to copy a texture image.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glCopyTexImage2D.xml">glCopyTexImage2D()
   * OpenGL ES manpage</a>
   */
  public void copyTexImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalFormat,
      int x,
      int y,
      int width,
      int height,
      int border) {
    this.pImpl_.copyTexImage2D(target, level, internalFormat, x, y, width, height, border);
  }
  /**
   * GL function that copies a part of a texture image.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glCopyTexSubImage2D.xml">glCopyTexSubImage2D()
   * OpenGL ES manpage</a>
   */
  public void copyTexSubImage2D(
      WGLWidget.GLenum target,
      int level,
      int xoffset,
      int yoffset,
      int x,
      int y,
      int width,
      int height) {
    this.pImpl_.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
  }
  /**
   * GL function that creates an empty VBO.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGenBuffers.xml">glGenBuffers()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.Buffer createBuffer() {
    return this.pImpl_.getCreateBuffer();
  }
  /**
   * GL function that creates a frame buffer object.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGenFramebuffers.xml">glGenFramebuffers()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.Framebuffer getCreateFramebuffer() {
    return this.pImpl_.getCreateFramebuffer();
  }
  /**
   * GL function that creates an empty program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glCreateProgram.xml">glCreateProgram()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.Program createProgram() {
    return this.pImpl_.getCreateProgram();
  }
  /**
   * GL function that creates a render buffer object.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGenRenderbuffers.xml">glGenRenderbuffers()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.Renderbuffer getCreateRenderbuffer() {
    return this.pImpl_.getCreateRenderbuffer();
  }
  /**
   * GL function that creates an empty shader.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glCreateShader.xml">glCreateShader()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.Shader createShader(WGLWidget.GLenum shader) {
    return this.pImpl_.createShader(shader);
  }
  /**
   * GL function that creates an empty texture.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGenTextures.xml">glGenTextures()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.Texture createTexture() {
    return this.pImpl_.getCreateTexture();
  }
  /**
   * GL function that creates an image texture.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGenTextures.xml">glGenTextures()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.Texture createTextureAndLoad(final String url) {
    return this.pImpl_.createTextureAndLoad(url);
  }
  /**
   * returns an paintdevice that can be used to paint a GL texture
   *
   * <p>If the client has a webGL enabled browser this function returns a {@link
   * WCanvasPaintDevice}.
   *
   * <p>If server-side rendering is used as fallback then this function returns a
   * WRasterPaintDevice.
   */
  public WPaintDevice createPaintDevice(final WLength width, final WLength height) {
    return this.pImpl_.createPaintDevice(width, height);
  }
  /**
   * GL function that configures the backface culling mode.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glCullFace.xml">glCullFace()
   * OpenGL ES manpage</a>
   */
  public void cullFace(WGLWidget.GLenum mode) {
    this.pImpl_.cullFace(mode);
  }
  /**
   * GL function that deletes a VBO.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDeleteBuffers.xml">glDeleteBuffers()
   * OpenGL ES manpage</a>
   */
  public void deleteBuffer(WGLWidget.Buffer buffer) {
    this.pImpl_.deleteBuffer(buffer);
  }
  /**
   * GL function that deletes a frame buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDeleteFramebuffers.xml">glDeleteFramebuffers()
   * OpenGL ES manpage</a>
   */
  public void deleteFramebuffer(WGLWidget.Framebuffer buffer) {
    this.pImpl_.deleteFramebuffer(buffer);
  }
  /**
   * GL function that deletes a program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDeleteProgram.xml">glDeleteProgram()
   * OpenGL ES manpage</a>
   */
  public void deleteProgram(WGLWidget.Program program) {
    this.pImpl_.deleteProgram(program);
  }
  /**
   * GL function that deletes a render buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDeleteRenderbuffers.xml">glDeleteRenderbuffers()
   * OpenGL ES manpage</a>
   */
  public void deleteRenderbuffer(WGLWidget.Renderbuffer buffer) {
    this.pImpl_.deleteRenderbuffer(buffer);
  }
  /**
   * GL function that depetes a shader.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDeleteShader.xml">glDeleteShader()
   * OpenGL ES manpage</a>
   */
  public void deleteShader(WGLWidget.Shader shader) {
    this.pImpl_.deleteShader(shader);
  }
  /**
   * GL function that deletes a texture.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDeleteTextures.xml">glDeleteTextures()
   * OpenGL ES manpage</a>
   */
  public void deleteTexture(WGLWidget.Texture texture) {
    this.pImpl_.deleteTexture(texture);
  }
  /**
   * GL function to set the depth test function.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDepthFunc.xml">glDepthFunc()
   * OpenGL ES manpage</a>
   */
  public void depthFunc(WGLWidget.GLenum func) {
    this.pImpl_.depthFunc(func);
  }
  /**
   * GL function that enables or disables writing to the depth buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDepthMask.xml">glDepthMask()
   * OpenGL ES manpage</a>
   */
  public void depthMask(boolean flag) {
    this.pImpl_.depthMask(flag);
  }
  /**
   * GL function that specifies to what range the normalized [-1,1] z values should match.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDepthRangef.xml">glDepthRangef()
   * OpenGL ES manpage</a>
   */
  public void depthRange(double zNear, double zFar) {
    this.pImpl_.depthRange(zNear, zFar);
  }
  /**
   * GL function that detaches a shader from a program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDetachShader.xml">glDetachShader()
   * OpenGL ES manpage</a>
   */
  public void detachShader(WGLWidget.Program program, WGLWidget.Shader shader) {
    this.pImpl_.detachShader(program, shader);
  }
  /**
   * GL function to disable features.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDisable.xml">glDisable()
   * OpenGL ES manpage</a>
   */
  public void disable(WGLWidget.GLenum cap) {
    this.pImpl_.disable(cap);
  }
  /**
   * GL function to disable the vertex attribute array.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDisableVertexAttribArray.xml">glDisableVertexAttribArray()
   * OpenGL ES manpage</a>
   */
  public void disableVertexAttribArray(WGLWidget.AttribLocation index) {
    this.pImpl_.disableVertexAttribArray(index);
  }
  /**
   * GL function to draw a VBO.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDrawArrays.xml">glDrawArrays()
   * OpenGL ES manpage</a>
   */
  public void drawArrays(WGLWidget.GLenum mode, int first, int count) {
    this.pImpl_.drawArrays(mode, first, count);
  }
  /**
   * GL function to draw indexed VBOs.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glDrawElements.xml">glDrawElements()
   * OpenGL ES manpage</a>
   */
  public void drawElements(WGLWidget.GLenum mode, int count, WGLWidget.GLenum type, int offset) {
    this.pImpl_.drawElements(mode, count, type, offset);
  }
  /**
   * GL function to enable features.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glEnable.xml">glEnable()
   * OpenGL ES manpage</a>
   */
  public void enable(WGLWidget.GLenum cap) {
    this.pImpl_.enable(cap);
  }
  /**
   * GL function to enable the vertex attribute array.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glEnableVertexAttribArray.xml">glEnableVertexAttribArray()
   * OpenGL ES manpage</a>
   */
  public void enableVertexAttribArray(WGLWidget.AttribLocation index) {
    this.pImpl_.enableVertexAttribArray(index);
  }
  /**
   * GL function to wait until given commands are executed.
   *
   * <p>This call is transfered to JS, but the server will never wait on this call.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glFinish.xml">glFinish()
   * OpenGL ES manpage</a>
   */
  public void finish() {
    this.pImpl_.finish();
  }
  /**
   * GL function to force execution of GL commands in finite time.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glFlush.xml">glFlush()
   * OpenGL ES manpage</a>
   */
  public void flush() {
    this.pImpl_.flush();
  }
  /**
   * GL function to attach the given renderbuffer to the currently bound frame buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glFramebufferRenderbuffer.xml">glFramebufferRenderbuffer()
   * OpenGL ES manpage</a>
   */
  public void framebufferRenderbuffer(
      WGLWidget.GLenum target,
      WGLWidget.GLenum attachment,
      WGLWidget.GLenum renderbuffertarget,
      WGLWidget.Renderbuffer renderbuffer) {
    this.pImpl_.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
  }
  /**
   * GL function to render directly into a texture image.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glFramebufferTexture2D.xml">glFramebufferTexture2D()
   * OpenGL ES manpage</a>
   */
  public void framebufferTexture2D(
      WGLWidget.GLenum target,
      WGLWidget.GLenum attachment,
      WGLWidget.GLenum textarget,
      WGLWidget.Texture texture,
      int level) {
    this.pImpl_.framebufferTexture2D(target, attachment, textarget, texture, level);
  }
  /**
   * GL function that specifies which side of a triangle is the front side.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glFrontFace.xml">glFrontFace()
   * OpenGL ES manpage</a>
   */
  public void frontFace(WGLWidget.GLenum mode) {
    this.pImpl_.frontFace(mode);
  }
  /**
   * GL function that generates a set of mipmaps for a texture object.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGenerateMipmap.xml">glGenerateMipmap()
   * OpenGL ES manpage</a>
   */
  public void generateMipmap(WGLWidget.GLenum target) {
    this.pImpl_.generateMipmap(target);
  }
  /**
   * GL function to retrieve an attribute&apos;s location in a {@link Program}.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGetAttribLocation.xml">glGetAttribLocation()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.AttribLocation getAttribLocation(
      WGLWidget.Program program, final String attrib) {
    return this.pImpl_.getAttribLocation(program, attrib);
  }
  /**
   * GL function to retrieve a Uniform&apos;s location in a {@link Program}.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glGetUniformLocation.xml">glGetUniformLocation()
   * OpenGL ES manpage</a>
   */
  public WGLWidget.UniformLocation getUniformLocation(
      WGLWidget.Program program, final String location) {
    return this.pImpl_.getUniformLocation(program, location);
  }
  /**
   * GL function to give hints to the render pipeline.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glHint.xml">glHint() OpenGL
   * ES manpage</a>
   */
  public void hint(WGLWidget.GLenum target, WGLWidget.GLenum mode) {
    this.pImpl_.hint(target, mode);
  }
  /**
   * GL function to set the line width.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glLineWidth.xml">glLineWidth()
   * OpenGL ES manpage</a>
   */
  public void lineWidth(double width) {
    this.pImpl_.lineWidth(width);
  }
  /**
   * GL function to link a program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glLinkProgram.xml">glLinkProgram()
   * OpenGL ES manpage</a>
   */
  public void linkProgram(WGLWidget.Program program) {
    this.pImpl_.linkProgram(program);
  }
  /**
   * GL function to set the pixel storage mode.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glPixelStorei.xml">glPixelStorei()
   * OpenGL ES manpage</a>
   */
  public void pixelStorei(WGLWidget.GLenum pname, int param) {
    this.pImpl_.pixelStorei(pname, param);
  }
  /**
   * GL function to apply modifications to Z values.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glPolygonOffset.xml">glPolygonOffset()
   * OpenGL ES manpage</a>
   */
  public void polygonOffset(double factor, double units) {
    this.pImpl_.polygonOffset(factor, units);
  }
  /**
   * GL function to allocate the appropriate amount of memory for a render buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glRenderbufferStorage.xml">glSampleCoverage()
   * OpenGL ES manpage</a>
   */
  public void renderbufferStorage(
      WGLWidget.GLenum target, WGLWidget.GLenum internalformat, int width, int height) {
    this.pImpl_.renderbufferStorage(target, internalformat, width, height);
  }
  /**
   * GL function to set multisample parameters.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glSampleCoverage.xml">glSampleCoverage()
   * OpenGL ES manpage</a>
   */
  public void sampleCoverage(double value, boolean invert) {
    this.pImpl_.sampleCoverage(value, invert);
  }
  /**
   * GL function to define the scissor box.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glScissor.xml">glScissor()
   * OpenGL ES manpage</a>
   */
  public void scissor(int x, int y, int width, int height) {
    this.pImpl_.scissor(x, y, width, height);
  }
  /**
   * GL function to set a shader&apos;s source code.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glShaderSource.xml">glShaderSource()
   * OpenGL ES manpage</a>
   */
  public void shaderSource(WGLWidget.Shader shader, final String src) {
    this.pImpl_.shaderSource(shader, src);
  }
  /**
   * GL function to set stencil test parameters.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glStencilFunc.xml">glStencilFunc()
   * OpenGL ES manpage</a>
   */
  public void stencilFunc(WGLWidget.GLenum func, int ref, int mask) {
    this.pImpl_.stencilFunc(func, ref, mask);
  }
  /**
   * GL function to set stencil test parameters for front and/or back stencils.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glStencilFuncSeparate.xml">glStencilFuncSeparate()
   * OpenGL ES manpage</a>
   */
  public void stencilFuncSeparate(WGLWidget.GLenum face, WGLWidget.GLenum func, int ref, int mask) {
    this.pImpl_.stencilFuncSeparate(face, func, ref, mask);
  }
  /**
   * GL function to control which bits are to be written in the stencil buffer.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glStencilMask.xml">glStencilMask()
   * OpenGL ES manpage</a>
   */
  public void stencilMask(int mask) {
    this.pImpl_.stencilMask(mask);
  }
  /**
   * GL function to control which bits are written to the front and/or back stencil buffers.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glStencilMaskSeparate.xml">glStencilMaskSeparate()
   * OpenGL ES manpage</a>
   */
  public void stencilMaskSeparate(WGLWidget.GLenum face, int mask) {
    this.pImpl_.stencilMaskSeparate(face, mask);
  }
  /**
   * GL function to set stencil test actions.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glStencilOp.xml">glStencilOp()
   * OpenGL ES manpage</a>
   */
  public void stencilOp(WGLWidget.GLenum fail, WGLWidget.GLenum zfail, WGLWidget.GLenum zpass) {
    this.pImpl_.stencilOp(fail, zfail, zpass);
  }
  /**
   * GL function to set front and/or back stencil test actions separately.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glStencilOpSeparate.xml">glStencilOpSeparate()
   * OpenGL ES manpage</a>
   */
  public void stencilOpSeparate(
      WGLWidget.GLenum face,
      WGLWidget.GLenum fail,
      WGLWidget.GLenum zfail,
      WGLWidget.GLenum zpass) {
    this.pImpl_.stencilOpSeparate(face, fail, zfail, zpass);
  }
  /**
   * GL function to reserve space for a 2D texture, without specifying its contents.
   *
   * <p>This corresponds to calling the WebGL function void texImage2D(GLenum target, GLint level,
   * GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type,
   * ArrayBufferView pixels) with null as last parameters. The value of &apos;type&apos; is then of
   * no importance and is therefore omitted from this function.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glTexImage2D.xml">glTexImage2D()
   * OpenGL ES manpage</a>
   */
  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      int width,
      int height,
      int border,
      WGLWidget.GLenum format) {
    this.pImpl_.texImage2D(target, level, internalformat, width, height, border, format);
  }
  /**
   * GL function to load a 2D texture from a {@link WImage}.
   *
   * <p>Note: {@link WImage} must be loaded before this function is executed.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glTexImage2D.xml">glTexImage2D()
   * OpenGL ES manpage</a>
   */
  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WImage image) {
    this.pImpl_.texImage2D(target, level, internalformat, format, type, image);
  }
  /**
   * GL function to load a 2D texture from a {@link WVideo}.
   *
   * <p>Note: the video must be loaded prior to calling this function. The current frame is used as
   * texture image.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glTexImage2D.xml">glTexImage2D()
   * OpenGL ES manpage</a>
   */
  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WVideo video) {
    this.pImpl_.texImage2D(target, level, internalformat, format, type, video);
  }
  /**
   * GL function to load a 2D texture from a file.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glTexImage2D.xml">glTexImage2D()
   * OpenGL ES manpage</a>
   */
  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      String imgFilename) {
    this.pImpl_.texImage2D(target, level, internalformat, format, type, imgFilename);
  }
  /**
   * GL function to load a 2D texture from a {@link WPaintDevice}.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glTexImage2D.xml">glTexImage2D()
   * OpenGL ES manpage</a>
   *
   * <p>
   *
   * @see WGLWidget#createPaintDevice(WLength width, WLength height)
   */
  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WPaintDevice paintdevice) {
    this.pImpl_.texImage2D(target, level, internalformat, format, type, paintdevice);
  }
  /**
   * GL function to load a 2D texture loaded with {@link WGLWidget#createTextureAndLoad(String url)
   * createTextureAndLoad()}
   *
   * <p>This function must only be used for textures created with {@link
   * WGLWidget#createTextureAndLoad(String url) createTextureAndLoad()}
   *
   * <p>Note: the {@link WGLWidget} implementation will delay rendering until all textures created
   * with {@link WGLWidget#createTextureAndLoad(String url) createTextureAndLoad()} are loaded in
   * the browser.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glTexImage2D.xml">glTexImage2D()
   * OpenGL ES manpage</a>
   */
  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WGLWidget.Texture texture) {
    this.pImpl_.texImage2D(target, level, internalformat, format, type, texture);
  }
  /**
   * GL function to set texture parameters.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glTexParameter.xml">glTexParameter()
   * OpenGL ES manpage</a>
   */
  public void texParameteri(
      WGLWidget.GLenum target, WGLWidget.GLenum pname, WGLWidget.GLenum param) {
    this.pImpl_.texParameteri(target, pname, param);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform1f(final WGLWidget.UniformLocation location, double x) {
    this.pImpl_.uniform1f(location, x);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform1fv(final WGLWidget.UniformLocation location, float[] value) {
    this.pImpl_.uniform1fv(location, value);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform1fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.pImpl_.uniform1fv(location, v);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform1i(final WGLWidget.UniformLocation location, int x) {
    this.pImpl_.uniform1i(location, x);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform1iv(final WGLWidget.UniformLocation location, int[] value) {
    this.pImpl_.uniform1iv(location, value);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform2f(final WGLWidget.UniformLocation location, double x, double y) {
    this.pImpl_.uniform2f(location, x, y);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform2fv(final WGLWidget.UniformLocation location, float[] value) {
    this.pImpl_.uniform2fv(location, value);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform2fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.pImpl_.uniform2fv(location, v);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform2i(final WGLWidget.UniformLocation location, int x, int y) {
    this.pImpl_.uniform2i(location, x, y);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform2iv(final WGLWidget.UniformLocation location, int[] value) {
    this.pImpl_.uniform2iv(location, value);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform3f(final WGLWidget.UniformLocation location, double x, double y, double z) {
    this.pImpl_.uniform3f(location, x, y, z);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform3fv(final WGLWidget.UniformLocation location, float[] value) {
    this.pImpl_.uniform3fv(location, value);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform3fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.pImpl_.uniform3fv(location, v);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform3i(final WGLWidget.UniformLocation location, int x, int y, int z) {
    this.pImpl_.uniform3i(location, x, y, z);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform3iv(final WGLWidget.UniformLocation location, int[] value) {
    this.pImpl_.uniform3iv(location, value);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform4f(
      final WGLWidget.UniformLocation location, double x, double y, double z, double w) {
    this.pImpl_.uniform4f(location, x, y, z, w);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform4fv(final WGLWidget.UniformLocation location, float[] value) {
    this.pImpl_.uniform4fv(location, value);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform4fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.pImpl_.uniform4fv(location, v);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform4i(final WGLWidget.UniformLocation location, int x, int y, int z, int w) {
    this.pImpl_.uniform4i(location, x, y, z, w);
  }
  /**
   * GL function to set the value of a uniform variable of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniform4iv(final WGLWidget.UniformLocation location, int[] value) {
    this.pImpl_.uniform4iv(location, value);
  }
  /**
   * GL function to set the value of a uniform matrix of the current program.
   *
   * <p>Attention: The OpenGL ES specification states that transpose MUST be false.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniformMatrix2fv(
      final WGLWidget.UniformLocation location, boolean transpose, double[] value) {
    this.pImpl_.uniformMatrix2fv(location, transpose, value);
  }
  /**
   * GL function to set the value of a uniform matrix of the current program.
   *
   * <p>This function renders the matrix in the proper row/column order.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniformMatrix2(final WGLWidget.UniformLocation location, final Matrix2f m) {
    this.pImpl_.uniformMatrix2(location, m);
  }
  /**
   * GL function to set the value of a uniform matrix of the current program.
   *
   * <p>Attention: The OpenGL ES specification states that transpose MUST be false.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniformMatrix3fv(
      final WGLWidget.UniformLocation location, boolean transpose, double[] value) {
    this.pImpl_.uniformMatrix3fv(location, transpose, value);
  }
  /**
   * GL function to set the value of a uniform matrix of the current program.
   *
   * <p>This function renders the matrix in the proper row/column order.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniformMatrix3(
      final WGLWidget.UniformLocation location, final javax.vecmath.Matrix3f m) {
    this.pImpl_.uniformMatrix3(location, m);
  }
  /**
   * GL function to set the value of a uniform matrix of the current program.
   *
   * <p>Attention: The OpenGL ES specification states that transpose MUST be false.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniformMatrix4fv(
      final WGLWidget.UniformLocation location, boolean transpose, double[] value) {
    this.pImpl_.uniformMatrix4fv(location, transpose, value);
  }
  /**
   * GL function to set the value of a uniform matrix of the current program.
   *
   * <p>This function renders the matrix in the proper row/column order.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniformMatrix4(
      final WGLWidget.UniformLocation location, final javax.vecmath.Matrix4f m) {
    this.pImpl_.uniformMatrix4(location, m);
  }
  /**
   * GL function to set the value of a uniform matrix of the current program.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUniform.xml">glUniform()
   * OpenGL ES manpage</a>
   */
  public void uniformMatrix4(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptMatrix4x4 jsm) {
    if (!jsm.isInitialized()) {
      throw new WException("JavaScriptMatrix4x4: matrix not initialized");
    }
    this.pImpl_.uniformMatrix4(location, jsm);
  }
  /**
   * GL function to set the current active shader program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glUseProgram.xml">glUseProgram()
   * OpenGL ES manpage</a>
   */
  public void useProgram(WGLWidget.Program program) {
    this.pImpl_.useProgram(program);
  }
  /**
   * GL function to validate a program.
   *
   * <p>implementation note: there is currently not yet a method to read out the validation result.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glValidateProgram.xml">glValidateProgram()
   * OpenGL ES manpage</a>
   */
  public void validateProgram(WGLWidget.Program program) {
    this.pImpl_.validateProgram(program);
  }
  /**
   * GL function to set the value of an attribute of the current program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttrib.xml">glVertexAttrib()
   * OpenGL ES manpage</a>
   */
  public void vertexAttrib1f(WGLWidget.AttribLocation location, double x) {
    this.pImpl_.vertexAttrib1f(location, x);
  }
  /**
   * GL function to set the value of an attribute of the current program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttrib.xml">glVertexAttrib()
   * OpenGL ES manpage</a>
   */
  public void vertexAttrib2f(WGLWidget.AttribLocation location, double x, double y) {
    this.pImpl_.vertexAttrib2f(location, x, y);
  }
  /**
   * GL function to set the value of an attribute of the current program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttrib.xml">glVertexAttrib()
   * OpenGL ES manpage</a>
   */
  public void vertexAttrib2fv(WGLWidget.AttribLocation location, float[] values) {
    this.vertexAttrib2f(location, values[0], values[1]);
  }
  /**
   * GL function to set the value of an attribute of the current program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttrib.xml">glVertexAttrib()
   * OpenGL ES manpage</a>
   */
  public void vertexAttrib3f(WGLWidget.AttribLocation location, double x, double y, double z) {
    this.pImpl_.vertexAttrib3f(location, x, y, z);
  }
  /**
   * GL function to set the value of an attribute of the current program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttrib.xml">glVertexAttrib()
   * OpenGL ES manpage</a>
   */
  public void vertexAttrib3fv(WGLWidget.AttribLocation location, float[] values) {
    this.vertexAttrib3f(location, values[0], values[1], values[2]);
  }
  /**
   * GL function to set the value of an attribute of the current program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttrib.xml">glVertexAttrib()
   * OpenGL ES manpage</a>
   */
  public void vertexAttrib4f(
      WGLWidget.AttribLocation location, double x, double y, double z, double w) {
    this.pImpl_.vertexAttrib4f(location, x, y, z, w);
  }
  /**
   * GL function to set the value of an attribute of the current program.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttrib.xml">glVertexAttrib()
   * OpenGL ES manpage</a>
   */
  public void vertexAttrib4fv(WGLWidget.AttribLocation location, float[] values) {
    this.vertexAttrib4f(location, values[0], values[1], values[2], values[3]);
  }
  /**
   * GL function to bind a VBO to an attribute.
   *
   * <p>This function links the given attribute to the VBO currently bound to the ARRAY_BUFFER
   * target.
   *
   * <p>The size parameter specifies the number of components per attribute (1 to 4). The type
   * parameter is also used to determine the size of each component.
   *
   * <p>The size of a float is 8 bytes.
   *
   * <p>In {@link WGLWidget}, the size of an int is 4 bytes.
   *
   * <p>The stride is in bytes.
   *
   * <p>The maximum stride is 255.
   *
   * <p><a
   * href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glVertexAttribPointer.xml">glVertexAttribPointer()
   * OpenGL ES manpage</a>
   */
  public void vertexAttribPointer(
      WGLWidget.AttribLocation location,
      int size,
      WGLWidget.GLenum type,
      boolean normalized,
      int stride,
      int offset) {
    this.pImpl_.vertexAttribPointer(location, size, type, normalized, stride, offset);
  }
  /**
   * GL function to set the viewport.
   *
   * <p><a href="http://www.khronos.org/opengles/sdk/2.0/docs/man/xhtml/glViewport.xml">glViewport()
   * OpenGL ES manpage</a>
   */
  public void viewport(int x, int y, int width, int height) {
    this.pImpl_.viewport(x, y, width, height);
  }
  /**
   * Create a matrix that can be manipulated in client-side JavaScript.
   *
   * <p>This is a shorthand for creating a {@link JavaScriptMatrix4x4}, then adding it to a {@link
   * WGLWidget} with addJavaScriptMatrix4, and initializing it with initJavaScriptMatrix4.
   *
   * <p>This method should only be called in {@link WGLWidget#initializeGL() initializeGL()}, {@link
   * WGLWidget#updateGL() updateGL()} or {@link WGLWidget#resizeGL(int width, int height)
   * resizeGL()}.
   */
  public WGLWidget.JavaScriptMatrix4x4 createJavaScriptMatrix4() {
    WGLWidget.JavaScriptMatrix4x4 mat = new WGLWidget.JavaScriptMatrix4x4();
    this.addJavaScriptMatrix4(mat);
    this.initJavaScriptMatrix4(mat);
    return mat;
  }
  /**
   * Register a matrix with this {@link WGLWidget}.
   *
   * <p>You can call this outside of {@link WGLWidget#resizeGL(int width, int height) resizeGL()},
   * {@link WGLWidget#paintGL() paintGL()}, {@link WGLWidget#updateGL() updateGL()} or {@link
   * WGLWidget#initializeGL() initializeGL()} methods. After a {@link JavaScriptMatrix4x4} is added
   * to a {@link WGLWidget}, its {@link WWidget#getJsRef()} becomes valid, and can be used in a
   * {@link JSlot}, for example.
   */
  public void addJavaScriptMatrix4(final WGLWidget.JavaScriptMatrix4x4 mat) {
    if (mat.hasContext()) {
      throw new WException("The given matrix is already associated with a WGLWidget!");
    }
    mat.assignToContext(this.jsValues_++, this);
    this.jsMatrixList_.add(
        new WGLWidget.jsMatrixMap(
            mat.getId(),
            new javax.vecmath.Matrix4f(
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f)));
  }
  /**
   * Initialize the client-side JavaScript for the given {@link JavaScriptMatrix4x4}.
   *
   * <p>If the given matrix is not associated with a widget yet, it will be added to this widget.
   *
   * <p>If the given matrix has already been added to a {@link WGLWidget}, then this {@link
   * WGLWidget} should be the same as the one you call initJavaScriptMatrix4 on.
   *
   * <p>This method should only be called in {@link WGLWidget#initializeGL() initializeGL()}, {@link
   * WGLWidget#updateGL() updateGL()} or {@link WGLWidget#resizeGL(int width, int height)
   * resizeGL()}.
   */
  public void initJavaScriptMatrix4(final WGLWidget.JavaScriptMatrix4x4 mat) {
    this.pImpl_.initJavaScriptMatrix4(mat);
  }
  /**
   * Set the value of a client-side JavaScript matrix created by createJavaScriptMatrix4x4()
   *
   * <p>This method should only be called in {@link WGLWidget#initializeGL() initializeGL()}, {@link
   * WGLWidget#updateGL() updateGL()} or {@link WGLWidget#resizeGL(int width, int height)
   * resizeGL()}.
   */
  public void setJavaScriptMatrix4(
      final WGLWidget.JavaScriptMatrix4x4 jsm, final javax.vecmath.Matrix4f m) {
    if (!jsm.isInitialized()) {
      throw new WException("JavaScriptMatrix4x4: matrix not initialized");
    }
    if (jsm.hasOperations()) {
      throw new WException("JavaScriptMatrix4x4: matrix was already operated on");
    }
    this.valueChanged_ = true;
    for (int i = 0; i < this.jsMatrixList_.size(); i++) {
      if (this.jsMatrixList_.get(i).id == jsm.getId()) {
        this.jsMatrixList_.get(i).serverSideCopy = m;
      }
    }
    this.pImpl_.setJavaScriptMatrix4(jsm, m);
  }
  /**
   * Create a vector of a certain length that can be manipulated in client-side JavaScript.
   *
   * <p>This is a shorthand for creating a {@link JavaScriptVector}, then adding it to a {@link
   * WGLWidget} with addJavaScriptVector, and initializing it with initJavaScriptVector.
   *
   * <p>This method should only be called in {@link WGLWidget#initializeGL() initializeGL()}, {@link
   * WGLWidget#updateGL() updateGL()} or {@link WGLWidget#resizeGL(int width, int height)
   * resizeGL()}.
   */
  public WGLWidget.JavaScriptVector createJavaScriptVector(int length) {
    WGLWidget.JavaScriptVector vec = new WGLWidget.JavaScriptVector(length);
    this.addJavaScriptVector(vec);
    this.initJavaScriptVector(vec);
    return vec;
  }
  /**
   * Register a vector with this {@link WGLWidget}.
   *
   * <p>You can call this outside of {@link WGLWidget#resizeGL(int width, int height) resizeGL()},
   * {@link WGLWidget#paintGL() paintGL()}, {@link WGLWidget#updateGL() updateGL()} or {@link
   * WGLWidget#initializeGL() initializeGL()} methods. After a {@link JavaScriptVector} is added to
   * a {@link WGLWidget}, its {@link WWidget#getJsRef()} becomes valid, and can be used in a {@link
   * JSlot}, for example.
   */
  public void addJavaScriptVector(final WGLWidget.JavaScriptVector vec) {
    if (vec.hasContext()) {
      throw new WException("The given matrix is already associated with a WGLWidget!");
    }
    vec.assignToContext(this.jsValues_++, this);
    List<Float> values = new ArrayList<Float>();
    for (int i = 0; i < vec.getLength(); ++i) {
      values.add(0.0f);
    }
    this.jsVectorList_.add(new WGLWidget.jsVectorMap(vec.getId(), values));
  }
  /**
   * Initialize the client-side JavaScript for the given {@link JavaScriptVector}.
   *
   * <p>If the given vector is not associated with a widget yet, it will be added to this widget.
   *
   * <p>If the given vector has already been added to a {@link WGLWidget}, then this {@link
   * WGLWidget} should be the same as the one you call initJavaScriptVector on.
   *
   * <p>This method should only be called in {@link WGLWidget#initializeGL() initializeGL()}, {@link
   * WGLWidget#updateGL() updateGL()} or {@link WGLWidget#resizeGL(int width, int height)
   * resizeGL()}.
   */
  public void initJavaScriptVector(final WGLWidget.JavaScriptVector vec) {
    this.pImpl_.initJavaScriptVector(vec);
  }
  /**
   * Set the value of a client-side JavaScript vector created by {@link
   * WGLWidget#createJavaScriptVector(int length) createJavaScriptVector()}
   *
   * <p>This method should only be called in {@link WGLWidget#initializeGL() initializeGL()}, {@link
   * WGLWidget#updateGL() updateGL()} or {@link WGLWidget#resizeGL(int width, int height)
   * resizeGL()}.
   */
  public void setJavaScriptVector(final WGLWidget.JavaScriptVector jsv, final List<Float> v) {
    if (!jsv.isInitialized()) {
      throw new WException("JavaScriptVector: vector not initialized");
    }
    this.valueChanged_ = true;
    for (int i = 0; i < this.jsVectorList_.size(); i++) {
      if (this.jsVectorList_.get(i).id == jsv.getId()) {
        Utils.copyList(v, this.jsVectorList_.get(i).serverSideCopy);
      }
    }
    this.pImpl_.setJavaScriptVector(jsv, v);
  }
  /**
   * Set a custom mouse handler based on the given JavaScript code.
   *
   * <p>The handler code should be JavaScript code that produces an object when evaluated.
   *
   * <p>A mouse handler is an object that can implement one or more of the following functions:
   *
   * <p>
   *
   * <ul>
   *   <li><b>setTarget(target)</b>: This is called immediately when the mouse handler is added with
   *       an object that uniquely identifies the {@link WGLWidget}, and a {@link
   *       WGLWidget#paintGL() paintGL()} method.
   *   <li><b>mouseDown(o, event)</b>: To handle the <code>mousedown</code> event. <code>o</code> is
   *       the <code>&lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code>
   *       (server-side rendering) element corresponding to this {@link WGLWidget}. <code>event
   *       </code> is the <code>MouseEvent</code>.
   *   <li><b>mouseUp(o, event)</b>: To handle the <code>mouseup</code> event. <code>o</code> is the
   *       <code>&lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code>
   *       (server-side rendering) element corresponding to this {@link WGLWidget}. <code>event
   *       </code> is the <code>MouseEvent</code>.
   *   <li><b>mouseDrag(o, event)</b>: Called when the mouse is dragged. <code>o</code> is the
   *       <code>&lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code>
   *       (server-side rendering) element corresponding to this {@link WGLWidget}. <code>event
   *       </code> is the <code>MouseEvent</code>.
   *   <li><b>mouseMove(o, event)</b>: Called when the mouse is moved. <code>o</code> is the <code>
   *       &lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code> (server-side
   *       rendering) element corresponding to this {@link WGLWidget}. <code>event</code> is the
   *       <code>MouseEvent</code>.
   *   <li><b>mouseWheel(o, event)</b>: Called when the mouse wheel is used. <code>o</code> is the
   *       <code>&lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code>
   *       (server-side rendering) element corresponding to this {@link WGLWidget}. <code>event
   *       </code> is the <code>MouseEvent</code>.
   *   <li><b>touchStart(o, event)</b>: To handle the <code>touchstart</code> event. <code>o</code>
   *       is the <code>&lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code>
   *       (server-side rendering) element corresponding to this {@link WGLWidget}. <code>event
   *       </code> is the <code>TouchEvent</code>.
   *   <li><b>touchEnd(o, event)</b>: To handle the <code>touchend</code> event. <code>o</code> is
   *       this <code>&lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code>
   *       (server-side rendering) element corresponding to this {@link WGLWidget}. <code>event
   *       </code> is the <code>TouchEvent</code>.
   *   <li><b>touchMoved(o, event)</b>: To handle the <code>touchmove</code> event. <code>o</code>
   *       is this <code>&lt;canvas&gt;</code> (client-side rendering) or <code>&lt;img&gt;</code>
   *       (server-side rendering) element corresponding to this {@link WGLWidget}. <code>event
   *       </code> is the <code>TouchEvent</code>.
   * </ul>
   *
   * <p>For example, if we wanted to scale some object when we scroll, we could create a {@link
   * JavaScriptMatrix4x4} called <code>transform_:</code>
   *
   * <p>
   *
   * <pre>{@code
   * private JavaScriptMatrix4x4 transform_;
   *
   * }</pre>
   *
   * <p>We can add this in the constructor:
   *
   * <p>
   *
   * <pre>{@code
   * transform_ = new JavaScriptMatrix4x4();
   * addJavaScriptMatrix4(transform_);
   *
   * }</pre>
   *
   * <p>Then, in {@link WGLWidget#initializeGL() initializeGL()}, we can initialize it and set the
   * value:
   *
   * <p>
   *
   * <pre>{@code
   * initJavaScriptMatrix4(transform_);
   * setJavaScriptMatrix4(transform_, new WMatrix4x4()); // Set to identity matrix
   *
   * }</pre>
   *
   * <p>Then, still in {@link WGLWidget#initializeGL() initializeGL()}, we can set a mouse handler
   * as such:
   *
   * <p>
   *
   * <pre>{@code
   * setClientSideMouseHandler("(function(){") +
   * "var MouseHandler = function(transform) {" +
   * "var target = null;" +
   * "this.setTarget = function(newTarget) {" +
   * "target = newTarget;" +
   * "};" +
   * "this.mouseWheel = function(o, event) {" +
   * "var fix = jQuery.event.fix(event);" +
   * "fix.preventDefault();" +
   * "fix.stopPropagation();" +
   * "var d = wheelDelta(event);" +
   * "var s = Math.pow(1.2, d);" +
   * "transform[0] *= s;" + // Scale X
   * "transform[5] *= s;" + // Scale Y
   * "transform[10] *= s;" + // Scale Z
   * "target.paintGL();" + // Repaint
   * "};" +
   * "function wheelDelta(e) {" +
   * "var delta = 0;" +
   * "if (e.wheelDelta) {" +
   * "delta = e.wheelDelta > 0 ? 1 : -1;" +
   * "} else if (e.detail) {" +
   * "delta = e.detail < 0 ? 1 : -1;" +
   * "}" +
   * "return delta;" +
   * "}" +
   * "};" +
   * "return new MouseHandler(" + transform_.jsRef() + ");" +
   * "})()");
   *
   * }</pre>
   *
   * <p>All that&apos;s left to do then is to use this transform somewhere as a uniform variable,
   * see {@link WGLWidget#getUniformLocation(WGLWidget.Program program, String location)
   * getUniformLocation()} and {@link WGLWidget#uniformMatrix4(WGLWidget.UniformLocation location,
   * javax.vecmath.Matrix4f m) uniformMatrix4()}.
   */
  public void setClientSideMouseHandler(final String handlerCode) {
    this.pImpl_.setClientSideMouseHandler(handlerCode);
  }
  /**
   * Add a mouse handler to the widget that looks at a given point.
   *
   * <p>This will allow a user to change client-side matrix m with the mouse. M is a model
   * transformation matrix, representing the viewpoint of the camera.
   *
   * <p>Through mouse operations, the camera can be changed by the user, but (lX, lY, lZ) will
   * always be at the center of the display, (uX, uY, uZ) is considered to be the up direction, and
   * the distance of the camera to (lX, lY, lZ) will never change.
   *
   * <p>Pressing the left mouse button and moving the mouse left/right will rotate the camera around
   * the up (uX, uY, uZ) direction. Moving up/down will tilt the camera (causing it to move up/down
   * to keep the lookpoint centered). The scroll wheel simulates zooming by scaling the scene.
   *
   * <p>pitchRate and yawRate control how much the camera will move per mouse pixel.
   *
   * <p>Usually this method is called after setting a camera transformation with a client-side
   * matrix in {@link WGLWidget#initializeGL() initializeGL()}. However, this function may also be
   * called from outside the intializeGL()/paintGL()/updateGL() methods (but not before m was
   * initialized).
   */
  public void setClientSideLookAtHandler(
      final WGLWidget.JavaScriptMatrix4x4 m,
      double centerX,
      double centerY,
      double centerZ,
      double uX,
      double uY,
      double uZ,
      double pitchRate,
      double yawRate) {
    this.pImpl_.setClientSideLookAtHandler(
        m, centerX, centerY, centerZ, uX, uY, uZ, pitchRate, yawRate);
  }
  /**
   * Add a mouse handler to the widget that allows &apos;walking&apos; in the scene.
   *
   * <p>This will allow a user to change client-side matrix m with the mouse. M is a model
   * transformation matrix, representing the viewpoint of the camera.
   *
   * <p>Through mouse operations, the camera can be changed by the user, as if he is walking around
   * on a plane.
   *
   * <p>Pressing the left mouse button and moving the mouse left/right will rotate the camera around
   * Y axis. Moving the mouse up/down will move the camera in the Z direction (walking
   * forward/backward). centered).
   *
   * <p>frontStep and rotStep control how much the camera will move per mouse pixel.
   */
  public void setClientSideWalkHandler(
      final WGLWidget.JavaScriptMatrix4x4 m, double frontStep, double rotStep) {
    this.pImpl_.setClientSideWalkHandler(m, frontStep, rotStep);
  }
  /**
   * Sets the content to be displayed when WebGL is not available.
   *
   * <p>If JWt cannot create a working WebGL context, this content will be shown to the user. This
   * may be a text explanation, or a pre-rendered image, or a video, a flash movie, ...
   *
   * <p>The default is a widget that explains to the user that he has no WebGL support.
   */
  public void setAlternativeContent(WWidget alternative) {
    {
      WWidget oldWidget = this.alternative_;
      this.alternative_ = alternative;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.alternative_);
        if (toRemove != null) toRemove.remove();
      }
    }
  }
  /**
   * A JavaScript slot that repaints the widget when triggered.
   *
   * <p>This is useful for client-side initiated repaints. You may e.g. use this if you write your
   * own client-side mouse handler, or if you updated a texture, or if you&apos;re playing a video
   * texture.
   */
  public JSlot getRepaintSlot() {
    return this.repaintSlot_;
  }
  /**
   * enable client-side error messages (read detailed doc!)
   *
   * <p>This option will add client-side code to check the result of every WebGL call, and will
   * popup an error dialog if a WebGL call returned an error. The JavaScript then invokes the
   * client-side debugger. This code is intended to test your application, and should not be used in
   * production.
   */
  public void enableClientErrorChecks(boolean enable) {
    this.pImpl_.enableClientErrorChecks(enable);
  }
  /**
   * enable client-side error messages (read detailed doc!)
   *
   * <p>Calls {@link #enableClientErrorChecks(boolean enable) enableClientErrorChecks(true)}
   */
  public final void enableClientErrorChecks() {
    enableClientErrorChecks(true);
  }
  /**
   * Inject JavaScript into the current js-stream.
   *
   * <p>Careful: this method directly puts the given jsString into the JavaScript stream, whatever
   * state it current has. For example, if called in initGL(), it will put the jsString into the
   * client-side initGL() code.
   */
  public void injectJS(final String jsString) {
    this.pImpl_.injectJS(jsString);
  }

  public void webglNotAvailable() {
    System.out.append("WebGL Not available in client!\n");
    this.webGlNotAvailable_ = true;
  }

  DomElementType getDomElementType() {
    if (ObjectUtils.cast(this.pImpl_, WClientGLWidget.class) != null) {
      return DomElementType.CANVAS;
    } else {
      return DomElementType.IMG;
    }
  }

  protected DomElement createDomElement(WApplication app) {
    DomElement result = null;
    if (!(this.pImpl_ != null)) {
      result = DomElement.createNew(DomElementType.DIV);
      result.addChild(this.alternative_.createSDomElement(app));
      this.webGlNotAvailable_ = true;
    } else {
      result = DomElement.createNew(this.getDomElementType());
      this.repaintGL(EnumSet.of(GLClientSideRenderer.PAINT_GL, GLClientSideRenderer.RESIZE_GL));
    }
    this.setId(result, app);
    this.updateDom(result, true);
    return result;
  }

  protected void getDomChanges(final List<DomElement> result, WApplication app) {
    super.getDomChanges(result, app);
  }

  void updateDom(final DomElement element, boolean all) {
    if (all || this.valueChanged_) {
      this.valueChanged_ = false;
    }
    if (this.webGlNotAvailable_) {
      return;
    }
    DomElement el = element;
    this.pImpl_.updateDom(el, all);
    super.updateDom(element, all);
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (flags.contains(RenderFlag.Full)) {
      if (!(this.pImpl_ != null)) {
        if (this.renderOptions_.contains(GLRenderOption.ClientSide)
            && WApplication.getInstance().getEnvironment().hasWebGL()) {
          this.pImpl_ = new WClientGLWidget(this);
        } else {
          if (!EnumUtils.mask(this.renderOptions_, GLRenderOption.ServerSide).isEmpty()) {
            this.pImpl_ = new WServerGLWidget(this);
          } else {
            this.pImpl_ = null;
          }
        }
      }
      if (this.pImpl_ != null && !this.getWidth().isAuto() && !this.getHeight().isAuto()) {
        this.layoutSizeChanged((int) this.getWidth().toPixels(), (int) this.getHeight().toPixels());
      }
      this.defineJavaScript();
    }
    if (this.pImpl_ != null) {
      this.pImpl_.render(this.getJsRef(), flags);
    }
    super.render(flags);
  }

  String renderRemoveJs(boolean recursive) {
    if (this.webGlNotAvailable_) {
      return this.alternative_.renderRemoveJs(recursive);
    } else {
      return super.renderRemoveJs(recursive);
    }
  }

  protected void layoutSizeChanged(int width, int height) {
    this.pImpl_.layoutSizeChanged(width, height);
    this.repaintGL(EnumSet.of(GLClientSideRenderer.RESIZE_GL));
  }

  protected void setFormData(final WObject.FormData formData) {
    if (this.valueChanged_) {
      return;
    }
    String[] parVals = formData.values;
    if ((parVals.length == 0)) {
      return;
    }
    if (parVals[0].equals("undefined")) {
      return;
    }
    List<String> matrices = new ArrayList<String>();
    StringUtils.split(matrices, parVals[0], ";", false);
    for (int i = 0; i < matrices.size(); i++) {
      if (matrices.get(i).equals("")) {
        break;
      }
      List<String> idAndData = new ArrayList<String>();
      StringUtils.split(idAndData, matrices.get(i), ":", false);
      int id = (int) StringUtils.asNumber(idAndData.get(0));
      int j = 0;
      for (j = 0; j < this.jsMatrixList_.size(); j++) {
        if (this.jsMatrixList_.get(j).id == id) {
          break;
        }
      }
      if (j == this.jsMatrixList_.size()) {
        for (j = 0; j < this.jsVectorList_.size(); j++) {
          if (this.jsVectorList_.get(j).id == id) {
            break;
          }
        }
        final List<Float> vec = this.jsVectorList_.get(j).serverSideCopy;
        List<String> mData = new ArrayList<String>();
        StringUtils.split(mData, idAndData.get(1), ",", false);
        for (int i1 = 0; i1 < vec.size(); i1++) {
          if (mData.get(i1).equals("Infinity")) {
            vec.set(i1, Float.POSITIVE_INFINITY);
          } else {
            if (mData.get(i1).equals("-Infinity")) {
              vec.set(i1, -Float.POSITIVE_INFINITY);
            } else {
              vec.set(i1, Float.parseFloat(mData.get(i1)));
            }
          }
        }
      } else {
        final javax.vecmath.Matrix4f mat = this.jsMatrixList_.get(j).serverSideCopy;
        List<String> mData = new ArrayList<String>();
        StringUtils.split(mData, idAndData.get(1), ",", false);
        for (int i1 = 0; i1 < 4; i1++) {
          for (int i2 = 0; i2 < 4; i2++) {
            mat.setElement(i2, i1, Float.parseFloat(mData.get(i1 * 4 + i2)));
          }
        }
      }
    }
  }

  protected void contextRestored() {
    this.restoringContext_ = true;
    this.pImpl_.restoreContext(this.getJsRef());
    this.repaintGL(
        EnumSet.of(
            GLClientSideRenderer.UPDATE_GL,
            GLClientSideRenderer.RESIZE_GL,
            GLClientSideRenderer.PAINT_GL));
    this.restoringContext_ = false;
  }

  EnumSet<GLRenderOption> renderOptions_;
  private WAbstractGLImplementation pImpl_;

  static class jsMatrixMap {
    private static Logger logger = LoggerFactory.getLogger(jsMatrixMap.class);

    public int id;
    public javax.vecmath.Matrix4f serverSideCopy;

    public jsMatrixMap(int matId, final javax.vecmath.Matrix4f ssCopy) {
      this.id = matId;
      this.serverSideCopy = ssCopy;
    }
  }

  static class jsVectorMap {
    private static Logger logger = LoggerFactory.getLogger(jsVectorMap.class);

    public int id;
    public List<Float> serverSideCopy;

    public jsVectorMap(int vecId, final List<Float> ssCopy) {
      this.id = vecId;
      this.serverSideCopy = ssCopy;
    }
  }

  private List<WGLWidget.jsMatrixMap> jsMatrixList_;
  private List<WGLWidget.jsVectorMap> jsVectorList_;
  private int jsValues_;
  private StringWriter js_;
  private JSignal repaintSignal_;

  private String getGlObjJsRef() {
    return "(function(){var r = "
        + this.getJsRef()
        + ";var o = r ? r.wtObj : null;return o ? o : {ctx: null};})()";
  }

  private WWidget alternative_;
  private JSignal webglNotAvailable_;
  private boolean webGlNotAvailable_;
  private JSignal contextRestored_;
  private boolean restoringContext_;
  private boolean valueChanged_;
  private JSlot mouseWentDownSlot_;
  private JSlot mouseWentUpSlot_;
  private JSlot mouseDraggedSlot_;
  private JSlot mouseMovedSlot_;
  private JSlot mouseWheelSlot_;
  private JSlot touchStarted_;
  private JSlot touchEnded_;
  private JSlot touchMoved_;
  private JSlot repaintSlot_;

  private void defineJavaScript() {
    WApplication app = WApplication.getInstance();
    if (this.renderOptions_.contains(GLRenderOption.ClientSide)
        && WApplication.getInstance().getEnvironment().hasWebGL()) {
      app.loadJavaScript("js/WPaintedWidget.js", wtjs11());
    }
    app.loadJavaScript("js/WtGlMatrix.js", wtjs2());
    app.loadJavaScript("js/WGLWidget.js", wtjs1());
  }

  static WJavaScriptPreamble wtjs10() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WPaintedWidget",
        "(function(t,n){n.wtObj=this;const e=this;this.imagePreloaders=[];this.images=[];this.canvas=document.getElementById(\"c\"+n.id);this.repaint=function(){};this.widget=n;this.cancelPreloaders=function(){for(const t of e.imagePreloaders)t.cancel();e.imagePreloaders=[]}})");
  }

  static WJavaScriptPreamble wtjs11() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptObject,
        "gfxUtils",
        "function(){const t=1024,n=4080;return new function(){const e=this;this.path_crisp=function(t){return t.map((function(t){return[Math.floor(t[0])+.5,Math.floor(t[1])+.5,t[2]]}))};this.transform_mult=function(t,n){if(2===n.length){const e=n[0],r=n[1];return[t[0]*e+t[2]*r+t[4],t[1]*e+t[3]*r+t[5]]}if(3===n.length){if(8===n[2]||9===n[2])return n.slice(0);const e=n[0],r=n[1];return[t[0]*e+t[2]*r+t[4],t[1]*e+t[3]*r+t[5],n[2]]}if(4===n.length){let r,i,o,s,a;const c=e.transform_mult(t,[n[0],n[1]]);r=c[0];o=c[0];i=c[1];s=c[1];for(let c=0;c<3;++c){a=e.transform_mult(t,0===c?[e.rect_left(n),e.rect_bottom(n)]:1===c?[e.rect_right(n),e.rect_top(n)]:[e.rect_right(n),e.rect_bottom(n)]);r=Math.min(r,a[0]);o=Math.max(o,a[0]);i=Math.min(i,a[1]);s=Math.max(s,a[1])}return[r,i,o-r,s-i]}return 6===n.length?[t[0]*n[0]+t[2]*n[1],t[1]*n[0]+t[3]*n[1],t[0]*n[2]+t[2]*n[3],t[1]*n[2]+t[3]*n[3],t[0]*n[4]+t[2]*n[5]+t[4],t[1]*n[4]+t[3]*n[5]+t[5]]:[]};this.transform_apply=function(t,n){const r=e.transform_mult;return n.map((function(n){return r(t,n)}))};this.transform_det=function(t){const n=t[0],e=t[2],r=t[1];return n*t[3]-r*e};this.transform_adjoint=function(t){const n=t[0],e=t[2],r=t[1],i=t[3],o=t[4],s=t[5];return[i,-e,-r,n,s*r-o*i,-(s*n-o*e)]};this.transform_inverted=function(t){const n=e.transform_det(t);if(0!==n){const r=e.transform_adjoint(t);return[r[0]/n,r[2]/n,r[1]/n,r[3]/n,r[4]/n,r[5]/n]}console.log(\"inverted(): oops, determinant == 0\");return t};this.transform_assign=function(t,n){t[0]=n[0];t[1]=n[1];t[2]=n[2];t[3]=n[3];t[4]=n[4];t[5]=n[5]};this.transform_equal=function(t,n){return t[0]===n[0]&&t[1]===n[1]&&t[2]===n[2]&&t[3]===n[3]&&t[4]===n[4]&&t[5]===n[5]};this.css_text=function(t){return\"rgba(\"+t[0]+\",\"+t[1]+\",\"+t[2]+\",\"+t[3]+\")\"};this.arcPosition=function(t,n,e,r,i){const o=-i/180*Math.PI;return[t+e*Math.cos(o),n+r*Math.sin(o)]};this.pnpoly=function(t,n){let r=!1,i=0,o=0;const s=t[0],a=t[1];let c,l;for(let t=0;t<n.length;++t){c=i;l=o;if(7===n[t][2]){const r=e.arcPosition(n[t][0],n[t][1],n[t+1][0],n[t+1][1],n[t+2][0]);c=r[0];l=r[1]}else if(9===n[t][2]){const r=e.arcPosition(n[t-2][0],n[t-2][1],n[t-1][0],n[t-1][1],n[t][0]+n[t][1]);c=r[0];l=r[1]}else if(8!==n[t][2]){c=n[t][0];l=n[t][1]}0!==n[t][2]&&o>a!=l>a&&s<(c-i)*(a-o)/(l-o)+i&&(r=!r);i=c;o=l}return r};this.rect_intersection=function(t,n){t=e.rect_normalized(t);n=e.rect_normalized(n);const r=e.rect_top,i=e.rect_bottom,o=e.rect_left,s=e.rect_right,a=Math.max(o(t),o(n)),c=Math.min(s(t),s(n)),l=Math.max(r(t),r(n));return[a,l,c-a,Math.min(i(t),i(n))-l]};this.drawRect=function(t,n,r,i){n=e.rect_normalized(n);const o=e.rect_top(n),s=e.rect_bottom(n),a=e.rect_left(n),c=e.rect_right(n),l=[[a,o,0],[c,o,1],[c,s,1],[a,s,1],[a,o,1]];e.drawPath(t,l,r,i,!1)};this.drawPath=function(t,n,r,i,o){let s=0,a=[],c=[],l=[];const f=1048576;function u(t){return t[0]}function h(t){return t[1]}function m(t){return t[2]}t.beginPath();n.length>0&&0!==m(n[0])&&t.moveTo(0,0);for(s=0;s<n.length;s++){const _=n[s];switch(m(_)){case 0:Math.abs(u(_))<=f&&Math.abs(h(_))<=f&&t.moveTo(u(_),h(_));break;case 1:!function(){const a=0===s?[0,0]:n[s-1];if(!r&&!o&&i&&(Math.abs(u(a))>f||Math.abs(h(a))>f||Math.abs(u(_))>f||Math.abs(h(_))>f)){!function(){const n=t.wtTransform?t.wtTransform:[1,0,0,1,0,0],r=e.transform_inverted(n),i=e.transform_mult(n,a),o=e.transform_mult(n,_),s=u(o)-u(i),c=h(o)-h(i),l=-50,f=t.canvas.width+50,m=-50,p=t.canvas.height+50;function g(t,n,e){return(e-t)/n}function d(t,n,e){return t+e*n}let b,w,P,M=null,T=null,y=null,x=null;if(u(i)<l&&u(o)>l){b=g(u(i),s,l);M=[l,d(h(i),c,b),b]}else if(u(i)>f&&u(o)<f){b=g(u(i),s,f);M=[f,d(h(i),c,b),b]}else{if(!(u(i)>l&&u(i)<f))return;M=[u(i),h(i),0]}if(h(i)<m&&h(o)>m){b=g(h(i),c,m);T=[d(u(i),s,b),m,b]}else if(h(i)>p&&h(o)<p){b=g(h(i),c,p);T=[d(u(i),s,b),p,b]}else{if(!(h(i)>m&&h(i)<p))return;T=[u(i),h(i),0]}w=M[2]>T[2]?[M[0],M[1]]:[T[0],T[1]];if(!(u(w)<l||u(w)>f||h(w)<m||h(w)>p)){if(u(i)<f&&u(o)>f){b=g(u(i),s,f);y=[f,d(h(i),c,b),b]}else if(u(i)>l&&u(o)<l){b=g(u(i),s,l);y=[l,d(h(i),c,b),b]}else{if(!(u(o)>l&&u(o)<f))return;y=[u(o),h(o),1]}if(h(i)<p&&h(o)>p){b=g(h(i),c,p);x=[d(u(i),s,b),p,b]}else if(h(i)>m&&h(o)<m){b=g(h(i),c,m);x=[d(u(i),s,b),m,b]}else{if(!(u(o)>m&&h(o)<p))return;x=[u(o),h(o),1]}P=y[2]<x[2]?[y[0],y[1]]:[x[0],x[1]];if(!(u(P)<l||u(P)>f||h(P)<m||h(P)>p)){w=e.transform_mult(r,w);P=e.transform_mult(r,P);t.moveTo(w[0],w[1]);t.lineTo(P[0],P[1])}}}();Math.abs(u(_))<=f&&Math.abs(h(_))<=f&&t.moveTo(u(_),h(_))}else t.lineTo(u(_),h(_))}();break;case 2:case 3:a.push(u(_),h(_));break;case 4:a.push(u(_),h(_));t.bezierCurveTo.apply(t,a);a=[];break;case 7:c.push(u(_),h(_));break;case 8:c.push(u(_));break;case 9:!function(){function n(t){const n=t%360;return n<0?n+360:n}function e(t){return t*Math.PI/180}const r=u(_),i=h(_),o=e(n(-r));let s;s=i>=360||i<=-360?o-2*Math.PI*(i>0?1:-1):e(n(-r-((a=i)>360?360:a<-360?-360:a)));var a;const l=i>0;c.push(o,s,l);t.arc.apply(t,c);c=[]}();break;case 5:l.push(u(_),h(_));break;case 6:l.push(u(_),h(_));t.quadraticCurveTo.apply(t,l);l=[]}}r&&t.fill();i&&t.stroke();o&&t.clip()};this.drawStencilAlongPath=function(t,n,r,i,o,s){function a(t){return t[1]}function c(t){return t[2]}for(let f=0;f<r.length;f++){const u=r[f];if((!s||!t.wtClipPath||e.pnpoly(u,e.transform_apply(t.wtClipPathTransform,t.wtClipPath)))&&(0===c(u)||1===c(u)||6===c(u)||4===c(u))){const r=e.transform_apply([1,0,0,1,(l=u,l[0]),a(u)],n);e.drawPath(t,r,i,o,!1)}}var l};this.drawText=function(r,i,o,s,a){if(a&&r.wtClipPath&&!e.pnpoly(a,e.transform_apply(r.wtClipPathTransform,r.wtClipPath)))return;const c=o&n;let l=null,f=null;switch(15&o){case 1:r.textAlign=\"left\";l=e.rect_left(i);break;case 2:r.textAlign=\"right\";l=e.rect_right(i);break;case 4:r.textAlign=\"center\";l=e.rect_center(i).x}switch(c){case 128:r.textBaseline=\"top\";f=e.rect_top(i);break;case t:r.textBaseline=\"bottom\";f=e.rect_bottom(i);break;case 512:r.textBaseline=\"middle\";f=e.rect_center(i).y}if(null===l||null===f)return;const u=r.fillStyle;r.fillStyle=r.strokeStyle;r.fillText(s,l,f);r.fillStyle=u};this.calcYOffset=function(n,e,r,i){return 512===i?-(e-1)*r/2+n*r:128===i?n*r:i===t?-(e-1-n)*r:0};this.drawTextOnPath=function(t,r,i,o,s,a,c,l,f){function u(t){return t[0]}function h(t){return t[1]}function m(t){return t[2]}const _=e.transform_apply(o,s);for(let o=0;o<s.length&&!(o>=r.length);o++){const p=s[o],g=_[o],d=r[o].split(\"\\n\");if(0===m(p)||1===m(p)||6===m(p)||4===m(p))if(0===a)for(let r=0;r<d.length;r++){const o=e.calcYOffset(r,d.length,c,l&n);e.drawText(t,[i[0]+u(g),i[1]+h(g)+o,i[2],i[3]],l,d[r],f?[u(g),h(g)]:null)}else{const r=a*Math.PI/180,o=Math.cos(-r),s=-Math.sin(-r),m=-s,_=o;t.save();t.transform(o,m,s,_,u(g),h(g));for(let r=0;r<d.length;r++){const o=e.calcYOffset(r,d.length,c,l&n);e.drawText(t,[i[0],i[1]+o,i[2],i[3]],l,d[r],f?[u(g),h(g)]:null)}t.restore()}}};this.setClipPath=function(t,n,r,i){if(i){t.setTransform.apply(t,r);e.drawPath(t,n,!1,!1,!0);t.setTransform(1,0,0,1,0,0)}t.wtClipPath=n;t.wtClipPathTransform=r};this.removeClipPath=function(t){delete t.wtClipPath;delete t.wtClipPathTransform};this.rect_top=function(t){return t[1]};this.rect_bottom=function(t){return t[1]+t[3]};this.rect_right=function(t){return t[0]+t[2]};this.rect_left=function(t){return t[0]};this.rect_topleft=function(t){return[t[0],t[1]]};this.rect_topright=function(t){return[t[0]+t[2],t[1]]};this.rect_bottomleft=function(t){return[t[0],t[1]+t[3]]};this.rect_bottomright=function(t){return[t[0]+t[2],t[1]+t[3]]};this.rect_center=function(t){return{x:(2*t[0]+t[2])/2,y:(2*t[1]+t[3])/2}};this.rect_normalized=function(t){let n,e,r,i;if(t[2]>0){n=t[0];r=t[2]}else{n=t[0]+t[2];r=-t[2]}if(t[3]>0){e=t[1];i=t[3]}else{e=t[1]+t[3];i=-t[3]}return[n,e,r,i]};this.drawImage=function(t,n,e,r,i){try{t.drawImage(n,r[0],r[1],r[2],r[3],i[0],i[1],i[2],i[3])}catch(t){let n=\"Error while drawing image: '\"+e+\"': \"+t.name;t.message&&(n+=\": \"+t.message);console.error(n)}}}}()");
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WGLWidget",
        "(function(t,e){e.wtObj=this;const n=this,i=t.WT,s=i.glMatrix.vec3,o=i.glMatrix.mat4;this.ctx=null;this.initializeGL=function(){};this.paintGL=function(){};this.resizeGL=function(){};this.updates=new Array;this.initialized=!1;this.preloadingTextures=0;this.preloadingBuffers=0;this.jsValues={};this.discoverContext=function(t,n){if(e.getContext){try{this.ctx=e.getContext(\"webgl\",{antialias:n})}catch(t){}if(null===this.ctx)try{this.ctx=e.getContext(\"experimental-webgl\",{antialias:n})}catch(t){}if(null===this.ctx){const n=e.firstChild;e.parentNode.insertBefore(n,e);e.style.display=\"none\";t()}}return this.ctx};if(e.addEventListener){e.addEventListener(\"webglcontextlost\",(function(t){t.preventDefault();n.initialized=!1}),!1);e.addEventListener(\"webglcontextrestored\",(function(n){t.emit(e,\"contextRestored\")}),!1)}let u=null;this.setMouseHandler=function(t){u=t;u.setTarget&&u.setTarget(this)};this.LookAtMouseHandler=function(t,u,a,l,c){const h=t,r=u,f=a,d=l,g=c;let p=null,x=null,m=null,y=null;const M=this;this.mouseDown=function(t,n){i.capture(null);i.capture(e);y=i.pageCoordinates(n)};this.mouseUp=function(t,e){null!==y&&(y=null)};this.mouseDrag=function(t,e){if(null===y)return;const n=i.pageCoordinates(e);1===i.buttons&&C(n)};this.mouseWheel=function(t,e){i.cancelEvent(e);v(i.wheelDelta(e))};function v(t){const e=Math.pow(1.2,t);o.translate(h,r);o.scale(h,[e,e,e]);s.negate(r);o.translate(h,r);s.negate(r);n.paintGL()}function C(t){const e=h[5]/s.length([h[1],h[5],h[9]]),i=h[6]/s.length([h[2],h[6],h[10]]),u=Math.atan2(i,e),a=t.x-y.x,l=t.y-y.y,c=s.create();c[0]=h[0];c[1]=h[4];c[2]=h[8];const p=o.create();o.identity(p);o.translate(p,r);let x=l*d;if(Math.abs(u+x)>=Math.PI/2){x=(u>0?1:-1)*Math.PI/2-u}o.rotate(p,x,c);o.rotate(p,a*g,f);s.negate(r);o.translate(p,r);s.negate(r);o.multiply(h,p,h);n.paintGL();y=t}this.touchStart=function(t,n){x=1===n.touches.length;m=2===n.touches.length;if(x){i.capture(null);i.capture(e);y=i.pageCoordinates(n.touches[0])}else{if(!m)return;{const t=i.pageCoordinates(n.touches[0]),e=i.pageCoordinates(n.touches[1]);p=Math.sqrt((t.x-e.x)*(t.x-e.x)+(t.y-e.y)*(t.y-e.y))}}n.preventDefault()};this.touchEnd=function(t,e){const n=0===e.touches.length;x=1===e.touches.length;m=2===e.touches.length;n&&M.mouseUp(null,null);(x||m)&&M.touchStart(t,e)};this.touchMoved=function(t,e){if(x||m){e.preventDefault();if(x){if(null===y)return;C(i.pageCoordinates(e))}if(m){const t=i.pageCoordinates(e.touches[0]),n=i.pageCoordinates(e.touches[1]),s=Math.sqrt((t.x-n.x)*(t.x-n.x)+(t.y-n.y)*(t.y-n.y));let o=s/p;if(Math.abs(o-1)<.05)return;o=o>1?1:-1;p=s;v(o)}}}};this.WalkMouseHandler=function(t,u,a){const l=t,c=u,h=a;let r=null;this.mouseDown=function(t,n){i.capture(null);i.capture(e);r=i.pageCoordinates(n)};this.mouseUp=function(t,e){null!==r&&(r=null)};this.mouseDrag=function(t,e){if(null===r)return;f(i.pageCoordinates(e))};function f(t){const e=t.x-r.x,u=t.y-r.y,a=o.create();o.identity(a);o.rotateY(a,e*h);const f=s.create();f[0]=0;f[1]=0;f[2]=-c*u;o.translate(a,f);o.multiply(a,l,l);n.paintGL();r=i.pageCoordinates(event)}};this.mouseDrag=function(t,e){(this.initialized||!this.ctx)&&u&&u.mouseDrag&&u.mouseDrag(t,e)};this.mouseMove=function(t,e){(this.initialized||!this.ctx)&&u&&u.mouseMove&&u.mouseMove(t,e)};this.mouseDown=function(t,e){(this.initialized||!this.ctx)&&u&&u.mouseDown&&u.mouseDown(t,e)};this.mouseUp=function(t,e){(this.initialized||!this.ctx)&&u&&u.mouseUp&&u.mouseUp(t,e)};this.mouseWheel=function(t,e){(this.initialized||!this.ctx)&&u&&u.mouseWheel&&u.mouseWheel(t,e)};this.touchStart=function(t,e){(this.initialized||!this.ctx)&&u&&u.touchStart&&u.touchStart(t,e)};this.touchEnd=function(t,e){(this.initialized||!this.ctx)&&u&&u.touchEnd&&u.touchEnd(t,e)};this.touchMoved=function(t,e){(this.initialized||!this.ctx)&&u&&u.touchMoved&&u.touchMoved(t,e)};this.handlePreload=function(){if(0===this.preloadingTextures&&0===this.preloadingBuffers)if(this.initialized){for(const t of this.updates)t();this.updates=new Array;this.resizeGL();this.paintGL()}else{this.initializeGL();this.resizeGL();this.paintGL()}};e.wtEncodeValue=function(){const t=e.wtObj;let n=\"\";for(const[e,i]of Object.entries(t.jsValues)){n+=e+\":\";for(let t=0;t<i.length;t++){n+=i[t];t!==i.length-1?n+=\",\":n+=\";\"}}return n};let a=null;const l=new Image;l.busy=!1;l.onload=function(){e.src=l.src;null!==a?l.src=a:l.busy=!1;a=null};l.onerror=l.onload;this.loadImage=function(t){if(l.busy)a=t;else{l.src=t;l.busy=!0}}})");
  }

  static WJavaScriptPreamble wtjs2() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptObject,
        "glMatrix",
        "function(){let t;t=\"undefined\"!=typeof Float32Array?Float32Array:Array;const n={create:function(n){const r=new t(3);if(n){r[0]=n[0];r[1]=n[1];r[2]=n[2]}return r},set:function(t,n){n[0]=t[0];n[1]=t[1];n[2]=t[2];return n},add:function(t,n,r){if(!r||t===r){t[0]+=n[0];t[1]+=n[1];t[2]+=n[2];return t}r[0]=t[0]+n[0];r[1]=t[1]+n[1];r[2]=t[2]+n[2];return r},subtract:function(t,n,r){if(!r||t===r){t[0]-=n[0];t[1]-=n[1];t[2]-=n[2];return t}r[0]=t[0]-n[0];r[1]=t[1]-n[1];r[2]=t[2]-n[2];return r},negate:function(t,n){n||(n=t);n[0]=-t[0];n[1]=-t[1];n[2]=-t[2];return n},scale:function(t,n,r){if(!r||t===r){t[0]*=n;t[1]*=n;t[2]*=n;return t}r[0]=t[0]*n;r[1]=t[1]*n;r[2]=t[2]*n;return r},normalize:function(t,n){n||(n=t);const r=t[0],e=t[1],u=t[2],o=Math.sqrt(r*r+e*e+u*u);if(!o){n[0]=0;n[1]=0;n[2]=0;return n}if(1===o){n[0]=r;n[1]=e;n[2]=u;return n}const c=1/o;n[0]=r*c;n[1]=e*c;n[2]=u*c;return n},cross:function(t,n,r){r||(r=t);const e=t[0],u=t[1],o=t[2],c=n[0],i=n[1],f=n[2];r[0]=u*f-o*i;r[1]=o*c-e*f;r[2]=e*i-u*c;return r},length:function(t){const n=t[0],r=t[1],e=t[2];return Math.sqrt(n*n+r*r+e*e)},dot:function(t,n){return t[0]*n[0]+t[1]*n[1]+t[2]*n[2]},direction:function(t,n,r){r||(r=t);const e=t[0]-n[0],u=t[1]-n[1],o=t[2]-n[2],c=Math.sqrt(e*e+u*u+o*o);if(!c){r[0]=0;r[1]=0;r[2]=0;return r}const i=1/c;r[0]=e*i;r[1]=u*i;r[2]=o*i;return r},str:function(t){return\"[\"+t[0]+\", \"+t[1]+\", \"+t[2]+\"]\"}},r={create:function(n){const r=new t(9);if(n){r[0]=n[0];r[1]=n[1];r[2]=n[2];r[3]=n[3];r[4]=n[4];r[5]=n[5];r[6]=n[6];r[7]=n[7];r[8]=n[8];r[9]=n[9]}return r},set:function(t,n){n[0]=t[0];n[1]=t[1];n[2]=t[2];n[3]=t[3];n[4]=t[4];n[5]=t[5];n[6]=t[6];n[7]=t[7];n[8]=t[8];return n},identity:function(t){t[0]=1;t[1]=0;t[2]=0;t[3]=0;t[4]=1;t[5]=0;t[6]=0;t[7]=0;t[8]=1;return t},toMat4:function(t,n){n||(n=e.create());n[0]=t[0];n[1]=t[1];n[2]=t[2];n[3]=0;n[4]=t[3];n[5]=t[4];n[6]=t[5];n[7]=0;n[8]=t[6];n[9]=t[7];n[10]=t[8];n[11]=0;n[12]=0;n[13]=0;n[14]=0;n[15]=1;return n},str:function(t){return\"[\"+t[0]+\", \"+t[1]+\", \"+t[2]+\", \"+t[3]+\", \"+t[4]+\", \"+t[5]+\", \"+t[6]+\", \"+t[7]+\", \"+t[8]+\"]\"}},e={create:function(n){const r=new t(16);if(n){r[0]=n[0];r[1]=n[1];r[2]=n[2];r[3]=n[3];r[4]=n[4];r[5]=n[5];r[6]=n[6];r[7]=n[7];r[8]=n[8];r[9]=n[9];r[10]=n[10];r[11]=n[11];r[12]=n[12];r[13]=n[13];r[14]=n[14];r[15]=n[15]}return r},set:function(t,n){n[0]=t[0];n[1]=t[1];n[2]=t[2];n[3]=t[3];n[4]=t[4];n[5]=t[5];n[6]=t[6];n[7]=t[7];n[8]=t[8];n[9]=t[9];n[10]=t[10];n[11]=t[11];n[12]=t[12];n[13]=t[13];n[14]=t[14];n[15]=t[15];return n},identity:function(t){t[0]=1;t[1]=0;t[2]=0;t[3]=0;t[4]=0;t[5]=1;t[6]=0;t[7]=0;t[8]=0;t[9]=0;t[10]=1;t[11]=0;t[12]=0;t[13]=0;t[14]=0;t[15]=1;return t},transpose:function(t,n){if(!n||t===n){const n=t[1],r=t[2],e=t[3],u=t[6],o=t[7],c=t[11];t[1]=t[4];t[2]=t[8];t[3]=t[12];t[4]=n;t[6]=t[9];t[7]=t[13];t[8]=r;t[9]=u;t[11]=t[14];t[12]=e;t[13]=o;t[14]=c;return t}n[0]=t[0];n[1]=t[4];n[2]=t[8];n[3]=t[12];n[4]=t[1];n[5]=t[5];n[6]=t[9];n[7]=t[13];n[8]=t[2];n[9]=t[6];n[10]=t[10];n[11]=t[14];n[12]=t[3];n[13]=t[7];n[14]=t[11];n[15]=t[15];return n},determinant:function(t){const n=t[0],r=t[1],e=t[2],u=t[3],o=t[4],c=t[5],i=t[6],f=t[7],s=t[8],a=t[9],l=t[10],M=t[11],h=t[12],d=t[13],y=t[14],m=t[15];return h*a*i*u-s*d*i*u-h*c*l*u+o*d*l*u+s*c*y*u-o*a*y*u-h*a*e*f+s*d*e*f+h*r*l*f-n*d*l*f-s*r*y*f+n*a*y*f+h*c*e*M-o*d*e*M-h*r*i*M+n*d*i*M+o*r*y*M-n*c*y*M-s*c*e*m+o*a*e*m+s*r*i*m-n*a*i*m-o*r*l*m+n*c*l*m},inverse:function(t,n){n||(n=t);const r=t[0],e=t[1],u=t[2],o=t[3],c=t[4],i=t[5],f=t[6],s=t[7],a=t[8],l=t[9],M=t[10],h=t[11],d=t[12],y=t[13],m=t[14],p=t[15],q=r*i-e*c,v=r*f-u*c,A=r*s-o*c,E=e*f-u*i,g=e*s-o*i,w=u*s-o*f,R=a*y-l*d,_=a*m-M*d,b=a*p-h*d,F=l*m-M*y,I=l*p-h*y,T=M*p-h*m,V=1/(q*T-v*I+A*F+E*b-g*_+w*R);n[0]=(i*T-f*I+s*F)*V;n[1]=(-e*T+u*I-o*F)*V;n[2]=(y*w-m*g+p*E)*V;n[3]=(-l*w+M*g-h*E)*V;n[4]=(-c*T+f*b-s*_)*V;n[5]=(r*T-u*b+o*_)*V;n[6]=(-d*w+m*A-p*v)*V;n[7]=(a*w-M*A+h*v)*V;n[8]=(c*I-i*b+s*R)*V;n[9]=(-r*I+e*b-o*R)*V;n[10]=(d*g-y*A+p*q)*V;n[11]=(-a*g+l*A-h*q)*V;n[12]=(-c*F+i*_-f*R)*V;n[13]=(r*F-e*_+u*R)*V;n[14]=(-d*E+y*v-m*q)*V;n[15]=(a*E-l*v+M*q)*V;return n},toRotationMat:function(t,n){n||(n=e.create());n[0]=t[0];n[1]=t[1];n[2]=t[2];n[3]=t[3];n[4]=t[4];n[5]=t[5];n[6]=t[6];n[7]=t[7];n[8]=t[8];n[9]=t[9];n[10]=t[10];n[11]=t[11];n[12]=0;n[13]=0;n[14]=0;n[15]=1;return n},toMat3:function(t,n){n||(n=r.create());n[0]=t[0];n[1]=t[1];n[2]=t[2];n[3]=t[4];n[4]=t[5];n[5]=t[6];n[6]=t[8];n[7]=t[9];n[8]=t[10];return n},toInverseMat3:function(t,n){const e=t[0],u=t[1],o=t[2],c=t[4],i=t[5],f=t[6],s=t[8],a=t[9],l=t[10],M=l*i-f*a,h=-l*c+f*s,d=a*c-i*s,y=e*M+u*h+o*d;if(!y)return null;const m=1/y;n||(n=r.create());n[0]=M*m;n[1]=(-l*u+o*a)*m;n[2]=(f*u-o*i)*m;n[3]=h*m;n[4]=(l*e-o*s)*m;n[5]=(-f*e+o*c)*m;n[6]=d*m;n[7]=(-a*e+u*s)*m;n[8]=(i*e-u*c)*m;return n},multiply:function(t,n,r){r||(r=t);const e=t[0],u=t[1],o=t[2],c=t[3],i=t[4],f=t[5],s=t[6],a=t[7],l=t[8],M=t[9],h=t[10],d=t[11],y=t[12],m=t[13],p=t[14],q=t[15],v=n[0],A=n[1],E=n[2],g=n[3],w=n[4],R=n[5],_=n[6],b=n[7],F=n[8],I=n[9],T=n[10],V=n[11],W=n[12],j=n[13],k=n[14],x=n[15];r[0]=v*e+A*i+E*l+g*y;r[1]=v*u+A*f+E*M+g*m;r[2]=v*o+A*s+E*h+g*p;r[3]=v*c+A*a+E*d+g*q;r[4]=w*e+R*i+_*l+b*y;r[5]=w*u+R*f+_*M+b*m;r[6]=w*o+R*s+_*h+b*p;r[7]=w*c+R*a+_*d+b*q;r[8]=F*e+I*i+T*l+V*y;r[9]=F*u+I*f+T*M+V*m;r[10]=F*o+I*s+T*h+V*p;r[11]=F*c+I*a+T*d+V*q;r[12]=W*e+j*i+k*l+x*y;r[13]=W*u+j*f+k*M+x*m;r[14]=W*o+j*s+k*h+x*p;r[15]=W*c+j*a+k*d+x*q;return r},multiplyVec3:function(t,n,r){r||(r=n);const e=n[0],u=n[1],o=n[2];r[0]=t[0]*e+t[4]*u+t[8]*o+t[12];r[1]=t[1]*e+t[5]*u+t[9]*o+t[13];r[2]=t[2]*e+t[6]*u+t[10]*o+t[14];return r},multiplyVec4:function(t,n,r){r||(r=n);const e=n[0],u=n[1],o=n[2],c=n[3];r[0]=t[0]*e+t[4]*u+t[8]*o+t[12]*c;r[1]=t[1]*e+t[5]*u+t[9]*o+t[13]*c;r[2]=t[2]*e+t[6]*u+t[10]*o+t[14]*c;r[3]=t[3]*e+t[7]*u+t[11]*o+t[15]*c;return r},translate:function(t,n,r){const e=n[0],u=n[1],o=n[2];if(!r||t===r){t[12]=t[0]*e+t[4]*u+t[8]*o+t[12];t[13]=t[1]*e+t[5]*u+t[9]*o+t[13];t[14]=t[2]*e+t[6]*u+t[10]*o+t[14];t[15]=t[3]*e+t[7]*u+t[11]*o+t[15];return t}const c=t[0],i=t[1],f=t[2],s=t[3],a=t[4],l=t[5],M=t[6],h=t[7],d=t[8],y=t[9],m=t[10],p=t[11];r[0]=c;r[1]=i;r[2]=f;r[3]=s;r[4]=a;r[5]=l;r[6]=M;r[7]=h;r[8]=d;r[9]=y;r[10]=m;r[11]=p;r[12]=c*e+a*u+d*o+t[12];r[13]=i*e+l*u+y*o+t[13];r[14]=f*e+M*u+m*o+t[14];r[15]=s*e+h*u+p*o+t[15];return r},scale:function(t,n,r){const e=n[0],u=n[1],o=n[2];if(!r||t===r){t[0]*=e;t[1]*=e;t[2]*=e;t[3]*=e;t[4]*=u;t[5]*=u;t[6]*=u;t[7]*=u;t[8]*=o;t[9]*=o;t[10]*=o;t[11]*=o;return t}r[0]=t[0]*e;r[1]=t[1]*e;r[2]=t[2]*e;r[3]=t[3]*e;r[4]=t[4]*u;r[5]=t[5]*u;r[6]=t[6]*u;r[7]=t[7]*u;r[8]=t[8]*o;r[9]=t[9]*o;r[10]=t[10]*o;r[11]=t[11]*o;r[12]=t[12];r[13]=t[13];r[14]=t[14];r[15]=t[15];return r},rotate:function(t,n,r,e){let u=r[0],o=r[1],c=r[2];const i=Math.sqrt(u*u+o*o+c*c);if(!i)return null;if(1!==i){const t=1/i;u*=t;o*=t;c*=t}const f=Math.sin(n),s=Math.cos(n),a=1-s,l=t[0],M=t[1],h=t[2],d=t[3],y=t[4],m=t[5],p=t[6],q=t[7],v=t[8],A=t[9],E=t[10],g=t[11],w=u*u*a+s,R=o*u*a+c*f,_=c*u*a-o*f,b=u*o*a-c*f,F=o*o*a+s,I=c*o*a+u*f,T=u*c*a+o*f,V=o*c*a-u*f,W=c*c*a+s;if(e){if(t!==e){e[12]=t[12];e[13]=t[13];e[14]=t[14];e[15]=t[15]}}else e=t;e[0]=l*w+y*R+v*_;e[1]=M*w+m*R+A*_;e[2]=h*w+p*R+E*_;e[3]=d*w+q*R+g*_;e[4]=l*b+y*F+v*I;e[5]=M*b+m*F+A*I;e[6]=h*b+p*F+E*I;e[7]=d*b+q*F+g*I;e[8]=l*T+y*V+v*W;e[9]=M*T+m*V+A*W;e[10]=h*T+p*V+E*W;e[11]=d*T+q*V+g*W;return e},rotateX:function(t,n,r){const e=Math.sin(n),u=Math.cos(n),o=t[4],c=t[5],i=t[6],f=t[7],s=t[8],a=t[9],l=t[10],M=t[11];if(r){if(t!==r){r[0]=t[0];r[1]=t[1];r[2]=t[2];r[3]=t[3];r[12]=t[12];r[13]=t[13];r[14]=t[14];r[15]=t[15]}}else r=t;r[4]=o*u+s*e;r[5]=c*u+a*e;r[6]=i*u+l*e;r[7]=f*u+M*e;r[8]=o*-e+s*u;r[9]=c*-e+a*u;r[10]=i*-e+l*u;r[11]=f*-e+M*u;return r},rotateY:function(t,n,r){const e=Math.sin(n),u=Math.cos(n),o=t[0],c=t[1],i=t[2],f=t[3],s=t[8],a=t[9],l=t[10],M=t[11];if(r){if(t!==r){r[4]=t[4];r[5]=t[5];r[6]=t[6];r[7]=t[7];r[12]=t[12];r[13]=t[13];r[14]=t[14];r[15]=t[15]}}else r=t;r[0]=o*u+s*-e;r[1]=c*u+a*-e;r[2]=i*u+l*-e;r[3]=f*u+M*-e;r[8]=o*e+s*u;r[9]=c*e+a*u;r[10]=i*e+l*u;r[11]=f*e+M*u;return r},rotateZ:function(t,n,r){const e=Math.sin(n),u=Math.cos(n),o=t[0],c=t[1],i=t[2],f=t[3],s=t[4],a=t[5],l=t[6],M=t[7];if(r){if(t!==r){r[8]=t[8];r[9]=t[9];r[10]=t[10];r[11]=t[11];r[12]=t[12];r[13]=t[13];r[14]=t[14];r[15]=t[15]}}else r=t;r[0]=o*u+s*e;r[1]=c*u+a*e;r[2]=i*u+l*e;r[3]=f*u+M*e;r[4]=o*-e+s*u;r[5]=c*-e+a*u;r[6]=i*-e+l*u;r[7]=f*-e+M*u;return r},frustum:function(t,n,r,u,o,c,i){i||(i=e.create());const f=n-t,s=u-r,a=c-o;i[0]=2*o/f;i[1]=0;i[2]=0;i[3]=0;i[4]=0;i[5]=2*o/s;i[6]=0;i[7]=0;i[8]=(n+t)/f;i[9]=(u+r)/s;i[10]=-(c+o)/a;i[11]=-1;i[12]=0;i[13]=0;i[14]=-c*o*2/a;i[15]=0;return i},perspective:function(t,n,r,u,o){const c=r*Math.tan(t*Math.PI/360),i=c*n;return e.frustum(-i,i,-c,c,r,u,o)},ortho:function(t,n,r,u,o,c,i){i||(i=e.create());const f=n-t,s=u-r,a=c-o;i[0]=2/f;i[1]=0;i[2]=0;i[3]=0;i[4]=0;i[5]=2/s;i[6]=0;i[7]=0;i[8]=0;i[9]=0;i[10]=-2/a;i[11]=0;i[12]=-(t+n)/f;i[13]=-(u+r)/s;i[14]=-(c+o)/a;i[15]=1;return i},lookAt:function(t,n,r,u){u||(u=e.create());const o=t[0],c=t[1],i=t[2],f=r[0],s=r[1],a=r[2],l=n[0],M=n[1],h=n[2];if(o===l&&c===M&&i===h)return e.identity(u);let d,y,m,p,q,v,A,E,g,w;d=o-n[0];y=c-n[1];m=i-n[2];w=1/Math.sqrt(d*d+y*y+m*m);d*=w;y*=w;m*=w;p=s*m-a*y;q=a*d-f*m;v=f*y-s*d;w=Math.sqrt(p*p+q*q+v*v);if(w){w=1/w;p*=w;q*=w;v*=w}else{p=0;q=0;v=0}A=y*v-m*q;E=m*p-d*v;g=d*q-y*p;w=Math.sqrt(A*A+E*E+g*g);if(w){w=1/w;A*=w;E*=w;g*=w}else{A=0;E=0;g=0}u[0]=p;u[1]=A;u[2]=d;u[3]=0;u[4]=q;u[5]=E;u[6]=y;u[7]=0;u[8]=v;u[9]=g;u[10]=m;u[11]=0;u[12]=-(p*o+q*c+v*i);u[13]=-(A*o+E*c+g*i);u[14]=-(d*o+y*c+m*i);u[15]=1;return u},str:function(t){return\"[\"+t[0]+\", \"+t[1]+\", \"+t[2]+\", \"+t[3]+\", \"+t[4]+\", \"+t[5]+\", \"+t[6]+\", \"+t[7]+\", \"+t[8]+\", \"+t[9]+\", \"+t[10]+\", \"+t[11]+\", \"+t[12]+\", \"+t[13]+\", \"+t[14]+\", \"+t[15]+\"]\"}};return{vec3:n,mat3:r,mat4:e}}()");
  }
}
