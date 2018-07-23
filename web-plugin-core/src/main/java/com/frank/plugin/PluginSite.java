package com.frank.plugin;

import java.io.Serializable;
import java.util.List;

public class PluginSite implements Serializable {
	/**
	 * 站点名称
	 */
    private String name;
	/**
	 * 插件配置信息
	 */
	private List<PluginConfig> configs;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<PluginConfig> getConfigs() {
		return configs;
	}

	public void setConfigs(List<PluginConfig> configs) {
		this.configs = configs;
	}

}