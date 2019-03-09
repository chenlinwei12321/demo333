package com.shuwen.crawler.scheduler.web.aop;

import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.common.util.CrawlerException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-11-20 11:36:27
 * 全局业务异常拦截，生成错误返回信息。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CrawlerException.class)
    @ResponseBody
    public Object handelCrawlerException(CrawlerException e){
        UicResultDO uicResultDO = new UicResultDO();
        uicResultDO.setMsg(e.getMessage());
        uicResultDO.setCode(e.getCode());
        return uicResultDO;
    }
}
