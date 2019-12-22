package com.xm.jfund.exposures;

import com.xm.jfund.riskgroup.RiskGroupWithExpFactor;
import com.xm.jfund.utils.StrategyParameters;
import com.xm.jfund.utils.StrategySymbolLimits;
import com.xm.jfund.utils.StrategySymbolLimits.TYPE;
import jAnalystUtils.SymbolModule.SymbolMetaData;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExposuresCollectorTest {
    private final Map<Integer, Set<SymbolMetaData>> mockStrategySymbolsMetadata = new HashMap<>();

    private final StrategySymbolLimits mockStrategySymbolLimits_A_STANDARD =
        StrategySymbolLimits.create(1, 0, "A", 0, 0, 0, 0, 0, 0, 0, 0, 0);
    private final StrategySymbolLimits mockStrategySymbolLimits_A_CATCH_UP =
        StrategySymbolLimits.create(1, 1, "A", 0, 0, 0, 0, 0, 0, 0, 0, 0);
    private final StrategySymbolLimits mockStrategySymbolLimits_B_CATCH_UP =
        StrategySymbolLimits.create(1, 1, "B", 0, 0, 0, 0, 0, 0, 0, 0, 0);

    private final Map<String, Map<TYPE, StrategySymbolLimits>> mockSymbolLimitsSymbolPair = new HashMap<>();

    private final Map<TYPE, StrategySymbolLimits> mockSymbolLimitsTypePair_A = new HashMap<>();
    private final Map<TYPE, StrategySymbolLimits> mockSymbolLimitsTypePair_B = new HashMap<>();

    @Before
    public void setupStrategyParametersSymbolLimitTypeTest() {
        // Initialize the mock StrategySymbolsMetadata
        // object as it is needed by ExposureCollector object
        mockStrategySymbolsMetadata.put(1, new HashSet<>());
    }

    @Test
    public void testStrategyParametersExecutionTypeInitializationWithValidValues() {
        mockSymbolLimitsSymbolPair.clear();
        mockSymbolLimitsTypePair_A.clear();

        mockSymbolLimitsTypePair_A.put(
            mockStrategySymbolLimits_A_STANDARD.getType(), mockStrategySymbolLimits_A_STANDARD);
        mockSymbolLimitsTypePair_A.put(
            mockStrategySymbolLimits_A_CATCH_UP.getType(), mockStrategySymbolLimits_A_CATCH_UP);

        mockSymbolLimitsSymbolPair.put("A", mockSymbolLimitsTypePair_A);

        final StrategyParameters mockStrategyParameters =
            getMockStrategyParameters(mockSymbolLimitsSymbolPair);

        final Map<Integer, StrategyParameters> mockStrategyParametersMap =
            getMockStrategyParametersMap(mockStrategyParameters);

        final ExposuresCollector mockExposuresCollector =
            getMockExposureCollector(mockStrategyParametersMap);

        /* Check if the initialization defaults to CATCH_UP Execution Type
        as a CATCH_UP Strategy Symbol Limit exists in Symbol A*/
        assertEquals(TYPE.CATCH_UP, mockExposuresCollector.getCurrentExecutionType(1, "A"));

        assertFalse(mockExposuresCollector.isCurrentlyStandardExecution(1, "A"));

        // Check if the execution type has switched to STANDARD for Symbol as instructed
        mockExposuresCollector.setCurrentExecutionTypeToStandard(1, "A");
        assertEquals(TYPE.STANDARD, mockExposuresCollector.getCurrentExecutionType(1, "A"));

        /* Check that if we invoke the method again for the switch to STANDARD
        for a particular symbol nothing changes*/
        mockExposuresCollector.setCurrentExecutionTypeToStandard(1, "A");
        assertEquals(TYPE.STANDARD, mockExposuresCollector.getCurrentExecutionType( 1, "A"));

        assertTrue(mockExposuresCollector.isCurrentlyStandardExecution(1, "A"));
    }

    // A StrategyParameter must always contain a STANDARD Strategy Symbol Limit type
    @Test(expected = RuntimeException.class)
    public void testStrategyParametersExecutionTypeInitializationWithNonValidValues() {
        mockSymbolLimitsSymbolPair.clear();
        mockSymbolLimitsTypePair_B.clear();

        mockSymbolLimitsTypePair_B.put(
            mockStrategySymbolLimits_B_CATCH_UP.getType(), mockStrategySymbolLimits_B_CATCH_UP);

        mockSymbolLimitsSymbolPair.put("B", mockSymbolLimitsTypePair_B);

        final StrategyParameters mockStrategyParameters =
            getMockStrategyParameters(mockSymbolLimitsSymbolPair);

        final Map<Integer, StrategyParameters> mockStrategyParametersMap =
            getMockStrategyParametersMap(mockStrategyParameters);

        getMockExposureCollector(mockStrategyParametersMap);
    }

    @Test
    public void testStrategyParameterWithSymbolOnlyContainingStandardType() {
        mockSymbolLimitsSymbolPair.clear();
        mockSymbolLimitsTypePair_A.clear();

        mockSymbolLimitsTypePair_A.put(
            mockStrategySymbolLimits_A_STANDARD.getType(), mockStrategySymbolLimits_A_STANDARD);

        mockSymbolLimitsSymbolPair.put("A", mockSymbolLimitsTypePair_A);

        final StrategyParameters mockStrategyParameters =
            getMockStrategyParameters(mockSymbolLimitsSymbolPair);

        final Map<Integer, StrategyParameters> mockStrategyParametersMap =
            getMockStrategyParametersMap(mockStrategyParameters);

        final ExposuresCollector mockExposuresCollector =
            getMockExposureCollector(mockStrategyParametersMap);

        assertEquals(TYPE.STANDARD, mockExposuresCollector.getCurrentExecutionType(1, "A"));
    }

    @Test
    public void testErrorMessage() {
        final double volume = 1.0;
        final double sumOfExposures = 2.0;
        final double contractSize = 10000.0;
        final double netJfundCoverage = 2000.0;

        final String message = ExposuresCollector.getErrorMessageForBadVolume(volume, sumOfExposures, contractSize, netJfundCoverage, 1);

        assertTrue(message.equals("JFUND, Something is wrong with our calculations. We need to investigate. Strategy: 1, Order volume: 1.000000, Sum of exposures: 2.000000, Net jfund coverage: 2000.000000, symbol contract size: 10000.000000"));
    }

    @Test
    public void testGetAccurateVolume() {
        final double volume = 4.14;
        final double contractSize = 100_000;

        final ExposuresCollector exposuresCollector = getMockExposureCollector(new HashMap<>());
        final BigDecimal accurateVolume = exposuresCollector.getAccurateVolume(volume, contractSize);

        assertTrue(Double.compare(accurateVolume.doubleValue(), 414000) == 0);
    }

    private StrategyParameters getMockStrategyParameters(
        final Map<String, Map<TYPE, StrategySymbolLimits>> mockSymbolLimitsSymbolPair) {
        final RiskGroupWithExpFactor mockRiskGroupWithExpFactor = RiskGroupWithExpFactor.create(null, 0.0);
        final List<RiskGroupWithExpFactor> mockRiskGroupWithExpFactors = new ArrayList<>();
        mockRiskGroupWithExpFactors.add(mockRiskGroupWithExpFactor);

        return StrategyParameters.create(null, mockRiskGroupWithExpFactors, 0.0, mockSymbolLimitsSymbolPair, null, null);
    }

    private Map<Integer, StrategyParameters> getMockStrategyParametersMap(
        final StrategyParameters mockStrategyParameters) {
        final Map<Integer, StrategyParameters> mockStrategyParametersMap = new HashMap<>();
        mockStrategyParametersMap.put(1, mockStrategyParameters);
        return mockStrategyParametersMap;
    }

    private ExposuresCollector getMockExposureCollector(final Map<Integer, StrategyParameters> mockStrategyParametersMap) {
        return ExposuresCollector.create(
            null,
            mockStrategyParametersMap,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            mockStrategySymbolsMetadata,
            null,
            false);
    }
}
