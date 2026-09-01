package com.kidsstory.service;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Step 6: Background music and sound-effect mixing.
 *
 * <p>Plug in a music-generation API or a licensed sound-effect library here,
 * matched to the story's mood, then mix it under each scene's narration
 * track. Stub - kept free/local.
 */
@Service
public class AudioMixingService {

    public List<SceneDraft> addMusicAndSfx(List<SceneDraft> scenes) {
        for (SceneDraft scene : scenes) {
            scene.setMixedAudioPath(scene.getNarrationAudioPath().replace("narration_", "mixed_"));
        }
        return scenes;
    }
}
