/**
 * 
 */
package com.shuwen.crawler.scheduler.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: chenkang
 * @email: chenkang@shuwen.com
 * @date: 2017年7月8日 下午2:44:47
 */
@RestController
@RequestMapping("/hello")
public class HelloController {
	private static final Logger LOG = LoggerFactory.getLogger(HelloController.class);


	@RequestMapping("/hello")
	public synchronized String submit() {
		return "starting....";
	}

}
