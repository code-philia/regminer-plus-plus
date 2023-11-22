package org.builder;

import org.builder.model.CompileResult;
import org.builder.model.ExecResult;
import org.builder.model.TestcaseResult;

public class CtReferees {

    public static CompileResult.CompileState JudgeCompileState(String message) {
        return message.toLowerCase().contains("build success") ? CompileResult.CompileState.SUCCESS :
                CompileResult.CompileState.CE;
    }

    public static TestcaseResult judgeTestcaseResult(ExecResult execResult) {

        TestcaseResult testcaseResult = new TestcaseResult();
        testcaseResult.setUsageTime(execResult.getUsageTime());
        String message = execResult.getMessage();

        message = message.toLowerCase();
        System.out.println(message);
        TestcaseResult.TestState testState = getTestState(execResult, message);
        testcaseResult.setState(testState);
        testcaseResult.setExceptionMessage(spiltExceptionMessage(message,testState));
        return testcaseResult;
    }

    private static TestcaseResult.TestState getTestState(ExecResult execResult, String message) {
        TestcaseResult.TestState testState;
        if (execResult.isTimeOut()) {
            testState = TestcaseResult.TestState.TE;
        } else if (message.contains("build success")) {//todo: optimize the log analysis here!
            testState = TestcaseResult.TestState.PASS;
        } else if (message.contains("compilation error") || message.contains("compilation failure")) {
            testState = TestcaseResult.TestState.CE;
        } else if (message.contains("no test")) {
            testState = TestcaseResult.TestState.NOTEST;
        } else {
            testState = TestcaseResult.TestState.FAL;
        }
        return testState;
    }

    public static String spiltExceptionMessage(String message, TestcaseResult.TestState testState) {
        if (testState != TestcaseResult.TestState.FAL) {
            return null;
        }
        int splitStartNum = message.indexOf("t e s t s\n[info] ") + ("t e s t s\n[info] ").length();
        int splitEndNum = message.indexOf("[info] results:");
        if (splitEndNum < splitStartNum) {
            return "some error";
        }
        String testResult = message.substring(splitStartNum, splitEndNum).replace("-","")
                .replace("[info] ","").replace("[error] ","")
                .replace("[info]","").replace("[error]","");
        System.out.println(testResult);
        return testResult;
    }

}
