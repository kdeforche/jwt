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

class WClientGLWidget extends WAbstractGLImplementation {
  private static Logger logger = LoggerFactory.getLogger(WClientGLWidget.class);

  public WClientGLWidget(WGLWidget glInterface) {
    super(glInterface);
    this.js_ = new StringWriter();
    this.shaders_ = 0;
    this.programs_ = 0;
    this.attributes_ = 0;
    this.uniforms_ = 0;
    this.buffers_ = 0;
    this.arrayBuffers_ = 0;
    this.framebuffers_ = 0;
    this.renderbuffers_ = 0;
    this.textures_ = 0;
    this.images_ = 1;
    this.canvas_ = 0;
    this.currentlyBoundBuffer_ = new WGLWidget.Buffer();
    this.currentlyBoundTexture_ = new WGLWidget.Texture();
    this.binaryResources_ = new ArrayList<WResource>();
    this.preloadImages_ = new ArrayList<WClientGLWidget.PreloadImage>();
    this.preloadArrayBuffers_ = new ArrayList<WClientGLWidget.PreloadArrayBuffer>();
  }

  public void debugger() {
    this.js_.append("debugger;\n");
  }

  public void activeTexture(WGLWidget.GLenum texture) {
    this.js_.append("ctx.activeTexture(").append("ctx." + texture.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void attachShader(WGLWidget.Program program, WGLWidget.Shader shader) {
    this.js_
        .append("ctx.attachShader(")
        .append(program.getJsRef())
        .append(", ")
        .append(shader.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bindAttribLocation(WGLWidget.Program program, int index, final String name) {
    this.js_
        .append("ctx.bindAttribLocation(")
        .append(program.getJsRef())
        .append(",")
        .append(String.valueOf(index))
        .append(",")
        .append(WWebWidget.jsStringLiteral(name))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bindBuffer(WGLWidget.GLenum target, WGLWidget.Buffer buffer) {
    this.js_
        .append("ctx.bindBuffer(")
        .append("ctx." + target.toString())
        .append(",")
        .append(buffer.getJsRef())
        .append(");");
    this.currentlyBoundBuffer_ = buffer;
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bindFramebuffer(WGLWidget.GLenum target, WGLWidget.Framebuffer buffer) {
    this.js_
        .append("ctx.bindFramebuffer(")
        .append("ctx." + target.toString())
        .append(",")
        .append(buffer.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bindRenderbuffer(WGLWidget.GLenum target, WGLWidget.Renderbuffer buffer) {
    this.js_
        .append("ctx.bindRenderbuffer(")
        .append("ctx." + target.toString())
        .append(",")
        .append(buffer.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bindTexture(WGLWidget.GLenum target, WGLWidget.Texture texture) {
    this.js_
        .append("ctx.bindTexture(")
        .append("ctx." + target.toString())
        .append(",")
        .append(texture.getJsRef())
        .append(");");
    this.currentlyBoundTexture_ = texture;
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void blendColor(double red, double green, double blue, double alpha) {
    char[] buf = new char[30];
    this.js_.append("ctx.blendColor(").append(WebGLUtils.makeFloat(red)).append(",");
    this.js_.append(WebGLUtils.makeFloat(green)).append(",");
    this.js_.append(WebGLUtils.makeFloat(blue)).append(",");
    this.js_.append(WebGLUtils.makeFloat(alpha)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void blendEquation(WGLWidget.GLenum mode) {
    this.js_.append("ctx.blendEquation(").append("ctx." + mode.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void blendEquationSeparate(WGLWidget.GLenum modeRGB, WGLWidget.GLenum modeAlpha) {
    this.js_
        .append("ctx.blendEquationSeparate(")
        .append("ctx." + modeRGB.toString())
        .append(",")
        .append("ctx." + modeAlpha.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void blendFunc(WGLWidget.GLenum sfactor, WGLWidget.GLenum dfactor) {
    this.js_
        .append("ctx.blendFunc(")
        .append("ctx." + sfactor.toString())
        .append(",")
        .append("ctx." + dfactor.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void blendFuncSeparate(
      WGLWidget.GLenum srcRGB,
      WGLWidget.GLenum dstRGB,
      WGLWidget.GLenum srcAlpha,
      WGLWidget.GLenum dstAlpha) {
    this.js_
        .append("ctx.blendFuncSeparate(")
        .append("ctx." + srcRGB.toString())
        .append(",")
        .append("ctx." + dstRGB.toString())
        .append(",")
        .append("ctx." + srcAlpha.toString())
        .append(",")
        .append("ctx." + dstAlpha.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferData(WGLWidget.GLenum target, int size, WGLWidget.GLenum usage) {
    this.js_.append("ctx.bufferData(").append("ctx." + target.toString()).append(",");
    this.js_.append(String.valueOf(size)).append(",");
    this.js_.append("ctx." + usage.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferData(
      WGLWidget.GLenum target, WGLWidget.ArrayBuffer res, WGLWidget.GLenum usage) {
    this.js_.append("ctx.bufferData(").append("ctx." + target.toString()).append(",");
    this.js_.append(res.getJsRef()).append(".data, ");
    this.js_.append("ctx." + usage.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferData(
      WGLWidget.GLenum target,
      WGLWidget.ArrayBuffer res,
      int bufferResourceOffset,
      int bufferResourceSize,
      WGLWidget.GLenum usage) {
    this.js_.append("ctx.bufferData(").append("ctx." + target.toString()).append(",");
    this.js_
        .append(res.getJsRef())
        .append(".data.slice(")
        .append(String.valueOf(bufferResourceOffset))
        .append(",")
        .append(String.valueOf(bufferResourceOffset + bufferResourceSize))
        .append("),");
    this.js_.append("ctx." + usage.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferSubData(WGLWidget.GLenum target, int offset, WGLWidget.ArrayBuffer res) {
    this.js_.append("ctx.bufferSubData(").append("ctx." + target.toString()).append(",");
    this.js_.append(String.valueOf(offset)).append(",");
    this.js_.append(res.getJsRef()).append(".data);");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferSubData(
      WGLWidget.GLenum target,
      int offset,
      WGLWidget.ArrayBuffer res,
      int bufferResourceOffset,
      int bufferResourceSize) {
    this.js_.append("ctx.bufferSubData(").append("ctx." + target.toString()).append(",");
    this.js_.append(String.valueOf(offset)).append(",");
    this.js_
        .append(res.getJsRef())
        .append(".data.slice(")
        .append(String.valueOf(bufferResourceOffset))
        .append(", ")
        .append(String.valueOf(bufferResourceOffset + bufferResourceSize))
        .append("));");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferDatafv(
      WGLWidget.GLenum target,
      final java.nio.ByteBuffer v,
      WGLWidget.GLenum usage,
      boolean binary) {
    if (binary) {
      WMemoryResource res = new WMemoryResource("application/octet");
      WMemoryResource resPtr = res;
      res.setData(v.array());
      this.binaryResources_.add(res);
      this.preloadArrayBuffers_.add(
          new WClientGLWidget.PreloadArrayBuffer(
              this.currentlyBoundBuffer_.getJsRef(), resPtr.getUrl()));
      this.js_.append("ctx.bufferData(").append("ctx." + target.toString()).append(",");
      this.js_.append(this.currentlyBoundBuffer_.getJsRef()).append(".data, ");
      this.js_.append("ctx." + usage.toString()).append(");");
    } else {
      this.bufferDatafv(target, ((java.nio.ByteBuffer) v.rewind()).asFloatBuffer(), usage);
    }
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferDatafv(
      WGLWidget.GLenum target, final java.nio.FloatBuffer buffer, WGLWidget.GLenum usage) {
    this.js_.append("ctx.bufferData(").append("ctx." + target.toString()).append(",");
    this.js_.append("new Float32Array([");
    char[] buf = new char[30];
    for (int i = 0; i < buffer.capacity(); i++) {
      this.js_.append(i == 0 ? "" : ",").append(WebGLUtils.makeFloat(buffer.get(i)));
    }
    this.js_.append("])");
    this.js_.append(",").append("ctx." + usage.toString()).append(");");
  }

  public void bufferSubDatafv(
      WGLWidget.GLenum target, int offset, final java.nio.ByteBuffer buffer, boolean binary) {
    if (binary) {
      WMemoryResource res = new WMemoryResource("application/octet");
      res.setData(buffer.array());
      this.preloadArrayBuffers_.add(
          new WClientGLWidget.PreloadArrayBuffer(
              this.currentlyBoundBuffer_.getJsRef(), res.getUrl()));
      this.binaryResources_.add(res);
      this.js_.append("ctx.bufferSubData(").append("ctx." + target.toString()).append(",");
      this.js_.append(String.valueOf(offset)).append(",");
      this.js_.append(this.currentlyBoundBuffer_.getJsRef()).append(".data);");
    } else {
      this.bufferSubDatafv(target, offset, ((java.nio.ByteBuffer) buffer.rewind()).asFloatBuffer());
    }
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferSubDatafv(
      WGLWidget.GLenum target, int offset, final java.nio.FloatBuffer buffer) {
    this.js_.append("ctx.bufferSubData(").append("ctx." + target.toString()).append(",");
    this.js_.append(String.valueOf(offset)).append(",");
    this.js_.append("new Float32Array([");
    char[] buf = new char[30];
    for (int i = 0; i < buffer.capacity(); i++) {
      this.js_.append(i == 0 ? "" : ",").append(WebGLUtils.makeFloat(buffer.get(i)));
    }
    this.js_.append("])");
    this.js_.append(");");
  }

  public void bufferDataiv(
      WGLWidget.GLenum target,
      final java.nio.IntBuffer buffer,
      WGLWidget.GLenum usage,
      WGLWidget.GLenum type) {
    this.js_.append("ctx.bufferData(").append("ctx." + target.toString()).append(",");
    renderiv(this.js_, buffer, type);
    this.js_.append(",").append("ctx." + usage.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void bufferSubDataiv(
      WGLWidget.GLenum target, int offset, final java.nio.IntBuffer buffer, WGLWidget.GLenum type) {
    this.js_
        .append("ctx.bufferSubData(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(offset))
        .append(",");
    renderiv(this.js_, buffer, type);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void clearBinaryResources() {
    this.binaryResources_.clear();
  }

  public void clear(EnumSet<WGLWidget.GLenum> mask) {
    this.js_.append("ctx.clear(");
    if (!EnumUtils.mask(mask, WGLWidget.GLenum.COLOR_BUFFER_BIT).isEmpty()) {
      this.js_.append("ctx.COLOR_BUFFER_BIT|");
    }
    if (!EnumUtils.mask(mask, WGLWidget.GLenum.DEPTH_BUFFER_BIT).isEmpty()) {
      this.js_.append("ctx.DEPTH_BUFFER_BIT|");
    }
    if (!EnumUtils.mask(mask, WGLWidget.GLenum.STENCIL_BUFFER_BIT).isEmpty()) {
      this.js_.append("ctx.STENCIL_BUFFER_BIT|");
    }
    this.js_.append("0);");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void clearColor(double r, double g, double b, double a) {
    char[] buf = new char[30];
    this.js_.append("ctx.clearColor(").append(WebGLUtils.makeFloat(r)).append(",");
    this.js_.append(WebGLUtils.makeFloat(g)).append(",");
    this.js_.append(WebGLUtils.makeFloat(b)).append(",");
    this.js_.append(WebGLUtils.makeFloat(a)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void clearDepth(double depth) {
    char[] buf = new char[30];
    this.js_.append("ctx.clearDepth(").append(WebGLUtils.makeFloat(depth)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void clearStencil(int s) {
    this.js_.append("ctx.clearStencil(").append(String.valueOf(s)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
    this.js_
        .append("ctx.colorMask(")
        .append(red ? "true" : "false")
        .append(",")
        .append(green ? "true" : "false")
        .append(",")
        .append(blue ? "true" : "false")
        .append(",")
        .append(alpha ? "true" : "false")
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void compileShader(WGLWidget.Shader shader) {
    this.js_.append("ctx.compileShader(").append(shader.getJsRef()).append(");");
    this.js_
        .append("if (!ctx.getShaderParameter(")
        .append(shader.getJsRef())
        .append(", ctx.COMPILE_STATUS)) {")
        .append("alert(ctx.getShaderInfoLog(")
        .append(shader.getJsRef())
        .append("));}");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void copyTexImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalFormat,
      int x,
      int y,
      int width,
      int height,
      int border) {
    this.js_
        .append("ctx.copyTexImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append("ctx." + internalFormat.toString())
        .append(",")
        .append(String.valueOf(x))
        .append(",")
        .append(String.valueOf(y))
        .append(",")
        .append(String.valueOf(width))
        .append(",")
        .append(String.valueOf(height))
        .append(",")
        .append(String.valueOf(border))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void copyTexSubImage2D(
      WGLWidget.GLenum target,
      int level,
      int xoffset,
      int yoffset,
      int x,
      int y,
      int width,
      int height) {
    this.js_
        .append("ctx.copyTexSubImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append(String.valueOf(xoffset))
        .append(",")
        .append(String.valueOf(yoffset))
        .append(",")
        .append(String.valueOf(x))
        .append(",")
        .append(String.valueOf(y))
        .append(",")
        .append(String.valueOf(width))
        .append(",")
        .append(String.valueOf(height))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public WGLWidget.Buffer getCreateBuffer() {
    WGLWidget.Buffer retval = new WGLWidget.Buffer(this.buffers_++);
    this.js_.append("if (!").append(retval.getJsRef()).append("){");
    this.js_.append(retval.getJsRef()).append("=ctx.createBuffer();");
    this.js_.append("\n}");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public WGLWidget.ArrayBuffer createAndLoadArrayBuffer(final String url) {
    WGLWidget.ArrayBuffer retval = new WGLWidget.ArrayBuffer(this.arrayBuffers_++);
    this.preloadArrayBuffers_.add(new WClientGLWidget.PreloadArrayBuffer(retval.getJsRef(), url));
    return retval;
  }

  public WGLWidget.Framebuffer getCreateFramebuffer() {
    WGLWidget.Framebuffer retval = new WGLWidget.Framebuffer(this.framebuffers_++);
    this.js_.append(retval.getJsRef()).append("=ctx.createFramebuffer();");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public WGLWidget.Program getCreateProgram() {
    WGLWidget.Program retval = new WGLWidget.Program(this.programs_++);
    this.js_.append(retval.getJsRef()).append("=ctx.createProgram();");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public WGLWidget.Renderbuffer getCreateRenderbuffer() {
    WGLWidget.Renderbuffer retval = new WGLWidget.Renderbuffer(this.renderbuffers_++);
    this.js_.append(retval.getJsRef()).append("=ctx.createRenderbuffer();");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public WGLWidget.Shader createShader(WGLWidget.GLenum shader) {
    WGLWidget.Shader retval = new WGLWidget.Shader(this.shaders_++);
    this.js_
        .append(retval.getJsRef())
        .append("=ctx.createShader(")
        .append("ctx." + shader.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public WGLWidget.Texture getCreateTexture() {
    WGLWidget.Texture retval = new WGLWidget.Texture(this.textures_++);
    this.js_.append("if (!").append(retval.getJsRef()).append("){");
    this.js_.append(retval.getJsRef()).append("=ctx.createTexture();");
    this.js_.append("\n}");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public WGLWidget.Texture createTextureAndLoad(final String url) {
    WGLWidget.Texture retval = new WGLWidget.Texture(this.textures_++);
    this.preloadImages_.add(new WClientGLWidget.PreloadImage(retval.getJsRef(), url, 0));
    return retval;
  }

  public WPaintDevice createPaintDevice(final WLength width, final WLength height) {
    return new WCanvasPaintDevice(width, height);
  }

  public void cullFace(WGLWidget.GLenum mode) {
    this.js_.append("ctx.cullFace(").append("ctx." + mode.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void deleteBuffer(WGLWidget.Buffer buffer) {
    if ((int) buffer.getId() >= this.buffers_) {
      return;
    }
    this.js_.append("ctx.deleteBuffer(").append(buffer.getJsRef()).append(");");
    this.js_.append("delete ").append(buffer.getJsRef()).append(";");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void deleteFramebuffer(WGLWidget.Framebuffer buffer) {
    if ((int) buffer.getId() >= this.framebuffers_) {
      return;
    }
    this.js_.append("ctx.deleteFramebuffer(").append(buffer.getJsRef()).append(");");
    this.js_.append("delete ").append(buffer.getJsRef()).append(";");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void deleteProgram(WGLWidget.Program program) {
    if ((int) program.getId() >= this.programs_) {
      return;
    }
    this.js_.append("ctx.deleteProgram(").append(program.getJsRef()).append(");");
    this.js_.append("delete ").append(program.getJsRef()).append(";");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void deleteRenderbuffer(WGLWidget.Renderbuffer buffer) {
    if ((int) buffer.getId() >= this.renderbuffers_) {
      return;
    }
    this.js_.append("ctx.deleteRenderbuffer(").append(buffer.getJsRef()).append(");");
    this.js_.append("delete ").append(buffer.getJsRef()).append(";");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void deleteShader(WGLWidget.Shader shader) {
    if ((int) shader.getId() >= this.shaders_) {
      return;
    }
    this.js_.append("ctx.deleteShader(").append(shader.getJsRef()).append(");");
    this.js_.append("delete ").append(shader.getJsRef()).append(";");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void deleteTexture(WGLWidget.Texture texture) {
    if ((int) texture.getId() >= this.textures_) {
      return;
    }
    this.js_.append("ctx.deleteTexture(").append(texture.getJsRef()).append(");");
    this.js_.append("delete ").append(texture.getJsRef()).append(";");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void depthFunc(WGLWidget.GLenum func) {
    this.js_.append("ctx.depthFunc(").append("ctx." + func.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void depthMask(boolean flag) {
    this.js_.append("ctx.depthMask(").append(flag ? "true" : "false").append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void depthRange(double zNear, double zFar) {
    char[] buf = new char[30];
    this.js_.append("ctx.depthRange(").append(WebGLUtils.makeFloat(zNear)).append(",");
    this.js_.append(WebGLUtils.makeFloat(zFar)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void detachShader(WGLWidget.Program program, WGLWidget.Shader shader) {
    if ((int) program.getId() >= this.programs_ || (int) shader.getId() >= this.shaders_) {
      return;
    }
    this.js_
        .append("if (")
        .append(program.getJsRef())
        .append(" && ")
        .append(shader.getJsRef())
        .append(") { ctx.detachShader(")
        .append(program.getJsRef())
        .append(",")
        .append(shader.getJsRef())
        .append("); }");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void disable(WGLWidget.GLenum cap) {
    this.js_.append("ctx.disable(").append("ctx." + cap.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void disableVertexAttribArray(WGLWidget.AttribLocation index) {
    this.js_.append("ctx.disableVertexAttribArray(").append(index.getJsRef()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void drawArrays(WGLWidget.GLenum mode, int first, int count) {
    this.js_
        .append("ctx.drawArrays(")
        .append("ctx." + mode.toString())
        .append(",")
        .append(String.valueOf(first))
        .append(",")
        .append(String.valueOf(count))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void drawElements(WGLWidget.GLenum mode, int count, WGLWidget.GLenum type, int offset) {
    this.js_
        .append("ctx.drawElements(")
        .append("ctx." + mode.toString())
        .append(",")
        .append(String.valueOf(count))
        .append(",")
        .append("ctx." + type.toString())
        .append(",")
        .append(String.valueOf(offset))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void enable(WGLWidget.GLenum cap) {
    this.js_.append("ctx.enable(").append("ctx." + cap.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void enableVertexAttribArray(WGLWidget.AttribLocation index) {
    this.js_.append("ctx.enableVertexAttribArray(").append(index.getJsRef()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void finish() {
    this.js_.append("ctx.finish();");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void flush() {
    this.js_.append("ctx.flush();");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void framebufferRenderbuffer(
      WGLWidget.GLenum target,
      WGLWidget.GLenum attachment,
      WGLWidget.GLenum renderbuffertarget,
      WGLWidget.Renderbuffer renderbuffer) {
    this.js_
        .append("ctx.framebufferRenderbuffer(")
        .append("ctx." + target.toString())
        .append(",")
        .append("ctx." + attachment.toString())
        .append(",")
        .append("ctx." + renderbuffertarget.toString())
        .append(",")
        .append(renderbuffer.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void framebufferTexture2D(
      WGLWidget.GLenum target,
      WGLWidget.GLenum attachment,
      WGLWidget.GLenum textarget,
      WGLWidget.Texture texture,
      int level) {
    this.js_
        .append("ctx.framebufferTexture2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append("ctx." + attachment.toString())
        .append(",")
        .append("ctx." + textarget.toString())
        .append(",")
        .append(texture.getJsRef())
        .append(",")
        .append(String.valueOf(level))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void frontFace(WGLWidget.GLenum mode) {
    this.js_.append("ctx.frontFace(").append("ctx." + mode.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void generateMipmap(WGLWidget.GLenum target) {
    this.js_.append("ctx.generateMipmap(").append("ctx." + target.toString()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public WGLWidget.AttribLocation getAttribLocation(
      WGLWidget.Program program, final String attrib) {
    WGLWidget.AttribLocation retval = new WGLWidget.AttribLocation(this.attributes_++);
    this.js_
        .append(retval.getJsRef())
        .append("=ctx.getAttribLocation(")
        .append(program.getJsRef())
        .append(",")
        .append(WWebWidget.jsStringLiteral(attrib))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public WGLWidget.UniformLocation getUniformLocation(
      WGLWidget.Program program, final String location) {
    WGLWidget.UniformLocation retval = new WGLWidget.UniformLocation(this.uniforms_++);
    this.js_
        .append(retval.getJsRef())
        .append("=ctx.getUniformLocation(")
        .append(program.getJsRef())
        .append(",")
        .append(WWebWidget.jsStringLiteral(location))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
    return retval;
  }

  public void hint(WGLWidget.GLenum target, WGLWidget.GLenum mode) {
    this.js_
        .append("ctx.hint(")
        .append("ctx." + target.toString())
        .append(",")
        .append("ctx." + mode.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void lineWidth(double width) {
    char[] buf = new char[30];
    if (!WApplication.getInstance().getEnvironment().agentIsIE()) {
      this.js_.append("ctx.lineWidth(").append(WebGLUtils.makeFloat(width)).append(");");
    }
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void linkProgram(WGLWidget.Program program) {
    this.js_.append("ctx.linkProgram(").append(program.getJsRef()).append(");");
    this.js_
        .append("if(!ctx.getProgramParameter(")
        .append(program.getJsRef())
        .append(",ctx.LINK_STATUS)){")
        .append("alert('Could not initialize shaders: ' + ctx.getProgramInfoLog(")
        .append(program.getJsRef())
        .append("));}");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void pixelStorei(WGLWidget.GLenum pname, int param) {
    this.js_
        .append("ctx.pixelStorei(")
        .append("ctx." + pname.toString())
        .append(",")
        .append(String.valueOf(param))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void polygonOffset(double factor, double units) {
    char[] buf = new char[30];
    this.js_.append("ctx.polygonOffset(").append(WebGLUtils.makeFloat(factor)).append(",");
    this.js_.append(WebGLUtils.makeFloat(units)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void renderbufferStorage(
      WGLWidget.GLenum target, WGLWidget.GLenum internalformat, int width, int height) {
    this.js_
        .append("ctx.renderbufferStorage(")
        .append("ctx." + target.toString())
        .append(",")
        .append("ctx." + internalformat.toString())
        .append(",")
        .append(String.valueOf(width))
        .append(",")
        .append(String.valueOf(height))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void sampleCoverage(double value, boolean invert) {
    char[] buf = new char[30];
    this.js_
        .append("ctx.sampleCoverage(")
        .append(WebGLUtils.makeFloat(value))
        .append(",")
        .append(invert ? "true" : "false")
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void scissor(int x, int y, int width, int height) {
    this.js_
        .append("ctx.scissor(")
        .append(String.valueOf(x))
        .append(",")
        .append(String.valueOf(y))
        .append(",")
        .append(String.valueOf(width))
        .append(",")
        .append(String.valueOf(height))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void shaderSource(WGLWidget.Shader shader, final String src) {
    this.js_
        .append("ctx.shaderSource(")
        .append(shader.getJsRef())
        .append(",")
        .append(WWebWidget.jsStringLiteral(src))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void stencilFunc(WGLWidget.GLenum func, int ref, int mask) {
    this.js_
        .append("ctx.stencilFunc(")
        .append("ctx." + func.toString())
        .append(",")
        .append(String.valueOf(ref))
        .append(",")
        .append(String.valueOf(mask))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void stencilFuncSeparate(WGLWidget.GLenum face, WGLWidget.GLenum func, int ref, int mask) {
    this.js_
        .append("ctx.stencilFuncSeparate(")
        .append("ctx." + face.toString())
        .append(",")
        .append("ctx." + func.toString())
        .append(",")
        .append(String.valueOf(ref))
        .append(",")
        .append(String.valueOf(mask))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void stencilMask(int mask) {
    this.js_.append("ctx.stencilMask(").append(String.valueOf(mask)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void stencilMaskSeparate(WGLWidget.GLenum face, int mask) {
    this.js_
        .append("ctx.stencilMaskSeparate(")
        .append("ctx." + face.toString())
        .append(",")
        .append(String.valueOf(mask))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void stencilOp(WGLWidget.GLenum fail, WGLWidget.GLenum zfail, WGLWidget.GLenum zpass) {
    this.js_
        .append("ctx.stencilOp(")
        .append("ctx." + fail.toString())
        .append(",")
        .append("ctx." + zfail.toString())
        .append(",")
        .append("ctx." + zpass.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void stencilOpSeparate(
      WGLWidget.GLenum face,
      WGLWidget.GLenum fail,
      WGLWidget.GLenum zfail,
      WGLWidget.GLenum zpass) {
    this.js_
        .append("ctx.stencilOpSeparate(")
        .append("ctx." + face.toString())
        .append(",")
        .append("ctx." + fail.toString())
        .append(",")
        .append("ctx." + zfail.toString())
        .append(",")
        .append("ctx." + zpass.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      int width,
      int height,
      int border,
      WGLWidget.GLenum format) {
    this.js_
        .append("ctx.texImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append("ctx." + internalformat.toString())
        .append(",")
        .append(String.valueOf(width))
        .append(",")
        .append(String.valueOf(height))
        .append(",")
        .append(String.valueOf(border))
        .append(",")
        .append("ctx." + format.toString())
        .append(",")
        .append("ctx." + WGLWidget.GLenum.UNSIGNED_BYTE.toString())
        .append(",null);");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WImage image) {
    this.js_
        .append("ctx.texImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append("ctx." + internalformat.toString())
        .append(",")
        .append("ctx." + format.toString())
        .append(",")
        .append("ctx." + type.toString())
        .append(",")
        .append(image.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WVideo video) {
    this.js_
        .append("if (")
        .append(video.getJsMediaRef())
        .append(") ctx.texImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append("ctx." + internalformat.toString())
        .append(",")
        .append("ctx." + format.toString())
        .append(",")
        .append("ctx." + type.toString())
        .append(",")
        .append(video.getJsMediaRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      String image) {
    int imgNb = this.images_++;
    WFileResource imgFile = new WFileResource("image/png", image);
    this.preloadImages_.add(
        new WClientGLWidget.PreloadImage(
            this.currentlyBoundTexture_.getJsRef(), imgFile.getUrl(), imgNb));
    this.js_
        .append("ctx.texImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append("ctx." + internalformat.toString())
        .append(",")
        .append("ctx." + format.toString())
        .append(",")
        .append("ctx." + type.toString())
        .append(",")
        .append(this.currentlyBoundTexture_.getJsRef())
        .append(".image")
        .append(String.valueOf(imgNb))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WPaintDevice paintdevice) {
    int imgNb = this.images_++;
    if (ObjectUtils.cast(paintdevice, WCanvasPaintDevice.class) != null) {
      WCanvasPaintDevice cpd = ObjectUtils.cast(paintdevice, WCanvasPaintDevice.class);
      String jsRef = this.currentlyBoundTexture_.getJsRef() + "Canvas";
      this.js_.append(jsRef).append("=document.createElement('canvas');");
      this.js_
          .append(jsRef)
          .append(".width=")
          .append(String.valueOf(cpd.getWidth().getValue()))
          .append(";");
      this.js_
          .append(jsRef)
          .append(".height=")
          .append(String.valueOf(cpd.getHeight().getValue()))
          .append(";");
      this.js_.append("var f = function(myCanvas) {");
      cpd.renderPaintCommands(this.js_, "myCanvas");
      this.js_.append("};");
      this.js_.append("f(").append(jsRef).append(");");
      this.js_
          .append(this.currentlyBoundTexture_.getJsRef())
          .append(".image")
          .append(String.valueOf(imgNb))
          .append("=")
          .append(jsRef)
          .append(";");
      this.js_.append("delete ").append(jsRef).append(";");
    } else {
      if (ObjectUtils.cast(paintdevice, WRasterPaintDevice.class) != null) {
        WRasterPaintDevice rpd = ObjectUtils.cast(paintdevice, WRasterPaintDevice.class);
        rpd.done();
        WResource mr = WebGLUtils.rpdToMemResource(rpd);
        this.preloadImages_.add(
            new WClientGLWidget.PreloadImage(
                this.currentlyBoundTexture_.getJsRef(), mr.getUrl(), imgNb));
      }
    }
    this.js_
        .append("ctx.texImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append("ctx." + internalformat.toString())
        .append(",")
        .append("ctx." + format.toString())
        .append(",")
        .append("ctx." + type.toString())
        .append(",")
        .append(this.currentlyBoundTexture_.getJsRef())
        .append(".image")
        .append(String.valueOf(imgNb))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void texImage2D(
      WGLWidget.GLenum target,
      int level,
      WGLWidget.GLenum internalformat,
      WGLWidget.GLenum format,
      WGLWidget.GLenum type,
      WGLWidget.Texture texture) {
    this.js_
        .append("ctx.texImage2D(")
        .append("ctx." + target.toString())
        .append(",")
        .append(String.valueOf(level))
        .append(",")
        .append("ctx." + internalformat.toString())
        .append(",")
        .append("ctx." + format.toString())
        .append(",")
        .append("ctx." + type.toString())
        .append(",")
        .append(texture.getJsRef())
        .append(".image0);");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void texParameteri(
      WGLWidget.GLenum target, WGLWidget.GLenum pname, WGLWidget.GLenum param) {
    this.js_
        .append("ctx.texParameteri(")
        .append("ctx." + target.toString())
        .append(",")
        .append("ctx." + pname.toString())
        .append(",")
        .append("ctx." + param.toString())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform1f(final WGLWidget.UniformLocation location, double x) {
    char[] buf = new char[30];
    this.js_
        .append("ctx.uniform1f(")
        .append(location.getJsRef())
        .append(",")
        .append(WebGLUtils.makeFloat(x))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform1fv(final WGLWidget.UniformLocation location, float[] value) {
    this.js_.append("ctx.uniform1fv(").append(location.getJsRef()).append(",");
    WebGLUtils.renderfv(this.js_, value, 1, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform1fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.js_
        .append("ctx.uniform1fv(")
        .append(location.getJsRef())
        .append(",")
        .append(v.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform1i(final WGLWidget.UniformLocation location, int x) {
    char[] buf = new char[30];
    this.js_.append("ctx.uniform1i(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeInt(x)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform1iv(final WGLWidget.UniformLocation location, int[] value) {
    this.js_.append("ctx.uniform1iv(").append(location.getJsRef()).append(",");
    renderiv(this.js_, value, 1, WGLWidget.GLenum.INT);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform2f(final WGLWidget.UniformLocation location, double x, double y) {
    char[] buf = new char[30];
    this.js_.append("ctx.uniform2f(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeFloat(x)).append(",");
    this.js_.append(WebGLUtils.makeFloat(y)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform2fv(final WGLWidget.UniformLocation location, float[] value) {
    this.js_.append("ctx.uniform2fv(").append(location.getJsRef()).append(",");
    WebGLUtils.renderfv(this.js_, value, 2, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform2fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.js_
        .append("ctx.uniform2fv(")
        .append(location.getJsRef())
        .append(",")
        .append(v.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform2i(final WGLWidget.UniformLocation location, int x, int y) {
    char[] buf = new char[30];
    this.js_.append("ctx.uniform2i(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeInt(x)).append(",");
    this.js_.append(WebGLUtils.makeInt(y)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform2iv(final WGLWidget.UniformLocation location, int[] value) {
    this.js_.append("ctx.uniform2iv(").append(location.getJsRef()).append(",");
    renderiv(this.js_, value, 2, WGLWidget.GLenum.INT);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform3f(final WGLWidget.UniformLocation location, double x, double y, double z) {
    char[] buf = new char[30];
    this.js_.append("ctx.uniform3f(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeFloat(x)).append(",");
    this.js_.append(WebGLUtils.makeFloat(y)).append(",");
    this.js_.append(WebGLUtils.makeFloat(z)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform3fv(final WGLWidget.UniformLocation location, float[] value) {
    this.js_.append("ctx.uniform3fv(").append(location.getJsRef()).append(",");
    WebGLUtils.renderfv(this.js_, value, 3, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform3fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.js_
        .append("ctx.uniform3fv(")
        .append(location.getJsRef())
        .append(",")
        .append(v.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform3i(final WGLWidget.UniformLocation location, int x, int y, int z) {
    char[] buf = new char[30];
    this.js_.append("ctx.uniform3i(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeInt(x)).append(",");
    this.js_.append(WebGLUtils.makeInt(y)).append(",");
    this.js_.append(WebGLUtils.makeInt(z)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform3iv(final WGLWidget.UniformLocation location, int[] value) {
    this.js_.append("ctx.uniform3iv(").append(location.getJsRef()).append(",");
    renderiv(this.js_, value, 3, WGLWidget.GLenum.INT);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform4f(
      final WGLWidget.UniformLocation location, double x, double y, double z, double w) {
    char[] buf = new char[30];
    this.js_.append("ctx.uniform4f(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeFloat(x)).append(",");
    this.js_.append(WebGLUtils.makeFloat(y)).append(",");
    this.js_.append(WebGLUtils.makeFloat(z)).append(",");
    this.js_.append(WebGLUtils.makeFloat(w)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform4fv(final WGLWidget.UniformLocation location, float[] value) {
    this.js_.append("ctx.uniform4fv(").append(location.getJsRef()).append(",");
    WebGLUtils.renderfv(this.js_, value, 4, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform4fv(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptVector v) {
    this.js_
        .append("ctx.uniform4fv(")
        .append(location.getJsRef())
        .append(",")
        .append(v.getJsRef())
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform4i(final WGLWidget.UniformLocation location, int x, int y, int z, int w) {
    char[] buf = new char[30];
    this.js_.append("ctx.uniform4i(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeInt(x)).append(",");
    this.js_.append(WebGLUtils.makeInt(y)).append(",");
    this.js_.append(WebGLUtils.makeInt(z)).append(",");
    this.js_.append(WebGLUtils.makeInt(w)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniform4iv(final WGLWidget.UniformLocation location, int[] value) {
    this.js_.append("ctx.uniform4iv(").append(location.getJsRef()).append(",");
    renderiv(this.js_, value, 4, WGLWidget.GLenum.INT);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniformMatrix2fv(
      final WGLWidget.UniformLocation location, boolean transpose, double[] value) {
    this.js_
        .append("ctx.uniformMatrix2fv(")
        .append(location.getJsRef())
        .append(",")
        .append(transpose ? "true" : "false")
        .append(",");
    WebGLUtils.renderfv(this.js_, value, 4, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniformMatrix2(final WGLWidget.UniformLocation location, final Matrix2f m) {
    this.js_.append("ctx.uniformMatrix2fv(").append(location.getJsRef()).append(",false,");
    Matrix2f t = WebGLUtils.transpose(m);
    WebGLUtils.renderfv(this.js_, t, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniformMatrix3fv(
      final WGLWidget.UniformLocation location, boolean transpose, double[] value) {
    this.js_
        .append("ctx.uniformMatrix3fv(")
        .append(location.getJsRef())
        .append(",")
        .append(transpose ? "true" : "false")
        .append(",");
    WebGLUtils.renderfv(this.js_, value, 9, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniformMatrix3(
      final WGLWidget.UniformLocation location, final javax.vecmath.Matrix3f m) {
    this.js_.append("ctx.uniformMatrix3fv(").append(location.getJsRef()).append(",false,");
    javax.vecmath.Matrix3f t = WebGLUtils.transpose(m);
    WebGLUtils.renderfv(this.js_, t, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniformMatrix4fv(
      final WGLWidget.UniformLocation location, boolean transpose, double[] value) {
    this.js_
        .append("ctx.uniformMatrix4fv(")
        .append(location.getJsRef())
        .append(",")
        .append(transpose ? "true" : "false")
        .append(",");
    WebGLUtils.renderfv(this.js_, value, 16, JsArrayType.Float32Array);
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniformMatrix4(
      final WGLWidget.UniformLocation location, final javax.vecmath.Matrix4f m) {
    this.js_.append("ctx.uniformMatrix4fv(").append(location.getJsRef()).append(",false,");
    this.js_.append("new Float32Array([");
    char[] buf = new char[30];
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        this.js_
            .append(i == 0 && j == 0 ? "" : ",")
            .append(WebGLUtils.makeFloat(m.getElement(j, i)));
      }
    }
    this.js_.append("])");
    this.js_.append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void uniformMatrix4(
      final WGLWidget.UniformLocation location, final WGLWidget.JavaScriptMatrix4x4 m) {
    this.js_.append("ctx.uniformMatrix4fv(").append(location.getJsRef()).append(",false,");
    this.js_.append(m.getJsRef()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void useProgram(WGLWidget.Program program) {
    this.js_.append("ctx.useProgram(").append(program.getJsRef()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void validateProgram(WGLWidget.Program program) {
    this.js_.append("ctx.validateProgram(").append(program.getJsRef()).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void vertexAttrib1f(WGLWidget.AttribLocation location, double x) {
    char[] buf = new char[30];
    this.js_.append("ctx.vertexAttrib1f(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeFloat(x)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void vertexAttrib2f(WGLWidget.AttribLocation location, double x, double y) {
    char[] buf = new char[30];
    this.js_.append("ctx.vertexAttrib2f(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeFloat(x)).append(",");
    this.js_.append(WebGLUtils.makeFloat(y)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void vertexAttrib3f(WGLWidget.AttribLocation location, double x, double y, double z) {
    char[] buf = new char[30];
    this.js_.append("ctx.vertexAttrib3f(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeFloat(x)).append(",");
    this.js_.append(WebGLUtils.makeFloat(y)).append(",");
    this.js_.append(WebGLUtils.makeFloat(z)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void vertexAttrib4f(
      WGLWidget.AttribLocation location, double x, double y, double z, double w) {
    char[] buf = new char[30];
    this.js_.append("ctx.vertexAttrib4f(").append(location.getJsRef()).append(",");
    this.js_.append(WebGLUtils.makeFloat(x)).append(",");
    this.js_.append(WebGLUtils.makeFloat(y)).append(",");
    this.js_.append(WebGLUtils.makeFloat(z)).append(",");
    this.js_.append(WebGLUtils.makeFloat(w)).append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void vertexAttribPointer(
      WGLWidget.AttribLocation location,
      int size,
      WGLWidget.GLenum type,
      boolean normalized,
      int stride,
      int offset) {
    this.js_
        .append("ctx.vertexAttribPointer(")
        .append(location.getJsRef())
        .append(",")
        .append(String.valueOf(size))
        .append(",")
        .append("ctx." + type.toString())
        .append(",")
        .append(normalized ? "true" : "false")
        .append(",")
        .append(String.valueOf(stride))
        .append(",")
        .append(String.valueOf(offset))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void viewport(int x, int y, int width, int height) {
    this.js_
        .append("ctx.viewport(")
        .append(String.valueOf(x))
        .append(",")
        .append(String.valueOf(y))
        .append(",")
        .append(String.valueOf(width))
        .append(",")
        .append(String.valueOf(height))
        .append(");");
    do {
      if (this.debugging_) {
        this.js_
            .append(
                "\n{var err = ctx.getError(); if(err != ctx.NO_ERROR && err != ctx.CONTEXT_LOST_WEBGL) {alert('error ")
            .append("(unknown)")
            .append(": ' + err); debugger;}}\n");
      }
    } while (false);
  }

  public void initJavaScriptMatrix4(final WGLWidget.JavaScriptMatrix4x4 mat) {
    if (!mat.hasContext()) {
      this.glInterface_.addJavaScriptMatrix4(mat);
    } else {
      if (mat.context_ != this.glInterface_) {
        throw new WException(
            "JavaScriptMatrix4x4: associated WGLWidget is not equal to the WGLWidget it's being initialized in");
      }
    }
    javax.vecmath.Matrix4f m = new javax.vecmath.Matrix4f(mat.getValue());
    this.js_.append(mat.getJsRef()).append("=");
    WebGLUtils.renderfv(this.js_, m, JsArrayType.Float32Array);
    this.js_.append(";");
    mat.initialize();
  }

  public void setJavaScriptMatrix4(
      final WGLWidget.JavaScriptMatrix4x4 jsm, final javax.vecmath.Matrix4f m) {
    this.js_.append("Wt4.11.2.glMatrix.mat4.set(");
    javax.vecmath.Matrix4f t = WebGLUtils.transpose(m);
    WebGLUtils.renderfv(this.js_, t, JsArrayType.Float32Array);
    this.js_.append(", ").append(jsm.getJsRef()).append(");");
  }

  public void initJavaScriptVector(final WGLWidget.JavaScriptVector vec) {
    if (!vec.hasContext()) {
      this.glInterface_.addJavaScriptVector(vec);
    } else {
      if (vec.context_ != this.glInterface_) {
        throw new WException(
            "JavaScriptVector: associated WGLWidget is not equal to the WGLWidget it's being initialized in");
      }
    }
    List<Float> v = vec.getValue();
    this.js_.append(vec.getJsRef()).append("= new Float32Array([");
    for (int i = 0; i < vec.getLength(); ++i) {
      String val = "";
      if (v.get(i) == Float.POSITIVE_INFINITY) {
        val = "Infinity";
      } else {
        if (v.get(i) == -Float.POSITIVE_INFINITY) {
          val = "-Infinity";
        } else {
          val = String.valueOf(v.get(i));
        }
      }
      if (i != 0) {
        this.js_.append(",");
      }
      this.js_.append(val);
    }
    this.js_.append("]);");
    vec.initialize();
  }

  public void setJavaScriptVector(final WGLWidget.JavaScriptVector jsv, final List<Float> v) {
    if (jsv.getLength() != v.size()) {
      throw new WException("Trying to set a JavaScriptVector with incompatible length!");
    }
    for (int i = 0; i < jsv.getLength(); ++i) {
      String val = "";
      if (v.get(i) == Float.POSITIVE_INFINITY) {
        val = "Infinity";
      } else {
        if (v.get(i) == -Float.POSITIVE_INFINITY) {
          val = "-Infinity";
        } else {
          val = String.valueOf(v.get(i));
        }
      }
      this.js_
          .append(jsv.getJsRef())
          .append("[")
          .append(String.valueOf(i))
          .append("] = ")
          .append(val)
          .append(";");
    }
  }

  public void setClientSideMouseHandler(final String handlerCode) {
    this.js_.append("obj.setMouseHandler(").append(handlerCode).append(");");
  }

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
    this.js_
        .append("obj.setMouseHandler(new obj.LookAtMouseHandler(")
        .append(m.getJsRef())
        .append(",[")
        .append(String.valueOf(centerX))
        .append(",")
        .append(String.valueOf(centerY))
        .append(",")
        .append(String.valueOf(centerZ))
        .append("],")
        .append("[")
        .append(String.valueOf(uX))
        .append(",")
        .append(String.valueOf(uY))
        .append(",")
        .append(String.valueOf(uZ))
        .append("],")
        .append(String.valueOf(pitchRate))
        .append(",")
        .append(String.valueOf(yawRate))
        .append("));");
  }

  public void setClientSideWalkHandler(
      final WGLWidget.JavaScriptMatrix4x4 m, double frontStep, double rotStep) {
    this.js_
        .append("obj.setMouseHandler(new obj.WalkMouseHandler(")
        .append(m.getJsRef())
        .append(",")
        .append(String.valueOf(frontStep))
        .append(",")
        .append(String.valueOf(rotStep))
        .append("));");
  }

  public JsArrayType getArrayType() {
    return JsArrayType.Float32Array;
  }

  public void injectJS(final String jsString) {
    this.js_.append(jsString);
  }

  public void restoreContext(final String jsRef) {
    StringWriter tmp = new StringWriter();
    tmp.append("{var o = ").append(this.glObjJsRef(jsRef)).append(";\n");
    this.shaders_ = 0;
    this.programs_ = 0;
    this.attributes_ = 0;
    this.uniforms_ = 0;
    for (int i = 0; i < this.buffers_; ++i) {
      tmp.append("o.ctx.WtBuffer").append(String.valueOf(i)).append("=null;");
    }
    this.buffers_ = 0;
    this.arrayBuffers_ = 0;
    this.framebuffers_ = 0;
    this.renderbuffers_ = 0;
    for (int i = 0; i < this.textures_; ++i) {
      tmp.append("o.ctx.WtTexture").append(String.valueOf(i)).append("=null;");
    }
    this.textures_ = 0;
    this.images_ = 0;
    this.canvas_ = 0;
    this.initializeGL(jsRef, tmp);
    tmp.append("o.initialized = false;}");
    WApplication.getInstance().doJavaScript(tmp.toString());
  }

  public void render(final String jsRef, EnumSet<RenderFlag> flags) {
    if (flags.contains(RenderFlag.Full)) {
      StringWriter tmp = new StringWriter();
      tmp.append("{\nvar o = new Wt4.11.2.WGLWidget(")
          .append(WApplication.getInstance().getJavaScriptClass())
          .append(",")
          .append(jsRef)
          .append(");\no.discoverContext(function(){")
          .append(this.webglNotAvailable_.createCall())
          .append("}, ")
          .append(
              this.glInterface_.renderOptions_.contains(GLRenderOption.AntiAliasing)
                  ? "true"
                  : "false")
          .append(");\n");
      this.initializeGL(jsRef, tmp);
      tmp.append("}\n");
      WApplication.getInstance().doJavaScript(tmp.toString());
    }
    if (this.updateGL_ || this.updateResizeGL_ || this.updatePaintGL_) {
      StringWriter tmp = new StringWriter();
      tmp.append("var o = ").append(this.glObjJsRef(jsRef)).append(";\nif(o.ctx){\n");
      if (this.updateGL_) {
        this.js_ = new StringWriter();
        this.glInterface_.updateGL();
        tmp.append("var update =function(){\nvar obj=")
            .append(this.glObjJsRef(jsRef))
            .append(";\nvar ctx=obj.ctx;if (!ctx) return;\n")
            .append(this.js_.toString())
            .append("\n};\no.updates.push(update);");
      }
      if (this.updateResizeGL_) {
        this.js_ = new StringWriter();
        this.glInterface_.resizeGL(this.renderWidth_, this.renderHeight_);
        tmp.append("o.resizeGL=function(){\nvar obj=")
            .append(this.glObjJsRef(jsRef))
            .append(";\nvar ctx=obj.ctx;if (!ctx) return;\n")
            .append(this.js_.toString())
            .append("};");
      }
      if (this.updatePaintGL_) {
        this.js_ = new StringWriter();
        this.glInterface_.paintGL();
        tmp.append("var updatePaint = function(){\n");
        tmp.append("var obj=").append(this.glObjJsRef(jsRef)).append(";\n");
        tmp.append("obj.paintGL=function(){\nvar obj=")
            .append(this.glObjJsRef(jsRef))
            .append(";\nvar ctx=obj.ctx;if (!ctx) return;\n")
            .append(this.js_.toString())
            .append("};");
        tmp.append("};\n");
        tmp.append("o.updates.push(updatePaint);");
      }
      this.js_ = new StringWriter();
      tmp.append("}\n");
      final boolean preloadingSomething =
          this.preloadImages_.size() > 0 || this.preloadArrayBuffers_.size() > 0;
      if (preloadingSomething) {
        if (this.preloadImages_.size() > 0) {
          tmp.append("o.preloadingTextures++;new ")
              .append(WApplication.getInstance().getJavaScriptClass())
              .append("._p_.ImagePreloader([");
          for (int i = 0; i < this.preloadImages_.size(); ++i) {
            if (i != 0) {
              tmp.append(',');
            }
            tmp.append('\'')
                .append(
                    WApplication.getInstance().resolveRelativeUrl(this.preloadImages_.get(i).url))
                .append('\'');
          }
          tmp.append("],function(images){\nvar o=")
              .append(this.glObjJsRef(jsRef))
              .append(";\nvar ctx=null;\nif(o) ctx=o.ctx;\nif(ctx == null) return;\n");
          for (int i = 0; i < this.preloadImages_.size(); ++i) {
            String texture = this.preloadImages_.get(i).jsRef;
            tmp.append(texture)
                .append("=ctx.createTexture();\n")
                .append(texture)
                .append(".image")
                .append(String.valueOf(this.preloadImages_.get(i).id))
                .append("=images[")
                .append(String.valueOf(i))
                .append("];\n");
          }
          tmp.append("o.preloadingTextures--;\n").append("o.handlePreload();\n").append("});");
          this.preloadImages_.clear();
        }
        if (this.preloadArrayBuffers_.size() > 0) {
          tmp.append("o.preloadingBuffers++;new ")
              .append(WApplication.getInstance().getJavaScriptClass())
              .append("._p_.ArrayBufferPreloader([");
          for (int i = 0; i < this.preloadArrayBuffers_.size(); ++i) {
            if (i != 0) {
              tmp.append(',');
            }
            tmp.append('\'').append(this.preloadArrayBuffers_.get(i).url).append('\'');
          }
          tmp.append("],function(bufferResources){\nvar o=")
              .append(this.glObjJsRef(jsRef))
              .append(";\nvar ctx=null;\n if(o) ctx=o.ctx;\nif(ctx == null) return;\n");
          for (int i = 0; i < this.preloadArrayBuffers_.size(); ++i) {
            String bufferResource = this.preloadArrayBuffers_.get(i).jsRef;
            tmp.append(bufferResource).append(" = ctx.createBuffer();");
            tmp.append("if (bufferResources[").append(String.valueOf(i)).append("]==null){");
            tmp.append(bufferResource).append(".data=[];\n");
            tmp.append("}else{");
            tmp.append(bufferResource)
                .append(".data=bufferResources[")
                .append(String.valueOf(i))
                .append("];\n");
            tmp.append("}");
          }
          tmp.append("o.preloadingBuffers--;").append("o.handlePreload();\n").append("});");
          this.preloadArrayBuffers_.clear();
        }
      } else {
        tmp.append("o.handlePreload();");
      }
      WApplication.getInstance().doJavaScript(tmp.toString());
      this.updateGL_ = this.updatePaintGL_ = this.updateResizeGL_ = false;
    }
  }

  private StringWriter js_;
  private int shaders_;
  private int programs_;
  private int attributes_;
  private int uniforms_;
  private int buffers_;
  private int arrayBuffers_;
  private int framebuffers_;
  private int renderbuffers_;
  private int textures_;
  private int images_;
  private int canvas_;
  private WGLWidget.Buffer currentlyBoundBuffer_;
  private WGLWidget.Texture currentlyBoundTexture_;
  private List<WResource> binaryResources_;

  static class PreloadImage {
    private static Logger logger = LoggerFactory.getLogger(PreloadImage.class);

    public PreloadImage(final String r, final String u, int i) {
      this.jsRef = r;
      this.url = u;
      this.id = i;
    }

    public String jsRef;
    public String url;
    public int id;
  }

  private List<WClientGLWidget.PreloadImage> preloadImages_;

  static class PreloadArrayBuffer {
    private static Logger logger = LoggerFactory.getLogger(PreloadArrayBuffer.class);

    public PreloadArrayBuffer(final String ref, final String u) {
      this.jsRef = ref;
      this.url = u;
    }

    public String jsRef;
    public String url;
  }

  private List<WClientGLWidget.PreloadArrayBuffer> preloadArrayBuffers_;

  private static String toString(WGLWidget.GLenum e) {
    switch (e) {
      case DEPTH_BUFFER_BIT:
        return "ctx.DEPTH_BUFFER_BIT";
      case STENCIL_BUFFER_BIT:
        return "ctx.STENCIL_BUFFER_BIT";
      case COLOR_BUFFER_BIT:
        return "ctx.COLOR_BUFFER_BIT";
      case POINTS:
        return "ctx.POINTS";
      case LINES:
        return "ctx.LINES";
      case LINE_LOOP:
        return "ctx.LINE_LOOP";
      case LINE_STRIP:
        return "ctx.LINE_STRIP";
      case TRIANGLES:
        return "ctx.TRIANGLES";
      case TRIANGLE_STRIP:
        return "ctx.TRIANGLE_STRIP";
      case TRIANGLE_FAN:
        return "ctx.TRIANGLE_FAN";
      case SRC_COLOR:
        return "ctx.SRC_COLOR";
      case ONE_MINUS_SRC_COLOR:
        return "ctx.ONE_MINUS_SRC_COLOR";
      case SRC_ALPHA:
        return "ctx.SRC_ALPHA";
      case ONE_MINUS_SRC_ALPHA:
        return "ctx.ONE_MINUS_SRC_ALPHA";
      case DST_ALPHA:
        return "ctx.DST_ALPHA";
      case ONE_MINUS_DST_ALPHA:
        return "ctx.ONE_MINUS_DST_ALPHA";
      case DST_COLOR:
        return "ctx.DST_COLOR";
      case ONE_MINUS_DST_COLOR:
        return "ctx.ONE_MINUS_DST_COLOR";
      case SRC_ALPHA_SATURATE:
        return "ctx.SRC_ALPHA_SATURATE";
      case FUNC_ADD:
        return "ctx.FUNC_ADD";
      case BLEND_EQUATION:
        return "ctx.BLEND_EQUATION";
      case BLEND_EQUATION_ALPHA:
        return "ctx.BLEND_EQUATION_ALPHA";
      case FUNC_SUBTRACT:
        return "ctx.FUNC_SUBTRACT";
      case FUNC_REVERSE_SUBTRACT:
        return "ctx.FUNC_REVERSE_SUBTRACT";
      case BLEND_DST_RGB:
        return "ctx.BLEND_DST_RGB";
      case BLEND_SRC_RGB:
        return "ctx.BLEND_SRC_RGB";
      case BLEND_DST_ALPHA:
        return "ctx.BLEND_DST_ALPHA";
      case BLEND_SRC_ALPHA:
        return "ctx.BLEND_SRC_ALPHA";
      case CONSTANT_COLOR:
        return "ctx.CONSTANT_COLOR";
      case ONE_MINUS_CONSTANT_COLOR:
        return "ctx.ONE_MINUS_CONSTANT_COLOR";
      case CONSTANT_ALPHA:
        return "ctx.CONSTANT_ALPHA";
      case ONE_MINUS_CONSTANT_ALPHA:
        return "ctx.ONE_MINUS_CONSTANT_ALPHA";
      case BLEND_COLOR:
        return "ctx.BLEND_COLOR";
      case ARRAY_BUFFER:
        return "ctx.ARRAY_BUFFER";
      case ELEMENT_ARRAY_BUFFER:
        return "ctx.ELEMENT_ARRAY_BUFFER";
      case ARRAY_BUFFER_BINDING:
        return "ctx.ARRAY_BUFFER_BINDING";
      case ELEMENT_ARRAY_BUFFER_BINDING:
        return "ctx.ELEMENT_ARRAY_BUFFER_BINDING";
      case STREAM_DRAW:
        return "ctx.STREAM_DRAW";
      case STATIC_DRAW:
        return "ctx.STATIC_DRAW";
      case DYNAMIC_DRAW:
        return "ctx.DYNAMIC_DRAW";
      case BUFFER_SIZE:
        return "ctx.BUFFER_SIZE";
      case BUFFER_USAGE:
        return "ctx.BUFFER_USAGE";
      case CURRENT_VERTEX_ATTRIB:
        return "ctx.CURRENT_VERTEX_ATTRIB";
      case FRONT:
        return "ctx.FRONT";
      case BACK:
        return "ctx.BACK";
      case FRONT_AND_BACK:
        return "ctx.FRONT_AND_BACK";
      case CULL_FACE:
        return "ctx.CULL_FACE";
      case BLEND:
        return "ctx.BLEND";
      case DITHER:
        return "ctx.DITHER";
      case STENCIL_TEST:
        return "ctx.STENCIL_TEST";
      case DEPTH_TEST:
        return "ctx.DEPTH_TEST";
      case SCISSOR_TEST:
        return "ctx.SCISSOR_TEST";
      case POLYGON_OFFSET_FILL:
        return "ctx.POLYGON_OFFSET_FILL";
      case SAMPLE_ALPHA_TO_COVERAGE:
        return "ctx.SAMPLE_ALPHA_TO_COVERAGE";
      case SAMPLE_COVERAGE:
        return "ctx.SAMPLE_COVERAGE";
      case INVALID_ENUM:
        return "ctx.INVALID_ENUM";
      case INVALID_VALUE:
        return "ctx.INVALID_VALUE";
      case INVALID_OPERATION:
        return "ctx.INVALID_OPERATION";
      case OUT_OF_MEMORY:
        return "ctx.OUT_OF_MEMORY";
      case CW:
        return "ctx.CW";
      case CCW:
        return "ctx.CCW";
      case LINE_WIDTH:
        return "ctx.LINE_WIDTH";
      case ALIASED_POINT_SIZE_RANGE:
        return "ctx.ALIASED_POINT_SIZE_RANGE";
      case ALIASED_LINE_WIDTH_RANGE:
        return "ctx.ALIASED_LINE_WIDTH_RANGE";
      case CULL_FACE_MODE:
        return "ctx.CULL_FACE_MODE";
      case FRONT_FACE:
        return "ctx.FRONT_FACE";
      case DEPTH_RANGE:
        return "ctx.DEPTH_RANGE";
      case DEPTH_WRITEMASK:
        return "ctx.DEPTH_WRITEMASK";
      case DEPTH_CLEAR_VALUE:
        return "ctx.DEPTH_CLEAR_VALUE";
      case DEPTH_FUNC:
        return "ctx.DEPTH_FUNC";
      case STENCIL_CLEAR_VALUE:
        return "ctx.STENCIL_CLEAR_VALUE";
      case STENCIL_FUNC:
        return "ctx.STENCIL_FUNC";
      case STENCIL_FAIL:
        return "ctx.STENCIL_FAIL";
      case STENCIL_PASS_DEPTH_FAIL:
        return "ctx.STENCIL_PASS_DEPTH_FAIL";
      case STENCIL_PASS_DEPTH_PASS:
        return "ctx.STENCIL_PASS_DEPTH_PASS";
      case STENCIL_REF:
        return "ctx.STENCIL_REF";
      case STENCIL_VALUE_MASK:
        return "ctx.STENCIL_VALUE_MASK";
      case STENCIL_WRITEMASK:
        return "ctx.STENCIL_WRITEMASK";
      case STENCIL_BACK_FUNC:
        return "ctx.STENCIL_BACK_FUNC";
      case STENCIL_BACK_FAIL:
        return "ctx.STENCIL_BACK_FAIL";
      case STENCIL_BACK_PASS_DEPTH_FAIL:
        return "ctx.STENCIL_BACK_PASS_DEPTH_FAIL";
      case STENCIL_BACK_PASS_DEPTH_PASS:
        return "ctx.STENCIL_BACK_PASS_DEPTH_PASS";
      case STENCIL_BACK_REF:
        return "ctx.STENCIL_BACK_REF";
      case STENCIL_BACK_VALUE_MASK:
        return "ctx.STENCIL_BACK_VALUE_MASK";
      case STENCIL_BACK_WRITEMASK:
        return "ctx.STENCIL_BACK_WRITEMASK";
      case VIEWPORT:
        return "ctx.VIEWPORT";
      case SCISSOR_BOX:
        return "ctx.SCISSOR_BOX";
      case COLOR_CLEAR_VALUE:
        return "ctx.COLOR_CLEAR_VALUE";
      case COLOR_WRITEMASK:
        return "ctx.COLOR_WRITEMASK";
      case UNPACK_ALIGNMENT:
        return "ctx.UNPACK_ALIGNMENT";
      case PACK_ALIGNMENT:
        return "ctx.PACK_ALIGNMENT";
      case MAX_TEXTURE_SIZE:
        return "ctx.MAX_TEXTURE_SIZE";
      case MAX_VIEWPORT_DIMS:
        return "ctx.MAX_VIEWPORT_DIMS";
      case SUBPIXEL_BITS:
        return "ctx.SUBPIXEL_BITS";
      case RED_BITS:
        return "ctx.RED_BITS";
      case GREEN_BITS:
        return "ctx.GREEN_BITS";
      case BLUE_BITS:
        return "ctx.BLUE_BITS";
      case ALPHA_BITS:
        return "ctx.ALPHA_BITS";
      case DEPTH_BITS:
        return "ctx.DEPTH_BITS";
      case STENCIL_BITS:
        return "ctx.STENCIL_BITS";
      case POLYGON_OFFSET_UNITS:
        return "ctx.POLYGON_OFFSET_UNITS";
      case POLYGON_OFFSET_FACTOR:
        return "ctx.POLYGON_OFFSET_FACTOR";
      case TEXTURE_BINDING_2D:
        return "ctx.TEXTURE_BINDING_2D";
      case SAMPLE_BUFFERS:
        return "ctx.SAMPLE_BUFFERS";
      case SAMPLES:
        return "ctx.SAMPLES";
      case SAMPLE_COVERAGE_VALUE:
        return "ctx.SAMPLE_COVERAGE_VALUE";
      case SAMPLE_COVERAGE_INVERT:
        return "ctx.SAMPLE_COVERAGE_INVERT";
      case NUM_COMPRESSED_TEXTURE_FORMATS:
        return "ctx.NUM_COMPRESSED_TEXTURE_FORMATS";
      case COMPRESSED_TEXTURE_FORMATS:
        return "ctx.COMPRESSED_TEXTURE_FORMATS";
      case DONT_CARE:
        return "ctx.DONT_CARE";
      case FASTEST:
        return "ctx.FASTEST";
      case NICEST:
        return "ctx.NICEST";
      case GENERATE_MIPMAP_HINT:
        return "ctx.GENERATE_MIPMAP_HINT";
      case BYTE:
        return "ctx.BYTE";
      case UNSIGNED_BYTE:
        return "ctx.UNSIGNED_BYTE";
      case SHORT:
        return "ctx.SHORT";
      case UNSIGNED_SHORT:
        return "ctx.UNSIGNED_SHORT";
      case INT:
        return "ctx.INT";
      case UNSIGNED_INT:
        return "ctx.UNSIGNED_INT";
      case FLOAT:
        return "ctx.FLOAT";
      case DEPTH_COMPONENT:
        return "ctx.DEPTH_COMPONENT";
      case ALPHA:
        return "ctx.ALPHA";
      case RGB:
        return "ctx.RGB";
      case RGBA:
        return "ctx.RGBA";
      case LUMINANCE:
        return "ctx.LUMINANCE";
      case LUMINANCE_ALPHA:
        return "ctx.LUMINANCE_ALPHA";
      case UNSIGNED_SHORT_4_4_4_4:
        return "ctx.UNSIGNED_SHORT_4_4_4_4";
      case UNSIGNED_SHORT_5_5_5_1:
        return "ctx.UNSIGNED_SHORT_5_5_5_1";
      case UNSIGNED_SHORT_5_6_5:
        return "ctx.UNSIGNED_SHORT_5_6_5";
      case FRAGMENT_SHADER:
        return "ctx.FRAGMENT_SHADER";
      case VERTEX_SHADER:
        return "ctx.VERTEX_SHADER";
      case MAX_VERTEX_ATTRIBS:
        return "ctx.MAX_VERTEX_ATTRIBS";
      case MAX_VERTEX_UNIFORM_VECTORS:
        return "ctx.MAX_VERTEX_UNIFORM_VECTORS";
      case MAX_VARYING_VECTORS:
        return "ctx.MAX_VARYING_VECTORS";
      case MAX_COMBINED_TEXTURE_IMAGE_UNITS:
        return "ctx.MAX_COMBINED_TEXTURE_IMAGE_UNITS";
      case MAX_VERTEX_TEXTURE_IMAGE_UNITS:
        return "ctx.MAX_VERTEX_TEXTURE_IMAGE_UNITS";
      case MAX_TEXTURE_IMAGE_UNITS:
        return "ctx.MAX_TEXTURE_IMAGE_UNITS";
      case MAX_FRAGMENT_UNIFORM_VECTORS:
        return "ctx.MAX_FRAGMENT_UNIFORM_VECTORS";
      case SHADER_TYPE:
        return "ctx.SHADER_TYPE";
      case DELETE_STATUS:
        return "ctx.DELETE_STATUS";
      case LINK_STATUS:
        return "ctx.LINK_STATUS";
      case VALIDATE_STATUS:
        return "ctx.VALIDATE_STATUS";
      case ATTACHED_SHADERS:
        return "ctx.ATTACHED_SHADERS";
      case ACTIVE_UNIFORMS:
        return "ctx.ACTIVE_UNIFORMS";
      case ACTIVE_UNIFORM_MAX_LENGTH:
        return "ctx.ACTIVE_UNIFORM_MAX_LENGTH";
      case ACTIVE_ATTRIBUTES:
        return "ctx.ACTIVE_ATTRIBUTES";
      case ACTIVE_ATTRIBUTE_MAX_LENGTH:
        return "ctx.ACTIVE_ATTRIBUTE_MAX_LENGTH";
      case SHADING_LANGUAGE_VERSION:
        return "ctx.SHADING_LANGUAGE_VERSION";
      case CURRENT_PROGRAM:
        return "ctx.CURRENT_PROGRAM";
      case NEVER:
        return "ctx.NEVER";
      case LESS:
        return "ctx.LESS";
      case EQUAL:
        return "ctx.EQUAL";
      case LEQUAL:
        return "ctx.LEQUAL";
      case GREATER:
        return "ctx.GREATER";
      case NOTEQUAL:
        return "ctx.NOTEQUAL";
      case GEQUAL:
        return "ctx.GEQUAL";
      case ALWAYS:
        return "ctx.ALWAYS";
      case KEEP:
        return "ctx.KEEP";
      case REPLACE:
        return "ctx.REPLACE";
      case INCR:
        return "ctx.INCR";
      case DECR:
        return "ctx.DECR";
      case INVERT:
        return "ctx.INVERT";
      case INCR_WRAP:
        return "ctx.INCR_WRAP";
      case DECR_WRAP:
        return "ctx.DECR_WRAP";
      case VENDOR:
        return "ctx.VENDOR";
      case RENDERER:
        return "ctx.RENDERER";
      case VERSION:
        return "ctx.VERSION";
      case NEAREST:
        return "ctx.NEAREST";
      case LINEAR:
        return "ctx.LINEAR";
      case NEAREST_MIPMAP_NEAREST:
        return "ctx.NEAREST_MIPMAP_NEAREST";
      case LINEAR_MIPMAP_NEAREST:
        return "ctx.LINEAR_MIPMAP_NEAREST";
      case NEAREST_MIPMAP_LINEAR:
        return "ctx.NEAREST_MIPMAP_LINEAR";
      case LINEAR_MIPMAP_LINEAR:
        return "ctx.LINEAR_MIPMAP_LINEAR";
      case TEXTURE_MAG_FILTER:
        return "ctx.TEXTURE_MAG_FILTER";
      case TEXTURE_MIN_FILTER:
        return "ctx.TEXTURE_MIN_FILTER";
      case TEXTURE_WRAP_S:
        return "ctx.TEXTURE_WRAP_S";
      case TEXTURE_WRAP_T:
        return "ctx.TEXTURE_WRAP_T";
      case TEXTURE_2D:
        return "ctx.TEXTURE_2D";
      case TEXTURE:
        return "ctx.TEXTURE";
      case TEXTURE_CUBE_MAP:
        return "ctx.TEXTURE_CUBE_MAP";
      case TEXTURE_BINDING_CUBE_MAP:
        return "ctx.TEXTURE_BINDING_CUBE_MAP";
      case TEXTURE_CUBE_MAP_POSITIVE_X:
        return "ctx.TEXTURE_CUBE_MAP_POSITIVE_X";
      case TEXTURE_CUBE_MAP_NEGATIVE_X:
        return "ctx.TEXTURE_CUBE_MAP_NEGATIVE_X";
      case TEXTURE_CUBE_MAP_POSITIVE_Y:
        return "ctx.TEXTURE_CUBE_MAP_POSITIVE_Y";
      case TEXTURE_CUBE_MAP_NEGATIVE_Y:
        return "ctx.TEXTURE_CUBE_MAP_NEGATIVE_Y";
      case TEXTURE_CUBE_MAP_POSITIVE_Z:
        return "ctx.TEXTURE_CUBE_MAP_POSITIVE_Z";
      case TEXTURE_CUBE_MAP_NEGATIVE_Z:
        return "ctx.TEXTURE_CUBE_MAP_NEGATIVE_Z";
      case MAX_CUBE_MAP_TEXTURE_SIZE:
        return "ctx.MAX_CUBE_MAP_TEXTURE_SIZE";
      case TEXTURE0:
        return "ctx.TEXTURE0";
      case TEXTURE1:
        return "ctx.TEXTURE1";
      case TEXTURE2:
        return "ctx.TEXTURE2";
      case TEXTURE3:
        return "ctx.TEXTURE3";
      case TEXTURE4:
        return "ctx.TEXTURE4";
      case TEXTURE5:
        return "ctx.TEXTURE5";
      case TEXTURE6:
        return "ctx.TEXTURE6";
      case TEXTURE7:
        return "ctx.TEXTURE7";
      case TEXTURE8:
        return "ctx.TEXTURE8";
      case TEXTURE9:
        return "ctx.TEXTURE9";
      case TEXTURE10:
        return "ctx.TEXTURE10";
      case TEXTURE11:
        return "ctx.TEXTURE11";
      case TEXTURE12:
        return "ctx.TEXTURE12";
      case TEXTURE13:
        return "ctx.TEXTURE13";
      case TEXTURE14:
        return "ctx.TEXTURE14";
      case TEXTURE15:
        return "ctx.TEXTURE15";
      case TEXTURE16:
        return "ctx.TEXTURE16";
      case TEXTURE17:
        return "ctx.TEXTURE17";
      case TEXTURE18:
        return "ctx.TEXTURE18";
      case TEXTURE19:
        return "ctx.TEXTURE19";
      case TEXTURE20:
        return "ctx.TEXTURE20";
      case TEXTURE21:
        return "ctx.TEXTURE21";
      case TEXTURE22:
        return "ctx.TEXTURE22";
      case TEXTURE23:
        return "ctx.TEXTURE23";
      case TEXTURE24:
        return "ctx.TEXTURE24";
      case TEXTURE25:
        return "ctx.TEXTURE25";
      case TEXTURE26:
        return "ctx.TEXTURE26";
      case TEXTURE27:
        return "ctx.TEXTURE27";
      case TEXTURE28:
        return "ctx.TEXTURE28";
      case TEXTURE29:
        return "ctx.TEXTURE29";
      case TEXTURE30:
        return "ctx.TEXTURE30";
      case TEXTURE31:
        return "ctx.TEXTURE31";
      case ACTIVE_TEXTURE:
        return "ctx.ACTIVE_TEXTURE";
      case REPEAT:
        return "ctx.REPEAT";
      case CLAMP_TO_EDGE:
        return "ctx.CLAMP_TO_EDGE";
      case MIRRORED_REPEAT:
        return "ctx.MIRRORED_REPEAT";
      case FLOAT_VEC2:
        return "ctx.FLOAT_VEC2";
      case FLOAT_VEC3:
        return "ctx.FLOAT_VEC3";
      case FLOAT_VEC4:
        return "ctx.FLOAT_VEC4";
      case INT_VEC2:
        return "ctx.INT_VEC2";
      case INT_VEC3:
        return "ctx.INT_VEC3";
      case INT_VEC4:
        return "ctx.INT_VEC4";
      case BOOL:
        return "ctx.BOOL";
      case BOOL_VEC2:
        return "ctx.BOOL_VEC2";
      case BOOL_VEC3:
        return "ctx.BOOL_VEC3";
      case BOOL_VEC4:
        return "ctx.BOOL_VEC4";
      case FLOAT_MAT2:
        return "ctx.FLOAT_MAT2";
      case FLOAT_MAT3:
        return "ctx.FLOAT_MAT3";
      case FLOAT_MAT4:
        return "ctx.FLOAT_MAT4";
      case SAMPLER_2D:
        return "ctx.SAMPLER_2D";
      case SAMPLER_CUBE:
        return "ctx.SAMPLER_CUBE";
      case VERTEX_ATTRIB_ARRAY_ENABLED:
        return "ctx.VERTEX_ATTRIB_ARRAY_ENABLED";
      case VERTEX_ATTRIB_ARRAY_SIZE:
        return "ctx.VERTEX_ATTRIB_ARRAY_SIZE";
      case VERTEX_ATTRIB_ARRAY_STRIDE:
        return "ctx.VERTEX_ATTRIB_ARRAY_STRIDE";
      case VERTEX_ATTRIB_ARRAY_TYPE:
        return "ctx.VERTEX_ATTRIB_ARRAY_TYPE";
      case VERTEX_ATTRIB_ARRAY_NORMALIZED:
        return "ctx.VERTEX_ATTRIB_ARRAY_NORMALIZED";
      case VERTEX_ATTRIB_ARRAY_POINTER:
        return "ctx.VERTEX_ATTRIB_ARRAY_POINTER";
      case VERTEX_ATTRIB_ARRAY_BUFFER_BINDING:
        return "ctx.VERTEX_ATTRIB_ARRAY_BUFFER_BINDING";
      case COMPILE_STATUS:
        return "ctx.COMPILE_STATUS";
      case INFO_LOG_LENGTH:
        return "ctx.INFO_LOG_LENGTH";
      case SHADER_SOURCE_LENGTH:
        return "ctx.SHADER_SOURCE_LENGTH";
      case LOW_FLOAT:
        return "ctx.LOW_FLOAT";
      case MEDIUM_FLOAT:
        return "ctx.MEDIUM_FLOAT";
      case HIGH_FLOAT:
        return "ctx.HIGH_FLOAT";
      case LOW_INT:
        return "ctx.LOW_INT";
      case MEDIUM_INT:
        return "ctx.MEDIUM_INT";
      case HIGH_INT:
        return "ctx.HIGH_INT";
      case FRAMEBUFFER:
        return "ctx.FRAMEBUFFER";
      case RENDERBUFFER:
        return "ctx.RENDERBUFFER";
      case RGBA4:
        return "ctx.RGBA4";
      case RGB5_A1:
        return "ctx.RGB5_A1";
      case RGB565:
        return "ctx.RGB565";
      case DEPTH_COMPONENT16:
        return "ctx.DEPTH_COMPONENT16";
      case STENCIL_INDEX:
        return "ctx.STENCIL_INDEX";
      case STENCIL_INDEX8:
        return "ctx.STENCIL_INDEX8";
      case DEPTH_STENCIL:
        return "ctx.DEPTH_STENCIL";
      case RENDERBUFFER_WIDTH:
        return "ctx.RENDERBUFFER_WIDTH";
      case RENDERBUFFER_HEIGHT:
        return "ctx.RENDERBUFFER_HEIGHT";
      case RENDERBUFFER_INTERNAL_FORMAT:
        return "ctx.RENDERBUFFER_INTERNAL_FORMAT";
      case RENDERBUFFER_RED_SIZE:
        return "ctx.RENDERBUFFER_RED_SIZE";
      case RENDERBUFFER_GREEN_SIZE:
        return "ctx.RENDERBUFFER_GREEN_SIZE";
      case RENDERBUFFER_BLUE_SIZE:
        return "ctx.RENDERBUFFER_BLUE_SIZE";
      case RENDERBUFFER_ALPHA_SIZE:
        return "ctx.RENDERBUFFER_ALPHA_SIZE";
      case RENDERBUFFER_DEPTH_SIZE:
        return "ctx.RENDERBUFFER_DEPTH_SIZE";
      case RENDERBUFFER_STENCIL_SIZE:
        return "ctx.RENDERBUFFER_STENCIL_SIZE";
      case FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE:
        return "ctx.FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE";
      case FRAMEBUFFER_ATTACHMENT_OBJECT_NAME:
        return "ctx.FRAMEBUFFER_ATTACHMENT_OBJECT_NAME";
      case FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL:
        return "ctx.FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL";
      case FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE:
        return "ctx.FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE";
      case COLOR_ATTACHMENT0:
        return "ctx.COLOR_ATTACHMENT0";
      case DEPTH_ATTACHMENT:
        return "ctx.DEPTH_ATTACHMENT";
      case STENCIL_ATTACHMENT:
        return "ctx.STENCIL_ATTACHMENT";
      case DEPTH_STENCIL_ATTACHMENT:
        return "ctx.DEPTH_STENCIL_ATTACHMENT";
      case FRAMEBUFFER_COMPLETE:
        return "ctx.FRAMEBUFFER_COMPLETE";
      case FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
        return "ctx.FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
      case FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
        return "ctx.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
      case FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
        return "ctx.FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
      case FRAMEBUFFER_UNSUPPORTED:
        return "ctx.FRAMEBUFFER_UNSUPPORTED";
      case FRAMEBUFFER_BINDING:
        return "ctx.FRAMEBUFFER_BINDING";
      case RENDERBUFFER_BINDING:
        return "ctx.RENDERBUFFER_BINDING";
      case MAX_RENDERBUFFER_SIZE:
        return "ctx.MAX_RENDERBUFFER_SIZE";
      case INVALID_FRAMEBUFFER_OPERATION:
        return "ctx.INVALID_FRAMEBUFFER_OPERATION";
      case UNPACK_FLIP_Y_WEBGL:
        return "ctx.UNPACK_FLIP_Y_WEBGL";
      case UNPACK_PREMULTIPLY_ALPHA_WEBGL:
        return "ctx.UNPACK_PREMULTIPLY_ALPHA_WEBGL";
      case CONTEXT_LOST_WEBGL:
        return "ctx.CONTEXT_LOST_WEBGL";
      case UNPACK_COLORSPACE_CONVERSION_WEBGL:
        return "ctx.UNPACK_COLORSPACE_CONVERSION_WEBGL";
      case BROWSER_DEFAULT_WEBGL:
        return "ctx.BROWSER_DEFAULT_WEBGL";
    }
    return "BAD_GL_ENUM";
  }
  // private WResource (WRasterPaintDevice  rpd) ;
  private static void renderiv(final Writer os, final java.nio.IntBuffer a, WGLWidget.GLenum type) {
    try {
      switch (type) {
        case BYTE:
          os.append("new Int8Array([");
          break;
        case UNSIGNED_BYTE:
          os.append("new Uint8Array([");
          break;
        case SHORT:
          os.append("new Int16Array([");
          break;
        case UNSIGNED_SHORT:
          os.append("new Uint16Array([");
          break;
        case INT:
          os.append("new Int32Array([");
          break;
        default:
        case UNSIGNED_INT:
          os.append("new Uint32Array([");
          break;
      }
      char[] buf = new char[30];
      int i;
      for (i = 0; i < a.capacity(); i++) {
        os.append(i == 0 ? "" : ",").append(WebGLUtils.makeInt(a.get(i)));
      }
      os.append("])");
    } catch (IOException ioe) {
      logger.info("Ignoring exception {}", ioe.getMessage(), ioe);
    }
  }

  private static void renderiv(final Writer os, int[] a, int size, WGLWidget.GLenum type) {
    try {
      switch (type) {
        case BYTE:
          os.append("new Int8Array([");
          break;
        case UNSIGNED_BYTE:
          os.append("new Uint8Array([");
          break;
        case SHORT:
          os.append("new Int16Array([");
          break;
        case UNSIGNED_SHORT:
          os.append("new Uint16Array([");
          break;
        case INT:
          os.append("new Int32Array([");
          break;
        default:
        case UNSIGNED_INT:
          os.append("new Uint32Array([");
          break;
      }
      char[] buf = new char[30];
      int i;
      for (i = 0; i < size; i++) {
        os.append(i == 0 ? "" : ",").append(WebGLUtils.makeInt(a[i]));
      }
      os.append("])");
    } catch (IOException ioe) {
      logger.info("Ignoring exception {}", ioe.getMessage(), ioe);
    }
  }

  private String glObjJsRef(final String jsRef) {
    return "(function(){var r = "
        + jsRef
        + ";var o = r ? r.wtObj : null;return o ? o : {ctx: null};})()";
  }

  private void initializeGL(final String jsRef, final StringWriter ss) {
    this.js_ = new StringWriter();
    this.glInterface_.initializeGL();
    ss.append("o.initializeGL=function(){\nvar obj=")
        .append(this.glObjJsRef(jsRef))
        .append(";\nvar ctx=obj.ctx; if(!ctx) return;\n")
        .append("")
        .append(this.js_.toString())
        .append(
            "obj.initialized = true;\nfor(const update of obj.updates) update();\nobj.updates = new Array();\nobj.resizeGL();\n};\n");
  }
}
