package com.shuwen.crawler.scheduler.web.hotswap;

import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.rpc.VersionListService;
import com.shuwen.crawler.rpc.dto.VersionDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("crawler/version")
public class VersionListController {
    @Resource
    private VersionListService versionListService;

    @GetMapping("")
    public UicResultDO getAllGroups(String operator,
                                @RequestParam(defaultValue = "1") Integer pageNo,
                                @RequestParam(defaultValue = "20") Integer pageRow,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<VersionDTO> allVersions = versionListService.getAllVersions(operator, queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(allVersions)
                .success()
                .getResultDO();
    }

    @GetMapping("{id}")
    public UicResultDO getGroup(String operator, @PathVariable("id")Long id){
        VersionDTO versionDTO = versionListService.getVersionById(id);
        return ResultGenerator.createGenerator(VersionDTO.class)
                .setData(versionDTO)
                .success()
                .getResultDO();
    }

    @PostMapping("")
    public UicResultDO saveGroup(String operator, @RequestBody VersionDTO versionDTO){
        versionDTO = versionListService.saveVersion(operator, versionDTO);
        return ResultGenerator.createGenerator(VersionDTO.class)
                .setData(versionDTO)
                .success()
                .getResultDO();
    }
}
