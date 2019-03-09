package com.shuwen.crawler.scheduler.web.hotswap;

import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.rpc.RemoteSiteListService;
import com.shuwen.crawler.rpc.dto.SiteDTO;
import com.shuwen.crawler.common.PageResult;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

import javax.annotation.Resource;


@RestController
@RequestMapping("crawler/sites")
public class SiteListController {
    @Resource
    private RemoteSiteListService siteListService;

    /**
     * 获取site列表
     * @param operator 操作者
     * @param pageNo 页码
     * @param pageRow 每页行数
     * @return site列表
     */
    @GetMapping("")
    public UicResultDO getAllSites(String operator,
                                @RequestParam(defaultValue = "1") Integer pageNo,
                                @RequestParam(defaultValue = "20") Integer pageRow,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<SiteDTO> moduleList = siteListService.getAllSites(operator, queryString, pageNo, pageRow);

        return ResultGenerator.createGenerator(PageResult.class)
                .setData(moduleList)
                .success()
                .getResultDO();
    }

    /**
     * 根据id获取site
     * @param operator 操作者
     * @param id site id
     * @return 站点信息
     */
    @GetMapping("{id}")
    public UicResultDO getSite(String operator, @PathVariable("id")Long id){
        SiteDTO moduleDTO = siteListService.getSiteById(id);

        return ResultGenerator.createGenerator(SiteDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }

    /**
     * 根据id删除site
     * @param operator 操作者
     * @param id site id
     * @return 站点信息
     */
    @DeleteMapping("{id}")
    public UicResultDO delSite(String operator, @PathVariable("id")Long id){
        SiteDTO moduleDTO = siteListService.deleteSiteById(id);
        moduleDTO.setDeleted(1);
        return ResultGenerator.createGenerator(SiteDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }


    /**
     * 新增site
     * @param operator 操作者
     * @param siteDTO 站点信息
     */
    @PostMapping("")
    public UicResultDO saveSite(String operator, @RequestBody SiteDTO siteDTO){
        siteDTO.setId(null);
        siteDTO.setDeleted(0);
        siteDTO = siteListService.saveSite(operator, siteDTO);

        return ResultGenerator.createGenerator(SiteDTO.class)
                .setData(siteDTO)
                .success()
                .getResultDO();
    }

    /**
     * 更新site
     * @param operator 操作者
     * @param id siteId
     * @param moduleDTO 站点信息
     */
    @PutMapping("{id}")
    public UicResultDO updateSite(String operator,@PathVariable("id")Long id, @RequestBody SiteDTO moduleDTO){
        moduleDTO.setId(id);
        moduleDTO.setDeleted(0);
        moduleDTO = siteListService.saveSite(operator, moduleDTO);

        return ResultGenerator.createGenerator(SiteDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }
}
