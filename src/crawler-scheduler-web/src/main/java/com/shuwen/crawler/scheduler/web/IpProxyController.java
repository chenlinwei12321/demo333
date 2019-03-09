package com.shuwen.crawler.scheduler.web;

import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.proxy.XhzyIpDTO;
import com.shuwen.crawler.common.util.CrawlerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import us.codecraft.webmagic.utils.XhzyIpProxyHttpUtils;

import java.util.List;

@RestController
@RequestMapping("ippool")
public class IpProxyController {

    @GetMapping("getIp")
    public Object listRecords(@RequestParam(defaultValue = "9999")Integer num,@RequestParam(defaultValue = "crawler")String type){
        List<XhzyIpDTO> ips =null;
        String serverUrl=null;
        if(type.equals("crawler")){
            serverUrl=XhzyIpProxyHttpUtils.CRAWLER_URL;
        }else if(type.equals("static")){
            serverUrl=XhzyIpProxyHttpUtils.STATIC_URL;
        }else{
            throw new CrawlerException("错误参数，type枚举值：crawler、static!");
        }
        ips=XhzyIpProxyHttpUtils.loadIp(num,serverUrl);
        return ResultGenerator.createGenerator().success().setData(ips).getResultDO();
    }

}
