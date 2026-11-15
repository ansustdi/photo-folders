package com.simple.photofolders.data

import java.io.File

data class Folder(
    val id: String,
    val name: String,
    val createdAt: Long,
    val photoCount: Int,
    val cover: File?,
)
