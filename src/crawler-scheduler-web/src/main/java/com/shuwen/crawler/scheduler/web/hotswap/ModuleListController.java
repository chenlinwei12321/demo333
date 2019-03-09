package com.shuwen.crawler.scheduler.web.hotswap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shuwen.crawler.common.ResultGenerator;
import com.shuwen.crawler.common.logtail.AdvancedLogClientUtil;
import com.shuwen.crawler.common.logtail.LogClientUtil;
import com.shuwen.crawler.common.task.service.impl.TaskServiceFactory;
import com.shuwen.crawler.common.util.CrawlerException;
import com.shuwen.crawler.common.util.HttpClientUtil;
import com.shuwen.crawler.rpc.*;
import com.shuwen.crawler.rpc.dao.FieldDO;
import com.shuwen.crawler.rpc.dao.HotSwapTableName;
import com.shuwen.crawler.rpc.dao.ModuleDO;
import com.shuwen.crawler.rpc.dto.*;
import com.shuwen.crawler.common.PageResult;
import com.shuwen.crawler.common.UicResultDO;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.shuwen.crawler.scheduler.util.ObjectSerilizable;
import com.shuwen.crawler.scheduler.web.filter.User;
import com.shuwen.crawler.scheduler.web.util.WebFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import us.codecraft.webmagic.utils.UrlUtils;
import javax.servlet.http.HttpServletRequest;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("crawler/modules")
public class ModuleListController {

    private static final Logger logger=LoggerFactory.getLogger(ModuleListController.class);

    @Resource
    private RemoteModuleListService remoteModuleListService;
    @Resource
    private RemoteHotSwapService hotSwapService;

    @Resource
    private RemoteModuleBaseFieldService remoteModuleBaseFieldService;

    @Resource
    private RemoteJobListService remoteJobListService;

    @Resource
    private RemoteSiteListService siteListService;

    @Resource
    private RoleGroupService roleGroupService;

    @Resource
    private UserService userService;

    @Resource
    private GroupListService groupListService;

    @Resource
    private LogClientUtil logClientUtil;

    @Resource
    private AdvancedLogClientUtil advancedLogClientUtil;

    @Resource
    private TaskServiceFactory taskServiceFactory;

    @Value("${crawler.env}")
    private String CRAWLER_ENV;

    private static final String config_fields_table= HotSwapTableName.FIELD_TABLE;

    private static final String context_fields_table=HotSwapTableName.CONTEXT_TABLE;

    /**
     * 获取module列表
     * @param operator 操作者
     * @param pageNo 页码
     * @param pageRow 每页行数
     * @return module列表
     */
    @GetMapping("")
    public UicResultDO getAllModules(String operator,
                                     @RequestParam(defaultValue = "1") Integer pageNo,
                                     @RequestParam(defaultValue = "20") Integer pageRow,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<ModuleDTO> moduleList = remoteModuleListService.getModuleList(operator, queryString, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(moduleList)
                .success()
                .getResultDO();
    }

    /**
     * module查询带有对应抓取记录
     * @param operator 操作者
     * @param pageNo 页码
     * @param pageRow 每页行数
     * @return module列表
     */
    @GetMapping("home")
    public UicResultDO getHomeModule(String operator,
                                     @RequestParam(defaultValue = "1") Integer pageNo,
                                     @RequestParam(defaultValue = "20") Integer pageRow,HttpServletRequest request){
        String queryString = request.getParameter("queryString");
        PageResult<ModuleDTO> moduleList = remoteModuleListService.getModuleList(operator, queryString, pageNo, pageRow);
        for(ModuleDTO moduleDTO: (List<ModuleDTO>)moduleList.getRows()){
            String name=moduleDTO.getName();
            String jsonString = JSON.toJSONString(moduleDTO);
            JSONObject result=JSONObject.parseObject(jsonString);
            try{
                JSONObject crawlerData=remoteModuleListService.proving(logClientUtil,advancedLogClientUtil,name);
                if(crawlerData!=null){
                    moduleDTO.setDesc(crawlerData.getJSONArray("result").toJSONString());
                }else{
                    moduleDTO.setDesc("");
                }
            }catch (Exception e){
            }
        }
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(moduleList)
                .success()
                .getResultDO();
    }

    /**
     * 获取已发布module列表
     *
     * @return module列表
     */
    @GetMapping("pub/{name}")
    public UicResultDO getAllPubModules(@PathVariable String name){
        List<ModuleDO> moduleList = remoteModuleListService.getPubModuleList(name);
        ModuleDO moduleDO=new ModuleDO();
        //默认值
        moduleDO.setId(-1L);
        moduleDO.setName("default");
        moduleList.add(0,moduleDO);
        return ResultGenerator.createGenerator(List.class)
                .setData(moduleList)
                .success()
                .getResultDO();
    }


    /**
     * 根据id获取module
     * @param operator 操作者
     * @param id module id
     * @return 模块信息
     */
    @GetMapping("{id}")
    public UicResultDO getModule(String operator, @PathVariable("id")Long id){
        ModuleDTO moduleDTO = remoteModuleListService.getModuleById(operator, id);

        return ResultGenerator.createGenerator(ModuleDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }


    /***
     * 爬虫下载
     * @param operator
     * @param id
     * @return
     */
    @GetMapping("/download/{id}")
    public void downLoadModule(String operator, @PathVariable("id")Long id,HttpServletResponse response){
        ModuleBackUpDTO moduleBackUpDTO = this.createModuleBackUpDTO(operator, id);;
        File file = new File(moduleBackUpDTO.getModuleDTO().getName()+System.currentTimeMillis()+".bak");
        ObjectSerilizable.write2File(moduleBackUpDTO,file);
        try {
            WebFileUtils.downLoad(response,file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ModuleBackUpDTO createModuleBackUpDTO(String operator,Long id){
        ModuleDTO moduleDTO = remoteModuleListService.getModuleById(operator, id);
        ModuleBackUpDTO moduleBackUpDTO = new ModuleBackUpDTO();
        moduleBackUpDTO.setModuleDTO(moduleDTO);
        SiteDTO siteById = siteListService.getSiteById(moduleDTO.getDomainId());
        moduleBackUpDTO.setSiteDTO(siteById);
        JobDTO jobDTO= remoteJobListService.getJobFromModuleName(moduleDTO.getName());
        moduleBackUpDTO.setJobDTO(jobDTO);
        List<FieldDO> config_fields = remoteModuleBaseFieldService.getFieldsdByModuleId(config_fields_table, id, 1, 10000).getRows();
        List<FieldDO> context_fields = remoteModuleBaseFieldService.getFieldsdByModuleId(context_fields_table, id, 1, 10000).getRows();
        moduleBackUpDTO.setConfig_fields(config_fields);
        moduleBackUpDTO.setContext_fields(context_fields);
        return moduleBackUpDTO;
    }

    /***
     * 爬虫导入
     * @param operator
     * @param file
     * @return
     */
    @PostMapping("/upload/module")
    public void uploadModule(String operator,@RequestParam() MultipartFile file){
        if(operator==null||"".equals(operator.trim())){
            throw new CrawlerException("操作人信息不能为空");
        }
        if (!file.isEmpty()) {
            try {
                InputStream is = file.getInputStream();
                ModuleBackUpDTO moduleBackUpDTO= (ModuleBackUpDTO) ObjectSerilizable.inputStream2Obj(is);
                boolean isPub=false;//判断是否为已发布爬虫
                ModuleDTO module = moduleBackUpDTO.getModuleDTO();
                JobDTO jobDTO = moduleBackUpDTO.getJobDTO();
                if(module!=null&&module.getStatus()!=null&&"已发布".equals(module.getStatus())){
                    isPub=true;
                }
                SiteDTO siteDTO = this.uploadModuleWithSite(operator, moduleBackUpDTO.getSiteDTO());
                ModuleDTO moduleDTO = this.uploadModuleWithModule(operator, siteDTO, module);
                //更新字段表
                this.uploadModuleWithFields(operator,moduleBackUpDTO,moduleDTO);
                if(isPub){
                    hotSwapService.publishModule(moduleDTO.getOperator(), moduleDTO);
                }
                if(jobDTO!=null){
                    jobDTO.setIsPause(1);
                    jobDTO.setUseProxy(0);
                    this.uploadModuleWithJob(jobDTO,siteDTO);
                }
            } catch (IOException e) {
                logger.error("爬虫导入失败，失败原因："+e.getMessage(),e);
                throw new CrawlerException("爬虫导入失败，失败原因："+e.getMessage());
            }
        }
    }
    /**
     * 预发环境爬虫导入到生产环境
     */
    @GetMapping("/import2Prod/{id}")
    //http://local.pachong.xinhuazhiyun.com:8080/crawler/modules/import2Prod/724
    public UicResultDO import2Prod(String operator, @PathVariable("id")Long id){
        try {
            this.checkAuthoriy();
            ModuleBackUpDTO moduleBackUpDTO = this.createModuleBackUpDTO(operator, id);
            this.remoteHttpImportModule(operator,moduleBackUpDTO);
            //上线后暂停老任务
            Long jobId = moduleBackUpDTO.getJobDTO().getId();
            JobDTO jobDTO=new JobDTO();
            jobDTO.setId(jobId);
            jobDTO.setIsPause(1);
            remoteJobListService.updateJob(jobDTO);
            //还需要把redis待调度task清掉
            int num = taskServiceFactory.deleteTaskByJobdId(jobId);
        }catch (Exception e){
            throw new CrawlerException("导入失败，失败原因:"+e.getMessage());
        }
        return ResultGenerator.createGenerator(String.class)
                .setData("导入成功")
                .success()
                .getResultDO();
    }

    private void remoteHttpImportModule(String operator,ModuleBackUpDTO moduleBackUpDTO) throws Exception {
        File file = new File(moduleBackUpDTO.getModuleDTO().getName()+System.currentTimeMillis()+".bak");
        ObjectSerilizable.write2File(moduleBackUpDTO,file);
        //预发环境路径
//        HttpClientUtil.uploadFile("http://test.pachong.xinhuazhiyun.com/crawler/modules/upload/module?operator="+moduleBackUpDTO.getModuleDTO().getOperator(),"file",file);
        //生产环境路径
        HttpClientUtil.uploadFile("http://pachong.xinhuazhiyun.com/crawler/modules/upload/module?operator="+operator,"file",file);
        if(file.exists()){
            file.delete();
        }
    }

    /***
     * 校验权限
     */
    private void checkAuthoriy(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                List<String> roles = user.getRoles();
                if(roles==null||roles.size()==0){
                    throw new CrawlerException("获取登录用户权限失败！");
                }
                boolean flag=false;
                for(int x=0;x<roles.size();x++){
                    String role = roles.get(x);
                    if("superAdmin".equals(role)){
                        flag=true;
                    }
                }
                if(!flag){
                    throw new CrawlerException("您无此操作权限！");
                }
            }
        }
    }

    /***
     * 【爬虫导入】爬虫绑定站点
     * @param operator
     * @param siteDTO
     * @return
     */
    private SiteDTO uploadModuleWithSite(String operator,SiteDTO siteDTO){
        if(siteDTO==null){
            return siteDTO;
        }
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("domain",siteDTO.getDomain());
        List<SiteDTO> rows = siteListService.getAllSites("", jsonObject.toJSONString(), 1, 10000).getRows();
        if(rows==null||rows.size()==0){//不存在，则新加一个站点
            Date date = new Date();
//            siteDTO.setGmtCreate(date);
            siteDTO.setGmtModified(date);
            siteDTO.setId(null);
//            siteDTO.setOperator(operator);
            return siteListService.saveSite(siteDTO.getOperator(), siteDTO);
        }else{//已经存在站点则换回这个站点信息
            return rows.get(0);
        }
    }

    /***
     * 【爬虫导入】新建爬虫
     * @param operator
     * @param siteDTO
     * @param moduleDTO
     * @return
     */
    private ModuleDTO uploadModuleWithModule(String operator,SiteDTO siteDTO,ModuleDTO moduleDTO){
        if(moduleDTO==null){
            return moduleDTO;
        }
        moduleDTO.setDomain(siteDTO.getDomain());
        moduleDTO.setDomainId(siteDTO.getId());
        Date date = new Date();
        moduleDTO.setGmtModified(date);
        ModuleDO tempModule = this.getModuleId(moduleDTO.getName());
        moduleDTO.setId(tempModule==null?null:tempModule.getId());
        if(tempModule!=null&&tempModule.getDelete()==1){//历史库假删除数据，需要更新edit和put表
            moduleDTO.setDelete(0);
            remoteModuleListService.deleteModule(HotSwapTableName.MODULE_PUB,tempModule.getName());
        }
        //如果存在此爬虫，则取导入的修改者，新增取创建者
        return hotSwapService.saveModule(moduleDTO.getId()==null?moduleDTO.getCreator():moduleDTO.getOperator(), moduleDTO);
    }

    /***
     * 获取库里的爬虫id，如果存在相同爬虫名称的，则获取库中的数据，如果不存在，则更新
     * @return
     */
    private ModuleDO getModuleId(String moduleName){
        ModuleDO moduleByNameAndTable = remoteModuleListService.getAllModuleByNameAndTable(moduleName, HotSwapTableName.MODULE_EDIT);
        return moduleByNameAndTable;
    }

    /***
     * 【爬虫导入】创建配置化的的字段
     * @param operator
     * @param moduleDTO
     * @return
     */
    private void uploadModuleWithFields(String operator,ModuleBackUpDTO moduleBackUpDTO,ModuleDTO moduleDTO){
        if(moduleDTO.getIsConfig()!=1){
            return;
        }
        List<FieldDO> config_fields = moduleBackUpDTO.getConfig_fields();
        List<FieldDO> context_fields = moduleBackUpDTO.getContext_fields();

        this.initField(config_fields,moduleDTO,HotSwapTableName.FIELD_TABLE);
        this.initField(context_fields,moduleDTO,HotSwapTableName.CONTEXT_TABLE);
        //配置化爬虫。已经存在爬虫了，根据最新的字段表，只更新code
        hotSwapService.saveModule(moduleDTO.getOperator(),moduleDTO);
    }

    /***
     * 初始化俩个字段的值
     * @param fields
     * @param moduleDTO
     * @param tableName
     */
    private void initField(List<FieldDO> fields,ModuleDTO moduleDTO,String tableName){
        ModuleDO moduleByNameAndTable = remoteModuleListService.getModuleByNameAndTable(moduleDTO.getName(),HotSwapTableName.MODULE_EDIT);
        //查询历史修改表，如果存在，则需要清除历史字段，并绑定新的字段
        if(moduleByNameAndTable!=null){
            hotSwapService.deleteOldFields(tableName,moduleByNameAndTable.getId());
        }
        //id值绑定
        Map<Long,Long> oldParentIdBuffer=new HashMap<>();
        for(FieldDO fieldDO:fields){
            fieldDO.setCreator(moduleDTO.getCreator());
            fieldDO.setModifier(moduleDTO.getCreator());
            fieldDO.setModuleId(moduleDTO.getId());
            Long oldId=fieldDO.getId();
            fieldDO.setId(null);
            if(fieldDO.getParentId()!=null && oldParentIdBuffer.containsKey(fieldDO.getParentId())){//需要在新库绑定关系
                fieldDO.setParentId(oldParentIdBuffer.get(fieldDO.getParentId()));
            }
            FieldDO newField = remoteModuleBaseFieldService.saveFieldDO(tableName, moduleDTO.getOperator(), fieldDO);
            oldParentIdBuffer.put(oldId,newField.getId());
        }

    }

    /****
     * 【爬虫导入】新建关联任务
     * @param operator
     * @param jobDTO
     * @return
     */
    private void uploadModuleWithJob(JobDTO jobDTO,SiteDTO siteDTO){
        if(jobDTO==null){
            return;
        }
        JobDTO oldJob=remoteJobListService.getJobByName(jobDTO.getName());
        if(oldJob==null){
            GroupDTO groupDTO=groupListService.selectByName("ots不存储");//导入的爬虫默认分组为ots不存储
            if(groupDTO!=null){
                jobDTO.setCategory(Integer.parseInt(groupDTO.getId()+""));
            }
            Date date = new Date();
            jobDTO.setGmtCreate(date);
            jobDTO.setGmtModified(date);
            jobDTO.setDomain(siteDTO.getDomain());
            jobDTO.setDomainId(siteDTO.getId());
            jobDTO.setId(null);
            remoteJobListService.saveJob(jobDTO.getCreator(),jobDTO);
        }else if(oldJob.getDeleted()){
            oldJob.setDeleted(false);
            oldJob.setIsPause(1);
            oldJob.setGmtModified(new Date());
            oldJob.setUseProxy(0);
            remoteJobListService.updateJob(oldJob);
        }
    }

    /**
     * 根据id获取module code
     * @param operator 操作者
     * @param id module id
     * @return 模块信息
     */
    @GetMapping("code/{id}")
    public String getModuleCode(String operator, @PathVariable("id")Long id){
        ModuleDTO moduleDTO = remoteModuleListService.getModuleById(operator, id);
        return moduleDTO.getCode();
    }

    /**
     * 新增module
     * @param operator 操作者
     * @param moduleDTO 模块信息
     */
    @PostMapping("")
    public UicResultDO saveModule(String operator, @RequestBody ModuleDTO moduleDTO){
        moduleDTO.setId(null);
        moduleDTO = hotSwapService.saveModule(operator, moduleDTO);

        return ResultGenerator.createGenerator(ModuleDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }



    /**
     * 更新module
     * @param operator 操作者
     * @param id moduleId
     * @param moduleDTO 模块信息
     */
    @PutMapping("{id}")
    public UicResultDO updateModule(String operator,@PathVariable("id")Long id, @RequestBody ModuleDTO moduleDTO){

        moduleDTO.setId(id);
        moduleDTO = hotSwapService.saveModule(operator, moduleDTO);

        return ResultGenerator.createGenerator(ModuleDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }


    /**
     *  删除pub module
     * @param operator 操作者
     * @param id moduleId
     */
    @DeleteMapping("{id}")
    public UicResultDO deleteEditModule(String operator,@PathVariable("id")Long id){
        ModuleDTO moduleDTO=remoteModuleListService.deleteModuleById(operator,id);

        return ResultGenerator.createGenerator(ModuleDTO.class)
                .setData(moduleDTO)
                .success()
                .getResultDO();
    }

    //爬虫配置化字段 start

    /****
     * 新增，修改，删除字段都会对应相应爬虫的code从新生成并入库
     * @param module_id
     */
    private void reloadModuleCode(Long module_id){
        ModuleDO moduleDO = new ModuleDO();
        moduleDO.setId(module_id);
        //edit同步之后
        hotSwapService.initConfigModuleCodeAndUpdateDB(moduleDO);
        //pub表也需要同步
    }

    /***
     *  根据爬虫id获取所属的所有字段（为了以后方便，设置了分页查询，但是前端可以不实现）
     * @param operator
     * @param module_id
     * @param pageNo
     * @param pageRow
     * @return
     */
    @GetMapping("/{module_id}/fields")
    public UicResultDO getAllModuleConfigFieldsByModuleId(String operator,@PathVariable("module_id")Long module_id,@RequestParam(defaultValue = "1") Integer pageNo,
                                                          @RequestParam(defaultValue = "200") Integer pageRow){
        PageResult<FieldDO> allConfigFieldByModuleId = remoteModuleBaseFieldService.getFieldsdByModuleId(config_fields_table,module_id, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(allConfigFieldByModuleId)
                .success()
                .getResultDO();
    }

    /**
     * 根据爬虫字段id获取详情
     * @param operator
     * @param module_id
     * @param field_id
     * @return
     */
    @GetMapping("/{module_id}/fields/{field_id}")
    public UicResultDO getModuleConfigFieldById(String operator, @PathVariable("module_id")Long module_id,@PathVariable("field_id")Long field_id){
        FieldDO configFieldById = remoteModuleBaseFieldService.getFieldById(config_fields_table,module_id, field_id);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(configFieldById)
                .success()
                .getResultDO();
    }

    /**
     * 新增爬虫字段
     * @param operator
     * @param entity
     * @return
     */
    @PostMapping("/{module_id}/fields")
    public UicResultDO saveModuleConfigField(@PathVariable("module_id")Long module_id,String operator, @RequestBody FieldDO entity){
        try {
            String checkUrl = entity.getCheckUrl();
            if(null != checkUrl && !checkUrl.isEmpty()) {
                URL url = new URL(checkUrl);
            }
        }catch (Exception e){
            throw new CrawlerException("测试URL不正确，请输入正确的测试URL");
        }

        entity.setId(null);
        entity.setModuleId(module_id);
        entity = remoteModuleBaseFieldService.saveFieldDO(config_fields_table,operator,entity);
        this.reloadModuleCode(module_id);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(entity)
                .success()
                .getResultDO();
    }

    /**
     * 修改爬虫字段
     * @param operator
     * @param module_id
     * @param field_id
     * @param entity
     * @return
     */
    @PutMapping("/{module_id}/fields/{field_id}")
    public UicResultDO updateModuleConfigField(String operator,@PathVariable("module_id")Long module_id,@PathVariable("field_id")Long field_id, @RequestBody FieldDO entity){

        try {
            String checkUrl = entity.getCheckUrl();
            if(null != checkUrl && !checkUrl.isEmpty()) {
                URL url = new URL(checkUrl);
            }
        }catch (Exception e){
            throw new CrawlerException("测试URL不正确，请输入正确的测试URL");
        }

        entity.setId(field_id);
        entity.setModuleId(module_id);
        entity = remoteModuleBaseFieldService.saveFieldDO(config_fields_table,operator, entity);
        this.reloadModuleCode(module_id);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(entity)
                .success()
                .getResultDO();
    }

    /**
     * 删除爬虫字段
     * @param operator
     * @param module_id
     * @param field_id
     * @return
     */
    @DeleteMapping("/{module_id}/fields/{field_id}")
    public UicResultDO deleteConfigField(String operator,@PathVariable("module_id")Long module_id,@PathVariable("field_id")Long field_id){
        FieldDO configFieldDO = remoteModuleBaseFieldService.deleteFieldById(config_fields_table,module_id, field_id);
        this.reloadModuleCode(module_id);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(configFieldDO)
                .success()
                .getResultDO();
    }
    //爬虫字段相关end
    //*****************************************************************************//
    //爬虫上下文字段相关start
    /***
     *  根据爬虫id获取所属的所有字段（为了以后方便，设置了分页查询，但是前端可以不实现）
     * @param operator
     * @param module_id
     * @param pageNo
     * @param pageRow
     * @return
     */
    @GetMapping("/{module_id}/contexts")
    public UicResultDO getAllModuleContextsFieldsByModuleId(String operator,@PathVariable("module_id")Long module_id,@RequestParam(defaultValue = "1") Integer pageNo,
                                                            @RequestParam(defaultValue = "200") Integer pageRow){
        PageResult<FieldDO> fieldsdByModuleId = remoteModuleBaseFieldService.getFieldsdByModuleId(context_fields_table, module_id, pageNo, pageRow);
        return ResultGenerator.createGenerator(PageResult.class)
                .setData(fieldsdByModuleId)
                .success()
                .getResultDO();
    }

    /**
     * 根据爬虫字段id获取详情
     * @param operator
     * @param module_id
     * @return
     */
    @GetMapping("/{module_id}/contexts/{context_id}")
    public UicResultDO getModuleContextsFieldById(String operator, @PathVariable("module_id")Long module_id,@PathVariable("context_id")Long context_id){
        FieldDO fieldById = remoteModuleBaseFieldService.getFieldById(context_fields_table, module_id, context_id);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(fieldById)
                .success()
                .getResultDO();
    }

    /**
     * 新增爬虫字段
     * @param operator
     * @param entity
     * @return
     */
    @PostMapping("/{module_id}/contexts")
    public UicResultDO saveModuleContextsField(@PathVariable("module_id")Long module_id,String operator, @RequestBody FieldDO entity){
        entity.setId(null);
        entity.setModuleId(module_id);
        entity = remoteModuleBaseFieldService.saveFieldDO(context_fields_table,operator,entity);
        this.reloadModuleCode(module_id);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(entity)
                .success()
                .getResultDO();
    }

    /**
     * 修改爬虫字段
     * @param operator
     * @param module_id
     * @param entity
     * @return
     */
    @PutMapping("/{module_id}/contexts/{context_id}")
    public UicResultDO updateModuleContextsField(String operator,@PathVariable("module_id")Long module_id,@PathVariable("context_id")Long context_id, @RequestBody FieldDO entity){
        entity.setId(context_id);
        entity.setModuleId(module_id);

        this.reloadModuleCode(module_id);

        entity =remoteModuleBaseFieldService.saveFieldDO(context_fields_table,operator, entity);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(entity)
                .success()
                .getResultDO();
    }

    /**
     * 删除爬虫字段
     * @param operator
     * @param module_id
     * @return
     */
    @DeleteMapping("/{module_id}/contexts/{context_id}")
    public UicResultDO deleteModuleContextFields(String operator,@PathVariable("module_id")Long module_id,@PathVariable("context_id")Long context_id){
        FieldDO entity = remoteModuleBaseFieldService.deleteFieldById(context_fields_table,module_id, context_id);
        this.reloadModuleCode(module_id);
        return ResultGenerator.createGenerator(FieldDO.class)
                .setData(entity)
                .success()
                .getResultDO();
    }
    //爬虫上下文end

    /**
     * 检查
     * @param operator
     * @param module_id
     * @return
     */
    @GetMapping("check/{module_id}")
    public UicResultDO checkModuleCfg(String operator, @PathVariable("module_id")Long module_id){
        String retValue = null;
        try {
            retValue = hotSwapService.checkModuleScript(operator, module_id, null);
        }catch (CrawlerException crawlerEcp){
            throw  crawlerEcp;
        }

        return ResultGenerator.createGenerator(String.class)
                .setData(retValue)
                .success()
                .getResultDO();
    }

    @GetMapping("/{module_id}/checkurls")
    public UicResultDO getModuleCheckUrls(String operator,@PathVariable("module_id")Long module_id){

        List<String> checkUrls = hotSwapService.getModuleCheckUrls(module_id);

        return ResultGenerator.createGenerator(List.class)
                .setData(checkUrls)
                .success()
                .getResultDO();
    }

    @GetMapping("/{module_id}/checkparams")
    public  UicResultDO getModuleContextByUrl(String operator,@PathVariable("module_id")Long moduleId,String checkUrl){

        JSONObject  contextObj = hotSwapService.getModuleContextByUrl(moduleId,checkUrl);
        return ResultGenerator.createGenerator(JSONObject.class)
                .setData(contextObj)
                .success()
                .getResultDO();
    }

    /**
     * 需求平台创建爬虫
     * @param param 远程参数
     * @return
     */
    @GetMapping("/crawler")
    private UicResultDO createCrawler(String param){
        JSONObject obj= JSON.parseObject(param);
        ModuleBackUpDTO mudel = new ModuleBackUpDTO();
        //首先创建网站
        SiteDTO siteDTO = new SiteDTO();
        String url=obj.getString("url");
        siteDTO.setDomain(getDomain(UrlUtils.getDomain(url)));
        siteDTO.setName(obj.getString("siteName"));
        siteDTO.setDescription("需求平台创建网站！");
        siteDTO.setDeleted(0);
        SiteDTO site = this.uploadModuleWithSite(obj.getString("operator"),siteDTO);
        ModuleDTO model = new ModuleDTO();
        model.setOperator(obj.getString("operator"));
        model.setGroupName(obj.getString("groupName"));
        model.setGroupId(roleGroupService.getGroupByName(obj.getString("groupName")).getId()); //需要查询获得
        UserDTO user=new UserDTO();
        user.setNick(obj.getString("operator"));
        model.setCreatorId(userService.getUser(user).getId());
        model.setDomain(site.getName());
        model.setDomainId(site.getId());
        model.setEnableJS(0);
        model.setIsConfig(1);
        model.setName(obj.getString("siteName")+"爬虫");
        model.setPageType("default_v2");
        model.setRegexUrl(".*");
        model.setRetry(5);
        model.setSleep(Long.valueOf(2000));
        model.setType(4);
        model.setUserAgent("Chrome");
        model.setFeatureString("{\"site\":{\"retry\":5,\"sleep\":2000,\"timeout\":20000,\"circleRetry\":5,\"useGzip\":true,\"userAgent\":\"Chrome\"},\"downloader\":\"httpClient\"}");
        ModuleDTO moduleDTO=hotSwapService.saveModule(model.getOperator(), model);
        mudel.setSiteDTO(site);
        mudel.setModuleDTO(moduleDTO);
        return ResultGenerator.createGenerator(ModuleBackUpDTO.class)
                .setData(mudel)
                .success()
                .getResultDO();
    }


    /**
     * 获取domain
     * @return
     */
    public String getDomain(String url){
        String str = "";
        String reg="www\\.(.*)";
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(url);
        while (m.find()) {
            str= m.group(1);
        }
        return str;
    }


    /**
     * 需求平台验收
     * @param crawlerName 爬虫名称
     * @return
     */
    @GetMapping("/proving")
    private UicResultDO proving(String crawlerName){
        JSONObject obj = new JSONObject();
        try{
            obj=remoteModuleListService.proving(logClientUtil,advancedLogClientUtil,crawlerName);
        }catch (Exception e){
            throw  new CrawlerException(e.getMessage());
        }
        return ResultGenerator.createGenerator(JSONObject.class)
                .setData(obj)
                .success()
                .getResultDO();
    }

}
