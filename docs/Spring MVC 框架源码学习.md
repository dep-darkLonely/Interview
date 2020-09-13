

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

 DispatcherServlet就是一个Servlet，Tomcat启动时，采用动态注册Servlet

Servlet声明周期: Servlet实例化  ----> 初始化(调用init())  ----> 提供服务(doService())  --->  销毁(destory(),当Servlet服务器关闭时，执行destory) 

Servlet的初始化和销毁在执行过程中，只会调用一次

由Servlet的生命周期，可知，会调用DispatcherServlet的init方法进行初始化.

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
    // 初始化文件上传解析器
    initMultipartResolver(context);
    initLocaleResolver(context);
    initThemeResolver(context);
    // 初始化处理器映射器
    initHandlerMappings(context);
    // 初始化处理器适配器
    initHandlerAdapters(context);
    // 初始化处理器异常解析器
    initHandlerExceptionResolvers(context);
    
    initRequestToViewNameTranslator(context);
    // 初始化视图解析器
    initViewResolvers(context);
    
    initFlashMapManager(context);
}
```

这里只关注HandlerMapping对象

org.springframework.web.servlet.DispatcherServlet#initHandlerMappings

```java
private void initHandlerMappings(ApplicationContext context) {
    this.handlerMappings = null;
	
    // true
    if (this.detectAllHandlerMappings) {
        // 获取ApplicationContext中所有的HandlerMapping Bean
        /**
         * beansOfTypeIncludingAncestors 这里获取主要是通过遍历所有BeanDefinitionNames
         * 集合，然后通过beanName获取对应的Bean，判断是否与指定的bean类型相同
         */
        Map<String, HandlerMapping> matchingBeans =
            BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
        // 非空
        if (!matchingBeans.isEmpty()) {
            // 将所有的HandlerMapping转换为List对象
            this.handlerMappings = new ArrayList<>(matchingBeans.values());
            // 排序处理
            AnnotationAwareOrderComparator.sort(this.handlerMappings);
        }
    }
    else {
        try {
            // 从上下文中获取HandlerMapping Bean对象
            HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
            this.handlerMappings = Collections.singletonList(hm);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Ignore, we'll add a default HandlerMapping later.
        }
    }

    // 若handlerMapping 为 null， 则采用默认的加载策略，初始化handlerMapping对象
    if (this.handlerMappings == null) {
        this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
        if (logger.isTraceEnabled()) {
            logger.trace("No HandlerMappings declared for servlet '" + getServletName() + "': using default strategies from DispatcherServlet.properties");
        }
    }
}
```

其余组件基本类似....

### 3. 请求流程调用源码分析

Servlet初始化完成之后，进行请求处理时都会调用doService方法

```java
// DispatcherServlet前段控制器是SpringMVC框架的核心，主要负责调度工作、用于控制流程
DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);
ServletRegistration.Dynamic registration = servletContext.addServlet("dispatcher", dispatcherServlet);
registration.setLoadOnStartup(1);
// 请求映射路径
registration.addMapping("/"); 
```

###### <1>  查看DispatcherServlet.java 文件的doService() ，处理请求

org.springframework.web.servlet.DispatcherServlet#doService

```java
@Override
protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
    logRequest(request);

    // 这里用于保存一个request的快照
    Map<String, Object> attributesSnapshot = null;
    if (WebUtils.isIncludeRequest(request)) {
        attributesSnapshot = new HashMap<>();
        Enumeration<?> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
                attributesSnapshot.put(attrName, request.getAttribute(attrName));
            }
        }
    }

    // 这里是设置request对象的一些属性
    request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
    request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
    request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
    request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

    if (this.flashMapManager != null) {
        FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
        if (inputFlashMap != null) {
            request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
        }
        request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
        request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
    }

    try {
        // ★★★ 核心处理流程
        doDispatch(request, response);
    }
    finally {
        if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
            // Restore the original attribute snapshot, in case of an include.
            if (attributesSnapshot != null) {
                restoreAttributesAfterInclude(request, attributesSnapshot);
            }
        }
    }
}
```

方法调用

org.springframework.web.servlet.DispatcherServlet#doDispatch

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;

    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

    try {
        ModelAndView mv = null;
        Exception dispatchException = null;

        try {
            // 检查是不是文件上传的Request
            processedRequest = checkMultipart(request);
            multipartRequestParsed = (processedRequest != request);

            // ★★ <2> 确定与请求对应的handler
            mappedHandler = getHandler(processedRequest);
            if (mappedHandler == null) {
                noHandlerFound(processedRequest, response);
                return;
            }

            // ★★ <3> 确定request的handlerAdapter；
            // RequestMappingHandlerMapping ---> RequestMappingHandlerAdapter
            // controller ---> SimpleControllerHandlerAdapter
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

			// 判断当前请求类型
            String method = request.getMethod();
            boolean isGet = "GET".equals(method);
            if (isGet || "HEAD".equals(method)) {
                long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                    return;
                }
            }
			
            // ★★ 拦截器的前置拦截器; Interceptor 前置拦截器必须返回true；否则是不会继续执行的
            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return;
            }

            // ★★ <4> 调用handler方法，进行处理request请求
            mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

            if (asyncManager.isConcurrentHandlingStarted()) {
                return;
            }
			
            // 若不存在视图，则设置默认视图
            applyDefaultViewName(processedRequest, mv);
            // 应用拦截器的后置处理器
            mappedHandler.applyPostHandle(processedRequest, response, mv);
        }
        catch (Exception ex) {
            dispatchException = ex;
        }
        catch (Throwable err) {
            // As of 4.3, we're processing Errors thrown from handler methods as well,
            // making them available for @ExceptionHandler methods and other scenarios.
            dispatchException = new NestedServletException("Handler dispatch failed", err);
        }
        // ★★ <5> 解析ModelAndView对象，渲染视图
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
    }
    catch (Exception ex) {
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
    }
    catch (Throwable err) {
        triggerAfterCompletion(processedRequest, response, mappedHandler,
                               new NestedServletException("Handler processing failed", err));
    }
    finally {
        if (asyncManager.isConcurrentHandlingStarted()) {
            // Instead of postHandle and afterCompletion
            if (mappedHandler != null) {
                mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
            }
        }
        else {
            // Clean up any resources used by a multipart request.
            if (multipartRequestParsed) {
                cleanupMultipart(processedRequest);
            }
        }
    }
}
```

###### <2>根据request请求找到对应的handler处理器

org.springframework.web.servlet.handler.AbstractHandlerMapping#getHandler

```java
// 返回结果为 处理器执行链
@Override
@Nullable
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {

    // ★★ 获取处理请求的Handler
    Object handler = getHandlerInternal(request);
    if (handler == null) {
        handler = getDefaultHandler();
    }
    if (handler == null) {
        return null;
    }
    // Bean name or resolved handler?
    if (handler instanceof String) {
        String handlerName = (String) handler;
        handler = obtainApplicationContext().getBean(handlerName);
    }

    // 将获取的handler组装成Handler执行链
    // 这里会将配置的所有拦截器Interceptor 加入到 处理器执行链中HandlerExecutionChain
    HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

    if (logger.isTraceEnabled()) {
        logger.trace("Mapped to " + handler);
    }
    else if (logger.isDebugEnabled() && !request.getDispatcherType().equals(DispatcherType.ASYNC)) {
        logger.debug("Mapped to " + executionChain.getHandler());
    }

    if (CorsUtils.isCorsRequest(request)) {
        CorsConfiguration globalConfig = this.corsConfigurationSource.getCorsConfiguration(request);
        CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
        CorsConfiguration config = (globalConfig != null ? globalConfig.combine(handlerConfig) : handlerConfig);
        executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
    }

    return executionChain;
}
```

org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getHandlerInternal

```java
@Override
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
    // 通过帮助类，获取request对象的url
    // eg: http://localhost:8080/login/2  ->  /login/2
    String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
    
    // 读取锁
    this.mappingRegistry.acquireReadLock();
    try {
        // 通过 请求的映射路径(URL) 找到对应 handler处理方法以及方法上面@RequestMapping注解对应的RequestMappingInfo对象
        HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
        // 将handlerMethod 对象进行封装
        return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
    }
    finally {
        this.mappingRegistry.releaseReadLock();
    }
}
```

org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod

```java
@Nullable
protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
    List<Match> matches = new ArrayList<>();
    // 从 urlLookup<key(url), value(method)> 集合中将请求的url当做key从集合中去直接获取
    List<T> directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath);
    if (directPathMatches != null) {
        // 将匹配的内容 加入到 matches 集合中
        addMatchingMappings(directPathMatches, matches, request);
    }
    if (matches.isEmpty()) {
		
        addMatchingMappings(this.mappingRegistry.getMappings().keySet(), matches, request);
    }
	
    // 直接匹配没有匹配到的情况下，通过 一系列 算法得到一个最优的处理器
    if (!matches.isEmpty()) {
        Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
        matches.sort(comparator);
        Match bestMatch = matches.get(0);
        if (matches.size() > 1) {
            if (logger.isTraceEnabled()) {
                logger.trace(matches.size() + " matching mappings: " + matches);
            }
            if (CorsUtils.isPreFlightRequest(request)) {
                return PREFLIGHT_AMBIGUOUS_MATCH;
            }
            Match secondBestMatch = matches.get(1);
            if (comparator.compare(bestMatch, secondBestMatch) == 0) {
                Method m1 = bestMatch.handlerMethod.getMethod();
                Method m2 = secondBestMatch.handlerMethod.getMethod();
                String uri = request.getRequestURI();
                throw new IllegalStateException(
                    "Ambiguous handler methods mapped for '" + uri + "': {" + m1 + ", " + m2 + "}");
            }
        }
        // 设置一个最适合(最优)的属性
        request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.handlerMethod);
        handleMatch(bestMatch.mapping, lookupPath, request);
        return bestMatch.handlerMethod;
    }
    else {
        // 没有找到对应的handler方法
        return handleNoMatch(this.mappingRegistry.getMappings().keySet(), lookupPath, request);
    }
}

```

###### <3> 根据请求request找到对应的HandlerMethod处理方法，然后通过处理方法找到对应的参数解析器、返回值处理对象列表

```java
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    if (this.handlerAdapters != null) {
        for (HandlerAdapter adapter : this.handlerAdapters) {
            // 找到对应的处理器 适配器对象
            if (adapter.supports(handler)) {
                // RequestMappingHandlerAdaptyer 对象
                return adapter;
            }
        }
    }
    throw new ServletException("No adapter for handler [" + handler +
                               "]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
}
```

###### <4> 根据处理器适配器对象调用handler方法进行处理请求 (反射)，返回ModelAndView对象

org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter#handle

```java
@Nullable
public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws Exception {

    return handleInternal(request, response, (HandlerMethod) handler);
}
```

org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#handleInternal

```java
protected ModelAndView handleInternal(HttpServletRequest request,
                                      HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

    ModelAndView mav;
    checkRequest(request);

    // Execute invokeHandlerMethod in synchronized block if required.
    // 默认为false
    if (this.synchronizeOnSession) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object mutex = WebUtils.getSessionMutex(session);
            synchronized (mutex) {
                mav = invokeHandlerMethod(request, response, handlerMethod);
            }
        }
        else {
            // No HttpSession available -> no mutex necessary
            mav = invokeHandlerMethod(request, response, handlerMethod);
        }
    }
    else {
        // ★★ 默认的执行调用handler
        mav = invokeHandlerMethod(request, response, handlerMethod);
    }

    if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
        if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
            applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
        }
        else {
            prepareResponse(response);
        }
    }

    return mav;
}
```

org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#invokeHandlerMethod

```java
@Nullable
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
                                           HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
	// 将Request请求封装成 ServletWebRequest
    ServletWebRequest webRequest = new ServletWebRequest(request, response);
    try {
        WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
        ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);
		
        // 将handlerMethod 封装成 ServletInvocableHandlerMethod 对象
        // 该对象中有处理器参数解析器、返回值解析器、数据绑定工厂
        ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
        if (this.argumentResolvers != null) {
            // 设置参数解析器
            invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
        }
        if (this.returnValueHandlers != null) {
            // 设置返回值解析器
            invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
        }
        // 设置数据绑定工厂
        invocableMethod.setDataBinderFactory(binderFactory);
        // 设置得到方法的参数名称
        invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
	
        // 创建一个ModelAndView的容器
        ModelAndViewContainer mavContainer = new ModelAndViewContainer();
        mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
        modelFactory.initModel(webRequest, mavContainer, invocableMethod);
        mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

        AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
        asyncWebRequest.setTimeout(this.asyncRequestTimeout);

        // 设置异步管理器
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        asyncManager.setTaskExecutor(this.taskExecutor);
        asyncManager.setAsyncWebRequest(asyncWebRequest);
        asyncManager.registerCallableInterceptors(this.callableInterceptors);
        asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);

        if (asyncManager.hasConcurrentResult()) {
            Object result = asyncManager.getConcurrentResult();
            mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
            asyncManager.clearConcurrentResult();
            LogFormatUtils.traceDebug(logger, traceOn -> {
                String formatted = LogFormatUtils.formatValue(result, !traceOn);
                return "Resume with async result [" + formatted + "]";
            });
            invocableMethod = invocableMethod.wrapConcurrentResult(result);
        }
		
        // 调用invokeAndHandle处理请求，将返回值设置到mavContainer容器中
        invocableMethod.invokeAndHandle(webRequest, mavContainer);

        if (asyncManager.isConcurrentHandlingStarted()) {
            return null;
        }

        // 获取ModelAndView对象
        return getModelAndView(mavContainer, modelFactory, webRequest);
    }
    finally {
        webRequest.requestCompleted();
    }
}

```

org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod#invokeAndHandle

```java
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
                            Object... providedArgs) throws Exception {

	// <4.1> 通过反射的方式，调用方法处理request请求，获取返回值
    Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
    // 设置请求的响应状态
    setResponseStatus(webRequest);

    if (returnValue == null) {
        if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
            disableContentCachingIfNecessary(webRequest);
            mavContainer.setRequestHandled(true);
            return;
        }
    }
    else if (StringUtils.hasText(getResponseStatusReason())) {
        mavContainer.setRequestHandled(true);
        return;
    }

    mavContainer.setRequestHandled(false);
    // 这里是判断是否存在 返回值处理器
    Assert.state(this.returnValueHandlers != null, "No return value handlers");
    try {
        // <4.2> ★★★ 使用返回值处理器，处理返回值
        this.returnValueHandlers.handleReturnValue(
            returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
    }
    catch (Exception ex) {
        if (logger.isTraceEnabled()) {
            logger.trace(formatErrorForReturnValue(returnValue), ex);
        }
        throw ex;
    }
}
```

<4.1> org.springframework.web.method.support.InvocableHandlerMethod#invokeForRequest

```java
@Nullable
public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
                               Object... providedArgs) throws Exception {

    // ★ 获取方法的参数值
    Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
    if (logger.isTraceEnabled()) {
        logger.trace("Arguments: " + Arrays.toString(args));
    }
    // ★ 执行invoke
    return doInvoke(args);
}
```

**方法参数值**

org.springframework.web.method.support.InvocableHandlerMethod#getMethodArgumentValues

```java
protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
                                           Object... providedArgs) throws Exception {

    // 获取方法参数
    MethodParameter[] parameters = getMethodParameters();
    if (ObjectUtils.isEmpty(parameters)) {
        return EMPTY_ARGS;
    }

    Object[] args = new Object[parameters.length];
    // 遍历方法参数
    for (int i = 0; i < parameters.length; i++) {
        MethodParameter parameter = parameters[i];
        // 获取参数的名称
        parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
        // 发现参数上使用的注解
        args[i] = findProvidedArgument(parameter, providedArgs);
        if (args[i] != null) {
            continue;
        }
        if (!this.resolvers.supportsParameter(parameter)) {
            throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
        }
        try {
            // 使用解析器解析参数，这里有26中参数解析器
            args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
        }
        catch (Exception ex) {
            // Leave stack trace for later, exception may actually be resolved and handled...
            if (logger.isDebugEnabled()) {
                String exMsg = ex.getMessage();
                if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
                    logger.debug(formatArgumentError(parameter, exMsg));
                }
            }
            throw ex;
        }
    }
    return args;
}
```

org.springframework.web.method.support.InvocableHandlerMethod#doInvoke

```java
@Nullable
protected Object doInvoke(Object... args) throws Exception {
    ReflectionUtils.makeAccessible(getBridgedMethod());
    try {
        // 使用桥接方法获取，调用invoke方法
        // 反射调用，实际上这里就会调用controller中对应的处理方法
        return getBridgedMethod().invoke(getBean(), args);
    }
    catch (IllegalArgumentException ex) {
        assertTargetBean(getBridgedMethod(), getBean(), args);
        String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
        throw new IllegalStateException(formatInvokeError(text, args), ex);
    }
    catch (InvocationTargetException ex) {
        // Unwrap for HandlerExceptionResolvers ...
        Throwable targetException = ex.getTargetException();
        if (targetException instanceof RuntimeException) {
            throw (RuntimeException) targetException;
        }
        else if (targetException instanceof Error) {
            throw (Error) targetException;
        }
        else if (targetException instanceof Exception) {
            throw (Exception) targetException;
        }
        else {
            throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
        }
    }
}
```

<4.2>  使用返回值处理器解析返回值

org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite#handleReturnValue

```java
@Override
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
                              ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

    // 通过返回值和返回值类型，获取合适的返回值处理器
    HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
    if (handler == null) {
        throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
    }
    // 处理返回值
    handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
}
```

org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor#handleReturnValue

```java
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

    // 设置请求处理完成
    mavContainer.setRequestHandled(true);
    // 创建input消息；实际上就是对webRequest进行封装
    ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
    // 创建output消息
    ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

    // 将返回值转换为指定类型输出到outputMessage中
    writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
}
```

将返回值转换为指定类型输出到outputMessage中

org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor#writeWithMessageConverters(T, org.springframework.core.MethodParameter, org.springframework.http.server.ServletServerHttpRequest, org.springframework.http.server.ServletServerHttpResponse)

```java
protected <T> void writeWithMessageConverters(@Nullable T value, MethodParameter returnType,
                                              ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)
    throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

    Object body;
    Class<?> valueType;
    Type targetType;

    if (value instanceof CharSequence) {
        body = value.toString();
        valueType = String.class;
        targetType = String.class;
    }
    else {
        // 赋值操作
        body = value;
        // 获取返回值类型
        valueType = getReturnValueType(body, returnType);
        targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());
    }

    // 查看返回值的类型是否 实现了 Resource接口
    if (isResourceType(value, returnType)) {
        outputMessage.getHeaders().set(HttpHeaders.ACCEPT_RANGES, "bytes");
        if (value != null && inputMessage.getHeaders().getFirst(HttpHeaders.RANGE) != null &&
            outputMessage.getServletResponse().getStatus() == 200) {
            Resource resource = (Resource) value;
            try {
                List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
                outputMessage.getServletResponse().setStatus(HttpStatus.PARTIAL_CONTENT.value());
                body = HttpRange.toResourceRegions(httpRanges, resource);
                valueType = body.getClass();
                targetType = RESOURCE_REGION_LIST_TYPE;
            }
            catch (IllegalArgumentException ex) {
                outputMessage.getHeaders().set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
                outputMessage.getServletResponse().setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
            }
        }
    }

    // 没有实现了 Resource接口
    MediaType selectedMediaType = null;
    // 获取Response的 contextType类型
    MediaType contentType = outputMessage.getHeaders().getContentType();
    if (contentType != null && contentType.isConcrete()) {
        if (logger.isDebugEnabled()) {
            logger.debug("Found 'Content-Type:" + contentType + "' in response");
        }
        selectedMediaType = contentType;
    }
    else {
        // 从封装的request对象中获取httpServletRequest对象
        HttpServletRequest request = inputMessage.getServletRequest();
        // 请求类型
        List<MediaType> acceptableTypes = getAcceptableMediaTypes(request);
        // 获取该请求响应的输出类型: application/json
        List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType);

        if (body != null && producibleTypes.isEmpty()) {
            throw new HttpMessageNotWritableException(
                "No converter found for return value of type: " + valueType);
        }
        // 筛选符合要求的响应输出类型
        List<MediaType> mediaTypesToUse = new ArrayList<>();
        for (MediaType requestedType : acceptableTypes) {
            for (MediaType producibleType : producibleTypes) {
                if (requestedType.isCompatibleWith(producibleType)) {
                    mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
                }
            }
        }
        if (mediaTypesToUse.isEmpty()) {
            if (body != null) {
                throw new HttpMediaTypeNotAcceptableException(producibleTypes);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("No match for " + acceptableTypes + ", supported: " + producibleTypes);
            }
            return;
        }

        // 排序
        MediaType.sortBySpecificityAndQuality(mediaTypesToUse);

        for (MediaType mediaType : mediaTypesToUse) {
            if (mediaType.isConcrete()) {
                selectedMediaType = mediaType;
                break;
            }
            else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
                selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
                break;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Using '" + selectedMediaType + "', given " +
                         acceptableTypes + " and supported " + producibleTypes);
        }
    }

    if (selectedMediaType != null) {
        // 获取确定的媒体格式类型
        selectedMediaType = selectedMediaType.removeQualityValue();
        // 进行消息转换; this.messageConverters中存在9个消息转换器
        // application/json ---> MappingJackson2HttpMessageConverter
        for (HttpMessageConverter<?> converter : this.messageConverters) {
            // 判断当前 messageConvert 是否实现了 GenericHttpMessageConverter 接口
            GenericHttpMessageConverter genericConverter = (converter instanceof GenericHttpMessageConverter ?
                                                            (GenericHttpMessageConverter<?>) converter : null);
            
            // 判断当前转换器是否可以 写为指定类型(即为序列化为指定类型)
            if (genericConverter != null ?
                ((GenericHttpMessageConverter) converter).canWrite(targetType, valueType, selectedMediaType) :
                converter.canWrite(valueType, selectedMediaType)) {

                // ★★★ 这里是获取一个切面；调用beforeBodyWrite方法，对body中的数据在进行处理
                // 主要用于扩展吧，可以将controller中的返回值在进一次的封装
                // 如果要进行扩展，则必须实现ResponseBodyAdvice该接口
                body = getAdvice().beforeBodyWrite(body, returnType, selectedMediaType,
                                                   (Class<? extends HttpMessageConverter<?>>) converter.getClass(),
                                                   inputMessage, outputMessage);
                if (body != null) {
                    Object theBody = body;
                    LogFormatUtils.traceDebug(logger, traceOn ->
                                              "Writing [" + LogFormatUtils.formatValue(theBody, !traceOn) + "]");

                    // 设置reponse 中关于文件下载的一些属性
                    addContentDispositionHeader(inputMessage, outputMessage);
                    if (genericConverter != null) {
                        // ★★ 底层使用ObjectMapper.writer() 将body中内容写入到outputMessage中
                        genericConverter.write(body, targetType, selectedMediaType, outputMessage);
                    }
                    else {
                        ((HttpMessageConverter) converter).write(body, selectedMediaType, outputMessage);
                    }
                }
                else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Nothing to write: null body");
                    }
                }
                return;
            }
        }
    }

    if (body != null) {
        throw new HttpMediaTypeNotAcceptableException(this.allSupportedMediaTypes);
    }
}
```

- MappingJackson2HttpMessageConverter的类图

![image-20200913120121679](.\Image\Spring\MappingJackson2MessageConveter.png)

###### <5> 解析ModelAndView对象，渲染视图

org.springframework.web.servlet.DispatcherServlet#processDispatchResult

```java
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
                                   @Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
                                   @Nullable Exception exception) throws Exception {

    boolean errorView = false;

    if (exception != null) {
        if (exception instanceof ModelAndViewDefiningException) {
            logger.debug("ModelAndViewDefiningException encountered", exception);
            mv = ((ModelAndViewDefiningException) exception).getModelAndView();
        }
        else {
            Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
            mv = processHandlerException(request, response, handler, exception);
            errorView = (mv != null);
        }
    }

    // 存在ModelAndView对象，并且 该对象没有被清除
    if (mv != null && !mv.wasCleared()) {
        // 渲染视图
        render(mv, request, response);
        if (errorView) {
            WebUtils.clearErrorRequestAttributes(request);
        }
    }
    else {
        if (logger.isTraceEnabled()) {
            logger.trace("No view rendering, null ModelAndView returned.");
        }
    }

    if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
        // Concurrent handling started during a forward
        return;
    }

    if (mappedHandler != null) {
        mappedHandler.triggerAfterCompletion(request, response, null);
    }
}
```



```java
protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {

    // 设置国际化内容
    Locale locale =
        (this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
    response.setLocale(locale);

    View view;
    // 获取视图名称
    String viewName = mv.getViewName();
    if (viewName != null) {
        // 解析视图名称，返回view对象
        view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
        if (view == null) {
            throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
                                       "' in servlet with name '" + getServletName() + "'");
        }
    }
    else {
        // No need to lookup: the ModelAndView object contains the actual View object.
        view = mv.getView();
        if (view == null) {
            throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
                                       "View object in servlet with name '" + getServletName() + "'");
        }
    }

    // Delegate to the View object for rendering.
    if (logger.isTraceEnabled()) {
        logger.trace("Rendering view [" + view + "] ");
    }
    try {
        if (mv.getStatus() != null) {
            response.setStatus(mv.getStatus().value());
        }
        // 进行渲染
        view.render(mv.getModelInternal(), request, response);
    }
    catch (Exception ex) {
        if (logger.isDebugEnabled()) {
            logger.debug("Error rendering view [" + view + "]", ex);
        }
        throw ex;
    }
}
```

### 4. SpringMVC 的工作原理图

![image-20200913130628007](.\Image\Spring\SpringMVC工作原理图.png)