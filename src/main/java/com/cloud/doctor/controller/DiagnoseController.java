package com.cloud.doctor.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.dto.DiagnoseReq;
import com.cloud.doctor.entity.vo.DiagnoseVO;
import com.cloud.doctor.service.DiagnoseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diagnose")
@Tag(name = "AI 智能问诊模块")
@RequiredArgsConstructor
public class DiagnoseController {

    private final DiagnoseService diagnoseService;

    @PostMapping("/chat")
    @Operation(summary = "智能问诊(关键词匹配版)")
    public Result<DiagnoseVO> chat(@RequestBody DiagnoseReq req) {
        DiagnoseVO vo = diagnoseService.chat(req);
        return Result.success(vo);
    }
}
