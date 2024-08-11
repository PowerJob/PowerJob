由于存在不同数据库、不同版本的升级，官方能给出的 upgrade SQL 相对有限，大家可参考以下方式自行生成升级 SQL：

- 【官方脚本】参考官方每个版本的数据库全库建表文件（项目 others - sql - schema），自行进行字段 DIFF

- 【自己动手版】导出当前您的 powerjob 数据库表结构，同时创建一个测试库，让 5.x 版本的 server 直连该测试库，自动建表。分别拿到两个版本的表结构 SQL 后，借用工具生产 update SQL 即可（navigate 等数据库管理软件均支持结构对比）

参考文档：https://www.yuque.com/powerjob/guidence/upgrade