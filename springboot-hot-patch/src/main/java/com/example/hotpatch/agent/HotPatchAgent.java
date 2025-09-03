package com.example.hotpatch.agent;

import com.example.hotpatch.instrumentation.InstrumentationHolder;

import java.lang.instrument.Instrumentation;

/**
 * Java Agent入口类
 */
public class HotPatchAgent {
    
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("HotPatch Agent 启动成功");
        InstrumentationHolder.setInstrumentation(inst);
    }
    
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("HotPatch Agent 动态加载成功");
        InstrumentationHolder.setInstrumentation(inst);
    }
}