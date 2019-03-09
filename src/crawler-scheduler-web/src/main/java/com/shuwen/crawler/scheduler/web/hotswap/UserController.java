package com.shuwen.crawler.scheduler.web.hotswap;

import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.UicResultDO;
import com.shuwen.crawler.rpc.UserService;
import com.shuwen.crawler.rpc.dto.UserDTO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户操作实体类
 */
@RestController
@RequestMapping("crawler/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 获取用户列表
     * @param operator 操作者
     * @param pageNo 页码
     * @param pageRow 每页行数
     * @return site列表
     */
    @GetMapping("")
    public UicResultDO getAllUser(String operator,
                                   @RequestParam(defaultValue = "1") Integer pageNo,
                                   @RequestParam(defaultValue = "20") Integer pageRow, HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<UserDTO> userList = userService.getAllUser(queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(userList)
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
        UserDTO user=new UserDTO();
        user.setId(id);
        user= userService.deleteUser(user);
        return ResultGenerator.createGenerator(UserDTO.class)
                .setData(user)
                .success()
                .getResultDO();
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/all")
    public UicResultDO getAllUser(){
        List<UserDTO> userList = userService.getAllUser();
        return ResultGenerator.createGenerator(List.class)
                .setData(userList)
                .success()
                .getResultDO();
    }

}
