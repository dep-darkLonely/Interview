# Spring的配置方式及Tomcat加载spring的流程

**Spring的配置方式有2中，分别是xml配置文件和JavaConfig(注解)形式.**

> [!Note|label:XML配置Spring的方式]

1. web.xml 配置启动时加载Spring

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">

    <!-- 配置监听器 在WebApplication应用程序启动时调用-->
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <context-param>
        <!-- Spring 配置文件信息 -->
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/app-context.xml</param-value>
    </context-param>

    <servlet>
        <!-- Spring MVC 配置文件信息 -->
        <servlet-name>application</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/mvc-context.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>application</servlet-name>
        <url-pattern>/*</url-pattern>
    </servlet-mapping>

</web-app>
```

> [!Warning|label:疑问？]
>
> 1. 为什么需要配置ContextLoaderListener?
> 2. 为什么需要实现ServletContextListener接口？
>
>    解释: ContextLoaderListener实现ServletContextListener接口，并且重写contextInitialized();
>
>    实现ServletContextListener接口的主要作用就是web应用程序初始化之前调用ServletContextListener接口的contexInitialized方法,引入Spring框架，载入IOC容器
>
>
> Servlet3.1规范中内容说明
>
> The following methods are added to ServletContext since Servlet 3.0 to enable
programmatic definition of servlets, filters and the url pattern that they map to.
These methods can only be called during the initialization of the application either
from the contexInitialized method of a ServletContextListener
implementation or from the onStartup method of a
ServletContainerInitializer implementation.

2. org.springframework.web.context.ContextLoaderListener源码分析

> - ContextLoaderListener.java 实现了ServletContextListener接口，重写了contextInitialized()方法

```java
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

    public ContextLoaderListener() {
    }

    public ContextLoaderListener(WebApplicationContext context) {
        super(context);
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // 初始化 Root webApplication context
        initWebApplicationContext(event.getServletContext());
    }


    @Override
    public void contextDestroyed(ServletContextEvent event) {
        closeWebApplicationContext(event.getServletContext());
        ContextCleanupListener.cleanupAttributes(event.getServletContext());
    }

}
```

3. initWebApplicationContext 初始化Root WebApplication Context上下文

```java
public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
    if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
        throw new IllegalStateException(
                "Cannot initialize context because there is already a root application context present - " +
                "check whether you have multiple ContextLoader* definitions in your web.xml!");
    }

    servletContext.log("Initializing Spring root WebApplicationContext");
    Log logger = LogFactory.getLog(ContextLoader.class);
    if (logger.isInfoEnabled()) {
        logger.info("Root WebApplicationContext: initialization started");
    }
    long startTime = System.currentTimeMillis();

    try {
        
        if (this.context == null) {
            // 创建Root WebApplicationContext
            this.context = createWebApplicationContext(servletContext);
        }
        // WebApplicationContext 接口的一个子接口
        if (this.context instanceof ConfigurableWebApplicationContext) {
            ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
            // 判断当前容器是否已经激活，即就是context上下文还没有进行refresh()
            if (!cwac.isActive()) {
                
                // 父容器 Servlet WebApplicationContext容器
                if (cwac.getParent() == null) {
                    
                    ApplicationContext parent = loadParentContext(servletContext);
                    // return null;设置父容器为null
                    cwac.setParent(parent);
                }
                // 配置并刷新WebApplicationContext容器
                configureAndRefreshWebApplicationContext(cwac, servletContext);
            }
        }

        // spring的xml配置中存在父子容器， 设置父容器；javaConfig形式中不存在
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (ccl == ContextLoader.class.getClassLoader()) {
            currentContext = this.context;
        }
        else if (ccl != null) {
            currentContextPerThread.put(ccl, this.context);
        }

        if (logger.isInfoEnabled()) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("Root WebApplicationContext initialized in " + elapsedTime + " ms");
        }

        return this.context;
    }
    catch (RuntimeException | Error ex) {
        logger.error("Context initialization failed", ex);
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
        throw ex;
    }
}
```

4. configureAndRefreshWebApplicationContext 配置并刷新 web Application 上下文

```java
/**
 * 配置 & refresh() webApplilcation 上下文
 */
protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {

    // 设置Root web Application Context Id
    if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
        // The application context id is still set to its original default value
        // -> assign a more useful id based on available information
        String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
        if (idParam != null) {
            wac.setId(idParam);
        }
        else {
            // Generate default id...
            wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
                    ObjectUtils.getDisplayString(sc.getContextPath()));
        }
    }

    // 设置当前 Root WebApplication的servlet 上下文;
    /**
     * ServletContext 是一个全局的储存信息空间
     * 服务器开始，就存在;
     * 服务器关闭是，其才释放;
     * request 一个用户可有多个；session 一个用户只有一个；
     * 而ServletContext 所有用户只有一个；
     * 每一个web应用程序都有一个与之相关的Servlet上下文
     */
    wac.setServletContext(sc);
    // 获取web.xml中init-param参数：contextConfigLocation
    String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);

    if (configLocationParam != null) {
        // 设置 Spring 配置文件的位置
        wac.setConfigLocation(configLocationParam);
    }

    // The wac environment's #initPropertySources will be called in any case when the context
    // is refreshed; do it eagerly here to ensure servlet property sources are in place for
    // use in any post-processing or initialization that occurs below prior to #refresh

    // 获取系统环境变量信息
    ConfigurableEnvironment env = wac.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment) {
        // 设置属性值
        ((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
    }

    // 自定义context
    customizeContext(sc, wac);

    // 刷新webApplicationContext上下文
    wac.refresh();
}
```

5. refresh() 刷新Spring应用的上下文

> - refresh() 方法调用流程和 JavaConfig 版本Spring refresh()方法调用流程相同

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 容器刷新前的准备，设置上下文状态、获取属性、初始化属性(property source)配置
        prepareRefresh();

        // 通过 CAS 操作，刷新BeanFactory并设置BeanFactory的序列化ID
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // 配置标准的beanFactory，设置ClassLoader，设置SpEL表达式解析器，添加忽略注入的接口，添加bean，添加bean后置处理器等
        prepareBeanFactory(beanFactory);

        try {
            // 获取容器BeanFactory，可以在真正初始化bean之前对bean做一些处理操作。
            // 允许我们在工厂里所有的bean被加载进来后但是还没初始化前，对所有bean的属性进行修改也可以add属性值。
            postProcessBeanFactory(beanFactory);

            // 实例化并调用所有注册的beanFactory后置处理器
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

            // refresh做完之后需要做的其他事情。（Spring Cloud也是从这里启动）
            finishRefresh();
        }
        catch (BeansException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Exception encountered during context initialization - " +
                        "cancelling refresh attempt: " + ex);
            }

            destroyBeans();
            cancelRefresh(ex);
            throw ex;
        }
        finally {
            resetCommonCaches();
        }
    }
}
```


>[!Warning|label:SPI机制]
>
> - SPI全称为Service Provider Interface即服务提供接口，这是一种服务发现机制，       
> 通过在classpath路径下的META-INF/services文件夹查找文件，自动加载文件里所定义的类;       
> 文件名命名是接口的全限定类名(如：org.springframework.cn.userService)，文件内容是实现类的全限定类名
> - Dubbo、JDBC中都使用到了SPI机制


> [!Note|label:JavaConfig配置Spring的方式] 
>
> - JavaConfig版本，Web应用程序启动Spring的方式与XML版本略有不同
> - JavaConfig版本，启动Spring采用的是SPI机制(SPI:Service Provider Interface)

1. spring-web中采用了SPI机制，SpringServletContainerInitializer.class实现了javax.servlet.ServletContainerInitializer接口

> 为什么需要实现了该接口？
>
> SpringServletContainerInitializer接口中重要的方法和注解?

```markdown
1. The ServletContainerInitializer class is looked up via the jar services API.
For each application, an instance of the ServletContainerInitializer is
created by the container at application startup time. The framework providing an
implementation of the ServletContainerInitializer MUST bundle in the
META-INF/services directory of the jar file a file called
javax.servlet.ServletContainerInitializer, as per the jar services API,
that points to the implementation class of the ServletContainerInitializer

2. In addition to the ServletContainerInitializer we also have an annotation -
HandlesTypes. The HandlesTypes annotation on the implementation of the
ServletContainerInitializer is used to express interest in classes that may
have annotations (type, method or field level annotations) specified in the value of
the HandlesTypes or if it extends / implements one those classes anywhere in the
class’ super types. The HandlesTypes annotation is applied irrespective of the
setting of metadata-complete.

3. The onStartup method of the ServletContainerInitializer will be invoked
when the application is coming up before any of the servlet listener events are fired.

4. The onStartup method of the ServletContainerInitializer is called with a
Set of Classes that either extend / implement the classes that the initializer
expressed interest in or if it is annotated with any of the classes specified via the
@HandlesTypes annotation
```
[Servelt3.1规范PDF](https://download.oracle.com/otn-pub/jcp/servlet-3_1-fr-eval-spec/servlet-3_1-final.pdf)

![SPI](/Image/Spring/SPI.jpg)

```java
// 设置@HandlesTypes 感兴趣的类，可以获取WebApplicationInitializer的所有子类

@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {

    // web 容器启动时，调用onStartup
	@Override
	public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {

		List<WebApplicationInitializer> initializers = new LinkedList<>();

		if (webAppInitializerClasses != null) {
			for (Class<?> waiClass : webAppInitializerClasses) {

                // 判断该类不是接口 && 不是抽象类 && 是WebApplicationInitializer的子类
				if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
						WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
					try {

                        // 将该类加入到集合中
						initializers.add((WebApplicationInitializer)
								ReflectionUtils.accessibleConstructor(waiClass).newInstance());
					}
					catch (Throwable ex) {
						throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
					}
				}
			}
		}

		if (initializers.isEmpty()) {
			servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
			return;
		}

		servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");

        // 排序
		AnnotationAwareOrderComparator.sort(initializers);

		for (WebApplicationInitializer initializer : initializers) {
            // 调用子类的onstartup方法
			initializer.onStartup(servletContext);
		}
	}
}
```

2. web.xml 配置WebApplicationInitializer.class的实现类

```xml
<?xml version="1.0" encoding="UTF-8"?>

<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xmlns="http://java.sun.com/xml/ns/javaee"
		 xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
                        http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
         version="3.0">
	<display-name>Archetype Created Web Application</display-name>
	
	<listener>
		<listener-class>org.springframework.cn.Config.MyWebApplicationInitializer</listener-class>
	</listener>

</web-app>
```

3. MyWebApplicationInitializer.class的onStartup中启动Spring容器

```java
public class MyWebApplicationInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletCxt) {

		// Load Spring web application configuration
		AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();

        // 加载Spring配置
		ac.register(AppConfig.class);
        // 设置servlet context上下文
		ac.setServletContext(servletCxt);
        // 刷新Spring容器
		ac.refresh();

		// 创建并配置DispathcherServlet
		DispatcherServlet servlet = new DispatcherServlet(ac);
		ServletRegistration.Dynamic registration = servletCxt.addServlet("dispatcher", servlet);
		registration.setLoadOnStartup(1);
		registration.addMapping("/");
	}
}
```

4. refresh() 与XML版本调用的 refresh()方法相同，参考下一章Spring源码分析
   
```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 容器刷新前的准备，设置上下文状态、获取属性、初始化属性(property source)配置
        prepareRefresh();

        // 通过 CAS 操作，刷新BeanFactory并设置BeanFactory的序列化ID
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // 配置标准的beanFactory，设置ClassLoader，设置SpEL表达式解析器，添加忽略注入的接口，添加bean，添加bean后置处理器等
        prepareBeanFactory(beanFactory);

        try {
            // 获取容器BeanFactory，可以在真正初始化bean之前对bean做一些处理操作。
            // 允许我们在工厂里所有的bean被加载进来后但是还没初始化前，对所有bean的属性进行修改也可以add属性值。
            postProcessBeanFactory(beanFactory);

            // 实例化并调用所有注册的beanFactory后置处理器
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

            // refresh做完之后需要做的其他事情。（Spring Cloud也是从这里启动）
            finishRefresh();
        }

        catch (BeansException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Exception encountered during context initialization - " +
                        "cancelling refresh attempt: " + ex);
            }

            destroyBeans();
            cancelRefresh(ex);

            throw ex;
        }

        finally {
            resetCommonCaches();
        }
    }
}
```

