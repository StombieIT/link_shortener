package ru.yartsev_vladislav.link_shortener.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yartsev_vladislav.link_shortener.entity.User;
import ru.yartsev_vladislav.link_shortener.repository.LinkRepository;
import ru.yartsev_vladislav.link_shortener.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class LinkControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private LinkRepository linkRepository;

  @Autowired private UserRepository userRepository;

  private String testUserId;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    // Подключаем H2 in-memory для интеграционных тестов
    registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    registry.add("spring.datasource.username", () -> "sa");
    registry.add("spring.datasource.password", () -> "");
    registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
  }

  @BeforeEach
  void setUp() {
    linkRepository.deleteAll();
    userRepository.deleteAll();

    User user = new User();
    userRepository.save(user);

    testUserId = user.getId();
  }

  @Test
  void createLink_ShouldReturn200AndShortUrl() throws Exception {
    String json =
        """
                {
                  "url": "https://example.com",
                  "limit": 5
                }
                """;

    mockMvc
        .perform(
            post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", testUserId)
                .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shortUrl").exists())
        .andExpect(jsonPath("$.userId").value(testUserId));
  }

  @Test
  void getLink_ShouldRedirectToOriginalUrl() throws Exception {
    // Создаем ссылку
    String json =
        """
                {
                  "url": "https://example.com"
                }
                """;

    String response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", testUserId)
                    .content(json))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String slug = response.split("/")[response.split("/").length - 1].replaceAll("[\"}]", "");

    mockMvc
        .perform(MockMvcRequestBuilders.get("/" + slug))
        .andExpect(MockMvcResultMatchers.status().isFound())
        .andExpect(MockMvcResultMatchers.header().string("Location", "https://example.com"));
  }

  @Test
  void deleteLink_ShouldReturn200() throws Exception {
    // Создаем ссылку
    String json =
        """
                {
                  "url": "https://example.com"
                }
                """;

    String response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", testUserId)
                    .content(json))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String slug = response.split("/")[response.split("/").length - 1].replaceAll("[\"}]", "");

    mockMvc
        .perform(MockMvcRequestBuilders.delete("/" + slug).header("X-User-Id", testUserId))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.result")
                .value("Link '" + slug + "' has been deleted"));
  }

  @Test
  void editLink_ShouldUpdateLimit() throws Exception {
    // Создаем ссылку
    String jsonCreate =
        """
                {
                  "url": "https://example.com",
                  "limit": 5
                }
                """;

    String response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", testUserId)
                    .content(jsonCreate))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String slug = response.split("/")[response.split("/").length - 1].replaceAll("[\"}]", "");

    // Редактируем лимит
    String jsonEdit =
        """
                {
                  "limit": 10
                }
                """;

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/" + slug)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", testUserId)
                .content(jsonEdit))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.result")
                .value("Link '" + slug + "' has been edited successfully"));
  }

  @Test
  void getNonExistentLink_ShouldReturn404() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/nonexistentSlug"))
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andExpect(MockMvcResultMatchers.jsonPath("$.error").exists());
  }
}
