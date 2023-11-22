package org.builder.api;

import org.builder.CtReferees;
import org.builder.domain.JDK;
import org.builder.exec.Executor;
import org.builder.model.CompileResult;
import org.builder.model.EnvCommands;
import org.builder.utils.CtUtils;
import org.builder.domain.Compiler;

public class BaseCompileAndTest extends CtStrategy {
    @Override
    public CompileResult compile() {
        EnvCommands envCommands = new EnvCommands();
        envCommands.setCompiler(Compiler.MVN);
        String osType = CtUtils.getOSType();
        envCommands.setOsName(osType);
        envCommands.takeCommand(EnvCommands.CommandKey.JDK, JDK.J8.getCommand());

        String compileCommand = Compiler.MVN.getCompileCommand(osType);

        envCommands.takeCommand(EnvCommands.CommandKey.COMPILE, compileCommand);
        System.out.println(envCommands.compute());

        return new CompileResult(
                CtReferees.JudgeCompileState(new Executor(osType).setDirectory(projectDir)
                        .exec(envCommands.compute()).getMessage()), envCommands
        );

    }
}
