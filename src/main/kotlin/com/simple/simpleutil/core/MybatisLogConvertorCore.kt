package com.simple.simpleutil.core

object MybatisLogConvertorCore {
    fun parseMyBatisLog(logContent: String): List<String> {
        val sqlStatements: MutableList<String> = ArrayList()
        var currentSql: String? = null

        // 使用换行符分割日志字符串
        val lines = logContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            var line = line
            line = line.trim { it <= ' ' }  // 去除前后空格

            // 查找 SQL 语句
            if (line.contains("Preparing:")) {
                currentSql = line.substring(line.lastIndexOf("Preparing:") + 10).trim { it <= ' ' }  // 提取 SQL 语句
            } else if (line.contains("Parameters:")) {
                val parameters = line.substring(line.lastIndexOf("Parameters:") + 11).trim { it <= ' ' }
                if (currentSql != null) {
                    // 解析并拼接参数到 SQL 语句
                    val completeSql = buildCompleteSql(currentSql, parameters)
                    sqlStatements.add(completeSql)
                    currentSql = null // 清空当前 SQL 语句以便下次使用
                }
            }
        }

        return sqlStatements
    }

    private fun buildCompleteSql(sql: String, parameters: String): String {
        // 使用逗号分隔参数并拆分为数组
        var sql = sql
        val params = parameters.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (param in params) {
            // 处理每个参数，替换 SQL 中的占位符
            sql = sql.replaceFirst("\\?".toRegex(), param.replace(Regex("\\(.*?\\)"), "").trim { it <= ' ' })
        }
        return sql
    }
}