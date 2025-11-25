package com.cloud.doctor.entity.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DiagnoseVO {
    private String userSymptom;      // 用户输入的症状
    private List<DiseaseResult> diseases; // 匹配到的疾病列表(按可能性排序)
    private String recommendDept;    // 最终推荐的科室

    @Data
    @Builder
    public static class DiseaseResult {
        private String name;         // 疾病名
        private Integer score;       // 匹配得分
        private Integer riskLevel;   // 风险等级
    }
}
