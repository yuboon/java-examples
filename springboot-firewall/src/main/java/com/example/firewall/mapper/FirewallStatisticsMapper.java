package com.example.firewall.mapper;

import com.example.firewall.entity.FirewallStatistics;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 防火墙统计Mapper接口
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Mapper
public interface FirewallStatisticsMapper {
    
    /**
     * 查询指定日期的统计数据
     * 
     * @param statDate 统计日期
     * @return 统计数据列表
     */
    @Select("SELECT * FROM firewall_statistics WHERE stat_date = #{statDate} ORDER BY total_requests DESC")
    List<FirewallStatistics> findByDate(LocalDate statDate);
    
    /**
     * 查询指定日期范围的统计数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据列表
     */
    @Select("SELECT * FROM firewall_statistics " +
            "WHERE stat_date >= #{startDate} AND stat_date <= #{endDate} " +
            "ORDER BY stat_date DESC, total_requests DESC")
    List<FirewallStatistics> findByDateRange(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);
    
    /**
     * 查询指定API路径的统计数据
     * 
     * @param apiPath API路径
     * @param limit 限制数量
     * @return 统计数据列表
     */
    @Select("SELECT * FROM firewall_statistics WHERE api_path = #{apiPath} " +
            "ORDER BY stat_date DESC LIMIT #{limit}")
    List<FirewallStatistics> findByApiPath(@Param("apiPath") String apiPath, @Param("limit") int limit);
    
    /**
     * 查询指定日期和API路径的统计数据
     * 
     * @param statDate 统计日期
     * @param apiPath API路径
     * @return 统计数据
     */
    @Select("SELECT * FROM firewall_statistics WHERE stat_date = #{statDate} AND api_path = #{apiPath}")
    FirewallStatistics findByDateAndApiPath(@Param("statDate") LocalDate statDate, 
                                           @Param("apiPath") String apiPath);
    
    /**
     * 插入统计数据
     * 
     * @param statistics 统计数据
     * @return 影响行数
     */
    @Insert("INSERT INTO firewall_statistics (stat_date, api_path, total_requests, blocked_requests, avg_response_time) " +
            "VALUES (#{statDate}, #{apiPath}, #{totalRequests}, #{blockedRequests}, #{avgResponseTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FirewallStatistics statistics);
    
    /**
     * 更新统计数据
     * 
     * @param statistics 统计数据
     * @return 影响行数
     */
    @Update("UPDATE firewall_statistics SET total_requests = #{totalRequests}, " +
            "blocked_requests = #{blockedRequests}, avg_response_time = #{avgResponseTime}, " +
            "updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(FirewallStatistics statistics);
    
    /**
     * 插入或更新统计数据
     * 
     * @param statistics 统计数据
     * @return 影响行数
     */
    @Insert("INSERT INTO firewall_statistics (stat_date, api_path, total_requests, blocked_requests, avg_response_time) " +
            "VALUES (#{statDate}, #{apiPath}, #{totalRequests}, #{blockedRequests}, #{avgResponseTime}) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_requests = total_requests + #{totalRequests}, " +
            "blocked_requests = blocked_requests + #{blockedRequests}, " +
            "avg_response_time = (avg_response_time * total_requests + #{avgResponseTime} * #{totalRequests}) / (total_requests + #{totalRequests}), " +
            "updated_time = CURRENT_TIMESTAMP")
    int insertOrUpdate(FirewallStatistics statistics);
    
    /**
     * 删除指定日期之前的统计数据
     * 
     * @param beforeDate 日期
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_statistics WHERE stat_date < #{beforeDate}")
    int deleteBeforeDate(LocalDate beforeDate);
    
    /**
     * 获取最近N天的总请求数
     * 
     * @param days 天数
     * @return 总请求数
     */
    @Select("SELECT COALESCE(SUM(total_requests), 0) FROM firewall_statistics " +
            "WHERE stat_date >= DATEADD('DAY', -#{days}, CURRENT_DATE)")
    long getTotalRequestsInDays(int days);
    
    /**
     * 获取最近N天的总拦截数
     * 
     * @param days 天数
     * @return 总拦截数
     */
    @Select("SELECT COALESCE(SUM(blocked_requests), 0) FROM firewall_statistics " +
            "WHERE stat_date >= DATEADD('DAY', -#{days}, CURRENT_DATE)")
    long getTotalBlockedInDays(int days);
    
    /**
     * 获取最近N天的平均响应时间
     * 
     * @param days 天数
     * @return 平均响应时间
     */
    @Select("SELECT COALESCE(AVG(avg_response_time), 0) FROM firewall_statistics " +
            "WHERE stat_date >= DATEADD('DAY', -#{days}, CURRENT_DATE)")
    double getAvgResponseTimeInDays(int days);
    
    /**
     * 获取访问量最高的API路径
     * 
     * @param days 最近天数
     * @param limit 限制数量
     * @return API路径和访问量
     */
    @Select("SELECT api_path, SUM(total_requests) as total FROM firewall_statistics " +
            "WHERE stat_date >= DATEADD('DAY', -#{days}, CURRENT_DATE) " +
            "GROUP BY api_path ORDER BY total DESC LIMIT #{limit}")
    @Results({
        @Result(column = "api_path", property = "apiPath"),
        @Result(column = "total", property = "total")
    })
    List<Object> getTopApiPaths(@Param("days") int days, @Param("limit") int limit);
    
    /**
     * 获取拦截率最高的API路径
     * 
     * @param days 最近天数
     * @param limit 限制数量
     * @return API路径和拦截率
     */
    @Select("SELECT api_path, " +
            "SUM(blocked_requests) * 100.0 / SUM(total_requests) as block_rate " +
            "FROM firewall_statistics " +
            "WHERE stat_date >= DATEADD('DAY', -#{days}, CURRENT_DATE) " +
            "AND total_requests > 0 " +
            "GROUP BY api_path " +
            "HAVING SUM(total_requests) >= 10 " +
            "ORDER BY block_rate DESC LIMIT #{limit}")
    @Results({
        @Result(column = "api_path", property = "apiPath"),
        @Result(column = "block_rate", property = "blockRate")
    })
    List<Object> getTopBlockedApiPaths(@Param("days") int days, @Param("limit") int limit);
    
    /**
     * 获取每日统计趋势
     * 
     * @param days 最近天数
     * @return 每日统计数据
     */
    @Select("SELECT stat_date, " +
            "SUM(total_requests) as total_requests, " +
            "SUM(blocked_requests) as blocked_requests, " +
            "AVG(avg_response_time) as avg_response_time " +
            "FROM firewall_statistics " +
            "WHERE stat_date >= DATEADD('DAY', -#{days}, CURRENT_DATE) " +
            "GROUP BY stat_date ORDER BY stat_date")
    @Results({
        @Result(column = "stat_date", property = "statDate"),
        @Result(column = "total_requests", property = "totalRequests"),
        @Result(column = "blocked_requests", property = "blockedRequests"),
        @Result(column = "avg_response_time", property = "avgResponseTime")
    })
    List<FirewallStatistics> getDailyTrend(int days);
    
    /**
     * 批量插入统计数据
     * 
     * @param statisticsList 统计数据列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO firewall_statistics (stat_date, api_path, total_requests, blocked_requests, avg_response_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.statDate}, #{item.apiPath}, #{item.totalRequests}, #{item.blockedRequests}, #{item.avgResponseTime})" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<FirewallStatistics> statisticsList);
    
    /**
     * 根据日期获取总阻止请求数
     * 
     * @param date 日期
     * @return 总阻止请求数
     */
    @Select("SELECT COALESCE(SUM(blocked_requests), 0) FROM firewall_statistics WHERE stat_date = #{date}")
    long getTotalBlockedByDate(@Param("date") LocalDate date);
    
    /**
     * 根据日期获取平均响应时间
     * 
     * @param date 日期
     * @return 平均响应时间
     */
    @Select("SELECT COALESCE(AVG(avg_response_time), 0) FROM firewall_statistics WHERE stat_date = #{date}")
    double getAvgResponseTimeByDate(@Param("date") LocalDate date);
    
    /**
     * 根据请求数获取热门API
     * 
     * @param date 日期
     * @param limit 限制数量
     * @return API路径和请求数
     */
    @Select("SELECT api_path, total_requests FROM firewall_statistics " +
            "WHERE stat_date = #{date} ORDER BY total_requests DESC LIMIT #{limit}")
    List<Map<String, Object>> getTopApisByRequests(@Param("date") LocalDate date, @Param("limit") int limit);
    
    /**
     * 根据阻止率获取热门API
     * 
     * @param date 日期
     * @param limit 限制数量
     * @return API路径和阻止率
     */
    @Select("SELECT api_path, " +
            "CASE WHEN total_requests > 0 THEN (blocked_requests * 100.0 / total_requests) ELSE 0 END as block_rate " +
            "FROM firewall_statistics WHERE stat_date = #{date} " +
            "ORDER BY block_rate DESC LIMIT #{limit}")
    List<Map<String, Object>> getTopApisByBlockRate(@Param("date") LocalDate date, @Param("limit") int limit);
    
    /**
     * 根据日期范围和API路径查询统计数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param apiPath API路径
     * @return 统计数据列表
     */
    @Select("SELECT * FROM firewall_statistics " +
            "WHERE stat_date >= #{startDate} AND stat_date <= #{endDate} AND api_path = #{apiPath} " +
            "ORDER BY stat_date DESC")
    List<FirewallStatistics> findByDateRangeAndApiPath(@Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate, 
                                                       @Param("apiPath") String apiPath);
    
    /**
     * 根据日期获取总请求数
     * 
     * @param date 日期
     * @return 总请求数
     */
    @Select("SELECT COALESCE(SUM(total_requests), 0) FROM firewall_statistics WHERE stat_date = #{date}")
    long getTotalRequestsByDate(@Param("date") LocalDate date);
}