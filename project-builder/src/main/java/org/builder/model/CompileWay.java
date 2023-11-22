package org.builder.model;

import org.builder.domain.JDK;
import org.builder.domain.Compiler;

public class CompileWay {
    private Compiler compiler;
    private JDK jdk;
    private boolean isMultipleModules;

    public Compiler getCompiler() {
        return compiler;
    }

    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }

    public JDK getJdk() {
        return jdk;
    }

    public void setJdk(JDK jdk) {
        this.jdk = jdk;
    }

    public boolean isMultipleModules() {
        return isMultipleModules;
    }

    public void setMultipleModules(boolean multipleModules) {
        isMultipleModules = multipleModules;
    }

}
