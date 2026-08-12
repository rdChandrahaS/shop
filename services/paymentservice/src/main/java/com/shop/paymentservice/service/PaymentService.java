package com.shop.paymentservice.service;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.razorpay.Utils;
import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;
import com.shop.paymentservice.exception.DuplicatePaymentException;
import com.shop.paymentservice.exception.PaymentNotFoundException;
import com.shop.paymentservice.model.Payment;
import com.shop.paymentservice.model.enums.PaymentMode;
import com.shop.paymentservice.model.enums.PaymentStatus;
import com.shop.paymentservice.repo.PaymentRepository;
import com.shop.paymentservice.strategy.PaymentStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
	private final Map<PaymentMode, PaymentStrategy> paymentStrategies;
	private final PaymentRepository paymentRepo;
	private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Boolean> paymentLockScript;
	
	@Value("${razorpay.webhook.secret}")
    private String webhookSecret;
	
	public PaymentStatus processPayment(PaymentRequest request) {
		
		//Checking if this payment is already processing
		String lockKey = "payment:lock:" + request.getId();
		Boolean isProcessing = redisTemplate.execute(paymentLockScript , List.of(lockKey) , "300"); //300 seconds for payment
		if(Boolean.FALSE.equals(isProcessing)) {
		    log.warn("Duplicate payment attempt blocked for Order ID: {}", request.getId());
		    throw new DuplicatePaymentException("A payment is already processing for this order.");
		}
		
		PaymentStrategy strategy = paymentStrategies.get(request.getMode().name());
		
		
		if (strategy == null) {
			log.error("Invalid payment mode: {}" , request.getMode());
			redisTemplate.delete(lockKey); // If the mode is invalid, release the lock so they can try again
            throw new IllegalArgumentException("Invalid payment mode: " + request.getMode());
        }
		
		try {
			//Saving the Payment Data into database
			Payment newPayment = new Payment();
			newPayment.setAmount(request.getAmount());
			newPayment.setMode(request.getMode());
			newPayment.setOrderId(request.getId());
			newPayment.setStatus(PaymentStatus.PENDING);
			
			paymentRepo.save(newPayment);
			
			//Processing the payment
			PaymentResponse response = strategy.processPayment(request);
			
			//Updating the payment status
			newPayment.setStatus(response.getStatus());
			newPayment.setTransactionId(response.getTransactionId());
			paymentRepo.save(newPayment);
			
			log.info("Payment Done by id : {}",request.getId());
			
			return response.getStatus();
		}catch(Exception e) {
			log.warn("Could not connect to Razorpay");
			redisTemplate.delete(lockKey);
            throw e;
		}		
	}
	
	public void handleRazorpayWebhook(String payload , String signature) {
		try {
			boolean matched = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
			if(!matched) {
				log.warn("ALERT: Invalid Razorpay Webhook Signature detected!");
				return;
			}
			
			JSONObject jsonPayload = new JSONObject(payload);
            String eventName = jsonPayload.getString("event");
            
            if(eventName.equals("payment.captured")) {
            	JSONObject paymentEntity = jsonPayload.getJSONObject("payload")
            										  .getJSONObject("payment")
            										  .getJSONObject("entity");
            	
            	String razorPayOrderID = paymentEntity.getString("order_id");
            	
            	Payment payment = paymentRepo.findByTransactionId(razorPayOrderID)
            	        .orElseThrow(() -> new PaymentNotFoundException("Payment Not Found"));
            	
            	payment.setStatus(PaymentStatus.SUCCESS);
            	paymentRepo.save(payment);
            	
            	log.info("Payment Success Confirmed for Order: {}", payment.getOrderId());
            }
		}catch (Exception e) {
			log.error("Error processing webhook payload: {}", e.getMessage(), e);
		}
	}
	
	public ResponseEntity<String> requestRefund(String orderId,String reason){
		Payment payment = paymentRepo.findById(orderId)
		        .orElseThrow(() -> new PaymentNotFoundException("Invalid OrderID!"));
		
		if (payment.getStatus() != PaymentStatus.SUCCESS) {
			log.info("Asking Refund For Unsuccessful Payment.");
	        return ResponseEntity.badRequest().body("Only successful payments can be refunded.");
	    }
		
		payment.setStatus(PaymentStatus.REFUND_REQUESTED);
	    paymentRepo.save(payment);
	    
	    log.info("Refund request submitted for manual review for order id : {}.",orderId);
	    return ResponseEntity.ok("Refund request submitted for manual review.");
	}

	public List<Payment> getPendingRefunds() {
		return paymentRepo.findByStatus(PaymentStatus.REFUND_REQUESTED);
	}

	public ResponseEntity<String> approveRefund(String orderId) {
		
		Payment payment = paymentRepo.findById(orderId)
		        .orElseThrow(()-> new PaymentNotFoundException("No Order Found"));
	    
	    if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
	        return ResponseEntity.badRequest().body("Payment is not pending a refund.");
	    }

	    PaymentStrategy strategy = paymentStrategies.get(payment.getMode().name());
	    
	    boolean isRefundSuccessful = strategy.processRefund(payment.getTransactionId(), payment.getAmount());
	    
	    if (isRefundSuccessful) {
	        payment.setStatus(PaymentStatus.REFUNDED);
	        paymentRepo.save(payment);
	        log.info("Refund approved and processed successfully for Order id : {}.", orderId);
	        return ResponseEntity.ok("Refund approved and processed successfully.");
	    } else {
	    	log.error("Gateway refund failed for Order id : {}.", orderId);
	        return ResponseEntity.internalServerError().body("Gateway refund failed.");
	    }
	}
	
}