package com.kidsstory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Step 8: User notification.
 *
 * <p>Plug in a push-notification / email / websocket service here to let
 * the user know their video is ready (or failed). Stub - the frontend
 * already polls the status endpoint, so this is just a log line for now.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyUser(String projectId, boolean success) {
        log.info("[notify] project={} status={}", projectId, success ? "완료" : "실패");
    }
}
