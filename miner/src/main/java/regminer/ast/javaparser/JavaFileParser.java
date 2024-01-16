package regminer.ast.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Song Rui
 */
public class JavaFileParser {
    List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationList;
    private String filePath;

    public String getOldFilePath() {
        return oldFilePath;
    }

    private String oldFilePath;

    public String getPackageName() {
        return packageName;
    }

    private String packageName;

    public List<MethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    public List<ConstructorDeclaration> getConstructorDeclarations() {
        return constructorDeclarations;
    }

    public JavaFileParser(String filePath, String oldFilePath) {
        this.filePath = filePath;
        this.oldFilePath = oldFilePath;
        methodDeclarations = new ArrayList<>();
        constructorDeclarations = new ArrayList<>();
    }

    List<MethodDeclaration> methodDeclarations;
    List<ConstructorDeclaration> constructorDeclarations;

    public boolean parse() {
        Optional<CompilationUnit> result;
        try {
            result = new JavaParser().parse(Paths.get(filePath)).getResult();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (result.isEmpty()) {
            return false;
        }
        com.github.javaparser.ast.CompilationUnit compilationUnit = result.get();
        if (compilationUnit.getPackageDeclaration().isPresent()) {
            packageName = compilationUnit.getPackageDeclaration().get().getName().asString();
        }
        classOrInterfaceDeclarationList = compilationUnit.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration c : classOrInterfaceDeclarationList) {
            methodDeclarations.addAll(c.findAll(MethodDeclaration.class));
            constructorDeclarations.addAll(c.findAll(ConstructorDeclaration.class));
        }
        return true;
    }
}
