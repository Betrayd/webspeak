package net.betrayd.webspeak.v1;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.betrayd.webspeak.v1.util.AudioModifier;
import net.betrayd.webspeak.v1.util.WebSpeakEvents;
import net.betrayd.webspeak.v1.util.WebSpeakEvents.WebSpeakEvent;

/**
 * A group that players may join. Players may be in any number of groups, and
 * these groups will dictate what audio modifiers are used.
 */
public class WebSpeakGroup {
    private final Set<WebSpeakPlayer> players = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Create a WebSpeak group.
     * @param name A group name to use for debugging.
     */
    public WebSpeakGroup(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Called when a change in modifiers has invalidaded the audio modifier for a set of players.
     */
    public final WebSpeakEvent<Consumer<Collection<? extends WebSpeakPlayer>>> ON_MODIFIER_INVALIDATED = WebSpeakEvents
            .createSimple();
    
    private void onModifierInvalidated(Collection<? extends WebSpeakPlayer> collection) {
        audioModifiedPlayersCache.clear();
        ON_MODIFIER_INVALIDATED.invoker().accept(collection);
    }
    
    public final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_ADDED = WebSpeakEvents.createSimple();
    public final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_REMOVED = WebSpeakEvents.createSimple();

    protected synchronized boolean onAddPlayer(WebSpeakPlayer player) {
        if (players.add(player)) {
            ON_PLAYER_ADDED.invoker().accept(player);
            return true;
        }
        return false;
    }

    protected synchronized boolean onRemovePlayer(WebSpeakPlayer player) {
        if (players.remove(player)) {
            ON_PLAYER_REMOVED.invoker().accept(player);
            return true;
        }
        return false;
    }

    public Set<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    // No need to keep track of audio modifiers for players & groups not being used.
    private final Map<WebSpeakGroup, AudioModifier> groupAudioModifiers = new WeakHashMap<>();
    private final Map<WebSpeakPlayer, AudioModifier> playerAudioModifiers = new WeakHashMap<>();

    /**
     * Called when any of the players in a group that has an audio modifier on it is updated.
     */
    private final Consumer<WebSpeakPlayer> groupTargetPlayerUpdatedListener = player -> {
        onModifierInvalidated(Collections.singleton(player));
    };

    /**
     * Set the audio modifier for a target player. Modifier will be heard by players
     * in this group.
     * 
     * @param player Target player.
     * @param value  Audio modifier.
     * @return The previous audio modifier on that player, if any.
     */
    public synchronized AudioModifier setAudioModifier(WebSpeakPlayer player, AudioModifier value) {
        AudioModifier prev;
        if (value != null) {
            prev = playerAudioModifiers.put(player, value);
        } else {
            prev = playerAudioModifiers.remove(player);
        }

        onModifierInvalidated(Collections.singleton(player));
        return prev;
    }

    /**
     * Remove the audio modifier from a target player.
     * 
     * @param player Target player.
     * @return The previous audio modifier on that player, if any.
     */
    public void clearAudioModifier(WebSpeakPlayer player) {
        setAudioModifier(player, null);
    }

    /**
     * Set the audio modifier for all players in a group. Modifier will be heard by players
     * in this group.
     * 
     * @param player Target group.
     * @param value  Audio modifier.
     * @return The previous audio modifier on that group, if any.
     */
    public synchronized AudioModifier setAudioModifier(WebSpeakGroup group, AudioModifier value) {
        AudioModifier prev;
        if (value != null) {
            prev = groupAudioModifiers.put(group, value);
            // Add listeners so we get an update if a player joins or leaves the group
            group.ON_PLAYER_ADDED.addListener(groupTargetPlayerUpdatedListener);
            group.ON_PLAYER_REMOVED.addListener(groupTargetPlayerUpdatedListener);
        } else {
            prev = groupAudioModifiers.remove(group);
            group.ON_PLAYER_ADDED.removeListener(groupTargetPlayerUpdatedListener);
            group.ON_PLAYER_REMOVED.removeListener(groupTargetPlayerUpdatedListener);
        }
        // ON_MODIFIER_INVALIDATED.invoker().accept(group.getPlayers());
        onModifierInvalidated(group.getPlayers());
        return prev;
    }

    /**
     * Remove the audio modifier from a target group.
     * 
     * @param group Target group.
     * @return The previous audio modifier on that group, if any.;
     */
    public AudioModifier clearAudioModifier(WebSpeakGroup group) {
        return setAudioModifier(group, null);
    }

    /**
     * Compute the audio modifier this group will use for a given player.
     * 
     * @param player Target player.
     * @return Audio modifier.
     */
    public synchronized AudioModifier computeAudioModifier(WebSpeakPlayer player) {
        AudioModifier modifier = AudioModifier.EMPTY;
        for (var group : player.getGroups()) {
            AudioModifier groupModifier = groupAudioModifiers.get(group);
            if (groupModifier != null) {
                modifier = AudioModifier.combine(groupModifier, modifier);
            }
        }

        AudioModifier playerModifier = playerAudioModifiers.get(player);
        if (playerModifier != null) {
            modifier = AudioModifier.combine(playerModifier, modifier);
        }
        return modifier;
    }

    private final Set<WebSpeakPlayer> audioModifiedPlayersCache = new HashSet<>();

    /**
     * Get a set of all players that have an audio modifier because of this group.
     * 
     * @return Set of players.
     */
    public synchronized Set<WebSpeakPlayer> getAudioModifiedPlayers() {
        if (audioModifiedPlayersCache.isEmpty()) {
            for (var group : groupAudioModifiers.keySet()) {
                audioModifiedPlayersCache.addAll(group.players);
            }
            audioModifiedPlayersCache.addAll(playerAudioModifiers.keySet());
        }
        return audioModifiedPlayersCache;

    }

    @Override
    public String toString() {
        return "WebSpeakGroup[" + getName() + "]";
    }
}
