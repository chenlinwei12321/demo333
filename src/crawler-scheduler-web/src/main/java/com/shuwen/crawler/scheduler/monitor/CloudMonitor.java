package com.shuwen.crawler.scheduler.monitor;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.cms.model.v20170301.QueryMetricListRequest;
import com.aliyuncs.cms.model.v20170301.QueryMetricListResponse;
import com.aliyuncs.cms.model.v20170301.QueryMetricLastRequest;
import com.aliyuncs.cms.model.v20170301.QueryMetricLastResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.shuwen.crawler.scheduler.monitor.mq.MqListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 云监控服务
 *
 * @author lvjuwang
 * @create 2018-06-21 16:56
 **/
@Service
public class CloudMonitor {
    private static final Logger logger = LoggerFactory.getLogger(CloudMonitor.class);

    @Value("${aliyun.ecs.regionId}")
    private String regionId;
    @Value("${aliyun.access.key}")
    private String accessKey;
    @Value("${aliyun.secret.key}")
    private String secretKey;

    private IAcsClient iAcsClient;

    @PostConstruct
    private void init(){
        try {
            IClientProfile profile = DefaultProfile.getProfile(regionId,accessKey,secretKey);
            iAcsClient = new DefaultAcsClient(profile);
        } catch (Exception e) {
            logger.error("初始化iAcsClient异常，cause by:"+e.getMessage(),e);
        }
    }

    public QueryMetricListResponse QueryEcsBasicMetricsList(String instanceId, String metrics,String period, String startTime, String endTime){

        QueryMetricListRequest request = new QueryMetricListRequest();
        request.setProject("acs_ecs_dashboard");
        request.setMetric(metrics);
        request.setPeriod(period);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        JSONObject dim = new JSONObject();
        dim.put("instanceId", instanceId);
        request.setDimensions(dim.toJSONString());
        request.setAcceptFormat(FormatType.JSON);
        try {
            QueryMetricListResponse response = iAcsClient.getAcsResponse(request);
            return  response;
        } catch (ServerException e) {
            e.printStackTrace();
            return null;
        } catch (ClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    public QueryMetricLastResponse QueryEcsBasicMetricsLast(String instanceId, String metrics,String period, String startTime, String endTime){

        QueryMetricLastRequest request = new QueryMetricLastRequest();
        request.setProject("acs_ecs_dashboard");
        request.setMetric(metrics);
        request.setPeriod(period);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        JSONObject dim = new JSONObject();
        dim.put("instanceId", instanceId);
        request.setDimensions(dim.toJSONString());
        request.setAcceptFormat(FormatType.JSON);
        try {
            QueryMetricLastResponse response = iAcsClient.getAcsResponse(request);
            return  response;
        } catch (ServerException e) {
            e.printStackTrace();
            return null;
        } catch (ClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    public QueryMetricListResponse QueryRedisBasicMetricsList(String instanceId, String metrics,String period, String startTime, String endTime){

        QueryMetricListRequest request = new QueryMetricListRequest();
        request.setProject("acs_kvstore");
        request.setMetric(metrics);
        request.setPeriod(period);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        JSONObject dim = new JSONObject();
        dim.put("instanceId", instanceId);
        request.setDimensions(dim.toJSONString());
        request.setAcceptFormat(FormatType.JSON);
        try {
            QueryMetricListResponse response = iAcsClient.getAcsResponse(request);
            return  response;
        } catch (ServerException e) {
            e.printStackTrace();
            return null;
        } catch (ClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    public QueryMetricLastResponse QueryRedisBasicMetricsLast(String instanceId, String metrics,String period, String startTime, String endTime){

        QueryMetricLastRequest request = new QueryMetricLastRequest();
        request.setProject("acs_kvstore");
        request.setMetric(metrics);
        request.setPeriod(period);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        JSONObject dim = new JSONObject();
        dim.put("instanceId", instanceId);
        request.setDimensions(dim.toJSONString());
        request.setAcceptFormat(FormatType.JSON);
        try {
            QueryMetricLastResponse response = iAcsClient.getAcsResponse(request);
            return  response;
        } catch (ServerException e) {
            e.printStackTrace();
            return null;
        } catch (ClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    public QueryMetricListResponse QueryRdsBasicMetricsList(String instanceId, String metrics,String period, String startTime, String endTime){

        QueryMetricListRequest request = new QueryMetricListRequest();
        request.setProject("acs_rds_dashboard");
        request.setMetric(metrics);
        request.setPeriod(period);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        JSONObject dim = new JSONObject();
        dim.put("instanceId", instanceId);
        request.setDimensions(dim.toJSONString());
        request.setAcceptFormat(FormatType.JSON);
        try {
            QueryMetricListResponse response = iAcsClient.getAcsResponse(request);
            return  response;
        } catch (ServerException e) {
            e.printStackTrace();
            return null;
        } catch (ClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    public QueryMetricLastResponse QueryRdsBasicMetricsLast(String instanceId, String metrics,String period, String startTime, String endTime){

        QueryMetricLastRequest request = new QueryMetricLastRequest();
        request.setProject("acs_rds_dashboard");
        request.setMetric(metrics);
        request.setPeriod(period);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        JSONObject dim = new JSONObject();
        dim.put("instanceId", instanceId);
        request.setDimensions(dim.toJSONString());
        request.setAcceptFormat(FormatType.JSON);
        try {
            QueryMetricLastResponse response = iAcsClient.getAcsResponse(request);
            return  response;
        } catch (ServerException e) {
            e.printStackTrace();
            return null;
        } catch (ClientException e) {
            e.printStackTrace();
            return null;
        }
    }
}
