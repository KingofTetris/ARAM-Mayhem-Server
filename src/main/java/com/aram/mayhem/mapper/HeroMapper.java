package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Hero;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 英雄数据访问接口（Mapper） —— 英雄数据的"数据库操作员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口是"英雄"数据和数据库之间的桥梁。
 * 当 Service 层需要查询、新增、修改或删除英雄数据时，
 * 不是直接写 SQL 语句，而是调用这个接口的方法。
 *
 * 打个比方：
 * - 数据库 = 仓库（存放所有英雄数据）
 * - HeroMapper = 仓库管理员（负责从仓库取东西、放东西）
 * - HeroService = 部门经理（告诉管理员需要什么，管理员去执行）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么这个接口看起来是"空的"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 你会发现这个接口里面没有任何方法定义，但它却能做很多事！
 * 这是因为它继承了 BaseMapper<Hero>，MyBatis-Plus 会自动提供以下方法：
 *
 * 【查询方法】
 * - selectById(id)              → 根据 ID 查询单个英雄
 * - selectBatchIds(idList)      → 根据 ID 列表批量查询多个英雄
 * - selectOne(wrapper)          → 根据条件查询单个英雄（如按 riotId 查询）
 * - selectList(wrapper)         → 根据条件查询英雄列表（如按 role 查询所有法师）
 * - selectPage(page, wrapper)   → 分页查询英雄列表（用于英雄列表页）
 * - selectCount(wrapper)        → 统计符合条件的英雄数量
 *
 * 【新增方法】
 * - insert(hero)                → 插入一条英雄记录
 *
 * 【修改方法】
 * - updateById(hero)            → 根据 ID 更新英雄信息
 * - update(hero, wrapper)       → 根据条件更新英雄信息
 *
 * 【删除方法】
 * - deleteById(id)              → 根据 ID 删除英雄（逻辑删除，设置 deleted=1）
 * - deleteBatchIds(idList)      → 根据 ID 列表批量删除
 * - delete(wrapper)             → 根据条件删除
 *
 * 这些方法都是 MyBatis-Plus 自动生成的，不需要我们手写 SQL！
 * 只需要继承 BaseMapper<Hero>，就能获得以上所有能力。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、如果需要自定义 SQL 怎么办？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 如果 BaseMapper 提供的方法不够用，有两种方式扩展：
 *
 * 方式 1：在这个接口中定义方法 + 写 XML 映射文件
 *   // 在 HeroMapper.java 中声明
 *   List<Hero> selectByRole(@Param("role") String role);
 *   // 在 HeroMapper.xml 中写 SQL
 *   <select id="selectByRole" resultType="Hero">
 *     SELECT * FROM tb_hero WHERE role = #{role}
 *   </select>
 *
 * 方式 2：使用 @Select 注解直接在方法上写 SQL
 *   @Select("SELECT * FROM tb_hero WHERE role = #{role}")
 *   List<Hero> selectByRole(@Param("role") String role);
 *
 * 目前项目中的查询都可以通过 BaseMapper + LambdaQueryWrapper 实现，
 * 所以暂时不需要自定义方法。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Mapper：告诉 MyBatis "这是一个 Mapper 接口，请为它创建实现类"。
 *          没有这个注解，MyBatis 不知道这个接口的存在，也就无法提供数据库操作。
 *          Spring Boot 启动时会扫描所有 @Mapper 注解的接口，
 *          自动为每个接口生成一个代理类（实现类），这个代理类包含了所有 SQL 操作。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 数据库 tb_hero 表
 *     ↕（MyBatis-Plus 自动映射）
 * HeroMapper（本接口）
 *     ↕（Service 调用 Mapper 方法）
 * HeroService / HeroServiceImpl
 *     ↕（Controller 调用 Service 方法）
 * HeroController
 *     ↕（HTTP API 返回 JSON）
 * Android 前端
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Hero：英雄实体类，对应 tb_hero 表
 * - HeroService / HeroServiceImpl：英雄的业务逻辑层，调用本接口的方法
 * - HeroController：英雄的 API 接口层，调用 Service 的方法
 * - BaseMapper<Hero>：MyBatis-Plus 提供的基础 CRUD 接口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 Service 层中的典型用法：
 *
 *   // 1. 注入 Mapper（通过构造函数注入，推荐方式）
 *   private final HeroMapper heroMapper;
 *
 *   // 2. 根据 ID 查询英雄
 *   Hero hero = heroMapper.selectById(1L);
 *
 *   // 3. 使用 LambdaQueryWrapper 条件查询
 *   LambdaQueryWrapper<Hero> wrapper = new LambdaQueryWrapper<>();
 *   wrapper.eq(Hero::getRole, "法师")        // 角色等于"法师"
 *          .orderByDesc(Hero::getWinRate);    // 按胜率降序排列
 *   List<Hero> mages = heroMapper.selectList(wrapper);
 *
 *   // 4. 分页查询
 *   Page<Hero> page = new Page<>(1, 20);     // 第1页，每页20条
 *   Page<Hero> result = heroMapper.selectPage(page, wrapper);
 *
 *   // 5. 插入新英雄
 *   Hero newHero = new Hero();
 *   newHero.setNameZh("新英雄");
 *   heroMapper.insert(newHero);
 *
 *   // 6. 更新英雄信息
 *   hero.setTier("S+");
 *   heroMapper.updateById(hero);
 */
@Mapper // 告诉 MyBatis 这是一个 Mapper 接口，启动时自动生成实现类
public interface HeroMapper extends BaseMapper<Hero> {
    // 当前不需要自定义方法，BaseMapper<Hero> 已提供所有基础 CRUD 操作
    // 如果未来需要复杂的自定义查询（如多表关联），可以在这里添加方法声明
}
