package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.user.UserSession;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface UserSessionRepository extends ListCrudRepository<UserSession, Long> {
    Optional<UserSession> findByChatId(long chatId);
}
