package com.shuwen.crawler.scheduler.web.monitor;

import cn.xhzy.crawler.basic.dao.mysql.CommonDao;
import cn.xhzy.monitor.MonitorLogger;
import cn.xhzy.monitor.MonitorRecordService;
import cn.xhzy.monitor.dao.DataRecordQuery;
import cn.xhzy.monitor.dao.StatisticsResult;
import cn.xhzy.monitor.model.MonitorRecordDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alicloud.openservices.tablestore.SyncClient;
import com.aliyuncs.cms.model.v20170301.QueryMetricLastResponse;
import com.aliyuncs.ons.model.v20170918.OnsConsumerAccumulateResponse;
import com.shuwen.crawler.common.CrawlerConstants;
import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.common.jedis.ScheduledTaskJedisUtil;
import com.shuwen.crawler.common.jedis.utils.JedisUtil;
import com.shuwen.crawler.common.logtail.AdvancedLogClientUtil;
import com.shuwen.crawler.common.logtail.LogClientUtil;
import com.shuwen.crawler.common.monitor.ServiceResourceDO;
import com.shuwen.crawler.common.ots.TaskJedisUtil;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.rpc.RemoteJobListService;
import com.shuwen.crawler.rpc.dto.JobDTO;
import com.shuwen.crawler.scheduler.util.MqDataBufferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import com.shuwen.crawler.common.util.HttpClientsUtils;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import com.shuwen.crawler.scheduler.monitor.CloudMonitor;


@RestController
@RequestMapping("monitor")
public class MonitorRecordController {

    private static final Logger logger= LoggerFactory.getLogger(MonitorRecordController.class);

    private static final String strTimeFormat = ("yyyy-MM-dd HH:mm:ss");
    private static final String strPeriod   = "60";

    @Resource
    private MonitorRecordService monitorRecordService;

    @Resource
    private RemoteJobListService remoteJobListService;

    @Resource
    private MqDataBufferService mqDataBufferService;

    @Resource
    private JedisUtil jedisUtil;

    @Resource
    private SyncClient syncClient;

    @Autowired
    private TaskServiceFactory taskService;

    @Resource
    private MonitorLogger monitorLogger;
    @Resource
    private LogClientUtil logClientUtil;
    @Resource
    private AdvancedLogClientUtil advancedLogClientUtil;

    @Resource
    private CloudMonitor  cloudMonitor;

    @Resource
    private CommonDao<ServiceResourceDO> resourceDao;

    private final String resourceTableName = "monitor_resource";

    @Value("${aliyun.logtail.isUse}")
    private boolean isLogService;

    HttpClientsUtils  httpClientsUtils = new HttpClientsUtils();

    /**
     * 查询所有关键字抓取记录
     * @param operator
     * @param query
     * @return
     */
    @GetMapping("selectAll")
    public Object selectAllRecord(String operator,
                                  @RequestParam(defaultValue = "1") Integer pageNo,
                                  @RequestParam(defaultValue = "20") Integer pageRow,
                                  DataRecordQuery query){
//        List<MonitorRecordDTO> results=new ArrayList<>();
        PageResult<MonitorRecordDTO> pageResult = null;
        if(isLogService){
            pageResult=monitorLogger.queryRecordList(logClientUtil,advancedLogClientUtil,query, pageNo, pageRow);
        }else{
            pageResult = monitorRecordService.queryRecordList(query, pageNo, pageRow);
        }
        for(MonitorRecordDTO monitor: pageResult.getRows()){
            MonitorRecordDTO record = null;
            if(!isLogService){
                record= monitorRecordService.getRecordById(monitor.getId());
            }else{
                record= monitorLogger.getRecordById(logClientUtil,advancedLogClientUtil,monitor.getId());
            }
            monitor.setResult(record.getResult());
//            if(record!=null){
//                results.add(record);
//            }
        }
        return ResultGenerator.createGenerator().success().setData(pageResult).getResultDO();
    }


    @GetMapping("records")
    public Object listRecords(String operator,
                              @RequestParam(defaultValue = "1") Integer pageNo,
                              @RequestParam(defaultValue = "20") Integer pageRow,
                              DataRecordQuery query){

        PageResult<MonitorRecordDTO> pageResult = null;
        if(isLogService){
            pageResult=monitorLogger.queryRecordList(logClientUtil,advancedLogClientUtil,query, --pageNo, pageRow);
        }else{
            pageResult = monitorRecordService.queryRecordList(query, pageNo, pageRow);
        }
        return ResultGenerator.createGenerator().success().setData(pageResult).getResultDO();
    }

    @GetMapping("records/{id}")
    public Object getRecord(String operator, @PathVariable("id") String id){
        MonitorRecordDTO record = null;
        if(!isLogService){
            record= monitorRecordService.getRecordById(id);
        }else{
            record= monitorLogger.getRecordById(logClientUtil,advancedLogClientUtil,id);
        }
        return ResultGenerator.createGenerator(MonitorRecordDTO.class).success().setData(record).getResultDO();
    }

    @GetMapping("records/info")
    public Object statistics(String operator, String domain, String startTime, String endTime){
        StatisticsResult statistics = null;
        if(!isLogService){
            statistics= monitorRecordService.statistics(domain, startTime, endTime);
        }else{
            statistics= monitorLogger.statistics(logClientUtil,advancedLogClientUtil,domain, startTime, endTime);
        }
        return ResultGenerator.createGenerator(StatisticsResult.class).success().setData(statistics).getResultDO();
    }

    @GetMapping("records/mq")
    public Object mq(){
        OnsConsumerAccumulateResponse.Data mqMessageData1 =mqDataBufferService.getTaskMqData();
        OnsConsumerAccumulateResponse.Data mqMessageData2 =mqDataBufferService.getVideoMqData();
        OnsConsumerAccumulateResponse.Data mqMessageData3 =mqDataBufferService.getDebugMqData();
        OnsConsumerAccumulateResponse.Data mqMessageData4 =mqDataBufferService.getAdvancedMqData();
        List<OnsConsumerAccumulateResponse.Data> result=new ArrayList<>();
        result.add(mqMessageData1);
        result.add(mqMessageData4);
        result.add(mqMessageData3);
        result.add(mqMessageData2);
        return ResultGenerator.createGenerator(List.class).success().setData(convert2Map(result)).getResultDO();
    }

    @GetMapping("resource/ecs")
    public Object getEcsResource(){
        try {
            CommonDao.Criteria criteria = resourceDao.createCriteria();
            criteria.eq("project", "acs_ecs_dashboard").eq("delete", 0);
            List<ServiceResourceDO> resourceList = resourceDao.selectByCriteria(resourceTableName, criteria, ServiceResourceDO.class);
            return ResultGenerator.createGenerator(List.class).success().setData(resourceList).getResultDO();
        }catch (Exception e){
            e.printStackTrace();
            return ResultGenerator.createGenerator(null).errorCode("获取ECS机器资源失败").setData(null).getResultDO();
        }
    }

    @GetMapping("resource/redis")
    public Object getRedisResource(){
        try {
            CommonDao.Criteria criteria = resourceDao.createCriteria();
            criteria.eq("project", "acs_kvstore").eq("delete", 0);
            List<ServiceResourceDO> resourceList = resourceDao.selectByCriteria(resourceTableName, criteria, ServiceResourceDO.class);
            return ResultGenerator.createGenerator(List.class).success().setData(resourceList).getResultDO();
        }catch (Exception e){
            e.printStackTrace();
            return ResultGenerator.createGenerator(null).errorCode("获取Redis资源失败").setData(null).getResultDO();
        }
    }

    @GetMapping("resource/rds")
    public Object getRdsResource(){
        try {
            CommonDao.Criteria criteria = resourceDao.createCriteria();
            criteria.eq("project", "acs_rds_dashboard").eq("delete", 0);
            List<ServiceResourceDO> resourceList = resourceDao.selectByCriteria(resourceTableName, criteria, ServiceResourceDO.class);
            return ResultGenerator.createGenerator(List.class).success().setData(resourceList).getResultDO();
        }catch (Exception e){
            e.printStackTrace();
            return ResultGenerator.createGenerator(null).errorCode("获取mysql资源失败").setData(null).getResultDO();
        }
    }
    //@GetMapping("check/worker")
    public UicResultDO checkWorkerAlive(String operator,String ip){
        if(null==ip || ip.isEmpty()){
            return ResultGenerator.createGenerator().errorCode("IP参数为空").setData(null).getResultDO();
        }
        String checkUrl = "";
        if(-1 == ip.indexOf("http://")){
            checkUrl="http://";
        }
        checkUrl = checkUrl+ (ip + "/ok");
        Map<String,Object> resultData = httpClientsUtils.httpGet(checkUrl,null);
        if(null != resultData){
            int statusCode = (int)resultData.get("RESPONSE_STATUS");
            String  htmlSource = (String)resultData.get("RESPONSE_SOURCE");

            if( 200 == statusCode  && "ok".equals(htmlSource)) {
                return ResultGenerator.createGenerator(String.class).success().setData(htmlSource).getResultDO();
            }else {
                return ResultGenerator.createGenerator(String.class).errorCode(new String(""+statusCode)).setData(htmlSource).getResultDO();
            }
        }else {
            return ResultGenerator.createGenerator().errorCode("连接服务器失败").setData(null).getResultDO();
        }
    }

    @GetMapping("/ecs/list/{instanceid}/{metrics}")
    public Object queryMetricListResponse(String operator,@PathVariable("instanceid") String instanceid,@PathVariable("metrics") String metrics,@RequestParam("beginTime") String beginTime,@RequestParam("endTime") String endTime){
        if(null == instanceid || instanceid.isEmpty()){
            return null;
        }

        SimpleDateFormat  timeFormater = new SimpleDateFormat(strTimeFormat);
        return cloudMonitor.QueryEcsBasicMetricsList(instanceid, metrics, strPeriod, timeFormater.format(Long.parseLong(beginTime)), timeFormater.format(Long.parseLong(endTime)));
    }

    @PostMapping("/ecs/last/{instanceid}")
    public Object queryMetricsLastResponse(String operator,@PathVariable("instanceid") String instanceid,String ip,String name,@RequestBody List<JSONObject> metrics){
       if(null == instanceid || instanceid.isEmpty()){
            return null;
        }

        SimpleDateFormat  timeFormater = new SimpleDateFormat(strTimeFormat);

        long timeNow  = System.currentTimeMillis();
        String endTime = timeFormater.format(timeNow);
        String beginTime = timeFormater.format(timeNow-5*60*1000);

        JSONObject results = new JSONObject();

        String    mname;
        String    mvalue;
        QueryMetricLastResponse  response;
        for (JSONObject jsonObject:metrics) {
            mname = jsonObject.getString("name");
            mvalue = jsonObject.getString("value");

            if("connectStat".equals(mname)){
                UicResultDO resultDO = checkWorkerAlive(operator,ip);
                if(null != resultDO){
                   results.put(mname,resultDO.isSuccess());
                }else {
                    results.put(mname,false);
                }
                continue;
            }

            response = cloudMonitor.QueryEcsBasicMetricsLast(instanceid, mvalue, strPeriod, beginTime, endTime);
            if(null!=response){
                JSONArray  dataPt = JSON.parseArray(response.getDatapoints());
                if(!dataPt.isEmpty()) {
                    results.put(mname, dataPt.get(0));
                }
            }
        }
        results.put("ip",ip);
        results.put("name",name);

        return results;
    }

    @GetMapping("/redis/list/{instanceid}/{metrics}")
    public Object queryRedisMetricListResponse(String operator,@PathVariable("instanceid") String instanceid,@PathVariable("metrics") String metrics,@RequestParam("beginTime") String beginTime,@RequestParam("endTime") String endTime){

        if(null == instanceid || instanceid.isEmpty()){
            return null;
        }
        SimpleDateFormat  timeFormater = new SimpleDateFormat(strTimeFormat);
        return cloudMonitor.QueryRedisBasicMetricsList(instanceid, metrics, strPeriod, timeFormater.format(Long.parseLong(beginTime)), timeFormater.format(Long.parseLong(endTime)));
    }

    @PostMapping("/redis/last/{instanceid}")
    public Object queryRedisMetricsLastResponse(String operator,@PathVariable("instanceid") String instanceid,@RequestBody List<JSONObject> metrics){
        if(null == instanceid || instanceid.isEmpty()){
            return null;
        }

        long timeNow  = System.currentTimeMillis();
        SimpleDateFormat  timeFormater = new SimpleDateFormat(strTimeFormat);

        String endTime   = timeFormater.format(timeNow);
        String beginTime = timeFormater.format(timeNow-5*60*1000);

        JSONObject results = new JSONObject();

        String    mname;
        String    mvalue;
        QueryMetricLastResponse  response;
        for (JSONObject jsonObject:metrics) {
            mname = jsonObject.getString("name");
            mvalue = jsonObject.getString("value");

            response = cloudMonitor.QueryRedisBasicMetricsLast(instanceid, mvalue, strPeriod, beginTime, endTime);
            if(null!=response){
                JSONArray  dataPt = JSON.parseArray(response.getDatapoints());
                if(!dataPt.isEmpty()) {
                    results.put(mname, dataPt.get(0));
                }
            }
        }

        return results;
    }

    @GetMapping("/rds/list/{instanceid}/{metrics}")
    public Object queryRdsMetricListResponse(String operator,@PathVariable("instanceid") String instanceid,@PathVariable("metrics") String metrics,@RequestParam("beginTime") String beginTime,@RequestParam("endTime") String endTime){
        if(null == instanceid || instanceid.isEmpty()){
            return null;
        }

        SimpleDateFormat  timeFormater = new SimpleDateFormat(strTimeFormat);
        return cloudMonitor.QueryRdsBasicMetricsList(instanceid, metrics, "300", timeFormater.format(Long.parseLong(beginTime)), timeFormater.format(Long.parseLong(endTime)));
    }

    @PostMapping("/rds/last/{instanceid}")
    public Object queryRdsMetricsLastResponse(String operator,@PathVariable("instanceid") String instanceid,@RequestBody List<JSONObject> metrics){
        if(null == instanceid || instanceid.isEmpty()){
            return null;
        }

        long timeNow  = System.currentTimeMillis();
        SimpleDateFormat  timeFormater = new SimpleDateFormat(strTimeFormat);

        String endTime = timeFormater.format(timeNow);
        String beginTime = timeFormater.format(timeNow-10*60*1000);

        JSONObject results = new JSONObject();

        String    mname;
        String    mvalue;
        QueryMetricLastResponse  response;
        for (JSONObject jsonObject:metrics) {
            mname = jsonObject.getString("name");
            mvalue = jsonObject.getString("value");

            response = cloudMonitor.QueryRdsBasicMetricsLast(instanceid, mvalue, "300", beginTime, endTime);
            if(null!=response){
                JSONArray  dataPt = JSON.parseArray(response.getDatapoints());
                if(!dataPt.isEmpty()) {
                    results.put(mname, dataPt.get(0));
                }
            }
        }

        return results;
    }


    @GetMapping("records/currentmq")
    public Object currentMq(){
        OnsConsumerAccumulateResponse.Data mqMessageData1 =mqDataBufferService.getCurrentTaskMqData();
        List<OnsConsumerAccumulateResponse.Data> result=new ArrayList<>();
        result.add(mqMessageData1);
        return ResultGenerator.createGenerator(List.class).success().setData(convert2Map(result)).getResultDO();
    }

    @GetMapping("records/config")
    public Object config(HttpServletRequest request){
        Map<String,Object> configMap=new HashMap<>();
        configMap.put("scheduler_type",taskService.getCurrentService().getValue());
        return ResultGenerator.createGenerator(Map.class).success().setData(configMap).getResultDO();
    }



    private List<Map<String,Object>> convert2Map(List<OnsConsumerAccumulateResponse.Data> result){
        List<Map<String,Object>> resultList=new ArrayList<>();
        for(OnsConsumerAccumulateResponse.Data data:result){
            Map<String,Object> map=new HashMap<>();
            if(data==null){
                continue;
            }
            map.put("consumeTps",data.getConsumeTps());
            map.put("delayTime",data.getDelayTime());
            map.put("lastTimestamp",data.getLastTimestamp());
            map.put("online",data.getOnline());
            map.put("totalDiff",data.getTotalDiff());

            List<OnsConsumerAccumulateResponse.Data.DetailInTopicDo> detailInTopicList = data.getDetailInTopicList();

            for(OnsConsumerAccumulateResponse.Data.DetailInTopicDo temp:detailInTopicList){
                String topic = temp.getTopic();
                if(topic.contains("%RETRY%")){
                    map.put("cid",topic.replace("%RETRY%",""));
                }else{
                    map.put("topic",topic);
                }
            }

            resultList.add(map);
        }
        return resultList;
    }


    @GetMapping("records/jedis")
    public Object jedis(String operator,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<JobDTO> allJobs = remoteJobListService.getAllJobs(operator, queryString,1,10000);
        List<JobDTO> rows = allJobs.getRows();
        List<JSONObject> result=new ArrayList<>();
        for(JobDTO row:rows){
            Long size = getTaskSizeLengthByJobId(row.getId());
            if(size==0){
               continue;
            }
            if(row.getIsPause()==1||row.getDeleted()){
                continue;
            }
            JSONObject jsonObject= (JSONObject) JSON.toJSON(row);
            jsonObject.put("size",size);
            result.add(jsonObject);
        }
        UicResultDO<List> resultDO = ResultGenerator.createGenerator(List.class).success().setData(result).getResultDO();
        JSONObject jsonObject= (JSONObject) JSONObject.toJSON(resultDO);
        jsonObject.put("total",result.size());
        return jsonObject;
    }

    @GetMapping("records/dump2File")
    public void dump2File(String operator){
        PageResult<JobDTO> allJobs = remoteJobListService.getAllJobs(operator,"{}",1,10000);
        List<JobDTO> rows = allJobs.getRows();
        File file = new File(System.currentTimeMillis() + ".task");
        StringBuffer sb=new StringBuffer();
        for(JobDTO row:rows){
            if(row.getIsPause()==1){
                continue;
            }
            long start = System.currentTimeMillis();
            Long size = getTaskSizeLengthByJobId(row.getId());
            long end = System.currentTimeMillis();
            StringBuffer line=new StringBuffer();
            sb.append("jobId:").append(row.getId()).append("\tjobName:").append(row.getName()).append("\tsize:").append(size).append("\tquerytimes:").append((end-start)+"ms").append("\r\n");
            line.append("jobId:").append(row.getId()).append("\tjobName:").append(row.getName()).append("\tsize:").append(size).append("\tquerytimes:").append((end-start)+"ms").append("\r\n");
            System.out.println(line.toString());
        }

        logger.info(sb.toString());

    }

    @GetMapping("records/joddelete/{jobId}")
    public Object deleteByJobId(@PathVariable("jobId")Long jobId){
        int deleteTotal = taskService.deleteTaskByJobdId(jobId);
        JSONObject jsonObject= new JSONObject();
        jsonObject.put("total",deleteTotal);
        jsonObject.put("jobId",jobId);
        return jsonObject;
    }

    @GetMapping("records/jobTaskdelete/{jobId}")
    public void jobTaskdelete(@PathVariable("jobId")Long jobId){
        ScheduledTaskJedisUtil.deleteTaskByJobId(jedisUtil,jobId);
    }

    @GetMapping("records/jobTasklist")
    public List<JSONObject> jobTasklist(String operator){

        PageResult<JobDTO> allJobs = remoteJobListService.getAllJobs(operator,"{}",1,10000);
        List<JobDTO> rows = allJobs.getRows();
        List<JSONObject> result=new ArrayList<>();
        for(JobDTO row:rows){
            Long size = ScheduledTaskJedisUtil.jobTasklist(jedisUtil, row.getId());
            if(size==null||size==0){
                continue;
            }
            JSONObject temp=new JSONObject();
            temp.put("jobId",row.getId());
            temp.put("jobName",row.getName());
            temp.put("size",size);
            result.add(temp);
        }
        return result;
    }


    private Long getTaskSizeLengthByJobId(Long jobId){
        Map<Long, Long> jobTaskSize = taskService.getJobTaskSize(jobId);
        return jobTaskSize.get(jobId);
    }

    @GetMapping("records/jedisInfo/{jobId}")
    public Object jedisByJobId( @PathVariable("jobId")Long jobId,@RequestParam(defaultValue = "1") Integer pageNo,
                                @RequestParam(defaultValue = "10") Integer pageRow){
        Jedis jedis = jedisUtil.getJedis();
        if(null == jedis){
            throw new NullPointerException("Jedis is Null");
        }

        String key = CrawlerConstants.JREDIS_CRAWLER_TASK_START_KEY + jobId;
        int start = 0;
        int end = 0;
        if(1 == pageNo){
            start=0;
        }else{
            start = (pageNo - 1)*pageRow;
        }
        end = start + pageRow;
        List<TaskDO> result = new ArrayList<>();

        try {
            List<String> lrange = jedis.lrange(key, 0, 9);

            for (String line : lrange) {
                TaskDO taskDO = TaskJedisUtil.string2Task(line);
                result.add(taskDO);
            }
        }catch(Exception e){
            logger.error(e.getMessage(), e);
        }finally{
            jedisUtil.returnResource(jedis);
        }

        UicResultDO<List> resultDO = ResultGenerator.createGenerator(List.class).success().setData(result).getResultDO();
        JSONObject jsonObject= (JSONObject) JSONObject.toJSON(resultDO);
        jsonObject.put("total",result.size());

        return jsonObject;
    }

}