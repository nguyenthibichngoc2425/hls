package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
