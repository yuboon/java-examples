package com.demo.shamir.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

/**
 * Shamir Secret Sharing 算法实现
 * 基于拉格朗日插值和有限域运算
 */
public class ShamirUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    // 使用一个大素数作为有限域的模
    private static final BigInteger PRIME = new BigInteger(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);

    /**
     * 密钥份额
     */
    public static class Share {
        private final int x; // 份额的 x 坐标
        private final BigInteger y; // 份额的 y 坐标

        public Share(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public BigInteger getY() {
            return y;
        }

        /**
         * 编码为 Base64 字符串，格式：x:y(hex)
         */
        public String encode() {
            return x + ":" + y.toString(16);
        }

        /**
         * 从编码字符串解码
         */
        public static Share decode(String encoded) {
            String[] parts = encoded.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid share format");
            }
            int x = Integer.parseInt(parts[0]);
            BigInteger y = new BigInteger(parts[1], 16);
            return new Share(x, y);
        }

        @Override
        public String toString() {
            return "Share{x=" + x + ", y=" + y.toString(16) + "}";
        }
    }

    /**
     * 拆分密钥
     *
     * @param secret    原始密钥（字节数组）
     * @param n         总份额数
     * @param threshold 门限值（至少需要多少份才能恢复）
     * @return 密钥份额列表
     */
    public static List<Share> split(byte[] secret, int n, int threshold) {
        if (threshold > n) {
            throw new IllegalArgumentException("Threshold cannot be greater than total shares");
        }
        if (threshold < 2) {
            throw new IllegalArgumentException("Threshold must be at least 2");
        }

        // 将密钥转换为 BigInteger
        BigInteger secretInt = new BigInteger(1, secret);

        // 确保密钥小于素数
        if (secretInt.compareTo(PRIME) >= 0) {
            throw new IllegalArgumentException("Secret is too large");
        }

        // 生成随机多项式系数：f(x) = a0 + a1*x + a2*x^2 + ... + a(t-1)*x^(t-1)
        // 其中 a0 = secret
        BigInteger[] coefficients = new BigInteger[threshold];
        coefficients[0] = secretInt;
        for (int i = 1; i < threshold; i++) {
            coefficients[i] = new BigInteger(PRIME.bitLength(), RANDOM).mod(PRIME);
        }

        // 生成 n 个份额
        List<Share> shares = new ArrayList<>();
        for (int x = 1; x <= n; x++) {
            BigInteger y = evaluatePolynomial(coefficients, x);
            shares.add(new Share(x, y));
        }

        return shares;
    }

    /**
     * 恢复密钥
     *
     * @param shares 至少 threshold 个份额
     * @return 原始密钥（字节数组）
     */
    public static byte[] combine(List<Share> shares) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Shares list cannot be empty");
        }

        // 使用拉格朗日插值恢复多项式在 x=0 处的值（即 a0，也就是密钥）
        BigInteger secret = lagrangeInterpolate(shares);

        return secret.toByteArray();
    }

    /**
     * 计算多项式在 x 处的值
     */
    private static BigInteger evaluatePolynomial(BigInteger[] coefficients, int x) {
        BigInteger result = BigInteger.ZERO;
        BigInteger xPower = BigInteger.ONE;
        BigInteger xBig = BigInteger.valueOf(x);

        for (BigInteger coefficient : coefficients) {
            result = result.add(coefficient.multiply(xPower)).mod(PRIME);
            xPower = xPower.multiply(xBig).mod(PRIME);
        }

        return result;
    }

    /**
     * 拉格朗日插值，计算 f(0) 的值
     */
    private static BigInteger lagrangeInterpolate(List<Share> shares) {
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < shares.size(); i++) {
            Share share = shares.get(i);
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < shares.size(); j++) {
                if (i == j) continue;

                Share otherShare = shares.get(j);
                // 计算拉格朗日基础多项式
                // numerator *= (0 - x_j)
                numerator = numerator.multiply(BigInteger.valueOf(-otherShare.getX())).mod(PRIME);
                // denominator *= (x_i - x_j)
                denominator = denominator.multiply(
                        BigInteger.valueOf(share.getX() - otherShare.getX())
                ).mod(PRIME);
            }

            // 计算 y_i * numerator / denominator
            BigInteger term = share.getY()
                    .multiply(numerator)
                    .multiply(denominator.modInverse(PRIME))
                    .mod(PRIME);

            result = result.add(term).mod(PRIME);
        }

        return result;
    }
}