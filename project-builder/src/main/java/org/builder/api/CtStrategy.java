package org.builder.api;

import org.builder.domain.JDK;
import org.builder.model.CompileResult;

import java.io.File;

public abstract class CtStrategy {
    File projectDir;
    JDK[] jdkSearchRange;

    public File getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }

    public JDK[] getJdkSearchRange() {
        return jdkSearchRange;
    }

    public void setJdkSearchRange(JDK[] jdkSearchRange) {
        this.jdkSearchRange = jdkSearchRange;
    }

    public abstract CompileResult compile();

}
