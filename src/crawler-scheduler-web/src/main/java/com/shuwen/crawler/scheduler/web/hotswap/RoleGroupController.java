package com.shuwen.crawler.scheduler.web.hotswap;


import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.rpc.RoleGroupService;
import com.shuwen.crawler.rpc.UserService;
import com.shuwen.crawler.rpc.dto.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户权限组控制类
 */
@RestController
@RequestMapping("crawler/role")
public class RoleGroupController {

    @Resource
    private UserService userService;
    @Resource
    private RoleGroupService roleGroupService;

    /**
     * 修改资源
     * @return
     */
    @PostMapping("/resource/update")
    public UicResultDO updateResource(@RequestBody RoleResourceDTO resourceDTO){
        RoleResourceDTO resource=roleGroupService.saveorUpdateResorce(resourceDTO);
        return ResultGenerator.createGenerator(RoleResourceDTO.class)
                .setData(resource)
                .success()
                .getResultDO();
    }

    /**
     * 获取下拉选资源列表
     * @param groupId 分组id
     * @param roleType 权限类型
     * @return 所有集合
     */
    @GetMapping("/resource/select")
    public UicResultDO selectResource( Long groupId, String roleType){
        List<RoleDTO> list=roleGroupService.getAllGroup(groupId,roleType);
        return ResultGenerator.createGenerator(List.class)
                .setData(list)
                .success()
                .getResultDO();
    }


    /**
     * 获取所有任务及爬虫列表
     * @param operator 操作者
     * @return 所有集合
     */
    @GetMapping("/resource")
    public UicResultDO getAllReource(String operator,
                                  @RequestParam(defaultValue = "1") Integer pageNo,
                                  @RequestParam(defaultValue = "20") Integer pageRow,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<RoleResourceDTO> jobs = roleGroupService.ownerResource(operator, queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(jobs)
                .success()
                .getResultDO();
    }


    /**
     * 查询分组列表
     * @param operator 操作者
     */
    @GetMapping("/group")
    public UicResultDO selectGroup(String operator){
        List<UserGroupDTO> list=roleGroupService.selectGroupByName(operator);
        return ResultGenerator.createGenerator(List.class)
                .setData(list)
                .success()
                .getResultDO();

    }
    /**
     * 查询分组列表
     */
    @GetMapping("/all")
    public UicResultDO selectGroup(){
        List<UserGroupDTO> list=roleGroupService.selectGroupByName("雒云飞");
        return ResultGenerator.createGenerator(List.class)
                .setData(list)
                .success()
                .getResultDO();

    }

    /**
     * 查询分组下用户信息
     * @param operator 操作者
     */
    @GetMapping("/group/{id}")
    public UicResultDO selectGroupUser(String operator, @PathVariable("id")Long id) {
        RoleDTO role = roleGroupService.selectGroup(id);
        return ResultGenerator.createGenerator(RoleDTO.class)
                .setData(role)
                .success()
                .getResultDO();
    }

    /**
     * 创建分组
     * @param operator 操作者
     * @param roleDTO  拥有情况
     */
    @PostMapping("/group")
    public UicResultDO createGroup(String operator, @RequestBody RoleDTO roleDTO){
        RoleDTO role=roleGroupService.saveOrUpdateGroup(roleDTO);
        return ResultGenerator.createGenerator(RoleDTO.class)
                .setData(role)
                .success()
                .getResultDO();
    }

    /**
     * 删除分组
     * @param operator 操作者
     * @param id  分组id
     */
    @GetMapping("/group/delete/{id}")
    public UicResultDO deleteGroup(String operator, @PathVariable("id")Long id){
        int count=roleGroupService.deleteGroup(id);
        return ResultGenerator.createGenerator(Integer.class)
                .setData(count)
                .success()
                .getResultDO();
    }



}
