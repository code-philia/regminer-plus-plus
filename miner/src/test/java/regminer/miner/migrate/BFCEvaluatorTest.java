package regminer.miner.migrate;

import junit.framework.TestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import regminer.constant.Conf;
import regminer.git.provider.Provider;
import regminer.miner.HeuristicBfcDetector;
import regminer.model.*;
import regminer.start.ConfigLoader;
import regminer.start.Miner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BFCEvaluatorTest extends TestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ConfigLoader.refresh();
        Miner.repo = new Provider().create(Provider.EXISITING).get(Conf.LOCAL_PROJECT_GIT);
        Miner.git = new Git(Miner.repo);
    }

    public void testEvolute() {
        try {
            Iterable<RevCommit> commits = Miner.git.log().all().call();
            String commitStr = "0a2e94323a642cc224b1b97ce28e87aaf073408e";
            RevCommit commit = null;
            for (RevCommit c : commits) {
                if (!c.getName().equals(commitStr)) {
                    continue;
                }
                commit = c;
            }
//            ObjectId newId1 = Miner.repo.resolve(commitStr);
//            if (newId1 != null) {
//                RevWalk revWalk = new RevWalk(Miner.repo);
//                commit = revWalk.parseCommit(newId1);
//            }
            HeuristicBfcDetector detector = new HeuristicBfcDetector(Miner.repo, Miner.git);
            List<ChangedFile> files = detector.getLastDiffFiles(commit);
            List<NormalFile> normalJavaFiles = detector.getNormalJavaFiles(files);
            List<SourceFile> sourceFiles = detector.getSourceFiles(files);
            // 1）若所有路径中存在任意一个路径包含test相关的Java文件则我们认为本次提交中包含测试用例。
            // 2）若所有路径中除了测试用例还包含其他的非测试用例的Java文件则commit符合条件
            PotentialRFC pRFC = new PotentialRFC(commit);
            pRFC.setNormalJavaFiles(normalJavaFiles);
            pRFC.setSourceFiles(sourceFiles);
            List<PotentialTestCase> pls = new ArrayList<>();
            PotentialTestCase potentialTestCase = new PotentialTestCase(1);
            detector.savePotentialTestFile(files,commit, potentialTestCase);
            String testFile = "src\\test\\java\\org\\apache\\commons\\jexl3\\parser\\JexlParser_ESTest.java";
            String file = "D:\\repo\\regminer-plus-plus\\miner\\temp\\apache_commons-jexl\\0a2e94323a642cc224b1b97ce28e87aaf073408e\\JexlParser_ESTest.java";
            potentialTestCase.fileMap.put(testFile, new File(file));
            pRFC.setTargetMethod("declarePragma");
            List<TestFile> testFiles = new ArrayList<>(){{
                add(new TestFile(testFile));
            }};
            potentialTestCase.setTestFiles(testFiles);
            potentialTestCase.setSourceFiles(sourceFiles);
            pRFC.setTestCaseFiles(testFiles);
            pls.add(potentialTestCase);
            pRFC.setPotentialTestCaseList(pls);
            pRFC.setTestcaseFrom(PotentialRFC.TESTCASE_FROM_DIFF_TEST);

            BFCEvaluator tm = new BFCEvaluator(Miner.repo);
            ConcurrentLinkedQueue<PotentialRFC> linkedQueue = new ConcurrentLinkedQueue<>();
            tm.evolute(pRFC);
        } catch (Exception e) {

        }
    }
}