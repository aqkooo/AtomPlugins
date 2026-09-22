package com.ejyqyl.atompvpbot.ai;

import com.ejyqyl.atompvpbot.entity.PvPBotEntity;

/**
 * Coordinated AI controller executing staggered sub-system updates.
 *
 * @author ejyqyl
 */
public class BotAI {

    private final PvPBotEntity bot;
    private final TargetingSystem targetingSystem;
    private final MovementSystem movementSystem;
    private final CombatSystem combatSystem;
    private final RotationSystem rotationSystem;
    private final InventorySystem inventorySystem;
    private final DefenseSystem defenseSystem;
    private final UtilitySystem utilitySystem;
    private final SurvivalSystem survivalSystem;

    private long tickCount = 0;

    public BotAI(PvPBotEntity bot) {
        this.bot = bot;
        this.targetingSystem = new TargetingSystem(bot);
        this.movementSystem = new MovementSystem(bot);
        this.combatSystem = new CombatSystem(bot);
        this.rotationSystem = new RotationSystem(bot);
        this.inventorySystem = new InventorySystem(bot);
        this.defenseSystem = new DefenseSystem(bot);
        this.utilitySystem = new UtilitySystem(bot);
        this.survivalSystem = new SurvivalSystem(bot);
    }

    public void tick() {
        if (bot.getMob() == null || !bot.getMob().isValid()) return;
        tickCount++;

        // Staggered Subsystem Execution

        // 1. Rotation: Every tick for perfectly smooth tracking
        rotationSystem.updateRotation();

        // 2. Combat: Every tick
        combatSystem.updateCombat();

        // 3. Movement & Pathfinding: Every 2 ticks
        if (tickCount % 2 == 0) {
            movementSystem.updateMovement();
        }

        // 4. Defense (Shielding): Every 3 ticks
        if (tickCount % 3 == 0) {
            defenseSystem.updateDefense();
        }

        // 5. Targeting & Line-of-sight: Every 5 ticks
        if (tickCount % 5 == 0) {
            targetingSystem.update();
        }

        // 6. Utility & Survival: Every 10 ticks
        if (tickCount % 10 == 0) {
            utilitySystem.updateUtility();
            survivalSystem.updateSurvival();
            bot.updateNameTag();
        }
    }

    public TargetingSystem getTargetingSystem() { return targetingSystem; }
    public MovementSystem getMovementSystem() { return movementSystem; }
    public CombatSystem getCombatSystem() { return combatSystem; }
    public RotationSystem getRotationSystem() { return rotationSystem; }
    public InventorySystem getInventorySystem() { return inventorySystem; }
    public DefenseSystem getDefenseSystem() { return defenseSystem; }
    public UtilitySystem getUtilitySystem() { return utilitySystem; }
    public SurvivalSystem getSurvivalSystem() { return survivalSystem; }
}
