package com.example.externalapi.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class CreateDemoOrderTable {

    private CreateDemoOrderTable() {
    }

    public static void main(String[] args) throws Exception {
        String url = System.getProperty("db.url",
                "jdbc:mysql://localhost:3306/demo_service?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
        String username = System.getProperty("db.username", "root");
        String password = System.getProperty("db.password", "123456");
        Path sqlPath = resolveProjectPath().resolve("src/main/resources/sql/demo_order.sql");
        String sql = Files.readString(sqlPath, StandardCharsets.UTF_8);

        try (Connection connection = DriverManager.getConnection(url, username, password);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }

        System.out.println("demo_order table created successfully.");
    }

    private static Path resolveProjectPath() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(current.resolve("pom.xml"))) {
            return current;
        }
        Path module = current.resolve("springboot-service-design");
        if (Files.exists(module.resolve("pom.xml"))) {
            return module;
        }
        throw new IllegalStateException("Cannot resolve project path.");
    }
}
