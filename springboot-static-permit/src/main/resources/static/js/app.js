// 简单的JavaScript文件，演示公开静态资源
console.log('Spring Boot 静态资源权限控制演示 - JavaScript已加载');

document.addEventListener('DOMContentLoaded', function() {
    console.log('页面加载完成');

    // 为所有链接添加点击提示
    const links = document.querySelectorAll('a[href^="/uploads/"], a[href^="/files/"]');
    links.forEach(link => {
        link.addEventListener('click', function(e) {
            const href = this.getAttribute('href');
            console.log('尝试访问:', href);
        });
    });
});