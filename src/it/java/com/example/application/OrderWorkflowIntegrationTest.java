package com.example.application;

import com.example.Main;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = Main.class)
@ActiveProfiles("orchestration")
class OrderWorkflowIntegrationTest extends OrderSagaIntegrationTest {
}