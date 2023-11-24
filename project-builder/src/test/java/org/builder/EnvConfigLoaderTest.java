package org.builder;

import org.junit.Assert;
import org.junit.Test;

public class EnvConfigLoaderTest {
    @Test
    public void testLoadConfig() {
        //The config file is in the miner module
        String moduleDir = EnvConfigLoader.getModuleAbsDir("miner");
        System.out.println(moduleDir);
        Assert.assertTrue(moduleDir.endsWith("miner"));
        EnvConfigLoader.setConfigPath(moduleDir + EnvConfigLoader.SEPARATOR + "miner.properties");
        EnvConfigLoader.refresh();
        Assert.assertFalse(EnvConfigLoader.j8File.isEmpty());
        Assert.assertTrue(EnvConfigLoader.j8File.contains("jdk1.8"));
        Assert.assertFalse(EnvConfigLoader.jdkDir.isEmpty());
    }
}