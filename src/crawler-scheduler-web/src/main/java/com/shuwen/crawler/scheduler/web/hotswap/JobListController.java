package com.shuwen.crawler.scheduler.web.hotswap;

import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.common.util.CrawlerException;
import com.shuwen.crawler.rpc.RemoteJobListService;
import com.shuwen.crawler.rpc.dto.JobDTO;
import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.UicResultDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("crawler/jobs")
public class JobListController {
    @Resource
    private RemoteJobListService remoteJobListService;

    @Resource
    private TaskServiceFactory taskServiceFactory;

    /**
     * 获取所有job信息
     * @param operator 操作者
     * @return 所有job信息
     */
    @GetMapping("")
    public UicResultDO getAllJobs(String operator,
                             @RequestParam(defaultValue = "1") Integer pageNo,
                             @RequestParam(defaultValue = "20") Integer pageRow,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<JobDTO> jobs = remoteJobListService.getAllJobs(operator, queryString, pageNo, pageRow);

        return ResultGenerator.createGenerator(PageResult.class)
                .setData(jobs)
                .success()
                .getResultDO();
    }

    /**
     * 获取某个job
     * @param operator 操作者
     * @param id job id
     * @return job信息
     */
    @GetMapping("{id}")
    public UicResultDO getJobById(String operator, @PathVariable("id")Long id){
        JobDTO jobDTO = remoteJobListService.getJobById(id);

        return ResultGenerator.createGenerator(JobDTO.class)
                .success()
                .setData(jobDTO)
                .getResultDO();
    }

    /**
     * 新建job
     * @param operator 操作者
     * @param jobDTO job信息
     */
    @PostMapping("")
    public UicResultDO createJob(String operator, @RequestBody JobDTO jobDTO){
        jobDTO.setId(null);
        jobDTO.setDeleted(false);
        this.checkCron(jobDTO.getCronExpression());
        jobDTO = remoteJobListService.saveJob(operator, jobDTO);
        return ResultGenerator.createGenerator(JobDTO.class)
                .setData(jobDTO)
                .success()
                .getResultDO();
    }

    /**
     * 更新某个job
     * @param operator 操作者
     * @param id id
     * @param jobDTO job信息
     */
    @PutMapping("{id}")
    public UicResultDO updateJob(String operator, @PathVariable("id")Long id, @RequestBody JobDTO jobDTO){
        jobDTO.setId(id);
        jobDTO.setDeleted(false);
        this.checkCron(jobDTO.getCronExpression());
        jobDTO = remoteJobListService.saveJob(operator, jobDTO);

        return ResultGenerator.createGenerator(JobDTO.class)
                .setData(jobDTO)
                .success()
                .getResultDO();
    }

    /**
     * 调度时间语法校验
     * @param cron
     */
    private void checkCron(String cron){
        if(StringUtils.isBlank(cron)){
            return;
        }
        try {
            new CronSequenceGenerator(cron);
        }catch (Exception e){
            throw new CrawlerException("调度规则异常，异常原因："+e.getMessage());

        }
    }


    /**
     * 删除某个job
     * @param operator 操作者
     * @param id id
     */
    @DeleteMapping("{id}")
    public UicResultDO deleteJob(String operator, @PathVariable("id")Long id){

        JobDTO jobDTO = remoteJobListService.deleteJobById(operator, id);

        return ResultGenerator.createGenerator(JobDTO.class)
                .setData(jobDTO)
                .success()
                .getResultDO();
    }


    /**
     * 获取调度时间
     */
    @GetMapping("cronList")
    public UicResultDO cronList(@RequestParam(defaultValue = "10") Integer times,@RequestParam(defaultValue = "") String cron){
        List<String> result=new ArrayList<>();
        try {
            CronSequenceGenerator sequenceGenerator = new CronSequenceGenerator(URLDecoder.decode(cron));
            SimpleDateFormat simpleDateFormat=new SimpleDateFormat("YYYY-MM-dd HH:mm:SS");
            Date date = new Date();
            for(int x=0;x<times;x++){
                date = sequenceGenerator.next(date);
                result.add(simpleDateFormat.format(date));
            }
        }catch (Exception e){
            throw new CrawlerException("调度规则异常，异常原因："+e.getMessage());
        }
        return ResultGenerator.createGenerator(List.class)
                .setData(result)
                .success()
                .getResultDO();
    }

    /**
     * 任务的代理分组ip更新
     */
    @PutMapping("{id}/refreshIp")
    public UicResultDO refreshIp(@PathVariable("id")Long id,String operator){
        remoteJobListService.refreshIp(id,operator);
        return ResultGenerator.createGenerator(String.class)
                .setData("更新成功")
                .success()
                .getResultDO();
    }


    @GetMapping("deleteAllTask/{id}")
    public UicResultDO deleteAllTask(@PathVariable("id")Long jobId){
        int num = taskServiceFactory.deleteTaskByJobdId(jobId);
        return ResultGenerator.createGenerator(String.class)
                .success()
                .setData("清除了"+num+"条！")
                .getResultDO();
    }

}
