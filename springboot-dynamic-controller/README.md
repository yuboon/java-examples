## 说明
1. 启动服务
2. 访问 http://localhost:8080/demo/hello ，此时应该是404，因为没有注册控制器
3. POST http://localhost:8080/controller/register?methodName=hello&controllerBeanName=demoController 注册控制器
4. 再次访问 http://localhost:8080/demo/hello
5. POST http://localhost:8080/controller/unregister?methodName=hello&controllerBeanName=demoController 卸载控制器
6. 再次访问 http://localhost:8080/demo/hello