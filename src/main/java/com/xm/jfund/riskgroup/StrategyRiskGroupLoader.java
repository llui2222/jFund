/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xm.jfund.riskgroup;

import com.xm.jfund.db.JFundDBConnectionProvider;
import com.xm.jfund.utils.RiskGroupWeightInfo;
import com.xm.jfund.utils.StrategyExecutionInfo;
import jAnalystUtils.riskGroups.RiskGroup;
import jAnalystUtils.riskGroups.RiskGroupLoader;
import jAnalystUtils.riskGroups.RiskGroupLoader.NoSuchGroupException;
import jxmUtils.BuildMode;
import jxmUtils.DBUtils;
import jxmUtils.Functionals;
import jxmUtils.StringUtils;
import jxmUtils.TupleModule.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author nmichael
 */
public final class StrategyRiskGroupLoader {

    private static final RiskGroupLoader sRiskGroupLoader = RiskGroupLoader.create(BuildMode.RELEASE);

    private static final String sAntiCoverageGroupNameTemplate = "AntiCoverage";

    /**
     * A later requirement was given to add checks in ExposureCollector that rely on exposures without weights affecting them.
     * Since ExposureCollector only holds total long and short client and antiCoverage exposures, reversing the weights for those checks
     * is only possible if the weights are the same in every client group.
     * This goes directly against being able to set variable weights when declaring client groups.
     *
     * @param strategyToRiskGroupsMap A mapping from the id of each loaded strategy to a list containing the strategy's risk groups (including the custom AntiCoverage group).
     */
    public static void ensureClientRiskGroupsHaveCommonWeight(final Map<Integer, List<RiskGroupWithExpFactor>> strategyToRiskGroupsMap) {

        strategyToRiskGroupsMap.forEach((strategyId, riskGroups) -> {

            final long numberOfDifferentWeightsInClientGroups = riskGroups.stream()
                .map(RiskGroupWithExpFactor::getExposureFactor)
                .distinct()
                .count();

            if (numberOfDifferentWeightsInClientGroups > 1) {
                throw new IllegalStateException(String.format("Client risk groups for strategy: %d have varying weights! \n The risk groups are: %s", strategyId, riskGroups.toString()));
            }
        });
    }

    /**
     * Get distinct group names since there could be duplicate groups among the strategies
     *
     * @param strategyToRiskGroup strategy id to a risk group weight info
     * @return a list of distinct group names among the passed strategy id's
     */
    static List<String> getDistinctGroupNames(final Map<Integer, List<RiskGroupWeightInfo>> strategyToRiskGroup) {
        return strategyToRiskGroup.values().stream()
            .flatMap(listOfInfo -> listOfInfo.stream().map(RiskGroupWeightInfo::getGroupName))
            .distinct().collect(Collectors.toList());
    }

    /**
     * Get risk groups from the list of group names
     * We have to load from the db all the relevant data for each name
     *
     * @param listOfGroupNames list of group names
     * @param loader           loader which loads group details
     * @param connection       to the db
     * @return a list of risk groups from the given names
     */
    private static List<RiskGroup> getRiskGroups(final List<String> listOfGroupNames, final RiskGroupLoader loader, final Connection connection) {
        return listOfGroupNames.stream().map(name -> loadRiskGroup(loader, name, connection)).collect(Collectors.toList());
    }

    /**
     * Get name to risk group map
     *
     * @param groups to create a map from
     * @return map of name to group
     */
    static Map<String, RiskGroup> getNameToRiskGroup(final List<RiskGroup> groups) {
        return groups.stream().map(group -> Pair.create(group.getGroupName(), group)).collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));
    }

    /**
     * Get list of group with exposure factor
     *
     * @param riskGroupWeightInfoList list of group weight info objects
     * @param nameToRiskGroupMap      map of group name to risk group
     * @return list of risk groups with exposure factor
     */
    static List<RiskGroupWithExpFactor> getRiskGroupWithExpFactorList(final List<RiskGroupWeightInfo> riskGroupWeightInfoList, final Map<String, RiskGroup> nameToRiskGroupMap) {
        return riskGroupWeightInfoList.stream()
            .map(info -> {
                final RiskGroup riskGroup = nameToRiskGroupMap.get(info.getGroupName());
                return RiskGroupWithExpFactor.create(riskGroup, info.getWeight());
            })
            .collect(Functionals.toArrayList(riskGroupWeightInfoList.size()));
    }

    public static Map<Integer, List<RiskGroupWithExpFactor>> loadRiskGroupsWithExposureFactor(final Map<Integer, StrategyExecutionInfo> strategyIdToExecutionInfoMap) throws SQLException {

        try (final Connection connectJFund = JFundDBConnectionProvider.getInstance().getConnection()) {

            final Map<Integer, List<RiskGroupWeightInfo>> strategyToRiskGroupsMap = loadRiskGroupNamesWithExposureFactors(strategyIdToExecutionInfoMap.keySet(), connectJFund);

            /*Fetch the RiskGroups once from the DB, since they can be reused from multiple strategies.*/

            final List<String> distinctGroupNames = getDistinctGroupNames(strategyToRiskGroupsMap);
            final List<RiskGroup> riskGroups = getRiskGroups(distinctGroupNames, sRiskGroupLoader, connectJFund);
            final Map<String, RiskGroup> nameToRiskGroupMap = getNameToRiskGroup(riskGroups);

            return strategyToRiskGroupsMap.entrySet().stream()
                .map(entry -> {
                    final Integer strategyId = entry.getKey();
                    final List<RiskGroupWeightInfo> riskGroupWeightInfoList = entry.getValue();

                    final List<RiskGroupWithExpFactor> riskGroupWithExpFactorsList = getRiskGroupWithExpFactorList(riskGroupWeightInfoList, nameToRiskGroupMap);

                    return Pair.create(strategyId, riskGroupWithExpFactorsList);
                })
                .collect(Functionals.toHashMap(Pair::getFirst, Pair::getSecond));
        }
    }

    public static TradingAccountRiskGroup getTradingAccountRiskGroup(final int strategyId, final String taker, final String marginAccount) {

        return TradingAccountRiskGroup.create(getAntiCoverageGroupName(strategyId), taker, marginAccount);
    }

    private static String getAntiCoverageGroupName(final int strategyId) {

        return String.format("%s_%d", sAntiCoverageGroupNameTemplate, strategyId);
    }

    private static Map<Integer, List<RiskGroupWeightInfo>> loadRiskGroupNamesWithExposureFactors(final Set<Integer> strategies, final Connection connectJFund) throws SQLException {

        final String strategiesAsCsv = StringUtils.streamToCsv(strategies.stream(), ",", "(", ")");

        final String selectStrategyGroupInfo = String.format(sSelectRiskGroupInfoTemplate, strategiesAsCsv);

        try (final PreparedStatement pstmtJFund = connectJFund.prepareStatement(selectStrategyGroupInfo)) {

            //jfund risk group weight is always 1
            final double jfundStrategyWeight = 1;
            return DBUtils.executeSelectQuery(pstmtJFund, (ResultSet rs) -> Pair.create(rs.getInt(2), RiskGroupWeightInfo.create(rs.getString(1), jfundStrategyWeight, true)))
                .collect(Functionals.groupingByToHashMap(Pair::getFirst, Collectors.mapping(Pair::getSecond, Functionals.toArrayList())));
        }
    }

    private static RiskGroup loadRiskGroup(final RiskGroupLoader riskGroupLoader, final String groupName, final Connection connectJFund) {

        try {
            return riskGroupLoader.getGroupByName(connectJFund, groupName);
        }
        catch (final NoSuchGroupException | SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final String sSelectRiskGroupInfoTemplate = "select RiskGroups.GroupName, strategy_risk_groups.strategyId "
        + "from fund_strategy.strategy_risk_groups join analyst_application.RiskGroups on strategy_risk_groups.groupId = RiskGroups.GroupId "
        + "where strategy_risk_groups.strategyId in %s ";
}