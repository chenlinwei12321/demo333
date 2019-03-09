package com.shuwen.crawler.scheduler.web.rule;

import com.shuwen.crawler.common.ResultGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-11-30 13:53:32
 */
@RestController
@RequestMapping("monitor")
public class RuleController {

    @GetMapping("rules")
    public Object list(String operator,
                       @RequestParam(defaultValue = "1") Integer pageNo,
                       @RequestParam(defaultValue = "20") Integer pageRow,
                       RuleVO ruleVO){
        return ResultGenerator.createGenerator().getResultDO();
    }
}
