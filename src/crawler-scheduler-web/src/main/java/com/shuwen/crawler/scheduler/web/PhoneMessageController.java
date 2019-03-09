package com.shuwen.crawler.scheduler.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.alicloud.openservices.tablestore.SyncClient;
import com.google.common.collect.Maps;
import com.shuwen.crawler.common.JobType;
import com.shuwen.crawler.common.PipelineCategory;
import com.shuwen.crawler.common.TaskExtraField;
import com.shuwen.crawler.common.monitor.FromType;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import org.apache.commons.lang3.StringUtils;
import com.shuwen.crawler.common.util.OtsUtil;
import com.shuwen.crawler.rpc.PhoneMsgService;
import com.shuwen.crawler.rpc.dto.PhoneDataDto;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.codecraft.webmagic.Request;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.*;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017/10/18 15:55
 * 手机通知栏
 * 提供给代理服务调用的接口
 */
@RestController
@RequestMapping("phoneMessage")
public class PhoneMessageController {
    Logger logger = LoggerFactory.getLogger(PhoneMessageController.class);

    Logger phoneMessageLog = LoggerFactory.getLogger("PHONE_MESSAGE_LOG");

    private final long jobId = 1001;

    @Autowired
    private TaskServiceFactory taskService;

    @Autowired
    private PhoneMsgService phoneMsgService;

    @Value("${aliyun.access.key}")
    private String akId;
    @Value("${ots.endpoint}")
    private String ots_endpoint;
    @Value("${aliyun.secret.key}")
    private String skId;
    @Value("${ots.instanceName}")
    private String instance;


    @RequestMapping("send")
    public String send(String url, String httpContent,String headers,String moduleConfig){
        TaskDO taskDO = new TaskDO();
        taskDO.setJobId(jobId);
        taskDO.setJobUrl(url);
        taskDO.setJobType(JobType.EXTRA_TASK);
        taskDO.setTaskFrom(FromType.APP_CLIENT.getValue());
        Map<String, Object> extras = Maps.newHashMap();
        extras.put(TaskExtraField.PAGE, httpContent);
        extras.put(TaskExtraField.IS_HOT, "1");

        if(!StringUtils.isBlank(headers)){
            JSONObject headerJson = JSONObject.parseObject(headers);
            extras.put(Request.ADD_HEADERS,headerJson);
        }

        if(!StringUtils.isBlank(moduleConfig)){
            JSONObject configJson = JSONObject.parseObject(moduleConfig);
            if(configJson.containsKey(TaskExtraField.GROUP_ID)){
                taskDO.setJobCategory(configJson.getInteger(TaskExtraField.GROUP_ID));
            }else{
                taskDO.setJobCategory(PipelineCategory.PHONE_MESSAGE.getValue());
            }
            Iterator<String> iterator = configJson.keySet().iterator();
            while(iterator.hasNext()){
                String key = iterator.next();
                Object o = configJson.get(key);
                extras.put(key,o);
            }
        }

        taskDO.setExtras(extras);
        taskDO.setExpectedRunTime(new Date());
        taskDO.setTaskVersion(new DateTime().toString("yyyyMMddHHmmss"));
        try {
            taskService.addTask(taskDO);
        } catch (IOException e) {
            logger.error("通知栏 http api 调用异常", e);
        }
        phoneMessageLog.info("add task: "+url);
//        logger.info("通知栏任务添加成功!"+ JSON.toJSONString(taskDO));
        return "ok";
    }

    @GetMapping("getData")
    public Set<PhoneDataDto> getData(String phoneType){
        List<PhoneDataDto> list=phoneMsgService.getPhoneData(phoneType);
        Set<PhoneDataDto>result = new HashSet(list);
        return result;
    }


    @Scheduled(cron = "0 0 6 * * ?")
    public String crawlerSeachOts(){
        String instance = "crawler-db";
        SyncClient syncClient = new SyncClient(ots_endpoint, akId, skId, instance);
        List<String> sr=new ArrayList<>();
        sr.add("site");sr.add("url");sr.add("keyWords");sr.add("dataType");
        phoneMsgService.savePhone(OtsUtil.getOtsName(syncClient,"xhzy_video_trans",sr),"douyin");
        return "";
    }


}
