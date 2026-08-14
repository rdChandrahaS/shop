package com.shop.paymentservice.strategy;

import java.math.BigDecimal;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;
import com.shop.paymentservice.model.enums.PaymentStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("ONLINE")
public class OnlinePaymentStrategy implements PaymentStrategy {
	
	@Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;
	
    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
    	try {
    		RazorpayClient razorpay = new RazorpayClient(apiKey, apiSecret);
    		
    		int amountInPaisa = request.getAmount().multiply(new BigDecimal("100")).intValue();
    		
    		JSONObject orderObject = new JSONObject();
    		orderObject.put("amount", amountInPaisa);   
    		orderObject.put("receipt", request.getId());
    		orderObject.put("currency", "INR");         	
    		
    		Order order = razorpay.orders.create(orderObject);
    		String paymentId = order.get("id");
    		log.info("Payment pending for the id : {}", paymentId);
    		return new PaymentResponse(PaymentStatus.PENDING, paymentId, request.getAmount());
    	}catch (RazorpayException e) {
    		log.error("Payment Failed! with exception : {}" , e);
    		return new PaymentResponse(PaymentStatus.FAILED, null, request.getAmount());
    	}
    }
    
    @Override
    public boolean processRefund(String transactionId, BigDecimal refundAmount) {
        try {
            RazorpayClient razorpay = new RazorpayClient(apiKey, apiSecret);
            
            int amountInPaisa = refundAmount.multiply(new BigDecimal("100")).intValue();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInPaisa);
            refundRequest.put("speed", "optimum");

            razorpay.payments.refund(transactionId, refundRequest);
            
            return true;

        } catch (RazorpayException e) {
            log.error("Refund failed for transaction {}: {}", transactionId, e.getMessage());
            return false;
        }
    }
}
