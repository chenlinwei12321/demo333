package com.shuwen.crawler.scheduler.web.hotswap;

import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.common.ots.OtsClientUtils;
import com.shuwen.crawler.common.util.CrawlerException;
import com.shuwen.crawler.rpc.GroupListService;
import com.shuwen.crawler.rpc.dto.GroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("crawler/group")
public class GroupListController {
    @Resource
    private GroupListService groupListService;

    @GetMapping("")
    public UicResultDO getAllGroups(String operator,
                                @RequestParam(defaultValue = "1") Integer pageNo,
                                @RequestParam(defaultValue = "20") Integer pageRow,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<GroupDTO> groupDTOs = groupListService.getAllGroups(operator, queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(groupDTOs)
                .success()
                .getResultDO();
    }

    @GetMapping("{id}")
    public UicResultDO getGroup(String operator, @PathVariable("id")Long id){
        GroupDTO groupDTO = groupListService.getGroupById(id);
        return ResultGenerator.createGenerator(GroupDTO.class)
                .setData(groupDTO)
                .success()
                .getResultDO();
    }

    @DeleteMapping("{id}")
    public UicResultDO delGroup(String operator, @PathVariable("id")Long id){
        GroupDTO groupDTO = groupListService.deleteGroupById(operator,id);
        return ResultGenerator.createGenerator(GroupDTO.class)
                .setData(groupDTO)
                .success()
                .getResultDO();
    }


    @PostMapping("")
    public UicResultDO saveGroup(String operator, @RequestBody GroupDTO groupDTO){
        groupDTO = groupListService.saveGroup(operator, groupDTO);

        return ResultGenerator.createGenerator(GroupDTO.class)
                .setData(groupDTO)
                .success()
                .getResultDO();
    }

    @PutMapping("{id}")
    //修改，无法修改ots表名，和主键信息！
    public UicResultDO updateGroup(String operator,@PathVariable("id")Long id, @RequestBody GroupDTO groupDTO){
        this.isCanDelete(id);
        groupDTO.setId(id);
        groupDTO = groupListService.updateGroup(operator, groupDTO);
        return ResultGenerator.createGenerator(GroupDTO.class)
                .setData(groupDTO)
                .success()
                .getResultDO();
    }

    private void isCanDelete(Long id){
        String[] ids="0,1,2,3,4,5,6,7,9".split(",");
        for(String idStr:ids){
            if(Long.valueOf(idStr).equals(id)){
                throw new CrawlerException("系统内置分组，不能修改！");
            }
        }
    }

}
