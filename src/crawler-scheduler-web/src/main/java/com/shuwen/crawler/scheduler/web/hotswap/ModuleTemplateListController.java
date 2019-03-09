package com.shuwen.crawler.scheduler.web.hotswap;

import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.rpc.RemoteModuleTemplateListService;
import com.shuwen.crawler.rpc.dto.ModuleTemplateDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("crawler/moduletemplates")
public class ModuleTemplateListController {

    @Resource
    private RemoteModuleTemplateListService remoteModuleTemplateListService;

    @GetMapping("")
    public UicResultDO getAll(String operator,
                                    @RequestParam(defaultValue = "1") Integer pageNo,
                                    @RequestParam(defaultValue = "20") Integer pageRow, HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<ModuleTemplateDTO> moduleTemplateDTOPageResult = remoteModuleTemplateListService.getAll(operator, queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(moduleTemplateDTOPageResult)
                .success()
                .getResultDO();
    }

    @GetMapping("{id}")
    public UicResultDO findById(String operator, @PathVariable("id")Long id){
        ModuleTemplateDTO moduleTemplateDTO = remoteModuleTemplateListService.findById(id);
        return ResultGenerator.createGenerator(ModuleTemplateDTO.class)
                .setData(moduleTemplateDTO)
                .success()
                .getResultDO();
    }

    @DeleteMapping("{id}")
    public UicResultDO deleteById(String operator, @PathVariable("id")Long id){
        ModuleTemplateDTO moduleTemplateDTO = remoteModuleTemplateListService.deleteById(operator,id);
        return ResultGenerator.createGenerator(ModuleTemplateDTO.class)
                .setData(moduleTemplateDTO)
                .success()
                .getResultDO();
    }


    @PostMapping("")
    public UicResultDO save(String operator, @RequestBody ModuleTemplateDTO moduleTemplateDTO){
        moduleTemplateDTO = remoteModuleTemplateListService.save(operator, moduleTemplateDTO);

        return ResultGenerator.createGenerator(ModuleTemplateDTO.class)
                .setData(moduleTemplateDTO)
                .success()
                .getResultDO();
    }

    @PutMapping("{id}")
    public UicResultDO update(String operator,@PathVariable("id")Long id, @RequestBody ModuleTemplateDTO moduleTemplateDTO){
        moduleTemplateDTO.setId(id);
        moduleTemplateDTO = remoteModuleTemplateListService.update(operator, moduleTemplateDTO);
        return ResultGenerator.createGenerator(ModuleTemplateDTO.class)
                .setData(moduleTemplateDTO)
                .success()
                .getResultDO();
    }

}