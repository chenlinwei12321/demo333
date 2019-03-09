package com.shuwen.crawler.scheduler.web.monitor;


import cn.xhzy.monitor.dao.StatisticsResult;
import cn.xhzy.monitor.dao.TimeStatistics;
import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.common.monitor.LimitCondition;
import com.shuwen.crawler.common.monitor.CountCondition;
import com.shuwen.crawler.common.monitor.QueryCondition;
import com.shuwen.crawler.common.logtail.LogClientUtil;
import com.shuwen.crawler.rpc.GroupListService;
import com.shuwen.crawler.rpc.RemoteJobListService;
import com.shuwen.crawler.rpc.RemoteSiteListService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import com.shuwen.crawler.rpc.dto.GroupDTO;

/**
 * 监控日志统计接口
 *
 * @author lvjuwang
 * @create 2018-06-07 9:49
 **/

@RestController
@RequestMapping("crawler/log/statistic")

public class MonitorStatisticController {
    private static final Logger logger = LoggerFactory.getLogger(MonitorStatisticController.class);
    @Resource
    private LogClientUtil logClientUtil;

    @Resource
    private RemoteJobListService remoteJobService;
    @Resource
    private  GroupListService  groupListService;


    private String prepareLimitsSql(String category, String indicator, int order, int limit) {
        int dim = Integer.parseInt(category);
        int indt = Integer.parseInt(indicator);
        StringBuilder strBuilder = new StringBuilder();
        /**
         * 域名
         */
        if (1 == dim) {
            if (1 == indt) {
                strBuilder.append("httpStatus:200 | SELECT  domain,count(*)  AS  count GROUP  BY  domain ORDER  BY count");
            } else if (2 == indt) {
                strBuilder.append("* | select domain,count(*)  as count WHERE httpStatus<>200 group by domain order by count");
            } else if (3 == indt) {
                strBuilder.append("status:0 AND type:1 | select domain,count(*)  as count group by domain order by count");
            } else if (4 == indt) {
                strBuilder.append("status:1 AND type:1 | select domain,count(*)  as count group by domain order by count");
            }
        } else if (2 == dim) {
            if (1 == indt) {
                strBuilder.append("httpStatus:200 | select processor,count(*)  as count group by processor order by count");
            } else if (2 == indt) {
                strBuilder.append("* | select processor,count(*)  as count WHERE httpStatus<>200 group by processor order by count");
            } else if (3 == indt) {
                strBuilder.append("status:0 AND type:1 | select processor,count(*)  as count group by processor order by count");
            } else if (4 == indt) {
                strBuilder.append("status:1 AND type: 1 | select processor,count(*)  as count group by processor order by count");
            }
        } else if (3 == dim) {
            if (1 == indt) {
                strBuilder.append("httpStatus:200 | select jobCategory,count(*)  as count group by jobCategory order by count");
            } else if (2 == indt) {
                strBuilder.append("* | select jobCategory,count(*)  as count WHERE httpStatus<>200 group by jobCategory order by count");
            } else if (3 == indt) {
                strBuilder.append("status:0 AND type:1 | select jobCategory,count(*)  as count group by jobCategory order by count");
            } else if (4 == indt) {
                strBuilder.append("status:1 AND type:1| select jobCategory,count(*)  as count group by jobCategory order by count");
            }
        }

        if (0 == order) {
            strBuilder.append(" DESC");
        } else if (1 == 0) {
            strBuilder.append(" ASC");
        }
        strBuilder.append(" LIMIT ");
        strBuilder.append(limit);

        return strBuilder.toString();
    }

    private String prepareGroupBySql( String category,String keywords,String groupBy){
        int dim = Integer.parseInt(category);
        int gb  = Integer.parseInt(groupBy);
        StringBuilder strBuilder = new StringBuilder();
        if(1==dim){
            strBuilder.append("domain:");
        }else if(2==dim){
            strBuilder.append("processor:");
        }else if(3==dim){
            strBuilder.append("jobCategory:");
        }
        strBuilder.append(StringUtils.trim(keywords));

        if(1==gb){
            strBuilder.append(" | select httpStatus,COUNT(httpStatus) AS count WHERE httpStatus<>200 GROUP BY httpStatus order by count DESC");
        }


        return  strBuilder.toString();
    }

    private String prepareCountSql(String category, String indicator, String keywords) {

        int dim = Integer.parseInt(category);
        int indt = Integer.parseInt(indicator);
        StringBuilder strBuilder = new StringBuilder();
        if (1 == dim) {
            strBuilder.append("domain:");
        } else if (2 == dim) {
            strBuilder.append("processor:");
        } else if (3 == dim) {
            strBuilder.append("jobCategory:");
        }
        if (null == keywords || keywords.isEmpty()) {
            strBuilder.append("*");
        } else {
            strBuilder.append(StringUtils.trim(keywords));
        }

        if (1 == indt) {
            strBuilder.append(" AND httpStatus:200 | select count(*) as count");
        } else if (2 == indt) {
            strBuilder.append("| select count(*) as count where httpStatus<>200");
        } else if (3 == indt) {
            strBuilder.append(" AND status:0 AND type:1 | select count(*) as count");
        } else if (4 == indt) {
            strBuilder.append(" AND status:1 AND type:1 | select count(*) as count");
        }

        return strBuilder.toString();
    }


    @GetMapping("")
    public String CheckOk() {

        return "OK";
    }

    /**
     * 对某一维度的某个指标进行统计
     *
     * @param operator
     * @param search
     * @return
     */

    @PostMapping("limit")
    public UicResultDO getLimitQueryData(String operator, @RequestBody LimitCondition search) {

        String querySql = prepareLimitsSql(search.getCategory(), search.getIndicators(), Integer.parseInt(search.getOrder()), Integer.parseInt(search.getLimit()));

        try {
            List<JSONObject> result = logClientUtil.getRawQueryResult(querySql, 0, Integer.parseInt(search.getLimit()), search.getBeginTime(), search.getEndTime());
            /**
             * 转换jobCategory名称
             */
            if(3 == Integer.parseInt(search.getCategory())){
                Map<String,String>  groupMap = new HashMap<>();
                String groupId;
                String groupName;
                GroupDTO groupDTO;
                for(JSONObject jsonObject:result){
                    groupId   = jsonObject.getString("jobCategory");
                    if(null == groupId){
                        jsonObject.put("jobCategory", "无存储分组");
                        continue;
                    }
                    groupName = groupMap.get(groupId);
                    if(null == groupName){
                        groupDTO = groupListService.getGroupById(Long.parseLong(groupId));
                        if(null != groupDTO){
                            groupName = groupDTO.getName();
                            groupMap.put(groupId,groupName);
                        }
                    }
                    if(null != groupName) {
                        jsonObject.put("jobCategory", groupName);
                    }
                }

            }

            if (null != result) {
                return ResultGenerator.createGenerator(List.class)
                        .setData(result)
                        .success()
                        .getResultDO();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResultGenerator.createGenerator(List.class)
                    .setData(null)
                    .errorCode("获取数据失败:"+e.getMessage())
                    .getResultDO();
        }

        return ResultGenerator.createGenerator(List.class)
                .setData(null)
                .errorCode("获取数据失败")
                .getResultDO();

    }

    @PostMapping("count")
    public UicResultDO  getCountQueryData(String operator, @RequestBody CountCondition search) {
        StatisticsResult statisticsResult = new StatisticsResult();
        for (String indistr : search.getIndicators()) {
            int indi = Integer.parseInt(indistr);
            Long count;
            String querySql = prepareCountSql(search.getCategory(), indistr, search.getKeywords());
            try {
                count = logClientUtil.getQureyResultCount(querySql, search.getBeginTime(), search.getEndTime());
                if (1 == indi) {
                    statisticsResult.setCrawlerCount(count);
                } else if (2 == indi) {
                    statisticsResult.setFailedCount(count);
                } else if (3 == indi) {
                    statisticsResult.setSaveCount(count);
                } else if (4 == indi) {
                    statisticsResult.setParseFailedCount(count);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ResultGenerator.createGenerator(List.class)
                        .setData(null)
                        .errorCode("获取数据失败")
                        .getResultDO();
            }
        }

        return ResultGenerator.createGenerator(StatisticsResult.class)
                .setData(statisticsResult)
                .success()
                .getResultDO();
    }

    @PostMapping("groupby")
    public UicResultDO getQureyGroupData(String operator,@RequestBody QueryCondition search){

        String querySql = prepareGroupBySql(search.getCategory(), search.getKeywords(),search.getGroupBy());
        try {
            List<JSONObject> result = logClientUtil.getRawQueryResult(querySql, search.getOffset(), search.getCount(), search.getBeginTime(), search.getEndTime());
            if (null != result) {
                return ResultGenerator.createGenerator(List.class)
                        .setData(result)
                        .success()
                        .getResultDO();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResultGenerator.createGenerator(List.class)
                    .setData(null)
                    .errorCode("获取数据失败")
                    .getResultDO();
        }

        return ResultGenerator.createGenerator(List.class)
                .setData(null)
                .errorCode("获取数据失败")
                .getResultDO();
    }


    @PostMapping("lastday")
    public UicResultDO getLastdayStatistic(String operator){

        String downSucSql = "* | select  date_format(date_trunc('hour', __time__), '%m-%d %H:%i')  as time, count(1) as count  WHERE httpStatus=200 group by date_format(date_trunc('hour',__time__), '%m-%d %H:%i')   order BY time";
        String downFailSql = "* | select  date_format(date_trunc('hour', __time__), '%m-%d %H:%i')  as time, count(1) as count  WHERE httpStatus<>200 group by date_format(date_trunc('hour',__time__), '%m-%d %H:%i')   order BY time";
        String saveSucSql = "* | select  date_format(date_trunc('hour', __time__), '%m-%d %H:%i')  as time, count(1) as count  WHERE type=1 and status=0 group by date_format(date_trunc('hour',__time__), '%m-%d %H:%i')   order BY time";
        String saveFailSql = "* | select  date_format(date_trunc('hour', __time__), '%m-%d %H:%i')  as time, count(1) as count  WHERE type=1 and status=1 group by date_format(date_trunc('hour',__time__), '%m-%d %H:%i')   order BY time";
        long from = System.currentTimeMillis() - 60*60*24*1000;//查询最近24小时
        long to =   System.currentTimeMillis();

        TimeStatistics result= new TimeStatistics();
        List<JSONObject> statictis = null;

        try {

            statictis = logClientUtil.getRawQueryResult(downSucSql, 0, 24, from, to);
            if (null != result) {
                result.setCrawlerCount(statictis);
            }

            statictis = logClientUtil.getRawQueryResult(downFailSql, 0, 24, from, to);
            if (null != result) {
                result.setFailedCount(statictis);
            }

            statictis = logClientUtil.getRawQueryResult(saveSucSql, 0, 24, from, to);
            if (null != result) {
                result.setSaveCount(statictis);
            }

            statictis = logClientUtil.getRawQueryResult(saveFailSql, 0, 24, from, to);
            if (null != result) {
                result.setParseFailedCount(statictis);
            }

            return ResultGenerator.createGenerator(TimeStatistics.class)
                    .setData(result)
                    .success()
                    .getResultDO();

        }catch (Exception e){
            e.printStackTrace();
            return ResultGenerator.createGenerator(List.class)
                    .setData(null)
                    .errorCode("获取数据失败")
                    .getResultDO();
        }

    }


    @PostMapping("lastweek")
    public UicResultDO getLastweekStatistic(String operator){

        String downSucSql = "* | select  date_format(date_trunc('day', __time__), '%m-%d')  as time, count(1) as count  WHERE httpStatus=200 group by date_format(date_trunc('day',__time__), '%m-%d')   order BY time";
        String downFailSql = "* | select  date_format(date_trunc('day', __time__), '%m-%d')  as time, count(1) as count  WHERE httpStatus<>200 group by date_format(date_trunc('day',__time__), '%m-%d')   order BY time";
        String saveSucSql = "* | select  date_format(date_trunc('day', __time__), '%m-%d')  as time, count(1) as count  WHERE type=1 and status=0 group by date_format(date_trunc('day',__time__), '%m-%d')   order BY time";
        String saveFailSql = "* | select  date_format(date_trunc('day', __time__), '%m-%d')  as time, count(1) as count  WHERE type=1 and status=1 group by date_format(date_trunc('day',__time__), '%m-%d')   order BY time";
        long from = System.currentTimeMillis() - 60*60*24*1000*7;//查询最近7天
        long to =   System.currentTimeMillis();

        TimeStatistics result= new TimeStatistics();
        List<JSONObject> statictis = null;

        try {

            statictis = logClientUtil.getRawQueryResult(downSucSql, 0, 24, from, to);
            if (null != result) {
                result.setCrawlerCount(statictis);
            }

            statictis = logClientUtil.getRawQueryResult(downFailSql, 0, 24, from, to);
            if (null != result) {
                result.setFailedCount(statictis);
            }

            statictis = logClientUtil.getRawQueryResult(saveSucSql, 0, 24, from, to);
            if (null != result) {
                result.setSaveCount(statictis);
            }

            statictis = logClientUtil.getRawQueryResult(saveFailSql, 0, 24, from, to);
            if (null != result) {
                result.setParseFailedCount(statictis);
            }

            return ResultGenerator.createGenerator(TimeStatistics.class)
                    .setData(result)
                    .success()
                    .getResultDO();

        }catch (Exception e){
            e.printStackTrace();
            return ResultGenerator.createGenerator(List.class)
                    .setData(null)
                    .errorCode("获取数据失败")
                    .getResultDO();
        }

    }


    /*

    @GetMapping("/sites/{category}")
    public long getSiteCountByCategory(String operator, @PathVariable("category") String id) {
        return remoteSiteService.getSiteCountByStorage(Integer.parseInt(id));
    }

    @GetMapping("/modules/{category}")
    public long getModuleCountByCategory(String operator, @PathVariable("category") String id) {
        return remoteSiteService.getModuleCountByStorage(Integer.parseInt(id));
    }
    */

}
