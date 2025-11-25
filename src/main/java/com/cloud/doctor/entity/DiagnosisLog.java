package com.cloud.doctor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI智能问诊记录表
 * @TableName bus_diagnosis_log
 */
@TableName(value ="bus_diagnosis_log")
@Data
public class DiagnosisLog implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private Long userId;

    /**
     * 用户输入的原始症状描述
     */
    private String symptomInput;

    /**
     * AI分析结果(JSON)
     */
    private String aiResultJson;

    /**
     * 最终推荐科室名称
     */
    private String recommendDeptName;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     * 
     */
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        DiagnosisLog other = (DiagnosisLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getSymptomInput() == null ? other.getSymptomInput() == null : this.getSymptomInput().equals(other.getSymptomInput()))
            && (this.getAiResultJson() == null ? other.getAiResultJson() == null : this.getAiResultJson().equals(other.getAiResultJson()))
            && (this.getRecommendDeptName() == null ? other.getRecommendDeptName() == null : this.getRecommendDeptName().equals(other.getRecommendDeptName()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getIsDeleted() == null ? other.getIsDeleted() == null : this.getIsDeleted().equals(other.getIsDeleted()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getSymptomInput() == null) ? 0 : getSymptomInput().hashCode());
        result = prime * result + ((getAiResultJson() == null) ? 0 : getAiResultJson().hashCode());
        result = prime * result + ((getRecommendDeptName() == null) ? 0 : getRecommendDeptName().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getIsDeleted() == null) ? 0 : getIsDeleted().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", symptomInput=").append(symptomInput);
        sb.append(", aiResultJson=").append(aiResultJson);
        sb.append(", recommendDeptName=").append(recommendDeptName);
        sb.append(", createTime=").append(createTime);
        sb.append(", isDeleted=").append(isDeleted);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}