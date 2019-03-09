package com.shuwen.crawler.scheduler.web.util;

import java.util.Map;

public class ReqMapUtils{

    public static String getStringFromReqMap(Map<String,Object> reqMap,String key){
        String result=null;
        if(!reqMap.containsKey(key)){
            return result;
        }
        try {
            result=reqMap.get(key).toString();
        }catch (Exception e){

        }
        return result;
    }

    public static Object withOutNullException(Map<String,Object> reqMap,String key){
        if(!reqMap.containsKey(key)){
            return null;
        }
        return reqMap.get(key);
    }

}
