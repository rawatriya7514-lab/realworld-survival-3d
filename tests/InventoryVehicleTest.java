import com.devagent.realworldsurvival3d.HostileAI;
import com.devagent.realworldsurvival3d.Inventory;
import com.devagent.realworldsurvival3d.Item;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.Vehicle;

import java.util.List;

/**
 * InventoryVehicleTest - Pure JVM verification of Phase 10:
 *  - 12-slot backpack inventory, item stacking, and capacity limits
 *  - Usable survival items (Medkit healing, Energy Drink stamina/adrenaline)
 *  - Vehicle physics (SUV & Motorcycle acceleration, steering, braking, reverse, handbrake)
 *  - Terrain height clamping & solid obstacle collisions
 *  - Hostile ramming / roadkill damage
 *  - Enter / exit player transitions & proximity queries
 */
public class InventoryVehicleTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean cond) {
        if (cond) {
            System.out.println("  PASS  " + name);
            passed++;
        } else {
            System.err.println("  FAIL  " + name);
            failed++;
        }
    }

    private static void checkApprox(String name, float a, float b, float eps) {
        if (Math.abs(a - b) <= eps) {
            System.out.println("  PASS  " + name + " (" + a + " ~ " + b + ")");
            passed++;
        } else {
            System.err.println("  FAIL  " + name + " (" + a + " != " + b + ", eps=" + eps + ")");
            failed++;
        }
    }

    private static Player createPlayer(float x, float y, float z) {
        Player p = new Player();
        p.x = x;
        p.y = y;
        p.z = z;
        return p;
    }

    public static void main(String[] args) {
        testInventoryBasicsAndStacking();
        testInventoryUsageAndConsumables();
        testInventoryCapacityLimits();
        testVehicleInitializationAndParams();
        testVehicleThrottleAndAcceleration();
        testVehicleBrakingAndReverse();
        testVehicleSteeringAndHandbrake();
        testVehicleObstacleCollisionAndDamage();
        testVehicleHostileRamming();
        testVehicleEnterExitAndProximity();
        testOpenWorldVehicleRoster();

        System.out.println("\nInventoryVehicleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testInventoryBasicsAndStacking() {
        System.out.println("Inventory: loadout & stacking");
        Inventory inv = new Inventory();

        check("starts with initial loadout (2 slots)", inv.getSlotCount() == 2);
        check("starts with 2 medkits", inv.getItemCount(Item.Type.MEDKIT) == 2);
        check("starts with 1 energy drink", inv.getItemCount(Item.Type.ENERGY_DRINK) == 1);

        // Add 2 more medkits -> stacks into existing slot (2 + 2 = 4 <= 5 maxStack)
        boolean added = inv.addItem(Item.Type.MEDKIT, 2);
        check("added 2 medkits", added);
        check("slot count still 2 (stacked)", inv.getSlotCount() == 2);
        check("medkit count is 4", inv.getItemCount(Item.Type.MEDKIT) == 4);

        // Add 2 more medkits -> 1 fits into slot (reaches 5), 1 spills into a new slot
        inv.addItem(Item.Type.MEDKIT, 2);
        check("spilled into 3rd slot", inv.getSlotCount() == 3);
        check("medkit count is 6", inv.getItemCount(Item.Type.MEDKIT) == 6);

        // Remove 3 medkits
        inv.removeItem(Item.Type.MEDKIT, 3);
        check("after removal medkit count is 3", inv.getItemCount(Item.Type.MEDKIT) == 3);
    }

    private static void testInventoryUsageAndConsumables() {
        System.out.println("Inventory: usable survival items");
        Inventory inv = new Inventory();
        Player player = createPlayer(0f, 0f, 0f);

        // 1. Medkit healing
        player.health = 50.0f; // damaged
        boolean used = inv.useItem(Item.Type.MEDKIT, player);
        check("medkit consumed successfully", used);
        checkApprox("heals +45 HP (50 -> 95)", player.health, 95.0f, 0.01f);
        check("medkit count decremented by 1", inv.getItemCount(Item.Type.MEDKIT) == 1);

        // Second medkit heals up to maxHealth (100)
        inv.useItem(Item.Type.MEDKIT, player);
        checkApprox("health capped at maxHealth 100", player.health, 100.0f, 0.01f);

        // Medkit at full health should NOT be consumed
        inv.addItem(Item.Type.MEDKIT, 1);
        boolean usedAtFull = inv.useItem(Item.Type.MEDKIT, player);
        check("medkit rejected at full health (prevents accidental waste)", !usedAtFull);

        // 2. Energy Drink
        player.stamina = 10.0f;
        player.adrenaline = 0.0f;
        boolean drinkUsed = inv.useItem(Item.Type.ENERGY_DRINK, player);
        check("energy drink consumed", drinkUsed);
        checkApprox("restores full stamina (100)", player.stamina, 100.0f, 0.01f);
        checkApprox("boosts adrenaline (+30)", player.adrenaline, 30.0f, 0.01f);

        // 3. Quest items cannot be consumed
        inv.addItem(Item.Type.RADIO_PART, 1);
        boolean questUsed = inv.useItem(Item.Type.RADIO_PART, player);
        check("quest radio parts cannot be ingested", !questUsed);
    }

    private static void testInventoryCapacityLimits() {
        System.out.println("Inventory: capacity bounds (12 max slots)");
        Inventory inv = new Inventory();

        // Fill all 12 slots with distinct or full items
        for (int i = inv.getSlotCount(); i < Inventory.MAX_SLOTS; i++) {
            inv.addItem(Item.Type.PARKOUR_TOKEN, 20); // each fills 1 slot
        }
        check("inventory at max capacity (12 slots)", inv.getSlotCount() == Inventory.MAX_SLOTS);

        // Adding a new item type when full is rejected
        boolean rejected = inv.addItem(Item.Type.RADIO_PART, 1);
        check("adding new item when inventory full is rejected", !rejected);
    }

    private static void testVehicleInitializationAndParams() {
        System.out.println("Vehicle: configurations & parameters");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 60f, 40f, 0f);
        Vehicle moto = new Vehicle(Vehicle.Type.MOTORCYCLE, "Trail Bike", 215f, 52f, 90f);

        check("SUV max speed is 22 m/s", suv.maxSpeed == 22.0f);
        check("Motorcycle max speed is 28 m/s", moto.maxSpeed == 28.0f);
        check("SUV health is 220", suv.health == 220.0f);
        check("Motorcycle health is 110", moto.health == 110.0f);
        check("SUV dimensions width=2.0 length=4.2", suv.width == 2.0f && suv.length == 4.2f);
        check("Motorcycle dimensions width=0.9 length=2.2", moto.width == 0.9f && moto.length == 2.2f);
    }

    private static void testVehicleThrottleAndAcceleration() {
        System.out.println("Vehicle: throttle & acceleration");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 0f, 0f, 0f);
        Player player = createPlayer(0f, 0f, 0f);
        suv.enter(player);

        check("vehicle starts at speed 0", suv.speed == 0f);

        // Full throttle forward for 1.0s
        suv.update(1.0f, 1.0f, 0f, false, null, player);
        check("accelerates forward (speed > 10 m/s)", suv.speed > 10.0f);

        // Run until top speed
        for (int i = 0; i < 20; i++) {
            suv.update(0.5f, 1.0f, 0f, false, null, player);
        }
        checkApprox("speed capped at maxSpeed (22 m/s)", suv.speed, 22.0f, 0.1f);
    }

    private static void testVehicleBrakingAndReverse() {
        System.out.println("Vehicle: braking & reverse");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 0f, 0f, 0f);
        Player player = createPlayer(0f, 0f, 0f);
        suv.enter(player);
        suv.speed = 15.0f; // moving forward

        // Apply reverse input (-1.0) -> acts as brake
        suv.update(0.5f, -1.0f, 0f, false, null, player);
        check("reverse input brakes forward motion", suv.speed < 15.0f);

        // Bring to halt then continue reversing
        suv.speed = 0f;
        suv.update(0.5f, -1.0f, 0f, false, null, player);
        check("reverse input accelerates in reverse (speed < 0)", suv.speed < 0f);
        check("reverse speed bounded by reverseSpeed", suv.speed >= suv.reverseSpeed - 0.1f);
    }

    private static void testVehicleSteeringAndHandbrake() {
        System.out.println("Vehicle: steering & handbrake drift");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 0f, 0f, 0f);
        Player player = createPlayer(0f, 0f, 0f);
        suv.enter(player);

        // Steering while standing still does NOT turn
        suv.speed = 0f;
        float initialYaw = suv.yaw;
        suv.update(0.5f, 0f, 1.0f, false, null, player);
        checkApprox("standing still steering does not rotate vehicle", suv.yaw, initialYaw, 0.01f);

        // Steering while moving turns the vehicle
        suv.speed = 10.0f;
        suv.update(0.5f, 0f, 1.0f, false, null, player);
        check("moving steering changes yaw", suv.yaw != initialYaw);

        // Handbrake sharply decelerates
        suv.speed = 20.0f;
        suv.update(0.5f, 0f, 0f, true, null, player);
        check("handbrake cuts speed rapidly", suv.speed < 10.0f);
    }

    private static void testVehicleObstacleCollisionAndDamage() {
        System.out.println("Vehicle: obstacle collision & impact damage");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 0f, 0f, 0f);
        Player player = createPlayer(0f, 0f, 0f);
        suv.enter(player);
        OpenWorld world = new OpenWorld();

        float initialHealth = suv.health;
        suv.speed = 18.0f; // fast speed

        // Place directly ahead of solid Safehouse wall (wall at z=52)
        suv.x = 210f;
        suv.z = 45f;
        suv.yaw = 180f; // pointing South (+Z) toward wall at z=52
        world.recomputeActive(suv.x, suv.z);

        // Move into wall
        suv.update(0.5f, 1.0f, 0f, false, world, player);
        check("solid impact deals damage to vehicle", suv.health < initialHealth);
        check("speed rebounds backward on impact", suv.speed <= 0f);
    }

    private static void testVehicleHostileRamming() {
        System.out.println("Vehicle: hostile roadkill ramming");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 100f, 100f, 0f);
        Player player = createPlayer(100f, 0f, 100f);
        suv.enter(player);
        OpenWorld world = new OpenWorld();

        // Spawn hostile directly in path
        HostileAI stalker = new HostileAI("Stalker", 100f, 98.5f, 10f);
        stalker.health = 50f;
        world.hostiles.clear();
        world.hostiles.add(stalker);

        suv.speed = 12.0f; // ramming speed
        suv.update(0.016f, 1.0f, 0f, false, world, player);

        check("high-speed impact deals heavy damage to hostile", stalker.health < 50f);
        check("impact knocks hostile backward", stalker.vz != 0f);
    }

    private static void testVehicleEnterExitAndProximity() {
        System.out.println("Vehicle: enter/exit transitions & proximity");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 50f, 50f, 0f);
        Player player = createPlayer(51f, 0f, 50f); // 1m away
        OpenWorld world = new OpenWorld();

        check("isNear is true within 3m", suv.isNear(player.x, player.z, 3.0f));

        // Enter
        boolean entered = suv.enter(player);
        check("player enters vehicle", entered);
        check("vehicle isOccupied is true", suv.isOccupied);
        check("engine starts", suv.isEngineOn);
        checkApprox("player X matches vehicle X", player.x, suv.x, 0.01f);

        // Exit
        suv.exit(player, world);
        check("vehicle isOccupied is false after exit", !suv.isOccupied);
        check("engine stops", !suv.isEngineOn);
        check("player positioned beside vehicle (not inside)", player.x != suv.x);
    }

    private static void testOpenWorldVehicleRoster() {
        System.out.println("OpenWorld: vehicle roster & nearest query");
        OpenWorld world = new OpenWorld();

        check("world contains 3 vehicles", world.vehicles.size() >= 3);

        // Query near Garage (60, 40)
        Vehicle v1 = world.nearestVehicle(61f, 41f, 4.0f);
        check("nearest query finds Garage SUV", v1 != null && "Scout SUV".equals(v1.name));

        // Query near Safehouse (215, 52)
        Vehicle v2 = world.nearestVehicle(216f, 53f, 4.0f);
        check("nearest query finds Safehouse Motorcycle", v2 != null && "Trail Bike".equals(v2.name));

        // Query empty area
        Vehicle vNone = world.nearestVehicle(0f, 0f, 2.0f);
        check("nearest query in empty area returns null", vNone == null);
    }
}
