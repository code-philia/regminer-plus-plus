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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Song Rui
 */
public abstract class BfcDetector {
    Repository repo;
    Git git;

    public BfcDetector(Repository repo, Git git) {
        this.repo = repo;
        this.git = git;
    }

    public void setRepo(Repository repo) {
        this.repo = repo;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    /**
     * 获取发生了修改的文件
     * @param entry 变更信息
     * @param files 所有文件
     * @throws Exception
     */
    void getChangedFile(DiffEntry entry, List<ChangedFile> files) throws Exception{
        String path = entry.getNewPath();
        if (path.contains("test") && path.endsWith(".java")) {
            ChangedFile file = new TestFile(entry.getNewPath());
            file.setOldPath(entry.getOldPath());
            file.setEditList(getEdits(entry));
            files.add(file);
        }
        if ((!path.contains("test")) && path.endsWith(".java")) {
            ChangedFile file = new NormalFile(entry.getNewPath());
            file.setOldPath(entry.getOldPath());
            file.setEditList(getEdits(entry));
            files.add(file);
        }

//      if not end with ".java",it may be source file
        if (!path.endsWith(".java")) {
            ChangedFile file = new SourceFile(entry.getNewPath());
            file.setOldPath(entry.getOldPath());
            file.setEditList(getEdits(entry));
            files.add(file);
        }
    }

    /**
     * 筛选出可能的BFC commit
     * @param commit 待判断commit
     * @param potentialRFCs 已确认为BFC的列表
     * @throws Exception
     */
    abstract void detect(RevCommit commit, List<PotentialRFC> potentialRFCs) throws Exception;


    public List<PotentialRFC> detectPotentialBFC() throws Exception {
        // 获取所有的commit，我们需要对所有的commit进行分析
        Iterable<RevCommit> commits = git.log().all().call();
        // 开始迭代每一个commit
        return detectAll(commits);
    }

    public List<PotentialRFC> detectPotentialBFC(List<String> commitsFilter) throws Exception {
        // 获取所有的commit，我们需要对所有的commit进行分析
        Iterable<RevCommit> commits = git.log().all().call();
        List<PotentialRFC> potentialRFCS = detectOnFilter(commitsFilter, commits);
        return potentialRFCS;
    }

    private List<PotentialRFC> detectAll(Iterable<RevCommit> commits) throws Exception {
        List<PotentialRFC> potentialRFCs = new LinkedList<PotentialRFC>();
        // 定义需要记录的实验数据
        int countAll = 0;
        // 开始迭代每一个commit
        for (RevCommit commit : commits) {
            detect(commit, potentialRFCs);
            countAll++;
        }
        FileUtilx.log("总共分析了" + countAll + "条commit\n");
        FileUtilx.log("pRFC in total :" + potentialRFCs.size());
        return potentialRFCs;
    }

    List<PotentialRFC> detectOnFilter(List<String> commitsFilter, Iterable<RevCommit> commits) throws Exception {
        List<PotentialRFC> potentialRFCs = new LinkedList<PotentialRFC>();
        // 定义需要记录的实验数据
        int countAll = 0;
        // 开始迭代每一个commit
        for (RevCommit commit : commits) {
            if (commitsFilter.contains(commit.getName())) {
                detect(commit, potentialRFCs);
                countAll++;
            }
        }
        FileUtilx.log("总共分析了" + countAll + "条commit\n");
        FileUtilx.log("pRFC in total :" + potentialRFCs.size());
        return potentialRFCs;
    }


    /**
     * 获取与父亲的差别
     *
     * @param commit
     * @return
     * @throws Exception
     */
    public List<ChangedFile> getLastDiffFiles(RevCommit commit) throws Exception {
        List<ChangedFile> files = new LinkedList<>();
        ObjectId id = commit.getTree().getId();
        ObjectId oldId;
        if (commit.getParentCount() > 0) {
            oldId = commit.getParent(0).getTree().getId();
        } else {
            return files;
        }
        return getChangedFiles(files, id, oldId);
    }

    List<Edit> getEdits(DiffEntry entry) throws Exception {
        List<Edit> result = new LinkedList<Edit>();
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repo);
            FileHeader fileHeader = diffFormatter.toFileHeader(entry);
            List<? extends HunkHeader> hunkHeaders = fileHeader.getHunks();
            for (HunkHeader hunk : hunkHeaders) {
                result.addAll(hunk.toEditList());
            }
        }
        return result;

    }

    /**
     * 任意两个diff之间的文件路径差别
     *
     * @param oldCommit
     * @param newCommit
     * @return
     * @throws Exception
     */
    List<ChangedFile> getDiffFiles(RevCommit oldCommit, RevCommit newCommit) throws Exception {
        List<ChangedFile> files = new LinkedList<>();
        ObjectId id = newCommit.getTree().getId();
        ObjectId oldId = oldCommit.getTree().getId();
        return getChangedFiles(files, id, oldId);
    }

    List<ChangedFile> getChangedFiles(List<ChangedFile> files, ObjectId id, ObjectId oldId) throws Exception {
        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldId);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, id);
            // finally get the list of changed files
            List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
            for (DiffEntry entry : diffs) {
                getChangedFile(entry, files);
            }
        }
        return files;
    }


    /**
     * 获取所有测试用例文件
     *
     * @param files
     * @return
     */
    List<TestFile> getTestFiles(List<ChangedFile> files) {
        List<TestFile> testFiles = new LinkedList<>();
        if (files == null) {
            return testFiles;
        }
        for (ChangedFile file : files) {
            if (file instanceof TestFile) {
                testFiles.add((TestFile) file);
            }
        }
        return testFiles;
    }

    /**
     * 获取所有普通文件
     */
    public List<NormalFile> getNormalJavaFiles(List<ChangedFile> files) {
        List<NormalFile> normalJavaFiles = new LinkedList<>();
        for (ChangedFile file : files) {
            if (file instanceof NormalFile) {
                normalJavaFiles.add((NormalFile) file);
            }
        }
        return normalJavaFiles;
    }

    public List<SourceFile> getSourceFiles(List<ChangedFile> files) {
        List<SourceFile> sourceFiles = new LinkedList<>();
        for (ChangedFile file : files) {
            if (file.getNewPath().contains("pom.xml") || file.getNewPath().equals(Constant.NONE_PATH)) {
                continue;
            }
            if (file instanceof SourceFile) {
                sourceFiles.add((SourceFile) file);
            }
        }
        return sourceFiles;
    }

    /**
     * 判断全部都是普通的Java文件
     *
     * @param files
     * @return
     */
    boolean justNormalJavaFile(List<ChangedFile> files) {
        for (ChangedFile file : files) {
            String str = file.getNewPath().toLowerCase();
            // 如果有一个文件路径中不包含test
            // 便立即返回false
            if (str.contains("test")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 如果一个程序中仅包含了fix但没有测试用例，那么我们将在(-3,+3)中检索是否有单独的测试用例被提交
     *
     * @param commit
     * @return
     * @throws Exception
     */
    List<PotentialTestCase> findTestCommit(RevCommit commit) throws Exception {
        List<PotentialTestCase> potentialTestCases = new ArrayList<>();
        RevWalk revWalk = new RevWalk(repo);
        // 树结构 ^2 ^1 c ～1 ～2
        // c^1
        ObjectId newId1 = repo.resolve(commit.getName() + "~1");
        RevCommit newRev1 = null;
        if (newId1 != null) {
            newRev1 = revWalk.parseCommit(newId1);
            List<ChangedFile> files = getDiffFiles(commit, newRev1);
            getPotentialTestCase(files,newRev1,1,potentialTestCases);
        }

        // c^2
        ObjectId newId2 = repo.resolve(commit.getName() + "~2");
        RevCommit newRev2 = null;
        if (newId1 != null && newId2 != null) {
            newRev2 = revWalk.parseCommit(newId2);
            List<ChangedFile> files = getDiffFiles(newRev1, newRev2);
            // 是否只有测试用例
            getPotentialTestCase(files,newRev2,2,potentialTestCases);
        }
        // c~1
        int num = commit.getParentCount();
        if (num > 1) {
            List<ChangedFile> files = getDiffFiles(commit.getParent(1), commit.getParent(0));
            getPotentialTestCase(files,null,-1,potentialTestCases);
            num--;
        }
        // c~2
        if (num > 1) {
            List<ChangedFile> files = getDiffFiles(commit.getParent(1), commit.getParent(0));
            getPotentialTestCase(files,null,-2,potentialTestCases);
            num--;
        }

        return potentialTestCases;
    }

    private void getPotentialTestCase(List<ChangedFile> files, RevCommit commit, int index, List<PotentialTestCase> potentialTestCaseList) throws Exception {
        if (!justChangeTestFileOnly(files)) {
            return;
        }
        PotentialTestCase potentialTestCase = new PotentialTestCase(index);
        List<TestFile> testFiles = getTestFiles(files);
        List<SourceFile> sourceFiles =getSourceFiles(files);

        potentialTestCase.setTestFiles(testFiles);
        potentialTestCase.setSourceFiles(sourceFiles);

        if (index > 0) {
            savePotentialTestFile(files,commit, potentialTestCase);
        }
        potentialTestCaseList.add(potentialTestCase);

    }

    public void savePotentialTestFile(List<ChangedFile> files, RevCommit commit, PotentialTestCase potentialTestCase) {
        for (ChangedFile changedFile : files) {
            String filePath = changedFile.getNewPath();
            if (!filePath.equals(Constant.NONE_PATH)) {
                File testFile = new File(Conf.TMP_FILE + File.separator+commit.getName()+File.separator+ filePath);
                try {
                    FileUtils.writeStringToFile(testFile, GitUtil.getContextWithFile(repo, commit, filePath));
                    potentialTestCase.fileMap.put(filePath, testFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 判断是否只有测试文件，如果所有的修改文件路径都包含test，认为所有的 被修改文件只与测试用例有关
     *
     * @param files
     * @return
     */
    private boolean justChangeTestFileOnly(List<ChangedFile> files) {
        int num = 0;
        int num_1 =0;
        for (ChangedFile file : files) {
            String str = file.getNewPath().toLowerCase();
            if (!str.contains("test") && str.endsWith(".java")) {
                num++;
            }
            if (str.endsWith(".java")){
                num_1++;
            }

        }
        return (num == 0 && num_1>0);
    }
}
