package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kidsstory.entity.AgeGroup;
import org.junit.jupiter.api.Test;

class StoryGenerationServiceTest {

    private final StoryGenerationService service = new StoryGenerationService();

    @Test
    void expandsUserSuppliedPromptRatherThanReplacingIt() {
        String story = service.generateStory("A puppy finds a lost star.", "photo insight", AgeGroup.AGE_6_8);

        assertThat(story).startsWith("A puppy finds a lost star.");
        assertThat(story).contains("photo insight");
        assertThat(story).contains(AgeGroup.AGE_6_8.getValue());
    }

    @Test
    void generatesADefaultStoryWhenPromptIsNull() {
        String story = service.generateStory(null, "photo insight", AgeGroup.AGE_3_5);

        assertThat(story).contains("마법의 숲");
        assertThat(story).contains("photo insight");
    }

    @Test
    void generatesADefaultStoryWhenPromptIsBlank() {
        String story = service.generateStory("   ", "photo insight", AgeGroup.AGE_9_12);

        assertThat(story).contains("마법의 숲");
    }
}
