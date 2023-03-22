package com.yuyin.demo.view.file

import com.yuyin.demo.utils.YuYinUtil
import java.io.File

class FileItem(path: File) {
    var file_name = ""
    private var resultFiles: YuYinUtil.ResultFiles
    var textFile: File
    var jsonFile: File

    init {
        file_name = path.nameWithoutExtension
        resultFiles = YuYinUtil.ResultFiles.getTextAndJson(path)
        textFile = resultFiles.textFile
        jsonFile = resultFiles.json
    }
}