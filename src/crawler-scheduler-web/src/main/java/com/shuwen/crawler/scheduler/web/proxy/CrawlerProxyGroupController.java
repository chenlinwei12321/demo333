package com.shuwen.crawler.scheduler.web.proxy;

import com.alibaba.fastjson.JSONArray;
import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.common.util.ApiUrlUtil;
import com.shuwen.crawler.common.util.CrawlerException;
import com.shuwen.crawler.rpc.CrawlerProxyGroupService;
import com.shuwen.crawler.rpc.dao.ProxyGroupDO;
import com.shuwen.crawler.rpc.dao.ProxyIpDO;
import com.shuwen.crawler.scheduler.web.util.ReqMapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("proxyGroup")
public class CrawlerProxyGroupController {

    @Value("${crawler.accessKey}")
    public String accessKey;

    @Value("${crawler.accessKeySecret}")
    public String accessKeySecret;


    @Autowired
    CrawlerProxyGroupService crawlerProxyGroupService;

    @PostMapping("")
    public UicResultDO saveProxyGroup(String operator, @RequestBody ProxyGroupDO proxyGroupDO){
        proxyGroupDO.setId(null);
        ProxyGroupDO result = crawlerProxyGroupService.saveOrUpdate(proxyGroupDO, operator);
        return ResultGenerator.createGenerator(ProxyGroupDO.class)
                .setData(result)
                .success()
                .getResultDO();
    }

    @PutMapping("{id}")
    public UicResultDO updateProxyGroup(String operator,@PathVariable("id")Integer id, @RequestBody ProxyGroupDO proxyGroupDO){
        proxyGroupDO.setId(id);
        ProxyGroupDO result = crawlerProxyGroupService.saveOrUpdate(proxyGroupDO, operator);
        return ResultGenerator.createGenerator(ProxyGroupDO.class)
                .setData(result)
                .success()
                .getResultDO();
    }

    @DeleteMapping("{id}")
    public UicResultDO delProxyGroup(String operator, @PathVariable("id")Integer id){
        crawlerProxyGroupService.delete(id,operator);
        return ResultGenerator.createGenerator(String.class)
                .setData("下线成功！")
                .success()
                .getResultDO();
    }

    @GetMapping("{id}")
    public UicResultDO getProxyGroup(@PathVariable("id")Integer id){
        ProxyGroupDO byId = crawlerProxyGroupService.get(id);

        return ResultGenerator.createGenerator(ProxyGroupDO.class)
                .setData(byId)
                .success()
                .getResultDO();
    }

    @GetMapping("")
    public UicResultDO load2Page(@RequestParam(defaultValue = "1") Integer pageNo,@RequestParam(defaultValue = "20") Integer pageRow,String queryString){
        PageResult<ProxyGroupDO> proxyGroupDOPageResult = crawlerProxyGroupService.load2Page(queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(proxyGroupDOPageResult)
                .success()
                .getResultDO();
    }

    @GetMapping("getAllGroup")
    public UicResultDO getAllGroup(){
        List<ProxyGroupDO> result = crawlerProxyGroupService.getAllGroup();
        return ResultGenerator.createGenerator(List.class)
                .setData(result)
                .success()
                .getResultDO();
    }


    @PutMapping("updateIpInfo/{id}")
    public UicResultDO updateIpInfo(@PathVariable("id")Integer id, @RequestBody Map<String,Object> reqMap,String operator){
        String username = ReqMapUtils.getStringFromReqMap(reqMap, "username");
        String password = ReqMapUtils.getStringFromReqMap(reqMap, "password");
        crawlerProxyGroupService.updateIpInfoByGroupId(id,username,password,operator);
        return ResultGenerator.createGenerator(String.class)
                .setData("修改成功")
                .success()
                .getResultDO();
    }

    @PutMapping("bindIps/{id}")
    public UicResultDO bindIps(@PathVariable("id")Integer id, @RequestBody Map<String,Object> reqMap,String operator){
        List<Integer> ips= (List<Integer>) ReqMapUtils.withOutNullException(reqMap,"ips");
        crawlerProxyGroupService.bindIps(id,ips,operator);
        return ResultGenerator.createGenerator(String.class)
                .setData("绑定成功")
                .success()
                .getResultDO();
    }

    @GetMapping("getAllIpsByGroup/{id}")
    public UicResultDO getAllIpByGroup(@PathVariable("id")Integer id){
        List<ProxyIpDO> ipByGroup = crawlerProxyGroupService.getAllOkIp(id);
        return ResultGenerator.createGenerator(List.class)
                .setData(ipByGroup)
                .success()
                .getResultDO();
    }


    @GetMapping("getIpByGroup/{id}")
    public UicResultDO getIpByGroup(@PathVariable Integer id,@RequestParam Integer count,@RequestParam String signature,@RequestParam String timestamp){
        boolean attestation = ApiUrlUtil.attestation(accessKey, accessKeySecret, signature, timestamp);
        if(!attestation){
            throw new CrawlerException("爬虫验签失败！");
        }

        long last = System.currentTimeMillis() - Long.valueOf(timestamp);
        if(last>1000*60*20){
            throw new CrawlerException("签名时间大于20分钟，请从新签名！");
        }

        List<ProxyIpDO> ipByGroup = crawlerProxyGroupService.getIpByGroup(id, count);
        return ResultGenerator.createGenerator(List.class)
                .setData(ipByGroup)
                .success()
                .getResultDO();
    }

    @GetMapping("getOtherIpByGroup/{id}")
    public UicResultDO getOtherIpByGroup(@PathVariable("id")Integer id, Integer num, String excludeIpJson){
        List<String> excludeIps = JSONArray.parseArray(excludeIpJson, String.class);
        List<ProxyIpDO> ipByGroup = crawlerProxyGroupService.getOtherIpByGroup(id, num,excludeIps);
        return ResultGenerator.createGenerator(List.class)
                .setData(ipByGroup)
                .success()
                .getResultDO();
    }

}
