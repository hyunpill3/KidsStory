package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AudioMixingServiceTest {

    private final AudioMixingService service = new AudioMixingService();

    @Test
    void derivesMixedAudioPathFromNarrationAudioPath() {
        SceneDraft scene = new SceneDraft(0, "narration", "visual");
        scene.setNarrationAudioPath("narration_0_male_ko.mp3");

        service.addMusicAndSfx(List.of(scene));

        assertThat(scene.getMixedAudioPath()).isEqualTo("mixed_0_male_ko.mp3");
    }
}
