package com.cybermanager.api.mappers.users;

import com.cybermanager.api.dtos.users.UserResponse;
import com.cybermanager.application.views.users.UserDetailsView;
import com.cybermanager.application.views.users.UserSummaryView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserApiMapper {
    UserResponse toResponse(UserDetailsView view);

    UserResponse toResponse(UserSummaryView view);
}

