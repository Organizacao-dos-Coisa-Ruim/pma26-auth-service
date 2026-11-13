package pma.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import pma.account.AccountController;
import pma.account.AccountOut;
import pma.account.LoginIn;

/** Sobe a aplicação inteira com o cliente Feign do Account mockado. */
// métricas ficam desligadas em testes por padrão; religamos para testar o endpoint do Prometheus
@AutoConfigureMetrics
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthResourceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AccountController accountController;

  @Test
  void healthCheck() throws Exception {
    mockMvc.perform(get("/auth/health-check")).andExpect(status().isOk());
  }

  @Test
  void metricasDoPrometheusComHistogramaDeLatencia() throws Exception {
    mockMvc.perform(get("/auth/health-check"));

    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("http_server_requests_seconds_bucket")));
  }

  @Test
  void registerDa201() throws Exception {
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Ana\",\"email\":\"ana@insper.edu.br\",\"password\":\"123456\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void loginEDepoisSolveDevolvemAConta() throws Exception {
    when(accountController.login(any(LoginIn.class)))
        .thenReturn(ResponseEntity.ok(new AccountOut("conta-1", "Ana", "ana@insper.edu.br")));

    String body = mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"ana@insper.edu.br\",\"password\":\"123456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jwt").isNotEmpty())
        .andReturn().getResponse().getContentAsString();
    String jwt = JsonPath.read(body, "$.jwt");

    // mesmo contrato que o gateway usa
    mockMvc.perform(post("/auth/solve")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"jwt\":\"" + jwt + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idAccount").value("conta-1"))
        .andExpect(jsonPath("$.role").value("user"));
  }

  @Test
  void solveComTokenInvalidoDa401() throws Exception {
    mockMvc.perform(post("/auth/solve")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"jwt\":\"lixo\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void solveSemTokenDa401() throws Exception {
    mockMvc.perform(post("/auth/solve")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isUnauthorized());
  }

}
