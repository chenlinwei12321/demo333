package com.shuwen.crawler.scheduler.build;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-12-07 10:11:03
 */
@RestController
public class HealthCheckController {

    @GetMapping("ok")
    public Object healthCheck(){
        return "ok";
    }
}
