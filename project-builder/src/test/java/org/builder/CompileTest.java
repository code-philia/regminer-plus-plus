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
        EnvConfigLoader.setConfigPath("/Users/zhjlu/codes/graduate/RegMiner/miner/miner.properties");// set to your own properties path!
        EnvConfigLoader.refresh();
        ProjectBuilder projectBuilder = new ProjectBuilder();
        projectBuilder.setProjectDir(new File("/Users/zhjlu/codes/graduate/miner-space/jackson-core/jackson-core_manual"));
        CompileResult result = projectBuilder.compile();
        Assert.assertEquals(CompileResult.CompileState.SUCCESS, result.getState());
    }

    @Test
    public void testAutoCompile() {
        EnvConfigLoader.setConfigPath("/Users/zhjlu/codes/graduate/RegMiner/miner/miner.properties");// set to your own properties path!
        EnvConfigLoader.refresh();
        ProjectBuilder projectBuilder = new ProjectBuilder(
                new File("/Users/zhjlu/codes/graduate/miner-space/jackson-core/jackson-core_manual"),
                new CtContext(new AutoCompileAndTest()));
        CompileResult result = projectBuilder.compile();
        Assert.assertEquals(CompileResult.CompileState.SUCCESS, result.getState());
    }

}