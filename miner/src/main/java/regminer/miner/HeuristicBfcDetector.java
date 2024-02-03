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
import regminer.exec.Executor;
import regminer.model.*;

import java.io.File;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import regminer.utils.FileUtilx;
import regminer.utils.GitUtil;

import java.io.IOException;
import java.util.*;

import static regminer.constant.Conf.PROJRCT_NAME;

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
        gitUtil = new GitUtil(Conf.META_PATH);
    }

    @Override
    List<PotentialRFC> detectOnFilter(List<String> commitsFilter, Iterable<RevCommit> commits) throws Exception {
        List<PotentialRFC> potentialRFCs = new LinkedList<PotentialRFC>();
        // 定义需要记录的实验数据
        int countAll = 0;
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
            // 使用启发式规则判断是否有可能为BFC，若有可能则尝试自动生成测试用例
            // call diff_tester to create test file and generate test cases
            MethodCallNode targetMethod = findEntranceMethodOfChanged(commit.getName(), normalJavaFiles);
            if (targetMethod == null) {
                return;
            }
        pRFC.setTargetMethod(targetMethod.getMethodName());
        // if generate successfully, add this commit into potentialRFCs list
//            List<PotentialTestCase> pls = generateTestCases(pRFC, commit, targetMethod, sourceFiles);
        List<PotentialTestCase> pls = generateTestCasesWithEvosuite(pRFC, targetMethod, sourceFiles);
//        savePotentialTestFile(files,commit, potentialTestCase);

        if (pls.isEmpty()) {
                return;
            }
            pRFC.setPotentialTestCaseList(pls);
            pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_DIFF_TEST);
            potentialRFCs.add(pRFC);
//        }
    }

    public List<PotentialTestCase> generateTestCasesWithEvosuite(PotentialRFC ptc, MethodCallNode targetMethod, List<SourceFile> sourceFiles) {
        gitUtil.checkout(ptc.getCommit().getName());
        String cmd = "java -jar D:\\repo\\evosuite\\evosuite-1.0.6.jar -class " + targetMethod.getClassName() + " -projectCP " + Conf.META_PATH+"\\target\\classes";
        String res = new Executor().exec(cmd);
        String testFile = "";
        List<PotentialTestCase> cases = new ArrayList<>();
        PotentialTestCase potentialTestCase = new PotentialTestCase(1);
        potentialTestCase.fileMap.put(testFile, new File(testFile));
        List<TestFile> testFiles = new ArrayList<>(){};
        testFiles.add(new TestFile(testFile));
        potentialTestCase.setTestFiles(testFiles);
        potentialTestCase.setSourceFiles(sourceFiles);
        ptc.setTestCaseFiles(testFiles);
        cases.add(potentialTestCase);
        return cases;
    }

    public List<PotentialTestCase> generateTestCases(PotentialRFC ptc, RevCommit commit, MethodCallNode targetMethod, List<SourceFile> sourceFiles) {
        List<PotentialTestCase> cases = new ArrayList<>();
        Map<String, List<Integer>> suspiciousLines = new HashMap<>(2) {{
            put("working", Collections.singletonList(6));
            put("regression", Collections.singletonList(6));
        }};
        String regressionCommit = "";
        try {
            RevCommit newRev1 = null;
            ObjectId newId1 = repo.resolve(commit.getName() + "^1");
            if (newId1 != null) {
                RevWalk revWalk = new RevWalk(repo);
                newRev1 = revWalk.parseCommit(newId1);
            }
            if (newRev1 == null) {
                return null;
            }
            regressionCommit = newRev1.getName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String testPath = "temp" + File.separator + PROJRCT_NAME + File.separator + commit.getName() + File.separator;
        String testFile = targetMethod.getMethodName() + "Test.java";
        try {
            File file = new File(testPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            gitUtil.checkout(commit.getName());
            String workingContent = FileUtilx.readContentFromFile(targetMethod.getFilePath());
            gitUtil.checkout(regressionCommit);
            String regressionContent = FileUtilx.readContentFromFile(targetMethod.getOldFilePath());
            DifferentialTester dt = new DifferentialTester(targetMethod.getFilePath().substring(targetMethod.getFilePath().lastIndexOf(File.separator)), testFile,
                    targetMethod.getMethodName(),
                    suspiciousLines, new MinerProcessor.MinerInfo(
                    Conf.META_PATH, commit.getName(), regressionCommit, targetMethod.getFilePath(), targetMethod.getOldFilePath(), testPath+testFile, targetMethod.getPackageName(), workingContent, regressionContent
            ));
            dt.run();
            PotentialTestCase potentialTestCase = new PotentialTestCase(1);
            potentialTestCase.fileMap.put(testFile, new File(testFile));
            potentialTestCase.setTestFiles((List<TestFile>) new TestFile(testFile));
            potentialTestCase.setSourceFiles(sourceFiles);
            ptc.setTestCaseFiles((List<TestFile>) new TestFile(testFile));
            cases.add(potentialTestCase);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cases;
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
                JavaFileParser parser = new JavaFileParser(file.getAbsolutePath(), javaFile.getOldPath());
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
        // 如果有多个方法修改，目前只返回一个
        return findEntranceFunction(commit, modifiedFunction);
    }


    private MethodCallNode findEntranceFunction(String commit, Set<MethodCallNode> modifiedFunction) {
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
                                MethodCallNode currentMethod = new MethodCallNode(parser.getPackageName(), filePath, methodDeclaration, parser.getOldFilePath());
                                MethodCallNode callee = new MethodCallNode(parser.getPackageName(), filePath, md, parser.getOldFilePath());
                                callee.getCallers().add(currentMethod);
                                currentMethod.getCalls().add(callee);
                                if (callSet.contains(currentMethod)) {
                                    callSet.stream().filter(m -> m.equals(currentMethod)).forEach(m -> m.getCalls().add(callee));
                                } else {
                                    callSet.add(currentMethod);
                                }
                                if (callSet.contains(callee)) {
                                    callSet.stream().filter(m -> m.equals(callee)).forEach(m -> m.getCallers().add(currentMethod));
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
        // 先找到根节点集合rootCallsMap，再两两寻找common parent, 注意寻找过程中会出现多头的情况，如
        // L: f, i, e
        // J: i, a
        // rootCallsMap的key为root，只用于保证两两寻找的节点在同一棵树上
        Map<MethodCallNode, List<MethodCallNode>> rootCallsMap = searchForRoots(modifiedFunction, callSet);
        Set<MethodCallNode> commonRootCalls = new HashSet<>();
        for (MethodCallNode methodCallNode : rootCallsMap.keySet()) {
            if (rootCallsMap.get(methodCallNode).size() == 1) {
                commonRootCalls.add(rootCallsMap.get(methodCallNode).get(0));
                continue;
            }
            MethodCallNode tempNode = rootCallsMap.get(methodCallNode).get(0);
            int size = rootCallsMap.get(methodCallNode).size();
            for (int i = 1;i<size;i++) {
                assert tempNode != null;
                tempNode = findLowestCommonParent(tempNode, rootCallsMap.get(methodCallNode).get(i));
            }
            if (tempNode == null) {continue;}
            commonRootCalls.add(tempNode);
        }
        if (commonRootCalls.isEmpty()) {
            return null;
        }
        return (MethodCallNode) commonRootCalls.toArray()[0];
    }

    private MethodCallNode findLowestCommonParent(MethodCallNode childA, MethodCallNode childB) {
        if (childB == null || childA.getCallers().isEmpty()) {
            return childA;
        }
        if (childB.getCallers().isEmpty()) {
            return childB;
        }
        MethodCallNode p = confirmChild(childA, childB);
        if (p != null) {
            return p;
        }
        p = confirmChild(childB, childA);
        if (p != null) {
            return p;
        }
        // A and B has a common parent above both of them
        for (MethodCallNode caller : childA.getCallers()) {
            p = confirmChild(caller, childB);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    private MethodCallNode confirmChild(MethodCallNode parent, MethodCallNode child) {
        for (MethodCallNode c : parent.getCalls()) {
            if (c.equals(child)) {
                return parent;
            }
            MethodCallNode methodCallNode = confirmChild(c, child);
            if (methodCallNode != null) {
                return methodCallNode;
            }
        }
        return null;
    }

    private void findCommonParents(Map<Integer, MethodCallNode> res, MethodCallNode root, MethodCallNode childA, MethodCallNode childB, int i, int low) {
        for (MethodCallNode child : root.getCalls()) {
            if (child.equals(childA) || child.equals(childB)) {
                res.put(i, root);
            }
        }
    }

    private Map<MethodCallNode, List<MethodCallNode>> searchForRoots(Set<MethodCallNode> modifiedFunction, Set<MethodCallNode> callSet) {
        // find roots, the key is modifiedFunction, value
        Map<MethodCallNode, List<MethodCallNode>> rootCallsMap = new HashMap<>(modifiedFunction.size());
        for (MethodCallNode m : modifiedFunction) {
            callSet.stream().filter(c -> c.equals(m)).forEach(c -> findRoots(c, c, rootCallsMap, modifiedFunction));
        }
        // L: f, i, e
        // D: a
        // J: i, a
        // 排除子树独立存在的情况, 可以排除子树D，只保留L和J
        for (MethodCallNode methodCallNode : rootCallsMap.keySet()) {
            for (MethodCallNode compareMethodCallNode : rootCallsMap.keySet()) {
                if (compareMethodCallNode.equals(methodCallNode)) {
                    continue;
                }
                if (rootCallsMap.get(methodCallNode).containsAll(rootCallsMap.get(compareMethodCallNode))) {
                    rootCallsMap.remove(compareMethodCallNode);
                }
            }
        }
        return rootCallsMap;
    }

    /**
     * for example, the rootCallsMap contains:
     * L: f, i, e
     * D: a
     * J: i, a
     * @param current the node to be search from
     * @param leaf the modified function
     * @param rootCallsMap the result, key is the leaf, value is the roots
     * @param modifiedFunction the modified function set
     */
    private void findRoots(MethodCallNode current, MethodCallNode leaf, Map<MethodCallNode, List<MethodCallNode>> rootCallsMap, Set<MethodCallNode> modifiedFunction) {
        if (current.getCallers().isEmpty()) {
            if (rootCallsMap.containsKey(current)) {
                rootCallsMap.get(current).add(leaf);
            } else {
                rootCallsMap.put(current, Collections.singletonList(leaf));
            }
            return;
        }
        Set<MethodCallNode> callers = current.getCallers();
        for (MethodCallNode p : callers) {
            if (modifiedFunction.contains(p)) {
                modifiedFunction.remove(leaf);
                break;
            }
            findRoots(p, leaf, rootCallsMap, modifiedFunction);
        }
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
                        modifiedFunction.add(new MethodCallNode(parser.getPackageName(), filePath, methodDeclaration, parser.getOldFilePath()));
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
            JavaFileParser parser = new JavaFileParser(file.getAbsolutePath(), normalFile.getOldPath());
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
