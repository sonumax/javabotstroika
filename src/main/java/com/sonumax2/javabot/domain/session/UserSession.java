package com.sonumax2.javabot.domain.session;

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
    @Column("username") private String username;
    @Column("panel_message_id") private Long panelMessageId;
    @Column("active_flow_ns") private String activeFlowNs;
    @Column("active_draft_type") private String activeDraftType;

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
    public void setLocale(String locale) {this.locale = locale;}

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

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getPanelMessageId() { return panelMessageId; }
    public void setPanelMessageId(Long panelMessageId) { this.panelMessageId = panelMessageId; }

    public String getActiveFlowNs() { return activeFlowNs; }
    public void setActiveFlowNs(String activeFlowNs) { this.activeFlowNs = activeFlowNs; }

    public String getActiveDraftType() { return activeDraftType; }
    public void setActiveDraftType(String activeDraftType) { this.activeDraftType = activeDraftType; }

}
