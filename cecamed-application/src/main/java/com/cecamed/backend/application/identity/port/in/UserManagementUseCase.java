package com.cecamed.backend.application.identity.port.in;

import com.cecamed.backend.application.identity.model.ChangePasswordCommand;
import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.application.identity.model.PagedResult;
import com.cecamed.backend.application.identity.model.UpdateUserCommand;
import com.cecamed.backend.application.identity.model.UserDetailResult;
import com.cecamed.backend.application.identity.model.UserSummaryResult;
import com.cecamed.backend.domain.identity.Role;

public interface UserManagementUseCase {
    UserDetailResult createUser(CreateUserCommand command);
    UserDetailResult getUser(Long id);
    PagedResult<UserSummaryResult> listUsers(int page, int size, String search, Role role, Boolean active);
    UserDetailResult updateUser(UpdateUserCommand command);
    UserDetailResult activateUser(Long id, String performedBy, String ipAddress, String userAgent);
    UserDetailResult deactivateUser(Long id, String performedBy, String ipAddress, String userAgent);
    void resetPassword(ChangePasswordCommand command);
}
