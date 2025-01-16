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
 * A widget which popups to assist in editing a textarea or lineedit.
 *
 * <p>This widget may be associated with one or more {@link WFormWidget WFormWidgets} (typically a
 * {@link WLineEdit} or a {@link WTextArea}).
 *
 * <p>The popup provides the user with suggestions to enter input. The popup can be used by one or
 * more editors, using {@link WSuggestionPopup#forEdit(WFormWidget edit, EnumSet triggers)
 * forEdit()}. The popup will show when the user starts editing the edit field, or when the user
 * opens the suggestions explicitly using a drop down icon or with the down key. The popup positions
 * itself intelligently just below or just on top of the edit field. It offers a list of suggestions
 * that match in some way with the current edit field, and dynamically adjusts this list. The
 * implementation for matching individual suggestions with the current text is provided through a
 * JavaScript function. This function may also highlight part(s) of the suggestions to provide
 * feed-back on how they match.
 *
 * <p>WSuggestionPopup is an MVC view class, using a simple {@link WStringListModel} by default. You
 * can set a custom model using {@link WSuggestionPopup#setModel(WAbstractItemModel model)
 * setModel()}. The model can provide different text for the suggestion text ({@link
 * ItemDataRole#Display}) and value ({@link WSuggestionPopup#getEditRole() getEditRole()}). The
 * member methods {@link WSuggestionPopup#clearSuggestions() clearSuggestions()} and {@link
 * WSuggestionPopup#addSuggestion(CharSequence suggestionText, CharSequence suggestionValue)
 * addSuggestion()} manipulate this model.
 *
 * <p>By default, the popup implements all filtering client-side. To support large datasets, you may
 * enable server-side filtering of suggestions based on the input. The server-side filtering may
 * provide a coarse filtering using a fixed size prefix of the entered text, and complement the
 * client-side filtering. To enable server-side filtering, use {@link
 * WSuggestionPopup#setFilterLength(int length) setFilterLength()} and listen to filter notification
 * using the modelFilter() signal. Whenever a filter event is generated you can adjust the
 * model&apos;s content according to the filter (e.g. using a {@link WSortFilterProxyModel}). By
 * using {@link WCompositeWidget#setMaximumSize(WLength width, WLength height)
 * WCompositeWidget#setMaximumSize()} you can also limit the maximum height of the popup, in which
 * case scrolling is supported (similar to a combo-box).
 *
 * <p>The class is initialized with an {@link Options} struct which configures how suggestion
 * filtering and result editing is done. Alternatively, you can provide two JavaScript functions,
 * one for filtering the suggestions, and one for editing the value of the textarea when a
 * suggestion is selected.
 *
 * <p>The matcherJS function must have the following JavaScript signature:
 *
 * <p>
 *
 * <pre>{@code
 * function (editElement) {
 * // fetch the location of cursor and current text in the editElement.
 *
 * // return a function that matches a given suggestion with the current value of the editElement.
 * return function(suggestion) {
 *
 * // 1) if suggestion is null, simply return the current text 'value'
 * // 2) check suggestion if it matches
 * // 3) add highlighting markup to suggestion if necessary
 *
 * return { match : ...,      // does the suggestion match ? (boolean)
 * suggestion : ...  // modified suggestion with highlighting
 * };
 * }
 * }
 *
 * }</pre>
 *
 * <p>The replacerJS function that edits the value has the following JavaScript signature.
 *
 * <p>
 *
 * <pre>{@code
 * function (editElement, suggestionText, suggestionValue) {
 * // editElement is the form element which must be edited.
 * // suggestionText is the displayed text for the matched suggestion.
 * // suggestionValue is the stored value for the matched suggestion.
 *
 * // computed modifiedEditValue and modifiedPos ...
 *
 * editElement.value = modifiedEditValue;
 * editElement.selectionStart = edit.selectionEnd = modifiedPos;
 * }
 *
 * }</pre>
 *
 * <p>To style the suggestions, you should style the &lt;span&gt; element inside this widget, and
 * the &lt;span&gt;.&quot;sel&quot; element to style the current selection.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // options for email address suggestions
 * WSuggestionPopup.Options contactOptions = new WSuggestionPopup.Options();
 * contactOptions.highlightBeginTag = "<b>";
 * contactOptions.highlightEndTag = "</b>";
 * contactOptions.listSeparator = ','; //for multiple addresses)
 * contactOptions.whitespace = " \n";
 * contactOptions.wordSeparators = "-., \"@\n;"; //within an address
 * contactOptions.appendReplacedText = ", "; //prepare next email address
 *
 * WSuggestionPopup popup = new WSuggestionPopup(contactOptions, this);
 *
 * WTextArea textEdit = new WTextArea(this);
 * popup.forEdit(textEdit);
 *
 * // load popup data
 * for (int i = 0; i < contacts.size(); ++i)
 * popup.addSuggestion(contacts.get(i).formatted(), contacts.get(i).formatted());
 *
 * }</pre>
 *
 * <p>
 *
 * <p><i><b>Note: </b>This widget is not supposed to be added explicitly, as it is a global widget
 * (much like {@link WPopupWidget}). Managing its lifetime can be achieved with
 * WContainerWidget::addNew() or WObject::addChild(), where the former uses the latter. Do NOT bind
 * it to a template using {@link WTemplate#bindWidget(String varName, WWidget widget)
 * WTemplate#bindWidget()}, WTemplate::bindNew() or {@link WLayout#addWidget(WWidget w)
 * WLayout#addWidget()}. If bound this way, the placeholder is not replaced with the correct list of
 * suggestions, since this causes the widget to be placed into the widget tree twice. </i> A
 * screenshot of this example:
 *
 * <table border="1" cellspacing="3" cellpadding="3">
 * <tr><td><div align="center">
 * <img src="doc-files/WSuggestionPopup-default-1.png">
 * <p>
 * <strong>An example WSuggestionPopup (default)</strong></p>
 * </div>
 *
 *
 * </td><td><div align="center">
 * <img src="doc-files/WSuggestionPopup-polished-1.png">
 * <p>
 * <strong>An example WSuggestionPopup (polished)</strong></p>
 * </div>
 *
 *
 * </td></tr>
 * </table>
 *
 * <p>When using the DropDownIcon trigger, an additional style class is provided for the edit field:
 * <code>Wt-suggest-dropdown</code>, which renders the icon to the right inside the edit field. This
 * class may be used to customize how the drop down icon is rendered.
 *
 * <p>
 *
 * <p><i><b>Note: </b>This widget requires JavaScript support. </i>
 */
public class WSuggestionPopup extends WPopupWidget {
  private static Logger logger = LoggerFactory.getLogger(WSuggestionPopup.class);

  /**
   * A configuration object to generate a matcher and replacer JavaScript function.
   *
   * <p>
   *
   * @see WSuggestionPopup
   */
  public static class Options {
    private static Logger logger = LoggerFactory.getLogger(Options.class);

    /** Constructor. */
    public Options() {
      this.highlightBeginTag = "";
      this.highlightEndTag = "";
      this.listSeparator = 0;
      this.whitespace = "";
      this.wordSeparators = "";
      this.appendReplacedText = "";
      this.wordStartRegexp = "";
    }
    /**
     * Open tag to highlight a match in a suggestion.
     *
     * <p>Must be an opening markup tag, such as &lt;b&gt;.
     *
     * <p>Used during matching.
     */
    public String highlightBeginTag;
    /**
     * Close tag to highlight a match in a suggestion.
     *
     * <p>Must be a closing markup tag, such as &lt;/b&gt;.
     *
     * <p>Used during matching.
     */
    public String highlightEndTag;
    /**
     * When editing a list of values, the separator used for different items.
     *
     * <p>For example, &apos;,&apos; to separate different values on comma. Specify 0
     * (&apos;\0&apos;) for no list separation.
     *
     * <p>Used during matching and replacing.
     */
    public char listSeparator;
    /**
     * When editing a value, the whitespace characters ignored before the current value.
     *
     * <p>For example, &quot; \n&quot; to ignore spaces and newlines.
     *
     * <p>Used during matching and replacing.
     */
    public String whitespace;
    /**
     * Characters that start a word in a suggestion to match against.
     *
     * <p>For example, &quot; .@&quot; will also match with suggestion text after a space, a dot (.)
     * or an at-symbol (@). Alternatively you may also specify this as a regular expression in
     * <code>wordStartRegexp</code>.
     *
     * <p>Used during matching.
     */
    public String wordSeparators;
    /**
     * When replacing the current edited value with suggestion value, append the following string as
     * well.
     *
     * <p>Used during replacing.
     */
    public String appendReplacedText;
    /**
     * Regular expression that starts a word in a suggestion to match against.
     *
     * <p>When empty, the value of <code>wordSeparators</code> is used instead.
     *
     * <p>Used during replacing.
     */
    public String wordStartRegexp;
  }
  /**
   * Creates a suggestion popup.
   *
   * <p>The popup using a standard matcher and replacer implementation that is configured using the
   * provided <code>options</code>.
   *
   * <p>
   *
   * @see WSuggestionPopup#generateMatcherJS(WSuggestionPopup.Options options)
   * @see WSuggestionPopup#generateReplacerJS(WSuggestionPopup.Options options)
   */
  public WSuggestionPopup(final WSuggestionPopup.Options options) {
    super(new WContainerWidget());
    this.model_ = null;
    this.modelColumn_ = 0;
    this.filterLength_ = 0;
    this.filtering_ = false;
    this.defaultValue_ = -1;
    this.isDropDownIconUnfiltered_ = false;
    this.isAutoSelectEnabled_ = true;
    this.currentItem_ = -1;
    this.editRole_ = ItemDataRole.User;
    this.matcherJS_ = generateMatcherJS(options);
    this.replacerJS_ = generateReplacerJS(options);
    this.filterModel_ = new Signal1<String>();
    this.activated_ = new Signal2<Integer, WFormWidget>();
    this.modelConnections_ = new ArrayList<AbstractSignal.Connection>();
    this.currentInputText_ = "";
    this.filter_ = new JSignal1<String>(this.getImplementation(), "filter") {};
    this.jactivated_ = new JSignal2<String, String>(this.getImplementation(), "select") {};
    this.edits_ = new ArrayList<WFormWidget>();
    this.init();
  }
  /**
   * Creates a suggestion popup with given matcherJS and replacerJS.
   *
   * <p>See supra for the expected signature of the matcher and replace JavaScript functions.
   */
  public WSuggestionPopup(final String matcherJS, final String replacerJS) {
    super(new WContainerWidget());
    this.model_ = null;
    this.modelColumn_ = 0;
    this.filterLength_ = 0;
    this.filtering_ = false;
    this.defaultValue_ = -1;
    this.isDropDownIconUnfiltered_ = false;
    this.isAutoSelectEnabled_ = true;
    this.currentItem_ = -1;
    this.editRole_ = ItemDataRole.User;
    this.matcherJS_ = matcherJS;
    this.replacerJS_ = replacerJS;
    this.filterModel_ = new Signal1<String>();
    this.activated_ = new Signal2<Integer, WFormWidget>();
    this.modelConnections_ = new ArrayList<AbstractSignal.Connection>();
    this.currentInputText_ = "";
    this.filter_ = new JSignal1<String>(this.getImplementation(), "filter") {};
    this.jactivated_ = new JSignal2<String, String>(this.getImplementation(), "select") {};
    this.edits_ = new ArrayList<WFormWidget>();
    this.init();
  }
  /**
   * Lets this suggestion popup assist in editing an edit field.
   *
   * <p>A single suggestion popup may assist in several edits by repeated calls of this method.
   *
   * <p>The <code>popupTriggers</code> control how editing is triggered (either by the user editing
   * the field by entering keys or by an explicit drop down menu that is shown inside the edit).
   *
   * <p>
   *
   * @see WSuggestionPopup#removeEdit(WFormWidget edit)
   */
  public void forEdit(WFormWidget edit, EnumSet<PopupTrigger> triggers) {
    final AbstractEventSignal b = edit.keyPressed();
    this.connectObjJS(b, "editKeyDown");
    this.connectObjJS(edit.keyWentDown(), "editKeyDown");
    this.connectObjJS(edit.keyWentUp(), "editKeyUp");
    this.connectObjJS(edit.blurred(), "delayHide");
    if (triggers.contains(PopupTrigger.Editing)) {
      edit.addStyleClass("Wt-suggest-onedit");
    }
    if (triggers.contains(PopupTrigger.DropDownIcon)) {
      edit.addStyleClass("Wt-suggest-dropdown");
      final AbstractEventSignal c = edit.clicked();
      this.connectObjJS(c, "editClick");
      this.connectObjJS(edit.mouseMoved(), "editMouseMove");
    }
    this.edits_.add(edit);
  }
  /**
   * Lets this suggestion popup assist in editing an edit field.
   *
   * <p>Calls {@link #forEdit(WFormWidget edit, EnumSet triggers) forEdit(edit, EnumSet.of(trigger,
   * triggers))}
   */
  public final void forEdit(WFormWidget edit, PopupTrigger trigger, PopupTrigger... triggers) {
    forEdit(edit, EnumSet.of(trigger, triggers));
  }
  /**
   * Lets this suggestion popup assist in editing an edit field.
   *
   * <p>Calls {@link #forEdit(WFormWidget edit, EnumSet triggers) forEdit(edit,
   * EnumSet.of(PopupTrigger.Editing))}
   */
  public final void forEdit(WFormWidget edit) {
    forEdit(edit, EnumSet.of(PopupTrigger.Editing));
  }
  /**
   * Removes the edit field from the list of assisted editors.
   *
   * <p>The editor will no longer be assisted by this popup widget.
   *
   * <p>
   *
   * @see WSuggestionPopup#forEdit(WFormWidget edit, EnumSet triggers)
   */
  public void removeEdit(WFormWidget edit) {
    if (this.edits_.remove(edit)) {
      edit.removeStyleClass("Wt-suggest-onedit");
      edit.removeStyleClass("Wt-suggest-dropdown");
    }
  }
  /**
   * Shows the suggestion popup at an edit field.
   *
   * <p>This is equivalent to the user triggering the suggestion popup to be shown.
   */
  public void showAt(WFormWidget edit) {
    this.doJavaScript(this.getJsRef() + ".wtObj.showAt(" + edit.getJsRef() + ");");
  }
  /**
   * Clears the list of suggestions.
   *
   * <p>This clears the underlying model.
   *
   * <p>
   *
   * @see WSuggestionPopup#addSuggestion(CharSequence suggestionText, CharSequence suggestionValue)
   */
  public void clearSuggestions() {
    this.model_.removeRows(0, this.model_.getRowCount());
  }
  /**
   * Adds a new suggestion.
   *
   * <p>This adds an entry to the underlying model. The <code>suggestionText</code> is set as {@link
   * ItemDataRole#Display} and the <code>suggestionValue</code> (which is inserted into the edit
   * field on selection) is set as {@link WSuggestionPopup#getEditRole() getEditRole()}.
   *
   * <p>
   *
   * @see WSuggestionPopup#clearSuggestions()
   * @see WSuggestionPopup#setModel(WAbstractItemModel model)
   */
  public void addSuggestion(final CharSequence suggestionText, final CharSequence suggestionValue) {
    int row = this.model_.getRowCount();
    if (this.model_.insertRow(row)) {
      this.model_.setData(row, this.modelColumn_, suggestionText, ItemDataRole.Display);
      if (!(suggestionValue.length() == 0)) {
        this.model_.setData(row, this.modelColumn_, suggestionValue, this.getEditRole());
      }
    }
  }
  /**
   * Adds a new suggestion.
   *
   * <p>Calls {@link #addSuggestion(CharSequence suggestionText, CharSequence suggestionValue)
   * addSuggestion(suggestionText, WString.Empty)}
   */
  public final void addSuggestion(final CharSequence suggestionText) {
    addSuggestion(suggestionText, WString.Empty);
  }
  /**
   * Sets the model to be used for the suggestions.
   *
   * <p>The <code>model</code> may not be <code>null</code>.
   *
   * <p>The default value is a {@link WStringListModel} that is owned by the suggestion popup.
   *
   * <p>The {@link ItemDataRole#Display} is used for the suggestion text. The {@link
   * WSuggestionPopup#getEditRole() getEditRole()} is used for the suggestion value, unless empty,
   * in which case the suggestion text is used as value.
   *
   * <p>
   *
   * @see WSuggestionPopup#setModelColumn(int modelColumn)
   */
  public void setModel(final WAbstractItemModel model) {
    if (this.model_ != null) {
      for (int i = 0; i < this.modelConnections_.size(); ++i) {
        this.modelConnections_.get(i).disconnect();
      }
      this.modelConnections_.clear();
    }
    this.model_ = model;
    this.modelConnections_.add(
        this.model_
            .rowsInserted()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WSuggestionPopup.this.modelRowsInserted(e1, e2, e3);
                }));
    this.modelConnections_.add(
        this.model_
            .rowsRemoved()
            .addListener(
                this,
                (WModelIndex e1, Integer e2, Integer e3) -> {
                  WSuggestionPopup.this.modelRowsRemoved(e1, e2, e3);
                }));
    this.modelConnections_.add(
        this.model_
            .dataChanged()
            .addListener(
                this,
                (WModelIndex e1, WModelIndex e2) -> {
                  WSuggestionPopup.this.modelDataChanged(e1, e2);
                }));
    this.modelConnections_.add(
        this.model_
            .layoutChanged()
            .addListener(
                this,
                () -> {
                  WSuggestionPopup.this.modelLayoutChanged();
                }));
    this.modelConnections_.add(
        this.model_
            .modelReset()
            .addListener(
                this,
                () -> {
                  WSuggestionPopup.this.modelLayoutChanged();
                }));
    this.setModelColumn(this.modelColumn_);
  }
  /**
   * Returns the data model.
   *
   * <p>
   *
   * @see WSuggestionPopup#setModel(WAbstractItemModel model)
   */
  public WAbstractItemModel getModel() {
    return this.model_;
  }
  /**
   * Sets the column in the model to be used for the items.
   *
   * <p>The column <code>index</code> in the model will be used to retrieve data.
   *
   * <p>The default value is 0.
   *
   * <p>
   *
   * @see WSuggestionPopup#setModel(WAbstractItemModel model)
   */
  public void setModelColumn(int modelColumn) {
    this.modelColumn_ = modelColumn;
    this.impl_.clear();
    this.modelRowsInserted(null, 0, this.model_.getRowCount() - 1);
  }
  /**
   * Sets a default selected value.
   *
   * <p><code>row</code> is the model row that is selected by default (only if it matches the
   * current input).
   *
   * <p>The default value is -1, indicating no default.
   */
  public void setDefaultIndex(int row) {
    if (this.defaultValue_ != row) {
      this.defaultValue_ = row;
      if (this.isRendered()) {
        this.doJavaScript(
            this.getJsRef() + ".wtObj.defaultValue = " + String.valueOf(this.defaultValue_) + ';');
      }
    }
  }
  /** Returns the default value. */
  public int getDefaultIndex() {
    return this.defaultValue_;
  }
  /**
   * Creates a standard matcher JavaScript function.
   *
   * <p>This returns a JavaScript function that provides a standard implementation for the matching
   * input, based on the given <code>options</code>.
   */
  public static String generateMatcherJS(final WSuggestionPopup.Options options) {
    return instantiateStdMatcher(options) + ".match";
  }
  /**
   * Creates a standard replacer JavaScript function.
   *
   * <p>This returns a JavaScript function that provides a standard implementation for reacting to a
   * match activation, editing the line edit text.
   */
  public static String generateReplacerJS(final WSuggestionPopup.Options options) {
    return instantiateStdMatcher(options) + ".replace";
  }
  /**
   * Sets the minimum input length before showing the popup.
   *
   * <p>When the user has typed this much characters, {@link WSuggestionPopup#filterModel()
   * filterModel()} is emitted which allows you to filter the model based on the initial input. The
   * filtering is done as long as the model indicates that results are partial by setting a {@link
   * ItemDataRole#StyleClass} of &quot;Wt-more-data&quot; on the last item.
   *
   * <p>The default value is 0.
   *
   * <p>A value of -1 is a equivalent to 0 but filtering is always applied as if the last item
   * always has &quot;Wt-more-data&quot; (for backwards compatibility)
   *
   * <p>
   *
   * @see WSuggestionPopup#filterModel()
   */
  public void setFilterLength(int length) {
    this.filterLength_ = length;
  }
  /**
   * Returns the filter length.
   *
   * <p>
   *
   * @see WSuggestionPopup#setFilterLength(int length)
   */
  public int getFilterLength() {
    return this.filterLength_;
  }
  /**
   * Signal that indicates that the model should be filtered.
   *
   * <p>The argument is the initial input. When {@link PopupTrigger#Editing} is used as edit
   * trigger, its length will always equal the {@link WSuggestionPopup#getFilterLength()
   * getFilterLength()}. When {@link PopupTrigger#DropDownIcon} is used as edit trigger, the input
   * length may be less than {@link WSuggestionPopup#getFilterLength() getFilterLength()}, and the
   * the signal will be called repeatedly as the user provides more input.
   *
   * <p>For example, if you are using a {@link WSortFilterProxyModel}, you could react to this
   * signal with:
   *
   * <pre>{@code
   * public filterSuggestions(String filter) {
   * proxyModel.setFilterRegExp(filter + ".*");
   * }
   *
   * }</pre>
   */
  public Signal1<String> filterModel() {
    return this.filterModel_;
  }
  /**
   * Signal emitted when a suggestion was selected.
   *
   * <p>The selected item is passed as the first argument and the editor as the second.
   */
  public Signal2<Integer, WFormWidget> activated() {
    return this.activated_;
  }
  /**
   * When drop down icon is clicked the popup content will be unfiltered.
   *
   * <p>
   *
   * @see WSuggestionPopup#forEdit(WFormWidget edit, EnumSet triggers)
   * @see PopupTrigger
   */
  public void setDropDownIconUnfiltered(boolean isUnfiltered) {
    this.isDropDownIconUnfiltered_ = isUnfiltered;
  }
  /**
   * When the popup is opened the first item is automatically selected.
   *
   * <p>This cannot be changed after the widget is rendered. The default value is true.
   */
  public void setAutoSelectEnabled(boolean enabled) {
    this.isAutoSelectEnabled_ = enabled;
  }
  /**
   * Returns the last activated index.
   *
   * <p>Returns -1 if the popup hasn&apos;t been activated yet.
   *
   * <p>
   *
   * @see WSuggestionPopup#activated()
   */
  public int getCurrentItem() {
    return this.currentItem_;
  }
  /**
   * Sets the role used for editing the line edit with a chosen item.
   *
   * <p>The default value is {@link ItemDataRole#User}.
   */
  public void setEditRole(ItemDataRole role) {
    this.editRole_ = role;
  }
  /**
   * Returns the role used for editing the line edit.
   *
   * <p><i>{@link WSuggestionPopup#setEditRole(ItemDataRole role) setEditRole()}</i>
   */
  public ItemDataRole getEditRole() {
    return this.editRole_;
  }

  private WContainerWidget impl_;
  private WAbstractItemModel model_;
  private int modelColumn_;
  private int filterLength_;
  private boolean filtering_;
  private int defaultValue_;
  private boolean isDropDownIconUnfiltered_;
  private boolean isAutoSelectEnabled_;
  private int currentItem_;
  private ItemDataRole editRole_;
  private String matcherJS_;
  private String replacerJS_;
  private Signal1<String> filterModel_;
  private Signal2<Integer, WFormWidget> activated_;
  private List<AbstractSignal.Connection> modelConnections_;
  private String currentInputText_;
  private JSignal1<String> filter_;
  private JSignal2<String, String> jactivated_;
  private List<WFormWidget> edits_;

  private void init() {
    this.impl_ = ObjectUtils.cast(this.getImplementation(), WContainerWidget.class);
    this.impl_.setList(true);
    this.impl_.setLoadLaterWhenInvisible(false);
    this.setAttributeValue("style", "display: none; overflow: auto");
    this.setModel(new WStringListModel());
    this.impl_
        .escapePressed()
        .addListener(
            this,
            () -> {
              WSuggestionPopup.this.hide();
            });
    this.filter_.addListener(
        this,
        (String e1) -> {
          WSuggestionPopup.this.scheduleFilter(e1);
        });
    this.jactivated_.addListener(
        this,
        (String e1, String e2) -> {
          WSuggestionPopup.this.doActivate(e1, e2);
        });
  }

  private void scheduleFilter(String input) {
    this.currentInputText_ = input;
    this.scheduleRender();
  }

  private void doFilter(String input) {
    this.filtering_ = true;
    this.filterModel_.trigger(input);
    this.filtering_ = false;
    WApplication.getInstance()
        .doJavaScript(
            this.getJsRef()
                + ".wtObj.filtered("
                + WWebWidget.jsStringLiteral(input)
                + ","
                + (this.isPartialResults() ? "1" : "0")
                + ");");
  }

  private void doActivate(String itemId, String editId) {
    WFormWidget edit = null;
    for (int i = 0; i < this.edits_.size(); ++i) {
      if (this.edits_.get(i).getId().equals(editId)) {
        edit = this.edits_.get(i);
        break;
      }
    }
    if (edit == null) {
      logger.error(new StringWriter().append("activate from bogus editor").toString());
      this.currentItem_ = -1;
      return;
    }
    for (int i = 0; i < this.impl_.getCount(); ++i) {
      if (this.impl_.getWidget(i).getId().equals(itemId)) {
        this.currentItem_ = i;
        this.activated_.trigger(i, edit);
        if (edit != null) {
          WLineEdit le = ObjectUtils.cast(edit, WLineEdit.class);
          WTextArea ta = ObjectUtils.cast(edit, WTextArea.class);
          if (le != null) {
            le.textInput().trigger();
          } else {
            if (ta != null) {
              ta.textInput().trigger();
            }
          }
          edit.changed().trigger();
        }
        return;
      }
    }
    this.currentItem_ = -1;
    logger.error(new StringWriter().append("activate for bogus item").toString());
  }

  private void connectObjJS(final AbstractEventSignal s, final String methodName) {
    String jsFunction =
        "function(obj, event) {var o = "
            + this.getJsRef()
            + ";if (o && o.wtObj) o.wtObj."
            + methodName
            + "(obj, event);}";
    s.addListener(jsFunction);
  }

  private void modelRowsInserted(final WModelIndex parent, int start, int end) {
    if (this.filterLength_ != 0 && !this.filtering_) {
      return;
    }
    if (this.modelColumn_ >= this.model_.getColumnCount()) {
      return;
    }
    if ((parent != null)) {
      return;
    }
    for (int i = start; i <= end; ++i) {
      WContainerWidget line = new WContainerWidget();
      this.impl_.insertWidget(i, line);
      WModelIndex index = this.model_.getIndex(i, this.modelColumn_);
      Object d = index.getData();
      TextFormat format =
          index.getFlags().contains(ItemFlag.XHTMLText) ? TextFormat.XHTML : TextFormat.Plain;
      WAnchor anchor = new WAnchor();
      line.addWidget(anchor);
      WText value = new WText(StringUtils.asString(d), format);
      anchor.addWidget(value);
      Object d2 = index.getData(this.editRole_);
      if (!(d2 != null)) {
        d2 = d;
      }
      value.setAttributeValue("sug", StringUtils.asString(d2).toString());
      Object styleclass = index.getData(ItemDataRole.StyleClass);
      if ((styleclass != null)) {
        value.setAttributeValue("class", StringUtils.asString(styleclass).toString());
      }
    }
  }

  private void modelRowsRemoved(final WModelIndex parent, int start, int end) {
    if ((parent != null)) {
      return;
    }
    for (int i = start; i <= end; ++i) {
      if (start < this.impl_.getCount()) {
        {
          WWidget toRemove = this.impl_.removeWidget(this.impl_.getWidget(start));
          if (toRemove != null) toRemove.remove();
        }

      } else {
        break;
      }
    }
  }

  private void modelDataChanged(final WModelIndex topLeft, final WModelIndex bottomRight) {
    if ((topLeft.getParent() != null)) {
      return;
    }
    if (this.modelColumn_ < topLeft.getColumn() || this.modelColumn_ > bottomRight.getColumn()) {
      return;
    }
    for (int i = topLeft.getRow(); i <= bottomRight.getRow(); ++i) {
      WContainerWidget w = ObjectUtils.cast(this.impl_.getWidget(i), WContainerWidget.class);
      WAnchor anchor = ObjectUtils.cast(w.getWidget(0), WAnchor.class);
      WText value = ObjectUtils.cast(anchor.getWidget(0), WText.class);
      WModelIndex index = this.model_.getIndex(i, this.modelColumn_);
      Object d = index.getData();
      value.setText(StringUtils.asString(d));
      TextFormat format =
          index.getFlags().contains(ItemFlag.XHTMLText) ? TextFormat.XHTML : TextFormat.Plain;
      value.setTextFormat(format);
      Object d2 = this.model_.getData(i, this.modelColumn_, this.getEditRole());
      if (!(d2 != null)) {
        d2 = d;
      }
      value.setAttributeValue("sug", StringUtils.asString(d2).toString());
    }
  }

  private void modelLayoutChanged() {
    this.impl_.clear();
    this.modelRowsInserted(null, 0, this.model_.getRowCount() - 1);
  }

  private boolean isPartialResults() {
    if (this.filterLength_ < 0) {
      return true;
    } else {
      if (this.model_.getRowCount() > 0) {
        WModelIndex index = this.model_.getIndex(this.model_.getRowCount() - 1, this.modelColumn_);
        Object styleclass = index.getData(ItemDataRole.StyleClass);
        return (StringUtils.asString(styleclass).toString().equals("Wt-more-data".toString()));
      } else {
        return false;
      }
    }
  }

  private void defineJavaScript() {
    WApplication app = WApplication.getInstance();
    String THIS_JS = "js/WSuggestionPopup.js";
    app.loadJavaScript(THIS_JS, wtjs1());
    app.loadJavaScript(THIS_JS, wtjs2());
    String ddUnfiltered = this.isDropDownIconUnfiltered_ ? "true" : "false";
    String autoSelect = this.isAutoSelectEnabled_ ? "true" : "false";
    this.setJavaScriptMember(
        " WSuggestionPopup",
        "new Wt4.11.2.WSuggestionPopup("
            + app.getJavaScriptClass()
            + ","
            + this.getJsRef()
            + ","
            + this.replacerJS_
            + ","
            + this.matcherJS_
            + ","
            + String.valueOf(Math.max(0, this.filterLength_))
            + ","
            + String.valueOf(this.isPartialResults())
            + ","
            + String.valueOf(this.defaultValue_)
            + ","
            + ddUnfiltered
            + ","
            + autoSelect
            + ");");
  }

  protected void render(EnumSet<RenderFlag> flags) {
    if (flags.contains(RenderFlag.Full)) {
      this.defineJavaScript();
    }
    if (WApplication.getInstance().getEnvironment().hasAjax()) {
      this.doFilter(this.currentInputText_);
    }
    super.render(flags);
  }

  static WJavaScriptPreamble wtjs1() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WSuggestionPopup",
        "(function(e,t,n,i,l,s,o,r,c){t.wtObj=this;const u=this,f=e.WT;let a=null,d=null,g=!1,h=null,p=null,y=s,C=null,v=null,k=!1;this.defaultValue=o;function T(){return\"object\"==typeof f.theme&&\"bootstrap\"===f.theme.type&&5===f.theme.version}function m(e){return e.classList.contains(\"Wt-suggest-onedit\")||e.classList.contains(\"Wt-suggest-dropdown\")}function L(){return\"none\"!==t.style.display}function E(i){const l=i.firstChild.firstChild,s=f.getElement(d),o=l.innerHTML,r=l.getAttribute(\"sug\");s.focus();n(s,o,r);e.emit(t,\"select\",i.id,s.id);b();d=null}let w=null;this.showPopup=function(e){t.style.display=\"block\";u.bringToFront();a=null;v=null;w=e.onkeydown;e.onkeydown=function(e){const t=e||window.event;u.editKeyDown(this,t)}};this.bringToFront=function(){const e=f.maxZIndex();if(e>t.style.zIndex){const n=e+1;t.style.zIndex=n}};function b(){t.style.display=\"none\";if(null!==d&&null!==w){f.getElement(d).onkeydown=w;w=null}}function S(e){if(!T())return 16;try{const t=getComputedStyle(e),n=parseInt(t.backgroundSize.match(/^([0-9]+)px ([0-9]+)px$/)[1],10);return n+parseInt(t.backgroundPositionX.match(/^calc[(]100% - ([0-9]+)px[)]$/)[1],10)}catch(e){return 28}}this.editMouseMove=function(e,t){if(!m(e))return;if(f.widgetCoordinates(e,t).x>e.offsetWidth-S(e)){e.classList&&e.classList.add(\"Wt-suggest-dropdown-hover\");e.style.cursor=\"default\"}else{e.classList&&e.classList.remove(\"Wt-suggest-dropdown-hover\");e.style.cursor=\"\"}};this.showAt=function(e,t){b();d=e.id;k=!0;void 0===t&&(t=\"\");u.refilter(t)};this.editClick=function(e,t){if(!m(e))return;if(f.widgetCoordinates(e,t).x>e.offsetWidth-S(e))if(d===e.id&&L()){b();d=null}else u.showAt(e,\"\")};function x(e,t){for(e=t?e.nextSibling:e.previousSibling;e;e=t?e.nextSibling:e.previousSibling)if(f.hasTag(e,\"LI\")&&\"none\"!==e.style.display)return e;return null}this.editKeyDown=function(e,n){if(!m(e))return!0;if(d!==e.id)if(e.classList.contains(\"Wt-suggest-onedit\")){d=e.id;k=!1}else{if(!e.classList.contains(\"Wt-suggest-dropdown\")||40!==n.keyCode){d=null;return!0}d=e.id;k=!0}if(L()){const i=a?f.getElement(a):null;if(13===n.keyCode||9===n.keyCode){if(i){E(i);f.cancelEvent(n);setTimeout((function(){e.focus()}),0)}else b();return!1}if(40===n.keyCode||38===n.keyCode||34===n.keyCode||33===n.keyCode){if(\"KEYDOWN\"===n.type.toUpperCase()){g=!0;f.cancelEvent(n,f.CancelDefaultAction)}if(\"KEYPRESS\"===n.type.toUpperCase()&&!0===g){f.cancelEvent(n);return!1}let e=i;const l=40===n.keyCode||34===n.keyCode;if(e){const s=34===n.keyCode||33===n.keyCode?t.clientHeight/i.offsetHeight:1;for(let t=0;e&&t<s;++t){const t=x(e,l);if(!t&&c)break;e=t}}else{e=function(e){const n=t.childNodes;for(let t=e?n[0]:n[n.length-1];t;t=e?t.nextSibling:t.previousSibling)if(f.hasTag(t,\"LI\")&&\"none\"!==t.style.display)return t;return null}(l);W(e)}if(i){i.classList.remove(\"active\");T()&&i.firstChild.classList.remove(\"active\");a=null}if(e&&f.hasTag(e,\"LI\")){e.classList.add(\"active\");T()&&e.firstChild.classList.add(\"active\");W(e);a=e.id}return!1}}return 13!==n.keyCode&&9!==n.keyCode};this.filtered=function(e,t){h=e;y=t;u.refilter(v)};function W(e){const t=e.parentNode;e.offsetTop+e.offsetHeight>t.scrollTop+t.clientHeight?t.scrollTop=e.offsetTop+e.offsetHeight-t.clientHeight:e.offsetTop<t.scrollTop&&(t.scrollTop=e.offsetTop)}this.refilter=function(n){if(!d)return;let o=a?f.getElement(a):null;const g=f.getElement(d),C=i(g),m=t.childNodes,E=r&&null!==n?n:C(null);v=r?n:g.value;if(l>0||s){if(E.length<l&&!k){b();return}{const n=y?E:E.substring(0,Math.max(null!==h?h.length:0,l));if(n!==h&&n!==p){p=n;e.emit(t,\"filter\",n)}}}let w=null,S=null;const x=k&&0===E.length;for(let e=0,t=m.length;e<t;++e){const t=m[e];if(f.hasTag(t,\"LI\")){const n=t.firstChild;null!==t.orig&&void 0!==t.orig||(t.orig=n.firstChild.innerHTML);const i=C(t.orig),l=x||i.match;i.suggestion!==n.firstChild.innerHTML&&(n.firstChild.innerHTML=i.suggestion);if(l){\"\"!==t.style.display&&(t.style.display=\"\");null===w&&(w=t);e===this.defaultValue&&(S=t)}else\"none\"!==t.style.display&&(t.style.display=\"none\");t.classList.remove(\"active\");T()&&t.firstChild.classList.remove(\"active\")}}if(null===w)b();else{if(!L()){!function(e){t.style.display=\"block\";f.positionAtWidget(t.id,e.id,f.Vertical)}(g);u.showPopup(g);o=null}if((c||S)&&(!o||\"none\"===o.style.display)){o=S||w;o.parentNode.scrollTop=0;a=o.id}if(o){o.classList.add(\"active\");T()&&o.firstChild.classList.add(\"active\");W(o)}}};this.editKeyUp=function(e,t){if(null!==d&&m(e)&&(L()||13!==t.keyCode&&9!==t.keyCode))if(27===t.keyCode||37===t.keyCode||39===t.keyCode)b();else if(40===t.keyCode||38===t.keyCode||34===t.keyCode||33===t.keyCode);else if(e.value!==v){d=e.id;u.refilter(e.value)}else{const e=a?f.getElement(a):null;e&&W(e)}};t.onclick=function(e){const t=e||window.event;let n=f.target(t);if(!f.hasTag(n,\"UL\")){for(;n&&!f.hasTag(n,\"LI\");)n=n.parentNode;n&&E(n)}};t.onscroll=function(){if(C){clearTimeout(C);const e=f.getElement(d);e&&e.focus()}};this.delayHide=function(e,n){C=setTimeout((function(){C=null;!t||null!==e&&d!==e.id||b()}),300)}})");
  }

  static WJavaScriptPreamble wtjs2() {
    return new WJavaScriptPreamble(
        JavaScriptScope.WtClassScope,
        JavaScriptObjectType.JavaScriptConstructor,
        "WSuggestionPopupStdMatcher",
        "(function(e,t,n,i,l,s,o){function r(e){const t=e.value,l=e.selectionStart?e.selectionStart:t.length;let s=n?t.lastIndexOf(n,l-1)+1:0;for(;s<l&&-1!==i.indexOf(t.charAt(s));)++s;return{start:s,end:l}}this.match=function(n){const i=r(n),o=n.value.substring(i.start,i.end);let c;if(0===s.length)if(0!==l.length){c=\"(^|(?:[\";for(let e=0;e<l.length;++e){let t=l.charCodeAt(e).toString(16);for(;t.length<4;)t=\"0\"+t;c+=\"\\\\u\"+t}c+=\"]))\"}else c=\"(^)\";else c=\"(\"+s+\")\";c+=\"(\"+o.replace(new RegExp(\"([\\\\^\\\\\\\\\\\\][\\\\-.$*+?()|{}])\",\"g\"),\"\\\\$1\")+\")\";c=new RegExp(c,\"gi\");return function(n){if(!n)return o;let i=!1;if(o.length){const l=n.replace(c,\"$1\"+e+\"$2\"+t);if(l!==n){i=!0;n=l}}return{match:i,suggestion:n}}};this.replace=function(e,t,n){const i=r(e);let l=e.value.substring(0,i.start)+n+o;i.end<e.value.length&&(l+=e.value.substring(i.end,e.value.length));e.value=l;if(e.selectionStart){e.selectionStart=i.start+n.length+o.length;e.selectionEnd=e.selectionStart}}})");
  }

  static String instantiateStdMatcher(final WSuggestionPopup.Options options) {
    StringBuilder s = new StringBuilder();
    s.append("new Wt4.11.2.WSuggestionPopupStdMatcher(")
        .append(WWebWidget.jsStringLiteral(options.highlightBeginTag))
        .append(", ")
        .append(WWebWidget.jsStringLiteral(options.highlightEndTag))
        .append(", ");
    if (options.listSeparator != 0) {
      s.append(WWebWidget.jsStringLiteral("" + options.listSeparator));
    } else {
      s.append("null");
    }
    s.append(", ")
        .append(WWebWidget.jsStringLiteral(options.whitespace))
        .append(", ")
        .append(WWebWidget.jsStringLiteral(options.wordSeparators))
        .append(", ")
        .append(WWebWidget.jsStringLiteral(options.wordStartRegexp))
        .append(", ")
        .append(WWebWidget.jsStringLiteral(options.appendReplacedText))
        .append(")");
    return s.toString();
  }
}
