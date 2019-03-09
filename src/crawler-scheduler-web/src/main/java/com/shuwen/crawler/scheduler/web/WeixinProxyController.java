package com.shuwen.crawler.scheduler.web;

import cn.xhzy.crawler.basic.dao.mysql.CommonDao;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.JobType;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.TaskService;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.rule.util.RegexUtil;
import com.shuwen.crawler.rule.util.TextUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017/8/24 15:54
 */
@RestController
@RequestMapping("proxy")
public class WeixinProxyController {
    Logger logger = LoggerFactory.getLogger(WeixinProxyController.class);

    @Autowired
    private TaskServiceFactory taskService;
    @Resource
    private CommonDao<JSONObject> commonDao;

    final int WX_JOB_ID = 101;

    @PostMapping("submitUrl")
    public Object submitUrl(String messageBody, String url, String contentType, String headers){
//        System.out.println(messageBody);
        //json格式的文章链接列表数据
        if(contentType.contains("json")){
            JSONObject json = JSON.parseObject(messageBody);
            String generalMsgString = json.getString("general_msg_list");
            if(generalMsgString!=null){
                JSONObject generalMsg = JSON.parseObject(generalMsgString);
                parseJson(url, generalMsg, headers);
            }
        }else if(contentType.contains("html")){
            //首次加载，链接列表在js变量中
            Html html = new Html(messageBody);
            parseHtml(url, html, headers);
        }
        return "ok";
    }

    private void parseJson(String url, JSONObject json, String headers){
        //需要额外添加的请求头
        JSONObject addHeaders = JSON.parseObject(headers);
        String pass_ticket = getQueryParam(url, "pass_ticket");
        String uin = getQueryParam(url, "uin");
        String key = getQueryParam(url, "key");
        String abtest_cookie = getQueryParam(url, "abtest_cookie");

        String urlMd5 = DigestUtils.md5Hex(url);

        List<JSONObject> list = new LinkedList<>();
        JSONArray msgList = json.getJSONArray("list");
        for(int i=0;i<msgList.size();i++){
            JSONObject appMsgExtInfo = msgList.getJSONObject(i).getJSONObject("app_msg_ext_info");
            if(appMsgExtInfo==null){
                continue;
            }
            JSONObject extObj = new JSONObject();
            extObj.put("title", TextUtil.filterEmoji(appMsgExtInfo.getString("title")));
            extObj.put("author", appMsgExtInfo.getString("author"));
            extObj.put("coverImg", appMsgExtInfo.getString("cover"));
            extObj.put("digest", TextUtil.filterEmoji(appMsgExtInfo.getString("digest")));
            extObj.put("url", appMsgExtInfo.getString("content_url"));
            extObj.put("rawDataUrlMd5", urlMd5);
            list.add(extObj);

            JSONArray multiItems = appMsgExtInfo.getJSONArray("multi_app_msg_item_list");
            for(int j=0;j<multiItems.size();j++){
                JSONObject item = multiItems.getJSONObject(j);
                JSONObject obj = new JSONObject();
                obj.put("title", TextUtil.filterEmoji(item.getString("title")));
                obj.put("author", item.getString("author"));
                obj.put("coverImg", item.getString("cover"));
                obj.put("digest", TextUtil.filterEmoji(item.getString("digest")));
                obj.put("url", item.getString("content_url"));
                obj.put("rawDataUrlMd5", urlMd5);
                list.add(obj);
            }
        }
        for(JSONObject obj: list){
//            System.out.println("title: "+obj.getString("title"));
//            System.out.println("author: "+obj.getString("author"));
//            System.out.println("imgUrl: "+obj.getString("imgUrl"));
//            System.out.println("digest:"+ obj.getString("digest"));
//            System.out.println("contentUrl:"+obj.getString("contentUrl"));
//            System.out.println("-----------------------------------------------");
            //生成爬取任务，加入待执行表
            TaskDO taskDO = new TaskDO();
            taskDO.setJobId(WX_JOB_ID);
            taskDO.setJobType(JobType.SIMPLE_WEB_PAGE);
            taskDO.setJobUrl(obj.getString("url"));
            taskDO.setExpectedRunTime(new Date());
            taskDO.setExtras(new JSONObject().fluentPut("passage", obj)
                    .fluentPut("uin", uin)
                    .fluentPut("key", key)
                    .fluentPut("abtest_cookie", abtest_cookie)
                    .fluentPut("pass_ticket", pass_ticket)
                    .fluentPut(Request.ADD_HEADERS, addHeaders));
            try {
                taskService.addTask(taskDO);
                logger.warn(JSON.toJSONString(taskDO)+"\n已写入任务表。");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        String rawData = json.toJSONString();
//        //保留原始数据
//        commonDao.save("crawler_wx_raw", "id", new JSONObject().fluentPut("rawData", rawData)
//                .fluentPut("url", url).fluentPut("urlMd5", urlMd5));

    }

    private void parseHtml(String url, Html html, String headers){
        List<Selectable> scriptTags = html.xpath("//script").nodes();
        Selectable scriptTag = scriptTags.get(scriptTags.size()-2);
        String rawData = scriptTag.regex("var msgList = '(\\{.+})';\\W*if\\(!!").get();
        rawData = rawData.replaceAll("&quot;", "\"");
        rawData = rawData.replaceAll("&nbsp;", " ");
        rawData = rawData.replaceAll("&amp;", "&");
        rawData = rawData.replaceAll("&amp;", "&");
        rawData = rawData.replaceAll("\\\\\\\\\\\\\\\\n", "");
        rawData = rawData.replaceAll("\\\\\\\\", "");

        JSONObject json = JSON.parseObject(rawData);
        parseJson(url, json, headers);
    }
    private String getQueryParam(String url, String name){
        try {
            URL url1 = new URL(url);
            String query = url1.getQuery();
            return RegexUtil.captureOne(query, name + "=([^&]*)&?");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
