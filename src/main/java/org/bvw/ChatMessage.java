package org.bvw;

import com.google.common.base.Objects;

public class ChatMessage {

    public String timestamp;
    public String message;
    public String user;

    public ChatMessage(String timestamp, String message, String user) {
        this.timestamp = timestamp;
        this.message = message;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equal(timestamp, that.timestamp) && Objects.equal(message, that.message) && Objects.equal(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(timestamp, message, user);
    }
}
