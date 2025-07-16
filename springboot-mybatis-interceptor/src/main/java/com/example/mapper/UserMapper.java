package com.example.mapper;

import com.example.model.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO user(username, phone, id_card) " +
            "VALUES(#{username}, #{phone}, #{idCard})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET username=#{username}, phone=#{phone}, id_card=#{idCard} " +
            "WHERE id=#{id}")
    int update(User user);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(@Param("id") Long id);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}