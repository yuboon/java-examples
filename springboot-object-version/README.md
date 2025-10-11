# Spring Boot 对象审计

极简版对象审计示例，使用 Spring Boot 3 与 Javers 在内存中记录实体变更，并通过 REST API 暴露。前端页面使用纯 HTML + Tailwind CSS，通过 Fetch API 调用接口，界面与提示均为中文。

## 后端

- 技术栈：Java 17、Spring Boot 3、Spring Web、Javers Core。
- 数据存储：`ConcurrentHashMap` 保存商品与审计日志，进程重启后数据会清空。
- 审计逻辑：拦截商品的新增、更新、删除操作，将 `javers.compare` 的 Diff JSON 直接写入审计流。
- 主要接口（前缀 `/api/products`）：
  - `GET /api/products` - 查询全部商品。
  - `GET /api/products/{id}` - 根据编号获取商品。
  - `PUT /api/products/{id}` - 新建或更新商品（可携带 `X-User` 头作为操作人）。
  - `DELETE /api/products/{id}` - 删除商品。
  - `GET /api/products/{id}/audits` - 查看指定商品的审计记录。
  - `GET /api/products/audits` - 查看全部审计记录。

## 前端

- 页面位置：`src/main/resources/static/index.html`（Spring Boot 会自动以静态资源目录提供服务）。
- 访问地址：后端启动后打开 `http://localhost:8080/index.html`；也可以直接双击文件本地预览。
- 功能说明：
  1. 表单区用于创建或更新商品，可选填操作人。
  2. 商品列表支持查看审计与删除操作。
  3. 审计时间线支持筛选单个商品或回看全部记录，Diff JSON 会解析成中文说明。

如需修改后端地址，请在页面顶部脚本中调整 `API_BASE` 常量。
