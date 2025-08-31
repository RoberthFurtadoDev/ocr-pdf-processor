package br.com.pdfprocessor;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
		info = @Info(
				title = "API do Processador de PDF com OCR",
				version = "1.0",
				description = "API para upload e processamento assíncrono de documentos PDF com extração de texto usando AWS Textract."
		)
)
public class OcrPdfProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(OcrPdfProcessorApplication.class, args);
	}

}
