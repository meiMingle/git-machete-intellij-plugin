package com.virtuslab.branchlayout.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.branchlayout.api.BaseBranchLayoutEntry;
import com.virtuslab.branchlayout.api.IBranchLayout;

@RequiredArgsConstructor
@Getter
public class BranchLayoutFileSaver {
  private final Path path;
  private Character indentCharacter = ' ';
  @NonNegative
  private int levelWidth = 0;

  public void save(IBranchLayout branchLayout, boolean backupOldFile) throws IOException {
    var lines = printBranchesOntoStringList(branchLayout.getRootBranches(), 0);

    if (backupOldFile) {
      Path parentDir = path.getParent();
      assert parentDir != null : "Can't get parent dir of branch relation file";
      Path pathToBackupFile = parentDir.resolve(path.getFileName() + "~");
      Files.copy(path, pathToBackupFile, StandardCopyOption.REPLACE_EXISTING);
    }

    Files.write(path, lines);
  }

  private List<String> printBranchesOntoStringList(List<BaseBranchLayoutEntry> branches, @NonNegative int level) {
    List<String> stringList = List.empty();
    for (var branch : branches) {
      var sb = new StringBuilder();
      sb.append(String.valueOf(indentCharacter).repeat(level * levelWidth)).append(branch.getName());
      Option<String> customAnnotation = branch.getCustomAnnotation();
      if (customAnnotation.isDefined()) {
        sb.append(" ").append(customAnnotation.get());
      }

      stringList = stringList.append(sb.toString())
          .appendAll(printBranchesOntoStringList(branch.getSubbranches(), level + 1));
    }
    return stringList;
  }
}
