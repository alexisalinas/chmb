package com.banamex.cops.service.timezonevalidation;

/** Condiciones de las transiciones del flujo OpUpdateOffer. */
public final class UpdateOfferFlowConditions {

    private UpdateOfferFlowConditions() { }

    /** Log request -> RequestTimeZoneValidation: lower-case(TranslateTimeZone)="true". */
    public static boolean translateTimeZone(String translateTimeZoneVar) {
        return "true".equalsIgnoreCase(translateTimeZoneVar);
    }

    /** RequestTimeZoneValidation -> PerformSuppressionCheck: último char de ResponseCode = '0' o = COP_TZCOD_114. */
    public static boolean continueToSuppression(String responseCode) {
        if (responseCode == null || responseCode.isEmpty()) return false;
        return responseCode.charAt(responseCode.length() - 1) == '0'
                || "COP_TZCOD_114".equals(responseCode);
    }
}
