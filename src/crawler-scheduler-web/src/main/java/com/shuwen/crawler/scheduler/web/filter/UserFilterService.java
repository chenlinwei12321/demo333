package com.shuwen.crawler.scheduler.web.filter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.shuwen.crawler.common.util.HttpClientUtil;
import com.shuwen.crawler.rpc.RoleGroupService;
import com.shuwen.crawler.rpc.UserService;
import com.shuwen.crawler.rpc.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class UserFilterService implements UserDetailsService {

    @Resource
    UserService userService;
    @Resource
    RoleGroupService roleGroupService;

    @Value("${cms.homeFileName}")
    private String cmsHomeFileName;

    @Value("${crowd.user.admin:all}")
    private String admins;

    private static final Logger logger = LoggerFactory.getLogger(UserFilterService.class);

    protected String verifyUrlTemplate = "http://sso.xinhuazhiyun.com/verify?dingtalk_sso_jwt=%s";

    @Override
    public UserDetails loadUserByUsername(String token) throws UsernameNotFoundException {
        logger.debug("登录验证 - _sw_token=" + token);
        try {
            if (token.indexOf("#")<6){
                String flag=token.split("#")[0];
                token=token.split("#")[1];
                if (!Boolean.valueOf(flag)) {//登录验证
                    verifyUrlTemplate = "http://sso.pachong.xinhuazhiyun.com/verify?zhiyunbao_sso_jwt=%s";
                }else{
                    verifyUrlTemplate = "http://sso.xinhuazhiyun.com/verify?dingtalk_sso_jwt=%s";
                }
            }
            SsoResult<User> resultDO = getSsoUser(token);
            if (resultDO == null || resultDO.getCode() != 200) {
                logger.error("登录验证 - rpc请求不成功，code=" + resultDO.getCode());
                return null;
            }

            User user = resultDO.getData();
            //将用户保存至数据库中
            UserDTO userDto=userService.saveUser(changeModel(user));
            List<String> roles = Arrays.asList("ROLE_ADMIN", "ROLE_USER");
            List arrList = new ArrayList(roles);
            arrList.add(roleGroupService.getUserRole(userDto.getNick()));
            user.setRoles(arrList);
            user.setId(userDto.getId());
            user.setUserId(userDto.getId() + "");
            if (user == null) {
                logger.error("登录验证 - 登录用户不存在, token=" + token);
                return null;
            }

            return user;
        } catch (Exception e) {
            logger.error("登录验证 - 登录异常, token=" + token, e);
        }
        return null;
    }

    private SsoResult<User> getSsoUser(String token) {
        String url = String.format(verifyUrlTemplate, token);
        String ret = HttpClientUtil.httpGet(url, null, 3, null);
        return JSONObject.parseObject(ret, new TypeReference<SsoResult<User>>() {
        });
    }

    /**
     *
     * @return
     */
    private UserDTO changeModel(User user){
        UserDTO userDTO=new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        return userDTO;
    }

}
