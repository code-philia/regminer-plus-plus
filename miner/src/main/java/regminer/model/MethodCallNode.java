package regminer.model;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Song Rui
 */
public class MethodCallNode {
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    String methodName;

    public String getClassName() {
        return className;
    }

    String className;

    public String getPackageName() {
        return packageName;
    }

    String packageName;

    public String getOldFilePath() {
        return oldFilePath;
    }

    String oldFilePath;

    public boolean getNotCalled() {
        return notCalled;
    }

    public void setNotCalled(boolean notCalled) {
        this.notCalled = notCalled;
    }

    boolean notCalled;

    public String getSignature() {
        return signature;
    }

    String signature;

    public String getMethodName() {
        return methodName;
    }

    public String getFilePath() {
        return this.file.getAbsolutePath();
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    MethodDeclaration methodDeclaration;

    public Set<MethodCallNode> getCalls() {
        return calls;
    }

    public Set<MethodCallNode> getCallers() {
        return callers;
    }

    public Set<MethodCallNode> getRootCallers() {
        return rootCallers;
    }

    Set<MethodCallNode> calls;
    Set<MethodCallNode> callers;
    Set<MethodCallNode> rootCallers;

    public File getFile() {
        return file;
    }

    File file;

    public MethodCallNode(String packageName, File file, MethodDeclaration methodDeclaration, String oldFilePath) {
        this.methodName = methodDeclaration.getNameAsString();
        this.methodDeclaration = methodDeclaration;
        this.packageName = packageName;
        this.file = file;
        this.className = findClassName();
        this.oldFilePath = oldFilePath;
        this.signature = generateSignature();
        callers = new HashSet<>();
        calls = new HashSet<>();
        rootCallers = new HashSet<>();
        notCalled = true;
    }

    private String findClassName() {
        Node node = methodDeclaration;
        while (node.getParentNode().isPresent()) {
            if (node.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
                return ((ClassOrInterfaceDeclaration) node.getParentNode().get()).getNameAsString();
            }
            node = node.getParentNode().get();
        }
        return "";
    }

    private String generateSignature() {
        StringBuilder sb = new StringBuilder()
                .append(packageName)
                .append(".")
                .append(className)
                .append(".");
        List<Parameter> parameters = methodDeclaration.getParameters();
        sb.append(methodName).append("(");
        int size = parameters.size();
        for (int i = 0;i< size;i++){
            sb.append(parameters.get(i).getType());
            if (i < size-1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return (this.getFilePath() + this.signature).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (o instanceof MethodCallNode) {
            MethodCallNode methodCallNode = (MethodCallNode) o;
            return this.signature.equals(methodCallNode.getSignature()) &&
                    this.getFilePath().equals(methodCallNode.getFilePath());
        }
        return false;
    }
}
