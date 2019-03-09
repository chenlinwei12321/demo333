package us.codecraft.webmagic.downloader.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.PlainText;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 使用Selenium调用浏览器进行渲染。目前仅支持chrome。<br>
 * 需要下载Selenium driver支持。<br>
 *
 * @author code4crafter@gmail.com <br>
 *         Date: 13-7-26 <br>
 *         Time: 下午1:37 <br>
 */
public class SeleniumDownloader implements Downloader, Closeable {

    private volatile WebDriverPool webDriverPool;

    Logger logger = LoggerFactory.getLogger(SeleniumDownloader.class);

    private int sleepTime = 1000;

    private int poolSize = 2;

    private String browserPath;

    /**
     * 新建
     *
     * @param chromeDriverPath chromeDriverPath
     */
    public SeleniumDownloader(String chromeDriverPath) {
        System.getProperties().setProperty("webdriver.chrome.driver", chromeDriverPath);
    }

    /***
     * 配置文件通过参数传入进来
     * @param chromeDriverPath
     * @param browserPath
     */
    public SeleniumDownloader(String chromeDriverPath, String browserPath) {
        System.getProperties().setProperty("webdriver.chrome.driver", chromeDriverPath);
        this.browserPath = browserPath;
    }

    /**
     * Constructor without any filed. Construct PhantomJS browser
     *
     * @author bob.li.0718@gmail.com
     */
    public SeleniumDownloader() {
        //URL resource = this.getClass().getResource("/script/chromedriver");
        String driverPath = "/bin/chromedriver";
        this.browserPath = "/bin/google-chrome-stable";
        File driverFile = new File(driverPath);
        if (driverFile.exists()) {
            logger.info("webdriver.chrome.driver path:" + driverPath);//在服务器使用的地址g
            System.getProperties().setProperty("webdriver.chrome.driver", driverPath);
        } else {
            driverPath = "/data/chrome/chromedriver";//在本地调试的地址
            this.browserPath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            logger.info("webdriver.chrome.driver path:" + driverPath);
            System.getProperties().setProperty("webdriver.chrome.driver", driverPath);
        }
    }

    /**
     * set sleep time to wait until load success
     *
     * @param sleepTime sleepTime
     * @return this
     */
    public SeleniumDownloader setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
        return this;
    }

    @Override
    public Page download(Request request, Task task) {
        logger.info("downloading page " + request.getUrl());
        checkInit();
        try {
            WebDriver webDriver = requestDriver(request);
            if (webDriver == null && task.getSite() != null && task.getSite().getRetryTimes() > 0) {
                for (int i = 0; i < task.getSite().getRetryTimes(); i++) {
                    webDriver = requestDriver(request);
                    if (webDriver != null) {
                        break;
                    }
                }
            }
            if (task.getSite() != null && task.getSite().getSleepTime() > 0) {
                Thread.sleep(task.getSite().getSleepTime());
            } else {
                Thread.sleep(sleepTime);
            }
            if (webDriver == null) return null;
            WebDriver.Options manage = webDriver.manage();
            Site site = task.getSite();
            if (site.getCookies() != null) {
                for (Map.Entry<String, String> cookieEntry : site.getCookies()
                        .entrySet()) {
                    Cookie cookie = new Cookie(cookieEntry.getKey(),
                            cookieEntry.getValue());
                    manage.addCookie(cookie);
                }
            }
            logger.info("downloaded page " + request.getUrl());
            WebElement webElement = webDriver.findElement(By.xpath("/html"));
            String content = webElement.getAttribute("outerHTML");
            Page page = new Page();
            page.setRawText(content);
            page.setHtml(new Html(content, request.getUrl()));
            page.setUrl(new PlainText(request.getUrl()));
            page.setRequest(request);
            page.setWebDriver(webDriver);
            //webDriverPool.returnToPool(webDriver);
            return page;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private WebDriver requestDriver(Request request) {
        WebDriver webDriver = null;
        try {
            webDriver = webDriverPool.get();
            webDriver.manage().timeouts().pageLoadTimeout(180, TimeUnit.SECONDS);
            logger.info("Selenium webDriver get:" + webDriver);
            webDriver.get(request.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            if (webDriver != null && webDriverPool != null) {
                webDriverPool.removeFromPool(webDriver);
            }
            webDriver = null;
        }
        return webDriver;
    }


    private void checkInit() {
        if (webDriverPool == null) {
            synchronized (this) {
                webDriverPool = new WebDriverPool(poolSize, browserPath);
            }
        }
    }

    public void returnToPool(WebDriver webDriver) {
        webDriverPool.returnToPool(webDriver);
    }

    @Override
    public void setThread(int thread) {
        this.poolSize = thread;
    }

    @Override
    public void close() throws IOException {
        webDriverPool.closeAll();
    }
}
