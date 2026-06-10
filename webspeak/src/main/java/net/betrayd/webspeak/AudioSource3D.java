package net.betrayd.webspeak;

import net.betrayd.webspeak.math.Vec3d;

/**
 * An audio source that can be positioned in 3D space.
 */
public interface AudioSource3D {

    //IDK what the heck I'm doing so I guess hacky workaround for now. 
    //TODO: actual implementation
    //MediaStreamTrack getAudioTrack();

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
