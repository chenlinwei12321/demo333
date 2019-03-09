/**
 * 
 */
package com.shuwen.crawler.scheduler.web;

import java.io.File;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.core.mq.MQProducer;

/**
 * @author: chenkang
 * @email: chenkang@shuwen.com
 * @date: 2017年7月8日 下午2:44:47
 */
@RestController
@RequestMapping("/bingLogo")
public class BingLogoController {
	private static final Logger LOG = LoggerFactory.getLogger(BingLogoController.class);

	@Autowired
	private MQProducer mQProducer;

	private Thread t;

	@RequestMapping("/submit")
	public synchronized String submit() {
		if (t != null) {
			return "has exist task";
		}
		t = new Thread() {
			@Override
			public void run() {
				process();
			}
		};
		t.setDaemon(true);
		t.start();
		return "starting....";
	}

	private void process() {
		try {// 词条文件
			@SuppressWarnings("unchecked")
			List<String> list = FileUtils.readLines(new File(BingLogoController.class.getClassLoader()
					.getResource("imagenet_synset_to_human_label_map.txt").getFile()));

			int i = 0;
			long cnt = 0;
			for (String line : list) {
				i++;
				String[] splits = line.split("\t");
				String id = splits[0];
				String[] words = splits[1].split(", ");
				for (int j = 0; j < words.length; j++) {
					String word = URLEncoder.encode(words[j], "utf8");
					String url = 
							"http://www.bing.com/images/async?async=content&q=" + word + "+logo&first=0&count=100";
					
					TaskDO task = new TaskDO();
					task.setJobId(-1);
					task.setJobType((byte) 0);
					task.setJobUrl(url);
					Map<String, Object> map = Maps.newHashMap();
					map.put("nodeId", id);
					map.put("word", word);

					task.setExtras(map);
					try {
						mQProducer.send(JSON.toJSONString(task), null, null);
					} catch (Exception e) {
						LOG.info("baiduBaikeLogError", e);
					}
					cnt++;
					if (cnt % 1000 == 0) {
						LOG.info("baiduBaikeLog:" + cnt);
					}
				}
			}
			LOG.info("bingLogoEnd:" + i + "," + cnt);
		} catch (Exception e) {
			LOG.error("读取文件内容出错", e);
		}
		this.t = null;
	}
}
