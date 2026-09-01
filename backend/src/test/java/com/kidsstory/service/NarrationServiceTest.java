package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kidsstory.entity.Language;
import com.kidsstory.entity.VoiceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class NarrationServiceTest {

    private final NarrationService service = new NarrationService();

    @Test
    void buildsAPlaceholderPathEncodingSceneVoiceAndLanguage() {
        SceneDraft scene = new SceneDraft(2, "narration text", "visual");

        List<SceneDraft> result = service.generateNarration(List.of(scene), VoiceType.CALM_BEDTIME, Language.EN);

        assertThat(scene.getNarrationAudioPath()).isEqualTo("narration_2_calm_bedtime_en.mp3");
        assertThat(result).containsExactly(scene);
    }
}
