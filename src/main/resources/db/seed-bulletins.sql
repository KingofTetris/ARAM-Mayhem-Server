-- ============================================================
-- ARAM Mayhem Assistant - 公告种子数据
-- ============================================================

INSERT INTO tb_bulletin (type, title, content, image_url, is_pinned, published_at, deleted) VALUES
('version', 'v1.0.0 正式上线', '<p>ARAM Mayhem Assistant 正式上线啦！</p><p>本次更新内容：</p><ul><li>✨ 英雄数据全面接入</li><li>✨ 海克斯符文推荐系统</li><li>✨ 社区玩法分享功能</li><li>✨ 实时公告推送</li></ul>', '/images/bulletins/v1.0.0.png', 1, NOW(), 0),

('event', 'ARAM 模式周免英雄活动', '<p>本周 ARAM 模式周免英雄已更新！</p><p>推荐尝试：亚托克斯、拉克丝、杰斯等强力英雄。</p><p>快打开 App 查看最新英雄强度排行吧！</p>', '/images/bulletins/weekly-rotation.png', 0, NOW(), 0),

('notice', '数据同步完成通知', '<p>英雄数据与海克斯数据已完成最新版本同步。</p><p>数据来源：Riot DataDragon + U.GG 统计</p><p>更新时间：2024-05-22</p>', '/images/bulletins/data-sync.png', 0, NOW(), 0),

('version', 'v1.1.0 版本预告', '<p>即将推出的新功能：</p><ul><li>🎯 英雄克制关系图谱</li><li>🎯 装备推荐系统</li><li>🎯 战绩查询功能</li></ul><p>敬请期待！</p>', '/images/bulletins/v1.1.0-preview.png', 0, DATE_ADD(NOW(), INTERVAL 1 DAY), 0);
