package com.sonumax2.javabot.domain.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserAccountRepo repo;

    public AuthService(UserAccountRepo repo) {
        this.repo = repo;
    }

    // notifyAdmins = true только если мы ТОЛЬКО ЧТО создали нового PENDING пользователя
    public record AuthResult(
            boolean allowed,
            UserRole role,
            UserStatus status,
            String replyText,
            boolean notifyAdmins
    ) {}

    @Transactional
    public AuthResult checkOrBootstrap(long chatId, String firstName, String username) {

        var accOpt = repo.findById(chatId);
        if (accOpt.isPresent()) {
            var acc = accOpt.get();

            return switch (acc.getStatus()) {
                case ACTIVE -> new AuthResult(true, acc.getRole(), acc.getStatus(), null, false);
                case PENDING -> new AuthResult(false, acc.getRole(), acc.getStatus(),
                        "Запрос на доступ отправлен. Ждите одобрения администратора.", false);
                case BLOCKED -> new AuthResult(false, acc.getRole(), acc.getStatus(),
                        "Доступ запрещён.", false);
            };
        }

        // bootstrap: если нет активных админов — первый становится админом
        boolean hasAdmin = repo.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) > 0;

        var now = LocalDateTime.now();
        var acc = new UserAccount();
        acc.setChatId(chatId);
        acc.setFirstName(firstName);
        acc.setUsername(username);
        acc.setCreatedAt(now);
        acc.setRequestedAt(now);

        if (!hasAdmin) {
            acc.setRole(UserRole.ADMIN);
            acc.setStatus(UserStatus.ACTIVE);
            repo.save(acc);
            return new AuthResult(true, acc.getRole(), acc.getStatus(),
                    "Bootstrap: вы назначены администратором.", false);
        }

        // обычный пользователь: создаём pending (и просим MainBot уведомить админов)
        acc.setRole(UserRole.USER);
        acc.setStatus(UserStatus.PENDING);
        repo.save(acc);

        return new AuthResult(false, acc.getRole(), acc.getStatus(),
                "Запрос на доступ отправлен. Ждите одобрения администратора.", true);
    }

    @Transactional
    public void approve(long targetChatId, long adminChatId) {
        var acc = repo.findById(targetChatId).orElseThrow();
        acc.setStatus(UserStatus.ACTIVE);
        acc.setApprovedAt(LocalDateTime.now());
        acc.setApprovedBy(adminChatId);
        repo.save(acc);
    }

    @Transactional
    public void block(long targetChatId, long adminChatId) {
        var acc = repo.findById(targetChatId).orElseThrow();
        acc.setStatus(UserStatus.BLOCKED);
        acc.setApprovedAt(LocalDateTime.now());
        acc.setApprovedBy(adminChatId);
        repo.save(acc);
    }

    @Transactional
    public boolean requestAccess(long chatId, String firstName, String username) {
        if (repo.existsById(chatId)) return false;

        boolean hasAdmin = repo.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) > 0;

        var now = LocalDateTime.now();
        var acc = new UserAccount();
        acc.setChatId(chatId);
        acc.setFirstName(firstName);
        acc.setUsername(username);
        acc.setCreatedAt(now);
        acc.setRequestedAt(now);

        if (!hasAdmin) {
            acc.setRole(UserRole.ADMIN);
            acc.setStatus(UserStatus.ACTIVE);
            repo.save(acc);
            return false;
        }

        acc.setRole(UserRole.USER);
        acc.setStatus(UserStatus.PENDING);
        repo.save(acc);
        return true;
    }

    public List<Long> adminChatIds() {
        return repo.findAllByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)
                .stream()
                .map(UserAccount::getChatId)
                .toList();
    }
}
