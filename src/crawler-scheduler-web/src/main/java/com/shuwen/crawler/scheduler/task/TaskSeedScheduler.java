package com.shuwen.crawler.scheduler.task;

import com.alibaba.fastjson.JSON;
import com.shuwen.crawler.common.job.pojo.JobDO;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.TaskService;
import com.shuwen.crawler.common.util.ExtrasUtils;
import com.shuwen.crawler.core.job.service.JobService;
import com.shuwen.crawler.core.mq.MQProducer;
import com.shuwen.crawler.scheduler.util.AbstractInterrupteThread;
import com.shuwen.crawler.scheduler.util.MqDataBufferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
/**
 * 评论爬虫调度
 */
public class TaskSeedScheduler{
    private static final Logger LOG = LoggerFactory.getLogger(TaskScheduler.class);

    private static final Logger SCHEDULER_INFO_LOG = LoggerFactory.getLogger("SCHEDULER_INFO_LOG");
    private static final Logger SCHEDULER_TASK_LOG = LoggerFactory.getLogger("SCHEDULER_TASK_LOG");

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MQProducer mQProducer;

    private Thread thread;

    private Integer num = 50;

    @Value("${current.scheduler.task}")
    private Integer currentSchedulerTaskServiceType;

    @Resource
    private MqDataBufferService mqDataBufferService;

    ExecutorService taskExecutor = new ThreadPoolExecutor(8, 20, 3, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(500));

    @PostConstruct
    public void schedule() {
        thread = new AbstractInterrupteThread("TaskScheduler@" + TaskScheduler.class.getClassLoader().hashCode()) {
            @Override
            public void doWork() {
                List<JobDO> jobList = jobService.getAllSeedJobOrderByPriorityNotPause();
                // 2.遍历job列表，查询任务表
                for (JobDO job : jobList) {
//                    taskExecutor.execute(() -> {
//                        try {
                            schedule(job);
//                        } catch (Exception e) {
//                            LOG.error("schedule execute error," + e.getMessage());
//                        }
//                    });
                }
            }
        };
        thread.start();

    }

    private void schedule(JobDO job) {
        Integer friquency = job.getFriquency();
        if (friquency == null) {
            friquency = num;
        }
        if (mqDataBufferService.isAdvancedTaskCanScheduler()) {
            schedule(job, friquency);
        }
    }

    private void schedule(JobDO jobDO, int num) {
        List<TaskDO> taskList;
        try {
            taskList = taskService.getTaskByJobId(jobDO.getId(), num,true);
        } catch (Exception e) {
            LOG.error(Long.toString(jobDO.getId()), e);
            return;
        }
        int cnt = 0;
        StringBuilder sb=new StringBuilder();
        for (TaskDO task : taskList) {
            ExtrasUtils.ipPool2Task(task,jobDO);
            task.setJobCategory(jobDO.getCategory());
            try {
                String messageId = mQProducer.send(JSON.toJSONString(task), null, task.getJobTag());
                sb.append(messageId).append(",");
                cnt++;
            } catch (Exception e) {
                LOG.error(JSON.toJSONString(task), e);
            }
        }
        SCHEDULER_INFO_LOG.info(jobDO.getId() + ":" + cnt+"\tots messages:"+sb.toString());
    }

    @PreDestroy
    public void close() {
        thread.interrupt();
    }

}
