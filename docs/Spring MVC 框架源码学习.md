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

###### <2> 配置web.xml文件，作用tomcat启动时调用SpringWebApplication的onStartUp方法;

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <listener>
        <listener-class>com.springframework.cn.SpringWebApplication</listener-class>
    </listener>
</web-app>
```

###### <3> 使用@EnableWebMvc注解，启用Spring MVC

```java
@EnableWebMvc
class MvcConfig implements WebMvcConfigurer {
}
```

### 2. Spring MVC框架源码调用流程分析

###### <1>  AnnotationConfigWebApplicationContext.java 流程分析；

AnnotationConfigWebApplicationContext类图

![image-20200908113042037](C:\Users\hanbin\AppData\Roaming\Typora\typora-user-images\image-20200908113042037.png)

[具体流程调用，查看Spring 框架源码分析](http://www.baidu.com)

```java
// 加载Spring IOC容器，将所有Bean实例对象交由Spring管理
AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new 	        AnnotationConfigWebApplicationContext();
// 加载配置类
annotationConfigWebApplicationContext.register(AppConfig.class);
// 设置ServletContext上下文
annotationConfigWebApplicationContext.setServletContext(servletContext);
// 刷新Application Context上下文
annotationConfigWebApplicationContext.refresh();
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

![image-20200908110305317](C:\Users\hanbin\AppData\Roaming\Typora\typora-user-images\image-20200908110305317.png)



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

