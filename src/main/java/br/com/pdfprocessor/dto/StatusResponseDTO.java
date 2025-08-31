package br.com.pdfprocessor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatusResponseDTO(
        UUID idProcessamento,
        String status,
        String textoExtraido
) {
}