package com.example.firewall.mapper;

import com.example.firewall.entity.FirewallWhitelist;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 防火墙白名单Mapper接口
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Mapper
public interface FirewallWhitelistMapper {
    
    /**
     * 查询所有有效的白名单
     * 
     * @return 白名单列表
     */
    @Select("SELECT * FROM firewall_whitelist WHERE enabled = true ORDER BY id")
    List<FirewallWhitelist> findAllValid();
    
    /**
     * 查询所有白名单
     * 
     * @return 白名单列表
     */
    @Select("SELECT * FROM firewall_whitelist ORDER BY id")
    List<FirewallWhitelist> findAll();
    
    /**
     * 根据ID查询白名单
     * 
     * @param id 白名单ID
     * @return 白名单
     */
    @Select("SELECT * FROM firewall_whitelist WHERE id = #{id}")
    FirewallWhitelist findById(Long id);
    
    /**
     * 根据IP地址查询白名单
     * 
     * @param ipAddress IP地址
     * @return 白名单
     */
    @Select("SELECT * FROM firewall_whitelist WHERE ip_address = #{ipAddress}")
    FirewallWhitelist findByIpAddress(String ipAddress);
    
    /**
     * 检查IP是否在白名单中
     * 
     * @param ipAddress IP地址
     * @return 是否在白名单中
     */
    @Select("SELECT COUNT(*) > 0 FROM firewall_whitelist " +
            "WHERE ip_address = #{ipAddress} AND enabled = true")
    boolean isWhitelisted(String ipAddress);
    
    /**
     * 插入新白名单
     * 
     * @param whitelist 白名单
     * @return 影响行数
     */
    @Insert("INSERT INTO firewall_whitelist (ip_address, description, enabled) " +
            "VALUES (#{ipAddress}, #{description}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FirewallWhitelist whitelist);
    
    /**
     * 更新白名单
     * 
     * @param whitelist 白名单
     * @return 影响行数
     */
    @Update("UPDATE firewall_whitelist SET ip_address = #{ipAddress}, description = #{description}, " +
            "enabled = #{enabled}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(FirewallWhitelist whitelist);
    
    /**
     * 删除白名单
     * 
     * @param id 白名单ID
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_whitelist WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 根据IP地址删除白名单
     * 
     * @param ipAddress IP地址
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_whitelist WHERE ip_address = #{ipAddress}")
    int deleteByIpAddress(String ipAddress);
    
    /**
     * 启用/禁用白名单
     * 
     * @param id 白名单ID
     * @param enabled 是否启用
     * @return 影响行数
     */
    @Update("UPDATE firewall_whitelist SET enabled = #{enabled}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
    
    /**
     * 批量插入白名单
     * 
     * @param whitelists 白名单列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO firewall_whitelist (ip_address, description, enabled) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.ipAddress}, #{item.description}, #{item.enabled})" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<FirewallWhitelist> whitelists);
    
    /**
     * 批量启用/禁用白名单
     * 
     * @param ids 白名单ID列表
     * @param enabled 是否启用
     * @return 影响行数
     */
    @Update("<script>" +
            "UPDATE firewall_whitelist SET enabled = #{enabled}, updated_time = CURRENT_TIMESTAMP " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);
    
    /**
     * 统计白名单数量
     * 
     * @return 白名单数量
     */
    @Select("SELECT COUNT(*) FROM firewall_whitelist")
    long count();
    
    /**
     * 统计有效的白名单数量
     * 
     * @return 有效的白名单数量
     */
    @Select("SELECT COUNT(*) FROM firewall_whitelist WHERE enabled = true")
    long countValid();
}