package regminer.model;

import java.util.List;

/**
 * @author Song Rui
 */
public class MethodCall {
    String methodName;
    String filePath;

    List<MethodCall> calls;
    List<MethodCall> callers;
    List<MethodCall> rootCallers;

    public MethodCall(String methodName, String filePath) {
        this.methodName = methodName;
        this.filePath = filePath;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
