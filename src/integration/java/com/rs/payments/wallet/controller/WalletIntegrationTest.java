package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateWalletForExistingUser() {
        User user = new User();
        user.setUsername("walletuser");
        user.setEmail("wallet@example.com");
        user = userRepository.save(user);

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(user.getId());

        String url = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> response = restTemplate.postForEntity(url, request, Wallet.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(UUID.randomUUID());

        String url = "http://localhost:" + port + "/wallets";
        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
    @Test
    void shouldDepositMoney() {
        //Create user
        User user = new User();
        user.setUsername("deposituser");
        user.setEmail("deposit@example.com");
        user = userRepository.save(user);

        //Create wallet
        CreateWalletRequest walletRequest = new CreateWalletRequest();
        walletRequest.setUserId(user.getId());

        String walletUrl = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> walletResponse =
                restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);

        Wallet wallet = walletResponse.getBody();

        //Deposit
        String depositUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";

        String request = "{\"amount\":100}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response =
                restTemplate.postForEntity(depositUrl, entity, Wallet.class);

        // Step 4: Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void shouldWithdrawSuccessfully() {
        // Create user
        User user = new User();
        user.setUsername("withdrawuser");
        user.setEmail("withdraw@example.com");
        user = userRepository.save(user);

        // Create wallet
        CreateWalletRequest walletRequest = new CreateWalletRequest();
        walletRequest.setUserId(user.getId());

        String walletUrl = "http://localhost:" + port + "/wallets";
        Wallet wallet = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class).getBody();

        // Deposit first
        String depositUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity(depositUrl,
                new HttpEntity<>("{\"amount\":200}", headers),
                Wallet.class);

        // Withdraw
        String withdrawUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/withdraw";

        ResponseEntity<Wallet> response =
                restTemplate.postForEntity(withdrawUrl,
                        new HttpEntity<>("{\"amount\":100}", headers),
                        Wallet.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void shouldFailWhenInsufficientBalance() {
        User user = new User();
        user.setUsername("failuser");
        user.setEmail("fail@example.com");
        user = userRepository.save(user);

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(user.getId());

        String walletUrl = "http://localhost:" + port + "/wallets";
        Wallet wallet = restTemplate.postForEntity(walletUrl, request, Wallet.class).getBody();

        String withdrawUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/withdraw";

        try {
            restTemplate.postForEntity(withdrawUrl,
                    new HttpEntity<>("{\"amount\":100}", new HttpHeaders()),
                    Wallet.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
