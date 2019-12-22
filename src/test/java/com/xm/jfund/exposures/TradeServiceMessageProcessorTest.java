package com.xm.jfund.exposures;

import com.xm.jfund.client.trade.model.Exposure;
import com.xm.jfund.utils.StrategyExecutionInfo;
import com.xm.jfund.utils.StrategyParameters;
import com.xm.jfund.utils.StrategySymbolLimits;
import com.xm.jfund.utils.StrategySymbolLimits.TYPE;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TradeServiceMessageProcessorTest {

    @Test
    public void testCreateSequenceNumberMap() {
        final String usdjpy = "USDJPY";
        final String eurusd = "EURUSD";
        final String audcad = "AUDCAD";

        final Map<Integer, StrategyParameters> stratParams = getStratParams();
        final Map<String, Map<String, Long>> sequenceNumberMap = TradeServiceMessageProcessor.createSequenceNumberMap(stratParams);
        final Map<String, Long> symbolToSequence1 = sequenceNumberMap.get("3002:2");
        final Map<String, Long> symbolToSequence2 = sequenceNumberMap.get("3003:3");

        assertThat(symbolToSequence1.get(usdjpy)).isEqualTo(0);
        assertThat(symbolToSequence1.size()).isEqualTo(1);
        assertThat(symbolToSequence2.get(eurusd)).isEqualTo(0);
        assertThat(symbolToSequence2.get(audcad)).isEqualTo(0);
        assertThat(symbolToSequence2.size()).isEqualTo(2);
    }

    private Map<Integer, StrategyParameters> getStratParams() {
        final String usdjpy = "USDJPY";
        final String eurusd = "EURUSD";
        final String audcad = "AUDCAD";
        final StrategyParameters mockStrategyParameters1 = mock(StrategyParameters.class);
        final StrategyParameters mockStrategyParameters2 = mock(StrategyParameters.class);

        final Map<String, Map<TYPE, StrategySymbolLimits>> symbolLimits1 = new HashMap<>();
        symbolLimits1.put(usdjpy, new HashMap<>());
        final Map<String, Map<TYPE, StrategySymbolLimits>> symbolLimits2 = new HashMap<>();
        symbolLimits2.put(eurusd, new HashMap<>());
        symbolLimits2.put(audcad, new HashMap<>());

        final StrategyExecutionInfo mockExecutionInfo1 = mock(StrategyExecutionInfo.class);
        final StrategyExecutionInfo mockExecutionInfo2 = mock(StrategyExecutionInfo.class);

        when(mockExecutionInfo1.getTakerLogin()).thenReturn("2");
        when(mockExecutionInfo1.getTakerName()).thenReturn("3002");

        when(mockExecutionInfo2.getTakerLogin()).thenReturn("3");
        when(mockExecutionInfo2.getTakerName()).thenReturn("3003");

        when(mockStrategyParameters1.getExecutionInfo()).thenReturn(mockExecutionInfo1);
        when(mockStrategyParameters2.getExecutionInfo()).thenReturn(mockExecutionInfo2);

        when(mockStrategyParameters1.getSymbolLimits()).thenReturn(symbolLimits1);
        when(mockStrategyParameters2.getSymbolLimits()).thenReturn(symbolLimits2);

        final Map<Integer, StrategyParameters> stratParams = new HashMap<>();
        stratParams.put(1, mockStrategyParameters1);
        stratParams.put(2, mockStrategyParameters2);

        return stratParams;
    }

    @Test
    public void testPopulateTradeSequenceMap() {

        final Exposure usdjpyExposure = Exposure.create(BigDecimal.valueOf(300), BigDecimal.valueOf(400), "USDJPY", "3002", "2", 10L);
        final Exposure eurUsdExposure = Exposure.create(BigDecimal.valueOf(300), BigDecimal.valueOf(400), "EURUSD", "3003", "3", 33L);
        final Exposure audCadExposure = Exposure.create(BigDecimal.valueOf(300), BigDecimal.valueOf(400), "AUDCAD", "3003", "3", 44L);

        final Map<Integer, StrategyParameters> stratParams = getStratParams();
        final Map<String, Map<String, Long>> sequenceNumberMap = TradeServiceMessageProcessor.createSequenceNumberMap(stratParams);

        TradeServiceMessageProcessor.populateTradeSequenceMap(usdjpyExposure.getTakerName(), Collections.singletonList(usdjpyExposure), usdjpyExposure.getTakerLogin(), sequenceNumberMap);
        TradeServiceMessageProcessor.populateTradeSequenceMap(eurUsdExposure.getTakerName(), Collections.singletonList(eurUsdExposure), eurUsdExposure.getTakerLogin(), sequenceNumberMap);
        TradeServiceMessageProcessor.populateTradeSequenceMap(audCadExposure.getTakerName(), Collections.singletonList(audCadExposure), audCadExposure.getTakerLogin(), sequenceNumberMap);

        final Map<String, Long> symbolToSequence1 = sequenceNumberMap.get("3002:2");
        final Map<String, Long> symbolToSequence2 = sequenceNumberMap.get("3003:3");

        assertThat(symbolToSequence1.get(usdjpyExposure.getSymbol())).isEqualTo(10);
        assertThat(symbolToSequence1.size()).isEqualTo(1);
        assertThat(symbolToSequence2.get(eurUsdExposure.getSymbol())).isEqualTo(33);
        assertThat(symbolToSequence2.get(audCadExposure.getSymbol())).isEqualTo(44);
        assertThat(symbolToSequence2.size()).isEqualTo(2);
    }

    @Test
    public void testMakeTakerNameAndLoginKey() {

        assertThat(TradeServiceMessageProcessor.makeTakerNameTakerLoginKey("takerName", "takerLogin")).isEqualTo("takerName:takerLogin");
    }
}