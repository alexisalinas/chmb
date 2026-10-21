import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el campo de control.
 * Contiene los atributos explícitos desde userField1 hasta userField100.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Omite los campos nulos al enviar el JSON
public class ControlFieldDTO {

    private String contextType; // example: controlField

    // --- Campos de usuario (userField1 a userField100) ---
    private String userField1;
    private String userField2;
    private String userField3;
    private String userField4;
    private String userField5;
    private String userField6;
    private String userField7;
    private String userField8;
    private String userField9;
    private String userField10;

    private String userField11;
    private String userField12;
    private String userField13;
    private String userField14;
    private String userField15;
    private String userField16;
    private String userField17;
    private String userField18;
    private String userField19;
    private String userField20;

    private String userField21;
    private String userField22;
    private String userField23;
    private String userField24;
    private String userField25;
    private String userField26;
    private String userField27;
    private String userField28;
    private String userField29;
    private String userField30;

    private String userField31;
    private String userField32;
    private String userField33;
    private String userField34;
    private String userField35;
    private String userField36;
    private String userField37;
    private String userField38;
    private String userField39;
    private String userField40;

    private String userField41;
    private String userField42;
    private String userField43;
    private String userField44;
    private String userField45;
    private String userField46;
    private String userField47;
    private String userField48;
    private String userField49;
    private String userField50;

    private String userField51;
    private String userField52;
    private String userField53;
    private String userField54;
    private String userField55;
    private String userField56;
    private String userField57;
    private String userField58;
    private String userField59;
    private String userField60;

    private String userField61;
    private String userField62;
    private String userField63;
    private String userField64;
    private String userField65;
    private String userField66;
    private String userField67;
    private String userField68;
    private String userField69;
    private String userField70;

    private String userField71;
    private String userField72;
    private String userField73;
    private String userField74;
    private String userField75;
    private String userField76;
    private String userField77;
    private String userField78;
    private String userField79;
    private String userField80;

    private String userField81;
    private String userField82;
    private String userField83;
    private String userField84;
    private String userField85;
    private String userField86;
    private String userField87;
    private String userField88;
    private String userField89;
    private String userField90;

    private String userField91;
    private String userField92;
    private String userField93;
    private String userField94;
    private String userField95;
    private String userField96;
    private String userField97;
    private String userField98;
    private String userField99;
    private String userField100;
}