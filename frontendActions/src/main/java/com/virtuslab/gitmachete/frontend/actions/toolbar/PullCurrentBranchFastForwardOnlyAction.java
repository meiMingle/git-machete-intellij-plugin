package com.virtuslab.gitmachete.frontend.actions.toolbar;

import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.BehindRemote;
import static com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus.Relation.InSyncToRemote;

import com.intellij.openapi.actionSystem.AnActionEvent;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BasePullBranchFastForwardOnlyAction;

public class PullCurrentBranchFastForwardOnlyAction extends BasePullBranchFastForwardOnlyAction {
  @Override
  public Option<String> getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getCurrentBranchNameIfManaged(anActionEvent);
  }

  @Override
  @UIEffect
  public void onUpdate(AnActionEvent anActionEvent) {
    var isBehindOrInSyncToRemote = getNameOfBranchUnderAction(anActionEvent)
        .flatMap(bn -> getGitMacheteBranchByName(anActionEvent, bn))
        .map(b -> b.getSyncToRemoteStatus().getRelation())
        .map(strs -> List.of(BehindRemote, InSyncToRemote).contains(strs))
        .getOrElse(false);

    anActionEvent.getPresentation().setEnabledAndVisible(isBehindOrInSyncToRemote);

    if (isBehindOrInSyncToRemote) {
      super.onUpdate(anActionEvent);
    }
  }
}
