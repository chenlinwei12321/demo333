package com.shuwen.crawler.scheduler.util;

import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.util.HttpClientUtil;
import us.codecraft.webmagic.selector.Html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频源码解析服务接口提供
 */
public class VideoParseUtil {

    /**
     * 解析优酷数据源码
     * @param url
     * @return
     */
    private static String getYouku(String url){
        String videoId="";
        Matcher matcher2= Pattern.compile("http:\\/\\/v\\.youku\\.com\\/v\\_show\\/id\\_(.*)==\\.html(.*)").matcher(url);
        while(matcher2.find()){
            videoId =matcher2.group(1);
        }
        return "http://player.youku.com/embed/"+videoId;
    }

    /**
     * 解析腾讯视频源码
     * @param url
     * @return
     */
    private static String getQQ(String url){
        String html=HttpClientUtil.httpGet("http://v.ranks.xin/video-parse.php?url="+url,null,3,null);
        JSONObject obj=JSONObject.parseObject(html);
        String result=obj.getJSONArray("data").getJSONObject(0).getString("url");
        return result;
    }


}
