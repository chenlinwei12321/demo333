package com.shuwen.crawler.scheduler.web;

import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.JobType;
import com.shuwen.crawler.common.MockConstants;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.rpc.dao.DAOException;
import com.shuwen.crawler.rpc.dao.ModuleDAO;
import com.shuwen.crawler.rpc.dao.ModuleDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017/11/16 11:56
 */
@RestController
public class OnlineDebugController {
    @Autowired
    private TaskServiceFactory taskService;
    @Resource
    private ModuleDAO moduleDAO;

    private final int jobId = 2001;

    @GetMapping("debug")
    public Object doDebug() throws IOException, DAOException {
        String debugUrl = "http://news.cnhubei.com/xw/hb/xt/201711/t4026908.shtml";
        TaskDO taskDO = new TaskDO();
        taskDO.setJobId(jobId);
        taskDO.setJobType(JobType.DEBUG_TASK);
        ModuleDO module = moduleDAO.getDevModuleById(2L);
        taskDO.setExtras(new JSONObject()
                .fluentPut(MockConstants.CREATOR, "刘瑞杰").fluentPut(MockConstants.DEBUG_MODULE_ID, module.getId())
                .fluentPut(MockConstants.UUID, UUID.randomUUID().toString().replaceAll("-","")));
        taskDO.setJobUrl(debugUrl);
        taskDO.setExpectedRunTime(new Date());

        taskService.addTask(taskDO);
        return "ok";
    }
}
