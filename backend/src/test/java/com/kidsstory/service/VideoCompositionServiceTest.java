package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kidsstory.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VideoCompositionServiceTest {

    private AppProperties appProperties;
    private VideoCompositionService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        service = new VideoCompositionService(appProperties);
    }

    @Test
    void singleSceneFilterConcatenatesJustThatOneClip() {
        String filter = service.buildFilterComplex(1, "KidsStory");

        assertThat(filter).startsWith("[0:v]concat=n=1:v=1:a=0[concatenated];");
        assertThat(filter).endsWith("[vout]");
    }

    @Test
    void multiSceneFilterReferencesEveryInputInOrder() {
        String filter = service.buildFilterComplex(3, "KidsStory");

        assertThat(filter).startsWith("[0:v][1:v][2:v]concat=n=3:v=1:a=0[concatenated];");
    }

    @Test
    void watermarkTextIsBurnedIntoTheDrawtextFilter() {
        String filter = service.buildFilterComplex(1, "KidsStory");

        assertThat(filter).contains("drawtext=text='KidsStory'");
    }

    @Test
    void watermarkTextWithFfmpegSpecialCharsIsEscaped() {
        // A colon or single quote in the watermark text would otherwise break
        // ffmpeg's filter-graph syntax.
        String filter = service.buildFilterComplex(1, "Kid's: Story");

        assertThat(filter).contains("drawtext=text='Kid\\'s\\: Story'");
    }

    @Test
    void alwaysPassesAnExplicitFontfileToAvoidTheFontconfigCrash() {
        // drawtext must never rely on fontconfig's default lookup - that's
        // exactly what crashes some Windows ffmpeg builds when no fonts.conf
        // is present (see VideoCompositionService's class Javadoc).
        String filter = service.buildFilterComplex(1, "KidsStory");

        assertThat(filter).contains("fontfile='");
    }

    @Test
    void explicitWatermarkFontPathOverridesTheOsDefault() {
        appProperties.setWatermarkFontPath("/custom/font.ttf");

        String filter = service.buildFilterComplex(1, "KidsStory");

        assertThat(filter).contains("fontfile='/custom/font.ttf'");
    }

    @Test
    void windowsPathColonInTheFontfileIsEscaped() {
        appProperties.setWatermarkFontPath("C:/Windows/Fonts/arialbd.ttf");

        String filter = service.buildFilterComplex(1, "KidsStory");

        assertThat(filter).contains("fontfile='C\\:/Windows/Fonts/arialbd.ttf'");
    }
}
