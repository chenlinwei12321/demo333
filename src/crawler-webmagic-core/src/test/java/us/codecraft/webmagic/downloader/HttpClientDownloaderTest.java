package us.codecraft.webmagic.downloader;

import com.github.dreamhead.moco.*;
import com.github.dreamhead.moco.Runnable;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Ignore;
import org.junit.Test;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.selector.Html;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.github.dreamhead.moco.Moco.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author code4crafer@gmail.com
 */
public class HttpClientDownloaderTest {

    @Ignore
    @Test
    public void testCookie() {
        Site site = Site.me().setDomain("www.diandian.com").addCookie("t", "43ztv9srfszl99yxv2aumx3zr7el7ybb");
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        Page download = httpClientDownloader.download(new Request("http://www.diandian.com"), site.toTask());
        assertTrue(download.getHtml().toString().contains("flashsword30"));
    }

    @Test
    public void testDownloader() {
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        Html html = httpClientDownloader.download("http://news.sina.com.cn/s/wh/2017-06-29/doc-ifyhrxtp6313113.shtml");
        assertTrue(!html.getFirstSourceText().isEmpty());
    }

    @Test
    public void testHttpPost() {
        Site site = Site.me().setDomain("www.infoq.cn");
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        Request request = new Request("https://www.infoq.cn/public/v1/article/getList").setMethod("POST").addHeader("Referer","https://www.infoq.cn/topic/8").addJsonData("{\"type\":1,\"size\":50,\"id\":8}","UTF-8");
        Page download = httpClientDownloader.download(request, site.toTask());
        //Html html = httpClientDownloader.download("http://news.sina.com.cn/s/wh/2017-06-29/doc-ifyhrxtp6313113.shtml");
        //assertTrue(!html.getFirstSourceText().isEmpty());
        String source = download.getRawText();
        System.out.print(source);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDownloaderInIllegalUrl() throws UnsupportedEncodingException {
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        httpClientDownloader.download("http://www.oschina.net/>");
    }

    @Test
    public void testCycleTriedTimes() {
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        Task task = Site.me().setDomain("localhost").setCycleRetryTimes(5).toTask();
        Request request = new Request("http://localhost/404");
        Page page = httpClientDownloader.download(request, task);
        assertThat(page.getTargetRequests().size() > 0);
        assertThat((Integer) page.getTargetRequests().get(0).getExtra(Request.CYCLE_TRIED_TIMES)).isEqualTo(1);
        page = httpClientDownloader.download(page.getTargetRequests().get(0), task);
        assertThat((Integer) page.getTargetRequests().get(0).getExtra(Request.CYCLE_TRIED_TIMES)).isEqualTo(2);
    }

    @Test
    public void testGetHtmlCharset() throws Exception {
        HttpServer server = httpserver(12306);
        server.get(by(uri("/header"))).response(header("Content-Type", "text/html; charset=gbk"));
        server.get(by(uri("/meta4"))).response(with(text("<html>\n" +
                "  <head>\n" +
                "    <meta charset='gbk'/>\n" +
                "  </head>\n" +
                "  <body></body>\n" +
                "</html>")),header("Content-Type",""));
        server.get(by(uri("/meta5"))).response(with(text("<html>\n" +
                "  <head>\n" +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=gbk\" />\n" +
                "  </head>\n" +
                "  <body></body>\n" +
                "</html>")),header("Content-Type",""));
        Runner.running(server, new Runnable() {
            @Override
            public void run() {
                String charset = getCharsetByUrl("http://127.0.0.1:12306/header");
                assertEquals(charset, "gbk");
                charset = getCharsetByUrl("http://127.0.0.1:12306/meta4");
                assertEquals(charset, "gbk");
                charset = getCharsetByUrl("http://127.0.0.1:12306/meta5");
                assertEquals(charset, "gbk");
            }

            private String getCharsetByUrl(String url) {
                HttpClientDownloader downloader = new HttpClientDownloader();
                Site site = Site.me();
                CloseableHttpClient httpClient = new HttpClientGenerator().getClient(site);
                // encoding in http header Content-Type
                Request requestGBK = new Request(url);
                CloseableHttpResponse httpResponse = null;
                try {
                    httpResponse = httpClient.execute(downloader.getHttpUriRequest(requestGBK, site, null));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String charset = null;
                try {
                    byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
                    String contentType = httpResponse.getEntity().getContentType() == null ? "" : httpResponse.getEntity().getContentType().getValue();
                    charset = downloader.getHtmlCharset(contentType,contentBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return charset;
            }
        });
    }
    @Test
    public void testGetBaseHref() throws  Exception{
        Site site = Site.me().setDomain("www.baidu.com");
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        Request request = new Request("http://www.baidu.com/");
        //Request request = new Request("http://sxhj.gov.cn/");
        Page download = httpClientDownloader.download(request, site.toTask());
        if(null != download) {
            String source = download.getRawText();
            String cookie = download.getRespCookie();
            //source = source.replaceAll("<base href=\"http://www.bbrtv.com/\" target=\"_blank\"/>","<base  target=\"_blank\"/>\n");
            String baseUrl = download.getHtmlBaseHref(source);
            System.out.print(baseUrl);
        }
    }
}
