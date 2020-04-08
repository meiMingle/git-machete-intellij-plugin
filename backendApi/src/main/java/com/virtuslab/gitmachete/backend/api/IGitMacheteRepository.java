package com.virtuslab.gitmachete.backend.api;

import java.util.Optional;

import io.vavr.collection.List;

public interface IGitMacheteRepository {
  List<BaseGitMacheteRootBranch> getRootBranches();

  Optional<BaseGitMacheteBranch> getCurrentBranchIfManaged();

  Optional<BaseGitMacheteBranch> getBranchByName(String branchName);

  IGitRebaseParameters getParametersForRebaseOntoParent(BaseGitMacheteNonRootBranch branch) throws GitMacheteException;

  IGitMergeParameters deriveParametersForMergeIntoParent(BaseGitMacheteNonRootBranch upstreamBranch);
}
