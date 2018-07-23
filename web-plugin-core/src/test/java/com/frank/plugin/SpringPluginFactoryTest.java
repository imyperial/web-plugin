package com.frank.plugin;

import org.junit.Before;
import org.junit.Test;

/**
 * @describtion:
 * @author: frank
 * @date: 2018/7/23 上午12:53
 */
public class SpringPluginFactoryTest {
    private DefaultPluginFactory factory;

    @Before
    public void init() {
        factory = new DefaultPluginFactory();
    }

    // 本地安装测试
    @Test
    public void installTest() {
        PluginConfig config = new PluginConfig();
        config.setActive(false);
        config.setId("2");
        config.setJarRemoteUrl(
                "file:D:/site/tuling-teach-spring-plugin-0.0.2-SNAPSHOT.jar");
        config.setClassName("com.tuling.plugin.CountingBeforeAdvice");
        config.setName("服务执行统计");
        factory.installPlugin(config, false);
        factory.getPluginList();
    }

    public void getPluginList(){
        factory.getPluginList();
    }
}
