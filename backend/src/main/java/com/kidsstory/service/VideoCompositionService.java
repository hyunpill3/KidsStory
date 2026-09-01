package com.kidsstory.service;

import com.kidsstory.config.AppProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Step 7: Final video composition.
 *
 * <p>Concatenates the rendered scene clips and burns in the watermark using
 * ffmpeg (installed in the Docker image), writing to a local temp file. The
 * pipeline caller then uploads this file to storage - this class never
 * touches Postgres or object storage directly.
 */
@Service
@RequiredArgsConstructor
public class VideoCompositionService {

    private static final String WATERMARK_FILTER =
            "drawtext=text='%s':fontfile='%s':x=w-tw-16:y=h-th-16:fontsize=18:"
                    + "fontcolor=white@0.8:box=1:boxcolor=black@0.4:boxborderw=6";

    // Common ffmpeg Windows builds (e.g. gyan.dev "full") link fontconfig but
    // this OS ships no fonts.conf, and drawtext's fontconfig fallback can hard
    // crash (access violation) instead of erroring cleanly when no fontfile is
    // given. Always passing an explicit fontfile bypasses fontconfig entirely.
    private static final String WINDOWS_DEFAULT_FONT = "C:/Windows/Fonts/arialbd.ttf";
    // Matches the Debian/Ubuntu `fonts-dejavu-core` package path (installed in the Docker image).
    private static final String LINUX_DEFAULT_FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf";

    private final AppProperties appProperties;

    public Path composeFinalVideo(List<SceneDraft> scenes, String projectId, String watermarkText) {
        Path outputPath = Path.of(System.getProperty("java.io.tmpdir"), "kidsstory_" + projectId + "_final.mp4");

        List<String> command = new ArrayList<>(List.of("ffmpeg", "-y"));
        for (SceneDraft scene : scenes) {
            command.add("-i");
            command.add(scene.getLocalVideoPath().toString());
        }

        command.addAll(List.of(
                "-filter_complex", buildFilterComplex(scenes.size(), watermarkText),
                "-map", "[vout]",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                outputPath.toString()));

        runFfmpeg(command);
        return outputPath;
    }

    /** Pure string-building, split out from composeFinalVideo so it's testable without invoking ffmpeg. */
    String buildFilterComplex(int sceneCount, String watermarkText) {
        StringBuilder concatRefs = new StringBuilder();
        for (int i = 0; i < sceneCount; i++) {
            concatRefs.append("[").append(i).append(":v]");
        }
        return concatRefs
                + "concat=n=" + sceneCount + ":v=1:a=0[concatenated];"
                + "[concatenated]"
                + WATERMARK_FILTER.formatted(escapeDrawtext(watermarkText), escapeDrawtext(resolveFontPath()))
                + "[vout]";
    }

    private String resolveFontPath() {
        String configured = appProperties.getWatermarkFontPath();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return isWindows ? WINDOWS_DEFAULT_FONT : LINUX_DEFAULT_FONT;
    }

    private void runFfmpeg(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "ffmpeg exited with code " + exitCode + ": " + new String(output));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to run ffmpeg.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running ffmpeg.", ex);
        }
    }

    private String escapeDrawtext(String text) {
        return text.replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\'");
    }
}
