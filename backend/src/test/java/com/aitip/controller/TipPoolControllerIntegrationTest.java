package com.aitip.controller;

import com.aitip.dto.CreateTipPoolRequest;
import com.aitip.dto.PoolMemberRequest;
import com.aitip.dto.TipPoolResponse;
import com.aitip.entity.DistributionType;
import com.aitip.entity.TipPoolStatus;
import com.aitip.entity.User;
import com.aitip.repository.TipPoolRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TipPoolControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipPoolRepository tipPoolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private String user2Token;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        tipPoolRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setName("User One");
        user1.setPassword(passwordEncoder.encode("password123"));
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setName("User Two");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2 = userRepository.save(user2);

        user1Token = jwtTokenProvider.generateToken(user1.getEmail());
        user2Token = jwtTokenProvider.generateToken(user2.getEmail());
    }

    @Test
    void requiresJwt() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        mockMvc.perform(post("/api/tip-pools")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPool_Returns201() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test Rest", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("Alice", null), new PoolMemberRequest("Bob", null))
        );

        mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(TipPoolStatus.DRAFT.name()))
                .andExpect(jsonPath("$.members[0].allocatedAmount").value(5.0))
                .andExpect(jsonPath("$.members[1].allocatedAmount").value(5.0));
    }

    @Test
    void getUserPools_ReturnsOnlyUserPools() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        String responseString = mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        TipPoolResponse createdPool = objectMapper.readValue(responseString, TipPoolResponse.class);

        // User 1 sees it
        mockMvc.perform(get("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(createdPool.id().toString()));

        // User 2 sees nothing
        mockMvc.perform(get("/api/tip-pools")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPoolById_UserIsolation_Returns404() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        String responseString = mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        TipPoolResponse createdPool = objectMapper.readValue(responseString, TipPoolResponse.class);

        // User 2 tries to GET
        mockMvc.perform(get("/api/tip-pools/" + createdPool.id())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePool_UserIsolation_Returns404() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        String responseString = mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        TipPoolResponse createdPool = objectMapper.readValue(responseString, TipPoolResponse.class);

        // User 2 tries to DELETE
        mockMvc.perform(delete("/api/tip-pools/" + createdPool.id())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void finalizePool_UserIsolation_Returns404() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        String responseString = mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        TipPoolResponse createdPool = objectMapper.readValue(responseString, TipPoolResponse.class);

        // User 2 tries to FINALIZE
        mockMvc.perform(post("/api/tip-pools/" + createdPool.id() + "/finalize")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDraft_Success_Returns204() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        String responseString = mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        TipPoolResponse createdPool = objectMapper.readValue(responseString, TipPoolResponse.class);

        mockMvc.perform(delete("/api/tip-pools/" + createdPool.id())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        assertEquals(0, tipPoolRepository.count());
    }

    @Test
    void finalizeDraft_Success() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        String responseString = mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        TipPoolResponse createdPool = objectMapper.readValue(responseString, TipPoolResponse.class);

        mockMvc.perform(post("/api/tip-pools/" + createdPool.id() + "/finalize")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TipPoolStatus.FINALIZED.name()));
    }

    @Test
    void deleteFinalized_Returns409() throws Exception {
        CreateTipPoolRequest req = new CreateTipPoolRequest(
                new BigDecimal("10.00"), "USD", "Test", DistributionType.EQUAL,
                List.of(new PoolMemberRequest("A", null), new PoolMemberRequest("B", null))
        );

        String responseString = mockMvc.perform(post("/api/tip-pools")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        TipPoolResponse createdPool = objectMapper.readValue(responseString, TipPoolResponse.class);

        mockMvc.perform(post("/api/tip-pools/" + createdPool.id() + "/finalize")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/tip-pools/" + createdPool.id())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isConflict());
    }
}
