package me.siwannie.staffhunt.listeners;

import me.siwannie.staffhunt.StaffHunt;
import me.siwannie.staffhunt.game.GameManager;
import me.siwannie.staffhunt.ui.UIManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CustomItemListener implements Listener {

    private final StaffHunt plugin;
    private final UIManager uiManager;
    private final GameManager gameManager;
    private final Random random = new Random();

    // Unified Cooldowns: Map<WeaponKey, Map<PlayerUUID, ExpiryTime>>
    private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();
    private final java.util.Set<UUID> chaosCharged = new java.util.HashSet<>();

    public CustomItemListener(StaffHunt plugin) {
        this.plugin = plugin;
        this.uiManager = plugin.getUiManager();
        this.gameManager = plugin.getGameManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ==========================================
    // RIGHT-CLICK ABILITIES (INTERACT EVENT)
    // ==========================================
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase().trim();
        Material type = item.getType();

        // --- ORIGINAL SRC.ZIP ITEMS ---
        if (type == Material.SULPHUR && displayName.equals("medkit")) {
            handleMedkit(player, event);
            return;
        }
        if (type == Material.FIREWORK && displayName.equals("propulsor")) {
            handlePropulsor(player, event);
            return;
        }
        if (type == Material.NETHER_STAR && displayName.contains("invisibility cloak")) {
            handleInvisibilityCloak(player, event);
            return;
        }
        if (type == Material.ENDER_PEARL && displayName.equals("summoner")) {
            handleSummoner(player, event);
            return;
        }

        // --- PORTED SHADOWEDLEAVES WEAPONS ---
        if (displayName.contains("shadow blade") && type == Material.IRON_SWORD) {
            handleShadowBlade(player, event);
        } else if (displayName.contains("beach ball") && type == Material.SKULL_ITEM) {
            handleBeachBall(player, event);
        } else if (displayName.contains("nitro speed") && type == Material.GLASS_BOTTLE) {
            handleNitroSpeed(player, event);
        } else if (displayName.contains("flash bomb") && type == Material.FLINT_AND_STEEL) {
            handleFlashBomb(player, event);
        } else if (displayName.contains("gravity launch") && type == Material.FISHING_ROD) {
            handleGravityLaunch(player, event);
        } else if (displayName.contains("lifesaver") && type == Material.RED_ROSE) {
            handleLifesaver(player, event);
        } else if (displayName.contains("ender totem") && type == Material.ENDER_PEARL) {
            handleEnderTotem(player, event);
        } else if (displayName.contains("cobweb ball") && type == Material.SNOW_BALL) {
            handleCobwebBall(player, event);
        } else if (displayName.contains("burrow") && type == Material.WEB) {
            handleBurrow(player, event);
        } else if (displayName.contains("leviathan axe") && type == Material.DIAMOND_AXE) {
            handleLeviathanAxe(player, event);
        } else if (displayName.contains("arrow rain") && type == Material.ARROW) {
            handleArrowRain(player, event);
        } else if (displayName.contains("phantom phase") && type == Material.MONSTER_EGG) {
            handlePhantomPhase(player, event);
        } else if (displayName.contains("chaos blade") && type == Material.GOLD_SWORD) {
            handleChaosBladeCharge(player, event);
        }
    }

    // ==========================================
    // MELEE ABILITIES (DAMAGE EVENT)
    // ==========================================
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Handle Vanish revealing
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            if (isVanished(damaged)) removeVanish(damaged);
        }

        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (isVanished(damager)) removeVanish(damager);

            ItemStack weapon = damager.getItemInHand();
            if (weapon != null && weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName()) {
                String name = ChatColor.stripColor(weapon.getItemMeta().getDisplayName()).toLowerCase().trim();

                // Lightning Sword Logic
                if (name.equals("lightning sword") && weapon.getType() == Material.DIAMOND_SWORD) {
                    if (random.nextInt(100) < 30) { // 30% chance
                        event.getEntity().getWorld().strikeLightningEffect(event.getEntity().getLocation());
                        event.setDamage(event.getDamage() + 4.0); // Bonus lightning damage
                        event.getEntity().setFireTicks(60);
                    }
                }

                // Knockback Fish Logic
                if (name.equals("knockback fish") && weapon.getType() == Material.RAW_FISH) {
                    Vector knockback = damager.getLocation().getDirection().normalize().multiply(1.5).setY(0.5);
                    event.getEntity().setVelocity(knockback);
                }

                // Chaos Blade Hit Logic
                if (name.equals("chaos blade") && weapon.getType() == Material.GOLD_SWORD) {
                    if (chaosCharged.contains(damager.getUniqueId())) {
                        chaosCharged.remove(damager.getUniqueId());
                        if (event.getEntity() instanceof Player) {
                            Player hitPlayer = (Player) event.getEntity();
                            hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10 * 20, 0));
                            hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 10 * 20, 0));
                            hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 10 * 20, 0)); // Nausea
                            damager.playSound(damager.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f);
                            uiManager.sendActionBar(damager, ChatColor.DARK_PURPLE + "Chaos unleashed on " + hitPlayer.getName() + "!");
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // PROJECTILE ABILITIES (BOWS & THROWABLES)
    // ==========================================
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack bow = event.getBow();
        if (bow != null && bow.hasItemMeta() && bow.getItemMeta().hasDisplayName()) {
            String name = ChatColor.stripColor(bow.getItemMeta().getDisplayName()).toLowerCase().trim();
            if (name.equals("explosion bow")) {
                event.getProjectile().setMetadata("explosive_arrow", new FixedMetadataValue(plugin, true));
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().hasMetadata("beach_ball")) {
            event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), 2.0F, false);
        } else if (event.getEntity().hasMetadata("explosive_arrow")) {
            event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), 2.0F, false);
            event.getEntity().remove();
        } else if (event.getEntity().hasMetadata("cobweb_ball")) {
            Location hitLoc = event.getEntity().getLocation();
            if (hitLoc.getBlock().getType() == Material.AIR) {
                hitLoc.getBlock().setType(Material.WEB);
                // Remove cobweb after 5 seconds
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (hitLoc.getBlock().getType() == Material.WEB) {
                        hitLoc.getBlock().setType(Material.AIR);
                    }
                }, 5 * 20L);
            }
        }
    }

    // ==========================================
    // PORTED SHADOWEDLEAVES WEAPON HANDLERS
    // ==========================================

    private void handleShadowBlade(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "shadow_blade", 5 * 1000L, "Shadow Blade")) {
            Vector direction = player.getLocation().getDirection().normalize().multiply(10);
            Location target = player.getLocation().add(direction);
            player.teleport(target);
            uiManager.sendActionBar(player, ChatColor.DARK_GRAY + "Shadow Step!");
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            setCooldown(player, "shadow_blade");
        }
    }

    private void handleBeachBall(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "beach_ball", 30 * 1000L, "Beach Ball")) {
            Snowball ball = player.launchProjectile(Snowball.class);
            ball.setMetadata("beach_ball", new FixedMetadataValue(plugin, true));
            consumeItemInHand(player);
            setCooldown(player, "beach_ball");
        }
    }

    private void handleNitroSpeed(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "nitro_speed", 30 * 1000L, "Nitro Speed")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15 * 20, 1));
            uiManager.sendActionBar(player, ChatColor.YELLOW + "Nitro Speed applied!");
            player.playSound(player.getLocation(), Sound.BURP, 1.0f, 1.0f);
            consumeItemInHand(player);
            setCooldown(player, "nitro_speed");
        }
    }

    private void handleFlashBomb(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "flash_bomb", 30 * 1000L, "Flash Bomb")) {
            int count = 0;
            for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                if (entity instanceof Player && gameManager.isHunter((Player) entity)) {
                    Player hunter = (Player) entity;
                    hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15 * 20, 0)); // Blindness 1
                    hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 15 * 20, 2)); // Slowness 3
                    count++;
                }
            }
            uiManager.sendActionBar(player, ChatColor.YELLOW + "Flash Bomb detonated! Blinded " + count + " hunters.");
            setCooldown(player, "flash_bomb");
        }
    }

    private void handleGravityLaunch(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "gravity_launch", 30 * 1000L, "Gravity Launch")) {
            int count = 0;
            for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                if (entity instanceof Player && gameManager.isHunter((Player) entity)) {
                    Player hunter = (Player) entity;
                    Vector launch = hunter.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                    launch.setY(0.8);
                    launch.multiply(2.0); // Boosted slightly to ensure they go flying
                    hunter.setVelocity(launch);
                    count++;
                }
            }
            uiManager.sendActionBar(player, ChatColor.AQUA + "Gravity Launch engaged! Launched " + count + " hunters.");
            setCooldown(player, "gravity_launch");
        }
    }

    private void handleLifesaver(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "lifesaver", 45 * 1000L, "Lifesaver")) {
            int count = 0;
            for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                if (entity instanceof Player) count++;
            }

            if (count > 0) {
                double healAmount = count * 2.0; // 1 full heart per player
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healAmount));
                uiManager.sendActionBar(player, ChatColor.RED + "Lifesaver healed you for " + count + " hearts!");
                consumeItemInHand(player);
                setCooldown(player, "lifesaver");
            } else {
                player.sendMessage(ChatColor.RED + "No players nearby to heal from!");
            }
        }
    }

    private void handleEnderTotem(Player player, PlayerInteractEvent event) {
        event.setCancelled(true); // VERY IMPORTANT: Cancels the Ender Pearl from throwing natively!
        if (checkCooldown(player, "ender_totem", 300 * 1000L, "Ender Totem")) {
            int count = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                String rank = gameManager.getEffectiveRank(online);
                if (gameManager.isStaff(online) && rank != null && rank.equalsIgnoreCase("admin")) {
                    online.teleport(player);
                    online.sendMessage(ChatColor.LIGHT_PURPLE + "You have been summoned by the Ender Totem!");
                    count++;
                }
            }

            if (count > 0) {
                uiManager.sendActionBar(player, ChatColor.LIGHT_PURPLE + "Summoned " + count + " Admins!");
                player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                setCooldown(player, "ender_totem");
            } else {
                player.sendMessage(ChatColor.RED + "No admins online to summon!");
                // Do not apply cooldown if no admins were summoned
            }
        }
    }

    private void handleCobwebBall(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "cobweb_ball", 25 * 1000L, "Cobweb Ball")) {
            Snowball ball = player.launchProjectile(Snowball.class);
            ball.setMetadata("cobweb_ball", new FixedMetadataValue(plugin, true));
            consumeItemInHand(player);
            setCooldown(player, "cobweb_ball");
        }
    }

    private void handleBurrow(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "burrow", 60 * 1000L, "Burrow")) {
            Location originalLoc = player.getLocation();
            Location loc = originalLoc.clone().subtract(0, 3, 0);

            // Teleport down without breaking any blocks
            player.teleport(loc);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1));
            player.setMetadata("staffhunt_burrowed", new FixedMetadataValue(plugin, true));

            uiManager.sendActionBar(player, ChatColor.GRAY + "You burrowed underground!");
            player.playSound(player.getLocation(), Sound.DIG_STONE, 1.0f, 0.5f);

            // Teleport back after 10 seconds (duration of the regen effect)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && player.hasMetadata("staffhunt_burrowed")) {
                    player.teleport(originalLoc);
                    player.removeMetadata("staffhunt_burrowed", plugin);
                    player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    uiManager.sendActionBar(player, ChatColor.YELLOW + "You resurfaced!");
                }
            }, 10 * 20L);

            setCooldown(player, "burrow");
        }
    }

    private void handleLeviathanAxe(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "leviathan_axe", 10 * 1000L, "Leviathan Axe")) {
            // Launch up
            player.setVelocity(new Vector(0, 1.5, 0));
            player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1.0f, 1.0f);

            // Slam down after 1 second
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setVelocity(new Vector(0, -2.5, 0));

                // Wait for ground impact
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.getWorld().strikeLightningEffect(player.getLocation());
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof Player && gameManager.isHunter((Player) entity)) {
                            ((Player) entity).damage(10.0, player);
                        }
                    }
                }, 10L);
            }, 15L);

            setCooldown(player, "leviathan_axe");
        }
    }

    private void handleArrowRain(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "arrow_rain", 45 * 1000L, "Arrow Rain")) {
            Location center = player.getLocation();
            uiManager.sendActionBar(player, ChatColor.RED + "Summoning Arrow Rain!");
            player.playSound(player.getLocation(), Sound.AMBIENCE_THUNDER, 1.0f, 1.0f);

            // Spawn 64 arrows randomly in a 5-block radius above over 2 seconds
            for (int i = 0; i < 64; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    double offsetX = (random.nextDouble() - 0.5) * 10;
                    double offsetZ = (random.nextDouble() - 0.5) * 10;
                    Location spawnLoc = center.clone().add(offsetX, 10, offsetZ);

                    org.bukkit.entity.Arrow arrow = player.getWorld().spawnArrow(spawnLoc, new Vector(0, -1, 0), 1.0f, 12.0f);
                    arrow.setShooter(player);
                    arrow.setMetadata("staffhunt_arrow_rain", new FixedMetadataValue(plugin, true));
                }, random.nextInt(40)); // Random tick delay between 0 and 40
            }
            setCooldown(player, "arrow_rain");
        }
    }

    private void handlePhantomPhase(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (checkCooldown(player, "phantom_phase", 30 * 1000L, "Phantom Phase")) {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 4 * 20, 0));

            // --- NEW: True Bat Morph ---
            // Temporarily store and unequip armor so it doesn't float visibly
            ItemStack[] savedArmor = player.getInventory().getArmorContents().clone();
            player.getInventory().setArmorContents(null);

            // Spawn a bat and attach it to the player
            org.bukkit.entity.Bat bat = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Bat.class);
            player.setPassenger(bat);

            uiManager.sendActionBar(player, ChatColor.DARK_PURPLE + "Phantom Phase active for 4 seconds!");
            player.playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1.0f, 1.0f);

            // Remove after 4 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);

                    // Restore their armor!
                    player.getInventory().setArmorContents(savedArmor);
                    uiManager.sendActionBar(player, ChatColor.YELLOW + "Phantom Phase ended.");
                }
                // Always remove the fake bat entity
                if (bat.isValid()) {
                    bat.remove();
                }
            }, 4 * 20L);

            setCooldown(player, "phantom_phase");
        }
    }

    private void handleChaosBladeCharge(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (chaosCharged.contains(player.getUniqueId())) {
            uiManager.sendActionBar(player, ChatColor.GRAY + "Your Chaos Blade is already charged!");
            return;
        }

        if (checkCooldown(player, "chaos_blade", 60 * 1000L, "Chaos Blade")) {
            chaosCharged.add(player.getUniqueId());
            uiManager.sendActionBar(player, ChatColor.GREEN + "Chaos Blade charged! Your next hit will apply chaos.");
            player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0f, 2.0f);
            setCooldown(player, "chaos_blade");
        }
    }

    // ==========================================
    // ORIGINAL SRC.ZIP HANDLERS
    // ==========================================

    private void handleMedkit(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        long cooldownTime = plugin.getConfig().getLong("custom-items.medkit.cooldown-seconds", 45) * 1000L;
        if (checkCooldown(player, "medkit", cooldownTime, "Medkit")) {
            int duration = plugin.getConfig().getInt("custom-items.medkit.heal-duration-seconds", 10) * 20;
            int amplifier = plugin.getConfig().getInt("custom-items.medkit.heal-amplifier", 1);

            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier));
            uiManager.sendActionBar(player, ChatColor.GREEN + "Used Medkit: Regeneration applied!");
            setCooldown(player, "medkit");
        }
    }

    private void handlePropulsor(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        long cooldownTime = plugin.getConfig().getLong("custom-items.propulsor.cooldown-seconds", 30) * 1000L;
        if (checkCooldown(player, "propulsor", cooldownTime, "Propulsor")) {
            double forwardPower = plugin.getConfig().getDouble("custom-items.propulsor.launch-power-forward", 2.5);
            double upwardPower = plugin.getConfig().getDouble("custom-items.propulsor.launch-power-upward", 1.2);

            Vector forwardVector = player.getLocation().getDirection();
            forwardVector.setY(0).normalize().multiply(forwardPower);
            Vector upwardVector = new Vector(0, upwardPower, 0);

            Vector finalVelocity = player.getVelocity().add(forwardVector).add(upwardVector);
            player.setVelocity(finalVelocity);

            uiManager.sendActionBar(player, ChatColor.AQUA + "Propulsor engaged!");
            setCooldown(player, "propulsor");
        }
    }

    private void handleInvisibilityCloak(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        long cooldownTime = plugin.getConfig().getLong("custom-items.invisibility-cloak.cooldown-seconds", 60) * 1000L;
        if (checkCooldown(player, "invisibility_cloak", cooldownTime, "Invisibility Cloak")) {
            int duration = plugin.getConfig().getInt("custom-items.invisibility-cloak.duration-seconds", 15) * 20;

            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
            setVanished(player, true, duration);
            uiManager.sendActionBar(player, ChatColor.GRAY + "You are now invisible!");
            setCooldown(player, "invisibility_cloak");
        }
    }

    private void handleSummoner(Player player, PlayerInteractEvent event) {
        event.setCancelled(true); // Prevent native pearl throw
        if (!player.hasPermission("staffhunt.manager")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this item.");
            return;
        }

        long cooldownTime = plugin.getConfig().getLong("custom-items.summoner.cooldown-seconds", 120) * 1000L;
        if (checkCooldown(player, "summoner", cooldownTime, "Summoner")) {
            Player targetVanguard = null;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("staffhunt.vanguard") && !onlinePlayer.equals(player)) {
                    targetVanguard = onlinePlayer;
                    break;
                }
            }

            if (targetVanguard != null) {
                targetVanguard.teleport(player);
                uiManager.sendActionBar(player, ChatColor.LIGHT_PURPLE + "You have summoned " + targetVanguard.getName() + "!");
                uiManager.sendActionBar(targetVanguard, ChatColor.GOLD + "You have been summoned by " + player.getName() + "!");
                setCooldown(player, "summoner");
            } else {
                player.sendMessage(ChatColor.RED + "No available Vanguards to summon.");
            }
        }
    }

    // ==========================================
    // UTILITIES & COOLDOWNS
    // ==========================================

    private boolean checkCooldown(Player player, String weaponKey, long cooldownMillis, String itemName) {
        Map<UUID, Long> itemCooldowns = cooldowns.computeIfAbsent(weaponKey, k -> new HashMap<>());
        if (itemCooldowns.containsKey(player.getUniqueId())) {
            long lastUse = itemCooldowns.get(player.getUniqueId());
            long elapsed = System.currentTimeMillis() - lastUse;
            long secondsLeft = (cooldownMillis - elapsed) / 1000;

            // --- FIX: ANTI DOUBLE-CLICK ---
            // If the event fires twice in the same tick or < 500ms, silently ignore without sending a message
            if (elapsed < 500) {
                return false;
            }

            if (secondsLeft > 0) {
                uiManager.sendActionBar(player, ChatColor.RED + itemName + " is on cooldown for another " + secondsLeft + "s!");
                player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                return false;
            }
        }
        return true;
    }

    private void setCooldown(Player player, String weaponKey) {
        cooldowns.computeIfAbsent(weaponKey, k -> new HashMap<>()).put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void consumeItemInHand(Player player) {
        ItemStack item = player.getItemInHand();
        if (item != null && item.getType() != Material.AIR) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.setItemInHand(null);
            }
        }
    }

    private void setVanished(Player player, boolean vanished, int durationTicks) {
        if (vanished) {
            player.setMetadata("vanished", new FixedMetadataValue(plugin, true));
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.hidePlayer(player);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> removeVanish(player), durationTicks);
        } else {
            removeVanish(player);
        }
    }

    private void removeVanish(Player player) {
        if (isVanished(player)) {
            player.removeMetadata("vanished", plugin);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showPlayer(player);
            }
            uiManager.sendActionBar(player, ChatColor.YELLOW + "Your invisibility has worn off.");
        }
    }

    private boolean isVanished(Player player) {
        return player.hasMetadata("vanished");
    }
}