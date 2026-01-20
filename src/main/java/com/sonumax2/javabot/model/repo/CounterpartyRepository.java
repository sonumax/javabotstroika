package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.reference.Counterparty;
import com.sonumax2.javabot.model.reference.CounterpartyKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CounterpartyRepository extends CrudRepository<Counterparty, Long> {

    List<Counterparty> findByActiveTrueOrderByNameAsc();
    List<Counterparty> findByActiveTrueAndKindOrderByNameAsc(CounterpartyKind kind);

    Optional<Counterparty> findFirstByActiveTrueAndNameNorm(String nameNorm);

    Optional<Counterparty> findFirstByActiveTrueAndKindAndNameNorm(CounterpartyKind kind, String nameNorm);

    Optional<Counterparty> findTop1ByNameNormOrderByIdDesc(String nameNorm);

    List<Counterparty> findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(Long chatId);

    List<Counterparty> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String q, Pageable p);
    List<Counterparty> findByActiveTrueAndKindAndNameContainingIgnoreCaseOrderByNameAsc(CounterpartyKind kind, String q, Pageable p);

    List<Counterparty> findByActiveTrueOrderByNameAsc(Pageable p);
    List<Counterparty> findByActiveTrueAndKindOrderByNameAsc(CounterpartyKind kind, Pageable p);

    List<Counterparty> findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(Long chatId, Pageable p);
}
