package com.mozi.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot 애플리케이션 진입점.
 *
 * @ConfigurationPropertiesScan: com.mozi.backend 패키지 하위의 모든
 * @ConfigurationProperties 빈을 자동 등록 (예: JwtProperties).
 * 이 어노테이션이 없으면 각 properties 클래스마다 @EnableConfigurationProperties를
 * 명시해야 하므로 한 곳에서 일괄 활성화하는 편이 깔끔하다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
