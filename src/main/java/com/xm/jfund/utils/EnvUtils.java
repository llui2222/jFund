package com.xm.jfund.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class EnvUtils {

    private static final String ENV_ACTIVE_PROFILE = "JFUND_ACTIVE_PROFILE";
    private static final String DEFAULT_PROFILE = "default";

    public static String getActiveProfile() {
        return Optional.ofNullable(System.getenv(ENV_ACTIVE_PROFILE)).orElse(DEFAULT_PROFILE);
    }

    public static String getProfileConfigurationFileName(final String profile) {
        if (StringUtils.isNotBlank(profile)) {
            if (isDefaultProfile(profile)) {
                return "conf/jFund.properties";
            }
            return String.format("conf/jFund-%s.properties", profile);
        }
        throw new IllegalArgumentException("Active profile cannot be empty");
    }

    public static boolean isDefaultProfile(final String profile) {
        return DEFAULT_PROFILE.equals(profile);
    }
}
