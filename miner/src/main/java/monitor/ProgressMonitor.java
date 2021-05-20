package monitor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import constant.Conf;
import model.PotentialRFC;
import utils.FileUtilx;

public class ProgressMonitor {

	public static Set<String> doneTaskList;

	public static void load() {
		doneTaskList = FileUtilx.readSetFromFile(Conf.PROJECT_PATH + File.separator + "progress.details");
	}

	public static void rePlan(List<PotentialRFC> pRFCs) {
		FileUtilx.log("已完成: " + doneTaskList.size());
		Iterator<PotentialRFC> iterator = pRFCs.iterator();
		while (iterator.hasNext()) {
			PotentialRFC pRfc = iterator.next();
			if (doneTaskList.contains(pRfc.getCommit().getName())) {
				iterator.remove();
			}
		}
		FileUtilx.log("剩余: " + pRFCs.size());
	}

	@SuppressWarnings("deprecation")
	public static void addDone(String name) {
		try {
			FileUtils.writeStringToFile(new File(Conf.PROJECT_PATH + File.separator + "progress.details"), name + "\n",
					true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
