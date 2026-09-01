package com.kidsstory.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Step 3: Scene splitting.
 *
 * <p>Plug in an LLM here to split the full story into a sequence of scenes,
 * each with a short visual description and narration line. videoLength
 * (seconds) determines roughly how many scenes to produce. Stub - kept
 * free/local.
 */
@Service
public class SceneSplittingService {

    private static final int SECONDS_PER_SCENE = 10;

    public List<SceneDraft> splitIntoScenes(String story, int videoLength) {
        int sceneCount = Math.max(1, videoLength / SECONDS_PER_SCENE);
        List<String> paragraphs = new ArrayList<>(Arrays.stream(story.split("\n"))
                .map(String::strip)
                .filter(p -> !p.isEmpty())
                .toList());
        if (paragraphs.isEmpty()) {
            paragraphs.add(story);
        }

        List<SceneDraft> scenes = new ArrayList<>();
        for (int index = 0; index < sceneCount; index++) {
            String text = paragraphs.get(index % paragraphs.size());
            String visualDescription = "장면 " + (index + 1) + ": " + text.substring(0, Math.min(40, text.length()));
            scenes.add(new SceneDraft(index, text, visualDescription));
        }
        return scenes;
    }
}
