package com.aram.mayhem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aram.mayhem.common.BusinessException;
import com.aram.mayhem.dto.BulletinDetailVO;
import com.aram.mayhem.dto.BulletinListVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.entity.Bulletin;
import com.aram.mayhem.mapper.BulletinMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("BulletinService 测试")
@ExtendWith(MockitoExtension.class)
class BulletinServiceTest {

    @Mock
    private BulletinMapper mockMapper;

    @InjectMocks
    private BulletinServiceImpl bulletinService;

    private List<Bulletin> mockBulletins;

    @BeforeEach
    void setUp() {
        mockBulletins = List.of(
                createBulletin(1L, "version", "v14.10 更新公告", "14.10版本更新内容...", "https://img.example.com/1.png", 1),
                createBulletin(2L, "event", "五一活动公告", "五一活动内容...", "https://img.example.com/2.png", 0),
                createBulletin(3L, "notice", "服务器维护通知", "服务器维护内容...", null, 0),
                createBulletin(4L, "version", "v14.9 更新公告", "14.9版本更新内容...", "https://img.example.com/4.png", 1),
                createBulletin(5L, "event", "周年庆活动", "周年庆活动内容...", "https://img.example.com/5.png", 0)
        );
    }

    private Bulletin createBulletin(Long id, String type, String title, String content, String imageUrl, Integer isPinned) {
        Bulletin bulletin = new Bulletin();
        bulletin.setId(id);
        bulletin.setType(type);
        bulletin.setTitle(title);
        bulletin.setContent(content);
        bulletin.setImageUrl(imageUrl);
        bulletin.setIsPinned(isPinned);
        bulletin.setPublishedAt(LocalDateTime.now().minusDays(id));
        bulletin.setCreatedAt(LocalDateTime.now().minusDays(id));
        return bulletin;
    }

    @Test
    @DisplayName("testGetBulletinList_noTypeFilter_returnsAllBulletins")
    void testGetBulletinList_noTypeFilter_returnsAllBulletins() {
        Page<Bulletin> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(mockBulletins);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<BulletinListVO> result = bulletinService.getBulletinList(null, 1, 10);

        assertNotNull(result);
        assertEquals(5, result.getTotal());
        assertEquals(5, result.getRecords().size());
        verify(mockMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("testGetBulletinList_withTypeFilter_returnsFilteredBulletins")
    void testGetBulletinList_withTypeFilter_returnsFilteredBulletins() {
        List<Bulletin> versionBulletins = mockBulletins.stream()
                .filter(b -> "version".equals(b.getType()))
                .toList();
        Page<Bulletin> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(versionBulletins);

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<BulletinListVO> result = bulletinService.getBulletinList("version", 1, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotal());
        result.getRecords().forEach(vo -> assertEquals("version", vo.getType()));
    }

    @Test
    @DisplayName("testGetBulletinList_emptyResult_returnsEmptyList")
    void testGetBulletinList_emptyResult_returnsEmptyList() {
        Page<Bulletin> mockPage = new Page<>(1, 10, 0);
        mockPage.setRecords(List.of());

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<BulletinListVO> result = bulletinService.getBulletinList("nonexistent", 1, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    @DisplayName("testGetBulletinList_pagination_worksCorrectly")
    void testGetBulletinList_pagination_worksCorrectly() {
        Page<Bulletin> mockPage = new Page<>(2, 2, 5);
        mockPage.setRecords(mockBulletins.subList(2, 4));

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<BulletinListVO> result = bulletinService.getBulletinList(null, 2, 2);

        assertEquals(5, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    @DisplayName("testGetLatestBulletins_returnsLimitedResults")
    void testGetLatestBulletins_returnsLimitedResults() {
        when(mockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockBulletins.subList(0, 3));

        List<BulletinListVO> result = bulletinService.getLatestBulletins(3);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(mockMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("testGetBulletinDetail_existingId_returnsDetail")
    void testGetBulletinDetail_existingId_returnsDetail() {
        Bulletin bulletin = mockBulletins.get(0);
        when(mockMapper.selectById(1L)).thenReturn(bulletin);

        BulletinDetailVO result = bulletinService.getBulletinDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("version", result.getType());
        assertEquals("v14.10 更新公告", result.getTitle());
        assertEquals("14.10版本更新内容...", result.getContent());
        assertEquals("https://img.example.com/1.png", result.getImageUrl());
        assertEquals(1, result.getIsPinned());
        verify(mockMapper).selectById(1L);
    }

    @Test
    @DisplayName("testGetBulletinDetail_nonExistingId_throwsBusinessException")
    void testGetBulletinDetail_nonExistingId_throwsBusinessException() {
        when(mockMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bulletinService.getBulletinDetail(999L));

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("999"));
        verify(mockMapper).selectById(999L);
    }

    @Test
    @DisplayName("testConvertToListVO_allFieldsMappedCorrectly")
    void testConvertToListVO_allFieldsMappedCorrectly() {
        Page<Bulletin> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(List.of(mockBulletins.get(0)));

        when(mockMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageResult<BulletinListVO> result = bulletinService.getBulletinList(null, 1, 10);

        BulletinListVO vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals("version", vo.getType());
        assertEquals("v14.10 更新公告", vo.getTitle());
        assertNotNull(vo.getPublishedAt());
        assertNotNull(vo.getCreatedAt());
    }
}
