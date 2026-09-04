package com.example.wanteat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WanteatApplicationTests {

	@Test
	void contextLoads() {
		// アプリケーションコンテキストが正常に起動できることを確認
	}

}
