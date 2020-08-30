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
#### Spring整体脉络图

#### 源码解析

> [!Warning|label:AnnotationConfigApplicationContext类图]
> ![Annotation...类图](./Image/Spring/AnnotationConfigApplicationContext.png)


> [!Note|label:AnnotationConfigApplicationContext源码解析]
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

> [!Note|label:Refresh方法是Spring的核心方法]   
> - **AbstractApplicationContext中的refresh()方法是Spring框架的核心;**
> - Refresh()方法总共可以分为12步骤，核心步骤finishBeanFactoryInitialization()

```java
org.springframework.context.support.AbstractApplicationContext#refresh

@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        /**
         * <1> 准备刷新阶段；
         * 设置开始时间，创建Environment环境变量，以及实例化一些用于存储监听器的集合
         */
        prepareRefresh();
        
        /**
         * <2> 获取BeanFactory,设置BeanFactory的ID属性
         * 涉及CAS操作，判断当前BeanFactory是否已经刷新过
         */
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        /**
         * <3> 填充BeanFactory的属性，如
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
             * <4> 实例化并调用所有BeanFactory的后置处理器，用于将通过注解
             * 扫描得到的Bean信息封装成BeanDefinition加入到BeanDefinitionMap中；
             * BeanDefinition中包含了当前Bean的各种信息，如Bean名称、是否依赖、
             * 初始化方法.....
             */
            invokeBeanFactoryPostProcessors(beanFactory);

            /**
             * <5> 实例化并注册所有的Bean后置处理器
             * 这里只是进行实例化，注册并没有进行调用；
             *  主要用于后期Bean初始化时进行调用
             */
            registerBeanPostProcessors(beanFactory);

            /**
             * <6> 加载国际化i18n等资源信息；
             * 初始化MessageSource
             */
            initMessageSource();

            /**
             * <7> 初始化应用程序事件传播器；
             * 这里使用了一种观察者的设计模式；
             */
            initApplicationEventMulticaster();

            /**
             * 没有实现
             */
            onRefresh();

            /**
             * <8> 注册监听器
             */
            registerListeners();

            /**
             * <9> 实例化剩余非懒加载的单例Bean
             */
            finishBeanFactoryInitialization(beanFactory);

            /**
             * <10> 刷新完成工作，包括初始化LifecycleProcessor，发布刷新完成事件等
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
             * <11> 重置Spring公共的缓存
             * declaredMethodsCache、declaredFieldsCache ....
             */
            resetCommonCaches();
        }
    }
}
```

> [!Warning|label:重点介绍finishBeanFactoryInitialization方法]  
> - 这里着重分析finishBeanFactoryInitialization()，该方法中实现了Spring的核心功能AOP(面向切换编程)和IOC(控制反转)以及DI(依赖注入)

```java
org.springframework.context.support.AbstractApplicationContext#finishBeanFactoryInitialization

/**
 * 完成Bean Factory的初始化，和所有单例Bean的初始化工作
 */ 
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {

    /**
     * 实例化上下文的conversion service
     * 实例化转换服务，如字段类型转换，对象转换
     * 依赖注入时，可用于类型转换
     */ 
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
            beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
        beanFactory.setConversionService(
                beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

    /**
     * 判断beanFactory中是否存在一个嵌入值解析器,不存在的话，则添加一个嵌入值解析器，主要用于解析属性表达式中的占位符
     * 如：@value(#{})
     */ 
    if (!beanFactory.hasEmbeddedValueResolver()) {
        // 添加一个嵌入值解析器
        beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
    }

    String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
    for (String weaverAwareName : weaverAwareNames) {
        getBean(weaverAwareName);
    }

    // 设置临时类加载器
    beanFactory.setTempClassLoader(null);

    /**
     * 将所有的BeanDefinition的metadata进行缓存，保证不被修改；
     * 主要就是将bean 定义的名称进行缓存
     * 底层:
     * this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
     */ 
    beanFactory.freezeConfiguration();

    /**
     * <p>
     *  <b>实例化剩余的单实例Bean</b>
     * </p>
     */ 
    beanFactory.preInstantiateSingletons();
}
```

> [!Warning|label:preInstantiateSingletons方法调用详细流程]
```java
@Override
public void preInstantiateSingletons() throws BeansException {
    if (logger.isTraceEnabled()) {
        logger.trace("Pre-instantiating singletons in " + this);
    }

    // 获取容器中所有的Bean定义的名称
    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

    // 循环创建所有的Bean实例对象
    for (String beanName : beanNames) {
        // 合并我们的bean定义
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);

        /**
         * 根据bean定义判断是不是抽象的 && 是单例的 && 不是懒加载的
         */
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            // 判断当前bean是否是FactoryBean，依据当前bean是否实现了FactoryBean接口
            if (isFactoryBean(beanName)) {
                // 是的话， 给beanName+前缀&符号
                Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);

                if (bean instanceof FactoryBean) {
                    final FactoryBean<?> factory = (FactoryBean<?>) bean;
                    boolean isEagerInit;
                    if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                        isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>)
                                        ((SmartFactoryBean<?>) factory)::isEagerInit,
                                getAccessControlContext());
                    }
                    else {
                        isEagerInit = (factory instanceof SmartFactoryBean &&
                                ((SmartFactoryBean<?>) factory).isEagerInit());
                    }
                    if (isEagerInit) {
                        // 调用真正的getBean的流程
                        getBean(beanName);
                    }
                }
            }
            else {
                /**
                 * <p>
                 *   <b>非工厂bean，普通bean</b>
                 * </p>
                 */ 
                getBean(beanName);
            }
        }
    }

    /**
     * <1> 判断IOC容器中的Bean是否实现SmartInitializingSingleton接口，如果实现接口，则回调起afterSingletonsInstantiated方法,作为一个后置处理操作
     * <2> 到这里所有的单实例的bean已经加载到单例缓存池(singleObjects)中
     */
    for (String beanName : beanNames) {
        // 从单例缓存池中获取beanName对象
        Object singletonInstance = getSingleton(beanName);
        // 判断当前对象是否实现了SmartInitializingSingleton接口
        if (singletonInstance instanceof SmartInitializingSingleton) {
            final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    smartSingleton.afterSingletonsInstantiated();
                    return null;
                }, getAccessControlContext());
            }
            else {
                smartSingleton.afterSingletonsInstantiated();
            }
        }
    }
}
```

> [!Warning|label:getBean调用流程]
> - getBean() 方法大概主要逻辑流程:
>   1. 先判断当前单例缓存池singletonObjects中是否含有已经创建好的对象，有则返回，无则进行创建
>   2. 创建Bean实例对象之前先对其做一个检查，如@dependOn依赖其他Bean的检查，注册其与其他bean之间的依赖关系
>   3. 创建Bean实例(createBeanInstance)，通过Java反射方法construct.newInstance()创建一个Bean实例对象，此时当前Bean实例对象的所有属性全部为null
>   4. Bean实例属性填充(populateBean), 属性填充时，设计DI依赖注入操作
>   5. 初始化Bean实例(initializeBean);
>       - Spring的另一个核心功能AOP就是此处实现
>       - 主要是用过调用init方法来Bean实例进行初始化操作
>       - 调用后置处理器对其创建的Bean实例对象进行一些特殊操作
```java
```