# FuFile
![License](https://fufile-architecture.oss-cn-beijing.aliyuncs.com/apache2.0.svg)
### 简介
FuFile是面向大规模数据存储的分布式文件系统，设计中借鉴了GFS，并做了一些创新优化。
### 链接
[官方网站](https://fufile.org)
### 架构
![Fufile Architecture](https://fufile-architecture.oss-cn-beijing.aliyuncs.com/Fufile%20Architecture.jpg)
### 功能特点
+ 自研高性能rpc
+ 存储节点可伸缩
+ 持续的服务监控
+ 服务高可用，灾难冗余，自动恢复
+ 支持巨大或小文件存储
+ 支持大规模的文件存储
+ 机架感知
+ 根据存储容量负载均衡存储服务
### 模块
+ client：客户端
+ core：核心功能
+ data-server：存储服务
+ name-server：名字服务
+ example：例子
### 贡献
如果你想参与贡献，请发邮件到xuffcc@gmail.com。



