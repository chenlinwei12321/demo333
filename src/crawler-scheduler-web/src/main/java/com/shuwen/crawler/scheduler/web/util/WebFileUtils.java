package com.shuwen.crawler.scheduler.web.util;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

public class WebFileUtils {

    public static void downLoad(HttpServletResponse response,File file){
        OutputStream os = null;
        FileInputStream fis = null;
        try {
            os=response.getOutputStream();
            response.setContentType("application/force-download");// 设置强制下载不打开
            response.addHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(file.getName(), "UTF-8"));// 设置文件名
            fis = new FileInputStream(file);
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.close();
            fis.close();
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                os.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void downLoad(HttpServletResponse response,String content,String fileName){
        OutputStream os = null;
        FileInputStream fis = null;
        try {
            os=response.getOutputStream();
            response.setContentType("application/force-download");// 设置强制下载不打开
            response.addHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(fileName, "UTF-8"));// 设置文件名
            os.write(content.getBytes());
            os.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                os.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
