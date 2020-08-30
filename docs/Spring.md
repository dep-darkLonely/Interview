# Spring 框架基础知识

# Spring 

Spring Boot 和 Spring 的区别：

Spring 是一个开源应用程序框架，简化开发，分层架构、方便解耦
Spring Boot 是在Spring 的基础上搭建的一个框架，就是Spring框架的一个扩展，简化了传统Spring项目繁琐的XML搭建配置过程
内置Tomcat、Jetty等服务器，可通过java -jar的形式直接启动
提供了一些starters 简化POM的配置
Spring AOP(面向切面编程) 实现原理

AOP 面向切面编程： AOP是OOP的一个补充，一种“横切”技术，用于将那些与核心业务流程无关的通用功能抽离出来、单独封装，行 成一个独立的切面。
AOP的实现一般都是 [代理模式]，一般分为 [动态代理] 和 [静态代理]； 2.1 动态代理：动态代理是在JVM中动态生成的，运行时增强 JDK 动态代理： 代理接口，目标对象的实现类必须实现接口 CGLIB 动态代理： 代理类，目标对象的实现类没有实现接口 2.2 静态代理：静态代理是在编译期间生成AOP代理类，编译时增强 ASpectJ 静态代理：支持 编译时、编译后、加载时织入（Weaving），会使用ajc编译器 织入： 表示的是通过特殊的编译器ajc来嵌入切面到java类中
Spring AOP 的5种通知类型： 3.1 前置通知： 在方法(切点)执行之前返回 3.2 环绕通知： 在方法(切点)执行前后执行 3.3 后置通知： 在方法(切点)执行之后返回 3.4 异常通知： 在方法(切点)抛出异常之后执行 3.5 返回通知： 在方法(切点)返回结果之后通知
Spring IOC/DI 控制反转或依赖注入

利用JAVA 反射机制实现
将JAVA 对象交给Spring 容器进行管理
Spring IOC 初始化过程： XML -- 读取 --> Resource -- 解析 --> BeanDefinition -- 注册Bean --> BeanFactory(生产和管理Bean) 3.1 读取XML中Bean 的配置信息 3.2 根据Bean
Spring Bean 的作用域

4.1 Singleton： 单例，Spring IOC容器中仅存在一个Bean实例，Bean以单例方式存在，默认值 4.2 Prototype： 原型, 每次获取Bean都会返回一个新的实例， 相当于执行new Bean() 4.3 Request： 每次HTTP请求都会创建一个Bean实例 4.4 Session： 同一个HTTP Session中，共享一个Bean 4.5 GlobalSession：一般用于Portal应用环境，Portal请求由Portal容器管理

Spring Transcation 事务: 5.1 事务的四大特性： 原子性，持久性，隔离性，一致性（ACID） 原子性： 原子是最小单位，指的是事务中包含的操作是不可分割的，要么全部成功要么全部失败 持久性： 指的是事务一旦被提交了，对数据库中的数据的改变是永久性的 隔离性： 当多个用户并发访问数据库时，数据库为每一个用户开启的事务是相互隔离的，互相不干扰的 一致性： 指的是事务必须使数据库从一个一致性状态转变到另一个一致性状态，也就是说

5.2 transcationDefinition.isolation_default, 默认使用后端数据库的默认隔离级别， MYSQL 默认使用的是REPEATABLE_READ隔离级别 Oracle 默认使用的是READ_COMMITED隔离级别 transcationDefinition.isolation_read_uncommited: 最低的隔离级别，允许读取事务中尚未提交的数据变更，可能会导致脏读、幻读 不可重复读 transcationDefinition.isolation_read_commited: 允许读取并发事务中已经提交的数据，可阻止脏读，但幻读和不可重读仍有可能发生 transcationDefinition.isolation_repetable_read: 重复读取，对同一字段的多次读取结果都是一致的，除非数据是被本身事务所修改 可阻止脏读、不可重复读，但不能阻止幻读 transcationDefinition.serializable: 最高的隔离级别，完全服从ACID的隔离级别，所有的事务都是依次顺序执行，这样事务 之间是不会相互影响的。可以防止脏读、幻读、不可重复读，但性能将会受到严重影响。