package com.yuboon.learning.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {


    @RequestMapping("/hello")
    public String hello(String name){
        try {
            String nullObject = null;
            nullObject.toString();
        } catch (Exception e) {

            e.printStackTrace();
            //System.err.println(ExceptionUtil.getRootStackElement().getClassName());
            StackTraceElement[] stackTraceElements = ExceptionUtil.getStackElements();
            ExceptionUtil.getThrowableList(e);

            ExceptionUtil.stacktraceToString(e);

        }
        return "hello : " + name;
    }


    /*@RequestMapping("/hello")
    public String hello2(String name){
        try {
            String nullObject = null;
            nullObject.toString();
        } catch (Exception e) {

            e.printStackTrace();
            //System.err.println(ExceptionUtil.getRootStackElement().getClassName());
            StackTraceElement[] stackTraceElements = ExceptionUtil.getStackElements();
            ExceptionUtil.getThrowableList(e);

            ExceptionUtil.stacktraceToString(e);

        }
        return "hello : " + name;
    }*/

}
