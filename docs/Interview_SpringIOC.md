# SpingIOC 源码学习

[!WARNING|创建User实体类]
```java
/**
 * User 登录用户信息
 */
@Component
public class User {

	private int id;
	private String name;
	private int age;
	private String address;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
```

```java
public interface UserService {

	/**
	 * 通过Id获取登录用户信息
	 * @param id 登录用户Id
	 * @return User
	 */
	User login(int id);
}

@Service
public class UserServiceImpl implements UserService {

	@Override
	public User login(int id) {
		User user = new User();
		user.setId(id);
		user.setAge(2);
		user.setName("张三");
		user.setAddress("陕西省西安市雁塔区");
		return user;
	}
}
```

```java
@Repository
public interface UsedDao {
}
```

```java
@RestController
public class UserController {

	@Autowired
	UserService userService;

	@RequestMapping(value = "/login/{id}")
	public User login(@PathVariable int id) {
		return userService.login(id);
	}
}
```

```java
@Configuration
@ComponentScan("org.springframework.cn.*")
public class AppConfig {
}
```

[!NORMAL|创建Junit]
```java
public class IOCTest {

	@Test
	public void testBean() {
		// Spring 上下文容器
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		UserService userService = annotationConfigApplicationContext.getBean(UserService.class);
		User user = userService.login(1);
		System.out.println(user.toString());
	}
}
```

```java
org.springframework.context.annotation.AnnotationConfigApplicationContext#AnnotationConfigApplicationContext(java.lang.Class<?>...)
	/**
	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions
	 * from the given component classes and automatically refreshing the context.
	 * @param componentClasses one or more component classes &mdash; for example,
	 * {@link Configuration @Configuration} classes
	 */
    // AnnotationConfigApplication 构造函数
	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		// 调用构造函数
		this();
		// 注册我们的配置类
		register(componentClasses);
		// IOC容器刷新接口
		refresh();
	}
```

[!NOTE|Spring核心方法refresh()]
```java
@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
			// 容器刷新前的准备，设置上下文状态、获取属性、初始化属性(property source)配置
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			// 获取新的beanFactory，销毁原有beanFactory、为每个bean生成BeanDefinition等
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			// 配置标准的beanFactory，设置ClassLoader，设置SpEL表达式解析器，添加忽略注入的接口，添加bean，添加bean后置处理器等
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				// 模板方法，此时，所有的beanDefinition已经加载，但是还没有实例化。
				// 允许在子类中对beanFactory进行扩展处理。比如添加ware相关接口自动装配设置，添加后置处理器等，是子类扩展prepareBeanFactory(beanFactory)的方法
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				// 实例化并调用所有注册的beanFactory后置处理器
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				// 实例化和注册beanFactory中扩展了BeanPostProcessor的bean
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				// 初始化国际化工具类MessageSource
				initMessageSource();

				// Initialize event multicaster for this context.
				// 初始化事件广播器
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				// 模板方法，在容器刷新的时候可以自定义逻辑，不同的Spring容器做不同的事情
				// SpringBoot是从这个方法进行启动Tomcat的
				onRefresh();

				// 注册监听器，广播early application events
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				// 实例化所有剩余的（非懒加载）单例
				finishBeanFactoryInitialization(beanFactory);

				// refresh做完之后需要做的其他事情。
				//清除上下文资源缓存
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


[!NOTE|调用BeanFactory的后置处理器invokeBeanFactoryPostProcessors]
```java
org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors

/**
 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
 * respecting explicit order if given.
 * <p>Must be called before singleton instantiation.
 */
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

    // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
    // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
    if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
        beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
        beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }
}

org.springframework.context.support.PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors

	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		//判断beanFactory是否实现了BeanDefinitionRegistry
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 保存BeanFactoryPostProcessor类型的后置处理器
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 保存BeanDefinitionRegistryPostProcessor类型的后置处理器
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				// 判断后置处理器是不是BeanDefinitionRegistryPostProcessor
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 执行后置处理器方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 添加到用于保存的BeanDefinitionRegistryPostProcessor的集合中
					registryProcessors.add(registryProcessor);
				}
				else {
					// 如果没有实现BeanDefinitionRegistryPostProcessor接口，则添加到BeanFactoryPostProcessor类型后置处理器集合中
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.

			// 对BeanDefinitionRegistryPostProcessors进行一个分类,分别为：
			// 1. 实现PriorityOrdered
			// 2. 实现Ordered
			// 3. 剩余的Bean(既没有实现PriorityOrdered，也没有实现Ordered)
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// 获取BeanDefinitionRegistryPostProcessor类型的BeanName
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 获取BeanDefinitionRegistryPostProcessor类型的Bean，并将其添加到List中
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 将beanName添加至指定集合中，该集合中存储的BeanName表示已经处理过的Bean
					processedBeans.add(ppName);
				}
			}

			// 对currentRegistryProcessors中的Bean进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 将 实现PriorityOrdered该接口的Bean实例添加到 registryProcessors集合
			registryProcessors.addAll(currentRegistryProcessors);

			// 调用实现PriorityOrdered接口的BeanDefinitionRegistry的后置处理器
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 移除 currentRegistryProcessors中的所有数据
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 获取 实现Ordered接口且类型为BeanDefinitionRegistryPostProcessor的Bean
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					// 获取BeanDefinitionRegistryPostProcessor类型的Bean，并将其添加到List中
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 将beanName添加至指定集合中，该集合中存储的BeanName表示已经处理过的Bean
					processedBeans.add(ppName);
				}
			}

			// 对currentRegistryProcessors中的Bean进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);

			// 将 实现PriorityOrdered该接口的Bean实例添加到 registryProcessors集合
			registryProcessors.addAll(currentRegistryProcessors);

			// 调用实现Ordered接口的BeanDefinitionRegistry的后置处理器
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			// 移除 currentRegistryProcessors中的所有数据
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				// 获取指定类型(BeanDefinitionRegistryPostProcessor)的Bean
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					// 判断当前Bean是否在processedBeans的集合中
					if (!processedBeans.contains(ppName)) {
						// 获取没有实现PriorityOrdered接口和Ordered接口的Bean
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						// 将beanName添加至指定集合中，该集合中存储的BeanName表示已经处理过的Bean
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				// 对currentRegistryProcessors中的Bean进行排序
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				// 将Bean实例添加到registryProcessors集合
				registryProcessors.addAll(currentRegistryProcessors);
				// 调用BeanDefinitionRegistry的后置处理器
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				// 移除 currentRegistryProcessors中的所有数据
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.

			// 调用后置处理器
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			// 调用在上下文中已经注册的BeanFactory的后置处理器
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!

		// 返回所有类型为BeanFactoryPostProcessor的BeanName
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.

		// BeanNames 分类
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 通过BeanName来获取指定名称的Bean,将其添加至指定集合中
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// PriorityOrdered 处理

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		// priorityOrderedPostProcessors排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 调用后置处理器
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// 实现Ordered接口的Bean的处理
		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 没有实现PriorityOrdered 和 Ordered接口的Bean的处理操作
		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		// 清除缓存
		beanFactory.clearMetadataCache();
	}
```