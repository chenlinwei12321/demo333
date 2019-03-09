package com.shuwen.crawler.scheduler.web.rule;

import cn.xhzy.monitor.MonitorRecordService;
import cn.xhzy.monitor.MonitorVerificationModuleService;
import cn.xhzy.monitor.dao.po.MonitorVerificationModule;
import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-11-30 11:58:20
 */
@RestController
@RequestMapping("monitor")
public class BaseRuleController {

    @Resource
    private MonitorVerificationModuleService monitorVerificationModuleService;

    @PostMapping("verifications")
    public Object create(String operator, MonitorVerificationModule monitorVerificationModule){
        MonitorVerificationModule newModule = monitorVerificationModuleService.insertSelective(monitorVerificationModule);
        return ResultGenerator.createGenerator(MonitorVerificationModule.class)
                .success().setData(newModule).getResultDO();
    }

    @PutMapping("verifications/{id}")
    public Object update(String operator, @PathVariable("id") Long id, MonitorVerificationModule monitorVerificationModule){
        monitorVerificationModule.setId(id);
        MonitorVerificationModule newModule = monitorVerificationModuleService.updateSelectiveBy(id, monitorVerificationModule);
        return ResultGenerator.createGenerator(MonitorVerificationModule.class)
                .success().setData(newModule).getResultDO();
    }

    @GetMapping("verifications")
    public Object list(String operator,
                        @RequestParam(defaultValue = "1") Integer pageNo,
                        @RequestParam(defaultValue = "20") Integer pageRow,
                        String name, Integer status){
        JSONObject result = monitorVerificationModuleService.selectOrderByCreateTime(pageNo, pageRow, true);
        PageResult pageResult = new PageResult();
        pageResult.setRows((List) result.get("list"));
        pageResult.setPageNo(pageNo);
        pageResult.setTotal(result.getLong("total"));

        return ResultGenerator.createGenerator().success().setData(pageResult)
                .getResultDO();
    }

    @GetMapping("verifications/{id}")
    public Object get(String operator, @PathVariable("id")Long id){
        MonitorVerificationModule monitorVerificationModule = monitorVerificationModuleService.selectBy(id);
        return ResultGenerator.createGenerator(MonitorVerificationModule.class)
                .success().setData(monitorVerificationModule).getResultDO();
    }
}
