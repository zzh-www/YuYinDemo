package com.yuyin.demo.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

class FilesManagerViewModel : ViewModel() {
    val openFile = MutableSharedFlow<File>()
}