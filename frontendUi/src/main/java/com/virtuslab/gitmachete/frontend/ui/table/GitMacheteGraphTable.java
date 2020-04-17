package com.virtuslab.gitmachete.frontend.ui.table;

import static com.virtuslab.gitmachete.frontend.actions.ActionIDs.ACTION_CHECK_OUT;
import static com.virtuslab.gitmachete.frontend.actions.ActionIDs.GROUP_TO_INVOKE_AS_CONTEXT_MENU;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.DataKeys;
import com.virtuslab.gitmachete.frontend.graph.coloring.GraphEdgeColorToJBColorMapper;
import com.virtuslab.gitmachete.frontend.graph.elements.IGraphElement;
import com.virtuslab.gitmachete.frontend.graph.print.GraphCellPainter;
import com.virtuslab.gitmachete.frontend.ui.VcsRootDropdown;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCell;
import com.virtuslab.gitmachete.frontend.ui.cell.BranchOrCommitCellRenderer;

// TODO (#99): consider applying SpeedSearch for branches and commits
public final class GitMacheteGraphTable extends JBTable implements DataProvider {
  private final GraphTableModel graphTableModel;
  private final Project project;
  private final AtomicReference<@Nullable IGitMacheteRepository> gitMacheteRepositoryRef;
  private final VcsRootDropdown vcsRootDropdown;
  private String upperTextForEmptyGraph = "";
  private String lowerTextForEmptyGraph = "";
  // wasTextForEmptyGraphChanged var is mainly to prevent invocation of getEmptyText() in updateUI() method in early
  // stage of GitMacheteGraphTable existence that cause IllegalStateException coz getEmptyText() returns null. This is
  // also introduced for performance optimization (to not update empty text when unnecessary)
  private boolean doesTextForEmptyGraphRequireUpdate = false;

  @Nullable
  private String selectedBranchName;

  @UIEffect
  public GitMacheteGraphTable(
      GraphTableModel graphTableModel,
      Project project,
      AtomicReference<@Nullable IGitMacheteRepository> gitMacheteRepositoryRef,
      VcsRootDropdown vcsRootDropdown) {
    super(graphTableModel);

    this.graphTableModel = graphTableModel;
    this.project = project;
    this.gitMacheteRepositoryRef = gitMacheteRepositoryRef;
    this.vcsRootDropdown = vcsRootDropdown;

    // InitializationChecker allows us to invoke the below methods because the class is final
    // and all `@NonNull` fields are already initialized. `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.

    GraphCellPainter graphCellPainter = new GraphCellPainter(
        GraphEdgeColorToJBColorMapper::getColor) {
      @Override
      protected int getRowHeight() {
        return GitMacheteGraphTable.this.getRowHeight();
      }
    };

    initColumns();

    @SuppressWarnings("guieffect:assignment.type.incompatible")
    @AlwaysSafe
    BranchOrCommitCellRenderer branchOrCommitCellRenderer = new BranchOrCommitCellRenderer(this, graphCellPainter);
    setDefaultRenderer(BranchOrCommitCell.class, branchOrCommitCellRenderer);

    setCellSelectionEnabled(false);
    setShowVerticalLines(false);
    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    getColumnModel().setColumnSelectionAllowed(false);

    ScrollingUtil.installActions(this, false);

    addMouseListener(new GitMacheteGraphTableMouseAdapter(this));
  }

  public void setTextForEmptyGraph(String upperText, String lowerText) {
    upperTextForEmptyGraph = upperText;
    lowerTextForEmptyGraph = lowerText;
    doesTextForEmptyGraphRequireUpdate = true;
  }

  @Override
  @UIEffect
  public void updateUI(@UnknownInitialization GitMacheteGraphTable this) {
    super.updateUI();
    if (doesTextForEmptyGraphRequireUpdate) {
      // We can safely ignore the warning related to `this` being possibly not fully initialized,
      // since `doesTextForEmptyGraphRequireUpdate` can only be set to true in `setTextForEmptyGraph()`,
      // which in turn can only be called on an @Initialized instance.
      @SuppressWarnings({"nullness:argument.type.incompatible", "nullness:method.invocation.invalid"})
      var __ = getEmptyText().setText(upperTextForEmptyGraph).appendSecondaryText(lowerTextForEmptyGraph,
          StatusText.DEFAULT_ATTRIBUTES, /* listener */ null);
      doesTextForEmptyGraphRequireUpdate = false;
    }
  }

  @UIEffect
  private void initColumns() {
    createDefaultColumnsFromModel();

    // Otherwise sizes would be recalculated after each TableColumn re-initialization
    setAutoCreateColumnsFromModel(false);
  }

  @Override
  public GraphTableModel getModel() {
    return graphTableModel;
  }

  private <T> Match.Case<String, T> typeSafeCase(DataKey<T> key, T value) {
    return Case($(key.getName()), value);
  }

  @Override
  @Nullable
  public Object getData(String dataId) {
    var gitMacheteRepository = gitMacheteRepositoryRef.get();
    return Match(dataId).of(
        typeSafeCase(DataKeys.KEY_IS_GIT_MACHETE_REPOSITORY_READY, gitMacheteRepository != null),
        typeSafeCase(DataKeys.KEY_GIT_MACHETE_REPOSITORY, gitMacheteRepository),
        typeSafeCase(DataKeys.KEY_SELECTED_BRANCH_NAME, selectedBranchName),
        typeSafeCase(DataKeys.KEY_SELECTED_VCS_REPOSITORY, vcsRootDropdown.getValue()),
        typeSafeCase(CommonDataKeys.PROJECT, project),
        Case($(), (Object) null));
  }

  protected class GitMacheteGraphTableMouseAdapter extends MouseAdapter {

    private final GitMacheteGraphTable graphTable;

    @UIEffect
    public GitMacheteGraphTableMouseAdapter(GitMacheteGraphTable graphTable) {
      this.graphTable = graphTable;
    }

    @UIEffect
    public void mouseClicked(MouseEvent e) {
      Point point = e.getPoint();
      int row = rowAtPoint(point);
      int col = columnAtPoint(point);

      // check if we click on one of branches
      if (row < 0 || col < 0) {
        return;
      }

      BranchOrCommitCell cell = (BranchOrCommitCell) getModel().getValueAt(row, col);
      IGraphElement element = cell.getElement();
      if (!element.isBranch()) {
        return;
      }

      selectedBranchName = element.getValue();

      if (SwingUtilities.isRightMouseButton(e)) {
        ActionGroup contextMenuGroup = (ActionGroup) ActionManager.getInstance()
            .getAction(GROUP_TO_INVOKE_AS_CONTEXT_MENU);
        ActionPopupMenu actionPopupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN,
            contextMenuGroup);
        actionPopupMenu.getComponent().show(graphTable, (int) point.getX(), (int) point.getY());
      } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && !e.isConsumed()) {
        e.consume();
        AnActionEvent actionEvent = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, new Presentation(),
            DataManager.getInstance().getDataContext(graphTable));
        ActionManager.getInstance().getAction(ACTION_CHECK_OUT).actionPerformed(actionEvent);
      }
    }
  }
}
