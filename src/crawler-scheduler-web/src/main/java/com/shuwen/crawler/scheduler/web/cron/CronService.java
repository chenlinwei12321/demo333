package com.shuwen.crawler.scheduler.web.cron;

import cn.xhzy.crawler.basic.dao.mysql.CommonDao;
import com.shuwen.crawler.common.job.pojo.JobDO;
import com.shuwen.crawler.common.task.dao.ScheduledTaskDAO;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.rpc.RemoteJobListService;
import com.shuwen.crawler.rpc.RemoteModuleListService;
import com.shuwen.crawler.rpc.dao.DebugRecord;
import com.shuwen.crawler.rpc.dao.ModuleDO;
import com.shuwen.crawler.rpc.util.RdsTableUtils;
import com.shuwen.crawler.scheduler.util.ModuleCodeCheckUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class CronService {

    private static final Logger SCHEDULER_INFO_LOG = LoggerFactory.getLogger("MODULE_CODE_ERROR_LOG");
    private static final Logger LOGGER = LoggerFactory.getLogger(CronService.class);

    private static final SimpleDateFormat simpleDateFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");

    @Resource
    private CommonDao<DebugRecord> debugRecordCommonDao;
    @Resource
    private RemoteModuleListService remoteModuleListService;

    @Resource
    private RemoteJobListService remoteJobListService;

    @Resource
    private TaskServiceFactory taskServiceFactory;

    @Autowired
    private ScheduledTaskDAO scheduledTaskDAO;

    /***
     * 每天10：15自动清除在线调试表一天前的数据
     */
    @Scheduled(cron = "0 15 10 * * ?")
    public void rdsDebugDataClear(){
        Date yesterday = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        debugRecordCommonDao.removeByCriteria(RdsTableUtils.ONLINE_DEBUG_RECORDS_TABLE, debugRecordCommonDao.createCriteria().lessThan("gmtCreate",yesterday));
    }

    /***
     * 每天10：00自动自动校验一遍热部署的脚本,启动时候也校验一下
     */
    @Scheduled(cron = "0 00 10 * * ?")
    public void groovyCodeCheck(){
        List<ModuleDO> moduleList = remoteModuleListService.getPubModuleList("");
        if(moduleList==null||moduleList.size()==0){
            return;
        }
        for(int x=0;x<moduleList.size();x++){
            ModuleDO moduleDO = moduleList.get(x);
            if(moduleDO.getIsConfig()==null||moduleDO.getIsConfig()!=1){
                try {
                    ModuleCodeCheckUtil.groovyCodeCheck(moduleDO.getCode(),moduleDO.getName());
                }catch (Exception e){
                    SCHEDULER_INFO_LOG.info(moduleDO.getName()+" "+moduleDO.getIsConfig()+" "+moduleDO.getOperator()+" "+simpleDateFormat.format(moduleDO.getGmtModified()));
                }
            }
        }

    }

    /***
     * 每天11：00自动清除历史redis的task
     */
    @Scheduled(cron = "0 00 11 * * ?")
    public void deleteHistoryTask(){
        List<JobDO> jobDOS = remoteJobListService.loadPauseJob();
        int date=7;//暂停7天的任务，redis的task自动清除
        for(int x=0;x<jobDOS.size();x++){
            JobDO jobDO = jobDOS.get(x);
            if(System.currentTimeMillis()-jobDO.getGmtModified().getTime()>1000*60*60*24*date){
                int num = taskServiceFactory.deleteTaskByJobdId(jobDO.getId());
                scheduledTaskDAO.deleteTaskByJobId(jobDO.getId());
                if(num!=0){
                    LOGGER.info("自动清除暂停"+date+"天，【"+jobDO.getName()+"】的历史"+num+"条task！");
                }
            }
        }
    }

}
