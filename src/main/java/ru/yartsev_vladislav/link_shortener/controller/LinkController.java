package ru.yartsev_vladislav.link_shortener.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

import java.util.Map;

@RestController
public class LinkController {
    private final LinkShortenerService linkShortenerService;

    @Autowired
    public LinkController(LinkShortenerService linkShortenerService) {
        this.linkShortenerService = linkShortenerService;
    }

    @PostMapping("/")
    public ResponseEntity<Object> createLink(
        @RequestBody CreateLinkOptions body,
        @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        try {
            return ResponseEntity.ok(linkShortenerService.createLink(body, userId));
        } catch (UserDoesNotExistException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        } catch (NotExpiredLinkAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Object> getLink(@PathVariable String slug) {
        try {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", linkShortenerService.getFullLink(slug))
                    .build();
        } catch (LinkDoesNotExistException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (LinkHasExpiredException | LinkLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Object> deleteLink(
        @PathVariable String slug,
        @RequestHeader(value = "X-User-Id") String userId
    ) {
        try {
            linkShortenerService.deleteLink(slug, userId);
            return ResponseEntity.ok(Map.of("result", String.format("Link '%s' has been deleted", slug)));
        } catch (UserHasNotEnoughRightsException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (LinkDoesNotExistException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (UserIsNotIdentifiedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{slug}")
    public ResponseEntity<Object> editLink(
        @PathVariable String slug,
        @RequestHeader(value = "X-User-Id") String userId,
        @RequestBody EditLinkOptions body
    ) {
        try {
            linkShortenerService.editLink(slug, userId, body);
            return ResponseEntity.ok(
                Map.of("result", String.format("Link '%s' has been edited successfully"))
            );
        } catch (LinkHasExpiredException e) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", e.getMessage()));
        } catch (UserHasNotEnoughRightsException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (LinkDoesNotExistException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (UserIsNotIdentifiedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
