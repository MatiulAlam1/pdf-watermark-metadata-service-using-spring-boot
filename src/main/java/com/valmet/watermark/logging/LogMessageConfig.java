package com.valmet.watermark.logging;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

@Configuration
public class LogMessageConfig implements ApplicationContextAware {

    /**
     * Resource name of the JSON file.
     */
    private static final String MESSAGE_NAME = "error-message-info.json";

    /**
     * Key for the level.
     */
    private static final String KEY_LEVEL = "level";

    /**
     * Key for the message.
     */
    private static final String KEY_MESSAGE = "message";
    private static ApplicationContext context;
    /**
     * JsonObject that holds error-message-info.json.
     */
    private final JsonObject config;

    /**
     * Constructor.
     */
    @Autowired
    public LogMessageConfig (Gson gson) {
        JsonReader reader = new JsonReader (new InputStreamReader (
                Objects.requireNonNull (this.getClass ().getClassLoader ().getResourceAsStream (MESSAGE_NAME)),
                StandardCharsets.UTF_8));
        this.config = gson.fromJson (reader, JsonObject.class);
    }

    /**
     * Accessing ApplicationContext from Non Spring Class
     *
     * @return
     */
    public static ApplicationContext getApplicationContext () {
        return context;
    }

    @Override
    public void setApplicationContext (ApplicationContext appContext) throws BeansException {
        context = appContext;
    }

    /**
     * Get error message information by error id.
     *
     * @param errorId Error id
     * @return Log level and message of the error id
     * @throws NoSuchElementException thrown if not found.
     */
    public ErrorMessageInfo getErrorMessageInfo (String errorId) {

        JsonElement infoObject = config.get (errorId);

        if (infoObject == null) {
            throw new NoSuchElementException ("Could not find corresponding value of the ErrorCode '" + errorId + "'");
        }

        JsonObject erroJsonObject = infoObject.getAsJsonObject ();
        return new ErrorMessageInfo (LogLevel.valueOf (erroJsonObject.get (KEY_LEVEL).getAsString ()),
                erroJsonObject.get (KEY_MESSAGE).getAsString ());
    }
}
