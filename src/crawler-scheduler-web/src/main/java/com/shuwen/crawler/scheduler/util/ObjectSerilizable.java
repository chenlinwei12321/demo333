package com.shuwen.crawler.scheduler.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class ObjectSerilizable {

    public static void write2File(Object obj,File file){
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(oos);
        }
    }

    public static Object file2Obj(File file){
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return inputStream2Obj(fileInputStream);
    }

    public static Object inputStream2Obj(InputStream is){
        ObjectInputStream ois = null;
        Object o=null;
        try {
            ois = new ObjectInputStream(is);
            o= ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            IOUtils.closeQuietly(ois);
        }
        return o;
    }

}
