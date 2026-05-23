package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据同步结果 DTO —— 记录一次数据同步任务的执行结果
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 每次数据同步任务（DataSyncScheduler.syncAllData()）执行完毕后，
 * 都会生成一个 SyncResult 对象，记录这次同步的详细结果。
 *
 * 就像快递员送完一批包裹后，需要填写一张"签收单"：
 * - 一共送了多少件？（heroInserted + heroUpdated）
 * - 有多少件签收成功？（validationPassed）
 * - 有多少件有问题？（validationFailed）
 * - 花了多长时间？（durationMs）
 * - 如果出了问题，问题是什么？（errorMessage）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、同步结果的使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. 日志记录：DataSyncScheduler 将 SyncResult 写入日志，方便运维人员监控
 * 2. API 返回：管理员可以通过 API 查看最近一次同步的结果
 * 3. 告警判断：如果 validationFailed 数量过多，可以触发告警
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、字段含义详解
 * ═══════════════════════════════════════════════════════════════════
 *
 * success：整体是否成功
 *   - true：同步完成（即使部分验证失败，只要同步流程走完就算成功）
 *   - false：同步失败（如获取版本号失败、获取锁失败等致命错误）
 *
 * heroInserted：新插入的英雄数量
 *   - 数据库中不存在的英雄，第一次同步时插入
 *   - 如新增了 3 个英雄，heroInserted = 3
 *
 * heroUpdated：更新的英雄数量
 *   - 数据库中已存在的英雄，数据有变化时更新
 *   - 如胜率变化了 20 个英雄，heroUpdated = 20
 *
 * augmentInserted / augmentUpdated：符文的插入/更新数量，含义同上
 *
 * validationPassed：数据验证通过的数量
 *   - MultiSourceValidator 对比两个数据源（Riot + U.GG）的数据
 *   - 差异在可接受范围内（≤5%）的算通过
 *
 * validationFailed：数据验证失败的数量
 *   - 两个数据源差异过大（>5%）或只有单源数据的算失败
 *   - 失败不意味着数据错误，可能是某个数据源更新延迟
 *
 * durationMs：同步耗时（毫秒）
 *   - 从开始获取锁到释放锁的总时间
 *   - 正常情况 1~5 分钟，如果超过 10 分钟需要关注
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - DataSyncScheduler：创建 SyncResult 的调度器
 * - DataAggregatorService：执行数据聚合，返回插入/更新数量
 * - MultiSourceValidator：执行数据验证，返回通过/失败数量
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    /**
     * 同步是否成功
     * - true：同步流程正常完成
     * - false：同步过程中遇到致命错误（如版本号获取失败、锁获取失败）
     */
    private boolean success;

    /** 同步完成时间 —— 记录这次同步是何时执行的 */
    private LocalDateTime syncTime;

    /** 新插入的英雄数量 —— 数据库中不存在的英雄首次写入 */
    private int heroInserted;

    /** 更新的英雄数量 —— 数据库中已存在的英雄数据被更新 */
    private int heroUpdated;

    /** 新插入的符文数量 —— 数据库中不存在的符文首次写入 */
    private int augmentInserted;

    /** 更新的符文数量 —— 数据库中已存在的符文数据被更新 */
    private int augmentUpdated;

    /** 数据验证通过的数量 —— 多源数据差异在可接受范围内 */
    private int validationPassed;

    /** 数据验证失败的数量 —— 多源数据差异过大或仅有单源数据 */
    private int validationFailed;

    /** 错误信息 —— 同步失败时的错误描述，成功时为 null */
    private String errorMessage;

    /** 同步耗时（毫秒）—— 从开始到结束的总时间 */
    private long durationMs;

    /**
     * 创建失败结果的工厂方法
     *
     * 当同步过程中遇到致命错误时调用，如：
     * - 获取分布式锁失败（另一台服务器正在同步）
     * - 获取 DataDragon 版本号失败（网络异常）
     *
     * @param errorMessage 错误描述
     * @return 只包含失败信息的 SyncResult
     */
    public static SyncResult fail(String errorMessage) {
        return SyncResult.builder()
                .success(false)
                .syncTime(LocalDateTime.now())
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建成功结果的工厂方法
     *
     * 当同步流程正常完成时调用，记录详细的统计信息。
     *
     * @param heroInserted      新插入的英雄数量
     * @param heroUpdated       更新的英雄数量
     * @param augmentInserted   新插入的符文数量
     * @param augmentUpdated    更新的符文数量
     * @param validationPassed  验证通过数量
     * @param validationFailed  验证失败数量
     * @param durationMs        同步耗时（毫秒）
     * @return 包含完整统计信息的 SyncResult
     */
    public static SyncResult success(int heroInserted, int heroUpdated,
                                     int augmentInserted, int augmentUpdated,
                                     int validationPassed, int validationFailed,
                                     long durationMs) {
        return SyncResult.builder()
                .success(true)
                .syncTime(LocalDateTime.now())
                .heroInserted(heroInserted)
                .heroUpdated(heroUpdated)
                .augmentInserted(augmentInserted)
                .augmentUpdated(augmentUpdated)
                .validationPassed(validationPassed)
                .validationFailed(validationFailed)
                .durationMs(durationMs)
                .build();
    }
}
