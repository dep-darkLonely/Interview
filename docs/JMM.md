# JAVA 内存模型JMM

> [!Note|label:相关概念]
> - **原子性**: 即一个操作或者多个操作 要么全部执行并且执行过程不会被任何因素打断，要么全部不执行
> - **原子操作**: 指一个或者多个不可再分割的操作，这些操作的执行顺序是不能被打乱的，也不可以被分割而只执行其中某一部分的内容
> - CAS(Compare And Swap)具有原子性，是原子操作
> - **volatile**: 不具有**原子性**，具有可见性和有序性


> [!Warning|label:CAS轻量级锁中存在的问题:|icon:null]
> + 经典的**ABA**问题
> + 自旋问题

> [!Warning|label:volatile底层实现|icon:null]