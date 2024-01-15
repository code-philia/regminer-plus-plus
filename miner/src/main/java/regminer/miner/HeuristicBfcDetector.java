package regminer.miner;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.tester.DifferentialTester;
import com.tester.processor.MinerProcessor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import regminer.ast.javaparser.JavaFileParser;
import regminer.constant.Conf;
import regminer.model.*;

import java.io.File;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import regminer.utils.FileUtilx;
import regminer.utils.GitUtil;

import java.io.IOException;
import java.util.*;

/**
 * @author Song Rui
 */
public class HeuristicBfcDetector extends BfcDetector {

    Map<String, Map<String,JavaFileParser>> javaFileParserMap;
    Map<String, Map<String, Set<Integer>>> javaIgnoreEditLines;
    List<TypeSolver> typeSolvers;
    GitUtil gitUtil;
    CombinedTypeSolver combinedTypeSolver;

    public HeuristicBfcDetector(Repository repo, Git git) {
        super(repo, git);
        javaIgnoreEditLines = new HashMap<>();
        javaFileParserMap = new HashMap<>();
        typeSolvers = new ArrayList<>(8);
        typeSolvers.add(new ReflectionTypeSolver());
    }

    @Override
    List<PotentialRFC> detectOnFilter(List<String> commitsFilter, Iterable<RevCommit> commits) throws Exception {
        List<PotentialRFC> potentialRFCs = new LinkedList<PotentialRFC>();
        // 定义需要记录的实验数据
        int countAll = 0;
        gitUtil = new GitUtil(Conf.META_PATH);
        typeSolvers.add(new JavaParserTypeSolver(Conf.SRC_JAVA_PATH));
        combinedTypeSolver = new CombinedTypeSolver();
        typeSolvers.forEach(combinedTypeSolver::add);
        // 开始迭代每一个commit
        for (RevCommit commit : commits) {
            if (commitsFilter.contains(commit.getName())) {
                gitUtil.checkout(commit.getName());
                detect(commit, potentialRFCs);
                countAll++;
            }
        }
        FileUtilx.log("总共分析了" + countAll + "条commit\n");
        FileUtilx.log("pRFC in total :" + potentialRFCs.size());
        return potentialRFCs;
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
        javaIgnoreEditLines.put(commit.getName(), new HashMap<>(4));
        // checkout to commit
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

//        if (!testcaseFiles.isEmpty() && !normalJavaFiles.isEmpty()) {
//            pRFC.setTestCaseFiles(testcaseFiles);
//            pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SELF);
//            potentialRFCs.add(pRFC);
//        } else if (justNormalJavaFile(files)) {
//				针对只标题只包含fix但是修改的文件路径中没有测试用例的提交
//				我们将在(c-3,c+3) 的范围内检索可能的测试用例
//				[TODO] songxuezhi
            List<PotentialTestCase> pls = findTestCommit(commit);
            pRFC.setNormalJavaFiles(normalJavaFiles);
//            if (pls != null && !pls.isEmpty()) {
//                pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SEARCH);
//                pRFC.setPotentialTestCaseList(pls);
//                potentialRFCs.add(pRFC);
//            } else {
                // 使用启发式规则判断是否有可能为BFC，若有可能则尝试自动生成测试用例
                // call diff_tester to create test file and generate test cases
                MethodCallNode targetMethod = findEntranceMethodOfChanged(commit.getName(), normalJavaFiles);
                if (targetMethod == null) {
                    return;
                }
                // 调用diff_test生成用例
                generateTestCases(commit, targetMethod, normalJavaFiles);
                // if generate successfully, add this commit into potentialRFCs list

                pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_DIFF_TEST);
                potentialRFCs.add(pRFC);
//            }
//        }
    }


    private void generateTestCases(RevCommit commit, MethodCallNode targetMethod, List<NormalFile> files) {
        Map<String, List<Integer>> suspiciousLines = new HashMap<String, List<Integer>>(2) {{
            put("working", Collections.singletonList(6));
            put("regression", Collections.singletonList(6));
        }};
        String workingCommit = "";
        try {
            RevCommit newRev1 = null;
            ObjectId newId1 = repo.resolve(commit.getName() + "^1");
            if (newId1 != null) {
                RevWalk revWalk = new RevWalk(repo);
                newRev1 = revWalk.parseCommit(newId1);
            }
            if (newRev1 == null) {
                return;
            }
            workingCommit = newRev1.getName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DifferentialTester dt = new DifferentialTester(targetMethod.getFilePath(),
                targetMethod.getMethodName(),
                suspiciousLines, new MinerProcessor.MinerInfo(
                        Conf.META_PATH, workingCommit, commit.getName(), targetMethod.getFilePath(), targetMethod.getFilePath(), ""
        ));
        dt.run();
    }


    /**
     * for modified methods, find the entrance function to generate test case
     * @param commit commit name
     * @param javaFiles modified files
     * @return entrance function name
     */
    private MethodCallNode findEntranceMethodOfChanged(String commit, List<NormalFile> javaFiles) {
        // 找到所有发生修改的方法，如果只有一个方法修改，直接return
        Set<MethodCallNode> modifiedFunction = new HashSet<>();
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
            return (MethodCallNode) modifiedFunctionArray[0];
        }
        // 如果有多个方法修改
        return findEntranceFunction(commit, modifiedFunction);
    }


    private MethodCallNode findEntranceFunction(String commit, Set<MethodCallNode> modifiedFunction) {
        Set<MethodCallNode> commonRootCalls = new HashSet<>();
        Set<MethodCallNode> callSet = new HashSet<>();
        Set<String> modifiedFiles = new HashSet<>();
        // parse method calls
        for (MethodCallNode methodCallNode : modifiedFunction) {
            modifiedFiles.add(methodCallNode.getFilePath());
        }
        for (String filePath : modifiedFiles) {
            JavaFileParser parser = javaFileParserMap.get(commit).get(filePath);
            List<MethodDeclaration> methodDeclarations = parser.getMethodDeclarations();
//            List<ConstructorDeclaration> constructorDeclarations = parser.getConstructorDeclarations();
            for (MethodDeclaration methodDeclaration : methodDeclarations) {
                if (methodDeclaration.getBody().isPresent()) {
                    BlockStmt blockStmt = methodDeclaration.getBody().get();
                    if (blockStmt.isEmpty()) {
                        continue;
                    }
                    List<Statement> statements = blockStmt.getStatements();
                    for (Statement s : statements) {
                        List<MethodCallExpr> methodCallExprList = s.findAll(MethodCallExpr.class);
                        if (methodCallExprList.isEmpty()) {
                            continue;
                        }
                        for (MethodCallExpr methodCallExpr : methodCallExprList) {
                            try {
                                // java.lang.OutOfMemoryError: Java heap space
                                if ("put".equals(methodCallExpr.getName())) {
                                    continue;
                                }
                                MethodUsage methodUsage = JavaParserFacade.get(combinedTypeSolver).solveMethodAsUsage(methodCallExpr);
                                MethodDeclaration md = ((JavaParserMethodDeclaration) methodUsage.getDeclaration()).getWrappedNode();
                                MethodCallNode currentMethod = new MethodCallNode(parser.getPackageName(), filePath, methodDeclaration);
                                MethodCallNode callee = new MethodCallNode(parser.getPackageName(), filePath, md);
                                callee.getCallers().add(currentMethod);
                                currentMethod.getCalls().add(callee);
                                if (callSet.contains(currentMethod)) {
                                    callSet.stream().filter(m -> m.equals(currentMethod)).forEach(m -> m.getCallers().add(callee));
                                } else {
                                    callSet.add(callee);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                }
            }
            // find common parent method
            for (MethodCallNode methodCallNode : modifiedFunction) {
                callSet.stream().filter(m -> m.equals(methodCallNode)).forEach(m -> m.getCallers()
                        .forEach(c -> {
                            if (modifiedFunction.contains(c)) {
                                commonRootCalls.add(c);
                            }
                            methodCallNode.setNotCalled(false);
                        }));
            }
            if (commonRootCalls.isEmpty()) {
                return null;
            }
            List<MethodCallNode> roots = new ArrayList<>();
            commonRootCalls.stream().filter(MethodCallNode::getNotCalled).forEach(roots::add);
            if (roots.isEmpty()) {
                return null;
            }
            return roots.get(0);
        }

    private void findModifiedFunction(String commit, String filePath, Set<MethodCallNode> modifiedFunction, List<Edit> editList) {
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
                if (javaIgnoreEditLines.get(commit) != null && javaIgnoreEditLines.get(commit).get(filePath) != null && javaIgnoreEditLines.get(commit).get(filePath).contains(line)) {
                    continue;
                }
                for (MethodDeclaration methodDeclaration : methods) {
                    if (line >= methodDeclaration.getBegin().get().line && line <= methodDeclaration.getEnd().get().line) {
                        modifiedFunction.add(new MethodCallNode(parser.getPackageName(), filePath, methodDeclaration));
                        break;
                    }
                }
//                for (ConstructorDeclaration c : constructorDeclarations) {
//                    if (line >= c.getBegin().get().line && line <= c.getEnd().get().line) {
//                        modifiedFunction.add(new MethodCallNode(getSignature(c.getNameAsString(), c.getParameters()), filePath, constructorDeclarations));
//                        break;
//                    }
//                }
            }
            
        }
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
                    if (hitStatements(commit, file.getAbsolutePath(), blockStmt.getChildNodes(), editInfo)) {
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
                    if (hitStatements(commit, file.getAbsolutePath(), constructorDeclaration.getBody().getChildNodes(), editInfo)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hitStatements(String commit, String filePath, List<Node> nodes, Edit editInfo) {
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
            // if the edit hit a comment or blank statement, then skip in the late parse()
            if (s instanceof Comment || s instanceof EmptyStmt) {
                for (int i = editInfo.getBeginB();i <= editInfo.getEndB();i++) {
                    if (i >= begin && i<= end) {
                        javaIgnoreEditLines.get(commit).computeIfAbsent(filePath, k -> new HashSet<>());
                        javaIgnoreEditLines.get(commit).get(filePath).add(i);
                    }
                }
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
            return hitStatements(commit, filePath, s.getChildNodes(), editInfo);
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

    @Override
    List<Edit> getEdits(DiffEntry entry) throws Exception {
        List<Edit> result = new LinkedList<>();
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            diffFormatter.setRepository(repo);
            FileHeader fileHeader = diffFormatter.toFileHeader(entry);
            List<? extends HunkHeader> hunkHeaders = fileHeader.getHunks();
            for (HunkHeader hunk : hunkHeaders) {
                result.addAll(hunk.toEditList());
            }
        }
        return result;

    }

}
