package br.com.pdfprocessor.Controller;

import br.com.pdfprocessor.dto.StatusResponseDTO;
import br.com.pdfprocessor.dto.UploadResponseDTO;
import br.com.pdfprocessor.model.ProcessamentoPdf;
import br.com.pdfprocessor.Service.PdfProcessamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pdf")
public class PdfUploadController {

    @Autowired
    private PdfProcessamentoService service;

    @PostMapping("/upload")
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
