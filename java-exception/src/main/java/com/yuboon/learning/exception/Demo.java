package com.yuboon.learning.exception;

/**
 * 类描述
 * Created by chenyb on 2019/12/11
 */
public class Demo {

    public static void main(String[] args) {
        //String nullObject = null;
        //nullObject.toString();

        "s".toString();
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for(StackTraceElement stackTraceElement : stackTraceElements){
            System.err.println(stackTraceElement.getMethodName());
        }
        System.err.println(stackTraceElements.length);


        new Thread(){
            @Override
            public void run() {
                String nullObject = null;
                nullObject.toString();
            }
        }.start();

        //System.err.println("ThreadName:" + Thread.currentThread().getName());
    }
}
