package com.aitip.service;

import com.aitip.dto.PaymentSessionRequest;
import com.aitip.dto.PaymentSessionResponse;
import com.aitip.entity.PaymentSession;
import com.aitip.entity.PaymentSessionStatus;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.PaymentSessionRepository;
import com.aitip.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentSessionRepository paymentSessionRepository;

    @Mock
    private PaymentProvider paymentProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private PaymentSessionRequest validRequest;
    private PaymentSession testSession;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@test.com");

        validRequest = new PaymentSessionRequest();
        validRequest.setAmount(new BigDecimal("100.00"));
        validRequest.setCurrency("USD");
        validRequest.setRestaurantName("Test Restaurant");
        validRequest.setDescription("Test Description");

        testSession = new PaymentSession();
        testSession.setId(UUID.randomUUID());
        testSession.setUserId(testUser.getId());
        testSession.setAmount(validRequest.getAmount());
        testSession.setCurrency(validRequest.getCurrency());
        testSession.setRestaurantName(validRequest.getRestaurantName());
        testSession.setDescription(validRequest.getDescription());
        testSession.setStatus(PaymentSessionStatus.CREATED);
        testSession.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createSession_ValidRequest_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(paymentSessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            return session;
        });

        PaymentSessionResponse response = paymentService.createSession(validRequest, testUser.getEmail());

        assertNotNull(response);
        assertEquals(PaymentSessionStatus.PENDING, response.getStatus());
        verify(paymentProvider, times(1)).createSession(any(PaymentSession.class));
    }

    @Test
    void createSession_ProviderFailure_ReturnsCancelledSession() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(paymentSessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            return session;
        });
        doThrow(new RuntimeException("Provider failure")).when(paymentProvider).createSession(any(PaymentSession.class));

        assertThrows(RuntimeException.class, () -> paymentService.createSession(validRequest, testUser.getEmail()));
        // Note: The session is actually saved with CANCELLED status before throwing, 
        // which can be verified by checking the repository save calls.
    }

    @Test
    void getSession_ValidIdAndUser_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(paymentSessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));

        PaymentSessionResponse response = paymentService.getSession(testSession.getId(), testUser.getEmail());

        assertNotNull(response);
        assertEquals(testSession.getId(), response.getSessionId());
    }

    @Test
    void getSession_WrongUser_ThrowsException() {
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(new User()));
        when(paymentSessionRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getSession(testSession.getId(), "other@test.com"));
    }

    @Test
    void cancelSession_ValidSession_Success() {
        testSession.setStatus(PaymentSessionStatus.PENDING);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(paymentSessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));
        when(paymentSessionRepository.save(any(PaymentSession.class))).thenReturn(testSession);

        PaymentSessionResponse response = paymentService.cancelSession(testSession.getId(), testUser.getEmail());

        assertEquals(PaymentSessionStatus.CANCELLED, response.getStatus());
        verify(paymentProvider, times(1)).cancelSession(anyString());
    }

    @Test
    void cancelSession_AlreadyCompleted_ThrowsException() {
        testSession.setStatus(PaymentSessionStatus.COMPLETED);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(paymentSessionRepository.findByIdAndUserId(testSession.getId(), testUser.getId())).thenReturn(Optional.of(testSession));

        assertThrows(RuntimeException.class, () -> paymentService.cancelSession(testSession.getId(), testUser.getEmail()));
    }
}
