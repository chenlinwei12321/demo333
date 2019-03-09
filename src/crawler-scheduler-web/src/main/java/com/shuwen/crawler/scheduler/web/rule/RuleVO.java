package com.shuwen.crawler.scheduler.web.rule;

import java.io.Serializable;
import java.util.List;

/**
 * @author: liuruijie
 * @email: i-liuruijie@shuwen.com
 * @date: 2017-11-30 13:56:15
 */
public class RuleVO implements Serializable{

    private static final long serialVersionUID = -3895798376175003855L;

    private Long warningId;
    private String tableName;
    private List<Long> ruleIds;
    private String email;
    private String remark;
    private Integer status;

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<Long> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<Long> ruleIds) {
        this.ruleIds = ruleIds;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
