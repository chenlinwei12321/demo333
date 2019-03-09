package com.shuwen.crawler.scheduler.web.hotswap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.util.CrawlerException;
import com.shuwen.crawler.core.mq.impl.DebugMqClear;
import com.shuwen.crawler.rpc.RemoteHotSwapService;
import com.shuwen.crawler.rpc.RemoteModuleListService;
import com.shuwen.crawler.rpc.RemoteSiteListService;
import com.shuwen.crawler.rpc.dao.ModuleDO;
import com.shuwen.crawler.rpc.dao.SiteDO;
import com.shuwen.crawler.rpc.dto.ModuleDTO;
import com.shuwen.crawler.rpc.dto.SearchDebug;
import com.shuwen.crawler.rpc.dto.SiteDTO;
import com.shuwen.crawler.rpc.model.DebugInfo;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.scheduler.util.ModuleCodeCheckUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.net.URL;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;


@RestController
@RequestMapping("crawler/hotswap")
public class HotSwapController {

    @Resource
    private RemoteHotSwapService hotSwapService;
    @Resource
    private RemoteModuleListService remoteModuleListService;
    @Resource
    private RemoteSiteListService siteListService;

    @Autowired
    private DebugMqClear debugMqClear;

    /**
     * 调试生产对外提供api  签名验证后续再加
     * @param operator 操作者
     * @param engine  引擎关键字
     * @param keyWord 关键字
     * @param signature 签名
     * @param timestamp 时间戳
     * @return
     * @throws Exception
     */
    @GetMapping("crawlerSearch")
    public UicResultDO debug(String operator, String engine ,
                             String keyWord)//,@RequestParam String signature,@RequestParam String timestamp)
            throws Exception {
        JSONArray array=new JSONArray();
        String engineStr=URLDecoder.decode(engine,"UTF-8");
        List<ModuleDO> modules= remoteModuleListService.getEditModuleList("搜索_首页"+engineStr);
        if(modules.size()>0){
            SiteDTO site=siteListService.getSiteById(modules.get(0).getDomainId());
            ModuleDTO moduleDTO=new ModuleDTO();
            moduleDTO.setCode(modules.get(0).getCode());
            moduleDTO.setDomain(site.getName());
            moduleDTO.setDomainId(site.getId());
            moduleDTO.setEnableJS(modules.get(0).getEnableJS());
            JSONObject json=new JSONObject();
            json.put("siteName",site.getName());
            json.put("siteDomain",site.getDomain());
            moduleDTO.setFeatureCode(json.toJSONString());
            moduleDTO.setId(modules.get(0).getId());
            moduleDTO.setIsConfig(modules.get(0).getIsConfig());
            moduleDTO.setName(modules.get(0).getName());
            moduleDTO.setPageType(modules.get(0).getPageType());
//            moduleDTO.setPart(modules.get(0).getPart());
            moduleDTO.setRegexUrl(modules.get(0).getRegexUrl());
            moduleDTO.setType(modules.get(0).getType());
            SearchDebug debug=hotSwapService.getTestUrl(moduleDTO.getId());
            String debugUrl=debug.getTestUrl();
            if(null == debugUrl){
                throw new CrawlerException("调试链接不能为空");
            }
            try {
                URL url = new URL(debugUrl);
            }catch (Exception e){
                throw new CrawlerException("调试URL不正确，请输入正确的调试URL");
            }
//            debugUrl = URLDecoder.decode(debugUrl, "utf-8");

            for(int i=0;i<10;i++){
                //url定义三个参数，关键字，分页，起始，
                String result=String.format(debugUrl,keyWord,i*20,20);
                String uuid = hotSwapService.debugModuleScript(operator, moduleDTO, result.trim(),"{ss}");
                DebugInfo debugInfo = hotSwapService.getDebugActions(operator, 0, uuid);
                getResult(array,debugInfo);
                int count=0;
                while(!debugInfo.isFinish()){
                    debugInfo = hotSwapService.getDebugActions(operator, debugInfo.getCurrentId(), uuid);
                    getResult(array,debugInfo);
                    count++;
                     if(count>10){ //防止调试死循环
                         break;
                     }
                }
            }

        }

        return ResultGenerator.createGenerator(JSONArray.class)
                .setData(array)
                .success()
                .getResultDO();
    }

    /**
     * 获取调试结果
     * @param debugInfo
     * @return
     */
    public void getResult(JSONArray array, DebugInfo debugInfo){
        List<String> result=debugInfo.getDebugActions();
        try{
            for(String str: result){
                if(str.indexOf("_results_")>-1){
                    String json=str.substring(str.indexOf("["),str.indexOf("]")+1);
                    array.addAll(JSONObject.parseArray(json));
                }
            }
        }catch (Exception e){
            throw new CrawlerException("获取API数据失败！");
        }

    }

    /**
     * 执行一次调试，返回调试的uuid
     * @param operator 操作者
     * @param moduleDTO 模块信息
     * @param debugUrl 调试链接
     * @return uuid
     */
    @PostMapping("debug")
    public UicResultDO debug(String operator, @RequestBody ModuleDTO moduleDTO,String debugUrl,@RequestParam(defaultValue = "{}")String debugParams) throws Exception {
        if(null == debugUrl){
            throw new CrawlerException("调试链接不能为空");
        }
        String normalUrl = debugUrl.trim();
        try {
            URL url = new URL(normalUrl);
        }catch (Exception e){
            throw new CrawlerException("调试URL不正确，请输入正确的调试URL");
        }

        debugUrl = URLDecoder.decode(debugUrl, "utf-8");

        String uuid = hotSwapService.debugModuleScript(operator, moduleDTO, normalUrl,debugParams);
        return ResultGenerator.createGenerator(JSONObject.class)
                .setData(new JSONObject().fluentPut("uuid", uuid))
                .success()
                .getResultDO();
    }

    /***
     * 按照字段调试。
     * @param operator
     * @param modulerId
     * @param debugUrl
     * @param debugParams json串=》要查询的表名和字段id。sw_module_context，sw_module_field
     * @return
     * @throws Exception
     */
    @GetMapping("debug/{modulerId}/config")
    public UicResultDO configModuleDebugByField(String operator, @PathVariable Integer modulerId,String debugUrl,@RequestParam(defaultValue = "{}")String debugParams) throws Exception {
        if(debugUrl==null){
            throw new CrawlerException("调试链接不能为空");
        }
        String nornalUrl = debugUrl.trim();
        try {
            URL url = new URL(nornalUrl);
        }catch (Exception e){
            throw new CrawlerException("调试URL不正确，请输入正确的调试URL");
        }

        ModuleDTO moduleById = remoteModuleListService.getModuleById(operator, modulerId);
        String uuid = hotSwapService.debugModuleScript(operator,moduleById,nornalUrl,debugParams);
        return ResultGenerator.createGenerator(JSONObject.class)
                .setData(new JSONObject().fluentPut("uuid", uuid))
                .success()
                .getResultDO();
    }

    /**
     * 获取某次调试的输出信息
     * @param operator 操作者
     * @param startId 消息开始id
     * @param uuid 调试的uuid
     * @return 调试信息
     */
    @GetMapping("debug/{uuid}")
    public UicResultDO debugInfo(String operator, Long startId,@PathVariable String uuid){
        DebugInfo debugInfo = hotSwapService.getDebugActions(operator, startId, uuid);
        return ResultGenerator.createGenerator(DebugInfo.class)
                .setData(debugInfo)
                .success()
                .getResultDO();
    }

    @GetMapping("check/{uuid}")
    public UicResultDO checkInfo(String operator, Long startId,@PathVariable String uuid){
        DebugInfo debugInfo = hotSwapService.getCheckAction(operator, startId, uuid);
        return ResultGenerator.createGenerator(DebugInfo.class)
                .setData(debugInfo)
                .success()
                .getResultDO();
    }


    /**
     * 发布一个module
     * @param operator 操作者
     * @param moduleDTO 模块信息
     */
    @PostMapping("publish")
    public UicResultDO publish(String operator, @RequestBody ModuleDTO moduleDTO){
        moduleDTO = hotSwapService.publishModule(operator, moduleDTO);

        if(moduleDTO.getType()==1){//groovy语法校验
            ModuleCodeCheckUtil.groovyCodeCheck(moduleDTO.getCode(),moduleDTO.getName());
        }

        return ResultGenerator.createGenerator(ModuleDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }
}
