package com.shuwen.crawler.scheduler.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shuwen.crawler.common.JobType;
import com.shuwen.crawler.common.LogOutputTemplate;
import com.shuwen.crawler.common.TaskExtraField;
import com.shuwen.crawler.common.job.pojo.JobDO;
import com.shuwen.crawler.common.monitor.FromType;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.SeedService;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.common.util.ExtrasUtils;
import com.shuwen.crawler.rpc.GroupListService;
import com.shuwen.crawler.rpc.dto.GroupDTO;
import com.shuwen.crawler.scheduler.job.JobScheduler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gbshine on 2018/5/8.
 */
@Service
public class TaskGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);
    private static final Logger JOB_START_TASK = LoggerFactory.getLogger("JOB_START_LOG");
    @Autowired
    private TaskServiceFactory taskService;

    @Autowired
    private SeedService seedService;

    @Autowired
    private GroupListService groupListService;

    ExecutorService executor = Executors.newFixedThreadPool(5);

    public void generateTaskFromJob(final JobDO job, String url) {
        if (job.getJobType().equals(JobType.OTS_TASK)) {
            executor.execute(() -> {
                try {
                    seedService.processAllSeeds(job.getId(), seed -> generateTask(job, seed.getUrl(), seed.getJobTag()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            generateTask(job, url, job.getTag());
        }
    }


    private void generateTask(JobDO job, String url, String tag) {
        try {
            TaskDO task = new TaskDO();
            task.setJobId(job.getId());
            task.setJobName(job.getName());
            task.setExpectedRunTime(job.getExpectedRunTime());
            task.setJobType(job.getJobType());
            task.setJobUrl(url);
            task.setJobTag(tag);
            Map<String, Object> taskExtras = JSON.parseObject(job.getJobExtra(), new TypeReference<Map<String, Object>>() {
            });
            task.setExtras(taskExtras);
            ExtrasUtils.initTaskExtras(task, job, tag);
            //job的category就是groupID
            GroupDTO groupDTO = groupListService.getGroupById(Long.valueOf(job.getCategory() + ""));
            this.convertGroup2TaskExtras(task, groupDTO);
            task.setJobCategory(job.getCategory());
            task.setTaskVersion(new DateTime().toString("yyyyMMddHHmmss"));
            if (job.getIsFull() != null && job.getIsFull() == 1) {//如果每次任务启动时候，全量爬取的话，则版本号为此次task版本号
                task.getExtras().put(TaskExtraField.VERSION_NO, task.getTaskVersion());
            }
            if(job.getJobType().equals(JobType.OTS_TASK)){
                task.setTaskFrom(FromType.ADVANCED.getValue());
            }else{
                task.setTaskFrom(FromType.CRAWLER.getValue());
            }
            //status,taskType,url,jobId,groupId,moduleName
            JOB_START_TASK.info(String.format(LogOutputTemplate.JOB_START_TEMPLATE,1,job.getName(),job.getJobType(),job.getId(),job.getCategory(),job.getModuleName()));
            if(job.getJobType()!=null&&job.getJobType().equals(JobType.OTS_TASK)){
                taskService.getOtsTaskService().addTask(task);//评论爬虫，只使用ots调度服务
            }else{
                taskService.addTask(task);//其他类型爬虫任务，根据设置，选择具体调度服务
            }
        } catch (Exception e) {
            LOG.error("errorProcessJob:" + job.getId(), e);
        }
    }


    /***
     * 需要的group信息追加到task的extras内
     * @param groupDTO
     * @param task
     */
    private void convertGroup2TaskExtras(TaskDO task, GroupDTO groupDTO) {
        if (groupDTO == null) {
            return;
        }
        Map<String, Object> extras = task.getExtras();
        extras.put(TaskExtraField.GROUP_ID, groupDTO.getId());
        extras.put(TaskExtraField.GROUP_OTS_TABLE, groupDTO.getOtsName());
        extras.put(TaskExtraField.GROUP_OTS_TABLE_KEYS, JSON.toJSONString(groupDTO.getKeys()));
        extras.put(TaskExtraField.GROUP_MQ_TAG, groupDTO.getMqTag());
    }
}

