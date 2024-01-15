package regminer.miner;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import regminer.constant.Conf;
import regminer.constant.Constant;
import regminer.model.*;
import regminer.utils.FileUtilx;
import regminer.utils.GitUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PotentialBFCDetector extends BfcDetector {

    public PotentialBFCDetector(Repository repo, Git git) {
        super(repo, git);
    }


    /**
     * @param commit
     * @param potentialRFCs
     * @throws Exception
     */
    @Override
    void detect(RevCommit commit, List<PotentialRFC> potentialRFCs) throws Exception {
        // 1)首先我们将记录所有的标题中包含fix的commti
        String message1 = commit.getFullMessage().toLowerCase();
//        if (message1.contains("fix") || message1.contains("close")) {
        if (true) {
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
            if (testcaseFiles.size() > 0 && normalJavaFiles.size() > 0) {
                PotentialRFC pRFC = new PotentialRFC(commit);
                pRFC.setTestCaseFiles(testcaseFiles);
                pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SELF);
                pRFC.setNormalJavaFiles(normalJavaFiles);
                pRFC.setSourceFiles(sourceFiles);
                potentialRFCs.add(pRFC);
            } else if (justNormalJavaFile(files) && (message1.contains("fix") || message1.contains("close"))) {
//				针对只标题只包含fix但是修改的文件路径中没有测试用例的提交
//				我们将在(c-3,c+3) 的范围内检索可能的测试用例
//				[TODO] songxuezhi
                List<PotentialTestCase> pls = findTestCommit(commit);
                if (pls != null && pls.size() > 0) {
                    PotentialRFC pRFC = new PotentialRFC(commit);
                    pRFC.setNormalJavaFiles(normalJavaFiles);
                    pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_SEARCH);
                    pRFC.setPotentialTestCaseList(pls);
                    potentialRFCs.add(pRFC);
                }
            }
        }
    }
}
