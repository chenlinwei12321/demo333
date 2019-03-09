package us.codecraft.webmagic;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.shuwen.crawler.common.TaskExtraField;
import com.shuwen.crawler.common.task.dao.ScheduledTaskDAO;
import com.shuwen.crawler.common.task.service.impl.TaskDuplicateRemover;
import com.shuwen.crawler.common.util.ExtrasUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.shuwen.crawler.common.task.pojo.TaskDO;
import com.shuwen.crawler.common.task.service.TaskService;

import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.downloader.selenium.SeleniumDownloader;
import us.codecraft.webmagic.pipeline.CollectorPipeline;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.pipeline.ResultItemsCollectorPipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.Scheduler;
import us.codecraft.webmagic.utils.UrlUtils;


/**
 * Entrance of a crawler.<br>
 * A spider contains four modules: Downloader, Scheduler, PageProcessor and
 * Pipeline.<br>
 * Every module is a field of Spider. <br>
 * The modules are defined in interface. <br>
 * You can customize a spider with various implementations of them. <br>
 * Examples: <br>
 * <br>
 * A simple crawler: <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")).run();<br>
 * <br>
 * Store results to files by FilePipeline: <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")) <br>
 * .pipeline(new FilePipeline("/data/temp/webmagic/")).run(); <br>
 * <br>
 * Use FileCacheQueueScheduler to store urls and cursor in files, so that a
 * Spider can resume the status when shutdown. <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")) <br>
 * .scheduler(new FileCacheQueueScheduler("/data/temp/webmagic/cache/")).run();
 * <br>
 *
 * @author code4crafter@gmail.com <br>
 * @see Downloader
 * @see Scheduler
 * @see PageProcessor
 * @see Pipeline
 * @since 0.1.0
 */
public class Spider implements Runnable, Task {

	protected Downloader downloader;

	public Downloader getDownloader() {
		return downloader;
	}

	public PageProcessor getPageProcessor() {
		return pageProcessor;
	}

	protected List<Pipeline> pipelines = new ArrayList<Pipeline>();

	protected PageProcessor pageProcessor;

	protected List<Request> startRequests;

	protected Site site;

	protected String uuid;

	private static final Logger logger = LoggerFactory.getLogger(Spider.class);

	/**
	 * httpclient的连接池，最大连接限制
	 */
	protected int threadNum = 100;

	protected AtomicInteger stat = new AtomicInteger(STAT_INIT);

	protected boolean exitWhenComplete = true;

	protected final static int STAT_INIT = 0;

	protected final static int STAT_RUNNING = 1;

	protected final static int STAT_STOPPED = 2;

	protected boolean spawnUrl = true;

	protected boolean destroyWhenExit = true;

	private List<SpiderListener> spiderListeners;

	private final AtomicLong pageCount = new AtomicLong(0);

	private Date startTime;

	private int emptySleepTime = 30000;

	private TaskService taskService;

	private ScheduledTaskDAO scheduledTaskDAO;

	private TaskDuplicateRemover duplicateRemover;
	/**
	 * create a spider with pageProcessor.
	 *
	 * @param pageProcessor
	 *            pageProcessor
	 * @return new spider
	 * @see PageProcessor
	 */
	public static Spider create(PageProcessor pageProcessor) {
		return new Spider(pageProcessor);
	}

	/**
	 * create a spider with pageProcessor.
	 *
	 * @param pageProcessor
	 *            pageProcessor
	 */
	public Spider(PageProcessor pageProcessor) {
		this.pageProcessor = pageProcessor;
		this.site = pageProcessor.getSite();
		this.startRequests = pageProcessor.getSite().getStartRequests();
		this.initComponent();
	}

	/**
	 * Set startUrls of Spider.<br>
	 * Prior to startUrls of Site.
	 *
	 * @param startUrls
	 *            startUrls
	 * @return this
	 */
	public Spider startUrls(List<String> startUrls) {
		checkIfRunning();
		this.startRequests = UrlUtils.convertToRequests(startUrls);
		return this;
	}

	/**
	 * Set startUrls of Spider.<br>
	 * Prior to startUrls of Site.
	 *
	 * @param startRequests
	 *            startRequests
	 * @return this
	 */
	public Spider startRequest(List<Request> startRequests) {
		checkIfRunning();
		this.startRequests = startRequests;
		return this;
	}

	/**
	 * Set an uuid for spider.<br>
	 * Default uuid is domain of site.<br>
	 *
	 * @param uuid
	 *            uuid
	 * @return this
	 */
	public Spider setUUID(String uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * add a pipeline for Spider
	 *
	 * @param pipeline
	 *            pipeline
	 * @return this
	 * @see #addPipeline(us.codecraft.webmagic.pipeline.Pipeline)
	 * @deprecated
	 */
	public Spider pipeline(Pipeline pipeline) {
		return addPipeline(pipeline);
	}

	/**
	 * add a pipeline for Spider
	 *
	 * @param pipeline
	 *            pipeline
	 * @return this
	 * @see Pipeline
	 * @since 0.2.1
	 */
	public Spider addPipeline(Pipeline pipeline) {
		checkIfRunning();
		this.pipelines.add(pipeline);
		return this;
	}

	/**
	 * set pipelines for Spider
	 *
	 * @param pipelines
	 *            pipelines
	 * @return this
	 * @see Pipeline
	 * @since 0.4.1
	 */
	public Spider setPipelines(List<Pipeline> pipelines) {
		checkIfRunning();
		this.pipelines = pipelines;
		return this;
	}

	/**
	 * clear the pipelines set
	 *
	 * @return this
	 */
	public Spider clearPipeline() {
		pipelines = new ArrayList<Pipeline>();
		return this;
	}

	/**
	 * set the downloader of spider
	 *
	 * @param downloader
	 *            downloader
	 * @return this
	 * @see #setDownloader(us.codecraft.webmagic.downloader.Downloader)
	 * @deprecated
	 */
	public Spider downloader(Downloader downloader) {
		return setDownloader(downloader);
	}

	/**
	 * set the downloader of spider
	 *
	 * @param downloader
	 *            downloader
	 * @return this
	 * @see Downloader
	 */
	public Spider setDownloader(Downloader downloader) {
		checkIfRunning();
		this.downloader = downloader;
		return this;
	}

	protected void initComponent() {
//		if (downloader == null) {
//			this.downloader = new HttpClientDownloader();
//		}
		// if (pipelines.isEmpty()) {
		// pipelines.add(new ConsolePipeline());
		// }
//		downloader.setThread(threadNum);
		startTime = new Date();
	}

	@Override
	public void run() {

	}

	protected void onError(Request request) {
		if (CollectionUtils.isNotEmpty(spiderListeners)) {
			for (SpiderListener spiderListener : spiderListeners) {
				spiderListener.onError(request);
			}
		}
	}

	protected void onSuccess(Request request) {
		if (CollectionUtils.isNotEmpty(spiderListeners)) {
			for (SpiderListener spiderListener : spiderListeners) {
				spiderListener.onSuccess(request);
			}
		}
	}

	private void checkRunningStat() {
		while (true) {
			int statNow = stat.get();
			if (statNow == STAT_RUNNING) {
				throw new IllegalStateException("Spider is already running!");
			}
			if (stat.compareAndSet(statNow, STAT_RUNNING)) {
				break;
			}
		}
	}

	public void close() {
		destroyEach(downloader);
		destroyEach(pageProcessor);
		for (Pipeline pipeline : pipelines) {
			destroyEach(pipeline);
		}
	}

	private void destroyEach(Object object) {
		if (object instanceof Closeable) {
			try {
				((Closeable) object).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Process specific urls without url discovering.
	 *
	 * @param urls
	 *            urls to process
	 */
	public void test(String... urls) {
		initComponent();
		if (urls.length > 0) {
			for (String url : urls) {
				processRequest(new Request(url));
			}
		}
	}

	public Spider processRequest(Request... requests) {
		for (Request request : requests) {
			processRequest(request);
		}
		return this;
	}

	public Spider processRequest(Request request) {
		Page page = downloader.download(request, this);
		if (page == null) {
			// sleep(site.getRetrySleepTime());
			onError(request);
			return this;
		}
		page.setSpider(this);
		// for cycle retry
		if (page.isNeedCycleRetry()) {
			extractAndAddRequests(page, true);
			// sleep(site.getRetrySleepTime());
			return this;
		}
		try {
			pageProcessor.process(page);
			postProcessPage(page);
		} catch (Exception e) {
			logger.error(pageProcessor.getClass() + ":" + request.getUrl(), e);
			throw e;
		}
		extractAndAddRequests(page, spawnUrl);
		if (!page.getResultItems().isSkip()) {
			for (Pipeline pipeline : pipelines) {
				pipeline.process(page.getResultItems(), this);
			}
		}
		// for proxy status management
		request.putExtra(Request.STATUS_CODE, page.getStatusCode());
		// sleep(site.getSleepTime());
		return this;
	}

	public void postProcessPage(Page page){
		if (this.getDownloader() instanceof SeleniumDownloader && page.getWebDriver() != null) {
			((SeleniumDownloader) this.getDownloader()).returnToPool(page.getWebDriver());
		}
	}

	public Spider processPage(Page page) {
		page.setSpider(this);
		try {
			pageProcessor.process(page);
		} catch (Exception e) {
			logger.error(pageProcessor.getClass() + ":" + page.getUrl().get(), e);
			throw e;
		}
		extractAndAddRequests(page, spawnUrl);
		if (!page.getResultItems().isSkip()) {
			for (Pipeline pipeline : pipelines) {
				pipeline.process(page.getResultItems(), this);
			}
		}
		return this;
	}

	protected void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void extractAndAddRequests(Page page, boolean spawnUrl) {
		this.distinctUrlByRequests(page);
		if (spawnUrl && CollectionUtils.isNotEmpty(page.getTargetRequests())) {
			for (Request request : page.getTargetRequests()) {
				try {
					addRequest(page, request);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/***
	 * 添加的url去重
	 * @param page
	 */
	private void distinctUrlByRequests(Page page) {
		List<Request> targetRequests = page.getTargetRequests();
		if(targetRequests==null||targetRequests.size()==0){
			return;
		}
		List<Request> distinctList=new ArrayList<>();
		Set<String> urlSet=new HashSet<>();
		for(Request request:targetRequests){
			String url = request.getUrl();
			Request currentRequest = page.getRequest();
			if(urlSet.contains(url)){
				continue;
			}else{
				urlSet.add(url);
			}
			//链接发现的子链，不应该把当前链接添加进去。
			if(currentRequest!=null&&!StringUtils.isBlank(currentRequest.getUrl())){
				if(currentRequest.getUrl().equals(url)){
					continue;
				}
			}
			distinctList.add(request);
		}
		targetRequests.clear();
		targetRequests.addAll(distinctList);
	}


	/**
	 *
	 * @param page
	 *            当前请求到的page
	 * @param request
	 *            需要加入的新请求
	 * @throws IOException
	 */
	public void addRequest(Page page, Request request) throws IOException {
		logger.debug("添加任务：" + request.getUrl());

		if(this.isDownloadSource(request)){
			return;
		}

		if (site.getDomain() == null && request != null && request.getUrl() != null) {
			site.setDomain(UrlUtils.getDomain(request.getUrl()));
		}

		int sleepTime = site.getSleepTime();
		request.putExtra(TaskExtraField.SLEEP_TIME, sleepTime);

		// 扔到待调度表
		TaskDO task = new TaskDO();
		task.setExpectedRunTime(new Date());
		task.setExtras(request.getExtras());
		task.setJobUrl(request.getUrl());
		task.setRequestMethod(request.getMethod());
		TaskDO currentTask;
		try {
			// 原请求中任务的部分参数拷贝
			currentTask = page.getRequest().getCurrentTask();
			ExtrasUtils.taskExtrasCopy(currentTask,task);//针对于一些特殊的扩展字段，需要上下层传递
			task.setJobId(currentTask.getJobId());
			task.setJobName(currentTask.getJobName());
			//task.setTaskCreateTime(currentTask.getTaskCreateTime());
			task.setTaskVersion(currentTask.getTaskVersion());
			task.setJobCategory(currentTask.getJobCategory());
			task.setTaskFrom(currentTask.getTaskFrom());
			task.setJobType(currentTask.getJobType());
			task.setJobTag(currentTask.getJobTag());
			//task.setTaskGenerateId(currentTask.getTaskGenerateId());
		} catch (Exception e) {
			logger.error("添加request异常，失败原因"+e.getMessage()+"\trequest:"+JSON.toJSONString(page.getRequest()), e);
		}

		if(duplicateRemover!=null&&duplicateRemover.duplicate(task)){
			return;
		}

		if(scheduledTaskDAO!=null){
			if(scheduledTaskDAO.containTask(task)){//保证每次任务的url都不能重爬。
//			logger.warn("添加任务失败，url组内去重：" + task.getJobId() + "\t" + task.getTaskVersion() + "\t" + task.getJobUrl() + "\t" + task.getJobTag());
				return;
			}else{
				scheduledTaskDAO.addTask(task);
			}
		}

		taskService.addTask(task);
		logger.debug("添加任务成功：" + task.getJobId() + "\t" + task.getTaskVersion() + "\t" + task.getJobUrl() + "\t" + task.getJobTag());
	}

	/**
	 * 简单的判断一下是否为下载资源，如flv，pdf等等。
	 * @param page
	 * @param request
	 * @return
	 */
	private static Set<String> sourceType=new HashSet<>();

	static {
		sourceType.add("flv");
		sourceType.add("pdf");
		sourceType.add("doc");
		sourceType.add("docx");
		sourceType.add("xls");
		sourceType.add("xlsx");
		sourceType.add("ppt");
		sourceType.add("pptx");
		sourceType.add("wma");
		sourceType.add("wmv");
		sourceType.add("rar");
		sourceType.add("zip");
		sourceType.add("avi");
		sourceType.add("txt");
		sourceType.add("mp4");
		sourceType.add("mp3");
	}

	private boolean isDownloadSource(Request request){
		String currentUrl=request.getUrl();
		//爬虫平台只做页面解析。
		Iterator<String> iterator = sourceType.iterator();
		while(iterator.hasNext()){
			String type = iterator.next();
			//假设url已.pdf结尾 被认定为资源链接
			if(currentUrl.endsWith("."+type)||currentUrl.contains("."+type+"?")){
				if(request.getCurrentTask()!=null){
					logger.warn("添加了疑似资源链接失败："+currentUrl+"\t任务id："+request.getCurrentTask().getJobId()+"\t任务名称："+request.getCurrentTask().getJobName());
				}
				return true;
			}
		}
		return false;
	}


	protected void checkIfRunning() {
		if (stat.get() == STAT_RUNNING) {
			throw new IllegalStateException("Spider is already running!");
		}
	}

	public void runAsync() {
		Thread thread = new Thread(this);
		thread.setDaemon(false);
		thread.start();
	}

	protected CollectorPipeline getCollectorPipeline() {
		return new ResultItemsCollectorPipeline();
	}

	public void start() {
		runAsync();
	}

	public void stop() {
		if (stat.compareAndSet(STAT_RUNNING, STAT_STOPPED)) {
			logger.info("Spider " + getUUID() + " stop success!");
		} else {
			logger.info("Spider " + getUUID() + " stop fail!");
		}
	}

	/**
	 * start with more than one threads
	 *
	 * @param threadNum
	 *            threadNum
	 * @return this
	 */
	public Spider thread(int threadNum) {
		checkIfRunning();
		this.threadNum = threadNum;
		if (threadNum <= 0) {
			throw new IllegalArgumentException("threadNum should be more than one!");
		}
		return this;
	}

	/**
	 * start with more than one threads
	 *
	 * @param executorService
	 *            executorService to run the spider
	 * @param threadNum
	 *            threadNum
	 * @return this
	 */
	public Spider thread(ExecutorService executorService, int threadNum) {
		checkIfRunning();
		this.threadNum = threadNum;
		if (threadNum <= 0) {
			throw new IllegalArgumentException("threadNum should be more than one!");
		}
		return this;
	}

	public boolean isExitWhenComplete() {
		return exitWhenComplete;
	}

	/**
	 * Exit when complete. <br>
	 * True: exit when all url of the site is downloaded. <br>
	 * False: not exit until call stop() manually.<br>
	 *
	 * @param exitWhenComplete
	 *            exitWhenComplete
	 * @return this
	 */
	public Spider setExitWhenComplete(boolean exitWhenComplete) {
		this.exitWhenComplete = exitWhenComplete;
		return this;
	}

	public boolean isSpawnUrl() {
		return spawnUrl;
	}

	/**
	 * Get page count downloaded by spider.
	 *
	 * @return total downloaded page count
	 * @since 0.4.1
	 */
	public long getPageCount() {
		return pageCount.get();
	}

	/**
	 * Get running status by spider.
	 *
	 * @return running status
	 * @see Status
	 * @since 0.4.1
	 */
	public Status getStatus() {
		return Status.fromValue(stat.get());
	}

	public enum Status {
		Init(0), Running(1), Stopped(2);

		private Status(int value) {
			this.value = value;
		}

		private int value;

		int getValue() {
			return value;
		}

		public static Status fromValue(int value) {
			for (Status status : Status.values()) {
				if (status.getValue() == value) {
					return status;
				}
			}
			// default value
			return Init;
		}
	}

	/**
	 * Whether add urls extracted to download.<br>
	 * Add urls to download when it is true, and just download seed urls when it
	 * is false. <br>
	 * DO NOT set it unless you know what it means!
	 *
	 * @param spawnUrl
	 *            spawnUrl
	 * @return this
	 * @since 0.4.0
	 */
	public Spider setSpawnUrl(boolean spawnUrl) {
		this.spawnUrl = spawnUrl;
		return this;
	}

	@Override
	public String getUUID() {
		if (uuid != null) {
			return uuid;
		}
		if (site != null) {
			return site.getDomain();
		}
		uuid = UUID.randomUUID().toString();
		return uuid;
	}

	@Override
	public Site getSite() {
		return site;
	}

	public List<SpiderListener> getSpiderListeners() {
		return spiderListeners;
	}

	public Spider setSpiderListeners(List<SpiderListener> spiderListeners) {
		this.spiderListeners = spiderListeners;
		return this;
	}

	public Date getStartTime() {
		return startTime;
	}

	/**
	 * Set wait time when no url is polled.<br>
	 * <br>
	 *
	 * @param emptySleepTime
	 *            In MILLISECONDS.
	 */
	public void setEmptySleepTime(int emptySleepTime) {
		this.emptySleepTime = emptySleepTime;
	}

	/**
	 * @return the taskService
	 */
	public TaskService getTaskService() {
		return taskService;
	}

	/**
	 * @param taskService
	 *            the taskService to set
	 */
	public void setTaskService(TaskService taskService) {
		this.taskService = taskService;
	}

	public ScheduledTaskDAO getScheduledTaskDAO() {
		return scheduledTaskDAO;
	}

	public void setScheduledTaskDAO(ScheduledTaskDAO scheduledTaskDAO) {
		this.scheduledTaskDAO = scheduledTaskDAO;
	}

	public TaskDuplicateRemover getDuplicateRemover() {
		return duplicateRemover;
	}

	public void setDuplicateRemover(TaskDuplicateRemover duplicateRemover) {
		this.duplicateRemover = duplicateRemover;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public List<Pipeline> getPipelines() {
		return pipelines;
	}

	/**
	 * 增加爬取深度控制
	 */
	public void setCrawleDepth(Integer depth){
		this.duplicateRemover.setRequestLimit(depth);
	}

	public  Integer getCrawleDepth(){
		return  this.duplicateRemover.getRequestLimit();
	}
}
