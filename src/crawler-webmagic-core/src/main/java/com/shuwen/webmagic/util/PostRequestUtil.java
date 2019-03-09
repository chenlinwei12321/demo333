package com.shuwen.webmagic.util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017/7/10 15:25
 */
public class PostRequestUtil {

    public static Request createPost(String url, Map<String, String> formParams){
        Request request = new Request(url);
        convertToPost(request, formParams);
        return request;
    }

    public static void convertToPost(Request request, Map<String, String> formParams){
        request.setMethod(HttpConstant.Method.POST);

        List<NameValuePair> nvs = new ArrayList<NameValuePair>();

        for(String key: formParams.keySet()){
            nvs.add(new BasicNameValuePair(key, formParams.get(key)));
        }

        NameValuePair[] values = nvs.toArray(new NameValuePair[nvs.size()]);

        Map<String, Object> params = new HashMap<>();
        params.put("nameValuePair", values);
        params.putAll(request.getExtras());
        request.setExtras(params);
    }
}
