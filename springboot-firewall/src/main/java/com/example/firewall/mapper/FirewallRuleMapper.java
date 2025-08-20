package com.example.firewall.mapper;

import com.example.firewall.entity.FirewallRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 防火墙规则Mapper接口
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Mapper
public interface FirewallRuleMapper {
    
    /**
     * 查询所有启用的规则
     * 
     * @return 规则列表
     */
    @Select("SELECT * FROM firewall_rule WHERE enabled = true ORDER BY id")
    List<FirewallRule> findAllEnabled();
    
    /**
     * 查询所有规则
     * 
     * @return 规则列表
     */
    @Select("SELECT * FROM firewall_rule ORDER BY id")
    List<FirewallRule> findAll();
    
    /**
     * 根据ID查询规则
     * 
     * @param id 规则ID
     * @return 规则
     */
    @Select("SELECT * FROM firewall_rule WHERE id = #{id}")
    FirewallRule findById(Long id);
    
    /**
     * 根据API模式查询规则
     * 
     * @param apiPattern API模式
     * @return 规则
     */
    @Select("SELECT * FROM firewall_rule WHERE api_pattern = #{apiPattern} AND enabled = true")
    FirewallRule findByApiPattern(String apiPattern);
    
    /**
     * 查询匹配指定API路径的规则
     * 
     * @param apiPath API路径
     * @return 规则列表
     */
    @Select("SELECT * FROM firewall_rule WHERE enabled = true ORDER BY LENGTH(api_pattern) DESC")
    List<FirewallRule> findMatchingRules(String apiPath);
    
    /**
     * 插入新规则
     * 
     * @param rule 规则
     * @return 影响行数
     */
    @Insert("INSERT INTO firewall_rule (rule_name, api_pattern, qps_limit, user_limit, time_window, enabled, description) " +
            "VALUES (#{ruleName}, #{apiPattern}, #{qpsLimit}, #{userLimit}, #{timeWindow}, #{enabled}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FirewallRule rule);
    
    /**
     * 更新规则
     * 
     * @param rule 规则
     * @return 影响行数
     */
    @Update("UPDATE firewall_rule SET rule_name = #{ruleName}, api_pattern = #{apiPattern}, " +
            "qps_limit = #{qpsLimit}, user_limit = #{userLimit}, time_window = #{timeWindow}, " +
            "enabled = #{enabled}, description = #{description}, updated_time = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int update(FirewallRule rule);
    
    /**
     * 删除规则
     * 
     * @param id 规则ID
     * @return 影响行数
     */
    @Delete("DELETE FROM firewall_rule WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 启用/禁用规则
     * 
     * @param id 规则ID
     * @param enabled 是否启用
     * @return 影响行数
     */
    @Update("UPDATE firewall_rule SET enabled = #{enabled}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
    
    /**
     * 批量启用/禁用规则
     * 
     * @param ids 规则ID列表
     * @param enabled 是否启用
     * @return 影响行数
     */
    @Update("<script>" +
            "UPDATE firewall_rule SET enabled = #{enabled}, updated_time = CURRENT_TIMESTAMP " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);
    
    /**
     * 统计规则数量
     * 
     * @return 规则数量
     */
    @Select("SELECT COUNT(*) FROM firewall_rule")
    long count();
    
    /**
     * 统计启用的规则数量
     * 
     * @return 启用的规则数量
     */
    @Select("SELECT COUNT(*) FROM firewall_rule WHERE enabled = true")
    long countEnabled();
}