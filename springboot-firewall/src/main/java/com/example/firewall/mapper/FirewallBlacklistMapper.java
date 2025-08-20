package com.example.firewall.mapper;

import com.example.firewall.entity.FirewallBlacklist;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 防火墙黑名单Mapper接口
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Mapper
public interface FirewallBlacklistMapper {
    
    /**
     * 查询所有有效的黑名单
     * 
     * @return 黑名单列表
     */
    @Select("SELECT * FROM firewall_blacklist WHERE enabled = true " +
            "AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP) " +
            "ORDER BY id")
    List<FirewallBlacklist> findAllValid();
    
    /**
     * 查询所有黑名单
     * 
     * @return 黑名单列表
     */
    @Select("SELECT * FROM firewall_blacklist ORDER BY id")
    List<FirewallBlacklist> findAll();
    
    /**
     * 根据ID查询黑名单
     * 
     * @param id 黑名单ID
     * @return 黑名单
     */
    @Select("SELECT * FROM firewall_blacklist WHERE id = #{id}")
    FirewallBlacklist findById(Long id);
    
    /**
     * 根据IP地址查询黑名单
     * 
     * @param ipAddress IP地址
     * @return 黑名单
     */
    @Select("SELECT * FROM firewall_blacklist WHERE ip_address = #{ipAddress}")
    FirewallBlacklist findByIpAddress(String ipAddress);
    
    /**
     * 检查IP是否在黑名单中
     * 
     * @param ipAddress IP地址
     * @return 是否在黑名单中
     */
    @Select("SELECT COUNT(*) > 0 FROM firewall_blacklist " +
            "WHERE ip_address = #{ipAddress} AND enabled = true " +
            "AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)")
    boolean isBlacklisted(String ipAddress);
    
    /**
     * 插入新黑名单
     * 
     * @param blacklist 黑名单
     * @return 影响行数
     */
    @Insert("INSERT INTO firewall_blacklist (ip_address, reason, expire_time, enabled) " +
            "VALUES (#{ipAddress}, #{reason}, #{expireTime}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FirewallBlacklist blacklist);
    
    /**
     * 更新黑名单
     * 
     * @param blacklist 黑名单
     * @return 影响行数
     */
    @Update("UPDATE firewall_blacklist SET ip_address = #{ipAddress}, reason = #{reason}, " +
            "expire_time = #{expireTime}, enabled = #{enabled}, updated_time = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int update(FirewallBlacklist blacklist);
    
    /**
     * 删除黑名单
     * 
     * @param id 黑名单ID
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_blacklist WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 根据IP地址删除黑名单
     * 
     * @param ipAddress IP地址
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_blacklist WHERE ip_address = #{ipAddress}")
    int deleteByIpAddress(String ipAddress);
    
    /**
     * 启用/禁用黑名单
     * 
     * @param id 黑名单ID
     * @param enabled 是否启用
     * @return 影响行数
     */
    @Update("UPDATE firewall_blacklist SET enabled = #{enabled}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
    
    /**
     * 清理过期的黑名单
     * 
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_blacklist WHERE expire_time IS NOT NULL AND expire_time <= CURRENT_TIMESTAMP")
    int cleanExpired();
    
    /**
     * 批量插入黑名单
     * 
     * @param blacklists 黑名单列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO firewall_blacklist (ip_address, reason, expire_time, enabled) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.ipAddress}, #{item.reason}, #{item.expireTime}, #{item.enabled})" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<FirewallBlacklist> blacklists);
    
    /**
     * 统计黑名单数量
     * 
     * @return 黑名单数量
     */
    @Select("SELECT COUNT(*) FROM firewall_blacklist")
    long count();
    
    /**
     * 统计有效的黑名单数量
     * 
     * @return 有效的黑名单数量
     */
    @Select("SELECT COUNT(*) FROM firewall_blacklist WHERE enabled = true " +
            "AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)")
    long countValid();
}