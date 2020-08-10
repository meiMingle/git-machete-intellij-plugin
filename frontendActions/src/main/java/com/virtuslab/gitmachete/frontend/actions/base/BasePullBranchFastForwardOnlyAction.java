package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

import com.virtuslab.gitmachete.backend.api.IGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRemoteBranch;
import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.FetchBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.backgroundables.PullCurrentBranchFastForwardOnlyBackgroundable;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyGitMacheteRepository;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeyProject;
import com.virtuslab.logger.IEnhancedLambdaLogger;

@CustomLog
public abstract class BasePullBranchFastForwardOnlyAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      IExpectsKeyGitMacheteRepository,
      IExpectsKeyProject,
      ISyncToRemoteStatusDependentAction {

  @Override
  public IEnhancedLambdaLogger log() {
    return LOG;
  }

  @Override
  public @I18nFormat({}) String getActionName() {
    return getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.action-name");
  }

  @Override
  public @I18nFormat({}) String getActionNameForDescription() {
    return getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.description-action-name");
  }

  @Override
  public List<SyncToRemoteStatus.Relation> getEligibleRelations() {
    return List.of(
        SyncToRemoteStatus.Relation.BehindRemote,
        SyncToRemoteStatus.Relation.InSyncToRemote);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    super.onUpdate(anActionEvent);
    syncToRemoteStatusDependentActionUpdate(anActionEvent);
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    log().debug("Performing");

    var project = getProject(anActionEvent);
    var gitRepository = getSelectedGitRepository(anActionEvent).getOrNull();
    var localBranchName = getNameOfBranchUnderAction(anActionEvent).getOrNull();
    var gitMacheteRepositorySnapshot = getGitMacheteRepositorySnapshot(anActionEvent).getOrNull();

    if (localBranchName != null && gitRepository != null && gitMacheteRepositorySnapshot != null) {
      var localBranch = gitMacheteRepositorySnapshot.getManagedBranchByName(localBranchName).getOrNull();
      if (localBranch == null) {
        // This is generally NOT expected, the action should never be triggered
        // for an unmanaged branch in the first place.
        log().warn("Branch '${localBranchName}' not found or not managed by Git Machete");
        return;
      }
      var remoteBranch = localBranch.getRemoteTrackingBranch().getOrNull();
      if (remoteBranch == null) {
        // This is generally NOT expected, the action should never be triggered
        // for an untracked branch in the first place (see `getEligibleRelations`)
        log().warn("Branch '${localBranchName}' does not have a remote tracking branch");
        return;
      }

      var currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(b -> b.getName()).getOrNull();
      if (localBranchName.equals(currentBranchName)) {
        doPullCurrentBranchFastForwardOnly(project, gitRepository, remoteBranch);
      } else {
        doPullNonCurrentBranchFastForwardOnly(project, gitRepository, localBranch, remoteBranch);
      }
    }
  }

  private void doPullCurrentBranchFastForwardOnly(Project project,
      GitRepository gitRepository,
      IGitMacheteRemoteBranch remoteBranch) {

    new PullCurrentBranchFastForwardOnlyBackgroundable(project, gitRepository, remoteBranch,
        getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.task-title")).queue();
  }

  private void doPullNonCurrentBranchFastForwardOnly(Project project,
      GitRepository gitRepository,
      IGitMacheteBranch localBranch,
      IGitMacheteRemoteBranch remoteBranch) {

    var remoteName = remoteBranch.getRemoteName();
    var remoteBranchFullNameAsLocalBranchOnRemote = remoteBranch.getFullNameAsLocalBranchOnRemote();
    var remoteBranchFullName = remoteBranch.getFullName();
    var localBranchFullName = localBranch.getFullName();

    // Note the '+' sign preceding the refspec. It permits non-fast-forward updates.
    // This strategy is used to fetch branch from remote repository to remote branch in our repository.
    var refspecFromRemoteRepoToOurRemoteBranch = "+${remoteBranchFullNameAsLocalBranchOnRemote}:${remoteBranchFullName}";

    // On the other hand this refspec has no '+' sign.
    // This is because we want a fetch from remote branch in our repository
    // to local branch in our repository to only ever be fast-forward.
    var refspecFromOurRemoteBranchToOurLocalBranch = "${remoteBranchFullName}:${localBranchFullName}";

    String taskTitle = getString("action.GitMachete.BasePullBranchFastForwardOnlyAction.task-title");

    new FetchBackgroundable(project, gitRepository, remoteName, refspecFromRemoteRepoToOurRemoteBranch, taskTitle) {
      // We can only enqueue the update of local branch once the update of remote branch is completed.
      @Override
      public void onSuccess() {
        new FetchBackgroundable(project, gitRepository, LOCAL_REPOSITORY_NAME,
            refspecFromOurRemoteBranchToOurLocalBranch, taskTitle).queue();
      }
    }.queue();
  }
}
