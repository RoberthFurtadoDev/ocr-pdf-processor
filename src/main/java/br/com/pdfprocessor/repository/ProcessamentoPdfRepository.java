package br.com.pdfprocessor.repository;

import br.com.pdfprocessor.model.ProcessamentoPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessamentoPdfRepository extends JpaRepository<ProcessamentoPdf, UUID> {
}