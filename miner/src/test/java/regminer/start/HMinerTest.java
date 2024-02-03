package regminer.start;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Ignore;
import org.junit.Test;
import regminer.constant.Conf;
import regminer.git.provider.Provider;
import regminer.miner.BfcDetector;
import regminer.miner.HeuristicBfcDetector;
import regminer.miner.PotentialBFCDetector;
import regminer.model.MethodCallNode;
import regminer.model.PotentialRFC;
import regminer.model.PotentialTestCase;
import regminer.model.SourceFile;
import regminer.testsuite.RegMinerTest;
import regminer.utils.FileUtilx;
import regminer.utils.GitUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HMinerTest extends RegMinerTest {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Miner.repo = new Provider().create(Provider.EXISITING).get(Conf.LOCAL_PROJECT_GIT);
        Miner.git = new Git(Miner.repo);
    }

    @Test
    public void regressionTest() throws Exception {
        List<String> filter = new ArrayList<>();
        // if check rule test case
//        filter.add("13ae28e80ac17dea57cd299ae8b7a49b25ef35c9");
        filter.add("960fa3152d0b2e983522456937c9d47e8d24a7d9");
        BfcDetector pBFCDetector = new HeuristicBfcDetector(Miner.repo, Miner.git);
        Miner.pRFCs = null;
        Miner.pRFCs = pBFCDetector.detectPotentialBFC(filter);
        Miner.singleThreadHandle();
    }

    @Test
    public void generateTestCasesTest() throws Exception {
        String commit = "960fa3152d0b2e983522456937c9d47e8d24a7d9";
        HeuristicBfcDetector pBFCDetector = new HeuristicBfcDetector(Miner.repo, Miner.git);
        Miner.pRFCs = null;
        RevCommit newRev = null;
        ObjectId newId1 = Miner.repo.resolve(commit);
        if (newId1 != null) {
            RevWalk revWalk = new RevWalk(Miner.repo);
            newRev = revWalk.parseCommit(newId1);
        }
        if (newRev == null) {
            return;
        }
        String packageName = "com.alibaba.fastjson.support.spring";
        String filePath = "D:\\repo\\miner_space\\repos\\regminer\\fastjson\\meta\\src\\main\\java\\com\\alibaba\\fastjson\\support\\spring\\FastJsonHttpMessageConverter.java";
        MethodCallNode targetMethod = new MethodCallNode(packageName, filePath, new MethodDeclaration(), filePath);
        targetMethod.setMethodName("getType");
        List<PotentialTestCase> pls = pBFCDetector.generateTestCases(new PotentialRFC(newRev), newRev, targetMethod, new LinkedList<>());
        if (pls.isEmpty()) {

        }
    }

    @Test
    public void generateCommonsJexlTestCasesTest() throws Exception {
        String commit = "0a2e94323a642cc224b1b97ce28e87aaf073408e";
        HeuristicBfcDetector pBFCDetector = new HeuristicBfcDetector(Miner.repo, Miner.git);
        Miner.pRFCs = null;
        RevCommit newRev = null;
        ObjectId newId1 = Miner.repo.resolve(commit);
        if (newId1 != null) {
            RevWalk revWalk = new RevWalk(Miner.repo);
            newRev = revWalk.parseCommit(newId1);
        }
        if (newRev == null) {
            return;
        }
        String packageName = "org.apache.commons.jexl3.parser";
        String filePath = "D:\\repo\\miner_space\\repos\\regminer\\apache_commons-jexl\\meta\\src\\main\\java\\org\\apache\\commons\\jexl3\\parser\\JexlParser.java";
        MethodCallNode targetMethod = new MethodCallNode(packageName, filePath, new MethodDeclaration(), filePath);
        targetMethod.setMethodName("declarePragma");
        targetMethod.setClassName("org.apache.commons.jexl3.parser.JexlParser");
//        List<PotentialTestCase> pls = pBFCDetector.generateTestCases(new PotentialRFC(newRev), newRev, targetMethod, new LinkedList<>());
        List<PotentialTestCase> pls = pBFCDetector.generateTestCasesWithEvosuite(new PotentialRFC(newRev), targetMethod, new LinkedList<>());

        if (pls.isEmpty()) {

        }
    }
}
