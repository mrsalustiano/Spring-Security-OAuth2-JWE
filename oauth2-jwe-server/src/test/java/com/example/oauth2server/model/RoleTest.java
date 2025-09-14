package com.example.oauth2server.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void role_ShouldBeCreatedWithCorrectProperties() {
        // Given & When
        Role role = new Role();
        role.setId(1L);
        role.setName("ADMIN");
        role.setDescription("Administrator role");

        // Then
        assertThat(role.getId()).isEqualTo(1L);
        assertThat(role.getName()).isEqualTo("ADMIN");
        assertThat(role.getDescription()).isEqualTo("Administrator role");
    }

    @Test
    void role_ShouldHandleUsersRelationship() {
        // Given
        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setUsername("user1");

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setUsername("user2");

        // When
        role.setUsers(Set.of(usuario1, usuario2));

        // Then
        assertThat(role.getUsers()).hasSize(2);
        assertThat(role.getUsers()).extracting(Usuario::getUsername)
                .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void role_ShouldHandleEqualsAndHashCode() {
        // Given
        Role role1 = new Role();
        role1.setId(1L);
        role1.setName("ADMIN");

        Role role2 = new Role();
        role2.setId(1L);
        role2.setName("ADMIN");

        Role role3 = new Role();
        role3.setId(2L);
        role3.setName("USER");

        // Then
        assertThat(role1).isEqualTo(role2);
        assertThat(role1).isNotEqualTo(role3);
        assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
    }
}