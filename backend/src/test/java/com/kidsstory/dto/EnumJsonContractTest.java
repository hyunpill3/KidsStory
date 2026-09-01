package com.kidsstory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsstory.entity.AgeGroup;
import com.kidsstory.entity.Language;
import com.kidsstory.entity.Plan;
import com.kidsstory.entity.ProjectStatus;
import com.kidsstory.entity.VideoStatus;
import com.kidsstory.entity.VisualStyle;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every enum's JSON string must match the frontend's TypeScript union type
 * literals exactly (frontend/src/types/index.ts) - the frontend is not part
 * of this rewrite and gets zero changes, so this is the contract that has
 * to hold. AgeGroup and VisualStyle are the two where a name-derived value
 * (e.g. AGE_3_5 -> "age_3_5") would be wrong; this test would have caught
 * that class of bug directly.
 */
class EnumJsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record Case(Enum<?> enumValue, String expectedJson) {
    }

    static Stream<Case> cases() {
        return Stream.of(
                new Case(AgeGroup.AGE_3_5, "3-5"),
                new Case(AgeGroup.AGE_6_8, "6-8"),
                new Case(AgeGroup.AGE_9_12, "9-12"),
                new Case(VisualStyle.THREE_D_CUTE, "3d_cute"),
                new Case(VisualStyle.STORYBOOK, "storybook"),
                new Case(VisualStyle.WATERCOLOR, "watercolor"),
                new Case(VisualStyle.CARTOON, "cartoon"),
                new Case(Language.KO, "ko"),
                new Case(Language.EN, "en"),
                new Case(Plan.FREE, "free"),
                new Case(Plan.BASIC, "basic"),
                new Case(Plan.PREMIUM, "premium"),
                new Case(VideoStatus.PROCESSING, "processing"),
                new Case(VideoStatus.COMPLETED, "completed"),
                new Case(VideoStatus.FAILED, "failed"),
                new Case(ProjectStatus.DRAFT, "draft"),
                new Case(ProjectStatus.QUEUED, "queued"),
                new Case(ProjectStatus.ANALYZING_PHOTOS, "analyzing_photos"),
                new Case(ProjectStatus.GENERATING_STORY, "generating_story"),
                new Case(ProjectStatus.GENERATING_SCENES, "generating_scenes"),
                new Case(ProjectStatus.GENERATING_NARRATION, "generating_narration"),
                new Case(ProjectStatus.COMPOSING_AUDIO, "composing_audio"),
                new Case(ProjectStatus.RENDERING_VIDEO, "rendering_video"),
                new Case(ProjectStatus.COMPLETED, "completed"),
                new Case(ProjectStatus.FAILED, "failed"));
    }

    @ParameterizedTest(name = "{0} <-> \"{1}\"")
    @MethodSource("cases")
    void serializesToTheExactFrontendExpectedString(Case testCase) throws Exception {
        String json = objectMapper.writeValueAsString(testCase.enumValue());

        assertThat(json).isEqualTo("\"" + testCase.expectedJson() + "\"");
    }

    @ParameterizedTest(name = "\"{1}\" -> {0}")
    @MethodSource("cases")
    void deserializesBackToTheSameEnumConstant(Case testCase) throws Exception {
        Object parsed = objectMapper.readValue(
                "\"" + testCase.expectedJson() + "\"", testCase.enumValue().getClass());

        assertThat(parsed).isEqualTo(testCase.enumValue());
    }

    @Test
    void projectOptionsSerializesAllFieldsInFrontendCamelCase() throws Exception {
        ProjectOptions options =
                new ProjectOptions(AgeGroup.AGE_6_8, 10, VisualStyle.STORYBOOK, com.kidsstory.entity.VoiceType.MALE,
                        Language.KO);

        @SuppressWarnings("unchecked")
        Map<String, Object> json =
                objectMapper.readValue(objectMapper.writeValueAsString(options), Map.class);

        assertThat(json)
                .containsEntry("ageGroup", "6-8")
                .containsEntry("videoLength", 10)
                .containsEntry("style", "storybook")
                .containsEntry("voice", "male")
                .containsEntry("language", "ko");
    }
}
