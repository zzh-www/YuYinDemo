package com.yuyin.demo.view.file

import java.io.File

class FileItem(path: File) {
    val file_path: File
    var file_name = ""

    init {
        file_name = path.nameWithoutExtension
        file_path = path
    }
}