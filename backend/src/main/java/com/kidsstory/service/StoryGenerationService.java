package com.kidsstory.service;

import com.kidsstory.entity.AgeGroup;
import org.springframework.stereotype.Service;

/**
 * Step 2: Story generation / expansion.
 *
 * <p>Plug in an LLM here. If the user supplied a story prompt, expand those
 * one or two sentences into a full children's story. If not, generate an
 * original story purely from the photo analysis. Stub - kept free/local.
 */
@Service
public class StoryGenerationService {

    public String generateStory(String storyPrompt, String photoInsight, AgeGroup ageGroup) {
        if (storyPrompt != null && !storyPrompt.isBlank()) {
            return storyPrompt + "\n\n(" + ageGroup.getValue() + " 아이 눈높이에 맞춰 확장된 이야기입니다. " + photoInsight + ")";
        }

        return "사진 속 주인공이 마법의 숲에서 새로운 친구를 만나는 이야기.\n("
                + ageGroup.getValue() + " 아이 눈높이에 맞춰 자동 생성된 이야기입니다. " + photoInsight + ")";
    }
}
