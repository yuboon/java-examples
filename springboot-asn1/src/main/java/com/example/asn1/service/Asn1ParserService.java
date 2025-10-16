package com.example.asn1.service;

import com.example.asn1.dto.Asn1ParseResponse;
import com.example.asn1.exception.Asn1ParseException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ASN.1解析服务类
 *
 * 
 * @version 1.0.0
 */
@Slf4j
@Service
public class Asn1ParserService {

    /**
     * 解析ASN.1数据
     *
     * @param data         ASN.1数据
     * @param encodingType 编码类型（HEX、BASE64、RAW）
     * @param verbose      是否输出详细信息
     * @return 解析结果
     */
    public Asn1ParseResponse parseAsn1Data(String data, String encodingType, boolean verbose) {
        try {
            log.debug("开始解析ASN.1数据，编码类型: {}, 数据长度: {}", encodingType, data.length());

            byte[] asn1Bytes = decodeAsn1Data(data, encodingType);

            // 尝试多种解析策略
            Asn1ParseResponse.Asn1Structure rootStructure = tryMultipleParsingStrategies(asn1Bytes, verbose);

            List<String> warnings = new ArrayList<>();
            Map<String, Object> metadata = createMetadata(asn1Bytes, encodingType);

            // 添加解析统计信息
            metadata.put("totalObjects", countTotalObjects(rootStructure));
            metadata.put("maxDepth", calculateMaxDepth(rootStructure));
            metadata.put("parsingStrategy", "multi-strategy");

            log.debug("ASN.1数据解析成功，使用多策略解析");
            return new Asn1ParseResponse(
                true,
                "ASN.1数据解析成功，使用智能解析策略",
                rootStructure,
                warnings,
                metadata
            );

        } catch (Asn1ParseException e) {
            log.error("ASN.1解析失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("ASN.1解析异常: ", e);
            throw new Asn1ParseException("ASN.1解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 尝试多种解析策略
     */
    private Asn1ParseResponse.Asn1Structure tryMultipleParsingStrategies(byte[] asn1Bytes, boolean verbose) {
        List<String> errors = new ArrayList<>();

        // 策略1: 标准ASN.1解析 - 优先使用，对于完整的ASN.1数据效果最好
        try {
            Asn1ParseResponse.Asn1Structure result = parseWithStandardStrategy(asn1Bytes, verbose);
            // 检查是否为容器结构（多对象），如果是且数据看起来像单一ASN.1结构，则尝试重解析
            if ("CONTAINER".equals(result.getTag()) && looksLikeSingleAsn1Structure(asn1Bytes)) {
                log.debug("检测到多对象容器，但数据像单一ASN.1结构，尝试单对象解析");
                return parseAsSingleObject(asn1Bytes, verbose);
            }
            return result;
        } catch (Exception e) {
            errors.add("标准解析失败: " + e.getMessage());
            log.debug("标准解析策略失败: {}", e.getMessage());
        }

        // 策略2: 容错解析
        try {
            return parseWithFaultTolerantStrategy(asn1Bytes, verbose);
        } catch (Exception e) {
            errors.add("容错解析失败: " + e.getMessage());
            log.debug("容错解析策略失败: {}", e.getMessage());
        }

        // 策略3: 分段解析
        try {
            return parseWithSegmentedStrategy(asn1Bytes, verbose);
        } catch (Exception e) {
            errors.add("分段解析失败: " + e.getMessage());
            log.debug("分段解析策略失败: {}", e.getMessage());
        }

        // 所有策略都失败，抛出异常
        throw new Asn1ParseException("所有解析策略都失败。错误详情: " + String.join("; ", errors));
    }

    /**
     * 标准解析策略 - 改进版
     */
    private Asn1ParseResponse.Asn1Structure parseWithStandardStrategy(byte[] asn1Bytes, boolean verbose) throws IOException {
        List<Asn1ParseResponse.Asn1Structure> structures = new ArrayList<>();

        try (ASN1InputStream asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(asn1Bytes))) {
            ASN1Primitive asn1Primitive;
            int offset = 0;

            while ((asn1Primitive = asn1InputStream.readObject()) != null) {
                Asn1ParseResponse.Asn1Structure structure = parseStructureWithOffset(
                    asn1Primitive, asn1Bytes, offset, verbose);
                structures.add(structure);

                // 精确计算下一个对象的偏移量
                offset = calculateNextObjectOffset(asn1Bytes, offset, asn1Primitive);

                log.debug("解析ASN.1对象: tag={}, offset={}, nextOffset={}",
                    asn1Primitive.getClass().getSimpleName(), offset, offset);
            }
        }

        if (structures.isEmpty()) {
            throw new RuntimeException("标准解析未找到有效的ASN.1结构");
        }

        return structures.size() == 1 ? structures.get(0) : createContainerStructure(structures, verbose);
    }

    /**
     * 精确解析ASN.1结构并计算偏移量
     */
    private Asn1ParseResponse.Asn1Structure parseStructureWithOffset(
            ASN1Primitive asn1, byte[] originalData, int currentOffset, boolean verbose) {

        Asn1ParseResponse.Asn1Structure structure = new Asn1ParseResponse.Asn1Structure();

        // 计算当前对象的实际长度
        int objectLength = calculateActualLength(asn1, originalData, currentOffset);

        // 基本属性设置
        structure.setOffset(currentOffset);
        structure.setLength(objectLength);

        // 根据类型解析
        if (asn1 instanceof ASN1TaggedObject) {
            parseTaggedObject((ASN1TaggedObject) asn1, structure, currentOffset, verbose);
        } else if (asn1 instanceof ASN1Sequence) {
            parseSequenceWithOffset((ASN1Sequence) asn1, structure, originalData, currentOffset, verbose);
        } else if (asn1 instanceof ASN1Set) {
            parseSetWithOffset((ASN1Set) asn1, structure, originalData, currentOffset, verbose);
        } else if (asn1 instanceof ASN1Integer) {
            parseInteger((ASN1Integer) asn1, structure, currentOffset);
        } else if (asn1 instanceof ASN1OctetString) {
            parseOctetString((ASN1OctetString) asn1, structure, currentOffset);
        } else if (asn1 instanceof DERUTF8String) {
            parseUTF8String((DERUTF8String) asn1, structure, currentOffset);
        } else if (asn1 instanceof DERPrintableString) {
            parsePrintableString((DERPrintableString) asn1, structure, currentOffset);
        } else if (asn1 instanceof ASN1ObjectIdentifier) {
            parseObjectIdentifier((ASN1ObjectIdentifier) asn1, structure, currentOffset);
        } else if (asn1 instanceof ASN1BitString) {
            parseBitString((ASN1BitString) asn1, structure, currentOffset);
        } else if (asn1 instanceof ASN1Boolean) {
            parseBoolean((ASN1Boolean) asn1, structure, currentOffset);
        } else if (asn1 instanceof ASN1Null) {
            parseNull(structure, currentOffset);
        } else if (asn1 instanceof DERIA5String) {
            parseIA5String((DERIA5String) asn1, structure, currentOffset);
        } else if (asn1 instanceof ASN1UTCTime) {
            parseUTCTime((ASN1UTCTime) asn1, structure, currentOffset);
        } else if (asn1 instanceof ASN1GeneralizedTime) {
            parseGeneralizedTime((ASN1GeneralizedTime) asn1, structure, currentOffset);
        } else {
            parseUnknown(asn1, structure, currentOffset);
        }

        // 添加详细属性
        if (verbose) {
            addVerboseProperties(asn1, structure);
        }

        return structure;
    }

    /**
     * 精确计算ASN.1对象的实际长度
     */
    private int calculateActualLength(ASN1Primitive asn1, byte[] data, int offset) {
        try {
            if (offset >= data.length) return 0;

            // 获取标签字节
            int tagByte = data[offset] & 0xFF;
            int lengthStart = offset + 1;

            // 检查是否为长格式长度编码
            if (lengthStart >= data.length) return 1;

            int lengthByte = data[lengthStart] & 0xFF;

            if ((lengthByte & 0x80) == 0) {
                // 短格式长度
                return 1 + 1 + lengthByte; // tag + length + content
            } else {
                // 长格式长度
                int lengthBytes = lengthByte & 0x7F;
                if (lengthBytes == 0 || lengthBytes > 4) {
                    // 不定长或长度字节过多，使用估算
                    return estimateLength(asn1);
                }

                if (lengthStart + lengthBytes >= data.length) {
                    return estimateLength(asn1);
                }

                // 读取长度值
                int contentLength = 0;
                for (int i = 0; i < lengthBytes; i++) {
                    contentLength = (contentLength << 8) | (data[lengthStart + 1 + i] & 0xFF);
                }

                return 1 + 1 + lengthBytes + contentLength; // tag + length_bytes + content
            }
        } catch (Exception e) {
            log.debug("计算长度失败，使用估算: {}", e.getMessage());
            return estimateLength(asn1);
        }
    }

    /**
     * 计算下一个对象的偏移量
     */
    private int calculateNextObjectOffset(byte[] data, int currentOffset, ASN1Primitive currentObject) {
        int objectLength = calculateActualLength(currentObject, data, currentOffset);
        int nextOffset = currentOffset + objectLength;

        // 确保不越界
        return Math.min(nextOffset, data.length);
    }

    /**
     * 改进的序列解析 - 精确计算子对象偏移量
     */
    private void parseSequenceWithOffset(ASN1Sequence sequence, Asn1ParseResponse.Asn1Structure structure,
            byte[] originalData, int sequenceOffset, boolean verbose) {
        structure.setTag("SEQUENCE");
        structure.setTagNumber(16);
        structure.setTagClass("UNIVERSAL");
        structure.setType("SEQUENCE");
        structure.setLength(sequence.size());
        structure.setOffset(sequenceOffset);

        List<Asn1ParseResponse.Asn1Structure> children = new ArrayList<>();

        try {
            // 获取序列的原始字节数据用于精确偏移计算
            byte[] sequenceBytes = extractSequenceBytes(originalData, sequenceOffset);
            int childOffset = sequenceOffset + getSequenceHeaderLength(originalData, sequenceOffset);

            for (Enumeration<?> e = sequence.getObjects(); e.hasMoreElements(); ) {
                ASN1Primitive element = (ASN1Primitive) e.nextElement();

                Asn1ParseResponse.Asn1Structure childStructure = parseStructureWithOffset(
                    element, originalData, childOffset, verbose);
                children.add(childStructure);

                // 更新子对象偏移量
                childOffset = calculateNextObjectOffset(originalData, childOffset, element);
            }
        } catch (Exception ex) {
            log.warn("序列偏移量计算失败，使用简化方式: {}", ex.getMessage());
            // 降级到简化方式
            int childOffset = sequenceOffset + 2;
            for (Enumeration<?> e = sequence.getObjects(); e.hasMoreElements(); ) {
                ASN1Primitive element = (ASN1Primitive) e.nextElement();
                children.add(parseStructure(element, childOffset, verbose));
                childOffset += estimateLength(element);
            }
        }

        structure.setChildren(children);
        structure.setValue(sequence.size() + " 个元素");
    }

    /**
     * 改进的集合解析
     */
    private void parseSetWithOffset(ASN1Set set, Asn1ParseResponse.Asn1Structure structure,
            byte[] originalData, int setOffset, boolean verbose) {
        structure.setTag("SET");
        structure.setTagNumber(17);
        structure.setTagClass("UNIVERSAL");
        structure.setType("SET");
        structure.setLength(set.size());
        structure.setOffset(setOffset);
        structure.setValue(set.size() + " 个元素");

        List<Asn1ParseResponse.Asn1Structure> children = new ArrayList<>();

        try {
            byte[] setBytes = extractSetBytes(originalData, setOffset);
            int childOffset = setOffset + getSetHeaderLength(originalData, setOffset);

            for (Enumeration<?> e = set.getObjects(); e.hasMoreElements(); ) {
                ASN1Primitive element = (ASN1Primitive) e.nextElement();
                Asn1ParseResponse.Asn1Structure childStructure = parseStructureWithOffset(
                    element, originalData, childOffset, verbose);
                children.add(childStructure);

                childOffset = calculateNextObjectOffset(originalData, childOffset, element);
            }
        } catch (Exception ex) {
            log.warn("集合偏移量计算失败，使用简化方式: {}", ex.getMessage());
            // 降级到简化方式
            int childOffset = setOffset + 2;
            for (Enumeration<?> e = set.getObjects(); e.hasMoreElements(); ) {
                ASN1Primitive element = (ASN1Primitive) e.nextElement();
                children.add(parseStructure(element, childOffset, verbose));
                childOffset += estimateLength(element);
            }
        }

        structure.setChildren(children);
    }

    /**
     * 提取序列的字节数据
     */
    private byte[] extractSequenceBytes(byte[] data, int offset) {
        if (offset >= data.length) return new byte[0];

        try {
            int tagByte = data[offset] & 0xFF;
            int lengthStart = offset + 1;

            if (lengthStart >= data.length) return new byte[0];

            int lengthByte = data[lengthStart] & 0xFF;
            int contentLength;
            int headerLength;

            if ((lengthByte & 0x80) == 0) {
                contentLength = lengthByte;
                headerLength = 2;
            } else {
                int lengthBytes = lengthByte & 0x7F;
                if (lengthBytes > 4 || lengthStart + lengthBytes >= data.length) {
                    return new byte[0];
                }

                contentLength = 0;
                for (int i = 0; i < lengthBytes; i++) {
                    contentLength = (contentLength << 8) | (data[lengthStart + 1 + i] & 0xFF);
                }
                headerLength = 1 + 1 + lengthBytes;
            }

            int totalLength = Math.min(headerLength + contentLength, data.length - offset);
            byte[] result = new byte[totalLength];
            System.arraycopy(data, offset, result, 0, totalLength);
            return result;

        } catch (Exception e) {
            log.debug("提取序列字节失败: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * 提取集合的字节数据
     */
    private byte[] extractSetBytes(byte[] data, int offset) {
        return extractSequenceBytes(data, offset); // SET和SEQUENCE的头部格式相同
    }

    /**
     * 获取序列头部长度
     */
    private int getSequenceHeaderLength(byte[] data, int offset) {
        if (offset >= data.length) return 2;

        int lengthByte = data[offset + 1] & 0xFF;
        if ((lengthByte & 0x80) == 0) {
            return 2; // tag + length
        } else {
            int lengthBytes = lengthByte & 0x7F;
            return 1 + 1 + Math.min(lengthBytes, 4);
        }
    }

    /**
     * 获取集合头部长度
     */
    private int getSetHeaderLength(byte[] data, int offset) {
        return getSequenceHeaderLength(data, offset);
    }

    /**
     * 容错解析策略
     */
    private Asn1ParseResponse.Asn1Structure parseWithFaultTolerantStrategy(byte[] asn1Bytes, boolean verbose) {
        List<Asn1ParseResponse.Asn1Structure> structures = new ArrayList<>();
        int position = 0;
        int maxAttempts = asn1Bytes.length;
        int attempts = 0;

        while (position < asn1Bytes.length && attempts < maxAttempts) {
            attempts++;

            try {
                // 寻找有效的ASN.1标签起始位置
                position = findNextValidTagStart(asn1Bytes, position);
                if (position >= asn1Bytes.length) break;

                // 检查剩余数据是否足够
                if (asn1Bytes.length - position < 2) {
                    break;
                }

                // 尝试从当前位置开始解析
                int remainingLength = asn1Bytes.length - position;
                byte[] segment = new byte[Math.min(remainingLength, 2000)]; // 增大片段大小
                System.arraycopy(asn1Bytes, position, segment, 0, segment.length);

                ASN1InputStream tempStream = new ASN1InputStream(new ByteArrayInputStream(segment));
                ASN1Primitive asn1Primitive = tempStream.readObject();

                if (asn1Primitive != null) {
                    // 使用精确解析
                    Asn1ParseResponse.Asn1Structure structure = parseStructureWithOffset(
                        asn1Primitive, asn1Bytes, position, verbose);
                    structures.add(structure);

                    // 精确计算下一个位置
                    position = calculateNextObjectOffset(asn1Bytes, position, asn1Primitive);

                    log.debug("容错解析成功: type={}, offset={}", asn1Primitive.getClass().getSimpleName(), position);
                } else {
                    position++;
                }
            } catch (Exception e) {
                log.debug("容错解析失败，位置 {}: {}", position, e.getMessage());
                position++;
            }
        }

        if (structures.isEmpty()) {
            throw new RuntimeException("容错解析未找到有效的ASN.1结构");
        }

        return structures.size() == 1 ? structures.get(0) : createContainerStructure(structures, verbose);
    }

    /**
     * 寻找下一个有效的ASN.1标签起始位置
     */
    private int findNextValidTagStart(byte[] data, int startPosition) {
        for (int i = startPosition; i < data.length - 1; i++) {
            int currentByte = data[i] & 0xFF;

            // 检查是否为有效的ASN.1标签
            if (isValidAsn1Tag(currentByte)) {
                // 进一步检查长度字节
                if (i + 1 < data.length) {
                    int lengthByte = data[i + 1] & 0xFF;
                    if (isValidLengthByte(lengthByte)) {
                        return i;
                    }
                }
            }
        }
        return data.length;
    }

    /**
     * 检查是否为有效的ASN.1标签
     */
    private boolean isValidAsn1Tag(int tagByte) {
        // 检查高位是否为0（universal标签）或其他有效标签格式
        return (tagByte & 0x1F) != 0x1F || // 不是长格式标签
               (tagByte & 0xC0) != 0xC0;   // 不是保留标签
    }

    /**
     * 检查是否为有效的长度字节
     */
    private boolean isValidLengthByte(int lengthByte) {
        if ((lengthByte & 0x80) == 0) {
            // 短格式长度
            return true;
        } else {
            // 长格式长度，检查长度字节数是否合理
            int lengthBytes = lengthByte & 0x7F;
            return lengthBytes > 0 && lengthBytes <= 4;
        }
    }

    /**
     * 分段解析策略 - 改进版
     */
    private Asn1ParseResponse.Asn1Structure parseWithSegmentedStrategy(byte[] asn1Bytes, boolean verbose) {
        // 使用更精确的标签查找策略
        List<Asn1ParseResponse.Asn1Structure> structures = new ArrayList<>();
        Set<Integer> processedPositions = new HashSet<>();

        // 扩展的ASN.1标签值，包含更多常见类型
        int[] commonTags = {
            0x30, // SEQUENCE
            0x31, // SET
            0x02, // INTEGER
            0x04, // OCTET STRING
            0x05, // NULL
            0x06, // OBJECT IDENTIFIER
            0x13, // PrintableString
            0x14, // T61String
            0x16, // IA5String
            0x17, // UTCTime
            0x18, // GeneralizedTime
            0x03, // BIT STRING
            0x01, // BOOLEAN
            0x0C, // UTF8String
            0x0A, // ENUMERATED
            0x19, // VisibleString
            0x1A, // BMPString
            0x1B, // UniversalString
            0x1E  // NUMERIC STRING
        };

        // 首先尝试在数据中查找常见的ASN.1结构起始点
        List<StructureCandidate> candidates = findStructureCandidates(asn1Bytes, commonTags);

        // 按优先级排序：SEQUENCE和SET优先
        candidates.sort((a, b) -> {
            if ((a.tagByte & 0xFF) == 0x30) return -1; // SEQUENCE
            if ((b.tagByte & 0xFF) == 0x30) return 1;
            if ((a.tagByte & 0xFF) == 0x31) return -1; // SET
            if ((b.tagByte & 0xFF) == 0x31) return 1;
            return Integer.compare(a.position, b.position);
        });

        // 处理找到的候选结构
        for (StructureCandidate candidate : candidates) {
            if (processedPositions.contains(candidate.position)) {
                continue; // 跳过已处理的位置
            }

            try {
                // 计算合理的解析长度
                int parseLength = calculateParseLength(asn1Bytes, candidate.position);
                parseLength = Math.min(parseLength, Math.min(5000, asn1Bytes.length - candidate.position));

                byte[] segment = new byte[parseLength];
                System.arraycopy(asn1Bytes, candidate.position, segment, 0, parseLength);

                ASN1InputStream tempStream = new ASN1InputStream(new ByteArrayInputStream(segment));
                ASN1Primitive asn1Primitive = tempStream.readObject();

                if (asn1Primitive != null) {
                    // 使用精确偏移量解析
                    Asn1ParseResponse.Asn1Structure structure = parseStructureWithOffset(
                        asn1Primitive, asn1Bytes, candidate.position, verbose);
                    structures.add(structure);

                    // 标记处理过的位置范围
                    int structureLength = calculateActualLength(asn1Primitive, asn1Bytes, candidate.position);
                    for (int i = candidate.position; i < candidate.position + structureLength && i < asn1Bytes.length; i++) {
                        processedPositions.add(i);
                    }

                    log.debug("分段解析成功: type={}, offset={}, length={}",
                        asn1Primitive.getClass().getSimpleName(), candidate.position, structureLength);
                }
            } catch (Exception e) {
                log.debug("分段解析失败，位置 {}: {}", candidate.position, e.getMessage());
            }
        }

        // 如果没有找到有效结构，尝试逐字节扫描
        if (structures.isEmpty()) {
            structures.addAll(scanByByte(asn1Bytes, verbose));
        }

        if (structures.isEmpty()) {
            throw new RuntimeException("分段解析未找到有效的ASN.1结构");
        }

        return structures.size() == 1 ? structures.get(0) : createContainerStructure(structures, verbose);
    }

    /**
     * 结构候选信息
     */
    private static class StructureCandidate {
        int position;
        int tagByte;
        int confidence;

        StructureCandidate(int position, int tagByte, int confidence) {
            this.position = position;
            this.tagByte = tagByte;
            this.confidence = confidence;
        }
    }

    /**
     * 查找结构候选
     */
    private List<StructureCandidate> findStructureCandidates(byte[] data, int[] commonTags) {
        List<StructureCandidate> candidates = new ArrayList<>();

        for (int tag : commonTags) {
            List<Integer> positions = findTagPositions(data, tag);

            for (int pos : positions) {
                // 验证候选位置的有效性
                int confidence = validateCandidate(data, pos, tag);
                if (confidence > 0) {
                    candidates.add(new StructureCandidate(pos, tag, confidence));
                }
            }
        }

        return candidates;
    }

    /**
     * 验证候选位置的有效性
     */
    private int validateCandidate(byte[] data, int position, int tagByte) {
        try {
            if (position + 1 >= data.length) {
                return 0;
            }

            int lengthByte = data[position + 1] & 0xFF;
            int confidence = 1;

            // 检查长度字节的合理性
            if ((lengthByte & 0x80) == 0) {
                // 短格式长度
                confidence += lengthByte > 0 ? 2 : 0;
            } else {
                // 长格式长度
                int lengthBytes = lengthByte & 0x7F;
                if (lengthBytes > 0 && lengthBytes <= 4) {
                    confidence += 2;
                    if (position + 1 + lengthBytes < data.length) {
                        // 检查长度值的合理性
                        int contentLength = 0;
                        for (int i = 0; i < lengthBytes; i++) {
                            contentLength = (contentLength << 8) | (data[position + 2 + i] & 0xFF);
                        }
                        if (contentLength > 0 && contentLength < 10000) {
                            confidence += 1;
                        }
                    }
                }
            }

            // 特殊标签的额外检查
            if (tagByte == 0x30 || tagByte == 0x31) { // SEQUENCE or SET
                confidence += 2; // 高优先级
            } else if (tagByte == 0x02) { // INTEGER
                confidence += 1;
            }

            return confidence;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 计算合理的解析长度
     */
    private int calculateParseLength(byte[] data, int startPosition) {
        try {
            if (startPosition >= data.length) return 100;

            int lengthByte = data[startPosition + 1] & 0xFF;
            int headerLength = 2;

            if ((lengthByte & 0x80) != 0) {
                int lengthBytes = lengthByte & 0x7F;
                if (lengthBytes > 0 && lengthBytes <= 4 && startPosition + 1 + lengthBytes < data.length) {
                    int contentLength = 0;
                    for (int i = 0; i < lengthBytes; i++) {
                        contentLength = (contentLength << 8) | (data[startPosition + 2 + i] & 0xFF);
                    }
                    headerLength = 1 + 1 + lengthBytes;
                    return headerLength + contentLength;
                }
            }

            return headerLength + Math.min(lengthByte, 1000);
        } catch (Exception e) {
            return 100;
        }
    }

    /**
     * 逐字节扫描最后手段
     */
    private List<Asn1ParseResponse.Asn1Structure> scanByByte(byte[] data, boolean verbose) {
        List<Asn1ParseResponse.Asn1Structure> structures = new ArrayList<>();

        for (int i = 0; i < Math.min(data.length - 2, 1000); i++) {
            try {
                byte[] segment = Arrays.copyOfRange(data, i, Math.min(i + 100, data.length));
                ASN1InputStream tempStream = new ASN1InputStream(new ByteArrayInputStream(segment));
                ASN1Primitive asn1Primitive = tempStream.readObject();

                if (asn1Primitive != null) {
                    Asn1ParseResponse.Asn1Structure structure = parseStructureWithOffset(
                        asn1Primitive, data, i, verbose);
                    structures.add(structure);
                    break; // 找到一个就停止
                }
            } catch (Exception e) {
                // 继续尝试
            }
        }

        return structures;
    }

    /**
     * 查找标签位置
     */
    private List<Integer> findTagPositions(byte[] data, int tag) {
        List<Integer> positions = new ArrayList<>();

        for (int i = 0; i < data.length - 1; i++) {
            if ((data[i] & 0xFF) == tag) {
                positions.add(i);
            }
        }

        return positions;
    }

    /**
     * 计算总对象数
     */
    private int countTotalObjects(Asn1ParseResponse.Asn1Structure structure) {
        int count = 1;

        if (structure.getChildren() != null) {
            for (Asn1ParseResponse.Asn1Structure child : structure.getChildren()) {
                count += countTotalObjects(child);
            }
        }

        return count;
    }

    /**
     * 解码ASN.1数据
     *
     * @param data         编码后的数据
     * @param encodingType 编码类型
     * @return 解码后的字节数组
     * @throws IOException 解码失败异常
     */
    private byte[] decodeAsn1Data(String data, String encodingType) throws IOException {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("输入数据不能为空");
        }

        data = data.trim().replaceAll("\\s+", "");

        try {
            switch (encodingType.toUpperCase()) {
                case "HEX":
                    return hexStringToByteArray(data);
                case "BASE64":
                    return Base64.getDecoder().decode(data);
                case "RAW":
                    return data.getBytes("UTF-8");
                default:
                    throw new IllegalArgumentException("不支持的编码类型: " + encodingType + "，支持的类型: HEX、BASE64、RAW");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("数据解码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 十六进制字符串转字节数组
     *
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    private byte[] hexStringToByteArray(String hex) {
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }

        byte[] bytes = new byte[hex.length() / 2];
        try {
            for (int i = 0; i < hex.length(); i += 2) {
                bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的十六进制格式: " + e.getMessage());
        }
        return bytes;
    }

    /**
     * 解析ASN.1结构
     *
     * @param asn1    ASN.1对象
     * @param offset  偏移量
     * @param verbose 是否输出详细信息
     * @return 解析后的结构
     */
    private Asn1ParseResponse.Asn1Structure parseStructure(ASN1Primitive asn1, int offset, boolean verbose) {
        Asn1ParseResponse.Asn1Structure structure = new Asn1ParseResponse.Asn1Structure();

        if (asn1 instanceof ASN1TaggedObject) {
            parseTaggedObject((ASN1TaggedObject) asn1, structure, offset, verbose);
        } else if (asn1 instanceof ASN1Sequence) {
            parseSequence((ASN1Sequence) asn1, structure, offset, verbose);
        } else if (asn1 instanceof ASN1Set) {
            parseSet((ASN1Set) asn1, structure, offset, verbose);
        } else if (asn1 instanceof ASN1Integer) {
            parseInteger((ASN1Integer) asn1, structure, offset);
        } else if (asn1 instanceof ASN1OctetString) {
            parseOctetString((ASN1OctetString) asn1, structure, offset);
        } else if (asn1 instanceof DERUTF8String) {
            parseUTF8String((DERUTF8String) asn1, structure, offset);
        } else if (asn1 instanceof DERPrintableString) {
            parsePrintableString((DERPrintableString) asn1, structure, offset);
        } else if (asn1 instanceof ASN1ObjectIdentifier) {
            parseObjectIdentifier((ASN1ObjectIdentifier) asn1, structure, offset);
        } else if (asn1 instanceof ASN1BitString) {
            parseBitString((ASN1BitString) asn1, structure, offset);
        } else if (asn1 instanceof ASN1Boolean) {
            parseBoolean((ASN1Boolean) asn1, structure, offset);
        } else if (asn1 instanceof ASN1Null) {
            parseNull(structure, offset);
        } else if (asn1 instanceof DERIA5String) {
            parseIA5String((DERIA5String) asn1, structure, offset);
        } else if (asn1 instanceof ASN1UTCTime) {
            parseUTCTime((ASN1UTCTime) asn1, structure, offset);
        } else if (asn1 instanceof ASN1GeneralizedTime) {
            parseGeneralizedTime((ASN1GeneralizedTime) asn1, structure, offset);
        } else {
            parseUnknown(asn1, structure, offset);
        }

        // 添加详细属性
        if (verbose) {
            addVerboseProperties(asn1, structure);
        }

        return structure;
    }

    /**
     * 解析标记对象
     */
    private void parseTaggedObject(ASN1TaggedObject tagged, Asn1ParseResponse.Asn1Structure structure, int offset, boolean verbose) {
        structure.setTag("TAGGED");
        structure.setTagNumber(tagged.getTagNo());
        structure.setTagClass(getTagClass(tagged.getTagClass()));
        structure.setOffset(offset);

        ASN1Primitive baseObject = tagged.getObject();
        if (baseObject instanceof ASN1OctetString && !tagged.isExplicit()) {
            structure.setType("IMPLICIT OCTET STRING");
            structure.setValue("0x" + bytesToHex(((ASN1OctetString) baseObject).getOctets()));
        } else {
            Asn1ParseResponse.Asn1Structure childStructure = parseStructure(baseObject, offset, verbose);
            structure.setType(childStructure.getType());
            structure.setValue(childStructure.getValue());
            structure.setChildren(childStructure.getChildren());
        }
    }

    /**
     * 解析序列
     */
    private void parseSequence(ASN1Sequence sequence, Asn1ParseResponse.Asn1Structure structure, int offset, boolean verbose) {
        structure.setTag("SEQUENCE");
        structure.setTagNumber(16);
        structure.setTagClass("UNIVERSAL");
        structure.setType("SEQUENCE");
        structure.setLength(sequence.size());
        structure.setOffset(offset);

        List<Asn1ParseResponse.Asn1Structure> children = new ArrayList<>();
        int childOffset = offset + 2; // 简化的偏移计算

        for (Enumeration<?> e = sequence.getObjects(); e.hasMoreElements(); ) {
            ASN1Primitive element = (ASN1Primitive) e.nextElement();
            children.add(parseStructure(element, childOffset, verbose));
            childOffset += 10; // 简化的长度计算
        }
        structure.setChildren(children);
        structure.setValue(sequence.size() + " 个元素");
    }

    /**
     * 解析集合
     */
    private void parseSet(ASN1Set set, Asn1ParseResponse.Asn1Structure structure, int offset, boolean verbose) {
        structure.setTag("SET");
        structure.setTagNumber(17);
        structure.setTagClass("UNIVERSAL");
        structure.setType("SET");
        structure.setLength(set.size());
        structure.setOffset(offset);
        structure.setValue(set.size() + " 个元素");

        List<Asn1ParseResponse.Asn1Structure> children = new ArrayList<>();
        for (Enumeration<?> e = set.getObjects(); e.hasMoreElements(); ) {
            children.add(parseStructure((ASN1Primitive) e.nextElement(), offset, verbose));
        }
        structure.setChildren(children);
    }

    /**
     * 解析整数
     */
    private void parseInteger(ASN1Integer integer, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("INTEGER");
        structure.setTagNumber(2);
        structure.setTagClass("UNIVERSAL");
        structure.setType("INTEGER");
        structure.setValue(integer.getValue().toString());
        structure.setOffset(offset);
        structure.setLength(integer.getValue().toByteArray().length);
    }

    /**
     * 解析八位字节字符串 - 增强版，支持嵌套ASN.1解析
     */
    private void parseOctetString(ASN1OctetString octetString, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("OCTET STRING");
        structure.setTagNumber(4);
        structure.setTagClass("UNIVERSAL");
        structure.setType("OCTET STRING");
        structure.setValue("0x" + bytesToHex(octetString.getOctets()));
        structure.setLength(octetString.getOctets().length);
        structure.setOffset(offset);

        // 尝试解析嵌套的ASN.1数据
        try {
            byte[] octetBytes = octetString.getOctets();
            if (octetBytes.length > 0) {
                // 检查是否可能是有效的ASN.1数据
                if (looksLikeAsn1Data(octetBytes)) {
                    List<Asn1ParseResponse.Asn1Structure> nestedStructures = new ArrayList<>();

                    try (ASN1InputStream nestedStream = new ASN1InputStream(new ByteArrayInputStream(octetBytes))) {
                        ASN1Primitive nestedAsn1;
                        int nestedOffset = 0;

                        while ((nestedAsn1 = nestedStream.readObject()) != null) {
                            Asn1ParseResponse.Asn1Structure nestedStructure = parseStructureWithOffset(
                                nestedAsn1, octetBytes, nestedOffset, false);
                            nestedStructures.add(nestedStructure);

                            // 计算下一个嵌套对象的偏移量
                            nestedOffset = calculateNextObjectOffset(octetBytes, nestedOffset, nestedAsn1);

                            // 防止无限循环
                            if (nestedOffset >= octetBytes.length) break;
                        }
                    }

                    if (!nestedStructures.isEmpty()) {
                        structure.setChildren(nestedStructures);
                        structure.setValue(nestedStructures.size() + " 个嵌套对象");
                        log.debug("OCTET STRING中解析出 {} 个嵌套ASN.1对象", nestedStructures.size());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("OCTET STRING嵌套解析失败: {}", e.getMessage());
            // 保持原始的十六进制值
        }
    }

    /**
     * 解析UTF8字符串
     */
    private void parseUTF8String(DERUTF8String utf8String, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("UTF8String");
        structure.setTagNumber(12);
        structure.setTagClass("UNIVERSAL");
        structure.setType("UTF8String");
        structure.setValue(utf8String.getString());
        structure.setOffset(offset);
        structure.setLength(utf8String.getString().getBytes().length);
    }

    /**
     * 解析可打印字符串
     */
    private void parsePrintableString(DERPrintableString printableString, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("PrintableString");
        structure.setTagNumber(19);
        structure.setTagClass("UNIVERSAL");
        structure.setType("PrintableString");
        structure.setValue(printableString.getString());
        structure.setOffset(offset);
        structure.setLength(printableString.getString().getBytes().length);
    }

    /**
     * 解析对象标识符
     */
    private void parseObjectIdentifier(ASN1ObjectIdentifier oid, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("OBJECT IDENTIFIER");
        structure.setTagNumber(6);
        structure.setTagClass("UNIVERSAL");
        structure.setType("OBJECT IDENTIFIER");
        structure.setValue(oid.getId());
        structure.setOffset(offset);
    }

    /**
     * 解析位字符串 - 增强版，支持嵌套ASN.1解析
     */
    private void parseBitString(ASN1BitString bitString, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("BIT STRING");
        structure.setTagNumber(3);
        structure.setTagClass("UNIVERSAL");
        structure.setType("BIT STRING");
        structure.setValue("0x" + bytesToHex(bitString.getBytes()));
        structure.setLength(bitString.getBytes().length);
        structure.setOffset(offset);

        // 尝试解析嵌套的ASN.1数据（如公钥、签名等）
        try {
            byte[] bitBytes = bitString.getBytes();
            if (bitBytes.length > 0) {
                // 根据BIT STRING的第一个字节判断是否跳过
                int unusedBits = bitBytes[0] & 0x07; // 最低3位表示未使用的位数
                byte[] contentBytes;

                if (unusedBits == 0) {
                    // 没有未使用的位数，解析整个字节序列
                    contentBytes = bitBytes;
                } else {
                    // 跳过未使用的位数
                    contentBytes = Arrays.copyOfRange(bitBytes, 1, bitBytes.length);
                }

                if (contentBytes.length > 0 && looksLikeAsn1Data(contentBytes)) {
                    List<Asn1ParseResponse.Asn1Structure> nestedStructures = new ArrayList<>();

                    try (ASN1InputStream nestedStream = new ASN1InputStream(new ByteArrayInputStream(contentBytes))) {
                        ASN1Primitive nestedAsn1;
                        int nestedOffset = 0;

                        while ((nestedAsn1 = nestedStream.readObject()) != null) {
                            Asn1ParseResponse.Asn1Structure nestedStructure = parseStructureWithOffset(
                                nestedAsn1, contentBytes, nestedOffset, false);
                            nestedStructures.add(nestedStructure);

                            nestedOffset = calculateNextObjectOffset(contentBytes, nestedOffset, nestedAsn1);
                            if (nestedOffset >= contentBytes.length) break;
                        }
                    }

                    if (!nestedStructures.isEmpty()) {
                        structure.setChildren(nestedStructures);
                        structure.setValue(nestedStructures.size() + " 个嵌套对象");
                        log.debug("BIT STRING中解析出 {} 个嵌套ASN.1对象", nestedStructures.size());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("BIT STRING嵌套解析失败: {}", e.getMessage());
            // 保持原始的十六进制值
        }
    }

    /**
     * 解析布尔值
     */
    private void parseBoolean(ASN1Boolean booleanObj, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("BOOLEAN");
        structure.setTagNumber(1);
        structure.setTagClass("UNIVERSAL");
        structure.setType("BOOLEAN");
        structure.setValue(booleanObj.isTrue() ? "TRUE" : "FALSE");
        structure.setOffset(offset);
        structure.setLength(1);
    }

    /**
     * 解析空值
     */
    private void parseNull(Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("NULL");
        structure.setTagNumber(5);
        structure.setTagClass("UNIVERSAL");
        structure.setType("NULL");
        structure.setValue("NULL");
        structure.setOffset(offset);
        structure.setLength(0);
    }

    /**
     * 解析IA5字符串
     */
    private void parseIA5String(DERIA5String ia5String, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("IA5String");
        structure.setTagNumber(22);
        structure.setTagClass("UNIVERSAL");
        structure.setType("IA5String");
        structure.setValue(ia5String.getString());
        structure.setOffset(offset);
        structure.setLength(ia5String.getString().getBytes().length);
    }

    /**
     * 解析UTC时间
     */
    private void parseUTCTime(ASN1UTCTime utcTime, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("UTCTime");
        structure.setTagNumber(23);
        structure.setTagClass("UNIVERSAL");
        structure.setType("UTCTime");
        structure.setValue(utcTime.getTime());
        structure.setOffset(offset);
        structure.setLength(utcTime.getTime().getBytes().length);
    }

    /**
     * 解析通用时间
     */
    private void parseGeneralizedTime(ASN1GeneralizedTime generalizedTime, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("GeneralizedTime");
        structure.setTagNumber(24);
        structure.setTagClass("UNIVERSAL");
        structure.setType("GeneralizedTime");
        structure.setValue(generalizedTime.getTime());
        structure.setOffset(offset);
        structure.setLength(generalizedTime.getTime().getBytes().length);
    }

    /**
     * 解析未知类型
     */
    private void parseUnknown(ASN1Primitive asn1, Asn1ParseResponse.Asn1Structure structure, int offset) {
        structure.setTag("UNKNOWN");
        structure.setTagNumber(-1);
        structure.setTagClass("UNKNOWN");
        structure.setType("UNKNOWN");
        structure.setValue(asn1.toString());
        structure.setOffset(offset);
    }

    /**
     * 获取标签类别
     */
    private String getTagClass(int tagClass) {
        switch (tagClass) {
            case 0:
                return "UNIVERSAL";
            case 1:
                return "APPLICATION";
            case 2:
                return "CONTEXT_SPECIFIC";
            case 3:
                return "PRIVATE";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 添加详细属性
     */
    private void addVerboseProperties(ASN1Primitive asn1, Asn1ParseResponse.Asn1Structure structure) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("className", asn1.getClass().getSimpleName());
        properties.put("hashCode", asn1.hashCode());
        structure.setProperties(properties);
    }

    /**
     * 创建元数据
     */
    private Map<String, Object> createMetadata(byte[] data, String encodingType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalLength", data.length);
        metadata.put("encodingType", encodingType);
        metadata.put("encodingTimestamp", System.currentTimeMillis());

        // 检测可能的编码规则
        String probableEncoding = detectEncodingRule(data);
        metadata.put("probableEncoding", probableEncoding);

        return metadata;
    }

    /**
     * 检测编码规则
     */
    private String detectEncodingRule(byte[] data) {
        if (data.length > 0) {
            byte firstByte = data[0];
            if ((firstByte & 0x1F) == 0x10) { // SEQUENCE tag
                if (isDerCompliant(data)) {
                    return "DER (Distinguished Encoding Rules)";
                } else {
                    return "BER (Basic Encoding Rules)";
                }
            }
        }
        return "Unknown";
    }

    /**
     * 检查是否符合DER规范
     */
    private boolean isDerCompliant(byte[] data) {
        // 简化的DER合规性检查
        if (data.length >= 2) {
            byte lengthByte = data[1];
            if ((lengthByte & 0x80) != 0) {
                int lengthBytes = lengthByte & 0x7F;
                if (lengthBytes == 1) {
                    return (data[2] & 0x80) != 0;
                }
            }
        }
        return true;
    }

    /**
     * 估算ASN.1对象的长度
     */
    private int estimateLength(ASN1Primitive asn1) {
        if (asn1 instanceof ASN1Sequence) {
            return ((ASN1Sequence) asn1).size() * 10 + 4; // 简化估算
        } else if (asn1 instanceof ASN1OctetString) {
            return ((ASN1OctetString) asn1).getOctets().length + 2;
        } else if (asn1 instanceof ASN1Integer) {
            return ((ASN1Integer) asn1).getValue().toByteArray().length + 2;
        } else {
            return 10; // 默认估算长度
        }
    }

    /**
     * 创建容器结构包含多个ASN.1对象
     */
    private Asn1ParseResponse.Asn1Structure createContainerStructure(List<Asn1ParseResponse.Asn1Structure> structures, boolean verbose) {
        Asn1ParseResponse.Asn1Structure container = new Asn1ParseResponse.Asn1Structure();
        container.setTag("CONTAINER");
        container.setTagNumber(-1);
        container.setTagClass("CONTAINER");
        container.setType("MULTIPLE_OBJECTS");
        container.setValue(structures.size() + " 个顶级对象");
        container.setOffset(0);
        container.setLength(structures.size());
        container.setChildren(structures);

        if (verbose) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("className", "Container");
            properties.put("objectCount", structures.size());
            container.setProperties(properties);
        }

        return container;
    }

    /**
     * 检查字节数组是否看起来像ASN.1数据
     */
    private boolean looksLikeAsn1Data(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }

        try {
            // 检查第一个字节是否为有效的ASN.1标签
            int firstByte = data[0] & 0xFF;

            // 常见的ASN.1标签值
            int[] commonTags = {
                0x30, // SEQUENCE
                0x31, // SET
                0x02, // INTEGER
                0x04, // OCTET STRING
                0x05, // NULL
                0x06, // OBJECT IDENTIFIER
                0x13, // PrintableString
                0x16, // IA5String
                0x17, // UTCTime
                0x18, // GeneralizedTime
                0x03, // BIT STRING
                0x01, // BOOLEAN
                0x0C, // UTF8String
            };

            boolean isValidTag = false;
            for (int tag : commonTags) {
                if (firstByte == tag) {
                    isValidTag = true;
                    break;
                }
            }

            if (!isValidTag) {
                return false;
            }

            // 检查长度字节是否合理
            int lengthByte = data[1] & 0xFF;

            if ((lengthByte & 0x80) == 0) {
                // 短格式长度
                return lengthByte <= data.length - 2;
            } else {
                // 长格式长度
                int lengthBytes = lengthByte & 0x7F;
                if (lengthBytes == 0 || lengthBytes > 4) {
                    return false;
                }

                if (2 + lengthBytes >= data.length) {
                    return false;
                }

                // 读取长度值
                int contentLength = 0;
                for (int i = 0; i < lengthBytes; i++) {
                    contentLength = (contentLength << 8) | (data[2 + i] & 0xFF);
                }

                // 检查内容长度是否合理
                return contentLength > 0 && contentLength <= 10000 &&
                       (2 + lengthBytes + contentLength) <= data.length;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算结构的最大深度
     */
    private int calculateMaxDepth(Asn1ParseResponse.Asn1Structure structure) {
        if (structure.getChildren() == null || structure.getChildren().isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;
        for (Asn1ParseResponse.Asn1Structure child : structure.getChildren()) {
            int childDepth = calculateMaxDepth(child);
            if (childDepth > maxChildDepth) {
                maxChildDepth = childDepth;
            }
        }

        return maxChildDepth + 1;
    }

    /**
     * 检查数据是否看起来像单一ASN.1结构
     */
    private boolean looksLikeSingleAsn1Structure(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }

        try {
            // 检查是否以常见的ASN.1结构标签开头
            int firstByte = data[0] & 0xFF;

            // 单一ASN.1结构通常以这些标签开头
            if (firstByte == 0x30) { // SEQUENCE
                // 检查长度是否匹配整个数据
                int lengthByte = data[1] & 0xFF;
                int expectedLength;

                if ((lengthByte & 0x80) == 0) {
                    expectedLength = 2 + lengthByte; // tag + length + content
                } else {
                    int lengthBytes = lengthByte & 0x7F;
                    if (lengthBytes == 0 || lengthBytes > 4 || 2 + lengthBytes >= data.length) {
                        return false;
                    }

                    int contentLength = 0;
                    for (int i = 0; i < lengthBytes; i++) {
                        contentLength = (contentLength << 8) | (data[2 + i] & 0xFF);
                    }
                    expectedLength = 2 + lengthBytes + contentLength;
                }

                return expectedLength == data.length;
            }

            // 其他单一结构检查...
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将数据作为单一ASN.1对象解析
     */
    private Asn1ParseResponse.Asn1Structure parseAsSingleObject(byte[] asn1Bytes, boolean verbose) throws IOException {
        try (ASN1InputStream asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(asn1Bytes))) {
            ASN1Primitive asn1Primitive = asn1InputStream.readObject();

            if (asn1Primitive == null) {
                throw new RuntimeException("无法解析ASN.1对象");
            }

            Asn1ParseResponse.Asn1Structure structure = parseStructureWithOffset(
                asn1Primitive, asn1Bytes, 0, verbose);

            log.debug("单对象解析成功: {}", asn1Primitive.getClass().getSimpleName());
            return structure;
        }
    }
}