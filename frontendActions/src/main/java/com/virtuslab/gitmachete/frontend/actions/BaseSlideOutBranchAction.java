package com.virtuslab.gitmachete.frontend.actions;

import static com.virtuslab.gitmachete.frontend.keys.ActionIDs.ACTION_REFRESH;

import java.io.IOException;

import javax.swing.Icon;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.impl.BranchLayoutFileSaver;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.frontend.keys.DataKeys;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_BRANCH_LAYOUT}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_FILE_PATH}</li>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link DataKeys#KEY_IS_GIT_MACHETE_REPOSITORY_READY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BaseSlideOutBranchAction extends GitMacheteRepositoryReadyAction {
  private static final Logger LOG = Logger.getInstance(BaseSlideOutBranchAction.class);

  public BaseSlideOutBranchAction(String text, String actionDescription, Icon icon) {
    super(text, actionDescription, icon);
  }

  public BaseSlideOutBranchAction() {}

  /**
   * See {@link BaseRebaseBranchOntoParentAction#actionPerformed} for the specific documentation.
   */
  @Override
  @UIEffect
  public abstract void actionPerformed(AnActionEvent anActionEvent);

  @UIEffect
  public void doSlideOut(AnActionEvent anActionEvent, BaseGitMacheteNonRootBranch branchToSlideOut) {

    Project project = anActionEvent.getProject();
    assert project != null;

    var branchLayout = anActionEvent.getData(DataKeys.KEY_BRANCH_LAYOUT);
    assert branchLayout != null;

    try {
      var branchName = branchToSlideOut.getName();
      var newBranchLayout = branchLayout.slideOut(branchName);
      var macheteFilePath = anActionEvent.getData(DataKeys.KEY_GIT_MACHETE_FILE_PATH);
      var branchLayoutFileSaver = new BranchLayoutFileSaver(macheteFilePath);

      try {
        branchLayoutFileSaver.save(newBranchLayout, /* backupOldFile */ true);
        ActionManager.getInstance().getAction(ACTION_REFRESH).actionPerformed(anActionEvent);
        VcsNotifier.getInstance(project).notifyInfo("Branch ${branchName} slid out");
      } catch (IOException e) {
        LOG.error("Failed to save machete file", e);
      }
    } catch (BranchLayoutException e) {
      String message = e.getMessage();
      VcsNotifier.getInstance(project).notifyError("Slide out failed", message == null ? "" : message);
    }
  }
}
