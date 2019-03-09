package com.shuwen.crawler.worker.app

import com.alibaba.fastjson.JSONObject
import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Request
import us.codecraft.webmagic.selector.Selectable

/**
 * startUrl = "http://www.cnhubei.com/"
 */
void process(String currentUrl, Page page){
    console.log("进入解析器");

    if(currentUrl.matches("http://www\\.cnhubei\\.com/")){
        console.log("首页...")
        /*
            获取所有列表页
         */
        page.addTargetRequest(new Request("http://news.cnhubei.com/gd/index.shtml"));
        for(int i=1;i<=1;i++){
            page.addTargetRequest(new Request("http://news.cnhubei.com/gd/index_"+i+".shtml"));
        }
    }else if(currentUrl.matches("http://news\\.cnhubei\\.com/gd/index(_\\d+)?\\.shtml")){
        console.log("列表页...");
        List<Selectable> nodes = page.getHtml().xpath("//div[@class=left_content]//li").nodes();
        /*
            获取列表中的所有链接，作为新任务
         */
        for(Selectable node: nodes){
            String url = node.xpath("//a/@href").get();
            page.addTargetRequest(new Request(url));
        }
    }else if(currentUrl.matches("http://news\\.cnhubei\\.com/.*/\\d{6}/t\\d+.shtml")){
        /*
            解析结构化数据
         */
        JSONObject result = new JSONObject().fluentPut("title", page.getHtml().xpath("//div[@class=\"title\"]/text()"));
        page.setResult(result);
    }
}
