package com.fudan.annotation.platform.backend.core;

/*
 *
 *  * Copyright 2021 SongXueZhi
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import org.apache.maven.model.*;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class JacocoMavenManager {
    final static String GROUP_ID = "org.jacoco";
    final static String ARTIFACT_ID = "jacoco-maven-plugin";
    final static String VERSION = "0.8.5";

    public void addJacocoFeatureToMaven(File bfcDir) throws Exception {
        //FIXME SongXuezhi judge whether exits, if true do nothing
        MavenManager mvnManager = new MavenManager();
        File pomFile = new File(bfcDir, "pom.xml");
        Model pomModel = mvnManager.getPomModel(pomFile);
        convertToHttps(pomModel);
        removeJacocoIfExist(pomModel);
        addJacocoDependency(pomModel);
        addJacocoPlugin(pomModel);
        mvnManager.saveModel(pomFile, pomModel);
    }

    private void convertToHttps(Model pomModel) {
        List<Repository> pluginRepos = pomModel.getPluginRepositories();
        for (Repository pluginRepo : pluginRepos) {
            String url = pluginRepo.getUrl();
            String[] split = url.split(":");
            if (split[0].equalsIgnoreCase("http")) {
                pluginRepo.setUrl("https:" + split[1]);
            }
        }
        List<Repository> repos = pomModel.getRepositories();
        for (Repository repo: repos) {
            String url = repo.getUrl();
            String[] split = url.split(":", 2);
            if (split[1].equalsIgnoreCase("//repo2.maven.org/maven2/")) {
                split[1] = "//repo1.maven.org/maven2/";
            }
            if (split[0].equalsIgnoreCase("http")) {
                repo.setUrl("https:" + split[1]);
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(repo.getUrl()).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    if (connection.getResponseCode() >= 300) {
                        throw new ProtocolException();
                    }
                } catch (Exception e) {
                    repo.setUrl(url);
                }
            }
        }
    }

    private void removeJacocoIfExist(Model pomModel) {
        Build build = pomModel.getBuild();
        if (build == null)
            return;
        List<Plugin> plugins = build.getPlugins();
        List<Plugin> toRemove = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin.getArtifactId().contains("jacoco")) {
                toRemove.add(plugin);
            } else if (plugin.getArtifactId().contains("animal-sniffer")) {
                plugin.setVersion("1.16");
            }
        }

        for (Plugin plugin: toRemove) {
            build.removePlugin(plugin);
        }
    }


    private void addJacocoPlugin(Model pomModel) {

        Plugin plugin = new Plugin();
        plugin.setGroupId(GROUP_ID);
        plugin.setArtifactId(ARTIFACT_ID);
        plugin.setVersion(VERSION);

        PluginExecution pluginExecution1 = new PluginExecution();
        pluginExecution1.addGoal("prepare-agent");

        PluginExecution pluginExecution2 = new PluginExecution();
        pluginExecution2.setId("report");
        pluginExecution2.setPhase("test");
        pluginExecution2.addGoal("report");

        plugin.addExecution(pluginExecution1);
        plugin.addExecution(pluginExecution2);

        Build build = pomModel.getBuild();
        if (build == null) {
            pomModel.setBuild(new Build());
            build = pomModel.getBuild();
        }
        build.addPlugin(plugin);

        Map<String, Plugin> map = build.getPluginsAsMap();
        Plugin surefirePlugin = map.get("org.apache.maven.plugins:maven-surefire-plugin");

        if (surefirePlugin != null) {
            Plugin newPlugin = new Plugin();
            newPlugin.setArtifactId(surefirePlugin.getArtifactId());
            newPlugin.setVersion(surefirePlugin.getVersion());
            build.removePlugin(surefirePlugin);
            build.addPlugin(newPlugin);
        }
    }

    private void addJacocoDependency(Model pomModel) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(GROUP_ID);
        dependency.setArtifactId(ARTIFACT_ID);
        dependency.setVersion(VERSION);
        dependency.setType("maven-plugin");
        pomModel.getDependencies().add(dependency);
    }

}