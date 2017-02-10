package com.bakkenbaeck.token.model.local;

import com.bakkenbaeck.token.model.sofa.SofaType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ChatMessage extends RealmObject {

    @PrimaryKey
    private String privateKey;
    private long creationTime;
    private @SofaType.Type int type;
    private @SendState.State int sendState;
    private String conversationId;
    private String payload;
    private boolean sentByLocal;

    public ChatMessage() {
        this.creationTime = System.currentTimeMillis();
    }

    public ChatMessage(final ChatMessage chatMessage) {
        this.privateKey = chatMessage.getPrivateKey();
        this.creationTime = chatMessage.getCreationTime();
        this.type = chatMessage.getType();
        this.sendState = chatMessage.getSendState();
        this.conversationId = chatMessage.getConversationId();
        this.payload = chatMessage.getPayload();
        this.sentByLocal = chatMessage.isSentByLocal();
    }

    // Setters

    private ChatMessage setType(final @SofaType.Type int type) {
        this.type = type;
        return this;
    }

    public ChatMessage setSendState(final @SendState.State int sendState) {
        this.sendState = sendState;
        return this;
    }

    private ChatMessage setConversationId(final String conversationId) {
        this.conversationId = conversationId;
        this.privateKey = this.conversationId + String.valueOf(this.creationTime);
        return this;
    }

    public ChatMessage setPayload(final String payload) {
        this.payload = payload;
        return this;
    }

    public ChatMessage setSentByLocal(final boolean sentByLocal) {
        this.sentByLocal = sentByLocal;
        return this;
    }

    // Getters

    private String getPrivateKey() {
        return this.privateKey;
    }

    public String getPayload() {
        return cleanPayload(this.payload);
    }

    public String getPayloadWithHeaders() {
        return this.payload;
    }

    // Return message in the correct format for SOFA
    public String getAsSofaMessage() {
        // Strip away local-only data before sending via Signal
        final String matcher = "\"" + SofaType.LOCAL_ONLY_PAYLOAD + "\":\\{.*?\\},";
        return this.payload.replaceFirst(matcher, "");
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public @SofaType.Type int getType() {
        return this.type;
    }

    public @SendState.State int getSendState() {
        return this.sendState;
    }

    public boolean isSentByLocal() {
        return this.sentByLocal;
    }

    private long getCreationTime() {
        return this.creationTime;
    }

    // Helper functions

    private String cleanPayload(final String payload) {
        final String regexString = "\\{.*\\}";
        final Pattern pattern = Pattern.compile(regexString);
        final Matcher m = pattern.matcher(payload);
        if (m.find()) {
            return m.group();
        }
        return payload;
    }

    private String getSofaHeader(final String payload) {
        final String regexString = "SOFA::.+?:";
        final Pattern pattern = Pattern.compile(regexString);
        final Matcher m = pattern.matcher(payload);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    public ChatMessage makeNew(
            final String conversationId,
            final boolean sentByLocal,
            final String sofaPayload) {
        final String sofaHeader = getSofaHeader(sofaPayload);
        final @SofaType.Type int sofaType = SofaType.getType(sofaHeader);

        return setConversationId(conversationId)
                .setSendState(SendState.STATE_SENDING)
                .setType(sofaType)
                .setSentByLocal(sentByLocal)
                .setPayload(sofaPayload);
    }

    public ChatMessage makeNew(final String sofaPayload) {
        final String sofaHeader = getSofaHeader(sofaPayload);
        final @SofaType.Type int sofaType = SofaType.getType(sofaHeader);

        return setType(sofaType)
                .setPayload(sofaPayload);
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof ChatMessage))return false;
        final ChatMessage otherChatMessage = (ChatMessage) other;
        return otherChatMessage.getPrivateKey().equals(this.privateKey);
    }

    @Override
    public int hashCode() {
        return privateKey.hashCode();
    }
}