package com.hgz.xunyoubackend.service;

import com.hgz.xunyoubackend.utils.AlgorithmUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class AlgorithmTest {

    @Test
    public void test() {
        String word1 = "喜欢写java";
        String word2 = "不喜欢写java";
        String word3 = "不太喜欢写java";
        int distance = AlgorithmUtils.minDistance(word1, word2);
        int distance1 = AlgorithmUtils.minDistance(word1, word3);
        System.out.println(distance);
        System.out.println(distance1);
    }

    @Test
    public void test1() {
        List<String> tagList1 = Arrays.asList("Java", "大一", "男");
        List<String> tagList2 = Arrays.asList("Java", "大一", "女");
        List<String> tagList3 = Arrays.asList("Python", "大二", "女");
        // 1
        int score1 = AlgorithmUtils.minDistanceByTags(tagList1, tagList2);
        // 3
        int score2 = AlgorithmUtils.minDistanceByTags(tagList1, tagList3);
        System.out.println(score1);
        System.out.println(score2);
    }

}
