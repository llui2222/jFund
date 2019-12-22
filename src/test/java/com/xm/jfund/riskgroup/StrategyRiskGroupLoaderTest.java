package com.xm.jfund.riskgroup;

import com.xm.jfund.utils.RiskGroupWeightInfo;
import jAnalystUtils.riskGroups.AccountsRiskGroup;
import jAnalystUtils.riskGroups.RiskGroup;
import jxmUtils.TupleModule.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StrategyRiskGroupLoaderTest {

    @Test
    public void testGetNameToRiskGroup() {
        final String groupName = "name1";
        final List<RiskGroup> riskGroups = getRiskGroups(groupName);
        final Map<String, RiskGroup> nameToRiskGroup = StrategyRiskGroupLoader.getNameToRiskGroup(riskGroups);

        final RiskGroup one = nameToRiskGroup.get(groupName);

        assertTrue(nameToRiskGroup.size() == 1);
        assertTrue(one.getGroupName().equals(groupName));
    }

    @Test
    public void testGetRiskGroupWithExpFactorList() {
        final String groupName = "name1";
        final List<RiskGroup> riskGroups = getRiskGroups(groupName);
        final Map<String, RiskGroup> nameToRiskGroup = StrategyRiskGroupLoader.getNameToRiskGroup(riskGroups);
        final List<RiskGroupWeightInfo> listOfRiskGroupWeightInfo = getListOfRiskGroupWeightInfo();

        final List<RiskGroupWithExpFactor> riskGroupWithExpFactorList = StrategyRiskGroupLoader.getRiskGroupWithExpFactorList(listOfRiskGroupWeightInfo, nameToRiskGroup);
        final List<RiskGroupWithExpFactor> sorted = riskGroupWithExpFactorList.stream().sorted(Comparator.comparingDouble(RiskGroupWithExpFactor::getExposureFactor)).collect(Collectors.toList());

        final RiskGroupWithExpFactor riskGroupWithExpFactor = sorted.get(0);

        assertTrue(sorted.size() == 2);

        assertTrue(riskGroupWithExpFactor.getExposureFactor() >= 1.0 && riskGroupWithExpFactor.getExposureFactor() < 2.0);
        assertNull(riskGroupWithExpFactor.getRiskGroup());

        final RiskGroupWithExpFactor riskGroupWithExpFactorTwo = sorted.get(1);

        assertTrue(riskGroupWithExpFactorTwo.getExposureFactor() >= 2.0 && riskGroupWithExpFactorTwo.getExposureFactor() < 3.0);
        assertNotNull(riskGroupWithExpFactorTwo.getRiskGroup());
    }

    @Test
    public void testGetDistinctGroupNames() {

        final Map<Integer, List<RiskGroupWeightInfo>> map = getOverlappingStratToRiskGroupMap();
        final List<String> distinctGroupNames = StrategyRiskGroupLoader.getDistinctGroupNames(map);
        final List<String> sorted = distinctGroupNames.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());

        assertTrue(sorted.size() == 2);
        assertTrue(sorted.get(0).equals("name1"));
        assertTrue(sorted.get(1).equals("name2"));
    }

    private List<RiskGroup> getRiskGroups(final String riskGroupName) {
        final List<Pair<Integer, Long>> serverLogin = new ArrayList<>();
        final Pair<Integer, Long> pair = Pair.create(1, 123L);
        serverLogin.add(pair);
        final List<RiskGroup> groups = new ArrayList<>();
        groups.add(getAccountRiskGroup(riskGroupName, serverLogin));
        return groups;
    }

    private AccountsRiskGroup getAccountRiskGroup(final String name, final List<Pair<Integer, Long>> serversLogins) {
        return AccountsRiskGroup.create(name, serversLogins);
    }

    private Map<Integer, List<RiskGroupWeightInfo>> getOverlappingStratToRiskGroupMap() {
        final int stratIdOne = 1;
        final int stratIdTwo = 2;

        final Map<Integer, List<RiskGroupWeightInfo>> strategyToGroup = new HashMap<>();
        final List<RiskGroupWeightInfo> list = getListOfRiskGroupWeightInfo();
        strategyToGroup.put(stratIdOne, list);
        strategyToGroup.put(stratIdTwo, list);
        return strategyToGroup;
    }

    private List<RiskGroupWeightInfo> getListOfRiskGroupWeightInfo() {
        final RiskGroupWeightInfo infoOne = RiskGroupWeightInfo.create("name1", 2.0, true);
        final RiskGroupWeightInfo infoTwo = RiskGroupWeightInfo.create("name2", 1.0, false);
        final List<RiskGroupWeightInfo> list = new ArrayList<>();
        list.add(infoOne);
        list.add(infoTwo);
        return list;
    }
}