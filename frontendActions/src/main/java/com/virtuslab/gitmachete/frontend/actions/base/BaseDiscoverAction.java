package com.virtuslab.gitmachete.frontend.actions.base;

import static com.intellij.openapi.application.ModalityState.NON_MODAL;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.GuiUtils;
import io.vavr.control.Try;
import lombok.CustomLog;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositoryCache;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepositorySnapshot;
import com.virtuslab.gitmachete.frontend.actions.dialogs.GraphTableDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseGraphTable;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public class BaseDiscoverAction extends DumbAwareAction implements IExpectsKeyProject {
  @Override
  public void actionPerformed(AnActionEvent anActionEvent) {
    var project = getProject(anActionEvent);
    var selectedRepoProvider = project.getService(SelectedGitRepositoryProvider.class)
        .getGitRepositorySelectionProvider();
    var selectedRepositoryOption = selectedRepoProvider.getSelectedGitRepository();
    assert selectedRepositoryOption.isDefined() : "Selected repository is undefined";
    var selectedRepository = selectedRepositoryOption.get();
    var mainDirPath = GitVfsUtils.getMainDirectoryPath(selectedRepository).toAbsolutePath();
    var gitDirPath = GitVfsUtils.getGitDirectoryPath(selectedRepository).toAbsolutePath();
    Try.of(() -> RuntimeBinding.instantiateSoleImplementingClass(IGitMacheteRepositoryCache.class)
        .getInstance(mainDirPath, gitDirPath).discoverLayoutAndCreateSnapshot())
        .onFailure(e -> GuiUtils.invokeLaterIfNeeded(() -> VcsNotifier.getInstance(project)
            .notifyError("Repository discover error", e.getMessage() != null ? e.getMessage() : ""), NON_MODAL))
        .onSuccess(repoSnapshot -> GuiUtils
            .invokeLaterIfNeeded(() -> GraphTableDialog.of(repoSnapshot, /* windowTitle */ "Discovered branch tree",
                repositorySnapshot -> saveDiscoveredLayout(repositorySnapshot,
                    GitVfsUtils.getMacheteFilePath(selectedRepository), project, getGraphTable(anActionEvent)),
                /* okButtonText */ "Save Discovered Layout", /* cancelButtonVisible */ true).show(), NON_MODAL));
  }

  private void saveDiscoveredLayout(IGitMacheteRepositorySnapshot repositorySnapshot, Path macheteFilePath, Project project,
      BaseGraphTable gitMacheteGraphTable) {
    var branchLayoutOption = repositorySnapshot.getBranchLayout();
    assert branchLayoutOption.isDefined() : "Branch layout from discovered repository is not defined";
    new Task.Backgroundable(project, getString("action.GitMachete.BaseDiscoverAction.write-file-task-title")) {
      @Override
      public void run(ProgressIndicator indicator) {
        Try.of(() -> {
          var branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
          branchLayoutWriter.write(macheteFilePath, branchLayoutOption.get(), /* backupBranchLayout */ true);
          return null;
        })
            .onFailure(e -> VcsNotifier.getInstance(project).notifyError(
                /* title */ getString("action.GitMachete.BaseDiscoverAction.write-file-error-title"),
                /* message */ e.getMessage() != null ? e.getMessage() : ""))
            .onSuccess(__ -> gitMacheteGraphTable.queueRepositoryUpdateAndModelRefresh(() -> {}));
      }
    }.queue();
  }

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }
}
