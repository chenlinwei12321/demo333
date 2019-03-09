package com.shuwen.crawler.scheduler.web.filter;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    UserFilterService userService;

    //不拦截的uri
    private static List<String> ignoreUris=new ArrayList<>();
    private static List<String> regIgnoreUris=new ArrayList<>();

    static{
        ignoreUris.add("/favicon.ico");
        ignoreUris.add("/**/*.css");
        ignoreUris.add("/**/*.js");
        ignoreUris.add("/ok");
        ignoreUris.add("/crawler/role/all");//获取权限分组
        ignoreUris.add("/phoneMessage/send");
        ignoreUris.add("/phoneMessage/getData");
        ignoreUris.add("/crawler/modules/crawler");//创建爬虫
        ignoreUris.add("/crawler/modules/proving");//验收
        ignoreUris.add("/crawler/datav/total");//datav
        ignoreUris.add("/crawler/datav/source");//datav
        ignoreUris.add("/crawler/datav/fenfa");//datav
        ignoreUris.add("/crawler/datav/mq");//datav
        ignoreUris.add("/crawler/datav/ecs");//datav
        ignoreUris.add("/crawler/datav/download");//datav
        ignoreUris.add("/crawler/datav/save");//datav
        ignoreUris.add("/crawler/datav/everyTime");//datav
        ignoreUris.add("/crawler/datav/huoyue");//datav
        ignoreUris.add("/crawler/datav/gundong/everyCrawler");//datav
        ignoreUris.add("/crawler/datav/gundong/host");//datav
        ignoreUris.add("/crawler/modules/upload/module");//爬虫导入
        ignoreUris.add("/crawler/hotswap/crawlerSearch");//爬虫debug

        ignoreUris.add("/prometheus");//prometheus 监控
//        ignoreUris.add("/crawler/datav/*/*");//datav
        //通过正则过滤,上边的这个针对于类似路径url无法配置
        regIgnoreUris.add("/proxyGroup/getIpByGroup/.*");
    }

    public static boolean isIgnore(String uri){
        if(ignoreUris.contains(uri)){
            return true;
        }
        return false;
    }


    public static boolean regIsIgnore(String uri){
        for(int x=0;x<regIgnoreUris.size();x++){
            String reg = regIgnoreUris.get(x);
            if(uri.matches(reg)){
                return true;
            }
        }
        return false;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService);
    }




    //设置了不拦截就是永远取不到用户信息
    @Override
    public void configure(WebSecurity web) throws Exception {
        for(int x=0;x<ignoreUris.size();x++){
            String uri = ignoreUris.get(x);
            web.ignoring().antMatchers(uri);
        }
        web.ignoring().antMatchers("/proxyGroup/getIpByGroup/*");
    }

    @Autowired
    public SsoAuthenticationTokenFilter ssoAuthenticationTokenFilterBean;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();//方便调试，关闭csrf

        http.authorizeRequests()
                .antMatchers(
                        "/",
                        "/index",
                        "/console/**"
                ).permitAll()
                .anyRequest().authenticated();
        http.addFilterBefore(ssoAuthenticationTokenFilterBean, UsernamePasswordAuthenticationFilter.class);
    }
}