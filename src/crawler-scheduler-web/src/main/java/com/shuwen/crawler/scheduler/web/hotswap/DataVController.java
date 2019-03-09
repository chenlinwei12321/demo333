package com.shuwen.crawler.scheduler.web.hotswap;


import cn.xhzy.crawler.basic.dao.mysql.CommonDao;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.cms.model.v20170301.QueryMetricLastResponse;
import com.aliyuncs.ons.model.v20170918.OnsConsumerAccumulateResponse;
import com.shuwen.crawler.common.monitor.ServiceResourceDO;
import com.shuwen.crawler.common.util.CrawlerException;
import com.shuwen.crawler.common.util.NumberUtils;
import com.shuwen.crawler.common.util.TimeUtils;
import com.shuwen.crawler.rpc.GroupListService;
import com.shuwen.crawler.rpc.VersionListService;
import com.shuwen.crawler.rpc.dto.DataVDto;
import com.shuwen.crawler.scheduler.monitor.CloudMonitor;
import com.shuwen.crawler.scheduler.util.MqDataBufferService;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("crawler/datav")
public class DataVController {

    @Resource
    private VersionListService versionListService;
    @Resource
    private GroupListService groupListService;
    @Resource
    private CloudMonitor cloudMonitor;
    @Resource
    private MqDataBufferService mqDataBufferService;
    @Resource
    private CommonDao<ServiceResourceDO> resourceDao;

    private final String resourceTableName = "monitor_resource";

    public static Map<String,String> param=new HashMap<>();

    private static Long BEFORSAVE= 0L;

    private static Long BEFORMSG= 0L;

    private static String REQUESTURL="";

    public void initParam(){
        //download失败top
        param.put("downloadError", "{\"datasets\":[{\"name\":\"a\",\"id\":\"29715\",\"dims\":[{\"key\":\"状态\",\"value\":\"fail\",\"type\":\"STATIC\"},{\"key\":\"爬虫名称\",\"value\":\"\",\"type\":\"ALL\"}]}],\"measures\":[{\"measureExpression\":\"a.COUNT__line\",\"measureLabel\":\"下载失败数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":\"下载失败数\",\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" + System.currentTimeMillis() + "}");
        //爬虫落库
        param.put("save", "{\"datasets\":[{\"name\":\"a\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"saved\",\"type\":\"STATIC\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"ALL\"}]}],\"measures\":[{\"measureExpression\":\"a.COUNT__line\",\"measureLabel\":\"\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":\"\",\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" + System.currentTimeMillis() + "}");
        //爬虫落库统计（成功/失败）
        param.put("saveTotal","{\"datasets\":[{\"name\":\"a\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"saved\",\"type\":\"STATIC\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"b\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"error\",\"type\":\"STATIC\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"DISABLED\"}]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"落库成功数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"落库失败数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":\"落库成功数\",\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //每分钟爬虫下载、落库、分发数据
        param.put("monitor", "{\"datasets\":[{\"name\":\"a\",\"id\":\"14611\",\"dims\":[{\"key\":\"状态\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"域名\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"http状态码\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"b\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"c\",\"id\":\"29660\",\"optionalDims\":[{\"key\":\"status\",\"value\":\"\",\"type\":\"DISABLED\"}],\"requiredDims\":[]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"下载总数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"落库总数\"},{\"measureExpression\":\" c.COUNT__line \",\"measureLabel\":\"消息总数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":null,\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":60,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //下载、落库、分发统计
        param.put("total", "{\"datasets\":[{\"name\":\"a\",\"id\":\"29660\",\"optionalDims\":[{\"key\":\"status\",\"value\":\"\",\"type\":\"DISABLED\"}],\"requiredDims\":[]},{\"name\":\"b\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"c\",\"id\":\"14611\",\"dims\":[{\"key\":\"状态\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"域名\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"http状态码\",\"value\":\"\",\"type\":\"DISABLED\"}]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"消息总数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"落库总数\"},{\"measureExpression\":\" c.COUNT__line \",\"measureLabel\":\"下载总数\"}],\"isRealtime\":true,\"secAgg\":\"SUM\",\"orderByKey\":null,\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //按照机器统计
        param.put("host", "{\"datasets\":[{\"name\":\"b\",\"id\":\"29675\",\"dims\":[{\"key\":\"服务器IP\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"c\",\"id\":\"29674\",\"dims\":[{\"key\":\"服务器ip\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"a\",\"id\":\"29676\",\"dims\":[{\"key\":\"hostip\",\"value\":\"\",\"type\":\"ALL\"},{\"key\":\"moduleName\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"groupId\",\"value\":\"\",\"type\":\"DISABLED\"}]}],\"measures\":[{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"下载数\"},{\"measureExpression\":\"c.COUNT__line\",\"measureLabel\":\"落库数\"},{\"measureExpression\":\"a.COUNT__line\",\"measureLabel\":\"消息数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":null,\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //每个爬虫当前
        param.put("everyCrawler", "{\"datasets\":[{\"name\":\"a\",\"id\":\"29749\",\"dims\":[{\"key\":\"状态\",\"value\":\"success\",\"type\":\"STATIC\"},{\"key\":\"爬虫名称\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"b\",\"id\":\"29692\",\"dims\":[{\"key\":\"爬虫名称\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"c\",\"id\":\"29693\",\"dims\":[{\"key\":\"moduleName\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"d\",\"id\":\"29725\",\"dims\":[{\"key\":\"爬虫名称\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"e\",\"id\":\"29749\",\"dims\":[{\"key\":\"状态\",\"value\":\"fail\",\"type\":\"STATIC\"},{\"key\":\"爬虫名称\",\"value\":\"\",\"type\":\"ALL\"}]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"下载成功数\"},{\"measureExpression\":\" e.COUNT__line \",\"measureLabel\":\"下载失败数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"落库数\"},{\"measureExpression\":\" c.COUNT__line \",\"measureLabel\":\"消息数\"},{\"measureExpression\":\" d.COUNT__line \",\"measureLabel\":\"异常数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":null,\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //业务分组分发数据
        param.put("fenfa", "{\"datasets\":[{\"name\":\"a\",\"id\":\"29705\",\"dims\":[{\"key\":\"分组ID\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"b\",\"id\":\"29704\",\"dims\":[{\"key\":\"分组ID\",\"value\":\"\",\"type\":\"ALL\"}]},{\"name\":\"c\",\"id\":\"29706\",\"dims\":[{\"key\":\"groupId\",\"value\":\"\",\"type\":\"ALL\"}]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"下载数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"落库数\"},{\"measureExpression\":\" c.COUNT__line \",\"measureLabel\":\"消息数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":\"下载数\",\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //下载 统计量
        param.put("downTotal","{\"datasets\":[{\"name\":\"a\",\"id\":\"14611\",\"dims\":[{\"key\":\"状态\",\"value\":\"success\",\"type\":\"STATIC\"},{\"key\":\"域名\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"http状态码\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"b\",\"id\":\"14611\",\"dims\":[{\"key\":\"状态\",\"value\":\"fail\",\"type\":\"STATIC\"},{\"key\":\"域名\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"http状态码\",\"value\":\"\",\"type\":\"DISABLED\"}]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"下载成功数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"下载失败数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":null,\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //下载成功top
        param.put("successTop","{\"datasets\":[{\"name\":\"a\",\"id\":\"29715\",\"dims\":[{\"key\":\"状态\",\"value\":\"success\",\"type\":\"STATIC\"},{\"key\":\"爬虫名称\",\"value\":\"\",\"type\":\"ALL\"}]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"下载成功数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":\"下载成功数\",\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //请求url
        REQUESTURL="https://arms.console.aliyun.com/shareapi/query.json?action=OlapAction&eventSubmitDoQueryMultiDsTileData=1";
        //获取前一天下载、落库、分发统计
        param.put("sql", "{\"datasets\":[{\"name\":\"a\",\"id\":\"29660\",\"optionalDims\":[{\"key\":\"status\",\"value\":\"\",\"type\":\"DISABLED\"}],\"requiredDims\":[]},{\"name\":\"b\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"c\",\"id\":\"14611\",\"dims\":[{\"key\":\"状态\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"域名\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"http状态码\",\"value\":\"\",\"type\":\"DISABLED\"}]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"消息总数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"落库总数\"},{\"measureExpression\":\" c.COUNT__line \",\"measureLabel\":\"下载总数\"}],\"isRealtime\":true,\"secAgg\":\"SUM\",\"orderByKey\":null,\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getBefore().getString("start") + ",\"maxTime\":" + TimeUtils.getBefore().getString("end") + "}");
        //后去落库来源 自建、评论、客户端
        param.put("saveSource","{\"datasets\":[{\"name\":\"a\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"saved\",\"type\":\"STATIC\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"ALL\"}]}],\"measures\":[{\"measureExpression\":\"a.COUNT__line\",\"measureLabel\":\"\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":\"\",\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":2147483647,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");
        //每个时间段爬虫运行情况
        param.put("everyTime","{\"datasets\":[{\"name\":\"a\",\"id\":\"14611\",\"dims\":[{\"key\":\"状态\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"域名\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"http状态码\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"b\",\"id\":\"14625\",\"dims\":[{\"key\":\"_line_gen_3_gen_0\",\"value\":\"\",\"type\":\"DISABLED\"},{\"key\":\"来源类型\",\"value\":\"\",\"type\":\"DISABLED\"}]},{\"name\":\"c\",\"id\":\"29660\",\"optionalDims\":[{\"key\":\"status\",\"value\":\"\",\"type\":\"DISABLED\"}],\"requiredDims\":[]}],\"measures\":[{\"measureExpression\":\" a.COUNT__line \",\"measureLabel\":\"下载总数\"},{\"measureExpression\":\" b.COUNT__line \",\"measureLabel\":\"落库总数\"},{\"measureExpression\":\" c.COUNT__line \",\"measureLabel\":\"消息总数\"}],\"isRealtime\":false,\"secAgg\":\"SUM\",\"orderByKey\":null,\"limit\":10,\"reduceTail\":false,\"dataType\":\"TASK\",\"intervalInSec\":60,\"minTime\":" + TimeUtils.getStartTime() + ",\"maxTime\":" +  System.currentTimeMillis() + "}");

    }

    @GetMapping("everyTime")
    public String getTime(){
        JSONArray array=new JSONArray();
        try{
            JSONArray ss =getResults("everyTime");
            for(int i=0;i<ss.size();i++){
                JSONObject str= ss.getJSONObject(i);
                String time=TimeUtils.transformatTime("HH:mm:ss",str.getLong("date"));
                array.add(new JSONObject().fluentPut("x",time).fluentPut("y",str.getLongValue("落库总数")).fluentPut("s","1"));
                array.add(new JSONObject().fluentPut("x",time).fluentPut("y",str.getLongValue("下载总数")).fluentPut("s","2"));
                array.add(new JSONObject().fluentPut("x",time).fluentPut("y",str.getLongValue("消息总数")).fluentPut("s","3"));
            }
        }catch (Exception e){
            throw new CrawlerException("everyTime！！");
        }
        return array.toJSONString();
    }


    @GetMapping("ecs")
    public String getEcs(){
        initParam();
        JSONArray array=new JSONArray();
        long timeNow  = System.currentTimeMillis();
        String endTime = TimeUtils.transformatTime(timeNow);
        String beginTime =  TimeUtils.transformatTime(timeNow-5*60*1000);
        try {
            CommonDao.Criteria criteria = resourceDao.createCriteria();
            criteria.eq("project", "acs_ecs_dashboard").eq("delete", 0);
            List<ServiceResourceDO> resourceList = resourceDao.selectByCriteria(resourceTableName, criteria, ServiceResourceDO.class);
            for(ServiceResourceDO servive: resourceList){
                JSONObject data=new JSONObject();
                QueryMetricLastResponse response =  cloudMonitor.QueryEcsBasicMetricsLast(servive.getInstanceId().replace("\t\r\n",""),"memory_usedutilization","60",beginTime,endTime);
                data.put("x",servive.getIp().replaceAll("\t\r\n",""));
                data.put("y",JSONObject.parseArray(response.getDatapoints()).getJSONObject(0).getString("Maximum"));
                data.put("s","内存");
                JSONObject data1=new JSONObject();
                QueryMetricLastResponse response1 =  cloudMonitor.QueryEcsBasicMetricsLast(servive.getInstanceId().replace("\t\r\n",""),"diskusage_utilization","60",beginTime,endTime);
                data1.put("x",servive.getIp().replaceAll("\t\r\n",""));
                data1.put("y",JSONObject.parseArray(response1.getDatapoints()).getJSONObject(0).getString("Maximum"));
                data1.put("s","磁盘");
                array.add(data);
                array.add(data1);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return array.toJSONString();
    }



    @GetMapping("mq")
    public String getMq(){
        initParam();
        OnsConsumerAccumulateResponse.Data mqMessageData1 =mqDataBufferService.getTaskMqData();
        OnsConsumerAccumulateResponse.Data mqMessageData2 =mqDataBufferService.getVideoMqData();
        OnsConsumerAccumulateResponse.Data mqMessageData3 =mqDataBufferService.getDebugMqData();
        OnsConsumerAccumulateResponse.Data mqMessageData4 =mqDataBufferService.getAdvancedMqData();
        JSONArray array=new JSONArray();
        array.add(new JSONObject().fluentPut("1","task").fluentPut("2",mqMessageData1.getTotalDiff()));
        array.add(new JSONObject().fluentPut("1","debug").fluentPut("2",mqMessageData3.getTotalDiff()));
        array.add(new JSONObject().fluentPut("1","advance").fluentPut("2",mqMessageData4.getTotalDiff()));
        array.add(new JSONObject().fluentPut("1","oss").fluentPut("2",mqMessageData2.getTotalDiff()));
        return array.toJSONString();
    }


    @GetMapping("huoyue")
    public String getHuoyue(){
        JSONArray array=new JSONArray();
        JSONObject data=new JSONObject();
        int count=0;
        try{
            JSONArray ss =getResults("everyCrawler");
            for(int i=0;i<ss.size();i++){
                JSONObject str= ss.getJSONObject(i);
                if(str.getInteger("下载成功数")>0){
                    count++;
                }
            }
            data.put("value1",count);
            data.put("name1","正常");
            data.put("value","success");

            if(count<400){
                data.put("name1","异常");
                data.put("value","error");
            }
            array.add(data);
        }catch (Exception e){
            throw new CrawlerException("获取huoyue失败！！");
        }
        return array.toJSONString();
    }




    @GetMapping("gundong/{type}")
    public String getFenFa(@PathVariable String type){
        JSONArray array=new JSONArray();
        try{
            JSONArray ss =getResults(type);
            for(int i=0;i<ss.size();i++){
                JSONObject str= ss.getJSONObject(i);
                str.remove("date");
                str.remove("下载失败数");
                str.remove("异常数");
                array.add(str);
            }
        }catch (Exception e){
            throw new CrawlerException("获取gundong失败！！");
        }
        return array.toJSONString();
    }


    @GetMapping("download")
    public String getDown(){
        JSONArray array=new JSONArray();
        try{
            JSONArray ss =getResults("downTotal");
            for(int i=0;i<ss.size();i++){
                JSONObject str= ss.getJSONObject(i);
                JSONObject data=new JSONObject();
                array.add(new JSONObject().fluentPut("type","成功").fluentPut("value",str.getLong("下载成功数")) );
                array.add(new JSONObject().fluentPut("type","失败").fluentPut("value",str.getLong("下载失败数")) );
            }
        }catch (Exception e){
            throw new CrawlerException("获取fenfa失败！！");
        }
        return array.toJSONString();

    }

    @GetMapping("save")
    public String getSave(){
        JSONArray array=new JSONArray();
        try{
            JSONArray ss =getResults("saveTotal");
            for(int i=0;i<ss.size();i++){
                JSONObject str= ss.getJSONObject(i);
                JSONObject data=new JSONObject();
                array.add(new JSONObject().fluentPut("type","成功").fluentPut("value",str.getLong("落库成功数")));
                array.add(new JSONObject().fluentPut("type","失败").fluentPut("value",str.getLong("落库失败数")) );
            }
        }catch (Exception e){
            throw new CrawlerException("获取fenfa失败！！");
        }
        return array.toJSONString();

    }
    @GetMapping("fenfa")
    public String getFenFa(){
        JSONArray array=new JSONArray();
        try{
            JSONArray ss =getResults("fenfa");
            for(int i=0;i<ss.size();i++){
                JSONObject str= ss.getJSONObject(i);
                JSONObject data=new JSONObject();
                data.put("value",str.getString("消息数"));
                data.put("content","【"+groupListService.getGroupById(str.getLong("分组ID")).getName()+"】--实时发送数据【"+str.getString("消息数")+"】条！");
                array.add(data);
            }
        }catch (Exception e){
            throw new CrawlerException("获取fenfa失败！！");
        }
        return array.toJSONString();

    }

    /**
     * 爬虫来源获取
     * @return
     */
    @GetMapping("source")
    public String getSoucre(){
        JSONArray array=new JSONArray();
        try{
            JSONArray ss =getResults("saveSource");
            for(int i=0;i<ss.size();i++){
                JSONObject str= ss.getJSONObject(i);
                JSONObject data=new JSONObject();
                data.put("x",str.getString("来源类型"));
                data.put("y",str.getLongValue("a.COUNT__line"));
                array.add(data);
            }
        }catch (Exception e){
            throw new CrawlerException("获取saveSource失败！！");
        }
        return array.toJSONString();
    }


    /**
     *  得到大屏单个数字统计
     * @return
     */
    @GetMapping("total")
    public String getTotal(){
        initParam();
        JSONArray array=new JSONArray();
        JSONObject result=new JSONObject();
        DataVDto datav=versionListService.getDataV("下载总数");
        try{
            if (datav.getNum() ==null || datav.getTotalNum()==null){ //执行入库操作
                JSONObject all=getResult("sql");
                long downNum=all.getLongValue("下载总数");
                long msgNum=all.getLongValue("消息总数");
                DataVDto down=new DataVDto();
                down.setDate(TimeUtils.getBefore().getLongValue("start"));
                down.setNum(downNum);
                down.setType("下载总数");
                versionListService.saveDataV(down);
                DataVDto msg=new DataVDto();
                msg.setDate(TimeUtils.getBefore().getLongValue("start"));
                msg.setNum(msgNum);
                msg.setType("消息总数");
                versionListService.saveDataV(msg);
            }
            BEFORSAVE=versionListService.getDataV("下载总数").getTotalNum();//获取昨天以前的数据
            BEFORMSG=versionListService.getDataV("消息总数").getTotalNum();//获取昨天以前的数据
            JSONObject total=getResult("total");
            result.fluentPut("download",total.getString("下载总数"));
            result.fluentPut("save",total.getString("落库总数"));
            result.fluentPut("msg",total.getString("消息总数"));
            JSONObject saveTotal=getResult("saveTotal");
            result.fluentPut("saveError",saveTotal.getString("落库失败数"));
            result.fluentPut("saveSuccess",saveTotal.getString("落库成功数"));
            JSONObject downloadTotal=getResult("downTotal");
            result.fluentPut("downError",downloadTotal.getString("下载失败数"));
            result.fluentPut("downSuccess",downloadTotal.getString("下载成功数"));
            result.fluentPut("allDown", NumberUtils.amountConversion(total.getLongValue("下载总数")+ BEFORSAVE));
            result.fluentPut("allMsg", NumberUtils.amountConversion(total.getLongValue("消息总数")+BEFORMSG));
            result.fluentPut("monitor","万象爬虫平台目前下载数据--【"+total.getString("下载总数")+"】条！落库--【"+total.getString("落库总数")+"】条！对外分发--【"+total.getString("消息总数")+"】条！下载总量--【"+result.getString("allDown")+"】条！对外分发总数--【"+result.getString("allMsg")+"】条！");
            array.add(result);
        }catch (Exception e){
            throw new CrawlerException("获取arms数据失败！"+e);
        }
        return array.toJSONString();
    }

    public JSONArray getResults(String key){
        initParam();
        JSONArray ss=new JSONArray();
        try {
            String data = send(REQUESTURL,"utf-8", param.get(key));
            JSONObject obj= JSON.parseObject(data);
            ss=obj.getJSONObject("data").getJSONArray("data");
            return ss;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getResult(String key){
        initParam();
        JSONObject ss=new JSONObject();
        try {
            String data = send(REQUESTURL,"utf-8", param.get(key));
            JSONObject obj= JSON.parseObject(data);
            ss=obj.getJSONObject("data").getJSONArray("data").getJSONObject(0);
            return ss;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取大屏数据
     * @param url
     * @param encoding
     * @param param
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public static String send(String url,String encoding,String param) throws ParseException, IOException {
        String body = "";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nvs = new ArrayList<NameValuePair>();
        nvs.add(new BasicNameValuePair("queryParams", param));
        NameValuePair[] values = nvs.toArray(new NameValuePair[] {});
        httpPost.setEntity(new UrlEncodedFormEntity(nvs, encoding));
        httpPost.addHeader("Host","arms.console.aliyun.com");
        httpPost.addHeader("Accept","application/json, text/plain, */*");
        httpPost.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:59.0) Gecko/20100101 Firefox/59.0");
        httpPost.addHeader("Referer","https://arms.console.aliyun.com/share/?login_arms_t3h_token=fGNxFUPQrfMwJ96qCsXH9LdnXFPYyLUYwXuvO06D0yO+8TyWKfA0hud07qYQpPvtNYQ0KbjWvDLIFFnv3CXQRi7sTH8dEogmp2KPDbhR6OpK8e814h2O4ju2M+Y+BM4cq3L8oHBGMjw7r13F2oFe+Tz55Hh64dsUdYCFKooc9zaPWpEAegZ3pVB4GZ4kr2MJLcWPH10cUFku7Ex/HRKIJqdijw24UejqmAzWt8cNoyw2OFVja5vc2fH9afDAfBAMLMNBeRBpguGaxSHlQGKPZkMWgHEBmms+dHu/ZyW472Lp0c+II3piIqa5avWIpWzK1BzhhcGr8hhcGbuW1t8EnAjtZeJKSAlFO8bjYmqvAeF1gIUqihz3Npd2poci7bQDbr/6cFMmx+s4auIV37/SQD58i6fy89JwO5g2t0Rrv2wYG6PTQABJNZ71tzgDBqDuCY0Zk/LumwQva9B5VixeJX+TNTwa6IuGsvuvUE+DlSwWnN1EbKZhs1ncz9qEyRIJUrasYnt2uQAz/pPpp8gbsvyxQc8D+vVlP8kbeDJypWQ01Fuqv+i/QsXGynl3qO3Q8EA2DN3XLqLrNdEMNQdcLvQDd8ddTVZGN7iQ57a/2c8=");
        httpPost.addHeader("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
        httpPost.addHeader("X-XSRF-TOKEN","iyd2HCzEfP4K2hQxLndwMC");
        httpPost.addHeader("Cookie","cnz=DUBcE3HAK24CAaS0zW/ma+xt; cna=k3BbE2eLq18CAWp4dI4cMQlc; isg=BFZWQ1bjEa2MqiRQoiEvJ5-apAqYX5l4gQ6pD8C5uzubg-UdKYKhQao1H9_KMJJJ; _ga=GA1.2.1015637574.1523929789; aliyun_site=CN; UM_distinctid=162d1567a46767-029e82a6adda3b8-495861-13c680-162d1567a48112c; aliyun_choice=CN; consoleRecentVisit=arms%2Cons%2Cots%2Coss; dauth_proxy_token_name=login_aliyunid_ticket%2Cmini_login_aliyunid_ticket; dauth_proxy_encoding=UTF-8; CLOSE_HELP_GUIDE=true; aliyun_lang=zh; channel=Mq5V5W6Hfv31rD1HvPAxwCRdYIocuB1VpyRSuWzP0HE%3D; currentRegionId=cn-hangzhou; _uab_collina=153535537296229511054828; _bl_uid=v8j54ldkbpdygvyya5nqzUseapeC; a_l=aliyun; a_d=.aliyun.com; aone_user_ticket=\\\"YmYxYjMxMTgtZDIyZi00MDc5LThiNjMtYmFkNmYxMDU4NmQyV2VkIEF1ZyAyOSAxODowOTozOCBDU1QgMjAxOA==\\\"; a_u_t=5DED9A1B759F1BD2CB37600BBE072A3E; aone_user_uuid=bf1b3118-d22f-4079-8b63-bad6f10586d2; arms_user_parent_id=1182390081402711; XSRF-TOKEN=XMemmMH39UuSwsvwcV1qb1; arms_selected_region=%22cn-hangzhou%22; consoleNavVersion=1.4.125; ping_test=true; t=6dfc78c17f6301b26abddbd96f3fa5d8; _tb_token_=3b5aef3551f6; cookie2=1f9af94837ab1fdef867770db28fa5bd;  _HOME_JSESSIONID=E1666S91-5XZX6WR2PCYJTX1JHI7Y1-13TACHLJ-67C4; _home_session0=AtIYsOFhqW6gAH3w3KyIN3VpaG2B0J%2BBCFHFVUPTKOlglZ9EITHhkwymhdZrbmd9eHwvBOPdcgqqWXoNfTqkqxaNDLWLDhURXqCiRPc39behS9HL6ReFpcW6nAq7p0QgzFGr4IxEwAWV9l9JEULFcw2dyMfBm0rc9KAOWY4pKFuCkFDrNTO2N5oNND3QtNw0TDIrjCQPMF28bSEYmO0oUS%2BJZO9W3Bl4j1x2cTCl0MkgPmX3FdiPeO6HfDljnIZnAx4xLdpMQH23ZQ1pvkDjyWesOrb5MjAvx8XbaWA%2FBCrPrm1LV2ihrH%2B0OG3qeg%2F6; JSESSIONID=9O866O81-W4AYVR9QTE1AC8MP1T4Y1-D7H3MLLJ-WS4; ");
        httpPost.addHeader("Connection","keep-alive");
        CloseableHttpResponse response = client.execute(httpPost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            body = EntityUtils.toString(entity, encoding);
        }
        EntityUtils.consume(entity);
        response.close();
        return body;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(send(REQUESTURL,"utf-8", param.get("downTotal")));
    }

}
