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
import java.util.List;
import java.util.Optional;

/**
 * @author Song Rui
 */
public class JavaFileParser {
    List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationList;
    private String filePath;

    public List<MethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    public List<ConstructorDeclaration> getConstructorDeclarations() {
        return constructorDeclarations;
    }

    public JavaFileParser(String filePath) {
        this.filePath = filePath;
    }

    List<MethodDeclaration> methodDeclarations;
    List<ConstructorDeclaration> constructorDeclarations;

    public boolean parse() {
        Optional<CompilationUnit> result = null;
        try {
            result = new JavaParser().parse(Paths.get(filePath)).getResult();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
//       if (o instanceof com.github.javaparser.ParseResult) {
//           ((com.github.javaparser.ParseResult<com.github.javaparser.ast.CompilationUnit>) o).getResult().ifPresent(r -> compilationUnit.set(r));
//       } else {
//           compilationUnit.set((com.github.javaparser.ast.CompilationUnit) o);
//       }
        if (result.isEmpty()) {
            return false;
        }
        com.github.javaparser.ast.CompilationUnit compilationUnit = result.get();
        classOrInterfaceDeclarationList = compilationUnit.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration c : classOrInterfaceDeclarationList) {
            methodDeclarations = c.findAll(MethodDeclaration.class);
            constructorDeclarations = c.findAll(ConstructorDeclaration.class);
        }
        return true;
    }
}
