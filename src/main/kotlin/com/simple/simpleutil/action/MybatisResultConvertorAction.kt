package com.simple.simpleutil.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.util.containers.stream
import com.simple.simpleutil.util.ClipboardService


class MybatisResultConvertorAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        //获取编辑器
        val edit = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return

        // 获取当前光标的位置
        val offset: Int = edit.caretModel.offset

        // 获取当前文件的虚拟文件
        val virtualFile = FileEditorManager.getInstance(project).selectedFiles[0] ?: return

        // 获取 PsiFile 对象
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return


        // 根据光标偏移量获取 PsiElement
        val elementAtCaret = psiFile.findElementAt(offset)
        if (elementAtCaret == null) {
            Messages.showMessageDialog(project, "请先将光标移到类名上", "提示", Messages.getWarningIcon())
            return
        }

        // 获取光标所在的类（如果有的话）
        val psiClass = PsiTreeUtil.getParentOfType(
            elementAtCaret, PsiClass::class.java
        )
        if (psiClass == null) {
            Messages.showMessageDialog(project, "请先将光标移到类名上", "提示", Messages.getWarningIcon())
            return
        }

        //解析成xml
        val result = getResult(psiClass)

        //复制到剪贴板
        ClipboardService.copyToClipboard(result)
    }

    private fun getResult(psiClass: PsiClass): String {
        val result = StringBuilder()
        result.appendLine("<resultMap id=\"${psiClass.name}ResultMap\" type=\"${psiClass.qualifiedName}\">")
        result.append(getDetail(psiClass))
        result.appendLine(" </resultMap>")
        return result.toString()
    }

    private fun getDetail(psiClass: PsiClass): String {
        val result = StringBuilder()
        val sortedFields = psiClass.fields.stream()
            .sorted(Comparator.comparing { field ->
                val canonicalText = field.type.canonicalText
                // 判断是否为基本类型
                if (isJavaCollection(canonicalText)) {
                    return@comparing 2 // 排前面
                } else if (isJavaBasicType(canonicalText)) {
                    return@comparing 0 // 排最后
                }
                1 // 其余类型排中间
            })
            .toList() // 收集到列表中
        for (field in sortedFields) {
            val type = field.type
            val fileName = field.name
            val canonicalText = type.getCanonicalText(false)
            val canonicalText1 = type.getCanonicalText(true)
            val presentableText = type.getPresentableText(false)
            val presentableText1 = type.getPresentableText(true)
            println("字段信息:${canonicalText}-${canonicalText1}-${presentableText}-${presentableText1}")
            if (fileName == "serialVersionUID") {
                continue
            }
            if (isJavaCollection(canonicalText)) {
                //集合类型
                val resolveClassInType = getGenerics(type)
                result.appendLine("<collection property=\"${fileName}\" columnPrefix=\"${fileName}.\" javaType=\"list\" ofType=\"${resolveClassInType.qualifiedName}\">")
                result.append(getDetail(resolveClassInType))
                result.appendLine("</collection>")
            } else if (isJavaBasicType(canonicalText)) {
                //基本类型
                result.appendLine("<result property=\"${fileName}\" column=\"${fileName}\"/>")
            } else {
                // 自定义类
                val resolveClassInType = PsiUtil.resolveClassInType(type)
                if (resolveClassInType != null) {
                    result.appendLine("<association property=\"${fileName}\" columnPrefix=\"${fileName}.\" javaType=\"${resolveClassInType?.qualifiedName}\">")
                    result.append(getDetail(resolveClassInType))
                    result.appendLine("</association>")
                }
            }
        }
        return result.toString()
    }

    private fun getGenerics(fieldType: PsiType): PsiClass {
        val classType = fieldType as PsiClassType

        // 获取字段的原始类型和泛型类型
        val parameters = classType.parameters

        if (parameters.isNotEmpty()) {
            val genericType = parameters[0]


            // 判断泛型类型是否为类类型
            if (genericType is PsiClassType) {
                val genericClass = genericType.resolve()
                if (genericClass != null) {
                    return genericClass;
                }
            }
        }
        throw RuntimeException("解析字段${fieldType.name}失败")
    }

    private fun isJavaCollection(canonicalText: String): Boolean {
        val javaCollection = arrayOf(
            "java.util.List",
            "java.util.Set",
            "java.util.ArrayList",
            "java.util.HashSet",
            "java.util.LinkedList",
            "java.util.Collections"
        )
        return javaCollection.stream().filter { canonicalText.startsWith(it) }
            .findFirst().isPresent
    }

    private fun isJavaBasicType(canonicalText: String): Boolean {
        val javaBasicType = arrayOf(
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.Void",
            "java.lang.String",
            "java.util.Map",
            "java.util.Date",
            "java.util.HashMap",
            "java.util.Iterator",
            "java.util.Objects",
            "java.time.LocalDate",
            "java.time.LocalTime",
            "java.time.LocalDateTime",
            "java.time.ZonedDateTime",
            "java.time.Instant",
            "java.time.Duration",
            "java.time.Period",
            "java.time.format.DateTimeFormatter",
            "int",
            "long",
            "double",
            "float",
            "boolean",
            "byte",
            "char",
            "short",
            "java.math.BigDecimal",
            "cn.hutool.core.lang.tree.TreeNodeConfig",
        )
        return javaBasicType.contains(canonicalText) || canonicalText.startsWith("java.") || canonicalText.startsWith("cn.hutool.")
    }

}