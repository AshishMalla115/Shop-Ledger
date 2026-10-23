
package com.shopledger.backend.controller;

import com.shopledger.backend.dto.request.PaymentRequest;
import com.shopledger.backend.dto.response.PaymentResponse;
import com.shopledger.backend.service.PaymentResult;
import com.shopledger.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment")
    public ResponseEntity<PaymentResponse> makePayment(
            @Valid @RequestBody PaymentRequest request) {

        PaymentResult result = paymentService.makePayment(request);

        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(result.response());
    }
}
