package com.rin.hlsserver.config;

import com.rin.hlsserver.model.Genre;
import com.rin.hlsserver.model.Role;
import com.rin.hlsserver.model.User;
import com.rin.hlsserver.repository.GenreRepository;
import com.rin.hlsserver.repository.RoleRepository;
import com.rin.hlsserver.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {


    PasswordEncoder passwordEncoder;
    static String DEFAULT_ADMIN_PASSWORD = "123";
    static String DEFAULT_ADMIN_EMAIL = "nrin000@yopmail.com";
    static String DEFAULT_ADMIN_FULLNAME = "Nguyen Rin";

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository, GenreRepository genreRepository) {
        log.info("Initializing application.....");
        return args -> {
            // Tạo admin user nếu chưa có
            if(userRepository.findByEmail(DEFAULT_ADMIN_EMAIL).isEmpty()){
                Role adminRole = createAdminRoleIfNotExist(roleRepository);
                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);
                User user = new User();
                user.setEmail(DEFAULT_ADMIN_EMAIL);
                user.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                user.setRoles(roles);
                user.setFullName(DEFAULT_ADMIN_FULLNAME);
                userRepository.save(user);
                log.warn("ADMIN USER CREATED WITH EMAIL: {} AND PASSWORD: {}", DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
            }
            
            // Tạo 3 thể loại mặc định nếu chưa có
            createDefaultGenresIfNotExist(genreRepository);
        };
    }

    private Role createAdminRoleIfNotExist( RoleRepository roleRepository) {
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        return roleRepository.findById("ADMIN")
                .orElseGet(() -> roleRepository.save(adminRole));
    }

    private void createDefaultGenresIfNotExist(GenreRepository genreRepository) {
        // Tạo 3 thể loại mặc định: Action, Drama, Comedy
        createGenreIfNotExist(genreRepository, "action", "Hành Động", 
                "Phim có nhiều cảnh hành động, chiến đấu, phiêu lưu");
        createGenreIfNotExist(genreRepository, "drama", "Chính Kịch", 
                "Phim tập trung vào câu chuyện con người, cảm xúc sâu sắc");
        createGenreIfNotExist(genreRepository, "comedy", "Hài", 
                "Phim hài hước, mang lại tiếng cười cho khán giả");
        
        log.info("Default genres checked/created successfully");
    }

    private void createGenreIfNotExist(GenreRepository genreRepository, String genreId, String name, String description) {
        if (!genreRepository.existsByGenreId(genreId)) {
            Genre genre = Genre.builder()
                    .genreId(genreId)
                    .name(name)
                    .description(description)
                    .build();
            genreRepository.save(genre);
            log.info("Created default genre: {} ({})", name, genreId);
        }
    }


}