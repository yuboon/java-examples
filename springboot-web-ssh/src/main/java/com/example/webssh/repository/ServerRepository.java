package com.example.webssh.repository;

import com.example.webssh.entity.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ServerRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final String INSERT_SERVER = """
        INSERT INTO servers (name, host, port, username, password, created_at, updated_at) 
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
    
    private final String SELECT_ALL_SERVERS = """
        SELECT id, name, host, port, username, password, created_at, updated_at 
        FROM servers ORDER BY created_at DESC
        """;
    
    private final String SELECT_SERVER_BY_ID = """
        SELECT id, name, host, port, username, password, created_at, updated_at 
        FROM servers WHERE id = ?
        """;
    
    private final String UPDATE_SERVER = """
        UPDATE servers SET name=?, host=?, port=?, username=?, password=?, updated_at=? 
        WHERE id=?
        """;
    
    private final String DELETE_SERVER = "DELETE FROM servers WHERE id = ?";
    
    public Long saveServer(ServerConfig server) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_SERVER, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, server.getName());
            ps.setString(2, server.getHost());
            ps.setInt(3, server.getPort());
            ps.setString(4, server.getUsername());
            ps.setString(5, server.getPassword());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey().longValue();
    }
    
    public List<ServerConfig> findAllServers() {
        return jdbcTemplate.query(SELECT_ALL_SERVERS, this::mapRowToServer);
    }
    
    public Optional<ServerConfig> findServerById(Long id) {
        try {
            ServerConfig server = jdbcTemplate.queryForObject(SELECT_SERVER_BY_ID, 
                    this::mapRowToServer, id);
            return Optional.ofNullable(server);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    public void updateServer(ServerConfig server) {
        jdbcTemplate.update(UPDATE_SERVER,
                server.getName(),
                server.getHost(), 
                server.getPort(),
                server.getUsername(),
                server.getPassword(),
                Timestamp.valueOf(LocalDateTime.now()),
                server.getId());
    }
    
    public void deleteServer(Long id) {
        jdbcTemplate.update(DELETE_SERVER, id);
    }
    
    private ServerConfig mapRowToServer(ResultSet rs, int rowNum) throws SQLException {
        ServerConfig server = new ServerConfig();
        server.setId(rs.getLong("id"));
        server.setName(rs.getString("name"));
        server.setHost(rs.getString("host"));
        server.setPort(rs.getInt("port"));
        server.setUsername(rs.getString("username"));
        server.setPassword(rs.getString("password"));
        server.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        server.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return server;
    }
}