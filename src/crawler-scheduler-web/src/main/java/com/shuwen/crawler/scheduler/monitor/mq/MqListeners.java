package com.shuwen.crawler.scheduler.monitor.mq;

import cn.xhzy.crawler.basic.utils.DomainUtils;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.ons.model.v20170918.OnsConsumerAccumulateRequest;
import com.aliyuncs.ons.model.v20170918.OnsConsumerAccumulateResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import javax.annotation.PostConstruct;

@Service
public class MqListeners {
    private static final Logger logger=LoggerFactory.getLogger(MqListeners.class);
    @Value("${task.mq.regionId}")
    private String regionId;
    @Value("${aliyun.access.key}")
    private String accessKey;
    @Value("${aliyun.secret.key}")
    private String secretKey;
    @Value("${task.mq.regionId}")
    private String endPointName;
    @Value("${task.mq.consumerId}")
    private String consumerId;
    @Value("${task.mq.domain}")
    private String domain;

    private String productName ="Ons";

    private IAcsClient iAcsClient;
    @PostConstruct
    private void init(){
        try {
            //http://onsaddr-internet.aliyun.com/rocketmq/nsaddr4client-internet
            DefaultProfile.addEndpoint(endPointName,regionId,productName,domain);
            IClientProfile profile= DefaultProfile.getProfile(regionId,accessKey,secretKey);
            iAcsClient = new DefaultAcsClient(profile);
        } catch (ClientException e) {
            logger.error("初始化iAcsClient异常，cause by:"+e.getMessage(),e);
        }
    }


    public OnsConsumerAccumulateResponse.Data getMqMessageData(String consumerId){
        OnsConsumerAccumulateResponse.Data data=null;
        try {
            OnsConsumerAccumulateRequest request = new OnsConsumerAccumulateRequest();
            request.setOnsRegionId(regionId);
            request.setPreventCache(System.currentTimeMillis());
            request.setAcceptFormat(FormatType.JSON);
            request.setDetail(true);
            request.setConsumerId(consumerId);
            OnsConsumerAccumulateResponse response=iAcsClient.getAcsResponse(request);
            data=response.getData();
        } catch (Exception e) {
            logger.error("获取mq监控信息失败，cause by:"+e.getMessage(),e);
        }
        return data;
    }

}
