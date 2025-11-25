package com.cloud.doctor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 医生排班号源表
 * @TableName bus_schedule
 */
@TableName(value ="bus_schedule")
@Data
public class Schedule implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 排班日期
     */
    private LocalDate workDate;

    /**
     * 班次: 1-上午, 2-下午, 3-晚班
     */
    private Integer shiftType;

    /**
     * 总号源
     */
    private Integer totalQuota;

    /**
     * 剩余号源
     */
    private Integer remainingQuota;

    /**
     * 乐观锁版本号 (防超卖核心)
     */
    private Integer version;

    /**
     * 状态: 1-正常, 0-停诊
     */
    private Integer status;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     * 
     */
    private LocalDateTime updateTime;

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
        Schedule other = (Schedule) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getDoctorId() == null ? other.getDoctorId() == null : this.getDoctorId().equals(other.getDoctorId()))
            && (this.getWorkDate() == null ? other.getWorkDate() == null : this.getWorkDate().equals(other.getWorkDate()))
            && (this.getShiftType() == null ? other.getShiftType() == null : this.getShiftType().equals(other.getShiftType()))
            && (this.getTotalQuota() == null ? other.getTotalQuota() == null : this.getTotalQuota().equals(other.getTotalQuota()))
            && (this.getRemainingQuota() == null ? other.getRemainingQuota() == null : this.getRemainingQuota().equals(other.getRemainingQuota()))
            && (this.getVersion() == null ? other.getVersion() == null : this.getVersion().equals(other.getVersion()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getDoctorId() == null) ? 0 : getDoctorId().hashCode());
        result = prime * result + ((getWorkDate() == null) ? 0 : getWorkDate().hashCode());
        result = prime * result + ((getShiftType() == null) ? 0 : getShiftType().hashCode());
        result = prime * result + ((getTotalQuota() == null) ? 0 : getTotalQuota().hashCode());
        result = prime * result + ((getRemainingQuota() == null) ? 0 : getRemainingQuota().hashCode());
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", doctorId=").append(doctorId);
        sb.append(", workDate=").append(workDate);
        sb.append(", shiftType=").append(shiftType);
        sb.append(", totalQuota=").append(totalQuota);
        sb.append(", remainingQuota=").append(remainingQuota);
        sb.append(", version=").append(version);
        sb.append(", status=").append(status);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}