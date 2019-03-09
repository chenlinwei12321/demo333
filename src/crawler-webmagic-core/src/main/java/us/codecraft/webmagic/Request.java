package us.codecraft.webmagic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.TaskExtraField;
import com.shuwen.crawler.common.task.pojo.TaskDO;

import us.codecraft.webmagic.utils.Experimental;

/**
 * Object contains url to crawl.<br>
 * It contains some additional information.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 2062192774891352043L;

    public static final String CYCLE_TRIED_TIMES = "_cycle_tried_times";
    public static final String STATUS_CODE = "statusCode";
    public static final String PROXY = "proxy";
    public static final String META_REDIRECT = "_is_meta_redirect_";
    public static final String REDIRECT_TIMES = "_redirect_times_";
    public static final String ADD_HEADERS = "__headers__";
    public static final String ERR_MSG = "errMessage";
    public static final String REQUEST_BODY ="_request_body_";

    public static abstract class ContentType {
        public static final String JSON = "application/json";
        public static final String XML = "text/xml";
        public static final String FORM = "application/x-www-form-urlencoded";
        public static final String MULTIPART = "multipart/form-data";
    }

    private String url;

    private String method;

    /**
     * When it is set to TRUE, the downloader will not try to parse response body to text.
     *
     */
    private boolean binaryContent = false;
    /**
     * Store additional information in extras.
     */
    private Map<String, Object> extras = new HashMap<String, Object>();

    private TaskDO currentTask;

    public Request() {
    }

    public Request(String url) {
        this.url = url;
    }

    public Object getExtra(String key) {
        if (extras == null) {
            return null;
        }
        return extras.get(key);
    }

    public Request putExtra(String key, Object value) {
        if (extras == null) {
            extras = new HashMap<String, Object>();
        }
        extras.put(key, value);
        return this;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Request request = (Request) o;

        if (!url.equals(request.url)) {
            return false;
        }
        return true;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    public Request setExtras(Map<String, Object> extras) {
        this.extras = extras;
        return this;
    }

    public Request copyExtras(Request request) {
        Map<String, Object> fromExtras = request.getExtras();
        for (Map.Entry<String, Object> entry : fromExtras.entrySet()) {
            putExtra(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Request setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * The http method of the request. Get for default.
     *
     * @return httpMethod
     * @see us.codecraft.webmagic.utils.HttpConstant.Method
     * @since 0.5.0
     */
    public String getMethod() {
        return method;
    }

    public Request setMethod(String method) {
        this.method = method;
        return  this;
    }

    public Request addHeader(String name,String value){
        if (extras == null) {
            extras = new HashMap<String, Object>();
        }
        Object obj = extras.get(Request.ADD_HEADERS);
        if(null == obj){
            JSONObject headers = new JSONObject();
            headers.put(name,value);
            extras.put(Request.ADD_HEADERS,headers);
        }else {
            JSONObject headers = (JSONObject) obj;
            headers.put(name,value);
        }

        return  this;
    }

    public String getHeader(String key){
        if (extras == null) {
            return null;
        }

        Object obj =  extras.get(Request.ADD_HEADERS);
        if(null == obj){
            return null;
        }
        JSONObject jsonObj = (JSONObject)obj;
        return  jsonObj.getString(key);
    }

    public JSONObject getHeaders(){
        if (extras == null) {
            return null;
        }
        Object obj =  extras.get(Request.ADD_HEADERS);
        if(null == obj){
            return null;
        }
        return  (JSONObject)obj;
    }

    public Request addFromData(String params, String encoding){
        addPostData(params,ContentType.FORM,encoding);
        return this;
    }

    public Request addJsonData(String json,String encoding){
        addPostData(json,ContentType.JSON,encoding);
        return  this;
    }

    public Request addMultipartData(String data, String encoding){
        addPostData(data,ContentType.MULTIPART,encoding);
        return  this;
    }

    public Request addXmlData(String xml, String encoding){
        addPostData(xml,ContentType.XML,encoding);
        return  this;
    }

    public Request addPostData(String data,String contentType, String encoding){
        if (extras == null) {
            extras = new HashMap<String, Object>();
        }

        Object obj = extras.get(Request.REQUEST_BODY);

        if(null == obj){
            JSONObject requestBody = new JSONObject();
            requestBody.put("contentType",contentType);
            requestBody.put("encoding",encoding);
            requestBody.put("body",data);
            extras.put(Request.REQUEST_BODY,requestBody);
        }else {
            JSONObject requestBody = (JSONObject)obj;
            requestBody.put("contentType",contentType);
            requestBody.put("encoding",encoding);
            requestBody.put("body",data);
        }

        return  this;
    }

    public JSONObject getRequestBody(){
        if (extras == null) {
            return null;
        }
        Object obj =  extras.get(Request.REQUEST_BODY);
        if(null == obj){
            return  null;
        }
        return (JSONObject)obj;
    }

    public Request setCharset(String charset){
        if(null != charset && !charset.isEmpty()) {
            if (extras == null) {
                extras = new HashMap<String, Object>();
            }
            extras.put(TaskExtraField.CHARSET, charset);
        }

        return this;
    }

    public String getCharset(){
        if (extras == null) {
            return null;
        }
        Object obj = extras.get(TaskExtraField.CHARSET);
        if(null == obj){
            return null;
        }
        return  obj.toString();
    }

    public boolean isBinaryContent() {
        return binaryContent;
    }

    public Request setBinaryContent(boolean binaryContent) {
        this.binaryContent = binaryContent;
        return this;
    }

    @Override
    public String toString() {
        return "Request{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", extras=" + extras +
                '}';
    }

    /**
     * @return the currentTask
     */
    public TaskDO getCurrentTask() {
        return currentTask;
    }

    /**
     * @param currentTask the currentTask to set
     */
    public void setCurrentTask(TaskDO currentTask) {
        this.currentTask = currentTask;
    }
}
