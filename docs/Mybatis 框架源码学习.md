# Mybatis 框架源码学习

### 1. 创建Mybatis配置文件信息

```java
package com.springframework.cn.config;

import com.springframework.cn.annotation.MybatisDao;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

public class DatabaseConfig {
	
    /**
	 * 配置数据源信息
	 * @return
	 */
	@Bean("pooledDataSource")
	public PooledDataSource getDataSource() {
		PooledDataSource pooledDataSource = new PooledDataSource();
		pooledDataSource.setDriver("com.mysql.cj.jdbc.Driver");
		pooledDataSource.setUrl("jdbc:mysql://localhost:3306/mybatis?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT");
		pooledDataSource.setUsername("root");
		pooledDataSource.setPassword("root");
		pooledDataSource.setPoolPingQuery("SELECT 'x' FROM DUAL");
		return pooledDataSource;
	}
    
    // 配置SqlSessionFactoryBean
	@Bean("sqlSessionFactoryBean")
	public SqlSessionFactoryBean getSqlSessionFactoryBean(PooledDataSource pooledDataSource)
			throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();

		// dataSource
		sqlSessionFactoryBean.setDataSource(pooledDataSource);
		// 映射mapper文件路径
	    Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:com/springframework/cn/dao/*.xml");
		sqlSessionFactoryBean.setMapperLocations(resources);
		sqlSessionFactoryBean.setTypeAliasesPackage("com.spingframework.cn");
		return sqlSessionFactoryBean;
	}

    /**
	 * 配置自动扫描Mapper接口，生成代理并将其注入到SpringIOC容器中
	 * @return
	 */
	@Bean("mapperScannerConfigurer")
	public MapperScannerConfigurer doMapperScannerConfigurer() {
		MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
		//mapperScannerConfigurer.setAnnotationClass(MybatisDao.class);
		mapperScannerConfigurer.setBasePackage("com.springframework.cn.dao");
		return mapperScannerConfigurer;
	}


	/**
	 * 事务管理
	 * @param dataSource
	 * @return
	 */
	@Bean("dataSourceTransactionManager")
	public DataSourceTransactionManager transactionManager(PooledDataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}
}

```

创建Spring配置文件信息，在Spring容器启动的时候，加载Mybatis配置文件信息，交给Spring配置处理

```java
@Configuration
@ComponentScan(basePackages = "com.springframework.cn.*")
@EnableAspectJAutoProxy
@Import({ MvcConfig.class, DatabaseConfig.class })
public class AppConfig {}
```

### 2. Mybatis配置文件中Bean的结构分析

###### 2.1 MapperScannerConfigurer 类图

![image-20200917102037692](.\Image\Mybatis\MapperScannerConfigurer.png)



MapperScannerConfigurer 实现了 InitializingBean 、 ApplicationContextAware、BeanNameAware、BeanFactoryPostProcessor、BeanDefinitionRegistryPostProcessor接口；

- ApplicationContextAware、BeanNameAware、InitializingBean 是在Bean实例进行初始化时(initializeBean)调用,调用顺序是: 

  调用 aware接口 (进行set操作)   ->   调用afterPropertiesSet() 方法  -> 其次调用init()

  org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean

  ```java
  protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
      if (System.getSecurityManager() != null) {
          AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
              invokeAwareMethods(beanName, bean);
              return null;
          }, getAccessControlContext());
      }
      else {
          // 调用实现Aware接口的类
          invokeAwareMethods(beanName, bean);
      }
  
      Object wrappedBean = bean;
      if (mbd == null || !mbd.isSynthetic()) {
          wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
      }
  
      try {
          // 调用自定义的init方法进行初始化，在这之前会调用afterPropertiesSet()方法
          invokeInitMethods(beanName, wrappedBean, mbd);
      }
      catch (Throwable ex) {
          throw new BeanCreationException(
              (mbd != null ? mbd.getResourceDescription() : null),
              beanName, "Invocation of init method failed", ex);
      }
      if (mbd == null || !mbd.isSynthetic()) {
          wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
      }
  
      return wrappedBean;
  }
  
  ```

- BeanFactoryPostProcessor、BeanDefinitionRegistryPostProcessor接口在invokeBeanFactoryPostProcessors中进行调用；

  调用顺序 BeanDefinitionRegistryPostProcessor  ---> BeanFactoryPostProcessor

  org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors

  ```java
  public static void invokeBeanFactoryPostProcessors(
      ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
  
      // Invoke BeanDefinitionRegistryPostProcessors first, if any.
      Set<String> processedBeans = new HashSet<>();
  
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
  
          // First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
          /** 
           * 获取实现接口BeanDefinitionRegistryPostProcessor的后置处理器，这里还没有进行配置文件的解析，
           * 只能获取到org.springframework.context.annotation.internalConfigurationAnnotationProcessor,
           * 此时还无法获取到MapperScannerConfigurer;
           * 
           * internalConfigurationAnnotationProcessor对应的类为ConfigurationClassPostProcessor，该类实现了
           * PriorityOrdered接口
           */
          String[] postProcessorNames =
              beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
          for (String ppName : postProcessorNames) {
              if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                  currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                  processedBeans.add(ppName);
              }
          }
          // 排序
          sortPostProcessors(currentRegistryProcessors, beanFactory);
          registryProcessors.addAll(currentRegistryProcessors);
          // 调用internalConfigurationAnnotationProcessor对应的后置处理器进行配置文件解析，解析@Configuration, 会将所有的Bean加载到BeanDefinitionMap集合中.
          invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
          currentRegistryProcessors.clear();
  
          // Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
          /** 
           * 再次获取BeanDefinitionRegistryPostProcessor的后置处理器
           * 可以获取到2个后置处理器，分别为internalConfigurationAnnotationProcessor和MapperScannerConfigurer
           * 后置处理器，但是没有实现Order接口
           */
          postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
          for (String ppName : postProcessorNames) {
              if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
                  currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                  processedBeans.add(ppName);
              }
          }
          sortPostProcessors(currentRegistryProcessors, beanFactory);
          registryProcessors.addAll(currentRegistryProcessors);
          // 调用后置处理器进行处理
          invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
          currentRegistryProcessors.clear();
  
          // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
          boolean reiterate = true;
          while (reiterate) {
              reiterate = false;
              // 再次获取实现BeanDefinitionRegistryPostProcessor接口的后置处理器；这里还是2个
              postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
              for (String ppName : postProcessorNames) {
                  // 此时当前MapperScannerConfigurer 的Bean实例对象还没有进行创建，初始化
                  if (!processedBeans.contains(ppName)) {
                      // ★★ MapperScannerConfigurer的Bean实例对象不存在，调用getBean对象从单例缓存池中获取对象实例，
                      // 对象实例不存在，创建Bean的实例对象，加入到单例缓存池singletonObjects中
                      currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
  
                      // 加入到已经处理完毕的Bean的集合中
                      processedBeans.add(ppName);
                      reiterate = true;
                  }
              }
              sortPostProcessors(currentRegistryProcessors, beanFactory);
              registryProcessors.addAll(currentRegistryProcessors);
              // ★★ 调用MapperScannerConfigurer的后置处理器
              invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
              currentRegistryProcessors.clear();
          }
  
          // Now, invoke the postProcessBeanFactory callback of all processors handled so far.
          // ★★ 调用BeanFactoryPostProcessor的后置处理
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
      List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
      for (String postProcessorName : orderedPostProcessorNames) {
          orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
      }
      sortPostProcessors(orderedPostProcessors, beanFactory);
      invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);
  
      // Finally, invoke all other BeanFactoryPostProcessors.
      List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
      for (String postProcessorName : nonOrderedPostProcessorNames) {
          nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
      }
      invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
  
      // Clear cached merged bean definitions since the post-processors might have
      // modified the original metadata, e.g. replacing placeholders in values...
      beanFactory.clearMetadataCache();
  }
  ```

###### 2.2 SqlSessionFactoryBean 类图

![image-20200917113740520](.\Image\Mybatis\SqlSessionFactoryBean.png)

​       SqlSessionFactoryBean实现了InitializingBean、ApplicationListener、FactoryBean接口， 调用顺序是afterPropertiesSet(InitializingBean)  ---> onApplicationEvent(ApplicationListener接口中的回调事件)

```java
/**
 * Finish the refresh of this context, invoking the LifecycleProcessor's
 * onRefresh() method and publishing the
 * {@link org.springframework.context.event.ContextRefreshedEvent}.
 */
// refresh() 方法中的最后一步
protected void finishRefresh() {
    // Clear context-level resource caches (such as ASM metadata from scanning).
    clearResourceCaches();

    // Initialize lifecycle processor for this context.
    initLifecycleProcessor();

    // Propagate refresh to lifecycle processor first.
    getLifecycleProcessor().onRefresh();

    // ★ 发布事件
    publishEvent(new ContextRefreshedEvent(this));

    // Participate in LiveBeansView MBean, if active.
    LiveBeansView.registerApplicationContext(this);
}
```

最终 会调用doInvokeListener

org.springframework.context.event.SimpleApplicationEventMulticaster#doInvokeListener

```java
private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
    try {
        // 回调onApplicationEvent
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

###### 2.3 PooledDataSource 类图

![image-20200917140425113](.\Image\Mybatis\PooledDataSource.png)

PooledDataSource 是默认的数据源连接池，实现了DataSource接口

### 3. Mybatis 源码调用流程分析

根据Spring容器的启动过程(Spring容器启动过程可查看Spring源码分析):

1. 首先会加载创建BeanFactory，其次是填充BeanFactory属性，创建BeanDefinition扫描器

2. 调用BeanFactory后置处理器，解析@Configuration配置类，扫描Bean将Bean封装为BeanDefinition对象添加到BeanDefinitionMap集合中

    2.1  首先会获取BeanDefinitionRegistryPostProcessor的后置处理器，解析@Configuration配置文件类，加载所有的Bean到BeanDefinitionMap集合中

    2.2  再次获取BeanDefinitionRegistryPostProcessor的后置处理器，调用MapperScannerConfigurer的postProcessBeanDefinitionRegistry处理函数

   2.3 调用BeanFactoryPostProcessor的后置处理器

3. 实例化剩余的所有单例的Bean对象，并将其加入到单例缓存池singletonObjects集合中

4. 完成refresh()，发布事件，回调onApplicationEvent

3.1 调用MapperScannerConfigurer的postProcessBeanDefinitionRegistry方法，扫描Mapper接口

org.mybatis.spring.mapper.MapperScannerConfigurer#postProcessBeanDefinitionRegistry

```java
  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    // 默认为false，判断是否存在属性值中是否存在占位符
    if (this.processPropertyPlaceHolders) {
      processPropertyPlaceHolders();
    }

    // 创建ClassPathMapper的一个扫描器
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
    scanner.setAddToConfig(this.addToConfig);
    // 设置扫描的注解class对象
    scanner.setAnnotationClass(this.annotationClass);
    scanner.setMarkerInterface(this.markerInterface);
    // 设置SqlSessionFactory 对象
    scanner.setSqlSessionFactory(this.sqlSessionFactory);
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
    scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
    scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
    scanner.setResourceLoader(this.applicationContext);
    // 设置BeanName的生成器
    scanner.setBeanNameGenerator(this.nameGenerator);
    scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
      
     // 设置是否是懒加载
    if (StringUtils.hasText(lazyInitialization)) {
      scanner.setLazyInitialization(Boolean.valueOf(lazyInitialization));
    }
    // 注册过滤器
    scanner.registerFilters();
    // ★★★ 扫描包
    scanner.scan(
        StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  }
```

包扫描org.springframework.context.annotation.ClassPathBeanDefinitionScanner#scan

```java
public int scan(String... basePackages) {
    int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

    // 调用扫描包的操作
    doScan(basePackages);

    // Register annotation config processors, if necessary.
    if (this.includeAnnotationConfig) {
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
    }

    return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
}
```

★★★ ClassPathMapperScanner 这个类的作用就是扫描ClassPath 路径下Mapper接口，扫描并注册到BeanDefinitionMap集合中

- 扫描并注册候选的BeanDefinition对象，也就是Dao接口的BeanDefinition对象，将其注册到BeanDefinitionMap集合中
- **★★ 调用processBeanDefinitions()，用来修改Mapper接口的BeanClass类型为MapperFactoryBean类型；BeanClass类型为当前Bean对象对应的原始类型，替换Mapper接口的BeanClass类型，将实际的类型当前构造器参数，这里了涉及到一个设计模式：建造者模式**

org.mybatis.spring.mapper.ClassPathMapperScanner#doScan

```java
@Override
public Set<BeanDefinitionHolder> doScan(String... basePackages) {
    
    // 调用父类的包扫描
    Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

    if (beanDefinitions.isEmpty()) {
        logger.warn("No MyBatis mapper was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
    } else {
        // ★★★  修改BeanDefinition的BeanClass属性修改为MapperFactoryBean
        // 设置构造器参数
        processBeanDefinitions(beanDefinitions);
    }

    return beanDefinitions;
}
```

调用父类的doScan()进行扫描包操作

org.springframework.context.annotation.ClassPathBeanDefinitionScanner#doScan

```java
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    // 创建一个BeanDefinitionHolder的集合； BeanDefinitionHolder是对BeanDefinition的一个封装
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    // 遍历package
    for (String basePackage : basePackages) {
        
        // ★★★ 从给定的basePackage中扫描给符合要求的组件即候选的组件
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
        // 遍历候选的Bean
        for (BeanDefinition candidate : candidates) {
            // 使用scopeMetadataResolver解析器解析候选的Bean对象，获取scopeMetadata
            ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
            // 设置scope作用域
            candidate.setScope(scopeMetadata.getScopeName());
            // 使用BeanName生成器，生成一个BeanName
            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
            // 判断当前候选的Bean是否实现了AbstractBeanDefinition接口
            if (candidate instanceof AbstractBeanDefinition) {
                postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
            }
            // 判断当前候选的Bean是否实现了AnnotatedBeanDefinition接口
            if (candidate instanceof AnnotatedBeanDefinition) {
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
            
            // 检查候选的Bean对象
            if (checkCandidate(beanName, candidate)) {
                // 将Bean对象封装成BeanDefinitionHolder对象
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                // 设置作用域的代理模式
                definitionHolder =
                    AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                // 将Bean添加到集合BeanDefinitions集合中
                beanDefinitions.add(definitionHolder);
                // 将BeanDefinition对象注册到BeanDefinitionMap集合中，即这里可以将UserDao接口对象
                registerBeanDefinition(definitionHolder, this.registry);
            }
        }
    }
    return beanDefinitions;
}
```

获取指定包路径下的候选的Component

org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#findCandidateComponents

```java
/**
 * Scan the class path for candidate components.
 * @param basePackage the package to check for annotated classes
 * @return a corresponding Set of autodetected bean definitions
 */
public Set<BeanDefinition> findCandidateComponents(String basePackage) {
    if (this.componentsIndex != null && indexSupportsIncludeFilters()) {
        return addCandidateComponentsFromIndex(this.componentsIndex, basePackage);
    }
    else {
        // 扫描候选的Components
        return scanCandidateComponents(basePackage);
    }
}
```

扫描组件scanCandidateComponents

org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#scanCandidateComponents

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
                        sbd.setSource(resource);
                        // ★★★★★
                        // Spring中对当前类的类型进行判断，若是接口或者抽象类，则跳过；
                        // Mybatis中ClassPathMapperScanner对该方法进行了重写，若class为Interface && 单独(顶层接口)，则为true；其余则为false；这里只处理接口
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

★★★ ClassPathMapperScanner 类图

![image-20200918161837740](.\Image\Mybatis\ClassPathMapperScanner.png)

ClassPathMapperScanner 方法图

