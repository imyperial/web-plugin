package com.frank.plugin;

import java.util.List;

/**
 * @describtion:
 * @author: frank
 * @date: 2018/7/22 下午11:58
 */
public interface PluginFactory {

    /**
     * 装载指定插件
     *
     * @param pluginId
     */
    void activePlugin(String pluginId);

    /**
     * 移除指定插件
     *
     * @param pluginId
     */
    void disablePlugin(String pluginId);

    /**
     * 安装插件
     * @param plugin
     * @param active
     */
    void installPlugin(PluginConfig plugin,Boolean active);

    /**
     * 卸载插件
     * @param plugin
     */
    void uninstallPlugin(PluginConfig plugin);


    /**
     * 获取插件列表
     * @return List<PluginConfig>
     */
    List<PluginConfig> getPluginList();
}
