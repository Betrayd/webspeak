package net.betrayd.webspeak;


import net.betrayd.webspeak.math.Vec3d;

/**
 * An audio source that can be positioned in 3D space.
 */
public interface AudioSource3D {
    /**
     * Get the global position of this audio source.
     * @return Source's global position.
     */
    Vec3d getLocation();

    /**
     * Get the forward direction of this audio source.
     * @return Source forward vector
     */
    default Vec3d getForward() {
        return Vec3d.FORWARD;
    };

    /**
     * Get the up direction of this audio source.
     * @return Source up vector
     */
    default Vec3d getUp() {
        return Vec3d.UP;
    }
}
