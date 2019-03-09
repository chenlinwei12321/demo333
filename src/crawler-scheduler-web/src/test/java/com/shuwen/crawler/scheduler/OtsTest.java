//package com.shuwen.crawler.scheduler;
//
//
//import com.alicloud.openservices.tablestore.SyncClient;
//import com.aliyun.openservices.ots.OTSClient;
//import com.aliyun.openservices.ots.model.*;
//import com.shuwen.crawler.common.util.AKUtil;
//
//public class OtsTest {
//
//    //ots 没有修改表的接口无法修改表名，列组等信息
//    public static final String ak=AKUtil.getAliyunAccessKey();
//    public static final String sk=AKUtil.getAliyunAccessSecret();
//    public static OTSClient otsClient;
//    static{
//        String endpoint = "http://crawler-db.cn-hangzhou.ots.aliyuncs.com";
//        String instance = "crawler-test";
//        otsClient = new OTSClient(endpoint, ak, sk, instance);
//    }
//
//    public static void main(String[] args){
//        TableMeta tableMeta = new TableMeta("ots_create_table_test");
//        tableMeta.addPrimaryKeyColumn("url_md5", PrimaryKeyType.STRING);
//        tableMeta.addPrimaryKeyColumn("date", PrimaryKeyType.STRING);
//        CapacityUnit capacityUnit = new CapacityUnit(0, 0);
//        CreateTableRequest createTableRequest=new CreateTableRequest();
//        createTableRequest.setTableMeta(tableMeta);
//        createTableRequest.setReservedThroughput(capacityUnit);
//        otsClient.createTable(createTableRequest);
//        otsClient.shutdown();
//    }
//
//}
