package com.shuwen.crawler.scheduler.web.persist;

import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.rpc.RemotePersistService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("persist")
public class PersistController {

    @Resource
    private RemotePersistService remotePersistService;

    /***
     * 抽出来功能到单独项目
     * @param start
     * @param limit
     * @param source
     * @param title
     * @param date
     * @return
     * @throws Exception
     */
    @GetMapping("/")
    public UicResultDO list(@RequestParam(defaultValue = "")String start,@RequestParam(defaultValue = "80")Integer limit,@RequestParam(defaultValue = "") String source,@RequestParam(defaultValue = "") String title,@RequestParam(defaultValue = "")String date) throws Exception {
//        List<Map<String,Object>> persistList = remotePersistService.getPersistList(start,source,title,date,limit);
//        return ResultGenerator.createGenerator(List.class).success().setData(persistList).getResultDO();
        return ResultGenerator.createGenerator(String.class).success().setData("ok").getResultDO();
    }

}
