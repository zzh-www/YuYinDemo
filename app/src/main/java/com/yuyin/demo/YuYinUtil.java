package com.yuyin.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class YuYinUtil {
    // 所需请求的权限
    public static final String[] appPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE
    };

    public static final int m_ALL_PERMISSIONS_PERMISSION_CODE = 1000;


    public static void save_file(Context context, ArrayList<SpeechText> speechList){
        Long timeStamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        String filename = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))))+".txt";
        File dir_path = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(dir_path.getAbsoluteFile()+File.separator+"YuYin"+File.separator+filename);
        StringBuilder total_result = new StringBuilder();

        for (SpeechText i : speechList ) {
            total_result.append(i.getText());
            total_result.append("\n");
        }
        try {
            if (file.createNewFile()) {
                try (OutputStream op = new FileOutputStream(file.getAbsolutePath())) {
                    op.write(total_result.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void get_all_result(ArrayList<SpeechText> speechList) {
//        while (true) {
//            String result = Recognize.getResult();
//            if (Recognize.getFinished()) {
//                break;
//            } else {
//                if (result.endsWith(" ")) {
//                    speechList.get(speechList.size()-1).setText(result.trim());
//                    speechList.add(new SpeechText("..."));
//                } else {
//                    speechList.get(speechList.size()-1).setText(result);
//                }
//            }
//        }
    }

    public static Boolean checkRequestPermissions(Activity activity, Context context) {
        List< String > listPermissionsNeeded = new ArrayList<>();
        for(String permission : appPermissions){
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED){
                listPermissionsNeeded.add(permission);
            }
        }

        if(!listPermissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), m_ALL_PERMISSIONS_PERMISSION_CODE);
            return false;
        }

        return true;
    }


    public static String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            YuYinLog.e("YUYINUTIl", "Error process asset " + assetName + " to file path");
        }
        return null;
    }

}
