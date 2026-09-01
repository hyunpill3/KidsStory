package com.kidsstory.service;

import com.kidsstory.entity.Language;
import com.kidsstory.entity.VoiceType;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Step 5: Narration generation.
 *
 * <p>Plug in a text-to-speech API here (e.g. ElevenLabs, Azure TTS, OpenAI
 * TTS) using each scene's narration text, the chosen voice type, and
 * language. Stub - kept free/local.
 */
@Service
public class NarrationService {

    public List<SceneDraft> generateNarration(List<SceneDraft> scenes, VoiceType voice, Language language) {
        for (SceneDraft scene : scenes) {
            scene.setNarrationAudioPath(
                    "narration_" + scene.getIndex() + "_" + voice.getValue() + "_" + language.getValue() + ".mp3");
        }
        return scenes;
    }
}
