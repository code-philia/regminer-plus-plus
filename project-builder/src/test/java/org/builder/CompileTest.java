package org.builder;

import org.builder.api.AutoCompileAndTest;
import org.builder.api.CtContext;
import org.builder.api.ProjectBuilder;
import org.builder.model.CompileResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class CompileTest {

    @Test
    public void testBaseCompile() {
        //The config file is in the miner module
        String moduleDir = EnvConfigLoader.getModuleAbsDir("miner");
        EnvConfigLoader.setConfigPath(moduleDir + EnvConfigLoader.SEPARATOR + "miner.properties");// set to your own properties path!
        EnvConfigLoader.refresh();
        ProjectBuilder projectBuilder = new ProjectBuilder();
        projectBuilder.setProjectDir(new File(EnvConfigLoader.testProjectDir));
        CompileResult result = projectBuilder.compile();
        Assert.assertEquals(CompileResult.CompileState.SUCCESS, result.getState());
    }

    @Test
    public void testAutoCompile() {
        String moduleDir = EnvConfigLoader.getModuleAbsDir("miner");
        EnvConfigLoader.setConfigPath(moduleDir + EnvConfigLoader.SEPARATOR + "miner.properties");// set to your own properties path!
        EnvConfigLoader.refresh();
        ProjectBuilder projectBuilder = new ProjectBuilder(
                new File(EnvConfigLoader.testProjectDir),
                new CtContext(new AutoCompileAndTest()));
        CompileResult result = projectBuilder.compile();
        Assert.assertEquals(CompileResult.CompileState.SUCCESS, result.getState());
    }

}