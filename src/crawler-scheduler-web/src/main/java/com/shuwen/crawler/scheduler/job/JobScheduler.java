package com.shuwen.crawler.scheduler.job;

import com.shuwen.crawler.common.JobType;
import com.shuwen.crawler.common.LogOutputTemplate;
import com.shuwen.crawler.common.job.pojo.JobDO;
import com.shuwen.crawler.common.task.dao.ScheduledTaskDAO;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.core.job.service.JobService;
import com.shuwen.crawler.rpc.impl.RemoteJobServiceImpl;
import com.shuwen.crawler.scheduler.task.TaskGenerator;
import com.shuwen.crawler.scheduler.util.AbstractInterrupteThread;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


@Service
public class JobScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);
	private static final Logger SCHEDULER_INFO_LOG = LoggerFactory.getLogger("SCHEDULER_INFO_LOG");
	private static final Logger JOB_START_TASK = LoggerFactory.getLogger("JOB_START_LOG");
	private static final Date MAX_DATE = Date
			.from(LocalDate.of(9999, 12, 31).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	private static final SimpleDateFormat simpleDateFormat=new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");
	@Autowired
	private JobService jobService;

    @Autowired
    private TaskGenerator taskGenerator;

    @Autowired
    private TaskServiceFactory taskService;

    @Autowired
	private RemoteJobServiceImpl remoteJobService;

    @Autowired
	private ScheduledTaskDAO scheduledTaskDAO;

	private Thread thread;

	@Value("${job.start.task.num}")
	private int JOB_START_TASK_NUM;

	@PostConstruct
	public void schedule() {
		thread = new AbstractInterrupteThread("JobScheduler") {
			@Override
			public void doWork() {
				List<JobDO> jobList = null;
				try {
					jobList = jobService.getReadyJobList();
				} catch (Exception e) {
					LOG.error("errorGetReadyJob", e);
				}
				if (jobList != null && jobList.size() > 0) {
					for (JobDO job : jobList) {
						refreshJobIpPool(job);
						long jobId = job.getId();
						try {

							String cronExp = job.getCronExpression();

							List<TaskDO> taskByJobId = null;

							if(job.getJobType()!=null&&job.getJobType().equals(JobType.OTS_TASK)){
								taskByJobId = taskService.getOtsTaskService().getTaskByJobId(jobId, JOB_START_TASK_NUM);
							}else{
								taskByJobId = taskService.getTaskByJobId(jobId, JOB_START_TASK_NUM);
							}

							if(taskByJobId!=null&&taskByJobId.size()>=JOB_START_TASK_NUM){//如果task表存在历史调度任务超过阈值，则不启动此次任务调度。并将调度时间调整为下个调度时间！
								Date next=null;
								if (StringUtils.isNotBlank(cronExp)) {
									CronSequenceGenerator sequenceGenerator = new CronSequenceGenerator(cronExp);
									next= sequenceGenerator.next(new Date());
									jobService.updateJobExpectedRunTime(jobId, next);
								}else{
									next=MAX_DATE;
									jobService.updateJobExpectedRunTime(jobId, MAX_DATE);
								}
								Date expectedRunTime=null;
								TaskDO taskDO = taskByJobId.get(0);
								try{
									expectedRunTime= taskDO.getExpectedRunTime();
								}catch (Exception e){
									SCHEDULER_INFO_LOG.warn("获取task调度信息异常，cause by:"+e.getMessage());
								}
								SCHEDULER_INFO_LOG.warn("job存在的任务数超过了阈值！阈值："+JOB_START_TASK_NUM+"\t任务ID："+jobId+"\t任务名称："+job.getName()+"\t下次执行时间："+simpleDateFormat.format(next)+"\tfirstTaskExpectTime:"+simpleDateFormat.format(expectedRunTime)+"\ttaskVersion:"+taskDO.getTaskVersion()+"\ttastCreateTime:"+taskDO.getTaskCreateTime());
								JOB_START_TASK.info(String.format(LogOutputTemplate.JOB_START_TEMPLATE,0,job.getName(),job.getJobType(),job.getId(),job.getCategory(),job.getModuleName()));
								continue;
							}else{
								scheduledTaskDAO.deleteTaskByJobId(job.getId());//启动任务的时候，redis把历史的task清除掉
							}

							taskGenerator.generateTaskFromJob(job, job.getJobUrl());

							if (StringUtils.isNotBlank(cronExp)) {
								try {
									CronSequenceGenerator sequenceGenerator = new CronSequenceGenerator(cronExp);
									jobService.updateJobExpectedRunTime(jobId, sequenceGenerator.next(new Date()));
								}catch (IllegalArgumentException e){
//									LOG.error("cron表达式有误");
									continue;
								}
							} else {
								jobService.updateJobExpectedRunTime(jobId, MAX_DATE);
							}
						} catch (Exception e) {
							LOG.error("errorProcessJob:" + jobId, e);
						}
					}
				}
			}
		};

		thread.start();
	}

	/**
	 * 后台更新任务绑定的代理ip
	 */
	private void refreshJobIpPool(JobDO job){
		if(job.getUseProxy()==0){
			return;
		}
		Date expectRefreshTime = job.getExpectRefreshTime();
		Date now = new Date();
		if(now.getTime()>expectRefreshTime.getTime()){
			remoteJobService.refreshIp(job.getId(),null);
		}else{
			return;
		}
		Integer ipDuration = jobService.getIpDuration(job.getId());
		ipDuration=ipDuration==null?5:ipDuration;
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(now);
		calendar.add(calendar.DATE,ipDuration);
		Date nextDate = calendar.getTime();
		jobService.updateJobExpectedRefreshTime(job.getId(),nextDate);


	}

	@PreDestroy
	public void close() {
		thread.interrupt();
	}
}
