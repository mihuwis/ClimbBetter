package pl.climbbetter.identity.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.climbbetter.identity.CurrentUserProvider;

@RestController
class CurrentUserController {
    private final CurrentUserProvider currentUserProvider;
    private final IdentityAccountService accounts;

    CurrentUserController(CurrentUserProvider currentUserProvider, IdentityAccountService accounts) {
        this.currentUserProvider = currentUserProvider;
        this.accounts = accounts;
    }

    @GetMapping("/api/v1/identity/me")
    AccountProfile currentUser() {
        return accounts.get(currentUserProvider.currentUserId());
    }

    @PostMapping("/api/v1/identity/bootstrap")
    AccountProfile bootstrap(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof IdentityPrincipal principal)) {
            throw new IdentityAccessException(401, "AUTH_REQUIRED", "Please sign in.");
        }
        return accounts.bootstrap(principal.identity());
    }

    @PutMapping("/api/v1/identity/me")
    AccountProfile update(@Valid @RequestBody ProfileRequest request) {
        return accounts.update(currentUserProvider.currentUserId(), request.displayName());
    }

    record ProfileRequest(@NotBlank @Size(max = 80) String displayName) { }
}
