package com.shuwen.crawler.scheduler.web.proxy;

import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.rpc.CrawlerProxyIpService;
import com.shuwen.crawler.rpc.dao.ProxyIpDO;
import com.shuwen.crawler.scheduler.web.util.WebFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("proxyIp")
public class CrawlerProxyIpController {

    @Autowired
    CrawlerProxyIpService crawlerProxyIpService;

    @PostMapping("")
    public UicResultDO saveProxyIp(String operator, @RequestBody ProxyIpDO proxyIpDO){
        proxyIpDO.setId(null);
        ProxyIpDO result = crawlerProxyIpService.saveOrUpdate(proxyIpDO, operator);
        return ResultGenerator.createGenerator(ProxyIpDO.class)
                .setData(result)
                .success()
                .getResultDO();
    }

    @PutMapping("{id}")
    public UicResultDO updateProxyIp(String operator,@PathVariable("id")Integer id, @RequestBody ProxyIpDO proxyIpDO){
        proxyIpDO.setId(id);
        ProxyIpDO result = crawlerProxyIpService.saveOrUpdate(proxyIpDO, operator);
        return ResultGenerator.createGenerator(ProxyIpDO.class)
                .setData(result)
                .success()
                .getResultDO();
    }

    @DeleteMapping("{id}")
    public UicResultDO delProxyIp(String operator, @PathVariable("id")Integer id){
        crawlerProxyIpService.delete(id,operator);
        return ResultGenerator.createGenerator(String.class)
                .setData("下线成功！")
                .success()
                .getResultDO();
    }


    @GetMapping("{id}")
    public UicResultDO getIp(@PathVariable("id")Integer id){
        ProxyIpDO byId = crawlerProxyIpService.getById(id);

        return ResultGenerator.createGenerator(ProxyIpDO.class)
                .setData(byId)
                .success()
                .getResultDO();
    }


    @GetMapping("")
    public UicResultDO load2Page(@RequestParam(defaultValue = "1") Integer pageNo,@RequestParam(defaultValue = "20") Integer pageRow,String queryString){
        PageResult<ProxyIpDO> proxyIpDOPageResult = crawlerProxyIpService.load2Page(queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(proxyIpDOPageResult)
                .success()
                .getResultDO();
    }

    @GetMapping("all")
    public UicResultDO all(String queryString){
        List<ProxyIpDO> proxyIpDOPageResult = crawlerProxyIpService.all(queryString);
        return ResultGenerator.createGenerator(List.class)
                .setData(proxyIpDOPageResult)
                .success()
                .getResultDO();
    }

    @PostMapping("/upload")
    public UicResultDO upload(String operator,@RequestParam() MultipartFile file){
        crawlerProxyIpService.importByExcel(file,operator);
        return ResultGenerator.createGenerator(String.class)
                .setData("导入成功！")
                .success()
                .getResultDO();
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response){
        try {
            String content="IP,端口,账号,密码,地域,代理商,描述,标签\r\n";
            WebFileUtils.downLoad(response,content,"IP导入模板.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
