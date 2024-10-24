package com.simple.simpleutil.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.simple.simpleutil.core.MybatisLogConvertorCore
import com.simple.simpleutil.util.ClipboardService

/**
 * mybatis 日志转换器动作
 * @author tang
 * @date 2024/10/24
 * @constructor 创建[MybatisLogConvertorAction]
 */
class MybatisLogConvertorAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        //获取编辑器
        val edit = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return

        //获取选中的字符
        val selectionModel = edit.selectionModel
        if (!selectionModel.hasSelection()) {
            // 如果没有选中文本，弹出提示信息
            Messages.showMessageDialog(project, "没有选中任何文本", "提示", Messages.getWarningIcon());
        }

        // 获取选中的文本
        val selectedText = selectionModel.selectedText!!

        //判断是选中的字符为mybatis日志文件
        if (!selectedText.contains("==>  Preparing:") && !selectedText.contains("==> Parameters:")) {
            Messages.showMessageDialog(
                project, "请选中mybatis日志文件(包含Preparing和Parameters)", "提示", Messages.getWarningIcon()
            );
        }

        val sql = MybatisLogConvertorCore.parseMyBatisLog(selectedText)
        if (sql.isNullOrEmpty()) {
            Messages.showMessageDialog(
                project, "解析失败，请检查日志文件是否正确", "提示", Messages.getWarningIcon()
            )
        }

        //复制到剪贴板
        ClipboardService.copyToClipboard(sql.joinToString("\n"))
    }
}