package com.chatapp.model;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sender;
    private String receiver;
    private String content;
    private String type; // TEXT, FILE, EMOJI, SYSTEM, LOGIN, REGISTER, etc.
    private long timestamp;
    private String groupId;
    private String fileName;
    private String fileData;
    
    // New fields for Reply and Unsend features
    private boolean isUnsent;
    private String replySnippet;
    private String replySender;

    public Message(String sender, String receiver, String content, String type) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String sender, String receiver, String content, String type, long timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
    }

    // ───── Getters & Setters ─────

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileData() {
        return fileData;
    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isUnsent() {
        return isUnsent;
    }

    public void setUnsent(boolean unsent) {
        isUnsent = unsent;
    }

    public String getReplySnippet() {
        return replySnippet;
    }

    public void setReplySnippet(String replySnippet) {
        this.replySnippet = replySnippet;
    }

    public String getReplySender() {
        return replySender;
    }

    public void setReplySender(String replySender) {
        this.replySender = replySender;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + sender + " → " + (groupId != null && !groupId.isEmpty() ? groupId : receiver) + ": " + content;
    }
}
