package com.valmet.watermark.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class for watermark settings.
 * <p>
 * This class maps properties from the `watermark-settings.properties` file into
 * fields that define the appearance and positioning of watermarks. The
 * properties are prefixed with `watermark.settings` and are loaded into this
 * class automatically at runtime.
 * </p>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Defines properties for watermark opacity, color, font, and position.</li>
 * <li>Uses {@link ConfigurationProperties} to bind properties from the external
 * configuration file.</li>
 * <li>Leverages Lombok's {@link Data} annotation to generate getters, setters,
 * and other utility methods.</li>
 * </ul>
 *
 * <p>
 * The class is annotated with:
 * </p>
 * <ul>
 * <li>{@link Configuration}: Marks this class as a Spring configuration
 * bean.</li>
 * <li>{@link PropertySource}: Specifies the external properties file to load
 * (`watermark-settings.properties`).</li>
 * <li>{@link ConfigurationProperties}: Maps properties with the prefix
 * `watermark.settings` to the fields of this class.</li>
 * <li>{@link Data}: Generates boilerplate code like getters, setters,
 * `toString`, and `equals` methods.</li>
 * </ul>
 *
 * <p>
 * <b>Example Configuration in `watermark-settings.properties`:</b>
 * </p>
 *
 * <pre>
 * watermark.settings.opacity=0.5
 * watermark.settings.logoOpacity=0.8
 * watermark.settings.colorCode=#000000
 * watermark.settings.xAxis=100
 * watermark.settings.yAxis=200
 * watermark.settings.fontName=Arial
 * watermark.settings.fontStyle=Bold
 * </pre>
 *
 * <p>
 * <b>Usage:</b>
 * </p>
 * <ol>
 * <li>Ensure that the `watermark-settings.properties` file is present in the
 * `classpath`.</li>
 * <li>Inject this class into components or services that require watermark
 * settings.</li>
 * <li>Access individual properties using the generated getter methods or
 * directly as fields in Spring-managed beans.</li>
 * </ol>
 *
 * @author BJIT
 * @version 1.0
 */
@Configuration
@PropertySource ("classpath:watermark-settings.properties")
@ConfigurationProperties (prefix = "watermark.settings")
@Data
public class WatermarkSettings {
    /**
     * The opacity of the watermark text (0.0 to 1.0).
     */
    private float opacity;
    /**
     * The opacity of the logo in the watermark (0.0 to 1.0).
     */
    private float logoOpacity;
    /**
     * The color code of the watermark text in hexadecimal format (e.g., #000000 for
     * black).
     */
    private String colorCode;
    /**
     * The x-axis offset for the watermark position.
     */
    private int xAxis;
    /**
     * The y-axis offset for the watermark position.
     */
    private int yAxis;
    /**
     * The font name to use for the watermark text (e.g., Arial, Times New Roman).
     */
    private String fontName;
    /**
     * The font style to use for the watermark text (e.g., Bold, Italic, Plain).
     */
    private String fontStyle;
}
