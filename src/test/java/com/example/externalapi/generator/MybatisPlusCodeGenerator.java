package com.example.externalapi.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.example.externalapi.entity.BaseEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public final class MybatisPlusCodeGenerator {

    /**
     * Tables to generate.
     *
     * Single table: new String[] {"sys_user"}
     * Multiple tables: new String[] {"sys_user", "sys_role"}
     * All tables: new String[0]
     */
    private static final String[] TABLE_NAMES = {"sys_user"};

    private MybatisPlusCodeGenerator() {
    }

    public static void main(String[] args) {
        String projectPath = resolveProjectPath();
        String url = "jdbc:mysql://localhost:3306/demo_service?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123456";

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author("codex")
                        .outputDir(projectPath + "/src/main/java")
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent("com.example.externalapi")
                        .entity("entity")
                        .mapper("mapper")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/java/com/example/externalapi/mapper")))
                .strategyConfig(builder -> builder
                        .addInclude(TABLE_NAMES)
                        .entityBuilder()
                        .superClass(BaseEntity.class)
                        .addSuperEntityColumns("create_time", "update_time", "create_by", "update_by", "deleted", "version")
                        .enableTableFieldAnnotation()
                        .enableFileOverride()
                        .formatFileName("%sEntity")
                        .mapperBuilder()
                        .enableMapperAnnotation()
                        .enableBaseResultMap()
                        .enableBaseColumnList()
                        .enableFileOverride()
                        .formatMapperFileName("%sMapper")
                        .formatXmlFileName("%sMapper")
                        .serviceBuilder()
                        .disable()
                        .controllerBuilder()
                        .disable())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private static String resolveProjectPath() {
        String configuredPath = System.getProperty("generator.projectPath");
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Paths.get(configuredPath).toAbsolutePath().normalize().toString();
        }

        Path current = Paths.get("").toAbsolutePath().normalize();
        if (Files.exists(current.resolve("pom.xml"))) {
            return current.toString();
        }

        Path module = current.resolve("springboot-service-design");
        if (Files.exists(module.resolve("pom.xml"))) {
            return module.toString();
        }

        throw new IllegalStateException("Cannot resolve project path. Use -Dgenerator.projectPath=<project-dir>.");
    }
}
