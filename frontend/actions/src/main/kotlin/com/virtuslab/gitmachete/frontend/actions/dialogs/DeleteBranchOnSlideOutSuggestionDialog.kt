package com.virtuslab.gitmachete.frontend.actions.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.virtuslab.gitmachete.frontend.actions.base.BaseSlideOutAction.DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.format
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.JComponent

data class SlideOutOptions(
  @get:JvmName("shouldRemember") val remember: Boolean = false,
  @get:JvmName("shouldDelete") val delete: Boolean = false
)

class DeleteBranchOnSlideOutSuggestionDialog(project: Project, val branchName: String) :
  DialogWrapper(project, /* canBeParent */ true) {

  // Must NOT be private to prevent:
  // java.lang.IllegalAccessException: class
  // kotlin.reflect.jvm.internal.calls.CallerImpl$FieldGetter$BoundInstance
  // cannot access a member of class ... with modifiers "private"
  var remember = false
  var delete = false

  init {
    title = getString("action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.title")
    isResizable = false
    super.init()
  }

  fun showAndGetSlideOutOptions() = if (showAndGet()) SlideOutOptions(remember, delete) else null

  override fun createActions(): Array<Action?> = emptyArray()

  override fun createCenterPanel() = panel {
    indent {
      row {
        label(
          format(
            getString(
              "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.note-1.HTML"
            ),
            branchName
          )
        )
      }
      row {
        label(
          format(
            getString(
              "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.note-2"
            )
          )
        )
      }
    }
    row {
      button(
        getString(
          "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.delete-text"
        )
      ) {
        delete = true
        doOKAction()
      }
        .component.apply { mnemonic = KeyEvent.VK_D }
      button(
        getString(
          "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.keep-text"
        )
      ) {
        delete = false
        doOKAction()
      }
        .component.apply { mnemonic = KeyEvent.VK_K }
      button(
        getString(
          "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.cancel-text"
        )
      ) {
        delete = true
        close(CANCEL_EXIT_CODE)
      }
        .component.apply { mnemonic = KeyEvent.VK_C }
    }
    row {
      checkBox(
        format(
          getString(
            "action.GitMachete.BaseSlideOutAction.deletion-suggestion-dialog.remember-choice.HTML"
          ),
          DELETE_LOCAL_BRANCH_ON_SLIDE_OUT_GIT_CONFIG_KEY
        )
      )
        .bindSelected(::remember)
        .component.apply {
          mnemonic = KeyEvent.VK_R
          isSelected = false
        }
    }
  }

  // this will lower the gap between the last row and the bottom border
  override fun createSouthPanel(): JComponent? = null
}
