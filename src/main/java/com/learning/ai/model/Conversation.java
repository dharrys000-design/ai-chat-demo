package com.learning.ai.model;

public class Conversation {

    private String conversationId;
    private String prompt;
    private String response;
    private long timestamp;

    public Conversation() {
    }

    public Conversation(String conversationId, String prompt, String response, long timestamp) {
        this.conversationId = conversationId;
        this.prompt = prompt;
        this.response = response;
        this.timestamp = timestamp;
    }

    public Conversation(String conversationId, String prompt, String response) {
        this.conversationId = conversationId;
        this.prompt = prompt;
        this.response = response;
        this.timestamp = System.currentTimeMillis();
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
