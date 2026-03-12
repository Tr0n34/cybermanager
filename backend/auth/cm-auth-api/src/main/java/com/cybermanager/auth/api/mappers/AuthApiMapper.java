package com.cybermanager.auth.api.mappers;

import com.cybermanager.auth.api.dtos.AuthenticationResponse;
import com.cybermanager.auth.api.dtos.CurrentUserResponse;
import com.cybermanager.auth.application.views.AuthenticationResultView;
import com.cybermanager.auth.application.views.CurrentUserView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthApiMapper {
    AuthenticationResponse toResponse(AuthenticationResultView view);

    CurrentUserResponse toResponse(CurrentUserView view);
}

