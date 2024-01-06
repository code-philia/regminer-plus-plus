package regminer.miner;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import regminer.model.*;

import java.util.List;

/**
 * @author Song Rui
 */
public class HeuristicBfcDetector extends BfcDetector {

    public HeuristicBfcDetector(Repository repo, Git git) {
        super(repo, git);
    }

    @Override
    void detect(RevCommit commit, List<PotentialRFC> potentialRFCs) throws Exception {
        // 1)首先我们将记录所有的标题中包含fix的commit
        String message1 = commit.getFullMessage().toLowerCase();
        if (!message1.contains("fix") &&  !message1.contains("close")) {
            return;
        }
        // 针对标题包含fix的commit我们进一步分析本次提交修改的文件路径
        List<ChangedFile> files = getLastDiffFiles(commit);
        if (files == null) {
            return;
        }
        List<TestFile> testcaseFiles = getTestFiles(files);
        List<NormalFile> normalJavaFiles = getNormalJavaFiles(files);
        List<SourceFile> sourceFiles = getSourceFiles(files);
        // 1）若所有路径中存在任意一个路径包含test相关的Java文件则我们认为本次提交中包含测试用例。
        // 2）若所有路径中除了测试用例还包含其他的非测试用例的Java文件则commit符合条件
        if (!testcaseFiles.isEmpty() && !normalJavaFiles.isEmpty()) {
            PotentialRFC pRFC = new PotentialRFC(commit);
            pRFC.setTestCaseFiles(testcaseFiles);
            pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SELF);
            pRFC.setNormalJavaFiles(normalJavaFiles);
            pRFC.setSourceFiles(sourceFiles);
            potentialRFCs.add(pRFC);
        } else if (justNormalJavaFile(files)) {
//				针对只标题只包含fix但是修改的文件路径中没有测试用例的提交
//				我们将在(c-3,c+3) 的范围内检索可能的测试用例
//				[TODO] songxuezhi
            List<PotentialTestCase> pls = findTestCommit(commit);
            PotentialRFC pRFC = new PotentialRFC(commit);
            pRFC.setNormalJavaFiles(normalJavaFiles);
            if (pls != null && !pls.isEmpty()) {
                pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SEARCH);
                pRFC.setPotentialTestCaseList(pls);
                potentialRFCs.add(pRFC);
            } else if (hitHeuristicFixingRules(normalJavaFiles)){
                // 使用启发式规则判断是否有可能为BFC，若有可能则尝试自动生成测试用例
                // create test file and generate test cases

                // if generate successfully, add this commit into potentialRFCs list

                pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_DIFF_TEST);
                potentialRFCs.add(pRFC);
            }
        }
    }

    private boolean hitHeuristicFixingRules(List<NormalFile> javaFiles) {

        return false;
    }

}
