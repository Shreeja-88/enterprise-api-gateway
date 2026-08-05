package com.gateway.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.queue.NotificationQueueService;
import com.gateway.queue.NotificationTask;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class NotificationServlet extends HttpServlet {
    private final NotificationQueueService queueService = new NotificationQueueService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String tenantId = req.getHeader("X-Tenant-API-Key");
        Map body = objectMapper.readValue(req.getInputStream(), Map.class);

        String taskId = UUID.randomUUID().toString();
        NotificationTask task = new NotificationTask(
            taskId,
            tenantId,
            (String) body.get("channel"),
            (String) body.get("recipient"),
            (String) body.get("message")
        );

        boolean enqueued = queueService.enqueue(task);

        resp.setContentType("application/json");
        if (enqueued) {
            resp.setStatus(HttpServletResponse.SC_ACCEPTED); // HTTP 202 Accepted
            resp.getWriter().write("{\"status\": \"ACCEPTED\", \"trackingId\": \"" + taskId + "\"}");
        } else {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); // HTTP 503 Backpressure
            resp.getWriter().write("{\"error\": \"System busy, queue full.\"}");
        }
    }
}