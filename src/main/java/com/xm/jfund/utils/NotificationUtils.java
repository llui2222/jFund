/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.xm.jfund.utils;

import com.xm.jfund.application.JFund;
import com.xm.jfund.riskgroup.RiskGroupWithExpFactor;
import jxmUtils.EmailCredentials.EmailCredentials;
import jxmUtils.EmailSender;
import jxmUtils.Functionals;
import jxmUtils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nmichael
 */
public final class NotificationUtils {

    public enum NotificationLevel {
        EMAIL_ONLY,
        FULL
    }

    private static final Logger sLogger = LoggerFactory.getLogger(NotificationUtils.class);

    private static final String sAppName = "jFund";
    private static final EmailCredentials sJFundEmailCreds = EmailCredentials.create("jFund@trading-point.com", "secretPassword");
    private static final List<String> sJFundAdmins = Arrays.asList("acleanthous", "smavrocostas", "mpapakokkinou", "vgeorgiou", "coquinn");
    private static final List<String> sJFundEmailList = sJFundAdmins.stream()
        .map(admin -> String.format("%s@xm.com", admin))
        .collect(Functionals.toArrayList(sJFundAdmins.size()));

    private static final String sSubject = String.format("Message from App: '%s'.", sAppName);

    private static void notifyAdminsImpl(final String msg, final NotificationLevel notificationLevel) {
        EmailSender.sendEmail(
            sJFundEmailCreds,
            sJFundEmailList,
            sSubject,
            false,
            msg);

        if (notificationLevel == NotificationLevel.FULL) {

            sJFundAdmins.forEach(admin -> EmailSender.sendSMSToUser(admin, msg));
        }
    }

    public static void notifyAdmins(final NotificationLevel notificationLevel, final String msg) {
        notifyAdminsImpl(msg, notificationLevel);
    }

    public static void notifyAdmins(final NotificationLevel notificationLevel, final String msg, final Throwable ex) {
        notifyAdminsImpl(StringUtils.makeErrorMessage(msg, ex), notificationLevel);
    }

    public static void notifyAdminsRateLimited(final NotificationLevel notificationLevel, final LocalDateTime lastSendTime, final int rateIntervalMinutes, final String message) {

        final boolean timeToNotify = lastSendTime.compareTo(LocalDateTime.now().minusMinutes(rateIntervalMinutes)) < 0;

        if (timeToNotify) {
            final String messageWithDisclaimer = String.format(sRateLimitedTemplate, message, rateIntervalMinutes);
            notifyAdmins(notificationLevel, messageWithDisclaimer);
        }
    }

    public static void printRunInformation(final Map<Integer, StrategyExecutionInfo> strategyToExecutionInfoMap, final Map<Integer, List<RiskGroupWithExpFactor>> strategyToRiskGroupMap, final Set<Integer> tradeDisabledStrategies, final Class<JFund> sc) {

        sLogger.info("Strategy Information:");
        strategyToExecutionInfoMap.
            forEach((strategyId, executionInfo) -> sLogger.info("Strategy Id: {} | Taker Login: {} | Taker Name: {} | Trade volume threshold: {} | Indicator Threshold: {} | Appetite factor: {} | Risk Group: {}",
                strategyId,
                executionInfo.getTakerLogin(),
                executionInfo.getTakerName(),
                executionInfo.getTradeVolumeThreshold(),
                executionInfo.getIndicatorThreshold(),
                executionInfo.getStrategyWeight(),
                strategyToRiskGroupMap.get(strategyId).toString())
            );

        if (!tradeDisabledStrategies.isEmpty()) {
            final String disabledStrategiesErrorMessage = String.format("JFund - Warning: Starting the application with the following strategies that have disabled trading: %s", tradeDisabledStrategies.toString());
            sLogger.warn(disabledStrategiesErrorMessage);
            NotificationUtils.notifyAdmins(NotificationLevel.FULL, disabledStrategiesErrorMessage);
        }
    }

    private static final String sRateLimitedTemplate = "%s If the problem persists you will receive one of these e-mails every %d minutes.";
}
