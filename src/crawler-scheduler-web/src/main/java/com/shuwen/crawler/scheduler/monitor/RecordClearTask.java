package com.shuwen.crawler.scheduler.monitor;

import cn.xhzy.monitor.MonitorRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-12-12 10:10:06
 */
@Service
@EnableScheduling
public class RecordClearTask {
    Logger logger = LoggerFactory.getLogger(RecordClearTask.class);

    @Resource
    private MonitorRecordService monitorRecordService;

    @Scheduled(cron = "0 0 0 * * ? ")
    public void clearTask(){
        final int day = 7;

        logger.info("【开始执行清除"+day+"天前监控记录的任务...】");
        long startTime = System.currentTimeMillis();
        long l = monitorRecordService.deleteRecordBefore(day);
        long endTime = System.currentTimeMillis();
        logger.info("【执行清除"+day+"天前监控记录任务完毕。共清除"+l+"条记录，耗时"+(endTime-startTime)+"ms】");
    }

}
