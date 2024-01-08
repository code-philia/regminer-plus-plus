package regminer.start;

import org.eclipse.jgit.api.Git;
import org.junit.Ignore;
import org.junit.Test;
import regminer.constant.Conf;
import regminer.git.provider.Provider;
import regminer.miner.BfcDetector;
import regminer.miner.HeuristicBfcDetector;
import regminer.miner.PotentialBFCDetector;
import regminer.model.PotentialRFC;
import regminer.testsuite.RegMinerTest;
import regminer.utils.FileUtilx;

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
        filter.add("13ae28e80ac17dea57cd299ae8b7a49b25ef35c9");
        BfcDetector pBFCDetector = new HeuristicBfcDetector(Miner.repo, Miner.git);
        Miner.pRFCs = null;
        Miner.pRFCs = (LinkedList<PotentialRFC>) pBFCDetector.detectPotentialBFC(filter);
        Miner.singleThreadHandle();
    }
}
