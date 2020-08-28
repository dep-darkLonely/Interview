# Spring 源码学习

> [!Note|label:创建Test程序]
```java
// 只介绍使用注解版配置文件
@Test
public void testIOC() {
    AnnotationConfigApplicationContext annotationConfigApplicaionContext = new AnnotationConfigApplicationContext(AppConfig.class);
    UserController userController = annotationConfigApplicaionContext.getBean(UserController.class);
    User user = userController.login(1);
    System.out.println(user);
}
```

##### 源码学习

> [Error|label:AnnotationConfigApplicationContext.class类图]

![Annotation...类图](./Image/Spring/AnnotationConfigApplicationContext.png)

> [!Note|label:AnnotationConfigApplicationContext.class源码解析]

```java
/**
 * 创建一个AnnotationConfigApplicationContext上下文对*象，从给定的配置类中读取Bean的定义信息，并刷新上下文
 */
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
    /**
     * 调用无参构造器,创建BeanDefinition的扫描器并且创建BeanFactory
     */
    this();
    // 注册配置类
    register(componentClasses);
    // 核心方法: 刷新ApplicationContext上下文
    refresh();
}

=================== this() 方法调用流程 =============
/**
 * 由于AnnotationConfigApplicationContext.class继承了GenericApplicationContext.class 类，首先会调用父类的无参构造器
 */

// 父类构造器 org.springframework.context.support.GenericApplicationContext#GenericApplicationContext()
public GenericApplicationContext() {
    /**
     * 创建BeanFactory，主要目的就是用于生产和管理Bean
     */
    this.beanFactory = new DefaultListableBeanFactory();
}

// 当前类无参构造器 org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext()
public AnnotationConfigApplicationContext() {
    // 基于注解的BeanDefinition的读取器
    this.reader = new AnnotatedBeanDefinitionReader(this);
    // 创建BeanDefinition扫描器，基于XML文件的包扫描器
    this.scanner = new ClassPathBeanDefinitionScanner(this);
}
====================================================
```

#### AbstractApplicationContext中的refresh()方法是Spring框架的核心

> [!Note|label:Refresh()核心方法]

**Refresh()方法总共可以分为12步骤，核心步骤finishBeanFactoryInitialization()** 

```java
org.springframework.context.support.AbstractApplicationContext#refresh

@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        /**
         * 准备刷新阶段；
         * 设置开始时间，创建Environment环境变量，以及实例化一些用于存储监听器的集合
         */
        prepareRefresh();
        
        /**
         * 获取BeanFactory，设置BeanFactory的ID属性
         */
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        /**
         * 填充BeanFactory的属性，如
         * 1. 类加载器、
         * 2. Bean表达式解析器用于解析EL表达式；
         *    如 @Value{#....}
         * 3. 增加aware后置处理器，
         * aware后置处理器的作用：是当前Bean对象可以获取当前应用程序上下文ApplicationContex
         */
        prepareBeanFactory(beanFactory);

        try {
            /**
             * 该接口尚未实现；
             * 主要作用：BeanFactory 后置处理器，用于改变BeanDefition中的信息；
             */
            postProcessBeanFactory(beanFactory);

            /**
             * 实例化并调用所有BeanFactory的后置处理器，用于将通过注解
             * 扫描得到的Bean信息封装成BeanDefinition加入到BeanDefinitionMap中；
             * BeanDefinition中包含了当前Bean的各种信息，如Bean名称、是否依赖、
             * 初始化方法.....
             */
            invokeBeanFactoryPostProcessors(beanFactory);

            /**
             * 实例化并注册所有的Bean后置处理器
             * 这里只是进行实例化，注册并没有进行调用；
             *  主要用于后期Bean初始化时进行调用
             */
            registerBeanPostProcessors(beanFactory);

            /**
             * 加载国际化i18n等资源信息；
             * 初始化MessageSource
             */
            initMessageSource();

            /**
             * 初始化应用程序事件传播器；
             * 这里使用了一种观察者的设计模式；
             */
            initApplicationEventMulticaster();

            /**
             * 没有实现
             */
            onRefresh();

            /**
             * 注册监听器
             */
            registerListeners();

            /**
             * 实例化剩余非懒加载的单例Bean
             */
            finishBeanFactoryInitialization(beanFactory);

            /**
             * 刷新完成工作，包括初始化LifecycleProcessor，发布刷新完成事件等
             */
            finishRefresh();
        }

        catch (BeansException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Exception encountered during context initialization - " +
                        "cancelling refresh attempt: " + ex);
            }

            // Destroy already created singletons to avoid dangling resources.
            destroyBeans();

            // Reset 'active' flag.
            cancelRefresh(ex);

            // Propagate exception to caller.
            throw ex;
        }

        finally {
            /**
             *重置Spring公共的缓存
             * declaredMethodsCache、declaredFieldsCache ....
             */
            resetCommonCaches();
        }
    }
}
```