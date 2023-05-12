package com.example.application.choreography;

import com.example.Main;
import com.example.application.OrderSagaIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = Main.class)
@ActiveProfiles("choreography")
public class OrderEntityIntegrationTest extends OrderSagaIntegrationTest {
}