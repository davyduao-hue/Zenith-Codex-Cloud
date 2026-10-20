package meteordevelopment.meteorclient.zenith;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Shared, bounded HUD data. Never scans chunks or sends gameplay packets. */
public final class Zenith {
    public static final Zenith INSTANCE = new Zenith();
    public final FrameHistory frames = new FrameHistory(240);
    public final FrameHistory memory = new FrameHistory(100);
    private final Deque<Long> leftClicks = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
    public LivingEntity target;
    public long lastAttack, joinedAt;
    public int hits, combo;
    public float damage;
    public double averageFps, lowFps;
    private float previousHealth;
    private long lastSample, lastStats;

    private Zenith() {}

    public static void init() {
        Modules.get().add(new ZenithEffects());
        Modules.get().add(new ZenithAim.AimAssist());
        Modules.get().add(new ZenithAim.Aimbot());
        Modules.get().add(new SmallTotem());
        MeteorClient.EVENT_BUS.subscribe(INSTANCE);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onAttack(AttackEntityEvent event) {
        if (event.isCancelled() || !(event.entity instanceof LivingEntity living)) return;
        if (living != target) { hits = combo = 0; damage = 0; }
        target = living;
        previousHealth = living.getHealth() + living.getAbsorptionAmount();
        lastAttack = System.nanoTime();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (target != null) {
            float health = target.getHealth() + target.getAbsorptionAmount();
            if (health < previousHealth && System.nanoTime() - lastAttack < 1_000_000_000L) {
                hits++; combo++; damage += previousHealth - health;
                ZenithEffects effects = Modules.get().get(ZenithEffects.class);
                if (effects.isActive()) effects.hit(target);
            }
            previousHealth = health;
            if (target.isRemoved() || System.nanoTime() - lastAttack > 5_000_000_000L) target = null;
        }
        long now = System.nanoTime();
        if (now - lastSample > 250_000_000L) {
            Runtime runtime = Runtime.getRuntime();
            memory.add((runtime.totalMemory() - runtime.freeMemory()) / 1048576.0);
            lastSample = now;
        }
    }

    @EventHandler
    private void onFrame(Render2DEvent event) {
        if (mc.player == null) return;
        frames.add(event.frameTime * 1000);
        long now = System.nanoTime();
        if (now - lastStats > 500_000_000L) {
            averageFps = frames.averageFps(); lowFps = frames.onePercentLow(); lastStats = now;
        }
    }

    @EventHandler
    private void onClick(MouseClickEvent event) {
        if (event.action != KeyAction.Press || mc.gui.screen() != null) return;
        Deque<Long> queue = event.click.button() == 0 ? leftClicks : event.click.button() == 1 ? rightClicks : null;
        if (queue != null) { if (queue.size() == 128) queue.removeFirst(); queue.addLast(System.nanoTime()); }
    }

    public int cps(boolean right) {
        Deque<Long> queue = right ? rightClicks : leftClicks;
        long cutoff = System.nanoTime() - 1_000_000_000L;
        while (!queue.isEmpty() && queue.peekFirst() < cutoff) queue.removeFirst();
        return queue.size();
    }

    @EventHandler private void onJoin(GameJoinedEvent event) { clear(); joinedAt = System.nanoTime(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clear(); }
    private void clear() {
        target = null; hits = combo = 0; damage = 0;
        frames.clear(); memory.clear(); leftClicks.clear(); rightClicks.clear();
    }
}
