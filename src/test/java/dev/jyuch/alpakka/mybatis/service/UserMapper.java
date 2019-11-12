package dev.jyuch.alpakka.mybatis.service;

import dev.jyuch.alpakka.mybatis.model.User;
import org.apache.ibatis.cursor.Cursor;

public interface UserMapper {
    int initialize();
    Cursor<User> select();
    User selectById(int id);
    int insert(User user);
}
