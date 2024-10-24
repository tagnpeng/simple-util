package com.simple.simpleutil.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object ClipboardService {
    fun copyToClipboard(text: String?) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }

    fun copySelectedTextToClipboard(project: Project?, editor: Editor) {
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            val selectedText = selectionModel.selectedText
            copyToClipboard(selectedText)
            Messages.showMessageDialog(
                project, "Copied to clipboard: $selectedText", "Info", Messages.getInformationIcon()
            )
        } else {
            Messages.showMessageDialog(project, "No text selected!", "Warning", Messages.getWarningIcon())
        }
    }


}