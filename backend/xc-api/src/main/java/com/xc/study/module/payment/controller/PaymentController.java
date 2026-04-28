package com.xc.study.module.payment.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.payment.dto.CreatePaymentOrderDTO;
import com.xc.study.module.payment.service.PaymentService;
import com.xc.study.module.payment.vo.PaymentNotificationResultVO;
import com.xc.study.module.payment.vo.PaymentOrderVO;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public PaymentController(PaymentService paymentService, CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/orders")
    public ApiResponse<PaymentOrderVO> createOrder(@Valid @RequestBody CreatePaymentOrderDTO request) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(paymentService.createOrder(userId, request));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<PaymentOrderVO> getOrder(@PathVariable String orderNo) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(paymentService.getStudentOrder(userId, orderNo));
    }

    @PostMapping("/orders/{orderNo}/simulate-paid")
    public ApiResponse<PaymentOrderVO> simulatePaid(@PathVariable String orderNo) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(paymentService.simulatePaid(userId, orderNo));
    }

    @PostMapping("/wechat/notify")
    public ApiResponse<PaymentNotificationResultVO> wechatNotify(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(paymentService.handleNotification("wechat_pay", payload));
    }

    @PostMapping("/alipay/notify")
    public ApiResponse<PaymentNotificationResultVO> alipayNotify(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(paymentService.handleNotification("alipay", payload));
    }
}
