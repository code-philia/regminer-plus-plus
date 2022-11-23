package com.fudan.annotation.platform.backend.service.impl;

import com.fudan.annotation.platform.backend.core.Migrator;
import com.fudan.annotation.platform.backend.core.ProbDD;
import com.fudan.annotation.platform.backend.core.SourceCodeManager;
import com.fudan.annotation.platform.backend.dao.RegressionMapper;
import com.fudan.annotation.platform.backend.entity.DeltaDebugResult;
import com.fudan.annotation.platform.backend.entity.HunkEntity;
import com.fudan.annotation.platform.backend.entity.Regression;
import com.fudan.annotation.platform.backend.entity.Revision;
import com.fudan.annotation.platform.backend.service.DeltaDebuggingService;
import com.fudan.annotation.platform.backend.util.GitUtil;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DeltaDebuggingServiceImpl implements DeltaDebuggingService {
    @Autowired
    private RegressionMapper regressionMapper;
    @Autowired
    private ProbDD probDD;
    @Autowired
    private SourceCodeManager sourceCodeManager;
    @Autowired
    private Migrator migrator;

    @Override
    public DeltaDebugResult getDeltaDebuggingResults(String regressionUuid, String userToken, List<Integer> stepRange, List<Double> cProb, List<Integer> cProbLeftIdx2Test) throws IOException {
        Regression regressionTest = regressionMapper.getRegressionInfo(regressionUuid);
        // get projectFile
        File projectDir = sourceCodeManager.getProjectDir(regressionTest.getProjectFullName());
        // checkout
        List<Revision> revisionList = checkoutDD(regressionTest, projectDir, userToken);
        File ricDir = revisionList.stream().filter(revision -> revision.getRevisionName().equals("bic")).collect(Collectors.toList()).get(0).getLocalCodeDir();
        List<HunkEntity> hunks = GitUtil.getHunksBetweenCommits(projectDir, regressionTest.getBic(), regressionTest.getWork());
//
        long startTime = System.currentTimeMillis();
        return probDD.probDD(ricDir.toString(), hunks, regressionTest.getTestcase(), stepRange, cProb,cProbLeftIdx2Test);
//        return probDD.runDeltaDebugging(regressionTest, projectDir, revisionList, stepRange, cProb, cProbLeftIdx2Test);
    }

//    @Override
//    public DDStep runDeltaDebuggingByStep(String regressionUuid, String userToken, int runTimes) {
//    }

    public List<Revision> checkoutDD(Regression regressionTest, File projectDir, String userToken) {
        List<Revision> targetCodeVersions = new ArrayList<>(4);
        Revision rfc = new Revision("bfc", regressionTest.getBfc());
        targetCodeVersions.add(rfc);
        targetCodeVersions.add(new Revision("buggy", regressionTest.getBuggy()));
        targetCodeVersions.add(new Revision("bic", regressionTest.getBic()));
        targetCodeVersions.add(new Revision("work", regressionTest.getWork()));

        targetCodeVersions.forEach(revision -> {
            revision.setLocalCodeDir(sourceCodeManager.checkout(revision, projectDir, regressionTest.getRegressionUuid(), userToken));
        });
        targetCodeVersions.remove(0);
        migrator.migrateTestAndDependency(rfc, targetCodeVersions, regressionTest.getTestcase());
        return targetCodeVersions;
    }

}
