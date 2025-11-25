package com.cloud.doctor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Disease;
import com.cloud.doctor.service.DiseaseService;
import com.cloud.doctor.mapper.DiseaseMapper;
import org.springframework.stereotype.Service;

/**
* @author 10533
* @description 针对表【ai_disease(疾病知识库)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:11
*/
@Service
public class DiseaseServiceImpl extends ServiceImpl<DiseaseMapper, Disease>
    implements DiseaseService{

}




