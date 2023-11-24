package org.builder.api;

import org.builder.core.AbstractProjectBuilder;
import org.builder.model.CompileResult;

import java.io.File;
import java.util.logging.Logger;

public class ProjectBuilder extends AbstractProjectBuilder {
    private CtContext ctContext;

    private File projectDir;

    private static final Logger logger = Logger.getLogger(ProjectBuilder.class.getName());

    public ProjectBuilder() {
        super();
        ctContext = new CtContext(new BaseCompileAndTest());
//        ctContext= new CtContext(new AutoCompileAndTest());
    }

    public ProjectBuilder(CtStrategy strategy) {
        super();
        this.ctContext = new CtContext(strategy);
    }

    public ProjectBuilder(File projectDir) {
        this();
        this.projectDir = projectDir;
    }

    public ProjectBuilder(File projectDir, CtContext ctContext) {
        this(projectDir);
        this.ctContext = ctContext;
    }

    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }

    /**
     * Get project structure graph to handle multi-modules project
     * @enhance feature
     */
    public void getProjectGraph(){

    }

    @Override
    public CompileResult compile(){
        if (projectDir == null) {
            logger.warning("Project dir is null");
            return null;
        }
        ctContext.setProjectDir(projectDir);
        return ctContext.compile();
    }
}
