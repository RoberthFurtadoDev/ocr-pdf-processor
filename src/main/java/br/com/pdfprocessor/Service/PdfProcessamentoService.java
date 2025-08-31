package br.com.pdfprocessor.Service;

import br.com.pdfprocessor.model.ProcessamentoPdf;
import br.com.pdfprocessor.model.StatusProcessamento;
import br.com.pdfprocessor.repository.ProcessamentoPdfRepository;
import io.awspring.cloud.s3.S3Template;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class PdfProcessamentoService {

    @Autowired
    private ProcessamentoPdfRepository repository;

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private S3Template s3Template;

    @Value("${app.aws.sqs.queue-name}")
    private String nomeFila;

    @Value("${app.aws.s3.bucket-name}")
    private String nomeBucket;

    public ProcessamentoPdf iniciarProcessamento(MultipartFile arquivo) throws IOException {
        String nomeArquivoOriginal = arquivo.getOriginalFilename();
        String chaveS3 = UUID.randomUUID().toString() + "_" + nomeArquivoOriginal;

        s3Template.upload(nomeBucket, chaveS3, arquivo.getInputStream());

        ProcessamentoPdf novoProcessamento = new ProcessamentoPdf();
        novoProcessamento.setStatus(StatusProcessamento.AGUARDANDO);
        novoProcessamento.setNomeArquivoOriginal(nomeArquivoOriginal);
        novoProcessamento.setUrlArquivo(chaveS3);

        ProcessamentoPdf processamentoSalvo = repository.save(novoProcessamento);

        sqsTemplate.send(nomeFila, processamentoSalvo.getId().toString());

        return processamentoSalvo;
    }

    /**
     * Consulta o status de um processamento de PDF pelo seu ID.
     * @param id O UUID do processamento.
     * @return Um Optional contendo a entidade ProcessamentoPdf se encontrada.
     */
    public Optional<ProcessamentoPdf> consultarStatus(UUID id) {
        return repository.findById(id);
    }
}
