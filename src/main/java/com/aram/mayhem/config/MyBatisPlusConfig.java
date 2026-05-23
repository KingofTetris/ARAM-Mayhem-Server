package com.aram.mayhem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * MyBatis-Plus 是 MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变。
 * 简单来说，它让数据库操作变得更简单：不用写 SQL，调用方法就能完成增删改查。
 *
 * 本类配置的核心功能：分页插件
 * - 分页就是"一页一页地显示数据"，比如英雄列表有 100 个英雄，每页显示 20 个，共 5 页
 * - 没有分页插件的话，MyBatis-Plus 的分页查询只会返回全部数据，不会自动截断
 * - 加上这个插件后，调用 selectPage() 方法就能自动实现"只查第 N 页的数据"
 *
 * 工作原理：
 * PaginationInnerInterceptor 会在 SQL 执行前自动拦截，
 * 给原始 SQL 加上 LIMIT 语句（MySQL）或对应的分页语法，
 * 同时执行一条 COUNT 查询获取总记录数，实现真正的物理分页。
 *
 * 关联类：
 * - HeroServiceImpl：调用 heroMapper.selectPage() 分页查询英雄列表
 * - StrategyServiceImpl：调用 strategyMapper.selectPage() 分页查询策略列表
 */
@Configuration // 声明这是一个 Spring 配置类，Spring 启动时自动加载
public class MyBatisPlusConfig {

    /**
     * 创建 MyBatis-Plus 拦截器 Bean
     *
     * MybatisPlusInterceptor 是 MyBatis-Plus 的拦截器容器，可以添加多个内部拦截器。
     * 我们目前只添加了分页拦截器，将来如果需要乐观锁、防止全表更新等功能，也可以在这里添加。
     *
     * @return 配置好的 MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建拦截器容器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页内部拦截器，指定数据库类型为 MySQL
        // DbType.MYSQL 告诉插件使用 MySQL 的分页语法（LIMIT offset, size）
        // 如果换成其他数据库（如 PostgreSQL、Oracle），需要改这里
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
