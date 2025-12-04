package com.cloud.doctor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.doctor.entity.Disease;
import com.cloud.doctor.entity.DiseaseSymptomRel;
import com.cloud.doctor.entity.Symptom;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.entity.dto.DiagnoseReq;
import com.cloud.doctor.entity.vo.DiagnoseVO;
import com.cloud.doctor.mapper.DiseaseMapper;
import com.cloud.doctor.mapper.DiseaseSymptomRelMapper;
import com.cloud.doctor.mapper.SymptomMapper;
import com.cloud.doctor.mapper.DepartmentMapper;
import com.cloud.doctor.service.DiagnoseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnoseServiceImpl implements DiagnoseService {

    private final SymptomMapper symptomMapper;
    private final DiseaseMapper diseaseMapper;
    private final DiseaseSymptomRelMapper relMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public DiagnoseVO chat(DiagnoseReq req) {
        String text = req.symptom(); // 用户输入的描述

        // 分词匹配 拿到数据库里所有的症状标签，看用户输入里包含了哪些
        List<Symptom> allSymptoms = symptomMapper.selectList(null);
        List<Symptom> matchedSymptoms = allSymptoms.stream()
                .filter(s -> text.contains(s.getName()))
                .toList();

        if (matchedSymptoms.isEmpty()) {
            return DiagnoseVO.builder()
                    .userSymptom(text)
                    .recommendDept("全科 / 咨询台") // 没匹配到，兜底
                    .diseases(Collections.emptyList())
                    .build();
        }

        // 计算疾病得分
        Map<Long, Integer> scoreMap = new HashMap<>();

        for (Symptom symptom : matchedSymptoms) {
            // 查出这个症状关联了哪些病
            List<DiseaseSymptomRel> rels = relMapper.selectList(new LambdaQueryWrapper<DiseaseSymptomRel>()
                    .eq(DiseaseSymptomRel::getSymptomId, symptom.getId()));

            for (DiseaseSymptomRel rel : rels) {
                // 累加分数 (基础分1 + 权重分)
                Long diseaseId = rel.getDiseaseId();
                int weight = rel.getWeight() == null ? 10 : rel.getWeight();
                scoreMap.merge(diseaseId, weight, Integer::sum);
            }
        }

        // 取出得分最高的 Top 3 疾病
        List<DiagnoseVO.DiseaseResult> results = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()) // 按分数倒序
                .limit(3)
                .map(entry -> {
                    Disease d = diseaseMapper.selectById(entry.getKey());
                    return DiagnoseVO.DiseaseResult.builder()
                            .name(d.getName())
                            .score(entry.getValue())
                            .riskLevel(d.getRiskLevel())
                            .build();
                })
                .collect(Collectors.toList());

        // 取得分最高的那个病的科室
        String deptName = "未知科室";
        if (!results.isEmpty()) {
            // 重新查一次最高分疾病的详情，为了拿 deptId
            Long topDiseaseId = scoreMap.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            Disease topDisease = diseaseMapper.selectById(topDiseaseId);
            Department dept = departmentMapper.selectById(topDisease.getRecommendDeptId());
            if (dept != null) deptName = dept.getName();
        }

        return DiagnoseVO.builder()
                .userSymptom(text)
                .diseases(results)
                .recommendDept(deptName)
                .build();
    }
}
