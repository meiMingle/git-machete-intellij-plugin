package com.virtuslab.gitmachete.backend.impl;

import java.util.Map;
import java.util.Optional;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.branchlayout.api.IBranchLayout;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.GitMacheteMissingForkPointException;
import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.api.IGitMergeParameters;
import com.virtuslab.gitmachete.backend.api.IGitRebaseParameters;

@RequiredArgsConstructor
public class GitMacheteRepository implements IGitMacheteRepository {

  @Getter
  private final List<BaseGitMacheteRootBranch> rootBranches;
  @Getter
  private final IBranchLayout branchLayout;

  @Nullable
  private final BaseGitMacheteBranch currentBranch;

  private final Map<String, BaseGitMacheteBranch> branchByName;

  @Override
  public Optional<BaseGitMacheteBranch> getCurrentBranchIfManaged() {
    return Optional.ofNullable(currentBranch);
  }

  @Override
  public Optional<BaseGitMacheteBranch> getBranchByName(String branchName) {
    return Optional.ofNullable(branchByName.get(branchName));
  }

  @Override
  public IGitRebaseParameters getParametersForRebaseOntoParent(BaseGitMacheteNonRootBranch branch)
      throws GitMacheteMissingForkPointException {
    var forkPoint = branch.getForkPoint();
    if (!forkPoint.isPresent()) {
      throw new GitMacheteMissingForkPointException(
          String.format("Cannot get fork point for branch \"%s\"", branch.getName()));
    }

    var newBaseBranch = branch.getUpstreamBranch();
    return new GitRebaseParameters(/* currentBranch */ branch, newBaseBranch.getPointedCommit(), forkPoint.get());
  }

  @Override
  public IGitMergeParameters deriveParametersForMergeIntoParent(BaseGitMacheteNonRootBranch branch) {
    return new GitMergeParameters(/* currentBranch */ branch, /* branchToMergeInto */ branch.getUpstreamBranch());
  }
}
