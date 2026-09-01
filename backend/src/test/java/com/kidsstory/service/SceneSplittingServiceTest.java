package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SceneSplittingServiceTest {

    private final SceneSplittingService service = new SceneSplittingService();

    @Test
    void oneSceneForFreeTierTenSecondVideo() {
        List<SceneDraft> scenes = service.splitIntoScenes("Once upon a time.", 10);

        assertThat(scenes).hasSize(1);
        assertThat(scenes.get(0).getIndex()).isZero();
        assertThat(scenes.get(0).getNarration()).isEqualTo("Once upon a time.");
    }

    @Test
    void sceneCountScalesWithVideoLength() {
        List<SceneDraft> scenes = service.splitIntoScenes("A story.", 30);

        assertThat(scenes).hasSize(3);
        assertThat(scenes).extracting(SceneDraft::getIndex).containsExactly(0, 1, 2);
    }

    @Test
    void neverProducesZeroScenesForAShortVideoLength() {
        // 5 / 10 == 0 in integer division; Math.max(1, ...) guards against zero scenes.
        List<SceneDraft> scenes = service.splitIntoScenes("A story.", 5);

        assertThat(scenes).hasSize(1);
    }

    @Test
    void cyclesThroughParagraphsWhenThereAreFewerParagraphsThanScenes() {
        String story = "First paragraph.\nSecond paragraph.";

        List<SceneDraft> scenes = service.splitIntoScenes(story, 30);

        assertThat(scenes).extracting(SceneDraft::getNarration)
                .containsExactly("First paragraph.", "Second paragraph.", "First paragraph.");
    }

    @Test
    void blankStoryFallsBackToUsingTheWholeStringAsOneParagraph() {
        List<SceneDraft> scenes = service.splitIntoScenes("   \n   ", 10);

        assertThat(scenes).hasSize(1);
        assertThat(scenes.get(0).getNarration()).isEqualTo("   \n   ");
    }

    @Test
    void visualDescriptionIsPrefixedAndTruncatedTo40Chars() {
        String longLine = "x".repeat(100);

        List<SceneDraft> scenes = service.splitIntoScenes(longLine, 10);

        String visualDescription = scenes.get(0).getVisualDescription();
        assertThat(visualDescription).startsWith("장면 1: ");
        assertThat(visualDescription).hasSize("장면 1: ".length() + 40);
    }
}
