package com.shuwen.crawler.scheduler.web.filter;

import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.UicResultDO;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;

@Component
public class SsoAuthenticationTokenFilter extends OncePerRequestFilter {

    public static String SW_TOKEN = "dingtalk_sso_jwt";

    @Autowired
    UserFilterService userService;

    @Value("${cms.homeFileName}")
    private String cmsHomeFileName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        boolean flag= false;
        StringBuffer checkUrl=request.getRequestURL();
        String requestURL = request.getRequestURI();
        if (checkUrl!=null&&checkUrl.toString().contains("http://pachong.xinhuazhiyun.com")){ //线上平台走公司的sso
            flag=true;
            SW_TOKEN = "dingtalk_sso_jwt";
        }else{//测试和预发的走自己部署的的sso
            SW_TOKEN="zhiyunbao_sso_jwt";
        }
        String token = getToken(request);
        if (requestURL.endsWith(".css") || requestURL.endsWith(".js") || requestURL.equals("/favicon.ico") || requestURL.equals("/ok") || requestURL.equals("/") || requestURL.equals("") || requestURL.equals("/index") || requestURL.startsWith("/console/")) {
            chain.doFilter(request, response);
            return;
        }

        if(WebSecurityConfig.isIgnore(request.getRequestURI())||WebSecurityConfig.regIsIgnore(request.getRequestURI())){
            chain.doFilter(request, response);
            return;
        }

        if (token != null) {
            Authentication userAuth = SecurityContextHolder.getContext().getAuthentication();
            if (userAuth == null || !(userAuth.getPrincipal() instanceof User)) {
                UserDetails userDetails = this.userService.loadUserByUsername(flag+"#"+token);
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    User user = (User) userDetails;
                    logger.info("authenticated user " + user.getUnionid() + ":" + user.getNick() + ", setting security context");
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    redirect(request, response);
                    return;
                }
            }
        } else {
            redirect(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            logger.debug("登录验证 - 没有设置cookie, path=" + request.getRequestURI());
            return null;
        }
        for (Cookie cookie : cookies) {
            if (!SsoAuthenticationTokenFilter.SW_TOKEN.equals(cookie.getName())) {
                continue;
            }
            String token = cookie.getValue();
            logger.debug("登录验证 - _sw_token=" + token);
            return token;
        }
        return null;
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            UicResultDO uicResultDO = new UicResultDO();
            uicResultDO.setMsg("请登录后访问");
            uicResultDO.setCode("SW-AUTH-401");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            response.getWriter().write(JSONObject.toJSONString(uicResultDO));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

}