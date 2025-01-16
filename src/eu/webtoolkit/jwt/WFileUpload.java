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
 * A widget that allows a file to be uploaded.
 *
 * <p>This widget is displayed as a box in which a filename can be entered and a browse button.
 *
 * <p>Depending on availability of JavaScript, the behaviour of the widget is different, but the API
 * is designed in a way which facilitates a portable use.
 *
 * <p>When JavaScript is available, the file will not be uploaded until {@link WFileUpload#upload()
 * upload()} is called. This will start an asynchronous upload (and thus return immediately). When
 * no JavaScript is available, the file will be uploaded with the next click event. Thus, {@link
 * WFileUpload#upload() upload()} has no effect &ndash; the file will already be uploaded, and the
 * corresponding signals will already be emitted. To test if {@link WFileUpload#upload() upload()}
 * will start an upload, you may check using the {@link WFileUpload#canUpload() canUpload()} call.
 *
 * <p>Thus, to properly use the widget, one needs to follow these rules:
 *
 * <ul>
 *   <li>Be prepared to handle the {@link WFileUpload#uploaded() uploaded()} or {@link
 *       WFileUpload#fileTooLarge() fileTooLarge()} signals also when {@link WFileUpload#upload()
 *       upload()} was not called.
 *   <li>Check using {@link WFileUpload#canUpload() canUpload()} if {@link WFileUpload#upload()
 *       upload()} will schedule a new upload. if (!canUpload()) then {@link WFileUpload#upload()
 *       upload()} will not have any effect. if ({@link WFileUpload#canUpload() canUpload()}),
 *       {@link WFileUpload#upload() upload()} will start a new file upload, which completes
 *       succesfully using an {@link WFileUpload#uploaded() uploaded()} signal or a {@link
 *       WFileUpload#fileTooLarge() fileTooLarge()} signals gets emitted.
 * </ul>
 *
 * <p>The WFileUpload widget must be hidden or deleted when a file is received. In addition it is
 * wise to prevent the user from uploading the file twice as in the example below.
 *
 * <p>The uploaded file is automatically spooled to a local temporary file which will be deleted
 * together with the {@link WFileUpload} widget, unless {@link WFileUpload#stealSpooledFile()
 * stealSpooledFile()} is called.
 *
 * <p>WFileUpload is an {@link WWidget#setInline(boolean inlined) inline } widget.
 *
 * <p>
 *
 * <h3>CSS</h3>
 *
 * <p>The file upload itself corresponds to a <code>&lt;input type=&quot;file&quot;&gt;</code> tag,
 * but may be wrapped in a <code>&lt;form&gt;</code> tag for an Ajax session to implement the
 * asynchronous upload action. This widget does not provide styling, and styling through CSS is not
 * well supported across browsers.
 */
public class WFileUpload extends WWebWidget {
  private static Logger logger = LoggerFactory.getLogger(WFileUpload.class);

  /** Creates a file upload widget. */
  public WFileUpload(WContainerWidget parentContainer) {
    super();
    this.flags_ = new BitSet();
    this.textSize_ = 20;
    this.uploadedFiles_ = new ArrayList<UploadedFile>();
    this.fileTooLarge_ = new JSignal1<Long>(this, "fileTooLarge") {};
    this.dataReceived_ = new Signal2<Long, Long>();
    this.displayWidget_ = (WInteractWidget) null;
    this.displayWidgetRedirect_ = new JSlot(this);
    this.fileUploadTarget_ = null;
    this.containedProgressBar_ = null;
    this.progressBar_ = null;
    this.acceptAttributes_ = "";
    WApplication app = WApplication.getInstance();
    if (app != null) {
      WBootstrap5Theme bs5Theme = ObjectUtils.cast(app.getTheme(), WBootstrap5Theme.class);
      if (bs5Theme != null) {
        super.setInline(false);
      } else {
        super.setInline(true);
      }
    }
    this.create();
    if (parentContainer != null) parentContainer.addWidget(this);
  }
  /**
   * Creates a file upload widget.
   *
   * <p>Calls {@link #WFileUpload(WContainerWidget parentContainer) this((WContainerWidget)null)}
   */
  public WFileUpload() {
    this((WContainerWidget) null);
  }

  public void remove() {
    if (this.flags_.get(BIT_UPLOADING)) {
      WApplication.getInstance().enableUpdates(false);
    }
    {
      WWidget oldWidget = this.containedProgressBar_;
      this.containedProgressBar_ = null;
      {
        WWidget toRemove = this.manageWidget(oldWidget, this.containedProgressBar_);
        if (toRemove != null) toRemove.remove();
      }
    }
    super.remove();
  }
  /**
   * Sets whether the file upload accepts multiple files.
   *
   * <p>In browsers which support the &quot;multiple&quot; attribute for the file upload (to be part
   * of HTML5) control, this will allow the user to select multiple files at once.
   *
   * <p>All uploaded files are available from {@link WFileUpload#getUploadedFiles()
   * getUploadedFiles()}. The single-file API will return only information on the first uploaded
   * file.
   *
   * <p>The default value is <code>false</code>.
   */
  public void setMultiple(boolean multiple) {
    this.flags_.set(BIT_MULTIPLE, multiple);
  }
  /**
   * Returns whether multiple files can be uploaded.
   *
   * <p>
   *
   * @see WFileUpload#setMultiple(boolean multiple)
   */
  public boolean isMultiple() {
    return this.flags_.get(BIT_MULTIPLE);
  }
  /** Sets the size of the file input. */
  public void setFileTextSize(int chars) {
    this.textSize_ = chars;
  }
  /** Returns the size of the file input. */
  public int getFileTextSize() {
    return this.textSize_;
  }
  /**
   * Returns the spooled location of the uploaded file.
   *
   * <p>Returns the temporary filename in which the uploaded file was spooled. The file is
   * guaranteed to exist as long as the {@link WFileUpload} widget is not deleted, or a new file is
   * not uploaded.
   *
   * <p>When multiple files were uploaded, this returns the information from the first file.
   *
   * <p>
   *
   * @see WFileUpload#stealSpooledFile()
   * @see WFileUpload#uploaded()
   */
  public String getSpoolFileName() {
    if (!this.isEmpty()) {
      return this.uploadedFiles_.get(0).getSpoolFileName();
    } else {
      return "";
    }
  }
  /**
   * Returns the client filename.
   *
   * <p>When multiple files were uploaded, this returns the information from the first file.
   *
   * <p>
   *
   * <p><i><b>Note: </b>Depending on the browser this is an absolute path or only the file name.
   * </i>
   */
  public String getClientFileName() {
    if (!this.isEmpty()) {
      return this.uploadedFiles_.get(0).getClientFileName();
    } else {
      return "";
    }
  }
  /**
   * Returns the client content description.
   *
   * <p>When multiple files were uploaded, this returns the information from the first file.
   */
  public String getContentDescription() {
    if (!this.isEmpty()) {
      return this.uploadedFiles_.get(0).getContentType();
    } else {
      return "";
    }
  }
  /**
   * Steals the spooled file.
   *
   * <p>By stealing the file, the spooled file will no longer be deleted together with this widget,
   * which means you need to take care of managing that.
   *
   * <p>When multiple files were uploaded, this returns the information from the first file.
   */
  public void stealSpooledFile() {
    if (!this.isEmpty()) {
      this.uploadedFiles_.get(0).stealSpoolFile();
    }
  }
  /** Returns whether one or more files have been uploaded. */
  public boolean isEmpty() {
    return this.uploadedFiles_.isEmpty();
  }
  /** Returns the uploaded files. */
  public List<UploadedFile> getUploadedFiles() {
    return this.uploadedFiles_;
  }
  /**
   * Returns whether {@link WFileUpload#upload() upload()} will start a new file upload.
   *
   * <p>A call to {@link WFileUpload#upload() upload()} will only start a new file upload if there
   * is no JavaScript support. Otherwise, the most recent file will already be uploaded.
   */
  public boolean canUpload() {
    return this.fileUploadTarget_ != null;
  }
  /**
   * Use the click signal of another widget to open the file picker.
   *
   * <p>This hides the default {@link WFileUpload} widget and uses the click-signal of the argument
   * to open the file picker. The upload logic is still handled by {@link WFileUpload} behind the
   * scenes. This action cannot be undone.
   *
   * <p>{@link WFileUpload} does not take ownership of the widget, nor does it display it. You must
   * still place it in the widget tree yourself.
   */
  public void setDisplayWidget(WInteractWidget widget) {
    if (this.displayWidget_ != null || !(widget != null)) {
      return;
    }
    this.displayWidget_ = widget;
    this.flags_.set(BIT_USE_DISPLAY_WIDGET, true);
    this.repaint();
  }
  /**
   * Signal emitted when a new file was uploaded.
   *
   * <p>This signal is emitted when file upload has been completed. It is good practice to hide or
   * delete the {@link WFileUpload} widget when a file has been uploaded succesfully.
   *
   * <p>
   *
   * @see WFileUpload#upload()
   * @see WFileUpload#fileTooLarge()
   */
  public EventSignal uploaded() {
    return this.voidEventSignal(UPLOADED_SIGNAL, true);
  }
  /**
   * Signal emitted when the user tried to upload a too large file.
   *
   * <p>The parameter is the (approximate) size of the file (in bytes) the user tried to upload.
   *
   * <p>The maximum file size is determined by the maximum request size, which may be configured in
   * the configuration file (&lt;max-request-size&gt;).
   *
   * <p>
   *
   * @see WFileUpload#uploaded()
   * @see WApplication#requestTooLarge()
   */
  public JSignal1<Long> fileTooLarge() {
    return this.fileTooLarge_;
  }
  /**
   * Signal emitted when the user selected a new file.
   *
   * <p>One could react on the user selecting a (new) file, by uploading the file immediately.
   *
   * <p>Caveat: this signal is not emitted with konqueror and possibly other browsers. Thus, in the
   * above scenario you should still provide an alternative way to call the {@link
   * WFileUpload#upload() upload()} method.
   */
  public EventSignal changed() {
    return this.voidEventSignal(CHANGE_SIGNAL, true);
  }
  /**
   * Starts the file upload.
   *
   * <p>The {@link WFileUpload#uploaded() uploaded()} signal is emitted when a file is uploaded, or
   * the {@link WFileUpload#fileTooLarge() fileTooLarge()} signal is emitted when the file size
   * exceeded the maximum request size.
   *
   * <p>
   *
   * @see WFileUpload#uploaded()
   * @see WFileUpload#canUpload()
   */
  public void upload() {
    if (this.fileUploadTarget_ != null && !this.flags_.get(BIT_UPLOADING)) {
      this.flags_.set(BIT_DO_UPLOAD);
      this.repaint();
      if (this.progressBar_ != null) {
        if (this.progressBar_.getParent() != this) {
          this.hide();
        } else {
          this.progressBar_.show();
        }
      }
      WApplication.getInstance().enableUpdates();
      this.flags_.set(BIT_UPLOADING);
    }
  }
  /**
   * Sets a progress bar to indicate upload progress.
   *
   * <p>When the file is being uploaded, upload progress is indicated using the provided progress
   * bar. Both the progress bar range and values are configured when the upload starts.
   *
   * <p>The file upload itself is hidden as soon as the upload starts.
   *
   * <p>The default progress bar is 0 (no upload progress is indicated).
   *
   * <p>To update the progess bar server push is used, you should only use this functionality when
   * using a Servlet 3.0 compatible servlet container.
   *
   * <p>
   *
   * @see WFileUpload#dataReceived()
   */
  public void setProgressBar(WProgressBar bar) {
    if (this.containedProgressBar_ != bar) {
      {
        WWidget oldWidget = this.containedProgressBar_;
        this.containedProgressBar_ = null;
        {
          WWidget toRemove = this.manageWidget(oldWidget, this.containedProgressBar_);
          if (toRemove != null) toRemove.remove();
        }
      }
    }
    this.progressBar_ = bar;
  }
  /**
   * Returns the progress bar.
   *
   * <p>
   *
   * @see WFileUpload#setProgressBar(WProgressBar bar)
   */
  public WProgressBar getProgressBar() {
    return this.progressBar_;
  }
  /**
   * Signal emitted while a file is being uploaded.
   *
   * <p>When supported by the connector library, you can track the progress of the file upload by
   * listening to this signal.
   *
   * <p>The first argument is the number of bytes received so far, and the second argument is the
   * total number of bytes.
   */
  public Signal2<Long, Long> dataReceived() {
    return this.dataReceived_;
  }

  public void enableAjax() {
    this.create();
    this.flags_.set(BIT_ENABLE_AJAX);
    this.repaint();
    super.enableAjax();
  }
  /**
   * Sets input accept attributes.
   *
   * <p>The accept attribute may be specified to provide user agents with a hint of what file types
   * will be accepted. Use html input accept attributes as input.
   *
   * <p>
   *
   * <pre>{@code
   * WFileUpload *fu = new WFileUpload(root());
   * fu.setFilters("image/*");
   *
   *
   * }</pre>
   */
  public void setFilters(final String acceptAttributes) {
    this.acceptAttributes_ = acceptAttributes;
    this.flags_.set(BIT_ACCEPT_ATTRIBUTE_CHANGED, true);
    this.repaint();
  }

  private static String CHANGE_SIGNAL = "M_change";
  private static String UPLOADED_SIGNAL = "M_uploaded";
  private static final int BIT_DO_UPLOAD = 0;
  private static final int BIT_ENABLE_AJAX = 1;
  private static final int BIT_UPLOADING = 2;
  private static final int BIT_MULTIPLE = 3;
  private static final int BIT_ENABLED_CHANGED = 4;
  private static final int BIT_ACCEPT_ATTRIBUTE_CHANGED = 5;
  private static final int BIT_USE_DISPLAY_WIDGET = 6;
  BitSet flags_;
  private int textSize_;
  private List<UploadedFile> uploadedFiles_;
  private JSignal1<Long> fileTooLarge_;
  private Signal2<Long, Long> dataReceived_;
  private WInteractWidget displayWidget_;
  private JSlot displayWidgetRedirect_;
  private WResource fileUploadTarget_;
  private WProgressBar containedProgressBar_;
  private WProgressBar progressBar_;
  private String acceptAttributes_;

  private void create() {
    boolean methodIframe = WApplication.getInstance().getEnvironment().hasAjax();
    if (methodIframe) {
      this.fileUploadTarget_ = new WFileUploadResource(this);
      this.fileUploadTarget_.setUploadProgress(true);
      this.fileUploadTarget_
          .dataReceived()
          .addListener(
              this,
              (Long e1, Long e2) -> {
                WFileUpload.this.onData(e1, e2);
              });
      this.fileUploadTarget_
          .dataExceeded()
          .addListener(
              this,
              (Long e1) -> {
                WFileUpload.this.onDataExceeded(e1);
              });
      this.setJavaScriptMember(
          WT_RESIZE_JS,
          "function(self, w, h) {if (w >= 0) self.querySelector('input').style.width = `${w}px`;}");
    } else {
      this.fileUploadTarget_ = null;
    }
    this.setFormObject(!(this.fileUploadTarget_ != null));
    this.displayWidgetRedirect_.setJavaScript(this.getDisplayWidgetClickJS());
    this.uploaded()
        .addListener(
            this,
            () -> {
              WFileUpload.this.onUploaded();
            });
    this.fileTooLarge()
        .addListener(
            this,
            (Long e1) -> {
              WFileUpload.this.onUploaded();
            });
  }

  private void onData(long current, long total) {
    this.dataReceived_.trigger(current, total);
    if (this.progressBar_ != null && this.flags_.get(BIT_UPLOADING)) {
      this.progressBar_.setRange(0, (double) total);
      this.progressBar_.setValue((double) current);
      WApplication app = WApplication.getInstance();
      app.triggerUpdate();
    }
  }

  private void onDataExceeded(long dataExceeded) {
    this.doJavaScript(
        "Wt4.11.2.$('if" + this.getId() + "').src='" + this.fileUploadTarget_.getUrl() + "';");
    if (this.flags_.get(BIT_UPLOADING)) {
      this.flags_.clear(BIT_UPLOADING);
      this.handleFileTooLarge(dataExceeded);
      WApplication app = WApplication.getInstance();
      app.triggerUpdate();
      app.enableUpdates(false);
    }
  }

  private String getDisplayWidgetClickJS() {
    return ""
        + "function(sender, event) {"
        + "  function redirectClick(el) {"
        + "    if (el && el.tagName && el.tagName.toLowerCase() === 'input') {"
        + "      el.click();"
        + "      return true;"
        + "    } else {"
        + "      return false;"
        + "    }"
        + "  };"
        + "  "
        + "  var ok = redirectClick("
        + this.getJsRef()
        + ");"
        + "  if (!ok) {"
        + "    var children = "
        + this.getJsRef()
        + ".children;"
        + "    for (var i=0; i < children.length; i++) {"
        + "      if (redirectClick(children[i])) {"
        + "        return;"
        + "      }"
        + "    }"
        + "  }"
        + "}";
  }

  void setRequestTooLarge(long size) {
    this.fileTooLarge().trigger(size);
  }

  void updateDom(final DomElement element, boolean all) {
    boolean containsProgress = this.progressBar_ != null && this.progressBar_.getParent() == this;
    DomElement inputE = null;
    if (element.getType() != DomElementType.INPUT
        && this.flags_.get(BIT_DO_UPLOAD)
        && containsProgress
        && !this.progressBar_.isRendered()) {
      element.addChild(this.progressBar_.createSDomElement(WApplication.getInstance()));
    }
    if (this.fileUploadTarget_ != null
        && this.flags_.get(BIT_USE_DISPLAY_WIDGET)
        && this.displayWidget_ != null) {
      this.addStyleClass("Wt-fileupload-hidden");
      this.displayWidget_.clicked().addListener(this.displayWidgetRedirect_);
    }
    if (this.fileUploadTarget_ != null && this.flags_.get(BIT_DO_UPLOAD)) {
      element.setAttribute("action", this.fileUploadTarget_.generateUrl());
      String maxFileSize = String.valueOf(WApplication.getInstance().getMaximumRequestSize());
      String command =
          "{var submit = true;var x = Wt4.11.2.$('in"
              + this.getId()
              + "');if (x.files != null) {for (var i = 0; i < x.files.length; i++) {var f = x.files[i];if (f.size > "
              + maxFileSize
              + ") {submit = false;"
              + this.fileTooLarge().createCall("f.size")
              + ";break;}}}if (submit)"
              + this.getJsRef()
              + ".submit(); }";
      element.callJavaScript(command);
      this.flags_.clear(BIT_DO_UPLOAD);
      if (containsProgress) {
        inputE = DomElement.getForUpdate("in" + this.getId(), DomElementType.INPUT);
        inputE.setProperty(Property.StyleDisplay, "none");
      }
    }
    if (this.flags_.get(BIT_ENABLED_CHANGED)) {
      if (!(inputE != null)) {
        inputE = DomElement.getForUpdate("in" + this.getId(), DomElementType.INPUT);
      }
      if (this.isEnabled()) {
        inputE.callMethod("disabled=false");
      } else {
        inputE.callMethod("disabled=true");
      }
    }
    if (this.flags_.get(BIT_ACCEPT_ATTRIBUTE_CHANGED) || this.flags_.get(BIT_ENABLED_CHANGED)) {
      if (!(inputE != null)) {
        inputE = DomElement.getForUpdate("in" + this.getId(), DomElementType.INPUT);
      }
      inputE.setAttribute("accept", this.acceptAttributes_);
    }
    this.flags_.clear(BIT_ENABLED_CHANGED);
    this.flags_.clear(BIT_ACCEPT_ATTRIBUTE_CHANGED);
    this.flags_.clear(BIT_USE_DISPLAY_WIDGET);
    EventSignal change = this.voidEventSignal(CHANGE_SIGNAL, false);
    if (change != null && change.needsUpdate(all)) {
      if (!(inputE != null)) {
        inputE = DomElement.getForUpdate("in" + this.getId(), DomElementType.INPUT);
      }
      this.updateSignalConnection(inputE, change, "change", all);
    }
    if (inputE != null) {
      element.addChild(inputE);
    }
    super.updateDom(element, all);
  }

  protected DomElement createDomElement(WApplication app) {
    DomElement result = DomElement.createNew(this.getDomElementType());
    if (result.getType() == DomElementType.FORM) {
      result.setId(this.getId());
      app.getTheme().apply(this, result, ElementThemeRole.FileUploadForm);
    } else {
      result.setName(this.getId());
      app.getTheme().apply(this, result, ElementThemeRole.FileUploadInput);
    }
    EventSignal change = this.voidEventSignal(CHANGE_SIGNAL, false);
    if (this.fileUploadTarget_ != null) {
      DomElement i = DomElement.createNew(DomElementType.IFRAME);
      i.setProperty(Property.Class, "Wt-resource");
      i.setProperty(Property.Src, this.fileUploadTarget_.getUrl());
      i.setName("if" + this.getId());
      if (app.getEnvironment().agentIsIE()) {
        i.setAttribute("APPLICATION", "yes");
      }
      DomElement form = result;
      form.setAttribute("method", "post");
      form.setAttribute("action", this.fileUploadTarget_.getUrl());
      form.setAttribute("enctype", "multipart/form-data");
      form.setProperty(Property.Target, "if" + this.getId());
      DomElement d = DomElement.createNew(DomElementType.SPAN);
      d.addChild(i);
      form.addChild(d);
      DomElement input = DomElement.createNew(DomElementType.INPUT);
      app.getTheme().apply(this, input, ElementThemeRole.FileUploadInput);
      input.setAttribute("type", "file");
      if (this.flags_.get(BIT_MULTIPLE)) {
        input.setAttribute("multiple", "multiple");
      }
      input.setAttribute("name", "data");
      input.setAttribute("size", String.valueOf(this.textSize_));
      input.setAttribute("accept", this.acceptAttributes_);
      input.setId("in" + this.getId());
      if (!this.isEnabled()) {
        input.setProperty(Property.Disabled, "true");
      }
      if (change != null) {
        this.updateSignalConnection(input, change, "change", true);
      }
      form.addChild(input);
      this.doJavaScript(
          "var a ="
              + this.getJsRef()
              + ".action;var f = function(event) {if (a.indexOf(event.origin) === 0) {var data = JSON.parse(event.data);if (data.type === 'upload') {if (data.fu == '"
              + this.getId()
              + "')"
              + app.getJavaScriptClass()
              + "._p_.update(null, data.signal, null, true);} else if (data.type === 'file_too_large') {"
              + this.fileTooLarge().createCall("data.fileTooLargeSize")
              + "  }}};if (window.addEventListener) window.addEventListener('message', f, false);else window.attachEvent('onmessage', f);");
    } else {
      result.setAttribute("type", "file");
      if (this.flags_.get(BIT_MULTIPLE)) {
        result.setAttribute("multiple", "multiple");
      }
      result.setAttribute("size", String.valueOf(this.textSize_));
      if (!this.isEnabled()) {
        result.setProperty(Property.Disabled, "true");
      }
      if (change != null) {
        this.updateSignalConnection(result, change, "change", true);
      }
    }
    this.updateDom(result, true);
    this.flags_.clear(BIT_ENABLE_AJAX);
    return result;
  }

  DomElementType getDomElementType() {
    return this.fileUploadTarget_ != null ? DomElementType.FORM : DomElementType.INPUT;
  }

  void propagateRenderOk(boolean deep) {
    super.propagateRenderOk(deep);
  }

  protected void getDomChanges(final List<DomElement> result, WApplication app) {
    if (this.flags_.get(BIT_ENABLE_AJAX)) {
      DomElement plainE = DomElement.getForUpdate(this, DomElementType.INPUT);
      DomElement ajaxE = this.createDomElement(app);
      plainE.replaceWith(ajaxE);
      result.add(plainE);
    } else {
      super.getDomChanges(result, app);
    }
  }

  protected void propagateSetEnabled(boolean enabled) {
    this.flags_.set(BIT_ENABLED_CHANGED);
    this.repaint();
    super.propagateSetEnabled(enabled);
  }

  String renderRemoveJs(boolean recursive) {
    boolean isIE = WApplication.getInstance().getEnvironment().agentIsIE();
    if (this.isRendered() && isIE) {
      String result = "Wt4.11.2.$('if" + this.getId() + "').innerHTML = \"\";";
      if (!recursive) {
        result += "Wt4.11.2.remove('" + this.getId() + "');";
      }
      return result;
    } else {
      return super.renderRemoveJs(recursive);
    }
  }

  private void handleFileTooLarge(long fileSize) {
    this.fileTooLarge().trigger(fileSize);
  }

  private void onUploaded() {
    if (this.flags_.get(BIT_UPLOADING)) {
      WApplication.getInstance().enableUpdates(false);
      this.flags_.clear(BIT_UPLOADING);
    }
  }

  protected void setFormData(final WObject.FormData formData) {
    this.setFiles(formData.files);
    logger.debug(
        new StringWriter()
            .append("setFormData() : ")
            .append(String.valueOf(formData.files.size()))
            .append(" file(s)")
            .toString());
    if (!formData.files.isEmpty()) {
      this.uploaded().trigger();
    }
  }

  void setFiles(final List<UploadedFile> files) {
    this.uploadedFiles_.clear();
    for (int i = 0; i < files.size(); ++i) {
      if (files.get(i).getClientFileName().length() != 0) {
        this.uploadedFiles_.add(files.get(i));
      }
    }
  }
}
