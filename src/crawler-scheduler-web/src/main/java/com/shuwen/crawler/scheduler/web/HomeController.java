/**
 *
 */
package com.shuwen.crawler.scheduler.web;

import com.alibaba.fastjson.JSONObject;
import com.shuwen.cms.client.CmsClient;
import com.shuwen.crawler.rpc.UserService;
import com.shuwen.crawler.rpc.dto.UserDTO;
import com.shuwen.crawler.scheduler.web.filter.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * @author: chenkang
 * @email: chenkang@shuwen.com
 * @date: 2017年7月8日 下午2:44:47
 */
@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private CmsClient cmsClient;
    @Resource
    UserService userService;


    @Value("${cms.homeFileName}")
    private String cmsHomeFileName;

    @RequestMapping(value = {"/", "/index", "/console/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public String home(Model model) {
        String userStr = "{}";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                userStr = JSONObject.toJSONString(user);
            }
        }
        //将 cms加载的HTML代码片段放在model中，提供给模板引擎进行渲染
        String assetsVersion = cmsClient.load(cmsHomeFileName);
        model.addAttribute("env", cmsHomeFileName);
        model.addAttribute("assetsHost", "s.newscdn.cn");
        model.addAttribute("assetsVersion", assetsVersion);
        model.addAttribute("user", userStr);
        logger.info(userStr);
        return "index";
    }

}
