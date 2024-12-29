package com.scm_saas.SCM_SaaS;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ScmSaaSApplicationTests {

	@Test
	void contextLoads() {
	}

}
