package com.promptnotepad.app.util

import com.promptnotepad.app.model.TodoTask

object TodoParser {

    private val priorityRegex = Regex("^\\(([A-Z])\\)\\s")
    private val contextRegex = Regex("@(\\w+)")
    private val projectRegex = Regex("\\+(\\w+)")

    fun parseLine(line: String): TodoTask {
        val isCompleted = line.startsWith("x ")
        val cleanLine = if (isCompleted) line.substring(2) else line

        val priority = priorityRegex.find(cleanLine)?.groupValues?.get(1)?.get(0)
        val context = contextRegex.find(cleanLine)?.groupValues?.get(1)
        val project = projectRegex.find(cleanLine)?.groupValues?.get(1)

        return TodoTask(
            rawText = line,
            isCompleted = isCompleted,
            priority = priority,
            context = context,
            project = project
        )
    }

    fun parseAll(content: String): List<TodoTask> {
        return content.lineSequence()
            .filter { it.isNotBlank() }
            .map { parseLine(it) }
            .toList()
    }

    fun isTodoLine(line: String): Boolean {
        val clean = if (line.startsWith("x ")) line.substring(2) else line
        return priorityRegex.containsMatchIn(clean) ||
            contextRegex.containsMatchIn(clean) ||
            projectRegex.containsMatchIn(clean)
    }
}
