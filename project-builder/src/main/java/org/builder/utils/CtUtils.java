package org.builder.utils;

import org.builder.domain.Compiler;
import org.builder.domain.OS;

public class CtUtils {

    public static String combineTestCommand(String packageName, String className, String methodName, Compiler compiler, String osName) {
        //TODO handle multi-modules project
        String result = packageName + "." + className;
        switch (compiler) {
            case GRADLE:
                return Compiler.GRADLE.getTestCommand(osName) + result + "." + methodName;
            case GRADLEW:
                return Compiler.GRADLEW.getTestCommand(osName) + result + "." + methodName;
            case MVNW:
                return Compiler.MVNW.getTestCommand(osName) + result + "#" + methodName;
            case MVN:
            default:
                return Compiler.MVN.getTestCommand(osName) + result + "#" + methodName;
        }
    }

    public static String getOSType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains(OS.WINDOWS)) {
            return OS.WINDOWS;
        } else {
            return OS.UNIX;
        }
    }
}