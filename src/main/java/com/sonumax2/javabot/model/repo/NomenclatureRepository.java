package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.reference.Nomenclature;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface NomenclatureRepository extends CrudRepository<Nomenclature, Long> {
    List<Nomenclature> findByActiveTrueOrderByNameAsc();
    Optional<Nomenclature> findFirstByActiveTrueAndNameNorm(String nameNorm);
    List<Nomenclature> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String q);
    List<Nomenclature> findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(Long chatId);

    List<Nomenclature> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String q, Pageable p);
    List<Nomenclature> findByActiveTrueOrderByNameAsc(Pageable p);
    List<Nomenclature> findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(Long chatId, Pageable p);
}
