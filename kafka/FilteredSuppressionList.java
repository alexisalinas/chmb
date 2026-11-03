package com.banamex.cops.update_offer.dto.suppression;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Salida del mapper CheckIfSuppressed: Results* es una lista. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilteredSuppressionList {

    @NotNull
    @Valid
    @Builder.Default
    private List<FilteredSuppressionResult> results = new ArrayList<>();
}
