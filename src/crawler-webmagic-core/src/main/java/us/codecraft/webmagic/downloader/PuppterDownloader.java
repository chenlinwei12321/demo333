package us.codecraft.webmagic.downloader;

import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.LogOutputTemplate;
import com.shuwen.crawler.common.TaskExtraField;
import com.shuwen.crawler.common.monitor.DataRecordDO;
import com.shuwen.crawler.common.util.HttpClientUtil;
import com.shuwen.crawler.common.util.LocalServerUtil;
import com.shuwen.crawler.common.util.StringZip;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.UrlUtils;

public class PuppterDownloader implements Downloader {

    Logger logger = LoggerFactory.getLogger(PuppterDownloader.class);

    private static final Logger download_log = LoggerFactory.getLogger("DOWNLOAD_LOG");

    private int sleepTime = 1000;

    private String puppterServerUrl;

    public void setPuppterServerUrl(String puppterServerUrl) {
        this.puppterServerUrl = puppterServerUrl;
    }

    @Override
    public Page download(Request request, Task task) {

        logger.info("downloading page " + request.getUrl());
        //获取监控信息传递对象
        DataRecordDO dataRecordDO = (DataRecordDO) request.getExtras().get(TaskExtraField.MONITOR_RECORD);
        if(dataRecordDO == null){
            dataRecordDO = new DataRecordDO();
        }

        try {
            if (task.getSite() != null && task.getSite().getSleepTime() > 0) {
                Thread.sleep(task.getSite().getSleepTime());
            } else {
                Thread.sleep(sleepTime);
            }
            logger.info("downloaded page " + request.getUrl());
            String content = null;
            try {
                String html=HttpClientUtil.httpGet(String.format(puppterServerUrl,request.getUrl()),null,3,null);
                JSONObject json=JSONObject.parseObject(html);
                content=json.getJSONObject("data").getString("sourcecode");
            }catch (Exception e){
                onError(request);
                dataRecordDO.setStatus(1);
                String reason="请求渲染服务器失败，失败原因："+e.getMessage();
                dataRecordDO.setReason(reason);
                logger.error(reason);
                return null;
            }
            Page page = new Page();
            page.setRawText(content);
//            page.setHtml(new Html(content, request.getUrl()));
            page.setUrl(new PlainText(request.getUrl()));
            page.setRequest(request);
            onSuccess(request);
            dataRecordDO.setHttpStatus(200);
            request.putExtra(Request.STATUS_CODE, 200);
            Object proxyHost = request.getExtra(Request.PROXY);
            String source = StringZip.zipString(page.getRawText());
            if(source.length()>2097152){//ots列最多2097152 值
                source="压缩后超出ots列字符2097152最大值!";
            }
            dataRecordDO.setHtml(source);
            if(proxyHost!=null){
                dataRecordDO.setHttpHost((HttpHost) proxyHost);
            }
            return page;
        } catch (Exception e) {
            e.printStackTrace();
            onError(request);
            dataRecordDO.setStatus(1);
            dataRecordDO.setReason("动态渲染失败，失败原因："+e.getMessage());
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void setThread(int threadNum) {

    }

    /**
     * 记录日志
     * @param request
     */
    protected void onSuccess(Request request) {
        String url = request.getUrl();
        ////domain url status codestatus proxyIp parentUrl requestDepth taskVersion moduleName jobId groupId serverIp
        Object proxyHost = request.getExtra(Request.PROXY);
        download_log.info(String.format(LogOutputTemplate.DOWNLOAD_TEMPLATE
                , UrlUtils.getBaseDomain(url), url, LogOutputTemplate.DOWNLOAD_SUCCESS
                , request.getExtra(Request.STATUS_CODE),proxyHost==null?null:((HttpHost)proxyHost).getHostName(),request.getExtras().get(TaskExtraField.PARENT_URL),
                request.getExtras().get(TaskExtraField.REQUEST_DEPTH),request.getCurrentTask()==null?null:request.getCurrentTask().getTaskVersion(),
                request.getExtras().get(TaskExtraField.MODULE_NAME),request.getCurrentTask()==null?null:request.getCurrentTask().getJobId(),
                request.getExtras().get(TaskExtraField.GROUP_ID),LocalServerUtil.getLocalIp()));
    }

    /**
     * 记录日志
     * @param request
     */
    protected void onError(Request request) {
        String url = request.getUrl();
        Object proxyHost = request.getExtra(Request.PROXY);
        download_log.info(String.format(LogOutputTemplate.DOWNLOAD_TEMPLATE
                , UrlUtils.getBaseDomain(url), url, LogOutputTemplate.DOWNLOAD_FAIL
                , request.getExtra(Request.STATUS_CODE),proxyHost==null?null:((HttpHost)proxyHost).getHostName(),request.getExtras().get(TaskExtraField.PARENT_URL),
                request.getExtras().get(TaskExtraField.REQUEST_DEPTH),request.getCurrentTask()==null?null:request.getCurrentTask().getTaskVersion(),
                request.getExtras().get(TaskExtraField.MODULE_NAME),request.getCurrentTask()==null?null:request.getCurrentTask().getJobId(),
                request.getExtras().get(TaskExtraField.GROUP_ID),LocalServerUtil.getLocalIp()));
    }


}
