package com.example.asn1.controller;

import com.example.asn1.dto.Asn1ParseRequest;
import com.example.asn1.dto.Asn1ParseResponse;
import com.example.asn1.service.Asn1ParserService;
import com.example.asn1.exception.Asn1ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

/**
 * ASN.1解析控制器
 *
 * @author SpringBoot
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/asn1")
@Validated
public class Asn1Controller {

    private final Asn1ParserService asn1ParserService;

    /**
     * 构造函数
     *
     * @param asn1ParserService ASN.1解析服务
     */
    public Asn1Controller(Asn1ParserService asn1ParserService) {
        this.asn1ParserService = asn1ParserService;
    }

    /**
     * 解析ASN.1数据
     *
     * @param request 解析请求
     * @return 解析结果
     */
    @PostMapping("/parse")
    public ResponseEntity<Asn1ParseResponse> parseAsn1(
            @Valid @RequestBody Asn1ParseRequest request) {

        log.info("收到ASN.1解析请求，编码类型: {}, 详细输出: {}",
                request.getEncodingType(), request.isVerbose());

        try {
            Asn1ParseResponse response = asn1ParserService.parseAsn1Data(
                request.getAsn1Data(),
                request.getEncodingType(),
                request.isVerbose()
            );

            log.info("ASN.1解析成功");
            return ResponseEntity.ok(response);

        } catch (Asn1ParseException e) {
            log.warn("ASN.1解析失败: {}", e.getMessage());
            Asn1ParseResponse errorResponse = createErrorResponse(e.getMessage(), e.getErrorCode());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("ASN.1解析异常: ", e);
            Asn1ParseResponse errorResponse = createErrorResponse("服务器内部错误", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取ASN.1解析器信息
     *
     * @return 解析器信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getAsn1Info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "SpringBoot ASN.1在线解析工具");
        info.put("version", "1.0.0");
        info.put("description", "支持多种ASN.1编码格式的在线解析工具");
        info.put("supportedEncodings", Arrays.asList("HEX", "BASE64", "RAW"));
        info.put("supportedTypes", Arrays.asList(
            "SEQUENCE", "SET", "INTEGER", "OCTET STRING",
            "UTF8String", "PrintableString", "OBJECT IDENTIFIER",
            "BIT STRING", "BOOLEAN", "NULL", "IA5String",
            "UTCTime", "GeneralizedTime", "TAGGED"
        ));
        info.put("encodingRules", Arrays.asList("BER", "DER"));
        info.put("library", "Bouncy Castle");
        info.put("bouncyCastleVersion", "1.75");

        return ResponseEntity.ok(info);
    }

    /**
     * 健康检查接口
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("application", "SpringBoot ASN.1 Parser");
        return ResponseEntity.ok(health);
    }

    /**
     * 获取支持的编码类型
     *
     * @return 支持的编码类型列表
     */
    @GetMapping("/encodings")
    public ResponseEntity<Map<String, Object>> getSupportedEncodings() {
        Map<String, Object> encodings = new HashMap<>();

        Map<String, String> hexEncoding = new HashMap<>();
        hexEncoding.put("name", "HEX");
        hexEncoding.put("description", "十六进制编码");

        Map<String, String> base64Encoding = new HashMap<>();
        base64Encoding.put("name", "BASE64");
        base64Encoding.put("description", "Base64编码");

        Map<String, String> rawEncoding = new HashMap<>();
        rawEncoding.put("name", "RAW");
        rawEncoding.put("description", "原始字符串编码");

        encodings.put("encodings", Arrays.asList(hexEncoding, base64Encoding, rawEncoding));
        encodings.put("default", "HEX");
        return ResponseEntity.ok(encodings);
    }

    /**
     * 获取示例数据
     *
     * @return 示例数据列表
     */
    @GetMapping("/samples")
    public ResponseEntity<Map<String, Object>> getSampleData() {
        Map<String, Object> samples = new HashMap<>();

        Map<String, Object> integerSample = new HashMap<>();
        integerSample.put("name", "整数");
        integerSample.put("data", "020101");
        integerSample.put("encoding", "HEX");
        integerSample.put("description", "简单的ASN.1整数，值为1");
        samples.put("integer", integerSample);

        Map<String, Object> sequenceSample = new HashMap<>();
        sequenceSample.put("name", "序列");
        sequenceSample.put("data", "3009020101020101020101");
        sequenceSample.put("encoding", "HEX");
        sequenceSample.put("description", "包含三个整数的序列");
        samples.put("sequence", sequenceSample);

        Map<String, Object> utf8Sample = new HashMap<>();
        utf8Sample.put("name", "UTF8字符串");
        utf8Sample.put("data", "0c0548656c6c6f");
        utf8Sample.put("encoding", "HEX");
        utf8Sample.put("description", "UTF8编码的字符串'Hello'");
        samples.put("utf8string", utf8Sample);

        Map<String, Object> oidSample = new HashMap<>();
        oidSample.put("name", "对象标识符");
        oidSample.put("data", "06032a0304");
        oidSample.put("encoding", "HEX");
        oidSample.put("description", "OID 1.2.3.4");
        samples.put("oid", oidSample);

        return ResponseEntity.ok(samples);
    }

    /**
     * 创建错误响应
     *
     * @param message  错误消息
     * @param errorCode 错误代码
     * @return 错误响应对象
     */
    private Asn1ParseResponse createErrorResponse(String message, String errorCode) {
        Asn1ParseResponse errorResponse = new Asn1ParseResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage(message);
        errorResponse.setRootStructure(null);
        errorResponse.setWarnings(null);
        errorResponse.setMetadata(Map.of(
            "errorCode", errorCode,
            "timestamp", System.currentTimeMillis()
        ));
        return errorResponse;
    }
}