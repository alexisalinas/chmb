package com.banamex.cops.update_offer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOfferRequest {

    // --- Campos Obligatorios (*) ---
    @NotNull(message = "channelCode es obligatorio")
    private String channelCode; 

    @NotNull(message = "campaignId es obligatorio")
    @Size(min = 1, max = 20, message = "campaignId debe tener entre 1 y 20 caracteres")
    private String campaignId; 

    @NotNull(message = "waveId es obligatorio")
    @Size(min = 1, max = 20, message = "waveId debe tener entre 1 y 20 caracteres")
    private String waveId; 

    @NotNull(message = "customerId es obligatorio")
    @Size(min = 1, max = 20, message = "customerId debe tener entre 1 y 20 caracteres")
    private String customerId; 

    // --- Campos Opcionales ---
    private KeyProcessEnum keyProcess; // processVersion en BW. Enum: V1, V2, V3

    @Size(min = 1, max = 35)
    private String accountNumber; 

    @Size(min = 1, max = 19)
    private String cardNumber; 

    private String offerSource; 

    @Size(min = 1, max = 5)
    private String offerPriority; 

    private String assignedBy; 
    private String assignedTo; 
    private String departmentId; 
    private String branchId; 
    private String appointmentDate; 
    private String appointmentStartTime; 
    private String appointmentEndTime; 
    private String offeredNumber; 

    @Size(min = 1, max = 2)
    private String offerStatus; 

    private String reasonCode; 
    private String reasonDescription; 
    private String amount; 
    private String offerActionCode; 
    private String comments; 

    // (?s) para que "." acepte saltos de línea. Aquí viajan los userField1..100
    @Pattern(regexp = "(?s)^\\{.*\\}$", message = "userText debe ser un JSON válido")
    private String userText; 

    private String contactIndicatorNumber; 
    private String customerReferenceNumber; 
    private String giftCode; 
    private String giftConfirmationNumber; 
    private String fulfillmentTrackerFlag; 
    private String levelCode; 

    @Size(min = 1, max = 3)
    private String schemaVersion; 

    private String workgroupId; 
    private String agentName; 
    private String divisionCode; 
    private String branchRegionIdentifier; 
    private String areaIdentifier; 
    private String contactType; 
    private String offerCategory; 
    private String branchName; 
    private String purpose; 
    private String departmentName; 
    private String responseChannel; 
    private String customerEmailAddress; 
    private String fulfillmentCode; 

    /**
     * Lista de ControlField (solo contextType). Antes: controlField.
     */
    @Valid
    private List<ControlFieldDTO> contextType;
}