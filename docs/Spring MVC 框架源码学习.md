

# Spring MVC 框架源码学习(JavaConfig)



### 1. 创建Demo工程，用于分析Spring MVC框架源码(采用注解版本)

###### <1> 创建SpringWebApplication，用于加载Spring IOC容器和注册DispatcherServlet

```java
public class SpringWebApplication implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {
        
        // 加载Spring IOC容器，将所有Bean实例对象交由Spring管理
        AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
        annotationConfigWebApplicationContext.register(AppConfig.class);
        annotationConfigWebApplicationContext.setServletContext(servletContext);
        annotationConfigWebApplicationContext.refresh();

        // 注册DispatcherServlet;
        // DispatcherServlet前段控制器是SpringMVC框架的核心，主要负责调度工作、用于控制流程
        DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);
        ServletRegistration.Dynamic registration = servletContext.addServlet("dispatcher", dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}
```

###### <2> 使用@EnableWebMvc注解，启用Spring MVC

```java
@EnableWebMvc
class MvcConfig implements WebMvcConfigurer {
}
```

### 2. Spring MVC框架源码调用流程分析

###### <1>AnnotationConfigWebApplicationContext.java 流程分析；

AnnotationConfigWebApplicationContext类图

![image-20200909222819298](.\Image\Spring\AnnotationConfigWebApplicationContext.png)

[具体流程调用，查看Spring 框架源码分析](http://www.baidu.com)

```java
// 加载Spring IOC容器，将所有Bean实例对象交由Spring管理
AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new 	        AnnotationConfigWebApplicationContext();
// ★★★ <1> 加载配置类
annotationConfigWebApplicationContext.register(AppConfig.class);
// 设置ServletContext上下文
annotationConfigWebApplicationContext.setServletContext(servletContext);
// ★★★ <2> 刷新Application Context上下文
annotationConfigWebApplicationContext.refresh();
```

###### <2> 注册配置类

org.springframework.web.context.support.AnnotationConfigWebApplicationContext#register

```java
/**
 * 用于注册配置文件类
 */
@Override
public void register(Class<?>... componentClasses) {
    Assert.notEmpty(componentClasses, "At least one component class must be specified");
    // 将 componentClasses 放入集合componentClasses中，用于优先加载
    Collections.addAll(this.componentClasses, componentClasses);
}
```

###### <3> refresh() 刷新ApplicationContext 上下文

org.springframework.context.support.AbstractApplicationContext#refresh

```java
/**
 * 刷新application 上下文
 */
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
      
        // 容器刷新前的准备，设置上下文状态、获取属性、初始化属性(property source)配置
        prepareRefresh();
      
        // ★★★ <1> 通过 CAS 操作，刷新BeanFactory并设置BeanFactory的序列化ID，加载BeanDefinition到BeanDefinitionMap
        // AnnotationConfigApplicationContext和AnnotationConfigWebApplicationContext调用是不相同的
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // 配置标准的beanFactory，设置ClassLoader，设置SpEL表达式解析器，添加忽略注入的接口，添加bean，添加bean后置处理器等
        prepareBeanFactory(beanFactory);

        try {
            // 获取容器BeanFactory，可以在真正初始化bean之前对bean做一些处理操作。
            // 允许我们在工厂里所有的bean被加载进来后但是还没初始化前，对所有bean的属性进行修改也可以add属性值。
            postProcessBeanFactory(beanFactory);
				
            //★★★ 实例化并调用所有注册的beanFactory后置处理器
            invokeBeanFactoryPostProcessors(beanFactory);

            // 实例化和注册beanFactory中扩展了BeanPostProcessor的bean
            registerBeanPostProcessors(beanFactory);

            // 初始化国际化工具类MessageSource
            initMessageSource();

            // 初始化事件广播器
            initApplicationEventMulticaster();

            // 模板方法，在容器刷新的时候可以自定义逻辑，不同的Spring容器做不同的事情
            // SpringBoot是从这个方法进行启动Tomcat的
            onRefresh();

            // 注册监听器，广播early application events
            registerListeners();

            // 实例化所有非懒加载的单例bean
            finishBeanFactoryInitialization(beanFactory);

            // refresh做完之后需要做的其他事情，发布事件
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
            // Reset common introspection caches in Spring's core, since we
            // might not ever need metadata for singleton beans anymore...
            resetCommonCaches();
        }
    }
}
```

###### <4>obtainFreshBeanFactory获取BeanFactory，并将配置类@Configurarion加载到BeanDefinitionMap

- 创建Bean的扫描器
- 将注册的配置类、加载到BeanDefinitionMap中，为后面调用BeanFactory后置处理器解析配置类，加载剩余Bean到BeanDefinitionMap中做准备; 这里只是将annotationConfigWebApplicationContext.register(AppConfig.class)注册的配置类，加载到BeanDefinitionMap中;

org.springframework.context.support.AbstractApplicationContext#obtainFreshBeanFactory

```java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    // 刷新beanFactory
    refreshBeanFactory();
    // 获取BeanFactory
    return getBeanFactory();
}
```

org.springframework.context.support.AbstractRefreshableApplicationContext#refreshBeanFactory

```java
@Override
protected final void refreshBeanFactory() throws BeansException {
    // 判断当前是否存在BeanFactory
    if (hasBeanFactory()) {
        // 若存在的话，则销毁BeanFactory
        destroyBeans();
        closeBeanFactory();
    }
    try {
        // 创建BeanFactory用于创建Bean和管理Bean
        DefaultListableBeanFactory beanFactory = createBeanFactory();
        // 设置BeanFactory的ID
        beanFactory.setSerializationId(getId());
        customizeBeanFactory(beanFactory);
        // ★★★ 加载BeanDefinition，将Bean封装成BeanDefinition放入BeanDefinitionMap集合中
        /**
         * 注册默认的处理器,主要作用是用于解析@Configuration配置文件类
         * 1. org.springframework.context.annotation.internalConfigurationBeanNameGenerator
         * 2. org.springframework.context.annotation.internalAutowiredAnnotationProcessor
         * 3. org.springframework.context.event.internalEventListenerFactory
         * 4. org.springframework.context.annotation.internalCommonAnnotationProcessor
         * 5. org.springframework.context.event.internalEventListenerProcessor
         */
        loadBeanDefinitions(beanFactory);
        synchronized (this.beanFactoryMonitor) {
            this.beanFactory = beanFactory;
        }
    }
    catch (IOException ex) {
        throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
    }
}
```

★★★★ **loadBeanDefinitions 解析@Configuration配置文件，并将其加入到BeanDefinitionMap集合中**

org.springframework.web.context.support.AnnotationConfigWebApplicationContext#loadBeanDefinitions

```java
@Override
protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
    //注解版 注册Bean扫描器
    AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);
    //  xml版本 Bean 定义 扫描器
    ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);

    // BeanName生成器
    BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
    if (beanNameGenerator != null) {
        reader.setBeanNameGenerator(beanNameGenerator);
        scanner.setBeanNameGenerator(beanNameGenerator);
        // 注册BeanName的生成器    
        beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
    }
    //  获取@scope 解析器
    ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
    if (scopeMetadataResolver != null) {
        reader.setScopeMetadataResolver(scopeMetadataResolver);
        scanner.setScopeMetadataResolver(scopeMetadataResolver);
    }

    if (!this.componentClasses.isEmpty()) {
        if (logger.isDebugEnabled()) {
            logger.debug("Registering component classes: [" +
                         StringUtils.collectionToCommaDelimitedString(this.componentClasses) + "]");
        }
        // 将注册的配置类加载到BeanDefinitionMap集合中
        reader.register(ClassUtils.toClassArray(this.componentClasses));
    }

    if (!this.basePackages.isEmpty()) {
        if (logger.isDebugEnabled()) {
            logger.debug("Scanning base packages: [" +
                         StringUtils.collectionToCommaDelimitedString(this.basePackages) + "]");
        }
        scanner.scan(StringUtils.toStringArray(this.basePackages));
    }

    // 获取配置文件位置
    String[] configLocations = getConfigLocations();
    if (configLocations != null) {
        for (String configLocation : configLocations) {
            try {
                Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
                if (logger.isTraceEnabled()) {
                    logger.trace("Registering [" + configLocation + "]");
                }
                reader.register(clazz);
            }
            catch (ClassNotFoundException ex) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Could not load class for config location [" + configLocation +
                                 "] - trying package scan. " + ex);
                }
                int count = scanner.scan(configLocation);
                if (count == 0 && logger.isDebugEnabled()) {
                    logger.debug("No component classes found for specified class/package [" + configLocation + "]");
                }
            }
        }
    }
}
```

###### <5> invokeBeanFactoryPostProcessors 调用BeanFactory的后置处理器，将剩余的Bean加载到BeanDefinitionMap集合中(即packageScan中定义的Bean)

org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors

```java
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    // 调用BeanFactory的后置处理器
    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

    // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
    // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
    if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
        beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
        beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }
}
```

org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.List<org.springframework.beans.factory.config.BeanFactoryPostProcessor>)

![image-20200909234824812](.\Image\Spring\DefaultListableBeanFactory.png)

```java

//beanfactory 类型为 DefaultListableBeanFactory
public static void invokeBeanFactoryPostProcessors(
    ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

    // Invoke BeanDefinitionRegistryPostProcessors first, if any.
    Set<String> processedBeans = new HashSet<>();
	// DefaultListableBeanFactory 实现了该接口
    if (beanFactory instanceof BeanDefinitionRegistry) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
        List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

        for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
            if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                BeanDefinitionRegistryPostProcessor registryProcessor =
                    (BeanDefinitionRegistryPostProcessor) postProcessor;
                registryProcessor.postProcessBeanDefinitionRegistry(registry);
                registryProcessors.add(registryProcessor);
            }
            else {
                regularPostProcessors.add(postProcessor);
            }
        }

        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let the bean factory post-processors apply to them!
        // Separate between BeanDefinitionRegistryPostProcessors that implement
        // PriorityOrdered, Ordered, and the rest.
        List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

        // 获取BeanDefinitionRegistryPostProcessor的后置处理器
        String[] postProcessorNames =
            beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
        
        for (String ppName : postProcessorNames) {
            // 判断当前后置处理器的Bean类型 是否和PriorityOrdered一致
            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                processedBeans.add(ppName);
            }
        }
        // 进行排序处理
        sortPostProcessors(currentRegistryProcessors, beanFactory);
        // 将currentRegistryProcessors中的所有后置处理器加入到registryProcessors中
        registryProcessors.addAll(currentRegistryProcessors);
        // ★★★★ <1> 调用BeanDefinitionRegistryPostProcessor的后置处理器
        invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        // 清空currentRegistryProcessors中数据
        currentRegistryProcessors.clear();

        // 获取BeanDefinitionRegistryPostProcessor的后置处理器
        postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
       
        for (String ppName : postProcessorNames) {
            // 判断当前的后置处理器的Bean类型是否和 Ordered类型一致
            if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
                currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                processedBeans.add(ppName);
            }
        }
        // 后置处理器进行排序
        sortPostProcessors(currentRegistryProcessors, beanFactory);
        registryProcessors.addAll(currentRegistryProcessors);
        // 调用实现Ordered类型的后置处理器
        invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        // 清空currentRegistryProcessors中数据
        currentRegistryProcessors.clear();

        // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
        boolean reiterate = true;
        while (reiterate) {
            reiterate = false;
            postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (!processedBeans.contains(ppName)) {
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    processedBeans.add(ppName);
                    reiterate = true;
                }
            }
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            registryProcessors.addAll(currentRegistryProcessors);
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            currentRegistryProcessors.clear();
        }

        // Now, invoke the postProcessBeanFactory callback of all processors handled so far.
        invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
        invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
    }

    else {
        // Invoke factory processors registered with the context instance.
        invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
    }

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let the bean factory post-processors apply to them!
    String[] postProcessorNames =
        beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

    // Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
    // Ordered, and the rest.
    List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
    List<String> orderedPostProcessorNames = new ArrayList<>();
    List<String> nonOrderedPostProcessorNames = new ArrayList<>();
    for (String ppName : postProcessorNames) {
        if (processedBeans.contains(ppName)) {
            // skip - already processed in first phase above
        }
        else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
            priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
        }
        else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
            orderedPostProcessorNames.add(ppName);
        }
        else {
            nonOrderedPostProcessorNames.add(ppName);
        }
    }

    // First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

    // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
    List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
    for (String postProcessorName : orderedPostProcessorNames) {
        orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

    // Finally, invoke all other BeanFactoryPostProcessors.
    List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
    for (String postProcessorName : nonOrderedPostProcessorNames) {
        nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

    // Clear cached merged bean definitions since the post-processors might have
    // modified the original metadata, e.g. replacing placeholders in values...
    beanFactory.clearMetadataCache();
}
```

最终会调用该方法，用来处理配置类中定义的Bean

org.springframework.context.annotation.ConfigurationClassPostProcessor#processConfigBeanDefinitions

```java
public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
    List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
    // 获取BeanDefinition的名称；即BeanDefinitionMap集合中所有BeanDefinition的名称，此时
    // 配置类(@Configuration)已经存在于BeanDefinitionMap中
    String[] candidateNames = registry.getBeanDefinitionNames();

    // 其余的不用关心，这里只关心我们的配置类(@Configuration)
    for (String beanName : candidateNames) {
        BeanDefinition beanDef = registry.getBeanDefinition(beanName);
        if (ConfigurationClassUtils.isFullConfigurationClass(beanDef) ||
            ConfigurationClassUtils.isLiteConfigurationClass(beanDef)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
            }
        }
        else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
            configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
        }
    }

    // 若没有@Configuration配置类，则直接结束
    if (configCandidates.isEmpty()) {
        return;
    }

    // 多个配置类进行排序
    configCandidates.sort((bd1, bd2) -> {
        int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
        int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
        return Integer.compare(i1, i2);
    });

    // Detect any custom bean name generation strategy supplied through the enclosing application context
    SingletonBeanRegistry sbr = null;
    if (registry instanceof SingletonBeanRegistry) {
        sbr = (SingletonBeanRegistry) registry;
        if (!this.localBeanNameGeneratorSet) {
            BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
            if (generator != null) {
                this.componentScanBeanNameGenerator = generator;
                this.importBeanNameGenerator = generator;
            }
        }
    }

    if (this.environment == null) {
        this.environment = new StandardEnvironment();
    }

    // 转换@Configuration 配置类
    ConfigurationClassParser parser = new ConfigurationClassParser(
        this.metadataReaderFactory, this.problemReporter, this.environment,
        this.resourceLoader, this.componentScanBeanNameGenerator, registry);

    Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
    // 已经解析完成的@Configuration 配置类
    Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
    
    do {
        // ★★★ 解析配置类，扫描包，将扫描到的类，加入到BeanDefinitionMap集合中
        parser.parse(candidates);
        parser.validate();

        Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
        configClasses.removeAll(alreadyParsed);

        // Read the model and create bean definitions based on its content
        if (this.reader == null) {
            this.reader = new ConfigurationClassBeanDefinitionReader(
                registry, this.sourceExtractor, this.resourceLoader, this.environment,
                this.importBeanNameGenerator, parser.getImportRegistry());
        }
        this.reader.loadBeanDefinitions(configClasses);
        alreadyParsed.addAll(configClasses);

        candidates.clear();
        if (registry.getBeanDefinitionCount() > candidateNames.length) {
            String[] newCandidateNames = registry.getBeanDefinitionNames();
            Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
            Set<String> alreadyParsedClasses = new HashSet<>();
            for (ConfigurationClass configurationClass : alreadyParsed) {
                alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
            }
            for (String candidateName : newCandidateNames) {
                if (!oldCandidateNames.contains(candidateName)) {
                    BeanDefinition bd = registry.getBeanDefinition(candidateName);
                    if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
                        !alreadyParsedClasses.contains(bd.getBeanClassName())) {
                        candidates.add(new BeanDefinitionHolder(bd, candidateName));
                    }
                }
            }
            candidateNames = newCandidateNames;
        }
    }
    while (!candidates.isEmpty());

    // Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
    if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
        sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
    }

    if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
        // Clear cache in externally provided MetadataReaderFactory; this is a no-op
        // for a shared cache since it'll be cleared by the ApplicationContext.
        ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
    }
}

```

★★  parse() 主要用于解析@Configuration配置类，将扫描到的Bean加入到BeanDefinitionMap中

org.springframework.context.annotation.ConfigurationClassParser#parse(java.util.Set<org.springframework.beans.factory.config.BeanDefinitionHolder>)

```java
public void parse(Set<BeanDefinitionHolder> configCandidates) {
    // 到这里为止，configCandidates中只含有我们自己创建的配置类@Configuration
    for (BeanDefinitionHolder holder : configCandidates) {
        BeanDefinition bd = holder.getBeanDefinition();
        try {
            // 判断当前BeanDefinition是否实现了AnnotatedBeanDefinition接口
            if (bd instanceof AnnotatedBeanDefinition) {
                // 进行转换
                parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
            }
            else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
                parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
            }
            else {
                parse(bd.getBeanClassName(), holder.getBeanName());
            }
        }
        catch (BeanDefinitionStoreException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BeanDefinitionStoreException(
                "Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
        }
    }

    this.deferredImportSelectorHandler.process();
}
```

★★ 处理配置类@Configuration

org.springframework.context.annotation.ConfigurationClassParser#processConfigurationClass

```java
protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
    // 是否应该skip
    if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
        return;
    }
	
    // 从配置类集合中获取配置类，当前配置集合类为null
    ConfigurationClass existingClass = this.configurationClasses.get(configClass);
    if (existingClass != null) {
        if (configClass.isImported()) {
            if (existingClass.isImported()) {
                existingClass.mergeImportedBy(configClass);
            }
            // Otherwise ignore new imported config class; existing non-imported class overrides it.
            return;
        }
        else {
            // Explicit bean definition found, probably replacing an import.
            // Let's remove the old one and go with the new one.
            this.configurationClasses.remove(configClass);
            this.knownSuperclasses.values().removeIf(configClass::equals);
        }
    }

    
    // ★★ 递归处理配置类
    SourceClass sourceClass = asSourceClass(configClass);
    do {
        // ★★★ 处理配置类中定义的Bean，扫描包
        sourceClass = doProcessConfigurationClass(configClass, sourceClass);
    }
    while (sourceClass != null);

    this.configurationClasses.put(configClass, configClass);
}
```

**处理配置类** org.springframework.context.annotation.ConfigurationClassParser#doProcessConfigurationClass

```java
// 处理配置类
@Nullable
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
    throws IOException {
	// 判断当前配置中是否有@Component注解
    if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
		// 处理类的成员中使用@Componet注解
        processMemberClasses(configClass, sourceClass);
    }

    // 处理@PropertySource注解
    for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
        sourceClass.getMetadata(), PropertySources.class,
        org.springframework.context.annotation.PropertySource.class)) {
        if (this.environment instanceof ConfigurableEnvironment) {
            processPropertySource(propertySource);
        }
        else {
            logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
                        "]. Reason: Environment must implement ConfigurableEnvironment");
        }
    }

    // 处理@ComponentScan注解
    // 获取@Configuration中componentScans、ComponentScan 属性值
    Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
        sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
    
    if (!componentScans.isEmpty() &&
        !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
        
        // 遍历componentScans
        for (AnnotationAttributes componentScan : componentScans) {
            // ★★★ 执行packageScan包扫描，返回使用@Component注解的类
            Set<BeanDefinitionHolder> scannedBeanDefinitions =
                this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());

            // 遍历scannedBeanDefinitions中的Bean；判断是Bean中是否使用@Configuration注解，如使用了，则进行递归调用，进行解析
            for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
                BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
                if (bdCand == null) {
                    bdCand = holder.getBeanDefinition();
                }
                if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
                    // 进行递归解析
                    parse(bdCand.getBeanClassName(), holder.getBeanName());
                }
            }
        }
    }

    // 处理@Import注解; 这里会对使用@Import的注解进行递归调用分析
    processImports(configClass, sourceClass, getImports(sourceClass), true);

    // 处理@ImportSource注解
    AnnotationAttributes importResource =
        AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
    if (importResource != null) {
        String[] resources = importResource.getStringArray("locations");
        Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
        for (String resource : resources) {
            String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
            configClass.addImportedResource(resolvedResource, readerClass);
        }
    }

    /** 
     *  处理类中@Bean注解的方法，将其@Bean中注解的方法加入到BeanDefinitionMap中,JavaConfig配置，加载SpringMVC时会解析@EnableWebMvc注解
     *  @EnableWebMc注解:
     *  @Retention(RetentionPolicy.RUNTIME)
     *  @Target(ElementType.TYPE)
     *  @Documented
     *  @Import(DelegatingWebMvcConfiguration.class)
     *  public @interface EnableWebMvc {
     *  }
     *  此时会将WebMvcConfigurationSupport该类中20个Bean加载进入到BeanDefinitionMap中
     */
    Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
    for (MethodMetadata methodMetadata : beanMethods) {
        configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
    }

    // 处理接口
    processInterfaces(configClass, sourceClass);

    // 处理超类
    if (sourceClass.getMetadata().hasSuperClass()) {
        String superclass = sourceClass.getMetadata().getSuperClassName();
        if (superclass != null && !superclass.startsWith("java") &&
            !this.knownSuperclasses.containsKey(superclass)) {
            this.knownSuperclasses.put(superclass, configClass);
            // Superclass found, return its annotation metadata and recurse
            return sourceClass.getSuperClass();
        }
    }

    // No superclass -> processing is complete
    return null;
}
```

**包扫描** this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());

org.springframework.context.annotation.ComponentScanAnnotationParser#parse

```java
// 这里是解析@Configuration注解中属性，设置属性值
public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, final String declaringClass) {

    // 获取扫描器
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,                     componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);
	
    // BeanName生成器
    Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
    boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
    // 设置BeanName
    scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :
                                 BeanUtils.instantiateClass(generatorClass));
	
    // 设置scope的代理模式，一般都是Default
    ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
    if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
        scanner.setScopedProxyMode(scopedProxyMode);
    }
    else {
        Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
        scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
    }

    // 设置ResourcePattern
    scanner.setResourcePattern(componentScan.getString("resourcePattern"));
    
    // 扫描含有某些字符的类
    for (AnnotationAttributes filter : componentScan.getAnnotationArray("includeFilters")) {
        for (TypeFilter typeFilter : typeFiltersFor(filter)) {
            scanner.addIncludeFilter(typeFilter);
        }
    }
    // 不扫描含有某些字符的类
    for (AnnotationAttributes filter : componentScan.getAnnotationArray("excludeFilters")) {
        for (TypeFilter typeFilter : typeFiltersFor(filter)) {
            scanner.addExcludeFilter(typeFilter);
        }
    }

    // 是否懒加载
    boolean lazyInit = componentScan.getBoolean("lazyInit");
    if (lazyInit) {
        scanner.getBeanDefinitionDefaults().setLazyInit(true);
    }

    // basePackages 扫描包的信息
    Set<String> basePackages = new LinkedHashSet<>();
    String[] basePackagesArray = componentScan.getStringArray("basePackages");
    for (String pkg : basePackagesArray) {
        String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
                                                               ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Collections.addAll(basePackages, tokenized);
    }
    // 解析basePackageClasses信息
    for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
        basePackages.add(ClassUtils.getPackageName(clazz));
    }
    if (basePackages.isEmpty()) {
        basePackages.add(ClassUtils.getPackageName(declaringClass));
    }

    scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
        @Override
        protected boolean matchClassName(String className) {
            return declaringClass.equals(className);
        }
    });
    // 扫描包操作
    return scanner.doScan(StringUtils.toStringArray(basePackages));
}
```

org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan

```java
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    for (String basePackage : basePackages) {
        // ★★★获取所有使用@Component注解的类，不包括接口、抽象类
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
        // 遍历获取需要进行实例化的Bean
        for (BeanDefinition candidate : candidates) {
            ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
            // 获取scope作用域；默认为singleton
            candidate.setScope(scopeMetadata.getScopeName());
            // 生成一个BeanName
            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
            if (candidate instanceof AbstractBeanDefinition) {
                postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
            }
            if (candidate instanceof AnnotatedBeanDefinition) {
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
            if (checkCandidate(beanName, candidate)) {
                // 将Bean进行封装
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                definitionHolder =
                    AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                // 将封装后的Bean加入到集合中
                beanDefinitions.add(definitionHolder);
                // ★ 将获取的Bean加入到BeanDefinitionMap中
                registerBeanDefinition(definitionHolder, this.registry);
            }
        }
    }
    return beanDefinitions;
}
```

findCandidateComponents --->    org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#scanCandidateComponents

```java
private Set<BeanDefinition> scanCandidateComponents(String basePackage) {
    Set<BeanDefinition> candidates = new LinkedHashSet<>();
    try {
        //包路径； ex: classpath*:com/springframework/cn/*/**/*.class
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
            resolveBasePackage(basePackage) + '/' + this.resourcePattern;
        // 通过资源解析器，获取资源；获取该路径下所有的*.class资源文件
        Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);
        boolean traceEnabled = logger.isTraceEnabled();
        boolean debugEnabled = logger.isDebugEnabled();
        // 遍历Resource资源文件
        for (Resource resource : resources) {
            if (traceEnabled) {
                logger.trace("Scanning " + resource);
            }
            // 是否可读
            if (resource.isReadable()) {
                try {
                    MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
                    // 判断是否使用了@Component注解
                    if (isCandidateComponent(metadataReader)) {
                        ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                        sbd.setResource(resource);
                        sbd.setSource(resource);、
                        // ★ 对当前类的类型进行判断，若是接口或者抽象类，则跳过；
                        if (isCandidateComponent(sbd)) {
                            if (debugEnabled) {
                                logger.debug("Identified candidate component class: " + resource);
                            }
                            // 加入到candidate集合中
                            candidates.add(sbd);
                        }
                        else {
                            if (debugEnabled) {
                                logger.debug("Ignored because not a concrete top-level class: " + resource);
                            }
                        }
                    }
                    else {
                        if (traceEnabled) {
                            logger.trace("Ignored because not matching any filter: " + resource);
                        }
                    }
                }
                catch (Throwable ex) {
                    throw new BeanDefinitionStoreException(
                        "Failed to read candidate component class: " + resource, ex);
                }
            }
            else {
                if (traceEnabled) {
                    logger.trace("Ignored because not readable: " + resource);
                }
            }
        }
    }
    catch (IOException ex) {
        throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
    }
    // 返回候选的component
    return candidates;
}
```

###### <6> finishBeanFactoryInitialization(beanFactory);  完成剩余的Bean的初始化工作

- 详细调用流程参考[Spring源码分析调用流程]
- 这里只分析SpringMVC中的9大核心组件中RequestMappingHandlerMapping的初始化工作

 初始化Bean

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)

```java
// 初始化Bean
protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            invokeAwareMethods(beanName, bean);
            return null;
        }, getAccessControlContext());
    }
    else {
        // 调用aware方法，设置属性
        invokeAwareMethods(beanName, bean);
    }

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        // 初始化Bean之前调用Bean的后置处理器
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        // ★★ 调用init方法进行初始化
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
    }
    if (mbd == null || !mbd.isSynthetic()) {
        // 初始化Bean之后调用Bean的后置处理器
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```

调用init方法对Bean进行初始化工作

org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#invokeInitMethods

```java
protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd)
    throws Throwable {

    // 判断当前Bean是否实现了InitializingBean接口
    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        if (logger.isTraceEnabled()) {
            logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
        }
        if (System.getSecurityManager() != null) {
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    ((InitializingBean) bean).afterPropertiesSet();
                    return null;
                }, getAccessControlContext());
            }
            catch (PrivilegedActionException pae) {
                throw pae.getException();
            }
        }
        else {
            // 回调afterPropertiesSet()
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }

	// 调用init方法，进行初始化工作
    if (mbd != null && bean.getClass() != NullBean.class) {
        String initMethodName = mbd.getInitMethodName();
        if (StringUtils.hasLength(initMethodName) &&
            !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
            invokeCustomInitMethod(beanName, bean, mbd);
        }
    }
}
```

★★★ SpringMVC的9大核心组件都实现了InitializingBean接口

- RequestMappingHandlerMapping

  ![image-20200911160733739](.\Image\Spring\RequestMappingHandlerMapping.png)

- RequestMappingHandlerAdapter

  ![image-20200911161027773](.\Image\Spring\RequestMappingHandlerAdapter.png)

- 其余核心组件类图...

RequestMappingHandlerMapping Bean的初始化分析

org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#afterPropertiesSet

```java
@Override
public void afterPropertiesSet() {
    // 构建一个配置文件
    this.config = new RequestMappingInfo.BuilderConfiguration();
    // 设置参数
    this.config.setUrlPathHelper(getUrlPathHelper());
    this.config.setPathMatcher(getPathMatcher());
    this.config.setSuffixPatternMatch(this.useSuffixPatternMatch);
    this.config.setTrailingSlashMatch(this.useTrailingSlashMatch);
    this.config.setRegisteredSuffixPatternMatch(this.useRegisteredSuffixPatternMatch);
    this.config.setContentNegotiationManager(getContentNegotiationManager());
	// 回调父级afterPropertiesSet()函数
    super.afterPropertiesSet();
}
```

org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#afterPropertiesSet

```java
@Override
public void afterPropertiesSet() {
    // 初始化handle方法
    initHandlerMethods();
}
```

初始化handle方法 org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#initHandlerMethods

```java
protected void initHandlerMethods() {
    // 获取所有的Bean
    for (String beanName : getCandidateBeanNames()) {
        // 判断当前Bean是否是以scopedTarget.开头的Bean
        if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
            // ★★ 处理候选的Bean
            processCandidateBean(beanName);
        }
    }
    // ???
    handlerMethodsInitialized(getHandlerMethods());
}
```

org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#processCandidateBean

```java
protected void processCandidateBean(String beanName) {
    Class<?> beanType = null;
    try {
        // 获取BeanType， 即就是Class类型
        beanType = obtainApplicationContext().getType(beanName);
    }
    catch (Throwable ex) {
        // An unresolvable bean type, probably from a lazy bean - let's ignore it.
        if (logger.isTraceEnabled()) {
            logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
        }
    }
    // beanType 不能为null && (beanType中使用了@Controller || @RquestMapping)
    if (beanType != null && isHandler(beanType)) {
        // 检测hanlde方法
        detectHandlerMethods(beanName);
    }
}
```

查找handler方法

org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#detectHandlerMethods

```java
protected void detectHandlerMethods(Object handler) {
    // 获取handler的class类型
	Class<?> handlerType = (handler instanceof String ?
			obtainApplicationContext().getType((String) handler) : handler.getClass());

	if (handlerType != null) {
        // ★获取真实的Class类型，此处用于解析代理类
		Class<?> userType = ClassUtils.getUserClass(handlerType);
        // method: 通过反射获取方法:ReflectionUtils.doWithMethods; T：RequestMappingInfo该方法对象的@RequestMapping信息
		Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
				(MethodIntrospector.MetadataLookup<T>) method -> {
					try {
						return getMappingForMethod(method, userType);
					}
					catch (Throwable ex) {
						throw new IllegalStateException("Invalid mapping on handler class [" +
								userType.getName() + "]: " + method, ex);
					}
				});
		if (logger.isTraceEnabled()) {
			logger.trace(formatMappings(userType, methods));
		}
        // 遍历获取的Methods
		methods.forEach((method, mapping) -> {
			Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
            /**
             * 将mapping映射路径与该URl的处理方法加入到mappingLookup集合中；
             * Map<T, handleMethod> 
             * key 为 mapping的映射路径
             * value 为 该映射路径的处理方法
             */
			registerHandlerMethod(handler, invocableMethod, mapping);
		});
	}
}
```

获取方法的映射信息，并将其封装成RequestMappingInfo对象

org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#getMappingForMethod

```java
@Override
@Nullable
protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    // 获取Method上的@RequestMapping注解信息
    RequestMappingInfo info = createRequestMappingInfo(method);
    if (info != null) {
        // 获取Class上的@RequestMapping注解信息
        RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
        if (typeInfo != null) {
            // 合并信息
            info = typeInfo.combine(info);
        }
        String prefix = getPathPrefix(handlerType);
        if (prefix != null) {
            info = RequestMappingInfo.paths(prefix).build().combine(info);
        }
    }
    // 返回RequestMappingInfo信息； RequestMapping信息中包括了请求参数、返回值类型....
    return info;
}
```

初始化剩余的SpringMVC的核心组件的Bean

###### <7> finishRefresh（）所有Bean初始化完成之后，调用LifecycleProcessor的onfresh()发布响应事件

org.springframework.context.support.AbstractApplicationContext#finishRefresh

```java
protected void finishRefresh() {
    // 清空资源缓存
    clearResourceCaches();

    // 初始化context的生命周期处理器
    initLifecycleProcessor();

    // 刷新
    getLifecycleProcessor().onRefresh();

    // 发布响应事件
    publishEvent(new ContextRefreshedEvent(this));

    // Participate in LiveBeansView MBean, if active.
    LiveBeansView.registerApplicationContext(this);
}
```

最终会调用该方法

org.springframework.context.support.AbstractApplicationContext#publishEvent(java.lang.Object, org.springframework.core.ResolvableType)

```java
protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
	Assert.notNull(event, "Event must not be null");

	// Decorate event as an ApplicationEvent if necessary
	ApplicationEvent applicationEvent;
	if (event instanceof ApplicationEvent) {
		applicationEvent = (ApplicationEvent) event;
	}
	else {
		applicationEvent = new PayloadApplicationEvent<>(this, event);
		if (eventType == null) {
			eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
		}
	}

	// Multicast right now if possible - or lazily once the multicaster is initialized
	if (this.earlyApplicationEvents != null) {
		this.earlyApplicationEvents.add(applicationEvent);
	}
	else {
        // ★★ 获取事件传播器，进行时间事件传播，SpringMVC中Dispatcher使用该方法进行初始化策略
		getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
	}

	// Publish event via parent context as well...
	if (this.parent != null) {
		if (this.parent instanceof AbstractApplicationContext) {
			((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
		}
		else {
			this.parent.publishEvent(event);
		}
	}
}
```

事件传播的底层调用multicastEvent

org.springframework.context.event.SimpleApplicationEventMulticaster#doInvokeListener

```java
@SuppressWarnings({"rawtypes", "unchecked"})
private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
    try {
        // 监听器，调用onApplicationEvent；SpringMVC中DispatcherServlet实现了该方法
        listener.onApplicationEvent(event);
    }
    catch (ClassCastException ex) {
        String msg = ex.getMessage();
        if (msg == null || matchesClassCastMessage(msg, event.getClass())) {
            // Possibly a lambda-defined listener which we could not resolve the generic event type for
            // -> let's suppress the exception and just log a debug message.
            Log logger = LogFactory.getLog(getClass());
            if (logger.isTraceEnabled()) {
                logger.trace("Non-matching event type for listener: " + listener, ex);
            }
        }
        else {
            throw ex;
        }
    }
}

```

- SpringMVC 中使用到了监听器模式

![image-20200909223015613](.\Image\Spring\DispatcherServlet.png)

org.springframework.web.servlet.FrameworkServlet#onApplicationEvent

```java
public void onApplicationEvent(ContextRefreshedEvent event) {
   this.refreshEventReceived = true;
   synchronized (this.onRefreshMonitor) {
      onRefresh(event.getApplicationContext());
   }
}
```

调用onFresh()方法初始化策略

```java
/**
	 * This implementation calls {@link #initStrategies}.
	 */
@Override
protected void onRefresh(ApplicationContext context) {
    initStrategies(context);
}
```

```java
protected void initStrategies(ApplicationContext context) {
    initMultipartResolver(context);
    initLocaleResolver(context);
    initThemeResolver(context);
    initHandlerMappings(context);
    initHandlerAdapters(context);
    initHandlerExceptionResolvers(context);
    initRequestToViewNameTranslator(context);
    initViewResolvers(context);
    initFlashMapManager(context);
}
```



































###### <2> 注册DispatcherServlet源码分析

```java
// DispatcherServlet前段控制器是SpringMVC框架的核心，主要负责调度工作、用于控制流程
DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);
ServletRegistration.Dynamic registration = servletContext.addServlet("dispatcher", dispatcherServlet);
registration.setLoadOnStartup(1);
registration.addMapping("/");
```

###### <3> DispatcherServlet就是一个Servlet，Tomcat启动时，采用动态注册Servlet

Servlet声明周期: Servlet实例化  ----> 初始化(调用init())  ----> 提供服务(doService())  --->  销毁(destory(),当Servlet服务器关闭时，执行destory) 

Servlet的初始化和销毁在执行过程中，只会调用一次

由Servlet的生命周期，可知，会调用DispatcherServlet的init方法进行初始化.

![image-20200909223015613](.\Image\Spring\DispatcherServlet.png)

加载DispatcherServlet.java时，执行静态代码块

```java
static {
    try {
        //  加载默认实例化策略文件DispatcherServlet.properties；该文件中定义了SpringMVC的九大组件
        ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
        defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
    }
    catch (IOException ex) {
        throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
    }
}
```

org.springframework.web.servlet.HttpServletBean#init

```java
@Override
public final void init() throws ServletException {

    // 设置ServletConfig的属性值
    PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
    if (!pvs.isEmpty()) {
        try {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
            ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
            bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
            initBeanWrapper(bw);
            bw.setPropertyValues(pvs, true);
        }
        catch (BeansException ex) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
            }
            throw ex;
        }
    }

    // ★★★ 初始化Servlet
    initServletBean();
}
```

org.springframework.web.servlet.FrameworkServlet#initServletBean

```java
@Override
protected final void initServletBean() throws ServletException {
    getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
    if (logger.isInfoEnabled()) {
        logger.info("Initializing Servlet '" + getServletName() + "'");
    }
    long startTime = System.currentTimeMillis();

    try {
        // ★★★ 初始化webApplicationContex上下文
        this.webApplicationContext = initWebApplicationContext();
        // 初始化FrameworkServlet，这个方法用于以后的扩展
        initFrameworkServlet();
    }
    catch (ServletException | RuntimeException ex) {
        logger.error("Context initialization failed", ex);
        throw ex;
    }

    if (logger.isDebugEnabled()) {
        String value = this.enableLoggingRequestDetails ?
            "shown which may lead to unsafe logging of potentially sensitive data" :
        "masked to prevent unsafe logging of potentially sensitive data";
        logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
                     "': request parameters and headers will be " + value);
    }

    if (logger.isInfoEnabled()) {
        logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
    }
}
```

org.springframework.web.servlet.FrameworkServlet#initWebApplicationContext

```java
// 初始化WebApplicationContext
protected WebApplicationContext initWebApplicationContext() {
    
    /** 
     * 获取Root WebApplicationContext容器
     * 1. 如果采用的XML配置文件的形式，这里会涉及到父子容器
     * 2. 如果采用JavaConfig的配置形式，不涉及父子容器
     */
    WebApplicationContext rootContext =
        WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    WebApplicationContext wac = null;

    if (this.webApplicationContext != null) {
        // this.webApplicationContext 这里的webApplicationContext即是AnnotationConfigWebApplicationContext
        wac = this.webApplicationContext;
        // 查看AnnotationConfigWebApplicationContext类图可知，该类实现了ConfigurableWebApplicationContext接口
        if (wac instanceof ConfigurableWebApplicationContext) {
            ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
            // 判断该容器是否调用过refresh()方法，SpringIOC容器加载Bean实例时已经调用过refresh(),故cwac.isActive()为true
            if (!cwac.isActive()) {
                if (cwac.getParent() == null) {
                    cwac.setParent(rootContext);
                }
                // ★★★ 配置并刷新webApplicationContext上下文
                configureAndRefreshWebApplicationContext(cwac);
            }
        }
    }
    if (wac == null) {
        // No context instance was injected at construction time -> see if one
        // has been registered in the servlet context. If one exists, it is assumed
        // that the parent context (if any) has already been set and that the
        // user has performed any initialization such as setting the context id
        wac = findWebApplicationContext();
    }
    if (wac == null) {
        // No context instance is defined for this servlet -> create a local one
        wac = createWebApplicationContext(rootContext);
    }

    if (!this.refreshEventReceived) {
        synchronized (this.onRefreshMonitor) {
            // ★★★ <2> 调用SpringMVC的onRefresh方法，实例化SpringMVC的9大组件
            onRefresh(wac);
        }
    }

    if (this.publishContext) {
        // Publish the context as a servlet context attribute.
        String attrName = getServletContextAttributeName();
        getServletContext().setAttribute(attrName, wac);
    }

    return wac;
}
```

org.springframework.web.servlet.FrameworkServlet#configureAndRefreshWebApplicationContext

```java
// 配置并刷新webApplication上下文
protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
    
    // 设置webApplication Context上下文的id
    if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
        if (this.contextId != null) {
            wac.setId(this.contextId);
        }
        else {
            // Generate default id...
            wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
                      ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
        }
    }

    // 设置ServletContext，全局的存储信息的空间，服务器开始是就存在，关闭时，销毁，所有用户共享
    wac.setServletContext(getServletContext());
    // 设置ServletConfig的配置信息
    wac.setServletConfig(getServletConfig());
    wac.setNamespace(getNamespace());
    // ★★★ 设置Application的监听事件，这里用于初始化Bean对象之后的回调处理
    wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

    ConfigurableEnvironment env = wac.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment) {
        ((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
    }
	// 设置webApplicationContext 容器刷新之前的一个回调处理
    postProcessWebApplicationContext(wac);
    applyInitializers(wac);
    // 调用IOC容器的refresh(),刷新容器
    wac.refresh();
}
```

org.springframework.web.servlet.DispatcherServlet#onRefresh 初始化SpringMVC的9大核心组件

```java
/**
 * 调用实例化策略
 */
@Override
protected void onRefresh(ApplicationContext context) {
    initStrategies(context);
}
```

org.springframework.web.servlet.DispatcherServlet#initStrategies 初始化核心组件

```java
protected void initStrategies(ApplicationContext context) {
    // 初始化文件上传解析器
    initMultipartResolver(context);
    // 初始化国际化解析器
    initLocaleResolver(context);
    // 初始化主题解析器
    initThemeResolver(context);
    // 初始化HandleMapping处理器映射器
    initHandlerMappings(context);
    // 初始化HandleAdapter处理器适配器
    initHandlerAdapters(context);
    // 初始化处理异常解析器
    initHandlerExceptionResolvers(context);
    // 初始化解析器，用于viewName转换为视图
    initRequestToViewNameTranslator(context);
    // 初始化视图解析器
    initViewResolvers(context);
    // 初始化
    initFlashMapManager(context);
}
```

org.springframework.web.servlet.DispatcherServlet#initHandlerMappings 初始化处理器映射器

```java
private void initHandlerMappings(ApplicationContext context) {
    this.handlerMappings = null;

    if (this.detectAllHandlerMappings) {
        // Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
        Map<String, HandlerMapping> matchingBeans =
            BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
        if (!matchingBeans.isEmpty()) {
            this.handlerMappings = new ArrayList<>(matchingBeans.values());
            // We keep HandlerMappings in sorted order.
            AnnotationAwareOrderComparator.sort(this.handlerMappings);
        }
    }
    else {
        try {
            HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
            this.handlerMappings = Collections.singletonList(hm);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Ignore, we'll add a default HandlerMapping later.
        }
    }

    // Ensure we have at least one HandlerMapping, by registering
    // a default HandlerMapping if no other mappings are found.
    if (this.handlerMappings == null) {
        this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
        if (logger.isTraceEnabled()) {
            logger.trace("No HandlerMappings declared for servlet '" + getServletName() +
                         "': using default strategies from DispatcherServlet.properties");
        }
    }
}
```



