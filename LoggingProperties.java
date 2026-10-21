package com.banamex.cops.getofferlistbykey.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Equivale a las GlobalVariables de BW (COP/Logging/AuditPointsEnabled, ApplicationName, etc.). */
@Data
@Component
@ConfigurationProperties(prefix = "update-offer.logging")
public class LoggingProperties {

    /** BW: substring(AuditPointsEnabled,1,1)="1" -> primer caracter igual a "1". */
    private String auditPointsEnabled = "0";
    private String applicationName;
    private String applicationCsiId;
    private String componentName;
}
