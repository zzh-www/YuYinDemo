package com.yuyin.demo

import java.io.File

class FileItem(path: File) {
    val file_path: File
    var file_name = ""

    init {
        file_name = path.name
        file_path = path
    }
}