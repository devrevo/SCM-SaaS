package com.scm_saas.SCM_SaaS;

import org.springframework.boot.SpringApplication;

public class TestScmSaaSApplication {

	public static void main(String[] args) {
		SpringApplication.from(ScmSaaSApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
