package com.goldatech.notificationservice.domain.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelFactory {

    private final ApplicationContext context;

    public NotificationChannel getChannel(String channelType) {
        return context.getBean(channelType, NotificationChannel.class);
    }
}
