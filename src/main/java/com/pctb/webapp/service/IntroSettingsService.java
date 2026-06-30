package com.pctb.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IntroSettingsService {
    static String INTRO_SETTINGS_KEY = "settings:intro";

    RedisService redisService;
    ObjectMapper objectMapper;

    public JsonNode getIntroSettings() {
        String savedSettings = redisService.get(INTRO_SETTINGS_KEY);
        if (savedSettings != null) {
            try {
                return objectMapper.readTree(savedSettings);
            } catch (JsonProcessingException ignored) {
                redisService.delete(INTRO_SETTINGS_KEY);
            }
        }
        return defaultIntroSettings();
    }

    public JsonNode updateIntroSettings(JsonNode request) {
        if (request == null || request.isNull() || !request.isObject()) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
        try {
            redisService.set(INTRO_SETTINGS_KEY, objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
        return request;
    }

    private ObjectNode defaultIntroSettings() {
        ObjectNode root = objectMapper.createObjectNode();
        root.putObject("hero");
        root.putObject("stats");
        root.putArray("features");
        root.putArray("howTo");
        root.putObject("cta");
        return root;
    }
}
