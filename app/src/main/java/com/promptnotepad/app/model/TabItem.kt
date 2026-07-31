package com.promptnotepad.app.model

import java.io.File
import java.util.UUID

data class TabItem(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val title: String = file.name
)
