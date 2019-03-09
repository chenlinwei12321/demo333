package com.shuwen.crawler.scheduler.task;

import com.alibaba.fastjson.JSON;
import com.shuwen.crawler.common.JobType;
import com.shuwen.crawler.common.MqTagConstants;
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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

/**
 * @author: chenkang
 * @email: chenkang@shuwen.com
 * @date: 2017年6月7日 下午2:46:04
 */
@Service
public class TaskScheduler {

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

	private Thread sThread;

	private Integer num = 50;

	@Value("${current.scheduler.task}")
	private Integer currentSchedulerTaskServiceType;

	@Resource
	private MqDataBufferService mqDataBufferService;

	@PostConstruct
	public void schedule() {

		if(TaskServiceFactory.TaskServiceType.OTS.getValue()!=currentSchedulerTaskServiceType) {
			return;
		}

		thread = new AbstractInterrupteThread("TaskScheduler@" + TaskScheduler.class.getClassLoader().hashCode()) {
			@Override
			public void doWork() {
				List<JobDO> jobList = jobService.getAllJobOrderByPriorityNotPause();
				// 2.遍历job列表，查询任务表
				for (JobDO job : jobList) {
					schedule(job);
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
		if (isAdvancedJob(job)) {
			if (mqDataBufferService.isAdvancedTaskCanScheduler()) {
				schedule(job, friquency);
			}
		} else {
			if (mqDataBufferService.isTaskCanScheduler()) {
				schedule(job, friquency);
			}
		}
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
		try {
			taskList = taskService.getTaskByJobId(jobDO.getId(), num);
//            taskService.batchDelete(taskList);
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
		try {
			int size = taskList.size();
			if(size>200){//OTS批量删除只支持200条以内的？
				List<TaskDO> temp=new ArrayList<>();
				for(int x=0;x<taskList.size();x++){
					TaskDO taskDO = taskList.get(x);
					if(x%200!=0){
						temp.add(taskDO);
					}else if(x%200==0){
						taskService.batchDelete(temp);
						temp=new ArrayList<>();
						temp.add(taskDO);
					}
				}
				taskService.batchDelete(temp);//最后不足200条的数据
			}else{
				taskService.batchDelete(taskList);
			}
		} catch (Exception e) {
			LOG.error("批量删除失败，cause by："+e.getMessage());
		}
		SCHEDULER_INFO_LOG.info(jobDO.getId() + ":" + cnt+"\tots messages:"+sb.toString());
	}

	private ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 10, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(1000), new AbortPolicy());

	@PreDestroy
	public void close() {
		thread.interrupt();
		sThread.interrupt();
		executor.shutdownNow();
	}
}

