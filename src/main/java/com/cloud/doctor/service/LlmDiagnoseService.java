package com.cloud.doctor.service;

import com.cloud.doctor.entity.dto.DiagnoseReq;
import com.cloud.doctor.entity.vo.DiagnoseVO;

public interface LlmDiagnoseService {

    DiagnoseVO chat(DiagnoseReq req);
}
