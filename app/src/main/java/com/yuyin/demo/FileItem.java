package com.yuyin.demo;

import java.io.File;

public class FileItem {
    private final File file_path;
    private String file_name = "";

    public FileItem(File path) {

        this.file_name = path.getName();
        this.file_path = path;
    }

    public String getFile_name() {
        return this.file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public File getFile_path() {
        return file_path;
    }
}
