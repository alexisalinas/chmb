import org.springframework.stereotype.Component;

/**
 * Equivalente al Assign "InitTZ" de BW/Common/TimeZoneValidation.
 *
 * XPath original:
 *   $GetVariable/root/LookUp[SystemName=$Start/root/System
 *                        and $Start/root/SysMode=Mode
 *                        and $Start/root/Type=Type]/<campo>
 */
@Component
public class InitTZ {

    public TimeZoneValidationFormats execute(TimeZoneValidationStartRoot start,
                                             TimeZoneValidationGetVariableRoot getVariable) {

        TimeZoneValidationGetVariableLookUp lookUp = getVariable.getLookUp().stream()
                .filter(l -> start.getSystem().equals(l.getSystemName())
                          && start.getSysMode().equals(l.getMode())
                          && start.getType().equals(l.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe LookUp para system=" + start.getSystem()
                        + ", sysMode=" + start.getSysMode()
                        + ", type=" + start.getType()));

        return TimeZoneValidationFormats.builder()
                .timeOffset(lookUp.getTimeOffset())
                .dateFormat(lookUp.getDateFormat())
                .timeFormat(lookUp.getTimeFormat())
                .dateTimeFormat(lookUp.getDateTimeFormat())
                .timeZone(lookUp.getTimeZone())
                .build();
    }
}
