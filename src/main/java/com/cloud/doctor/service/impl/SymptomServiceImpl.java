package com.cloud.doctor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Symptom;
import com.cloud.doctor.service.SymptomService;
import com.cloud.doctor.mapper.SymptomMapper;
import org.springframework.stereotype.Service;

/**
* @author 10533
* @description 针对表【ai_symptom(症状标签库)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:11
*/
@Service
public class SymptomServiceImpl extends ServiceImpl<SymptomMapper, Symptom>
    implements SymptomService{

}




