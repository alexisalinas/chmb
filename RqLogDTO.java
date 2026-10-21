package com.banamex.cops.getofferlistbykey.dto.logging;

import com.banamex.cops.getofferlistbykey.dto.RqHeaderDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entrada del subproceso LogRequest (BW: RqLog = RqHeader + RqBody any). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RqLogDTO {

    @NotNull
    @Valid
    private RqHeaderDTO rqHeader;

    /** Cualquier request (ej. UpdateOfferRq). */
    @NotNull
    private Object rqBody;
}
