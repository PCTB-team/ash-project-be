package com.pctb.webapp.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class LocalStorageConfig implements WebMvcConfigurer {
    @Value("${app.upload.dir:uploads}")
    String uploadDir;

    @Override
    // Cho phép truy cập file trong thư mục upload qua URL /api/v1/uploads/**.
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();

        // up load ảnh từ cục bộ theo đường dẫn upload path.
        // Spring sẽ map /uploads/** đến thư mục local được cấu hình bởi app.upload.dir.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
