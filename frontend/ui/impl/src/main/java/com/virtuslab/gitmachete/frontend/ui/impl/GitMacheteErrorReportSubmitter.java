package com.virtuslab.gitmachete.frontend.ui.impl;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.awt.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.Consumer;
import com.intellij.util.ModalityUiUtil;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.utils.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

@CustomLog
public class GitMacheteErrorReportSubmitter extends ErrorReportSubmitter {

  @Override
  public String getReportActionText() {
    return getString("string.GitMachete.error-report-submitter.report-action-text");
  }

  @Override
  public boolean submit(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo,
      Component parentComponent,
      Consumer<? super SubmittedReportInfo> consumer) {
    try {
      val uri = constructNewGitHubIssueUri(events, additionalInfo);

      ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, () -> BrowserUtil.browse(uri));
    } catch (URISyntaxException e) {
      LOG.error("Cannot construct URI to open new bug issue!", e);
    }
    return true;
  }

  URI constructNewGitHubIssueUri(IdeaLoggingEvent[] events, @Nullable String additionalInfo) throws URISyntaxException {
    String title = Arrays.stream(events)
        .map(IdeaLoggingEvent::getThrowableText)
        .map(t -> t.indexOf(System.lineSeparator()) > 0 ? t.substring(0, t.indexOf(System.lineSeparator())) : t)
        .collect(Collectors.joining("; "));
    String reportBody = getReportBody(events, additionalInfo);

    val uriBuilder = new URIBuilder("https://github.com/VirtusLab/git-machete-intellij-plugin/issues/new");
    uriBuilder.addParameter("title", title);
    uriBuilder.addParameter("labels", "bug");
    uriBuilder.addParameter("body", reportBody);
    return uriBuilder.build();
  }

  private String getReportBody(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo) {
    String reportBody = getBugTemplate();
    for (java.util.Map.Entry<String, String> entry : getTemplateVariables(events, additionalInfo).entrySet()) {
      reportBody = reportBody.replace("%${entry.getKey()}%", entry.getValue());
    }
    return reportBody;
  }

  // An error (from a typo in resource name) will be captured by the tests.
  @SneakyThrows
  private String getBugTemplate() {
    return IOUtils.resourceToString("/bug_report.md", StandardCharsets.UTF_8);
  }

  private java.util.Map<String, String> getTemplateVariables(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo) {
    val templateVariables = new java.util.HashMap<String, String>();

    // Ide version, ie. Intellij Community 2021.3.1
    templateVariables.put("ide", ApplicationInfo.getInstance().getFullApplicationName());

    // Plugin version, ie. 1.1.1-10-SNAPSHOT git.c9a0e89-dirty
    IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.virtuslab.git-machete"));
    templateVariables.put("macheteVersion", pluginDescriptor != null ? pluginDescriptor.getVersion() : "<unknown>");

    // OS name and version
    val osName = SystemUtils.OS_NAME != null ? SystemUtils.OS_NAME : "";
    val osVersion = SystemUtils.OS_VERSION != null ? SystemUtils.OS_VERSION : "";
    templateVariables.put("os", osName + " " + osVersion);

    // Additional info about error
    templateVariables.put("additionalInfo", additionalInfo != null ? additionalInfo : "N/A");

    // Error stacktraces for events
    val sep = System.lineSeparator();
    String stacktraces = Arrays.stream(events)
        .map(IdeaLoggingEvent::getThrowableText)
        .map(t -> "```${sep}${t.strip()}${sep}```")
        .collect(Collectors.joining("${sep}${sep}"));
    templateVariables.put("stacktraces", stacktraces);

    return templateVariables;
  }
}
