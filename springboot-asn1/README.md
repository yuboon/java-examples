# SpringBoot ASN.1在线解析工具

基于SpringBoot和BouncyCastle实现的ASN.1在线解析工具，支持多种ASN.1编码格式的解析和可视化展示。


# 📋 支持的ASN.1类型

| 类型 | 描述 | 示例 |
|------|------|------|
| SEQUENCE | 序列类型 | `3009...` |
| SET | 集合类型 | `3109...` |
| INTEGER | 整数类型 | `020101` |
| OCTET STRING | 八位字节串 | `04048899aabb` |
| UTF8String | UTF-8字符串 | `0c0548656c6c6f` |
| PrintableString | 可打印字符串 | `130548656c6c6f` |
| OBJECT IDENTIFIER | 对象标识符 | `06032a0304` |
| BIT STRING | 位串 | `030200ff` |
| BOOLEAN | 布尔值 | `0101ff` |
| NULL | 空值 | `0500` |
| UTCTime | UTC时间 | `170d32333031303132303539305a` |
| GeneralizedTime | 通用时间 | `18113332333031303132303539305a` |
