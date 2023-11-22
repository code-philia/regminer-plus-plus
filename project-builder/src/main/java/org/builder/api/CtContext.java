package org.builder.api;

import org.builder.domain.JDK;
import org.builder.model.CompileResult;

import java.io.File;

public class CtContext {
    private CtStrategy strategy;

    public CtContext(CtStrategy strategy) {
        this.strategy = strategy;
    }

    public CtContext setProjectDir(File projectDir) {
        strategy.setProjectDir(projectDir);
        return this;
    }

    public CtContext setJdkSearchRange(JDK[] jdkSearchRange) {
        strategy.setJdkSearchRange(jdkSearchRange);
        return this;
    }

    public CompileResult compile() {
        return this.strategy.compile();
    }



}
