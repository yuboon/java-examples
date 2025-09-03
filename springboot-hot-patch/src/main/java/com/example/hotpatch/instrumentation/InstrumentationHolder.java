package com.example.hotpatch.instrumentation;

import java.lang.instrument.Instrumentation;

/**
 * Instrumentation持有器 - 用于获取JVM的Instrumentation实例
 */
public class InstrumentationHolder {
    private static volatile Instrumentation instrumentation;
    
    public static void setInstrumentation(Instrumentation inst) {
        instrumentation = inst;
    }
    
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
    
    public static boolean isAvailable() {
        return instrumentation != null;
    }
}