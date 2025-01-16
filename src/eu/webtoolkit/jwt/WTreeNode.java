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
 * A single node in a tree.
 *
 * <p>A tree list is constructed by combining several tree node objects in a tree hierarchy, by
 * passing the parent tree node as the last argument in the child node constructor, or by using
 * {@link WTreeNode#addChildNode(WTreeNode node) addChildNode()}, to add a child to its parent.
 *
 * <p>Each tree node has a label, and optionally a label icon pair. The icon pair offers the
 * capability to show a different icon depending on the state of the node (expanded or collapsed).
 * When the node has any children, a child count may be displayed next to the label using {@link
 * WTreeNode#setChildCountPolicy(ChildCountPolicy policy) setChildCountPolicy()}.
 *
 * <p>Expanding a tree node it will collapse all its children, so that a user may collapse/expand a
 * node as a short-cut to collapsing all children.
 *
 * <p>The treenode provides several policies to communicate the current contents of the tree to the
 * client (if possible):
 *
 * <ul>
 *   <li>{@link ContentLoading#Eager}: the entire tree is transmitted to the client, and all tree
 *       navigation requires no further communication.
 *   <li>ContentLoading::Lazy: only the minimum is transmitted to the client. When expanding a node
 *       for the first time, only then it is transmitted to the client, and this may thus have some
 *       latency.
 *   <li>{@link ContentLoading#NextLevel}: all leafs of visible children are transmitted, but not
 *       their children. This provides a good trade-off between bandwith use and interactivity,
 *       since expanding any tree node will happen instantly, and at the same time trigger some
 *       communication in the back-ground to load the next level of invisible nodes.
 * </ul>
 *
 * <p>The default policy is ContentLoading::Lazy. Another load policy may be specified using {@link
 * WTreeNode#setLoadPolicy(ContentLoading loadPolicy) setLoadPolicy()} on the root node and before
 * adding any children. The load policy is inherited by all children in the tree.
 *
 * <p>There are a few scenarios where it makes sense to specialize the WTreeNode class. One scenario
 * is create a tree that is populated dynamically while browsing. For this purpose you should
 * reimplement the {@link WTreeNode#populate() populate()} method, whose default implementation does
 * nothing. This method is called when &apos;loading&apos; the node. The exact moment for loading a
 * treenode depends on the LoadPolicy.
 *
 * <p>A second scenario that is if you want to customize the look of the tree label (see {@link
 * WTreeNode#getLabelArea() getLabelArea()}) or if you want to modify or augment the event
 * collapse/expand event handling (see {@link WTreeNode#doExpand() doExpand()} and {@link
 * WTreeNode#doCollapse() doCollapse()}).
 *
 * <p>See {@link WTree} for a usage example.
 *
 * <p>
 *
 * @see WTree
 * @see WTreeTableNode
 */
public class WTreeNode extends WCompositeWidget {
  private static Logger logger = LoggerFactory.getLogger(WTreeNode.class);

  /**
   * Creates a tree node with the given label.
   *
   * <p>The labelIcon, if specified, will appear just before the label and its state reflect the
   * expand/collapse state of the node.
   *
   * <p>The node is initialized to be collapsed.
   */
  public WTreeNode(final CharSequence labelText, WIconPair labelIcon, WTreeNode parentNode) {
    super();
    this.collapsed_ = true;
    this.selectable_ = true;
    this.visible_ = true;
    this.childrenDecorated_ = true;
    this.parentNode_ = null;
    this.notLoadedChildren_ = new ArrayList<WTreeNode>();
    this.childCountPolicy_ = ChildCountPolicy.Disabled;
    this.labelIcon_ = labelIcon;
    this.childrenLoaded_ = false;
    this.populated_ = false;
    this.interactive_ = true;
    this.selected_ = new Signal1<Boolean>();
    this.clickedConnection_ = new AbstractSignal.Connection();
    this.init(labelText, labelIcon);
    if (parentNode != null) parentNode.addChildNode(this);
  }
  /**
   * Creates a tree node with the given label.
   *
   * <p>Calls {@link #WTreeNode(CharSequence labelText, WIconPair labelIcon, WTreeNode parentNode)
   * this(labelText, null, (WTreeNode)null)}
   */
  public WTreeNode(final CharSequence labelText) {
    this(labelText, null, (WTreeNode) null);
  }
  /**
   * Creates a tree node with the given label.
   *
   * <p>Calls {@link #WTreeNode(CharSequence labelText, WIconPair labelIcon, WTreeNode parentNode)
   * this(labelText, labelIcon, (WTreeNode)null)}
   */
  public WTreeNode(final CharSequence labelText, WIconPair labelIcon) {
    this(labelText, labelIcon, (WTreeNode) null);
  }
  /** Destructor. */
  public void remove() {
    super.remove();
  }
  /**
   * Returns the tree.
   *
   * <p>By default if this node has no parent the result will be 0.
   */
  public WTree getTree() {
    return this.parentNode_ != null ? this.parentNode_.getTree() : null;
  }
  /** Returns the label. */
  public WText getLabel() {
    return this.labelText_;
  }
  /** Returns the label icon. */
  public WIconPair getLabelIcon() {
    return this.labelIcon_;
  }
  /** Sets the label icon. */
  public void setLabelIcon(WIconPair labelIcon) {
    if (this.labelIcon_ != null) {
      {
        WWidget toRemove = this.labelIcon_.removeFromParent();
        if (toRemove != null) toRemove.remove();
      }
    }
    this.labelIcon_ = labelIcon;
    if (labelIcon != null) {
      if (this.labelText_ != null) {
        this.getLabelArea().insertBefore(labelIcon, this.labelText_);
      } else {
        this.getLabelArea().addWidget(labelIcon);
      }
      this.labelIcon_.setState(this.isExpanded() ? 1 : 0);
    }
    this.resetLearnedSlots();
  }
  /**
   * Inserts a child node.
   *
   * <p>Inserts the node <code>node</code> at index <code>index</code>.
   */
  public void insertChildNode(int index, WTreeNode node) {
    node.parentNode_ = this;
    WTreeNode added = node;
    if (this.childrenLoaded_) {
      this.getChildContainer().insertWidget(index, node);
    } else {
      this.notLoadedChildren_.add(0 + index, node);
    }
    this.descendantAdded(added);
    if (this.loadPolicy_ != added.loadPolicy_) {
      added.setLoadPolicy(this.loadPolicy_);
    }
    if (this.childCountPolicy_ != added.childCountPolicy_) {
      added.setChildCountPolicy(this.childCountPolicy_);
    }
    if (index == (int) this.getChildCount() - 1 && this.getChildCount() > 1) {
      this.getChildNodes().get(this.getChildCount() - 2).update();
    }
    added.update();
    this.update();
    this.resetLearnedSlots();
  }
  /**
   * Adds a child node.
   *
   * <p>Equivalent to:
   *
   * <pre>{@code
   * insertChildNode(childNodes().size(), std::move(node));
   *
   * }</pre>
   *
   * <p>
   *
   * @see WTreeNode#insertChildNode(int index, WTreeNode node)
   */
  public WTreeNode addChildNode(WTreeNode node) {
    WTreeNode result = node;
    this.insertChildNode(this.getChildCount(), node);
    return result;
  }
  /** Removes a child node. */
  public WTreeNode removeChildNode(WTreeNode node) {
    node.parentNode_ = null;
    WTreeNode result = null;
    if (this.childrenLoaded_) {
      result = WidgetUtils.remove(this.getChildContainer(), node);
    } else {
      result = CollectionUtils.take(this.notLoadedChildren_, node);
    }
    this.descendantRemoved(node);
    this.updateChildren();
    return result;
  }
  /** Returns the list of children. */
  public List<WTreeNode> getChildNodes() {
    List<WTreeNode> result = new ArrayList<WTreeNode>();
    ;

    for (int i = 0; i < this.getChildContainer().getCount(); ++i) {
      result.add(ObjectUtils.cast(this.getChildContainer().getWidget(i), WTreeNode.class));
    }
    for (WTreeNode i : this.notLoadedChildren_) {
      result.add(i);
    }
    return result;
  }
  /**
   * Returns the number of children that should be displayed.
   *
   * <p>This is used to display the count in the count label. The default implementation simply
   * returns {@link WTreeNode#getChildNodes() getChildNodes()}.size().
   */
  public int getDisplayedChildCount() {
    return this.getChildCount();
  }
  /**
   * Configures how and when the child count should be displayed.
   *
   * <p>By default, no child count indication is disabled (this is the behaviour since 2.1.1). Use
   * this method to enable child count indications.
   *
   * <p>The child count policy is inherited by all children in the tree.
   */
  public void setChildCountPolicy(ChildCountPolicy policy) {
    if (policy != ChildCountPolicy.Disabled && !(this.childCountLabel_ != null)) {
      this.childCountLabel_ = new WText();
      this.getLabelArea().addWidget(this.childCountLabel_);
      this.childCountLabel_.setMargin(new WLength(7), EnumSet.of(Side.Left));
      this.childCountLabel_.setStyleClass("Wt-childcount");
    }
    this.childCountPolicy_ = policy;
    if (this.childCountPolicy_ == ChildCountPolicy.Enabled) {
      WTreeNode parent = this.getParentNode();
      if (parent != null && parent.isExpanded()) {
        if (this.isDoPopulate()) {
          this.update();
        }
      }
    }
    if (this.childCountPolicy_ != ChildCountPolicy.Disabled) {
      List<WTreeNode> children = this.getChildNodes();
      for (WTreeNode c : children) {
        c.setChildCountPolicy(this.childCountPolicy_);
      }
    }
  }
  /**
   * Returns the child count policy.
   *
   * <p>
   *
   * @see WTreeNode#setChildCountPolicy(ChildCountPolicy policy)
   */
  public ChildCountPolicy getChildCountPolicy() {
    return this.childCountPolicy_;
  }
  /**
   * Sets the load policy for this tree.
   *
   * <p>This may only be set on the root of a tree, and before adding any children.
   */
  public void setLoadPolicy(ContentLoading loadPolicy) {
    this.loadPolicy_ = loadPolicy;
    switch (loadPolicy) {
      case Eager:
        this.loadChildren();
        break;
      case NextLevel:
        if (this.isExpanded()) {
          this.loadChildren();
          this.loadGrandChildren();
        } else {
          WTreeNode parent = this.getParentNode();
          if (parent != null && parent.isExpanded()) {
            this.loadChildren();
          }
          this.expandIcon_
              .icon1Clicked()
              .addListener(
                  this,
                  (WMouseEvent e1) -> {
                    WTreeNode.this.loadGrandChildren();
                  });
        }
        break;
      case Lazy:
        if (this.isExpanded()) {
          this.loadChildren();
        } else {
          if (this.childCountPolicy_ == ChildCountPolicy.Enabled) {
            WTreeNode parent = this.getParentNode();
            if (parent != null && parent.isExpanded()) {
              this.isDoPopulate();
            }
          }
          this.expandIcon_
              .icon1Clicked()
              .addListener(
                  this,
                  (WMouseEvent e1) -> {
                    WTreeNode.this.expand();
                  });
        }
    }
    if (this.loadPolicy_ != ContentLoading.Lazy) {
      List<WTreeNode> children = this.getChildNodes();
      for (WTreeNode c : children) {
        c.setLoadPolicy(this.loadPolicy_);
      }
    }
  }
  /** Returns whether this node is expanded. */
  public boolean isExpanded() {
    return !this.collapsed_;
  }
  /**
   * Allows this node to be selected.
   *
   * <p>By default, all nodes may be selected.
   *
   * <p>
   *
   * @see WTreeNode#isSelectable()
   * @see WTree#select(WTreeNode node, boolean selected)
   */
  public void setSelectable(boolean selectable) {
    this.selectable_ = selectable;
  }
  /**
   * Returns if this node may be selected.
   *
   * <p>
   *
   * @see WTreeNode#setSelectable(boolean selectable)
   */
  public boolean isSelectable() {
    return this.selectable_;
  }
  /**
   * Returns the parent node.
   *
   * <p>
   *
   * @see WTreeNode#getChildNodes()
   */
  public WTreeNode getParentNode() {
    return this.parentNode_;
  }
  /**
   * Sets the visibility of the node itself.
   *
   * <p>If <code>false</code>, then the node itself is not displayed, but only its children. This is
   * typically used to hide the root node of a tree.
   */
  public void setNodeVisible(boolean visible) {
    this.visible_ = visible;
    this.updateChildren(false);
  }
  /**
   * Sets whether this node&apos;s children are decorated.
   *
   * <p>By default, node&apos;s children have expand/collapse and other lines to display their
   * linkage and offspring.
   *
   * <p>By setting <code>decorated</code> to <code>false</code>, you can hide the decorations for
   * the node&apos;s children.
   */
  public void setChildrenDecorated(boolean decorated) {
    this.childrenDecorated_ = decorated;
    this.updateChildren(false);
  }
  /**
   * Sets whether this node is interactive.
   *
   * <p>Interactive nodes can be clicked upon and will populate a list of children when clicked. By
   * disabling the interactivity, a node will not react to a click event.
   */
  public void setInteractive(boolean interactive) {
    this.interactive_ = interactive;
  }
  /**
   * Expands this node.
   *
   * <p>Besides the actual expansion of the node, this may also trigger the loading and population
   * of the node children, or of the children&apos;s children.
   *
   * <p>
   *
   * @see WTreeNode#collapse()
   * @see WTreeNode#doExpand()
   */
  public void expand() {
    if (!this.isExpanded()) {
      if (!this.childrenLoaded_) {
        this.loadChildren();
      }
      if (this.getParentNode() != null && this.getChildCount() == 0) {
        this.getParentNode().resetLearnedSlots();
        this.update();
      }
      if (this.loadPolicy_ == ContentLoading.NextLevel) {
        this.loadGrandChildren();
      }
      this.doExpand();
      this.updateChildren();
    }
  }
  /**
   * Collapses this node.
   *
   * <p>
   *
   * @see WTreeNode#expand()
   * @see WTreeNode#doCollapse()
   */
  public void collapse() {
    if (this.isExpanded()) {
      this.doCollapse();
    }
  }
  /**
   * Signal emitted when the node is expanded by the user.
   *
   * <p>
   *
   * @see WTreeNode#collapsed()
   */
  public EventSignal1<WMouseEvent> expanded() {
    return this.expandIcon_.icon1Clicked();
  }
  /**
   * Signal emitted when the node is collapsed by the user.
   *
   * <p>
   *
   * @see WTreeNode#expanded()
   */
  public EventSignal1<WMouseEvent> collapsed() {
    return this.expandIcon_.icon2Clicked();
  }
  /**
   * Signal that is emitted when the node is added or removed from the selection
   *
   * <p>
   *
   * @see WTree#itemSelectionChanged()
   */
  public Signal1<Boolean> selected() {
    return this.selected_;
  }
  /**
   * Creates a tree node with empty {@link WTreeNode#getLabelArea() getLabelArea()}.
   *
   * <p>This tree node has no label or labelicon, and is therefore ideally suited to provide a
   * custom look.
   */
  protected WTreeNode(WTreeNode parentNode) {
    super();
    this.collapsed_ = true;
    this.selectable_ = true;
    this.visible_ = true;
    this.childrenDecorated_ = true;
    this.parentNode_ = null;
    this.notLoadedChildren_ = new ArrayList<WTreeNode>();
    this.childCountPolicy_ = ChildCountPolicy.Disabled;
    this.labelIcon_ = null;
    this.childrenLoaded_ = false;
    this.populated_ = false;
    this.interactive_ = true;
    this.selected_ = new Signal1<Boolean>();
    this.clickedConnection_ = new AbstractSignal.Connection();
    this.init(WString.Empty, (WIconPair) null);
    if (parentNode != null) parentNode.addChildNode(this);
  }
  /**
   * Creates a tree node with empty {@link WTreeNode#getLabelArea() getLabelArea()}.
   *
   * <p>Calls {@link #WTreeNode(WTreeNode parentNode) this((WTreeNode)null)}
   */
  protected WTreeNode() {
    this((WTreeNode) null);
  }
  /**
   * Accesses the container widget that holds the label area.
   *
   * <p>Use this to customize how the label should look like.
   */
  protected WContainerWidget getLabelArea() {
    return (WContainerWidget) this.layout_.resolveWidget("label-area");
  }
  /**
   * Populates the node dynamically on loading.
   *
   * <p>Reimplement this method if you want to populate the widget dynamically, as the tree is being
   * browsed and therefore loaded. This is only usefull with LazyLoading or NextLevelLoading
   * strategies.
   */
  protected void populate() {}
  /**
   * Returns whether this node has already been populated.
   *
   * <p>
   *
   * @see WTreeNode#populate()
   */
  protected boolean isPopulated() {
    return this.populated_;
  }
  /**
   * Returns whether this node can be expanded.
   *
   * <p>The default implementation populates the node if necessary, and then checks if there are any
   * child nodes.
   *
   * <p>You may wish to reimplement this method if you reimplement {@link WTreeNode#populate()
   * populate()}, and you have a quick default for determining whether a node may be expanded (which
   * does not require populating the node).
   *
   * <p>
   *
   * @see WTreeNode#populate()
   */
  protected boolean isExpandable() {
    if (this.interactive_) {
      this.isDoPopulate();
      return this.getChildCount() > 0;
    } else {
      return false;
    }
  }
  /**
   * Renders the node to be selected.
   *
   * <p>The default implementation changes the style class of the {@link WTreeNode#getLabelArea()
   * getLabelArea()} to &quot;selected&quot;.
   */
  protected void renderSelected(boolean isSelected) {
    this.layout_.bindString(
        "selected", isSelected ? WApplication.getInstance().getTheme().getActiveClass() : "");
    this.selected().trigger(isSelected);
  }
  /**
   * Reacts to the removal of a descendant node.
   *
   * <p>Reimplement this method if you wish to react on the removal of a descendant node. The
   * default implementation simply propagates the event to the parent.
   */
  protected void descendantRemoved(WTreeNode node) {
    WTreeNode parent = this.getParentNode();
    if (parent != null) {
      parent.descendantRemoved(node);
    }
  }
  /**
   * Reacts to the addition of a descendant node.
   *
   * <p>Reimplement this method if you wish to react on the addition of a descendant node. The
   * default implementation simply propagates the event to the parent.
   */
  protected void descendantAdded(WTreeNode node) {
    WTreeNode parent = this.getParentNode();
    if (parent != null) {
      parent.descendantAdded(node);
    }
  }
  /**
   * The actual expand.
   *
   * <p>This method, which is implemented as a stateless slot, performs the actual expansion of the
   * node.
   *
   * <p>You may want to reimplement this function (and {@link WTreeNode#undoDoExpand()
   * undoDoExpand()}) if you wish to do additional things on node expansion.
   *
   * <p>
   *
   * @see WTreeNode#doCollapse()
   * @see WTreeNode#expand()
   */
  protected void doExpand() {
    this.wasCollapsed_ = !this.isExpanded();
    this.collapsed_ = false;
    this.expandIcon_.setState(1);
    this.getChildContainer().show();
    if (this.labelIcon_ != null) {
      this.labelIcon_.setState(1);
    }
    List<WTreeNode> children = this.getChildNodes();
    for (WTreeNode c : children) {
      c.doCollapse();
    }
  }
  /**
   * The actual collapse.
   *
   * <p>This method, which is implemented as a stateless slot, performs the actual collapse of the
   * node.
   *
   * <p>You may want to reimplement this function (and {@link WTreeNode#undoDoCollapse()
   * undoDoCollapse()}) if you wish to do additional things on node expansion.
   *
   * <p>
   *
   * @see WTreeNode#doExpand()
   * @see WTreeNode#collapse()
   */
  protected void doCollapse() {
    this.wasCollapsed_ = !this.isExpanded();
    this.collapsed_ = true;
    this.expandIcon_.setState(0);
    this.getChildContainer().hide();
    if (this.labelIcon_ != null) {
      this.labelIcon_.setState(0);
    }
  }
  /**
   * Undo method for {@link WTreeNode#doCollapse() doCollapse()} stateless implementation.
   *
   * <p>
   *
   * @see WTreeNode#doCollapse()
   */
  protected void undoDoExpand() {
    if (this.wasCollapsed_) {
      this.expandIcon_.setState(0);
      this.getChildContainer().hide();
      if (this.labelIcon_ != null) {
        this.labelIcon_.setState(0);
      }
      this.collapsed_ = true;
    }
    List<WTreeNode> children = this.getChildNodes();
    for (WTreeNode c : children) {
      c.undoDoCollapse();
    }
  }
  /**
   * Undo method for {@link WTreeNode#doCollapse() doCollapse()} stateless implementation.
   *
   * <p>
   *
   * @see WTreeNode#doExpand()
   */
  protected void undoDoCollapse() {
    if (!this.wasCollapsed_) {
      this.expandIcon_.setState(1);
      this.getChildContainer().show();
      if (this.labelIcon_ != null) {
        this.labelIcon_.setState(1);
      }
      this.collapsed_ = false;
    }
  }
  /** Accesses the icon pair that allows expansion of the tree node. */
  protected WIconPair getExpandIcon() {
    return this.expandIcon_;
  }

  protected WTemplate getImpl() {
    return this.layout_;
  }

  private boolean collapsed_;
  private boolean selectable_;
  private boolean visible_;
  private boolean childrenDecorated_;
  private WTreeNode parentNode_;
  private List<WTreeNode> notLoadedChildren_;
  private ContentLoading loadPolicy_;
  private ChildCountPolicy childCountPolicy_;
  private WTemplate layout_;
  private WIconPair expandIcon_;
  private WText noExpandIcon_;
  private WIconPair labelIcon_;
  private WText labelText_;
  private WText childCountLabel_;
  private boolean childrenLoaded_;
  private boolean populated_;
  private boolean interactive_;
  private Signal1<Boolean> selected_;

  private void init(final CharSequence labelText, WIconPair labelIcon) {
    this.setImplementation(
        this.layout_ = new WTemplate(tr("Wt.WTreeNode.template"), (WContainerWidget) null));
    this.setStyleClass("Wt-tree");
    this.layout_.setSelectable(false);
    this.layout_.bindEmpty("cols-row");
    this.layout_.bindEmpty("trunk-class");
    // this.implementStateless(WTreeNode.doExpand,WTreeNode.undoDoExpand);
    // this.implementStateless(WTreeNode.doCollapse,WTreeNode.undoDoCollapse);
    WApplication app = WApplication.getInstance();
    WContainerWidget children = new WContainerWidget();
    this.layout_.bindWidget("children", children);
    children.setList(true);
    children.hide();
    if (WApplication.getInstance().getLayoutDirection() == LayoutDirection.RightToLeft) {
      this.expandIcon_ =
          new WIconPair(
              app.getTheme().getResourcesUrl() + imagePlusRtl_,
              app.getTheme().getResourcesUrl() + imageMinRtl_);
      this.layout_.bindWidget("expand", this.expandIcon_);
    } else {
      this.expandIcon_ =
          new WIconPair(
              app.getTheme().getResourcesUrl() + imagePlus_,
              app.getTheme().getResourcesUrl() + imageMin_);
      this.layout_.bindWidget("expand", this.expandIcon_);
    }
    this.expandIcon_.setStyleClass("Wt-ctrl Wt-expand");
    this.expandIcon_.hide();
    this.noExpandIcon_ = new WText();
    this.layout_.bindWidget("no-expand", this.noExpandIcon_);
    this.noExpandIcon_.setStyleClass("Wt-ctrl Wt-noexpand");
    this.addStyleClass("Wt-trunk");
    this.layout_.bindWidget("label-area", new WContainerWidget());
    this.childCountLabel_ = null;
    if (this.labelIcon_ != null) {
      this.getLabelArea().addWidget(labelIcon);
      this.labelIcon_.setVerticalAlignment(AlignmentFlag.Middle);
    }
    this.labelText_ = new WText(WString.toWString(labelText));
    this.getLabelArea().addWidget(this.labelText_);
    this.labelText_.setStyleClass("Wt-label");
    this.childrenLoaded_ = false;
    this.setLoadPolicy(ContentLoading.Lazy);
  }

  private WContainerWidget getChildContainer() {
    return (WContainerWidget) this.layout_.resolveWidget("children");
  }

  private int getChildCount() {
    return this.getChildContainer().getCount() + this.notLoadedChildren_.size();
  }

  private void loadChildren() {
    if (!this.childrenLoaded_) {
      this.isDoPopulate();
      for (int i = 0; i < this.notLoadedChildren_.size(); ++i) {
        this.getChildContainer().addWidget(this.notLoadedChildren_.get(i));
      }
      this.notLoadedChildren_.clear();
      this.expandIcon_
          .icon1Clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WTreeNode.this.doExpand();
              });
      this.expandIcon_
          .icon2Clicked()
          .addListener(
              this,
              (WMouseEvent e1) -> {
                WTreeNode.this.doCollapse();
              });
      this.resetLearnedSlots();
      this.childrenLoaded_ = true;
    }
  }

  private void loadGrandChildren() {
    List<WTreeNode> children = this.getChildNodes();
    for (WTreeNode c : children) {
      c.loadChildren();
    }
  }
  // private void create() ;
  private void update() {
    boolean isLast = this.isLastChildNode();
    if (!this.visible_) {
      this.layout_.bindString("selected", "Wt-root");
      this.getChildContainer().addStyleClass("Wt-root");
    } else {
      if (this.getTree() != null) {
        final Set<WTreeNode> s = this.getTree().getSelectedNodes();
        if (s.contains(this) != false) {
          this.layout_.bindString(
              "selected", WApplication.getInstance().getTheme().getActiveClass());
        } else {
          this.layout_.bindEmpty("selected");
        }
      } else {
        this.layout_.bindEmpty("selected");
      }
      this.getChildContainer().removeStyleClass("Wt-root");
    }
    WTreeNode parent = this.getParentNode();
    if (parent != null && !parent.childrenDecorated_) {}
    if (this.expandIcon_.getState() != (this.isExpanded() ? 1 : 0)) {
      this.expandIcon_.setState(this.isExpanded() ? 1 : 0);
    }
    if (this.getChildContainer().isHidden() != !this.isExpanded()) {
      this.getChildContainer().setHidden(!this.isExpanded());
    }
    if (this.labelIcon_ != null && this.labelIcon_.getState() != (this.isExpanded() ? 1 : 0)) {
      this.labelIcon_.setState(this.isExpanded() ? 1 : 0);
    }
    this.toggleStyleClass("Wt-trunk", !isLast);
    this.layout_.bindString("trunk-class", isLast ? "Wt-end" : "Wt-trunk");
    if (!(this.getParentNode() != null) || this.getParentNode().isExpanded()) {
      if (this.childCountPolicy_ == ChildCountPolicy.Enabled && !this.populated_) {
        this.isDoPopulate();
      }
      this.expandIcon_.setHidden(!this.isExpandable());
      this.noExpandIcon_.setHidden(this.isExpandable());
    }
    if (this.childCountPolicy_ != ChildCountPolicy.Disabled
        && this.populated_
        && this.childCountLabel_ != null) {
      int n = this.getDisplayedChildCount();
      if (n != 0) {
        this.childCountLabel_.setText(new WString("(" + String.valueOf(n) + ")"));
      } else {
        this.childCountLabel_.setText(new WString());
      }
    }
    if (this.getTree() != null
        && this.getTree().isDisabled()
        && this.getTree().getTreeRoot() != null) {
      WTreeNode root = this.getTree().getTreeRoot();
      WApplication app = WApplication.getInstance();
      String themedDisabledClass = app != null ? app.getTheme().getDisabledClass() : "";
      for (WTreeNode node : root.getChildNodes()) {
        if (!node.hasStyleClass(themedDisabledClass)) {
          node.addStyleClass(themedDisabledClass);
          node.getLabel().addStyleClass(themedDisabledClass);
        }
      }
    }
  }

  private boolean isLastChildNode() {
    WTreeNode parent = this.getParentNode();
    if (parent != null) {
      return parent.getChildNodes().get(parent.getChildNodes().size() - 1) == this;
    } else {
      return true;
    }
  }

  private void updateChildren(boolean recursive) {
    List<WTreeNode> children = this.getChildNodes();
    for (WTreeNode c : children) {
      if (recursive) {
        c.updateChildren(recursive);
      } else {
        c.update();
      }
    }
    this.update();
    this.resetLearnedSlots();
  }

  private final void updateChildren() {
    updateChildren(false);
  }

  private boolean wasCollapsed_;

  private boolean isDoPopulate() {
    if (!this.populated_) {
      this.populated_ = true;
      this.populate();
      return true;
    } else {
      return false;
    }
  }

  private static String imagePlus_ = "nav-plus.gif";
  private static String imageMin_ = "nav-minus.gif";
  private static String imagePlusRtl_ = "nav-plus-rtl.gif";
  private static String imageMinRtl_ = "nav-minus-rtl.gif";
  AbstractSignal.Connection clickedConnection_;
}
