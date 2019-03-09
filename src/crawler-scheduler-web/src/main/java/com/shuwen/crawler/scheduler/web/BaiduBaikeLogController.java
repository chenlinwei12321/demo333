/**
 * 
 */
package com.shuwen.crawler.scheduler.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.tomcat.util.http.fileupload.IOUtils;
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
@RequestMapping("/baiduBaikeLog")
public class BaiduBaikeLogController {
	private static final Logger LOG = LoggerFactory.getLogger(BaiduBaikeLogController.class);

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
				String file = System.getProperty("user.home") + "/baike_logo.txt";
				process(file);
			}
		};
		t.setDaemon(true);
		t.start();
		return "starting....";
	}

	private void process(String filePath) {
		try {
			String encoding = "UTF-8";
			File file = new File(filePath);
			if (file.isFile() && file.exists()) { // 判断文件是否存在
				LOG.info("baiduBaikeLog start");
				InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格式
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;

				TaskDO task = new TaskDO();
				task.setJobId(-1);
				task.setJobType((byte) 0);
				task.setJobUrl("http://d.hiphotos.baidu.com/baike/");
				long cnt = 0;
				while ((lineTxt = bufferedReader.readLine()) != null) {
					Map<String, Object> map = Maps.newHashMap();
					map.put("data", lineTxt);
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
				IOUtils.closeQuietly(bufferedReader);
				IOUtils.closeQuietly(read);
				LOG.info("baiduBaikeLog end");
			} else {
				LOG.info("找不到指定的文件");
			}
		} catch (Exception e) {
			LOG.error("读取文件内容出错", e);
		}
		this.t = null;
	}
}
