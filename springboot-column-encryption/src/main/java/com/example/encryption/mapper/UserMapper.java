package com.example.encryption.mapper;

import com.example.encryption.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 *
 * 注意：
 * 1. 不需要手动指定 TypeHandler，加密会自动处理
 * 2. 查询条件中的加密字段需要在应用层处理
 * 3. 支持复杂的查询操作
 */
@Mapper
public interface UserMapper {

    /**
     * 插入用户
     * 加密字段会自动加密存储
     */
    @Insert("INSERT INTO users (username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark) " +
            "VALUES (#{username}, #{phone}, #{idCard}, #{email}, #{bankCard}, #{address}, #{age}, #{gender}, #{occupation}, " +
            "#{createTime}, #{updateTime}, #{enabled}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 批量插入用户
     */
    @Insert({
            "<script>",
            "INSERT INTO users (username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark) VALUES ",
            "<foreach collection='users' item='user' separator=','>",
            "(#{user.username}, #{user.phone}, #{user.idCard}, #{user.email}, #{user.bankCard}, #{user.address}, " +
            "#{user.age}, #{user.gender}, #{user.occupation}, #{user.createTime}, #{user.updateTime}, #{user.enabled}, #{user.remark})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("users") List<User> users);

    /**
     * 根据ID查询用户
     * 加密字段会自动解密返回
     */
    @Select("SELECT id, username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark " +
            "FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT id, username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark " +
            "FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);

    /**
     * 查询所有用户
     */
    @Select("SELECT id, username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark " +
            "FROM users ORDER BY create_time DESC")
    List<User> findAll();

    /**
     * 根据手机号查询用户（注意：由于手机号加密，这里需要在应用层处理）
     */
    @Select("SELECT id, username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark " +
            "FROM users WHERE phone = #{encryptedPhone}")
    Optional<User> findByPhone(String encryptedPhone);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT id, username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark " +
            "FROM users WHERE email = #{encryptedEmail}")
    Optional<User> findByEmail(String encryptedEmail);

    /**
     * 分页查询用户
     */
    @Select("SELECT id, username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark " +
            "FROM users ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<User> findByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户总数
     */
    @Select("SELECT COUNT(*) FROM users")
    long count();

    /**
     * 更新用户信息
     */
    @Update("UPDATE users SET username = #{username}, phone = #{phone}, id_card = #{idCard}, email = #{email}, " +
            "bank_card = #{bankCard}, address = #{address}, age = #{age}, gender = #{gender}, " +
            "occupation = #{occupation}, update_time = #{updateTime}, enabled = #{enabled}, remark = #{remark} " +
            "WHERE id = #{id}")
    int update(User user);

    /**
     * 更新部分用户信息
     */
    @Update({
            "<script>",
            "UPDATE users SET update_time = #{updateTime}",
            "<if test='username != null'>, username = #{username}</if>",
            "<if test='phone != null'>, phone = #{phone}</if>",
            "<if test='idCard != null'>, id_card = #{idCard}</if>",
            "<if test='email != null'>, email = #{email}</if>",
            "<if test='bankCard != null'>, bank_card = #{bankCard}</if>",
            "<if test='address != null'>, address = #{address}</if>",
            "<if test='age != null'>, age = #{age}</if>",
            "<if test='gender != null'>, gender = #{gender}</if>",
            "<if test='occupation != null'>, occupation = #{occupation}</if>",
            "<if test='enabled != null'>, enabled = #{enabled}</if>",
            "<if test='remark != null'>, remark = #{remark}</if>",
            "WHERE id = #{id}",
            "</script>"
    })
    int updateSelective(User user);

    /**
     * 删除用户
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 批量删除用户
     */
    @Delete({
            "<script>",
            "DELETE FROM users WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据条件查询用户数量
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) FROM users WHERE 1=1",
            "<if test='username != null and username != \"\"'>AND username LIKE CONCAT('%', #{username}, '%')</if>",
            "<if test='enabled != null'>AND enabled = #{enabled}</if>",
            "<if test='age != null'>AND age = #{age}</if>",
            "<if test='gender != null and gender != \"\"'>AND gender = #{gender}</if>",
            "</script>"
    })
    long countByCondition(@Param("username") String username,
                         @Param("enabled") Boolean enabled,
                         @Param("age") Integer age,
                         @Param("gender") String gender);

    /**
     * 根据条件查询用户列表
     */
    @Select({
            "<script>",
            "SELECT id, username, phone, id_card, email, bank_card, address, age, gender, occupation, create_time, update_time, enabled, remark " +
            "FROM users WHERE 1=1",
            "<if test='username != null and username != \"\"'>AND username LIKE CONCAT('%', #{username}, '%')</if>",
            "<if test='enabled != null'>AND enabled = #{enabled}</if>",
            "<if test='age != null'>AND age = #{age}</if>",
            "<if test='gender != null and gender != \"\"'>AND gender = #{gender}</if>",
            "ORDER BY create_time DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<User> findByCondition(@Param("username") String username,
                              @Param("enabled") Boolean enabled,
                              @Param("age") Integer age,
                              @Param("gender") String gender,
                              @Param("offset") int offset,
                              @Param("limit") int limit);
}