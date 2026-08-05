package com.gateway.queue;

public record NotificationTask(
    String taskId, 
    String tenantId, 
    String channel, 
    String recipient, 
    String message
) {}