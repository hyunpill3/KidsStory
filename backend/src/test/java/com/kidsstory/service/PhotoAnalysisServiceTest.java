package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PhotoAnalysisServiceTest {

    private final PhotoAnalysisService service = new PhotoAnalysisService();

    @Test
    void returnsPlaceholderTextWhenNoPhotosUploaded() {
        assertThat(service.analyzePhotos(List.of())).isEqualTo("사진 정보 없음");
        assertThat(service.analyzePhotos(null)).isEqualTo("사진 정보 없음");
    }

    @Test
    void listsEachPhotoByPositionWhenPhotosArePresent() {
        String result = service.analyzePhotos(List.of("http://a", "http://b"));

        assertThat(result).contains("사진 1").contains("사진 2");
    }
}
