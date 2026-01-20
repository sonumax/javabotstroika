package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.user.UserDraft;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserDraftRepository extends CrudRepository<UserDraft, Long> {
    Optional<UserDraft> findByChatId(Long chatId);
    Optional<UserDraft> findByChatIdAndDraftType(Long chatId, String draftType);
    void deleteByChatId(Long chatId);
    void deleteByChatIdAndDraftType(Long chatId, String draftType);
}
