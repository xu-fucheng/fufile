# FuFile
[![LICENSE](https://img.shields.io/github/license/xu-fucheng/fufile)](https://gitee.com/xufucheng/fufile/blob/trunk/LICENSE)
[![Language](https://img.shields.io/badge/language-Java-red)](https://java.com/)

> 如果此项目长时间未更新，那很有可能是我在憋大的！
## 简介

FuFile是一款开源的大规模分布式存储系统，可作为大文件或小文件存储使用，也可作为数据库使用。

> 作者目前在开发nameserver的raft功能，基于Diego Ongaro and John Ousterhout. In Search of an Understandable Consensus Algorithm (Extended Version), Stanford University, 2014.论文。

## FuFile提供五大核心特性：

+ 高可用

在发生网络故障或部分节点宕机的情况下，只要剩余可用节点数量占多数，则集群可继续提供服务。

+ 强一致性

只要集群可用，同一时刻，任意客户端总能读取到最新的已提交操作。

+ 容灾

具备机架感知功能，当某个机房出现故障时，其他机房仍可提供服务并且数据不会丢失。

+ 可扩缩容

服务节点在不停机的情况下，可扩容和缩容。

+ 高性能

数据被分成16m大小的块存储，将io压力分散到多个服务节点。

## 链接

[官方网站](https://fufile.org)（目前作者在全力开发，官网暂时停止维护。）

## 架构

图片仅供参考，具体设计以项目代码为准。

![fufile-architecture](./images/fufile-architecture.jpg)

## 开发计划

|Version|Feature|Date|
|:-:|:-:|:-:|
|v1|rpc base on jdk||
|v1|name-server: raft||
|v1|name-server: dir tree&persistence||
|v1|message code generate||
|v1|memory pool||
|v1|time wheel||
|v1|data-server: PacificA||
|v1|data-server: Quorum NRW||
|v1|data-server: file transfer&persistence||
|v1|name-server: multi-raft (maybe)||
|v2|compute-server: analysis&compute||
|v2|grpc||
|v2|rpc base on netty (maybe)||
## 贡献

您好，目前项目处于初期，正在全力开发中，感谢关注！

Fufile将基于业界最新的设计与论文，项目严格保证高质量并持续开发。

如果您对此项目感兴趣，欢迎加入，对于新手，可提供指导，您可以收获包括免费正版idea ultimate license、学习设计大型系统的经验和提升技术的深度等等。

河冰结合,非一日之寒；积土成山,非斯须之作。一天很长，一年很短，只要坚持，定会改变原来的人生轨迹。

如果您想参与贡献，请联系作者微信：xuxuffcc



