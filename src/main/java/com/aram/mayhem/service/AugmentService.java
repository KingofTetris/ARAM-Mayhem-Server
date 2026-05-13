package com.aram.mayhem.service;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;

public interface AugmentService {

    PageResult<AugmentListVO> getAugmentList(int page, int size, String quality, String synergySet);

    AugmentVO getAugmentDetail(Long id);
}
