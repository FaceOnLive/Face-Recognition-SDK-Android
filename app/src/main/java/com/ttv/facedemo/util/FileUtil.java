package com.ttv.facedemo.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {

    public static byte[] fileToData(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean saveDataToFile(byte[] data, File file, boolean append) {
        if (data == null){
            return false;
        }
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            return false;
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            return false;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, append);
            int bufferSize = 1024;
            int index = 0;
            while (index < data.length) {
                if (data.length - index < bufferSize) {
                    bufferSize = data.length - index;
                }
                fos.write(data, index, bufferSize);
                index += bufferSize;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean saveDataToFile(byte[] data, File file) {
        return saveDataToFile(data, file, false);
    }

}
