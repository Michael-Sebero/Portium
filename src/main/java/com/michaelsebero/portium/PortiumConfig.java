package com.michaelsebero.portium;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Every Portium feature can be turned off here (config/portium.cfg, or Mods > Portium > Config in game).
 * <p>
 * Blocks, items, mobs, enchantments and effects are only registered when on: switching one off removes it from the
 * game and from existing worlds, needs a restart, and must match between a server and its clients. Everything else
 * applies as soon as it changes, unless its comment says otherwise.
 */
@Config(modid = Tags.MOD_ID)
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumConfig {

    @Config.Comment({"New blocks. Off: not registered, so it's removed from the game, from recipes and from world generation.",
            "Existing worlds lose that block. Servers and clients must match."})
    @Config.RequiresMcRestart
    public static final Blocks blocks = new Blocks();

    @Config.Comment({"New items. Off: not registered, so it's removed from the game, from recipes, loot and mob equipment.",
            "Existing worlds lose that item. Servers and clients must match."})
    @Config.RequiresMcRestart
    public static final Items items = new Items();

    @Config.Comment({"New mobs. Off: not registered, so they never spawn and existing ones disappear. Servers and clients must match."})
    @Config.RequiresMcRestart
    public static final Mobs mobs = new Mobs();

    @Config.Comment({"New enchantments. Off: not registered, so enchanting never offers them and existing ones stop working.",
            "Servers and clients must match."})
    @Config.RequiresMcRestart
    public static final Enchantments enchantments = new Enchantments();

    @Config.Comment({"New status effects and potions. Off: not registered, and nothing gives that effect any more.",
            "Servers and clients must match."})
    @Config.RequiresMcRestart
    public static final Effects effects = new Effects();

    @Config.Comment("Player movement and other gameplay changes.")
    public static final Mechanics mechanics = new Mechanics();

    @Config.Comment("Rendering, sound and HUD changes. These only matter on the client.")
    public static final Visuals visuals = new Visuals();

    @Config.Comment("World generation. Only affects chunks generated after the change.")
    public static final WorldGen worldGen = new WorldGen();

    @Config.Comment("Compatibility with Minecraft 1.13: its mods, worlds and data packs.")
    public static final Compatibility compatibility = new Compatibility();

    public static final class Blocks {

        @Config.Comment("Blue ice (1.13). Frozen oceans then have no blue ice.")
        public boolean blueIce = true;

        @Config.Comment({"Bubble columns above soul sand and magma blocks, which lift or drag swimmers, boats and items (1.13).",
                "Off: soul sand and magma under water behave as in 1.12."})
        public boolean bubbleColumns = true;

        @Config.Comment("The conduit (1.13).")
        public boolean conduit = true;

        @Config.Comment("Coral, coral fans and coral blocks, living and dead (1.13). Warm oceans then have no coral reefs.")
        public boolean coral = true;

        @Config.Comment("Kelp, dried kelp and dried kelp blocks (1.13). Oceans then have no kelp forests.")
        public boolean kelp = true;

        @Config.Comment("Seagrass and tall seagrass (1.13). Turtles are bred and tempted with seagrass.")
        public boolean seagrass = true;

        @Config.Comment("Sea pickles (1.13).")
        public boolean seaPickles = true;

        @Config.Comment("Turtle eggs (1.13). Without them turtles can't lay eggs.")
        public boolean turtleEggs = true;

        @Config.Comment({"The uncarved pumpkin, which shears carve into a carved pumpkin (1.13).",
                "Off: pumpkins are carved as in 1.12, and stems and pumpkin patches grow 1.12's pumpkin."})
        public boolean uncarvedPumpkin = true;

        @Config.Comment("Mushroom blocks and mushroom stems that keep the faces they're placed with (1.13).")
        public boolean mushroomBlocks = true;

        @Config.Comment("Smooth stone, smooth sandstone, smooth red sandstone and smooth quartz blocks (1.13/1.14).")
        public boolean smoothStone = true;

        @Config.Comment("Prismarine, prismarine brick and dark prismarine stairs and slabs (1.13).")
        public boolean prismarineStairsAndSlabs = true;

        @Config.Comment("Spruce, birch, jungle, acacia and dark oak buttons and pressure plates (1.13).")
        public boolean woodenButtonsAndPressurePlates = true;

        @Config.Comment("Spruce, birch, jungle, acacia and dark oak trapdoors (1.13).")
        public boolean woodenTrapdoors = true;

        @Config.Comment("Stripped logs, wood (bark on all six sides) and stripped wood (1.13). Axes can't strip logs without them.")
        public boolean strippedLogsAndWood = true;

        @Config.Comment({"The crimson and warped forests' blocks (1.16): nylium, stems, hyphae, wart blocks, shroomlights, fungi, roots, sprouts",
                "and weeping and twisting vines. The 1.16.5 Nether then has bare forests."})
        public boolean netherForests = true;

        @Config.Comment("Soul soil and soul fire (1.16).")
        public boolean soulSoil = true;

        @Config.Comment("Basalt and polished basalt (1.16).")
        public boolean basalt = true;

        @Config.Comment("Blackstone and gilded blackstone (1.16).")
        public boolean blackstone = true;

        @Config.Comment("Nether gold ore (1.16).")
        public boolean netherGoldOre = true;

        @Config.Comment("Ancient debris (1.16).")
        public boolean ancientDebris = true;

        @Config.Comment("Crying obsidian (1.16).")
        public boolean cryingObsidian = true;

        @Config.Comment("Crimson and warped planks, slabs, stairs, fences, fence gates, doors, trapdoors, buttons, pressure plates and signs (1.16). Needs the Nether forests.")
        public boolean netherWoods = true;

        @Config.Comment("Polished blackstone, its bricks (cracked, chiseled), and blackstone's stairs, slabs, walls, button and pressure plate (1.16). Needs blackstone.")
        public boolean blackstoneFamily = true;

        @Config.Comment("Cracked and chiseled nether bricks and quartz bricks (1.16).")
        public boolean netherBricks116 = true;

        @Config.Comment("1.14's stairs, slabs and walls of stone, granite, diorite, andesite, mossy stone, smooth sandstone and quartz, end stone bricks, red nether bricks and more.")
        public boolean stoneVariants = true;

        @Config.Comment("Signs of spruce, birch, jungle, acacia and dark oak (1.14).")
        public boolean woodSigns = true;

        @Config.Comment("The lantern (1.14) and soul lantern (1.16).")
        public boolean lanterns = true;

        @Config.Comment("Chains (1.16).")
        public boolean chains = true;

        @Config.Comment("Soul torches (1.16).")
        public boolean soulTorches = true;

        @Config.Comment("The campfire (1.14) and soul campfire (1.16): light, smoke signals, and cooking food.")
        public boolean campfires = true;

        @Config.Comment("The target block (1.16): gives off redstone power when hit by projectiles.")
        public boolean target = true;

        @Config.Comment("The lodestone, which compasses can be locked onto (1.16). Needs netherite for its recipe.")
        public boolean lodestone = true;

        @Config.Comment("The respawn anchor (1.16): a respawn point in the Nether, charged with glowstone. Needs crying obsidian for its recipe.")
        public boolean respawnAnchor = true;

        @Config.Comment("The cornflower, lily of the valley and wither rose (1.14), in flower pots too.")
        public boolean flowers114 = true;

        @Config.Comment("Sweet berry bushes and sweet berries (1.14).")
        public boolean sweetBerries = true;

        @Config.Comment("Bamboo (1.14).")
        public boolean bamboo = true;

        @Config.Comment("The barrel (1.14).")
        public boolean barrel = true;

        @Config.Comment("The smoker and blast furnace (1.14): furnaces for food and for ores, twice as fast.")
        public boolean smokerAndBlastFurnace = true;

        @Config.Comment("The composter (1.14), turning plants into bone meal.")
        public boolean composter = true;

        @Config.Comment("The fletching table (1.14), a fletcher villager's workstation.")
        public boolean fletchingTable = true;

        @Config.Comment("The grindstone (1.14): repairs and disenchants.")
        public boolean grindstone = true;

        @Config.Comment("The stonecutter (1.14): cuts stone into slabs, stairs, walls and more.")
        public boolean stonecutter = true;

        @Config.Comment("The loom (1.14) for banner patterns, with the banner pattern items and 1.16's globe and snout (piglin) patterns.")
        public boolean loom = true;

        @Config.Comment("The cartography table (1.14): copies, zooms out and locks maps.")
        public boolean cartographyTable = true;

        @Config.Comment("The bell (1.14): rung by hand, redstone or projectiles; reveals raiders nearby.")
        public boolean bell = true;

        @Config.Comment("The lectern (1.14): holds a book for anyone to read; turning pages gives redstone pulses.")
        public boolean lectern = true;

        @Config.Comment("Scaffolding (1.14): climbable, built out sideways and up from itself, falls when unsupported. Made from bamboo.")
        public boolean scaffolding = true;

        @Config.Comment("Honey blocks and honeycomb blocks (1.15), with honey bottles and honeycombs. Honey slows, softens falls, slides and sticks to pistons (not to slime).")
        public boolean honey = true;

        @Config.Comment("Bees, bee nests and beehives (1.15): bees pollinate flowers and fill hives with honey (sheared for honeycomb, bottled for honey), grow crops, and sting when angered. Nests generate on oak and birch trees in 1.16.5 worlds. Needs honey.")
        public boolean beehives = true;
    }

    public static final class Items {

        @Config.Comment("The trident (1.13). Drowned then carry fishing rods instead.")
        public boolean trident = true;

        @Config.Comment("Netherite (1.16): scrap from ancient debris, ingots, tools, armour, the netherite block, and the smithing table that upgrades diamond gear. Needs ancient debris.")
        public boolean netherite = true;

        @Config.Comment("The turtle shell helmet and scutes (1.13). Also needed for the Potion of the Turtle Master.")
        public boolean turtleShell = true;

        @Config.Comment("The heart of the sea, found in buried treasure and used for conduits (1.13).")
        public boolean heartOfTheSea = true;

        @Config.Comment("The nautilus shell, from fishing and drowned, used for conduits (1.13).")
        public boolean nautilusShell = true;

        @Config.Comment("Phantom membranes, dropped by phantoms (1.13). Used for slow falling potions and repairing elytra.")
        public boolean phantomMembrane = true;

        @Config.Comment("Buckets of cod, salmon, pufferfish and tropical fish (1.13), for each fish mob that's on.")
        public boolean fishBuckets = true;

        @Config.Comment("The debug stick, for creative players (1.13).")
        public boolean debugStick = true;

        @Config.Comment("White, blue, brown and black dyes as their own items (1.14), made from bone meal, lapis, cocoa, ink sacs and 1.14's flowers. Bone meal and the rest stay dyes too.")
        public boolean dyes114 = true;

        @Config.Comment("The crossbow (1.14) with multishot, piercing and quick charge; it also shoots fireworks, and fireworks with stars hurt what's near their burst (1.14).")
        public boolean crossbow = true;

        @Config.Comment("Leather horse armour (1.14): crafted from leather, dyeable.")
        public boolean leatherHorseArmor = true;

        @Config.Comment("Suspicious stew (1.14): mushroom stew with a flower, giving the flower's effect.")
        public boolean suspiciousStew = true;

        @Config.Comment("The Pigstep music disc (1.16), found in bastion remnant chests.")
        public boolean pigstep = true;
    }

    public static final class Mobs {

        @Config.Comment("Cod (1.13).")
        public boolean cod = true;

        @Config.Comment("Salmon (1.13).")
        public boolean salmon = true;

        @Config.Comment("Pufferfish (1.13).")
        public boolean pufferfish = true;

        @Config.Comment("Tropical fish (1.13).")
        public boolean tropicalFish = true;

        @Config.Comment("Dolphins (1.13).")
        public boolean dolphins = true;

        @Config.Comment("Sea turtles (1.13).")
        public boolean turtles = true;

        @Config.Comment("Drowned (1.13). Without them zombies don't turn into drowned under water.")
        public boolean drowned = true;

        @Config.Comment("Phantoms (1.13).")
        public boolean phantoms = true;

        @Config.Comment("Foxes (1.14), red in taigas and white in snowy taigas.")
        public boolean foxes = true;

        @Config.Comment("Pandas (1.14) in jungles, with their personalities; they eat bamboo.")
        public boolean pandas = true;

        @Config.Comment("Cats (1.14), split from ocelots: strays in villages and witch huts, tamed with fish. Ocelots become 1.14's untameable ones that learn to trust; tamed ocelots in existing worlds become cats.")
        public boolean cats = true;

        @Config.Comment("Pillagers (1.14): crossbow illagers in patrols that roam near players from day five. Killing a patrol's banner-carrying leader gives Bad Omen. Illagers and witches can lead patrols too.")
        public boolean pillagers = true;

        @Config.Comment("Ravagers (1.14): raid beasts that trample leaves and are stunned when a shield blocks their bite.")
        public boolean ravagers = true;

        @Config.Comment("Wandering traders and their trader llamas (1.14), with 1.14's trading screen for them.")
        public boolean wanderingTraders = true;

        @Config.Comment("Piglins (1.16) in the Nether: barter for gold ingots, love gold, attack players not wearing gold, hunt hoglins, turn into zombie pigmen outside the Nether.")
        public boolean piglins = true;

        @Config.Comment("Hoglins (1.16) in crimson forests, bred with crimson fungus, scared of warped fungus; and zoglins, which they become outside the Nether.")
        public boolean hoglins = true;

        @Config.Comment("Striders (1.16): lava walkers, ridden with a saddle and steered with a warped fungus on a stick (which comes with them).")
        public boolean striders = true;

        @Config.Comment("Zombified piglins (1.16): in worlds made with 1.16.5's generation, zombie pigmen become zombified piglins (1.16's model, texture and sounds). Other worlds keep pigmen.")
        public boolean zombifiedPiglins = true;

        @Config.Comment("Bees (1.15). Needs beehives (blocks) on.")
        public boolean bees = true;

        @Config.Comment("Villagers (1.14): in worlds made with 1.16.5 generation, villagers take professions at workstations, level up with trading, follow a daily schedule (work, meet at the bell, sleep in beds), breed when fed and a bed is free, and dress by biome. Other worlds keep 1.12's villagers.")
        public boolean villagers = true;

        @Config.Comment("Brown mooshrooms (1.14): lightning turns mooshrooms brown and back; brown ones fed a flower give suspicious stew.")
        public boolean brownMooshrooms = true;
    }

    public static final class Enchantments {

        @Config.Comment("Impaling: extra damage to water mobs (1.13).")
        public boolean impaling = true;

        @Config.Comment("Loyalty: a thrown trident returns (1.13).")
        public boolean loyalty = true;

        @Config.Comment("Riptide: a trident launches its thrower when wet (1.13).")
        public boolean riptide = true;

        @Config.Comment("Channeling: a trident summons lightning in thunderstorms (1.13).")
        public boolean channeling = true;

        @Config.Comment("Soul Speed: boots that run fast on soul sand and soul soil (1.16). Only from piglin bartering.")
        public boolean soulSpeed = true;
    }

    public static final class Effects {

        @Config.Comment("Conduit Power, given by conduits (1.13).")
        public boolean conduitPower = true;

        @Config.Comment("Dolphin's Grace, given by swimming dolphins (1.13).")
        public boolean dolphinsGrace = true;

        @Config.Comment("Slow Falling and its potions (1.13).")
        public boolean slowFalling = true;

        @Config.Comment("The Potion of the Turtle Master (1.13).")
        public boolean turtleMaster = true;
    }

    public static final class Mechanics {

        @Config.Comment("Portium's blocks and items take minecraft: ids (minecraft:heart_of_the_sea), as 1.13 to 1.16 name them, so they look native in F3, tooltips and commands. Their old portium: ids keep working. Needs a restart.")
        @Config.RequiresMcRestart
        public boolean minecraftIds = true;

        @Config.Comment("Sprint in water to swim (1.13).")
        public boolean swimming = true;

        @Config.Comment("Iron golems show cracks as they're hurt, and an iron ingot mends them (1.15).")
        public boolean golemCracks = true;

        @Config.Comment("Players are forced to crawl in one block tall gaps (1.14).")
        public boolean crawling = true;

        @Config.Comment("Sneaking players are 1.5 blocks tall and fit under slabs (1.14).")
        public boolean shorterSneaking = true;

        @Config.Comment("Dropped items float up in water (1.13).")
        public boolean itemBuoyancy = true;

        @Config.Comment("Other mods' furnace recipes also work in 1.14's cookers: their food in smokers and on campfires, their ores in blast furnaces. Minecraft's own items follow 1.14's recipes either way.")
        public boolean modCookingRecipes = true;

        @Config.Comment("Dispensers and droppers keep the facing they're placed with (1.13). 1.12 turns them away from a solid block in front when they're set by commands or structures.")
        public boolean dispenserFacing113 = true;

        @Config.Comment("Air refills gradually after surfacing, and upward bubble columns refill air (1.13).")
        public boolean gradualAirRefill = true;

        @Config.Comment("Grass and mycelium covered by water turn into dirt (1.16).")
        public boolean underwaterGrassToDirt = true;

        @Config.Comment("Walls rise to the block above them, show a post only where 1.16 does (not along a straight run under a block), and join glass panes and iron bars (1.16).")
        public boolean walls116 = true;

        @Config.Comment("Elytra can be deployed while moving upwards (1.15).")
        public boolean easyElytraTakeoff = true;

        @Config.Comment("Shoulder parrots stay on until the player sneaks or enters water, instead of leaving on every jump or fall.")
        public boolean sneakToDropParrots = true;

        @Config.Comment("Exploding beds in the Nether/End credit [Intentional Game Design] (1.16).")
        public boolean intentionalGameDesign = true;

        @Config.Comment("Vanilla commands use 1.13's syntax (execute, data, scoreboard, selectors, flattened ids). Other mods' commands keep working. Data pack functions always use 1.13 syntax.")
        public boolean modernCommands = true;

        @Config.Comment("Zombies sink in water; after 30 seconds under water zombies and zombie villagers turn into drowned and husks into zombies (1.13).")
        public boolean zombieDrowning = true;

        @Config.Comment("Right-click logs and wood with an axe to strip them (1.13).")
        public boolean axeStripping = true;

        @Config.Comment("Squids squirt ink and flee when attacked (1.13).")
        public boolean squidInk = true;

        @Config.Comment({"Blocks can hold water (1.13 waterlogging): 1.13's stairs, slabs, fences, glass panes, iron bars, chests, ladders, signs,",
                "trapdoors and walls, and also enchanting tables, anvils, mob heads, redstone torches, dust, repeaters and comparators, hoppers,",
                "doors, fence gates, levers, daylight sensors, buttons, pressure plates, leaves, shulker boxes and pistons."})
        public boolean waterlogging = true;

        @Config.Comment("Other mods' blocks can hold water too when they aren't full cubes (and aren't plants or fluids), or extend a block that can.")
        public boolean waterlogModBlocks = true;

        @Config.Comment("More blocks that can hold water, by id (\"modid:block_name\"), whatever their shape.")
        public String[] waterloggableBlocks = {};

        @Config.Comment("Blocks that can never hold water, by id (\"modid:block_name\"). Wins over everything else.")
        public String[] neverWaterloggableBlocks = {};

        @Config.Comment("Undead mobs (zombies, skeletons, drowned, phantoms...) don't drown (1.13). Zombies then live to turn into drowned.")
        public boolean undeadBreatheUnderwater = true;

        @Config.Comment({"Water dims light by 1 per block instead of 3, so oceans are brighter deeper down (1.13).",
                "Existing chunks relight gradually as they change."})
        @Config.RequiresMcRestart
        public boolean clearerWater = true;

        @Config.Comment("Buttons on floors and ceilings turn to face the player who places them (1.13).")
        public boolean rotatingButtons = true;

        @Config.Comment("Item frames can be placed on floors and ceilings (1.13).")
        public boolean floorItemFrames = true;

        @Config.Comment("Up to 15 water mobs around each player instead of 5, making room for fish (1.13).")
        public boolean moreWaterMobs = true;

        @Config.Comment("Bone meal used on the sea floor grows seagrass, and coral in warm oceans (1.13).")
        public boolean underwaterBonemeal = true;

        @Config.Comment("Fishing can catch nautilus shells as treasure, and lily pads count as junk (1.13).")
        public boolean newFishingLoot = true;

        @Config.Comment("Zombies trample turtle eggs; zombies, skeletons, wild wolves and ocelots hunt baby turtles on land (1.13).")
        public boolean turtleHunting = true;

        @Config.Comment("Phantoms appear at night above players who haven't slept for three days (1.13).")
        public boolean phantomSpawning = true;

        @Config.Comment("Phantom membranes repair elytra in an anvil (1.13).")
        public boolean elytraMembraneRepair = true;

        @Config.Comment("Boats over bubble columns rock, then get launched or dragged under (1.13).")
        public boolean bubbleColumnBoats = true;

        @Config.Comment("Placing a banner makes the wood placing sound, which 1.12 forgot.")
        public boolean bannerPlaceSound = true;
    }

    public static final class Visuals {

        @Config.Comment("Use 1.13 water textures and per-biome water colors.")
        public boolean newWaterColors = true;

        @Config.Comment("Use 1.13 underwater fog: per-biome fog color and vision that improves over time.")
        public boolean newUnderwaterFog = true;

        @Config.Comment({"1.13's Biome Blend video setting, which smooths grass, leaf and water colours across biome borders (Portium's default is 15x15).",
                "Off: 1.12's fixed 3x3 blend, and no slider."})
        public boolean biomeBlend = true;

        @Config.Comment("The screen overlay while under water is much fainter (1.13).")
        public boolean fainterUnderwaterOverlay = true;

        @Config.Comment("Smoothly animate the camera height when sneaking, swimming or crawling (1.14).")
        public boolean smoothEyeHeight = true;

        @Config.Comment("Disable the camera pitch bobbing while walking.")
        public boolean disablePitchBobbing = false;

        @Config.Comment("Play 1.13 underwater ambience, enter and exit sounds.")
        public boolean underwaterAmbience = true;

        @Config.Comment({"Items, blocks and mobs take 1.13's names (\"Oak Log\" rather than \"Oak Wood\", \"Enchanted Golden Apple\").",
                "Needs a restart, or a resource reload (F3+T)."})
        public boolean modernNames = true;

        @Config.Comment("Each 1.16 Nether biome's own fog colour, blended across borders (1.16).")
        public boolean netherFog = true;

        @Config.Comment("Ash floating in soul sand valleys and basalt deltas, and spores in crimson and warped forests (1.16).")
        public boolean netherParticles = true;

        @Config.Comment({"Each Nether biome's ambient sound loop, mood sounds and random additions, in place of 1.12's cave sounds there (1.16).",
                "Needs AssetMover for the sounds."})
        public boolean netherAmbientSounds = true;

        @Config.Comment("Each Nether biome's music, including Rubedo, Chrysopoeia and So Below (1.16). Needs AssetMover for the new tracks.")
        public boolean netherMusic = true;

        @Config.Comment("Show the air bar while air is refilling (1.13).")
        public boolean airBarWhileRefilling = true;

        @Config.Comment("While a command is typed in chat, colour its arguments and show suggestions, usage and errors above it (1.13). Needs Portium on the server.")
        public boolean commandHelp = true;

        @Config.Comment("Open the command suggestion list while typing rather than on Tab (1.13's \"Command Suggestions\" chat setting).")
        public boolean autoSuggestCommands = true;
    }

    public static final class WorldGen {

        @Config.Comment({"World generation for new worlds: \"1.16.5\" (everything Portium backports), \"1.13.2\" (1.13's generation below, without",
                "1.14-1.16's) or \"1.12.2\" (none of it). The Create World screen can choose per world; existing worlds keep what they were made with.",
                "Other mods' generation runs either way."})
        public String defaultGeneration = "1.16.5";

        @Config.Comment("The \"World Generation: 1.16.5 / 1.13.2 / 1.12.2\" button on the Create World screen. Off: new worlds use defaultGeneration.")
        public boolean generationButton = true;

        @Config.Comment({"1.16's Nether in worlds of 1.16.5 generation: its terrain, caves and lava sea, soul sand valleys, crimson and warped",
                "forests and basalt deltas (each needs its blocks), and their features. Off: those worlds keep 1.12's Nether."})
        @Config.RequiresMcRestart
        public boolean netherUpdate = true;

        @Config.Comment("1.14's plants in worlds generated as 1.16.5: cornflowers, lilies of the valley, sweet berry bushes in taigas, bamboo in jungles.")
        public boolean plants114 = true;

        @Config.Comment("Generate warm, lukewarm, cold and frozen ocean variants (1.13).")
        public boolean oceanVariants = true;

        @Config.Comment("Generate coral reefs and sea pickles in warm oceans.")
        public boolean coralReefs = true;

        @Config.Comment("Generate kelp forests.")
        public boolean kelp = true;

        @Config.Comment("Generate seagrass in oceans, rivers and swamps.")
        public boolean seagrass = true;

        @Config.Comment("Generate icebergs and blue ice in frozen oceans.")
        public boolean icebergs = true;

        @Config.Comment("Generate jagged packed ice formations at the surface of frozen oceans.")
        public boolean frozenOceanSurface = true;

        @Config.Comment("Pumpkin patches generate uncarved pumpkins.")
        public boolean uncarvedPumpkins = true;

        @Config.Comment("Underwater caves and canyons that cut through the ocean floor, with magma and obsidian at the bottom (1.13).")
        public boolean underwaterCaves = true;

        @Config.Comment("Generate shipwrecks, ocean ruins and buried treasure (1.13). Needs AssetMover for the structure templates.")
        public boolean oceanStructures = true;

        @Config.Comment("1.16.5's jigsaw structures in worlds made with 1.16.5 generation: villages (plains, desert, savanna, snowy, taiga; they replace 1.12's), pillager outposts, and Nether bastion remnants. Their templates and layouts come from the official 1.16.5 jar through AssetMover.")
        public boolean jigsawStructures = true;

        @Config.Comment("Bamboo jungles (1.14): in worlds made with 1.16.5 generation, one jungle region in ten is a bamboo jungle (or its hills). Needs bamboo (plants) on.")
        public boolean bambooJungle = true;

        @Config.Comment("The End's outer biomes (1.13): end highlands, midlands, barrens and small end islands beyond the main island, in worlds made with 1.13.2 generation or newer.")
        @Config.RequiresMcRestart
        public boolean endBiomes = true;

        @Config.Comment("The Buffet world type (1.13): one biome, or a few, across the whole world.")
        @Config.RequiresMcRestart
        public boolean buffetWorldType = true;
    }

    public static final class Compatibility {

        @Config.Comment({"Load Forge 1.13.2 mods placed in mods/1.13.2. The first time one is used, Forge 1.13.2 and Minecraft 1.13.2's code are",
                "downloaded. Off: that folder is ignored."})
        @Config.RequiresMcRestart
        public boolean modernMods = true;

        @Config.Comment({"Load Forge 1.16.5 mods placed in mods/1.16.5. The first time one is used, Forge 1.16.5 and Minecraft 1.16.5's code are",
                "downloaded. Off: that folder is ignored. Early work: few 1.16.5 mods run yet."})
        @Config.RequiresMcRestart
        public boolean mods116 = true;

        @Config.Comment({"Worlds saved by Minecraft 1.13 to 1.16.5 can be opened from the world list, and are converted to 1.12's format (a backup is",
                "offered first). Off: the world list won't open them."})
        public boolean modernWorlds = true;

        @Config.Comment({"At first launch, copy Minecraft 1.16.5's block, item and entity textures and models (from its client jar, downloaded from",
                "Mojang) into resourcepacks/Portium - Minecraft 1.16.5, and switch that pack on once, below any others. Modern resource packs",
                "(1.13 to 1.19) then work: Portium points 1.12's blocks at the newer models whenever an enabled pack has them."})
        @Config.RequiresMcRestart
        public boolean importTextures116 = true;

        @Config.Comment({"Data packs (1.13): each world's datapacks folder and the /datapack, /function and /reload commands, for recipes,",
                "advancements, loot tables, structures, functions and tags. Applies to worlds opened after the change."})
        public boolean dataPacks = true;

        @Config.Comment({"Data packs in the game's own datapacks folder (beside mods and config) load in every world, named global/<pack>.",
                "Each world can still turn one off with /datapack disable. Needs dataPacks."})
        public boolean globalDataPacks = true;

        @Config.Comment({"The built-in \"vanilla\" data pack, which gives 1.13.2's advancements, loot tables and tags in place of 1.12's.",
                "Needs dataPacks. Applies to worlds opened after the change."})
        public boolean vanillaDataPack = true;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Tags.MOD_ID)) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }

    private PortiumConfig() {}
}
