package us.codecraft.webmagic;

import com.shuwen.crawler.common.CrawlerConstants;
import com.shuwen.crawler.common.TaskExtraField;
import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Json;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.selector.Selectable;
import us.codecraft.webmagic.utils.UrlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object storing extracted result and urls to fetch.<br>
 * Not thread safe.<br>
 * Main method： <br>
 * {@link #getUrl()} get url of current page <br>
 * {@link #getHtml()} get content of current page <br>
 * {@link #putField(String, Object)} save extracted result <br>
 * {@link #getResultItems()} get extract results to be used in
 * {@link us.codecraft.webmagic.pipeline.Pipeline}<br>
 * {@link #addTargetRequests(java.util.List)} {@link #addTargetRequest(String)}
 * add urls to fetch <br>
 *
 * @author code4crafter@gmail.com <br>
 * @see us.codecraft.webmagic.downloader.Downloader
 * @see us.codecraft.webmagic.processor.PageProcessor
 * @since 0.1.0
 */
public class Page {

    private Request request;

    private HttpResponse httpResponse;

    private ResultItems resultItems = new ResultItems();

    private Html html;

    private Json json;

    private String rawText;

    // 响应原始字节
    private byte[] rawData;

    private Selectable url;

    private Map<String,List<String>> headers;

    private WebDriver webDriver;

    private int statusCode;

    private boolean needCycleRetry;

    private List<Request> targetRequests = new ArrayList<Request>();

    private Spider spider;

    public Page() {
        //默认跳过pipeline
        setSkip(true);
        resultItems.setPage(this);
    }

    public Page setSkip(boolean skip) {
        resultItems.setSkip(skip);
        return this;
    }

    /**
     * store extract results
     *
     * @param key   key
     * @param field field
     */
    public void putField(String key, Object field) {
        setSkip(false);
        resultItems.put(key, field);
    }

    /**
     * get html content of page
     *
     * @return html
     */
    public Html getHtml() {
        if (html == null) {
            String baseUri = getHtmlBaseHref(rawText);
            if(null == baseUri || baseUri.isEmpty()){
                baseUri = request.getUrl();
            }

            html = new Html(UrlUtils.fixAllRelativeHrefs(rawText, baseUri));
        }
        return html;
    }

    /**
     * get json content of page
     *
     * @return json
     * @since 0.5.0
     */
    public Json getJson() {
        if (json == null) {
            json = new Json(rawText);
        }
        return json;
    }

    /**
     * @param html html
     * @deprecated since 0.4.0 The html is parse just when first time of calling
     * {@link #getHtml()}, so use {@link #setRawText(String)}
     * instead.
     */
    public void setHtml(Html html) {
        this.html = html;
    }

    public List<Request> getTargetRequests() {
        return targetRequests;
    }

    /**
     * add requests to fetch
     *
     * @param request request
     */
    public void addTargetRequest(Request request) {
        synchronized (targetRequests) {
            this.initRequest(request);
            targetRequests.add(request);
        }
    }

    public void addTargetRequests(List<Request> requests) {
        synchronized (targetRequests) {
            this.initRequest(requests);
            targetRequests.addAll(requests);
        }
    }

    public void addTargetRequest(String url, Map<String, Object> extras) {
        Request request = new Request(url);
        extras.put(TaskExtraField.PARENT_URL,this.url.get());//把父级url添加
        request.setExtras(extras);
        addTargetRequest(request);
    }

    public void addTargetRequest(String url) {
        addTargetRequest(url, new HashMap<>());
    }

    /**
     * get url of current page
     *
     * @return url of current page
     */
    public Selectable getUrl() {
        return url;
    }

    public void setUrl(Selectable url) {
        this.url = url;
    }

    /**
     * get request of current page
     *
     * @return request
     */
    public Request getRequest() {
        return request;
    }

    public boolean isNeedCycleRetry() {
        return needCycleRetry;
    }

    public void setNeedCycleRetry(boolean needCycleRetry) {
        this.needCycleRetry = needCycleRetry;
    }

    public void setRequest(Request request) {
        this.request = request;
        this.resultItems.setRequest(request);
    }

    public ResultItems getResultItems() {
        return resultItems;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public String getRawText() {
        return rawText;
    }

    public Page setRawText(String rawText) {
        this.rawText = rawText;
        return this;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "Page{" + "request=" + request + ", resultItems=" + resultItems + ", rawText='" + rawText + '\''
                + ", url=" + url + ", statusCode=" + statusCode + ", targetRequests=" + targetRequests + '}';
    }

    /**
     * @return the rawData
     */
    public byte[] getRawData() {
        return rawData;
    }

    /**
     * @param rawData the rawData to set
     */
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    /**
     * @return the spider
     */
    public Spider getSpider() {
        return spider;
    }

    public void setWebDriver(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public Page click(By by, int waitMills) throws InterruptedException {
        if (webDriver == null) {
            return null;
        }
        webDriver.findElement(by).click();
        if (waitMills > 0) {
            Thread.sleep(waitMills);
        }
        return getPageInDriver();
    }

    public Page sendKeys(By by, String userInput, int waitMills) throws InterruptedException {
        if (webDriver == null) {
            return null;
        }
        webDriver.findElement(by).sendKeys(userInput);
        if (waitMills > 0) {
            Thread.sleep(waitMills);
        }
        return getPageInDriver();
    }

    private Page getPageInDriver() {
        WebElement webElement = webDriver.findElement(By.xpath("/html"));
        String content = webElement.getAttribute("outerHTML");
        Page page = new Page();
        page.setRawText(content);
        page.setHtml(new Html(content, request.getUrl()));
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setWebDriver(webDriver);
        return page;
    }

    /**
     * @param spider the spider to set
     */
    public void setSpider(Spider spider) {
        this.spider = spider;
    }

    public void setResult(Map<String, Object> result) {
        setSkip(false);
        putField(CrawlerConstants.RESULT_FOR_SAVING, result);
    }

    private void initRequest(Request request){
        Map<String, Object> extras = request.getExtras();
        if(extras==null){
            extras=new HashMap<>();
        }
        extras.put(TaskExtraField.PARENT_URL,this.url.get());//把父级url添加
    }

    private void initRequest(List<Request> requests){
        for(int x=0;x<requests.size();x++){
            this.initRequest(requests.get(x));
        }
    }

    /**
     * 获取html base href
     * @param html
     * @return
     */
    public String getHtmlBaseHref(String html){
        if(null == html || html.length() <= 0){
            return  "";
        }
        Document document = Jsoup.parse(html);

        if(null!=document) {
            Elements baseItems = document.select("head > base[href]");
            for (Element base : baseItems) {
                String baseUrl = base.attr("href");
                if(null != baseUrl && !baseUrl.isEmpty()){
                    return  baseUrl;
                }
            }
        }
        return  "";
    }

    public  String getRespCookie(){
        if(null != headers){
            List<String> values = headers.get("Set-Cookie");
            if(null != values && values.size() > 0){
                int size = values.size();
                StringBuilder buf = new StringBuilder(256);
                for (int index = 0; index < size; index++) {
                    String cookie = values.get(index);
                    if(index > 0) {
                        buf.append(';');
                    }
                    int splitPos = cookie.indexOf(';');
                    if(-1 != splitPos) {
                        buf.append(cookie.substring(0,splitPos));
                    }else {
                        buf.append(cookie);
                    }
                }
                return  buf.toString();
            }
        }
        return "";
    }


}
