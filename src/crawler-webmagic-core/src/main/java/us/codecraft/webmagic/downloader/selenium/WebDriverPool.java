package us.codecraft.webmagic.downloader.selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author code4crafter@gmail.com <br>
 *         Date: 13-7-26 <br>
 *         Time: 下午1:41 <br>
 */
class WebDriverPool {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final static int DEFAULT_CAPACITY = 5;

    private final int capacity;

    private String browserPath;

    private final static int STAT_RUNNING = 1;

    private final static int STAT_CLODED = 2;

    private AtomicInteger stat = new AtomicInteger(STAT_RUNNING);

    /*
     * new fields for configuring phantomJS
     */
    private WebDriver mDriver = null;
    private boolean mAutoQuitDriver = true;
    private static final String DRIVER_FIREFOX = "firefox";
    private static final String DRIVER_CHROME = "chrome";
    private static final String DRIVER_PHANTOMJS = "phantomjs";

    protected static Properties sConfig;
    protected static DesiredCapabilities sCaps;

    /**
     * Configure the GhostDriver, and initialize a WebDriver instance. This part
     * of code comes from GhostDriver.
     * https://github.com/detro/ghostdriver/tree/master/test/java/src/test/java/ghostdriver
     *
     * @throws IOException
     * @author bob.li.0718@gmail.com
     */
    public void configure() throws IOException {
        // Read config file
        sConfig = new Properties();

        // Prepare capabilities
        sCaps = new DesiredCapabilities();
        sCaps.setJavascriptEnabled(true);
        sCaps.setCapability("takesScreenshot", false);
        String driver = "chrome";
        ArrayList<String> cliArgsCap = new ArrayList<String>();
        cliArgsCap.add("--web-security=false");
        cliArgsCap.add("--ssl-protocol=any");
        cliArgsCap.add("--ignore-ssl-errors=true");
        sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
                cliArgsCap);
        sCaps.setCapability(
                PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
                new String[]{"--logLevel="
                        + (sConfig.getProperty("phantomjs_driver_loglevel") != null ? sConfig
                        .getProperty("phantomjs_driver_loglevel")
                        : "INFO")});

        // String driver = sConfig.getProperty("driver", DRIVER_PHANTOMJS);
        if (isUrl(driver)) {
            sCaps.setBrowserName("phantomjs");
            mDriver = new RemoteWebDriver(new URL(driver), sCaps);
        } else if (driver.equals(DRIVER_FIREFOX)) {
            mDriver = new FirefoxDriver(sCaps);
        } else if (driver.equals(DRIVER_CHROME)) {
            List<String> switches = new ArrayList();
            switches.add("--ignore-certificate-errors");
            sCaps.setCapability("chrome.switches", switches);
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");
            if (this.browserPath != null) {
                options.setBinary(this.browserPath);
            }
            sCaps.setCapability(ChromeOptions.CAPABILITY, options);
            mDriver = new ChromeDriver(sCaps);
        } else if (driver.equals(DRIVER_PHANTOMJS)) {
            mDriver = new PhantomJSDriver(sCaps);
        }
    }

    /**
     * check whether input is a valid URL
     *
     * @param urlString urlString
     * @return true means yes, otherwise no.
     * @author bob.li.0718@gmail.com
     */
    private boolean isUrl(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException mue) {
            return false;
        }
    }

    /**
     * store webDrivers created
     */
    private List<WebDriver> webDriverList = Collections
            .synchronizedList(new ArrayList<WebDriver>());

    /**
     * store webDrivers available
     */
    private BlockingDeque<WebDriver> innerQueue = new LinkedBlockingDeque<WebDriver>();

    public WebDriverPool(int capacity) {
        this.capacity = capacity;
    }

    public WebDriverPool(int capacity, String browserPath) {
        this.capacity = capacity;
        this.browserPath = browserPath;
    }

    public WebDriverPool() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * @return
     * @throws InterruptedException
     */
    public WebDriver get() throws InterruptedException {
        checkRunning();
        WebDriver poll = innerQueue.poll();
        if (poll != null) {
            return poll;
        }
        //if (webDriverList.size() < capacity) {
            //synchronized (webDriverList) {
                if (webDriverList.size() < capacity) {

                    // add new WebDriver instance into pool
                    try {
                        configure();
                        innerQueue.add(mDriver);
                        webDriverList.add(mDriver);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            //}
        //}
        return innerQueue.take();
    }

    public void removeFromPool(WebDriver webDriver) {
        if (webDriver == null) return;
        webDriverList.remove(webDriver);
        webDriver.close();
        webDriver.quit();
    }

    public void returnToPool(WebDriver webDriver) {
        checkRunning();
        innerQueue.add(webDriver);
    }

    protected void checkRunning() {
        if (!stat.compareAndSet(STAT_RUNNING, STAT_RUNNING)) {
            throw new IllegalStateException("Already closed!");
        }
    }

    public void closeAll() {
        boolean b = stat.compareAndSet(STAT_RUNNING, STAT_CLODED);
        if (!b) {
            throw new IllegalStateException("Already closed!");
        }
        for (WebDriver webDriver : webDriverList) {
            logger.info("Quit webDriver" + webDriver);
            webDriver.quit();
            webDriver = null;
        }
    }

}