package com.shuwen.crawler.scheduler.task;

import com.alibaba.fastjson.JSON;
import com.shuwen.crawler.common.CrawlerConstants;
import com.shuwen.crawler.common.JobType;
import com.shuwen.crawler.common.LogOutputTemplate;
import com.shuwen.crawler.common.MqTagConstants;
import com.shuwen.crawler.common.jedis.utils.JedisUtil;
import com.shuwen.crawler.common.job.pojo.JobDO;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.TaskService;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
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
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.*;

@Service
public class TaskJedisScheduler {

    private static final Logger logger=LoggerFactory.getLogger(TaskJedisScheduler.class);

    private static final Logger LOG = LoggerFactory.getLogger(TaskJedisScheduler.class);

    private static final Logger SCHEDULER_INFO_LOG = LoggerFactory.getLogger("SCHEDULER_INFO_LOG");
    private static final Logger SCHEDULER_TASK_LOG = LoggerFactory.getLogger("SCHEDULER_TASK_LOG");

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskService taskJedisService;

    @Autowired
    private MQProducer mQProducer;

    @Resource
    private MqDataBufferService mqDataBufferService;

    private Thread thread;

    private Thread sThread;

    private Integer num = 50;

    @Value("${current.scheduler.task}")
    private Integer currentSchedulerTaskServiceType;

    @Resource
    JedisUtil jedisUtil;

    @PostConstruct
    public void schedule() {
        if(TaskServiceFactory.TaskServiceType.REDIS.getValue()!=currentSchedulerTaskServiceType) {
            return;
        }
        thread = new AbstractInterrupteThread("TaskJedisScheduler@"+TaskJedisScheduler.class.getClassLoader().hashCode()) {
            @Override
            public void doWork() {
                //创建一个10个任务的线程池
                ExecutorService taskExecutor = Executors.newFixedThreadPool(20);
                long rds_query_start = System.currentTimeMillis();
                List<JobDO> jobList = jobService.getAllJobOrderByPriorityNotPause();
                long rds_query_end = System.currentTimeMillis();
                // 2.遍历job列表，查询任务表
                for (JobDO job : jobList){
                    if(!existTask(job)){
                        continue;
                    }
                    taskExecutor.execute(() -> {
                        try {
                            schedule(job);
                        } catch (Exception e) {
                            LOG.error("taskExecutor execute error,"+e.getMessage());
                        }
                    });
                }

                try {
                    taskExecutor.shutdown();
//					taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                } catch (Exception e) {
                    LOG.error("InterruptedException error,"+e.getMessage());
                }
                long rds_end = System.currentTimeMillis();
                SCHEDULER_INFO_LOG.info("joblist size:"+jobList.size()+"\tschedule one time,query:"+(rds_query_end-rds_query_start)+"ms\texecute:"+(rds_end-rds_query_start)+"ms");
            }
        };
        thread.start();

    }

    /***
     * 判断redis 是否包含了这个待调度的任务
     * @param job
     * @return
     */
    private boolean existTask(JobDO job){
        String key = CrawlerConstants.JREDIS_CRAWLER_TASK_START_KEY + job.getId();
        Jedis jedis=null;
        try {
            jedis= jedisUtil.getJedis();
            return jedis.exists(key);
        }catch (Exception e){
            logger.error("exists key error,cause by:"+e.getMessage(),e);
            return false;
        }finally {
            jedisUtil.returnResource(jedis);
        }
    }

    private void schedule(JobDO job) {
        Integer friquency = job.getFriquency();
        if (friquency == null) {
            friquency = num;
        }
        if (isAdvancedJob(job)) {
            if (mqDataBufferService.isAdvancedTaskCanScheduler()) {
                schedule(job, friquency);
            }else{
                this.onLater(job);
            }
        } else {
            if (mqDataBufferService.isTaskCanScheduler()) {
                schedule(job, friquency);
            }else{
                this.onLater(job);
            }
        }
    }

    private void onLater(JobDO job){
        SCHEDULER_TASK_LOG.info(String.format(LogOutputTemplate.SCHEDULER_TASK_TEMPLATE,0,job.getName(),job.getJobType(),job.getId(),job.getCategory(),job.getModuleName(),0));
    }

    private boolean isAdvancedJob(JobDO job) {
        if (job.getJobType() != null && job.getJobType().equals(JobType.OTS_TASK)) {
            return true;
        } else if (job.getTag() != null && (job.getTag().equals(MqTagConstants.TAG_SEED_JSON_API) || job.getTag().equals(MqTagConstants.TAG_SEED_COMMON))) {
            return true;
        }
        return false;
    }


    private void schedule(JobDO jobDO, int num) {
        List<TaskDO> taskList;
        long db_start = System.currentTimeMillis();
        try {
            taskList = taskJedisService.getTaskByJobId(jobDO.getId(), num,true);//获取完直接删除
        } catch (Exception e) {
            LOG.error(Long.toString(jobDO.getId()), e);
            return;
        }
        long db_end = System.currentTimeMillis();
        int cnt = 0;
        StringBuilder sb=new StringBuilder();
        long mq_start = System.currentTimeMillis();
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
        long mq_end = System.currentTimeMillis();
        if(cnt!=0){
            //真正有调度记录的
            SCHEDULER_TASK_LOG.info(String.format(LogOutputTemplate.SCHEDULER_TASK_TEMPLATE,1,jobDO.getName(),jobDO.getJobType(),jobDO.getId(),jobDO.getCategory(),jobDO.getModuleName(),cnt));
        }
        SCHEDULER_INFO_LOG.info(jobDO.getId() + ":" + cnt+"\tredis messages:"+sb.toString()+"\tdb:"+(db_end-db_start)+"ms\tmq:"+(mq_end-mq_start)+"ms");
    }

    @PreDestroy
    public void close() {
        thread.interrupt();
        sThread.interrupt();
    }
}
