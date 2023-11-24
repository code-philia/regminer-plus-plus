package regminer.start;

import org.builder.EnvConfigLoader;
import org.builder.api.ProjectBuilder;
import org.builder.model.CompileResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import regminer.constant.Conf;
import regminer.git.provider.Provider;
import regminer.miner.migrate.BFCEvaluator;
import regminer.utils.FileUtilx;

import java.io.File;

public class CompileProject {//This java file is used to compile all commits of a given project

    public static void main(String[] args) throws Exception {
        String moduleDir = ConfigLoader.getModuleAbsDir("miner");
        System.out.println(moduleDir);
        EnvConfigLoader.setConfigPath(moduleDir + ConfigLoader.SEPARATOR + "miner.properties");// set to your own properties path!
        EnvConfigLoader.refresh();
        ProjectBuilder projectBuilder = new ProjectBuilder();//use BaseCompileAndTest strategy

        ConfigLoader.setConfigPath(moduleDir + ConfigLoader.SEPARATOR + "env.properties");
        ConfigLoader.refresh();
        Repository repo = new Provider().create(Provider.EXISITING).get(Conf.LOCAL_PROJECT_GIT);
        Git git = new Git(repo);
        Iterable<RevCommit> commits = git.log().all().call();

        BFCEvaluator evaluator = new BFCEvaluator(repo);
        int commitCnt = 0;
        int successCnt = 0;
        for (RevCommit commit : commits) {
            commitCnt++;
            String commitId = commit.getName();
            File checkedOutDir = evaluator.checkout(commitId, commitId, "commit");
            projectBuilder.setProjectDir(checkedOutDir);
            CompileResult result = projectBuilder.compile();
            evaluator.emptyCache(commitId);

            if (result.getState() == CompileResult.CompileState.SUCCESS) {
                successCnt++;
            }
            System.out.println(result.getState() + ": " + commitId);
            FileUtilx.log(result.getState() + ": " + commitId);
            System.out.println("Now: " + successCnt + " of " + commitCnt);
            FileUtilx.log("Now: " + successCnt + " of " + commitCnt);
        }

        System.out.println(commitCnt + " commits!");
        System.out.println(successCnt + " successes!");
        FileUtilx.log(commitCnt + " commits!");
        FileUtilx.log(successCnt + " successes!");
    }
}
