package com.sonumax2.javabot.model.user;

import com.sonumax2.javabot.model.UserState;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_session")
public class UserSession {

    @Id @Column("id") private Long id;
    @Column("chat_id") private Long chatId;
    @Column("locale") private String locale;
    @Column("first_name") private String firstName;
    @Column("user_state") private UserState userState;

    public UserState getUserState() {
        return userState;
    }
    public void setUserState(UserState userState) {
        this.userState = userState;
    }

    public Long getChatId() {
        return chatId;
    }
    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getLocale() {
        return locale;
    }
    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
}
