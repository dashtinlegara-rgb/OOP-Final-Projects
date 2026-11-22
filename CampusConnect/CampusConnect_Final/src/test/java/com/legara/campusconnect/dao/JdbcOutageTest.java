package com.legara.campusconnect.dao;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.legara.campusconnect.exception.DataAccessException;

class JdbcOutageTest {

    @Test
    @DisplayName("D01 Unreachable MySQL: every DAO call fails with DataAccessException wrapping SQLException")
    void unreachableDatabase() {
        ConnectionFactory dead = new ConnectionFactory(
                "jdbc:mysql://127.0.0.1:1/campusconnect?connectTimeout=1000", "root", "none");
        JdbcRequestStore store = new JdbcRequestStore(dead);
        DataAccessException e = assertThrows(DataAccessException.class, () -> store.findAll());
        assertTrue(e.getCause() instanceof SQLException);
        assertThrows(DataAccessException.class, () -> store.findById("T0001"));
        assertThrows(DataAccessException.class, () -> store.delete("T0001"));
    }

    @Test
    @DisplayName("D02 Missing db.properties gives a clear configuration message")
    void missingConfig(@TempDir File dir) {
        String missing = new File(dir, "db.properties").getPath();
        DataAccessException e = assertThrows(DataAccessException.class, () -> ConnectionFactory.fromFile(missing));
        assertTrue(e.getMessage().contains("db.properties.example"));
    }

    @Test
    @DisplayName("D03 db.properties without a user is rejected")
    void invalidConfig(@TempDir File dir) throws Exception {
        File file = new File(dir, "db.properties");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("db.url=jdbc:mysql://localhost:3306/campusconnect\n");
        }
        assertThrows(DataAccessException.class, () -> ConnectionFactory.fromFile(file.getPath()));
    }
}
