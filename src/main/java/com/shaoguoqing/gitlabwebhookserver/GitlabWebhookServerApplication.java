package com.shaoguoqing.gitlabwebhookserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("gitlab.properties")
public class GitlabWebhookServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitlabWebhookServerApplication.class, args);
	}

}
