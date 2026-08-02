package com.promptnotepad.app.model

data class TodoTask(
    val rawText: String,
    val isCompleted: Boolean,
    val priority: Char?,
    val context: String?,
    val project: String?
)
