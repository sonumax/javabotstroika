package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.reference.WorkObject;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface WorkObjectRepository extends CrudRepository<WorkObject, Long> {
    List<WorkObject> findByActiveTrueOrderByNameAsc();
    Optional<WorkObject> findFirstByActiveTrueAndNameNorm(String nameNorm);
}
