package regminer.miner;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.Statement;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import regminer.ast.javaparser.JavaFileParser;
import regminer.constant.Conf;
import regminer.model.*;

import java.io.File;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;

/**
 * @author Song Rui
 */
public class HeuristicBfcDetector extends BfcDetector {

    Map<String, Map<String,JavaFileParser>> javaFileParserMap;

    public HeuristicBfcDetector(Repository repo, Git git) {
        super(repo, git);
    }

    @Override
    void detect(RevCommit commit, List<PotentialRFC> potentialRFCs) throws Exception {
        // 1)首先我们将记录所有的标题中包含fix的commit
        String message1 = commit.getFullMessage().toLowerCase();
        if (!message1.contains("fix") && !message1.contains("close")) {
            return;
        }
        // 针对标题包含fix的commit我们进一步分析本次提交修改的文件路径
        List<ChangedFile> files = getLastDiffFiles(commit);
        if (files == null) {
            return;
        }
        List<NormalFile> normalJavaFiles = getNormalJavaFiles(files);
        if (!hitHeuristicFixingRules(commit.getName(), normalJavaFiles)) {
            return;
        }
        List<TestFile> testcaseFiles = getTestFiles(files);
        List<SourceFile> sourceFiles = getSourceFiles(files);
        // 1）若所有路径中存在任意一个路径包含test相关的Java文件则我们认为本次提交中包含测试用例。
        // 2）若所有路径中除了测试用例还包含其他的非测试用例的Java文件则commit符合条件
        PotentialRFC pRFC = new PotentialRFC(commit);
        pRFC.setNormalJavaFiles(normalJavaFiles);
        pRFC.setSourceFiles(sourceFiles);

        if (!testcaseFiles.isEmpty() && !normalJavaFiles.isEmpty()) {
            pRFC.setTestCaseFiles(testcaseFiles);
            pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SELF);
            potentialRFCs.add(pRFC);
        } else if (justNormalJavaFile(files)) {
//				针对只标题只包含fix但是修改的文件路径中没有测试用例的提交
//				我们将在(c-3,c+3) 的范围内检索可能的测试用例
//				[TODO] songxuezhi
            List<PotentialTestCase> pls = findTestCommit(commit);
            pRFC.setNormalJavaFiles(normalJavaFiles);
            if (pls != null && !pls.isEmpty()) {
                pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SEARCH);
                pRFC.setPotentialTestCaseList(pls);
                potentialRFCs.add(pRFC);
            } else {
                // 使用启发式规则判断是否有可能为BFC，若有可能则尝试自动生成测试用例
                // call diff_tester to create test file and generate test cases
                Methodx targetMethod = findEntranceMethodOfChanged(commit.getName(), normalJavaFiles);
                if (targetMethod == null) {
                    return;
                }
                // 调用diff_test生成用例
                generateTestCases(targetMethod);
                // if generate successfully, add this commit into potentialRFCs list

                pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_DIFF_TEST);
                potentialRFCs.add(pRFC);
            }
        }
    }

    private void generateTestCases(Methodx targetMethod) {
    }


    /**
     * for modified methods, find the entrance function to generate test case
     * @param commit commit name
     * @param javaFiles modified files
     * @return entrance function name
     */
    private Methodx findEntranceMethodOfChanged(String commit, List<NormalFile> javaFiles) {
        // 找到所有发生修改的方法，如果只有一个方法修改，直接return
        Set<Methodx> modifiedFunction = new HashSet<>();
        for (NormalFile javaFile : javaFiles) {
            File file = new File(Conf.META_PATH, javaFile.getNewPath());
            if (javaFileParserMap.get(commit).get(file.getAbsolutePath()) == null) {
                JavaFileParser parser = new JavaFileParser(file.getAbsolutePath());
                parser.parse();
                javaFileParserMap.get(commit).put(file.getAbsolutePath(), parser);
            }
            findModifiedFunction(commit, file.getAbsolutePath(), modifiedFunction, javaFile.getEditList());
        }
        if (modifiedFunction.isEmpty()) {
            return null;
        }
        if (modifiedFunction.size() == 1) {
            Object[] modifiedFunctionArray = modifiedFunction.toArray();
            return (Methodx) modifiedFunctionArray[0];
        }
        // 如果有多个方法修改
        return findEntranceFunction(commit, modifiedFunction);
    }

    private Methodx findEntranceFunction(String commit, Set<Methodx> modifiedFunction) {
        List<MethodCall> commonRootCalls = new ArrayList<>(2);
        // parse method calls
        for (Methodx methodx : modifiedFunction) {
            JavaFileParser parser = javaFileParserMap.get(commit).get(methodx.getFilePath());

        }

        return null;
    }

    private void findModifiedFunction(String commit, String filePath, Set<Methodx> modifiedFunction, List<Edit> editList) {
        if (editList.isEmpty()) {
            return;
        }
        JavaFileParser parser = javaFileParserMap.get(commit).get(filePath);
        List<MethodDeclaration> methods = parser.getMethodDeclarations();
        List<ConstructorDeclaration> constructorDeclarations = parser.getConstructorDeclarations();
        for (Edit editInfo : editList) {
            if (Edit.Type.EMPTY.equals(editInfo.getType())) {
                continue;
            }
            for (int line = editInfo.getBeginB();line < editInfo.getEndB();line++) {
                for (MethodDeclaration methodDeclaration : methods) {
                    if (line >= methodDeclaration.getBegin().get().line && line <= methodDeclaration.getEnd().get().line) {
                        modifiedFunction.add(new Methodx(getSignature(methodDeclaration.getNameAsString(), methodDeclaration.getParameters()), filePath));
                        break;
                    }
                }
                for (ConstructorDeclaration c : constructorDeclarations) {
                    if (line >= c.getBegin().get().line && line <= c.getEnd().get().line) {
                        modifiedFunction.add(new Methodx(getSignature(c.getNameAsString(), c.getParameters()), filePath));
                        break;
                    }
                }
            }
            
        }
    }

    private String getSignature(String name, List<Parameter> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
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


    private boolean hitHeuristicFixingRules(String commit, List<NormalFile> javaFiles) {
        for (NormalFile normalFile : javaFiles) {
            File file = new File(Conf.META_PATH, normalFile.getNewPath());
                // parse file to find if modified code hits the rules
            JavaFileParser parser = new JavaFileParser(file.getAbsolutePath());
            if (!parser.parse()) {
                return false;
            }
            javaFileParserMap.computeIfAbsent(commit, k -> new HashMap<>(4));
            javaFileParserMap.get(commit).put(file.getAbsolutePath(), parser);
            // todo 判断发生的变更是什么，排除非节点变更类型如格式、注释
            for (Edit editInfo : normalFile.getEditList()) {
                if (Edit.Type.EMPTY.equals(editInfo.getType())) {
                    continue;
                }
                for (MethodDeclaration methodDeclaration : parser.getMethodDeclarations()) {
                    if (methodDeclaration.getBody().isEmpty() || methodDeclaration.getBegin().isEmpty() || methodDeclaration.getEnd().isEmpty()) {
                        continue;
                    }
                    if (methodDeclaration.getBegin().get().line > editInfo.getEndB() || methodDeclaration.getEnd().get().line < editInfo.getBeginB()) {
                        continue;
                    }
                    BlockStmt blockStmt = methodDeclaration.getBody().get();
                    if (blockStmt.isEmpty()) {
                        continue;
                    }
                    if (hitStatements(blockStmt.getChildNodes(), editInfo)) {
                        return true;
                    }
                }
                for (ConstructorDeclaration constructorDeclaration : parser.getConstructorDeclarations()) {
                    if (constructorDeclaration.getBody().isEmpty() || constructorDeclaration.getBegin().isEmpty() || constructorDeclaration.getEnd().isEmpty()) {
                        continue;
                    }
                    if (constructorDeclaration.getBegin().get().line > editInfo.getEndB() || constructorDeclaration.getEnd().get().line < editInfo.getBeginB()) {
                        continue;
                    }
                    if (hitStatements(constructorDeclaration.getBody().getChildNodes(), editInfo)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hitStatements(List<Node> nodes, Edit editInfo) {
        if (nodes.isEmpty()) {
            return false;
        }
        for (Node s : nodes) {
            int begin;
            int end;
            if (s.getBegin().isPresent()) {
                begin = s.getBegin().get().line;
            } else {
                continue;
            }
            if (s.getEnd().isPresent()) {
                end = s.getEnd().get().line;
            } else {
                continue;
            }
            if (begin > editInfo.getEndB() || end < editInfo.getBeginB()){
                continue;
            }
            if (s instanceof ThrowStmt || s instanceof CatchClause) {
                if (hitEditLines(begin, end, editInfo)) {
                    return true;
                }
                continue;
            }
            if (s instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) s;
                begin = ifStmt.getCondition().getBegin().get().line;
                end = ifStmt.getCondition().getEnd().get().line;
                if (hitEditLines(begin, end, editInfo)) {
                    return true;
                }
            }
            // search for child statements
            return hitStatements(s.getChildNodes(), editInfo);
        }
        return false;
    }

    private boolean hitEditLines(int begin, int end, Edit editInfo) {
        for (int i = editInfo.getBeginB();i <= editInfo.getEndB();i++) {
            if (i >= begin && i<= end) {
                return true;
            }
        }
        return false;
    }

}
