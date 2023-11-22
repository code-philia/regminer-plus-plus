package org.builder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

public class ConfigLoader {
    //base_repo_path here is the root module path of the file which invoked this class!
    //eg: a class in miner module invoked this class, then base_repo_path is the root path of miner module, the env.properties file should be in the root path of miner module
    private static final String BASE_REPO_PATH = System.getProperty("user.dir");
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final Properties prop = new Properties();
    private static String CONFIGPATH = "env.properties";
    private final static String JDK_DIR = "jdk_dir";
    private final static String JDK_HOME = "jdk_home";
    private final static String JDK7 = "j7_file";
    private final static String JDK8 = "j8_file";
    private final static String JDK9 = "j9_file";
    private final static String JDK10 = "j10_file";
    private final static String JDK11 = "j11_file";
    private final static String JDK12 = "j12_file";
    private final static String JDK13 = "j13_file";
    private final static String JDK14 = "j14_file";
    private final static String JDK15 = "j15_file";
    private final static String JDK16 = "j16_file";
    private final static String JDK17 = "j17_file";

    public static String jdkDir = "";
    public static String jdkHome = "";
    public static String j7File = "";
    public static String j8File = "";
    public static String j9File = "";
    public static String j10File = "";
    public static String j11File = "";
    public static String j12File = "";
    public static String j13File = "";
    public static String j14File = "";
    public static String j15File = "";
    public static String j16File = "";
    public static String j17File = "";

    public static void setConfigPath(String path) {
        CONFIGPATH = path;
    }

    public static void refresh() {
        File file = new File(CONFIGPATH);
        if (file.exists()) {
            System.out.println("config file exists!");
        } else {
            System.out.println("config file not exists!");
        }
        try (InputStream inStream = Files.newInputStream(Paths.get(CONFIGPATH))) {
            prop.load(inStream);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.out.println("refresh error here!");
        }

        jdkDir = prop.getProperty(JDK_DIR);
        jdkHome = prop.getProperty(JDK_HOME);
        j7File = jdkDir + prop.getProperty(JDK7) + jdkHome;
        j8File = jdkDir + prop.getProperty(JDK8) + jdkHome;
        j9File = jdkDir + prop.getProperty(JDK9) + jdkHome;
        j10File = jdkDir + prop.getProperty(JDK10) + jdkHome;
        j11File = jdkDir + prop.getProperty(JDK11) + jdkHome;
        j12File = jdkDir + prop.getProperty(JDK12) + jdkHome;
        j13File = jdkDir + prop.getProperty(JDK13) + jdkHome;
        j14File = jdkDir + prop.getProperty(JDK14) + jdkHome;
        j15File = jdkDir + prop.getProperty(JDK15) + jdkHome;
        j16File = jdkDir + prop.getProperty(JDK16) + jdkHome;
        j17File = jdkDir + prop.getProperty(JDK17) + jdkHome;

    }

}
