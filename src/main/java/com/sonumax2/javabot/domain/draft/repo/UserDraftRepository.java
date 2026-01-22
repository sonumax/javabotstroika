package com.sonumax2.javabot.domain.draft.repo;

import com.sonumax2.javabot.domain.draft.UserDraft;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserDraftRepository extends CrudRepository<UserDraft, Long> {
    Optional<UserDraft> findByChatIdAndDraftType(long chatId, String draftType);
    void deleteByChatIdAndDraftType(long chatId, String draftType);
    void deleteByChatId(long chatId);
    List<UserDraft> findAllByChatId(long chatId);
}
