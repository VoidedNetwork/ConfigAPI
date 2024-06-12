# ConfigAPI

All in one, cross-platform, yaml config api.

Shout out to dejvokep for [boosted yaml](https://github.com/dejvokep/boosted-yaml),
it is used as a dependency in this api.

## Support

If you need any assistance using or installing my ConfigAPI,
feel free to contact me by either adding me on discord (@J4C0B3Y)
or by creating an issue and explaining your problem or question.

## Installation

Prebuilt jars can be found in [releases](https://github.com/VoidedNetwork/ConfigAPI/releases).

> **NOTE:** <br/>
> You can copy-paste the class into your project if
> you want to rename it or change it in any way.
> Make sure you keep the credit at the top of the file.

### Maven & Gradle

Replace `VERSION` with the latest release version on GitHub.

```kts
repositories {
    maven("https://jitpack.io/")
}

dependencies {
    implementation("com.github.VoidedNetwork:ConfigAPI:VERSION")
}
```

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.VoidedNetwork</groupId>
        <artifactId>ConfigAPI</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```

### Building

1. Clone this repository and enter its directory.
2. Run the intellij build configuration by clicking the top right icon.
3. Alternatively you can run `gradle delete classes shadow copy clean`.
4. The output jar file will be located in the `jars` directory.

## Usage

To make a config file, all you have to do is extend the `Config` class.

```java
public class Settings extends Config {
    public Settings(JavaPlugin plugin) throws IOException {
        // Provide a file to load / save from.
        super(new File(plugin.getDataFolder(), "settings.yml"));
    }
}
```

Next you can define your config values, each field
or class must be exclusively public and static.

```java
public class Settings extends Config {
    // ...
    
    public static boolean ENABLED = true;
    public static int SOMETHING = 2;
    
    public static class TEST {
        public static double NUMBER = 6.3;
        public static String MESSAGE = "whatever";
        public static boolean FORCE = false;
    }
}
```

This will result in this being saved to the file:

```yml
enabled: false
something: 2

test:
  number: 6.3
  message: whatever
```

You can access the config values anywhere in your code, statically.

They are updated with the values from the file when the config is
initialized or reloaded, you can even reassign them and call `Config#save()`.

```java
if (Settings.TEST.FORCE) {
    player.sendMessage(Settings.TEST.MESSAGE);
}
```

Lastly you need to initialize your config, 
here is how you can do it with a minecraft plugin.

```java
public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            this.settings = new Settings(this);
        } catch (IOException exception) {
            getLogger().severe("Failed to initialize config");
        }
    }
}
```

### Annotations

```java
public class Kits extends Config {
    // ...

    // Changes the key of a class or field to the specified value.
    @Key("test")
    // Prevents a class or field from being saved to the file.
    @Ignore 
    // Doesn't save the value to the config but still loads it,
    // if a class is hidden and its fields aren't, the fields
    // will appear if the class key is added manually to the file.
    @Hidden 
    public static class WHATEVER {
        // Always resets the value in the file to the default specified.
        // This can only be on fields and not classes.
        @Final
        public static int SOMETHING = 4;
    }
}
```

### Advanced Usage

You can set optional config behaviour in the constructor.

```java
public Settings(JavaPlugin plugin) throws IOException {
    // ...
    
    // This is false by default.
    setRemoveUnrecognised(true);
}
```

You can also change the way keys are formatted from the field / class names.

```java
public class Settings extends Config {
    // ...
    
    @Override
    protected String formatKey(String key) {
        // This is the default formatter.
        return key.toLowerCase().replace("_", "-");
    }
}
```

### Want more?

Each method and field in my config api has detailed java
docs explaining what everything does and how it works.

> Made with ❤ // J4C0B3Y 2024