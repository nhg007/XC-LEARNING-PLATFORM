package com.xc.study.module.payment.service;

import com.xc.study.module.payment.entity.PaymentNotification;
import com.xc.study.module.payment.mapper.PaymentNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentNotificationRecorder {

    private final PaymentNotificationMapper paymentNotificationMapper;

    public PaymentNotificationRecorder(PaymentNotificationMapper paymentNotificationMapper) {
        this.paymentNotificationMapper = paymentNotificationMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(PaymentNotification notification) {
        paymentNotificationMapper.insertNotification(notification);
    }
}
