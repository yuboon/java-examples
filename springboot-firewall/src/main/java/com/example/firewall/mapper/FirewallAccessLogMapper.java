package com.example.firewall.mapper;

import com.example.firewall.entity.FirewallAccessLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 防火墙访问日志Mapper接口
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Mapper
public interface FirewallAccessLogMapper {
    
    /**
     * 查询最近的访问日志
     * 
     * @param limit 限制数量
     * @return 访问日志列表
     */
    @Select("SELECT * FROM firewall_access_log ORDER BY request_time DESC LIMIT #{limit}")
    List<FirewallAccessLog> findRecent(int limit);
    
    /**
     * 根据时间范围查询访问日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 访问日志列表
     */
    @Select("SELECT * FROM firewall_access_log " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime} " +
            "ORDER BY request_time DESC")
    List<FirewallAccessLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据IP地址查询访问日志
     * 
     * @param ipAddress IP地址
     * @param limit 限制数量
     * @return 访问日志列表
     */
    @Select("SELECT * FROM firewall_access_log WHERE ip_address = #{ipAddress} " +
            "ORDER BY request_time DESC LIMIT #{limit}")
    List<FirewallAccessLog> findByIpAddress(@Param("ipAddress") String ipAddress, @Param("limit") int limit);
    
    /**
     * 根据API路径查询访问日志
     * 
     * @param apiPath API路径
     * @param limit 限制数量
     * @return 访问日志列表
     */
    @Select("SELECT * FROM firewall_access_log WHERE api_path = #{apiPath} " +
            "ORDER BY request_time DESC LIMIT #{limit}")
    List<FirewallAccessLog> findByApiPath(@Param("apiPath") String apiPath, @Param("limit") int limit);
    
    /**
     * 查询被拦截的访问日志
     * 
     * @param limit 限制数量
     * @return 访问日志列表
     */
    @Select("SELECT * FROM firewall_access_log WHERE block_reason IS NOT NULL " +
            "ORDER BY request_time DESC LIMIT #{limit}")
    List<FirewallAccessLog> findBlocked(int limit);
    
    /**
     * 插入访问日志
     * 
     * @param log 访问日志
     * @return 影响行数
     */
    @Insert("INSERT INTO firewall_access_log (ip_address, api_path, user_agent, request_method, " +
            "status_code, block_reason, request_time, response_time) " +
            "VALUES (#{ipAddress}, #{apiPath}, #{userAgent}, #{requestMethod}, " +
            "#{statusCode}, #{blockReason}, #{requestTime}, #{responseTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FirewallAccessLog log);
    
    /**
     * 批量插入访问日志
     * 
     * @param logs 访问日志列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO firewall_access_log (ip_address, api_path, user_agent, request_method, " +
            "status_code, block_reason, request_time, response_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.ipAddress}, #{item.apiPath}, #{item.userAgent}, #{item.requestMethod}, " +
            "#{item.statusCode}, #{item.blockReason}, #{item.requestTime}, #{item.responseTime})" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<FirewallAccessLog> logs);
    
    /**
     * 删除指定时间之前的日志
     * 
     * @param beforeTime 时间点
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_access_log WHERE request_time < #{beforeTime}")
    int deleteBeforeTime(LocalDateTime beforeTime);
    
    /**
     * 统计指定时间范围内的请求数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请求数
     */
    @Select("SELECT COUNT(*) FROM firewall_access_log " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime}")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime, 
                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内被拦截的请求数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 被拦截的请求数
     */
    @Select("SELECT COUNT(*) FROM firewall_access_log " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime} " +
            "AND block_reason IS NOT NULL")
    long countBlockedByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定IP地址在时间范围内的请求数
     * 
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请求数
     */
    @Select("SELECT COUNT(*) FROM firewall_access_log " +
            "WHERE ip_address = #{ipAddress} " +
            "AND request_time >= #{startTime} AND request_time <= #{endTime}")
    long countByIpAndTimeRange(@Param("ipAddress") String ipAddress,
                              @Param("startTime") LocalDateTime startTime, 
                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定API路径在时间范围内的请求数
     * 
     * @param apiPath API路径
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请求数
     */
    @Select("SELECT COUNT(*) FROM firewall_access_log " +
            "WHERE api_path = #{apiPath} " +
            "AND request_time >= #{startTime} AND request_time <= #{endTime}")
    long countByApiAndTimeRange(@Param("apiPath") String apiPath,
                               @Param("startTime") LocalDateTime startTime, 
                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取访问最频繁的IP地址
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return IP地址和访问次数
     */
    @Select("SELECT ip_address, COUNT(*) as count FROM firewall_access_log " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime} " +
            "GROUP BY ip_address ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> getTopIpAddresses(@Param("startTime") LocalDateTime startTime, 
                                  @Param("endTime") LocalDateTime endTime, 
                                  @Param("limit") int limit);
    
    /**
     * 获取访问最频繁的API路径
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return API路径和访问次数
     */
    @Select("SELECT api_path, COUNT(*) as count FROM firewall_access_log " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime} " +
            "GROUP BY api_path ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> getTopApiPaths(@Param("startTime") LocalDateTime startTime, 
                               @Param("endTime") LocalDateTime endTime, 
                               @Param("limit") int limit);
    
    /**
     * 删除指定时间之前的旧日志
     * 
     * @param cutoffTime 截止时间
     * @return 删除的记录数
     */
    @Delete("DELETE FROM firewall_access_log WHERE request_time < #{cutoffTime}")
    int deleteOldLogs(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 统计所有访问日志数量
     * 
     * @return 总数量
     */
    @Select("SELECT COUNT(*) FROM firewall_access_log")
    long countAll();
}