package br.com.pdfprocessor.dto;

import java.util.UUID;

public record UploadResponseDTO(
        UUID idProcessamento,
        String status,
        String mensagem
) {
}