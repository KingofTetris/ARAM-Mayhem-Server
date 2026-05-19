package com.aram.mayhem.service;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.HeroDetailVO;
import com.aram.mayhem.dto.PageResult;

import java.util.List;

public interface CacheWarmupService {

    List<Long> warmupHeroCache(int topN);

    int warmupAugmentCache();

    int warmupHeroListCache();

    int cleanupStaleCache();
}
