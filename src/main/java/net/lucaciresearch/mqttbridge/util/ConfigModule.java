package net.lucaciresearch.mqttbridge.util;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.lucaciresearch.mqttbridge.mqtt.MqttConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ConfigModule<DCTy> extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(ConfigModule.class);

    private final String configPath;
    private MqttConfig mqttConfig;
    private DCTy deviceConfig;
    private Config<DCTy> masterConfig;
    private String deviceCodename;

    public ConfigModule(String configPath) {
        this.configPath = configPath;
    }

    public boolean initialize(boolean readJustCodename, TypeReference<Config<DCTy>> typeReference) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        Config<DCTy> config;
        try {
            if (readJustCodename) {
                JsonNode tree = mapper.readTree(new File(configPath));
                if (!tree.isObject()) {
                    logger.error("Config file root is not an json object");
                    return false;
                }
                JsonNode codenameNode = tree.get("deviceCodename");
                if (codenameNode == null) {
                    logger.error("Config file missing deviceCodename field");
                    return false;
                }
                if (!codenameNode.isValueNode()) {
                    logger.error("Config file codenameNode is not a value field");
                    return false;
                }
                if (!codenameNode.isTextual()) {
                    logger.error("Config file codenameNode is not a text field");
                    return false;
                }
                deviceCodename = codenameNode.textValue();
            }
            config = mapper.readValue(new File(configPath), typeReference);
        } catch (IOException e) {
            logger.error("Failed to read configuration file: {}", e.getMessage());
            return false;
        }
        if (config == null) {
            logger.error("Config file is null");
            return false;
        }
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<Config<DCTy>>> violations = validator.validate(config);
            if (!violations.isEmpty()) {
                for (ConstraintViolation<Config<DCTy>> v : violations) {
                    logger.error("Config error: {} {}", v.getPropertyPath(), v.getMessage());
                }
                return false;
            }
        }
        mqttConfig = config.mqtt();
        deviceConfig = config.device();
        masterConfig = config;
        return true;
    }

    @Provides
    public MqttConfig getMqtt() {
        return mqttConfig;
    }

    @Provides
    public Config getConfig() {
        return masterConfig;
    }

    public DCTy getDevice() {
        return deviceConfig;
    }

    @Provides
    public String getDeviceCodename() { return deviceCodename; }

}
