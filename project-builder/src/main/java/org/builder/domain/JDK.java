package org.builder.domain;

import org.builder.EnvConfigLoader;

public enum JDK {

    J8 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j8File;
        }
    },

    J7 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j7File;

        }
    },

    J11 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j11File;
        }
    },

    J17 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j17File;
        }
    },


    J15 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j15File;
        }
    },
    J9 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j9File;
        }
    },
    J10 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j10File;
        }
    },
    J12 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j12File;
        }
    },
    J13 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j13File;
        }
    },
    J14 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j14File;
        }
    },
    J16 {
        @Override
        public String getCommand() {
            return "export JAVA_HOME=" + EnvConfigLoader.j16File;
        }
    };

    public abstract String getCommand();
}
