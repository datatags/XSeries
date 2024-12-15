import com.cryptomorin.xseries.*;
import com.cryptomorin.xseries.base.XBase;
import com.cryptomorin.xseries.base.XRegistry;
import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.mojang.MojangAPI;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.cryptomorin.xseries.profiles.objects.transformer.ProfileTransformer;
import com.cryptomorin.xseries.reflection.XReflection;
import com.github.cryptomorin.test.ReflectionTests;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public final class XSeriesTests {
    private static final Path DESKTOP = Paths.get(System.getProperty("user.home") + "/Desktop/");
    /**
     * This sends unnecessary requests to Mojang and also delays out work too,
     * so let's not test when it's not needed.
     */
    private static final boolean TEST_MOJANG_API = false;

    private XSeriesTests() {}

    private static void print(String str) {
        System.out.println(str);
    }

    private static void err(String str) {
        System.err.println(str);
    }

    // @Test
    public void enumToRegistry() {
        URL resource = XSeriesTests.class.getResource("XEnchantment.java");
        try {
            Path path = Paths.get(resource.toURI());
            ClassConverter.enumToRegistry(path, DESKTOP);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void test() {
        print("\n\n\nTest begin...");

        print("Writing enum differences...");
        DifferenceHelper.versionDifference();

        testXMaterial();
        testXSound();
        testXPotion();
        testXEnchantment();
        testXItemStack();
        testXAttribute();
        testXParticle();

        testXTag();
        testReflection();

        if (TEST_MOJANG_API) testSkulls();
        else {
            try {
                Class.forName("com.cryptomorin.xseries.profiles.mojang.MojangAPI");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        print("\n\n\nTest end...");
    }

    private static void testReflection() {
        print("Testing reflection...");
        print("Version pack: " + XReflection.getVersionInformation());
        ReflectionTests.parser();
        if (XReflection.supports(12)) initializeReflection();

        print("Testing XWorldBorder...");
        Iterator<World> worldIter = Bukkit.getWorlds().iterator();
        if (worldIter.hasNext()) XWorldBorder.of(worldIter.next().getSpawnLocation());
        else {
            double init = XWorldBorder.MAX_SIZE;
            System.err.println("Cannot test XWorldBorder because no worlds are loaded.");
        }
    }

    private static void testXTag() {
        print("Testing XTag...");
        assertPresent(XTag.getTag("INVENTORY_NOT_DISPLAYABLE"));
        assertTrue(XTag.DEBUFFS.isTagged(XPotion.POISON));
        assertTrue(XTag.EFFECTIVE_SMITE_ENTITIES.isTagged(XEntityType.ZOMBIE));
        assertTrue(XTag.CORALS.isTagged(XMaterial.TUBE_CORAL));
        assertTrue(XTag.LOGS_THAT_BURN.isTagged(XMaterial.STRIPPED_ACACIA_LOG));
        assertFalse(XTag.ANVIL.isTagged(XMaterial.BEDROCK));
    }

    private static void testXParticle() {
        if (XReflection.supports(9)) {
            print("Testing particles...");
            ParticleDisplay.of(XParticle.CLOUD).
                    withLocation(new Location(null, 1, 1, 1))
                    .rotate(90, 90, 90).withCount(-1).offset(5, 5, 5).withExtra(1).forceSpawn(true);
            commonRegistryTest(XParticle.REGISTRY, Arrays.asList(values(Particle.class)));
        }
    }

    @SuppressWarnings("unchecked")
    private static <E> E[] values(Class<?> clazz) {
        try {
            return (E[]) XReflection.of(clazz).method()
                    .asStatic().named("values").returns(XReflection.of(clazz).asArray())
                    .reflect().invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void testXAttribute() {
        if (XReflection.supports(9)) {
            print("Testing XAttribute...");
            print("Attribute " + XAttribute.ARMOR_TOUGHNESS + " is " + XAttribute.ARMOR_TOUGHNESS.get());
            assertPresent(XAttribute.of("MAX_HEALTH"));
            commonRegistryTest(XAttribute.REGISTRY, Arrays.asList(values(Attribute.class)));
        }
    }

    private static void testXSound() {
        print("Testing XSound...");
        assertPresent(XSound.of("BLOCK_ENCHANTMENT_TABLE_USE"));
        assertPresent(XSound.of("AMBIENCE_CAVE"));
        assertPresent(XSound.of("RECORD_11"));
        commonRegistryTest(XSound.REGISTRY, Arrays.asList(values(Sound.class)));
    }

    private static void testXPotion() {
        print("Testing XPotion...");
        assertPresent(XPotion.of("INVIS"));
        assertPresent(XPotion.of("AIR"));
        assertPresent(XPotion.of("BLIND"));
        assertPresent(XPotion.of("DAMAGE_RESISTANCE"));
        commonRegistryTest(XPotion.REGISTRY, Arrays.asList(values(PotionEffectType.class)));
        for (PotionType potionType : PotionType.values()) {
            switch (potionType) {
                // These aren't really "effects" they're literally just potion types without an effect.
                case WATER:
                case THICK:
                case AWKWARD:
                case MUNDANE:

                case TURTLE_MASTER:
                case LONG_TURTLE_MASTER:
                case STRONG_TURTLE_MASTER:
                    continue;
            }
            if (potionType.name().equals("UNCRAFTABLE")) continue; // Present in 1.12

            String bukkitName = potionType.name();

            assertNotNull(XPotion.of(potionType), () -> "null for (Bukkit -> XForm): " + potionType);
            assertPresent(XPotion.of(bukkitName), "null for (String -> XForm): " + bukkitName);
        }
    }

    private static void testXEnchantment() {
        print("Testing XEnchantment...");
        assertPresent(XEnchantment.of("EFFICIENCY"));
        print("XENchants: " + Enchantment.KNOCKBACK + ':' + Enchantment.KNOCKBACK.getClass().getName() + ':' + Enchantment.KNOCKBACK.hashCode());
        print("XENchants: " + XEnchantment.REGISTRY.bukkitMapping().keySet().stream().map(x -> x.toString() + ':' + x.getClass().getName() + ':' + x.hashCode()).collect(Collectors.joining(", ")));
        assertNotNull(XEnchantment.of(Enchantment.KNOCKBACK));
        if (XReflection.supports(11)) assertNotNull(XEnchantment.of(Enchantment.SWEEPING_EDGE));
        commonRegistryTest(XEnchantment.REGISTRY, Arrays.asList(values(Enchantment.class)));
    }

    private static <XForm extends XBase<XForm, BukkitForm>, BukkitForm> void commonRegistryTest(
            XRegistry<XForm, BukkitForm> xRegistry,
            Collection<BukkitForm> bukkitRegistry
    ) {
        for (BukkitForm bukkitForm : bukkitRegistry) {
            if (bukkitForm == null) {
                print("Detected null standard field for: " + xRegistry);
                continue;
            }
            if (bukkitForm.toString().startsWith("LEGACY_")) {
                print("Skipping legacy bukkit form: " + xRegistry.getName() + "::" + bukkitForm);
                continue;
            }
            String bukkitName = XRegistry.getBukkitName(bukkitForm);

            assertNotNull(xRegistry.getByBukkitForm(bukkitForm), () -> "null for (Bukkit -> XForm): " + bukkitForm);
            assertPresent(xRegistry.getByName(bukkitName), "null for (String -> XForm): " + bukkitName);
        }
        for (XForm xForm : xRegistry) {
            assertNotNull(xRegistry.getByBukkitForm(xForm.get()), () -> "null for (Bukkit -> XForm): " + xForm.get() + " -> " + xForm);
            assertPresent(xRegistry.getByName(xForm.name()), "null for (String -> XForm): " + xForm.name());
        }
    }

    private static void testXMaterial() {
        print("Testing XMaterial...");
        assertPresent(XMaterial.matchXMaterial("AIR"));
        assertSame(XMaterial.matchXMaterial("CLAY_BRICK"), XMaterial.BRICK);
        assertMaterial("MELON", "MELON");

        if (XMaterial.supports(14)) {
            assertMaterial(XMaterial.RED_DYE, Material.RED_DYE);
            assertMaterial(XMaterial.GREEN_DYE, Material.GREEN_DYE);
            assertMaterial(XMaterial.BLACK_DYE, Material.BLACK_DYE);

            assertMaterial("RED_BED", "RED_BED");
            assertMaterial("GREEN_CONCRETE_POWDER", "CONCRETE_POWDER:13");
        } else if (XMaterial.supports(13)) {
            print("Black dye is " + XMaterial.BLACK_DYE.get() + " - " + XMaterial.BLACK_DYE.parseItem());
            assertMaterial(XMaterial.CYAN_DYE, Material.CYAN_DYE);
            assertMaterial(XMaterial.GREEN_DYE, Material.valueOf("CACTUS_GREEN"));

            // BLACK_DYE doesn't exist in 1.13 but INK_SACK does.
            // So naturally, INK_SAC:0 gets mapped to XMaterial.INK_SAC because
            // INK_SAC remains a material for later versions.
            // But should we expect it to get mapped to BLACK_DYE instead?
            // Well, no and yes. The conclusion is that we just chose for
            // it to get mapped to INK_SAC because that'd make more sense on this version.
            // So this would happen: XMaterial.BLACK_DYE -> Material.INK_SACK -> XMaterial.INK_SAC
            Material inkSack = Material.valueOf("INK_SAC");
            Assertions.assertSame(XMaterial.BLACK_DYE.get(), inkSack);
            Assertions.assertSame(XMaterial.matchXMaterial("BLACK_DYE").get(), XMaterial.BLACK_DYE);
            Assertions.assertSame(XMaterial.matchXMaterial("INK_SAC").get(), XMaterial.INK_SAC);

            assertMaterial(XMaterial.INK_SAC, inkSack);
        } else {
            Material inkSack = XMaterial.INK_SAC.get(); // INK_SACK for 1.8
            assertMaterial(XMaterial.CYAN_DYE, inkSack);
            assertMaterial(XMaterial.GREEN_DYE, inkSack);
            assertMaterial(XMaterial.BLACK_DYE, inkSack);
        }

        // assertFalse(XMaterial.MAGENTA_TERRACOTTA.isOneOf(Arrays.asList("GREEN_TERRACOTTA", "BLACK_BED", "DIRT")));
        // assertTrue(XMaterial.BLACK_CONCRETE.isOneOf(Arrays.asList("RED_CONCRETE", "CONCRETE:15", "CONCRETE:14")));
        // commonRegistryTest(XMaterial.REGISTRY, Arrays.asList(Material.values()));
        for (Material material : Material.values())
            if (!material.name().startsWith("LEGACY")) XMaterial.matchXMaterial(material);
    }

    private static void assertMaterial(XMaterial original, Material expect) {
        assertSame(XMaterial.matchXMaterial(original.name()), original);
        Assertions.assertSame(original.get(), expect);
        Assertions.assertSame(XMaterial.matchXMaterial(original.parseItem()), original);
        Assertions.assertSame(XMaterial.matchXMaterial(XMaterial.matchXMaterial(original.name()).get().parseItem()), original);
    }

    private static void testXItemStack() {
        print("Testing XItemStack...");
        try {
            serializeItemStack();
            deserializeItemStack();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deserializeItemStack() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(ResourceHelper.getResourceAsFile("itemstack.yml"));

        for (String section : yaml.getKeys(false)) {
            ConfigurationSection itemSection = yaml.getConfigurationSection(section);
            ItemStack item = XItemStack.deserialize(itemSection);
            print("[Item] " + section + ": " + item);
        }
    }

    private static ItemStack createItem(XMaterial material, String name, Consumer<ItemMeta> metaConsumer) {
        ItemStack item = material.parseItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        metaConsumer.accept(meta);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("CodeBlock2Expr")
    private static void serializeItemStack() throws IOException {
        File file = new File(Bukkit.getWorldContainer(), "serialized.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        List<ItemStack> items = new ArrayList<>();
        items.add(createItem(XMaterial.DIAMOND, "Diamonds", meta -> {
            meta.setLore(Arrays.asList("Line 1", "", "Line 2"));
        }));
        items.add(createItem(XMaterial.PLAYER_HEAD, "head-notch", meta -> {
            XSkull.of(meta).profile(
                    Profileable.username("Notch")
                            .transform(ProfileTransformer.includeOriginalValue())
            ).apply();
        }));
        items.add(createItem(XMaterial.PLAYER_HEAD, "head-uuid", meta -> {
            XSkull.of(meta).profile(
                    Profileable.of(UUID.fromString("45d3f688-0765-4725-b5dd-dbc28fdfc9ab"))
                            .transform(ProfileTransformer.includeOriginalValue())
            ).apply();
        }));
        items.add(createItem(XMaterial.PLAYER_HEAD, "head-username-no-transform", meta -> {
            XSkull.of(meta).profile(
                    Profileable.of(UUID.fromString("45d3f688-0765-4725-b5dd-dbc28fdfc9ab"))
            ).apply();
        }));
        items.add(createItem(XMaterial.PLAYER_HEAD, "no-op head", meta -> {}));

        YamlConfiguration yaml = new YamlConfiguration();
        for (ItemStack item : items) {
            ItemMeta meta = item.getItemMeta();
            ConfigurationSection section = yaml.createSection(meta.getDisplayName());
            XItemStack.serialize(item, section);
        }
        yaml.save(file);
    }

    private static void testSkulls() {
        print("Testing skulls UUID...");
        XSkull.createItem().profile(Profileable.of(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"))).apply();
        print("Testing skulls username");
        XSkull.createItem().profile(Profileable.username("Notch")).apply();
        print("Testing skulls Base64");
        XSkull.createItem().profile(Profileable.detect("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzI0ZTY3ZGNlN2E0NDE4ZjdkYmE3MTE3MDQxODAzMDQ1MDVhMDM3YzEyZjE1NWE3MDYwM2UxOWYxMzIwMzRiMSJ9fX0=")).apply();
        print("Testing skulls textures hash");
        XSkull.createItem().profile(Profileable.detect("f9f28fe3a81d67e67472b7b91caad063722477dfc37f0d729a19be49c2ec2990")).apply();
        print("Testing skulls textures URL");
        XSkull.createItem().profile(Profileable.detect("https://textures.minecraft.net/texture/f9f28fe3a81d67e67472b7b91caad063722477dfc37f0d729a19be49c2ec2990")).apply();
        print("Testing skulls usernamed fallback");
        XSkull.createItem()
                .profile(Profileable.username("hjkSF3809HFGhs"))
                .fallback(Profileable.username("CryptoMorin"))
                .apply();
        print("Testing skulls lenient silence");
        XSkull.createItem()
                .profile(Profileable.username("F(&$#%Y(@&$(@#$Y_{GFS!"))
                .lenient().apply();

        print("Testing bulk username to UUID conversion");
        Map<UUID, String> mapped = MojangAPI.usernamesToUUIDs(Arrays.asList("yourmom1212",
                "ybe", "Scavage", "Tinchosz", "daerb",
                "verflow", "Brazzer", "Trillest", "EZix",
                "Meritocracia", "otpe", "nn_mc", "Hershey",
                "ElsaPlayzz", "HACKIN0706", "Angelisim", "iFraz",
                "KolevBG", "thebreadrat", "VIRGlN", "ImPuddles",
                "AlphaAce", "ggsophie", "TheDark_00", "yeezydealer",
                "HKa1", "Natheyy", "l0ves1ckk", "Bucyrus"), null);
        print("Result of bulk requests: " + mapped);

        print("Skull value of Notch: " + Profileable.username("Notch").getProfileValue());
        print("Skull value of Base64: " + Profileable.detect("f9f28fe3a81d67e67472b7b91caad063722477dfc37f0d729a19be49c2ec2990").getProfileValue());

        profilePreparation(); // Takes ~5 seconds (If we ignore the previous requests in this method)
        profilePreparation(); // Takes less than a second
    }

    private static void profilePreparation() {
        print("Profileable preparation test");
        Profileable.prepare(Arrays.asList(
                        Profileable.username("ImPuddles"), Profileable.username("HACKIN0706"), Profileable.username("yeezydealer"),
                        Profileable.detect("Bucyrus"),
                        Profileable.detect("https://textures.minecraft.net/texture/f9f28fe3a81d67e67472b7b91caad063722477dfc37f0d729a19be49c2ec2990")
                ), session -> session.exceptionally((a, b) -> {
                    print("Session Exceptionally");
                    b.printStackTrace();
                    return false;
                }), x -> {
                    print("Error Handler " + x.getMessage());
                    // x.printStackTrace(); Don't print, it'll not include the full stacktrace
                    return false;
                })
                .thenRun(() -> print("profile preparation done"))
                .exceptionally((ex) -> {
                    print("Profile preparation done exceptionally: ");
                    ex.printStackTrace();
                    return null;
                })
                .join();
    }

    private static void initializeReflection() {
        try {
            Class.forName("com.cryptomorin.xseries.XWorldBorder");
            Class.forName("com.cryptomorin.xseries.messages.ActionBar");
            Class.forName("com.cryptomorin.xseries.messages.Titles");
            Class.forName("com.cryptomorin.xseries.profiles.builder.XSkull");
            Class.forName("com.cryptomorin.xseries.reflection.minecraft.NMSExtras");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> void assertSame(Optional<T> expected, T actual) {
        Assertions.assertTrue(expected.isPresent(), () -> "Item is not present to compare with " + actual);
        Assertions.assertSame(expected.get(), actual);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void assertPresent(Optional<?> opt) {
        Assertions.assertTrue(opt.isPresent());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void assertPresent(Optional<?> opt, String details) {
        Assertions.assertTrue(opt.isPresent(), details);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static void assertMaterial(String bukkitMaterial, String xMaterial) {
        Optional<XMaterial> mat = XMaterial.matchXMaterial(xMaterial);
        assertPresent(mat);
        Assertions.assertSame(Material.matchMaterial(bukkitMaterial), mat.get().get());
    }
}
