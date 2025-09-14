package com.example.oauth2server.model;

import com.example.oauth2.model.Role;
import com.example.oauth2.model.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    void usuario_ShouldBeCreatedWithCorrectProperties() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Role role = new Role();
        role.setId(1L);
        role.setName("ADMIN");

        // When
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");
        usuario.setPassword("hashedpassword");
        usuario.setEmail("test@example.com");
        usuario.setEnabled(true);
        usuario.setCreatedAt(now);
        usuario.setUpdatedAt(now);
        usuario.setRoles(Set.of(role));

        // Then
        assertThat(usuario.getId()).isEqualTo(1L);
        assertThat(usuario.getUsername()).isEqualTo("testuser");
        assertThat(usuario.getPassword()).isEqualTo("hashedpassword");
        assertThat(usuario.getEmail()).isEqualTo("test@example.com");
        assertThat(usuario.isEnabled()).isTrue();
        assertThat(usuario.getCreatedAt()).isEqualTo(now);
        assertThat(usuario.getUpdatedAt()).isEqualTo(now);
        assertThat(usuario.getRoles()).hasSize(1);
        assertThat(usuario.getRoles().iterator().next().getName()).isEqualTo("ADMIN");
    }

    @Test
    void usuario_ShouldHandleEqualsAndHashCode() {
        // Given
        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setUsername("testuser");

        Usuario usuario2 = new Usuario();
        usuario2.setId(1L);
        usuario2.setUsername("testuser");

        Usuario usuario3 = new Usuario();
        usuario3.setId(2L);
        usuario3.setUsername("otheruser");

        // Then
        assertThat(usuario1).isEqualTo(usuario2);
        assertThat(usuario1).isNotEqualTo(usuario3);
        assertThat(usuario1.hashCode()).isEqualTo(usuario2.hashCode());
    }

    @Test
    void usuario_ShouldHandleToString() {
        // Given
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");
        usuario.setEmail("test@example.com");

        // When
        String toString = usuario.toString();

        // Then
        assertThat(toString).contains("testuser");
        assertThat(toString).contains("test@example.com");
    }
}