package br.com.pdfprocessor.Service;

import br.com.pdfprocessor.model.ProcessamentoPdf;
import br.com.pdfprocessor.model.StatusProcessamento;
import br.com.pdfprocessor.repository.ProcessamentoPdfRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SqsListenerService {

    private static final Logger logger = LoggerFactory.getLogger(SqsListenerService.class);

    @Autowired
    private ProcessamentoPdfRepository repository;

    @Autowired
    private TextractClient textractClient;

    @Value("${app.aws.s3.bucket-name}")
    private String nomeBucket;

    @SqsListener("${app.aws.sqs.queue-name}")
    public void escutarFilaPdf(String idProcessamentoStr) {
        logger.info("Mensagem recebida da fila SQS: {}", idProcessamentoStr);

        UUID idProcessamento = UUID.fromString(idProcessamentoStr);
        Optional<ProcessamentoPdf> processamentoOpt = repository.findById(idProcessamento);

        if (processamentoOpt.isEmpty()) {
            logger.error("Processamento com ID {} não encontrado no banco de dados.", idProcessamento);
            return;
        }

        ProcessamentoPdf processamento = processamentoOpt.get();

        try {
            processamento.setStatus(StatusProcessamento.PROCESSANDO);
            repository.save(processamento);

            String textoExtraido = chamarTextract(nomeBucket, processamento.getUrlArquivo());

            logger.info("Análise do Textract concluída com sucesso para o arquivo: {}", processamento.getNomeArquivoOriginal());

            processamento.setTextoExtraido(textoExtraido);
            processamento.setStatus(StatusProcessamento.CONCLUIDO);
            repository.save(processamento);

        } catch (Exception e) {
            logger.error("Erro grave ao processar o PDF com ID {} usando Textract: {}", idProcessamento, e.getMessage());
            e.printStackTrace();
            processamento.setStatus(StatusProcessamento.ERRO);
            repository.save(processamento);
        }
    }

    private String chamarTextract(String bucket, String chaveS3) throws InterruptedException {
        StartDocumentTextDetectionRequest startRequest = StartDocumentTextDetectionRequest.builder()
                .documentLocation(DocumentLocation.builder()
                        .s3Object(S3Object.builder()
                                .bucket(bucket)
                                .name(chaveS3)
                                .build())
                        .build())
                .build();

        StartDocumentTextDetectionResponse startResponse = textractClient.startDocumentTextDetection(startRequest);
        String jobId = startResponse.jobId();
        logger.info("Trabalho do Textract iniciado com JobId: {}", jobId);

        GetDocumentTextDetectionResponse getResponse;
        String jobStatus;
        do {
            Thread.sleep(5000);
            GetDocumentTextDetectionRequest getRequest = GetDocumentTextDetectionRequest.builder().jobId(jobId).build();
            getResponse = textractClient.getDocumentTextDetection(getRequest);
            jobStatus = getResponse.jobStatusAsString();
            logger.info("Status atual do JobId {}: {}", jobId, jobStatus);
        } while (jobStatus.equals(JobStatus.IN_PROGRESS.toString()));

        if (!jobStatus.equals(JobStatus.SUCCEEDED.toString())) {
            throw new RuntimeException("Falha no processamento do Textract. Status final: " + jobStatus);
        }

        StringBuilder textoCompleto = new StringBuilder();
        List<Block> blocos = getResponse.blocks();
        for (Block bloco : blocos) {
            if (bloco.blockType() == BlockType.LINE) {
                textoCompleto.append(bloco.text()).append("\n");
            }
        }

        String nextToken = getResponse.nextToken();
        while (nextToken != null) {
            GetDocumentTextDetectionRequest nextPageRequest = GetDocumentTextDetectionRequest.builder().jobId(jobId).nextToken(nextToken).build();
            getResponse = textractClient.getDocumentTextDetection(nextPageRequest);
            for (Block bloco : getResponse.blocks()) {
                if (bloco.blockType() == BlockType.LINE) {
                    textoCompleto.append(bloco.text()).append("\n");
                }
            }
            nextToken = getResponse.nextToken();
        }

        return textoCompleto.toString();
    }
}