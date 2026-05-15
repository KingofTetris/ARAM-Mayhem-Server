package com.aram.mayhem.service;

import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;

import java.util.List;

public interface BulletinService {

    PageResult<BulletinListVO> getBulletinList(String type, int page, int size);

    List<BulletinListVO> getLatestBulletins(int limit);

    BulletinDetailVO getBulletinDetail(Long id);
}
