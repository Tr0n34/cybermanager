package com.cybermanager.application.services.users;

import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;

public class UserManagementException extends BusinessException {
    public UserManagementException(BusinessErrorType type, String code, String message) {
        super(type, code, message);
    }
}

