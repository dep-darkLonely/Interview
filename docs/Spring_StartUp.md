# Spring 启动流程解析

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
>

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
        // Store context in local instance variable, to guarantee that
        // it is available on ServletContext shutdown.
        if (this.context == null) {
            // 创建Root WebApplicationContext
            this.context = createWebApplicationContext(servletContext);
        }
        // WebApplicationContext 接口的一个子接口
        if (this.context instanceof ConfigurableWebApplicationContext) {
            ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
            // 判断当前容器是否已经激活，即就是context上下文还没有进行refresh()
            if (!cwac.isActive()) {
                // The context has not yet been refreshed -> provide services such as
                // setting the parent context, setting the application context id, etc
                // 父容器 Servlet WebApplicationContext容器
                if (cwac.getParent() == null) {
                    // The context instance was injected without an explicit parent ->
                    // determine parent for root web application context, if any.
                    ApplicationContext parent = loadParentContext(servletContext);
                    // return null;设置父容器为null
                    cwac.setParent(parent);
                }
                // 配置并刷新WebApplicationContext容器
                configureAndRefreshWebApplicationContext(cwac, servletContext);
            }
        }
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

> [!Note|label:JavaConfig配置Spring的方式] 
