package com.virtuslab.gitmachete.buildsrc

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.*
import java.util.*

fun Project.configureIntellijPlugin() {
  apply<IntelliJPlugin>()

  val isCI: Boolean by rootProject.extra
  val jetbrainsMarketplaceToken: String? by rootProject.extra

  configure<IntelliJPluginExtension> {
    instrumentCode.set(false)
    pluginName.set("git-machete-intellij-plugin")
    version.set(IntellijVersions.buildTarget)
    plugins.set(listOf("git4idea")) // Needed solely for ArchUnit
  }

  if (!isCI) {
    // The output of this task is for some reason very poorly cached,
    // and the task takes a significant amount of time,
    // while the index of searchable options is of little use for local development.
    tasks.withType<BuildSearchableOptionsTask> { enabled = false }
  }

  tasks.withType<PatchPluginXmlTask> {
    // `sinceBuild` is exclusive when we are using `*` in version but inclusive when without `*`
    sinceBuild.set(
      IntellijVersionHelper.toBuildNumber(IntellijVersions.earliestSupportedMajor)
    )

    // In `untilBuild` situation is inverted: it's inclusive when using `*` but exclusive when without `*`
    untilBuild.set(
      IntellijVersionHelper.toBuildNumber(IntellijVersions.latestSupportedMajor) + ".*"
    )

    // Note that the first line of the description should be self-contained since it is placed into embeddable card:
    // see e.g. https://plugins.jetbrains.com/search?search=git%20machete
    pluginDescription.set(file("$rootDir/DESCRIPTION.html").readText())

    changeNotes.set(
      "<h3>v${rootProject.version}</h3>\n\n${file("$rootDir/CHANGE-NOTES.html").readText()}"
    )
  }

  tasks.withType<RunIdeTask> { maxHeapSize = "4G" }

  tasks.withType<RunPluginVerifierTask> {
    val maybeEap = listOfNotNull(
      IntellijVersions.eapOfLatestSupportedMajor?.replace("-EAP-(CANDIDATE-)?SNAPSHOT".toRegex(), "")
    )

    ideVersions.set(
      IntellijVersions.latestMinorsOfOldSupportedMajors +
        IntellijVersions.latestStable +
        maybeEap
    )

    val skippedFailureLevels =
      EnumSet.of(
        RunPluginVerifierTask.FailureLevel.DEPRECATED_API_USAGES,
        RunPluginVerifierTask.FailureLevel.EXPERIMENTAL_API_USAGES,
        RunPluginVerifierTask.FailureLevel.NOT_DYNAMIC,
        RunPluginVerifierTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES
      )
    failureLevel.set(EnumSet.complementOf(skippedFailureLevels))
  }

  tasks.withType<PublishPluginTask> { token.set(jetbrainsMarketplaceToken) }
}
