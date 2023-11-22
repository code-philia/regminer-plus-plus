package org.builder;

import org.junit.Assert;
import org.junit.Test;

public class ConfigLoaderTest {
    @Test
    public void testLoadConfig() {
        ConfigLoader.setConfigPath("/Users/zhjlu/codes/graduate/RegMiner/miner/miner.properties");// set to your own properties path!
        ConfigLoader.refresh();
        Assert.assertFalse(ConfigLoader.j8File.isEmpty());
        Assert.assertTrue(ConfigLoader.j8File.contains("jdk1.8"));
        Assert.assertFalse(ConfigLoader.jdkDir.isEmpty());
    }
}