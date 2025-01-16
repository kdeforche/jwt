/*
 * Copyright (C) 2020 Emweb bv, Herent, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.examples.widgetgallery;

import eu.webtoolkit.jwt.*;
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

class TreesTables extends Topic {
  private static Logger logger = LoggerFactory.getLogger(TreesTables.class);

  public TreesTables() {
    super();
    this.filteredCocktails = null;
    this.filteredSortedCocktails = null;
  }

  public void populateSubMenu(WMenu menu) {
    menu.setInternalBasePath("/trees-tables");
    menu.addItem(
            "Tables",
            DeferredWidget.deferCreate(
                () -> {
                  return TreesTables.this.tables();
                }))
        .setPathComponent("");
    menu.addItem(
        "Trees",
        DeferredWidget.deferCreate(
            () -> {
              return TreesTables.this.trees();
            }));
    menu.addItem(
        "Tree Tables",
        DeferredWidget.deferCreate(
            () -> {
              return TreesTables.this.treeTables();
            }));
    menu.addItem(
        "MVC Table Views",
        DeferredWidget.deferCreate(
            () -> {
              return TreesTables.this.tableViews();
            }));
    menu.addItem(
        "MVC Tree Views",
        DeferredWidget.deferCreate(
            () -> {
              return TreesTables.this.treeViews();
            }));
    menu.addItem(
        "MVC Item models",
        DeferredWidget.deferCreate(
            () -> {
              return TreesTables.this.itemModels();
            }));
  }

  private WLineEdit regexpFilter;
  private WSortFilterProxyModel filteredCocktails;
  private WSortFilterProxyModel filteredSortedCocktails;

  private WWidget tables() {
    TopicTemplate result = new TopicTemplate("treestables-Tables");
    result.bindWidget("PlainTable", PlainTable());
    result.bindWidget("StyledTable", StyledTable());
    return result;
  }

  private WWidget trees() {
    TopicTemplate result = new TopicTemplate("treestables-Trees");
    result.bindWidget("Tree", Tree());
    return result;
  }

  private WWidget treeTables() {
    TopicTemplate result = new TopicTemplate("treestables-TreeTables");
    result.bindWidget("TreeTable", TreeTable());
    return result;
  }

  private WWidget tableViews() {
    TopicTemplate result = new TopicTemplate("treestables-TableViews");
    result.bindWidget("SmallTableView", SmallTableView());
    result.bindWidget("LargeTableView", LargeTableView());
    result.bindWidget("ComboDelegateTable", ComboDelegateTable());
    return result;
  }

  private WWidget treeViews() {
    TopicTemplate result = new TopicTemplate("treestables-TreeViews");
    result.bindWidget("TreeView", TreeView());
    return result;
  }

  private WWidget itemModels() {
    TopicTemplate result = new TopicTemplate("treestables-ItemModels");
    result.bindWidget("LargeTableView", LargeTableView());
    result.bindWidget("TreeView", TreeView());
    return result;
  }
  // private WWidget proxyModels() ;
  private WStringListModel stringList_;
  // private void changeRegexp() ;
  private static Employee[] employees = {
    new Employee("Mark", "Otto", 100),
    new Employee("Jacob", "Thornton", 50),
    new Employee("Larry the Bird", "", 10)
  };

  WWidget PlainTable() {
    WTable table = new WTable();
    table.setHeaderCount(1);
    table.setWidth(new WLength("100%"));
    new WText("#", (WContainerWidget) table.getElementAt(0, 0));
    new WText("First Name", (WContainerWidget) table.getElementAt(0, 1));
    new WText("Last Name", (WContainerWidget) table.getElementAt(0, 2));
    new WText("Pay", (WContainerWidget) table.getElementAt(0, 3));
    for (int i = 0; i < 3; ++i) {
      final Employee employee = employees[i];
      int row = i + 1;
      new WText(new WString("{1}").arg(row), (WContainerWidget) table.getElementAt(row, 0));
      new WText(employee.firstName, (WContainerWidget) table.getElementAt(row, 1));
      new WText(employee.lastName, (WContainerWidget) table.getElementAt(row, 2));
      new WLineEdit(
          new WString("{1}").arg(employee.pay).toString(),
          (WContainerWidget) table.getElementAt(row, 3));
    }
    return table;
  }

  void addOptionToggle(
      final WWidget widget, String option, final String styleClass, WContainerWidget parent) {
    final WCheckBox checkBox = new WCheckBox(option, (WContainerWidget) parent);
    checkBox.setInline(false);
    checkBox
        .changed()
        .addListener(
            this,
            () -> {
              widget.toggleStyleClass(styleClass, checkBox.isChecked());
            });
  }

  WWidget StyledTable() {
    WTable table = new WTable();
    WTable table_ = table;
    table_.setHeaderCount(1);
    new WText("#", (WContainerWidget) table_.getElementAt(0, 0));
    new WText("First Name", (WContainerWidget) table_.getElementAt(0, 1));
    new WText("Last Name", (WContainerWidget) table_.getElementAt(0, 2));
    new WText("Pay", (WContainerWidget) table_.getElementAt(0, 3));
    for (int i = 0; i < 3; ++i) {
      final Employee employee = employees[i];
      int row = i + 1;
      new WText(new WString("{1}").arg(row), (WContainerWidget) table_.getElementAt(row, 0));
      new WText(employee.firstName, (WContainerWidget) table_.getElementAt(row, 1));
      new WText(employee.lastName, (WContainerWidget) table_.getElementAt(row, 2));
      new WLineEdit(
          new WString("{1}").arg(employee.pay).toString(),
          (WContainerWidget) table_.getElementAt(row, 3));
    }
    table_.addStyleClass("table");
    WContainerWidget result = new WContainerWidget();
    result.addWidget(table);
    new WText("Options:", (WContainerWidget) result);
    addOptionToggle(table_, "borders", "table-bordered", result);
    addOptionToggle(table_, "hover", "table-hover", result);
    addOptionToggle(table_, "small", "table-sm", result);
    addOptionToggle(table_, "stripes", "table-striped", result);
    return result;
  }

  WWidget Tree() {
    WContainerWidget container = new WContainerWidget();
    WTree tree = new WTree();
    container.addWidget(tree);
    tree.setSelectionMode(SelectionMode.Extended);
    WIconPair folderIcon =
        new WIconPair("icons/yellow-folder-closed.png", "icons/yellow-folder-open.png", false);
    WTreeNode node = new WTreeNode("Furniture", folderIcon);
    final WTreeNode furnitureNode = node;
    tree.setTreeRoot(node);
    tree.getTreeRoot().getLabel().setTextFormat(TextFormat.Plain);
    tree.getTreeRoot().setLoadPolicy(ContentLoading.NextLevel);
    tree.getTreeRoot().addChildNode(new WTreeNode("Table"));
    tree.getTreeRoot().addChildNode(new WTreeNode("Cupboard"));
    WTreeNode subtree = new WTreeNode("Chair");
    WTreeNode subtree_ = tree.getTreeRoot().addChildNode(subtree);
    tree.getTreeRoot().addChildNode(new WTreeNode("Coach"));
    tree.getTreeRoot().expand();
    subtree_.addChildNode(new WTreeNode("Doc"));
    subtree_.addChildNode(new WTreeNode("Grumpy"));
    subtree_.addChildNode(new WTreeNode("Happy"));
    subtree_.addChildNode(new WTreeNode("Sneezy"));
    subtree_.addChildNode(new WTreeNode("Dopey"));
    subtree_.addChildNode(new WTreeNode("Bashful"));
    subtree_.addChildNode(new WTreeNode("Sleepy"));
    WPushButton imageButton = new WPushButton("Use Image Icons");
    container.addWidget(imageButton);
    imageButton
        .clicked()
        .addListener(
            this,
            () -> {
              WIconPair icon =
                  new WIconPair(
                      "icons/yellow-folder-closed.png", "icons/yellow-folder-open.png", false);
              furnitureNode.setLabelIcon(icon);
            });
    WPushButton FAButton = new WPushButton("Use Font-Awesome Icons");
    container.addWidget(FAButton);
    FAButton.clicked()
        .addListener(
            this,
            () -> {
              WIconPair icon = new WIconPair("folder", "folder-open", false);
              icon.setIconsType(WIconPair.IconType.IconName);
              furnitureNode.setLabelIcon(icon);
            });
    return container;
  }

  static WTreeTableNode addNode(
      WTreeTableNode parent, String name, String yuppie, String holidays, String favorite) {
    WTreeTableNode node = new WTreeTableNode(name);
    WTreeTableNode node_ = node;
    parent.addChildNode(node);
    node_.setColumnWidget(1, new WText(yuppie));
    node_.setColumnWidget(2, new WText(holidays));
    node_.setColumnWidget(3, new WText(favorite));
    return node_;
  }

  WWidget TreeTable() {
    WTreeTable treeTable = new WTreeTable();
    treeTable.resize(new WLength(650), new WLength(200));
    treeTable.getTree().setSelectionMode(SelectionMode.Extended);
    treeTable.addColumn("Yuppie Factor", new WLength(125));
    treeTable.addColumn("# Holidays", new WLength(125));
    treeTable.addColumn("Favorite Item", new WLength(125));
    WTreeTableNode root = new WTreeTableNode("All Personnel");
    treeTable.setTreeRoot(root, "Emweb Organigram");
    WTreeTableNode group = new WTreeTableNode("Upper Management");
    WTreeTableNode group_ = group;
    treeTable.getTreeRoot().addChildNode(group);
    addNode(group_, "Chief Anything Officer", "-2.8", "20", "Scepter");
    addNode(group_, "Vice President of Parties", "13.57", "365", "Flag");
    addNode(group_, "Vice President of Staplery", "3.42", "27", "Perforator");
    group = new WTreeTableNode("Middle management");
    group_ = group;
    treeTable.getTreeRoot().addChildNode(group);
    addNode(group_, "Boss of the house", "9.78", "35", "Happy Animals");
    addNode(group_, "Xena caretaker", "8.66", "10", "Yellow bag");
    group = new WTreeTableNode("Actual Workforce");
    group_ = group;
    treeTable.getTreeRoot().addChildNode(group);
    addNode(group_, "The Dork", "9.78", "22", "Mojito");
    addNode(group_, "The Stud", "8.66", "46", "Toothbrush");
    addNode(group_, "The Ugly", "13.0", "25", "Paper bag");
    treeTable.getTreeRoot().expand();
    return treeTable;
  }

  WWidget SmallTableView() {
    WTableView tableView = new WTableView();
    tableView.setModel(CsvUtil.csvToModel("" + "table.csv"));
    tableView.setColumnResizeEnabled(false);
    tableView.setColumnAlignment(0, AlignmentFlag.Center);
    tableView.setHeaderAlignment(0, EnumSet.of(AlignmentFlag.Center));
    tableView.setAlternatingRowColors(true);
    tableView.setRowHeight(new WLength(28));
    tableView.setHeaderHeight(new WLength(28));
    tableView.setSelectionMode(SelectionMode.Single);
    tableView.setEditTriggers(EnumSet.of(EditTrigger.None));
    final int WIDTH = 120;
    for (int i = 0; i < tableView.getModel().getColumnCount(); ++i) {
      tableView.setColumnWidth(i, new WLength(120));
    }
    tableView.setWidth(new WLength((WIDTH + 7) * tableView.getModel().getColumnCount() + 2));
    return tableView;
  }

  WWidget LargeTableView() {
    WTableView tableView = new WTableView();
    tableView.setModel(new VirtualModel(10000, 50));
    tableView.setRowHeaderCount(1);
    tableView.setSortingEnabled(false);
    tableView.setAlternatingRowColors(true);
    tableView.setRowHeight(new WLength(28));
    tableView.setHeaderHeight(new WLength(28));
    tableView.setSelectionMode(SelectionMode.Extended);
    tableView.setEditTriggers(EnumSet.of(EditTrigger.None));
    tableView.resize(new WLength(650), new WLength(400));
    return tableView;
  }

  WWidget ComboDelegateTable() {
    WTableView table = new WTableView();
    List<WString> options = new ArrayList<WString>();
    options.add(new WString("apples"));
    options.add(new WString("pears"));
    options.add(new WString("bananas"));
    options.add(new WString("cherries"));
    WStandardItemModel model = new WStandardItemModel();
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        WStandardItem item = new WStandardItem();
        item.setData(0, ItemDataRole.User);
        item.setData(options.get(0), ItemDataRole.Display);
        item.setFlags(EnumSet.of(ItemFlag.Editable));
        model.setItem(i, j, item);
      }
    }
    table.setModel(model);
    table.setEditTriggers(EnumSet.of(EditTrigger.SingleClicked));
    WStringListModel slModel = new WStringListModel();
    slModel.setStringList(options);
    ComboDelegate customdelegate = new ComboDelegate(slModel);
    table.setItemDelegate(customdelegate);
    table.setSortingEnabled(false);
    table.setColumnResizeEnabled(false);
    table.setRowHeight(new WLength(40));
    table.setHeaderHeight(new WLength(0));
    final int WIDTH = 120;
    for (int i = 0; i < table.getModel().getColumnCount(); ++i) {
      table.setColumnWidth(i, new WLength(WIDTH));
    }
    table.setWidth(new WLength((WIDTH + 7) * table.getModel().getColumnCount() + 2));
    return table;
  }

  WWidget TreeView() {
    WTreeView treeView = new WTreeView();
    treeView.resize(new WLength(600), new WLength(400));
    GitModel model = new GitModel("/home/jwt/jwt/.git");
    treeView.setModel(model);
    treeView.setRowHeight(new WLength(24));
    treeView.setHeaderHeight(new WLength(24));
    treeView.setSortingEnabled(false);
    treeView.setSelectionMode(SelectionMode.Single);
    treeView.setEditTriggers(EnumSet.of(EditTrigger.None));
    return treeView;
  }
}
