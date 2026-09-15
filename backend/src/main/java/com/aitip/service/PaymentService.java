package com.aitip.service;

import com.aitip.dto.PaymentSessionRequest;
import com.aitip.dto.PaymentSessionResponse;
import com.aitip.entity.PaymentSession;
import com.aitip.entity.PaymentSessionStatus;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.PaymentSessionRepository;
import com.aitip.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentSessionRepository paymentSessionRepository;
    private final PaymentProvider paymentProvider;
    private final UserRepository userRepository;

    public PaymentService(PaymentSessionRepository paymentSessionRepository, PaymentProvider paymentProvider, UserRepository userRepository) {
        this.paymentSessionRepository = paymentSessionRepository;
        this.paymentProvider = paymentProvider;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentSessionResponse createSession(PaymentSessionRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PaymentSession session = new PaymentSession();
        session.setUserId(user.getId());
        session.setAmount(request.getAmount());
        session.setCurrency(request.getCurrency());
        session.setRestaurantName(request.getRestaurantName());
        session.setDescription(request.getDescription());
        session.setStatus(PaymentSessionStatus.CREATED);

        session = paymentSessionRepository.save(session);
        
        try {
            paymentProvider.createSession(session);
            session.setStatus(PaymentSessionStatus.PENDING);
        } catch (Exception e) {
            session.setStatus(PaymentSessionStatus.CANCELLED);
            session.setDescription(session.getDescription() + " - " + e.getMessage());
            paymentSessionRepository.save(session);
            throw new RuntimeException("Failed to create payment session: " + e.getMessage());
        }
        
        return toResponse(paymentSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public PaymentSessionResponse getSession(UUID sessionId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PaymentSession session = paymentSessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment session not found"));
                
        return toResponse(session);
    }

    @Transactional
    public PaymentSessionResponse cancelSession(UUID sessionId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PaymentSession session = paymentSessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment session not found"));

        if (session.getStatus() == PaymentSessionStatus.COMPLETED || session.getStatus() == PaymentSessionStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel a completed or already cancelled session");
        }

        try {
            // Mock provider needs some session ID representation, we use our own ID for simplicity in mock
            paymentProvider.cancelSession("mock_sess_" + session.getId());
            session.setStatus(PaymentSessionStatus.CANCELLED);
            session = paymentSessionRepository.save(session);
        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel payment session: " + e.getMessage());
        }

        return toResponse(session);
    }

    private PaymentSessionResponse toResponse(PaymentSession session) {
        PaymentSessionResponse response = new PaymentSessionResponse();
        response.setSessionId(session.getId());
        response.setAmount(session.getAmount());
        response.setCurrency(session.getCurrency());
        response.setRestaurantName(session.getRestaurantName());
        response.setStatus(session.getStatus());
        response.setCreatedAt(session.getCreatedAt());
        return response;
    }
}
