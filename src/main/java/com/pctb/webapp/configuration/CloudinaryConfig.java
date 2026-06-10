package com.pctb.webapp.configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    // Điền đúng tên cloud của bạn là testcloud từ file application.properties
    @Value("${app.cloudinary.cloud-name:dyvwr1u7e}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:295667836828874}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:z-A_n1X3XDrTSXY1oKWdTjzL6pw}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}