/**
 * 
 */
package com.shuwen.crawler.scheduler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: chenkang
 * @email: chenkang@shuwen.com
 * @date: 2017年6月13日 下午7:41:23
 */
public abstract class AbstractInterrupteThread extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractInterrupteThread.class);

	public AbstractInterrupteThread(String threadName) {
		super(threadName);
	}

	@Override
	public void run() {
		while (true) {
			if (Thread.interrupted()) {
				return;
			}

			try {
				doWork();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOG.error("interrupt:", e);
				return;
			} catch (Exception e) {
				LOG.error("error:", e);
			}
		}
	}

	public abstract void doWork() throws InterruptedException;
}
