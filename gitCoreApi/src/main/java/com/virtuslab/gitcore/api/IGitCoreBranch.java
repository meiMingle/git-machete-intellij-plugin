package com.virtuslab.gitcore.api;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The only criterion for equality of any instances of any class implementing this interface is equality of
 * {@code getFullName} and {@code derivePointedCommit}
 */
public interface IGitCoreBranch {
  boolean isLocal();

  /**
   * @return {@code X} part of {@code refs/heads/X} or {@code [remote-name]/X} part of {@code refs/heads/[remote-name]/X}
   */
  String getName();

  /**
   * @return {@code refs/heads/X} or {@code refs/remotes/[remote-name]/X}
   */
  String getFullName();

  IGitCoreCommit getPointedCommit();

  List<IGitCoreReflogEntry> getReflog();

  @EnsuresNonNullIf(expression = "#2", result = true)
  @SuppressWarnings("interning:not.interned") // to allow for `self == other`
  static boolean defaultEquals(IGitCoreBranch self, @Nullable Object other) {
    if (self == other) {
      return true;
    } else if (!(other instanceof IGitCoreBranch)) {
      return false;
    } else {
      var o = (IGitCoreBranch) other;
      return self.getFullName().equals(o.getFullName())
          && Try.of(() -> self.getPointedCommit().equals(o.getPointedCommit())).getOrElse(false);
    }
  }

  static int defaultHashCode(IGitCoreBranch self) {
    return self.getFullName().hashCode() * 37 + Try.of(() -> self.getPointedCommit().hashCode()).getOrElse(0);
  }
}
