package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import com.virtuslab.gitmachete.frontend.actions.common.GitMacheteBundle.message
import kotlin.apply
import kotlin.text.isEmpty
import kotlin.text.trim

class SlideInDialog constructor(project: Project,
                                private var parentName: String)
  : DialogWrapper(project, true) {

  private var branchName = ""

  init {
    title = message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.title")
    setOKButtonText(message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.ok-button"))
    setOKButtonMnemonic('I'.toInt())
    init()
  }

  fun showAndGetBranchName() = if (showAndGet()) branchName.trim() else null

  override fun createCenterPanel() = panel {
    row(message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.label.parent")) {
      label(parentName, bold = true)
    }
    row {
      label(message("action.GitMachete.BaseSlideInBranchBelowAction.dialog.slide-in.label"))
    }
    row {
      textField(::branchName, { branchName = it }).focused().withValidationOnApply(
          validateBranchName()).apply { startTrackingValidationIfNeeded() }
    }
  }

  private fun validateBranchName(): ValidationInfoBuilder.(javax.swing.JTextField) -> ValidationInfo? = {
    val errorInfo = git4idea.validators.checkRefName(it.text)
    if (errorInfo != null) error(errorInfo.message)
    else null
  }

  private fun CellBuilder<javax.swing.JTextField>.startTrackingValidationIfNeeded() {
    if (branchName.isEmpty()) {
      component.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: javax.swing.event.DocumentEvent) {
          startTrackingValidation()
          component.document.removeDocumentListener(this)
        }
      })
    } else {
      startTrackingValidation()
    }
  }
}
