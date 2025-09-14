package com.example.oauth2.repository;

import com.example.oauth2.model.Usuario;
import com.example.oauth2.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Usuario> usuarioRowMapper = new RowMapper<Usuario>() {
        @Override
        public Usuario mapRow(ResultSet rs, int rowNum) throws SQLException {
            Usuario usuario = new Usuario();
            usuario.setId(rs.getLong("id"));
            usuario.setNome(rs.getString("nome"));
            usuario.setEmail(rs.getString("email"));
            usuario.setLogin(rs.getString("login"));
            usuario.setSenha(rs.getString("senha"));
            usuario.setAtivo(rs.getBoolean("ativo"));
            usuario.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            usuario.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return usuario;
        }
    };

    private final RowMapper<Role> roleRowMapper = new RowMapper<Role>() {
        @Override
        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
            Role role = new Role();
            role.setId(rs.getLong("id"));
            role.setRoleName(rs.getString("role_name"));
            role.setDescricao(rs.getString("descricao"));
            role.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            role.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return role;
        }
    };

    public Optional<Usuario> findByLogin(String login) {
        String sql = "SELECT * FROM usuarios WHERE login = ? AND ativo = TRUE";
        List<Usuario> usuarios = jdbcTemplate.query(sql, usuarioRowMapper, login);

        if (usuarios.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarios.get(0);
        usuario.setRoles(findRolesByUsuarioId(usuario.getId()));
        return Optional.of(usuario);
    }

    public Optional<Usuario> findByEmail(String email) {
        String sql = "SELECT * FROM usuarios WHERE email = ? AND ativo = TRUE";
        List<Usuario> usuarios = jdbcTemplate.query(sql, usuarioRowMapper, email);

        if (usuarios.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarios.get(0);
        usuario.setRoles(findRolesByUsuarioId(usuario.getId()));
        return Optional.of(usuario);
    }

    public Optional<Usuario> findById(Long id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        List<Usuario> usuarios = jdbcTemplate.query(sql, usuarioRowMapper, id);

        if (usuarios.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarios.get(0);
        usuario.setRoles(findRolesByUsuarioId(usuario.getId()));
        return Optional.of(usuario);
    }

    public List<Role> findRolesByUsuarioId(Long usuarioId) {
        String sql = """
            SELECT r.* FROM roles r 
            INNER JOIN usuario_roles ur ON r.id = ur.role_id 
            WHERE ur.usuario_id = ?
        """;
        return jdbcTemplate.query(sql, roleRowMapper, usuarioId);
    }

    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {
            return insert(usuario);
        } else {
            return update(usuario);
        }
    }

    private Usuario insert(Usuario usuario) {
        String sql = """
            INSERT INTO usuarios (nome, email, login, senha, ativo, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        LocalDateTime now = LocalDateTime.now();
        usuario.setCreatedAt(now);
        usuario.setUpdatedAt(now);

        jdbcTemplate.update(sql,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getLogin(),
                usuario.getSenha(),
                usuario.getAtivo(),
                now,
                now
        );

        // Get the generated ID
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        usuario.setId(id);

        return usuario;
    }

    private Usuario update(Usuario usuario) {
        String sql = """
            UPDATE usuarios SET nome = ?, email = ?, login = ?, senha = ?, 
            ativo = ?, updated_at = ? WHERE id = ?
        """;

        LocalDateTime now = LocalDateTime.now();
        usuario.setUpdatedAt(now);

        jdbcTemplate.update(sql,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getLogin(),
                usuario.getSenha(),
                usuario.getAtivo(),
                now,
                usuario.getId()
        );

        return usuario;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Usuario> findAll() {
        String sql = "SELECT * FROM usuarios ORDER BY created_at DESC";
        List<Usuario> usuarios = jdbcTemplate.query(sql, usuarioRowMapper);

        for (Usuario usuario : usuarios) {
            usuario.setRoles(findRolesByUsuarioId(usuario.getId()));
        }

        return usuarios;
    }

    public boolean existsByLogin(String login) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE login = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, login);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }
}