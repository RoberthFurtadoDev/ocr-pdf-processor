package br.com.pdfprocessor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "processamentos_pdf")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessamentoPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusProcessamento status;

    @Column(nullable = false)
    private String nomeArquivoOriginal;

    private String urlArquivo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String textoExtraido;
}