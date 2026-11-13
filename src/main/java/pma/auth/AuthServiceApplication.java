package pma.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import pma.account.AccountController;

/** Registra só o cliente Feign do Account (o AuthController é implementado aqui, não consumido). */
@SpringBootApplication
@EnableFeignClients(clients = AccountController.class)
public class AuthServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthServiceApplication.class, args);
  }

}
