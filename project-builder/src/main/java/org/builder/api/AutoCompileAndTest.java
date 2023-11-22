package org.builder.api;

import org.apache.tools.ant.DirectoryScanner;

import org.builder.CtReferees;
import org.builder.domain.JDK;
import org.builder.exec.Executor;
import org.builder.model.CompileResult;
import org.builder.model.CompileWay;
import org.builder.model.EnvCommands;
import org.builder.utils.CtUtils;
import org.builder.domain.Compiler;

import java.io.File;

public class AutoCompileAndTest extends CtStrategy {
    @Override
    public CompileResult compile() {
        CompileWay compileWay = new CompileWay();
        EnvCommands envCommands = new EnvCommands();
        compileWay.setCompiler(detectBuildTool(projectDir));
        envCommands.setCompiler(compileWay.getCompiler());
        String osType = CtUtils.getOSType();
        envCommands.setOsName(osType);
        String compileCommand = compileWay.getCompiler().getCompileCommand(osType);

        CompileResult.CompileState compileState = CompileResult.CompileState.CE;
        CompileResult compileResult = new CompileResult(compileState);
        String message = "";

        for (JDK jdk : JDK.values()) {
            envCommands.takeCommand(EnvCommands.CommandKey.JDK, jdk.getCommand());
            envCommands.takeCommand(EnvCommands.CommandKey.COMPILE, compileCommand);
            message = new Executor(osType).setDirectory(projectDir).exec(envCommands.compute()).getMessage();
            compileState = CtReferees.JudgeCompileState(message);
            if (compileState == CompileResult.CompileState.SUCCESS){
                compileWay.setJdk(jdk);
                compileResult = new CompileResult(compileState,envCommands,compileWay);
                break;
            } else {
                envCommands.remove(EnvCommands.CommandKey.JDK);
                envCommands.remove(EnvCommands.CommandKey.COMPILE);
            }
        }
        if (compileState == CompileResult.CompileState.CE){
            compileResult.setExceptionMessage(message);
        }

        return compileResult;

    }

    public Compiler detectBuildTool(File projectDir){
        Compiler buildTool = Compiler.MVN;
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(projectDir);
        scanner.setIncludes(new String[] {"pom.xml","**\\pom.xml","build.gradle","**\\build.gradle",
                "maven-wrapper.properties","**\\maven-wrapper.properties",
                "gradle-wrapper.properties","**\\gradle-wrapper.properties"});
        scanner.setCaseSensitive(true);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        for (String file: files){
            if(file.contains("maven-wrapper.properties")){
                buildTool = Compiler.MVNW;
                break;
            } else if(file.contains("gradle-wrapper.properties")){
                buildTool = Compiler.GRADLEW;
                break;
            } else if(file.contains("pom.xml")){
                buildTool = Compiler.MVN;
            } else if(file.contains("build.gradle")){
                buildTool = Compiler.GRADLE;
            } else {
                System.out.println("没有找到构建工具配置文件，默认使用mvn命令");
            }
        }
        return buildTool;
    }
}
