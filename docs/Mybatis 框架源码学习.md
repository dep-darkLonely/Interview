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

    2.2  再次获取BeanDefinitionRegistryPostProcessor的后置处理器，调用MapperScannerConfigurer的postProcessBeanDefinitionRegistry处理函数，

   2.3 调用BeanFactoryPostProcessor的后置处理器

3. 实例化剩余的所有单例的Bean对象，并将其加入到单例缓存池singletonObjects集合中

4. 完成refresh()，发布事件，回调onApplicationEvent

###### 3.1 调用MapperScannerConfigurer的postProcessBeanDefinitionRegistry方法，扫描Mapper接口

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
    
    // 调用父类的包扫描； 得到所有的Mapper接口
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
                // 处理@Autowired注解
                postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
            }
            // 判断当前候选的Bean是否实现了AnnotatedBeanDefinition接口
            if (candidate instanceof AnnotatedBeanDefinition) {
               // 处理一些 公共的注解 @Description、@Role....
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
                // ★★★ 将BeanDefinition对象注册到BeanDefinitionMap集合中，即这里可以将UserDao接口对象
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

![image-20200922082251877](.\Image\Mybatis\ClassPathMapperScanner_Method.png)

```java
/**
 * {@inheritDoc}
 */
@Override
protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    // 判断当前类是一个 接口 && 当前类是独立的(顶级类 或 嵌套类(静态内部类))
    return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
}
```

将扫描得到的BeanDefinition添加到BeanDefinitionMap集合中

org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition

```java
@Override
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
    throws BeanDefinitionStoreException {

    Assert.hasText(beanName, "Bean name must not be empty");
    Assert.notNull(beanDefinition, "BeanDefinition must not be null");

    // 判断 beanDefinition是否实现了AbstractBeanDefinition接口
    if (beanDefinition instanceof AbstractBeanDefinition) {
        try {
            // 验证BeanDefinition
            ((AbstractBeanDefinition) beanDefinition).validate();
        }
        catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                                                   "Validation of bean definition failed", ex);
        }
    }

    // 根据beanName从BeanDefinitionMap集合中获取指定名称的对象
    BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
    if (existingDefinition != null) {
        if (!isAllowBeanDefinitionOverriding()) {
            throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
        }
        else if (existingDefinition.getRole() < beanDefinition.getRole()) {
            // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
            if (logger.isInfoEnabled()) {
                logger.info("Overriding user-defined bean definition for bean '" + beanName +
                            "' with a framework-generated bean definition: replacing [" +
                            existingDefinition + "] with [" + beanDefinition + "]");
            }
        }
        else if (!beanDefinition.equals(existingDefinition)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Overriding bean definition for bean '" + beanName +
                             "' with a different definition: replacing [" + existingDefinition +
                             "] with [" + beanDefinition + "]");
            }
        }
        else {
            if (logger.isTraceEnabled()) {
                logger.trace("Overriding bean definition for bean '" + beanName +
                             "' with an equivalent definition: replacing [" + existingDefinition +
                             "] with [" + beanDefinition + "]");
            }
        }
        // 将beanName作为key，beanDefinition作为value加入到beanDefinitionMap集合中
        this.beanDefinitionMap.put(beanName, beanDefinition);
    }
    else {
        if (hasBeanCreationStarted()) {
            // Cannot modify startup-time collection elements anymore (for stable iteration)
            synchronized (this.beanDefinitionMap) {
                this.beanDefinitionMap.put(beanName, beanDefinition);
                List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
                updatedDefinitions.addAll(this.beanDefinitionNames);
                updatedDefinitions.add(beanName);
                this.beanDefinitionNames = updatedDefinitions;
                removeManualSingletonName(beanName);
            }
        }
        else {
            // Still in startup registration phase
            this.beanDefinitionMap.put(beanName, beanDefinition);
            this.beanDefinitionNames.add(beanName);
            removeManualSingletonName(beanName);
        }
        this.frozenBeanDefinitionNames = null;
    }

    if (existingDefinition != null || containsSingleton(beanName)) {
        resetBeanDefinition(beanName);
    }
}
```

**★★★  processBeanDefinitions()修改BeanDefinition的BeanClass属性修改为MapperFactoryBean并且设置构造器参数**

org.mybatis.spring.mapper.ClassPathMapperScanner#processBeanDefinitions

```java
private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
    GenericBeanDefinition definition;
    // 遍历扫描到Mapper接口
    for (BeanDefinitionHolder holder : beanDefinitions) {
      // 获取BeanDefinition，并强制转换为GenericBeanDefinition
      definition = (GenericBeanDefinition) holder.getBeanDefinition();
      // 获取beanClassName
      // ex: userDao --> com.springframework.cn.dao.UserDao
      String beanClassName = definition.getBeanClassName();
      LOGGER.debug(() -> "Creating MapperFactoryBean with name '" + holder.getBeanName() + "' and '" + beanClassName
          + "' mapperInterface");

      // the mapper interface is the original class of the bean
      // but, the actual class of the bean is MapperFactoryBean
      // ★★ 设置BeanDefinition实例化时的参数值; 
      // ex: com.springframework.cn.dao.UserDao
      definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59
      // ★★ 设置beanDefinition的具体类型为MapperFactoryBean,具体的类而不是接口或者抽象类
      definition.setBeanClass(this.mapperFactoryBeanClass);
      // 设置属性值
      definition.getPropertyValues().add("addToConfig", this.addToConfig);

      boolean explicitFactoryUsed = false;
      if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
        definition.getPropertyValues().add("sqlSessionFactory",
            new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
        explicitFactoryUsed = true;
      } else if (this.sqlSessionFactory != null) {
        definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
        explicitFactoryUsed = true;
      }

      if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
        if (explicitFactoryUsed) {
          LOGGER.warn(
              () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
        }
        definition.getPropertyValues().add("sqlSessionTemplate",
            new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
        explicitFactoryUsed = true;
      } else if (this.sqlSessionTemplate != null) {
        if (explicitFactoryUsed) {
          LOGGER.warn(
              () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
        }
        definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
        explicitFactoryUsed = true;
      }

      if (!explicitFactoryUsed) {
        LOGGER.debug(() -> "Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
        // 设置注入模式为根据类型注入
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
      }
      definition.setLazyInit(lazyInitialization);
    }
  }
```

整个Mybatis的核心

```java
// ★★ 设置BeanDefinition实例化时的参数值;  
// ex: com.springframework.cn.dao.UserDao
definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59
// ★★ 设置beanDefinition的具体类型为MapperFactoryBean,具体的类而不是接口或者抽象类
definition.setBeanClass(this.mapperFactoryBeanClass);
```

**在这里将BeanDefinition中bean的实际类型 接口 → MapperFactoryBean类，即实例化完成之后，(userDao)接口的Bean类型不是UserDao而是MapperFactoryBean类型**

###### 3.2 实例化剩余的所有单例的Bean对象，并将其加入到单例缓存池singletonObjects集合中

- Dao接口在 实例化 → 填充属性 → 初始化 过程中与普通Bean基本是相同的
- 不同之处在创建动态代理的时机不同，AOP动态代理是在对象初始化完成之后的后置处理器applyBeanPostProcessorsAfterInitialization时产生代理对象；而Dao在此时是不会产生动态代理，产生动态代理是getObjectForBeanInstance时产生

###### 3.3  IOC中Bean的真实类型为MapperFactoryBean类型，该类型会产生一个代理对象

org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean

```java
@SuppressWarnings("unchecked")
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
                          @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {
    ...
    // Create bean instance.
	if (mbd.isSingleton()) {
        // 从单例缓存池中获取对象，若单例缓存池中不存在，则进行创建
        // userDao → MapperFactoryBean@xxx 实例
		sharedInstance = getSingleton(beanName, () -> {
			try {
				return createBean(beanName, mbd, args);
			}
			catch (BeansException ex) {
				// Explicitly remove instance from singleton cache: It might have been put there
				// eagerly by the creation process, to allow for circular reference resolution.
				// Also remove any beans that received a temporary reference to the bean.
				destroySingleton(beanName);
				throw ex;
			}
		});
        // ★★★ 生产一个MapperFactoryBean的动态代理对象，使用的是JDK动态代理
		bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
	}
    ...
```

获取BeanInstance的实例对象，若当前Bean没有实现FactoryBean接口，则返回当前Bean实例，若实现了该接口，则返回代理对象

org.springframework.beans.factory.support.AbstractBeanFactory#getObjectForBeanInstance

```java
protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

    // 判断当前Bean是否是工厂Bean，工厂Bean的name都是用&开头
    if (BeanFactoryUtils.isFactoryDereference(name)) {
        if (beanInstance instanceof NullBean) {
            return beanInstance;
        }
        if (!(beanInstance instanceof FactoryBean)) {
            throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
        }
        if (mbd != null) {
            mbd.isFactoryBean = true;
        }
        return beanInstance;
    }

    // 普通Bean对象
    if (!(beanInstance instanceof FactoryBean)) {
        return beanInstance;
    }

    // 实现了FactoryBean对象的Bean
    Object object = null;
    // 这里的mbd是MapperFactoryBean@xxx 
    if (mbd != null) {
        // 设置isFactoryBean属性
        mbd.isFactoryBean = true;
    }
    else {
        object = getCachedObjectForFactoryBean(beanName);
    }
    if (object == null) {
        // 将Bean转换FactoryBean类型
        FactoryBean<?> factory = (FactoryBean<?>) beanInstance;

        if (mbd == null && containsBeanDefinition(beanName)) {
            mbd = getMergedLocalBeanDefinition(beanName);
        }
        boolean synthetic = (mbd != null && mbd.isSynthetic());
        // ★★★ 使用FactoryBean<?>(MapperFactoryBean)工厂来产生一个Bean代理对象
        object = getObjectFromFactoryBean(factory, beanName, !synthetic);
    }
    return object;
}
```

★★★ 使用FactoryBean<?>产生一个MapperFactoryBean的代理对象

```java
protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
    if (factory.isSingleton() && containsSingleton(beanName)) {
        synchronized (getSingletonMutex()) {
            // 从factoryBeanObjectCache缓存中获取指定beanName的对象，
            // 该缓存主要用来保存FactoryBean创建的对象
            Object object = this.factoryBeanObjectCache.get(beanName);
            if (object == null) {
                // ★★★ 获取指定的beanName的代理对象
                object = doGetObjectFromFactoryBean(factory, beanName);
                // Only post-process and store if not put there already during getObject() call above
                // (e.g. because of circular reference processing triggered by custom getBean calls)
                Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
                if (alreadyThere != null) {
                    object = alreadyThere;
                }
                else {
                    if (shouldPostProcess) {
                        if (isSingletonCurrentlyInCreation(beanName)) {
                            // Temporarily return non-post-processed object, not storing it yet..
                            return object;
                        }
                        beforeSingletonCreation(beanName);
                        try {
                            object = postProcessObjectFromFactoryBean(object, beanName);
                        }
                        catch (Throwable ex) {
                            throw new BeanCreationException(beanName,
                                                            "Post-processing of FactoryBean's singleton object failed", ex);
                        }
                        finally {
                            afterSingletonCreation(beanName);
                        }
                    }
                    if (containsSingleton(beanName)) {
                        this.factoryBeanObjectCache.put(beanName, object);
                    }
                }
            }
            return object;
        }
    }
    else {
        Object object = doGetObjectFromFactoryBean(factory, beanName);
        if (shouldPostProcess) {
            try {
                object = postProcessObjectFromFactoryBean(object, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
            }
        }
        return object;
    }
}
```

org.springframework.beans.factory.support.FactoryBeanRegistrySupport#doGetObjectFromFactoryBean

```java
private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
      throws BeanCreationException {

   Object object;
   try {
      if (System.getSecurityManager() != null) {
         AccessControlContext acc = getAccessControlContext();
         try {
            object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
         }
         catch (PrivilegedActionException pae) {
            throw pae.getException();
         }
      }
      else {
         // 获取对象
         // ex: factory → MapperFactoryBean@xxx
         object = factory.getObject();
      }
   }
   catch (FactoryBeanNotInitializedException ex) {
      throw new BeanCurrentlyInCreationException(beanName, ex.toString());
   }
   catch (Throwable ex) {
      throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
   }

   // Do not accept a null value for a FactoryBean that's not fully
   // initialized yet: Many FactoryBeans just return null then.
   if (object == null) {
      if (isSingletonCurrentlyInCreation(beanName)) {
         throw new BeanCurrentlyInCreationException(
               beanName, "FactoryBean which is currently in creation returned null from getObject");
      }
      object = new NullBean();
   }
   // 返回代理对象
   return object;
}
```

调用MapperFactoryBean的getObject()获取代理对象

org.mybatis.spring.mapper.MapperFactoryBean#getObject

```java
/**
 * {@inheritDoc}
 */
@Override
public T getObject() throws Exception {
  return getSqlSession().getMapper(this.mapperInterface);
}
```

★★★★★ 最终会调用org.apache.ibatis.session.Configuration#getMapper

```java
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    return mapperRegistry.getMapper(type, sqlSession);
}
```

```java
@SuppressWarnings("unchecked")
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    // 获取knownMappers集合中获取代理工厂
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
        throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
        // 创建MapperProxyFactory代理实例
        return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
        throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
}
```

创建MapperProxy<T>作为拦截处理，MapperProxy类实现了InvocationHandler接口，并且重写invoke方法

MapperProxy类图

![image-20200923182536603](.\Image\Mybatis\MapperProxy.png)

```java
@SuppressWarnings("unchecked")
protected T newInstance(MapperProxy<T> mapperProxy) {
  //★★★★★ 底层采用JDK动态代理， proxy.newProxyInstance()产生一个代理对象
  return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
}

public T newInstance(SqlSession sqlSession) {
  // MapperProxy做动态拦截处理
  final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
  return newInstance(mapperProxy);
}
```

###### **4. SqlSessionFactoryBean**的实例时在加载MapperScannerConfigurer之后，Mapper接口映射之前

SqlSessionFactoryBean的初始化流程

![image-20200917113740520](.\Image\Mybatis\SqlSessionFactoryBean.png)

4.1 SqlSessionFactoryBean 实例化完成 → 填充属性 → 初始化操作(调用afterPropertiesSet()方法 → 自定义init())

- SqlSessionFactoryBean 实现了InitializingBean接口

SqlSessionFactoryBean初始化时回调afterPropertiesSet()方法，主要作用是用来构建sqlSessionFactory

org.mybatis.spring.SqlSessionFactoryBean#afterPropertiesSet

```java
/**
 * {@inheritDoc}
 */
@Override
public void afterPropertiesSet() throws Exception {
    // 验证dataSource属性不能为null
    notNull(dataSource, "Property 'dataSource' is required");
    // 验证sqlSessionFactoryBuilder属性不能为null
    notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");
    state((configuration == null && configLocation == null) || !(configuration != null && configLocation != null),
          "Property 'configuration' and 'configLocation' can not specified with together");

    // ★★ 构建sqlSessionFactory
    this.sqlSessionFactory = buildSqlSessionFactory();
}
```

org.mybatis.spring.SqlSessionFactoryBean#buildSqlSessionFactory

```java
protected SqlSessionFactory buildSqlSessionFactory() throws Exception {

    final Configuration targetConfiguration;

    // XML 文件解析器
    XMLConfigBuilder xmlConfigBuilder = null;
    // 判断configuration属性是否为null
    // 这里configuration、configLocation属性全部 未设置 为 null
    if (this.configuration != null) {
      targetConfiguration = this.configuration;
      if (targetConfiguration.getVariables() == null) {
        targetConfiguration.setVariables(this.configurationProperties);
      } else if (this.configurationProperties != null) {
        targetConfiguration.getVariables().putAll(this.configurationProperties);
      }
    } else if (this.configLocation != null) {
      xmlConfigBuilder = new XMLConfigBuilder(this.configLocation.getInputStream(), null, this.configurationProperties);
      targetConfiguration = xmlConfigBuilder.getConfiguration();
    } else {
      
      // 设置默认的Configuration
      LOGGER.debug(
          () -> "Property 'configuration' or 'configLocation' not specified, using default MyBatis Configuration");
      targetConfiguration = new Configuration();
      Optional.ofNullable(this.configurationProperties).ifPresent(targetConfiguration::setVariables);
    }

    Optional.ofNullable(this.objectFactory).ifPresent(targetConfiguration::setObjectFactory);
    Optional.ofNullable(this.objectWrapperFactory).ifPresent(targetConfiguration::setObjectWrapperFactory);
    Optional.ofNullable(this.vfs).ifPresent(targetConfiguration::setVfsImpl);

    if (hasLength(this.typeAliasesPackage)) {
      scanClasses(this.typeAliasesPackage, this.typeAliasesSuperType).stream()
          .filter(clazz -> !clazz.isAnonymousClass()).filter(clazz -> !clazz.isInterface())
          .filter(clazz -> !clazz.isMemberClass()).forEach(targetConfiguration.getTypeAliasRegistry()::registerAlias);
    }

    if (!isEmpty(this.typeAliases)) {
      Stream.of(this.typeAliases).forEach(typeAlias -> {
        targetConfiguration.getTypeAliasRegistry().registerAlias(typeAlias);
        LOGGER.debug(() -> "Registered type alias: '" + typeAlias + "'");
      });
    }

    if (!isEmpty(this.plugins)) {
      Stream.of(this.plugins).forEach(plugin -> {
        targetConfiguration.addInterceptor(plugin);
        LOGGER.debug(() -> "Registered plugin: '" + plugin + "'");
      });
    }

    if (hasLength(this.typeHandlersPackage)) {
      scanClasses(this.typeHandlersPackage, TypeHandler.class).stream().filter(clazz -> !clazz.isAnonymousClass())
          .filter(clazz -> !clazz.isInterface()).filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
          .forEach(targetConfiguration.getTypeHandlerRegistry()::register);
    }

    if (!isEmpty(this.typeHandlers)) {
      Stream.of(this.typeHandlers).forEach(typeHandler -> {
        targetConfiguration.getTypeHandlerRegistry().register(typeHandler);
        LOGGER.debug(() -> "Registered type handler: '" + typeHandler + "'");
      });
    }

    targetConfiguration.setDefaultEnumTypeHandler(defaultEnumTypeHandler);

    if (!isEmpty(this.scriptingLanguageDrivers)) {
      Stream.of(this.scriptingLanguageDrivers).forEach(languageDriver -> {
        targetConfiguration.getLanguageRegistry().register(languageDriver);
        LOGGER.debug(() -> "Registered scripting language driver: '" + languageDriver + "'");
      });
    }
    Optional.ofNullable(this.defaultScriptingLanguageDriver)
        .ifPresent(targetConfiguration::setDefaultScriptingLanguage);

    if (this.databaseIdProvider != null) {// fix #64 set databaseId before parse mapper xmls
      try {
        targetConfiguration.setDatabaseId(this.databaseIdProvider.getDatabaseId(this.dataSource));
      } catch (SQLException e) {
        throw new NestedIOException("Failed getting a databaseId", e);
      }
    }

    Optional.ofNullable(this.cache).ifPresent(targetConfiguration::addCache);

    if (xmlConfigBuilder != null) {
      try {
        xmlConfigBuilder.parse();
        LOGGER.debug(() -> "Parsed configuration file: '" + this.configLocation + "'");
      } catch (Exception ex) {
        throw new NestedIOException("Failed to parse config resource: " + this.configLocation, ex);
      } finally {
        ErrorContext.instance().reset();
      }
    }

    targetConfiguration.setEnvironment(new Environment(this.environment,
        this.transactionFactory == null ? new SpringManagedTransactionFactory() : this.transactionFactory,
        this.dataSource));

    // 设置 映射mapper文件路径,即设置
    // MapperLocation设置UserMapper.xml文件位置
    // sqlSessionFactoryBean.setMapperLocations(resources);
    if (this.mapperLocations != null) {
      if (this.mapperLocations.length == 0) {
        LOGGER.warn(() -> "Property 'mapperLocations' was specified but matching resources are not found.");
      } else {
        for (Resource mapperLocation : this.mapperLocations) {
          if (mapperLocation == null) {
            continue;
          }
          try {
            // 创建XMLMapperBuilder对象，用于解析mapper.xml文件
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
                targetConfiguration, mapperLocation.toString(), targetConfiguration.getSqlFragments());
            // <1> 解析xml文件内容
            xmlMapperBuilder.parse();
          } catch (Exception e) {
            throw new NestedIOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
          } finally {
            ErrorContext.instance().reset();
          }
          LOGGER.debug(() -> "Parsed mapper file: '" + mapperLocation + "'");
        }
      }
    } else {
      LOGGER.debug(() -> "Property 'mapperLocations' was not specified.");
    }

    return this.sqlSessionFactoryBuilder.build(targetConfiguration);
  }
```

 <1> org.apache.ibatis.builder.xml.XMLMapperBuilder#parse

```java
public void parse() {
    // 判断当前的 resource是否已经加载完成
    if (!configuration.isResourceLoaded(resource)) {
        // ★★ <2> 获取Mapper.xml文件中mapper节点信息，并将其内部节点转换一个对象
        // 这里只解析Mapper.xml文件中节点信息，不会解析Dao.java中方法注解上的SQL信息
        configurationElement(parser.evalNode("/mapper"));
        // 将resource加入到已经集合中，表示当前resource文件已经加载完成
        configuration.addLoadedResource(resource);
		// ★★ <3> 用于解析Mapper.xml 文件中mapper节点信息和Dao.java 中方法注解上的SQL信息
        // 绑定Mapper接口到namespace
        bindMapperForNamespace();
    }

    parsePendingResultMaps();
    parsePendingCacheRefs();
    parsePendingStatements();
}
```

<2> 将Mapper.xml文件中的mapper节点内容进行转换，构建Confiugration对象，该对象中包含xml文件中的一切对象信息

org.apache.ibatis.builder.xml.XMLMapperBuilder#configurationElement

```java

private void configurationElement(XNode context) {
    try {
        // 获取 mapper节点中 namespace属性值
        String namespace = context.getStringAttribute("namespace");
        // 验证namespace 属性
        if (namespace == null || namespace.isEmpty()) {
            throw new BuilderException("Mapper's namespace cannot be empty");
        }
        
        // 设置namespace属性，namespace不能为空 && 必须唯一
        builderAssistant.setCurrentNamespace(namespace);
        // 引用其它命名空间的缓存配置。
        cacheRefElement(context.evalNode("cache-ref"));
        // 该命名空间的缓存配置
        cacheElement(context.evalNode("cache"));
        // ★ 获取parameterMap节点信息，并将其加入到指定集合中
        // <parameterMap></parameterMap> 这是 旧风格的参数映射，这种已经被废弃
        parameterMapElement(context.evalNodes("/mapper/parameterMap"));
        
        // ★ 获取resultMap节点信息, 并将其加入到指定集合中
        // <2.1> resultMap 主要用作结果映射
        resultMapElements(context.evalNodes("/mapper/resultMap"));
        // 获取<sql></sql>标签内容，并将其添加到集合中
        sqlElement(context.evalNodes("/mapper/sql"));
        // ★ <2.2> 获取<select>、<update>、<insert>、<delete>节点的内容，将其添加到集合中
        buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
        throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
}
```

<2.1> resultMap结果映射

org.apache.ibatis.builder.xml.XMLMapperBuilder#resultMapElements

```java
// list 表示获取的所有的<resultMap>节点集合
private void resultMapElements(List<XNode> list) {
    for (XNode resultMapNode : list) {
        try {
            // 逐个节点进行解析
            resultMapElement(resultMapNode);
        } catch (IncompleteElementException e) {
            // ignore, it will be retried
        }
    }
}
```

```java
private ResultMap resultMapElement(XNode resultMapNode) {
  // 当前的resultMap节点内容、创建一个空的list对象
  return resultMapElement(resultMapNode, Collections.emptyList(), null);
}
```

UserDao.xml配置文件，主要用来分析resultMap的流程

```xml
<mapper namespace="com.springframework.cn.dao.UserDao">
    <resultMap id="userResultMap" type="com.springframework.cn.entity.User">
        <id property="id" column="id" />
        <result property="name" column="name"/>
        <result property="adress" column="adress"/>
    </resultMap>
</mapper>
```

解析<resultMap></resultMap> 节点内容，并将其内容转换为ResultMap.java 对象，加入到resultMaps集合中，key为 namespace + "." + id的形式

使用到了构造器[建造者]设计模式

```java
private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings, Class<?> enclosingType) {
  ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    
  // 获取resultMap节点的type属性
  // 先获取 type → 若type不存在，则获取ofType → 若ofType不存在，则获取resultType → 若resultType不存在，则获取javaType属性
  String type = resultMapNode.getStringAttribute("type",
      resultMapNode.getStringAttribute("ofType",
          resultMapNode.getStringAttribute("resultType",
              resultMapNode.getStringAttribute("javaType"))));
  // 获取type对应的Class类
  // 从typeAliases[别名注册表]中获取type对应的Class
  Class<?> typeClass = resolveClass(type);
  if (typeClass == null) {
    typeClass = inheritEnclosingType(resultMapNode, enclosingType);
  }
  
  Discriminator discriminator = null;
  List<ResultMapping> resultMappings = new ArrayList<>(additionalResultMappings);
  // 获取<resultMap></resultMap>节点中所有子节点
  List<XNode> resultChildren = resultMapNode.getChildren();
  // 遍历所有子节点
  for (XNode resultChild : resultChildren) {
    // 判断子节点的的类型是否是constructor
    if ("constructor".equals(resultChild.getName())) {
      processConstructorElement(resultChild, typeClass, resultMappings);
    } else if ("discriminator".equals(resultChild.getName())) {
      // 判断子节点的的类型是否是discriminator
      discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
    } else {
      //constructor && discriminator 之外的类型的节点
      List<ResultFlag> flags = new ArrayList<>();
      if ("id".equals(resultChild.getName())) {
        // <id></id> 
        flags.add(ResultFlag.ID);
      }
      // buildResultMappingFromContext(resultChild, typeClass, flags) → 构建一个ResultMapping对象
      // 将构建的ResultMapping对象加入到集合中
      // ★★★ ResultMapping使用的是构造器(建造者)涉及模式
      resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
    }
  }
  // 获取<resultMap></resultMap>节点的id
  String id = resultMapNode.getStringAttribute("id",
          resultMapNode.getValueBasedIdentifier());
  // 获取extends 属性
  String extend = resultMapNode.getStringAttribute("extends");
  // 获取 autoMapping属性
  Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
  // 构建一个resultMap 解析器
  ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
  try {
    // 返回解析结果
    return resultMapResolver.resolve();
  } catch (IncompleteElementException e) {
    configuration.addIncompleteResultMap(resultMapResolver);
    throw e;
  }
}
```

<2.2> 构建Statement对象即要执行的SQL语句: buildStatementFromContext

org.apache.ibatis.builder.xml.XMLMapperBuilder#buildStatementFromContext(java.util.List<org.apache.ibatis.parsing.XNode>)

```java
private void buildStatementFromContext(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
        buildStatementFromContext(list, configuration.getDatabaseId());
    }
    // 构建statement对象
    buildStatementFromContext(list, null);
}
```

```java
// list 表示的所有的 <insert>、<select>、<delete>、<update>节点信息
private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
        // 创建一个xmlstatement的构造器对象
        final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
        try {
            // 转换statementnode
            statementParser.parseStatementNode();
        } catch (IncompleteElementException e) {
            configuration.addIncompleteStatement(statementParser);
        }
    }
}
```

```java
public void parseStatementNode() {
    String id = context.getStringAttribute("id");
    String databaseId = context.getStringAttribute("databaseId");

    if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
      return;
    }
	
    // 获取node名称
    String nodeName = context.getNode().getNodeName();
    // 获取 标签的类型，即SQL语句的类型 insert、select、update、delete
    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    // 是否刷新缓存
    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
    // 是否使用缓存
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

    // Include Fragments before parsing
    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
    includeParser.applyIncludes(context.getNode());

    // 获取parameterType类型
    String parameterType = context.getStringAttribute("parameterType");
    // 获取parameterType对应的Class类型
    Class<?> parameterTypeClass = resolveClass(parameterType);

    String lang = context.getStringAttribute("lang");
    LanguageDriver langDriver = getLanguageDriver(lang);

    // Parse selectKey after includes and remove them.
    processSelectKeyNodes(id, parameterTypeClass, langDriver);

    // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
    // 转换SQL , 对SQL语句进行处理
    KeyGenerator keyGenerator;
    // 创建一个key 格式为 id + "!selectKey"
    String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
    // 判断configuration中是否已经存在该 key
    if (configuration.hasKeyGenerator(keyStatementId)) {
      keyGenerator = configuration.getKeyGenerator(keyStatementId);
    } else {
      // 创建key的生成器
      keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
          configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
          ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
    }
	
    /**
     * ★★★ 解析select|insert|update|delete节点中SQL语句内容,
     * 使用#{xxx}时， Mybatis会创建PreparedStatement 参数占位符， 即#{xxx} → ?;
     * ex: select * from user where id = #{id} → select * from user where id = ?;
     * 这样做得好处不存在 SQL注入
     * ${} 是直接插入一个字符串，该字符串是不会进行转义的，存在SQL注入的问题
     */
    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);

    // ☆ 设置statementType 类型，默认使用的是 [prepared]表示创建的是PreparedStatement，该statement会进行预编译
    StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    
    // 获取参数值
    Integer fetchSize = context.getIntAttribute("fetchSize");
    Integer timeout = context.getIntAttribute("timeout");
    String parameterMap = context.getStringAttribute("parameterMap");
    String resultType = context.getStringAttribute("resultType");
    Class<?> resultTypeClass = resolveClass(resultType);
    String resultMap = context.getStringAttribute("resultMap");
    String resultSetType = context.getStringAttribute("resultSetType");
    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
    if (resultSetTypeEnum == null) {
      resultSetTypeEnum = configuration.getDefaultResultSetType();
    }
    String keyProperty = context.getStringAttribute("keyProperty");
    String keyColumn = context.getStringAttribute("keyColumn");
    String resultSets = context.getStringAttribute("resultSets");

    // 构建MappedStatement对象，并将其加入到指定集合中
    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered,
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
  }
```

org.apache.ibatis.builder.MapperBuilderAssistant#addMappedStatement()

```java
public MappedStatement addMappedStatement(
    String id,
    SqlSource sqlSource,
    StatementType statementType,
    SqlCommandType sqlCommandType,
    Integer fetchSize,
    Integer timeout,
    String parameterMap,
    Class<?> parameterType,
    String resultMap,
    Class<?> resultType,
    ResultSetType resultSetType,
    boolean flushCache,
    boolean useCache,
    boolean resultOrdered,
    KeyGenerator keyGenerator,
    String keyProperty,
    String keyColumn,
    String databaseId,
    LanguageDriver lang,
    String resultSets) {

  if (unresolvedCacheRef) {
    throw new IncompleteElementException("Cache-ref not yet resolved");
  }

  // Map集合中的key: 格式为 namespace + id
  id = applyCurrentNamespace(id, false);
  // 判断是否是select语句
  boolean isSelect = sqlCommandType == SqlCommandType.SELECT;

  MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
      .resource(resource)
      .fetchSize(fetchSize)
      .timeout(timeout)
      .statementType(statementType)
      .keyGenerator(keyGenerator)
      .keyProperty(keyProperty)
      .keyColumn(keyColumn)
      .databaseId(databaseId)
      .lang(lang)
      .resultOrdered(resultOrdered)
      .resultSets(resultSets)
      .resultMaps(getStatementResultMaps(resultMap, resultType, id))
      .resultSetType(resultSetType)
      .flushCacheRequired(valueOrDefault(flushCache, !isSelect))
      .useCache(valueOrDefault(useCache, isSelect))
      .cache(currentCache);

  // parameterMap 已经被废弃
  ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
  if (statementParameterMap != null) {
    statementBuilder.parameterMap(statementParameterMap);
  }

  // 构造MapperStatement对象
  MappedStatement statement = statementBuilder.build();
  // 将该对象加入到mappedStatements集合中
  configuration.addMappedStatement(statement);
  return statement;
}
```

★★★ Configuration类

Configuration类 是一个核心类，主要用来存储Mapper.xml文件的解析结果

```java
public class Configuration {

  protected Environment environment;

  protected boolean safeRowBoundsEnabled;
  protected boolean safeResultHandlerEnabled = true;
  protected boolean mapUnderscoreToCamelCase;
  protected boolean aggressiveLazyLoading;
  protected boolean multipleResultSetsEnabled = true;
  protected boolean useGeneratedKeys;
  protected boolean useColumnLabel = true;
  protected boolean cacheEnabled = true;
  protected boolean callSettersOnNulls;
  protected boolean useActualParamName = true;
  protected boolean returnInstanceForEmptyRow;
  protected boolean shrinkWhitespacesInSql;

  protected String logPrefix;
  protected Class<? extends Log> logImpl;
  protected Class<? extends VFS> vfsImpl;
  protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
  protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
  protected Set<String> lazyLoadTriggerMethods = new HashSet<>(Arrays.asList("equals", "clone", "hashCode", "toString"));
  protected Integer defaultStatementTimeout;
  protected Integer defaultFetchSize;
  protected ResultSetType defaultResultSetType;
  protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
  protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
  protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;

  protected Properties variables = new Properties();
  protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
  protected ObjectFactory objectFactory = new DefaultObjectFactory();
  protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();

  protected boolean lazyLoadingEnabled = false;
  protected ProxyFactory proxyFactory = new JavassistProxyFactory(); // #224 Using internal Javassist instead of OGNL

  protected String databaseId;
  /**
   * Configuration factory class.
   * Used to create Configuration for loading deserialized unread properties.
   *
   * @see <a href='https://code.google.com/p/mybatis/issues/detail?id=300'>Issue 300 (google code)</a>
   */
  protected Class<?> configurationFactory;

  protected final MapperRegistry mapperRegistry = new MapperRegistry(this);
  protected final InterceptorChain interceptorChain = new InterceptorChain();
  protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry(this);
  protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
  protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

  protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection")
      .conflictMessageProducer((savedValue, targetValue) ->
          ". please check " + savedValue.getResource() + " and " + targetValue.getResource());
  protected final Map<String, Cache> caches = new StrictMap<>("Caches collection");
  protected final Map<String, ResultMap> resultMaps = new StrictMap<>("Result Maps collection");
  protected final Map<String, ParameterMap> parameterMaps = new StrictMap<>("Parameter Maps collection");
  protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<>("Key Generators collection");

  protected final Set<String> loadedResources = new HashSet<>();
  protected final Map<String, XNode> sqlFragments = new StrictMap<>("XML fragments parsed from previous mappers");

  protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<>();
  protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<>();
  protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<>();
  protected final Collection<MethodResolver> incompleteMethods = new LinkedList<>();

  /*
   * A map holds cache-ref relationship. The key is the namespace that
   * references a cache bound to another namespace and the value is the
   * namespace which the actual cache is bound to.
   */
  protected final Map<String, String> cacheRefMap = new HashMap<>();
}
```

<3> bindMapperForNamespace() 绑定Mapper接口

- 用于解析Mapper.xml 文件中mapper节点信息

- 解析Dao.java 中方法上注解的SQL信息

```java
private void bindMapperForNamespace() {
    // 获取namespace
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
        Class<?> boundType = null;
        try {
            // 通过namespace获取对应的类
            boundType = Resources.classForName(namespace);
        } catch (ClassNotFoundException e) {
            //ignore, bound type is not required
        }
        if (boundType != null) {
            // 判断knownMappers集合中是否已经包含boundType，如果包含表示已经解析过了
            // 并且已经创建了 boundType（Class）对应的 MapperProxyFactory
            if (!configuration.hasMapper(boundType)) {
                // Spring may not know the real resource name so we set a flag
                // to prevent loading again this resource from the mapper interface
                // look at MapperAnnotationBuilder#loadXmlResource
                // 将namespace: + namespace 加入到loadResource集合中，表示这个资源已经加载过
                configuration.addLoadedResource("namespace:" + namespace);
                
                // 解析Mapper.xml文件内容和Dao.java中接口上的注解信息，添加映射
                configuration.addMapper(boundType);
            }
        }
    }
}
```

org.apache.ibatis.session.Configuration#addMapper

```java
public <T> void addMapper(Class<T> type) {
    mapperRegistry.addMapper(type);
}
```

org.apache.ibatis.binding.MapperRegistry#addMapper

```java
  public <T> void addMapper(Class<T> type) {
    // 判断当前type 是否是 接口
    if (type.isInterface()) {
      // 如果已经存在映射，即knownMappers集合中是否包含指定的key
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        // 将key,value → type,MapperProxyFactoty 加入到集合中，MapperProxyFactory<T> 主要用于生成一个MapperFactoryBean的代理对象
        knownMappers.put(type, new MapperProxyFactory<T>(type));
        // It's important that the type is added before the parser is run
        // otherwise the binding may automatically be attempted by the
        // mapper parser. If the type is already known, it won't try.
          
        // 创建一个MapperAnnotationBuilder
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        // ★★★ 就行解析操作
        parser.parse();
        loadCompleted = true;
      } finally {
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }
```

org.apache.ibatis.builder.annotation.MapperAnnotationBuilder#parse

```java
public void parse() {
  String resource = type.toString();
  // 判断resource是否已经加载
  // ex: resource → com.springframework.cn.Dao.userDao
  if (!configuration.isResourceLoaded(resource)) {
    // 加载Mapper.xml文件信息，前面已经解析过xml文件信息，故这里不再进行解析
    loadXmlResource();
    // 设置当前Resource已经加载过
    configuration.addLoadedResource(resource);
    assistant.setCurrentNamespace(type.getName());
    parseCache();
    parseCacheRef();
    // 获取当前的Mapper接口的方法
    Method[] methods = type.getMethods();
    // 遍历方法
    for (Method method : methods) {
      try {
        // issue #237
        if (!method.isBridge()) {
          // 获取方法上的Statement，并将其加入到指定集合中
          parseStatement(method);
        }
      } catch (IncompleteElementException e) {
        configuration.addIncompleteMethod(new MethodResolver(this, method));
      }
    }
  }
  parsePendingMethods();
}
```

org.apache.ibatis.builder.annotation.MapperAnnotationBuilder#parseStatement

解析方法注解上的SQL信息

```java
void parseStatement(Method method) {
  // 获取方法上面的参数类型
  Class<?> parameterTypeClass = getParameterType(method);
  LanguageDriver languageDriver = getLanguageDriver(method);
  // 获取方法注解上面的SQL信息,这个是已经使用占位符替换之后的
  // ex: 结果 select * from user where id = ?;
  SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);
  if (sqlSource != null) {
    Options options = method.getAnnotation(Options.class);
    // statementid: 全类名 + . + 方法名
    final String mappedStatementId = type.getName() + "." + method.getName();
    // 剩下的为设置参数
    Integer fetchSize = null;
    Integer timeout = null;
    StatementType statementType = StatementType.PREPARED;
    ResultSetType resultSetType = ResultSetType.FORWARD_ONLY;
    SqlCommandType sqlCommandType = getSqlCommandType(method);
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    boolean flushCache = !isSelect;
    boolean useCache = isSelect;

    KeyGenerator keyGenerator;
    String keyProperty = "id";
    String keyColumn = null;
    if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
      // first check for SelectKey annotation - that overrides everything else
      SelectKey selectKey = method.getAnnotation(SelectKey.class);
      if (selectKey != null) {
        keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method), languageDriver);
        keyProperty = selectKey.keyProperty();
      } else if (options == null) {
        keyGenerator = configuration.isUseGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
      } else {
        keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
        keyProperty = options.keyProperty();
        keyColumn = options.keyColumn();
      }
    } else {
      keyGenerator = NoKeyGenerator.INSTANCE;
    }

    if (options != null) {
      if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
        flushCache = true;
      } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
        flushCache = false;
      }
      useCache = options.useCache();
      fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null; //issue #348
      timeout = options.timeout() > -1 ? options.timeout() : null;
      statementType = options.statementType();
      resultSetType = options.resultSetType();
    }

    String resultMapId = null;
    ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
    if (resultMapAnnotation != null) {
      String[] resultMaps = resultMapAnnotation.value();
      StringBuilder sb = new StringBuilder();
      for (String resultMap : resultMaps) {
        if (sb.length() > 0) {
          sb.append(",");
        }
        sb.append(resultMap);
      }
      resultMapId = sb.toString();
    } else if (isSelect) {
      resultMapId = parseResultMap(method);
    }
    
    // ★★ 构造MappedStatement，并将其加入到mappedStatements集合中
    // 到此所有的Mapper节点信息全部解析完成
    assistant.addMappedStatement(
        mappedStatementId,
        sqlSource,
        statementType,
        sqlCommandType,
        fetchSize,
        timeout,
        // ParameterMapID
        null,
        parameterTypeClass,
        resultMapId,
        getReturnType(method),
        resultSetType,
        flushCache,
        useCache,
        // TODO gcode issue #577
        false,
        keyGenerator,
        keyProperty,
        keyColumn,
        // DatabaseID
        null,
        languageDriver,
        // ResultSets
        options != null ? nullOrEmpty(options.resultSets()) : null);
  }
}
```