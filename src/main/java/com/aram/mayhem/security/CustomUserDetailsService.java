package com.aram.mayhem.security;

import com.aram.mayhem.entity.User;
import com.aram.mayhem.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 自定义用户详情服务
 *
 * 这个类是 Spring Security 和我们数据库之间的"翻译官"。
 * Spring Security 需要用户信息来做认证和授权，但它不知道我们的用户数据存在哪里、长什么样。
 * 这个类就是告诉 Spring Security："你要找用户？我来帮你从数据库里查。"
 *
 * 工作流程：
 * 1. JwtAuthenticationFilter 从 Token 中提取出用户邮箱
 * 2. 调用 loadUserByUsername(email) 方法
 * 3. 本类根据邮箱查询数据库，找到对应的用户记录
 * 4. 将用户记录转换为 Spring Security 能理解的 UserDetails 对象
 *
 * UserDetails 是 Spring Security 的标准用户接口，包含：
 * - 用户名（我们用用户 ID 代替）
 * - 密码（加密后的密文）
 * - 权限列表（如 ROLE_USER、ROLE_ADMIN）
 *
 * 权限格式说明：
 * - Spring Security 要求角色权限以 "ROLE_" 前缀开头
 * - 数据库中存储的是 "USER" 或 "ADMIN"，我们加上 "ROLE_" 前缀变成 "ROLE_USER" 或 "ROLE_ADMIN"
 * - 这样 @PreAuthorize("hasRole('ADMIN')") 才能正确匹配
 *
 * 关联类：
 * - UserMapper：数据库访问层，执行用户查询
 * - User：用户实体类，对应数据库的 users 表
 * - JwtAuthenticationFilter：调用本类加载用户信息
 *
 * @see UserMapper
 * @see User
 * @see JwtAuthenticationFilter
 */
@Service // 注册为 Spring Service，Spring 会自动将它注入到需要 UserDetailsService 的地方
public class CustomUserDetailsService implements UserDetailsService {

    // 用户数据库访问接口，MyBatis-Plus 自动实现
    private final UserMapper userMapper;

    /**
     * 构造函数注入 UserMapper
     *
     * @param userMapper 用户数据库访问接口
     */
    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 根据邮箱加载用户详情 —— Spring Security 认证时自动调用
     *
     * 这个方法名虽然叫 loadUserByUsername，但我们用邮箱作为查询条件，
     * 因为我们的系统使用邮箱登录（而非用户名）。
     *
     * 执行步骤：
     * 1. 使用 MyBatis-Plus 的 LambdaQueryWrapper 构建查询条件
     * 2. 查询数据库中 email 字段等于参数值的用户记录
     * 3. 如果找不到，抛出 UsernameNotFoundException
     * 4. 如果找到了，转换为 Spring Security 的 UserDetails 对象
     *
     * @param email 用户邮箱（虽然参数名叫 username，实际传入的是邮箱）
     * @return UserDetails 对象，包含用户 ID、密码和权限信息
     * @throws UsernameNotFoundException 当数据库中找不到对应邮箱的用户时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 使用 MyBatis-Plus 的 LambdaQueryWrapper 构建查询条件
        // LambdaQueryWrapper 是类型安全的查询构造器，用 Lambda 表达式引用字段名
        // 等价于 SQL：SELECT * FROM users WHERE email = ?
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );

        // 如果查询结果为 null，说明该邮箱没有注册过
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        // 将我们的 User 实体转换为 Spring Security 的 UserDetails
        // 参数说明：
        // - user.getId().toString()：用户名（用 ID 的字符串形式，因为 Spring Security 需要一个非空用户名）
        // - user.getPassword()：加密后的密码（BCrypt 密文）
        // - Collections.singletonList(...)：权限列表，只有一个元素
        //   - SimpleGrantedAuthority：Spring Security 的权限对象
        //   - "ROLE_" + user.getRole()：加上 ROLE_ 前缀，如 ROLE_USER、ROLE_ADMIN
        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
