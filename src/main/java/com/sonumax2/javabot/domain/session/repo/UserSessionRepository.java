package com.sonumax2.javabot.domain.session.repo;

import com.sonumax2.javabot.domain.session.UserSession;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface UserSessionRepository extends ListCrudRepository<UserSession, Long> {
    Optional<UserSession> findByChatId(long chatId);
}
