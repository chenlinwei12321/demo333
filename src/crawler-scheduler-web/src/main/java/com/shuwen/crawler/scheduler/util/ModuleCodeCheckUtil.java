package com.shuwen.crawler.scheduler.util;

import com.shuwen.crawler.rpc.dao.ModuleDO;
import com.shuwen.crawler.rpc.dto.ModuleTransformer;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.runtime.InvokerHelper;
import us.codecraft.webmagic.Page;

import java.util.ArrayList;

public class ModuleCodeCheckUtil {

    public static void groovyCodeCheck(String code,String moduleName){
        try {
            if(StringUtils.isBlank(code)){
                return;
            }
            if(moduleName.contains("-")){
                throw new RuntimeException("热部署爬虫名称不能包含-");
            }

            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
            Class scriptClass = groovyClassLoader.parseClass(code,moduleName);
            //拷贝binding
            Binding binding = new Binding();
            Script script = InvokerHelper.createScript(scriptClass, binding);
            ArrayList<Object> params = new ArrayList<>(2);
            params.add("http://xinhuazhiyun.com");
            params.add(new Page());
            script.run();
        } catch (Throwable e) {
            throw new RuntimeException("热部署脚本编译失败，失败原因:"+e.getMessage(),e);
        }
    }
    private static void groovyCodeCheck(ModuleDO moduleDO){
        groovyCodeCheck(moduleDO.getCode(),moduleDO.getName());
    }

    public static void main(String[] args) throws Exception {
        groovyCodeCheck("package groovycode\n" +
                "\n" +
                "import com.alibaba.fastjson.JSON\n" +
                "import com.alibaba.fastjson.JSONArray\n" +
                "import com.alibaba.fastjson.JSONObject;\n" +
                "import com.shuwen.crawler.worker.app.news.other.pojo.News;\n" +
                "import com.shuwen.crawler.worker.app.news.other.pojo.NewsGenerator;\n" +
                "\n" +
                "import us.codecraft.webmagic.Page;\n" +
                "import us.codecraft.webmagic.Request;\n" +
                "import us.codecraft.webmagic.selector.Selectable;\n" +
                "import java.util.regex.Matcher;\n" +
                "import java.util.regex.Pattern;\n" +
                "\n" +
                "public void process(String currentUrl, Page page) {\n" +
                "    final String ROOT_URL = \"http://36kr.com/\";//起始路径\n" +
                "    //原api路径:http://36kr.com/api/info-flow/main_site/posts?column_id=&b_id=5108736&per_page=20&_=1513672785836\n" +
                "    //修改后（拿到前50条数据）http://36kr.com/api/info-flow/main_site/posts?column_id=&per_page=50\n" +
                "    final String CHANNEL_URL = \"http://36kr.com/api/info-flow/main_site/posts?column_id=&per_page=50\";//api路径\n" +
                "    final String  DETAILS_URL= \"http://36kr.com/p/ID.html\";//详情页路径\n" +
                "    if(ROOT_URL.equals(currentUrl)){\n" +
                "        page.addTargetRequest(new Request(CHANNEL_URL));\n" +
                "    }else if(CHANNEL_URL.equals(currentUrl)){\n" +
                "        String rawText = page.getRawText();\n" +
                "        JSONObject object = JSONObject.parseObject(rawText);\n" +
                "        JSONObject data = object.getJSONObject(\"data\");\n" +
                "        JSONArray items = data.getJSONArray(\"items\");\n" +
                "        for (int i = 0; i < items.size(); i++) {\n" +
                "            JSONObject o = items.getJSONObject(i);\n" +
                "            String title = o.getString(\"title\");\n" +
                "            String summary = o.getString(\"summary\");\n" +
                "            String postTime = o.getString(\"published_at\");\n" +
                "\n" +
                "            JSONObject column = o.getJSONObject(\"column\");\n" +
                "            String category = column.getString(\"name\");\n" +
                "\n" +
                "            JSONObject user = o.getJSONObject(\"user\");\n" +
                "            String author = user.getString(\"name\");\n" +
                "\n" +
                "            /*\n" +
                "                评论数和点赞数\n" +
                "             */\n" +
                "            def counters = o.getJSONObject('counters');\n" +
                "            def likeCount = counters?.like;\n" +
                "            def readCount = counters?.view_count;\n" +
                "            def commentCount = counters?.comment;\n" +
                "\n" +
                "            String img = o.getString(\"cover\");\n" +
                "            String imgs = \"[\\\"\"+img+\"\\\"]\";\n" +
                "//\t\t\t\tcom.alibaba.fastjson.JSONArray imgs = JSON.parseArray(imgsS);\n" +
                "\n" +
                "            String labelStr = o.getString(\"extraction_tags\");\n" +
                "            String labelString = \"[\";\n" +
                "            JSONArray parseArray = JSONObject.parseArray(labelStr);\n" +
                "            for (int j = 0; j < parseArray.size(); j++) {\n" +
                "                JSONArray ja = (JSONArray) parseArray.get(j);\n" +
                "                if(ja.get(1)==2){\n" +
                "                    String la= (String) ja.get(0);\n" +
                "                    labelString+=\"\\\"\";\n" +
                "                    labelString+=la;\n" +
                "                    labelString+=\"\\\"\";\n" +
                "                    if(j != parseArray.size()-1){\n" +
                "                        labelString+=\",\";\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "            String label = labelString+\"]\";\n" +
                "            String id = o.getString(\"id\");\n" +
                "            String url = DETAILS_URL.replace(\"ID\", id);\n" +
                "            page.addTargetRequest(new Request(url)\n" +
                "                    .putExtra(\"title\",title)\n" +
                "                    .putExtra(\"summary\",summary)\n" +
                "                    .putExtra(\"category\",category)\n" +
                "                    .putExtra(\"author\",author)\n" +
                "                    .putExtra(\"imgUrls\",imgs)\n" +
                "                    .putExtra(\"postTime\",postTime)\n" +
                "                    .putExtra(\"label\", label)\n" +
                "                    .putExtra('likeCount', likeCount)\n" +
                "                    .putExtra('commentCount', commentCount)\n" +
                "                    .putExtra('readCount', readCount));\n" +
                "        }\n" +
                "\n" +
                "    }else if(currentUrl.matches(\"http://36kr\\\\.com/p/\\\\d+\\\\.html\")){\n" +
                "\n" +
                "        String contentHtml = page.getHtml().xpath(\"//script[4]/html()\").get();\n" +
                "        int indexOf = contentHtml.indexOf(\"\\\"content\\\":\");\n" +
                "        int indexOf2 = contentHtml.indexOf(\"\\\",\\\"cover\\\":\");\n" +
                "        String substring = contentHtml.substring(indexOf, indexOf2);\n" +
                "        String[] split = substring.split(\"\\\"content\\\":\\\"\");\n" +
                "        String content = split[1];\n" +
                "        int i = content.indexOf(\"\\\"http://36kr.com/p/\\\\\\\"\");\n" +
                "        if(content!=null && content.indexOf(\"\\\"http://36kr.com/p/\\\\\\\"\")!=-1){\n" +
                "            content=content.replace(\"\\\"http://36kr.com/p/\\\\\\\"\",\"\");\n" +
                "        }\n" +
                "\n" +
                "        /*Matcher matcher = Pattern.compile(\"\\\"content\\\":\\\"([\\\\W\\\\w]+</p>)\\\"\").matcher(page.getHtml().toString());\n" +
                "        if (matcher.find()){\n" +
                "            content = matcher.group(1).trim();\n" +
                "        }*/\n" +
                "      \n" +
                "      \t//console.log(content)\n" +
                "        News news = new News();\n" +
                "        Request req = page.getRequest();\n" +
                "        news.setTitle((String) req.getExtra(\"title\"));\n" +
                "        news.setCategory((String) req.getExtra(\"category\"));\n" +
                "        news.setSource(\"36kr\");\n" +
                "        news.setPostTime((String) req.getExtra(\"postTime\"));\n" +
                "        news.setContent(content);\n" +
                "        news.setAuthor((String) req.getExtra(\"author\"));\n" +
                "        news.setDomain(\"36kr.com\");\n" +
                "        news.setSite(\"36氪\");\n" +
                "        news.setSummary((String) req.getExtra(\"summary\"));\n" +
                "        news.setUrl(req.getUrl());\n" +
                "\n" +
                "        news.setImgUrls((String) req.getExtra(\"imgUrls\"));\n" +
                "//\t        String label = subLabel(page);\n" +
                "        news.setLabel((String)req.getExtra(\"label\"));\n" +
                "        JSONObject result = NewsGenerator.wrapJson(news);\n" +
                "\n" +
                "        /*\n" +
                "            点赞数和评论数\n" +
                "         */\n" +
                "        result.put('readCount', page.getRequest().getExtra('readCount'))\n" +
                "        result.put('likeCount', page.getRequest().getExtra('likeCount'))\n" +
                "        result.put('commentCount', page.getRequest().getExtra('commentCount'))\n" +
                "        page.setResult(result);\n" +
                "\n" +
                "    }\n" +
                "}","test");
    }

}
