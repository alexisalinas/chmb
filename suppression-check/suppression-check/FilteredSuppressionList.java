package com.banamex.cops.suppression;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Salida del mapper CheckIfSuppressed. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilteredSuppressionList {

    @Valid
    @Builder.Default
    private List<FilteredSuppressionResult> results = new ArrayList<>();
}
