package com.sonumax2.javabot.domain.auth;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserAccountRepo extends CrudRepository<UserAccount, Long> {

    boolean existsByRole(UserRole role);

    List<UserAccount> findAllByStatus(UserStatus status);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    List<UserAccount> findAllByRoleAndStatus(UserRole role, UserStatus status);

    boolean existsByChatId(Long chatId);

}