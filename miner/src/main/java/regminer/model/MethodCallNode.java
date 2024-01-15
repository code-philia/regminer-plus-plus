package regminer.model;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Song Rui
 */
public class MethodCallNode {
    String methodName;
    String className;
    String packageName;

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
        return filePath;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    String filePath;
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

    public MethodCallNode(String packageName, String filePath, MethodDeclaration methodDeclaration) {
        this.methodName = methodDeclaration.getNameAsString();
        this.filePath = filePath;
        this.methodDeclaration = methodDeclaration;
        this.packageName = packageName;
        this.className = findClassName();
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
        return (this.filePath + this.signature).hashCode();
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
                    this.filePath.equals(methodCallNode.getFilePath());
        }
        return false;
    }
}
