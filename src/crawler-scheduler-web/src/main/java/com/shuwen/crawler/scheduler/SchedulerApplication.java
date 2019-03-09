/**
 * 
 */
package com.shuwen.crawler.scheduler;

import com.shuwen.cms.client.CmsClient;
import com.shuwen.diamon.spring.client.DiamondPropertySourceFactory;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;


@SpringBootApplication(scanBasePackages={"com.shuwen.crawler.scheduler","com.shuwen.crawler.rpc", "com.shuwen.crawler.core","com.shuwen.crawler.common.logtail","cn.xhzy.monitor","com.shuwen.crawler.common.jedis.utils","com.shuwen.crawler.common.ots"})
@PropertySource("classpath:env.properties")
@ImportResource("classpath:application-core.xml")
@PropertySource(name = "diamond", value = "", factory = DiamondPropertySourceFactory.class)
@EnablePrometheusEndpoint
public class SchedulerApplication extends SpringBootServletInitializer{

	@Bean(initMethod = "init", destroyMethod = "destroy")
	public CmsClient cmsClient() {
		CmsClient cmsClient = new CmsClient();
		cmsClient.setAppName("crawler-web");
		cmsClient.setEnv("test");
		return cmsClient;
	}

	@PostConstruct
	public void init(){
		DefaultExports.initialize();
	}

	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SchedulerApplication.class);
    }

	public static void main(String[] args) {
		SpringApplication.run(SchedulerApplication.class, args);
	}

}
