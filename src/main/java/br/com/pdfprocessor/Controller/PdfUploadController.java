package br.com.pdfprocessor.Controller;

import br.com.pdfprocessor.dto.StatusResponseDTO;
import br.com.pdfprocessor.dto.UploadResponseDTO;
import br.com.pdfprocessor.model.ProcessamentoPdf;
import br.com.pdfprocessor.Service.PdfProcessamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pdf")
@Tag(name = "Processamento de PDF", description = "Endpoints para upload e consulta de status do processamento de PDFs")
public class PdfUploadController {

    @Autowired
    private PdfProcessamentoService service;

    @Operation(summary = "Realiza o upload de um arquivo PDF para iniciar o processamento",
            description = "Recebe um arquivo PDF, o salva no S3, cria um registro no banco de dados e enfileira a tarefa de OCR no SQS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload bem-sucedido, processamento iniciado",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UploadResponseDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "Nenhum arquivo enviado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor durante o upload", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ProcessamentoPdf processamento = service.iniciarProcessamento(file);

            UploadResponseDTO response = new UploadResponseDTO(
                    processamento.getId(),
                    processamento.getStatus().toString(),
                    "Arquivo recebido. O processamento foi iniciado."
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Consulta o status de um processamento de PDF",
            description = "Retorna o status atual (AGUARDANDO, PROCESSANDO, CONCLUIDO, ERRO) e o texto extraído, se já estiver concluído.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status do processamento retornado com sucesso",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StatusResponseDTO.class)) }),
            @ApiResponse(responseCode = "404", description = "ID de processamento não encontrado", content = @Content)
    })
    @GetMapping("/status/{id}")
    public ResponseEntity<StatusResponseDTO> consultarStatus(@PathVariable UUID id) {
        Optional<ProcessamentoPdf> processamentoOpt = service.consultarStatus(id);

        return processamentoOpt.map(processamento -> {
            StatusResponseDTO response = new StatusResponseDTO(
                    processamento.getId(),
                    processamento.getStatus().toString(),
                    processamento.getTextoExtraido()
            );
            return ResponseEntity.ok(response);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
