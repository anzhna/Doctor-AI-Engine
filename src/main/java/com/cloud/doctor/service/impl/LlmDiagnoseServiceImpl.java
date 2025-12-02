package com.cloud.doctor.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cloud.doctor.entity.dto.DiagnoseReq;
import com.cloud.doctor.entity.vo.DiagnoseVO;
import com.cloud.doctor.mapper.DepartmentMapper;
import com.cloud.doctor.service.DiagnoseService;
import com.cloud.doctor.entity.Department;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * AI 智能诊断服务实现类 (基于 DeepSeek 大模型)
 * 实现了 DiagnoseService 接口，用来替代旧的关键词匹配逻辑
 */
@Slf4j // 开启日志打印，方便调试
@Service("llmDiagnoseService") // 给这个 Bean 起个专门的名字，方便在 Controller 里指定注入
public class LlmDiagnoseServiceImpl implements DiagnoseService {

    @Autowired
    private DepartmentMapper departmentMapper;

    // 从 application.yml 读取配置，不要硬编码！
    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    @Value("${ai.model-name}")
    private String modelName;

    @Value("${ai.temperature}")
    private Double temperature;

    @Override
    public DiagnoseVO chat(DiagnoseReq req) {
        // 1. 先查出所有科室名称 (比如：呼吸内科, 消化内科...)
        // 实际项目中建议把这个 List 缓存到 Redis，别每次都查库
        List<String> deptNames = departmentMapper.selectList(null)
                .stream().map(Department::getName).toList();
        String deptListStr = String.join(", ", deptNames);

        // 2. 修改 Prompt
        String prompt = """
            你是一名专业的全科医生助手。用户描述症状如下：“%s”。
            
            ⚠️【严格限制】推荐科室必须从以下列表中选择一个，绝对不能创造新词：
            [%s]
            
            如果用户症状不属于以上任何科室，请返回 "全科"。
            请分析可能患有的疾病（最多3个），并推荐挂号科室。
            
            【重要】请直接返回纯 JSON 格式数据，不要包含 Markdown 标记（如 ```json），不要包含任何其他废话。
            格式严格如下：
            {
                "recommendDept": "推荐科室名称(仅1个)",
                "diseases": [
                    {"name": "疾病名称1", "score": 90, "riskLevel": 2},
                    {"name": "疾病名称2", "score": 70, "riskLevel": 1}
                ]
            }
            注：riskLevel范围1-3 (1轻微, 2一般, 3严重)，score范围0-100 (可能性)。
            """.formatted(req.symptom(),deptListStr);

        // 2. 构造请求体 (符合 OpenAI 接口标准)
        // 结构：{ "model": "...", "messages": [...], "temperature": ... }
        JSONObject userMessage = new JSONObject();
        userMessage.set("role", "user");
        userMessage.set("content", prompt);

        JSONObject requestBody = new JSONObject();
        requestBody.set("model", modelName);
        requestBody.set("messages", new JSONArray().put(userMessage));
        requestBody.set("temperature", temperature);
        // 强制让 AI 返回 JSON 模式 (DeepSeek 支持这个参数，能极大提高 JSON 成功率)
        requestBody.set("response_format", new JSONObject().set("type", "json_object"));

        try {
            log.info(">>> 正在呼叫 AI 医生，症状描述：{}", req.symptom());

            // 3. 发送 HTTP POST 请求 (使用 Hutool 工具链)
            String responseBody = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey) // 身份认证
                    .header("Content-Type", "application/json")  // 告诉 AI 我发的是 JSON
                    .body(requestBody.toString())                // 发送请求体
                    .timeout(30000)                              // 设置超时 30秒 (AI 思考需要时间)
                    .execute()                                   // 发射！
                    .body();                                     // 获取响应内容

            log.info("<<< AI 响应原始数据: {}", responseBody);

            // 4. 解析响应结果 (这一步最容易出错，要小心处理)
            // 响应结构通常是：{ "choices": [ { "message": { "content": "你的JSON结果" } } ] }
            JSONObject jsonResp = JSONUtil.parseObj(responseBody);

            // 检查有没有报错
            if (jsonResp.containsKey("error")) {
                throw new RuntimeException("AI 调用报错: " + jsonResp.getStr("error"));
            }

            // 层层剥洋葱，拿到 content 字段
            String content = jsonResp.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

            // 5. 清洗数据 (防止 AI 不听话加了 ```json ... ```)
            // 虽然我们在 prompt 里禁止了，但多做一步处理更保险
            content = content.replace("```json", "").replace("```", "").trim();

            // 6. 反序列化：把 AI 的 JSON 字符串，变成 Java 的 VO 对象
            // 这里我们先转成 JSONObject，再手动组装，最稳妥
            JSONObject aiResult = JSONUtil.parseObj(content);

            // 提取推荐科室
            String dept = aiResult.getStr("recommendDept");
            // 提取疾病列表
            JSONArray diseasesArray = aiResult.getJSONArray("diseases");
            List<DiagnoseVO.DiseaseResult> diseaseList = JSONUtil.toList(diseasesArray, DiagnoseVO.DiseaseResult.class);

            // 7. 组装最终结果并返回
            return DiagnoseVO.builder()
                    .userSymptom(req.symptom())
                    .recommendDept(dept)
                    .diseases(diseaseList)
                    .build();

        } catch (Exception e) {
            log.error("AI 问诊失败", e);
            // 8. 兜底策略 (降级)
            // 万一 AI 挂了、欠费了、超时了，不能让前端崩掉
            // 返回一个默认的提示
            return DiagnoseVO.builder()
                    .userSymptom(req.symptom())
                    .recommendDept("人工咨询台 (AI繁忙)")
                    .diseases(Collections.emptyList())
                    .build();
        }
    }
}