package org.builder;

import org.junit.Assert;
import org.junit.Test;

public class EnvConfigLoaderTest {
    @Test
    public void testLoadConfig() {
        EnvConfigLoader.setConfigPath("/Users/zhjlu/codes/graduate/RegMiner/miner/miner.properties");// set to your own properties path!
        EnvConfigLoader.refresh();
        Assert.assertFalse(EnvConfigLoader.j8File.isEmpty());
        Assert.assertTrue(EnvConfigLoader.j8File.contains("jdk1.8"));
        Assert.assertFalse(EnvConfigLoader.jdkDir.isEmpty());
    }
}