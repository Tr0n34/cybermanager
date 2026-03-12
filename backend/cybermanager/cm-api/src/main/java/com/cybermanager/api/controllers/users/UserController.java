package com.cybermanager.api.controllers.users;

import com.cybermanager.api.dtos.users.CreateUserRequest;
import com.cybermanager.api.dtos.users.UpdateUserRequest;
import com.cybermanager.api.dtos.users.UserResponse;
import com.cybermanager.api.mappers.users.UserApiMapper;
import com.cybermanager.application.commands.users.ChangeUserStatusCommand;
import com.cybermanager.application.commands.users.CreateUserCommand;
import com.cybermanager.application.commands.users.DeleteUserCommand;
import com.cybermanager.application.commands.users.UpdateUserCommand;
import com.cybermanager.application.queries.users.GetUserDetailsQuery;
import com.cybermanager.application.queries.users.SearchUsersQuery;
import com.cybermanager.application.usecases.users.CreateUserUseCase;
import com.cybermanager.application.usecases.users.DisableUserUseCase;
import com.cybermanager.application.usecases.users.DeleteUserUseCase;
import com.cybermanager.application.usecases.users.EnableUserUseCase;
import com.cybermanager.application.usecases.users.GetUserDetailsUseCase;
import com.cybermanager.application.usecases.users.SearchUsersUseCase;
import com.cybermanager.application.usecases.users.UpdateUserUseCase;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final SearchUsersUseCase searchUsersUseCase;
    private final GetUserDetailsUseCase getUserDetailsUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final EnableUserUseCase enableUserUseCase;
    private final DisableUserUseCase disableUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final JwtAccessTokenReader jwtAccessTokenReader;
    private final UserApiMapper userApiMapper;

    public UserController(
            SearchUsersUseCase searchUsersUseCase,
            GetUserDetailsUseCase getUserDetailsUseCase,
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase,
            EnableUserUseCase enableUserUseCase,
            DisableUserUseCase disableUserUseCase,
            DeleteUserUseCase deleteUserUseCase,
            JwtAccessTokenReader jwtAccessTokenReader,
            UserApiMapper userApiMapper
    ) {
        this.searchUsersUseCase = searchUsersUseCase;
        this.getUserDetailsUseCase = getUserDetailsUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.enableUserUseCase = enableUserUseCase;
        this.disableUserUseCase = disableUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.jwtAccessTokenReader = jwtAccessTokenReader;
        this.userApiMapper = userApiMapper;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> search(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "term", required = false) String term,
            @RequestParam(name = "status", required = false) String status
    ) {
        var actor = jwtAccessTokenReader.read(extractToken(authorization));
        var result = searchUsersUseCase.execute(new SearchUsersQuery(actor.email(), actor.roles(), term, status))
                .stream()
                .map(userApiMapper::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") UUID id
    ) {
        var actor = jwtAccessTokenReader.read(extractToken(authorization));
        return ResponseEntity.ok(userApiMapper.toResponse(
                getUserDetailsUseCase.execute(new GetUserDetailsQuery(actor.email(), actor.roles(), id))
        ));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateUserRequest request
    ) {
        var actor = jwtAccessTokenReader.read(extractToken(authorization));
        return ResponseEntity.ok(userApiMapper.toResponse(
                createUserUseCase.execute(new CreateUserCommand(
                        actor.email(),
                        actor.roles(),
                        request.email(),
                        request.firstName(),
                        request.lastName(),
                        request.password(),
                        request.roles()
                ))
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        var actor = jwtAccessTokenReader.read(extractToken(authorization));
        return ResponseEntity.ok(userApiMapper.toResponse(
                updateUserUseCase.execute(new UpdateUserCommand(
                        actor.email(),
                        actor.roles(),
                        id,
                        request.email(),
                        request.firstName(),
                        request.lastName(),
                        request.password(),
                        request.roles()
                ))
        ));
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enable(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = jwtAccessTokenReader.read(extractToken(authorization));
        return ResponseEntity.ok(userApiMapper.toResponse(
                enableUserUseCase.execute(new ChangeUserStatusCommand(actor.email(), actor.roles(), id))
        ));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disable(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = jwtAccessTokenReader.read(extractToken(authorization));
        return ResponseEntity.ok(userApiMapper.toResponse(
                disableUserUseCase.execute(new ChangeUserStatusCommand(actor.email(), actor.roles(), id))
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = jwtAccessTokenReader.read(extractToken(authorization));
        deleteUserUseCase.execute(new DeleteUserCommand(actor.email(), actor.roles(), id));
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token");
        }
        return authorization.substring(7);
    }
}

