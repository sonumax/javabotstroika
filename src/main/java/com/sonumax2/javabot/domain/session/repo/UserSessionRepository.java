package com.sonumax2.javabot.domain.session.repo;

import com.sonumax2.javabot.domain.session.UserSession;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSessionRepository extends ListCrudRepository<UserSession, Long> {
    Optional<UserSession> findByChatId(long chatId);

    @Modifying
    @Query("UPDATE user_session SET panel_message_id = :panelId WHERE chat_id = :chatId")
    int updatePanelId(@Param("chatId") long chatId, @Param("panelId") Long panelId);

    @Modifying
    @Query("UPDATE user_session SET locale = :locale WHERE chat_id = :chatId")
    int updateLocale(@Param("chatId") long chatId, @Param("locale") String locale);

    @Modifying
    @Query("UPDATE user_session SET user_state = :state WHERE chat_id = :chatId")
    int updateUserState(@Param("chatId") long chatId, @Param("state") String state);

    @Modifying
    @Query("UPDATE user_session SET active_flow_ns = :ns, active_draft_type = :draftType WHERE chat_id = :chatId")
    int updateActiveFlow(@Param("chatId") long chatId, @Param("ns") String ns, @Param("draftType") String draftType);

    @Modifying
    @Query("UPDATE user_session SET active_flow_ns = NULL, active_draft_type = NULL WHERE chat_id = :chatId")
    int clearActiveFlow(@Param("chatId") long chatId);

    @Modifying
    @Query("UPDATE user_session SET timezone = :timezone WHERE chat_id = :chatId")
    int updateTimezone(@Param("chatId") long chatId, @Param("timezone") String timezone);

}
