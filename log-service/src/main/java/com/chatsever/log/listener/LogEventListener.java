package com.chatsever.log.listener;

import com.chatsever.common.dto.LogEntry;
import com.chatsever.log.config.RabbitMQConfig;
import com.chatsever.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consume LogEntry events tu RabbitMQ.
 * Spec: doc/03_thiet_ke_chi_tiet.md § 3.2.5, doc/04_giao_thuc_truyen_thong.md § 4.6
 */
@Component
public class LogEventListener {

    private static final Logger log = LoggerFactory.getLogger(LogEventListener.class);

    private final LogService logService;

    public LogEventListener(LogService logService) {
        this.logService = logService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onLogEvent(LogEntry entry) {
        log.debug("Nhan log event: type={} sender={}", entry.getEventType(), entry.getSender());
        logService.log(entry);
    }
}
