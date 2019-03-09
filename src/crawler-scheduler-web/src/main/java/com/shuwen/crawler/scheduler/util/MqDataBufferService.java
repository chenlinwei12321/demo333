package com.shuwen.crawler.scheduler.util;

import com.aliyuncs.ons.model.v20170918.OnsConsumerAccumulateResponse;
import com.shuwen.crawler.scheduler.monitor.mq.MqListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Service
public class MqDataBufferService {
    private static final Logger SCHEDULER_INFO_LOG = LoggerFactory.getLogger("SCHEDULER_INFO_LOG");
    @Resource
    private MqListeners mqListeners;

    @Value("${task.mq.consumerId}")
    private String consumerId;//TASK的cid

    @Value("${task.mq.resources.consumerId}")
    private String resources_consumerId;//视频资源的cid

    @Value("${task.mq.debug.consumerId}")
    private String debug_consumerId;//调试任务的cid

    private String CID_CRAWLER_TASK_ADVANCED="CID_CRAWLER_TASK_ADVANCED";//种子库

    @Value("${mq.diff.scheduler.num}")
    private Integer mq_diff_scheduler_num;//task待调度阈值

    private OnsConsumerAccumulateResponse.Data taskMqData;
    private OnsConsumerAccumulateResponse.Data videoMqData;
    private OnsConsumerAccumulateResponse.Data debugMqData;
    private OnsConsumerAccumulateResponse.Data advancedMqData;

    public OnsConsumerAccumulateResponse.Data getCurrentTaskMqData(){
        this.taskMqData = mqListeners.getMqMessageData(consumerId);
        return this.taskMqData;
    }

    /***
     * 获取待调度任务的mq监控信息
     * @return
     */
    public OnsConsumerAccumulateResponse.Data getTaskMqData(){
        if(this.taskMqData==null){
            this.taskMqData = mqListeners.getMqMessageData(consumerId);
        }
        return this.taskMqData;
    }

    /**
     * 获取视频资源队列的监控信息
     * @return
     */
    public OnsConsumerAccumulateResponse.Data getVideoMqData(){
        return this.videoMqData;
    }

    /**
     * 获取视频资源队列的监控信息
     * @return
     */
    public OnsConsumerAccumulateResponse.Data getDebugMqData(){
        return this.debugMqData;
    }

    /**
     * 获取视频资源队列的监控信息
     * @return
     */
    public OnsConsumerAccumulateResponse.Data getAdvancedMqData(){
        return this.advancedMqData;
    }

    /***
     * 获取待调度任务mq堆积量的阈值
     * @return
     */
    public Integer getMq_diff_scheduler_num(){
        return this.mq_diff_scheduler_num;
    }

    /***
     * 判断任务调度是否可以启动（通过待调度mq堆积数量判断）
     * @return
     */
    public boolean isTaskCanScheduler(){
        if(this.getTaskMqData()==null){
            SCHEDULER_INFO_LOG.error("当前task mq信息为空，不能调度。。。");
            return false;
        }
        Long totalDiff = this.getTaskMqData().getTotalDiff();
        Integer mq_diff_scheduler_num = this.getMq_diff_scheduler_num();
        if(totalDiff>mq_diff_scheduler_num){
            //SCHEDULER_INFO_LOG.warn("任务不能调度，当前mq堆积消息:"+totalDiff+"\t调度阈值:"+mq_diff_scheduler_num);
            return false;
        }
        return true;
    }
    /***
     * 判断任务调度是否可以启动（通过待调度mq堆积数量判断）
     * @return
     */
    public boolean isAdvancedTaskCanScheduler(){
        if(this.getAdvancedMqData()==null){
            SCHEDULER_INFO_LOG.error("当前task mq信息为空，不能调度。。。");
            return false;
        }
        Long totalDiff = this.getAdvancedMqData().getTotalDiff();
        Integer mq_diff_scheduler_num = this.getMq_diff_scheduler_num();
        if(totalDiff>mq_diff_scheduler_num){
            //SCHEDULER_INFO_LOG.warn("任务不能调度，当前mq堆积消息:"+totalDiff+"\t调度阈值:"+mq_diff_scheduler_num);
            return false;
        }
        return true;
    }

    @PostConstruct
    public void startThread(){
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("mq消费缓存更新线程。。。");
                return thread;
            }
        });

        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                taskMqData = mqListeners.getMqMessageData(consumerId);
                long end = System.currentTimeMillis();
                videoMqData = mqListeners.getMqMessageData(resources_consumerId);
                long end2 = System.currentTimeMillis();
                debugMqData=mqListeners.getMqMessageData(debug_consumerId);
                long end3 = System.currentTimeMillis();
                try{
                    advancedMqData=mqListeners.getMqMessageData(CID_CRAWLER_TASK_ADVANCED);
                }catch (Exception e){

                }
                SCHEDULER_INFO_LOG.info("获取任务mq信息:"+(end-start)+"ms\t堆积:"+taskMqData.getTotalDiff()+"\t获取视频mq信息:"+(end2-end)+"ms\t堆积:"+videoMqData.getTotalDiff()+"\t获取debugmq信息:"+(end3-end2)+"ms\t堆积:"+debugMqData.getTotalDiff());

            }
        }, 0, 5, TimeUnit.MINUTES);//五分钟获取一次
    }

}
