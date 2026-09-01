package com.kidsstory.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

/**
 * Step 1: Photo analysis.
 *
 * <p>Plug in a vision-capable model (e.g. GPT-4o / Gemini Vision) here to
 * detect subjects, mood, and setting from the uploaded photos. The result
 * feeds the story generation step so the story matches what's actually in
 * the photos. Stub - kept free/local, out of scope for the video-generation
 * feature this pipeline was built to demonstrate.
 */
@Service
public class PhotoAnalysisService {

    public String analyzePhotos(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "사진 정보 없음";
        }

        String subjects = IntStream.range(0, imageUrls.size())
                .mapToObj(i -> "사진 " + (i + 1))
                .collect(Collectors.joining(", "));
        return "업로드된 사진(" + subjects + ")에서 밝고 따뜻한 분위기의 주인공을 발견했습니다.";
    }
}
