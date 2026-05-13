package com.aram.mayhem.service;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentRecommendRequest;
import com.aram.mayhem.dto.AugmentRecommendResponse;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.SynergyProgressResponse;

import java.util.List;

public interface AugmentService {

    PageResult<AugmentListVO> getAugmentList(int page, int size, String quality, String synergySet);

    AugmentVO getAugmentDetail(Long id);

    List<SynergyProgressResponse> getSynergyProgress(String augmentIds);

    List<AugmentRecommendResponse> getRecommendations(AugmentRecommendRequest request);
}