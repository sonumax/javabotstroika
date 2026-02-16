package com.sonumax2.javabot.domain.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserAccountRepo repo;

    public AuthService(UserAccountRepo repo) {
        this.repo = repo;
    }

    public record AuthResult(boolean allowed, UserRole role, UserStatus status, String replyText) {}

    @Transactional
    public AuthResult checkOrBootstrap(long chatId, String firstName, String username) {

        var accOpt = repo.findById(chatId);
        if (accOpt.isPresent()) {
            var acc = accOpt.get();
            return switch (acc.getStatus()) {
                case ACTIVE -> new AuthResult(true, acc.getRole(), acc.getStatus(), null);
                case PENDING -> new AuthResult(false, acc.getRole(), acc.getStatus(),
                        "Запрос на доступ отправлен. Ждите одобрения администратора.");
                case BLOCKED -> new AuthResult(false, acc.getRole(), acc.getStatus(),
                        "Доступ запрещён.");
            };
        }

        // bootstrap: если нет активных админов — первый становится админом
        boolean hasAdmin = repo.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) > 0;

        var now = LocalDateTime.now();
        var acc = new UserAccount();
        acc.setChatId(chatId);
        acc.setFirstName(firstName);
        acc.setUsername(username);

        if (!hasAdmin) {
            acc.setRole(UserRole.ADMIN);
            acc.setStatus(UserStatus.ACTIVE);
            acc.setCreatedAt(now);
            acc.setRequestedAt(now);
            repo.save(acc);
            return new AuthResult(true, acc.getRole(), acc.getStatus(),
                    "Bootstrap: вы назначены администратором.");
        }

        // обычный пользователь: создаём pending
        acc.setRole(UserRole.USER);
        acc.setStatus(UserStatus.PENDING);
        acc.setCreatedAt(now);
        acc.setRequestedAt(now);
        repo.save(acc);

        return new AuthResult(false, acc.getRole(), acc.getStatus(),
                "Запрос на доступ отправлен. Ждите одобрения администратора.");
    }
}
