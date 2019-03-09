package us.codecraft.webmagic.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.proxy.XhzyIpDTO;
import com.shuwen.crawler.common.util.ApiUrlUtil;
import com.shuwen.crawler.common.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class XhzyIpProxyHttpUtils {

    public static final Logger logger= LoggerFactory.getLogger(XhzyIpProxyHttpUtils.class);

    public static final String STATIC_URL = "http://116.62.95.196:8080/crawler-proxy/proxys/static/%d/%s/%d";//买的高可用固定ip

    public static final String CRAWLER_URL = "http://116.62.95.196:8080/crawler-proxy/proxys/crawler/%d/%s/%d";//爬虫爬取的代理ip

    public static final String accessSecret = "e919e835732df9c5a59a3ed29fcedd18";

    public static final String accessKey = "carwler_ip";

    /***
     * 获取代理ip
     * @param querySize 数量
     * @return
     */
    public static List<XhzyIpDTO> loadIp(Integer querySize){
        return loadIp(querySize,STATIC_URL);//查询高可用的代理ip
    }

    /***
     * 获取代理ip
     * @param querySize 数量
     * @return
     */
    public static List<XhzyIpDTO> loadIp(Integer querySize,String serverUrl){
        List<XhzyIpDTO> result=new ArrayList<>();
        if(querySize==null||querySize==0){
            querySize=10;
        }
        long timestamp = System.currentTimeMillis();
        String url = String.format(serverUrl, querySize, ApiUrlUtil.genSignature(accessSecret + timestamp + accessKey), timestamp);
        String ret = HttpClientUtil.httpGet(url, null, 3, null);

        JSONObject resultJson=JSONObject.parseObject(ret);
        //没有ip数据
        if(resultJson==null||!resultJson.containsKey("code")||resultJson.getInteger("code")!=200||!resultJson.containsKey("data")){
            return result;
        }
        JSONArray data = resultJson.getJSONArray("data");
        for(int x=0;x<data.size();x++){
            JSONObject jsonObject = data.getJSONObject(x);
            XhzyIpDTO xhzyIpDTO=new XhzyIpDTO();
            if(!jsonObject.containsKey("proxyType")||!jsonObject.containsKey("proxyAddress")||!jsonObject.containsKey("proxyPort")){
                continue;
            }
            String proxyType=jsonObject.getString("proxyType");
            String proxyAddress=jsonObject.getString("proxyAddress");
            Integer proxyPort = jsonObject.getInteger("proxyPort");
            xhzyIpDTO.setProxyPort(proxyPort);
            xhzyIpDTO.setProxyAddress(proxyAddress);
            xhzyIpDTO.setProxyType(proxyType);
            result.add(xhzyIpDTO);
        }
        return result;
    }

    public static void main(String[] args) {
        List<XhzyIpDTO> xhzyIpDTOS = loadIp(3,CRAWLER_URL);
        for(int x=0;x<xhzyIpDTOS.size();x++){
            System.out.println(xhzyIpDTOS.get(x).getProxyAddress());
        }

    }


}
