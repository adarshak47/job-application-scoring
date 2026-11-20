
package com.adarsh.job_application_scoring.application.question.dto;

import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionFieldIdentifierType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionFieldAliasesRequest(

        @NotBlank
        String questionKey,

        @NotEmpty
        List<@Valid FieldAliasRequest> fieldAliases

) {

    public record FieldAliasRequest(

            @NotNull
            ApplicationQuestionFieldIdentifierType identifierType,

            @NotBlank
            @Size(max = 255)
            String fieldIdentifier

    ) {
    }
}
