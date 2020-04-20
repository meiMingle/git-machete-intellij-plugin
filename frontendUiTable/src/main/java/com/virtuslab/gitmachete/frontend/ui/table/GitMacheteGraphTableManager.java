package com.virtuslab.gitmachete.frontend.ui.table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import com.intellij.util.messages.Topic;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.IGitMacheteRepository;
import com.virtuslab.gitmachete.backend.root.GitMacheteRepositoryBuilder;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraph;
import com.virtuslab.gitmachete.frontend.graph.repository.RepositoryGraphFactory;
import com.virtuslab.gitmachete.frontend.ui.selection.ISelectionChangeObservable;

public final class GitMacheteGraphTableManager {
  private static final Logger LOG = Logger.getInstance(GitMacheteGraphTableManager.class);

  private final Project project;
  @Getter
  @Setter
  private boolean isListingCommits;
  @Getter
  private final GitMacheteGraphTable gitMacheteGraphTable;
  private final AtomicReference<@Nullable IGitMacheteRepository> repositoryRef = new AtomicReference<>(null);
  private final RepositoryGraphFactory repositoryGraphFactory;
  private final ISelectionChangeObservable<GitRepository> selectionChangeObservable;

  public GitMacheteGraphTableManager(Project project,
      ISelectionChangeObservable<GitRepository> selectionChangeObservable) {
    this.project = project;
    this.isListingCommits = false;
    GraphTableModel graphTableModel = new GraphTableModel(RepositoryGraphFactory.getNullRepositoryGraph());
    this.gitMacheteGraphTable = new GitMacheteGraphTable(graphTableModel, project, repositoryRef,
        selectionChangeObservable);
    this.repositoryGraphFactory = new RepositoryGraphFactory();
    this.selectionChangeObservable = selectionChangeObservable;

    // InitializationChecker allows us to invoke instance methods below because the class is final
    // and all fields are already initialized. Hence, `this` is already `@Initialized` (and not just
    // `@UnderInitialization(GitMacheteGraphTableManager.class)`, as would be with a non-final class) at this point.
    subscribeToVcsRootChanges();
    subscribeToGitRepositoryChanges();
  }

  private void subscribeToVcsRootChanges() {
    // The method reference is invoked when user changes repository in dropdown menu
    selectionChangeObservable.addObserver(this::updateAndRefreshInBackground);
  }

  private void subscribeToGitRepositoryChanges() {
    Topic<GitRepositoryChangeListener> topic = GitRepository.GIT_REPO_CHANGE;
    GitRepositoryChangeListener listener = repository -> updateAndRefreshInBackground();
    project.getMessageBus().connect().subscribe(topic, listener);
  }

  public void refreshGraphTable() {
    GitRepository gitRepository = selectionChangeObservable.getValue();
    Path macheteFilePath = getMacheteFilePath(gitRepository);
    boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);

    refreshGraphTable(macheteFilePath, isMacheteFilePresent);
  }

  /** Creates a new repository graph and sets it to the graph table model. */
  private void refreshGraphTable(Path macheteFilePath, boolean isMacheteFilePresent) {
    // isUnitTestMode() checks if IDEA is running as a command line applet or in unit test mode.
    // No UI should be shown when IDEA is running in this mode.
    if (!project.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    // TODO (#176): When machete file is not present or it's empty, propose using automatically detected (by discover
    // functionality) branch layout

    IGitMacheteRepository gitMacheteRepository = repositoryRef.get();
    RepositoryGraph repositoryGraph;
    if (gitMacheteRepository == null) {
      repositoryGraph = RepositoryGraph.getNullRepositoryGraph();
    } else {
      repositoryGraph = repositoryGraphFactory.getRepositoryGraph(gitMacheteRepository, isListingCommits);
      if (gitMacheteRepository.getRootBranches().isEmpty()) {
        gitMacheteGraphTable.setTextForEmptyGraph(
            "Your machete file is empty.",
            "Please use 'git machete discover' CLI command to automatically fill in the machete file.");
      }
    }
    gitMacheteGraphTable.getModel().setRepositoryGraph(repositoryGraph);

    if (!isMacheteFilePresent) {
      gitMacheteGraphTable.setTextForEmptyGraph(
          "There is no machete file (${macheteFilePath}) for this repository.",
          "Please use 'git machete discover' CLI command to automatically create machete file.");
    }

    GuiUtils.invokeLaterIfNeeded(gitMacheteGraphTable::updateUI, ModalityState.NON_MODAL);
  }

  private Path getMacheteFilePath(GitRepository gitRepository) {
    // Using the deprecated `GitRepository#getGitDir` and not `GitRepository#getRepositoryFiles`
    // since the latter apparently doesn't allow to extract git dir
    // (just the paths of specific predefined files within the git dir).
    Path gitDirPath = Paths.get(gitRepository.getGitDir().getPath());
    return gitDirPath.resolve("machete");
  }

  public void updateAndRefreshInBackground() {
    if (project != null && !project.isDisposed()) {
      new Task.Backgroundable(project, "Updating Git Machete repository and refreshing") {
        @Override
        @UIEffect
        public void run(ProgressIndicator indicator) {
          GitRepository gitRepository = selectionChangeObservable.getValue();
          Path macheteFilePath = getMacheteFilePath(gitRepository);
          boolean isMacheteFilePresent = Files.isRegularFile(macheteFilePath);
          Path repoRootPath = Paths.get(gitRepository.getRoot().getPath());

          updateRepository(repoRootPath, isMacheteFilePresent);
          refreshGraphTable(macheteFilePath, isMacheteFilePresent);
        }
      }.queue();
    }
  }

  /**
   * Updates repository which is the base of graph table model. The change will be seen after
   * {@link GitMacheteGraphTableManager#refreshGraphTable()}.
   */
  private void updateRepository(Path repoRootPath, boolean isMacheteFilePresent) {
    if (isMacheteFilePresent) {
      var builder = new GitMacheteRepositoryBuilder(repoRootPath);
      var repository = Try.of(() -> builder.build())
          .onFailure(e -> LOG.error("Unable to create Git Machete repository", e)).get();
      repositoryRef.set(repository);
    } else {
      repositoryRef.set(null);
    }
  }
}
