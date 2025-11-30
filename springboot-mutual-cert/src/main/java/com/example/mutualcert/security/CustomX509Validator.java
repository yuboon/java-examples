package com.example.mutualcert.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CustomX509Validator {

    private static final Logger logger = LoggerFactory.getLogger(CustomX509Validator.class);

    // 黑名单存储（实际项目中应该从数据库或配置文件读取）
    private final Set<String> blacklistedSerialNumbers = new HashSet<>(Arrays.asList(
        "1234567890ABCDEF1234567890ABCDEF",
        "FEDCBA0987654321FEDCBA0987654321"
    ));

    private final Set<String> blacklistedSubjectDNs = new HashSet<>(Arrays.asList(
        "CN=BlacklistedClient, OU=Blacklisted Dept, O=Blacklisted Corp, C=US",
        "CN=RevokedClient, OU=Revoked Dept, O=Revoked Corp, C=CN"
    ));

    // 允许的组织列表（白名单）
    private final Set<String> allowedOrganizations = new HashSet<>(Arrays.asList(
        "DemoCompany"
    ));

    // 允许的CA序列号（用于验证证书链）
    private final Set<String> trustedRootCASerials = new HashSet<>(Arrays.asList(
        "3EE5FAF49D1073C87F738E69DF71A8E1CBA0752"  // 我们的根CA序列号
    ));

    // 吊销证书的CRL文件路径（生产环境中应该配置）
    private final String crlPath = null; // 配置为null，使用在线检查替代

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 验证客户端证书 - 优化版本
     * 实现真正的证书链验证、黑名单检查和签名验证
     */
    public ValidationResult validateCertificate(X509Certificate cert) {
        ValidationResult result = new ValidationResult();

        try {
            logger.info("开始证书验证: {}", cert.getSubjectX500Principal().getName());

            // 1. 证书有效期验证
            validateCertificateValidity(cert, result);

            // 2. 证书黑名单检查
            validateBlacklist(cert, result);

            // 3. 证书链验证（真正的签名验证）
            validateCertificateChain(cert, result);

            // 4. 证书组织验证
            validateOrganization(cert, result);

            // 5. 证书密钥强度验证
            validateKeyStrength(cert, result);

            // 6. 证书扩展验证
            //validateExtensions(cert, result);

            // 7. 总体验证结果
            if (!result.hasErrors()) {
                result.setValid(true);
                result.addInfo("证书验证通过 - 所有检查均通过");
            }

        } catch (Exception e) {
            result.addError("证书验证过程中发生异常: " + e.getMessage());
            logger.error("证书验证异常", e);
        }

        // 记录验证结果
        logValidationResult(cert, result);

        return result;
    }

    /**
     * 1. 验证证书有效期
     */
    private void validateCertificateValidity(X509Certificate cert, ValidationResult result) {
        try {
            cert.checkValidity();
            Date now = new Date();
            Date notAfter = cert.getNotAfter();
            long daysUntilExpiry = (notAfter.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);

            result.addInfo(String.format("证书有效期验证通过 - 剩余%d天", daysUntilExpiry));

            if (daysUntilExpiry < 30) {
                result.addWarning(String.format("证书将在%d天后过期，请及时更新", daysUntilExpiry));
            }
        } catch (CertificateExpiredException e) {
            result.addError("证书已过期: " + e.getMessage());
        } catch (CertificateNotYetValidException e) {
            result.addError("证书尚未生效: " + e.getMessage());
        }
    }

    /**
     * 2. 验证证书黑名单
     */
    private void validateBlacklist(X509Certificate cert, ValidationResult result) {
        // 自定义黑名单规则校验
        String serialNumber = cert.getSerialNumber().toString(16).toUpperCase();
        if (blacklistedSerialNumbers.contains(serialNumber)) {
            result.addError("证书序列号在黑名单中: " + serialNumber);
            return;
        }

        String subjectDN = cert.getSubjectX500Principal().getName();
        if (blacklistedSubjectDNs.contains(subjectDN)) {
            result.addError("证书主题在黑名单中: " + subjectDN);
            return;
        }

        //todo 基于CRL、OCSP等进行在线检查

        result.addInfo("证书黑名单验证通过");
    }

    /**
     * 3. 验证证书链
     */
    private void validateCertificateChain(X509Certificate cert, ValidationResult result) {
        try {
            // 从classpath加载根CA证书
            X509Certificate rootCACert = null;
            try (InputStream caCertStream = getClass().getClassLoader().getResourceAsStream("root-ca-cert.pem")) {
                if (caCertStream == null) {
                    throw new RuntimeException("找不到根CA证书文件 root-ca-cert.pem");
                }
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                rootCACert = (X509Certificate) cf.generateCertificate(caCertStream);
            }

            // 验证客户端证书是否由根CA签署
            try {
                cert.verify(rootCACert.getPublicKey());
                result.addInfo("客户端证书签名验证通过 - 使用根CA公钥验证成功");
            } catch (SignatureException e) {
                result.addError("客户端证书签名验证失败: 证书不是由可信CA签发的");
                return;
            } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e) {
                result.addError("客户端证书签名验证失败: 技术错误 - " + e.getMessage());
                return;
            }

            // 验证根CA证书的有效性
            try {
                rootCACert.checkValidity();
                result.addInfo("根CA证书本身有效");
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                result.addError("根CA证书无效: " + e.getMessage());
                return;
            }

            /*// 检查根CA序列号
            String rootCASerial = rootCACert.getSerialNumber().toString(16).toUpperCase();
            if (!trustedRootCASerials.contains(rootCASerial)) {
                result.addError("根CA序列号不在信任列表中: " + rootCASerial);
                return;
            }*/

            // 更智能的颁发者信息检查 - 解析DN而不是简单的字符串比较
            String issuerDN = cert.getIssuerX500Principal().getName();
            String rootCAIssuerDN = rootCACert.getSubjectX500Principal().getName();

            // 比较颁发者和根CA的DN字段
            boolean isIssuerValid = compareDistinguishedNames(issuerDN, rootCAIssuerDN);

            if (isIssuerValid) {
                result.addInfo("证书颁发者DN与根CA完全匹配");
            } else {
                result.addWarning("证书颁发者DN与根CA基本匹配但不完全一致");
            }

            result.addInfo("证书链验证通过");

        } catch (Exception e) {
            result.addError("证书链验证失败: " + e.getMessage());
            logger.error("证书链验证异常", e);
        }
    }

    /**
     * 智能比较两个DN是否相等，忽略顺序和格式差异
     */
    private boolean compareDistinguishedNames(String dn1, String dn2) {
        if (dn1 == null || dn2 == null) {
            return false;
        }

        if (dn1.equals(dn2)) {
            return true;
        }

        // 解析DN为字段映射
        Map<String, String> dnFields1 = parseDN(dn1);
        Map<String, String> dnFields2 = parseDN(dn2);

        // 比较字段是否相等
        return dnFields1.equals(dnFields2);
    }

    /**
     * 解析DN字符串为字段映射
     */
    private Map<String, String> parseDN(String dn) {
        Map<String, String> fields = new TreeMap<>(); // 使用TreeMap保持顺序一致

        String[] parts = dn.split(",");
        for (String part : parts) {
            String trimmedPart = part.trim();
            if (trimmedPart.isEmpty()) {
                continue;
            }

            int equalsIndex = trimmedPart.indexOf('=');
            if (equalsIndex != -1) {
                String key = trimmedPart.substring(0, equalsIndex).trim().toUpperCase();
                String value = trimmedPart.substring(equalsIndex + 1).trim();
                fields.put(key, value);
            }
        }

        return fields;
    }

    /**
     * 4. 验证证书组织
     */
    private void validateOrganization(X509Certificate cert, ValidationResult result) {
        String subject = cert.getSubjectX500Principal().getName();
        String organization = extractOrganization(subject);

        if (!allowedOrganizations.contains(organization)) {
            result.addError("证书组织不在允许列表中: " + organization);
            return;
        }

        result.addInfo("证书组织验证通过: " + organization);
    }

    /**
     * 5. 验证证书密钥强度
     */
    private void validateKeyStrength(X509Certificate cert, ValidationResult result) {
        try {
            PublicKey publicKey = cert.getPublicKey();

            if (publicKey instanceof RSAPublicKey) {
                RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
                int keyLength = rsaPublicKey.getModulus().bitLength();

                if (keyLength < 2048) {
                    result.addWarning("RSA密钥长度较短: " + keyLength + "位，建议使用2048位或更高");
                } else if (keyLength >= 4096) {
                    result.addInfo("RSA密钥强度高: " + keyLength + "位");
                } else {
                    result.addInfo("RSA密钥强度适中: " + keyLength + "位");
                }

                result.addInfo("密钥强度验证通过");
            } else {
                result.addWarning("非RSA密钥，使用其他算法: " + publicKey.getAlgorithm());
            }
        } catch (Exception e) {
            result.addError("密钥强度验证失败: " + e.getMessage());
        }
    }

    /**
     * 6. 验证证书扩展
     */
    private void validateExtensions(X509Certificate cert, ValidationResult result) {
        try {
            boolean[] keyUsage = cert.getKeyUsage();
            if (keyUsage != null) {
                List<String> usages = new ArrayList<>();
                if (keyUsage[0]) usages.add("digitalSignature");
                if (keyUsage[1]) usages.add("nonRepudiation");
                if (keyUsage[2]) usages.add("keyEncipherment");
                if (keyUsage[3]) usages.add("dataEncipherment");
                if (keyUsage[4]) usages.add("keyAgreement");
                if (keyUsage[5]) usages.add("keyCertSign");
                if (keyUsage[6]) usages.add("cRLSign");
                if (keyUsage[7]) usages.add("encipherOnly");
                if (keyUsage[8]) usages.add("decipherOnly");

                result.addInfo("密钥用途: " + String.join(", ", usages));
            }

            List<String> extendedKeyUsage = cert.getExtendedKeyUsage();
            if (extendedKeyUsage != null) {
                result.addInfo("扩展密钥用途: " + String.join(", ", extendedKeyUsage));

                if (!extendedKeyUsage.contains("clientAuth")) {
                    result.addWarning("证书缺少客户端认证用途 (clientAuth)");
                }
            }

            result.addInfo("证书扩展验证通过");

        } catch (Exception e) {
            result.addError("证书扩展验证失败: " + e.getMessage());
        }
    }

    /**
     * 计算SHA-256指纹
     */
    private String calculateSHA256Fingerprint(X509Certificate cert) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] derEncoded = cert.getEncoded();
        byte[] digest = md.digest(derEncoded);

        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString().toUpperCase();
    }

    /**
     * 从证书主题中提取组织信息
     */
    private String extractOrganization(String subject) {
        Map<String, String> dnFields = parseDN(subject);
        return dnFields.getOrDefault("O", "Unknown");
    }

    /**
     * 从证书主题中提取通用名(CN)
     */
    private String extractCN(String subject) {
        Map<String, String> dnFields = parseDN(subject);
        return dnFields.getOrDefault("CN", "Unknown");
    }

    /**
     * 记录验证结果
     */
    private void logValidationResult(X509Certificate cert, ValidationResult result) {
        try {
            String subject = cert.getSubjectX500Principal().getName();
            String issuer = cert.getIssuerX500Principal().getName();
            String serialNumber = cert.getSerialNumber().toString(16);

            logger.info("证书验证完成 - 主题: {}, 颁发者: {}, 序列号: {}", subject, issuer, serialNumber);
            logger.info("验证结果: {}", result.isValid() ? "通过" : "失败");

            if (result.hasErrors()) {
                logger.error("证书验证错误: {}", result.getErrors());
            }

            if (result.hasWarnings()) {
                logger.warn("证书验证警告: {}", result.getWarnings());
            }

        } catch (Exception e) {
            logger.error("记录验证结果失败", e);
        }
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid = false;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> info = new ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void addError(String error) {
            this.errors.add(error);
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public List<String> getInfo() {
            return info;
        }

        public void addInfo(String info) {
            this.info.add(info);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}