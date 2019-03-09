package com.shuwen.crawler.scheduler.web.aop;

import cn.xhzy.crawler.basic.dao.mysql.CommonDao;
import com.shuwen.crawler.common.ResultCode;
import com.shuwen.crawler.common.util.CrawlerException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-11-20 11:19:09
 */
//@Service
@Aspect
public class PrivilegeAspect {

    @Resource
    private CommonDao<String> commonDao;

    private String tableName = "sw_white_list";

    @Around("execution(* com.shuwen.crawler.rpc.*.*(..))")
    public Object checkPrivilege(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if(args.length>1){
            String operator = (String) args[0];
            if(operator!=null){
                List<String> whiteList = commonDao.selectByCriteria(tableName, commonDao.createCriteria()
                                .eq("operator", operator)
                        , String.class, "operator");
                if(whiteList.size()>0){
                    return joinPoint.proceed();
                }
            }
            throw new CrawlerException("当前用户权限不足", ResultCode.NO_PRIVILEGE);
        }else{
            return joinPoint.proceed();
        }
    }
}
