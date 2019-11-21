package com.virtuslab.gitmachete.gitmachetejgit;

import com.virtuslab.gitmachete.gitmacheteapi.Branch;
import com.virtuslab.gitmachete.gitmacheteapi.Repository;
import lombok.Getter;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

@Getter
public class GitMacheteRepository implements Repository {
    private Path pathToRoot;
    List<Branch> rootBranches = new LinkedList<>();

    public GitMacheteRepository(Path pathToRoot) {
        this.pathToRoot = pathToRoot;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for(var b : rootBranches) {
            printBranch(b, 0, sb);
        }

        return sb.toString();
    }

    private void printBranch(Branch branch, int level, StringBuilder sb) {
        sb.append("\t".repeat(level));
        sb.append(branch.getName());
        sb.append(" - (Remote: ");
        sb.append(branch.getSyncToOriginStatus());
        sb.append("; Parent: ");
        sb.append(branch.getSyncToParentStatus());
        sb.append(")");
        for(var c : branch.getCommits()) {
            sb.append("; ");
            sb.append(c.getMessage().split("\n", 2)[0]);
        }
        sb.append(System.lineSeparator());

        for(var b : branch.getBranches()) {
            printBranch(b, level+1, sb);
        }
    }
}
