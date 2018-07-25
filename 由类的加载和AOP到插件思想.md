---
title: 由类的动态加载和AOP到插件化思想
date: 2018-05-12 16:49:48
tags:
	- Java
categories:
	- idea
---

>  良句积累: 与善人居，如入芝兰之室，久而自芳矣。

### 写在前面
&emsp;&emsp;web应用的升级和改造一般都需要重启应用，作为线上的应用，重新部署不免会影响用户的使用，当然现在运维技术高速发展，已经可以实现灰度等平滑过渡方式，但总的来说还是通过重新部署的方式。以前接触jenkins的时候，觉得这个东西真的好用啊，持续集成，节约了我们大部分时间，后面发现jenkins优秀的不止那么一点点，它的另一个功能引起了我的注意，插件管理，通过插件功能，可以实现在线升级和安装新应用。所以我就想能不能实现一个类似的功能，通过基于远程web端的插件下载和更新，在不重启应用的情况下装载运行，并且提供相应的插件控制台，实现插件的输出展示等？当然可以，下面我们就来一起实现。

### 项目设计
&emsp;&emsp;废话不多说，根据设想绘制项目体系结构图：
![image](C:\Users\yangpe\Pictures\picture\p1.png)

&emsp;&emsp;考虑到web插件的适用性，设计为远程插件仓库，通过类似于maven的方式实现下载安装，并且项目集成的插件可以随时卸载、暂停与启用，所以，相应的UML类图可绘制：
![image](C:\Users\yangpe\Pictures\picture\p2.png)

&emsp;&emsp;基本思路我们又了，下面我们就来开发它吧。

### 具体开发
* 下载安装</br>
&emsp;&emsp;我们把插件信息配置成json格式存在远程，类似于：
``` 
{
		"configs": [
	    {
	      "active": true,
	      "className": "com.frank.plugin.ServerLogPlugin",
	      "id": "1",
	      "jarRemoteUrl": "file:D:/site/com-frank-log-plugin-0.0.1-SNAPSHOT.jar",
	      "name": "参数日志打印"
	    }
		],
		"name": "插件仓库"
}
```
&emsp;&emsp;通过下载插件,在服务启动的情况下加载我们定义好的jar包插件，然后把类加载至jvm中，便于调用执行，这里涉及到类的动态加载，那什么是类动态加载呢？</br>
&emsp;&emsp;动态加载指的是**每个编写的java拓展名类文件都存储着需要执行的程序逻辑，这些java文件经过Java编译器编译成拓展名为class的文件，class文件中保存着Java代码经转换后的虚拟机指令，当需要使用某个类时，虚拟机将会加载它的class文件，并创建对应的class对象，将class文件加载到虚拟机的内存。**
![image](C:\Users\yangpe\Pictures\picture\p3.png)</br>
&emsp;&emsp;我们这里借用idea生成的类图结构看出URLClassLoader中存在一个URLClassPath类，通过这个类就可以找到要加载的字节码流，也就是说URLClassPath类负责找到要加载的字节码，再读取成字节流，最后通过defineClass()方法创建类的Class对象。从URLClassLoader类的结构图可以看出其构造方法都有一个必须传递的参数URL[]，该参数的元素是代表字节码文件的路径,换句话说在创建URLClassLoader对象时必须要指定这个类加载器的到那个目录下找class文件，为了更加便捷的加载类，我们可以通过反射的方式调用其protected方法addURL来加载类。所以代码可以这样写：</br>
``` 
	File jarFile = new File(getLocalJarFile(config));
        // 从远程下载plugin 文件至本地
        if (!jarFile.exists()) {
            URL url = new URL(config.getJarRemoteUrl());
            InputStream stream = url.openStream();
            jarFile.getParentFile().mkdirs();
            try {
                Files.copy(stream, jarFile.toPath());
            } catch (Exception e) {
                jarFile.deleteOnExit();
                throw new RuntimeException(e);
            }
            stream.close();
        }
        // 将本地jar 文件加载至 classLoader
        URLClassLoader loader = (URLClassLoader) getClass().getClassLoader();
        URL targetUrl = jarFile.toURI().toURL();
        boolean isLoader = false;
        for (URL url : loader.getURLs()) {
            if (url.equals(targetUrl)) {
                isLoader = true;
                break;
            }
        }
        if (!isLoader) {
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            add.setAccessible(true);
            add.invoke(loader, targetUrl);
        }
        // 初始化 Plugin Advice 实例化
        Class<?> adviceClass = loader.loadClass(config.getClassName());
        if (!Advice.class.isAssignableFrom(adviceClass)) {
            throw new RuntimeException(
                    String.format("plugin 配置错误 %s非 %s的实现类 ", config.getClassName(), Advice.class.getName()));
        }
        adviceCache.put(adviceClass.getName(), (Advice) adviceClass.newInstance());
``` 
* 插件启用</br>
&emsp;&emsp;在下载安装阶段我们已经通过类的动态加载把插件类加载至jvm中，但是要实现对功能调用和切入，必须得使用AOP的方式。先来看看准备好的一个小插件：</br>
```
public class ServerLogPlugin implements MethodBeforeAdvice {

	public void before(Method method, Object[] args, Object target) throws Throwable {
		String result = String.format("%s.%s() 参数:%s", method.getDeclaringClass().getName(),
		 method.getName(),Arrays.toString(args));
		System.out.println(result);
	}
}
```
这里用到了AOP，什么是AOP呢？</br>
&emsp;&emsp;**AOP是Spring提供的关键特性之一。AOP即面向切面编程，是OOP编程的有效补充。使用AOP技术，可以将一些系统性相关的编程工作，独立提取出来，独立实现，然后通过切面切入进系统。从而避免了在业务逻辑的代码中混入很多的系统相关的逻辑——比如权限管理，事物管理，日志记录等等。这些系统性的编程工作都可以独立编码实现，然后通过AOP技术切入进系统即可。从而达到了 将不同的关注点分离出来的效果。**</br>
&emsp;&emsp;插件类实现了MethodBeforeAdvice接口，这个接口是aop的接口Advice的继承，它是一种前置增强，定义我们的插件在所切入的方法之前执行，能够达到在service方法调用之前打印其传入参数。所以我们在启用插件的时候就是去开始aop的切入，具体的实现方法入下：
```
<!-- 接入系统aop配置 -->
<!-- 插件工厂配置 -->
	<bean id="pluginFactory" class="com.frank.plugin.DefaultPluginFactory" />
	<aop:config>
		<aop:aspect id="aspect" ref="pluginFactory">
			<aop:pointcut id="point" expression="execution(* *.*(..))" />
			<aop:before method="doBefore" pointcut-ref="point" />
		</aop:aspect>
	</aop:config>
```
```
//DefaultPluginFactory中的启用插件的方法实现

PluginConfig config = configs.get(pluginConfigId);
Arrays.stream(applicationContext.getBeanDefinitionNames())
        .map(applicationContext::getBean)
        .filter(o -> o!=null).filter(o -> (o instanceof Advised))
        .filter(o -> findAdvice(config.getClassName(), (Advised) o) ==null)
        .forEach(o -> {
            Advice advice;
            try {
                advice = buildAdvice(config);
                ((Advised) o).addAdvice(advice);
            } catch (Exception e) {
                throw new RuntimeException("启用失败", e);
            }
        });
try {
    config.setActive(true);
    storeConfigs();
} catch (IOException e) {
    // TODO 需要回滚已添加的切面
    throw new RuntimeException("启用失败", e);
}
``` 
* 停用、卸载</br>
&emsp;&emsp;停用和启用插件方法相反，只需移除相关插件类的切面通知即可。卸载判断是否被启用，如果启用则停用后再删除对应插件，代码比较简单，不做阐述。

### 最终效果
&emsp;&emsp;本文实现的插件系统作为测试使用，功能模块比较简单，有很多不完善的地方，最后实现的功能入下：
![image](C:\Users\yangpe\Pictures\picture\p4.png)</br>
![image](C:\Users\yangpe\Pictures\picture\p5.png)</br>
### 写在后面
&emsp;&emsp;经过本次实践，进一步加深了对类的加载和aop的理解，后面了解到，插件这种类似的功能在安卓开发上有应用，安卓中通过动态代理的方式Hook系统服务,实现各种特定的功能，当然它的做法和实现复杂的多，但是我想他也离不开动态加载等。最后关于这web插件的扩展，其实有很多种方式，可以做成平台，也可以在自己公司搭建通用化组件。
![image](C:\Users\yangpe\Pictures\picture\p7.png)</br>