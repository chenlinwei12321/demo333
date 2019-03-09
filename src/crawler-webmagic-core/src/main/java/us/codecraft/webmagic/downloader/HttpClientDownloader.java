package us.codecraft.webmagic.downloader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

import com.shuwen.crawler.common.LogOutputTemplate;
import com.shuwen.crawler.common.MonitorFailedReason;
import com.shuwen.crawler.common.TaskExtraField;
import com.shuwen.crawler.common.monitor.DataRecordDO;
import com.shuwen.crawler.common.util.LocalServerUtil;
import com.shuwen.crawler.common.util.StringZip;
import com.virjar.dungproxy.client.util.PoolUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.ChallengeState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.HttpConstant;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * The http downloader based on HttpClient.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public class HttpClientDownloader extends AbstractDownloader {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientDownloader.class);

	private static final Logger download_log = LoggerFactory.getLogger("DOWNLOAD_LOG");

	private final Map<String, CloseableHttpClient> httpClients = new HashMap<String, CloseableHttpClient>();

	private HttpClientGenerator httpClientGenerator = new HttpClientGenerator();

	//重定向默认深度为5层
	private static final Integer DEFAULT_REDIRECT_TIMES=5;
	//
	private static final String XHZY_STATIC_PROXY_USERNAME="cj95m";
	private static final String XHZY_STATIC_PROXY_PASSWORD="cj95m";

	private static final String REDIRECT_URL_FLAG="redirect=true";

	private CloseableHttpClient getHttpClient(Site site) {
		if (site == null) {
			return httpClientGenerator.getClient(null);
		}
		String domain = site.getDomain();
		CloseableHttpClient httpClient = httpClients.get(domain);
		if (httpClient == null) {
			synchronized (this) {
				httpClient = httpClients.get(domain);
				if (httpClient == null) {
					httpClient = httpClientGenerator.getClient(site);
					httpClients.put(domain, httpClient);
				}
			}
		}
		return httpClient;
	}

	/**
	 * 如果设置了代理ip的账号密码。则使用，否则使用默认的。
	 * @param request
	 * @param httpClientContext
	 */
	private void setProxyUserNamePasswordInfo(Request request,HttpClientContext httpClientContext){
		//使用调度过来的代理服务
		if(request.getCurrentTask()!=null&&request.getCurrentTask().getExtras()!=null&&request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY)!=null&&Integer.parseInt(request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY).toString())==1){
			Object username = request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY_USERNAME);
			Object password = request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY_PASSWORD);
			//买的代理ip地址，需要账号密码，如果代理服务不需要的话，设置也不影响
			AuthState authState = new AuthState();
			if(username==null||password==null){
				authState.update(new BasicScheme(ChallengeState.PROXY), new UsernamePasswordCredentials(XHZY_STATIC_PROXY_USERNAME,XHZY_STATIC_PROXY_PASSWORD));
			}else{
				authState.update(new BasicScheme(ChallengeState.PROXY), new UsernamePasswordCredentials(username.toString(),password.toString()));
			}
			httpClientContext.setAttribute(HttpClientContext.PROXY_AUTH_STATE, authState);
		}
	}

	@Override
	public Page download(Request request, Task task) {
		//获取监控信息传递对象
		DataRecordDO dataRecordDO = (DataRecordDO) request.getExtras().get(TaskExtraField.MONITOR_RECORD);
		if(dataRecordDO == null){
			dataRecordDO = new DataRecordDO();
		}

		Site site = null;
		if (task != null) {
			site = task.getSite();
		}
		Set<Integer> acceptStatCode;
		String charset = null;
		Map<String, String> headers = null;
		if (site != null) {
			acceptStatCode = site.getAcceptStatCode();
			charset = site.getCharset();
			headers = site.getHeaders();
		} else {
			acceptStatCode = Sets.newHashSet(200,301,302);
		}
		//字符集，通过 extras 来扩展
		String requestCharset = request.getCharset();
		if(null != requestCharset &&!"".equals(requestCharset.trim())){
			charset = requestCharset;
		}

		logger.debug("downloading page {}", request.getUrl());
		CloseableHttpResponse httpResponse = null;
		int statusCode = 0;
		long start = System.currentTimeMillis();
		HttpClientContext httpClientContext = null;
		HttpUriRequest httpUriRequest = null;

		try {
			httpUriRequest = getHttpUriRequest(request, site, headers);
			httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
			//买的代理ip地址，需要账号密码，如果代理服务不需要的话，设置也不影响
			setProxyUserNamePasswordInfo(request,httpClientContext);
			httpResponse = getHttpClient(site).execute(httpUriRequest, httpClientContext);
			statusCode = httpResponse.getStatusLine().getStatusCode();

			dataRecordDO.setHttpStatus(statusCode);

			request.putExtra(Request.STATUS_CODE, statusCode);

			Object proxyHost = request.getExtra(Request.PROXY);
			if(proxyHost!=null){
				dataRecordDO.setHttpHost((HttpHost) proxyHost);
			}

			if (statusAccept(acceptStatCode, statusCode)) {
				Page page = handleResponse(request, charset, httpResponse, task);
				//html 方式的重定向
				Object isMetaRedirect = request.getExtra(Request.META_REDIRECT);
				if(request.getUrl().contains(REDIRECT_URL_FLAG)||(isMetaRedirect!=null&&(Boolean)isMetaRedirect)) {
					this.finallyClose(request,httpUriRequest,httpResponse,dataRecordDO,statusCode);
					page = this.htmlRedirect(request, page, task,httpUriRequest,httpResponse,dataRecordDO,statusCode);
				}
				String source = StringZip.zipString(page.getRawText());

				if(source.length()>2097152){//ots列最多2097152 值
					source="压缩后超出ots列字符2097152最大值!";
				}

				dataRecordDO.setHtml(source);
				onSuccess(request);

				return page;
			} else {
				onError(request);
				logger.warn("get page {} error, status code {} ", request.getUrl(), statusCode);

				if (needOfflineProxy(statusCode)) {
					logger.warn("statusCode异常:{},IP下线",statusCode);
					PoolUtil.offline(httpClientContext);// webMagic对状态码的拦截可能出现在这里,所以也要在这里下线IP
					return addToCycleRetry(request, site);
				}

				dataRecordDO.setStatus(1);
				dataRecordDO.setReason(MonitorFailedReason.HTTP_STATUS_ERROR);

				return null;
			}
		} catch (IOException e) {
			long d = System.currentTimeMillis() - start;
			logger.warn("download page " + request.getUrl() + " error:" + d +" " + e.getMessage());

			if (needOfflineProxy(e)) {
				logger.warn("发生异常:{},IP下线",e);
				PoolUtil.offline(httpClientContext);// 由IP异常导致,直接重试
				return addToCycleRetry(request, site);
			}

			if (isLastRetry(request, site)) {// 移动异常日志位置,只记录最终失败的。中途失败不算失败
				logger.warn("download page {} error", request.getUrl(), e);
			}

			if (site != null && site.getCycleRetryTimes() > 0) {
//				dataRecordDO.setCanSave(true);
				dataRecordDO.setStatus(1);
				dataRecordDO.setReason("重试,剩余次数:"+site.getCycleRetryTimes());
				return addToCycleRetry(request, site);
			}

			dataRecordDO.setStatus(1);
			dataRecordDO.setReason(MonitorFailedReason.TIME_OUT);

			request.putExtra(Request.ERR_MSG,e.getMessage());
			onError(request);
			return null;
		} catch (Exception e){
			request.putExtra(Request.ERR_MSG,e.getMessage());
			onError(request);
			return null;
		} finally {
			this.finallyClose(request,httpUriRequest,httpResponse,dataRecordDO,statusCode);
		}
	}

	private void finallyClose(Request request,HttpUriRequest httpUriRequest,CloseableHttpResponse httpResponse,DataRecordDO dataRecordDO,int statusCode){
		request.putExtra(Request.STATUS_CODE, statusCode);
		try {
			// 先释放链接,在consume,consume本身会释放链接,但是可能提前抛错导致链接释放失败
			if (httpUriRequest != null) {
				try {
					httpUriRequest.abort();
				} catch (UnsupportedOperationException unsupportedOperationException) {
					logger.error("can not abort connection", unsupportedOperationException);
				}
			}

			if (httpResponse != null) {
				// ensure the connection is released back to pool
				EntityUtils.consume(httpResponse.getEntity());
			}
		} catch (IOException e) {
			dataRecordDO.setStatus(1);
			dataRecordDO.setReason(MonitorFailedReason.CONNECTION_CLOSE_ERROR);
			logger.warn("close response fail", e);
		}
	}

	/***
	 * 页面重定向逻辑，每次下载需要close一下。
	 * @param request
	 * @param page
	 * @param task
	 * @param httpUriRequest
	 * @param httpResponse
	 * @param dataRecordDO
	 * @param statusCode
	 * @return
	 */
	private Page htmlRedirect(Request request,Page page,Task task,HttpUriRequest httpUriRequest,CloseableHttpResponse httpResponse,DataRecordDO dataRecordDO,int statusCode){
		Elements meta = null;
		try {
			meta=page.getHtml().getDocument().getElementsByTag("META");
			if(meta!=null&&meta.size()!=0){
				for(int x=0;x<meta.size();x++){
					Element temp = meta.get(x);
					//< meta http-equiv="Refresh" content="秒数; url=跳转的文件或地址" >
					if((temp.hasAttr("HTTP-EQUIV")||temp.hasAttr("http-equiv"))&&("REFRESH".equals(temp.attr("HTTP-EQUIV").toUpperCase())||"REFRESH".equals(temp.attr("http-equiv").toUpperCase()))&&(temp.hasAttr("content")||temp.hasAttr("CONTENT"))){
						String[] contents = temp.attr("content").split(";");
						if(contents.length!=2){
							contents=temp.attr("CONTENT").split(";");
						}
						if(contents.length==2){
							String redirectUrl=contents[1].split("=")[1];
							String absoluteUrl=UrlUtils.relativeToAbsolute(request.getUrl(),redirectUrl);
							Integer times;
							if(request.getExtras().containsKey(Request.REDIRECT_TIMES)){
								times= (Integer) request.getExtras().get(Request.REDIRECT_TIMES);
							}else{
								times=1;
							}
							if(times<=DEFAULT_REDIRECT_TIMES){//深度为5
								request.setUrl(absoluteUrl);
								request.getExtras().put(Request.REDIRECT_TIMES,times++);
								request.getExtras().put(Request.META_REDIRECT,true);
								return this.download(request,task);
							}
						}
					}
				}
			}
		}catch (Exception e){
			logger.error("redirect error,cause by:"+e.getMessage(),e);
		}finally {
			this.finallyClose(request,httpUriRequest,httpResponse,dataRecordDO,statusCode);
		}
		return page;
	}

	/**
	 * 判断当前请求是不是最后的重试,流程等同于 addToCycleRetry
	 *
	 * @see us.codecraft.webmagic.downloader.AbstractDownloader#addToCycleRetry(us.codecraft.webmagic.Request,
	 *      us.codecraft.webmagic.Site)
	 * @param request request
	 * @param site site
	 * @return 是否是最后一次重试
	 */
	protected boolean isLastRetry(Request request, Site site) {
		Object cycleTriedTimesObject = request.getExtra(Request.CYCLE_TRIED_TIMES);
		if (cycleTriedTimesObject == null) {
			return false;
		} else {
			int cycleTriedTimes = (Integer) cycleTriedTimesObject;
			cycleTriedTimes++;
			if (cycleTriedTimes >= site.getCycleRetryTimes()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 默认封禁403和401两个状态码的IP
	 *
	 * @param page 爬取结果
	 * @return 是否需要封禁这个IP
	 */
	protected boolean needOfflineProxy(Page page) {
		Integer statusCode = (Integer) page.getRequest().getExtra(Request.STATUS_CODE);
		if (statusCode == null) {
			return false;// 不知道状态码
		}
		return statusCode == 401 || statusCode == 403;// 401和403两个状态强制下线IP
	}

	protected boolean needOfflineProxy(IOException e) {
		return false;
	}

	protected boolean needOfflineProxy(int statusCode) {
		return statusCode == 401 || statusCode == 403;
	}

	@Override
	public void setThread(int thread) {
		httpClientGenerator.setPoolSize(thread);
	}

	protected boolean statusAccept(Set<Integer> acceptStatCode, int statusCode) {
		return acceptStatCode.contains(statusCode);
	}

	protected HttpUriRequest getHttpUriRequest(Request request, Site site, Map<String, String> headers)
			throws MalformedURLException {
		RequestBuilder requestBuilder = selectRequestMethod(request).setUri(com.gargoylesoftware.htmlunit.util.UrlUtils
				.encodeUrl(new URL(request.getUrl()), false, "utf-8").toString());
		JSONObject requestHeaders = request.getHeaders();
		if(requestHeaders==null){
			requestHeaders = new JSONObject();
		}
		if(headers == null){
			headers = new HashMap<>();
		}
		Map<String, String> addHeaders = new HashMap<>();
		for(Map.Entry<String, Object> entry: requestHeaders.entrySet()){
			Object value = entry.getValue();
			if(value != null){
				addHeaders.put(entry.getKey(), value.toString());
			}
		}
		headers.putAll(addHeaders);
		for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
			requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
		}
		// setConnectTimeout：设置连接超时时间，单位毫秒。
		// setConnectionRequestTimeout：设置从connect Manager获取Connection
		// 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
		// setSocketTimeout：请求获取数据的超时时间，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。

		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
				.setConnectionRequestTimeout(site.getTimeOut()).setSocketTimeout(site.getTimeOut())
				.setConnectTimeout(site.getTimeOut()).setCookieSpec(CookieSpecs.BEST_MATCH);

//		HttpHost proxy = new HttpHost("103.28.205.84", 888);
//		HttpHost proxy = new HttpHost("23.106.132.51", 8888);
//		request.putExtra(Request.PROXY, proxy);
//		requestConfigBuilder.setProxy(proxy);

		//使用调度过来的代理服务
		if(request.getCurrentTask()!=null&&request.getCurrentTask().getExtras()!=null&&request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY)!=null&&Integer.parseInt(request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY).toString())==1){
			String ip = request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY_IP).toString();
			Integer port = Integer.parseInt(request.getCurrentTask().getExtras().get(TaskExtraField.USE_PROXY_PORT).toString());
			HttpHost httpHost = new HttpHost(ip, port);
			request.putExtra(Request.PROXY, httpHost);
			requestConfigBuilder.setProxy(httpHost);
		}
		requestBuilder.setConfig(requestConfigBuilder.build());
		return requestBuilder.build();
	}

	protected RequestBuilder selectRequestMethod(Request request) {
		String method = request.getMethod();
		if (method == null || method.equalsIgnoreCase(HttpConstant.Method.GET)) {
			// default get
			return RequestBuilder.get();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.POST)) {
			return addFormParams(RequestBuilder.post(),request);
		} else if (method.equalsIgnoreCase(HttpConstant.Method.HEAD)) {
			return RequestBuilder.head();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.PUT)) {
			return RequestBuilder.put();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.DELETE)) {
			return RequestBuilder.delete();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.TRACE)) {
			return RequestBuilder.trace();
		}
		throw new IllegalArgumentException("Illegal HTTP Method " + method);
	}

	private RequestBuilder addFormParams(RequestBuilder requestBuilder, Request request) {
		NameValuePair[] nameValuePair = null;
		Object nv = request.getExtra(TaskExtraField.REQUEST_PARAM);
		if (nv != null) {
			if (nv instanceof JSONArray) {
				List<NameValuePair> list = Lists.newArrayList();
				for (Object obj : ((JSONArray) nv)) {
					JSONObject jo = (JSONObject) obj;
					list.add(new BasicNameValuePair(jo.getString("name"), jo.getString("value")));
				}
				//nameValuePair = list.toArray(new NameValuePair[0]);
				nameValuePair = list.toArray(new NameValuePair[list.size()]);
			} else {
				nameValuePair = (NameValuePair[]) nv;
			}
		}
		if (nameValuePair != null && nameValuePair.length > 0) {
			requestBuilder.addParameters(nameValuePair);
		}

		JSONObject  dataObj = request.getRequestBody();
		if(null != dataObj){
			String contentType = dataObj.getString("contentType");
			String encoding = dataObj.getString("encoding");
			String body     = dataObj.getString("body");
			if(null!=contentType && null!=encoding && null != body) {
				try {
					ByteArrayEntity entity = new ByteArrayEntity(body.getBytes(encoding));
					entity.setContentType(contentType);
					requestBuilder.setEntity(entity);
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException("illegal encoding " + encoding, e);
				}
			}
		}

		return requestBuilder;
	}

	protected Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task) throws IOException {
		byte[] bytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
		String contentType = httpResponse.getEntity().getContentType() == null ? "" : httpResponse.getEntity().getContentType().getValue();
		Page page = new Page();
		page.setRawData(bytes);
		if (!request.isBinaryContent()){
			if (charset == null) {
				charset = getHtmlCharset(contentType, bytes);
			}
			if(charset == null) {
				charset = "UTF-8";
			}else {
				if (StringUtils.equalsIgnoreCase(charset, "gbk2312")||StringUtils.equalsIgnoreCase(charset, "gb-2312")) {
					charset = "gbk";
				}
				if(StringUtils.equalsIgnoreCase(charset, "gb2312")){
					charset = "gbk";
				}
			}

			page.setRawText(new String(bytes, charset));
		}
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);
		page.setHeaders(convertHeaders(httpResponse.getAllHeaders()));
		page.setHttpResponse(httpResponse);
		page.setStatusCode(httpResponse.getStatusLine().getStatusCode());

		return page;
	}

	/**
	 * 新增方法
	 * @param headers
	 * @return
	 */
	public static Map<String,List<String>> convertHeaders(Header[] headers){
		Map<String,List<String>> results = new HashMap<String, List<String>>();
		for (Header header : headers) {
			List<String> list = results.get(header.getName());
			if (list == null) {
				list = new ArrayList<String>();
				results.put(header.getName(), list);
			}
			list.add(header.getValue());
		}
		return results;
	}
	/*

	protected String getContent(String charset, HttpResponse httpResponse) throws IOException {
		if (charset == null) {
			byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
			String htmlCharset = getHtmlCharset(httpResponse, contentBytes);
			if (htmlCharset != null) {
				if (StringUtils.equalsIgnoreCase(htmlCharset, "gbk2312")||StringUtils.equalsIgnoreCase(htmlCharset, "gb-2312")) {
					htmlCharset = "gbk";
				}
				if(StringUtils.equalsIgnoreCase(htmlCharset, "gb2312")){
					htmlCharset = "gbk";
				}
				return new String(contentBytes, htmlCharset);
			} else {
				return new String(contentBytes, "UTF-8");
			}
		} else {
			return IOUtils.toString(httpResponse.getEntity().getContent(), charset);
		}
	}
	*/

	protected String getHtmlCharset(String  contentType, byte[] contentBytes) throws IOException {
		String charset;
		// charset
		// 1、encoding in http header Content-Type
		//String value = httpResponse.getEntity().getContentType().getValue();
		charset = UrlUtils.getCharset(contentType);
		if (StringUtils.isNotBlank(charset)) {
			logger.debug("Auto get charset: {}", charset);
			return charset;
		}
		// use default charset to decode first time
		Charset defaultCharset = Charset.defaultCharset();
		String content = new String(contentBytes, defaultCharset.name());
		// 2、charset in meta
		if (StringUtils.isNotEmpty(content)) {
			Document document = Jsoup.parse(content);
			Elements links = document.select("meta");
			for (Element link : links) {
				// 2.1、html4.01 <meta http-equiv="Content-Type"
				// content="text/html; charset=UTF-8" />
				String metaContent = link.attr("content");
				String metaCharset = link.attr("charset");
				if (metaContent.indexOf("charset") != -1) {
					metaContent = metaContent.substring(metaContent.indexOf("charset"), metaContent.length());
					charset = metaContent.split("=")[1];
					break;
				}
				// 2.2、html5 <meta charset="UTF-8" />
				else if (StringUtils.isNotEmpty(metaCharset)) {
					charset = metaCharset;
					break;
				}
			}
		}
		logger.debug("Auto get charset: {}", charset);
		// 3、todo use tools as cpdetector for content decode
		return charset;
	}

	@Override
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

	@Override
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
