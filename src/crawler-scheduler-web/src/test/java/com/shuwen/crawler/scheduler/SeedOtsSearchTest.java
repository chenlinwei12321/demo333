package com.shuwen.crawler.scheduler;


import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import com.shuwen.crawler.common.task.pojo.SeedDO;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.access.method.P;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class SeedOtsSearchTest {

    private static final String TABLE_NAME = "crawler_seeds";
    private static final String PK_NAME = "jobId_urlMd5";

    /**
     * @param row
     * @return
     */
    public static SeedDO row2Seed(Row row) {
        SeedDO seed = new SeedDO();
        setColumnValue(row, seed, "jobId");
        setColumnValue(row, seed, "url");
        seed.setUrlMd5(DigestUtils.md5Hex(seed.getUrl()));
        setColumnValue(row, seed, "jobTag");
        setColumnValue(row, seed, "jobName");
        setColumnValue(row, seed, "date");
        setColumnValue(row, seed, "gmtModified");
        setColumnValue(row, seed, "requestMethod");
        return seed;
    }

    private static void setColumnValue(Row row, SeedDO seed, String columnName) {
        Column col = row.getLatestColumn(columnName);
        if (col != null) {
            try {
                BeanUtils.setProperty(seed, columnName, col.getValue());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static void findAllSeedListByJob(SyncClient syncClient, long jobId, int limit) {
        RangeRowQueryCriteria criteria = new RangeRowQueryCriteria(TABLE_NAME);
        PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn(PK_NAME, PrimaryKeyValue.fromString(jobId + "_0"));
        criteria.setInclusiveStartPrimaryKey(primaryKeyBuilder.build());

        primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
        primaryKeyBuilder.addPrimaryKeyColumn(PK_NAME, PrimaryKeyValue.fromString(jobId + "_z"));
        criteria.setExclusiveEndPrimaryKey(primaryKeyBuilder.build());

        criteria.setMaxVersions(1);
        criteria.setLimit(limit);
        Map<String,Integer> total=new HashMap<>();
        Integer size=0;
        while (true) {
            GetRangeResponse getRangeResponse = syncClient.getRange(new GetRangeRequest(criteria));
            for (Row row : getRangeResponse.getRows()) {
                SeedDO seedDO = row2Seed(row);
//                if(total.containsKey(seedDO.getUrlMd5())){
//                    Integer integer = total.get(seedDO.getUrlMd5());
//                    total.put(seedDO.getUrlMd5(),integer+1);
//                }else{
//                    total.put(seedDO.getUrlMd5(),1);
//                }
//                System.out.println(total);
                size++;
            }
            // 若nextStartPrimaryKey不为null, 则继续读取.
            if (getRangeResponse.getNextStartPrimaryKey() != null) {
                criteria.setInclusiveStartPrimaryKey(getRangeResponse.getNextStartPrimaryKey());
            } else {
                break;
            }
        }
        System.out.println(size+"\t"+jobId);
    }

    public static void main(String[] args){

        String endpoint = "http://crawler-db.cn-hangzhou.ots.aliyuncs.com";
        String instance = "crawler-db";
        String akId = "LTAIdrPSSGzQBVrb";
        String sk = "EkYrVbauiMm9lIDGyqdamhGXvfP4Uj";
        SyncClient syncClient = new SyncClient(endpoint, akId, sk, instance);

        findAllSeedListByJob(syncClient,2543,20000);

    }


}
