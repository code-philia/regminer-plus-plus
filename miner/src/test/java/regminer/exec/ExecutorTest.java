package regminer.exec;

import org.junit.Test;
import regminer.model.MigrateItem;

import java.io.File;

public class ExecutorTest {

	TestExecutor exec = new TestExecutor();

	@Test
	public void testExec() throws Exception {
		exec.setDirectory(new File("C:\\Users\\sxzdh\\Documents\\pcode\\fastjson\\fastjson"));
		boolean cc = exec.execBuildWithResult("mvn compile", false);
		System.out.println(cc);
		exec.execPrintln("git show;mvn compile");
	}

    @Test
    public void testExecWithSpecificTestMethod() throws Exception {
        exec.setDirectory(new File("D:\\repo\\miner_space\\repos\\regminer\\apache_commons-jexl\\cache\\0a2e94323a642cc224b1b97ce28e87aaf073408e\\0a2e94323a642cc224b1b97ce28e87aaf073408e\\bfc_0c033470-c8a5-4560-b7e8-f323e3d9699a\\meta"));
        boolean cc = exec.execBuildWithResult("mvn compile test-compile", false);
        System.out.println(cc);
        exec.execPrintln("git show;mvn compile");
    }

    @Test
    public void testExecTestWithSpecificTestMethod() throws Exception {
        exec.setDirectory(new File("D:\\repo\\miner_space\\repos\\regminer\\apache_commons-jexl\\cache\\0a2e94323a642cc224b1b97ce28e87aaf073408e\\0a2e94323a642cc224b1b97ce28e87aaf073408e\\bfc_0c033470-c8a5-4560-b7e8-f323e3d9699a\\meta"));
        MigrateItem.MigrateFailureType r = exec.execTestWithResult("mvn test -Drat.skip=true -Dtest=org.apache.commons.jexl3.parser.JexlParser_ESTest#test28");
        System.out.println(r);
        exec.execPrintln("git show;mvn compile");
    }

    @Test
    public void setDirectory() {
    }

    @Test
    public void exec() {
    }

    @Test
    public void execPrintln() {
        exec.setDirectory(new File("C:\\Users\\sxzdh\\Documents\\pcode\\fastjson\\fastjson"));
        exec.execPrintln("git show");
    }
}
