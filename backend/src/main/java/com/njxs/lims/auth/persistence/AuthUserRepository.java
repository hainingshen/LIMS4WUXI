package com.njxs.lims.auth.persistence;

import com.njxs.lims.auth.domain.AuthUser;
import com.njxs.lims.auth.domain.ClientType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository {

  Optional<AuthUser> findActiveByUsernameAndClientType(String username, ClientType clientType);

  Optional<AuthUser> findActiveByIdAndClientType(UUID userId, ClientType clientType);

  List<String> findDataScopes(List<String> roleCodes);
}
