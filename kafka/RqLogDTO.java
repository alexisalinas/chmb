package com.banamex.cops.update_offer.dto.log;

import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entrada del subproceso LogRequest: RqHeader (obligatorio) + RqBody (any, obligatorio). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RqLogDTO {

    @NotNull
    @Valid
    private RqHeaderDTO rqHeader;

    /** Cualquier request (ej. UpdateOfferRequest). */
    @NotNull
    private Object rqBody;
}
