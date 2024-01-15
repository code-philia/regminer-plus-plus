package regminer.utils;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;

public class GitUtil {
	protected static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
	protected String REPO_PATH;
	protected Repository repository;
	protected RevWalk revWalk;
	protected Git git;
	protected static final String FORMAT = "yyyy-MM-dd HH:mm:ss";
	protected static final long TO_MILLISECOND = 1000L;
	protected static final int COMMIT_SIZE = 1000;

	public static String getContextWithFile(Repository repo, RevCommit commit, String filePath) throws Exception {
		RevWalk walk = new RevWalk(repo);
		RevTree revTree = commit.getTree();
		TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, revTree);
		// 文件名错误
		if (treeWalk == null)
			return null;

		ObjectId blobId = treeWalk.getObjectId(0);
		ObjectLoader loader = repo.open(blobId);
		byte[] bytes = loader.getBytes();
		if (bytes != null)
			return new String(bytes);
		return null;

	}

	public GitUtil(String repoPath) {
		this.REPO_PATH = repoPath;
		String gitDir = IS_WINDOWS ? repoPath + "\\.git" : repoPath + "/.git";
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		try {
			this.repository = ((FileRepositoryBuilder)((FileRepositoryBuilder)((FileRepositoryBuilder)builder.setGitDir(new File(gitDir))).readEnvironment()).findGitDir()).build();
			this.git = new Git(this.repository);
			this.revWalk = new RevWalk(this.repository);
		} catch (IOException var5) {
			var5.printStackTrace();
		}

	}

	public String getRepoPath() {
		return this.REPO_PATH;
	}

	public void checkout(String commit) {
		try {
			if (commit == null) {
				commit = this.repository.getBranch();
			}

			this.git.reset().setMode(ResetCommand.ResetType.HARD).call();
			CheckoutCommand checkoutCommand = this.git.checkout();
			checkoutCommand. setName(commit).call();
		} catch (JGitInternalException var3) {
			var3.printStackTrace();
			try {
				String str = var3.getCause().toString().substring(var3.getCause().toString().indexOf("C:"));
				int br = str.indexOf(" ");
				git.rm().addFilepattern(str.substring(0, str.indexOf(" ", br+1))).call();
				this.git.add().addFilepattern(".").call();
				this.git.stashCreate().call();
				this.git.add().addFilepattern(".").call();
				this.git.stashCreate().call();
				this.git.reset().setMode(ResetCommand.ResetType.HARD).setRef("master").call();

				checkout(commit);
			} catch (GitAPIException e) {
				e.printStackTrace();
				FileUtilx.log("before error commit=: " + commit);
				FileUtilx.log("JGit checkout error:{} " + var3.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				this.git.reset().setMode(ResetCommand.ResetType.HARD).setRef("master").call();
				this.git.add().addFilepattern(".").call();
				this.git.stashCreate().call();
				checkout(commit);
			} catch (GitAPIException gitAPIException) {
				gitAPIException.printStackTrace();
			}
		}

	}

}
