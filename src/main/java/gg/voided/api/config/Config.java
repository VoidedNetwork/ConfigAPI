package gg.voided.api.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.Block;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.FlowStyle;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All-in-one config utility.
 *
 * @author J4C0B3Y
 * @version ConfigAPI
 * @since 25/05/2024
 */
public class Config {
    /**
     * The modifiers that should exclusively be on a field or class.
     */
    private final static int MODIFIERS = Modifier.PUBLIC | Modifier.STATIC;

    /**
     * Maps the key / path of each value to its corresponding static field.
     */
    private final Map<String, Field> fields = new HashMap<>();

    /**
     * The file to save / load from.
     */
    @Getter private final File file;

    /**
     * The underlying yaml document.
     */
    @Getter private final YamlDocument document;

    /**
     * Should all unrecognised values get
     * removed when the file is saved.
     */
    @Setter private boolean removeUnrecognised = false;

    /**
     * If the config was loaded without errors.
     */
    private boolean success = true;

    /**
     * Creates and loads configuration
     * from the provided file and defaults.
     *
     * @param file The file to create and load from.
     * @param defaults The default configuration file.
     * @throws IOException If there was an IO exception.
     */
    public Config(File file, InputStream defaults) throws IOException {
        GeneralSettings general = GeneralSettings.builder()
            .setUseDefaults(false)
            .build();

        LoaderSettings loader = LoaderSettings.builder()
            .setAllowDuplicateKeys(false)
            .build();

        DumperSettings dumper = DumperSettings.builder()
            .setFlowStyle(FlowStyle.BLOCK)
            .setIndicatorIndentation(2)
            .build();

        this.file = file;
        this.document = YamlDocument.create(file, defaults, general, loader, dumper);

        reload();
    }

    /**
     * Creates and loads configuration
     * from the provided file.
     *
     * @param file The file to create and load from.
     * @throws IOException If there was an IO exception.
     */
    public Config(File file) throws IOException {
        this(file, null);
    }

    /**
     * Saves a class's fields to the yaml document.
     *
     * @param parent The class to save fields from.
     * @param path Prefixes the key in the document.
     */
    private void save(Class<?> parent, String path) {
        for (Field field : parent.getDeclaredFields()) {
            if (field.getModifiers() != MODIFIERS || field.isAnnotationPresent(Ignore.class)) continue;

            String key = path + getKey(field.getAnnotation(Key.class), field.getName());
            this.fields.put(key, field);

            if (document.get(key) == null && !field.isAnnotationPresent(Hidden.class) || field.isAnnotationPresent(Final.class)) {
                try {
                    document.set(key, field.get(parent));
                } catch (IllegalAccessException exception) {
                    throw new RuntimeException("Failed to get field for key '" + key + "'", exception);
                }
            }

            setComment(document.getBlock(key), field.getAnnotation(Comment.class));
        }

        for (Class<?> clazz : parent.getDeclaredClasses()) {
            if (clazz.getModifiers() != MODIFIERS || clazz.isAnnotationPresent(Ignore.class)) continue;

            String key = path + getKey(clazz.getAnnotation(Key.class), clazz.getSimpleName());
            Section section = document.getSection(key);

            if (section == null && !clazz.isAnnotationPresent(Hidden.class)) {
                section = document.createSection(key);
            }

            setComment(section, clazz.getAnnotation(Comment.class));

            if (section != null) {
                save(clazz, key + ".");
            }
        }
    }

    /**
     * Saves the config to the yaml document.
     *
     * @throws IOException The YamlDocument's IO exception.
     */
    public void save() throws IOException {
        try {
            if (!this.success) {
                throw new RuntimeException("Cannot save until successful load, check above for errors.");
            }

            if (removeUnrecognised) {
                document.clear();
            }

            save(getClass(), "");
            document.save();
        } catch (Exception exception) {
            throw new IOException("Save failed for file '" + file.getName() + "'.", exception);
        }
    }

    /**
     * Updates the config fields with the yaml document values.
     *
     * @throws IOException The YamlDocument's IO exception.
     */
    public void reload() throws IOException {
        document.reload();
        save();

        for (Map.Entry<String, Field> entry : this.fields.entrySet()) {
            String key = entry.getKey();
            Field field = entry.getValue();

            try {
                Class<?> type = field.get(null).getClass();
                Object value = document.get(key);

                if (!type.isAssignableFrom(value.getClass())) {
                    throw new RuntimeException(
                        "Expected type '" + type.getSimpleName() +
                        "' but found '" + value.getClass().getSimpleName() + "'."
                    );
                }

                field.set(null, value);
            } catch (Exception exception) {
                this.success = false;
                throw new RuntimeException(
                    "Load failed for key '" + key +
                    "' in file '" + file.getName() + "'.",
                    exception
                );
            }
        }

        this.success = true;
    }

    /**
     * Formats a field / class name into a key.
     *
     * @param key The field / class name.
     * @return The formatted key.
     */
    protected String formatKey(String key) {
        return key.toLowerCase().replace("_", "-");
    }

    /**
     * Uses the annotation key if present or
     * formats the class / field name if not.
     *
     * @param annotation The key annotation.
     * @param name The class / field name.
     * @return The key.
     */
    private String getKey(Key annotation, String name) {
        if (annotation == null || annotation.value().isEmpty()) {
            return formatKey(name);
        } else {
            return annotation.value();
        }
    }

    /**
     * Sets the comment if the annotation is present.
     *
     * @param block The yaml block.
     * @param annotation The comment annotation.
     */
    private void setComment(Block<?> block, Comment annotation) {
        if (block == null || annotation == null) return;

        List<String> comment = new ArrayList<>();

        for (String line : annotation.value()) {
            comment.add(" " + line);
        }

        block.setComments(comment);
    }

    /**
     * Change the key to use for saving.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    protected @interface Key {
        /**
         * @return The key to use for saving.
         */
        String value();
    }

    /**
     * Set the comment for a yaml block.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    protected @interface Comment {
        /**
         * @return The comment.
         */
        String[] value();
    }

    /**
     * Doesn't add a class / field to the
     * file but still loads values from it.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    protected @interface Hidden {
    }

    /**
     * Marks a class / field to not
     * be treated as a config value.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    protected @interface Ignore {
    }

    /**
     * Marks a field for its value
     * to always be reset to default.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface Final {
    }
}
