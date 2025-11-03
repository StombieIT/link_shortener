package ru.yartsev_vladislav.link_shortener.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.yartsev_vladislav.link_shortener.exception.LinkDoesNotExistException;
import ru.yartsev_vladislav.link_shortener.exception.LinkHasExpiredException;
import ru.yartsev_vladislav.link_shortener.exception.LinkLimitExceededException;
import ru.yartsev_vladislav.link_shortener.exception.NotExpiredLinkAlreadyExistsException;
import ru.yartsev_vladislav.link_shortener.exception.UserDoesNotExistException;
import ru.yartsev_vladislav.link_shortener.exception.UserHasNotEnoughRightsException;
import ru.yartsev_vladislav.link_shortener.exception.UserIsNotIdentifiedException;
import ru.yartsev_vladislav.link_shortener.model.CreateLinkOptions;
import ru.yartsev_vladislav.link_shortener.model.EditLinkOptions;
import ru.yartsev_vladislav.link_shortener.service.LinkShortenerService;

@RestController
@Tag(
    name = "Links",
    description = "Операции для создания, получения, редактирования и удаления коротких ссылок")
public class LinkController {
  private final LinkShortenerService linkShortenerService;

  @Autowired
  public LinkController(LinkShortenerService linkShortenerService) {
    this.linkShortenerService = linkShortenerService;
  }

  @Operation(
      summary = "Создать короткую ссылку",
      description =
          "Создаёт новую короткую ссылку для переданного URL. "
              + "Если у пользователя уже есть неистёкшая ссылка с таким URL, вернёт ошибку.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ссылка успешно создана"),
    @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content),
    @ApiResponse(responseCode = "401", description = "Пользователь не найден", content = @Content),
    @ApiResponse(
        responseCode = "409",
        description = "Такая ссылка уже существует и ещё не истекла",
        content = @Content)
  })
  @PostMapping("/")
  public ResponseEntity<Object> createLink(
      @RequestBody CreateLinkOptions body,
      @Parameter(
              description = "Идентификатор пользователя",
              example = "f19b92f3-8a3e-4c63-9b77-1e9c50450fa1")
          @RequestHeader(value = "X-User-Id", required = false)
          String userId) {
    try {
      return ResponseEntity.ok(linkShortenerService.createLink(body, userId));
    } catch (UserDoesNotExistException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (NotExpiredLinkAlreadyExistsException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
  }

  @Operation(
      summary = "Переход по короткой ссылке",
      description =
          "Перенаправляет на оригинальный URL по короткому slug. "
              + "Если ссылка истекла или превышен лимит — вернёт ошибку.")
  @ApiResponses({
    @ApiResponse(responseCode = "302", description = "Перенаправление на оригинальную ссылку"),
    @ApiResponse(responseCode = "404", description = "Ссылка не найдена", content = @Content),
    @ApiResponse(
        responseCode = "410",
        description = "Ссылка истекла или превышен лимит",
        content = @Content)
  })
  @GetMapping("/{slug}")
  public ResponseEntity<Object> getLink(
      @Parameter(description = "Короткий идентификатор ссылки", example = "3UmIUftimB9")
          @PathVariable
          String slug) {
    try {
      return ResponseEntity.status(HttpStatus.FOUND)
          .header("Location", linkShortenerService.getFullLink(slug))
          .build();
    } catch (LinkDoesNotExistException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (LinkHasExpiredException | LinkLimitExceededException e) {
      return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", e.getMessage()));
    }
  }

  @Operation(
      summary = "Удалить короткую ссылку",
      description = "Удаляет короткую ссылку, если пользователь является её владельцем.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ссылка удалена"),
    @ApiResponse(
        responseCode = "403",
        description = "Недостаточно прав у пользователя",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Ссылка не найдена", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Пользователь не идентифицирован",
        content = @Content)
  })
  @DeleteMapping("/{slug}")
  public ResponseEntity<Object> deleteLink(
      @Parameter(description = "Короткий идентификатор ссылки", example = "3UmIUftimB9")
          @PathVariable
          String slug,
      @Parameter(
              description = "Идентификатор пользователя",
              example = "f19b92f3-8a3e-4c63-9b77-1e9c50450fa1")
          @RequestHeader(value = "X-User-Id")
          String userId) {
    try {
      linkShortenerService.deleteLink(slug, userId);
      return ResponseEntity.ok(Map.of("result", String.format("Link '%s' has been deleted", slug)));
    } catch (UserHasNotEnoughRightsException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (LinkDoesNotExistException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (UserIsNotIdentifiedException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }
  }

  @Operation(
      summary = "Редактировать короткую ссылку",
      description =
          "Позволяет изменить лимит попыток для существующей ссылки, если она ещё не истекла.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ссылка успешно изменена"),
    @ApiResponse(responseCode = "400", description = "Некорректные данные", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Пользователь не идентифицирован",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Недостаточно прав у пользователя",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Ссылка не найдена", content = @Content),
    @ApiResponse(responseCode = "410", description = "Ссылка истекла", content = @Content)
  })
  @PutMapping("/{slug}")
  public ResponseEntity<Object> editLink(
      @Parameter(description = "Короткий идентификатор ссылки", example = "3UmIUftimB9")
          @PathVariable
          String slug,
      @Parameter(
              description = "Идентификатор пользователя",
              example = "f19b92f3-8a3e-4c63-9b77-1e9c50450fa1")
          @RequestHeader(value = "X-User-Id")
          String userId,
      @RequestBody EditLinkOptions body) {
    try {
      linkShortenerService.editLink(slug, userId, body);
      return ResponseEntity.ok(
          Map.of("result", String.format("Link '%s' has been edited successfully", slug)));
    } catch (LinkHasExpiredException e) {
      return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", e.getMessage()));
    } catch (UserHasNotEnoughRightsException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (LinkDoesNotExistException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (UserIsNotIdentifiedException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
  }
}
