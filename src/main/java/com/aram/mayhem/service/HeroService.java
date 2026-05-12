package com.aram.mayhem.service;

import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.HeroListVO;
import com.aram.mayhem.dto.PageResult;

public interface HeroService {

    PageResult<HeroListVO> getHeroList(int page, int size, String keyword, String tier, String sortBy);

    HeroDetailVO getHeroDetail(Long id);
}
