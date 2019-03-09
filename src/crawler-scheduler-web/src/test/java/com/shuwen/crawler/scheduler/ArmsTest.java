package com.shuwen.crawler.scheduler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.arms.model.v20180620.ARMSQueryDataSetRequest;
import com.aliyuncs.arms.model.v20180620.ARMSQueryDataSetResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.shuwen.crawler.common.util.AKUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ArmsTest {

    private static final SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");

    public static void main(String[] args) throws Exception {

        Long dataSetId=15120l;
        Integer limitMinute=60*60*24;

        Long end=format.parse("2018-08-29 00:00:00").getTime();
        Integer queryMinute=60*24;
        Long start=end-1000*60*queryMinute;

        ARMSQueryDataSetResponse response = getResponse(dataSetId,limitMinute,start,end);
        JSONObject json=JSONObject.parseObject(response.getData());
        String resultSize = json.getString("resultSize");
        JSONArray data = json.getJSONObject("dimData").getJSONArray("");
        for(int x=0;x<data.size();x++){
            JSONObject jsonObject = data.getJSONObject(x);
            Long date = jsonObject.getLong("date");
            Long total = jsonObject.getLong("COUNT__line");
            System.out.println("date:"+format.format(new Date(date))+"\ttotal:"+total);
        }

    }

    /***
     *
     * @param dataSetId 数据集id
     * @param limitMinute 返回的数据的时间间隔
     * @param start 查询的起始日期
     * @param end   查询的截止日期
     * @return
     * @throws ClientException
     */
    public static ARMSQueryDataSetResponse getResponse(Long dataSetId,Integer limitMinute,Long start,Long end) throws ClientException {
        IAcsClient client = createClient();
        // 设置业务参数
        ARMSQueryDataSetRequest armsQueryDataSetRequest = new ARMSQueryDataSetRequest();
        //DatasetID在数据集管理页面上可查询到
        armsQueryDataSetRequest.setDatasetId(dataSetId);
        //查询数据的返回间隔，请务必自行保证为60的倍数
        armsQueryDataSetRequest.setIntervalInSec(limitMinute);
        //注意单位为毫秒
        armsQueryDataSetRequest.setMinTime(start);
        armsQueryDataSetRequest.setMaxTime(end);

        List<ARMSQueryDataSetRequest.Dimensions> list=new ArrayList<>();
        ARMSQueryDataSetRequest.Dimensions dimensions=new ARMSQueryDataSetRequest.Dimensions();
//        dimensions.setKey("区域");
//        dimensions.setValue("杭州");
        dimensions.setType("ALL");
        list.add(dimensions);
        armsQueryDataSetRequest.setDimensionss(list);


        List<String> measuress = new ArrayList<String>();
        measuress.add("0437.com");
        measuress.add("163.com");
//        armsQueryDataSetRequest.setMeasuress(measuress);

        try {
            //发送请求
            ARMSQueryDataSetResponse armsQueryDataSetResponse = client.getAcsResponse(armsQueryDataSetRequest);
            //获取并打印请求结果
            return armsQueryDataSetResponse;
        } catch (ClientException e) {
            return null;
        }
    }


    public static IAcsClient createClient() throws ClientException {
        // 用户主账号/RAM 子账号的 AK，或者 RAM 用户角色的临时安全令牌的 AK
        String accessKeyId = AKUtil.getAliyunAccessKey();
        // 用户主账号/RAM 子账号的 SK，或者 RAM 用户角色的临时安全令牌的 SK
        String accessKeySecret = AKUtil.getAliyunAccessSecret();
        //Region和endpoint保持一致，具体内容和ARMS的region绑定，
        String endpoint = "cn-hangzhou";//例如 cn-hangzhou
        String region = "cn-hangzhou";//例如 cn-hangzhou
        //产品名请固定填"ARMS"
        String productName = "ARMS";
        //如果是非杭州区域，请改写，如cn-beijing，
        String domain = "arms.cn-hangzhou.aliyuncs.com";
        IClientProfile profile = DefaultProfile.getProfile(region, accessKeyId, accessKeySecret);
        DefaultProfile.addEndpoint(endpoint, region, productName, domain);
        // ****后续的2、3示例代码插入此处****
        IAcsClient client = new DefaultAcsClient(profile);
        return client;
    }
}
