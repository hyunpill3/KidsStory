package com.kidsstory.service;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

/**
 * In-flight scene data threaded through the pipeline stages (mirrors the
 * Python worker's per-scene dict, which accumulates keys as each stage
 * runs). Only {@code localVideoPath} is ever real; narration/mixed audio
 * paths stay as placeholder strings since those stages remain stubs.
 */
@Getter
@Setter
public class SceneDraft {
    private final int index;
    private String narration;
    private String visualDescription;
    private String style;
    private Path localVideoPath;
    private String narrationAudioPath;
    private String mixedAudioPath;

    public SceneDraft(int index, String narration, String visualDescription) {
        this.index = index;
        this.narration = narration;
        this.visualDescription = visualDescription;
    }
}
