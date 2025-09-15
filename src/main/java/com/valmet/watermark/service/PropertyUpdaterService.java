package com.valmet.watermark.service;

import com.valmet.watermark.config.WatermarkSettings;
import com.valmet.watermark.enums.ResultCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

import static com.valmet.watermark.response.WatermarkResponseUtil.throwApplicationException;

@Service
@Slf4j
public class PropertyUpdaterService {
    private final WatermarkSettings watermarkSettings;
    Path path = null;

    public PropertyUpdaterService (WatermarkSettings watermarkSettings) {
        this.watermarkSettings = watermarkSettings;
    }

    public String updateProperty (String key, String value) throws IOException {
        Properties properties = getProperties ();
        // Update the property
        if (!properties.containsKey (key)) {
            throwApplicationException (ResultCodeConstants.INVALID_PROPERTY_KEY);
        }
        log.info ("Updated key: {}, value: {}", key, value);
        properties.setProperty (key, value);
        // Save back to the file
        try (OutputStream output = new FileOutputStream (path.toFile ())) {
            properties.store (output, null);
        }
        if ("watermark.settings.opacity".equalsIgnoreCase (key)) {
            watermarkSettings.setOpacity (Integer.parseInt (value));
        } else if ("watermark.settings.logoOpacity".equalsIgnoreCase (key)) {
            watermarkSettings.setLogoOpacity (Integer.parseInt (value));
        } else if ("watermark.settings.colorCode".equalsIgnoreCase (key)) {
            watermarkSettings.setColorCode (value);
        } else if ("watermark.settings.xAxis".equalsIgnoreCase (key)) {
            watermarkSettings.setXAxis (Integer.parseInt (value));
        } else if ("watermark.settings.yAxis".equalsIgnoreCase (key)) {
            watermarkSettings.setYAxis (Integer.parseInt (value));
        } else if ("watermark.settings.fontName".equalsIgnoreCase (key)) {
            watermarkSettings.setFontName (value);
        } else if ("watermark.settings.fontStyle".equalsIgnoreCase (key)) {
            watermarkSettings.setFontStyle (value);
        }

        return "Property updated successfully!";
    }

    public Properties getProperties () throws IOException {
        ClassPathResource propertiesFile = new ClassPathResource ("watermark-settings.properties");
        Properties properties = new Properties ();
        if (propertiesFile.exists ()) {
            path = propertiesFile.getFile ().toPath ();
            try (InputStream input = new FileInputStream (path.toFile ())) {
                properties.load (input);
            }
        }

        return properties;
    }
}
